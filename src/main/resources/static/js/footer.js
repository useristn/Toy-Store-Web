/**
 * Footer Categories Loader
 * Loads product categories dynamically into the footer
 */

document.addEventListener('DOMContentLoaded', function() {
    loadFooterCategories();
});

/**
 * Load categories and populate footer
 */
async function loadFooterCategories() {
    const footerCategoriesEl = document.getElementById('footerCategories');
    
    if (!footerCategoriesEl) {
        return; // Element not found, skip
    }
    
    try {
        const response = await fetch('/api/products/categories');
        
        if (!response.ok) {
            throw new Error('Failed to load categories');
        }
        
        const categories = await response.json();
        
        // Build categories HTML
        let categoriesHTML = `
            <li class="mb-1">
                <a href="/products" class="text-light text-decoration-none hover-danger">
                    Tất cả sản phẩm
                </a>
            </li>
        `;
        
        // Add each category
        categories.forEach(category => {
            const categoryUrl = `/products?category=${category.id}`;
            
            categoriesHTML += `
                <li class="mb-1">
                    <a href="${categoryUrl}" class="text-light text-decoration-none hover-danger">
                        ${category.name}
                    </a>
                </li>
            `;
        });
        
        footerCategoriesEl.innerHTML = categoriesHTML;
        
    } catch (error) {
        console.error('Error loading footer categories:', error);
        
        // Fallback content if loading fails
        footerCategoriesEl.innerHTML = `
            <li class="mb-1">
                <a href="/products" class="text-light text-decoration-none hover-danger">
                    Tất cả sản phẩm
                </a>
            </li>
            <li class="mb-1">
                <a href="/products" class="text-light text-decoration-none hover-danger">
                    Xem tất cả danh mục
                </a>
            </li>
        `;
    }
}
