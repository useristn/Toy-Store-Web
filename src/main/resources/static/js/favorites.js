document.addEventListener('DOMContentLoaded', function() {
    checkAuth();
    loadFavorites();
});

function checkAuth() {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    if (!token || !userEmail) {
        showToast('Vui lòng đăng nhập để xem danh sách yêu thích!', 'warning');
        setTimeout(() => {
            window.location.href = '/login';
        }, 1500);
    }
}

async function loadFavorites() {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    const container = document.getElementById('favoritesContainer');
    if (!container) return;
    
    try {
        const response = await fetch('/api/favorites', {
            headers: {
                'Authorization': `Bearer ${token}`,
                'X-User-Email': userEmail
            }
        });
        
        if (response.status === 401) {
            showToast('Phiên đăng nhập đã hết hạn!', 'warning');
            setTimeout(() => {
                window.location.href = '/login';
            }, 1500);
            return;
        }
        
        if (!response.ok) {
            throw new Error('Cannot load favorites');
        }
        
        const favorites = await response.json();
        
        // Update count
        const countEl = document.getElementById('favoriteCount');
        if (countEl) countEl.textContent = favorites.length;
        
        displayFavorites(favorites);
    } catch (error) {
        console.error('Error loading favorites:', error);
        container.innerHTML = `
            <div class="col-12 text-center py-5">
                <i class="fas fa-exclamation-triangle fa-3x text-danger mb-3"></i>
                <p class="text-danger">Không thể tải danh sách yêu thích!</p>
                <button class="btn btn-primary" onclick="loadFavorites()">
                    <i class="fas fa-redo me-2"></i>Thử lại
                </button>
            </div>
        `;
    }
}

function displayFavorites(favorites) {
    const container = document.getElementById('favoritesContainer');
    if (!container) return;
    
    if (favorites.length === 0) {
        container.innerHTML = `
            <div class="col-12 text-center py-5">
                <i class="fas fa-heart-broken fa-5x text-muted mb-3"></i>
                <h4 class="text-muted">Chưa có sản phẩm yêu thích</h4>
                <p class="text-muted">Hãy thêm những món đồ chơi bạn thích vào đây!</p>
                <a href="/products" class="btn btn-primary">
                    <i class="fas fa-shopping-bag me-2"></i>Khám phá sản phẩm
                </a>
            </div>
        `;
        return;
    }
    
    container.innerHTML = favorites.map(favorite => {
        const product = favorite.product;
        return `
            <div class="col-lg-3 col-md-4 col-sm-6 mb-4">
                <div class="card featured-card h-100">
                    <div class="position-relative">
                        <img src="${product.imageUrl || 'https://via.placeholder.com/300x200/6c5ce7/FFFFFF?text=' + encodeURIComponent(product.name)}" 
                             class="card-img-top" alt="${product.name}"
                             style="cursor: pointer; height: 200px; object-fit: cover;" 
                             onclick="viewProduct(${product.id})">
                        ${product.discountPrice ? '<span class="badge bg-danger position-absolute top-0 end-0 m-2">SALE</span>' : ''}
                        ${product.featured ? '<span class="badge bg-warning position-absolute top-0 start-0 m-2">⭐ HOT</span>' : ''}
                    </div>
                    <div class="card-body d-flex flex-column">
                        <h5 class="card-title" style="cursor: pointer;" onclick="viewProduct(${product.id})">${product.name}</h5>
                        <p class="card-text text-muted small flex-grow-1">${product.description ? (product.description.length > 80 ? product.description.substring(0, 80) + '...' : product.description) : 'Sản phẩm tuyệt vời!'}</p>
                        
                        ${product.category ? `<div class="mb-2"><span class="badge bg-primary">${product.category.icon || ''} ${product.category.name}</span></div>` : ''}
                        
                        <div class="mt-auto">
                            <div class="mb-2 d-flex justify-content-between align-items-center">
                                <div>
                                    ${product.discountPrice ? 
                                        `<div class="h5 text-danger mb-0">${formatPrice(product.discountPrice)}</div>
                                         <small class="text-muted text-decoration-line-through">${formatPrice(product.price)}</small>` :
                                        `<div class="h5 text-danger mb-0">${formatPrice(product.price)}</div>`
                                    }
                                </div>
                                <button class="btn btn-danger btn-sm" 
                                        onclick="removeFavorite(${product.id}, event)" 
                                        title="Xóa khỏi yêu thích">
                                    <i class="fas fa-trash-alt"></i>
                                </button>
                            </div>
                            <div class="d-grid gap-2">
                                <button class="btn btn-outline-success btn-sm" onclick="addToCart(${product.id}, event)">
                                    <i class="fas fa-cart-plus me-1"></i>Thêm vào giỏ
                                </button>
                                <button class="btn btn-primary btn-sm" onclick="viewProduct(${product.id})">
                                    <i class="fas fa-eye me-1"></i>Xem chi tiết
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

async function removeFavorite(productId, event) {
    if (event) event.stopPropagation();
    
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    try {
        const response = await fetch(`/api/favorites/remove/${productId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`,
                'X-User-Email': userEmail
            }
        });
        
        if (!response.ok) {
            throw new Error('Cannot remove favorite');
        }
        
        showToast('Đã xóa khỏi yêu thích!', 'success');
        
        // Reload favorites list
        setTimeout(() => {
            loadFavorites();
        }, 500);
    } catch (error) {
        console.error('Error removing favorite:', error);
        showToast('Không thể xóa khỏi yêu thích!', 'danger');
    }
}

function viewProduct(id) {
    window.location.href = `/product/${id}`;
}

async function addToCart(id, event) {
    if (event) event.stopPropagation();
    
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

function formatPrice(price) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
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
