// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import com.google.common.base.Preconditions;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.PrimitiveIterator.OfInt;
import java.util.Random;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Property;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.cpachecker.util.testcase.TestCaseExporter;

@Options(prefix = "testcase")
public class RandomTestGeneratorAlgorithm implements Algorithm, StatisticsProvider, Statistics {

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Configuration config;
  private final CFA cfa;
  private final Property specProp;

  private int numExportedTestCases = 0;
  private StatInt testCaseLengths = new StatInt(StatKind.AVG, "Average test case length");

  @Option(secure = true, description = "Random seed for random test-case generation")
  private long randomInputSeed = 0;

  @Option(secure = true, description = "Number of random test cases that should be generated")
  private int numRandomTests = 1;

  @Option(
      secure = true,
      name = "random.maxLength",
      description = "Number of random test cases that should be generated")
  private int maxLength = 20;

  @Option(secure = true, name = "random.min", description = "Minimum value randomly generated")
  private int minVal = 0;

  @Option(secure = true, name = "random.max", description = "Maximum value randomly generated")
  private int maxVal = 20;

  public RandomTestGeneratorAlgorithm(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa,
      final Specification pSpec)
      throws InvalidConfigurationException {
    config = pConfig;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    cfa = pCfa;

    config.inject(this);

    numRandomTests = Math.max(numRandomTests, 0);
    maxVal = Math.max(minVal + 1, maxVal);

    if (pSpec.getProperties().size() == 1) {
      specProp = pSpec.getProperties().iterator().next();
      Preconditions.checkArgument(
          specProp.isCoverage(), "Property %s not supported for test generation", specProp);
    } else {
      specProp = null;
    }
  }

  @Override
  public AlgorithmStatus run(final ReachedSet pReachedSet)
      throws CPAException, InterruptedException {
    logger.log(Level.INFO, "Start generating", numRandomTests, "random tests");

    TestCaseExporter exporter;
    try {
      exporter = new TestCaseExporter(cfa, logger, config);

      Random randomGenerator = new Random(randomInputSeed);
      OfInt randomInt = randomGenerator.ints(minVal, maxVal).iterator();

      int testLength;
      List<String> inputs = new ArrayList<>();
      genTests:
      for (int i = 0; i < numRandomTests; i++) {
        shutdownNotifier.shutdownIfNecessary();
        testLength = randomGenerator.nextInt(maxLength + 1);
        testCaseLengths.setNextValue(testLength);
        inputs.clear();
        for (int j = 0; j < testLength; j++) {
          shutdownNotifier.shutdownIfNecessary();
          if (randomInt.hasNext()) {
            inputs.add(String.valueOf(randomInt.nextInt()));
          } else {
            break genTests;
          }
        }
        shutdownNotifier.shutdownIfNecessary();
        logger.log(Level.FINE, "Export test case of length ", testLength);
        exporter.writeTestCaseFiles(inputs, Optional.ofNullable(specProp));
        numExportedTestCases++;
      }

      logger.log(Level.INFO, "Finished random test generation");
    } catch (InvalidConfigurationException e) {
      logger.logException(Level.INFO, e, "Abort test generation due to wrong configuration");
    }
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(this);
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    StatisticsWriter statWriter = StatisticsWriter.writingStatisticsTo(pOut);
    statWriter.put(testCaseLengths).put("Generated random tests", numExportedTestCases);
  }

  @Override
  public @Nullable String getName() {
    return "RandomTestGeneratorAlgorithm";
  }
}
