package cpaplugin.cpa.cpas.octagon;

import java.util.Enumeration;
import java.util.Hashtable;

public class VariableMap {
	
	private int numOfVars;
	private Hashtable<Variable, Integer> varList;
	
	public VariableMap(int i){
		this.numOfVars = i;
		varList =  new Hashtable<Variable, Integer> ();
	}
	
	public VariableMap(int i, Hashtable<Variable, Integer> vars){
		this.numOfVars = i;
		Hashtable<Variable, Integer> newList = new Hashtable<Variable, Integer>();
		Enumeration<Integer> elements = vars.elements();
		Enumeration<Variable> keys = vars.keys();
		while(keys.hasMoreElements()){
			Variable var = keys.nextElement();
			Integer inVal = elements.nextElement();
			newList.put(var, inVal);
		}
		varList = newList;
	}

	public void addVar(String varname, String functionname){
		Variable variable = new Variable(varname, functionname);
		varList.put(variable, new Integer(numOfVars));
		numOfVars++;
	}
	
	public void removeVar(String varname, String functionname) {
		Variable var = getVariable(varname, functionname);
		varList.remove(var);
		numOfVars--;
	}
	
	public int getVarId(String varname, String functionname){
		Variable var = getVariable(varname, functionname);
		Integer in = varList.get(var);
		int i = in.intValue();
		return i;
	}
	
	public String getVarName(int id){
		Enumeration<Variable> keysCol = varList.keys();
		while(keysCol.hasMoreElements()){
			Variable var = keysCol.nextElement();
			if(getVarId(var.getVariableName(), var.getFunctionName()) == id){
				return var.getVariableName();
			}
		}
		return null;
	}
	
	public Variable getVariable(String varName, String functionName){
		Enumeration<Variable> keysCol = varList.keys();
		while(keysCol.hasMoreElements()){
			Variable var = keysCol.nextElement();
			if(var.getVariableName().compareTo(varName) == 0 && 
					var.getFunctionName().compareTo(functionName) == 0){
				return var;
			}
		}
		return null;
	}

	public int getSize() {
		return numOfVars;
	}
	
	public VariableMap clone(){
		return new VariableMap(numOfVars, varList);
	}

	public boolean contains(String varName, String functionName) {
		Enumeration<Variable> keysCol = varList.keys();
		while(keysCol.hasMoreElements()){
			Variable var = keysCol.nextElement();
			if(var.getVariableName().compareTo(varName) == 0 && 
					var.getFunctionName().compareTo(functionName) == 0){
				return true;
			}
		}
		return false;
	}

	public int removeVariablesOfFunction(String fname) {
		int noOfRemovedVars = 0;
		Enumeration<Variable> keysCol = varList.keys();
		while(keysCol.hasMoreElements()){
			Variable var = keysCol.nextElement();
			if(var.getFunctionName().compareTo(fname) == 0){
				String varname = var.getVariableName();
				String functionname = var.getFunctionName();
				removeVar(varname, functionname);
				noOfRemovedVars++;
			}
		}
		return noOfRemovedVars;
	}
}
