# Nginx 部署配置指南

## 一、部署步骤

### 1. 本地构建前端
```bash
cd D:/workspace/工作/config_compare/web_compare
npm run build
```

### 2. 打包dist目录
```bash
tar -czf dist.tar.gz dist/
```

### 3. 上传到服务器
```bash
scp dist.tar.gz user@server:/tmp/
```

### 4. 服务器上部署
```bash
# 登录服务器
ssh user@server

# 备份旧版本（如果有）
sudo mv /usr/share/nginx/html/config_compare/dist \
        /usr/share/nginx/html/config_compare/dist.bak_$(date +%Y%m%d)

# 创建目录
sudo mkdir -p /usr/share/nginx/html/config_compare

# 解压新版本
cd /usr/share/nginx/html/config_compare
sudo tar -xzf /tmp/dist.tar.gz

# 设置权限
sudo chown -R nginx:nginx /usr/share/nginx/html/config_compare
sudo chmod -R 755 /usr/share/nginx/html/config_compare
```

### 5. 配置Nginx

#### 方案A：创建独立配置文件（推荐）
```bash
# 创建配置文件
sudo nano /etc/nginx/conf.d/config_compare.conf
```

复制以下内容（根据实际情况修改）：
```nginx
server {
    listen 80;
    server_name your-domain.com;  # 改成你的域名或IP
    
    root /usr/share/nginx/html/config_compare/dist;
    index index.html;
    
    # 前端路由
    location / {
        try_files $uri $uri/ /index.html;
        add_header Cache-Control "no-cache";
    }
    
    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2)$ {
        expires 7d;
        add_header Cache-Control "public";
    }
    
    # API代理到后端
    location /api/ {
        proxy_pass http://localhost:8080/api/;  # 改成你的后端地址
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
    
    access_log /var/log/nginx/config_compare_access.log;
    error_log /var/log/nginx/config_compare_error.log;
}
```

#### 方案B：修改现有配置
```bash
# 编辑默认配置
sudo nano /etc/nginx/conf.d/default.conf
# 或
sudo nano /etc/nginx/nginx.conf
```

在现有的 `server` 块中添加：
```nginx
    location /config_compare/ {
        alias /usr/share/nginx/html/config_compare/dist/;
        index index.html;
        try_files $uri $uri/ /config_compare/index.html;
    }
    
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
```

### 6. 测试并重载配置
```bash
# 测试配置语法
sudo nginx -t

# 如果测试通过，重载配置
sudo nginx -s reload
# 或
sudo systemctl reload nginx
```

### 7. 验证部署
```bash
# 检查nginx状态
sudo systemctl status nginx

# 查看日志
sudo tail -f /var/log/nginx/config_compare_access.log
sudo tail -f /var/log/nginx/config_compare_error.log
```

浏览器访问：
- 如果是独立配置：`http://your-server-ip/`
- 如果是子路径配置：`http://your-server-ip/config_compare/`

## 二、常见问题排查

### 问题1：访问页面显示404
**原因**：nginx找不到文件或配置路径错误

**解决**：
```bash
# 检查文件是否存在
ls -lh /usr/share/nginx/html/config_compare/dist/index.html

# 检查权限
sudo chmod -R 755 /usr/share/nginx/html/config_compare
sudo chown -R nginx:nginx /usr/share/nginx/html/config_compare

# 查看nginx错误日志
sudo tail -f /var/log/nginx/error.log
```

### 问题2：刷新页面后404（Vue Router问题）
**原因**：缺少 `try_files $uri $uri/ /index.html;`

**解决**：
确保nginx配置中有：
```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```

### 问题3：API请求失败或CORS错误
**原因**：后端服务未启动或代理配置错误

**解决**：
```bash
# 检查后端服务
curl http://localhost:8080/api/systems

# 如果不通，检查后端服务状态
ps aux | grep java
# 或
sudo systemctl status config_compare_backend

# 确保nginx配置中有正确的proxy_pass
location /api/ {
    proxy_pass http://localhost:8080/api/;
}
```

### 问题4：样式或JS加载失败
**原因**：路径问题或缓存

**解决**：
```bash
# 清除浏览器缓存（Ctrl+F5）

# 检查构建配置
# 确保 web_compare/vite.config.js 中的 base 设置正确：
base: '/',  # 或 '/config_compare/' 取决于部署路径

# 重新构建
cd web_compare
npm run build
```

### 问题5：更新后看不到新版本
**原因**：浏览器缓存或nginx缓存

**解决**：
```bash
# 方法1：强制刷新（Ctrl+F5）

# 方法2：清除nginx缓存（如果有配置）
sudo rm -rf /var/cache/nginx/*
sudo nginx -s reload

# 方法3：修改nginx配置，禁用缓存（调试用）
add_header Cache-Control "no-cache, no-store, must-revalidate";
```

## 三、快速部署脚本

创建 `deploy_nginx.sh`：
```bash
#!/bin/bash
set -e

SERVER="user@server"
BACKEND_PORT="8080"  # 后端端口

echo "📦 本地构建..."
cd web_compare
npm run build

echo "📤 上传到服务器..."
tar -czf dist.tar.gz dist/
scp dist.tar.gz $SERVER:/tmp/

echo "🚀 服务器部署..."
ssh $SERVER << 'EOF'
    # 备份
    sudo mv /usr/share/nginx/html/config_compare/dist \
            /usr/share/nginx/html/config_compare/dist.bak_$(date +%Y%m%d_%H%M%S) 2>/dev/null || true
    
    # 部署
    sudo mkdir -p /usr/share/nginx/html/config_compare
    cd /usr/share/nginx/html/config_compare
    sudo tar -xzf /tmp/dist.tar.gz
    sudo chown -R nginx:nginx .
    sudo chmod -R 755 .
    
    # 重载nginx
    sudo nginx -t && sudo nginx -s reload
    
    echo "✅ 部署完成！"
EOF

echo "✅ 全部完成！"
```

使用：
```bash
chmod +x deploy_nginx.sh
./deploy_nginx.sh
```

## 四、完整nginx配置示例

```nginx
# /etc/nginx/conf.d/config_compare.conf

server {
    listen 80;
    server_name config-compare.yourdomain.com;  # 改成你的域名
    
    # 静态文件目录
    root /usr/share/nginx/html/config_compare/dist;
    index index.html;
    
    # 日志
    access_log /var/log/nginx/config_compare_access.log;
    error_log /var/log/nginx/config_compare_error.log;
    
    # Gzip压缩
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
    
    # 前端路由（Vue Router history模式）
    location / {
        try_files $uri $uri/ /index.html;
        
        # 开发阶段禁用缓存
        add_header Cache-Control "no-cache, no-store, must-revalidate";
        add_header Pragma "no-cache";
        add_header Expires "0";
    }
    
    # 静态资源设置长缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
    
    # API代理到后端
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        # 禁用代理缓冲（实时数据）
        proxy_buffering off;
    }
    
    # 安全头
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-XSS-Protection "1; mode=block" always;
}
```

## 五、检查清单

部署前：
- [ ] 本地 `npm run build` 成功
- [ ] dist 目录包含 index.html 和 assets 文件夹
- [ ] 后端服务正常运行

部署后：
- [ ] nginx -t 测试通过
- [ ] 浏览器能访问首页
- [ ] F12 控制台无报错
- [ ] API请求能正常响应
- [ ] 页面刷新不会404
- [ ] 样式和图片正常显示

