// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.oc;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializers;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.mutex.MutexFunctions;
import org.sosy_lab.cpachecker.cpa.mutex.MutexLock;
import org.sosy_lab.cpachecker.cpa.oc.ThreadInstance.InstanceKey;
import org.sosy_lab.cpachecker.cpa.por.GlobalAccessRenamer;
import org.sosy_lab.cpachecker.cpa.por.GlobalAccessRenamer.UnsupportedAccessException;
import org.sosy_lab.cpachecker.cpa.por.PorEdgeCloner;
import org.sosy_lab.cpachecker.cpa.por.ThreadFunctions;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

/**
 * Expands the per-thread exploration trees: all leaving edges are followed regardless of
 * feasibility, thread creations spawn disjunct root states (one per dynamic thread instance), and
 * all events, guards, and guarded edge formulas are recorded in the {@link OcExplorationRegistry}.
 */
public class OrderingConsistencyTransferRelation implements TransferRelation {

  private static final ImmutableSet<String> PROGRAM_EXIT_FUNCTIONS =
      ImmutableSet.of("abort", "exit", "_exit", "_Exit", "__assert_fail");

  /** Functions with this name prefix execute atomically per the SV-COMP convention. */
  private static final String ATOMIC_FUNCTION_PREFIX = "__VERIFIER_atomic_";

  private final OrderingConsistencyCPA cpa;
  private final ShutdownNotifier shutdownNotifier;
  private final PathFormulaManager pathFormulaManager;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final MachineModel machineModel;
  private final Set<String> registeredBases = new HashSet<>();
  private final Set<String> aggregateInitEmitted = new HashSet<>();
  private OcExplorationRegistry registry;
  private @Nullable Formula zeroOffsetTerm;

  OrderingConsistencyTransferRelation(
      OrderingConsistencyCPA pCpa, ShutdownNotifier pShutdownNotifier) {
    cpa = pCpa;
    shutdownNotifier = pShutdownNotifier;
    registry = pCpa.getRegistry();
    pathFormulaManager = pCpa.getPathFormulaManager();
    fmgr = pCpa.getSolver().getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    machineModel = pCpa.getCfa().getMachineModel();
  }

  /** Starts collecting into a fresh registry (one iterative-deepening round). */
  void resetRegistry(OcExplorationRegistry pRegistry) {
    registry = pRegistry;
    registeredBases.clear();
    aggregateInitEmitted.clear();
    zeroOffsetTerm = null;
  }

  /** Creates an event chained in program order after all of the state's last events. */
  private MemoryEvent addEventAfter(
      OrderingConsistencyState pState,
      EventKind pKind,
      @Nullable MemoryLocation pMemoryLocation,
      @Nullable String pCssaName,
      @Nullable Formula pVariable,
      @Nullable String pMutexId,
      int pOtherInstanceId,
      @Nullable String pRegionId,
      @Nullable Formula pAddressTerm,
      @Nullable CFAEdge pEdge) {
    ImmutableList<Integer> predecessors = pState.getLastEventIds();
    MemoryEvent event =
        registry.addEvent(
            pState.getInstanceId(),
            pKind,
            predecessors.isEmpty() ? MemoryEvent.NO_EVENT : predecessors.get(0),
            pState.getGuard(),
            pMemoryLocation,
            pCssaName,
            pVariable,
            pMutexId,
            pOtherInstanceId,
            pRegionId,
            pAddressTerm,
            null,
            false,
            pEdge);
    for (int i = 1; i < predecessors.size(); i++) {
      registry.addPoPredecessor(event.id(), predecessors.get(i));
    }
    return event;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pState, Precision pPrecision)
      throws CPATransferException, InterruptedException {
    shutdownNotifier.shutdownIfNecessary();
    OrderingConsistencyState state = (OrderingConsistencyState) pState;
    if (state.isTarget()) {
      return ImmutableList.of();
    }
    state.markExpanded();

    CFANode node = state.getLocationState().getLocationNode();
    if (node instanceof FunctionExitNode && state.getCallstackState().getDepth() == 1) {
      // the thread's start routine returns: normal termination, the only joinable kind of path
      addThreadExitEvent(state, null);
      return ImmutableList.of();
    }
    List<CFAEdge> leavingEdges = node.getLeavingEdges().toList();
    if (leavingEdges.isEmpty()) {
      return ImmutableList.of();
    }

    List<AbstractState> successors = new ArrayList<>();
    if (leavingEdges.size() == 2
        && leavingEdges.get(0) instanceof CAssumeEdge first
        && leavingEdges.get(1) instanceof CAssumeEdge second) {
      handleAssumePair(state, first, second, successors);
    } else {
      for (CFAEdge edge : leavingEdges) {
        handleEdge(state, pPrecision, edge, successors);
      }
    }
    return ImmutableList.copyOf(successors);
  }

  private void handleEdge(
      OrderingConsistencyState pState,
      Precision pPrecision,
      CFAEdge pEdge,
      List<AbstractState> pSuccessors)
      throws CPATransferException, InterruptedException {
    String callee = getCalledFunctionName(pEdge);
    if (callee != null && cpa.getErrorFunctions().contains(callee)) {
      handleError(pState, pEdge, pSuccessors);
      return;
    }
    if (callee != null && ThreadFunctions.isThreadExitFunction(callee)) {
      addThreadExitEvent(pState, pEdge); // the path ends here, the thread terminates normally
      return;
    }
    if (callee != null && PROGRAM_EXIT_FUNCTIONS.contains(callee)) {
      // the whole program dies here; the event blocks any pthread_join of this instance
      addEventAfter(
          pState, EventKind.ABORT, null, null, null, null, MemoryEvent.NO_INSTANCE, null, null,
          pEdge);
      return;
    }
    if (callee != null && ThreadFunctions.isCreateFunction(callee)) {
      handleCreate(pState, pEdge, pSuccessors);
      return;
    }
    if (callee != null && ThreadFunctions.isJoinFunction(callee)) {
      handleJoin(pState, pEdge, pSuccessors);
      return;
    }
    if (pEdge instanceof CFunctionCallEdge
        && callee != null
        && callee.startsWith(ATOMIC_FUNCTION_PREFIX)) {
      // SV-COMP semantics: the body of a __VERIFIER_atomic_* function executes atomically. Model
      // the call as acquiring the global atomic-block pseudo-mutex; the matching release happens
      // at the function-return edge below. A body that never returns (abort/error inside) keeps
      // the section open to the path leaf, like an unclosed __VERIFIER_atomic_begin.
      MemoryEvent lockEvent =
          addEventAfter(
              pState,
              EventKind.LOCK,
              null,
              null,
              null,
              MemoryEvent.ATOMIC_BLOCK_MUTEX,
              MemoryEvent.NO_INSTANCE,
              null,
              null,
              pEdge);
      handleRegularEdge(
          withAtomicSection(pState, ImmutableList.of(lockEvent.id()), 1),
          pPrecision,
          pEdge,
          pSuccessors);
      return;
    }
    if (pEdge instanceof CFunctionReturnEdge
        && pEdge.getPredecessor().getFunctionName().startsWith(ATOMIC_FUNCTION_PREFIX)) {
      int firstNew = pSuccessors.size();
      handleRegularEdge(pState, pPrecision, pEdge, pSuccessors);
      for (int i = firstNew; i < pSuccessors.size(); i++) {
        OrderingConsistencyState successor = (OrderingConsistencyState) pSuccessors.get(i);
        MemoryEvent unlockEvent =
            addEventAfter(
                successor,
                EventKind.UNLOCK,
                null,
                null,
                null,
                MemoryEvent.ATOMIC_BLOCK_MUTEX,
                MemoryEvent.NO_INSTANCE,
                null,
                null,
                pEdge);
        pSuccessors.set(
            i, withAtomicSection(successor, ImmutableList.of(unlockEvent.id()), -1));
      }
      return;
    }
    if (handleMutex(pState, pEdge, pSuccessors)) {
      return;
    }
    handleRegularEdge(pState, pPrecision, pEdge, pSuccessors);
  }

  /**
   * Copy of the state entering or leaving a {@code __VERIFIER_atomic_*} function: the last events
   * point at the freshly added LOCK/UNLOCK event and the atomic-block lock depth is adjusted.
   */
  private OrderingConsistencyState withAtomicSection(
      OrderingConsistencyState pState, ImmutableList<Integer> pLastEventIds, int pDepthDelta) {
    return new OrderingConsistencyState(
        pState.getInstanceId(),
        pState.getLocationState(),
        pState.getCallstackState(),
        pState.getPathFormula(),
        pState.getGuard(),
        pLastEventIds,
        pState.getCreateCounts(),
        pState.getThreadHandles(),
        pState.getLoopCounts(),
        withLockDepth(pState.getLockDepths(), MemoryEvent.ATOMIC_BLOCK_MUTEX, pDepthDelta),
        pState.isTarget());
  }

  private void addThreadExitEvent(OrderingConsistencyState pState, @Nullable CFAEdge pEdge) {
    addEventAfter(
        pState, EventKind.THREAD_EXIT, null, null, null, null, MemoryEvent.NO_INSTANCE, null, null,
        pEdge);
  }

  /** Returns the name of the function called by this edge, or null if it is not a call. */
  private static @Nullable String getCalledFunctionName(CFAEdge pEdge) {
    if (pEdge instanceof CFunctionCallEdge callEdge) {
      return callEdge.getSuccessor().getFunctionName();
    }
    return MutexFunctions.getFunctionCallName(pEdge);
  }

  private void handleError(
      OrderingConsistencyState pState, CFAEdge pEdge, List<AbstractState> pSuccessors) {
    MemoryEvent event =
        addEventAfter(
            pState, EventKind.ERROR, null, null, null, null, MemoryEvent.NO_INSTANCE, null, null,
            pEdge);
    pSuccessors.add(
        new OrderingConsistencyState(
            pState.getInstanceId(),
            locationAt(pEdge.getSuccessor()),
            pState.getCallstackState(),
            pState.getPathFormula(),
            pState.getGuard(),
            ImmutableList.of(event.id()),
            pState.getCreateCounts(),
            pState.getThreadHandles(),
            pState.getLoopCounts(),
            pState.getLockDepths(),
            true));
  }

  private void handleCreate(
      OrderingConsistencyState pState, CFAEdge pEdge, List<AbstractState> pSuccessors)
      throws CPATransferException, InterruptedException {
    List<? extends AExpression> params = getCallParameters(pEdge);
    String function;
    String handle;
    try {
      function = ThreadFunctions.extractCreateFunctionName(params);
      handle = ThreadFunctions.extractCreateHandle(params);
    } catch (IllegalStateException e) {
      // e.g. thread handles stored in arrays
      throw new UnsupportedCodeException(e.getMessage(), pEdge);
    }
    int ordinal = pState.getCreateCounts().getOrDefault(function, 0);

    // the thread argument must be modeled, not ignored: a null constant carries no data, the
    // address of a global is bound to that global's allocation base, anything else is unsupported
    CVariableDeclaration argTarget = null;
    CExpression argument = stripCasts((CExpression) params.get(3));
    if (argument instanceof CUnaryExpression unary
        && unary.getOperator() == CUnaryExpression.UnaryOperator.AMPER
        && unary.getOperand() instanceof CIdExpression id
        && id.getDeclaration() instanceof CVariableDeclaration decl
        && decl.isGlobal()) {
      argTarget = decl;
    } else if (!(argument instanceof CIntegerLiteralExpression literal
        && literal.getValue().signum() == 0)) {
      throw new UnsupportedCodeException("unsupported thread argument", pEdge);
    }

    FunctionEntryNode entry = cpa.getCfa().getAllFunctions().get(function);
    if (entry == null) {
      throw new UnsupportedCodeException(
          "pthread_create of function without body: " + function, pEdge);
    }

    InstanceKey key = new InstanceKey(pState.getInstanceId(), function, ordinal);
    ThreadInstance existing = registry.getInstance(key);
    boolean isNew = existing == null;
    ThreadInstance instance = isNew ? registry.newInstance(key) : existing;

    MemoryEvent event =
        addEventAfter(
            pState, EventKind.CREATE, null, null, null, null, instance.getId(), null, null, pEdge);
    registry.addCreateEvent(instance.getId(), event.id());

    PathFormula rootFormula = pathFormulaManager.makeEmptyPathFormula();
    if (argTarget != null && !entry.getFunctionParameters().isEmpty()) {
      rootFormula = bindThreadArgument(pState, pEdge, instance, entry, argTarget);
    }

    addSuccessor(
        pSuccessors,
        pState,
        pEdge.getSuccessor(),
        pState.getCallstackState(),
        pState.getPathFormula(),
        pState.getGuard(),
        ImmutableList.of(event.id()),
        withEntry(pState.getCreateCounts(), function, ordinal + 1),
        withEntry(pState.getThreadHandles(), handle, instance.getId()),
        pState.getLockDepths());

    if (isNew) {
      // the created thread is not a control-flow successor of its spawner (no CFA edge connects
      // them); queue its root so the algorithm seeds it as a separate parentless root of the
      // exploration forest, keeping the reached set's ARG one tree per thread instance
      cpa.addPendingThreadRoot(
          new OrderingConsistencyState(
              instance.getId(),
              locationAt(entry),
              (CallstackState)
                  cpa.getCallstackCPA()
                      .getInitialState(entry, StateSpacePartition.getDefaultPartition()),
              rootFormula,
              bfmgr.makeTrue(),
              ImmutableList.of(),
              ImmutableMap.of(),
              ImmutableMap.of(),
              ImmutableMap.of(),
              ImmutableMap.of(),
              false));
    }
  }

  /**
   * Constrains the created thread's parameter to the address of the global whose address is passed.
   * The parameter symbol is indexed in the returned root context, so the thread's own reads use the
   * same SSA index as the binding (built identically for every create site of the instance).
   */
  private PathFormula bindThreadArgument(
      OrderingConsistencyState pState,
      CFAEdge pEdge,
      ThreadInstance pInstance,
      FunctionEntryNode pEntry,
      CVariableDeclaration pArgTarget)
      throws CPATransferException, InterruptedException {
    var parameter = pEntry.getFunctionParameters().get(0);
    String argName = "T" + pInstance.getId() + "_" + parameter.getQualifiedName();
    CType argType = (CType) parameter.getType();
    String baseName = baseNameFor(pArgTarget.getQualifiedName());
    CType baseType = new CPointerType(CTypeQualifiers.NONE, pArgTarget.getType());

    PathFormula rootFormula =
        indexSymbol(pathFormulaManager.makeEmptyPathFormula(), argName, argType, pEdge);
    PathFormula resolution =
        indexSymbol(
            pathFormulaManager.makeEmptyPathFormulaWithContextFrom(rootFormula),
            baseName,
            baseType,
            pEdge);
    Formula argTerm = resolve(resolution, argName, argType, pEdge);
    Formula baseTerm = resolve(resolution, baseName, baseType, pEdge);
    if (registeredBases.add(baseName)) {
      registry.addAddressBase(baseTerm);
    }
    registry.addPathConstraint(
        bfmgr.implication(pState.getGuard(), fmgr.makeEqual(argTerm, baseTerm)));
    return rootFormula;
  }

  /**
   * Resolves the mutex argument of a lock/unlock call to the identity of the mutex <em>object</em>.
   * {@code &m} names the object directly. A plain pointer-typed id is sound only when it is a
   * parameter of the current function: the context-sensitive callstack then determines the
   * argument it was bound to at the call site, and the resolution recurses on that argument in the
   * caller's frame. Using the syntactic name itself would wrongly identify different mutexes
   * passed through the same parameter (goblint munge/funarg pattern) and mask races. Anything
   * unresolvable is rejected rather than guessed.
   */
  private String resolveMutexObject(
      @Nullable CExpression pExpression, @Nullable CallstackState pFrame, CFAEdge pEdge)
      throws UnsupportedCodeException {
    CExpression expression = pExpression == null ? null : stripCasts(pExpression);
    if (expression instanceof CUnaryExpression unary
        && unary.getOperator() == CUnaryExpression.UnaryOperator.AMPER
        && unary.getOperand() instanceof CIdExpression id) {
      return id.getName();
    }
    if (expression instanceof CIdExpression id
        && id.getDeclaration() instanceof CParameterDeclaration parameter
        && pFrame != null
        && pFrame.getCallNode() != null) {
      for (CFunctionCallEdge callEdge :
          pFrame.getCallNode().getLeavingEdges().filter(CFunctionCallEdge.class)) {
        FunctionEntryNode entry = callEdge.getSuccessor();
        if (!entry.getFunctionName().equals(pFrame.getCurrentFunction())) {
          continue;
        }
        List<? extends AParameterDeclaration> parameters = entry.getFunctionParameters();
        List<? extends AExpression> arguments =
            callEdge.getFunctionCall().getFunctionCallExpression().getParameterExpressions();
        for (int i = 0; i < parameters.size() && i < arguments.size(); i++) {
          if (parameters.get(i) instanceof CParameterDeclaration candidate
              && candidate.getQualifiedName().equals(parameter.getQualifiedName())
              && arguments.get(i) instanceof CExpression argument) {
            return resolveMutexObject(argument, pFrame.getPreviousState(), pEdge);
          }
        }
      }
    }
    throw new UnsupportedCodeException("mutex identity not recognized", pEdge);
  }

  private static CExpression stripCasts(CExpression pExpression) {
    CExpression result = pExpression;
    while (result instanceof CCastExpression cast) {
      result = cast.getOperand();
    }
    return result;
  }

  private void handleJoin(
      OrderingConsistencyState pState, CFAEdge pEdge, List<AbstractState> pSuccessors)
      throws CPATransferException {
    List<? extends AExpression> params = getCallParameters(pEdge);
    String handle;
    try {
      handle = ThreadFunctions.extractJoinHandle(params);
    } catch (IllegalStateException e) {
      // e.g. thread handles stored in arrays
      throw new UnsupportedCodeException(e.getMessage(), pEdge);
    }
    Integer joined = pState.getThreadHandles().get(handle);
    if (joined == null) {
      throw new UnsupportedCodeException("pthread_join with unknown thread handle", pEdge);
    }
    MemoryEvent event =
        addEventAfter(pState, EventKind.JOIN, null, null, null, null, joined, null, null, pEdge);
    addSuccessor(
        pSuccessors,
        pState,
        pEdge.getSuccessor(),
        pState.getCallstackState(),
        pState.getPathFormula(),
        pState.getGuard(),
        ImmutableList.of(event.id()),
        pState.getCreateCounts(),
        pState.getThreadHandles(),
        pState.getLockDepths());
  }

  /** Handles mutex and atomic-block edges; returns false if the edge is none of those. */
  private boolean handleMutex(
      OrderingConsistencyState pState, CFAEdge pEdge, List<AbstractState> pSuccessors)
      throws CPATransferException {
    EventKind kind;
    String mutexId;
    boolean readLock = false;
    if (MutexFunctions.isAtomicBeginCall(pEdge)) {
      kind = EventKind.LOCK;
      mutexId = MemoryEvent.ATOMIC_BLOCK_MUTEX;
    } else if (MutexFunctions.isAtomicEndCall(pEdge)) {
      kind = EventKind.UNLOCK;
      mutexId = MemoryEvent.ATOMIC_BLOCK_MUTEX;
    } else {
      MutexLock lock = MutexFunctions.getLockMutex(pEdge);
      MutexLock unlock = MutexFunctions.getUnlockMutex(pEdge);
      if (lock != null) {
        kind = EventKind.LOCK;
        readLock = lock.isReadLock();
      } else if (unlock != null) {
        kind = EventKind.UNLOCK;
      } else {
        kind = null;
      }
      if (kind != null) {
        List<? extends AExpression> arguments = getCallParameters(pEdge);
        mutexId =
            resolveMutexObject(
                arguments.isEmpty() || !(arguments.get(0) instanceof CExpression argument)
                    ? null
                    : argument,
                pState.getCallstackState(),
                pEdge);
      } else {
        mutexId = null;
      }
      if (kind == null) {
        String callee = MutexFunctions.getFunctionCallName(pEdge);
        if (callee != null
            && (MutexFunctions.isInitFunction(callee)
                || MutexFunctions.isDestroyFunction(callee))) {
          addSuccessor(
              pSuccessors,
              pState,
              pEdge.getSuccessor(),
              pState.getCallstackState(),
              pState.getPathFormula(),
              pState.getGuard(),
              pState.getLastEventIds(),
              pState.getCreateCounts(),
              pState.getThreadHandles(),
              pState.getLockDepths());
          return true;
        }
        return false;
      }
    }
    MemoryEvent event =
        addEventAfter(
            pState, kind, null, null, null, mutexId, MemoryEvent.NO_INSTANCE, null, null, pEdge);
    if (readLock) {
      registry.markReadLock(event.id());
    }
    ImmutableMap<String, Integer> lockDepths =
        withLockDepth(pState.getLockDepths(), mutexId, kind == EventKind.LOCK ? 1 : -1);
    addSuccessor(
        pSuccessors,
        pState,
        pEdge.getSuccessor(),
        pState.getCallstackState(),
        pState.getPathFormula(),
        pState.getGuard(),
        ImmutableList.of(event.id()),
        pState.getCreateCounts(),
        pState.getThreadHandles(),
        lockDepths);
    return true;
  }

  private void handleRegularEdge(
      OrderingConsistencyState pState,
      Precision pPrecision,
      CFAEdge pEdge,
      List<AbstractState> pSuccessors)
      throws CPATransferException, InterruptedException {
    Collection<? extends AbstractState> callstackSuccessors =
        cpa.getCallstackCPA()
            .getTransferRelation()
            .getAbstractSuccessorsForEdge(pState.getCallstackState(), pPrecision, pEdge);
    if (callstackSuccessors.isEmpty()) {
      return; // e.g. a return edge to the wrong call site
    }
    CallstackState nextCallstack = (CallstackState) Iterables.getOnlyElement(callstackSuccessors);

    CollectingRenamer renamer = new CollectingRenamer();
    PathFormula edgeFormula;
    try {
      CFAEdge rewritten = PorEdgeCloner.cloneSingleEdge(pEdge, pState.getInstanceId(), renamer);
      edgeFormula =
          pathFormulaManager.makeAnd(
              pathFormulaManager.makeEmptyPathFormulaWithContextFrom(pState.getPathFormula()),
              rewritten);
    } catch (GlobalAccessRenamer.UnsupportedAccessException e) {
      throw new UnsupportedCodeException(e.getMessage(), pEdge);
    }
    BooleanFormula formula = edgeFormula.getFormula();
    // Converter-minted fresh symbols (malloc!N, nondet!N, ...) share one namespace across thread
    // instances because every instance explores from an empty SSA context; prefix them with the
    // instance id, otherwise e.g. two threads' mallocs are forced to return the same pointer,
    // which contradicts heap-base distinctness and makes the whole violation query unsatisfiable.
    formula =
        fmgr.renameFreeVariablesAndUFs(
            formula,
            name ->
                name.indexOf('!') >= 0 ? "T" + pState.getInstanceId() + "_" + name : name);
    if (!bfmgr.isTrue(formula)) {
      registry.addPathConstraint(bfmgr.implication(pState.getGuard(), formula));
    }
    ImmutableList<Integer> lastEventIds = chainAccessEvents(pState, renamer, edgeFormula, pEdge);
    lastEventIds = handleMallocIfAny(pState, pEdge, renamer, edgeFormula, lastEventIds);

    addSuccessor(
        pSuccessors,
        pState,
        pEdge.getSuccessor(),
        nextCallstack,
        edgeFormula,
        pState.getGuard(),
        lastEventIds,
        pState.getCreateCounts(),
        pState.getThreadHandles(),
        pState.getLockDepths());
  }

  /**
   * Both branches of a condition share one rewritten condition expression and hence one set of read
   * events, so their guards are genuinely mutually exclusive.
   */
  private void handleAssumePair(
      OrderingConsistencyState pState,
      CAssumeEdge pFirst,
      CAssumeEdge pSecond,
      List<AbstractState> pSuccessors)
      throws CPATransferException, InterruptedException {
    CollectingRenamer renamer = new CollectingRenamer();
    CAssumeEdge rewrittenFirst;
    try {
      rewrittenFirst =
          (CAssumeEdge) PorEdgeCloner.cloneSingleEdge(pFirst, pState.getInstanceId(), renamer);
    } catch (GlobalAccessRenamer.UnsupportedAccessException e) {
      throw new UnsupportedCodeException(e.getMessage(), pFirst);
    }
    CAssumeEdge rewrittenSecond =
        new CAssumeEdge(
            pSecond.getRawStatement(),
            pSecond.getFileLocation(),
            pSecond.getPredecessor(),
            pSecond.getSuccessor(),
            rewrittenFirst.getExpression(),
            pSecond.getTruthAssumption(),
            pSecond.isSwapped(),
            pSecond.isArtificialIntermediate());

    PathFormula firstFormula =
        pathFormulaManager.makeAnd(
            pathFormulaManager.makeEmptyPathFormulaWithContextFrom(pState.getPathFormula()),
            rewrittenFirst);
    ImmutableList<Integer> sharedLastEventIds =
        chainAccessEvents(pState, renamer, firstFormula, pFirst);
    PathFormula secondFormula =
        pathFormulaManager.makeAnd(
            pathFormulaManager.makeEmptyPathFormulaWithContextFrom(pState.getPathFormula()),
            rewrittenSecond);

    addSuccessor(
        pSuccessors,
        pState,
        pFirst.getSuccessor(),
        pState.getCallstackState(),
        firstFormula,
        bfmgr.and(pState.getGuard(), firstFormula.getFormula()),
        sharedLastEventIds,
        pState.getCreateCounts(),
        pState.getThreadHandles(),
        pState.getLockDepths());
    addSuccessor(
        pSuccessors,
        pState,
        pSecond.getSuccessor(),
        pState.getCallstackState(),
        secondFormula,
        bfmgr.and(pState.getGuard(), secondFormula.getFormula()),
        sharedLastEventIds,
        pState.getCreateCounts(),
        pState.getThreadHandles(),
        pState.getLockDepths());
  }

  /**
   * Registers the accesses collected while rewriting one edge, reads before writes (matching
   * evaluation order), chained in program order after all of the state's last events. Returns the
   * singleton list of the new last event id, or the state's unchanged last event ids if there were
   * no accesses.
   */
  private ImmutableList<Integer> chainAccessEvents(
      OrderingConsistencyState pState,
      CollectingRenamer pRenamer,
      PathFormula pEdgeFormula,
      CFAEdge pEdge)
      throws CPATransferException, InterruptedException {
    // symbols that occur in no formula (replaced dereferences and their pointers, address
    // constants) are indexed via trivial assume edges in a throwaway resolution context
    PathFormula resolution = pathFormulaManager.makeEmptyPathFormulaWithContextFrom(pEdgeFormula);
    for (PendingAccess access : pRenamer.accesses) {
      if (!resolution.getSsa().containsVariable(access.cssaName)) {
        resolution = indexSymbol(resolution, access.cssaName, access.type, pEdge);
      }
      if (access.addressName != null && !resolution.getSsa().containsVariable(access.addressName)) {
        resolution = indexSymbol(resolution, access.addressName, access.addressType, pEdge);
      }
    }
    for (PendingBase base : pRenamer.mintedBases) {
      if (!resolution.getSsa().containsVariable(base.name())) {
        resolution = indexSymbol(resolution, base.name(), base.type(), pEdge);
      }
      if (registeredBases.add(base.name())) {
        registry.addAddressBase(resolve(resolution, base.name(), base.type(), pEdge));
      }
    }

    ImmutableList<Integer> predecessors = pState.getLastEventIds();
    int lastEventId = predecessors.isEmpty() ? MemoryEvent.NO_EVENT : predecessors.get(0);
    boolean anyEvent = false;

    for (CVariableDeclaration aggregate : pRenamer.aggregateDecls) {
      if (!aggregateInitEmitted.add(aggregate.getQualifiedName())) {
        continue;
      }
      String baseName = baseNameFor(aggregate.getQualifiedName());
      CType baseType = new CPointerType(CTypeQualifiers.NONE, aggregate.getType());
      if (!resolution.getSsa().containsVariable(baseName)) {
        resolution = indexSymbol(resolution, baseName, baseType, pEdge);
      }
      Formula baseTerm = resolve(resolution, baseName, baseType, pEdge);
      if (registeredBases.add(baseName)) {
        registry.addAddressBase(baseTerm);
      }
      for (AggregateCell cell : aggregateInitCells(aggregate, pEdge)) {
        String initName = registry.freshCssaName("__oc_agginit");
        resolution = indexSymbol(resolution, initName, cell.type(), pEdge);
        Formula valueTerm = resolve(resolution, initName, cell.type(), pEdge);
        if (cell.value() != null) {
          registry.addPathConstraint(
              bfmgr.implication(
                  pState.getGuard(),
                  fmgr.makeEqual(
                      valueTerm, fmgr.makeNumber(fmgr.getFormulaType(valueTerm), cell.value()))));
        }
        Formula offsetTerm =
            cell.fill() ? null : evaluateInt(longLiteral(cell.offset()), resolution, pEdge);
        MemoryEvent event =
            registry.addEvent(
                pState.getInstanceId(),
                EventKind.WRITE,
                lastEventId,
                pState.getGuard(),
                null,
                initName,
                valueTerm,
                null,
                MemoryEvent.NO_INSTANCE,
                regionOf(cell.type()),
                baseTerm,
                offsetTerm,
                cell.fill(),
                pEdge);
        if (!anyEvent) {
          for (int i = 1; i < predecessors.size(); i++) {
            registry.addPoPredecessor(event.id(), predecessors.get(i));
          }
          anyEvent = true;
        }
        lastEventId = event.id();
      }
    }

    for (boolean writes : new boolean[] {false, true}) {
      for (PendingAccess access : pRenamer.accesses) {
        if (access.write != writes) {
          continue;
        }
        Formula variable = resolve(resolution, access.cssaName, access.type, pEdge);
        Formula addressTerm =
            access.addressName == null
                ? null
                : resolve(resolution, access.addressName, access.addressType, pEdge);
        Formula offsetTerm =
            access.regionId == null ? null : offsetTermOf(access, resolution, pEdge);
        MemoryEvent event =
            registry.addEvent(
                pState.getInstanceId(),
                access.write ? EventKind.WRITE : EventKind.READ,
                lastEventId,
                pState.getGuard(),
                access.memoryLocation,
                access.cssaName,
                variable,
                null,
                MemoryEvent.NO_INSTANCE,
                access.regionId,
                addressTerm,
                offsetTerm,
                false,
                pEdge);
        if (access.type.getCanonicalType().isAtomic()) {
          // an access to an _Atomic-qualified lvalue is atomic and cannot be one side of a data
          // race; canonicalize because the qualifier may sit on a typedef'd real type (atomic_int)
          registry.markAtomicAccess(event.id());
        }
        if (!anyEvent) {
          for (int i = 1; i < predecessors.size(); i++) {
            registry.addPoPredecessor(event.id(), predecessors.get(i));
          }
          anyEvent = true;
        }
        lastEventId = event.id();
      }
    }
    return anyEvent ? ImmutableList.of(lastEventId) : pState.getLastEventIds();
  }

  /** Brings a symbol that occurs in no formula into the SSA context via a trivial assumption. */
  private PathFormula indexSymbol(PathFormula pFormula, String pName, CType pType, CFAEdge pEdge)
      throws CPATransferException, InterruptedException {
    CIdExpression id = syntheticIdExpression(pName, pType);
    CBinaryExpression identity =
        new CBinaryExpression(
            FileLocation.DUMMY,
            CNumericTypes.INT,
            pType,
            id,
            id,
            CBinaryExpression.BinaryOperator.EQUALS);
    CAssumeEdge indexingEdge =
        new CAssumeEdge(
            "", FileLocation.DUMMY, pEdge.getPredecessor(), pEdge.getSuccessor(), identity, true);
    return pathFormulaManager.makeAnd(pFormula, indexingEdge);
  }

  private Formula resolve(PathFormula pContext, String pName, CType pType, CFAEdge pEdge)
      throws UnsupportedCodeException {
    try {
      return pathFormulaManager.makeFormulaForVariable(pContext, pName, pType);
    } catch (IllegalArgumentException e) {
      // the converter never referenced the symbol, so the construct is unsupported
      throw new UnsupportedCodeException(pName + " has no formula encoding", pEdge);
    }
  }

  /**
   * Evaluates an (already cloned) integer expression to a formula by binding it to a fresh symbol
   * via a synthetic assumption and resolving that symbol; reuses the path-formula converter for all
   * arithmetic instead of interpreting the expression here. The binding is asserted globally so the
   * returned symbol is constrained to the expression's value (the symbol is fresh, so an unguarded
   * binding is sound: it only feeds this access's guarded address constraints).
   */
  private Formula evaluateInt(CExpression pExpr, PathFormula pContext, CFAEdge pEdge)
      throws CPATransferException, InterruptedException {
    CType type = pExpr.getExpressionType();
    String name = registry.freshCssaName("__oc_idx");
    CIdExpression id = syntheticIdExpression(name, type);
    CBinaryExpression binding =
        new CBinaryExpression(
            FileLocation.DUMMY,
            CNumericTypes.INT,
            type,
            id,
            pExpr,
            CBinaryExpression.BinaryOperator.EQUALS);
    CAssumeEdge bindingEdge =
        new CAssumeEdge(
            "", FileLocation.DUMMY, pEdge.getPredecessor(), pEdge.getSuccessor(), binding, true);
    PathFormula bound = pathFormulaManager.makeAnd(pContext, bindingEdge);
    registry.addPathConstraint(bound.getFormula());
    return resolve(bound, name, type, pEdge);
  }

  /** Binds the freshly written pointer of a {@code lhs = malloc(...)} to a distinct heap base. */
  private ImmutableList<Integer> handleMallocIfAny(
      OrderingConsistencyState pState,
      CFAEdge pEdge,
      CollectingRenamer pRenamer,
      PathFormula pEdgeFormula,
      ImmutableList<Integer> pLastEventIds)
      throws CPATransferException, InterruptedException {
    if (!(pEdge instanceof AStatementEdge statementEdge)
        || !(statementEdge.getStatement() instanceof CFunctionCallAssignmentStatement call)
        || !(call.getFunctionCallExpression().getFunctionNameExpression()
            instanceof CIdExpression function)
        || !function.getName().equals("malloc")) {
      return pLastEventIds;
    }
    if (!(call.getLeftHandSide() instanceof CIdExpression lhs)
        || !(lhs.getExpressionType().getCanonicalType() instanceof CPointerType pointerType)) {
      throw new UnsupportedCodeException("unsupported form of malloc", pEdge);
    }
    CType pointee = pointerType.getType();

    String lhsName;
    if (lhs.getDeclaration() instanceof CVariableDeclaration decl && decl.isGlobal()) {
      lhsName = null;
      for (int i = pRenamer.accesses.size() - 1; i >= 0; i--) {
        if (pRenamer.accesses.get(i).write) {
          lhsName = pRenamer.accesses.get(i).cssaName;
          break;
        }
      }
      if (lhsName == null) {
        throw new UnsupportedCodeException("unsupported form of malloc", pEdge);
      }
    } else {
      lhsName = "T" + pState.getInstanceId() + "_" + lhs.getDeclaration().getQualifiedName();
    }
    CType lhsType = lhs.getExpressionType();

    PathFormula resolution = pathFormulaManager.makeEmptyPathFormulaWithContextFrom(pEdgeFormula);
    String baseName = registry.freshCssaName("__oc_heap");
    resolution = indexSymbol(resolution, baseName, lhsType, pEdge);

    Formula lhsTerm = resolve(resolution, lhsName, lhsType, pEdge);
    Formula baseTerm = resolve(resolution, baseName, lhsType, pEdge);
    registry.addAddressBase(baseTerm);
    registry.addPathConstraint(
        bfmgr.implication(pState.getGuard(), fmgr.makeEqual(lhsTerm, baseTerm)));

    // the allocation provides one indeterminate value across each of its scalar leaf regions: a
    // fill write covers the whole allocation, so a later read at any offset can read from it and
    // uninitialised reads stay feasible (cells of one region share one indeterminate value)
    ImmutableList<Integer> lastEventIds = pLastEventIds;
    for (CType leafType : scalarLeafTypes(pointee)) {
      String initName = registry.freshCssaName("__oc_heapinit");
      resolution = indexSymbol(resolution, initName, leafType, pEdge);
      int primary = lastEventIds.isEmpty() ? MemoryEvent.NO_EVENT : lastEventIds.get(0);
      MemoryEvent initialWrite =
          registry.addEvent(
              pState.getInstanceId(),
              EventKind.WRITE,
              primary,
              pState.getGuard(),
              null,
              initName,
              resolve(resolution, initName, leafType, pEdge),
              null,
              MemoryEvent.NO_INSTANCE,
              regionOf(leafType),
              baseTerm,
              null,
              true,
              pEdge);
      for (int i = 1; i < lastEventIds.size(); i++) {
        registry.addPoPredecessor(initialWrite.id(), lastEventIds.get(i));
      }
      lastEventIds = ImmutableList.of(initialWrite.id());
    }
    return lastEventIds;
  }

  /**
   * One initial write of an aggregate global. A fill cell covers the whole object of its region
   * with a uniform value; a point cell writes one scalar at a literal byte offset (an explicit
   * non-zero initializer entry). A null value means an indeterminate (extern) fill.
   */
  private record AggregateCell(
      CType type, boolean fill, @Nullable Long offset, @Nullable BigInteger value) {}

  private static final int MAX_AGGREGATE_POINT_CELLS = 256;

  /**
   * The initial writes of an aggregate global declaration: an extern object has indeterminate
   * contents (one fill per scalar region), while a defined object is zero-initialized (one
   * zero-valued fill per scalar region) with the explicit non-zero initializer entries overlaid as
   * point writes. One fill covers an array of any size.
   */
  private ImmutableList<AggregateCell> aggregateInitCells(
      CVariableDeclaration pDeclaration, CFAEdge pEdge) throws CPATransferException {
    if (pDeclaration.getType().getCanonicalType() instanceof CElaboratedType) {
      return ImmutableList.of(); // incomplete type: address-only object, no value cells
    }
    if (pDeclaration.getCStorageClass() == CStorageClass.EXTERN) {
      ImmutableList.Builder<AggregateCell> cells = ImmutableList.builder();
      for (CType leafType : scalarLeafTypes(pDeclaration.getType())) {
        cells.add(new AggregateCell(leafType, true, null, null));
      }
      return cells.build();
    }
    // C zero-initializes static storage: one zero-valued fill per scalar leaf region covers the
    // whole object (an array of any size), and the explicit non-zero initializer entries are
    // overlaid as point writes, which take precedence at their cell because they are emitted after
    // the fills
    List<AggregateCell> cells = new ArrayList<>();
    for (CType leafType : scalarLeafTypes(pDeclaration.getType())) {
      cells.add(new AggregateCell(leafType, true, null, BigInteger.ZERO));
    }
    int points = 0;
    // reuse CPAchecker's own initializer flattener (resolves designators, nesting, partial lists)
    for (CExpressionAssignmentStatement assignment :
        CInitializers.convertToAssignments(pDeclaration, pEdge)) {
      BigInteger value = literalValueOf(assignment, pEdge);
      if (value.signum() == 0) {
        continue; // already covered by the zero fill
      }
      if (++points > MAX_AGGREGATE_POINT_CELLS) {
        throw new UnsupportedCodeException("aggregate with too many explicit initializers", pEdge);
      }
      long offset = staticOffsetOf(assignment.getLeftHandSide(), pEdge);
      CType type = assignment.getLeftHandSide().getExpressionType().getCanonicalType();
      cells.add(new AggregateCell(type, false, offset, value));
    }
    return ImmutableList.copyOf(cells);
  }

  /** The constant byte offset of a designator-resolved lvalue rooted at the declared object. */
  private long staticOffsetOf(CExpression pLvalue, CFAEdge pEdge) throws UnsupportedCodeException {
    CExpression lvalue = stripCasts(pLvalue);
    if (lvalue instanceof CIdExpression) {
      return 0;
    }
    if (lvalue instanceof CFieldReference field && !field.isPointerDereference()) {
      CType ownerType = field.getFieldOwner().getExpressionType().getCanonicalType();
      if (ownerType instanceof CCompositeType composite) {
        return staticOffsetOf(field.getFieldOwner(), pEdge)
            + fieldOffsetBytes(composite, field.getFieldName());
      }
    }
    if (lvalue instanceof CArraySubscriptExpression subscript
        && stripCasts(subscript.getSubscriptExpression())
            instanceof CIntegerLiteralExpression idx) {
      return staticOffsetOf(subscript.getArrayExpression(), pEdge)
          + idx.getValue().longValueExact() * sizeofBytes(subscript.getExpressionType());
    }
    throw new UnsupportedCodeException("unsupported aggregate initializer target", pEdge);
  }

  private static BigInteger literalValueOf(
      CExpressionAssignmentStatement pAssignment, CFAEdge pEdge) throws UnsupportedCodeException {
    if (stripCasts(pAssignment.getRightHandSide()) instanceof CIntegerLiteralExpression literal) {
      return literal.getValue();
    }
    throw new UnsupportedCodeException("non-literal aggregate initializer value", pEdge);
  }

  /** The distinct scalar member types an object of the given type decomposes into. */
  private static ImmutableList<CType> scalarLeafTypes(CType pType) {
    Set<CType> leaves = new java.util.LinkedHashSet<>();
    collectScalarLeafTypes(pType.getCanonicalType(), leaves);
    return ImmutableList.copyOf(leaves);
  }

  private static void collectScalarLeafTypes(CType pCanonical, Set<CType> pLeaves) {
    if (pCanonical instanceof CCompositeType composite) {
      for (CCompositeType.CCompositeTypeMemberDeclaration member : composite.getMembers()) {
        collectScalarLeafTypes(member.getType().getCanonicalType(), pLeaves);
      }
    } else if (pCanonical instanceof CArrayType array) {
      collectScalarLeafTypes(array.getType().getCanonicalType(), pLeaves);
    } else if (isValueType(pCanonical)) {
      pLeaves.add(pCanonical);
    }
    // void / incomplete leaves (e.g. void* malloc) have no typed cell until accessed through a
    // typed pointer; they get no initial write here
  }

  /** Whether a value of this canonical type can be held in a declared solver symbol. */
  private static boolean isValueType(CType pCanonical) {
    return !(pCanonical instanceof CVoidType) && !(pCanonical instanceof CElaboratedType);
  }

  private static List<? extends AExpression> getCallParameters(CFAEdge pEdge)
      throws CPATransferException {
    if (pEdge instanceof AStatementEdge statementEdge
        && statementEdge.getStatement() instanceof AFunctionCall call) {
      return call.getFunctionCallExpression().getParameterExpressions();
    }
    throw new UnsupportedCodeException("unsupported form of thread-function call", pEdge);
  }

  /** Adds the successor state unless the loop bound cuts it. */
  private void addSuccessor(
      List<AbstractState> pSuccessors,
      OrderingConsistencyState pState,
      CFANode pNextNode,
      CallstackState pCallstack,
      PathFormula pPathFormula,
      BooleanFormula pGuard,
      ImmutableList<Integer> pLastEventIds,
      ImmutableMap<String, Integer> pCreateCounts,
      ImmutableMap<String, Integer> pThreadHandles,
      ImmutableMap<String, Integer> pLockDepths) {
    ImmutableMap<CFANode, Integer> loopCounts = pState.getLoopCounts();
    if (pNextNode.isLoopStart()) {
      int count = loopCounts.getOrDefault(pNextNode, 0) + 1;
      if (count > cpa.getMaxLoopIterations()) {
        registry.markTruncated();
        // the thread has not terminated on this path, so it can never be joined here
        int primary = pLastEventIds.isEmpty() ? MemoryEvent.NO_EVENT : pLastEventIds.get(0);
        MemoryEvent truncated =
            registry.addEvent(
                pState.getInstanceId(),
                EventKind.TRUNCATED,
                primary,
                pGuard,
                null,
                null,
                null,
                null,
                MemoryEvent.NO_INSTANCE,
                null);
        for (int i = 1; i < pLastEventIds.size(); i++) {
          registry.addPoPredecessor(truncated.id(), pLastEventIds.get(i));
        }
        return;
      }
      loopCounts = withEntry(loopCounts, pNextNode, count);
    }
    pSuccessors.add(
        new OrderingConsistencyState(
            pState.getInstanceId(),
            locationAt(pNextNode),
            pCallstack,
            pPathFormula,
            pGuard,
            pLastEventIds,
            pCreateCounts,
            pThreadHandles,
            loopCounts,
            pLockDepths,
            false));
  }

  private org.sosy_lab.cpachecker.cpa.location.LocationState locationAt(CFANode pNode) {
    return cpa.getLocationCPA().getStateFactory().getState(pNode);
  }

  private static <K> ImmutableMap<K, Integer> withEntry(
      ImmutableMap<K, Integer> pMap, K pKey, int pValue) {
    ImmutableMap.Builder<K, Integer> builder = ImmutableMap.builder();
    for (var entry : pMap.entrySet()) {
      if (!entry.getKey().equals(pKey)) {
        builder.put(entry);
      }
    }
    builder.put(pKey, pValue);
    return builder.buildOrThrow();
  }

  /**
   * Updates one mutex's lock depth by the given delta, clamped at zero; entries at zero are removed
   * so the map stays in canonical (merge-key) form.
   */
  private static ImmutableMap<String, Integer> withLockDepth(
      ImmutableMap<String, Integer> pLockDepths, String pMutexId, int pDelta) {
    int depth = Math.max(0, pLockDepths.getOrDefault(pMutexId, 0) + pDelta);
    ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
    for (var entry : pLockDepths.entrySet()) {
      if (!entry.getKey().equals(pMutexId)) {
        builder.put(entry);
      }
    }
    if (depth > 0) {
      builder.put(pMutexId, depth);
    }
    return builder.buildOrThrow();
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge) {
    throw new UnsupportedOperationException(
        "the ordering-consistency exploration computes successors per state");
  }

  /** Collects one pending READ/WRITE record per renamed access, in callback order. */
  private final class CollectingRenamer implements GlobalAccessRenamer {
    private final List<PendingAccess> accesses = new ArrayList<>();
    private final List<PendingBase> mintedBases = new ArrayList<>();
    private final List<CVariableDeclaration> aggregateDecls = new ArrayList<>();

    @Override
    public String freshName(CVariableDeclaration pDeclaration, boolean pIsWrite) {
      String qualifiedName = pDeclaration.getQualifiedName();
      String cssaName = registry.freshCssaName(qualifiedName);
      CType canonicalType = pDeclaration.getType().getCanonicalType();
      if (canonicalType instanceof CCompositeType
          || canonicalType instanceof CArrayType
          || canonicalType instanceof CElaboratedType) {
        // aggregate globals have no whole-object value symbol; their cells live in the aliasing
        // regime, seeded with initial writes when the declaration is first seen. An incomplete
        // (only forward-declared) type can never be value-accessed, so it gets no cells at all
        aggregateDecls.add(pDeclaration);
        return cssaName;
      }
      PendingAccess access =
          new PendingAccess(
              MemoryLocation.forDeclaration(pDeclaration),
              cssaName,
              pIsWrite,
              pDeclaration.getType());
      if (cpa.getAddressedVariables().contains(qualifiedName)) {
        // a pointer may also reach this variable, so it takes part in address-equality read-from
        access.regionId = regionOf(pDeclaration.getType());
        access.addressName = baseNameFor(qualifiedName);
        access.addressType = new CPointerType(CTypeQualifiers.NONE, pDeclaration.getType());
        mintedBases.add(new PendingBase(access.addressName, access.addressType));
      }
      accesses.add(access);
      return cssaName;
    }

    @Override
    public CExpression replaceAddressOf(CUnaryExpression pOriginalAddressOf) {
      CExpression operand = pOriginalAddressOf.getOperand();
      if (operand instanceof CIdExpression id) {
        if (id.getExpressionType().getCanonicalType() instanceof CFunctionType) {
          return null; // function designators keep the default cloning
        }
        if (id.getDeclaration() instanceof CVariableDeclaration decl && decl.isGlobal()) {
          String baseName = baseNameFor(decl.getQualifiedName());
          CType baseType = new CPointerType(CTypeQualifiers.NONE, decl.getType());
          mintedBases.add(new PendingBase(baseName, baseType));
          return syntheticIdExpression(baseName, baseType);
        }
      }
      throw new UnsupportedAccessException(
          "unsupported address-of expression: " + pOriginalAddressOf.toASTString());
    }

    @Override
    public CIdExpression replaceAliasedAccess(
        CExpression pOriginalAccess, boolean pIsWrite, UnaryOperator<CExpression> pSubCloner) {
      CType valueType = pOriginalAccess.getExpressionType();
      CType canonical = valueType.getCanonicalType();
      if (!(canonical instanceof CSimpleType || canonical instanceof CPointerType)) {
        return null; // only scalar cells; fall back to default cloning
      }
      PendingAccess access =
          new PendingAccess(null, registry.freshCssaName("__oc_mem"), pIsWrite, valueType);
      BaseInfo base = analyzeLvalue(pOriginalAccess, access, pSubCloner);
      if (base == null) {
        return null; // a local, non-aliased object; fall back to default cloning
      }
      access.regionId = regionOf(valueType);
      access.addressName = base.name();
      access.addressType = base.type();
      accesses.add(access);
      return syntheticIdExpression(access.cssaName, valueType);
    }

    /**
     * Walks an lvalue, accumulating its byte offset (constant field/literal parts plus symbolic
     * index contributions) into the access, and returns the base object's identity: a distinct
     * constant for a global object, or the value of a pointer. Returns null if the base is a local
     * non-pointer object (not an aliased global access).
     */
    private @Nullable BaseInfo analyzeLvalue(
        CExpression pLvalue, PendingAccess pAccess, UnaryOperator<CExpression> pSubCloner) {
      CExpression lvalue = stripCasts(pLvalue);
      if (lvalue instanceof CIdExpression id) {
        if (id.getDeclaration() instanceof CVariableDeclaration decl && decl.isGlobal()) {
          return mintObjectBase(decl.getQualifiedName(), decl.getType());
        }
        return null; // local object base
      }
      if (lvalue instanceof CPointerExpression deref) {
        return pointerBase(deref.getOperand(), pSubCloner);
      }
      if (lvalue instanceof CArraySubscriptExpression subscript) {
        CExpression arrayExpr = stripCasts(subscript.getArrayExpression());
        long stride = sizeofBytes(subscript.getExpressionType());
        CExpression index = subscript.getSubscriptExpression();
        if (index instanceof CIntegerLiteralExpression literal) {
          addOffset(pAccess, longLiteral(literal.getValue().longValueExact() * stride));
        } else {
          addOffset(pAccess, scaled(pSubCloner.apply(index), stride));
        }
        if (arrayExpr.getExpressionType().getCanonicalType() instanceof CArrayType) {
          return analyzeLvalue(arrayExpr, pAccess, pSubCloner); // array object indexed
        }
        return pointerBase(arrayExpr, pSubCloner); // pointer indexed (p[i])
      }
      if (lvalue instanceof CFieldReference field) {
        CExpression owner = field.getFieldOwner();
        CType ownerType =
            field.isPointerDereference()
                ? ((CPointerType) stripCasts(owner).getExpressionType().getCanonicalType())
                    .getType()
                : owner.getExpressionType();
        if (!(ownerType.getCanonicalType() instanceof CCompositeType composite)) {
          throw new UnsupportedAccessException("field of non-composite: " + lvalue.toASTString());
        }
        addOffset(pAccess, longLiteral(fieldOffsetBytes(composite, field.getFieldName())));
        return field.isPointerDereference()
            ? pointerBase(owner, pSubCloner) // owner->field
            : analyzeLvalue(owner, pAccess, pSubCloner); // owner.field
      }
      throw new UnsupportedAccessException("unsupported aliased access: " + lvalue.toASTString());
    }

    /** The base of a pointer expression: the cloned pointer's value symbol. */
    private BaseInfo pointerBase(CExpression pPointer, UnaryOperator<CExpression> pSubCloner) {
      CExpression cloned = stripCasts(pSubCloner.apply(pPointer));
      if (cloned instanceof CIdExpression id
          && id.getExpressionType().getCanonicalType() instanceof CPointerType) {
        return new BaseInfo(id.getDeclaration().getQualifiedName(), id.getExpressionType());
      }
      throw new UnsupportedAccessException("unsupported pointer base: " + pPointer.toASTString());
    }

    /** A distinct base constant for a global object of the given qualified name and type. */
    private BaseInfo mintObjectBase(String pQualifiedName, CType pObjectType) {
      String baseName = baseNameFor(pQualifiedName);
      CType baseType = new CPointerType(CTypeQualifiers.NONE, pObjectType);
      mintedBases.add(new PendingBase(baseName, baseType));
      return new BaseInfo(baseName, baseType);
    }
  }

  /** The base object identity of an aliased access. */
  private record BaseInfo(String name, CType type) {}

  private long sizeofBytes(CType pType) {
    return machineModel.getSizeof(pType.getCanonicalType()).longValueExact();
  }

  private long fieldOffsetBytes(CCompositeType pComposite, String pField) {
    return machineModel.getFieldOffsetInBits(pComposite, pField).longValueExact()
        / machineModel.getSizeofCharInBits();
  }

  /** One renamed access; region fields are set for accesses of the aliasing regime. */
  private static final class PendingAccess {
    private final @Nullable MemoryLocation memoryLocation;
    private final String cssaName;
    private final boolean write;
    private final CType type;
    private @Nullable String regionId;
    private @Nullable String addressName;
    private @Nullable CType addressType;
    // byte offset within the object as a C expression, or null for a plain zero offset
    private @Nullable CExpression offsetExpr;

    private PendingAccess(
        @Nullable MemoryLocation pMemoryLocation, String pCssaName, boolean pWrite, CType pType) {
      memoryLocation = pMemoryLocation;
      cssaName = pCssaName;
      write = pWrite;
      type = pType;
    }
  }

  private static void addOffset(PendingAccess pAccess, CExpression pTerm) {
    pAccess.offsetExpr = pAccess.offsetExpr == null ? pTerm : plus(pAccess.offsetExpr, pTerm);
  }

  /**
   * Builds the byte-offset term of a region access as one integer formula in the solver's native
   * word type, by evaluating the accumulated offset expression (zero if none) through the
   * path-formula converter so its type matches the index values.
   */
  private Formula offsetTermOf(PendingAccess pAccess, PathFormula pResolution, CFAEdge pEdge)
      throws CPATransferException, InterruptedException {
    if (pAccess.offsetExpr == null) {
      // offset-free accesses share one constant zero term instead of minting a symbol each
      if (zeroOffsetTerm == null) {
        zeroOffsetTerm = evaluateInt(longLiteral(0), pResolution, pEdge);
      }
      return zeroOffsetTerm;
    }
    return evaluateInt(pAccess.offsetExpr, pResolution, pEdge);
  }

  private static CExpression longLiteral(long pValue) {
    return new CIntegerLiteralExpression(
        FileLocation.DUMMY, CNumericTypes.LONG_INT, BigInteger.valueOf(pValue));
  }

  private static CExpression plus(CExpression pLeft, CExpression pRight) {
    return new CBinaryExpression(
        FileLocation.DUMMY,
        CNumericTypes.LONG_INT,
        CNumericTypes.LONG_INT,
        pLeft,
        pRight,
        CBinaryExpression.BinaryOperator.PLUS);
  }

  private static CExpression scaled(CExpression pIndex, long pScale) {
    return new CBinaryExpression(
        FileLocation.DUMMY,
        CNumericTypes.LONG_INT,
        CNumericTypes.LONG_INT,
        pIndex,
        longLiteral(pScale),
        CBinaryExpression.BinaryOperator.MULTIPLY);
  }

  /** The address constant of one address-taken variable or heap allocation. */
  private record PendingBase(String name, CType type) {}

  private static String baseNameFor(String pQualifiedName) {
    return "__oc_base_" + pQualifiedName;
  }

  private static String regionOf(CType pType) {
    return pType.getCanonicalType().toString();
  }

  private static CIdExpression syntheticIdExpression(String pName, CType pType) {
    CVariableDeclaration declaration =
        new CVariableDeclaration(
            FileLocation.DUMMY, true, CStorageClass.AUTO, pType, pName, pName, pName, null);
    return new CIdExpression(FileLocation.DUMMY, pType, pName, declaration);
  }
}
