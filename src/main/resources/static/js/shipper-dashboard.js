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
            
            // Calculate and display success rate
            const totalCompleted = (stats.completedDeliveries || 0) + (stats.failedDeliveries || 0);
            const successRate = totalCompleted > 0 
                ? ((stats.completedDeliveries || 0) / totalCompleted * 100).toFixed(1) 
                : 0;
            document.getElementById('successRate').textContent = successRate + '%';
            
            // Update progress bar
            const successPercent = successRate;
            const progressBar = document.getElementById('successProgress');
            if (progressBar) {
                progressBar.style.width = successPercent + '%';
            }
            
            // Calculate estimated earnings (example: 20,000 VND per successful delivery)
            const earningsPerDelivery = 20000;
            const estimatedEarnings = (stats.completedDeliveries || 0) * earningsPerDelivery;
            document.getElementById('estimatedEarnings').textContent = formatCurrency(estimatedEarnings);
            
            // Update alerts
            updateAlerts(stats);
            
            // Update charts
            updateDeliveryChart(stats);
            updateStatusChart(stats);
            
            // Load recent activity
            loadRecentActivity();
            
        } else {
            console.error('Failed to load stats');
        }
    } catch (error) {
        console.error('Error loading stats:', error);
    }
}

// Update alerts based on stats
function updateAlerts(stats) {
    let hasAlerts = false;
    
    // Check for available orders
    if (stats.availableOrders > 0) {
        document.getElementById('availableCount').textContent = stats.availableOrders;
        document.getElementById('availableOrderAlert').classList.remove('d-none');
        document.getElementById('noAlerts').classList.add('d-none');
        hasAlerts = true;
    } else {
        document.getElementById('availableOrderAlert').classList.add('d-none');
    }
    
    // Check for active deliveries
    if (stats.activeDeliveries > 0) {
        document.getElementById('activeCount').textContent = stats.activeDeliveries;
        document.getElementById('activeOrderAlert').classList.remove('d-none');
        document.getElementById('noAlerts').classList.add('d-none');
        hasAlerts = true;
    } else {
        document.getElementById('activeOrderAlert').classList.add('d-none');
    }
    
    // Show no alerts message if no alerts
    if (!hasAlerts) {
        document.getElementById('noAlerts').classList.remove('d-none');
    }
}

// Chart instances
let deliveryChartInstance = null;
let statusChartInstance = null;

// Update delivery chart
function updateDeliveryChart(stats) {
    const ctx = document.getElementById('deliveryChart');
    if (!ctx) return;
    
    // Destroy existing chart
    if (deliveryChartInstance) {
        deliveryChartInstance.destroy();
    }
    
    deliveryChartInstance = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: ['Có thể nhận', 'Đang giao', 'Đã giao hôm nay', 'Giao thành công', 'Giao thất bại'],
            datasets: [{
                label: 'Số đơn hàng',
                data: [
                    stats.availableOrders || 0,
                    stats.activeDeliveries || 0,
                    stats.completedDeliveries || 0,
                    stats.completedDeliveries || 0,
                    stats.failedDeliveries || 0
                ],
                backgroundColor: [
                    'rgba(54, 185, 204, 0.8)',
                    'rgba(78, 115, 223, 0.8)',
                    'rgba(28, 200, 138, 0.8)',
                    'rgba(28, 200, 138, 0.8)',
                    'rgba(231, 74, 59, 0.8)'
                ],
                borderColor: [
                    'rgb(54, 185, 204)',
                    'rgb(78, 115, 223)',
                    'rgb(28, 200, 138)',
                    'rgb(28, 200, 138)',
                    'rgb(231, 74, 59)'
                ],
                borderWidth: 2
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            return context.label + ': ' + context.parsed.y + ' đơn';
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        stepSize: 1
                    }
                }
            }
        }
    });
}

// Update status chart
function updateStatusChart(stats) {
    const ctx = document.getElementById('statusChart');
    if (!ctx) return;
    
    // Destroy existing chart
    if (statusChartInstance) {
        statusChartInstance.destroy();
    }
    
    const totalCompleted = (stats.completedDeliveries || 0);
    const totalFailed = (stats.failedDeliveries || 0);
    const totalActive = (stats.activeDeliveries || 0);
    
    statusChartInstance = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['Thành công', 'Thất bại', 'Đang giao'],
            datasets: [{
                data: [totalCompleted, totalFailed, totalActive],
                backgroundColor: [
                    'rgba(28, 200, 138, 0.8)',
                    'rgba(231, 74, 59, 0.8)',
                    'rgba(78, 115, 223, 0.8)'
                ],
                borderColor: [
                    'rgb(28, 200, 138)',
                    'rgb(231, 74, 59)',
                    'rgb(78, 115, 223)'
                ],
                borderWidth: 2
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: {
                        padding: 15,
                        font: {
                            size: 12
                        }
                    }
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            const label = context.label || '';
                            const value = context.parsed || 0;
                            const total = context.dataset.data.reduce((a, b) => a + b, 0);
                            const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : 0;
                            return label + ': ' + value + ' (' + percentage + '%)';
                        }
                    }
                }
            }
        }
    });
}

// Load recent activity
async function loadRecentActivity() {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    if (!token || !userEmail) return;

    try {
        const response = await fetch('/api/shipper/orders/history?limit=10', {
            headers: {
                'Authorization': `Bearer ${token}`,
                'X-User-Email': userEmail
            }
        });

        if (response.ok) {
            const orders = await response.json();
            displayRecentActivity(orders);
        } else {
            document.getElementById('activityTable').innerHTML = 
                '<tr><td colspan="4" class="text-center text-muted">Không có hoạt động gần đây</td></tr>';
        }
    } catch (error) {
        console.error('Error loading recent activity:', error);
        document.getElementById('activityTable').innerHTML = 
            '<tr><td colspan="4" class="text-center text-danger">Lỗi tải dữ liệu</td></tr>';
    }
}

// Display recent activity
function displayRecentActivity(orders) {
    const tableBody = document.getElementById('activityTable');
    
    if (orders.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="4" class="text-center text-muted">Chưa có hoạt động nào</td></tr>';
        return;
    }
    
    tableBody.innerHTML = orders.slice(0, 10).map(order => {
        const action = order.status === 'DELIVERED' ? 'Đã giao hàng' : 
                      order.status === 'FAILED' ? 'Giao hàng thất bại' : 
                      order.status === 'SHIPPING' ? 'Đang giao hàng' : 'Nhận đơn hàng';
        
        return `
            <tr>
                <td>${formatDateTime(order.updatedAt || order.createdAt)}</td>
                <td><strong>${order.orderNumber}</strong></td>
                <td>${action}</td>
                <td>${getStatusBadge(order.status)}</td>
            </tr>
        `;
    }).join('');
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
                    <i class="fas fa-check me-1"></i>Giao thành công
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
let allHistoryOrders = []; // Store all orders for filtering
let currentFilter = 'ALL'; // Current filter status

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
            allHistoryOrders = orders; // Store all orders
            updateHistoryStats(orders);
            filterHistory(currentFilter); // Apply current filter
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

// Update history statistics
function updateHistoryStats(orders) {
    const totalCount = orders.length;
    const successCount = orders.filter(o => o.status === 'DELIVERED').length;
    const failedCount = orders.filter(o => o.status === 'FAILED').length;
    
    // Count today's deliveries
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const todayCount = orders.filter(o => {
        const orderDate = new Date(o.createdAt);
        orderDate.setHours(0, 0, 0, 0);
        return orderDate.getTime() === today.getTime() && (o.status === 'DELIVERED' || o.status === 'FAILED');
    }).length;
    
    document.getElementById('historyTotalCount').textContent = totalCount;
    document.getElementById('historyTodayCount').textContent = todayCount;
    document.getElementById('historySuccessCount').textContent = successCount;
    document.getElementById('historyFailedCount').textContent = failedCount;
}

// Filter history by status
function filterHistory(status) {
    currentFilter = status;
    
    // Filter orders
    let filteredOrders = allHistoryOrders;
    if (status === 'DELIVERED') {
        filteredOrders = allHistoryOrders.filter(order => order.status === 'DELIVERED');
    } else if (status === 'FAILED') {
        filteredOrders = allHistoryOrders.filter(order => order.status === 'FAILED');
    } else if (status === 'TODAY') {
        // Filter today's deliveries (both success and failed)
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        filteredOrders = allHistoryOrders.filter(order => {
            const orderDate = new Date(order.createdAt);
            orderDate.setHours(0, 0, 0, 0);
            return orderDate.getTime() === today.getTime() && (order.status === 'DELIVERED' || order.status === 'FAILED');
        });
    }
    // If status === 'ALL', show all orders
    
    // Display filtered orders
    displayHistory(filteredOrders);
}

// Display delivery history
function displayHistory(orders) {
    const tableBody = document.getElementById('historyTable');
    
    if (orders.length === 0) {
        const filterText = currentFilter === 'DELIVERED' ? 'giao thành công' : 
                          currentFilter === 'FAILED' ? 'giao thất bại' : 
                          currentFilter === 'TODAY' ? 'giao thành công hôm nay' : '';
        const message = filterText ? `Không có đơn hàng ${filterText}` : 'Chưa có lịch sử giao hàng';
        tableBody.innerHTML = `<tr><td colspan="6" class="text-center text-muted">${message}</td></tr>`;
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
async function showOrderDetail(orderId) {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    if (!token || !userEmail) return;

    // Show modal
    const modal = new bootstrap.Modal(document.getElementById('orderDetailModal'));
    modal.show();
    
    // Show loading state
    document.getElementById('orderDetailContent').innerHTML = `
        <div class="text-center py-5">
            <span class="spinner-border spinner-border-lg text-primary" role="status"></span>
            <p class="mt-3 text-muted">Đang tải thông tin...</p>
        </div>
    `;

    try {
        const response = await fetch(`/api/shipper/orders/${orderId}`, {
            headers: {
                'Authorization': `Bearer ${token}`,
                'X-User-Email': userEmail
            }
        });

        if (response.ok) {
            const order = await response.json();
            displayOrderDetail(order);
        } else {
            document.getElementById('orderDetailContent').innerHTML = `
                <div class="alert alert-danger">
                    <i class="fas fa-exclamation-triangle me-2"></i>
                    Không thể tải thông tin đơn hàng
                </div>
            `;
        }
    } catch (error) {
        console.error('Error loading order detail:', error);
        document.getElementById('orderDetailContent').innerHTML = `
            <div class="alert alert-danger">
                <i class="fas fa-exclamation-triangle me-2"></i>
                Lỗi kết nối. Vui lòng thử lại!
            </div>
        `;
    }
}

// Display order detail in modal
function displayOrderDetail(order) {
    const content = `
        <!-- Order Info -->
        <div class="row mb-4">
            <div class="col-md-6">
                <div class="card border-left-primary">
                    <div class="card-body">
                        <h6 class="text-primary mb-3"><i class="fas fa-info-circle me-2"></i>Thông tin đơn hàng</h6>
                        <table class="table table-sm table-borderless mb-0">
                            <tr>
                                <td class="text-muted" width="40%">Mã đơn hàng:</td>
                                <td><strong>${order.orderNumber}</strong></td>
                            </tr>
                            <tr>
                                <td class="text-muted">Trạng thái:</td>
                                <td>${getStatusBadge(order.status)}</td>
                            </tr>
                            <tr>
                                <td class="text-muted">Ngày đặt:</td>
                                <td>${formatDateTime(order.createdAt)}</td>
                            </tr>
                            <tr>
                                <td class="text-muted">Tổng tiền:</td>
                                <td><strong class="text-success">${formatCurrency(order.totalAmount)}</strong></td>
                            </tr>
                        </table>
                    </div>
                </div>
            </div>
            <div class="col-md-6">
                <div class="card border-left-success">
                    <div class="card-body">
                        <h6 class="text-success mb-3"><i class="fas fa-user me-2"></i>Thông tin khách hàng</h6>
                        <table class="table table-sm table-borderless mb-0">
                            <tr>
                                <td class="text-muted" width="40%">Tên khách hàng:</td>
                                <td><strong>${order.customerName}</strong></td>
                            </tr>
                            <tr>
                                <td class="text-muted">Số điện thoại:</td>
                                <td>
                                    <a href="tel:${order.customerPhone}" class="text-decoration-none">
                                        <i class="fas fa-phone me-1"></i>${order.customerPhone}
                                    </a>
                                </td>
                            </tr>
                            <tr>
                                <td class="text-muted">Email:</td>
                                <td>${order.customerEmail || 'N/A'}</td>
                            </tr>
                            <tr>
                                <td class="text-muted">Địa chỉ:</td>
                                <td>
                                    ${order.shippingAddress}
                                    <a href="https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(order.shippingAddress)}" 
                                       target="_blank" class="btn btn-sm btn-outline-primary ms-2">
                                        <i class="fas fa-map-marked-alt"></i> Bản đồ
                                    </a>
                                </td>
                            </tr>
                        </table>
                    </div>
                </div>
            </div>
        </div>

        <!-- Order Items -->
        <div class="card border-left-info">
            <div class="card-header bg-white">
                <h6 class="mb-0 text-info"><i class="fas fa-box me-2"></i>Sản phẩm trong đơn hàng</h6>
            </div>
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-hover mb-0">
                        <thead class="bg-light">
                            <tr>
                                <th width="60">Hình</th>
                                <th>Sản phẩm</th>
                                <th width="100">Đơn giá</th>
                                <th width="80">Số lượng</th>
                                <th width="120">Thành tiền</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${order.items && order.items.length > 0 ? order.items.map(item => `
                                <tr>
                                    <td>
                                        <img src="${item.productImageUrl || '/images/no-image.png'}" 
                                             alt="${item.productName}" 
                                             class="img-thumbnail" 
                                             style="width: 50px; height: 50px; object-fit: cover;">
                                    </td>
                                    <td>
                                        <strong>${item.productName}</strong>
                                    </td>
                                    <td>${formatCurrency(item.price)}</td>
                                    <td class="text-center"><span class="badge bg-secondary">${item.quantity}</span></td>
                                    <td><strong>${formatCurrency(item.subtotal)}</strong></td>
                                </tr>
                            `).join('') : `
                                <tr>
                                    <td colspan="5" class="text-center text-muted py-4">Không có sản phẩm</td>
                                </tr>
                            `}
                        </tbody>
                        <tfoot class="bg-light">
                            <tr>
                                <td colspan="4" class="text-end"><strong>Tổng cộng:</strong></td>
                                <td><strong class="text-success fs-5">${formatCurrency(order.totalAmount)}</strong></td>
                            </tr>
                        </tfoot>
                    </table>
                </div>
            </div>
        </div>

        <!-- Payment Info -->
        ${order.paymentMethod ? `
        <div class="alert alert-info mt-3">
            <i class="fas fa-credit-card me-2"></i>
            <strong>Phương thức thanh toán:</strong> ${order.paymentMethod}
        </div>
        ` : ''}

        <!-- Notes -->
        ${order.notes ? `
        <div class="alert alert-secondary mt-3">
            <i class="fas fa-sticky-note me-2"></i>
            <strong>Ghi chú:</strong> ${order.notes}
        </div>
        ` : ''}
    `;
    
    document.getElementById('orderDetailContent').innerHTML = content;
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
        if (event && event.target) {
            event.target.classList.add('active');
        }
        
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

// Tab navigation with filter (for history tab)
function showTabWithFilter(tabName, filterStatus) {
    // First show the tab
    showTab(tabName);
    
    // Then apply the filter if it's history tab
    if (tabName === 'history' && filterStatus) {
        // Wait a bit for the data to load
        setTimeout(() => {
            filterHistory(filterStatus);
        }, 500);
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
        'DELIVERED': { text: 'Giao thành công', class: 'success' },
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
