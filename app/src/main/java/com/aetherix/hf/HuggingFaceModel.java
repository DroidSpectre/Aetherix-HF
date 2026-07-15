package com.aetherix.hf;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class HuggingFaceModel {
    private String id;
    private int downloads;
    private String lastModified;
    private String tags;
    private String pipelineTag;
    private String libraryName;
    private int likes;
    private boolean isPrivate;
    private boolean isGated;

    public HuggingFaceModel(String id, int downloads, String lastModified, String tags,
                            String pipelineTag, String libraryName, int likes,
                            boolean isPrivate, boolean isGated) {
        this.id = id;
        this.downloads = downloads;
        this.lastModified = lastModified;
        this.tags = tags;
        this.pipelineTag = pipelineTag;
        this.libraryName = libraryName;
        this.likes = likes;
        this.isPrivate = isPrivate;
        this.isGated = isGated;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getDownloads() { return downloads; }
    public void setDownloads(int downloads) { this.downloads = downloads; }

    public String getLastModified() { return lastModified; }
    public void setLastModified(String lastModified) { this.lastModified = lastModified; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getPipelineTag() { return pipelineTag; }
    public void setPipelineTag(String pipelineTag) { this.pipelineTag = pipelineTag; }

    public String getLibraryName() { return libraryName; }
    public void setLibraryName(String libraryName) { this.libraryName = libraryName; }

    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }

    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean aPrivate) { isPrivate = aPrivate; }

    public boolean isGated() { return isGated; }
    public void setGated(boolean gated) { isGated = gated; }

    public String getUrlEncodedId() {
        try {
            return URLEncoder.encode(id, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return id;
        }
    }

    public boolean hasPipelineTag() {
        return pipelineTag != null && !pipelineTag.isEmpty();
    }

    public String getFormattedDownloads() {
        if (downloads < 0) return "0";
        if (downloads < 1000) return String.valueOf(downloads);
        if (downloads < 1_000_000) return String.format("%.1fK", downloads / 1000.0);
        if (downloads < 1_000_000_000) return String.format("%.1fM", downloads / 1_000_000.0);
        return String.format("%.1fB", downloads / 1_000_000_000.0);
    }

    @Override
    public String toString() {
        return id + " (" + getFormattedDownloads() + " downloads)";
    }
}