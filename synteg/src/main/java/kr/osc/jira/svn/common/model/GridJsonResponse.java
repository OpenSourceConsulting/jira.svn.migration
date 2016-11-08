package kr.osc.jira.svn.common.model;

import java.util.List;

/**
 * <pre>
 * Extjs Grid에 의한 서버 list 조회 작업 결과에 대한 JSON Response 클래스.
 * - ex) {"total": 30, "list":[{....},{....}]}
 * </pre>
 * @author Bong-Jin Kwon
 * @version 1.0
 */
public class GridJsonResponse {
	
	/**
	 * 서버 작업 성공여부
	 */
	private boolean success = true;
	
	/**
	 * 서버 작업 결과 메시지.
	 */
	private String msg;
	
	private int total;
	private List<?> list;

	/**
	 * <pre>
	 * 
	 * </pre>
	 */
	public GridJsonResponse() {
		// TODO Auto-generated constructor stub
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public List<?> getList() {
		return list;
	}

	public void setList(List<?> list) {
		this.list = list;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}
	
	

}
//end of GridJsonResponse.java