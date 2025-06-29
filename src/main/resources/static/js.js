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
    isHistoryCollapsed = !isHistoryCollapsed;
    historyPanel.classList.toggle('collapsed', isHistoryCollapsed);
    expandHistoryBtn.style.display = isHistoryCollapsed ? 'flex' : 'none';
}

// 页面加载时初始化按钮状态
function initializeButtonState() {
    // 检查侧边栏是否处于折叠状态
    isHistoryCollapsed = historyPanel.classList.contains('collapsed');
    // 设置展开按钮的显示状态
    expandHistoryBtn.style.display = isHistoryCollapsed ? 'flex' : 'none';
}

// 绑定事件
toggleHistoryBtn.addEventListener('click', toggleHistory);
expandHistoryBtn.addEventListener('click', toggleHistory);

// 调整文本框高度
function adjustTextareaHeight() {
    messageInput.style.height = 'auto';
    messageInput.style.height = messageInput.scrollHeight + 'px';
}

// 滚动到底部
function scrollToBottom() {
    chatContainer.scrollTop = chatContainer.scrollHeight;
}

// 事件监听
messageInput.addEventListener('input', adjustTextareaHeight);
messageInput.addEventListener('keydown', function (e) {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        chatForm.submit(); // 提交表单
    }
});

// 初始调整文本框高度
adjustTextareaHeight();

// 初始滚动到底部
scrollToBottom();

// 初始化按钮状态
initializeButtonState();