package com.vaadin.demo;

import java.util.List;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.loader.github.GitHubDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.ApplicationArguments;


@Configuration
public class AIConfig {

    private static final Logger log = LoggerFactory.getLogger(AIConfig.class);

    private final ValidateProperties validateProperties;

    public AIConfig(ValidateProperties validateProperties) {
        this.validateProperties = validateProperties;
    }

    /*
     * Keep track of the chat history for each chat.
     * This is used to generate the context for the AI.
     * Consider using TokenWindowChatMemory for more control.
     */
    @Bean
    ChatMemoryProvider chatMemoryProvider() {
        return chatId -> MessageWindowChatMemory.withMaxMessages(10);
    }

    /*
     * Define the embedding store (Vector database) to use.
     * Defaults to an in-memory store, use a proper database for larger datasets.
     * See
     * https://docs.langchain4j.dev/integrations/embedding-stores/ for more information.
     */
    @Bean
    EmbeddingStore<TextSegment> embeddingStore(
        @Value("${pinecone.api-key}") String apiKey,
        @Value("${pinecone.index}") String index) {

        if ("pinecone".equals(validateProperties.getEmbeddingStoreType())) {
            log.info("Using 'pinecone' embedding store");
            return PineconeEmbeddingStore.builder()
                .apiKey(apiKey)
                .index(index)
                .build();
        } else {
            log.info("Using 'in-memory' embedding store");
            return new InMemoryEmbeddingStore<>();
        }
    }

    @Bean
    EmbeddingModel embeddingModel(@Value("${open-ai.embedding-model.api-key}") String apiKey) {
        if ("openai".equals(validateProperties.getEmbeddingModelType()) && !apiKey.isEmpty()) {
            log.info("Using OpenAI embedding model");
            return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName("text-embedding-3-small")
                .build();
        } else {
            log.info("Using default embedding model");
            return null;
        }
    }

    /*
     * Import the documents from multiple sources into the embedding store.
     * Note: In real-world scenarios, you most likely want to process docs
     * on a separate build server as they are updated, not in the app that's
     * consuming them.
     */
    @Bean
    ApplicationRunner docImporter(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel, ApplicationArguments args) {
        return runnerArgs -> {
            if ("inmemory".equals(validateProperties.getEmbeddingStoreType()) || args.containsOption("import-docs")) {
                switch (validateProperties.getDocsSourceType()) {
                    case "local":
                        importLocalDocuments(embeddingStore, embeddingModel);
                        break;
                    case "github":
                        importGitHubDocuments(embeddingStore, embeddingModel);
                        break;
                    default:
                        log.error("Unknown document source type '{}'", validateProperties.getDocsSourceType());
                        break;
                }
            } else {
                log.info("Skipping document import. Use --import-docs to import documents.");
            }
        };
    }

    /*
     * Import the documents from the local file system into the embedding store.
     * Note: In real-world scenarios, you most likely want to process docs
     * on a separate build server as they are updated, not in the app that's
     * consuming them.
     */
    private void importLocalDocuments(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        log.info("Importing documents from {}", validateProperties.getDocsLocation());
        List<Document> docs = FileSystemDocumentLoader.loadDocumentsRecursively(validateProperties.getDocsLocation());
        log.info("Processing {} documents from local file system", docs.size());
        ingestDocumentsInBatches(docs, embeddingStore, embeddingModel);
    }

    /*
     * Import the documents from the github repository into the embedding store.
     * Note: In real-world scenarios, you most likely want to process docs
     * on a separate build server as they are updated, not in the app that's
     * consuming them.
     */
    private void importGitHubDocuments(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        log.info("Importing documents from github repo {}", validateProperties.getGithubRepo());
        DocumentParser parser = new TextDocumentParser();
        GitHubDocumentLoader loader = GitHubDocumentLoader.builder().gitHubToken(validateProperties.getGithubAccessToken()).build();
        List<Document> allDocs = loader.loadDocuments(validateProperties.getGithubOwner(), validateProperties.getGithubRepo(), validateProperties.getGithubBranch(), parser);
        log.info("Processing {} documents from GitHub", allDocs.size());
        ingestDocumentsInBatches(allDocs, embeddingStore, embeddingModel);
    }

    /*
     * Injest the documents in batches into the embedding store.
     */
     private void ingestDocumentsInBatches(List<Document> allDocs, EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .build();

            for (int i = 0; i < allDocs.size(); i += validateProperties.getInjestBatchSize()) {
                int end = Math.min(i + validateProperties.getInjestBatchSize(), allDocs.size());
                List<Document> batch = allDocs.subList(i, end);
                try {
                    ingestor.ingest(batch);
                    log.info("Imported batch {} to {} of {} documents", i, end, allDocs.size());
                } catch (Exception e) {
                    log.error("Error importing batch {} to {}: {}", i, end, e.getMessage());
                }
            }
        

        log.info("Finished importing {} documents", allDocs.size());        
    }

    /*
     * Define the content retriever to use. This is used to retrieve the relevant parts of a documents
     * from the embedding store before answering questions.
     */
    @Bean
    ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        if (embeddingModel != null) {
            return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .build();
        } else {
            return EmbeddingStoreContentRetriever.from(embeddingStore);
        }
    }
}
