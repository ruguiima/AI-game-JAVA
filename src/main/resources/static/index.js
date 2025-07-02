// 等待DOM完全加载后再执行JavaScript
document.addEventListener('DOMContentLoaded', function() {
    // ===== 常量定义 =====
    const CONSTANTS = {
        FILE_SIZE_LIMIT: 5 * 1024 * 1024, // 5MB
        NOTIFICATION_AUTO_CLOSE_TIME: 3000,
        DEFAULT_LOADING_TEXT: '处理中...',
        API_ENDPOINTS: {
            CHAT_STREAM: '/api/chat/stream',
            USER_PROFILE: '/api/user/profile',
            USER_AVATAR: '/api/user/avatar',
            MODEL_SETTINGS: '/api/model-settings'
        },
        SELECTORS: {
            HISTORY_PANEL: '#historyPanel',
            TOGGLE_HISTORY_BTN: '#toggleHistory',
            EXPAND_HISTORY_BTN: '#expandHistoryBtn',
            CHAT_CONTAINER: '#chatContainer',
            MESSAGE_INPUT: '#messageInput',
            CHAT_FORM: '#chatForm',
            EMPTY_MESSAGE: '.empty-chat-message',
            INPUT_CONTAINER: '.input-container'
        },
        CLASSES: {
            COLLAPSED: 'collapsed',
            EMPTY: 'empty',
            GENERATING: 'generating',
            SHOW: 'show',
            ACTIVE: 'active'
        },
        DEFAULT_MODEL_SETTINGS: {
            model: 'deepseek-chat',
            maxTokens: 500,
            temperature: 0.2,
            responseLength: 'short',
            creativity: 'precise'
        }
    };

    // ===== DOM元素缓存 =====
    const DOM = {
        historyPanel: document.getElementById('historyPanel'),
        toggleHistoryBtn: document.getElementById('toggleHistory'),
        expandHistoryBtn: document.getElementById('expandHistoryBtn'),
        chatContainer: document.getElementById('chatContainer'),
        messageInput: document.getElementById('messageInput'),
        chatForm: document.getElementById('chatForm'),
        emptyMessage: document.querySelector('.empty-chat-message')
    };
    
    // ===== 应用状态管理 =====
    const AppState = {
        currentAiMessage: null,
        currentSessionId: document.querySelector('input[name="sessionId"]')?.value || null,
        isGenerating: false,
        isHistoryCollapsed: false,
        currentModelSettings: { ...CONSTANTS.DEFAULT_MODEL_SETTINGS }
    };

    // ===== 工具函数 =====
    const Utils = {
        // 时间格式化
        getCurrentTime() {
            const now = new Date();
            const hours = String(now.getHours()).padStart(2, '0');
            const minutes = String(now.getMinutes()).padStart(2, '0');
            const seconds = String(now.getSeconds()).padStart(2, '0');
            return `${hours}:${minutes}:${seconds}`;
        },

        getFullTimeString() {
            const now = new Date();
            return now.getFullYear() + '-' + 
                   String(now.getMonth() + 1).padStart(2, '0') + '-' + 
                   String(now.getDate()).padStart(2, '0') + ' ' + 
                   String(now.getHours()).padStart(2, '0') + ':' + 
                   String(now.getMinutes()).padStart(2, '0');
        },

        // DOM操作工具
        removeEmptyState() {
            const emptyMessage = document.querySelector(CONSTANTS.SELECTORS.EMPTY_MESSAGE);
            if (emptyMessage) {
                emptyMessage.remove();
            }
            
            const inputContainer = document.querySelector(CONSTANTS.SELECTORS.INPUT_CONTAINER);
            if (inputContainer && inputContainer.classList.contains(CONSTANTS.CLASSES.EMPTY)) {
                inputContainer.classList.remove(CONSTANTS.CLASSES.EMPTY);
            }
        },

        // 按钮状态管理
        setButtonLoading(button, loadingText = CONSTANTS.DEFAULT_LOADING_TEXT) {
            if (!button) return null;
            const originalText = button.textContent;
            button.textContent = loadingText;
            button.disabled = true;
            return originalText;
        },

        resetButtonState(button, originalText) {
            if (!button || !originalText) return;
            button.textContent = originalText;
            button.disabled = false;
        },

        // 表单设置更新
        updateFormSettings(settings) {
            const modelSelect = document.getElementById('modelSelect');
            if (modelSelect && settings.model) {
                modelSelect.value = settings.model;
            }
            
            const responseLengthRadios = document.querySelectorAll('input[name="responseLength"]');
            responseLengthRadios.forEach(radio => {
                radio.checked = radio.value === settings.responseLength;
            });
            
            const creativityRadios = document.querySelectorAll('input[name="creativity"]');
            creativityRadios.forEach(radio => {
                radio.checked = radio.value === settings.creativity;
            });
        }
    };

    // ===== UI控制器 =====
    const UIController = {
        // 历史面板控制
        toggleHistory() {
            if (!DOM.historyPanel || !DOM.expandHistoryBtn) return;
            AppState.isHistoryCollapsed = !AppState.isHistoryCollapsed;
            DOM.historyPanel.classList.toggle(CONSTANTS.CLASSES.COLLAPSED, AppState.isHistoryCollapsed);
            DOM.expandHistoryBtn.style.display = AppState.isHistoryCollapsed ? 'flex' : 'none';
        },

        initializeButtonState() {
            if (!DOM.historyPanel || !DOM.expandHistoryBtn) return;
            AppState.isHistoryCollapsed = DOM.historyPanel.classList.contains(CONSTANTS.CLASSES.COLLAPSED);
            DOM.expandHistoryBtn.style.display = AppState.isHistoryCollapsed ? 'flex' : 'none';
        },

        adjustTextareaHeight() {
            if (!DOM.messageInput) return;
            DOM.messageInput.style.height = 'auto';
            DOM.messageInput.style.height = DOM.messageInput.scrollHeight + 'px';
        },

        scrollToBottom() {
            if (!DOM.chatContainer) return;
            DOM.chatContainer.scrollTop = DOM.chatContainer.scrollHeight;
        },

        // 消息创建
        createUserMessage(content) {
            Utils.removeEmptyState();
            
            const messageDiv = document.createElement('div');
            messageDiv.className = 'message user-message';
            
            messageDiv.innerHTML = `
                <div class="message-content">
                    <div class="message-bubble"></div>
                    <div class="message-time">${Utils.getCurrentTime()}</div>
                </div>
                <div class="message-avatar user-avatar">我</div>
            `;
            
            const messageBubble = messageDiv.querySelector('.message-bubble');
            messageBubble.textContent = content;
            
            DOM.chatContainer.appendChild(messageDiv);
            this.scrollToBottom();
            return messageDiv;
        },

        createAiMessage(content = '') {
            Utils.removeEmptyState();
            
            const messageDiv = document.createElement('div');
            messageDiv.className = 'message ai-message';
            
            // 如果没有内容，显示加载状态
            if (!content) {
                messageDiv.classList.add('loading');
            }
            
            messageDiv.innerHTML = `
                <div class="message-avatar ai-avatar">AI</div>
                <div class="message-content">
                    <div class="message-bubble"></div>
                    <div class="typing-indicator">
                        <div class="typing-dot"></div>
                        <div class="typing-dot"></div>
                        <div class="typing-dot"></div>
                    </div>
                    <div class="message-time">${Utils.getCurrentTime()}</div>
                </div>
            `;
            
            const messageBubble = messageDiv.querySelector('.message-bubble');
            messageBubble.textContent = content;
            
            DOM.chatContainer.appendChild(messageDiv);
            this.scrollToBottom();
            return messageDiv;
        },

        updateAiMessage(messageElement, content) {
            if (!messageElement) return;
            const messageBubble = messageElement.querySelector('.message-bubble');
            if (messageBubble) {
                // 移除加载状态
                messageElement.classList.remove('loading');
                
                messageBubble.textContent = content;
                
                if (content.endsWith('...')) {
                    messageElement.classList.add(CONSTANTS.CLASSES.GENERATING);
                } else {
                    messageElement.classList.remove(CONSTANTS.CLASSES.GENERATING);
                }
                
                this.scrollToBottom();
            }
        },

        updateHistoryPanel(sessionId, sessionName) {
            const historyList = document.getElementById('historyList');
            if (!historyList || !sessionId) return;
            
            const existingItem = historyList.querySelector(`[data-session-id="${sessionId}"]`);
            if (existingItem) {
                const allItems = historyList.querySelectorAll('.history-item');
                allItems.forEach(item => item.classList.remove(CONSTANTS.CLASSES.ACTIVE));
                existingItem.classList.add(CONSTANTS.CLASSES.ACTIVE);
                return;
            }
            
            const historyItem = document.createElement('div');
            historyItem.className = `history-item ${CONSTANTS.CLASSES.ACTIVE}`;
            historyItem.setAttribute('data-session-id', sessionId);
            historyItem.style.cursor = 'pointer';
            historyItem.onclick = function() {
                window.location.href = '/session/' + sessionId;
            };
            
            const timeString = Utils.getFullTimeString();
            
            historyItem.innerHTML = `
                <div class="history-question"></div>
                <div class="history-time">${timeString}</div>
            `;
            
            const questionDiv = historyItem.querySelector('.history-question');
            questionDiv.textContent = sessionName;
            
            const allItems = historyList.querySelectorAll('.history-item');
            allItems.forEach(item => item.classList.remove(CONSTANTS.CLASSES.ACTIVE));
            
            historyList.insertBefore(historyItem, historyList.firstChild);
        }
    };
    
    // ===== API通信模块 =====
    const APIService = {
        // 发送流式消息
        async sendStreamMessage(message) {
            if (AppState.isGenerating) return;
            AppState.isGenerating = true;
            
            UIController.createUserMessage(message);
            AppState.currentAiMessage = UIController.createAiMessage(); // 移除初始的'...'以显示加载动画
            
            const requestData = {
                sessionId: AppState.currentSessionId,
                message: message,
                modelSettings: AppState.currentModelSettings
            };
            
            try {
                const response = await fetch(CONSTANTS.API_ENDPOINTS.CHAT_STREAM, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'text/event-stream'
                    },
                    body: JSON.stringify(requestData)
                });

                if (!response.ok) {
                    throw new Error('网络错误: ' + response.status);
                }

                if (!response.headers.get('Content-Type').includes('text/event-stream')) {
                    console.log('非SSE响应，使用常规读取方式');
                    const data = await response.json();
                    AppState.isGenerating = false;
                    if (data.error) {
                        UIController.updateAiMessage(AppState.currentAiMessage, '错误: ' + data.error);
                    } else {
                        UIController.updateAiMessage(AppState.currentAiMessage, data.message || JSON.stringify(data));
                    }
                    return;
                }

                await this.processSSEStream(response);
            } catch (error) {
                console.error('请求错误:', error);
                AppState.isGenerating = false;
                UIController.updateAiMessage(AppState.currentAiMessage, '发送请求时出错，请重试');
            }
            
            DOM.messageInput.value = '';
            UIController.adjustTextareaHeight();
        },

        // 处理SSE流
        async processSSEStream(response) {
            const reader = response.body.getReader();
            const decoder = new TextDecoder('utf-8');
            let buffer = '';
            let fullResponse = '';
            
            const processStream = async () => {
                try {
                    const { done, value } = await reader.read();
                    
                    if (done) {
                        console.log('流式响应结束');
                        AppState.isGenerating = false;
                        return;
                    }
                    
                    buffer += decoder.decode(value, { stream: true });
                    console.log('收到数据块:', buffer);
                    
                    const lines = buffer.split('\n');
                    let eventName = 'message';
                    let eventId = '';
                    let eventData = '';
                    
                    for (let i = 0; i < lines.length; i++) {
                        const line = lines[i];
                        
                        if (line.trim() === '') {
                            if (eventData) {
                                console.log(`处理${eventName}事件:`, eventData);
                                try {
                                    const data = JSON.parse(eventData);
                                    
                                    if (data.error) {
                                        console.error('API错误:', data.error);
                                        UIController.updateAiMessage(AppState.currentAiMessage, '生成回复时出错: ' + data.error);
                                        AppState.isGenerating = false;
                                        return;
                                    }
                                    
                                    if (data.sessionId && !AppState.currentSessionId) {
                                        AppState.currentSessionId = data.sessionId;
                                        this.updateSessionId(data.sessionId);
                                        
                                        if (data.isNewSession && data.sessionName) {
                                            UIController.updateHistoryPanel(AppState.currentSessionId, data.sessionName);
                                        }
                                    }
                                    
                                    if (data.token !== undefined) {
                                        if (data.token !== null && data.token !== "null" && data.token.trim() !== "") {
                                            fullResponse += data.token;
                                            UIController.updateAiMessage(AppState.currentAiMessage, fullResponse);
                                        }
                                    }
                                    
                                    if (data.done) {
                                        console.log('收到完成标识');
                                        AppState.isGenerating = false;
                                        
                                        if (data.fullText) {
                                            UIController.updateAiMessage(AppState.currentAiMessage, data.fullText);
                                        }
                                    }
                                } catch (e) {
                                    console.error('解析事件数据失败:', e, eventData);
                                }
                                
                                eventName = 'message';
                                eventId = '';
                                eventData = '';
                            }
                            continue;
                        }
                        
                        if (line.startsWith('event:')) {
                            eventName = line.substring(6).trim();
                        } else if (line.startsWith('id:')) {
                            eventId = line.substring(3).trim();
                        } else if (line.startsWith('data:')) {
                            eventData = line.substring(5).trim();
                        } else if (line === 'data') {
                            eventData = '';
                        }
                    }
                    
                    buffer = eventData ? 'data:' + eventData + '\n' : '';
                    return processStream();
                } catch (err) {
                    console.error('流式读取错误:', err);
                    AppState.isGenerating = false;
                    UIController.updateAiMessage(AppState.currentAiMessage, fullResponse + '\n\n[读取回复时出错]');
                }
            };
            
            return processStream();
        },

        // 更新会话ID
        updateSessionId(sessionId) {
            const sessionIdInput = document.querySelector('input[name="sessionId"]');
            if (sessionIdInput) {
                sessionIdInput.value = sessionId;
            } else {
                const input = document.createElement('input');
                input.type = 'hidden';
                input.name = 'sessionId';
                input.value = sessionId;
                DOM.chatForm.appendChild(input);
            }
        },

        // 通用API调用
        async apiCall(url, options = {}) {
            const defaultOptions = {
                headers: { 'Content-Type': 'application/json' },
                ...options
            };
            
            try {
                const response = await fetch(url, defaultOptions);
                return await response.json();
            } catch (error) {
                console.error('API调用失败:', error);
                throw error;
            }
        }
    };
    // ===== 事件绑定和初始化 =====
    const EventHandlers = {
        // 绑定历史面板事件
        bindHistoryEvents() {
            if (DOM.toggleHistoryBtn) {
                DOM.toggleHistoryBtn.addEventListener('click', UIController.toggleHistory);
            }
            if (DOM.expandHistoryBtn) {
                DOM.expandHistoryBtn.addEventListener('click', UIController.toggleHistory);
            }
        },

        // 绑定输入框事件
        bindInputEvents() {
            if (!DOM.messageInput) return;

            DOM.messageInput.addEventListener('input', UIController.adjustTextareaHeight);
            
            DOM.messageInput.addEventListener('keydown', (e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    const message = DOM.messageInput.value.trim();
                    if (message && !AppState.isGenerating) {
                        APIService.sendStreamMessage(message);
                    }
                }
            });
            
            DOM.messageInput.addEventListener('input', () => {
                const sendButton = document.getElementById('sendButton');
                if (sendButton) {
                    const hasContent = DOM.messageInput.value.trim() && !AppState.isGenerating;
                    sendButton.disabled = !hasContent;
                }
            });
        },

        // 绑定表单提交事件
        bindFormEvents() {
            if (DOM.chatForm && DOM.messageInput) {
                DOM.chatForm.addEventListener('submit', (e) => {
                    e.preventDefault();
                    
                    const message = DOM.messageInput.value.trim();
                    if (!message || AppState.isGenerating) {
                        return;
                    }
                    
                    APIService.sendStreamMessage(message);
                });
            }
        },

        // 初始化所有事件
        init() {
            this.bindHistoryEvents();
            this.bindInputEvents();
            this.bindFormEvents();
        }
    };

    // ===== 应用初始化 =====
    const App = {
        init() {
            // 初始化UI状态
            UIController.adjustTextareaHeight();
            UIController.scrollToBottom();
            UIController.initializeButtonState();
            
            // 检查聊天窗口状态
            if (!DOM.emptyMessage && DOM.chatContainer.children.length > 0) {
                const inputContainer = document.querySelector(CONSTANTS.SELECTORS.INPUT_CONTAINER);
                if (inputContainer && inputContainer.classList.contains(CONSTANTS.CLASSES.EMPTY)) {
                    inputContainer.classList.remove(CONSTANTS.CLASSES.EMPTY);
                }
            }
            
            // 绑定事件
            EventHandlers.init();
            
            // 初始化用户相关功能
            UserProfileManager.init();
            
            // 初始化模型设置
            ModelSettingsManager.init();
            
            console.log('应用初始化完成');
        }
    };

    
    // ===== 通知系统 =====
    const NotificationManager = {
        show(message, type = 'info') {
            const notification = document.createElement('div');
            notification.className = `notification notification-${type}`;
            notification.innerHTML = `
                <span class="notification-message">${message}</span>
                <button class="notification-close">&times;</button>
            `;
            
            document.body.appendChild(notification);
            setTimeout(() => notification.classList.add(CONSTANTS.CLASSES.SHOW), 100);
            
            const autoClose = setTimeout(() => this.remove(notification), CONSTANTS.NOTIFICATION_AUTO_CLOSE_TIME);
            
            notification.querySelector('.notification-close').addEventListener('click', () => {
                clearTimeout(autoClose);
                this.remove(notification);
            });
        },

        remove(notificationEl) {
            notificationEl.classList.remove(CONSTANTS.CLASSES.SHOW);
            setTimeout(() => {
                if (notificationEl.parentNode) {
                    notificationEl.parentNode.removeChild(notificationEl);
                }
            }, 300);
        }
    };

    // ===== 用户资料管理 =====
    const UserProfileManager = {
        elements: {
            userAvatar: document.getElementById('userAvatar'),
            profileDropdown: document.getElementById('profileDropdown'),
            avatarUpload: document.getElementById('avatarUpload'),
            uploadAvatarBtn: document.getElementById('uploadAvatarBtn'),
            logoutBtn: document.querySelector('.logout-btn'),
            saveBtn: document.querySelector('.save-btn')
        },

        async loadProfile() {
            try {
                const data = await APIService.apiCall(CONSTANTS.API_ENDPOINTS.USER_PROFILE);
                if (data.username) {
                    this.updateUI(data);
                }
            } catch (error) {
                console.error('获取用户信息失败:', error);
            }
        },

        updateUI(profile) {
            const fields = [
                { id: 'nickname', value: profile.nickname },
                { id: 'gender', value: profile.gender },
                { id: 'birthday', value: profile.birthday }
            ];

            fields.forEach(field => {
                const element = document.getElementById(field.id);
                if (element && field.value) {
                    element.value = field.value;
                }
            });

            const displays = [
                { id: 'usernameDisplay', value: profile.username },
                { id: 'emailDisplay', value: profile.email }
            ];

            displays.forEach(display => {
                const element = document.getElementById(display.id);
                if (element && display.value) {
                    element.textContent = display.value;
                }
            });

            if (profile.avatarUrl) {
                this.updateAvatarDisplay(profile.avatarUrl);
            }
        },

        updateAvatarDisplay(avatarUrl) {
            const elements = {
                avatarImg: document.querySelector('.avatar-img'),
                avatarImgLarge: document.querySelector('.avatar-img-large'),
                avatarText: document.querySelector('.avatar-text'),
                avatarTextLarge: document.querySelector('.avatar-text-large')
            };

            if (elements.avatarImg && elements.avatarImgLarge) {
                elements.avatarImg.src = avatarUrl;
                elements.avatarImg.style.display = 'block';
                elements.avatarImgLarge.src = avatarUrl;
                elements.avatarImgLarge.style.display = 'block';

                if (elements.avatarText) elements.avatarText.style.display = 'none';
                if (elements.avatarTextLarge) elements.avatarTextLarge.style.display = 'none';
            }
        },

        showProfilePanel() {
            if (this.elements.profileDropdown) {
                this.elements.profileDropdown.classList.add(CONSTANTS.CLASSES.SHOW);
            }
        },

        hideProfilePanel() {
            if (this.elements.profileDropdown) {
                this.elements.profileDropdown.classList.remove(CONSTANTS.CLASSES.SHOW);
            }
        },

        async uploadAvatar(file) {
            if (file.size > CONSTANTS.FILE_SIZE_LIMIT) {
                NotificationManager.show('文件大小不能超过5MB', 'error');
                return;
            }

            const formData = new FormData();
            formData.append('avatar', file);

            NotificationManager.show('正在上传头像...', 'info');

            try {
                const response = await fetch(CONSTANTS.API_ENDPOINTS.USER_AVATAR, {
                    method: 'POST',
                    body: formData
                });
                const data = await response.json();

                if (data.success) {
                    NotificationManager.show('头像上传成功', 'success');
                    if (data.avatarUrl) {
                        this.updateAvatarDisplay(data.avatarUrl);
                    }
                } else {
                    throw new Error(data.message || '上传失败');
                }
            } catch (error) {
                console.error('头像上传失败:', error);
                NotificationManager.show('头像上传失败: ' + error.message, 'error');
            }
        },

        async saveProfile() {
            const nickname = document.getElementById('nickname')?.value.trim();
            const gender = document.getElementById('gender')?.value;
            const birthday = document.getElementById('birthday')?.value;

            if (!nickname) {
                NotificationManager.show('请输入昵称', 'error');
                return;
            }

            const originalText = Utils.setButtonLoading(this.elements.saveBtn);

            try {
                const data = await APIService.apiCall(CONSTANTS.API_ENDPOINTS.USER_PROFILE, {
                    method: 'PUT',
                    body: JSON.stringify({ nickname, gender, birthday })
                });

                if (data.success) {
                    NotificationManager.show('设置保存成功', 'success');
                    if (data.profile) {
                        this.updateUI(data.profile);
                    }
                } else {
                    throw new Error(data.message || '保存失败');
                }
            } catch (error) {
                console.error('保存用户信息失败:', error);
                NotificationManager.show('保存失败: ' + error.message, 'error');
            } finally {
                Utils.resetButtonState(this.elements.saveBtn, originalText);
            }
        },

        bindEvents() {
            const { userAvatar, profileDropdown, avatarUpload, uploadAvatarBtn, logoutBtn, saveBtn } = this.elements;

            if (userAvatar && profileDropdown) {
                userAvatar.addEventListener('click', (e) => {
                    e.stopPropagation();
                    const isVisible = profileDropdown.classList.contains(CONSTANTS.CLASSES.SHOW);
                    isVisible ? this.hideProfilePanel() : this.showProfilePanel();
                });

                document.addEventListener('click', (e) => {
                    if (!userAvatar.contains(e.target) && !profileDropdown.contains(e.target)) {
                        this.hideProfilePanel();
                    }
                });

                profileDropdown.addEventListener('click', (e) => e.stopPropagation());

                document.addEventListener('keydown', (e) => {
                    if (e.key === 'Escape' && profileDropdown.classList.contains(CONSTANTS.CLASSES.SHOW)) {
                        this.hideProfilePanel();
                    }
                });
            }

            if (avatarUpload) {
                avatarUpload.addEventListener('change', (e) => {
                    const file = e.target.files[0];
                    if (file && file.type.startsWith('image/')) {
                        this.uploadAvatar(file);
                    } else {
                        NotificationManager.show('请选择有效的图片文件', 'error');
                    }
                });
            }

            if (uploadAvatarBtn) {
                uploadAvatarBtn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    avatarUpload?.click();
                });
            }

            if (logoutBtn) {
                logoutBtn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    if (confirm('确定要退出登录吗？')) {
                        window.location.href = '/logout';
                    }
                });
            }

            if (saveBtn) {
                saveBtn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    this.saveProfile();
                });
            }
        },

        init() {
            this.loadProfile();
            this.bindEvents();
        }
    };

    // ===== 模型设置管理 =====
    const ModelSettingsManager = {
        elements: {
            moreSettingsBtn: document.getElementById('moreSettingsBtn'),
            modelSettingsPanel: document.getElementById('modelSettingsPanel'),
            closeSettingsBtn: document.getElementById('closeSettingsBtn'),
            applySettingsBtn: document.getElementById('applySettingsBtn'),
            resetSettingsBtn: document.getElementById('resetSettingsBtn'),
            settingsPreview: document.getElementById('settingsPreview')
        },

        async loadUserModelSettings() {
            try {
                const response = await fetch(CONSTANTS.API_ENDPOINTS.MODEL_SETTINGS);
                if (response.ok) {
                    const settings = await response.json();
                    AppState.currentModelSettings = settings;
                    this.updateSettingsUI(settings);
                    this.updateSettingsPreview();
                    console.log('加载的用户设置:', settings);
                } else {
                    console.log('加载用户设置失败，使用默认设置');
                    this.loadDefaultSettings();
                }
            } catch (error) {
                console.error('加载用户设置出错:', error);
                this.loadDefaultSettings();
            }
        },

        updateSettingsUI(settings) {
            Utils.updateFormSettings(settings);
        },

        loadDefaultSettings() {
            AppState.currentModelSettings = { ...CONSTANTS.DEFAULT_MODEL_SETTINGS };
            this.updateSettingsUI(CONSTANTS.DEFAULT_MODEL_SETTINGS);
            this.updateSettingsPreview();
        },

        showModelSettings() {
            console.log('显示模型设置面板');
            this.elements.modelSettingsPanel?.classList.add(CONSTANTS.CLASSES.SHOW);
            this.updateSettingsPreview();
        },

        hideModelSettings() {
            console.log('隐藏模型设置面板');
            this.elements.modelSettingsPanel?.classList.remove(CONSTANTS.CLASSES.SHOW);
        },

        updateSettingsPreview() {
            const modelSelect = document.getElementById('modelSelect');
            const responseLengthRadios = document.querySelectorAll('input[name="responseLength"]');
            const creativityRadios = document.querySelectorAll('input[name="creativity"]');

            const modelText = modelSelect ? modelSelect.options[modelSelect.selectedIndex].text : 'DeepSeek Chat';

            let lengthText = '简短回复';
            responseLengthRadios.forEach(radio => {
                if (radio.checked) {
                    switch(radio.value) {
                        case 'short': lengthText = '简短回复'; break;
                        case 'medium': lengthText = '中等回复'; break;
                        case 'long': lengthText = '复杂回复'; break;
                    }
                }
            });

            let creativityText = '规则模式';
            creativityRadios.forEach(radio => {
                if (radio.checked) {
                    switch(radio.value) {
                        case 'precise': creativityText = '规则模式'; break;
                        case 'balanced': creativityText = '平衡模式'; break;
                        case 'creative': creativityText = '创造模式'; break;
                    }
                }
            });

            if (this.elements.settingsPreview) {
                this.elements.settingsPreview.textContent = `${modelText} | ${lengthText} | ${creativityText}`;
            }
        },

        async applySettings() {
            const originalText = Utils.setButtonLoading(this.elements.applySettingsBtn);

            try {
                const modelSelect = document.getElementById('modelSelect');
                const responseLengthRadios = document.querySelectorAll('input[name="responseLength"]');
                const creativityRadios = document.querySelectorAll('input[name="creativity"]');

                let responseLength = 'short';
                responseLengthRadios.forEach(radio => {
                    if (radio.checked) responseLength = radio.value;
                });

                let creativity = 'precise';
                creativityRadios.forEach(radio => {
                    if (radio.checked) creativity = radio.value;
                });

                const settingsData = {
                    model: modelSelect ? modelSelect.value : 'deepseek-chat',
                    responseLength: responseLength,
                    creativity: creativity
                };

                const result = await APIService.apiCall(CONSTANTS.API_ENDPOINTS.MODEL_SETTINGS, {
                    method: 'POST',
                    body: JSON.stringify(settingsData)
                });

                if (result.success) {
                    AppState.currentModelSettings = result.settings;
                    localStorage.setItem('modelSettings', JSON.stringify(AppState.currentModelSettings));
                    NotificationManager.show('模型设置已保存', 'success');
                    this.hideModelSettings();
                    console.log('应用的模型设置:', AppState.currentModelSettings);
                } else {
                    NotificationManager.show(result.error || '保存失败', 'error');
                }
            } catch (error) {
                console.error('保存设置失败:', error);
                NotificationManager.show('保存失败，请重试', 'error');
            } finally {
                Utils.resetButtonState(this.elements.applySettingsBtn, originalText);
            }
        },

        resetSettings() {
            const modelSelect = document.getElementById('modelSelect');
            const responseLengthRadios = document.querySelectorAll('input[name="responseLength"]');
            const creativityRadios = document.querySelectorAll('input[name="creativity"]');

            if (modelSelect) {
                modelSelect.value = 'deepseek-chat';
            }

            responseLengthRadios.forEach(radio => {
                radio.checked = radio.value === 'short';
            });

            creativityRadios.forEach(radio => {
                radio.checked = radio.value === 'precise';
            });

            AppState.currentModelSettings = { ...CONSTANTS.DEFAULT_MODEL_SETTINGS };
            localStorage.removeItem('modelSettings');
            this.updateSettingsPreview();
            NotificationManager.show('设置已重置为默认值', 'info');
        },

        loadSavedSettings() {
            const savedSettings = localStorage.getItem('modelSettings');
            if (savedSettings) {
                try {
                    const settings = JSON.parse(savedSettings);
                    AppState.currentModelSettings = { ...AppState.currentModelSettings, ...settings };
                    Utils.updateFormSettings(settings);
                    this.updateSettingsPreview();
                } catch (e) {
                    console.error('加载保存的设置失败:', e);
                }
            }
        },

        bindEvents() {
            const { moreSettingsBtn, modelSettingsPanel, closeSettingsBtn, applySettingsBtn, resetSettingsBtn } = this.elements;

            if (moreSettingsBtn && modelSettingsPanel) {
                moreSettingsBtn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    const isVisible = modelSettingsPanel.classList.contains(CONSTANTS.CLASSES.SHOW);
                    isVisible ? this.hideModelSettings() : this.showModelSettings();
                });

                if (closeSettingsBtn) {
                    closeSettingsBtn.addEventListener('click', () => this.hideModelSettings());
                }

                document.addEventListener('click', (e) => {
                    if (!modelSettingsPanel.contains(e.target) && !moreSettingsBtn.contains(e.target)) {
                        this.hideModelSettings();
                    }
                });

                modelSettingsPanel.addEventListener('click', (e) => e.stopPropagation());

                document.addEventListener('keydown', (e) => {
                    if (e.key === 'Escape' && modelSettingsPanel.classList.contains(CONSTANTS.CLASSES.SHOW)) {
                        this.hideModelSettings();
                    }
                });
            }

            if (applySettingsBtn) {
                applySettingsBtn.addEventListener('click', () => this.applySettings());
            }

            if (resetSettingsBtn) {
                resetSettingsBtn.addEventListener('click', () => this.resetSettings());
            }

            // 监听设置变化
            document.addEventListener('change', (e) => {
                if (e.target.matches('#modelSelect, input[name="responseLength"], input[name="creativity"]')) {
                    this.updateSettingsPreview();
                }
            });
        },

        init() {
            this.loadSavedSettings();
            this.loadUserModelSettings();
            this.bindEvents();
        }
    };

    // 启动应用
    App.init();
});