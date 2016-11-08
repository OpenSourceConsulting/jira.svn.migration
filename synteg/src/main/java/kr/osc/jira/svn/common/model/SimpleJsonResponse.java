package kr.osc.jira.svn.common.model;

/**
 * <pre>
 * 서버 C,U,D 작업 결과에 대한 JSON Response 클래스.
 * - ex) {"success": true, "msg":"Create sucess"}
 * - 특히 Extjs form.Panel 에서 submit() 함수 사용시 필수 사용해야함.
 * </pre>
 * @author Bong-Jin Kwon
 * @version 1.0
 */
public class SimpleJsonResponse {
	
	/**
	 * 서버 작업 성공여부
	 */
	private boolean success = true;
	
	/**
	 * 서버 작업 결과 메시지.
	 */
	private String msg;
	
	/**
	 * 부가 정보.
	 */
	private Object data;

	/**
	 * <pre>
	 * 
	 * </pre>
	 */
	public SimpleJsonResponse() {
		// TODO Auto-generated constructor stub
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

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

}
//end of SimpleJsonResponse.java