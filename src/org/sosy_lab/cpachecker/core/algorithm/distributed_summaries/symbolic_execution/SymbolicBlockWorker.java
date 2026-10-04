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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.symbolic_execution.SymbolicBlockAnalysis.BlockEncoding;
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
 * <p>A violation condition only travels on while its block has not received a precondition yet.
 * Once a block has received one, the forward direction has reached it and checks every later
 * precondition against all stored violation conditions, so propagating further would only repeat
 * that work.
 *
 * <p>A block reacts only to conditions that are not covered by the ones it already stores. This
 * keeps the analysis from running around loops forever once their states repeat, although, like any
 * symbolic execution, it still does not terminate on loops whose states never repeat.
 *
 * <p>The state of a worker is kept as SMT-LIB strings, so any thread can analyze the block with its
 * own {@link SymbolicBlockAnalysis}. The formulas parsed into the context of the last analysis are
 * cached, and are only parsed again when another analysis, e.g., another thread, takes over.
 *
 * <p>A worker is not thread-safe, but it may move between threads as long as accesses are ordered.
 */
public final class SymbolicBlockWorker {

  static final String FORMULA_KEY = "formula";

  private final BlockNode block;
  private final ImmutableSet<CFANode> targets;
  private final ImmutableSet<String> postconditionReceivers;
  private final DssMessageFactory messageFactory;
  private final SymbolicBlockExecutionStatistics statistics;
  private final LogManager logger;

  private boolean initialized;

  /** The transition of the block in SMT-LIB, or null if no path reaches the block end. */
  private @Nullable String transition;

  private SSAMap exitSsa = SSAMap.emptySSAMap();

  /**
   * The states at the block entry from which a path reaches a target location inside the block, in
   * SMT-LIB, or null if there are none.
   */
  private @Nullable String targetCondition;

  private final List<String> preconditions = new ArrayList<>();
  private final List<String> violationConditions = new ArrayList<>();

  private @Nullable FormulasInContext formulas;

  /** The stored conditions parsed into the solver context of one analysis. */
  private static final class FormulasInContext {
    private final SymbolicBlockAnalysis analysis;
    private final @Nullable BooleanFormula transition;
    private final @Nullable BooleanFormula targetCondition;
    private final List<BooleanFormula> preconditions = new ArrayList<>();
    private final List<BooleanFormula> preconditionsForCoverage = new ArrayList<>();
    private final List<BooleanFormula> violationConditionsForCoverage = new ArrayList<>();
    private final List<BooleanFormula> violationConditionsAtExit = new ArrayList<>();

    private FormulasInContext(
        SymbolicBlockAnalysis pAnalysis,
        @Nullable BooleanFormula pTransition,
        @Nullable BooleanFormula pTargetCondition) {
      analysis = pAnalysis;
      transition = pTransition;
      targetCondition = pTargetCondition;
    }
  }

  /**
   * Creates the worker of a block.
   *
   * @param pBlock the block
   * @param pTargets all target locations of the program; the block finds the ones it reaches
   * @param pPostconditionReceivers the successors of the block that can reach a target, the only
   *     ones its postconditions matter for
   */
  SymbolicBlockWorker(
      BlockNode pBlock,
      ImmutableSet<CFANode> pTargets,
      ImmutableSet<String> pPostconditionReceivers,
      DssMessageFactory pMessageFactory,
      SymbolicBlockExecutionStatistics pStatistics,
      LogManager pLogger) {
    block = pBlock;
    targets = pTargets;
    postconditionReceivers = pPostconditionReceivers;
    messageFactory = pMessageFactory;
    statistics = pStatistics;
    logger = pLogger;
  }

  String getBlockId() {
    return block.getId();
  }

  /** The blocks that receive the postconditions of this block. */
  ImmutableSet<String> postconditionReceivers() {
    return postconditionReceivers;
  }

  /** The blocks that receive the violation conditions of this block. */
  ImmutableSet<String> violationConditionReceivers() {
    return block.getPredecessorIds();
  }

  /**
   * Encodes the block and starts the symbolic execution: the root block propagates the
   * postcondition of all states, and a block with a reachable target location propagates the states
   * that reach it.
   */
  ImmutableList<DssMessage> initialize(SymbolicBlockAnalysis pAnalysis)
      throws CPAException, InterruptedException, SolverException {
    checkState(!initialized, "Block %s is already initialized", block.getId());
    initialized = true;
    BlockEncoding encoding = pAnalysis.encode(block, targets);

    BooleanFormula transitionFormula = null;
    if (encoding.transition().isPresent()) {
      PathFormula pathFormula = encoding.transition().orElseThrow();
      transitionFormula = pathFormula.getFormula();
      transition = pAnalysis.dump(transitionFormula);
      exitSsa = pathFormula.getSsa();
    }
    BooleanFormula targetFormula = null;
    if (encoding.pathsToTarget().isPresent()) {
      BooleanFormula condition = pAnalysis.entryCondition(encoding.pathsToTarget().orElseThrow());
      if (pAnalysis.isSatisfiable(condition)) {
        targetFormula = condition;
        targetCondition = pAnalysis.dump(condition);
      }
    }
    formulas = new FormulasInContext(pAnalysis, transitionFormula, targetFormula);

    if (block.isRoot()) {
      // The program starts in every state at the entry of the root block.
      return addPreconditions(pAnalysis, ImmutableList.of(pAnalysis.dump(pAnalysis.makeTrue())));
    }
    if (targetCondition != null) {
      logger.log(Level.FINE, "Block", block.getId(), "contains a reachable target location");
      return ImmutableList.of(violationConditionMessage(targetCondition));
    }
    return ImmutableList.of();
  }

  /**
   * Processes the postconditions of predecessors and the violation conditions of successors among
   * the given messages, all at once. Other messages are ignored; ending the analysis is up to the
   * caller.
   *
   * @return the conditions to propagate, or a single result message if the program is unsafe
   */
  ImmutableList<DssMessage> processMessages(
      SymbolicBlockAnalysis pAnalysis, List<DssMessage> pMessages)
      throws InterruptedException, SolverException {
    checkState(initialized, "Block %s must be initialized first", block.getId());
    List<String> receivedPreconditions = new ArrayList<>();
    List<String> receivedViolationConditions = new ArrayList<>();
    for (DssMessage message : pMessages) {
      switch (message) {
        case DssPostConditionMessage postcondition ->
            receivedPreconditions.add(formulaOf(postcondition));
        case DssViolationConditionMessage violationCondition ->
            receivedViolationConditions.add(formulaOf(violationCondition));
        case DssResultMessage result -> {}
        case DssExceptionMessage exception -> {}
        case DssWitnessMessage witness -> {}
      }
    }
    ensureParsedFor(pAnalysis);
    // Forward first: once a block has a precondition, its violation conditions need not travel on.
    ImmutableList<DssMessage> forward = addPreconditions(pAnalysis, receivedPreconditions);
    if (isViolation(forward)) {
      return forward;
    }
    return ImmutableList.<DssMessage>builder()
        .addAll(forward)
        .addAll(addViolationConditions(pAnalysis, receivedViolationConditions))
        .build();
  }

  private static boolean isViolation(List<DssMessage> pMessages) {
    return pMessages.stream().anyMatch(DssResultMessage.class::isInstance);
  }

  private ImmutableList<DssMessage> addPreconditions(
      SymbolicBlockAnalysis pAnalysis, List<String> pReceived)
      throws InterruptedException, SolverException {
    if (pReceived.isEmpty()) {
      return ImmutableList.of();
    }
    FormulasInContext context = ensureParsedFor(pAnalysis);
    statistics.preconditionsReceived.add(pReceived.size());
    List<BooleanFormula> parsed = parseAll(pAnalysis, pReceived);
    BooleanFormula precondition = pAnalysis.or(parsed);
    if (pAnalysis.isCovered(precondition, context.preconditionsForCoverage)) {
      statistics.preconditionsCovered.add(pReceived.size());
      return ImmutableList.of();
    }
    preconditions.addAll(pReceived);
    for (BooleanFormula formula : parsed) {
      context.preconditions.add(formula);
      context.preconditionsForCoverage.add(pAnalysis.closeForCoverage(formula));
    }

    if (context.targetCondition != null
        && pAnalysis.isSatisfiable(precondition, context.targetCondition)) {
      logger.log(Level.FINE, "Block", block.getId(), "reaches a target location");
      return ImmutableList.of(violation());
    }
    if (context.transition == null) {
      return ImmutableList.of();
    }
    if (!context.violationConditionsAtExit.isEmpty()
        && pAnalysis.isSatisfiable(
            precondition, context.transition, pAnalysis.or(context.violationConditionsAtExit))) {
      logger.log(Level.FINE, "Block", block.getId(), "reaches a violation condition");
      return ImmutableList.of(violation());
    }
    if (postconditionReceivers.isEmpty()) {
      return ImmutableList.of();
    }
    BooleanFormula postcondition =
        pAnalysis.postcondition(precondition, context.transition, exitSsa);
    if (pAnalysis.isFalse(postcondition)) {
      return ImmutableList.of();
    }
    statistics.postconditionsSent.increment();
    return ImmutableList.of(postconditionMessage(pAnalysis.dump(postcondition)));
  }

  private ImmutableList<DssMessage> addViolationConditions(
      SymbolicBlockAnalysis pAnalysis, List<String> pReceived)
      throws InterruptedException, SolverException {
    if (pReceived.isEmpty()) {
      return ImmutableList.of();
    }
    FormulasInContext context = ensureParsedFor(pAnalysis);
    statistics.violationConditionsReceived.add(pReceived.size());
    List<BooleanFormula> parsed = parseAll(pAnalysis, pReceived);
    BooleanFormula violationCondition = pAnalysis.or(parsed);
    if (pAnalysis.isCovered(violationCondition, context.violationConditionsForCoverage)) {
      statistics.violationConditionsCovered.add(pReceived.size());
      return ImmutableList.of();
    }
    violationConditions.addAll(pReceived);
    for (BooleanFormula formula : parsed) {
      context.violationConditionsForCoverage.add(pAnalysis.closeForCoverage(formula));
      context.violationConditionsAtExit.add(pAnalysis.atExit(formula, exitSsa));
    }

    if (context.transition == null) {
      // no state at the block end, so none satisfies the condition
      return ImmutableList.of();
    }
    if (!context.preconditions.isEmpty()) {
      if (pAnalysis.isSatisfiable(
          pAnalysis.or(context.preconditions),
          context.transition,
          pAnalysis.atExit(violationCondition, exitSsa))) {
        logger.log(Level.FINE, "Block", block.getId(), "reaches a violation condition");
        return ImmutableList.of(violation());
      }
      return ImmutableList.of();
    }
    BooleanFormula preimage = pAnalysis.preimage(context.transition, exitSsa, violationCondition);
    if (pAnalysis.isFalse(preimage)) {
      return ImmutableList.of();
    }
    statistics.violationConditionsSent.increment();
    return ImmutableList.of(violationConditionMessage(pAnalysis.dump(preimage)));
  }

  private static List<BooleanFormula> parseAll(
      SymbolicBlockAnalysis pAnalysis, List<String> pFormulas) {
    List<BooleanFormula> parsed = new ArrayList<>(pFormulas.size());
    for (String formula : pFormulas) {
      parsed.add(pAnalysis.parse(formula));
    }
    return parsed;
  }

  /** Parses the stored conditions into the context of the given analysis, unless they are. */
  private FormulasInContext ensureParsedFor(SymbolicBlockAnalysis pAnalysis) {
    FormulasInContext current = formulas;
    if (current != null && current.analysis == pAnalysis) {
      return current;
    }
    statistics.contextSwitches.increment();
    FormulasInContext parsed =
        new FormulasInContext(
            pAnalysis,
            transition == null ? null : pAnalysis.parse(transition),
            targetCondition == null ? null : pAnalysis.parse(targetCondition));
    for (String precondition : preconditions) {
      BooleanFormula formula = pAnalysis.parse(precondition);
      parsed.preconditions.add(formula);
      parsed.preconditionsForCoverage.add(pAnalysis.closeForCoverage(formula));
    }
    for (String violationCondition : violationConditions) {
      BooleanFormula formula = pAnalysis.parse(violationCondition);
      parsed.violationConditionsForCoverage.add(pAnalysis.closeForCoverage(formula));
      parsed.violationConditionsAtExit.add(pAnalysis.atExit(formula, exitSsa));
    }
    formulas = parsed;
    return parsed;
  }

  private DssMessage postconditionMessage(String pFormula) {
    return messageFactory.createDssPostConditionMessage(
        block.getId(), AlgorithmStatus.SOUND_AND_PRECISE, content(pFormula));
  }

  private DssMessage violationConditionMessage(String pFormula) {
    return messageFactory.createViolationConditionMessage(
        block.getId(), AlgorithmStatus.SOUND_AND_PRECISE, content(pFormula));
  }

  private DssMessage violation() {
    return messageFactory.createDssResultMessage(block.getId(), Result.FALSE);
  }

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
    return "SymbolicBlockWorker{block=" + block.getId() + "}";
  }
}
