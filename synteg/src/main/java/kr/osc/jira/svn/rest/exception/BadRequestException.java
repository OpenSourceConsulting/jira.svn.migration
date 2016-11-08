package kr.osc.jira.svn.rest.exception;


/**
 * <pre>
 * 
 * </pre>
 * @author Sang-cheon Park
 * @version 1.0
 */
public class BadRequestException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	public BadRequestException(String exceptionMessage) {
		super(exceptionMessage);
	}

	public BadRequestException(String exceptionMessage, Throwable t) {
		super(exceptionMessage, t);
	}

	public BadRequestException(Throwable t) {
		super(t);
	}

}
//end of BadRequestException.java