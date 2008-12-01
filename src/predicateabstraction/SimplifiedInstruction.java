package predicateabstraction;

public class SimplifiedInstruction {

	private String leftVariable;
	private String rightVariable;
	private Operator operator;

	public SimplifiedInstruction(String leftVariable, String rightVariable,
			Operator operator) {
		super();
		this.leftVariable = leftVariable;
		this.rightVariable = rightVariable;
		this.operator = operator;
	}

	public SimplifiedInstruction() {
		// TODO Auto-generated constructor stub
	}
	public String getLeftVariable() {
		return leftVariable;
	}
	public String getRightVariable() {
		return rightVariable;
	}
	public Operator getOperator() {
		return operator;
	}

}
