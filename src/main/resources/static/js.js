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
                                        
                                        // 如果是新会话，更新历史侧边栏
                                        if (data.isNewSession && data.sessionName) {
                                            updateHistoryPanel(currentSessionId, data.sessionName);
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

    // 更新历史侧边栏
    function updateHistoryPanel(sessionId, sessionName) {
        const historyList = document.getElementById('historyList');
        if (!historyList || !sessionId) return;
        
        // 检查是否已经存在该会话
        const existingItem = historyList.querySelector(`[data-session-id="${sessionId}"]`);
        if (existingItem) {
            // 如果已经存在，更新活动状态
            const allItems = historyList.querySelectorAll('.history-item');
            allItems.forEach(item => item.classList.remove('active'));
            existingItem.classList.add('active');
            return;
        }
        
        // 创建新的历史记录项
        const historyItem = document.createElement('div');
        historyItem.className = 'history-item active';
        historyItem.setAttribute('data-session-id', sessionId);
        historyItem.style.cursor = 'pointer';
        historyItem.onclick = function() {
            window.location.href = '/session/' + sessionId;
        };
        
        // 获取当前时间
        const now = new Date();
        const timeString = now.getFullYear() + '-' + 
                          String(now.getMonth() + 1).padStart(2, '0') + '-' + 
                          String(now.getDate()).padStart(2, '0') + ' ' + 
                          String(now.getHours()).padStart(2, '0') + ':' + 
                          String(now.getMinutes()).padStart(2, '0');
        
        historyItem.innerHTML = `
            <div class="history-question">${sessionName}</div>
            <div class="history-time">${timeString}</div>
        `;
        
        // 移除其他项的活动状态
        const allItems = historyList.querySelectorAll('.history-item');
        allItems.forEach(item => item.classList.remove('active'));
        
        // 将新项添加到列表顶部
        historyList.insertBefore(historyItem, historyList.firstChild);
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
    
    // 用户头像和个人信息面板控制
    const userAvatar = document.getElementById('userAvatar');
    const profileDropdown = document.getElementById('profileDropdown');
    
    // 页面加载时获取用户信息
    loadUserProfile();
    
    if (userAvatar && profileDropdown) {
        // 点击头像显示/隐藏面板
        userAvatar.addEventListener('click', function(e) {
            e.stopPropagation();
            const isVisible = profileDropdown.classList.contains('show');
            
            if (isVisible) {
                hideProfilePanel();
            } else {
                showProfilePanel();
            }
        });
        
        // 显示个人信息面板
        function showProfilePanel() {
            profileDropdown.classList.add('show');
        }
        
        // 隐藏个人信息面板
        function hideProfilePanel() {
            profileDropdown.classList.remove('show');
        }
        
        // 点击文档其他地方关闭面板
        document.addEventListener('click', function(e) {
            // 检查点击的元素是否在用户头像或面板内部
            if (!userAvatar.contains(e.target) && !profileDropdown.contains(e.target)) {
                hideProfilePanel();
            }
        });
        
        // 点击面板内部不关闭（防止事件冒泡）
        profileDropdown.addEventListener('click', function(e) {
            e.stopPropagation();
        });
        
        // ESC键关闭面板
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' && profileDropdown.classList.contains('show')) {
                hideProfilePanel();
            }
        });
    }
    
    // 通知系统
    function showNotification(message, type = 'info') {
        // 创建通知元素
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.innerHTML = `
            <span class="notification-message">${message}</span>
            <button class="notification-close">&times;</button>
        `;
        
        // 添加到页面
        document.body.appendChild(notification);
        
        // 显示动画
        setTimeout(() => notification.classList.add('show'), 100);
        
        // 自动关闭
        const autoClose = setTimeout(() => removeNotification(notification), 3000);
        
        // 手动关闭
        notification.querySelector('.notification-close').addEventListener('click', () => {
            clearTimeout(autoClose);
            removeNotification(notification);
        });
        
        function removeNotification(notificationEl) {
            notificationEl.classList.remove('show');
            setTimeout(() => {
                if (notificationEl.parentNode) {
                    notificationEl.parentNode.removeChild(notificationEl);
                }
            }, 300);
        }
    }
    
    // 加载用户信息
    function loadUserProfile() {
        fetch('/api/user/profile')
            .then(response => response.json())
            .then(data => {
                if (data.username) {
                    updateUIWithProfile(data);
                }
            })
            .catch(error => {
                console.error('获取用户信息失败:', error);
            });
    }
    
    // 更新界面用户信息
    function updateUIWithProfile(profile) {
        // 更新昵称
        const nicknameInput = document.getElementById('nickname');
        if (nicknameInput && profile.nickname) {
            nicknameInput.value = profile.nickname;
        }
        
        // 更新性别
        const genderSelect = document.getElementById('gender');
        if (genderSelect && profile.gender) {
            genderSelect.value = profile.gender;
        }
        
        // 更新生日
        const birthdayInput = document.getElementById('birthday');
        if (birthdayInput && profile.birthday) {
            birthdayInput.value = profile.birthday;
        }
        
        // 更新用户名和邮箱（只读）
        const usernameSpan = document.getElementById('usernameDisplay');
        if (usernameSpan && profile.username) {
            usernameSpan.textContent = profile.username;
        }
        
        const emailSpan = document.getElementById('emailDisplay');
        if (emailSpan && profile.email) {
            emailSpan.textContent = profile.email;
        }
        
        // 更新头像
        if (profile.avatarUrl) {
            updateAvatarDisplay(profile.avatarUrl);
        }
    }
    
    // 更新头像显示
    function updateAvatarDisplay(avatarUrl) {
        const avatarImg = document.querySelector('.avatar-img');
        const avatarImgLarge = document.querySelector('.avatar-img-large');
        const avatarText = document.querySelector('.avatar-text');
        const avatarTextLarge = document.querySelector('.avatar-text-large');
        
        if (avatarImg && avatarImgLarge) {
            avatarImg.src = avatarUrl;
            avatarImg.style.display = 'block';
            avatarImgLarge.src = avatarUrl;
            avatarImgLarge.style.display = 'block';
            
            // 隐藏文字头像
            if (avatarText) avatarText.style.display = 'none';
            if (avatarTextLarge) avatarTextLarge.style.display = 'none';
        }
    }
    
    // 头像上传处理
    const avatarUpload = document.getElementById('avatarUpload');
    if (avatarUpload) {
        avatarUpload.addEventListener('change', function(e) {
            const file = e.target.files[0];
            if (file && file.type.startsWith('image/')) {
                // 验证文件大小（5MB限制）
                if (file.size > 5 * 1024 * 1024) {
                    showNotification('文件大小不能超过5MB', 'error');
                    return;
                }
                
                // 上传头像
                uploadAvatar(file);
            } else {
                showNotification('请选择有效的图片文件', 'error');
            }
        });
    }
    
    // 上传头像到服务器
    function uploadAvatar(file) {
        const formData = new FormData();
        formData.append('avatar', file);
        
        // 显示上传中状态
        showNotification('正在上传头像...', 'info');
        
        fetch('/api/user/avatar', {
            method: 'POST',
            body: formData
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showNotification('头像上传成功', 'success');
                // 更新头像显示
                if (data.avatarUrl) {
                    updateAvatarDisplay(data.avatarUrl);
                }
            } else {
                throw new Error(data.message || '上传失败');
            }
        })
        .catch(error => {
            console.error('头像上传失败:', error);
            showNotification('头像上传失败: ' + error.message, 'error');
        });
    }
    
    // 头像上传按钮处理
    const uploadAvatarBtn = document.getElementById('uploadAvatarBtn');
    if (uploadAvatarBtn) {
        uploadAvatarBtn.addEventListener('click', function(e) {
            e.stopPropagation(); // 防止事件冒泡导致面板关闭
            document.getElementById('avatarUpload').click();
        });
    }
    
    // 退出登录处理
    const logoutBtn = document.querySelector('.logout-btn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', function(e) {
            e.stopPropagation(); // 防止事件冒泡导致面板关闭
            if (confirm('确定要退出登录吗？')) {
                // 可以添加退出登录的API调用
                window.location.href = '/logout';
            }
        });
    }
    
    // 保存设置按钮处理
    const saveBtn = document.querySelector('.save-btn');
    if (saveBtn) {
        saveBtn.addEventListener('click', function(e) {
            e.stopPropagation(); // 防止事件冒泡导致面板关闭
            
            // 获取表单数据
            const nickname = document.getElementById('nickname').value.trim();
            const gender = document.getElementById('gender').value;
            const birthday = document.getElementById('birthday').value;
            
            // 验证必填字段
            if (!nickname) {
                showNotification('请输入昵称', 'error');
                return;
            }
            
            // 显示保存中状态
            const originalText = saveBtn.textContent;
            saveBtn.textContent = '保存中...';
            saveBtn.disabled = true;
            
            // 调用后端API
            fetch('/api/user/profile', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    nickname: nickname,
                    gender: gender,
                    birthday: birthday
                })
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    showNotification('设置保存成功', 'success');
                    // 更新界面上的用户信息
                    if (data.profile) {
                        updateUIWithProfile(data.profile);
                    }
                } else {
                    throw new Error(data.message || '保存失败');
                }
            })
            .catch(error => {
                console.error('保存用户信息失败:', error);
                showNotification('保存失败: ' + error.message, 'error');
            })
            .finally(() => {
                // 恢复按钮状态
                saveBtn.textContent = originalText;
                saveBtn.disabled = false;
            });
        });
    }
});