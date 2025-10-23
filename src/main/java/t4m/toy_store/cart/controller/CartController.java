package t4m.toy_store.cart.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import t4m.toy_store.cart.dto.AddToCartRequest;
import t4m.toy_store.cart.dto.CartResponse;
import t4m.toy_store.cart.dto.UpdateCartItemRequest;
import t4m.toy_store.cart.exception.CartItemNotFoundException;
import t4m.toy_store.cart.exception.InsufficientStockException;
import t4m.toy_store.cart.service.CartService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AddToCartRequest request,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            // Get email from UserDetails or fallback to header
            String email = userDetails != null ? userDetails.getUsername() : userEmail;
            
            if (email == null || email.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            CartResponse cart = cartService.addToCart(email, request);
            return ResponseEntity.ok(cart);
        } catch (InsufficientStockException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping
    public ResponseEntity<?> getCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            // Get email from UserDetails or fallback to header
            String email = userDetails != null ? userDetails.getUsername() : userEmail;
            
            if (email == null || email.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            CartResponse cart = cartService.getCartByUser(email);
            return ResponseEntity.ok(cart);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<?> updateCartItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            // Get email from UserDetails or fallback to header
            String email = userDetails != null ? userDetails.getUsername() : userEmail;
            
            if (email == null || email.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            CartResponse cart = cartService.updateCartItem(email, cartItemId, request);
            return ResponseEntity.ok(cart);
        } catch (CartItemNotFoundException | InsufficientStockException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<?> removeCartItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long cartItemId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            // Get email from UserDetails or fallback to header
            String email = userDetails != null ? userDetails.getUsername() : userEmail;
            
            if (email == null || email.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            CartResponse cart = cartService.removeCartItem(email, cartItemId);
            return ResponseEntity.ok(cart);
        } catch (CartItemNotFoundException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<?> clearCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            // Get email from UserDetails or fallback to header
            String email = userDetails != null ? userDetails.getUsername() : userEmail;
            
            if (email == null || email.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            cartService.clearCart(email);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Cart cleared successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
