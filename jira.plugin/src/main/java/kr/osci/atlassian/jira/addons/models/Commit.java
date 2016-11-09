package kr.osci.atlassian.jira.addons.models;

import java.io.Serializable;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonAutoDetect
public class Commit {
	@JsonProperty
	private int revision;
	@JsonProperty
	private String key;
	@JsonProperty
	private String author;
	@JsonProperty
	private String message;
	@JsonProperty
	private String project;
	@JsonProperty
	private String repository;

	public Commit(int revision, String key, String author, String message, String project, String repostiory) {
		this.setKey(key);
		this.revision = revision;
		this.author = author;
		this.message = message;
		this.project = project;
		this.repository = repostiory;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public int getRevision() {
		return revision;
	}

	public void setRevision(int revision) {
		this.revision = revision;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public String getRepository() {
		return repository;
	}

	public void setRepository(String repository) {
		this.repository = repository;
	}
}
