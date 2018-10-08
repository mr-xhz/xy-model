package com.xy.model.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.util.CollectionUtils;

import com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException;
import com.xy.model.annotation.Table;
import com.xy.model.dto.ModelJoin;
import com.xy.model.dto.ModelSet;
import com.xy.model.dto.ModelWhere;
import com.xy.model.dto.TableField;
import com.xy.model.emun.ModelCondition;
import com.xy.model.emun.ModelOperator;
import com.xy.model.exception.ModelNoWhereException;
import com.xy.model.interfaces.BaseModelDao;
import com.xy.utils.spring.SpringContextUtil;
import com.xy.utils.util.IntUtil;
import com.xy.utils.util.ListUtil;
import com.xy.utils.util.LongUtil;
import com.xy.utils.util.MapUtil;
import com.xy.utils.util.StringUtil;

public class Model<T> extends BaseModel{
	
	private Class<T> clazz;
	private boolean bInit = false;
	private Object initLock = new Object();
	
	private BaseModelDao modelDao;
	
	public Model(String table,String prefix,Class<T> clazz){
		super(table,prefix);
		this.clazz = clazz;
	}
	
	public Model(String table,String prefix,Table annotationTable,Class<T> clazz){
		super(table,prefix);
		this.clazz = clazz;
		this.setAnnotationTable(annotationTable);
	}
	
	public void init(){
		if(bInit){
			return;
		}
		synchronized (initLock) {
			if(bInit){
				return;
			}
			List<Map<String,Object>> mapFields = null;
			try{
				mapFields = this.query("SHOW FULL COLUMNS FROM `"+this.getTable()+"`");
			}catch(BadSqlGrammarException e){
				if(e.getCause() instanceof MySQLSyntaxErrorException){
					MySQLSyntaxErrorException ee = (MySQLSyntaxErrorException)e.getCause();
					if(ee.getErrorCode() == 1146){
						//table 不存在
						if(this.getAnnotationTable() != null && this.getAnnotationTable().create()){
							logger.debug("[Model.init] 表`{}`不存在，创建表",this.getTable());
							//如果该表为自动创建的话
							String createSQL = ModelHelper.getCreateSQL(this.getClazz());
							logger.debug("[Model.init] SQL 语句:{}",createSQL);
							query(createSQL);
							logger.debug("[Model.init] SQL 成功创建表:`{}`",this.getTable());
							//判断是否有初始化数据
							List initData = ModelHelper.getInitData(clazz);
							if(initData != null){
								this.ignoreOnce(true).insert(initData);
							}else{
								init();
							}
							return;
						}
					}
				} else {
					logger.error("model error:", e);
				}
				
			}
			
			liTableField = ModelHelper.convert(mapFields, TableField.class);
			for(TableField tableField : liTableField){
				if("PRI".equalsIgnoreCase(tableField.getKey())){
					this.setPriKey(tableField.getField());
				}
				if("auto_increment".equalsIgnoreCase(tableField.getExtra())){
					this.setAutoIncrement(true);
				}
			}
			//判断列是否相等，找出不存在的列
			if(this.getAnnotationTable() != null && this.getAnnotationTable().create()){
				List<TableField> currentTableField = ModelHelper.getTableFields(this.getClazz());
				Map<String,TableField> mapTableField = MapUtil.toMapString(currentTableField, "field");
				for(TableField tableField : liTableField){
					mapTableField.remove(tableField.getField());
				}
				if(mapTableField.size() != 0){
					//设置新的field
					String sql = ModelHelper.getAddColumnSQL(this.getClazz(),mapTableField);
					logger.debug("[Model.init] SQL 语句:{}",sql);
					query(sql);
					init();
				}
			}
			bInit= true;
		}
	}
	
	public List<T> list(){
		init();
		return list(this.getClazz());
	}
	
	public <T2> List<T2> list(Class<T2> clazz){
		init();
		if(this.getModelJoin() != null){
			return listJoin(clazz);
		}
		return ModelHelper.convert(query(listSQL()), clazz);
	}
	
	private <T2> List<T2> listJoin(Class<T2> clazz){
		init();
		ModelJoin modelJoin = this.getModelJoin();
		
		List<Object> whereCondition = new ArrayList<Object>();
		
		List<Object> listSource = new ArrayList<Object>();
		if(!(modelJoin.getSource() instanceof List)){
			listSource.add(modelJoin.getSource());
		}else{
			listSource = (List)modelJoin.getSource();
		}
		
		for(Object o : listSource){
			whereCondition.add(ModelHelper.getValue(o, modelJoin.getSourceColumn()));
		}

		this.include(modelJoin.getTargetColumn().keySet().toArray(new String[0]),modelJoin.getSourceColumnAlias())
		.where(modelJoin.getSourceColumnAlias(),whereCondition,ModelOperator.IN);
		
		List<Map<String,Object>> list = query(listSQL());
		
		Map<Object,Map<String,Object>> listMap = new HashMap<Object,Map<String,Object>>();
		
		for(Map<String,Object> l : list){
			listMap.put(l.get(modelJoin.getSourceColumnAlias()).toString(), l);
		}
		
		List<Map> mapSource = ModelHelper.convert(listSource, Map.class);
		
		for(Map s : mapSource){
			Object o = s.get(modelJoin.getSourceColumn());
			if(o == null){
				continue;
			}
			Object tmp = listMap.get(o.toString());
			if(tmp == null){
				continue;
			}
			for(String key : modelJoin.getTargetColumn().keySet()){
				s.put(modelJoin.getTargetColumn().get(key), ModelHelper.getValue(tmp, key));
			}
		}
		
		return ModelHelper.convert(mapSource, clazz);
	}
	
	public T get(){
		init();
		return get(this.getClazz());
	}
	
	public T get(Object key){
		init();
		if(key != null){
			this.where(this.getPriKey(),key);
		}
		return this.get();
	}
	
	public <T2> T2 get(Class<T2> clazz){
		init();
		if(this.getModelJoin() != null){
			List<T2> result = listJoin(clazz);
			if(result == null || result.isEmpty()){
				return null;
			}
			return result.get(0);
		}
		this.limit(1);
		List<T2> list = ModelHelper.convert(query(listSQL()), clazz);
		if(list == null || list.size() == 0){
			return null;
		}
		return list.get(0);
	}
	
	public boolean isExists(){
		return this.get() != null;
	}
	
	public boolean isNotExists(){
		return this.get() == null;
	}
	
	public boolean isExists(Object value){
		return this.get(value) != null;
	}
	
	public boolean isNotExists(Object value){
		return this.get(value) == null;
	}
	
	public int count(){
		this.fields("count(1)");
		return IntUtil.toInt(this.getSingle(Integer.class));
	}
	
	public <T2> List<T2> listSingle(Class<T2> clazz){
		init();
		List<Map<String,Object>> list = query(this.listSQL());
		List<T2> result = new ArrayList<T2>();
		for(Map<String,Object> m : list){
			for(String key : m.keySet()){
				result.add(ModelHelper.convertSingle(m.get(key), clazz));
				break;
			}
		}
		return result;
	}
	
	public <T2> T2 getSingle(Class<T2> clazz){
		this.limit(1);
		List<T2> list = listSingle(clazz);
		if(CollectionUtils.isEmpty(list)){
			return null;
		}
		return list.get(0);
	}
	
	public String getString(Object value){
		this.where(this.getPriKey(),value);
		return getSingle(String.class);
	}
	
	
	public String getString(){
		return getSingle(String.class);
	}
	
	public Integer getInteger(Object value){
		this.where(this.getPriKey(),value);
		return getSingle(Integer.class);
	}
	
	public Integer getInteger(){
		return getSingle(Integer.class);
	}
	
	public List<String> listString(){
		return listSingle(String.class);
	}
	
	public List<Integer> listInteger(){
		return listSingle(Integer.class);
	}
	
	public Model<T> ignoreOnce(boolean ignoreOnce){
		this.setIgnoreOnce(ignoreOnce);
		return this;
	}
	////////////////////////////////////////////////////////////////////////////////////////
	public Model<T> limit(int size){
		this.getModelWhereExt().limit(size);
		return this;
	}
	
	public Model<T> limit(int start,int size){
		this.getModelWhereExt().limit(start,size);
		return this;
	}
	
	public Model<T> distinct(){
		this.setDistinct(true);
		return this;
	}
	
	public Model<T> orderBy(String orderBy){
		this.getModelWhereExt().orderBy(orderBy);
		return this;
	}
	
	public Model<T> orderBy(String...args){
		List<String> orderBy = new ArrayList<String>();
		String key = null;
		for(String arg : args){
			if(key == null){
				key = arg;
				if(key.indexOf("`") == -1){
					key = "`"+key+"`";
				}
				continue;
			}
			orderBy.add(key+" "+arg);
			key = null;
		}
		if(key != null){
			orderBy.add(key+" ASC");
		}
		this.getModelWhereExt().orderBy(ListUtil.join(orderBy));
		return this;
	}
	
	public Model<T> groupBy(String groupBy){
		this.getModelWhereExt().groupBy(groupBy);
		return this;
	}
	
	public Model<T> fields(String fields){
		this.setFields(fields);
		return this;
	}
	
	public Model<T> include(String fields){
		this.setFields(fields);
		return this;
	}
	
	public Model<T> include(String ...args){
		this.setFields(ListUtil.join(args,","));
		return this;
	}
	
	public Model<T> include(String[] arrArgs,String ...args){
		String fields = ListUtil.join(arrArgs,",");
		if(args.length != 0){
			fields+=","+ListUtil.join(args,",");
		}
		this.setFields(fields);
		return this;
	}
	
	public Model<T> exclude(String fields){
		this.setExcludeFields(fields);
		return this;
	}
	
	public Model<T> exclude(String ...args){
		this.setExcludeFields(ListUtil.join(args,","));
		return this;
	}
	
	public Model<T> exclude(String[] arrArgs,String ...args){
		String excludeFields = ListUtil.join(arrArgs,",");
		if(args.length != 0){
			excludeFields+=","+ListUtil.join(args,",");
		}
		this.setExcludeFields(excludeFields);
		return this;
	}
	
	//////////////////////////////////////////////////////////////////////////////////
	// where start
	//////////////////////////////////////////////////////////////////////////////////
	
	public Model<T> or(String column,Object value){
		return this.where(column, value,ModelCondition.OR);
	}
	
	public Model<T> and(String column,Object value){
		return this.where(column, value,ModelCondition.AND);
	}
	
	public Model<T> where(ModelWhere mw){
		this.getModelWhere().add(mw);
		return this;
	}
	
	public Model<T> where(Object value){
		this.init();
		return this.where(this.getPriKey(),value);
	}
	
	public Model<T> where(ModelWhere mw,ModelCondition mc){
		this.getModelWhere().add(mw,mc);
		return this;
	}
	
	public Model<T> where(String column,ModelOperator mo){
		return this.where(column, null,mo,null);
	}
	
	public Model<T> where(String column,Object value){
		return this.where(column, value,null,null);
	}
	
	public Model<T> where(String column,Object value,ModelOperator mo){
		return this.where(column, value,mo,null);
	}
	
	public Model<T> where(String column,Object value,ModelCondition mc){
		return this.where(column, value,null,mc);
	}
	
	public Model<T> where(String column,Object value,ModelOperator mo,ModelCondition mc){
		if(value == null && !ModelOperator.IS_NULL.equals(mo) && !ModelOperator.IS_NOT_NULL.equals(mo)){
			return this;
		}
		this.getModelWhere().add(column,value,mo,mc);
		return this;
	}
	
	public Model<T> whereFormat(String whereStr,Object ...args){
		this.getModelWhere().addFormat(whereStr,args);
		return this;
	}
	
	public Model<T> whereFormat(ModelCondition mc,String whereStr,Object ...args){
		this.getModelWhere().addFormat(mc,whereStr,args);
		return this;
	}
	
	/////////////////////////////////////////////////////////////////////
	// where end
	/////////////////////////////////////////////////////////////////////
	
	/////////////////////////////////////////////////////////////////////
	// join start
	/////////////////////////////////////////////////////////////////////
	
	public Model<T> join(Object source,String sourceColumn,String sourceColumnAlias,String ...targetColumn){
		ModelJoin modelJoin = new ModelJoin(); 
		modelJoin.setSource(source);
		modelJoin.setSourceColumn(sourceColumn);
		modelJoin.setSourceColumnAlias(sourceColumnAlias);
		Map<String,String> targetColumnMap = new HashMap<String,String>();
		String key = null;
		for(String strTargetColumn : targetColumn){
			if(key == null){
				key = strTargetColumn;
			}else{
				targetColumnMap.put(key, strTargetColumn);
				key = null;
			}
		}
		if(targetColumn.length % 2 == 1){
			targetColumnMap.put(key, key);
		}
		modelJoin.setTargetColumn(targetColumnMap);
		this.setModelJoin(modelJoin);
		return this;
	}
	
	/////////////////////////////////////////////////////////////////////
	// join end
	/////////////////////////////////////////////////////////////////////
	
	
	/////////////////////////////////////////////////////////////////////
	// insert
	/////////////////////////////////////////////////////////////////////
	
	public String insertSQL(Object insertModel,boolean ignorePriKey){
		
		String fields = getFields().trim();
		if(fields.equals("*")){
			fields = "";
		}
		List<String> includeFields = new ArrayList<String>();
		for(String field : fields.split(",")){
			if(StringUtil.isEmpty(field)){
				continue;
			}
			includeFields.add(field.replaceAll("`", ""));
		}
		
		String sql = String.format("INSERT INTO `%s` %s VALUES %s",this.getTable(),
				ModelHelper.getInsertColumn(insertModel,includeFields, liTableField, ignorePriKey),
				ModelHelper.getInsertValue(insertModel,includeFields, liTableField, ignorePriKey)
				);
		this.clear();
		return sql;
	}
	
	public int insert(Object insertModel,boolean ignorePriKey){
		init();
		int result =  IntUtil.toInt(query(insertSQL(insertModel,ignorePriKey)).get(0).get("affactRow"));
		result = 1;
		if(result > 0 && this.getAutoIncrement()){
			//获取lastInsertId
			long lastInsertId = LongUtil.toLong(query("SELECT LAST_INSERT_ID() as id").get(0).get("id"));
			ModelHelper.setLastInsertId(insertModel, this.getPriKey(), lastInsertId);
		}
		
		return result;
	}
	
	public int insert(Object insertModel){
		return insert(insertModel,this.getAutoIncrement());
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private String updateSQL(Object updateModel,String fields,boolean cleanWhere,boolean ignorePriKey){
		String sql = "";
		if(fields.equals("*")){
			fields = "";
		}
		if(!this.hasWhere()){
			Object priValue = ModelHelper.getValue(updateModel, this.getPriKey());
			if(priValue == null){
				priValue = ModelHelper.getValue(updateModel, "_value");
				if(priValue == null){
					throw new ModelNoWhereException("请传入更新条件");
				}else{
					this.where(ModelHelper.getValue(updateModel, "_column").toString(), priValue);
					cleanWhere = true;
				}
			}else{
				this.where(this.getPriKey(), priValue);
				cleanWhere = true;
			}
		}
		List<String> includeField = new ArrayList<String>();
		for(String field : fields.split(",")){
			if(StringUtil.isEmpty(field)){
				continue;
			}
			includeField.add(field.replaceAll("`", ""));
		}
		
		sql = String.format("UPDATE `%s` SET %s %s", this.getTable(),
				ModelHelper.getSetSQL(updateModel,includeField,this.liTableField,ignorePriKey),
				this.getModelWhereString());
		if(cleanWhere){
			this.clear();
		}
		return sql;
	}
	
	public String updateSQL(Object updateModel){
		return updateSQL(updateModel,true);
	}
	
	public String updateSQL(Object updateModel,boolean ignorePriKey){
		String fields = getFields().trim();
		if(updateModel instanceof List){
			List<String> liSQL = new ArrayList<String>();
			for(Object o : (List)updateModel){
				if(o instanceof ModelSet){
					o = ((ModelSet)o).get();
				}
				String sql = this.updateSQL(o,fields,false,ignorePriKey);
				liSQL.add(sql);
			}
			this.clear();
			return ListUtil.join(liSQL,";");
		}else{
			return this.updateSQL(updateModel,fields,true,ignorePriKey);
		}
	}
	
	public Model<T> sub(String column,Object value){
		this.getUpdateModel().add(column, value,ModelOperator.SUB);
		return this;
	}
	
	public Model<T> add(String column,Object value){
		this.getUpdateModel().add(column, value,ModelOperator.ADD);
		return this;
	}
	
	public int update(){
		if(this.getUpdateModel().isEmpty()){
			this.clear();
			return 0;
		}
		return this.update(this.getUpdateModel());
	}
	
	public int update(String column,Object value){
		Map<String,Object> map = new HashMap<String,Object>();
		map.put(column, value);
		return update(map);
	}
	
	public int update(ModelSet updateModel){
		return update(updateModel.get());
	}
	
	public int update(Object updateModel){
		init();
		int result =  IntUtil.toInt(query(updateSQL(updateModel)).get(0).get("affactRow"));
		return result;
	}
	
	private String deleteSQL(){
		String sql = String.format("DELETE FROM `%s` %s", this.getTable(),
				this.getModelWhereString());
		this.clear();
		return sql;
	}
	
	//保存
	public int save(Object saveModel){
		if(StringUtil.isEmpty(this.getPriKey())){
			return this.insert(saveModel);
		}
		Object key = ModelHelper.getValue(saveModel, this.getPriKey());
		if(key == null){
			return this.insert(saveModel);
		}else{
			return this.update(saveModel);
		}
	}
	
	public int delete(Object value){
		this.where(this.getPriKey(),value);
		return delete();
	}
	
	public int delete(){
		init();
		if(!this.hasWhere()){
			throw new ModelNoWhereException("请传入删除条件，防止误删");
		}
		int result =  IntUtil.toInt(query(deleteSQL()).get(0).get("affactRow"));
		return result;
	}

	
	////////////////////////////////////////////////////////
	public List<Map<String,Object>> query(String sql) {
		logger.debug(sql);
		long currentTime = System.currentTimeMillis();
		while(SpringContextUtil.getContext() == null){
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
			if(System.currentTimeMillis() - currentTime > 60000){
				logger.error("请先配置SpringContextUtil");
				return null;
			}
			continue;
		}
		if(modelDao == null && SpringContextUtil.getContext() != null){
			modelDao = (BaseModelDao) SpringContextUtil
					.getBean(this.getResource());
		}
		List<Map<String,Object>> result = new ArrayList<Map<String,Object>>();
		if (sql.toLowerCase().indexOf("select ") == 0) {
			return modelDao.select(sql);
		} else if (sql.toLowerCase().indexOf("insert ") == 0) {
		    Map<String,Object> affactRow = new HashMap<String,Object>();
		    affactRow.put("affactRow", modelDao.insert(sql));
		    result.add(affactRow);
		} else if (sql.toLowerCase().indexOf("update ") == 0) {
		    Map<String,Object> affactRow = new HashMap<String,Object>();
            affactRow.put("affactRow", modelDao.update(sql));
            result.add(affactRow);
		} else if (sql.toLowerCase().indexOf("delete ") == 0) {
		    Map<String,Object> affactRow = new HashMap<String,Object>();
            affactRow.put("affactRow", modelDao.delete(sql));
            result.add(affactRow);
		}else{
			return modelDao.select(sql);
		}
		return result;
	}

	protected Class<T> getClazz() {
		return clazz;
	}

	protected void setClazz(Class<T> clazz) {
		this.clazz = clazz;
	}
	
	public ModelSelect createSelect(){
		return new ModelSelect(this.clazz);
	}
}
