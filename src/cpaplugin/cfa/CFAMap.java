package cpaplugin.cfa;

import java.util.Collection;
import java.util.HashMap;

import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;

/**
 * A map to save all CFAs of the program. Provides operations to retrieve and insert
 * new CFAs to the map
 * @author erkan
 *
 */
public class CFAMap {
	
	private HashMap<String, CFAFunctionDefinitionNode> cfaMap;
	
	/**
	 * Class constructor. Creates a new {@link HashMap} to map the name of the function
	 * to the first node of the function.
	 */
	public CFAMap (){
		cfaMap = new HashMap<String, CFAFunctionDefinitionNode>();
	}
	
	/**
	 * Add a new CFA to the map.
	 * @param name name of the function.
	 * @param initNode the first node of the function. All nodes in the CFA of a function
	 * is accessible through its first node.
	 */
	public void addCFA (String name, CFAFunctionDefinitionNode initNode){
		cfaMap.put(name, initNode);
	}
	
	/**
	 * Retrieves the function's definition (the first) node.
	 * @param name name of the function.
	 * @return the first node of the function.
	 */
	public CFAFunctionDefinitionNode getCFA (String name){
		return cfaMap.get(name);
	}
	
	/**
	 * @return number of CFAs.
	 */
	public int size (){
		return cfaMap.size();
	}
	
	/**
	 * @return a collection of function nodes on the map.
	 */
	public Collection <CFAFunctionDefinitionNode> cfaMapIterator (){
		return cfaMap.values();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString(){
		String s = "";
		Collection <CFAFunctionDefinitionNode> cfas = cfaMapIterator ();
		for(CFAFunctionDefinitionNode cfa:cfas){
			s = s + cfa.getFunctionName() + "\n";
		}
		return s;
	}
}
