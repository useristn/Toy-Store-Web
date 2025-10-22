let currentPage = 0;
let currentCategory = null;
let currentSearch = '';
let currentPriceRange = null;
let currentSort = 'newest';

document.addEventListener('DOMContentLoaded', function() {
    // Get parameters from URL
    const urlParams = new URLSearchParams(window.location.search);
    const categoryParam = urlParams.get('category');
    const searchParam = urlParams.get('search');
    
    if (categoryParam) {
        currentCategory = categoryParam;
    }
    
    if (searchParam) {
        currentSearch = searchParam;
        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.value = searchParam;
        }
    }
    
    loadCategories();
    loadProducts();
    
    // Event listeners
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('input', debounce(handleSearch, 500));
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                handleSearch(e);
            }
        });
    }
    
    const priceFilter = document.getElementById('priceFilter');
    if (priceFilter) {
        priceFilter.addEventListener('change', handlePriceChange);
    }
    
    const sortFilter = document.getElementById('sortFilter');
    if (sortFilter) {
        sortFilter.addEventListener('change', handleSortChange);
    }
});

function loadCategories() {
    fetch('/api/products/categories')
        .then(response => response.json())
        .then(categories => {
            const container = document.getElementById('categoryFilter');
            if (!container) return;
            
            container.innerHTML = `
                <div class="form-check mb-2">
                    <input class="form-check-input" type="radio" name="category" id="categoryAll" value="" ${!currentCategory ? 'checked' : ''}>
                    <label class="form-check-label" for="categoryAll">
                        <strong>Tất cả danh mục</strong>
                    </label>
                </div>
                <hr class="my-2">
            `;
            
            categories.forEach(cat => {
                container.innerHTML += `
                    <div class="form-check mb-2">
                        <input class="form-check-input" type="radio" name="category" id="category${cat.id}" value="${cat.id}" ${currentCategory == cat.id ? 'checked' : ''}>
                        <label class="form-check-label" for="category${cat.id}">
                            ${cat.icon ? cat.icon + ' ' : ''}${cat.name}
                        </label>
                    </div>
                `;
            });
            
            document.querySelectorAll('input[name="category"]').forEach(radio => {
                radio.addEventListener('change', handleCategoryChange);
            });
        })
        .catch(error => {
            console.error('Error loading categories:', error);
        });
}

function handleCategoryChange(e) {
    currentCategory = e.target.value || null;
    currentPage = 0;
    loadProducts();
}

function handlePriceChange(e) {
    currentPriceRange = e.target.value || null;
    currentPage = 0;
    loadProducts();
}

function handleSortChange(e) {
    currentSort = e.target.value;
    currentPage = 0;
    loadProducts();
}

function handleSearch(e) {
    currentSearch = e.target.value.trim();
    currentPage = 0;
    
    // Update URL without reload
    const url = new URL(window.location);
    if (currentSearch) {
        url.searchParams.set('search', currentSearch);
    } else {
        url.searchParams.delete('search');
    }
    window.history.pushState({}, '', url);
    
    loadProducts();
}

function loadProducts() {
    const container = document.getElementById('productsContainer');
    if (!container) return;
    
    container.innerHTML = `
        <div class="col-12 text-center py-5">
            <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">Đang tải...</span>
            </div>
            <p class="mt-3 text-muted">Đang tải sản phẩm...</p>
        </div>
    `;
    
    let url = `/api/products?page=${currentPage}&size=12`;
    
    // Apply category filter
    if (currentCategory) {
        url = `/api/products/category/${currentCategory}?page=${currentPage}&size=12`;
    }
    
    // Apply search filter
    if (currentSearch) {
        url = `/api/products/search?keyword=${encodeURIComponent(currentSearch)}&page=${currentPage}&size=12`;
    }
    
    fetch(url)
        .then(response => {
            if (!response.ok) throw new Error('Network response was not ok');
            return response.json();
        })
        .then(data => {
            let products = data.content;
            
            // Client-side price filtering
            if (currentPriceRange) {
                products = filterByPrice(products, currentPriceRange);
            }
            
            // Client-side sorting
            products = sortProducts(products, currentSort);
            
            displayProducts(products);
            updatePagination(data);
            
            const countEl = document.getElementById('productCount');
            if (countEl) countEl.textContent = products.length;
        })
        .catch(error => {
            console.error('Error loading products:', error);
            container.innerHTML = `
                <div class="col-12 text-center py-5">
                    <i class="fas fa-exclamation-triangle fa-3x text-danger mb-3"></i>
                    <p class="text-danger">Không thể tải sản phẩm. Vui lòng thử lại!</p>
                    <button class="btn btn-primary" onclick="loadProducts()">
                        <i class="fas fa-redo me-2"></i>Thử lại
                    </button>
                </div>
            `;
        });
}

function filterByPrice(products, priceRange) {
    if (!priceRange) return products;
    
    const [min, max] = priceRange.split('-').map(Number);
    
    return products.filter(product => {
        const price = product.discountPrice || product.price;
        return price >= min && price <= max;
    });
}

function sortProducts(products, sortType) {
    const sorted = [...products];
    
    switch(sortType) {
        case 'price-asc':
            return sorted.sort((a, b) => {
                const priceA = a.discountPrice || a.price;
                const priceB = b.discountPrice || b.price;
                return priceA - priceB;
            });
        case 'price-desc':
            return sorted.sort((a, b) => {
                const priceA = a.discountPrice || a.price;
                const priceB = b.discountPrice || b.price;
                return priceB - priceA;
            });
        case 'name':
            return sorted.sort((a, b) => a.name.localeCompare(b.name));
        case 'newest':
        default:
            return sorted.sort((a, b) => {
                const dateA = new Date(a.createdAt);
                const dateB = new Date(b.createdAt);
                return dateB - dateA;
            });
    }
}

function displayProducts(products) {
    const container = document.getElementById('productsContainer');
    if (!container) return;
    
    if (products.length === 0) {
        container.innerHTML = `
            <div class="col-12 text-center py-5">
                <i class="fas fa-space-shuttle fa-3x text-muted mb-3"></i>
                <h4 class="text-muted">Không tìm thấy sản phẩm nào</h4>
                <p class="text-muted">Thử điều chỉnh bộ lọc hoặc tìm kiếm khác</p>
                <button class="btn btn-primary" onclick="resetFilters()">
                    <i class="fas fa-redo me-2"></i>Đặt lại bộ lọc
                </button>
            </div>
        `;
        return;
    }
    
    container.innerHTML = products.map(product => `
        <div class="col-lg-4 col-md-6 mb-4">
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
                    
                    <div class="d-flex justify-content-between align-items-end mt-auto">
                        <div>
                            ${product.discountPrice ? 
                                `<div class="h5 text-danger mb-0">${formatPrice(product.discountPrice)}</div>
                                 <small class="text-muted text-decoration-line-through">${formatPrice(product.price)}</small>` :
                                `<div class="h5 text-danger mb-0">${formatPrice(product.price)}</div>`
                            }
                        </div>
                        <div>
                            <button class="btn btn-outline-primary btn-sm me-1" onclick="viewProduct(${product.id})" title="Xem chi tiết">
                                <i class="fas fa-eye"></i>
                            </button>
                            <button class="btn btn-outline-success btn-sm" onclick="addToCart(${product.id}, event)" title="Thêm vào giỏ">
                                <i class="fas fa-cart-plus"></i>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `).join('');
}

function resetFilters() {
    currentCategory = null;
    currentSearch = '';
    currentPriceRange = null;
    currentSort = 'newest';
    currentPage = 0;
    
    // Reset UI
    const searchInput = document.getElementById('searchInput');
    if (searchInput) searchInput.value = '';
    
    const priceFilter = document.getElementById('priceFilter');
    if (priceFilter) priceFilter.value = '';
    
    const sortFilter = document.getElementById('sortFilter');
    if (sortFilter) sortFilter.value = 'newest';
    
    const categoryAll = document.getElementById('categoryAll');
    if (categoryAll) categoryAll.checked = true;
    
    // Clear URL params
    window.history.pushState({}, '', '/products');
    
    loadProducts();
}

function updatePagination(data) {
    const pagination = document.getElementById('pagination');
    if (!pagination) return;
    
    const totalPages = data.totalPages;
    
    if (totalPages <= 1) {
        pagination.innerHTML = '';
        return;
    }
    
    let html = '';
    
    // Previous button
    if (currentPage > 0) {
        html += `<li class="page-item">
                    <a class="page-link" href="#" onclick="changePage(${currentPage - 1}); return false;">
                        <i class="fas fa-chevron-left"></i>
                    </a>
                 </li>`;
    }
    
    // Page numbers with ellipsis
    const maxPages = 5;
    let startPage = Math.max(0, currentPage - 2);
    let endPage = Math.min(totalPages - 1, startPage + maxPages - 1);
    
    if (endPage - startPage < maxPages - 1) {
        startPage = Math.max(0, endPage - maxPages + 1);
    }
    
    if (startPage > 0) {
        html += `<li class="page-item">
                    <a class="page-link" href="#" onclick="changePage(0); return false;">1</a>
                 </li>`;
        if (startPage > 1) {
            html += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
        }
    }
    
    for (let i = startPage; i <= endPage; i++) {
        html += `<li class="page-item ${i === currentPage ? 'active' : ''}">
                    <a class="page-link" href="#" onclick="changePage(${i}); return false;">${i + 1}</a>
                 </li>`;
    }
    
    if (endPage < totalPages - 1) {
        if (endPage < totalPages - 2) {
            html += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
        }
        html += `<li class="page-item">
                    <a class="page-link" href="#" onclick="changePage(${totalPages - 1}); return false;">${totalPages}</a>
                 </li>`;
    }
    
    // Next button
    if (currentPage < totalPages - 1) {
        html += `<li class="page-item">
                    <a class="page-link" href="#" onclick="changePage(${currentPage + 1}); return false;">
                        <i class="fas fa-chevron-right"></i>
                    </a>
                 </li>`;
    }
    
    pagination.innerHTML = html;
}

function changePage(page) {
    currentPage = page;
    loadProducts();
    window.scrollTo({ top: 0, behavior: 'smooth' });
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

function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
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
