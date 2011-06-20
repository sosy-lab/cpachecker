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
package org.sosy_lab.cpachecker.cpa.explicit;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableElement;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.assumptions.FormulaReportingElement;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;

import com.google.common.base.Preconditions;

public class ExplicitElement implements AbstractQueryableElement, FormulaReportingElement {

  // map that keeps the name of variables and their constant values
  private final Map<String, Long> constantsMap;

  private final Map<String, Integer> noOfReferences;

  // element from the previous context
  // used for return edges
  private final ExplicitElement previousElement;

  @Option(description="variables whose name contains this will be seen by ExplicitCPA as having non-deterministic values")
  // TODO this is completely broken, name doesn't match, the option is never read from file etc.
  private String noAutoInitPrefix = "__BLAST_NONDET";

  public ExplicitElement() {
    constantsMap = new HashMap<String, Long>();
    noOfReferences = new HashMap<String, Integer>();
    previousElement = null;
  }

  public ExplicitElement(ExplicitElement previousElement) {
    constantsMap = new HashMap<String, Long>();
    noOfReferences = new HashMap<String, Integer>();
    this.previousElement = previousElement;
  }


  public ExplicitElement(Map<String, Long> constantsMap,
      Map<String, Integer> referencesMap,
      ExplicitElement previousElement) {
    this.constantsMap = constantsMap;
    this.noOfReferences = referencesMap;
    this.previousElement = previousElement;
  }

  /**
   * Assigns a value to the variable and puts it in the map
   * @param nameOfVar name of the variable.
   * @param value value to be assigned.
   * @param pThreshold threshold from property explicitAnalysis.threshold
   */
  void assignConstant(String nameOfVar, long value, int pThreshold){

    if(constantsMap.containsKey(nameOfVar) &&
        constantsMap.get(nameOfVar).longValue() == value){
      return;
    }

    if(pThreshold == 0){
      return;
    }

    if(nameOfVar.contains(noAutoInitPrefix)){
      return;
    }

    if(noOfReferences.containsKey(nameOfVar)){
      int currentVal = noOfReferences.get(nameOfVar).intValue();
      if(currentVal >= pThreshold) {
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

  ExplicitElement getPreviousElement() {
    return previousElement;
  }

  @Override
  public ExplicitElement clone() {
    ExplicitElement newElement = new ExplicitElement(previousElement);
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

    //assert (other instanceof ExplicitElement);

    if (other == null) {
      return false;
    }

    if (!getClass().equals(other.getClass())) {
      return false;
    }

    ExplicitElement otherElement = (ExplicitElement) other;
    if (otherElement.previousElement != previousElement) {
      return false;
    }

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
      s = s  + " <" +key + " = " + val +
      " :: " + refCount +
      "> ";
    }
    return s + "] size->  " + constantsMap.size();
  }

  public Map<String, Long> getConstantsMap(){
    return constantsMap;
  }

  void forget(String assignedVar) {
    if(constantsMap.containsKey(assignedVar)){
      constantsMap.remove(assignedVar);
    }
  }

  Map<String, Integer> getNoOfReferences() {
    return noOfReferences;
  }

  @Override
  public Object evaluateProperty(String pProperty) throws InvalidQueryException {
    pProperty = pProperty.trim();
    if (pProperty.startsWith("contains(")) {
      String varName = pProperty.substring("contains(".length(),pProperty.length()-1);
      return this.constantsMap.containsKey(varName);
    } else {
      String[] parts = pProperty.split("==");
      if (parts.length != 2) {
        Long value = this.constantsMap.get(pProperty);
        if (value != null) {
          return value;
        } else {
          throw new InvalidQueryException("The Query \"" + pProperty + "\" is invalid. Could not find the variable \"" + pProperty + "\"");
        }
      } else {
        return checkProperty(pProperty);
      }
    }
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
  public void modifyProperty(String pModification)
  throws InvalidQueryException {
    Preconditions.checkNotNull(pModification);
    // either "deletevalues(methodname::varname)" or "setvalue(methodname::varname:=1929)"
    String[] statements = pModification.split(";");
    for (int i = 0; i < statements.length; i++) {
      String statement = statements[i].trim().toLowerCase();
      if (statement.startsWith("deletevalues(")) {
        if (!statement.endsWith(")")) throw new InvalidQueryException(statement +" should end with \")\"");
        String varName = statement.substring("deletevalues(".length(), statement.length()-1);

        Object x = this.constantsMap.remove(varName);
        Object y = this.noOfReferences.remove(varName);

        if (x==null || y==null) {
          // varname was not present in one of the maps
          // i would like to log an error here, but no logger is available
        }
      } else if (statement.startsWith("setvalue(")) {
        if (!statement.endsWith(")")) throw new InvalidQueryException(statement +" should end with \")\"");
        String assignment = statement.substring("setvalue(".length(), statement.length()-1);
        String[] assignmentParts = assignment.split(":=");
        if (assignmentParts.length != 2)
          throw new InvalidQueryException("The Query \"" + pModification + "\" is invalid. Could not split the property string correctly.");
        else {
          String varName = assignmentParts[0].trim();
          try {
            long newValue = Long.parseLong(assignmentParts[1].trim());
            this.assignConstant(varName, newValue, 1); // threshold is passed as 1! This will only succeed if no other value for this variable is present
          } catch (NumberFormatException e) {
            throw new InvalidQueryException("The Query \"" + pModification + "\" is invalid. Could not parse the long \"" + assignmentParts[1].trim() + "\"");
          }
        }
      }
    }
  }

  @Override
  public String getCPAName() {
    return "ExplicitAnalysis";
  }

  @Override
  public Formula getFormulaApproximation(FormulaManager manager) {

    Formula formula = manager.makeTrue();
    for (Map.Entry<String, Long> entry : constantsMap.entrySet()) {
      Formula var = manager.makeVariable(entry.getKey());
      Formula val = manager.makeNumber(entry.getValue().toString());
      formula = manager.makeAnd(formula, manager.makeEqual(var, val));
    }

    return formula;
  }
}
