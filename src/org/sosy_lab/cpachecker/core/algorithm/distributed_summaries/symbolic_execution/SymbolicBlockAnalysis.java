// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.symbolic_execution;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.logging.Level;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.ExistentialProjection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.PredicateOperatorUtil;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.symbolic_execution.SymbolicBlockExecutionOptions.QuantifierElimination;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.QuantifiedFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.QuantifiedFormulaManager.Quantifier;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.Tactic;
import org.sosy_lab.java_smt.api.visitors.DefaultFormulaVisitor;
import org.sosy_lab.java_smt.api.visitors.TraversalProcess;

/**
 * The formula operations of symbolic block execution, bound to one solver context.
 *
 * <p>A block is summarized by one forward path formula from its initial to its final location,
 * encoded exactly like the predicate analysis encodes a block of large-block encoding: edge by edge
 * with the {@link PathFormulaManagerImpl} and joined with {@link PathFormulaManager#makeOr} where
 * paths meet. SSA index 1 then denotes the value of a symbol at the block entry, and the index in
 * the SSA map of the path formula the value at the block exit, because {@code makeOr} aligns both
 * branches of a join to the higher index.
 *
 * <p>The conditions that blocks exchange are in <em>interface form</em>: every program symbol
 * carries SSA index 1 and stands for its value at the boundary the condition describes, i.e., the
 * entry of the receiving block for postconditions and its exit for violation conditions. Every
 * other symbol is existentially quantified. Such symbols are projected away where possible, and
 * otherwise carry a private name that is unique in the whole analysis, so that two conditions never
 * share an existential symbol by accident.
 *
 * <p>Instances are not thread-safe; every thread owns one, together with its solver context.
 * Formulas move between contexts as SMT-LIB strings.
 */
public final class SymbolicBlockAnalysis implements AutoCloseable {

  /** The SSA index of every program symbol in interface form. */
  private static final int INTERFACE_INDEX = 1;

  /**
   * Prefix of the private names of existential symbols. It contains the character {@code !}, so
   * {@link PredicateOperatorUtil#isFreshPerUse(String)} treats such symbols as fresh, too.
   */
  private static final String PRIVATE_PREFIX = "__sbe!";

  /** The summary of a block: its transition to the block end and its paths to a target. */
  record BlockEncoding(Optional<PathFormula> transition, Optional<PathFormula> pathsToTarget) {}

  /** A location in the block together with the calls entered in the block that are still open. */
  private record Point(
      CFANode location, ImmutableList<FunctionSummaryEdge> calls, boolean isStart) {}

  private record Step(CFAEdge edge, Point successor) {}

  private final Solver solver;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final PathFormulaManager pathFormulaManager;
  private final ExistentialProjection projection;
  private final @Nullable QuantifiedFormulaManagerView quantifiers;
  private final SymbolicBlockExecutionOptions options;
  private final SymbolicBlockExecutionStatistics statistics;
  private final AtomicLong privateNames;
  private final ShutdownNotifier shutdownNotifier;
  private final LogManager logger;

  /**
   * The shutdown requests for the thread that owns this analysis, which are forwarded to the solver
   * only while it is open.
   *
   * <p>A solver turns a shutdown request into an interrupt of its native context. If the request
   * reached the solver directly, it could interrupt the context while the owning thread deletes it
   * in {@link #close()}, which crashes Z3. Forwarding and closing therefore exclude each other.
   */
  private final ShutdownNotifier requests;

  private final ShutdownRequestListener forwardRequest;
  private final Object solverLock = new Object();

  @GuardedBy("solverLock")
  private boolean closed;

  /**
   * Creates the solver context of one thread.
   *
   * @param pPrivateNames shared by all instances of one analysis, so that private names are unique
   *     across solver contexts
   */
  SymbolicBlockAnalysis(
      Configuration pConfiguration,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      SymbolicBlockExecutionOptions pOptions,
      SymbolicBlockExecutionStatistics pStatistics,
      AtomicLong pPrivateNames)
      throws InvalidConfigurationException {
    logger = pLogger;
    options = pOptions;
    statistics = pStatistics;
    privateNames = pPrivateNames;
    requests = pShutdownNotifier;
    ShutdownManager solverShutdown = ShutdownManager.create();
    shutdownNotifier = solverShutdown.getNotifier();
    solver = Solver.create(pConfiguration, pLogger, shutdownNotifier);
    try {
      fmgr = solver.getFormulaManager();
      bfmgr = fmgr.getBooleanFormulaManager();
      pathFormulaManager =
          new PathFormulaManagerImpl(
              fmgr, pConfiguration, pLogger, shutdownNotifier, pCfa, AnalysisDirection.FORWARD);
      projection = new ExistentialProjection(solver, true);
      quantifiers = pOptions.useQuantifiers() ? quantifiersIfSupported(fmgr) : null;
    } catch (InvalidConfigurationException | RuntimeException e) {
      solver.close();
      throw e;
    }
    forwardRequest =
        reason -> {
          synchronized (solverLock) {
            if (!closed) {
              solverShutdown.requestShutdown(reason);
            }
          }
        };
    requests.registerAndCheckImmediately(forwardRequest);
  }

  private static @Nullable QuantifiedFormulaManagerView quantifiersIfSupported(
      FormulaManagerView pFmgr) {
    try {
      return pFmgr.getQuantifiedFormulaManager();
    } catch (UnsupportedOperationException e) {
      return null;
    }
  }

  BooleanFormula parse(String pFormula) {
    return fmgr.parse(pFormula);
  }

  String dump(BooleanFormula pFormula) {
    return fmgr.dumpFormula(pFormula).toString();
  }

  BooleanFormula makeTrue() {
    return bfmgr.makeTrue();
  }

  boolean isFalse(BooleanFormula pFormula) {
    return bfmgr.isFalse(pFormula);
  }

  BooleanFormula or(List<BooleanFormula> pFormulas) {
    return bfmgr.or(pFormulas);
  }

  boolean isSatisfiable(BooleanFormula... pConjuncts) throws SolverException, InterruptedException {
    shutdownNotifier.shutdownIfNecessary();
    statistics.satisfiabilityChecks.increment();
    return !solver.isUnsat(bfmgr.and(pConjuncts));
  }

  /**
   * Encodes the block as path formulas: the transition from the block entry to the block end, and
   * all paths from the block entry to a target location of the block.
   *
   * <p>The block is unfolded from its initial location along its own edges. A point of the
   * unfolding is a location together with the calls entered inside the block that have not returned
   * yet, so a return edge only continues a path whose last open call it belongs to. Returns out of
   * the function the block starts in are not restricted, which is precise because the inlining
   * decomposition gives every block exactly one calling context. The final location and the target
   * locations end a path. Paths do not continue after reaching a target, because the program
   * violates the specification there already.
   *
   * @throws CPAException if the block contains a cycle that does not pass its final location,
   *     because such a block has no finite path formula
   */
  BlockEncoding encode(BlockNode pBlock, ImmutableSet<CFANode> pTargets)
      throws CPAException, InterruptedException {
    long start = statistics.encodingTime.start();
    BlockEncoding encoding;
    try {
      encoding = unfold(pBlock, pTargets);
    } finally {
      statistics.encodingTime.stop(start);
    }
    for (PathFormula paths :
        Stream.of(encoding.transition(), encoding.pathsToTarget())
            .flatMap(Optional::stream)
            .toList()) {
      if (accessesMemory(paths)) {
        throw new CPAException(
            "Symbolic block execution does not support programs that access memory through"
                + " pointers or arrays yet, but block "
                + pBlock.getId()
                + " does.");
      }
    }
    return encoding;
  }

  /**
   * Whether a path formula accesses memory in the pointer-aliasing encoding.
   *
   * <p>That encoding decides along a path whether a variable lives in memory, based on the
   * pointer-target set built up on the path so far, and names dynamic allocations by a counter of
   * that set. A block encoded on its own starts with an empty set, so two blocks could encode the
   * same variable differently or give two allocations the same address. Such conditions can be
   * wrong in either direction. Until the pointer-target set travels with the conditions, like it
   * does in DSS, blocks that access memory are rejected rather than risking a wrong verdict.
   */
  private boolean accessesMemory(PathFormula pPaths) {
    return !pPaths.getPointerTargetSet().getBases().isEmpty()
        || fmgr.extractFunctionNames(pPaths.getFormula()).stream()
            .anyMatch(symbol -> symbol.startsWith("*") || symbol.contains("__ADDRESS_OF_"));
  }

  private BlockEncoding unfold(BlockNode pBlock, ImmutableSet<CFANode> pTargets)
      throws CPAException, InterruptedException {
    Point start = new Point(pBlock.getInitialLocation(), ImmutableList.of(), true);
    Map<Point, List<Step>> steps = new HashMap<>();
    List<Point> postorder = new ArrayList<>();
    Set<Point> finished = new HashSet<>();
    Set<Point> onStack = new HashSet<>();
    Deque<Point> stack = new ArrayDeque<>();
    Deque<Iterator<Step>> pending = new ArrayDeque<>();

    // iterative depth-first search, which also finds the cycles
    List<Step> startSteps = stepsOf(start, pBlock, pTargets);
    steps.put(start, startSteps);
    stack.push(start);
    onStack.add(start);
    pending.push(startSteps.iterator());
    while (!stack.isEmpty()) {
      shutdownNotifier.shutdownIfNecessary();
      Iterator<Step> next = pending.element();
      if (next.hasNext()) {
        Point successor = next.next().successor();
        if (onStack.contains(successor)) {
          throw new CPAException(
              String.format(
                  "Block %s has a cycle through %s that does not pass its final location %s, but"
                      + " symbolic block execution requires loop-free blocks.",
                  pBlock.getId(), successor.location(), pBlock.getFinalLocation()));
        }
        if (!finished.contains(successor)) {
          List<Step> successorSteps = stepsOf(successor, pBlock, pTargets);
          steps.put(successor, successorSteps);
          stack.push(successor);
          onStack.add(successor);
          pending.push(successorSteps.iterator());
        }
      } else {
        Point done = stack.pop();
        pending.pop();
        onStack.remove(done);
        finished.add(done);
        postorder.add(done);
      }
    }

    // In reverse postorder, every point comes after all points with a step to it.
    Map<Point, PathFormula> formulas = new HashMap<>();
    formulas.put(start, pathFormulaManager.makeEmptyPathFormula());
    PathFormula transition = null;
    PathFormula pathsToTarget = null;
    for (Point point : Lists.reverse(postorder)) {
      PathFormula formula = formulas.remove(point);
      if (!point.isStart()) {
        if (pTargets.contains(point.location())) {
          pathsToTarget = orNullable(pathsToTarget, formula);
        } else if (point.location().equals(pBlock.getFinalLocation())) {
          transition = orNullable(transition, formula);
        }
      }
      for (Step step : steps.get(point)) {
        PathFormula extended = pathFormulaManager.makeAnd(formula, step.edge());
        formulas.put(step.successor(), orNullable(formulas.get(step.successor()), extended));
      }
    }
    return new BlockEncoding(Optional.ofNullable(transition), Optional.ofNullable(pathsToTarget));
  }

  private PathFormula orNullable(@Nullable PathFormula pFirst, PathFormula pSecond)
      throws InterruptedException {
    return pFirst == null ? pSecond : pathFormulaManager.makeOr(pFirst, pSecond);
  }

  private static List<Step> stepsOf(
      Point pPoint, BlockNode pBlock, ImmutableSet<CFANode> pTargets) {
    CFANode location = pPoint.location();
    if (!pPoint.isStart()
        && (location.equals(pBlock.getFinalLocation()) || pTargets.contains(location))) {
      return ImmutableList.of();
    }
    ImmutableList.Builder<Step> steps = ImmutableList.builder();
    for (CFAEdge edge : location.getLeavingEdges()) {
      if (!pBlock.getEdges().contains(edge)
          || edge.getDescription().equals(BlockGraph.GHOST_EDGE_DESCRIPTION)) {
        continue;
      }
      ImmutableList<FunctionSummaryEdge> calls = pPoint.calls();
      if (edge instanceof FunctionCallEdge call) {
        calls =
            ImmutableList.<FunctionSummaryEdge>builder()
                .addAll(calls)
                .add(call.getSummaryEdge())
                .build();
      } else if (edge instanceof FunctionReturnEdge returnEdge && !calls.isEmpty()) {
        if (!calls.getLast().equals(returnEdge.getSummaryEdge())) {
          continue;
        }
        calls = calls.subList(0, calls.size() - 1);
      }
      steps.add(new Step(edge, new Point(edge.getSuccessor(), calls, false)));
    }
    return steps.build();
  }

  /**
   * The strongest postcondition of the block for the given precondition, in interface form for the
   * successors of the block.
   *
   * @param pPrecondition a condition in interface form for the entry of the block
   * @param pTransition the transition of the block, see {@link #encode}
   * @param pExitSsa the SSA map of the transition
   */
  BooleanFormula postcondition(
      BooleanFormula pPrecondition, BooleanFormula pTransition, SSAMap pExitSsa)
      throws SolverException, InterruptedException {
    return toInterface(bfmgr.and(pPrecondition, pTransition), exitIndices(pExitSsa));
  }

  /**
   * The states at the entry of the block from which the block can reach a state at its end that
   * satisfies the given violation condition (the existential preimage of the condition, i.e., the
   * negated weakest liberal precondition of its negation), in interface form for the predecessors
   * of the block.
   *
   * @param pTransition the transition of the block, see {@link #encode}
   * @param pExitSsa the SSA map of the transition
   * @param pViolationCondition a condition in interface form for the end of the block
   */
  BooleanFormula preimage(
      BooleanFormula pTransition, SSAMap pExitSsa, BooleanFormula pViolationCondition)
      throws SolverException, InterruptedException {
    return toInterface(
        bfmgr.and(pTransition, atExit(pViolationCondition, pExitSsa)),
        SymbolicBlockAnalysis::entryIndex);
  }

  /**
   * The states at the entry of the block from which a path of the given path formula starts, in
   * interface form for the predecessors of the block.
   */
  BooleanFormula entryCondition(PathFormula pPaths) throws SolverException, InterruptedException {
    return toInterface(pPaths.getFormula(), SymbolicBlockAnalysis::entryIndex);
  }

  /**
   * Renames a condition in interface form for the end of a block such that it talks about the
   * symbols of the transition of the block at its exit indices. Existential symbols keep their
   * private names, which no transition uses.
   */
  BooleanFormula atExit(BooleanFormula pInterfaceFormula, SSAMap pExitSsa) {
    ToIntFunction<String> exitIndices = exitIndices(pExitSsa);
    return fmgr.renameFreeVariablesAndUFs(
        pInterfaceFormula,
        symbol -> {
          Pair<String, OptionalInt> parsed = FormulaManagerView.parseName(symbol);
          if (parsed.getSecond().isEmpty()) {
            return symbol;
          }
          return instantiated(parsed.getFirst(), exitIndices.applyAsInt(parsed.getFirst()));
        });
  }

  private static ToIntFunction<String> exitIndices(SSAMap pExitSsa) {
    return name -> pExitSsa.containsVariable(name) ? pExitSsa.getIndex(name) : INTERFACE_INDEX;
  }

  /** Every program symbol has the interface index at the block entry. */
  private static int entryIndex(@SuppressWarnings("unused") String pName) {
    return INTERFACE_INDEX;
  }

  private static String instantiated(String pName, int pIndex) {
    return pName + FormulaManagerView.INDEX_SEPARATOR + pIndex;
  }

  /**
   * Whether a symbol is existentially quantified in a formula that describes the boundary at which
   * every program symbol has the given index: nondeterministic values, private symbols, and all
   * values of program symbols at other indices.
   */
  private static boolean isExistential(String pSymbol, ToIntFunction<String> pBoundaryIndices) {
    if (PredicateOperatorUtil.isFreshPerUse(pSymbol)) {
      return true;
    }
    Pair<String, OptionalInt> parsed = FormulaManagerView.parseName(pSymbol);
    return parsed.getSecond().isPresent()
        && parsed.getSecond().orElseThrow() != pBoundaryIndices.applyAsInt(parsed.getFirst());
  }

  /**
   * Brings a formula over SSA-indexed symbols into interface form for the boundary at which every
   * program symbol has the index that {@code pBoundaryIndices} assigns to it.
   *
   * <p>First, the existential symbols are eliminated as far as {@link ExistentialProjection} can
   * without changing the meaning of the formula, then, if enabled, by quantifier elimination of the
   * solver. The remaining existential symbols get fresh private names, and the boundary symbols get
   * the interface index. Both renamings happen at once, so a boundary symbol never captures an
   * existential one that had the same name.
   */
  private BooleanFormula toInterface(
      BooleanFormula pFormula, ToIntFunction<String> pBoundaryIndices)
      throws SolverException, InterruptedException {
    shutdownNotifier.shutdownIfNecessary();
    Predicate<String> isExistential = symbol -> isExistential(symbol, pBoundaryIndices);
    long start = statistics.projectionTime.start();
    BooleanFormula projected;
    try {
      projected = projection.project(pFormula, isExistential);
    } finally {
      statistics.projectionTime.stop(start);
    }
    projected = eliminateRemaining(projected, isExistential);
    Map<String, String> renaming = new LinkedHashMap<>();
    for (String symbol : fmgr.extractFunctionNames(projected)) {
      if (isExistential.test(symbol)) {
        renaming.put(symbol, freshPrivateName(symbol));
      } else {
        Pair<String, OptionalInt> parsed = FormulaManagerView.parseName(symbol);
        if (parsed.getSecond().isPresent()) {
          renaming.put(symbol, instantiated(parsed.getFirst(), INTERFACE_INDEX));
        }
      }
    }
    if (renaming.keySet().stream().anyMatch(isExistential)) {
      statistics.conditionsWithExistentials.increment();
    }
    return fmgr.renameFreeVariablesAndUFs(
        projected, symbol -> renaming.getOrDefault(symbol, symbol));
  }

  private String freshPrivateName(String pSymbol) {
    String original =
        pSymbol.startsWith(PRIVATE_PREFIX)
            ? pSymbol.substring(pSymbol.indexOf('#') + 1)
            : pSymbol.replace(FormulaManagerView.INDEX_SEPARATOR, '!');
    return PRIVATE_PREFIX + privateNames.getAndIncrement() + "#" + original;
  }

  /**
   * Eliminates the existential variables that the projection kept with the quantifier elimination
   * of the solver, as far as enabled and supported. Variables that the solver leaves bound by an
   * existential quantifier at the top of the result become free again, i.e., private variables of
   * the condition. If the solver fails or leaves any other quantifier, the formula stays as it was.
   * Uninterpreted functions cannot be quantified and always stay.
   */
  private BooleanFormula eliminateRemaining(
      BooleanFormula pFormula, Predicate<String> pIsExistential) throws InterruptedException {
    if (quantifiers == null || options.quantifierElimination() == QuantifierElimination.NONE) {
      return pFormula;
    }
    List<Formula> remaining = existentialVariables(pFormula, pIsExistential);
    if (remaining.isEmpty()) {
      return pFormula;
    }
    long start = statistics.quantifierEliminationTime.start();
    try {
      BooleanFormula quantified = quantifiers.exists(remaining, pFormula);
      BooleanFormula eliminated =
          switch (options.quantifierElimination()) {
            case LIGHT -> fmgr.applyTactic(quantified, Tactic.QE_LIGHT);
            case FULL -> quantifiers.eliminateQuantifiers(quantified);
            case NONE -> throw new AssertionError("handled above");
          };
      Optional<BooleanFormula> quantifierFree = withoutOuterExistential(eliminated);
      if (quantifierFree.isEmpty()) {
        statistics.failedQuantifierEliminations.increment();
        return pFormula;
      }
      statistics.quantifierEliminations.increment();
      return quantifierFree.orElseThrow();
    } catch (SolverException | UnsupportedOperationException e) {
      statistics.failedQuantifierEliminations.increment();
      logger.logDebugException(e, "Quantifier elimination failed, keeping existential variables");
      return pFormula;
    } finally {
      statistics.quantifierEliminationTime.stop(start);
    }
  }

  /**
   * The given formula without an existential quantifier at its top, whose variables become free.
   * This preserves satisfiability and, for a condition, its meaning, because all free variables
   * that are not interface symbols are read existentially. Empty if any quantifier remains.
   */
  private Optional<BooleanFormula> withoutOuterExistential(BooleanFormula pFormula) {
    BooleanFormula body =
        fmgr.visit(
            pFormula,
            new DefaultFormulaVisitor<BooleanFormula>() {
              @Override
              protected BooleanFormula visitDefault(Formula pF) {
                return pFormula;
              }

              @Override
              public BooleanFormula visitQuantifier(
                  BooleanFormula pF,
                  Quantifier pQuantifier,
                  List<Formula> pBoundVariables,
                  BooleanFormula pBody) {
                return pQuantifier == Quantifier.EXISTS ? pBody : pFormula;
              }
            });
    return containsQuantifier(body) ? Optional.empty() : Optional.of(body);
  }

  private List<Formula> existentialVariables(
      BooleanFormula pFormula, Predicate<String> pIsExistential) {
    ImmutableList.Builder<Formula> variables = ImmutableList.builder();
    for (Entry<String, Formula> variable : fmgr.extractVariables(pFormula).entrySet()) {
      if (pIsExistential.test(variable.getKey())) {
        variables.add(variable.getValue());
      }
    }
    return variables.build();
  }

  private boolean containsQuantifier(BooleanFormula pFormula) {
    AtomicBoolean found = new AtomicBoolean();
    fmgr.visitRecursively(
        pFormula,
        new DefaultFormulaVisitor<TraversalProcess>() {
          @Override
          protected TraversalProcess visitDefault(Formula pF) {
            return TraversalProcess.CONTINUE;
          }

          @Override
          public TraversalProcess visitQuantifier(
              BooleanFormula pF,
              Quantifier pQuantifier,
              List<Formula> pBoundVariables,
              BooleanFormula pBody) {
            found.set(true);
            return TraversalProcess.ABORT;
          }
        });
    return found.get();
  }

  /**
   * A known condition in interface form prepared for {@link #isCovered}: its private variables are
   * existentially quantified if quantifiers are enabled and supported.
   *
   * <p>A private variable must be quantified before the condition is negated: free, it would be
   * read existentially for the whole query, and {@code new ∧ ¬known} would ask whether some value
   * of it falsifies the known condition, which almost always holds. A coverage check would then
   * never succeed and the analysis would not terminate on loops.
   */
  BooleanFormula closeForCoverage(BooleanFormula pKnown) {
    if (quantifiers == null || !options.quantifiedCoverage()) {
      return pKnown;
    }
    List<Formula> privateVariables =
        existentialVariables(pKnown, PredicateOperatorUtil::isFreshPerUse);
    return privateVariables.isEmpty() ? pKnown : quantifiers.exists(privateVariables, pKnown);
  }

  /**
   * Whether every state of the new condition is a state of one of the known conditions, i.e.,
   * whether the condition adds no states. All conditions are in interface form for the same
   * boundary, and the known ones were prepared by {@link #closeForCoverage}. If the solver cannot
   * decide the query, the condition counts as new, which only costs exploration.
   */
  boolean isCovered(BooleanFormula pCondition, List<BooleanFormula> pKnown)
      throws InterruptedException {
    shutdownNotifier.shutdownIfNecessary();
    statistics.coverageChecks.increment();
    long start = statistics.coverageTime.start();
    try {
      return solver.isUnsat(bfmgr.and(pCondition, bfmgr.not(bfmgr.or(pKnown))));
    } catch (SolverException e) {
      logger.logDebugException(e, "Coverage check failed, treating the condition as new");
      logger.log(Level.FINE, "Coverage check failed, treating the condition as new");
      return false;
    } finally {
      statistics.coverageTime.stop(start);
    }
  }

  @Override
  public void close() {
    requests.unregister(forwardRequest);
    synchronized (solverLock) {
      closed = true;
      solver.close();
    }
  }
}
