// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy.partitioning;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.pcc.BalancedGraphPartitioner;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PartialReachedConstructionAlgorithm;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.pcc.strategy.AbstractStrategy.PCStrategyStatistics;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.PartialCertificateTypeProvider;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.PartialReachedSetDirectedGraph;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.GraphPartitionerFactory.PartitioningHeuristics;
import org.sosy_lab.cpachecker.pcc.util.ProofStatesInfoCollector;
import org.sosy_lab.cpachecker.util.Pair;

@Options(prefix = "pcc.partitioning")
public class PartitioningIOHelper {

  @Option(
      secure = true,
      description =
          "If enabled uses the number of nodes saved in certificate to compute partition number"
              + " otherwise the number of states explored during analysis")
  private boolean useGraphSizeToComputePartitionNumber = false;

  @Option(
      secure = true,
      description =
          "Specifies the maximum size of the partition. This size is used to compute the number of"
              + " partitions if a proof (reached set) should be written. Default value 0 means"
              + " always a single partition.")
  private int maxNumElemsPerPartition = 0;

  @Option(
      secure = true,
      description = "Heuristic for computing partitioning of proof (partial reached set).")
  private PartitioningHeuristics partitioningStrategy = PartitioningHeuristics.RANDOM;

  private final LogManager logger;
  private final PartialReachedConstructionAlgorithm partialConstructor;
  private final BalancedGraphPartitioner partitioner;
  private int savedReachedSetSize;
  private int numPartitions;
  private List<Pair<AbstractState[], AbstractState[]>> partitions;
  private Statistics currentGraphStatistics;
  private ProofStatesInfoCollector infoCollector;

  public PartitioningIOHelper(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    this(pConfig, pLogger, pShutdownNotifier, false);
  }

  protected PartitioningIOHelper(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final boolean withCMC)
      throws InvalidConfigurationException {
    pConfig.inject(this, PartitioningIOHelper.class);
    logger = pLogger;

    partialConstructor =
        new PartialCertificateTypeProvider(pConfig, false, withCMC).getCertificateConstructor();
    partitioner =
        GraphPartitionerFactory.createPartitioner(
            logger, partitioningStrategy, pShutdownNotifier, pConfig);
  }

  public int getSavedReachedSetSize() {
    return savedReachedSetSize;
  }

  public int getNumPartitions() {
    return numPartitions;
  }

  public @Nullable Pair<AbstractState[], AbstractState[]> getPartition(int pIndex) {
    if (0 <= pIndex && pIndex < numPartitions && pIndex < partitions.size()) {
      return partitions.get(pIndex);
    }
    return null;
  }

  public void constructInternalProofRepresentation(
      final UnmodifiableReachedSet pReached, final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException, InterruptedException {
    saveInternalProof(pReached.size(), computePartialReachedSetAndPartition(pReached, pCpa));
  }

  protected void saveInternalProof(
      final int size,
      final Pair<PartialReachedSetDirectedGraph, List<Set<Integer>>> pPartitionDescription) {
    savedReachedSetSize = size;

    numPartitions = pPartitionDescription.getSecond().size();
    partitions = new ArrayList<>(numPartitions);

    for (Set<Integer> partition : pPartitionDescription.getSecond()) {
      partitions.add(
          Pair.of(
              pPartitionDescription.getFirst().getSetNodes(partition, false),
              pPartitionDescription.getFirst().getSuccessorNodesOutsideSet(partition, false)));
    }
  }

  public Pair<PartialReachedSetDirectedGraph, List<Set<Integer>>>
      computePartialReachedSetAndPartition(
          final UnmodifiableReachedSet pReached, final ConfigurableProgramAnalysis pCpa)
          throws InvalidConfigurationException, InterruptedException {
    AbstractState[] partialCertificate =
        partialConstructor.computePartialReachedSet(pReached, pCpa);
    ARGState[] argNodes = new ARGState[partialCertificate.length];
    for (int i = 0; i < partialCertificate.length; i++) {
      argNodes[i] = (ARGState) partialCertificate[i];
    }

    PartialReachedSetDirectedGraph graph = new PartialReachedSetDirectedGraph(argNodes);
    currentGraphStatistics = graph;

    if (useGraphSizeToComputePartitionNumber) {
      return Pair.of(
          graph,
          partitioner.computePartitioning(
              maxNumElemsPerPartition <= 0
                  ? 1
                  : (int) Math.ceil(graph.getNumNodes() / (double) maxNumElemsPerPartition),
              graph));
    } else {
      return Pair.of(
          graph,
          partitioner.computePartitioning(
              maxNumElemsPerPartition <= 0
                  ? 1
                  : (int) Math.ceil(pReached.size() / (double) maxNumElemsPerPartition),
              graph));
    }
  }

  public void readPartition(final ObjectInputStream pIn, final PCStrategyStatistics pStats)
      throws ClassNotFoundException, IOException {
    Pair<AbstractState[], AbstractState[]> result = readPartitionContent(pIn);
    partitions.add(result);
    pStats.increaseProofSize(result.getFirst().length + result.getSecond().length);
  }

  private Pair<AbstractState[], AbstractState[]> readPartitionContent(final ObjectInputStream pIn)
      throws ClassNotFoundException, IOException {
    return Pair.of((AbstractState[]) pIn.readObject(), (AbstractState[]) pIn.readObject());
  }

  public void readPartition(
      final ObjectInputStream pIn, final PCStrategyStatistics pStats, final Lock pLock)
      throws ClassNotFoundException, IOException {
    checkArgument(pLock != null, "Cannot protect against parallel access");
    Pair<AbstractState[], AbstractState[]> result = readPartitionContent(pIn);
    int partialProofSize = result.getFirst().length + result.getSecond().length;
    pLock.lock();
    try {
      partitions.add(result);
      pStats.increaseProofSize(partialProofSize);
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

  public void readProof(final ObjectInputStream pIn, final PCStrategyStatistics pStats)
      throws IOException, ClassNotFoundException {
    readMetadata(pIn, true);
    for (int i = 0; i < numPartitions; i++) {
      readPartition(pIn, pStats);
    }
  }

  public void writeMetadata(
      final ObjectOutputStream pOut, final int pReachedSetSize, final int pNumPartitions)
      throws IOException {
    logger.log(Level.FINER, "Write metadata of partition");
    pOut.writeInt(pReachedSetSize);
    pOut.writeInt(pNumPartitions);
    pOut.reset();
  }

  public void writePartition(
      final ObjectOutputStream pOut,
      final Set<Integer> pPartition,
      final PartialReachedSetDirectedGraph pPartialReachedSetDirectedGraph)
      throws IOException {
    logger.log(Level.FINER, "Write partition");
    writePartition(
        pOut,
        pPartialReachedSetDirectedGraph.getSetNodes(pPartition, false),
        pPartialReachedSetDirectedGraph.getSuccessorNodesOutsideSet(pPartition, false));
  }

  public void writePartition(
      ObjectOutputStream pOut, Pair<AbstractState[], AbstractState[]> pPartition)
      throws IOException {
    writePartition(pOut, pPartition.getFirst(), pPartition.getSecond());
  }

  private void writePartition(
      final ObjectOutputStream pOut,
      final AbstractState[] pPartitionNodes,
      AbstractState[] pAdjacentNodesOutside)
      throws IOException {
    if (infoCollector != null) {
      infoCollector.addInfoForStates(pPartitionNodes);
    }
    pOut.writeObject(pPartitionNodes);
    pOut.writeObject(pAdjacentNodesOutside);
  }

  public void writeProof(
      final ObjectOutputStream pOut,
      final UnmodifiableReachedSet pReached,
      final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException, IOException, InterruptedException {
    Pair<PartialReachedSetDirectedGraph, List<Set<Integer>>> partitionDescription =
        computePartialReachedSetAndPartition(pReached, pCpa);

    writeMetadata(pOut, pReached.size(), partitionDescription.getSecond().size());
    for (Set<Integer> partition : partitionDescription.getSecond()) {
      writePartition(pOut, partition, partitionDescription.getFirst());
    }
  }

  public void setProofInfoCollector(final ProofStatesInfoCollector pInfoCollector) {
    infoCollector = pInfoCollector;
  }

  public Statistics getPartitioningStatistc() {
    return new PartitioningStatistics();
  }

  public Statistics getGraphStatistic() {
    if (currentGraphStatistics == null) {
      return new Statistics() {

        @Override
        public void printStatistics(
            PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {}

        @Override
        public @Nullable String getName() {
          return null;
        }
      };
    }
    return currentGraphStatistics;
  }

  private class PartitioningStatistics implements Statistics {

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      if (numPartitions > 0 && partitions != null) {
        pOut.printf("Number of partitions: %d%n", numPartitions);
        pOut.printf("The following numbers are given in number of states.%n");
        computeAndPrintDetailedPartitioningStats(pOut);
      }

      if (currentGraphStatistics != null) {
        pOut.println(
            "\nStatistics for partial reached set directed graph used in proof construction");
        currentGraphStatistics.printStatistics(pOut, pResult, pReached);
      }
    }

    @Override
    public void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {
      if (currentGraphStatistics != null) {
        currentGraphStatistics.writeOutputFiles(pResult, pReached);
      }
    }

    private void computeAndPrintDetailedPartitioningStats(PrintStream pOut) {
      int maxP = 0,
          maxO = 0,
          minP = Integer.MAX_VALUE,
          minO = Integer.MAX_VALUE,
          totalO = 0,
          totalS = 0,
          current;

      for (Pair<AbstractState[], AbstractState[]> partition : partitions) {
        current = partition.getSecond().length;
        maxO = Math.max(maxO, current);
        minO = Math.min(minO, current);
        totalO += current;

        current += partition.getFirst().length;
        maxP = Math.max(maxP, current);
        minP = Math.min(minP, current);
        totalS += current;
      }

      pOut.printf("Certificate size: %d %n", totalS);
      pOut.printf("Total overhead:  %d%n", totalO);
      pOut.printf("Avg. partition size:  %.2f%n", ((double) totalS) / numPartitions);
      pOut.printf("Avg. partition overhead: %.2f%n", ((double) totalO) / numPartitions);
      pOut.printf("Max partition size: %d%n", maxP);
      pOut.printf("Max partition overhead: %d%n", maxO);
      pOut.printf("Min partition size: %d%n", minP);
      pOut.printf("Min partition overhead: %d%n", minO);
    }

    @Override
    public String getName() {
      return "PCC Partitioning Statistic";
    }
  }
}
