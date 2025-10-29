document.addEventListener('DOMContentLoaded', function() {
    const productId = window.location.pathname.split('/').pop();
    loadProductDetail(productId);
});

function loadProductDetail(id) {
    fetch(`/api/products/${id}`)
        .then(response => {
            if (!response.ok) throw new Error('Product not found');
            return response.json();
        })
        .then(product => {
            displayProductDetail(product);
            // Display rating from product data directly
            displayRatingFromProduct(product);
            loadRelatedProducts(product.category?.id);
            document.getElementById('breadcrumbProduct').textContent = product.name;
        })
        .catch(() => {
            document.getElementById('productDetailContainer').innerHTML = `
                <div class="alert alert-danger text-center">
                    <i class="fas fa-exclamation-triangle me-2"></i>
                    Không tìm thấy sản phẩm
                </div>
            `;
        });
}

function displayProductDetail(product) {
    const container = document.getElementById('productDetailContainer');
    
    container.innerHTML = `
        <div class="row">
            <div class="col-lg-6 mb-4">
                <div class="card">
                    <img src="${product.imageUrl || 'https://via.placeholder.com/600x400/6c5ce7/FFFFFF?text=' + product.name}" 
                         class="card-img-top" alt="${product.name}">
                </div>
            </div>
            <div class="col-lg-6">
                <div class="card h-100">
                    <div class="card-body">
                        ${product.discountPrice ? '<span class="badge bg-danger mb-3">ĐANG GIẢM GIÁ</span>' : ''}
                        <h2 class="fw-bold space-text mb-3">${product.name}</h2>
                        
                        <div id="ratingDisplay" class="mb-3">
                            <span class="text-muted">Đang tải đánh giá...</span>
                        </div>
                        
                        <div class="mb-4">
                            ${product.discountPrice ? 
                                `<h3 class="text-danger mb-2">${formatPrice(product.discountPrice)}</h3>
                                 <p class="text-muted text-decoration-line-through">${formatPrice(product.price)}</p>` :
                                `<h3 class="text-danger">${formatPrice(product.price)}</h3>`
                            }
                        </div>
                        
                        <div class="mb-4">
                            <p class="text-muted">${product.description || 'Sản phẩm tuyệt vời đang chờ bạn khám phá!'}</p>
                        </div>
                        
                        <div class="mb-4">
                            <strong>Danh mục:</strong> 
                            <span class="badge bg-primary">${product.category?.name || 'Khác'}</span>
                        </div>
                        
                        <div class="mb-4">
                            <strong>Tình trạng:</strong> 
                            ${product.stock > 0 ? 
                                `<span class="text-success"><i class="fas fa-check-circle me-1"></i>Còn hàng (${product.stock} sản phẩm)</span>` :
                                `<span class="text-danger"><i class="fas fa-times-circle me-1"></i>Hết hàng</span>`
                            }
                        </div>
                        
                        <div class="row mb-4">
                            <div class="col-4">
                                <label class="form-label">Số lượng</label>
                                <input type="number" class="form-control" value="1" min="1" max="${product.stock || 1}" id="quantity">
                            </div>
                        </div>
                        
                        <div class="d-grid gap-2">
                            <button class="btn btn-danger btn-lg" onclick="addToCart(${product.id})" ${product.stock <= 0 ? 'disabled' : ''}>
                                <i class="fas fa-shopping-cart me-2"></i>Thêm vào giỏ hàng
                            </button>
                            <button class="btn btn-outline-primary btn-lg" onclick="buyNow(${product.id})" ${product.stock <= 0 ? 'disabled' : ''}>
                                <i class="fas fa-rocket me-2"></i>Mua ngay
                            </button>
                            <button class="btn btn-outline-danger btn-lg" style="border-width: 2px;" onclick="toggleFavorite(${product.id})" id="favoriteBtn-detail">
                                <i class="far fa-heart me-2"></i>Thêm vào yêu thích
                            </button>
                        </div>
                        
                        <hr class="my-4">
                        
                        <div class="row text-center">
                            <div class="col-4">
                                <i class="fas fa-shipping-fast fa-2x text-primary mb-2"></i>
                                <p class="small mb-0">Giao hàng nhanh</p>
                            </div>
                            <div class="col-4">
                                <i class="fas fa-shield-alt fa-2x text-success mb-2"></i>
                                <p class="small mb-0">Bảo hành 1 năm</p>
                            </div>
                            <div class="col-4">
                                <i class="fas fa-undo fa-2x text-warning mb-2"></i>
                                <p class="small mb-0">Đổi trả 7 ngày</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    // Load favorite state after rendering
    loadFavoriteState(product.id);
}

function loadRelatedProducts(categoryId) {
    if (!categoryId) {
        document.getElementById('relatedProducts').innerHTML = '';
        return;
    }
    
    fetch(`/api/products/category/${categoryId}?page=0&size=4`)
        .then(response => response.json())
        .then(data => {
            const products = data.content.slice(0, 4);
            const container = document.getElementById('relatedProducts');
            
            if (products.length === 0) {
                container.innerHTML = '';
                return;
            }
            
            container.innerHTML = products.map(product => `
                <div class="col-lg-3 col-md-6 mb-4">
                    <div class="card featured-card h-100">
                        <img src="${product.imageUrl || 'https://via.placeholder.com/300x200/6c5ce7/FFFFFF?text=' + product.name}" 
                             class="card-img-top" alt="${product.name}">
                        <div class="card-body d-flex flex-column">
                            <h5 class="card-title">${product.name}</h5>
                            ${generateRatingStars(product.averageRating, product.ratingCount)}
                            <div class="mt-auto d-flex justify-content-between align-items-center">
                                <span class="h5 text-danger mb-0">${formatPrice(product.discountPrice || product.price)}</span>
                                <button class="btn btn-outline-primary btn-sm" onclick="viewProduct(${product.id})">
                                    <i class="fas fa-eye"></i>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            `).join('');
        });
}

async function addToCart(id) {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    if (!token || !userEmail) {
        showNotification('Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng!', 'warning');
        setTimeout(() => {
            window.location.href = '/login';
        }, 1500);
        return;
    }

    const quantity = parseInt(document.getElementById('quantity').value);
    
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
                quantity: quantity
            })
        });

        if (response.status === 401) {
            showNotification('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại!', 'warning');
            setTimeout(() => {
                window.location.href = '/login';
            }, 1500);
            return;
        }

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Không thể thêm vào giỏ hàng');
        }

        // Show success notification with quantity
        const message = quantity > 1 
            ? `Đã thêm ${quantity} sản phẩm vào giỏ hàng! 🚀` 
            : 'Đã thêm vào giỏ hàng! 🚀';
        showNotification(message, 'success');
        
        // Update cart badge in header
        if (typeof updateCartBadge === 'function') {
            updateCartBadge();
        }
    } catch (error) {
        console.error('Error adding to cart:', error);
        showNotification(error.message || 'Không thể thêm vào giỏ hàng!', 'danger');
    }
}

async function buyNow(id) {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    if (!token || !userEmail) {
        showNotification('Vui lòng đăng nhập để mua hàng!', 'warning');
        setTimeout(() => {
            window.location.href = '/login';
        }, 1500);
        return;
    }

    const quantity = parseInt(document.getElementById('quantity').value);
    
    try {
        // Disable button and show loading
        const buyNowBtn = event.target.closest('button');
        const originalContent = buyNowBtn.innerHTML;
        buyNowBtn.disabled = true;
        buyNowBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang xử lý...';

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
                quantity: quantity
            })
        });

        if (response.status === 401) {
            showNotification('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại!', 'warning');
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
        showNotification('Đang chuyển đến trang thanh toán...', 'success');
        
        // Small delay for better UX
        setTimeout(() => {
            window.location.href = '/checkout';
        }, 800);

    } catch (error) {
        console.error('Error buying now:', error);
        showNotification(error.message || 'Không thể mua hàng. Vui lòng thử lại!', 'danger');
        
        // Re-enable button
        const buyNowBtn = event.target.closest('button');
        buyNowBtn.disabled = false;
        buyNowBtn.innerHTML = '<i class="fas fa-rocket me-2"></i>Mua ngay';
    }
}

function viewProduct(id) {
    window.location.href = `/product/${id}`;
}

function formatPrice(price) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
}

// Show notification
function showNotification(message, type = 'info') {
    // Create notification element
    const notification = document.createElement('div');
    
    // Map type to Bootstrap alert class
    let alertClass = 'info';
    let icon = 'info-circle';
    
    if (type === 'success') {
        alertClass = 'success';
        icon = 'check-circle';
    } else if (type === 'warning') {
        alertClass = 'warning';
        icon = 'exclamation-triangle';
    } else if (type === 'danger' || type === 'error') {
        alertClass = 'danger';
        icon = 'exclamation-circle';
    }
    
    notification.className = `alert alert-${alertClass} alert-dismissible fade show position-fixed`;
    notification.style.cssText = 'top: 80px; right: 20px; z-index: 9999; min-width: 300px;';
    notification.innerHTML = `
        <i class="fas fa-${icon} me-2"></i>
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;

    document.body.appendChild(notification);

    // Auto remove after 3 seconds
    setTimeout(() => {
        notification.remove();
    }, 3000);
}

// ===== FAVORITE FUNCTIONS =====

async function toggleFavorite(productId) {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    if (!token || !userEmail) {
        showNotification('Vui lòng đăng nhập để thêm yêu thích!', 'warning');
        setTimeout(() => {
            window.location.href = '/login';
        }, 1500);
        return;
    }
    
    const btn = document.getElementById('favoriteBtn-detail');
    if (!btn) return;
    
    try {
        // Check current state
        const checkResponse = await fetch(`/api/favorites/check/${productId}`, {
            headers: {
                'Authorization': `Bearer ${token}`,
                'X-User-Email': userEmail
            }
        });
        
        if (checkResponse.status === 401) {
            showNotification('Phiên đăng nhập đã hết hạn!', 'warning');
            setTimeout(() => {
                window.location.href = '/login';
            }, 1500);
            return;
        }
        
        const checkData = await checkResponse.json();
        const isFavorite = checkData.isFavorite;
        
        if (isFavorite) {
            // Remove from favorites
            const response = await fetch(`/api/favorites/remove/${productId}`, {
                method: 'DELETE',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'X-User-Email': userEmail
                }
            });
            
            if (!response.ok) throw new Error('Cannot remove favorite');
            
            btn.innerHTML = '<i class="far fa-heart me-2"></i>Thêm vào yêu thích';
            btn.classList.remove('btn-danger');
            btn.classList.add('btn-outline-danger');
            btn.style.borderWidth = '2px';
            showNotification('Đã xóa khỏi yêu thích!', 'success');
        } else {
            // Add to favorites
            const response = await fetch('/api/favorites/add', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json',
                    'X-User-Email': userEmail
                },
                body: JSON.stringify({ productId: productId })
            });
            
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.error || 'Cannot add to favorites');
            }
            
            btn.innerHTML = '<i class="fas fa-heart me-2"></i>Đã yêu thích';
            btn.classList.remove('btn-outline-danger');
            btn.classList.add('btn-danger');
            btn.style.borderWidth = '2px';
            showNotification('Đã thêm vào yêu thích! ❤️', 'success');
        }
    } catch (error) {
        console.error('Error toggling favorite:', error);
        showNotification(error.message || 'Có lỗi xảy ra!', 'danger');
    }
}

async function loadFavoriteState(productId) {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    if (!token || !userEmail) return;
    
    try {
        const response = await fetch(`/api/favorites/check/${productId}`, {
            headers: {
                'Authorization': `Bearer ${token}`,
                'X-User-Email': userEmail
            }
        });
        
        if (!response.ok) return;
        
        const data = await response.json();
        const btn = document.getElementById('favoriteBtn-detail');
        
        if (btn && data.isFavorite) {
            btn.innerHTML = '<i class="fas fa-heart me-2"></i>Đã yêu thích';
            btn.classList.remove('btn-outline-danger');
            btn.classList.add('btn-danger');
            btn.style.borderWidth = '2px';
        } else if (btn) {
            btn.style.borderWidth = '2px';
        }
    } catch (error) {
        console.error('Error loading favorite state:', error);
    }
}

// ===== RATING FUNCTIONS =====

function displayRatingFromProduct(product) {
    const container = document.getElementById('ratingDisplay');
    
    if (!container) return;
    
    const avgRating = product.averageRating || 0;
    const ratingCount = product.ratingCount || 0;
    
    if (ratingCount === 0) {
        container.innerHTML = `
            <div class="d-flex align-items-center">
                <span class="text-muted me-2">Chưa có đánh giá</span>
            </div>
        `;
        return;
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
    
    container.innerHTML = `
        <div class="d-flex align-items-center">
            <span class="me-2">${starsHtml}</span>
            <span class="fw-bold me-2">${avgRating.toFixed(1)}</span>
            <span class="text-muted">(${ratingCount} đánh giá)</span>
        </div>
    `;
}

async function loadProductRating(productId) {
    try {
        const response = await fetch(`/api/ratings/product/${productId}/summary`);
        
        if (!response.ok) {
            document.getElementById('ratingDisplay').innerHTML = '';
            return;
        }
        
        const rating = await response.json();
        displayRating(rating);
    } catch (error) {
        console.error('Error loading rating:', error);
        document.getElementById('ratingDisplay').innerHTML = '';
    }
}

function displayRating(rating) {
    const container = document.getElementById('ratingDisplay');
    
    if (rating.ratingCount === 0) {
        container.innerHTML = `
            <div class="d-flex align-items-center">
                <span class="text-muted me-2">Chưa có đánh giá</span>
            </div>
        `;
        return;
    }
    
    const avgRating = rating.averageRating || 0;
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
    
    container.innerHTML = `
        <div class="d-flex align-items-center">
            <span class="me-2">${starsHtml}</span>
            <span class="fw-bold me-2">${avgRating.toFixed(1)}</span>
            <span class="text-muted">(${rating.ratingCount} đánh giá)</span>
        </div>
    `;
}

// ===== RATING STARS GENERATOR FOR PRODUCT LISTS =====
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
