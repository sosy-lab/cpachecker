/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.util;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

/** Instances of this class map textual ids to numeric ids. */
public class NumericIdProvider {

  private final boolean attemptParsing;

  private final TreeRangeSet<Integer> usedIds =
      TreeRangeSet.create(Collections.singleton(Range.lessThan(0)));

  private final Map<String, Integer> mappedIds = Maps.newHashMap();

  private NumericIdProvider(boolean pAttemptParsing) {
    attemptParsing = pAttemptParsing;
  }

  /**
   * Provides a numeric id for the given textual id. Calling this function multiple times on the
   * same textual id is guaranteed to yield the same numeric id each time, and calling the function
   * on different textual ids is guaranteed to provide different numeric ids.
   *
   * @param pId the textual id.
   * @return a numeric id.
   */
  public int provideNumericId(String pId) {
    Integer id = mappedIds.get(pId);
    if (id != null) {
      return id;
    }
    if (attemptParsing) {
      try {
        id = Integer.parseInt(pId);
      } catch (NumberFormatException e) {
        // Not already a numeric id, so we will need to generate an artificial number
      }
    }
    // If we did not attempt parsing, or parsing failed, or the parsed id is already in use,
    // we need to generate an artificial numeric id:
    if (id == null || usedIds.contains(id)) {
      RangeSet<Integer> remainingIds = usedIds.complement();
      Iterator<Range<Integer>> rangeIterator = remainingIds.asRanges().iterator();
      while (rangeIterator.hasNext()) {
        Range<Integer> range = rangeIterator.next();
        ContiguousSet<Integer> contiguousRange =
            ContiguousSet.create(range, DiscreteDomain.integers());
        if (!contiguousRange.isEmpty()) {
          id = contiguousRange.first();
          break;
        }
      }
    }
    assert id != null && !usedIds.contains(id);
    mappedIds.put(pId, id);
    usedIds.add(Range.open(id - 1, id + 1));
    return id;
  }

  public static NumericIdProvider create() {
    return new NumericIdProvider(true);
  }
}
