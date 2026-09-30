// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssSingleWorkerStatistics.SourceRefreshCause;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalyses.DssBlockAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Explores the block per predecessor and caches what it found, so that an update only costs the
 * exploration of what actually changed.
 *
 * <p>A <em>source</em> is the precondition set of one active predecessor (see {@link
 * PartialReplacePreconditionHandler}) or, as long as some predecessor has not sent anything yet,
 * the unconstrained start state. For every source the engine caches
 *
 * <ul>
 *   <li>the summaries at the block end and the violations that originate inside the block, which
 *       depend only on the source, and
 *   <li>the violating paths under every single violation condition, i.e., one entry per pair of
 *       source and condition.
 * </ul>
 *
 * <p>A source whose precondition set was replaced is explored again under all conditions. For every
 * other source, conditions no successor reports anymore are dropped from the cache, and only the
 * conditions it has not been explored under yet are explored, all of them in one run. The
 * violations of all sources are published together as one result.
 *
 * <p>As long as some source has a violation, no postcondition is published at all, and the
 * successors keep the one they received last, like with {@link AlwaysReplaceExplorationEngine}.
 * Publishing the summaries of only the sources without violations would shrink the precondition of
 * the successors, because a postcondition message replaces what a successor stored for this block.
 * A successor would then lose the violations that exist only from the missing states, among them
 * the ones this block found its violations under, and replace its violation conditions here with a
 * set without them. Around such a loop, violations can get lost for good, which made the analysis
 * report a wrong proof on {@code product-lines_simple-06.c}. Publishing the summaries of all
 * sources instead is not an option either: a source with a violation is typically one whose
 * precision is not refined yet, so its summaries are coarse. A block that is its own predecessor,
 * like a loop, then explores itself from such a coarse precondition, from which every violation
 * condition it sends itself is feasible again, and unrolls the loop forever instead of waiting for
 * the refinement of its other predecessors ({@code for.c}).
 *
 * <p>A round reports either violations or a postcondition, never both. The violations of the
 * speculative exploration from the unconstrained start state therefore hold the postcondition back
 * only in the round they are actually published in. From the unconstrained start state nearly every
 * violation condition is feasible, so a set that is unchanged is not published again and lets the
 * postcondition through; otherwise a loop block, which is its own predecessor, would never publish
 * one ({@code for.c}).
 *
 * <p>Publishing the same result again only makes the neighbors check that nothing changed, so the
 * engine leaves out a postcondition or a set of violations that equals what it published last.
 *
 * <p>This engine ignores program points. It is meant for the inlining decomposition, where all
 * preconditions and all violation conditions of a block are at the same program point anyway.
 */
final class PartialReplaceExplorationEngine implements DssExplorationEngine {

  /**
   * What the engine found for one source. Mutable, because it is updated condition by condition.
   */
  private static final class SourceResult {

    /** The explored precondition set. Compared by identity to find out whether it is up to date. */
    private final ImmutableList<@NonNull StateAndPrecision> preconditions;

    private ImmutableSet<StateAndPrecision> summaries = ImmutableSet.of();

    /**
     * The precision this source was explored with, or {@code null} while it never was. DSS shares
     * what a block learns through messages, so a refined precision reaches this block as a new
     * received precision; comparing the value detects that as reliably as a worker-local version
     * counter did, without the block having to keep one.
     */
    private @Nullable Precision precision;

    private ImmutableSet<ArgPathAndCondition> violationsFromOrigin = ImmutableSet.of();

    /**
     * The violating paths per violation condition this source has been explored under. A condition
     * without violating path maps to the empty set, so that it counts as explored. The conditions
     * are {@link org.sosy_lab.cpachecker.cpa.arg.ARGState}s and thus compared by identity.
     */
    private final Map<AbstractState, ImmutableSet<ArgPathAndCondition>> violationsPerCondition =
        new LinkedHashMap<>();

    private SourceResult(ImmutableList<@NonNull StateAndPrecision> pPreconditions) {
      preconditions = pPreconditions;
    }

    private Iterable<ArgPathAndCondition> violations() {
      return Iterables.concat(
          violationsFromOrigin, Iterables.concat(violationsPerCondition.values()));
    }
  }

  /** A postcondition as published to the successors, see {@link #lastPostcondition}. */
  private record Postcondition(ImmutableSet<AbstractState> summaries, boolean unreachable) {}

  private final DssBlockAnalysis analysis;
  private final DistributedConfigurableProgramAnalysis dcpa;
  private final PartialReplacePreconditionHandler preconditionHandler;
  private final PartialReplaceViolationConditionHandler violationConditionHandler;

  /**
   * The cached result of every predecessor that has been explored. Entries of covered predecessors
   * are kept: as long as their precondition set does not change, the entry becomes valid again as
   * soon as the predecessor is uncovered.
   */
  private final Map<String, SourceResult> resultPerPredecessor = new HashMap<>();

  /** The unconstrained start state, created once so that its cached result can be recognized. */
  private @Nullable ImmutableList<@NonNull StateAndPrecision> startState;

  private @Nullable SourceResult startStateResult;

  /**
   * Everything the refinements of this block learned so far, or {@code null} if nothing is kept,
   * see {@link DssAnalysisOptions#retainLearnedPrecision()}. Without it, a block that refutes one
   * violation condition forgets the predicates of the refutation in the next round, when it refutes
   * another one, and two conditions can make it alternate between two postconditions.
   */
  private @Nullable Precision learnedPrecision;

  /** The postcondition the successors currently hold for this block, if any. */
  private @Nullable Postcondition lastPostcondition;

  /** The violations the predecessors currently hold for this block. */
  private ImmutableSet<ArgPathAndCondition> lastViolations = ImmutableSet.of();

  PartialReplaceExplorationEngine(
      DssBlockAnalysis pAnalysis,
      PartialReplacePreconditionHandler pPreconditionHandler,
      PartialReplaceViolationConditionHandler pViolationConditionHandler) {
    analysis = pAnalysis;
    dcpa = pAnalysis.getDcpa();
    preconditionHandler = pPreconditionHandler;
    violationConditionHandler = pViolationConditionHandler;
  }

  @Override
  public AnalysisResult exploreInitially() throws CPAException, InterruptedException {
    startStateResult =
        refresh(
            null, startState(), violationConditionHandler.states(), analysis.makeStartPrecision());
    // The initial run publishes only the violations that originate inside the block. Its
    // postcondition stays unpublished, like in AlwaysReplaceExplorationEngine, except for the root
    // block if loop blocks iterate themselves: such a block covers its later iterations, so from
    // the unconstrained start state it only reports the violations of the iterations it explored,
    // and it needs real preconditions to find the others. They start at the root, whose start
    // state is the entry of the program.
    boolean publishPostcondition =
        analysis.getBlock().isRoot() && analysis.getOptions().iterateLoopBlocks();
    return publish(
        publishPostcondition ? startStateResult.summaries : ImmutableSet.of(),
        startStateResult.violations(),
        false);
  }

  @Override
  public AnalysisResult explore(boolean pViolationConditionsChanged)
      throws CPAException, InterruptedException {
    boolean unreachable = preconditionHandler.isUnreachable();
    if (unreachable && !pViolationConditionsChanged) {
      // every predecessor reported an unreachable block end, so this block cannot be entered
      return publish(ImmutableSet.of(), ImmutableList.of(), true);
    }
    ImmutableList<AbstractState> conditions = violationConditionHandler.states();
    Precision precision = combinedPrecision();

    // The precision is fixed for this round: DSS shares what a block learns through messages, so a
    // refinement reaches this block as a received precision of a later round, not while it
    // explores. Every source of this round is therefore explored at the same precision.
    ImmutableSet.Builder<StateAndPrecision> summaries = ImmutableSet.builder();
    ImmutableList.Builder<ArgPathAndCondition> violations = ImmutableList.builder();
    boolean anySourceHasViolations = false;
    boolean anyRealSource = false;
    for (Entry<String, ImmutableList<@NonNull StateAndPrecision>> active :
        preconditionHandler.getActivePreconditions().entrySet()) {
      String predecessor = active.getKey();
      if (active.getValue().isEmpty()) {
        // the block end of this predecessor is unreachable, so it contributes nothing
        resultPerPredecessor.remove(predecessor);
        continue;
      }
      SourceResult result =
          refresh(resultPerPredecessor.get(predecessor), active.getValue(), conditions, precision);
      resultPerPredecessor.put(predecessor, result);
      anyRealSource = true;
      ImmutableList<ArgPathAndCondition> sourceViolations =
          ImmutableList.copyOf(result.violations());
      anySourceHasViolations |= !sourceViolations.isEmpty();
      violations.addAll(sourceViolations);
      summaries.addAll(result.summaries);
    }

    // A silent predecessor leaves the entry unconstrained. Explore from top while one is
    // silent, or when every predecessor is unreachable but a new violation condition arrives.
    boolean speculative = unreachable || preconditionHandler.isAnyPredecessorSilent();
    if (speculative && (!anyRealSource || analysis.getOptions().publishSpeculativeViolations())) {
      // its summaries are never published: the start state is not a real precondition
      startStateResult = refresh(startStateResult, startState(), conditions, precision);
      violations.addAll(startStateResult.violations());
    }

    // See the class documentation for why a violation of a real source holds back the whole
    // postcondition. The speculative violations only do so in the round they are published in,
    // which publish decides.
    ImmutableSet<StateAndPrecision> allSummaries =
        anySourceHasViolations ? ImmutableSet.of() : summaries.build();
    ImmutableList<ArgPathAndCondition> allViolations = violations.build();
    return publish(
        allSummaries,
        allViolations,
        allSummaries.isEmpty() && allViolations.isEmpty() && !speculative);
  }

  /**
   * Brings the cached result of one source up to date with the given precondition set and violation
   * conditions, exploring the block only where the cache does not suffice.
   *
   * @param pCached the result cached for this source, if any
   * @return the up-to-date result, which is {@code pCached} itself if its precondition set is still
   *     {@code pPreconditions}
   */
  private SourceResult refresh(
      @Nullable SourceResult pCached,
      ImmutableList<@NonNull StateAndPrecision> pPreconditions,
      ImmutableList<AbstractState> pConditions,
      Precision pPrecision)
      throws CPAException, InterruptedException {
    if (pCached == null
        || pCached.preconditions != pPreconditions
        || !pPrecision.equals(pCached.precision)) {
      analysis
          .statistics()
          .recordSourceRefresh(
              pCached == null
                  ? SourceRefreshCause.FIRST_EXPLORATION
                  : pCached.preconditions != pPreconditions
                      ? SourceRefreshCause.CHANGED_PRECONDITION
                      : SourceRefreshCause.CHANGED_PRECISION);
      SourceResult result = new SourceResult(pPreconditions);
      explore(result, pConditions, pPrecision);
      return result;
    }
    Set<AbstractState> current = new LinkedHashSet<>(pConditions);
    // a condition no successor reports anymore must not keep the summaries of this source back
    pCached.violationsPerCondition.keySet().retainAll(current);
    current.removeAll(pCached.violationsPerCondition.keySet());
    if (!current.isEmpty()) {
      // Only the conditions this source has not been explored under yet are new, so only they are
      // explored. The summaries and the violations from the origin do not depend on the conditions,
      // but they are replaced by those of this run anyway, see explore.
      analysis.statistics().recordSourceRefresh(SourceRefreshCause.NEW_CONDITIONS);
      explore(pCached, ImmutableList.copyOf(current), pPrecision);
    } else {
      analysis.statistics().recordSourceRefresh(SourceRefreshCause.CACHE_HIT);
    }
    return pCached;
  }

  /**
   * Explores the block from every precondition of {@code pResult} under all given violation
   * conditions at once and records the violating paths per condition in {@code pResult}.
   *
   * <p>The summaries and the violations from the origin of {@code pResult} are replaced by those of
   * this run, although they do not depend on the conditions. Refuting a new condition refines the
   * precision, and only the summaries computed with the refined precision tell the successors what
   * the refinement learned. Keeping the old ones made a loop block wait forever for a precondition
   * strong enough to refute the conditions it sends itself ({@code for.c}), because its predecessor
   * never published what it learned from refuting them.
   *
   * <p>The block analysis retains its learned and received precisions across runs. A cached source
   * is refreshed when that precision grows, even if its states and conditions remain unchanged.
   */
  private void explore(
      SourceResult pResult, ImmutableList<AbstractState> pConditions, Precision pPrecision)
      throws CPAException, InterruptedException {
    Map<AbstractState, ImmutableSet.Builder<ArgPathAndCondition>> violationsPerCondition =
        new LinkedHashMap<>();
    for (AbstractState condition : pConditions) {
      violationsPerCondition.put(condition, ImmutableSet.builder());
    }
    ImmutableSet.Builder<StateAndPrecision> summaries = ImmutableSet.builder();
    ImmutableSet.Builder<ArgPathAndCondition> violationsFromOrigin = ImmutableSet.builder();

    // Real-source summaries are reusable only at the precision used for all their start states.
    // Refinement may leave earlier, coarser summaries in the reached set.
    pResult.precision = pPrecision;
    for (AbstractState precondition : statesToExplore(pResult.preconditions)) {
      analysis
          .statistics()
          .recordExploration(pResult.preconditions == startState, pConditions.size());
      DssBlockAnalysisResult result;
      try {
        result = analysis.runBlockAnalysis(dcpa.reset(precondition), pPrecision, pConditions);
        if (analysis.getOptions().retainLearnedPrecision()) {
          Precision learned = analysis.precisionOfLastRun();
          learnedPrecision =
              learnedPrecision == null ? learned : analysis.unionOf(learnedPrecision, learned);
        }
      } catch (UnrecognizedCodeException e) {
        if (!isSpeculative(pResult)) {
          throw e;
        }
        // The unconstrained start state also reaches code that no execution of the program
        // reaches, and the predicate analysis gives up on some of it, e.g., memset through a void
        // pointer. Nothing is lost by dropping this run: its summaries are never published, and
        // every violation it would find is found again from the real preconditions, which fail in
        // the same way if they reach this code.
        analysis
            .getLogger()
            .logUserException(
                Level.INFO, e, "Ignoring unsupported code in a speculative block analysis");
        continue;
      }
      for (ArgPathAndCondition violation :
          analysis.pathsWithCondition(result.getViolationConditionViolations())) {
        ImmutableSet.Builder<ArgPathAndCondition> forCondition =
            violationsPerCondition.get(violation.condition());
        checkState(
            forCondition != null, "Found a violation of an unknown condition: %s", violation);
        forCondition.add(violation);
      }
      violationsFromOrigin.addAll(analysis.pathsFromOrigin(result.getTargetStates()));
      summaries.addAll(analysis.summariesOf(result));
    }

    for (Entry<AbstractState, ImmutableSet.Builder<ArgPathAndCondition>> entry :
        violationsPerCondition.entrySet()) {
      pResult.violationsPerCondition.put(entry.getKey(), entry.getValue().build());
    }
    pResult.summaries = summaries.build();
    pResult.violationsFromOrigin = violationsFromOrigin.build();
  }

  /**
   * Whether the result belongs to the exploration from the unconstrained start state of a block
   * that has predecessors. For the root block, the start state is the entry of the program.
   */
  private boolean isSpeculative(SourceResult pResult) {
    return pResult.preconditions == startState
        && !analysis.getBlock().getPredecessorIds().isEmpty();
  }

  private ImmutableList<AbstractState> statesToExplore(
      ImmutableList<@NonNull StateAndPrecision> pPreconditions)
      throws CPAException, InterruptedException {
    ImmutableList<AbstractState> states =
        transformedImmutableListCopy(pPreconditions, sap -> dcpa.reset(sap.state()));
    return analysis.combineStates(states);
  }

  /**
   * The precision to explore a source with: the combination of the precisions of all known
   * preconditions. The block analysis also adds retained precision updates from successors.
   */
  private Precision combinedPrecision() throws InterruptedException {
    ImmutableList<@NonNull StateAndPrecision> known = preconditionHandler.getKnownPreconditions();
    Precision received =
        known.isEmpty() ? analysis.makeStartPrecision() : analysis.combinePrecisions(known);
    return learnedPrecision == null ? received : analysis.unionOf(received, learnedPrecision);
  }

  private ImmutableList<@NonNull StateAndPrecision> startState() throws InterruptedException {
    if (startState == null) {
      startState =
          ImmutableList.of(
              new StateAndPrecision(analysis.makeStartState(true), analysis.makeStartPrecision()));
    }
    return startState;
  }

  /**
   * Turns the combined findings of one round into the result to publish, leaving out what the
   * neighbors already hold.
   *
   * @param pUnreachable whether the block end is unreachable, in which case {@code pSummaries} is
   *     empty
   */
  private AnalysisResult publish(
      ImmutableSet<StateAndPrecision> pSummaries,
      Iterable<ArgPathAndCondition> pViolations,
      boolean pUnreachable)
      throws CPAException, InterruptedException {
    ImmutableSet<ArgPathAndCondition> violations = ImmutableSet.copyOf(pViolations);
    if (violations.isEmpty() || violations.equals(lastViolations)) {
      // Nothing new to publish: the predecessors keep what they received last, just like with
      // AlwaysReplaceExplorationEngine, which never publishes an empty set of violations either.
      violations = ImmutableSet.of();
    } else {
      lastViolations = violations;
    }

    // A round reports either violations or a postcondition, never both. Deciding this here, after
    // the violations that are not published again were dropped, keeps an unchanged set of
    // speculative violations from withholding the postcondition round after round.
    ImmutableSet<StateAndPrecision> toPublish =
        violations.isEmpty() ? pSummaries : ImmutableSet.of();

    ImmutableList<StateAndPrecision> summaries = ImmutableList.of();
    boolean unreachable = false;
    if (!toPublish.isEmpty() || (pUnreachable && violations.isEmpty())) {
      // Summaries are compared by their states, which are ARG states and thus compared by identity.
      // This is cheap and catches exactly the case that matters: nothing was explored again.
      Postcondition postcondition =
          new Postcondition(
              FluentIterable.from(toPublish).transform(StateAndPrecision::state).toSet(),
              pUnreachable);
      if (!postcondition.equals(lastPostcondition)) {
        lastPostcondition = postcondition;
        summaries = analysis.combineSummaries(toPublish);
        unreachable = pUnreachable;
      }
    }
    return new AnalysisResult(summaries, violations, unreachable);
  }
}
