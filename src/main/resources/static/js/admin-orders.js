let currentPage = 0;
let currentStatus = '';
let currentSearch = '';
const pageSize = 20;

document.addEventListener('DOMContentLoaded', function() {
    if (!checkAdminAuth()) {
        return; // Stop execution if not authenticated
    }
    
    // Check URL parameters for auto-filter
    const urlParams = new URLSearchParams(window.location.search);
    const statusParam = urlParams.get('status');
    if (statusParam) {
        currentStatus = statusParam;
        // Find and highlight the corresponding status card
        const statusCards = document.querySelectorAll('.status-card');
        statusCards.forEach(card => {
            card.classList.remove('border-primary', 'border-3', 'active');
            if (card.dataset.status === statusParam) {
                card.classList.add('border-primary', 'border-3', 'active');
            }
        });
    }
    
    loadOrderStats();
    loadOrders();
});

function checkAdminAuth() {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    const userRole = localStorage.getItem('userRole');
    
    if (!token || !userEmail) {
        showToast('Vui lòng đăng nhập để truy cập!', 'warning');
        setTimeout(() => {
            window.location.href = '/login?error=unauthorized';
        }, 1500);
        return false;
    }
    
    // Check admin role
    if (!userRole || !userRole.includes('ADMIN')) {
        showToast('Bạn không có quyền truy cập trang này!', 'danger');
        setTimeout(() => {
            window.location.href = '/login?error=access_denied';
        }, 1500);
        return false;
    }
    
    return true;
}

async function loadOrderStats() {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    try {
        const response = await fetch('/api/admin/orders/stats', {
            headers: {
                'Authorization': `Bearer ${token}`,
                'X-User-Email': userEmail
            }
        });
        
        if (response.ok) {
            const stats = await response.json();
            document.getElementById('totalOrders').textContent = stats.total || 0;
            document.getElementById('pendingOrders').textContent = stats.pending || 0;
            document.getElementById('processingOrders').textContent = stats.processing || 0;
            document.getElementById('shippedOrders').textContent = stats.shipped || 0;
            document.getElementById('deliveredOrders').textContent = stats.delivered || 0;
            document.getElementById('failedOrders').textContent = stats.failed || 0;
            document.getElementById('cancelledOrders').textContent = stats.cancelled || 0;
        }
    } catch (error) {
        console.error('Error loading stats:', error);
    }
}

async function loadOrders() {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    const tbody = document.getElementById('ordersTableBody');
    tbody.innerHTML = `
        <tr>
            <td colspan="7" class="text-center py-5">
                <div class="spinner-border text-primary" role="status"></div>
                <p class="mt-3 text-muted">Đang tải...</p>
            </td>
        </tr>
    `;

    try {
        let url = `/api/admin/orders?page=${currentPage}&size=${pageSize}`;
        
        if (currentStatus) {
            url += `&status=${currentStatus}`;
        }
        
        if (currentStatus) {
            url += `&status=${currentStatus}`;
        }

        const response = await fetch(url, {
            headers: {
                'Authorization': `Bearer ${token}`,
                'X-User-Email': userEmail
            }
        });

        if (!response.ok) {
            throw new Error('Cannot load orders');
        }

        const data = await response.json();
        displayOrders(data);
        updatePagination(data);

    } catch (error) {
        console.error('Error loading orders:', error);
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="text-center py-5 text-danger">
                    <i class="fas fa-exclamation-triangle fa-3x mb-3"></i>
                    <p>Không thể tải dữ liệu!</p>
                </td>
            </tr>
        `;
        showToast('Không thể tải danh sách đơn hàng!', 'danger');
    }
}

function displayOrders(data) {
    const tbody = document.getElementById('ordersTableBody');
    
    if (data.content.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="9" class="text-center py-5">
                    <i class="fas fa-shopping-cart fa-3x text-muted mb-3"></i>
                    <p class="text-muted">Không có đơn hàng nào</p>
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = data.content.map(order => {
        const statusBadge = getStatusBadge(order.status);
        const items = order.items || [];
        const itemsHtml = items.slice(0, 2).map(item => `
            <div class="order-item">
                <img src="${item.productImageUrl || 'https://via.placeholder.com/40'}" 
                     alt="${item.productName || 'Product'}"
                     onerror="this.src='https://via.placeholder.com/40'">
                <span>${item.productName || 'N/A'} (x${item.quantity})</span>
            </div>
        `).join('');
        const moreItems = items.length > 2 ? `<small class="text-muted">+${items.length - 2} sản phẩm khác</small>` : '';
        
        // Payment method display
        const paymentMethodText = getPaymentMethodText(order.paymentMethod);
        
        // Payment status badge (pass orderStatus for COD logic)
        const paymentStatusBadge = getPaymentStatusBadge(order.paymentStatus, order.paymentMethod, order.status);
        
        // Add special class for delivered orders
        const rowClass = order.status === 'DELIVERED' ? 'order-delivered' : '';
        
        return `
            <tr class="${rowClass}">
                <td>
                    <span class="order-id" onclick="viewOrderDetail(${order.id})">#${order.id}</span>
                </td>
                <td>
                    <div class="customer-info">
                        <span class="customer-name">${order.customerName || order.user?.fullName || 'N/A'}</span>
                        <span class="customer-email">${order.customerEmail || order.user?.email || 'N/A'}</span>
                    </div>
                </td>
                <td>
                    <div class="order-items">
                        ${itemsHtml}
                        ${moreItems}
                    </div>
                </td>
                <td><strong>${formatPrice(order.totalAmount)}</strong></td>
                <td><span class="badge bg-secondary">${paymentMethodText}</span></td>
                <td>${paymentStatusBadge}</td>
                <td>
                    ${getOrderStatusControl(order)}
                </td>
                <td>${formatDate(order.createdAt)}</td>
                <td>
                    <div class="btn-group">
                        <button class="btn btn-sm btn-outline-primary" onclick="viewOrderDetail(${order.id})" title="Xem chi tiết">
                            <i class="fas fa-eye"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function getStatusBadge(status) {
    const badges = {
        'PENDING_PAYMENT': '<span class="badge bg-warning"><i class="fas fa-credit-card me-1"></i>Chờ thanh toán</span>',
        'PENDING': '<span class="badge bg-warning"><i class="fas fa-clock me-1"></i>Chờ xử lý</span>',
        'PROCESSING': '<span class="badge bg-info"><i class="fas fa-cogs me-1"></i>Đang xử lý</span>',
        'SHIPPING': '<span class="badge bg-primary"><i class="fas fa-shipping-fast me-1"></i>Đang giao</span>',
        'DELIVERED': '<span class="badge bg-success badge-delivered"><i class="fas fa-check-circle me-1"></i>Giao thành công</span>',
        'FAILED': '<span class="badge bg-warning text-dark"><i class="fas fa-exclamation-triangle me-1"></i>Giao thất bại</span>',
        'CANCELLED': '<span class="badge bg-danger"><i class="fas fa-times-circle me-1"></i>Đơn bị hủy</span>'
    };
    return badges[status] || '<span class="badge bg-secondary">N/A</span>';
}

function getStatusClass(status) {
    const classes = {
        'PENDING_PAYMENT': 'bg-warning',
        'PENDING': 'bg-warning',
        'PROCESSING': 'bg-info',
        'SHIPPING': 'bg-primary',
        'DELIVERED': 'bg-success',
        'FAILED': 'bg-warning',
        'CANCELLED': 'bg-danger'
    };
    return classes[status] || '';
}

function getOrderStatusControl(order) {
    const status = order.status;
    
    // If order is in status that admin can't control, show badge only
    if (status === 'SHIPPING' || status === 'DELIVERED' || status === 'FAILED' || status === 'CANCELLED') {
        return getStatusBadge(status);
    }
    
    // Admin can only control PENDING and PROCESSING
    return `
        <select class="status-select ${getStatusClass(status)}" 
                onchange="updateOrderStatus(${order.id}, this.value)">
            <option value="PENDING" ${status === 'PENDING' ? 'selected' : ''}>Chờ xử lý</option>
            <option value="PROCESSING" ${status === 'PROCESSING' ? 'selected' : ''}>Đang xử lý</option>
        </select>
    `;
}

function filterByStatus(status, element) {
    currentStatus = status;
    currentPage = 0;
    
    // Remove active states from all cards
    document.querySelectorAll('.stat-card').forEach(card => {
        card.classList.remove('border-primary', 'border-3', 'active');
    });
    
    // Add active state to clicked card
    if (element) {
        element.classList.add('border-primary', 'border-3', 'active');
    }
    
    loadOrders();
}

function clearFilters() {
    currentStatus = '';
    currentSearch = '';
    currentPage = 0;
    
    // Reset all stat cards
    document.querySelectorAll('.stat-card').forEach(card => {
        card.classList.remove('border-primary', 'border-3', 'active');
    });
    
    // Set "Tất cả" as active
    const allCard = document.getElementById('stat-all');
    if (allCard) {
        allCard.classList.add('border-primary', 'border-3', 'active');
    }
    
    loadOrders();
    showToast('Đã xóa bộ lọc', 'info');
}

function searchOrders() {
    currentSearch = document.getElementById('searchInput').value.trim();
    currentPage = 0;
    loadOrders();
}

async function updateOrderStatus(orderId, newStatus) {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    if (!confirm(`Bạn có chắc muốn cập nhật trạng thái đơn hàng #${orderId} thành "${getStatusText(newStatus)}"?`)) {
        loadOrders(); // Reload to reset select
        return;
    }
    
    try {
        const response = await fetch(`/api/admin/orders/${orderId}/status`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${token}`,
                'X-User-Email': userEmail,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ status: newStatus })
        });
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Cannot update order status');
        }
        
        showToast('Cập nhật trạng thái đơn hàng thành công!', 'success');
        loadOrderStats();
        loadOrders();
        
    } catch (error) {
        console.error('Error updating order status:', error);
        showToast(error.message || 'Không thể cập nhật trạng thái!', 'danger');
        loadOrders(); // Reload to reset select
    }
}

async function viewOrderDetail(orderId) {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    try {
        const response = await fetch(`/api/admin/orders/${orderId}`, {
            headers: {
                'Authorization': `Bearer ${token}`,
                'X-User-Email': userEmail
            }
        });
        
        if (!response.ok) throw new Error('Cannot load order detail');
        
        const order = await response.json();
        displayOrderDetail(order);
        
        const modal = new bootstrap.Modal(document.getElementById('orderDetailModal'));
        modal.show();
        
    } catch (error) {
        console.error('Error loading order detail:', error);
        showToast('Không thể tải chi tiết đơn hàng!', 'danger');
    }
}

function displayOrderDetail(order) {
    document.getElementById('modalOrderId').textContent = `#${order.id}`;
    
    const content = document.getElementById('orderDetailContent');
    const items = order.items || [];
    
    content.innerHTML = `
        <!-- Order Info -->
        <div class="order-detail-section">
            <h6><i class="fas fa-info-circle"></i> Thông tin đơn hàng</h6>
            <div class="detail-row">
                <span class="detail-label">Mã đơn hàng:</span>
                <span class="detail-value">#${order.id}</span>
            </div>
            <div class="detail-row">
                <span class="detail-label">Trạng thái:</span>
                <span class="detail-value">${getStatusBadge(order.status)}</span>
            </div>
            <div class="detail-row">
                <span class="detail-label">Ngày đặt:</span>
                <span class="detail-value">${formatDateTime(order.createdAt)}</span>
            </div>
            <div class="detail-row">
                <span class="detail-label">Cập nhật lần cuối:</span>
                <span class="detail-value">${formatDateTime(order.updatedAt)}</span>
            </div>
        </div>
        
        <!-- Customer Info -->
        <div class="order-detail-section">
            <h6><i class="fas fa-user"></i> Thông tin khách hàng</h6>
            <div class="detail-row">
                <span class="detail-label">Họ tên:</span>
                <span class="detail-value">${order.customerName || order.user?.fullName || 'N/A'}</span>
            </div>
            <div class="detail-row">
                <span class="detail-label">Email:</span>
                <span class="detail-value">${order.customerEmail || order.user?.email || 'N/A'}</span>
            </div>
            <div class="detail-row">
                <span class="detail-label">Số điện thoại:</span>
                <span class="detail-value">${order.shippingPhone || 'N/A'}</span>
            </div>
            <div class="detail-row">
                <span class="detail-label">Địa chỉ giao hàng:</span>
                <span class="detail-value">${order.shippingAddress || 'N/A'}</span>
            </div>
        </div>
        
        <!-- Order Items -->
        <div class="order-detail-section">
            <h6><i class="fas fa-shopping-bag"></i> Sản phẩm (${items.length})</h6>
            <div class="table-responsive">
                <table class="table table-sm">
                    <thead>
                        <tr>
                            <th>Sản phẩm</th>
                            <th class="text-center">Số lượng</th>
                            <th class="text-end">Đơn giá</th>
                            <th class="text-end">Thành tiền</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${items.map(item => `
                            <tr>
                                <td>
                                    <div class="d-flex align-items-center">
                                        <img src="${item.productImageUrl || 'https://via.placeholder.com/50'}" 
                                             alt="${item.productName || 'Product'}"
                                             style="width: 50px; height: 50px; object-fit: cover; border-radius: 5px; margin-right: 10px;"
                                             onerror="this.src='https://via.placeholder.com/50'">
                                        <strong>${item.productName || 'N/A'}</strong>
                                    </div>
                                </td>
                                <td class="text-center">${item.quantity}</td>
                                <td class="text-end">${formatPrice(item.price)}</td>
                                <td class="text-end"><strong>${formatPrice(item.price * item.quantity)}</strong></td>
                            </tr>
                        `).join('')}
                    </tbody>
                    <tfoot>
                        <tr>
                            <td colspan="3" class="text-end"><strong>Tổng cộng:</strong></td>
                            <td class="text-end">
                                <h5 class="mb-0 text-primary"><strong>${formatPrice(order.totalAmount)}</strong></h5>
                            </td>
                        </tr>
                    </tfoot>
                </table>
            </div>
        </div>
        
        <!-- Payment Info -->
        <div class="order-detail-section">
            <h6><i class="fas fa-credit-card"></i> Thông tin thanh toán</h6>
            <div class="detail-row">
                <span class="detail-label">Phương thức:</span>
                <span class="detail-value">
                    <span class="badge bg-secondary">${getPaymentMethodText(order.paymentMethod)}</span>
                </span>
            </div>
            <div class="detail-row">
                <span class="detail-label">Trạng thái thanh toán:</span>
                <span class="detail-value">${getPaymentStatusBadge(order.paymentStatus, order.paymentMethod, order.status)}</span>
            </div>
            ${order.paymentMethod === 'COD' && order.status === 'DELIVERED' ? `
                <div class="alert alert-success mt-3 payment-success-alert">
                    <div class="d-flex align-items-center">
                        <i class="fas fa-check-circle me-3 text-success"></i>
                        <div>
                            <h6 class="alert-heading mb-1"><i class="fas fa-money-bill-wave me-1"></i> Thanh toán COD hoàn tất</h6>
                            <small>Đơn hàng đã được giao thành công và tiền đã được thu từ khách hàng.</small>
                        </div>
                    </div>
                </div>
            ` : ''}
            ${order.paymentMethod === 'E_WALLET' && order.vnpayTransactionNo ? `
                <div class="alert alert-info mt-3">
                    <h6 class="alert-heading"><i class="fas fa-wallet"></i> Thông tin giao dịch VNPay</h6>
                    <hr>
                    <div class="detail-row mb-2">
                        <span class="detail-label"><strong>Mã giao dịch:</strong></span>
                        <span class="detail-value"><code>${order.vnpayTransactionNo}</code></span>
                    </div>
                    ${order.vnpayBankCode ? `
                        <div class="detail-row mb-2">
                            <span class="detail-label"><strong>Ngân hàng:</strong></span>
                            <span class="detail-value"><span class="badge bg-primary">${order.vnpayBankCode}</span></span>
                        </div>
                    ` : ''}
                    ${order.vnpayResponseCode ? `
                        <div class="detail-row mb-2">
                            <span class="detail-label"><strong>Mã phản hồi:</strong></span>
                            <span class="detail-value">
                                <span class="badge ${order.vnpayResponseCode === '00' ? 'bg-success' : 'bg-danger'}">
                                    ${order.vnpayResponseCode} - ${getVNPayResponseText(order.vnpayResponseCode)}
                                </span>
                            </span>
                        </div>
                    ` : ''}
                </div>
            ` : ''}
            <div class="detail-row">
                <span class="detail-label">Phương thức:</span>
                <span class="detail-value">${order.paymentMethod || 'COD'}</span>
            </div>
            <div class="detail-row">
                <span class="detail-label">Trạng thái thanh toán:</span>
                <span class="detail-value">
                    ${order.paymentStatus === 'PAID' 
                        ? '<span class="badge bg-success">Đã thanh toán</span>' 
                        : '<span class="badge bg-warning">Chưa thanh toán</span>'}
                </span>
            </div>
            ${order.voucherCode && order.voucherDiscount && order.voucherDiscount > 0 ? `
            <div class="detail-row">
                <span class="detail-label">Mã giảm giá:</span>
                <span class="detail-value">
                    <span class="badge bg-success">${order.voucherCode}</span>
                    <span class="text-success ms-2">-${formatPrice(order.voucherDiscount)}</span>
                </span>
            </div>
            ` : ''}
        </div>
        
        <!-- Notes -->
        ${order.notes ? `
        <div class="order-detail-section">
            <h6><i class="fas fa-sticky-note"></i> Ghi chú</h6>
            <p class="mb-0">${order.notes}</p>
        </div>
        ` : ''}
    `;
}

function getStatusText(status) {
    const texts = {
        'PENDING': 'Chờ xử lý',
        'PROCESSING': 'Đang xử lý',
        'SHIPPING': 'Đang giao',
        'DELIVERED': 'Giao thành công',
        'FAILED': 'Giao thất bại',
        'CANCELLED': 'Đơn bị hủy'
    };
    return texts[status] || status;
}

function updatePagination(data) {
    const pagination = document.getElementById('pagination');
    if (!pagination) return;
    
    const totalPages = data.totalPages;
    
    if (totalPages <= 1) {
        pagination.innerHTML = '';
        return;
    }
    
    let html = '';
    
    // Previous
    if (currentPage > 0) {
        html += `<li class="page-item">
                    <a class="page-link" href="#" onclick="changePage(${currentPage - 1}); return false;">
                        <i class="fas fa-chevron-left"></i>
                    </a>
                 </li>`;
    }
    
    // Pages
    for (let i = 0; i < totalPages; i++) {
        if (i < 3 || i >= totalPages - 3 || (i >= currentPage - 1 && i <= currentPage + 1)) {
            html += `<li class="page-item ${i === currentPage ? 'active' : ''}">
                        <a class="page-link" href="#" onclick="changePage(${i}); return false;">${i + 1}</a>
                     </li>`;
        } else if (i === 3 || i === totalPages - 4) {
            html += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
        }
    }
    
    // Next
    if (currentPage < totalPages - 1) {
        html += `<li class="page-item">
                    <a class="page-link" href="#" onclick="changePage(${currentPage + 1}); return false;">
                        <i class="fas fa-chevron-right"></i>
                    </a>
                 </li>`;
    }
    
    pagination.innerHTML = html;
}

function changePage(page) {
    currentPage = page;
    loadOrders();
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function formatPrice(price) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
}

function formatDate(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN');
}

function formatDateTime(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleString('vi-VN');
}

function getPaymentMethodText(method) {
    const methodMap = {
        'COD': 'COD',
        'E_WALLET': 'VNPay',
        'BANK_TRANSFER': 'Banking'
    };
    return methodMap[method] || method || 'N/A';
}

function getPaymentStatusBadge(status, paymentMethod, orderStatus) {
    // For COD orders, show payment status based on delivery status
    if (paymentMethod === 'COD') {
        if (orderStatus === 'DELIVERED') {
            return '<span class="badge bg-success"><i class="fas fa-check-circle"></i> Đã thanh toán</span>';
        } else {
            return '<span class="badge bg-info"><i class="fas fa-money-bill-wave"></i> COD</span>';
        }
    }
    
    if (!status) {
        return '<span class="badge bg-secondary">N/A</span>';
    }
    
    const statusMap = {
        'PENDING': { class: 'warning', text: 'Chờ thanh toán', icon: 'clock' },
        'PAID': { class: 'success', text: 'Đã thanh toán', icon: 'check-circle' },
        'FAILED': { class: 'danger', text: 'Thất bại', icon: 'times-circle' }
    };
    
    const statusInfo = statusMap[status] || { class: 'secondary', text: status, icon: 'question-circle' };
    return `<span class="badge bg-${statusInfo.class}">
                <i class="fas fa-${statusInfo.icon}"></i> ${statusInfo.text}
            </span>`;
}

function getVNPayResponseText(code) {
    const responseMap = {
        '00': 'Giao dịch thành công',
        '07': 'Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường).',
        '09': 'Thẻ/Tài khoản chưa đăng ký dịch vụ InternetBanking tại ngân hàng.',
        '10': 'Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần',
        '11': 'Đã hết hạn chờ thanh toán. Xin quý khách vui lòng thực hiện lại giao dịch.',
        '12': 'Thẻ/Tài khoản bị khóa.',
        '13': 'Quý khách nhập sai mật khẩu xác thực giao dịch (OTP).',
        '24': 'Khách hàng hủy giao dịch',
        '51': 'Tài khoản không đủ số dư để thực hiện giao dịch.',
        '65': 'Tài khoản đã vượt quá hạn mức giao dịch trong ngày.',
        '75': 'Ngân hàng thanh toán đang bảo trì.',
        '79': 'KH nhập sai mật khẩu thanh toán quá số lần quy định.',
        '99': 'Các lỗi khác'
    };
    return responseMap[code] || 'Không xác định';
}

function showToast(message, type = 'success') {
    const toastDiv = document.createElement('div');
    toastDiv.className = `alert alert-${type} position-fixed top-0 start-50 translate-middle-x mt-3`;
    toastDiv.style.zIndex = '9999';
    const icon = type === 'success' ? 'check-circle' : type === 'warning' ? 'exclamation-triangle' : 'exclamation-circle';
    toastDiv.innerHTML = `<i class="fas fa-${icon} me-2"></i>${message}`;
    document.body.appendChild(toastDiv);
    
    setTimeout(() => {
        toastDiv.style.opacity = '0';
        toastDiv.style.transition = 'opacity 0.5s';
        setTimeout(() => toastDiv.remove(), 500);
    }, 3000);
}

// Logout function
function logout() {
    if (confirm('Bạn có chắc muốn đăng xuất?')) {
        localStorage.removeItem('authToken');
        localStorage.removeItem('token');
        localStorage.removeItem('authEmail');
        localStorage.removeItem('userEmail');
        localStorage.removeItem('userRole');
        showToast('Đã đăng xuất thành công!', 'success');
        setTimeout(() => {
            window.location.href = '/login';
        }, 1000);
    }
}

// Update admin info in sidebar on page load
window.addEventListener('DOMContentLoaded', function() {
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    if (userEmail) {
        const adminEmailEl = document.getElementById('adminEmail');
        const adminNameEl = document.getElementById('adminName');
        if (adminEmailEl) adminEmailEl.textContent = userEmail;
        if (adminNameEl) adminNameEl.textContent = userEmail.split('@')[0];
    }
});
