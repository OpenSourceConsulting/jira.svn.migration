package kr.osc.jira.svn.rest.models;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SVNElement implements Comparable<SVNElement> {
	private String url;
	private String type;
	private String lastAuthor;
	private long revision;
	private String resource;
	private Date lastChanged;

	private List<SVNElement> childNodes;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getLastAuthor() {
		return lastAuthor;
	}

	public void setLastAuthor(String lastAuthor) {
		this.lastAuthor = lastAuthor;
	}

	public long getRevision() {
		return revision;
	}

	public void setRevision(long revision) {
		this.revision = revision;
	}

	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public Date getLastChanged() {
		return lastChanged;
	}

	public String getLastChangedStr() {
		if (lastChanged == null) {
			return "";
		}
		SimpleDateFormat formatter = new SimpleDateFormat(
				"yyyyy-mm-dd hh:mm:ss");
		return formatter.format(lastChanged);
	}

	public void setLastChanged(Date lastChanged) {
		this.lastChanged = lastChanged;
	}

	public List<SVNElement> getChildNodes() {
		return childNodes;
	}

	public void setChildNodes(List<SVNElement> childNodes) {
		this.childNodes = childNodes;
	}

	public void addChildNode(SVNElement child) {
		if (this.childNodes == null) {
			this.childNodes = new ArrayList<SVNElement>();
		}
		this.childNodes.add(child);
	}

	@Override
	public int compareTo(SVNElement o) {
		return this.getType().compareTo(o.getType());
	}
}
