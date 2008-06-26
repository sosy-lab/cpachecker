package cpaplugin.cpa.cpas.octagon;

public class Variable {
	
	private String variableName;
	// in which function did we create the variable
	private String functionName;
	
	public Variable(String variableName, String functionName) {
		super();
		this.variableName = variableName;
		this.functionName = functionName;
	}
	public String getVariableName() {
		return variableName;
	}
	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}
	public String getFunctionName() {
		return functionName;
	}
	public void setFunctionName(String functionName) {
		this.functionName = functionName;
	}
}
