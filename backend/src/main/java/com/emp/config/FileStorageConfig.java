package com.emp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileStorageConfig {

    @Bean
    @ConfigurationProperties(prefix = "file.upload")
    public FileStorageProperties fileStorageProperties() {
        return new FileStorageProperties();
    }

    public static class FileStorageProperties {
        private String uploadDir = "uploads";

        public String getUploadDir() {
            return uploadDir;
        }

        public void setUploadDir(String uploadDir) {
            this.uploadDir = uploadDir;
        }

        public Path getUploadPath() {
            return Paths.get(uploadDir).toAbsolutePath().normalize();
        }
    }
}