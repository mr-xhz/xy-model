//package com.xy.model.dto;
//
//import java.lang.reflect.Method;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//import com.xy.model.core.ModelHelper;
//import com.xy.model.emun.ModelCondition;
//import com.xy.model.emun.ModelOperator;
//
//public class ModelWhereList implements ModelWhereBase {
//	
//	private List<ModelWhereBase> _liWhere = new ArrayList<ModelWhereBase>();
//	
//	
//	/** 操作符*/
//	private ModelOperator operator;
//	
//	/** 条件*/
//	private ModelCondition condition;
//	
//	public boolean add(ModelWhereBase mwhere)
//	{
//		return _liWhere.add(mwhere);
//	}
//	
//	public boolean add(String column,Object value)
//	{
//		return _liWhere.add(new ModelWhere(column,value));
//	}
//	
//	public boolean add(String column,Object value,ModelCondition mc)
//	{
//		return _liWhere.add(new ModelWhere(column,value,mc));
//	}
//	
//	public boolean add(String column,Object value,ModelOperator mo)
//	{
//		return _liWhere.add(new ModelWhere(column,value,mo));
//	}
//	
//	public void clear()
//	{
//		_liWhere.clear();
//	}
//	
//	public int size()
//	{
//		return _liWhere.size();
//	}
//	
//	public void setOperator(ModelOperator mop)
//	{
//		this.operator = mop;
//	}
//	
//	public void setCondition(ModelCondition condition)
//	{
//		this.condition = condition;
//	}
//	
//	@SuppressWarnings("unchecked")
//	public void translate(Object where,ModelOperator mop){
//		Class<?> clazz = where.getClass();
//		Class<?>[] interfaces = clazz.getInterfaces();
//		boolean isMap = false;
//		for (Class<?> i : interfaces) {
//			if (i.getName().equals("java.util.Map")) {
//				isMap = true;
//				break;
//			}
//		}
//		if (isMap) {
//			Map<String, Object> map = (Map<String, Object>) where;
//			for (String key : map.keySet()) {
//				Object value = map.get(key);
//				ModelWhere mwhere = new ModelWhere();
//				mwhere.setColumn(key);
//				mwhere.setValue(value);
//				mwhere.setOperator(mop);
//				_liWhere.add(mwhere);
//			}
//		} else {
//			for (Method method : clazz.getMethods()) {
//				if (method.getName().indexOf("get") == 0
//						&& !method.getName().equals("getClass")) {
//					try {
//						Object value = method.invoke(where);
//						if (value == null) {
//							continue;
//						}
//
//						String key = ModelHelper.toLCase(method.getName()
//								.replaceFirst("^get", ""));
//						ModelWhere mwhere = new ModelWhere();
//						mwhere.setColumn(key);
//						mwhere.setValue(value);
//						mwhere.setOperator(mop);
//						_liWhere.add(mwhere);
//					} catch (Exception e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//						continue;
//					}
//				}
//			}
//		}
//		this.setCondition(ModelCondition.AND);
//	}
//	
//	public void translate(Object condition)
//	{
//		translate(condition,ModelOperator.EQ);
//	}
//	
//	@Override
//	public String toString()
//	{
//		if(_liWhere.size() == 0){
//			return "";
//		}
//		String where = "";
//		for(int i=0;i<_liWhere.size();i++){
//			ModelWhereBase mwhere = _liWhere.get(i);
//			String strWhere = mwhere.toString();
//			if(!where.trim().equals("")){
//				if(mwhere.getCondition()!= null && !strWhere.equals("")){
//					where+=mwhere.getCondition().toString();
//				}else if(!strWhere.equals("")){
//					where+=" AND ";
//				}
//			}
//			where+=strWhere;
//		}
//		if(!where.trim().equals("")){
//			where = "("+where+")";
//		}
//		return where;
//	}
//
//	@Override
//	public ModelOperator getOperator() {
//		return this.operator;
//	}
//
//	@Override
//	public ModelCondition getCondition() {
//		return this.condition;
//	}
//}
