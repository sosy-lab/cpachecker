// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;
import static org.sosy_lab.cpachecker.util.smg.join.SMGRecoverableFailure.DELAYED_MERGE;
import static org.sosy_lab.cpachecker.util.smg.join.SMGRecoverableFailure.LEFT_LIST_LONGER;
import static org.sosy_lab.cpachecker.util.smg.join.SMGRecoverableFailure.RIGHT_LIST_LONGER;

import com.google.common.base.Equivalence;
import com.google.common.base.Equivalence.Wrapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.annotation.Nonnull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cpa.smg2.SMGOptions.SMGExportLevel;
import org.sosy_lab.cpachecker.cpa.smg2.abstraction.SMGCPAMaterializer;
import org.sosy_lab.cpachecker.cpa.smg2.constraint.ConstantSymbolicExpressionLocator;
import org.sosy_lab.cpachecker.cpa.smg2.util.CFunctionDeclarationAndOptionalValue;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGAndSMGObjects;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGHasValueEdgesAndSPC;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectsAndValues;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.SPCAndSMGObjects;
import org.sosy_lab.cpachecker.cpa.smg2.util.ValueAndValueSize;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueWrapper;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.datastructures.PersistentSet;
import org.sosy_lab.cpachecker.util.smg.datastructures.PersistentStack;
import org.sosy_lab.cpachecker.util.smg.graph.SMGDoublyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGNode;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGSinglyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;
import org.sosy_lab.cpachecker.util.smg.join.SMGMergeStatus;
import org.sosy_lab.cpachecker.util.smg.join.SMGMergeStatusOrRecoverableFailure;
import org.sosy_lab.cpachecker.util.smg.join.SMGRecoverableFailure;
import org.sosy_lab.cpachecker.util.smg.util.MergedSPCAndMapping;
import org.sosy_lab.cpachecker.util.smg.util.MergedSPCAndMergeStatus;
import org.sosy_lab.cpachecker.util.smg.util.MergedSPCAndMergeStatusWithMergingSPCsAndMapping;
import org.sosy_lab.cpachecker.util.smg.util.MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue;
import org.sosy_lab.cpachecker.util.smg.util.MergedSPCWithMappingsAndAddressValue;
import org.sosy_lab.cpachecker.util.smg.util.MergingSPCsAndMergeStatus;
import org.sosy_lab.cpachecker.util.smg.util.SMGAndHasValueEdges;
import org.sosy_lab.cpachecker.util.smg.util.SMGAndSMGValues;
import org.sosy_lab.cpachecker.util.smg.util.SMGObjectMergeTriple;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * This class models the memory with its global/heap/stack variables. Its idea is that we handle
 * only SMG specific stuff here, and have already transformed all CPA and C (or other) specific
 * stuff. This class splits the values by types, the values in themselves are value ranges or
 * addresses. Also, we handle (in)equality for the value analysis here, as this is more delicate
 * than just ==, <, > etc. see proveInequality() for more info. Variable ranges are needed for the
 * abstraction (join of SMGs) to succeed more often. The abstraction might however merge SMG objects
 * and value ranges making read and equality non-trivial.
 */
public class SymbolicProgramConfiguration {

  // Buffer in between numeric memory assumptions. See currentMemoryAssumptionMax for more info.
  // TODO: make this changeable in an option
  private BigInteger NUMERIC_MEMORY_BUFFER = BigInteger.valueOf(512);

  /** The SMG modelling this memory image. */
  private final SMG smg;

  /** Mapping of all global variables to their SMGObjects. Use the value mapping to get values. */
  private final PersistentMap<String, SMGObject> globalVariableMapping;

  /* Stack with atexit() handlers */
  private final PersistentStack<Value> atExitStack;

  /* The stack of stackFrames.
   * (Each function call creates a new one that has to be popd once the function is returned/ends)
   */
  private final PersistentStack<StackFrame> stackVariableMapping;

  /* Remember the types of variables for precision adjustments */
  private final PersistentMap<String, CType> variableToTypeMap;

  /* (SMG)Objects on the heap. */
  private final PersistentSet<SMGObject> heapObjects;

  /*
   * To be able to cast addresses to numbers etc., we assume some value for them and calculate
   * changes based on it. Each memory section gets a unique number when created.
   */
  private final PersistentMap<SMGObject, BigInteger> memoryAddressAssumptionsMap;
  private BigInteger currentMemoryAssumptionMax = BigInteger.ZERO;

  /* Map of (SMG)Objects externally allocated.
   * The bool denotes validity, true = valid, false = invalid i.e. after free()
   */
  private final PersistentMap<SMGObject, Boolean> externalObjectAllocation;

  /*
   * This map remembers which SMGObjects are created using malloc(0).
   * Reason: they are flagged as invalidated and treated as such, except for free() calls.
   * Those are valid.
   */
  private final PersistentMap<SMGObject, Boolean> mallocZeroMemory;

  /**
   * Maps the symbolic value ranges to their abstract SMG counterparts. (SMGs use only abstract, but
   * unique values. Such that a SMGValue with id 1 is always equal only with a SMGValue with id 1.
   * The only exception are addresses, hence why they are separate) . Important: You NEED to map the
   * SMGValue using the mapping of the SPC!
   */
  private final ImmutableBiMap<Equivalence.Wrapper<Value>, SMGValue> valueMapping;

  // Needed for merging values (and creation of the symbolic values in the process)
  private final PersistentMap<SMGValue, CType> valueToTypeMap;

  private static final ValueWrapper valueWrapper = new ValueWrapper();

  // Throws exception on reading this object (i.e. because we know we can't handle this)
  private Set<SMGObject> readBlacklist;

  private SymbolicProgramConfiguration(
      SMG pSmg,
      PersistentMap<String, SMGObject> pGlobalVariableMapping,
      PersistentStack<Value> pAtExitStack,
      PersistentStack<StackFrame> pStackVariableMapping,
      PersistentSet<SMGObject> pHeapObjects,
      PersistentMap<SMGObject, Boolean> pExternalObjectAllocation,
      ImmutableBiMap<Equivalence.Wrapper<Value>, SMGValue> pValueMapping,
      PersistentMap<String, CType> pVariableToTypeMap,
      PersistentMap<SMGObject, BigInteger> pMemoryAddressAssumptionMap,
      PersistentMap<SMGObject, Boolean> pMallocZeroMemory) {
    globalVariableMapping = pGlobalVariableMapping;
    stackVariableMapping = pStackVariableMapping;
    atExitStack = pAtExitStack;
    smg = pSmg;
    externalObjectAllocation = pExternalObjectAllocation;
    heapObjects = pHeapObjects;
    valueMapping = pValueMapping;
    variableToTypeMap = pVariableToTypeMap;
    memoryAddressAssumptionsMap = pMemoryAddressAssumptionMap;
    mallocZeroMemory = pMallocZeroMemory;
    readBlacklist = ImmutableSet.of();
    valueToTypeMap =
        PathCopyingPersistentTreeMap.<SMGValue, CType>of()
            .putAndCopy(SMGValue.zeroValue(), CNumericTypes.INT)
            .putAndCopy(SMGValue.zeroDoubleValue(), CNumericTypes.INT)
            .putAndCopy(SMGValue.zeroFloatValue(), CNumericTypes.INT);
  }

  private SymbolicProgramConfiguration(
      SMG pSmg,
      PersistentMap<String, SMGObject> pGlobalVariableMapping,
      PersistentStack<Value> pAtExitStack,
      PersistentStack<StackFrame> pStackVariableMapping,
      PersistentSet<SMGObject> pHeapObjects,
      PersistentMap<SMGObject, Boolean> pExternalObjectAllocation,
      ImmutableBiMap<Equivalence.Wrapper<Value>, SMGValue> pValueMapping,
      PersistentMap<String, CType> pVariableToTypeMap,
      PersistentMap<SMGObject, BigInteger> pMemoryAddressAssumptionMap,
      PersistentMap<SMGObject, Boolean> pMallocZeroMemory,
      Set<SMGObject> pReadBlacklist,
      PersistentMap<SMGValue, CType> pValueToTypeMap) {
    globalVariableMapping = pGlobalVariableMapping;
    stackVariableMapping = pStackVariableMapping;
    atExitStack = pAtExitStack;
    smg = pSmg;
    externalObjectAllocation = pExternalObjectAllocation;
    heapObjects = pHeapObjects;
    valueMapping = pValueMapping;
    variableToTypeMap = pVariableToTypeMap;
    memoryAddressAssumptionsMap = pMemoryAddressAssumptionMap;
    mallocZeroMemory = pMallocZeroMemory;
    readBlacklist = pReadBlacklist;
    valueToTypeMap = pValueToTypeMap;
  }

  /**
   * Tries to join this and pOther into a single {@link SymbolicProgramConfiguration}. Returns
   * {@link Optional} empty if this fails.
   *
   * @param pOther Another {@link SymbolicProgramConfiguration}
   * @return Either {@link Optional} empty for failed join, or a new {@link
   *     SymbolicProgramConfiguration} that represents the best merge of the 2 given ones with the
   *     merge status.
   */
  public Optional<MergedSPCAndMergeStatus> merge(
      SymbolicProgramConfiguration pOther, MachineModel pMachineModel) throws SMGException {

    Set<SMGObject> newReadBlackList =
        ImmutableSet.<SMGObject>builder()
            .addAll(readBlacklist)
            .addAll(pOther.readBlacklist)
            .build();

    if (!mallocZeroMemory.equals(pOther.mallocZeroMemory)) {
      return Optional.empty();
    }

    if (!NUMERIC_MEMORY_BUFFER.equals(pOther.NUMERIC_MEMORY_BUFFER)) {
      return Optional.empty();
    }

    if (!currentMemoryAssumptionMax.equals(pOther.currentMemoryAssumptionMax)) {
      return Optional.empty();
    }

    if (!variableToTypeMap.equals(pOther.variableToTypeMap)) {
      return Optional.empty();
    }

    // Rebuild the SMG according to the SMG paper
    Optional<MergedSPCAndMergeStatus> maybeMergedSMGs = mergeSPC(this, pOther, pMachineModel);

    if (maybeMergedSMGs.isEmpty()) {
      return Optional.empty();
    }

    SymbolicProgramConfiguration mergedSMGWithMergedStackAndValues =
        maybeMergedSMGs.orElseThrow().getMergedSPC();
    return Optional.of(
        MergedSPCAndMergeStatus.of(
            new SymbolicProgramConfiguration(
                mergedSMGWithMergedStackAndValues.smg,
                mergedSMGWithMergedStackAndValues.globalVariableMapping,
                mergedSMGWithMergedStackAndValues.atExitStack,
                mergedSMGWithMergedStackAndValues.stackVariableMapping,
                mergedSMGWithMergedStackAndValues.heapObjects,
                mergedSMGWithMergedStackAndValues.externalObjectAllocation,
                mergedSMGWithMergedStackAndValues.valueMapping,
                variableToTypeMap,
                memoryAddressAssumptionsMap,
                mallocZeroMemory,
                newReadBlackList,
                mergedSMGWithMergedStackAndValues.valueToTypeMap),
            maybeMergedSMGs.orElseThrow().getMergeStatus()));
  }

  // We join the SPCs here, (Algorithm 10: joinSPCs)
  private static Optional<MergedSPCAndMergeStatus> mergeSPC(
      SymbolicProgramConfiguration thisSMG,
      SymbolicProgramConfiguration otherSMG,
      MachineModel pMachineModel)
      throws SMGException {
    if (!thisSMG.externalObjectAllocation.isEmpty()
        || !otherSMG.externalObjectAllocation.isEmpty()) {
      throw new SMGException("Error: external allocation not yet implemented for merge.");
    }
    // 1. create fresh, empty SMG
    SymbolicProgramConfiguration mergedSPC = of(thisSMG.getSizeOfPointer());
    ImmutableMap<SMGNode, SMGNode> mapping1 = ImmutableMap.of();
    ImmutableMap<SMGNode, SMGNode> mapping2 = ImmutableMap.of();
    Deque<SMGObjectMergeTriple> objectsToBeMerged = new ArrayDeque<>();

    // 2. For each program variable:
    //      create a fresh region with a matching label/mapping to the 2 SPCs given
    //      (We know that the stack frames are equal as we abort if It's not beforehand)
    for (Entry<String, SMGObject> entry : thisSMG.globalVariableMapping.entrySet()) {
      String thisVarName = entry.getKey();
      SMGObject thisGlobalObj = entry.getValue();
      if (!otherSMG.globalVariableMapping.containsKey(thisVarName)) {
        return Optional.empty();
      }
      SMGObject otherGlobalObj = otherSMG.globalVariableMapping.get(thisVarName);
      if (!thisGlobalObj.isSizeEqual(otherGlobalObj)
          || !thisGlobalObj.getOffset().equals(otherGlobalObj.getOffset())
          || thisSMG.smg.isValid(thisGlobalObj) != otherSMG.smg.isValid(otherGlobalObj)) {
        return Optional.empty();
      }
      if (thisSMG.isObjectExternallyAllocated(thisGlobalObj)
          != otherSMG.isObjectExternallyAllocated(otherGlobalObj)) {
        return Optional.empty();
      }
      CType thisType = thisSMG.variableToTypeMap.get(thisVarName);
      CType otherType = otherSMG.variableToTypeMap.get(thisVarName);
      if (thisType == null ^ otherType == null) {
        return Optional.empty();
      } else if (thisType != null
          && otherType != null
          && !thisType.getCanonicalType().equals(otherType.getCanonicalType())) {
        return Optional.empty();
      }
      SMGObject newObject =
          SMGObject.of(0, thisGlobalObj.getSize(), thisGlobalObj.getOffset(), thisVarName);
      mergedSPC = mergedSPC.copyAndAddGlobalObject(newObject, thisVarName, thisType);
      mapping1 =
          ImmutableMap.<SMGNode, SMGNode>builder()
              .putAll(mapping1)
              .put(thisGlobalObj, newObject)
              .buildOrThrow();
      mapping2 =
          ImmutableMap.<SMGNode, SMGNode>builder()
              .putAll(mapping2)
              .put(otherGlobalObj, newObject)
              .buildOrThrow();
      addObjectsToBeMerged(
          thisSMG,
          thisGlobalObj,
          otherSMG,
          otherGlobalObj,
          newObject,
          thisVarName,
          objectsToBeMerged,
          true,
          thisSMG.isObjectExternallyAllocated(thisGlobalObj));
    }

    Iterator<StackFrame> thisStackFrames = thisSMG.stackVariableMapping.iterator();
    Iterator<StackFrame> otherStackFrames = otherSMG.stackVariableMapping.iterator();
    while (otherStackFrames.hasNext()) {
      if (!thisStackFrames.hasNext()) {
        return Optional.empty();
      }
      StackFrame thisFrame = thisStackFrames.next();
      StackFrame otherFrame = otherStackFrames.next();
      CFunctionDeclaration thisFunDef = thisFrame.getFunctionDefinition();
      CFunctionDeclaration otherFunDef = otherFrame.getFunctionDefinition();
      if (!thisFunDef.equals(otherFunDef)) {
        return Optional.empty();
      }
      ImmutableList<Value> variableArgs = null;
      if (thisFrame.hasVariableArguments() && otherFrame.hasVariableArguments()) {
        if (thisFrame.getVariableArguments().equals(otherFrame.getVariableArguments())) {
          variableArgs = thisFrame.getVariableArguments();
        } else {
          return Optional.empty();
        }
      } else if (thisFrame.hasVariableArguments() || otherFrame.hasVariableArguments()) {
        return Optional.empty();
      }
      mergedSPC = mergedSPC.copyAndAddStackFrame(thisFunDef, pMachineModel, variableArgs);
      Optional<SMGObject> maybeThisReturnObj = thisFrame.getReturnObject();
      Optional<SMGObject> maybeOtherReturnObj = otherFrame.getReturnObject();
      if (maybeOtherReturnObj.isPresent() && maybeThisReturnObj.isPresent()) {
        Preconditions.checkArgument(mergedSPC.hasReturnObjectForCurrentStackFrame());
        addObjectsToBeMerged(
            thisSMG,
            maybeThisReturnObj.orElseThrow(),
            otherSMG,
            maybeOtherReturnObj.orElseThrow(),
            mergedSPC.getReturnObjectForCurrentStackFrame().orElseThrow(),
            thisFunDef.getQualifiedName() + "::__CPAchecker_internal_return_object",
            objectsToBeMerged,
            false,
            false);
      } else if (maybeOtherReturnObj.isPresent() || maybeThisReturnObj.isPresent()) {
        return Optional.empty();
      }

      Map<String, SMGObject> thisVariables = thisFrame.getVariables();
      for (Entry<String, SMGObject> otherEntry : otherFrame.getVariables().entrySet()) {
        String otherVarName = otherEntry.getKey();
        SMGObject otherObj = otherEntry.getValue();
        if (!thisVariables.containsKey(otherVarName)) {
          return Optional.empty();
        }
        SMGObject thisObj = thisVariables.get(otherVarName);
        if (!thisObj.isSizeEqual(otherObj)
            || !thisObj.getOffset().equals(otherObj.getOffset())
            || thisSMG.smg.isValid(thisObj) != otherSMG.smg.isValid(otherObj)) {
          return Optional.empty();
        }
        CType thisType = thisSMG.variableToTypeMap.get(otherVarName);
        CType otherType = otherSMG.variableToTypeMap.get(otherVarName);
        if (thisType == null ^ otherType == null) {
          return Optional.empty();
        } else if (thisType != null
            && otherType != null
            && !thisType.getCanonicalType().equals(otherType.getCanonicalType())) {
          return Optional.empty();
        }
        // Copy
        SMGObject newObject = SMGObject.of(0, otherObj.getSize(), BigInteger.ZERO, otherVarName);
        mergedSPC = mergedSPC.copyAndAddStackObject(newObject, otherVarName, thisType);
        mapping1 =
            ImmutableMap.<SMGNode, SMGNode>builder()
                .putAll(mapping1)
                .put(thisObj, newObject)
                .buildOrThrow();
        mapping2 =
            ImmutableMap.<SMGNode, SMGNode>builder()
                .putAll(mapping2)
                .put(otherObj, newObject)
                .buildOrThrow();
        addObjectsToBeMerged(
            thisSMG,
            thisObj,
            otherSMG,
            otherObj,
            newObject,
            otherVarName,
            objectsToBeMerged,
            false,
            thisSMG.isObjectExternallyAllocated(thisObj));
      }
    }

    SMGMergeStatus mergeStatus = SMGMergeStatus.EQUAL;
    SymbolicProgramConfiguration thisSPC = thisSMG;
    SymbolicProgramConfiguration otherSPC = otherSMG;
    // 3. For each program variable:
    //      Perform joinSubSMG() for the region of the var and all 3 SMGs
    //      Abort for bottom join/merge status
    for (SMGObjectMergeTriple objects : objectsToBeMerged) {
      SMGObject thisGlobalObj = objects.getLeftVariableObject();
      SMGObject otherGlobalObj = objects.getRightVariableObject();
      SMGObject newGlobalObj = objects.getMergeObject();

      Optional<MergedSPCAndMergeStatusWithMergingSPCsAndMapping> maybeMergeResult =
          mergeSubSMGs(
              thisSPC,
              otherSPC,
              thisGlobalObj,
              otherGlobalObj,
              mergedSPC,
              newGlobalObj,
              mergeStatus,
              mapping1,
              mapping2,
              0);
      if (maybeMergeResult.isEmpty() || maybeMergeResult.orElseThrow().isRecoverableFailure()) {
        return Optional.empty();
      }
      MergedSPCAndMergeStatusWithMergingSPCsAndMapping mergeResult = maybeMergeResult.orElseThrow();
      thisSPC = mergeResult.getMergingSPC1();
      otherSPC = mergeResult.getMergingSPC2();
      mergeStatus = mergeResult.getMergeStatus();
      mergedSPC = mergeResult.getMergedSPC();
      mapping1 = mergeResult.getMapping1();
      mapping2 = mergeResult.getMapping2();
    }

    // 4. If there is a cycle consisting of only 0+ in the new SPC/SMG, return bottom
    for (SMGSinglyLinkedListSegment sll : mergedSPC.smg.getAllValidAbstractedObjects()) {
      Set<SMGObject> seen = new HashSet<>();
      BigInteger nextOffset = sll.getNextOffset();
      SMGSinglyLinkedListSegment walkerSLL = sll;
      while (walkerSLL.getMinLength() == 0) {
        if (seen.contains(walkerSLL)) {
          // Loop of 0+ses found
          return Optional.empty();
        }
        SMGAndHasValueEdges readEdges =
            mergedSPC.smg.readValue(walkerSLL, nextOffset, thisSPC.getSizeOfPointer(), false);
        if (readEdges.getHvEdges().size() != 1) {
          break;
        }
        SMGValue nextPtrValue = readEdges.getHvEdges().get(0).hasValue();
        if (nextPtrValue.isZero() || !mergedSPC.smg.isPointer(nextPtrValue)) {
          break;
        }
        SMGObject nextObj = mergedSPC.smg.getPTEdge(nextPtrValue).orElseThrow().pointsTo();
        if (!(nextObj instanceof SMGSinglyLinkedListSegment nextSll)
            || !nextSll.getNextOffset().equals(nextOffset)) {
          break;
        }
        seen.add(walkerSLL);
        walkerSLL = (SMGSinglyLinkedListSegment) nextObj;
      }
    }

    // Those are never supposed to end up in the mappings!
    assert !mapping1.containsKey(SMGValue.zeroValue());
    assert !mapping1.containsKey(SMGObject.nullInstance());
    assert !mapping2.containsKey(SMGValue.zeroValue());
    assert !mapping2.containsKey(SMGObject.nullInstance());
    return Optional.of(MergedSPCAndMergeStatus.of(mergedSPC, mergeStatus));
  }

  private static void addObjectsToBeMerged(
      SymbolicProgramConfiguration thisSPC,
      SMGObject pThisObj,
      SymbolicProgramConfiguration otherSPC,
      SMGObject pOtherObj,
      SMGObject pNewObject,
      String varName,
      Deque<SMGObjectMergeTriple> mergePrioQueue,
      boolean isGlobalVariable,
      boolean isExternallyAllocated) {

    Set<SMGHasValueEdge> hves2 =
        otherSPC
            .smg
            .getSMGObjectsWithSMGHasValueEdges()
            .getOrDefault(pOtherObj, PersistentSet.of());
    Set<SMGHasValueEdge> hves1 =
        thisSPC.smg.getSMGObjectsWithSMGHasValueEdges().getOrDefault(pThisObj, PersistentSet.of());

    // One value 0, the other is not, or non-pointers are priority
    // (the first leads to failure most of the time, the second might, but is always cheap)
    boolean prio =
        hves1.stream()
            .anyMatch(
                hve1 ->
                    hves2.stream()
                        .anyMatch(
                            hve2 ->
                                hve1.getOffset().equals(hve2.getOffset())
                                    && hve1.getSizeInBits().equals(hve2.getSizeInBits())
                                    && ((hve1.hasValue().isZero() ^ hve2.hasValue().isZero())
                                        || (!thisSPC.getSmg().isPointer(hve1.hasValue())
                                            && !otherSPC.getSmg().isPointer(hve2.hasValue())))));
    if (prio) {
      mergePrioQueue.addFirst(
          SMGObjectMergeTriple.of(
              varName, pThisObj, pOtherObj, pNewObject, isGlobalVariable, isExternallyAllocated));
    } else {
      mergePrioQueue.addLast(
          SMGObjectMergeTriple.of(
              varName, pThisObj, pOtherObj, pNewObject, isGlobalVariable, isExternallyAllocated));
    }
  }

  private static Optional<MergedSPCAndMergeStatusWithMergingSPCsAndMapping> mergeSubSMGs(
      SymbolicProgramConfiguration originalSpc1,
      SymbolicProgramConfiguration originalSpc2,
      SMGObject obj1,
      SMGObject obj2,
      SymbolicProgramConfiguration originalNewSPC,
      SMGObject newObj,
      SMGMergeStatus initialStatus,
      ImmutableMap<SMGNode, SMGNode> mapping1,
      ImmutableMap<SMGNode, SMGNode> mapping2,
      int nestingDiff)
      throws SMGException {
    Preconditions.checkNotNull(initialStatus);
    // 1. Let res := joinFields(G1 , G2 , o1 , o2 ). If res = ⊥, return ⊥.
    // Otherwise, let (s0 , G1 , G2 ) := res and s := updateJoinStatus(s, s0).
    Optional<MergingSPCsAndMergeStatus> maybeMergedFields =
        mergeFields(originalSpc1, originalSpc2, obj1, obj2);
    if (maybeMergedFields.isEmpty()) {
      return Optional.empty();
    }
    MergingSPCsAndMergeStatus mergedFields = maybeMergedFields.orElseThrow();
    SMGMergeStatus joinOfFieldsStatus = mergedFields.getMergeStatus();
    SMGMergeStatus status = initialStatus.updateWith(joinOfFieldsStatus);
    SymbolicProgramConfiguration spc1 = mergedFields.getMergingSPC1();
    SymbolicProgramConfiguration spc2 = mergedFields.getMergingSPC2();

    // 2. Collect the set F of all pairs (of, t) occurring in has-value edges leading from o1 or o2
    SortedMap<Integer, Integer> offsetsToSize1 = new TreeMap<>();
    for (SMGHasValueEdge hve1 :
        spc1.smg.getSMGObjectsWithSMGHasValueEdges().getOrDefault(obj1, PersistentSet.of())) {
      offsetsToSize1.put(hve1.getOffset().intValue(), hve1.getSizeInBits().intValue());
    }
    for (SMGHasValueEdge hve2 :
        spc2.smg.getSMGObjectsWithSMGHasValueEdges().getOrDefault(obj2, PersistentSet.of())) {
      boolean offsetsMatch = offsetsToSize1.containsKey(hve2.getOffset().intValue());
      if (offsetsMatch) {
        boolean sizesMatch =
            offsetsToSize1.get(hve2.getOffset().intValue()) == hve2.getSizeInBits().intValue();
        if (!sizesMatch) {
          // Fields not matching. Is this allowed? Would cause problems below. Fix in mergeFields().
          throw new SMGException(
              "Unexpected merge error. Please inform the dev of SMG2 to investigate this.");
        }
      } else {
        // There is an edge in 2 that's not in 1. Is this a problem? Currently i interpret
        // joinFields in a way that this is not supposed to be possible?
        // TODO: check that there is no overlapping field from 1?
        throw new SMGException(
            "Unexpected merge error. Please inform the dev of SMG2 to investigate this.");
      }
    }

    SymbolicProgramConfiguration newSPC = originalNewSPC;
    // 3. For each field (of, t) ∈ F do:
    for (Entry<Integer, Integer> offsetAndSize : offsetsToSize1.entrySet()) {
      int offset = offsetAndSize.getKey();
      int size = offsetAndSize.getValue();
      SMGValue v1 =
          spc1.getSmg()
              .readValue(obj1, BigInteger.valueOf(offset), BigInteger.valueOf(size), false)
              .getHvEdges()
              .get(0)
              .hasValue();
      assert spc1.getValueFromSMGValue(v1).isPresent();
      SMGValue v2 =
          spc2.getSmg()
              .readValue(obj2, BigInteger.valueOf(offset), BigInteger.valueOf(size), false)
              .getHvEdges()
              .get(0)
              .hasValue();
      assert spc2.getValueFromSMGValue(v2).isPresent();
      int currentNestingDiff = nestingDiff;
      // Calculate diff of nesting levels for linked lists/regions
      if (obj1 instanceof SMGSinglyLinkedListSegment sll1) {
        boolean isNextOrPrev = sll1.getNextOffset().intValue() == offset;
        if (!isNextOrPrev && obj1 instanceof SMGDoublyLinkedListSegment dll1) {
          isNextOrPrev = dll1.getPrevOffset().intValue() == offset;
        }
        currentNestingDiff = isNextOrPrev ? currentNestingDiff : currentNestingDiff + 1;
      }
      if (obj2 instanceof SMGSinglyLinkedListSegment sll2) {
        boolean isNextOrPrev = sll2.getNextOffset().intValue() == offset;
        if (!isNextOrPrev && obj2 instanceof SMGDoublyLinkedListSegment dll2) {
          isNextOrPrev = dll2.getPrevOffset().intValue() == offset;
        }
        currentNestingDiff = isNextOrPrev ? currentNestingDiff : currentNestingDiff - 1;
      }
      // joinValues()
      // Returns a value that represents both inputs, inclusive their memory, if it succeeds.
      Optional<MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue> maybeJoinedValuesResult =
          mergeValues(spc1, spc2, v1, v2, newSPC, mapping1, mapping2, status, currentNestingDiff);
      if (maybeJoinedValuesResult.isEmpty()
          || maybeJoinedValuesResult.orElseThrow().getRecoverableFailure() == DELAYED_MERGE) {
        return Optional.empty();
      } else if (maybeJoinedValuesResult.orElseThrow().isRecoverableFailure()) {
        // Extension over the paper, allows list length mismatch correction
        return Optional.of(
            MergedSPCAndMergeStatusWithMergingSPCsAndMapping.recoverableFailure(
                maybeJoinedValuesResult.orElseThrow().getRecoverableFailure()));
      }
      MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue joinedValuesResult =
          maybeJoinedValuesResult.orElseThrow();
      MergedSPCAndMergeStatusWithMergingSPCsAndMapping joinedSPCsAndMappingsAndStatus =
          joinedValuesResult.getJoinSPCAndJoinStatusWithJoinedSPCsAndMapping();
      status = joinedSPCsAndMappingsAndStatus.getMergeStatus();
      spc1 = joinedSPCsAndMappingsAndStatus.getMergingSPC1();
      spc2 = joinedSPCsAndMappingsAndStatus.getMergingSPC2();
      mapping1 = joinedSPCsAndMappingsAndStatus.getMapping1();
      mapping2 = joinedSPCsAndMappingsAndStatus.getMapping2();
      newSPC = joinedSPCsAndMappingsAndStatus.getMergedSPC();
      SMGValue newValue = joinedValuesResult.getSMGValue();

      // Add new HVE for joined value.
      newSPC =
          newSPC.writeValue(newObj, BigInteger.valueOf(offset), BigInteger.valueOf(size), newValue);
      // Check that the new pointer also has a PTE
      Preconditions.checkArgument(
          !(spc1.smg.isPointer(v1) || spc2.smg.isPointer(v2)) || newSPC.smg.isPointer(newValue));
    }

    // 4. Return (s, G1 , G2 , G, m1 , m2 ).
    return Optional.of(
        MergedSPCAndMergeStatusWithMergingSPCsAndMapping.of(
            newSPC, status, spc1, spc2, mapping1, mapping2));
  }

  private static Optional<MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue> mergeValues(
      SymbolicProgramConfiguration pSpc1,
      SymbolicProgramConfiguration pSpc2,
      SMGValue v1,
      SMGValue v2,
      SymbolicProgramConfiguration pNewSpc,
      ImmutableMap<SMGNode, SMGNode> mapping1,
      ImmutableMap<SMGNode, SMGNode> mapping2,
      SMGMergeStatus initialJoinStatus,
      int nestingDiff)
      throws SMGException {
    Preconditions.checkNotNull(initialJoinStatus);

    // 1. if v1 == v2, return
    Value v1v = pSpc1.getValueFromSMGValue(v1).orElseThrow();
    Value v2v = pSpc2.getValueFromSMGValue(v2).orElseThrow();
    CType maybeV1Type = pSpc1.valueToTypeMap.get(v1);
    CType maybeV2Type = pSpc2.valueToTypeMap.get(v2);
    int v1NestingLvl = pSpc1.getNestingLevel(v1v);
    int v2NestingLvl = pSpc2.getNestingLevel(v2v);
    if (maybeV1Type == null || maybeV2Type == null) {
      throw new SMGException(
          "Error when merging. A symbolic value could not be compared or created due to a missing"
              + " type.");
    }
    CType sharedType = maybeV1Type.getCanonicalType();

    // Return for equal values (e.g. numeric values) but not pointers
    // TODO: this only works if the memory is also eq, so pull areValuesEqual() down to SPC level
    if (v1NestingLvl == v2NestingLvl
        && ((v1v.isNumericValue() && v2v.isNumericValue() && v1v.equals(v2v))
            || (v1.equals(v2) && !pSpc1.getSmg().isPointer(v1) && !pSpc2.getSmg().isPointer(v2)))) {
      if (!maybeV1Type.getCanonicalType().equals(maybeV2Type.getCanonicalType())) {
        // Not really an error, look into this
        throw new SMGException(
            "Error when merging. A symbolic value could not be compared or created due to a missing"
                + " type.");
      }
      SymbolicProgramConfiguration newSPC =
          pNewSpc.copyAndPutValue(
              pSpc1.getValueFromSMGValue(v1).orElseThrow(), v1, v1NestingLvl, sharedType);
      return Optional.of(
          MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue.of(
              v1,
              MergedSPCAndMergeStatusWithMergingSPCsAndMapping.of(
                  newSPC, initialJoinStatus, pSpc1, pSpc2, mapping1, mapping2)));
    }

    // 2. if m1(v1) == m2(v2) = v != 0, return (already joined). (m1 == mapping1 etc.)
    if (mapping1.containsKey(v1) && mapping2.containsKey(v2)) {
      SMGValue mv1 = (SMGValue) mapping1.get(v1);
      SMGValue mv2 = (SMGValue) mapping2.get(v2);
      if (mv1.equals(mv2) && !mv1.isZero()) {
        return Optional.of(
            MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue.of(
                mv1,
                MergedSPCAndMergeStatusWithMergingSPCsAndMapping.of(
                    pNewSpc, initialJoinStatus, pSpc1, pSpc2, mapping1, mapping2)));
      }
    }

    SMGMergeStatus status = initialJoinStatus;
    // 3. If both v1 and v2 are non-address values, then:
    if (!pSpc1.smg.isPointer(v1) && !pSpc2.smg.isPointer(v2)) {
      if (mapping1.containsKey(v1) || mapping2.containsKey(v2)) {
        // If there is a mapping for v1 or v2 already present, return bottom.
        return Optional.empty();
      }

      if (v1v.isNumericValue() || v2v.isNumericValue()) {
        if (v1v.isNumericValue()
            && v2v.isNumericValue()
            && v1v.asNumericValue()
                    .bigIntegerValue()
                    .compareTo(v2v.asNumericValue().bigIntegerValue())
                != 0) {
          // TODO: use a symbolic value with a constraint?
          return Optional.empty();
        } else {
          // Symbolic and concrete
          // TODO: try to include the concrete in the symbolic?
          return Optional.empty();
        }
      } else {
        // 2 symbolic values, check that constraints match
        if (!maybeV1Type.getCanonicalType().equals(maybeV2Type.getCanonicalType())) {
          // Types not matching.
          // TODO: This is not necessarily a bad thing!
          return Optional.empty();
        }
        // TODO: constraints equality is currently handled in SMGState merge() (we reject all
        // possible states with non-equal constraints)

      }

      // Create a new value v such that level(v) = max(level(v1), level(v2))
      Value valueToWrite = pNewSpc.getNewSymbolicValueForType(sharedType);
      SMGValue v = SMGValue.of();
      int level = Integer.max(v1NestingLvl, v2NestingLvl);
      SymbolicProgramConfiguration newSPC =
          pNewSpc.copyAndPutValue(valueToWrite, v, level, sharedType);
      // Extend mapping such that m1(v1) == m2(v2) == v
      mapping1 =
          ImmutableMap.<SMGNode, SMGNode>builder().putAll(mapping1).put(v1, v).buildOrThrow();
      mapping2 =
          ImmutableMap.<SMGNode, SMGNode>builder().putAll(mapping2).put(v2, v).buildOrThrow();
      assert newSPC.smg.hasValue(v);
      assert newSPC.getValueFromSMGValue(v).isPresent();

      // Note on the nesting level here: we use 0 for all non-ptr values, so this will never fail.
      // Reason being that we don't want to have copies of concrete values that are irrelevant in
      // the nesting, and we don't want to add additional complexity to the SMT solving
      // (constraints of values with copies based on nesting level etc.)
      // Since values can only be changed when merging or on concrete elements, this is no issue.

      // If level(v1) - level(v2) < nestingLevel, update join status with ⊏
      if (v1NestingLvl - v2NestingLvl < nestingDiff) {
        status = status.updateWith(SMGMergeStatus.LEFT_ENTAIL);
      }
      // If level(v1) - level(v2) > nestingLevel, update join status with ⊐
      if (v1NestingLvl - v2NestingLvl > nestingDiff) {
        status = status.updateWith(SMGMergeStatus.RIGHT_ENTAIL);
      }
      // Return with new value equal v (the paper says v1, but that's clearly wrong!)
      return Optional.of(
          MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue.of(
              v,
              MergedSPCAndMergeStatusWithMergingSPCsAndMapping.of(
                  newSPC, status, pSpc1, pSpc2, mapping1, mapping2)));
    }

    // 4. If one of v1 or v2 are non-address values, return bottom.
    if (!pSpc1.smg.isPointer(v1) || !pSpc2.smg.isPointer(v2)) {
      return Optional.empty();
    }

    // 5. joinTargetObject(), if it returns bottom, return bottom.
    Optional<MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue> res =
        mergeTargetObjects(pNewSpc, pSpc1, pSpc2, v1, v2, mapping1, mapping2, status, nestingDiff);
    // If it does not return recoverable failure, return result of joinTargetObject().
    if (res.isEmpty() || !res.orElseThrow().isRecoverableFailure()) {
      return res;
    }

    // Subsequent parts of the paper are handled in the following method
    return handleRecoverableFailure(
        pSpc1, pSpc2, v1, v2, pNewSpc, mapping1, mapping2, initialJoinStatus, nestingDiff, res);
  }

  /**
   * Handles recoverable failures. Will try to insert a 0+ object for recoverable failures and will
   * try to extend the list by materializing for other types of failure.
   */
  @Nonnull
  private static Optional<MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue>
      handleRecoverableFailure(
          SymbolicProgramConfiguration pSpc1,
          SymbolicProgramConfiguration pSpc2,
          SMGValue v1,
          SMGValue v2,
          SymbolicProgramConfiguration pNewSpc,
          ImmutableMap<SMGNode, SMGNode> mapping1,
          ImmutableMap<SMGNode, SMGNode> mapping2,
          SMGMergeStatus initialJoinStatus,
          int nestingDiff,
          Optional<MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue> res)
          throws SMGException {
    SMGRecoverableFailure failureType = res.orElseThrow().getRecoverableFailure();
    // 6. Let targetObj1/2 be the target of the PTEs of v1 and v2
    SMGPointsToEdge pte1 = pSpc1.smg.getPTEdge(v1).orElseThrow();
    SMGPointsToEdge pte2 = pSpc2.smg.getPTEdge(v2).orElseThrow();
    SMGObject t1 = pte1.pointsTo();
    SMGObject t2 = pte2.pointsTo();

    // 7. If targetObj1 is an abstracted obj, insertLeftLLsAndJoin().
    //      If bottom, return bottom, if not recoverable failure, return result.
    if (t1 instanceof SMGSinglyLinkedListSegment && (failureType == DELAYED_MERGE)) {
      res =
          insertLeftLLAndJoin(
              pSpc1, pSpc2, v1, v2, pNewSpc, mapping1, mapping2, initialJoinStatus, nestingDiff);
      if (res.isEmpty() || !res.orElseThrow().isRecoverableFailure()) {
        return res;
      }
      failureType = res.orElseThrow().getRecoverableFailure();
    }

    // 8. If targetObj2 is an abstracted obj, insertRightLLsAndJoin().
    //      If bottom, return bottom, else return result.
    if (t2 instanceof SMGSinglyLinkedListSegment && (failureType == DELAYED_MERGE)) {
      res =
          insertRightLLAndJoin(
              pSpc1, pSpc2, v1, v2, pNewSpc, mapping1, mapping2, initialJoinStatus, nestingDiff);
      if (res.isEmpty() || res.orElseThrow().getRecoverableFailure() == DELAYED_MERGE) {
        return Optional.empty();
      }
      failureType = res.orElseThrow().getRecoverableFailure();
    }

    if (failureType == DELAYED_MERGE) {
      return Optional.empty();
    }

    // Against the papers algorithm we also return recoverable failure below here. This allows us to
    // join lists with differing length here or in previously merged abstract list segments.
    if (t2 instanceof SMGSinglyLinkedListSegment sll2 && failureType == LEFT_LIST_LONGER) {
      // Extend the right list with a 0+ from another abstracted element right
      // res = insertRightLLIntoRightAndJoin();

      // TODO: if this works, make materialization work on SPC level
      Optional<SymbolicProgramConfiguration> maybeNewSPC2 =
          getMaterializedListAndSPC(pSpc2, v2, sll2);

      if (maybeNewSPC2.isEmpty()) {
        return Optional.empty();
      }

      // Multiple extensions are possible, but happen 1 element further now.
      res =
          mergeValues(
              pSpc1,
              maybeNewSPC2.orElseThrow(),
              v1,
              v2,
              pNewSpc,
              mapping1,
              mapping2,
              initialJoinStatus,
              nestingDiff);

      if (res.isEmpty()) {
        return Optional.empty();
      } else if (res.orElseThrow().isRecoverableFailure()) {
        // Either delayed merge or suddenly we want to extend the other side?
        // Or the extension failed here, and has to be done earlier.
        throw new RuntimeException("investigate merge error for list extensions");
      }
    }

    if (t1 instanceof SMGSinglyLinkedListSegment sll1 && failureType == RIGHT_LIST_LONGER) {
      // Extend the left list with a 0+ from another abstracted element also left
      // res = insertLeftLLIntoLeftAndJoin(pSpc1, pSpc2, v1, t1, v2, t2, pNewSpc, mapping1,
      // mapping2, initialJoinStatus, nestingDiff);

      // TODO: if this works, make materialization work on SPC level
      Optional<SymbolicProgramConfiguration> maybeNewSPC1 =
          getMaterializedListAndSPC(pSpc1, v1, sll1);

      if (maybeNewSPC1.isEmpty()) {
        return Optional.empty();
      }

      // Multiple extensions are possible, but happen 1 element further now.
      res =
          mergeValues(
              maybeNewSPC1.orElseThrow(),
              pSpc2,
              v1,
              v2,
              pNewSpc,
              mapping1,
              mapping2,
              initialJoinStatus,
              nestingDiff);

      if (res.isEmpty()) {
        return Optional.empty();
      } else if (res.orElseThrow().isRecoverableFailure()) {
        // Either delayed merge or suddenly we want to extend the other side?
        // Or the extension failed here, and has to be done earlier.
        throw new RuntimeException("investigate merge error for list extensions");
      }
    }
    return res;
  }

  // TODO: this is a dummy, lower mat to SPC level.
  @Nonnull
  private static Optional<SymbolicProgramConfiguration> getMaterializedListAndSPC(
      SymbolicProgramConfiguration pSpc, SMGValue v, SMGSinglyLinkedListSegment sll)
      throws SMGException {
    try {
      List<SMGValueAndSMGState> matList =
          new SMGCPAMaterializer(LogManager.createTestLogManager(), new SMGCPAStatistics())
              .handleMaterialisation(
                  v,
                  sll,
                  SMGState.of(
                          MachineModel.LINUX32,
                          new LogManagerWithoutDuplicates(LogManager.createTestLogManager()),
                          new SMGOptions(Configuration.defaultConfiguration()),
                          new SMGCPAExpressionEvaluator(
                              MachineModel.LINUX32,
                              new LogManagerWithoutDuplicates(LogManager.createTestLogManager()),
                              new SMGCPAExportOptions(null, SMGExportLevel.NEVER),
                              new SMGOptions(Configuration.defaultConfiguration()),
                              null),
                          new SMGCPAStatistics())
                      .copyAndReplaceMemoryModel(pSpc));
      if (matList.size() != 1) {
        return Optional.empty();
      }
      return Optional.of(matList.get(0).getSMGState().getMemoryModel());
    } catch (InvalidConfigurationException pE) {
      throw new RuntimeException(pE);
    }
  }

  private static boolean areMappedValuesInSPC(
      SymbolicProgramConfiguration pNewSpc, ImmutableMap<SMGNode, SMGNode> mapping) {
    for (SMGNode targetMappings : mapping.values()) {
      if (targetMappings instanceof SMGValue v1Target) {
        if (!pNewSpc.smg.hasValue(v1Target)) {
          return false;
        }
      }
    }
    return true;
  }

  private static Optional<SMGMergeStatusOrRecoverableFailure> matchObjects(
      SMGMergeStatus initialJoinStatus,
      SymbolicProgramConfiguration spc1,
      SymbolicProgramConfiguration spc2,
      ImmutableMap<SMGNode, SMGNode> mapping1,
      ImmutableMap<SMGNode, SMGNode> mapping2,
      SMGObject obj1,
      SMGObject obj2) {
    // 1. If o1 = # or o2 = #, return ⊥.
    if (obj1.isZero() && obj2.isZero()) {
      // Should not be possible, but better safe than sorry
      return Optional.of(SMGMergeStatusOrRecoverableFailure.of(initialJoinStatus));
    } else if (obj1.isZero()) {
      return Optional.of(SMGMergeStatusOrRecoverableFailure.of(RIGHT_LIST_LONGER));
    } else if (obj2.isZero()) {
      return Optional.of(SMGMergeStatusOrRecoverableFailure.of(LEFT_LIST_LONGER));
    }
    SMGNode m1o1 = mapping1.get(obj1);
    SMGNode m2o2 = mapping2.get(obj2);
    boolean m1Exists = mapping1.containsKey(obj1);
    boolean m2Exists = mapping2.containsKey(obj2);
    // 2. If m1 (o1) != ⊥ != m2 (o2) and m1 (o1) != m2 (o2), return ⊥.
    if (m1Exists && m2Exists && !m1o1.equals(m2o2)) {
      return Optional.empty();
    }
    // 3. If m1 (o1) != ⊥ and ∃o'2 ∈ O2 : m1 (o1) = m2 (o'2), return ⊥.
    if (m1Exists) {
      for (Entry<SMGNode, SMGNode> nodes : mapping2.entrySet()) {
        if (nodes.getValue() instanceof SMGObject m2o2Iter && m1o1.equals(m2o2Iter)) {
          assert spc2.isHeapObject((SMGObject) nodes.getKey());
          // Suspect that the right list is longer and might find the correct mapping later
          return Optional.of(SMGMergeStatusOrRecoverableFailure.of(RIGHT_LIST_LONGER));
        }
      }
    }
    // 4. If m2 (o2) != ⊥ and ∃o'1 ∈ O1 : m1 (o'1) = m2 (o2), return ⊥.
    if (m2Exists) {
      for (Entry<SMGNode, SMGNode> nodes : mapping1.entrySet()) {
        if (nodes.getValue() instanceof SMGObject m1o1Iter && m2o2.equals(m1o1Iter)) {
          assert spc1.isHeapObject((SMGObject) nodes.getKey());
          return Optional.empty();
        }
      }
    }
    // 5. If size1(o1) != size2(o2) or valid1(o1) != valid2(o2), return ⊥.
    if (!obj1.isSizeEqual(obj2) || spc1.isObjectValid(obj1) != spc2.isObjectValid(obj2)) {
      return Optional.empty();
    }
    SMGMergeStatus status = initialJoinStatus;
    // 6. If kind1(o1) = kind2(o2) = dls, then:
    if (obj1 instanceof SMGSinglyLinkedListSegment sll1
        && obj2 instanceof SMGSinglyLinkedListSegment sll2) {
      // – If nfo1(o1) != nfo2(o2), pfo1(o1) != pfo2(o2) or hfo1(o1) != hfo2(o2), return ⊥.
      // TODO: also check for target offset and equality cache?
      if (!sll1.getNextOffset().equals(sll2.getNextOffset())
          || !sll1.getHeadOffset().equals(sll2.getHeadOffset())) {
        return Optional.empty();
      }
      if (obj1 instanceof SMGDoublyLinkedListSegment dll1
          && obj2 instanceof SMGDoublyLinkedListSegment dll2) {
        if (!dll1.getPrevOffset().equals(dll2.getPrevOffset())) {
          return Optional.empty();
        }
      }
    }
    // 7. Collect the set F of all pairs (of, t) occurring in has-value edges leading from o1 or o2.
    Set<SMGHasValueEdge> hves1 =
        new HashSet<>(
            spc1.smg.getSMGObjectsWithSMGHasValueEdges().getOrDefault(obj1, PersistentSet.of()));
    Set<SMGHasValueEdge> hves2 =
        new HashSet<>(
            spc2.smg.getSMGObjectsWithSMGHasValueEdges().getOrDefault(obj2, PersistentSet.of()));
    // 8. For each field (of, t) ∈ F do:
    Iterator<SMGHasValueEdge> iter1 = hves1.iterator();
    while (iter1.hasNext()) {
      SMGHasValueEdge hve1 = iter1.next();
      int offset1 = hve1.getOffset().intValueExact();
      int size1 = hve1.getSizeInBits().intValueExact();
      SMGValue v1 = hve1.hasValue();
      // – Let v1 = H1 (o1, of, t) and v2 = H2 (o2, of, t).
      Iterator<SMGHasValueEdge> iter2 = hves2.iterator();
      while (iter2.hasNext()) {
        SMGHasValueEdge hve2 = iter2.next();
        int offset2 = hve2.getOffset().intValueExact();
        int size2 = hve2.getSizeInBits().intValueExact();
        SMGValue v2 = hve2.hasValue();
        if (offset1 == offset2 && size1 == size2) {
          hves2.remove(hve2);
          // – If v1 != ⊥ != v2 and m1 (v1) != ⊥ != m2 (v2) and m1 (v1) != m2(v2), return ⊥.
          if (mapping1.containsKey(v1)
              && mapping2.containsKey(v2)
              && !mapping1.get(v1).equals(mapping2.get(v2))) {
            return Optional.empty();
          }
          iter2 = hves2.iterator();
        }
      }
    }
    // 9. If len'1 (o1) < len'2 (o2) or kind1(o1) = dls ∧ kind2(o2) = reg,
    if ((obj1 instanceof SMGSinglyLinkedListSegment sll1
        && obj2 instanceof SMGSinglyLinkedListSegment sll2
        && sll1.getMinLength() < sll2.getMinLength())) {
      //    let s := updateJoinStatus(s, ⊐).
      status = status.updateWith(SMGMergeStatus.RIGHT_ENTAIL);
    }
    if ((obj1 instanceof SMGSinglyLinkedListSegment
            && !(obj2 instanceof SMGSinglyLinkedListSegment))
        || (obj1 instanceof SMGDoublyLinkedListSegment
            && !(obj2 instanceof SMGDoublyLinkedListSegment))) {
      //    let s := updateJoinStatus(s, ⊐).
      status = status.updateWith(SMGMergeStatus.RIGHT_ENTAIL);
    }
    // 10. If len'1(o1) > len'2(o2) or kind1(o1) = reg ∧ kind2 (o2) = dls,
    if ((obj1 instanceof SMGSinglyLinkedListSegment sll1
        && obj2 instanceof SMGSinglyLinkedListSegment sll2
        && sll1.getMinLength() > sll2.getMinLength())) {
      //      let s := updateJoinStatus(s, ⊏).
      status = status.updateWith(SMGMergeStatus.LEFT_ENTAIL);
    }
    if ((obj2 instanceof SMGSinglyLinkedListSegment
            && !(obj1 instanceof SMGSinglyLinkedListSegment))
        || (obj2 instanceof SMGDoublyLinkedListSegment
            && !(obj1 instanceof SMGDoublyLinkedListSegment))) {
      //      let s := updateJoinStatus(s, ⊏).
      status = status.updateWith(SMGMergeStatus.LEFT_ENTAIL);
    }
    // 11. Return s.
    return Optional.of(SMGMergeStatusOrRecoverableFailure.of(status));
  }

  /**
   * "Skips" the left (v1) target by inserting a fictitious 0+ element on the right hand side SMG
   * (between v2 and its target). You could also say we extend the right list. Tries to save a
   * failing merge by inserting a 0+ in between list segments. The left (v1) value points towards an
   * abstracted objects. We insert a 0+ element before the objects pointed to by right (v2) and
   * merge based on the 2 abstracted objects, "skipping" the target of v1. Traversal direction and
   * aNext is based on the specifier of v1, using the next or last offset of the linked-list left to
   * find the next pointer.
   *
   * <p>The value (pointer) a points to the new copy of the linked-list d1 called d. JoinValues() in
   * 10. merges the sub-SMG from the next or prev pointer of the linked-list to the value a2. This
   * ensures that the new linked-list is inserted, but the rest is merged normally, skipping the
   * linked-list in a sense.
   */
  private static Optional<MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue>
      insertLeftLLAndJoin(
          SymbolicProgramConfiguration pSpc1,
          SymbolicProgramConfiguration pSpc2,
          SMGValue v1,
          SMGValue v2,
          SymbolicProgramConfiguration pNewSpc,
          ImmutableMap<SMGNode, SMGNode> mapping1,
          ImmutableMap<SMGNode, SMGNode> mapping2,
          SMGMergeStatus initialJoinStatus,
          int nestingDiff)
          throws SMGException {
    assert areMappedValuesInSPC(pNewSpc, mapping1);
    assert areMappedValuesInSPC(pNewSpc, mapping2);
    Preconditions.checkArgument(
        pSpc1.smg.isPointer(v1)
            && pSpc1.smg.getPTEdge(v1).orElseThrow().pointsTo()
                instanceof SMGSinglyLinkedListSegment);
    SMGPointsToEdge pte1 = pSpc1.smg.getPTEdge(v1).orElseThrow();
    SMGTargetSpecifier targetSpec1 = pte1.targetSpecifier();
    SMGSinglyLinkedListSegment sll1 = (SMGSinglyLinkedListSegment) pte1.pointsTo();
    SMGDoublyLinkedListSegment dll1 = null;
    if (sll1 instanceof SMGDoublyLinkedListSegment) {
      dll1 = (SMGDoublyLinkedListSegment) sll1;
    }
    // 2. get working offset (nf) based on type of list
    int workingOffset; // nf
    if (targetSpec1.equals(SMGTargetSpecifier.IS_FIRST_POINTER)) {
      workingOffset = sll1.getNextOffset().intValue(); // nf
    } else if (targetSpec1.equals(SMGTargetSpecifier.IS_LAST_POINTER)) {
      Preconditions.checkNotNull(dll1);
      workingOffset = dll1.getPrevOffset().intValue();
    } else {
      Preconditions.checkArgument(targetSpec1.equals(SMGTargetSpecifier.IS_ALL_POINTER));
      // return recoverable failure
      return Optional.of(
          MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue.recoverableFailure(
              DELAYED_MERGE));
    }

    // 3. Get existing HVE in the target of v1 with the working offset found
    Preconditions.checkState(
        sll1.getSize().isNumericValue()
            && sll1.getSize()
                    .asNumericValue()
                    .bigIntegerValue()
                    .subtract(BigInteger.valueOf(workingOffset))
                    .compareTo(BigInteger.valueOf(workingOffset))
                >= 0);
    Optional<SMGHasValueEdge> maybeANext =
        pSpc1.smg.getHasValueEdgeByPredicate(
            sll1,
            h ->
                h.getOffset().intValue() == workingOffset
                    && h.getSizeInBits().equals(pSpc1.getSizeOfPointer()));
    SMGValue aNext = SMGValue.zeroValue();
    // joinFields might have zeroed the field (if there was no ptr/value before)
    if (maybeANext.isPresent()) {
      aNext = maybeANext.orElseThrow().hasValue();
    }

    SymbolicProgramConfiguration newSPC = pNewSpc;
    SMGMergeStatus status = initialJoinStatus;
    SymbolicProgramConfiguration spc1 = pSpc1;
    SymbolicProgramConfiguration spc2 = pSpc2;

    // 4. If m1(d1) exists:
    SMGObject d;
    if (mapping1.containsKey(sll1)) {
      // Let d = m1(d1)
      d = (SMGObject) mapping1.get(sll1);
      // If exists object: m2(o) == d, return recoverable failure
      for (SMGNode m2Values : mapping2.values()) {
        if (m2Values.equals(d)) {
          return Optional.of(
              MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue.recoverableFailure(
                  DELAYED_MERGE));
        }
      }
      // If m1(v1) does not exist, create a new value node a,
      //   and a new PTE a -> d (with offset/size of v1) for the new SMG,
      //   and extend the mapping of nodes such that m1(v1) = a.
      if (!mapping1.containsKey(v1)) {
        Value addressValue = SymbolicValueFactory.getInstance().newIdentifier(null);
        CType type1 = spc1.valueToTypeMap.get(v1);
        Preconditions.checkNotNull(type1);
        newSPC =
            newSPC.copyAndAddPointerFromAddressToMemory(
                addressValue, d, type1, pte1.getOffset(), 0, pte1.targetSpecifier());
        SMGValue newSMGValue = newSPC.getSMGValueFromValue(addressValue).orElseThrow();
        mapping1 =
            ImmutableMap.<SMGNode, SMGNode>builder()
                .putAll(mapping1)
                .put(v1, newSMGValue)
                .buildOrThrow();
        assert newSPC.smg.hasValue(newSMGValue);
        assert newSPC.getValueFromSMGValue(newSMGValue).isPresent();
      } else {
        //   Otherwise let a = m1(v1) and return with a.
        SMGValue a = (SMGValue) mapping1.get(v1);
        return Optional.of(
            MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue.of(
                a,
                MergedSPCAndMergeStatusWithMergingSPCsAndMapping.of(
                    newSPC, initialJoinStatus, pSpc1, pSpc2, mapping1, mapping2)));
      }
      // Let res = joinValues(status, SMG1, SMG2, new SMG, m1, m2, aNext, v2, ldiff).
      Optional<MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue> maybeRes =
          mergeValues(spc1, spc2, aNext, v2, newSPC, mapping1, mapping2, status, nestingDiff);
      if (maybeRes.isEmpty() || maybeRes.orElseThrow().getRecoverableFailure() == DELAYED_MERGE) {
        return Optional.empty();
      } else if (maybeRes.orElseThrow().isRecoverableFailure()) {
        // Hand the extension request back, extend and then possibly restart this method
        return maybeRes;
      }
      MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue res = maybeRes.orElseThrow();
      // If res = bottom, return bottom.
      // TODO: this a is wierd. It is never actually used in the algorithm. SMG1 also never used it.
      // SMGValue a = res.getSMGValue();
      MergedSPCAndMergeStatusWithMergingSPCsAndMapping resRest =
          res.getJoinSPCAndJoinStatusWithJoinedSPCsAndMapping();
      status = resRest.getMergeStatus();
      // Else, update parameters with result and let a be the returned value.
      spc1 = resRest.getMergingSPC1();
      spc2 = resRest.getMergingSPC2();
      mapping1 = resRest.getMapping1();
      mapping2 = resRest.getMapping2();
    }
    // 5. If m1(aNext) not existing and m2(v2) not existing and m1(aNext) != m2(v2), return
    // recoverable fail.
    if (mapping1.containsKey(aNext)
        && mapping2.containsKey(v2)
        && !mapping1.get(aNext).equals(mapping2.get(v2))) {
      return Optional.of(
          MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue.recoverableFailure(
              DELAYED_MERGE));
    }
    // 6. Update status with: If length of d1 == 0 -> ⊐, else incomparable
    if (sll1.getMinLength() == 0) {
      status = status.updateWith(SMGMergeStatus.RIGHT_ENTAIL);
    } else {
      status = status.updateWith(SMGMergeStatus.INCOMPARABLE);
    }

    // 7. Extend new SMG with a fresh copy of the nf restricted sub-SMG of SMG1 rooted at d1 (sll1),
    // but excluding the nodes that are already mapped in m1, such that the copy of d1 is a linked
    // list d. (nf either next or prev, depending on specifier)
    // Then, extend the mapping m1 such that the newly created nodes in O u V are mapped from
    // the corresponding nodes of O1 u V1.
    // 8. Initialize the labeling of d to match the labeling of d1 up to minimum length,
    //      which is fixed to 0
    d = sll1.copyWithNewMinimumLength(0);
    // For SLLs we have the next pointer leading into this that's created below and aNext is the
    // next pointer of d which is mapped below as well.
    // For DLLs we have 2 cases, next pointer case: same as above + back of d is already known
    // (either the ptr or the obj or 0) while the back ptr of the "next" obj (behind aNext) is
    // mapped due to the mapping of m1(d1) = d
    //   For the prev ptr the same in reverse.
    MergedSPCAndMapping newSPCAndMapping =
        copySubSMGRootedAt(
            sll1,
            (SMGSinglyLinkedListSegment) d,
            spc1,
            newSPC,
            BigInteger.valueOf(workingOffset),
            mapping1);
    newSPC = newSPCAndMapping.getMergedSPC();
    mapping1 = newSPCAndMapping.getMapping();

    Preconditions.checkArgument(newSPC.heapObjects.contains(d));
    Preconditions.checkArgument(newSPC.smg.getObjects().contains(d));
    Preconditions.checkState(mapping1.containsKey(sll1) && mapping1.get(sll1).equals(d));
    Preconditions.checkArgument(((SMGSinglyLinkedListSegment) d).getMinLength() == 0);
    // 9. Let value a be the address such that the PTE of a equals the offset,
    //      size and target d if such an address already exists in new SMG.
    //    Otherwise, create a new value a with those characteristics and
    //      extend the mapping of nodes such that m1(v1) = a
    Optional<SMGValue> maybeAddressValue =
        newSPC.getAddressValueForPointsToTarget(d, pte1.getOffset(), targetSpec1);
    SMGValue a;
    if (maybeAddressValue.isEmpty()) {
      Value addressValue = SymbolicValueFactory.getInstance().newIdentifier(null);
      CType type1 = spc1.valueToTypeMap.get(v1);
      Preconditions.checkNotNull(type1);
      int nestingLevel = spc1.getNestingLevel(v1);
      newSPC =
          newSPC.copyAndAddPointerFromAddressToMemory(
              addressValue, d, type1, pte1.getOffset(), nestingLevel, pte1.targetSpecifier());
      a = newSPC.getSMGValueFromValue(addressValue).orElseThrow();
    } else {
      a = maybeAddressValue.orElseThrow();
    }

    // Remember the mapping of v1 to a
    mapping1 = ImmutableMap.<SMGNode, SMGNode>builder().putAll(mapping1).put(v1, a).buildOrThrow();
    assert newSPC.smg.hasValue(a);
    assert newSPC.getValueFromSMGValue(a).isPresent();

    // 10. Let res = joinValues(..., aNext, v2, ...); if bot, return bot.
    // Note: we merge the next/prev pointer of the linked-list a1 points to and v2,
    //   skipping the inserted list element in a sense.
    Optional<MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue> maybeRes =
        mergeValues(spc1, spc2, aNext, v2, newSPC, mapping1, mapping2, status, nestingDiff);
    if (maybeRes.isEmpty() || maybeRes.orElseThrow().getRecoverableFailure() == DELAYED_MERGE) {
      return maybeRes;
    } else if (maybeRes.orElseThrow().isRecoverableFailure()) {
      // Investigate this, most likely the list of the right hand side is longer than the one on the
      // left
      throw new RuntimeException("Recoverable failure exception when merging");
    }
    // Else new value a' for the value returned.
    MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue res = maybeRes.orElseThrow();
    // If res = bottom, return bottom.
    SMGValue aStar = res.getSMGValue();
    MergedSPCAndMergeStatusWithMergingSPCsAndMapping resRest =
        res.getJoinSPCAndJoinStatusWithJoinedSPCsAndMapping();
    status = resRest.getMergeStatus();
    // Else, update parameters with result and let a be the returned value.
    spc1 = resRest.getMergingSPC1();
    spc2 = resRest.getMergingSPC2();
    mapping1 = resRest.getMapping1();
    mapping2 = resRest.getMapping2();
    newSPC = resRest.getMergedSPC();

    // 11. Introduce new HVE in d for offset nf and size ptr for a'.
    newSPC =
        newSPC.writeValue(d, BigInteger.valueOf(workingOffset), newSPC.getSizeOfPointer(), aStar);
    // 12. Return with value a
    Preconditions.checkArgument(newSPC.smg.isPointer(aStar));
    assert newSPC.getValueFromSMGValue(a).isPresent();
    assert newSPC.getValueFromSMGValue(aStar).isPresent();
    return Optional.of(
        MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue.of(
            a,
            MergedSPCAndMergeStatusWithMergingSPCsAndMapping.of(
                newSPC, status, spc1, spc2, mapping1, mapping2)));
  }

  /*
   * Tries to save a failing merge by inserting a 0+ in between list segments.
   * The right (v2) value points towards a concrete objects, while the left (v1) points towards an abstracted list.
   * We insert a 0+ element before the objects pointed to by right (v2) and merge
   * based on the 2 abstracted objects.
   * aNext is the next or last pointer of the linked-list left.
   *
   * The value (pointer) a points to the new copy of the left linked-list d1 called d.
   * JoinValues() in 10. merges the sub-SMG from the next or prev pointer of the linked-list to the
   * value a1 (v1). This ensures that the new linked-list is inserted,
   * but the rest is merged normally, skipping the linked-list in a sense.
   * (this is a mirrored copy of insertLeftLLAndJoin(),
   *   but we can't merge them due to the order of the called methods)
   */
  private static Optional<MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue>
      insertRightLLAndJoin(
          SymbolicProgramConfiguration pSpc1,
          SymbolicProgramConfiguration pSpc2,
          SMGValue v1,
          SMGValue v2,
          SymbolicProgramConfiguration pNewSpc,
          ImmutableMap<SMGNode, SMGNode> mapping1,
          ImmutableMap<SMGNode, SMGNode> mapping2,
          SMGMergeStatus initialJoinStatus,
          int nestingDiff)
          throws SMGException {
    assert areMappedValuesInSPC(pNewSpc, mapping1);
    assert areMappedValuesInSPC(pNewSpc, mapping2);
    Preconditions.checkArgument(
        pSpc2.smg.isPointer(v2)
            && pSpc2.smg.getPTEdge(v2).orElseThrow().pointsTo()
                instanceof SMGSinglyLinkedListSegment);
    SMGPointsToEdge pte2 = pSpc2.smg.getPTEdge(v2).orElseThrow();
    SMGTargetSpecifier targetSpec2 = pte2.targetSpecifier();
    SMGSinglyLinkedListSegment sll2 = (SMGSinglyLinkedListSegment) pte2.pointsTo();
    SMGDoublyLinkedListSegment dll2 = null;
    if (sll2 instanceof SMGDoublyLinkedListSegment) {
      dll2 = (SMGDoublyLinkedListSegment) sll2;
    }
    // 2. get working offset (nf) based on type of list
    int workingOffset; // nf
    if (targetSpec2.equals(SMGTargetSpecifier.IS_FIRST_POINTER)) {
      workingOffset = sll2.getNextOffset().intValue(); // nf
    } else if (targetSpec2.equals(SMGTargetSpecifier.IS_LAST_POINTER)) {
      Preconditions.checkNotNull(dll2);
      workingOffset = dll2.getPrevOffset().intValue();
    } else {
      Preconditions.checkArgument(targetSpec2.equals(SMGTargetSpecifier.IS_ALL_POINTER));
      // return recoverable failure
      return Optional.of(
          MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue.recoverableFailure(
              DELAYED_MERGE));
    }

    // 3. Get existing HVE in the target of v2 with the working offset found
    Preconditions.checkState(
        sll2.getSize().isNumericValue()
            && sll2.getSize()
                    .asNumericValue()
                    .bigIntegerValue()
                    .subtract(BigInteger.valueOf(workingOffset))
                    .compareTo(BigInteger.valueOf(workingOffset))
                >= 0);
    Optional<SMGHasValueEdge> maybeANext =
        pSpc2.smg.getHasValueEdgeByPredicate(
            sll2,
            h ->
                h.getOffset().intValue() == workingOffset
                    && h.getSizeInBits().equals(pSpc2.getSizeOfPointer()));
    SMGValue aNext = SMGValue.zeroValue();
    // joinFields might have zeroed the field (if there was no ptr/value before)
    if (maybeANext.isPresent()) {
      aNext = maybeANext.orElseThrow().hasValue();
    }

    SymbolicProgramConfiguration newSPC = pNewSpc;
    SMGMergeStatus status = initialJoinStatus;
    SymbolicProgramConfiguration spc1 = pSpc1;
    SymbolicProgramConfiguration spc2 = pSpc2;

    // 4. If m2(d2) exists:
    SMGObject d;
    if (mapping2.containsKey(sll2)) {
      // Let d = m2(d2)
      d = (SMGObject) mapping2.get(sll2);
      // If exists object: m1(o) == d, return recoverable failure
      for (SMGNode m1Values : mapping1.values()) {
        if (m1Values.equals(d)) {
          return Optional.of(
              MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue.recoverableFailure(
                  DELAYED_MERGE));
        }
      }
      // If m2(v2) does not exist, create a new value node a,
      //   and a new PTE a -> d (with offset/size of v2) for the new SMG,
      //   and extend the mapping of nodes such that m2(v2) = a.
      if (!mapping2.containsKey(v2)) {
        Value addressValue = SymbolicValueFactory.getInstance().newIdentifier(null);
        CType type2 = spc2.valueToTypeMap.get(v2);
        Preconditions.checkNotNull(type2);
        newSPC =
            newSPC.copyAndAddPointerFromAddressToMemory(
                addressValue, d, type2, pte2.getOffset(), 0, pte2.targetSpecifier());
        SMGValue newSMGValue = newSPC.getSMGValueFromValue(addressValue).orElseThrow();
        mapping2 =
            ImmutableMap.<SMGNode, SMGNode>builder()
                .putAll(mapping2)
                .put(v2, newSMGValue)
                .buildOrThrow();
        assert newSPC.smg.hasValue(newSMGValue);
        assert newSPC.getValueFromSMGValue(newSMGValue).isPresent();
      } else {
        //   Otherwise let a = m2(v2) and return with a.
        SMGValue a = (SMGValue) mapping2.get(v2);
        return Optional.of(
            MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue.of(
                a,
                MergedSPCAndMergeStatusWithMergingSPCsAndMapping.of(
                    newSPC, initialJoinStatus, pSpc1, pSpc2, mapping1, mapping2)));
      }
      // Let res = joinValues(status, SMG1, SMG2, new SMG, m1, m2, v1, aNext, ldiff).
      Optional<MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue> maybeRes =
          mergeValues(spc1, spc2, v1, aNext, newSPC, mapping1, mapping2, status, nestingDiff);
      if (maybeRes.isEmpty() || maybeRes.orElseThrow().getRecoverableFailure() == DELAYED_MERGE) {
        return Optional.empty();
      } else if (maybeRes.orElseThrow().isRecoverableFailure()) {
        // Investigate this, most likely the list of the left hand side is longer than the one on
        // the right
        throw new RuntimeException("Recoverable failure exception when merging");
      }
      MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue res = maybeRes.orElseThrow();
      // If res = bottom, return bottom.
      // TODO: this a is weird. It is never actually used in the algorithm. SMG1 also never used it.
      // SMGValue a = res.getSMGValue();
      MergedSPCAndMergeStatusWithMergingSPCsAndMapping resRest =
          res.getJoinSPCAndJoinStatusWithJoinedSPCsAndMapping();
      status = resRest.getMergeStatus();
      // Else, update parameters with result and let a be the returned value.
      spc1 = resRest.getMergingSPC1();
      spc2 = resRest.getMergingSPC2();
      mapping1 = resRest.getMapping1();
      mapping2 = resRest.getMapping2();
    }
    // 5. If m2(aNext) not existing and m1(v1) not existing and m2(aNext) != m1(v1), return
    // recoverable fail.
    if (mapping2.containsKey(aNext)
        && mapping1.containsKey(v1)
        && !mapping2.get(aNext).equals(mapping1.get(v1))) {
      return Optional.of(
          MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue.recoverableFailure(
              DELAYED_MERGE));
    }
    // 6. Update status with: If length of d2 == 0 -> ⊐, else incomparable
    if (sll2.getMinLength() == 0) {
      status = status.updateWith(SMGMergeStatus.LEFT_ENTAIL);
    } else {
      status = status.updateWith(SMGMergeStatus.INCOMPARABLE);
    }

    // 7. Extend new SMG with a fresh copy of the nf restricted sub-SMG of SMG1 rooted at d2 (sll2),
    // but excluding the nodes that are already mapped in m2, such that the copy of d2 is a linked
    // list d. (nf either next or prev, depending on specifier)
    // Then, extend the mapping m2 such that the newly created nodes in O u V are mapped from
    // the corresponding nodes of O2 u V2.
    // 8. Initialize the labeling of d to match the labeling of d2 up to minimum length,
    //      which is fixed to 0
    d = sll2.copyWithNewMinimumLength(0);
    // For SLLs we have the next pointer leading into this that's created below and aNext is the
    // next pointer of d which is mapped below as well.
    // For DLLs we have 2 cases, next pointer case: same as above + back of d is already known
    // (either the ptr or the obj or 0) while the back ptr of the "next" obj (behind aNext) is
    // mapped due to the mapping of m2(d2) = d
    //   For the prev ptr the same in reverse.
    MergedSPCAndMapping newSPCAndMapping =
        copySubSMGRootedAt(
            sll2,
            (SMGSinglyLinkedListSegment) d,
            spc2,
            newSPC,
            BigInteger.valueOf(workingOffset),
            mapping2);
    newSPC = newSPCAndMapping.getMergedSPC();
    mapping2 = newSPCAndMapping.getMapping();

    Preconditions.checkArgument(newSPC.heapObjects.contains(d));
    Preconditions.checkArgument(newSPC.smg.getObjects().contains(d));
    Preconditions.checkState(mapping2.containsKey(sll2) && mapping2.get(sll2).equals(d));
    Preconditions.checkArgument(((SMGSinglyLinkedListSegment) d).getMinLength() == 0);
    // 9. Let value a be the address such that the PTE of a equals the offset,
    //      size and target d if such an address already exists in new SMG.
    //    Otherwise, create a new value a with those characteristics and
    //      extend the mapping of nodes such that m2(v2) = a
    Optional<SMGValue> maybeAddressValue =
        newSPC.getAddressValueForPointsToTarget(d, pte2.getOffset(), targetSpec2);
    SMGValue a;
    if (maybeAddressValue.isEmpty()) {
      Value addressValue = SymbolicValueFactory.getInstance().newIdentifier(null);
      CType type2 = spc2.valueToTypeMap.get(v2);
      Preconditions.checkNotNull(type2);
      int nestingLevel = spc2.getNestingLevel(v2);
      newSPC =
          newSPC.copyAndAddPointerFromAddressToMemory(
              addressValue, d, type2, pte2.getOffset(), nestingLevel, pte2.targetSpecifier());
      a = newSPC.getSMGValueFromValue(addressValue).orElseThrow();
    } else {
      a = maybeAddressValue.orElseThrow();
    }
    // Remember the mapping of v2 to a
    mapping2 = ImmutableMap.<SMGNode, SMGNode>builder().putAll(mapping2).put(v2, a).buildOrThrow();
    assert newSPC.smg.hasValue(a);
    assert newSPC.getValueFromSMGValue(a).isPresent();

    // 10. Let res = joinValues(..., v1, aNext, ...); if bot, return bot.
    // Note: we merge the next/prev pointer of the linked-list aNext points to the object after the
    // abstracted list,
    //   skipping the abstracted list element.
    Optional<MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue> maybeRes =
        mergeValues(spc1, spc2, v1, aNext, newSPC, mapping1, mapping2, status, nestingDiff);
    if (maybeRes.isEmpty() || maybeRes.orElseThrow().getRecoverableFailure() == DELAYED_MERGE) {
      return maybeRes;
    } else if (maybeRes.orElseThrow().isRecoverableFailure()) {
      // Hand the extension request back, extend and then possibly restart this method
      return maybeRes;
    }
    // Else new value a' for the value returned.
    MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue res = maybeRes.orElseThrow();
    // If res = bottom, return bottom.
    SMGValue aStar = res.getSMGValue();
    MergedSPCAndMergeStatusWithMergingSPCsAndMapping resRest =
        res.getJoinSPCAndJoinStatusWithJoinedSPCsAndMapping();
    status = resRest.getMergeStatus();
    // Else, update parameters with result and let a be the returned value.
    spc1 = resRest.getMergingSPC1();
    spc2 = resRest.getMergingSPC2();
    mapping1 = resRest.getMapping1();
    mapping2 = resRest.getMapping2();
    newSPC = resRest.getMergedSPC();

    // 11. Introduce new HVE in d for offset nf and size ptr for a'.
    newSPC =
        newSPC.writeValue(d, BigInteger.valueOf(workingOffset), newSPC.getSizeOfPointer(), aStar);
    // 12. Return with value a
    Preconditions.checkArgument(newSPC.smg.isPointer(aStar));
    assert newSPC.getValueFromSMGValue(a).isPresent();
    assert newSPC.getValueFromSMGValue(aStar).isPresent();
    return Optional.of(
        MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue.of(
            a,
            MergedSPCAndMergeStatusWithMergingSPCsAndMapping.of(
                newSPC, status, spc1, spc2, mapping1, mapping2)));
  }

  /**
   * Adds pNewObject to heap objects (or external objects) including the SMG, copies all values from
   * pOldObject to the new object, copies the validity of the old to the new object, switches all
   * pointers from the old to the new object and then removes the old obj from the SMG and the SPC.
   *
   * @param pOldObject object to be replaced, copied and removed.
   * @param pNewObject new object that replaces pOldObject, i.e. all pointers pointing to pOldObject
   *     are switched to point at this and all values of pOldObject are copied to be in this obj.
   *     This obj retains the validity of pOldObject. This obj is expected to not be in the SPC/SMG
   *     yet.
   * @return new SPC with the changes.
   */
  public SymbolicProgramConfiguration copyAndReplaceObjectAndRemoveOld(
      SMGObject pOldObject, SMGObject pNewObject) {
    if (pOldObject == pNewObject) {
      return this;
    }

    PersistentMap<SMGObject, Boolean> newExternalObjectAllocation = externalObjectAllocation;
    if (newExternalObjectAllocation.containsKey(pOldObject)) {
      newExternalObjectAllocation =
          newExternalObjectAllocation
              .putAndCopy(pNewObject, newExternalObjectAllocation.get(pOldObject))
              .removeAndCopy(newExternalObjectAllocation);
    }

    Preconditions.checkState(isHeapObject(pOldObject));

    SymbolicProgramConfiguration newSPC =
        copyAndAddHeapObject(pNewObject)
            .copyWithNewSMG(smg.copyAndReplaceObject(pOldObject, pNewObject));

    Preconditions.checkState(
        newSPC.smg.getAllSourcesForPointersPointingTowards(pOldObject).isEmpty());

    return new SymbolicProgramConfiguration(
        newSPC.smg,
        newSPC.globalVariableMapping,
        newSPC.atExitStack,
        newSPC.stackVariableMapping,
        newSPC.heapObjects.removeAndCopy(pOldObject),
        newExternalObjectAllocation,
        newSPC.valueMapping,
        newSPC.variableToTypeMap,
        newSPC.memoryAddressAssumptionsMap.removeAndCopy(pOldObject),
        newSPC.mallocZeroMemory,
        newSPC.readBlacklist,
        newSPC.valueToTypeMap);
  }

  /**
   * Copies the SMG rooted at root with SPC rootSPC into newMemory with newSPC. First, copies all
   * values of root to newMemory except restrictionOffset. Then copies the memory behind pointers.
   * For all copies this tries to find mappings and use those if they exist. Values and objects are
   * mapped if they are not yet mapped.
   *
   * @param root abstracted list in rootSPC to be copied to newMemory.
   * @param newMemory the object root is copied into and the root of the copied sub-SMG.
   * @param rootSPC SPC for root and the source of the copied sub-SMG.
   * @param newSPC SPC for newMemory.
   * @param restrictionOffset the restricted offset (either NFO or PFO) not to be copied.
   * @param mappingOfStates the mapping already known of rootSPC -> newSPC
   * @return a new SPC, based on newSPC, with the sub-SMG copied. Note: mappingOfStates is assumed
   *     to be mutable, else we need to return this as well.
   * @throws SMGException in case of unimplemented features or critical errors.
   */
  private static MergedSPCAndMapping copySubSMGRootedAt(
      SMGSinglyLinkedListSegment root,
      SMGSinglyLinkedListSegment newMemory,
      SymbolicProgramConfiguration rootSPC,
      SymbolicProgramConfiguration newSPC,
      BigInteger restrictionOffset,
      ImmutableMap<SMGNode, SMGNode> mappingOfStates)
      throws SMGException {
    // Add all values. next/prev pointer is wrong here, depending on left/right sided
    // materialization! We write this later in the materialization.
    // If one of those is a pointer, we copy the pointer and memory structure
    Set<BigInteger> excludedOffsets = ImmutableSet.of(restrictionOffset);
    if (root instanceof SMGDoublyLinkedListSegment dllListSeg) {
      Preconditions.checkArgument(
          root.getNextOffset().equals(restrictionOffset)
              || dllListSeg.getPrevOffset().equals(restrictionOffset));
    } else {
      Preconditions.checkArgument(root.getNextOffset().equals(restrictionOffset));
    }
    return copyMemoryOfTo(root, newMemory, rootSPC, newSPC, mappingOfStates, excludedOffsets);
  }

  /**
   * Copies all values from rootObj to newMemory. Memory of pointers copied is then also copied,
   * except for the pointers at the offsets given in excludedOffsets. Will use already mapped values
   * or objects and will add them for non-existing mappings.
   *
   * <p>This method does NOT handle pointers towards the memory copied or the original memory of the
   * copy currently!
   *
   * @param rootObj object to be copied into newMemory with its sub-SMG.
   * @param newMemory target object.
   * @param rootSPC rootObjs SPC.
   * @param newSPC the new SPC to be the target of the copy.
   * @param mappingOfNodes already known mappings of rootSPC to newSPC.
   * @param excludedOffsets offsets not copied.
   * @return a new SPC based on newSPC with the sub-SMG added. Note: all mappings are added in
   *     mappingOfNodes, this is expected to be mutable, else we need to return it.
   * @throws SMGException for not implemented features or errors.
   */
  private static MergedSPCAndMapping copyMemoryOfTo(
      SMGObject rootObj,
      SMGObject newMemory,
      SymbolicProgramConfiguration rootSPC,
      SymbolicProgramConfiguration newSPC,
      ImmutableMap<SMGNode, SMGNode> mappingOfNodes,
      Set<BigInteger> excludedOffsets)
      throws SMGException {
    // We can't handle stack variables here. Alloca would be fine.
    // TODO: handle non variable stack memory here.
    Preconditions.checkState(rootSPC.isHeapObject(rootObj));
    SymbolicProgramConfiguration currentNewSPC = newSPC;
    SMGNode mappedNodeForRootObj = mappingOfNodes.get(rootObj);
    if (mappedNodeForRootObj != null) {
      // Already added/done
      Preconditions.checkState(mappedNodeForRootObj == newMemory);
      assert rootSPC
          .smg
          .getSMGObjectsWithSMGHasValueEdges()
          .getOrDefault(rootObj, PersistentSet.of())
          .equals(
              newSPC
                  .smg
                  .getSMGObjectsWithSMGHasValueEdges()
                  .getOrDefault(newMemory, PersistentSet.of()));
      return MergedSPCAndMapping.of(currentNewSPC, mappingOfNodes);
    } else {
      mappingOfNodes =
          ImmutableMap.<SMGNode, SMGNode>builder()
              .putAll(mappingOfNodes)
              .put(rootObj, newMemory)
              .buildOrThrow();
      currentNewSPC = newSPC.copyAndAddHeapObject(newMemory);
    }
    if (rootSPC.externalObjectAllocation.containsKey(rootObj)) {
      currentNewSPC = currentNewSPC.copyAndAddExternalObject(newMemory);
    }
    if (!rootSPC.isObjectValid(rootObj)) {
      currentNewSPC = currentNewSPC.invalidateSMGObject(newMemory, false);
    }
    // copyHVEdgesFromTo() uses the mapping and adds the mapping for SMGValues only
    // It does not copy memory. So while the SMGValue and Value of ptrs are present now, the PTE and
    // the Object behind them are missing.
    MergedSPCAndMapping currentNewSPCAndMapping =
        copyHVEdgesFromTo(
            rootSPC, rootObj, currentNewSPC, newMemory, mappingOfNodes, excludedOffsets);
    currentNewSPC = currentNewSPCAndMapping.getMergedSPC();
    mappingOfNodes = currentNewSPCAndMapping.getMapping();
    // All HVEs copied from root to new SPC, with the exception filtered out
    Set<SMGHasValueEdge> ptrValues =
        rootSPC
            .getSmg()
            .getSMGObjectsWithSMGHasValueEdges()
            .getOrDefault(rootObj, PersistentSet.of())
            .stream()
            .filter(
                hve ->
                    !excludedOffsets.contains(hve.getOffset())
                        && rootSPC.getSmg().isPointer(hve.hasValue())
                        && !hve.hasValue().isZero())
            .collect(ImmutableSet.toImmutableSet());

    // All values from the old root are copied to the new obj already,
    //   we might need to do some adjustments and copy memory
    // Now we have all values whose memory we might need to copy
    for (SMGHasValueEdge hve : ptrValues) {
      SMGValue smgValueRoot = hve.hasValue();
      SMGPointsToEdge pteRoot = rootSPC.smg.getPTEdge(smgValueRoot).orElseThrow();
      SMGObject rootTargetMemory = pteRoot.pointsTo();
      Preconditions.checkState(mappingOfNodes.containsKey(smgValueRoot));
      SMGValue newSMGValue = (SMGValue) mappingOfNodes.get(smgValueRoot);
      boolean copyMemoryRecursivly = false;

      // Replicate sub-SMG if not yet done (the value was copied, but not the PTE)
      if (!currentNewSPC.smg.isPointer(newSMGValue)) {
        SMGObject newMemoryTargetObject;
        SMGNode mappedNodeForTargetMemory = mappingOfNodes.get(rootTargetMemory);
        if (mappedNodeForTargetMemory != null) {
          // Known memory, just insert PTE
          newMemoryTargetObject = (SMGObject) mappedNodeForTargetMemory;
          Preconditions.checkState(currentNewSPC.isHeapObject(newMemoryTargetObject));
          assert currentNewSPC
              .smg
              .getSMGObjectsWithSMGHasValueEdges()
              .getOrDefault(newMemoryTargetObject, PersistentSet.of())
              .equals(rootSPC.smg.getEdges(rootTargetMemory));

        } else {
          // Copy memory and insert PTE
          Preconditions.checkState(!currentNewSPC.heapObjects.contains(rootTargetMemory));
          // We can't handle stack variables here. Alloca would be fine.
          // TODO: handle non variable stack memory here.
          Preconditions.checkState(rootSPC.isHeapObject(rootTargetMemory));
          newMemoryTargetObject = rootTargetMemory;
          copyMemoryRecursivly = true;
        }
        // Insert correct PTE in new SPC
        SMGPointsToEdge newPTE =
            new SMGPointsToEdge(
                newMemoryTargetObject, pteRoot.getOffset(), pteRoot.targetSpecifier());
        assert !currentNewSPC.smg.getPTEdges().anyMatch(e -> e.equals(newPTE));
        currentNewSPC =
            currentNewSPC.copyWithNewSMG(
                currentNewSPC.smg.copyAndAddPTEdgeWithInternalMapping(newPTE, newSMGValue));

        if (copyMemoryRecursivly) {
          // Now copy all values and copy all memory for pointers again recursively
          currentNewSPCAndMapping =
              copyMemoryOfTo(
                  rootTargetMemory,
                  rootTargetMemory,
                  rootSPC,
                  currentNewSPC,
                  mappingOfNodes,
                  ImmutableSet.of());
          currentNewSPC = currentNewSPCAndMapping.getMergedSPC();
          mappingOfNodes = currentNewSPCAndMapping.getMapping();
        }

      } else {
        // There is a PTE in the new SPC already, make some checks that it's OK.
        SMGPointsToEdge pteNew = currentNewSPC.smg.getPTEdge(newSMGValue).orElseThrow();
        Preconditions.checkState(pteRoot.getOffset().equals(pteNew.getOffset()));
        Preconditions.checkState(mappingOfNodes.containsKey(pteRoot.pointsTo()));
        Preconditions.checkState(mappingOfNodes.get(pteRoot.pointsTo()).equals(pteNew.pointsTo()));
        Preconditions.checkState(pteRoot.targetSpecifier().equals(pteNew.targetSpecifier()));
      }
    }
    return MergedSPCAndMapping.of(currentNewSPC, mappingOfNodes);
  }

  /*
   * Merges the sub-SMGs rooted at the target objects of v1 and v2 (which are addresses) and returns a single value that points to a single node that represents the merge of the target objects.
   *
   *
   * Returns bot for complete failure and rec fail if inserting a linked list might save it.
   */
  private static Optional<MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue>
      mergeTargetObjects(
          SymbolicProgramConfiguration pNewSPC,
          SymbolicProgramConfiguration pSpc1,
          SymbolicProgramConfiguration pSpc2,
          SMGValue v1,
          SMGValue v2,
          ImmutableMap<SMGNode, SMGNode> mapping1,
          ImmutableMap<SMGNode, SMGNode> mapping2,
          SMGMergeStatus initialJoinStatus,
          int nestingDiff)
          throws SMGException {
    Preconditions.checkNotNull(initialJoinStatus);
    Preconditions.checkArgument(pSpc1.smg.isPointer(v1)); // a1
    Preconditions.checkArgument(pSpc2.smg.isPointer(v2)); // a2

    // 1. If the offsets of the PTEs of the 2 values are not equal
    //      or the level diff of the 2 is not equal return recoverable failure
    SMGPointsToEdge pte1 = pSpc1.smg.getPTEdge(v1).orElseThrow(); // P1(a1)
    SMGPointsToEdge pte2 = pSpc2.smg.getPTEdge(v2).orElseThrow();
    if (!pte1.getOffset().equals(pte2.getOffset())) {
      return Optional.of(
          MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue.recoverableFailure(
              DELAYED_MERGE));
    } else if (pSpc1.smg.getNestingLevel(v1) - pSpc2.smg.getNestingLevel(v2) != nestingDiff) {
      return Optional.of(
          MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue.recoverableFailure(
              DELAYED_MERGE));
    }

    // 2. Let o1/2 be the targets of v1/2
    SMGObject o1 = pte1.pointsTo();
    SMGObject o2 = pte2.pointsTo();
    SymbolicProgramConfiguration newSPC = pNewSPC;
    SymbolicProgramConfiguration spc1 = pSpc1;
    SymbolicProgramConfiguration spc2 = pSpc2;

    // 3. If o1 = o2 = 0 or m1(o1) = m2(o1) (with existing mappings),
    //      let newSMG, the mappings and a be the result of mapTargetAdress() and return with those.
    //      In this case the targets are already joined,
    //      and we need to create a new address for the object o in newSMG.
    if ((o2.isZero() && o1.isZero())
        || (mapping1.containsKey(o1)
            && mapping2.containsKey(o2)
            && mapping1.get(o1).equals(mapping2.get(o2)))) {
      MergedSPCWithMappingsAndAddressValue res =
          mapTargetAddress(newSPC, spc1, spc2, v1, v2, mapping1, mapping2);
      newSPC = res.getMergedSPC();
      mapping1 = res.getMapping1();
      mapping2 = res.getMapping2();
      SMGValue a = res.getAddressValue();
      Preconditions.checkState(newSPC.valueToTypeMap.containsKey(a));
      Preconditions.checkState(newSPC.smg.isPointer(a));
      Preconditions.checkState(
          newSPC.smg.getPTEdge(a).orElseThrow().pointsTo().equals(mapping1.get(o1)));
      return Optional.of(
          MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue.of(
              a,
              MergedSPCAndMergeStatusWithMergingSPCsAndMapping.of(
                  newSPC, initialJoinStatus, spc1, spc2, mapping1, mapping2)));
    }

    // 4. If kind(o1) = kind(o2) and tg(P1(a1)) != tg(P2(a2)), return recoverable failure.
    if (!pte1.targetSpecifier().equals(pte2.targetSpecifier())
        && o1.getClass().equals(o2.getClass())) {
      return Optional.of(
          MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue.recoverableFailure(
              DELAYED_MERGE));
    }

    // 5. If kind(o1) != kind(o2) and m1(o1) != m2(o2), return recoverable failure.
    if (!o1.getClass().equals(o2.getClass())) {
      boolean contains1 = mapping1.containsKey(o1);
      boolean contains2 = mapping2.containsKey(o2);
      if (contains1 != contains2 || (contains1 && !mapping1.get(o1).equals(mapping2.get(o2)))) {
        // TODO: is this the correct intepretation?
        return Optional.of(
            MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue.recoverableFailure(
                DELAYED_MERGE));
      }
    }

    // 6. Let s = matchObjects(). If bottom, return recoverable failure.
    //      (This makes sure that the objects are the same size/validity etc.)
    Optional<SMGMergeStatusOrRecoverableFailure> s =
        matchObjects(initialJoinStatus, spc1, spc2, mapping1, mapping2, o1, o2);
    if (s.isEmpty() || s.orElseThrow().isRecoverableFailure()) {
      SMGRecoverableFailure failure =
          s.isEmpty() ? DELAYED_MERGE : s.orElseThrow().getRecoverableFailure();
      return Optional.of(
          MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue.recoverableFailure(failure));
    }
    SMGMergeStatus status = s.orElseThrow().getStatus();

    // 7. Create new Object o.
    // 8. Initialize labeling of o to match the labeling of o1 if kind(o1) = dls, or to match the
    // labeling of o2 if kind(o2) = dls, otherwise take the labeling from any of them (since they
    // are equal).
    // 9. If LL, let min length = min of o1 or o2
    // 10. Let level(o) = max level of o1 and o2
    SMGObject o = o1.join(o2);
    // TODO: We could also have stack objs due to alloca(), if we hit this, implement it.
    Preconditions.checkState(spc1.heapObjects.contains(o1) && spc2.heapObjects.contains(o2));
    assert o.getMinLength() == Integer.min(o1.getMinLength(), o2.getMinLength());
    if (!newSPC.heapObjects.contains(o)) {
      newSPC = newSPC.copyAndAddHeapObject(o);
    }
    if (!pSpc1.smg.isValid(o1)) {
      newSPC = newSPC.invalidateSMGObject(o, false);
    }
    // 11. If m1(o1) exists, replace each edge leading to m1(o1) by an equally labeled edge leading
    // to o.
    //     Remove m1(o1) together with all nodes and edges of G that are reachable via m1(o1) only,
    //     and remove the items of m1 whose target nodes were removed.
    boolean mapO1 = true;
    boolean mapO2 = true;
    if (mapping1.containsKey(o1) && mapping1.get(o1) != o) {
      SMGObject oldTarget1 = (SMGObject) mapping1.get(o1);
      // Replace pointers in SMG
      newSPC = newSPC.replaceAllPointersTowardsWith(oldTarget1, o);
      Preconditions.checkState(
          newSPC
              .smg
              .getAllSourcesForPointersPointingTowardsWithNumOfOccurrences(oldTarget1)
              .isEmpty());
      // Remove old objs
      SPCAndSMGObjects newSPCAndRemoved = newSPC.copyAndRemoveObjectAndAssociatedSubSMG(oldTarget1);
      newSPC = newSPCAndRemoved.getSPC();
      Collection<SMGObject> removed1 = newSPCAndRemoved.getSMGObjects();
      ImmutableMap.Builder<SMGNode, SMGNode> newMapping1 = ImmutableMap.builder();
      for (Entry<SMGNode, SMGNode> mappingIn1 : mapping1.entrySet()) {
        if (mappingIn1.getValue() instanceof SMGValue m1v) {
          if (newSPC.smg.isPointer(m1v)) {
            if (!removed1.contains(newSPC.smg.getPTEdge(m1v).orElseThrow().pointsTo())) {
              newMapping1.put(mappingIn1);
            }
          } else {
            newMapping1.put(mappingIn1);
          }
        } else if (!removed1.contains(mappingIn1.getValue())) {
          newMapping1.put(mappingIn1);
        }
      }

      mapping1 = newMapping1.buildOrThrow();
      Preconditions.checkArgument(!mapping1.containsKey(o1));
    } else if (mapping1.containsKey(o1)) {
      mapO1 = false;
    }

    //     If m2(o2) exists, do the same.
    //     This performs a delayed join (C.6)
    if (mapping2.containsKey(o2) && mapping2.get(o2) != o) {
      SMGObject oldTarget2 = (SMGObject) mapping2.get(o2);
      newSPC = newSPC.replaceAllPointersTowardsWith(oldTarget2, o);
      Preconditions.checkState(
          newSPC
              .smg
              .getAllSourcesForPointersPointingTowardsWithNumOfOccurrences(oldTarget2)
              .isEmpty());
      // Remove old objs
      SPCAndSMGObjects newSPCAndRemoved = newSPC.copyAndRemoveObjectAndAssociatedSubSMG(oldTarget2);
      newSPC = newSPCAndRemoved.getSPC();
      Collection<SMGObject> removed2 = newSPCAndRemoved.getSMGObjects();
      ImmutableMap.Builder<SMGNode, SMGNode> newMapping2 = ImmutableMap.builder();
      for (Entry<SMGNode, SMGNode> mappingIn2 : mapping2.entrySet()) {
        if (mappingIn2.getValue() instanceof SMGValue m2v) {
          if (newSPC.smg.isPointer(m2v)) {
            if (!removed2.contains(newSPC.smg.getPTEdge(m2v).orElseThrow().pointsTo())) {
              newMapping2.put(mappingIn2);
            }
          } else {
            newMapping2.put(mappingIn2);
          }
        } else if (!removed2.contains(mappingIn2.getValue())) {
          newMapping2.put(mappingIn2);
        }
      }

      mapping2 = newMapping2.buildOrThrow();
      Preconditions.checkArgument(!mapping2.containsKey(o2));
    } else if (mapping2.containsKey(o2)) {
      mapO2 = false;
    }

    // 12. Extend mapping
    if (mapO1) {
      mapping1 =
          ImmutableMap.<SMGNode, SMGNode>builder().putAll(mapping1).put(o1, o).buildOrThrow();
    }
    if (mapO2) {
      mapping2 =
          ImmutableMap.<SMGNode, SMGNode>builder().putAll(mapping2).put(o2, o).buildOrThrow();
    }
    assert mapping1.get(o1) == o;
    assert mapping2.get(o2) == o;

    // 13. Let (G, m1 , m2 , a) := mapTargetAddress(G1 , G2 , G, m1 , m2 , a1 , a2 ).
    MergedSPCWithMappingsAndAddressValue resMapTargetAddress =
        mapTargetAddress(newSPC, spc1, spc2, v1, v2, mapping1, mapping2);
    SMGValue a = resMapTargetAddress.getAddressValue();
    newSPC = resMapTargetAddress.getMergedSPC();
    Preconditions.checkState(newSPC.smg.isPointer(a));
    Preconditions.checkState(newSPC.valueToTypeMap.containsKey(a));
    Preconditions.checkState(newSPC.smg.getPTEdge(a).orElseThrow().pointsTo().equals(o));
    mapping1 = resMapTargetAddress.getMapping1();
    mapping2 = resMapTargetAddress.getMapping2();

    // 14. Let res := joinSubSMGs(s, G1 , G2 , G, m1 , m2 , o1 , o2 , o, ldif f ).
    Optional<MergedSPCAndMergeStatusWithMergingSPCsAndMapping> maybeRes =
        mergeSubSMGs(spc1, spc2, o1, o2, newSPC, o, status, mapping1, mapping2, nestingDiff);
    // If res = ⊥, return ⊥.
    if (maybeRes.isEmpty()) {
      return Optional.empty();
    } else if (maybeRes.orElseThrow().isRecoverableFailure()) {
      SMGRecoverableFailure recFailure = maybeRes.orElseThrow().getRecoverableFailure();
      Preconditions.checkArgument(recFailure != DELAYED_MERGE);
      return Optional.of(
          MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue.recoverableFailure(recFailure));
    }
    MergedSPCAndMergeStatusWithMergingSPCsAndMapping res = maybeRes.orElseThrow();
    newSPC = res.getMergedSPC();
    spc1 = res.getMergingSPC1();
    spc2 = res.getMergingSPC2();
    status = res.getMergeStatus();
    mapping1 = res.getMapping1();
    mapping2 = res.getMapping2();
    // Otherwise return (s, G1 , G2 , G, m1 , m2 , a).
    return Optional.of(
        MergedSPCAndMergeStatusWithMergingSPCsAndMappingAndValue.of(
            a,
            MergedSPCAndMergeStatusWithMergingSPCsAndMapping.of(
                newSPC, status, spc1, spc2, mapping1, mapping2)));
  }

  private static MergedSPCWithMappingsAndAddressValue mapTargetAddress(
      SymbolicProgramConfiguration pMergedSPC,
      SymbolicProgramConfiguration pMergingSPC1,
      SymbolicProgramConfiguration pMergingSPC2,
      SMGValue a1,
      SMGValue a2,
      ImmutableMap<SMGNode, SMGNode> pMapping1,
      ImmutableMap<SMGNode, SMGNode> pMapping2)
      throws SMGException {
    // 1. Let o1 := o(P1(a1)), of := of (P1(a1)).
    SMGPointsToEdge pte1 = pMergingSPC1.smg.getPTEdge(a1).orElseThrow();
    SMGPointsToEdge pte2 = pMergingSPC2.smg.getPTEdge(a2).orElseThrow();
    Preconditions.checkArgument(pte1.getOffset().equals(pte2.getOffset()));
    Value offset = pte1.getOffset();
    SMGObject o1 = pte1.pointsTo();
    SMGObject o2 = pte2.pointsTo();
    SMGObject o;
    // 2. If o1 = #, let o := #. Otherwise, let o := m1(o1).
    if (o1.isZero()) {
      o = SMGObject.nullInstance();
    } else {
      Preconditions.checkArgument(pMapping1.containsKey(o1) && pMapping2.containsKey(o2));
      o = (SMGObject) pMapping1.get(o1);
      Preconditions.checkArgument(o.equals(pMapping2.get(o2)));
    }
    // 3. If kind1(o1) = dls, let tg := tg(P1(a1)). Otherwise, let tg := tg(P2(a2)).
    SMGTargetSpecifier tg;
    if (o1 instanceof SMGSinglyLinkedListSegment) {
      tg = pte1.targetSpecifier();
    } else {
      tg = pte2.targetSpecifier();
    }
    // 4. If there is an address a ∈ A such that P (a) = (of, tg, o), return (G, m1 , m2 , a).
    Set<SMGValue> addressesPointingAtNewObj = pMergedSPC.smg.getPointerValuesForTarget(o);
    for (SMGValue maybeA : addressesPointingAtNewObj) {
      SMGPointsToEdge pte = pMergedSPC.smg.getPTEdge(maybeA).orElseThrow();
      if (pte.targetSpecifier().equals(tg) && pte.getOffset().equals(offset)) {
        // Found PTE, find value and return
        return MergedSPCWithMappingsAndAddressValue.of(pMergedSPC, maybeA, pMapping1, pMapping2);
      }
    }
    SymbolicProgramConfiguration mergedSPC = pMergedSPC;
    // 5. Extend A by a fresh address a, then extend P by a new points-to edge a−of,tg−→o.
    // Note: The nesting level may differ here by 1, if we are currently merging a nested object
    // with a non nested object.
    Preconditions.checkArgument(offset.isNumericValue());
    CType type1 = pMergingSPC1.valueToTypeMap.get(a1);
    CType type2 = pMergingSPC2.valueToTypeMap.get(a2);
    if (type1 == null
        || type2 == null
        || !type1.getCanonicalType().equals(type2.getCanonicalType())) {
      // Fix or return Optional.Empty if this is hit
      throw new SMGException("Unequal types when merging SMGStates.");
    }
    Value newAddressValue = SymbolicValueFactory.getInstance().newIdentifier(null);
    mergedSPC =
        mergedSPC.copyAndAddPointerFromAddressToRegionWithNestingLevel(
            newAddressValue,
            o,
            type1,
            offset.asNumericValue().bigIntegerValue(),
            pMergingSPC1.smg.getNestingLevel(a1),
            tg);
    SMGValue a = mergedSPC.getSMGValueFromValue(newAddressValue).orElseThrow();
    // 6. Extend the mapping of nodes such that m1 (a1) = m2 (a2) = a.
    pMapping1 =
        ImmutableMap.<SMGNode, SMGNode>builder().putAll(pMapping1).put(a1, a).buildOrThrow();
    pMapping2 =
        ImmutableMap.<SMGNode, SMGNode>builder().putAll(pMapping2).put(a2, a).buildOrThrow();
    assert mergedSPC.smg.hasValue(a);
    assert mergedSPC.getValueFromSMGValue(a).isPresent();
    // 7. Return (G, m1 , m2 , a).
    return MergedSPCWithMappingsAndAddressValue.of(mergedSPC, a, pMapping1, pMapping2);
  }

  /**
   * Merges the fields of 2 objects of 2 SMGs as far as possible without looking at the details of
   * values besides the zero value. One can expect that if there is 2 distinct values in the same
   * position of the object in the 2 SMGs, it is NOT changed in either. See joinValues() for that.
   */
  private static Optional<MergingSPCsAndMergeStatus> mergeFields(
      SymbolicProgramConfiguration spc1,
      SymbolicProgramConfiguration spc2,
      SMGObject obj1,
      SMGObject obj2)
      throws SMGException {
    // TODO: this method can be heavily refactored once we don't need all the info for debugging ;D

    // TreeMap is sorted. Entry set and even key/value sets are always sorted ascending by key.
    PersistentSet<SMGHasValueEdge> hves1 =
        spc1.smg.getSMGObjectsWithSMGHasValueEdges().getOrDefault(obj1, PersistentSet.of());
    PersistentSet<SMGHasValueEdge> hves2 =
        spc2.smg.getSMGObjectsWithSMGHasValueEdges().getOrDefault(obj2, PersistentSet.of());

    SortedMap<Integer, SMGHasValueEdge> offsetsToZero1 = new TreeMap<>();
    ImmutableMap.Builder<Integer, SMGHasValueEdge> offsetsToNonZeroPtrs1Builder =
        ImmutableMap.builder();
    // ImmutableMap.Builder<Integer, SMGHasValueEdge> offsetsToNonZeroNonPtrs1Builder =
    // ImmutableMap.builder();
    for (SMGHasValueEdge hve1 : hves1) {
      SMGValue value = hve1.hasValue();
      int offset = hve1.getOffset().intValue();
      if (value.isZero()) {
        offsetsToZero1.put(offset, hve1);
      } else if (spc1.smg.isPointer(value)) {
        offsetsToNonZeroPtrs1Builder.put(offset, hve1);
      }
    }

    ImmutableMap<Integer, SMGHasValueEdge> offsetsToNonZeroPtrs1 =
        offsetsToNonZeroPtrs1Builder.buildOrThrow();
    // Might be useful later
    // ImmutableMap<Integer, SMGHasValueEdge> offsetsToNonZeroNonPtrs1 =
    // offsetsToNonZeroNonPtrs1Builder.build();

    SortedMap<Integer, SMGHasValueEdge> offsetsToZero2 = new TreeMap<>();
    ImmutableMap.Builder<Integer, SMGHasValueEdge> offsetsToNonZeroPtrs2Builder =
        ImmutableMap.builder();
    // ImmutableMap.Builder<Integer, SMGHasValueEdge> offsetsToNonZeroNonPtrs2Builder =
    // ImmutableMap.builder();
    for (SMGHasValueEdge hve2 : hves2) {
      SMGValue value = hve2.hasValue();
      int offset = hve2.getOffset().intValue();
      if (value.isZero()) {
        offsetsToZero2.put(offset, hve2);
      } else if (spc2.smg.isPointer(value)) {
        offsetsToNonZeroPtrs2Builder.put(offset, hve2);
      }
    }

    ImmutableMap<Integer, SMGHasValueEdge> offsetsToNonZeroPtrs2 =
        offsetsToNonZeroPtrs2Builder.buildOrThrow();
    // Might be useful later
    // ImmutableMap<Integer, SMGHasValueEdge> offsetsToNonZeroNonPtrs2 =
    // offsetsToNonZeroNonPtrs2Builder.build();

    // 2. Process the set of pointers in HVEs for both SMGs in relation to the other.
    //    First, remove all 0 edges that are not present in both objects and extend it again with
    // the minimal set of 0s.
    // It is actually important here not to use a write reinterpretation,
    //   we want the offsets/sizes of even 0 edges to match between the objects as far as possible.

    //   a  First, remove all 0 edges
    for (SMGHasValueEdge zeroEdge1 : offsetsToZero1.values()) {
      hves1 = hves1.removeAndCopy(zeroEdge1);
    }
    for (SMGHasValueEdge zeroEdge2 : offsetsToZero2.values()) {
      hves2 = hves2.removeAndCopy(zeroEdge2);
    }
    // TODO: is the minimal overlapping set the same?
    // TODO: optimize

    //   b  Extend the sets without zeros with the smallest set of edges to zero, that both original
    // sets shared. (Both have the same 0 edges after this)

    // Adds the zero edges for hves1
    hves1 =
        addMinimalOverlappingZeroEdgesTo(offsetsToZero1.values(), offsetsToZero2.values(), hves1);
    // Adds the zero edges for hves2
    hves2 =
        addMinimalOverlappingZeroEdgesTo(offsetsToZero2.values(), offsetsToZero1.values(), hves2);

    // Gather all non-zero pointers in the 2 sets that are not in the other
    ImmutableSet.Builder<SMGHasValueEdge> hves1NotZeroPtrsNotIn2Builder = ImmutableSet.builder();
    for (SMGHasValueEdge h1 : offsetsToNonZeroPtrs1.values()) {
      if (hves2 == null) {
        hves1NotZeroPtrsNotIn2Builder.add(h1);
        continue;
      }
      ImmutableList<SMGHasValueEdge> allEqualEdgesIn2 =
          hves2.stream()
              .filter(
                  h2 ->
                      h2.getOffset().equals(h1.getOffset())
                          && h2.getSizeInBits().equals(h1.getSizeInBits()))
              .collect(ImmutableList.toImmutableList());
      if (allEqualEdgesIn2.size() > 1) {
        throw new SMGException("Error when merging.");
      } else if (allEqualEdgesIn2.isEmpty()) {
        hves1NotZeroPtrsNotIn2Builder.add(h1);
      } else {
        SMGHasValueEdge hve2 = allEqualEdgesIn2.get(0);
        assert !hve2.hasValue().isZero();
        if (!spc2.smg.isPointer(hve2.hasValue())) {
          hves1NotZeroPtrsNotIn2Builder.add(h1);
        }
      }
    }
    Set<SMGHasValueEdge> hves1NotZeroPtrsNotIn2 = hves1NotZeroPtrsNotIn2Builder.build();

    ImmutableSet.Builder<SMGHasValueEdge> hves2NotZeroPtrsNotIn1Builder = ImmutableSet.builder();
    for (SMGHasValueEdge h2 : offsetsToNonZeroPtrs2.values()) {
      if (hves1 == null) {
        hves2NotZeroPtrsNotIn1Builder.add(h2);
        continue;
      }
      ImmutableList<SMGHasValueEdge> allEqualEdgesIn1 =
          hves1.stream()
              .filter(
                  h1 ->
                      h1.getOffset().equals(h2.getOffset())
                          && h1.getSizeInBits().equals(h2.getSizeInBits()))
              .collect(ImmutableList.toImmutableList());
      if (allEqualEdgesIn1.size() > 1) {
        throw new SMGException("Error when merging.");
      } else if (allEqualEdgesIn1.isEmpty()) {
        hves2NotZeroPtrsNotIn1Builder.add(h2);
      } else {
        SMGHasValueEdge hve1 = allEqualEdgesIn1.get(0);
        assert !hve1.hasValue().isZero();
        if (!spc1.smg.isPointer(hve1.hasValue())) {
          hves2NotZeroPtrsNotIn1Builder.add(h2);
        }
      }
    }
    Set<SMGHasValueEdge> hves2NotZeroPtrsNotIn1 = hves2NotZeroPtrsNotIn1Builder.build();

    SymbolicProgramConfiguration updatedSPC1 =
        spc1.copyWithNewSMG(spc1.smg.copyAndReplaceHVEdgesAt(obj1, hves1));
    SymbolicProgramConfiguration updatedSPC2 =
        spc2.copyWithNewSMG(spc2.smg.copyAndReplaceHVEdgesAt(obj2, hves2));

    //   c Then, extend each object, that does not include a (non 0) ptr at a location
    //      where the other has a non 0 ptr, with a 0 ptr
    // (After this, all 0 edges are either shared in both, or when one has a pointer where the other
    // had none, the none is replaced by a zero edge)
    for (SMGHasValueEdge hve2NotZeroPtrNotIn1 : hves2NotZeroPtrsNotIn1) {
      updatedSPC1 =
          updatedSPC1.writeValue(
              obj1,
              hve2NotZeroPtrNotIn1.getOffset(),
              hve2NotZeroPtrNotIn1.getSizeInBits(),
              SMGValue.zeroValue());
    }
    for (SMGHasValueEdge hve1NotZeroPtrNotIn1 : hves1NotZeroPtrsNotIn2) {
      updatedSPC2 =
          updatedSPC2.writeValue(
              obj2,
              hve1NotZeroPtrNotIn1.getOffset(),
              hve1NotZeroPtrNotIn1.getSizeInBits(),
              SMGValue.zeroValue());
    }
    // There might be concrete values on one, but not the other. So a mismatch in edges is possible!
    // But there should be a match in pointer edges (not target/equal, but if there is one in one
    // SMG, there is one in the other)

    // 3. equal merge status
    SMGMergeStatus status = SMGMergeStatus.EQUAL;

    // 4. Update join status based on the (new) zero edges in both
    //      If a 0 edge in the original obj1 is shrunk or no longer present, ⊏
    status =
        checkZeroEdgesAndUpdateStatus(
            updatedSPC1, offsetsToZero1, obj1, status, SMGMergeStatus.LEFT_ENTAIL);
    //      If a 0 edge in the original obj2 is shrunk or no longer present, ⊐
    status =
        checkZeroEdgesAndUpdateStatus(
            updatedSPC2, offsetsToZero2, obj2, status, SMGMergeStatus.RIGHT_ENTAIL);

    // 5. Add values present in ones SMGs object in the other as far as possible
    // TODO: this merging of values could be a Symbolic Expression of (0 OR other value)
    hves1 =
        updatedSPC1.smg.getSMGObjectsWithSMGHasValueEdges().getOrDefault(obj1, PersistentSet.of());
    Set<SMGHasValueEdge> hves1NotZerosNotPointers =
        hves1.stream()
            .filter(h -> !spc1.smg.isPointer(h.hasValue()))
            .collect(ImmutableSet.toImmutableSet());
    hves2 =
        updatedSPC2.smg.getSMGObjectsWithSMGHasValueEdges().getOrDefault(obj2, PersistentSet.of());
    Set<SMGHasValueEdge> hves2NotZerosNotPointers =
        hves2.stream()
            .filter(h -> !spc2.smg.isPointer(h.hasValue()))
            .collect(ImmutableSet.toImmutableSet());
    // Extend obj2 with new values where modified obj2 has a 0 edge but modified obj1 does not
    for (SMGHasValueEdge hve1NotZero : hves1NotZerosNotPointers) {
      BigInteger offset1 = hve1NotZero.getOffset();
      BigInteger size1 = hve1NotZero.getSizeInBits();
      List<SMGHasValueEdge> maybeHve2 =
          updatedSPC2.readValue(obj2, offset1, size1, false).getSMGHasValueEdges();
      if (maybeHve2.size() != 1) {
        throw new SMGException("Error when merging 2 has-value-edges.");
      }
      if (maybeHve2.get(0).hasValue().isZero()) {
        CType maybeV1Type = spc1.valueToTypeMap.get(hve1NotZero.hasValue());
        if (maybeV1Type == null) {
          throw new SMGException(
              "Error when merging. A new symbolic value could not have been created due to a"
                  + " missing type.");
        }
        maybeV1Type = maybeV1Type.getCanonicalType();
        Value valueToWrite = updatedSPC2.getNewSymbolicValueForType(maybeV1Type.getCanonicalType());
        SMGValue newSMGValue = SMGValue.of();
        // TODO: we can do better with SMT solvers
        updatedSPC2 = updatedSPC2.copyAndPutValue(valueToWrite, newSMGValue, 0, maybeV1Type);
        updatedSPC2 = updatedSPC2.writeValue(obj2, offset1, size1, newSMGValue);
      }
    }
    // Extend obj1 with new values where modified obj1 has a 0 edge but modified obj2 does not
    for (SMGHasValueEdge hve2NotZero : hves2NotZerosNotPointers) {
      BigInteger offset2 = hve2NotZero.getOffset();
      BigInteger size2 = hve2NotZero.getSizeInBits();
      List<SMGHasValueEdge> maybeHve1 =
          updatedSPC1.readValue(obj1, offset2, size2, false).getSMGHasValueEdges();
      if (maybeHve1.size() != 1) {
        throw new SMGException("Error when merging 2 has-value-edges.");
      }
      if (maybeHve1.get(0).hasValue().isZero()) {
        SMGValue smgValueFrom2 = hve2NotZero.hasValue();
        CType maybeV2Type = spc2.valueToTypeMap.get(smgValueFrom2);
        if (maybeV2Type == null) {
          throw new SMGException(
              "Error when merging. A new symbolic value could not have been created due to a"
                  + " missing type.");
        }
        maybeV2Type = maybeV2Type.getCanonicalType();
        SMGValue newSMGValue = SMGValue.of();
        // TODO: we can do better with SMT solvers
        Value valueToWrite = updatedSPC2.getNewSymbolicValueForType(maybeV2Type);
        updatedSPC2 = updatedSPC2.copyAndPutValue(valueToWrite, newSMGValue, 0, maybeV2Type);
        updatedSPC2 = updatedSPC2.writeValue(obj2, offset2, size2, newSMGValue);
      }
    }

    // Every field that was a pointer before, is a pointer now
    assert assertMergeFields(spc1, updatedSPC1, obj1);

    assert assertMergeFields(spc2, updatedSPC2, obj1);

    return Optional.of(MergingSPCsAndMergeStatus.of(updatedSPC1, updatedSPC2, status));
  }

  private static boolean assertMergeFields(
      SymbolicProgramConfiguration originalSPC,
      SymbolicProgramConfiguration updatedSPC,
      SMGObject objectMerged) {
    for (SMGHasValueEdge oldPointerHves :
        originalSPC.smg.getHasValueEdgesByPredicate(
            objectMerged, h -> originalSPC.smg.isPointer(h.hasValue()))) {
      Optional<SMGHasValueEdge> maybeNewHVE =
          updatedSPC.smg.getHasValueEdgeByPredicate(
              objectMerged,
              h ->
                  h.getOffset().equals(oldPointerHves.getOffset())
                      && h.getSizeInBits().equals(oldPointerHves.getSizeInBits()));
      if (oldPointerHves.hasValue().isZero()) {
        if (maybeNewHVE.isEmpty() || !maybeNewHVE.orElseThrow().hasValue().isZero()) {
          return false;
        }
      } else {
        if (maybeNewHVE.isEmpty()
            || !updatedSPC.smg.isPointer(maybeNewHVE.orElseThrow().hasValue())) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Checks if 0 edges have been changed and changes the status with the given status if they have
   * been changed.
   */
  private static SMGMergeStatus checkZeroEdgesAndUpdateStatus(
      SymbolicProgramConfiguration currentSPC,
      SortedMap<Integer, SMGHasValueEdge> originalZeroEdges,
      SMGObject object,
      SMGMergeStatus currentStatus,
      SMGMergeStatus newStatus) {
    SortedMap<Integer, SMGHasValueEdge> newOffsetsToZero = new TreeMap<>();
    for (SMGHasValueEdge hve :
        currentSPC
            .smg
            .getSMGObjectsWithSMGHasValueEdges()
            .getOrDefault(object, PersistentSet.of())) {
      if (hve.hasValue().isZero()) {
        newOffsetsToZero.put(hve.getOffset().intValue(), hve);
      }
    }

    for (Entry<Integer, SMGHasValueEdge> origEdge : originalZeroEdges.entrySet()) {
      SMGHasValueEdge maybeSameEdge = newOffsetsToZero.get(origEdge.getKey());
      // TODO: is this the correct interpretation of the following?
      // For each 0 ≤ i < size1 (o1 ):
      //   If ∃(e : o1 −of,t-> 0) ∈ H1 such that i ∈ I(e) and ∀(e´ : o1 -of´,t´->0) ∈ H1´ : i /∈I
      // (e´)
      //     then let s := updateJoinStatus(s, ...).
      // With H1 being the original 0 edges in the object, H1´ the new zero edges
      // and I(e) being some edge in the object where the edge originates from
      // and o1 -of,t->0 representing some zero edge in o1 with offset of and size t.
      if (maybeSameEdge == null
          || !maybeSameEdge.getSizeInBits().equals(origEdge.getValue().getSizeInBits())) {
        // If there is a zero edge in the original that is not present or the same size in the new
        //   -> change merge status
        return currentStatus.updateWith(newStatus);
      }
    }
    return currentStatus;
  }

  /**
   * Adds all 0 edges present in both to hves. The numbers in hves1OnlyZeros and hves2OnlyZeros do
   * not matter.
   */
  private static @Nullable PersistentSet<SMGHasValueEdge> addMinimalOverlappingZeroEdgesTo(
      Collection<SMGHasValueEdge> hves1OnlyZeros,
      Collection<SMGHasValueEdge> hves2OnlyZeros,
      PersistentSet<SMGHasValueEdge> hves) {
    for (SMGHasValueEdge zeroEdge1 : hves1OnlyZeros) {
      int offset1 = zeroEdge1.getOffset().intValueExact();
      int size1 = zeroEdge1.getSizeInBits().intValueExact();
      int offsetPlusSize1 = offset1 + size1;
      for (SMGHasValueEdge zeroEdge2 : hves2OnlyZeros) {
        int offset2 = zeroEdge2.getOffset().intValueExact();
        int size2 = zeroEdge2.getSizeInBits().intValueExact();
        int offsetPlusSize2 = offset2 + size2;
        if (Integer.max(offset1, offset2) <= Integer.min(offsetPlusSize1, offsetPlusSize2)) {
          int overlapStartOffset = Integer.max(offset1, offset2);
          int overlapSize = Integer.min(offsetPlusSize1, offsetPlusSize2) - overlapStartOffset;
          if (overlapSize > 0) {
            // TODO: merge 0 blocks? Is this even possibly needed here?
            hves =
                hves.addAndCopy(
                    new SMGHasValueEdge(
                        SMGValue.zeroValue(),
                        BigInteger.valueOf(overlapStartOffset),
                        BigInteger.valueOf(overlapSize)));
          }
        }
        // TODO: we use sorted collections, abort if exceeds
      }
    }
    return hves;
  }

  /**
   * Returns the canonical type for the value. Throws an {@link NullPointerException} if the type is
   * null.
   */
  public CType getTypeForValue(SMGValue value) {
    CType type = valueToTypeMap.get(value);
    Preconditions.checkNotNull(type);
    return type.getCanonicalType();
  }

  /**
   * Returns the canonical type for the value. Throws an {@link NullPointerException} if the type is
   * null.
   */
  public CType getTypeForValue(Value value) {
    return getTypeForValue(getSMGValueFromValue(value).orElseThrow());
  }

  /**
   * Returns a new symbolic constant value. This is meant to transform UNKNOWN Values into usable
   * values with unknown value but known type.
   *
   * @param valueType the {@link CType} of the Value. Don't use the canonical type if possible!
   * @return a new symbolic Value.
   */
  Value getNewSymbolicValueForType(CType valueType) {
    // For unknown values we use a new symbolic value without memory location as this is
    // handled by the SMGs
    SymbolicValueFactory factory = SymbolicValueFactory.getInstance();
    return factory.asConstant(factory.newIdentifier(null), valueType);
  }

  /**
   * Creates a new {@link SymbolicProgramConfiguration} out of the elements given.
   *
   * @param pSmg the {@link SMG} of this SPC.
   * @param pGlobalVariableMapping the global variable map as {@link PersistentMap} mapping {@link
   *     String} to {@link SMGObject}.
   * @param pStackVariableMapping the stack variable mappings as a {@link PersistentStack} of {@link
   *     StackFrame}s.
   * @param pHeapObjects the heap {@link SMGObject}s on a {@link PersistentStack}.
   * @param pExternalObjectAllocation externally allocated {@link SMGObject}s on a map that saves
   *     the validity of the object. False is invalid.
   * @param pValueMapping {@link BiMap} mapping the {@link Value}s and {@link SMGValue}s.
   * @param pVariableToTypeMap mapping variables (global and local) to their types.
   * @return the newly created {@link SymbolicProgramConfiguration}.
   */
  public static SymbolicProgramConfiguration of(
      SMG pSmg,
      PersistentMap<String, SMGObject> pGlobalVariableMapping,
      PersistentStack<Value> pAtExitStack,
      PersistentStack<StackFrame> pStackVariableMapping,
      PersistentSet<SMGObject> pHeapObjects,
      PersistentMap<SMGObject, Boolean> pExternalObjectAllocation,
      ImmutableBiMap<Equivalence.Wrapper<Value>, SMGValue> pValueMapping,
      PersistentMap<String, CType> pVariableToTypeMap,
      PersistentMap<SMGObject, BigInteger> pMemoryAddressAssumptionMap,
      PersistentMap<SMGObject, Boolean> pMallocZeroMemory,
      Set<SMGObject> pReadBlacklist,
      PersistentMap<SMGValue, CType> pValueToTypeMap) {
    return new SymbolicProgramConfiguration(
        pSmg,
        pGlobalVariableMapping,
        pAtExitStack,
        pStackVariableMapping,
        pHeapObjects,
        pExternalObjectAllocation,
        pValueMapping,
        pVariableToTypeMap,
        pMemoryAddressAssumptionMap,
        pMallocZeroMemory,
        pReadBlacklist,
        pValueToTypeMap);
  }

  /**
   * Creates a new, empty {@link SymbolicProgramConfiguration} and returns it.
   *
   * @param sizeOfPtr the size of the pointers in this new SPC in bits as {@link BigInteger}.
   * @return The newly created {@link SymbolicProgramConfiguration}.
   */
  public static SymbolicProgramConfiguration of(BigInteger sizeOfPtr) {
    PersistentMap<SMGObject, BigInteger> newMemoryAddressAssumptionsMap =
        PathCopyingPersistentTreeMap.of();
    newMemoryAddressAssumptionsMap =
        newMemoryAddressAssumptionsMap.putAndCopy(SMGObject.nullInstance(), BigInteger.ZERO);
    return new SymbolicProgramConfiguration(
        new SMG(sizeOfPtr),
        PathCopyingPersistentTreeMap.of(),
        PersistentStack.of(),
        PersistentStack.of(),
        PersistentSet.of(SMGObject.nullInstance()),
        PathCopyingPersistentTreeMap.of(),
        ImmutableBiMap.of(
            valueWrapper.wrap(new NumericValue(0)),
            SMGValue.zeroValue(),
            valueWrapper.wrap(new NumericValue(0.0f)),
            SMGValue.zeroFloatValue(),
            valueWrapper.wrap(new NumericValue(0.0)),
            SMGValue.zeroDoubleValue()),
        PathCopyingPersistentTreeMap.of(),
        newMemoryAddressAssumptionsMap,
        PathCopyingPersistentTreeMap.of());
  }

  /**
   * Returns the same SPC with the {@link PersistentStack} of {@link StackFrame}s replaced with the
   * given. Only to be used temporarily (e.g. to retrieve something of the StackFrame the top one)
   * such that the old stack is restored, as this method does not remove other information related
   * to the stack, for example memory/pointers etc.
   *
   * @param pNewStackFrames new {@link PersistentStack} of {@link StackFrame}s to replace the old.
   * @return a new SymbolicProgramConfiguration that is a copy of the current one with
   *     pNewStackFrames changed.
   */
  public SymbolicProgramConfiguration withNewStackFrame(
      PersistentStack<StackFrame> pNewStackFrames) {
    return new SymbolicProgramConfiguration(
        smg,
        globalVariableMapping,
        atExitStack,
        pNewStackFrames,
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory,
        readBlacklist,
        valueToTypeMap);
  }

  public SymbolicProgramConfiguration withNewValueMappings(
      ImmutableBiMap<Wrapper<Value>, SMGValue> pNewValueMappings) {
    return new SymbolicProgramConfiguration(
        smg,
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        pNewValueMappings,
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory,
        readBlacklist,
        valueToTypeMap);
  }

  public SymbolicProgramConfiguration copyAndRemoveHasValueEdges(
      SMGObject memory, Collection<SMGHasValueEdge> edgesToRemove) {
    SMG newSMG = smg.copyAndRemoveHVEdges(edgesToRemove, memory);
    return new SymbolicProgramConfiguration(
        newSMG,
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory,
        readBlacklist,
        valueToTypeMap);
  }

  /**
   * Returns the global variable mapping in a {@link PersistentMap} from String to the {@link
   * SMGObject}.
   */
  public PersistentMap<String, SMGObject> getGlobalVariableToSmgObjectMap() {
    return globalVariableMapping;
  }

  /**
   * Returns the numerically assumed address for a memory region as in C. (in Bytes!)
   *
   * @param memoryRegion Some memory region as {@link SMGObject}.
   * @return memory address in Bytes.
   */
  public BigInteger getNumericAssumptionForMemoryRegion(SMGObject memoryRegion) {
    Preconditions.checkArgument(memoryAddressAssumptionsMap.containsKey(memoryRegion));
    return memoryAddressAssumptionsMap.get(memoryRegion);
  }

  /**
   * Returns the map for memory regions to their numeric address assumption. This map is immutable.
   *
   * @return map for memory regions to their numeric address assumption.
   */
  public PersistentMap<SMGObject, BigInteger> getNumericAssumptionForMemoryRegionMap() {
    return memoryAddressAssumptionsMap;
  }

  /**
   * Adds a new entry in the numeric address assumptions map for the object given. Will return the
   * old map for a known object.
   *
   * @param newObject the new memory region
   * @return a new, immutable map with the numeric assumption added.
   */
  private PersistentMap<SMGObject, BigInteger> calculateNewNumericAddressMapForNewSMGObject(
      SMGObject newObject) {
    if (memoryAddressAssumptionsMap.containsKey(newObject)
        || !newObject.getSize().isNumericValue()) {
      return memoryAddressAssumptionsMap;
    }
    // Add buffer
    currentMemoryAssumptionMax = currentMemoryAssumptionMax.add(NUMERIC_MEMORY_BUFFER);
    PersistentMap<SMGObject, BigInteger> newMap =
        memoryAddressAssumptionsMap.putAndCopy(newObject, currentMemoryAssumptionMax);
    currentMemoryAssumptionMax =
        currentMemoryAssumptionMax.add(
            newObject.getSize().asNumericValue().bigIntegerValue().divide(BigInteger.valueOf(8)));
    return newMap;
  }

  /** Returns the SMG that models the memory used in this {@link SymbolicProgramConfiguration}. */
  public SMG getSmg() {
    return smg;
  }

  public int getNestingLevel(SMGValue pSMGValue) {
    return smg.getNestingLevel(pSMGValue);
  }

  /**
   * Returns the nesting level of the value given. This will not check if the given value is
   * existing in the SMG! It will fail if the value does not exist!
   *
   * @param pValue a {@link Value} with existing {@link SMGValue} mapping.
   * @return a nesting level >= 0
   */
  public int getNestingLevel(Value pValue) {
    return smg.getNestingLevel(getSMGValueFromValue(pValue).orElseThrow());
  }

  /**
   * Returns the size of the pointer used for the SMG of this {@link SymbolicProgramConfiguration}.
   */
  public BigInteger getSizeOfPointer() {
    return smg.getSizeOfPointer();
  }

  /**
   * Returns true if the value is a pointer that points to a 0+ abstracted list segment. Else false.
   *
   * @param value some {@link Value}. Does not have to be a pointer.
   * @return true for 0+ target. false else.
   */
  public boolean pointsToZeroPlus(Value value) {
    Preconditions.checkNotNull(value);
    return smg.pointsToZeroPlus(getSMGValueFromValue(value).orElse(null));
  }

  /**
   * Returns number of global and local variables on all stack frames. Note: this might be
   * surprisingly large and should only be used as comparison not face value. We use encoded
   * variables for Strings/functions etc.
   */
  int getNumberOfVariables() {
    int size = globalVariableMapping.size();
    for (StackFrame frame : stackVariableMapping) {
      size += frame.getVariables().size();
    }
    return size;
  }

  /**
   * Copies this {@link SymbolicProgramConfiguration} and adds the mapping from the variable
   * identifier pVarName to the {@link SMGObject} pNewObject.
   *
   * @param pNewObject - The {@link SMGObject} that the variable is mapped to.
   * @param pVarName - The variable identifier that the {@link SMGObject} is mapped to.
   * @return The copied SPC with the global object added.
   */
  public SymbolicProgramConfiguration copyAndAddGlobalObject(
      SMGObject pNewObject, String pVarName, CType type) {
    return of(
        smg.copyAndAddObject(pNewObject),
        globalVariableMapping.putAndCopy(pVarName, pNewObject),
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap.putAndCopy(pVarName, type),
        calculateNewNumericAddressMapForNewSMGObject(pNewObject),
        mallocZeroMemory,
        readBlacklist,
        valueToTypeMap);
  }

  /**
   * Returns the stack of {@link StackFrame}s modeling the function stacks of this {@link
   * SymbolicProgramConfiguration}.
   */
  public PersistentStack<StackFrame> getStackFrames() {
    return stackVariableMapping;
  }

  /**
   * Copies this {@link SymbolicProgramConfiguration} and adds the stack object given with the
   * variable name given to it.
   *
   * @param pNewObject the new stack object.
   * @param pVarName the name of the added stack object.
   * @return Copy of this SPC with the stack variable mapping added.
   */
  public SymbolicProgramConfiguration copyAndAddStackObject(
      SMGObject pNewObject, String pVarName, CType type) {
    return copyAndAddStackObject(pNewObject, pVarName, type, false);
  }

  /**
   * Copies this {@link SymbolicProgramConfiguration} and adds the stack object given with the
   * variable name given to it.
   *
   * @param pNewObject the new stack object.
   * @param pVarName the name of the added stack object.
   * @param type CType of the variable.
   * @param exceptionOnRead throws an exception if this object is ever read
   * @return Copy of this SPC with the stack variable mapping added.
   */
  public SymbolicProgramConfiguration copyAndAddStackObject(
      SMGObject pNewObject, String pVarName, CType type, boolean exceptionOnRead) {
    StackFrame currentFrame = stackVariableMapping.peek();

    // Sanity check for correct stack frames
    // Restriction because of tests that don't emulate stack frames
    if (pVarName.contains(":")) {
      String functionName = pVarName.substring(0, pVarName.indexOf(":"));
      Preconditions.checkArgument(
          currentFrame.getFunctionDefinition().getQualifiedName().equals(functionName));
    }

    PersistentStack<StackFrame> tmpStack = stackVariableMapping.popAndCopy();
    currentFrame = currentFrame.copyAndAddStackVariable(pVarName, pNewObject);
    Set<SMGObject> newReadBlacklist = readBlacklist;
    if (exceptionOnRead) {
      newReadBlacklist =
          ImmutableSet.<SMGObject>builder().addAll(newReadBlacklist).add(pNewObject).build();
    }
    return of(
        smg.copyAndAddObject(pNewObject),
        globalVariableMapping,
        atExitStack,
        tmpStack.pushAndCopy(currentFrame),
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap.putAndCopy(pVarName, type),
        calculateNewNumericAddressMapForNewSMGObject(pNewObject),
        mallocZeroMemory,
        newReadBlacklist,
        valueToTypeMap);
  }

  /* Adds the local variable given to the stack with the function name given */
  SymbolicProgramConfiguration copyAndAddStackObjectToSpecificStackFrame(
      String functionName, SMGObject pNewObject, String pVarName, CType type) {

    PersistentStack<StackFrame> topStack = PersistentStack.of();
    StackFrame currentFrame = stackVariableMapping.peek();
    PersistentStack<StackFrame> tmpStack = stackVariableMapping.popAndCopy();
    while (!currentFrame.getFunctionDefinition().getQualifiedName().equals(functionName)) {
      topStack = topStack.pushAndCopy(currentFrame);
      currentFrame = tmpStack.peek();
      tmpStack = tmpStack.popAndCopy();
    }

    currentFrame = currentFrame.copyAndAddStackVariable(pVarName, pNewObject);
    tmpStack = tmpStack.pushAndCopy(currentFrame);

    while (!topStack.isEmpty()) {
      tmpStack = tmpStack.pushAndCopy(topStack.peek());
      topStack = topStack.popAndCopy();
    }

    return of(
        smg.copyAndAddObject(pNewObject),
        globalVariableMapping,
        atExitStack,
        tmpStack,
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap.putAndCopy(pVarName, type),
        calculateNewNumericAddressMapForNewSMGObject(pNewObject),
        mallocZeroMemory,
        readBlacklist,
        valueToTypeMap);
  }

  /**
   * Copies this {@link SymbolicProgramConfiguration} and adds a {@link StackFrame} based on the
   * entered model and function definition. More information on StackFrames can be found in the
   * Stackframe class.
   *
   * @param pFunctionDefinition - The {@link CFunctionDeclaration} that the {@link StackFrame} will
   *     be based upon.
   * @param model - The {@link MachineModel} the new {@link StackFrame} be based upon.
   * @param variableArguments null for no variable arguments, else a ImmutableList (that may be
   *     EMPTY!) of the Values in order.
   * @return The SPC copy with the new {@link StackFrame}.
   */
  public SymbolicProgramConfiguration copyAndAddStackFrame(
      CFunctionDeclaration pFunctionDefinition,
      MachineModel model,
      @Nullable ImmutableList<Value> variableArguments) {
    StackFrame newStackFrame = new StackFrame(pFunctionDefinition, model, variableArguments);
    CType returnType = pFunctionDefinition.getType().getReturnType().getCanonicalType();
    if (returnType instanceof CVoidType) {
      // use a plain int as return type for void functions
      returnType = CNumericTypes.INT;
    }
    Optional<SMGObject> returnObj = newStackFrame.getReturnObject();
    if (returnObj.isEmpty()) {
      return of(
          smg,
          globalVariableMapping,
          atExitStack,
          stackVariableMapping.pushAndCopy(newStackFrame),
          heapObjects,
          externalObjectAllocation,
          valueMapping,
          variableToTypeMap,
          memoryAddressAssumptionsMap,
          mallocZeroMemory,
          readBlacklist,
          valueToTypeMap);
    }
    return of(
        smg.copyAndAddObject(returnObj.orElseThrow()),
        globalVariableMapping,
        atExitStack,
        stackVariableMapping.pushAndCopy(newStackFrame),
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap.putAndCopy(
            pFunctionDefinition.getQualifiedName() + "::__retval__", returnType),
        calculateNewNumericAddressMapForNewSMGObject(returnObj.orElseThrow()),
        mallocZeroMemory,
        readBlacklist,
        valueToTypeMap);
  }

  SymbolicProgramConfiguration copyAndAddDummyStackFrame() {
    StackFrame newStackFrame = StackFrame.ofDummyStackframe();
    return of(
        smg,
        globalVariableMapping,
        atExitStack,
        stackVariableMapping.pushAndCopy(newStackFrame),
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory,
        readBlacklist,
        valueToTypeMap);
  }

  /**
   * Copies this {@link SymbolicProgramConfiguration} and removes the stack variable given.
   *
   * @param pIdentifier - String identifier for the variable to be removed.
   * @return Copy of the SPC with the variable removed.
   */
  public SymbolicProgramConfiguration copyAndRemoveStackVariable(String pIdentifier) {
    // If a stack variable moves out of scope, there is not more than one frames which could
    // contain the variable
    if (stackVariableMapping.isEmpty()) {
      return this;
    }

    StackFrame frame = stackVariableMapping.peek();
    if (frame.containsVariable(pIdentifier)) {
      SMGObject objToRemove = frame.getVariable(pIdentifier);
      StackFrame newFrame = frame.copyAndRemoveVariable(pIdentifier);
      PersistentStack<StackFrame> newStack =
          stackVariableMapping.replace(f -> f == frame, newFrame);
      /* TODO: Remove unneeded types
      FluentIterable<SMGHasValueEdge> edgesInObj =
          smg.getHasValueEdgesByPredicate(objToRemove, e -> true);
       */
      SMG newSmg = smg.copyAndInvalidateObject(objToRemove, true);
      PersistentMap<SMGValue, CType> newValueToTypeMap = valueToTypeMap;
      /*
      for (SMGHasValueEdge hve : edgesInObj) {
        if (newSmg.getValuesToRegionsTheyAreSavedIn().getOrDefault(hve.hasValue(), PathCopyingPersistentTreeMap.of()).isEmpty()) {
          newValueToTypeMap = newValueToTypeMap.removeAndCopy();
        }
      }
       */
      return of(
          newSmg,
          globalVariableMapping,
          atExitStack,
          newStack,
          heapObjects,
          externalObjectAllocation,
          valueMapping,
          variableToTypeMap.removeAndCopy(pIdentifier),
          memoryAddressAssumptionsMap.removeAndCopy(objToRemove),
          mallocZeroMemory,
          readBlacklist,
          newValueToTypeMap);
    }
    return this;
  }

  /**
   * Copy SPC and add an object to the heap. * With checks: throws {@link IllegalArgumentException}
   * when asked to add an object already present.
   *
   * @param pObject Object to add.
   */
  public SymbolicProgramConfiguration copyAndAddHeapObject(SMGObject pObject) {
    if (heapObjects.contains(pObject)) {
      throw new IllegalArgumentException("Heap object already in the SMG: [" + pObject + "]");
    }
    return of(
        smg.copyAndAddObject(pObject),
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects.addAndCopy(pObject),
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        calculateNewNumericAddressMapForNewSMGObject(pObject),
        mallocZeroMemory,
        readBlacklist,
        valueToTypeMap);
  }

  // Only to be used by materialization to copy a SMGObject
  public SymbolicProgramConfiguration copyAllValuesFromObjToObj(
      SMGObject source, SMGObject target) {
    return copyHVEdgesFromTo(source, target);
  }

  // We need this here due to the update to the nesting level as this changes the SPC
  private SymbolicProgramConfiguration copyHVEdgesFromTo(SMGObject source, SMGObject target) {
    PersistentSet<SMGHasValueEdge> setOfValues =
        smg.getSMGObjectsWithSMGHasValueEdges().getOrDefault(source, PersistentSet.of());
    // We expect that there are NO edges in the target!
    assert smg.getSMGObjectsWithSMGHasValueEdges()
        .getOrDefault(target, PersistentSet.of())
        .isEmpty();

    SMG newSMG = smg;
    for (SMGHasValueEdge hve : setOfValues) {
      newSMG = newSMG.incrementValueToMemoryMapEntry(target, hve.hasValue());
    }
    return of(
        newSMG.copyAndSetHVEdges(setOfValues, target),
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory,
        readBlacklist,
        valueToTypeMap);
  }

  private static MergedSPCAndMapping copyHVEdgesFromTo(
      SymbolicProgramConfiguration sourceSPC,
      SMGObject source,
      SymbolicProgramConfiguration pTargetSPC,
      SMGObject target,
      ImmutableMap<SMGNode, SMGNode> mappingBetweenStates,
      Set<BigInteger> excludedOffsets)
      throws SMGException {
    SymbolicProgramConfiguration newTargetState = pTargetSPC;
    // Get edges in source
    PersistentSet<SMGHasValueEdge> setOfValuesSource =
        sourceSPC.smg.getSMGObjectsWithSMGHasValueEdges().getOrDefault(source, PersistentSet.of());
    // We expect that there are NO edges in the target!
    Preconditions.checkState(
        newTargetState
            .smg
            .getSMGObjectsWithSMGHasValueEdges()
            .getOrDefault(target, PersistentSet.of())
            .isEmpty());

    // Go through edges in source and copy all to the new, but use existing mappings if possible
    for (SMGHasValueEdge hveSource : setOfValuesSource) {
      if (excludedOffsets.contains(hveSource.getOffset())) {
        continue;
      }
      SMGValue smgValueSource = hveSource.hasValue();
      Value valueInSource = sourceSPC.getValueFromSMGValue(smgValueSource).orElseThrow();
      SMGValue smgValueInTarget = smgValueSource;
      Value valueInTarget = valueInSource;
      int nestingLevel = sourceSPC.getNestingLevel(smgValueSource);
      if (mappingBetweenStates.containsKey(smgValueSource)) {
        // We know the value mapping already, use it
        smgValueInTarget = (SMGValue) mappingBetweenStates.get(smgValueSource);
        Preconditions.checkArgument(newTargetState.smg.getValues().containsKey(smgValueInTarget));
        Optional<Value> maybeNewSPCValue = newTargetState.getValueFromSMGValue(smgValueInTarget);
        if (maybeNewSPCValue.isEmpty()) {
          throw new SMGException("Error when mapping values when merging.");
        }
        valueInTarget = maybeNewSPCValue.orElseThrow();
        nestingLevel = sourceSPC.getNestingLevel(smgValueSource);

      } else {
        // Never map 0, 0 is always present and the same values.
        if (!smgValueSource.isZero()) {
          // Add new mapping
          // Check that the target SPC/SMG does not have a mapping for this value
          Preconditions.checkState(!newTargetState.containsValueInMapping(valueInSource));
          Preconditions.checkState(!newTargetState.containsValueInMapping(smgValueInTarget));
          mappingBetweenStates =
              ImmutableMap.<SMGNode, SMGNode>builder()
                  .putAll(mappingBetweenStates)
                  .put(smgValueSource, smgValueInTarget)
                  .buildOrThrow();
        }
      }
      CType typeSource = sourceSPC.valueToTypeMap.get(smgValueSource);
      // Check that the type exists and can be copied
      Preconditions.checkNotNull(typeSource);

      // This only updates the nesting level if a mapping exists
      newTargetState =
          newTargetState.copyAndPutValue(valueInTarget, smgValueInTarget, nestingLevel, typeSource);
      newTargetState =
          newTargetState.writeValue(
              target, hveSource.getOffset(), hveSource.getSizeInBits(), smgValueInTarget);
      assert newTargetState.smg.hasValue(smgValueInTarget);
      assert newTargetState.getValueFromSMGValue(smgValueInTarget).isPresent();
    }

    return MergedSPCAndMapping.of(newTargetState, mappingBetweenStates);
  }

  public PersistentStack<Value> getAtExitStack() {
    return atExitStack;
  }

  public SymbolicProgramConfiguration copyAndReplaceAtExitStack(PersistentStack<Value> pStack) {
    return new SymbolicProgramConfiguration(
        smg,
        globalVariableMapping,
        pStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory);
  }

  /**
   * Checks if the entered {@link SMGObject} is part of the heap.
   *
   * @param pObject {@link SMGObject} to be checked.
   * @return true if the object is in the heap.
   */
  public boolean isHeapObject(SMGObject pObject) {
    return heapObjects.contains(pObject);
  }

  public PersistentSet<SMGObject> getHeapObjects() {
    return heapObjects;
  }

  /**
   * Returns the number of heap objects, with abstracted objects counted with their min length.
   * (Counts only valid objects)
   */
  public int getHeapObjectsMinSize() {
    int size = 0;
    for (SMGObject obj : heapObjects) {
      if (obj instanceof SMGSinglyLinkedListSegment sll) {
        size += sll.getMinLength();
      } else {
        size++;
      }
    }
    return size;
  }

  /**
   * Remove a top stack frame from the SMG, along with all objects in it, and any edges leading
   * from/to it.
   *
   * <p>TODO: A test case with (invalid) passing of an address of a dropped frame object outside,
   * and working with them. For that, we should probably keep those as invalid, so we can spot such
   * bugs.
   */
  public SymbolicProgramConfiguration copyAndDropStackFrame() {
    StackFrame frame = stackVariableMapping.peek();
    PersistentStack<StackFrame> newStack = stackVariableMapping.popAndCopy();
    SMG newSmg = smg;
    PersistentMap<String, CType> newVariableToTypeMap = variableToTypeMap;
    // Get all SMGObjects referenced by other stack frames
    Set<SMGObject> validObjects = getObjectsValidInOtherStackFrames();
    PersistentMap<SMGObject, BigInteger> newMemoryAddressAssumptionsMap =
        memoryAddressAssumptionsMap;
    for (SMGObject object : frame.getAllObjects()) {
      // Don't invalidate objects that are referenced by another stack frame!
      // Pointers from may be given as array argument, then we have the object, but don't own it,
      // hence no heap objs.
      if (!validObjects.contains(object) && !isHeapObject(object)) {
        newSmg = newSmg.copyAndInvalidateObject(object, false);
        newMemoryAddressAssumptionsMap = newMemoryAddressAssumptionsMap.removeAndCopy(object);
      }
    }
    for (String varName : frame.getVariables().keySet()) {
      newVariableToTypeMap = newVariableToTypeMap.removeAndCopy(varName);
    }
    assert newSmg.checkSMGSanity();
    // TODO: remove types of values no longer needed

    return of(
        newSmg,
        globalVariableMapping,
        atExitStack,
        newStack,
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        newVariableToTypeMap,
        newMemoryAddressAssumptionsMap,
        mallocZeroMemory,
        readBlacklist,
        valueToTypeMap);
  }

  protected Set<SMGObject> getObjectsValidInOtherStackFrames() {
    // Get all SMGObjects referenced by other stack frames
    ImmutableSet.Builder<SMGObject> validObjectsBuilder = ImmutableSet.builder();
    for (StackFrame otherFrame : stackVariableMapping.popAndCopy()) {
      validObjectsBuilder.addAll(otherFrame.getAllObjects());
    }
    return validObjectsBuilder.build();
  }

  /** For Tests only! */
  public SymbolicProgramConfiguration copyWithNewSMG(SMG pSmg) {
    return of(
        pSmg,
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory,
        readBlacklist,
        valueToTypeMap);
  }

  /**
   * Copies this {@link SymbolicProgramConfiguration} and returns a new SPC with all entered
   * unreachable SMGObjects pruned + a collection of the unreachable {@link SMGObject}s.
   *
   * @return The new SPC without the entered SMGObjects and the unreachable {@link SMGObject}s.
   */
  public SPCAndSMGObjects copyAndPruneUnreachable() {
    Collection<SMGObject> visibleObjects =
        FluentIterable.concat(
                globalVariableMapping.values(),
                FluentIterable.from(stackVariableMapping)
                    .transformAndConcat(StackFrame::getAllObjects))
            .toSet();
    SMGObjectsAndValues reachable = smg.collectReachableObjectsAndValues(visibleObjects);
    Set<SMGObject> unreachableObjects =
        new HashSet<>(Sets.difference(smg.getObjects(), reachable.getObjects()));

    // Remove 0 object
    unreachableObjects =
        unreachableObjects.stream()
            .filter(this::isObjectValid)
            .collect(ImmutableSet.toImmutableSet());

    SMG newSmg = smg.copyAndRemoveObjects(unreachableObjects);
    // copy into return collection
    PersistentSet<SMGObject> newHeapObjects = heapObjects;
    PersistentMap<SMGObject, BigInteger> newMemoryAddressAssumptionsMap =
        memoryAddressAssumptionsMap;
    for (SMGObject smgObject : unreachableObjects) {
      newHeapObjects = newHeapObjects.removeAndCopy(smgObject);
      newMemoryAddressAssumptionsMap = newMemoryAddressAssumptionsMap.removeAndCopy(smgObject);
    }
    assert newSmg.getObjects().size() == smg.getObjects().size();
    // TODO: remove types of values deleted
    return SPCAndSMGObjects.of(
        of(
            newSmg,
            globalVariableMapping,
            atExitStack,
            stackVariableMapping,
            newHeapObjects,
            externalObjectAllocation,
            valueMapping,
            variableToTypeMap,
            newMemoryAddressAssumptionsMap,
            mallocZeroMemory,
            readBlacklist,
            valueToTypeMap),
        unreachableObjects);
  }

  public SymbolicProgramConfiguration replaceSMGValueNestingLevel(SMGValue value, int newLevel) {
    return copyAndReplaceSMG(smg.replaceSMGValueNestingLevel(value, newLevel));
  }

  /**
   * Removes all objects of the sub-SMG rooted below the given object, including the given object,
   * that are not reachable from outside the sub-SMG of the root object.
   *
   * @param root {@link SMGObject} to be removed.
   * @return a new SPC with the object and subSMG removed and all removed objects.
   */
  public SPCAndSMGObjects copyAndRemoveObjectAndAssociatedSubSMG(SMGObject root) {
    if (root.isZero()) {
      return SPCAndSMGObjects.of(this, ImmutableSet.of());
    }

    SMGAndSMGObjects newSMGAndRemovedObjects = smg.copyAndRemoveObjectAndSubSMG(root);
    SMG newSMG = newSMGAndRemovedObjects.getSMG();

    PersistentSet<SMGObject> newHeapObject = heapObjects;
    PersistentMap<SMGObject, BigInteger> newMemoryAddressAssumptionsMap =
        memoryAddressAssumptionsMap;
    for (SMGObject toRemove : newSMGAndRemovedObjects.getSMGObjects()) {
      newHeapObject = newHeapObject.removeAndCopy(toRemove);
      newMemoryAddressAssumptionsMap = newMemoryAddressAssumptionsMap.removeAndCopy(root);
    }

    SymbolicProgramConfiguration newSPC =
        of(
            newSMG,
            globalVariableMapping,
            atExitStack,
            stackVariableMapping,
            newHeapObject,
            externalObjectAllocation,
            valueMapping,
            variableToTypeMap,
            newMemoryAddressAssumptionsMap,
            mallocZeroMemory,
            readBlacklist,
            valueToTypeMap);

    return SPCAndSMGObjects.of(newSPC, newSMGAndRemovedObjects.getSMGObjects());
  }

  /** Returns {@link SMGObject} reserved for the return value of the current StackFrame. */
  public Optional<SMGObject> getReturnObjectForCurrentStackFrame() {
    return stackVariableMapping.peek().getReturnObject();
  }

  /** Returns true if there is a return object for the current stack frame. */
  public boolean hasReturnObjectForCurrentStackFrame() {
    return stackVariableMapping.peek().getReturnObject().isPresent();
  }

  public SymbolicProgramConfiguration replacePointerValuesWithExistingOrNew(
      SMGObject pOldTargetObj, SMGValue pointerToNewObj, Set<SMGTargetSpecifier> pSpecifierToSwitch)
      throws SMGException {
    if (!smg.isPointer(pointerToNewObj)) {
      throw new SMGException(
          "Non-address value found when trying to replace a pointer. This might be caused by"
              + " overapproximations, e.g. removal of concrete values (null).");
    }
    SMGObject newTargetObj = smg.getPTEdge(pointerToNewObj).orElseThrow().pointsTo();

    SMGAndSMGValues newSMGAndNewValuesForMapping =
        smg.replaceHVEPointersWithExistingHVEPointers(
            pOldTargetObj, newTargetObj, pSpecifierToSwitch);
    SymbolicProgramConfiguration newSPC = copyAndReplaceSMG(newSMGAndNewValuesForMapping.getSMG());
    for (Entry<SMGValue, SMGValue> oldNewSMGValue :
        newSMGAndNewValuesForMapping.getSMGValues().entrySet()) {
      SMGValue newSMGValue = oldNewSMGValue.getValue();
      SMGValue oldValue = oldNewSMGValue.getKey();
      Value newAddressValue = SymbolicValueFactory.getInstance().newIdentifier(null);
      CType type = newSPC.valueToTypeMap.get(oldValue);
      Preconditions.checkNotNull(type);
      newSPC =
          newSPC.copyAndPutValue(
              newAddressValue, newSMGValue, smg.getNestingLevel(pointerToNewObj), type);
    }
    return newSPC;
  }

  private boolean containsValueInMapping(Value value) {
    return valueMapping.containsKey(valueWrapper.wrap(value));
  }

  private boolean containsValueInMapping(SMGValue value) {
    return valueMapping.inverse().containsKey(value);
  }

  public SymbolicProgramConfiguration replaceValueWithAndCopy(Value oldValue, Value newValue)
      throws SMGException {
    // This has to find all occurrences of oldValue, even nested in other expressions, and replace
    // them
    if (oldValue.equals(newValue)) {
      return this;
    }

    boolean found = false;
    Map<Value, SMGValue> valuesToUpdate = new HashMap<>();
    for (Entry<Equivalence.Wrapper<Value>, SMGValue> mapping : valueMapping.entrySet()) {
      Value mappedValue = mapping.getKey().get();
      if (mappedValue.equals(oldValue)) {
        valuesToUpdate.putIfAbsent(mappedValue, mapping.getValue());
        found = true;
        continue;
      } else if (mappedValue instanceof ConstantSymbolicExpression constSym
          && constSym.getValue() instanceof SymbolicIdentifier symIdent
          && symIdent.equals(oldValue)) {
        valuesToUpdate.putIfAbsent(mappedValue, mapping.getValue());
        found = true;
        continue;
      }
      Set<SymbolicIdentifier> identsInValue = getSymbolicIdentifiersForValue(mappedValue);
      if (oldValue instanceof SymbolicIdentifier oldSymIden && identsInValue.contains(oldSymIden)) {
        valuesToUpdate.putIfAbsent(mappedValue, mapping.getValue());
      }
    }

    // TODO:
    // Values in valuesToUpdate have to be updated in the following way:
    // 1. Get the value containing a sym value that is to be assigned
    // 2. Replace the sym value with the concrete value
    // 3. Evaluate the expression, as it might now change
    // 4. replace the old value of 1. with the new value of 3.

    if (valuesToUpdate.size() != 1 || !found) {
      // TODO: implement
      throw new SMGException(
          "Error trying to assign more than one symbolic value with a concrete value.");
    }

    SymbolicProgramConfiguration newSPC = this;
    for (Entry<Value, SMGValue> valueMappingToUpdate : valuesToUpdate.entrySet()) {
      Value correctOldValue = valueMappingToUpdate.getKey();
      if (!newSPC.valueMapping.containsKey(valueWrapper.wrap(newValue))) {
        // No mapping for the new value means we can just replace the mapping as it is not yet in
        // the SMG. We just have to remove the old mapping first.
        if (!newSPC.valueMapping.containsKey(valueWrapper.wrap(correctOldValue))) {
          // Not mapping is known at all
          SMGValue newSMGValue = SMGValue.of();

          return newSPC.copyAndPutValue(newValue, newSMGValue, 0, getTypeForValue(correctOldValue));
        } else {
          SMGValue oldSMGValue = valueMappingToUpdate.getValue();
          ImmutableBiMap.Builder<Equivalence.Wrapper<Value>, SMGValue> newValueMapping =
              ImmutableBiMap.builder();

          for (Entry<Wrapper<Value>, SMGValue> mappedValue : newSPC.valueMapping.entrySet()) {
            if (mappedValue.getValue() != oldSMGValue) {
              newValueMapping.put(mappedValue);
            }
          }
          newSPC = newSPC.withNewValueMappings(newValueMapping.buildOrThrow());

          return newSPC.copyAndPutValue(newValue, oldSMGValue, 0, getTypeForValue(correctOldValue));
        }
      }
      SMGValue newSMGValue = newSPC.getSMGValueFromValue(newValue).orElseThrow();
      SMGValue oldSMGValue = newSPC.getSMGValueFromValue(correctOldValue).orElseThrow();
      Preconditions.checkArgument(
          newSPC.smg.getNestingLevel(newSMGValue) == newSPC.smg.getNestingLevel(oldSMGValue));
      // Actually go into the SMG and replace the SMGValues for oldValue
      newSPC =
          newSPC.copyWithNewSMG(
              newSPC.smg.replaceValueWithAndCopy(correctOldValue, oldSMGValue, newSMGValue));
    }
    ImmutableBiMap.Builder<Wrapper<Value>, SMGValue> newValueMapping = ImmutableBiMap.builder();
    Collection<SMGValue> valueMappingsToRemove = valuesToUpdate.values();
    for (Entry<Wrapper<Value>, SMGValue> mappedValue : valueMapping.entrySet()) {
      if (!valueMappingsToRemove.contains(mappedValue.getValue())) {
        newValueMapping.put(mappedValue);
      }
    }
    newSPC = newSPC.withNewValueMappings(newValueMapping.buildOrThrow());
    assert newSPC.checkValueMappingConsistency();
    return newSPC;
  }

  /**
   * Returns all {@link ConstantSymbolicExpression}s with {@link SymbolicIdentifier}s inside located
   * in the given value. Preserves type info in the const expr.
   */
  protected Map<SymbolicIdentifier, CType> getSymbolicIdentifiersWithTypesForValue(Value value) {
    ConstantSymbolicExpressionLocator symIdentVisitor =
        ConstantSymbolicExpressionLocator.getInstance();
    ImmutableMap.Builder<SymbolicIdentifier, CType> identsBuilder = ImmutableMap.builder();
    // Get all symbolic values in sizes (they might not have a SMGValue mapping anymore below!)
    if (value instanceof SymbolicValue symValue) {
      for (ConstantSymbolicExpression constSym : symValue.accept(symIdentVisitor)) {
        if (constSym.getValue() instanceof SymbolicIdentifier symIdent) {
          identsBuilder.put(symIdent, (CType) constSym.getType());
        }
      }
    }
    return identsBuilder.buildOrThrow();
  }

  protected Set<SymbolicIdentifier> getSymbolicIdentifiersForValue(Value value) {
    return getSymbolicIdentifiersWithTypesForValue(value).keySet();
  }

  private boolean checkValueMappingConsistency() {
    Set<SMGValue> existingSMGValuesInMapping = new HashSet<>();
    Set<Value> existingValuesInMapping = new HashSet<>();
    for (Entry<Wrapper<Value>, SMGValue> mappedValue : valueMapping.entrySet()) {
      // Double entry check
      if (existingSMGValuesInMapping.contains(mappedValue.getValue())) {
        return false;
      } else if (existingValuesInMapping.contains(mappedValue.getKey().get())) {
        return false;
      }
      existingValuesInMapping.add(mappedValue.getKey().get());
      existingSMGValuesInMapping.add(mappedValue.getValue());
    }
    return smg.checkValueMappingConsistency(valueMapping, valueWrapper);
  }

  /**
   * Copies the {@link SymbolicProgramConfiguration} and puts the mapping for the cValue to the
   * smgValue (and vice versa) into the returned copy. Also adds the value to the SMG if not
   * present, updates the nesting level if it does exist.
   *
   * @param value {@link Value} that is mapped to the entered smgValue.
   * @param smgValue {@link SMGValue} that is mapped to the entered cValue.
   * @param nestingLevel nesting level for the {@link SMGValue}.
   * @param valueType CType of the value.
   * @return A copy of this SPC with the value mapping added.
   */
  public SymbolicProgramConfiguration copyAndPutValue(
      Value value, SMGValue smgValue, int nestingLevel, CType valueType) {
    ImmutableBiMap.Builder<Equivalence.Wrapper<Value>, SMGValue> builder = ImmutableBiMap.builder();
    if (valueMapping.containsKey(valueWrapper.wrap(value))) {
      return of(
          smg.copyAndAddValue(smgValue, nestingLevel),
          globalVariableMapping,
          atExitStack,
          stackVariableMapping,
          heapObjects,
          externalObjectAllocation,
          valueMapping,
          variableToTypeMap,
          memoryAddressAssumptionsMap,
          mallocZeroMemory,
          readBlacklist,
          valueToTypeMap);
    }
    return of(
        smg.copyAndAddValue(smgValue, nestingLevel),
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        builder.putAll(valueMapping).put(valueWrapper.wrap(value), smgValue).buildOrThrow(),
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory,
        readBlacklist,
        valueToTypeMap.putAndCopy(smgValue, valueType));
  }

  /**
   * Copies the {@link SymbolicProgramConfiguration} and sets the externally allocated {@link
   * SMGObject} validity to false. If the entered {@link SMGObject} is not yet in the external
   * allocation map this will enter it!
   *
   * @param pObject the {@link SMGObject} that is externally allocated to be set to invalid.
   * @return A copy of this SPC with the validity of the external object changed.
   */
  public SymbolicProgramConfiguration copyAndInvalidateExternalAllocation(SMGObject pObject) {
    return of(
        smg,
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation.putAndCopy(pObject, false),
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory,
        readBlacklist,
        valueToTypeMap);
  }

  /**
   * Changes the validity of a external object to valid.
   *
   * @param pObject the {@link SMGObject} that is externally allocated to be set to valid.
   * @return A copy of this SPC with the validity of the external object changed.
   */
  public SymbolicProgramConfiguration copyAndValidateExternalAllocation(SMGObject pObject) {
    return of(
        smg,
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation.putAndCopy(pObject, true),
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory,
        readBlacklist,
        valueToTypeMap);
  }

  /**
   * Returns an {@link Optional} that may be empty if no {@link SMGValue} for the entered {@link
   * Value} exists in the value mapping. If a mapping exists, it returns the {@link SMGValue} for
   * the entered {@link Value} inside the Optional.
   *
   * @param cValue The {@link Value} you want the potential {@link SMGValue} for.
   * @return {@link Optional} that contains the {@link SMGValue} for the entered {@link Value} if it
   *     exists, empty else.
   */
  public Optional<SMGValue> getSMGValueFromValue(Value cValue) {
    SMGValue value = valueMapping.get(valueWrapper.wrap(cValue));
    if (value == null) {
      return Optional.empty();
    }
    return Optional.of(value);
  }

  /**
   * Returns an {@link Optional} that may be empty if no {@link Value} for the entered {@link
   * SMGValue} exists in the value mapping. If a mapping exists, it returns the {@link Value} for
   * the entered {@link SMGValue} inside the Optional. Reverse method of getSMGValue();
   *
   * @param smgValue The {@link SMGValue} you want the potential {@link Value} for.
   * @return {@link Optional} that contains the {@link Value} for the entered {@link SMGValue} if it
   *     exists, empty else.
   */
  public Optional<Value> getValueFromSMGValue(SMGValue smgValue) {
    Wrapper<Value> got = valueMapping.inverse().get(smgValue);
    if (got == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(got.get());
  }

  /**
   * Copies this {@link SymbolicProgramConfiguration} and creates a mapping of a {@link Value} to a
   * newly created {@link SMGValue}. This checks if there is a mapping already, and if there exists
   * a mapping the unchanged SPC will be returned.
   *
   * @param cValue The {@link Value} you want to create a new, symbolic {@link SMGValue} for and map
   *     them to each other.
   * @return The new SPC with the new {@link SMGValue} and the value mapping from the entered {@link
   *     Value} to the new {@link SMGValue}.
   */
  public SymbolicProgramConfiguration copyAndCreateValue(Value cValue, CType type) {
    return copyAndCreateValue(cValue, 0, type);
  }

  /**
   * Copies this {@link SymbolicProgramConfiguration} and creates a mapping of a {@link Value} to a
   * newly created {@link SMGValue}. This checks if there is a mapping already, and if there exists
   * a mapping the unchanged SPC will be returned.
   *
   * @param cValue The {@link Value} you want to create a new, symbolic {@link SMGValue} for and map
   *     them to each other.
   * @param nestingLevel Nesting level of the new value.
   * @param type CType of the value.
   * @return The new SPC with the new {@link SMGValue} and the value mapping from the entered {@link
   *     Value} to the new {@link SMGValue}.
   */
  public SymbolicProgramConfiguration copyAndCreateValue(
      Value cValue, int nestingLevel, CType type) {
    SMGValue newSMGValue = SMGValue.of();
    return copyAndPutValue(cValue, newSMGValue, nestingLevel, type);
  }

  /**
   * Copies this {@link SymbolicProgramConfiguration} and adds the {@link SMGObject} pObject to it
   * before returning the new SPC.
   *
   * @param pObject The {@link SMGObject} you want to add to the SPC.
   * @return The new SPC with the {@link SMGObject} added.
   */
  public SymbolicProgramConfiguration copyAndAddExternalObject(SMGObject pObject) {
    return of(
        smg,
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation.putAndCopy(pObject, true),
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory,
        readBlacklist,
        valueToTypeMap);
  }

  /**
   * Copies this {@link SymbolicProgramConfiguration} and replaces the SMG with a new one. Meant for
   * read/write operations. The SMG has to be a successor of the old one.
   *
   * @param pSMG the new {@link SMG} that replaces the old one but is a successor of the old one.
   * @return The new SPC with the new {@link SMG}.
   */
  private SymbolicProgramConfiguration copyAndReplaceSMG(SMG pSMG) {
    return of(
        pSMG,
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory,
        readBlacklist,
        valueToTypeMap);
  }

  /**
   * Replaces the memory to numeric address assumption map with a new map with the entered object
   * added.
   *
   * @param pNewObject a new memory region.
   * @return a new SPC with a new memoryAddressAssumptionsMap that has the entered memory region
   *     added.
   */
  private SymbolicProgramConfiguration copyAndReplaceNumericMemoryAssumption(SMGObject pNewObject) {
    return of(
        smg,
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        calculateNewNumericAddressMapForNewSMGObject(pNewObject),
        mallocZeroMemory,
        readBlacklist,
        valueToTypeMap);
  }

  /**
   * Removes the memory to numeric address assumption in the map and returns a fresh copy.
   *
   * @param pNewObject memory region to delete.
   * @return a new SPC with a new memoryAddressAssumptionsMap that has the entered region deleted.
   */
  private SymbolicProgramConfiguration copyAndRemoveNumericAddressAssumption(SMGObject pNewObject) {
    return of(
        smg,
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap.removeAndCopy(pNewObject),
        mallocZeroMemory,
        readBlacklist,
        valueToTypeMap);
  }

  /**
   * Adds a SMGObject to the list of known SMGObjects, but nothing else.
   *
   * @param newObject the new {@link SMGObject}.
   * @return a copy of the SPC + the object added.
   */
  public SymbolicProgramConfiguration copyAndAddStackObject(SMGObject newObject) {
    return copyAndReplaceSMG(getSmg().copyAndAddObject(newObject))
        .copyAndReplaceNumericMemoryAssumption(newObject);
  }

  /**
   * Tries to search for a variable that is currently visible in the current {@link StackFrame} and
   * in the global variables and returns the variable if found. If it is not found, the {@link
   * Optional} will be empty. Note: this returns the SMGObject in which the value for the variable
   * is written. Read with the correct type! This will peek the previous stack frame if the current
   * stack frame is not fully initialized.
   *
   * @param pName Name of the variable you want to search for as a {@link String}.
   * @return {@link Optional} that contains the variable if found, but is empty if not found.
   */
  public Optional<SMGObject> getObjectForVisibleVariable(String pName) {
    return getObjectForVisibleVariable(pName, true);
  }

  /**
   * Tries to search for a variable that is currently visible in the current {@link StackFrame} and
   * in the global variables and returns the variable if found. If it is not found, the {@link
   * Optional} will be empty. Note: this returns the SMGObject in which the value for the variable
   * is written. Read with the correct type!
   *
   * @param pName Name of the variable you want to search for as a {@link String}.
   * @param peekPreviousFrame peeks the previous frame if the current one is not fully initialized
   *     and this parameter is true.
   * @return {@link Optional} that contains the variable if found, but is empty if not found.
   */
  public Optional<SMGObject> getObjectForVisibleVariable(String pName, boolean peekPreviousFrame) {
    // globals
    if (globalVariableMapping.containsKey(pName)) {
      return Optional.of(globalVariableMapping.get(pName));
    }

    // Needed because of CEGAR
    if (stackVariableMapping.isEmpty()) {
      return Optional.empty();
    }
    // Only look in the current stack frame for now
    StackFrame currentFrame = stackVariableMapping.peek();
    if (pName.contains("::")) {
      String variableFunctionName = pName.substring(0, pName.indexOf(':'));
      if (!currentFrame
          .getFunctionDefinition()
          .getQualifiedName()
          .contentEquals(variableFunctionName)) {
        // Check 1 frame above, sometimes CPAchecker forces us to look there
        if (stackVariableMapping.size() > 1) {
          currentFrame = stackVariableMapping.popAndCopy().peek();
          Preconditions.checkArgument(
              currentFrame
                  .getFunctionDefinition()
                  .getQualifiedName()
                  .contentEquals(variableFunctionName));
        }
      }
    }

    if (!peekPreviousFrame) {
      if (currentFrame.containsVariable(pName)) {
        return Optional.of(currentFrame.getVariable(pName));
      } else {
        // no variable found
        return Optional.empty();
      }
    }

    int sizeOfVariables = currentFrame.getVariables().size();
    if (currentFrame.hasVariableArguments()) {
      sizeOfVariables = sizeOfVariables + currentFrame.getVariableArguments().size();
    }
    if (sizeOfVariables < currentFrame.getFunctionDefinition().getParameters().size()) {
      // We are currently creating a function and may ask for a value from the old function for
      // array size
      if (stackVariableMapping.size() > 1) {
        StackFrame prevFrame = stackVariableMapping.popAndCopy().peek();
        if (prevFrame.containsVariable(pName)) {
          // This may only happen for creation of new stack frames!!!!!!
          return Optional.of(prevFrame.getVariable(pName));
        }
      }
    }

    if (currentFrame.containsVariable(pName)) {
      return Optional.of(currentFrame.getVariable(pName));
    } else {
      // no variable found
      return Optional.empty();
    }
  }

  /* Only used to reconstruct the state after interpolation! */
  Optional<SMGObject> getObjectForVariable(String pName) {
    // globals
    if (globalVariableMapping.containsKey(pName)) {
      return Optional.of(globalVariableMapping.get(pName));
    }

    // all locals
    for (StackFrame frame : stackVariableMapping) {
      if (frame.containsVariable(pName)) {
        return Optional.of(frame.getVariable(pName));
      }
    }

    // no variable found
    return Optional.empty();
  }

  /**
   * Tries to search for a variable that is currently visible in the {@link StackFrame} above the
   * current one and in the global variables and returns the variable if found. If it is not found,
   * the {@link Optional} will be empty. Note: this returns the SMGObject in which the value for the
   * variable is written. Read with the correct type!
   *
   * @param pName Name of the variable you want to search for as a {@link String}.
   * @return {@link Optional} that contains the variable if found, but is empty if not found.
   */
  public Optional<SMGObject> getObjectForVisibleVariableFromPreviousStackframe(String pName) {
    // Only look in the stack frame below the current
    StackFrame lowerFrame = stackVariableMapping.popAndCopy().peek();
    if (lowerFrame.containsVariable(pName)) {
      return Optional.of(lowerFrame.getVariable(pName));
    }

    // Second check global
    if (globalVariableMapping.containsKey(pName)) {
      return Optional.of(globalVariableMapping.get(pName));
    }
    // no variable found
    return Optional.empty();
  }

  /**
   * Returns <code>true</code> if the entered {@link SMGObject} is externally allocated. This does
   * not check the validity of the external object.
   *
   * @param pObject The {@link SMGObject} you want to know if its externally allocated or not.
   * @return <code>true</code> if the entered pObject is externally allocated, <code>false</code> if
   *     not.
   */
  public boolean isObjectExternallyAllocated(SMGObject pObject) {
    return externalObjectAllocation.containsKey(pObject);
  }

  /**
   * Checks if a smg object is valid in the current context.
   *
   * @param pObject - the object to be checked
   * @return smg.isValid(pObject)
   */
  public boolean isObjectValid(SMGObject pObject) {
    return smg.isValid(pObject);
  }

  /**
   * The exact read method as specified by the original SMG paper! My guess is that a lot of other
   * forms of read (more exact reads) are not possible once we use join/abstraction.
   *
   * @param pObject the {@link SMGObject} read.
   * @param pFieldOffset {@link BigInteger} offset.
   * @param pSizeofInBits {@link BigInteger} sizeInBits.
   * @param preciseRead true for reads that cut SMGHasValueEdges down from existing concrete values.
   *     False for default SMG behavior of creating new, smaller/larger symbolic values.
   * @return {@link SMGHasValueEdgesAndSPC} tuple for the copy of the SPC with the value read and
   *     the {@link SMGValue} read from it.
   */
  public SMGHasValueEdgesAndSPC readValue(
      SMGObject pObject, BigInteger pFieldOffset, BigInteger pSizeofInBits, boolean preciseRead)
      throws SMGException {
    checkReadBlackList(pObject);
    SMGAndHasValueEdges newSMGAndValue =
        smg.readValue(pObject, pFieldOffset, pSizeofInBits, preciseRead);
    return SMGHasValueEdgesAndSPC.of(
        newSMGAndValue.getHvEdges(), copyAndReplaceSMG(newSMGAndValue.getSMG()));
  }

  public void checkReadBlackList(SMGObject pObject) throws SMGException {
    if (readBlacklist.contains(pObject)) {
      throw new SMGException("Complex entry function arguments can not be handled at the moment.");
    }
  }

  /**
   * Copy SPC and add a pointer to an object at a specified offset. If the mapping, Value <->
   * SMGValue, does not exist it is created, else the old SMGValue is used. If there was a pointer
   * from this SMGValue to an SMGObject it is replaced with the one given.
   *
   * @param address the {@link Value} representing the address to the {@link SMGObject} at the
   *     specified offset.
   * @param target the {@link SMGObject} the {@link Value} points to.
   * @param type CType of the pointer.
   * @param offsetInBits the offset in the {@link SMGObject} in bits as {@link BigInteger}.
   * @param nestingLevel the nesting level of the value.
   * @return a copy of the SPC with the pointer to the {@link SMGObject} and the specified offset
   *     added.
   */
  public SymbolicProgramConfiguration copyAndAddPointerFromAddressToMemory(
      Value address,
      SMGObject target,
      CType type,
      Value offsetInBits,
      int nestingLevel,
      SMGTargetSpecifier pSMGTargetSpecifier) {
    // If there is no SMGValue for this Value (address) we create it, else we use the existing
    SymbolicProgramConfiguration spc = copyAndCreateValue(address, nestingLevel, type);
    SMGValue smgAddress = spc.getSMGValueFromValue(address).orElseThrow();
    spc = spc.updateNestingLevel(smgAddress, nestingLevel);
    // Now we create a points-to-edge from this value to the target object at the
    // specified offset, overriding any existing from this value
    assert !(target instanceof SMGSinglyLinkedListSegment)
        || !pSMGTargetSpecifier.equals(SMGTargetSpecifier.IS_REGION);
    assert target instanceof SMGSinglyLinkedListSegment
        || pSMGTargetSpecifier.equals(SMGTargetSpecifier.IS_REGION);
    SMGPointsToEdge pointsToEdge = new SMGPointsToEdge(target, offsetInBits, pSMGTargetSpecifier);
    return spc.copyAndReplaceSMG(
        spc.getSmg()
            .copyAndAddValue(smgAddress, nestingLevel)
            .copyAndAddPTEdge(pointsToEdge, smgAddress));
  }

  /*
   * Same as copyAndAddPointerFromAddressToRegion but with a specific nesting level in the value.
   * Creates a new pointer.
   */
  public SymbolicProgramConfiguration copyAndAddPointerFromAddressToRegionWithNestingLevel(
      Value address, SMGObject target, CType type, BigInteger offsetInBits, int nestingLevel) {
    // If there is no SMGValue for this address we create it, else we use the existing
    SymbolicProgramConfiguration spc = copyAndCreateValue(address, nestingLevel, type);
    // If there is an existing SMGValue for address, no new one is created, but the old one is
    // returned. The nesting level might be wrong, however.
    SMGValue smgAddress = spc.getSMGValueFromValue(address).orElseThrow();
    // There was a mapping, update nesting level
    spc = spc.updateNestingLevel(smgAddress, nestingLevel);
    assert !(target instanceof SMGSinglyLinkedListSegment);
    // Now we create a points-to-edge from this value to the target object at the
    // specified offset, overriding any existing from this value
    SMGPointsToEdge pointsToEdge =
        new SMGPointsToEdge(target, offsetInBits, SMGTargetSpecifier.IS_REGION);
    if (target instanceof SMGSinglyLinkedListSegment) {
      Preconditions.checkArgument(
          ((SMGSinglyLinkedListSegment) target).getMinLength() >= nestingLevel);
    }
    Preconditions.checkArgument(nestingLevel >= 0);
    return spc.copyAndReplaceSMG(spc.getSmg().copyAndAddPTEdge(pointsToEdge, smgAddress));
  }

  public SymbolicProgramConfiguration copyAndAddPointerFromAddressToRegionWithNestingLevel(
      Value address,
      SMGObject target,
      CType type,
      BigInteger offsetInBits,
      int nestingLevel,
      SMGTargetSpecifier specifier) {
    assert !(target instanceof SMGSinglyLinkedListSegment)
        || !specifier.equals(SMGTargetSpecifier.IS_REGION);
    assert target instanceof SMGSinglyLinkedListSegment
        || specifier.equals(SMGTargetSpecifier.IS_REGION);
    // If there is no SMGValue for this address we create it, else we use the existing
    SymbolicProgramConfiguration spc = copyAndCreateValue(address, nestingLevel, type);
    // If there is an existing SMGValue for address, no new one is created, but the old one is
    // returned. The nesting level might be wrong, however.
    SMGValue smgAddress = spc.getSMGValueFromValue(address).orElseThrow();
    // There was a mapping, update nesting level
    spc = spc.updateNestingLevel(smgAddress, nestingLevel);
    // Now we create a points-to-edge from this value to the target object at the
    // specified offset, overriding any existing from this value
    SMGPointsToEdge pointsToEdge = new SMGPointsToEdge(target, offsetInBits, specifier);
    Preconditions.checkArgument(nestingLevel >= 0);
    return spc.copyAndReplaceSMG(spc.getSmg().copyAndAddPTEdge(pointsToEdge, smgAddress));
  }

  private SymbolicProgramConfiguration updateNestingLevel(SMGValue value, int nestingLevel) {
    return copyAndReplaceSMG(smg.copyAndAddValue(value, nestingLevel));
  }

  /**
   * Checks if a {@link SMGPointsToEdge} exists for the entered target object and offset and returns
   * a {@link Optional} that is filled with the SMGValue leading to the points-to-edge, empty if
   * there is none. (This always assumes SMGTargetSpecifier.IS_REGION)
   *
   * @param target {@link SMGObject} that is the target of the points-to-edge.
   * @param offset {@link Value} offset in bits in the target.
   * @return either an empty {@link Optional} if there is no such edge, but the {@link SMGValue}
   *     within if there is such a points-to-edge.
   */
  public Optional<SMGValue> getAddressValueForPointsToTarget(SMGObject target, Value offset) {
    assert !target.isSLL();
    Map<SMGValue, SMGPointsToEdge> pteMapping = getSmg().getPTEdgeMapping();
    SMGPointsToEdge searchedForEdge =
        new SMGPointsToEdge(target, offset, SMGTargetSpecifier.IS_REGION);

    for (Entry<SMGValue, SMGPointsToEdge> entry : pteMapping.entrySet()) {
      if (entry.getValue().equals(searchedForEdge)) {
        return Optional.of(entry.getKey());
      }
    }
    return Optional.empty();
  }

  public Optional<SMGValue> getAddressValueForPointsToTarget(
      SMGObject target, Value offset, SMGTargetSpecifier specifier) {
    Map<SMGValue, SMGPointsToEdge> pteMapping = getSmg().getPTEdgeMapping();
    SMGPointsToEdge searchedForEdge = new SMGPointsToEdge(target, offset, specifier);

    for (Entry<SMGValue, SMGPointsToEdge> entry : pteMapping.entrySet()) {
      if (entry.getValue().equals(searchedForEdge)) {
        return Optional.of(entry.getKey());
      }
    }
    return Optional.empty();
  }

  /**
   * Checks if a {@link SMGPointsToEdge} exists for the entered target object and offset and returns
   * a {@link Optional} that is filled with the SMGValue leading to the points-to-edge, empty if
   * there is none. (This always assumes SMGTargetSpecifier.IS_REGION)
   *
   * @param target {@link SMGObject} that is the target of the points-to-edge.
   * @param offset {@link BigInteger} offset in bits in the target.
   * @param pointerLevel nesting level of the pointer we search for
   * @return either an empty {@link Optional} if there is no such edge, but the {@link SMGValue}
   *     within if there is such a points-to-edge.
   */
  public Optional<SMGValue> getAddressValueForPointsToTarget(
      SMGObject target, Value offset, int pointerLevel) {
    Map<SMGValue, SMGPointsToEdge> pteMapping = getSmg().getPTEdgeMapping();
    SMGPointsToEdge searchedForEdge =
        new SMGPointsToEdge(target, offset, SMGTargetSpecifier.IS_REGION);

    for (Entry<SMGValue, SMGPointsToEdge> entry : pteMapping.entrySet()) {
      if (entry.getValue().equals(searchedForEdge)
          && smg.getNestingLevel(entry.getKey()) == pointerLevel) {
        return Optional.of(entry.getKey());
      }
    }
    return Optional.empty();
  }

  /**
   * Checks if a {@link SMGPointsToEdge} exists for the entered target object and offset and nesting
   * level and returns a {@link Optional} that is filled with the SMGValue leading to the
   * points-to-edge, empty if there is none. (This always assumes SMGTargetSpecifier.IS_REGION)
   *
   * @param target {@link SMGObject} that is the target of the points-to-edge.
   * @param offset {@link BigInteger} offset in bits in the target.
   * @param nestingLevel nesting level to search for.
   * @return either an empty {@link Optional} if there is no such edge, but the {@link SMGValue}
   *     within if there is such a points-to-edge.
   */
  public Optional<SMGValue> getAddressValueForPointsToTargetWithNestingLevel(
      SMGObject target, BigInteger offset, int nestingLevel) {
    return smg.getAddressValueForPointsToTargetWithNestingLevel(target, offset, nestingLevel);
  }

  /**
   * Checks if a {@link SMGPointsToEdge} exists for the entered target object and offset and nesting
   * level and returns a {@link Optional} that is filled with the SMGValue leading to the
   * points-to-edge, empty if there is none. (This always assumes SMGTargetSpecifier.IS_REGION)
   *
   * @param target {@link SMGObject} that is the target of the points-to-edge.
   * @param offset {@link BigInteger} offset in bits in the target.
   * @param nestingLevel nesting level to search for.
   * @param specifier {@link SMGTargetSpecifier} that the searched for ptr needs to have.
   * @return either an empty {@link Optional} if there is no such edge, but the {@link SMGValue}
   *     within if there is such a points-to-edge.
   */
  public Optional<SMGValue> getAddressValueForPointsToTargetWithNestingLevel(
      SMGObject target,
      BigInteger offset,
      int nestingLevel,
      SMGTargetSpecifier specifier,
      Set<SMGTargetSpecifier> specifierAllowedToOverride) {
    return smg.getAddressValueForPointsToTargetWithNestingLevel(
        target, offset, nestingLevel, specifier, specifierAllowedToOverride);
  }

  /* This expects the Value to be a valid pointer! */
  SMGTargetSpecifier getPointerSpecifier(Value pointer) {
    SMGValue smgValueAddress = valueMapping.get(valueWrapper.wrap(pointer));
    SMGPointsToEdge ptEdge = smg.getPTEdge(smgValueAddress).orElseThrow();
    Preconditions.checkNotNull(ptEdge);
    return ptEdge.targetSpecifier();
  }

  /**
   * Returns true if the value entered is a pointer in the current SPC. This checks for the
   * existence of a known mapping from Value to SMGValue to a SMGPointsToEdge.
   *
   * @param maybePointer {@link Value} that you want to check.
   * @return true is the entered {@link Value} is an address that points to a memory location. False
   *     else.
   */
  public boolean isPointer(Value maybePointer) {
    if (valueMapping.containsKey(valueWrapper.wrap(maybePointer))) {
      return smg.isPointer(valueMapping.get(valueWrapper.wrap(maybePointer)));
    }
    return false;
  }

  /**
   * Write value into the SMG at the specified offset in bits with the size given in bits. This
   * assumes that the SMGValue is already correctly mapped, but will insert it into the SMG if it is
   * not. The nesting level of the value will be defaulted to 0 if no mapping exists.
   */
  public SymbolicProgramConfiguration writeValue(
      SMGObject pObject, BigInteger pFieldOffset, BigInteger pSizeofInBits, SMGValue pValue) {
    return copyAndReplaceSMG(smg.writeValue(pObject, pFieldOffset, pSizeofInBits, pValue));
  }

  /**
   * This assumes that the entered {@link SMGObject} is part of the SPC!
   *
   * @param pObject the {@link SMGObject} to invalidate.
   * @return a new SPC with the entered object invalidated.
   */
  public SymbolicProgramConfiguration invalidateSMGObject(
      SMGObject pObject, boolean deleteDanglingPointers) {
    Preconditions.checkArgument(smg.getObjects().contains(pObject));
    SymbolicProgramConfiguration newSPC = this;
    if (isObjectExternallyAllocated(pObject)) {
      newSPC = copyAndInvalidateExternalAllocation(pObject);
    }
    SMG newSMG = newSPC.getSmg().copyAndInvalidateObject(pObject, deleteDanglingPointers);
    assert newSMG.checkSMGSanity();
    return newSPC.copyAndReplaceSMG(newSMG).copyAndRemoveNumericAddressAssumption(pObject);
  }

  /**
   * This assumes that the entered {@link SMGObject} is part of the SPC! Only to be used for free of
   * malloc(0) memory.
   *
   * @param pObject the {@link SMGObject} to validate.
   * @return a new SPC with the entered object validated.
   */
  public SymbolicProgramConfiguration validateSMGObject(SMGObject pObject) {
    Preconditions.checkArgument(smg.getObjects().contains(pObject));
    SymbolicProgramConfiguration newSPC = this;
    if (isObjectExternallyAllocated(pObject)) {
      newSPC = copyAndValidateExternalAllocation(pObject);
    }
    SMG newSMG = newSPC.getSmg().copyAndValidateObject(pObject);
    return newSPC.copyAndReplaceSMG(newSMG);
  }

  /**
   * Returns local and global variables as {@link MemoryLocation}s and their respective Values and
   * type sizes (SMGs allow reads from different types, just the size has to match). This does not
   * return any heap related memory. This does return pointers, but not the structure behind the
   * pointers. Nor the return value in a stack frame.
   *
   * @return a mapping of global/local program variables to their values and sizes.
   */
  public PersistentMap<MemoryLocation, ValueAndValueSize>
      getMemoryLocationsAndValuesForSPCWithoutHeap() {

    PersistentMap<MemoryLocation, ValueAndValueSize> map = PathCopyingPersistentTreeMap.of();

    for (Entry<String, SMGObject> globalEntry : globalVariableMapping.entrySet()) {
      String qualifiedName = globalEntry.getKey();
      SMGObject memory = globalEntry.getValue();
      Preconditions.checkArgument(smg.isValid(memory));
      for (SMGHasValueEdge valueEdge : smg.getEdges(memory)) {
        MemoryLocation memLoc =
            MemoryLocation.fromQualifiedName(qualifiedName, valueEdge.getOffset().longValueExact());
        SMGValue smgValue = valueEdge.hasValue();
        BigInteger typeSize = valueEdge.getSizeInBits();
        Preconditions.checkArgument(valueMapping.containsValue(smgValue));
        Value value = valueMapping.inverse().get(smgValue).get();
        Preconditions.checkNotNull(value);
        ValueAndValueSize valueAndValueSize = ValueAndValueSize.of(value, typeSize);
        map = map.putAndCopy(memLoc, valueAndValueSize);
      }
    }

    for (StackFrame stackframe : stackVariableMapping) {
      if (stackframe.getReturnObject().isPresent()) {
        String funName = stackframe.getFunctionDefinition().getQualifiedName();
        // There is a return object!
        for (SMGHasValueEdge valueEdge : smg.getEdges(stackframe.getReturnObject().orElseThrow())) {
          MemoryLocation memLoc =
              MemoryLocation.fromQualifiedName(
                  funName + "::__retval__", valueEdge.getOffset().longValueExact());
          SMGValue smgValue = valueEdge.hasValue();
          BigInteger typeSize = valueEdge.getSizeInBits();
          Preconditions.checkArgument(valueMapping.containsValue(smgValue));
          Value value = valueMapping.inverse().get(smgValue).get();
          Preconditions.checkNotNull(value);
          ValueAndValueSize valueAndValueSize = ValueAndValueSize.of(value, typeSize);
          map = map.putAndCopy(memLoc, valueAndValueSize);
        }
      }
      for (Entry<String, SMGObject> localVariable : stackframe.getVariables().entrySet()) {
        String qualifiedName = localVariable.getKey();
        SMGObject memory = localVariable.getValue();
        if (!smg.isValid(memory)) {
          // Skip non valid memory
          continue;
        }
        for (SMGHasValueEdge valueEdge : smg.getEdges(memory)) {
          MemoryLocation memLoc =
              MemoryLocation.fromQualifiedName(
                  qualifiedName, valueEdge.getOffset().longValueExact());
          SMGValue smgValue = valueEdge.hasValue();
          BigInteger typeSize = valueEdge.getSizeInBits();
          Preconditions.checkArgument(valueMapping.containsValue(smgValue));
          Value value = valueMapping.inverse().get(smgValue).get();
          Preconditions.checkNotNull(value);
          ValueAndValueSize valueAndValueSize = ValueAndValueSize.of(value, typeSize);
          map = map.putAndCopy(memLoc, valueAndValueSize);
        }
      }
    }
    return map;
  }

  public Map<String, Value> getSizeObMemoryForSPCWithoutHeap() {
    Map<String, Value> variableNameToMemorySizeInBits = new HashMap<>();
    for (Entry<String, SMGObject> globalEntry : globalVariableMapping.entrySet()) {
      String qualifiedName = globalEntry.getKey();
      SMGObject memory = globalEntry.getValue();
      variableNameToMemorySizeInBits.put(qualifiedName, memory.getSize());
    }

    for (StackFrame stackframe : stackVariableMapping) {
      for (Entry<String, SMGObject> localVariable : stackframe.getVariables().entrySet()) {
        String qualifiedName = localVariable.getKey();
        SMGObject memory = localVariable.getValue();
        variableNameToMemorySizeInBits.put(qualifiedName, memory.getSize());
      }
    }

    return variableNameToMemorySizeInBits;
  }

  /* We need the stack frames with their definitions as copies to rebuild the state. */
  public PersistentStack<CFunctionDeclarationAndOptionalValue>
      getFunctionDeclarationsFromStackFrames() {
    PersistentStack<CFunctionDeclarationAndOptionalValue> decls = PersistentStack.of();
    for (StackFrame frame : stackVariableMapping) {
      CFunctionDeclaration funcDef = frame.getFunctionDefinition();
      if (funcDef == null) {
        // Test frame
        continue;
      }
      if (frame.getReturnObject().isEmpty()) {
        decls =
            decls.pushAndCopy(CFunctionDeclarationAndOptionalValue.of(funcDef, Optional.empty()));
      } else {
        // Search for the return Value, there might be none if we are not on the return edge
        FluentIterable<SMGHasValueEdge> edges =
            smg.getHasValueEdgesByPredicate(frame.getReturnObject().orElseThrow(), n -> true);
        if (edges.isEmpty()) {
          decls =
              decls.pushAndCopy(
                  CFunctionDeclarationAndOptionalValue.of(
                      frame.getFunctionDefinition(), Optional.empty()));
          continue;
        }
        Preconditions.checkArgument(edges.size() == 1);
        Value returnValue = getValueFromSMGValue(edges.get(0).hasValue()).orElseThrow();
        decls =
            decls.pushAndCopy(
                CFunctionDeclarationAndOptionalValue.of(
                    frame.getFunctionDefinition(), Optional.of(returnValue)));
      }
    }
    return decls;
  }

  public CType getTypeOfVariable(MemoryLocation memLoc) {
    return variableToTypeMap.get(memLoc.getQualifiedName());
  }

  public PersistentMap<String, CType> getVariableTypeMap() {
    return variableToTypeMap;
  }

  public SymbolicProgramConfiguration copyAndSetSpecifierOfPtrsTowards(
      SMGObject target, SMGTargetSpecifier specifierToSet) {
    return new SymbolicProgramConfiguration(
        smg.copyAndSetTargetSpecifierForPtrsTowards(target, specifierToSet),
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory,
        readBlacklist,
        valueToTypeMap);
  }

  public SymbolicProgramConfiguration copyAndSetSpecifierOfPtrsTowards(
      SMGObject target,
      SMGTargetSpecifier specifierToSet,
      Set<SMGTargetSpecifier> specifierToSwitch) {
    return new SymbolicProgramConfiguration(
        smg.copyAndSetTargetSpecifierForPtrsTowards(target, specifierToSet, specifierToSwitch),
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory,
        readBlacklist,
        valueToTypeMap);
  }

  public SymbolicProgramConfiguration copyAndSetTargetSpecifierForPointer(
      SMGValue pPtrValue, SMGTargetSpecifier specifierToSet) {
    return new SymbolicProgramConfiguration(
        smg.copyAndSetTargetSpecifierForPointer(pPtrValue, specifierToSet),
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory,
        readBlacklist,
        valueToTypeMap);
  }

  /*
   * Remove the entered object from the heap and general memory mappings.
   * Also, all has-value-edges are pruned. Nothing else.
   */
  public SymbolicProgramConfiguration copyAndRemoveObjectFromHeap(SMGObject obj) {
    return new SymbolicProgramConfiguration(
        smg.copyAndRemoveObjects(ImmutableSet.of(obj)),
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects.removeAndCopy(obj),
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap.removeAndCopy(obj),
        mallocZeroMemory,
        readBlacklist,
        valueToTypeMap);
  }

  /*
   * Remove the entered object from the heap and general memory mappings.
   * Also, all has-value-edges are pruned. Nothing else.
   */
  public SymbolicProgramConfiguration copyAndRemoveAbstractedObjectFromHeap(SMGObject obj) {
    return new SymbolicProgramConfiguration(
        smg.copyAndRemoveAbstractedObjectFromHeap(obj),
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects.removeAndCopy(obj),
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap.removeAndCopy(obj),
        mallocZeroMemory,
        readBlacklist,
        valueToTypeMap);
  }

  /**
   * Search for all pointers towards the object old and replaces them with pointers pointing towards
   * the new object. If the newTarget is a region, specifiers are set to region. All other
   * specifiers are retained.
   *
   * @param oldObj old object.
   * @param newObject new target object.
   * @return a new SPC with the replacement.
   */
  public SymbolicProgramConfiguration replaceAllPointersTowardsWith(
      SMGObject oldObj, SMGObject newObject) {
    return new SymbolicProgramConfiguration(
        smg.replaceAllPointersTowardsWith(oldObj, newObject),
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory,
        readBlacklist,
        valueToTypeMap);
  }

  /**
   * Search for all pointers towards the object old and replaces them with pointers pointing towards
   * the new object. Sets the specifier to the given one.
   *
   * @param oldObj old object.
   * @param newObject new target object.
   * @return a new SPC with the replacement.
   */
  public SymbolicProgramConfiguration replaceAllPointersTowardsWithAndSetSpecifier(
      SMGObject oldObj, SMGObject newObject, SMGTargetSpecifier newSpecifier) {
    return new SymbolicProgramConfiguration(
        smg.replaceAllPointersTowardsWithAndSetSpecifier(oldObj, newObject, newSpecifier),
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory,
        readBlacklist,
        valueToTypeMap);
  }

  /**
   * Search for all self-pointers from and towards the object given and replaces their specifier
   * with ALL or REGION. pointers at exemptOffsets are ignored.
   *
   * @param object object to switch self-pointers.
   * @param exemptOffsets ignored offsets.
   */
  public SymbolicProgramConfiguration replaceAllSelfPointersWithNewSpecifier(
      SMGObject object, Set<BigInteger> exemptOffsets) {
    return new SymbolicProgramConfiguration(
        smg.replaceAllSelfPointersWithNewSpecifier(object, exemptOffsets),
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory,
        readBlacklist,
        valueToTypeMap);
  }

  /**
   * Search for all pointers towards the object old and replaces them with pointers pointing towards
   * the new object only if their specifier is equal to the given. Then switches the nesting level
   * of the switched pointers to 0.
   *
   * @param oldObj old object.
   * @param newObject new target object.
   * @param specifierToSwitch the specifiers that are allowed to be switched to the new object. All
   *     others remain on old obj.
   * @return a new SMG with the replacement.
   */
  public SymbolicProgramConfiguration replaceSpecificPointersTowardsWith(
      SMGObject oldObj, SMGObject newObject, Set<SMGTargetSpecifier> specifierToSwitch) {
    return new SymbolicProgramConfiguration(
        smg.replaceSpecificPointersTowardsWith(oldObj, newObject, specifierToSwitch),
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory,
        readBlacklist,
        valueToTypeMap);
  }

  public SymbolicProgramConfiguration copyAndReplaceHVEdgesAt(
      SMGObject objectToReplace, PersistentSet<SMGHasValueEdge> newHVEdges) {
    return new SymbolicProgramConfiguration(
        smg.copyAndReplaceHVEdgesAt(objectToReplace, newHVEdges),
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory,
        readBlacklist,
        valueToTypeMap);
  }

  public SymbolicProgramConfiguration replaceValueAtWithAndCopy(
      SMGObject object, BigInteger offsetInBits, BigInteger sizeInBits, SMGHasValueEdge newHVEdge) {
    return new SymbolicProgramConfiguration(
        smg.copyAndReplaceHVEdgeAt(object, offsetInBits, sizeInBits, newHVEdge),
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory,
        readBlacklist,
        valueToTypeMap);
  }

  /**
   * Removes the {@link SMGPointsToEdge} and {@link SMGValue} from the {@link SMG}. Caution when
   * using this method, should only ever be applied to SMGValues that are no longer used! This has
   * side effects and will modify the pointer to object map!
   *
   * @return a new {@link SMG} with the {@link SMGValue} and its {@link SMGPointsToEdge} removed.
   */
  public SymbolicProgramConfiguration removeLastPointerFromSMGAndCopy(SMGValue value) {
    return new SymbolicProgramConfiguration(
        smg.copyAndRemovePointsToEdge(value).copyAndRemoveValue(value),
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory,
        readBlacklist,
        valueToTypeMap);
  }

  /**
   * Removes the {@link SMGPointsToEdge} and {@link SMGValue} from the {@link SMG}. Caution when
   * using this method, should only ever be applied to SMGValues that are no longer used! This has
   * no side effects and will not modify the pointer to object map etc.! This means this is only
   * safe to use if there is already no entry in this map!
   *
   * @return a new {@link SMG} with the {@link SMGValue} and its {@link SMGPointsToEdge} removed.
   */
  public SymbolicProgramConfiguration removePointerFromSMGWithoutSideEffectsAndCopy(
      SMGValue value) {
    return new SymbolicProgramConfiguration(
        smg.copyAndRemovePointsToEdgeWithoutSideEffects(value).copyAndRemoveValue(value),
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory,
        readBlacklist,
        valueToTypeMap);
  }

  /**
   * Checks if there is an association of the memory to malloc(0), thus checking if this allowed to
   * be freed.
   *
   * @param memory the {@link SMGObject} to check if it was the result of malloc(0).
   * @return true if the memory was the result of malloc(0). False else.
   */
  public boolean memoryIsResultOfMallocZero(SMGObject memory) {
    return mallocZeroMemory.containsKey(memory) && mallocZeroMemory.get(memory);
  }

  /**
   * Adds an association of the memory given to malloc(0), thus remembering that this is allowed to
   * be freed.
   *
   * @param memory the {@link SMGObject} that was the result of malloc(0).
   * @return a new SPC with the association added.
   */
  public SymbolicProgramConfiguration setMemoryAsResultOfMallocZero(SMGObject memory) {
    return new SymbolicProgramConfiguration(
        smg,
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory.putAndCopy(memory, true),
        readBlacklist,
        valueToTypeMap);
  }

  /**
   * Removes the association of the memory given to malloc(0). (i.e. it was freed)
   *
   * @param memory the {@link SMGObject} that should no longer be associated to malloc(0).
   * @return a new SPC with the association removed.
   */
  public SymbolicProgramConfiguration removeMemoryAsResultOfMallocZero(SMGObject memory) {
    return new SymbolicProgramConfiguration(
        smg,
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory.removeAndCopy(memory),
        readBlacklist,
        valueToTypeMap);
  }

  public Set<SMGObject> getAllSourcesForPointersPointingTowards(SMGObject target) {
    // TODO: use valuesToRegionsTheyAreSavedIn
    return smg.getAllSourcesForPointersPointingTowards(target);
  }

  public Set<SMGObject> getAllTargetsOfPointersInObject(SMGObject source) {
    return smg.getTargetsForPointersIn(source);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append("Global variables:\n");
    for (Entry<String, SMGObject> globalEntry : globalVariableMapping.entrySet()) {
      // Global vars:
      String qualifiedName = globalEntry.getKey();
      SMGObject memory = globalEntry.getValue();
      String memoryString = " in ";
      if (smg.isValid(memory)) {
        memoryString = memoryString + memory;
      } else {
        memoryString = memoryString + "invalid " + memory;
      }
      for (SMGHasValueEdge valueEdge :
          ImmutableList.sortedCopyOf(
              Comparator.comparing(SMGHasValueEdge::getOffset), smg.getEdges(memory))) {
        SMGValue smgValue = valueEdge.hasValue();
        Preconditions.checkArgument(valueMapping.containsValue(smgValue));
        Value value = valueMapping.inverse().get(smgValue).get();
        Preconditions.checkNotNull(value);
        String pointerInfo = "";
        if (smg.isPointer(smgValue)) {
          pointerInfo = " -> " + smg.getPTEdge(smgValue).orElseThrow();
        }
        builder
            .append(qualifiedName)
            .append(": ")
            .append(value)
            .append("(")
            .append(smgValue)
            .append("[" + valueEdge.getOffset() + "," + valueEdge.getSizeInBits() + ")")
            .append(pointerInfo)
            .append(")")
            .append(memoryString)
            .append("\n");
        builder.append("\n");
      }
    }
    // Local vars:
    builder.append("\n");
    builder.append("Local Variables per StackFrame:");
    builder.append("\n");
    for (StackFrame stackframe : stackVariableMapping) {
      CFunctionDeclaration funDef = stackframe.getFunctionDefinition();
      String funName;
      if (funDef != null) {
        funName = funDef.getQualifiedName();
      } else {
        funName = "DummyFrame";
      }
      if (stackframe.getReturnObject().isPresent()) {
        // There is a return object!
        builder.append("\nFunction ").append(funName).append(" return object :");
        if (smg.isValid(stackframe.getReturnObject().orElseThrow())) {
          builder.append(stackframe.getReturnObject().orElseThrow());
        } else {
          builder.append(" invalid ").append(stackframe.getReturnObject().orElseThrow());
        }
        builder.append(" with values: ");
        for (SMGHasValueEdge valueEdge :
            ImmutableList.sortedCopyOf(
                Comparator.comparing(SMGHasValueEdge::getOffset),
                smg.getEdges(stackframe.getReturnObject().orElseThrow()))) {
          MemoryLocation memLoc =
              MemoryLocation.fromQualifiedName(
                  funName + "::__retval__", valueEdge.getOffset().longValueExact());
          SMGValue smgValue = valueEdge.hasValue();
          Preconditions.checkArgument(valueMapping.containsValue(smgValue));
          Value value = valueMapping.inverse().get(smgValue).get();
          Preconditions.checkNotNull(value);

          String pointerInfo = "";
          if (smg.isPointer(smgValue)) {
            pointerInfo = " -> " + smg.getPTEdge(smgValue);
          }
          builder.append("\n");
          builder
              .append(memLoc.getQualifiedName())
              .append(": ")
              .append(value)
              .append("(")
              .append(smgValue)
              .append("[" + valueEdge.getOffset() + "," + valueEdge.getSizeInBits() + ")")
              .append(pointerInfo)
              .append(")")
              .append("\n");
        }
      } else {
        builder.append("\n");
        builder.append("Function ").append(funName);
        builder.append("\n");
      }
      for (Entry<String, SMGObject> localVariable : stackframe.getVariables().entrySet()) {
        String qualifiedName = localVariable.getKey();
        SMGObject memory = localVariable.getValue();
        String memoryString = " in ";
        if (smg.isValid(memory)) {
          memoryString = memoryString + memory;
        } else {
          memoryString = memoryString + " invalid " + memory;
        }
        Set<SMGHasValueEdge> edges = smg.getEdges(memory);
        if (edges.isEmpty()) {
          builder
              .append("  " + qualifiedName)
              .append(": (empty)")
              .append(memoryString)
              .append("\n");
        }
        for (SMGHasValueEdge valueEdge :
            ImmutableList.sortedCopyOf(Comparator.comparing(SMGHasValueEdge::getOffset), edges)) {
          SMGValue smgValue = valueEdge.hasValue();
          Preconditions.checkArgument(valueMapping.containsValue(smgValue));
          Value value = valueMapping.inverse().get(smgValue).get();
          Preconditions.checkNotNull(value);
          String pointerInfo = "";
          if (smg.isPointer(smgValue)) {
            pointerInfo = " -> " + smg.getPTEdge(smgValue);
          }
          builder
              .append("  " + qualifiedName)
              .append(": ")
              .append(value)
              .append("(")
              .append(smgValue)
              .append("[" + valueEdge.getOffset() + "," + valueEdge.getSizeInBits() + ")")
              .append(pointerInfo)
              .append(")")
              .append(memoryString)
              .append("\n");
        }
      }
      builder.append("\n");
    }
    builder.append("\n");
    builder.append(
        "Pointers -> (spec) [pointer offset] targets[offset, size in bits) with values:");
    builder.append("\n");

    for (Entry<SMGValue, SMGPointsToEdge> entry : smg.getPTEdgeMapping().entrySet()) {
      String validity = "";
      if (!smg.isValid(entry.getValue().pointsTo())) {
        validity = " (invalid object)";
      }
      ImmutableList<SMGHasValueEdge> orderedHVes =
          ImmutableList.sortedCopyOf(
              Comparator.comparing(SMGHasValueEdge::getOffset),
              smg.getHasValueEdgesByPredicate(entry.getValue().pointsTo(), n -> true));

      builder
          .append(entry.getKey())
          .append(" (" + smg.getNestingLevel(entry.getKey()) + ")")
          .append(entry.getValue())
          .append(
              transformedImmutableListCopy(
                  orderedHVes, hve -> (smg.isPointer(hve.hasValue()) ? "(ptr) " : "") + hve))
          .append(validity);
      builder.append("\n");
    }

    builder.append("\n");
    builder.append("Value mappings:");
    builder.append("\n");

    for (Entry<Wrapper<Value>, SMGValue> entry : valueMapping.entrySet()) {
      builder.append(entry.getValue()).append(" <- ").append(entry.getKey().get());
      builder.append("\n");
    }

    return builder.toString();
  }

  /** Returns number of times the value is saved in memory (stack variables, heap etc.) */
  public int getNumberOfValueUsages(Value pValue) {
    Optional<SMGValue> maybeSMGValue = getSMGValueFromValue(pValue);
    if (maybeSMGValue.isEmpty()) {
      return 0;
    }
    return smg.getNumberOfValueUsages(maybeSMGValue.orElseThrow());
  }

  /**
   * Returns the {@link SMGTargetSpecifier} for a given pointer {@link Value}.
   *
   * @param pPointerValue a pointer Value.
   * @return the target specifier.
   */
  public SMGTargetSpecifier getTargetSpecifier(Value pPointerValue) {
    Optional<SMGPointsToEdge> pte =
        smg.getPTEdge(getSMGValueFromValue(pPointerValue).orElseThrow());
    Preconditions.checkArgument(pte.isPresent());
    return pte.orElseThrow().targetSpecifier();
  }

  ImmutableBiMap<Wrapper<Value>, SMGValue> getValueToSMGValueMapping() {
    return valueMapping;
  }

  public SymbolicProgramConfiguration removeUnusedValues() {
    // TODO: this is incomplete and wrong. We also remove values still used in hve offsets/sizes
    //  and object sizes
    PersistentMap<SMGValue, PersistentMap<SMGObject, Integer>> valuesToRegionsTheyAreSavedIn =
        smg.getValuesToRegionsTheyAreSavedIn();
    Set<SMGValue> allValues = smg.getValues().keySet();
    SymbolicProgramConfiguration newSPC = this;
    ImmutableSet.Builder<SMGValue> valueMappingsToRemoveBuilder = ImmutableSet.builder();
    SMG newSMG = smg;
    outer:
    for (SMGValue value : allValues) {
      Optional<Value> maybeMapping = getValueFromSMGValue(value);

      // Don't remove zero ever, and don't remove values that are referenced by the atexit stack
      // Remove everything that is not used and not a numeric value
      //   (they don't do harm and having a mapping is quicker later)
      if (!value.isZero()
          && !valuesToRegionsTheyAreSavedIn.containsKey(value)
          && (maybeMapping.isEmpty() || !maybeMapping.orElseThrow().isNumericValue())) {
        // Check if the value is referenced by the atexit stack
        if (maybeMapping.isPresent()) {
          for (Value atExitAddressValue : atExitStack) {
            Value mappedValue = maybeMapping.orElseThrow();
            if (atExitAddressValue.equals(mappedValue)) {
              continue outer;
            }
          }
        }
        // Remove from PTEs and values
        if (newSMG.isPointer(value)) {
          newSMG = newSPC.getSmg().copyAndRemovePointsToEdge(value);
        }
        newSMG = newSMG.copyAndRemoveValue(value);
        // TODO: this empty saves us from removing values, see todo above!
        valueMappingsToRemoveBuilder.add();
      }
    }
    newSPC = newSPC.copyAndReplaceSMG(newSMG);
    ImmutableBiMap.Builder<Wrapper<Value>, SMGValue> newValueMapping = ImmutableBiMap.builder();
    ImmutableSet<SMGValue> valueMappingsToRemove = valueMappingsToRemoveBuilder.build();
    for (Entry<Wrapper<Value>, SMGValue> mappedValue : valueMapping.entrySet()) {
      if (!valueMappingsToRemove.contains(mappedValue.getValue())) {
        newValueMapping.put(mappedValue);
      }
    }
    return newSPC.withNewValueMappings(newValueMapping.buildOrThrow());
  }

  public SymbolicProgramConfiguration removeValueMappings(Set<SMGValue> valueMappingsToRemove) {
    ImmutableBiMap.Builder<Wrapper<Value>, SMGValue> newValueMapping = ImmutableBiMap.builder();
    for (Entry<Wrapper<Value>, SMGValue> mappedValue : valueMapping.entrySet()) {
      if (!valueMappingsToRemove.contains(mappedValue.getValue())) {
        newValueMapping.put(mappedValue);
      }
    }
    return withNewValueMappings(newValueMapping.buildOrThrow());
  }

  public boolean checkSMGSanity() {
    return smg.checkSMGSanity(stackVariableMapping, globalVariableMapping);
  }

  public boolean isPointingToMallocZero(SMGValue pSMGValue) {
    return !mallocZeroMemory.isEmpty()
        && getSmg().isPointer(pSMGValue)
        && mallocZeroMemory.containsKey(getSmg().getPTEdge(pSMGValue).orElseThrow().pointsTo());
  }
}
