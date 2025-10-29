package t4m.toy_store.product.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.folder:toy-store-products}")
    private String folder;

    /**
     * Upload image to Cloudinary with transformations
     * @param file Image file to upload
     * @return Map containing secure_url and public_id
     */
    public Map<String, String> uploadImage(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }

        // Validate file size (max 10MB)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size must not exceed 10MB");
        }

        try {
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String publicId = folder + "/" + UUID.randomUUID().toString() + fileExtension;

            // Upload with transformations
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "folder", folder,
                            "resource_type", "image",
                            "transformation", new com.cloudinary.Transformation()
                                    .width(800)
                                    .height(800)
                                    .crop("limit")
                                    .quality("auto")
                                    .fetchFormat("auto"),
                            "overwrite", false
                    ));

            String secureUrl = (String) uploadResult.get("secure_url");
            String uploadedPublicId = (String) uploadResult.get("public_id");

            log.info("Successfully uploaded image to Cloudinary: {}", uploadedPublicId);

            return Map.of(
                    "url", secureUrl,
                    "publicId", uploadedPublicId
            );

        } catch (IOException e) {
            log.error("Error uploading image to Cloudinary: {}", e.getMessage());
            throw new IOException("Failed to upload image: " + e.getMessage(), e);
        }
    }

    /**
     * Delete image from Cloudinary
     * @param publicId Public ID of the image to delete
     */
    public void deleteImage(String publicId) {
        if (publicId == null || publicId.isEmpty()) {
            log.warn("Cannot delete image: publicId is null or empty");
            return;
        }

        try {
            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            String resultStatus = (String) result.get("result");

            if ("ok".equals(resultStatus)) {
                log.info("Successfully deleted image from Cloudinary: {}", publicId);
            } else {
                log.warn("Failed to delete image from Cloudinary: {} - Status: {}", publicId, resultStatus);
            }

        } catch (IOException e) {
            log.error("Error deleting image from Cloudinary: {}", e.getMessage());
        }
    }

    /**
     * Replace existing image with new one
     * @param oldPublicId Public ID of the old image to delete
     * @param newFile New image file to upload
     * @return Map containing new secure_url and public_id
     */
    public Map<String, String> replaceImage(String oldPublicId, MultipartFile newFile) throws IOException {
        // Upload new image first
        Map<String, String> uploadResult = uploadImage(newFile);

        // Delete old image if exists
        if (oldPublicId != null && !oldPublicId.isEmpty()) {
            deleteImage(oldPublicId);
        }

        return uploadResult;
    }

    /**
     * Get optimized image URL with transformations
     * @param publicId Public ID of the image
     * @param width Desired width
     * @param height Desired height
     * @return Transformed image URL
     */
    public String getOptimizedImageUrl(String publicId, int width, int height) {
        return cloudinary.url()
                .transformation(new com.cloudinary.Transformation()
                        .width(width)
                        .height(height)
                        .crop("fill")
                        .quality("auto")
                        .fetchFormat("auto"))
                .generate(publicId);
    }

    /**
     * Get thumbnail URL (200x200)
     * @param publicId Public ID of the image
     * @return Thumbnail URL
     */
    public String getThumbnailUrl(String publicId) {
        return getOptimizedImageUrl(publicId, 200, 200);
    }
}
