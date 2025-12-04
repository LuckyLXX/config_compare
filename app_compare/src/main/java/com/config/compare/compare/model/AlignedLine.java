package com.config.compare.compare.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 对齐后的行信息（用于前端显示）
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-11-03
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlignedLine {
    
    /**
     * 基线行号（从1开始，-1表示空行）
     */
    private int baselineLineNumber;
    
    /**
     * 当前行号（从1开始，-1表示空行）
     */
    private int currentLineNumber;
    
    /**
     * 基线行内容
     */
    private String baselineContent;
    
    /**
     * 当前行内容
     */
    private String currentContent;
    
    /**
     * 差异类型：EQUAL、ADD、DELETE、MODIFY
     */
    private String diffType;
}

