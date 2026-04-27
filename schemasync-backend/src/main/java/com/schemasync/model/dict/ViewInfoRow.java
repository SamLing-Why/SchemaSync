package com.schemasync.model.dict;

/**
 * 视图定义行
 * 
 * @author SchemaSync Team
 * @since 2026-04-27
 */
public class ViewInfoRow {
    
    /**
     * 视图名称
     */
    private String viewName;
    
    /**
     * 视图SQL定义
     */
    private String viewDefinition;
    
    /**
     * 备注
     */
    private String comment;

    public String getViewName() { return viewName; }
    public void setViewName(String viewName) { this.viewName = viewName; }
    public String getViewDefinition() { return viewDefinition; }
    public void setViewDefinition(String viewDefinition) { this.viewDefinition = viewDefinition; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
