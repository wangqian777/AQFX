package controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import service.DataService;
import until.GenerateSql;
import until.ResultJson;

@Controller
public class DataController {
	@Autowired
	private DataService dataService;
	private String json = "";
	private GenerateSql generateSql = new GenerateSql();

	// 获取树结构json
	@RequestMapping("getTreeJson.do")
	public void getTreeJson(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		response.setContentType("text/html;charset=utf-8");
		PrintWriter out = response.getWriter();
		String sql = "select * from 菜单";
		List<LinkedHashMap<String, Object>> mapList = new ArrayList<LinkedHashMap<String, Object>>();
		mapList = dataService.getData(sql);
		List<String> pidList = new ArrayList<String>();
		json = "[";
		for (LinkedHashMap<String, Object> linkedHashMap : mapList) {
			if ("".equals(linkedHashMap.get("PID")) || linkedHashMap.get("PID") == null) {
				pidList.add(linkedHashMap.get("ID").toString());
				JSONArray js = JSONArray.fromObject(linkedHashMap);
				json += "{"+ js.toString().substring(2, js.toString().length() - 2);
				searchSubNotActiveMenu((String) linkedHashMap.get("ID"));
				json += "},";
			}

		}
		json = json.substring(0, json.length() - 1) + "]";
		json = json.replace("null", "\"\"");
		json = json.replace("名称", "text");
		out.println(json);
		out.flush();
		out.close();
	}
	
	// 冒泡
	public void searchSubNotActiveMenu(String parentId) {
		String sql = "select * from 菜单 where pid='" + parentId + "'";
		List<LinkedHashMap<String, Object>> mapList = new ArrayList<LinkedHashMap<String, Object>>();
		mapList = dataService.getData(sql);
		if (mapList != null && mapList.size() > 0) {
			json += ",\"nodes\":[";
			for (LinkedHashMap<String, Object> linkedHashMap : mapList) {
				JSONArray js = JSONArray.fromObject(linkedHashMap);
				json += "{"+ js.toString().substring(2, js.toString().length() - 2);
				searchSubNotActiveMenu((String) linkedHashMap.get("ID"));
				json += "},";
			}
			json = json.substring(0, json.length() - 1);
			json += "]";
		}
	}

	// 菜单增删改事件
	@RequestMapping("menuManage.do")
	public void menuManage(String v_json, String action,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		response.setContentType("text/html;charset=utf-8");
		PrintWriter out = response.getWriter();
		ResultJson __ret = new ResultJson();
		List<String> sql=new ArrayList<String>();
		try{
			if (action.equals("M")) {
				Map<String,List> sqlMap=generateSql.menuUpdateSql(v_json);
				sql=sqlMap.get("updateSql");
				for(String s:sql){
					dataService.updateData(s);
				}
				sql=sqlMap.get("deleteSql");
				for(String s:sql){
					dataService.deleteData(s);
				}
				sql=sqlMap.get("insertSql");
				for(String s:sql){
					s=s.replace("'null'","null");
					dataService.insertData(s);
				}
			}else if(action.equals("C")){
				Map<String,List> sqlMap=generateSql.generateInsertSql(v_json);
				sql=sqlMap.get("zhuInsertSql");
				for(String s:sql){
					dataService.insertData(s);
				}
			}else if(action.equals("D")){
				Map<String,List> sqlMap=generateSql.generateDeleteSql(v_json);
				sql=sqlMap.get("zhuDeleteSql");
				for(String s:sql){
					dataService.deleteData(s);
				}
				sql=sqlMap.get("ziDeleteSql");
				for(String s:sql){
					dataService.deleteData(s);
				}
			}
			__ret.setState("1");
		}catch (Exception e) {
			__ret.setState("0");
		}
		
		String resultJson = __ret.GenerateResultJson();
		out.println(resultJson);
		out.flush();
		out.close();
	}

	
	

	
	
	public void setDataService(DataService dataService) {
		this.dataService = dataService;
	}
}
