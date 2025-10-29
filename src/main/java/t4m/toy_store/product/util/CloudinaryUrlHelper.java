package t4m.toy_store.product.util;

public class CloudinaryUrlHelper {

    /**
     * Add Cloudinary transformations to image URL
     * @param url Original Cloudinary URL
     * @param transformations Transformation string (e.g., "f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0")
     * @return Transformed URL
     */
    public static String addTransformations(String url, String transformations) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        // Only process Cloudinary URLs
        if (!url.contains("cloudinary.com")) {
            return url;
        }

        // Check if URL already has transformations
        if (url.contains("/upload/") && !url.contains("/upload/" + transformations)) {
            // Insert transformations after /upload/
            return url.replace("/upload/", "/upload/" + transformations + "/");
        }

        return url;
    }

    /**
     * Get optimized thumbnail URL (300x200, auto format, auto quality)
     */
    public static String getThumbnailUrl(String url) {
        return addTransformations(url, "f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0");
    }

    /**
     * Get product detail URL (800x800, auto format, auto quality)
     */
    public static String getDetailUrl(String url) {
        return addTransformations(url, "f_auto,q_auto,w_800,h_800,c_limit,dpr_2.0");
    }

    /**
     * Get admin list URL (100x100, auto format, auto quality)
     */
    public static String getAdminListUrl(String url) {
        return addTransformations(url, "f_auto,q_auto,w_100,h_100,c_fill,dpr_2.0");
    }

    /**
     * Remove transformations from URL (get original)
     */
    public static String removeTransformations(String url) {
        if (url == null || !url.contains("cloudinary.com/")) {
            return url;
        }

        // Match pattern: /upload/[transformations]/
        return url.replaceAll("/upload/[^/]+/", "/upload/");
    }
}
