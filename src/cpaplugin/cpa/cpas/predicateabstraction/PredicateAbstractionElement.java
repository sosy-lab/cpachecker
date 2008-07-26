package cpaplugin.cpa.cpas.predicateabstraction;

import java.io.IOException;
import java.util.List;

import predicateabstraction.MathSatWrapper;
import predicateabstraction.Operator;
import predicateabstraction.Predicate;
import predicateabstraction.PredicateList;
import predicateabstraction.PredicateListConstructor;
import predicateabstraction.SimplifiedInstruction;
import predicateabstraction.ThreeValuedBoolean;
import cpaplugin.cpa.common.interfaces.AbstractElement;

public class PredicateAbstractionElement implements AbstractElement{

	private PredicateList predicateList;
	private boolean isFalsePredicate;

	public PredicateAbstractionElement(String functionName, String fileName){
		isFalsePredicate = false;
		predicateList = new PredicateList();
		PredicateListConstructor.constructList(predicateList, functionName, fileName);
	}

	public PredicateAbstractionElement(PredicateList pl, boolean isFalse){
		predicateList = pl;
		isFalsePredicate = isFalse;
	}

	public PredicateAbstractionElement clone(){
		if(!isFalsePredicate){
			PredicateList pl = predicateList.clone();
			return new PredicateAbstractionElement(pl, false);
		}
		return new PredicateAbstractionElement(null, true);
	}

	public boolean equals(Object other){
		if (this == other)
			return true;

		if (!(other instanceof PredicateAbstractionElement))
			return false;

		PredicateAbstractionElement otherPredicateElement = (PredicateAbstractionElement) other;

		if(otherPredicateElement.isFalsePredicate && this.isFalsePredicate){
			return true;
		}

		return predicateList.equals(otherPredicateElement.getPredicateList());
	}

	public PredicateList getPredicateList() {
		return predicateList;
	}

	public String getRegion(){
		if(isFalsePredicate){
			return "false";
		}
		return predicateList.getRegion();
	}

	public void updateAssignment(String previousState, SimplifiedInstruction simpIns) throws IOException
	{
		if(isFalsePredicate){
			return;
		}

		String leftVar = simpIns.getLeftVariable();
		String rightvar = simpIns.getRightVariable();
		Operator op = simpIns.getOperator();

		predicateList.updateAssignment(previousState, leftVar, rightvar, op);
	}

	public void updateFunctionCall(String previousState, String parameterAssignment) throws IOException
	{
		if(isFalsePredicate){
			return;
		}

		predicateList.updateFunctionCall(previousState, parameterAssignment);
	}

	public void updateFunctionReturn(String query) throws IOException {
		if(isFalsePredicate){
			return;
		}

		predicateList.updateFunctionReturn(query);
	}

	public void updateAssumption(String previousState, String instruction) throws IOException
	{
		if(isFalsePredicate){
			return;
		}

		if(!MathSatWrapper.satisfiability(" & [ " + previousState + " " + instruction + " ] ")){
			isFalsePredicate = true;
			predicateList = null;

		}
		else {
			predicateList.updateAssumption(previousState, instruction);
		}
	}

	public String toString(){
		if(isFalsePredicate){
			return "false";
		}
		return predicateList.toString();
	}

	public boolean isFalsePredicate(){
		return isFalsePredicate;
	}

	public void empty(){
		predicateList.emptyList();
	}

	public void addPredicates(PredicateAbstractionElement newElement) {
		for(Predicate pred:newElement.getPredicateList().getPredicates()){
			Predicate newPred = pred.clone();
			predicateList.addPredicate(newPred);
		}
	}

	public void addPredicateOnTheFly(Predicate pred){
		predicateList.addPredicate(pred);
	}

	public String getRegionWithoutVariable(List<String> modifiedVariables) {
		return predicateList.getRegionWithoutVariable(modifiedVariables);
	}
}
