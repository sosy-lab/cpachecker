package octagon;

import java.util.Hashtable;


/**
 * Used to create a mapping from function names to variable maps, this class
 * is used to keep variables of each function. see {@link VariableMap}.
 * @author erkan
 *
 */
public class FunctionMap {

	Hashtable<String, VariableMap> funcMap;

	/**
	 * Class constructor.
	 */
	public FunctionMap(){
		funcMap = new Hashtable<String, VariableMap>();
	}

	/**
	 * Return {@link VariableMap} for required function.
	 * @param name Function name
	 * @return Variable map for the function
	 */
	public VariableMap getMapForFunction(String name){
		return funcMap.get(name);
	}

	/**
	 * Add a new empty {@link VariableMap} for the given function if it does not
	 * exists.
	 * @param name Function name.
	 */
	public void addMapForFunction(String name){
		if(!funcMap.containsKey(name)){
			VariableMap vmap = new VariableMap(0);
			funcMap.put(name, vmap);
		}
	}
	
	/**
	 * Query to ask if the map contains a key with the given function name.
	 * @param name Name of the function.
	 * @return true if the map contains name key, false o.w.
	 */
	public boolean containsMap(String name){
		return funcMap.containsKey(name);
	}

	/**
	 * Adds a new variable variableName to function functionName if it does not
	 * already exists. 
	 * @param functionName Name of the function.
	 * @param variableName Name of the variable.
	 */
	public void addVariableToFunction(String functionName, String variableName){
		if(!containsMap(functionName)){
			addMapForFunction(functionName);
		}
		VariableMap vmap = getMapForFunction(functionName);
		vmap.addVar(variableName);
	}
}
