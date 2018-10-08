package com.xy.model.core;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.annotation.JSONField;
import com.xy.model.annotation.Column;
import com.xy.model.annotation.Table;
import com.xy.model.dto.TableField;
import com.xy.utils.util.BeanUtil;
import com.xy.utils.util.DateUtil;
import com.xy.utils.util.FastJsonUtils;
import com.xy.utils.util.ListUtil;
import com.xy.utils.util.StringUtil;

public class ModelHelper {
	
	
	/**
	 * "abc" -> "Abc"
	 * @param str
	 * @return
	 */
	public static String toUCase(String str) {
		return ("" + str.charAt(0)).toUpperCase() + str.substring(1);
	}
	
	/**
	 * "Abc" -> "abc"
	 * @param str
	 * @return
	 */
	public static String toLCase(String str) {
		return ("" + str.charAt(0)).toLowerCase() + str.substring(1);
	}
	
	/**
	 * "abc_xyz" -> "AbcXyz"
	 * @param str
	 * @return
	 */
	public static String toHHCase(String str) {
		StringBuilder sb = new StringBuilder();
		for(String s : str.split("_")) {
			sb.append(toUCase(s));
		}
		return sb.toString();
	}
	
	/**
	 * "AbcXye" -> "abc_xyz"
	 * @param str
	 * @return
	 */
	public static String toUUCase(String str) {
		str = str.replaceAll("([A-Z][a-z])", "_$1");
		StringBuilder sb = new StringBuilder();
		for(String s : str.split("_")) {
			if("".equals(s.trim())){
				continue;
			}
			sb.append("_"+toLCase(s));
		}
		return sb.toString().substring(1);
	}
	
	public static String getNameByAnnotation(Class<?> clazz,String field){
        List<Field> liField = BeanUtil.getAllField(clazz);
        for(Field f : liField){
        		Column column = f.getAnnotation(Column.class);
        		if(column == null){
	        	 	JSONField jsonField = f.getAnnotation(JSONField.class);	
	                if(jsonField == null){
	                	continue;    
	                }
	                if(jsonField.name() != null && jsonField.name().equals(field)){
	                    return f.getName();
	                }
        		}else{
        			if(column.name() != null && column.name().equals(field)){
	                    return f.getName();
	                }
        		}
        }
        return field;
	}
	
	public static String getAliasNameByAnnotation(Class<?> clazz,String field){
        List<Field> liField = BeanUtil.getAllField(clazz);
        for(Field f : liField){
        		Column column = f.getAnnotation(Column.class);
        		if(column == null){
	        	 	continue;
        		}else{
        			if(column.name() != null && column.name().equals(field)){
	                    return f.getName();
	                }
        		}
        }
        return field;
	}
	
	public static void main(String[] args){
		System.out.println("(1,2,3,4)".replaceAll("\\($", "["));
		System.out.println("(1,2,3,4)".matches("^\\(.*\\)$"));
	}
	
	public static String toUfirst(String name) {
		String first = name.substring(0, 1).toUpperCase();
		String rest = name.substring(1, name.length());
		return new StringBuffer(first).append(rest).toString(); 
	}
	
	public static <T> List<T> convert(Object source,Class<T> clazz){
		if(source == null) return null;
		if(source instanceof List){
			List list = (List)source;
			if(list.size() != 0){
				Object tmpO = list.get(0);
				Map<String,String> alias = new HashMap<String,String>();
				if(tmpO instanceof Map){
					Map<String,Object> tmpMap = (Map<String,Object>)tmpO;
					for(String key : tmpMap.keySet()){
						String realName = getAliasNameByAnnotation(clazz,key);
						if(StringUtil.isEmpty(realName) || realName.equals(key)){
							continue;
						}
						alias.put(key, realName);
					}
				}
				
				for(String key : alias.keySet()){
					for(Object o : (List)source){
						if(o instanceof Map){
							Map<String,Object> tmp = (Map<String,Object>)o;
							Object value = tmp.get(key);
							tmp.remove(key);
							tmp.put(alias.get(key), value);
						}
					}
				}
				
			}
		}
		return FastJsonUtils.getBeanList(FastJsonUtils.toJsonString(source), clazz);
	}
	

	
	//将Object转换成指定类型
	public static <T> T convertSingle(Object source,Class<T> clazz)
	{
		if(source == null){
			return null;
		}
		Class<?> sourceClazz = source.getClass();
		if(sourceClazz.equals(clazz)){
			return (T)source;
		}
		try {
			if(clazz.equals(String.class)){
				return (T)source.toString();
			}
			Method valueOf = clazz.getMethod("valueOf", String.class);
			return (T)valueOf.invoke(null, source.toString());
		} catch (NoSuchMethodException e) {
			return null;
		} catch (Exception e) {
			return null;
		}
	}
	
	
	
	public static Map<String,Object> formatCols(String cols,Object value)
	{
		Map<String,Object> map = new HashMap<String,Object>();
		if(cols.indexOf("?") >= 0){
			String[] arrCols = cols.split(",");
			for(int i=0;i<arrCols.length;i++){
				String col = arrCols[i].split("=")[0].replaceAll("\\s", "");
				String methodName = "get"+toUCase(col);
				try {
					Method method = value.getClass().getMethod(methodName);
					map.put(col,method.invoke(value));
				} catch (Exception e) {
				} 
				
			}
		}else{
			map.put(cols, value);
		}
		return map;
	}
	
	public static String getSetSQL(Object updateModel,List<String> includeFields,List<TableField> liTableField,boolean ignorePriKey){
		
		List<String> fields = new ArrayList<String>();
		
		for(TableField tableField : liTableField){
			if("PRI".equalsIgnoreCase(tableField.getKey()) && "auto_increment".equalsIgnoreCase(tableField.getExtra()) && ignorePriKey){
				continue;
			}
			if(!(updateModel instanceof List)){
				if(getValue(updateModel,tableField.getField()) == null){
					continue;
				}
			}
			if(includeFields.isEmpty()){
				fields.add(tableField.getField());
			}else{
				if(includeFields.indexOf(tableField.getField()) != -1){
					fields.add(tableField.getField());
				}
			}
		}
		
		List<String> setValue = new ArrayList<String>();
		
		for(String field : fields){
			Object v = getValue(updateModel,field);
			if(v == null){
				setValue.add("`"+field+"` = null");
			}else{
				if(v instanceof String){
					if(v.toString().indexOf(field) >= 0 && (v.toString().indexOf("+") >= 0 || v.toString().indexOf("-") >= 0)){
						setValue.add("`"+field+"` = " + v.toString());
						continue;
					}
				}
				setValue.add("`"+field+"` = " + filter(v,false).trim());
			}
		}
		
		return ListUtil.join(setValue);
		
	}
	
	public static Object getValue(Object source,String field){
		if(source == null) return null;
		Object value = null;
		if(source instanceof Map){
			value = ((Map)source).get(field); 
		}else{
			try {
				field = getNameByAnnotation(source.getClass(),field);
				PropertyDescriptor pd = new PropertyDescriptor(field,source.getClass());
				Method method = pd.getReadMethod();
				value = method.invoke(source);
			} catch (Exception e) {
				return null;
			}
		}
		return value;
	}
	
	public static void setLastInsertId(Object source,String priKey,Long lastId){
		if(source == null) return;
		if(!(source instanceof List)){
			List<Object> tmp = new ArrayList<Object>();
			tmp.add(source);
			source = tmp;
		}
		
		
		for(Object o : (List)source){
			if(o instanceof Map){
				((Map)o).put(priKey, lastId);
			}else{
				try {
					priKey = getNameByAnnotation(o.getClass(),priKey);
					PropertyDescriptor pd = new PropertyDescriptor(priKey,o.getClass());
					Method method = pd.getWriteMethod();
					Field field = BeanUtil.getField(o.getClass(),priKey);
					Class<?> paramType = field.getType();
					Object value = lastId;
					Method valueOf = paramType.getMethod("valueOf",String.class);
					value = valueOf.invoke(null,lastId.toString());
					method.invoke(o,value);
				} catch (Exception e) {
					return;
				}
			}
			lastId++;
		}
	}
	
	
	/** 替换特殊字符 防止sql语句注入*/
	public static String filter(Object value,boolean noMarks)
	{
		if(value == null){
			return "null";
		}
		if(value instanceof String){
			String tmpStr = value.toString().replace("\\", "\\\\").replace("'", "\\'").replace("\r", "").replace("\n", "");
			if(tmpStr.matches("^\\(.*\\)$") || noMarks){
				return " "+tmpStr+" ";
			}
			return " '"+tmpStr+"' ";
		}else if(value instanceof Date){
			return " '"+DateUtil.getDateTimeFormat((Date)value)+"' ";
		}
		return " "+value.toString()+" ";
	}
	
	/** 获取值*/
	public static String getInsertValue(Object insertModel,List<String> includeFields,List<TableField> liTableField,boolean ignorePriKey)
	{
		List<String> values = new ArrayList<String>();
		
		List<String> fields = new ArrayList<String>();
		
		Map<String,TableField> mapTableField = new HashMap<String,TableField>();
		
		for(TableField tableField : liTableField){
			if("PRI".equalsIgnoreCase(tableField.getKey()) && "auto_increment".equalsIgnoreCase(tableField.getExtra()) && ignorePriKey){
				continue;
			}
			mapTableField.put(tableField.getField(), tableField);
			if(!(insertModel instanceof List)){
				if(getValue(insertModel,tableField.getField()) == null){
					continue;
				}
			}
			if(includeFields.isEmpty()){
				fields.add(tableField.getField());
			}else{
				if(includeFields.indexOf(tableField.getField()) != -1){
					fields.add(tableField.getField());
				}
			}
		}
		
		if(!(insertModel instanceof List)){
			List<Object> tmp = new ArrayList<Object>();
			tmp.add(insertModel);
			insertModel = tmp;
		}
		
		
		for(Object o : (List)insertModel){
			
			List<String> value = new ArrayList<String>();
			for(String field : fields){
				Object v = getValue(o,field);
				if(v == null){
					TableField tableField = mapTableField.get(field);
					if(tableField.getNull() != null && tableField.getNull().equalsIgnoreCase("NO")
							&& tableField.getDefault() != null
							){
						value.add(filter(tableField.getDefault(),false).trim());
					}else{
						value.add("null");
					}
				}else{
					value.add(filter(v,false).trim());
				}
			}
			
			values.add("("+ListUtil.join(value)+")");
			
		}
		
		return ListUtil.join(values);
	}
	
	/** 获取列名*/
	public static String getInsertColumn(Object insertModel,List<String> includeFields,List<TableField> liTableField,boolean ignorePriKey)
	{
		List<String> cloumn = new ArrayList<String>();
		
		for(TableField tableField : liTableField){
			if("PRI".equalsIgnoreCase(tableField.getKey()) && "auto_increment".equalsIgnoreCase(tableField.getExtra()) && ignorePriKey){
				continue;
			}
			if(!(insertModel instanceof List)){
				if(getValue(insertModel,tableField.getField()) == null){
					continue;
				}
			}
			if(includeFields.isEmpty()){
				cloumn.add("`"+tableField.getField()+"`");
			}else{
				if(includeFields.indexOf(tableField.getField()) != -1){
					cloumn.add("`"+tableField.getField()+"`");
				}
			}
		}
		return "("+ListUtil.join(cloumn)+")";
	}
	public static List<TableField> getTableFields(Class<?> clazz){
		List<TableField> liTableField = new ArrayList<TableField>();
		List<Field> liField = BeanUtil.getAllField(clazz);
		for(Field field : liField){
			TableField tableField = new TableField();
			liTableField.add(tableField);
			
			Column column = field.getAnnotation(Column.class);
			if(column == null){
				continue;
			}
			String name = field.getName();
			Column columnField = field.getAnnotation(Column.class);
			if(columnField != null){
				name = columnField.name();
			}
			
			if(column.priKey()){
				tableField.setKey("PRI");
			}
			tableField.setComment(column.comment());
			tableField.setDefault(column.def());
			tableField.setExtra("");
			tableField.setField(name);
			tableField.setType(column.jdbcType());
			tableField.setNull(column.notNull()?"NO":"YES");
		}
		return liTableField;
	}
	
	public static String getCreateSQL(Class<?> clazz){
		Table table = clazz.getAnnotation(Table.class);
		
		List<String> liColumn = new ArrayList<String>();
		List<Field> liField = BeanUtil.getAllField(clazz);
		String priKeyColumn = "";
		for(Field field : liField){
			Column column = field.getAnnotation(Column.class);
			if(column == null){
				continue;
			}
			String name = field.getName();
			Column columnField = field.getAnnotation(Column.class);
			if(columnField != null){
				name = columnField.name();
			}
			boolean autoIncrement = column.priKey()?column.autoIncrement():false;
			if(column.priKey()){
				priKeyColumn = "PRIMARY KEY (`"+name+"`)";
			}
			liColumn.add(String.format("`%s` %s %s %s %s %s COMMENT '%s'", 
					name,
					column.jdbcType(),
					(column.notNull()?"NOT NULL":""),
					(autoIncrement?"AUTO_INCREMENT":""),
					(StringUtil.isNotEmpty(column.def())?"DEFAULT "+column.def():""),
					(StringUtil.isNotEmpty(column.charset())?"CHARSET "+column.charset():""),
					column.comment()
				));
		}
		if(StringUtil.isNotEmpty(priKeyColumn)){
			liColumn.add(priKeyColumn);
		}
		String strColumn = ListUtil.join(liColumn,",");
		String sql = "CREATE TABLE `%s`( %s ) ENGINE=%s DEFAULT CHARSET=%s COMMENT='%s'";
		sql = String.format(sql, table.value(),strColumn,table.engine(),table.charset(),table.comment());
		return sql;
	}
	
	public static String getAddColumnSQL(Class<?> clazz,Map<String,TableField> mapTableField){
		Table table = clazz.getAnnotation(Table.class);
		List<String> liSQL = new ArrayList<String>();
		for(String key : mapTableField.keySet()){
			TableField tableField = mapTableField.get(key);
			liSQL.add(String.format("ALTER TABLE `%s` ADD %s;", table.value(),
					String.format("`%s` %s %s %s COMMENT '%s'", 
							tableField.getField(),
							tableField.getType(),
					(tableField.getNull().equalsIgnoreCase("NO")?"NOT NULL":""),
					(StringUtil.isNotEmpty(tableField.getDefault())?"DEFAULT "+tableField.getDefault():""),
					tableField.getComment()
				)));
		}
		return ListUtil.join(liSQL,"");
	}
	
	public static <T> List<T> getInitData(Class<T> clazz){
		try {
			Method method = clazz.getMethod("init");
			Object result = method.invoke(null);
			if(result != null){
				return (List<T>)result;
			}
		} catch (Exception e) {
			
		} 
		return null;
	}
}
