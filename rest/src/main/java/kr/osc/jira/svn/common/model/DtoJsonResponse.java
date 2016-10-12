package kr.osc.jira.svn.common.model;

/**
 * @author Tran Ho
 * @version 1.0
 */
public class DtoJsonResponse extends SimpleJsonResponse {

	private Object data;

	public DtoJsonResponse() {
		// TODO Auto-generated constructor stub
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

}
// end of DtoJsonResponse.java