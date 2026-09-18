package com.ecommerce.project.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private Cloudinary cloudinary;

    @Override
    public String uploadImage(String path, MultipartFile file) throws IOException {
        // Upload the file to Cloudinary
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", "vastu_products",
                "public_id", UUID.randomUUID().toString()
        ));

        // Return the secure cloud URL instead of a local file name
        return uploadResult.get("secure_url").toString();
    }
}