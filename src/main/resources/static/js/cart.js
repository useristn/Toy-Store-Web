// Cart functionality
document.addEventListener('DOMContentLoaded', function() {
    loadCart();

    // Clear cart button
    const clearCartBtn = document.getElementById('clearCartBtn');
    if (clearCartBtn) {
        clearCartBtn.addEventListener('click', clearCart);
    }

    // Checkout button
    const checkoutBtn = document.getElementById('checkoutBtn');
    if (checkoutBtn) {
        checkoutBtn.addEventListener('click', function() {
            window.location.href = '/checkout';
        });
    }
});

// Load cart from API
async function loadCart() {
    console.log('loadCart() called');
    const cartLoading = document.getElementById('cartLoading');
    const emptyCartMessage = document.getElementById('emptyCartMessage');
    const cartItemsList = document.getElementById('cartItemsList');
    const checkoutBtn = document.getElementById('checkoutBtn');
    const clearCartBtn = document.getElementById('clearCartBtn');

    // Show loading
    if (cartLoading) cartLoading.style.display = 'block';

    try {
        const token = localStorage.getItem('authToken') || localStorage.getItem('token');
        const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
        
        if (!token || !userEmail) {
            window.location.href = '/login';
            return;
        }

        const response = await fetch('/api/cart', {
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
                'X-User-Email': userEmail
            }
        });

        if (response.status === 401) {
            window.location.href = '/login';
            return;
        }

        if (!response.ok) {
            throw new Error('Failed to load cart');
        }

        const cart = await response.json();
        console.log('Cart loaded:', cart);

        // Hide loading
        cartLoading.style.display = 'none';

        if (cart.items && cart.items.length > 0) {
            console.log('Displaying', cart.items.length, 'items');
            // Show cart items
            emptyCartMessage.style.display = 'none';
            cartItemsList.innerHTML = '';
            
            cart.items.forEach(item => {
                const itemElement = createCartItemElement(item);
                cartItemsList.appendChild(itemElement);
            });

            // Update summary
            updateCartSummary(cart);

            // Enable buttons
            if (checkoutBtn) checkoutBtn.disabled = false;
            if (clearCartBtn) clearCartBtn.disabled = false;
        } else {
            // Show empty cart message
            emptyCartMessage.style.display = 'block';
            cartItemsList.innerHTML = '';
            
            // Update summary to zero
            document.getElementById('totalItems').textContent = '0';
            document.getElementById('totalPrice').textContent = '0 ₫';

            // Disable buttons
            if (checkoutBtn) checkoutBtn.disabled = true;
            if (clearCartBtn) clearCartBtn.disabled = true;
        }
    } catch (error) {
        console.error('Error loading cart:', error);
        if (cartLoading) {
            cartLoading.innerHTML = `
            <div class="alert alert-danger">
                <i class="fas fa-exclamation-circle me-2"></i>
                Không thể tải giỏ hàng. Vui lòng thử lại sau.
            </div>
        `;
        }
    }
}

// Create cart item element
function createCartItemElement(item) {
    const template = document.getElementById('cartItemTemplate');
    const clone = template.content.cloneNode(true);

    const cartItemDiv = clone.querySelector('.cart-item');
    cartItemDiv.setAttribute('data-item-id', item.id);

    // Set image
    const img = clone.querySelector('.cart-item-image');
    img.src = item.productImageUrl || '/images/placeholder.jpg';
    img.alt = item.productName;

    // Set product name
    clone.querySelector('.cart-item-name').textContent = item.productName;

    // Set price
    clone.querySelector('.cart-item-price').textContent = formatPrice(item.price);

    // Set stock
    clone.querySelector('.cart-item-stock').textContent = item.availableStock;

    // Set quantity
    const quantityInput = clone.querySelector('.quantity-input');
    quantityInput.value = item.quantity;
    quantityInput.max = item.availableStock;

    // Set subtotal
    clone.querySelector('.cart-item-subtotal').textContent = formatPrice(item.subtotal);

    // Add event listeners for quantity buttons
    const decreaseBtn = clone.querySelector('.quantity-decrease');
    const increaseBtn = clone.querySelector('.quantity-increase');
    const removeBtn = clone.querySelector('.remove-item');

    decreaseBtn.addEventListener('click', () => {
        if (parseInt(quantityInput.value) > 1) {
            quantityInput.value = parseInt(quantityInput.value) - 1;
            updateCartItemQuantity(item.id, parseInt(quantityInput.value));
        }
    });

    increaseBtn.addEventListener('click', () => {
        if (parseInt(quantityInput.value) < item.availableStock) {
            quantityInput.value = parseInt(quantityInput.value) + 1;
            updateCartItemQuantity(item.id, parseInt(quantityInput.value));
        } else {
            showNotification('Không đủ số lượng trong kho!', 'warning');
        }
    });

    quantityInput.addEventListener('change', () => {
        let value = parseInt(quantityInput.value);
        if (value < 1) value = 1;
        if (value > item.availableStock) {
            value = item.availableStock;
            showNotification('Không đủ số lượng trong kho!', 'warning');
        }
        quantityInput.value = value;
        updateCartItemQuantity(item.id, value);
    });

    removeBtn.addEventListener('click', () => {
        removeCartItem(item.id);
    });

    return clone;
}

// Update cart item quantity
async function updateCartItemQuantity(itemId, quantity) {
    try {
        const token = localStorage.getItem('authToken') || localStorage.getItem('token');
        const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
        
        if (!token || !userEmail) {
            window.location.href = '/login';
            return;
        }

        const response = await fetch(`/api/cart/items/${itemId}`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
                'X-User-Email': userEmail
            },
            body: JSON.stringify({ quantity: quantity })
        });

        if (response.status === 401) {
            window.location.href = '/login';
            return;
        }

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Failed to update cart item');
        }

        const cart = await response.json();
        
        // Update the specific item's subtotal
        const itemElement = document.querySelector(`[data-item-id="${itemId}"]`);
        if (itemElement) {
            const itemData = cart.items.find(i => i.id === itemId);
            if (itemData) {
                itemElement.querySelector('.cart-item-subtotal').textContent = formatPrice(itemData.subtotal);
            }
        }

        // Update summary
        updateCartSummary(cart);
        showNotification('Đã cập nhật số lượng!', 'success');
        
        // Update cart badge
        if (typeof updateCartBadge === 'function') {
            updateCartBadge();
        }
    } catch (error) {
        console.error('Error updating cart item:', error);
        showNotification(error.message || 'Không thể cập nhật số lượng!', 'error');
        // Reload cart to reset values
        loadCart();
    }
}

// Remove cart item
async function removeCartItem(itemId) {
    console.log('Attempting to remove item with ID:', itemId);

    try {
        const token = localStorage.getItem('authToken') || localStorage.getItem('token');
        const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
        
        if (!token || !userEmail) {
            window.location.href = '/login';
            return;
        }

        console.log('Sending DELETE request to:', `/api/cart/items/${itemId}`);
        const response = await fetch(`/api/cart/items/${itemId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
                'X-User-Email': userEmail
            }
        });

        console.log('Response status:', response.status);
        
        if (response.status === 401) {
            window.location.href = '/login';
            return;
        }

        if (!response.ok) {
            const error = await response.json();
            console.error('Error response:', error);
            throw new Error(error.error || 'Failed to remove cart item');
        }

        console.log('Successfully removed item');
        showNotification('Đã xóa sản phẩm khỏi giỏ hàng!', 'success');
        
        // Update cart badge immediately
        if (typeof updateCartBadge === 'function') {
            console.log('Updating cart badge...');
            updateCartBadge();
        }
        
        // Reload cart
        console.log('Reloading cart...');
        loadCart();
    } catch (error) {
        console.error('Error removing cart item:', error);
        showNotification(error.message || 'Không thể xóa sản phẩm!', 'error');
    }
}

// Clear entire cart
async function clearCart() {
    try {
        const token = localStorage.getItem('authToken') || localStorage.getItem('token');
        const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
        
        if (!token || !userEmail) {
            window.location.href = '/login';
            return;
        }

        const response = await fetch('/api/cart/clear', {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
                'X-User-Email': userEmail
            }
        });

        if (response.status === 401) {
            window.location.href = '/login';
            return;
        }

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Failed to clear cart');
        }

        showNotification('Đã xóa toàn bộ giỏ hàng!', 'success');
        
        // Reset cart badge immediately
        if (typeof updateCartBadge === 'function') {
            updateCartBadge();
        }
        
        // Reload cart
        loadCart();
    } catch (error) {
        console.error('Error clearing cart:', error);
        showNotification(error.message || 'Không thể xóa giỏ hàng!', 'error');
    }
}

// Update cart summary
function updateCartSummary(cart) {
    const subtotal = cart.totalPrice || 0;
    document.getElementById('totalItems').textContent = cart.totalItems || 0;
    document.getElementById('subtotalPrice').textContent = formatPrice(subtotal);
    
    // Calculate final total with voucher discount
    const voucherDiscount = parseFloat(localStorage.getItem('voucherDiscount')) || 0;
    const finalTotal = Math.max(0, subtotal - voucherDiscount);
    
    document.getElementById('totalPrice').textContent = formatPrice(finalTotal);
    
    // Store subtotal for voucher validation
    localStorage.setItem('cartSubtotal', subtotal);
    
    // Update cart badge in header if function exists
    if (typeof updateCartBadge === 'function') {
        updateCartBadge();
    }
    
    // Setup voucher button handlers
    setupVoucherHandlers();
    
    // Display applied voucher if exists
    displayAppliedVoucher();
}

// Format price with Vietnamese currency
function formatPrice(price) {
    if (price === null || price === undefined) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(price);
}

// Show notification
function showNotification(message, type = 'info') {
    // Create notification element
    const notification = document.createElement('div');
    notification.className = `alert alert-${type === 'error' ? 'danger' : type === 'success' ? 'success' : 'warning'} alert-dismissible fade show position-fixed`;
    notification.style.cssText = 'top: 80px; right: 20px; z-index: 9999; min-width: 300px;';
    notification.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;

    document.body.appendChild(notification);

    // Auto remove after 3 seconds
    setTimeout(() => {
        notification.remove();
    }, 3000);
}

// Add to cart function (to be used from product pages)
async function addToCart(productId, quantity = 1) {
    try {
        const token = localStorage.getItem('authToken') || localStorage.getItem('token');
        const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
        
        if (!token || !userEmail) {
            window.location.href = '/login';
            return;
        }

        const response = await fetch('/api/cart/add', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
                'X-User-Email': userEmail
            },
            body: JSON.stringify({
                productId: productId,
                quantity: quantity
            })
        });

        if (response.status === 401) {
            window.location.href = '/login';
            return;
        }

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Failed to add to cart');
        }

        showNotification('Đã thêm vào giỏ hàng!', 'success');
        
        // Update cart badge in header if function exists
        if (typeof updateCartBadge === 'function') {
            updateCartBadge();
        }
        
        return true;
    } catch (error) {
        console.error('Error adding to cart:', error);
        showNotification(error.message || 'Không thể thêm vào giỏ hàng!', 'error');
        return false;
    }
}

// ==================== VOUCHER FUNCTIONS ====================

function setupVoucherHandlers() {
    const applyBtn = document.getElementById('applyVoucherBtn');
    const removeBtn = document.getElementById('removeVoucherBtn');
    const voucherInput = document.getElementById('voucherCodeInput');
    
    if (applyBtn && !applyBtn.hasAttribute('data-handler-attached')) {
        applyBtn.addEventListener('click', applyVoucher);
        applyBtn.setAttribute('data-handler-attached', 'true');
    }
    
    if (removeBtn && !removeBtn.hasAttribute('data-handler-attached')) {
        removeBtn.addEventListener('click', removeVoucher);
        removeBtn.setAttribute('data-handler-attached', 'true');
    }
    
    if (voucherInput && !voucherInput.hasAttribute('data-handler-attached')) {
        voucherInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                applyVoucher();
            }
        });
        voucherInput.setAttribute('data-handler-attached', 'true');
    }
}

async function applyVoucher() {
    const voucherInput = document.getElementById('voucherCodeInput');
    const voucherCode = voucherInput.value.trim().toUpperCase();
    const messageDiv = document.getElementById('voucherMessage');
    
    if (!voucherCode) {
        showVoucherMessage('Vui lòng nhập mã giảm giá', 'danger');
        return;
    }
    
    // Get cart subtotal
    const subtotal = parseFloat(localStorage.getItem('cartSubtotal')) || 0;
    
    if (subtotal <= 0) {
        showVoucherMessage('Giỏ hàng trống', 'danger');
        return;
    }
    
    try {
        const token = localStorage.getItem('authToken') || localStorage.getItem('token');
        const response = await fetch(`/api/vouchers/validate?code=${voucherCode}&orderTotal=${subtotal}`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });
        
        const result = await response.json();
        
        if (result.valid) {
            // Save voucher info to localStorage
            localStorage.setItem('voucherCode', result.voucherCode);
            localStorage.setItem('voucherDiscount', result.discountAmount);
            
            // Update display
            displayAppliedVoucher();
            updateTotalPrice();
            
            showVoucherMessage(result.message, 'success');
            
            // Hide input, show applied voucher
            document.getElementById('voucherInputGroup').style.display = 'none';
            document.getElementById('appliedVoucherGroup').style.display = 'block';
        } else {
            showVoucherMessage(result.message, 'danger');
        }
    } catch (error) {
        console.error('Error validating voucher:', error);
        showVoucherMessage('Lỗi kết nối máy chủ', 'danger');
    }
}

function removeVoucher() {
    // Clear voucher from localStorage
    localStorage.removeItem('voucherCode');
    localStorage.removeItem('voucherDiscount');
    
    // Hide applied voucher, show input
    document.getElementById('appliedVoucherGroup').style.display = 'none';
    document.getElementById('voucherInputGroup').style.display = 'block';
    document.getElementById('voucherCodeInput').value = '';
    
    // Update total price
    updateTotalPrice();
    
    showNotification('Đã xóa mã giảm giá', 'info');
}

function displayAppliedVoucher() {
    const voucherCode = localStorage.getItem('voucherCode');
    const voucherDiscount = parseFloat(localStorage.getItem('voucherDiscount')) || 0;
    
    if (voucherCode && voucherDiscount > 0) {
        document.getElementById('appliedVoucherCode').textContent = voucherCode;
        document.getElementById('voucherDiscount').textContent = '- ' + formatPrice(voucherDiscount);
        document.getElementById('voucherInputGroup').style.display = 'none';
        document.getElementById('appliedVoucherGroup').style.display = 'block';
    } else {
        document.getElementById('voucherInputGroup').style.display = 'block';
        document.getElementById('appliedVoucherGroup').style.display = 'none';
    }
}

function updateTotalPrice() {
    const subtotal = parseFloat(localStorage.getItem('cartSubtotal')) || 0;
    const voucherDiscount = parseFloat(localStorage.getItem('voucherDiscount')) || 0;
    const finalTotal = Math.max(0, subtotal - voucherDiscount);
    
    document.getElementById('totalPrice').textContent = formatPrice(finalTotal);
}

function showVoucherMessage(message, type) {
    const messageDiv = document.getElementById('voucherMessage');
    messageDiv.className = `alert alert-${type} py-1 px-2 small mb-0`;
    messageDiv.textContent = message;
    messageDiv.style.display = 'block';
    
    setTimeout(() => {
        messageDiv.style.display = 'none';
    }, 5000);
}
