document.addEventListener('DOMContentLoaded', function() {
    if (!checkShipperAuth()) {
        return; // Stop execution if not authenticated
    }
    
    // Initialize dashboard
    loadDashboardStats();
    loadAvailableOrders();
    
    // Auto refresh every 30 seconds for new orders notification
    setInterval(checkNewOrders, 30000);
});

// Check if user has SHIPPER role
function checkShipperAuth() {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    const userRole = localStorage.getItem('userRole');
    
    if (!token || !userEmail) {
        showToast('Vui lòng đăng nhập để truy cập trang shipper!', 'warning');
        setTimeout(() => {
            window.location.href = '/login?error=unauthorized';
        }, 1500);
        return false;
    }
    
    // Check if user has SHIPPER role
    if (!userRole || !userRole.includes('SHIPPER')) {
        showToast('Bạn không có quyền truy cập trang này!', 'danger');
        setTimeout(() => {
            window.location.href = '/login?error=access_denied';
        }, 1500);
        return false;
    }
    
    // Update shipper info in sidebar
    document.getElementById('shipperEmail').textContent = userEmail;
    document.getElementById('shipperName').textContent = userEmail.split('@')[0];
    
    return true;
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

// Refresh dashboard
function refreshDashboard() {
    showToast('Đang làm mới dữ liệu...', 'info');
    loadDashboardStats();
    loadAvailableOrders();
    loadActiveOrders();
}

// Load dashboard stats
async function loadDashboardStats() {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    if (!token || !userEmail) return;

    try {
        const response = await fetch('/api/shipper/stats', {
            headers: {
                'Authorization': `Bearer ${token}`,
                'X-User-Email': userEmail
            }
        });

        if (response.ok) {
            const stats = await response.json();
            
            // Update main stats cards
            animateCounter('availableOrders', stats.availableOrders || 0);
            animateCounter('activeDeliveries', stats.activeDeliveries || 0);
            animateCounter('completedToday', stats.completedDeliveries || 0);
            animateCounter('totalDeliveries', stats.totalDeliveries || 0);
            
            // Update detailed stats cards
            animateCounter('completedDeliveries', stats.completedDeliveries || 0);
            animateCounter('failedDeliveries', stats.failedDeliveries || 0);
            
            // Show notification if there are available orders
            if (stats.availableOrders > 0) {
                showNotification(`Có ${stats.availableOrders} đơn hàng đang chờ bạn nhận!`, 'info');
            }
        } else {
            console.error('Failed to load stats');
        }
    } catch (error) {
        console.error('Error loading stats:', error);
    }
}

// Load available orders (PROCESSING status)
async function loadAvailableOrders() {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    if (!token || !userEmail) return;

    try {
        const response = await fetch('/api/shipper/orders/available', {
            headers: {
                'Authorization': `Bearer ${token}`,
                'X-User-Email': userEmail
            }
        });

        if (response.ok) {
            const orders = await response.json();
            displayAvailableOrders(orders);
        } else {
            document.getElementById('availableOrdersTable').innerHTML = 
                '<tr><td colspan="7" class="text-center text-danger">Không thể tải danh sách đơn hàng</td></tr>';
        }
    } catch (error) {
        console.error('Error loading available orders:', error);
        document.getElementById('availableOrdersTable').innerHTML = 
            '<tr><td colspan="7" class="text-center text-danger">Lỗi kết nối</td></tr>';
    }
}

// Display available orders in table
function displayAvailableOrders(orders) {
    const tableBody = document.getElementById('availableOrdersTable');
    
    if (orders.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">Không có đơn hàng nào</td></tr>';
        return;
    }
    
    tableBody.innerHTML = orders.map(order => `
        <tr>
            <td><strong>${order.orderNumber}</strong></td>
            <td>${order.customerName}</td>
            <td>${order.customerPhone}</td>
            <td class="text-truncate" style="max-width: 200px;" title="${order.shippingAddress}">${order.shippingAddress}</td>
            <td><strong>${formatCurrency(order.totalAmount)}</strong></td>
            <td>${formatDateTime(order.createdAt)}</td>
            <td>
                <button class="btn btn-sm btn-primary" onclick="acceptOrder(${order.id})">
                    <i class="fas fa-hand-holding me-1"></i>Xác nhận đơn
                </button>
                <button class="btn btn-sm btn-outline-info" onclick="showOrderDetail(${order.id})">
                    <i class="fas fa-eye"></i>
                </button>
                <button class="btn btn-sm btn-outline-success" onclick="openGoogleMaps('${encodeURIComponent(order.shippingAddress)}')">
                    <i class="fas fa-map-marked-alt"></i>
                </button>
            </td>
        </tr>
    `).join('');
}

// Load active orders (SHIPPING status for this shipper)
async function loadActiveOrders() {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    if (!token || !userEmail) return;

    try {
        const response = await fetch('/api/shipper/orders/active', {
            headers: {
                'Authorization': `Bearer ${token}`,
                'X-User-Email': userEmail
            }
        });

        if (response.ok) {
            const orders = await response.json();
            displayActiveOrders(orders);
        } else {
            document.getElementById('activeOrdersTable').innerHTML = 
                '<tr><td colspan="7" class="text-center text-danger">Không thể tải danh sách đơn hàng</td></tr>';
        }
    } catch (error) {
        console.error('Error loading active orders:', error);
        document.getElementById('activeOrdersTable').innerHTML = 
            '<tr><td colspan="7" class="text-center text-danger">Lỗi kết nối</td></tr>';
    }
}

// Display active orders in table
function displayActiveOrders(orders) {
    const tableBody = document.getElementById('activeOrdersTable');
    
    if (orders.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">Bạn chưa có đơn hàng nào đang giao</td></tr>';
        return;
    }
    
    tableBody.innerHTML = orders.map(order => `
        <tr>
            <td><strong>${order.orderNumber}</strong></td>
            <td>${order.customerName}</td>
            <td>${order.customerPhone}</td>
            <td class="text-truncate" style="max-width: 200px;" title="${order.shippingAddress}">${order.shippingAddress}</td>
            <td><strong>${formatCurrency(order.totalAmount)}</strong></td>
            <td>${formatDateTime(order.createdAt)}</td>
            <td>
                <button class="btn btn-sm btn-success me-1" onclick="completeOrder(${order.id})">
                    <i class="fas fa-check me-1"></i>Đã giao
                </button>
                <button class="btn btn-sm btn-danger me-1" onclick="failOrder(${order.id})">
                    <i class="fas fa-times me-1"></i>Thất bại
                </button>
                <button class="btn btn-sm btn-outline-info me-1" onclick="showOrderDetail(${order.id})">
                    <i class="fas fa-eye"></i>
                </button>
                <button class="btn btn-sm btn-outline-success" onclick="openGoogleMaps('${encodeURIComponent(order.shippingAddress)}')">
                    <i class="fas fa-map-marked-alt"></i>
                </button>
            </td>
        </tr>
    `).join('');
}

// Load delivery history
async function loadHistory() {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    if (!token || !userEmail) return;

    try {
        const response = await fetch('/api/shipper/orders/history', {
            headers: {
                'Authorization': `Bearer ${token}`,
                'X-User-Email': userEmail
            }
        });

        if (response.ok) {
            const orders = await response.json();
            displayHistory(orders);
        } else {
            document.getElementById('historyTable').innerHTML = 
                '<tr><td colspan="6" class="text-center text-danger">Không thể tải lịch sử</td></tr>';
        }
    } catch (error) {
        console.error('Error loading history:', error);
        document.getElementById('historyTable').innerHTML = 
            '<tr><td colspan="6" class="text-center text-danger">Lỗi kết nối</td></tr>';
    }
}

// Display delivery history
function displayHistory(orders) {
    const tableBody = document.getElementById('historyTable');
    
    if (orders.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">Chưa có lịch sử giao hàng</td></tr>';
        return;
    }
    
    tableBody.innerHTML = orders.map(order => `
        <tr>
            <td><strong>${order.orderNumber}</strong></td>
            <td>${order.customerName}</td>
            <td class="text-truncate" style="max-width: 250px;" title="${order.shippingAddress}">${order.shippingAddress}</td>
            <td><strong>${formatCurrency(order.totalAmount)}</strong></td>
            <td>${getStatusBadge(order.status)}</td>
            <td>${formatDateTime(order.createdAt)}</td>
        </tr>
    `).join('');
}

// Accept order (PROCESSING -> SHIPPING)
async function acceptOrder(orderId) {
    if (!confirm('Bạn có chắc muốn nhận đơn hàng này?')) {
        return;
    }
    
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    if (!token || !userEmail) return;

    try {
        const response = await fetch(`/api/shipper/orders/${orderId}/accept`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${token}`,
                'X-User-Email': userEmail
            }
        });

        if (response.ok) {
            showToast('Đã nhận đơn hàng thành công!', 'success');
            // Refresh data
            loadDashboardStats();
            loadAvailableOrders();
            loadActiveOrders();
        } else {
            const error = await response.json();
            showToast(error.error || 'Không thể nhận đơn hàng', 'danger');
        }
    } catch (error) {
        console.error('Error accepting order:', error);
        showToast('Lỗi kết nối', 'danger');
    }
}

// Complete order (SHIPPING -> DELIVERED)
async function completeOrder(orderId) {
    if (!confirm('Xác nhận đã giao hàng thành công?')) {
        return;
    }
    
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    if (!token || !userEmail) return;

    try {
        const response = await fetch(`/api/shipper/orders/${orderId}/complete`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${token}`,
                'X-User-Email': userEmail
            }
        });

        if (response.ok) {
            showToast('Đã hoàn thành giao hàng!', 'success');
            // Refresh data
            loadDashboardStats();
            loadActiveOrders();
        } else {
            const error = await response.json();
            showToast(error.error || 'Không thể hoàn thành đơn hàng', 'danger');
        }
    } catch (error) {
        console.error('Error completing order:', error);
        showToast('Lỗi kết nối', 'danger');
    }
}

// Report delivery failure
async function failOrder(orderId) {
    const reason = prompt('Vui lòng nhập lý do giao hàng thất bại:', 'Không thể liên hệ khách hàng');
    
    if (!reason) {
        return; // User cancelled
    }
    
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    if (!token || !userEmail) return;

    try {
        const response = await fetch(`/api/shipper/orders/${orderId}/fail`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${token}`,
                'X-User-Email': userEmail,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ reason: reason })
        });

        if (response.ok) {
            showToast('Đã báo cáo giao hàng thất bại!', 'warning');
            // Refresh data
            loadDashboardStats();
            loadActiveOrders();
        } else {
            const error = await response.json();
            showToast(error.error || 'Không thể cập nhật đơn hàng', 'danger');
        }
    } catch (error) {
        console.error('Error reporting failure:', error);
        showToast('Lỗi kết nối', 'danger');
    }
}

// Open Google Maps with address
function openGoogleMaps(address) {
    const mapsUrl = `https://www.google.com/maps/search/?api=1&query=${address}`;
    window.open(mapsUrl, '_blank');
}

// Show order detail modal
function showOrderDetail(orderId) {
    // TODO: Implement order detail modal
    showToast('Chức năng chi tiết đơn hàng đang được phát triển', 'info');
}

// Check for new orders (real-time notification)
let lastAvailableCount = 0;
async function checkNewOrders() {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    if (!token || !userEmail) return;

    try {
        const response = await fetch('/api/shipper/stats', {
            headers: {
                'Authorization': `Bearer ${token}`,
                'X-User-Email': userEmail
            }
        });

        if (response.ok) {
            const stats = await response.json();
            
            if (stats.availableOrders > lastAvailableCount && lastAvailableCount > 0) {
                const newOrders = stats.availableOrders - lastAvailableCount;
                showToast(`🔔 Có ${newOrders} đơn hàng mới!`, 'success');
                playNotificationSound();
            }
            
            lastAvailableCount = stats.availableOrders;
        }
    } catch (error) {
        console.error('Error checking new orders:', error);
    }
}

// Play notification sound
function playNotificationSound() {
    // Simple beep using Web Audio API
    try {
        const audioContext = new (window.AudioContext || window.webkitAudioContext)();
        const oscillator = audioContext.createOscillator();
        const gainNode = audioContext.createGain();
        
        oscillator.connect(gainNode);
        gainNode.connect(audioContext.destination);
        
        oscillator.frequency.value = 800;
        oscillator.type = 'sine';
        
        gainNode.gain.setValueAtTime(0.3, audioContext.currentTime);
        gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.5);
        
        oscillator.start(audioContext.currentTime);
        oscillator.stop(audioContext.currentTime + 0.5);
    } catch (error) {
        console.log('Cannot play notification sound');
    }
}

// Show notification in notification area
function showNotification(message, type = 'info') {
    const notificationArea = document.getElementById('notificationArea');
    const alertClass = `alert-${type}`;
    const icon = type === 'info' ? 'info-circle' : type === 'success' ? 'check-circle' : 'exclamation-triangle';
    
    notificationArea.innerHTML = `
        <div class="alert ${alertClass} mb-0">
            <i class="fas fa-${icon} me-2"></i>
            ${message}
        </div>
    `;
}

// Tab navigation
function showTab(tabName) {
    // Hide all tabs
    document.querySelectorAll('.content-tab').forEach(tab => {
        tab.style.display = 'none';
    });
    
    // Remove active class from all nav links
    document.querySelectorAll('.sidebar .nav-link').forEach(link => {
        link.classList.remove('active');
    });
    
    // Show selected tab
    const tabs = {
        'dashboard': 'dashboardTab',
        'orders': 'ordersTab',
        'active': 'activeTab',
        'history': 'historyTab'
    };
    
    const tabId = tabs[tabName];
    if (tabId) {
        document.getElementById(tabId).style.display = 'block';
        
        // Set active nav link
        event.target.classList.add('active');
        
        // Load data for the tab
        switch(tabName) {
            case 'dashboard':
                loadDashboardStats();
                break;
            case 'orders':
                loadAvailableOrders();
                break;
            case 'active':
                loadActiveOrders();
                break;
            case 'history':
                loadHistory();
                break;
        }
    }
}

// Utility functions
function animateCounter(elementId, targetValue) {
    const element = document.getElementById(elementId);
    if (!element) return;
    
    const duration = 1000;
    const startValue = 0;
    const startTime = performance.now();
    
    function update(currentTime) {
        const elapsed = currentTime - startTime;
        const progress = Math.min(elapsed / duration, 1);
        
        const currentValue = Math.floor(startValue + (targetValue - startValue) * progress);
        element.textContent = currentValue;
        
        if (progress < 1) {
            requestAnimationFrame(update);
        }
    }
    
    requestAnimationFrame(update);
}

function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(amount);
}

function formatDateTime(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleString('vi-VN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function getStatusBadge(status) {
    const statusMap = {
        'PENDING': { text: 'Chờ xử lý', class: 'warning' },
        'CONFIRMED': { text: 'Đã xác nhận', class: 'info' },
        'PROCESSING': { text: 'Đang xử lý', class: 'primary' },
        'SHIPPING': { text: 'Đang giao', class: 'primary' },
        'DELIVERED': { text: 'Đã giao', class: 'success' },
        'FAILED': { text: 'Giao thất bại', class: 'danger' },
        'CANCELLED': { text: 'Đã hủy', class: 'danger' },
        'REFUNDED': { text: 'Đã hoàn tiền', class: 'secondary' }
    };
    
    const statusInfo = statusMap[status] || { text: status, class: 'secondary' };
    return `<span class="badge bg-${statusInfo.class}">${statusInfo.text}</span>`;
}

function showToast(message, type = 'info') {
    // Create toast element
    const toast = document.createElement('div');
    toast.className = `alert alert-${type} position-fixed top-0 end-0 m-3`;
    toast.style.zIndex = '9999';
    toast.style.minWidth = '300px';
    toast.innerHTML = message;
    
    document.body.appendChild(toast);
    
    // Remove after 3 seconds
    setTimeout(() => {
        toast.remove();
    }, 3000);
}
