// Payment Pending functionality
let currentOrder = null;
let countdownInterval = null;

document.addEventListener('DOMContentLoaded', function() {
    loadOrderDetails();
    setupEventListeners();
});

function setupEventListeners() {
    const continuePaymentBtn = document.getElementById('continuePaymentBtn');
    const cancelOrderBtn = document.getElementById('cancelOrderBtn');
    const confirmCancelBtn = document.getElementById('confirmCancelBtn');

    if (continuePaymentBtn) {
        continuePaymentBtn.addEventListener('click', handleContinuePayment);
    }

    if (cancelOrderBtn) {
        cancelOrderBtn.addEventListener('click', showCancelModal);
    }

    if (confirmCancelBtn) {
        confirmCancelBtn.addEventListener('click', handleCancelOrder);
    }
}

async function loadOrderDetails() {
    const loadingState = document.getElementById('loadingState');
    const orderDetails = document.getElementById('orderDetails');
    const errorState = document.getElementById('errorState');

    try {
        // Get order number from URL
        const pathParts = window.location.pathname.split('/');
        const orderNumber = pathParts[pathParts.length - 1];

        if (!orderNumber) {
            throw new Error('Order number not found');
        }

        // Try to use public endpoint first
        let response = await fetch(`/api/orders/public/${orderNumber}`);
        
        // If public endpoint fails, try authenticated endpoint
        if (response.status === 404) {
            const token = localStorage.getItem('authToken') || localStorage.getItem('token');
            const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');

            if (!token || !userEmail) {
                window.location.href = '/login';
                return;
            }

            response = await fetch(`/api/orders/${orderNumber}`, {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json',
                    'X-User-Email': userEmail
                }
            });
        }

        if (response.status === 401) {
            window.location.href = '/login';
            return;
        }

        if (!response.ok) {
            throw new Error('Order not found');
        }

        const order = await response.json();
        console.log('Order loaded:', order);

        // Check if order is actually pending payment
        if (order.status !== 'PENDING_PAYMENT') {
            // Redirect to order confirmation if order is not pending payment
            window.location.href = '/order-confirmation/' + orderNumber;
            return;
        }

        // Save current order
        currentOrder = order;

        // Display order details
        displayOrderDetails(order);

        // Start countdown timer
        startCountdown(order.createdAt);

        loadingState.style.display = 'none';
        orderDetails.style.display = 'block';

    } catch (error) {
        console.error('Error loading order:', error);
        loadingState.style.display = 'none';
        errorState.style.display = 'block';
    }
}

function displayOrderDetails(order) {
    // Order number
    const orderNumberEl = document.getElementById('orderNumberDisplay');
    if (orderNumberEl) {
        orderNumberEl.textContent = order.orderNumber || '---';
    }

    // Customer info
    const customerNameEl = document.getElementById('customerName');
    if (customerNameEl) {
        customerNameEl.textContent = order.customerName || '---';
    }

    const customerPhoneEl = document.getElementById('customerPhone');
    if (customerPhoneEl) {
        customerPhoneEl.textContent = order.customerPhone || '---';
    }

    const customerEmailEl = document.getElementById('customerEmail');
    if (customerEmailEl) {
        customerEmailEl.textContent = order.customerEmail || '---';
    }

    const shippingAddressEl = document.getElementById('shippingAddress');
    if (shippingAddressEl) {
        shippingAddressEl.textContent = order.shippingAddress || '---';
    }

    // Payment method
    const paymentMethodMap = {
        'COD': 'Thanh toán khi nhận hàng',
        'E_WALLET': 'Ví điện tử VNPay',
        'BANK_TRANSFER': 'Chuyển khoản ngân hàng',
        'CREDIT_CARD': 'Thẻ tín dụng'
    };
    
    const paymentMethodEl = document.getElementById('paymentMethod');
    if (paymentMethodEl) {
        paymentMethodEl.textContent = paymentMethodMap[order.paymentMethod] || order.paymentMethod || '---';
    }

    // Order date
    if (order.createdAt) {
        const orderDate = new Date(order.createdAt);
        const orderDateEl = document.getElementById('orderDate');
        if (orderDateEl) {
            orderDateEl.textContent = formatDate(orderDate);
        }
    }

    // Order items
    if (order.items && Array.isArray(order.items)) {
        displayOrderItems(order.items);
    }

    // Calculate subtotal
    const subtotal = order.items ? order.items.reduce((sum, item) => sum + (item.price * item.quantity), 0) : 0;
    const orderSubtotalEl = document.getElementById('orderSubtotal');
    if (orderSubtotalEl) {
        orderSubtotalEl.textContent = formatCurrency(subtotal);
    }

    // Voucher discount
    if (order.voucherDiscount && order.voucherDiscount > 0) {
        const voucherDiscountRow = document.getElementById('voucherDiscountRow');
        if (voucherDiscountRow) {
            voucherDiscountRow.style.display = 'flex';
        }
        
        const displayVoucherCode = document.getElementById('displayVoucherCode');
        if (displayVoucherCode) {
            displayVoucherCode.textContent = order.voucherCode || '';
        }
        
        const displayVoucherDiscount = document.getElementById('displayVoucherDiscount');
        if (displayVoucherDiscount) {
            displayVoucherDiscount.textContent = formatCurrency(order.voucherDiscount);
        }
    }

    // Total
    const orderTotalEl = document.getElementById('orderTotal');
    if (orderTotalEl) {
        orderTotalEl.textContent = formatCurrency(order.totalAmount || 0);
    }
}

function displayOrderItems(items) {
    const container = document.getElementById('orderItemsList');
    container.innerHTML = '';

    items.forEach(item => {
        const itemDiv = document.createElement('div');
        itemDiv.className = 'order-item d-flex align-items-center mb-3 pb-3 border-bottom';
        itemDiv.innerHTML = `
            <img src="${item.productImageUrl || '/images/placeholder.png'}" 
                 alt="${item.productName}" 
                 class="rounded me-3" 
                 style="width: 80px; height: 80px; object-fit: cover;">
            <div class="flex-grow-1">
                <h6 class="mb-1">${item.productName}</h6>
                <p class="text-muted mb-0 small">Số lượng: ${item.quantity}</p>
            </div>
            <div class="text-end">
                <p class="mb-0 fw-bold">${formatCurrency(item.price * item.quantity)}</p>
                <p class="text-muted mb-0 small">${formatCurrency(item.price)} x ${item.quantity}</p>
            </div>
        `;
        container.appendChild(itemDiv);
    });
}

function startCountdown(createdAt) {
    if (!createdAt) {
        console.error('Created date not provided');
        return;
    }

    const createdTime = new Date(createdAt);
    
    // Validate date
    if (isNaN(createdTime.getTime())) {
        console.error('Invalid created date:', createdAt);
        return;
    }

    const expiryTime = new Date(createdTime.getTime() + 15 * 60 * 1000); // 15 minutes

    function updateCountdown() {
        const now = new Date();
        const timeLeft = expiryTime - now;

        if (timeLeft <= 0) {
            const timeRemainingEl = document.getElementById('timeRemaining');
            const continuePaymentBtn = document.getElementById('continuePaymentBtn');
            
            if (timeRemainingEl) {
                timeRemainingEl.textContent = 'Đã hết hạn';
                timeRemainingEl.classList.add('text-danger');
            }
            
            if (continuePaymentBtn) {
                continuePaymentBtn.disabled = true;
                continuePaymentBtn.classList.add('disabled');
                continuePaymentBtn.innerHTML = '<i class="fas fa-clock me-2"></i>Đã hết hạn thanh toán';
            }
            
            clearInterval(countdownInterval);
            return;
        }

        const minutes = Math.floor(timeLeft / 60000);
        const seconds = Math.floor((timeLeft % 60000) / 1000);
        
        const timeRemainingEl = document.getElementById('timeRemaining');
        if (timeRemainingEl) {
            timeRemainingEl.textContent = `${minutes} phút ${seconds} giây`;
        }
    }

    updateCountdown();
    countdownInterval = setInterval(updateCountdown, 1000);
}

async function handleContinuePayment() {
    if (!currentOrder) {
        showNotification('Không tìm thấy thông tin đơn hàng', 'danger');
        return;
    }

    const btn = document.getElementById('continuePaymentBtn');
    if (!btn) return;
    
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang xử lý...';

    try {
        const token = localStorage.getItem('authToken') || localStorage.getItem('token');
        const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');

        if (!token || !userEmail) {
            showNotification('Phiên đăng nhập đã hết hạn!', 'warning');
            setTimeout(() => {
                window.location.href = '/login';
            }, 1500);
            return;
        }

        const response = await fetch(`/api/payment/vnpay/create-url/${currentOrder.orderNumber}`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
                'X-User-Email': userEmail
            }
        });

        if (response.status === 401) {
            showNotification('Phiên đăng nhập đã hết hạn!', 'warning');
            setTimeout(() => {
                window.location.href = '/login';
            }, 1500);
            return;
        }

        if (!response.ok) {
            throw new Error('Không thể tạo URL thanh toán');
        }

        const result = await response.json();
        
        if (result.paymentUrl) {
            showNotification('Đang chuyển đến cổng thanh toán VNPay...', 'info');
            setTimeout(() => {
                window.location.href = result.paymentUrl;
            }, 1000);
        } else {
            throw new Error('Không nhận được URL thanh toán');
        }

    } catch (error) {
        console.error('Error creating payment URL:', error);
        showNotification('Có lỗi xảy ra: ' + error.message, 'danger');
        btn.disabled = false;
        btn.innerHTML = '<i class="fas fa-credit-card me-2"></i>Tiếp tục thanh toán';
    }
}

function showCancelModal() {
    const modal = new bootstrap.Modal(document.getElementById('cancelConfirmModal'));
    modal.show();
}

async function handleCancelOrder() {
    if (!currentOrder) {
        showNotification('Không tìm thấy thông tin đơn hàng', 'danger');
        return;
    }

    const btn = document.getElementById('confirmCancelBtn');
    if (!btn) return;
    
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang hủy...';

    try {
        const token = localStorage.getItem('authToken') || localStorage.getItem('token');
        const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');

        if (!token || !userEmail) {
            showNotification('Phiên đăng nhập đã hết hạn!', 'warning');
            setTimeout(() => {
                window.location.href = '/login';
            }, 1500);
            return;
        }

        const response = await fetch(`/api/orders/${currentOrder.orderNumber}/cancel`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
                'X-User-Email': userEmail
            }
        });

        if (response.status === 401) {
            showNotification('Phiên đăng nhập đã hết hạn!', 'warning');
            setTimeout(() => {
                window.location.href = '/login';
            }, 1500);
            return;
        }

        if (!response.ok) {
            throw new Error('Không thể hủy đơn hàng');
        }

        showNotification('Đơn hàng đã được hủy thành công!', 'success');
        
        // Hide modal
        const modal = bootstrap.Modal.getInstance(document.getElementById('cancelConfirmModal'));
        modal.hide();

        // Redirect after 2 seconds
        setTimeout(() => {
            window.location.href = '/orders';
        }, 2000);

    } catch (error) {
        console.error('Error canceling order:', error);
        showNotification('Có lỗi xảy ra: ' + error.message, 'danger');
        btn.disabled = false;
        btn.innerHTML = '<i class="fas fa-times me-2"></i>Xác nhận hủy';
    }
}

function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(amount);
}

function formatDate(date) {
    return new Intl.DateTimeFormat('vi-VN', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    }).format(date);
}

function showNotification(message, type = 'info') {
    // Create toast element
    const toastHtml = `
        <div class="toast align-items-center text-white bg-${type} border-0" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body">
                    ${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
        </div>
    `;

    // Add to toast container
    let toastContainer = document.querySelector('.toast-container');
    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3';
        document.body.appendChild(toastContainer);
    }

    toastContainer.innerHTML = toastHtml;
    const toastElement = toastContainer.querySelector('.toast');
    const toast = new bootstrap.Toast(toastElement);
    toast.show();
}

// Cleanup on page unload
window.addEventListener('beforeunload', function() {
    if (countdownInterval) {
        clearInterval(countdownInterval);
    }
});
