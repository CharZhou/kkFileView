package cn.keking.web.controller;

import org.jodconverter.core.office.OfficeException;
import org.jodconverter.local.LocalConverter;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/convert")
public class ConvertController {

    @PostMapping("/convertToPdf")
    public ResponseEntity<FileSystemResource> convertToPdf(@RequestParam("file") MultipartFile file) throws IOException, OfficeException {
        // 获取文件名
        String fileName = file.getOriginalFilename();
        assert fileName != null;

        fileName = fileName.substring(fileName.lastIndexOf(File.separator) + 1);

        String fileType = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();

        File inFile = File.createTempFile("kkfile-", "." + fileType);
        new FileOutputStream(inFile).write(file.getBytes());

        File outFile = File.createTempFile("kkfile-", ".pdf");

        LocalConverter.Builder builder = LocalConverter.builder();
        builder.build().convert(inFile).to(outFile).execute();
        String contentDisposition = ContentDisposition
                .builder("attachment")
                .filename(outFile.getPath(), StandardCharsets.UTF_8)
                .build().toString();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(outFile));
    }
}
