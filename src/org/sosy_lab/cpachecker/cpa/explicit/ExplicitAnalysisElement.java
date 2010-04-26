/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.explicit;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableElement;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

public class ExplicitAnalysisElement implements AbstractQueryableElement {

  // map that keeps the name of variables and their constant values
  private Map<String, Long> constantsMap;

  private Map<String, Integer> noOfReferences;

  public ExplicitAnalysisElement() {
    constantsMap = new HashMap<String, Long>();
    noOfReferences = new HashMap<String, Integer>();
  }

  public ExplicitAnalysisElement(Map<String, Long> constantsMap,
                                 Map<String, Integer> referencesMap) {
    this.constantsMap = constantsMap;
    this.noOfReferences = referencesMap;
  }

  /**
   * Assigns a value to the variable and puts it in the map
   * @param nameOfVar name of the variable.
   * @param value value to be assigned.
   * @param pThreshold threshold from property explicitAnalysis.threshold
   */
  public void assignConstant(String nameOfVar, long value, int pThreshold){

    if(constantsMap.containsKey(nameOfVar) &&
        constantsMap.get(nameOfVar).intValue() == value){
      return;
    }

    if(pThreshold == 0){
      return;
    }

    if(noOfReferences.containsKey(nameOfVar)){
      int currentVal = noOfReferences.get(nameOfVar).intValue();
      if(currentVal >= pThreshold){
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

  public long getValueFor(String variableName){
    return constantsMap.get(variableName).longValue();
  }

  public boolean contains(String variableName){
    return constantsMap.containsKey(variableName);
  }

  @Override
  public ExplicitAnalysisElement clone() {
    ExplicitAnalysisElement newElement = new ExplicitAnalysisElement();
    for (String s: constantsMap.keySet()){
      long val = constantsMap.get(s).longValue();
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
      if(otherElement.constantsMap.get(s).longValue() !=
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
      long val = constantsMap.get(key);
      int refCount = noOfReferences.get(key);
      s = s  + " <" +key + " = " + val + " :: " + refCount + "> ";
    }
    return s + "] size->  " + constantsMap.size();
  }

  public Map<String, Long> getConstantsMap(){
    return constantsMap;
  }

  public void forget(String assignedVar) {
    if(constantsMap.containsKey(assignedVar)){
      constantsMap.remove(assignedVar);
    }
  }

  public Map<String, Integer> getNoOfReferences() {
    return noOfReferences;
  }

  @Override
  public boolean isError() {
    return false;
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    // e.g. "x==5" where x is a variable. Returns if 5 is the associated constant
    String[] parts = pProperty.split("==");
    if (parts.length != 2)
      throw new InvalidQueryException("The Query \"" + pProperty + "\" is invalid. Could not split the property string correctly.");
    else {
      Long value = this.constantsMap.get(parts[0]);
      if (value == null) {
        return false;
      } else {
        try {
          return value.longValue() == Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
          // The command might contains something like "main::p==cmd" where the user wants to compare the variable p to the variable cmd (nearest in scope)
          // perhaps we should omit the "main::" and find the variable via static scoping ("main::p" is also not intuitive for a user)
          // TODO: implement Variable finding via static scoping
          throw new InvalidQueryException("The Query \"" + pProperty + "\" is invalid. Could not parse the long \"" + parts[1] + "\"");
        }
      }
    }
  }

  @Override
  public String getCPAName() {
    return "ExplicitAnalysis";
  }
}
