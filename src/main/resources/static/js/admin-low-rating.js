// Admin Low Rating Products JavaScript
let currentPage = 0;
let currentSize = 20;
let currentCategory = null;
let currentMaxRating = 3.0;
let allProducts = [];

document.addEventListener('DOMContentLoaded', function() {
    console.log('Admin Low Rating: Initializing...');
    checkAuth();
    loadCategories();
    loadRatingStats();
    loadLowRatingProducts();
    
    // Setup event listeners
    document.getElementById('categoryFilter').addEventListener('change', function() {
        currentCategory = this.value || null;
        currentPage = 0;
        loadLowRatingProducts();
    });
    
    document.getElementById('maxRatingFilter').addEventListener('change', function() {
        currentMaxRating = parseFloat(this.value);
        currentPage = 0;
        loadLowRatingProducts();
    });
    
    document.getElementById('pageSizeFilter').addEventListener('change', function() {
        currentSize = parseInt(this.value);
        currentPage = 0;
        loadLowRatingProducts();
    });
});

function checkAuth() {
    const token = localStorage.getItem('authToken');
    const userRole = localStorage.getItem('userRole');
    
    console.log('Checking auth:', { token: !!token, role: userRole });
    
    if (!token) {
        console.error('No token found, redirecting to login');
        window.location.href = '/login';
        return;
    }
    
    if (!userRole || !userRole.includes('ADMIN')) {
        console.error('Not an admin, redirecting to home');
        window.location.href = '/';
        return;
    }
    
    // Display admin info
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    if (userEmail) {
        document.getElementById('adminEmail').textContent = userEmail;
    }
}

function getAuthHeaders() {
    const token = localStorage.getItem('authToken');
    return {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    };
}

function loadCategories() {
    fetch('/api/products/categories')
        .then(response => response.json())
        .then(categories => {
            const select = document.getElementById('categoryFilter');
            categories.forEach(cat => {
                const option = document.createElement('option');
                option.value = cat.id;
                option.textContent = `${cat.icon || ''} ${cat.name}`;
                select.appendChild(option);
            });
        })
        .catch(error => {
            console.error('Error loading categories:', error);
        });
}

function loadRatingStats() {
    fetch('/api/admin/products/stats/rating', {
        headers: getAuthHeaders()
    })
    .then(response => {
        if (!response.ok) throw new Error('Failed to load stats');
        return response.json();
    })
    .then(stats => {
        displayRatingStats(stats);
    })
    .catch(error => {
        console.error('Error loading rating stats:', error);
        document.getElementById('statsContainer').innerHTML = `
            <div class="col-12">
                <div class="alert alert-danger">
                    <i class="fas fa-exclamation-triangle me-2"></i>
                    Không thể tải thống kê. Vui lòng thử lại!
                </div>
            </div>
        `;
    });
}

function displayRatingStats(stats) {
    const container = document.getElementById('statsContainer');
    
    const html = `
        <div class="col-md-4 col-lg-2">
            <div class="stats-card" style="background: linear-gradient(135deg, #6c91c2 0%, #4a6fa5 100%);">
                <div class="d-flex justify-content-between align-items-start mb-2">
                    <h6 class="mb-0 text-white"><i class="fas fa-star"></i> 0-1.4</h6>
                    <span class="badge bg-white text-dark">${stats.veryPoor}</span>
                </div>
                <p class="mb-0 small text-white">Rất tệ</p>
            </div>
        </div>
        <div class="col-md-4 col-lg-2">
            <div class="stats-card" style="background: linear-gradient(135deg, #6c91c2 0%, #4a6fa5 100%);">
                <div class="d-flex justify-content-between align-items-start mb-2">
                    <h6 class="mb-0 text-white"><i class="fas fa-star"></i><i class="fas fa-star"></i> 1.5-2.4</h6>
                    <span class="badge bg-white text-dark">${stats.poor}</span>
                </div>
                <p class="mb-0 small text-white">Tệ</p>
            </div>
        </div>
        <div class="col-md-4 col-lg-2">
            <div class="stats-card" style="background: linear-gradient(135deg, #6c91c2 0%, #4a6fa5 100%);">
                <div class="d-flex justify-content-between align-items-start mb-2">
                    <h6 class="mb-0 text-white"><i class="fas fa-star"></i><i class="fas fa-star"></i><i class="fas fa-star"></i> 2.5-3.4</h6>
                    <span class="badge bg-white text-dark">${stats.average}</span>
                </div>
                <p class="mb-0 small text-white">Trung bình</p>
            </div>
        </div>
        <div class="col-md-4 col-lg-2">
            <div class="stats-card" style="background: linear-gradient(135deg, #6c91c2 0%, #4a6fa5 100%);">
                <div class="d-flex justify-content-between align-items-start mb-2">
                    <h6 class="mb-0 text-white"><i class="fas fa-star"></i><i class="fas fa-star"></i><i class="fas fa-star"></i><i class="fas fa-star"></i> 3.5-4.4</h6>
                    <span class="badge bg-white text-dark">${stats.good}</span>
                </div>
                <p class="mb-0 small text-white">Tốt</p>
            </div>
        </div>
        <div class="col-md-4 col-lg-2">
            <div class="stats-card" style="background: linear-gradient(135deg, #6c91c2 0%, #4a6fa5 100%);">
                <div class="d-flex justify-content-between align-items-start mb-2">
                    <h6 class="mb-0 text-white"><i class="fas fa-star"></i><i class="fas fa-star"></i><i class="fas fa-star"></i><i class="fas fa-star"></i><i class="fas fa-star"></i> 4.5-5.0</h6>
                    <span class="badge bg-white text-dark">${stats.excellent}</span>
                </div>
                <p class="mb-0 small text-white">Xuất sắc</p>
            </div>
        </div>
        <div class="col-md-4 col-lg-2">
            <div class="stats-card" style="background: linear-gradient(135deg, #e9ecef 0%, #dee2e6 100%);">
                <div class="d-flex justify-content-between align-items-start mb-2">
                    <h6 class="mb-0"><i class="fas fa-question"></i></h6>
                    <span class="badge bg-secondary">${stats.noRating}</span>
                </div>
                <p class="mb-0 small">Chưa có đánh giá</p>
            </div>
        </div>
    `;
    
    container.innerHTML = html;
}

function loadLowRatingProducts() {
    const url = new URL('/api/admin/products/low-rating', window.location.origin);
    url.searchParams.append('page', currentPage);
    url.searchParams.append('size', currentSize);
    url.searchParams.append('maxRating', currentMaxRating);
    if (currentCategory) {
        url.searchParams.append('categoryId', currentCategory);
    }
    
    fetch(url, {
        headers: getAuthHeaders()
    })
    .then(response => {
        if (!response.ok) throw new Error('Failed to load products');
        return response.json();
    })
    .then(data => {
        allProducts = data.content;
        displayProducts(data);
        displayPagination(data);
        document.getElementById('totalProducts').textContent = data.totalElements;
    })
    .catch(error => {
        console.error('Error loading products:', error);
        document.getElementById('productsContainer').innerHTML = `
            <div class="alert alert-danger">
                <i class="fas fa-exclamation-triangle me-2"></i>
                Không thể tải danh sách sản phẩm. Vui lòng thử lại!
            </div>
        `;
    });
}

function displayProducts(data) {
    const container = document.getElementById('productsContainer');
    
    if (data.content.length === 0) {
        container.innerHTML = `
            <div class="text-center py-5">
                <i class="fas fa-smile fa-4x text-success mb-3"></i>
                <h4>Tuyệt vời! Không có sản phẩm nào có rating thấp</h4>
                <p class="text-muted">Tất cả sản phẩm đều được khách hàng đánh giá tốt</p>
            </div>
        `;
        return;
    }
    
    const html = data.content.map(product => {
        const rating = product.averageRating || 0;
        const ratingCount = product.ratingCount || 0;
        const ratingClass = getRatingClass(rating);
        const ratingBadge = getRatingBadge(rating);
        const stars = getStarRating(rating);
        
        return `
            <div class="product-card bg-white p-3 mb-3">
                <div class="row align-items-center">
                    <div class="col-md-1">
                        <img src="${product.imageUrl || 'https://via.placeholder.com/80'}" 
                             alt="${product.name}" 
                             class="product-img">
                    </div>
                    <div class="col-md-3">
                        <h6 class="mb-1">${product.name}</h6>
                        <small class="text-muted">
                            <i class="fas fa-layer-group me-1"></i>
                            ${product.categoryName || 'Chưa phân loại'}
                        </small>
                    </div>
                    <div class="col-md-2 text-center">
                        <div class="rating-badge ${ratingClass}">
                            ${stars} ${rating.toFixed(1)}
                        </div>
                        <small class="text-muted d-block mt-1">${ratingCount} đánh giá</small>
                    </div>
                    <div class="col-md-2 text-center">
                        <div class="mb-1">
                            <strong>${formatPrice(product.price)}</strong>
                        </div>
                        ${product.discountPrice ? `
                            <small class="text-decoration-line-through text-muted">
                                ${formatPrice(product.discountPrice)}
                            </small>
                        ` : ''}
                    </div>
                    <div class="col-md-2 text-center">
                        <span class="badge ${product.stock > 10 ? 'bg-success' : product.stock > 0 ? 'bg-warning' : 'bg-danger'}">
                            <i class="fas fa-box me-1"></i>
                            ${product.stock || 0}
                        </span>
                    </div>
                    <div class="col-md-2 text-end">
                        <button class="btn btn-sm btn-outline-primary me-1" 
                                onclick="viewProduct(${product.id})"
                                title="Xem chi tiết">
                            <i class="fas fa-eye"></i>
                        </button>
                        <button class="btn btn-sm btn-outline-warning" 
                                onclick="editProduct(${product.id})"
                                title="Chỉnh sửa">
                            <i class="fas fa-edit"></i>
                        </button>
                    </div>
                </div>
            </div>
        `;
    }).join('');
    
    container.innerHTML = html;
}

function getRatingClass(rating) {
    if (rating >= 4.5) return 'rating-excellent';
    if (rating >= 3.5) return 'rating-good';
    if (rating >= 2.5) return 'rating-average';
    if (rating >= 1.5) return 'rating-poor';
    return 'rating-very-poor';
}

function getRatingBadge(rating) {
    if (rating >= 4.5) return '⭐⭐⭐⭐⭐';
    if (rating >= 3.5) return '⭐⭐⭐⭐';
    if (rating >= 2.5) return '⭐⭐⭐';
    if (rating >= 1.5) return '⭐⭐';
    return '⭐';
}

function getStarRating(rating) {
    const fullStars = Math.floor(rating);
    const hasHalfStar = rating - fullStars >= 0.5;
    let stars = '';
    
    for (let i = 0; i < fullStars; i++) {
        stars += '<i class="fas fa-star"></i>';
    }
    
    if (hasHalfStar) {
        stars += '<i class="fas fa-star-half-alt"></i>';
    }
    
    return stars;
}

function displayPagination(data) {
    const container = document.getElementById('paginationContainer');
    
    if (data.totalPages <= 1) {
        container.innerHTML = '';
        return;
    }
    
    let html = '';
    
    // Previous button
    html += `
        <li class="page-item ${data.first ? 'disabled' : ''}">
            <a class="page-link" href="#" onclick="changePage(${currentPage - 1}); return false;">
                <i class="fas fa-chevron-left"></i>
            </a>
        </li>
    `;
    
    // Page numbers
    const maxVisible = 5;
    let startPage = Math.max(0, currentPage - Math.floor(maxVisible / 2));
    let endPage = Math.min(data.totalPages - 1, startPage + maxVisible - 1);
    
    if (endPage - startPage < maxVisible - 1) {
        startPage = Math.max(0, endPage - maxVisible + 1);
    }
    
    for (let i = startPage; i <= endPage; i++) {
        html += `
            <li class="page-item ${i === currentPage ? 'active' : ''}">
                <a class="page-link" href="#" onclick="changePage(${i}); return false;">
                    ${i + 1}
                </a>
            </li>
        `;
    }
    
    // Next button
    html += `
        <li class="page-item ${data.last ? 'disabled' : ''}">
            <a class="page-link" href="#" onclick="changePage(${currentPage + 1}); return false;">
                <i class="fas fa-chevron-right"></i>
            </a>
        </li>
    `;
    
    container.innerHTML = html;
}

function changePage(page) {
    if (page < 0) return;
    currentPage = page;
    loadLowRatingProducts();
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function viewProduct(id) {
    window.open(`/product/${id}`, '_blank');
}

function editProduct(id) {
    window.location.href = `/admin/products?edit=${id}`;
}

function formatPrice(price) {
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(price);
}

function exportToCSV() {
    if (allProducts.length === 0) {
        alert('Không có dữ liệu để xuất!');
        return;
    }
    
    // Create CSV content
    let csv = 'ID,Tên sản phẩm,Danh mục,Rating,Số đánh giá,Giá,Tồn kho\n';
    
    allProducts.forEach(product => {
        csv += `${product.id},"${product.name}","${product.categoryName || ''}",${product.averageRating || 0},${product.ratingCount || 0},${product.price},${product.stock || 0}\n`;
    });
    
    // Download CSV
    const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    
    link.setAttribute('href', url);
    link.setAttribute('download', `low-rating-products-${new Date().toISOString().split('T')[0]}.csv`);
    link.style.visibility = 'hidden';
    
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}

// Logout handler
document.getElementById('logoutLink')?.addEventListener('click', function(e) {
    e.preventDefault();
    localStorage.clear();
    window.location.href = '/login';
});
