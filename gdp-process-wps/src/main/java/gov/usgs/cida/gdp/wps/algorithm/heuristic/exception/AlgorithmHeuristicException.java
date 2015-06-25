package gov.usgs.cida.gdp.wps.algorithm.heuristic.exception;

public class AlgorithmHeuristicException extends Exception {
	private static final long serialVersionUID = 1467607385043208978L;

	private final AlgorithmHeuristicExceptionID exceptionid;	// uniquely generated id for this exception
	private final String classname; 							// the name of the class that threw the exception
	private final String method; 								// the name of the method that threw the exception
	private final String message; 								// a detailed message
	private AlgorithmHeuristicException previous = null;		// the exception which was caught
	private String delimeter = "\n"; 							// line separator
	
	public AlgorithmHeuristicException(final AlgorithmHeuristicExceptionID id, final String classname, final String method, final String message) {
		this.exceptionid	= id;
		this.classname  	= classname;
		this.method    		= method;
		this.message   		= message;
		this.previous  		= null;
	}

	public AlgorithmHeuristicException(final AlgorithmHeuristicExceptionID id, final String classname, final String method, final String message, final AlgorithmHeuristicException previous) {
		this.exceptionid	= id;
		this.classname  	= classname;
		this.method    		= method;
		this.message   		= message;
		this.previous  		= previous;
	}

	public String traceBack() {
		return traceBack("\n");
	}

	public String traceBack(final String sep) {
		this.delimeter = sep;
		int level = 0;
		AlgorithmHeuristicException e = this;
		final StringBuffer text = new StringBuffer(line("AlgorithmHeuristicException Trace: Calling sequence (top to bottom)"));
		while (e != null) {
			level++;
			text.append(this.delimeter);
			text.append(line("--level " + level + "--------------------------------------"));
			text.append(line("Class/Method: " + e.classname + "/" + e.method));
			text.append(line("Id          : " + e.exceptionid));
			text.append(line("Message     : " + e.message));
			e = e.previous;
		}
		return text.toString();
	}

	private String line(final String s) {
		return s + this.delimeter;
	}

	@Override
	public String getMessage() {
		return this.traceBack();
	}

	@Override
	public String toString() {
		return this.traceBack();
	}

	public AlgorithmHeuristicExceptionID getExceptionid() {
		return this.exceptionid;
	}

	public String getClassname() {
		return this.classname;
	}

	public String getMethod() {
		return this.method;
	}

	public AlgorithmHeuristicException getPrevious() {
		return this.previous;
	}

	public String getMessageOnly() {
		return this.message;
	}
}
