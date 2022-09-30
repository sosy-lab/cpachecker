// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.refiner;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentStack;
import org.sosy_lab.cpachecker.cpa.smg2.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.smg2.SymbolicProgramConfiguration;
import org.sosy_lab.cpachecker.cpa.smg2.util.CFunctionDeclarationAndOptionalValue;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMG2Exception;
import org.sosy_lab.cpachecker.cpa.smg2.util.ValueAndValueSize;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.refinement.Interpolant;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * This class represents a SMG-Value-Analysis interpolant, itself, just a mere wrapper around a map
 * from memory locations to values, representing a variable assignment.
 */
public final class SMGInterpolant implements Interpolant<SMGState, SMGInterpolant> {

  /** the variable assignment of the interpolant */
  private final @Nullable PersistentMap<MemoryLocation, ValueAndValueSize> nonHeapAssignments;

  private final @Nullable Map<String, BigInteger> variableNameToMemorySizeInBits;

  private final @Nullable Map<String, CType> variableToTypeMap;

  private final @Nullable PersistentStack<CFunctionDeclarationAndOptionalValue>
      stackFrameDeclarations;

  private final @Nullable Set<Value> allowedHeapValues;

  private @Nullable SymbolicProgramConfiguration memoryModel;

  private final SMGOptions options;
  private final MachineModel machineModel;
  private final LogManager logger;

  // We need this because stackFrameDeclarations must not always exist, this does!
  private final CFunctionDeclaration cfaEntryFunctionDeclaration;

  /** Constructor for a new, empty interpolant, i.e. the interpolant representing "true" */
  private SMGInterpolant(
      SMGOptions pOptions,
      MachineModel pMachineModel,
      LogManager pLogger,
      CFunctionDeclaration pCFAEntryFunctionDef) {
    options = pOptions;
    machineModel = pMachineModel;
    logger = pLogger;
    nonHeapAssignments = PathCopyingPersistentTreeMap.of();
    variableNameToMemorySizeInBits = new HashMap<>();
    variableToTypeMap = new HashMap<>();
    stackFrameDeclarations =
        PersistentStack.<CFunctionDeclarationAndOptionalValue>of()
            .pushAndCopy(
                CFunctionDeclarationAndOptionalValue.of(pCFAEntryFunctionDef, Optional.empty()));
    cfaEntryFunctionDeclaration = pCFAEntryFunctionDef;
    allowedHeapValues = ImmutableSet.of();
    memoryModel =
        SymbolicProgramConfiguration.of(BigInteger.valueOf(pMachineModel.getSizeofPtrInBits()));
  }

  /**
   * Constructor for a new interpolant representing the given non heap variable assignment, the var
   * name to type and name maps and the stack frame stack. Note the stack frame stack is expected to
   * be reversed, that the main function is on top.
   *
   * @param pNonHeapAssignments the variable assignment to be represented by the interpolant
   */
  public SMGInterpolant(
      SMGOptions pOptions,
      MachineModel pMachineModel,
      LogManager pLogger,
      PersistentMap<MemoryLocation, ValueAndValueSize> pNonHeapAssignments,
      Map<String, BigInteger> pVariableNameToMemorySizeInBits,
      Map<String, CType> pVariableToTypeMap,
      PersistentStack<CFunctionDeclarationAndOptionalValue> pStackFrameDeclarations,
      CFunctionDeclaration pCfaEntryFunDecl,
      Set<Value> pAllowedHeapValues,
      SymbolicProgramConfiguration memMod) {
    options = pOptions;
    machineModel = pMachineModel;
    logger = pLogger;
    nonHeapAssignments = pNonHeapAssignments;
    variableNameToMemorySizeInBits = pVariableNameToMemorySizeInBits;
    variableToTypeMap = pVariableToTypeMap;
    Preconditions.checkArgument(
        pStackFrameDeclarations == null
            || ((pStackFrameDeclarations.size() >= 1)
                && hasEntryFunDef(pStackFrameDeclarations, pCfaEntryFunDecl)));
    stackFrameDeclarations = pStackFrameDeclarations;
    Preconditions.checkNotNull(pCfaEntryFunDecl);
    cfaEntryFunctionDeclaration = pCfaEntryFunDecl;
    allowedHeapValues = pAllowedHeapValues;
    memoryModel = memMod;
  }

  // For UseDefInterpolation
  // No mem model, -> use pNonHeapAssignments to build a mem model
  public SMGInterpolant(
      SMGOptions pOptions,
      MachineModel pMachineModel,
      LogManager pLogger,
      PersistentMap<MemoryLocation, ValueAndValueSize> pNonHeapAssignments,
      Map<String, BigInteger> pVariableNameToMemorySizeInBits,
      Map<String, CType> pVariableToTypeMap,
      PersistentStack<CFunctionDeclarationAndOptionalValue> pStackFrameDeclarations,
      CFunctionDeclaration pCfaEntryFunDecl,
      Set<Value> pAllowedHeapValues) {
    options = pOptions;
    machineModel = pMachineModel;
    logger = pLogger;
    nonHeapAssignments = pNonHeapAssignments;
    variableNameToMemorySizeInBits = pVariableNameToMemorySizeInBits;
    variableToTypeMap = pVariableToTypeMap;
    Preconditions.checkArgument(
        pStackFrameDeclarations == null
            || ((pStackFrameDeclarations.size() >= 1)
                && hasEntryFunDef(pStackFrameDeclarations, pCfaEntryFunDecl)));
    stackFrameDeclarations = pStackFrameDeclarations;
    Preconditions.checkNotNull(pCfaEntryFunDecl);
    cfaEntryFunctionDeclaration = pCfaEntryFunDecl;
    allowedHeapValues = pAllowedHeapValues;
    try {
      memoryModel =
          SMGState.of(
                  machineModel,
                  SymbolicProgramConfiguration.of(
                      BigInteger.valueOf(pMachineModel.getSizeofPtrInBits())),
                  logger,
                  options,
                  ImmutableList.of())
              .reconstructStackFrames(stackFrameDeclarations)
              .reconstructSMGStateFromNonHeapAssignments(
                  nonHeapAssignments,
                  variableNameToMemorySizeInBits,
                  variableToTypeMap,
                  stackFrameDeclarations)
              .getMemoryModel();
    } catch (SMG2Exception e) {
      memoryModel =
          SymbolicProgramConfiguration.of(BigInteger.valueOf(pMachineModel.getSizeofPtrInBits()));
    }
  }

  // Precondition
  private boolean hasEntryFunDef(
      PersistentStack<CFunctionDeclarationAndOptionalValue> pStackFrameDeclarations,
      CFunctionDeclaration pCfaEntryFunDecl) {
    CFunctionDeclaration firstDef = null;
    for (CFunctionDeclarationAndOptionalValue fundef : pStackFrameDeclarations) {
      firstDef = fundef.getCFunctionDeclaration();
      break;
    }
    Preconditions.checkNotNull(firstDef);
    return (firstDef == pCfaEntryFunDecl);
  }

  /**
   * This method serves as factory method for an initial, i.e. an interpolant representing "true"
   */
  public static SMGInterpolant createInitial(
      SMGOptions pOptions,
      MachineModel pMachineModel,
      LogManager pLogger,
      CFunctionEntryNode cfaFuncEntryNode) {
    return new SMGInterpolant(
        pOptions, pMachineModel, pLogger, cfaFuncEntryNode.getFunctionDefinition());
  }

  /** the interpolant representing "true" */
  public static SMGInterpolant createTRUE(
      SMGOptions pOptions,
      MachineModel pMachineModel,
      LogManager pLogger,
      CFunctionEntryNode cfaFuncEntryNode) {
    return createInitial(pOptions, pMachineModel, pLogger, cfaFuncEntryNode);
  }

  /** Keeps interpolant information (i.e. "true") but copies and adds stack frame information. */
  public SMGInterpolant addStackFrameInformationAndCopy(SMGState stateForFrameInfo) {
    return new SMGInterpolant(
        options,
        machineModel,
        logger,
        nonHeapAssignments,
        variableNameToMemorySizeInBits,
        variableToTypeMap,
        stateForFrameInfo.getMemoryModel().getFunctionDeclarationsFromStackFrames(),
        cfaEntryFunctionDeclaration,
        allowedHeapValues,
        memoryModel);
  }

  /** the interpolant representing "false" */
  public static SMGInterpolant createFALSE(
      SMGOptions pOptions,
      MachineModel pMachineModel,
      LogManager pLogger,
      CFunctionDeclaration cfaEntryFuncDef) {
    return new SMGInterpolant(
        pOptions, pMachineModel, pLogger, null, null, null, null, cfaEntryFuncDef, null, null);
  }

  @Override
  public Set<MemoryLocation> getMemoryLocations() {
    return isFalse() ? ImmutableSet.of() : Collections.unmodifiableSet(nonHeapAssignments.keySet());
  }

  /**
   * This method joins to value-analysis interpolants. If the underlying map contains different
   * values for a key contained in both maps, the behaviour is undefined.
   *
   * @param other the value-analysis interpolant to join with this one
   * @return a new value-analysis interpolant containing the joined mapping of this and the other
   *     value-analysis interpolant
   */
  @Override
  public SMGInterpolant join(final SMGInterpolant other) {
    // We expect that if nonHeapAssignments != null all other nullables are not null also except for
    // maybe the state!
    if (nonHeapAssignments == null || other.nonHeapAssignments == null || memoryModel == null) {
      return createFALSE(options, machineModel, logger, cfaEntryFunctionDeclaration);
    }

    // add other itp mapping - one by one for now, to check for correctness
    // newAssignment.putAll(other.assignment);
    PersistentMap<MemoryLocation, ValueAndValueSize> newAssignment = nonHeapAssignments;
    for (Entry<MemoryLocation, ValueAndValueSize> entry : other.nonHeapAssignments.entrySet()) {
      if (newAssignment.containsKey(entry.getKey())) {
        assert entry.getValue().equals(other.nonHeapAssignments.get(entry.getKey()))
            : "interpolants mismatch in " + entry.getKey();
      }
      newAssignment = newAssignment.putAndCopy(entry.getKey(), entry.getValue());
      String thisEntryQualName = entry.getKey().getQualifiedName();
      variableNameToMemorySizeInBits.put(
          thisEntryQualName, other.variableNameToMemorySizeInBits.get(thisEntryQualName));
      variableToTypeMap.put(thisEntryQualName, other.variableToTypeMap.get(thisEntryQualName));

      assert Objects.equals(
              entry.getValue().getSizeInBits(),
              other.nonHeapAssignments.get(entry.getKey()).getSizeInBits())
          : "interpolants mismatch in " + entry.getKey();
    }

    @Nullable PersistentStack<CFunctionDeclarationAndOptionalValue> stackFrameDecl =
        stackFrameDeclarations;
    if (stackFrameDecl == null
        || (other.stackFrameDeclarations != null
            && other.stackFrameDeclarations.size() > stackFrameDecl.size())) {
      stackFrameDecl = other.stackFrameDeclarations;
    }

    return new SMGInterpolant(
        options,
        machineModel,
        logger,
        newAssignment,
        variableNameToMemorySizeInBits,
        variableToTypeMap,
        stackFrameDecl,
        cfaEntryFunctionDeclaration,
        ImmutableSet.<Value>builder()
            .addAll(allowedHeapValues)
            .addAll(other.allowedHeapValues)
            .build(),
        memoryModel);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(nonHeapAssignments);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }

    SMGInterpolant other = (SMGInterpolant) obj;
    // technically this is not correct as we leave out the heap. But thats ok for now.
    return Objects.equals(nonHeapAssignments, other.nonHeapAssignments)
        && Objects.equals(allowedHeapValues, other.allowedHeapValues);
  }

  /**
   * The method checks for trueness of the interpolant.
   *
   * @return true, if the interpolant represents "true", else false
   */
  @Override
  public boolean isTrue() {
    return !isFalse() && nonHeapAssignments.isEmpty() && allowedHeapValues.isEmpty();
  }

  /**
   * The method checks for falseness of the interpolant.
   *
   * @return true, if the interpolant represents "false", else true
   */
  @Override
  public boolean isFalse() {
    return nonHeapAssignments == null && allowedHeapValues == null;
  }

  public Set<Value> getAllowedHeapValues() {
    if (allowedHeapValues == null) {
      return ImmutableSet.of();
    }
    return allowedHeapValues;
  }

  /**
   * This method serves as factory method to create a smg2 state from the interpolant
   *
   * @return a smg2 state that represents the same variable assignment as the interpolant
   */
  @Override
  public SMGState reconstructState() {
    if (nonHeapAssignments == null) {
      throw new IllegalStateException("Can't reconstruct state from FALSE-interpolant");
    } else {
      // TODO: heap?
      return SMGState.of(machineModel, memoryModel, logger, options, ImmutableList.of())
          .reconstructStackFrames(stackFrameDeclarations);
      /*.reconstructSMGStateFromNonHeapAssignments(
      nonHeapAssignments,
      variableNameToMemorySizeInBits,
      variableToTypeMap,
      stackFrameDeclarations,
      allowedHeapValues);*/
    }
  }

  @Override
  public String toString() {
    if (isFalse()) {
      return "FALSE";
    }

    if (isTrue()) {
      return "TRUE";
    }

    return nonHeapAssignments.toString() + "\n" + "Allowed heap values: " + allowedHeapValues;
  }

  // TODO: currently not used. Originally used in IMPACT refinement. Would need to be edited to be
  // immutable, that it returns the new state. We would also need to reconstruct the stack frames!
  /*
    public boolean strengthen(SMGState state, ARGState argState) {
      if (isTrivial()) {
        return false;
      }

      boolean strengthened = false;
      SMGState currentState = state;

      for (Entry<MemoryLocation, ValueAndValueSize> itp : nonHeapAssignments.entrySet()) {
        if (!currentState.isLocalOrGlobalVariablePresent(itp.getKey())) {
          try {
            SMGState.of(machineModel, logger, options, cfaEntryFunctionDeclaration)
                .assignNonHeapConstant(
                    itp.getKey(), itp.getValue(), variableNameToMemorySizeInBits, variableToTypeMap);
          } catch (SMG2Exception e) {
            // Critical error
            throw new RuntimeException(e);
          }
          strengthened = true;

        } else {
          verify(
              currentState.verifyVariableEqualityWithValueAt(itp.getKey(), itp.getValue()),
              "state and interpolant do not match in value for variable %s [state = %s != %s = itp]"
                  + " for state %s",
              itp.getKey(),
              currentState.getValueToVerify(itp.getKey(), itp.getValue()),
              itp.getValue(),
              argState.getStateId());
        }
      }

      return strengthened;
    }
  */

  /**
   * This method weakens the interpolant to the given set of memory location identifiers.
   *
   * <p>As the information on what to retain is derived in a static syntactical analysis, the set to
   * retain is a collection of memory location identifiers, instead of {@link MemoryLocation}s, as
   * offsets cannot be provided.
   *
   * @param toRetain the set of memory location identifiers to retain in the interpolant.
   * @return the weakened interpolant
   */
  @SuppressWarnings("ConstantConditions") // isTrivial() checks for FALSE-interpolants
  public SMGInterpolant weaken(Set<String> toRetain) {
    if (isTrivial()) {
      return this;
    }

    PersistentMap<MemoryLocation, ValueAndValueSize> weakenedAssignments = nonHeapAssignments;
    for (MemoryLocation current : nonHeapAssignments.keySet()) {
      if (!toRetain.contains(current.getExtendedQualifiedName())) {
        weakenedAssignments = weakenedAssignments.removeAndCopy(current);
        variableNameToMemorySizeInBits.remove(current.getQualifiedName());
        // We don't delete types out of variableToTypeMap because in for example arrays we have
        // multiple entries, one for each offset. We don't want to delete the type because of that.
        // If it were to change it would be overridden.
      }
    }

    return new SMGInterpolant(
        options,
        machineModel,
        logger,
        weakenedAssignments,
        variableNameToMemorySizeInBits,
        variableToTypeMap,
        stackFrameDeclarations,
        cfaEntryFunctionDeclaration,
        allowedHeapValues,
        memoryModel);
  }

  @SuppressWarnings("ConstantConditions") // isTrivial() asserts that assignment != null
  @Override
  public int getSize() {
    return isTrivial() ? 0 : nonHeapAssignments.size();
  }

  /**
   * Interal sanity check for the interpolant.
   *
   * @return true if the interpolant makes sense.
   */
  public boolean isSanityIntact() {
    ImmutableSet.Builder<String> availableFunctionsBuilder = ImmutableSet.builder();
    for (CFunctionDeclarationAndOptionalValue fundef : stackFrameDeclarations) {
      availableFunctionsBuilder.add(fundef.getCFunctionDeclaration().getQualifiedName());
    }
    ImmutableSet<String> availableFunctions = availableFunctionsBuilder.build();
    for (MemoryLocation assignment : nonHeapAssignments.keySet()) {
      if (assignment.isOnFunctionStack()) {
        if (!availableFunctions.contains(assignment.getFunctionName())) {
          return false;
        }
      }
    }
    return true;
  }
}
