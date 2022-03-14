// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.counterexample;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.ACastExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.TypeHandlerWithPointerAliasing;

/**
 * This class is used to represent the partial concrete memory of a C program at a statement of the
 * given counter-example path. It is used to calculate the concrete values of the left hand side
 * expressions in the assignments along the path.
 *
 * <p>CPAs have to create an object of this class for every CFA Edge {@link CFAEdge} along an Error
 * Path {@link ARGPath} to create an object of the concrete state path {@link ConcreteStatePath}.
 * The allocator class {@link AssumptionToEdgeAllocator} uses this object to create an error path
 * {@link CFAPathWithAssumptions} where every assignment, when possible, has a concrete value.
 */
public final class ConcreteState {

  private static final ConcreteState EMPTY_CONCRETE_STATE = new ConcreteState();

  private final Map<LeftHandSide, Object> variables;
  private final Map<String, Memory> allocatedMemory;
  private final Map<LeftHandSide, Address> variableAddressMap;
  private final ConcreteExpressionEvaluator analysisConcreteExpressionEvaluation;
  private final MemoryName memoryNameAllocator;
  @Nullable private final MachineModel machineModel;

  /**
   * Creates an object of this class.
   *
   * @param pVariables a map that assigns variables a concrete value, without the need to assign a
   *     concrete address to a variable.
   * @param pAllocatedMemory a map that assigns the allocated memory to its name.
   * @param pVariableAddressMap a map that assigns variables along the error path an unique address.
   * @param pMemoryName a class that, given a cfa expression {@link CRightHandSide}, calculate the
   *     memory that contains the value.
   */
  public ConcreteState(
      Map<LeftHandSide, Object> pVariables,
      Map<String, Memory> pAllocatedMemory,
      Map<LeftHandSide, Address> pVariableAddressMap,
      MemoryName pMemoryName,
      MachineModel pMachineModel) {
    variableAddressMap = ImmutableMap.copyOf(pVariableAddressMap);
    allocatedMemory = ImmutableMap.copyOf(pAllocatedMemory);
    variables = ImmutableMap.copyOf(pVariables);
    memoryNameAllocator = pMemoryName;
    machineModel = pMachineModel;
    analysisConcreteExpressionEvaluation = new DefaultConcreteExpressionEvaluator();
  }

  /**
   * Creates an object of this class.
   *
   * @param pVariables a map that assigns variables a concrete value, without the need to assign a
   *     concrete address to a variable.
   * @param pAllocatedMemory a map that assigns the allocated memory to its name.
   * @param pVariableAddressMap a map that assigns variables along the error path an unique address.
   * @param pMemoryName a class that, given a cfa expression {@link CRightHandSide}, calculate the
   *     memory that contains the value.
   */
  public ConcreteState(
      Map<LeftHandSide, Object> pVariables,
      Map<String, Memory> pAllocatedMemory,
      Map<LeftHandSide, Address> pVariableAddressMap,
      MemoryName pMemoryName,
      ConcreteExpressionEvaluator pAnalysisConcreteExpressionEvaluation,
      MachineModel pMachineModel) {
    variableAddressMap = ImmutableMap.copyOf(pVariableAddressMap);
    allocatedMemory = ImmutableMap.copyOf(pAllocatedMemory);
    variables = ImmutableMap.copyOf(pVariables);
    memoryNameAllocator = pMemoryName;
    analysisConcreteExpressionEvaluation = pAnalysisConcreteExpressionEvaluation;
    machineModel = pMachineModel;
  }

  private ConcreteState() {
    variableAddressMap = ImmutableMap.of();
    allocatedMemory = ImmutableMap.of();
    variables = ImmutableMap.of();
    memoryNameAllocator = (pExp) -> "";
    analysisConcreteExpressionEvaluation = new DefaultConcreteExpressionEvaluator();
    machineModel = null;
  }

  /**
   * Creates an object of this class.
   *
   * @param pVariables a map that assigns variables a concrete value, without the need to assign a
   *     concrete address to a variable.
   * @param pAllocatedMemory a map that assigns the allocated memory to its name.
   * @param pVariableAddressMap a map that assigns variables along the error path an unique address.
   * @param pMemoryName a class that, given a cfa expression {@link CRightHandSide}, calculate the
   *     memory that contains the value.
   */
  public ConcreteState(
      Map<LeftHandSide, Object> pVariables,
      Map<String, Memory> pAllocatedMemory,
      Map<LeftHandSide, Address> pVariableAddressMap,
      MemoryName pMemoryName) {
    variableAddressMap = ImmutableMap.copyOf(pVariableAddressMap);
    allocatedMemory = ImmutableMap.copyOf(pAllocatedMemory);
    variables = ImmutableMap.copyOf(pVariables);
    memoryNameAllocator = pMemoryName;
    analysisConcreteExpressionEvaluation = new DefaultConcreteExpressionEvaluator();
    machineModel = null;
  }

  /**
   * Returns the concrete value of the given expression at the given address. The allocated memory
   * is determined by the given expression.
   *
   * @param exp return the concrete value of this C expression
   * @param address return the concrete value, that is stored at this address.
   * @return the concrete value at the given address of the given expression.
   */
  public @Nullable Object getValueFromMemory(CRightHandSide exp, Address address) {

    String memoryName = memoryNameAllocator.getMemoryName(exp);

    if (!allocatedMemory.containsKey(memoryName)) {
      return null;
    }

    Memory memory = allocatedMemory.get(memoryName);

    if (TypeHandlerWithPointerAliasing.isByteArrayAccessName(memoryName)) {
      Preconditions.checkArgument(
          machineModel != null,
          "Sound computation of heap values with byte array encodings requires the machine model.");

      // For ByteArray heap encoding actual values needs to be computed.
      int typeSizeInByte = machineModel.getSizeof(exp.getExpressionType()).intValueExact();
      // TODO missing handling for unaligned bitfields
      if (typeSizeInByte == 1) {
        if (memory.hasValue(address)) {
          // no special handling needed for byte size values
          return memory.getValue(address);
        }
        return null;
      }
      // read bytes one by one
      int offset = 0;
      int ret = 0;
      boolean memoryHasValue = false;
      while (offset < typeSizeInByte) {
        Address addressWithOffset = address.addOffset(BigInteger.valueOf(offset));
        // handle don't care bytes optimized by SMT solver
        BigInteger byteJunk;
        if (memory.hasValue(addressWithOffset)) {
          memoryHasValue = true;
          byteJunk = (BigInteger) memory.getValue(addressWithOffset);
        } else {
          byteJunk = BigInteger.ZERO;
        }
        int shiftBy;
        // handle endianness
        if (machineModel.getEndianness() == ByteOrder.BIG_ENDIAN) {
          shiftBy = 8 * (typeSizeInByte - 1 - offset++);
        } else {
          shiftBy = 8 * offset++;
        }
        // bitwise left shift byte junks and adding up return value
        ret += byteJunk.intValueExact() << shiftBy;
      }
      // return null if all bytes are don't cares
      return memoryHasValue ? BigInteger.valueOf(ret) : null;
    }
    if (memory.hasValue(address)) {
      return memory.getValue(address);
    }

    return null;
  }

  /**
   * Checks, whether this variable has a value assigned, independent of the address of the variable.
   * Directly storing a concrete value for a variable avoids having to calculate the address for the
   * variable. Having no concrete value directly assigned to the variable does however not mean,
   * that no concrete value can be calculated for this variable.
   *
   * @param variable Checks, whether this variable has a value assigned directly.
   * @return true, if a concrete value is directly assigned to this variable, false otherwise.
   */
  public boolean hasValueForLeftHandSide(LeftHandSide variable) {
    return variables.containsKey(variable);
  }

  /**
   * Get the concrete value directly stored at the given variable. Directly storing a concrete value
   * for a variable avoids having to calculate the address for the variable. Having no concrete
   * value directly assigned to the variable does however not mean, that no concrete value can be
   * calculated for this variable.
   *
   * @param variable get the concrete value for this variable.
   * @return returns the concrete value of the given variable.
   */
  public Object getVariableValue(LeftHandSide variable) {
    Preconditions.checkArgument(variables.containsKey(variable));

    return variables.get(variable);
  }

  /**
   * Get the concrete expression evaluation of the analysis. Used to evaluate expressions while
   * calculating the counterexample of the analysis. Necessary, because different analysis calculate
   * concrete expressions differently.
   *
   * @return class that can evaluate concrete expressions, if the concrete operands are given.
   */
  public ConcreteExpressionEvaluator getAnalysisConcreteExpressionEvaluation() {
    return analysisConcreteExpressionEvaluation;
  }

  /**
   * Checks, whether the given variable has a concrete address. A variable without a concrete
   * address may have a value directly assigned to the variable.
   *
   * @param variable check the concrete address of this variable.
   * @return true, if the given variable has a concrete address, false otherwise.
   */
  public boolean hasAddressOfVariable(LeftHandSide variable) {
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
  @SuppressFBWarnings("EQ_UNUSUAL")
  public boolean equals(Object obj) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return "variables="
        + variables
        + System.lineSeparator()
        + "allocatedMemory="
        + allocatedMemory
        + System.lineSeparator()
        + " variableAddressMap="
        + variableAddressMap;
  }

  /**
   * Return an Empty Concrete State.
   *
   * @return an empty concrete State.
   */
  public static ConcreteState empty() {
    return EMPTY_CONCRETE_STATE;
  }

  private static class DefaultConcreteExpressionEvaluator implements ConcreteExpressionEvaluator {

    @Override
    public boolean shouldEvaluateExpressionWithThisEvaluator(AExpression pExp) {
      return false;
    }

    private Value throwUnsupportedOperationException(AExpression pExp) {
      throw new UnsupportedOperationException(
          pExp.toASTString() + "should not be evaluated with this class.");
    }

    @Override
    public Value evaluate(ABinaryExpression pBinExp, Value pOp1, Value pOp2) {
      return throwUnsupportedOperationException(pBinExp);
    }

    @Override
    public Value evaluate(AUnaryExpression pUnaryExpression, Value pOperand) {
      return throwUnsupportedOperationException(pUnaryExpression);
    }

    @Override
    public Value evaluate(ACastExpression pCastExpression, Value pOperand) {
      return throwUnsupportedOperationException(pCastExpression);
    }
  }
}
