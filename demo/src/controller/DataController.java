package controller;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import service.DataService;
import utils.DocConverter;
import utils.GenerateSql;
import utils.JsonResult;
import utils.PageResult;

@Controller
public class DataController {
	@Autowired
	private DataService dataService;
	private String json = "";
	private GenerateSql generateSql = new GenerateSql();
	private List<LinkedHashMap<String, Object>> mapList = new ArrayList<LinkedHashMap<String, Object>>();

	// 获取树结构json
	@RequestMapping("getTreeJson.do")
	public void getTreeJson(String table, String orderBy, HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		response.setContentType("text/html;charset=utf-8");
		PrintWriter out = response.getWriter();
		String sql = String.format("select * from %s order by %s ", table, orderBy);

		mapList = dataService.getData(sql);
		List<String> pidList = new ArrayList<String>();
		json = "[";
		for (LinkedHashMap<String, Object> linkedHashMap : mapList) {
			if ("".equals(linkedHashMap.get("PID")) || linkedHashMap.get("PID") == null) {
				pidList.add(linkedHashMap.get("ID").toString());
				JSONArray js = JSONArray.fromObject(linkedHashMap);
				json += "{" + js.toString().substring(2, js.toString().length() - 2);
				searchSubNotActiveMenu(table, (String) linkedHashMap.get("ID"));
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
	public void searchSubNotActiveMenu(String table, String parentId) {
		String sql = String.format("select * from %s where pid='%s'", table, parentId);
		List<LinkedHashMap<String, Object>> mapList = new ArrayList<LinkedHashMap<String, Object>>();
		mapList = dataService.getData(sql);
		if (mapList != null && mapList.size() > 0) {
			json += ",\"nodes\":[";
			for (LinkedHashMap<String, Object> linkedHashMap : mapList) {
				JSONArray js = JSONArray.fromObject(linkedHashMap);
				json += "{" + js.toString().substring(2, js.toString().length() - 2);
				searchSubNotActiveMenu(table, (String) linkedHashMap.get("ID"));
				json += "},";
			}
			json = json.substring(0, json.length() - 1);
			json += "]";
		}
	}
     
	
	
	@RequestMapping("getDataCounts.do")
	public void getDataCounts(String id, String table, HttpServletResponse response) throws IOException {
		response.setContentType("terxt/html;charset=utf-8");
		PrintWriter out = response.getWriter();
		JsonResult json;
		String sql = String.format("select count(1) from %s where pid='%s'", table, id);
		int counts = dataService.getDataCounts(sql);
		if (counts > 0) {
			json = JsonResult.Success();
		} else {
			json = JsonResult.Error();
		}
		out.print(json.toJson());
		out.flush();
		out.close();
	}

	// 查询数据字典列表
	@RequestMapping("getDatadictionaryList.do")
	public void getDatadictionaryList(String v_json, HttpServletResponse respone) throws IOException {
		respone.setContentType("text/html;charset=utf-8");
		PrintWriter out = respone.getWriter();
		String sql = "select * from 数据字典 where 1=1 ";
		if (v_json != null) {
			JSONObject json = JSONObject.fromObject(v_json);
			Iterator it = json.keys();
			while (it.hasNext()) {
				String key = (String) it.next();
				Object value = json.get(key);
				sql += String.format(" and %s='%s'", key, value.toString());
			}
		}
		mapList = dataService.getData(sql);
		JSONArray js = JSONArray.fromObject(mapList);
		out.print(js);
		out.flush();
		out.close();
	}
	//查询数据字典明细表
	@RequestMapping("getDatadictionary_mx.do")
	public void getDatadictionary_mx(String v_json, HttpServletResponse respone) throws IOException{
		respone.setContentType("text/html;charset=utf-8");
		PrintWriter out = respone.getWriter();
		String sql = "select * from 数据字典_明细  where 1=1 ";
		if (v_json != null) {
			JSONObject json = JSONObject.fromObject(v_json);
			Iterator it = json.keys();
			while (it.hasNext()) {
				String key = (String) it.next();
				Object value = json.get(key);
				sql += String.format(" and %s='%s'", key, value.toString());
			}
		}
		mapList = dataService.getData(sql);
		JSONArray js = JSONArray.fromObject(mapList);
		out.print(js);
		out.flush();
		out.close();
		
	}
	//根据数据字典类型查询明细
	@RequestMapping("getDatadictionaryByType.do")
	public void getDatadictionaryByType(String type, HttpServletResponse respone) throws IOException{
		respone.setContentType("text/html;charset=utf-8");
		PrintWriter out = respone.getWriter();
		String sql="select sj_mx.* from 数据字典 sj "
				+ "left join 数据字典_明细 sj_mx on sj_mx.FID=sj.ID and NVL(sj_mx.是否禁用, 0)!=1 "
				+ "where NVL(sj.是否禁用,0)!=1 and sj.名称='"+type+"'";
		mapList = dataService.getData(sql);
		JSONArray js = JSONArray.fromObject(mapList);
		out.print(js);
		out.flush();
		out.close();
	}
	
	// 单表json处理
	@RequestMapping("SingleJson.do")
	public void SingleJson(String v_json, String action, HttpServletResponse response) throws IOException {
		response.setContentType("text/html;charset=utf-8");
		PrintWriter out = response.getWriter();
		List<String> sql = new ArrayList<String>();
		Map<String, List> sqlMap = new HashMap<String, List>();
		JsonResult json;
		try {
			if (action.equals("C")) {
				sqlMap = generateSql.generateInsertSql(v_json);
				sql = sqlMap.get("zhuSql");
				for (String s : sql) {
					dataService.insertData(s);
				}
			} else if (action.equals("M")) {
				sqlMap = generateSql.generateUpdateSql(v_json);
				sql = sqlMap.get("zhuSql");
				for (String s : sql) {
					dataService.updateData(s);
				}
			} else if (action.equals("D")) {
				sqlMap = generateSql.generateDeleteSql(v_json);
				sql = sqlMap.get("zhuSql");
				for (String s : sql) {
					dataService.deleteData(s);
				}
			}
			json = JsonResult.Success();
		} catch (Exception e) {
			json = JsonResult.Error();
			System.out.println(e);
		}
		out.print(json.toJson());
		out.flush();
		out.close();
	}
	//主子表json处理
	@RequestMapping("manyJson.do")
	public void manyJson(String v_json,String action,HttpServletResponse response) throws IOException{
		response.setContentType("text/html;charset=utf-8");
		PrintWriter out=response.getWriter();
		List<String> sql = new ArrayList<String>();
		Map<String, List> sqlMap = new HashMap<String, List>();
		JsonResult json;
		try {
			if (action.equals("C")) {
				sqlMap = generateSql.generateInsertSql(v_json);
				sql = sqlMap.get("zhuSql");
				for (String s : sql) {
					dataService.insertData(s);
				}
				sql=sqlMap.get("ziSql");
				if(sql!=null && sql.size()>0){
					for (String s : sql) {
						dataService.insertData(s);
					}
				}
				
			} else if (action.equals("M")) {
				sqlMap = generateSql.generateUpdateSql(v_json);
				sql = sqlMap.get("zhuSql");
				for (String s : sql) {
					dataService.updateData(s);
				}
				sql=sqlMap.get("ziSql");
				if(sql!=null && sql.size()>0){
					for (String s : sql) {
						dataService.updateData(s);
					}
				}
			} else if (action.equals("D")) {
				sqlMap = generateSql.generateDeleteSql(v_json);
				sql = sqlMap.get("zhuSql");
				for (String s : sql) {
					dataService.deleteData(s);
				}
				sql=sqlMap.get("ziSql");
				if(sql!=null && sql.size()>0){
					for (String s : sql) {
						dataService.deleteData(s);
					}
				}
			}else if(action.equals("MC")){
				sqlMap = generateSql.generateUpdateSqlMC(v_json);
				sql = sqlMap.get("zhuSql");
				for (String s : sql) {
					dataService.updateData(s);
				}
				sql=sqlMap.get("ziSql");
				if(sql!=null && sql.size()>0){
					for (String s : sql) {
						dataService.updateData(s);
					}
				}
			}
			json = JsonResult.Success();
		} catch (Exception e) {
			json = JsonResult.Error();
			System.out.println(e);
		}
		out.print(json.toJson());
		out.flush();
		out.close();
	}
	// 查询列表数据
	@RequestMapping("getPageData.do")
	public void getPageData(String listSql, String params, HttpServletResponse respone) throws IOException {
		respone.setContentType("text/html;charset=utf-8");
		PrintWriter out = respone.getWriter();
		if (listSql != null) {
			PageResult pageData = dataService.getPageData(listSql,params);
			out.print(pageData.toJson());
			out.flush();
			out.close();
		}
	}
	//office预览
	
	@RequestMapping("filePreview.do")
	public void filePreview(String filePath,HttpServletRequest request, HttpServletResponse respone) throws IOException{
		respone.setContentType("text/html;charset=utf-8");
		PrintWriter out = respone.getWriter();
		//调用转换类DocConverter,并将需要转换的文件传递给该类的构造方法  
        DocConverter d = new DocConverter(filePath);  
        //调用conver方法开始转换，先执行doc2pdf()将office文件转换为pdf;再执行pdf2swf()将pdf转换为swf;  
        d.conver();  
        System.out.println(d.getswfPath());  
        //生成swf相对路径，以便传递给flexpaper播放器  
        String swfpath = "file"+d.getswfPath().substring(d.getswfPath().lastIndexOf("/"));  
        System.out.println(swfpath);  
        //将相对路径放入sessio中保存  
        String pdfpath=swfpath.substring(0, swfpath.lastIndexOf(".")+1)+"pdf";  
        Map map=new HashMap();  
        map.put("swfpath", swfpath);  
        map.put("pdfpath", pdfpath);  
        JSONArray js = JSONArray.fromObject(map);
		out.print(js);
		out.flush();
		out.close();
	}
	//登录方法
	@RequestMapping("login.do")
	public void login(String user,String pwd,HttpServletResponse response) throws IOException{
		response.setContentType("text/html;charset=utf-8");
		PrintWriter out=response.getWriter();
		String sql=String.format("select * from 用户 where 登录名='%s' and 密码='%s' and NVL(禁用,0)=0",user,pwd);
		mapList = dataService.getData(sql);
		JSONArray js = JSONArray.fromObject(mapList);
		out.print(js);
		out.flush();
		out.close();
	}
	
	public void setDataService(DataService dataService) {
		this.dataService = dataService;
	}
}
