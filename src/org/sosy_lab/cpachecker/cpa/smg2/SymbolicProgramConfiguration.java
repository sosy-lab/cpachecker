// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cpa.smg2.constraint.ConstantSymbolicExpressionLocator;
import org.sosy_lab.cpachecker.cpa.smg2.util.CFunctionDeclarationAndOptionalValue;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGAndSMGObjects;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGHasValueEdgesAndSPC;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectsAndValues;
import org.sosy_lab.cpachecker.cpa.smg2.util.SPCAndSMGObjects;
import org.sosy_lab.cpachecker.cpa.smg2.util.ValueAndValueSize;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueWrapper;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.datastructures.PersistentSet;
import org.sosy_lab.cpachecker.util.smg.datastructures.PersistentStack;
import org.sosy_lab.cpachecker.util.smg.graph.SMGDoublyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGSinglyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;
import org.sosy_lab.cpachecker.util.smg.util.SMGAndHasValueEdges;
import org.sosy_lab.cpachecker.util.smg.util.SMGAndSMGValues;
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
   * unique values. Such that an SMGValue with id 1 is always equal only with an SMGValue with id 1.
   * The only exception are addresses, hence why they are separate) . Important: You NEED to map the
   * SMGValue using the mapping of the SPC!
   */
  private final ImmutableBiMap<Equivalence.Wrapper<Value>, SMGValue> valueMapping;

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
      Set<SMGObject> pReadBlacklist) {
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
      Set<SMGObject> pReadBlacklist) {
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
        pReadBlacklist);
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
            valueWrapper.wrap(new NumericValue(FloatValue.zero(FloatValue.Format.Float32))),
            SMGValue.zeroFloatValue(),
            valueWrapper.wrap(new NumericValue(FloatValue.zero(FloatValue.Format.Float64))),
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
        mallocZeroMemory);
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
        mallocZeroMemory);
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
        mallocZeroMemory);
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
        readBlacklist);
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
        newReadBlacklist);
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

    while (topStack.size() > 0) {
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
        readBlacklist);
  }

  /**
   * Copies this {@link SymbolicProgramConfiguration} and adds a {@link StackFrame} based on the
   * entered model and function definition. More information on StackFrames can be found in the
   * Stackframe class.
   *
   * @param pFunctionDefinition - The {@link CFunctionDeclaration} that the {@link StackFrame} will
   *     be based upon.
   * @param model - The {@link MachineModel} the new {@link StackFrame} be based upon.
   * @param variableArguments null for no variable arguments, else an ImmutableList (that may be
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
          readBlacklist);
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
        readBlacklist);
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
        readBlacklist);
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
      SMG newSmg = smg.copyAndInvalidateObject(objToRemove, true);
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
          readBlacklist);
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
        readBlacklist);
  }

  // Only to be used by materialization to copy an SMGObject
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
    boolean updateNesting =
        target instanceof SMGSinglyLinkedListSegment targetSLL
            && (!(source instanceof SMGSinglyLinkedListSegment sourceSLL)
                || targetSLL.getNestingLevel() != sourceSLL.getNestingLevel());
    Map<SMGObject, SMGObject> topListsAndNestedToUpdate = new HashMap<>();
    SMG newSMG = smg;
    for (SMGHasValueEdge hve : setOfValues) {
      newSMG = newSMG.incrementValueToMemoryMapEntry(target, hve.hasValue());
      if (updateNesting) {
        SMGSinglyLinkedListSegment targetSLL = (SMGSinglyLinkedListSegment) target;
        if (newSMG.isPointer(hve.hasValue())
            && !hve.getOffset().equals(targetSLL.getNextOffset())) {
          if (targetSLL instanceof SMGDoublyLinkedListSegment targetDLL
              && targetDLL.getPrevOffset().equals(hve.getOffset())) {
            continue;
          }
          // Update nesting level of directly nested abstracted structures
          if (newSMG.getPTEdge(hve.hasValue()).orElseThrow().pointsTo()
              instanceof SMGSinglyLinkedListSegment nestedLL) {
            topListsAndNestedToUpdate.put(targetSLL, nestedLL);
          }
        }
      }
    }
    SymbolicProgramConfiguration newSPC =
        of(
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
            readBlacklist);

    for (Entry<SMGObject, SMGObject> topListAndNestedToUpdate :
        topListsAndNestedToUpdate.entrySet()) {
      newSPC =
          newSPC.updateNestingLevelOf(
              topListAndNestedToUpdate.getValue(),
              topListAndNestedToUpdate.getKey().getNestingLevel() + 1);
    }
    return newSPC;
  }

  private SymbolicProgramConfiguration updateNestingLevelOf(
      SMGObject objectToUpdate, int newNestingLevel) {
    Preconditions.checkArgument(
        !(objectToUpdate instanceof SMGSinglyLinkedListSegment) || newNestingLevel >= 0);
    Preconditions.checkArgument(
        objectToUpdate instanceof SMGSinglyLinkedListSegment || newNestingLevel == 0);
    if (objectToUpdate.getNestingLevel() == newNestingLevel) {
      return this;
    }
    SymbolicProgramConfiguration newSPC = this;
    Preconditions.checkArgument(getSmg().isValid(objectToUpdate) && isHeapObject(objectToUpdate));
    SMGObject newObjWNestingLevel = objectToUpdate.copyWithNewLevel(newNestingLevel);
    // Add new heap obj
    newSPC = newSPC.copyAndAddHeapObject(newObjWNestingLevel);
    // Switch all HVEs to new
    newSPC = newSPC.copyHVEdgesFromTo(objectToUpdate, newObjWNestingLevel);
    // Switch all ptrs from old to new obj
    newSPC = newSPC.replaceAllPointersTowardsWith(objectToUpdate, newObjWNestingLevel);
    // invalidate old obj
    Preconditions.checkArgument(
        newSPC
            .smg
            .getAllSourcesForPointersPointingTowardsWithNumOfOccurrences(objectToUpdate)
            .isEmpty());
    return newSPC.invalidateSMGObject(objectToUpdate, false);
  }

  // Replace the pointer behind value with a new pointer with the new SMGObject target
  public SymbolicProgramConfiguration replaceAllPointersTowardsWith(
      SMGValue pointerValue, SMGObject newTarget) {
    return of(
        smg.replaceAllPointersTowardsWith(pointerValue, newTarget),
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory,
        readBlacklist);
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
        readBlacklist);
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
        readBlacklist);
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
            readBlacklist),
        unreachableObjects);
  }

  // For tests only
  public SymbolicProgramConfiguration replaceSMGValueNestingLevel(SMGValue value, int newLevel) {
    return copyAndReplaceSMG(smg.replaceSMGValueNestingLevel(value, newLevel));
  }

  /**
   * Removes the given object and all objects with pointers towards it or them recursively. (Removes
   * the subSMG, for objects pointing towards removed objects and pointers in those objects pointing
   * towards other memory)
   *
   * @param object {@link SMGObject} to be removed.
   * @return a new SPC with the object and subSMG removed.
   */
  public SymbolicProgramConfiguration copyAndRemoveObjectAndAssociatedSubSMG(SMGObject object) {
    // TODO: rework urgently
    // The following condition is obviously wrong!
    // There might be valid memory pointed to by other sources, but some memory we want to get rid
    // of.
    if (!getAllSourcesForPointersPointingTowards(object).isEmpty() || object.isZero()) {
      return this;
    }
    Set<SMGObject> targetsOfCurrent = getAllTargetsOfPointersInObject(object);
    SMGAndSMGObjects newSMGAndToRemoveObjects = smg.copyAndRemoveObjectAndSubSMG(object);
    SMG newSMG = newSMGAndToRemoveObjects.getSMG();
    PersistentSet<SMGObject> newHeapObject = heapObjects.removeAndCopy(object);
    for (SMGObject toRemove : newSMGAndToRemoveObjects.getSMGObjects()) {
      newHeapObject = newHeapObject.removeAndCopy(toRemove);
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
            memoryAddressAssumptionsMap.removeAndCopy(object),
            mallocZeroMemory,
            readBlacklist);
    for (SMGObject objectToRemove : targetsOfCurrent) {
      newSPC = newSPC.copyAndRemoveObjectAndAssociatedSubSMG(objectToRemove);
    }
    return newSPC;
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
    for (SMGValue newSMGValue : newSMGAndNewValuesForMapping.getSMGValues()) {
      Value newAddressValue = SymbolicValueFactory.getInstance().newIdentifier(null);
      newSPC =
          newSPC.copyAndPutValue(
              newAddressValue, newSMGValue, smg.getNestingLevel(pointerToNewObj));
    }
    return newSPC;
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
          return newSPC.copyAndPutValue(newValue, newSMGValue, 0);
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

          return newSPC.copyAndPutValue(newValue, oldSMGValue, 0);
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
    // Get all symbolic values in sizes (they might not have an SMGValue mapping anymore below!)
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
   * @return A copy of this SPC with the value mapping added.
   */
  public SymbolicProgramConfiguration copyAndPutValue(
      Value value, SMGValue smgValue, int nestingLevel) {
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
          readBlacklist);
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
        readBlacklist);
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
        readBlacklist);
  }

  /**
   * Changes the validity of an external object to valid.
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
        readBlacklist);
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
  public SymbolicProgramConfiguration copyAndCreateValue(Value cValue) {
    return copyAndCreateValue(cValue, 0);
  }

  /**
   * Copies this {@link SymbolicProgramConfiguration} and creates a mapping of a {@link Value} to a
   * newly created {@link SMGValue}. This checks if there is a mapping already, and if there exists
   * a mapping the unchanged SPC will be returned.
   *
   * @param cValue The {@link Value} you want to create a new, symbolic {@link SMGValue} for and map
   *     them to each other.
   * @param nestingLevel Nesting level of the new value.
   * @return The new SPC with the new {@link SMGValue} and the value mapping from the entered {@link
   *     Value} to the new {@link SMGValue}.
   */
  public SymbolicProgramConfiguration copyAndCreateValue(Value cValue, int nestingLevel) {
    SMGValue newSMGValue = SMGValue.of();
    return copyAndPutValue(cValue, newSMGValue, nestingLevel);
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
        readBlacklist);
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
        readBlacklist);
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
        readBlacklist);
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
        readBlacklist);
  }

  /**
   * Adds an SMGObject to the list of known SMGObjects, but nothing else.
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
   * Checks if an SMG object is valid in the current context.
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
   * @param offsetInBits the offset in the {@link SMGObject} in bits as {@link BigInteger}.
   * @param nestingLevel the nesting level of the value.
   * @return a copy of the SPC with the pointer to the {@link SMGObject} and the specified offset
   *     added.
   */
  public SymbolicProgramConfiguration copyAndAddPointerFromAddressToMemory(
      Value address,
      SMGObject target,
      Value offsetInBits,
      int nestingLevel,
      SMGTargetSpecifier pSMGTargetSpecifier) {
    // If there is no SMGValue for this Value (address) we create it, else we use the existing
    SymbolicProgramConfiguration spc = copyAndCreateValue(address, nestingLevel);
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
      Value address, SMGObject target, BigInteger offsetInBits, int nestingLevel) {
    // If there is no SMGValue for this address we create it, else we use the existing
    SymbolicProgramConfiguration spc = copyAndCreateValue(address, nestingLevel);
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
    if (target instanceof SMGSinglyLinkedListSegment sMGSinglyLinkedListSegment) {
      Preconditions.checkArgument(sMGSinglyLinkedListSegment.getMinLength() >= nestingLevel);
    }
    Preconditions.checkArgument(nestingLevel >= 0);
    return spc.copyAndReplaceSMG(spc.getSmg().copyAndAddPTEdge(pointsToEdge, smgAddress));
  }

  public SymbolicProgramConfiguration copyAndAddPointerFromAddressToRegionWithNestingLevel(
      Value address,
      SMGObject target,
      BigInteger offsetInBits,
      int nestingLevel,
      SMGTargetSpecifier specifier) {
    assert !(target instanceof SMGSinglyLinkedListSegment)
        || !specifier.equals(SMGTargetSpecifier.IS_REGION);
    assert target instanceof SMGSinglyLinkedListSegment
        || specifier.equals(SMGTargetSpecifier.IS_REGION);
    // If there is no SMGValue for this address we create it, else we use the existing
    SymbolicProgramConfiguration spc = copyAndCreateValue(address, nestingLevel);
    // If there is an existing SMGValue for address, no new one is created, but the old one is
    // returned. The nesting level might be wrong, however.
    SMGValue smgAddress = spc.getSMGValueFromValue(address).orElseThrow();
    // There was a mapping, update nesting level
    spc = spc.updateNestingLevel(smgAddress, nestingLevel);
    // Now we create a points-to-edge from this value to the target object at the
    // specified offset, overriding any existing from this value
    SMGPointsToEdge pointsToEdge = new SMGPointsToEdge(target, offsetInBits, specifier);
    if (target instanceof SMGSinglyLinkedListSegment sMGSinglyLinkedListSegment) {
      Preconditions.checkArgument(sMGSinglyLinkedListSegment.getMinLength() >= nestingLevel);
    }
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
   * existence of a known mapping from Value to SMGValue to an SMGPointsToEdge.
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
    CType type = variableToTypeMap.get(memLoc.getQualifiedName());
    return type;
  }

  public PersistentMap<String, CType> getVariableTypeMap() {
    return variableToTypeMap;
  }

  public SymbolicProgramConfiguration copyAndSetSpecifierOfPtrsTowards(
      SMGObject target, int nestingLvlToChange, SMGTargetSpecifier specifierToSet) {
    return new SymbolicProgramConfiguration(
        smg.copyAndSetTargetSpecifierForPtrsTowards(target, nestingLvlToChange, specifierToSet),
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory);
  }

  public SymbolicProgramConfiguration copyAndSetSpecifierOfPtrsTowards(
      SMGObject target,
      int nestingLvlToChange,
      SMGTargetSpecifier specifierToSet,
      Set<SMGTargetSpecifier> specifierToSwitch) {
    return new SymbolicProgramConfiguration(
        smg.copyAndSetTargetSpecifierForPtrsTowards(
            target, nestingLvlToChange, specifierToSet, specifierToSwitch),
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory);
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
        mallocZeroMemory);
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
        mallocZeroMemory);
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
        mallocZeroMemory);
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
        mallocZeroMemory);
  }

  /**
   * Search for all pointers towards the object old and replaces them with pointers pointing towards
   * the new object. All pointer nesting levels are decremented by 1. If the newTarget is a region,
   * specifiers are set to region. All other specifiers are retained.
   *
   * @param oldObj old object.
   * @param newObject new target object.
   * @return a new SPC with the replacement.
   */
  public SymbolicProgramConfiguration replaceAllPointersTowardsWithAndDecrementNestingLevel(
      SMGObject oldObj, SMGObject newObject) {
    return new SymbolicProgramConfiguration(
        smg.replaceAllPointersTowardsWithAndDecrementNestingLevel(oldObj, newObject),
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory);
  }

  /**
   * Search for all pointers towards the oldObj and switch them to newTarget. Then increments the
   * nesting level of the values of the changed pointers by 1. We expect that the newTarget does not
   * have any pointers towards it. Sets the specifiers for pointers so that if oldObj is not
   * abstracted, it's a first, all others become all.
   *
   * @param oldObj old object.
   * @param newObject new target object.
   * @return a new SMG with the replacement.
   */
  public SymbolicProgramConfiguration replaceAllPointersTowardsWithAndIncrementNestingLevel(
      SMGObject oldObj, SMGObject newObject, int incrementAmount) {
    return new SymbolicProgramConfiguration(
        smg.replaceAllPointersTowardsWithAndIncrementNestingLevel(
            oldObj, newObject, incrementAmount),
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory);
  }

  /**
   * Search for all pointers towards the object old and replaces them with pointers pointing towards
   * the new object only if their nesting level and specifier is equal to the given. Then switches
   * the nesting level of the switched to 0.
   *
   * @param oldObj old object.
   * @param newObject new target object.
   * @param replacementLevel the level to switch
   * @param specifierToSwitch the specifiers that are allowed to be switched to the new object. All
   *     others remain on old obj.
   * @return a new SMG with the replacement.
   */
  public SymbolicProgramConfiguration replaceSpecificPointersTowardsWithAndSetNestingLevelZero(
      SMGObject oldObj,
      SMGObject newObject,
      int replacementLevel,
      Set<SMGTargetSpecifier> specifierToSwitch) {
    return new SymbolicProgramConfiguration(
        smg.replaceSpecificPointersTowardsWithAndSetNestingLevelZero(
            oldObj, newObject, replacementLevel, specifierToSwitch),
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory);
  }

  /**
   * Search for all pointers towards the object old and replaces them with pointers pointing towards
   * the new object only if their nesting level is equal to the given. Then switches the nesting
   * level of the switched to 0.
   *
   * @param oldTargetObj old target object of pointers to switch.
   * @param replacementValue new tSMGValue that replaces all pointers found.
   * @param nestingLevelToSwitch the level of pointers to switch to the new value.
   * @param specifierToSwitch all specifiers to switch to the new value.
   * @return a new SMG with the replacement.
   */
  public SymbolicProgramConfiguration replacePointersWithSMGValue(
      SMGObject oldTargetObj,
      SMGValue replacementValue,
      int nestingLevelToSwitch,
      Set<SMGTargetSpecifier> specifierToSwitch) {
    return new SymbolicProgramConfiguration(
        smg.replacePointersWithSMGValue(
            oldTargetObj, replacementValue, nestingLevelToSwitch, specifierToSwitch),
        globalVariableMapping,
        atExitStack,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping,
        variableToTypeMap,
        memoryAddressAssumptionsMap,
        mallocZeroMemory);
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
        mallocZeroMemory);
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
        mallocZeroMemory);
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
        mallocZeroMemory);
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
        mallocZeroMemory);
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
        mallocZeroMemory.putAndCopy(memory, true));
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
        mallocZeroMemory.removeAndCopy(memory));
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

  public boolean isPointingToMallocZero(SMGValue pSMGValue) {
    return !mallocZeroMemory.isEmpty()
        && getSmg().isPointer(pSMGValue)
        && mallocZeroMemory.containsKey(getSmg().getPTEdge(pSMGValue).orElseThrow().pointsTo());
  }
}
