package com.xy.model.emun;

public enum ModelOperator {
	/** 等于*/
	EQ("="),
	/** 不等于*/
	NEQ("<>"),
	/** 大于*/
	GT(">"),
	/** 大于等于*/
	EGT(">="),
	/** 小于*/
	LT("<"),
	/** 小于等于*/
	ELT("<="),
	/** 模糊查询*/
	LIKE("LIKE"),
	LIKE_LEFT("LIKE"),
	LIKE_RIGHT("LIKE"),
	/** IN*/
	IN("IN"),
	/** NOT IN*/
	NOT_IN("NOT IN"),
	/** BETWEEN*/
	BETWEEN("BETWEEN"),
	/** NOT BETWEEN*/
	NOT_BETWEEN("NOT BETWEEN"),
	/** 加*/
	ADD("+"),
	/** 减*/
	SUB("-"),
	/**判断非空*/
	IS_NOT_NULL("IS NOT NULL"),
	/**判断为空*/
	IS_NULL("IS NULL")
	;
	
	private String operator;
	private ModelOperator(String operator){
		this.operator = operator;
	}
	public String getOp()
	{
		return this.operator;
	}
}
