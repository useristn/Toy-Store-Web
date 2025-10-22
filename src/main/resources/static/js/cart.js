// Cart functionality
document.addEventListener('DOMContentLoaded', function() {
    loadCart();

    // Clear cart button
    const clearCartBtn = document.getElementById('clearCartBtn');
    if (clearCartBtn) {
        clearCartBtn.addEventListener('click', clearCart);
    }

    // Checkout button (placeholder for now)
    const checkoutBtn = document.getElementById('checkoutBtn');
    if (checkoutBtn) {
        checkoutBtn.addEventListener('click', function() {
            alert('Chức năng thanh toán sẽ được triển khai sau!');
        });
    }
});

// Load cart from API
async function loadCart() {
    const cartLoading = document.getElementById('cartLoading');
    const emptyCartMessage = document.getElementById('emptyCartMessage');
    const cartItemsList = document.getElementById('cartItemsList');
    const checkoutBtn = document.getElementById('checkoutBtn');
    const clearCartBtn = document.getElementById('clearCartBtn');

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

        // Hide loading
        cartLoading.style.display = 'none';

        if (cart.items && cart.items.length > 0) {
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
        cartLoading.innerHTML = `
            <div class="alert alert-danger">
                <i class="fas fa-exclamation-circle me-2"></i>
                Không thể tải giỏ hàng. Vui lòng thử lại sau.
            </div>
        `;
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
    } catch (error) {
        console.error('Error updating cart item:', error);
        showNotification(error.message || 'Không thể cập nhật số lượng!', 'error');
        // Reload cart to reset values
        loadCart();
    }
}

// Remove cart item
async function removeCartItem(itemId) {
    if (!confirm('Bạn có chắc muốn xóa sản phẩm này khỏi giỏ hàng?')) {
        return;
    }

    try {
        const token = localStorage.getItem('authToken') || localStorage.getItem('token');
        const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
        
        if (!token || !userEmail) {
            window.location.href = '/login';
            return;
        }

        const response = await fetch(`/api/cart/items/${itemId}`, {
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
            throw new Error(error.error || 'Failed to remove cart item');
        }

        showNotification('Đã xóa sản phẩm khỏi giỏ hàng!', 'success');
        
        // Reload cart
        loadCart();
    } catch (error) {
        console.error('Error removing cart item:', error);
        showNotification(error.message || 'Không thể xóa sản phẩm!', 'error');
    }
}

// Clear entire cart
async function clearCart() {
    if (!confirm('Bạn có chắc muốn xóa toàn bộ giỏ hàng?')) {
        return;
    }

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
        
        // Reload cart
        loadCart();
    } catch (error) {
        console.error('Error clearing cart:', error);
        showNotification(error.message || 'Không thể xóa giỏ hàng!', 'error');
    }
}

// Update cart summary
function updateCartSummary(cart) {
    document.getElementById('totalItems').textContent = cart.totalItems || 0;
    document.getElementById('totalPrice').textContent = formatPrice(cart.totalPrice || 0);
    
    // Update cart badge in header if function exists
    if (typeof updateCartBadge === 'function') {
        updateCartBadge();
    }
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
