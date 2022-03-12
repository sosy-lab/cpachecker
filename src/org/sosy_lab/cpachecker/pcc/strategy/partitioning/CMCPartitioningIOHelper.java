// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy.partitioning;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.pcc.strategy.AbstractStrategy.PCStrategyStatistics;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.PartialReachedSetDirectedGraph;
import org.sosy_lab.cpachecker.util.Pair;

public class CMCPartitioningIOHelper extends PartitioningIOHelper {

  private List<int[][]> savedSuccessors;
  private AbstractState root;

  private final Set<ARGState> automatonStates;
  private final Set<ARGState> unexploredStates;

  public CMCPartitioningIOHelper(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final Set<ARGState> pAutomatonStates,
      final Set<ARGState> pUnexploredStates,
      final @Nullable ARGState pRoot)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pShutdownNotifier, true);
    automatonStates = pAutomatonStates;
    unexploredStates = pUnexploredStates;
    if (pRoot != null) {
      root = pRoot.getWrappedState();
    }
  }

  public CMCPartitioningIOHelper(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    this(pConfig, pLogger, pShutdownNotifier, ImmutableSet.of(), ImmutableSet.of(), null);
  }

  public int @Nullable [][] getEdgesForPartition(final int pIndex) {
    if (0 <= pIndex && pIndex < getNumPartitions() && pIndex < savedSuccessors.size()) {
      return savedSuccessors.get(pIndex);
    }
    return null;
  }

  @Override
  protected void saveInternalProof(
      final int size,
      final Pair<PartialReachedSetDirectedGraph, List<Set<Integer>>> pPartitionDescription) {
    super.saveInternalProof(size, pPartitionDescription);
    savedSuccessors = new ArrayList<>(pPartitionDescription.getSecond().size());
    int[][] partitionSuccessors;
    List<Integer> successors;
    Pair<AbstractState[], AbstractState[]> partition;
    Iterator<Integer> partitionNodes;
    int nodeIndex;
    ARGState node;

    int[] empty = {};

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
              partitionSuccessors[j][k] =
                  findSuccessorIndex(
                      ((ARGState) pPartitionDescription.getFirst().getNode(successors.get(k)))
                          .getWrappedState(),
                      partition.getFirst(),
                      partition.getSecond());
            }
          }
        }
      }
    }
  }

  @Override
  public void writePartition(
      final ObjectOutputStream pOut,
      final Set<Integer> pPartition,
      final PartialReachedSetDirectedGraph pPartialReachedSetDirectedGraph)
      throws IOException {
    super.writePartition(pOut, pPartition, pPartialReachedSetDirectedGraph);

    AbstractState[] partitionNodes = pPartialReachedSetDirectedGraph.getSetNodes(pPartition, true);
    AbstractState[] externalNodes =
        pPartialReachedSetDirectedGraph.getSuccessorNodesOutsideSet(pPartition, true);
    int[][] successorLinks = new int[pPartition.size()][];

    int count = 0;
    int[] empty = {};
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
                findSuccessorIndex(
                    pPartialReachedSetDirectedGraph.getNode(successors.get(i)),
                    partitionNodes,
                    externalNodes);
            Preconditions.checkState(successorLinks[count][i] != -1);
          }
        }
      }
      count++;
    }
    pOut.writeObject(successorLinks);
  }

  private int findSuccessorIndex(
      final AbstractState pNode,
      final AbstractState[] pPartitionNodes,
      final AbstractState[] pExternalNodes) {
    for (int i = 0; i < pPartitionNodes.length; i++) {
      if (pNode == pPartitionNodes[i]) {
        return i;
      }
    }
    for (int i = 0; i < pExternalNodes.length; i++) {
      if (pNode == pPartitionNodes[i]) {
        return pPartitionNodes.length + i;
      }
    }
    return -1;
  }

  private boolean isUnexplored(final ARGState pNode) {
    return unexploredStates.contains(pNode);
  }

  private boolean isAutomatonState(final ARGState node) {
    return automatonStates.contains(node);
  }

  @Override
  public void readPartition(final ObjectInputStream pIn, final PCStrategyStatistics pStats)
      throws ClassNotFoundException, IOException {
    super.readPartition(pIn, pStats);
    savedSuccessors.add((int[][]) pIn.readObject());
  }

  @Override
  public void readPartition(
      final ObjectInputStream pIn, final PCStrategyStatistics pStats, final Lock pLock)
      throws ClassNotFoundException, IOException {
    checkArgument(pLock != null, "Cannot protect against parallel access");
    pLock.lock();
    try {
      readPartition(pIn, pStats);
    } finally {
      pLock.unlock();
    }
  }

  @Override
  public void writeMetadata(
      final ObjectOutputStream pOut, final int pReachedSetSize, final int pNumPartitions)
      throws IOException {
    super.writeMetadata(pOut, pReachedSetSize, pNumPartitions);
    pOut.writeObject(root);
  }

  @Override
  public void readMetadata(final ObjectInputStream pIn, final boolean pSave) throws IOException {
    super.readMetadata(pIn, pSave);
    if (pSave) {
      savedSuccessors = new ArrayList<>(getNumPartitions());
      try {
        root = (AbstractState) pIn.readObject();
      } catch (ClassNotFoundException e) {
        root = null;
      }
    } else {
      try {
        pIn.readObject();
      } catch (ClassNotFoundException e) {
        throw new AssertionError(e);
      }
    }
  }

  public @Nullable AbstractState getRoot() {
    return root;
  }
}
