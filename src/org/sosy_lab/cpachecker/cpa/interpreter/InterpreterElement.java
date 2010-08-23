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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;

public class InterpreterElement implements InterpreterDomainElement {

  // map that keeps the name of variables and their constant values
  private final Map<String, Long> mConstantsMap;

  private final InterpreterElement mPreviousElement;
  
  public InterpreterElement() {
    mConstantsMap = new HashMap<String, Long>();
    mPreviousElement = null;
  }

  /**
   * Just for testing.
   */
  public InterpreterElement(Map<String, Long> pConstantsMap) {
    this(checkNotNull(pConstantsMap), null);
  }

  /**
   * Copy constructor.
   */
  public InterpreterElement(InterpreterElement pElement) {
    assert(pElement != null);
    assert(pElement.mConstantsMap != null);

    this.mConstantsMap = new HashMap<String, Long>(pElement.mConstantsMap);
    this.mPreviousElement = pElement.mPreviousElement;
  }

  /**
   * Factory method because there is another constructor with the same parameters.
   */
  public static InterpreterElement createForFunctionCall(InterpreterElement pPreviousElement) {
    return new InterpreterElement(new HashMap<String, Long>(), pPreviousElement);
  }
  
  private InterpreterElement(Map<String, Long> pConstantsMap, InterpreterElement pPreviousElement) {
    this.mConstantsMap = pConstantsMap;
    this.mPreviousElement = pPreviousElement;
  }
  
  /**
   * Assigns a value to the variable and puts it in the map
   * @param pNameOfVar name of the variable.
   * @param pValue value to be assigned.
   */
  public void assignConstant(String pNameOfVar, long pValue){
    if (mConstantsMap.containsKey(pNameOfVar) &&
        mConstantsMap.get(pNameOfVar).longValue() == pValue) {
      return;
    }

    mConstantsMap.put(pNameOfVar, pValue);
  }

  public long getValueFor(String pVariableName){
    return mConstantsMap.get(pVariableName).longValue();
  }

  public boolean contains(String pVariableName){
    return mConstantsMap.containsKey(pVariableName);
  }

  public InterpreterElement getPreviousElement() {
    return mPreviousElement;
  }
  
  @Override
  public boolean equals (Object pOther) {
    if (this == pOther) {
      return true;
    }

    if (pOther == null) {
      return false;
    }

    if (pOther.getClass() == getClass()) {
      InterpreterElement lOtherElement = (InterpreterElement) pOther;

      return lOtherElement.mConstantsMap.equals(mConstantsMap);
    }

    return false;
  }

  /*
   * CAUTION: The hash code of an object can change during its lifetime.
   *
   * (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return mConstantsMap.hashCode();
  }

  @Override
  public String toString() {
    StringBuffer lBuffer = new StringBuffer();

    lBuffer.append("[");

    for (String key: mConstantsMap.keySet()){
      long val = mConstantsMap.get(key);

      lBuffer.append(" <");
      lBuffer.append(key);
      lBuffer.append(" = ");
      lBuffer.append(val);
      lBuffer.append("> ");
    }

    lBuffer.append("] size->  ");
    lBuffer.append(mConstantsMap.size());

    return lBuffer.toString();
  }

}
