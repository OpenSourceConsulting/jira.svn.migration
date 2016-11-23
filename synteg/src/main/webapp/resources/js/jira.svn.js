var CONTEXT_PATH = "/svninteg"
function getProjectList(selectId) {
	var select = $("#" + selectId);
	var url = CONTEXT_PATH + "/api/projects";
	var options = "<option value='0' selected>--Select project--</option>";
	$.ajax({
		url : url,
		success : function(json) {
			$.each(json.list, function(k, v) {
				options += "<option value='" + v.id + "'>" + v.key + " - "
						+ v.name + "</option>";
			})
			select.html(options);
		},
		error : function(e) {
			alert(e.msg);
		}
	});
}

function filterIssues(gridId, projectId, fromDate, toDate, fields) {
	var url = CONTEXT_PATH + "/api/issues/filter";
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
		var url = CONTEXT_PATH + "/api/svn/export";
		window.location.href = url + "?issueKeys[]=" + issueKeys.join(",");
	}
}

function generateSVNTree(treeId, callback) {
	var tree = $("#" + treeId);
	var url = CONTEXT_PATH + "/api/svn/tree";
	
	$.ajax({
		type : "GET",
		url : url,
		success : function(json) {
			var rootNode = '<li><span><i class="fa fa-lg fa-database"></i>&nbsp;'
					+ json.resource + '</span>';
			rootNode += "<span class='hidden'>" + json.url + "</span>";
			var childNodes = "<ul>"
			childNodes += listChildNodes(json.childNodes);
			childNodes += "</ul>";
			rootNode += childNodes;
			rootNode += "</li>";
			tree.html("<ul>" + rootNode + "</ul>");
			callback();
		},
		error: function(json){
			alert("Connection error: Could not access to SVN.\nPlease check your SVN configuration.");
		}
	});
}
function loadChildNodes(parentPath, callback) {
	var url = CONTEXT_PATH + "/api/svn/tree/load?parent=" + parentPath;
	$.get(url, function(json) {
		var childNodes = listChildNodes(json.list);
		callback(childNodes);
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
				+ v.lastChangedStr + "\n" + "Last Author:" + v.lastAuthor
				+ "\n" + "Revision:" + v.revision;
		;
		var cls = "fa fa-lg "
		if (v.type === "dir") {
			cls += "fa-folder";
			result += '<li><span title="' + tooltip
					+ '" expanded="false"><i  class="' + cls + '"></i>&nbsp;'
					+ v.resource + '\t' + v.revision + '</span>';
			result += "<span class='hidden'>" + v.url + "</span>";
			if (v.childNodes !== null) {
				result += "<ul>";
				result += listChildNodes(v.childNodes);
				result += "</ul>";
			}
		} else if (v.type === "file") {
			cls += "fa-file-code-o";
			result += '<li><span title="' + tooltip + '"><i class="' + cls
					+ '"></i>&nbsp;' + v.resource + '\t' + v.revision
					+ '</span>';
			result += "<span class='hidden'>" + v.url + "</span>";
		}

	});
	return result;
}

function submitImportForm() {
	var submit = false;
	var selectPathType = $("#selectedType").val();
	if (selectPathType === "file") {
		$("#message-dialog").attr("title", "Warning");
		$("#message-dialog")
				.text(
						"You are selecting a file on subversion. It will be deleted and replaced to the uploaded file.");
		$("#message-dialog").dialog({
			modal : true,
			buttons : {
				Ok : function() {
					submit = true;
					$(this).dialog("close");
					doSubmit();
				},
				Cancel : function() {
					submit = false;
					$(this).dialog("close");
				}
			}
		});
	} else {
		doSubmit();
	}
}

function doSubmit() {
	if ($("#selectedPath").val() === "") {
		alert("Please select path.");
		return;
	}
	if ($("#file").val() === "") {
		alert("Please select imported file.");
		return;
	}

	var formData = new FormData();
	formData.append("selectedPath", $("#selectedPath").val());
	formData.append("message", $("#message").val());
	formData.append("file", $("#file")[0].files[0]);
	formData.append("isExtract", $("#extractZip").is(":checked"));
	var url = CONTEXT_PATH + "/api/svn/import";
	Pace.track(function() { // for progress bar
		$.ajax({
			type : "POST",
			url : url,
			data : formData,
			processData : false,
			contentType : false,
			enctype : 'multipart/form-data',
			success : function(json) {
				if (json.success) {
					$("#message-dialog").attr("title", "Information");
					$("#message-dialog").text("Import files successfully.");
					$("#message-dialog").dialog({
						modal : true,
						buttons : {
							Ok : function() {
								$(this).dialog("close");
								window.location.reload();
							}
						}
					});
				}
			}
		});
	});
}

function addReloadAction(parentSelector) {
	$(parentSelector).find(".fa-folder").parent().css("cursor", "pointer");
	$(parentSelector).find('ul').attr('role', 'group');
	$(parentSelector).find(' ul > li').find('li:has(ul)').addClass('parent_li')
			.attr('role', 'treeitem');
	$(parentSelector).find('ul > li').on('click', function(e) {
		var selecteds = $(this).find("span.hidden");
		if (selecteds.length > 1) {
			return;
		}
		var selectedPath = $(selecteds).text();

		$("#selectedPath").val(selectedPath);
		if ($(this).find(' > span > i').hasClass("fa-file-code-o")) {
			$("#selectedType").val("file");
		} else {
			$("#selectedType").val("dir");
		}
		e.stopPropagation();
	});

	$(parentSelector).find(".fa-folder").parent().on("click", function() {
		var parentPath = $(this).parent().find("span.hidden").text();
		var parent = $(this).parent();
		var me = this;
		if (parent.find("ul[role='group']").length === 0) {
			loadChildNodes(parentPath, function(childNodes) {
				parent.append("<ul role='group'></ul>");
				parent.find("ul[role='group']").html(childNodes);
				addReloadAction(parent);
			});
			$(me).find(' > i').removeClass().addClass('fa fa-folder-open');
		}

	});

	$(parentSelector).find(".fa-folder-open").parent().on("click", function() {
		var parent = $(this).parent();
		if (parent.find("ul[role='group']").length > 0) {
			parent.find("ul[role='group']").detach();
			$(this).find(' > i').removeClass().addClass('fa fa-folder');
		}
	})

}