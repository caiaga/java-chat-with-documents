package com.vaadin.demo;

import java.util.List;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.ApplicationArguments;

import static dev.langchain4j.internal.Utils.randomUUID;

@Configuration
public class AIConfig {

    private static final Logger log = LoggerFactory.getLogger(AIConfig.class);

    @Value("${ai.docs.location}")
    private String docsLocation;

    @Value("${ai.embedding-store}")
    private String embeddingStoreType;

    @Value("${ai.embedding-model}")
    private String embeddingModelType;

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

        if ("pinecone".equals(embeddingStoreType)) {
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
        if ("openai".equals(embeddingModelType) && !apiKey.isEmpty()) {
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
     * Import the documents from the file system into the embedding store.
     * Note: In real-world scenarios, you most likely want to process docs
     * on a separate build server as they are updated, not in the app that's
     * consuming them.
     */
    @Bean
    ApplicationRunner docImporter(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel, ApplicationArguments args) {
        return runnerArgs -> {
            if ("inmemory".equals(embeddingStoreType) || args.containsOption("import-docs")) {
                if (docsLocation == null || docsLocation.isEmpty()) {
                    log.error("No document location specified, configure 'ai.docs.location' in application.properties");
                    return;
                }
                log.info("Importing documents from {}", docsLocation);
                var docs = FileSystemDocumentLoader.loadDocumentsRecursively(docsLocation);

                if (embeddingModel != null) {
                    EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                        .embeddingStore(embeddingStore)
                        .embeddingModel(embeddingModel)
                        .build();

                    int batchSize = 1;
                    for (int i = 0; i < docs.size(); i += batchSize) {
                        int end = Math.min(i + batchSize, docs.size());
                        List<Document> batch = docs.subList(i, end);
                        try {
                            ingestor.ingest(batch);
                            log.info("Imported batch {} to {} of {} documents", i, end, docs.size());
                        } catch (Exception e) {
                            log.error("Error importing batch {} to {}: {}", i, end, e.getMessage());
                        }
                    }
                } else {
                    EmbeddingStoreIngestor.ingest(docs, embeddingStore);
                }
                log.info("Finished importing {} documents", docs.size());
            } else {
                log.info("Skipping document import. Use --import-docs to import documents.");
            }
        };
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
