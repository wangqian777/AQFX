﻿$(function(){
	console.log(localStorage.FormMode);
	if(localStorage.FormMode=="Edit"){
		var json = { "ID": localStorage.sjzdID };
        var __str = JSON.stringify(json);
		$.ajax({
			url:"../../getDatadictionaryList.do",
			type:"POST",
			data:{"v_json":__str},
			dataType:"JSON",
			async:false,
			success:function(data){
				$("#frmdata").fill(data[0], { styleElementName: 'none' });
				$("input[name=是否禁用]").attr("checked",data[0].是否禁用); 
				$("#id").val(data[0].ID);
				
			}
		});
	}
	$("#btntable-save").click(function(){
		var json = {};
		var action="";
		var data;
		if(localStorage.FormMode=="Add"){
			action="C";
		}else{
	        action="M";
		}
		data = $("#frmdata").serializeObject();
		json.table = $("#frmdata").data("table");
        json.tabledata = data;
        FormAction(json,action);
	});
	function FormAction(data,action){
		 var __str= JSON.stringify(data);
		 $.ajax({
				type:"POST",
				url:"../../SingleJson.do",
				data:{"v_json":__str,"action":action},
				dataType:"JSON",
				async:false,
				success:function(data){
					if(data.state==1){
						layer.msg("操作成功");
					}else{
						layer.msg("操作失败");
					}
				}
			});
	 }
});