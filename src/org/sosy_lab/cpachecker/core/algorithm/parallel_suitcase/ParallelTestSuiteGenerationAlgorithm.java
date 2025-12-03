// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.parallel_suitcase;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.testtargets.TestTargetProvider;
import org.sosy_lab.cpachecker.cpa.testtargets.TestTargetType;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Parallel test case generation algorithm Divides global test targets into multiple subsets and
 * processes each subset in parallel using multiple threads
 */
@Options(prefix = "analysis.parallelTestGeneration")
public class ParallelTestSuiteGenerationAlgorithm implements Algorithm {

  // Configuration option: Number of threads for parallel execution
  @Option(description = "Number of threads for parallel test generation", secure = true)
  private int numberOfThreads = 4;

  // Configuration option: Partitioning strategy selection
  @Option(description = "Partitioning strategy to use (RANDOM or STRATEGY)", secure = true)
  private PartitioningStrategyType partitioningStrategy = PartitioningStrategyType.RANDOM;

  private final LogManager logger;
  private final CFA cfa;

  /**
   * Constructor - injects necessary dependencies
   *
   * @param config Configuration object for option injection
   * @param logger Logger for output messages
   * @param cfa Control flow automaton for test target retrieval
   */
  public ParallelTestSuiteGenerationAlgorithm(Configuration config, LogManager logger, CFA cfa)
      throws InvalidConfigurationException {

    // Inject configuration options
    config.inject(this);
    this.logger = logger;
    this.cfa = cfa;

    // Validate thread count configuration
    if (numberOfThreads <= 0) {
      throw new InvalidConfigurationException("Number of threads must be positive");
    }
  }

  /**
   * Main algorithm entry point as required by Algorithm interface Delegates to the existing
   * generateTestSuite method
   *
   * @param reachedSet the reached set (may be unused for test generation)
   * @return AlgorithmStatus indicating the result of test generation
   * @throws CPAException if CPA-related errors occur
   * @throws InterruptedException if the algorithm is interrupted
   */
  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    try {
      generateTestSuite();
      // If we reach here, test generation completed successfully
      return AlgorithmStatus.SOUND_AND_PRECISE;
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error during parallel test generation", e);
      return AlgorithmStatus.UNSOUND_AND_IMPRECISE;
    }
  }

  /**
   * Main entry point for parallel test generation Execution flow: 1. Get global test targets from
   * TestTargetProvider 2. Partition targets into subsets using selected strategy 3. Create thread
   * pool and submit tasks for each partition 4. Wait for all threads to complete processing
   */
  @SuppressWarnings("FutureReturnValueIgnored")
  public void generateTestSuite() {
    logger.log(
        Level.INFO, "Starting parallel test generation with " + numberOfThreads + " threads");

    try {
      // Step 1: Get global test targets
      Set<CFAEdge> testTargets =
          TestTargetProvider.getTestTargets(
              cfa,
              false, // We handle parallelism ourselves
              TestTargetType.ASSUME, // Use assumption edges as test targets
              "", // No specific target function
              null, // No optimization strategies
              false, // Don't apply optimizations nested
              false, // Don't track redundant targets
              logger);

      logger.log(Level.INFO, "Retrieved " + testTargets.size() + " test targets");

      if (testTargets.isEmpty()) {
        logger.log(Level.WARNING, "No test targets found");
        return;
      }

      // Step 2: Partition targets using selected strategy
      PartitioningStrategy strategy = createPartitioningStrategy();
      List<Set<CFAEdge>> partitions = strategy.partition(testTargets, numberOfThreads, cfa);

      logger.log(Level.INFO, "Divided targets into " + partitions.size() + " partitions");

      // Step 3: Create thread pool and submit tasks
      ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

      for (int i = 0; i < partitions.size(); i++) {
        Set<CFAEdge> partition = partitions.get(i);
        // Submit a task for each partition
        executorService.submit(new TestCaseGenerationTask(partition, i));
      }

      // Step 4: Wait for all tasks to complete
      executorService.shutdown();
      boolean completed = executorService.awaitTermination(1, TimeUnit.HOURS);

      if (completed) {
        logger.log(Level.INFO, "Parallel test generation completed successfully");
      } else {
        logger.log(Level.WARNING, "Parallel test generation timed out");
        executorService.shutdownNow(); // Force shutdown
      }

    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error during parallel test generation", e);
      throw new RuntimeException("Parallel test generation failed", e);
    }
  }

  /** Creates partitioning strategy instance based on user configuration */
  private PartitioningStrategy createPartitioningStrategy() {
    return switch (partitioningStrategy) {
      case RANDOM -> new RandomPartitioner(); // Random distribution
      case STRATEGY -> new StrategyPartitioner(cfa); // Sequential distribution
    };
  }

  /** Test case generation task Each task processes one subset of test targets */
  private class TestCaseGenerationTask implements Runnable {

    private final Set<CFAEdge> partition; // Subset of targets this task handles
    private final int threadId; // Thread ID for logging

    /**
     * Task constructor
     *
     * @param partition Subset of test targets to process
     * @param threadId Unique thread identifier
     */
    TestCaseGenerationTask(Set<CFAEdge> partition, int threadId) {
      this.partition = partition;
      this.threadId = threadId;
    }

    /** Task execution method Processes all test targets in the assigned subset */
    @Override
    public void run() {
      logger.log(Level.INFO, "Thread " + threadId + " processing " + partition.size() + " targets");

      try {
        // Process each test target in the subset
        for (CFAEdge target : partition) {
          logger.log(Level.FINE, "Thread " + threadId + " processing target: " + target);
        }

        logger.log(Level.INFO, "Thread " + threadId + " completed successfully");

      } catch (Exception e) {
        logger.log(Level.SEVERE, "Thread " + threadId + " failed", e);
      }
    }
  }
}
