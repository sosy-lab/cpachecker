package octagon;

import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.cpas.octagon.OctElement;

public class LibraryAccess {
	
	public static OctWrapper or = new OctWrapper();
	
	// widening operator, used for joining
	public static OctElement widening(
			OctElement o1, OctElement o2){
		Octagon oct1 = o1.getOctagon();
		Octagon oct2 = o2.getOctagon();
		Octagon res = or.J_widening(oct1, oct2, true, 0);
		return new OctElement(res, o1.getVariableMap());
	}
	
	public static int getDim(OctElement octElm){
		Octagon oct = octElm.getOctagon();
		int i = or.J_dimension(oct);
		return i;
	}
	
	// union operator, used for joining
	public static OctElement union(
			OctElement o1, OctElement o2){
		Octagon oct1 = o1.getOctagon();
		Octagon oct2 = o2.getOctagon();
		Octagon res = or.J_union(oct1, oct2, true);
		return new OctElement(res, o1.getVariableMap());
	}
	
	// creating an empty octagon, considered as the bottom element
	public static Octagon empty(int numOfVars) {
		Octagon res = or.J_empty(numOfVars);
		return res;
	}
	
	// returns true if empty (used for checking if element is bottom element)
	public static boolean isEmpty(AbstractElement element1) {
		Octagon oct = ((OctElement)element1).getOctagon();
		return or.J_isEmpty(oct);
	}
	
	// creating a full octagon ov numOfVars dimensions, considered as the top element
	public static Octagon universe(int numOfVars) {
		Octagon res = or.J_universe(numOfVars);
		return res;
	}

	public static boolean isIn(AbstractElement element1,
			AbstractElement element2) {
		Octagon oct1 = ((OctElement)element1).getOctagon();
		Octagon oct2 = ((OctElement)element2).getOctagon();
		
		return or.J_isIncludedIn(oct1, oct2);
		
	}
	
	public static boolean isEqual(AbstractElement element1,
			AbstractElement element2) {
		Octagon oct1 = ((OctElement)element1).getOctagon();
		Octagon oct2 = ((OctElement)element2).getOctagon();
		
		return or.J_isEqual(oct1, oct2);
		
	}

	public static OctElement forget(OctElement elem, int var) {
		Octagon oct1 = elem.getOctagon();
		Octagon res = or.J_forget(oct1, var, true);
		return new OctElement(res, elem.getVariableMap());
	}

	public static OctElement assignVar(OctElement octElement, int var,
			Num[] array) {
		Octagon oct = octElement.getOctagon();
		Octagon res = or.J_assingVar(oct, var, array, true);
		return new OctElement(res, octElement.getVariableMap());
	}

	public static OctElement addDimension(OctElement octElement, int numOfVars) {
		Octagon oct = octElement.getOctagon();
		Octagon res = or.J_addDimenensionAndEmbed(oct, numOfVars, true);
		return new OctElement(res, octElement.getVariableMap());
	}
	
	public static OctElement removeDimension(OctElement octElement, int numOfVars) {
		Octagon oct = octElement.getOctagon();
		Octagon res = or.J_removeDimension(oct, numOfVars, true);
		return new OctElement(res, octElement.getVariableMap());
	}
	
	public static OctElement removeDimensionAtPosition(OctElement octElement, int variablePos, int numOfDims){
		Octagon oct = octElement.getOctagon();
		Octagon res = or.J_removeDimensionAtPosition(oct, variablePos, numOfDims, true);
		return new OctElement(res, octElement.getVariableMap());
	}
	
	public static OctElement addConstraint(OctElement octElement,
			Num[] array) {
		Octagon oct = octElement.getOctagon();
		Octagon res = or.J_addConstraint(oct, array, true);
		return new OctElement(res, octElement.getVariableMap());
	}
	
}
