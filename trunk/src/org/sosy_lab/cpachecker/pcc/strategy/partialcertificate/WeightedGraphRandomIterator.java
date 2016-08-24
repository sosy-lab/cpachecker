/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.pcc.strategy.partialcertificate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;


public class WeightedGraphRandomIterator implements Iterator<WeightedNode> {

  private final WeightedGraph wGraph;
  private final int size;
  private int current;
  private final List<Integer> permutation;

  public WeightedGraphRandomIterator(WeightedGraph wGraph) {
    super();
    this.wGraph = wGraph;
    size=wGraph.getNumNodes();
    permutation=shuffledIndices(size);
    current = 0;
  }

  @Override
  public boolean hasNext() {
    if(current<size){
      return true;
    }
    return false;
  }

  @Nullable
  @Override
  public WeightedNode next() {
    int nodeIndex=permutation.get(current++);
    return wGraph.getNode(nodeIndex);
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();

  }
  /**
   * Compute permutation of 0..n-1 to randomly iterate over an array
   * @param n number of list entries
   * @return a list containing 0..n-1 in a randomized order
   */
  private List<Integer> shuffledIndices(int n) {
    List<Integer> permutation = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      permutation.add(i);
    }
    Collections.shuffle(permutation);
    return permutation;
  }
}
