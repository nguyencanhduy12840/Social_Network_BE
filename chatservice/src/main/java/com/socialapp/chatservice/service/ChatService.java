package com.socialapp.chatservice.service;

import com.socialapp.chatservice.client.ProfileClient;
import com.socialapp.chatservice.dto.request.CreateChatRequest;
import com.socialapp.chatservice.dto.request.SendMessageRequest;
import com.socialapp.chatservice.dto.response.ChatResponse;
import com.socialapp.chatservice.dto.response.MessageResponse;
import com.socialapp.chatservice.dto.response.OneUserProfileResponse;
import com.socialapp.chatservice.dto.response.UserResponse;
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
    public MessageResponse sendMessage(String currentUserId, SendMessageRequest request, List<org.springframework.web.multipart.MultipartFile> files) {
        // Kiểm tra chat có tồn tại không
        Chat chat = chatRepository.findById(request.getChatId())
                .orElseThrow(() -> new RuntimeException("Chat không tồn tại"));

        // Kiểm tra user có phải là thành viên của chat không
        if (!chat.getParticipants().contains(currentUserId)) {
            throw new RuntimeException("Bạn không có quyền gửi tin nhắn trong chat này");
        }

        List<String> attachments = new ArrayList<>();

        // Xử lý file upload
        if (files != null && !files.isEmpty()) {
            for (org.springframework.web.multipart.MultipartFile file : files) {
                if (file.isEmpty()) continue;
                
                String contentType = file.getContentType();
                String url;
                
                if (contentType != null) {
                    if (contentType.startsWith("image/")) {
                        url = cloudinaryService.uploadImage(file);
                    } else if (contentType.startsWith("video/")) {
                        url = cloudinaryService.uploadVideo(file);
                    } else if (contentType.startsWith("audio/")) {
                        url = cloudinaryService.uploadVideo(file); 
                    } else {
                        url = cloudinaryService.uploadFile(file);
                    }
                } else {
                    url = cloudinaryService.uploadFile(file);
                }
                attachments.add(url);
            }
        }

        // Tạo tin nhắn mới
        Message message = new Message(
            request.getChatId(),
            currentUserId,
            request.getContent(),
            attachments
        );

        message = messageRepository.save(message);

        // Cập nhật thông tin tin nhắn cuối trong chat
        chat.setLastMessageId(message.getId());
        
        String lastMessageContent = message.getContent();
        if (lastMessageContent == null || lastMessageContent.isEmpty()) {
            if (!attachments.isEmpty()) {
                lastMessageContent = "Sent " + (attachments.size() > 1 ? attachments.size() + " attachments" : "an attachment");
            } else {
                 lastMessageContent = "Sent a message";
            }
        }
        
        chat.setLastMessage(lastMessageContent);
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

        // Filter out chats that are effectively empty (no last message visible)
        // Note: This modifies the page size potentially.
        List<ChatResponse> filteredChats = chats.stream()
            .map(chat -> mapToChatResponse(chat, currentUserId))
            .filter(chatResponse -> chatResponse.getLastMessageTime() != null) 
            .toList();

        return new org.springframework.data.domain.PageImpl<>(filteredChats, pageable, chats.getTotalElements()); 
        // Note: Total elements might be inaccurate if we filter, but it's hard to get accurate count without complex aggregation query. 
        // User accepted this trade-off in plan.
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
        UserResponse otherParticipant = null;
        if (otherUserId != null) {
            // boolean isOnline = onlineUserService.isUserOnline(otherUserId);
            try {
                OneUserProfileResponse response = profileClient.getUserProfile(otherUserId);
                if (response != null && response.getData() != null) {
                     OneUserProfileResponse.UserProfileOne profile = response.getData();
                     otherParticipant = UserResponse.builder()
                            .id(profile.getId())
                            .username(profile.getUsername())
                            .avatarUrl(profile.getAvatarUrl())
                            // .isOnline(isOnline)
                            .build();
                }
            } catch (Exception e) {
                System.err.println("Error fetching user profile: " + e.getMessage());
            }
            
            // Fallback nếu không lấy được thông tin
            if (otherParticipant == null) {
                otherParticipant = UserResponse.builder()
                        .id(otherUserId)
                        .username("Unknown User")
                        .avatarUrl(null)
                        // .isOnline(isOnline)
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

            String content = lastMessage.getContent();
            if (content != null && !content.isEmpty()) {
                lastMessageContent = content;
            } else if (lastMessage.getAttachments() != null && !lastMessage.getAttachments().isEmpty()) {
                lastMessageContent = "Sent " + (lastMessage.getAttachments().size() > 1 ? lastMessage.getAttachments().size() + " attachments" : "an attachment");
            } else {
                lastMessageContent = "Sent a message";
            }
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
                .attachments(message.getAttachments())
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
            OneUserProfileResponse senderProfileResponse = profileClient.getUserProfile(currentUserId);
            String senderName = "Unknown User";
            String senderAvatar = null;
            
            if (senderProfileResponse != null && senderProfileResponse.getData() != null) {
                senderName = senderProfileResponse.getData().getUsername();
                senderAvatar = senderProfileResponse.getData().getAvatarUrl();
            }

            // Tìm người nhận
            String recipientId = chat.getParticipants().stream()
                    .filter(id -> !id.equals(currentUserId))
                    .findFirst()
                    .orElse(null);

            UserResponse senderUser = UserResponse.builder()
                    .id(message.getSenderId())
                    .username(senderName)
                    .avatarUrl(senderAvatar)
                    .build();

            ChatMessageEvent event = ChatMessageEvent.builder()
                    .eventType(ChatMessageEvent.EventType.NEW_MESSAGE)
                    .chatId(message.getChatId())
                    .messageId(message.getId())
                    .sender(senderUser)
                    .recipientId(recipientId)
                    .content(message.getContent())
                    .attachments(message.getAttachments())
                    .createdAt(message.getCreatedAt())
                    .readBy(message.getReadBy())
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
     * Gửi event tin nhắn đã đọc qua Kafka
     */
    private void sendReadMessageEventToKafka(Message message, Chat chat, String readerId) {
        try {
            String otherUserId = chat.getParticipants().stream()
                    .filter(id -> !id.equals(readerId))
                    .findFirst()
                    .orElse(null);

            ChatMessageEvent event = ChatMessageEvent.builder()
                    .eventType(ChatMessageEvent.EventType.MESSAGE_READ)
                    .chatId(message.getChatId())
                    .messageId(message.getId())
                    .sender(UserResponse.builder().id(readerId).build())
                    .recipientId(otherUserId)
                    .readBy(message.getReadBy())
                    .build();

            kafkaProducerService.sendChatMessage(event);

        } catch (Exception e) {
            System.err.println("Error sending read event to Kafka: " + e.getMessage());
        }
    }
}
