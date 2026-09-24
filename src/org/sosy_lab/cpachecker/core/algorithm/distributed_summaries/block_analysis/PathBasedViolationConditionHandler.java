// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;
import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalysis.blockStateOf;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssDebugUtils;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssSingleWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssViolationConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Keeps only the violation conditions of the latest message per sending block: an update discards
 * everything previously received from that block.
 */
final class PathBasedViolationConditionHandler implements DssViolationConditionHandler {

  private final Map<String, Multimap<String, StateAndPrecision>> conditions;

  private final DssBlockAnalysis analysis;

  /**
   * The conditions stored for all senders, grouped into classes of equal conditions, in the order
   * in which the classes arose. Updating the classes with what a message adds and removes spares
   * comparing every pair of stored conditions on each message.
   */
  private final List<ConditionClass> classes = new ArrayList<>();

  /** The class of each stored condition. */
  private final IdentityHashMap<StateAndPrecision, ConditionClass> classOf =
      new IdentityHashMap<>();

  /** Conditions that still need to be explored, with equivalent ones kept only once. */
  private ImmutableList<StateAndPrecision> conditionsToExplore = ImmutableList.of();

  PathBasedViolationConditionHandler(DssBlockAnalysis pAnalysis) {
    conditions = new LinkedHashMap<>();
    analysis = pAnalysis;
  }

  @Override
  public DssMessageProcessing store(DssViolationConditionMessage pReceived)
      throws InterruptedException, CPAException {
    analysis
        .getLogger()
        .log(Level.INFO, "Running forward analysis with respect to error condition");
    ImmutableList<@NonNull StateAndPrecision> received = analysis.deserialize(pReceived);
    DssSingleWorkerStatistics stats = analysis.statistics();
    stats.getStoreViolationConditionStatesTimer().start();

    try {
      conditions.putIfAbsent(pReceived.getSenderId(), ArrayListMultimap.create());
      Multimap<String, StateAndPrecision> mapForSuccessor = conditions.get(pReceived.getSenderId());
      ImmutableListMultimap<String, @NonNull StateAndPrecision> vcsByID =
          indexByPrecondition(received);
      Set<String> remaining = ImmutableSet.copyOf(pReceived.getRemainingPreconditions());
      if (!changesConditions(mapForSuccessor, vcsByID, remaining)) {
        // Deciding this per precondition of the sender spares recomputing the conditions over all
        // senders, which compares every pair of conditions, for the frequent message that only
        // repeats what this block knows.
        return DssMessageProcessing.stop();
      }
      Set<StateAndPrecision> before = identitySetOf(mapForSuccessor.values());
      // replace all newly received keys
      vcsByID.keySet().forEach(id -> mapForSuccessor.removeAll(id));
      mapForSuccessor.putAll(vcsByID);
      List<String> toRemove =
          mapForSuccessor.keySet().stream().filter(k -> !remaining.contains(k)).toList();
      toRemove.forEach(remainingId -> mapForSuccessor.removeAll(remainingId));
      Set<StateAndPrecision> after = identitySetOf(mapForSuccessor.values());

      // The added conditions join their classes before the removed ones leave them, so that a
      // condition replaced by an equal one keeps its class.
      boolean changed = false;
      for (StateAndPrecision added : Sets.difference(after, before)) {
        changed |= addToClass(added);
      }
      for (StateAndPrecision removed : Sets.difference(before, after)) {
        changed |= removeFromClass(removed);
      }
      if (!changed) {
        return DssMessageProcessing.stop();
      }
      conditionsToExplore = transformedImmutableListCopy(classes, c -> c.representative);
      return DssMessageProcessing.proceed();
    } finally {
      stats.getStoreViolationConditionStatesTimer().stop();
      stats.getStoreViolationConditionStatesCounter().add(received.size());
    }
  }

  private static Set<StateAndPrecision> identitySetOf(Collection<StateAndPrecision> pConditions) {
    Set<StateAndPrecision> set = Sets.newIdentityHashSet();
    set.addAll(pConditions);
    return set;
  }

  /**
   * Adds the given condition to the class of the conditions it equals, or to a new class.
   *
   * <p>Conditions are compared with equality, not coverage: a condition that unrolls one more loop
   * iteration is the previous one's path formula conjoined with the next loop guard, so it is
   * subsumed by the condition it refines. Under coverage such a condition would never look new,
   * this block would stop instead of exploring it, and the loop would never unroll far enough to
   * reach a violation that only occurs after several iterations.
   *
   * @return whether a new class arose, i.e., the conditions to explore changed
   */
  private boolean addToClass(StateAndPrecision pCondition)
      throws CPAException, InterruptedException {
    for (ConditionClass conditionClass : classes) {
      if (analysis.isSameViolationCondition(pCondition, conditionClass.representative)) {
        conditionClass.members.add(pCondition);
        classOf.put(pCondition, conditionClass);
        return false;
      }
    }
    ConditionClass conditionClass = new ConditionClass(pCondition);
    classes.add(conditionClass);
    classOf.put(pCondition, conditionClass);
    return true;
  }

  /**
   * Removes the given condition from its class, and the class if it has no members anymore.
   *
   * @return whether the class was removed, i.e., the conditions to explore changed
   */
  private boolean removeFromClass(StateAndPrecision pCondition) {
    ConditionClass conditionClass = checkNotNull(classOf.remove(pCondition));
    conditionClass.members.remove(pCondition);
    if (conditionClass.members.isEmpty()) {
      classes.remove(conditionClass);
      return true;
    }
    return false;
  }

  /**
   * Stored conditions that are equal to each other, one of which is explored for all of them.
   *
   * <p>The representative is the condition that founded the class, and stays so for as long as the
   * class has members, even after it was removed itself: a condition replaced by an equal one is
   * then still the same condition for the exploration, which does not check the contexts against it
   * again (see {@link PathBasedExplorationEngine}).
   */
  private static final class ConditionClass {

    private final StateAndPrecision representative;

    private final Set<StateAndPrecision> members = Sets.newIdentityHashSet();

    private ConditionClass(StateAndPrecision pRepresentative) {
      representative = pRepresentative;
      members.add(pRepresentative);
    }
  }

  /**
   * Whether storing the given conditions of one sender changes what is stored for it: a
   * precondition of the sender gets new conditions, or one with conditions is gone.
   */
  private boolean changesConditions(
      Multimap<String, StateAndPrecision> pStored,
      ImmutableListMultimap<String, @NonNull StateAndPrecision> pReceived,
      Set<String> pRemaining)
      throws CPAException, InterruptedException {
    if (!pRemaining.containsAll(pStored.keySet())) {
      return true;
    }
    for (String id : pReceived.keySet()) {
      if (pRemaining.contains(id)
          && !analysis.violationConditionsEqual(pReceived.get(id), pStored.get(id))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Indexes the received violation conditions by the id of the precondition of the sender they were
   * computed for.
   *
   * <p>Violation conditions that reach the same program point may be combined into a single state,
   * whose id then combines the ids of all preconditions it stems from. Such a state is indexed
   * under each of these ids, so that it is replaced as soon as any of them is superseded or gone.
   */
  private ImmutableListMultimap<String, @NonNull StateAndPrecision> indexByPrecondition(
      List<@NonNull StateAndPrecision> pViolationConditions) {
    ImmutableListMultimap.Builder<String, @NonNull StateAndPrecision> vcsByID =
        ImmutableListMultimap.builder();
    for (StateAndPrecision violationCondition : pViolationConditions) {
      for (String id :
          BlockState.splitUniqueId(blockStateOf(violationCondition.state()).getUniqueId())) {
        vcsByID.put(id, violationCondition);
      }
    }
    return vcsByID.build();
  }

  @Override
  public ImmutableList<AbstractState> states() {
    return transformedImmutableListCopy(conditionsToExplore, StateAndPrecision::state);
  }

  /** Renders {@link #conditions} and {@link #conditionsToExplore} for debugging. */
  @Override
  public String toString() {
    List<List<String>> conditionRows = new ArrayList<>();
    for (Entry<String, Multimap<String, StateAndPrecision>> bySender : conditions.entrySet()) {
      String sender = bySender.getKey();
      for (Entry<String, StateAndPrecision> byPrecondition : bySender.getValue().entries()) {
        conditionRows.add(
            ImmutableList.of(
                sender,
                byPrecondition.getKey(),
                DssDebugUtils.oneLine(byPrecondition.getValue().state())));
        sender = "";
      }
    }
    String conditionsBody =
        conditionRows.isEmpty()
            ? "<none>"
            : DssDebugUtils.table(
                ImmutableList.of("from", "preconditionId", "state"), conditionRows);

    List<List<String>> toExploreRows = new ArrayList<>();
    int index = 0;
    for (StateAndPrecision stateAndPrecision : conditionsToExplore) {
      toExploreRows.add(
          ImmutableList.of(
              Integer.toString(index++), DssDebugUtils.oneLine(stateAndPrecision.state())));
    }
    String toExploreBody =
        toExploreRows.isEmpty()
            ? "<none>"
            : DssDebugUtils.table(ImmutableList.of("#", "state"), toExploreRows);

    String body =
        "conditions ("
            + conditionRows.size()
            + " states from "
            + conditions.size()
            + " senders):\n"
            + DssDebugUtils.indent("  ", conditionsBody)
            + "\n\nconditions to explore ("
            + conditionsToExplore.size()
            + "):\n"
            + DssDebugUtils.indent("  ", toExploreBody);
    return DssDebugUtils.box("ViolationConditions of Block " + analysis.getBlock().getId(), body);
  }
}
