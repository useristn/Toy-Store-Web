document.addEventListener('DOMContentLoaded', function() {
    if (!checkAdminAuth()) {
        return; // Stop execution if not authenticated
    }
    loadDashboardStats();
});

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
            document.getElementById('totalProducts').textContent = productStats.totalProducts || 0;
            document.getElementById('inStockProducts').textContent = productStats.inStockProducts || 0;
            document.getElementById('lowStockProducts').textContent = productStats.lowStockProducts || 0;
            document.getElementById('outOfStockProducts').textContent = productStats.outOfStockProducts || 0;
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
            document.getElementById('totalOrders').textContent = orderStats.totalOrders || 0;
            document.getElementById('pendingOrders').textContent = orderStats.pendingOrders || 0;
            document.getElementById('processingOrders').textContent = orderStats.processingOrders || 0;
            document.getElementById('deliveredOrders').textContent = orderStats.deliveredOrders || 0;
        }

    } catch (error) {
        console.error('Error loading dashboard stats:', error);
        showToast('Không thể tải thống kê!', 'danger');
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
