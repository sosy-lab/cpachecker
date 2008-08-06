package octagon;

/**
 * Represents a variable in the program.
 * @author erkan
 *
 */
public class Variable {
	
	private String variableName;
	
	public Variable(String variableName) {
		this.variableName = variableName;
	}
	public String getVariableName() {
		return variableName;
	}
	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}
}
