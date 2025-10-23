package t4m.toy_store.image.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Map;

@Service
public class ImageService {

    private final Cloudinary cloudinary;

    // <-- Spring "inject" bean Cloudinary qua constructor
    public ImageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    // Upload từ URL (Cloudinary sẽ tải ảnh hộ bạn)
    public String uploadFromUrl(String imageUrl, String publicId) throws Exception {
        Map<?, ?> res = cloudinary.uploader().upload(
                imageUrl,
                ObjectUtils.asMap(
                        "public_id", "toy-store/" + publicId, // thư mục & tên bạn muốn
                        "overwrite", true
                )
        );
        return (String) res.get("secure_url"); // link ảnh dùng để lưu DB
    }

    // Upload từ file local (nếu có)
    public String uploadFromFile(Path path, String publicId) throws Exception {
        Map<?, ?> res = cloudinary.uploader().upload(
                path.toFile(),
                ObjectUtils.asMap(
                        "public_id", "toy-store/" + publicId,
                        "overwrite", true
                )
        );
        return (String) res.get("secure_url");
    }
}
