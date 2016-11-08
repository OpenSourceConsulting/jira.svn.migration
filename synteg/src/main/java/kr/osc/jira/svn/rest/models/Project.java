package kr.osc.jira.svn.rest.models;

public class Project {
	private String id;
	private String name;
	private String key;
	private String url;

	public Project(String id, String key, String name, String url) {
		this.id = id;
		this.setKey(key);
		this.name = name;
		this.url = url;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
}
