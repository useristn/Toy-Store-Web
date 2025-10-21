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
                        <div class="card-body">
                            <h5 class="card-title">${product.name}</h5>
                            <div class="d-flex justify-content-between align-items-center">
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

function addToCart(id) {
    const quantity = document.getElementById('quantity').value;
    alert(`Thêm ${quantity} sản phẩm vào giỏ hàng!`);
}

function buyNow(id) {
    const quantity = document.getElementById('quantity').value;
    alert(`Mua ngay ${quantity} sản phẩm!`);
}

function viewProduct(id) {
    window.location.href = `/product/${id}`;
}

function formatPrice(price) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
}
