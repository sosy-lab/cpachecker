/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.summary.summaryGeneration;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.Summary;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.SummaryManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Waitlist for figuring out which states should be processed once
 * a new summary is available.
 */
final class SummaryWaitlist {

  /**
   * Map from (non-weakened) callsites to the summary computation states they occur in.
   * The idea is to return those states as successors once a new summary comes in.
   */
  private final Map<String, Multimap<AbstractState, SummaryComputationState>> data;
  private final SummaryManager wrappedManager;
  private final AbstractDomain wrappedAbstractDomain;

  SummaryWaitlist(SummaryManager pSummaryManager, AbstractDomain pWrappedAbstractDomain) {
    wrappedAbstractDomain = pWrappedAbstractDomain;
    data = new HashMap<>();
    wrappedManager = pSummaryManager;
  }

  /**
   * Get all summary computation requests which now should be recomputed
   * due to the availability of the new summaries.
   *
   * @param newSummaries Collection of newly available summaries.
   * @param pSummaryComputationState State to set the {@code parent} state on the returned state.
   * @return Summary computation requests to reinsert into the toplevel reached set.
   */
  Collection<SummaryComputationState> getToRecompute(
      Collection<Summary> newSummaries,
      long timestamp,
      SummaryComputationState pSummaryComputationState) {
    if (newSummaries.isEmpty()) {
      return Collections.emptyList();
    }

    String partitionKey = wrappedManager.getSummaryPartition(newSummaries.iterator().next());
    Multimap<AbstractState, SummaryComputationState> partition = data.get(partitionKey);
    Set<SummaryComputationState> out = new HashSet<>();
    for (Summary summary : newSummaries) {
      for (AbstractState callsite : partition.keySet()) {
        if (wrappedManager.isCallsiteLessThanSummary(callsite, summary)) {
          for (SummaryComputationState scs : partition.get(callsite)) {
            ReachedSet reached = scs.getReached();
            reached.reAddToWaitlist(callsite);
            out.add(scs.withNewTimestamp(timestamp, pSummaryComputationState));
          }
        }
      }
    }
    return out;
  }

  /**
   * State the summary computation request:
   * during the intraprocedural analysis, at {@code pCallsite}
   * no <em>sound</em> matching summary was found.
   * Thus, once a summary is generated, the state should be revisited.
   *
   * @param pCallsite Non-weakened abstract state associated with the callsite.
   * @param pState State to "resurrect" (add to waitlist) once a new summary is available.
   */
  void registerDependency(AbstractState pCallsite, SummaryComputationState pState)
      throws CPAException, InterruptedException {
    String partitionKey = wrappedManager.getCallstatePartition(pCallsite);
    Multimap<AbstractState, SummaryComputationState> partition =
        data.computeIfAbsent(partitionKey, k -> HashMultimap.create());
    for (SummaryComputationState scs : partition.get(pCallsite)) {
      if (wrappedAbstractDomain.isLessOrEqual(pState.getEntryState(), scs.getEntryState())) {
        return;
      }
    }
    partition.put(pCallsite, pState);
  }
}
