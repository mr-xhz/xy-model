package com.xy.model.exception;

public class ModelNoWhereException extends RuntimeException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public ModelNoWhereException(String message){
		super(message);
	}

}
