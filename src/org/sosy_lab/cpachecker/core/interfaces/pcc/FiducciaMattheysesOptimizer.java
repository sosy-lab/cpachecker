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

import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedGraph;

/**
 * Interface for classes which are used to compute the optimization criterion.
 */
public interface FiducciaMattheysesOptimizer {

  /**
   * Compute the gain of a node move, i.e usually external-internal degree
   * @param node Node which might be moved to another partition
   * @param toPartition Partition where the node probably is moved
   * @param wGraph the graph node belongs to
   * @return its gain with respect to the chosen move
   */
  public int computeGain(int node, int toPartition, int[] nodeToPartition, WeightedGraph wGraph);

  /**
   * Compute a node's internal degree, i.e. an integer describing its connectivity into its own partition
   * @param node node whose degree is computed
   * @param nodeToPartition a map from a nodes onto their belonging partition
   * @param wGraph the graph on which this stuff is computed
   * @return the node's internal degree
   */
  public int computeInternalDegree(int node, int[] nodeToPartition, WeightedGraph wGraph);

  /**
   * Compute a nodes extends degree, i.e the integer which describes the connectivity into the external partition.
   * @param node node whose degree is computed
   * @param toPartition partition where node might be moved
   * @param nodeToPartition a map from a nodes onto their belonging partition
   * @param wGraph the graph on which this stuff is computed
   * @return the node's external degree
   */
  public int computeExternalDegree(int node, int toPartition, int[] nodeToPartition,
      WeightedGraph wGraph);
}
