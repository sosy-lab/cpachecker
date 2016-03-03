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
package org.sosy_lab.cpachecker.core.interfaces.pcc;

import java.util.Map;

import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedGraph;

/**
 * Interface for computing matchings. Matchings are given implicitly by a map from node number to a new node.
 * I.e. if 2 nodes are matched, they point to the same super node.
 */
public interface MatchingGenerator {

  /**
   * Compute a matching on a weighted graph. I.e. >>each<< node has to be mapped onto another node number.
   * And a node cannot be matched twice
   * @param wGraph the graph, on which a matching is computed
   * @return  the computed matching
   */
  public Map<Integer, Integer> computeMatching(WeightedGraph wGraph);
}
