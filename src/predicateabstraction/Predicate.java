package predicateabstraction;

import java.io.IOException;


public class Predicate {

	private String firstVariable;
	private Operator operator;
	private String secondVariable;
	private ThreeValuedBoolean truthValue;

	public Predicate(String leftvariable, Operator op, String rightVariable){
		firstVariable = leftvariable;
		operator = op;
		secondVariable = rightVariable;
		truthValue = ThreeValuedBoolean.DONTKNOW;
	}

	public Predicate(Predicate predicate) {
		firstVariable = predicate.getFirstVariable();
		operator = predicate.getOperator();
		secondVariable = predicate.getSecondVariable();
		truthValue = predicate.getTruthValue();
	}

	public void setTruthValue(ThreeValuedBoolean value){
		truthValue = value;
	}

	public String getFirstVariable(){
		return firstVariable;
	}

	public String getSecondVariable(){
		return secondVariable;
	}

	public Operator getOperator(){
		return operator;
	}

	public ThreeValuedBoolean getTruthValue(){
		return truthValue;
	}

	public String getPredicateAsQuery(){

		String res = "";

		if(operator == Operator.equals || operator == Operator.smallarOrEqual ||
				operator == Operator.notEquals){
			res =  operator + " " + firstVariable + " " +  secondVariable;
		}
		else if(operator == Operator.largerOrEqual){
			res = "<= " + secondVariable + " " +  firstVariable;
		}
		else if(operator == Operator.larger){
			res = "& [ " + "<= " + secondVariable + " " +  firstVariable +
			" ~ = " + secondVariable + " " +  firstVariable + " ]";
		}
		else if(operator == Operator.smaller){
			res = "& [ " + "<= " + firstVariable + " " +  secondVariable +
			" ~= " + secondVariable + " " +  firstVariable + " ]";
		}
		else {
			assert(false);
			// TODO exception
			System.exit(0);
			res = "";
		}

		if(truthValue == ThreeValuedBoolean.TRUE){
			return res;
		}
		else if(truthValue == ThreeValuedBoolean.FALSE){
			return "~ " + res;
		}
		else{
			assert(false);
			// TODO is that true? normally this should never be executed
			return "";
		}
	}
	
	public String getPredicateAsString(){
		String res = "";

		if(operator == Operator.equals || operator == Operator.smallarOrEqual ||
				operator == Operator.notEquals){
			res =  operator + " " + firstVariable + " " +  secondVariable;
		}
		else if(operator == Operator.largerOrEqual){
			res = "<= " + secondVariable + " " +  firstVariable;
		}
		else if(operator == Operator.larger){
			res = " ~= " + secondVariable + " " +  firstVariable + " ]";
		}
		else if(operator == Operator.smaller){
			res = "& [ " + "<= " + firstVariable + " " +  secondVariable +
			" ~= " + secondVariable + " " +  firstVariable + " ]";
		}
		else {
			assert(false);
			// TODO exception
			System.exit(0);
			res = "";
		}
		
		return res;
		
	}

	public boolean samePredicate(Predicate other){
		if(!(firstVariable.compareTo(other.getFirstVariable())==0))
			return false;
		if(!(secondVariable.compareTo(other.getSecondVariable())==0))
			return false;
		if(operator != other.getOperator())
			return false;
		return true;
	}


	public String WPAssignment(String leftVar, String rigthVar, Operator op, boolean negated){
		String s1 = "";
		String s2 = "";
		String wp = "";

		assert(op == Operator.equals);

		if(leftVar.compareTo(this.firstVariable)==0){
			s1 = rigthVar;
		}
		else{
			s1 = this.firstVariable;
		}
		if(leftVar.compareTo(this.secondVariable)==0){
			s2 = rigthVar;
		}
		else{
			s2 = this.secondVariable;
		}

		if(operator == Operator.equals || operator == Operator.smallarOrEqual ||
				operator == Operator.notEquals){
			wp = operator + " " + s1 + " " +  s2;
		}
		else if(operator == Operator.largerOrEqual){
			wp = "<= " + s2 + " " +  s1;
		}
		else if(operator == Operator.larger){
			wp = "& [ " + "<= " + s2 + " " +  s1 +
			" ~= " + s2 + " " +  s1 + " ]";
		}
		else if(operator == Operator.smaller){
			wp = "& [ " + "<= " + s1 + " " +  s2 +
			" ~= " + s1 + " " +  s2 + " ]";
		}
		else {
			assert(false);
			return null;
		}

		if(!negated)
			return wp;
		else
			return "~ " + wp;
	}

	public String WPAssumption(boolean negated){
		if(!negated)
			return getPredicateAsString();
		else
			return "~ " + getPredicateAsString();
	}


	public boolean equals(Object other){
		Predicate otherPredicate = (Predicate) other;
		if(this.firstVariable.compareTo(otherPredicate.getFirstVariable())!=0)
			return false;
		if(this.secondVariable.compareTo(otherPredicate.getSecondVariable())!=0)
			return false;
		if(this.operator != otherPredicate.getOperator())
			return false;
		if(this.truthValue != otherPredicate.getTruthValue())
			return false;
		return true;
	}

	public String toString(){
		return firstVariable + " " + operator + " " + secondVariable + ": " + truthValue ;
	}

	public void updateAssumption(String previousState, String instruction) throws IOException{

		String preCondition = "& [ " + previousState + " " + instruction + " ]"; 
		String postCondition = WPAssumption(false);

		if (TheoremProverInterface.implies(preCondition, postCondition) == ThreeValuedBoolean.TRUE){
			setTruthValue(ThreeValuedBoolean.TRUE);
			return;
		}

		postCondition = WPAssumption(true);

		if (TheoremProverInterface.implies(preCondition, postCondition) == ThreeValuedBoolean.TRUE){
			setTruthValue(ThreeValuedBoolean.FALSE);
			return;
		}
		
		setTruthValue(ThreeValuedBoolean.DONTKNOW);
	}

	public void updateAssignment(String previousState, String leftVar, String rightVar, Operator op) throws IOException{

		String postCondition = WPAssignment(leftVar, rightVar, op, false);
		
		if(postCondition.contains("__________cpa_________unknownVal___")){
			setTruthValue(ThreeValuedBoolean.DONTKNOW);
			return;
		}
		
		if (TheoremProverInterface.implies(previousState, postCondition) == ThreeValuedBoolean.TRUE){
			setTruthValue(ThreeValuedBoolean.TRUE);
			return;
		}

		postCondition = WPAssignment(leftVar, rightVar, op, true);

		if (TheoremProverInterface.implies(previousState, postCondition) == ThreeValuedBoolean.TRUE){
			setTruthValue(ThreeValuedBoolean.FALSE);
			return;
		}
		
		else{
			setTruthValue(ThreeValuedBoolean.DONTKNOW);
		}
	}
	
	public void updateFunctionCall(String previousState, String parameterAssignment) throws IOException{

		String postCondition = "& [ " + parameterAssignment +  " " + getPredicateAsString() + " ]"; 

		if (TheoremProverInterface.satisfiability("& [ " + previousState +  " " + postCondition + " ]") == ThreeValuedBoolean.TRUE){
			setTruthValue(ThreeValuedBoolean.TRUE);
			return;
		}

		postCondition = "& [ " + parameterAssignment +  " ~ " + getPredicateAsString() + " ]"; 
		
		if (TheoremProverInterface.satisfiability("& [ " + previousState +  " " + postCondition + " ]") == ThreeValuedBoolean.TRUE){
			setTruthValue(ThreeValuedBoolean.FALSE);
			return;
		}
		
		setTruthValue(ThreeValuedBoolean.DONTKNOW);
	}
	
	public void updateFunctionReturn(String query) {
		
		String queryPos = " & [ " + getPredicateAsString() + " " + query + " ] ";

		if (TheoremProverInterface.satisfiability(queryPos) == ThreeValuedBoolean.TRUE){
			System.out.println(queryPos);
			setTruthValue(ThreeValuedBoolean.TRUE);
			return;
		}
		
		String queryNeg = " & [ ~ " + getPredicateAsString() + " " + query + " ] ";

		if (TheoremProverInterface.satisfiability(queryNeg) == ThreeValuedBoolean.TRUE){
			System.out.println(queryNeg);
			setTruthValue(ThreeValuedBoolean.FALSE);
			return;
		}
		setTruthValue(ThreeValuedBoolean.DONTKNOW);
	}

	public boolean containsVariable(String modifiedVariableName) {
		return firstVariable.equals(modifiedVariableName) || secondVariable.equals(modifiedVariableName);
	}
	
	public Predicate clone(){
		return new Predicate(this);
	}
}
