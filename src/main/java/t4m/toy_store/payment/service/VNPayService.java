package t4m.toy_store.payment.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import t4m.toy_store.config.VNPayConfig;
import t4m.toy_store.payment.util.VNPayUtil;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * VNPay Payment Service
 * Xử lý tạo URL thanh toán VNPay
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VNPayService {

    private final VNPayConfig vnPayConfig;

    /**
     * Tạo URL thanh toán VNPay
     * 
     * @param orderNumber Mã đơn hàng
     * @param totalAmount Số tiền thanh toán
     * @param orderInfo Thông tin đơn hàng
     * @param request HttpServletRequest để lấy IP
     * @return URL thanh toán VNPay
     */
    public String createPaymentUrl(String orderNumber, BigDecimal totalAmount, String orderInfo, HttpServletRequest request) {
        try {
            // Chuyển đổi số tiền sang đơn vị VNPay yêu cầu (nhân 100)
            long amount = totalAmount.multiply(new BigDecimal(100)).longValue();
            
            // Lấy IP Address
            String vnp_IpAddr = VNPayUtil.getIpAddress(request);
            
            // Tạo các tham số
            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", VNPayConfig.VERSION);
            vnp_Params.put("vnp_Command", VNPayConfig.COMMAND);
            vnp_Params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
            vnp_Params.put("vnp_Amount", String.valueOf(amount));
            vnp_Params.put("vnp_CurrCode", VNPayConfig.CURRENCY_CODE);
            
            // Mã ngân hàng/ví điện tử - để trống để khách chọn
            // Hoặc có thể set: VNPAYQR, VNBANK, INTCARD
            // vnp_Params.put("vnp_BankCode", "");
            
            vnp_Params.put("vnp_TxnRef", orderNumber);
            vnp_Params.put("vnp_OrderInfo", orderInfo);
            vnp_Params.put("vnp_OrderType", VNPayConfig.ORDER_TYPE);
            vnp_Params.put("vnp_Locale", VNPayConfig.LOCALE);
            vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
            vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
            
            // Tạo thời gian
            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnp_CreateDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
            
            // Thời gian hết hạn thanh toán (15 phút)
            cld.add(Calendar.MINUTE, 15);
            String vnp_ExpireDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);
            
            // Build hash data và query string
            String hashData = VNPayUtil.hashAllFields(vnp_Params);
            String vnpSecureHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData);
            
            // Build query URL
            String queryUrl = VNPayUtil.buildQuery(vnp_Params);
            queryUrl += "&vnp_SecureHash=" + vnpSecureHash;
            
            String paymentUrl = vnPayConfig.getPayUrl() + "?" + queryUrl;
            
            // Log để debug
            log.info("=== VNPay Payment URL Debug ===");
            log.info("Order: {}", orderNumber);
            log.info("Amount: {} VND", totalAmount);
            log.info("IP Address: {}", vnp_IpAddr);
            log.info("TmnCode: {}", vnPayConfig.getTmnCode());
            log.info("Hash Data: {}", hashData);
            log.info("Secure Hash: {}", vnpSecureHash);
            log.info("Payment URL: {}", paymentUrl);
            
            return paymentUrl;
            
        } catch (Exception e) {
            throw new RuntimeException("Error creating VNPay payment URL: " + e.getMessage(), e);
        }
    }

    /**
     * Verify checksum từ VNPay response
     */
    public boolean verifyPaymentResponse(Map<String, String> params) {
        // Tạo bản sao để không modify params gốc
        Map<String, String> fields = new HashMap<>(params);
        
        // Lấy secure hash từ VNPay
        String vnp_SecureHash = fields.get("vnp_SecureHash");
        
        // Loại bỏ hash fields trước khi tính checksum
        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");
        
        // Tính hash từ các params còn lại
        String hashData = VNPayUtil.hashAllFields(fields);
        String calculatedHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData);
        
        return calculatedHash.equalsIgnoreCase(vnp_SecureHash);
    }
}
