function getProjectList(selectId) {
	var select = $("#" + selectId);
	var url = "/rest/jira/rest/api/projects";
	var options = "<option value='0' selected>--Select project--</option>";
	$.get(url, function(json) {
		$.each(json.list, function(k, v) {
			options += "<option value='" + v.id + "'>" + v.key + " - " + v.name
					+ "</option>";
		})
		select.html(options);
	});
}

function filterIssues(gridId, projectId, fromDate, toDate, fields) {
	var url = "/rest/jira/rest/api/issues/filter";
	var data = [];
	if (projectId == 0) { // maybe 0 or "0"
		$("#message").text("Project is required");
		$("#message").dialog({
			modal : true,
			buttons : {
				Ok : function() {
					$(this).dialog("close");
				}
			}
		});
		return;
	} else {
		$.get(url, {
			"projectId" : projectId,
			"fromDate" : fromDate,
			"toDate" : toDate,
			"fields" : fields
		}, function(json) {
			$.each(json.list, function(k, v) {
				var commits = "";
				$.each(v.commits, function(key, value) {
					commits += "Revision:" + value.revision + "-["
							+ value.author + "] " + value.message + "<br/>";
				});
				data.push({
					id : v.id,
					key : v.key,
					summary : v.summary,
					created : v.created,
					last_updated : v.updated,
					status : v.status,
					commits : commits
				});
			});
			$("#" + gridId).jqGrid('setGridParam', {
				data : data
			}).trigger('reloadGrid');
		});
	}
}