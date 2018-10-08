package com.xy.model.dto;

import java.util.HashMap;
import java.util.Map;

import com.xy.model.emun.ModelOperator;

public class ModelSet {

	private Map<String,Object> _set = new HashMap<String,Object>();
	
	public ModelSet(){
		
	}
	
	public ModelSet(String column,Object value){
		_set.put(column, value);
	}
	
	
	
	public ModelSet add(String column,Object value){
		_set.put(column, value);
		return this;
	}
	
	public ModelSet add(ModelSet ms){
		for(String key : ms.get().keySet()){
			_set.put(key, ms.get().get(key));
		}
		return this;
	}
	
	
	public ModelSet add(String column,Object value,ModelOperator mo){
		value = "`"+column+"` "+mo.getOp()+" " + value.toString();
		_set.put(column, value);
		return this;
	}
	
	public boolean isEmpty(){
		return _set.size() == 0;
	}
	
	public Map<String,Object> get(){
		return _set;
	}
}
