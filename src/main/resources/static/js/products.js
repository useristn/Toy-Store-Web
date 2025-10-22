let currentPage = 0;
let currentCategory = null;
let currentSearch = '';
let currentPriceRange = null;
let currentSort = 'newest';

document.addEventListener('DOMContentLoaded', function() {
    console.log('Products page loaded - Version 3.0');
    
    // Get parameters from URL
    const urlParams = new URLSearchParams(window.location.search);
    const categoryParam = urlParams.get('category');
    const searchParam = urlParams.get('search');
    
    if (categoryParam) {
        currentCategory = categoryParam;
    }
    
    if (searchParam) {
        currentSearch = searchParam;
        // Set value for BOTH search inputs (header and filter)
        const headerSearchInput = document.getElementById('searchInput');
        const filterSearchInput = document.getElementById('filterSearchInput');
        if (headerSearchInput) {
            headerSearchInput.value = searchParam;
        }
        if (filterSearchInput) {
            filterSearchInput.value = searchParam;
        }
    }
    
    loadCategories();
    loadProducts();
    
    // Set default sort value in dropdown
    const sortFilter = document.getElementById('sortFilter');
    if (sortFilter) {
        sortFilter.value = currentSort;
        sortFilter.addEventListener('change', handleSortChange);
    }
    
    // Header search (same as home.js)
    const headerSearchBtn = document.getElementById('searchBtn');
    const headerSearchInput = document.getElementById('searchInput');
    
    if (headerSearchBtn && headerSearchInput) {
        console.log('Header search found, attaching listeners');
        headerSearchBtn.addEventListener('click', function() {
            performHeaderSearch();
        });
        
        headerSearchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                performHeaderSearch();
            }
        });
    }
    
    // Filter search (simple version - just reload page with search param)
    const filterSearchBtn = document.getElementById('filterSearchBtn');
    const filterSearchInput = document.getElementById('filterSearchInput');
    
    if (filterSearchBtn && filterSearchInput) {
        console.log('Filter search found, attaching listeners');
        
        filterSearchBtn.addEventListener('click', function() {
            performFilterSearch();
        });
        
        filterSearchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                performFilterSearch();
            }
        });
        
        // Also debounced search on input
        filterSearchInput.addEventListener('input', debounce(function() {
            performFilterSearch();
        }, 800));
    } else {
        console.error('Filter search input NOT found!');
    }
    
    const priceFilter = document.getElementById('priceFilter');
    if (priceFilter) {
        priceFilter.addEventListener('change', handlePriceChange);
    }
});

function performHeaderSearch() {
    const searchInput = document.getElementById('searchInput');
    if (!searchInput) return;
    
    const keyword = searchInput.value.trim();
    if (keyword) {
        console.log('Header search:', keyword);
        window.location.href = `/products?search=${encodeURIComponent(keyword)}`;
    } else {
        window.location.href = `/products`;
    }
}

function performFilterSearch() {
    const searchInput = document.getElementById('filterSearchInput');
    if (!searchInput) return;
    
    const keyword = searchInput.value.trim();
    console.log('Filter search triggered:', keyword);
    
    // Visual feedback
    searchInput.style.borderColor = '#0d6efd';
    searchInput.style.boxShadow = '0 0 0 0.25rem rgba(13, 110, 253, 0.25)';
    setTimeout(() => {
        searchInput.style.borderColor = '';
        searchInput.style.boxShadow = '';
    }, 300);
    
    // Build URL with current filters
    const url = new URL(window.location.href);
    url.search = ''; // Clear all params
    
    if (keyword) {
        url.searchParams.set('search', keyword);
    }
    
    if (currentCategory) {
        url.searchParams.set('category', currentCategory);
    }
    
    console.log('Redirecting to:', url.toString());
    window.location.href = url.toString();
}

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
    
    // Build filter URL with all parameters
    let url = `/api/products/filter?page=${currentPage}&size=12`;
    
    // Add search keyword
    if (currentSearch && currentSearch.trim() !== '') {
        url += `&keyword=${encodeURIComponent(currentSearch.trim())}`;
        console.log('Searching with keyword:', currentSearch);
    }
    
    // Add category filter
    if (currentCategory) {
        url += `&categoryId=${currentCategory}`;
        console.log('Filtering by category:', currentCategory);
    }
    
    // Add price range filter
    if (currentPriceRange) {
        const [minPrice, maxPrice] = currentPriceRange.split('-').map(Number);
        url += `&minPrice=${minPrice}&maxPrice=${maxPrice}`;
        console.log('Filtering by price range:', minPrice, '-', maxPrice);
    }
    
    // Add sort parameter to backend
    if (currentSort) {
        url += `&sort=${currentSort}`;
        console.log('Sorting by:', currentSort);
    }
    
    console.log('Fetching products from:', url);
    
    fetch(url)
        .then(response => {
            if (!response.ok) throw new Error('Network response was not ok');
            return response.json();
        })
        .then(data => {
            // No more client-side sorting - backend handles it
            displayProducts(data.content);
            updatePagination(data);
            
            // Update product count with search info
            const countEl = document.getElementById('productCount');
            if (countEl) {
                const totalElements = data.totalElements || data.content.length;
                countEl.textContent = totalElements;
                
                // Show search info if searching
                const searchInfo = document.getElementById('searchInfo');
                if (currentSearch) {
                    if (!searchInfo) {
                        const badge = document.createElement('span');
                        badge.id = 'searchInfo';
                        badge.className = 'badge bg-info ms-2';
                        badge.textContent = `Tìm kiếm: "${currentSearch}"`;
                        countEl.parentElement.appendChild(badge);
                    } else {
                        searchInfo.textContent = `Tìm kiếm: "${currentSearch}"`;
                    }
                } else if (searchInfo) {
                    searchInfo.remove();
                }
            }
            
            console.log(`Loaded ${data.content.length} products (total: ${data.totalElements})`);
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
                    
                    <div class="mt-auto">
                        <div class="d-flex justify-content-between align-items-center mb-2">
                            <div>
                                ${product.discountPrice ? 
                                    `<div class="h5 text-danger mb-0">${formatPrice(product.discountPrice)}</div>
                                     <small class="text-muted text-decoration-line-through">${formatPrice(product.price)}</small>` :
                                    `<div class="h5 text-danger mb-0">${formatPrice(product.price)}</div>`
                                }
                            </div>
                            <div class="d-flex gap-2">
                                <button class="btn btn-outline-danger btn-sm rounded-circle" 
                                        style="width: 40px; height: 40px; padding: 0; border-width: 2px;" 
                                        onclick="toggleFavorite(${product.id}, event)" 
                                        title="Yêu thích" 
                                        id="favoriteBtn-${product.id}">
                                    <i class="far fa-heart"></i>
                                </button>
                                <button class="btn btn-outline-primary btn-sm rounded-circle" 
                                        style="width: 40px; height: 40px; padding: 0; border-width: 2px;" 
                                        onclick="viewProduct(${product.id})" 
                                        title="Xem chi tiết">
                                    <i class="fas fa-eye"></i>
                                </button>
                            </div>
                        </div>
                        <div class="d-grid gap-2">
                            <button class="btn btn-outline-success btn-sm" onclick="addToCart(${product.id}, event)" title="Thêm vào giỏ">
                                <i class="fas fa-cart-plus me-1"></i>Thêm vào giỏ
                            </button>
                            <button class="btn btn-primary btn-sm" onclick="buyNow(${product.id}, event)" title="Mua ngay">
                                <i class="fas fa-rocket me-1"></i>Mua ngay
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `).join('');
    
    // Load favorite states after rendering
    loadFavoriteStates();
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
    
    // Previous button with modern styling
    if (currentPage > 0) {
        html += `<li class="page-item">
                    <a class="page-link rounded-start" href="#" onclick="changePage(${currentPage - 1}); return false;" 
                       style="border: 2px solid #6c5ce7; color: #6c5ce7; font-weight: 500; padding: 0.5rem 0.75rem;">
                        <i class="fas fa-chevron-left"></i>
                    </a>
                 </li>`;
    } else {
        html += `<li class="page-item disabled">
                    <span class="page-link rounded-start" 
                          style="border: 2px solid #dee2e6; color: #6c757d; padding: 0.5rem 0.75rem;">
                        <i class="fas fa-chevron-left"></i>
                    </span>
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
                    <a class="page-link mx-1" href="#" onclick="changePage(0); return false;"
                       style="border: 2px solid #6c5ce7; color: #6c5ce7; font-weight: 500; padding: 0.5rem 0.75rem; border-radius: 0.5rem;">
                        1
                    </a>
                 </li>`;
        if (startPage > 1) {
            html += `<li class="page-item disabled">
                        <span class="page-link" style="border: none; color: #6c757d; padding: 0.5rem 0.5rem;">...</span>
                     </li>`;
        }
    }
    
    for (let i = startPage; i <= endPage; i++) {
        if (i === currentPage) {
            html += `<li class="page-item active">
                        <span class="page-link mx-1" 
                              style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); 
                                     border: 2px solid #6c5ce7; color: white; font-weight: 600; 
                                     padding: 0.5rem 0.75rem; border-radius: 0.5rem; box-shadow: 0 4px 6px rgba(108, 92, 231, 0.3);">
                            ${i + 1}
                        </span>
                     </li>`;
        } else {
            html += `<li class="page-item">
                        <a class="page-link mx-1" href="#" onclick="changePage(${i}); return false;"
                           style="border: 2px solid #6c5ce7; color: #6c5ce7; font-weight: 500; 
                                  padding: 0.5rem 0.75rem; border-radius: 0.5rem; transition: all 0.3s ease;"
                           onmouseover="this.style.background='linear-gradient(135deg, #667eea 0%, #764ba2 100%)'; this.style.color='white';"
                           onmouseout="this.style.background=''; this.style.color='#6c5ce7';">
                            ${i + 1}
                        </a>
                     </li>`;
        }
    }
    
    if (endPage < totalPages - 1) {
        if (endPage < totalPages - 2) {
            html += `<li class="page-item disabled">
                        <span class="page-link" style="border: none; color: #6c757d; padding: 0.5rem 0.5rem;">...</span>
                     </li>`;
        }
        html += `<li class="page-item">
                    <a class="page-link mx-1" href="#" onclick="changePage(${totalPages - 1}); return false;"
                       style="border: 2px solid #6c5ce7; color: #6c5ce7; font-weight: 500; padding: 0.5rem 0.75rem; border-radius: 0.5rem;">
                        ${totalPages}
                    </a>
                 </li>`;
    }
    
    // Next button with modern styling
    if (currentPage < totalPages - 1) {
        html += `<li class="page-item">
                    <a class="page-link rounded-end" href="#" onclick="changePage(${currentPage + 1}); return false;"
                       style="border: 2px solid #6c5ce7; color: #6c5ce7; font-weight: 500; padding: 0.5rem 0.75rem;">
                        <i class="fas fa-chevron-right"></i>
                    </a>
                 </li>`;
    } else {
        html += `<li class="page-item disabled">
                    <span class="page-link rounded-end" 
                          style="border: 2px solid #dee2e6; color: #6c757d; padding: 0.5rem 0.75rem;">
                        <i class="fas fa-chevron-right"></i>
                    </span>
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

async function buyNow(id, event) {
    if (event) event.stopPropagation();
    
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
    const icon = type === 'success' ? 'check-circle' : type === 'warning' ? 'exclamation-triangle' : 'exclamation-circle';
    toastDiv.innerHTML = `<i class="fas fa-${icon} me-2"></i>${message}`;
    document.body.appendChild(toastDiv);
    
    setTimeout(() => {
        toastDiv.style.opacity = '0';
        toastDiv.style.transition = 'opacity 0.5s';
        setTimeout(() => toastDiv.remove(), 500);
    }, 2000);
}

// ===== FAVORITE FUNCTIONS =====

async function toggleFavorite(productId, event) {
    if (event) event.stopPropagation();
    
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    if (!token || !userEmail) {
        showToast('Vui lòng đăng nhập để thêm yêu thích!', 'warning');
        setTimeout(() => {
            window.location.href = '/login';
        }, 1500);
        return;
    }
    
    const btn = document.getElementById(`favoriteBtn-${productId}`);
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
            showToast('Phiên đăng nhập đã hết hạn!', 'warning');
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
            
            btn.innerHTML = '<i class="far fa-heart"></i>';
            btn.classList.remove('btn-danger');
            btn.classList.add('btn-outline-danger');
            btn.style.borderWidth = '2px';
            showToast('Đã xóa khỏi yêu thích!', 'success');
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
            
            btn.innerHTML = '<i class="fas fa-heart"></i>';
            btn.classList.remove('btn-outline-danger');
            btn.classList.add('btn-danger');
            btn.style.borderWidth = '2px';
            showToast('Đã thêm vào yêu thích! ❤️', 'success');
        }
    } catch (error) {
        console.error('Error toggling favorite:', error);
        showToast(error.message || 'Có lỗi xảy ra!', 'danger');
    }
}

async function loadFavoriteStates() {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    if (!token || !userEmail) return;
    
    try {
        const response = await fetch('/api/favorites/product-ids', {
            headers: {
                'Authorization': `Bearer ${token}`,
                'X-User-Email': userEmail
            }
        });
        
        if (!response.ok) return;
        
        const favoriteIds = await response.json();
        
        // Update UI for favorite products
        favoriteIds.forEach(productId => {
            const btn = document.getElementById(`favoriteBtn-${productId}`);
            if (btn) {
                btn.innerHTML = '<i class="fas fa-heart"></i>';
                btn.classList.remove('btn-outline-danger');
                btn.classList.add('btn-danger');
                btn.style.borderWidth = '2px';
            }
        });
    } catch (error) {
        console.error('Error loading favorite states:', error);
    }
}
