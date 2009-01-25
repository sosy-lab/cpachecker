/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.explicit;

import java.util.HashMap;
import java.util.Map;

import cpa.common.interfaces.AbstractElement;

public class ExplicitAnalysisElement implements AbstractElement {

  // map that keeps the name of variables and their constant values
  private Map<String, Integer> constantsMap;
  private boolean isBottom = false;
  
  private Map<String, Integer> noOfReferences;
  
  public ExplicitAnalysisElement() {
    constantsMap = new HashMap<String, Integer>();
    noOfReferences = new HashMap<String, Integer>();
  }
  
  public ExplicitAnalysisElement(Map<String, Integer> constantsMap,
                                 Map<String, Integer> referencesMap) {
    this.constantsMap = constantsMap;
    this.noOfReferences = referencesMap;
  }
  
  /**
   * Assigns a value to the variable and puts it in the map
   * @param nameOfVar name of the variable.
   * @param value value to be assigned.
   */
  public void assignConstant(String nameOfVar, int value){

    if(constantsMap.containsKey(nameOfVar) && 
        constantsMap.get(nameOfVar).intValue() == value){
      return;
    }
    
    if(ExplicitAnalysisConstants.threshold == 0){
      return;
    }
    
    if(noOfReferences.containsKey(nameOfVar)){
      int currentVal = noOfReferences.get(nameOfVar).intValue();
      if(currentVal >= ExplicitAnalysisConstants.threshold){
        forget(nameOfVar);
        return;
      }
      int newVal = currentVal + 1;
      noOfReferences.put(nameOfVar, newVal);
    }
    else{
      noOfReferences.put(nameOfVar, 1);
    }
    
    constantsMap.put(nameOfVar, value);
  }
  
  public int getValueFor(String variableName){
    return constantsMap.get(variableName).intValue();
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
            int val = constantsMap.get(s).intValue();
            newElement.constantsMap.put(s, val);
        }
        for (String s: noOfReferences.keySet()){
          int val = noOfReferences.get(s).intValue();
          newElement.noOfReferences.put(s, val);
        }
        return newElement;
    }
  
  @Override
    public boolean equals (Object other) {
        if (this == other)
            return true;

        assert (other instanceof ExplicitAnalysisElement);   

        ExplicitAnalysisElement otherElement = (ExplicitAnalysisElement) other;
        if (otherElement.constantsMap.size() != constantsMap.size()){
            return false;
        }

        for (String s: constantsMap.keySet()){
            if(!otherElement.constantsMap.containsKey(s)){
              return false;
            }
            if(otherElement.constantsMap.get(s).intValue() != 
              constantsMap.get(s)){
              return false;
            }
        }
        return true;
    }
  
  @Override
  public int hashCode() {
    return constantsMap.hashCode();
  }
  
  @Override
  public String toString() {
    String s = "[";
    for (String key: constantsMap.keySet()){
            int val = constantsMap.get(key);
            int refCount = noOfReferences.get(key);
            s = s  + " <" +key + " = " + val + " :: " + refCount + "> ";
        }
    return s + " size-> " + constantsMap.size() + " .. "+ isBottom + "]";
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
    noOfReferences = newElement.getNoOfReferences();
  }

  public Map<String, Integer> getNoOfReferences() {
    return noOfReferences;
  }
}
