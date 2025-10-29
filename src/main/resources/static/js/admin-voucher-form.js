// Admin Voucher Form Management
let isEditMode = false;
let voucherId = null;

document.addEventListener('DOMContentLoaded', function() {
    checkAdminAuth();
    setupFormHandlers();
    checkEditMode();
    setDefaultDates();
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

function setupFormHandlers() {
    // Discount type change handler
    const typeRadios = document.querySelectorAll('input[name="discountType"]');
    typeRadios.forEach(radio => {
        radio.addEventListener('change', handleDiscountTypeChange);
    });

    // Form submit handler
    document.getElementById('voucherForm').addEventListener('submit', handleSubmit);
}

function handleDiscountTypeChange(e) {
    const type = e.target.value;
    const valueLabel = document.getElementById('valueLabel');
    const discountValue = document.getElementById('discountValue');
    const maxDiscountGroup = document.getElementById('maxDiscountGroup');

    switch(type) {
        case 'PERCENTAGE':
            valueLabel.innerHTML = 'Giá trị giảm (%) <span class="text-danger">*</span>';
            discountValue.placeholder = 'VD: 15';
            discountValue.max = '100';
            maxDiscountGroup.style.display = 'block';
            break;
        case 'FIXED_AMOUNT':
            valueLabel.innerHTML = 'Số tiền giảm (VNĐ) <span class="text-danger">*</span>';
            discountValue.placeholder = 'VD: 50000';
            discountValue.removeAttribute('max');
            maxDiscountGroup.style.display = 'none';
            break;
        case 'FREE_SHIPPING':
            valueLabel.innerHTML = 'Giảm phí ship tối đa (VNĐ)';
            discountValue.placeholder = 'VD: 30000 (để trống = miễn phí hoàn toàn)';
            discountValue.removeAttribute('max');
            discountValue.removeAttribute('required');
            maxDiscountGroup.style.display = 'none';
            break;
    }
}

function checkEditMode() {
    const urlPath = window.location.pathname;
    const match = urlPath.match(/\/admin\/vouchers\/edit\/(\d+)/);
    
    if (match) {
        isEditMode = true;
        voucherId = match[1];
        document.getElementById('pageTitle').innerHTML = '🎫 Chỉnh sửa mã giảm giá';
        document.getElementById('submitBtn').innerHTML = '<i class="fas fa-save me-1"></i>Cập nhật';
        loadVoucherData(voucherId);
    }
}

function loadVoucherData(id) {
    fetch(`/api/admin/vouchers/${id}`, {
        headers: getAuthHeaders()
    })
    .then(response => {
        if (!response.ok) throw new Error('Không tìm thấy mã giảm giá');
        return response.json();
    })
    .then(voucher => {
        // Basic info
        document.getElementById('code').value = voucher.code;
        document.getElementById('description').value = voucher.description || '';
        document.getElementById('active').checked = voucher.active;

        // Discount type & value
        document.querySelector(`input[name="discountType"][value="${voucher.discountType}"]`).checked = true;
        handleDiscountTypeChange({ target: { value: voucher.discountType } });
        document.getElementById('discountValue').value = voucher.discountValue;
        if (voucher.maxDiscount) {
            document.getElementById('maxDiscount').value = voucher.maxDiscount;
        }

        // Conditions
        if (voucher.minOrderValue) {
            document.getElementById('minOrderValue').value = voucher.minOrderValue;
        }
        if (voucher.limitPerUser) {
            document.getElementById('limitPerUser').value = voucher.limitPerUser;
        }

        // Quantity & Time
        document.getElementById('totalQuantity').value = voucher.totalQuantity;
        document.getElementById('startDate').value = formatDateTimeLocal(voucher.startDate);
        document.getElementById('endDate').value = formatDateTimeLocal(voucher.endDate);
    })
    .catch(error => {
        console.error('Error loading voucher:', error);
        alert('Lỗi tải thông tin mã giảm giá');
        window.location.href = '/admin/vouchers';
    });
}

function formatDateTimeLocal(dateString) {
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
}

function setDefaultDates() {
    if (!isEditMode) {
        const now = new Date();
        const tomorrow = new Date(now);
        tomorrow.setDate(tomorrow.getDate() + 1);
        const nextMonth = new Date(now);
        nextMonth.setMonth(nextMonth.getMonth() + 1);

        document.getElementById('startDate').value = formatDateTimeLocal(tomorrow);
        document.getElementById('endDate').value = formatDateTimeLocal(nextMonth);
    }
}

function generateCode() {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    let code = '';
    for (let i = 0; i < 8; i++) {
        code += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    document.getElementById('code').value = code;
}

function handleSubmit(e) {
    e.preventDefault();

    // Validate dates
    const startDate = new Date(document.getElementById('startDate').value);
    const endDate = new Date(document.getElementById('endDate').value);
    
    if (endDate <= startDate) {
        alert('Ngày kết thúc phải sau ngày bắt đầu!');
        return;
    }

    // Build request data
    const formData = {
        code: document.getElementById('code').value.toUpperCase().trim(),
        description: document.getElementById('description').value.trim(),
        discountType: document.querySelector('input[name="discountType"]:checked').value,
        discountValue: parseFloat(document.getElementById('discountValue').value) || 0,
        maxDiscount: parseFloat(document.getElementById('maxDiscount').value) || null,
        minOrderValue: parseFloat(document.getElementById('minOrderValue').value) || null,
        totalQuantity: parseInt(document.getElementById('totalQuantity').value),
        limitPerUser: parseInt(document.getElementById('limitPerUser').value) || null,
        startDate: document.getElementById('startDate').value,
        endDate: document.getElementById('endDate').value,
        active: document.getElementById('active').checked,
        applicableCategories: null,
        applicableProducts: null,
        applicableUserGroups: null
    };

    // Validate discount value
    if (formData.discountType === 'PERCENTAGE') {
        if (formData.discountValue <= 0 || formData.discountValue > 100) {
            alert('Giá trị giảm giá phải từ 0-100%');
            return;
        }
    } else if (formData.discountType === 'FIXED_AMOUNT') {
        if (formData.discountValue <= 0) {
            alert('Số tiền giảm phải lớn hơn 0');
            return;
        }
    }

    // Submit form
    const url = isEditMode ? `/api/admin/vouchers/${voucherId}` : '/api/admin/vouchers';
    const method = isEditMode ? 'PUT' : 'POST';

    document.getElementById('submitBtn').disabled = true;
    document.getElementById('submitBtn').innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i>Đang xử lý...';

    fetch(url, {
        method: method,
        headers: getAuthHeaders(),
        body: JSON.stringify(formData)
    })
    .then(response => {
        if (!response.ok) {
            return response.text().then(text => {
                throw new Error(text || 'Lỗi lưu mã giảm giá');
            });
        }
        return response.json();
    })
    .then(data => {
        showAlert(isEditMode ? 'Cập nhật mã giảm giá thành công!' : 'Tạo mã giảm giá thành công!', 'success');
        setTimeout(() => {
            window.location.href = '/admin/vouchers';
        }, 1500);
    })
    .catch(error => {
        console.error('Error saving voucher:', error);
        showAlert(error.message || 'Lỗi lưu mã giảm giá', 'danger');
        document.getElementById('submitBtn').disabled = false;
        document.getElementById('submitBtn').innerHTML = isEditMode ? 
            '<i class="fas fa-save me-1"></i>Cập nhật' : 
            '<i class="fas fa-save me-1"></i>Lưu mã giảm giá';
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
