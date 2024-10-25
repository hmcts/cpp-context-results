package uk.gov.moj.cpp.results.event.service;

import java.util.UUID;

public class FileParams {
    private UUID fileId;
    private String filename;

    public FileParams(UUID fileId, String filename) {
        this.fileId = fileId;
        this.filename = filename;
    }

    public FileParams() {}

    public UUID getFileId() {            return fileId;        }
    public void setFileId(UUID fileId) {            this.fileId = fileId;        }
    public String getFilename() {             return filename;        }
    public void setFilename(String filename) {             this.filename = filename;         }
}
