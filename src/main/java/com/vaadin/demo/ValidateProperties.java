package com.vaadin.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;

@Component
@Validated
@ConfigurationProperties
public class ValidateProperties {

    @Value("${ai.docs.location}")
    private String docsLocation;

    @NotEmpty(message = "ai.embedding-store must be set")
    @Value("${ai.embedding-store}")
    private String embeddingStoreType;

    @NotEmpty(message = "ai.embedding-model must be set")
    @Value("${ai.embedding-model}")
    private String embeddingModelType;

    @NotEmpty (message = "ai.docs.source.type must be set")
    @Value("${ai.docs.source.type}")
    private String docsSourceType;

    @Value("${ai.injest.batch.size}")
    private int injestBatchSize;

    @Value("${base.doc.url}")
    private String baseDocUrl;

    @Value("${github.repo}")
    private String githubRepo;

    @Value("${github.branch}")
    private String githubBranch;

    @Value("${github.owner}")
    private String githubOwner;

    @Value("${github.access.token}")
    private String githubAccessToken;
   
    // Getters and Setters
    public String getDocsLocation() {
        return docsLocation;
    }

    public void setDocsLocation(String docsLocation) {
        this.docsLocation = docsLocation;
    }

    public String getEmbeddingStoreType() {
        return embeddingStoreType;
    }

    public void setEmbeddingStoreType(String embeddingStoreType) {
        this.embeddingStoreType = embeddingStoreType;
    }

    public String getEmbeddingModelType() {
        return embeddingModelType;
    }

    public void setEmbeddingModelType(String embeddingModelType) {
        this.embeddingModelType = embeddingModelType;
    }

    public String getDocsSourceType() {
        return docsSourceType;
    }

    public void setDocsSourceType(String docsSourceType) {
        this.docsSourceType = docsSourceType;
    }

    public int getInjestBatchSize() {
        return injestBatchSize;
    }

    public void setInjestBatchSize(int injestBatchSize) {
        this.injestBatchSize = injestBatchSize;
    }

    public String getBaseDocUrl() {
        return baseDocUrl;
    }

    public void setBaseDocUrl(String baseDocUrl) {
        this.baseDocUrl = baseDocUrl;
    }

    public String getGithubRepo() {
        return githubRepo;
    }

    public void setGithubRepo(String githubRepo) {
        this.githubRepo = githubRepo;
    }

    public String getGithubBranch() {
        return githubBranch;
    }

    public void setGithubBranch(String githubBranch) {
        this.githubBranch = githubBranch;
    }

    public String getGithubOwner() {
        return githubOwner;
    }

    public void setGithubOwner(String githubOwner) {
        this.githubOwner = githubOwner;
    }

    public String getGithubAccessToken() {
        return githubAccessToken;
    }

    public void setGithubAccessToken(String githubAccessToken) {
        this.githubAccessToken = githubAccessToken;
    }

    @PostConstruct
    public void validate() {
        if ("github".equalsIgnoreCase(docsSourceType)) {
            if (githubRepo == null || githubRepo.isEmpty()) {
                throw new IllegalArgumentException("GitHub repo URL must be configured");
            }
            if (githubBranch == null || githubBranch.isEmpty()) {
                throw new IllegalArgumentException("GitHub branch must be configured");
            }
            if (githubOwner == null || githubOwner.isEmpty()) {
                throw new IllegalArgumentException("GitHub owner must be configured");
            }
            if (githubAccessToken == null || githubAccessToken.isEmpty()) {
                throw new IllegalArgumentException("GitHub access token must be configured");
            }
        } else if ("local".equalsIgnoreCase(docsSourceType)) {
            if (docsLocation == null || docsLocation.isEmpty()) {
                throw new IllegalArgumentException("Document location must be configured");
            }
        } else {
            throw new IllegalArgumentException("Unknown document source type '" + docsSourceType + "'");
        }
    }
}