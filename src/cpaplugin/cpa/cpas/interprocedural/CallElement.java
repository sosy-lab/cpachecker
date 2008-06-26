package cpaplugin.cpa.cpas.interprocedural;

public class CallElement {
	
	private String functionName;
	private int callerLine;
	
	public CallElement( ) {
		super();
	}
	
	public CallElement(String functionName, int callerLine) {
		super();
		this.functionName = functionName;
		this.callerLine = callerLine;
	}

	public String getFunctionName() {
		return functionName;
	}

	public void setFunctionName(String functionName) {
		this.functionName = functionName;
	}

	public int getCallerLine() {
		return callerLine;
	}

	public void setCallerLine(int callerLine) {
		this.callerLine = callerLine;
	}
	
	public boolean equals(Object e){
		CallElement element = (CallElement) e;
		if(element.getCallerLine() == this.callerLine){
			if(element.getFunctionName().compareTo(this.functionName) == 0){
				return true;
			}
		}
		return false;
	}
	
	public String toString(){
		return "Call Element: " + functionName + " @ line " + callerLine;
	}
}
