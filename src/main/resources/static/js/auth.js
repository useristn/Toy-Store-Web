// Check authentication status on page load
document.addEventListener('DOMContentLoaded', function() {
    checkAuthStatus();
    updateCartBadge();
});

function checkAuthStatus() {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    const userRole = localStorage.getItem('userRole');

    const authButtons = document.getElementById('authButtons');
    const userMenu = document.getElementById('userMenu');
    const userName = document.getElementById('userName');

    console.log('Checking auth:', { token: !!token, email: userEmail, role: userRole });

    if (token && userEmail) {
        // User is logged in
        if (authButtons) authButtons.classList.add('d-none');
        if (userMenu) {
            userMenu.classList.remove('d-none');
            if (userName) {
                userName.textContent = userEmail.split('@')[0];
            }
        }

        // Show admin menu if user is admin
        if (userRole && userRole.includes('ADMIN')) {
            const adminMenuLink = document.getElementById('adminMenuLink');
            const adminDashboardLink = document.getElementById('adminDashboardLink');
            if (adminMenuLink) adminMenuLink.classList.remove('d-none');
            if (adminDashboardLink) adminDashboardLink.classList.remove('d-none');
        }
    } else {
        // User is not logged in
        if (authButtons) authButtons.classList.remove('d-none');
        if (userMenu) userMenu.classList.add('d-none');
    }

    // Setup logout handler
    const logoutLink = document.getElementById('logoutLink');
    if (logoutLink) {
        logoutLink.addEventListener('click', function(e) {
            e.preventDefault();
            // Remove both old and new token keys
            localStorage.removeItem('authToken');
            localStorage.removeItem('authEmail');
            localStorage.removeItem('token');
            localStorage.removeItem('userEmail');
            localStorage.removeItem('userRole');
            showMessage('loginMessage', 'Đã đăng xuất! Hẹn gặp lại phi hành gia! 👋', 'success');
            setTimeout(() => {
                window.location.href = '/';
            }, 1000);
        });
    }
}

// Determine the base path for the API from a meta tag or default to '/'
const ctx = document.querySelector('meta[name="ctx"]')?.content || '/';
const base = ctx.endsWith('/') ? ctx.slice(0, -1) : ctx;
const apiBase = `${base}/api/auth`;

/**
 * Displays a success or error message in a specified container.
 * @param {string} containerId - The ID of the DOM element to display the message in.
 * @param {string} message - The text message to display.
 * @param {boolean} [isError=false] - Toggles error (red) or success (green) styling.
 */
function displayMessage(containerId, message, isError = false) {
    const container = document.getElementById(containerId);
    if (container) {
        container.className = `alert ${isError ? 'alert-danger' : 'alert-success'}`;
        container.innerHTML = `${isError ? '😢' : '🎉'} ${message}`;
        container.classList.remove('d-none');
    }
}

/**
 * Handles the user registration form submission.
 * Validates passwords and sends registration data to the API.
 * On success, stores the email for verification and redirects to the OTP page.
 * @param {Event} event - The form submission event.
 */
function handleRegister(event) {
    event.preventDefault();
    const email = document.getElementById('registerEmail').value.trim();
    const password = document.getElementById('registerPassword').value;
    const confirmPassword = document.getElementById('registerConfirmPassword').value;
    const role = document.getElementById('registerRole').value.trim();

    if (password !== confirmPassword) {
        displayMessage('registerMessage', 'Mật khẩu không khớp. Vui lòng kiểm tra lại.', true);
        return;
    }

    fetch(`${apiBase}/register`, {
        method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({email, password, role})
    }).then(async (response) => {
        const data = await response.json().catch(() => ({}));
        if (response.ok) {
            displayMessage('registerMessage', data.message || 'Đăng ký thành công! Mã OTP đã được gửi đến email của bạn.');
            // Store email in session to pre-fill on the verification page.
            sessionStorage.setItem('verifyEmail', email);
            // Redirect to verification page.
            setTimeout(() => {
                window.location.href = '/verify-otp';
            }, 1500);
        } else {
            displayMessage('registerMessage', data.message || 'Đăng ký không thành công. Vui lòng thử lại.', true);
        }
    }).catch((error) => {
        displayMessage('registerMessage', 'Lỗi kết nối: ' + error.message, true);
    });
}

/**
 * Handles the "Resend OTP" request for account activation.
 * @param {Event} event - The button click event.
 */
function handleSendActivationOtp(event) {
    event.preventDefault();
    const email = document.getElementById('verifyEmail').value.trim();
    if (!email) {
        displayMessage('verifyMessage', 'Vui lòng nhập email của bạn.', true);
        return;
    }
    fetch(`${apiBase}/active-account`, {
        method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({email})
    }).then(async (response) => {
        const data = await response.json().catch(() => ({}));
        if (response.ok) {
            displayMessage('verifyMessage', data.message || 'Mã OTP đã được gửi. Vui lòng kiểm tra email.');
        } else {
            displayMessage('verifyMessage', data.message || 'Gửi lại OTP thất bại. Vui lòng thử lại.', true);
        }
    }).catch((error) => {
        displayMessage('verifyMessage', 'Lỗi: ' + error.message, true);
    });
}

/**
 * Handles the account verification (OTP) form submission.
 * On success, redirects to the login page.
 * @param {Event} event - The form submission event.
 */
function handleVerifyAccount(event) {
    event.preventDefault();
    const email = document.getElementById('verifyEmail').value.trim();
    const otp = document.getElementById('verifyOtp').value.trim();
    if (!email || !otp) {
        displayMessage('verifyMessage', 'Vui lòng nhập đầy đủ Email và OTP.', true);
        return;
    }
    fetch(`${apiBase}/verify-account`, {
        method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({email, otp})
    }).then(async (response) => {
        const data = await response.json().catch(() => ({}));
        if (response.ok) {
            displayMessage('verifyMessage', data.message || 'Xác thực tài khoản thành công! Đang chuyển đến trang đăng nhập...');
            setTimeout(() => {
                window.location.href = '/login';
            }, 1500);
        } else {
            displayMessage('verifyMessage', data.message || 'Xác thực thất bại. OTP không chính xác hoặc đã hết hạn.', true);
        }
    }).catch((error) => {
        displayMessage('verifyMessage', 'Lỗi: ' + error.message, true);
    });
}

/**
 * Handles the user login form submission.
 * On success, stores auth token/email in localStorage and redirects to profile.
 * If login fails due to inactive account, it triggers the resend-OTP flow.
 * @param {Event} event - The form submission event.
 */
function handleLogin(event) {
    event.preventDefault();
    const email = document.getElementById('loginEmail').value.trim();
    const password = document.getElementById('loginPassword').value;

    fetch(`${apiBase}/login`, {
        method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({email, password})
    }).then(async (response) => {
        const data = await response.json().catch(() => ({}));

        if (response.ok && data.token) {
            // 1. Handle successful login
            localStorage.setItem('authToken', data.token);
            localStorage.setItem('authEmail', data.email || email);
            
            // Store user role if provided
            if (data.role) {
                localStorage.setItem('userRole', data.role);
            }
            
            // Check if user is admin and redirect accordingly
            if (data.role && data.role.includes('ADMIN')) {
                displayMessage('loginMessage', 'Đăng nhập Admin thành công! Đang chuyển đến trang quản trị...');
                setTimeout(() => {
                    window.location.href = '/admin';
                }, 1500);
            } else {
                displayMessage('loginMessage', data.message || 'Đăng nhập thành công! Đang chuyển đến trang cá nhân...');
                setTimeout(() => {
                    window.location.href = '/profile';
                }, 1500);
            }
        } else {
            // 2. Handle login failure
            // Check for the specific "Account not activated" message from the backend.
            // (Assumes backend returns this specific string)
            const isInactive = data.message && data.message.includes("Account not activated");

            if (isInactive) {
                // If inactive, trigger the activation flow automatically.
                displayMessage('loginMessage', 'Tài khoản chưa kích hoạt. Đang tự động gửi lại mã OTP...', true);

                // Call the resend OTP API
                fetch(`${apiBase}/active-account`, {
                    method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({email})
                }).then(async (otpResponse) => {
                    const otpData = await otpResponse.json().catch(() => ({}));
                    if (otpResponse.ok) {
                        // On successful resend, redirect to verification page
                        displayMessage('loginMessage', otpData.message || 'Đã gửi lại OTP. Vui lòng kiểm tra email để kích hoạt tài khoản.');
                        sessionStorage.setItem('verifyEmail', email); // Pre-fill email on verify page
                        setTimeout(() => {
                            window.location.href = '/verify-otp';
                        }, 2000);
                    } else {
                        // Handle failure to resend OTP
                        displayMessage('loginMessage', otpData.message || 'Không thể tự động gửi lại OTP. Vui lòng thử lại sau.', true);
                    }
                }).catch((error) => {
                    displayMessage('loginMessage', 'Lỗi khi yêu cầu OTP: ' + error.message, true);
                });

            } else {
                // If it's a different error (e.g., wrong password, user not found)
                displayMessage('loginMessage', data.message || 'Đăng nhập thất bại. Vui lòng kiểm tra lại email hoặc mật khẩu.', true);
            }
        }
    }).catch((error) => {
        displayMessage('loginMessage', 'Lỗi kết nối: ' + error.message, true);
    });
}

/**
 * Handles the "Forgot Password" form submission.
 * On success, stores email and redirects to the reset password page.
 * @param {Event} event - The form submission event.
 */
function handleForgotPassword(event) {
    event.preventDefault();
    const email = document.getElementById('forgotEmail').value.trim();
    if (!email) {
        displayMessage('forgotMessage', 'Vui lòng nhập email của bạn.', true);
        return;
    }
    fetch(`${apiBase}/forgot-password`, {
        method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({email})
    }).then(async (response) => {
        const data = await response.json().catch(() => ({}));
        if (response.ok) {
            displayMessage('forgotMessage', data.message || 'Đã gửi mã OTP. Vui lòng kiểm tra email để đặt lại mật khẩu.');
            // Store email in session to pre-fill on the reset page.
            sessionStorage.setItem('resetEmail', email);
            setTimeout(() => {
                window.location.href = '/reset-password';
            }, 1500);
        } else {
            displayMessage('forgotMessage', data.message || 'Gửi OTP thất bại. Vui lòng thử lại.', true);
        }
    }).catch((error) => {
        displayMessage('forgotMessage', 'Lỗi: ' + error.message, true);
    });
}

/**
 * Handles the "Reset Password" form submission with email, OTP, and new password.
 * On success, redirects to the login page.
 * @param {Event} event - The form submission event.
 */
function handleResetPassword(event) {
    event.preventDefault();
    const email = document.getElementById('resetEmail').value.trim();
    const otp = document.getElementById('resetOtp').value.trim();
    const newPassword = document.getElementById('resetPassword').value;
    const confirmNewPassword = document.getElementById('resetConfirmPassword').value;
    if (newPassword !== confirmNewPassword) {
        displayMessage('resetMessage', 'Mật khẩu mới không khớp. Vui lòng nhập lại.', true);
        return;
    }
    fetch(`${apiBase}/reset-password`, {
        method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({email, otp, newPassword})
    }).then(async (response) => {
        const data = await response.json().catch(() => ({}));
        if (response.ok) {
            displayMessage('resetMessage', data.message || 'Đặt lại mật khẩu thành công! Đang chuyển đến trang đăng nhập...');
            setTimeout(() => {
                window.location.href = '/login';
            }, 1500);
        } else {
            displayMessage('resetMessage', data.message || 'Đặt lại mật khẩu thất bại. OTP có thể sai hoặc đã hết hạn.', true);
        }
    }).catch((error) => {
        displayMessage('resetMessage', 'Lỗi: ' + error.message, true);
    });
}

/**
 * Fetches and displays the current user's profile information.
 * Reads auth email/token from localStorage for the API request.
 * If not logged in, redirects to the login page.
 * Also pre-fills the "Update Profile" form fields.
 */
function loadProfile() {
    const email = localStorage.getItem('authEmail');
    const token = localStorage.getItem('authToken');

    // User must be logged in to view profile.
    if (!email) {
        window.location.href = '/login';
        return;
    }

    // Prepare authorization header.
    const headers = {};
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }

    // Map technical role names to user-friendly, localized strings.
    const roleMap = {
        'ROLE_USER': 'Khách hàng',
        'ROLE_VENDOR': 'Người bán',
        'ROLE_SHIPPER': 'Người giao hàng',
        'ROLE_ADMIN': 'Quản trị viên'
    };

    /**
     * Helper to translate a single role.
     * @param {string} role - The technical role name (e.g., "ROLE_USER").
     * @returns {string} The friendly role name (e.g., "Khách hàng").
     */
    const getFriendlyRole = (role) => {
        return roleMap[role] || role; // Fallback to the original role if not in map
    };

    fetch(`${apiBase}/user?email=${encodeURIComponent(email)}`, {
        method: 'GET', headers
    }).then(async (response) => {
        if (response.ok) {
            const data = await response.json();
            const infoDiv = document.getElementById('profileInfo');

            // Populate the static profile info list
            if (infoDiv) {
                infoDiv.innerHTML = ''; // Clear existing info

                // Create a table for structured key-value pair alignment.
                // We will use three columns: Label, Colon, Value.
                const table = document.createElement('table');
                table.className = 'profile-table';

                // Set border spacing for a cleaner look (inline style)
                table.style.borderSpacing = '0 0.25rem';
                table.style.borderCollapse = 'separate';

                const tbody = document.createElement('tbody');
                table.appendChild(tbody);

                /**
                 * Helper function to add a new row (<tr>) to the table.
                 * Each row now contains three cells (<td>) to ensure
                 * all colons (:) are perfectly aligned vertically in the second cell.
                 * @param {string} label - The descriptive label (e.g., "Name").
                 * @param {string} value - The user's data (e.g., "John Doe").
                 */
                const addItem = (label, value) => {
                    const tr = document.createElement('tr');

                    // --- Cell 1: The Label (e.g., "Họ và tên") ---
                    const tdLabel = document.createElement('td');
                    // Right-align text for a clean look next to the colon
                    tdLabel.style.textAlign = 'right';
                    tdLabel.style.verticalAlign = 'top';
                    tdLabel.style.paddingRight = '0.5rem'; // Space before the colon
                    const strong = document.createElement('strong');
                    strong.textContent = label; // Label text *without* the colon
                    tdLabel.appendChild(strong);

                    // --- Cell 2: The Colon (:) ---
                    const tdColon = document.createElement('td');
                    tdColon.style.verticalAlign = 'top';
                    tdColon.textContent = ':'; // Only the colon

                    // --- Cell 3: The Value (e.g., "John Doe") ---
                    const tdValue = document.createElement('td');
                    tdValue.style.verticalAlign = 'top';
                    tdValue.style.paddingLeft = '1rem'; // Space after the colon
                    // Provide a fallback text if the value is empty
                    tdValue.textContent = value || '(Chưa cập nhật)';

                    // Append all three cells to the row
                    tr.appendChild(tdLabel);
                    tr.appendChild(tdColon);
                    tr.appendChild(tdValue);

                    // Append the row to the table body
                    tbody.appendChild(tr);
                };

                // Add all profile items as table rows
                addItem('Tài khoản', data.email);
                addItem('Họ tên', data.name);
                addItem('Liên lạc', data.phone);
                addItem('Địa chỉ', data.address);

                // Handle role display (single string or array)
                if (data.roles) {
                    let friendlyRoles = '';
                    if (Array.isArray(data.roles)) {
                        // Map all roles to their friendly names and join with a comma
                        friendlyRoles = data.roles.map(getFriendlyRole).join(', ');
                    } else {
                        // Translate the single role
                        friendlyRoles = getFriendlyRole(data.roles);
                    }
                    addItem('Vai trò', friendlyRoles);
                }

                // Append the completed table to the main info container
                infoDiv.appendChild(table);

                // Pre-fill the editable update form
                const nameField = document.getElementById('profileName');
                const phoneField = document.getElementById('profilePhone');
                const addressField = document.getElementById('profileAddress');
                if (nameField) nameField.value = data.name || '';
                if (phoneField) phoneField.value = data.phone || '';
                if (addressField) addressField.value = data.address || '';
            }
        } else {
            // If token is invalid or user not found, redirect to login.
            window.location.href = '/login';
        }
    }).catch(() => {
        // Handle network errors
        window.location.href = '/login';
    });
}

/**
 * Handles the "Update Profile" form submission.
 * Sends updated name, phone, and address to the API.
 * @param {Event} event - The form submission event.
 */
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
        method: 'PUT', headers, body: JSON.stringify({name, phone, address})
    }).then(async (response) => {
        const data = await response.json().catch(() => ({}));
        if (response.ok) {
            displayMessage('profileMessage', data.message || 'Cập nhật hồ sơ thành công.');
            // Refresh the displayed profile info
            loadProfile();
        } else {
            displayMessage('profileMessage', data.message || 'Cập nhật hồ sơ thất bại. Vui lòng thử lại.', true);
        }
    }).catch((error) => {
        displayMessage('profileMessage', 'Lỗi: ' + error.message, true);
    });
}

/**
 * Updates the navigation/header UI based on the user's login state.
 * Shows/hides login/register buttons vs. the user profile menu.
 */
function updateAuthUI() {
    const token = localStorage.getItem('authToken');
    const email = localStorage.getItem('authEmail');
    const authButtons = document.getElementById('authButtons');
    const userMenu = document.getElementById('userMenu');
    const userName = document.getElementById('userName');

    if (token && email) {
        // User is logged in: Hide auth buttons, show user menu
        if (authButtons) authButtons.classList.add('d-none');
        if (userMenu) {
            userMenu.classList.remove('d-none');
            if (userName) {
                // Display username part of email
                userName.textContent = `${email.split('@')[0]} 🚀`;
            }
        }
    } else {
        // User is not logged in: Show auth buttons, hide user menu
        if (authButtons) authButtons.classList.remove('d-none');
        if (userMenu) userMenu.classList.add('d-none');
    }
}

/**
 * Handles the newsletter subscription form submission.
 * @param {Event} event - The form submission event.
 */
function handleNewsletter(event) {
    event.preventDefault();
    const email = document.getElementById('newsletterEmail').value.trim();
    if (!email) {
        return;
    }
    // Placeholder: In a real app, this would send to a newsletter API
    // For now, just clear the field as a visual confirmation
    document.getElementById('newsletterEmail').value = '';
}

/**
 * Handles the search button click or Enter keypress.
 * (Currently just logs the query to the console).
 */
function handleSearch() {
    const searchInput = document.getElementById('searchInput');
    if (searchInput && searchInput.value.trim()) {
        const query = searchInput.value.trim();
        // Placeholder: In a real app, redirect to a search results page
        console.log(`🔍 Đang tìm kiếm: "${query}"`);
    }
}

/**
 * Logs the user out by clearing localStorage.
 * Updates the UI and redirects to the homepage.
 */
function handleLogout() {
    localStorage.removeItem('authToken');
    localStorage.removeItem('authEmail');
    updateAuthUI(); // Refresh header
    window.location.href = '/'; // Redirect to home
}

/**
 * Main entry point. Attaches all event listeners after the DOM is fully loaded.
 */
document.addEventListener('DOMContentLoaded', () => {

    // --- Global UI ---
    updateAuthUI(); // Set initial header state

    const newsletterForm = document.getElementById('newsletterForm');
    if (newsletterForm) {
        newsletterForm.addEventListener('submit', handleNewsletter);
    }

    const searchBtn = document.getElementById('searchBtn');
    if (searchBtn) {
        searchBtn.addEventListener('click', handleSearch);
    }

    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                handleSearch();
            }
        });
    }

    const logoutLink = document.getElementById('logoutLink');
    if (logoutLink) {
        logoutLink.addEventListener('click', (e) => {
            e.preventDefault();
            handleLogout();
        });
    }

    // --- Category Navigation (Placeholder) ---
    document.querySelectorAll('[data-category]').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const category = e.target.closest('[data-category]').dataset.category;
            console.log(`🚀 Chuyển đến danh mục: ${category}! ✨`);
            // In a real app: window.location.href = `/products?category=${category}`;
        });
    });

    // --- Auth Pages ---

    // Register page
    const registerForm = document.getElementById('registerForm');
    if (registerForm) {
        registerForm.addEventListener('submit', handleRegister);
    }

    // Verify account page
    const verifyForm = document.getElementById('verifyForm');
    if (verifyForm) {
        verifyForm.addEventListener('submit', handleVerifyAccount);

        // Auto-fill email from registration flow.
        const emailField = document.getElementById('verifyEmail');
        const emailFromRegister = sessionStorage.getItem('verifyEmail');
        if (emailField && emailFromRegister) {
            emailField.value = emailFromRegister;
            // Clear the session item after use to prevent stale data
            sessionStorage.removeItem('verifyEmail');
        }
    }

    const resendOtpBtn = document.getElementById('resendOtpButton');
    if (resendOtpBtn) {
        resendOtpBtn.addEventListener('click', handleSendActivationOtp);
    }

    // Login page
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', handleLogin);
    }

    // Forgot password page
    const forgotForm = document.getElementById('forgotForm');
    if (forgotForm) {
        forgotForm.addEventListener('submit', handleForgotPassword);
    }

    // Reset password page
    const resetForm = document.getElementById('resetForm');
    if (resetForm) {
        resetForm.addEventListener('submit', handleResetPassword);

        // Auto-fill email from "forgot password" flow.
        const emailField = document.getElementById('resetEmail');
        const emailFromForgot = sessionStorage.getItem('resetEmail');
        if (emailField && emailFromForgot) {
            emailField.value = emailFromForgot;
            // Clear the session item after use
            sessionStorage.removeItem('resetEmail');
        }
    }

    // --- Profile Page ---

    // Attach update form handler
    const updateProfileForm = document.getElementById('updateProfileForm');
    if (updateProfileForm) {
        updateProfileForm.addEventListener('submit', handleUpdateProfile);
    }

    // Attach logout button (if one exists outside the main nav)
    const logoutBtn = document.getElementById('logoutButton');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', handleLogout);
    }

    // Load profile data if on the profile page
    if (document.getElementById('profileInfo')) {
        loadProfile();
    }
});

// Update cart badge with number of items
async function updateCartBadge() {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    const cartBadge = document.getElementById('cartBadge');
    
    if (!token || !userEmail || !cartBadge) {
        return;
    }

    try {
        const response = await fetch('/api/cart', {
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
                'X-User-Email': userEmail
            }
        });

        if (response.ok) {
            const cart = await response.json();
            const totalItems = cart.totalItems || 0;
            
            // Update badge text (show 99+ if more than 99)
            cartBadge.textContent = totalItems > 99 ? '99+' : totalItems;
            
            // Show/hide badge and add pulse animation
            if (totalItems > 0) {
                cartBadge.classList.remove('d-none');
                
                // Add pulse animation
                cartBadge.style.animation = 'none';
                setTimeout(() => {
                    cartBadge.style.animation = 'pulse 0.5s ease-in-out';
                }, 10);
            } else {
                cartBadge.classList.add('d-none');
            }
        }
    } catch (error) {
        console.error('Error updating cart badge:', error);
    }
}
