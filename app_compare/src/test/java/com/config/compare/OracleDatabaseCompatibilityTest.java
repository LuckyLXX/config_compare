package com.config.compare;

import com.config.compare.config.DatabaseDialectConfig;
import com.config.compare.config.DatabaseProperties;
import com.config.compare.util.SqlCompatibilityUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Oracle数据库兼容性测试类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@SpringBootTest
@ActiveProfiles("test")
public class OracleDatabaseCompatibilityTest {

    @Autowired
    private DatabaseProperties databaseProperties;

    @Autowired
    private DatabaseDialectConfig databaseDialectConfig;

    @Autowired
    private SqlCompatibilityUtil sqlCompatibilityUtil;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 测试数据库类型配置
     */
    @Test
    public void testDatabaseTypeConfiguration() {
        assertNotNull(databaseProperties);
        assertNotNull(databaseProperties.getType());
        assertTrue(databaseProperties.getType().equals("mysql") || databaseProperties.getType().equals("oracle"));
    }

    /**
     * 测试数据库方言配置
     */
    @Test
    public void testDatabaseDialectConfig() {
        assertNotNull(databaseDialectConfig);
        DatabaseDialectConfig.DatabaseType dbType = databaseDialectConfig.getCurrentDatabaseType();
        assertNotNull(dbType);
    }

    /**
     * 测试分页SQL兼容性
     */
    @Test
    public void testPageSqlCompatibility() {
        String originalSql = "SELECT * FROM sys_system_info WHERE status = 1";
        String pageSql = sqlCompatibilityUtil.handlePageSql(originalSql, 0, 10);
        
        assertNotNull(pageSql);
        assertTrue(pageSql.contains("LIMIT") || pageSql.contains("ROWNUM"));
    }

    /**
     * 测试GROUP_CONCAT兼容性
     */
    @Test
    public void testGroupConcatCompatibility() {
        String groupConcatSql = sqlCompatibilityUtil.handleGroupConcat("system_name", ",");
        
        assertNotNull(groupConcatSql);
        assertTrue(groupConcatSql.contains("GROUP_CONCAT") || groupConcatSql.contains("LISTAGG"));
    }

    /**
     * 测试IFNULL兼容性
     */
    @Test
    public void testIfNullCompatibility() {
        String ifNullSql = sqlCompatibilityUtil.handleIfNull("system_name", "'默认值'");
        
        assertNotNull(ifNullSql);
        assertTrue(ifNullSql.contains("IFNULL") || ifNullSql.contains("NVL"));
    }

    /**
     * 测试时间函数兼容性
     */
    @Test
    public void testTimeFunctionCompatibility() {
        String timeFunction = sqlCompatibilityUtil.handleCurrentTime();
        
        assertNotNull(timeFunction);
        assertTrue(timeFunction.contains("NOW()") || timeFunction.contains("CURRENT_TIMESTAMP"));
    }

    /**
     * 测试数据库连接
     */
    @Test
    public void testDatabaseConnection() {
        assertDoesNotThrow(() -> {
            String testSql = databaseDialectConfig.getCurrentDatabaseType() == DatabaseDialectConfig.DatabaseType.ORACLE 
                ? "SELECT 1 FROM DUAL" 
                : "SELECT 1";
            Integer result = jdbcTemplate.queryForObject(testSql, Integer.class);
            assertEquals(1, result);
        });
    }

    /**
     * 测试表是否存在
     */
    @Test
    public void testTableExists() {
        assertDoesNotThrow(() -> {
            String tableName = "sys_system_info";
            String testSql;
            
            if (databaseDialectConfig.getCurrentDatabaseType() == DatabaseDialectConfig.DatabaseType.ORACLE) {
                testSql = "SELECT COUNT(*) FROM user_tables WHERE table_name = UPPER('" + tableName + "')";
            } else {
                testSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = '" + tableName + "'";
            }
            
            Integer count = jdbcTemplate.queryForObject(testSql, Integer.class);
            assertTrue(count > 0, "表 " + tableName + " 应该存在");
        });
    }

    /**
     * 测试基本CRUD操作
     */
    @Test
    public void testBasicCrudOperations() {
        assertDoesNotThrow(() -> {
            // 测试查询
            String countSql = "SELECT COUNT(*) FROM sys_system_info";
            Integer count = jdbcTemplate.queryForObject(countSql, Integer.class);
            assertNotNull(count);
            
            // 测试插入（如果表结构允许）
            if (databaseDialectConfig.getCurrentDatabaseType() == DatabaseDialectConfig.DatabaseType.ORACLE) {
                // Oracle数据库测试
                String insertSql = "INSERT INTO sys_system_info (id, system_name, system_desc, env_type, status) VALUES (seq_sys_system_info.NEXTVAL, '测试系统', '测试描述', 'TEST', 1)";
                jdbcTemplate.update(insertSql);
                
                // 验证插入
                String verifySql = "SELECT COUNT(*) FROM sys_system_info WHERE system_name = '测试系统'";
                Integer verifyCount = jdbcTemplate.queryForObject(verifySql, Integer.class);
                assertTrue(verifyCount > 0);
                
                // 清理测试数据
                String deleteSql = "DELETE FROM sys_system_info WHERE system_name = '测试系统'";
                jdbcTemplate.update(deleteSql);
            }
        });
    }
}