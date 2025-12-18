// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.parallel_suitcase;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.testtargets.TestTargetCPA;
import org.sosy_lab.cpachecker.cpa.testtargets.TestTargetProvider;
import org.sosy_lab.cpachecker.cpa.testtargets.TestTargetTransferRelation;
import org.sosy_lab.cpachecker.cpa.testtargets.TestTargetType;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;

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

  private final Specification specification;
  private final ShutdownManager shutdownManager;

  // private final Configuration globalConfig;

  /**
   * Constructor - injects necessary dependencies
   *
   * @param config Configuration object for option injection
   * @param logger Logger for output messages
   * @param cfa Control flow automaton for test target retrieval
   */
  public ParallelTestSuiteGenerationAlgorithm(Configuration config, LogManager logger, CFA cfa)
      throws InvalidConfigurationException {

    // Call the private 5-parameter constructor with internally created dependencies
    this(config, logger, cfa, createDefaultSpecification(), createDefaultShutdownNotifier());

    logger.log(Level.INFO, "=== ParallelTestSuiteGenerationAlgorithm CONSTRUCTOR CALLED ===");
  }

  private ParallelTestSuiteGenerationAlgorithm(
      Configuration config,
      LogManager logger,
      CFA cfa,
      Specification spec,
      ShutdownNotifier shutdownNotifier)
      throws InvalidConfigurationException {

    // Inject configuration options
    config.inject(this);
    this.logger = logger;
    this.cfa = cfa;
    this.specification = spec;
    this.shutdownManager = ShutdownManager.createWithParent(shutdownNotifier);
    // this.globalConfig = config;

    // Validate thread count configuration
    if (numberOfThreads <= 0) {
      throw new InvalidConfigurationException("Number of threads must be positive");
    }
  }

  /**
   * Creates a default specification for test generation Uses Specification.alwaysSatisfied() which
   * returns an empty specification
   */
  private static Specification createDefaultSpecification() {
    // This method exists and returns an empty specification
    return Specification.alwaysSatisfied();
  }

  /** Creates a default shutdown notifier */
  private static ShutdownNotifier createDefaultShutdownNotifier() {
    ShutdownManager manager = ShutdownManager.create();
    return manager.getNotifier();
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
    logger.log(Level.INFO, "=== PARALLEL TEST SUITE GENERATION STARTING ===");
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
              Collections.emptyList(), // No optimization strategies
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
        Path configFile =
            Paths.get("config/includes/testing/valueAnalysis-PrallelTestcaseGen.properties");
        executorService.submit(new TestCaseGenerationTask(partition, i, configFile));
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

  /** Test case generation task - each task processes one subset of test targets */
  private class TestCaseGenerationTask implements Runnable {

    private final Set<CFAEdge> partition; // Subset of targets this task handles
    private final int threadId; // Thread ID for logging
    private final Path configFile; // Configuration file for test generation

    /**
     * Task constructor
     *
     * @param partition Subset of test targets to process
     * @param threadId Unique thread identifier
     * @param configFile Configuration file path for test generation
     */
    TestCaseGenerationTask(Set<CFAEdge> partition, int threadId, Path configFile) {
      this.partition = partition;
      this.threadId = threadId;
      this.configFile = configFile;
    }

    /**
     * Task execution method Processes all test targets in the assigned subset by creating and
     * running an independent test generation algorithm instance
     */
    @Override
    public void run() {
      logger.log(Level.INFO, "Thread " + threadId + " processing " + partition.size() + " targets");
      logger.log(Level.INFO, "THREAD_DEBUG " + threadId + " START"); // debug

      try {

        // Create a completely new configuration, DO NOT copy from globalConfig
        Configuration threadConfig =
            Configuration.builder()
                .loadFromFile(configFile) // Only load the worker thread's specific configuration
                .build();

        logger.log(Level.INFO, "THREAD_DEBUG " + threadId + " created clean config");

        // Step 2: Create independent components for this thread
        // Following the pattern from ParallelAlgorithm.createParallelAnalysis
        ShutdownManager threadShutdownManager =
            ShutdownManager.createWithParent(shutdownManager.getNotifier());

        LogManager threadLogger = logger.withComponentName("TestGen-Thread-" + threadId);

        // Create core components factory
        CoreComponentsFactory coreComponents =
            new CoreComponentsFactory(
                threadConfig,
                threadLogger,
                threadShutdownManager.getNotifier(),
                AggregatedReachedSets.empty(),
                cfa);

        // Step 3: Create CPA, Algorithm and ReachedSet instances
        ConfigurableProgramAnalysis cpa = coreComponents.createCPA(specification);
        Algorithm algorithm = coreComponents.createAlgorithm(cpa, specification);
        String algorithmName = algorithm.getClass().getSimpleName(); // debug
        logger.log(
            Level.INFO,
            "THREAD_DEBUG " + threadId + " created algorithm = " + algorithmName); // debug

        if (algorithm instanceof ParallelTestSuiteGenerationAlgorithm) {
          logger.log(Level.SEVERE, "THREAD_DEBUG " + threadId + " RECURSION DETECTED!");
          return;
        }

        ReachedSet reachedSet = coreComponents.createReachedSet(cpa);

        // Step 4: Extract and update TestTargetCPA with partition targets
        // As advisor suggested: retrieve TestTargetCPA and update its transfer relation
        TestTargetCPA testTargetCPA = CPAs.retrieveCPA(cpa, TestTargetCPA.class);
        if (testTargetCPA == null) {
          throw new IllegalStateException("TestTargetCPA not found in CPA structure");
        }

        // Get transfer relation and update test targets
        // This requires adding setTestTargets() method to TestTargetTransferRelation
        TestTargetTransferRelation transferRel =
            (TestTargetTransferRelation) testTargetCPA.getTransferRelation();
        transferRel.setTestTargets(partition); // New method to be implemented

        // Step 5: Initialize reached set and run algorithm
        // Following ParallelAlgorithm.runParallelAnalysis pattern
        coreComponents.initializeReachedSet(reachedSet, cfa.getMainFunction(), cpa);

        // Run the test generation algorithm
        logger.log(Level.INFO, "THREAD_DEBUG " + threadId + " running algorithm...");
        AlgorithmStatus status = algorithm.run(reachedSet);
        logger.log(
            Level.INFO, "THREAD_DEBUG " + threadId + " algorithm finished, status = " + status);

        // Step 6: Log results
        if (status.isSound() && status.isPrecise()) {
          logger.log(Level.INFO, "Thread " + threadId + " completed successfully");
        } else {
          logger.log(Level.WARNING, "Thread " + threadId + " finished with status: " + status);
        }

      } catch (InvalidConfigurationException e) {
        logger.log(Level.SEVERE, "Thread " + threadId + " configuration error", e);
      } catch (CPAException e) {
        logger.log(Level.SEVERE, "Thread " + threadId + " CPA error during test generation", e);
      } catch (InterruptedException e) {
        logger.log(Level.INFO, "Thread " + threadId + " was interrupted", e);
        Thread.currentThread().interrupt(); // Restore interrupt status
      } catch (UnsupportedOperationException e) {
        // Add complete stack information
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String stackTrace = sw.toString();

        logger.log(Level.SEVERE, "Thread " + threadId + " UnsupportedOperationException DETAILS:");
        logger.log(Level.SEVERE, "Message: " + e.getMessage());
        logger.log(Level.SEVERE, "Full stack trace:");
        // Record the stack trace line by line to ensure nothing is lost.
        for (String line : stackTrace.split("\n")) {
          logger.log(Level.SEVERE, line);
        }
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Thread " + threadId + " unexpected error", e);
        // Same
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        logger.log(Level.SEVERE, "Full stack trace:\n" + sw.toString());
      }
      }
  }
}
