var CONTEXT_PATH = "/svninteg"
function getProjectList(selectId) {
	var select = $("#" + selectId);
	var url = CONTEXT_PATH + "/api/projects";
	var options = "<option value='0' selected>--Select project--</option>";
	$.ajax({
		url : url,
		success : function(json) {
			if (json.success) {
				$.each(json.list, function(k, v) {
					options += "<option value='" + v.id + "'>" + v.key + " - "
							+ v.name + "</option>";
				})
				select.html(options);
			} else {
				alert(json.msg);
			}
		},
		error : function(e) {
			alert(e.msg);
		}
	});
}
function getStatusList(statusDivId) {
	var statusDiv = $("#" + statusDivId);
	var url = CONTEXT_PATH + "/api/statuses";
	$
			.ajax({
				url : url,
				success : function(json) {
					if (json.success) {
						var checkboxes = "";
						$
								.each(
										json.list,
										function(k, v) {
											checkboxes += "<label class='checkbox'><input type='checkbox' name='checkbox-inline[]' value="
													+ v.id
													+ "><i></i>"
													+ v.name + "</label>";
										});
						statusDiv.html(checkboxes);
					} else {
						alert(json.msg)
					}
				},
				error : function(e) {
					alert(e.msg);
				}
			});

}
function filterIssues(gridId, projectId, fromDate, toDate, statuses, fields) {
	var url = CONTEXT_PATH + "/api/issues/filter";
	var data = [];

	if (projectId == 0 || projectId === null) { // maybe 0 or "0"
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
		$.ajax({
			url : url,
			data : {
				"projectId" : projectId,
				"fromDate" : fromDate,
				"toDate" : toDate,
				"statuses[]" :statuses,
				"fields" : fields
			},
			success : function(json) {
				if (json.success) {
					$.each(json.list, function(k, v) {
						var commits = "";
						$.each(v.commits, function(key, value) {
							commits += "Revision:" + value.revision + "-["
									+ value.author + "] " + value.message
									+ "<br/>";
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
				} else {
					alert(json.msg);
				}
			},
			error : function(e) {
				alert(e.msg);
			}
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
	$
			.ajax({
				type : "GET",
				url : url,
				success : function(json) {
					if (json.success) {
						var rootNode = '<li><span><i class="fa fa-lg fa-database"></i>&nbsp;'
								+ json.data.resource + '</span>';
						rootNode += "<span class='hidden'>" + json.data.url
								+ "</span>";
						var childNodes = "<ul>"
						childNodes += listChildNodes(json.data.childNodes);
						childNodes += "</ul>";
						rootNode += childNodes;
						rootNode += "</li>";
						tree.html("<ul>" + rootNode + "</ul>");
						callback();
					} else {
						alert(json.msg);
					}
				},
				error : function(json) {
					alert(json.msg);
				}
			});
}
function loadChildNodes(parentPath, callback) {
	var url = CONTEXT_PATH + "/api/svn/tree/load?parent=" + parentPath;
	$.ajax({
		type : "GET",
		url : url,
		success : function(json) {
			if (json.success) {
				var childNodes = listChildNodes(json.list);
				callback(childNodes);
			} else {
				alert(json.msg);
			}
		},
		error : function(e) {
			alert(e.msg);
		}
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

function doSVNImport() {
	var url = CONTEXT_PATH + "/api/svn/import";
	Pace.track(function() { // for progress bar
		$.ajax({
			type : "POST",
			url : url,
			data : {
				"message" : $("#message").val(),
				"isMultipleFiles" : $("#extractZip").is(":checked")
			},
			success : function(json) {
				var dialog = null;
				if (json.success) {
					$("#message-dialog").attr("title", "Information");
					$("#message-dialog").text("Upload is done.");
					dialog = $("#message-dialog").dialog({
						modal : true,
						buttons : {
							Ok : function() {
								$(this).dialog("close");
								window.location.reload();
							}
						}
					});
				} else {
					alert(json.msg);
					$('.ui-dialog-content').dialog('close');
				}
			},
			error : function(e) {
				alert(e.msg);
				$('.ui-dialog-content').dialog('close');
			}
		});
	});
}

function doCheckDiff() {
	if (!validate()) {
		return;
	}
	var formData = new FormData();
	formData.append("selectedPath", $("#selectedPath").val());
	formData.append("file", $("#file")[0].files[0]);
	formData.append("fileLocation", $('input[name="file-location"]:checked')
			.val().trim());
	formData.append("serverFilePath", $("#input-file-server").val().trim());
	formData.append("isExtract", $("#extractZip").is(":checked"));
	var url = CONTEXT_PATH + "/api/svn/checkdiff";
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
					$("#log_dialog_content").text(json.data);
					$('#log_dialog').dialog({
						width : 700,
						height : 700,
						modal : true,
						title : "SVN Diff",
						buttons : [ {
							html : "Merge & Commit",
							"class" : "btn btn-primary",
							click : function() {
								doSVNImport();
							}
						}, {
							html : "<i class='fa fa-times'></i>&nbsp; Cancel",
							"class" : "btn btn-default",
							click : function() {
								$(this).dialog("close");
							}
						} ]
					});

				} else {
					alert(json.msg);
				}
			},
			error : function(e) {
				alert(e.msg);
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

function validate() {

	if ($("#selectedPath").val() === "" || $("#selectedType").val() !== "dir") {
		alert("Please select correct directory path.");
		return false;
	}
	if ($("#file").val() === ""
			&& $('input[name="file-location"]:checked').val() === "local") {
		alert("Please select imported file.");
		return false;
	}
	if ($("#input-file-server").val() === ""
			&& $('input[name="file-location"]:checked').val() === "server") {
		alert("Please enter your file location in server.");
		return false;
	}
	return true;
}
