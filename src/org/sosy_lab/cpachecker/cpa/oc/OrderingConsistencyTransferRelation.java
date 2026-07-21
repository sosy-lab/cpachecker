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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
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
import org.sosy_lab.cpachecker.cpa.oc.ThreadInstance.InstanceKey;
import org.sosy_lab.cpachecker.cpa.por.GlobalAccessRenamer;
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

  /**
   * External library functions that write to memory through a pointer argument. The generic
   * path-formula converter has no body for these (they are undeclared/external) and assumes unknown
   * external calls are pure, so a write through such a pointer is otherwise silently dropped: a
   * race on the written memory would go undetected rather than reported (e.g. {@code scanf("%d",
   * &global)} racing with a concurrent access to {@code global}). Rejecting calls that target a
   * global is preferred over silently mis-verifying the race property as safe.
   */
  private static final ImmutableSet<String> WRITE_THROUGH_POINTER_FUNCTIONS =
      ImmutableSet.of(
          "scanf", "fscanf", "sscanf", "vscanf", "vfscanf", "vsscanf", "gets", "fgets", "getline");

  private final OrderingConsistencyCPA cpa;
  private final ShutdownNotifier shutdownNotifier;
  private final PathFormulaManager pathFormulaManager;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final MachineModel machineModel;

  /**
   * The program's {@code __thread} variables, scanned once: every spawned instance's private copy
   * of each of them has to be seeded in its root context (see {@link #initializeThreadLocals}).
   */
  private final ImmutableList<CVariableDeclaration> threadLocalGlobals;

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
    threadLocalGlobals = ThreadFunctions.threadLocalGlobals(pCpa.getCfa());
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
            predecessors.isEmpty() ? MemoryEvent.NO_EVENT : predecessors.getFirst(),
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
            0,
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
        && leavingEdges.getFirst() instanceof CAssumeEdge first
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
          pState,
          EventKind.ABORT,
          null,
          null,
          null,
          null,
          MemoryEvent.NO_INSTANCE,
          null,
          null,
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
        pSuccessors.set(i, withAtomicSection(successor, ImmutableList.of(unlockEvent.id()), -1));
      }
      return;
    }
    if (callee != null
        && WRITE_THROUGH_POINTER_FUNCTIONS.contains(callee)
        && callsWithAddressOfGlobal(pEdge)) {
      throw new UnsupportedCodeException(
          callee
              + " writes to a global through a pointer argument, which the data-race analysis"
              + " does not model",
          pEdge);
    }
    if (handleTrylock(pState, pEdge, pSuccessors)) {
      return;
    }
    if (handleMutex(pState, pEdge, pSuccessors)) {
      return;
    }
    handleRegularEdge(pState, pPrecision, pEdge, pSuccessors);
  }

  /** Whether the call on this edge takes the address of a global variable as an argument. */
  private static boolean callsWithAddressOfGlobal(CFAEdge pEdge) throws CPATransferException {
    for (AExpression param : getCallParameters(pEdge)) {
      CExpression argument = stripCasts((CExpression) param);
      if (argument instanceof CUnaryExpression unary
          && unary.getOperator() == CUnaryExpression.UnaryOperator.AMPER
          && unary.getOperand() instanceof CIdExpression id
          && id.getDeclaration() instanceof CVariableDeclaration decl
          && decl.isGlobal()) {
        return true;
      }
    }
    return false;
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
        pState.getLiveInstanceIds(),
        pState.getLoopCounts(),
        withLockDepth(pState.getLockDepths(), MemoryEvent.ATOMIC_BLOCK_MUTEX, pDepthDelta),
        pState.isTarget());
  }

  private void addThreadExitEvent(OrderingConsistencyState pState, @Nullable CFAEdge pEdge) {
    addEventAfter(
        pState,
        EventKind.THREAD_EXIT,
        null,
        null,
        null,
        null,
        MemoryEvent.NO_INSTANCE,
        null,
        null,
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
            pState,
            EventKind.ERROR,
            null,
            null,
            null,
            null,
            MemoryEvent.NO_INSTANCE,
            null,
            null,
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
            pState.getLiveInstanceIds(),
            pState.getLoopCounts(),
            pState.getLockDepths(),
            true));
  }

  private void handleCreate(
      OrderingConsistencyState pState, CFAEdge pEdge, List<AbstractState> pSuccessors)
      throws CPATransferException, InterruptedException {
    List<? extends AExpression> params = getCallParameters(pEdge);
    ThreadFunctions.checkCreateParams(params);
    String function = ThreadFunctions.extractCreateFunctionName(params);
    int ordinal = pState.getCreateCounts().getOrDefault(function, 0);

    // the thread argument must be modeled, not ignored: a null constant carries no data, the
    // address of an object (global, or a local of the creating thread that thereby escapes to the
    // new one) is bound to that object's allocation base, anything else is unsupported
    CVariableDeclaration argTarget = null;
    CExpression argument = stripCasts((CExpression) params.get(3));
    if (argument instanceof CUnaryExpression unary
        && unary.getOperator() == CUnaryExpression.UnaryOperator.AMPER
        && unary.getOperand() instanceof CIdExpression id
        && id.getDeclaration() instanceof CVariableDeclaration decl) {
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
    ThreadInstance instance = existing != null ? existing : registry.newInstance(key);

    MemoryEvent createEvent =
        addEventAfter(
            pState, EventKind.CREATE, null, null, null, null, instance.getId(), null, null, pEdge);
    registry.addCreateEvent(instance.getId(), createEvent.id());

    // a fresh literal identifying this instance is written through the (arbitrary) handle pointer
    // via the normal aliased-write pipeline, so e.g. &t[i] is handled exactly like any other array
    // write (symbolic index included); pthread_join later recovers the instance by reading this
    // location back and branching over candidates (see handleJoin), not by resolving a name
    OrderingConsistencyState afterCreate =
        withGuardAndEvents(pState, pState.getGuard(), ImmutableList.of(createEvent.id()));
    ChainedEdge handleWrite =
        writeThreadHandle(afterCreate, (CExpression) params.get(0), instance.getId(), pEdge);

    PathFormula rootFormula = pathFormulaManager.makeEmptyPathFormula();
    if (argTarget != null && !entry.getFunctionParameters().isEmpty()) {
      rootFormula = bindThreadArgument(pState, pEdge, instance, entry, argTarget);
    }
    rootFormula = initializeThreadLocals(pState, pEdge, instance, rootFormula);

    addSuccessor(
        pSuccessors,
        pState,
        pEdge.getSuccessor(),
        pState.getCallstackState(),
        handleWrite.edgeFormula(),
        pState.getGuard(),
        handleWrite.lastEventIds(),
        withEntry(pState.getCreateCounts(), function, ordinal + 1),
        withAdded(pState.getLiveInstanceIds(), instance.getId()),
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
              ImmutableSet.of(),
              ImmutableMap.of(),
              ImmutableMap.of(),
              false));
    }
  }

  /**
   * Constrains the created thread's parameter to the address of the object whose address is passed.
   * The parameter symbol is indexed in the returned root context, so the thread's own reads use the
   * same SSA index as the binding (built identically for every create site of the instance). When
   * the object is a local of the creating thread, its base is the creator's per-instance base (see
   * {@link #baseNameFor(CVariableDeclaration, int)}) — {@code pState.getInstanceId()} — so the new
   * thread's reads through the parameter alias the creator's own accesses to that escaped local.
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
    String baseName = baseNameFor(pArgTarget, pState.getInstanceId());
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
      registry.addAddressBase(baseTerm, objectSizeBytes(pArgTarget.getType()));
    }
    registry.addPathConstraint(
        bfmgr.implication(pState.getGuard(), fmgr.makeEqual(argTerm, baseTerm)));
    return rootFormula;
  }

  /**
   * Seeds the created instance's private copy of every {@code __thread} variable with its initial
   * value, in the instance's own root context, and returns the extended context.
   *
   * <p>A {@code __thread} variable is privatized to {@code T{instance}_x} exactly like a local (see
   * {@code org.sosy_lab.cpachecker.cpa.por.PorAstCloner}), which is what stops it from being read
   * as shared state — but a spawned instance explores from its start routine's entry and so never
   * folds in the file-scope declaration edge that carries the initializer; only the main instance
   * does. Without this the copy would be an unconstrained symbol, i.e. an arbitrary value, and an
   * {@code assert(x == 0)} would report a violation the program cannot exhibit.
   *
   * <p>Like {@link #bindThreadArgument}, the constraint is guarded by the <em>creator's</em> guard
   * (the instance only exists on paths that reach the create) and indexed in the root context, so
   * the instance's own first read resolves the same SSA index this write assigns.
   *
   * @throws UnsupportedCodeException if the address of a {@code __thread} variable is taken
   *     anywhere: it then lives in the aliasing regime as a memory region keyed on its declaration
   *     — one region shared by all instances, and not seedable by the single value assignment made
   *     here — so both its per-instance identity and this initialization would be wrong
   */
  private PathFormula initializeThreadLocals(
      OrderingConsistencyState pState,
      CFAEdge pEdge,
      ThreadInstance pInstance,
      PathFormula pRootFormula)
      throws CPATransferException, InterruptedException {
    PathFormula rootFormula = pRootFormula;
    for (CVariableDeclaration threadLocal : threadLocalGlobals) {
      String qualifiedName = threadLocal.getQualifiedName();
      if (cpa.getAddressedVariables().contains(qualifiedName)) {
        throw new UnsupportedCodeException(
            "address of thread-local variable " + qualifiedName + " is taken", pEdge);
      }
      CExpression value = ThreadFunctions.threadLocalInitValue(threadLocal, machineModel, pEdge);
      CType type = threadLocal.getType();
      CIdExpression copy =
          syntheticIdExpression(
              ThreadFunctions.perThreadName(pInstance.getId(), qualifiedName), type);
      // an assume rather than a statement: it indexes the copy in the root context and yields the
      // equality as its formula in one step, exactly like indexSymbol's identity assumption does
      CBinaryExpression initialized =
          new CBinaryExpression(
              FileLocation.DUMMY,
              CNumericTypes.INT,
              type,
              copy,
              value,
              CBinaryExpression.BinaryOperator.EQUALS);
      CAssumeEdge initEdge =
          new CAssumeEdge(
              "",
              FileLocation.DUMMY,
              pEdge.getPredecessor(),
              pEdge.getSuccessor(),
              initialized,
              true);
      rootFormula =
          pathFormulaManager.makeAnd(
              pathFormulaManager.makeEmptyPathFormulaWithContextFrom(rootFormula), initEdge);
      registry.addPathConstraint(bfmgr.implication(pState.getGuard(), rootFormula.getFormula()));
    }
    return rootFormula;
  }

  private static CExpression stripCasts(CExpression pExpression) {
    CExpression result = pExpression;
    while (result instanceof CCastExpression cast) {
      result = cast.getOperand();
    }
    return result;
  }

  /**
   * A pthread_join's handle expression is not resolved to a single instance statically: it is
   * branched over every thread instance created so far on this path as a candidate, mirroring how
   * {@link #handleAssumePair} branches on the two arms of an {@code if} — each candidate gets its
   * own synthetic {@code handle == candidate's id} assume edge, cloned and folded independently
   * (like handleAssumePair's two arms, and unlike {@link #cloneAndChain}'s callers, the equality
   * must not become an unconditional path constraint: it belongs in *this branch's own* guard, not
   * asserted for every branch at once). Since every instance's id is a distinct literal, a
   * candidate whose handle cannot really equal it is automatically unsatisfiable without any
   * bespoke alias-checking code here — the existing SSA/converter machinery answers that question
   * the same way it would for a real {@code if (handle == candidate) ...}. Passing the handle
   * expression itself (not a pre-resolved value) also sidesteps the fact that a local, non-global
   * handle variable (the overwhelmingly common case, e.g. {@code pthread_t t;}) never becomes an
   * aliased {@link MemoryEvent} in the first place — {@link CollectingRenamer} only tracks
   * globals/aliasable memory, and plain per-instance SSA already threads a local's value correctly
   * within one instance's own path.
   */
  private void handleJoin(
      OrderingConsistencyState pState, CFAEdge pEdge, List<AbstractState> pSuccessors)
      throws CPATransferException, InterruptedException {
    List<? extends AExpression> params = getCallParameters(pEdge);
    ThreadFunctions.checkJoinParams(params);
    CExpression handle = (CExpression) params.get(0);

    for (int candidate : pState.getLiveInstanceIds()) {
      CIntegerLiteralExpression literal =
          new CIntegerLiteralExpression(
              FileLocation.DUMMY,
              handle.getExpressionType(),
              BigInteger.valueOf((long) candidate + 1));
      CBinaryExpression equalsCandidate =
          new CBinaryExpression(
              FileLocation.DUMMY,
              CNumericTypes.INT,
              handle.getExpressionType(),
              handle,
              literal,
              CBinaryExpression.BinaryOperator.EQUALS);
      CAssumeEdge assumeEdge =
          new CAssumeEdge(
              "",
              FileLocation.DUMMY,
              pEdge.getPredecessor(),
              pEdge.getSuccessor(),
              equalsCandidate,
              true);

      CollectingRenamer renamer = new CollectingRenamer(pState.getInstanceId());
      CAssumeEdge rewritten;
      try {
        rewritten =
            (CAssumeEdge)
                PorEdgeCloner.cloneSingleEdge(assumeEdge, pState.getInstanceId(), renamer);
      } catch (GlobalAccessRenamer.UnsupportedAccessException e) {
        throw new UnsupportedCodeException(e.getMessage(), pEdge);
      }
      PathFormula assumeFormula =
          pathFormulaManager.makeAnd(
              pathFormulaManager.makeEmptyPathFormulaWithContextFrom(pState.getPathFormula()),
              rewritten);
      int firstEventId = registry.nextEventId();
      ImmutableList<Integer> lastEventIds =
          chainAccessEvents(pState, renamer, assumeFormula, pEdge);
      for (int id = firstEventId; id < registry.nextEventId(); id++) {
        registry.markThreadHandleAccess(id);
      }

      BooleanFormula branchGuard = bfmgr.and(pState.getGuard(), assumeFormula.getFormula());
      OrderingConsistencyState branchState = withGuardAndEvents(pState, branchGuard, lastEventIds);
      MemoryEvent joinEvent =
          addEventAfter(
              branchState, EventKind.JOIN, null, null, null, null, candidate, null, null, pEdge);
      addSuccessor(
          pSuccessors,
          pState,
          pEdge.getSuccessor(),
          pState.getCallstackState(),
          assumeFormula,
          branchGuard,
          ImmutableList.of(joinEvent.id()),
          pState.getCreateCounts(),
          pState.getLiveInstanceIds(),
          pState.getLockDepths());
    }
  }

  /** The pointee type of an lvalue expected to be a pointer (a pthread_create/join handle). */
  private static CType threadHandlePointeeType(CExpression pHandle, CFAEdge pEdge)
      throws UnsupportedCodeException {
    CType type = pHandle.getExpressionType().getCanonicalType();
    if (type instanceof CPointerType pointerType) {
      return pointerType.getType();
    }
    throw new UnsupportedCodeException("thread handle is not a pointer expression", pEdge);
  }

  /**
   * The lvalue a pthread_create/join handle expression actually designates. When the handle is
   * syntactically {@code &lvalue} (by far the common case, e.g. {@code &t} or {@code &t[i]}), that
   * inner lvalue *is* the target — dereferencing it again ({@code *(&lvalue)}) is a pattern the C
   * frontend never itself produces (it already folds {@code *&x} to {@code x}), so the
   * general-purpose lvalue analysis used by {@link #chainAccessEvents} does not recognize it.
   * Otherwise the handle is already pointer-typed (e.g. a {@code pthread_t*} parameter), and the
   * lvalue is the ordinary dereference {@code *handle}.
   */
  private static CLeftHandSide threadHandleLvalue(CExpression pHandle, CFAEdge pEdge)
      throws UnsupportedCodeException {
    if (pHandle instanceof CUnaryExpression unary
        && unary.getOperator() == CUnaryExpression.UnaryOperator.AMPER
        && unary.getOperand() instanceof CLeftHandSide lvalue) {
      return lvalue;
    }
    return new CPointerExpression(
        FileLocation.DUMMY, threadHandlePointeeType(pHandle, pEdge), pHandle);
  }

  /**
   * Writes a fresh literal identifying {@code pInstanceId} through the (arbitrary) pointer
   * expression that names the new thread's handle, via a synthetic {@code *handle = id;} edge run
   * through the normal aliased-write pipeline ({@link #cloneAndChain}) — this is what makes e.g.
   * {@code &t[i]} work: the existing symbolic-offset machinery in {@link CollectingRenamer}/{@link
   * #chainAccessEvents} already handles array/struct lvalues with symbolic indices, nothing new is
   * needed here. If the handle is a plain local variable (the common case), no event is created at
   * all (locals never enter the aliasing regime — see {@link #handleJoin}'s note) and the write's
   * effect lives purely in the returned edge formula's updated SSA, exactly like an ordinary local
   * assignment; if it does create a (global/aliased) event, that event is tagged via {@link
   * OcExplorationRegistry#markThreadHandleAccess} so it is excluded from data-race candidates.
   */
  private ChainedEdge writeThreadHandle(
      OrderingConsistencyState pState, CExpression pHandle, int pInstanceId, CFAEdge pEdge)
      throws CPATransferException, InterruptedException {
    CLeftHandSide lhs = threadHandleLvalue(pHandle, pEdge);
    CIntegerLiteralExpression rhs =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY,
            lhs.getExpressionType(),
            BigInteger.valueOf((long) pInstanceId + 1));
    CStatementEdge writeEdge =
        new CStatementEdge(
            "",
            new CExpressionAssignmentStatement(FileLocation.DUMMY, lhs, rhs),
            FileLocation.DUMMY,
            pEdge.getPredecessor(),
            pEdge.getSuccessor());
    return markAsThreadHandleAccess(pState, writeEdge);
  }

  /**
   * Runs {@link #cloneAndChain} and tags every event it produced as thread-handle bookkeeping (see
   * {@link OcExplorationRegistry#markThreadHandleAccess}), so callers of {@link #writeThreadHandle}
   * don't have to.
   */
  private ChainedEdge markAsThreadHandleAccess(
      OrderingConsistencyState pState, CFAEdge pSyntheticEdge)
      throws CPATransferException, InterruptedException {
    int firstEventId = registry.nextEventId();
    ChainedEdge chained = cloneAndChain(pState, pSyntheticEdge);
    int lastEventId =
        chained.lastEventIds().isEmpty() ? firstEventId - 1 : chained.lastEventIds().getFirst();
    for (int id = firstEventId; id <= lastEventId; id++) {
      registry.markThreadHandleAccess(id);
    }
    return chained;
  }

  /**
   * A path formula and the chained READ/WRITE events produced by cloning one (possibly synthetic)
   * CFA edge; see {@link #cloneAndChain}.
   */
  private record ChainedEdge(PathFormula edgeFormula, ImmutableList<Integer> lastEventIds) {}

  private ChainedEdge cloneAndChain(OrderingConsistencyState pState, CFAEdge pEdge)
      throws CPATransferException, InterruptedException {
    return cloneAndChain(pState, new CollectingRenamer(pState.getInstanceId()), pEdge);
  }

  /**
   * Clones {@code pEdge} into {@code pState}'s instance (renaming locals, recording global/pointer
   * accesses into {@code pRenamer}), folds it into the path formula, asserts the resulting
   * constraint (instance-prefixing converter-minted fresh symbols so they stay distinct across
   * instances — see {@link #handleRegularEdge}), and turns the recorded accesses into chained
   * READ/WRITE events.
   */
  private ChainedEdge cloneAndChain(
      OrderingConsistencyState pState, CollectingRenamer pRenamer, CFAEdge pEdge)
      throws CPATransferException, InterruptedException {
    PathFormula edgeFormula;
    try {
      CFAEdge rewritten = PorEdgeCloner.cloneSingleEdge(pEdge, pState.getInstanceId(), pRenamer);
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
            name -> name.indexOf('!') >= 0 ? "T" + pState.getInstanceId() + "_" + name : name);
    if (!bfmgr.isTrue(formula)) {
      registry.addPathConstraint(bfmgr.implication(pState.getGuard(), formula));
    }
    ImmutableList<Integer> lastEventIds = chainAccessEvents(pState, pRenamer, edgeFormula, pEdge);
    return new ChainedEdge(edgeFormula, lastEventIds);
  }

  /**
   * A copy of {@code pState} with a different guard and last-event chain, for building one
   * candidate branch's temporary context (see {@link #handleJoin}) or advancing past a freshly
   * added event (see {@link #handleCreate}) before delegating to {@link #cloneAndChain}.
   */
  private static OrderingConsistencyState withGuardAndEvents(
      OrderingConsistencyState pState,
      BooleanFormula pGuard,
      ImmutableList<Integer> pLastEventIds) {
    return new OrderingConsistencyState(
        pState.getInstanceId(),
        pState.getLocationState(),
        pState.getCallstackState(),
        pState.getPathFormula(),
        pGuard,
        pLastEventIds,
        pState.getCreateCounts(),
        pState.getLiveInstanceIds(),
        pState.getLoopCounts(),
        pState.getLockDepths(),
        false);
  }

  private static ImmutableSet<Integer> withAdded(ImmutableSet<Integer> pSet, int pValue) {
    return ImmutableSet.<Integer>builder().addAll(pSet).add(pValue).build();
  }

  /** Handles mutex and atomic-block edges; returns false if the edge is none of those. */
  private boolean handleMutex(
      OrderingConsistencyState pState, CFAEdge pEdge, List<AbstractState> pSuccessors)
      throws CPATransferException, InterruptedException {
    EventKind kind;
    // nesting key (pairs a lock with its unlock within one thread); cross-thread identity is by
    // address. An atomic block has no address and its own pseudo-mutex id.
    String mutexId;
    boolean readLock = false;
    boolean ambiguousUnlock = false;
    @Nullable String regionId = null;
    @Nullable Formula addressTerm = null;
    ImmutableList<Integer> lastEventIds = pState.getLastEventIds();
    if (MutexFunctions.isAtomicBeginCall(pEdge)) {
      kind = EventKind.LOCK;
      mutexId = MemoryEvent.ATOMIC_BLOCK_MUTEX;
    } else if (MutexFunctions.isAtomicEndCall(pEdge)) {
      kind = EventKind.UNLOCK;
      mutexId = MemoryEvent.ATOMIC_BLOCK_MUTEX;
    } else {
      String callee = MutexFunctions.getFunctionCallName(pEdge);
      if (callee != null && MutexFunctions.isLockFunction(callee)) {
        kind = EventKind.LOCK;
        readLock = MutexFunctions.isReadLockFunction(callee);
      } else if (callee != null && MutexFunctions.isUnlockFunction(callee)) {
        kind = EventKind.UNLOCK;
      } else {
        // a mutex init/destroy is a no-op for ordering; any other call is not ours
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
              pState.getLiveInstanceIds(),
              pState.getLockDepths());
          return true;
        }
        return false;
      }
      // resolve the mutex by its flat address, exactly like ordinary memory: two lock/unlock
      // operations act on the same mutex iff their addresses coincide, so `&(p->lock)`, `&arr[i]`
      // and aliased mutexes are all handled without a bespoke name-resolution step
      List<? extends AExpression> arguments = getCallParameters(pEdge);
      if (arguments.isEmpty() || !(arguments.get(0) instanceof CExpression argument)) {
        throw new UnsupportedCodeException("mutex call without a mutex argument", pEdge);
      }
      MutexAddress mutexAddress = evaluateMutexAddress(pState, argument, pEdge);
      mutexId = mutexNestingKey(argument);
      // An unlock whose target object is not statically fixed (a bare pointer value or a symbolic
      // index, as opposed to `&globalMutex`) can release a lock held under a different syntactic
      // name; its critical section must then be closed by address, not by the nesting key.
      ambiguousUnlock = kind == EventKind.UNLOCK && isAmbiguousMutexTarget(argument);
      regionId = MemoryEvent.MUTEX_REGION;
      addressTerm = mutexAddress.term();
      lastEventIds = mutexAddress.lastEventIds();
    }
    MemoryEvent event =
        addEventAfter(
            withGuardAndEvents(pState, pState.getGuard(), lastEventIds),
            kind,
            null,
            null,
            null,
            mutexId,
            MemoryEvent.NO_INSTANCE,
            regionId,
            addressTerm,
            pEdge);
    if (readLock) {
      registry.markReadLock(event.id());
    }
    if (ambiguousUnlock) {
      registry.markAmbiguousUnlock(event.id());
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
        pState.getLiveInstanceIds(),
        lockDepths);
    return true;
  }

  /**
   * Models a non-blocking / timed lock-acquisition call (e.g. {@code __CPAchecker_TMP =
   * pthread_mutex_trylock(&m)}) as a two-way branch: a SUCCESS successor that acquires the lock and
   * pins the returned value to 0, and a FAILURE successor that acquires nothing and pins it to
   * non-zero. The assume-pair the front-end emits on that return value (e.g. from {@code while
   * (pthread_mutex_trylock(&m)) {}}) then keeps only the consistent control-flow branch, so the
   * loop correctly exits holding the lock. This is purely additive — it never removes an
   * interleaving — so it cannot turn a real violation into an unsound TRUE; without it the lock is
   * simply never modelled and the guarded region is missed (a spurious data race). Returns false if
   * the edge is not a recognized try/timed-lock call.
   */
  private boolean handleTrylock(
      OrderingConsistencyState pState, CFAEdge pEdge, List<AbstractState> pSuccessors)
      throws CPATransferException, InterruptedException {
    String callee = MutexFunctions.getFunctionCallName(pEdge);
    if (callee == null || !MutexFunctions.isTrylockFunction(callee)) {
      return false;
    }
    List<? extends AExpression> arguments = getCallParameters(pEdge);
    if (arguments.isEmpty() || !(arguments.get(0) instanceof CExpression mutexArg)) {
      throw new UnsupportedCodeException("try-lock call without a mutex argument", pEdge);
    }
    // the assigned result variable (0 == acquired), or null when the result is discarded
    CLeftHandSide retLhs = null;
    if (pEdge instanceof AStatementEdge sEdge
        && sEdge.getStatement() instanceof CFunctionCallAssignmentStatement assign) {
      retLhs = assign.getLeftHandSide();
    }

    // Pin the returned value: 0 on the success branch, non-zero on the failure branch. Both
    // branches
    // share one rewritten condition (like handleAssumePair) so their guards stay mutually exclusive
    // and the reads of the result variable are counted once. A discarded result leaves both
    // branches
    // unconstrained, which is still sound.
    BooleanFormula successCond = bfmgr.makeTrue();
    BooleanFormula failureCond = bfmgr.makeTrue();
    PathFormula successFormula = pState.getPathFormula();
    PathFormula failureFormula = pState.getPathFormula();
    ImmutableList<Integer> sharedLastEventIds = pState.getLastEventIds();
    if (retLhs != null) {
      CBinaryExpression retIsZero =
          new CBinaryExpression(
              FileLocation.DUMMY,
              CNumericTypes.INT,
              CNumericTypes.INT,
              retLhs,
              CIntegerLiteralExpression.ZERO,
              CBinaryExpression.BinaryOperator.EQUALS);
      CAssumeEdge successEdge =
          new CAssumeEdge(
              "",
              FileLocation.DUMMY,
              pEdge.getPredecessor(),
              pEdge.getSuccessor(),
              retIsZero,
              true);
      CollectingRenamer renamer = new CollectingRenamer(pState.getInstanceId());
      CAssumeEdge rewritten;
      try {
        rewritten =
            (CAssumeEdge)
                PorEdgeCloner.cloneSingleEdge(successEdge, pState.getInstanceId(), renamer);
      } catch (GlobalAccessRenamer.UnsupportedAccessException e) {
        throw new UnsupportedCodeException(e.getMessage(), pEdge);
      }
      successFormula =
          pathFormulaManager.makeAnd(
              pathFormulaManager.makeEmptyPathFormulaWithContextFrom(pState.getPathFormula()),
              rewritten);
      sharedLastEventIds = chainAccessEvents(pState, renamer, successFormula, pEdge);
      CAssumeEdge failureEdge =
          new CAssumeEdge(
              rewritten.getRawStatement(),
              FileLocation.DUMMY,
              pEdge.getPredecessor(),
              pEdge.getSuccessor(),
              rewritten.getExpression(),
              false);
      failureFormula =
          pathFormulaManager.makeAnd(
              pathFormulaManager.makeEmptyPathFormulaWithContextFrom(pState.getPathFormula()),
              failureEdge);
      successCond = successFormula.getFormula();
      failureCond = failureFormula.getFormula();
    }

    // SUCCESS: acquire the lock (chained after the result reads and the address bookkeeping).
    BooleanFormula successGuard = bfmgr.and(pState.getGuard(), successCond);
    MutexAddress mutexAddress =
        evaluateMutexAddress(
            withGuardAndEvents(pState, successGuard, sharedLastEventIds), mutexArg, pEdge);
    String mutexId = mutexNestingKey(mutexArg);
    MemoryEvent lockEvent =
        addEventAfter(
            withGuardAndEvents(pState, successGuard, mutexAddress.lastEventIds()),
            EventKind.LOCK,
            null,
            null,
            null,
            mutexId,
            MemoryEvent.NO_INSTANCE,
            MemoryEvent.MUTEX_REGION,
            mutexAddress.term(),
            pEdge);
    if (MutexFunctions.isReadTrylockFunction(callee)) {
      registry.markReadLock(lockEvent.id());
    }
    addSuccessor(
        pSuccessors,
        pState,
        pEdge.getSuccessor(),
        pState.getCallstackState(),
        successFormula,
        successGuard,
        ImmutableList.of(lockEvent.id()),
        pState.getCreateCounts(),
        pState.getLiveInstanceIds(),
        withLockDepth(pState.getLockDepths(), mutexId, 1));

    // FAILURE: nothing is acquired.
    addSuccessor(
        pSuccessors,
        pState,
        pEdge.getSuccessor(),
        pState.getCallstackState(),
        failureFormula,
        bfmgr.and(pState.getGuard(), failureCond),
        sharedLastEventIds,
        pState.getCreateCounts(),
        pState.getLiveInstanceIds(),
        pState.getLockDepths());
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

    CollectingRenamer renamer = new CollectingRenamer(pState.getInstanceId());
    ChainedEdge chained = cloneAndChain(pState, renamer, pEdge);
    PathFormula edgeFormula = chained.edgeFormula();
    ImmutableList<Integer> lastEventIds =
        handleMallocIfAny(pState, pEdge, renamer, edgeFormula, chained.lastEventIds());

    addSuccessor(
        pSuccessors,
        pState,
        pEdge.getSuccessor(),
        nextCallstack,
        edgeFormula,
        pState.getGuard(),
        lastEventIds,
        pState.getCreateCounts(),
        pState.getLiveInstanceIds(),
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
    CollectingRenamer renamer = new CollectingRenamer(pState.getInstanceId());
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
        pState.getLiveInstanceIds(),
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
        pState.getLiveInstanceIds(),
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
        registry.addAddressBase(
            resolve(resolution, base.name(), base.type(), pEdge), pointeeSizeBytes(base.type()));
      }
    }

    ImmutableList<Integer> predecessors = pState.getLastEventIds();
    int lastEventId = predecessors.isEmpty() ? MemoryEvent.NO_EVENT : predecessors.getFirst();
    boolean anyEvent = false;
    int firstNewEventId = registry.nextEventId();

    // On a declaration edge the declared object is not a value use; its initial writes (below)
    // already cover it, so the whole-object access recorded for it in freshName is skipped.
    String declaredObjectName =
        pEdge instanceof CDeclarationEdge decl
                && decl.getDeclaration() instanceof CVariableDeclaration variable
            ? variable.getQualifiedName()
            : null;
    // value symbols of whole-object reads on this edge, keyed by region; a whole-object write of
    // the same region (a copy `s = other;`) is constrained to the source's value (see below)
    Map<String, Formula> aggregateReadValueByRegion = new HashMap<>();

    for (CVariableDeclaration aggregate : pRenamer.aggregateDecls) {
      if (!aggregateInitEmitted.add(aggregate.getQualifiedName())) {
        continue;
      }
      String baseName = baseNameFor(aggregate, pState.getInstanceId());
      CType baseType = new CPointerType(CTypeQualifiers.NONE, aggregate.getType());
      if (!resolution.getSsa().containsVariable(baseName)) {
        resolution = indexSymbol(resolution, baseName, baseType, pEdge);
      }
      Formula baseTerm = resolve(resolution, baseName, baseType, pEdge);
      if (registeredBases.add(baseName)) {
        registry.addAddressBase(baseTerm, objectSizeBytes(aggregate.getType()));
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
                cell.fill() ? objectSizeBytes(aggregate.getType()) : 0,
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
                0,
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
      // Whole-object accesses (e.g. `s = other;`): a struct/array copy touches every cell of the
      // object, so it must be visible as memory events, otherwise a data race through it is missed
      // (and, under reachability, its store is lost). Model it with one fill event per scalar leaf
      // region — each covers the whole object of that region — mirroring the aggregate-init writes.
      for (AggregateAccess aggregate : pRenamer.aggregateAccesses) {
        if (aggregate.write() != writes
            || aggregate.declaration().getQualifiedName().equals(declaredObjectName)) {
          continue;
        }
        CVariableDeclaration decl = aggregate.declaration();
        String baseName = baseNameFor(decl, pState.getInstanceId());
        CType baseType = new CPointerType(CTypeQualifiers.NONE, decl.getType());
        if (!resolution.getSsa().containsVariable(baseName)) {
          resolution = indexSymbol(resolution, baseName, baseType, pEdge);
        }
        Formula baseTerm = resolve(resolution, baseName, baseType, pEdge);
        if (registeredBases.add(baseName)) {
          registry.addAddressBase(baseTerm, objectSizeBytes(decl.getType()));
        }
        long fillSize = objectSizeBytes(decl.getType());
        // a whole-object access of an _Atomic-qualified aggregate is itself atomic
        boolean atomicObject = decl.getType().getCanonicalType().isAtomic();
        for (CType leafType : scalarLeafTypes(decl.getType())) {
          String region = regionOf(leafType);
          String accName = registry.freshCssaName("__oc_aggacc");
          resolution = indexSymbol(resolution, accName, leafType, pEdge);
          Formula valueTerm = resolve(resolution, accName, leafType, pEdge);
          if (aggregate.write()) {
            // a copy `s = other;` propagates the source's value at region granularity; a write
            // with no matching source read (e.g. `s = f();`) leaves the value unconstrained
            Formula sourceValue = aggregateReadValueByRegion.get(region);
            if (sourceValue != null) {
              registry.addPathConstraint(
                  bfmgr.implication(pState.getGuard(), fmgr.makeEqual(valueTerm, sourceValue)));
            }
          } else {
            aggregateReadValueByRegion.put(region, valueTerm);
          }
          MemoryEvent event =
              registry.addEvent(
                  pState.getInstanceId(),
                  aggregate.write() ? EventKind.WRITE : EventKind.READ,
                  lastEventId,
                  pState.getGuard(),
                  null,
                  accName,
                  valueTerm,
                  null,
                  MemoryEvent.NO_INSTANCE,
                  region,
                  baseTerm,
                  null,
                  true,
                  fillSize,
                  pEdge);
          if (atomicObject || leafType.getCanonicalType().isAtomic()) {
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
    }
    if (anyEvent) {
      // several accesses on one edge (e.g. both operands of `a != b`) chain into one event
      // sequence, but only the last one becomes this state's lastEventIds; record how to resolve
      // an earlier event in the chain back to this state (see
      // OcExplorationRegistry#chainTerminalEventId).
      registry.registerChainTerminal(firstNewEventId, lastEventId);
      return ImmutableList.of(lastEventId);
    }
    return pState.getLastEventIds();
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

  /** The flat address of a mutex object, plus the events its evaluation chained. */
  private record MutexAddress(Formula term, ImmutableList<Integer> lastEventIds) {}

  /**
   * The flat address of the mutex object a lock/unlock argument designates, evaluated through the
   * same renamer used for ordinary memory: {@code &m} becomes the object's address, {@code
   * &(p->lock)} becomes p's value plus the field offset, a bare {@code pthread_mutex_t*} becomes
   * its value. Any base the address minted (e.g. {@code &globalMutex}) is registered for the
   * layout, and the binding is asserted so the returned term carries the value. The reads the
   * evaluation itself performs (of the pointer sub-expressions) are marked as bookkeeping so they
   * are not data-race candidates; the lock/unlock event is chained after them.
   */
  private MutexAddress evaluateMutexAddress(
      OrderingConsistencyState pState, CExpression pArg, CFAEdge pEdge)
      throws CPATransferException, InterruptedException {
    CType pointerType = pArg.getExpressionType();
    String name = registry.freshCssaName("__oc_mtxaddr");
    // a fresh LOCAL id, so cloning instance-prefixes it to a name we can predict and resolve (a
    // global id would instead be renamed to an unpredictable fresh name by the global renamer)
    CVariableDeclaration declaration =
        new CVariableDeclaration(
            FileLocation.DUMMY, false, CStorageClass.AUTO, pointerType, name, name, name, null);
    CIdExpression freshId = new CIdExpression(FileLocation.DUMMY, pointerType, name, declaration);
    CBinaryExpression binding =
        new CBinaryExpression(
            FileLocation.DUMMY,
            CNumericTypes.INT,
            pointerType,
            freshId,
            pArg,
            CBinaryExpression.BinaryOperator.EQUALS);
    CAssumeEdge bindingEdge =
        new CAssumeEdge(
            "", FileLocation.DUMMY, pEdge.getPredecessor(), pEdge.getSuccessor(), binding, true);
    int firstEventId = registry.nextEventId();
    ChainedEdge chained =
        cloneAndChain(pState, new CollectingRenamer(pState.getInstanceId()), bindingEdge);
    for (int id = firstEventId; id < registry.nextEventId(); id++) {
      registry.markThreadHandleAccess(id); // mutex bookkeeping: not a real memory access to race on
    }
    String clonedName = "T" + pState.getInstanceId() + "_" + name;
    return new MutexAddress(
        resolve(chained.edgeFormula(), clonedName, pointerType, pEdge), chained.lastEventIds());
  }

  /**
   * A per-thread syntactic key that pairs a lock with its matching unlock for nesting-depth
   * tracking (a critical section's static extent). Cross-thread mutex identity — whether two
   * sections exclude each other — is decided by the mutex address, not by this key, so a purely
   * syntactic key is enough here and never has to be rejected.
   */
  private static String mutexNestingKey(CExpression pArg) {
    CExpression expression = stripCasts(pArg);
    if (expression instanceof CUnaryExpression unary
        && unary.getOperator() == CUnaryExpression.UnaryOperator.AMPER) {
      return unary.getOperand().toASTString();
    }
    return expression.toASTString();
  }

  /**
   * Whether a mutex argument's target object is <em>not</em> statically fixed. {@code
   * &staticallyResolvableLvalue} (e.g. {@code &m}, {@code &arr[0]}, {@code &p.lock}) always denotes
   * the same object, so it is unambiguous; a bare pointer value ({@code m}), a dereference, or
   * {@code &arr[symbolic]} may point at different objects on different paths and is therefore
   * ambiguous. Used to decide whether an unlock must be paired with its lock by address rather than
   * by the syntactic nesting key (see {@link #mutexNestingKey} and {@code OcEncoder.buildCsPairs}).
   */
  private static boolean isAmbiguousMutexTarget(CExpression pArg) {
    CExpression expression = stripCasts(pArg);
    return !(expression instanceof CUnaryExpression unary
        && unary.getOperator() == CUnaryExpression.UnaryOperator.AMPER
        && MutexFunctions.extractMutexName(unary) != null);
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
    // a heap allocation's size is not statically known (it may be an array of the pointee type),
    // so reserve a whole default-sized range to keep it disjoint from every other object
    registry.addAddressBase(baseTerm, DEFAULT_OBJECT_SIZE);
    registry.addPathConstraint(
        bfmgr.implication(pState.getGuard(), fmgr.makeEqual(lhsTerm, baseTerm)));

    // the allocation provides one indeterminate value across each of its scalar leaf regions: a
    // fill write covers the whole allocation, so a later read at any offset can read from it and
    // uninitialised reads stay feasible (cells of one region share one indeterminate value)
    ImmutableList<Integer> lastEventIds = pLastEventIds;
    for (CType leafType : scalarLeafTypes(pointee)) {
      String initName = registry.freshCssaName("__oc_heapinit");
      resolution = indexSymbol(resolution, initName, leafType, pEdge);
      int primary = lastEventIds.isEmpty() ? MemoryEvent.NO_EVENT : lastEventIds.getFirst();
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
              DEFAULT_OBJECT_SIZE,
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
      ImmutableSet<Integer> pLiveInstanceIds,
      ImmutableMap<String, Integer> pLockDepths) {
    ImmutableMap<CFANode, Integer> loopCounts = pState.getLoopCounts();
    if (pNextNode.isLoopStart()) {
      int count = loopCounts.getOrDefault(pNextNode, 0) + 1;
      if (count > cpa.getMaxLoopIterations()) {
        registry.markTruncated();
        // the thread has not terminated on this path, so it can never be joined here
        int primary = pLastEventIds.isEmpty() ? MemoryEvent.NO_EVENT : pLastEventIds.getFirst();
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
            pLiveInstanceIds,
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
    // the instance whose edge is being cloned; addressed locals get a per-instance base (see
    // baseNameFor(CVariableDeclaration, int)), so this must be the base of every local address here
    private final int instanceId;
    private final List<PendingAccess> accesses = new ArrayList<>();
    private final List<PendingBase> mintedBases = new ArrayList<>();
    private final List<CVariableDeclaration> aggregateDecls = new ArrayList<>();
    private final List<AggregateAccess> aggregateAccesses = new ArrayList<>();

    private CollectingRenamer(int pInstanceId) {
      instanceId = pInstanceId;
    }

    @Override
    public boolean treatsLocalAsRegion(CVariableDeclaration pLocal) {
      // an address-taken local joins the aliasing regime, so a pointer or a thread the local
      // escaped to reaches the same memory (see baseNameFor(CVariableDeclaration, int))
      return cpa.getAddressedVariables().contains(pLocal.getQualifiedName());
    }

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
        // A whole-object value use (e.g. `s = other;`) reads or writes every cell of the object.
        // Record it so chainAccessEvents can emit per-region fill events; the object's own
        // declaration edge is filtered out there (its initial writes already cover it).
        aggregateAccesses.add(new AggregateAccess(pDeclaration, pIsWrite));
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
        access.addressName = baseNameFor(pDeclaration, instanceId);
        access.addressType = new CPointerType(CTypeQualifiers.NONE, pDeclaration.getType());
        mintedBases.add(new PendingBase(access.addressName, access.addressType));
      }
      accesses.add(access);
      return cssaName;
    }

    @Override
    public CExpression replaceAddressOf(
        CUnaryExpression pOriginalAddressOf, UnaryOperator<CExpression> pSubCloner) {
      CExpression operand = stripCasts(pOriginalAddressOf.getOperand());
      if (operand instanceof CIdExpression id
          && id.getExpressionType().getCanonicalType() instanceof CFunctionType) {
        return null; // function designators keep the default cloning
      }
      if (operand instanceof CIdExpression id
          && id.getDeclaration() instanceof CVariableDeclaration decl) {
        // the address of a whole named object: a shared base for a global, a per-instance base for
        // a local (an escaped local reaches the same base in the spawned thread, see
        // bindThreadArgument). Offset zero, so the base term is the address.
        String baseName = baseNameFor(decl, instanceId);
        CType baseType = new CPointerType(CTypeQualifiers.NONE, decl.getType());
        mintedBases.add(new PendingBase(baseName, baseType));
        return syntheticIdExpression(baseName, baseType);
      }
      // the address of an interior lvalue (&a[i], &s.field, &p->field): its byte address is the
      // object's base plus the accumulated offset. In the flat memory layout a full address
      // base + offset is a first-class value, so a pointer holding it aliases a direct access to
      // the same cell exactly (see OcEncoder.sameAddress). Reuse analyzeLvalue to resolve the base
      // and offset, then hand back base + offset as one pointer value.
      if (operand instanceof CLeftHandSide lvalue) {
        PendingAccess offsetHolder = new PendingAccess(null, "", false, lvalue.getExpressionType());
        BaseInfo base = analyzeLvalue(lvalue, offsetHolder, pSubCloner);
        CExpression baseValue = syntheticIdExpression(base.name(), base.type());
        if (offsetHolder.offsetExpr == null) {
          return baseValue;
        }
        CType charPtr = new CPointerType(CTypeQualifiers.NONE, CNumericTypes.CHAR);
        CExpression byteBase = new CCastExpression(FileLocation.DUMMY, charPtr, baseValue);
        CExpression address =
            new CBinaryExpression(
                FileLocation.DUMMY,
                charPtr,
                charPtr,
                byteBase,
                offsetHolder.offsetExpr,
                CBinaryExpression.BinaryOperator.PLUS);
        return new CCastExpression(
            FileLocation.DUMMY, pOriginalAddressOf.getExpressionType(), address);
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
        if (id.getDeclaration() instanceof CVariableDeclaration decl) {
          return mintObjectBase(baseNameFor(decl, instanceId), decl.getType());
        }
        return null; // a parameter or other non-object base; handled elsewhere
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

    /** A distinct base constant of the given (already global-or-instance-scoped) name and type. */
    private BaseInfo mintObjectBase(String pBaseName, CType pObjectType) {
      CType baseType = new CPointerType(CTypeQualifiers.NONE, pObjectType);
      mintedBases.add(new PendingBase(pBaseName, baseType));
      return new BaseInfo(pBaseName, baseType);
    }
  }

  /** The base object identity of an aliased access. */
  private record BaseInfo(String name, CType type) {}

  /**
   * A whole-object read or write of an aggregate (struct/array/union) object — a global, or an
   * address-taken local in the aliasing regime. {@link #chainAccessEvents} models it as one fill
   * event per scalar leaf region, each covering the whole object of that region.
   */
  private record AggregateAccess(CVariableDeclaration declaration, boolean write) {}

  private long sizeofBytes(CType pType) {
    return machineModel.getSizeof(pType.getCanonicalType()).longValueExact();
  }

  /**
   * Fallback object size (bytes) for the flat memory layout when the exact size is unavailable — an
   * incomplete type, a variable-length array, or a heap allocation of statically unknown size.
   * Large enough that no realistic single object exceeds it (so ranges stay disjoint) yet small
   * enough that many objects still fit the address space; mirrors the role of {@code
   * FormulaEncodingWithPointerAliasingOptions.defaultAllocationSize}.
   */
  private static final long DEFAULT_OBJECT_SIZE = 1L << 20;

  /**
   * Byte size of the object an allocation base heads, for laying it out disjointly from other
   * objects. Returns {@link #DEFAULT_OBJECT_SIZE} when the type has no known constant size.
   */
  private long objectSizeBytes(CType pType) {
    CType canonical = pType.getCanonicalType();
    if (!canonical.hasKnownConstantSize()) {
      return DEFAULT_OBJECT_SIZE;
    }
    long size = machineModel.getSizeof(canonical).longValueExact();
    return size <= 0 ? DEFAULT_OBJECT_SIZE : size;
  }

  /** Byte size of the object a {@code T*} base term points at (its pointee), for the layout. */
  private long pointeeSizeBytes(CType pPointerType) {
    CType canonical = pPointerType.getCanonicalType();
    return canonical instanceof CPointerType pointer
        ? objectSizeBytes(pointer.getType())
        : DEFAULT_OBJECT_SIZE;
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

  /**
   * Base name for an address-taken object. A global names one shared object, so its base is keyed
   * on the qualified name alone. A local (or the array/struct it lives in) is a <em>distinct</em>
   * object per thread instance — two threads running the same function have separate copies — so
   * its base is additionally keyed on the owning instance id. The same per-instance name is what a
   * spawned thread binds its parameter to when a creator passes {@code &local} (see {@link
   * #bindThreadArgument}), which is how an escaped local is recognized as one shared object across
   * the two threads.
   */
  private static String baseNameFor(CVariableDeclaration pDecl, int pInstanceId) {
    return pDecl.isGlobal()
        ? baseNameFor(pDecl.getQualifiedName())
        : "__oc_base_T" + pInstanceId + "_" + pDecl.getQualifiedName();
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
