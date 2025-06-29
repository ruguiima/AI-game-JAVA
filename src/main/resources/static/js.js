// 等待DOM完全加载后再执行JavaScript
document.addEventListener('DOMContentLoaded', function() {
    // 获取DOM元素
    const historyPanel = document.getElementById('historyPanel');
    const toggleHistoryBtn = document.getElementById('toggleHistory');
    const expandHistoryBtn = document.getElementById('expandHistoryBtn');
    const chatContainer = document.getElementById('chatContainer');
    const messageInput = document.getElementById('messageInput');
    const chatForm = document.getElementById('chatForm'); // 获取表单元素

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
                if (messageInput.value.trim() && chatForm) {
                    chatForm.submit(); // 提交表单
                    messageInput.value = ''; // 清空输入框
                }
            }
        });
    }

    // 表单提交处理
    if (chatForm && messageInput) {
        chatForm.addEventListener('submit', function (e) {
            // 防止空消息提交
            if (!messageInput.value.trim()) {
                e.preventDefault();
                return;
            }
            
            // 提交后清空输入框
            setTimeout(function() {
                messageInput.value = '';
                adjustTextareaHeight();
            }, 0);
        });
    }

    // 初始化
    adjustTextareaHeight();  // 初始调整文本框高度
    scrollToBottom();        // 初始滚动到底部
    initializeButtonState(); // 初始化按钮状态
});