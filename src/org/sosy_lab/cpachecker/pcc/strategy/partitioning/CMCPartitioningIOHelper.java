/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.pcc.strategy.partitioning;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.pcc.strategy.AbstractStrategy.PCStrategyStatistics;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.PartialReachedSetDirectedGraph;

import com.google.common.base.Preconditions;


public class CMCPartitioningIOHelper extends PartitioningIOHelper{

  private List<int[][]> savedSuccessors;
  private final Set<ARGState> automatonStates;
  private final Set<ARGState> unexploredStates;

  public CMCPartitioningIOHelper(Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier,
      Set<ARGState> pAutomatonStates, Set<ARGState> pUnexploredStates) throws InvalidConfigurationException {
    super(pConfig, pLogger, pShutdownNotifier);
    automatonStates = pAutomatonStates;
    unexploredStates = pUnexploredStates;
  }

  public CMCPartitioningIOHelper(Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    this(pConfig, pLogger, pShutdownNotifier, Collections.<ARGState> emptySet(), Collections.<ARGState> emptySet());
  }

  @Override
  protected void saveInternalProof(final int size,
      final Pair<PartialReachedSetDirectedGraph, List<Set<Integer>>> pPartitionDescription) {
    super.saveInternalProof(size, pPartitionDescription);
    savedSuccessors = new ArrayList<>(pPartitionDescription.getSecond().size());

    int[][] partitionSuccessors;
    List<Integer> successors;
    Pair<AbstractState[], AbstractState[]> partition;
    Iterator<Integer> partitionNodes;
    int nodeIndex;
    ARGState node;

    int[] empty = new int[0];

    for (int i = 0; i < savedSuccessors.size(); i++) {
      partition = getPartition(i);

      partitionSuccessors = new int[partition.getFirst().length][];
      savedSuccessors.add(partitionSuccessors);
      partitionNodes = pPartitionDescription.getSecond().get(i).iterator();

      for (int j = 0; j < partitionSuccessors.length; j++) {
        nodeIndex = partitionNodes.next();
        node = (ARGState) pPartitionDescription.getFirst().getNode(nodeIndex);
        assert (node.getWrappedState() == partition.getFirst()[j]);

        if (isAutomatonState(node)) {
          if (isUnexplored(node)) {
            partitionSuccessors[j] = empty;
          } else {
            successors = pPartitionDescription.getFirst().getAdjacencyList().get(nodeIndex);
            partitionSuccessors[j] = new int[successors.size()];

            for (int k = 0; k < partitionSuccessors[j].length; k++) {
              partitionSuccessors[j][k] = findSuccessorIndex(
                  ((ARGState) pPartitionDescription.getFirst().getNode(successors.get(k))).getWrappedState(),
                  partition.getFirst(), partition.getSecond());
            }
          }
        }
      }
    }
  }

  @Override
  public void writePartition(final ObjectOutputStream pOut, final Set<Integer> pPartition,
      final PartialReachedSetDirectedGraph pPartialReachedSetDirectedGraph) throws IOException {
    super.writePartition(pOut, pPartition, pPartialReachedSetDirectedGraph);

    AbstractState[] partitionNodes = pPartialReachedSetDirectedGraph.getSetNodes(pPartition, true);
    AbstractState[] externalNodes = pPartialReachedSetDirectedGraph.getSuccessorNodesOutsideSet(pPartition, true);
    int[][] successorLinks = new int[pPartition.size()][];

    int count = 0;
    int[] empty = new int[0];
    ARGState node;
    List<Integer> successors;
    for (Integer partitionNode : pPartition) {
      node = (ARGState) pPartialReachedSetDirectedGraph.getNode(partitionNode);
      if (isAutomatonState(node)) {
        if (isUnexplored(node)) {
          successorLinks[count] = empty;
        } else {
          successors = pPartialReachedSetDirectedGraph.getAdjacencyList().get(partitionNode);
          successorLinks[count] = new int[successors.size()];
          for (int i = 0; i < successorLinks[count].length; i++) {
            successorLinks[count][i] =
                findSuccessorIndex(pPartialReachedSetDirectedGraph.getNode(successors.get(i)), partitionNodes,
                    externalNodes);
            Preconditions.checkState(successorLinks[count][i] != -1);
          }
        }
      }
      count++;
    }
    pOut.writeObject(successorLinks);
  }

  private int findSuccessorIndex(AbstractState pNode, AbstractState[] pPartitionNodes, AbstractState[] pExternalNodes) {
    for (int i = 0; i < pPartitionNodes.length; i++) {
      if (pNode == pPartitionNodes[i]) { return i; }
    }
    for (int i = 0; i < pExternalNodes.length; i++) {
      if (pNode == pPartitionNodes[i]) { return pPartitionNodes.length + i; }
    }
    return -1;
  }

  private boolean isUnexplored(ARGState pNode) {
    return unexploredStates.contains(pNode);
  }

  private boolean isAutomatonState(ARGState node) {
    return automatonStates.contains(node);
  }

  @Override
  public void readPartition(final ObjectInputStream pIn, final PCStrategyStatistics pStats)
      throws ClassNotFoundException, IOException {
    super.readPartition(pIn, pStats);
    savedSuccessors.add((int[][]) pIn.readObject());
  }


  @Override
  public void readPartition(final ObjectInputStream pIn, final PCStrategyStatistics pStats, final Lock pLock)
      throws ClassNotFoundException, IOException {
    if (pLock == null) { throw new IllegalArgumentException("Cannot protect against parallel access"); }
    pLock.lock();
    try {
      readPartition(pIn, pStats);
    } finally {
      pLock.unlock();
    }
  }

  @Override
  public void readMetadata(final ObjectInputStream pIn, final boolean pSave) throws IOException {
    super.readMetadata(pIn, pSave);
    if (pSave) {
      savedSuccessors = new ArrayList<>(getNumPartitions());
    }
  }

}
