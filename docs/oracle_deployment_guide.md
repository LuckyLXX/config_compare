# Oracle数据库部署指南

## 概述

本文档提供了在内网环境中部署Oracle 19c数据库并配置UAT生产环境配置比对系统的详细指南。

## 环境要求

### 硬件要求

- **CPU**: 最少2核，推荐4核以上
- **内存**: 最少8GB，推荐16GB以上
- **存储**: 最少100GB可用空间，推荐SSD
- **网络**: 稳定的网络连接

### 软件要求

- **操作系统**: Linux (CentOS 7+, RHEL 7+, Ubuntu 18+) 或 Windows Server 2016+
- **Oracle数据库**: Oracle 19c (19.3+)
- **Java**: JDK 11或以上版本
- **应用服务器**: Tomcat 9+ 或 Spring Boot内置服务器

## Oracle数据库安装

### 1. 准备工作

#### 1.1 创建用户和组（Linux）

```bash
# 创建用户组
sudo groupadd oinstall
sudo groupadd dba

# 创建用户
sudo useradd -g oinstall -G dba oracle

# 设置密码
sudo passwd oracle
```

#### 1.2 创建目录结构

```bash
# 创建Oracle基础目录
sudo mkdir -p /u01/app/oracle
sudo mkdir -p /u01/app/oraInventory

# 设置目录权限
sudo chown -R oracle:oinstall /u01
sudo chmod -R 775 /u01
```

#### 1.3 配置系统参数

编辑 `/etc/sysctl.conf` 文件：

```bash
# Oracle参数配置
fs.file-max = 6815744
kernel.sem = 250 32000 100 128
kernel.shmmni = 4096
kernel.shmall = 1073741824
kernel.shmmax = 4398046511104
net.core.rmem_default = 262144
net.core.rmem_max = 4194304
net.core.wmem_default = 262144
net.core.wmem_max = 1048576
net.ipv4.ip_local_port_range = 9000 65500
```

应用参数：

```bash
sudo sysctl -p
```

### 2. 安装Oracle数据库

#### 2.1 下载Oracle 19c安装包

从Oracle官网下载Oracle Database 19c安装包：
- LINUX.X64_193000_db_home.zip
- 或Windows版本的安装包

#### 2.2 解压安装包

```bash
# 切换到oracle用户
su - oracle

# 解压安装包
unzip LINUX.X64_193000_db_home.zip -d /u01/app/oracle/product/19.0.0/dbhome_1
```

#### 2.3 运行安装程序

```bash
cd /u01/app/oracle/product/19.0.0/dbhome_1
./runInstaller
```

按照图形界面提示完成安装：
1. 选择"创建和配置单实例数据库"
2. 选择"服务器类"
3. 选择"典型安装"
4. 设置管理员密码
5. 等待安装完成

### 3. 配置监听器和数据库

#### 3.1 配置监听器

```bash
# 启动监听器配置工具
netca

# 或者手动配置监听器
# 编辑 $ORACLE_HOME/network/admin/listener.ora
```

#### 3.2 创建数据库

```bash
# 启动数据库配置助手
dbca

# 选择创建数据库
# 设置数据库名称：ORCL（或其他名称）
# 设置字符集：AL32UTF8
# 设置内存配置
```

### 4. 配置数据库参数

#### 4.1 连接数据库

```bash
# 使用SQL*Plus连接
sqlplus / as sysdba
```

#### 4.2 修改数据库参数

```sql
-- 修改进程数参数
ALTER SYSTEM SET processes = 300 SCOPE = SPFILE;

-- 修改会话数参数
ALTER SYSTEM SET sessions = 335 SCOPE = SPFILE;

-- 修改游标数参数
ALTER SYSTEM SET open_cursors = 300 SCOPE = SPFILE;

-- 重启数据库使参数生效
SHUTDOWN IMMEDIATE;
STARTUP;
```

## 应用数据库配置

### 1. 创建表空间

```sql
-- 创建应用表空间
CREATE TABLESPACE config_compare_data
DATAFILE 'config_compare_data.dbf' SIZE 100M
AUTOEXTEND ON NEXT 10M MAXSIZE UNLIMITED;

-- 创建索引表空间
CREATE TABLESPACE config_compare_idx
DATAFILE 'config_compare_idx.dbf' SIZE 50M
AUTOEXTEND ON NEXT 5M MAXSIZE UNLIMITED;
```

### 2. 创建用户

```sql
-- 创建应用用户
CREATE USER config_compare IDENTIFIED BY "config_compare_2024"
DEFAULT TABLESPACE config_compare_data
TEMPORARY TABLESPACE temp;

-- 授权
GRANT CONNECT, RESOURCE TO config_compare;
GRANT UNLIMITED TABLESPACE TO config_compare;
GRANT CREATE VIEW TO config_compare;
GRANT CREATE PROCEDURE TO config_compare;
GRANT CREATE SEQUENCE TO config_compare;
GRANT CREATE TRIGGER TO config_compare;
```

### 3. 执行DDL脚本

```bash
# 将database/oracle_schema.sql复制到数据库服务器
# 使用SQL*Plus执行脚本

sqlplus config_compare/config_compare_2024@ORCL @oracle_schema.sql
```

或者使用SQL Developer工具执行脚本。

## 应用配置

### 1. 修改application.yml

```yaml
# 数据库类型配置
database:
  type: oracle  # mysql/oracle
  oracle:
    driver-class-name: oracle.jdbc.OracleDriver
    url: jdbc:oracle:thin:@//your-oracle-host:1521/ORCL
    username: config_compare
    password: config_compare_2024

# MyBatis Plus配置
mybatis-plus:
  configuration:
    database-id: oracle
  global-config:
    db-config:
      id-type: INPUT
```

### 2. 配置连接池

```yaml
spring:
  datasource:
    druid:
      validation-query: SELECT 1 FROM DUAL
      default-auto-commit: false
      # 连接池配置
      initial-size: 5
      min-idle: 5
      max-active: 20
```

## 部署验证

### 1. 数据库连接测试

```bash
# 使用SQL*Plus测试连接
sqlplus config_compare/config_compare_2024@ORCL

# 查看表结构
SELECT table_name FROM user_tables;
```

### 2. 应用启动测试

```bash
# 启动应用
java -jar config-compare-1.0.0-SNAPSHOT.jar

# 查看启动日志
tail -f logs/config-compare.log
```

### 3. 功能测试

1. 访问应用首页
2. 测试登录功能
3. 测试数据查询功能
4. 测试数据插入功能
5. 测试分页查询功能

## 性能优化

### 1. 数据库优化

```sql
-- 创建索引
CREATE INDEX idx_sys_system_name ON sys_system_info(system_name);
CREATE INDEX idx_server_type_code ON server_type(type_code);

-- 分析表统计信息
BEGIN
    DBMS_STATS.GATHER_TABLE_STATS('CONFIG_COMPARE', 'SYS_SYSTEM_INFO');
    DBMS_STATS.GATHER_TABLE_STATS('CONFIG_COMPARE', 'SERVER_TYPE');
    -- 为其他表收集统计信息
END;
/
```

### 2. 连接池优化

```yaml
spring:
  datasource:
    druid:
      # Oracle优化配置
      initial-size: 10
      min-idle: 10
      max-active: 50
      max-wait: 60000
      time-between-eviction-runs-millis: 60000
      min-evictable-idle-time-millis: 300000
      validation-query: SELECT 1 FROM DUAL
      test-while-idle: true
      test-on-borrow: false
      test-on-return: false
```

## 备份与恢复

### 1. 数据备份

```bash
# 使用RMAN备份
rman target /

RMAN> BACKUP DATABASE;

# 或者使用expdp导出
expdp config_compare/config_compare_2024@ORCL DIRECTORY=dp_dir DUMPFILE=config_compare_backup.dmp FULL=Y
```

### 2. 数据恢复

```bash
# 使用RMAN恢复
rman target /

RMAN> RESTORE DATABASE;
RMAN> RECOVER DATABASE;

# 或者使用impdp导入
impdp config_compare/config_compare_2024@ORCL DIRECTORY=dp_dir DUMPFILE=config_compare_backup.dmp FULL=Y
```

## 故障排除

### 1. 常见问题

#### 1.1 连接失败

**问题**: 应用无法连接到Oracle数据库

**解决方案**:
1. 检查监听器状态：`lsnrctl status`
2. 检查数据库状态：`sqlplus / as sysdba` 然后执行 `SELECT status FROM v$instance;`
3. 检查防火墙设置
4. 验证连接字符串格式

#### 1.2 权限问题

**问题**: 应用提示权限不足

**解决方案**:
```sql
-- 检查用户权限
SELECT * FROM user_tab_privs WHERE table_name = 'YOUR_TABLE_NAME';

-- 重新授权
GRANT ALL PRIVILEGES ON YOUR_TABLE_NAME TO config_compare;
```

#### 1.3 性能问题

**问题**: 查询速度慢

**解决方案**:
1. 检查执行计划：`EXPLAIN PLAN FOR your_query;`
2. 创建适当的索引
3. 收集表统计信息
4. 优化SQL语句

### 2. 日志查看

```bash
# Oracle告警日志
tail -f $ORACLE_BASE/diag/rdbms/*/trace/alert*.log

# 应用日志
tail -f logs/config-compare.log

# 监听器日志
tail -f $ORACLE_HOME/diag/tnslsnr/*/listener/alert/log.xml
```

## 安全配置

### 1. 网络安全

```bash
# 配置防火墙，只允许特定IP访问Oracle端口
sudo firewall-cmd --permanent --add-rich-rule='rule family="ipv4" source address="192.168.1.0/24" port protocol="tcp" port="1521" accept'
sudo firewall-cmd --reload
```

### 2. 数据库安全

```sql
-- 修改默认密码
ALTER USER sys IDENTIFIED BY "strong_password";
ALTER USER system IDENTIFIED BY "strong_password";

-- 锁定不需要的用户
ALTER USER scott ACCOUNT LOCK;
ALTER USER hr ACCOUNT LOCK;
```

## 监控与维护

### 1. 监控指标

- 数据库连接数
- 表空间使用率
- 内存使用情况
- 查询执行时间

### 2. 定期维护

```sql
-- 定期收集统计信息
BEGIN
    DBMS_STATS.GATHER_SCHEMA_STATS('CONFIG_COMPARE');
END;
/

-- 定期检查表空间使用情况
SELECT tablespace_name, 
       ROUND(bytes/1024/1024, 2) "Size MB",
       ROUND(maxbytes/1024/1024, 2) "Max Size MB",
       ROUND((bytes/maxbytes)*100, 2) "Usage %"
FROM user_ts_quotas;
```

## 总结

通过本指南，您应该能够在内网环境中成功部署Oracle 19c数据库并配置UAT生产环境配置比对系统。如果在部署过程中遇到问题，请参考故障排除部分或联系数据库管理员。