-- Apollo配置检查脚本
-- 用于诊断采集任务执行失败的问题

-- 1. 检查Apollo类型的采集模板配置
SELECT 
    ct.id,
    ct.template_name,
    ct.template_type,
    ct.template_content,
    ct.config_params,
    CASE 
        WHEN ct.template_content IS NULL OR ct.template_content = '' THEN '模板内容为空'
        WHEN ct.template_content NOT LIKE '%configServiceUrl%' AND ct.template_content NOT LIKE '%serverUrl%' THEN '缺少Apollo服务器地址'
        WHEN ct.template_content NOT LIKE '%appId%' THEN '缺少应用ID'
        WHEN ct.template_content NOT LIKE '%namespaces%' THEN '缺少命名空间'
        ELSE '配置完整'
    END as config_status
FROM collect_template ct
WHERE ct.template_type = 'APOLLO' 
AND ct.status = 1;

-- 2. 检查服务器实例的Apollo配置
SELECT 
    si.id,
    si.instance_name,
    si.system_id,
    st.type_name,
    si.apollo_server_url,
    si.apollo_app_id,
    si.apollo_cluster,
    si.apollo_namespaces,
    CASE 
        WHEN si.apollo_server_url IS NULL OR si.apollo_server_url = '' THEN '缺少Apollo服务器地址'
        WHEN si.apollo_app_id IS NULL OR si.apollo_app_id = '' THEN '缺少应用ID'
        WHEN si.apollo_namespaces IS NULL OR si.apollo_namespaces = '' THEN '缺少命名空间'
        ELSE '配置完整'
    END as apollo_config_status
FROM server_instance si
JOIN server_type st ON si.server_type_id = st.id
WHERE si.status = 1
AND st.type_code = 'APOLLO_CONFIG';

-- 3. 检查Apollo采集任务的关联情况
SELECT 
    ct.id as template_id,
    ct.template_name,
    ct.template_type,
    ct.template_content as template_config,
    COUNT(DISTINCT ct2.id) as task_count,
    GROUP_CONCAT(DISTINCT ct2.task_name) as task_names
FROM collect_template ct
LEFT JOIN collect_task ct2 ON ct.id = ct2.template_id
WHERE ct.template_type = 'APOLLO'
AND ct.status = 1
GROUP BY ct.id, ct.template_name, ct.template_type, ct.template_content;

-- 4. 检查最近失败的Apollo采集执行记录
SELECT 
    ce.id,
    ce.execute_id,
    ce.task_id,
    ct.task_name,
    ce.execute_status,
    ce.error_message,
    ce.start_time,
    ce.end_time
FROM collect_execution ce
JOIN collect_task ct ON ce.task_id = ct.id
JOIN collect_template ctemp ON ct.template_id = ctemp.id
WHERE ctemp.template_type = 'APOLLO'
AND ce.execute_status = 3  -- 失败状态
AND ce.start_time >= DATE_SUB(NOW(), INTERVAL 7 DAY)
ORDER BY ce.start_time DESC
LIMIT 10;

-- 5. 综合检查：模板和服务器实例的配置匹配情况
SELECT 
    ct.id as template_id,
    ct.template_name,
    si.id as server_id,
    si.instance_name,
    CASE 
        WHEN (ct.template_content LIKE '%configServiceUrl%' OR ct.template_content LIKE '%serverUrl%') 
             AND (si.apollo_server_url IS NOT NULL AND si.apollo_server_url != '') THEN '双向配置'
        WHEN (ct.template_content LIKE '%configServiceUrl%' OR ct.template_content LIKE '%serverUrl%') 
             AND (si.apollo_server_url IS NULL OR si.apollo_server_url = '') THEN '仅模板配置'
        WHEN ct.template_content NOT LIKE '%configServiceUrl%' AND ct.template_content NOT LIKE '%serverUrl%'
             AND (si.apollo_server_url IS NOT NULL AND si.apollo_server_url != '') THEN '仅服务器配置'
        ELSE '无配置'
    END as config_source
FROM collect_template ct
CROSS JOIN server_instance si
JOIN server_type st ON si.server_type_id = st.id
WHERE ct.template_type = 'APOLLO'
AND ct.status = 1
AND si.status = 1
AND st.type_code = 'APOLLO_CONFIG'
ORDER BY ct.id, si.id;