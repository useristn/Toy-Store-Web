// Customer Support Chat JavaScript
let supportStompClient = null;
let supportSessionId = null;
let userInfo = null;

// Helper function to get auth token
function getAuthToken() {
    return localStorage.getItem('authToken');
}

// Wait for DOM to be fully loaded
function initSupportChat() {
    console.log('Initializing support chat...');
    
    const openSupportBtn = document.getElementById('openSupportChat');
    if (openSupportBtn) {
        console.log('Support button found, attaching click listener');
        openSupportBtn.addEventListener('click', openSupportChat);
    } else {
        console.error('Support button not found!');
    }
    
    const supportChatForm = document.getElementById('supportChatForm');
    if (supportChatForm) {
        console.log('Support form found, attaching submit listener');
        supportChatForm.addEventListener('submit', sendSupportMessage);
    } else {
        console.log('Support form not found yet (will be in modal)');
    }
    
    // Typing indicator
    const messageInput = document.getElementById('supportMessageInput');
    if (messageInput) {
        let typingTimeout;
        messageInput.addEventListener('input', function() {
            if (supportSessionId && supportStompClient && supportStompClient.connected) {
                clearTimeout(typingTimeout);
                sendTypingIndicatorCustomer();
                typingTimeout = setTimeout(() => {
                    // Stop typing indicator after 1 second
                }, 1000);
            }
        });
    }
    
    // Close modal cleanup
    const modal = document.getElementById('supportChatModal');
    if (modal) {
        modal.addEventListener('hidden.bs.modal', function() {
            if (supportStompClient && supportStompClient.connected) {
                const leaveMessage = {
                    sessionId: supportSessionId,
                    senderType: 'USER',
                    messageType: 'LEAVE'
                };
                supportStompClient.send('/app/support.leave', {}, JSON.stringify(leaveMessage));
            }
        });
    }
}

// Initialize when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initSupportChat);
} else {
    initSupportChat();
}

function openSupportChat() {
    console.log('Opening support chat...');
    // Get user info from profile
    getUserInfo().then(() => {
        const modal = new bootstrap.Modal(document.getElementById('supportChatModal'));
        modal.show();
        
        // Re-attach form listener after modal is shown
        setTimeout(() => {
            const supportChatForm = document.getElementById('supportChatForm');
            if (supportChatForm && !supportChatForm.hasAttribute('data-listener-attached')) {
                console.log('Attaching form submit listener in modal');
                supportChatForm.addEventListener('submit', sendSupportMessage);
                supportChatForm.setAttribute('data-listener-attached', 'true');
            }
        }, 100);
        
        // Initialize support session
        initializeSupportSession();
    }).catch(error => {
        console.error('Error getting user info:', error);
        alert('Không thể lấy thông tin người dùng. Vui lòng đăng nhập lại.');
    });
}

function getUserInfo() {
    return fetch('/api/user/profile', {
        headers: {
            'Authorization': `Bearer ${localStorage.getItem('authToken')}`,
            'Content-Type': 'application/json'
        }
    })
    .then(response => response.json())
    .then(data => {
        userInfo = data;
        return data;
    })
    .catch(error => {
        console.error('Error getting user info:', error);
        showAlert('Không thể lấy thông tin người dùng', 'danger');
    });
}

function initializeSupportSession() {
    console.log('Initializing support session...');
    
    const token = getAuthToken();
    if (!token) {
        console.error('No auth token found');
        document.getElementById('supportMessages').innerHTML = `
            <div class="alert alert-warning m-3">
                <i class="fas fa-exclamation-triangle"></i> 
                <strong>Chưa đăng nhập</strong><br>
                Vui lòng đăng nhập để sử dụng tính năng hỗ trợ.
                <br><a href="/login" class="btn btn-sm btn-primary mt-2">Đăng nhập</a>
            </div>
        `;
        return;
    }
    
    fetch('/api/support/session', {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    })
    .then(response => {
        console.log('Session response status:', response.status);
        if (!response.ok) {
            return response.text().then(text => {
                throw new Error(`HTTP ${response.status}: ${text}`);
            });
        }
        return response.json();
    })
    .then(session => {
        console.log('Session created:', session);
        supportSessionId = session.sessionId;
        
        // Connect WebSocket AFTER session is created
        connectSupportWebSocket();
        
        // Load existing messages
        loadSupportMessages();
    })
    .catch(error => {
        console.error('Error creating support session:', error);
        document.getElementById('supportMessages').innerHTML = `
            <div class="alert alert-danger m-3">
                <i class="fas fa-exclamation-triangle"></i> 
                <strong>Không thể kết nối</strong><br>
                <small>${error.message}</small><br>
                <button class="btn btn-sm btn-primary mt-2" onclick="location.reload()">
                    Tải lại trang
                </button>
            </div>
        `;
    });
}

function connectSupportWebSocket() {
    console.log('Connecting to WebSocket with session:', supportSessionId);
    
    if (!supportSessionId) {
        console.error('Cannot connect WebSocket: No session ID');
        return;
    }
    
    const socket = new SockJS('/ws-support');
    supportStompClient = Stomp.over(socket);
    
    supportStompClient.connect({}, function(frame) {
        console.log('Connected to support WebSocket:', frame);
        
        // Subscribe to session messages
        supportStompClient.subscribe('/topic/support.' + supportSessionId, function(message) {
            const chatMessage = JSON.parse(message.body);
            console.log('Received message:', chatMessage);
            
            if (chatMessage.messageType === 'CHAT') {
                displaySupportMessage(chatMessage);
                
                // Mark admin messages as read
                if (chatMessage.senderType === 'ADMIN') {
                    markSupportAsRead();
                }
            } else if (chatMessage.messageType === 'TYPING' && chatMessage.senderType === 'ADMIN') {
                showTypingIndicatorCustomer();
            }
        });
        
        // Send join notification
        const joinMessage = {
            sessionId: supportSessionId,
            userId: userInfo.id,
            userName: userInfo.name,
            userEmail: userInfo.email,
            senderType: 'USER',
            messageType: 'JOIN'
        };
        supportStompClient.send('/app/support.join', {}, JSON.stringify(joinMessage));
        console.log('Sent join notification');
        
    }, function(error) {
        console.error('Support WebSocket connection error:', error);
        document.getElementById('supportMessages').innerHTML = `
            <div class="alert alert-warning m-3">
                <i class="fas fa-exclamation-circle"></i> Mất kết nối. Đang thử kết nối lại...
            </div>
        `;
        setTimeout(connectSupportWebSocket, 5000);
    });
}

function loadSupportMessages() {
    fetch(`/api/support/session/${supportSessionId}/messages`, {
        headers: {
            'Authorization': `Bearer ${localStorage.getItem('authToken')}`,
            'Content-Type': 'application/json'
        }
    })
    .then(response => response.json())
    .then(messages => {
        const supportMessages = document.getElementById('supportMessages');
        supportMessages.innerHTML = '';
        
        if (messages.length === 0) {
            supportMessages.innerHTML = `
                <div class="text-center py-5 text-muted">
                    <i class="fas fa-comments fa-3x mb-3"></i>
                    <p>Bắt đầu cuộc trò chuyện với admin</p>
                    <small>Admin sẽ phản hồi trong thời gian sớm nhất</small>
                </div>
            `;
        } else {
            messages.forEach(msg => {
                displaySupportMessage({
                    senderType: msg.senderType,
                    userName: msg.userName,
                    message: msg.message,
                    createdAt: msg.createdAt
                });
            });
        }
        
        scrollSupportToBottom();
        markSupportAsRead();
    })
    .catch(error => {
        console.error('Error loading support messages:', error);
    });
}

function displaySupportMessage(chatMessage) {
    const supportMessages = document.getElementById('supportMessages');
    
    // Remove empty state if exists
    const emptyState = supportMessages.querySelector('.text-center.py-5');
    if (emptyState) {
        emptyState.remove();
    }
    
    const messageDiv = document.createElement('div');
    messageDiv.className = `message-customer ${chatMessage.senderType.toLowerCase()}`;
    
    const time = new Date(chatMessage.createdAt).toLocaleTimeString('vi-VN', {
        hour: '2-digit',
        minute: '2-digit'
    });
    
    messageDiv.innerHTML = `
        <div class="message-bubble-customer">
            <div class="message-sender-customer">${chatMessage.senderType === 'USER' ? 'Bạn' : 'Admin'}</div>
            <div class="message-text-customer">${escapeHtmlCustomer(chatMessage.message)}</div>
            <div class="message-time-customer">${time}</div>
        </div>
    `;
    
    supportMessages.appendChild(messageDiv);
    scrollSupportToBottom();
}

function sendSupportMessage(e) {
    e.preventDefault();
    console.log('Sending support message...');
    
    const messageInput = document.getElementById('supportMessageInput');
    const message = messageInput.value.trim();
    
    console.log('Message:', message);
    console.log('Session ID:', supportSessionId);
    console.log('Stomp Client:', supportStompClient);
    console.log('User Info:', userInfo);
    
    if (!message) {
        console.warn('Message is empty');
        return;
    }
    
    if (!supportSessionId) {
        console.error('No session ID');
        alert('Chưa kết nối với hệ thống. Vui lòng thử lại.');
        return;
    }
    
    if (!supportStompClient || !supportStompClient.connected) {
        console.error('WebSocket not connected');
        alert('Mất kết nối. Vui lòng đóng và mở lại cửa sổ chat.');
        return;
    }
    
    if (!userInfo) {
        console.error('No user info');
        alert('Không có thông tin người dùng. Vui lòng thử lại.');
        return;
    }
    
    const chatMessage = {
        sessionId: supportSessionId,
        userId: userInfo.id,
        userName: userInfo.name,
        userEmail: userInfo.email,
        senderType: 'USER',
        message: message,
        messageType: 'CHAT'
    };
    
    console.log('Sending message:', chatMessage);
    
    try {
        supportStompClient.send('/app/support.sendMessage', {}, JSON.stringify(chatMessage));
        console.log('Message sent successfully');
        messageInput.value = '';
        messageInput.focus();
    } catch (error) {
        console.error('Error sending message:', error);
        alert('Không thể gửi tin nhắn. Vui lòng thử lại.');
    }
}

function sendTypingIndicatorCustomer() {
    if (!supportStompClient || !supportSessionId || !userInfo) return;
    
    const typingMessage = {
        sessionId: supportSessionId,
        userId: userInfo.id,
        userName: userInfo.name,
        senderType: 'USER',
        messageType: 'TYPING'
    };
    
    supportStompClient.send('/app/support.typing', {}, JSON.stringify(typingMessage));
}

function showTypingIndicatorCustomer() {
    const indicator = document.getElementById('typingIndicatorCustomer');
    indicator.style.display = 'block';
    
    setTimeout(() => {
        indicator.style.display = 'none';
    }, 3000);
}

function markSupportAsRead() {
    if (!supportSessionId) return;
    
    fetch(`/api/support/session/${supportSessionId}/read?senderType=USER`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${localStorage.getItem('authToken')}`,
            'Content-Type': 'application/json'
        }
    })
    .catch(error => {
        console.error('Error marking as read:', error);
    });
}

function scrollSupportToBottom() {
    const supportMessages = document.getElementById('supportMessages');
    supportMessages.scrollTop = supportMessages.scrollHeight;
}

function escapeHtmlCustomer(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function showAlert(message, type = 'info') {
    // You can implement a toast notification here
    alert(message);
}
