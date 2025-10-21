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
                <div class="card-body">
                    <h5 class="card-title" style="cursor: pointer;" onclick="viewProduct(${product.id})">${product.name}</h5>
                    <p class="card-text text-muted">${product.description ? (product.description.length > 50 ? product.description.substring(0, 50) + '...' : product.description) : 'Sản phẩm tuyệt vời!'}</p>
                    <div class="d-flex justify-content-between align-items-center">
                        <div>
                            ${product.discountPrice ? 
                                `<span class="h5 text-danger mb-0">${formatPrice(product.discountPrice)}</span>
                                 <span class="text-muted text-decoration-line-through small d-block">${formatPrice(product.price)}</span>` :
                                `<span class="h5 text-danger mb-0">${formatPrice(product.price)}</span>`
                            }
                        </div>
                        <button class="btn btn-outline-primary btn-sm" onclick="addToCart(${product.id}, event)">
                            <i class="fas fa-cart-plus"></i>
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

function addToCart(id, event) {
    if (event) {
        event.stopPropagation();
    }
    
    // TODO: Implement add to cart functionality
    const Toast = {
        show: function(message, type = 'success') {
            const toastDiv = document.createElement('div');
            toastDiv.className = `alert alert-${type} position-fixed top-0 start-50 translate-middle-x mt-3`;
            toastDiv.style.zIndex = '9999';
            toastDiv.innerHTML = `
                <i class="fas fa-${type === 'success' ? 'check-circle' : 'exclamation-circle'} me-2"></i>
                ${message}
            `;
            document.body.appendChild(toastDiv);
            
            setTimeout(() => {
                toastDiv.style.opacity = '0';
                toastDiv.style.transition = 'opacity 0.5s';
                setTimeout(() => toastDiv.remove(), 500);
            }, 2000);
        }
    };
    
    Toast.show('Đã thêm vào giỏ hàng! 🚀', 'success');
}

function formatPrice(price) {
    return new Intl.NumberFormat('vi-VN', { 
        style: 'currency', 
        currency: 'VND' 
    }).format(price);
}
