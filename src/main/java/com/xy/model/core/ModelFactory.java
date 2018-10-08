package com.xy.model.core;

import java.util.HashMap;
import java.util.Map;

import com.xy.model.annotation.Table;

public class ModelFactory {

	private static Map<String,Model<?>> _cache = new HashMap<String,Model<?>>();
	
	public static <T> Model<T> getModel(Class<T> clazz){
		//先获取表名
		Table table = clazz.getAnnotation(Table.class);
		String tableName;
		String prefix = "";
		boolean create = false;
		if(table == null){
			tableName = ModelHelper.toUUCase(clazz.getSimpleName());
		}else{
			tableName = table.value().split(",")[0];
			prefix = table.prefix();
			create = table.create();
		}
		String cacheKey = tableName+"@@"+clazz.getName();
		Model<?> model = _cache.get(cacheKey);
		if(model == null){
			model = new Model<T>(tableName,prefix,table,clazz);
			_cache.put(cacheKey, model);
		}
		return (Model<T>)model;
	}
}
