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

        // Try to use public endpoint first (for VNPay redirect case)
        let response = await fetch(`/api/orders/public/${orderNumber}`);
        
        // If public endpoint fails (404), try authenticated endpoint
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
            const error = await response.json().catch(() => ({}));
            throw new Error(error.message || 'Order not found');
        }

        const order = await response.json();
        console.log('Order loaded successfully:', order);
        console.log('Order status:', order.status);
        console.log('Payment status:', order.paymentStatus);
        console.log('Payment method:', order.paymentMethod);

        // Save current order
        currentOrder = order;

        // Check if order is PENDING_PAYMENT and redirect to payment-pending page
        if (order.status === 'PENDING_PAYMENT' && order.paymentMethod === 'E_WALLET') {
            console.log('Order is pending payment, redirecting to payment-pending page...');
            window.location.href = '/payment-pending/' + orderNumber;
            return;
        }

        // Check if order is CANCELLED (any paymentStatus) for E_WALLET payments
        if (order.paymentMethod === 'E_WALLET' && order.status === 'CANCELLED') {
            console.log('Payment was cancelled/failed, showing cancelled message...');
            
            // Display order details FIRST
            displayOrderDetails(order);
            
            // Then modify the UI to show cancelled message
            displayCancelledPaymentMessage(order);
            
            loadingState.style.display = 'none';
            orderDetails.style.display = 'block';
            return;
        }

        console.log('Displaying normal order details...');
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

function displayCancelledPaymentMessage(order) {
    console.log('=== displayCancelledPaymentMessage called ===', order);

    const statusIcon = document.getElementById('orderStatusIcon');
    if (statusIcon) {
        statusIcon.innerHTML = '<i class="fas fa-times-circle text-danger" style="font-size: 5rem;"></i>';
    }

    const statusTitle = document.getElementById('orderStatusTitle');
    if (statusTitle) {
        statusTitle.textContent = '❌ Thanh toán đã bị hủy';
        statusTitle.classList.remove('space-text');
        statusTitle.classList.add('text-danger');
    }

    const statusMessage = document.getElementById('orderStatusMessage');
    if (statusMessage) {
        statusMessage.innerHTML = 'Đơn hàng của bạn đã bị hủy do thanh toán không thành công.';
        statusMessage.classList.remove('text-muted');
        statusMessage.classList.add('text-danger');
    }

    const orderNumberBox = document.getElementById('orderNumberBox');
    if (orderNumberBox) {
        orderNumberBox.style.background = 'linear-gradient(135deg, #ffebee 0%, #ffcdd2 100%)';
    }

    const orderNumberDisplay = document.getElementById('orderNumberDisplay');
    if (orderNumberDisplay) {
        orderNumberDisplay.classList.remove('text-primary');
        orderNumberDisplay.classList.add('text-danger');
    }

    const statusActions = document.getElementById('orderStatusActions');
    if (statusActions) {
    const cancelReason = getPaymentCancelReason(order.vnpayResponseCode, order.paymentStatus);
        statusActions.innerHTML = `
            <div class="alert alert-danger mt-3" role="alert">
                <i class="fas fa-info-circle me-2"></i>
                <strong>Lý do hủy:</strong> ${cancelReason}
            </div>
            <div class="d-flex justify-content-center flex-wrap gap-2 mt-3">
                <a href="/products" class="btn btn-primary">
                    <i class="fas fa-shopping-bag me-2"></i>Đặt hàng mới
                </a>
                <a href="/orders" class="btn btn-outline-secondary">
                    <i class="fas fa-list me-2"></i>Xem đơn hàng của tôi
                </a>
            </div>
        `;
    }

    console.log('=== displayCancelledPaymentMessage completed ===');
}

function getPaymentCancelReason(responseCode, paymentStatus) {
    const reasons = {
        '24': 'Khách hàng hủy giao dịch',
        '11': 'Đã hết hạn chờ thanh toán',
        '13': 'Nhập sai mật khẩu OTP',
        '51': 'Tài khoản không đủ số dư',
        '65': 'Vượt quá hạn mức giao dịch',
        '75': 'Ngân hàng đang bảo trì',
        '79': 'Nhập sai mật khẩu quá số lần quy định'
    };
    if (responseCode && reasons[responseCode]) {
        return reasons[responseCode];
    }

    if (paymentStatus === 'CANCELLED') {
        return 'Đơn hàng đã được hủy bởi bạn.';
    }

    return 'Thanh toán không thành công. Vui lòng thử lại.';
}

function displayOrderDetails(order) {
    // Reset status header to success state by default
    const statusIcon = document.getElementById('orderStatusIcon');
    if (statusIcon) {
        statusIcon.innerHTML = '<i class="fas fa-check-circle text-success" style="font-size: 5rem;"></i>';
    }

    const statusTitle = document.getElementById('orderStatusTitle');
    if (statusTitle) {
        statusTitle.textContent = '🎉 Tàu đã phóng thành công!';
        statusTitle.classList.add('space-text');
        statusTitle.classList.remove('text-danger');
    }

    const statusMessage = document.getElementById('orderStatusMessage');
    if (statusMessage) {
        statusMessage.innerHTML = 'Cảm ơn phi hành gia đã đặt hàng tại T4M!';
        statusMessage.classList.add('text-muted');
        statusMessage.classList.remove('text-danger');
    }

    const orderNumberBox = document.getElementById('orderNumberBox');
    if (orderNumberBox) {
        orderNumberBox.style.background = 'linear-gradient(135deg, #e3f2fd 0%, #f3e5f5 100%)';
    }

    const orderNumberDisplay = document.getElementById('orderNumberDisplay');
    if (orderNumberDisplay) {
        orderNumberDisplay.classList.add('text-primary');
        orderNumberDisplay.classList.remove('text-danger');
    }

    const statusActions = document.getElementById('orderStatusActions');
    if (statusActions) {
        statusActions.innerHTML = '';
    }

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
    const actionButtons = document.getElementById('actionButtons');
    
    // Show cancel button for PENDING orders with COD payment
    if (order.status === 'PENDING' && order.paymentMethod === 'COD') {
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
    
    // Show "Continue Payment" button for PENDING_PAYMENT E_WALLET orders
    if (order.status === 'PENDING_PAYMENT' && order.paymentMethod === 'E_WALLET') {
        // Add continue payment button
        const continuePaymentBtn = document.createElement('button');
        continuePaymentBtn.className = 'btn btn-primary btn-lg mt-2 mt-md-0';
        continuePaymentBtn.innerHTML = '<i class="fas fa-credit-card me-2"></i>Tiếp tục thanh toán';
        continuePaymentBtn.onclick = () => continuePayment(order.orderNumber, order.totalAmount);
        
        actionButtons.insertBefore(continuePaymentBtn, actionButtons.firstChild);
        
        // Add warning alert
        const warningAlert = document.createElement('div');
        warningAlert.className = 'alert alert-warning mt-3';
        warningAlert.innerHTML = '<i class="fas fa-exclamation-triangle me-2"></i><strong>Chưa thanh toán:</strong> Vui lòng hoàn tất thanh toán để xử lý đơn hàng của bạn.';
        actionButtons.parentElement.insertBefore(warningAlert, actionButtons);
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

async function continuePayment(orderNumber, totalAmount) {
    try {
        const token = localStorage.getItem('authToken') || localStorage.getItem('token');
        const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
        
        if (!token || !userEmail) {
            window.location.href = '/login';
            return;
        }

        // Show loading overlay instead of modal
        const loadingOverlay = document.createElement('div');
        loadingOverlay.id = 'paymentLoadingOverlay';
        loadingOverlay.style.cssText = `
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0,0,0,0.7);
            display: flex;
            align-items: center;
            justify-content: center;
            z-index: 9999;
        `;
        loadingOverlay.innerHTML = `
            <div class="card text-center p-4" style="min-width: 300px;">
                <div class="spinner-border text-primary mx-auto" role="status" style="width: 3rem; height: 3rem;">
                    <span class="visually-hidden">Đang xử lý...</span>
                </div>
                <p class="mt-3 mb-0">Đang tạo liên kết thanh toán...</p>
            </div>
        `;
        document.body.appendChild(loadingOverlay);

        // Call backend to recreate VNPay payment URL
        const response = await fetch('/api/payment/vnpay/create-payment-link', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
                'X-User-Email': userEmail
            },
            body: JSON.stringify({
                orderNumber: orderNumber,
                amount: totalAmount,
                orderInfo: `Thanh toan don hang ${orderNumber}`
            })
        });

        if (!response.ok) {
            throw new Error('Không thể tạo liên kết thanh toán');
        }

        const data = await response.json();
        
        // Redirect to VNPay
        if (data.paymentUrl) {
            window.location.href = data.paymentUrl;
        } else {
            throw new Error('Không nhận được URL thanh toán');
        }

    } catch (error) {
        // Remove loading overlay
        const overlay = document.getElementById('paymentLoadingOverlay');
        if (overlay) {
            overlay.remove();
        }
        
        console.error('Error continuing payment:', error);
        showErrorModal('Không thể tiếp tục thanh toán. Vui lòng thử lại sau!');
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
    document.getElementById('errorMessage').innerHTML = message;
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
