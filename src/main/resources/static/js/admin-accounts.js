// Admin Accounts Management JavaScript

let currentPage = 0;
let pageSize = 10;
let selectedAccountIds = new Set();
let accountToDelete = null;

// Helper function to get token
function getAuthToken() {
    return localStorage.getItem('authToken') || localStorage.getItem('token') || '';
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    if (!checkAdminAuth()) {
        return; // Stop execution if not authenticated
    }
    loadAccounts();
});

// Load accounts with filters and pagination
function loadAccounts() {
    const search = document.getElementById('searchInput').value;
    const roleFilter = document.getElementById('roleFilter').value;
    pageSize = parseInt(document.getElementById('pageSizeSelect').value);

    const params = new URLSearchParams({
        page: currentPage,
        size: pageSize,
        search: search,
        roleFilter: roleFilter,
        sortBy: 'created',
        sortDir: 'desc'
    });

    fetch(`/api/admin/accounts?${params}`, {
        headers: {
            'Authorization': 'Bearer ' + getAuthToken()
        }
    })
    .then(response => response.json())
    .then(data => {
        displayAccounts(data.accounts);
        updatePagination(data.currentPage, data.totalPages, data.totalItems);
        selectedAccountIds.clear();
        document.getElementById('selectAll').checked = false;
    })
    .catch(error => {
        console.error('Error loading accounts:', error);
        showAlert('Không thể tải danh sách tài khoản', 'danger');
    });
}

// Display accounts in table
function displayAccounts(accounts) {
    const tbody = document.getElementById('accountsTableBody');
    
    if (accounts.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="text-center py-4">
                    <i class="fas fa-inbox fa-3x text-muted mb-3"></i>
                    <p class="text-muted">Không tìm thấy tài khoản nào</p>
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = accounts.map(account => `
        <tr>
            <td>
                <input type="checkbox" class="form-check-input account-checkbox" 
                       value="${account.id}" onchange="toggleAccountSelection(${account.id})">
            </td>
            <td>
                <strong>${escapeHtml(account.name)}</strong>
            </td>
            <td>${escapeHtml(account.email)}</td>
            <td>${account.phone || '<span class="text-muted">N/A</span>'}</td>
            <td>${getRoleBadges(account.roles)}</td>
            <td>${formatDate(account.created)}</td>
            <td>
                <div class="btn-group btn-group-sm" role="group">
                    <button class="btn btn-info" onclick="viewAccount(${account.id})" 
                            title="Xem chi tiết">
                        <i class="fas fa-eye"></i>
                    </button>
                    <button class="btn btn-warning" onclick="editAccount(${account.id})" 
                            title="Chỉnh sửa">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn btn-danger" onclick="openDeleteModal(${account.id})" 
                            title="Xóa">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </td>
        </tr>
    `).join('');
}

// Get role badges
function getRoleBadges(roles) {
    const roleColors = {
        'ADMIN': 'primary',
        'USER': 'secondary',
        'SHIPPER': 'warning'
    };

    return roles.map(role => {
        const color = roleColors[role] || 'secondary';
        return `<span class="badge bg-${color} me-1">${role}</span>`;
    }).join('');
}



// Format date
function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
    });
}

// Update pagination
function updatePagination(current, total, totalItems) {
    const pagination = document.getElementById('pagination');
    const pageInfo = document.getElementById('pageInfo');

    // Page info
    const start = current * pageSize + 1;
    const end = Math.min((current + 1) * pageSize, totalItems);
    pageInfo.innerHTML = `Hiển thị ${start}-${end} / ${totalItems}`;

    // Pagination buttons
    let paginationHTML = '';

    // Previous button
    paginationHTML += `
        <li class="page-item ${current === 0 ? 'disabled' : ''}">
            <a class="page-link" href="#" onclick="changePage(${current - 1}); return false;">
                <i class="fas fa-chevron-left"></i>
            </a>
        </li>
    `;

    // Page numbers
    const maxPages = 5;
    let startPage = Math.max(0, current - Math.floor(maxPages / 2));
    let endPage = Math.min(total - 1, startPage + maxPages - 1);

    if (endPage - startPage < maxPages - 1) {
        startPage = Math.max(0, endPage - maxPages + 1);
    }

    for (let i = startPage; i <= endPage; i++) {
        paginationHTML += `
            <li class="page-item ${i === current ? 'active' : ''}">
                <a class="page-link" href="#" onclick="changePage(${i}); return false;">
                    ${i + 1}
                </a>
            </li>
        `;
    }

    // Next button
    paginationHTML += `
        <li class="page-item ${current === total - 1 ? 'disabled' : ''}">
            <a class="page-link" href="#" onclick="changePage(${current + 1}); return false;">
                <i class="fas fa-chevron-right"></i>
            </a>
        </li>
    `;

    pagination.innerHTML = paginationHTML;
}

// Change page
function changePage(page) {
    currentPage = page;
    loadAccounts();
}

// Search accounts
let searchTimeout;
function searchAccounts() {
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => {
        currentPage = 0;
        loadAccounts();
    }, 500);
}

// Toggle select all
function toggleSelectAll() {
    const selectAll = document.getElementById('selectAll').checked;
    const checkboxes = document.querySelectorAll('.account-checkbox');
    
    checkboxes.forEach(checkbox => {
        checkbox.checked = selectAll;
        const id = parseInt(checkbox.value);
        if (selectAll) {
            selectedAccountIds.add(id);
        } else {
            selectedAccountIds.delete(id);
        }
    });
}

// Toggle account selection
function toggleAccountSelection(id) {
    if (selectedAccountIds.has(id)) {
        selectedAccountIds.delete(id);
    } else {
        selectedAccountIds.add(id);
    }
}

// Handle bulk action
function handleBulkAction() {
    const action = document.getElementById('bulkActionSelect').value;
    if (!action) return;

    if (selectedAccountIds.size === 0) {
        showAlert('Vui lòng chọn ít nhất một tài khoản', 'warning');
        document.getElementById('bulkActionSelect').value = '';
        return;
    }

    if (action === 'delete') {
        if (confirm(`Bạn có chắc chắn muốn XÓA VĨNH VIỄN ${selectedAccountIds.size} tài khoản đã chọn?\n\nLưu ý: Chỉ xóa được tài khoản chưa có đơn hàng. Hành động này không thể hoàn tác!`)) {
            const requestData = {
                userIds: Array.from(selectedAccountIds),
                action: action
            };

            fetch('/api/admin/accounts/bulk-action', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + getAuthToken()
                },
                body: JSON.stringify(requestData)
            })
            .then(response => {
                if (!response.ok) {
                    return response.json().then(data => {
                        throw new Error(data.error || 'Không thể xóa các tài khoản đã chọn');
                    });
                }
                return response.json();
            })
            .then(data => {
                if (data.message) {
                    showAlert(data.message, 'success');
                    loadAccounts();
                }
            })
            .catch(error => {
                console.error('Error:', error);
                showAlert(error.message || 'Có lỗi xảy ra khi thực hiện hành động', 'danger');
            })
            .finally(() => {
                document.getElementById('bulkActionSelect').value = '';
            });
        } else {
            document.getElementById('bulkActionSelect').value = '';
        }
    }
}

// Open create modal
function openCreateModal() {
    document.getElementById('accountModalTitle').textContent = 'Tạo Tài khoản Mới';
    document.getElementById('accountForm').reset();
    document.getElementById('accountId').value = '';
    document.getElementById('passwordFields').style.display = 'block';
    document.getElementById('accountPassword').required = true;
    document.getElementById('accountConfirmPassword').required = true;
    document.getElementById('passwordChangeFields').style.display = 'none';
    document.getElementById('changePasswordCheck').checked = false;
    document.getElementById('passwordChangeInputs').style.display = 'none';
    
    const modal = new bootstrap.Modal(document.getElementById('accountModal'));
    modal.show();
}

// Edit account
function editAccount(id) {
    fetch(`/api/admin/accounts/${id}`, {
        headers: {
            'Authorization': 'Bearer ' + getAuthToken()
        }
    })
    .then(response => response.json())
    .then(account => {
        document.getElementById('accountModalTitle').textContent = 'Chỉnh sửa Tài khoản';
        document.getElementById('accountId').value = account.id;
        document.getElementById('accountName').value = account.name;
        document.getElementById('accountEmail').value = account.email;
        document.getElementById('accountPhone').value = account.phone || '';
        document.getElementById('accountRole').value = account.roles[0] || '';
        document.getElementById('passwordFields').style.display = 'none';
        document.getElementById('accountPassword').required = false;
        document.getElementById('accountConfirmPassword').required = false;
        document.getElementById('passwordChangeFields').style.display = 'block';
        document.getElementById('changePasswordCheck').checked = false;
        document.getElementById('passwordChangeInputs').style.display = 'none';
        document.getElementById('newPassword').value = '';
        document.getElementById('confirmNewPassword').value = '';
        
        const modal = new bootstrap.Modal(document.getElementById('accountModal'));
        modal.show();
    })
    .catch(error => {
        console.error('Error:', error);
        showAlert('Không thể tải thông tin tài khoản', 'danger');
    });
}

// Save account
function saveAccount() {
    const id = document.getElementById('accountId').value;
    const name = document.getElementById('accountName').value.trim();
    const email = document.getElementById('accountEmail').value.trim();
    const phone = document.getElementById('accountPhone').value.trim();
    const role = document.getElementById('accountRole').value;

    if (!name || !email || !role) {
        showAlert('Vui lòng điền đầy đủ thông tin bắt buộc', 'warning');
        return;
    }

    if (id) {
        // Update existing account
        const changePassword = document.getElementById('changePasswordCheck').checked;
        let updateData = { name, email, phone, role };
        
        if (changePassword) {
            const newPassword = document.getElementById('newPassword').value;
            const confirmNewPassword = document.getElementById('confirmNewPassword').value;
            
            if (!newPassword || !confirmNewPassword) {
                showAlert('Vui lòng nhập mật khẩu mới', 'warning');
                return;
            }
            
            if (newPassword !== confirmNewPassword) {
                showAlert('Mật khẩu xác nhận không khớp', 'warning');
                return;
            }
            
            if (newPassword.length < 6) {
                showAlert('Mật khẩu mới phải có ít nhất 6 ký tự', 'warning');
                return;
            }
            
            updateData.password = newPassword;
            updateData.confirmPassword = confirmNewPassword;
        }
        
        updateAccountData(id, updateData);
    } else {
        // Create new account
        const password = document.getElementById('accountPassword').value;
        const confirmPassword = document.getElementById('accountConfirmPassword').value;

        if (!password || !confirmPassword) {
            showAlert('Vui lòng nhập mật khẩu', 'warning');
            return;
        }

        if (password !== confirmPassword) {
            showAlert('Mật khẩu xác nhận không khớp', 'warning');
            return;
        }

        if (password.length < 6) {
            showAlert('Mật khẩu phải có ít nhất 6 ký tự', 'warning');
            return;
        }

        createAccountData({ name, email, phone, role, password, confirmPassword });
    }
}

// Create account
function createAccountData(data) {
    fetch('/api/admin/accounts', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + getAuthToken()
        },
        body: JSON.stringify(data)
    })
    .then(response => response.json())
    .then(result => {
        if (result.message) {
            showAlert(result.message, 'success');
            bootstrap.Modal.getInstance(document.getElementById('accountModal')).hide();
            loadAccounts();
        } else if (result.error) {
            showAlert(result.error, 'danger');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showAlert('Có lỗi xảy ra khi tạo tài khoản', 'danger');
    });
}

// Update account
function updateAccountData(id, data) {
    fetch(`/api/admin/accounts/${id}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + getAuthToken()
        },
        body: JSON.stringify(data)
    })
    .then(response => response.json())
    .then(result => {
        if (result.message) {
            showAlert(result.message, 'success');
            bootstrap.Modal.getInstance(document.getElementById('accountModal')).hide();
            loadAccounts();
        } else if (result.error) {
            showAlert(result.error, 'danger');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showAlert('Có lỗi xảy ra khi cập nhật tài khoản', 'danger');
    });
}

// View account details
function viewAccount(id) {
    fetch(`/api/admin/accounts/${id}`, {
        headers: {
            'Authorization': 'Bearer ' + getAuthToken()
        }
    })
    .then(response => response.json())
    .then(account => {
        const modalBody = document.getElementById('viewAccountBody');
        modalBody.innerHTML = `
            <div class="row g-3">
                <div class="col-md-6">
                    <strong><i class="fas fa-user me-2 text-primary"></i>Họ và Tên:</strong><br>
                    <span class="ms-4">${escapeHtml(account.name)}</span>
                </div>
                <div class="col-md-6">
                    <strong><i class="fas fa-envelope me-2 text-info"></i>Email:</strong><br>
                    <span class="ms-4">${escapeHtml(account.email)}</span>
                </div>
                <div class="col-md-6">
                    <strong><i class="fas fa-phone me-2 text-success"></i>Số điện thoại:</strong><br>
                    <span class="ms-4">${account.phone || '<span class="text-muted">Chưa cập nhật</span>'}</span>
                </div>
                <div class="col-md-6">
                    <strong><i class="fas fa-map-marker-alt me-2 text-danger"></i>Địa chỉ:</strong><br>
                    <span class="ms-4">${account.address || '<span class="text-muted">Chưa cập nhật</span>'}</span>
                </div>
                <div class="col-md-6">
                    <strong><i class="fas fa-user-tag me-2 text-warning"></i>Vai trò:</strong><br>
                    <span class="ms-4">${getRoleBadges(account.roles)}</span>
                </div>
                <div class="col-md-6">
                    <strong><i class="fas fa-calendar-plus me-2 text-primary"></i>Ngày tham gia:</strong><br>
                    <span class="ms-4">${formatDate(account.created)}</span>
                </div>
                <div class="col-md-12">
                    <strong><i class="fas fa-clock me-2 text-secondary"></i>Cập nhật lần cuối:</strong><br>
                    <span class="ms-4">${formatDate(account.updated)}</span>
                </div>
            </div>
        `;
        
        const modal = new bootstrap.Modal(document.getElementById('viewAccountModal'));
        modal.show();
    })
    .catch(error => {
        console.error('Error:', error);
        showAlert('Không thể tải thông tin tài khoản', 'danger');
    });
}

// Reset password
function resetPassword(id) {
    if (confirm('Gửi email đặt lại mật khẩu cho người dùng này?')) {
        fetch(`/api/admin/accounts/${id}/reset-password`, {
            method: 'POST',
            headers: {
                'Authorization': 'Bearer ' + getAuthToken()
            }
        })
        .then(response => response.json())
        .then(data => {
            if (data.message) {
                showAlert(data.message, 'success');
            } else if (data.error) {
                showAlert(data.error, 'danger');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showAlert('Có lỗi xảy ra khi gửi email', 'danger');
        });
    }
}

// Open delete modal
function openDeleteModal(id) {
    accountToDelete = id;
    const modal = new bootstrap.Modal(document.getElementById('deleteModal'));
    modal.show();
}

// Confirm delete
function confirmDelete() {
    if (accountToDelete) {
        fetch(`/api/admin/accounts/${accountToDelete}`, {
            method: 'DELETE',
            headers: {
                'Authorization': 'Bearer ' + getAuthToken()
            }
        })
        .then(response => {
            if (!response.ok) {
                return response.json().then(data => {
                    throw new Error(data.error || 'Không thể xóa tài khoản');
                });
            }
            return response.json();
        })
        .then(data => {
            if (data.message) {
                showAlert(data.message, 'success');
                loadAccounts();
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showAlert(error.message || 'Có lỗi xảy ra khi xóa tài khoản', 'danger');
        })
        .finally(() => {
            bootstrap.Modal.getInstance(document.getElementById('deleteModal')).hide();
            accountToDelete = null;
        });
    }
}

// Show alert
function showAlert(message, type) {
    const alertContainer = document.getElementById('alertContainer');
    const alert = document.createElement('div');
    alert.className = `alert alert-${type} alert-dismissible fade show`;
    alert.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    alertContainer.appendChild(alert);

    setTimeout(() => {
        alert.remove();
    }, 5000);
}

// Escape HTML
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Check admin authentication
function checkAdminAuth() {
    const token = getAuthToken();
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    const userRole = localStorage.getItem('userRole');
    
    if (!token || !userEmail) {
        showAlert('Vui lòng đăng nhập để truy cập trang quản trị!', 'warning');
        setTimeout(() => {
            window.location.href = '/login?error=unauthorized';
        }, 1500);
        return false;
    }
    
    // Check if user has ADMIN role
    if (!userRole || !userRole.includes('ADMIN')) {
        showAlert('Bạn không có quyền truy cập trang này!', 'danger');
        setTimeout(() => {
            window.location.href = '/';
        }, 1500);
        return false;
    }
    
    // Update admin info display
    const userName = userEmail.split('@')[0];
    document.getElementById('adminName').textContent = userName.charAt(0).toUpperCase() + userName.slice(1);
    document.getElementById('adminEmail').textContent = userEmail;
    
    return true;
}

// Event listener for password change checkbox
document.getElementById('changePasswordCheck').addEventListener('change', function() {
    const passwordInputs = document.getElementById('passwordChangeInputs');
    const newPassword = document.getElementById('newPassword');
    const confirmNewPassword = document.getElementById('confirmNewPassword');
    
    if (this.checked) {
        passwordInputs.style.display = 'block';
        newPassword.required = true;
        confirmNewPassword.required = true;
    } else {
        passwordInputs.style.display = 'none';
        newPassword.required = false;
        confirmNewPassword.required = false;
        newPassword.value = '';
        confirmNewPassword.value = '';
    }
});

// Logout
function logout() {
    if (confirm('Bạn có chắc muốn đăng xuất?')) {
        localStorage.removeItem('authToken');
        localStorage.removeItem('token');
        localStorage.removeItem('authEmail');
        localStorage.removeItem('userEmail');
        localStorage.removeItem('userRole');
        showAlert('Đã đăng xuất thành công!', 'success');
        setTimeout(() => {
            window.location.href = '/login';
        }, 1000);
    }
}
