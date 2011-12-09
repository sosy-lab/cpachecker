/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.cpa.interpreter.exceptions.AccessToUninitializedVariableException;
import org.sosy_lab.cpachecker.cpa.interpreter.exceptions.MissingInputException;

import com.google.common.base.Joiner;

public class InterpreterElement implements AbstractElement {

  // map that keeps the name of variables and their constant values
  private final Map<String, Long> mConstantsMap;

  // element from the previous context
  // used for return edges
  private final InterpreterElement mPreviousElement;

  // TODO make final
  private int mInputIndex;
  private int[] mInputs;

  public InterpreterElement() {
    this(new int[0]);
  }

  public InterpreterElement(int[] pInputs) {
    this(new HashMap<String, Long>(), null, 0, pInputs);
  }

  public InterpreterElement(InterpreterElement pPreviousContextElement, int pInputIndex, int[] pInputs) {
    this(new HashMap<String, Long>(), pPreviousContextElement, pInputIndex, pInputs);
  }

  public InterpreterElement(Map<String, Long> pConstantsMap, InterpreterElement pPreviousContextElement, int pInputIndex, int[] pInputs) {
    if (pInputs == null) {
      throw new IllegalArgumentException();
    }

    mConstantsMap = pConstantsMap;
    mPreviousElement = pPreviousContextElement;
    mInputIndex = pInputIndex;

    mInputs = pInputs;
  }

  public int getInputIndex() {
    return mInputIndex;
  }

  public int[] getInputs() {
    return mInputs;
  }

  private void setInputIndex(int pIndex) {
    mInputIndex = pIndex;
  }

  // TODO change
  private void incIndex() {
    mInputIndex = mInputIndex + 1;
  }

  public int getCurrentInput() throws MissingInputException {
    if (mInputIndex < 0) {
      throw new RuntimeException("Input index is 0");
    }

    if (mInputIndex >= mInputs.length) {
      throw new MissingInputException("__BLAST_NONDET");
    }

    return mInputs[mInputIndex];
  }

  /**
   * Assigns a value to the variable and puts it in the map
   * @param nameOfVar name of the variable.
   * @param value value to be assigned.
   */
  public void assignConstant(String nameOfVar, long value) {

    if(mConstantsMap.containsKey(nameOfVar) &&
        mConstantsMap.get(nameOfVar).longValue() == value) {
      return;
    }

    mConstantsMap.put(nameOfVar, value);
  }

  public long getValueFor(String pVariableName) throws AccessToUninitializedVariableException {
    if (!mConstantsMap.containsKey(pVariableName)) {
      throw new AccessToUninitializedVariableException(pVariableName);
    }

    return mConstantsMap.get(pVariableName).longValue();
  }

  public boolean contains(String variableName){
    return mConstantsMap.containsKey(variableName);
  }

  public InterpreterElement getPreviousElement() {
    return mPreviousElement;
  }

  public InterpreterElement nextInputElement() {
    InterpreterElement lTmpElement = clone();

    lTmpElement.incIndex();

    return lTmpElement;
  }

  public InterpreterElement getUpdatedPreviousElement() {
    InterpreterElement previousElem = getPreviousElement();
    InterpreterElement newElement = previousElem.clone();

    newElement.mInputs = mInputs;

    newElement.setInputIndex(getInputIndex());

    return newElement;
  }

  @Override
  public InterpreterElement clone() {
    // TODO change this

    InterpreterElement newElement = new InterpreterElement(mPreviousElement, mInputIndex, mInputs);

    for (String s: mConstantsMap.keySet()){
      long val = mConstantsMap.get(s).longValue();
      newElement.mConstantsMap.put(s, val);
    }

    return newElement;
  }

  @Override
  public boolean equals (Object other) {
    if (this == other) {
      return true;
    }

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

    if (mInputs.length != otherElement.mInputs.length) {
      return false;
    }

    for (int lIndex = 0; lIndex < mInputs.length; lIndex++) {
      if (mInputs[lIndex] != otherElement.mInputs[lIndex]) {
        return false;
      }
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
    StringBuilder sb = new StringBuilder();
    sb.append("{");

    Joiner.on(", ").appendTo(sb, Arrays.asList(mInputs));

    sb.append("}[");
    sb.append(mInputIndex);
    sb.append("] [");

    for (Map.Entry<String, Long> entry: mConstantsMap.entrySet()){
      String key = entry.getKey();
      long val = entry.getValue();
      sb.append(" <");
      sb.append(key);
      sb.append(" = ");
      sb.append(val);
      sb.append("> ");
    }
    return sb.append("] size->  ").append(mConstantsMap.size()).toString();
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
