package com.emp.controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:8080"})
public class FileUploadController {
    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        String path = System.getProperty("user.dir") + "/uploads/" + file.getOriginalFilename();
        file.transferTo(new File(path));
        return "Uploaded successfully: " + file.getOriginalFilename();
    }
}
