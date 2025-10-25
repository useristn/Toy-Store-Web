// Admin Support Chat JavaScript
let stompClient = null;
let currentSessionId = null;
let currentUserName = null;
let currentUserEmail = null;

document.addEventListener('DOMContentLoaded', function() {
    console.log('Admin Support: Initializing...');
    checkAuth();
    loadSessions();
    connectWebSocket();
    
    // Setup chat form
    const chatForm = document.getElementById('chatForm');
    if (chatForm) {
        console.log('Admin Support: Chat form found');
        chatForm.addEventListener('submit', sendMessage);
    } else {
        console.error('Admin Support: Chat form not found!');
    }
    
    // Typing indicator
    const messageInput = document.getElementById('messageInput');
    if (messageInput) {
        let typingTimeout;
        messageInput.addEventListener('input', function() {
            if (currentSessionId && stompClient && stompClient.connected) {
                clearTimeout(typingTimeout);
                sendTypingIndicator();
                typingTimeout = setTimeout(() => {
                    // Stop typing indicator after 1 second
                }, 1000);
            }
        });
    }
});

function checkAuth() {
    const token = localStorage.getItem('authToken');
    console.log('Admin Support: Token exists:', !!token);
    if (!token) {
        console.error('Admin Support: No token found, redirecting to login');
        window.location.href = '/login';
        return;
    }
}

function getAuthHeaders() {
    const token = localStorage.getItem('authToken');
    return {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    };
}

function connectWebSocket() {
    const socket = new SockJS('/ws-support');
    stompClient = Stomp.over(socket);
    
    stompClient.connect({}, function(frame) {
        console.log('Connected to WebSocket:', frame);
        
        // Subscribe to admin notifications
        stompClient.subscribe('/topic/admin.notifications', function(message) {
            const notification = JSON.parse(message.body);
            console.log('New notification:', notification);
            loadSessions(); // Refresh sessions list
            playNotificationSound();
        });
    }, function(error) {
        console.error('WebSocket connection error:', error);
        setTimeout(connectWebSocket, 5000); // Reconnect after 5 seconds
    });
}

function loadSessions() {
    console.log('Admin Support: Loading sessions...');
    console.log('Admin Support: API URL:', '/api/support/admin/sessions');
    console.log('Admin Support: Token:', localStorage.getItem('authToken')?.substring(0, 20) + '...');
    
    fetch('/api/support/admin/sessions', {
        headers: getAuthHeaders()
    })
    .then(response => {
        console.log('Admin Support: Response status:', response.status);
        console.log('Admin Support: Response headers:', response.headers);
        
        if (response.status === 401) {
            throw new Error('Unauthorized - Token không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.');
        }
        
        if (response.status === 403) {
            throw new Error('Forbidden - Bạn không có quyền truy cập. Cần role ADMIN.');
        }
        
        if (!response.ok) {
            return response.text().then(text => {
                console.error('Admin Support: Response error:', text);
                throw new Error(`HTTP ${response.status}: ${text}`);
            });
        }
        return response.json();
    })
    .then(sessions => {
        console.log('Admin Support: Sessions loaded:', sessions);
        displaySessions(sessions);
    })
    .catch(error => {
        console.error('Admin Support: Error loading sessions:', error);
        
        // Check if it's an auth error
        if (error.message.includes('Unauthorized') || error.message.includes('401')) {
            document.getElementById('sessionsList').innerHTML = `
                <div class="alert alert-warning m-3">
                    <i class="fas fa-exclamation-triangle"></i> 
                    <strong>Phiên đăng nhập hết hạn</strong><br>
                    <small>Vui lòng đăng nhập lại</small><br>
                    <button class="btn btn-sm btn-primary mt-2" onclick="window.location.href='/login'">
                        Đăng nhập
                    </button>
                </div>
            `;
        } else if (error.message.includes('Forbidden') || error.message.includes('403')) {
            document.getElementById('sessionsList').innerHTML = `
                <div class="alert alert-danger m-3">
                    <i class="fas fa-ban"></i> 
                    <strong>Không có quyền truy cập</strong><br>
                    <small>Chức năng này chỉ dành cho Admin</small><br>
                    <button class="btn btn-sm btn-secondary mt-2" onclick="window.location.href='/admin'">
                        Quay lại Dashboard
                    </button>
                </div>
            `;
        } else {
            document.getElementById('sessionsList').innerHTML = `
                <div class="alert alert-danger m-3">
                    <i class="fas fa-exclamation-triangle"></i> 
                    <strong>Không thể tải danh sách</strong><br>
                    <small>${error.message}</small><br>
                    <button class="btn btn-sm btn-outline-danger mt-2" onclick="loadSessions()">
                        Thử lại
                    </button>
                </div>
            `;
        }
    });
}

function displaySessions(sessions) {
    const sessionsList = document.getElementById('sessionsList');
    
    if (sessions.length === 0) {
        sessionsList.innerHTML = `
            <div class="text-center py-5 text-muted">
                <i class="fas fa-inbox fa-3x mb-3"></i>
                <p>Chưa có khách hàng nào cần hỗ trợ</p>
            </div>
        `;
        return;
    }
    
    sessionsList.innerHTML = sessions.map(session => `
        <div class="session-item ${session.sessionId === currentSessionId ? 'active' : ''}" 
             onclick="selectSession('${session.sessionId}', '${session.userName}', '${session.userEmail}')">
            <div class="d-flex justify-content-between align-items-start">
                <div class="flex-grow-1">
                    <div class="user-name">
                        <i class="fas fa-user-circle me-2"></i>${session.userName || 'Khách hàng'}
                    </div>
                    <div class="user-email">${session.userEmail}</div>
                    <div class="last-message">${session.lastMessage || 'Chưa có tin nhắn'}</div>
                </div>
                ${session.unreadCount > 0 ? `<span class="badge bg-danger">${session.unreadCount}</span>` : ''}
            </div>
        </div>
    `).join('');
}

function selectSession(sessionId, userName, userEmail) {
    currentSessionId = sessionId;
    currentUserName = userName;
    currentUserEmail = userEmail;
    
    // Update UI
    document.getElementById('chatUserName').textContent = userName || 'Khách hàng';
    document.getElementById('chatUserEmail').textContent = userEmail;
    document.getElementById('chatInputContainer').style.display = 'block';
    
    // Update active session
    document.querySelectorAll('.session-item').forEach(item => {
        item.classList.remove('active');
    });
    event.target.closest('.session-item').classList.add('active');
    
    // Load messages
    loadMessages(sessionId);
    
    // Subscribe to session messages
    if (stompClient && stompClient.connected) {
        stompClient.subscribe('/topic/support.' + sessionId, function(message) {
            const chatMessage = JSON.parse(message.body);
            
            if (chatMessage.messageType === 'CHAT') {
                displayMessage(chatMessage);
            } else if (chatMessage.messageType === 'TYPING' && chatMessage.senderType === 'USER') {
                showTypingIndicator();
            }
        });
    }
    
    // Mark as read
    markAsRead(sessionId);
}

function loadMessages(sessionId) {
    fetch(`/api/support/session/${sessionId}/messages`, {
        headers: getAuthHeaders()
    })
    .then(response => response.json())
    .then(messages => {
        const chatMessages = document.getElementById('chatMessages');
        chatMessages.innerHTML = '';
        
        messages.forEach(msg => {
            displayMessage({
                senderType: msg.senderType,
                userName: msg.userName,
                message: msg.message,
                createdAt: msg.createdAt
            });
        });
        
        scrollToBottom();
    })
    .catch(error => {
        console.error('Error loading messages:', error);
    });
}

function displayMessage(chatMessage) {
    const chatMessages = document.getElementById('chatMessages');
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${chatMessage.senderType.toLowerCase()}`;
    
    const time = new Date(chatMessage.createdAt).toLocaleTimeString('vi-VN', {
        hour: '2-digit',
        minute: '2-digit'
    });
    
    messageDiv.innerHTML = `
        <div class="message-bubble">
            <div class="message-sender">${chatMessage.senderType === 'ADMIN' ? 'Bạn (Admin)' : chatMessage.userName}</div>
            <div class="message-text">${escapeHtml(chatMessage.message)}</div>
            <div class="message-time">${time}</div>
        </div>
    `;
    
    chatMessages.appendChild(messageDiv);
    scrollToBottom();
}

function sendMessage(e) {
    e.preventDefault();
    console.log('Admin Support: Sending message...');
    
    const messageInput = document.getElementById('messageInput');
    const message = messageInput.value.trim();
    
    console.log('Admin Support: Message:', message);
    console.log('Admin Support: Session ID:', currentSessionId);
    console.log('Admin Support: Stomp Client connected:', stompClient && stompClient.connected);
    
    if (!message) {
        console.warn('Admin Support: Message is empty');
        return;
    }
    
    if (!currentSessionId) {
        console.error('Admin Support: No session selected');
        alert('Vui lòng chọn một khách hàng để chat');
        return;
    }
    
    if (!stompClient || !stompClient.connected) {
        console.error('Admin Support: WebSocket not connected');
        alert('Mất kết nối WebSocket. Đang thử kết nối lại...');
        connectWebSocket();
        return;
    }
    
    const chatMessage = {
        sessionId: currentSessionId,
        userName: 'Admin',
        userEmail: 'admin',
        senderType: 'ADMIN',
        message: message,
        messageType: 'CHAT'
    };
    
    console.log('Admin Support: Sending message:', chatMessage);
    
    try {
        stompClient.send('/app/support.sendMessage', {}, JSON.stringify(chatMessage));
        console.log('Admin Support: Message sent successfully');
        messageInput.value = '';
        messageInput.focus();
    } catch (error) {
        console.error('Admin Support: Error sending message:', error);
        alert('Không thể gửi tin nhắn. Vui lòng thử lại.');
    }
}

function sendTypingIndicator() {
    if (!stompClient || !currentSessionId) return;
    
    const typingMessage = {
        sessionId: currentSessionId,
        senderType: 'ADMIN',
        messageType: 'TYPING'
    };
    
    stompClient.send('/app/support.typing', {}, JSON.stringify(typingMessage));
}

function showTypingIndicator() {
    const indicator = document.getElementById('typingIndicator');
    indicator.classList.add('active');
    
    setTimeout(() => {
        indicator.classList.remove('active');
    }, 3000);
}

function markAsRead(sessionId) {
    fetch(`/api/support/session/${sessionId}/read?senderType=ADMIN`, {
        method: 'POST',
        headers: getAuthHeaders()
    })
    .then(() => {
        loadSessions(); // Refresh to update unread count
    })
    .catch(error => {
        console.error('Error marking as read:', error);
    });
}

function scrollToBottom() {
    const chatMessages = document.getElementById('chatMessages');
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function playNotificationSound() {
    // Optional: Add notification sound
    const audio = new Audio('data:audio/wav;base64,UklGRnoGAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQoGAACBhYqFbF1fdJivrJBhNjVgodDbq2EcBj+a2/LDciUFLIHO8tiJNwgZaLvt559NEAxQp+PwtmMcBjiR1/LMeSwFJHfH8N2QQAoUXrTp66hVFApGn+DyvmwhBTGH0fPTgjMGHm7A7+OZSBAKQ5bf8LTMRQ4QV67m7LJaHQQ7leHwxokfCCFp');
    audio.play().catch(e => console.log('Could not play notification sound'));
}

// Refresh sessions every 30 seconds
setInterval(loadSessions, 30000);
