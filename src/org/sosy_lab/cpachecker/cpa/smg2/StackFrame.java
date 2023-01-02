// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;

/** Represents a C language stack frame. */
public final class StackFrame {
  public static final String RETVAL_LABEL = "___cpa_temp_result_var_";

  /** A mapping from variable names to a set of SMG objects, representing local variables. */
  private final PersistentMap<String, SMGObject> stackVariables;

  /** Function to which this stack frame belongs */
  private final CFunctionDeclaration stackFunction;
  /** An object to store function return value. The Object is Null if function has Void-type. */
  private final Optional<SMGObject> returnValueObject;

  /** var args are given when a function is called (if they are not the optional is empty). * */
  private final Optional<ImmutableList<Value>> variableArguments;

  private StackFrame(
      CFunctionDeclaration pDeclaration,
      PersistentMap<String, SMGObject> pVariables,
      Optional<SMGObject> pReturnValueObject,
      Optional<ImmutableList<Value>> pVariableArguments) {
    stackVariables = Preconditions.checkNotNull(pVariables);
    returnValueObject = pReturnValueObject;
    stackFunction = pDeclaration;
    variableArguments = pVariableArguments;
  }

  public StackFrame(CFunctionDeclaration pDeclaration, MachineModel pMachineModel) {
    stackVariables = PathCopyingPersistentTreeMap.of();
    stackFunction = pDeclaration;
    CType returnType = pDeclaration.getType().getReturnType().getCanonicalType();
    if (returnType instanceof CVoidType) {
      // use a plain int as return type for void functions
      returnValueObject = Optional.empty();
    } else {
      BigInteger returnValueSize = pMachineModel.getSizeofInBits(returnType);
      returnValueObject = Optional.of(SMGObject.of(0, returnValueSize, BigInteger.ZERO));
    }
    variableArguments = Optional.empty();
  }

  public StackFrame(
      CFunctionDeclaration pDeclaration,
      MachineModel pMachineModel,
      @Nullable ImmutableList<Value> pVariableArguments) {
    stackVariables = PathCopyingPersistentTreeMap.of();
    stackFunction = pDeclaration;
    CType returnType = pDeclaration.getType().getReturnType().getCanonicalType();
    if (returnType instanceof CVoidType) {
      // use a plain int as return type for void functions
      returnValueObject = Optional.empty();
    } else {
      BigInteger returnValueSize = pMachineModel.getSizeofInBits(returnType);
      returnValueObject = Optional.of(SMGObject.of(0, returnValueSize, BigInteger.ZERO));
    }
    variableArguments = Optional.ofNullable(pVariableArguments);
  }

  /**
   * Adds an SMG object pObj to a stack frame, representing variable pVariableName
   *
   * <p>Throws {@link IllegalArgumentException} when some object is already present with the name
   * pVariableName
   *
   * @param pVariableName A name of the variable
   * @param pObject An object to put into the stack frame
   */
  public StackFrame copyAndAddStackVariable(String pVariableName, SMGObject pObject) {
    Preconditions.checkArgument(
        !stackVariables.containsKey(pVariableName),
        "Stack frame for function already contains a variable '%s'",
        pVariableName);

    return new StackFrame(
        stackFunction,
        stackVariables.putAndCopy(pVariableName, pObject),
        returnValueObject,
        variableArguments);
  }

  public StackFrame copyAndRemoveVariable(String pName) {
    if (RETVAL_LABEL.equals(pName)) {
      // Do nothing for the moment
      return this;
    } else {
      return new StackFrame(
          stackFunction, stackVariables.removeAndCopy(pName), returnValueObject, variableArguments);
    }
  }

  /* ********************************************* */
  /* Non-modifying functions: getters and the like */
  /* ********************************************* */

  public CFunctionDeclaration getFunctionDefinition() {
    return stackFunction;
  }

  /**
   * Getter for obtaining an object corresponding to a variable name. Note: this is the object in
   * which the value for the variable is written (or not). You have to read it with the correct
   * type!
   *
   * <p>Throws {@link NoSuchElementException} when passed a name not present
   *
   * @param pName Variable name
   * @return SMG object corresponding to pName in the frame
   */
  public SMGObject getVariable(String pName) {

    if (pName.equals(RETVAL_LABEL) && returnValueObject.isPresent()) {
      return returnValueObject.orElseThrow();
    }

    Optional<SMGObject> to_return = Optional.ofNullable(stackVariables.get(pName));
    if (to_return.isEmpty()) {
      throw new NoSuchElementException(
          String.format("No variable with name '%s' in stack frame for function", pName));
    }
    return to_return.orElseThrow();
  }

  /**
   * Returns true if variable pName is present, false otherwise.
   *
   * @param pName Variable name
   */
  public boolean containsVariable(String pName) {
    if (pName.equals(RETVAL_LABEL)) {
      return returnValueObject.isPresent();
    } else {
      return stackVariables.containsKey(pName);
    }
  }

  /** Returns a mapping from variables name to SMGObjects. */
  public Map<String, SMGObject> getVariables() {
    return stackVariables;
  }

  /** Returns a set of all objects: return value object, variables, parameters. */
  public FluentIterable<SMGObject> getAllObjects() {
    if (returnValueObject.isPresent()) {
      return FluentIterable.concat(
          stackVariables.values(), ImmutableSet.of(returnValueObject.orElseThrow()));
    }
    return FluentIterable.from(stackVariables.values());
  }

  /** Returns an {@link SMGObject} reserved for function return value. */
  public Optional<SMGObject> getReturnObject() {
    return returnValueObject;
  }

  /** returns true if stack contains the given variable, else false. */
  public boolean hasVariable(String var) {
    return stackVariables.containsKey(var);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (Entry<String, SMGObject> entry : stackVariables.entrySet()) {
      builder.append(entry.getKey() + " -> " + entry.getValue() + "  ");
    }
    if (variableArguments.isPresent()) {
      builder.append("variable arguments:");
      for (Value value : variableArguments.orElseThrow()) {
        builder.append(" " + value + "  ");
      }
    }
    if (returnValueObject.isPresent()) {
      builder.append(" return object:" + " -> " + returnValueObject.orElseThrow() + "  ");
    }
    return builder.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof StackFrame)) {
      return false;
    }
    StackFrame other = (StackFrame) o;
    return Objects.equals(stackVariables, other.stackVariables)
        && Objects.equals(stackFunction, other.stackFunction)
        && Objects.equals(returnValueObject, other.returnValueObject)
        && Objects.equals(variableArguments, other.variableArguments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(stackVariables, stackFunction, returnValueObject, variableArguments);
  }

  public boolean hasVariableArguments() {
    return variableArguments.isPresent();
  }

  public ImmutableList<Value> getVariableArguments() {
    return variableArguments.orElseThrow();
  }

  public StackFrame copyWith(
      Optional<SMGObject> pReturnOptional, Map<String, SMGObject> pFrameMapping) {
    return new StackFrame(
        stackFunction,
        PathCopyingPersistentTreeMap.copyOf(pFrameMapping),
        pReturnOptional,
        variableArguments);
  }
}
