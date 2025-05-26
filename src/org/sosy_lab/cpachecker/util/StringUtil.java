// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.ImmutableSortedSet;
import java.util.Comparator;

public class StringUtil {

  /*
   * Create a compact string describing a set of integers in a human-readable way.
   * Continuous ranges will be abbreviated by a dash, e.g. "1-5",
   * non-continous integers will be separated by commas.
   * All integers will be sorted and duplicates are ignored.
   * No negative values may be present.
   * Example: The integers {7,5,1,3,4,5,7} will be written as "1,3-5,7"
   */
  public static StringBuilder convertIntegerRangesToStringCollapsed(Iterable<Integer> pNumbers) {
    ImmutableSortedSet<Integer> numbers = from(pNumbers).toSortedSet(Comparator.naturalOrder());
    StringBuilder builder = new StringBuilder();
    int state = 0;
    int lastNumber = -1;
    for (Integer currentNumber : numbers) {
      checkArgument(currentNumber >= 0);
      switch (state) {
        case 0 -> {
          // initial state
          builder.append(currentNumber);
          state = 1;
        }
        case 1 -> {
          // builder ends with lastNumber
          if (currentNumber != lastNumber + 1) {
            builder.append(",").append(currentNumber);
            // stay in state 1
          } else if (currentNumber.equals(numbers.last())) {
            builder.append(",").append(currentNumber);
            state = -1; // we should be finished, next transition would lead to exception
          } else {
            state = 2;
          }
        }
        case 2 -> {
          // builder ends with lastNumber-1 (still undecided whether this becomes a range)
          if (currentNumber != lastNumber + 1) {
            builder.append(",").append(lastNumber).append(",").append(currentNumber);
            state = 1;
          } else if (currentNumber.equals(numbers.last())) {
            builder.append("-").append(currentNumber);
            state = -1; // we should be finished, next transition would lead to exception
          } else {
            state = 3;
          }
        }
        case 3 -> {
          // builder ends with number that is smaller than lastNumber (we are in a range)
          if (currentNumber != lastNumber + 1) {
            builder.append("-").append(lastNumber).append(",").append(currentNumber);
            state = 1;
          } else if (currentNumber.equals(numbers.last())) {
            builder.append("-").append(currentNumber);
            state = -1; // we should be finished, next transition would lead to exception
          } else {
            // stay in state 3
          }
        }
        default -> throw new RuntimeException("Unexpected state in string generation automaton");
      }
      lastNumber = currentNumber;
    }
    return builder;
  }
}
