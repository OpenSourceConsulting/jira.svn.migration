package kr.osc.jira.svn.rest.models;

import java.util.List;

public class Issue {
	private int id;
	private String key;
	private String url;
	private String summary;
	private String created;
	private String updated;
	private String status;
	private List<Commit> commits;

	public Issue(int id, String key, String url, String summary, String created, String updated, String status, List<Commit> commits) {
		this.id = id;
		this.key = key;
		this.url = url;
		this.summary = summary;
		this.created = created;
		this.updated = updated;
		this.status = status;
		this.commits = commits;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getCreated() {
		return created;
	}

	public void setCreated(String created) {
		this.created = created;
	}

	public String getUpdated() {
		return updated;
	}

	public void setUpdated(String updated) {
		this.updated = updated;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public List<Commit> getCommits() {
		return commits;
	}

	public void setCommits(List<Commit> commits) {
		this.commits = commits;
	}

}
