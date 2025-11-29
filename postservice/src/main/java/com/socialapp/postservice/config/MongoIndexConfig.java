package com.socialapp.postservice.config;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
@RequiredArgsConstructor
public class MongoIndexConfig {

    private final MongoTemplate mongoTemplate;

    @Bean
    public CommandLineRunner createIndexes() {
        return args -> {
            MongoCollection<Document> collection = mongoTemplate.getCollection("posts");

            collection.createIndex(
                    Indexes.ascending("privacy"),
                    new IndexOptions().name("idx_privacy")
            );

            collection.createIndex(
                    Indexes.ascending("authorId"),
                    new IndexOptions().name("idx_authorId")
            );

            collection.createIndex(
                    Indexes.descending("createdAt"),
                    new IndexOptions().name("idx_createdAt_desc")
            );

            collection.createIndex(
                    Indexes.compoundIndex(
                            Indexes.ascending("authorId"),
                            Indexes.ascending("privacy")
                    ),
                    new IndexOptions().name("idx_authorId_privacy")
            );

            collection.createIndex(
                    Indexes.compoundIndex(
                            Indexes.ascending("privacy"),
                            Indexes.descending("createdAt")
                    ),
                    new IndexOptions().name("idx_privacy_createdAt")
            );

            System.out.println("MongoDB indexes created successfully for posts collection");
        };
    }
}

