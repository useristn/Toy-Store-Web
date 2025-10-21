/*
 * auth.js
 *
 * This file contains client‑side helpers for interacting with the
 * authentication APIs exposed by the Toy Store backend. Each page
 * includes this script via a `<script>` tag with the `defer`
 * attribute so that the functions are available after the DOM
 * loads. Functions are organised by feature: registration,
 * account activation/verification, login, password reset and
 * profile management. All network calls use the Fetch API and
 * communicate with the Spring Boot backend using JSON. If a
 * function succeeds, it will update the DOM with a success
 * message or redirect the user to the appropriate page.
 */

const ctx = document.querySelector('meta[name="ctx"]')?.content || '/';
const base = ctx.endsWith('/') ? ctx.slice(0, -1) : ctx;
const apiBase = `${base}/api/auth`;

// Utility: display a message inside a container element
function displayMessage(containerId, message, isError = false) {
    const container = document.getElementById(containerId);
    if (container) {
        container.className = `alert ${isError ? 'alert-danger' : 'alert-success'}`;
        container.innerHTML = `${isError ? '😢' : '🎉'} ${message}`;
        container.classList.remove('d-none');
    }
}

// Registration handler
function handleRegister(event) {
    event.preventDefault();
    const email = document.getElementById('registerEmail').value.trim();
    const password = document.getElementById('registerPassword').value;
    const confirmPassword = document.getElementById('registerConfirmPassword').value;
    const role = document.getElementById('registerRole').value.trim();

    if (password !== confirmPassword) {
        displayMessage('registerMessage', 'Mật khẩu không khớp! Hãy thử lại nhé! 🔒', true);
        return;
    }

    fetch(`${apiBase}/register`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({email, password, role})
    }).then(async (response) => {
        const data = await response.json().catch(() => ({}));
        if (response.ok) {
            displayMessage('registerMessage', data.message || '🎉 Đăng ký thành công! Chào mừng phi hành gia mới! 🚀');
            // Lưu email vào session để trang xác thực có thể lấy
            sessionStorage.setItem('verifyEmail', email);
            // After registration, redirect to verification page so user can enter OTP
            setTimeout(() => {
                window.location.href = '/verify-otp';
            }, 1500);
        } else {
            displayMessage('registerMessage', data.message || '😢 Đăng ký không thành công. Hãy thử lại nhé!', true);
        }
    }).catch((error) => {
        displayMessage('registerMessage', '🛸 Có lỗi kết nối: ' + error.message, true);
    });
}

// Send activation OTP handler (resend)
function handleSendActivationOtp(event) {
    event.preventDefault();
    const email = document.getElementById('verifyEmail').value.trim();
    if (!email) {
        displayMessage('verifyMessage', 'Please enter your email', true);
        return;
    }
    fetch(`${apiBase}/active-account`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({email})
    }).then(async (response) => {
        const data = await response.json().catch(() => ({}));
        if (response.ok) {
            displayMessage('verifyMessage', data.message || 'OTP has been sent');
        } else {
            displayMessage('verifyMessage', data.message || 'Failed to send OTP', true);
        }
    }).catch((error) => {
        displayMessage('verifyMessage', 'Error: ' + error.message, true);
    });
}

// Account verification handler
function handleVerifyAccount(event) {
    event.preventDefault();
    const email = document.getElementById('verifyEmail').value.trim();
    const otp = document.getElementById('verifyOtp').value.trim();
    if (!email || !otp) {
        displayMessage('verifyMessage', 'Email and OTP are required', true);
        return;
    }
    fetch(`${apiBase}/verify-account`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({email, otp})
    }).then(async (response) => {
        const data = await response.json().catch(() => ({}));
        if (response.ok) {
            displayMessage('verifyMessage', data.message || 'Account verified');
            setTimeout(() => {
                window.location.href = '/login';
            }, 1500);
        } else {
            displayMessage('verifyMessage', data.message || 'Verification failed', true);
        }
    }).catch((error) => {
        displayMessage('verifyMessage', 'Error: ' + error.message, true);
    });
}

// Login handler
function handleLogin(event) {
    event.preventDefault();
    const email = document.getElementById('loginEmail').value.trim();
    const password = document.getElementById('loginPassword').value;
    
    fetch(`${apiBase}/login`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({email, password})
    }).then(async (response) => {
        const data = await response.json().catch(() => ({}));
        if (response.ok && data.token) {
            // Persist token and email for subsequent authenticated requests
            localStorage.setItem('authToken', data.token);
            localStorage.setItem('authEmail', data.email || email);
            displayMessage('loginMessage', data.message || '🚀 Đăng nhập thành công! Chào mừng trở lại phi hành gia! 🌟');
            setTimeout(() => {
                window.location.href = '/profile';
            }, 1500);
        } else {
            displayMessage('loginMessage', data.message || '😢 Đăng nhập không thành công. Kiểm tra email và mật khẩu nhé!', true);
        }
    }).catch((error) => {
        displayMessage('loginMessage', '🛸 Có lỗi kết nối: ' + error.message, true);
    });
}

// Forgot password handler
function handleForgotPassword(event) {
    event.preventDefault();
    const email = document.getElementById('forgotEmail').value.trim();
    if (!email) {
        displayMessage('forgotMessage', 'Please enter your email', true);
        return;
    }
    fetch(`${apiBase}/forgot-password`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({email})
    }).then(async (response) => {
        const data = await response.json().catch(() => ({}));
        if (response.ok) {
            displayMessage('forgotMessage', data.message || 'OTP sent to your email');
            // Lưu email vào session để trang reset có thể lấy
            sessionStorage.setItem('resetEmail', email);
            setTimeout(() => {
                window.location.href = '/reset-password';
            }, 1500);
        } else {
            displayMessage('forgotMessage', data.message || 'Failed to send OTP', true);
        }
    }).catch((error) => {
        displayMessage('forgotMessage', 'Error: ' + error.message, true);
    });
}

// Reset password handler
function handleResetPassword(event) {
    event.preventDefault();
    const email = document.getElementById('resetEmail').value.trim();
    const otp = document.getElementById('resetOtp').value.trim();
    const newPassword = document.getElementById('resetPassword').value;
    const confirmNewPassword = document.getElementById('resetConfirmPassword').value;
    if (newPassword !== confirmNewPassword) {
        displayMessage('resetMessage', 'Passwords do not match', true);
        return;
    }
    fetch(`${apiBase}/reset-password`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({email, otp, newPassword})
    }).then(async (response) => {
        const data = await response.json().catch(() => ({}));
        if (response.ok) {
            displayMessage('resetMessage', data.message || 'Password reset successfully');
            setTimeout(() => {
                window.location.href = '/login';
            }, 1500);
        } else {
            displayMessage('resetMessage', data.message || 'Failed to reset password', true);
        }
    }).catch((error) => {
        displayMessage('resetMessage', 'Error: ' + error.message, true);
    });
}

// Load user profile
function loadProfile() {
    const email = localStorage.getItem('authEmail');
    const token = localStorage.getItem('authToken');
    if (!email) {
        // Not logged in
        window.location.href = '/login';
        return;
    }
    const headers = {};
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }

    // Tạo một đối tượng để map các role
    const roleMap = {
        'ROLE_USER': 'Khách hàng',
        'ROLE_VENDOR': 'Người bán',
        'ROLE_SHIPPER': 'Người giao hàng',
        'ROLE_ADMIN': 'Quản trị viên',
        'SOLE_ADMIN': 'Quản trị viên' // Thêm cả trường hợp SOLE_ADMIN bạn đã viết
    };

    // Hàm helper để dịch role
    const getFriendlyRole = (role) => {
        return roleMap[role] || role; // Trả về giá trị đã map, hoặc chính role đó nếu không tìm thấy
    };

    fetch(`${apiBase}/user?email=${encodeURIComponent(email)}`, {
        method: 'GET',
        headers
    }).then(async (response) => {
        if (response.ok) {
            const data = await response.json();
            const infoDiv = document.getElementById('profileInfo');
            if (infoDiv) {
                infoDiv.innerHTML = '';
                const list = document.createElement('ul');
                const addItem = (label, value) => {
                    const li = document.createElement('li');
                    li.textContent = `${label}: ${value || ''}`;
                    list.appendChild(li);
                };
                addItem('Email', data.email);
                addItem('Name', data.name);
                addItem('Phone', data.phone);
                addItem('Address', data.address);

                // *** BẮT ĐẦU THAY ĐỔI ***
                if (data.roles) {
                    let friendlyRoles = '';
                    if (Array.isArray(data.roles)) {
                        // Nếu là mảng, map từng giá trị
                        friendlyRoles = data.roles.map(getFriendlyRole).join(', ');
                    } else {
                        // Nếu là một chuỗi đơn
                        friendlyRoles = getFriendlyRole(data.roles);
                    }
                    addItem('Roles', friendlyRoles);
                }
                // *** KẾT THÚC THAY ĐỔI ***

                infoDiv.appendChild(list);
                // Pre-fill form fields
                const nameField = document.getElementById('profileName');
                const phoneField = document.getElementById('profilePhone');
                const addressField = document.getElementById('profileAddress');
                if (nameField) nameField.value = data.name || '';
                if (phoneField) phoneField.value = data.phone || '';
                if (addressField) addressField.value = data.address || '';
            }
        } else {
            // If not found or unauthorized, redirect to login
            window.location.href = '/login';
        }
    }).catch(() => {
        window.location.href = '/login';
    });
}

// Update profile handler
function handleUpdateProfile(event) {
    event.preventDefault();
    const email = localStorage.getItem('authEmail');
    const token = localStorage.getItem('authToken');
    const name = document.getElementById('profileName').value.trim();
    const phone = document.getElementById('profilePhone').value.trim();
    const address = document.getElementById('profileAddress').value.trim();
    const headers = {'Content-Type': 'application/json'};
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }
    fetch(`${apiBase}/update-profile?email=${encodeURIComponent(email)}`, {
        method: 'PUT',
        headers,
        body: JSON.stringify({name, phone, address})
    }).then(async (response) => {
        const data = await response.json().catch(() => ({}));
        if (response.ok) {
            displayMessage('profileMessage', data.message || 'Profile updated successfully');
            // refresh the profile details after update
            loadProfile();
        } else {
            displayMessage('profileMessage', data.message || 'Failed to update profile', true);
        }
    }).catch((error) => {
        displayMessage('profileMessage', 'Error: ' + error.message, true);
    });
}

// Update auth UI based on login state
function updateAuthUI() {
    const token = localStorage.getItem('authToken');
    const email = localStorage.getItem('authEmail');
    const authButtons = document.getElementById('authButtons');
    const userMenu = document.getElementById('userMenu');
    const userName = document.getElementById('userName');

    if (token && email) {
        // User is logged in
        if (authButtons) authButtons.classList.add('d-none');
        if (userMenu) {
            userMenu.classList.remove('d-none');
            if (userName) {
                userName.textContent = `${email.split('@')[0]} 🚀`; // Show username part of email with rocket
            }
        }
    } else {
        // User is not logged in
        if (authButtons) authButtons.classList.remove('d-none');
        if (userMenu) userMenu.classList.add('d-none');
    }
}

// Newsletter subscription handler
function handleNewsletter(event) {
    event.preventDefault();
    const email = document.getElementById('newsletterEmail').value.trim();
    if (!email) {
        // Just show inline message, no alert
        return;
    }
    
    // Clear the email field to show success
    document.getElementById('newsletterEmail').value = '';
    // Could add a small success indicator here if needed
}

// Search handler
function handleSearch() {
    const searchInput = document.getElementById('searchInput');
    
    if (searchInput && searchInput.value.trim()) {
        const query = searchInput.value.trim();
        
        // Just perform search without alert - in real app, this would redirect to search results
        console.log(`🔍 Tìm kiếm: "${query}"`);
    }
}

// Enhanced logout handler
function handleLogout() {
    localStorage.removeItem('authToken');
    localStorage.removeItem('authEmail');
    updateAuthUI();
    window.location.href = '/';
}

// Attach event listeners once the DOM content is loaded on each page
document.addEventListener('DOMContentLoaded', () => {
    // Update auth UI on page load
    updateAuthUI();
    
    // Newsletter form
    const newsletterForm = document.getElementById('newsletterForm');
    if (newsletterForm) {
        newsletterForm.addEventListener('submit', handleNewsletter);
    }
    
    // Search functionality
    const searchBtn = document.getElementById('searchBtn');
    const searchInput = document.getElementById('searchInput');
    if (searchBtn) {
        searchBtn.addEventListener('click', handleSearch);
    }
    if (searchInput) {
        searchInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                handleSearch();
            }
        });
    }
    
    // Logout link in user menu
    const logoutLink = document.getElementById('logoutLink');
    if (logoutLink) {
        logoutLink.addEventListener('click', (e) => {
            e.preventDefault();
            handleLogout();
        });
    }
    
    // Category navigation
    document.querySelectorAll('[data-category]').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const category = e.target.closest('[data-category]').dataset.category;
            // Just log or redirect, no alert
            console.log(`🚀 Chuyển đến danh mục: ${category}! ✨`);
        });
    });

    // Register page
    const registerForm = document.getElementById('registerForm');
    if (registerForm) registerForm.addEventListener('submit', handleRegister);

    // Verify account page
    const verifyForm = document.getElementById('verifyForm');
    if (verifyForm) { // Chúng ta gộp logic cho trang verify vào đây
        verifyForm.addEventListener('submit', handleVerifyAccount);

        // Tự động điền email từ trang đăng ký
        const emailField = document.getElementById('verifyEmail');
        const emailFromRegister = sessionStorage.getItem('verifyEmail');

        if (emailField && emailFromRegister) {
            emailField.value = emailFromRegister;
            // Xóa khỏi session để không dùng lại
            sessionStorage.removeItem('verifyEmail');
        }
    }

    const resendOtpBtn = document.getElementById('resendOtpButton');
    if (resendOtpBtn) resendOtpBtn.addEventListener('click', handleSendActivationOtp);

    // Login page
    const loginForm = document.getElementById('loginForm');
    if (loginForm) loginForm.addEventListener('submit', handleLogin);

    // Forgot password page
    const forgotForm = document.getElementById('forgotForm');
    if (forgotForm) forgotForm.addEventListener('submit', handleForgotPassword);

    // Reset password page
    const resetForm = document.getElementById('resetForm');
    if (resetForm) resetForm.addEventListener('submit', handleResetPassword);

    // Tự động điền email từ trang quên mật khẩu
    const emailField = document.getElementById('resetEmail');
    const emailFromForgot = sessionStorage.getItem('resetEmail');

    if (emailField && emailFromForgot) {
        emailField.value = emailFromForgot;
        // Xóa khỏi session để không dùng lại
        sessionStorage.removeItem('resetEmail');
    }

    // Profile page
    const updateProfileForm = document.getElementById('updateProfileForm');
    if (updateProfileForm) updateProfileForm.addEventListener('submit', handleUpdateProfile);
    const logoutBtn = document.getElementById('logoutButton');
    if (logoutBtn) logoutBtn.addEventListener('click', handleLogout);
    // If on profile page, load profile data
    if (document.getElementById('profileInfo')) {
        loadProfile();
    }
});