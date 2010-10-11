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
package org.sosy_lab.cpachecker.cpa.interpreter;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;

public class InterpreterElement implements AbstractElement {

  // map that keeps the name of variables and their constant values
  private final Map<String, Long> mConstantsMap;

  // element from the previous context
  // used for return edges
  private final InterpreterElement mPreviousElement;
  
  // TODO make final
  private int mInputIndex;
  
  @Option
  private String noAutoInitPrefix = "__BLAST_NONDET";

  public InterpreterElement() {
    mConstantsMap = new HashMap<String, Long>();
    mPreviousElement = null;
    mInputIndex = 0;
  }

  public InterpreterElement(InterpreterElement previousElement, int lInputIndex) {
    this(new HashMap<String, Long>(), previousElement, lInputIndex);
  }


  public InterpreterElement(Map<String, Long> constantsMap, InterpreterElement previousElement, int lInputIndex) {
    mConstantsMap = constantsMap;
    mPreviousElement = previousElement;
    mInputIndex = lInputIndex;
  }
  
  public int getInputIndex() {
    if (mInputIndex == -1) {
      throw new RuntimeException();
    }
    
    return mInputIndex;
  }

  // TODO change
  public void incIndex() {
    mInputIndex = mInputIndex + 1;
  }
  
  /**
   * Assigns a value to the variable and puts it in the map
   * @param nameOfVar name of the variable.
   * @param value value to be assigned.
   */
  public void assignConstant(String nameOfVar, long value) {

    if(mConstantsMap.containsKey(nameOfVar) &&
        mConstantsMap.get(nameOfVar).intValue() == value) {
      return;
    }
    
    if(nameOfVar.contains(noAutoInitPrefix)){
      throw new RuntimeException(nameOfVar);
    }

    mConstantsMap.put(nameOfVar, value);
  }

  public long getValueFor(String pVariableName){
    if (pVariableName.endsWith("::__BLAST_NONDET")) {
      throw new RuntimeException();
    }
    
    if (!mConstantsMap.containsKey(pVariableName)) {
      throw new RuntimeException("Unassigned variable: " + pVariableName);
    }
    
    return mConstantsMap.get(pVariableName).longValue();
  }

  public boolean contains(String variableName){
    return mConstantsMap.containsKey(variableName);
  }

  public InterpreterElement getPreviousElement() {
    return mPreviousElement;
  }
  
  @Override
  public InterpreterElement clone() {
    // TODO change this
    
    InterpreterElement newElement = new InterpreterElement(mPreviousElement, mInputIndex);
    
    for (String s: mConstantsMap.keySet()){
      long val = mConstantsMap.get(s).longValue();
      newElement.mConstantsMap.put(s, val);
    }
    
    return newElement;
  }

  @Override
  public boolean equals (Object other) {
    if (this == other)
      return true;

    //assert (other instanceof ExplicitAnalysisElement);
    
    if (other == null) {
      return false;
    }
    
    if (!getClass().equals(other.getClass())) {
      return false;
    }

    InterpreterElement otherElement = (InterpreterElement) other;
    if (otherElement.mConstantsMap.size() != mConstantsMap.size()){
      return false;
    }
    
    if (mInputIndex != otherElement.mInputIndex) {
      return false;
    }

    for (String s: mConstantsMap.keySet()){
      if(!otherElement.mConstantsMap.containsKey(s)){
        return false;
      }
      if(otherElement.mConstantsMap.get(s).longValue() !=
        mConstantsMap.get(s)){
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    return mConstantsMap.hashCode();
  }

  @Override
  public String toString() {
    String s = "idx: " + mInputIndex + " [";
    for (String key: mConstantsMap.keySet()){
      long val = mConstantsMap.get(key);
      s = s  + " <" +key + " = " + val + "> ";
    }
    return s + "] size->  " + mConstantsMap.size();
  }

  public Map<String, Long> getConstantsMap(){
    return mConstantsMap;
  }

  public void forget(String assignedVar) {
    if(mConstantsMap.containsKey(assignedVar)){
      mConstantsMap.remove(assignedVar);
    }
    
    throw new RuntimeException();
  }
 
}
