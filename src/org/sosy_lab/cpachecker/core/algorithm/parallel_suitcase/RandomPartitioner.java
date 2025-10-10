// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.parallel_suitcase;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;

/**
 * RandomPartitioner randomly distributes the states in a ReachedSet into multiple partitions for
 * parallel test generation.
 */
public class RandomPartitioner implements PartitioningStrategy {

  private final ReachedSetFactory reachedSetFactory;
  private final ConfigurableProgramAnalysis cpa;
  private final Random random;
  private final LogManager logger;

  /**
   * Constructor.
   *
   * @param pFactory The ReachedSetFactory used to create sub-ReachedSets.
   * @param pCpa The CPA instance needed to create ReachedSets.
   * @param seed Random seed for reproducibility.
   * @param pLogger Logger for status messages.
   */
  public RandomPartitioner(
      ReachedSetFactory pFactory, ConfigurableProgramAnalysis pCpa, long seed, LogManager pLogger) {
    this.reachedSetFactory = pFactory;
    this.cpa = pCpa;
    this.random = new Random(seed);
    this.logger = pLogger;
  }

  @Override
  public List<ReachedSet> partition(ReachedSet reachedSet, int numPartitions) {
    List<ReachedSet> partitions = new ArrayList<>();

    // Create empty sub-ReachedSets
    for (int i = 0; i < numPartitions; i++) {
      partitions.add(reachedSetFactory.create(cpa));
    }

    // Randomly assign each AbstractState to a partition
    for (AbstractState state : reachedSet) {
      int index = random.nextInt(numPartitions);
      partitions.get(index).add(state, reachedSet.getPrecision(state));
    }

    logger.log(Level.INFO, "Random partitioning finished: created " + numPartitions + " partitions.");
    return partitions;
  }
}
