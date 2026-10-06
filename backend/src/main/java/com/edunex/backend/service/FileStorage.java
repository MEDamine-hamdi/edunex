package com.edunex.backend.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {

    /** Stores the file and returns its storage key. */
    String store(MultipartFile file);

    Resource load(String key);

    void delete(String key);
}
