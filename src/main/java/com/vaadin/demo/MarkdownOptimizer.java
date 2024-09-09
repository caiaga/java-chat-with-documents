package com.vaadin.demo;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.DocumentTransformer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MarkdownOptimizer implements DocumentTransformer {

    private final String docsLocation;
    private final String baseDocUrl;

    public MarkdownOptimizer(String docsLocation, String baseDocUrl) {
        this.docsLocation = docsLocation;
        this.baseDocUrl = baseDocUrl;
    }

    @Override
    public Document transform(Document document) {
        String content = document.text();
        Metadata metadata = document.metadata();

        // Remove Markdown headers, image links, and table rows
        content = removeMarkdownElements(content);

        // Remove code blocks
        content = removeCodeBlocks(content);

        // Trim and clean up the content
        content = cleanContent(content);

        // add additional meta information like the display file name and doc url
        addMetaInformation(metadata);

        // Add display_file_breadcrumbs and doc_url to the beginning of the content
        String newContent = String.format("Breadcrumbs: %s\nURL: %s\n\n%s",
        metadata.get("display_file_breadcrumbs"),
        metadata.get("doc_url"),
        content);

        // add the display_file_breadcrumbs and doc_url to the beginning of the doc content

        // Create a new document with the transformed content
        Document transformedDocument = new Document(newContent, metadata);

        // Add additional meta information in a separate step
        return transformedDocument;
    }

    private void addMetaInformation(Metadata metadata) {
        String absolutePath = metadata.get("absolute_directory_path");
        String fileName = metadata.get("file_name");

        // Determine the file name for display
        String displayFileName = "readme.md".equalsIgnoreCase(fileName) 
            ? formatFolderName(getEnclosingFolderName(absolutePath))
            : formatFileName(fileName);

        // Transform the path to a GitHub URL
        String relativePath = Paths.get(docsLocation).relativize(Paths.get(absolutePath)).toString();
        String docUrl = baseDocUrl + relativePath + "/" + fileName;

        // Create the display_file_breadcrumbs
        List<String> breadcrumbsParts = Arrays.stream(relativePath.split("/"))
            .filter(part -> !part.isEmpty())
            .map(this::formatFolderName)
            .collect(Collectors.toList());

        // Add displayFileName to breadcrumbs if it's not already the last part
        if (breadcrumbsParts.isEmpty() || !breadcrumbsParts.get(breadcrumbsParts.size() - 1).equals(displayFileName)) {
            breadcrumbsParts.add(displayFileName);
        }

        String displayFileBreadcrumbs = String.join(" / ", breadcrumbsParts);

        metadata.add("display_file_name", displayFileName);
        metadata.add("doc_url", docUrl);
        metadata.add("display_file_breadcrumbs", displayFileBreadcrumbs);
    }

    private String getEnclosingFolderName(String path) {
        return Paths.get(path).getFileName().toString();
    }

    private String formatFolderName(String folderName) {
        return Arrays.stream(folderName.split("[-_]"))
            .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
            .collect(Collectors.joining(" "));
    }

    private String formatFileName(String fileName) {
        return formatFolderName(fileName.replaceFirst("\\.md$", ""));
    }

    private String removeMarkdownElements(String content) {
        return Arrays.stream(content.split("\n"))
                .filter(line -> !line.trim().startsWith("#") && 
                                !line.trim().startsWith("!") && 
                                !line.trim().startsWith("|"))
                .collect(Collectors.joining("\n"));
    }

    private String removeCodeBlocks(String content) {
        return content.replaceAll("```[\\s\\S]*?```", "");
    }

    private String cleanContent(String content) {
        // Remove extra whitespace, ensure single empty line between paragraphs, and trim
        return content.trim()
                      .replaceAll("\\s*\n\\s*", "\n\n")  // Replace any whitespace around newlines with two newlines
                      .replaceAll("\n{3,}", "\n\n")      // Replace 3 or more consecutive newlines with 2
                      .replaceAll("(?m)^\\s+$", "")      // Remove lines that contain only whitespace
                      .replaceAll("\n{3,}", "\n\n");     // Final check to ensure no more than one empty line
    }
}