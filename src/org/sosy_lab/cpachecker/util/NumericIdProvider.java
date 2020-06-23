// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Instances of this class map textual ids to numeric ids. */
public class NumericIdProvider {

  private final boolean attemptParsing;

  private final TreeRangeSet<Integer> usedIds =
      TreeRangeSet.create(Collections.singleton(Range.lessThan(0)));

  private final Map<String, Integer> mappedIds = new HashMap<>();

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
      for (Range<Integer> range : remainingIds.asRanges()) {
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
