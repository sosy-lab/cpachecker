package octagon;

/**
 * Represents a variable in the program. A variable is represented by its functionname, its unique id
 * in its scope and its name.
 * @author erkan
 *
 */
public class Variable {
	
	private String variableName;
	private String functionName;
	private int id;
	
	public Variable(String variableName, String functionName, int varId) {
		this.variableName = variableName;
		this.functionName = functionName;
		this.id = varId;
	}
	
	public String getVariableName() {
		return variableName;
	}
	
	public String getFunctionName(){
		return this.functionName;
	}
	
	public int getId(){
		return id;
	}
}
