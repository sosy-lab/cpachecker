package cpaplugin.cfa;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;

public class CFAMap {
	
	private HashMap<String, CFAFunctionDefinitionNode> cfaMap;
	
	public CFAMap (){
		cfaMap = new HashMap<String, CFAFunctionDefinitionNode>();
	}
	
	public void addCFA (String name, CFAFunctionDefinitionNode initNode){
		cfaMap.put(name, initNode);
	}
	
	public CFAFunctionDefinitionNode getCFA (String name){
		return cfaMap.get(name);
	}
	
	public int size (){
		return cfaMap.size();
	}
	
	public Collection <CFAFunctionDefinitionNode> cfaMapIterator (){
		return cfaMap.values();
	}
	
	public String toString(){
		String s = "";
		Collection <CFAFunctionDefinitionNode> cfas = cfaMapIterator ();
		for(CFAFunctionDefinitionNode cfa:cfas){
			s = s + cfa.getFunctionName() + "\n";
		}
		return s;
	}
}
