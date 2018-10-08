package com.xy.model.interfaces;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

public interface BaseModelDao {

	public List<Map<String,Object>> select(@Param("paramSQL")String sql);
	
	public int insert(@Param("paramSQL")String sql);
	
	public int update(@Param("paramSQL")String sql);
	
	public int delete(@Param("paramSQL")String sql);
}
