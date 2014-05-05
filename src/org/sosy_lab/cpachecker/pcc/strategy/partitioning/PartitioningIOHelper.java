/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.pcc.BalancedGraphPartitioner;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PartialReachedConstructionAlgorithm;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.ARGBasedPartialReachedSetConstructionAlgorithm;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.MonotoneTransferFunctionARGBasedPartialReachedSetConstructionAlgorithm;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.PartialCertificateTypeProvider;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.PartialReachedSetDirectedGraph;
import org.sosy_lab.cpachecker.util.CPAs;

@Options(prefix = "pcc")
public class PartitioningIOHelper {

  @Option(
      description = "Specifies the maximum size of the partition. This size is used to compute the number of partitions if a proof (reached set) should be written. Default value 0 means always a single partition.")
  private int maxNumElemsPerPartition = 0;

  @Option(description = "Heuristic for computing partitioning of proof (partial reached set).")
  private PartitioningHeuristics partitioning = PartitioningHeuristics.RANDOM;

  public enum PartitioningHeuristics {
    RANDOM,
    OPTIMAL
  }

  private final LogManager logger;
  private final PartialReachedConstructionAlgorithm partialConstructor;
  private final BalancedGraphPartitioner partitioner;
  private int savedReachedSetSize;
  private int numPartitions;
  private List<Pair<AbstractState[], AbstractState[]>> partitions;

  public PartitioningIOHelper(final Configuration pConfig, final LogManager pLogger,
      ShutdownNotifier pShutdownNotifier, ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    pConfig.inject(this, PartitioningIOHelper.class);
    logger = pLogger;

    switch (new PartialCertificateTypeProvider(pConfig, false).getCertificateType()) {
    case MONOTONESTOPARG:
      partialConstructor = new MonotoneTransferFunctionARGBasedPartialReachedSetConstructionAlgorithm(true);
      break;
    default: // ARG
      ARGCPA cpa = CPAs.retrieveCPA(pCpa, ARGCPA.class);
      if (cpa == null) { throw new InvalidConfigurationException(
          "Require ARCPA"); }
      partialConstructor = new ARGBasedPartialReachedSetConstructionAlgorithm(cpa.getWrappedCPAs().get(0), true);
    }

    switch (partitioning) {
    case OPTIMAL:
      partitioner = new ExponentialOptimalBalancedGraphPartitioner(pShutdownNotifier);
      break;
    default: // RANDOM
      partitioner = new RandomBalancedGraphPartitioner();
    }
  }

  public int getSavedReachedSetSize() {
    return savedReachedSetSize;
  }

  public int getNumPartitions() {
    return numPartitions;
  }

  public @Nullable Pair<AbstractState[], AbstractState[]> getPartition(int pIndex) {
    if(0<=pIndex && pIndex<numPartitions){
      return partitions.get(pIndex);
    }
    return null;
  }

  public void constructInternalProofRepresentation(final UnmodifiableReachedSet pReached)
      throws InvalidConfigurationException {
    savedReachedSetSize = pReached.size();

    Pair<PartialReachedSetDirectedGraph, List<Set<Integer>>> partitionDescription =
        computePartialReachedSetAndPartition(pReached);

    numPartitions = partitionDescription.getSecond().size();
    partitions = new ArrayList<>(numPartitions);

    for (Set<Integer> partition : partitionDescription.getSecond()) {
      partitions.add(Pair.of(partitionDescription.getFirst().getSetNodes(partition, false), partitionDescription
          .getFirst()
          .getAdjacentNodesOutsideSet(partition, false)));
    }
  }

  public Pair<PartialReachedSetDirectedGraph, List<Set<Integer>>> computePartialReachedSetAndPartition(
      final UnmodifiableReachedSet pReached) throws InvalidConfigurationException {
    AbstractState[] partialCertificate = partialConstructor.computePartialReachedSet(pReached);
    ARGState[] argNodes = new ARGState[partialCertificate.length];
    for (int i = 0; i < partialCertificate.length; i++) {
      argNodes[i] = (ARGState) partialCertificate[i];
    }

    PartialReachedSetDirectedGraph graph = new PartialReachedSetDirectedGraph(argNodes);


    return Pair.of(graph,
        partitioner.computePartitioning(
            maxNumElemsPerPartition <= 0 ? 1 : (int) Math.ceil(pReached.size() / (double) maxNumElemsPerPartition),
            graph));
  }

  public void readPartition(final ObjectInputStream pIn)
      throws ClassNotFoundException, IOException {
    partitions.add(readPartitionContent(pIn));
  }

  private Pair<AbstractState[], AbstractState[]> readPartitionContent(final ObjectInputStream pIn)
      throws ClassNotFoundException, IOException {
    return Pair.of((AbstractState[]) pIn.readObject(), (AbstractState[]) pIn.readObject());
  }

  public void readPartition(final ObjectInputStream pIn, final Lock pLock) throws ClassNotFoundException, IOException {
    if (pLock == null) { throw new IllegalArgumentException("Cannot protect against parallel access"); }
    Pair<AbstractState[], AbstractState[]> result = readPartitionContent(pIn);
    pLock.lock();
    try {
      partitions.add(result);
    } finally {
      pLock.unlock();
    }
  }

  public void readMetadata(final ObjectInputStream pIn, final boolean pSave) throws IOException {
    if (pSave) {
      savedReachedSetSize = pIn.readInt();
      numPartitions = pIn.readInt();
      partitions = new ArrayList<>(numPartitions);
    } else {
      pIn.readInt();
      pIn.readInt();
    }
  }


  public void writeMetadata(final ObjectOutputStream pOut, final int pReachedSetSize, final int pNumPartitions)
      throws IOException {
    logger.log(Level.FINER,"Write metadata of partition");
    pOut.write(pReachedSetSize);
    pOut.write(pNumPartitions);
  }

  public void writePartition(final ObjectOutputStream pOut, final Set<Integer> pPartition,
      final PartialReachedSetDirectedGraph pPartialReachedSetDirectedGraph) throws IOException {
    logger.log(Level.FINER, "Write partition");
    writePartition(pOut, pPartialReachedSetDirectedGraph.getSetNodes(pPartition, false),
        pPartialReachedSetDirectedGraph.getAdjacentNodesOutsideSet(pPartition, false));
  }

  public void writePartition(ObjectOutputStream pOut, Pair<AbstractState[], AbstractState[]> pPartition)
      throws IOException {
    writePartition(pOut, pPartition.getFirst(), pPartition.getSecond());
  }

  private void writePartition(final ObjectOutputStream pOut, final AbstractState[] pPartitionNodes,
      AbstractState[] pAdjacentNodesOutside) throws IOException {
    pOut.writeObject(pPartitionNodes);
    pOut.writeObject(pAdjacentNodesOutside);

  }

  public void writeProof(final ObjectOutputStream pOut, final UnmodifiableReachedSet pReached)
      throws InvalidConfigurationException, IOException {
    Pair<PartialReachedSetDirectedGraph, List<Set<Integer>>> partitionDescription =
        computePartialReachedSetAndPartition(pReached);
    writeMetadata(pOut, pReached.size(), partitionDescription.getSecond().size());
    for (Set<Integer> partition : partitionDescription.getSecond()) {
      writePartition(pOut, partition, partitionDescription.getFirst());
    }
  }
}
