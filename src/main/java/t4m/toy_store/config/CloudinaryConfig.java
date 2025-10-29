package t4m.toy_store.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary() {
        Map<String, String> cfg = Map.of(
                "cloud_name", "t4m",
                "api_key", "859852221532925",
                "api_secret", "_eV7ZF7Eu71bj5jPHSx3GKKcl9E"
        );
        return new Cloudinary(cfg);
    }
}
