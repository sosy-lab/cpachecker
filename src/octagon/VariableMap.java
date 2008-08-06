package octagon;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Keeps a mapping from variable to variable IDs used in octagon domain. In
 * octagon abstract domain, we need to know what is the id of a variable to 
 * manipulate octagons. Each function has a VariableMap that maps its variables
 * in its scope to unique IDs for variables.
 * @author erkan
 *
 */
/**
 * @author erkan
 *
 */
public class VariableMap {

	/** total number of variables in the variable map, this may be required
	 * to keep an independent counter on variables*/
	private int numOfVars;
	/** variable-id map*/
	private Hashtable<Variable, Integer> varMap;

	public VariableMap(int i){
		this.numOfVars = i;
		varMap =  new Hashtable<Variable, Integer> ();
	}

	/**
	 * Add variable to the variable map. Do not forget to increment the number
	 * of variables after adding the variable.
	 * @param variableName Name of the variable.
	 */
	protected void addVar(String variableName){
		Variable variable = new Variable(variableName);
		varMap.put(variable, new Integer(numOfVars));
		numOfVars++;
	}

	/**
	 * Remove variable with the given name from the map.
	 * @param varname Name of the variable.
	 */
	protected void removeVar(String varname) {
		Variable var = getVariable(varname);
		varMap.remove(var);
		numOfVars--;
	}

	/**
	 * Get id of the variable with the given name.
	 * @param varname Name of the variable.
	 * @return id of the variable.
	 */
	protected int getVarId(String varname){
		Variable var = getVariable(varname);
		Integer in = varMap.get(var);
		int i = in.intValue();
		return i;
	}

	/**
	 * Get name of the variable with the given id.
	 * @param id id of the variable.
	 * @return Name of the variable.
	 */
	protected String getVarName(int id){
		Enumeration<Variable> keysCol = varMap.keys();
		while(keysCol.hasMoreElements()){
			Variable var = keysCol.nextElement();
			if(getVarId(var.getVariableName()) == id){
				return var.getVariableName();
			}
		}
		return null;
	}

	/**
	 * Get variable that wraps the variable with name varName.
	 * @param varName Name of the variable.
	 * @return 
	 */
	private Variable getVariable(String varName){
		Enumeration<Variable> keysCol = varMap.keys();
		while(keysCol.hasMoreElements()){
			Variable var = keysCol.nextElement();
			if(var.getVariableName().equals(varName)){
				return var;
			}
		}
		return null;
	}

	/**
	 * Size of the variable map.
	 * @return
	 */
	protected int getSize() {
		return numOfVars;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	protected VariableMap clone(){
		
		VariableMap newmap = new VariableMap(numOfVars);
		
		Hashtable<Variable, Integer> newList = new Hashtable<Variable, Integer>();
		Enumeration<Integer> elements = varMap.elements();
		Enumeration<Variable> keys = varMap.keys();
		while(keys.hasMoreElements()){
			Variable var = keys.nextElement();
			Integer inVal = elements.nextElement();
			newList.put(var, inVal);
		}
		
		this.varMap = newList;
		return newmap;
	}

	/**
	 * Query to ask if the mao contains a variable with the given name.
	 * @param varName Name of the variable.
	 * @return true if the map contains varName, false o.w.
	 */
	protected boolean contains(String varName) {
		Enumeration<Variable> keysCol = varMap.keys();
		while(keysCol.hasMoreElements()){
			Variable var = keysCol.nextElement();
			if(var.getVariableName().equals(varName)){
				return true;
			}
		}
		return false;
	}
}
