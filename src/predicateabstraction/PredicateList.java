package predicateabstraction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PredicateList {

	private List<Predicate> predicates;

	public PredicateList(){
		predicates = new ArrayList<Predicate>();
	}

	public void addPredicate(Predicate p){
		predicates.add(p);
	}

	public List<Predicate> getPredicates(){
		return predicates;
	}

	public Predicate getPredicate(String var1, Operator op, String var2){
		for(Predicate pred:this.getPredicates()){
			if(pred.getFirstVariable().compareTo(var1)==0 &&
					pred.getOperator() == op &&
					pred.getSecondVariable().compareTo(var2)==0){
				return pred;
			}
		}
		assert(false);
		return null;
	}

	public Predicate getPredicate(Predicate pred){
		return getPredicate(pred.getFirstVariable(), pred.getOperator(), pred.getSecondVariable());
	}

	public int size(){
		return predicates.size();
	}

	public PredicateList clone(){
		PredicateList newPl = new PredicateList();
		for(Predicate predicate: predicates){
			Predicate p= new Predicate(predicate);
			newPl.addPredicate(p);
		}
		return newPl;
	}

	public boolean equals(Object other){
		if (this == other)
			return true;

		if (!(other instanceof PredicateList))
			return false;
		PredicateList otherPredicatelist = (PredicateList) other;
		if(otherPredicatelist.size() != this.size()){
			return false;
		}
		List<Predicate> predList = otherPredicatelist.getPredicates();
		for(Predicate predicate:predList){
			if(!predicates.contains(predicate)){
				return false;
			}
		}
		return true;
	}

	public String getRegion() {
		String s = "& [ "; //= S1 1 ~ = S2 0 = S2 0 ]
		for(Predicate predicate:predicates){
			if(predicate.getTruthValue() != ThreeValuedBoolean.DONTKNOW){
				s = s + predicate.getPredicateAsQuery() + " ";
			}
		}
		s = s + " ]";
		
//		if(s.compareTo("& [  ]") == 0){
//			s = "true";
//		}
		
		return s;
	}

	public void updateAssignment(String previousState, String leftVar, String rigthVar, Operator op) throws IOException
	{
		for(Predicate predicate:predicates){
			predicate.updateAssignment(previousState, leftVar, rigthVar, op);
		}
	}

	public void updateAssumption(String previousState, String instruction) throws IOException
	{
		for(Predicate predicate:predicates){
			predicate.updateAssumption(previousState, instruction);
		}
	}
	
	public String toString(){
		String s = "";
		for(Predicate predicate:predicates){
			s = s + predicate.getPredicateAsQuery() + " ";
		}
		return s;
	}
	
}