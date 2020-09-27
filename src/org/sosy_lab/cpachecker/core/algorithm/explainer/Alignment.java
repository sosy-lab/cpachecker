// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.explainer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class Alignment is used for making alignments between two Elements. It guarantees that the size
 * of the two contained lists remains always the same.
 *
 * @param <T> T is here either a CFAEdge or a ARGState
 */
class Alignment<T> implements Iterable<T> {

  private final List<T> counterexample = new ArrayList<>();
  private final List<T> safePath = new ArrayList<>();

  public void addPair(T counterexampleElement, T safePathElement) {
    counterexample.add(counterexampleElement);
    safePath.add(safePathElement);
  }

  public T getSafePathElement(int i) {
    return safePath.get(i);
  }

  public T getCounterexampleElement(int i) {
    return counterexample.get(i);
  }

  public List<T> getCounterexample() {
    return counterexample;
  }

  public List<T> getSafePath() {
    return safePath;
  }

  @Override
  public Iterator<T> iterator() {
    List<T> joinLists = new ArrayList<>();
    for (int i = 0; i < counterexample.size(); i++) {
      joinLists.add(counterexample.get(i));
      joinLists.add(safePath.get(i));
    }
    return joinLists.iterator();
  }
}
