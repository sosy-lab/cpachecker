// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.common.collect.Collections3.listAndElement;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentStack;
import org.sosy_lab.cpachecker.cpa.smg2.SMGErrorInfo.Property;
import org.sosy_lab.cpachecker.cpa.smg2.abstraction.SMGCPAMaterializer;
import org.sosy_lab.cpachecker.cpa.smg2.constraint.BooleanAndSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.constraint.ConstraintFactory;
import org.sosy_lab.cpachecker.cpa.smg2.refiner.SMGInterpolant;
import org.sosy_lab.cpachecker.cpa.smg2.util.CFunctionDeclarationAndOptionalValue;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGHasValueEdgesAndSPC;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectAndSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGSolverException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGStateAndOptionalSMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.SPCAndSMGObjects;
import org.sosy_lab.cpachecker.cpa.smg2.util.ValueAndValueSize;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AddressExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue.NegativeNaN;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.refinement.ImmutableForgetfulState;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.graph.SMGDoublyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGSinglyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;
import org.sosy_lab.cpachecker.util.smg.join.SMGJoinSPC;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;

/**
 * Class holding the SPC (SymbolicProgramConfiguration = memory model) for heap, global
 * variables/constants and the stack. Also provides methods to manipulate the SMG; meaning
 * adding/pruning/reading and memory error/leak handling. This class is meant to represent the
 * CPAState, while the memory state is represented by the SPC. This class therefore hands down
 * write/read and other memory operations. It is expected that in the SPC no CPA specific stuff is
 * handled.
 */
public class SMGState
    implements ImmutableForgetfulState<SMGInformation>,
        LatticeAbstractState<SMGState>,
        AbstractQueryableState,
        Graphable {

  // Properties:
  private static final String HAS_INVALID_FREES = "has-invalid-frees";

  private static final String HAS_INVALID_READS = "has-invalid-reads";

  private static final String HAS_INVALID_WRITES = "has-invalid-writes";

  private static final String HAS_LEAKS = "has-leaks";

  private static final String HAS_HEAP_OBJECTS = "has-heap-objects";

  @SuppressWarnings("unused")
  private static final Pattern externalAllocationRecursivePattern =
      Pattern.compile("^(r_)(\\d+)(_.*)$");

  // All memory models (SMGs) (heap/global/stack)
  private final SymbolicProgramConfiguration memoryModel;

  private final MachineModel machineModel;
  private final LogManagerWithoutDuplicates logger;
  private final List<SMGErrorInfo> errorInfo;
  private final SMGOptions options;
  // Transformer for abstracted heap into concrete heap
  private final SMGCPAMaterializer materializer;

  // Tracks the last checked memory access Constraint
  private final Optional<Constraint> lastCheckedMemoryAccess;

  // Holds the constraints and the model/definite value assignment
  private final ConstraintsState constraintsState;

  private final SMGCPAExpressionEvaluator evaluator;

  // Constructor only for NEW/EMPTY SMGStates!
  private SMGState(
      MachineModel pMachineModel,
      SymbolicProgramConfiguration spc,
      LogManagerWithoutDuplicates logManager,
      SMGOptions opts,
      SMGCPAExpressionEvaluator pEvaluator) {
    memoryModel = spc;
    machineModel = pMachineModel;
    logger = logManager;
    options = opts;
    errorInfo = ImmutableList.of();
    materializer = new SMGCPAMaterializer(logger);
    lastCheckedMemoryAccess = Optional.empty();
    evaluator = pEvaluator;
    constraintsState = new ConstraintsState();
  }

  private SMGState(
      MachineModel pMachineModel,
      SymbolicProgramConfiguration spc,
      LogManagerWithoutDuplicates logManager,
      SMGOptions opts,
      List<SMGErrorInfo> errorInf,
      SMGCPAMaterializer pMaterializer,
      Optional<Constraint> pLastCheckedMemoryAccess,
      ConstraintsState pConstraintsState,
      SMGCPAExpressionEvaluator pEvaluator) {
    memoryModel = spc;
    machineModel = pMachineModel;
    logger = logManager;
    options = opts;
    errorInfo = errorInf;
    materializer = pMaterializer;
    lastCheckedMemoryAccess = pLastCheckedMemoryAccess;
    evaluator = pEvaluator;
    constraintsState = pConstraintsState;
  }

  private SMGState of(ImmutableList<Constraint> pConstraints) {
    checkNotNull(pConstraints);
    return new SMGState(
        machineModel,
        memoryModel,
        logger,
        options,
        errorInfo,
        materializer,
        lastCheckedMemoryAccess,
        constraintsState.copyWithNew(pConstraints),
        evaluator);
  }

  private SMGState ofModelAssignment(
      ImmutableCollection<ValueAssignment> pDefiniteAssignment,
      ImmutableList<ValueAssignment> pLastModelAsAssignment) {
    return new SMGState(
        machineModel,
        memoryModel,
        logger,
        options,
        errorInfo,
        materializer,
        lastCheckedMemoryAccess,
        constraintsState
            .copyWithDefiniteAssignment(pDefiniteAssignment)
            .copyWithSatisfyingModel(pLastModelAsAssignment),
        evaluator);
  }

  private SMGState ofLastCheckedMemoryBounds(Optional<Constraint> pLastCheckedMemoryAccess) {
    return new SMGState(
        machineModel,
        memoryModel,
        logger,
        options,
        errorInfo,
        materializer,
        pLastCheckedMemoryAccess,
        constraintsState,
        evaluator);
  }

  public SMGState addConstraint(Constraint pConstraint) {
    checkNotNull(pConstraint);
    return of(listAndElement(constraintsState, pConstraint));
  }

  public SMGState updateLastCheckedMemoryBounds(Constraint pConstraint) {
    checkNotNull(pConstraint);
    return ofLastCheckedMemoryBounds(Optional.of(pConstraint));
  }

  public boolean isEmptyConstraints() {
    return constraintsState.isEmpty();
  }

  public Optional<Constraint> getLastAddedConstraint() {
    return constraintsState.getLastAddedConstraint();
  }

  public SMGState replaceModelAndDefAssignmentAndCopy(
      Optional<ImmutableCollection<ValueAssignment>> pDefiniteAssignment,
      Optional<ImmutableList<ValueAssignment>> pLastModelAsAssignment) {
    return ofModelAssignment(
        pDefiniteAssignment.orElse(ImmutableList.of()), pLastModelAsAssignment.orElseThrow());
  }

  public ConstraintsState getConstraints() {
    return constraintsState;
  }

  @SuppressWarnings("unused")
  public boolean containsConstraint(Constraint o) {
    return constraintsState.contains(o);
  }

  /**
   * Returns the known unambiguous assignment of variables so this state's {@link Constraint}s are
   * fulfilled. Variables that can have more than one valid assignment are not included in the
   * returned assignments.
   *
   * @return the known assignment of variables that have no other fulfilling assignment
   */
  public ImmutableCollection<ValueAssignment> getDefiniteAssignment() {
    return constraintsState.getDefiniteAssignment();
  }

  /** Returns the last model computed for this constraints state. */
  public ImmutableList<ValueAssignment> getModel() {
    return constraintsState.getModel();
  }

  @Override
  public Object evaluateProperty(String pProperty) throws InvalidQueryException {
    return switch (pProperty) {
      case "toString" -> toString();
      case "heapObjects" -> memoryModel.getHeapObjects();
      default ->
          // try boolean properties
          checkProperty(pProperty);
    };
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    switch (pProperty) {
      case HAS_LEAKS:
        if (hasMemoryLeak()) {
          // TODO: Give more information
          issueMemoryError("Memory leak found", false);
          return true;
        }
        return false;
      case HAS_INVALID_WRITES:
        if (hasInvalidWrite()) {
          // TODO: Give more information
          issueMemoryError("Invalid write found", true);
          return true;
        }
        return false;
      case HAS_INVALID_READS:
        if (hasInvalidRead()) {
          // TODO: Give more information
          issueMemoryError("Invalid read found", true);
          return true;
        }
        return false;
      case HAS_INVALID_FREES:
        if (hasInvalidFree()) {
          // TODO: Give more information
          issueMemoryError("Invalid free found", true);
          return true;
        }
        return false;
      case HAS_HEAP_OBJECTS:
        // Having heap objects is not an error on its own.
        // However, when combined with program exit, we can detect property MemCleanup.
        PersistentSet<SMGObject> heapObs = memoryModel.getHeapObjects();
        Preconditions.checkState(
            !heapObs.isEmpty() && heapObs.contains(SMGObject.nullInstance()),
            "NULL must always be a heap object");
        // TODO: check the validity check!
        for (SMGObject object : heapObs) {
          if (!memoryModel.isObjectValid(object)) {
            heapObs = heapObs.removeAndCopy(object);
          }
        }
        return !heapObs.isEmpty();

      default:
        throw new InvalidQueryException("Query '" + pProperty + "' is invalid.");
    }
  }

  private void issueMemoryError(String pMessage, boolean pUndefinedBehavior) {
    if (options.isMemoryErrorTarget()) {
      logger.log(Level.FINE, pMessage);
    } else if (pUndefinedBehavior) {
      logger.log(Level.FINE, pMessage);
      logger.log(
          Level.FINE,
          "Non-target undefined behavior detected. The verification result is unreliable.");
    }
  }

  private boolean hasInvalidWrite() {
    for (SMGErrorInfo errorInf : errorInfo) {
      if (errorInf.isInvalidWrite()) {
        return true;
      }
    }
    return false;
  }

  private boolean hasInvalidRead() {
    for (SMGErrorInfo errorInf : errorInfo) {
      if (errorInf.isInvalidRead()) {
        return true;
      }
    }
    return false;
  }

  private boolean hasInvalidFree() {
    for (SMGErrorInfo errorInf : errorInfo) {
      if (errorInf.isInvalidFree()) {
        return true;
      }
    }
    return false;
  }

  public boolean hasMemoryLeak() {
    for (SMGErrorInfo errorInf : errorInfo) {
      if (errorInf.hasMemoryLeak()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Creates a new, empty {@link SMGState} with the {@link SMGOptions} given. The {@link
   * SymbolicProgramConfiguration} and {@link SMGErrorInfo} inside are new and empty as well. This
   * does not create a stack frame and should only be used for testing!
   *
   * @param pMachineModel the {@link MachineModel} used to determine the size of types.
   * @param logManager {@link LogManager} to log important information.
   * @param opts {@link SMGOptions} to be used.
   * @return a newly created {@link SMGState} with a new and empty {@link
   *     SymbolicProgramConfiguration} inside.
   */
  public static SMGState of(
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates logManager,
      SMGOptions opts,
      SMGCPAExpressionEvaluator pEvaluator) {
    return new SMGState(
        pMachineModel,
        SymbolicProgramConfiguration.of(BigInteger.valueOf(pMachineModel.getSizeofPtrInBits())),
        logManager,
        opts,
        pEvaluator);
  }

  /**
   * Creates a new, empty {@link SMGState} with the {@link SMGOptions} given. The {@link
   * SymbolicProgramConfiguration} and {@link SMGErrorInfo} inside are new and empty as well. The
   * given CPA is used to extract the main function if possible and create the inital stack frame
   * automatically.
   *
   * @param pMachineModel the {@link MachineModel} used to determine the size of types.
   * @param logManager {@link LogManager} to log important information.
   * @param opts {@link SMGOptions} to be used.
   * @param pCfa used to extract the main function.
   * @return a newly created {@link SMGState} with a new and empty {@link
   *     SymbolicProgramConfiguration} inside. The only thing added is the inital stack frame if
   *     possible.
   */
  public static SMGState of(
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates logManager,
      SMGOptions opts,
      CFA pCfa,
      SMGCPAExpressionEvaluator pEvaluator) {
    FunctionEntryNode pNode = pCfa.getMainFunction();
    return of(pMachineModel, logManager, opts, pNode, pEvaluator);
  }

  /**
   * Creates a new, empty {@link SMGState} with the {@link SMGOptions} given. The {@link
   * SymbolicProgramConfiguration} and {@link SMGErrorInfo} inside are new and empty as well. The
   * given CPA is used to extract the main function if possible and create the inital stack frame
   * automatically.
   *
   * @param pMachineModel the {@link MachineModel} used to determine the size of types.
   * @param logManager {@link LogManager} to log important information.
   * @param opts {@link SMGOptions} to be used.
   * @param cfaFunEntryNode main function node from the CFA!
   * @return a newly created {@link SMGState} with a new and empty {@link
   *     SymbolicProgramConfiguration} inside. The only thing added is the inital stack frame if
   *     possible.
   */
  public static SMGState of(
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates logManager,
      SMGOptions opts,
      FunctionEntryNode cfaFunEntryNode,
      SMGCPAExpressionEvaluator pEvaluator) {
    SMGState newState = of(pMachineModel, logManager, opts, pEvaluator);
    if (cfaFunEntryNode instanceof CFunctionEntryNode functionNode) {
      return newState.copyAndAddStackFrame(functionNode.getFunctionDefinition());
    }
    return newState;
  }

  /**
   * Creates a new, empty {@link SMGState} with the {@link SMGOptions} given. The {@link
   * SymbolicProgramConfiguration} and {@link SMGErrorInfo} inside are new and empty as well. The
   * given CPA is used to extract the main function if possible and create the inital stack frame
   * automatically.
   *
   * @param pMachineModel the {@link MachineModel} used to determine the size of types.
   * @param logManager {@link LogManager} to log important information.
   * @param opts {@link SMGOptions} to be used.
   * @param cfaEntryFunDecl main function declaration from the CFA!
   * @return a newly created {@link SMGState} with a new and empty {@link
   *     SymbolicProgramConfiguration} inside. The only thing added is the inital stack frame if
   *     possible.
   */
  public static SMGState of(
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates logManager,
      SMGOptions opts,
      CFunctionDeclaration cfaEntryFunDecl,
      SMGCPAExpressionEvaluator pEvaluator) {
    return of(pMachineModel, logManager, opts, pEvaluator).copyAndAddStackFrame(cfaEntryFunDecl);
  }

  /**
   * Creates a new {@link SMGState} out of the parameters given. No new elements are created by
   * this.
   *
   * @param pMachineModel the {@link MachineModel} used to determine the size of types.
   * @param pSPC the {@link SymbolicProgramConfiguration} to be used in the new state.
   * @param logManager the {@link LogManager} to be used in the new state.
   * @param opts {@link SMGOptions} to be used.
   * @param pErrorInfo the {@link SMGErrorInfo} holding error information.
   * @return a new {@link SMGState} with the arguments given.
   */
  public SMGState off(
      MachineModel pMachineModel,
      SymbolicProgramConfiguration pSPC,
      LogManagerWithoutDuplicates logManager,
      SMGOptions opts,
      List<SMGErrorInfo> pErrorInfo,
      SMGCPAExpressionEvaluator pEvaluator) {
    return new SMGState(
        pMachineModel,
        pSPC,
        logManager,
        opts,
        pErrorInfo,
        materializer,
        lastCheckedMemoryAccess,
        constraintsState,
        pEvaluator);
  }

  /**
   * Copies the state and replaces the SPC
   *
   * @param pSPC a new SPC that has to be consistent with the rest of the state.
   * @return a new state with the spc given
   */
  public SMGState copyAndReplaceMemoryModel(SymbolicProgramConfiguration pSPC) {
    return new SMGState(
        machineModel,
        pSPC,
        logger,
        options,
        errorInfo,
        materializer,
        lastCheckedMemoryAccess,
        constraintsState,
        evaluator);
  }

  /**
   * Checks the presence of a {@link MemoryLocation} (excluding the offset) as a global or local
   * variable anywhere (not only the current stack frame).
   *
   * @param memLoc this method will only extract the qualified name!
   * @return true if it exists as a variable.
   */
  public boolean isLocalOrGlobalVariablePresent(MemoryLocation memLoc) {
    String qualifiedName = memLoc.getQualifiedName();
    return isGlobalVariablePresent(qualifiedName) || isLocalVariablePresentAnywhere(qualifiedName);
  }

  public boolean isLocalOrGlobalVariablePresent(String qualifiedName) {
    return isGlobalVariablePresent(qualifiedName) || isLocalVariablePresent(qualifiedName);
  }

  /**
   * We might have invalidated a local variable. This checks for that. If a variable is not visible,
   * empty is returned.
   *
   * @param qualifiedName name of the variable.
   * @return Optional.of(true) if valid, flase if invalid, empty for variable not found.
   */
  public Optional<Boolean> isLocalOrGlobalVariableValid(String qualifiedName) {
    if (isGlobalVariablePresent(qualifiedName) || isLocalVariablePresentAnywhere(qualifiedName)) {
      Optional<SMGObject> memRegion = memoryModel.getObjectForVisibleVariable(qualifiedName);
      if (memRegion.isPresent()) {
        return Optional.of(memoryModel.isObjectValid(memRegion.orElseThrow()));
      }
    }
    return Optional.empty();
  }

  public SMGState copyAndRemoveStackVariable(String qualifiedName) {
    return copyAndReplaceMemoryModel(memoryModel.copyAndRemoveStackVariable(qualifiedName));
  }

  @SuppressWarnings("unused")
  private SMGState assignReturnValue(
      MemoryLocation memLoc,
      ValueAndValueSize valueAndSize,
      Map<String, BigInteger> variableNameToMemorySizeInBits,
      Map<String, CType> variableTypeMap)
      throws SMGSolverException {
    SMGState currentState = this;
    SMGObject obj = getReturnObjectForMemoryLocation(memLoc);
    BigInteger offsetToWriteToInBits = BigInteger.valueOf(memLoc.getOffset());
    @Nullable BigInteger sizeOfWriteInBits = valueAndSize.getSizeInBits();
    Preconditions.checkArgument(sizeOfWriteInBits != null);
    Value valueToWrite = valueAndSize.getValue();
    Preconditions.checkArgument(!valueToWrite.isUnknown());
    CType typeOfUnknown = null;
    CType simpleType = variableTypeMap.get(memLoc.getQualifiedName());
    if (simpleType != null && simpleType.getCanonicalType() instanceof CSimpleType) {
      typeOfUnknown = simpleType;
    }
    // TODO: use variableTypeMap and deconstruct struct and array types to the correct ones
    // This is only needed for floats nested in these types btw.
    return currentState.writeValueWithChecks(
        obj,
        new NumericValue(offsetToWriteToInBits),
        sizeOfWriteInBits,
        valueToWrite,
        typeOfUnknown,
        null);
  }

  /*
   *  Checks the existing of the return variable with the entered memLoc.
   *  False if the memloc is no function return.
   *  Our CEGAR algorithm has forced my hand on this.
   */
  private boolean isFunctionReturnVariableAndPresent(MemoryLocation memLoc) {
    if (memLoc.getIdentifier().equals("__retval__")) {
      if (getReturnObjectForMemoryLocation(memLoc) != null) {
        return true;
      }
    }
    return false;
  }

  /**
   * Writes the Value given to the variable (local or global) given.
   *
   * @param memLoc Name and offset in bits for the variables to write.
   * @param valueAndSize new Value and size of the type in bits.
   * @param variableNameToMemorySizeInBits the size of the variable in total in bits.
   * @return a new state with given values written to the variable given at the position given.
   * @throws SMGException should never happen in this case as the writes are copies and therefore
   *     save.
   */
  public SMGState assignNonHeapConstant(
      MemoryLocation memLoc,
      ValueAndValueSize valueAndSize,
      Map<String, BigInteger> variableNameToMemorySizeInBits,
      Map<String, CType> variableTypeMap)
      throws SMGException, SMGSolverException {

    if (isFunctionReturnVariableAndPresent(memLoc)) {
      return assignReturnValue(
          memLoc, valueAndSize, variableNameToMemorySizeInBits, variableTypeMap);
    }
    SMGState currentState = this;
    // Deconstruct MemoryLocation to get the qualified name of global/local vars
    // And remember the offset. Offset + size from ValueAndValueSize are the
    // SMGHasValueEdge information besides the mapping, which is either a new mapping
    // or an old one found in the current mapping
    String qualifiedName = memLoc.getQualifiedName();
    if (!isLocalOrGlobalVariablePresent(memLoc)) {
      // Create the variable first
      BigInteger sizeInBits = variableNameToMemorySizeInBits.get(qualifiedName);
      if (memLoc.isOnFunctionStack()) {
        // Add depending on function stack!
        currentState =
            currentState.copyAndAddLocalVariableToSpecificStackframe(
                memLoc.getFunctionName(),
                sizeInBits,
                qualifiedName,
                variableTypeMap.get(qualifiedName));
      } else {
        currentState =
            currentState.copyAndAddGlobalVariable(
                sizeInBits, qualifiedName, variableTypeMap.get(qualifiedName));
      }
    }
    BigInteger offsetToWriteToInBits = BigInteger.valueOf(memLoc.getOffset());
    @Nullable BigInteger sizeOfWriteInBits = valueAndSize.getSizeInBits();
    Preconditions.checkArgument(sizeOfWriteInBits != null);
    Value valueToWrite = valueAndSize.getValue();
    Preconditions.checkArgument(!valueToWrite.isUnknown());
    // Null is fine because that would only be needed for the unknown case which can't happen
    CType typeOfUnknown = null;
    // Write (easier then inserting everything on its own, and guaranteed to succeed as its a copy
    // from the original state)
    CType simpleType = variableTypeMap.get(memLoc.getQualifiedName());
    if (simpleType != null && simpleType.getCanonicalType() instanceof CSimpleType) {
      typeOfUnknown = simpleType;
    }
    // TODO: use variableTypeMap and deconstruct struct and array types to the correct ones
    // This is only needed for floats nested in these types btw.
    return currentState.writeToAnyStackOrGlobalVariable(
        qualifiedName, offsetToWriteToInBits, sizeOfWriteInBits, valueToWrite, typeOfUnknown);
  }

  public SMGState reconstructStackFrames(
      PersistentStack<CFunctionDeclarationAndOptionalValue> pStackDeclarations)
      throws SMGSolverException {
    SMGState currentState = this;
    // the given stack is reversed! We can
    Iterator<StackFrame> existingFrames = currentState.memoryModel.getStackFrames().iterator();
    Iterator<CFunctionDeclarationAndOptionalValue> shouldBeFrames = pStackDeclarations.iterator();
    // The current should have the main!
    // The other can be empty for the false/true interpolants
    while (shouldBeFrames.hasNext()) {
      if (existingFrames.hasNext()) {
        // As long as there are frames on both, they should match
        StackFrame thisFrame = existingFrames.next();
        CFunctionDeclarationAndOptionalValue otherFunDefAndReturnValue = shouldBeFrames.next();
        CFunctionDeclaration otherFunDef = otherFunDefAndReturnValue.getCFunctionDeclaration();
        Preconditions.checkArgument(thisFrame.getFunctionDefinition().equals(otherFunDef));
      } else {
        // Start adding to the current
        CFunctionDeclarationAndOptionalValue otherFunDefAndReturnValue = shouldBeFrames.next();
        CFunctionDeclaration otherFunDef = otherFunDefAndReturnValue.getCFunctionDeclaration();
        currentState = currentState.copyAndAddStackFrame(otherFunDef);
        if (otherFunDefAndReturnValue.hasReturnValue()) {
          currentState = currentState.writeToReturn(otherFunDefAndReturnValue.getReturnValue());
        }
      }
    }

    return currentState;
  }

  /**
   * Verifies that the given {@link MemoryLocation} has the given {@link Value} (with respect to
   * their numerical value only). Used as sanity check for interpolants.
   *
   * @param variableAndOffset Variable name and offset to read in bits.
   * @param valueAndSize expected Value and read size in bits.
   * @return true if the Values match with respect to their numeric interpretation without types.
   *     False else.
   */
  @SuppressWarnings("unused")
  public boolean verifyVariableEqualityWithValueAt(
      MemoryLocation variableAndOffset, ValueAndValueSize valueAndSize) throws SMGException {
    Value expectedValue = valueAndSize.getValue();
    Value readValue = getValueToVerify(variableAndOffset, valueAndSize);
    // Note: asNumericValue() returns null for non numerics
    return expectedValue.asNumericValue().longValue() == readValue.asNumericValue().longValue();
  }

  /* public for debugging purposes in interpolation only! */
  public Value getValueToVerify(MemoryLocation variableAndOffset, ValueAndValueSize valueAndSize)
      throws SMGException {
    String variableName = variableAndOffset.getQualifiedName();
    BigInteger offsetInBits = BigInteger.valueOf(variableAndOffset.getOffset());
    // Null for new interpolants, return unknown
    @Nullable BigInteger sizeOfReadInBits = valueAndSize.getSizeInBits();
    if (sizeOfReadInBits == null) {
      return UnknownValue.getInstance();
    }

    SMGObject memoryToRead = memoryModel.getObjectForVariable(variableName).orElseThrow();
    // We don't expect materialization here
    return readValueWithoutMaterialization(memoryToRead, offsetInBits, sizeOfReadInBits, null)
        .getValue();
  }

  /**
   * Removes ALL {@link MemoryLocation}s given from the state and then adds them back in with the
   * values given. The given Values should never represent any heap related Values (pointers). It is
   * expected that only if nonHeapAssignments is null, the other 2 nullables are null as well.
   *
   * @param nonHeapAssignments {@link MemoryLocation}s and matching {@link ValueAndValueSize} for
   *     each variable to be changed.
   * @param variableNameToMemorySizeInBits the overall size of the variables.
   * @return a new SMGState with all entered variables (MemoryLocations) removed and then
   * @throws SMGException should never be thrown! If it is thrown, then there is a bug.
   */
  public SMGState reconstructSMGStateFromNonHeapAssignments(
      @Nullable PersistentMap<MemoryLocation, ValueAndValueSize> nonHeapAssignments,
      @Nullable Map<String, BigInteger> variableNameToMemorySizeInBits,
      @Nullable Map<String, CType> variableTypeMap,
      PersistentStack<CFunctionDeclarationAndOptionalValue> pStackDeclarations)
      throws SMGException, SMGSolverException {
    if (nonHeapAssignments == null || pStackDeclarations == null) {
      return this;
    }
    SMGState currentState = this;
    // Reconstruct the stack frames first
    currentState = currentState.reconstructStackFrames(pStackDeclarations);

    for (Entry<MemoryLocation, ValueAndValueSize> entry : nonHeapAssignments.entrySet()) {
      currentState =
          currentState.assignNonHeapConstant(
              entry.getKey(), entry.getValue(), variableNameToMemorySizeInBits, variableTypeMap);
    }
    return currentState;
  }

  /**
   * Merge the error info of pOther into this {@link SMGState}.
   *
   * @param pOther the state you want the error info from.
   * @return this state with the error info of this + other.
   */
  public SMGState withViolationsOf(SMGState pOther) {
    if (errorInfo.equals(pOther.errorInfo)) {
      return this;
    }
    return copyWithNewErrorInfo(
        new ImmutableList.Builder<SMGErrorInfo>()
            .addAll(errorInfo)
            .addAll(pOther.errorInfo)
            .build());
  }

  /**
   * Copy SMGState with a newly created object and put it into the global namespace. This replaces
   * an existing old global variable!
   *
   * @param pTypeSizeInBits Size of the type of the new global variable.
   * @param pVarName Name of the global variable.
   * @return Newly created {@link SMGState} with the object added for the name specified.
   */
  public SMGState copyAndAddGlobalVariable(int pTypeSizeInBits, String pVarName, CType type) {
    // TODO: do we really need this for ints?
    return copyAndAddGlobalVariable(BigInteger.valueOf(pTypeSizeInBits), pVarName, type);
  }

  /**
   * Copy SMGState with a newly created object and put it into the global namespace. This replaces
   * an existing old global variable!
   *
   * @param pTypeSizeInBits Size of the type of the new global variable.
   * @param pVarName Name of the global variable.
   * @return Newly created {@link SMGState} with the object added for the name specified.
   */
  public SMGState copyAndAddGlobalVariable(
      BigInteger pTypeSizeInBits, String pVarName, CType type) {
    SMGObject newObject = SMGObject.of(0, pTypeSizeInBits, BigInteger.ZERO);
    if (pVarName.endsWith("_STRING_LITERAL")) {
      newObject = newObject.copyAsConstStringInBinary();
    }
    return copyAndReplaceMemoryModel(memoryModel.copyAndAddGlobalObject(newObject, pVarName, type));
  }

  /**
   * Copy SMGState with a newly created {@link SMGObject} and returns the new state + the new {@link
   * SMGObject} with the size specified in bits. Make sure that you reuse the {@link SMGObject}
   * right away to create a points-to-edge and not just use SMGObjects in the code.
   *
   * @param pTypeSizeInBits Size of the type of the new global variable.
   * @return Newly created object + state with it.
   */
  public SMGObjectAndSMGState copyAndAddHeapObject(BigInteger pTypeSizeInBits) {
    SMGObject newObject = SMGObject.of(0, pTypeSizeInBits, BigInteger.ZERO);
    return SMGObjectAndSMGState.of(
        newObject, copyAndReplaceMemoryModel(memoryModel.copyAndAddHeapObject(newObject)));
  }

  /* Only used by abstraction materialization */
  public SMGState copyAndAddObjectToHeap(SMGObject object) {
    return copyAndReplaceMemoryModel(memoryModel.copyAndAddHeapObject(object));
  }

  // Only to be used by materilization to copy a SMGObject
  public SMGState copyAllValuesFromObjToObj(SMGObject source, SMGObject target) {
    return copyAndReplaceMemoryModel(memoryModel.copyAllValuesFromObjToObj(source, target));
  }

  // Only to be used by materilization to copy a SMGObject
  // Replace the pointer behind value with a new pointer with the new SMGObject target
  public SMGState replaceAllPointersTowardsWith(SMGValue pointerValue, SMGObject newTarget) {
    return copyAndReplaceMemoryModel(
        memoryModel.replaceAllPointersTowardsWith(pointerValue, newTarget));
  }

  /**
   * Copy SMGState with a newly created {@link SMGObject} and returns the new state + the new {@link
   * SMGObject} with the size specified in bits. Make sure that you reuse the {@link SMGObject}
   * right away to create a points-to-edge and not just use SMGObjects in the code.
   *
   * @param pTypeSizeInBits Size of the type of the new global variable.
   * @return Newly created object + state with it.
   */
  public SMGObjectAndSMGState copyAndAddStackObject(BigInteger pTypeSizeInBits) {
    SMGObject newObject = SMGObject.of(0, pTypeSizeInBits, BigInteger.ZERO);
    return SMGObjectAndSMGState.of(
        newObject, copyAndReplaceMemoryModel(memoryModel.copyAndAddStackObject(newObject)));
  }

  /**
   * Checks if a global variable exists for the name given.
   *
   * @param pVarName Name of the global variable.
   * @return true if the var exists, false else.
   */
  public boolean isGlobalVariablePresent(String pVarName) {
    return memoryModel.getGlobalVariableToSmgObjectMap().containsKey(pVarName);
  }

  /**
   * Checks if a local variable exists for the name given. Note: this checks ALL stack frames.
   *
   * @param pVarName Name of the local variable.
   * @return true if the var exists, false else.
   */
  private boolean isLocalVariablePresentAnywhere(String pVarName) {
    PersistentStack<StackFrame> frames = memoryModel.getStackFrames();
    for (StackFrame stackframe : frames) {
      if (stackframe.getVariables().containsKey(pVarName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if a local variable exists for the name given. Note: this checks only the topmost stack
   * frames.
   *
   * @param pVarName Name of the local variable.
   * @return true if the var exists, false else.
   */
  private boolean isLocalVariablePresent(String pVarName) {
    PersistentStack<StackFrame> frames = memoryModel.getStackFrames();
    StackFrame stackframe = frames.peek();
    return stackframe.getVariables().containsKey(pVarName);
  }

  /**
   * Checks if a local variable exists for the name given for the previous function. CPAchecker
   * forces my hand in things like strengthening as it wants to give me assume edges for previous
   * functions.....
   *
   * @param pVarName Name of the local variable.
   * @return true if the var exists, false else.
   */
  protected boolean isLocalVariablePresentOnPreviousStackFrame(String pVarName) {
    PersistentStack<StackFrame> frames = memoryModel.getStackFrames();
    if (frames.size() < 2) {
      return false;
    }
    StackFrame stackframe = frames.popAndCopy().peek();
    if (stackframe.getVariables().containsKey(pVarName)) {
      return true;
    }

    return false;
  }

  /**
   * Copy SMGState with a newly created object with the size given and put it into the current stack
   * frame. If there is no stack frame this throws an exception!
   *
   * <p>Keeps consistency: yes
   *
   * @param pTypeSize Size of the type the new local variable in bits.
   * @param pVarName Name of the local variable
   * @return {@link SMGState} with the new variables searchable by the name given.
   * @throws SMGException thrown if the stack frame is empty.
   */
  public SMGState copyAndAddLocalVariable(int pTypeSize, String pVarName, CType type)
      throws SMGException {
    if (memoryModel.getStackFrames().isEmpty()) {
      throw new SMGException(
          "Can't add a variable named "
              + pVarName
              + " to the memory model because there is no stack frame.");
    }
    SMGObject newObject = SMGObject.of(0, BigInteger.valueOf(pTypeSize), BigInteger.ZERO);
    return copyAndReplaceMemoryModel(memoryModel.copyAndAddStackObject(newObject, pVarName, type));
  }

  /**
   * Add a local variable based on an existing SMGObject. I.e. a local array.
   *
   * @param object the existing memory
   * @param pVarName variable name qualified
   * @param type the type of the object
   * @return the new state with the association
   * @throws SMGException in case of critical errors
   */
  public SMGState copyAndAddLocalVariable(SMGObject object, String pVarName, CType type)
      throws SMGException {
    if (memoryModel.getStackFrames().isEmpty()) {
      throw new SMGException(
          "Can't add a variable named "
              + pVarName
              + " to the memory model because there is no stack frame.");
    }
    return copyAndReplaceMemoryModel(memoryModel.copyAndAddStackObject(object, pVarName, type));
  }

  /**
   * Copy SMGState with a newly created object with the size given and put it into the current stack
   * frame. If there is no stack frame this throws an exception!
   *
   * <p>Keeps consistency: yes
   *
   * @param pTypeSize Size of the type the new local variable in bits.
   * @param pVarName Name of the local variable
   * @return {@link SMGState} with the new variables searchable by the name given.
   * @throws SMGException thrown if the stack frame is empty.
   */
  public SMGState copyAndAddLocalVariable(BigInteger pTypeSize, String pVarName, CType type)
      throws SMGException {
    return copyAndAddLocalVariable(pTypeSize, pVarName, type, false);
  }

  /**
   * Copy SMGState with a newly created object with the size given and put it into the current stack
   * frame. If there is no stack frame this throws an exception!
   *
   * <p>Keeps consistency: yes
   *
   * @param pTypeSize Size of the type the new local variable in bits.
   * @param pVarName Name of the local variable
   * @param exceptionOnRead throws an exception if this object is ever read
   * @return {@link SMGState} with the new variables searchable by the name given.
   * @throws SMGException thrown if the stack frame is empty.
   */
  public SMGState copyAndAddLocalVariable(
      BigInteger pTypeSize, String pVarName, CType type, boolean exceptionOnRead)
      throws SMGException {
    if (memoryModel.getStackFrames().isEmpty()) {
      throw new SMGException(
          "Can't add a variable named "
              + pVarName
              + " to the memory model because there is no stack frame.");
    }
    SMGObject newObject = SMGObject.of(0, pTypeSize, BigInteger.ZERO);
    return copyAndReplaceMemoryModel(
        memoryModel.copyAndAddStackObject(newObject, pVarName, type, exceptionOnRead));
  }

  private SMGState copyAndAddLocalVariableToSpecificStackframe(
      String functionNameForStackFrame, BigInteger pTypeSize, String pVarName, CType type)
      throws SMGException {
    if (memoryModel.getStackFrames().isEmpty()) {
      throw new SMGException(
          "Can't add a variable named "
              + pVarName
              + " to the memory model because there is no stack frame.");
    }
    SMGObject newObject = SMGObject.of(0, pTypeSize, BigInteger.ZERO);
    return copyAndReplaceMemoryModel(
        memoryModel.copyAndAddStackObjectToSpecificStackFrame(
            functionNameForStackFrame, newObject, pVarName, type));
  }

  /**
   * Returns true if there exists a variable on the stack with the name entered.
   *
   * @param pState state to check the memory model for.
   * @param variableName name of the variable.
   * @return true if the variable exists, false otherwise.
   */
  public boolean checkVariableExists(SMGState pState, String variableName) {
    return pState.getMemoryModel().getObjectForVisibleVariable(variableName).isPresent();
  }

  /**
   * Copy SMGState and adds a new frame for the function.
   *
   * <p>Keeps consistency: yes
   *
   * @param pFunctionDefinition A function for which to create a new stack frame
   */
  public SMGState copyAndAddStackFrame(CFunctionDeclaration pFunctionDefinition) {
    return copyAndAddStackFrame(pFunctionDefinition, null);
  }

  /**
   * Copy SMGState and adds a new frame for the function. Also saves the variable arguments of this
   * function. Null as argument means no variable arguments. The list of variable arguments may be
   * empty if var args are possible but not used.
   *
   * @param pFunctionDefinition A function for which to create a new stack frame
   */
  public SMGState copyAndAddStackFrame(
      CFunctionDeclaration pFunctionDefinition,
      @Nullable ImmutableList<Value> variableArgumentsInOrder) {
    return copyAndReplaceMemoryModel(
        memoryModel.copyAndAddStackFrame(
            pFunctionDefinition, machineModel, variableArgumentsInOrder));
  }

  @Override
  public String toDOTLabel() {
    StringBuilder sb = new StringBuilder();

    sb.append("[");
    Joiner.on(", ")
        .withKeyValueSeparator("=")
        .appendTo(sb, getMemoryModel().getMemoryLocationsAndValuesForSPCWithoutHeap());
    sb.append("]");

    return sb.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public String getCPAName() {
    return "SMGCPA";
  }

  /*
   * Join 2 SMGStates and as a consequence its SMGs as far as possible.
   */
  @Override
  public SMGState join(SMGState pOther) throws CPAException, InterruptedException {
    SMGJoinSPC joinSPC = new SMGJoinSPC(memoryModel, pOther.memoryModel);
    if (!(joinSPC.getStatus() == SMGJoinStatus.INCOMPARABLE && joinSPC.isDefined())) {
      return pOther;
    }
    // return new SMGState(machineModel, joinSPC.getResult(), logger, options);
    return this;
  }

  private boolean checkErrorEqualityForTwoStates(SMGState pOther) {
    if (!errorInfo.isEmpty()) {
      // As long as the other has at least once the same type of error its fine
      ImmutableSet<Property> otherSetOfPropertyViolations =
          pOther.errorInfo.stream()
              .map(SMGErrorInfo::getPropertyViolated)
              .collect(ImmutableSet.toImmutableSet());
      if (!errorInfo.stream()
          .map(SMGErrorInfo::getPropertyViolated)
          .allMatch(otherSetOfPropertyViolations::contains)) {
        return false;
      }
    }
    return true;
  }

  private boolean checkStackFrameEqualityForTwoStates(
      SMGState pOther, EqualityCache<Value> equalityCache) {
    Iterator<CFunctionDeclarationAndOptionalValue> thisStackFrames =
        memoryModel.getFunctionDeclarationsFromStackFrames().iterator();
    Iterator<CFunctionDeclarationAndOptionalValue> otherStackFrames =
        pOther.memoryModel.getFunctionDeclarationsFromStackFrames().iterator();
    while (otherStackFrames.hasNext()) {
      if (!thisStackFrames.hasNext()) {
        return false;
      }
      CFunctionDeclarationAndOptionalValue thisFrame = thisStackFrames.next();
      CFunctionDeclarationAndOptionalValue otherFrame = otherStackFrames.next();
      if (otherFrame.hasReturnValue()) {
        Value otherRetVal = otherFrame.getReturnValue();
        if (!thisFrame.hasReturnValue()) {
          return false;
        }
        Value thisRetVal = thisFrame.getReturnValue();
        // TODO: overapproximation is OK! We can accept that a concrete value is covered by a
        // overapproximation
        if (!areValuesEqual(this, thisRetVal, pOther, otherRetVal, equalityCache)) {
          return false;
        }
      } else {
        if (thisFrame.hasReturnValue()) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean checkEqualityOfMemoryForTwoStates(
      SMGState pOther, EqualityCache<Value> equalityCache) {
    // We check the tolerant way; i.e. ignore all type information
    // Get all (global and local) variables
    PersistentMap<MemoryLocation, ValueAndValueSize> thisAllMemLocAndValues =
        memoryModel.getMemoryLocationsAndValuesForSPCWithoutHeap();
    for (Entry<MemoryLocation, ValueAndValueSize> otherMemLocAndValue :
        pOther.memoryModel.getMemoryLocationsAndValuesForSPCWithoutHeap().entrySet()) {
      MemoryLocation otherMemLoc = otherMemLocAndValue.getKey();
      Value otherValue = otherMemLocAndValue.getValue().getValue();
      ValueAndValueSize thisValueAndType = thisAllMemLocAndValues.get(otherMemLoc);

      // Now check the equality of all values. For concrete values, we allow overapproximations.
      // Pointers/memory is compared by shape, subsumtion is allowed for equal linked lists, such
      // that the smaller subsumes the larger (5+ >= 6+)
      if (thisValueAndType == null
          || !areValuesEqual(
              this, thisValueAndType.getValue(), pOther, otherValue, equalityCache)) {
        return false;
      }
      // Remove the checked values (don't double-check later)
      thisAllMemLocAndValues = thisAllMemLocAndValues.removeAndCopy(otherMemLoc);
    }
    // Now check the remaining values. We don't allow the merging/subsumption of states if one has
    // pointers/heap and the other doesn't. The rest is covered by overapproximations.
    for (Entry<MemoryLocation, ValueAndValueSize> remainingThisEntry :
        thisAllMemLocAndValues.entrySet()) {
      Value otherValue = remainingThisEntry.getValue().getValue();
      if (memoryModel.isPointer(otherValue)) {
        return false;
      }
    }

    // Check that there is no memory left that is not present in the other state
    if (thisAllMemLocAndValues.size() > 0) {
      return false;
    }
    return true;
  }

  @Override
  public boolean isLessOrEqual(SMGState pOther) throws CPAException, InterruptedException {
    // This state needs the same amount of variables as the other state
    if (getSize() != pOther.getSize()) {
      return false;
    }

    if (!pOther.constraintsState.containsAll(constraintsState)) {
      return false;
    }

    // We may not forget any errors already found
    if (!copyAndPruneUnreachable()
        .checkErrorEqualityForTwoStates(pOther.copyAndPruneUnreachable())) {
      return false;
    }

    // Cache equalities that we already found
    EqualityCache<Value> equalityCache = EqualityCache.of();
    // Check that both have the same stack frames
    if (!checkStackFrameEqualityForTwoStates(pOther, equalityCache)) {
      return false;
    }

    // Check that the values of all variables (local and global) are either equal or
    // overapproximated and that the memory is equal (such that the shape of memory reachable by
    // pointers is lessOrEqual)
    // Validity is checked while checking values and the shape!
    // There might linger some invalidated memory with no connection and that's fine.
    return checkEqualityOfMemoryForTwoStates(pOther, equalityCache);
  }

  /**
   * Check the equality of values. Depending on the options symbolics are always equal or only for
   * ids. Addresses are compared by the shape of their memory. This includes validity of the memory.
   * Public for tests only!
   *
   * @param thisState state of thisValue
   * @param thisValue value in thisState
   * @param otherState state for otherValue
   * @param otherValue value in otherState
   * @param equalityCache current cache of values that are known to be equal
   * @return true if the entered values are equal.
   */
  public boolean areValuesEqual(
      SMGState thisState,
      @Nullable Value thisValue,
      SMGState otherState,
      @Nullable Value otherValue,
      EqualityCache<Value> equalityCache) {
    return areValuesEqual(
        thisState, thisValue, otherState, otherValue, equalityCache, new HashSet<>());
  }

  /**
   * Check the equality of values. Depending on the options symbolics are always equal or only for
   * ids. Addresses are compared by the shape of their memory. This includes validity of the memory.
   * Public for tests only!
   *
   * @param thisState state of thisValue
   * @param thisValue value in thisState
   * @param otherState state for otherValue
   * @param otherValue value in otherState
   * @return true if the entered values are equal.
   */
  public static boolean areValuesEqual(
      SMGState thisState,
      @Nullable Value thisValue,
      SMGState otherState,
      @Nullable Value otherValue) {
    return thisState.areValuesEqual(
        thisState, thisValue, otherState, otherValue, new EqualityCache<>(), new HashSet<>());
  }

  /**
   * Check the equality of values. Depending on the options symbolics are always equal or only for
   * ids. Addresses are compared by the shape of their memory. This includes validity of the memory.
   *
   * @param thisState state of thisValue
   * @param thisValue value in thisState
   * @param otherState state for otherValue
   * @param otherValue value in otherState
   * @param equalityCache current cache of values that are known to be equal
   * @return true if the entered values are equal.
   */
  private boolean areValuesEqual(
      SMGState thisState,
      @Nullable Value thisValue,
      SMGState otherState,
      @Nullable Value otherValue,
      EqualityCache<Value> equalityCache,
      Set<Value> thisAlreadyCheckedPointers) {
    // Comparing pointers leads to == true, but they may be not equal because of the heap!!!
    if (thisValue == otherValue && thisValue.isExplicitlyKnown()) {
      return true;
    }
    if (otherValue == null || thisValue == null) {
      return false;
    }

    if (thisValue.isNumericValue() && otherValue.isNumericValue()) {
      Number thisNum = thisValue.asNumericValue().getNumber();
      Number otherNum = otherValue.asNumericValue().getNumber();
      if (thisNum.getClass() != otherNum.getClass()) {
        return false;
      } else if (thisNum instanceof Float
          && (((Float) thisNum).isNaN() || ((Float) otherNum).isNaN())) {
        return false;
      } else if (thisNum instanceof Double
          && (((Double) thisNum).isNaN() || ((Double) otherNum).isNaN())) {
        return false;
      }
      return thisNum.equals(otherNum);
    }

    // Pointers are more difficult, they are represented by a SymbolicIdentifier, again unique
    // id. We need to use the CPA method
    if (memoryModel.isPointer(thisValue) && otherState.memoryModel.isPointer(otherValue)) {
      if (equalityCache.isEqualityKnown(thisValue, otherValue)) {
        return true;
      } else if (thisAlreadyCheckedPointers.contains(thisValue)) {
        equalityCache.addEquality(thisValue, otherValue);
        return true;
      } else {
        // Pointers can be cyclic! We remember already checked values.
        thisAlreadyCheckedPointers.add(thisValue);
      }
      if (isHeapEqualForTwoPointersWithTwoStates(
          thisState,
          thisValue,
          otherState,
          otherValue,
          equalityCache,
          thisAlreadyCheckedPointers)) {
        equalityCache.addEquality(thisValue, otherValue);
        return true;
      }
      return false;
    }

    // Unknowns in this current CPA implementation are not comparable in different states!
    // Each state generates a unique ConstantSymbolicExpression id (as its statically generated)
    // Comparable is only that both are ConstantSymbolicExpressions and the type matches and
    // that they do represent the same location
    if (thisValue instanceof SymbolicExpression
        && otherValue instanceof SymbolicExpression
        && ((SymbolicExpression) thisValue)
            .getType()
            .equals(((SymbolicExpression) otherValue).getType())) {
      if (options.isTreatSymbolicValuesAsUnknown()) {
        return true;
      } else {
        return thisValue.equals(otherValue);
      }
    }

    return thisValue.equals(otherValue);
  }

  /* Check heap equality as far as possible. This has some limitations.
   * We just check the shape and known values/pointers and validity. */

  /**
   * Check heap equality as far as possible for 2 pointers. We just check the shape and known
   * values/pointers and validity.
   *
   * @param thisState {@link SMGState} for thisAddress
   * @param thisAddress pointer in thisState
   * @param otherState {@link SMGState} for otherAddress
   * @param otherAddress pointer in otherState
   * @param equalityCache current {@link EqualityCache}
   * @param thisAlreadyCheckedPointers already checked pointers (can be expected to be equal)
   * @return false if not equal. True else.
   */
  private boolean isHeapEqualForTwoPointersWithTwoStates(
      SMGState thisState,
      Value thisAddress,
      SMGState otherState,
      Value otherAddress,
      EqualityCache<Value> equalityCache,
      Set<Value> thisAlreadyCheckedPointers) {
    // Careful, dereference might materialize new memory out of abstractions!
    Optional<SMGStateAndOptionalSMGObjectAndOffset> thisDeref =
        thisState.dereferencePointerWithoutMaterilization(thisAddress);
    Optional<SMGStateAndOptionalSMGObjectAndOffset> otherDeref =
        otherState.dereferencePointerWithoutMaterilization(otherAddress);

    if (thisDeref.isPresent() && otherDeref.isPresent()) {
      SMGStateAndOptionalSMGObjectAndOffset thisDerefObjAndOffset = thisDeref.orElseThrow();
      SMGStateAndOptionalSMGObjectAndOffset otherDerefObjAndOffset = otherDeref.orElseThrow();
      thisState = thisDerefObjAndOffset.getSMGState();
      otherState = otherDerefObjAndOffset.getSMGState();
      SMGObject thisObj = thisDerefObjAndOffset.getSMGObject();
      SMGObject otherObj = otherDerefObjAndOffset.getSMGObject();

      if ((getMemoryModel().isObjectValid(thisObj)
              && !otherState.getMemoryModel().isObjectValid(otherObj))
          || (!getMemoryModel().isObjectValid(thisObj)
              && otherState.getMemoryModel().isObjectValid(otherObj))) {
        // One invalid, one valid
        return false;
      }

      Value thisDerefOffset = thisDerefObjAndOffset.getOffsetForObject();
      Value otherDerefOffset = otherDerefObjAndOffset.getOffsetForObject();
      if (!thisDerefOffset.equals(otherDerefOffset)) {
        return false;
      } else if (!thisState
          .memoryModel
          .getPointerSpecifier(thisAddress)
          .equals(otherState.memoryModel.getPointerSpecifier(otherAddress))) {
        return false;
      } else if (!(thisObj.getSize().compareTo(otherObj.getSize()) == 0
          && thisObj.getNestingLevel() == otherObj.getNestingLevel()
          && thisObj.getOffset().compareTo(otherObj.getOffset()) == 0)) {
        return false;
      }

      if (thisObj instanceof SMGSinglyLinkedListSegment
          || otherObj instanceof SMGSinglyLinkedListSegment) {
        return checkAbstractedListEquality(
            thisState, thisObj, otherState, otherObj, equalityCache, thisAlreadyCheckedPointers);
      }

      if (!getMemoryModel().isObjectValid(thisObj)
          && !otherState.getMemoryModel().isObjectValid(otherObj)) {
        // both invalid (we checked sizes etc. already)
        return true;
      }

      return checkEqualValuesForTwoStatesWithExemptions(
          thisObj,
          otherObj,
          ImmutableList.of(),
          thisState,
          otherState,
          equalityCache,
          thisAlreadyCheckedPointers);
    }
    return false;
  }

  /*
   * Checks equality of 2 objects of which at least 1 is an abstracted list.
   */
  private boolean checkAbstractedListEquality(
      SMGState thisState,
      SMGObject thisObj,
      SMGState otherState,
      SMGObject otherObj,
      EqualityCache<Value> equalityCache,
      Set<Value> thisPointerValueAlreadyVisited) {

    // If one is DLL and the other is SLL, something is wrong
    if ((otherObj instanceof SMGDoublyLinkedListSegment
            && !(thisObj instanceof SMGDoublyLinkedListSegment)
            && thisObj.isSLL())
        || (thisObj instanceof SMGDoublyLinkedListSegment
            && !(otherObj instanceof SMGDoublyLinkedListSegment)
            && otherObj.isSLL())) {
      return false;
    }

    if (otherObj instanceof SMGSinglyLinkedListSegment otherSLL
        && thisObj instanceof SMGSinglyLinkedListSegment thisSLL) {
      if (thisSLL.getMinLength() >= otherSLL.getMinLength()
          && thisSLL.getNextOffset().compareTo(otherSLL.getNextOffset()) == 0
          && thisSLL.getHeadOffset().compareTo(otherSLL.getHeadOffset()) == 0) {

        if (otherObj instanceof SMGDoublyLinkedListSegment otherDLL
            && thisObj instanceof SMGDoublyLinkedListSegment thisDLL) {
          if (thisDLL.getPrevOffset().compareTo(otherDLL.getPrevOffset()) != 0) {
            // Check that the values are equal and that the back pointer is as well
            return false;
          }
        }
        // Check that the values are equal and that the next and back pointers are as well
        return checkEqualValuesForTwoStatesWithExemptions(
            thisSLL,
            otherSLL,
            ImmutableList.of(),
            thisState,
            otherState,
            equalityCache,
            thisPointerValueAlreadyVisited);
      }
    } else {
      // Don't check for equality of abstracted and concrete lists for lessOrEqual!
      return false;
    }

    return false;
  }

  // Saved for later usage; State comparison
  @SuppressWarnings("unused")
  private boolean abstractedAndConcreteListEquality(
      SMGState thisState,
      Value thisAddress,
      SMGObject thisObj,
      SMGState otherState,
      Value otherAddress,
      SMGObject otherObj,
      EqualityCache<Value> equalityCache,
      Set<Value> thisPointerValuesAlreadyVisited)
      throws SMGException {
    // one is an abstracted list, the other is not, we check this by materializing the abstracted
    // as long as the concrete allows
    if (thisObj instanceof SMGSinglyLinkedListSegment thisSLL) {
      if (thisSLL.getMinLength() <= 1) {
        // TODO: merge with the case below (important: don't switch this and other!!!)
        // For == 1 the next pointer might not be correct as the list materializes for reads as
        // well
        // hence why we need <= 1
        // We create more than 1 state in those cases by reading the next pointer
        // One extends the list, the other does not, only 1 has to be equal
        List<SMGStateAndOptionalSMGObjectAndOffset> derefs =
            thisState.dereferencePointer(thisAddress);
        for (SMGStateAndOptionalSMGObjectAndOffset deref : derefs) {
          List<ValueAndSMGState> readStatesAndUseless =
              deref
                  .getSMGState()
                  .readValue(
                      deref.getSMGObject(),
                      thisSLL.getNextOffset(),
                      memoryModel.getSizeOfPointer(),
                      null);

          for (ValueAndSMGState stateAndUseless : readStatesAndUseless) {
            if (stateAndUseless
                .getState()
                .areValuesEqual(
                    stateAndUseless.getState(),
                    thisAddress,
                    otherState,
                    otherAddress,
                    equalityCache,
                    thisPointerValuesAlreadyVisited)) {
              equalityCache.addEquality(thisAddress, otherAddress);
              return true;
            }
          }
        }

      } else {

        // This list only ever has 1 element
        for (SMGStateAndOptionalSMGObjectAndOffset deref :
            thisState.dereferencePointer(thisAddress)) {
          // At least one of deref has to be true
          return deref
              .getSMGState()
              .areValuesEqual(
                  deref.getSMGState(),
                  thisAddress,
                  otherState,
                  otherAddress,
                  equalityCache,
                  thisPointerValuesAlreadyVisited);
        }
      }
    }

    if (otherObj instanceof SMGSinglyLinkedListSegment otherSLL) {
      if (otherSLL.getMinLength() <= 1) {
        // For == 1 the next pointer might not be correct as the list materializes for reads as
        // well
        // hence why we need <= 1
        // At least one of deref has to be true
        // Check the stop case first (nfo == otherAddress) (cheaper)
        List<SMGStateAndOptionalSMGObjectAndOffset> derefs =
            otherState.dereferencePointer(otherAddress);
        for (SMGStateAndOptionalSMGObjectAndOffset deref : derefs) {
          List<ValueAndSMGState> readStatesAndUseless =
              deref
                  .getSMGState()
                  .readValue(
                      deref.getSMGObject(),
                      otherSLL.getNextOffset(),
                      memoryModel.getSizeOfPointer(),
                      null,
                      options.isPreciseSMGRead());

          for (ValueAndSMGState stateAndUseless : readStatesAndUseless) {
            if (stateAndUseless
                .getState()
                .areValuesEqual(
                    thisState,
                    thisAddress,
                    stateAndUseless.getState(),
                    otherAddress,
                    equalityCache,
                    thisPointerValuesAlreadyVisited)) {
              equalityCache.addEquality(thisAddress, otherAddress);
              return true;
            }
          }
        }
      } else {
        // this has only 1 list element always
        for (SMGStateAndOptionalSMGObjectAndOffset deref :
            otherState.dereferencePointer(otherAddress)) {
          // At least one of deref has to be true
          return deref
              .getSMGState()
              .areValuesEqual(
                  thisState,
                  thisAddress,
                  deref.getSMGState(),
                  otherAddress,
                  equalityCache,
                  thisPointerValuesAlreadyVisited);
        }
      }
    }
    return false;
  }

  /**
   * Compare 2 values, but do not compare the exempt offsets. Compares pointers by shape of the
   * memory they point to. Needed for lists and their next/prev pointers.
   *
   * @param thisObject object of the this state to compare.
   * @param otherObject object of the other state to compare.
   * @param exemptOffsets exempt offsets, e.g. nfo, pfo offsets.
   * @param thisState the state to which the this object belongs.
   * @param otherState the state to which the other object belongs.
   * @param equalityCache basic value check cache.
   * @return true if the 2 memory sections given are equal. False else.
   * @throws SMGException for critical errors.
   */
  public boolean checkEqualValuesForTwoStatesWithExemptions(
      SMGObject thisObject,
      SMGObject otherObject,
      ImmutableList<BigInteger> exemptOffsets,
      SMGState thisState,
      SMGState otherState,
      EqualityCache<Value> equalityCache)
      throws SMGException {
    return checkEqualValuesForTwoStatesWithExemptions(
        thisObject,
        otherObject,
        exemptOffsets,
        thisState,
        otherState,
        equalityCache,
        new HashSet<>());
  }

  /**
   * Compare 2 values, but do not compare the exempt offsets. Compares pointers by shape of the
   * memory they point to. Needed for lists and their next/prev pointers.
   *
   * @param thisObject object of the this state to compare.
   * @param otherObject object of the other state to compare.
   * @param exemptOffsets exempt offsets, e.g. nfo, pfo offsets.
   * @param thisState the state to which the this object belongs.
   * @param otherState the state to which the other object belongs.
   * @param equalityCache basic value check cache.
   * @return true if the 2 memory sections given are equal. False else.
   */
  private boolean checkEqualValuesForTwoStatesWithExemptions(
      SMGObject thisObject,
      SMGObject otherObject,
      ImmutableList<BigInteger> exemptOffsets,
      SMGState thisState,
      SMGState otherState,
      EqualityCache<Value> equalityCache,
      Set<Value> thisPointerValuesAlreadyVisited) {

    Map<BigInteger, SMGHasValueEdge> otherOffsetToHVEdgeMap = new HashMap<>();
    for (SMGHasValueEdge hve :
        otherState
            .memoryModel
            .getSmg()
            .getSMGObjectsWithSMGHasValueEdges()
            .getOrDefault(otherObject, PersistentSet.of())) {
      if (!exemptOffsets.contains(hve.getOffset())) {
        otherOffsetToHVEdgeMap.put(hve.getOffset(), hve);
      }
    }

    Map<BigInteger, SMGHasValueEdge> thisOffsetToHVEdgeMap = new HashMap<>();

    for (SMGHasValueEdge hve :
        thisState
            .memoryModel
            .getSmg()
            .getSMGObjectsWithSMGHasValueEdges()
            .getOrDefault(thisObject, PersistentSet.of())) {
      if (!exemptOffsets.contains(hve.getOffset())) {
        thisOffsetToHVEdgeMap.put(hve.getOffset(), hve);
        if (memoryModel.getSmg().isPointer(hve.hasValue())) {
          // Pointers are necessary!!!!
          if (otherOffsetToHVEdgeMap.get(hve.getOffset()) == null) {
            return false;
          }
        }
      }
    }

    for (Entry<BigInteger, SMGHasValueEdge> otherHVEAndOffset : otherOffsetToHVEdgeMap.entrySet()) {
      BigInteger otherOffset = otherHVEAndOffset.getKey();
      SMGHasValueEdge otherHVE = otherHVEAndOffset.getValue();
      SMGHasValueEdge thisHVE = thisOffsetToHVEdgeMap.get(otherOffset);
      if (thisHVE == null || thisHVE.getSizeInBits().compareTo(otherHVE.getSizeInBits()) != 0) {
        return false;
      }
      // Check the Value (not the SMGValue!). If a SMGValue exists, a Value mapping exists.
      Value otherHVEValue =
          otherState.memoryModel.getValueFromSMGValue(otherHVE.hasValue()).orElseThrow();
      Value thisHVEValue =
          thisState.memoryModel.getValueFromSMGValue(thisHVE.hasValue()).orElseThrow();

      // These values are either numeric, pointer or unknown
      if (!areValuesEqual(
          thisState,
          thisHVEValue,
          otherState,
          otherHVEValue,
          equalityCache,
          thisPointerValuesAlreadyVisited)) {
        return false;
      }
      // They are equal, we don't need to check it again later
      thisOffsetToHVEdgeMap.remove(otherOffset);
    }
    // At this point we know that from the perspective of other, this is equal or greater
    // Now we need to know if it is reverse also
    for (Entry<BigInteger, SMGHasValueEdge> thisHVEAndOffset : thisOffsetToHVEdgeMap.entrySet()) {
      BigInteger thisOffset = thisHVEAndOffset.getKey();
      SMGHasValueEdge thisHVE = thisHVEAndOffset.getValue();
      SMGHasValueEdge otherHVE = otherOffsetToHVEdgeMap.get(thisOffset);
      if (otherHVE == null || thisHVE.getSizeInBits().compareTo(otherHVE.getSizeInBits()) != 0) {
        return false;
      }
      // Check the Value (not the SMGValue!). If a SMGValue exists, a Value mapping exists.
      Value otherHVEValue =
          otherState.memoryModel.getValueFromSMGValue(otherHVE.hasValue()).orElseThrow();
      Value thisHVEValue =
          thisState.memoryModel.getValueFromSMGValue(thisHVE.hasValue()).orElseThrow();
      // These values are either numeric, pointer or unknown
      // Nothing == symbolic; symbolic != concrete and everything != pointer expect the same
      // pointer!
      if (!areValuesEqual(
          thisState,
          thisHVEValue,
          otherState,
          otherHVEValue,
          equalityCache,
          thisPointerValuesAlreadyVisited)) {
        return false;
      }
      equalityCache.addEquality(thisHVEValue, otherHVEValue);
    }
    return true;
  }

  public boolean hasMemoryErrors() {
    for (SMGErrorInfo info : errorInfo) {
      if (info.hasMemoryErrors()) {
        return true;
      }
    }
    return false;
  }

  /*
   * Check non-equality of the 2 entered potential addresses. Never use == or equals on addresses!
   * Tries to prove the not equality of two given addresses. Returns true if the prove of
   * not equality succeeded, returns false if both are potentially equal.
   * This method expects the Values to be the actual addresses and NOT AddressExpressions!
   */
  public boolean areNonEqualAddresses(Value pValue1, Value pValue2) {
    Optional<SMGValue> smgValue1 = memoryModel.getSMGValueFromValue(pValue1);
    Optional<SMGValue> smgValue2 = memoryModel.getSMGValueFromValue(pValue2);
    if (smgValue1.isEmpty() || smgValue2.isEmpty()) {
      // The return value should not matter here as this is checked before
      return true;
    }
    return memoryModel.proveInequality(smgValue1.orElseThrow(), smgValue2.orElseThrow());
  }

  /**
   * True iff both values are pointers pointing to the same memory region. Offset does not matter!
   *
   * @param pValue1 a pointer {@link Value}
   * @param pValue2 a pointer {@link Value}
   * @return true iff both point to the same memory region.
   */
  public boolean pointsToSameMemoryRegion(Value pValue1, Value pValue2) {
    if (!memoryModel.isPointer(pValue1) || !memoryModel.isPointer(pValue2)) {
      return false;
    }

    Optional<SMGValue> maybeSmgValue1 = memoryModel.getSMGValueFromValue(pValue1);
    Optional<SMGValue> maybeSmgValue2 = memoryModel.getSMGValueFromValue(pValue2);
    if (maybeSmgValue1.isEmpty() || maybeSmgValue2.isEmpty()) {
      return false;
    }

    SMGValue smgValue1 = maybeSmgValue1.orElseThrow();
    SMGValue smgValue2 = maybeSmgValue2.orElseThrow();

    SMGPointsToEdge targetEdge1 = memoryModel.getSmg().getPTEdge(smgValue1).orElseThrow();
    SMGPointsToEdge targetEdge2 = memoryModel.getSmg().getPTEdge(smgValue2).orElseThrow();
    if (targetEdge1.pointsTo().equals(targetEdge2.pointsTo())) {
      return true;
    }
    return false;
  }

  /**
   * Returns the offset of a pointer in relation to the beginning of a memory region. Or UNKNOWN if
   * some error happens.
   *
   * @param pValue some {@link Value} that may be a pointer.
   * @return UNKNOWN or a {@link NumericValue} that is the offset.
   */
  public Value getPointerOffset(Value pValue) {
    if (!memoryModel.isPointer(pValue)) {
      return UnknownValue.getInstance();
    }

    Optional<SMGValue> maybeSmgValue1 = memoryModel.getSMGValueFromValue(pValue);
    if (maybeSmgValue1.isEmpty()) {
      return UnknownValue.getInstance();
    }

    SMGValue smgValue = maybeSmgValue1.orElseThrow();
    SMGPointsToEdge targetEdge = memoryModel.getSmg().getPTEdge(smgValue).orElseThrow();
    return new NumericValue(targetEdge.getOffset());
  }

  /** Logs the error entered using the states logger. */
  private void logMemoryError(String pMessage, boolean pUndefinedBehavior) {
    if (options.isMemoryErrorTarget()) {
      logger.log(Level.FINE, pMessage);
    } else if (pUndefinedBehavior) {
      logger.log(Level.FINE, pMessage);
      logger.log(
          Level.FINE,
          "Non-target undefined behavior detected. The verification result is unreliable.");
    }
  }

  // Only public for builtin functions
  public SMGState copyAndPruneFunctionStackVariable(String variableName) {
    return copyAndReplaceMemoryModel(memoryModel.copyAndRemoveStackVariable(variableName));
  }

  public SMGState dropStackFrame() {
    return copyAndReplaceMemoryModel(memoryModel.copyAndDropStackFrame());
  }

  /*
   * Copy the current state and prune all unreachable SMGObjects.
   * Used for example after a function return to prune out of scope memory.
   * This also detects memory leaks and updates the error state if one is found!
   */
  public SMGState copyAndPruneUnreachable() {
    SPCAndSMGObjects newHeapAndUnreachables = memoryModel.copyAndPruneUnreachable();
    SymbolicProgramConfiguration newHeap = newHeapAndUnreachables.getSPC();
    Collection<SMGObject> unreachableObjects = newHeapAndUnreachables.getSMGObjects();

    if (unreachableObjects.isEmpty()) {
      return this;
    }

    return copyAndReplaceMemoryModel(newHeap).copyWithMemLeak(unreachableObjects, null);
  }

  public SMGState copyAndPruneUnreachable(CFAEdge edge) {
    SPCAndSMGObjects newHeapAndUnreachables = memoryModel.copyAndPruneUnreachable();
    SymbolicProgramConfiguration newHeap = newHeapAndUnreachables.getSPC();
    Collection<SMGObject> unreachableObjects =
        newHeapAndUnreachables.getSMGObjects().stream()
            .filter(o -> !o.isConstStringMemory())
            .collect(ImmutableList.toImmutableList());

    if (unreachableObjects.isEmpty()) {
      return this;
    }
    for (SMGObject unreachable : unreachableObjects) {
      if (unreachable instanceof SMGSinglyLinkedListSegment sll && sll.getMinLength() == 0) {
        throw new UnsupportedOperationException(
            "Error in tracking abstracted list memory for materialization.");
      }
    }

    return copyAndReplaceMemoryModel(newHeap).copyWithMemLeak(unreachableObjects, edge);
  }

  /*
   * Remove the entered object from the heap and general memory mappings.
   * Also, all has-value-edges are pruned. Nothing else.
   */
  public SMGState copyAndRemoveObjectFromHeap(SMGObject obj) {
    return copyAndReplaceMemoryModel(memoryModel.copyAndRemoveObjectFromHeap(obj));
  }

  /*
   * Copy the state with an error attached. This method is used for memory leaks, meaning it is a
   *  non-fatal error.
   */
  private SMGState copyWithMemLeak(Collection<SMGObject> leakedObjects, @Nullable CFAEdge edge) {

    String leakedObjectsLabels =
        leakedObjects.stream().map(Object::toString).collect(Collectors.joining(","));
    String errorMSG = "Memory leak of " + leakedObjectsLabels + " is detected";
    if (edge != null) {
      errorMSG = errorMSG + " in line " + edge.getFileLocation().getStartingLineInOrigin();
    }
    errorMSG = errorMSG + ".";
    SMGErrorInfo newErrorInfo =
        SMGErrorInfo.of()
            .withProperty(Property.INVALID_HEAP)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(leakedObjects);
    logMemoryError(errorMSG, true);
    return copyWithNewErrorInfo(newErrorInfo);
  }

  // TODO: invalid read/write because of invalidated object/memory

  /**
   * Use of an variable that was not initialized. The value will be unknown, but generally
   * undefined.
   *
   * @param uninitializedVariableName the {@link String} that is not initialized.
   * @return A new {@link SMGState} with the error info.
   */
  public SMGState withUninitializedVariableUsage(String uninitializedVariableName) {
    String errorMSG =
        "Usage of uninitialized variable: "
            + uninitializedVariableName
            + ". A unknown value was assumed, but behavior is of this variable is generally"
            + " undefined.";
    SMGErrorInfo newErrorInfo =
        SMGErrorInfo.of()
            .withProperty(Property.INVALID_READ)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(uninitializedVariableName));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithNewErrorInfo(newErrorInfo);
  }

  /**
   * Error for dereferencing unknown pointer {@link Value} when reading. I.e. int bla = *value; with
   * value being unknown.
   *
   * @param unknownAddress the {@link Value} that is unknown to the memory model and was tried to be
   *     dereferenced.
   * @return A new {@link SMGState} with the error info.
   */
  public SMGState withUnknownPointerDereferenceWhenReading(Value unknownAddress) {
    String errorMSG = "Unknown value pointer dereference for value: " + unknownAddress + ".";
    SMGErrorInfo newErrorInfo =
        SMGErrorInfo.of()
            .withProperty(Property.INVALID_READ)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(unknownAddress));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithNewErrorInfo(newErrorInfo);
  }

  public SMGState withUnknownPointerDereferenceWhenReading(Value unknownAddress, CFAEdge edge) {
    String errorMSG =
        "Unknown value pointer dereference for value: " + unknownAddress + " at " + edge;
    SMGErrorInfo newErrorInfo =
        SMGErrorInfo.of()
            .withProperty(Property.INVALID_READ)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(unknownAddress));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithNewErrorInfo(newErrorInfo);
  }

  /**
   * Error for dereferencing unknown pointer {@link Value} when reading with intent to write. I.e.
   * *value = 3; with value being unknown.
   *
   * @param unknownAddress the {@link Value} that is unknown to the memory model and was tried to be
   *     dereferenced.
   * @return A new {@link SMGState} with the error info.
   */
  public SMGState withUnknownPointerDereferenceWhenWriting(Value unknownAddress) {
    String errorMSG =
        "Unknown value pointer dereference with intent to write to it for value: "
            + unknownAddress
            + ".";
    SMGErrorInfo newErrorInfo =
        SMGErrorInfo.of()
            .withProperty(Property.INVALID_READ)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(unknownAddress));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithNewErrorInfo(newErrorInfo);
  }

  /**
   * Error for trying to write to a local/global variable that is not declared.
   *
   * @param variableName the variable name of the variable that was tried to write to.
   * @return A new {@link SMGState} with the error info.
   */
  public SMGState withWriteToUnknownVariable(String variableName) {
    String errorMSG = "Failed write to variable " + variableName + ".";
    SMGErrorInfo newErrorInfo =
        SMGErrorInfo.of()
            .withProperty(Property.INVALID_WRITE)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(variableName));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithNewErrorInfo(newErrorInfo);
  }

  /**
   * The error sais invalid read as the point that fails is the read of the {@link SMGObject} before
   * writing! I.e. *pointer = ...; With pointer failing to dereference because its pointing to 0.
   *
   * @param nullObject the {@link SMGObject} that is null and was tried to be dereferenced.
   * @return A new SMGState with the error info.
   */
  public SMGState withNullPointerDereferenceWhenWriting(SMGObject nullObject) {
    // Get the SMGValue and Value that lead to this null pointer dereference
    String errorMSG =
        "Null pointer dereference on read of object with the intent to write to: "
            + nullObject
            + ".";
    SMGErrorInfo newErrorInfo =
        SMGErrorInfo.of()
            .withProperty(Property.INVALID_READ)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(nullObject));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithNewErrorInfo(newErrorInfo);
  }

  /**
   * Copy the state with a memory leak error set.
   *
   * @param errorMsg custom error message specific to the error reason.
   * @param pUnreachableObjects the object at fault.
   * @return a copy of the current state with the error info added.
   */
  public SMGState withMemoryLeak(String errorMsg, Collection<Object> pUnreachableObjects) {
    // TODO: replace Object; currently it is only used by Value (address to SMGObject)
    SMGErrorInfo newErrorInfo =
        SMGErrorInfo.of()
            .withProperty(Property.INVALID_HEAP)
            .withErrorMessage(errorMsg)
            .withInvalidObjects(pUnreachableObjects);
    // Log the error in the logger
    logMemoryError(errorMsg, true);
    return copyWithNewErrorInfo(newErrorInfo);
  }

  /**
   * Invalid write to a not initialized, unknown or non-existing or beyond the boundries of a memory
   * region.
   *
   * @param invalidAddress the invalid address pointing to nothing.
   * @return A new SMGState with the error info.
   */
  public SMGState withInvalidWrite(Value invalidAddress) {
    String errorMSG =
        "Write to invalid, unknown or non-existing memory region, pointed to by: "
            + invalidAddress
            + ".";
    SMGErrorInfo newErrorInfo =
        SMGErrorInfo.of()
            .withProperty(Property.INVALID_WRITE)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(invalidAddress));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithNewErrorInfo(newErrorInfo);
  }

  /**
   * Invalid write to a not initialized, unknown or non-existing or beyond the boundries of a memory
   * region.
   *
   * @param invalidWriteRegion the invalid address pointing to nothing.
   * @return A new SMGState with the error info.
   */
  public SMGState withInvalidWrite(SMGObject invalidWriteRegion) {
    String errorMSG =
        "Write to invalid, unknown or non-existing or beyond the boundries of a memory region: "
            + invalidWriteRegion
            + ".";
    SMGErrorInfo newErrorInfo =
        SMGErrorInfo.of()
            .withProperty(Property.INVALID_WRITE)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(invalidWriteRegion));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithNewErrorInfo(newErrorInfo);
  }

  /**
   * Invalid write to 0. (0 SMGObject)
   *
   * @param invalidWriteRegion the invalid address pointing to nothing.
   * @return A new SMGState with the error info.
   */
  public SMGState withInvalidWriteToZeroObject(SMGObject invalidWriteRegion) {
    String errorMSG = "Write to invalid memory region: NULL.";
    SMGErrorInfo newErrorInfo =
        SMGErrorInfo.of()
            .withProperty(Property.INVALID_WRITE)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(invalidWriteRegion));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithNewErrorInfo(newErrorInfo);
  }

  /**
   * Invalid write with custom error msg.
   *
   * @param invalidValue the invalid value. Either address or write value or something like a size
   *     specifier etc.
   * @return A new SMGState with the error info.
   */
  public SMGState withInvalidWrite(String errorMSG, Value invalidValue) {
    SMGErrorInfo newErrorInfo =
        SMGErrorInfo.of()
            .withProperty(Property.INVALID_WRITE)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(invalidValue));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithNewErrorInfo(newErrorInfo);
  }

  /**
   * I.e. int bla = *pointer; With pointer failing to dereference because its pointing to 0.
   *
   * @param nullObject the {@link SMGObject} that is null and was tried to be dereferenced.
   * @return A new SMGState with the error info.
   */
  public SMGState withNullPointerDereferenceWhenReading(SMGObject nullObject) {
    // getValueForSMGValue(pValue)
    // Get the SMGValue and Value that lead to this null pointer dereference
    String errorMSG =
        "Null pointer dereference on read of object with the intent to read it: "
            + nullObject
            + ".";
    SMGErrorInfo newErrorInfo =
        SMGErrorInfo.of()
            .withProperty(Property.INVALID_READ)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(nullObject));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithNewErrorInfo(newErrorInfo);
  }

  /**
   * I.e. int bla = blub; With blub not existing or having no memory.
   *
   * @param readVariable the variable that was tried to be read.
   * @return A new SMGState with the error info.
   */
  public SMGState withInvalidStackVariableRead(String readVariable) {
    String errorMSG = "Invalid read of variable named: " + readVariable + ".";
    SMGErrorInfo newErrorInfo =
        SMGErrorInfo.of()
            .withProperty(Property.INVALID_READ)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(readVariable));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithNewErrorInfo(newErrorInfo);
  }

  /**
   * General read outside of memory boundries.
   *
   * @param readMemory the memory {@link SMGObject} that was tried to be read.
   * @return A new SMGState with the error info.
   */
  public SMGState withInvalidRead(SMGObject readMemory) {
    String errorMSG = "Invalid read of memory object: " + readMemory + ".";
    SMGErrorInfo newErrorInfo =
        SMGErrorInfo.of()
            .withProperty(Property.INVALID_READ)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(readMemory));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithNewErrorInfo(newErrorInfo);
  }

  /**
   * Copy and update this {@link SMGState} with an error resulting from trying to write outside of
   * the range of the {@link SMGObject}. Returns an updated state with the error in it.
   *
   * @param objectWrittenTo the {@link SMGObject} that should have been written to.
   * @param writeOffset The offset in bits where you want to write the {@link Value} to.
   * @param writeSize the size of the {@link Value} in bits.
   * @param pValue the {@link Value} you wanted to write.
   * @return A new SMGState with the error info.
   */
  public SMGState withOutOfRangeWrite(
      SMGObject objectWrittenTo,
      BigInteger writeOffset,
      BigInteger writeSize,
      Value pValue,
      CFAEdge edge) {

    if (getMemoryModel().isHeapObject(objectWrittenTo)) {
      // Invalid deref
      return withInvalidDeref(objectWrittenTo, edge);
    }

    int lineInOrigin = edge.getFileLocation().getStartingLineInOrigin();
    String errorMSG =
        String.format(
            "Try writing value %s with size %d at offset %d bit to object sized %d bit in line %d.",
            pValue, writeSize, writeOffset, objectWrittenTo.getSize(), lineInOrigin);
    SMGErrorInfo newErrorInfo =
        SMGErrorInfo.of()
            .withProperty(Property.INVALID_WRITE)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(objectWrittenTo));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithNewErrorInfo(newErrorInfo);
  }

  /**
   * Copy and update this {@link SMGState} with an error resulting from trying to write outside of
   * the range of the {@link SMGObject}. Returns an updated state with the error in it.
   *
   * @param objectWrittenTo the {@link SMGObject} that should have been written to.
   * @param writeOffset The offset in bits where you want to write the {@link Value} to.
   * @param writeSize the size of the {@link Value} in bits.
   * @param pValue the {@link Value} you wanted to write.
   * @return A new SMGState with the error info.
   */
  public SMGState withOutOfRangeWrite(
      SMGObject objectWrittenTo,
      Value writeOffset,
      BigInteger writeSize,
      Value pValue,
      CFAEdge edge) {

    if (writeOffset.isNumericValue()) {
      return withOutOfRangeWrite(
          objectWrittenTo, writeOffset.asNumericValue().bigIntegerValue(), writeSize, pValue, edge);
    }

    // TODO: get model for offset
    int lineInOrigin = edge.getFileLocation().getStartingLineInOrigin();
    String errorMSG =
        String.format(
            "Try writing value %s with size %d at unknown possible offset bit to object sized %d"
                + " bit in line %d.",
            pValue, writeSize, objectWrittenTo.getSize(), lineInOrigin);
    SMGErrorInfo newErrorInfo =
        SMGErrorInfo.of()
            .withProperty(Property.INVALID_WRITE)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(objectWrittenTo));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithNewErrorInfo(newErrorInfo);
  }

  private SMGState withInvalidDeref(SMGObject objectDerefed, CFAEdge edge) {
    Preconditions.checkArgument(
        getMemoryModel().isHeapObject(objectDerefed)
            || !getMemoryModel().isObjectValid(objectDerefed));

    int lineInOrigin = edge.getFileLocation().getStartingLineInOrigin();
    String errorMSG =
        String.format(
            "valid-deref: invalid pointer dereference in line %d with: " + edge, lineInOrigin);
    SMGErrorInfo newErrorInfo =
        SMGErrorInfo.of()
            .withProperty(Property.INVALID_HEAP)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(objectDerefed));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithNewErrorInfo(newErrorInfo);
  }

  /**
   * Copy and update this {@link SMGState} with an error resulting from trying to read outside of
   * the range of the {@link SMGObject}. Returns an updated state with the error in it.
   *
   * @param objectRead the {@link SMGObject} that should have been read.
   * @param readOffset The offset in bits as {@link BigInteger} where you want to read.
   * @param readSize the size of the type in bits to read as {@link BigInteger}.
   * @return A new SMGState with the error info.
   */
  public SMGState withOutOfRangeRead(
      SMGObject objectRead, BigInteger readOffset, BigInteger readSize) {
    String errorMSG =
        String.format(
            "Try reading object %s with size %d bits at offset %d bit with read type size %d bit",
            objectRead, objectRead.getSize(), readOffset, readSize);
    SMGErrorInfo newErrorInfo =
        SMGErrorInfo.of()
            .withProperty(Property.INVALID_READ)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(objectRead));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithNewErrorInfo(newErrorInfo);
  }

  /**
   * Copy and update this {@link SMGState} with an error resulting from trying to read outside of
   * the range of the {@link SMGObject}. Returns an updated state with the error in it.
   *
   * @param objectRead the {@link SMGObject} that should have been read.
   * @param readOffset The offset in bits as {@link Value} where you want to read. Might be
   *     symbolic!
   * @param readSize the size of the type in bits to read as {@link BigInteger}.
   * @return A new SMGState with the error info.
   */
  public SMGState withOutOfRangeRead(SMGObject objectRead, Value readOffset, BigInteger readSize) {
    // TODO: extract model for readOffset and print here
    String errorMSG =
        String.format(
            "Try reading object %s with size %d bits at offset %s bit with read type size %d bit",
            objectRead, objectRead.getSize(), readOffset, readSize);
    SMGErrorInfo newErrorInfo =
        SMGErrorInfo.of()
            .withProperty(Property.INVALID_READ)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(objectRead));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithNewErrorInfo(newErrorInfo);
  }

  public SMGState withUnknownOffsetMemoryAccess() {
    String errorMSG =
        "Memory access with an invalid or unknown offset detected. This might be the result of"
            + " an overapproximation and might be a false positive.";
    SMGErrorInfo newErrorInfo =
        SMGErrorInfo.of()
            .withProperty(Property.INVALID_READ)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(ImmutableSet.of());
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithNewErrorInfo(newErrorInfo);
  }

  /**
   * Log undefined behavior.
   *
   * @param errorMSG custom error msg.
   * @param reason the reasons for the undefined behavior. I.e. invalid memcpy pointers.
   * @return a new state with the error attached.
   */
  public SMGState withUndefinedbehavior(String errorMSG, Collection<Object> reason) {
    SMGErrorInfo newErrorInfo =
        SMGErrorInfo.of()
            .withProperty(Property.UNDEFINED_BEHAVIOR)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(reason);
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithNewErrorInfo(newErrorInfo);
  }

  /**
   * Copy and update this {@link SMGState} with an error resulting from trying to free a address
   * {@link Value} invalidly. Returns an updated state with the error in it.
   *
   * @param errorMSG the error message.
   * @param invalidValue the {@link Value} that was invalidly freed.
   * @return A new SMGState with the error info.
   */
  public SMGState withInvalidFree(String errorMSG, Value invalidValue) {
    SMGErrorInfo newErrorInfo =
        SMGErrorInfo.of()
            .withProperty(Property.INVALID_FREE)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(invalidValue));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithNewErrorInfo(newErrorInfo);
  }

  /**
   * Returns a copy of this {@link SMGState} with the entered SPC and a new {@link SMGErrorInfo}
   * added.
   *
   * @param pErrorInfo The new {@link SMGErrorInfo} tied to the returned state.
   * @return a copy of the {@link SMGState} this is based on with the newly entered SPC and error
   *     info.
   */
  public SMGState copyWithNewErrorInfo(SMGErrorInfo pErrorInfo) {
    return copyWithNewErrorInfo(
        new ImmutableList.Builder<SMGErrorInfo>().addAll(errorInfo).add(pErrorInfo).build());
  }

  private SMGState copyWithNewErrorInfo(ImmutableList<SMGErrorInfo> pNewErrorInfo) {
    return new SMGState(
        machineModel,
        memoryModel,
        logger,
        options,
        pNewErrorInfo,
        materializer,
        lastCheckedMemoryAccess,
        constraintsState,
        evaluator);
  }

  /** Returns memory model, including Heap, stack and global vars. */
  public SymbolicProgramConfiguration getMemoryModel() {
    return memoryModel;
  }

  /**
   * Add the {@link Value} mapping if it was not mapped to a {@link SMGValue}, if it was already
   * present the state is unchanged and the known {@link SMGValue} returned. The {@link SMGValue} is
   * not added to the SPC yet, writeValue() will do that.
   *
   * @param pValue the {@link Value} you want to add to the SPC.
   * @return a copy of the current {@link SMGState} with the mapping of the {@link Value} to its
   *     {@link SMGValue} entered if it was not mapped, if it was already present the state is
   *     unchanged and the known {@link SMGValue} returned.
   */
  public SMGValueAndSMGState copyAndAddValue(Value pValue) {
    Optional<SMGValue> maybeValue = memoryModel.getSMGValueFromValue(pValue);
    if (maybeValue.isPresent()) {
      return SMGValueAndSMGState.of(this, maybeValue.orElseThrow());
    } else {
      SMGValue newSMGValue = SMGValue.of();
      return SMGValueAndSMGState.of(
          copyAndReplaceMemoryModel(memoryModel.copyAndPutValue(pValue, newSMGValue)), newSMGValue);
    }
  }

  public List<SMGErrorInfo> getErrorInfo() {
    return errorInfo;
  }

  /**
   * Determines the SMGRegion object which is pointed by a given Value address representation.
   * Return Null SMGObject if there is no such existing address. (will result in null deref later,
   * but not here!) TODO: do we need unknown derefs here?
   *
   * @param pValue - the given Value representation of the address.
   * @return the SMGObject which the address points to, or empty if none is found.
   */
  public Optional<SMGObjectAndOffset> getPointsToTarget(Value pValue) {
    Optional<SMGValue> addressOptional = memoryModel.getSMGValueFromValue(pValue);
    if (addressOptional.isPresent()) {
      Optional<SMGPointsToEdge> pointerEdgeOptional =
          memoryModel.getSmg().getPTEdge(addressOptional.orElseThrow());
      if (pointerEdgeOptional.isPresent()) {
        return Optional.of(
            SMGObjectAndOffset.of(
                pointerEdgeOptional.orElseThrow().pointsTo(),
                new NumericValue(pointerEdgeOptional.orElseThrow().getOffset())));
      }
    }
    return Optional.empty();
  }

  /*
   * Transforms any AddressExpression into a new AddressExpression but with offset 0 and
   * a potentially new memory location (pointer) with the offset incorporated. Reuses
   * existing pointers before creating new ones. Always returns the entered value for
   * offset == 0. But unknown for unknown offsets.
   */
  private ValueAndSMGState searchOrCreateAddressForAddressExpr(Value pValue) {
    if (pValue instanceof AddressExpression addressExprValue) {
      Value offsetAddr = addressExprValue.getOffset();
      if (offsetAddr.isNumericValue()) {
        BigInteger offsetAddrBI = offsetAddr.asNumericValue().bigIntegerValue();
        if (offsetAddrBI.compareTo(BigInteger.ZERO) != 0) {
          Optional<SMGObjectAndOffset> maybeTargetAndOffset =
              getPointsToTarget(addressExprValue.getMemoryAddress());
          if (maybeTargetAndOffset.isEmpty()) {
            return ValueAndSMGState.ofUnknownValue(this);
          }
          SMGObjectAndOffset targetAndOffset = maybeTargetAndOffset.orElseThrow();

          SMGObject target = targetAndOffset.getSMGObject();
          Value offsetPointer = targetAndOffset.getOffsetForObject();
          if (!offsetPointer.isNumericValue()) {
            return ValueAndSMGState.ofUnknownValue(this);
          }
          BigInteger offsetOverall =
              offsetPointer.asNumericValue().bigIntegerValue().add(offsetAddrBI);
          // search for existing pointer first and return if found; else make a new one
          ValueAndSMGState addressAndState = searchOrCreateAddress(target, offsetOverall);
          return ValueAndSMGState.of(
              AddressExpression.withZeroOffset(
                  addressAndState.getValue(), addressExprValue.getType()),
              addressAndState.getState());
        }
      } else {
        return ValueAndSMGState.ofUnknownValue(this);
      }
    }
    return ValueAndSMGState.of(pValue, this);
  }

  /**
   * Takes a target and offset and tries to find a address (not AddressExpression) that fits them.
   * If none can be found a new address (SMGPointsToEdge) is created and returned as Value (Not
   * AddressExpression).
   *
   * @param targetObject {@link SMGObject} target.
   * @param offsetInBits Offset as BigInt.
   * @return a {@link Value} (NOT AddressExpression) and state with the address/address added.
   */
  public ValueAndSMGState searchOrCreateAddress(SMGObject targetObject, BigInteger offsetInBits) {
    // search for existing pointer first and return if found
    Optional<SMGValue> maybeAddressValue =
        getMemoryModel().getAddressValueForPointsToTarget(targetObject, offsetInBits);

    if (maybeAddressValue.isPresent()) {
      Optional<Value> valueForSMGValue =
          getMemoryModel().getValueFromSMGValue(maybeAddressValue.orElseThrow());
      // Reuse pointer; there should never be a SMGValue without counterpart!
      // TODO: this might actually be expensive, check once this runs!
      return ValueAndSMGState.of(valueForSMGValue.orElseThrow(), this);
    }

    Value addressValue = SymbolicValueFactory.getInstance().newIdentifier(null);
    SMGState newState = createAndAddPointer(addressValue, targetObject, offsetInBits);
    return ValueAndSMGState.of(addressValue, newState);
  }

  /**
   * Read the value in the {@link SMGObject} at the position specified by the offset and size.
   * Checks for validity of the object and if its externally allocated and may fail because of that.
   * The read {@link SMGValue} will be translated into a {@link Value}. If the Value is known, the
   * known value is used, unknown symbolic else. Might materialize a list if an abstracted 0+ list
   * is read.
   *
   * @param pObject {@link SMGObject} where to read. May not be 0.
   * @param pFieldOffset {@link BigInteger} offset.
   * @param pSizeofInBits {@link BigInteger} sizeInBits.
   * @param readType the {@link CType} of the read. Not cast! Null for irrelevant types.
   * @return The {@link Value} read and the {@link SMGState} after the read.
   * @throws SMGException for critical errors if a list is materialized.
   */
  public List<ValueAndSMGState> readValue(
      SMGObject pObject,
      BigInteger pFieldOffset,
      BigInteger pSizeofInBits,
      @Nullable CType readType)
      throws SMGException {
    return readValue(pObject, pFieldOffset, pSizeofInBits, readType, options.isPreciseSMGRead());
  }

  /**
   * Read the value in the {@link SMGObject} at the position specified by the offset and size.
   * Checks for validity of the object and if its externally allocated and may fail because of that.
   * The read {@link SMGValue} will be translated into a {@link Value}. If the Value is known, the
   * known value is used, unknown symbolic else. Might materialize a list if an abstracted 0+ list
   * is read.
   *
   * @param pObject {@link SMGObject} where to read. May not be 0.
   * @param pFieldOffset {@link BigInteger} offset.
   * @param pSizeofInBits {@link BigInteger} sizeInBits.
   * @param readType the {@link CType} of the read. Not cast! Null for irrelevant types.
   * @return The {@link Value} read and the {@link SMGState} after the read.
   * @throws SMGException for critical errors if a list is materialized.
   */
  public List<ValueAndSMGState> readValue(
      SMGObject pObject,
      BigInteger pFieldOffset,
      BigInteger pSizeofInBits,
      @Nullable CType readType,
      boolean preciseRead)
      throws SMGException {
    if (!memoryModel.isObjectValid(pObject) && !memoryModel.isObjectExternallyAllocated(pObject)) {
      return ImmutableList.of(
          ValueAndSMGState.of(UnknownValue.getInstance(), withInvalidRead(pObject)));
    }
    SMGHasValueEdgesAndSPC valueAndNewSPC =
        memoryModel.readValue(pObject, pFieldOffset, pSizeofInBits, preciseRead);
    // Try to translate the SMGValue to a Value or create a new mapping (the same read on the same
    // object/offset/size yields the same SMGValue, so it should return the same Value)
    SMGState currentState = copyAndReplaceMemoryModel(valueAndNewSPC.getSPC());
    // Only use the new state for changes!
    // Only 1 for now, change once we support more
    if (valueAndNewSPC.getSMGHasValueEdges().size() > 1) {
      // TODO: implement all cases
      logger.log(
          Level.FINE,
          "Failed to accurately read type "
              + readType
              + " from memory, as multiple values would need to be combined. The analysis defaulted"
              + " back to symbolic read.");
      return readValue(pObject, pFieldOffset, pSizeofInBits, readType, false);
    }
    SMGHasValueEdge readSMGValueEdge = valueAndNewSPC.getSMGHasValueEdges().get(0);
    boolean exactRead =
        readSMGValueEdge.getOffset().equals(pFieldOffset)
            && readSMGValueEdge.getSizeInBits().equals(pSizeofInBits);
    SMGValue readSMGValue = readSMGValueEdge.hasValue();
    ImmutableList.Builder<ValueAndSMGState> returnBuilder = ImmutableList.builder();
    if (memoryModel.getSmg().isPointer(readSMGValue)
        && memoryModel.getSmg().pointsToZeroPlus(readSMGValue)
        && exactRead) {
      // 0+ needs Materialization as we generate 2 states, one of which deleted the 0+, and for this
      // state the read value is wrong!
      for (SMGStateAndOptionalSMGObjectAndOffset newState :
          materializeLinkedList(
              readSMGValue,
              memoryModel.getSmg().getPTEdge(readSMGValue).orElseThrow(),
              currentState)) {
        // This is expected not to Materialize again
        List<ValueAndSMGState> readAfterMat =
            newState.getSMGState().readValue(pObject, pFieldOffset, pSizeofInBits, readType, false);
        Preconditions.checkArgument(readAfterMat.size() == 1);
        returnBuilder.addAll(readAfterMat);
      }
    } else {
      Optional<Value> maybeValue = getMemoryModel().getValueFromSMGValue(readSMGValue);
      if (!exactRead) {
        if (maybeValue.isEmpty()) {
          return readValue(pObject, pFieldOffset, pSizeofInBits, readType, false);
        } else {
          // Interpret the larger value as a smaller
          // TODO: general case with overlapping values
          Value valueInterpretation =
              currentState.transformSingleHVEdgeToTargetValue(
                  readSMGValueEdge, pFieldOffset, pSizeofInBits);
          if (valueInterpretation.isUnknown()) {
            logger.log(
                Level.FINE,
                "Failed to accurately read type "
                    + readType
                    + " from memory and default back to symbolic read.");
            return readValue(pObject, pFieldOffset, pSizeofInBits, readType, false);
          }
          returnBuilder.add(ValueAndSMGState.of(valueInterpretation, currentState));
          return returnBuilder.build();
        }
      }
      returnBuilder.add(currentState.handleReadSMGValue(readSMGValue, readType));
    }
    return returnBuilder.build();
  }

  // Similar to readValue but without materialization. For internal == comparisons.
  public ValueAndSMGState readValueWithoutMaterialization(
      SMGObject pObject,
      BigInteger pFieldOffset,
      BigInteger pSizeofInBits,
      @Nullable CType readType)
      throws SMGException {
    if (!memoryModel.isObjectValid(pObject) && !memoryModel.isObjectExternallyAllocated(pObject)) {
      return ValueAndSMGState.of(UnknownValue.getInstance(), withInvalidRead(pObject));
    }
    // TODO: it is a assumption that we don't need precise reads here, as the top value needs to be
    // the same?
    SMGHasValueEdgesAndSPC valueAndNewSPC =
        memoryModel.readValue(pObject, pFieldOffset, pSizeofInBits, false);
    // Try to translate the SMGValue to a Value or create a new mapping (the same read on the same
    // object/offset/size yields the same SMGValue, so it should return the same Value)
    SMGState currentState = copyAndReplaceMemoryModel(valueAndNewSPC.getSPC());
    // Only use the new state for changes!
    assert valueAndNewSPC.getSMGHasValueEdges().get(0).getOffset().equals(pFieldOffset)
        && valueAndNewSPC.getSMGHasValueEdges().get(0).getSizeInBits().equals(pSizeofInBits);
    SMGValue readSMGValue = valueAndNewSPC.getSMGHasValueEdges().get(0).hasValue();

    return currentState.handleReadSMGValue(readSMGValue, readType);
  }

  // Expects an exact read
  private ValueAndSMGState handleReadSMGValue(SMGValue readSMGValue, @Nullable CType readType) {
    Optional<Value> maybeValue = getMemoryModel().getValueFromSMGValue(readSMGValue);
    if (maybeValue.isPresent()) {
      // The Value to the SMGValue is already known, use it
      Preconditions.checkArgument(!(maybeValue.orElseThrow() instanceof AddressExpression));
      Value valueRead = maybeValue.orElseThrow();
      if (readType != null && doesRequireUnionFloatConversion(valueRead, readType)) {
        // Float conversion is limited to the Java float types at the moment.
        // Larger float types are almost always unknown
        valueRead = castValueForUnionFloatConversion(valueRead, readType);
      }
      return ValueAndSMGState.of(valueRead, this);

    } else {
      // If there is no Value for the SMGValue, we need to create it as an unknown, map it and
      // return
      Value unknownValue = getNewSymbolicValueForType(readType);
      return ValueAndSMGState.of(
          unknownValue,
          copyAndReplaceMemoryModel(getMemoryModel().copyAndPutValue(unknownValue, readSMGValue)));
    }
  }

  // Expects a (single) read SMGHasValueEdge that was not exact (to the offset/size read) and needs
  // to be cut to size
  private Value transformSingleHVEdgeToTargetValue(
      SMGHasValueEdge readSMGHVValue, BigInteger readOffset, BigInteger readSizeInBits) {
    Value value = getMemoryModel().getValueFromSMGValue(readSMGHVValue.hasValue()).orElseThrow();
    int shiftRight;
    if (machineModel.getEndianness().equals(ByteOrder.LITTLE_ENDIAN)) {
      // Little Endian = Least significant value is stored first
      shiftRight = readOffset.intValueExact() - readSMGHVValue.getOffset().intValueExact();
      if (shiftRight < 0) {
        // Read larger edge on smaller, not supported
        return UnknownValue.getInstance();
      }
      assert shiftRight >= 0;
      assert shiftRight <= readSMGHVValue.getSizeInBits().intValueExact();
    } else {
      // Big Endian = Most significant value is stored first in big endian
      // example: 00000001 is 1
      int offsetDif = readOffset.intValueExact() - readSMGHVValue.getOffset().intValueExact();
      Preconditions.checkArgument(offsetDif >= 0);
      shiftRight =
          readSMGHVValue
              .getOffset()
              .add(readSMGHVValue.getSizeInBits())
              .subtract(readOffset.add(readSizeInBits))
              .intValueExact();
      assert readSMGHVValue.getOffset().add(readSMGHVValue.getSizeInBits()).intValueExact()
          >= readOffset.add(readSizeInBits).intValueExact();
      assert machineModel.getEndianness().equals(ByteOrder.BIG_ENDIAN);
    }

    // We have a partial read of the value given. We can just shift the value a few times until only
    // the relevant bits are left.
    if (value.isNumericValue()) {
      if (value.asNumericValue().bigIntegerValue().compareTo(BigInteger.valueOf(Long.MAX_VALUE))
              <= 0
          && value.asNumericValue().bigIntegerValue().compareTo(BigInteger.valueOf(Long.MIN_VALUE))
              >= 0) {
        // long
        long longValue = value.asNumericValue().bigIntegerValue().longValueExact();
        long mask = getMask(readSizeInBits);
        return new NumericValue(BigInteger.valueOf(((longValue >>> shiftRight) & mask)));

      } else {
        // larger than long
        // TODO: can we handle this in Java?

      }
    } else if (!value.isUnknown()) {
      // Some symbolic value. Wrap in symbolic shift operations
      // TODO:
      String msg =
          "Partial read of symbolic value detected. Overapproximated due to missing"
              + " implementation.";
      logger.log(Level.INFO, msg);
      throw new UnsupportedOperationException(
          "Symbolic handling of partial reads are not supported at the moment.");
      // SymbolicValueFactory vF = SymbolicValueFactory.getInstance();
      // asConstant needed?
      /*
      return vF.binaryAnd(vF.shiftRightUnsigned(value, new NumericValue(shiftRight), , ),
              new NumericValue(getMask(readSizeInBits), , ));
              */

      // TODO: As we are interpreting this as BV, we could use extract()
    }

    // Fallthrough. Unknown value -> unknown value
    return UnknownValue.getInstance();
  }

  private static long getMask(BigInteger readSizeInBits) {
    int readSize = readSizeInBits.intValueExact();
    long mask;
    if (readSize == 1) {
      mask = 1;
    } else if (readSize == 2) {
      mask = 3;
    } else if (readSize == 4) {
      mask = 0x0000000F;
    } else if (readSize == 8) {
      mask = 0x000000FF;
    } else if (readSize == 16) {
      mask = 0x0000FFFF;
    } else if (readSize == 32) {
      mask = 0xFFFFFFFF;
    } else {
      assert readSize == 64;
      mask = Long.MAX_VALUE;
    }
    return mask;
  }

  /*
   * Only to be used by abstraction and tests! This will not materialize anything!
   */
  public SMGValueAndSMGState readSMGValue(
      SMGObject pObject, BigInteger pFieldOffset, BigInteger pSizeofInBits) throws SMGException {
    SMGHasValueEdgesAndSPC valueAndNewSPC =
        memoryModel.readValue(pObject, pFieldOffset, pSizeofInBits, false);
    SMGState newState = copyAndReplaceMemoryModel(valueAndNewSPC.getSPC());
    SMGValue readSMGValue = valueAndNewSPC.getSMGHasValueEdges().get(0).hasValue();
    // This might create SMGValues without Value counterparts as part of this read (the abstraction
    // might have deleted a previous value)
    if (newState.memoryModel.getValueFromSMGValue(readSMGValue).isEmpty()) {
      // The type should always be a pointer
      CType pointerType = CPointerType.POINTER_TO_VOID;
      BigInteger sizeOfPointer = machineModel.getSizeofInBits(pointerType);
      Preconditions.checkArgument(sizeOfPointer.equals(pSizeofInBits));
      Value unknownValue = getNewSymbolicValueForType(pointerType);
      return SMGValueAndSMGState.of(
          copyAndReplaceMemoryModel(
              newState.getMemoryModel().copyAndPutValue(unknownValue, readSMGValue)),
          readSMGValue);
    }
    return SMGValueAndSMGState.of(newState, readSMGValue);
  }

  private boolean doesRequireUnionFloatConversion(Value valueRead, CType readType) {
    if (!valueRead.isNumericValue()) {
      return false;
    }
    if (readType instanceof CSimpleType) {
      // if only one of them is no integer type, a conversion is necessary
      return isFloatingPointType(valueRead) != isFloatingPointType(readType);
    } else {
      return false;
    }
  }

  private boolean isFloatingPointType(CType pType) {
    return pType instanceof CSimpleType && ((CSimpleType) pType).getType().isFloatingPointType();
  }

  private boolean isFloatingPointType(Value value) {
    if (!value.isNumericValue()) {
      return false;
    }
    Number num = value.asNumericValue().getNumber();
    return num instanceof Float || num instanceof Double || num == NegativeNaN.VALUE;
  }

  /**
   * The only important thing is that the expectedType is NOT the left hand side type or any cast
   * type, but the type of the read before any casts etc.! *
   */
  private Value castValueForUnionFloatConversion(Value readValue, CType expectedType) {
    if (readValue.isNumericValue()) {
      if (isFloatingPointType(readValue)) {
        return extractFloatingPointValueAsIntegralValue(readValue);
      } else if (isFloatingPointType(expectedType.getCanonicalType())
          && !isFloatingPointType(readValue)) {
        return extractIntegralValueAsFloatingPointValue(expectedType.getCanonicalType(), readValue);
      } else {
        return readValue;
      }
    }

    return UnknownValue.getInstance();
  }

  private Value extractFloatingPointValueAsIntegralValue(Value readValue) {
    Number numberValue = readValue.asNumericValue().getNumber();

    if (numberValue instanceof Float) {
      float floatValue = numberValue.floatValue();
      int intBits = Float.floatToIntBits(floatValue);

      return new NumericValue(BigInteger.valueOf(intBits));
    } else if (numberValue instanceof Double) {
      double doubleValue = numberValue.doubleValue();
      long longBits = Double.doubleToLongBits(doubleValue);

      return new NumericValue(BigInteger.valueOf(longBits));
    }

    return UnknownValue.getInstance();
  }

  private Value extractIntegralValueAsFloatingPointValue(CType pReadType, Value readValue) {
    if (pReadType instanceof CSimpleType) {
      CBasicType basicReadType = ((CSimpleType) pReadType.getCanonicalType()).getType();
      NumericValue numericValue = readValue.asNumericValue();

      if (basicReadType.equals(CBasicType.FLOAT)) {
        int bits = numericValue.bigIntegerValue().intValue();
        float floatValue = Float.intBitsToFloat(bits);

        return new NumericValue(floatValue);
      } else if (basicReadType.equals(CBasicType.DOUBLE)) {
        long bits = numericValue.bigIntegerValue().longValue();
        double doubleValue = Double.longBitsToDouble(bits);

        return new NumericValue(doubleValue);
      }
    }
    return UnknownValue.getInstance();
  }

  /**
   * This performs a call to free(addressToFree) with addressToFree being a {@link Value} that
   * should be a address to a memory region, but can be any Value. This method determines if the
   * valueToFree is a valid, not yet freed address and frees the memory behind it, returning the
   * {@link SMGState} with the freed memory. It might however return a state with an error info
   * attached, for example double free. In case of a null-pointer being freed, the logger logs the
   * event without errors.
   *
   * @param addressToFree any {@link Value} thought to be a pointer to a memory region, but it may
   *     be not. It might be a {@link AddressExpression} as well.
   * @param pFunctionCall debug / logging info.
   * @param cfaEdge debug / logging info.
   * @return a new {@link SMGState} with the memory region behind the {@link Value} freed.
   * @throws SMGException in case of critical errors in the concretization of memory.
   */
  public List<SMGState> free(
      Value addressToFree, CFunctionCallExpression pFunctionCall, CFAEdge cfaEdge)
      throws SMGException {
    Value sanitizedAddressToFree = addressToFree;
    BigInteger baseOffset = BigInteger.ZERO;
    // if the entered value is a AddressExpression think of it as a internal wrapper of pointer +
    // offset. We use the value as pointer and then add the offset to the found offset! If however
    // the offset is non numeric we can't calculate if the free is valid or not.
    if (addressToFree instanceof AddressExpression addressExpr) {
      // We just disassamble the AddressExpression and use it as if it were a normal pointer
      sanitizedAddressToFree = addressExpr.getMemoryAddress();

      if (!addressExpr.getOffset().isNumericValue()) {
        // return a freed and an unfreed state for not numeric values
        return ImmutableList.of(
            this,
            this.withInvalidFree("Invalid free of unallocated object is found.", addressToFree));
      }
      baseOffset = addressExpr.getOffset().asNumericValue().bigIntegerValue();
    }

    // Value == 0 can happen by user input and is valid!
    if (sanitizedAddressToFree.isNumericValue()
        && sanitizedAddressToFree.asNumericValue().bigIntegerValue().compareTo(BigInteger.ZERO)
            == 0) {
      logger.log(
          Level.FINE,
          pFunctionCall.getFileLocation(),
          ":",
          "The argument of a free invocation:",
          cfaEdge.getRawStatement(),
          "is 0");
      return ImmutableList.of(this);
    }

    ImmutableList.Builder<SMGState> returnBuilder = ImmutableList.builder();
    for (SMGStateAndOptionalSMGObjectAndOffset maybeRegion :
        dereferencePointer(sanitizedAddressToFree)) {

      if (!maybeRegion.hasSMGObjectAndOffset()) {
        // If there is no region the deref failed, which means we can't evaluate the free
        logger.log(
            Level.FINE,
            "Free on expression ",
            pFunctionCall.getParameterExpressions().get(0).toASTString(),
            " is invalid, because the target of the address could not be calculated.");
        // return maybeRegion.getSMGState();
        returnBuilder.add(
            maybeRegion
                .getSMGState()
                .withInvalidFree("Invalid free of unallocated object is found.", addressToFree));
        continue;
      }
      SMGState currentState = maybeRegion.getSMGState();
      SymbolicProgramConfiguration currentMemModel = currentState.getMemoryModel();
      SMGObject regionToFree = maybeRegion.getSMGObject();
      Value regionOffset = maybeRegion.getOffsetForObject();

      if (!regionOffset.isNumericValue()) {
        // TODO: check that baseOffset == 0 and regionOffset == 0 (or targeting initial pointer)
        // with SMT solver
        returnBuilder.add(
            currentState.withInvalidFree("Invalid free of of object is found.", addressToFree));
        continue;
      }
      BigInteger offsetInBits = baseOffset.add(regionOffset.asNumericValue().bigIntegerValue());

      // free(0) is a nop in C
      if (regionToFree.isZero()) {
        logger.log(
            Level.FINE,
            pFunctionCall.getFileLocation(),
            ":",
            "The argument of a free invocation:",
            cfaEdge.getRawStatement(),
            "is 0");
        returnBuilder.add(currentState);
        continue;
      }

      if (!currentMemModel.isHeapObject(regionToFree)
          && !currentMemModel.isObjectExternallyAllocated(regionToFree)) {
        // You may not free any objects not on the heap.
        // It could be that the object was on the heap but was freed before!
        returnBuilder.add(
            currentState.withInvalidFree(
                "Invalid free of unallocated object is found.", addressToFree));
        continue;
      }

      if (currentMemModel.memoryIsResultOfMallocZero(regionToFree)) {
        // Memory result of malloc(0), validate to free successfully
        currentMemModel = currentMemModel.removeMemoryAsResultOfMallocZero(regionToFree);
        currentMemModel = currentMemModel.validateSMGObject(regionToFree);
        currentState = currentState.copyAndReplaceMemoryModel(currentMemModel);
      }

      if (!currentMemModel.isObjectValid(regionToFree)) {
        // you may not invoke free multiple times on the same object
        returnBuilder.add(
            currentState.withInvalidFree(
                "Free has been used on this memory before.", addressToFree));
        continue;
      }

      if (offsetInBits.compareTo(BigInteger.ZERO) != 0
          && !currentMemModel.isObjectExternallyAllocated(regionToFree)) {
        // you may not invoke free on any address that you
        // didn't get through a malloc, calloc or realloc invocation.
        // (undefined behavour, same as double free)

        returnBuilder.add(
            currentState.withInvalidFree(
                "Invalid free as a pointer was used that was not returned by malloc, calloc or"
                    + " realloc.",
                addressToFree));
        continue;
      }

      // Perform free by invalidating the object behind the address and delete all its edges.
      SymbolicProgramConfiguration newSPC = currentMemModel.invalidateSMGObject(regionToFree);
      // state in our implementation.
      // performConsistencyCheck(SMGRuntimeCheck.HALF);
      returnBuilder.add(currentState.copyAndReplaceMemoryModel(newSPC));
    }
    return returnBuilder.build();
  }

  /**
   * Don't use this method outside of this class or tests! Writes into the given {@link SMGObject}
   * at the specified offset in bits with the size in bits the value given. This method adds the
   * Value <-> SMGValue mapping if none is known, else it uses an existing mapping. This method
   * makes all checks (write to 0, sizes, validity).
   *
   * @param object the memory {@link SMGObject} to write to.
   * @param writeOffsetInBits offset in bits to be written
   * @param sizeInBits size in bits to be written
   * @param valueToWrite the value to write. Is automatically either translated to a known SMGValue
   *     or a new SMGValue is added to the returned state.
   * @param valueType type of the valueToWrite.
   * @param edge {@link CFAEdge} if possible. Null otherwise. (i.e. CEGAR)
   * @return a new SMGState with the value written.
   */
  public SMGState writeValueWithChecks(
      SMGObject object,
      Value writeOffsetInBits,
      BigInteger sizeInBits,
      Value valueToWrite,
      CType valueType,
      @Nullable CFAEdge edge)
      throws SMGSolverException {
    if (object.isZero()) {
      // Write to 0
      return withInvalidWriteToZeroObject(object);
    } else if (!memoryModel.isObjectValid(object)) {
      // Write to an object that is invalidated (already freed)
      // If object part if heap -> invalid deref
      return this.withInvalidWrite(object);
    }
    SMGState currentState = this;
    if (valueToWrite instanceof AddressExpression) {
      ValueAndSMGState valueToWriteAndState = transformAddressExpression(valueToWrite);
      valueToWrite = valueToWriteAndState.getValue();
      currentState = valueToWriteAndState.getState();
    }

    if (valueToWrite.isUnknown()) {
      Preconditions.checkNotNull(valueType);
      valueToWrite = getNewSymbolicValueForType(valueType);
    }

    BigInteger numericOffsetInBits = null;
    if (writeOffsetInBits.isNumericValue()) {
      numericOffsetInBits = writeOffsetInBits.asNumericValue().bigIntegerValue();
      // Check that the target can hold the value
      if (object.getOffset().compareTo(numericOffsetInBits) > 0
          || object.getSize().compareTo(sizeInBits.add(numericOffsetInBits)) < 0) {
        // Out of range write
        // If object part if heap -> invalid deref
        return withOutOfRangeWrite(object, numericOffsetInBits, sizeInBits, valueToWrite, edge);
      }

    } else if (options.trackErrorPredicates()) {
      // Use an SMT solver to argue about the offset/size validity
      final ConstraintFactory constraintFactory =
          ConstraintFactory.getInstance(
              currentState, machineModel, logger, options, evaluator, edge);
      final Collection<Constraint> newConstraints =
          constraintFactory.checkValidMemoryAccess(
              writeOffsetInBits,
              new NumericValue(sizeInBits),
              new NumericValue(object.getSize()),
              CNumericTypes.INT,
              currentState);

      String stackFrameFunctionName = currentState.getStackFrameTopFunctionName();

      // Iff SAT -> memory-safety is violated
      BooleanAndSMGState isUnsatAndState =
          evaluator.checkMemoryConstraintsAreUnsatIndividually(
              newConstraints, stackFrameFunctionName, currentState);
      boolean isUnsat = isUnsatAndState.getBoolean();
      currentState = isUnsatAndState.getState();

      if (!isUnsat) {
        // Unknown value that should not be used with an error state that should stop the analysis
        // Stop the analysis, error found
        currentState =
            currentState.withOutOfRangeWrite(
                object, writeOffsetInBits, sizeInBits, valueToWrite, edge);
      }

      // delete ALL edges in the target region, as they may all be now different
      currentState =
          currentState.copyAndReplaceMemoryModel(
              currentState.memoryModel.copyAndReplaceHVEdgesAt(object, PersistentSet.of()));

      // Either the error is now in the state, or there is no error, but we can't write as we don't
      // know the offset
      return currentState;
    }

    Preconditions.checkArgument(!(valueToWrite instanceof AddressExpression));
    Preconditions.checkNotNull(numericOffsetInBits);
    SMGValueAndSMGState valueAndState = copyAndAddValue(valueToWrite);
    SMGValue smgValue = valueAndState.getSMGValue();
    currentState = valueAndState.getSMGState();
    return currentState.writeValueWithoutChecks(object, numericOffsetInBits, sizeInBits, smgValue);
  }

  /*
   * If you are wondering why there are so many writes;
   * this is to optimize the checks and variableName <-> SMGObject and
   * Value <-> SMGValue mappings. I don't want to do unneeded checks multiple times.
   * This is public only for abstraction and tests!!!!.
   */
  public SMGState writeValueWithoutChecks(
      SMGObject object,
      BigInteger writeOffsetInBits,
      BigInteger sizeInBits,
      SMGValue valueToWrite) {

    Preconditions.checkArgument(memoryModel.isObjectValid(object));
    return copyAndReplaceMemoryModel(
        memoryModel.writeValue(object, writeOffsetInBits, sizeInBits, valueToWrite));
  }

  /**
   * Writes the Value given to the memory reserved for the return statement of a stack frame. Make
   * sure that there is a return object before calling this. This will check sizes before writing
   * and will map the Value to a SMGValue if there is no mapping. This always assumes offset = 0.
   *
   * @param sizeInBits the size of the Value to write in bits.
   * @param valueToWrite the {@link Value} to write.
   * @return a new {@link SMGState} with either an error info in case of an error or the value
   *     written to the return memory.
   */
  public SMGState writeToReturn(
      BigInteger sizeInBits, Value valueToWrite, CType returnValueType, CFAEdge edge)
      throws SMGSolverException {
    SMGObject returnObject = getMemoryModel().getReturnObjectForCurrentStackFrame().orElseThrow();
    if (valueToWrite.isUnknown()) {
      valueToWrite = getNewSymbolicValueForType(returnValueType);
    }
    // Check that the target can hold the value
    if (returnObject.getOffset().compareTo(BigInteger.ZERO) > 0
        || returnObject.getSize().compareTo(sizeInBits) < 0) {
      // Out of range write
      return withOutOfRangeWrite(returnObject, BigInteger.ZERO, sizeInBits, valueToWrite, edge);
    }
    return writeValueWithChecks(
        returnObject,
        new NumericValue(BigInteger.ZERO),
        sizeInBits,
        valueToWrite,
        returnValueType,
        edge);
  }

  /** Writes the value exactly to the size of the return of the current stack frame. */
  private SMGState writeToReturn(Value valueToWrite) throws SMGSolverException {
    SMGObject returnObject = memoryModel.getReturnObjectForCurrentStackFrame().orElseThrow();
    return writeValueWithChecks(
        returnObject,
        new NumericValue(BigInteger.ZERO),
        returnObject.getSize(),
        valueToWrite,
        null,
        null);
  }

  /**
   * Writes the entered {@link Value} to the region that the addressToMemory points to at the
   * specified offset with the specified size both in bits. It can be used for heap and stack, it
   * just assumes that the {@link SMGObject} exist in the SPC, so make sure beforehand! The Value
   * will either add or find its {@link SMGValue} counterpart automatically. Also this checks that
   * the {@link SMGObject} is large enough for the write. If something fails, this throws an
   * exception with an error info inside the state thrown with.
   *
   * @param addressToMemory the {@link Value} representing the address of the region to write to.
   * @param writeOffsetInBits the offset in bits for the write of the value.
   * @param sizeInBits size of the written value in bits.
   * @param valueToWrite {@link Value} that gets written into the SPC. Will be mapped to a {@link
   *     SMGValue} automatically.
   * @param valueType the type of the value to be written. Used for unknown values only, to
   *     translate them into a symbolic value.
   * @return new {@link SMGState} with the value written to the object.
   * @throws SMGException if something goes wrong. I.e. the sizes of the write don't match with the
   *     size of the object.
   */
  public List<SMGState> writeValueTo(
      Value addressToMemory,
      BigInteger writeOffsetInBits,
      BigInteger sizeInBits,
      Value valueToWrite,
      CType valueType,
      CFAEdge edge)
      throws SMGException, SMGSolverException {
    ImmutableList.Builder<SMGState> returnBuilder = ImmutableList.builder();
    for (SMGStateAndOptionalSMGObjectAndOffset maybeRegion : dereferencePointer(addressToMemory)) {
      if (!maybeRegion.hasSMGObjectAndOffset()) {
        // Can't write to non-existing memory. However, we might not track that memory at the
        // moment!
        returnBuilder.add(maybeRegion.getSMGState());
        continue;
      }

      SMGState currentState = maybeRegion.getSMGState();
      SMGObject memoryRegion = maybeRegion.getSMGObject();

      if (!currentState.memoryModel.isObjectValid(memoryRegion)) {
        // The dereference before this detected the error deref at this point, just return the state
        returnBuilder.add(currentState);
        continue;
      }

      // TODO: check if this is truly correct
      Value writeOffset =
          SMGCPAExpressionEvaluator.addOffsetValues(
              maybeRegion.getOffsetForObject(), writeOffsetInBits);

      returnBuilder.add(
          currentState.writeValueWithChecks(
              memoryRegion, writeOffset, sizeInBits, valueToWrite, valueType, edge));
    }
    return returnBuilder.build();
  }

  /**
   * Writes the memory, that is accessed by dereferencing the pointer (address) of the {@link Value}
   * given, completely to 0.
   *
   * @param addressToMemory {@link Value} that is a address pointing to a memory region.
   * @return the new {@link SMGState} with the memory region pointed to by the address written 0
   *     completely.
   * @throws SMGException if there is no memory/or pointer for the given Value.
   */
  public List<SMGState> writeToZero(Value addressToMemory, CType type, CFAEdge edge)
      throws SMGException, SMGSolverException {
    ImmutableList.Builder<SMGState> returnBuilder = ImmutableList.builder();
    for (SMGStateAndOptionalSMGObjectAndOffset maybeRegion : dereferencePointer(addressToMemory)) {
      if (!maybeRegion.hasSMGObjectAndOffset()) {
        // Can't write to non existing memory. However, we might not track that memory at the
        // moment!
        // TODO: log
        returnBuilder.add(maybeRegion.getSMGState());
        continue;
      }

      SMGState currentState = maybeRegion.getSMGState();
      SMGObject memoryRegion = maybeRegion.getSMGObject();
      Preconditions.checkArgument(
          maybeRegion
              .getOffsetForObject()
              .asNumericValue()
              .bigIntegerValue()
              .equals(BigInteger.ZERO));
      returnBuilder.add(
          currentState.writeValueWithChecks(
              memoryRegion,
              maybeRegion.getOffsetForObject(),
              memoryRegion.getSize(),
              new NumericValue(0),
              type,
              edge));
    }
    return returnBuilder.build();
  }

  /**
   * Copies all (complete) has-value-edges from source to target, starting from source offset,
   * targeting target offset, for the size given. This method checks validity/correct sizes etc.
   *
   * @param sourceObject {@link SMGObject} to copy from.
   * @param sourceStartOffset {@link Value} offset to start the copy from the source object. Might
   *     be symbolic.
   * @param targetObject {@link SMGObject} to copy to.
   * @param targetStartOffset {@link Value} offset to start the copy to the source object. Might be
   *     symbolic.
   * @param copySize {@link Value} size of the copy.
   * @return a {@link SMGState} with either an error state in case of errors, a valid state with the
   *     values written, or a state in which all values in the offset range of the target are
   *     deleted, making them unknown, for symbolic values.
   * @throws SMGException in case of critical errors or solver errors. You need to unpack solver
   *     errors!
   */
  public SMGState copySMGObjectContentToSMGObject(
      SMGObject sourceObject,
      Value sourceStartOffset,
      SMGObject targetObject,
      Value targetStartOffset,
      Value copySize)
      throws SMGException {
    if (sourceStartOffset.isNumericValue()
        && targetStartOffset.isNumericValue()
        && copySize.isNumericValue()) {
      BigInteger copySizeInBits = copySize.asNumericValue().bigIntegerValue();
      BigInteger sourceOffset = sourceStartOffset.asNumericValue().bigIntegerValue();
      BigInteger targetOffset = targetStartOffset.asNumericValue().bigIntegerValue();
      // Check that we don't read beyond the source size and don't write beyonde the target size
      // and that we don't start before the object begins
      if (sourceObject.getSize().subtract(sourceOffset).compareTo(copySizeInBits) < 0
          || sourceOffset.compareTo(BigInteger.ZERO) < 0) {
        // This would be an invalid read
        SMGState currentState = this.withInvalidRead(sourceObject);
        if (targetObject.getSize().subtract(targetOffset).compareTo(copySizeInBits) < 0
            || targetOffset.compareTo(BigInteger.ZERO) < 0) {
          // That would be an invalid write
          currentState = currentState.withInvalidWrite(sourceObject);
        }
        return currentState;
      }
      if (targetObject.getSize().subtract(targetOffset).compareTo(copySizeInBits) < 0
          || targetOffset.compareTo(BigInteger.ZERO) < 0) {
        // That would be an invalid write
        return this.withInvalidWrite(sourceObject);
      }
      return copySMGObjectContentToSMGObject(
          sourceObject,
          sourceStartOffset.asNumericValue().bigIntegerValue(),
          targetObject,
          targetStartOffset.asNumericValue().bigIntegerValue(),
          copySizeInBits);
    }
    // Unknown/Symbolic offset Values, we need to check them using a SMT solver
    // TODO:
    if (options.trackErrorPredicates()) {
      // TODO: we can check the ranges etc. symbolically and make all possible targets unknown
      throw new SMGException(
          "Failure to copy a structure based on symbolic offsets or size. Report this case to"
              + " CPAchecker issue tracker please.");
    } else {
      return this.withInvalidWrite(sourceObject);
    }
  }

  /**
   * Copies the content (Values) of the source {@link SMGObject} starting from the sourceOffset into
   * the target {@link SMGObject} starting from the target offset. This copies until the size limit
   * is reached. If an edge starts within the size, but ends outside, it is not copied. This expects
   * that both the source and target exist in the SPC and all checks are made before calling this.
   * These checks should include range checks, overlapping memorys etc.
   *
   * @param sourceObject {@link SMGObject} from which is to be copied.
   * @param sourceStartOffset offset from which the copy is started.
   * @param targetObject target {@link SMGObject}
   * @param targetStartOffset target offset, this is the start of the writes in the target.
   * @param copySizeInBits maximum copied bits. If a edge starts within the accepted range but ends
   *     outside, its not copied.
   * @return {@link SMGState} with the content of the source copied into the target.
   */
  public SMGState copySMGObjectContentToSMGObject(
      SMGObject sourceObject,
      BigInteger sourceStartOffset,
      SMGObject targetObject,
      BigInteger targetStartOffset,
      BigInteger copySizeInBits) {
    SMGState currentState = this;
    BigInteger maxReadOffsetPlusSize = sourceStartOffset.add(copySizeInBits);
    // Removal of edges in the target is not necessary as the write deletes old overlapping edges
    // Get all source edges and copy them
    Set<SMGHasValueEdge> sourceContents = memoryModel.getSmg().getEdges(sourceObject);
    for (SMGHasValueEdge edge : sourceContents) {
      BigInteger edgeOffsetInBits = edge.getOffset();
      BigInteger edgeSizeInBits = edge.getSizeInBits();
      // We only write edges that are >= the beginning offset of the source and edgeOffsetInBits +
      // edgeSizeInBits < sourceStartOffset + copySizeInBits
      if (sourceStartOffset.compareTo(edgeOffsetInBits) <= 0
          && edgeOffsetInBits.add(edgeSizeInBits).compareTo(maxReadOffsetPlusSize) <= 0) {
        // We need to take the targetOffset to source offset difference into account
        BigInteger finalWriteOffsetInBits =
            edgeOffsetInBits.subtract(sourceStartOffset).add(targetStartOffset);
        SMGValue value = edge.hasValue();
        currentState =
            currentState.writeValueWithoutChecks(
                targetObject, finalWriteOffsetInBits, edgeSizeInBits, value);
      }
    }

    return currentState;
  }

  /**
   * Write to a stack (or global) variable with the name given. This method assumes that the
   * variable exists!!!! The offset and size are in bits. The {@link Value} will be added as a
   * {@link SMGValue} mapping if not known.
   *
   * @param variableName name of the variable that should be known already.
   * @param writeOffsetInBits in bits.
   * @param writeSizeInBits in bits.
   * @param valueToWrite {@link Value} to write. If its not yet known as a {@link SMGValue} then the
   *     mapping will be added.
   * @return a {@link SMGState} with the {@link Value} written at the given position in the variable
   *     given.
   * @throws SMGException if the write is out of range or invalid due to the variable being unknown.
   */
  public SMGState writeToStackOrGlobalVariable(
      String variableName,
      Value writeOffsetInBits,
      BigInteger writeSizeInBits,
      Value valueToWrite,
      CType valueType,
      CFAEdge edge)
      throws SMGException, SMGSolverException {
    Optional<SMGObject> maybeVariableMemory =
        getMemoryModel().getObjectForVisibleVariable(variableName);

    if (maybeVariableMemory.isEmpty()) {
      // Write to unknown variable
      return withWriteToUnknownVariable(variableName);
    }

    SMGObject variableMemory = maybeVariableMemory.orElseThrow();
    return writeValueWithChecks(
        variableMemory, writeOffsetInBits, writeSizeInBits, valueToWrite, valueType, edge);
  }

  /* Helper method to reconstruct the state after interpolation. This writes to ANY local variable, independent of stack frame */
  private SMGState writeToAnyStackOrGlobalVariable(
      String variableName,
      BigInteger writeOffsetInBits,
      BigInteger writeSizeInBits,
      Value valueToWrite,
      @Nullable CType valueType)
      throws SMGSolverException {
    // expected to never be empty!
    Optional<SMGObject> maybeVariableMemory = getMemoryModel().getObjectForVariable(variableName);

    SMGObject variableMemory = maybeVariableMemory.orElseThrow();
    // Expected to be always in range
    return writeValueWithChecks(
        variableMemory,
        new NumericValue(writeOffsetInBits),
        writeSizeInBits,
        valueToWrite,
        valueType,
        null);
  }

  /**
   * Transforms the entered Value into a non AddressExpression. If the entered Value is none, the
   * entered Value is returned. If the entered Value is a AddressExpression it is transformed into a
   * single Value representing the complete address (with offset). If the offset is non numeric, a
   * unknown value is returned.
   *
   * @param value might be AddressExpression.
   * @return a non AddressExpression Value.
   */
  ValueAndSMGState transformAddressExpression(Value value) {
    if (value instanceof AddressExpression) {
      ValueAndSMGState valueToWriteAndState = searchOrCreateAddressForAddressExpr(value);
      // The returned Value might be a non AddressExpression
      Value valueToWrite = valueToWriteAndState.getValue();
      SMGState currentState = valueToWriteAndState.getState();
      if (valueToWrite instanceof AddressExpression) {
        Preconditions.checkArgument(
            ((AddressExpression) valueToWrite)
                    .getOffset()
                    .asNumericValue()
                    .bigIntegerValue()
                    .compareTo(BigInteger.ZERO)
                == 0);
        valueToWrite = ((AddressExpression) valueToWrite).getMemoryAddress();
        return ValueAndSMGState.of(valueToWrite, currentState);
      } else {
        return ValueAndSMGState.of(valueToWrite, currentState);
      }
    }
    return ValueAndSMGState.of(value, this);
  }

  /**
   * Writes the entire variable given to 0. Same as writeToStackOrGlobalVariable() else.
   *
   * @param variableName name of the variable that should be known already.
   * @return a {@link SMGState} with the {@link Value} wirrten at the given position in the variable
   *     given.
   * @throws SMGException in case of errors like write to not declared variable.
   */
  public SMGState writeToStackOrGlobalVariableToZero(String variableName, CType type, CFAEdge edge)
      throws SMGException, SMGSolverException {
    Optional<SMGObject> maybeVariableMemory =
        getMemoryModel().getObjectForVisibleVariable(variableName);

    if (maybeVariableMemory.isEmpty()) {
      // Write to unknown variable
      throw new SMGException(withWriteToUnknownVariable(variableName));
    }

    SMGObject variableMemory = maybeVariableMemory.orElseThrow();
    return writeValueWithChecks(
        variableMemory,
        new NumericValue(variableMemory.getOffset()),
        variableMemory.getSize(),
        new NumericValue(0),
        type,
        edge);
  }

  /**
   * Creates a pointer (points-to-edge) from the value to the target at the specified offset. The
   * Value is mapped to a SMGValue if no mapping exists, else the existing will be used. This does
   * not check whether or not a pointer already exists but will override the target if the value
   * already has a mapping!
   *
   * @param addressValue {@link Value} used as address pointing to the target at the offset.
   * @param target {@link SMGObject} where the pointer points to.
   * @param offsetInBits offset in the object.
   * @return the new {@link SMGState} with the pointer and mapping added.
   */
  public SMGState createAndAddPointer(
      Value addressValue, SMGObject target, BigInteger offsetInBits) {
    return copyAndReplaceMemoryModel(
        memoryModel.copyAndAddPointerFromAddressToRegion(addressValue, target, offsetInBits));
  }

  /*
   * Same as createAndAddPointer but with a specific nesting level in the SMGObject
   */
  public ValueAndSMGState createAndAddPointerWithNestingLevel(
      SMGObject target, BigInteger offsetInBits, int nestingLevel) {
    // search for existing pointer first and return if found
    Optional<SMGValue> maybeAddressValue =
        getMemoryModel()
            .getAddressValueForPointsToTargetWithNestingLevel(target, offsetInBits, nestingLevel);

    if (maybeAddressValue.isPresent()) {
      Optional<Value> valueForSMGValue =
          getMemoryModel().getValueFromSMGValue(maybeAddressValue.orElseThrow());
      if (maybeAddressValue.orElseThrow().getNestingLevel() != nestingLevel) {
        Preconditions.checkArgument(
            maybeAddressValue.orElseThrow().getNestingLevel() == nestingLevel);
      }
      return ValueAndSMGState.of(valueForSMGValue.orElseThrow(), this);
    }
    Value newAddressValue = SymbolicValueFactory.getInstance().newIdentifier(null);
    return ValueAndSMGState.of(
        newAddressValue,
        copyAndReplaceMemoryModel(
            memoryModel.copyAndAddPointerFromAddressToRegionWithNestingLevel(
                newAddressValue, target, offsetInBits, nestingLevel)));
  }

  /**
   * Sets the entered variable to extern. The variable has to exist in the current memory model or
   * an exception is thrown. This keeps the former association of the variable intact! So if its
   * declared global before, it is still after this method.
   *
   * @param variableName name of the variable.
   * @return new {@link SMGState} with the variable set to external.
   */
  public SMGState setExternallyAllocatedFlag(String variableName) {
    return copyAndReplaceMemoryModel(
        memoryModel.copyAndAddExternalObject(
            getMemoryModel().getObjectForVisibleVariable(variableName).orElseThrow()));
  }

  /**
   * Returns a new symbolic constant value. This is meant to transform UNKNOWN Values into usable
   * values with unknown value but known type.
   *
   * @param valueType the {@link CType} of the Value. Don't use the canonical type if possible!
   * @return a new symbolic Value.
   */
  private Value getNewSymbolicValueForType(CType valueType) {
    // For unknown values we use a new symbolic value without memory location as this is
    // handled by the SMGs
    SymbolicValueFactory factory = SymbolicValueFactory.getInstance();
    return factory.asConstant(factory.newIdentifier(null), valueType);
  }

  /**
   * Searches for an existing SMGObject holding the address to a function. We treat function
   * addresses as global variables.
   *
   * @param pDeclaration the {@link CFunctionDeclaration} of the function you are searching for.
   *     Doubles as name in an encoded form similar to qualified name.
   * @return Either a {@link SMGObject} for the function, or empty.
   */
  public Optional<SMGObject> getObjectForFunction(CFunctionDeclaration pDeclaration) {
    String functionQualifiedSMGName = getUniqueFunctionName(pDeclaration);
    return memoryModel.getObjectForVisibleVariable(functionQualifiedSMGName);
  }

  /**
   * Generates a (global) variable for the entered function.
   *
   * @param pDeclaration {@link CFunctionDeclaration} of the function that should be put into a
   *     variable.
   * @return a copy of the SMGState with the variable for the function added.
   */
  public SMGState copyAndAddFunctionVariable(CFunctionDeclaration pDeclaration) {
    String functionQualifiedSMGName = getUniqueFunctionName(pDeclaration);
    return copyAndAddGlobalVariable(0, functionQualifiedSMGName, pDeclaration.getType());
  }

  /*
   * Generates a unique String based on the entered function declaration. Can be used as variable name.
   */
  public String getUniqueFunctionName(CFunctionDeclaration pDeclaration) {

    StringBuilder functionName = new StringBuilder(pDeclaration.getQualifiedName());

    for (CParameterDeclaration parameterDcl : pDeclaration.getParameters()) {
      functionName.append("_");
      functionName.append(CharMatcher.anyOf("* ").replaceFrom(parameterDcl.toASTString(), "_"));
    }

    return "__" + functionName;
  }

  /*
   * Generates a unique String based on the entered function declaration for variable arguments.
   * Can be used as variable name.
   */
  public String getUniqueFunctionBasedNameForVarArgs(CFunctionDeclaration pDeclaration) {
    return getUniqueFunctionName(pDeclaration) + "_varArgs";
  }

  @Override
  public Set<MemoryLocation> getTrackedMemoryLocations() {
    return memoryModel.getMemoryLocationsAndValuesForSPCWithoutHeap().keySet();
  }

  /**
   * A set of Values in the heap with explicit values.
   *
   * @return A set of Values with explicit values.
   */
  public Set<Value> getTrackedHeapValues() {
    ImmutableSet.Builder<Value> trackedHeapValues = ImmutableSet.builder();
    PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> valuesOfObj =
        memoryModel.getSmg().getSMGObjectsWithSMGHasValueEdges();
    for (SMGObject heapObj : memoryModel.getHeapObjects()) {
      if (memoryModel.isObjectValid(heapObj) && valuesOfObj.containsKey(heapObj)) {
        for (SMGHasValueEdge hve : valuesOfObj.get(heapObj)) {
          // We expect all SMGValues to always be known as Values
          Value value = memoryModel.getValueFromSMGValue(hve.hasValue()).orElseThrow();
          // filter out unknowns and pointers
          if (value.isExplicitlyKnown()) {
            trackedHeapValues.add(value);
          }
        }
      }
    }
    return trackedHeapValues.build();
  }

  /*
   * Returns the number of variables in the memory model
   */
  @Override
  public int getSize() {
    // Note: this might be inaccurate! We track Strings and functions as encoded variables!
    return memoryModel.getNumberOfVariables();
  }

  public SMGInterpolant createInterpolant(boolean isMemorySafety) {
    PersistentStack<CFunctionDeclarationAndOptionalValue> funDecls =
        memoryModel.getFunctionDeclarationsFromStackFrames();
    Iterator<CFunctionDeclarationAndOptionalValue> funDeclsIter = funDecls.iterator();
    Preconditions.checkArgument(funDeclsIter.hasNext());

    return new SMGInterpolant(
        options,
        machineModel,
        logger,
        memoryModel.getMemoryLocationsAndValuesForSPCWithoutHeap(),
        memoryModel.getSizeObMemoryForSPCWithoutHeap(),
        memoryModel.getVariableTypeMap(),
        funDecls,
        funDeclsIter.next().getCFunctionDeclaration(),
        getTrackedHeapValues(),
        memoryModel,
        isMemorySafety ? errorInfo : ImmutableList.of(),
        evaluator);
  }

  /**
   * Takes the {@link Value}s that should be retained in the Heap and removes all other explicit
   * Values from the heap.
   *
   * @param valuesToRetain the (concrete) {@link Value}s to retain in all of the heap.
   * @return the new {@link SMGState} with the removed heap values.
   */
  public SMGState enforceHeapValuePrecision(Set<Value> valuesToRetain) {
    // Changing the value mappings would not work, as we would change existing values in local and
    // global variables as well
    // We search for the SMGValues in the Has-Value-Edges and translate them to Values. We don´t
    // want to remove pointers or unknown values, just concrete values not in the set. This triggers
    // a read re-interpretation in the future that generates new unknowns.
    // However, we remember the mapping. It suffices to remember Object -> old Has-Value-Edge

    SMG currentSMG = memoryModel.getSmg();
    SMGState currentState = this;
    for (SMGObject heapObject : memoryModel.getHeapObjects()) {
      if (memoryModel.isObjectValid(heapObject)) {
        // Remove if all match:
        // is not pointer
        // is not in the given set
        // is not unknown
        FluentIterable<SMGHasValueEdge> retainIterable =
            memoryModel
                .getSmg()
                .getHasValueEdgesByPredicate(
                    heapObject,
                    hv ->
                        currentSMG.isPointer(hv.hasValue())
                            || (memoryModel.getValueFromSMGValue(hv.hasValue()).isEmpty()
                                || !memoryModel
                                    .getValueFromSMGValue(hv.hasValue())
                                    .orElseThrow()
                                    .isExplicitlyKnown()
                                || valuesToRetain.contains(
                                    memoryModel
                                        .getValueFromSMGValue(hv.hasValue())
                                        .orElseThrow())));

        currentState =
            currentState.copyAndReplaceMemoryModel(
                currentState
                    .getMemoryModel()
                    .copyAndReplaceHVEdgesAt(heapObject, PersistentSet.copyOf(retainIterable)));
      }
    }

    return currentState;
  }

  public SMGState removeHeapValue(Value valueToRemove) {
    // Changing the value mappings would not work, as we would change existing values in local and
    // global variables as well
    // We search for the SMGValues in the Has-Value-Edges and translate them to Values. We don´t
    // want to remove pointers or unknown values, just concrete values not in the set. This triggers
    // a read re-interpretation in the future that generates new unknowns.
    // We remember the mapping however. It suffices to remember Object -> old Has-Value-Edge

    SMG currentSMG = memoryModel.getSmg();
    SMGState currentState = this;
    for (SMGObject heapObject : memoryModel.getHeapObjects()) {
      if (memoryModel.isObjectValid(heapObject)) {
        // Remove if all match:
        // is not pointer
        // is equal to given Value
        // is not unknown
        FluentIterable<SMGHasValueEdge> retainIterable =
            memoryModel
                .getSmg()
                .getHasValueEdgesByPredicate(
                    heapObject,
                    hv ->
                        currentSMG.isPointer(hv.hasValue())
                            || memoryModel.getValueFromSMGValue(hv.hasValue()).isEmpty()
                            || !memoryModel
                                .getValueFromSMGValue(hv.hasValue())
                                .orElseThrow()
                                .isExplicitlyKnown()
                            || !valueToRemove.equals(
                                memoryModel.getValueFromSMGValue(hv.hasValue()).orElseThrow()));

        currentState =
            currentState.copyAndReplaceMemoryModel(
                currentState
                    .getMemoryModel()
                    .copyAndReplaceHVEdgesAt(heapObject, PersistentSet.copyOf(retainIterable)));
      }
    }

    return currentState;
  }

  public SMGState copyAndRemember(SMGInformation pForgottenInformation) {
    SMGState currentState = this;
    for (Entry<SMGObject, Set<SMGHasValueEdge>> entry :
        pForgottenInformation.getHeapValuesPerObjectMap().entrySet()) {
      SMGObject object = entry.getKey();

      for (SMGHasValueEdge edgeToInsert : entry.getValue()) {
        currentState =
            currentState.copyAndReplaceMemoryModel(
                currentState.memoryModel.replaceValueAtWithAndCopy(
                    object, edgeToInsert.getOffset(), edgeToInsert.getSizeInBits(), edgeToInsert));
      }
    }

    return currentState;
  }

  @Override
  public StateAndInfo<SMGState, SMGInformation> copyAndForget(MemoryLocation pLocation) {
    String qualifiedName = pLocation.getQualifiedName();
    BigInteger offsetInBits = BigInteger.valueOf(pLocation.getOffset());
    SMGObject memory;
    if (qualifiedName.contains("::__retval__")) {
      // Return obj
      memory = getReturnObjectForMemoryLocation(pLocation);
    } else {
      // This is expected to succeed for global and local vars
      memory = getMemoryModel().getObjectForVariable(qualifiedName).orElseThrow();
    }

    Optional<SMGHasValueEdge> maybeEdgeToRemove =
        memoryModel
            .getSmg()
            .getHasValueEdgeByPredicate(memory, o -> o.getOffset().compareTo(offsetInBits) == 0);
    // It can be that the edge is already removed, i.e. through shared memory i.e. arrays
    if (maybeEdgeToRemove.isEmpty()) {
      return new StateAndInfo<>(
          this,
          new SMGInformation(
              PathCopyingPersistentTreeMap.of(),
              getMemoryModel().getSizeObMemoryForSPCWithoutHeap(),
              memoryModel.getVariableTypeMap(),
              memoryModel.getFunctionDeclarationsFromStackFrames()));
    }

    SMGHasValueEdge edgeToRemove = maybeEdgeToRemove.orElseThrow();

    Value removedValue = memoryModel.getValueFromSMGValue(edgeToRemove.hasValue()).orElseThrow();

    SymbolicProgramConfiguration newSPC =
        memoryModel.copyAndRemoveHasValueEdges(memory, ImmutableList.of(edgeToRemove));
    // We don't need to remove the entire variable! We just need to return unknown for it, which is
    // fulfilled by removing the value edge.
    SMGState newState = copyAndReplaceMemoryModel(newSPC);

    return new StateAndInfo<>(
        newState,
        new SMGInformation(
            PathCopyingPersistentTreeMap.<MemoryLocation, ValueAndValueSize>of()
                .putAndCopy(
                    pLocation, ValueAndValueSize.of(removedValue, edgeToRemove.getSizeInBits())),
            getMemoryModel().getSizeObMemoryForSPCWithoutHeap(),
            memoryModel.getVariableTypeMap(),
            memoryModel.getFunctionDeclarationsFromStackFrames()));
  }

  private SMGObject getReturnObjectForMemoryLocation(MemoryLocation memLoc) {
    String funcName = memLoc.getFunctionName();
    for (StackFrame stack : memoryModel.getStackFrames()) {
      if (stack.getFunctionDefinition().getQualifiedName().equals(funcName)) {
        return stack.getReturnObject().orElseThrow();
      }
    }
    // I can't throw a good exception because of the ValueAnalysis interface, forgive me
    return null;
  }

  @Override
  public SMGState copyAndRemember(MemoryLocation pLocation, SMGInformation pForgottenInformation) {
    ValueAndValueSize valueAndSize = pForgottenInformation.getAssignments().get(pLocation);
    SMGState newState;
    try {
      newState =
          assignNonHeapConstant(
              pLocation,
              valueAndSize,
              pForgottenInformation.getSizeInformationForVariablesMap(),
              pForgottenInformation.getTypeOfVariablesMap());
    } catch (SMGException | SMGSolverException e) {
      // The interface forces me to add this. However, this should never be thrown, as we remember
      // by concrete values, so e.g. no solver is ever used here
      throw new RuntimeException(e);
    }
    return newState;
  }

  public int getNumberOfGlobalVariables() {
    return memoryModel.getGlobalVariableToSmgObjectMap().size();
  }

  public boolean hasStackFrameForFunctionDef(CFunctionDeclaration edgeToCheck) {
    for (StackFrame frame : memoryModel.getStackFrames()) {
      // Yes == !
      if (frame.getFunctionDefinition() == edgeToCheck) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    if (!errorInfo.isEmpty()) {
      builder.append("Latest error found: " + errorInfo.get(errorInfo.size() - 1));
    }

    for (Entry<MemoryLocation, ValueAndValueSize> memLoc :
        memoryModel.getMemoryLocationsAndValuesForSPCWithoutHeap().entrySet()) {
      CType readType = memoryModel.getTypeOfVariable(memLoc.getKey());
      Value valueRead = memLoc.getValue().getValue();
      if (readType != null && doesRequireUnionFloatConversion(valueRead, readType)) {
        // Float conversion is limited to the Java float types at the moment.
        // Larger float types are almost always unknown
        valueRead = castValueForUnionFloatConversion(valueRead, readType);
      }
      if (memoryModel.isPointer(valueRead)) {
        builder.append(memLoc.getKey() + ":  pointer: " + valueRead);
        builder.append("\n");
      } else {
        builder.append(memLoc.getKey() + ": " + valueRead);
        builder.append("\n");
      }
    }

    return builder.toString();
  }

  /*
   * Abstracts candidates into a DLL. May abstract the chain behind the first root into more than 1 list! Depending on == values.
   * Only abstracts lists with == values.
   */
  public SMGState abstractIntoDLL(
      SMGObject root, BigInteger nfo, BigInteger pfo, Set<SMGObject> alreadyVisited)
      throws SMGException {
    // Check that the next object exists, is valid, has the same size and the same value in head
    Optional<SMGObject> maybeNext = getValidNextSLL(root, nfo);
    if (maybeNext.isEmpty()
        || maybeNext.orElseThrow().equals(root)
        || alreadyVisited.contains(maybeNext.orElseThrow())) {
      return this;
    }
    SMGObject nextObj = maybeNext.orElseThrow();
    // Values not equal, continue traverse
    if (!checkEqualValuesForTwoStatesWithExemptions(
        root,
        nextObj,
        ImmutableList.of(nfo, pfo),
        this,
        this,
        EqualityCache.<Value>of(),
        new HashSet<>())) {
      return abstractIntoDLL(
          nextObj,
          nfo,
          pfo,
          ImmutableSet.<SMGObject>builder().addAll(alreadyVisited).add(root).build());
    }
    // If it does, create a new SLL with the correct stuff
    // Copy the edges from the next object to the SLL
    SMGDoublyLinkedListSegment newDLL;
    int incrementAmount = 1;
    if (root.isSLL()) {
      // Something went wrong
      // TODO: log and decide what to do here (can this even happen?)
      return this;
    } else if (root instanceof SMGDoublyLinkedListSegment oldDLL) {
      int newMinLength = ((SMGDoublyLinkedListSegment) root).getMinLength();
      if (nextObj instanceof SMGSinglyLinkedListSegment) {
        newMinLength = newMinLength + ((SMGSinglyLinkedListSegment) nextObj).getMinLength();
        incrementAmount = ((SMGSinglyLinkedListSegment) nextObj).getMinLength();
      } else {
        newMinLength++;
      }
      newDLL =
          new SMGDoublyLinkedListSegment(
              oldDLL.getNestingLevel(),
              oldDLL.getSize(),
              oldDLL.getOffset(),
              oldDLL.getHeadOffset(),
              oldDLL.getNextOffset(),
              oldDLL.getPrevOffset(),
              newMinLength);
    } else {
      // We assume that the head is either at 0 if the nfo is not, or right behind the nfo if it is
      // not at 0, or right behind the pfo if the pfo is right behind nfo
      BigInteger headOffset;
      if (nfo.compareTo(root.getOffset()) == 0 || pfo.compareTo(root.getOffset()) == 0) {
        // 0 is taken
        if (nfo.compareTo(root.getOffset().add(memoryModel.getSizeOfPointer())) == 0
            || pfo.compareTo(root.getOffset().add(memoryModel.getSizeOfPointer())) == 0) {
          headOffset =
              root.getOffset()
                  .add(memoryModel.getSizeOfPointer())
                  .add(memoryModel.getSizeOfPointer());
        } else {
          // The slot in between the 2 pointers
          headOffset = root.getOffset().add(memoryModel.getSizeOfPointer());
        }
      } else {
        headOffset = BigInteger.ZERO;
      }
      int newMinLength = 1;
      if (nextObj instanceof SMGSinglyLinkedListSegment) {
        newMinLength = newMinLength + ((SMGSinglyLinkedListSegment) nextObj).getMinLength();
        incrementAmount = ((SMGSinglyLinkedListSegment) nextObj).getMinLength();
      } else {
        newMinLength++;
      }
      newDLL =
          new SMGDoublyLinkedListSegment(
              root.getNestingLevel(),
              root.getSize(),
              root.getOffset(),
              headOffset,
              nfo,
              pfo,
              newMinLength);
    }
    SMGState currentState = copyAndAddObjectToHeap(newDLL);
    // TODO: check that the other values are not pointers, if they are we want to merge the pointers
    // and increment the pointer level
    currentState = currentState.copyAllValuesFromObjToObj(nextObj, newDLL);
    // Write prev from root into the current prev
    SMGValueAndSMGState prevPointer =
        currentState.readSMGValue(root, pfo, memoryModel.getSizeOfPointer());
    currentState =
        prevPointer
            .getSMGState()
            .writeValueWithoutChecks(
                newDLL, pfo, memoryModel.getSizeOfPointer(), prevPointer.getSMGValue());

    // Replace ALL pointers that previously pointed to the root or the next object to the SLL
    // This currently simply changes where the pointers point to, the values are the same
    // Careful as to not introduce a loop! As root does point to next
    SMGValueAndSMGState nextPointerFromRoot =
        currentState.readSMGValue(root, nfo, memoryModel.getSizeOfPointer());
    if (incrementAmount == 0) {
      // If we merge a 0+ currently, we actually want to remove the pointer instead of switching it
      // to the new Obj, as it already exists from the previous segment (same nesting level)
      // There can never be more than 1 pointer to a 0+, and that is the next pointer
      // Since we override the next pointer anyway, we can just ignore the pointer

      // Assert that it truly only points towards the 0+
      assert (currentState
              .getMemoryModel()
              .getSmg()
              .getNumberOfSMGValueUsages(nextPointerFromRoot.getSMGValue())
          == 1);
      assert (currentState.getMemoryModel().getSmg().getNumberOfSMGPointsToEdgesTowards(nextObj)
          == 1);

      // Delete old 0+ pointer
      currentState =
          currentState.copyAndReplaceMemoryModel(
              currentState
                  .getMemoryModel()
                  .removePointerFromSMGAndCopy(nextPointerFromRoot.getSMGValue()));

      // Switch all other pointers
      currentState =
          currentState.copyAndReplaceMemoryModel(
              currentState.memoryModel.replaceAllPointersTowardsWithAndIncrementNestingLevel(
                  root, newDLL, incrementAmount));
    } else {
      currentState =
          currentState.copyAndReplaceMemoryModel(
              currentState.memoryModel.replaceAllPointersTowardsWith(nextObj, newDLL));
      currentState =
          currentState.copyAndReplaceMemoryModel(
              currentState.memoryModel.replaceAllPointersTowardsWithAndIncrementNestingLevel(
                  root, newDLL, incrementAmount));
    }

    // Remove the 2 old objects and continue
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState.memoryModel.copyAndRemoveObjectFromHeap(nextObj));
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState.memoryModel.copyAndRemoveObjectFromHeap(root));

    if (incrementAmount == 0) {
      assert (currentState
              .getMemoryModel()
              .getSmg()
              .getNumberOfSMGValueUsages(nextPointerFromRoot.getSMGValue())
          == 0);
      assert (currentState.getMemoryModel().getSmg().getNumberOfSMGPointsToEdgesTowards(nextObj)
          == 0);
    }
    return currentState.abstractIntoDLL(
        newDLL,
        nfo,
        pfo,
        ImmutableSet.<SMGObject>builder().addAll(alreadyVisited).add(newDLL).build());
  }

  /*
   * Abstracts candidates into a SLL. May abstract the chain behind the first root into more than 1 list! Depending on == values.
   * Only abstracts lists with == values.
   */
  public SMGState abstractIntoSLL(SMGObject root, BigInteger nfo, Set<SMGObject> alreadyVisited)
      throws SMGException {

    // Check that the next object exists, is valid, has the same size and the same value in head
    Optional<SMGObject> maybeNext = getValidNextSLL(root, nfo);
    if (maybeNext.isEmpty()
        || maybeNext.orElseThrow().equals(root)
        || alreadyVisited.contains(maybeNext.orElseThrow())) {
      return this;
    }
    SMGObject nextObj = maybeNext.orElseThrow();

    // Values not equal, continue traverse
    if (!checkEqualValuesForTwoStatesWithExemptions(
        root, nextObj, ImmutableList.of(nfo), this, this, EqualityCache.of(), new HashSet<>())) {
      return abstractIntoSLL(
          nextObj, nfo, ImmutableSet.<SMGObject>builder().addAll(alreadyVisited).add(root).build());
    }

    // If it does, create a new SLL with the correct stuff
    // Copy the edges from the next object to the SLL
    SMGSinglyLinkedListSegment newSLL;
    int incrementAmount = 1;
    if (root instanceof SMGDoublyLinkedListSegment) {
      // Something went wrong
      // TODO: log and decide what to do here (can this even happen?)
      return this;
    } else if (root instanceof SMGSinglyLinkedListSegment oldSLL) {
      int newMinLength = oldSLL.getMinLength();
      if (nextObj instanceof SMGSinglyLinkedListSegment) {
        newMinLength = newMinLength + ((SMGSinglyLinkedListSegment) nextObj).getMinLength();
        incrementAmount = ((SMGSinglyLinkedListSegment) nextObj).getMinLength();
      } else {
        newMinLength++;
      }
      newSLL =
          new SMGSinglyLinkedListSegment(
              oldSLL.getNestingLevel(),
              oldSLL.getSize(),
              oldSLL.getOffset(),
              oldSLL.getHeadOffset(),
              nfo,
              newMinLength);
    } else {
      // We assume that the head is either at 0 if the nfo is not, or right behind the nfo if it is
      // not.
      // We don't care about it however
      int newMinLength = 1;
      if (nextObj instanceof SMGSinglyLinkedListSegment) {
        newMinLength = newMinLength + ((SMGSinglyLinkedListSegment) nextObj).getMinLength();
        incrementAmount = ((SMGSinglyLinkedListSegment) nextObj).getMinLength();
      } else {
        newMinLength++;
      }
      BigInteger headOffset =
          nfo.compareTo(root.getOffset()) == 0
              ? nfo.add(memoryModel.getSizeOfPointer())
              : BigInteger.ZERO;
      newSLL =
          new SMGSinglyLinkedListSegment(
              root.getNestingLevel(),
              root.getSize(),
              root.getOffset(),
              headOffset,
              nfo,
              newMinLength);
    }
    SMGState currentState = copyAndAddObjectToHeap(newSLL);
    // TODO: check that the other values are not pointers, if they are we want to merge the pointers
    // and increment the pointer level
    currentState = currentState.copyAllValuesFromObjToObj(nextObj, newSLL);

    // Replace ALL pointers that previously pointed to the root or the next object to the SLL
    // We increment their nesting level by 1
    // We don´t change the nesting level of the pointers that were pointed towards the concrete
    // segment as this is the current bottom with 0
    // Careful as to not introduce a loop! As root does point to next,
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState.memoryModel.replaceAllPointersTowardsWith(nextObj, newSLL));
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState.memoryModel.replaceAllPointersTowardsWithAndIncrementNestingLevel(
                root, newSLL, incrementAmount));

    // Remove the 2 old objects and continue
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState.memoryModel.copyAndRemoveObjectFromHeap(nextObj));

    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState.memoryModel.copyAndRemoveObjectFromHeap(root));

    // TODO: write a test that checks that we remove all unnecessary pointers/values etc.
    return currentState.abstractIntoSLL(
        newSLL, nfo, ImmutableSet.<SMGObject>builder().addAll(alreadyVisited).add(newSLL).build());
  }

  private Optional<SMGObject> getValidNextSLL(SMGObject root, BigInteger nfo) throws SMGException {
    SMGState currentState = this;
    SMGValueAndSMGState valueAndState =
        currentState.readSMGValue(root, nfo, memoryModel.getSizeOfPointer());
    SMGValue value = valueAndState.getSMGValue();
    if (!memoryModel.getSmg().isPointer(value)) {
      return Optional.empty();
    }
    SMGObject nextObject = memoryModel.getSmg().getPTEdge(value).orElseThrow().pointsTo();
    if (!memoryModel.getSmg().isValid(nextObject)
        || root.getSize().compareTo(nextObject.getSize()) != 0) {
      return Optional.empty();
    }
    // Same object size, same content expect for the pointers, its valid -> ok
    // We don't need the state as it would only change for unknown reads
    return Optional.of(nextObject);
  }

  /**
   * Invalidates variables. For local variables that i.e. went out of scope.
   *
   * @param variable {@link MemoryLocation} for the variable to be invalidated.
   * @return a new state with the variables SMGObject invalid.
   */
  public SMGState invalidateVariable(MemoryLocation variable) {
    if (isLocalOrGlobalVariablePresent(variable)) {
      Optional<SMGObject> maybeVariableObject =
          memoryModel.getObjectForVisibleVariable(variable.getQualifiedName());
      if (maybeVariableObject.isPresent()) {
        // Get objects present outside of the current scope
        Set<SMGObject> otherPresentObjects = memoryModel.getObjectsValidInOtherStackFrames();
        if (!otherPresentObjects.contains(maybeVariableObject.orElseThrow())) {
          return copyAndReplaceMemoryModel(
              memoryModel.invalidateSMGObject(maybeVariableObject.orElseThrow()));
        }
      }
    }
    return this;
  }

  /**
   * Tries to dereference the pointer given by the argument {@link Value}. Returns a
   * SMGStateAndOptionalSMGObjectAndOffset without object and offset but maybe a updated state if
   * the dereference fails because the entered {@link Value} is not known as a pointer. This does
   * not check validity of the Value! If a 0+ abstracted list element is materialized, 2 states are
   * returned. The first in the list being the minimal list.
   *
   * @param pointer the {@link Value} to dereference.
   * @return Optional filled with the {@link SMGObjectAndOffset} of the target of the pointer. Empty
   *     if its not a pointer in the current {@link SymbolicProgramConfiguration}.
   * @throws SMGException in case of critical errors in the materialization of abstract memory.
   */
  public List<SMGStateAndOptionalSMGObjectAndOffset> dereferencePointer(Value pointer)
      throws SMGException {
    if (!memoryModel.isPointer(pointer)) {
      // Not known or not known as a pointer, return nothing
      return ImmutableList.of(SMGStateAndOptionalSMGObjectAndOffset.of(this));
    }
    SMGState currentState = this;
    SMGValue smgValueAddress = memoryModel.getSMGValueFromValue(pointer).orElseThrow();
    SMGPointsToEdge ptEdge = memoryModel.getSmg().getPTEdge(smgValueAddress).orElseThrow();
    // Every DLL is also a SLL
    if (ptEdge.pointsTo() instanceof SMGSinglyLinkedListSegment) {
      // When materializing the first element is the minimal list (for 0+)
      return materializeLinkedList(smgValueAddress, ptEdge, currentState);
    }
    Preconditions.checkArgument(!(ptEdge.pointsTo() instanceof SMGSinglyLinkedListSegment));
    return ImmutableList.of(
        SMGStateAndOptionalSMGObjectAndOffset.of(
            ptEdge.pointsTo(), new NumericValue(ptEdge.getOffset()), currentState));
  }

  /**
   * Materializes a list, starting from the pointer {@link SMGValue} given as initialPointerValue.
   * Expects initialPointerValue to be a pointer towards an abstracted list segment. Returns a
   * materialized memory region. For multiple possible results, returns the minimal result first
   * (i.e. the ended list).
   *
   * @param initialPointerValue the SMGValue that is a pointer and has the {@link SMGPointsToEdge}
   *     ptEdge, which points towards an abstracted list segment.
   * @param ptEdge the points to edge of the pointer value from initialPointerValue.
   * @param pState current {@link SMGState}
   * @return a list of {@link SMGStateAndOptionalSMGObjectAndOffset} with the state after the
   *     materialization and the target object of the pointer, but materialized, plus the offset in
   *     that object. If there are multiple possible results, the minimal result (i.e. for a 0+ list
   *     segment the list that ends) is handed back first in index 0.
   * @throws SMGException for critical errors. (Mostly to debug if something goes wrong)
   */
  private List<SMGStateAndOptionalSMGObjectAndOffset> materializeLinkedList(
      SMGValue initialPointerValue, SMGPointsToEdge ptEdge, SMGState pState) throws SMGException {
    SMGState currentState = pState;
    if (ptEdge.pointsTo() instanceof SMGSinglyLinkedListSegment) {
      List<SMGValueAndSMGState> newPointersValueAndStates =
          currentState.materializeReturnPointerValueAndCopy(initialPointerValue);
      if (newPointersValueAndStates.size() == 2) {
        Preconditions.checkArgument(
            ((SMGSinglyLinkedListSegment) ptEdge.pointsTo()).getMinLength() == 0);
        ImmutableList.Builder<SMGStateAndOptionalSMGObjectAndOffset> returnBuilder =
            ImmutableList.builder();
        // 0+ case, this is the end of the materialization as outside pointers to this do not exist
        // and access can only happen for one element at a time through next (and prev) pointers
        for (SMGValueAndSMGState newPointerValueAndState : newPointersValueAndStates) {
          currentState = newPointerValueAndState.getSMGState();
          Optional<SMGPointsToEdge> maybePtEdge =
              currentState.memoryModel.getSmg().getPTEdge(newPointerValueAndState.getSMGValue());
          if (maybePtEdge.isEmpty()) {
            returnBuilder.add(SMGStateAndOptionalSMGObjectAndOffset.of(currentState));
            continue;
          }
          ptEdge = maybePtEdge.orElseThrow();
          Preconditions.checkArgument(!(ptEdge.pointsTo() instanceof SMGSinglyLinkedListSegment));
          returnBuilder.add(
              SMGStateAndOptionalSMGObjectAndOffset.of(
                  ptEdge.pointsTo(), new NumericValue(ptEdge.getOffset()), currentState));
        }
        return returnBuilder.build();

      } else if (newPointersValueAndStates.size() != 1) {
        // Error
        throw new SMGException("Critical error: Unexpected return from list materialization.");
      }
      // Default case, only 1 returned list segment
      SMGValueAndSMGState newPointerValueAndState = newPointersValueAndStates.get(0);
      currentState = newPointerValueAndState.getSMGState();
      Optional<SMGPointsToEdge> maybePtEdge =
          currentState.memoryModel.getSmg().getPTEdge(newPointerValueAndState.getSMGValue());
      if (maybePtEdge.isEmpty()) {
        return ImmutableList.of(SMGStateAndOptionalSMGObjectAndOffset.of(currentState));
      }
      ptEdge = maybePtEdge.orElseThrow();
    }
    Preconditions.checkArgument(!(ptEdge.pointsTo() instanceof SMGSinglyLinkedListSegment));
    return ImmutableList.of(
        SMGStateAndOptionalSMGObjectAndOffset.of(
            ptEdge.pointsTo(), new NumericValue(ptEdge.getOffset()), currentState));
  }

  /**
   * Test/Debug method. Dereferences without materializing lists. Not to be used in regular
   * execution.
   *
   * @param pointer target pointer.
   * @return {@link SMGStateAndOptionalSMGObjectAndOffset} with the target if it exists.
   */
  public Optional<SMGStateAndOptionalSMGObjectAndOffset> dereferencePointerWithoutMaterilization(
      Value pointer) {
    if (!memoryModel.isPointer(pointer)) {
      // Not known or not known as a pointer, return nothing
      return Optional.empty();
    }
    SMGState currentState = this;
    SMGValue smgValueAddress = memoryModel.getSMGValueFromValue(pointer).orElseThrow();
    SMGPointsToEdge ptEdge = memoryModel.getSmg().getPTEdge(smgValueAddress).orElseThrow();
    return Optional.of(
        SMGStateAndOptionalSMGObjectAndOffset.of(
            ptEdge.pointsTo(), new NumericValue(ptEdge.getOffset()), currentState));
  }

  /**
   * Takes the value leading to a pointer that points towards abstracted heap (lists). The
   * abstracted heap is then materialized into a concrete list up to that and including the segment
   * pointed to and the old pointer is returned with the state of the materialization. (The old
   * value is equivalent to the new in the context of the new state!) If more than one state is
   * generated (see handleMaterilisation for 0+ list) the first element in the returned list is the
   * minimal list (for witnesses etc.).
   *
   * @param valueToPointerToAbstractObject a SMGValue that has a points to edge leading to
   *     abstracted memory.
   * @return SMGValueAndSMGState with the pointer value to the concrete memory extracted.
   * @throws SMGException in case of critical errors.
   */
  public List<SMGValueAndSMGState> materializeReturnPointerValueAndCopy(
      SMGValue valueToPointerToAbstractObject) throws SMGException {
    SMGPointsToEdge ptEdge =
        memoryModel.getSmg().getPTEdge(valueToPointerToAbstractObject).orElseThrow();
    SMGState currentState = this;
    List<SMGValueAndSMGState> materializationAndState = null;

    while (ptEdge.pointsTo() instanceof SMGSinglyLinkedListSegment) {
      SMGObject obj = ptEdge.pointsTo();
      if (obj.isZero() || !currentState.memoryModel.isObjectValid(obj)) {
        throw new SMGException("");
      }
      // DLLs are also SLLs
      Preconditions.checkArgument(obj instanceof SMGSinglyLinkedListSegment);
      materializationAndState =
          currentState.materializer.handleMaterialisation(
              valueToPointerToAbstractObject, obj, currentState);
      if (materializationAndState.size() == 1) {
        // We can assume that this is the default case
        currentState = materializationAndState.get(0).getSMGState();
        ptEdge =
            currentState
                .memoryModel
                .getSmg()
                .getPTEdge(valueToPointerToAbstractObject)
                .orElseThrow();

      } else {
        // Size should be 2 in all other cases. This is the 0+ case, which is always the last
        Preconditions.checkArgument(materializationAndState.size() == 2);
        // None of the values points to a SMGSinglyLinkedListSegment
        // The 0 state does not materialize anything, the 1 state does materialize one more concrete
        // list segment and appends another 0+ segment after that
        if (currentState
            .memoryModel
            .getSmg()
            .isPointer(materializationAndState.get(0).getSMGValue())) {
          Preconditions.checkArgument(
              !(materializationAndState
                      .get(0)
                      .getSMGState()
                      .memoryModel
                      .getSmg()
                      .getPTEdge(materializationAndState.get(0).getSMGValue())
                      .orElseThrow()
                      .pointsTo()
                  instanceof SMGSinglyLinkedListSegment));
        }
        Preconditions.checkArgument(
            !(materializationAndState
                    .get(1)
                    .getSMGState()
                    .memoryModel
                    .getSmg()
                    .getPTEdge(materializationAndState.get(1).getSMGValue())
                    .orElseThrow()
                    .pointsTo()
                instanceof SMGSinglyLinkedListSegment));
        break;
      }
    }

    Preconditions.checkNotNull(materializationAndState);
    return materializationAndState;
  }

  public boolean hasPointer(MemoryLocation memLoc) {
    String qualifiedName = memLoc.getQualifiedName();
    BigInteger offsetInBits = BigInteger.valueOf(memLoc.getOffset());
    SMGObject memory;
    if (qualifiedName.contains("::__retval__")) {
      // Return obj
      memory = getReturnObjectForMemoryLocation(memLoc);
    } else {
      // This is expected to succeed for global and local vars
      Optional<SMGObject> maybeMemory = getMemoryModel().getObjectForVariable(qualifiedName);
      if (maybeMemory.isEmpty()) {
        return false;
      }
      memory = maybeMemory.orElseThrow();
    }

    Optional<SMGHasValueEdge> maybeEdge =
        memoryModel
            .getSmg()
            .getHasValueEdgeByPredicate(memory, o -> o.getOffset().compareTo(offsetInBits) == 0);

    if (maybeEdge.isEmpty() || maybeEdge.orElseThrow().hasValue().isZero()) {
      // Also return for 0, as 0 is invalid for memory but valid as a value.
      // If we don't remove 0s, we allow all variables, e.g. length, that start at 0
      return false;
    }
    return memoryModel.getSmg().isPointer(maybeEdge.orElseThrow().hasValue());
  }

  /**
   * Searches for a numeric address assumption and returns it if possible. The assumption has all
   * possible offsets already added. As in C standard.
   *
   * @param addressValue the pointer {@link Value} or {@link AddressExpression}
   * @return Optional, either a {@link BigInteger} as numeric address (pointer in Bytes) or empty.
   */
  public Optional<Value> transformAddressIntoNumericValue(Value addressValue) {
    BigInteger offset;
    SMGObject target;
    if (addressValue instanceof AddressExpression addressExpr) {
      if (addressExpr.getOffset().isNumericValue()) {
        offset = addressExpr.getOffset().asNumericValue().bigIntegerValue();
      } else {
        return Optional.empty();
      }
      SMGPointsToEdge ptEdge =
          memoryModel
              .getSmg()
              .getPTEdge(
                  memoryModel.getSMGValueFromValue(addressExpr.getMemoryAddress()).orElseThrow())
              .orElseThrow();
      target = ptEdge.pointsTo();
      offset = offset.add(ptEdge.getOffset());

    } else if (memoryModel.isPointer(addressValue)) {
      SMGPointsToEdge ptEdge =
          memoryModel
              .getSmg()
              .getPTEdge(memoryModel.getSMGValueFromValue(addressValue).orElseThrow())
              .orElseThrow();
      target = ptEdge.pointsTo();
      offset = ptEdge.getOffset();
    } else {
      return Optional.empty();
    }
    return Optional.of(
        new NumericValue(memoryModel.getNumericAssumptionForMemoryRegion(target).add(offset)));
  }

  public boolean isSMGObjectAStackVariable(SMGObject obj) {
    if (memoryModel.isHeapObject(obj)) {
      return false;
    }
    for (StackFrame frame : memoryModel.getStackFrames()) {
      if (frame.getAllObjects().contains(obj)) {
        return true;
      }
    }
    return false;
  }

  /*
   * Get the name of the topmost stack frame function.
   */
  public String getStackFrameTopFunctionName() {
    return memoryModel.getStackFrames().peek().getFunctionDefinition().getQualifiedName();
  }

  /*
   * Searches source for pointers (except for at the given exceptions) and copies the memory
   * precisely and replaces the old pointers with new ones towards the new copied memory.
   * Then the old memory is checked for external pointers towards it, and if there are any with
   * the correct nesting level, they are replaced by the pointers towards the new copied memory.
   * This is part of list materialization.
   */
  @SuppressWarnings("unused")
  public SMGState copyMemoryNotOriginatingFrom(
      SMGObject source, Set<BigInteger> offsetExceptions, int nestingLevel) throws SMGException {
    FluentIterable<SMGHasValueEdge> allHveWOExceptions =
        memoryModel
            .getSmg()
            .getHasValueEdgesByPredicate(
                source, hve -> !offsetExceptions.contains(hve.getOffset()));
    SMGState currentState = this;
    for (SMGHasValueEdge hve : allHveWOExceptions) {
      Optional<Value> maybeValue = currentState.memoryModel.getValueFromSMGValue(hve.hasValue());
      // 0 is a pointer, but can freely be used, no need to copy
      if (maybeValue.isPresent()
          && currentState.memoryModel.isPointer(maybeValue.orElseThrow())
          && !hve.hasValue().isZero()) {
        // The target of this needs to be copied
        Value ptrToTarget = maybeValue.orElseThrow();
        Optional<SMGStateAndOptionalSMGObjectAndOffset> target =
            currentState.dereferencePointerWithoutMaterilization(ptrToTarget);
        if (target.isEmpty() || !target.orElseThrow().hasSMGObjectAndOffset()) {
          continue;
        }
        SMGObject targetObj = target.orElseThrow().getSMGObject();
        Set<SMGHasValueEdge> targetValues = currentState.memoryModel.getSmg().getEdges(targetObj);
        SMGObject copyOfTarget = SMGObject.of(targetObj);
        // TODO: this might be a stack object!
        currentState = currentState.copyAndAddObjectToHeap(copyOfTarget);
        // For now only a shallow copy (no nested pointers)
        for (SMGHasValueEdge targetHVE : targetValues) {
          Optional<Value> maybeTargetValue =
              currentState.memoryModel.getValueFromSMGValue(targetHVE.hasValue());
          if (maybeTargetValue.isPresent()
              && currentState.memoryModel.isPointer(maybeTargetValue.orElseThrow())) {
            throw new SMGException(
                "Nested list abstraction with non-trivial objects not yet supported.");
          }
          currentState =
              currentState.writeValueWithoutChecks(
                  copyOfTarget,
                  targetHVE.getOffset(),
                  targetHVE.getSizeInBits(),
                  targetHVE.hasValue());
        }
        SMGPointsToEdge ptEdgeToTarget =
            currentState.memoryModel.getSmg().getPTEdge(hve.hasValue()).orElseThrow();
        // Create new valid pointer and replace the old one in the source
        Value ptrToCopiedTarget = SymbolicValueFactory.getInstance().newIdentifier(null);
        currentState =
            currentState.createAndAddPointer(
                ptrToCopiedTarget, copyOfTarget, ptEdgeToTarget.getOffset());
        currentState =
            currentState.writeValueWithoutChecks(
                source,
                hve.getOffset(),
                hve.getSizeInBits(),
                currentState.memoryModel.getSMGValueFromValue(ptrToCopiedTarget).orElseThrow());
      }
    }
    // TODO: Then we need to check for pointers towards the old target and switch pointers with the
    // correct nesting level to this new pointer
    // Careful when there is the same pointer twice in this obj

    return currentState;
  }

  // TODO: To be replaced with a better structure, i.e. union-find
  // This is mutable on purpose!
  public static class EqualityCache<V> {
    private final SetMultimap<V, V> primitiveCache;

    private EqualityCache() {
      primitiveCache = HashMultimap.create();
    }

    private EqualityCache(SetMultimap<V, V> newPrimitiveCache) {
      primitiveCache = newPrimitiveCache;
    }

    public static <V> EqualityCache<V> of() {
      return new EqualityCache<>();
    }

    public void addEquality(V thisEqual, V otherEqual) {
      primitiveCache.put(thisEqual, otherEqual);
    }

    /*
     * Returns true is thisEqual and otherEqual are truly equal. False if equality is UNKNOWN!
     * We don't ever save inequalities as we expect the isEqual algorithm to abort for non-equals.
     */
    private boolean isEqualForKnownKey(V thisEqual, V otherEqual) {
      // This is supposed to run into an exception for non-existing!
      return primitiveCache.get(thisEqual).contains(otherEqual);
    }

    /*
     * Use this to check if a known value mapping exists. If this returns false, don't call isEqual!
     */
    private boolean knownKey(V thisEqual) {
      return primitiveCache.containsKey(thisEqual);
    }

    /**
     * Returns true if both entered values are equal, false if its UNKNOWN! False never means they
     * are not equal!
     *
     * @param thisEqual some value
     * @param otherEqual some other value
     * @return true for equality, false for unknown equality.
     */
    public boolean isEqualityKnown(V thisEqual, V otherEqual) {
      return knownKey(thisEqual) && isEqualForKnownKey(thisEqual, otherEqual);
    }
  }
}
