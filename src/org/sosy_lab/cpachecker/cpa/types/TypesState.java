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
package org.sosy_lab.cpachecker.cpa.types;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.types.Type.FunctionType;

public class TypesState implements AbstractState {

  private final Map<String, Type> variables;

  private final Map<String, Type> typedefs;

  private final Map<String, FunctionType> functions;

  public TypesState() {
    this.variables = new HashMap<>();
    this.typedefs  = new HashMap<>();
    this.functions = new HashMap<>();
  }

  public TypesState(Map<String, Type> variables, Map<String, Type> typedefs,
                      Map<String, FunctionType> functions) {
    this.variables = new HashMap<>(variables);
    this.typedefs  = new HashMap<>(typedefs);
    this.functions = new HashMap<>(functions);
  }

  public Map<String, Type> getTypedefs() {
    return Collections.unmodifiableMap(typedefs);
  }

  public Map<String, Type> getVariableTypes() {
    return Collections.unmodifiableMap(variables);
  }

  public Map<String, FunctionType> getFunctions() {
    return Collections.unmodifiableMap(functions);
  }

  public void addVariable(String function, String name, Type type) {
    variables.put(getFullVariableName(function, name), type);
  }

  public void addTypedef(String name, Type type) {
    if (variables.containsKey(name)) {
      throw new IllegalArgumentException("Redeclared type " + name);
    }
    typedefs.put(name, type);
  }

  public void addFunction(String name, FunctionType type) {
    if (variables.containsKey(name)) {
      throw new IllegalArgumentException("Redeclared function " + name);
    }
    functions.put(name, type);
  }

  public Type getVariableType(String function, String name) {
    Type result = variables.get(getFullVariableName(function, name));
    if (result == null && function != null) {
      assert functions.containsKey(function);
      // try parameter instead of local variable
      result = functions.get(function).getParameterType(name);
    }
    return result;
  }

  public Type getTypedef(String name) {
    return typedefs.get(name);
  }

  public FunctionType getFunction(String name) {
    return functions.get(name);
  }

  public void join(TypesState other) {
    if (other == null) {
      throw new IllegalArgumentException();
    }
    if (other != this) {
      this.variables.putAll(other.variables);
      this.typedefs.putAll(other.typedefs);
    }
  }

  public boolean isSubsetOf(TypesState other) {
    if (other == null) {
      throw new IllegalArgumentException();
    }
    if (variables.size() > other.variables.size()
        || typedefs.size() > other.typedefs.size()
        || functions.size() > other.functions.size()) {
      return false;
    }

    for (String var : variables.keySet()) {
      if (!variables.get(var).equals(other.variables.get(var))) {
        return false;
      }
    }
    for (String type : typedefs.keySet()) {
      if (!typedefs.get(type).equals(other.typedefs.get(type))) {
        return false;
      }
    }
    for (String function : functions.keySet()) {
      if (!functions.get(function).equals(other.functions.get(function))) {
        return false;
      }
    }
    return true;
  }

  private String getFullVariableName(String function, String variable) {
    if (function == null) {
      return variable;
    } else {
      return function + "::" + variable;
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof TypesState)) {
      return false;
    }
    TypesState other = (TypesState)obj;
    return variables.equals(other.variables)
        && typedefs.equals(other.typedefs)
        && functions.equals(other.functions);
  }

  @Override
  public int hashCode() {
    return variables.hashCode() * typedefs.hashCode() * functions.hashCode();
  }

  @Override
  public String toString() {
    return variables.toString() + " " + functions.toString();
  }
}