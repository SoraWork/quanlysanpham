package com.hoaiphong.quanlysanpham.service.impl;

import com.hoaiphong.quanlysanpham.configuration.Translator;
import com.hoaiphong.quanlysanpham.exception.SomeThingWrongException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageServiceImpl {

    private final Path uploadDir = Paths.get("uploads");

    public FileStorageServiceImpl() throws IOException {
        Files.createDirectories(uploadDir);
    }

    public String save(MultipartFile file) {
        try {
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = uploadDir.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/" + fileName;
        } catch (IOException e) {
            throw new SomeThingWrongException("file lưu thất bại");
        }
    }
}

