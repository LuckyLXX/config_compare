const mysql = require('mysql2/promise');

async function testConnection() {
  console.log('🔍 测试 MySQL MCP 数据库连接...');

  try {
    const connection = await mysql.createConnection({
      host: '117.72.150.127',
      port: 3306,
      user: 'root',
      password: 'Qq@857251389',
      database: 'config_compare'
    });

    console.log('✅ 数据库连接成功！');

    // 测试基本查询
    const [versionResult] = await connection.execute('SELECT VERSION() as version, DATABASE() as database');
    console.log('📊 MySQL 版本信息:', versionResult[0]);

    // 列出所有表
    const [tablesResult] = await connection.execute('SHOW TABLES');
    console.log('📋 数据库表列表:');
    tablesResult.forEach((row, index) => {
      const tableName = Object.values(row)[0];
      console.log(`  ${index + 1}. ${tableName}`);
    });

    // 检查一些关键表的结构
    if (tablesResult.length > 0) {
      const firstTable = Object.values(tablesResult[0])[0];
      console.log(`\n🔍 表 ${firstTable} 的结构:`);
      const [structureResult] = await connection.execute(`DESCRIBE ${firstTable}`);
      structureResult.forEach(col => {
        console.log(`  - ${col.Field}: ${col.Type} (${col.Null === 'YES' ? '可空' : '非空'})`);
      });

      // 查看表的数据量
      const [countResult] = await connection.execute(`SELECT COUNT(*) as count FROM ${firstTable}`);
      console.log(`📈 ${firstTable} 表的数据量: ${countResult[0].count} 条记录`);
    }

    await connection.end();
    console.log('\n🎉 所有测试完成！MySQL MCP 服务应该可以正常工作。');

  } catch (error) {
    console.error('❌ 数据库连接失败:', error.message);
    console.error('详细错误:', error);
  }
}

testConnection();