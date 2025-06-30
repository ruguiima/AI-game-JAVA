// 等待DOM完全加载后再执行JavaScript
document.addEventListener('DOMContentLoaded', function() {
    // 获取DOM元素
    const historyPanel = document.getElementById('historyPanel');
    const toggleHistoryBtn = document.getElementById('toggleHistory');
    const expandHistoryBtn = document.getElementById('expandHistoryBtn');
    const chatContainer = document.getElementById('chatContainer');
    const messageInput = document.getElementById('messageInput');
    const chatForm = document.getElementById('chatForm');
    const emptyMessage = document.querySelector('.empty-chat-message');
    
    // 全局变量
    let currentAiMessage = null;  // 当前正在生成的AI消息元素
    let currentSessionId = document.querySelector('input[name="sessionId"]')?.value || null;
    let isGenerating = false;     // 是否正在生成AI回复

    // 历史记录面板状态
    let isHistoryCollapsed = false;

    // 切换历史记录面板
    function toggleHistory() {
        if (!historyPanel || !expandHistoryBtn) return;
        isHistoryCollapsed = !isHistoryCollapsed;
        historyPanel.classList.toggle('collapsed', isHistoryCollapsed);
        expandHistoryBtn.style.display = isHistoryCollapsed ? 'flex' : 'none';
    }

    // 页面加载时初始化按钮状态
    function initializeButtonState() {
        if (!historyPanel || !expandHistoryBtn) return;
        // 检查侧边栏是否处于折叠状态
        isHistoryCollapsed = historyPanel.classList.contains('collapsed');
        // 设置展开按钮的显示状态
        expandHistoryBtn.style.display = isHistoryCollapsed ? 'flex' : 'none';
    }

    // 调整文本框高度
    function adjustTextareaHeight() {
        if (!messageInput) return;
        messageInput.style.height = 'auto';
        messageInput.style.height = messageInput.scrollHeight + 'px';
    }

    // 滚动到底部
    function scrollToBottom() {
        if (!chatContainer) return;
        chatContainer.scrollTop = chatContainer.scrollHeight;
    }
    
    // 创建用户消息元素
    function createUserMessage(content) {
        // 移除空消息提示
        if (emptyMessage) {
            emptyMessage.remove();
        }
        
        // 移除输入容器的空状态
        const inputContainer = document.querySelector('.input-container');
        if (inputContainer && inputContainer.classList.contains('empty')) {
            inputContainer.classList.remove('empty');
        }
        
        const messageDiv = document.createElement('div');
        messageDiv.className = 'message user-message';
        
        messageDiv.innerHTML = `
            <div class="message-content">
                <div class="message-bubble">${content}</div>
                <div class="message-time">${getCurrentTime()}</div>
            </div>
            <div class="message-avatar user-avatar">我</div>
        `;
        
        chatContainer.appendChild(messageDiv);
        scrollToBottom();
        return messageDiv;
    }
    
    // 创建AI消息元素
    function createAiMessage(content = '') {
        // 移除空消息提示（如果仍然存在）
        const existingEmptyMessage = document.querySelector('.empty-chat-message');
        if (existingEmptyMessage) {
            existingEmptyMessage.remove();
        }
        
        // 移除输入容器的空状态
        const inputContainer = document.querySelector('.input-container');
        if (inputContainer && inputContainer.classList.contains('empty')) {
            inputContainer.classList.remove('empty');
        }
        
        const messageDiv = document.createElement('div');
        messageDiv.className = 'message ai-message';
        
        messageDiv.innerHTML = `
            <div class="message-avatar ai-avatar">AI</div>
            <div class="message-content">
                <div class="message-bubble">${content}</div>
                <div class="message-time">${getCurrentTime()}</div>
            </div>
        `;
        
        chatContainer.appendChild(messageDiv);
        scrollToBottom();
        return messageDiv;
    }
    
    // 获取当前时间
    function getCurrentTime() {
        const now = new Date();
        const hours = String(now.getHours()).padStart(2, '0');
        const minutes = String(now.getMinutes()).padStart(2, '0');
        const seconds = String(now.getSeconds()).padStart(2, '0');
        return `${hours}:${minutes}:${seconds}`;
    }
    
    // 更新AI消息内容
    function updateAiMessage(messageElement, content) {
        if (!messageElement) return;
        const messageBubble = messageElement.querySelector('.message-bubble');
        if (messageBubble) {
            // 使用普通文本而非HTML以避免XSS风险
            messageBubble.textContent = content;
            
            // 添加生成中的状态
            if (content.endsWith('...')) {
                messageElement.classList.add('generating');
            } else {
                messageElement.classList.remove('generating');
            }
            
            scrollToBottom();
        }
    }
    
    // 发送流式消息请求
    function sendStreamMessage(message) {
        if (isGenerating) return; // 防止多次请求
        isGenerating = true;
        
        // 立即显示用户消息
        createUserMessage(message);
        
        // 创建AI消息占位符
        currentAiMessage = createAiMessage('...');
        
        // 准备请求数据
        const requestData = {
            sessionId: currentSessionId,
            message: message
        };
        
        // 发送请求 - 使用EventSource处理SSE
        fetch('/api/chat/stream', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'text/event-stream'
            },
            body: JSON.stringify(requestData)
        }).then(response => {
            if (!response.ok) {
                throw new Error('网络错误: ' + response.status);
            }
            
            // 检查是否是SSE响应
            if (!response.headers.get('Content-Type').includes('text/event-stream')) {
                // 如果响应不是SSE，我们使用标准处理方式
                console.log('非SSE响应，使用常规读取方式');
                return response.json().then(data => {
                    isGenerating = false;
                    if (data.error) {
                        updateAiMessage(currentAiMessage, '错误: ' + data.error);
                    } else {
                        updateAiMessage(currentAiMessage, data.message || JSON.stringify(data));
                    }
                });
            }
            
            // 处理SSE流
            const reader = response.body.getReader();
            const decoder = new TextDecoder('utf-8');
            let buffer = '';
            let fullResponse = '';
            
            // 处理流式响应
            function processStream() {
                return reader.read().then(({ done, value }) => {
                    if (done) {
                        console.log('流式响应结束');
                        isGenerating = false;
                        return;
                    }
                    
                    // 解码数据
                    buffer += decoder.decode(value, { stream: true });
                    console.log('收到数据块:', buffer);
                    
                    // 处理收到的事件
                    const lines = buffer.split('\n');
                    let currentEvent = '';
                    let eventName = 'message';
                    let eventId = '';
                    let eventData = '';
                    
                    for (let i = 0; i < lines.length; i++) {
                        const line = lines[i];
                        
                        // 空行表示事件边界
                        if (line.trim() === '') {
                            if (eventData) {
                                console.log(`处理${eventName}事件:`, eventData);
                                try {
                                    const data = JSON.parse(eventData);
                                    
                                    if (data.error) {
                                        console.error('API错误:', data.error);
                                        updateAiMessage(currentAiMessage, '生成回复时出错: ' + data.error);
                                        isGenerating = false;
                                        return;
                                    }
                                    
                                    // 处理会话ID
                                    if (data.sessionId && !currentSessionId) {
                                        currentSessionId = data.sessionId;
                                        // 更新隐藏的sessionId输入框
                                        const sessionIdInput = document.querySelector('input[name="sessionId"]');
                                        if (sessionIdInput) {
                                            sessionIdInput.value = currentSessionId;
                                        } else {
                                            // 如果不存在，则创建
                                            const input = document.createElement('input');
                                            input.type = 'hidden';
                                            input.name = 'sessionId';
                                            input.value = currentSessionId;
                                            chatForm.appendChild(input);
                                        }
                                    }
                                    
                                    // 如果有token，更新显示
                                    if (data.token !== undefined) {
                                        fullResponse += data.token;
                                        updateAiMessage(currentAiMessage, fullResponse);
                                    }
                                    
                                    // 处理结束信号
                                    if (data.done) {
                                        console.log('收到完成标识');
                                        isGenerating = false;
                                        
                                        if (data.fullText) {
                                            updateAiMessage(currentAiMessage, data.fullText);
                                        }
                                    }
                                } catch (e) {
                                    console.error('解析事件数据失败:', e, eventData);
                                }
                                
                                // 重置事件数据
                                eventName = 'message';
                                eventId = '';
                                eventData = '';
                            }
                            continue;
                        }
                        
                        // 解析事件字段
                        if (line.startsWith('event:')) {
                            eventName = line.substring(6).trim();
                        } else if (line.startsWith('id:')) {
                            eventId = line.substring(3).trim();
                        } else if (line.startsWith('data:')) {
                            eventData = line.substring(5).trim();
                        } else if (line === 'data') {
                            // 有时数据字段可能是单独一行
                            eventData = '';
                        }
                    }
                    
                    // 保留不完整的事件内容
                    buffer = eventData ? 'data:' + eventData + '\n' : '';
                    
                    // 继续读取流
                    return processStream();
                }).catch(err => {
                    console.error('流式读取错误:', err);
                    isGenerating = false;
                    updateAiMessage(currentAiMessage, fullResponse + '\n\n[读取回复时出错]');
                });
            }
            
            return processStream();
        }).catch(error => {
            console.error('请求错误:', error);
            isGenerating = false;
            updateAiMessage(currentAiMessage, '发送请求时出错，请重试');
        });
        
        // 清空输入框
        messageInput.value = '';
        adjustTextareaHeight();
    }

    // 绑定历史面板事件
    if (toggleHistoryBtn) {
        toggleHistoryBtn.addEventListener('click', toggleHistory);
    }
    if (expandHistoryBtn) {
        expandHistoryBtn.addEventListener('click', toggleHistory);
    }

    // 绑定输入框事件
    if (messageInput) {
        // 输入时自动调整高度
        messageInput.addEventListener('input', adjustTextareaHeight);
        
        // 回车发送消息
        messageInput.addEventListener('keydown', function (e) {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                const message = messageInput.value.trim();
                if (message && !isGenerating) {
                    sendStreamMessage(message);
                }
            }
        });
        
        // 更新发送按钮状态
        messageInput.addEventListener('input', function() {
            const sendButton = document.getElementById('sendButton');
            if (sendButton) {
                if (messageInput.value.trim() && !isGenerating) {
                    sendButton.classList.remove('disabled');
                    sendButton.disabled = false;
                } else {
                    sendButton.classList.add('disabled');
                    sendButton.disabled = isGenerating;
                }
            }
        });
    }

    // 表单提交处理 - 改为使用流式API
    if (chatForm && messageInput) {
        chatForm.addEventListener('submit', function (e) {
            e.preventDefault(); // 阻止传统提交
            
            // 防止空消息提交
            const message = messageInput.value.trim();
            if (!message || isGenerating) {
                return;
            }
            
            sendStreamMessage(message);
        });
    }

    // 初始化
    adjustTextareaHeight();  // 初始调整文本框高度
    scrollToBottom();        // 初始滚动到底部
    initializeButtonState(); // 初始化按钮状态
    
    // 检查聊天窗口是否为空，如果不为空，则移除输入框的empty类
    if (!emptyMessage && chatContainer.children.length > 0) {
        const inputContainer = document.querySelector('.input-container');
        if (inputContainer && inputContainer.classList.contains('empty')) {
            inputContainer.classList.remove('empty');
        }
    }
});