// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.concurrent;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.common.collect.Collections3.elementAndList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.overflow.OverflowState;
import org.sosy_lab.cpachecker.cpa.predicate.BAMBlockFormulaStrategy;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

/** Encodes ARG choices explicitly and replays the selected schedule with per-thread call stacks. */
final class ConcurrentPathChecker extends PathChecker {
  private record Context(
      PathFormula formula, ImmutableMap<Integer, ImmutableList<SSAMap>> stacks) {}

  private record Choice(ARGState parent, BooleanFormula selector) {}

  private final PathFormulaManager formulas;
  private final Solver solver;
  private final BooleanFormulaManagerView booleans;
  private final ShutdownNotifier shutdown;
  private final ConcurrentTransferRelation transfer;
  private long nextSelector;

  ConcurrentPathChecker(
      PredicateCPA predicate, ConcurrentTransferRelation pTransfer, LogManager logger)
      throws InvalidConfigurationException {
    super(
        predicate.getConfiguration(),
        logger,
        predicate.getShutdownNotifier(),
        predicate.getCfa().getMachineModel(),
        predicate.getPathFormulaManager(),
        predicate.getSolver());
    formulas = predicate.getPathFormulaManager();
    solver = predicate.getSolver();
    booleans = solver.getFormulaManager().getBooleanFormulaManager();
    shutdown = predicate.getShutdownNotifier();
    transfer = pTransfer;
  }

  Optional<ARGPath> findSchedule(ARGPath fallback)
      throws CPATransferException, SolverException, InterruptedException {
    ARGState root = fallback.getFirstState();
    ARGState target = fallback.getLastState();
    if (!solver.isUnsat(createPrecisePathFormula(fallback).getFirst().getFormula())) {
      return Optional.of(fallback);
    }
    Set<ARGState> relevant = ARGUtils.getAllStatesOnPathsTo(target);
    if (relevant.stream().allMatch(state -> state.getParents().size() <= 1)) {
      return Optional.empty();
    }
    Map<ARGState, Integer> remainingParents = new HashMap<>();
    Map<ARGState, Context> contexts = new HashMap<>();
    Map<ARGState, List<Choice>> choices = new HashMap<>();
    for (ARGState state : relevant) {
      remainingParents.put(
          state, (int) state.getParents().stream().filter(relevant::contains).count());
    }
    contexts.put(
        root,
        new Context(addAssumptions(formulas.makeEmptyPathFormula(), root), ImmutableMap.of()));
    ArrayDeque<ARGState> pending = new ArrayDeque<>();
    pending.add(root);
    int processed = 0;
    while (!pending.isEmpty()) {
      shutdown.shutdownIfNecessary();
      ARGState state = pending.removeFirst();
      processed++;
      if (state != root) {
        Context merged = null;
        List<Choice> incoming = new ArrayList<>();
        for (ARGState parent : state.getParents()) {
          if (!relevant.contains(parent)) {
            continue;
          }
          Context next = advance(checkNotNull(contexts.get(parent)), parent, state, null);
          BooleanFormula selector =
              booleans.makeVariable("__CPAchecker_concurrent_schedule_" + nextSelector++);
          incoming.add(new Choice(parent, selector));
          if (merged == null) {
            merged = next;
          } else {
            if (!merged.stacks().equals(next.stacks())) {
              throw new CPATransferException(
                  "Incompatible call contexts in a merged concurrent ARG");
            }
            // Opposite guards retain the choice between the incoming histories.
            PathFormula joined =
                formulas.makeOr(
                    formulas.makeAnd(next.formula(), selector),
                    formulas.makeAnd(merged.formula(), booleans.not(selector)));
            merged = new Context(joined, next.stacks());
          }
        }
        contexts.put(state, checkNotNull(merged));
        choices.put(state, incoming);
      }
      for (ARGState child : state.getChildren()) {
        if (relevant.contains(child)) {
          int remaining = checkNotNull(remainingParents.get(child)) - 1;
          remainingParents.put(child, remaining);
          if (remaining == 0) {
            pending.add(child);
          }
        }
      }
    }
    if (processed != relevant.size()) {
      throw new CPATransferException("Schedule reconstruction requires an acyclic ARG");
    }
    try (ProverEnvironment prover = solver.newProverEnvironment()) {
      prover.push(checkNotNull(contexts.get(target)).formula().getFormula());
      if (prover.isUnsat()) {
        return Optional.empty();
      }
      List<ARGState> path = new ArrayList<>();
      ARGState state = target;
      while (state != root) {
        shutdown.shutdownIfNecessary();
        path.add(state);
        List<Choice> incoming = checkNotNull(choices.get(state));
        ARGState parent = incoming.getFirst().parent();
        for (int i = incoming.size() - 1; i > 0; i--) {
          Choice choice = incoming.get(i);
          prover.push(choice.selector());
          if (!prover.isUnsat()) {
            parent = choice.parent();
            break;
          }
          prover.pop();
          prover.addConstraint(booleans.not(choice.selector()));
        }
        state = parent;
      }
      path.add(root);
      Collections.reverse(path);
      return Optional.of(new ARGPath(path));
    }
  }

  CounterexampleInfo checkSchedule(ARGPath path)
      throws CPATransferException, SolverException, InterruptedException {
    PathFormula replayed = createPrecisePathFormula(path).getFirst();
    if (solver.isUnsat(replayed.getFormula())) {
      throw new CPATransferException("The reconstructed concurrent schedule is infeasible");
    }
    return handleFeasibleCounterexample(
        CounterexampleTraceInfo.feasible(ImmutableList.of(replayed.getFormula()), path), path);
  }

  @Override
  protected Pair<PathFormula, List<SSAMap>> createPrecisePathFormula(ARGPath path)
      throws CPATransferException, InterruptedException {
    List<SSAMap> ssaMaps = new ArrayList<>();
    Context context =
        new Context(
            addAssumptions(formulas.makeEmptyPathFormula(), path.getFirstState()),
            ImmutableMap.of());
    List<ARGState> states = path.asStatesList();
    for (int i = 1; i < states.size(); i++) {
      shutdown.shutdownIfNecessary();
      context = advance(context, states.get(i - 1), states.get(i), ssaMaps);
    }
    return Pair.of(context.formula(), ssaMaps);
  }

  private Context advance(
      Context previous, ARGState parent, ARGState child, @Nullable List<SSAMap> ssaMaps)
      throws CPATransferException, InterruptedException {
    ConcurrentState before =
        checkNotNull(AbstractStates.extractStateByType(parent, ConcurrentState.class));
    ConcurrentState after =
        checkNotNull(AbstractStates.extractStateByType(child, ConcurrentState.class));
    PathFormula formula = previous.formula();
    Map<Integer, ImmutableList<SSAMap>> stacks = new HashMap<>(previous.stacks());
    List<CFAEdge> edges = parent.getEdgesToChild(child);
    if (edges.isEmpty()) {
      throw new CPATransferException("Concurrent ARG transition has no CFA edges");
    }
    for (CFAEdge edge : edges) {
      int threadId = edge.getPredecessor().getCloneThreadId().orElseThrow();
      ImmutableList<SSAMap> stack = stacks.getOrDefault(threadId, ImmutableList.of());
      if (edge instanceof FunctionCallEdge) {
        SSAMap.SSAMapBuilder locals = SSAMap.emptySSAMap().builder();
        for (String variable : formula.getSsa().allVariables()) {
          if (isThreadLocal(variable, threadId)) {
            locals.setIndex(
                variable, formula.getSsa().getType(variable), formula.getSsa().getIndex(variable));
          }
        }
        stacks.put(threadId, elementAndList(locals.build(), stack));
      } else if (edge instanceof FunctionReturnEdge returning) {
        if (stack.isEmpty()) {
          throw new CPATransferException("Concurrent function return has no matching call");
        }
        SSAMap current = formula.getSsa();
        formula =
            BAMBlockFormulaStrategy.rebuildStateAfterFunctionCall(
                formula,
                formulas.makeEmptyPathFormulaWithContext(
                    stack.getFirst(), formula.getPointerTargetSet()),
                returning.getPredecessor());
        SSAMap.SSAMapBuilder restored = formula.getSsa().builder();
        // A return must not restore another thread's local variables.
        for (String variable : current.allVariables()) {
          if (!isThreadLocal(variable, threadId)) {
            restored.deleteVariable(variable);
            restored.setIndex(variable, current.getType(variable), current.getIndex(variable));
          }
        }
        formula = formula.withContext(restored.build(), formula.getPointerTargetSet());
        if (stack.size() == 1) {
          stacks.remove(threadId);
        } else {
          stacks.put(threadId, stack.subList(1, stack.size()));
        }
      }
      formula = formulas.makeAnd(formula, edge);
      for (CFAEdge bookkeeping : transfer.getBookkeepingEdges(before, after, edge)) {
        formula = formulas.makeAnd(formula, bookkeeping);
      }
      if (ssaMaps != null) {
        ssaMaps.add(formula.getSsa());
      }
    }
    stacks.keySet().retainAll(after.threads().keySet());
    formula = addAssumptions(formula, child);
    return new Context(formula, ImmutableMap.copyOf(stacks));
  }

  private static boolean isThreadLocal(String variable, int threadId) {
    return variable.startsWith(ThreadFunctions.perThreadNamePrefix(threadId))
        && variable.contains("::");
  }

  private PathFormula addAssumptions(PathFormula formula, ARGState state)
      throws CPATransferException, InterruptedException {
    for (AbstractStateWithAssumptions component :
        AbstractStates.asIterable(state).filter(AbstractStateWithAssumptions.class)) {
      AbstractStateWithAssumptions assumptions = component;
      if (component instanceof OverflowState overflow && overflow.hasOverflow()) {
        assumptions = overflow.getParent();
      }
      for (AExpression expression : assumptions.getAssumptions()) {
        if (!(expression instanceof CExpression cExpression)) {
          throw new CPATransferException("Unsupported assumption in a concurrent counterexample");
        }
        formula = formulas.makeAnd(formula, cExpression);
      }
    }
    return formula;
  }
}
