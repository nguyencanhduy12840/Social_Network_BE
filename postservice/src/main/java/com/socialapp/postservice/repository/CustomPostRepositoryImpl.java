package com.socialapp.postservice.repository;

import com.socialapp.postservice.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CustomPostRepositoryImpl implements CustomPostRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<Post> findPostsForMainScreen(String currentUserId, List<String> friendIds, String type, Pageable pageable) {
        // Tạo criteria phức tạp để filter posts theo logic:
        // 1. Posts của chính mình (tất cả privacy)
        // 2. Posts PUBLIC của bất kỳ ai
        // 3. Posts FRIENDS của bạn bè

        Criteria typeCriteria = type != null ? Criteria.where("type").is(type) : new Criteria();

        Criteria privacyCriteria = new Criteria().orOperator(
            // Tất cả posts của chính mình
            Criteria.where("authorId").is(currentUserId),

            // Tất cả posts PUBLIC
            Criteria.where("privacy").is("PUBLIC"),

            // Posts FRIENDS của bạn bè
            new Criteria().andOperator(
                Criteria.where("privacy").is("FRIENDS"),
                Criteria.where("authorId").in(friendIds)
            )
        );

        // Lọc bỏ group posts (groupId phải là null)
        Criteria notGroupPostCriteria = new Criteria().orOperator(
            Criteria.where("groupId").isNull(),
            Criteria.where("groupId").exists(false)
        );

        // Kết hợp type, privacy, và not-group criteria
        Criteria finalCriteria;
        if (type != null) {
            finalCriteria = new Criteria().andOperator(typeCriteria, privacyCriteria, notGroupPostCriteria);
        } else {
            finalCriteria = new Criteria().andOperator(privacyCriteria, notGroupPostCriteria);
        }

        // Query với pagination và sorting
        Query query = new Query(finalCriteria)
                .with(pageable)
                .with(pageable.getSort());

        // Đếm tổng số documents thỏa mãn criteria
        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Post.class);

        // Lấy posts với pagination
        List<Post> posts = mongoTemplate.find(query, Post.class);

        return new PageImpl<>(posts, pageable, total);
    }
}

