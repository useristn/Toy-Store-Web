// Admin Vouchers Management
let currentPage = 0;
const pageSize = 10;
let currentFilters = {};
let deleteVoucherId = null;

document.addEventListener('DOMContentLoaded', function() {
    checkAdminAuth();
    loadStatistics();
    loadVouchers();
});

function checkAdminAuth() {
    const authToken = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userRole = localStorage.getItem('userRole');
    
    if (!authToken || !userRole || !userRole.includes('ADMIN')) {
        alert('Bạn không có quyền truy cập trang này');
        window.location.href = '/login';
        return;
    }

    // Load admin info
    const adminName = localStorage.getItem('userName');
    const adminEmail = localStorage.getItem('userEmail');
    if (adminName) document.getElementById('adminName').textContent = adminName;
    if (adminEmail) document.getElementById('adminEmail').textContent = adminEmail;
}

function getAuthHeaders() {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    return {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    };
}

function loadStatistics() {
    fetch('/api/admin/vouchers/stats', {
        headers: getAuthHeaders()
    })
    .then(response => response.json())
    .then(stats => {
        document.getElementById('totalVouchers').textContent = stats.totalVouchers || 0;
        document.getElementById('activeVouchers').textContent = stats.activeVouchers || 0;
        document.getElementById('upcomingVouchers').textContent = stats.upcomingVouchers || 0;
        document.getElementById('totalUsage').textContent = stats.totalUsage || 0;
    })
    .catch(error => {
        console.error('Error loading statistics:', error);
    });
}

function loadVouchers(page = 0) {
    currentPage = page;
    const params = new URLSearchParams({
        page: page,
        size: pageSize,
        sortBy: 'createdAt',
        sortDir: 'desc'
    });

    // Add filters
    if (currentFilters.code) params.append('code', currentFilters.code);
    if (currentFilters.status) params.append('status', currentFilters.status);
    if (currentFilters.discountType) params.append('discountType', currentFilters.discountType);

    fetch(`/api/admin/vouchers?${params.toString()}`, {
        headers: getAuthHeaders()
    })
    .then(response => response.json())
    .then(data => {
        displayVouchers(data.content);
        displayPagination(data);
    })
    .catch(error => {
        console.error('Error loading vouchers:', error);
        document.getElementById('vouchersTableBody').innerHTML = 
            '<tr><td colspan="8" class="text-center text-danger">Lỗi tải dữ liệu</td></tr>';
    });
}

function displayVouchers(vouchers) {
    const tbody = document.getElementById('vouchersTableBody');
    
    if (!vouchers || vouchers.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center">Không có mã giảm giá nào</td></tr>';
        return;
    }

    tbody.innerHTML = vouchers.map(voucher => {
        const statusBadge = getStatusBadge(voucher.status);
        const typeBadge = getTypeBadge(voucher.discountType);
        const valueDisplay = getValueDisplay(voucher);
        
        return `
            <tr>
                <td><strong>${voucher.code}</strong></td>
                <td><small>${voucher.description || '-'}</small></td>
                <td>${typeBadge}</td>
                <td>${valueDisplay}</td>
                <td>
                    <span class="badge ${voucher.usedQuantity >= voucher.totalQuantity ? 'bg-danger' : 'bg-info'}">
                        ${voucher.usedQuantity}/${voucher.totalQuantity}
                    </span>
                </td>
                <td>
                    <small>
                        ${formatDate(voucher.startDate)}<br/>
                        <i class="fas fa-arrow-down"></i><br/>
                        ${formatDate(voucher.endDate)}
                    </small>
                </td>
                <td>${statusBadge}</td>
                <td>
                    <div class="btn-group btn-group-sm" role="group">
                        <a href="/admin/vouchers/edit/${voucher.id}" class="btn btn-outline-primary" title="Sửa">
                            <i class="fas fa-edit"></i>
                        </a>
                        <button type="button" class="btn btn-outline-${voucher.active ? 'warning' : 'success'}" 
                                onclick="toggleStatus(${voucher.id})" title="${voucher.active ? 'Tắt' : 'Bật'}">
                            <i class="fas fa-power-off"></i>
                        </button>
                        <button type="button" class="btn btn-outline-danger" 
                                onclick="showDeleteModal(${voucher.id})" title="Xóa">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function getStatusBadge(status) {
    const badges = {
        'ACTIVE': '<span class="badge bg-active">Đang hoạt động</span>',
        'UPCOMING': '<span class="badge bg-upcoming">Sắp diễn ra</span>',
        'EXPIRED': '<span class="badge bg-expired">Đã hết hạn</span>',
        'DISABLED': '<span class="badge bg-disabled">Đã tắt</span>',
        'OUT_OF_STOCK': '<span class="badge bg-out-of-stock">Hết lượt</span>'
    };
    return badges[status] || '<span class="badge bg-secondary">N/A</span>';
}

function getTypeBadge(type) {
    const badges = {
        'PERCENTAGE': '<span class="badge bg-primary">Giảm %</span>',
        'FIXED_AMOUNT': '<span class="badge bg-success">Giảm tiền</span>',
        'FREE_SHIPPING': '<span class="badge bg-info">Freeship</span>'
    };
    return badges[type] || '<span class="badge bg-secondary">N/A</span>';
}

function getValueDisplay(voucher) {
    switch(voucher.discountType) {
        case 'PERCENTAGE':
            let display = `${voucher.discountValue}%`;
            if (voucher.maxDiscount) {
                display += `<br/><small>(Tối đa ${formatCurrency(voucher.maxDiscount)})</small>`;
            }
            return display;
        case 'FIXED_AMOUNT':
            return formatCurrency(voucher.discountValue);
        case 'FREE_SHIPPING':
            return voucher.discountValue ? `Tối đa ${formatCurrency(voucher.discountValue)}` : 'Miễn phí ship';
        default:
            return '-';
    }
}

function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

function displayPagination(data) {
    const pagination = document.getElementById('pagination');
    const totalPages = data.totalPages;
    const currentPage = data.number;

    if (totalPages <= 1) {
        pagination.innerHTML = '';
        return;
    }

    let html = '';
    
    // Previous button
    html += `
        <li class="page-item ${currentPage === 0 ? 'disabled' : ''}">
            <a class="page-link" href="#" onclick="loadVouchers(${currentPage - 1}); return false;">Trước</a>
        </li>
    `;

    // Page numbers
    for (let i = 0; i < totalPages; i++) {
        if (i === 0 || i === totalPages - 1 || (i >= currentPage - 2 && i <= currentPage + 2)) {
            html += `
                <li class="page-item ${i === currentPage ? 'active' : ''}">
                    <a class="page-link" href="#" onclick="loadVouchers(${i}); return false;">${i + 1}</a>
                </li>
            `;
        } else if (i === currentPage - 3 || i === currentPage + 3) {
            html += '<li class="page-item disabled"><span class="page-link">...</span></li>';
        }
    }

    // Next button
    html += `
        <li class="page-item ${currentPage === totalPages - 1 ? 'disabled' : ''}">
            <a class="page-link" href="#" onclick="loadVouchers(${currentPage + 1}); return false;">Sau</a>
        </li>
    `;

    pagination.innerHTML = html;
}

function applyFilters() {
    currentFilters = {
        code: document.getElementById('filterCode').value.trim(),
        status: document.getElementById('filterStatus').value,
        discountType: document.getElementById('filterType').value
    };
    loadVouchers(0);
}

function refreshVouchers() {
    loadStatistics();
    loadVouchers(currentPage);
}

function toggleStatus(id) {
    if (!confirm('Bạn có chắc chắn muốn thay đổi trạng thái voucher này?')) {
        return;
    }

    fetch(`/api/admin/vouchers/${id}/toggle`, {
        method: 'PATCH',
        headers: getAuthHeaders()
    })
    .then(response => {
        if (response.ok) {
            showAlert('Cập nhật trạng thái thành công!', 'success');
            loadVouchers(currentPage);
            loadStatistics();
        } else {
            showAlert('Lỗi cập nhật trạng thái', 'danger');
        }
    })
    .catch(error => {
        console.error('Error toggling status:', error);
        showAlert('Lỗi kết nối máy chủ', 'danger');
    });
}

function showDeleteModal(id) {
    deleteVoucherId = id;
    const modal = new bootstrap.Modal(document.getElementById('deleteModal'));
    modal.show();
}

function confirmDelete() {
    if (!deleteVoucherId) return;

    fetch(`/api/admin/vouchers/${deleteVoucherId}`, {
        method: 'DELETE',
        headers: getAuthHeaders()
    })
    .then(response => {
        if (response.ok) {
            showAlert('Xóa mã giảm giá thành công!', 'success');
            const modal = bootstrap.Modal.getInstance(document.getElementById('deleteModal'));
            modal.hide();
            loadVouchers(currentPage);
            loadStatistics();
        } else {
            return response.text().then(text => {
                throw new Error(text || 'Lỗi xóa mã giảm giá');
            });
        }
    })
    .catch(error => {
        console.error('Error deleting voucher:', error);
        showAlert(error.message || 'Không thể xóa mã giảm giá đã được sử dụng', 'danger');
    });
}

function showAlert(message, type) {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type} alert-dismissible fade show position-fixed top-0 start-50 translate-middle-x mt-3`;
    alertDiv.style.zIndex = '9999';
    alertDiv.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    document.body.appendChild(alertDiv);
    
    setTimeout(() => {
        alertDiv.remove();
    }, 3000);
}

function logout() {
    localStorage.clear();
    window.location.href = '/login';
}
