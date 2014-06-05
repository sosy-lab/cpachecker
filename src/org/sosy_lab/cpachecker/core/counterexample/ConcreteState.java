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

import java.util.Map;

import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;


public final class ConcreteState {

  private final Map<LeftHandSide, Object> variables;
  private final Map<String, Memory> allocatedMemory;
  private final Map<LeftHandSide, Address> variableAddressMap;

  private final MemoryName memoryNameAllocator;

  public ConcreteState(Map<LeftHandSide, Object> pVariables,
      Map<String, Memory> pAllocatedMemory,
      Map<LeftHandSide, Address> pVariableAddressMap,
      MemoryName pMemoryName) {
    variableAddressMap = ImmutableMap.copyOf(pVariableAddressMap);
    allocatedMemory = ImmutableMap.copyOf(pAllocatedMemory);
    variables = ImmutableMap.copyOf(pVariables);
    memoryNameAllocator = pMemoryName;
  }

  //TODO abstract?
  @SuppressWarnings("unused")
  private String getMemoryName(CRightHandSide exp, Address address) {

    CType expectedType = exp.getExpressionType().getCanonicalType();

    expectedType = CTypes.withoutConst(expectedType);
    expectedType = CTypes.withoutVolatile(expectedType);

    return "*" + expectedType.toString();
  }

  public Object getValueFromMemory(CRightHandSide exp, Address address) {

    String memoryName = memoryNameAllocator.getMemoryName(exp, address);

    if (!allocatedMemory.containsKey(memoryName)) {
      return null;
    }

    Memory memory = allocatedMemory.get(memoryName);

    if (memory.hasValue(address)) {
      return memory.getValue(address);
    }

    return null;
  }

  public boolean hasValueForLeftHandSide(LeftHandSide variable) {
    return variables.containsKey(variable);
  }

  public Object getVariableValue(LeftHandSide variable) {
    Preconditions.checkArgument(variables.containsKey(variable));

    return variables.get(variable);
  }

  public boolean hasAddressOfVaribable(LeftHandSide variable) {
    return variableAddressMap.containsKey(variable);
  }

  public Address getVariableAddress(LeftHandSide variable) {
    Preconditions.checkArgument(variableAddressMap.containsKey(variable));

    return variableAddressMap.get(variable);
  }

  @Override
  public int hashCode() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean equals(Object obj) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return "variables=" + variables
        + System.lineSeparator() + "allocatedMemory=" + allocatedMemory
        + System.lineSeparator() + " variableAddressMap=" + variableAddressMap;
  }
}