/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.counterexample;

import java.util.Collection;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.counterexample.Model.Function;

import com.google.common.collect.Multimap;


public class ConcreteState {

  private static final String ADDRESS_PREFIX = "__ADDRESS_OF_";

  private final Map<String, Assignment> variableModel;


  private final Multimap<String, Assignment> uFModel;


  private final Map<String, Object> variableAddressMap;

  public ConcreteState(Map<String, Assignment> pVariableModel,
      Multimap<String, Assignment> pUFModel,
      Map<String, Object> pVariableAddressMap) {
    variableAddressMap = pVariableAddressMap;
    uFModel = pUFModel;
    variableModel = pVariableModel;
  }

  public Object getValueFromUF(CType pType, Address address) {

    CType type = pType.getCanonicalType();

    String ufName = getUFName(type);

    if (ufName == null) {
      return null;
    }

    Collection<Assignment> assignments = uFModel.get(ufName);

    for (Assignment assignment : assignments) {
      Function function = (Function) assignment.getTerm();

      if (function.getArity() != 1) {
        break;
      }

      if (address.comparesToUFArgument(function.getArgument(0))) {
        return assignment.getValue();
      }
    }
    return null;
  }

  private String getUFName(CType pType) {
    CType type = pType.getCanonicalType(false, false);

    //TODO Seems to work, for now
    //TODO ugly ... rewrite
    String name = type.toString().replace("volatile ", "").replace("const ", "").replace(" ", "_");

//    String name = type.accept(new TypeUFNameVisitor());

    if (name == null) {
      return null;
    }

    return "*" + name;
  }

  public boolean containsVariableName(String variableName) {
    return variableModel.containsKey(variableName);
  }

  public Object getVariableValue(String variableName) {
    if (variableModel.containsKey(variableName)) {
      return variableModel.get(variableName).getValue();
    } else {
      throw new IllegalArgumentException("Name of variable " + variableName + "not found in model.");
    }
  }

  public boolean containsVariableAddress(String varName) {
    String addressName = getAddressPrefix() + varName;
    return variableAddressMap.containsKey(addressName);
  }

  public Address getVariableAddress(String varName) {
    String addressName = getAddressPrefix() + varName;

    if (variableAddressMap.containsKey(addressName)) {
      Object addressO = variableAddressMap.get(addressName);
      return Address.valueOf(addressO);
    } else {
      throw new IllegalArgumentException();
    }
  }

  public static String getAddressPrefix() {
    return ADDRESS_PREFIX;
  }

  @Override
  public String toString() {
    return "ModelAtCFAEdge\n variableModel=" + variableModel + "\n uFModel=" + uFModel + "\n variableAddressMap="
        + variableAddressMap;
  }
}