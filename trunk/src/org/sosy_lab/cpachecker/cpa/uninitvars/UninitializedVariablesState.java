// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.uninitvars;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Triple;

public class UninitializedVariablesState implements AbstractQueryableState, Serializable {

  private static final long serialVersionUID = 5745797034946117366L;
  private final List<String> globalVars;
  private final Deque<Pair<String, List<String>>> localVars;

  private final Collection<Triple<Integer, String, String>> warnings;

  enum ElementProperty {
    UNINITIALIZED_RETURN_VALUE,
    UNINITIALIZED_VARIABLE_USED
  }

  private Set<ElementProperty> properties = EnumSet.noneOf(ElementProperty.class); // emptySet

  public UninitializedVariablesState(String entryFunction) {
    globalVars = new ArrayList<>();
    localVars = new ArrayDeque<>();
    warnings = new ArrayList<>();
    // create context of the entry function
    callFunction(entryFunction);
  }

  public UninitializedVariablesState(
      List<String> globalVars,
      Deque<Pair<String, List<String>>> localVars,
      Collection<Triple<Integer, String, String>> warnings) {
    this.globalVars = globalVars;
    this.localVars = localVars;
    this.warnings = warnings;
  }

  public void addGlobalVariable(String name) {
    if (!globalVars.contains(name)) {
      globalVars.add(name);
    }
  }

  public void removeGlobalVariable(String name) {
    globalVars.remove(name);
  }

  public Collection<String> getGlobalVariables() {
    return globalVars;
  }

  public void addLocalVariable(String name) {
    if (!localVars.peekLast().getSecond().contains(name)) {
      localVars.peekLast().getSecond().add(name);
    }
  }

  public void removeLocalVariable(String name) {
    localVars.peekLast().getSecond().remove(name);
  }

  public Collection<String> getLocalVariables() {
    return localVars.peekLast().getSecond();
  }

  public Deque<Pair<String, List<String>>> getallLocalVariables() {
    return localVars;
  }

  public Collection<Triple<Integer, String, String>> getWarnings() {
    return warnings;
  }

  public boolean isUninitialized(String variable) {
    return globalVars.contains(variable) || localVars.peekLast().getSecond().contains(variable);
  }

  public void callFunction(String functionName) {
    localVars.addLast(Pair.of(functionName, new ArrayList<String>()));
  }

  public void returnFromFunction() {
    localVars.pollLast();
  }

  public void addWarning(Integer lineNumber, String variable, String message) {
    Triple<Integer, String, String> warning = Triple.of(lineNumber, variable, message);
    if (!warnings.contains(warning)) {
      warnings.add(warning);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof UninitializedVariablesState)) {
      return false;
    }
    if (this == o) {
      return true;
    }

    UninitializedVariablesState otherElement = (UninitializedVariablesState) o;

    return globalVars.equals(otherElement.globalVars) && localVars.equals(otherElement.localVars);
  }

  @Override
  public int hashCode() {
    return localVars.hashCode();
  }

  @Override
  protected UninitializedVariablesState clone() {
    Deque<Pair<String, List<String>>> newLocalVars = new ArrayDeque<>();

    for (Pair<String, List<String>> localContext : localVars) {
      newLocalVars.addLast(
          Pair.of(localContext.getFirst(), new ArrayList<>(localContext.getSecond())));
    }

    return new UninitializedVariablesState(
        new ArrayList<>(globalVars), newLocalVars, new ArrayList<>(warnings));
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[<global:");
    for (String var : globalVars) {
      sb.append(" " + var + " ");
    }
    for (Pair<String, List<String>> stackframe : localVars) {
      sb.append("> <" + stackframe.getFirst() + ":");
      for (String var : stackframe.getSecond()) {
        sb.append(" " + var + " ");
      }
    }
    sb.append(">]");
    return sb.toString();
  }

  /**
   * Adds a property to this element
   *
   * @param pProp the property to add
   */
  void addProperty(ElementProperty pProp) {
    properties.add(pProp);
  }
  /** Returns all properties set for this element. */
  Set<ElementProperty> getProperties() {
    return properties;
  }
  /** Removes all property of this element */
  void clearProperties() {
    properties.clear();
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    ElementProperty prop;
    try {
      prop = ElementProperty.valueOf(pProperty);
    } catch (IllegalArgumentException e) {
      // thrown if the Enum does not contain the property
      throw new InvalidQueryException(
          "The Query \"" + pProperty + "\" is not defined for this CPA (\"" + getCPAName() + "\"");
    }
    return properties.contains(prop);
  }

  @Override
  public String getCPAName() {
    return "uninitVars";
  }
}
