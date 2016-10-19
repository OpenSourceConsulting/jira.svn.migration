function getProjectList(selectId) {
	var select = $("#" + selectId);
	var url = "/rest/api/projects";
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
	var url = "/rest/api/issues/filter";
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
				if (commits === "") {
					commits = "No commit.";
				}
				data.push({
					id : v.id,
					key : v.key,
					summary : v.summary,
					url : v.url,
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

// format cell jqgrid for adding a link to summary column
function showJiraLink(cellvalue, options, rowObject) {
	var restUrl = rowObject["url"];
	var baseLink = restUrl.substring(0, restUrl.indexOf("rest"));
	var url = baseLink + "browse/" + rowObject["key"];

	return "<a target='_blank' href='" + url + "'>" + cellvalue + "</a>";
}
function exportSourceCode(issueIds) {
	var issueKeys = [];
	for (i = 0; i < issueIds.length; i++) {
		id = issueIds[i];
		var key = $("#issueList").jqGrid('getCell', id, 'key');
		issueKeys.push(key);
	}
	if (issueKeys.length == 0) {
		return;
	} else {
		var url = "/rest/api/svn/export";
		window.location.href = url + "?issueKeys[]=" + issueKeys.join(",");
	}
}

function generateSVNTree(treeId, callback) {
	var tree = $("#" + treeId);
	var url = "/rest/api/svn/tree";
	$.get(url, function(json) {

		var rootNode = '<li><span><i class="fa fa-lg fa-database"></i>'
				+ json.resource + '</span>';
		rootNode += "<span class='hidden'>" + json.url + "</span>";
		var childNodes = "<ul>"
		childNodes += listChildNodes(json.childNodes);
		childNodes += "</ul>";
		rootNode += childNodes;
		rootNode += "</li>";
		tree.html("<ul>" + rootNode + "</ul>");
		callback();
	});
}

function listChildNodes(childNodes) {
	var tooltip = "";
	var result = "";
	if (childNodes === undefined || childNodes === null) {
		return;
	}
	$.each(childNodes, function(k, v) {
		tooltip = "Resource:" + v.resource + "\n" + "Last Change Date:"
				+ v.lastChanged + "\n" + "Last Author:" + v.lastAuthor;
		var cls = "fa fa-lg "
		if (v.type === "dir") {
			cls += "fa-folder";
			result += '<li style="display:none"><span title="' + tooltip
					+ '"><i class="' + cls + '"></i>' + v.resource + '\t'
					+ v.revision + '</span>';
			result += "<span class='hidden'>" + v.url + "</span>";
			if (v.childNodes !== null) {
				result += "<ul>";
				result += listChildNodes(v.childNodes);
				result += "</ul>";
			}
		} else if (v.type === "file") {
			cls += "fa-file-code-o";
			result += '<li style="display:none"><span title="' + tooltip
					+ '"><i class="' + cls + '"></i>' + v.resource + '\t'
					+ v.revision + '</span>';
			result += "<span class='hidden'>" + v.url + "</span>";
		}

	});
	return result;
}

function submitImportForm() {
	var formData = new FormData();
	formData.append("selectedPath",$("#selectedPath").val());
	formData.append("message",$("#message").val());
	formData.append("file",$("#file")[0].files[0]);
	var url = "/rest/api/svn/import";
	$.ajax({
		type : "POST",
		url : url,
		data: formData,
		processData: false,
		contentType: false,
		enctype:'multipart/form-data',
		success : function() {
			alert("submitted")
		},
		failure : function(e) {
			alert(e);

		}
	});
}