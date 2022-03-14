// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy.partialcertificate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import org.checkerframework.checker.nullness.qual.Nullable;

public class WeightedGraphRandomIterator implements Iterator<WeightedNode> {

  private static final Random rnd = new Random(0);

  private final WeightedGraph wGraph;
  private final int size;
  private int current;
  private final List<Integer> permutation;

  public WeightedGraphRandomIterator(WeightedGraph wGraph) {
    this.wGraph = wGraph;
    size = wGraph.getNumNodes();
    permutation = shuffledIndices(size);
    current = 0;
  }

  @Override
  public boolean hasNext() {
    if (current < size) {
      return true;
    }
    return false;
  }

  @Nullable
  @Override
  public WeightedNode next() {
    int nodeIndex = permutation.get(current++);
    return wGraph.getNode(nodeIndex);
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
  /**
   * Compute permutation of 0..n-1 to randomly iterate over an array
   *
   * @param n number of list entries
   * @return a list containing 0..n-1 in a randomized order
   */
  private static List<Integer> shuffledIndices(int n) {
    List<Integer> permutation = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      permutation.add(i);
    }
    Collections.shuffle(permutation, rnd);
    return permutation;
  }
}
