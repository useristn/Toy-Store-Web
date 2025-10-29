/**
 * Profile Page JavaScript
 * Handles profile management, password change, and quick actions
 */

document.addEventListener('DOMContentLoaded', function() {
    loadProfile();
    setupEventListeners();
});

/**
 * Setup event listeners
 */
function setupEventListeners() {
    // Update profile form
    const updateForm = document.getElementById('updateProfileForm');
    if (updateForm) {
        updateForm.addEventListener('submit', handleUpdateProfile);
    }

    // Change password form
    const changePasswordForm = document.getElementById('changePasswordForm');
    if (changePasswordForm) {
        changePasswordForm.addEventListener('submit', handleChangePassword);
    }

    // Logout button
    const logoutButton = document.getElementById('logoutButton');
    if (logoutButton) {
        logoutButton.addEventListener('click', handleLogout);
    }
}

/**
 * Load user profile
 */
async function loadProfile() {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const email = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    if (!token || !email) {
        window.location.href = '/login';
        return;
    }

    const profileInfo = document.getElementById('profileInfo');
    
    try {
        const response = await fetch('/api/auth/profile', {
            headers: {
                'Authorization': `Bearer ${token}`,
                'X-User-Email': email
            }
        });

        if (!response.ok) {
            throw new Error('Failed to load profile');
        }

        const user = await response.json();
        
        // Display profile info
        profileInfo.innerHTML = `
            <div class="text-center mb-4">
                <div class="profile-avatar mx-auto mb-3" style="width: 100px; height: 100px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 50%; display: flex; align-items: center; justify-content: center;">
                    <i class="fas fa-user fa-3x text-white"></i>
                </div>
                <h4 class="mb-1">${user.name || 'Phi hành gia'}</h4>
                <p class="text-muted mb-0">${user.email}</p>
            </div>
            
            <div class="list-group list-group-flush">
                <div class="list-group-item d-flex justify-content-between align-items-center">
                    <span><i class="fas fa-envelope text-primary me-2"></i>Email</span>
                    <strong>${user.email}</strong>
                </div>
                <div class="list-group-item d-flex justify-content-between align-items-center">
                    <span><i class="fas fa-phone text-success me-2"></i>Điện thoại</span>
                    <strong>${user.phone || 'Chưa cập nhật'}</strong>
                </div>
                <div class="list-group-item d-flex justify-content-between align-items-center">
                    <span><i class="fas fa-map-marker-alt text-danger me-2"></i>Địa chỉ</span>
                    <strong>${user.address || 'Chưa cập nhật'}</strong>
                </div>
                <div class="list-group-item d-flex justify-content-between align-items-center">
                    <span><i class="fas fa-user-tag text-info me-2"></i>Vai trò</span>
                    <span class="badge bg-primary">${user.role || 'USER'}</span>
                </div>
            </div>
        `;

        // Fill update form
        document.getElementById('profileName').value = user.name || '';
        document.getElementById('profilePhone').value = user.phone || '';
        document.getElementById('profileAddress').value = user.address || '';

    } catch (error) {
        console.error('Error loading profile:', error);
        profileInfo.innerHTML = `
            <div class="alert alert-danger">
                <i class="fas fa-exclamation-triangle me-2"></i>
                Không thể tải thông tin hồ sơ. Vui lòng thử lại sau.
            </div>
        `;
    }
}

/**
 * Handle profile update
 */
async function handleUpdateProfile(e) {
    e.preventDefault();
    
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const email = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    const name = document.getElementById('profileName').value.trim();
    const phone = document.getElementById('profilePhone').value.trim();
    const address = document.getElementById('profileAddress').value.trim();
    
    const messageDiv = document.getElementById('profileMessage');
    const submitBtn = e.target.querySelector('button[type="submit"]');
    
    // Show loading
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang cập nhật...';
    
    try {
        const response = await fetch('/api/auth/profile', {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${token}`,
                'X-User-Email': email,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ name, phone, address })
        });

        if (!response.ok) {
            throw new Error('Failed to update profile');
        }

        // Show success message
        messageDiv.className = 'alert alert-success mt-3';
        messageDiv.innerHTML = '<i class="fas fa-check-circle me-2"></i>Cập nhật thông tin thành công!';
        messageDiv.style.display = 'block';
        
        // Reload profile
        loadProfile();
        
        // Hide message after 3 seconds
        setTimeout(() => {
            messageDiv.style.display = 'none';
        }, 3000);

    } catch (error) {
        console.error('Error updating profile:', error);
        messageDiv.className = 'alert alert-danger mt-3';
        messageDiv.innerHTML = '<i class="fas fa-exclamation-triangle me-2"></i>Không thể cập nhật thông tin. Vui lòng thử lại.';
        messageDiv.style.display = 'block';
    } finally {
        // Reset button
        submitBtn.disabled = false;
        submitBtn.innerHTML = '<i class="fas fa-save me-2"></i>Cập nhật hồ sơ';
    }
}

/**
 * Open change password modal
 */
function changePassword() {
    const modal = new bootstrap.Modal(document.getElementById('changePasswordModal'));
    modal.show();
    
    // Reset form
    document.getElementById('changePasswordForm').reset();
    document.getElementById('passwordMessage').style.display = 'none';
}

/**
 * Toggle password visibility
 */
function togglePassword(inputId) {
    const input = document.getElementById(inputId);
    const icon = event.target.closest('button').querySelector('i');
    
    if (input.type === 'password') {
        input.type = 'text';
        icon.classList.remove('fa-eye');
        icon.classList.add('fa-eye-slash');
    } else {
        input.type = 'password';
        icon.classList.remove('fa-eye-slash');
        icon.classList.add('fa-eye');
    }
}

/**
 * Handle password change
 */
async function handleChangePassword(e) {
    e.preventDefault();
    
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const email = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    const currentPassword = document.getElementById('currentPassword').value;
    const newPassword = document.getElementById('newPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;
    
    const messageDiv = document.getElementById('passwordMessage');
    const submitBtn = e.target.querySelector('button[type="submit"]');
    
    // Validate
    if (newPassword !== confirmPassword) {
        messageDiv.className = 'alert alert-danger';
        messageDiv.innerHTML = '<i class="fas fa-times-circle me-2"></i>Mật khẩu mới không khớp!';
        messageDiv.style.display = 'block';
        return;
    }
    
    if (newPassword.length < 6) {
        messageDiv.className = 'alert alert-danger';
        messageDiv.innerHTML = '<i class="fas fa-times-circle me-2"></i>Mật khẩu mới phải có ít nhất 6 ký tự!';
        messageDiv.style.display = 'block';
        return;
    }
    
    // Show loading
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang đổi mật khẩu...';
    
    try {
        const response = await fetch('/api/auth/change-password', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'X-User-Email': email,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                currentPassword,
                newPassword
            })
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to change password');
        }

        // Show success
        messageDiv.className = 'alert alert-success';
        messageDiv.innerHTML = '<i class="fas fa-check-circle me-2"></i>Đổi mật khẩu thành công!';
        messageDiv.style.display = 'block';
        
        // Close modal after 2 seconds
        setTimeout(() => {
            const modal = bootstrap.Modal.getInstance(document.getElementById('changePasswordModal'));
            modal.hide();
            document.getElementById('changePasswordForm').reset();
        }, 2000);

    } catch (error) {
        console.error('Error changing password:', error);
        messageDiv.className = 'alert alert-danger';
        messageDiv.innerHTML = `<i class="fas fa-exclamation-triangle me-2"></i>${error.message || 'Không thể đổi mật khẩu. Vui lòng kiểm tra lại mật khẩu hiện tại.'}`;
        messageDiv.style.display = 'block';
    } finally {
        // Reset button
        submitBtn.disabled = false;
        submitBtn.innerHTML = '<i class="fas fa-save me-2"></i>Đổi mật khẩu';
    }
}

/**
 * Handle logout
 */
function handleLogout() {
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

/**
 * Show toast message
 */
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

// Make functions available globally
window.changePassword = changePassword;
window.togglePassword = togglePassword;
