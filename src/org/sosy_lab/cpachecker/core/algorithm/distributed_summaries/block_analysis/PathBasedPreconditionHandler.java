// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssDebugUtils;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssSingleWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.WithholdingStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraphPath;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Groups preconditions by the path through the block graph along which they were produced.
 *
 * <p>Every path identifies one context of this block, i.e., one chain of contexts in the blocks it
 * passes. An incoming postcondition replaces exactly the context of its own path, so a context that
 * is new and not related to any path seen so far is kept beside all others instead of replacing
 * them, and only the contexts an update affects are explored again. The contexts derived from a
 * replaced one stay until the exploration of the replacement re-derives them: it replaces them,
 * confirms them unchanged, or retracts them.
 *
 * <p>Because a successor keys what it receives by path as well, it has to learn when a context
 * stops producing a postcondition: otherwise it would keep what it derived from the context
 * forever. A context that is removed without a replacement is therefore retracted with the next
 * message (see {@link #consumeRetractedContexts()}), and a retraction received from a predecessor
 * removes the contexts derived from the retracted one and is passed on in turn.
 *
 * <p>Only the root block starts with a context, the unconstrained entry state. Every other block
 * starts without one and learns its contexts from the forward wave that starts at the root, like
 * with {@link AlwaysReplacePreconditionHandler}. Until it has learned all of them, it has to be
 * explored speculatively from the unconstrained entry state (see {@link #mayMissContexts()}), which
 * also covers the contexts a block upstream withholds because they reach a violation that is not
 * resolved yet. Which blocks withhold contexts is passed on with the postconditions.
 */
final class PathBasedPreconditionHandler implements DssPreconditionHandler {

  /**
   * The states covering all received preconditions, grouped by the path through the blocks they
   * were produced along, which decides which of them a new precondition replaces.
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

  /**
   * The paths that were added or changed since the block was last explored. The worker stores a
   * whole burst of messages before it explores once, so this accumulates over several calls to
   * {@link #store} and is only consumed by {@link #consumeChangedContexts()}.
   */
  private final Set<BlockGraphPath> pathsToAnalyze = new LinkedHashSet<>();

  /**
   * The paths of contexts that were removed since the last exploration and not stored again, whose
   * successors may still hold what they derived from them. Consumed by {@link
   * #consumeRetractedContexts()}.
   */
  private final Set<BlockGraphPath> retractedContexts = new LinkedHashSet<>();

  /**
   * Parked states whose covering context was removed, to be added again once the current update is
   * stored, so that the states it brings can cover them.
   */
  private final List<StatesByPath> uncovered = new ArrayList<>();

  /** The predecessors that have sent at least one postcondition message. */
  private final Set<String> predecessorsHeardFrom = new LinkedHashSet<>();

  /**
   * The contexts whose postcondition this block withholds, because exploring them found a violation
   * that has to be resolved first (see {@link PathBasedExplorationEngine}).
   */
  private final Set<BlockGraphPath> withheldContexts = new LinkedHashSet<>();

  /** How often the status this block announces for itself changed. */
  private int withholdingEpoch = 0;

  /** The status this block announced for itself last. */
  private boolean ownStatusWithholding = true;

  /**
   * The latest status of every other block that this block learned from its predecessors, i.e., of
   * every block that can pass states to it, directly or indirectly.
   */
  private final Map<String, WithholdingStatus> withholdingUpstream = new LinkedHashMap<>();

  /** Whether this block learned its own status from a predecessor, i.e., lies on a cycle. */
  private boolean onCycle = false;

  /** The statuses this block published last, to tell whether it has to publish them again. */
  private ImmutableMap<String, WithholdingStatus> announcedWithholding = ImmutableMap.of();

  /**
   * How often a store changed what the next exploration has to do or publish, to tell whether a
   * single store did.
   */
  private int changes = 0;

  private final DssBlockAnalysis analysis;

  /**
   * The unconstrained entry state that the block is explored from speculatively while it may still
   * miss contexts (see {@link #mayMissContexts()}), or empty for the root block. It is created
   * once, so that the violation conditions found from it always carry the same id.
   */
  private final Optional<StateAndPrecision> speculativeStart;

  /** Whether the latest exploration explored the block from {@link #speculativeStart}. */
  private boolean exploredSpeculatively = false;

  PathBasedPreconditionHandler(DssBlockAnalysis pAnalysis) throws InterruptedException {
    analysis = pAnalysis;
    speculativeStart =
        analysis.getBlock().isRoot()
            ? Optional.empty()
            : Optional.of(
                new StateAndPrecision(
                    pAnalysis.makeStartState(true), pAnalysis.makeStartPrecision()));

    if (analysis.getBlock().isRoot()) {
      // The root block has no predecessor, so its only context is the unconstrained entry state.
      // Its path consists of this block alone, and every path through the block graph starts with
      // it.
      AbstractState startState = pAnalysis.withBlockInHistory(pAnalysis.makeStartState(false));
      BlockGraphPath startPath = BlockGraphPath.of(analysis.getBlock().getId());
      preconditions.put(
          startPath,
          new StatesByPath(
              startPath,
              ImmutableList.of(new StateAndPrecision(startState, pAnalysis.makeStartPrecision()))));
      // not published before the block is explored for the first time, see
      // PathBasedExplorationEngine#exploreInitially
      pathsToAnalyze.add(startPath);
    }
  }

  @Override
  public DssMessageProcessing store(DssPostConditionMessage pReceived)
      throws InterruptedException, SolverException, CPAException {
    analysis.getLogger().log(Level.INFO, "Running forward analysis with new precondition");
    predecessorsHeardFrom.add(pReceived.getSenderId());
    // Recorded here, by the receiver, rather than by the sender before it serializes its
    // postcondition, so that a path which loops back to this block contains it once per visit.
    ImmutableList<@NonNull StateAndPrecision> received =
        analysis.withBlockInHistory(analysis.deserialize(pReceived));
    DssSingleWorkerStatistics stats = analysis.statistics();
    stats.getStorePreconditionStatesTimer().start();
    try {
      DssMessageProcessing processing = analysis.shouldProceedForward(received);
      if (!processing.shouldProceed()) {
        return processing;
      }

      int changesBefore = changes;
      mergeWithholding(pReceived.getWithholdingStatus());
      ImmutableList.Builder<BlockGraphPath> continuations = ImmutableList.builder();
      for (BlockGraphPath retracted : pReceived.getRetractedContexts()) {
        // the retracted context of the sender continues into this block as the path extended by it
        BlockGraphPath continuation = extendedByThisBlock(retracted);
        removeWithDescendants(continuation);
        continuations.add(continuation);
      }

      // The sender may have explored a context in the same round in which it retracted one of its
      // ancestors, before it learned that the context itself is gone. What it derived from such a
      // context descends from a retracted one and is outdated already.
      ImmutableList<BlockGraphPath> retractedHere = continuations.build();
      Map<BlockGraphPath, StatesByPath> groups =
          Maps.filterKeys(
              groupStatesByPath(received),
              path -> retractedHere.stream().noneMatch(retracted -> retracted.isPrefixOf(path)));
      storeStates(groups);
      addUncovered();
      if (!getWithholdingStatus().equals(announcedWithholding)) {
        // the successors have to learn about the change, even if no postcondition changes
        changes++;
      }

      if (changes == changesBefore) {
        return DssMessageProcessing.stop();
      }
      return processing;
    } finally {
      stats.getStorePreconditionStatesTimer().stop();
      stats.getStorePreconditionStatesCounter().add(received.size());
    }
  }

  /** Adopts the statuses of the given map that are newer than the known ones. */
  private void mergeWithholding(ImmutableMap<String, WithholdingStatus> pReceived) {
    String self = analysis.getBlock().getId();
    for (Entry<String, WithholdingStatus> entry : pReceived.entrySet()) {
      if (entry.getKey().equals(self)) {
        // this block knows its own status best, but learns that it can pass states to itself
        onCycle = true;
        continue;
      }
      WithholdingStatus known = withholdingUpstream.get(entry.getKey());
      if (known == null || entry.getValue().isNewerThan(known)) {
        withholdingUpstream.put(entry.getKey(), entry.getValue());
      }
    }
  }

  /** Records whether this block withholds the postcondition of the context of the given path. */
  void setWithheld(BlockGraphPath pPath, boolean pWithheld) {
    if (pWithheld) {
      withheldContexts.add(pPath);
    } else {
      withheldContexts.remove(pPath);
    }
  }

  private boolean heardFromAllPredecessors() {
    return predecessorsHeardFrom.containsAll(analysis.getBlock().getPredecessorIds());
  }

  /**
   * The status of this block and of every block upstream of it, as this block knows them.
   *
   * <p>This block counts as withholding not only while it withholds the postcondition of one of its
   * contexts, but also while some predecessor has not sent anything yet: until then, it cannot
   * publish what it would derive from that predecessor either. Whether it misses contexts because
   * of a block further upstream is told by that block's own status.
   */
  ImmutableMap<String, WithholdingStatus> getWithholdingStatus() {
    boolean withholding = !withheldContexts.isEmpty() || !heardFromAllPredecessors();
    if (withholding != ownStatusWithholding) {
      ownStatusWithholding = withholding;
      withholdingEpoch++;
    }
    return ImmutableMap.<String, WithholdingStatus>builder()
        .putAll(withholdingUpstream)
        .put(analysis.getBlock().getId(), new WithholdingStatus(withholdingEpoch, withholding))
        .buildOrThrow();
  }

  /**
   * The statuses to publish with the next message, or empty if the successors already know them.
   * Calling this records them as published.
   */
  Optional<ImmutableMap<String, WithholdingStatus>> consumeWithholdingToAnnounce() {
    ImmutableMap<String, WithholdingStatus> current = getWithholdingStatus();
    if (current.equals(announcedWithholding)) {
      return Optional.empty();
    }
    announcedWithholding = current;
    return Optional.of(current);
  }

  private BlockGraphPath extendedByThisBlock(BlockGraphPath pPath) {
    return BlockGraphPath.of(
        ImmutableList.<String>builder()
            .addAll(pPath.path())
            .add(analysis.getBlock().getId())
            .build());
  }

  /**
   * Stores the given groups of states, each of which replaces what is stored for its path.
   *
   * <p>A replacement leaves the contexts derived from the replaced one untouched. They were
   * computed from the previous states of the path, but exploring the new states re-derives each of
   * them: it either replaces a derived context, finds it unchanged, which confirms that it is still
   * valid, or retracts it. Removing the derived contexts right away would lose the confirmed ones,
   * because an unchanged context is not propagated any further.
   */
  private void storeStates(Map<@NonNull BlockGraphPath, StatesByPath> pGroupedStates)
      throws CPAException, InterruptedException {
    for (StatesByPath newStates :
        Maps.filterKeys(pGroupedStates, this::shouldConsiderPath).values()) {
      StatesByPath existing = preconditions.get(newStates.path);
      if (existing != null && analysis.statesEqual(newStates.states, existing.states)) {
        continue;
      }
      // an earlier generation of the same path that was parked is superseded as well
      coveredStates.entries().removeIf(entry -> entry.getValue().path.equals(newStates.path));
      boolean wasStored = remove(newStates.path);
      add(newStates, wasStored);
    }
  }

  /**
   * Adds the given states, which are not stored under their path at the moment, either as a context
   * to explore or, if another context covers them, as parked states.
   *
   * @param pReplacesStoredContext whether the path held a context until now, so that successors may
   *     hold what they derived from it
   */
  private void add(StatesByPath pStates, boolean pReplacesStoredContext)
      throws CPAException, InterruptedException {
    Optional<BlockGraphPath> coveringPath = findCoveringPath(pStates);
    if (coveringPath.isPresent()) {
      // No need to analyze now, but store in case the covering state is removed later
      coveredStates.put(coveringPath.orElseThrow(), pStates);
      if (pReplacesStoredContext) {
        // What was derived from the previous states of the path is superseded by what the
        // covering context produces.
        retract(pStates.path);
        removeDescendants(pStates.path);
      }
      return;
    }
    preconditions.put(pStates.path, pStates);
    pathsToAnalyze.add(pStates.path);
    // the context is explored again, so its successors learn about it from its postcondition
    retractedContexts.remove(pStates.path);
    changes++;
  }

  /**
   * Removes the context stored under the given path, if any, and re-adds the states it covered.
   *
   * @return whether a context was stored under the path
   */
  private boolean remove(BlockGraphPath pPath) throws CPAException, InterruptedException {
    StatesByPath removed = preconditions.remove(pPath);
    if (removed == null) {
      return false;
    }
    pathsToAnalyze.remove(pPath);
    setWithheld(pPath, false);
    uncovered.addAll(coveredStates.removeAll(pPath));
    return true;
  }

  /** Adds the parked states again whose covering context was removed. */
  private void addUncovered() throws CPAException, InterruptedException {
    while (!uncovered.isEmpty()) {
      StatesByPath states = uncovered.removeFirst();
      if (!preconditions.containsKey(states.path)) {
        add(states, false);
      }
    }
  }

  /**
   * Removes the context of the given path and all contexts derived from it, parked ones included,
   * and retracts the removed contexts.
   */
  private void removeWithDescendants(BlockGraphPath pPath)
      throws CPAException, InterruptedException {
    if (remove(pPath)) {
      retract(pPath);
    }
    coveredStates.entries().removeIf(entry -> entry.getValue().path.equals(pPath));
    uncovered.removeIf(states -> states.path.equals(pPath));
    removeDescendants(pPath);
  }

  /**
   * Removes all contexts derived from the context of the given path, i.e., those whose path extends
   * it, parked ones included, and retracts the removed contexts.
   */
  private void removeDescendants(BlockGraphPath pPath) throws CPAException, InterruptedException {
    coveredStates
        .entries()
        .removeIf(
            entry ->
                pPath.isPrefixOf(entry.getValue().path) && !pPath.equals(entry.getValue().path));
    uncovered.removeIf(states -> pPath.isPrefixOf(states.path) && !pPath.equals(states.path));
    for (BlockGraphPath stored : ImmutableList.copyOf(preconditions.keySet())) {
      if (pPath.isPrefixOf(stored) && !pPath.equals(stored) && remove(stored)) {
        retract(stored);
      }
    }
  }

  /** Remembers that successors have to drop what they derived from the given context. */
  private void retract(BlockGraphPath pPath) {
    if (retractedContexts.add(pPath)) {
      changes++;
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

  private boolean shouldConsiderPath(BlockGraphPath pBlockGraphPath) {
    // Every context originates in the root block. A path that starts with another block could
    // only stem from a state that this block published without a context, which it never does.
    return !pBlockGraphPath.path().getFirst().equals(analysis.getBlock().getId())
        || analysis.getBlock().isRoot();
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

  /**
   * Records that the context of the given path produced no postcondition when it was explored, so
   * that successors drop what they derived from an earlier generation of it. The context itself
   * stays: it is still a valid precondition, it just does not reach the block end.
   */
  void contextProducedNoPostcondition(BlockGraphPath pPath)
      throws CPAException, InterruptedException {
    retract(pPath);
    removeDescendants(pPath);
    addUncovered();
  }

  @Override
  public ImmutableList<@NonNull StateAndPrecision> getKnownPreconditions() {
    return FluentIterable.from(preconditions.values())
        .transformAndConcat(StatesByPath::states)
        .toList();
  }

  /**
   * The paths of the contexts that were added or changed since the last call, i.e., those whose
   * postcondition the successors do not know yet. Calling this consumes them.
   */
  ImmutableSet<BlockGraphPath> consumeChangedContexts() {
    ImmutableSet<BlockGraphPath> changed = ImmutableSet.copyOf(pathsToAnalyze);
    pathsToAnalyze.clear();
    return changed;
  }

  /** The paths of all stored contexts. */
  ImmutableSet<BlockGraphPath> getAllContexts() {
    return ImmutableSet.copyOf(preconditions.keySet());
  }

  /** Whether a context is stored under the given path. */
  boolean hasContext(BlockGraphPath pPath) {
    return preconditions.containsKey(pPath);
  }

  /** The states of the context stored under the given path. */
  Collection<@NonNull StateAndPrecision> getStates(BlockGraphPath pPath) {
    return preconditions.get(pPath).states();
  }

  /**
   * The paths of the contexts that were removed since the last call without being stored again.
   * Calling this consumes them.
   */
  ImmutableList<BlockGraphPath> consumeRetractedContexts() {
    ImmutableList<BlockGraphPath> retracted = ImmutableList.copyOf(retractedContexts);
    retractedContexts.clear();
    return retracted;
  }

  @Override
  public ImmutableList<String> getIdsOfExploredStates() {
    ImmutableList<String> ids = DssPreconditionHandler.super.getIdsOfExploredStates();
    if (speculativeStart.isPresent() && (exploredSpeculatively || mayMissContexts())) {
      return ImmutableList.<String>builder()
          .addAll(ids)
          .add(speculativeStart.orElseThrow().getBlockState().getUniqueId())
          .build();
    }
    return ids;
  }

  /**
   * Records whether the current exploration explores the block from {@link #getSpeculativeStart()},
   * so that the violation conditions it finds are not dropped as conditions of a state this block
   * does not hold (see {@link #getIdsOfExploredStates()}).
   */
  void setExploredSpeculatively(boolean pExploredSpeculatively) {
    exploredSpeculatively = pExploredSpeculatively;
  }

  /**
   * The unconstrained entry state to explore the block from speculatively, or empty for the root
   * block, whose only context already is that state.
   */
  Optional<StateAndPrecision> getSpeculativeStart() {
    return speculativeStart;
  }

  /**
   * Whether the entry of this block may still be reached by states it does not know yet, because
   * some predecessor has not sent anything so far, or because a block upstream withholds the
   * postcondition of one of its contexts. As long as this holds, the block has to be explored
   * speculatively from the unconstrained entry state, or it would not report the violations that
   * the contexts it misses reach.
   */
  boolean mayMissContexts() {
    if (!heardFromAllPredecessors()) {
      return true;
    }
    // A block upstream that withholds a postcondition withholds everything derived from it as
    // well, so this block may miss contexts although every predecessor has sent something.
    return withholdingUpstream.values().stream().anyMatch(WithholdingStatus::withholding)
        || (onCycle && !withheldContexts.isEmpty());
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
            ImmutableList.of(renderedPath, DssDebugUtils.describe(stateAndPrecision)));
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
}
