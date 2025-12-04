#!/bin/bash

###############################################################################
# 报告颜色优化 - 服务器部署脚本
# 用途：将修改的前端文件部署到服务器
###############################################################################

# ===== 配置区域（请根据实际情况修改）=====
SERVER_USER="your_username"          # SSH用户名
SERVER_HOST="192.168.1.100"          # 服务器IP或域名
SERVER_PORT="22"                      # SSH端口
PROJECT_PATH="/opt/config_compare"   # 服务器上项目路径

# ===== 脚本开始 =====
set -e  # 遇到错误立即退出

echo "========================================"
echo "  报告颜色优化 - 部署到服务器"
echo "========================================"

# 检查本地文件是否存在
LOCAL_FILE="web_compare/src/views/report/compare-report/index.vue"
if [ ! -f "$LOCAL_FILE" ]; then
    echo "❌ 错误：找不到本地文件 $LOCAL_FILE"
    exit 1
fi

echo ""
echo "📋 部署信息："
echo "  服务器: $SERVER_USER@$SERVER_HOST:$SERVER_PORT"
echo "  项目路径: $PROJECT_PATH"
echo "  更新文件: $LOCAL_FILE"
echo ""

read -p "确认以上信息是否正确？(y/n): " confirm
if [ "$confirm" != "y" ]; then
    echo "❌ 已取消部署"
    exit 0
fi

echo ""
echo "🚀 开始部署..."

# 1. 上传文件到服务器临时目录
echo ""
echo "📤 [1/5] 上传文件到服务器..."
scp -P $SERVER_PORT "$LOCAL_FILE" "$SERVER_USER@$SERVER_HOST:/tmp/index.vue.new"

# 2. 在服务器上执行部署操作
echo ""
echo "🔧 [2/5] 备份原文件..."
ssh -p $SERVER_PORT "$SERVER_USER@$SERVER_HOST" << EOF
    set -e
    cd $PROJECT_PATH
    
    # 备份原文件
    if [ -f "web_compare/src/views/report/compare-report/index.vue" ]; then
        cp web_compare/src/views/report/compare-report/index.vue \
           web_compare/src/views/report/compare-report/index.vue.bak_\$(date +%Y%m%d_%H%M%S)
        echo "✅ 原文件已备份"
    fi
EOF

echo ""
echo "📝 [3/5] 替换文件..."
ssh -p $SERVER_PORT "$SERVER_USER@$SERVER_HOST" << EOF
    set -e
    cd $PROJECT_PATH
    
    # 替换文件
    cp /tmp/index.vue.new web_compare/src/views/report/compare-report/index.vue
    rm /tmp/index.vue.new
    echo "✅ 文件已替换"
EOF

echo ""
echo "🔨 [4/5] 重新构建前端..."
ssh -p $SERVER_PORT "$SERVER_USER@$SERVER_HOST" << EOF
    set -e
    cd $PROJECT_PATH/web_compare
    
    echo "开始构建..."
    npm run build
    
    if [ \$? -eq 0 ]; then
        echo "✅ 构建成功"
    else
        echo "❌ 构建失败"
        exit 1
    fi
EOF

echo ""
echo "✅ [5/5] 部署完成！"

echo ""
echo "========================================"
echo "  部署成功！"
echo "========================================"
echo ""
echo "📌 验证步骤："
echo "  1. 浏览器访问: http://$SERVER_HOST/report/compare-report"
echo "  2. 清除浏览器缓存或按 Ctrl+F5 强制刷新"
echo "  3. 选择系统，点击'报告总览'"
echo "  4. 查看差异分析详情中的颜色："
echo "     - 新增项：基线值灰色，当前值绿色"
echo "     - 缺失项：基线值红色，当前值灰色"
echo "     - 修改项：基线值蓝色，当前值橙色"
echo ""
echo "🔄 如需回滚："
echo "  ssh $SERVER_USER@$SERVER_HOST"
echo "  cd $PROJECT_PATH"
echo "  ls web_compare/src/views/report/compare-report/index.vue.bak_*"
echo "  # 选择备份文件恢复"
echo ""

