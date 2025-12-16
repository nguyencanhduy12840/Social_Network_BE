package com.socialapp.chatservice.service;

import com.socialapp.chatservice.client.ProfileClient;
import com.socialapp.chatservice.dto.request.CreateChatRequest;
import com.socialapp.chatservice.dto.request.SendMessageRequest;
import com.socialapp.chatservice.dto.response.ChatResponse;
import com.socialapp.chatservice.dto.response.MessageResponse;
import com.socialapp.chatservice.dto.response.UserProfileResponse;
import com.socialapp.chatservice.entity.Chat;
import com.socialapp.chatservice.entity.Message;
import com.socialapp.chatservice.repository.ChatRepository;
import com.socialapp.chatservice.repository.MessageRepository;
import com.socialapp.chatservice.dto.event.ChatMessageEvent;
import com.socialapp.chatservice.kafka.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final ProfileClient profileClient;
    private final KafkaProducerService kafkaProducerService;
    private final OnlineUserService onlineUserService;
    private final CloudinaryService cloudinaryService;

    public boolean isUserOnline(String userId) {
        return onlineUserService.isUserOnline(userId);
    }

    /**
     * Tạo hoặc lấy conversation giữa 2 người
     */
    @Transactional
    public ChatResponse createOrGetChat(String currentUserId, CreateChatRequest request) {
        String recipientId = request.getRecipientId();

        // Kiểm tra không thể chat với chính mình
        if (currentUserId.equals(recipientId)) {
            throw new RuntimeException("Không thể tạo chat với chính mình");
        }

        // Tìm xem đã có conversation chưa
        Optional<Chat> existingChat = chatRepository.findByParticipants(currentUserId, recipientId);

        Chat chat;
        if (existingChat.isPresent()) {
            chat = existingChat.get();
        } else {
            // Tạo mới conversation
            List<String> participants = Arrays.asList(currentUserId, recipientId);
            chat = new Chat(participants);
            chat = chatRepository.save(chat);
        }

        return mapToChatResponse(chat, currentUserId);
    }

    @Transactional
    public MessageResponse sendMessage(String currentUserId, SendMessageRequest request) {
        return sendMessage(currentUserId, request, null);
    }

    /**
     * Gửi tin nhắn
     */
    @Transactional
    public MessageResponse sendMessage(String currentUserId, SendMessageRequest request, org.springframework.web.multipart.MultipartFile file) {
        // Kiểm tra chat có tồn tại không
        Chat chat = chatRepository.findById(request.getChatId())
                .orElseThrow(() -> new RuntimeException("Chat không tồn tại"));

        // Kiểm tra user có phải là thành viên của chat không
        if (!chat.getParticipants().contains(currentUserId)) {
            throw new RuntimeException("Bạn không có quyền gửi tin nhắn trong chat này");
        }

        String fileUrl = null;
        Message.MessageType type = Message.MessageType.TEXT;

        // Xử lý file upload
        if (file != null && !file.isEmpty()) {
            String contentType = file.getContentType();
            
            if (contentType != null) {
                if (contentType.startsWith("image/")) {
                    type = Message.MessageType.IMAGE;
                    fileUrl = cloudinaryService.uploadImage(file);
                } else if (contentType.startsWith("video/")) {
                    type = Message.MessageType.VIDEO;
                    fileUrl = cloudinaryService.uploadVideo(file);
                } else if (contentType.startsWith("audio/")) {
                    type = Message.MessageType.AUDIO;
                    fileUrl = cloudinaryService.uploadVideo(file); 
                } else {
                    type = Message.MessageType.FILE;
                    fileUrl = cloudinaryService.uploadFile(file);
                }
            } else {
                type = Message.MessageType.FILE;
                fileUrl = cloudinaryService.uploadFile(file);
            }
        }

        // Tạo tin nhắn mới
        Message message = Message.builder()
                .chatId(request.getChatId())
                .senderId(currentUserId)
                .content(request.getContent())
                .type(type)
                .fileUrl(fileUrl)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .isDeleted(false)
                .readBy(new ArrayList<>(Collections.singletonList(currentUserId)))
                .build();

        message = messageRepository.save(message);

        // Cập nhật thông tin tin nhắn cuối trong chat
        chat.setLastMessageId(message.getId());
        String lastMessageContent = message.getContent();
        if (type == Message.MessageType.IMAGE) lastMessageContent = "Đã gửi một ảnh";
        else if (type == Message.MessageType.VIDEO) lastMessageContent = "Đã gửi một video";
        else if (type == Message.MessageType.AUDIO) lastMessageContent = "Đã gửi một ghi âm";
        else if (type == Message.MessageType.FILE) lastMessageContent = "Đã gửi một file";
        
        if (message.getContent() != null && !message.getContent().isEmpty()) {
             chat.setLastMessage(message.getContent());
        } else {
             chat.setLastMessage(lastMessageContent);
        }
        
        chat.setLastMessageTime(message.getCreatedAt());
        chat.setLastMessageSenderId(currentUserId);
        chat.setUpdatedAt(Instant.now());
        chat.setReadBy(new ArrayList<>(Collections.singletonList(currentUserId))); 
        
        if (chat.getDeletedBy() != null) {
            chat.getDeletedBy().clear();
        } else {
            chat.setDeletedBy(new ArrayList<>());
        }
        
        chatRepository.save(chat);

        // === KAFKA ===
        sendMessageEventToKafka(message, chat, currentUserId);

        return mapToMessageResponse(message, currentUserId);
    }

    /**
     * Lấy danh sách tin nhắn trong conversation với phân trang
     */
    public Page<MessageResponse> getMessages(String currentUserId, String chatId, int page, int size) {
        // Kiểm tra chat có tồn tại không
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat không tồn tại"));

        // Kiểm tra user có phải là thành viên của chat không
        if (!chat.getParticipants().contains(currentUserId)) {
            throw new RuntimeException("Bạn không có quyền xem tin nhắn trong chat này");
        }

        // Lấy tin nhắn với phân trang, loại bỏ tin nhắn đã xóa soft và tin nhắn bị ẩn bởi người dùng hiện tại
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Message> messages = messageRepository.findByChatIdAndIsDeletedFalseAndDeletedByNotContains(
                chatId, currentUserId, pageable);

        return messages.map(message -> mapToMessageResponse(message, currentUserId));
    }

    /**
     * Lấy danh sách conversation của user với phân trang
     */
    public Page<ChatResponse> getChats(String currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Chat> chats = chatRepository.findByParticipantsContainingAndDeletedByNotContaining(currentUserId, currentUserId, pageable);

        return chats.map(chat -> mapToChatResponse(chat, currentUserId));
    }

    public long getGlobalUnreadCount(String currentUserId) {
        return messageRepository.countAllUnreadMessages(currentUserId);
    }

    /**
     * Đánh dấu tất cả tin nhắn trong chat là đã đọc
     */
    @Transactional
    public void markAsRead(String currentUserId, String chatId) {
        // Kiểm tra chat có tồn tại không
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat không tồn tại"));

        // Kiểm tra user có phải là thành viên của chat không
        if (!chat.getParticipants().contains(currentUserId)) {
            throw new RuntimeException("Bạn không có quyền trong chat này");
        }

        // Lấy tất cả tin nhắn chưa đọc
        List<Message> unreadMessages = messageRepository.findUnreadMessages(chatId, currentUserId);

        // Đánh dấu đã đọc
        for (Message message : unreadMessages) {
            if (!message.getReadBy().contains(currentUserId)) {
                message.getReadBy().add(currentUserId);
            }
        }

        if (!unreadMessages.isEmpty()) {
            messageRepository.saveAll(unreadMessages);
        }

        // Cập nhật readBy trong chat
        if (!chat.getReadBy().contains(currentUserId)) {
            chat.getReadBy().add(currentUserId);
            chatRepository.save(chat);
        }

        // === KAFKA: Gửi event đã đọc ===
        if (!unreadMessages.isEmpty()) {
            sendReadMessageEventToKafka(unreadMessages.get(unreadMessages.size() - 1), chat, currentUserId);
        }
    }

    /**
     * Xóa tin nhắn (soft delete)
     */
    @Transactional
    public void deleteMessage(String currentUserId, String messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Tin nhắn không tồn tại"));

        // Check if user is in the chat
        Chat chat = chatRepository.findById(message.getChatId())
                 .orElseThrow(() -> new RuntimeException("Chat không tồn tại"));

        if (!chat.getParticipants().contains(currentUserId)) {
            throw new RuntimeException("Bạn không có quyền xóa tin nhắn trong chat này");
        }

        // Soft delete (chỉ xóa phía mình)
        if (message.getDeletedBy() == null) {
            message.setDeletedBy(new ArrayList<>());
        }
        if (!message.getDeletedBy().contains(currentUserId)) {
            message.getDeletedBy().add(currentUserId);
            message.setUpdatedAt(Instant.now());
            messageRepository.save(message);
        }
    }

    @Transactional
    public void deleteChat(String currentUserId, String chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat không tồn tại"));

        // Kiểm tra permission
        if (!chat.getParticipants().contains(currentUserId)) {
            throw new RuntimeException("Bạn không có quyền xóa conversation này");
        }

        // Create list if null
        if (chat.getDeletedBy() == null) {
            chat.setDeletedBy(new ArrayList<>());
        }

        // Add user to chat deleted list
        if (!chat.getDeletedBy().contains(currentUserId)) {
            chat.getDeletedBy().add(currentUserId);
            chatRepository.save(chat);
        }

        // Soft delete all messages in chat for this user
        List<Message> messages = messageRepository.findAllByChatId(chatId);
        for (Message msg : messages) {
            if (msg.getDeletedBy() == null) {
                msg.setDeletedBy(new ArrayList<>());
            }
            if (!msg.getDeletedBy().contains(currentUserId)) {
                msg.getDeletedBy().add(currentUserId);
            }
        }
        messageRepository.saveAll(messages);
    }

    /**
     * Lấy chi tiết một conversation
     */
    public ChatResponse getChatById(String currentUserId, String chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat không tồn tại"));

        // Kiểm tra user có phải là thành viên của chat không
        if (!chat.getParticipants().contains(currentUserId)) {
            throw new RuntimeException("Bạn không có quyền xem chat này");
        }

        return mapToChatResponse(chat, currentUserId);
    }

    /**
     * Lấy ID người tham gia còn lại trong chat
     */
    public String getOtherParticipantId(String chatId, String currentUserId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat không tồn tại"));
        
        return chat.getParticipants().stream()
                .filter(id -> !id.equals(currentUserId))
                .findFirst()
                .orElse(null);
    }

    // === Helper Methods ===

    private ChatResponse mapToChatResponse(Chat chat, String currentUserId) {
        // Tìm người chat với mình
        String otherUserId = chat.getParticipants().stream()
                .filter(id -> !id.equals(currentUserId))
                .findFirst()
                .orElse(null);

        // Lấy thông tin user từ profile service
        ChatResponse.ParticipantInfo otherParticipant = null;
        if (otherUserId != null) {
            boolean isOnline = onlineUserService.isUserOnline(otherUserId);
            try {
                UserProfileResponse profile = profileClient.getUserProfile(otherUserId);
                otherParticipant = ChatResponse.ParticipantInfo.builder()
                        .userId(profile.getUserId())
                        .fullName(profile.getFullName())
                        .avatarUrl(profile.getAvatarUrl())
                        .isOnline(isOnline)
                        .build();
            } catch (Exception e) {
                System.err.println("Error fetching user profile: " + e.getMessage());
                // Fallback nếu không lấy được thông tin
                otherParticipant = ChatResponse.ParticipantInfo.builder()
                        .userId(otherUserId)
                        .fullName("Unknown User")
                        .avatarUrl(null)
                        .isOnline(isOnline)
                        .build();
            }
        }

        // Calculate unread count (exclude deleted messages)
        long unreadCount = messageRepository.countUnreadMessages(chat.getId(), currentUserId);

        // Get visible last message for this user
        Message lastMessage = messageRepository.findFirstByChatIdAndIsDeletedFalseAndDeletedByNotContainingOrderByCreatedAtDesc(chat.getId(), currentUserId);
        
        String lastMessageContent = null;
        Instant lastMessageTime = null;
        String lastMessageSenderId = null;

        if (lastMessage != null) {
            lastMessageTime = lastMessage.getCreatedAt();
            lastMessageSenderId = lastMessage.getSenderId();

            // Format content based on type
            if (lastMessage.getType() == Message.MessageType.IMAGE) lastMessageContent = "Send an image";
            else if (lastMessage.getType() == Message.MessageType.VIDEO) lastMessageContent = "Send a video";
            else if (lastMessage.getType() == Message.MessageType.AUDIO) lastMessageContent = "Send an audio";
            else if (lastMessage.getType() == Message.MessageType.FILE) lastMessageContent = "Send a file";
            else lastMessageContent = lastMessage.getContent();
        }

        return ChatResponse.builder()
                .id(chat.getId())
                .participants(chat.getParticipants())
                .lastMessage(lastMessageContent)
                .lastMessageTime(lastMessageTime)
                .lastMessageSenderId(lastMessageSenderId)
                .unreadCount((int) unreadCount)
                .otherParticipant(otherParticipant)
                .createdAt(chat.getCreatedAt())
                .updatedAt(chat.getUpdatedAt())
                .build();
    }

    private MessageResponse mapToMessageResponse(Message message, String currentUserId) {
        return MessageResponse.builder()
                .id(message.getId())
                .chatId(message.getChatId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .type(message.getType())
                .fileUrl(message.getFileUrl())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .isDeleted(message.isDeleted())
                .readBy(message.getReadBy())
                .build();
    }

    // === KAFKA Helper Methods ===

    /**
     * Gửi event tin nhắn mới qua Kafka
     */
    private void sendMessageEventToKafka(Message message, Chat chat, String currentUserId) {
        try {
            // Lấy thông tin người gửi
            UserProfileResponse senderProfile = profileClient.getUserProfile(currentUserId);

            // Tìm người nhận
            String recipientId = chat.getParticipants().stream()
                    .filter(id -> !id.equals(currentUserId))
                    .findFirst()
                    .orElse(null);

            ChatMessageEvent event = ChatMessageEvent.builder()
                    .messageId(message.getId())
                    .chatId(message.getChatId())
                    .senderId(message.getSenderId())
                    .senderName(senderProfile.getFullName())
                    .senderAvatar(senderProfile.getAvatarUrl())
                    .recipientId(recipientId)
                    .content(message.getContent())
                    .type(message.getType())
                    .fileUrl(message.getFileUrl())
                    .createdAt(message.getCreatedAt())
                    .readBy(message.getReadBy())
                    .eventType(ChatMessageEvent.EventType.NEW_MESSAGE)
                    .build();

            // Gửi qua Kafka
            kafkaProducerService.sendChatMessage(event);

            // Gửi notification
            if (recipientId != null) {
                kafkaProducerService.sendChatNotification(event);
            }

        } catch (Exception e) {
            System.err.println("Error sending message event to Kafka: " + e.getMessage());
        }
    }

    /**
                    .recipientId(otherUserId)
                    .eventType(ChatMessageEvent.EventType.MESSAGE_DELETED)
                    .build();

            kafkaProducerService.sendChatMessage(event);

        } catch (Exception e) {
            System.err.println("Error sending delete event to Kafka: " + e.getMessage());
        }
    }

    /**
     * Gửi event tin nhắn đã đọc qua Kafka
     */
    private void sendReadMessageEventToKafka(Message message, Chat chat, String readerId) {
        try {
            String otherUserId = chat.getParticipants().stream()
                    .filter(id -> !id.equals(readerId))
                    .findFirst()
                    .orElse(null);

            ChatMessageEvent event = ChatMessageEvent.builder()
                    .messageId(message.getId())
                    .chatId(message.getChatId())
                    .senderId(readerId)
                    .recipientId(otherUserId)
                    .readBy(message.getReadBy())
                    .eventType(ChatMessageEvent.EventType.MESSAGE_READ)
                    .build();

            kafkaProducerService.sendChatMessage(event);

        } catch (Exception e) {
            System.err.println("Error sending read event to Kafka: " + e.getMessage());
        }
    }
}
