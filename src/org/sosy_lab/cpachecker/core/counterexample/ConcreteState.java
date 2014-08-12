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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.MutableARGPath;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/**
 * This class is used to represent the partial concrete memory of a C program at a statement
 * of the given counter-example path. It is used to calculate the concrete values of
 * the left hand side expressions in the assignments along the path.
 *
 * CPAs have to create an object of this class for every CFA Edge {@link CFAEdge}
 * along an Error Path {@link MutableARGPath} to create an object of
 * the concrete state path {@link ConcreteStatePath}. The allocator class
 * {@link AssignmentToEdgeAllocator} uses this object to create
 * an error path {@link CFAPathWithAssignments} where every assignment,
 * when possible, has a concrete value.
 *
 */
public final class ConcreteState {

  private final Map<LeftHandSide, Object> variables;
  private final Map<String, Memory> allocatedMemory;
  private final Map<LeftHandSide, Address> variableAddressMap;

  private final MemoryName memoryNameAllocator;

  /**
   * Creates an object of this class.
   *
   *
   * @param pVariables a map that assigns variables a concrete value, without the need to assign a concrete address to a variable.
   * @param pAllocatedMemory a map that assigns the allocated memory to its name.
   * @param pVariableAddressMap a map that assigns variables along the error path an unique address.
   * @param pMemoryName a class that, given a cfa expression {@link CRightHandSide},
   * calculate the memory that contains the value.
   */
  public ConcreteState(Map<LeftHandSide, Object> pVariables,
      Map<String, Memory> pAllocatedMemory,
      Map<LeftHandSide, Address> pVariableAddressMap,
      MemoryName pMemoryName) {
    variableAddressMap = ImmutableMap.copyOf(pVariableAddressMap);
    allocatedMemory = ImmutableMap.copyOf(pAllocatedMemory);
    variables = ImmutableMap.copyOf(pVariables);
    memoryNameAllocator = pMemoryName;
  }

  /**
   * Returns the concrete value of the given expression at the given address.
   * The allocated memory is determined by the given expression.
   *
   * @param exp return the concrete value of this C expression
   * @param address return the concrete value, that is stored at this address.
   * @return the concrete value at the given address of the given expression.
   */
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

  /**
   * Checks, whether this variable has a value assigned, independent of the address of
   * the variable. Directly storing a concrete value for a variable avoids having to
   * calculate the address for the variable. Having no concrete value directly assigned to
   * the variable does however not mean, that no concrete value can be calculated for this
   * variable.
   *
   * @param variable  Checks, whether this variable has a value assigned directly.
   * @return true, if a concrete value is directly assigned to this variable,
   *         false otherwise.
   */
  public boolean hasValueForLeftHandSide(LeftHandSide variable) {
    return variables.containsKey(variable);
  }

  /**
   * Get the concrete value directly stored at the given variable.
   * Directly storing a concrete value for a variable avoids having to
   * calculate the address for the variable. Having no concrete value directly assigned to
   * the variable does however not mean, that no concrete value can be calculated for this
   * variable.
   *
   * @param variable get the concrete value for this variable.
   * @return returns the concrete value of the given variable.
   */
  public Object getVariableValue(LeftHandSide variable) {
    Preconditions.checkArgument(variables.containsKey(variable));

    return variables.get(variable);
  }

  /**
   * Checks, whether the given variable has a concrete address.
   * A variable without a concrete address may have a value directly assigned
   * to the variable.
   *
   * @param variable check the concrete address of this variable.
   * @return true, if the given variable has a concrete address, false otherwise.
   */
  public boolean hasAddressOfVaribable(LeftHandSide variable) {
    return variableAddressMap.containsKey(variable);
  }

  /**
   * Get the concrete Address of the given variable.
   *
   * @param variable Get the concrete address of this variable.
   * @return returns the concrete address of the given variable.
   */
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