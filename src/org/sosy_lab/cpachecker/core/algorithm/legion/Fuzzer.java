// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.legion;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.value.NondeterministicValueProvider;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;

@Options(prefix = "legion")
public class Fuzzer {

  @Option(
      secure = true,
      description = "How many passes to fuzz before asking the solver for the first time.")
  private int initialPasses = 3;

  @Option(secure = true, description = "fuzzingPasses = ⌈ fuzzingMultiplier * fuzzingSolutions ⌉")
  private double fuzzingMultiplier = 1;

  @Option(secure = true, description = "If 0 fuzzing would run, instead run this amount of passes.")
  private int emergencyFuzzingPasses = 1;

  private final LogManager logger;
  private final ValueAnalysisCPA valueCpa;
  private final OutputWriter outputWriter;
  private final ShutdownNotifier shutdownNotifier;
  private final LegionComponentStatistics stats;
  private final NondeterministicValueProvider nonDetValueProvider;
  private int passes;

  public Fuzzer(
      String pName,
      final LogManager pLogger,
      ValueAnalysisCPA pValueCPA,
      OutputWriter pOutputWriter,
      ShutdownNotifier pShutdownNotifier,
      Configuration pConfig
      )
      throws InvalidConfigurationException {

    pConfig.inject(this, Fuzzer.class);
    this.logger = pLogger;
    this.shutdownNotifier = pShutdownNotifier;

    this.valueCpa = pValueCPA;
    this.nonDetValueProvider = pValueCPA.getTransferRelation().getNonDetValueProvider();

    this.outputWriter = pOutputWriter;
    this.stats = new LegionComponentStatistics(pName);

    this.passes = initialPasses;
  }

  /**
   * Run the fuzzing phase using pAlgorithm pPasses times on the states in pReachedSet.
   *
   * <p>Runs the Algorithm on the pReachedSet as a fuzzer. pPreloadedValues are used where
   * applicable.
   */
  public ReachedSet fuzz(
      ReachedSet pReachedSet, Algorithm pAlgorithm, List<List<ValueAssignment>> pPreLoadedValues)
      throws CPAEnabledAnalysisPropertyViolationException, CPAException, InterruptedException {

    for (int i = 0; i < this.passes; i++) {
      this.stats.start();
      logger.log(Level.FINE, "Fuzzing pass", i + 1);

      // Preload values if they exist
      int size = pPreLoadedValues.size();
      if (size > 0) {
        int j = i % size;
        logger.log(Level.FINER, "pPreLoadedValues at", j, "/", size);
        preloadValues(pPreLoadedValues.get(j));
      }
      try {
        // Run algorithm and collect result
        pAlgorithm.run(pReachedSet);
      } finally {
        this.stats.finish();
        this.outputWriter.writeTestCases(pReachedSet);
      }

      // Check whether to shut down
      try {
        shutdownNotifier.shutdownIfNecessary();
      } finally {
        this.stats.finish();
      }

      // Otherwise, start from the beginning again
      pReachedSet.reAddToWaitlist(pReachedSet.getFirstState());
      this.stats.finish();
    }
    return pReachedSet;
  }

  public void setPasses(int pPasses) {
    this.passes = pPasses;
  }

  public void computePasses(int pPreloadedValuesSize) {
    int fuzzingPasses = (int) Math.ceil(fuzzingMultiplier * pPreloadedValuesSize);
    if (fuzzingPasses == 0) {
      fuzzingPasses = this.emergencyFuzzingPasses;
    }
    this.passes = fuzzingPasses;
  }

  /** Use assignments to preload the ValueCPA in order to use them as applicable. */
  private List<Value> preloadValues(List<ValueAssignment> assignments) {
    List<Value> values = new ArrayList<>();
    for (ValueAssignment a : assignments) {
      values.add(ValueConverter.toValue(a.getValue()));
    }

    this.nonDetValueProvider.setKnownValues(values);

    return values;
  }

  public LegionComponentStatistics getStats() {
    return this.stats;
  }
}
