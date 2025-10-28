document.addEventListener('DOMContentLoaded', function() {
    if (!checkAdminAuth()) {
        return; // Stop execution if not authenticated
    }
    
    // Initialize dashboard
    loadDashboardStats();
    loadRecentActivity();
    
    // Auto refresh every 5 minutes
    setInterval(refreshDashboard, 300000);
});

// Refresh dashboard
function refreshDashboard() {
    showToast('Đang làm mới dữ liệu...', 'info');
    loadDashboardStats();
    loadRecentActivity();
}

// Logout function
function logout() {
    // Create beautiful logout modal
    const modalHtml = `
        <div class="modal fade" id="logoutModal" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content" style="border: none; border-radius: 20px; overflow: hidden;">
                    <div class="modal-header" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; border: none;">
                        <h5 class="modal-title">
                            <i class="fas fa-sign-out-alt me-2"></i>Xác nhận đăng xuất
                        </h5>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body text-center py-4">
                        <div class="mb-3">
                            <i class="fas fa-question-circle" style="font-size: 4rem; color: #667eea;"></i>
                        </div>
                        <h5 class="mb-3">Bạn có chắc muốn đăng xuất?</h5>
                        <p class="text-muted">Bạn sẽ cần đăng nhập lại để tiếp tục sử dụng hệ thống.</p>
                    </div>
                    <div class="modal-footer" style="border: none; justify-content: center; gap: 10px;">
                        <button type="button" class="btn btn-secondary px-4" data-bs-dismiss="modal" style="border-radius: 25px;">
                            <i class="fas fa-times me-2"></i>Hủy bỏ
                        </button>
                        <button type="button" class="btn btn-danger px-4" onclick="confirmLogout()" style="border-radius: 25px;">
                            <i class="fas fa-sign-out-alt me-2"></i>Đăng xuất
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    // Remove existing modal if any
    const existingModal = document.getElementById('logoutModal');
    if (existingModal) {
        existingModal.remove();
    }
    
    // Add modal to body
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    
    // Show modal
    const modal = new bootstrap.Modal(document.getElementById('logoutModal'));
    modal.show();
    
    // Remove modal from DOM after it's hidden
    document.getElementById('logoutModal').addEventListener('hidden.bs.modal', function() {
        this.remove();
    });
}

// Confirm logout action
function confirmLogout() {
    // Close modal
    const modal = bootstrap.Modal.getInstance(document.getElementById('logoutModal'));
    modal.hide();
    
    // Clear storage
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

function checkAdminAuth() {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    const userRole = localStorage.getItem('userRole');
    
    if (!token || !userEmail) {
        showToast('Vui lòng đăng nhập để truy cập trang quản trị!', 'warning');
        setTimeout(() => {
            window.location.href = '/login?error=unauthorized';
        }, 1500);
        return false;
    }
    
    // Check if user has ADMIN role
    if (!userRole || !userRole.includes('ADMIN')) {
        showToast('Bạn không có quyền truy cập trang này!', 'danger');
        setTimeout(() => {
            window.location.href = '/login?error=access_denied';
        }, 1500);
        return false;
    }
    
    // Update admin info in sidebar
    document.getElementById('adminEmail').textContent = userEmail;
    document.getElementById('adminName').textContent = userEmail.split('@')[0];
    
    return true;
}

async function loadDashboardStats() {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    if (!token || !userEmail) return;

    try {
        // Load product stats
        const productStatsResponse = await fetch('/api/admin/products/stats/stock', {
            headers: {
                'Authorization': `Bearer ${token}`,
                'X-User-Email': userEmail
            }
        });

        if (productStatsResponse.ok) {
            const productStats = await productStatsResponse.json();
            
            // Update product stats
            animateCounter('totalProducts', productStats.totalProducts || 0);
            animateCounter('inStockProducts', productStats.inStockProducts || 0);
            animateCounter('lowStockProducts', productStats.lowStockProducts || 0);
            animateCounter('outOfStockProducts', productStats.outOfStockProducts || 0);
            
            // Update progress bar
            const inStockPercent = productStats.totalProducts > 0 
                ? (productStats.inStockProducts / productStats.totalProducts * 100).toFixed(1)
                : 0;
            document.getElementById('inStockProgress').style.width = inStockPercent + '%';
            
            // Update alerts
            updateAlerts(productStats);
            
            // Update stock chart
            updateStockChart(productStats);
            
        } else if (productStatsResponse.status === 401 || productStatsResponse.status === 403) {
            showToast('Phiên đăng nhập hết hạn hoặc không có quyền truy cập!', 'danger');
            setTimeout(() => window.location.href = '/login?error=unauthorized', 1500);
            return;
        }

        // Load order stats
        const orderStatsResponse = await fetch('/api/admin/orders/stats', {
            headers: {
                'Authorization': `Bearer ${token}`,
                'X-User-Email': userEmail
            }
        });

        if (orderStatsResponse.ok) {
            const orderStats = await orderStatsResponse.json();
            
            // Update order stats - map API response to UI
            animateCounter('totalOrders', orderStats.total || 0);
            animateCounter('todayOrders', orderStats.today || 0);
            animateCounter('pendingOrders', orderStats.pending || 0);
            animateCounter('processingOrders', orderStats.processing || 0);
            animateCounter('shippingOrders', orderStats.shipping || 0);
            animateCounter('deliveredOrders', orderStats.delivered || 0);
            animateCounter('failedOrders', orderStats.failed || 0);
            animateCounter('cancelledOrders', orderStats.cancelled || 0);
            
            // Update order chart with correct mapping
            updateOrderChart({
                totalOrders: orderStats.total || 0,
                pendingOrders: orderStats.pending || 0,
                processingOrders: orderStats.processing || 0,
                shippingOrders: orderStats.shipping || 0,
                deliveredOrders: orderStats.delivered || 0,
                failedOrders: orderStats.failed || 0,
                cancelledOrders: orderStats.cancelled || 0
            });
            
            // Check for pending orders alert
            if (orderStats.pending > 0) {
                document.getElementById('pendingCount').textContent = orderStats.pending;
                document.getElementById('pendingOrderAlert').classList.remove('d-none');
                document.getElementById('noAlerts').classList.add('d-none');
            }
        }

        // Load revenue stats
        const revenueResponse = await fetch('/api/admin/orders/revenue', {
            headers: {
                'Authorization': `Bearer ${token}`,
                'X-User-Email': userEmail
            }
        });

        if (revenueResponse.ok) {
            const revenue = await revenueResponse.json();
            
            // Format and update revenue stats
            document.getElementById('totalRevenue').textContent = formatCurrency(revenue.total || 0);
            document.getElementById('monthlyRevenue').textContent = formatCurrency(revenue.monthly || 0);
            document.getElementById('todayRevenue').textContent = formatCurrency(revenue.today || 0);
            document.getElementById('averageOrderValue').textContent = formatCurrency(revenue.average || 0);
        }

    } catch (error) {
        console.error('Error loading dashboard stats:', error);
        showToast('Không thể tải thống kê!', 'danger');
    }
}

// Format currency
function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0
    }).format(amount);
}

// Animate counter
function animateCounter(elementId, target) {
    const element = document.getElementById(elementId);
    const duration = 1000; // 1 second
    const start = 0;
    const increment = target / (duration / 16); // 60fps
    let current = start;
    
    const timer = setInterval(() => {
        current += increment;
        if (current >= target) {
            element.textContent = target;
            clearInterval(timer);
        } else {
            element.textContent = Math.floor(current);
        }
    }, 16);
}

// Update alerts
function updateAlerts(productStats) {
    let hasAlerts = false;
    
    if (productStats.lowStockProducts > 0) {
        document.getElementById('lowStockCount').textContent = productStats.lowStockProducts;
        document.getElementById('lowStockAlert').classList.remove('d-none');
        document.getElementById('noAlerts').classList.add('d-none');
        hasAlerts = true;
    }
    
    if (productStats.outOfStockProducts > 0) {
        document.getElementById('outOfStockCount').textContent = productStats.outOfStockProducts;
        document.getElementById('outOfStockAlert').classList.remove('d-none');
        document.getElementById('noAlerts').classList.add('d-none');
        hasAlerts = true;
    }
    
    if (!hasAlerts) {
        document.getElementById('noAlerts').classList.remove('d-none');
    }
}

// Chart instances
let orderChartInstance = null;
let stockChartInstance = null;

// Update order chart
function updateOrderChart(orderStats) {
    const ctx = document.getElementById('orderChart');
    if (!ctx) return;
    
    // Destroy existing chart
    if (orderChartInstance) {
        orderChartInstance.destroy();
    }
    
    orderChartInstance = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: ['Chờ xử lý', 'Đang xử lý', 'Đang giao', 'Giao thành công', 'Giao thất bại', 'Đã hủy'],
            datasets: [{
                label: 'Số đơn hàng',
                data: [
                    orderStats.pendingOrders || 0,
                    orderStats.processingOrders || 0,
                    orderStats.shippingOrders || 0,
                    orderStats.deliveredOrders || 0,
                    orderStats.failedOrders || 0,
                    orderStats.cancelledOrders || 0
                ],
                backgroundColor: [
                    'rgba(246, 194, 62, 0.8)',
                    'rgba(78, 115, 223, 0.8)',
                    'rgba(54, 185, 204, 0.8)',
                    'rgba(28, 200, 138, 0.8)',
                    'rgba(255, 193, 7, 0.8)',
                    'rgba(231, 74, 59, 0.8)'
                ],
                borderColor: [
                    'rgb(246, 194, 62)',
                    'rgb(78, 115, 223)',
                    'rgb(54, 185, 204)',
                    'rgb(28, 200, 138)',
                    'rgb(255, 193, 7)',
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

// Update stock chart
function updateStockChart(productStats) {
    const ctx = document.getElementById('stockChart');
    if (!ctx) return;
    
    // Destroy existing chart
    if (stockChartInstance) {
        stockChartInstance.destroy();
    }
    
    stockChartInstance = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['Còn hàng', 'Sắp hết', 'Hết hàng'],
            datasets: [{
                data: [
                    productStats.inStockProducts || 0,
                    productStats.lowStockProducts || 0,
                    productStats.outOfStockProducts || 0
                ],
                backgroundColor: [
                    'rgba(28, 200, 138, 0.8)',
                    'rgba(246, 194, 62, 0.8)',
                    'rgba(231, 74, 59, 0.8)'
                ],
                borderColor: [
                    'rgb(28, 200, 138)',
                    'rgb(246, 194, 62)',
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
                    position: 'bottom'
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            const total = context.dataset.data.reduce((a, b) => a + b, 0);
                            const percentage = ((context.parsed / total) * 100).toFixed(1);
                            return context.label + ': ' + context.parsed + ' (' + percentage + '%)';
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
    const activityTable = document.getElementById('activityTable');
    
    if (!token || !userEmail) return;
    
    try {
        // Load recent orders for activity
        const ordersResponse = await fetch('/api/admin/orders?page=0&size=5', {
            headers: {
                'Authorization': `Bearer ${token}`,
                'X-User-Email': userEmail
            }
        });
        
        const activities = [];
        
        if (ordersResponse.ok) {
            const ordersData = await ordersResponse.json();
            const orders = ordersData.content || [];
            
            orders.forEach(order => {
                const createdDate = new Date(order.createdAt);
                const now = new Date();
                const diffMs = now - createdDate;
                const diffMins = Math.floor(diffMs / 60000);
                const diffHours = Math.floor(diffMs / 3600000);
                const diffDays = Math.floor(diffMs / 86400000);
                
                let timeStr = '';
                if (diffMins < 1) timeStr = 'Vừa xong';
                else if (diffMins < 60) timeStr = `${diffMins} phút trước`;
                else if (diffHours < 24) timeStr = `${diffHours} giờ trước`;
                else timeStr = `${diffDays} ngày trước`;
                
                let description = `Đơn hàng #${order.id} `;
                let status = 'info';
                
                switch(order.status) {
                    case 'PENDING':
                        description += 'đang chờ xử lý';
                        status = 'warning';
                        break;
                    case 'PROCESSING':
                        description += 'đang được xử lý';
                        status = 'info';
                        break;
                    case 'SHIPPING':
                        description += 'đang được giao';
                        status = 'info';
                        break;
                    case 'DELIVERED':
                        description += 'đã giao thành công';
                        status = 'success';
                        break;
                    case 'FAILED':
                        description += 'giao thất bại';
                        status = 'warning';
                        break;
                    case 'CANCELLED':
                        description += 'đã bị hủy';
                        status = 'danger';
                        break;
                    default:
                        description += 'được tạo';
                }
                
                activities.push({
                    time: timeStr,
                    type: 'Đơn hàng',
                    description: description,
                    status: status
                });
            });
        }
        
        // If no activities, show placeholder
        if (activities.length === 0) {
            activityTable.innerHTML = `
                <tr>
                    <td colspan="4" class="text-center text-muted">
                        <i class="fas fa-info-circle me-2"></i>
                        Chưa có hoạt động nào
                    </td>
                </tr>
            `;
            return;
        }
        
        activityTable.innerHTML = activities.map(activity => `
            <tr>
                <td><small class="text-muted">${activity.time}</small></td>
                <td><span class="badge bg-secondary">${activity.type}</span></td>
                <td>${activity.description}</td>
                <td>
                    <span class="badge bg-${activity.status === 'success' ? 'success' : activity.status === 'warning' ? 'warning' : activity.status === 'danger' ? 'danger' : 'info'}">
                        <i class="fas fa-${activity.status === 'success' ? 'check' : activity.status === 'warning' ? 'exclamation-triangle' : activity.status === 'danger' ? 'times' : 'info-circle'}"></i>
                    </span>
                </td>
            </tr>
        `).join('');
        
    } catch (error) {
        console.error('Error loading recent activity:', error);
        activityTable.innerHTML = `
            <tr>
                <td colspan="4" class="text-center text-danger">
                    <i class="fas fa-exclamation-circle me-2"></i>
                    Không thể tải hoạt động gần đây
                </td>
            </tr>
        `;
    }
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
    }, 2000);
}
