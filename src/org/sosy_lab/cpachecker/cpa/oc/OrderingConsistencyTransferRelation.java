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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
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
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
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

  private final OrderingConsistencyCPA cpa;
  private final ShutdownNotifier shutdownNotifier;
  private final OcExplorationRegistry registry;
  private final PathFormulaManager pathFormulaManager;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final Set<String> registeredBases = new HashSet<>();

  OrderingConsistencyTransferRelation(
      OrderingConsistencyCPA pCpa, ShutdownNotifier pShutdownNotifier) {
    cpa = pCpa;
    shutdownNotifier = pShutdownNotifier;
    registry = pCpa.getRegistry();
    pathFormulaManager = pCpa.getPathFormulaManager();
    fmgr = pCpa.getSolver().getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
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

    CFANode node = state.getLocationState().getLocationNode();
    if (node instanceof FunctionExitNode && state.getCallstackState().getDepth() == 1) {
      // the thread's start routine returns: normal termination, the only joinable kind of path
      addThreadExitEvent(state);
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
      addThreadExitEvent(pState); // the path ends here, the thread terminates normally
      return;
    }
    if (callee != null && PROGRAM_EXIT_FUNCTIONS.contains(callee)) {
      // the whole program dies here; the event blocks any pthread_join of this instance
      registry.addEvent(
          pState.getInstanceId(),
          EventKind.ABORT,
          pState.getLastEventId(),
          pState.getGuard(),
          null,
          null,
          null,
          null,
          MemoryEvent.NO_INSTANCE);
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
    if (handleMutex(pState, pEdge, pSuccessors)) {
      return;
    }
    handleRegularEdge(pState, pPrecision, pEdge, pSuccessors);
  }

  private void addThreadExitEvent(OrderingConsistencyState pState) {
    registry.addEvent(
        pState.getInstanceId(),
        EventKind.THREAD_EXIT,
        pState.getLastEventId(),
        pState.getGuard(),
        null,
        null,
        null,
        null,
        MemoryEvent.NO_INSTANCE);
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
        registry.addEvent(
            pState.getInstanceId(),
            EventKind.ERROR,
            pState.getLastEventId(),
            pState.getGuard(),
            null,
            null,
            null,
            null,
            MemoryEvent.NO_INSTANCE);
    pSuccessors.add(
        new OrderingConsistencyState(
            pState.getInstanceId(),
            locationAt(pEdge.getSuccessor()),
            pState.getCallstackState(),
            pState.getPathFormula(),
            pState.getGuard(),
            event.id(),
            pState.getCreateCounts(),
            pState.getThreadHandles(),
            pState.getLoopCounts(),
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
        registry.addEvent(
            pState.getInstanceId(),
            EventKind.CREATE,
            pState.getLastEventId(),
            pState.getGuard(),
            null,
            null,
            null,
            null,
            instance.getId());
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
        event.id(),
        withEntry(pState.getCreateCounts(), function, ordinal + 1),
        withEntry(pState.getThreadHandles(), handle, instance.getId()));

    if (isNew) {
      pSuccessors.add(
          new OrderingConsistencyState(
              instance.getId(),
              locationAt(entry),
              (CallstackState)
                  cpa.getCallstackCPA()
                      .getInitialState(entry, StateSpacePartition.getDefaultPartition()),
              rootFormula,
              bfmgr.makeTrue(),
              MemoryEvent.NO_EVENT,
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
        registry.addEvent(
            pState.getInstanceId(),
            EventKind.JOIN,
            pState.getLastEventId(),
            pState.getGuard(),
            null,
            null,
            null,
            null,
            joined);
    addSuccessor(
        pSuccessors,
        pState,
        pEdge.getSuccessor(),
        pState.getCallstackState(),
        pState.getPathFormula(),
        pState.getGuard(),
        event.id(),
        pState.getCreateCounts(),
        pState.getThreadHandles());
  }

  /** Handles mutex and atomic-block edges; returns false if the edge is none of those. */
  private boolean handleMutex(
      OrderingConsistencyState pState, CFAEdge pEdge, List<AbstractState> pSuccessors)
      throws UnsupportedCodeException {
    EventKind kind;
    String mutexId;
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
        mutexId = lock.handle();
      } else if (unlock != null) {
        kind = EventKind.UNLOCK;
        mutexId = unlock.handle();
      } else {
        kind = null;
        mutexId = null;
      }
      if (kind != null && mutexId == null) {
        throw new UnsupportedCodeException("mutex identity not recognized", pEdge);
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
              pState.getLastEventId(),
              pState.getCreateCounts(),
              pState.getThreadHandles());
          return true;
        }
        return false;
      }
    }
    MemoryEvent event =
        registry.addEvent(
            pState.getInstanceId(),
            kind,
            pState.getLastEventId(),
            pState.getGuard(),
            null,
            null,
            null,
            mutexId,
            MemoryEvent.NO_INSTANCE);
    addSuccessor(
        pSuccessors,
        pState,
        pEdge.getSuccessor(),
        pState.getCallstackState(),
        pState.getPathFormula(),
        pState.getGuard(),
        event.id(),
        pState.getCreateCounts(),
        pState.getThreadHandles());
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
    if (!bfmgr.isTrue(formula)) {
      registry.addPathConstraint(bfmgr.implication(pState.getGuard(), formula));
    }
    int lastEventId = chainAccessEvents(pState, renamer, edgeFormula, pEdge);
    lastEventId = handleMallocIfAny(pState, pEdge, renamer, edgeFormula, lastEventId);

    addSuccessor(
        pSuccessors,
        pState,
        pEdge.getSuccessor(),
        nextCallstack,
        edgeFormula,
        pState.getGuard(),
        lastEventId,
        pState.getCreateCounts(),
        pState.getThreadHandles());
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
    int sharedLastEventId = chainAccessEvents(pState, renamer, firstFormula, pFirst);
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
        sharedLastEventId,
        pState.getCreateCounts(),
        pState.getThreadHandles());
    addSuccessor(
        pSuccessors,
        pState,
        pSecond.getSuccessor(),
        pState.getCallstackState(),
        secondFormula,
        bfmgr.and(pState.getGuard(), secondFormula.getFormula()),
        sharedLastEventId,
        pState.getCreateCounts(),
        pState.getThreadHandles());
  }

  /**
   * Registers the accesses collected while rewriting one edge, reads before writes (matching
   * evaluation order), chained in program order after the state's last event. Returns the new last
   * event id.
   */
  private int chainAccessEvents(
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

    int lastEventId = pState.getLastEventId();
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
                addressTerm);
        lastEventId = event.id();
      }
    }
    return lastEventId;
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

  /** Binds the freshly written pointer of a {@code lhs = malloc(...)} to a distinct heap base. */
  private int handleMallocIfAny(
      OrderingConsistencyState pState,
      CFAEdge pEdge,
      CollectingRenamer pRenamer,
      PathFormula pEdgeFormula,
      int pLastEventId)
      throws CPATransferException, InterruptedException {
    if (!(pEdge instanceof AStatementEdge statementEdge)
        || !(statementEdge.getStatement() instanceof CFunctionCallAssignmentStatement call)
        || !(call.getFunctionCallExpression().getFunctionNameExpression()
            instanceof CIdExpression function)
        || !function.getName().equals("malloc")) {
      return pLastEventId;
    }
    if (!(call.getLeftHandSide() instanceof CIdExpression lhs)
        || !(lhs.getExpressionType().getCanonicalType() instanceof CPointerType pointerType)) {
      throw new UnsupportedCodeException("unsupported form of malloc", pEdge);
    }
    CType pointee = pointerType.getType();
    CType canonicalPointee = pointee.getCanonicalType();
    if (!(canonicalPointee instanceof CSimpleType || canonicalPointee instanceof CPointerType)) {
      throw new UnsupportedCodeException("malloc of a non-scalar cell", pEdge);
    }

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
    String initName = registry.freshCssaName("__oc_heapinit");
    resolution = indexSymbol(resolution, baseName, lhsType, pEdge);
    resolution = indexSymbol(resolution, initName, pointee, pEdge);

    Formula lhsTerm = resolve(resolution, lhsName, lhsType, pEdge);
    Formula baseTerm = resolve(resolution, baseName, lhsType, pEdge);
    registry.addAddressBase(baseTerm);
    registry.addPathConstraint(
        bfmgr.implication(pState.getGuard(), fmgr.makeEqual(lhsTerm, baseTerm)));

    // the allocated cell starts with an unconstrained value
    MemoryEvent initialWrite =
        registry.addEvent(
            pState.getInstanceId(),
            EventKind.WRITE,
            pLastEventId,
            pState.getGuard(),
            null,
            initName,
            resolve(resolution, initName, pointee, pEdge),
            null,
            MemoryEvent.NO_INSTANCE,
            regionOf(pointee),
            baseTerm);
    return initialWrite.id();
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
      int pLastEventId,
      ImmutableMap<String, Integer> pCreateCounts,
      ImmutableMap<String, Integer> pThreadHandles) {
    ImmutableMap<CFANode, Integer> loopCounts = pState.getLoopCounts();
    if (pNextNode.isLoopStart()) {
      int count = loopCounts.getOrDefault(pNextNode, 0) + 1;
      if (count > cpa.getMaxLoopIterations()) {
        registry.markTruncated();
        // the thread has not terminated on this path, so it can never be joined here
        registry.addEvent(
            pState.getInstanceId(),
            EventKind.TRUNCATED,
            pLastEventId,
            pGuard,
            null,
            null,
            null,
            null,
            MemoryEvent.NO_INSTANCE);
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
            pLastEventId,
            pCreateCounts,
            pThreadHandles,
            loopCounts,
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

    @Override
    public String freshName(CVariableDeclaration pDeclaration, boolean pIsWrite) {
      String qualifiedName = pDeclaration.getQualifiedName();
      String cssaName = registry.freshCssaName(qualifiedName);
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
    public CIdExpression replaceAliasedAccess(CExpression pClonedAccess, boolean pIsWrite) {
      CIdExpression pointer;
      if (pClonedAccess instanceof CPointerExpression deref
          && stripCasts(deref.getOperand()) instanceof CIdExpression id) {
        pointer = id;
      } else if (pClonedAccess instanceof CArraySubscriptExpression subscript
          && stripCasts(subscript.getArrayExpression()) instanceof CIdExpression id
          && subscript.getSubscriptExpression() instanceof CIntegerLiteralExpression literal
          && literal.getValue().signum() == 0) {
        pointer = id;
      } else {
        throw new UnsupportedAccessException(
            "unsupported dereferencing access: " + pClonedAccess.toASTString());
      }
      if (!(pointer.getExpressionType().getCanonicalType() instanceof CPointerType)) {
        throw new UnsupportedAccessException(
            "dereference of a non-pointer: " + pClonedAccess.toASTString());
      }
      CType valueType = pClonedAccess.getExpressionType();
      CType canonicalValueType = valueType.getCanonicalType();
      if (!(canonicalValueType instanceof CSimpleType
          || canonicalValueType instanceof CPointerType)) {
        throw new UnsupportedAccessException(
            "non-scalar dereference: " + pClonedAccess.toASTString());
      }

      String pointerName = pointer.getDeclaration().getQualifiedName();
      String tempName = registry.freshCssaName("__oc_mem");
      PendingAccess access = new PendingAccess(null, tempName, pIsWrite, valueType);
      access.regionId = regionOf(valueType);
      access.addressName = pointerName;
      access.addressType = pointer.getExpressionType();
      accesses.add(access);
      return syntheticIdExpression(tempName, valueType);
    }
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

    private PendingAccess(
        @Nullable MemoryLocation pMemoryLocation, String pCssaName, boolean pWrite, CType pType) {
      memoryLocation = pMemoryLocation;
      cssaName = pCssaName;
      write = pWrite;
      type = pType;
    }
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
