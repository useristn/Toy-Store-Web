// Admin Reviews Management JavaScript
let currentPage = 0;
let pageSize = 10;
let sortBy = 'createdAt';
let sortDirection = 'DESC';
let selectedReviewIds = new Set();
let deleteModal, bulkDeleteModal;
let reviewToDelete = null;

// Initialize on page load
document.addEventListener('DOMContentLoaded', function () {
  initializeModals();
  checkAdminAuth();
  loadStatistics();
  loadReviews();
});

// Initialize Bootstrap modals
function initializeModals() {
  deleteModal = new bootstrap.Modal(document.getElementById('deleteModal'));
  bulkDeleteModal = new bootstrap.Modal(document.getElementById('bulkDeleteModal'));
}

// Check if user is admin
function checkAdminAuth() {
  const token = localStorage.getItem('authToken') || localStorage.getItem('token');
  const userEmail = localStorage.getItem('authEmail') || localStorage.getItem('userEmail');
  const userRole = localStorage.getItem('userRole');

  if (!token || !userEmail) {
    alert('Vui lòng đăng nhập để truy cập trang quản trị!');
    window.location.href = '/login';
    return;
  }

  // Check if user has ADMIN role
  if (!userRole || !userRole.includes('ADMIN')) {
    alert('Bạn không có quyền truy cập trang này!');
    window.location.href = '/login';
    return;
  }

  // Load admin info
  const userName = userEmail.split('@')[0];
  document.getElementById('adminName').textContent = userName;
  document.getElementById('adminEmail').textContent = userEmail;
}

// Load statistics
async function loadStatistics() {
  try {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const response = await fetch('/api/admin/ratings/stats', {
      headers: {
        Authorization: 'Bearer ' + token,
      },
    });

    if (response.ok) {
      const stats = await response.json();
      updateStatistics(stats);
    } else {
      console.error('Failed to load statistics');
    }
  } catch (error) {
    console.error('Error loading statistics:', error);
  }
}

// Update statistics on UI
function updateStatistics(stats) {
  document.getElementById('totalReviews').textContent = stats.totalReviews || 0;
  document.getElementById('averageRating').textContent = (stats.averageRating || 0).toFixed(1);
  document.getElementById('fiveStars').textContent = stats.fiveStars || 0;
  
  const lowStars = (stats.oneStar || 0) + (stats.twoStars || 0);
  document.getElementById('lowStars').textContent = lowStars;
}

// Load reviews with filters
async function loadReviews() {
  try {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const params = buildFilterParams();
    const queryString = new URLSearchParams(params).toString();

    const response = await fetch(`/api/admin/ratings?${queryString}`, {
      headers: {
        Authorization: 'Bearer ' + token,
      },
    });

    if (response.ok) {
      const data = await response.json();
      displayReviews(data);
      updatePagination(data);
    } else {
      showError('Không thể tải danh sách đánh giá');
    }
  } catch (error) {
    console.error('Error loading reviews:', error);
    showError('Lỗi khi tải dữ liệu');
  }
}

// Build filter parameters
function buildFilterParams() {
  const params = {
    page: currentPage,
    size: pageSize,
    sortBy: sortBy,
    sortDirection: sortDirection,
  };

  const userName = document.getElementById('userNameSearch').value.trim();
  if (userName) params.userName = userName;

  const productName = document.getElementById('productNameSearch').value.trim();
  if (productName) params.productName = productName;

  const minStars = document.getElementById('minStars').value;
  if (minStars) params.minStars = minStars;

  const maxStars = document.getElementById('maxStars').value;
  if (maxStars) params.maxStars = maxStars;

  const startDate = document.getElementById('startDate').value;
  if (startDate) params.startDate = startDate + 'T00:00:00';

  const endDate = document.getElementById('endDate').value;
  if (endDate) params.endDate = endDate + 'T23:59:59';

  return params;
}

// Display reviews in table
function displayReviews(data) {
  const tbody = document.getElementById('reviewsTableBody');
  
  if (!data.content || data.content.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="7" class="text-center py-4">
          <i class="fas fa-inbox fa-3x text-muted mb-3"></i>
          <p class="text-muted">Không có đánh giá nào</p>
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = data.content
    .map(
      (review) => `
    <tr>
      <td>
        <input 
          type="checkbox" 
          class="form-check-input review-checkbox" 
          value="${review.id}"
          onchange="handleCheckboxChange()"
        />
      </td>
      <td>
        <img 
          src="${review.productImage || '/images/placeholder.png'}" 
          alt="${review.productName}" 
          class="product-thumbnail"
        />
      </td>
      <td>
        <div class="fw-semibold">${review.productName}</div>
        <small class="text-muted">ID: ${review.productId}</small>
      </td>
      <td>
        <div>${review.userName}</div>
        <small class="text-muted">${review.userEmail}</small>
      </td>
      <td>
        <div class="star-rating">
          ${generateStarRating(review.stars)}
        </div>
      </td>
      <td>
        <div>${formatDate(review.createdAt)}</div>
        <small class="text-muted">${formatTime(review.createdAt)}</small>
      </td>
      <td class="text-center">
        <button 
          class="btn btn-danger btn-sm btn-action" 
          onclick="openDeleteModal(${review.id}, '${escapeHtml(review.productName)}', '${escapeHtml(review.userName)}')"
          title="Xóa đánh giá"
        >
          <i class="fas fa-trash"></i>
        </button>
      </td>
    </tr>
  `
    )
    .join('');
}

// Generate star rating HTML
function generateStarRating(stars) {
  let html = '';
  for (let i = 1; i <= 5; i++) {
    if (i <= stars) {
      html += '<i class="fas fa-star"></i>';
    } else {
      html += '<i class="fas fa-star empty-star"></i>';
    }
  }
  return html + ` <span class="ms-1">(${stars})</span>`;
}

// Format date
function formatDate(dateString) {
  const date = new Date(dateString);
  return date.toLocaleDateString('vi-VN');
}

// Format time
function formatTime(dateString) {
  const date = new Date(dateString);
  return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
}

// Escape HTML to prevent XSS
function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

// Update pagination
function updatePagination(data) {
  const totalPages = data.totalPages;
  const currentPageNum = data.number;
  const totalElements = data.totalElements;
  
  // Update info text
  const start = currentPageNum * pageSize + 1;
  const end = Math.min((currentPageNum + 1) * pageSize, totalElements);
  document.getElementById('paginationInfo').textContent = 
    `Hiển thị ${start} đến ${end} của ${totalElements} đánh giá`;

  // Generate pagination controls
  const paginationControls = document.getElementById('paginationControls');
  paginationControls.innerHTML = '';

  if (totalPages <= 1) return;

  // Previous button
  const prevLi = document.createElement('li');
  prevLi.className = `page-item ${currentPageNum === 0 ? 'disabled' : ''}`;
  prevLi.innerHTML = `
    <a class="page-link" href="#" onclick="changePage(${currentPageNum - 1}); return false;">
      <i class="fas fa-chevron-left"></i>
    </a>
  `;
  paginationControls.appendChild(prevLi);

  // Page numbers
  const maxPagesToShow = 5;
  let startPage = Math.max(0, currentPageNum - Math.floor(maxPagesToShow / 2));
  let endPage = Math.min(totalPages - 1, startPage + maxPagesToShow - 1);
  
  if (endPage - startPage < maxPagesToShow - 1) {
    startPage = Math.max(0, endPage - maxPagesToShow + 1);
  }

  for (let i = startPage; i <= endPage; i++) {
    const li = document.createElement('li');
    li.className = `page-item ${i === currentPageNum ? 'active' : ''}`;
    li.innerHTML = `
      <a class="page-link" href="#" onclick="changePage(${i}); return false;">
        ${i + 1}
      </a>
    `;
    paginationControls.appendChild(li);
  }

  // Next button
  const nextLi = document.createElement('li');
  nextLi.className = `page-item ${currentPageNum === totalPages - 1 ? 'disabled' : ''}`;
  nextLi.innerHTML = `
    <a class="page-link" href="#" onclick="changePage(${currentPageNum + 1}); return false;">
      <i class="fas fa-chevron-right"></i>
    </a>
  `;
  paginationControls.appendChild(nextLi);
}

// Change page
function changePage(page) {
  currentPage = page;
  loadReviews();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

// Change page size
function changePageSize() {
  pageSize = parseInt(document.getElementById('pageSizeSelect').value);
  currentPage = 0;
  loadReviews();
}

// Change sorting
function changeSorting(field) {
  if (sortBy === field) {
    sortDirection = sortDirection === 'ASC' ? 'DESC' : 'ASC';
  } else {
    sortBy = field;
    sortDirection = 'DESC';
  }
  
  updateSortIcons();
  loadReviews();
}

// Update sort icons
function updateSortIcons() {
  // Reset all icons
  document.getElementById('sortStarsIcon').className = 'fas fa-sort';
  document.getElementById('sortCreatedAtIcon').className = 'fas fa-sort';
  
  // Update active icon
  const iconId = sortBy === 'stars' ? 'sortStarsIcon' : 'sortCreatedAtIcon';
  const iconClass = sortDirection === 'ASC' ? 'fas fa-sort-up' : 'fas fa-sort-down';
  document.getElementById(iconId).className = iconClass;
}

// Apply filters
function applyFilters() {
  currentPage = 0;
  loadReviews();
}

// Clear filters
function clearFilters() {
  document.getElementById('userNameSearch').value = '';
  document.getElementById('productNameSearch').value = '';
  document.getElementById('minStars').value = '';
  document.getElementById('maxStars').value = '';
  document.getElementById('startDate').value = '';
  document.getElementById('endDate').value = '';
  
  currentPage = 0;
  loadReviews();
}

// Refresh reviews
function refreshReviews() {
  selectedReviewIds.clear();
  updateBulkActionsUI();
  loadStatistics();
  loadReviews();
}

// Handle checkbox change
function handleCheckboxChange() {
  selectedReviewIds.clear();
  
  document.querySelectorAll('.review-checkbox:checked').forEach((checkbox) => {
    selectedReviewIds.add(parseInt(checkbox.value));
  });
  
  updateBulkActionsUI();
}

// Toggle select all
function toggleSelectAll() {
  const selectAllCheckbox = document.getElementById('selectAllCheckbox');
  const checkboxes = document.querySelectorAll('.review-checkbox');
  
  checkboxes.forEach((checkbox) => {
    checkbox.checked = selectAllCheckbox.checked;
  });
  
  handleCheckboxChange();
}

// Update bulk actions UI
function updateBulkActionsUI() {
  const bulkActionsCard = document.getElementById('bulkActionsCard');
  const selectedCount = document.getElementById('selectedCount');
  
  if (selectedReviewIds.size > 0) {
    bulkActionsCard.style.display = 'block';
    selectedCount.textContent = `${selectedReviewIds.size} đánh giá được chọn`;
  } else {
    bulkActionsCard.style.display = 'none';
  }
  
  // Update select all checkbox state
  const allCheckboxes = document.querySelectorAll('.review-checkbox');
  const checkedCheckboxes = document.querySelectorAll('.review-checkbox:checked');
  const selectAllCheckbox = document.getElementById('selectAllCheckbox');
  
  if (allCheckboxes.length === 0) {
    selectAllCheckbox.checked = false;
    selectAllCheckbox.indeterminate = false;
  } else if (checkedCheckboxes.length === allCheckboxes.length) {
    selectAllCheckbox.checked = true;
    selectAllCheckbox.indeterminate = false;
  } else if (checkedCheckboxes.length > 0) {
    selectAllCheckbox.checked = false;
    selectAllCheckbox.indeterminate = true;
  } else {
    selectAllCheckbox.checked = false;
    selectAllCheckbox.indeterminate = false;
  }
}

// Open delete modal
function openDeleteModal(reviewId, productName, userName) {
  reviewToDelete = reviewId;
  
  const detailsDiv = document.getElementById('reviewDetailsToDelete');
  detailsDiv.innerHTML = `
    <div><strong>Sản phẩm:</strong> ${productName}</div>
    <div><strong>Người dùng:</strong> ${userName}</div>
    <div class="text-muted mt-2">ID: ${reviewId}</div>
  `;
  
  deleteModal.show();
}

// Confirm delete single review
async function confirmDelete() {
  if (!reviewToDelete) return;
  
  try {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const response = await fetch(`/api/admin/ratings/${reviewToDelete}`, {
      method: 'DELETE',
      headers: {
        Authorization: 'Bearer ' + token,
      },
    });

    if (response.ok) {
      showSuccess('Đã xóa đánh giá thành công');
      deleteModal.hide();
      refreshReviews();
    } else {
      const error = await response.json();
      showError(error.error || 'Không thể xóa đánh giá');
    }
  } catch (error) {
    console.error('Error deleting review:', error);
    showError('Lỗi khi xóa đánh giá');
  }
  
  reviewToDelete = null;
}

// Bulk delete reviews
function bulkDeleteReviews() {
  if (selectedReviewIds.size === 0) {
    showError('Vui lòng chọn ít nhất một đánh giá');
    return;
  }
  
  document.getElementById('bulkDeleteCount').textContent = selectedReviewIds.size;
  bulkDeleteModal.show();
}

// Confirm bulk delete
async function confirmBulkDelete() {
  if (selectedReviewIds.size === 0) return;
  
  try {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const response = await fetch('/api/admin/ratings/bulk', {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer ' + token,
      },
      body: JSON.stringify(Array.from(selectedReviewIds)),
    });

    if (response.ok) {
      showSuccess(`Đã xóa ${selectedReviewIds.size} đánh giá thành công`);
      bulkDeleteModal.hide();
      selectedReviewIds.clear();
      refreshReviews();
    } else {
      const error = await response.json();
      showError(error.error || 'Không thể xóa đánh giá');
    }
  } catch (error) {
    console.error('Error bulk deleting reviews:', error);
    showError('Lỗi khi xóa đánh giá');
  }
}

// Show success message
function showSuccess(message) {
  // You can use a toast library here, for now just alert
  alert(message);
}

// Show error message
function showError(message) {
  alert('Lỗi: ' + message);
}

// Logout function
function logout() {
  localStorage.clear();
  window.location.href = '/login';
}
