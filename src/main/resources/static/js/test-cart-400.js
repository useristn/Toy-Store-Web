// Quick Test Script - Paste in Browser Console (F12)
console.log('=== CART API 400 DEBUG ===\n');

// Check localStorage
const authToken = localStorage.getItem('authToken');
const token = localStorage.getItem('token');
const authEmail = localStorage.getItem('authEmail');
const userEmail = localStorage.getItem('userEmail');

console.log('1. LOCALSTORAGE CHECK:');
console.log('   authToken:', authToken ? '✅ ' + authToken.substring(0, 30) + '...' : '❌ NULL');
console.log('   token:', token ? '✅ ' + token.substring(0, 30) + '...' : '❌ NULL');
console.log('   authEmail:', authEmail ? '✅ ' + authEmail : '❌ NULL');
console.log('   userEmail:', userEmail ? '✅ ' + userEmail : '❌ NULL');

const finalToken = authToken || token;
const finalEmail = authEmail || userEmail;

if (!finalToken || !finalEmail) {
    console.error('\n❌ MISSING CREDENTIALS!');
    console.log('Token exists:', !!finalToken);
    console.log('Email exists:', !!finalEmail);
    console.log('\n💡 SOLUTION: Login again to get fresh credentials');
    console.log('   localStorage.clear();');
    console.log('   window.location.href = "/login";');
} else {
    console.log('\n✅ Credentials OK\n');
    
    // Test API call with proper headers
    console.log('2. TESTING /api/cart WITH HEADERS:');
    console.log('   Headers:');
    console.log('   - Authorization: Bearer ' + finalToken.substring(0, 20) + '...');
    console.log('   - X-User-Email: ' + finalEmail);
    
    fetch('/api/cart', {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${finalToken}`,
            'Content-Type': 'application/json',
            'X-User-Email': finalEmail
        }
    })
    .then(response => {
        console.log('\n3. RESPONSE STATUS:', response.status);
        
        if (response.status === 200) {
            console.log('   ✅ SUCCESS!');
            return response.json();
        } else if (response.status === 400) {
            console.error('   ❌ 400 BAD REQUEST');
            console.log('\n   Possible causes:');
            console.log('   1. Email không tồn tại trong DB');
            console.log('   2. Backend validation error');
            console.log('   3. Missing required fields');
            return response.json().then(err => {
                console.error('   Error:', err);
                throw new Error(err.error || '400 Bad Request');
            });
        } else if (response.status === 401) {
            console.error('   ❌ 401 UNAUTHORIZED');
            console.log('   Token hết hạn. Đăng nhập lại!');
            throw new Error('Unauthorized');
        } else if (response.status === 500) {
            console.error('   ❌ 500 SERVER ERROR');
            console.log('   Kiểm tra server console logs!');
            throw new Error('Server Error');
        } else {
            console.error('   ❌ UNEXPECTED STATUS:', response.status);
            throw new Error('Unexpected error: ' + response.status);
        }
    })
    .then(cart => {
        console.log('\n4. CART DATA:');
        console.log('   Cart ID:', cart.cartId);
        console.log('   Items:', cart.totalItems);
        console.log('   Total:', cart.totalPrice);
        
        if (cart.items && cart.items.length > 0) {
            console.log('\n5. ITEMS DETAIL:');
            cart.items.forEach((item, i) => {
                console.log(`   [${i+1}] ${item.productName}`);
                console.log(`       Qty: ${item.quantity} x ${item.price} = ${item.subtotal}`);
            });
        }
        
        console.log('\n✅✅✅ CART API WORKING! ✅✅✅');
    })
    .catch(error => {
        console.error('\n❌❌❌ ERROR:', error.message, '❌❌❌');
        console.log('\n📋 DEBUG STEPS:');
        console.log('1. Check server is running: http://localhost:8080');
        console.log('2. Check database connection');
        console.log('3. Check server console for errors');
        console.log('4. Try login again: localStorage.clear(); location.href="/login"');
        console.log('5. Check user exists in database:');
        console.log(`   SELECT * FROM user WHERE email = '${finalEmail}';`);
    });
}

console.log('\n=== END TEST ===');
