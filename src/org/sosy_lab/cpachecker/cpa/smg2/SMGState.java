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
import com.google.common.base.Equivalence.Wrapper;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
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
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.smg2.SMGErrorInfo.Property;
import org.sosy_lab.cpachecker.cpa.smg2.abstraction.SMGCPAMaterializer;
import org.sosy_lab.cpachecker.cpa.smg2.constraint.ConstantSymbolicExpressionLocator;
import org.sosy_lab.cpachecker.cpa.smg2.constraint.ConstraintFactory;
import org.sosy_lab.cpachecker.cpa.smg2.constraint.SatisfiabilityAndSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.refiner.SMGInterpolant;
import org.sosy_lab.cpachecker.cpa.smg2.util.CFunctionDeclarationAndOptionalValue;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGHasValueEdgesAndSPC;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectAndOffsetMaybeNestingLvl;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectAndSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGSolverException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGStateAndOptionalSMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGValueAndSMGObject;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.SPCAndSMGObjects;
import org.sosy_lab.cpachecker.cpa.smg2.util.ValueAndValueSize;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AddressExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinarySymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.UnarySymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicIdentifierLocator;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicValues;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue.NegativeNaN;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.refinement.ImmutableForgetfulState;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.SMGProveNequality;
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
import org.sosy_lab.cpachecker.util.smg.util.SMGAndHasValueEdges;
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
        Partitionable,
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

  private final SMGCPAStatistics statistics;

  // Remembers the location (block-end or not) to determine better merge locations
  private final boolean blockEnd;

  // Constructor only for NEW/EMPTY SMGStates!
  private SMGState(
      MachineModel pMachineModel,
      SymbolicProgramConfiguration spc,
      LogManagerWithoutDuplicates logManager,
      SMGOptions opts,
      SMGCPAExpressionEvaluator pEvaluator,
      SMGCPAStatistics pStatistics) {
    memoryModel = spc;
    machineModel = pMachineModel;
    logger = logManager;
    options = opts;
    errorInfo = ImmutableList.of();
    statistics = pStatistics;
    materializer = new SMGCPAMaterializer(logger, statistics);
    lastCheckedMemoryAccess = Optional.empty();
    evaluator = pEvaluator;
    constraintsState = new ConstraintsState();
    blockEnd = false;
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
      SMGCPAExpressionEvaluator pEvaluator,
      SMGCPAStatistics pStatistics,
      boolean pBlockEnd) {
    memoryModel = spc;
    machineModel = pMachineModel;
    logger = logManager;
    options = opts;
    errorInfo = errorInf;
    materializer = pMaterializer;
    lastCheckedMemoryAccess = pLastCheckedMemoryAccess;
    evaluator = pEvaluator;
    constraintsState = pConstraintsState;
    statistics = pStatistics;
    blockEnd = pBlockEnd;
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
      SMGCPAExpressionEvaluator pEvaluator,
      SMGCPAStatistics pStatistics) {
    memoryModel = spc;
    machineModel = pMachineModel;
    logger = logManager;
    options = opts;
    errorInfo = errorInf;
    materializer = pMaterializer;
    lastCheckedMemoryAccess = pLastCheckedMemoryAccess;
    evaluator = pEvaluator;
    constraintsState = pConstraintsState;
    statistics = pStatistics;
    blockEnd = false;
  }

  public SMGState withBlockEnd() {
    return new SMGState(
        machineModel,
        memoryModel,
        logger,
        options,
        errorInfo,
        materializer,
        lastCheckedMemoryAccess,
        constraintsState,
        evaluator,
        statistics,
        true);
  }

  private SMGState copyWithAddedConstraints(ImmutableList<Constraint> pConstraints) {
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
        evaluator,
        statistics);
  }

  private SMGState copyWithNewConstraints(Set<Constraint> pConstraints) {
    checkNotNull(pConstraints);
    return new SMGState(
        machineModel,
        memoryModel,
        logger,
        options,
        errorInfo,
        materializer,
        lastCheckedMemoryAccess,
        new ConstraintsState(pConstraints),
        evaluator,
        statistics);
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
        evaluator,
        statistics);
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
        evaluator,
        statistics);
  }

  public SMGState addConstraint(Constraint pConstraint) {
    checkNotNull(pConstraint);
    return copyWithAddedConstraints(listAndElement(constraintsState, pConstraint));
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

  public void logUnknownValue(String msg) {
    logger.log(Level.FINE, msg);
  }

  public void logUnknownValue(String msg, CFAEdge edge) {
    logUnknownValue(msg + edge);
  }

  public void logUnknownValue(CFAEdge edge) {
    logUnknownValue("A unknown value was assumed in ", edge);
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
        // However, when combined with program exit, we can detect the property MemCleanup.
        PersistentSet<SMGObject> heapObs = memoryModel.getHeapObjects();
        Preconditions.checkState(
            !heapObs.isEmpty() && heapObs.contains(SMGObject.nullInstance()),
            "NULL must always be a heap object");
        heapObs = heapObs.removeAndCopy(SMGObject.nullInstance());
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
      SMGCPAExpressionEvaluator pEvaluator,
      SMGCPAStatistics pStatistics) {
    return new SMGState(
        pMachineModel,
        SymbolicProgramConfiguration.of(BigInteger.valueOf(pMachineModel.getSizeofPtrInBits())),
        logManager,
        opts,
        pEvaluator,
        pStatistics);
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
      SMGCPAExpressionEvaluator pEvaluator,
      SMGCPAStatistics pStatistics) {
    FunctionEntryNode pNode = pCfa.getMainFunction();
    return of(pMachineModel, logManager, opts, pNode, pEvaluator, pStatistics);
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
      SMGCPAExpressionEvaluator pEvaluator,
      SMGCPAStatistics pStatistics) {
    SMGState newState = of(pMachineModel, logManager, opts, pEvaluator, pStatistics);
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
      SMGCPAExpressionEvaluator pEvaluator,
      SMGCPAStatistics pStatistics) {
    return of(pMachineModel, logManager, opts, pEvaluator, pStatistics)
        .copyAndAddStackFrame(cfaEntryFunDecl);
  }

  /**
   * Creates a new {@link SMGState} out of the parameters given. No new elements are created by
   * this.
   *
   * @param pMachineModel the {@link MachineModel} used to determine the size of types.
   * @param pSPC the {@link SymbolicProgramConfiguration} to be used in the new state.
   * @param logManager the {@link LogManager} to be used in the new state.
   * @param opts {@link SMGOptions} to be used.
   * @return a new {@link SMGState} with the arguments given.
   */
  public SMGState of(
      MachineModel pMachineModel,
      SymbolicProgramConfiguration pSPC,
      LogManagerWithoutDuplicates logManager,
      SMGOptions opts,
      SMGCPAExpressionEvaluator pEvaluator,
      SMGCPAStatistics pStatistics) {
    return new SMGState(
        pMachineModel,
        pSPC,
        logManager,
        opts,
        errorInfo,
        materializer,
        lastCheckedMemoryAccess,
        constraintsState,
        pEvaluator,
        pStatistics);
  }

  public MachineModel getMachineModel() {
    return machineModel;
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
        evaluator,
        statistics);
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
      Map<String, Value> variableNameToMemorySizeInBits,
      Map<String, CType> variableTypeMap)
      throws SMGException, SMGSolverException {
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
        new NumericValue(sizeOfWriteInBits),
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
   */
  public SMGState assignNonHeapConstant(
      MemoryLocation memLoc,
      ValueAndValueSize valueAndSize,
      Map<String, Value> variableNameToMemorySizeInBits,
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
      Value sizeInBits = variableNameToMemorySizeInBits.get(qualifiedName);
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
        qualifiedName,
        offsetToWriteToInBits,
        new NumericValue(sizeOfWriteInBits),
        valueToWrite,
        typeOfUnknown);
  }

  public SMGState reconstructStackFrames(
      PersistentStack<CFunctionDeclarationAndOptionalValue> pStackDeclarations)
      throws SMGException, SMGSolverException {
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
      @Nullable Map<String, Value> variableNameToMemorySizeInBits,
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
    return copyAndAddGlobalVariable(
        new NumericValue(BigInteger.valueOf(pTypeSizeInBits)), pVarName, type);
  }

  /**
   * Copy SMGState with a newly created object and put it into the global namespace. This replaces
   * an existing old global variable!
   *
   * @param pTypeSizeInBits Size of the type of the new global variable.
   * @param pVarName Name of the global variable.
   * @return Newly created {@link SMGState} with the object added for the name specified.
   */
  public SMGState copyAndAddGlobalVariable(Value pTypeSizeInBits, String pVarName, CType type) {
    SMGObject newObject = SMGObject.of(0, pTypeSizeInBits, BigInteger.ZERO, pVarName);
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
   * @param pTypeSizeInBits Size of the type of the new memory.
   * @return Newly created object + state with it.
   */
  public SMGObjectAndSMGState copyAndAddNewHeapObject(Value pTypeSizeInBits) {
    SMGObject newObject = SMGObject.of(0, pTypeSizeInBits, BigInteger.ZERO);
    return SMGObjectAndSMGState.of(
        newObject, copyAndReplaceMemoryModel(memoryModel.copyAndAddHeapObject(newObject)));
  }

  /**
   * Copy SMGState with a newly created {@link SMGObject} that is a region from the given object and
   * returns the new state + the new {@link SMGObject}. Make sure that you reuse the {@link
   * SMGObject} right away to create a points-to-edge and not just use SMGObjects in the code.
   *
   * @param objectToCopy Any {@link SMGObject} that is copied as a region.
   * @return Newly created object + state with it.
   */
  public SMGObjectAndSMGState copyAndAddNewHeapRegion(SMGObject objectToCopy) {
    SMGObject newObject =
        SMGObject.of(
            objectToCopy.getNestingLevel(), objectToCopy.getSize(), objectToCopy.getOffset());
    return SMGObjectAndSMGState.of(
        newObject, copyAndReplaceMemoryModel(memoryModel.copyAndAddHeapObject(newObject)));
  }

  /**
   * Copy SMGState with a newly created {@link SMGObject} and returns the new state + the new {@link
   * SMGObject} with the size and type of the given. Make sure that you reuse the {@link SMGObject}
   * right away to create a points-to-edge and not just use SMGObjects in the code.
   *
   * @param objectToCopy The object copied. Size, type, nfo, pfo etc. are all copied.
   * @return Newly created object + state with it.
   */
  public SMGObjectAndSMGState copyAndAddNewHeapObject(SMGObject objectToCopy, int newNestingLevel) {
    SMGObject newObject = objectToCopy.freshCopy().copyWithNewNestingLevel(newNestingLevel);
    Preconditions.checkState(newObject.getClass() == objectToCopy.getClass());
    if (!memoryModel.isObjectValid(objectToCopy)) {
      return SMGObjectAndSMGState.of(
          newObject,
          copyAndReplaceMemoryModel(
              memoryModel.copyAndAddHeapObject(newObject).invalidateSMGObject(newObject, false)));
    }
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

  /**
   * Copy SMGState with a newly created {@link SMGObject} and returns the new state + the new {@link
   * SMGObject} with the size specified in bits. Make sure that you reuse the {@link SMGObject}
   * right away to create a points-to-edge and not just use SMGObjects in the code.
   *
   * @param pTypeSizeInBits Size of the type of the new global variable.
   * @return Newly created object + state with it.
   */
  public SMGObjectAndSMGState copyAndAddStackObject(Value pTypeSizeInBits) {
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
  public SMGState copyAndAddLocalVariable(Value pTypeSize, String pVarName, CType type)
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
      Value pTypeSize, String pVarName, CType type, boolean exceptionOnRead) throws SMGException {
    if (memoryModel.getStackFrames().isEmpty()) {
      throw new SMGException(
          "Can't add a variable named "
              + pVarName
              + " to the memory model because there is no stack frame.");
    }
    SMGObject newObject = SMGObject.of(0, pTypeSize, BigInteger.ZERO, pVarName);
    return copyAndReplaceMemoryModel(
        memoryModel.copyAndAddStackObject(newObject, pVarName, type, exceptionOnRead));
  }

  private SMGState copyAndAddLocalVariableToSpecificStackframe(
      String functionNameForStackFrame, Value pTypeSize, String pVarName, CType type)
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

  /** Copy SMGState and adds a new dummy frame for tests. */
  public SMGState copyAndAddDummyStackFrame() {
    return copyAndReplaceMemoryModel(memoryModel.copyAndAddDummyStackFrame());
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
   * Merge 2 SMGStates and as a consequence its SMGs as far as possible or return no state if they
   * are incomparable.
   *
   */
  public Optional<SMGState> merge(SMGState pOther) throws CPAException {
    if (getSize() != pOther.getSize()) {
      // If there is a non-equal number of (stack/global) variables, the merge fails anyway
      return Optional.empty();
    }

    // A merge w/o nested lists is expensive but most of the time not needed
    Set<SMGSinglyLinkedListSegment> abstrObjs1 =
        getMemoryModel().getSmg().getAllValidAbstractedObjects();
    Set<SMGSinglyLinkedListSegment> abstrObjs2 =
        pOther.getMemoryModel().getSmg().getAllValidAbstractedObjects();
    if (abstrObjs1.isEmpty() && abstrObjs2.isEmpty()) {
      return Optional.empty();
    }

    // We may not forget any errors already found
    if (!checkErrorEqualityForTwoStates(pOther)
        || !copyAndPruneUnreachable()
            .checkErrorEqualityForTwoStates(pOther.copyAndPruneUnreachable())) {
      return Optional.empty();
    }

    if (!lastCheckedMemoryAccess.equals(pOther.lastCheckedMemoryAccess)) {
      // TODO: most likely too strict.
      return Optional.empty();
    }

    SMGState otherSanitizedState = pOther.removeOldConstraints();
    SMGState thisSanitizedState = removeOldConstraints();
    if (!otherSanitizedState.constraintsState.equals(thisSanitizedState.constraintsState)) {
      // TODO: Problem: there might still be distinct symbolic values with the same constraints.
      //   => Compare those by location.
      //   Example: imagine a loop, the loop bound may be against a nondet() function,
      //     the comparison is always i (concretely known, for example 1) < nondet().
      //     The nondet() might be reassigned each loop, thus different.
      //     But since the constraint is equal for location, that would be OK!
      return Optional.empty();
    }
    otherSanitizedState = otherSanitizedState.removeUnusedValues();
    thisSanitizedState = thisSanitizedState.removeUnusedValues();

    // The merge must happen on garbage free memory models
    Optional<SymbolicProgramConfiguration> maybeNewSPC =
        thisSanitizedState.memoryModel.merge(otherSanitizedState.memoryModel, machineModel);

    if (maybeNewSPC.isEmpty()) {
      return Optional.empty();
    }

    assert maybeNewSPC.orElseThrow().checkSMGSanity();
    return Optional.of(
        new SMGState(
            machineModel,
            maybeNewSPC.orElseThrow(),
            logger,
            options,
            errorInfo,
            materializer,
            lastCheckedMemoryAccess,
            thisSanitizedState.constraintsState,
            evaluator,
            statistics));
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

  /** */
  private boolean checkStackFrameEqualityForTwoStates(
      SMGState pOther, EqualityCache<Value> equalityCache, EqualityCache<SMGObject> objectCache) {
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
        if (!areValuesEqual(
            this,
            thisRetVal,
            pOther,
            otherRetVal,
            equalityCache,
            objectCache,
            treatSymbolicsAsEqualWEqualConstrains(pOther, null))) {
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
      SMGState pOther, EqualityCache<Value> equalityCache, EqualityCache<SMGObject> objectCache) {
    // We check the tolerant way; i.e. ignore all type information
    // Get all (global and local) variables
    PersistentMap<MemoryLocation, ValueAndValueSize> thisAllMemLocAndValues =
        memoryModel.getMemoryLocationsAndValuesForSPCWithoutHeap();
    for (Entry<MemoryLocation, ValueAndValueSize> otherMemLocAndValue :
        pOther.memoryModel.getMemoryLocationsAndValuesForSPCWithoutHeap().entrySet()) {
      MemoryLocation otherMemLoc = otherMemLocAndValue.getKey();
      Value otherValue = otherMemLocAndValue.getValue().getValue();
      ValueAndValueSize thisValueAndType = thisAllMemLocAndValues.get(otherMemLoc);
      if (thisValueAndType == null) {
        return false;
      }
      // Now check the equality of all values. For concrete values, we allow overapproximations.
      // Pointers/memory is compared by shape, subsumtion is allowed for equal linked lists, such
      // that the smaller subsumes the larger (5+ >= 6+)
      if (!areValuesEqual(
          this,
          thisValueAndType.getValue(),
          pOther,
          otherValue,
          equalityCache,
          objectCache,
          treatSymbolicsAsEqualWEqualConstrains(pOther, otherMemLoc))) {
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

  /** Returns number of times the value is saved in memory (stack variables, heap etc.) */
  public int getNumberOfValueUsages(Value value) {
    return memoryModel.getNumberOfValueUsages(value);
  }

  @SuppressWarnings("unused")
  private boolean experimentalNestedListFilter(SMGState pOther) {
    if (!(memoryModel.getHeapObjectsMinSize() >= pOther.memoryModel.getHeapObjectsMinSize())) {
      return false;
    }

    // Ordered (by min len) linked list segments in both states
    List<SMGSinglyLinkedListSegment> thisValidAbstrObjs =
        ImmutableList.sortedCopyOf(
            Comparator.comparingInt(SMGSinglyLinkedListSegment::getMinLength),
            getMemoryModel().getSmg().getAllValidAbstractedObjects());
    List<SMGSinglyLinkedListSegment> otherValidAbstrObjs =
        new ArrayList<>(pOther.getMemoryModel().getSmg().getAllValidAbstractedObjects());
    otherValidAbstrObjs.sort(Comparator.comparingInt(SMGSinglyLinkedListSegment::getMinLength));

    // Now check that every linked list segment in this has a smaller or equal equivalent in pOther
    for (SMGSinglyLinkedListSegment thisLL : thisValidAbstrObjs) {
      int thisMin = thisLL.getMinLength();
      // Search for the closest in pOther and kick it out
      for (int i = 0; i < otherValidAbstrObjs.size(); i++) {
        int otherMin = otherValidAbstrObjs.get(i).getMinLength();
        if (thisMin < otherMin) {
          // Kick out the one before
          if (i == 0) {
            return false;
          } else {
            otherValidAbstrObjs.remove(i);
            break;
          }
        }
        if (i == otherValidAbstrObjs.size() - 1) {
          // All in otherValidAbstrObjs are smaller, kick out any
          otherValidAbstrObjs.remove(i);
        }
      }
      // If none can be found -> false
    }
    if (!otherValidAbstrObjs.isEmpty()) {
      return false;
    }
    return true;
  }

  @Override
  public SMGState join(SMGState other) throws CPAException, InterruptedException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isLessOrEqual(SMGState pOther) throws CPAException, InterruptedException {
    // This state needs the same amount of variables as the other state
    if (getSize() != pOther.getSize()) {
      return false;
    }

    if (getMemoryModel().getSmg().getNumberOfAbstractedLists()
        != pOther.getMemoryModel().getSmg().getNumberOfAbstractedLists()) {
      return false;
      // TODO: think about this more
    }

    // This removed unused symbolic values from the constraints
    if (!pOther
        .removeOldConstraints()
        .getConstraints()
        .containsAll(removeOldConstraints().getConstraints())) {
      // TODO: Problem: there might still be distinct symbolic values with the same constraints.
      //   => Compare those by location.
      //   Example: imagine a loop, the loop bound may be against a nondet() function,
      //     the comparison is always i (concretely known, for example 1) < nondet().
      //     The nondet() might be reassigned each loop, thus different.
      //     But since the constraint is equal for location, that would be OK!
      return false;
    }

    // We may not forget any errors already found
    if (!copyAndPruneUnreachable()
        .checkErrorEqualityForTwoStates(pOther.copyAndPruneUnreachable())) {
      return false;
    }

    // Cache value equalities and object (shape) equalities that we already found
    EqualityCache<Value> equalityCache = EqualityCache.of();
    EqualityCache<SMGObject> objectCache = EqualityCache.of();
    // Check that both have the same stack frames
    if (!checkStackFrameEqualityForTwoStates(pOther, equalityCache, objectCache)) {
      return false;
    }

    // Check that the values of all variables (local and global) are either equal or
    // overapproximated and that the memory is equal (such that the shape of memory reachable by
    // pointers is lessOrEqual)
    // Validity is checked while checking values and the shape!
    // There might linger some invalidated memory with no connection and that's fine.
    return checkEqualityOfMemoryForTwoStates(pOther, equalityCache, objectCache);
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
      EqualityCache<Value> equalityCache,
      EqualityCache<SMGObject> objectCache,
      boolean treatSymbolicsAsEqualWEqualConstrains) {
    return areValuesEqual(
        thisState,
        thisValue,
        otherState,
        otherValue,
        ImmutableMap.of(),
        equalityCache,
        objectCache,
        new HashSet<>(),
        treatSymbolicsAsEqualWEqualConstrains,
        false);
  }

  /**
   * This is UNSOUND! But a good enough estimation. Checks if list materialization generates
   * symbolic values that are equal for abstraction. Better: track all symbolic values generated
   * from list materialization and treat them as equal for equal constraints.
   *
   * @param pOther the other state compared in lessOrEquals.
   * @return true if symbolics are to be treated as equal for equal constrains. False else.
   */
  private boolean treatSymbolicsAsEqualWEqualConstrains(
      SMGState pOther, @Nullable MemoryLocation possibleMemLoc) {
    Set<SMGSinglyLinkedListSegment> allAbstr =
        getMemoryModel().getSmg().getAllValidAbstractedObjects();
    if (allAbstr.isEmpty()) {
      return false;
    }
    if (possibleMemLoc != null
        && possibleMemLoc.getExtendedQualifiedName().contains("__CPAchecker_TMP_")) {
      return true;
    }
    Set<SMGSinglyLinkedListSegment> otherAllAbstr =
        pOther.getMemoryModel().getSmg().getAllValidAbstractedObjects();
    if (!otherAllAbstr.isEmpty()
        && !allAbstr.stream().allMatch(ll -> ll.getRelevantEqualities().primitiveCache.isEmpty())
        && !otherAllAbstr.stream()
            .allMatch(ll -> ll.getRelevantEqualities().primitiveCache.isEmpty())) {
      return true;
    }
    return false;
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
        thisState,
        thisValue,
        otherState,
        otherValue,
        ImmutableMap.of(),
        new EqualityCache<>(),
        new EqualityCache<>(),
        new HashSet<>(),
        false,
        false);
  }

  /**
   * Removes {@link Constraint}s that consists of unused values only (that are unused in other
   * constraints).
   */
  SMGState removeOldConstraints() {
    ConstantSymbolicExpressionLocator symIdentVisitor =
        ConstantSymbolicExpressionLocator.getInstance();
    // There are 3 sources of constraints, values in objects (HVEs), offsets and sizes.

    Set<SymbolicIdentifier> allUsedIdentifiers = new HashSet<>();
    for (SMGPointsToEdge pte : getMemoryModel().getSmg().getPTEdges()) {
      Value value = pte.getOffset();
      // Get all symbolic values in offsets (they might not have a SMGValue mapping anymore below!)
      if (value instanceof SymbolicValue symValue) {
        for (ConstantSymbolicExpression constSym : symValue.accept(symIdentVisitor)) {
          if (constSym.getValue() instanceof SymbolicIdentifier symIdent) {
            allUsedIdentifiers.add(symIdent);
          }
        }
      }
    }

    for (SMGObject obj : getMemoryModel().getSmg().getObjects()) {
      Value value = obj.getSize();
      // Get all symbolic values in sizes (they might not have a SMGValue mapping anymore below!)
      if (value instanceof SymbolicValue symValue) {
        for (ConstantSymbolicExpression constSym : symValue.accept(symIdentVisitor)) {
          if (constSym.getValue() instanceof SymbolicIdentifier symIdent) {
            allUsedIdentifiers.add(symIdent);
          }
        }
      }
    }

    // First, get all SMGValues for possible identifier
    for (Entry<Wrapper<Value>, SMGValue> mapping :
        memoryModel.getValueToSMGValueMapping().entrySet()) {
      Value value = mapping.getKey().get();
      SMGValue smgValue = mapping.getValue();
      if (memoryModel.getSmg().getNumberOfSMGValueUsages(smgValue) > 0) {
        if (value instanceof SymbolicValue symValue) {
          for (ConstantSymbolicExpression constSym : symValue.accept(symIdentVisitor)) {
            if (constSym.getValue() instanceof SymbolicIdentifier symIdent) {
              allUsedIdentifiers.add(symIdent);
            }
          }
        }
      }
    }

    boolean done = false;
    ImmutableSet.Builder<Constraint> constraintsToKeep = ImmutableSet.builder();
    Set<Constraint> constraintsToCheck = new HashSet<>(getConstraints());
    Map<Constraint, Set<SymbolicIdentifier>> constraintsToIdentsInside = new HashMap<>();
    while (!done) {
      done = true;
      for (Iterator<Constraint> constraintIter = constraintsToCheck.iterator();
          constraintIter.hasNext(); ) {
        Constraint constraint = constraintIter.next();
        Set<SymbolicIdentifier> identsInConstraint = constraintsToIdentsInside.get(constraint);
        if (identsInConstraint == null) {
          identsInConstraint = memoryModel.getSymbolicIdentifiersForValue(constraint);
          constraintsToIdentsInside.put(constraint, identsInConstraint);
        }

        if (!Collections.disjoint(allUsedIdentifiers, identsInConstraint)) {
          // Shares idents, remove all useful constraints from constraintIter
          allUsedIdentifiers.addAll(identsInConstraint);
          constraintsToKeep.add(constraint);
          constraintIter.remove();
          done = false;
        }
      }
    }

    // The constraints left in constraintsToCheck could not be removed -> they are useless
    return copyWithNewConstraints(constraintsToKeep.build());
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
      Map<SMGObject, List<BigInteger>> exemptOffsetsPerObject,
      EqualityCache<Value> equalityCache,
      EqualityCache<SMGObject> objectCache,
      Set<Value> thisAlreadyCheckedPointers,
      boolean treatSymbolicsAsEqualWEqualConstrains,
      boolean allowAbstractedSelfPointers) {
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
          exemptOffsetsPerObject,
          equalityCache,
          objectCache,
          thisAlreadyCheckedPointers,
          treatSymbolicsAsEqualWEqualConstrains,
          allowAbstractedSelfPointers)) {
        equalityCache.addEquality(thisValue, otherValue);
        return true;
      }
      // Possibly 2 symbolic values that are equal by constraints

      return false;
    }

    // Unknowns in this current CPA implementation are not comparable in different states!
    // Each state generates a unique ConstantSymbolicExpression id (as its statically generated)
    // Comparable is only that both are ConstantSymbolicExpressions and the type matches and
    // that they do represent the same location
    if ((thisValue instanceof SymbolicExpression thisSymExpr
        && otherValue instanceof SymbolicExpression otherSymExpr
        && thisSymExpr.getType().equals(otherSymExpr.getType()))) {
      // Note: SymbolicIdentifier without const expr wrapper are pointers,
      //   while those with wrapper are values
      if (thisValue.equals(otherValue)) {
        return true;

      } else if (treatSymbolicsAsEqualWEqualConstrains
          && equalConstraintsInSymbolicValues(thisValue, thisState, otherValue, otherState)) {
        // Check matching constraints
        equalityCache.addEquality(thisValue, otherValue);
        return true;
      } else if (options.isTreatSymbolicValuesAsUnknown()) {
        return true;
      }
    }

    return thisValue.equals(otherValue);
  }

  private boolean equalConstraintsInSymbolicValues(
      Value pThisValue, SMGState thisState, Value pOtherValue, SMGState otherState) {
    return !thisState.valueContainedInConstraints(pThisValue)
        && !otherState.valueContainedInConstraints(pOtherValue);
    // TODO: find a way to find Constraints with specific symbolic values in them
    // TODO: build visitor that replaces symbolic values in constraints
    // TODO: carry a SymbolicGenerator Object with all found SymbolicValues that can be replaced by
    // it
    //  Important/Difficulty: make sure the symbolic value relations to occurrences outside of the
    // list are respected somehow
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
      Map<SMGObject, List<BigInteger>> exemptOffsetsPerObject,
      EqualityCache<Value> equalityCache,
      EqualityCache<SMGObject> objectCache,
      Set<Value> thisAlreadyCheckedPointers,
      boolean treatSymbolicsAsEqualWEqualConstrains,
      boolean allowAbstractedSelfPointers) {
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

      Value thisObjSize = thisObj.getSize();
      Value otherObjSize = otherObj.getSize();
      Value thisDerefOffset = thisDerefObjAndOffset.getOffsetForObject();
      Value otherDerefOffset = otherDerefObjAndOffset.getOffsetForObject();
      SMGTargetSpecifier thisSpecifier = thisState.memoryModel.getPointerSpecifier(thisAddress);
      SMGTargetSpecifier otherSpecifier = otherState.memoryModel.getPointerSpecifier(otherAddress);
      if (!thisDerefOffset.equals(otherDerefOffset)) {
        return false;
      } else if (!thisSpecifier.equals(otherSpecifier)) {
        // For abstracting self-pointers in linked lists,
        //   we allow all to be equal to region while abstracting
        boolean thisEqAll = thisSpecifier.equals(SMGTargetSpecifier.IS_ALL_POINTER);
        boolean otherEqAll = otherSpecifier.equals(SMGTargetSpecifier.IS_ALL_POINTER);
        boolean thisEqReg = thisSpecifier.equals(SMGTargetSpecifier.IS_REGION);
        boolean otherEqReg = otherSpecifier.equals(SMGTargetSpecifier.IS_REGION);
        if (!(allowAbstractedSelfPointers
            && ((thisEqAll && otherEqReg) || (otherEqAll && thisEqReg)))) {
          return false;
        }
      } else if (!(thisObjSize.equals(otherObjSize)
          || !thisObj.getOffset().equals(otherObj.getOffset()))) {
        return false;
      } else if (this != otherState && thisObj.getNestingLevel() != otherObj.getNestingLevel()) {
        // lessOrEquals
        return false;
      } else if (this == otherState) {
        // Equal heap comparison of the same state, e.g. abstraction
        if (thisObj.getNestingLevel() > otherObj.getNestingLevel()
            && thisObj.getNestingLevel() - otherObj.getNestingLevel() != 1) {
          return false;
        } else if (thisObj.getNestingLevel() < otherObj.getNestingLevel()
            && otherObj.getNestingLevel() - thisObj.getNestingLevel() != 1) {
          return false;
        }
      }

      if (thisObj instanceof SMGSinglyLinkedListSegment
          || otherObj instanceof SMGSinglyLinkedListSegment) {
        return checkAbstractedListEquality(
            thisState,
            thisObj,
            otherState,
            otherObj,
            exemptOffsetsPerObject,
            equalityCache,
            objectCache,
            thisAlreadyCheckedPointers,
            treatSymbolicsAsEqualWEqualConstrains,
            allowAbstractedSelfPointers);
      }

      if (!getMemoryModel().isObjectValid(thisObj)
          && !otherState.getMemoryModel().isObjectValid(otherObj)) {
        // both invalid (we checked sizes etc. already)
        return true;
      }

      if (objectCache.isEqualityKnown(thisObj, otherObj)) {
        return true;
      } else {
        // Assume them to be equal from here on, this way we don't check them multiple times
        // This also means we need to remove it if it's found to be not equal!
        objectCache.addEquality(thisObj, otherObj);
      }

      if (checkEqualValuesForTwoStatesWithExemptions(
          thisObj,
          otherObj,
          exemptOffsetsPerObject,
          thisState,
          otherState,
          equalityCache,
          objectCache,
          thisAlreadyCheckedPointers,
          treatSymbolicsAsEqualWEqualConstrains,
          allowAbstractedSelfPointers)) {
        return true;
      }
      objectCache.removeEquality(thisObj, otherObj);
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
      Map<SMGObject, List<BigInteger>> exemptOffsetsPerObject,
      EqualityCache<Value> equalityCache,
      EqualityCache<SMGObject> objectCache,
      Set<Value> thisPointerValueAlreadyVisited,
      boolean treatSymbolicsAsEqualWEqualConstrains,
      boolean allowAbstractedSelfPointers) {

    if (objectCache.isEqualityKnown(thisObj, otherObj)) {
      return true;
    }
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
      if (thisSLL.getNextOffset().compareTo(otherSLL.getNextOffset()) != 0
          && thisSLL.getHeadOffset().compareTo(otherSLL.getHeadOffset()) != 0) {
        return false;
      }
      if (otherObj instanceof SMGDoublyLinkedListSegment otherDLL
          && thisObj instanceof SMGDoublyLinkedListSegment thisDLL) {
        if (thisDLL.getPrevOffset().compareTo(otherDLL.getPrevOffset()) != 0) {
          // Check that the values are equal and that the back pointer is as well
          return false;
        }
      }

      if (this == otherState && thisSLL.getMinLength() != otherSLL.getMinLength()) {
        // Not lessOrEqual, but true equality check
        return false;
      }

      if (thisSLL.getMinLength() >= otherSLL.getMinLength()) {
        // This is a look through case (either one subsumses the other by being larger,
        //   or potentially larger with 0+, or a 0+ is at some point found in the list later on)
        // FIXME: missing cases

        // Check that the values are equal and that the next and back pointers are as well
        // If we check 2 lists that are connected, the exemption list will exclude
        //   nfo/pfo for those objects
        return checkEqualValuesForTwoStatesWithExemptions(
            thisSLL,
            otherSLL,
            exemptOffsetsPerObject,
            thisState,
            otherState,
            equalityCache,
            objectCache,
            thisPointerValueAlreadyVisited,
            treatSymbolicsAsEqualWEqualConstrains,
            allowAbstractedSelfPointers);

      } else if (exemptOffsetsPerObject.containsKey(thisSLL)
          && exemptOffsetsPerObject.containsKey(otherSLL)) {
        List<BigInteger> thisExempt = exemptOffsetsPerObject.get(thisSLL);
        List<BigInteger> otherExempt = exemptOffsetsPerObject.get(otherSLL);
        Set<BigInteger> thisListOffsets = ImmutableSet.of(thisSLL.getNextOffset());
        Set<BigInteger> otherListOffsets = ImmutableSet.of(otherSLL.getNextOffset());
        if (otherObj instanceof SMGDoublyLinkedListSegment otherDLL
            && thisObj instanceof SMGDoublyLinkedListSegment thisDLL) {
          thisListOffsets = ImmutableSet.of(thisSLL.getNextOffset(), thisDLL.getPrevOffset());
          otherListOffsets = ImmutableSet.of(otherSLL.getNextOffset(), otherDLL.getPrevOffset());
        }
        if (thisExempt.containsAll(thisListOffsets) && otherExempt.containsAll(otherListOffsets)) {
          // We only compare the content of 2 lists (e.g. 0+ and 3+ merging)
          return checkEqualValuesForTwoStatesWithExemptions(
              thisSLL,
              otherSLL,
              exemptOffsetsPerObject,
              thisState,
              otherState,
              equalityCache,
              objectCache,
              thisPointerValueAlreadyVisited,
              treatSymbolicsAsEqualWEqualConstrains,
              allowAbstractedSelfPointers);
        }
      }
    } else {
      // We could end up here because of abstraction with one abstracted element and a
      // concrete element and both have a self pointer and next/prev pointers blocked
      List<BigInteger> exemptOffsetsThis = exemptOffsetsPerObject.get(otherObj);
      List<BigInteger> exemptOffsetsOther = exemptOffsetsPerObject.get(otherObj);
      BigInteger neededNext;
      BigInteger neededPrev = null;
      if (exemptOffsetsOther == null
          || exemptOffsetsThis == null
          || exemptOffsetsThis.isEmpty()
          || exemptOffsetsOther.isEmpty()) {
        return false;
      }

      if (otherObj instanceof SMGSinglyLinkedListSegment otherSLL) {
        Preconditions.checkArgument(!(thisObj instanceof SMGSinglyLinkedListSegment));
        neededNext = otherSLL.getNextOffset();
        if (otherObj instanceof SMGDoublyLinkedListSegment otherDLL) {
          neededPrev = otherDLL.getPrevOffset();
        }
      } else {
        SMGSinglyLinkedListSegment thisSLL = (SMGSinglyLinkedListSegment) thisObj;
        Preconditions.checkArgument(!(otherObj instanceof SMGSinglyLinkedListSegment));
        neededNext = thisSLL.getNextOffset();
        if (thisObj instanceof SMGDoublyLinkedListSegment thisDLL) {
          neededPrev = thisDLL.getPrevOffset();
        }
      }

      if (exemptOffsetsOther.contains(neededNext) && exemptOffsetsThis.contains(neededNext)) {
        if (neededPrev != null) {
          if (!exemptOffsetsOther.contains(neededPrev) || !exemptOffsetsThis.contains(neededPrev)) {
            return false;
          }
        }
        return checkEqualValuesForTwoStatesWithExemptions(
            thisObj,
            otherObj,
            exemptOffsetsPerObject,
            thisState,
            otherState,
            equalityCache,
            objectCache,
            thisPointerValueAlreadyVisited,
            treatSymbolicsAsEqualWEqualConstrains,
            allowAbstractedSelfPointers);
      }

      // Don't check for equality of abstracted and concrete lists for lessOrEqual!
      return false;
    }

    return false;
  }

  /**
   * Compare 2 values, but do not compare the exempt offsets. Compares pointers by shape of the
   * memory they point to. Needed for lists and their next/prev pointers.
   *
   * @param thisObject object of the this state to compare.
   * @param otherObject object of the other state to compare.
   * @param exemptOffsetsPerObject exempt offsets, e.g. nfo, pfo offsets per object (objects might
   *     be visited again! through pointers).
   * @param thisState the state to which the this object belongs.
   * @param otherState the state to which the other object belongs.
   * @param equalityCache basic value check cache.
   * @param treatSymbolicsAsEqualWEqualConstrains true if you want 2 non-equal symbolic variables
   *     with the same constraints to be treated as equals.
   * @return true if the 2 memory sections given are equal. False else.
   */
  public boolean checkEqualValuesForTwoStatesWithExemptions(
      SMGObject thisObject,
      SMGObject otherObject,
      Map<SMGObject, List<BigInteger>> exemptOffsetsPerObject,
      SMGState thisState,
      SMGState otherState,
      EqualityCache<Value> equalityCache,
      EqualityCache<SMGObject> objectCache,
      boolean treatSymbolicsAsEqualWEqualConstrains,
      boolean allowAbstractedSelfPointers) {
    return checkEqualValuesForTwoStatesWithExemptions(
        thisObject,
        otherObject,
        exemptOffsetsPerObject,
        thisState,
        otherState,
        equalityCache,
        objectCache,
        new HashSet<>(),
        treatSymbolicsAsEqualWEqualConstrains,
        allowAbstractedSelfPointers);
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
  public boolean checkEqualValuesForTwoStatesWithExemptions(
      SMGObject thisObject,
      SMGObject otherObject,
      Map<SMGObject, List<BigInteger>> exemptOffsets,
      SMGState thisState,
      SMGState otherState,
      EqualityCache<Value> equalityCache,
      EqualityCache<SMGObject> objectCache) {
    return checkEqualValuesForTwoStatesWithExemptions(
        thisObject,
        otherObject,
        exemptOffsets,
        thisState,
        otherState,
        equalityCache,
        objectCache,
        new HashSet<>(),
        false,
        false);
  }

  // Test only version of checkEqualValuesForTwoStatesWithExemptions
  public boolean checkEqualValuesForTwoStatesWithExemptions(
      SMGObject thisObject,
      SMGObject otherObject,
      Map<SMGObject, List<BigInteger>> exemptOffsets,
      SMGState thisState,
      SMGState otherState,
      EqualityCache<Value> equalityCache) {
    return checkEqualValuesForTwoStatesWithExemptions(
        thisObject,
        otherObject,
        exemptOffsets,
        thisState,
        otherState,
        equalityCache,
        EqualityCache.of(),
        new HashSet<>(),
        false,
        false);
  }

  /**
   * Compare 2 values, but do not compare the exempt offsets. Compares pointers by shape of the
   * memory they point to. Needed for lists and their next/prev pointers.
   *
   * @param thisObject object of the thisState to compare.
   * @param otherObject object of the otherState to compare.
   * @param exemptOffsetsPerObject exempt offsets, e.g. nfo, pfo offsets per object (objects might
   *     be visited again! through pointers).
   * @param thisState the state to which the thisObject belongs.
   * @param otherState the state to which the otherObject belongs.
   * @param equalityCache basic value check cache.
   * @return true if the 2 memory sections given are equal. False else.
   */
  private boolean checkEqualValuesForTwoStatesWithExemptions(
      SMGObject thisObject,
      SMGObject otherObject,
      Map<SMGObject, List<BigInteger>> exemptOffsetsPerObject,
      SMGState thisState,
      SMGState otherState,
      EqualityCache<Value> equalityCache,
      EqualityCache<SMGObject> objectCache,
      Set<Value> thisPointerValuesAlreadyVisited,
      boolean treatSymbolicsAsEqualWEqualConstrains,
      boolean allowAbstractedSelfPointers) {

    Preconditions.checkArgument(
        thisState.getMemoryModel().getSmg().getObjects().contains(thisObject));
    Preconditions.checkArgument(
        otherState.getMemoryModel().getSmg().getObjects().contains(otherObject));

    Map<BigInteger, SMGHasValueEdge> otherOffsetToHVEdgeMap = new HashMap<>();
    for (SMGHasValueEdge hve :
        otherState
            .memoryModel
            .getSmg()
            .getSMGObjectsWithSMGHasValueEdges()
            .getOrDefault(otherObject, PersistentSet.of())) {
      if (!exemptOffsetsPerObject
          .getOrDefault(otherObject, ImmutableList.of())
          .contains(hve.getOffset())) {
        otherOffsetToHVEdgeMap.put(hve.getOffset(), hve);
      }
    }

    Map<BigInteger, SMGHasValueEdge> thisOffsetToHVEdgeMap = new HashMap<>();

    // Check all HVEs for this object are present in other object
    for (SMGHasValueEdge hve :
        thisState
            .memoryModel
            .getSmg()
            .getSMGObjectsWithSMGHasValueEdges()
            .getOrDefault(thisObject, PersistentSet.of())) {
      if (!exemptOffsetsPerObject
          .getOrDefault(otherObject, ImmutableList.of())
          .contains(hve.getOffset())) {
        thisOffsetToHVEdgeMap.put(hve.getOffset(), hve);
        if (memoryModel.getSmg().isPointer(hve.hasValue())) {
          // Pointers are necessary!!!!
          SMGHasValueEdge otherHVE = otherOffsetToHVEdgeMap.get(hve.getOffset());
          if (otherHVE == null) {
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
      if (thisObject instanceof SMGSinglyLinkedListSegment
          && otherObject instanceof SMGSinglyLinkedListSegment) {
        treatSymbolicsAsEqualWEqualConstrains = true;
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
          exemptOffsetsPerObject,
          equalityCache,
          objectCache,
          thisPointerValuesAlreadyVisited,
          treatSymbolicsAsEqualWEqualConstrains,
          allowAbstractedSelfPointers)) {
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
          exemptOffsetsPerObject,
          equalityCache,
          objectCache,
          thisPointerValuesAlreadyVisited,
          treatSymbolicsAsEqualWEqualConstrains,
          allowAbstractedSelfPointers)) {
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
  public boolean areNonEqualAddresses(Value pValue1, Value pValue2) throws SMGSolverException {
    Optional<SMGValue> smgValue1 = memoryModel.getSMGValueFromValue(pValue1);
    Optional<SMGValue> smgValue2 = memoryModel.getSMGValueFromValue(pValue2);
    if (smgValue1.isEmpty() || smgValue2.isEmpty()) {
      // The return value should not matter here as this is checked before
      return true;
    }
    return proveInequality(smgValue1.orElseThrow(), smgValue2.orElseThrow());
  }

  /**
   * Tries to check for inequality of 2 {@link SMGValue}s used in the SMG of this {@link
   * SymbolicProgramConfiguration}. This does NOT check the (concrete) CValues of the entered
   * values, but only if they refer to the same memory location in the SMG or not!
   *
   * @param pValue1 A {@link SMGValue} to be checked for inequality with pValue2.
   * @param pValue2 A {@link SMGValue} to be checked for inequality with pValue1.
   * @return True if the 2 {@link SMGValue}s are not equal, false if they are equal.
   */
  public boolean proveInequality(SMGValue pValue1, SMGValue pValue2) throws SMGSolverException {
    // Can this be solved without creating a new SMGProveNequality every time?
    // TODO: Since we need to rework the values anyway, make a new class for this.
    SMGProveNequality nequality = new SMGProveNequality(this);
    return nequality.proveInequality(pValue1, pValue2);
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
    return targetEdge.getOffset();
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

  public SMGState copyAndPruneUnreachable(CFAEdge edge) throws SMGException {
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
        throw new SMGException("Error in tracking abstracted list memory for materialization.");
      }
    }

    return copyAndReplaceMemoryModel(newHeap).copyWithMemLeak(unreachableObjects, edge);
  }

  /*
   * Remove the entered object from the heap and delete it internally and general memory mappings.
   * Also, all has-value-edges are pruned. Nothing else.
   */
  public SMGState copyAndRemoveAbstractedObjectFromHeap(SMGObject obj) {
    return copyAndReplaceMemoryModel(memoryModel.copyAndRemoveAbstractedObjectFromHeap(obj));
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
   * To be used if a malloc(0) returned pointer is evaluated.
   *
   * @param readPointer the pointer {@link Value} that was evaluated.
   * @return A new SMGState with the error info.
   */
  public SMGState withInvalidReadOfMallocZeroPointer(Value readPointer) {
    if (readPointer instanceof AddressExpression addrExpr) {
      readPointer = addrExpr.getMemoryAddress();
    }
    String errorMSG = "Invalid evaluation of malloc(0) returned address: " + readPointer + ".";
    SMGValue smgPointer = getMemoryModel().getSMGValueFromValue(readPointer).orElseThrow();
    SMGErrorInfo newErrorInfo =
        SMGErrorInfo.of()
            .withProperty(Property.INVALID_READ)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(
                Collections.singleton(
                    getMemoryModel().getSmg().getPTEdge(smgPointer).orElseThrow().pointsTo()));
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
      SMGObject objectWrittenTo, Value writeOffset, Value writeSize, Value pValue, CFAEdge edge) {

    if (getMemoryModel().isHeapObject(objectWrittenTo)) {
      // Invalid deref
      return withInvalidDeref(objectWrittenTo, edge);
    }

    int lineInOrigin = edge.getFileLocation().getStartingLineInOrigin();
    String errorMSG =
        "Try writing value "
            + pValue
            + " with size "
            + writeSize
            + " at offset "
            + writeOffset
            + " bit to object sized "
            + objectWrittenTo.getSize()
            + " bit in line "
            + lineInOrigin
            + ".";
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
      return withOutOfRangeWrite(objectWrittenTo, writeOffset, writeSize, pValue, edge);
    }

    // TODO: get model for offset
    int lineInOrigin = edge.getFileLocation().getStartingLineInOrigin();
    String errorMSG =
        "Try writing value "
            + pValue
            + " with size "
            + writeSize
            + " at unknown possible offset bit to object sized "
            + objectWrittenTo.getSize()
            + " bit in line "
            + lineInOrigin
            + ".";
    SMGErrorInfo newErrorInfo =
        SMGErrorInfo.of()
            .withProperty(Property.INVALID_WRITE)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(objectWrittenTo));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithNewErrorInfo(newErrorInfo);
  }

  public SMGState withInvalidDerefForRead(SMGObject objectDerefed, CFAEdge edge) {
    Preconditions.checkArgument(!getMemoryModel().isObjectValid(objectDerefed));

    String errorMSG = "valid-deref: invalid pointer dereference in : " + edge;
    SMGErrorInfo newErrorInfo =
        SMGErrorInfo.of()
            .withProperty(Property.INVALID_READ)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(objectDerefed));

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
            .withProperty(Property.INVALID_WRITE)
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
   * @param readOffset The offset in bits as {@link Value} where you want to read. Might be
   *     symbolic!
   * @param readSize the size of the type in bits to read as {@link BigInteger}.
   * @return A new SMGState with the error info.
   */
  public SMGState withOutOfRangeRead(SMGObject objectRead, Value readOffset, Value readSize) {
    // TODO: extract model for readOffset and print here
    if (readOffset.isNumericValue() && readSize.isNumericValue()) {
      return withOutOfRangeRead(
          objectRead,
          readOffset.asNumericValue().bigIntegerValue(),
          readSize.asNumericValue().bigIntegerValue());
    }
    String errorMSG =
        "Try reading object "
            + objectRead
            + " with size "
            + objectRead.getSize()
            + " bits at offset "
            + readOffset
            + " bit with read type size "
            + readSize
            + " bit";
    SMGErrorInfo newErrorInfo =
        SMGErrorInfo.of()
            .withProperty(Property.INVALID_READ)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(objectRead));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithNewErrorInfo(newErrorInfo);
  }

  public SMGState withOutOfRangeRead(
      SMGObject objectRead, BigInteger readOffset, BigInteger readSize) {
    // TODO: extract model for readOffset and print here
    String sizeToPrint = objectRead.getSize().toString();
    if (objectRead.getSize().isNumericValue()) {
      sizeToPrint = objectRead.getSize().asNumericValue().bigIntegerValue().toString();
    }
    String errorMSG =
        "Try reading object "
            + objectRead
            + " with size "
            + sizeToPrint
            + " bits at offset "
            + readOffset
            + " bit with read type size "
            + readSize
            + " bit";
    SMGErrorInfo newErrorInfo =
        SMGErrorInfo.of()
            .withProperty(Property.INVALID_READ)
            .withErrorMessage(errorMSG)
            .withInvalidObjects(Collections.singleton(objectRead));
    // Log the error in the logger
    logMemoryError(errorMSG, true);
    return copyWithNewErrorInfo(newErrorInfo);
  }

  public SMGState withOutOfRangeRead(SMGObject objectRead, Value readOffset, BigInteger readSize) {
    // TODO: extract model for readOffset and print here
    if (readOffset.isNumericValue()) {
      return withOutOfRangeRead(
          objectRead, readOffset.asNumericValue().bigIntegerValue(), readSize);
    }
    String errorMSG =
        "Try reading object "
            + objectRead
            + " with size "
            + objectRead.getSize()
            + " bits at offset "
            + readOffset
            + " bit with read type size "
            + readSize
            + " bit";
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
        evaluator,
        statistics);
  }

  /** Returns memory model, including Heap, stack and global vars. */
  public SymbolicProgramConfiguration getMemoryModel() {
    return memoryModel;
  }

  /**
   * Add the {@link Value} mapping if it was not mapped to a {@link SMGValue}, if it was already
   * present the state is unchanged and the known {@link SMGValue} returned. The {@link SMGValue} is
   * also added to the SMG with nesting level 0 if not present.
   *
   * @param pValue the {@link Value} you want to add to the SPC.
   * @return a copy of the current {@link SMGState} with the mapping of the {@link Value} to its
   *     {@link SMGValue} entered if it was not mapped, if it was already present the state is
   *     unchanged and the known {@link SMGValue} returned.
   */
  public SMGValueAndSMGState copyAndAddValue(Value pValue, CType type) {

    Optional<SMGValue> maybeValue = memoryModel.getSMGValueFromValue(pValue);
    CType canonizedType = SMGCPAExpressionEvaluator.getCanonicalType(type);
    if (maybeValue.isPresent()) {
      SMGValue existingSMGValue = maybeValue.orElseThrow();
      // Note: type information is not preserved here!
      // Reusing a value with a differing type will lead to the old type being associated with the
      // value!
      return SMGValueAndSMGState.of(this, existingSMGValue);
    } else {
      SMGValue newSMGValue = SMGValue.of();
      return SMGValueAndSMGState.of(
          copyAndReplaceMemoryModel(
              memoryModel.copyAndPutValue(pValue, newSMGValue, 0, canonizedType)),
          newSMGValue);
    }
  }

  public boolean valueContainedInConstraints(Value pValue) {
    // TODO: this currently is a quick and dirty fix. Do properly.
    Set<SymbolicIdentifier> symIdents = ImmutableSet.of();
    if (pValue instanceof SymbolicExpression symExpr) {
      symIdents = symExpr.accept(SymbolicIdentifierLocator.getInstance());
    }
    Set<Constraint> constraints = getConstraints();
    if (!symIdents.isEmpty()) {
      for (Constraint co : constraints) {
        Set<SymbolicIdentifier> symIdentsConstr =
            co.accept(SymbolicIdentifierLocator.getInstance());
        if (!Collections.disjoint(symIdentsConstr, symIdents)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Add the {@link Value} mapping if it was not mapped to a {@link SMGValue}, if it was already
   * present the state is changed to the new nesting level and the known {@link SMGValue} returned.
   * The {@link SMGValue} is also added to the SMG with the nesting level given if not already in
   * the SMG.
   *
   * @param pValue the {@link Value} you want to add to the SPC.
   * @param nestingLevel the new nesting level for the SMG value.
   * @return a copy of the current {@link SMGState} with the mapping of the {@link Value} to its
   *     {@link SMGValue} entered if it was not mapped, if it was already present the state is
   *     changed so that the nesting level is updated and the known {@link SMGValue} is returned.
   */
  public SMGValueAndSMGState copyAndAddValue(Value pValue, CType type, int nestingLevel) {
    Optional<SMGValue> maybeValue = memoryModel.getSMGValueFromValue(pValue);
    SMGValue newSMGValue;
    if (maybeValue.isPresent()) {
      newSMGValue = maybeValue.orElseThrow();
    } else {
      newSMGValue = SMGValue.of();
    }
    return SMGValueAndSMGState.of(
        copyAndReplaceMemoryModel(
            memoryModel.copyAndPutValue(pValue, newSMGValue, nestingLevel, type)),
        newSMGValue);
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
  public Optional<SMGObjectAndOffsetMaybeNestingLvl> getPointsToTarget(Value pValue) {
    Optional<SMGValue> addressOptional = memoryModel.getSMGValueFromValue(pValue);
    if (addressOptional.isPresent()) {
      Optional<SMGPointsToEdge> pointerEdgeOptional =
          memoryModel.getSmg().getPTEdge(addressOptional.orElseThrow());
      if (pointerEdgeOptional.isPresent()) {
        return Optional.of(
            SMGObjectAndOffsetMaybeNestingLvl.of(
                pointerEdgeOptional.orElseThrow().pointsTo(),
                pointerEdgeOptional.orElseThrow().getOffset()));
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
  private ValueAndSMGState searchOrCreateAddressForAddressExpr(Value pValue, CType type)
      throws SMGException {
    if (pValue instanceof AddressExpression addressExprValue) {
      Value offsetAddr = addressExprValue.getOffset();

      if (offsetAddr.isNumericValue()
          && offsetAddr.asNumericValue().bigIntegerValue().equals(BigInteger.ZERO)) {
        return ValueAndSMGState.of(pValue, this);
      }

      Optional<SMGObjectAndOffsetMaybeNestingLvl> maybeTargetAndOffset =
          getPointsToTarget(addressExprValue.getMemoryAddress());
      if (maybeTargetAndOffset.isEmpty()) {
        return ValueAndSMGState.ofUnknownValue(
            this,
            "Returned unknown value because the target of a requested pointer could not be found.");
      }
      SMGObjectAndOffsetMaybeNestingLvl targetAndOffset = maybeTargetAndOffset.orElseThrow();

      SMGObject target = targetAndOffset.getSMGObject();
      Value offsetPointer = targetAndOffset.getOffsetForObject();
      Value offsetOverall = evaluator.addBitOffsetValues(offsetPointer, offsetAddr);
      SMGTargetSpecifier specifier = SMGTargetSpecifier.IS_REGION;
      assert !(target instanceof SMGSinglyLinkedListSegment);
      Preconditions.checkArgument(
          0 == getMemoryModel().getNestingLevel(addressExprValue.getMemoryAddress()));
      // search for existing pointer first and return if found; else make a new one
      ValueAndSMGState addressAndState =
          searchOrCreateAddress(target, type, offsetOverall, 0, specifier);
      return ValueAndSMGState.of(
          AddressExpression.withZeroOffset(addressAndState.getValue(), addressExprValue.getType()),
          addressAndState.getState());

    } else {
      return ValueAndSMGState.ofUnknownValue(
          this,
          "Returned unknown value because the target of a requested pointer could not be found.");
    }
  }

  /**
   * Takes a target and offset and tries to find an address (not AddressExpression) that fits them.
   * If none can be found a new address (SMGPointsToEdge) is created and returned as Value (Not
   * AddressExpression).
   *
   * @param targetObject {@link SMGObject} target.
   * @param offsetInBits Offset in the target as Value.
   * @return a {@link Value} (NOT AddressExpression) and state with the address/address added.
   */
  public ValueAndSMGState searchOrCreateAddress(
      SMGObject targetObject, CType type, Value offsetInBits) {
    assert !(targetObject instanceof SMGSinglyLinkedListSegment);
    return searchOrCreateAddress(targetObject, type, offsetInBits, 0, SMGTargetSpecifier.IS_REGION);
  }

  /**
   * Takes a target and offset and tries to find an address (not AddressExpression) that fits them.
   * If none can be found a new address (SMGPointsToEdge) is created and returned as Value (Not
   * AddressExpression).
   *
   * @param targetObject {@link SMGObject} target.
   * @param offsetInBits Offset in the target as BigInt.
   * @return a {@link Value} (NOT AddressExpression) and state with the address/address added.
   */
  public ValueAndSMGState searchOrCreateAddress(
      SMGObject targetObject, CType type, BigInteger offsetInBits) {
    assert !(targetObject instanceof SMGSinglyLinkedListSegment);
    return searchOrCreateAddress(
        targetObject, type, new NumericValue(offsetInBits), 0, SMGTargetSpecifier.IS_REGION);
  }

  /**
   * Takes a target and offset and tries to find an address (not AddressExpression) that fits them.
   * If none can be found a new address (SMGPointsToEdge) is created and returned as Value (Not
   * AddressExpression).
   *
   * @param targetObject {@link SMGObject} target.
   * @param offsetInBits Offset as BigInt.
   * @param pointerNestingLevel pointer nesting level
   * @param pTargetSpecifier the {@link SMGTargetSpecifier}
   * @return a {@link Value} (NOT AddressExpression) and state with the address/address added.
   */
  public ValueAndSMGState searchOrCreateAddress(
      SMGObject targetObject,
      CType type,
      BigInteger offsetInBits,
      int pointerNestingLevel,
      SMGTargetSpecifier pTargetSpecifier) {
    return searchOrCreateAddress(
        targetObject, type, new NumericValue(offsetInBits), pointerNestingLevel, pTargetSpecifier);
  }

  /**
   * Takes a target and offset and tries to find an address (not AddressExpression) that fits them.
   * If none can be found a new address (SMGPointsToEdge) is created and returned as Value (Not
   * AddressExpression).
   *
   * @param targetObject {@link SMGObject} target.
   * @param offsetInBits Offset as Value.
   * @param pointerNestingLevel pointer nesting level
   * @param pTargetSpecifier the {@link SMGTargetSpecifier}
   * @return a {@link Value} (NOT AddressExpression) and state with the address/address added.
   */
  public ValueAndSMGState searchOrCreateAddress(
      SMGObject targetObject,
      CType type,
      Value offsetInBits,
      int pointerNestingLevel,
      SMGTargetSpecifier pTargetSpecifier) {
    Preconditions.checkNotNull(type);
    Preconditions.checkArgument(pointerNestingLevel >= 0);
    Preconditions.checkArgument(
        !(targetObject instanceof SMGSinglyLinkedListSegment)
            || !pTargetSpecifier.equals(SMGTargetSpecifier.IS_REGION));
    // search for existing pointer first and return if found
    Optional<SMGValue> maybeAddressValue =
        getMemoryModel()
            .getAddressValueForPointsToTarget(targetObject, offsetInBits, pointerNestingLevel);

    if (maybeAddressValue.isPresent()) {
      Optional<Value> valueForSMGValue =
          getMemoryModel().getValueFromSMGValue(maybeAddressValue.orElseThrow());
      Preconditions.checkArgument(
          memoryModel.getNestingLevel(valueForSMGValue.orElseThrow()) == pointerNestingLevel);
      // Reuse pointer; there should never be a SMGValue without counterpart!
      return ValueAndSMGState.of(valueForSMGValue.orElseThrow(), this);
    }

    Value addressValue = SymbolicValueFactory.getInstance().newIdentifier(null);
    SMGState newState =
        createAndAddPointer(
            addressValue, targetObject, type, offsetInBits, pointerNestingLevel, pTargetSpecifier);
    return ValueAndSMGState.of(addressValue, newState);
  }

  /**
   * Read the value in the {@link SMGObject} at the position specified by the offset and size.
   * Checks for validity of the object and if its externally allocated and may fail because of that.
   * The read {@link SMGValue} will be translated into a {@link Value}. If the Value is known, the
   * known value is used, unknown symbolic else. Might materialize a list if an abstracted list is
   * read (Materializes if we read a pointer to an abstract list that does not point towards the
   * head).
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

    return readValue(
        pObject, pFieldOffset, pSizeofInBits, readType, options.isPreciseSMGRead(), true);
  }

  /**
   * Read the value in the {@link SMGObject} at the position specified by the offset and size.
   * Checks for validity of the object and if its externally allocated and may fail because of that.
   * The read {@link SMGValue} will be translated into a {@link Value}. If the Value is known, the
   * known value is used, unknown symbolic else. Might materialize a list if an abstracted list is
   * read (Materializes if we read a pointer to an abstract list that does not point towards the
   * head).
   *
   * @param pObject {@link SMGObject} where to read. May not be 0.
   * @param pFieldOffset {@link BigInteger} offset.
   * @param pSizeofInBits {@link BigInteger} sizeInBits.
   * @param readType the {@link CType} of the read. Not cast! Null for irrelevant types.
   * @param preciseRead if true, tries to read partial has value edges (e.g. read a short from an
   *     int)
   * @param materialize if true, materializes correctly. Never materializes on false.
   * @return The {@link Value} read and the {@link SMGState} after the read.
   * @throws SMGException for critical errors if a list is materialized.
   */
  public List<ValueAndSMGState> readValue(
      SMGObject pObject,
      BigInteger pFieldOffset,
      BigInteger pSizeofInBits,
      @Nullable CType readType,
      boolean preciseRead,
      boolean materialize)
      throws SMGException {
    Preconditions.checkArgument(!(pObject instanceof SMGSinglyLinkedListSegment));
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
      return readValue(pObject, pFieldOffset, pSizeofInBits, readType, false, true);
    }
    SMGHasValueEdge readSMGValueEdge = valueAndNewSPC.getSMGHasValueEdges().get(0);
    boolean exactRead =
        readSMGValueEdge.getOffset().equals(pFieldOffset)
            && readSMGValueEdge.getSizeInBits().equals(pSizeofInBits);
    SMGValue readSMGValue = readSMGValueEdge.hasValue();
    ImmutableList.Builder<ValueAndSMGState> returnBuilder = ImmutableList.builder();
    if (memoryModel.getSmg().isPointer(readSMGValue)
        && memoryModel.getSmg().pointsToMaterializableList(readSMGValue)
        && exactRead
        && materialize) {
      // TODO: do we need to materialize if the object read is abstract (and we read non head)?
      // Materialize for all pointers towards an abstracted list, excluding the hfo offset
      // Materialization might generate 2 states, one of which deleted the 0+, and for this
      // state the read value is wrong!
      for (SMGStateAndOptionalSMGObjectAndOffset newState :
          materializeLinkedList(
              readSMGValue,
              memoryModel.getSmg().getPTEdge(readSMGValue).orElseThrow(),
              currentState)) {
        // This is expected not to Materialize again
        List<ValueAndSMGState> readAfterMat =
            newState
                .getSMGState()
                .readValue(pObject, pFieldOffset, pSizeofInBits, readType, false, true);
        Preconditions.checkArgument(readAfterMat.size() == 1);
        returnBuilder.addAll(readAfterMat);
      }
    } else {
      Optional<Value> maybeValue = getMemoryModel().getValueFromSMGValue(readSMGValue);
      if (!exactRead) {
        if (maybeValue.isEmpty()) {
          return readValue(pObject, pFieldOffset, pSizeofInBits, readType, false, true);
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
            return readValue(pObject, pFieldOffset, pSizeofInBits, readType, false, true);
          }
          returnBuilder.add(ValueAndSMGState.of(valueInterpretation, currentState));
          return returnBuilder.build();
        }
      }
      returnBuilder.add(currentState.handleReadSMGValue(readSMGValue, readType));
    }
    return returnBuilder.build();
  }

  /**
   * This method does not check the boundaries of the read! Reads without materialization. Read the
   * value in the {@link SMGObject} at the position specified by the offset and size. Checks for
   * validity of the object and if its externally allocated and may fail because of that. The read
   * {@link SMGValue} will be translated into a {@link Value}. If the Value is known, the known
   * value is used, unknown symbolic else.
   *
   * @param pObject {@link SMGObject} where to read. May not be 0.
   * @param pFieldOffset {@link BigInteger} offset.
   * @param pSizeofInBits {@link BigInteger} sizeInBits.
   * @param readType the {@link CType} of the read. Not cast! Null for irrelevant types.
   * @return The {@link Value} read and the {@link SMGState} after the read.
   * @throws SMGException for critical errors if a list is materialized.
   */
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
          copyAndReplaceMemoryModel(
              getMemoryModel().copyAndPutValue(unknownValue, readSMGValue, 0, readType)));
    }
  }

  // Expects a (single) read SMGHasValueEdge that was not exact (to the offset/size read) and needs
  // to be cut to size
  private Value transformSingleHVEdgeToTargetValue(
      SMGHasValueEdge readSMGHVValue, BigInteger readOffset, BigInteger readSizeInBits)
      throws SMGException {
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
      throw new SMGException("Symbolic handling of partial reads are not supported at the moment.");
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

  private static long getMask(BigInteger readSizeInBits) throws SMGException {
    int readSize = readSizeInBits.intValueExact();
    long mask;
    switch (readSize) {
      case 1 -> mask = 1;
      case 2 -> mask = 3;
      case 4 -> mask = 0x0000000F;
      case 8 -> mask = 0x000000FF;
      case 16 -> mask = 0x0000FFFF;
      case 32 -> mask = 0xFFFFFFFF;
      case 64 -> mask = -1;
      default -> throw new SMGException("Unhandled bit size in partial memory read.");
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
              newState
                  .getMemoryModel()
                  .copyAndPutValue(unknownValue, readSMGValue, 0, pointerType)),
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

  public boolean isLastPtr(SMGValue pointer) {
    Preconditions.checkArgument(memoryModel.getSmg().isPointer(pointer));
    return memoryModel.getSmg().getPTEdge(pointer).orElseThrow().targetSpecifier()
        == SMGTargetSpecifier.IS_LAST_POINTER;
  }

  public boolean isFirstPtr(SMGValue pointer) {
    Preconditions.checkArgument(memoryModel.getSmg().isPointer(pointer));
    return memoryModel.getSmg().getPTEdge(pointer).orElseThrow().targetSpecifier()
        == SMGTargetSpecifier.IS_FIRST_POINTER;
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
            this, withInvalidFree("Invalid free of unallocated object is found.", addressToFree));
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
      SymbolicProgramConfiguration newSPC = currentMemModel.invalidateSMGObject(regionToFree, true);
      // state in our implementation.
      // performConsistencyCheck(SMGRuntimeCheck.HALF);
      returnBuilder.add(currentState.copyAndReplaceMemoryModel(newSPC));
    }
    return returnBuilder.build();
  }

  /**
   * Checks if the offset of the access can be less than the size of the object or if the offset +
   * size of the access exceeds the size of the object (under- and overread/write). This check is
   * performed with an SMT solver. Additional {@link Constraint}s are taken from the current {@link
   * SMGState}. Returns the satisfiablility (SAT/UNSAT/UNKNOWN) of the check and a new {@link
   * SMGState} with possible assignments based on satisfiable results. SAT results can be
   * interpreted as invalid memory access (with the model giving detailed values for the violation).
   * The {@link Constraint}s are not saved in the state.
   *
   * @param object the {@link SMGObject} that is accessed.
   * @param offsetOfAccessInBits Offset of the access to the object.
   * @param sizeOfAccessInBits size of the access to the object.
   * @param edge Debug {@link CFAEdge}. May be null.
   * @return the satisfiablity (SAT -> memory safety violated) and a new state with the relevant
   *     model.
   * @throws SMGSolverException in case of solver errors.
   */
  public SatisfiabilityAndSMGState checkBoundariesOfMemoryAccessWithSolver(
      SMGObject object,
      Value offsetOfAccessInBits,
      Value sizeOfAccessInBits,
      @Nullable CFAEdge edge)
      throws SMGSolverException {
    CType calcTypeForMemAccess = evaluator.getCTypeForBitPreciseMemoryAddresses();
    // Use an SMT solver to argue about the offset/size validity
    final ConstraintFactory constraintFactory =
        ConstraintFactory.getInstance(this, machineModel, logger, options, evaluator, edge);
    final Collection<Constraint> newConstraints =
        constraintFactory.checkValidMemoryAccess(
            offsetOfAccessInBits, sizeOfAccessInBits, object.getSize(), calcTypeForMemAccess, this);

    String stackFrameFunctionName = getStackFrameTopFunctionName();

    // Iff SAT -> memory-safety is violated
    return evaluator.checkMemoryConstraintsAreUnsatIndividually(
        newConstraints, stackFrameFunctionName, this);
  }

  /**
   * Checks if the memory access of the offset and offset + size are possible inside the given
   * object for the current constraints. This saves the constraints in all cases and allows us to
   * access a model for the concrete values of the used offsets or sizes consistently.
   *
   * @param object object that is accessed.
   * @param offsetOfAccessInBits offset of the access, may be symbolic, in bits.
   * @param sizeOfAccessInBits size of the access (read/write), maybe symbolic, in bits.
   * @param edge current {@link CFAEdge}.
   * @return SAT or UNSAT and a state with the constraints added and possibly a model.
   * @throws SMGException in case of errors.
   * @throws SMGSolverException in case of errors.
   */
  public SatisfiabilityAndSMGState checkForConcreteMemoryAccessAssignmentWithSolver(
      SMGObject object,
      Value offsetOfAccessInBits,
      Value sizeOfAccessInBits,
      @Nullable CFAEdge edge)
      throws SMGException, SMGSolverException {
    CType calcTypeForMemAccess = evaluator.getCTypeForBitPreciseMemoryAddresses();
    // Use an SMT solver to argue about the offset/size values
    final ConstraintFactory constraintFactory =
        ConstraintFactory.getInstance(this, machineModel, logger, options, evaluator, edge);
    final List<Constraint> newConstraints =
        constraintFactory.checkForConcreteMemoryAccessAssignmentWithSolver(
            offsetOfAccessInBits, sizeOfAccessInBits, object.getSize(), calcTypeForMemAccess, this);

    String stackFrameFunctionName = getStackFrameTopFunctionName();

    // Iff SAT -> there is an assignment
    return evaluator.checkIsUnsatAndAddConstraints(newConstraints, stackFrameFunctionName, this);
  }

  /** Returns a new state with a new constraint added: symbolicValueToBlock != valueToBlock. */
  private SMGState copyAndAddValueBlockingConstraint(
      SymbolicValue symbolicValueToBlock,
      Value valueToBlock,
      CType typeOfValueToBlock,
      CFAEdge edge) {

    // Use an SMT solver to argue about the offset/size values
    final ConstraintFactory constraintFactory =
        ConstraintFactory.getInstance(this, machineModel, logger, options, evaluator, edge);
    final Constraint newConstraint =
        constraintFactory.getUnequalConstraint(
            symbolicValueToBlock, valueToBlock, typeOfValueToBlock, this);

    return this.addConstraint(newConstraint);
  }

  /** Returns a new state with a new constraint added: symbolicValue == value. */
  private SMGState copyAndAddValuesEqualConstraint(
      Value symbolicValueToAssign, Value value, CType typeOfValue, CFAEdge edge) {

    // Use an SMT solver to argue about the offset/size values
    final ConstraintFactory constraintFactory =
        ConstraintFactory.getInstance(this, machineModel, logger, options, evaluator, edge);
    final Constraint newConstraint =
        constraintFactory.getEqualConstraint(symbolicValueToAssign, value, typeOfValue, this);

    return this.addConstraint(newConstraint);
  }

  /**
   * Writes into the given {@link SMGObject} at the specified offset in bits with the size in bits
   * the value given. This method adds the Value <-> SMGValue mapping if none is known, else it uses
   * an existing mapping. This method makes all checks (write to 0, sizes, validity). For tests,
   * known concrete offsets/sizes or disabled option findConcreteValuesForSymbolicOffsets only.
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
      Value sizeInBits,
      Value valueToWrite,
      CType valueType,
      @Nullable CFAEdge edge)
      throws SMGSolverException, SMGException {
    return writeValueWithChecks(
            object, writeOffsetInBits, sizeInBits, null, valueToWrite, valueType, null, edge)
        .get(0);
  }

  /**
   * Don't use this method outside of this class or tests! Writes into the given {@link SMGObject}
   * at the specified offset in bits with the size in bits the value given. This method adds the
   * Value <-> SMGValue mapping if none is known, else it uses an existing mapping. This method
   * makes all checks (write to 0, sizes, validity). Only returns multiple states with option
   * findConcreteValuesForSymbolicOffsets and symbolic offset or sizes.
   *
   * @param object the memory {@link SMGObject} to write to.
   * @param writeOffsetInBits offset in bits to be written
   * @param sizeInBits size in bits to be written
   * @param lValueExpr left-hand side expression that is written to.
   * @param valueToWrite the value to write. Is automatically either translated to a known SMGValue
   *     or a new SMGValue is added to the returned state.
   * @param valueType type of the valueToWrite.
   * @param rValueExpr right-hand side expression that is written into the left-hand sided expr.
   * @param edge {@link CFAEdge} if possible. Null otherwise. (i.e. CEGAR)
   * @return a new SMGState with the value written.
   */
  public List<SMGState> writeValueWithChecks(
      SMGObject object,
      Value writeOffsetInBits,
      Value sizeInBits,
      CExpression lValueExpr,
      Value valueToWrite,
      CType valueType,
      CRightHandSide rValueExpr,
      @Nullable CFAEdge edge)
      throws SMGException, SMGSolverException {
    if (object.isZero()) {
      // Write to 0
      return ImmutableList.of(withInvalidWriteToZeroObject(object));
    } else if (!memoryModel.isObjectValid(object)) {
      // Write to an object that is invalidated (already freed)
      // If object part of the heap -> invalid deref
      return ImmutableList.of(this.withInvalidWrite(object));
    }
    SMGState currentState = this;
    if (valueToWrite instanceof AddressExpression) {
      ValueAndSMGState valueToWriteAndState = transformAddressExpression(valueToWrite, valueType);
      valueToWrite = valueToWriteAndState.getValue();
      currentState = valueToWriteAndState.getState();
    }

    if (valueToWrite.isUnknown()) {
      Preconditions.checkNotNull(valueType);
      valueToWrite = getNewSymbolicValueForType(valueType);
    }

    BigInteger numericOffsetInBits = null;
    Value objSize = object.getSize();
    if (writeOffsetInBits.isNumericValue()
        && objSize.isNumericValue()
        && sizeInBits.isNumericValue()) {
      numericOffsetInBits = writeOffsetInBits.asNumericValue().bigIntegerValue();
      // Check that the target can hold the value
      if (object.getOffset().compareTo(numericOffsetInBits) > 0
          || object
                  .getSize()
                  .asNumericValue()
                  .bigIntegerValue()
                  .compareTo(sizeInBits.asNumericValue().bigIntegerValue().add(numericOffsetInBits))
              < 0) {
        // Out of range write
        // If object part if heap -> invalid deref
        return ImmutableList.of(
            withOutOfRangeWrite(object, writeOffsetInBits, sizeInBits, valueToWrite, edge));
      }

      if ((currentState.memoryModel.isPointer(writeOffsetInBits)
              && !writeOffsetInBits.isNumericValue())
          || (currentState.memoryModel.isPointer(sizeInBits) && !sizeInBits.isNumericValue())) {
        // This would be stupid, but valid.
        // TODO: create SV-Benchmark tasks that do this!
        throw new SMGException(
            "Stop analysis because symbolic offset or memory size has been detected as a pointer. "
                + edge);
      }

    } else if (options.trackErrorPredicates()) {
      // Symbolic Execution
      // Iff SAT -> memory-safety is violated. The model gives you the violating assignments.
      SatisfiabilityAndSMGState memsafetyCheckAndState =
          currentState.checkBoundariesOfMemoryAccessWithSolver(
              object, writeOffsetInBits, sizeInBits, edge);
      currentState = memsafetyCheckAndState.getState();

      if (memsafetyCheckAndState.isSAT()) {
        // Unknown value that should not be used with an error state that should stop the analysis
        // Stop the analysis, error found
        return ImmutableList.of(
            currentState.withOutOfRangeWrite(
                object, writeOffsetInBits, sizeInBits, valueToWrite, edge));
      }
      // Memsafety not violated

      if (!writeOffsetInBits.isNumericValue()) {
        return handleSymbolicOffsetForWriteOperation(
            object,
            writeOffsetInBits,
            sizeInBits,
            lValueExpr,
            valueToWrite,
            valueType,
            rValueExpr,
            edge,
            currentState);
      } else {
        // offset numeric, but size symbolic, but write range is inside the size, -> write
        numericOffsetInBits = writeOffsetInBits.asNumericValue().bigIntegerValue();
      }

      // Since we checked with a solver, we know that memsafety is not violated, so symbolic
      // obj size is no problem here. We just have to be careful to not exceed the symbolic size
      // when assigning concrete values.
      if (!sizeInBits.isNumericValue()) {
        // TODO: handle with solver to find assignments
        if (options.isOverapproximateValuesForSymbolicSize()) {
          // TODO: Delete ALL edges in the target region with an offset greater than the current
          return ImmutableList.of(
              currentState.copyAndReplaceMemoryModel(
                  currentState.memoryModel.copyAndReplaceHVEdgesAt(object, PersistentSet.of())));
        } else {
          throw new SMGException(
              "Stop analysis because the size of the value written in a write operation could not"
                  + " be determined. Enable the option overapproximateValuesForSymbolicSize if you"
                  + " want to continue. "
                  + edge);
        }
      }

    } else {
      // Value analysis with non-concrete offset or size or obj size
      if (!writeOffsetInBits.isNumericValue()) {
        // offset symbolic in value analysis, overapproximate
        return ImmutableList.of(currentState.withUnknownOffsetMemoryAccess());

      } else if (!objSize.isNumericValue() || !sizeInBits.isNumericValue()) {
        // obj size symbolic in value analysis, overapproximate
        // or sizeInBits symbolic
        return ImmutableList.of(
            currentState.withOutOfRangeRead(object, writeOffsetInBits, sizeInBits));
      }
    }
    if (!sizeInBits.isNumericValue()) {
      if (options.isOverapproximateValuesForSymbolicSize()) {
        // TODO: sizeInBits symbolic, can't write, invalidate the whole obj starting from the
        // numeric offset
      }
      throw new SMGException("Symbolic memory write size found that could not be handled. " + edge);
    }

    Preconditions.checkArgument(!(valueToWrite instanceof AddressExpression));
    Preconditions.checkNotNull(numericOffsetInBits);
    Preconditions.checkArgument(sizeInBits.isNumericValue());
    SMGValueAndSMGState valueAndState = copyAndAddValue(valueToWrite, valueType);
    SMGValue smgValue = valueAndState.getSMGValue();
    currentState = valueAndState.getSMGState();
    return ImmutableList.of(
        currentState.writeValueWithoutChecks(
            object, numericOffsetInBits, sizeInBits.asNumericValue().bigIntegerValue(), smgValue));
  }

  /**
   * Handles symbolic offsets (and sizes if they are symbolic as well) in read operations. Will
   * check if there are options to handle symbolic values, if yes, will use those, e.g.
   * overapproximate or find concrete values.
   */
  @NonNull
  public List<ValueAndSMGState> handleSymbolicOffsetForReadOperation(
      SMGObject object,
      Value readOffsetInBits,
      BigInteger sizeInBits,
      CExpression exprReadingFromValueVisitor,
      @Nullable CFAEdge edge,
      SMGState currentState)
      throws SMGException, SMGSolverException {
    if (options.isOverapproximateSymbolicOffsets()) {
      // Delete ALL edges in the target region, as they may all be now different
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState.copyAndReplaceMemoryModel(
                  currentState.memoryModel.copyAndReplaceHVEdgesAt(object, PersistentSet.of())),
              "Returned unknown as a result of option isOverapproximateSymbolicOffsets true for a"
                  + " symbolic offset.",
              edge));

    } else if (options.isFindConcreteValuesForSymbolicOffsets()) {
      // Find the possible assignments of the offset (or the symbolic value therein)
      //   and create concrete states based on concrete offsets them.
      // We know that memory-safety is not violated, so the values are most likely finite.
      return assignConcreteValuesForSymbolicValuesAndRead(
          object, readOffsetInBits, sizeInBits, exprReadingFromValueVisitor, edge, currentState);

    } else {
      throw new SMGException(
          "Stop analysis because of an error in symbolic offset in read operation. Enable the"
              + " option findConcreteValuesForSymbolicOffsets or overapproximateSymbolicOffsets if"
              + " you want to continue. "
              + edge);
    }
  }

  @NonNull
  private List<ValueAndSMGState> assignConcreteValuesForSymbolicValuesAndRead(
      SMGObject object,
      Value offsetValueInBits,
      BigInteger sizeInBits,
      CExpression exprReadingFromValueVisitor,
      @Nullable CFAEdge edge,
      SMGState currentState)
      throws SMGException, SMGSolverException {
    options.incConcreteValueForSymbolicOffsetsAssignmentMaximum();

    List<SMGState> assignedStates =
        findAssignmentsWithSolverAndReplaceSymbolicValues(
            object, offsetValueInBits, new NumericValue(sizeInBits), edge, currentState);

    // (The concrete assignments don't invoke an SMT check anymore if there are no more symbolic
    // values in any of the size or offset expressions)
    if (assignedStates.isEmpty()) {
      // No more assignments possible.
      options.decConcreteValueForSymbolicOffsetsAssignmentMaximum();
      return ImmutableList.of();
    }

    // Temporary restriction
    Preconditions.checkArgument(assignedStates.size() == 2);
    // Run the evaluation for the values with symbolic idents assigned in them again
    // e.g. offset/size
    ImmutableList.Builder<ValueAndSMGState> finalValuesAndStates = ImmutableList.builder();
    for (SMGState assignedState : assignedStates) {
      // The offset is not necessarily concrete now! We assign a concrete value in one state and
      //   block it in the symbolic value for another state.
      List<ValueAndSMGState> assignedValuesAndStates =
          reEvaluateValueAfterConcreteAssignment(exprReadingFromValueVisitor, edge, assignedState);
      finalValuesAndStates.addAll(assignedValuesAndStates);
    }
    options.decConcreteValueForSymbolicOffsetsAssignmentMaximum();
    return finalValuesAndStates.build();
  }

  /**
   * Handles smybolic offsets (and sizes if they are symbolic as well) in write operations. Will
   * check if there are options to handle symbolic values, if yes, will use those, e.g.
   * overapproximate or find concrete values.
   */
  @NonNull
  private List<SMGState> handleSymbolicOffsetForWriteOperation(
      SMGObject object,
      Value writeOffsetInBits,
      Value sizeInBits,
      CExpression lValueExpr,
      Value valueToWrite,
      CType valueType,
      CRightHandSide rValueExpr,
      @Nullable CFAEdge edge,
      SMGState currentState)
      throws SMGException, SMGSolverException {
    if (options.isOverapproximateSymbolicOffsets()) {
      // Delete ALL edges in the target region, as they may all be now different
      return ImmutableList.of(
          currentState.copyAndReplaceMemoryModel(
              currentState.memoryModel.copyAndReplaceHVEdgesAt(object, PersistentSet.of())));

    } else if (options.isFindConcreteValuesForSymbolicOffsets()) {
      // Find the possible assignments of the offset (or the symbolic value therein)
      //   and create concrete states based on concrete offsets them.
      // We know that memory-safety is not violated, so the values are most likely finite.
      return assignConcreteValuesForSymbolicValuesAndWrite(
          object,
          writeOffsetInBits,
          sizeInBits,
          lValueExpr,
          valueToWrite,
          valueType,
          rValueExpr,
          edge,
          currentState);

    } else {
      throw new SMGException(
          "Stop analysis because of an error in symbolic offset in write operation. Enable the"
              + " option findConcreteValuesForSymbolicOffsets or overapproximateSymbolicOffsets if"
              + " you want to continue. "
              + edge);
    }
  }

  @NonNull
  private List<SMGState> assignConcreteValuesForSymbolicValuesAndWrite(
      SMGObject object,
      Value writeOffsetInBits,
      Value sizeInBits,
      CExpression lValueExpr,
      Value valueToWrite,
      CType valueType,
      CRightHandSide rValueExpr,
      @Nullable CFAEdge edge,
      SMGState currentState)
      throws SMGException, SMGSolverException {

    if (lValueExpr == null || rValueExpr == null) {
      throw new SMGException(
          "Stop analysis because of an error in symbolic offset in write operation. Option"
              + " findConcreteValuesForSymbolicOffsets failed. You can use "
              + "overapproximateSymbolicOffsets if you want to continue. "
              + edge);
    }

    List<SMGState> assignedStates =
        findAssignmentsWithSolverAndReplaceSymbolicValues(
            object, writeOffsetInBits, sizeInBits, edge, currentState);

    // (The concrete assignments don't invoke an SMT check anymore if there are no more symbolic
    // values in any of the size or offset expressions)
    if (assignedStates.isEmpty()) {
      // No more assignments possible.
      return ImmutableList.of();
    }

    // Temporary restriction
    Preconditions.checkArgument(assignedStates.size() == 2);
    // Run the evaluation for the values with symbolic idents assigned in them again
    // e.g. offset/size
    ImmutableList.Builder<SMGState> finalStates = ImmutableList.builder();
    for (SMGState assignedState : assignedStates) {
      // The offset is not necessarily concrete now! We assign a concrete value in one state and
      //   block it in the symbolic value for another state.
      SMGStateAndOptionalSMGObjectAndOffset targetAndOffsetAndState =
          reEvaluateTargetObjectAndOffsetAfterConcreteAssignment(lValueExpr, edge, assignedState);
      SMGState currentAssignedState = targetAndOffsetAndState.getSMGState();
      Preconditions.checkArgument(object == targetAndOffsetAndState.getSMGObject());
      Value writeOffsetInBitsEvaluated = targetAndOffsetAndState.getOffsetForObject();

      if (!sizeInBits.isNumericValue()) {
        // TODO: we can also handle concrete sizes the same way.
        throw new SMGException(
            "Stop analysis because of an error in symbolic offset in write operation. Option"
                + " findConcreteValuesForSymbolicOffsets failed. You can use "
                + "overapproximateSymbolicOffsets if you want to continue. "
                + edge);
      }
      // TODO: once we assign sizes we also need to reevaluate them
      Value writeSizeInBitsEvaluated = sizeInBits;

      Value newValueToWrite = valueToWrite;
      if (!valueToWrite.isNumericValue() && !memoryModel.isPointer(valueToWrite)) {
        ValueAndSMGState possibleNewValueAndState =
            reEvaluateValueToWriteAfterConcreteAssignment(rValueExpr, edge, currentAssignedState);
        newValueToWrite = possibleNewValueAndState.getValue();
        currentAssignedState = possibleNewValueAndState.getState();
      }

      finalStates.addAll(
          currentAssignedState.writeValueWithChecks(
              object,
              writeOffsetInBitsEvaluated,
              writeSizeInBitsEvaluated,
              lValueExpr,
              newValueToWrite,
              valueType,
              rValueExpr,
              edge));
    }
    return finalStates.build();
  }

  /**
   * Handles pointer requests on subscript expressions with symbolic values. If there is no
   * constraints that can be used to calculate the model for the assignment, null is returned.
   * Usually this signals that the symbolic offset will lead to a memsafety violation or a change
   * later.
   */
  public List<SMGStateAndOptionalSMGObjectAndOffset>
      assignConcreteValuesForSymbolicValuesAndReevaluateExpressionInAddressVisitor(
          SymbolicValue valueToAssign, CArraySubscriptExpression expr, @Nullable CFAEdge edge)
          throws CPATransferException {
    options.incConcreteValueForSymbolicOffsetsAssignmentMaximum();

    if (expr == null) {
      throw new SMGException(
          "Stop analysis because of an error in symbolic offset in subscript operation. Option"
              + " findConcreteValuesForSymbolicOffsets failed. "
              + edge);
    }

    // We don't know if the access is safe or if the values are only allowed to be in the range of
    // the object as we are just asking for an address!
    List<SMGState> assignedStates =
        findSubscriptAssignmentsWithSolverAndReplaceSymbolicValues(valueToAssign, edge);

    // (The concrete assignments don't invoke an SMT check anymore if there are no more symbolic
    // values in any of the size or offset expressions)
    if (assignedStates.isEmpty()) {
      // No more assignments possible.
      options.decConcreteValueForSymbolicOffsetsAssignmentMaximum();
      return ImmutableList.of();
    }

    // Run the evaluation again to get the concrete offsets
    ImmutableList.Builder<SMGStateAndOptionalSMGObjectAndOffset> results = ImmutableList.builder();
    for (SMGState assignedState : assignedStates) {
      // The offset is not necessarily concrete now! We assign a concrete value in one state and
      //   block it in the symbolic value for another state.
      List<SMGStateAndOptionalSMGObjectAndOffset> assignedAndEvaldStates =
          expr.accept(new SMGCPAAddressVisitor(evaluator, assignedState, edge, logger, options));
      if (options.isMemoryErrorTarget()) {
        for (SMGStateAndOptionalSMGObjectAndOffset assignedAndEvaldState : assignedAndEvaldStates) {
          if (assignedAndEvaldState.getSMGState().hasMemoryErrors()) {
            options.decConcreteValueForSymbolicOffsetsAssignmentMaximum();
            return ImmutableList.of(assignedAndEvaldState);
          }
        }
      }
      results.addAll(assignedAndEvaldStates);
    }
    options.decConcreteValueForSymbolicOffsetsAssignmentMaximum();
    return results.build();
  }

  @NonNull
  private SMGStateAndOptionalSMGObjectAndOffset
      reEvaluateTargetObjectAndOffsetAfterConcreteAssignment(
          CExpression lValueExpr, @Nullable CFAEdge edge, SMGState assignedState)
          throws SMGException, SMGSolverException {
    List<SMGStateAndOptionalSMGObjectAndOffset> targetsAndOffsetsAndStates;
    try {
      targetsAndOffsetsAndStates =
          lValueExpr.accept(
              new SMGCPAAddressVisitor(evaluator, assignedState, edge, logger, options));
    } catch (CPATransferException e) {
      if (e instanceof SMGException) {
        throw (SMGException) e;
      } else if (e instanceof SMGSolverException) {
        throw (SMGSolverException) e;
      }
      // This can never happen, but i am forced to do this as the visitor demands the
      // CPATransferException
      throw new RuntimeException(e);
    }
    Preconditions.checkArgument(targetsAndOffsetsAndStates.size() == 1);
    SMGStateAndOptionalSMGObjectAndOffset targetAndOffsetAndState =
        targetsAndOffsetsAndStates.get(0);
    if (!targetAndOffsetAndState.hasSMGObjectAndOffset()) {
      // No memory for the left hand side found -> ERROR, we had it before
      throw new SMGException(
          "Stop analysis because of an error in symbolic offset in write operation. Option"
              + " findConcreteValuesForSymbolicOffsets failed. You can use "
              + "overapproximateSymbolicOffsets if you want to continue. "
              + edge);
    }
    return targetAndOffsetAndState;
  }

  /** Re-evaluates the value visitor with the given expression */
  private List<ValueAndSMGState> reEvaluateValueAfterConcreteAssignment(
      CExpression exprReading, @Nullable CFAEdge edge, SMGState assignedState)
      throws SMGException, SMGSolverException {
    List<ValueAndSMGState> valuesAndStates;
    try {
      valuesAndStates =
          exprReading.accept(
              new SMGCPAValueVisitor(evaluator, assignedState, edge, logger, options));
    } catch (CPATransferException e) {
      if (e instanceof SMGException) {
        throw (SMGException) e;
      } else if (e instanceof SMGSolverException) {
        throw (SMGSolverException) e;
      }
      // This can never happen, but i am forced to do this as the visitor demands the
      // CPATransferException
      throw new RuntimeException(e);
    }
    return valuesAndStates;
  }

  @NonNull
  private ValueAndSMGState reEvaluateValueToWriteAfterConcreteAssignment(
      CRightHandSide rValueExpr, @NonNull CFAEdge edge, SMGState currentAssignedState)
      throws SMGException, SMGSolverException {
    SMGCPAValueVisitor vv =
        new SMGCPAValueVisitor(evaluator, currentAssignedState, edge, logger, options);
    List<ValueAndSMGState> possibleValues;
    try {
      possibleValues = rValueExpr.accept(vv);
    } catch (CPATransferException e) {
      if (e instanceof SMGException) {
        throw (SMGException) e;
      } else if (e instanceof SMGSolverException) {
        throw (SMGSolverException) e;
      }
      // This can never happen, but i am forced to do this as the visitor demands the
      // CPATransferException
      throw new RuntimeException(e);
    }
    Preconditions.checkArgument(possibleValues.size() == 1);
    return possibleValues.get(0);
  }

  @NonNull
  private List<SMGState> findAssignmentsWithSolverAndReplaceSymbolicValues(
      SMGObject object,
      Value offsetInBits,
      Value sizeInBits,
      @Nullable CFAEdge edge,
      SMGState currentState)
      throws SMGException, SMGSolverException {
    Map<SymbolicIdentifier, Value> assignments = new HashMap<>();
    // Add the parameters for the memory access and check sat to get a model
    SatisfiabilityAndSMGState maybeAssignmentResultAndState =
        currentState.checkForConcreteMemoryAccessAssignmentWithSolver(
            object, offsetInBits, sizeInBits, edge);
    currentState = maybeAssignmentResultAndState.getState();

    // Get used variables
    Map<SymbolicIdentifier, CType> sizeIdentsAndTypes =
        memoryModel.getSymbolicIdentifiersWithTypesForValue(sizeInBits);
    Map<SymbolicIdentifier, CType> offsetIdentsAndTypes =
        memoryModel.getSymbolicIdentifiersWithTypesForValue(offsetInBits);
    ImmutableMap.Builder<SymbolicIdentifier, CType> allIdentsAndTypesBuilder =
        ImmutableMap.builder();
    for (Entry<SymbolicIdentifier, CType> sizeIdentAndType : sizeIdentsAndTypes.entrySet()) {
      if (!offsetIdentsAndTypes.containsKey(sizeIdentAndType.getKey())) {
        allIdentsAndTypesBuilder.put(sizeIdentAndType);
      } else {
        Preconditions.checkArgument(
            sizeIdentAndType
                .getValue()
                .equals(offsetIdentsAndTypes.get(sizeIdentAndType.getKey())));
      }
    }
    Map<SymbolicIdentifier, CType> identsToReplaceWithTypes =
        allIdentsAndTypesBuilder.putAll(offsetIdentsAndTypes).buildOrThrow();

    ImmutableList.Builder<SMGState> assignedStatesBuilder = ImmutableList.builder();

    if (maybeAssignmentResultAndState.isSAT()) {
      // Found assignment
      ImmutableList<ValueAssignment> solverModel = currentState.getModel();
      for (ValueAssignment va : solverModel) {
        if (SymbolicValues.isSymbolicTerm(va.getName())) {
          SymbolicIdentifier identifier =
              SymbolicValues.convertTermToSymbolicIdentifierWithoutMemLoc(va.getName());
          if (identsToReplaceWithTypes.containsKey(identifier)) {
            Value value = SymbolicValues.convertToValue(va);
            assignments.put(identifier, value);
            logger.log(
                Level.FINE,
                "Variable %s was used as part of symbolic value %s and was assigned a concrete"
                    + " value %s.",
                va.getName(),
                offsetInBits,
                value);
          }
        }
      }
      // We allow only simple assignments to pure variables for the moment
      // TODO: we need to evaluate all values that use the assigned values and then assign them
      // again

      // Got all possible assignments. Now we need to assign them in all possible combinations.
      for (Entry<SymbolicIdentifier, Value> assignment : assignments.entrySet()) {
        if (assignments.size() > 1) {
          throw new SMGException(
              "Stop analysis because of symbolic offset in write operation. Enable the option"
                  + " overapproximateForSymbolicWrite if you want to continue. "
                  + edge);
        }
        SymbolicIdentifier oldIdent = assignment.getKey();
        List<SMGState> simpleAssignedStates =
            currentState.assignSymbolicVariable(
                oldIdent, assignment.getValue(), identsToReplaceWithTypes.get(oldIdent), edge);
        // Each of those states now must be combined with each other possible assignment state
        assignedStatesBuilder.addAll(simpleAssignedStates);
      }
    }
    return assignedStatesBuilder.build();
  }

  private List<SMGState> findSubscriptAssignmentsWithSolverAndReplaceSymbolicValues(
      SymbolicValue valueToAssign, @Nullable CFAEdge edge) throws SMGException, SMGSolverException {
    Map<SymbolicIdentifier, Value> assignments = new HashMap<>();
    // Get used variables
    Set<SymbolicIdentifier> identsToReplace;
    if (valueToAssign instanceof SymbolicIdentifier symIdenToReplace) {
      identsToReplace = ImmutableSet.of(symIdenToReplace);
    } else {
      identsToReplace = memoryModel.getSymbolicIdentifiersForValue(valueToAssign);
    }

    ImmutableList.Builder<SMGState> assignedStatesBuilder = ImmutableList.builder();

    SatisfiabilityAndSMGState maybeAssignmentResultAndState;
    if (identsToReplace.size() == 1) {
      // Some nested variable is to be replaced
      // Add the parameters for the memory access and check sat to get a model
      maybeAssignmentResultAndState = evaluator.checkIsUnsatWithCurrentConstraints(this);
      SMGState currentState = maybeAssignmentResultAndState.getState();

      if (maybeAssignmentResultAndState.isSAT()) {
        // Found assignment
        ImmutableList<ValueAssignment> solverModel = currentState.getModel();
        for (ValueAssignment va : solverModel) {
          if (SymbolicValues.isSymbolicTerm(va.getName())) {
            SymbolicIdentifier identifier =
                SymbolicValues.convertTermToSymbolicIdentifierWithoutMemLoc(va.getName());
            if (identsToReplace.contains(identifier)) {
              Value value = SymbolicValues.convertToValue(va);
              assignments.put(identifier, value);
              logger.log(
                  Level.FINE,
                  "Variable %s was used as part of symbolic value subscript offset and was "
                      + "assigned a concrete value %s.",
                  va.getName(),
                  value);
            }
          }
        }
        // We allow only simple assignments to pure variables for the moment
        if (assignments.size() > 1) {
          throw new SMGException(
              "Stop analysis because of symbolic offset in subscript expression that could not be"
                  + " assigned. "
                  + edge);
        }

        // Got all possible assignments. Now we need to assign them in all possible combinations.
        // Subscript is always int
        CType calcTypeForMemAccess = CNumericTypes.INT;
        for (Entry<SymbolicIdentifier, Value> assignment : assignments.entrySet()) {
          List<SMGState> simpleAssignedStates =
              currentState.assignSymbolicVariable(
                  assignment.getKey(), assignment.getValue(), calcTypeForMemAccess, edge);
          // Each of those states now must be combined with each other possible assignment state
          assignedStatesBuilder.addAll(simpleAssignedStates);
        }
      }
    } else {
      Preconditions.checkArgument(identsToReplace.size() > 1);
      // valueToAssign is exactly a variable to assign, but consists of more than one variable.
      // Assign it, remember the constraints and then replace the var with the concrete.
      List<ValueAndSMGState> concreteValueAndNewStates =
          findValueAssignmentsWithSolver(valueToAssign, edge);

      // Got all possible assignments. Now we need to assign them in all possible combinations.
      // Subscript is always int
      CType calcTypeForMemAccess = CNumericTypes.INT;
      for (ValueAndSMGState assignmentAndState : concreteValueAndNewStates) {
        SMGState stateWithConstraints = assignmentAndState.getState();
        Value concreteAssignment = assignmentAndState.getValue();
        assert concreteAssignment.isNumericValue();

        List<SMGState> simpleAssignedStates =
            stateWithConstraints.assignSymbolicVariable(
                valueToAssign, concreteAssignment, calcTypeForMemAccess, edge);
        // Each of those states now must be combined with each other possible assignment state
        assignedStatesBuilder.addAll(simpleAssignedStates);
      }
    }
    return assignedStatesBuilder.build();
  }

  public boolean isConcreteAssignmentFeasible(SymbolicValue valueToAssign) {
    // Get used variables
    Set<SymbolicIdentifier> identsToReplace =
        memoryModel.getSymbolicIdentifiersForValue(valueToAssign);

    boolean isNotConstraints = true;
    for (Constraint constraint : getConstraints()) {
      if (identsToReplace.stream()
          .anyMatch(i -> memoryModel.getSymbolicIdentifiersForValue(constraint).contains(i))) {
        isNotConstraints = false;
        break;
      }
    }
    // For no constraints or no idents, no assignment feasible.
    return !isNotConstraints;
  }

  /**
   * Returns a filled Optional if there is a concrete assignment possible to a variable with that
   * variable inside. Either its only 1, that's just a single variable that we assign. If it more,
   * we try to strip the concrete offset calculation (e.g. 8 bit mult) and find the next best
   * variable to assign. Example: int var = x + y - z; array[bla]; We try to find var and remember
   * that x + y - z == the concrete value of var. If that's not possible, we return an empty
   * optional.
   */
  public Optional<SymbolicValue> isVariableAssignmentFeasible(SymbolicValue valueToAssign)
      throws SMGException {
    // Get used variables
    Set<SymbolicIdentifier> identsToReplace =
        memoryModel.getSymbolicIdentifiersForValue(valueToAssign);

    if (identsToReplace.size() == 1) {
      for (SymbolicValue ident : identsToReplace) {
        return Optional.of(ident);
      }
    } else {
      // A combining variable needs to include all identifiers
      SymbolicValue strippedValue = valueToAssign;
      while (memoryModel
          .getSymbolicIdentifiersForValue(strippedValue)
          .containsAll(identsToReplace)) {
        if (memoryModel.getSMGValueFromValue(strippedValue).isPresent()) {
          return Optional.of(strippedValue);
        }
        if (valueToAssign instanceof BinarySymbolicExpression binExpr) {
          if (binExpr.getOperand1().isNumericValue() && binExpr.getOperand2().isNumericValue()) {
            // Not possible, but better be sure
            throw new SMGException(
                "Expression that could be evaluated concretely found when assigning symbolic"
                    + " values.");
          } else if (binExpr.getOperand1().isNumericValue()) {
            strippedValue = binExpr.getOperand2();
          } else if (binExpr.getOperand2().isNumericValue()) {
            strippedValue = binExpr.getOperand1();
          } else {
            // Either split into 2+ variables, or concrete nested expression that is not evaluated
            // correctly
            // TODO: investigate if the later is possible
            return Optional.empty();
          }
        } else if (valueToAssign instanceof UnarySymbolicExpression unExpr) {
          strippedValue = unExpr.getOperand();
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Returns a value that is the concrete and numeric assignment found by the solver. Its state
   * remembers a constraint for that value. Also, might return many more states with other concrete
   * values and constraints.
   *
   * @param valueToAssign a (symbolic) value that is supposed to get a concrete value assigned.
   * @param edge current CFAEdge
   * @return either null for no values possible at all or if there are no constraints for it.
   *     Otherwise, a list of concrete values as replacements for the input value and their states.
   * @throws SMGSolverException in case of errors, e.g. interrupts.
   */
  public List<ValueAndSMGState> findValueAssignmentsWithSolver(
      Value valueToAssign, @Nullable CFAEdge edge) throws SMGSolverException {
    // The constraint x + y - z == valueToAssign is assigned a concrete value with constraints only.
    // This is slow with the solver later on.

    // Create a new, unique, symbolic value and add a == constraint to the given.
    SymbolicIdentifier newSymbolicToBeAssigned =
        SymbolicValueFactory.getInstance().newIdentifier(null);
    // Got all possible assignments. Now we need to assign them in all possible combinations.
    // Subscript is always int
    CType calcTypeForMemAccess = CNumericTypes.INT;
    SMGState equalsState =
        copyAndAddValuesEqualConstraint(
            valueToAssign, newSymbolicToBeAssigned, calcTypeForMemAccess, edge);

    // Add the parameters for the memory access and check sat to get a model
    SatisfiabilityAndSMGState maybeAssignmentResultAndState =
        evaluator.checkIsUnsatWithCurrentConstraints(equalsState);
    equalsState = maybeAssignmentResultAndState.getState();

    ImmutableList.Builder<ValueAndSMGState> assignedBuilder = ImmutableList.builder();

    if (maybeAssignmentResultAndState.isSAT()) {
      Value assignment = null;
      // Found assignment
      ImmutableList<ValueAssignment> solverModel = equalsState.getModel();
      for (ValueAssignment va : solverModel) {
        if (SymbolicValues.isSymbolicTerm(va.getName())) {
          SymbolicIdentifier identifier =
              SymbolicValues.convertTermToSymbolicIdentifierWithoutMemLoc(va.getName());
          if (identifier.equals(newSymbolicToBeAssigned)) {
            assignment = SymbolicValues.convertToValue(va);
            logger.log(
                Level.FINE,
                "Assigned concrete value %s as symbolic value subscript offset (in Bits).",
                assignment);
            break;
          }
        }
      }

      if (assignment != null) {
        equalsState =
            equalsState.copyAndAddValuesEqualConstraint(
                valueToAssign, assignment, calcTypeForMemAccess, edge);
        assignedBuilder.add(ValueAndSMGState.of(assignment, equalsState));

        SMGState unEqualState =
            copyAndAddValueBlockingConstraint(
                (SymbolicValue) valueToAssign, assignment, calcTypeForMemAccess, edge);

        List<ValueAndSMGState> recursiveAssignments =
            unEqualState.findValueAssignmentsWithSolver(valueToAssign, edge);

        assignedBuilder.addAll(recursiveAssignments);
      }
    }
    return assignedBuilder.build();
  }

  /**
   * Assigns the value concretely in one state to the given identifier and blocks it via a
   * constraint in the second state returned.
   *
   * @param symbolicValueToAssignTo the symbolic value (identifier) that is assigned. No pointers
   *     allowed.
   * @param newlyAssignedValue the concrete value to assign to the symbolic value.
   * @param typeOfValueToAssign the type of the value that is assigned.
   * @return 2 states, one with the value assigned for every entry of the symbolic value and a
   *     second state with a constraint that the symbolic value is not equal to the given value.
   */
  private List<SMGState> assignSymbolicVariable(
      SymbolicIdentifier symbolicValueToAssignTo,
      Value newlyAssignedValue,
      CType typeOfValueToAssign,
      CFAEdge edge)
      throws SMGException {
    Preconditions.checkArgument(!memoryModel.isPointer(symbolicValueToAssignTo));
    ImmutableList.Builder<SMGState> returnStates = ImmutableList.builder();

    returnStates.add(copyAndReplaceValue(symbolicValueToAssignTo, newlyAssignedValue));
    returnStates.add(
        copyAndAddValueBlockingConstraint(
            symbolicValueToAssignTo, newlyAssignedValue, typeOfValueToAssign, edge));
    return returnStates.build();
  }

  /**
   * Assigns the value concretely in one state to the given identifier and blocks it via a
   * constraint in the second state returned.
   *
   * @param symbolicValueToAssignTo the symbolic value (identifier) that is assigned. No pointers
   *     allowed.
   * @param newlyAssignedValue the concrete value to assign to the symbolic value.
   * @param typeOfValueToAssign the type of the value that is assigned.
   * @return 2 states, one with the value assigned for every entry of the symbolic value and a
   *     second state with a constraint that the symbolic value is not equal to the given value.
   */
  private List<SMGState> assignSymbolicVariable(
      SymbolicValue symbolicValueToAssignTo,
      Value newlyAssignedValue,
      CType typeOfValueToAssign,
      CFAEdge edge)
      throws SMGException {
    Preconditions.checkArgument(!memoryModel.isPointer(symbolicValueToAssignTo));
    ImmutableList.Builder<SMGState> returnStates = ImmutableList.builder();

    returnStates.add(copyAndReplaceValue(symbolicValueToAssignTo, newlyAssignedValue));
    returnStates.add(
        copyAndAddValueBlockingConstraint(
            symbolicValueToAssignTo, newlyAssignedValue, typeOfValueToAssign, edge));
    return returnStates.build();
  }

  private SMGState copyAndReplaceValue(
      SymbolicValue pSymbolicValueToAssignTo, Value pNewlyAssignedValue) throws SMGException {
    // We need to not only replace the value, but every other value that has the values inside of it
    return copyAndReplaceMemoryModel(
        memoryModel.replaceValueWithAndCopy(pSymbolicValueToAssignTo, pNewlyAssignedValue));
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
      throws CPATransferException {
    SMGObject returnObject = getMemoryModel().getReturnObjectForCurrentStackFrame().orElseThrow();
    if (valueToWrite.isUnknown()) {
      valueToWrite = getNewSymbolicValueForType(returnValueType);
    }
    // Check that the target can hold the value
    if (returnObject.getOffset().compareTo(BigInteger.ZERO) > 0
        || returnObject.getSize().asNumericValue().bigIntegerValue().compareTo(sizeInBits) < 0) {
      // Out of range write
      return withOutOfRangeWrite(
          returnObject, new NumericValue(BigInteger.ZERO), sizeInBits, valueToWrite, edge);
    }
    return writeValueWithChecks(
        returnObject,
        new NumericValue(BigInteger.ZERO),
        new NumericValue(sizeInBits),
        valueToWrite,
        returnValueType,
        edge);
  }

  /** Writes the value exactly to the size of the return of the current stack frame. */
  private SMGState writeToReturn(Value valueToWrite) throws SMGException, SMGSolverException {
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
   * exception with an error info inside the state thrown with. The dereference might materialize a
   * list!
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
      Value sizeInBits,
      Value valueToWrite,
      CType valueType,
      CFAEdge edge)
      throws CPATransferException {
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
          evaluator.addBitOffsetValues(maybeRegion.getOffsetForObject(), writeOffsetInBits);

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
    Value targetObjSize = targetObject.getSize();
    Value sourceObjSize = sourceObject.getSize();
    if (sourceStartOffset.isNumericValue()
        && targetStartOffset.isNumericValue()
        && copySize.isNumericValue()
        && targetObjSize.isNumericValue()
        && sourceObjSize.isNumericValue()) {
      BigInteger copySizeInBits = copySize.asNumericValue().bigIntegerValue();
      BigInteger sourceOffset = sourceStartOffset.asNumericValue().bigIntegerValue();
      BigInteger targetOffset = targetStartOffset.asNumericValue().bigIntegerValue();
      // Check that we don't read beyond the source size and don't write beyonde the target size
      // and that we don't start before the object begins
      if (sourceObjSize
                  .asNumericValue()
                  .bigIntegerValue()
                  .subtract(sourceOffset)
                  .compareTo(copySizeInBits)
              < 0
          || sourceOffset.compareTo(BigInteger.ZERO) < 0) {
        // This would be an invalid read
        SMGState currentState = withInvalidRead(sourceObject);
        if (targetObjSize
                    .asNumericValue()
                    .bigIntegerValue()
                    .subtract(targetOffset)
                    .compareTo(copySizeInBits)
                < 0
            || targetOffset.compareTo(BigInteger.ZERO) < 0) {
          // That would be an invalid write
          currentState = currentState.withInvalidWrite(sourceObject);
        }
        return currentState;
      }
      if (targetObjSize
                  .asNumericValue()
                  .bigIntegerValue()
                  .subtract(targetOffset)
                  .compareTo(copySizeInBits)
              < 0
          || targetOffset.compareTo(BigInteger.ZERO) < 0) {
        // That would be an invalid write
        return this.withInvalidWrite(sourceObject);
      }
      return copySMGObjectContentToSMGObject(
          sourceObject,
          sourceStartOffset.asNumericValue().bigIntegerValue(),
          targetObject,
          targetStartOffset.asNumericValue().bigIntegerValue(),
          copySize);
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
      Value copySizeInBits)
      throws SMGException {
    if (!copySizeInBits.isNumericValue()) {
      throw new SMGException("Symbolic size of internal memory copy operation.");
    }
    SMGState currentState = this;
    BigInteger maxReadOffsetPlusSize =
        sourceStartOffset.add(copySizeInBits.asNumericValue().bigIntegerValue());
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
      Value writeSizeInBits,
      Value valueToWrite,
      CType valueType,
      CFAEdge edge)
      throws CPATransferException {
    Preconditions.checkArgument(
        writeOffsetInBits.isNumericValue() && writeSizeInBits.isNumericValue());
    return writeToStackOrGlobalVariable(
            variableName,
            writeOffsetInBits,
            writeSizeInBits,
            null,
            valueToWrite,
            valueType,
            null,
            edge)
        .get(0);
  }

  /**
   * Write to a stack (or global) variable with the name given. This method assumes that the
   * variable exists!!!! The offset and size are in bits. The {@link Value} will be added as a
   * {@link SMGValue} mapping if not known. May only returns multiple states for symbolic
   * offsets/sizes and the option findConcreteValuesForSymbolicOffsets enabled.
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
  public List<SMGState> writeToStackOrGlobalVariable(
      String variableName,
      Value writeOffsetInBits,
      Value writeSizeInBits,
      @Nullable CExpression leftHandSideExpr,
      Value valueToWrite,
      CType valueType,
      @Nullable CRightHandSide rightHandSideExpr,
      CFAEdge edge)
      throws CPATransferException {
    Optional<SMGObject> maybeVariableMemory =
        getMemoryModel().getObjectForVisibleVariable(variableName, false);

    if (maybeVariableMemory.isEmpty()) {
      // Write to unknown variable
      return ImmutableList.of(withWriteToUnknownVariable(variableName));
    }

    SMGObject variableMemory = maybeVariableMemory.orElseThrow();
    return writeValueWithChecks(
        variableMemory,
        writeOffsetInBits,
        writeSizeInBits,
        leftHandSideExpr,
        valueToWrite,
        valueType,
        rightHandSideExpr,
        edge);
  }

  /* Helper method to reconstruct the state after interpolation. This writes to ANY local variable, independent of stack frame */
  private SMGState writeToAnyStackOrGlobalVariable(
      String variableName,
      BigInteger writeOffsetInBits,
      Value writeSizeInBits,
      Value valueToWrite,
      @Nullable CType valueType)
      throws SMGException, SMGSolverException {
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
  ValueAndSMGState transformAddressExpression(Value value, CType type) throws SMGException {
    if (value instanceof AddressExpression) {
      ValueAndSMGState valueToWriteAndState = searchOrCreateAddressForAddressExpr(value, type);
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
      throws CPATransferException {
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
   * not check whether a pointer already exists but will override the target if the value already
   * has a mapping! The used specifier will be REGION and nesting 0.
   *
   * @param addressValue {@link Value} used as address pointing to the target at the offset.
   * @param target {@link SMGObject} where the pointer points to.
   * @param offsetInBits offset in the object.
   * @param type CType of the pointer.
   * @return the new {@link SMGState} with the pointer and mapping added.
   */
  public SMGState createAndAddPointer(
      Value addressValue, SMGObject target, Value offsetInBits, CType type) {
    assert !(target instanceof SMGSinglyLinkedListSegment);
    return createAndAddPointer(
        addressValue, target, type, offsetInBits, 0, SMGTargetSpecifier.IS_REGION);
  }

  /**
   * Creates a pointer (points-to-edge) from the value to the target at the specified offset. The
   * Value is mapped to a SMGValue if no mapping exists, else the existing will be used. This does
   * not check whether a pointer already exists but will override the target if the value already
   * has a mapping!
   *
   * @param addressValue {@link Value} used as address pointing to the target at the offset.
   * @param target {@link SMGObject} where the pointer points to.
   * @param offsetInBits offset in the object.
   * @param nestingLevel nesting level of the value.
   * @param specifier {@link SMGTargetSpecifier} used for the pointer.
   * @return the new {@link SMGState} with the pointer and mapping added.
   */
  public SMGState createAndAddPointer(
      Value addressValue,
      SMGObject target,
      CType type,
      BigInteger offsetInBits,
      int nestingLevel,
      SMGTargetSpecifier specifier) {
    return copyAndReplaceMemoryModel(
        memoryModel.copyAndAddPointerFromAddressToMemory(
            addressValue, target, type, new NumericValue(offsetInBits), nestingLevel, specifier));
  }

  /**
   * Creates a pointer (points-to-edge) from the value to the target at the specified offset. The
   * Value is mapped to a SMGValue if no mapping exists, else the existing will be used. This does
   * not check whether a pointer already exists but will override the target if the value already
   * has a mapping!
   *
   * @param addressValue {@link Value} used as address pointing to the target at the offset.
   * @param target {@link SMGObject} where the pointer points to.
   * @param type CType of the pointer.
   * @param offsetInBits offset in the object.
   * @param nestingLevel nesting level of the value.
   * @param specifier {@link SMGTargetSpecifier} used for the pointer.
   * @return the new {@link SMGState} with the pointer and mapping added.
   */
  public SMGState createAndAddPointer(
      Value addressValue,
      SMGObject target,
      CType type,
      Value offsetInBits,
      int nestingLevel,
      SMGTargetSpecifier specifier) {
    return copyAndReplaceMemoryModel(
        memoryModel.copyAndAddPointerFromAddressToMemory(
            addressValue, target, type, offsetInBits, nestingLevel, specifier));
  }

  /**
   * Takes a target and offset and tries to find an address (not AddressExpression) that fits them
   * (with nesting level and specifier). If none can be found a new address (SMGPointsToEdge) is
   * created and returned as Value (Not AddressExpression).
   *
   * @param targetObject {@link SMGObject} target.
   * @param offsetInBits Offset as BigInt.
   * @param nestingLevel nesting level that the pointer needs to have.
   * @param finalSpecifier specifier that the ptr needs to have.
   * @param specifierAllowedToOverride set of specifiers allowed to be changed to the new specifier.
   * @return a {@link Value} (NOT AddressExpression) and state with the address/address added.
   */
  public ValueAndSMGState searchOrCreateAddress(
      SMGObject targetObject,
      CType type,
      BigInteger offsetInBits,
      int nestingLevel,
      SMGTargetSpecifier finalSpecifier,
      Set<SMGTargetSpecifier> specifierAllowedToOverride) {
    Preconditions.checkArgument(nestingLevel >= 0);
    // search for existing pointer first and return if found
    Optional<SMGValue> maybeAddressValue =
        memoryModel.getAddressValueForPointsToTargetWithNestingLevel(
            targetObject, offsetInBits, nestingLevel, finalSpecifier, specifierAllowedToOverride);

    if (maybeAddressValue.isPresent()) {
      SMGValue addressValue = maybeAddressValue.orElseThrow();
      Optional<Value> valueForSMGValue = getMemoryModel().getValueFromSMGValue(addressValue);
      Preconditions.checkArgument(memoryModel.getNestingLevel(addressValue) == nestingLevel);
      SMGState currentState = this;
      if (!getMemoryModel()
          .getPointerSpecifier(valueForSMGValue.orElseThrow())
          .equals(finalSpecifier)) {
        currentState =
            currentState.copyAndReplaceMemoryModel(
                currentState.memoryModel.copyAndSetTargetSpecifierForPointer(
                    addressValue, finalSpecifier));
      }

      return ValueAndSMGState.of(valueForSMGValue.orElseThrow(), currentState);
    }
    Value newAddressValue = SymbolicValueFactory.getInstance().newIdentifier(null);
    return ValueAndSMGState.of(
        newAddressValue,
        copyAndReplaceMemoryModel(
            memoryModel.copyAndAddPointerFromAddressToRegionWithNestingLevel(
                newAddressValue, targetObject, type, offsetInBits, nestingLevel, finalSpecifier)));
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
    return memoryModel.getNewSymbolicValueForType(valueType);
  }

  /**
   * Returns a new symbolic constant value based on the type of another.
   *
   * @param valueToTakeTypeFrom the {@link CType} will be extracted from this {@link Value} if
   *     possible.
   * @return a new symbolic Value.
   */
  public Value getNewSymbolicValue(Value valueToTakeTypeFrom) {
    CType valueType = null;
    if (valueToTakeTypeFrom instanceof ConstantSymbolicExpression constSym) {
      valueType = (CType) constSym.getType();
    }
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

  /**
   * Returns true if there is a pointer from outside a linked list (assuming that the linked list is
   * always the same given shape) in between the element given (e.g. from stack variables). The
   * prevObj is checked for LAST pointers only, while the nextObj is checked for all FIRST and
   * REGION pointers. Returns false if there is either no pointers towards the given object,
   * self-pointers, or only pointers from list elements to other list elements with the correct
   * given shape. Abstracted elements are only checked for FIRST pointers here!
   *
   * @param prevObj {@link SMGObject} to check for pointers from outside a given list shape. This
   *     objs next pointer points towards nextObj. Only abstracted elements are checked and only for
   *     LAST pointers!
   * @param nextObj {@link SMGObject} to check for pointers from outside a given list shape. This
   *     object is connected to prevObj via prevObjs next pointer and potentially nextObjs prev
   *     pointer points towards prevObj if there is a prev pointer. All obj types are checked, but
   *     Abstracted elements are only checked for FIRST pointers here!
   * @param nfo suspected next pointer offset. Pointers originating from other offsets are rejected.
   * @param nextPointerTargetOffset next pointer target offset. Pointers originating with other
   *     target offsets are rejected.
   * @param maybePfo suspected prev pointer offset. Pointers originating from other offsets are
   *     rejected. May be empty, then its ignored.
   * @param prevPointerTargetOffset suspected prev pointer target offset. Pointers originating with
   *     other target offsets are rejected. May be empty, then its ignored.
   * @return true if there is ptrs from outside the list shape. False else.
   */
  public boolean listElementsHaveOutsidePointerInBetween(
      SMGObject prevObj,
      SMGObject nextObj,
      BigInteger nfo,
      BigInteger nextPointerTargetOffset,
      Optional<BigInteger> maybePfo,
      Optional<BigInteger> prevPointerTargetOffset) {
    // There is no outside pointer allowed in between 2 regions that are joined to form an
    //   abstracted list segment

    // S/DLLs need to be treated differently. If we check for abstraction, the direction matters.
    // If we want to abstract a list and have a region followed by an abstracted list
    //   (and the region can be absorbed),
    //   but there is a first pointer from outside the list, it can not be abstracted.
    // If the only first ptr(s) come from the region that is absorbed, that's OK!
    // If there are last ptrs from outside the list, the region can still be absorbed
    //   (if it has the only fst ptrs).
    if (prevObj instanceof SMGSinglyLinkedListSegment) {
      // TODO: remove nfo for this case?
      if (linkedListSegmentHasPointersFromOutsideOfList(
          prevObj,
          nfo,
          SMGTargetSpecifier.IS_LAST_POINTER,
          nextPointerTargetOffset,
          maybePfo,
          prevPointerTargetOffset)) {
        return true;
      }
    }
    return objectHasOutsidePointerTowards(
        nextObj, nfo, nextPointerTargetOffset, maybePfo, prevPointerTargetOffset);
  }

  /**
   * Returns true if there is a pointer from outside a linked list (assuming that the linked list is
   * always the same given shape) towards the target list element given (e.g. from stack variables).
   * Returns false if there is either no pointers towards the given object, self-pointers, or only
   * pointers from list elements to other list elements with the correct given shape. Abstracted
   * elements are only checked for FIRST pointers here!
   *
   * @param object {@link SMGObject} to check for pointers from outside a given list shape.
   *     Abstracted elements are only checked for FIRST pointers here!
   * @param nfo suspected next pointer offset. Pointers originating from other offsets are rejected.
   * @param nextPointerTargetOffset next pointer target offset. Pointers originating with other
   *     target offsets are rejected.
   * @param maybePfo suspected prev pointer offset. Pointers originating from other offsets are
   *     rejected. May be empty, then its ignored.
   * @param prevPointerTargetOffset suspected prev pointer target offset. Pointers originating with
   *     other target offsets are rejected. May be empty, then its ignored.
   * @return true if there is ptrs from outside the list shape. False else.
   */
  public boolean objectHasOutsidePointerTowards(
      SMGObject object,
      BigInteger nfo,
      BigInteger nextPointerTargetOffset,
      Optional<BigInteger> maybePfo,
      Optional<BigInteger> prevPointerTargetOffset) {
    if (object instanceof SMGSinglyLinkedListSegment) {
      // TODO: remove pfo for this case?
      return linkedListSegmentHasPointersFromOutsideOfList(
          object,
          nfo,
          SMGTargetSpecifier.IS_FIRST_POINTER,
          nextPointerTargetOffset,
          maybePfo,
          prevPointerTargetOffset);
    } else {
      return regionHasPointersFromOutsideOfList(
          object, nfo, nextPointerTargetOffset, maybePfo, prevPointerTargetOffset);
    }
  }

  private boolean linkedListSegmentHasPointersFromOutsideOfList(
      SMGObject target,
      BigInteger nfo,
      SMGTargetSpecifier allowedSpec,
      BigInteger nextPointerTargetOffset,
      Optional<BigInteger> maybePfo,
      Optional<BigInteger> prevPointerTargetOffset) {
    Preconditions.checkArgument(
        target instanceof SMGSinglyLinkedListSegment sll
            && sll.getNextOffset().equals(nfo)
            && sll.getNextPointerTargetOffset().equals(nextPointerTargetOffset));

    if (target instanceof SMGDoublyLinkedListSegment dll && maybePfo.isPresent()) {
      Preconditions.checkArgument(
          dll.getPrevOffset().equals(maybePfo.orElseThrow())
              && dll.getPrevPointerTargetOffset().equals(prevPointerTargetOffset.orElseThrow()));
    }

    SMG smg = getMemoryModel().getSmg();
    Set<SMGValue> pointersTowardsTarget = smg.getPointerValuesForTarget(target);
    for (SMGValue pointerTowardsTarget : pointersTowardsTarget) {
      SMGPointsToEdge pte = smg.getPTEdge(pointerTowardsTarget).orElseThrow();
      SMGTargetSpecifier spec = pte.targetSpecifier();

      if (!spec.equals(allowedSpec)) {
        continue;
      }

      Set<SMGObject> objsWPointersTowardsTarget =
          smg.getAllObjectsWithValueInThem(pointerTowardsTarget);
      for (SMGObject objWPointersTowardsTarget : objsWPointersTowardsTarget) {
        if (!areTwoObjectsPartOfList(
            target,
            objWPointersTowardsTarget,
            nfo,
            nextPointerTargetOffset,
            maybePfo,
            prevPointerTargetOffset)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns true if there is a pointer from outside a linked list (assuming that the linked list is
   * always the same given shape) towards the target list element given (e.g. from stack variables).
   * Returns false if there is either no pointers towards the given object, self-pointers, or only
   * pointers from list elements to other list elements with the correct given shape.
   *
   * @param target {@link SMGObject} that is a REGION, to check for pointers from outside a given
   *     list shape.
   * @param nfo suspected next pointer offset. Pointers originating from other offsets are rejected.
   * @param nextPointerTargetOffset next pointer target offset. Pointers originating with other
   *     target offsets are rejected.
   * @param maybePfo suspected prev pointer offset. Pointers originating from other offsets are
   *     rejected. May be empty, then its ignored.
   * @param prevPointerTargetOffset suspected prev pointer target offset. Pointers originating with
   *     other target offsets are rejected. May be empty, then its ignored.
   * @return true if there is ptrs from outside the list shape. False else.
   */
  private boolean regionHasPointersFromOutsideOfList(
      SMGObject target,
      BigInteger nfo,
      BigInteger nextPointerTargetOffset,
      Optional<BigInteger> maybePfo,
      Optional<BigInteger> prevPointerTargetOffset) {
    Preconditions.checkArgument(!(target instanceof SMGSinglyLinkedListSegment));
    SMG smg = getMemoryModel().getSmg();
    // TODO: add cache for this combination of nfo/pfo to arguments, Map<SMGObject, Triple<nextObjs,
    // PrevObjs, nonListObjs>>
    // TODO: handle ALL ptrs from outside that do not have the ALL spec yet correctly

    Set<SMGObject> objsWPointersTowardsTarget = smg.getAllSourcesForPointersPointingTowards(target);
    for (SMGObject objWPointersTowardsTarget : objsWPointersTowardsTarget) {
      if (!areTwoObjectsPartOfList(
          target,
          objWPointersTowardsTarget,
          nfo,
          nextPointerTargetOffset,
          maybePfo,
          prevPointerTargetOffset)) {
        return true;
      }
    }
    return false;
  }

  public boolean nestedMemoryHasEqualOutsidePointers(
      SMGObject pTarget, SMGObject pOtherTarget, BigInteger pNfo, Optional<BigInteger> pMaybePfo) {
    SMG smg = getMemoryModel().getSmg();
    // TODO: this can be improved massivly, but its not that expensive as this check usually has 0
    //   iterations in the loops below
    // Check that nested structures (e.g. lists) don't have outside pointers
    // Traverse all connected heap and gather all objects. Also gather all objs pointing towards
    // these objects and then compare. The only obj allowed for the abstraction case is the initial
    // obj pointing inside
    Set<SMGObject> traversedObjectsTarget = new HashSet<>();
    // We add the obj leading into the nested memory directly
    traversedObjectsTarget.add(pTarget);
    Set<SMGObject> objsPointingAtTraversedTarget = new HashSet<>();
    Set<SMGObject> traversedObjectsOtherTarget = new HashSet<>();
    traversedObjectsTarget.add(pOtherTarget);
    Set<SMGObject> objsPointingAtTraversedOtherTarget = new HashSet<>();
    // Filter out nfo, pfo, zeros and non-pointers
    Predicate<SMGHasValueEdge> nonListPtrFilter =
        h ->
            !h.hasValue().isZero()
                && smg.isPointer(h.hasValue())
                && !h.getOffset().equals(pNfo)
                && (pMaybePfo.isEmpty() || !h.getOffset().equals(pMaybePfo.orElseThrow()));

    FluentIterable<SMGHasValueEdge> nonListPointersTarget =
        smg.getHasValueEdgesByPredicate(pTarget, nonListPtrFilter);
    for (SMGHasValueEdge ptrEdgeTarget : nonListPointersTarget) {
      SMGObject nonListMemory = smg.getPTEdge(ptrEdgeTarget.hasValue()).orElseThrow().pointsTo();
      gatherConnectedMemory(nonListMemory, traversedObjectsTarget, objsPointingAtTraversedTarget);
    }

    FluentIterable<SMGHasValueEdge> nonListPointersOtherTarget =
        smg.getHasValueEdgesByPredicate(pOtherTarget, nonListPtrFilter);

    for (SMGHasValueEdge ptrEdgeOtherTarget : nonListPointersOtherTarget) {
      SMGObject nonListMemory =
          smg.getPTEdge(ptrEdgeOtherTarget.hasValue()).orElseThrow().pointsTo();
      gatherConnectedMemory(
          nonListMemory, traversedObjectsOtherTarget, objsPointingAtTraversedOtherTarget);
    }

    // The list objs obviously point at the nested memory
    //   (and this might be shared in between them, which is fine for abstraction)
    objsPointingAtTraversedTarget.remove(pTarget);
    objsPointingAtTraversedOtherTarget.remove(pOtherTarget);
    objsPointingAtTraversedTarget.remove(pOtherTarget);
    objsPointingAtTraversedOtherTarget.remove(pTarget);

    return objsPointingAtTraversedOtherTarget.size() == objsPointingAtTraversedTarget.size();
  }

  private void gatherConnectedMemory(
      SMGObject nestedMemory,
      Set<SMGObject> pTraversedObjects,
      Set<SMGObject> pObjsPointingAtTraversed) {
    if (pTraversedObjects.contains(nestedMemory)) {
      return;
    }
    pTraversedObjects.add(nestedMemory);
    SMG smg = getMemoryModel().getSmg();

    pObjsPointingAtTraversed.addAll(smg.getAllSourcesForPointersPointingTowards(nestedMemory));

    FluentIterable<SMGHasValueEdge> conntectedMemoryPtrs =
        smg.getHasValueEdgesByPredicate(
            nestedMemory, h -> !h.hasValue().isZero() && smg.isPointer(h.hasValue()));
    for (SMGHasValueEdge ptrEdge : conntectedMemoryPtrs) {
      SMGObject connectedMemory = smg.getPTEdge(ptrEdge.hasValue()).orElseThrow().pointsTo();
      gatherConnectedMemory(connectedMemory, pTraversedObjects, pObjsPointingAtTraversed);
      assert pTraversedObjects.contains(connectedMemory)
          || pObjsPointingAtTraversed.contains(nestedMemory);
    }
  }

  /**
   * Checks that the 2 given {@link SMGObject}s are in a list, such that objWPointersTowardsTarget
   * has either a next or prev ptr towards target. Checks validity, heap inclusion and size of
   * objWPointersTowardsTarget.
   *
   * @param target the object that the ptrs from objWPointersTowardsTarget point towards.
   * @param objWPointersTowardsTarget a potential list object for the same list target belongs to.
   * @param nfo next pointer offset.
   * @param nextPointerTargetOffset next pointer target offset.
   * @return true if objWPointersTowardsTarget has pointers pointing towards target (either nfo or
   *     pfo) and both have matching list shape.
   */
  public boolean areTwoObjectsPartOfList(
      SMGObject target,
      SMGObject objWPointersTowardsTarget,
      BigInteger nfo,
      BigInteger nextPointerTargetOffset) {
    return areTwoObjectsPartOfList(
        target,
        objWPointersTowardsTarget,
        nfo,
        nextPointerTargetOffset,
        Optional.empty(),
        Optional.empty());
  }

  /**
   * Checks that the 2 given {@link SMGObject}s are in a list, such that objWPointersTowardsTarget
   * has either a next or prev ptr towards target. Checks validity, heap inclusion and size of
   * objWPointersTowardsTarget.
   *
   * @param target the object that the ptrs from objWPointersTowardsTarget point towards.
   * @param objWPointersTowardsTarget a potential list object for the same list target belongs to.
   * @param nfo next pointer offset.
   * @param nextPointerTargetOffset next pointer target offset.
   * @param maybePfo prev ptr offset. Empty for ignoring it.
   * @param prevPointerTargetOffset prev ptr target offset. Empty for ignoring.
   * @return true if objWPointersTowardsTarget has pointers pointing towards target (either nfo or
   *     pfo) and both have matching list shape.
   */
  public boolean areTwoObjectsPartOfList(
      SMGObject target,
      SMGObject objWPointersTowardsTarget,
      BigInteger nfo,
      BigInteger nextPointerTargetOffset,
      Optional<BigInteger> maybePfo,
      Optional<BigInteger> prevPointerTargetOffset) {
    SMG smg = getMemoryModel().getSmg();
    // Pointer from the list to the list are OK.
    // TODO: Alternatively pointer that point towards each list segment are OK.

    // 1 or more ptrs towards target
    if (objWPointersTowardsTarget.equals(target)) {
      // Self-pointer is OK?
      // return false;
    }
    if (!objWPointersTowardsTarget.getSize().equals(target.getSize())) {
      // Size does not match
      return false;
    } else if (!getMemoryModel().isHeapObject(objWPointersTowardsTarget)
        || !getMemoryModel().isHeapObject(target)) {
      // Ptr source is not on the heap
      return false;
    } else if (!smg.isValid(objWPointersTowardsTarget) || !smg.isValid(target)) {
      // Ptr source is not valid
      return false;
    }

    if (target instanceof SMGSinglyLinkedListSegment sllTarget) {
      if (!sllTarget.getNextOffset().equals(nfo)
          || !sllTarget.getNextPointerTargetOffset().equals(nextPointerTargetOffset)) {
        return false;
      }
    }
    if (objWPointersTowardsTarget instanceof SMGSinglyLinkedListSegment sllOtherObj) {
      if (!sllOtherObj.getNextOffset().equals(nfo)
          || !sllOtherObj.getNextPointerTargetOffset().equals(nextPointerTargetOffset)) {
        return false;
      }
    }

    SMGAndHasValueEdges nfoReadEdges =
        smg.readValue(objWPointersTowardsTarget, nfo, smg.getSizeOfPointer(), false);
    if (nfoReadEdges.getHvEdges().size() != 1
        || !smg.isPointer(nfoReadEdges.getHvEdges().get(0).hasValue())) {
      return false;
    }
    SMGPointsToEdge nfoPteForHv =
        smg.getPTEdge(nfoReadEdges.getHvEdges().get(0).hasValue()).orElseThrow();
    if (!nfoPteForHv.pointsTo().equals(target)
        || !nfoPteForHv.getOffset().isNumericValue()
        || !nfoPteForHv
            .getOffset()
            .asNumericValue()
            .bigIntegerValue()
            .equals(nextPointerTargetOffset)) {
      // Next ptr location/Offset does not match
      if (maybePfo.isEmpty()) {
        return false;
      } else {
        BigInteger pfo = maybePfo.orElseThrow();
        SMGAndHasValueEdges pfoReadEdges =
            smg.readValue(objWPointersTowardsTarget, pfo, smg.getSizeOfPointer(), false);
        if (pfoReadEdges.getHvEdges().size() != 1
            || !smg.isPointer(pfoReadEdges.getHvEdges().get(0).hasValue())) {
          return false;
        }
        SMGPointsToEdge pfoPteForHv =
            smg.getPTEdge(pfoReadEdges.getHvEdges().get(0).hasValue()).orElseThrow();
        if (!pfoPteForHv.pointsTo().equals(target)
            || !pfoPteForHv.getOffset().isNumericValue()
            || !pfoPteForHv
                .getOffset()
                .asNumericValue()
                .bigIntegerValue()
                .equals(prevPointerTargetOffset.orElseThrow())) {
          // Prev ptr location does not match
          return false;
        }
      }
    }

    return true;
  }

  /**
   * Returns the number of global and local variables in the memory model.
   *
   * @return num of vars.
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
        evaluator,
        statistics);
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
    throw new RuntimeException("copyAndForget(): FIX ME");
    /*
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
            */
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
    } catch (CPATransferException e) {
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
   * Only abstracts lists with equal values.
   */
  public SMGState abstractIntoDLL(
      SMGObject root,
      BigInteger nfo,
      BigInteger nextPointerTargetOffset,
      BigInteger pfo,
      BigInteger prevPointerTargetOffset,
      Set<SMGObject> alreadyVisited,
      Set<SMGNode> alreadyIncremented)
      throws SMGException {
    statistics.incrementListAbstractions();
    // Check that the next object exists, is valid, has the same size and the same value in head
    Optional<SMGValueAndSMGObject> maybeNext = getValidNextSLL(root, nfo);

    if (maybeNext.isEmpty() || maybeNext.orElseThrow().getObject().equals(root)) {
      return this;
    }

    assert getMemoryModel().checkSMGSanity();
    SMGObject nextObj = maybeNext.orElseThrow().getObject();
    SMGValue nextPtrFromRoot = maybeNext.orElseThrow().getValue();

    if (alreadyVisited.contains(nextObj)) {
      // We check for next as this might happen:
      // list1 -> list2 -> loop1 -> loop to leftmost
      // We check list1 -> list2, can't merge
      // We check list2 -> loop1, can't merge
      // We check list1 -> list1, CAN merge
      // Now list2 is found again (first next) and abort
      return this;
    }

    // Values not equal, pointer offsets not equal, pointer in between elements, continue traverse
    EqualityCache<Value> eqCache = EqualityCache.of();
    EqualityCache<SMGObject> objectCache = EqualityCache.of();
    if (!checkEqualValuesForTwoStatesWithExemptions(
            nextObj,
            root,
            ImmutableMap.of(nextObj, ImmutableList.of(nfo, pfo), root, ImmutableList.of(nfo, pfo)),
            this,
            this,
            eqCache,
            objectCache,
            new HashSet<>(),
            true,
            true)
        || !isPointerTargetOffsetEqualTo(root, nfo, nextPointerTargetOffset)
        || !isPointerTargetOffsetEqualTo(nextObj, pfo, prevPointerTargetOffset)
        || listElementsHaveOutsidePointerInBetween(
            root,
            nextObj,
            nfo,
            nextPointerTargetOffset,
            Optional.of(pfo),
            Optional.of(prevPointerTargetOffset))) {
      // split lists 3+ -> concrete -> 3+ -> 0 are detected by the abstraction finder and abstracted
      // in 2 calls to this
      return this;
    }

    // Self-pointers are possible in list elements (outside of next and prev)
    // We expect those to uniformly point to itself, and not to the same target
    // (all to first or something like it). These should end up with ALL ptrs.
    Set<SMGValue> selfPointersOfRoot =
        memoryModel.getSmg().getAllPointerValuesPointingTowardsFrom(root, root);
    Set<SMGValue> selfPointersOfNextObj =
        memoryModel.getSmg().getAllPointerValuesPointingTowardsFrom(nextObj, nextObj);
    Preconditions.checkState(selfPointersOfNextObj.size() == selfPointersOfRoot.size());

    // When the equality cache is empty, identical values were found.
    // If it has values, those are equal but not identical.
    // (right = root, left = next)
    // (order in the cache is important, as we carry over the values/pointers of the next element
    //   and want to easily check them later on)

    // Copy the edges from the next object to the DLL
    SMGDoublyLinkedListSegment newDLL;
    if (root.isSLL()) {
      throw new SMGException(
          "Error when abstracting a linked-list. Tried to abstract a singly-linked-list with a"
              + " doubly-linked-list.");

    } else if (root instanceof SMGDoublyLinkedListSegment oldDLLLeft) {
      int newMinLength = oldDLLLeft.getMinLength() + 1;
      if (nextObj instanceof SMGSinglyLinkedListSegment oldDllRight) {
        newMinLength = oldDLLLeft.getMinLength() + oldDllRight.getMinLength();
      }
      newDLL =
          oldDLLLeft.copyWithNewMinimumLength(newMinLength).copyWithNewRelevantEqualities(eqCache);

    } else {
      // Calculate head offset
      // We assume that the head is either at 0 if the nfo is not, or right behind the nfo if it is
      // not at 0, or right behind the pfo if the pfo is right behind nfo
      BigInteger headOffset = BigInteger.ZERO;
      while (nfo.compareTo(headOffset) == 0 || pfo.compareTo(headOffset) == 0) {
        headOffset = headOffset.add(memoryModel.getSizeOfPointer());
      }

      int newMinLength = 2; // 2 concrete elements
      if (nextObj instanceof SMGSinglyLinkedListSegment oldDllRight) {
        newMinLength = 1 + oldDllRight.getMinLength();
      }

      newDLL =
          new SMGDoublyLinkedListSegment(
              root.getNestingLevel(),
              root.getSize(),
              root.getOffset(),
              headOffset,
              nfo,
              nextPointerTargetOffset,
              pfo,
              prevPointerTargetOffset,
              newMinLength,
              eqCache);
    }

    SMGState currentState = copyAndAddObjectToHeap(newDLL);
    // Copy all values from nextObj to the new DLL. The next pointer will be set correctly by this.
    currentState = currentState.copyAllValuesFromObjToObj(nextObj, newDLL);
    // Write prev from root into the new DLL prev
    SMGValueAndSMGState prevPointerRoot =
        currentState.readSMGValue(root, pfo, memoryModel.getSizeOfPointer());
    currentState =
        prevPointerRoot
            .getSMGState()
            .writeValueWithoutChecks(
                newDLL, pfo, memoryModel.getSizeOfPointer(), prevPointerRoot.getSMGValue());

    // Remember prev ptr of nextObj to delete the value later
    SMGValue prevPointerNextObj =
        currentState.readSMGValue(nextObj, pfo, memoryModel.getSizeOfPointer()).getSMGValue();

    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState.memoryModel.replaceAllPointersTowardsWithAndSetSpecifier(
                nextObj, newDLL, SMGTargetSpecifier.IS_LAST_POINTER));

    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState.memoryModel.replaceAllPointersTowardsWithAndSetSpecifier(
                root, newDLL, SMGTargetSpecifier.IS_FIRST_POINTER));

    // There might be pointers from the sub-SMG (including self-pointers) with the wrong specifier
    // now (First or Last), that need an ALL!
    // Increment nesting level of nested values and objects based on equality-cache
    // (nested objects are copied and pointers from them back to the S/DLL have the ALL specifier as
    // they point to each list segment, while addresses pointing to the same object for all
    // list-elements are not nested and don't have the all specifier) and set the ALL specifier for
    // qualifying pointers.
    // TODO: we could simplify this and only do it when merging 2 regions, as when using a S/DLL
    //  we already did this and can reuse it.
    currentState =
        currentState.incrementNestingLevelAndSetSpecifierOfSubSMG(newDLL, alreadyIncremented);

    // Remove the 2 old objects and continue to abstract.
    // For this to work without issues, we rewrite pointers in them to 0 first,
    //  then remove the pointers that are not needed anymore,
    //  then we delete their objects/sub-SMGs.
    currentState =
        currentState.writeValueWithoutChecks(
            root, nfo, memoryModel.getSizeOfPointer(), SMGValue.zeroValue());
    currentState =
        currentState.writeValueWithoutChecks(
            nextObj, nfo, memoryModel.getSizeOfPointer(), SMGValue.zeroValue());

    currentState =
        currentState.writeValueWithoutChecks(
            root, pfo, memoryModel.getSizeOfPointer(), SMGValue.zeroValue());
    currentState =
        currentState.writeValueWithoutChecks(
            nextObj, pfo, memoryModel.getSizeOfPointer(), SMGValue.zeroValue());

    // Remove the PTE for the next ptr of root to nextObj if not used anymore
    if (currentState.getMemoryModel().getSmg().getNumberOfSMGValueUsages(nextPtrFromRoot) == 0) {
      currentState =
          currentState.copyAndReplaceMemoryModel(
              currentState
                  .getMemoryModel()
                  .removePointerFromSMGWithoutSideEffectsAndCopy(nextPtrFromRoot));
      currentState = currentState.removeValueMappings(ImmutableSet.of(nextPtrFromRoot));
    }

    // Remove the PTE for the prev ptr of nextObj to root if not used anymore
    if (currentState.getMemoryModel().getSmg().getNumberOfSMGValueUsages(prevPointerNextObj) == 0) {
      currentState =
          currentState.copyAndReplaceMemoryModel(
              currentState
                  .getMemoryModel()
                  .removePointerFromSMGWithoutSideEffectsAndCopy(prevPointerNextObj));
      currentState = currentState.removeValueMappings(ImmutableSet.of(prevPointerNextObj));
    }

    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState.getMemoryModel().copyAndRemoveObjectAndAssociatedSubSMG(root).getSPC());
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState.getMemoryModel().copyAndRemoveObjectAndAssociatedSubSMG(nextObj).getSPC());

    // Remove old self pointer PTE if unused now
    for (SMGValue oldSelfPtr : selfPointersOfNextObj) {
      if (currentState.getMemoryModel().getSmg().getNumberOfValueUsages(oldSelfPtr) == 0) {
        currentState =
            currentState.copyAndReplaceMemoryModel(
                currentState
                    .getMemoryModel()
                    .removePointerFromSMGWithoutSideEffectsAndCopy(oldSelfPtr));
      }
    }

    assert currentState.getMemoryModel().checkSMGSanity();
    assert (currentState.getMemoryModel().getSmg().getNumberOfSMGPointsToEdgesTowards(root) == 0);
    assert (currentState.getMemoryModel().getSmg().getNumberOfSMGPointsToEdgesTowards(nextObj)
        == 0);

    return currentState.abstractIntoDLL(
        newDLL,
        nfo,
        nextPointerTargetOffset,
        pfo,
        prevPointerTargetOffset,
        ImmutableSet.<SMGObject>builder().addAll(alreadyVisited).add(newDLL).build(),
        alreadyIncremented);
  }

  /*
   * Abstracts candidates into an SLL. May abstract the chain behind the first root into more than 1 list! Depending on == values.
   * Only abstracts lists with == values.
   * Last pointers are only set for concrete next segments (each ptr towards a concrete next is set to last)
   * First pointers are only set for each concrete root.
   */
  public SMGState abstractIntoSLL(
      SMGObject root,
      BigInteger nfo,
      BigInteger nextPointerTargetOffset,
      Set<SMGObject> alreadyVisited,
      Set<SMGNode> alreadyIncremented)
      throws SMGException {
    assert !copyAndPruneUnreachable().hasMemoryErrors();
    statistics.incrementListAbstractions();
    // Check that the next object exists, is valid, has the same size and the same value in head
    Optional<SMGValueAndSMGObject> maybeNext = getValidNextSLL(root, nfo);

    if (maybeNext.isEmpty()
        || maybeNext.orElseThrow().getObject().equals(root)
        || alreadyVisited.contains(maybeNext.orElseThrow().getObject())) {
      return this;
    }
    SMGObject nextObj = maybeNext.orElseThrow().getObject();
    SMGValue nextPtrFromRoot = maybeNext.orElseThrow().getValue();

    if (alreadyVisited.contains(nextObj)) {
      // We check for next as this might happen:
      // list1 -> list2 -> loop1 -> loop to leftmost
      // We check list1 -> list2, can't merge
      // We check list2 -> loop1, can't merge
      // We check list1 -> list1, CAN merge
      // Now list2 is found again (first next) and abort
      return this;
    }

    // Values not equal, ptr offset not equal, or outside pointers to next, continue traverse
    EqualityCache<Value> eqCache = EqualityCache.of();
    EqualityCache<SMGObject> objectCache = EqualityCache.of();
    if (!checkEqualValuesForTwoStatesWithExemptions(
            nextObj,
            root,
            ImmutableMap.of(nextObj, ImmutableList.of(nfo), root, ImmutableList.of(nfo)),
            this,
            this,
            eqCache,
            objectCache,
            new HashSet<>(),
            true,
            true)
        || !isPointerTargetOffsetEqualTo(root, nfo, nextPointerTargetOffset)
        || listElementsHaveOutsidePointerInBetween(
            root, nextObj, nfo, nextPointerTargetOffset, Optional.empty(), Optional.empty())) {
      // split lists 3+ -> concrete -> 3+ -> 0 are detected by the abstraction finder and abstracted
      // in 2 calls to this
      return this;
    }
    // When the equality cache is empty, identical values were found.
    // If it has values, those are equal but not identical.
    // (right = root, left = next)
    // (order in the cache is important, as we carry over the values/pointers of the next element
    //   and want to easily check them later on)

    Map<SMGObject, Integer> ptrsTowardsNextObj =
        memoryModel.getSmg().getAllSourcesForPointersPointingTowardsWithNumOfOccurrences(nextObj);
    Preconditions.checkState(
        ptrsTowardsNextObj.size() == 1
            && ptrsTowardsNextObj.entrySet().stream().allMatch(e -> e.getValue() == 1));

    // If it does, create a new SLL with the correct information
    // Copy the edges from one of the objects object into the SLL
    SMGSinglyLinkedListSegment newSLL;
    Preconditions.checkArgument(!(root instanceof SMGDoublyLinkedListSegment));
    if (root instanceof SMGSinglyLinkedListSegment oldSLL) {
      // root is an SLL
      int newMinLength = oldSLL.getMinLength();
      if (nextObj instanceof SMGSinglyLinkedListSegment) {
        newMinLength = newMinLength + ((SMGSinglyLinkedListSegment) nextObj).getMinLength();
      } else {
        newMinLength++;
      }
      newSLL = oldSLL.copyWithNewMinimumLength(newMinLength).copyWithNewRelevantEqualities(eqCache);

    } else {
      // root is a region
      // We assume that the head is either at 0 if the nfo is not, or right behind the nfo if it is
      // not. We don't care about it however
      int newMinLength = 2;
      if (nextObj instanceof SMGSinglyLinkedListSegment) {
        newMinLength = 1 + ((SMGSinglyLinkedListSegment) nextObj).getMinLength();
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
              nextPointerTargetOffset,
              newMinLength,
              eqCache);
    }
    SMGState currentState = copyAndAddObjectToHeap(newSLL);
    currentState = currentState.copyAllValuesFromObjToObj(nextObj, newSLL);

    // Replace ALL pointers that previously pointed to the root or the next object to the SLL.
    // If there is a pointer towards the nextObj, it is now a last pointer. The pointer from root to
    // nextObj is deleted below.
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState.memoryModel.replaceAllPointersTowardsWithAndSetSpecifier(
                nextObj, newSLL, SMGTargetSpecifier.IS_LAST_POINTER));

    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState.memoryModel.replaceAllPointersTowardsWithAndSetSpecifier(
                root, newSLL, SMGTargetSpecifier.IS_FIRST_POINTER));

    // There might be pointers from the sub-SMG (including self-pointers) with the wrong specifier
    // now (First or Last), that need an ALL!
    // Increment nesting level of nested values and objects based on equality-cache
    // (nested objects are copied and pointers from them back to the S/DLL have the ALL specifier as
    // they point to each list segment, while addresses pointing to the same object for all
    // list-elements are not nested and don't have the all specifier) and set the ALL specifier for
    // qualifying pointers.
    // TODO: we could simplify this and only do it when merging 2 regions, as when using a S/DLL
    //  we already did this and can reuse it.
    currentState =
        currentState.incrementNestingLevelAndSetSpecifierOfSubSMG(newSLL, alreadyIncremented);

    // Remove the 2 old objects
    // For this to work without issues, we rewrite the next pointers to 0 in them
    currentState =
        currentState.writeValueWithoutChecks(
            root, nfo, memoryModel.getSizeOfPointer(), SMGValue.zeroValue());

    // Remove the PTE for the next ptr of root to nextObj if not used anymore
    if (currentState.getMemoryModel().getSmg().getNumberOfValueUsages(nextPtrFromRoot) == 0) {
      currentState =
          currentState.copyAndReplaceMemoryModel(
              currentState
                  .getMemoryModel()
                  .removePointerFromSMGWithoutSideEffectsAndCopy(nextPtrFromRoot));
      currentState = currentState.removeValueMappings(ImmutableSet.of(nextPtrFromRoot));
    }

    currentState =
        currentState.writeValueWithoutChecks(
            nextObj, nfo, memoryModel.getSizeOfPointer(), SMGValue.zeroValue());
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState.getMemoryModel().copyAndRemoveObjectAndAssociatedSubSMG(root).getSPC());
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState.getMemoryModel().copyAndRemoveObjectAndAssociatedSubSMG(nextObj).getSPC());

    assert currentState.getMemoryModel().checkSMGSanity();
    assert (currentState.getMemoryModel().getSmg().getNumberOfSMGPointsToEdgesTowards(root) == 0);
    assert (currentState.getMemoryModel().getSmg().getNumberOfSMGPointsToEdgesTowards(nextObj)
        == 0);

    assert !currentState.copyAndPruneUnreachable().hasMemoryErrors();
    return currentState.abstractIntoSLL(
        newSLL,
        nfo,
        nextPointerTargetOffset,
        ImmutableSet.<SMGObject>builder().addAll(alreadyVisited).add(newSLL).build(),
        alreadyIncremented);
  }

  /**
   * Goes through all addresses in the given linked-list segment and increments the nesting level
   * (e.g. parent S/DLL nesting level + 1 if there is no other abstracted object) of the sub-SMG IFF
   * the addresses are truly nested. Nesting is determined by the EqualityCache in the linked-list
   * segment, i.e. if there is no entry for an address from S/DLL, the target is the same object for
   * all list elements, and it is not nested. Entries in the EqualityCache are nested
   * objects/values/sub-SMGs that are copied when materialized. The next and prev (i.e. nfo/pfo)
   * offsets/addresses in the S/DLL given are ignored/not traversed. If an address (PTE) pointing
   * back at the S/DLL is found, and the source object is nested, the address pointing back is
   * labeled with the ALL specifier.
   *
   * @param newLinkedList either a {@link SMGSinglyLinkedListSegment} or a {@link
   *     SMGDoublyLinkedListSegment} who's sub-SMG is supposed to get a nesting update because of
   *     recent abstraction and creation of this segment.
   * @return a new {@link SMGState} with the sub-SMG updated according to the description above.
   */
  private SMGState incrementNestingLevelAndSetSpecifierOfSubSMG(
      SMGSinglyLinkedListSegment newLinkedList, Set<SMGNode> alreadyIncremented) {
    // TODO: check if we can replace alreadyIncremented with a simple nesting level check (see value
    // incrementation as example)
    SMGState currentState = this;
    EqualityCache<Value> eqCache = newLinkedList.getRelevantEqualities();
    ImmutableSet<BigInteger> restrictedOffsets =
        newLinkedList instanceof SMGDoublyLinkedListSegment dll
            ? ImmutableSet.of(dll.getNextOffset(), dll.getPrevOffset())
            : ImmutableSet.of(newLinkedList.getNextOffset());

    alreadyIncremented.add(newLinkedList);
    alreadyIncremented.add(SMGObject.nullInstance());

    for (SMGHasValueEdge hve :
        getMemoryModel()
            .getSmg()
            .getHasValueEdgesByPredicate(
                newLinkedList, h -> !restrictedOffsets.contains(h.getOffset()))) {

      SMGValue smgValue = hve.hasValue();
      Value value = getMemoryModel().getValueFromSMGValue(smgValue).orElseThrow();
      // Each object nested should have a nesting level that is 1 greater than the current
      //  (there can only be 1 parent S/DLL)
      // However, it might increase more if other abstracted memory is encountered.
      if (currentState.getMemoryModel().getSmg().isPointer(smgValue)) {
        if (eqCache.knownKey(value) && !eqCache.isEqualityKnown(value, value)) {
          // Known equality of objects, i.e. the object is truly nested.
          SMGPointsToEdge pte =
              currentState.getMemoryModel().getSmg().getPTEdge(smgValue).orElseThrow();
          SMGObject nestedObj = pte.pointsTo();

          if (nestedObj.equals(newLinkedList)) {
            if (!pte.targetSpecifier().equals(SMGTargetSpecifier.IS_ALL_POINTER)) {
              // Self-pointer for all list elements, set ALL specifier
              currentState =
                  currentState.copyAndReplaceMemoryModel(
                      currentState
                          .getMemoryModel()
                          .copyAndSetTargetSpecifierForPointer(
                              smgValue, SMGTargetSpecifier.IS_ALL_POINTER));
            }
            continue;
          }

          // This also sets the ALL spec for other back ptrs
          currentState =
              currentState.incrementNestingLevelAndSetSpecifierOfSubSMG(
                  nestedObj, newLinkedList, alreadyIncremented, eqCache, 0);

          Preconditions.checkState(
              currentState
                      .getMemoryModel()
                      .getSmg()
                      .getPTEdge(smgValue)
                      .orElseThrow()
                      .pointsTo()
                      .getNestingLevel()
                  == newLinkedList.getNestingLevel() + 1);

        } else {
          // Non-nested region, i.e. when materializing all list elements point to the same obj.
          Preconditions.checkState(
              currentState
                  .getMemoryModel()
                  .getTargetSpecifier(value)
                  .equals(SMGTargetSpecifier.IS_REGION));
        }
      } else if (eqCache.knownKey(value)
          && !value.isNumericValue()
          && !alreadyIncremented.contains(smgValue)
          && !eqCache.isEqualityKnown(value, value)
          && currentState.getMemoryModel().getNestingLevel(smgValue)
              == newLinkedList.getNestingLevel()) {
        alreadyIncremented.add(smgValue);
        // Symbolic values that are distinct need incrementation
        int nestingLevel = currentState.getMemoryModel().getNestingLevel(smgValue);
        // nesting > root is not possible here, it would mean we already incremented it
        currentState =
            currentState.copyAndReplaceMemoryModel(
                currentState
                    .getMemoryModel()
                    .replaceSMGValueNestingLevel(smgValue, nestingLevel + 1));
      }
    }
    return currentState;
  }

  /**
   * This method traverses all memory connected with PTEs in currentMemory and increments the
   * nesting level by 1 and also sets all found PTEs pointing to parentLinkedList to ALL.
   *
   * @param currentObject object who's nesting level is to be incremented by 1.
   * @param parentLinkedList always the one parent of the nesting.
   * @param alreadyIncremented all already incremented or processed objects of the sub-SMG. It is
   *     expected to contain parentLinkedList and the NULL obj at all times.
   * @return a new {@link SMGState} with all reachable objects incremented.
   */
  private SMGState incrementNestingLevelAndSetSpecifierOfSubSMG(
      SMGObject currentObject,
      SMGSinglyLinkedListSegment parentLinkedList,
      Set<SMGNode> alreadyIncremented,
      EqualityCache<Value> eqCache,
      int foundOtherAbstractedObj) {
    if (alreadyIncremented.contains(currentObject)) {
      return this;
    }
    int levelOfParent = parentLinkedList.getNestingLevel();

    if (currentObject.getNestingLevel() > levelOfParent + foundOtherAbstractedObj) {
      alreadyIncremented.add(currentObject);
      return this;
    }

    // Create a new object with nesting level +1, add it equally to the old object (validity etc.)
    // switch all pointers from old object to new, copy all HVEs from old to new, remove old obj
    SMGObject newObject =
        currentObject.copyWithNewNestingLevel(currentObject.getNestingLevel() + 1);

    Preconditions.checkState(
        foundOtherAbstractedObj > 0 || levelOfParent + 1 == newObject.getNestingLevel());
    Preconditions.checkState(
        !(foundOtherAbstractedObj > 0) || levelOfParent + 1 >= newObject.getNestingLevel());
    SymbolicProgramConfiguration spc =
        getMemoryModel().copyAndReplaceObjectAndRemoveOld(currentObject, newObject);
    SMGState currentState = this.copyAndReplaceMemoryModel(spc);

    alreadyIncremented.add(currentObject);
    alreadyIncremented.add(newObject);

    SMG smg = spc.getSmg();

    // Get all values from this obj and traverse subSMG for pointers that are nested.
    for (SMGHasValueEdge hve :
        smg.getSMGObjectsWithSMGHasValueEdges().getOrDefault(newObject, PersistentSet.of())) {
      SMGValue smgValue = hve.hasValue();
      int valueNestingLvl = smg.getNestingLevel(smgValue);
      Value value = currentState.getMemoryModel().getValueFromSMGValue(smgValue).orElseThrow();
      int nestingLevel = currentState.getMemoryModel().getNestingLevel(smgValue);

      if (smg.isPointer(smgValue)) {
        SMGPointsToEdge pte = smg.getPTEdge(smgValue).orElseThrow();
        SMGTargetSpecifier specifier = pte.targetSpecifier();
        SMGObject target = pte.pointsTo();

        // Target (and value) nesting level should be exactly at the same level for nested objects
        // that have not yet been incremented
        int targetNestingLvl = target.getNestingLevel();

        if (target.isZero()) {
          assert specifier.equals(SMGTargetSpecifier.IS_REGION);
          assert targetNestingLvl == 0;
          assert valueNestingLvl == 0;
          continue;
        } else if (target.equals(parentLinkedList)
            && !specifier.equals(SMGTargetSpecifier.IS_ALL_POINTER)) {
          // Set ALL spec, as the current obj is nested
          currentState =
              currentState.copyAndReplaceMemoryModel(
                  currentState
                      .getMemoryModel()
                      .copyAndSetTargetSpecifierForPointer(
                          smgValue, SMGTargetSpecifier.IS_ALL_POINTER));
        }

        currentState =
            currentState.incrementNestingLevelAndSetSpecifierOfSubSMG(
                target,
                parentLinkedList,
                alreadyIncremented,
                eqCache,
                currentObject instanceof SMGSinglyLinkedListSegment
                    ? foundOtherAbstractedObj + 1
                    : foundOtherAbstractedObj);

      } else if (eqCache.knownKey(value)
          && !value.isNumericValue()
          && !alreadyIncremented.contains(smgValue)
          && nestingLevel == levelOfParent + foundOtherAbstractedObj) {
        alreadyIncremented.add(smgValue);
        // Symbolic values that are distinct need incrementation
        currentState =
            currentState.copyAndReplaceMemoryModel(
                currentState
                    .getMemoryModel()
                    .replaceSMGValueNestingLevel(smgValue, nestingLevel + 1));
      }
    }
    return currentState;
  }

  public SMGState removeUnusedValues() {
    return copyAndReplaceMemoryModel(memoryModel.removeUnusedValues());
  }

  public SMGState removeValueMappings(Set<SMGValue> valueMappingsToRemove) {
    return copyAndReplaceMemoryModel(memoryModel.removeValueMappings(valueMappingsToRemove));
  }

  private boolean isPointerTargetOffsetEqualTo(
      SMGObject pRoot, BigInteger readOffsetOfPointer, BigInteger pNextPointerTargetOffset) {
    SMG smg = getMemoryModel().getSmg();
    List<SMGHasValueEdge> readValues =
        smg.readValue(pRoot, readOffsetOfPointer, getMemoryModel().getSizeOfPointer(), false)
            .getHvEdges();
    if (readValues.size() == 1) {
      SMGHasValueEdge readValue = readValues.get(0);
      if (smg.isPointer(readValue.hasValue())
          && smg.getPTEdge(readValue.hasValue()).orElseThrow().getOffset().isNumericValue()
          && smg.getPTEdge(readValue.hasValue())
              .orElseThrow()
              .getOffset()
              .asNumericValue()
              .bigIntegerValue()
              .equals(pNextPointerTargetOffset)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if there is a valid next linked list object and returns it if yes. Traverses the nfo
   * pointer and checks if the next object is valid and the same size as root.
   *
   * @param root current root object (left side of 2 list elements)
   * @param nfo (suspected) next pointer offset in root
   * @return optional filled with valid next sll or empty
   * @throws SMGException critical errors
   */
  private Optional<SMGValueAndSMGObject> getValidNextSLL(SMGObject root, BigInteger nfo)
      throws SMGException {
    SMGState currentState = this;
    SMGValueAndSMGState valueAndState =
        currentState.readSMGValue(root, nfo, memoryModel.getSizeOfPointer());
    SMGValue value = valueAndState.getSMGValue();
    if (!memoryModel.getSmg().isPointer(value)) {
      return Optional.empty();
    }
    SMGObject nextObject = memoryModel.getSmg().getPTEdge(value).orElseThrow().pointsTo();
    Value rootSize = root.getSize();
    Value nextSize = nextObject.getSize();
    if (!rootSize.isNumericValue() || !nextSize.isNumericValue()) {
      // TODO: handle with solver
      throw new SMGException("Symbolic memory size in linked list abstraction can not be handled.");
    } else if (!memoryModel.getSmg().isValid(nextObject)
        || rootSize
                .asNumericValue()
                .bigIntegerValue()
                .compareTo(nextSize.asNumericValue().bigIntegerValue())
            != 0) {
      return Optional.empty();
    }
    // Same object size, same content expect for the pointers, its valid -> ok
    // We don't need the state as it would only change for unknown reads
    return Optional.of(SMGValueAndSMGObject.of(value, nextObject));
  }

  /**
   * Invalidates variables. For local variables that i.e. went out of scope.
   *
   * @param variable {@link MemoryLocation} for the variable to be invalidated.
   * @param deleteDanglingPointers if true, does delete points-to-edges that are no longer saved in
   *     any memory location after invalidating the given.
   * @return a new state with the variables SMGObject invalid.
   */
  public SMGState invalidateVariable(MemoryLocation variable, boolean deleteDanglingPointers) {
    if (isLocalOrGlobalVariablePresent(variable)) {
      Optional<SMGObject> maybeVariableObject =
          memoryModel.getObjectForVisibleVariable(variable.getQualifiedName());
      if (maybeVariableObject.isPresent()) {
        // Get objects present outside the current scope
        Set<SMGObject> otherPresentObjects = memoryModel.getObjectsValidInOtherStackFrames();
        // Pointers from may be given as array argument, then we have the object, but don't own it,
        // hence no heap objs.
        if (!otherPresentObjects.contains(maybeVariableObject.orElseThrow())
            && !memoryModel.isHeapObject(maybeVariableObject.orElseThrow())) {
          return copyAndReplaceMemoryModel(
              memoryModel.invalidateSMGObject(
                  maybeVariableObject.orElseThrow(), deleteDanglingPointers));
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
   * @return Optional filled with the {@link SMGObjectAndOffsetMaybeNestingLvl} of the target of the
   *     pointer. Empty if its not a pointer in the current {@link SymbolicProgramConfiguration}.
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
            ptEdge.pointsTo(), ptEdge.getOffset(), currentState));
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
      // Nesting is ordered that the pointer to the first element (from the left) starts with
      // abstractionMinLen - 1, then decrements until the last concrete element has nesting lvl 0
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
          returnBuilder.add(
              SMGStateAndOptionalSMGObjectAndOffset.of(
                  ptEdge.pointsTo(), ptEdge.getOffset(), currentState));
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
            ptEdge.pointsTo(), ptEdge.getOffset(), currentState));
  }

  /**
   * Test/Debug method. Dereferences without materializing lists. Not to be used in regular
   * execution. Expects the pointer parameter to be a valid pointer!
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
            ptEdge.pointsTo(), ptEdge.getOffset(), currentState));
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
        throw new SMGException(
            "Critical error in materialization. Either a pointer was pointing to zero, or to an"
                + " invalid memory region. This is a critical problem with the analysis. Please"
                + " report in CPAchecker issue tracker.");
      }
      // DLLs are also SLLs
      Preconditions.checkArgument(obj instanceof SMGSinglyLinkedListSegment);
      materializationAndState =
          currentState.materializer.handleMaterialisation(
              valueToPointerToAbstractObject, (SMGSinglyLinkedListSegment) obj, currentState);
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
        // The values might point to a SMGSinglyLinkedListSegments if a followup is not merged
        // The 0 state does not materialize anything, the 1 state does materialize one more concrete
        // list segment and appends another 0+ segment after that

        // This can be violated for example through a last pointer still pointing to a 0+ after left
        // sided mat, so check that
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
  public Optional<Value> transformAddressIntoNumericValue(Value addressValue) throws SMGException {
    Value offset;
    SMGObject target;
    if (addressValue instanceof AddressExpression addressExpr) {
      if (addressExpr.getOffset().isNumericValue()) {
        offset = addressExpr.getOffset();
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
      offset = evaluator.addBitOffsetValues(offset, ptEdge.getOffset());

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
    if (!offset.isNumericValue()) {
      throw new SMGException(
          "Symbolic pointer offsets can not be used to assume a numerical offset.");
    }
    return Optional.of(
        new NumericValue(
            memoryModel
                .getNumericAssumptionForMemoryRegion(target)
                .add(offset.asNumericValue().bigIntegerValue())));
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
        // TODO: is the nesting level and specifier here correct?
        currentState =
            currentState.createAndAddPointer(
                ptrToCopiedTarget,
                copyOfTarget,
                currentState.memoryModel.getTypeForValue(hve.hasValue()),
                ptEdgeToTarget.getOffset(),
                nestingLevel,
                ptEdgeToTarget.targetSpecifier());
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

  /**
   * True if the entered value is a pointer returned from malloc(0). These pointers are never
   * allowed to be evaluated, but they may be assigned to other variables.
   *
   * @param pPointerValue some {@link Value}. Does not have to be a pointer.
   * @return true if it is a pointers that has been returned by malloc(0).
   */
  public boolean isPointingToMallocZero(Value pPointerValue) {
    if (pPointerValue instanceof AddressExpression addrExpr) {
      pPointerValue = addrExpr.getMemoryAddress();
    }
    Optional<SMGValue> maybeSMGValue = getMemoryModel().getSMGValueFromValue(pPointerValue);
    if (maybeSMGValue.isPresent()) {
      SMGValue smgValue = maybeSMGValue.orElseThrow();
      return getMemoryModel().isPointingToMallocZero(smgValue);
    }
    return false;
  }

  public SMGCPAStatistics getStatistics() {
    return statistics;
  }

  @Override
  public @Nullable Object getPartitionKey() {
    return getMemoryModel().getSmg().getNumberOfAbstractedLists() * 100000 + getSize();
  }

  public boolean createdAtBlockEnd() {
    return blockEnd;
  }

  public boolean mergeAtBlockEnd() {
    return options.getJoinOnBlockEnd();
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
    public boolean knownKey(V thisEqual) {
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

    public void removeEquality(V pThisObj, V pOtherObj) {
      if (knownKey(pThisObj)) {
        primitiveCache.remove(pThisObj, pOtherObj);
      }
    }
  }
}
