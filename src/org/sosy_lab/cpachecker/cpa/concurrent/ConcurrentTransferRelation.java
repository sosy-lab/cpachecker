// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.concurrent;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.composite.BasicBlockAggregator;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.mutex.MutexState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.dependencegraph.EdgeDefUseData;
import org.sosy_lab.cpachecker.util.refinement.ForgetfulState;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class ConcurrentTransferRelation implements TransferRelation {
  private final TransferRelation wrappedTransferRelation;
  private final ConfigurableProgramAnalysis threadSpecificCPA;
  private final TransferRelation threadSpecificTransferRelation;

  private final CFA cfa;

  private final PartialOrderReductionStrategy por;

  private final LogManager logger;

  private final boolean aggregateBasicBlocks;
  private final BasicBlockAggregator basicBlockAggregator;

  private final Random random;

  /**
   * Thread-specific precision is not implemented properly yet, as thread-specific CPAs do not use a
   * precision for current analyses. The wrapper precision of the POR CPA should be extended to also
   * include thread-specific precisions (similarly to this transfer relation), if needed.
   */
  private final Precision threadSpecificUnitPrecision;

  /**
   * The program's {@code __thread} variables, scanned once: every spawned thread's private copy of
   * each of them has to be initialized at its {@code pthread_create} (see {@link
   * #initializeThreadLocals}).
   */
  private final ImmutableList<CVariableDeclaration> threadLocalDeclarations;

  private final EdgeDefUseData.Extractor defUseExtractor =
      new EdgeDefUseData.CachingExtractor(EdgeDefUseData.createExtractor(true, true));

  @SuppressWarnings("unused") // pConfig and the declared exceptions are kept for future use
  public ConcurrentTransferRelation(
      ConfigurableProgramAnalysis wrappedCpa,
      ConfigurableProgramAnalysis pThreadSpecificCPA,
      Configuration pConfig,
      CFA pCfa,
      PartialOrderReductionStrategy pPor,
      boolean pAggregateBasicBlocks,
      LogManager pLogger,
      Random pRandom)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    wrappedTransferRelation = wrappedCpa.getTransferRelation();

    // Construct thread specific CPA
    threadSpecificCPA = pThreadSpecificCPA;
    threadSpecificTransferRelation = threadSpecificCPA.getTransferRelation();
    threadSpecificUnitPrecision =
        threadSpecificCPA.getInitialPrecision(
            CFANode.newDummyCFANode(), StateSpacePartition.getDefaultPartition());

    cfa = pCfa;
    por = pPor;
    logger = pLogger;
    threadLocalDeclarations = ThreadFunctions.threadLocalGlobals(pCfa);

    aggregateBasicBlocks = pAggregateBasicBlocks;
    basicBlockAggregator =
        aggregateBasicBlocks ? new SingleGlobalStatementBlockAggregator(pCfa) : null;

    random = pRandom;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState state, Precision precision) throws CPATransferException, InterruptedException {
    if (!(state instanceof ConcurrentState pConcurrentState)) {
      throw new CPATransferException("State is not a ConcurrentState.");
    }
    if (!(precision instanceof ConcurrentPrecision pConcurrentPrecision)) {
      throw new CPATransferException("Precision is not ConcurrentPrecision");
    }

    Collection<CFAEdge> sourceSet =
        pConcurrentState.getEdgesToExplore(pConcurrentPrecision, basicBlockAggregator);
    List<AbstractState> allSuccessors = new ArrayList<>();
    for (CFAEdge edge : sourceSet) {
      allSuccessors.addAll(getAbstractSuccessorsForEdge(state, precision, edge));
    }
    return allSuccessors;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    if (!(state instanceof ConcurrentState pConcurrentState)) {
      throw new CPATransferException("State is not a ConcurrentState.");
    }
    if (!(precision instanceof ConcurrentPrecision pConcurrentPrecision)) {
      throw new CPATransferException("Precision is not ConcurrentPrecision");
    }

    // Determine which thread this edge belongs to (populated by getOutgoingEdges)
    final Integer threadId = pConcurrentState.getEdgeThreadId(cfaEdge);
    if (threadId == null) {
      throw new CPATransferException("Could not determine thread for edge " + cfaEdge);
    }

    CFANode startNode = cfaEdge.getPredecessor();
    if (aggregateBasicBlocks
        && basicBlockAggregator.isValidMultiEdgeStart(startNode)
        && basicBlockAggregator.isValidMultiEdgeComponent(startNode, cfaEdge)) {
      return getAbstractSuccessorsForBasicBlock(
          pConcurrentState, pConcurrentPrecision, cfaEdge, threadId);
    }

    Collection<ConcurrentState> results = new ArrayList<>(1);
    getAbstractSuccessorsForEdge(
        pConcurrentState, pConcurrentPrecision, cfaEdge, threadId, results);
    return ImmutableList.copyOf(results);
  }

  private Collection<ConcurrentState> getAbstractSuccessorsForBasicBlock(
      ConcurrentState initialState, ConcurrentPrecision precision, CFAEdge firstEdge, int threadId)
      throws CPATransferException, InterruptedException {
    CFANode startNode = firstEdge.getPredecessor();
    CFAEdge edge = firstEdge;
    Collection<ConcurrentState> currentStates = new ArrayList<>(1);
    currentStates.add(initialState);
    boolean hasAnyResults = false;

    while (basicBlockAggregator.isValidMultiEdgeComponent(startNode, edge)) {
      Collection<ConcurrentState> successorStates = new ArrayList<>(currentStates.size());
      for (ConcurrentState currentState : currentStates) {
        getAbstractSuccessorsForEdge(currentState, precision, edge, threadId, successorStates);
      }

      if (successorStates.isEmpty()) {
        return hasAnyResults ? ImmutableList.copyOf(currentStates) : ImmutableList.of();
      }
      if (from(successorStates).anyMatch(AbstractStates::isTargetState)) {
        return ImmutableList.copyOf(successorStates);
      }

      currentStates = Collections.unmodifiableCollection(successorStates);
      hasAnyResults = true;

      if (edge.getSuccessor().getNumLeavingEdges() == 1) {
        edge = currentStates.iterator().next().getNextBasicBlockEdge(threadId);
      } else {
        break;
      }
    }

    return ImmutableList.copyOf(currentStates);
  }

  private void getAbstractSuccessorsForEdge(
      ConcurrentState state,
      ConcurrentPrecision precision,
      CFAEdge cfaEdge,
      int threadId,
      Collection<ConcurrentState> result)
      throws CPATransferException, InterruptedException {
    Collection<? extends AbstractState> wrappedSuccessors =
        applyEdgeWithForgetting(precision, state.getWrappedState(), cfaEdge, threadId);
    if (wrappedSuccessors.isEmpty()) {
      return;
    }

    Optional<String> functionName = calledFunctionName(cfaEdge);
    if (functionName.isPresent()) {
      String name = functionName.orElseThrow();
      List<? extends AExpression> params = callParameters(cfaEdge);
      if (ThreadFunctions.isCreateFunction(name)) {
        handleThreadCreate(state, precision, cfaEdge, threadId, params, wrappedSuccessors, result);
        return;
      }
      if (ThreadFunctions.isJoinFunction(name)) {
        handleThreadJoin(state, precision, cfaEdge, threadId, params, wrappedSuccessors, result);
        return;
      }
      if (ThreadFunctions.isThreadExitFunction(name)) {
        handleThreadExit(state, cfaEdge, threadId, wrappedSuccessors, result);
        return;
      }
    }

    finishEdge(state, cfaEdge, threadId, wrappedSuccessors, result);
  }

  private void handleThreadCreate(
      ConcurrentState state,
      ConcurrentPrecision precision,
      CFAEdge cfaEdge,
      int threadId,
      List<? extends AExpression> params,
      Collection<? extends AbstractState> wrappedSuccessors,
      Collection<ConcurrentState> result)
      throws CPATransferException, InterruptedException {
    ThreadFunctions.checkCreateParams(params);
    String threadFunction = ThreadFunctions.extractCreateFunctionName(params);
    int newThreadId = state.threads().size();
    List<AbstractState> afterHandleWrite = new ArrayList<>();
    for (AbstractState wrapped : wrappedSuccessors) {
      afterHandleWrite.addAll(
          applyBookkeepingEdge(
              precision,
              wrapped,
              writeThreadHandleEdge((CExpression) params.getFirst(), newThreadId, cfaEdge),
              threadId));
    }

    Optional<CFAEdge> argumentInit =
        threadArgumentInitEdge(threadFunction, (CExpression) params.get(3), newThreadId, cfaEdge);
    if (argumentInit.isPresent()) {
      CFAEdge argumentInitEdge = argumentInit.orElseThrow();
      List<AbstractState> afterArgumentInit = new ArrayList<>(afterHandleWrite.size());
      for (AbstractState wrapped : afterHandleWrite) {
        afterArgumentInit.addAll(
            applyBookkeepingEdge(precision, wrapped, argumentInitEdge, threadId));
      }
      afterHandleWrite = afterArgumentInit;
    }

    List<AbstractState> afterThreadLocalInit =
        initializeThreadLocals(precision, afterHandleWrite, newThreadId, cfaEdge, threadId);
    Optional<String> handleName =
        ThreadFunctions.canonicalHandleAddressKey((CExpression) params.getFirst());
    finishEdge(
        addNewThread(state, threadFunction, handleName),
        cfaEdge,
        threadId,
        afterThreadLocalInit,
        result);
  }

  private void handleThreadJoin(
      ConcurrentState state,
      ConcurrentPrecision precision,
      CFAEdge cfaEdge,
      int threadId,
      List<? extends AExpression> params,
      Collection<? extends AbstractState> wrappedSuccessors,
      Collection<ConcurrentState> result)
      throws CPATransferException, InterruptedException {
    ThreadFunctions.checkJoinParams(params);
    CExpression handle = (CExpression) params.getFirst();

    // Fast path for the common case: the handle expression's storage location can be determined
    // syntactically and a single create call already paired that location with a thread ID. Joining
    // directly avoids adding a synthetic identity assumption to the wrapped analysis, which could
    // otherwise steer refinement toward facts about the handle rather than program behavior.
    Optional<String> handleKey = ThreadFunctions.canonicalHandleLvalueKey(handle);
    if (handleKey.isPresent()) {
      OptionalInt hint = state.getLastCreatedThreadId(handleKey.orElseThrow());
      if (hint.isPresent() && state.liveThreadIds().contains(hint.orElseThrow())) {
        Optional<ConcurrentState> joined = state.joinThread(hint.orElseThrow());
        if (joined.isPresent()) {
          finishEdge(joined.orElseThrow(), cfaEdge, threadId, wrappedSuccessors, result);
        }
        return;
      }
    }

    // Without a unique syntactic mapping, branch over live instances and let the wrapped analysis
    // filter each candidate's synthetic identity assumption; this needs no alias analysis here.
    for (int candidateThreadId : state.liveThreadIds()) {
      Optional<ConcurrentState> joined = state.joinThread(candidateThreadId);
      if (joined.isEmpty()) {
        continue;
      }
      List<AbstractState> filtered = new ArrayList<>();
      for (AbstractState wrapped : wrappedSuccessors) {
        filtered.addAll(
            applyBookkeepingEdge(
                precision,
                wrapped,
                assumeThreadHandleEqualsEdge(handle, candidateThreadId, cfaEdge),
                threadId));
      }
      if (!filtered.isEmpty()) {
        finishEdge(joined.orElseThrow(), cfaEdge, threadId, filtered, result);
      }
    }
  }

  private void handleThreadExit(
      ConcurrentState state,
      CFAEdge cfaEdge,
      int threadId,
      Collection<? extends AbstractState> wrappedSuccessors,
      Collection<ConcurrentState> result)
      throws CPATransferException, InterruptedException {
    ThreadState threadState = state.threads().get(threadId);
    assert threadState != null : "threads must contain thread ID to exit " + threadId;
    CFANode currentNode = threadState.getLocationNode();
    CFANode originalCurrentNode = currentNode.getOriginal();
    String function = originalCurrentNode.getFunctionName();
    CFANode originalExitNode =
        cfa.nodes().stream()
            .filter(
                node ->
                    node instanceof FunctionExitNode
                        && function.equals(node.getFunctionName())
                        && node.getNumLeavingEdges() == 0)
            .findAny()
            .orElseThrow();
    CFANode clonedExitNode = ConcurrentEdgeCloner.getClonedNode(originalExitNode, threadId, cfa);
    String description = "Thread exit dummy edge";
    CFAEdge exitEdge =
        new BlankEdge(
            description, cfaEdge.getFileLocation(), currentNode, clonedExitNode, description);

    Collection<? extends AbstractState> threadSpecificSuccessors =
        threadSpecificTransferRelation.getAbstractSuccessorsForEdge(
            threadState.getWrappedState(), threadSpecificUnitPrecision, exitEdge);
    for (AbstractState threadSpecificSuccessor : threadSpecificSuccessors) {
      if (!(threadSpecificSuccessor instanceof CompositeState nextWrappedState)) {
        throw new CPATransferException("Thread-specific successor is not a ThreadState");
      }
      ThreadState nextThreadState = new ThreadState(nextWrappedState);
      ConcurrentState exited = state.stepThread(threadId, nextThreadState);
      finishEdge(exited, cfaEdge, threadId, wrappedSuccessors, result);
    }
  }

  private void finishEdge(
      ConcurrentState old,
      CFAEdge cfaEdge,
      int threadId,
      Collection<? extends AbstractState> wrappedSuccessors,
      Collection<ConcurrentState> result)
      throws CPATransferException, InterruptedException {
    final ThreadState threadState = old.threads().get(threadId);
    if (threadState == null) {
      throw new CPATransferException("Thread state not found for thread ID " + threadId);
    }

    final Collection<? extends AbstractState> nextThreadSpecificStates =
        threadSpecificTransferRelation.getAbstractSuccessorsForEdge(
            threadState.getWrappedState(), threadSpecificUnitPrecision, cfaEdge);
    final List<ConcurrentState> successors = new ArrayList<>();
    for (AbstractState nextThreadSpecificState : nextThreadSpecificStates) {
      if (!(nextThreadSpecificState instanceof CompositeState nextWrappedState)) {
        throw new CPATransferException("Thread-specific successor is not a ThreadState");
      }
      ThreadState nextThreadState = new ThreadState(nextWrappedState);
      successors.add(old.stepThread(threadId, nextThreadState));
    }

    for (ConcurrentState porSuccessor : successors) {
      for (AbstractState wrappedSuccessor : wrappedSuccessors) {
        result.add(porSuccessor.withWrappedState(wrappedSuccessor));
      }
    }
  }

  /**
   * Applies one of POR's own synthetic bookkeeping edges (the thread-handle write at a create, the
   * handle-equality assume at a join) to {@code wrappedState}, keeping the state itself if the edge
   * yields nothing.
   *
   * <p>These edges are not part of the program: they exist only so the wrapped analysis learns
   * which instance a handle denotes. So they must never be able to <b>lose</b> a state. They can,
   * because a CPA is allowed to report a violation by producing no successors at all — {@link
   * org.sosy_lab.cpachecker.cpa.overflow.OverflowTransferRelation} does exactly that ("once we have
   * an overflow there is no need to continue"). Feeding such an already-violating state through a
   * bookkeeping edge would return an empty collection, POR would take that for "infeasible branch",
   * and the violation would be silently dropped — a wrong TRUE on a program that really does
   * overflow. When the state is already a target, keep it unchanged: exploration ends there anyway,
   * so the handle value it never got told about cannot matter.
   */
  private Collection<? extends AbstractState> applyBookkeepingEdge(
      ConcurrentPrecision precision, AbstractState wrappedState, CFAEdge edge, int threadId)
      throws CPATransferException, InterruptedException {
    Collection<? extends AbstractState> successors =
        applyEdgeWithForgetting(precision, wrappedState, edge, threadId);
    if (successors.isEmpty() && AbstractStates.isTargetState(wrappedState)) {
      return ImmutableList.of(wrappedState);
    }
    return successors;
  }

  /**
   * Runs {@code edge} through the wrapped transfer relation, first temporarily forgetting any value
   * the reduction currently treats as ignorable (see {@link
   * ConcurrentPrecision#canIgnoreVariable}): a domain that tracks concrete/precise values (e.g.
   * ValueAnalysisCPA) could otherwise decide the edge's outcome (an assume's direction, in
   * particular) from a value the reduction assumed did not need cross-thread ordering, silently
   * baking in whichever single interleaving happened to be explored instead of exploring every
   * possibility the way an analysis with no information at all would (e.g. predicate abstraction
   * with no predicate on the variable). Forgetting makes every domain behave like the latter for
   * precisely the variables POR is treating as independent.
   *
   * <p>Also registers {@code threadId} as {@code edge}'s executing thread on any {@link MutexState}
   * component: MutexCPA's transfer relation requires a thread ID for every edge it processes (see
   * {@code MutexTransferRelation}), and a synthetic edge built on the fly (the thread-handle
   * write/assume edges) is not one ConcurrentState's normal edge-enumeration already registered.
   * Re-registering the real edge here too is a harmless no-op (same value it already has).
   */
  private Collection<? extends AbstractState> applyEdgeWithForgetting(
      ConcurrentPrecision precision, AbstractState wrappedState, CFAEdge edge, int threadId)
      throws CPATransferException, InterruptedException {
    MutexState mutexState = AbstractStates.extractStateByType(wrappedState, MutexState.class);
    if (mutexState != null) {
      mutexState.addEdgePids(new HashMap<>(ImmutableMap.of(edge, threadId)));
    }
    List<Runnable> restoreForgotten = new ArrayList<>();
    Iterable<MemoryLocation> uses = defUseExtractor.extract(edge).getUses();
    for (AbstractState component : AbstractStates.asIterable(wrappedState)) {
      if (component instanceof ForgetfulState<?> forgetfulState) {
        for (MemoryLocation location : uses) {
          if (precision.canIgnoreVariable(location)
              && forgetfulState.getTrackedMemoryLocations().contains(location)) {
            restoreForgotten.add(forgetTemporarily(forgetfulState, location));
          }
        }
      }
    }
    try {
      return wrappedTransferRelation.getAbstractSuccessorsForEdge(
          wrappedState, precision.getWrappedPrecision(), edge);
    } finally {
      restoreForgotten.forEach(Runnable::run);
    }
  }

  /**
   * Forgets {@code location} from {@code state} and returns a callback that restores it. The
   * generic parameter is captured from the wildcard at the call site so the removed information can
   * be handed back to {@link ForgetfulState#remember} with the right type.
   */
  private static <T> Runnable forgetTemporarily(ForgetfulState<T> state, MemoryLocation location) {
    T forgotten = state.forget(location);
    return () -> state.remember(location, forgotten);
  }

  /** The called function's name, or null if {@code edge} is not a function-call statement. */
  private static Optional<String> calledFunctionName(CFAEdge edge) {
    if (edge instanceof AStatementEdge statementEdge
        && statementEdge.getStatement() instanceof AFunctionCall call
        && call.getFunctionCallExpression().getFunctionNameExpression()
            instanceof AIdExpression functionName) {
      return Optional.of(functionName.getName());
    }
    return Optional.empty();
  }

  private static List<? extends AExpression> callParameters(CFAEdge edge) {
    AFunctionCall call = (AFunctionCall) ((AStatementEdge) edge).getStatement();
    return call.getFunctionCallExpression().getParameterExpressions();
  }

  /** The pointee type of an lvalue expected to be a pointer (a pthread_create/join handle). */
  private static CType threadHandlePointeeType(CExpression handle, CFAEdge edge)
      throws UnsupportedCodeException {
    CType type = handle.getExpressionType().getCanonicalType();
    if (type instanceof CPointerType pointerType) {
      return pointerType.getType();
    }
    throw new UnsupportedCodeException("thread handle is not a pointer expression", edge);
  }

  /**
   * The lvalue a pthread_create/join handle expression actually designates. When the handle is
   * syntactically {@code &lvalue} (by far the common case, e.g. {@code &t} or {@code &t[i]}), that
   * inner lvalue *is* the target — dereferencing it again ({@code *(&lvalue)}) is a pattern the C
   * frontend never itself produces (it already folds {@code *&x} to {@code x}), so the wrapped
   * analysis's own C-expression handling does not recognize it. Otherwise the handle is already
   * pointer-typed (e.g. a {@code pthread_t*} parameter), and the lvalue is the ordinary dereference
   * {@code *handle}.
   */
  private static CLeftHandSide threadHandleLvalue(CExpression handle, CFAEdge edge)
      throws UnsupportedCodeException {
    if (handle instanceof CUnaryExpression unary
        && unary.getOperator() == CUnaryExpression.UnaryOperator.AMPER
        && unary.getOperand() instanceof CLeftHandSide lvalue) {
      return lvalue;
    }
    return new CPointerExpression(
        FileLocation.DUMMY, threadHandlePointeeType(handle, edge), handle);
  }

  /**
   * A synthetic {@code handleLvalue = id;} edge that writes a fresh literal identifying {@code
   * threadId} through the (arbitrary) lvalue naming the new thread's handle. Handed to the wrapped
   * analysis exactly like any other edge, so whatever it already supports for regular lvalues
   * (array elements, struct fields, ...) is all that is needed here — nothing POR-specific.
   */
  private static CFAEdge writeThreadHandleEdge(CExpression handle, int threadId, CFAEdge edge)
      throws UnsupportedCodeException {
    CLeftHandSide lhs = threadHandleLvalue(handle, edge);
    CIntegerLiteralExpression rhs =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY, lhs.getExpressionType(), BigInteger.valueOf((long) threadId + 1));
    return new CStatementEdge(
        "",
        new CExpressionAssignmentStatement(FileLocation.DUMMY, lhs, rhs),
        FileLocation.DUMMY,
        edge.getPredecessor(),
        edge.getSuccessor());
  }

  /**
   * Initializes the thread being created at {@code cfaEdge}: applies one synthetic assignment per
   * {@code __thread} variable, writing that variable's initial value to the <b>child's</b> private
   * copy. Returns the states resulting from applying all of them in sequence to {@code pStates}.
   *
   * <p>Without this, privatizing a {@code __thread} variable (see {@link ConcurrentAstCloner})
   * would make things worse rather than better: the child's copy is a variable no edge on its path
   * ever assigns, so the wrapped analysis treats it as indeterminate and an {@code assert(data ==
   * 0)} fails on a value the child can never actually hold. Only the main thread's clone contains
   * the file-scope declaration edge that carries the initializer; a spawned thread enters at its
   * start routine and never passes it.
   *
   * <p>The assignments happen on the <b>creating</b> thread's edge (as that thread's bookkeeping),
   * which is sound precisely because the target is private to the child: no other thread can
   * observe it, so where in the schedule the write lands cannot matter.
   */
  private List<AbstractState> initializeThreadLocals(
      ConcurrentPrecision precision,
      List<AbstractState> pStates,
      int newThreadId,
      CFAEdge cfaEdge,
      int creatorThreadId)
      throws CPATransferException, InterruptedException {
    List<AbstractState> states = pStates;
    for (CVariableDeclaration threadLocalDeclaration : threadLocalDeclarations) {
      CFAEdge initEdge = threadLocalInitEdge(threadLocalDeclaration, newThreadId, cfaEdge);
      List<AbstractState> next = new ArrayList<>(states.size());
      for (AbstractState wrapped : states) {
        next.addAll(applyBookkeepingEdge(precision, wrapped, initEdge, creatorThreadId));
      }
      states = next;
    }
    return states;
  }

  /**
   * A synthetic {@code T{newThreadId}_startRoutine::param = <4th pthread_create argument>;} edge
   * binding the spawned thread's start-routine parameter to the argument the creator passes.
   * Without it the child's parameter is a variable no edge on any path ever assigns, so the wrapped
   * analysis treats it as indeterminate — sound, but any property that hinges on the pointed-to
   * data (e.g. {@code *((int *) arg)} staying in a known range) then admits spurious
   * counterexamples, observed as wrong FALSE verdicts on tasks passing {@code &local} through
   * {@code pthread_create} (goblint-regression escape_* / container_of families).
   *
   * <p>The argument expression comes from the creator's already-cloned call edge, so it is
   * evaluated in the creator's namespace — exactly where the concrete semantics evaluate it. The
   * parameter is private to the child, so writing it on the creating thread's bookkeeping edge is
   * sound for the same reason as {@link #threadLocalInitEdge}. Returns {@code null} when the start
   * routine declares no parameter (binding is then meaningless and the old behavior — an unbound
   * parameter — remains).
   */
  private Optional<CFAEdge> threadArgumentInitEdge(
      String pThreadFunc, CExpression pArgument, int pNewThreadId, CFAEdge pEdge) {
    FunctionEntryNode entry = cfa.getFunctionHead(pThreadFunc);
    if (entry == null || entry.getFunctionParameters().isEmpty()) {
      return Optional.empty();
    }
    if (!(entry.getFunctionParameters().getFirst() instanceof CParameterDeclaration origParam)) {
      return Optional.empty();
    }
    CParameterDeclaration childParam =
        new CParameterDeclaration(
            origParam.getFileLocation(), origParam.getType(), origParam.getName());
    childParam.setQualifiedName(
        ThreadFunctions.perThreadName(pNewThreadId, origParam.getQualifiedName()));
    CIdExpression lhs =
        new CIdExpression(
            origParam.getFileLocation(), origParam.getType(), origParam.getName(), childParam);
    CExpression rhs = pArgument;
    if (!rhs.getExpressionType()
        .getCanonicalType()
        .equals(origParam.getType().getCanonicalType())) {
      rhs = new CCastExpression(FileLocation.DUMMY, origParam.getType(), pArgument);
    }
    return Optional.of(
        new CStatementEdge(
            "",
            new CExpressionAssignmentStatement(FileLocation.DUMMY, lhs, rhs),
            FileLocation.DUMMY,
            pEdge.getPredecessor(),
            pEdge.getSuccessor()));
  }

  /**
   * A synthetic {@code T{threadId}_x = <initial value>;} edge for one {@code __thread} variable.
   * The left-hand side is built as the very declaration {@link ConcurrentAstCloner} renames {@code
   * pDecl} to for thread {@code threadId} — same qualified name, same non-global flag — so that
   * this write and the child's own reads of the variable are the same symbol to the wrapped
   * analysis.
   */
  private CFAEdge threadLocalInitEdge(CVariableDeclaration pDecl, int threadId, CFAEdge edge)
      throws UnsupportedCodeException {
    CExpression value = ThreadFunctions.threadLocalInitValue(pDecl, cfa.getMachineModel(), edge);
    CVariableDeclaration childCopy =
        new CVariableDeclaration(
            pDecl.getFileLocation(),
            false,
            pDecl.getCStorageClass(),
            pDecl.getType(),
            pDecl.getName(),
            pDecl.getOrigName(),
            ThreadFunctions.perThreadName(threadId, pDecl.getQualifiedName()),
            null,
            pDecl.hasThreadLocalStorage());
    CIdExpression lhs =
        new CIdExpression(pDecl.getFileLocation(), pDecl.getType(), pDecl.getName(), childCopy);
    return new CStatementEdge(
        "",
        new CExpressionAssignmentStatement(FileLocation.DUMMY, lhs, value),
        FileLocation.DUMMY,
        edge.getPredecessor(),
        edge.getSuccessor());
  }

  /**
   * A synthetic {@code handle == id} assume for one join candidate; see {@link
   * #getAbstractSuccessorsForEdge}'s join dispatch. Unlike pthread_create's handle, pthread_join's
   * is passed <b>by value</b> (POSIX: {@code int pthread_join(pthread_t thread, void **retval)}),
   * so it is compared directly, no dereference.
   */
  private static CFAEdge assumeThreadHandleEqualsEdge(
      CExpression handle, int candidateThreadId, CFAEdge edge) {
    CIntegerLiteralExpression literal =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY,
            handle.getExpressionType(),
            BigInteger.valueOf((long) candidateThreadId + 1));
    CBinaryExpression identity =
        new CBinaryExpression(
            FileLocation.DUMMY,
            CNumericTypes.INT,
            handle.getExpressionType(),
            handle,
            literal,
            CBinaryExpression.BinaryOperator.EQUALS);
    return new CAssumeEdge(
        "", FileLocation.DUMMY, edge.getPredecessor(), edge.getSuccessor(), identity, true);
  }

  /**
   * Recreates thread identity and initialization constraints for concrete counterexample replay.
   */
  List<CFAEdge> getBookkeepingEdges(ConcurrentState parent, ConcurrentState child, CFAEdge edge)
      throws CPATransferException {
    List<CFAEdge> edges = new ArrayList<>();
    Optional<String> name = calledFunctionName(edge);
    if (name.isEmpty()) {
      return edges;
    }
    List<? extends AExpression> params = callParameters(edge);
    if (ThreadFunctions.isCreateFunction(name.orElseThrow())) {
      int newThreadId = parent.threads().size();
      edges.add(writeThreadHandleEdge((CExpression) params.getFirst(), newThreadId, edge));
      threadArgumentInitEdge(
              ThreadFunctions.extractCreateFunctionName(params),
              (CExpression) params.get(3),
              newThreadId,
              edge)
          .ifPresent(edges::add);
      for (CVariableDeclaration declaration : threadLocalDeclarations) {
        edges.add(threadLocalInitEdge(declaration, newThreadId, edge));
      }
    } else if (ThreadFunctions.isJoinFunction(name.orElseThrow())) {
      CExpression handle = (CExpression) params.getFirst();
      for (int threadId : parent.liveThreadIds()) {
        if (!child.liveThreadIds().contains(threadId)) {
          edges.add(assumeThreadHandleEqualsEdge(handle, threadId, edge));
        }
      }
    }
    return edges;
  }

  ConcurrentState initial(AbstractState wrappedInitialState) throws InterruptedException {
    return addNewThreadNode(
        por.emptyState(wrappedInitialState, cfa, logger, random), false, "main", Optional.empty());
  }

  ConcurrentState addNewThread(
      final ConcurrentState old, final String functionName, Optional<String> handleName)
      throws InterruptedException {
    return addNewThreadNode(old, true, functionName, handleName);
  }

  private ConcurrentState addNewThreadNode(
      final ConcurrentState old,
      boolean isJoinable,
      final String functionName,
      Optional<String> handleName)
      throws InterruptedException {
    CFANode functionCallNode =
        Preconditions.checkNotNull(
            cfa.getFunctionHead(functionName), "Function '%s' was not found.", functionName);

    // Compute the thread ID for the new thread so we can get its cloned entry node.
    int newThreadId = old.threads().size();
    CFANode clonedEntryNode =
        ConcurrentEdgeCloner.getClonedNode(functionCallNode, newThreadId, cfa);
    AbstractState initialWrappedState =
        threadSpecificCPA.getInitialState(
            clonedEntryNode, StateSpacePartition.getDefaultPartition());
    checkState(
        initialWrappedState instanceof CompositeState,
        "Thread-specific CPA's initial state is not a CompositeState");
    ThreadState initialThreadState = new ThreadState((CompositeState) initialWrappedState);
    return old.addNewThread(isJoinable, handleName, initialThreadState);
  }
}
