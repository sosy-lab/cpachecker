// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.symbolic_execution;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.jspecify.annotations.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssExceptionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssResultMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssViolationConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssWitnessMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.symbolic_execution.SymbolicBlockAnalysis.AnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.symbolic_execution.SymbolicBlockAnalysis.BlockEncoding;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.symbolic_execution.SymbolicBlockAnalysis.SafeAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.symbolic_execution.SymbolicBlockAnalysis.UnsafeAnalysisResult;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * The symbolic execution of one block.
 *
 * <p>Symbolic execution runs in both directions at once. Forward, it starts at the root block with
 * all states and propagates the strongest postcondition of each block to its successors. Backward,
 * it starts at every block that contains a target location and propagates the existential preimage
 * of each block to its predecessors. A block stores every condition it receives, and both
 * directions meet in the block where a precondition and a violation condition intersect, which
 * proves the program unsafe. All conditions are exact, so this is never a false alarm. If no work
 * is left, every block has seen all states it can reach without a violation, which proves the
 * program safe.
 *
 * <p>A block reacts only to the states of a condition that its stored conditions do not contain
 * yet, and it propagates only these new states. A violation condition only travels on while its
 * block has not received a precondition yet; once it has, the forward direction has reached the
 * block and checks every later precondition against all stored violation conditions.
 *
 * <p>The conditions of a worker are also kept as SMT-LIB strings, so any thread can analyze the
 * block with its own {@link SymbolicBlockAnalysis}. The formulas parsed into the context of the
 * last analysis are cached, and are only parsed again when another analysis, e.g., another thread,
 * takes over. A worker is not thread-safe, but it may move between threads as long as accesses are
 * ordered.
 */
public final class SymbolicBlockWorker {

  static final String FORMULA_KEY = "formula";

  private final BlockNode blockNode;
  private final ImmutableSet<CFANode> targets;
  private final ImmutableSet<String> postconditionReceivers;

  /** See {@link SymbolicBlockExecutionOptions#subtractKnownStates()}. */
  private final boolean subtractKnownStates;

  private final DssMessageFactory dssMessageFactory;
  private final SymbolicBlockExecutionStatistics statistics;
  private final LogManager logger;

  /** The block formula in SMT-LIB, set by {@link #initialize}. */
  private @Nullable String blockFormula;

  /** The SSA indices of the block formula at the block end. */
  private SSAMap exitSsa = SSAMap.emptySSAMap();

  /**
   * The states at the block entry from which a path reaches a target location inside the block, in
   * SMT-LIB, or null if there are none.
   */
  private @Nullable String targetCondition;

  private final List<String> preconditions = new ArrayList<>();
  private final List<String> violationConditions = new ArrayList<>();

  private boolean shutdown;

  private @Nullable FormulasInContext formulas;

  /** The formulas of the worker parsed into the solver context of one analysis. */
  private static final class FormulasInContext {
    private final SymbolicBlockAnalysis analysis;
    private final BooleanFormula blockFormula;
    private final @Nullable BooleanFormula targetCondition;
    private final List<BooleanFormula> preconditions = new ArrayList<>();
    private final List<BooleanFormula> preconditionsForCoverage = new ArrayList<>();
    private final List<BooleanFormula> violationConditionsForCoverage = new ArrayList<>();
    private final List<BooleanFormula> violationConditionsAtExit = new ArrayList<>();

    private FormulasInContext(
        SymbolicBlockAnalysis pAnalysis,
        BooleanFormula pBlockFormula,
        @Nullable BooleanFormula pTargetCondition) {
      analysis = pAnalysis;
      blockFormula = pBlockFormula;
      targetCondition = pTargetCondition;
    }
  }

  /**
   * Creates the worker of a block.
   *
   * @param pBlockNode the block
   * @param pTargets all target locations of the program; the block finds the ones it reaches
   * @param pPostconditionReceivers the successors of the block that can reach a target, the only
   *     ones its postconditions matter for
   * @param pSubtractKnownStates whether to analyze and propagate only the states of a new condition
   *     that the known conditions do not contain
   */
  SymbolicBlockWorker(
      BlockNode pBlockNode,
      ImmutableSet<CFANode> pTargets,
      ImmutableSet<String> pPostconditionReceivers,
      boolean pSubtractKnownStates,
      DssMessageFactory pMessageFactory,
      SymbolicBlockExecutionStatistics pStatistics,
      LogManager pLogger) {
    blockNode = pBlockNode;
    targets = pTargets;
    postconditionReceivers = pPostconditionReceivers;
    dssMessageFactory = pMessageFactory;
    statistics = pStatistics;
    logger = pLogger;
    subtractKnownStates = pSubtractKnownStates;
  }

  String getBlockId() {
    return blockNode.getId();
  }

  /** The blocks that receive the postconditions of this block. */
  ImmutableSet<String> postconditionReceivers() {
    return postconditionReceivers;
  }

  /** The blocks that receive the violation conditions of this block. */
  ImmutableSet<String> violationConditionReceivers() {
    return blockNode.getPredecessorIds();
  }

  /** Whether a result or an exception arrived, after which the worker is not needed anymore. */
  boolean shutdownRequested() {
    return shutdown;
  }

  /**
   * Whether this block keeps violation conditions that a later precondition could still meet: it
   * has a precondition, so it does not send new violation conditions to its predecessors, see
   * {@link ProofTracker}. The precondition of the root contains all states already, so the root has
   * checked its violation conditions against everything that can arrive.
   */
  boolean blocksBackwardProof() {
    return !blockNode.isRoot() && !preconditions.isEmpty() && !violationConditions.isEmpty();
  }

  /**
   * Computes the block formula and starts the symbolic execution: the root block analyzes the
   * precondition that contains all states, and a block with a reachable target location sends the
   * states that reach it to its predecessors.
   *
   * <p>The block formula needs a solver, which only exists in the thread that analyzes the block,
   * so it is computed here and not in the constructor.
   */
  ImmutableList<DssMessage> initialize(SymbolicBlockAnalysis pAnalysis)
      throws CPAException, InterruptedException, SolverException {
    checkState(blockFormula == null, "Block %s is already initialized", blockNode.getId());
    computeBlockFormula(pAnalysis);
    if (blockNode.isRoot()) {
      // The program starts in every state at the entry of the root block.
      return analyzeNewPrecondition(pAnalysis, pAnalysis.initialStates());
    }
    if (targetCondition != null) {
      logger.log(Level.FINE, "Block", blockNode.getId(), "contains a reachable target location");
      statistics.violationConditionsSent.increment();
      return ImmutableList.of(violationConditionMessage(targetCondition));
    }
    return ImmutableList.of();
  }

  /**
   * Computes the block formula, i.e., the transition from the block entry to the block end ({@code
   * false} if no path reaches the block end), and the states at the block entry from which a path
   * reaches a target location inside the block.
   */
  private void computeBlockFormula(SymbolicBlockAnalysis pAnalysis)
      throws CPAException, InterruptedException, SolverException {
    BlockEncoding encoding = pAnalysis.encode(blockNode, targets);
    BooleanFormula transition = pAnalysis.makeFalse();
    if (encoding.transition().isPresent()) {
      PathFormula pathFormula = encoding.transition().orElseThrow();
      transition = pathFormula.getFormula();
      exitSsa = pathFormula.getSsa();
    }
    blockFormula = pAnalysis.serialize(transition);
    BooleanFormula reachesTarget = null;
    if (encoding.pathsToTarget().isPresent()) {
      BooleanFormula condition = pAnalysis.entryCondition(encoding.pathsToTarget().orElseThrow());
      if (pAnalysis.isSatisfiable(condition)) {
        reachesTarget = condition;
        targetCondition = pAnalysis.serialize(condition);
      }
    }
    formulas = new FormulasInContext(pAnalysis, transition, reachesTarget);
    logger.log(Level.FINEST, "Block", blockNode);
    logger.log(Level.FINEST, "Block", blockNode.getId(), "has the transition", transition);
    logger.log(Level.FINEST, "Block", blockNode.getId(), "reaches a target from", reachesTarget);
  }

  public ImmutableList<DssMessage> processMessage(
      SymbolicBlockAnalysis pAnalysis, DssMessage pMessage)
      throws SolverException, InterruptedException {
    checkState(blockFormula != null, "Block %s must be initialized first", blockNode.getId());
    ensureParsedFor(pAnalysis);
    return switch (pMessage) {
      case DssPostConditionMessage precondition ->
          analyzeNewPrecondition(pAnalysis, pAnalysis.deserialize(formulaOf(precondition)));
      case DssViolationConditionMessage violation ->
          analyzeNewViolationCondition(pAnalysis, pAnalysis.deserialize(formulaOf(violation)));
      case DssExceptionMessage exception -> stop();
      case DssResultMessage result -> stop();
      case DssWitnessMessage witness -> stop();
    };
  }

  private ImmutableList<DssMessage> stop() {
    shutdown = true;
    return ImmutableList.of();
  }

  private ImmutableList<DssMessage> analyzeNewPrecondition(
      SymbolicBlockAnalysis pAnalysis, BooleanFormula newCondition)
      throws SolverException, InterruptedException {
    FormulasInContext context = ensureParsedFor(pAnalysis);
    statistics.preconditionsReceived.increment();
    BooleanFormula newEntryStates =
        subtractKnownStates
            ? pAnalysis.and(newCondition, pAnalysis.not(pAnalysis.or(context.preconditions)))
            : newCondition;
    // The unsatisfiability check of the new entry states, with the private variables of the stored
    // preconditions quantified, see SymbolicBlockAnalysis#closeForCoverage
    if (pAnalysis.isCovered(newCondition, context.preconditionsForCoverage)) {
      // no new states
      statistics.preconditionsCovered.increment();
      return ImmutableList.of();
    }
    preconditions.add(pAnalysis.serialize(newCondition));
    context.preconditions.add(newCondition);
    context.preconditionsForCoverage.add(pAnalysis.closeForCoverage(newCondition));
    if (context.targetCondition != null
        && pAnalysis.isSatisfiable(newEntryStates, context.targetCondition)) {
      // a target location inside the block is reachable
      return ImmutableList.of(violation());
    }
    AnalysisResult result =
        analyze(pAnalysis, newEntryStates, pAnalysis.or(context.violationConditionsAtExit));
    return switch (result) {
      case SafeAnalysisResult safe -> {
        if (postconditionReceivers.isEmpty()) {
          // no successor would receive it
          yield ImmutableList.of();
        }
        statistics.postconditionsSent.increment();
        yield ImmutableList.of(
            dssMessageFactory.createDssPostConditionMessage(
                blockNode.getId(),
                AlgorithmStatus.SOUND_AND_PRECISE,
                content(pAnalysis.serialize(pAnalysis.postcondition(safe.getResult(), exitSsa)))));
      }
      case UnsafeAnalysisResult ignore -> ImmutableList.of(violation());
    };
  }

  private ImmutableList<DssMessage> analyzeNewViolationCondition(
      SymbolicBlockAnalysis pAnalysis, BooleanFormula newCondition)
      throws SolverException, InterruptedException {
    FormulasInContext context = ensureParsedFor(pAnalysis);
    statistics.violationConditionsReceived.increment();
    BooleanFormula newViolatingStates =
        subtractKnownStates
            ? pAnalysis.and(
                pAnalysis.atExit(newCondition, exitSsa),
                pAnalysis.not(pAnalysis.or(context.violationConditionsAtExit)))
            : pAnalysis.atExit(newCondition, exitSsa);
    // The unsatisfiability check of the new violating states, with the private variables of the
    // stored violation conditions quantified. analyze() would find these states safe.
    boolean noNewStates = pAnalysis.isCovered(newCondition, context.violationConditionsForCoverage);
    violationConditions.add(pAnalysis.serialize(newCondition));
    context.violationConditionsForCoverage.add(pAnalysis.closeForCoverage(newCondition));
    context.violationConditionsAtExit.add(pAnalysis.atExit(newCondition, exitSsa));
    if (noNewStates) {
      statistics.violationConditionsCovered.increment();
      return ImmutableList.of();
    }
    boolean isUninitialized = context.preconditions.isEmpty();
    AnalysisResult result =
        analyze(
            pAnalysis,
            isUninitialized ? pAnalysis.makeTrue() : pAnalysis.or(context.preconditions),
            newViolatingStates);
    return switch (result) {
      case SafeAnalysisResult ignore -> ImmutableList.of();
      case UnsafeAnalysisResult unsafe -> {
        if (isUninitialized) {
          statistics.violationConditionsSent.increment();
          yield ImmutableList.of(
              violationConditionMessage(
                  pAnalysis.serialize(pAnalysis.preimage(unsafe.getResult()))));
        } else {
          yield ImmutableList.of(violation());
        }
      }
    };
  }

  /**
   * Checks whether states of the precondition reach states of the violation condition at the block
   * end. The precondition is in interface form for the block entry, the violation condition already
   * talks about the block end, see {@link SymbolicBlockAnalysis#atExit}.
   *
   * @return the precondition and the block formula if the check fails, otherwise the block formula
   *     and the violation condition
   */
  private AnalysisResult analyze(
      SymbolicBlockAnalysis pAnalysis,
      BooleanFormula precondition,
      BooleanFormula violationCondition)
      throws SolverException, InterruptedException {
    BooleanFormula blockFormulaInContext = ensureParsedFor(pAnalysis).blockFormula;
    BooleanFormula formulaToCheck =
        pAnalysis.and(precondition, blockFormulaInContext, violationCondition);
    if (pAnalysis.isUnsat(formulaToCheck)) {
      return new SafeAnalysisResult(pAnalysis.and(precondition, blockFormulaInContext));
    }
    return new UnsafeAnalysisResult(pAnalysis.and(blockFormulaInContext, violationCondition));
  }

  /** Parses the formulas of the worker into the context of the given analysis, unless they are. */
  private FormulasInContext ensureParsedFor(SymbolicBlockAnalysis pAnalysis) {
    FormulasInContext current = formulas;
    if (current != null && current.analysis == pAnalysis) {
      return current;
    }
    statistics.contextSwitches.increment();
    FormulasInContext parsed =
        new FormulasInContext(
            pAnalysis,
            pAnalysis.deserialize(blockFormula),
            targetCondition == null ? null : pAnalysis.deserialize(targetCondition));
    for (String precondition : preconditions) {
      BooleanFormula formula = pAnalysis.deserialize(precondition);
      parsed.preconditions.add(formula);
      parsed.preconditionsForCoverage.add(pAnalysis.closeForCoverage(formula));
    }
    for (String violationCondition : violationConditions) {
      BooleanFormula formula = pAnalysis.deserialize(violationCondition);
      parsed.violationConditionsForCoverage.add(pAnalysis.closeForCoverage(formula));
      parsed.violationConditionsAtExit.add(pAnalysis.atExit(formula, exitSsa));
    }
    formulas = parsed;
    return parsed;
  }

  private DssMessage violationConditionMessage(String pFormula) {
    return dssMessageFactory.createViolationConditionMessage(
        blockNode.getId(), AlgorithmStatus.SOUND_AND_PRECISE, content(pFormula));
  }

  private DssMessage violation() {
    logger.log(Level.FINE, "Block", blockNode.getId(), "found a violation");
    return dssMessageFactory.createDssResultMessage(blockNode.getId(), Result.FALSE);
  }

  /**
   * The content of a condition message. The formula lies below the level of {@link
   * PredicateAbstractState}, where {@link DssMessage#getAbstractStateContent} looks for it.
   */
  private static ImmutableMap<String, String> content(String pFormula) {
    return ContentBuilder.builder()
        .pushLevel(PredicateAbstractState.class.getName())
        .put(FORMULA_KEY, pFormula)
        .build();
  }

  private static String formulaOf(DssMessage pMessage) {
    return pMessage.getAbstractStateContent(PredicateAbstractState.class).get(FORMULA_KEY);
  }

  @Override
  public String toString() {
    return "SymbolicBlockWorker{block=" + blockNode.getId() + "}";
  }
}
