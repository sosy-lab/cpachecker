// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.parallel_suitcase;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.management.JMException;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
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
import org.sosy_lab.cpachecker.util.resources.ProcessCpuTimeLimit;
import org.sosy_lab.cpachecker.util.resources.ResourceLimit;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;


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
   * @param spec Specification for the analysis
   * @param shutdownNotifier Shutdown notifier for interruption handling
   */
  public ParallelTestSuiteGenerationAlgorithm(
      Configuration config,
      LogManager logger,
      ShutdownNotifier shutdownNotifier,
      CFA cfa,
      Specification spec)
      throws InvalidConfigurationException {
    config.inject(this);
    this.logger = logger;
    this.shutdownManager = ShutdownManager.createWithParent(shutdownNotifier);
    this.cfa = cfa;
    this.specification = spec;


    if (numberOfThreads <= 0) {
      throw new InvalidConfigurationException("Number of threads must be positive");
  }

    logger.log(Level.INFO, "=== ParallelTestSuiteGenerationAlgorithm CONSTRUCTOR CALLED ===");
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
      executorService.shutdown();
      executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
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

      ShutdownManager threadShutdownManager = null;
      LogManager threadLogger = null;

      logger.log(Level.INFO, "Thread " + threadId + " processing " + partition.size() + " targets");
      logger.log(Level.INFO, "THREAD_DEBUG " + threadId + " START");

      try {
        // 1. Thread-local config
        Configuration threadConfig = Configuration.builder().loadFromFile(configFile).build();
        logger.log(Level.INFO, "THREAD_DEBUG " + threadId + " created clean config");

        // 2. Thread-local shutdown + limits
        threadShutdownManager = ShutdownManager.createWithParent(shutdownManager.getNotifier());

        List<ResourceLimit> limits = new ArrayList<>();
        try {
          limits.add(ProcessCpuTimeLimit.create(TimeSpan.ofSeconds(120)));
        } catch (JMException e) {
          logger.log(Level.SEVERE, "CPU time measurement not supported", e);
        }

        ResourceLimitChecker limitChecker = new ResourceLimitChecker(threadShutdownManager, limits);
        limitChecker.start();

        threadLogger = logger.withComponentName("TestGen-Thread-" + threadId);

        // 3. Create CPA + Algorithm
        CoreComponentsFactory coreComponents =
            new CoreComponentsFactory(
                threadConfig,
                threadLogger,
                threadShutdownManager.getNotifier(),
                AggregatedReachedSets.empty(),
                cfa);

        ConfigurableProgramAnalysis cpa = coreComponents.createCPA(specification);

        Algorithm algorithm = coreComponents.createAlgorithm(cpa, specification);

        logger.log(
            Level.INFO,
            "THREAD_DEBUG "
                + threadId
                + " created algorithm = "
                + algorithm.getClass().getSimpleName());

        if (algorithm instanceof ParallelTestSuiteGenerationAlgorithm) {
          logger.log(Level.SEVERE, "THREAD_DEBUG " + threadId + " RECURSION DETECTED!");
          return;
        }

        // 4. Inject partition into TestTargetCPA
        TestTargetCPA testTargetCPA = CPAs.retrieveCPA(cpa, TestTargetCPA.class);
        if (testTargetCPA == null) {
          throw new IllegalStateException("TestTargetCPA not found");
        }

        TestTargetTransferRelation transferRel =
            (TestTargetTransferRelation) testTargetCPA.getTransferRelation();

        transferRel.setTestTargets(partition);
        threadLogger.log(
            Level.INFO, "Thread " + threadId + " set " + partition.size() + " test targets");

        // 5. Create + initialize reached set (ONLY ONCE)
        ReachedSet reachedSet = coreComponents.createReachedSet(cpa);

        coreComponents.initializeReachedSet(reachedSet, cfa.getMainFunction(), cpa);

        // 6. Run algorithm
        logger.log(Level.INFO, "THREAD_DEBUG " + threadId + " running algorithm...");
        AlgorithmStatus status = algorithm.run(reachedSet);

        logger.log(
            Level.INFO, "THREAD_DEBUG " + threadId + " algorithm finished, status = " + status);

      } catch (InvalidConfigurationException e) {
        logger.log(Level.SEVERE, "Thread " + threadId + " configuration error", e);

      } catch (CPAException e) {
        logger.log(Level.SEVERE, "Thread " + threadId + " CPA error", e);

      } catch (InterruptedException e) {
        logger.log(Level.INFO, "Thread " + threadId + " interrupted", e);
        Thread.currentThread().interrupt();

      } catch (Exception e) {
        logger.log(Level.SEVERE, "Thread " + threadId + " unexpected error", e);

      } finally {
        try {
          if (threadLogger != null) {
            threadLogger.flush();
          }
          logger.flush();
        } catch (Exception ignore) {
        }

        if (threadShutdownManager != null) {
          threadShutdownManager.requestShutdown("Thread finished");
        }

        logger.log(Level.INFO, "THREAD_DEBUG " + threadId + " EXITING");
      }
    }
  }
}
