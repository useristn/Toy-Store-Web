// Checkout functionality
document.addEventListener('DOMContentLoaded', function() {
    loadCheckoutData();
    setupFormValidation();
    setupCheckoutButton();
    setupPaymentMethodListeners();
});

// Setup payment method change listeners
function setupPaymentMethodListeners() {
    const bankTransferRadio = document.getElementById('bankTransfer');
    const creditCardRadio = document.getElementById('creditCard');
    
    if (bankTransferRadio) {
        bankTransferRadio.addEventListener('change', function() {
            if (this.checked) {
                showComingSoonModal('Chuyển khoản ngân hàng');
                // Switch back to COD
                document.getElementById('codPayment').checked = true;
            }
        });
    }
    
    if (creditCardRadio) {
        creditCardRadio.addEventListener('change', function() {
            if (this.checked) {
                showComingSoonModal('Thanh toán thẻ tín dụng/ATM');
                // Switch back to COD
                document.getElementById('codPayment').checked = true;
            }
        });
    }
}

// Show coming soon modal
function showComingSoonModal(paymentMethod) {
    const modalHtml = `
        <div class="modal fade" id="comingSoonModal" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content">
                    <div class="modal-header bg-warning text-dark">
                        <h5 class="modal-title">
                            <i class="fas fa-tools me-2"></i>Chức năng đang hoàn thiện
                        </h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body text-center py-4">
                        <i class="fas fa-hard-hat fa-4x text-warning mb-3"></i>
                        <h5 class="mb-3">Tính năng "${paymentMethod}" đang được phát triển</h5>
                        <p class="text-muted">
                            Chúng tôi đang hoàn thiện tính năng này. 
                            Hiện tại bạn có thể sử dụng các phương thức thanh toán khác:
                        </p>
                        <div class="alert alert-info mt-3">
                            <i class="fas fa-check-circle me-2"></i>Thanh toán khi nhận hàng<br>
                            <i class="fas fa-check-circle me-2"></i>Ví điện tử VNPay
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-primary" data-bs-dismiss="modal">
                            <i class="fas fa-check me-2"></i>Đã hiểu
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    // Remove existing modal if any
    const existingModal = document.getElementById('comingSoonModal');
    if (existingModal) {
        existingModal.remove();
    }
    
    // Add modal to body
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    
    // Show modal
    const modal = new bootstrap.Modal(document.getElementById('comingSoonModal'));
    modal.show();
    
    // Clean up after modal is hidden
    document.getElementById('comingSoonModal').addEventListener('hidden.bs.modal', function() {
        this.remove();
    });
}

// Load cart data for checkout
async function loadCheckoutData() {
    const summaryLoading = document.getElementById('summaryLoading');
    const orderItemsList = document.getElementById('orderItemsList');

    try {
        const token = localStorage.getItem('authToken') || localStorage.getItem('token');
        const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
        
        if (!token || !userEmail) {
            window.location.href = '/login';
            return;
        }

        // Load user profile to pre-fill form
        await loadUserProfile(userEmail, token);

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

        if (!cart.items || cart.items.length === 0) {
            showNotification('Giỏ hàng trống! Đang chuyển về trang sản phẩm...', 'warning');
            setTimeout(() => {
                window.location.href = '/products';
            }, 2000);
            return;
        }

        // Display cart items
        displayCheckoutItems(cart.items);
        updateCheckoutSummary(cart);

        summaryLoading.style.display = 'none';
        orderItemsList.style.display = 'block';

    } catch (error) {
        console.error('Error loading checkout data:', error);
        summaryLoading.innerHTML = `
            <div class="alert alert-danger">
                <i class="fas fa-exclamation-circle me-2"></i>
                Không thể tải thông tin giỏ hàng. Vui lòng thử lại.
            </div>
        `;
    }
}

// Load user profile to pre-fill form
async function loadUserProfile(email, token) {
    try {
        const response = await fetch(`/api/auth/user?email=${encodeURIComponent(email)}`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (response.ok) {
            const profile = await response.json();
            
            // Pre-fill form
            document.getElementById('customerName').value = profile.name || '';
            document.getElementById('customerEmail').value = profile.email || email;
            document.getElementById('customerPhone').value = profile.phone || '';
            document.getElementById('shippingAddress').value = profile.address || '';
        }
    } catch (error) {
        console.error('Error loading profile:', error);
    }
}

// Display checkout items
function displayCheckoutItems(items) {
    const container = document.getElementById('itemsContainer');
    container.innerHTML = '';

    items.forEach(item => {
        const itemDiv = document.createElement('div');
        itemDiv.className = 'checkout-item d-flex align-items-center mb-3 pb-3 border-bottom';
        itemDiv.innerHTML = `
            <img src="${item.productImageUrl || 'https://via.placeholder.com/80'}" 
                 alt="${item.productName}" 
                 class="rounded me-3" 
                 style="width: 60px; height: 60px; object-fit: cover;">
            <div class="flex-grow-1">
                <h6 class="mb-1">${item.productName}</h6>
                <small class="text-muted">Số lượng: ${item.quantity}</small>
            </div>
            <div class="text-end">
                <p class="mb-0 fw-bold text-danger">${formatPrice(item.subtotal)}</p>
                <small class="text-muted">${formatPrice(item.price)} x ${item.quantity}</small>
            </div>
        `;
        container.appendChild(itemDiv);
    });
}

// Update checkout summary
function updateCheckoutSummary(cart) {
    const subtotal = cart.totalPrice || 0;
    document.getElementById('subtotal').textContent = formatPrice(subtotal);
    
    // Load voucher info from localStorage
    const voucherCode = localStorage.getItem('voucherCode');
    const voucherDiscount = parseFloat(localStorage.getItem('voucherDiscount')) || 0;
    
    if (voucherCode && voucherDiscount > 0) {
        // Display voucher
        document.getElementById('voucherDisplay').style.display = 'block';
        document.getElementById('displayVoucherCode').textContent = voucherCode;
        document.getElementById('displayVoucherDiscount').textContent = '- ' + formatPrice(voucherDiscount);
        
        // Calculate final total
        const finalTotal = Math.max(0, subtotal - voucherDiscount);
        document.getElementById('totalAmount').textContent = formatPrice(finalTotal);
    } else {
        // Hide voucher display
        document.getElementById('voucherDisplay').style.display = 'none';
        document.getElementById('totalAmount').textContent = formatPrice(subtotal);
    }
}

// Setup form validation
function setupFormValidation() {
    const form = document.getElementById('checkoutForm');
    
    // Add Bootstrap validation classes
    const inputs = form.querySelectorAll('input[required], textarea[required]');
    inputs.forEach(input => {
        input.addEventListener('blur', function() {
            if (this.checkValidity()) {
                this.classList.remove('is-invalid');
                this.classList.add('is-valid');
            } else {
                this.classList.remove('is-valid');
                this.classList.add('is-invalid');
            }
        });
    });

    // Phone validation
    const phoneInput = document.getElementById('customerPhone');
    phoneInput.addEventListener('input', function() {
        this.value = this.value.replace(/[^0-9]/g, '');
    });
}

// Setup checkout button
function setupCheckoutButton() {
    const placeOrderBtn = document.getElementById('placeOrderBtn');
    
    placeOrderBtn.addEventListener('click', async function() {
        const form = document.getElementById('checkoutForm');
        
        // Validate form
        if (!form.checkValidity()) {
            form.classList.add('was-validated');
            showNotification('Vui lòng điền đầy đủ thông tin!', 'warning');
            
            // Scroll to first invalid field
            const firstInvalid = form.querySelector(':invalid');
            if (firstInvalid) {
                firstInvalid.scrollIntoView({ behavior: 'smooth', block: 'center' });
                firstInvalid.focus();
            }
            return;
        }

        // Get form data
        const checkoutData = {
            customerName: document.getElementById('customerName').value.trim(),
            customerEmail: document.getElementById('customerEmail').value.trim(),
            customerPhone: document.getElementById('customerPhone').value.trim(),
            shippingAddress: document.getElementById('shippingAddress').value.trim(),
            paymentMethod: document.querySelector('input[name="paymentMethod"]:checked').value,
            notes: document.getElementById('notes').value.trim(),
            voucherCode: localStorage.getItem('voucherCode') || null
        };

        // Disable button and show loading
        placeOrderBtn.disabled = true;
        placeOrderBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang xử lý...';

        try {
            const token = localStorage.getItem('authToken') || localStorage.getItem('token');
            const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
            
            const response = await fetch('/api/orders/checkout', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json',
                    'X-User-Email': userEmail
                },
                body: JSON.stringify(checkoutData)
            });

            if (response.status === 401) {
                showNotification('Phiên đăng nhập đã hết hạn!', 'warning');
                setTimeout(() => {
                    window.location.href = '/login';
                }, 1500);
                return;
            }

            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.error || 'Đặt hàng thất bại');
            }

            const result = await response.json();
            console.log('Checkout result:', result);
            
            // Check if need to redirect to VNPay
            if (result.redirectToPayment && result.paymentUrl) {
                const order = result.order;
                console.log('Redirecting to VNPay for payment:', result.paymentUrl);
                
                // Clear voucher from localStorage
                localStorage.removeItem('voucherCode');
                localStorage.removeItem('voucherDiscount');
                localStorage.removeItem('cartSubtotal');
                
                // Show message and redirect to VNPay
                showNotification('Đang chuyển đến cổng thanh toán VNPay...', 'info');
                
                setTimeout(() => {
                    window.location.href = result.paymentUrl;
                }, 1000);
                return;
            }
            
            // For COD payment
            const order = result.order || result;
            console.log('Order created successfully:', order);
            console.log('Order number:', order.orderNumber);
            
            if (!order.orderNumber) {
                throw new Error('Order number not returned from server');
            }
            
            // Clear voucher from localStorage after successful order
            localStorage.removeItem('voucherCode');
            localStorage.removeItem('voucherDiscount');
            localStorage.removeItem('cartSubtotal');
            
            // Show success and redirect
            showNotification('Đặt hàng thành công! Đang chuyển hướng...', 'success');
            
            setTimeout(() => {
                console.log('Redirecting to:', `/order-confirmation/${order.orderNumber}`);
                window.location.href = `/order-confirmation/${order.orderNumber}`;
            }, 1500);

        } catch (error) {
            console.error('Error placing order:', error);
            showNotification(error.message || 'Đặt hàng thất bại. Vui lòng thử lại!', 'danger');
            
            // Re-enable button
            placeOrderBtn.disabled = false;
            placeOrderBtn.innerHTML = '<i class="fas fa-rocket me-2"></i>Phóng tàu ngay! 🚀';
        }
    });
}

// Format price
function formatPrice(price) {
    return new Intl.NumberFormat('vi-VN', { 
        style: 'currency', 
        currency: 'VND' 
    }).format(price);
}

// Show notification
function showNotification(message, type = 'success') {
    const toastDiv = document.createElement('div');
    toastDiv.className = `alert alert-${type} position-fixed top-0 start-50 translate-middle-x mt-3`;
    toastDiv.style.zIndex = '9999';
    toastDiv.style.minWidth = '300px';
    toastDiv.innerHTML = `
        <i class="fas fa-${type === 'success' ? 'check-circle' : type === 'warning' ? 'exclamation-triangle' : 'exclamation-circle'} me-2"></i>
        ${message}
    `;
    document.body.appendChild(toastDiv);
    
    setTimeout(() => {
        toastDiv.style.opacity = '0';
        toastDiv.style.transition = 'opacity 0.5s';
        setTimeout(() => toastDiv.remove(), 500);
    }, 3000);
}
