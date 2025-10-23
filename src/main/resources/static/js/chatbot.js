(function(){
    // Chatbot widget with Gemini AI integration - friendly customer care for children's toy store
    const rootId = 'chatbot-root';
    const root = document.getElementById(rootId);
    if (!root) return;

    // Templates
    const buttonHtml = `
        <div class="chatbot-button" id="chatbot-toggle" aria-label="Mở chat hỗ trợ" role="button" tabindex="0">
            <svg width="30" height="30" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M12 2C7 2 3.5 5.2 3.5 9.3C3.5 11.6 4.6 13.7 6.5 15.1V20L10.7 17.9C11.7 18 12.8 18 14 18C19 18 22.5 14.8 22.5 10.7C22.5 6.6 19 2 12 2Z" fill="white"/>
            </svg>
        </div>
    `;

    const bubbleHtml = `
        <div class="chatbot-bubble" id="chatbot-bubble" role="dialog" aria-label="Chat hỗ trợ khách hàng" aria-hidden="true">
            <div class="chatbot-header">
                <div class="avatar">🤖</div>
                <div>
                    <div class="title">T4M AI Trợ lý</div>
                    <div class="subtitle">Powered by Gemini AI ✨</div>
                </div>
            </div>
            <div class="chatbot-messages" id="chatbot-messages" aria-live="polite"></div>
            <div class="suggestions-toggle collapsed" id="suggestions-toggle">
                <span>Gợi ý nhanh</span>
                <span class="arrow">▼</span>
            </div>
            <div class="chatbot-suggestions collapsed" id="chatbot-suggestions"></div>
            <div class="chatbot-input">
                <input type="text" id="chatbot-input" placeholder="Gõ câu hỏi của bạn..." aria-label="Nhập câu hỏi">
                <button class="send" id="chatbot-send">Gửi</button>
            </div>
        </div>
    `;

    root.innerHTML = buttonHtml + bubbleHtml;

    // Elements
    const toggle = document.getElementById('chatbot-toggle');
    const bubble = document.getElementById('chatbot-bubble');
    const messagesEl = document.getElementById('chatbot-messages');
    const suggestionsEl = document.getElementById('chatbot-suggestions');
    const suggestionsToggle = document.getElementById('suggestions-toggle');
    const inputEl = document.getElementById('chatbot-input');
    const sendBtn = document.getElementById('chatbot-send');

    // Friendly suggestions (suitable for children's toy store)
    const suggestions = [
        'Tư vấn quà cho bé 5 tuổi',
        'Đồ chơi nào đang sale?',
        'Tìm đồ chơi theo độ tuổi',
        'Chính sách đổi trả',
        'Thời gian giao hàng'
    ];

    // State
    let open = false;
    let conversationId = null;
    let isProcessing = false;
    let suggestionsExpanded = false; // Track suggestion panel state

    function openBubble() {
        bubble.setAttribute('aria-hidden', 'false');
        bubble.style.display = 'flex';
        open = true;
        inputEl.focus();
        renderSuggestions();
        // initial greeting
        if (!messagesEl.hasChildNodes()) {
            pushAgentMessage('Chào bạn! Mình là T4M AI Trợ lý. Mình có thể giúp bạn tìm đồ chơi phù hợp, tư vấn quà tặng, giải đáp về chính sách. Bạn muốn hỏi gì?');
        }
    }

    function closeBubble() {
        bubble.setAttribute('aria-hidden', 'true');
        bubble.style.display = 'none';
        open = false;
    }

    // Toggle suggestions
    function toggleSuggestions() {
        suggestionsExpanded = !suggestionsExpanded;
        if (suggestionsExpanded) {
            suggestionsEl.classList.remove('collapsed');
            suggestionsEl.classList.add('expanded');
            suggestionsToggle.classList.remove('collapsed');
        } else {
            suggestionsEl.classList.remove('expanded');
            suggestionsEl.classList.add('collapsed');
            suggestionsToggle.classList.add('collapsed');
        }
    }

    // Toggle chatbot bubble
    toggle.addEventListener('click', () => {
        open ? closeBubble() : openBubble();
    });
    toggle.addEventListener('keypress', (e) => { 
        if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            open ? closeBubble() : openBubble(); 
        }
    });

    // Toggle suggestions panel
    suggestionsToggle.addEventListener('click', toggleSuggestions);

    // Show/hide bubble initially closed
    closeBubble();

    // render suggestions
    function renderSuggestions() {
        suggestionsEl.innerHTML = '';
        suggestions.forEach(s => {
            const btn = document.createElement('button');
            btn.className = 'suggestion';
            btn.textContent = s;
            btn.addEventListener('click', () => {
                handleUserMessage(s);
            });
            suggestionsEl.appendChild(btn);
        });
    }

    // push user message
    function pushUserMessage(text) {
        const msg = document.createElement('div');
        msg.className = 'msg user';
        msg.textContent = text;
        messagesEl.appendChild(msg);
        messagesEl.scrollTop = messagesEl.scrollHeight;
    }

    // push agent message with typing indicator (preserve line breaks)
    function pushAgentMessage(text, immediate = false) {
        const typing = document.createElement('div');
        typing.className = 'msg agent';
        typing.textContent = '...';
        messagesEl.appendChild(typing);
        messagesEl.scrollTop = messagesEl.scrollHeight;

        const delay = immediate ? 100 : (700 + Math.min(1200, text.length * 15));
        setTimeout(() => {
            // Use textContent to preserve natural line breaks (with CSS white-space: pre-wrap)
            typing.textContent = text;
            messagesEl.scrollTop = messagesEl.scrollHeight;
        }, delay);
    }

    // Call backend API to get AI response
    async function getAIResponse(userMessage) {
        try {
            const response = await fetch('/api/chatbot/message', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    message: userMessage,
                    conversationId: conversationId
                })
            });

            if (!response.ok) {
                throw new Error('Network response was not ok');
            }

            const data = await response.json();
            
            if (data.success) {
                // Update conversation ID for context
                conversationId = data.conversationId;
                return data.reply;
            } else {
                throw new Error(data.error || 'Unknown error');
            }
        } catch (error) {
            console.error('Error calling chatbot API:', error);
            // Fallback error message
            return 'Xin lỗi bạn, mình đang gặp chút trục trặc kỹ thuật. Bạn có thể thử lại sau hoặc gọi hotline 1800-363-363 để được hỗ trợ trực tiếp nhé! 😊';
        }
    }

    // handle user input
    async function handleUserMessage(text) {
        if (!text || !text.trim()) return;
        if (isProcessing) return; // Prevent multiple simultaneous requests
        
        const userMessage = text.trim();
        pushUserMessage(userMessage);
        inputEl.value = '';
        
        // Disable input while processing
        isProcessing = true;
        inputEl.disabled = true;
        sendBtn.disabled = true;
        
        // Show typing indicator
        const typingIndicator = document.createElement('div');
        typingIndicator.className = 'msg agent';
        typingIndicator.id = 'typing-indicator';
        typingIndicator.textContent = '🤖 Đang suy nghĩ...';
        messagesEl.appendChild(typingIndicator);
        messagesEl.scrollTop = messagesEl.scrollHeight;
        
        try {
            // Get AI response from backend
            const aiReply = await getAIResponse(userMessage);
            
            // Remove typing indicator
            const indicator = document.getElementById('typing-indicator');
            if (indicator) {
                indicator.remove();
            }
            
            // Show AI response
            pushAgentMessage(aiReply, true);
            
        } catch (error) {
            console.error('Error handling message:', error);
            const indicator = document.getElementById('typing-indicator');
            if (indicator) {
                indicator.remove();
            }
            pushAgentMessage('Xin lỗi bạn, mình gặp lỗi khi xử lý câu hỏi. Vui lòng thử lại! 😊', true);
        } finally {
            // Re-enable input
            isProcessing = false;
            inputEl.disabled = false;
            sendBtn.disabled = false;
            inputEl.focus();
        }
    }

    sendBtn.addEventListener('click', () => handleUserMessage(inputEl.value));
    inputEl.addEventListener('keypress', (e) => { 
        if (e.key === 'Enter' && !isProcessing) {
            e.preventDefault();
            handleUserMessage(inputEl.value);
        }
    });

    // Accessibility
    toggle.addEventListener('focus', () => toggle.classList.add('focus'));
    toggle.addEventListener('blur', () => toggle.classList.remove('focus'));

    // expose for debugging
    window._t4m_chatbot = { openBubble, closeBubble, conversationId };
})();
