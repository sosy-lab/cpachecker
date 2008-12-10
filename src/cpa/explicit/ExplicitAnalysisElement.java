package cpa.explicit;

import java.util.HashMap;
import java.util.Map;

import cpa.common.interfaces.AbstractElement;

public class ExplicitAnalysisElement implements AbstractElement {

	// map that keeps the name of variables and their constant values
	private Map<String, Integer> constantsMap;
	private boolean isBottom = false;
	
	public ExplicitAnalysisElement() {
		constantsMap = new HashMap<String, Integer>();
	}
	
	public ExplicitAnalysisElement(Map<String, Integer> constantsMap) {
		this.constantsMap = constantsMap;
	}
	
	/**
	 * Assigns a value to the variable and puts it in the map
	 * @param nameOfVar name of the variable.
	 * @param value value to be assigned.
	 */
	public void assignConstant(String nameOfVar, int value){
		constantsMap.put(nameOfVar, value);
	}
	
	public int getValueFor(String variableName){
		return constantsMap.get(variableName);
	}
	
	public boolean contains(String variableName){
		return constantsMap.containsKey(variableName);
	}
	
	public void setBottom(){
	  isBottom = true;
	}
	
	public boolean isBottom(){
	  return isBottom;
	}
	
	@Override
    public ExplicitAnalysisElement clone() {
		ExplicitAnalysisElement newElement = new ExplicitAnalysisElement();
        for (String s: constantsMap.keySet()){
            int val = constantsMap.get(s);
            newElement.assignConstant(s, val);
        }
        return newElement;
    }
	
	@Override
    public boolean equals (Object other) {
        if (this == other)
            return true;

        assert (other instanceof ExplicitAnalysisElement);   

        ExplicitAnalysisElement otherElement = (ExplicitAnalysisElement) other;
        if (otherElement.constantsMap.size() != constantsMap.size())
            return false;

        for (String s: constantsMap.keySet()){
            if(!otherElement.constantsMap.containsKey(s)){
            	return false;
            }
            if(otherElement.constantsMap.get(s) != 
            	constantsMap.get(s)){
            	return false;
            }
        }
        return true;
    }
	
	@Override
	public String toString() {
		String s = "[";
		for (String key: constantsMap.keySet()){
            int val = constantsMap.get(key);
            s = s  + " <" +key + " = " + val + "> ";
        }
		return s + "]";
	}
	
	public Map<String, Integer> getConstantsMap(){
		return constantsMap;
	}

	public void forget(String assignedVar) {
		if(constantsMap.containsKey(assignedVar)){
			constantsMap.remove(assignedVar);
		}
	}

  public void update(ExplicitAnalysisElement newElement) {
    constantsMap = newElement.getConstantsMap();
  }
}
