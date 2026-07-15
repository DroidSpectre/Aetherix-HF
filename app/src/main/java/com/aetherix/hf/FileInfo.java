package com.aetherix.hf;

public class FileInfo {
    private String filename;
    private long sizeBytes;
    private boolean isLfs;

    public FileInfo(String filename, long sizeBytes, boolean isLfs) {
        this.filename = filename;
        this.sizeBytes = sizeBytes;
        this.isLfs = isLfs;
    }

    public FileInfo(String filename, long sizeBytes) {
        this(filename, sizeBytes, false);
    }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    public boolean isLfs() { return isLfs; }
    public void setLfs(boolean lfs) { isLfs = lfs; }

    public String getFormattedSize() {
        if (sizeBytes < 1024) {
            return sizeBytes + " B";
        } else if (sizeBytes < 1024 * 1024) {
            return String.format("%.1f KB", sizeBytes / 1024.0);
        } else if (sizeBytes < 1024L * 1024 * 1024) {
            return String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", sizeBytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    @Override
    public String toString() {
        String size = getFormattedSize();
        String lfsMarker = isLfs ? " [LFS]" : "";
        return filename + " (" + size + lfsMarker + ")";
    }
}