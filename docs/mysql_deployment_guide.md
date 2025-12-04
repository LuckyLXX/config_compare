# MySQL数据库内网部署指南

## 概述

本文档提供了UAT生产环境配置比对系统MySQL数据库的内网部署指南。

## 系统要求

### 硬件要求
- CPU: 4核心以上
- 内存: 8GB以上
- 存储: 100GB以上可用空间
- 网络: 千兆网络

### 软件要求
- 操作系统: CentOS 7+/Ubuntu 18+/Windows Server 2016+
- MySQL版本: 5.7+ 或 8.0+
- Java版本: JDK 8+

## 部署步骤

### 1. MySQL服务器安装

#### CentOS/RHEL
```bash
# 安装MySQL 8.0
sudo yum install -y https://dev.mysql.com/get/mysql80-community-release-el7-3.noarch.rpm
sudo yum install -y mysql-server

# 启动MySQL服务
sudo systemctl start mysqld
sudo systemctl enable mysqld

# 获取临时密码
sudo grep 'temporary password' /var/log/mysqld.log
```

#### Ubuntu/Debian
```bash
# 安装MySQL 8.0
sudo apt update
sudo apt install -y mysql-server

# 启动MySQL服务
sudo systemctl start mysql
sudo systemctl enable mysql
```

### 2. MySQL安全配置

```bash
# 运行安全配置脚本
sudo mysql_secure_installation
```

### 3. 数据库初始化

#### 3.1 连接MySQL服务器
```bash
mysql -u root -p
```

#### 3.2 执行初始化脚本
```sql
-- 方法1: 在MySQL命令行中执行
source /path/to/mysql_init_complete.sql;

-- 方法2: 使用命令行直接执行
mysql -u root -p < /path/to/mysql_init_complete.sql
```

### 4. 数据库配置优化

#### 4.1 MySQL配置文件优化 (my.cnf)
```ini
[mysqld]
# 基本配置
port = 3306
socket = /var/lib/mysql/mysql.sock
pid-file = /var/run/mysqld/mysqld.pid
user = mysql
bind-address = 0.0.0.0

# 字符集配置
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci
init_connect = 'SET NAMES utf8mb4'

# 内存配置
innodb_buffer_pool_size = 4G
innodb_log_file_size = 256M
innodb_log_buffer_size = 16M
key_buffer_size = 32M
max_connections = 200
thread_cache_size = 16

# 查询缓存
query_cache_type = 1
query_cache_size = 64M

# 日志配置
log-error = /var/log/mysql/error.log
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 2

# 二进制日志
log-bin = mysql-bin
binlog_format = ROW
expire_logs_days = 7
max_binlog_size = 100M

# 安全配置
local_infile = 0
skip_show_database = 1
```

#### 4.2 重启MySQL服务
```bash
# CentOS/RHEL
sudo systemctl restart mysqld

# Ubuntu/Debian
sudo systemctl restart mysql
```

### 5. 网络配置

#### 5.1 防火墙配置
```bash
# CentOS/RHEL (firewalld)
sudo firewall-cmd --permanent --add-port=3306/tcp
sudo firewall-cmd --reload

# Ubuntu/Debian (ufw)
sudo ufw allow 3306/tcp
sudo ufw reload
```

#### 5.2 网络访问限制
```sql
-- 创建特定IP访问的用户
CREATE USER 'config_compare_app'@'192.168.1.%' IDENTIFIED BY 'ConfigCompare_App_2024!';
CREATE USER 'config_compare_readonly'@'192.168.1.%' IDENTIFIED BY 'ConfigCompare_Readonly_2024!';

-- 授权
GRANT SELECT, INSERT, UPDATE, DELETE ON config_compare.* TO 'config_compare_app'@'192.168.1.%';
GRANT SELECT ON config_compare.* TO 'config_compare_readonly'@'192.168.1.%';

FLUSH PRIVILEGES;
```

### 6. 应用配置

#### 6.1 修改application.yml
```yaml
database:
  type: mysql
  mysql:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://your-mysql-server:3306/config_compare?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: config_compare_app
    password: ConfigCompare_App_2024!
```

#### 6.2 连接池配置优化
```yaml
spring:
  datasource:
    druid:
      # 连接池配置
      initial-size: 10
      min-idle: 10
      max-active: 50
      max-wait: 60000
      time-between-eviction-runs-millis: 60000
      min-evictable-idle-time-millis: 300000
      validation-query: SELECT 1
      test-while-idle: true
      test-on-borrow: false
      test-on-return: false
```

### 7. 备份策略

#### 7.1 全量备份脚本
```bash
#!/bin/bash
# backup_mysql.sh

BACKUP_DIR="/backup/mysql"
DATE=$(date +%Y%m%d_%H%M%S)
DB_NAME="config_compare"

# 创建备份目录
mkdir -p $BACKUP_DIR

# 执行备份
mysqldump -u root -p --single-transaction --routines --triggers $DB_NAME > $BACKUP_DIR/config_compare_$DATE.sql

# 压缩备份文件
gzip $BACKUP_DIR/config_compare_$DATE.sql

# 删除7天前的备份
find $BACKUP_DIR -name "config_compare_*.sql.gz" -mtime +7 -delete

echo "备份完成: $BACKUP_DIR/config_compare_$DATE.sql.gz"
```

#### 7.2 定时备份配置
```bash
# 添加到crontab，每天凌晨2点执行
crontab -e
0 2 * * * /path/to/backup_mysql.sh
```

### 8. 监控配置

#### 8.1 MySQL监控指标
- 连接数
- 查询响应时间
- 慢查询数量
- 缓冲池命中率
- 磁盘I/O

#### 8.2 监控脚本示例
```bash
#!/bin/bash
# mysql_monitor.sh

# 检查MySQL服务状态
if ! systemctl is-active --quiet mysql; then
    echo "MySQL服务未运行，尝试重启..."
    systemctl restart mysql
fi

# 检查连接数
CONNECTIONS=$(mysql -u root -p -e "SHOW STATUS LIKE 'Threads_connected';" | awk 'NR==2 {print $2}')
if [ $CONNECTIONS -gt 150 ]; then
    echo "警告: MySQL连接数过高: $CONNECTIONS"
fi

# 检查慢查询
SLOW_QUERIES=$(mysql -u root -p -e "SHOW GLOBAL STATUS LIKE 'Slow_queries';" | awk 'NR==2 {print $2}')
if [ $SLOW_QUERIES -gt 100 ]; then
    echo "警告: 慢查询数量过多: $SLOW_QUERIES"
fi
```

### 9. 性能优化建议

#### 9.1 索引优化
```sql
-- 分析表索引使用情况
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    CARDINALITY,
    SUB_PART,
    PACKED,
    NULLABLE,
    INDEX_TYPE
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'config_compare'
ORDER BY TABLE_NAME, SEQ_IN_INDEX;

-- 检查未使用的索引
SELECT 
    OBJECT_SCHEMA,
    OBJECT_NAME,
    INDEX_NAME
FROM performance_schema.table_io_waits_summary_by_index_usage
WHERE INDEX_NAME IS NOT NULL
  AND COUNT_STAR = 0
  AND OBJECT_SCHEMA = 'config_compare';
```

#### 9.2 查询优化
```sql
-- 启用查询分析
SET profiling = 1;
-- 执行查询
-- 查看分析结果
SHOW PROFILE;
SHOW PROFILES;
```

### 10. 安全加固

#### 10.1 用户权限最小化
```sql
-- 撤销不必要的权限
REVOKE ALL PRIVILEGES ON *.* FROM 'config_compare_app'@'%';
REVOKE GRANT OPTION ON *.* FROM 'config_compare_app'@'%';

-- 只授予必要的权限
GRANT SELECT, INSERT, UPDATE, DELETE ON config_compare.* TO 'config_compare_app'@'%';
```

#### 10.2 SSL配置
```ini
# my.cnf
[mysqld]
# 启用SSL
require_secure_transport = ON
ssl-ca = /etc/mysql/certs/ca.pem
ssl-cert = /etc/mysql/certs/server-cert.pem
ssl-key = /etc/mysql/certs/server-key.pem
```

### 11. 故障排查

#### 11.1 常见问题
1. **连接超时**
   - 检查防火墙设置
   - 验证用户权限
   - 检查max_connections配置

2. **性能问题**
   - 检查慢查询日志
   - 分析执行计划
   - 优化索引

3. **磁盘空间不足**
   - 清理二进制日志
   - 优化表结构
   - 增加存储空间

#### 11.2 日志分析
```bash
# 错误日志
tail -f /var/log/mysql/error.log

# 慢查询日志
mysqldumpslow /var/log/mysql/slow.log

# 二进制日志
mysqlbinlog /var/lib/mysql/mysql-bin.000001
```

## 验证部署

### 1. 数据库连接测试
```bash
# 使用应用用户连接
mysql -u config_compare_app -p -h your-mysql-server config_compare

# 检查表结构
SHOW TABLES;
DESCRIBE sys_system_info;
```

### 2. 应用连接测试
```bash
# 启动应用并检查日志
tail -f logs/config-compare.log | grep -i database
```

### 3. 功能测试
- 创建系统信息
- 创建服务器实例
- 执行采集任务
- 执行比对任务

## 维护建议

1. **定期备份**: 每日全量备份，每小时增量备份
2. **监控告警**: 设置关键指标告警阈值
3. **性能优化**: 定期分析慢查询和索引使用情况
4. **安全审计**: 定期检查用户权限和访问日志
5. **版本升级**: 定期更新MySQL版本以获取安全补丁

## 联系支持

如遇到部署问题，请联系技术支持团队：
- 邮箱: support@company.com
- 电话: 400-xxx-xxxx