package com.xy.model.dto;

import java.util.Map;

public class ModelJoin {

	private Object source;
	
	private String sourceColumn;
	
	private String sourceColumnAlias;
	
	private Map<String,String> targetColumn;

	public Object getSource() {
		return source;
	}

	public void setSource(Object source) {
		this.source = source;
	}

	public String getSourceColumn() {
		return sourceColumn;
	}

	public void setSourceColumn(String sourceColumn) {
		this.sourceColumn = sourceColumn;
	}

	public String getSourceColumnAlias() {
		return sourceColumnAlias;
	}

	public void setSourceColumnAlias(String sourceColumnAlias) {
		this.sourceColumnAlias = sourceColumnAlias;
	}

	public Map<String, String> getTargetColumn() {
		return targetColumn;
	}

	public void setTargetColumn(Map<String, String> targetColumn) {
		this.targetColumn = targetColumn;
	}
	
	
	
}
