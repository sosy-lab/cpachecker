package cpaplugin.cpa.cpas.octagon;

import java.util.HashMap;
import java.util.Set;

import octagon.LibraryAccess;
import octagon.Octagon;
import octagon.Variable;
import cpaplugin.cpa.common.interfaces.AbstractElement;

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
	// mapping from variable name to its variable object
	private HashMap<String, Variable> variables;

	/**
	 * Class constructor creating a new octagon and an empty variables list.
	 */
	public OctElement(){
		oct = LibraryAccess.empty(1);
		this.variables = new HashMap<String, Variable>();
	}

	/**
	 * Class constructor with an existing octagon and map to variables.
	 * @param oct Octagon.
	 * @param variables Variables map.
	 */
	public OctElement(Octagon oct, HashMap<String, Variable> variables){
		this.oct = oct;
		this.variables = variables; 
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public OctElement clone(){
		HashMap<String, Variable> newMap = new HashMap<String, Variable>();
		Set<String> keys = variables.keySet();
		for(String key:keys){
			Variable var = variables.get(key);
			newMap.put(key, var);
		}
		return new OctElement(oct, newMap);
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
			Variable var = variables.get(i);
			String varName = var.getVariableName() + "@" + var.getFunctionName(); 
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
				Variable ivar = variables.get(i);
				String iVarName = ivar.getVariableName() + "@" + ivar.getFunctionName();
				Variable jvar = variables.get(j);
				String jVarName = jvar.getVariableName() + "@" + jvar.getFunctionName();
				
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
	 * Add a new variable to the list of variables.
	 * @param varName Name of the variable.
	 * @param funcName Name of the function.
	 */
	public void addVar(String varName, String funcName) {
		Variable variable = new Variable(varName, funcName, variables.size());
		variables.put(varName, variable);
	}

	/**
	 * Remove a variable from the list by its name.
	 * @param varName
	 */
	public void removeVar(String varName) {
		Variable var = getVariable(varName);
		variables.remove(var);
	}
	
	/**
	 * Retrieve a variable by its name.
	 * @param varName Name of the variable.
	 * @return the variable object.
	 */
	public Variable getVariable(String varName){
		return variables.get(varName);
	}

	/**
	 * Total number of variables.
	 * @return
	 */
	public int getNumberOfVars() {
		return variables.size();
	}

	/**
	 * Get the id of a variable by its name.
	 * @param varName Name of the variable.
	 * @return id of the variable.
	 */
	public int getVarId(String varName) {
		Variable var = getVariable(varName);
		Integer in = variables.get(var).getId();
		int i = in.intValue();
		return i;
	}

//	public boolean contains(String varName) {
//		return variables.containsKey(varName);
//	}

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
	public HashMap getMap(){
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
