let currentPage = 0;
let currentSearch = '';
let currentStockFilter = ''; // Add stock filter variable
const pageSize = 20;
let categories = [];

document.addEventListener('DOMContentLoaded', function() {
    if (!checkAdminAuth()) {
        return; // Stop execution if not authenticated
    }
    
    // Check URL parameters for auto-filter
    const urlParams = new URLSearchParams(window.location.search);
    const filterParam = urlParams.get('filter');
    if (filterParam) {
        const stockFilterEl = document.getElementById('stockFilter');
        if (stockFilterEl && (filterParam === 'in-stock' || filterParam === 'low-stock' || filterParam === 'out-of-stock')) {
            stockFilterEl.value = filterParam;
            currentStockFilter = filterParam;
        }
    }
    
    loadCategories();
    loadProducts();
});

function checkAdminAuth() {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    const userRole = localStorage.getItem('userRole');
    
    if (!token || !userEmail) {
        showToast('Vui lòng đăng nhập để truy cập!', 'warning');
        setTimeout(() => {
            window.location.href = '/login?error=unauthorized';
        }, 1500);
        return false;
    }
    
    // Check admin role
    if (!userRole || !userRole.includes('ADMIN')) {
        showToast('Bạn không có quyền truy cập trang này!', 'danger');
        setTimeout(() => {
            window.location.href = '/login?error=access_denied';
        }, 1500);
        return false;
    }
    
    return true;
}

async function loadCategories() {
    try {
        const response = await fetch('/api/products/categories');
        if (response.ok) {
            categories = await response.json();
            
            // Populate category select in modal
            const categorySelect = document.getElementById('productCategory');
            if (categorySelect) {
                categorySelect.innerHTML = '<option value="">Không có danh mục</option>';
                categories.forEach(cat => {
                    categorySelect.innerHTML += `<option value="${cat.id}">${cat.icon || ''} ${cat.name}</option>`;
                });
            }
        }
    } catch (error) {
        console.error('Error loading categories:', error);
    }
}

// Cache for filtered products when client-side search is needed
let cachedFilteredProducts = null;
let cacheKey = null;

async function loadProducts() {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    const tbody = document.getElementById('productsTableBody');
    tbody.innerHTML = `
        <tr>
            <td colspan="9" class="text-center py-5">
                <div class="spinner-border text-primary" role="status"></div>
                <p class="mt-3 text-muted">Đang tải...</p>
            </td>
        </tr>
    `;

    try {
        let url;
        let needsClientSearch = false;
        const currentCacheKey = `${currentStockFilter}-${currentSearch}`;
        
        // Determine URL based on stock filter
        if (currentStockFilter === 'out-of-stock') {
            url = `/api/admin/products/out-of-stock?page=0&size=1000`; // Load all for client-side filtering
            needsClientSearch = currentSearch.length > 0;
        } else if (currentStockFilter === 'low-stock') {
            url = `/api/admin/products/low-stock?page=0&size=1000`; // Load all for client-side filtering
            needsClientSearch = currentSearch.length > 0;
        } else if (currentStockFilter === 'in-stock') {
            url = `/api/admin/products/in-stock?page=0&size=1000`; // Load all for client-side filtering
            needsClientSearch = currentSearch.length > 0;
        } else {
            // Normal listing with backend search
            url = `/api/admin/products?page=${currentPage}&size=${pageSize}`;
            if (currentSearch) {
                url += `&search=${encodeURIComponent(currentSearch)}`;
            }
        }

        let data;
        
        // Use cache if available and key matches
        if (needsClientSearch && cacheKey === currentStockFilter && cachedFilteredProducts) {
            data = { content: cachedFilteredProducts };
        } else {
            const response = await fetch(url, {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'X-User-Email': userEmail
                }
            });

            if (!response.ok) {
                throw new Error('Cannot load products');
            }

            data = await response.json();
            
            // Cache if we loaded all products for a filter
            if (currentStockFilter && !currentSearch) {
                cachedFilteredProducts = data.content;
                cacheKey = currentStockFilter;
            }
        }
        
        // Apply client-side search if needed
        if (needsClientSearch) {
            const searchLower = currentSearch.toLowerCase();
            const allProducts = data.content;
            const filtered = allProducts.filter(product => 
                product.name.toLowerCase().includes(searchLower) ||
                (product.category?.name || '').toLowerCase().includes(searchLower)
            );
            
            // Paginate filtered results
            const totalElements = filtered.length;
            const totalPages = Math.ceil(totalElements / pageSize);
            const start = currentPage * pageSize;
            const end = start + pageSize;
            const paginatedContent = filtered.slice(start, end);
            
            data = {
                content: paginatedContent,
                totalElements: totalElements,
                totalPages: totalPages,
                number: currentPage,
                size: pageSize
            };
        }
        
        displayProducts(data);
        updatePagination(data);
        
        document.getElementById('totalProducts').textContent = data.totalElements || 0;
        
        // Update search result text
        const resultText = document.getElementById('searchResultText');
        if (currentSearch && resultText) {
            const totalResults = data.totalElements || 0;
            resultText.textContent = `Tìm thấy ${totalResults} sản phẩm phù hợp với "${currentSearch}"`;
            resultText.style.display = 'block';
        }

    } catch (error) {
        console.error('Error loading products:', error);
        tbody.innerHTML = `
            <tr>
                <td colspan="9" class="text-center py-5 text-danger">
                    <i class="fas fa-exclamation-triangle fa-3x mb-3"></i>
                    <p>Không thể tải dữ liệu!</p>
                </td>
            </tr>
        `;
        showToast('Không thể tải danh sách sản phẩm!', 'danger');
    }
}

function displayProducts(data) {
    const tbody = document.getElementById('productsTableBody');
    
    if (data.content.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="9" class="text-center py-5">
                    <i class="fas fa-box-open fa-3x text-muted mb-3"></i>
                    <p class="text-muted">Không có sản phẩm nào</p>
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = data.content.map(product => {
        const stockBadge = getStockBadge(product.stock);
        const price = formatPrice(product.price);
        const discountPrice = product.discountPrice ? formatPrice(product.discountPrice) : '-';
        const categoryName = product.category ? `${product.category.icon || ''} ${product.category.name}` : '-';
        
        return `
            <tr>
                <td>${product.id}</td>
                <td>
                    <img src="${product.imageUrl || 'https://via.placeholder.com/60'}" 
                         alt="${product.name}" 
                         style="width: 60px; height: 60px; object-fit: cover; border-radius: 4px;"
                         onerror="this.src='https://via.placeholder.com/60'">
                </td>
                <td>
                    <strong>${product.name}</strong>
                    ${product.featured ? '<span class="badge bg-warning ms-2">⭐ HOT</span>' : ''}
                </td>
                <td>${categoryName}</td>
                <td>${price}</td>
                <td>${discountPrice}</td>
                <td><strong>${product.stock || 0}</strong></td>
                <td>${stockBadge}</td>
                <td>
                    <div class="btn-group btn-group-sm">
                        <button class="btn btn-outline-primary" onclick="editProduct(${product.id})" title="Sửa">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button class="btn btn-outline-danger" onclick="deleteProduct(${product.id}, '${product.name}')" title="Xóa">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function getStockBadge(stock) {
    if (!stock || stock === 0) {
        return '<span class="badge bg-danger">Hết hàng</span>';
    } else if (stock <= 10) {
        return '<span class="badge bg-warning">Sắp hết</span>';
    } else {
        return '<span class="badge bg-success">Còn hàng</span>';
    }
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
    
    // Previous
    if (currentPage > 0) {
        html += `<li class="page-item">
                    <a class="page-link" href="#" onclick="changePage(${currentPage - 1}); return false;">
                        <i class="fas fa-chevron-left"></i>
                    </a>
                 </li>`;
    }
    
    // Pages
    for (let i = 0; i < totalPages; i++) {
        if (i < 3 || i >= totalPages - 3 || (i >= currentPage - 1 && i <= currentPage + 1)) {
            html += `<li class="page-item ${i === currentPage ? 'active' : ''}">
                        <a class="page-link" href="#" onclick="changePage(${i}); return false;">${i + 1}</a>
                     </li>`;
        } else if (i === 3 || i === totalPages - 4) {
            html += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
        }
    }
    
    // Next
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

function searchProducts() {
    const searchInput = document.getElementById('searchInput');
    const clearBtn = document.getElementById('clearSearchBtn');
    const resultText = document.getElementById('searchResultText');
    
    currentSearch = searchInput.value.trim();
    currentPage = 0;
    
    // Show/hide clear button and result text
    if (currentSearch) {
        clearBtn.style.display = 'block';
        resultText.style.display = 'block';
        resultText.textContent = `Đang tìm kiếm: "${currentSearch}"...`;
    } else {
        clearBtn.style.display = 'none';
        resultText.style.display = 'none';
    }
    
    loadProducts();
}

// Handle Enter key in search input
function handleSearchKeypress(event) {
    if (event.key === 'Enter') {
        event.preventDefault();
        searchProducts();
    }
}

// Clear search function
function clearSearch() {
    const searchInput = document.getElementById('searchInput');
    const clearBtn = document.getElementById('clearSearchBtn');
    const resultText = document.getElementById('searchResultText');
    
    searchInput.value = '';
    currentSearch = '';
    currentPage = 0;
    clearBtn.style.display = 'none';
    resultText.style.display = 'none';
    
    loadProducts();
}

function filterByStock() {
    const stockFilter = document.getElementById('stockFilter').value;
    currentStockFilter = stockFilter;
    currentPage = 0;
    
    // Clear cache when changing filter
    cachedFilteredProducts = null;
    cacheKey = null;
    
    loadProducts();
}

function openAddModal() {
    document.getElementById('modalTitle').textContent = 'Thêm sản phẩm mới';
    document.getElementById('productForm').reset();
    document.getElementById('productId').value = '';
    
    // Clear image preview
    removeImage();
    
    const modal = new bootstrap.Modal(document.getElementById('productModal'));
    modal.show();
}

async function editProduct(id) {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    try {
        const response = await fetch(`/api/admin/products/${id}`, {
            headers: {
                'Authorization': `Bearer ${token}`,
                'X-User-Email': userEmail
            }
        });
        
        if (!response.ok) throw new Error('Cannot load product');
        
        const product = await response.json();
        
        // Fill form
        document.getElementById('modalTitle').textContent = 'Sửa sản phẩm';
        document.getElementById('productId').value = product.id;
        document.getElementById('productName').value = product.name;
        document.getElementById('productDescription').value = product.description || '';
        document.getElementById('productPrice').value = product.price;
        document.getElementById('productDiscountPrice').value = product.discountPrice || '';
        document.getElementById('productStock').value = product.stock || 0;
        document.getElementById('productCategory').value = product.category ? product.category.id : '';
        document.getElementById('productFeatured').checked = product.featured || false;
        
        // Show existing image preview
        if (product.imageUrl) {
            const preview = document.getElementById('imagePreview');
            const container = document.getElementById('imagePreviewContainer');
            preview.src = product.imageUrl;
            container.style.display = 'block';
            document.getElementById('existingImageUrl').value = product.imageUrl;
            document.getElementById('imageFileName').textContent = 'Ảnh hiện tại';
            document.getElementById('imageFileSize').textContent = '';
        }
        
        // Clear file input
        document.getElementById('productImageFile').value = '';
        
        const modal = new bootstrap.Modal(document.getElementById('productModal'));
        modal.show();
        
    } catch (error) {
        console.error('Error loading product:', error);
        showToast('Không thể tải thông tin sản phẩm!', 'danger');
    }
}

async function saveProduct() {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    const id = document.getElementById('productId').value;
    const name = document.getElementById('productName').value.trim();
    const description = document.getElementById('productDescription').value.trim();
    const price = document.getElementById('productPrice').value;
    const discountPrice = document.getElementById('productDiscountPrice').value;
    const stock = document.getElementById('productStock').value;
    const categoryId = document.getElementById('productCategory').value;
    const featured = document.getElementById('productFeatured').checked;
    const imageFile = document.getElementById('productImageFile').files[0];
    const existingImageUrl = document.getElementById('existingImageUrl').value;
    
    // Validation
    if (!name || !price || !stock || stock < 0) {
        showToast('Vui lòng điền đầy đủ thông tin bắt buộc!', 'warning');
        return;
    }

    // Check if image is provided (required for new product)
    if (!id && !imageFile) {
        showToast('Vui lòng chọn ảnh sản phẩm!', 'warning');
        return;
    }
    
    try {
        // Show loading
        const saveBtn = document.querySelector('#productModal .btn-primary');
        const originalBtnText = saveBtn.innerHTML;
        saveBtn.disabled = true;
        saveBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang lưu...';

        let response;

        // Use FormData if image is provided
        if (imageFile) {
            const formData = new FormData();
            formData.append('name', name);
            formData.append('description', description);
            formData.append('price', price);
            if (discountPrice) formData.append('discountPrice', discountPrice);
            formData.append('stock', stock);
            if (categoryId) formData.append('categoryId', categoryId);
            formData.append('featured', featured);
            formData.append('image', imageFile);

            const url = id ? `/api/admin/products/${id}/with-image` : '/api/admin/products/with-image';
            const method = id ? 'PUT' : 'POST';

            response = await fetch(url, {
                method: method,
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'X-User-Email': userEmail
                },
                body: formData
            });
        } else {
            // No new image, use JSON for update (existing image URL is kept)
            const data = {
                name: name,
                description: description,
                price: parseFloat(price),
                discountPrice: discountPrice ? parseFloat(discountPrice) : null,
                stock: parseInt(stock),
                categoryId: categoryId ? parseInt(categoryId) : null,
                imageUrl: existingImageUrl,
                featured: featured
            };

            response = await fetch(`/api/admin/products/${id}`, {
                method: 'PUT',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'X-User-Email': userEmail,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(data)
            });
        }
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Cannot save product');
        }
        
        showToast(id ? 'Cập nhật sản phẩm thành công!' : 'Thêm sản phẩm thành công!', 'success');
        
        // Close modal and reload
        const modal = bootstrap.Modal.getInstance(document.getElementById('productModal'));
        modal.hide();
        
        // Reset button
        saveBtn.disabled = false;
        saveBtn.innerHTML = originalBtnText;
        
        loadProducts();
        
    } catch (error) {
        console.error('Error saving product:', error);
        showToast(error.message || 'Không thể lưu sản phẩm!', 'danger');
        
        // Reset button on error
        const saveBtn = document.querySelector('#productModal .btn-primary');
        saveBtn.disabled = false;
        saveBtn.innerHTML = '<i class="fas fa-save me-2"></i>Lưu';
    }
}

async function deleteProduct(id, name) {
    if (!confirm(`Bạn có chắc muốn xóa sản phẩm "${name}"?\n\nHành động này không thể hoàn tác!`)) {
        return;
    }
    
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    
    try {
        const response = await fetch(`/api/admin/products/${id}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`,
                'X-User-Email': userEmail
            }
        });
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Cannot delete product');
        }
        
        showToast('Xóa sản phẩm thành công!', 'success');
        loadProducts();
        
    } catch (error) {
        console.error('Error deleting product:', error);
        showToast(error.message || 'Không thể xóa sản phẩm!', 'danger');
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
    }, 3000);
}

// Logout function
function logout() {
    if (confirm('Bạn có chắc muốn đăng xuất?')) {
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
}

// Update admin info in sidebar
document.addEventListener('DOMContentLoaded', function() {
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
    if (userEmail) {
        const adminEmailEl = document.getElementById('adminEmail');
        const adminNameEl = document.getElementById('adminName');
        if (adminEmailEl) adminEmailEl.textContent = userEmail;
        if (adminNameEl) adminNameEl.textContent = userEmail.split('@')[0];
    }
});

// ==================== IMAGE UPLOAD FUNCTIONS ====================

/**
 * Preview image before upload
 */
function previewImage(event) {
    const file = event.target.files[0];
    
    if (!file) {
        removeImage();
        return;
    }

    // Validate file type
    const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
    if (!validTypes.includes(file.type)) {
        showToast('Vui lòng chọn file ảnh hợp lệ (JPG, PNG, WebP)', 'danger');
        event.target.value = '';
        return;
    }

    // Validate file size (max 10MB)
    const maxSize = 10 * 1024 * 1024; // 10MB
    if (file.size > maxSize) {
        showToast('Kích thước file không được vượt quá 10MB', 'danger');
        event.target.value = '';
        return;
    }

    // Show preview
    const reader = new FileReader();
    reader.onload = function(e) {
        const preview = document.getElementById('imagePreview');
        const container = document.getElementById('imagePreviewContainer');
        const fileName = document.getElementById('imageFileName');
        const fileSize = document.getElementById('imageFileSize');

        preview.src = e.target.result;
        container.style.display = 'block';
        fileName.textContent = file.name;
        fileSize.textContent = formatFileSize(file.size);
    };
    reader.readAsDataURL(file);
}

/**
 * Remove selected image
 */
function removeImage() {
    const fileInput = document.getElementById('productImageFile');
    const preview = document.getElementById('imagePreview');
    const container = document.getElementById('imagePreviewContainer');
    
    fileInput.value = '';
    preview.src = '';
    container.style.display = 'none';
    
    // Clear hidden fields
    document.getElementById('existingImageUrl').value = '';
}

/**
 * Format file size for display
 */
function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
}

/**
 * Upload image to Cloudinary via backend
 */
async function uploadImage(file) {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');

    const formData = new FormData();
    formData.append('image', file);

    try {
        const response = await fetch('/api/admin/products/upload-image', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'X-User-Email': userEmail
            },
            body: formData
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Upload failed');
        }

        return await response.json(); // Returns { url, publicId }

    } catch (error) {
        console.error('Error uploading image:', error);
        throw error;
    }
}
