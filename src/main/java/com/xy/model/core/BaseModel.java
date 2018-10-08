package com.xy.model.core;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xy.model.annotation.Table;
import com.xy.model.dto.ModelJoin;
import com.xy.model.dto.ModelSet;
import com.xy.model.dto.ModelWhere;
import com.xy.model.dto.ModelWhereExt;
import com.xy.model.dto.TableField;
import com.xy.utils.util.ListUtil;
import com.xy.utils.util.StringUtil;

public class BaseModel {
	
	public Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private String table;
	
	private String prefix;
	
	private Table annotationTable;

	private String priKey;
	
	private Boolean autoIncrement;
	
	
	private ThreadLocal<ModelWhereExt> modelWhereExtHolder = new ThreadLocal<ModelWhereExt>();
	
	private ThreadLocal<String> fieldsHolder = new ThreadLocal<String>();
	private ThreadLocal<String> excludeFieldsHolder = new ThreadLocal<String>();
	
	private ThreadLocal<ModelWhere> modelWhereHolder = new ThreadLocal<ModelWhere>();
	
	private ThreadLocal<ModelSet> updateSetModelHolder = new ThreadLocal<ModelSet>();
	
	private ThreadLocal<Boolean> distinctHolder = new ThreadLocal<Boolean>();
	
	private ThreadLocal<Boolean> ignoreOnceHolder = new ThreadLocal<Boolean>();
	
	private ThreadLocal<ModelJoin> modelJoinHolder = new ThreadLocal<ModelJoin>();
	
	protected List<TableField> liTableField;
	
	public BaseModel(String table,String prefix){
		this.table = table;
		this.prefix = prefix;
	}
	
	public ModelSet getUpdateModel(){
		if(updateSetModelHolder.get() == null){
			updateSetModelHolder.set(new ModelSet());
		}
		return updateSetModelHolder.get();
	}

	public String getTable() {
		return table;
	}

	protected void setTable(String table) {
		this.table = table;
	}
	
	

	protected Table getAnnotationTable() {
		return annotationTable;
	}

	protected void setAnnotationTable(Table annotationTable) {
		this.annotationTable = annotationTable;
	}

	protected String getPrefix() {
		return prefix;
	}

	protected void setPrefix(String prefix) {
		this.prefix = prefix;
	}
	
	protected String getResource()
	{
		if(this.prefix == null || "".equals(this.prefix)){
			return "modelDao";
		}
		return this.prefix+"ModelDao";
	}

	protected String getPriKey() {
		return priKey;
	}

	protected void setPriKey(String priKey) {
		this.priKey = priKey;
	}

	protected Boolean getAutoIncrement() {
		return autoIncrement == null?false:autoIncrement;
	}

	protected void setAutoIncrement(Boolean autoIncrement) {
		this.autoIncrement = autoIncrement;
	}
	
	protected ModelWhereExt getModelWhereExt() {
		if(modelWhereExtHolder.get() == null){
			modelWhereExtHolder.set(new ModelWhereExt());
		}
		return modelWhereExtHolder.get();
	}
	
	protected String getModelWhereExtString() {
		return this.getModelWhereExt().toString();
	}
	
	protected ModelWhere getModelWhere() {
		if(modelWhereHolder.get() == null){
			modelWhereHolder.set(new ModelWhere());
		}
		return modelWhereHolder.get();
	}
	
	public boolean hasWhere(){
		return this.getModelWhere().hasWhere();
	}
	
	protected String getModelWhereString() {
		String where = this.getModelWhere().toString();
		if(StringUtil.isEmpty(where)){
			return "";
		}
		return "WHERE "+where;
	}
	
	protected void setDistinct(Boolean distinct){
		distinctHolder.set(distinct);
	}
	
	protected String getDistinct(){
		 Boolean bDistinct = this.distinctHolder.get();
         if(bDistinct != null && bDistinct){
                 return " distinct ";
         }
         return "";
	}
	
	
	protected void setFields(String fields) {
		fieldsHolder.set(fields);
	}
	
	protected void setExcludeFields(String fields) {
		excludeFieldsHolder.set(fields);
	}
	
	protected String getExcludeFields() {
		if(excludeFieldsHolder.get() == null){
			return null;
		}
		return excludeFieldsHolder.get();
	}
	
	protected String getFields() {
		if(fieldsHolder.get() == null && excludeFieldsHolder.get() == null){
			return "*";
		}
		
		
		
		if(fieldsHolder.get() != null){
			return fieldsHolder.get();
		}
		
		List<String> fields = new ArrayList<String>();
		if(excludeFieldsHolder.get() != null && liTableField != null){
			for(TableField tableField : liTableField){
				fields.add("`"+tableField.getField()+"`");
			}
			for(String tmp : excludeFieldsHolder.get().split(",")){
				tmp = "`"+tmp.replaceAll("`", "").trim()+"`";
				fields.remove(tmp);
			}
		}
		String result = ListUtil.join(fields,",");
		if(StringUtil.isEmpty(result)){
			result = "*";
		}
		return result;
	}
	
	protected void setIgnoreOnce(boolean ignoreOnce){
		ignoreOnceHolder.set(ignoreOnce);
	}
	
	protected boolean getIgnoreOnce(){
		return ignoreOnceHolder.get() == null?false:ignoreOnceHolder.get();
	}
	
	
	
	protected ModelJoin getModelJoin() {
		return modelJoinHolder.get();
	}

	protected void setModelJoin(ModelJoin modelJoin) {
		this.modelJoinHolder.set(modelJoin);
	}

	protected void clear() {
		if(this.getIgnoreOnce()){
			ignoreOnceHolder.remove();
			return;
		}
		ignoreOnceHolder.remove();
		modelWhereExtHolder.remove();
		fieldsHolder.remove();
		excludeFieldsHolder.remove();
		modelWhereHolder.remove();
		updateSetModelHolder.remove();
		distinctHolder.remove();
		modelJoinHolder.remove();
	}
	
	public String listSQL(){
		String sql = String.format("SELECT %s %s FROM `%s` %s %s",
				this.getDistinct(),
				this.getFields(),this.getTable(),
				this.getModelWhereString(),
				this.getModelWhereExtString());
		this.clear();
		return sql;
	}
	
	

}
