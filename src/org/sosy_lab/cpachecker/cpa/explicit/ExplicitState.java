/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;

import com.google.common.base.Preconditions;

public class ExplicitState implements AbstractQueryableState, FormulaReportingState, Serializable {
  private static final long serialVersionUID = -3152134511524554357L;

  /**
   * the map that keeps the name of variables and their constant values
   */
  private final Map<String, Long> constantsMap;

  /**
   * the element from the previous context, needed for return edges
   */
  private final ExplicitState previousState;

  public ExplicitState() {
    constantsMap    = new HashMap<String, Long>();
    previousState = null;
  }

  ExplicitState(ExplicitState previousElement) {
    constantsMap          = new HashMap<String, Long>();
    this.previousState  = previousElement;
  }

  private ExplicitState(Map<String, Long> constantsMap, ExplicitState previousElement) {
    this.constantsMap     = constantsMap;
    this.previousState  = previousElement;
  }

  /**
   * Assigns a value to the variable and puts it in the map
   * @param variableName name of the variable.
   * @param value value to be assigned.
   */
  void assignConstant(String variableName, Long value) {
    constantsMap.put(checkNotNull(variableName), checkNotNull(value));
  }

  void forget(String variableName) {
    constantsMap.remove(variableName);
  }

  public Long getValueFor(String variableName) {
    return checkNotNull(constantsMap.get(variableName));
  }

  public boolean contains(String variableName) {
    return constantsMap.containsKey(variableName);
  }

  ExplicitState getPreviousState() {
    return previousState;
  }

  public int getSize() {
    return constantsMap.size();
  }

  /**
   * This element joins this element with another element.
   *
   * @param other the other element to join with this element
   * @return a new state representing the join of this element and the other element
   */
  ExplicitState join(ExplicitState other) {
    int size = Math.min(constantsMap.size(), other.constantsMap.size());

    Map<String, Long> newConstantsMap = new HashMap<String, Long>(size);

    for (Map.Entry<String, Long> otherEntry : other.constantsMap.entrySet()) {
      String key = otherEntry.getKey();

      if (equal(otherEntry.getValue(), constantsMap.get(key))) {
        newConstantsMap.put(key, otherEntry.getValue());
      }
    }

    return new ExplicitState(newConstantsMap, previousState);
  }

  /**
   * This method decides if this element is less or equal than the other element, based on the order imposed by the lattice.
   *
   * @param other the other element
   * @return true, if this element is less or equal than the other element, based on the order imposed by the lattice
   */
  boolean isLessOrEqual(ExplicitState other) {
    // this element is not less or equal than the other element, if the previous elements differ
    if (previousState != other.previousState) {
      return false;
    }

    // also, this element is not less or equal than the other element, if it contains less elements
    if (constantsMap.size() < other.constantsMap.size()) {
      return false;
    }

    // also, this element is not less or equal than the other element,
    // if any one constant's value of the other element differs from the constant's value in this element
    for (Map.Entry<String, Long> otherEntry : other.constantsMap.entrySet()) {
      String key = otherEntry.getKey();

      if (!otherEntry.getValue().equals(constantsMap.get(key))) {
        return false;
      }
    }

    return true;
  }

  @Override
  public ExplicitState clone() {
    return new ExplicitState(new HashMap<String, Long>(constantsMap), previousState);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (other == null) {
      return false;
    }

    if (!getClass().equals(other.getClass())) {
      return false;
    }

    ExplicitState otherElement = (ExplicitState) other;

    return (otherElement.previousState == previousState)
        && otherElement.constantsMap.equals(constantsMap);
  }

  @Override
  public int hashCode() {
    return constantsMap.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (Map.Entry<String, Long> entry : constantsMap.entrySet()) {
      String key = entry.getKey();
      sb.append(" <");
      sb.append(key);
      sb.append(" = ");
      sb.append(entry.getValue());
      sb.append(">\n");
    }

    return sb.append("] size->  ").append(constantsMap.size()).toString();
  }

  public String toCompactString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[");

    boolean first = true;
    for (Map.Entry<String, Long> entry: constantsMap.entrySet())
    {
      if(first) {
        first = false;
      } else {
        sb.append(", ");
      }
      String key = entry.getKey();
      sb.append(key);
      sb.append("=");
      sb.append(entry.getValue());
    }
    sb.append("]");

    return sb.toString();
  }

  @Override
  public Object evaluateProperty(String pProperty) throws InvalidQueryException {
    pProperty = pProperty.trim();

    if (pProperty.startsWith("contains(")) {
      String varName = pProperty.substring("contains(".length(), pProperty.length() - 1);
      return this.constantsMap.containsKey(varName);
    } else {
      String[] parts = pProperty.split("==");
      if (parts.length != 2) {
        Long value = this.constantsMap.get(pProperty);
        if (value != null) {
          return value;
        } else {
          throw new InvalidQueryException("The Query \"" + pProperty + "\" is invalid. Could not find the variable \""
              + pProperty + "\"");
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

    if (parts.length != 2) {
      throw new InvalidQueryException("The Query \"" + pProperty
          + "\" is invalid. Could not split the property string correctly.");
    } else {
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
          throw new InvalidQueryException("The Query \"" + pProperty + "\" is invalid. Could not parse the long \""
              + parts[1] + "\"");
        }
      }
    }
  }

  @Override
  public void modifyProperty(String pModification) throws InvalidQueryException {
    Preconditions.checkNotNull(pModification);

    // either "deletevalues(methodname::varname)" or "setvalue(methodname::varname:=1929)"
    String[] statements = pModification.split(";");
    for (int i = 0; i < statements.length; i++) {
      String statement = statements[i].trim().toLowerCase();
      if (statement.startsWith("deletevalues(")) {
        if (!statement.endsWith(")")) {
          throw new InvalidQueryException(statement + " should end with \")\"");
        }

        String varName = statement.substring("deletevalues(".length(), statement.length() - 1);

        Object x = this.constantsMap.remove(varName);

        if (x == null) {
          // varname was not present in one of the maps
          // i would like to log an error here, but no logger is available
        }
      }

      else if (statement.startsWith("setvalue(")) {
        if (!statement.endsWith(")")) {
          throw new InvalidQueryException(statement + " should end with \")\"");
        }

        String assignment = statement.substring("setvalue(".length(), statement.length() - 1);
        String[] assignmentParts = assignment.split(":=");

        if (assignmentParts.length != 2) {
          throw new InvalidQueryException("The Query \"" + pModification
              + "\" is invalid. Could not split the property string correctly.");
        } else {
          String varName = assignmentParts[0].trim();
          try {
            long newValue = Long.parseLong(assignmentParts[1].trim());
            this.assignConstant(varName, newValue);
          } catch (NumberFormatException e) {
            throw new InvalidQueryException("The Query \"" + pModification
                + "\" is invalid. Could not parse the long \"" + assignmentParts[1].trim() + "\"");
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

  void deleteValue(String varName) {
    this.constantsMap.remove(varName);
  }

  Set<String> getTrackedVariableNames() {
    return constantsMap.keySet();
  }

  Map<String, Long> getConstantsMap() {
    return constantsMap;
  }
}
