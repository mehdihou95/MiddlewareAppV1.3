package com.middleware.processor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import com.middleware.shared.model.Interface;

@Data
@AllArgsConstructor
public class MessageContent {
    private byte[] fileContent;
    private String filename;
    private Long interfaceId;
    private Long clientId;
    private Interface interfaceEntity;
    
    public MultipartFile getMultipartFile() {
        return new MultipartFile() {
            @Override
            public String getName() { return filename; }
            @Override
            public String getOriginalFilename() { return filename; }
            @Override
            public String getContentType() { return "application/xml"; }
            @Override
            public boolean isEmpty() { return fileContent.length == 0; }
            @Override
            public long getSize() { return fileContent.length; }
            @Override
            public byte[] getBytes() { return fileContent; }
            @Override
            public java.io.InputStream getInputStream() { 
                return new java.io.ByteArrayInputStream(fileContent); 
            }
            @Override
            public void transferTo(java.io.File dest) throws java.io.IOException {
                java.nio.file.Files.write(dest.toPath(), fileContent);
            }
        };
    }
} 
