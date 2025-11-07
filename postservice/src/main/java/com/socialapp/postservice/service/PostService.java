package com.socialapp.postservice.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.socialapp.postservice.dto.request.LikePostRequest;
import com.socialapp.postservice.dto.response.CreatePostResponse;
import com.socialapp.postservice.entity.Post;
import com.socialapp.postservice.mapper.PostConverter;
import com.socialapp.postservice.repository.PostRepository;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final PostConverter postConverter;
    private final CloudinaryService cloudinaryService;

    public PostService(PostRepository postRepository, PostConverter postConverter, CloudinaryService cloudinaryService) {
        this.postRepository = postRepository;
        this.postConverter = postConverter;
        this.cloudinaryService = cloudinaryService;
    }

    public CreatePostResponse createPost(String userId, String content, String groupId, String privacy, MultipartFile[] mediaFiles) {
        List<String> mediaUrls = new ArrayList<>();

        if (mediaFiles != null && mediaFiles.length > 0) {
            for (MultipartFile file : mediaFiles) {
                String fileType = file.getContentType();
                String url;

                if (fileType != null && fileType.startsWith("video")) {
                    url = cloudinaryService.uploadVideo(file);
                } else {
                    url = cloudinaryService.uploadImage(file);
                }

                mediaUrls.add(url);
            }
        }

        String type = (groupId == null || groupId.isEmpty()) ? "personal" : "group";

        Post post = Post.builder()
                .authorId(userId)
                .groupId(groupId)
                .type(type)
                .content(content)
                .media(mediaUrls)
                .createdAt(Instant.now())
                .likes(new ArrayList<>())
                .commentsCount(0)
                .build();

        Post savedPost = postRepository.save(post);

        return postConverter.convertToCreatePostResponse(savedPost);
    }

    public Post handleLikePost(LikePostRequest likePostRequest) {
        Optional<Post> post = postRepository.findById(likePostRequest.getPostId());
        if(post.isPresent()){
            Post existingPost = post.get();
            List<String> likes = existingPost.getLikes();
            if (likes.contains(likePostRequest.getUserId())) {
                likes.remove(likePostRequest.getUserId());
            } else {
                likes.add(likePostRequest.getUserId());
            }
            existingPost.setLikes(likes);
            return postRepository.save(existingPost);
        }
        return null;
    }
}
