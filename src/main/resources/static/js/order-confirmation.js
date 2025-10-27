// Order confirmation functionality
let currentOrder = null;

document.addEventListener('DOMContentLoaded', function() {
    loadOrderDetails();
});

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

        // Get authentication token
        const token = localStorage.getItem('authToken') || localStorage.getItem('token');
        const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');

        if (!token || !userEmail) {
            // Redirect to login if not authenticated
            window.location.href = '/login';
            return;
        }

        const response = await fetch(`/api/orders/${orderNumber}`, {
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
            const error = await response.json().catch(() => ({}));
            throw new Error(error.message || 'Order not found');
        }

        const order = await response.json();
        console.log('Order loaded successfully:', order);

        // Save current order
        currentOrder = order;

        // Display order details
        displayOrderDetails(order);

        loadingState.style.display = 'none';
        orderDetails.style.display = 'block';

        // Add cancel button if applicable
        addCancelButton(order);

        // Trigger confetti animation
        celebrateOrder();

    } catch (error) {
        console.error('Error loading order:', error);
        console.error('Order number:', pathParts[pathParts.length - 1]);
        loadingState.style.display = 'none';
        errorState.style.display = 'block';
        
        // Show more detailed error message
        const errorMessage = document.querySelector('#errorState .card-body');
        if (errorMessage) {
            errorMessage.innerHTML = `
                <h3 class="text-danger mb-3">
                    <i class="fas fa-exclamation-triangle me-2"></i>
                    Không tìm thấy đơn hàng
                </h3>
                <p class="text-muted mb-4">Đơn hàng không tồn tại hoặc đã bị xóa.</p>
                <p class="text-danger mb-4">Chi tiết lỗi: ${error.message}</p>
                <a href="/orders" class="btn btn-primary me-2">
                    <i class="fas fa-list me-2"></i>Xem danh sách đơn hàng
                </a>
                <a href="/products" class="btn btn-outline-primary">
                    <i class="fas fa-shopping-bag me-2"></i>Tiếp tục mua sắm
                </a>
            `;
        }
    }
}

function displayOrderDetails(order) {
    // Order number
    document.getElementById('orderNumberDisplay').textContent = order.orderNumber;

    // Customer info
    document.getElementById('customerName').textContent = order.customerName;
    document.getElementById('customerEmail').textContent = order.customerEmail;
    document.getElementById('customerPhone').textContent = order.customerPhone;
    document.getElementById('shippingAddress').textContent = order.shippingAddress;

    // Payment method
    const paymentMethodText = getPaymentMethodText(order.paymentMethod);
    document.getElementById('paymentMethod').textContent = paymentMethodText;

    // Order items
    const itemsList = document.getElementById('orderItemsList');
    itemsList.innerHTML = '';

    order.items.forEach(item => {
        const itemDiv = document.createElement('div');
        itemDiv.className = 'order-item d-flex align-items-center mb-3 pb-3 border-bottom';
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
        itemsList.appendChild(itemDiv);
    });

    // Calculate subtotal (items total before discount)
    const itemsSubtotal = order.items.reduce((sum, item) => sum + item.subtotal, 0);
    
    // Order totals
    document.getElementById('orderSubtotal').textContent = formatPrice(itemsSubtotal);
    
    // Display voucher discount if applied
    const voucherRow = document.getElementById('voucherDiscountRow');
    
    // Debug: Log voucher info
    console.log('Voucher Debug:', {
        voucherCode: order.voucherCode,
        voucherDiscount: order.voucherDiscount,
        voucherType: typeof order.voucherDiscount
    });
    
    const hasVoucher = order.voucherCode && 
                       order.voucherCode.trim() !== '' && 
                       order.voucherDiscount && 
                       parseFloat(order.voucherDiscount) > 0;
    
    if (hasVoucher) {
        voucherRow.style.display = 'flex';
        document.getElementById('displayVoucherCode').textContent = order.voucherCode;
        document.getElementById('displayVoucherDiscount').textContent = formatPrice(order.voucherDiscount);
    } else {
        voucherRow.style.display = 'none';
    }
    
    document.getElementById('orderTotal').textContent = formatPrice(order.totalAmount);
}

function getPaymentMethodText(method) {
    const methodMap = {
        'COD': 'Thanh toán khi nhận hàng (COD)',
        'BANK_TRANSFER': 'Chuyển khoản ngân hàng',
        'E_WALLET': 'Ví điện tử',
        'CREDIT_CARD': 'Thẻ tín dụng/ATM'
    };
    return methodMap[method] || method;
}

function celebrateOrder() {
    // Add success animation
    const successIcon = document.querySelector('.success-animation i');
    if (successIcon) {
        successIcon.style.animation = 'scaleIn 0.5s ease-out';
    }

    // Add confetti effect if you want (optional)
    // You can add a confetti library here
}

function formatPrice(price) {
    return new Intl.NumberFormat('vi-VN', { 
        style: 'currency', 
        currency: 'VND' 
    }).format(price);
}

function addCancelButton(order) {
    // Only show cancel button for PENDING orders with COD payment
    if (order.status === 'PENDING' && order.paymentMethod === 'COD') {
        const actionButtons = document.getElementById('actionButtons');
        
        // Add cancel button
        const cancelBtn = document.createElement('button');
        cancelBtn.className = 'btn btn-outline-danger btn-lg mt-2 mt-md-0';
        cancelBtn.innerHTML = '<i class="fas fa-times me-2"></i>Hủy đơn hàng';
        cancelBtn.onclick = () => cancelOrder(order.id, order.orderNumber);
        
        actionButtons.insertBefore(cancelBtn, actionButtons.firstChild);
        
        // Add info alert
        const infoAlert = document.createElement('div');
        infoAlert.className = 'alert alert-warning mt-3';
        infoAlert.innerHTML = '<i class="fas fa-info-circle me-2"></i>Bạn có thể hủy đơn hàng COD trong khi đơn đang chờ xử lý.';
        actionButtons.parentElement.insertBefore(infoAlert, actionButtons);
    }
}

let currentCancelOrderId = null;
let currentCancelOrderNumber = null;

function cancelOrder(orderId, orderNumber) {
    // Store order info
    currentCancelOrderId = orderId;
    currentCancelOrderNumber = orderNumber;
    
    // Update modal content
    document.getElementById('cancelOrderNumber').textContent = orderNumber;
    
    // Show cancel confirmation modal
    const cancelModal = new bootstrap.Modal(document.getElementById('cancelOrderModal'));
    cancelModal.show();
    
    // Setup confirm button handler
    setupCancelConfirmHandler();
}

function setupCancelConfirmHandler() {
    const confirmCancelBtn = document.getElementById('confirmCancelBtn');
    if (!confirmCancelBtn) return;
    
    // Remove old listeners
    const newBtn = confirmCancelBtn.cloneNode(true);
    confirmCancelBtn.parentNode.replaceChild(newBtn, confirmCancelBtn);
    
    // Add new listener
    newBtn.addEventListener('click', async function() {
        // Hide cancel modal
        const cancelModal = bootstrap.Modal.getInstance(document.getElementById('cancelOrderModal'));
        cancelModal.hide();
        
        // Show loading state
        newBtn.disabled = true;
        newBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang xử lý...';
        
        await performCancelOrder();
        
        // Reset button
        newBtn.disabled = false;
        newBtn.innerHTML = '<i class="fas fa-times me-2"></i>Xác nhận hủy';
    });
}

async function performCancelOrder() {
    try {
        const token = localStorage.getItem('authToken') || localStorage.getItem('token');
        const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
        
        if (!token || !userEmail) {
            showErrorModal('Vui lòng đăng nhập để hủy đơn hàng!');
            setTimeout(() => window.location.href = '/login', 2000);
            return;
        }

        const response = await fetch(`/api/orders/${currentCancelOrderId}/cancel`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
                'X-User-Email': userEmail
            }
        });

        if (response.status === 401) {
            showErrorModal('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại!');
            setTimeout(() => window.location.href = '/login', 2000);
            return;
        }

        const data = await response.json();

        if (!response.ok) {
            showErrorModal(data.error || 'Không thể hủy đơn hàng. Vui lòng thử lại!');
            return;
        }

        // Show success modal
        const successModal = new bootstrap.Modal(document.getElementById('successModal'));
        successModal.show();
        
        // Start countdown timer
        startCountdown(5);

    } catch (error) {
        console.error('Error cancelling order:', error);
        showErrorModal('Đã xảy ra lỗi khi hủy đơn hàng. Vui lòng thử lại sau!');
    }
}

function startCountdown(seconds) {
    let timeLeft = seconds;
    const timerElement = document.getElementById('countdownTimer');
    
    const countdown = setInterval(() => {
        timeLeft--;
        if (timerElement) {
            timerElement.textContent = timeLeft;
        }
        
        if (timeLeft <= 0) {
            clearInterval(countdown);
            window.location.href = '/orders';
        }
    }, 1000);
}

function showErrorModal(message) {
    document.getElementById('errorMessage').textContent = message;
    const errorModal = new bootstrap.Modal(document.getElementById('errorModal'));
    errorModal.show();
}

// Add CSS animation
const style = document.createElement('style');
style.textContent = `
    @keyframes scaleIn {
        0% {
            transform: scale(0);
            opacity: 0;
        }
        50% {
            transform: scale(1.2);
        }
        100% {
            transform: scale(1);
            opacity: 1;
        }
    }
`;
document.head.appendChild(style);
