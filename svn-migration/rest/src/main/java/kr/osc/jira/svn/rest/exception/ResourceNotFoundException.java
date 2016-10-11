package kr.osc.jira.svn.rest.exception;

/**
 * <pre>
 * 
 * </pre>
 * 
 * @author Sang-cheon Park
 * @version 1.0
 */
public class ResourceNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ResourceNotFoundException(String exceptionMessage) {
		super(exceptionMessage);
	}

	public ResourceNotFoundException(String exceptionMessage, Throwable t) {
		super(exceptionMessage, t);
	}

	public ResourceNotFoundException(Throwable t) {
		super(t);
	}

}
// end of ResourceNotFoundException.java