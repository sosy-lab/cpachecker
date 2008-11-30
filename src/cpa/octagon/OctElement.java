package cpa.octagon;

import java.util.HashMap;
import java.util.Set;

import octagon.LibraryAccess;
import octagon.OctWrapper;
import octagon.Octagon;
import cpa.common.interfaces.AbstractElement;

/**
 * An element of octagon abstract domain. This element contains an {@link Octagon} which 
 * is the concrete representation of the octagon and a map which
 * provides a mapping from variable names to variables. 
 * see {@link Variable}.
 * 
 * @author erkan
 *
 */
public class OctElement implements AbstractElement{

	private Octagon oct;
	// mapping from variable name to its identifier
	private HashMap<String, Integer> variables;

	/**
	 * Class constructor creating a new octagon and an empty variables list.
	 */
	public OctElement(){
		oct = LibraryAccess.universe(0);
		this.variables = new HashMap<String, Integer>();
	}

	/**
	 * Class constructor with an existing octagon and map to variables.
	 * @param oct Octagon.
	 * @param variables Variables map.
	 */
	public OctElement(Octagon oct, HashMap<String, Integer> variables){
		this.oct = oct;
		this.variables = variables; 
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public OctElement clone(){
		HashMap<String, Integer> newMap = new HashMap<String, Integer>();
		Set<String> keys = variables.keySet();
		for(String key:keys){
			Integer id = variables.get(key);
			newMap.put(key, id);
		}
		return new OctElement(oct.clone(), newMap);
	}

	/**
	 * Update the element with a new octagon element. This method is accessed frequently by {@link OctTransferRelation}
	 * to update the current region.
	 * @param ent
	 */
	public void update(OctElement ent){
		this.oct = ent.oct;
		this.variables = ent.variables;
	}

	@Override
	public boolean equals (Object other)
	{
		if (this == other)
			return true;

		if (!(other instanceof OctElement))
			return false;

		OctElement otherOctagon = (OctElement) other;
		// we check for equality on native library because it optimizes octagons first and
		// check for equality
		return LibraryAccess.isEqual(this, otherOctagon);
	}

	public String getVarNameForId(int i){
		for(String varName:variables.keySet()){
			if(variables.get(varName) == i){
				return varName;
			}
		}
		assert(false);
		return null;
	}

	@Override
	public String toString() {

		String s = "";

		if (oct.getState() == 0){
			s = s + "[ empty ] \n";
			return s;
		}

		if (oct.getState() == 2){
			s = s + " [ closed ]";
		}

		for (int i=0; i<oct.getDimension(); i++) {
			String varName = getVarNameForId(i);
			if (oct.getMatrix()[oct.matPos(2*i,2*i)].f > 0) {
				s = s + "\n  " + varName + "-" + varName + " <= " + oct.getMatrix()[oct.matPos(2*i, 2*i)].f;
			}
			if (oct.getMatrix()[oct.matPos(2*i+1,2*i+1)].f > 0) {
				s = s + "\n  " + "-"+ varName + "+" + varName + " <= " + oct.getMatrix()[oct.matPos(2*i+1,2*i+1)].f;
			} 
			if ((oct.getMatrix()[oct.matPos(2*i+1,2*i)].f != Double.NEGATIVE_INFINITY) &&
					(oct.getMatrix()[oct.matPos(2*i+1,2*i)].f != Double.POSITIVE_INFINITY)) {
				s = s + "\n  " + varName + " <= " + (oct.getMatrix()[oct.matPos(2*i+1,2*i)].f)/2;
			}
			if ((oct.getMatrix()[oct.matPos(2*i,2*i+1)].f != Double.NEGATIVE_INFINITY) &&
					(oct.getMatrix()[oct.matPos(2*i,2*i+1)].f != Double.POSITIVE_INFINITY)) {
				s = s + "\n  " + "-" + varName + " <= " + (oct.getMatrix()[oct.matPos(2*i,2*i+1)].f)/2;
			}
		}

		for (int i=0; i<oct.getDimension(); i++){
			for (int j=i+1; j<oct.getDimension(); j++) {
				String iVarName = getVarNameForId(i);
				String jVarName = getVarNameForId(j);

				if((oct.getMatrix()[oct.matPos(2*j,2*i)].f != Double.NEGATIVE_INFINITY) &&
						(oct.getMatrix()[oct.matPos(2*j,2*i)].f != Double.POSITIVE_INFINITY)){
					s = s + "\n  " + iVarName + "-" + jVarName +" <= " + (oct.getMatrix()[oct.matPos(2*j,2*i)].f);
				}
				// 2*j,2*i+1
				if ((oct.getMatrix()[oct.matPos(2*j,2*i+1)].f != Double.NEGATIVE_INFINITY) &&
						(oct.getMatrix()[oct.matPos(2*j,2*i+1)].f != Double.POSITIVE_INFINITY)) {
					s = s + "\n  " + "-" + iVarName + "-" + jVarName +" <= " + (oct.getMatrix()[oct.matPos(2*j,2*i+1)].f);
				}
				// 2*j+1,2*i
				if ((oct.getMatrix()[oct.matPos(2*j+1,2*i)].f != Double.NEGATIVE_INFINITY) &&
						(oct.getMatrix()[oct.matPos(2*j+1,2*i)].f != Double.POSITIVE_INFINITY)) {
					s = s + "\n  " + iVarName + "+" + jVarName +" <= " + (oct.getMatrix()[oct.matPos(2*j+1,2*i)]);
				}
				// 2*j+1,2*i+1
				if ((oct.getMatrix()[oct.matPos(2*j+1,2*i+1)].f != Double.NEGATIVE_INFINITY) &&
						(oct.getMatrix()[oct.matPos(2*j+1,2*i+1)].f != Double.POSITIVE_INFINITY)){
					s = s + "\n  " + jVarName + "-" + iVarName +" <= " + (oct.getMatrix()[oct.matPos(2*j+1,2*i+1)]);
				}
			}
		}
		s = s + "\n";
		return s;

	}

	/**
	 * Adds a new variable in form of functionName::variableName. For example if a 
	 * new variable a is declared in function main(), main::a is added to the
	 * list of variables. After this operation you have to increse octagon
	 * dimension manually!
	 * @param varName Name of the variable.
	 * @param funcName Name of the function that contains the variable
	 * @return true if variable is added succesfully
	 */
	public boolean addVar(String varName, String funcName) {
		String variableName = funcName + "::" + varName;
		// add if the variable is not already in the variables set
		if(!variables.containsKey(variableName)){
			variables.put(variableName, getNumberOfVars());
			return true;
		}
		return false;
	}

	/**
	 * Remove a variable from the list by its variable id.
	 * @param varId Variable id.
	 */
	public void removeVar(int varId, int numOfDims) {
		// TODO
		OctWrapper ow = new OctWrapper();
		oct = ow.J_removeDimensionAtPosition(oct, varId, numOfDims, true);
		variables.remove(getVarNameForId(varId));
	}

	/**
	 * Retrieve the variable's id by its name.
	 * @param globalVars List of global variables
	 * @param varName Name of the variable.
	 * @param functionName Name of the function.
	 * @return id of the variable
	 */
	public int getVariableId(Set<String> globalVars, String varName, String functionName){
		String variableName = "";
		if(globalVars.contains(varName)){
			variableName = "::" + varName;
		}
		else{
			variableName = functionName + "::" + varName;
		}
		return variables.get(variableName);
	}

	/**
	 * Total number of variables.
	 * @return
	 */
	public int getNumberOfVars() {
//		assert(variables.size() == oct.getDimension());
		return variables.size();
	}

	/** Is octagon empty?
	 * @return true if octagon is empty, false o.w.
	 */
	public boolean isEmpty(){
		return LibraryAccess.isEmpty(this);
	}

	/**
	 * Retrieves the octagon of this element.
	 * @return the octagon.
	 */
	public Octagon getOctagon(){
		return oct;
	}

	/**
	 * Retrieve the map of variables.
	 * @return map of variables.
	 */
	public HashMap<String, Integer> getVariableMap(){
		return variables;
	}

	/**
	 * Asks if the variable list contains a variable.
	 * @param varName Name of the variable.
	 * @return true if the list contains variable, false o.w.
	 */
	public boolean contains(String varName) {
		return variables.containsKey(varName);
	}
	
	public void changeVarId(String varName, int id){
		variables.remove(varName);
		variables.put(varName, id);
	}
	
	public void changeVarName(String formerName, String newName){
		//sdf
	}

	public void setVariableMap(HashMap<String, Integer> newVariablesMap) {
		variables = newVariablesMap;
		
	}

	// TODO fix this
//	public void addVariablesFrom(OctElement octEl1) {
//	VariableMap oct1Map = octEl1.getVarMap();
//	for(int i=0; i<oct1Map.getSize(); i++){
//	String s = oct1Map.getVarName(i);
//	if(!this.variables.contains(s)){
//	this.update(LibraryAccess.addDimension(this, 1));
//	this.addVar(s);
//	}
//	}
//	}

}
