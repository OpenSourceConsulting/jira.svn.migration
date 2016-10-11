package kr.osc.jira.svn.rest.exception;

public class ErrorInfo {
	public final String url;
	public final String ex;

	public ErrorInfo(String url, Exception ex) {
		this.url = url;
		this.ex = ex.getLocalizedMessage();
	}
}