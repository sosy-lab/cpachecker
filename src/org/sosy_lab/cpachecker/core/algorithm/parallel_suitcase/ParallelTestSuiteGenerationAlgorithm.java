// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.parallel_suitcase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.TestCaseGeneratorAlgorithm;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.testcase.TestVector;

/**
 * ParallelTestSuiteGenerationAlgorithm executes multiple TestCaseGeneratorAlgorithm instances in
 * parallel on partitions of a full ReachedSet, collects all generated test vectors, deduplicates
 * them, and produces the same output as the sequential generator.
 */
public class ParallelTestSuiteGenerationAlgorithm {

  private final PartitioningStrategy partitioner;
  private final int threadCount;
  private final LogManager logger;

  /**
   * Constructor.
   *
   * @param partitioner The strategy to split ReachedSet into partitions.
   * @param threadCount Number of threads for parallel execution.
   * @param logger Logger for messages.
   */
  public ParallelTestSuiteGenerationAlgorithm(
      PartitioningStrategy partitioner, int threadCount, LogManager logger) {
    this.partitioner = partitioner;
    this.threadCount = threadCount;
    this.logger = logger;
  }

  /**
   * Run parallel test generation.
   *
   * @param fullReachedSet Full ReachedSet containing all states to generate tests from.
   * @param generatorSupplier Supplies a fresh TestCaseGeneratorAlgorithm per thread.
   * @return Deduplicated set of TestVector covering all partitions.
   * @throws InterruptedException if thread execution is interrupted.
   * @throws ExecutionException if a thread throws an exception.
   */
  public Set<TestVector> runParallel(
      ReachedSet fullReachedSet,
      java.util.function.Supplier<TestCaseGeneratorAlgorithm> generatorSupplier)
      throws InterruptedException, ExecutionException {

    logger.log(Level.INFO, "Starting parallel test generation with " + threadCount + " threads.");

    // 1. Partition the ReachedSet
    List<ReachedSet> partitions = partitioner.partition(fullReachedSet, threadCount);

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    List<Future<List<TestVector>>> futures = new ArrayList<>();

    // 2. Submit tasks: each thread runs the sequential generator on its partition
    for (ReachedSet subSet : partitions) {
      futures.add(
          executor.submit(
              () -> {
                List<TestVector> localResults = new ArrayList<>();
                try {
                  TestCaseGeneratorAlgorithm generator = generatorSupplier.get();
                  generator.run(subSet); // run sequential algorithm on partition
                  // Collect generated test vectors
                  localResults.addAll(
                      generator
                          .getGeneratedTestVectors()); // i do not know should i write this method

                } catch (Exception e) {
                  logger.log(Level.SEVERE, "Error in parallel test generation thread.", e);
                }
                return localResults;
              }));
    }

    // 3. Merge results and deduplicate
    Set<TestVector> allTests = new HashSet<>();
    for (Future<List<TestVector>> f : futures) {
      List<TestVector> threadResults = f.get();
      if (threadResults != null) {
        allTests.addAll(threadResults);
      }
    }

    executor.shutdown();
    logger.log(
        Level.INFO,
        "Parallel test generation completed. Total unique test vectors: " + allTests.size());

    return allTests;
  }
}
