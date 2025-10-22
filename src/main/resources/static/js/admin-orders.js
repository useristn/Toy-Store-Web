let currentPage = 0;
let currentStatus = '';
let currentSearch = '';
const pageSize = 20;

document.addEventListener('DOMContentLoaded', function() {
    checkAdminAuth();
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
            window.location.href = '/login';
        }, 1500);
        return;
    }
    
    // Check admin role
    if (!userRole || !userRole.includes('ADMIN')) {
        showToast('Bạn không có quyền truy cập trang này!', 'danger');
        setTimeout(() => {
            window.location.href = '/';
        }, 1500);
        return;
    }
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
        
        if (currentSearch) {
            url += `&search=${encodeURIComponent(currentSearch)}`;
        }
        
        const dateFrom = document.getElementById('dateFrom').value;
        const dateTo = document.getElementById('dateTo').value;
        if (dateFrom) url += `&dateFrom=${dateFrom}`;
        if (dateTo) url += `&dateTo=${dateTo}`;

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
                <td colspan="7" class="text-center py-5">
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
        
        return `
            <tr>
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
                <td>
                    <select class="status-select ${getStatusClass(order.status)}" 
                            onchange="updateOrderStatus(${order.id}, this.value)"
                            ${order.status === 'DELIVERED' || order.status === 'CANCELLED' ? 'disabled' : ''}>
                        <option value="PENDING" ${order.status === 'PENDING' ? 'selected' : ''}>Chờ xử lý</option>
                        <option value="PROCESSING" ${order.status === 'PROCESSING' ? 'selected' : ''}>Đang xử lý</option>
                        <option value="SHIPPING" ${order.status === 'SHIPPING' ? 'selected' : ''}>Đang giao</option>
                        <option value="DELIVERED" ${order.status === 'DELIVERED' ? 'selected' : ''}>Đã giao</option>
                        <option value="CANCELLED" ${order.status === 'CANCELLED' ? 'selected' : ''}>Đã hủy</option>
                    </select>
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
        'PENDING': '<span class="badge bg-warning">Chờ xử lý</span>',
        'PROCESSING': '<span class="badge bg-info">Đang xử lý</span>',
        'SHIPPING': '<span class="badge bg-primary">Đang giao</span>',
        'DELIVERED': '<span class="badge bg-success">Đã giao</span>',
        'CANCELLED': '<span class="badge bg-danger">Đã hủy</span>'
    };
    return badges[status] || '<span class="badge bg-secondary">N/A</span>';
}

function getStatusClass(status) {
    const classes = {
        'PENDING': 'bg-warning',
        'PROCESSING': 'bg-info',
        'SHIPPING': 'bg-primary',
        'DELIVERED': 'bg-success',
        'CANCELLED': 'bg-danger'
    };
    return classes[status] || '';
}

function filterByStatus(status) {
    currentStatus = status;
    currentPage = 0;
    
    // Update active stat card
    document.querySelectorAll('.stat-card').forEach(card => card.classList.remove('active'));
    event.target.closest('.stat-card').classList.add('active');
    
    loadOrders();
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
        'DELIVERED': 'Đã giao',
        'CANCELLED': 'Đã hủy'
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
