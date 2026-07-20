// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

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
import java.util.Random;
import org.checkerframework.checker.nullness.qual.Nullable;
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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
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
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.composite.BasicBlockAggregator;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.mutex.MutexState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.dependencegraph.EdgeDefUseData;
import org.sosy_lab.cpachecker.util.refinement.ForgetfulState;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class PORTransferRelation implements TransferRelation {
  private final LocationCPA locationCPA;
  private final CallstackCPA callstackCPA;
  private final TransferRelation wrappedTransferRelation;

  private final CFA cfa;

  private final boolean aggregateBasicBlocks;
  private final BasicBlockAggregator basicBlockAggregator;

  private final Random random;

  /**
   * The program's {@code __thread} variables, scanned once: every spawned thread's private copy of
   * each of them has to be initialized at its {@code pthread_create} (see {@link
   * #initializeThreadLocals}).
   */
  private final ImmutableList<CVariableDeclaration> threadLocalGlobals;

  private final EdgeDefUseData.Extractor defUseExtractor =
      new EdgeDefUseData.CachingExtractor(EdgeDefUseData.createExtractor(true, true));

  public PORTransferRelation(
      ConfigurableProgramAnalysis wrappedCpa,
      Configuration pConfig,
      CFA pCfa,
      boolean pAggregateBasicBlocks,
      LogManager pLogger,
      Random pRandom)
      throws InvalidConfigurationException {
    wrappedTransferRelation = wrappedCpa.getTransferRelation();
    locationCPA = LocationCPA.create(pCfa, pConfig);
    callstackCPA = new CallstackCPA(pConfig, pLogger);

    cfa = pCfa;
    threadLocalGlobals = ThreadFunctions.threadLocalGlobals(pCfa);

    aggregateBasicBlocks = pAggregateBasicBlocks;
    basicBlockAggregator =
        aggregateBasicBlocks ? new SingleGlobalStatementBlockAggregator(pCfa) : null;

    random = pRandom;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState state, Precision precision) throws CPATransferException, InterruptedException {
    if (!(state instanceof PORState porState)) {
      throw new CPATransferException("State is not a PORState.");
    }
    if (!(precision instanceof PORPrecision porPrecision)) {
      throw new CPATransferException("Precision is not PORPrecision");
    }

    Collection<CFAEdge> sourceSet = porState.getSourceSet(porPrecision, basicBlockAggregator);
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
    if (!(state instanceof PORState porState)) {
      throw new CPATransferException("State is not a PORState.");
    }
    if (!(precision instanceof PORPrecision porPrecision)) {
      throw new CPATransferException("Precision is not PORPrecision");
    }

    // Determine which thread this edge belongs to (populated by getOutgoingEdges)
    final Integer pid = porState.getEdgePid(cfaEdge);
    if (pid == null) {
      throw new CPATransferException("Could not determine thread for edge " + cfaEdge);
    }

    if (aggregateBasicBlocks) {
      final CFANode startNode = cfaEdge.getPredecessor();

      // dynamic multiEdges may be used if the following conditions apply
      if (basicBlockAggregator.isValidMultiEdgeStart(startNode)
          && basicBlockAggregator.isValidMultiEdgeComponent(startNode, cfaEdge)) {

        Collection<PORState> currentStates = new ArrayList<>(1);
        currentStates.add(porState);
        boolean hasAnyResults = false;

        while (basicBlockAggregator.isValidMultiEdgeComponent(startNode, cfaEdge)) {
          Collection<PORState> successorStates = new ArrayList<>(currentStates.size());

          for (PORState currentState : currentStates) {
            getAbstractSuccessorsForEdge(currentState, porPrecision, cfaEdge, pid, successorStates);
          }

          // if there are no successors for the current edge, we do not need to continue
          if (successorStates.isEmpty()) {
            if (hasAnyResults) {
              return ImmutableList.copyOf(currentStates);
            }
            return ImmutableList.of();
          }

          // if we found a target state in the current successors immediately return
          if (from(successorStates).anyMatch(AbstractStates::isTargetState)) {
            return ImmutableList.copyOf(successorStates);
          }

          // make successor states the new to-be-handled states for the next edge
          currentStates = Collections.unmodifiableCollection(successorStates);
          hasAnyResults = true;

          // if there is more than one leaving edge we do not create a further multi edge part
          if (cfaEdge.getSuccessor().getNumLeavingEdges() == 1) {
            // all current states should be the same PORState
            cfaEdge = currentStates.iterator().next().getNextBasicBlockEdge(pid);
          } else {
            break;
          }
        }

        return ImmutableList.copyOf(currentStates);
      }
    }

    Collection<PORState> results = new ArrayList<>(1);
    getAbstractSuccessorsForEdge(porState, porPrecision, cfaEdge, pid, results);
    return ImmutableList.copyOf(results);
  }

  private void getAbstractSuccessorsForEdge(
      PORState state, PORPrecision precision, CFAEdge cfaEdge, int pid, Collection<PORState> result)
      throws CPATransferException, InterruptedException {
    Collection<? extends AbstractState> wrappedSuccessors =
        applyEdgeWithForgetting(precision, state.getWrappedState(), cfaEdge, pid);
    if (wrappedSuccessors.isEmpty()) {
      return;
    }

    String functionName = calledFunctionName(cfaEdge);
    if (functionName != null) {
      List<? extends AExpression> params = callParameters(cfaEdge);

      if (ThreadFunctions.isCreateFunction(functionName)) {
        ThreadFunctions.checkCreateParams(params);
        String threadFunc = ThreadFunctions.extractCreateFunctionName(params);
        int newPid = state.threads().size();
        List<AbstractState> afterWrite = new ArrayList<>();
        for (AbstractState wrapped : wrappedSuccessors) {
          afterWrite.addAll(
              applyBookkeepingEdge(
                  precision,
                  wrapped,
                  writeThreadHandleEdge((CExpression) params.get(0), newPid, cfaEdge),
                  pid));
        }
        CFAEdge argumentInitEdge =
            threadArgumentInitEdge(threadFunc, (CExpression) params.get(3), newPid, cfaEdge);
        if (argumentInitEdge != null) {
          List<AbstractState> afterArgumentInit = new ArrayList<>(afterWrite.size());
          for (AbstractState wrapped : afterWrite) {
            afterArgumentInit.addAll(
                applyBookkeepingEdge(precision, wrapped, argumentInitEdge, pid));
          }
          afterWrite = afterArgumentInit;
        }
        afterWrite = initializeThreadLocals(precision, afterWrite, newPid, cfaEdge, pid);
        String handleName = ThreadFunctions.canonicalHandleAddressKey((CExpression) params.get(0));
        finishEdge(
            addNewThread(state, threadFunc, handleName),
            precision,
            cfaEdge,
            pid,
            afterWrite,
            result);
        return;
      }

      if (ThreadFunctions.isJoinFunction(functionName)) {
        ThreadFunctions.checkJoinParams(params);
        CExpression handle = (CExpression) params.get(0);

        // Fast path for the common case: the handle expression's storage location can be
        // determined purely syntactically (a plain variable, or a path to it through only literal
        // array indices / non-pointer field accesses — see
        // ThreadFunctions#canonicalHandleLvalueKey)
        // and a single create
        // call already unambiguously paired that same key with a pid (see PORState#handleHints).
        // Joining directly here, without any synthetic assume edge, avoids polluting the wrapped
        // analysis's own reasoning (and, for predicate abstraction, CEGAR's interpolation) with an
        // identity fact about the handle that has nothing to do with the program's actual
        // behavior — observed in practice to make refinement latch onto an irrelevant "handle !=
        // other candidate's id" predicate instead of the real one, in one case even discarding a
        // feasible schedule along with the spurious one it meant to exclude. The general
        // candidate-branching path below remains the sound fallback for every case this fast path
        // does not apply to (e.g. a runtime-computed array index).
        String handleKey = ThreadFunctions.canonicalHandleLvalueKey(handle);
        if (handleKey != null) {
          Integer hint = state.getHandleHint(handleKey);
          if (hint != null && state.livePids().contains(hint)) {
            PORState joined = state.joinThread(hint);
            if (joined != null) {
              finishEdge(joined, precision, cfaEdge, pid, wrappedSuccessors, result);
            }
            return;
          }
        }

        // Candidate-set branching: the handle expression is not resolved to a single instance
        // statically (it may be an array element, a struct field, ...); instead every live
        // thread instance is tried as a candidate, mirroring how POR already branches on the two
        // arms of an if. A candidate branch survives only if (a) that instance has actually
        // finished (joinThread returns non-null) and (b) the wrapped analysis finds "*handle ==
        // candidate's synthetic id" feasible — a candidate whose handle cannot really alias this
        // one is filtered out by the wrapped analysis's own semantics, no bespoke alias-checking
        // needed here (and forgetting an ignorable handle value, same as any other edge, lets
        // more than one candidate stay feasible when POR's reduction has no information yet).
        for (int candidate : state.livePids()) {
          PORState joined = state.joinThread(candidate);
          if (joined == null) {
            continue; // that candidate has not finished yet
          }
          List<AbstractState> filtered = new ArrayList<>();
          for (AbstractState wrapped : wrappedSuccessors) {
            filtered.addAll(
                applyBookkeepingEdge(
                    precision,
                    wrapped,
                    assumeThreadHandleEqualsEdge(handle, candidate, cfaEdge),
                    pid));
          }
          if (!filtered.isEmpty()) {
            finishEdge(joined, precision, cfaEdge, pid, filtered, result);
          }
        }
        return;
      }

      if (ThreadFunctions.isThreadExitFunction(functionName)) {
        PORState exited = state.exitThread(pid, locationCPA.getStateFactory());
        if (exited != null) {
          finishEdge(exited, precision, cfaEdge, pid, wrappedSuccessors, result);
        }
        return;
      }
    }

    finishEdge(state, precision, cfaEdge, pid, wrappedSuccessors, result);
  }

  /**
   * Advances {@code pid}'s location/callstack past {@code cfaEdge} and combines every resulting POR
   * successor with every given wrapped-analysis successor. Shared tail of {@link
   * #getAbstractSuccessorsForEdge}, called once per candidate branch for a join and once otherwise.
   */
  private void finishEdge(
      PORState old,
      PORPrecision precision,
      CFAEdge cfaEdge,
      int pid,
      Collection<? extends AbstractState> wrappedSuccessors,
      Collection<PORState> result)
      throws CPATransferException, InterruptedException {
    final PORThreadState threadState = old.threads().get(pid);
    if (threadState == null) {
      throw new CPATransferException("Thread state not found for PID " + pid);
    }
    final var loc = threadState.pLocationState();
    final var stack = threadState.pCallstackState();

    final var nextLocs =
        locationCPA.getTransferRelation().getAbstractSuccessorsForEdge(loc, precision, cfaEdge);
    final var nextStacks =
        callstackCPA.getTransferRelation().getAbstractSuccessorsForEdge(stack, precision, cfaEdge);

    List<PORState> successors =
        nextLocs.stream()
            .flatMap(
                nextLoc ->
                    nextStacks.stream()
                        .map(
                            nextStack ->
                                old.stepThread(
                                    pid, (LocationState) nextLoc, (CallstackState) nextStack)))
            .toList();

    for (PORState porSuccessor : successors) {
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
      PORPrecision precision, AbstractState wrappedState, CFAEdge edge, int pid)
      throws CPATransferException, InterruptedException {
    Collection<? extends AbstractState> successors =
        applyEdgeWithForgetting(precision, wrappedState, edge, pid);
    if (successors.isEmpty() && AbstractStates.isTargetState(wrappedState)) {
      return ImmutableList.of(wrappedState);
    }
    return successors;
  }

  /**
   * Runs {@code edge} through the wrapped transfer relation, first temporarily forgetting any value
   * the reduction currently treats as ignorable (see {@link PORPrecision#canIgnoreVariable}): a
   * domain that tracks concrete/precise values (e.g. ValueAnalysisCPA) could otherwise decide the
   * edge's outcome (an assume's direction, in particular) from a value the reduction assumed did
   * not need cross-thread ordering, silently baking in whichever single interleaving happened to be
   * explored instead of exploring every possibility the way an analysis with no information at all
   * would (e.g. predicate abstraction with no predicate on the variable). Forgetting makes every
   * domain behave like the latter for precisely the variables POR is treating as independent.
   *
   * <p>Also registers {@code pid} as {@code edge}'s executing thread on any {@link MutexState}
   * component: MutexCPA's transfer relation requires a PID for every edge it processes (see {@code
   * MutexTransferRelation}), and a synthetic edge built on the fly (the thread-handle write/assume
   * edges) is not one PORState's normal edge-enumeration already registered. Re-registering the
   * real edge here too is a harmless no-op (same value it already has).
   */
  private Collection<? extends AbstractState> applyEdgeWithForgetting(
      PORPrecision precision, AbstractState wrappedState, CFAEdge edge, int pid)
      throws CPATransferException, InterruptedException {
    MutexState mutexState = AbstractStates.extractStateByType(wrappedState, MutexState.class);
    if (mutexState != null) {
      mutexState.addEdgePids(new HashMap<>(ImmutableMap.of(edge, pid)));
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
  private static @Nullable String calledFunctionName(CFAEdge edge) {
    if (edge instanceof AStatementEdge statementEdge
        && statementEdge.getStatement() instanceof AFunctionCall call
        && call.getFunctionCallExpression().getFunctionNameExpression()
            instanceof AIdExpression functionName) {
      return functionName.getName();
    }
    return null;
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
   * A synthetic {@code handleLvalue = id;} edge that writes a fresh literal identifying {@code pid}
   * through the (arbitrary) lvalue naming the new thread's handle. Handed to the wrapped analysis
   * exactly like any other edge, so whatever it already supports for regular lvalues (array
   * elements, struct fields, ...) is all that is needed here — nothing POR-specific.
   */
  private static CFAEdge writeThreadHandleEdge(CExpression handle, int pid, CFAEdge edge)
      throws UnsupportedCodeException {
    CLeftHandSide lhs = threadHandleLvalue(handle, edge);
    CIntegerLiteralExpression rhs =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY, lhs.getExpressionType(), BigInteger.valueOf((long) pid + 1));
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
   * <p>Without this, privatizing a {@code __thread} variable (see {@link PorAstCloner}) would make
   * things worse rather than better: the child's copy is a variable no edge on its path ever
   * assigns, so the wrapped analysis treats it as indeterminate and an {@code assert(data == 0)}
   * fails on a value the child can never actually hold. Only the main thread's clone contains the
   * file-scope declaration edge that carries the initializer; a spawned thread enters at its start
   * routine and never passes it.
   *
   * <p>The assignments happen on the <b>creating</b> thread's edge (as {@code pid}'s bookkeeping),
   * which is sound precisely because the target is private to the child: no other thread can
   * observe it, so where in the schedule the write lands cannot matter.
   */
  private List<AbstractState> initializeThreadLocals(
      PORPrecision precision, List<AbstractState> pStates, int newPid, CFAEdge cfaEdge, int pid)
      throws CPATransferException, InterruptedException {
    List<AbstractState> states = pStates;
    for (CVariableDeclaration threadLocal : threadLocalGlobals) {
      CFAEdge initEdge = threadLocalInitEdge(threadLocal, newPid, cfaEdge);
      List<AbstractState> next = new ArrayList<>(states.size());
      for (AbstractState wrapped : states) {
        next.addAll(applyBookkeepingEdge(precision, wrapped, initEdge, pid));
      }
      states = next;
    }
    return states;
  }

  /**
   * A synthetic {@code T{newPid}_startRoutine::param = <4th pthread_create argument>;} edge binding
   * the spawned thread's start-routine parameter to the argument the creator passes. Without it the
   * child's parameter is a variable no edge on any path ever assigns, so the wrapped analysis
   * treats it as indeterminate — sound, but any property that hinges on the pointed-to data (e.g.
   * {@code *((int *) arg)} staying in a known range) then admits spurious counterexamples, observed
   * as wrong FALSE verdicts on tasks passing {@code &local} through {@code pthread_create}
   * (goblint-regression escape_* / container_of families).
   *
   * <p>The argument expression comes from the creator's already-cloned call edge, so it is
   * evaluated in the creator's namespace — exactly where the concrete semantics evaluate it. The
   * parameter is private to the child, so writing it on the creating thread's bookkeeping edge is
   * sound for the same reason as {@link #threadLocalInitEdge}. Returns {@code null} when the start
   * routine declares no parameter (binding is then meaningless and the old behavior — an unbound
   * parameter — remains).
   */
  private @Nullable CFAEdge threadArgumentInitEdge(
      String pThreadFunc, CExpression pArgument, int pNewPid, CFAEdge pEdge) {
    FunctionEntryNode entry = cfa.getFunctionHead(pThreadFunc);
    if (entry == null || entry.getFunctionParameters().isEmpty()) {
      return null;
    }
    if (!(entry.getFunctionParameters().get(0) instanceof CParameterDeclaration origParam)) {
      return null;
    }
    CParameterDeclaration childParam =
        new CParameterDeclaration(
            origParam.getFileLocation(), origParam.getType(), origParam.getName());
    childParam.setQualifiedName(
        ThreadFunctions.perThreadName(pNewPid, origParam.getQualifiedName()));
    CIdExpression lhs =
        new CIdExpression(
            origParam.getFileLocation(), origParam.getType(), origParam.getName(), childParam);
    CExpression rhs = pArgument;
    if (!rhs.getExpressionType()
        .getCanonicalType()
        .equals(origParam.getType().getCanonicalType())) {
      rhs = new CCastExpression(FileLocation.DUMMY, origParam.getType(), pArgument);
    }
    return new CStatementEdge(
        "",
        new CExpressionAssignmentStatement(FileLocation.DUMMY, lhs, rhs),
        FileLocation.DUMMY,
        pEdge.getPredecessor(),
        pEdge.getSuccessor());
  }

  /**
   * A synthetic {@code T{pid}_x = <initial value>;} edge for one {@code __thread} variable. The
   * left-hand side is built as the very declaration {@link PorAstCloner} renames {@code pDecl} to
   * for thread {@code pid} — same qualified name, same non-global flag — so that this write and the
   * child's own reads of the variable are the same symbol to the wrapped analysis.
   */
  private CFAEdge threadLocalInitEdge(CVariableDeclaration pDecl, int pid, CFAEdge edge)
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
            ThreadFunctions.perThreadName(pid, pDecl.getQualifiedName()),
            null);
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
      CExpression handle, int candidatePid, CFAEdge edge) {
    CIntegerLiteralExpression literal =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY,
            handle.getExpressionType(),
            BigInteger.valueOf((long) candidatePid + 1));
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

  PORState initial(AbstractState wrappedInitialState) {
    return addNewThreadNode(PORState.empty(wrappedInitialState, cfa, random), false, "main", null);
  }

  PORState addNewThread(
      final PORState old, final String functionName, @Nullable String handleName) {
    return addNewThreadNode(old, true, functionName, handleName);
  }

  private PORState addNewThreadNode(
      final PORState old,
      boolean addToLivePids,
      final String functionName,
      @Nullable String handleName) {
    CFANode functionCallNode =
        Preconditions.checkNotNull(
            cfa.getFunctionHead(functionName), "Function '%s' was not found.", functionName);

    // Compute the PID for the new thread so we can get its cloned entry node
    int newPid = old.threads().size();
    CFANode clonedEntryNode = PorEdgeCloner.getClonedNode(functionCallNode, newPid, cfa);

    CallstackState initialStack =
        (CallstackState)
            callstackCPA.getInitialState(
                clonedEntryNode, StateSpacePartition.getDefaultPartition());
    LocationState initialLoc =
        locationCPA.getInitialState(clonedEntryNode, StateSpacePartition.getDefaultPartition());

    return old.addNewThread(addToLivePids, handleName, initialLoc, initialStack);
  }
}
