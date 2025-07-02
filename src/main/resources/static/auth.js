// auth.js - 处理用户认证相关的前端逻辑
document.addEventListener('DOMContentLoaded', function() {
    
    // 获取注册表单
    const registrationForm = document.querySelector('.signup form');
    const loginForm = document.querySelector('.login form');
    
    // 清空表单和提示信息的公共函数
    const clearFormAndMessages = (form) => {
        if (form) {
            form.reset();
            const messageContainer = form.querySelector('.message-container');
            if (messageContainer) {
                messageContainer.innerHTML = '';
            }
        }
    };
    
    // 表单切换监听器
    document.querySelector('.signupbtn').addEventListener('click', function() {
        clearFormAndMessages(loginForm);
    });
    
    document.querySelector('.loginbtn').addEventListener('click', function() {
        clearFormAndMessages(registrationForm);
    });
    
    // 如果找到注册表单，拦截其提交事件
    if (registrationForm) {
        registrationForm.addEventListener('submit', function(e) {
            e.preventDefault(); // 阻止表单默认提交行为
            
            // 获取表单数据
            const formData = {
                username: registrationForm.querySelector('input[name="username"]').value,
                email: registrationForm.querySelector('input[name="email"]').value,
                password: registrationForm.querySelector('input[name="password"]').value
            };
            
            // 显示正在处理提示
            const messageContainer = registrationForm.querySelector('.message-container');
            messageContainer.innerHTML = '<div class="info-message" style="color: #3273dc; font-size: 0.85rem; text-align: center; padding: 5px;">正在处理...</div>';
            
            // 发送AJAX请求
            fetch('/api/register', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(formData)
            })
            .then(response => response.json())
            .then(data => {
                // 清空处理提示
                messageContainer.innerHTML = '';
                
                if (data.success) {
                    // 注册成功
                    const successDiv = document.createElement('div');
                    successDiv.className = 'success-message';
                    successDiv.style.cssText = 'color: #23d160; font-size: 0.85rem; text-align: center; padding: 5px;';
                    successDiv.textContent = "注册成功，请登录！";
                    messageContainer.appendChild(successDiv);
                    
                    // 清空表单
                    registrationForm.reset();
                    
                    // 可选：3秒后自动切换到登录表单
                    setTimeout(() => {
                        document.querySelector('.loginbtn').click();
                    }, 3000);
                } else {
                    // 注册失败
                    const errorDiv = document.createElement('div');
                    errorDiv.className = 'error-message';
                    errorDiv.style.cssText = 'color: #ff3860; font-size: 0.85rem; text-align: center; padding: 5px;';
                    errorDiv.textContent = data.message || '注册失败，请重试';
                    messageContainer.appendChild(errorDiv);
                }
            })
            .catch(error => {
                console.error('注册请求出错:', error);
                
                // 显示错误信息
                messageContainer.innerHTML = '';
                const errorDiv = document.createElement('div');
                errorDiv.className = 'error-message';
                errorDiv.style.cssText = 'color: #ff3860; font-size: 0.85rem; text-align: center; padding: 5px;';
                errorDiv.textContent = '网络错误，请稍后重试';
                messageContainer.appendChild(errorDiv);
            });
        });
    }
});
