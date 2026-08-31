// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalysis.blockStateOf;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Level;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssDebugUtils;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssSingleWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalyses.DssBlockAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraphPath;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Groups preconditions by the path through the block graph along which they were produced.
 *
 * <p>An incoming postcondition only replaces the preconditions whose path the incoming overlaps
 * (see {@link BlockGraphPath}), which lets the block tell a genuinely new context apart from a
 * repetition of one it has already explored. Only the paths affected by the last update are
 * re-explored, and a precondition that does not constrain the block entry is explored with the
 * initial precision instead of the accumulated one.
 */
final class PathBasedPreconditionHandler implements DssPreconditionHandler {

  /**
   * The states covering all received Preconditions, sorted by their path through the blocks (for
   * determining which should be removed when a new precondition arrives).
   */
  private final Map<BlockGraphPath, @NonNull StatesByPath> preconditions = new LinkedHashMap<>();

  /**
   * States that have been received as precondition, but are covered by another precondition, so do
   * not need to be analyzed as well. Needed for when the covering state is later replaced by a
   * different one.
   *
   * <p>An entry A -> B means B is covered by A.
   */
  private final Multimap<BlockGraphPath, StatesByPath> coveredStates = ArrayListMultimap.create();

  /** The paths that were added / changed by the last call to store */
  private final List<BlockGraphPath> pathsToAnalyze = new ArrayList<>();

  private final DssBlockAnalysis analysis;
  private final boolean resetPrecisionsForEveryRun;

  PathBasedPreconditionHandler(DssBlockAnalysis pAnalysis) throws InterruptedException {
    analysis = pAnalysis;
    resetPrecisionsForEveryRun = pAnalysis.getOptions().doResetPrecisionsForEveryRun();

    // Start from a top state, replaceable by any predecessor path. Its own path already contains
    // this block instead of being empty, so that a path which loops all the way back to this block
    // without ever incorporating a real predecessor still starts with this block's id, no matter
    // how
    // many other blocks the loop passes through in between -- see shouldConsiderPath and store(),
    // which appends to a path's history on receipt instead of on send for the same reason.
    AbstractState startState = pAnalysis.makeStartState(!analysis.getBlock().isRoot());
    blockStateOf(startState).addHistory(analysis.getBlock());
    BlockGraphPath startPath = BlockGraphPath.of(analysis.getBlock().getId());
    preconditions.put(
        startPath,
        new StatesByPath(
            startPath,
            ImmutableList.of(new StateAndPrecision(startState, pAnalysis.makeStartPrecision()))));
  }

  @Override
  public Collection<DssMessage> runInitialAnalysis()
      throws CPAException, InterruptedException, SolverException {

    StateAndPrecision initialTopState =
        Iterables.getOnlyElement(
            FluentIterable.from(preconditions.values()).transformAndConcat(StatesByPath::states));

    DssBlockAnalysisResult result =
        analysis.runInitialBlockAnalysis(initialTopState.state(), initialTopState.precision());

    ImmutableList.Builder<DssMessage> initialMessages = ImmutableList.builder();
    if (!result.getFinalLocationStates().isEmpty()) {
      initialMessages.addAll(analysis.reportPostconditions(analysis.finalLocationStatesOf(result)));
    }
    if (!result.getAllViolations().isEmpty()) {
      initialMessages.addAll(analysis.reportFirstViolationConditions(result.getAllViolations()));
    }
    return initialMessages.build();
  }

  @Override
  public DssMessageProcessing store(DssPostConditionMessage pReceived)
      throws InterruptedException, SolverException, CPAException {
    pathsToAnalyze.clear();
    analysis.getLogger().log(Level.INFO, "Running forward analysis with new precondition");
    ImmutableList<@NonNull StateAndPrecision> received = analysis.deserialize(pReceived);
    // Recorded here, by the receiver, rather than by the sender before it serializes its
    // postcondition: see the constructor for why this block has to end up in its own history.
    received.forEach(sap -> sap.getBlockState().addHistory(analysis.getBlock()));
    DssSingleWorkerStatistics stats = analysis.statistics();
    stats.getStorePreconditionStatesTimer().start();
    try {
      DssMessageProcessing processing = analysis.shouldProceedForward(received);
      if (!processing.shouldProceed()) {
        return processing;
      }

      Map<BlockGraphPath, StatesByPath> groupedStates = groupStatesByPath(received);

      if (preconditions.isEmpty()) {
        preconditions.putAll(groupedStates);
        pathsToAnalyze.addAll(groupedStates.keySet());
        return DssMessageProcessing.proceed();
      }

      storeStates(groupedStates);

      if (pathsToAnalyze.isEmpty()) {
        return DssMessageProcessing.stop();
      }

      return processing;
    } finally {
      stats.getStorePreconditionStatesTimer().stop();
      stats.getStorePreconditionStatesCounter().add(received.size());
    }
  }

  private void storeStates(Map<@NonNull BlockGraphPath, StatesByPath> pGroupedStates)
      throws CPAException, InterruptedException {

    Map<BlockGraphPath, StatesByPath> filteredGroups =
        Maps.filterKeys(pGroupedStates, this::shouldConsiderPath);

    ImmutableSet.Builder<StatesByPath> noLongerCovered = ImmutableSet.builder();

    for (StatesByPath newStates : filteredGroups.values()) {
      if (sameInformationAsExisting(newStates)) {
        continue;
      }

      removeStatesThatUseOutdatedKnowledge(newStates.path, noLongerCovered);

      Optional<BlockGraphPath> coveringPath = findCoveringPath(newStates);
      if (coveringPath.isPresent()) {
        // No need to analyze now, but store in case the covering state is removed later
        coveredStates.put(coveringPath.orElseThrow(), newStates);
      } else {
        preconditions.put(newStates.path, newStates);
        pathsToAnalyze.add(newStates.path);
      }
    }

    readdStatesThatAreNoLongerCovered(noLongerCovered);
  }

  /**
   * Re-adds the given states to preconditions, as if they had just arrived in a new message.
   *
   * @param noLongerCovered A set of states that were covered before and no longer are.
   */
  private void readdStatesThatAreNoLongerCovered(ImmutableSet.Builder<StatesByPath> noLongerCovered)
      throws CPAException, InterruptedException {
    ImmutableMap.Builder<BlockGraphPath, StatesByPath> toAddBuilder = ImmutableMap.builder();
    for (StatesByPath statesByPath : noLongerCovered.build()) {
      toAddBuilder.put(statesByPath.path, statesByPath);
    }

    ImmutableMap<BlockGraphPath, StatesByPath> toAdd = toAddBuilder.buildKeepingLast();
    if (!toAdd.isEmpty()) {
      storeStates(toAdd);
    }
  }

  /**
   * Tests if the new states are covered by the states of any single path in the preconditions.
   *
   * @param newStates the states to check
   * @return the path of the covering precondition, if one exists
   */
  private Optional<BlockGraphPath> findCoveringPath(StatesByPath newStates)
      throws CPAException, InterruptedException {
    for (StatesByPath existing : preconditions.values()) {
      if (analysis.allCovered(newStates.states, existing.states)) {
        return Optional.of(existing.path);
      }
    }
    return Optional.empty();
  }

  /**
   * Removes preconditions whose path overlaps with {@code addedPath}, since their knowledge may now
   * be outdated. Removes from preconditions, coveredStates and pathsToAnalyze.
   *
   * <p>E.g. adding {@code [L0]} removes an existing {@code [L0, L5]} (prefix), and adding {@code
   * [A, B]} removes an existing {@code [B, C]} (boundary overlap on {@code B})
   *
   * @param addedPath the path of the newly added precondition
   * @param noLongerCovered A builder collecting all the states that were covered by the removed
   *     states
   */
  private void removeStatesThatUseOutdatedKnowledge(
      BlockGraphPath addedPath, ImmutableSet.Builder<StatesByPath> noLongerCovered) {

    Iterator<StatesByPath> preconditionsIterator = preconditions.values().iterator();
    while (preconditionsIterator.hasNext()) {
      StatesByPath existing = preconditionsIterator.next();
      if (addedPath.overlapsWith(existing.path)) {
        preconditionsIterator.remove();
        pathsToAnalyze.remove(existing.path);
        noLongerCovered.addAll(coveredStates.removeAll(existing.path));
      }
    }

    coveredStates.entries().removeIf(entry -> addedPath.overlapsWith(entry.getValue().path));
  }

  /**
   * Checks if the new information already exists in the preconditions in the same way.
   *
   * @param newStates the states to check
   * @return true if it exists
   */
  private boolean sameInformationAsExisting(StatesByPath newStates)
      throws CPAException, InterruptedException {
    for (StatesByPath existing : preconditions.values()) {
      // TODO could work similarly with suffix instead of just equal path, where we replace the
      // path stored with the new path, but as long as the race condition exists, we deny shorter
      // paths and would not unroll loops properly
      if (newStates.path.equals(existing.path)
          && analysis.statesEqual(newStates.states, existing.states)) {
        return true;
      }
    }
    return false;
  }

  private boolean shouldConsiderPath(BlockGraphPath pBlockGraphPath) {

    // Do not unroll a loop without prefix
    if (pBlockGraphPath.path().getFirst().equals(analysis.getBlock().getId())) {
      return false;
    }

    // if this message had been here, it would have been removed because of the overlap check. This
    // is a subset of the race condition below
    for (BlockGraphPath existingPath : preconditions.keySet()) {
      if (!existingPath.isPrefixOf(pBlockGraphPath) && existingPath.overlapsWith(pBlockGraphPath)) {
        return false;
      }
    }

    // TODO fix race condition with old messages that are on the way

    return true;
  }

  private record StatesByPath(BlockGraphPath path, Collection<StateAndPrecision> states) {

    @Override
    public String toString() {
      return "StatesByPath[path=" + path + ", states=" + states.size() + "]";
    }
  }

  private Map<BlockGraphPath, StatesByPath> groupStatesByPath(
      Collection<StateAndPrecision> states) {
    ImmutableMap.Builder<BlockGraphPath, StatesByPath> builder = ImmutableMap.builder();
    for (Entry<BlockGraphPath, Collection<StateAndPrecision>> entry :
        Multimaps.index(states, state -> state.getBlockGraphPath()).asMap().entrySet()) {
      builder.put(entry.getKey(), new StatesByPath(entry.getKey(), entry.getValue()));
    }
    return builder.buildOrThrow();
  }

  @Override
  public Collection<DssMessage> analyze()
      throws SolverException, InterruptedException, CPAException {
    ImmutableSet.Builder<DssMessage> messages = ImmutableSet.builder();
    AnalysisResult round = explore(Optional.empty());
    if (!round.violationConditions().isEmpty()) {
      messages.addAll(analysis.reportViolationConditions(round.violationConditions()));
    }
    if (!round.summaries().isEmpty()) {
      messages.addAll(analysis.reportPostconditions(round.summaries()));
    }
    return messages.build();
  }

  @Override
  public Collection<DssMessage> analyzeFor(String pViolationConditionSender)
      throws SolverException, InterruptedException, CPAException {
    checkArgument(
        !analysis.getViolationConditionHandler().isEmptyFor(pViolationConditionSender),
        "No violation condition found for sender ID: %s",
        pViolationConditionSender);
    ImmutableList.Builder<DssMessage> messages = ImmutableList.builder();
    AnalysisResult round = explore(Optional.of(pViolationConditionSender));
    if (!round.summaries().isEmpty()) {
      messages.addAll(analysis.reportPostconditions(round.summaries()));
    }
    if (!round.violationConditions().isEmpty()) {
      messages.addAll(analysis.reportViolationConditions(round.violationConditions()));
    }
    return messages.build();
  }

  @Override
  public ImmutableList<@NonNull StateAndPrecision> getKnownPreconditions() {
    return FluentIterable.from(preconditions.values())
        .transformAndConcat(StatesByPath::states)
        .toList();
  }

  @Override
  public void violationConditionsChanged() {
    pathsToAnalyze.clear();
    pathsToAnalyze.addAll(preconditions.keySet());
  }

  /**
   * Renders {@link #preconditions}, {@link #coveredStates} and {@link #pathsToAnalyze} for
   * debugging.
   */
  @Override
  public String toString() {
    List<List<String>> preconditionRows = new ArrayList<>();
    for (StatesByPath statesByPath : preconditions.values()) {
      String renderedPath = DssDebugUtils.render(statesByPath.path());
      for (StateAndPrecision stateAndPrecision : statesByPath.states()) {
        preconditionRows.add(
            ImmutableList.of(renderedPath, DssDebugUtils.oneLine(stateAndPrecision.state())));
        renderedPath = "";
      }
    }
    String preconditionsBody =
        preconditionRows.isEmpty()
            ? "<none>"
            : DssDebugUtils.table(ImmutableList.of("path", "state"), preconditionRows);

    List<List<String>> coveredRows = new ArrayList<>();
    for (BlockGraphPath coveringPath : coveredStates.keySet()) {
      String renderedCoveringPath = DssDebugUtils.render(coveringPath);
      for (StatesByPath covered : coveredStates.get(coveringPath)) {
        String renderedCoveredPath = DssDebugUtils.render(covered.path());
        for (StateAndPrecision stateAndPrecision : covered.states()) {
          coveredRows.add(
              ImmutableList.of(
                  renderedCoveringPath,
                  renderedCoveredPath,
                  DssDebugUtils.oneLine(stateAndPrecision.state())));
          renderedCoveringPath = "";
          renderedCoveredPath = "";
        }
      }
    }
    String coveredBody =
        coveredRows.isEmpty()
            ? "<none>"
            : DssDebugUtils.table(ImmutableList.of("coveredBy", "path", "state"), coveredRows);

    String pathsToAnalyzeBody =
        pathsToAnalyze.isEmpty()
            ? "<none>"
            : Joiner.on('\n')
                .join(FluentIterable.from(pathsToAnalyze).transform(DssDebugUtils::render));

    String body =
        "preconditions ("
            + preconditionRows.size()
            + " states in "
            + preconditions.size()
            + " paths):\n"
            + DssDebugUtils.indent("  ", preconditionsBody)
            + "\n\ncovered states ("
            + coveredRows.size()
            + " states covered by "
            + coveredStates.keySet().size()
            + " paths):\n"
            + DssDebugUtils.indent("  ", coveredBody)
            + "\n\npaths to analyze ("
            + pathsToAnalyze.size()
            + "):\n"
            + DssDebugUtils.indent("  ", pathsToAnalyzeBody);
    return DssDebugUtils.box("Block " + analysis.getBlock().getId(), body);
  }

  /**
   * Runs the CPA under an error condition, i.e., if the current block contains a block-end edge,
   * the error condition will be attached to that edge. In case this makes the path formula
   * infeasible, we compute an abstraction. If no error condition is present, we run the CPA.
   *
   * @param pSender restricts the exploration to the violation conditions of one block, if given
   */
  private AnalysisResult explore(Optional<String> pSender)
      throws CPAException, InterruptedException {
    ImmutableList.Builder<StateAndPrecision> summaries = ImmutableList.builder();
    ImmutableSet.Builder<ArgPathAndCondition> violations = ImmutableSet.builder();

    for (BlockGraphPath path : pathsToAnalyze) {
      Precision currentPrecision = analysis.combinePrecisions(preconditions.get(path).states);
      for (StateAndPrecision precondition : ImmutableList.copyOf(preconditions.get(path).states)) {
        boolean isTrivial = analysis.getDcpa().isMostGeneralBlockEntryState(precondition.state());
        Precision precision =
            resetPrecisionsForEveryRun || isTrivial
                ? analysis.makeStartPrecision()
                : currentPrecision;

        DssBlockAnalysisResult result =
            analysis.runBlockAnalysis(
                analysis.getDcpa().reset(precondition.state()),
                precision,
                analysis.getViolationConditionHandler().statesOf(pSender));

        summaries.addAll(analysis.summariesOf(result));

        //TODO we only want to combine violations with the same precondition id
        if (!result.getAllViolations().isEmpty()) {
          violations.addAll(analysis.pathsWithCondition(result.getViolationConditionViolations()));
          violations.addAll(analysis.pathsFromOrigin(result.getTargetStates()));
        }
      }
    }
    return new AnalysisResult(
        analysis.deduplicateStatesAndPrecisions(summaries.build()), violations.build());
  }
}
