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

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.math.LongMath;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.Summary;

/**
 * Stateful datastructure singleton for storing summaries.
 */
public class SummaryStorage {

  /**
   * Store summaries grouped by partition key.
   *
   * @see org.sosy_lab.cpachecker.cpa.summary.interfaces.SummaryManager#getSummaryPartition
   * @see org.sosy_lab.cpachecker.cpa.summary.interfaces.SummaryManager#getCallstatePartition
   */
  private final Multimap<String, Summary> data;

  /**
   * Set of summaries which are sound and will not require further recomputations.
   */
  private final Set<Summary> soundSummaries;
  private long timestamp = 0;

  SummaryStorage() {
    data = HashMultimap.create();
    soundSummaries = new HashSet<>();
  }

  /**
   * A larger timestamp states that a new summary has appeared in the meantime.
   */
  public long getTimestamp() {
    return timestamp;
  }

  void add(String pPartition, Summary pSummary, boolean pIsSound) {
    data.put(pPartition, pSummary);
    if (pIsSound) {
      soundSummaries.add(pSummary);
    }
    updateTimestamp();
  }

  void addAll(String pPartition, Collection<Summary> pSummaries) {
    data.get(pPartition).addAll(pSummaries);
    updateTimestamp();
  }

  void removeAll(String pPartition, Collection<Summary> pSummaries) {
    data.get(pPartition).removeAll(pSummaries);
    soundSummaries.removeAll(pSummaries);
    updateTimestamp();
  }

  void addToSound(Summary pSummary) {
    soundSummaries.add(pSummary);
  }

  public Collection<Summary> get(String pPartition) {
    return Collections.unmodifiableCollection(data.get(pPartition));
  }

  public boolean isSound(Summary pSummary) {
    return soundSummaries.contains(pSummary);
  }

  private void updateTimestamp() {
    timestamp = LongMath.checkedAdd(timestamp, 1);
  }

  @Override
  public String toString() {
    return data.asMap().entrySet().stream().map(
        e -> e.getKey() + ":\n\n" + Joiner.on("\n*").join(
            e.getValue()
        )
    ).reduce(String::concat).toString();
  }
}
