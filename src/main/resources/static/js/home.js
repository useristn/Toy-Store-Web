document.addEventListener('DOMContentLoaded', function() {
    loadFeaturedProducts();
    
    // Header search functionality for home page
    const searchBtn = document.getElementById('searchBtn');
    const searchInput = document.getElementById('searchInput');
    
    if (searchBtn && searchInput) {
        searchBtn.addEventListener('click', function() {
            performSearch();
        });
        
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                performSearch();
            }
        });
    }
    
    function performSearch() {
        const keyword = searchInput.value.trim();
        if (keyword) {
            window.location.href = `/products?search=${encodeURIComponent(keyword)}`;
        }
    }
});

function loadFeaturedProducts() {
    fetch('/api/products/featured')
        .then(response => response.json())
        .then(products => {
            displayFeaturedProducts(products);
        })
        .catch(error => {
            console.error('Error loading featured products:', error);
            document.getElementById('featuredProducts').innerHTML = `
                <div class="col-12 text-center py-5">
                    <i class="fas fa-exclamation-triangle fa-3x text-warning mb-3"></i>
                    <p class="text-muted">Không thể tải sản phẩm. Vui lòng thử lại sau!</p>
                </div>
            `;
        });
}

function displayFeaturedProducts(products) {
    const container = document.getElementById('featuredProducts');
    
    if (!products || products.length === 0) {
        container.innerHTML = `
            <div class="col-12 text-center py-5">
                <i class="fas fa-space-shuttle fa-3x text-muted mb-3"></i>
                <p class="text-muted">Chưa có sản phẩm nổi bật</p>
            </div>
        `;
        return;
    }
    
    // Hiển thị tối đa 4 sản phẩm
    const displayProducts = products.slice(0, 4);
    
    container.innerHTML = displayProducts.map(product => `
        <div class="col-lg-3 col-md-6 mb-4">
            <div class="card featured-card h-100">
                ${product.discountPrice ? '<span class="badge bg-danger position-absolute top-0 start-0 m-2">SALE</span>' : ''}
                ${product.featured ? '<span class="badge bg-warning position-absolute top-0 end-0 m-2">⭐ HOT</span>' : ''}
                <img src="${product.imageUrl || 'https://via.placeholder.com/300x200/6c5ce7/FFFFFF?text=' + encodeURIComponent(product.name)}" 
                     class="card-img-top" 
                     alt="${product.name}"
                     style="cursor: pointer;"
                     onclick="viewProduct(${product.id})">
                <div class="card-body d-flex flex-column">
                    <h5 class="card-title" style="cursor: pointer;" onclick="viewProduct(${product.id})">${product.name}</h5>
                    <p class="card-text text-muted flex-grow-1">${product.description ? (product.description.length > 50 ? product.description.substring(0, 50) + '...' : product.description) : 'Sản phẩm tuyệt vời!'}</p>
                    
                    ${generateRatingStars(product.averageRating, product.ratingCount)}
                    
                    <div class="mb-2">
                        ${product.discountPrice ? 
                            `<div class="h5 text-danger mb-0">${formatPrice(product.discountPrice)}</div>
                             <small class="text-muted text-decoration-line-through">${formatPrice(product.price)}</small>` :
                            `<div class="h5 text-danger mb-0">${formatPrice(product.price)}</div>`
                        }
                    </div>
                    <div class="d-grid gap-2">
                        <button class="btn btn-outline-success btn-sm" onclick="addToCart(${product.id}, event)">
                            <i class="fas fa-cart-plus me-1"></i>Thêm vào giỏ
                        </button>
                        <button class="btn btn-primary btn-sm" onclick="buyNow(${product.id}, event)">
                            <i class="fas fa-rocket me-1"></i>Mua ngay
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `).join('');
}

function viewProduct(id) {
    window.location.href = `/product/${id}`;
}

async function addToCart(id, event) {
    if (event) {
        event.stopPropagation();
    }
    
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    if (!token || !userEmail) {
        showToast('Vui lòng đăng nhập để thêm vào giỏ hàng!', 'warning');
        setTimeout(() => {
            window.location.href = '/login';
        }, 1500);
        return;
    }
    
    try {
        const response = await fetch('/api/cart/add', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
                'X-User-Email': userEmail
            },
            body: JSON.stringify({
                productId: id,
                quantity: 1
            })
        });
        
        if (response.status === 401) {
            showToast('Phiên đăng nhập đã hết hạn!', 'warning');
            setTimeout(() => {
                window.location.href = '/login';
            }, 1500);
            return;
        }
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Không thể thêm vào giỏ hàng');
        }
        
        showToast('Đã thêm vào giỏ hàng! 🚀', 'success');
        
        // Update cart badge
        if (typeof updateCartBadge === 'function') {
            updateCartBadge();
        }
    } catch (error) {
        console.error('Error adding to cart:', error);
        showToast(error.message || 'Không thể thêm vào giỏ hàng!', 'danger');
    }
}

async function buyNow(id, event) {
    if (event) {
        event.stopPropagation();
    }
    
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    if (!token || !userEmail) {
        showToast('Vui lòng đăng nhập để mua hàng!', 'warning');
        setTimeout(() => {
            window.location.href = '/login';
        }, 1500);
        return;
    }
    
    try {
        // Add to cart first
        const response = await fetch('/api/cart/add', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
                'X-User-Email': userEmail
            },
            body: JSON.stringify({
                productId: id,
                quantity: 1
            })
        });
        
        if (response.status === 401) {
            showToast('Phiên đăng nhập đã hết hạn!', 'warning');
            setTimeout(() => {
                window.location.href = '/login';
            }, 1500);
            return;
        }
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Không thể thêm vào giỏ hàng');
        }
        
        // Success - redirect to checkout
        showToast('Đang chuyển đến trang thanh toán...', 'success');
        
        setTimeout(() => {
            window.location.href = '/checkout';
        }, 800);
        
    } catch (error) {
        console.error('Error buying now:', error);
        showToast(error.message || 'Không thể mua hàng!', 'danger');
    }
}

function showToast(message, type = 'success') {
    const toastDiv = document.createElement('div');
    toastDiv.className = `alert alert-${type} position-fixed top-0 start-50 translate-middle-x mt-3`;
    toastDiv.style.zIndex = '9999';
    toastDiv.innerHTML = `
        <i class="fas fa-${type === 'success' ? 'check-circle' : type === 'warning' ? 'exclamation-triangle' : 'exclamation-circle'} me-2"></i>
        ${message}
    `;
    document.body.appendChild(toastDiv);
    
    setTimeout(() => {
        toastDiv.style.opacity = '0';
        toastDiv.style.transition = 'opacity 0.5s';
        setTimeout(() => toastDiv.remove(), 500);
    }, 2000);
}

function formatPrice(price) {
    return new Intl.NumberFormat('vi-VN', { 
        style: 'currency', 
        currency: 'VND' 
    }).format(price);
}

// ===== RATING DISPLAY HELPER =====
function generateRatingStars(averageRating, ratingCount) {
    const avgRating = averageRating || 0;
    const count = ratingCount || 0;
    
    if (count === 0) {
        return `
            <div class="mb-2">
                <small class="text-muted">
                    <i class="far fa-star text-warning"></i>
                    <i class="far fa-star text-warning"></i>
                    <i class="far fa-star text-warning"></i>
                    <i class="far fa-star text-warning"></i>
                    <i class="far fa-star text-warning"></i>
                    <span class="ms-1">Chưa có đánh giá</span>
                </small>
            </div>
        `;
    }
    
    const fullStars = Math.floor(avgRating);
    const hasHalfStar = avgRating % 1 >= 0.5;
    
    let starsHtml = '';
    for (let i = 0; i < 5; i++) {
        if (i < fullStars) {
            starsHtml += '<i class="fas fa-star text-warning"></i>';
        } else if (i === fullStars && hasHalfStar) {
            starsHtml += '<i class="fas fa-star-half-alt text-warning"></i>';
        } else {
            starsHtml += '<i class="far fa-star text-warning"></i>';
        }
    }
    
    return `
        <div class="mb-2">
            <small>
                ${starsHtml}
                <span class="ms-1 fw-bold">${avgRating.toFixed(1)}</span>
                <span class="text-muted">(${count})</span>
            </small>
        </div>
    `;
}
