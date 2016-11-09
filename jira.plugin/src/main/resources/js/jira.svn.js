function retrieve(url, callback) {
	jQuery.get(url, function(json) {
		callback(json);
	})
}

function insertData(selectBox, arr) {
	var options = "";
	for (i = 0; i < arr.length; i++) {
		p = arr[i];
		options += "<option value='" + p.key + "'>" + p.name + "</option>";
	}
	selectBox.html(options);
}