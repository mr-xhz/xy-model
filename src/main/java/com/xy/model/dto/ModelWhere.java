package com.xy.model.dto;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.CollectionUtils;

import com.xy.model.core.ModelHelper;
import com.xy.model.emun.ModelCondition;
import com.xy.model.emun.ModelOperator;
import com.xy.utils.util.FastJsonUtils;
import com.xy.utils.util.StringUtil;

public class ModelWhere {

	private String column;
	private Object value;
	private ModelCondition mc;
	private ModelOperator mo;
	private String whereString;
	private List<ModelWhere> listSubWhere;
	
	public ModelWhere(){
		
	}
	
	public ModelWhere(String column,Object value,ModelOperator mo){
		this.column = column;
		this.value = value;
		this.mo = mo;
		this.mc = ModelCondition.AND;
	}
	
	public ModelWhere(String column,Object value,ModelOperator mo,ModelCondition mc){
		this.column = column;
		this.value = value;
		this.mo = mo;
		this.mc = mc;
	}
	
	public ModelWhere where(String column,Object value,ModelOperator mo,ModelCondition mc){
		this.column = column;
		this.value = value;
		this.mo = mo;
		this.mc = mc;
		return this;
	}
	
	public ModelWhere add(ModelWhere mw){
		if(mw == null || !mw.hasWhere()){
			return this;
		}
		if(listSubWhere == null){
			listSubWhere = new ArrayList<ModelWhere>();
		}
		listSubWhere.add(mw);
		return this;
	}
	
	public ModelWhere add(ModelWhere mw,ModelCondition mc){
		if(mw == null || !mw.hasWhere()){
			return this;
		}
		if(listSubWhere == null){
			listSubWhere = new ArrayList<ModelWhere>();
		}
		mw.mc = mc;
		listSubWhere.add(mw);
		return this;
	}
	
	public ModelWhere add(String column,Object value){
		return this.add(column,value,null,null);
	}
	
	public ModelWhere add(String column,Object value,ModelOperator mo){
		return this.add(column,value,mo,null);
	}
	
	public ModelWhere add(String column,Object value,ModelCondition mc){
		return this.add(column,value,null,mc);
	}
	
	public ModelWhere add(String column,Object value,ModelOperator mo,ModelCondition mc){
		if(listSubWhere == null){
			listSubWhere = new ArrayList<ModelWhere>();
		}
		listSubWhere.add(new ModelWhere(column,value,mo,mc));
		return this;
	}
	
	public ModelWhere addFormat(ModelCondition mc,String whereStr,Object... args){
		if(listSubWhere == null){
			listSubWhere = new ArrayList<ModelWhere>();
		}
		ModelWhere mw = new ModelWhere();
		mw.mc = mc;
		mw.whereString = String.format(whereStr, args);
		listSubWhere.add(mw);
		return this;
	}
	
	public ModelWhere addFormat(String whereStr,Object... args){
		return this.addFormat(ModelCondition.AND,whereStr,args);
	}
	
	
	public boolean hasWhere(){
		if(StringUtil.isEmpty(this.toString())){
			return false;
		}
		return true;
	}
	
	@Override
	public String toString(){
		return toString(false);
	}

	public String toString(boolean last){
		String where = "";
		if(StringUtil.isEmpty(column) && CollectionUtils.isEmpty(listSubWhere) && StringUtil.isEmpty(whereString)){
			return where;
		}
		
		if(mo == null){
			mo = ModelOperator.EQ;
		}
		if(StringUtil.isNotEmpty(whereString)){
			return whereString;
		}else if(StringUtil.isNotEmpty(column) && (mo.equals(ModelOperator.IS_NULL) || mo.equals(ModelOperator.IS_NOT_NULL))){
			where += "`"+this.column+"` "+mo.getOp();
		}else if(StringUtil.isNotEmpty(column) && value != null){
			boolean noMarks = false;
			if(mo.equals(ModelOperator.LIKE)){
				if(value.toString().indexOf("%") == -1){
					value = "%"+value.toString()+"%";
				}
			}else if(mo.equals(ModelOperator.LIKE_LEFT)){
				if(value.toString().indexOf("%") == -1){
					value = value.toString()+"%";
				}
			}else if(mo.equals(ModelOperator.LIKE_RIGHT)){
				if(value.toString().indexOf("%") == -1){
					value = "%"+value.toString();
				}
			}else if(mo.equals(ModelOperator.IN) || mo.equals(ModelOperator.NOT_IN)){
				if(value instanceof String){
					if(!value.toString().matches("\\(.*?\\)")){
						value = "(" +value+ ")";
					}
				}else{
					value = FastJsonUtils.toJsonString(value);
				}
				value = value.toString().replaceAll("^\\[", "(").replaceAll("\\]$", ")");
			}else if(mo.equals(ModelOperator.BETWEEN) || mo.equals(ModelOperator.NOT_BETWEEN)){
				String open = "(",close = ")";
				if(last){
					open = "";
					close = "";
				}
				if(value instanceof String){
					value = open +value+ close;
				}else{
					value = FastJsonUtils.toJsonString(value);
				}
				value = value.toString()
						.replaceAll("^\\(", open).replaceAll("\\)$", close)
						.replaceAll("^\\[", open).replaceAll("\\]$", close).replaceAll(",", " AND ");
				noMarks = true;
			}
			where += "`"+this.column+"` "+mo.getOp() + ModelHelper.filter(value,noMarks);
		}else if(!CollectionUtils.isEmpty(listSubWhere)){
			for(ModelWhere modelWhere : listSubWhere){
				if(StringUtil.isNotEmpty(where)){
					if(modelWhere.mc == null){
						modelWhere.mc = ModelCondition.AND;
					}
					where += modelWhere.mc.getCondition();
				}
				boolean bLast = false;
				if(listSubWhere.indexOf(modelWhere) == listSubWhere.size() - 1){
					bLast = true;
				}
				where += modelWhere.toString(bLast);
			}
			where = "(" + where + ")";
		}
		
		return where;
	}
}
