package com.akdemya.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.rag")
public class RagProperties {

    private String storagePath = "/tmp/akademia-sources";
    private int chunkSize = 1000;
    private int chunkOverlap = 200;
    private int retrievalTopK = 8;

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public int getChunkSize() { return chunkSize; }
    public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }

    public int getChunkOverlap() { return chunkOverlap; }
    public void setChunkOverlap(int chunkOverlap) { this.chunkOverlap = chunkOverlap; }

    public int getRetrievalTopK() { return retrievalTopK; }
    public void setRetrievalTopK(int retrievalTopK) { this.retrievalTopK = retrievalTopK; }
}
