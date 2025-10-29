package t4m.toy_store.payment.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import t4m.toy_store.order.entity.OrderStatus;
import t4m.toy_store.order.service.OrderService;
import t4m.toy_store.payment.service.VNPayService;

import java.util.*;

/**
 * VNPay Payment Controller
 * Xử lý Return URL và IPN URL từ VNPay
 */
@Controller
@RequestMapping("/api/payment/vnpay")
@RequiredArgsConstructor
@Slf4j
public class VNPayController {

    private final VNPayService vnPayService;
    private final OrderService orderService;

    /**
     * Return URL - Nơi VNPay redirect khách hàng về sau khi thanh toán
     * Cập nhật database tại đây vì IPN không hoạt động với localhost
     */
    @GetMapping("/return")
    public String paymentReturn(HttpServletRequest request, Model model) {
        try {
            // Lấy tất cả parameters từ VNPay
            Map<String, String> params = new HashMap<>();
            Enumeration<String> parameterNames = request.getParameterNames();
            while (parameterNames.hasMoreElements()) {
                String paramName = parameterNames.nextElement();
                String paramValue = request.getParameter(paramName);
                params.put(paramName, paramValue);
            }

            log.info("VNPay Return URL params: {}", params);

            // Verify checksum
            boolean isValidSignature = vnPayService.verifyPaymentResponse(params);
            
            if (!isValidSignature) {
                log.error("Invalid VNPay signature");
                model.addAttribute("success", false);
                model.addAttribute("message", "Chữ ký không hợp lệ");
                return "payment-result";
            }

            // Lấy thông tin từ response
            String vnp_ResponseCode = params.get("vnp_ResponseCode");
            String vnp_TransactionNo = params.get("vnp_TransactionNo");
            String vnp_TxnRef = params.get("vnp_TxnRef"); // Order number
            String vnp_Amount = params.get("vnp_Amount");
            String vnp_BankCode = params.get("vnp_BankCode");
            String vnp_CardType = params.get("vnp_CardType");
            String vnp_PayDate = params.get("vnp_PayDate");

            // Check response code
            boolean isSuccess = "00".equals(vnp_ResponseCode);

            // ✅ UPDATE DATABASE HERE (vì localhost không nhận được IPN)
            try {
                // Check if order exists
                if (orderService.orderExists(vnp_TxnRef)) {
                    // Check if not already processed
                    if (!orderService.isPaymentConfirmed(vnp_TxnRef)) {
                        // Update payment status
                        orderService.updateVNPayPaymentStatus(
                            vnp_TxnRef, 
                            isSuccess, 
                            vnp_TransactionNo, 
                            vnp_BankCode,
                            vnp_ResponseCode
                        );
                        log.info("✅ Payment status updated from Return URL for order: {}", vnp_TxnRef);
                    } else {
                        log.info("Order {} already confirmed", vnp_TxnRef);
                    }
                }
            } catch (Exception e) {
                log.error("Error updating payment status from Return URL", e);
            }

            // Add to model for display
            model.addAttribute("success", isSuccess);
            model.addAttribute("orderNumber", vnp_TxnRef);
            model.addAttribute("transactionNo", vnp_TransactionNo);
            model.addAttribute("amount", Long.parseLong(vnp_Amount) / 100); // Convert back
            model.addAttribute("bankCode", vnp_BankCode);
            model.addAttribute("cardType", vnp_CardType);
            model.addAttribute("payDate", vnp_PayDate);
            model.addAttribute("responseCode", vnp_ResponseCode);
            model.addAttribute("message", getResponseMessage(vnp_ResponseCode));

            log.info("Payment result for order {}: success={}, responseCode={}", vnp_TxnRef, isSuccess, vnp_ResponseCode);

            // Redirect based on payment result
            if (isSuccess) {
                // Payment successful - redirect to order confirmation
                return "redirect:/order-confirmation/" + vnp_TxnRef;
            } else {
                // Check if user just returned without completing (no actual transaction attempt)
                // or if payment actually failed/cancelled
                
                // If order is still PENDING_PAYMENT (not yet updated), redirect to payment-pending
                // If order is already CANCELLED/FAILED, redirect to order-confirmation to show the error
                try {
                    if (orderService.orderExists(vnp_TxnRef)) {
                        t4m.toy_store.order.dto.OrderResponse order = orderService.getOrderByNumber(vnp_TxnRef);
                        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
                            // Still pending, redirect to payment-pending page
                            return "redirect:/payment-pending/" + vnp_TxnRef;
                        } else {
                            // Already updated to CANCELLED/FAILED, redirect to order-confirmation
                            return "redirect:/order-confirmation/" + vnp_TxnRef;
                        }
                    }
                } catch (Exception e) {
                    log.error("Error checking order status", e);
                }
                
                // Default: redirect to payment pending
                return "redirect:/payment-pending/" + vnp_TxnRef;
            }

        } catch (Exception e) {
            log.error("Error processing VNPay return", e);
            model.addAttribute("success", false);
            model.addAttribute("message", "Lỗi xử lý kết quả thanh toán");
            return "payment-result";
        }
    }

    /**
     * IPN URL - VNPay gọi về để cập nhật kết quả thanh toán
     * Đây là server-to-server call, cập nhật database tại đây
     */
    @GetMapping("/ipn")
    @ResponseBody
    public ResponseEntity<Map<String, String>> paymentIpn(HttpServletRequest request) {
        Map<String, String> response = new HashMap<>();
        
        try {
            // Lấy tất cả parameters từ VNPay
            Map<String, String> params = new HashMap<>();
            Enumeration<String> parameterNames = request.getParameterNames();
            while (parameterNames.hasMoreElements()) {
                String paramName = parameterNames.nextElement();
                String paramValue = request.getParameter(paramName);
                params.put(paramName, paramValue);
            }

            log.info("VNPay IPN params: {}", params);

            // Verify checksum
            boolean isValidSignature = vnPayService.verifyPaymentResponse(params);
            
            if (!isValidSignature) {
                log.error("Invalid VNPay signature at IPN");
                response.put("RspCode", "97");
                response.put("Message", "Invalid Checksum");
                return ResponseEntity.ok(response);
            }

            // Lấy thông tin từ IPN
            String vnp_TxnRef = params.get("vnp_TxnRef"); // Order number
            String vnp_TransactionNo = params.get("vnp_TransactionNo");
            String vnp_ResponseCode = params.get("vnp_ResponseCode");
            String vnp_Amount = params.get("vnp_Amount");
            String vnp_BankCode = params.get("vnp_BankCode");
            String vnp_TransactionStatus = params.get("vnp_TransactionStatus");

            // Check if order exists
            boolean orderExists = orderService.orderExists(vnp_TxnRef);
            if (!orderExists) {
                log.warn("Order not found: {}", vnp_TxnRef);
                response.put("RspCode", "01");
                response.put("Message", "Order not Found");
                return ResponseEntity.ok(response);
            }

            // Check if order amount matches
            long vnpayAmount = Long.parseLong(vnp_Amount) / 100;
            boolean amountMatches = orderService.verifyOrderAmount(vnp_TxnRef, vnpayAmount);
            if (!amountMatches) {
                log.warn("Amount mismatch for order: {}", vnp_TxnRef);
                response.put("RspCode", "04");
                response.put("Message", "Invalid Amount");
                return ResponseEntity.ok(response);
            }

            // Check if order already confirmed
            boolean alreadyConfirmed = orderService.isPaymentConfirmed(vnp_TxnRef);
            if (alreadyConfirmed) {
                log.info("Order already confirmed: {}", vnp_TxnRef);
                response.put("RspCode", "02");
                response.put("Message", "Order already confirmed");
                return ResponseEntity.ok(response);
            }

            // Update payment status
            boolean isSuccess = "00".equals(vnp_ResponseCode);
            orderService.updateVNPayPaymentStatus(
                vnp_TxnRef, 
                isSuccess, 
                vnp_TransactionNo, 
                vnp_BankCode,
                vnp_ResponseCode
            );

            log.info("Updated payment status for order {}: success={}", vnp_TxnRef, isSuccess);

            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing VNPay IPN", e);
            response.put("RspCode", "99");
            response.put("Message", "Unknown error");
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Create new payment URL for existing pending order
     */
    @PostMapping("/create-url/{orderNumber}")
    @ResponseBody
    public ResponseEntity<?> createPaymentUrlForOrder(@PathVariable String orderNumber,
                                                      HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get order details
            if (!orderService.orderExists(orderNumber)) {
                response.put("error", "Order not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Get order to check status and amount
            t4m.toy_store.order.dto.OrderResponse order = orderService.getOrderByNumber(orderNumber);
            
            // Check if order is still PENDING_PAYMENT
            if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
                response.put("error", "Order is not pending payment. Current status: " + order.getStatus());
                return ResponseEntity.badRequest().body(response);
            }
            
            // Create VNPay payment URL
            String paymentUrl = vnPayService.createPaymentUrl(
                orderNumber,
                order.getTotalAmount(),
                "Thanh toan don hang " + orderNumber,
                httpRequest
            );
            
            response.put("paymentUrl", paymentUrl);
            response.put("success", true);
            
            log.info("Created new payment URL for order: {}", orderNumber);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error creating payment URL for order: {}", orderNumber, e);
            response.put("error", "Cannot create payment URL: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Create new payment link for existing order (for retry payment)
     */
    @PostMapping("/create-payment-link")
    @ResponseBody
    public ResponseEntity<?> createPaymentLink(@RequestBody Map<String, Object> requestData, 
                                                HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String orderNumber = (String) requestData.get("orderNumber");
            String orderInfo = (String) requestData.get("orderInfo");
            Number amountNumber = (Number) requestData.get("amount");
            
            if (orderNumber == null || amountNumber == null) {
                response.put("error", "Missing required parameters");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Convert amount to BigDecimal
            java.math.BigDecimal amount = new java.math.BigDecimal(amountNumber.toString());
            
            // Check if order exists and is still PENDING_PAYMENT
            if (!orderService.orderExists(orderNumber)) {
                response.put("error", "Order not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Create VNPay payment URL
            String paymentUrl = vnPayService.createPaymentUrl(
                orderNumber,
                amount,
                orderInfo != null ? orderInfo : "Thanh toan don hang " + orderNumber,
                httpRequest
            );
            
            response.put("paymentUrl", paymentUrl);
            response.put("success", true);
            
            log.info("Created payment link for order: {}", orderNumber);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error creating payment link", e);
            response.put("error", "Cannot create payment link");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get user-friendly message from response code
     */
    private String getResponseMessage(String responseCode) {
        return switch (responseCode) {
            case "00" -> "Giao dịch thành công";
            case "07" -> "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường).";
            case "09" -> "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng chưa đăng ký dịch vụ InternetBanking tại ngân hàng.";
            case "10" -> "Giao dịch không thành công do: Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần";
            case "11" -> "Giao dịch không thành công do: Đã hết hạn chờ thanh toán. Xin quý khách vui lòng thực hiện lại giao dịch.";
            case "12" -> "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng bị khóa.";
            case "13" -> "Giao dịch không thành công do Quý khách nhập sai mật khẩu xác thực giao dịch (OTP).";
            case "24" -> "Giao dịch không thành công do: Khách hàng hủy giao dịch";
            case "51" -> "Giao dịch không thành công do: Tài khoản của quý khách không đủ số dư để thực hiện giao dịch.";
            case "65" -> "Giao dịch không thành công do: Tài khoản của Quý khách đã vượt quá hạn mức giao dịch trong ngày.";
            case "75" -> "Ngân hàng thanh toán đang bảo trì.";
            case "79" -> "Giao dịch không thành công do: KH nhập sai mật khẩu thanh toán quá số lần quy định.";
            default -> "Giao dịch thất bại. Vui lòng liên hệ hỗ trợ.";
        };
    }
}
