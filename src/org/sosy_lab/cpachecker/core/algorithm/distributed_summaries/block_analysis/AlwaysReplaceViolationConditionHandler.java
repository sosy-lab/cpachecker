// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssSingleWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssViolationConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.pathrestriction.SegmentedPaths;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Keeps only the violation conditions of the latest message per sending block: an update discards
 * everything previously received from that block.
 */
final class AlwaysReplaceViolationConditionHandler implements DssViolationConditionHandler {

  private final Multimap<String, @NonNull StateAndPrecision> conditions =
      ArrayListMultimap.create();

  private final DssBlockAnalysis analysis;

  AlwaysReplaceViolationConditionHandler(DssBlockAnalysis pAnalysis) {
    analysis = pAnalysis;
  }

  @Override
  public DssMessageProcessing store(DssViolationConditionMessage pReceived)
      throws InterruptedException, SolverException {
    analysis
        .getLogger()
        .log(Level.INFO, "Running forward analysis with respect to error condition");
    ImmutableList<@NonNull StateAndPrecision> received = analysis.deserialize(pReceived);
    DssSingleWorkerStatistics stats = analysis.statistics();
    stats.getStoreViolationConditionStatesTimer().start();
    try {
      String sender = pReceived.getSenderId();
      boolean combineByHash = analysis.getOptions().combineByHash();

      Collection<@NonNull StateAndPrecision> replaced = conditions.removeAll(sender);
      Set<SegmentedPaths> knownWitnesses =
          transformedImmutableSetCopy(replaced, sap -> analysis.witnessOf(sap.state()));

      int equal = 0;
      for (StateAndPrecision condition : received) {
        if (knownWitnesses.contains(analysis.witnessOf(condition.state()))) {
          equal++;
          if (combineByHash) {
            continue;
          }
        }
        if (analysis.shouldProceedBackward(condition.state())) {
          conditions.put(sender, condition);
        }
      }

      if (conditions.get(sender).isEmpty() || equal == received.size()) {
        return DssMessageProcessing.stop();
      }
      return DssMessageProcessing.proceed();
    } finally {
      stats.getStoreViolationConditionStatesTimer().stop();
      stats.getStoreViolationConditionStatesCounter().add(received.size());
    }
  }

  @Override
  public boolean isEmpty() {
    return conditions.isEmpty();
  }

  @Override
  public boolean isEmptyFor(String pSenderId) {
    return conditions.get(pSenderId).isEmpty();
  }

  @Override
  public ImmutableList<AbstractState> statesOf(Optional<String> pSenderId) {
    return transformedImmutableListCopy(
        pSenderId.map(conditions::get).orElse(conditions.values()), StateAndPrecision::state);
  }
}
