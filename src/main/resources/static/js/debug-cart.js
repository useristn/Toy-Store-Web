// Test script cho giỏ hàng - Dán vào Console của browser (F12)

console.log('=== CART DEBUG SCRIPT ===\n');

// 1. Kiểm tra token
const authToken = localStorage.getItem('authToken');
const token = localStorage.getItem('token');
console.log('1. TOKEN CHECK:');
console.log('   authToken:', authToken ? '✅ Có (' + authToken.substring(0, 20) + '...)' : '❌ Không có');
console.log('   token:', token ? '✅ Có (' + token.substring(0, 20) + '...)' : '❌ Không có');

const finalToken = authToken || token;
if (!finalToken) {
    console.error('❌ KHÔNG CÓ TOKEN! Vui lòng đăng nhập.');
} else {
    console.log('✅ Token hợp lệ\n');
    
    // 2. Test API call
    console.log('2. TESTING API CALL:');
    fetch('/api/cart', {
        headers: {
            'Authorization': `Bearer ${finalToken}`,
            'Content-Type': 'application/json'
        }
    })
    .then(response => {
        console.log('   Response status:', response.status);
        if (response.status === 200) {
            console.log('   ✅ API call thành công!');
            return response.json();
        } else if (response.status === 401) {
            console.error('   ❌ Token không hợp lệ hoặc đã hết hạn');
            throw new Error('Unauthorized');
        } else {
            console.error('   ❌ Lỗi:', response.status);
            throw new Error('API Error: ' + response.status);
        }
    })
    .then(cart => {
        console.log('\n3. CART DATA:');
        console.log('   Cart ID:', cart.cartId);
        console.log('   Total Items:', cart.totalItems);
        console.log('   Total Price:', cart.totalPrice);
        console.log('   Items:', cart.items);
        
        if (cart.items && cart.items.length > 0) {
            console.log('\n4. CART ITEMS DETAIL:');
            cart.items.forEach((item, index) => {
                console.log(`   Item ${index + 1}:`);
                console.log(`      - Product: ${item.productName}`);
                console.log(`      - Price: ${item.price}`);
                console.log(`      - Quantity: ${item.quantity}`);
                console.log(`      - Subtotal: ${item.subtotal}`);
                console.log(`      - Stock: ${item.availableStock}`);
            });
        } else {
            console.log('\n   ℹ️ Giỏ hàng trống');
        }
        
        console.log('\n✅ CART LOADED SUCCESSFULLY!');
    })
    .catch(error => {
        console.error('\n❌ LỖI:', error.message);
        console.log('\nGợi ý:');
        console.log('- Kiểm tra server có đang chạy không');
        console.log('- Kiểm tra database connection');
        console.log('- Thử đăng nhập lại');
    });
}

console.log('\n=== END DEBUG ===');
