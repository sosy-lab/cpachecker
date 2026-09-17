// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.concurrent.atomic.LongAdder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsStatistics;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@Options(prefix = "cpa.smg2")
public class SMGCPAStatistics extends ConstraintsStatistics implements Statistics {

  @Option(secure = true, description = "target file to hold the exported precision")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path precisionFile = null;

  private final LongAdder iterations = new LongAdder();

  private final StatCounter listMaterializations =
      new StatCounter("Number of list materialization's");
  private final StatTimer totalMaterializationTime =
      new StatTimer("Time spend on list materialization");

  private final StatCounter zeroPlusMaterializations =
      new StatCounter("Number of 0+ list materialization's");
  private final StatTimer totalZeroPlusMaterializationTime =
      new StatTimer("Time spend on 0+ list materialization");

  private final StatCounter listAbstractions = new StatCounter("Number of list abstractions");
  private final StatTimer totalAbstractionTime = new StatTimer("Time spend on list abstraction");
  private final StatCounter exceptionsIgnoredDuringListAbstractions =
      new StatCounter("Number of exceptions during list abstractions that have been ignored");

  private final StatTimer totalListSearchTime =
      new StatTimer("Time spend on searching for lists to abstract");

  private final StatCounter errorPathAllocations =
      new StatCounter("Number of concrete error paths allocated");
  private final StatTimer totalErrorPathsAllocationTime =
      new StatTimer("Time spend on allocating concrete error paths");

  private final StatTimer totalMergeOpTime = new StatTimer("Total time spend on merging operator");
  private final StatTimer totalActualMergeTimeWithoutChecks =
      new StatTimer(
          "Total time spend on merging states (i.e. SMGs and SPCs) without precondition checks");
  private StatCounter failedMergesDueToPrecicionLoss =
      new StatCounter("Number of merges failed due to precision loss (partially set by options)");
  private StatCounter successfulMerges = new StatCounter("Number of successful merges");
  private StatCounter mergeOperatorCalls = new StatCounter("Number of merge operator applications");
  private StatCounter mergeAttemptsAfterChecks =
      new StatCounter("Number of actual merge attempts after preconditions have been met");

  private StatCounter assumptions = new StatCounter("Number of assumptions");
  private StatCounter deterministicAssumptions =
      new StatCounter("Number of deterministic assumptions");

  public SMGCPAStatistics() {
    super("SMGCPA");
  }

  @Override
  public String getName() {
    return "SMGCPA";
  }

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    StatInt numberOfVariables = new StatInt(StatKind.AVG, "Number of variables per state");
    StatInt numberOfGlobalVariables =
        new StatInt(StatKind.AVG, "Number of global variables per state");

    for (AbstractState currentAbstractState : reached) {
      SMGState currentState =
          AbstractStates.extractStateByType(currentAbstractState, SMGState.class);

      numberOfVariables.setNextValue(currentState.getSize());
      numberOfGlobalVariables.setNextValue(currentState.getNumberOfGlobalVariables());
    }

    StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(out);
    writer.put(numberOfVariables);
    writer.put(numberOfGlobalVariables);

    if (precisionFile != null) {
      exportPrecision(reached);
    }

    super.printStatistics(out, result, reached);
    writer.put(assumptions);
    writer.put(deterministicAssumptions);
    writer.put("Level of Determinism", getCurrentLevelOfDeterminism() + "%");
    writer.put("Number of list materializations: ", listMaterializations.getValue());
    writer.put("Total time spent on materialization: ", totalMaterializationTime.getConsumedTime());
    writer.put("Max time spent on materialization: ", totalMaterializationTime.getMaxTime());
    writer.put("Number of 0+ materializations", zeroPlusMaterializations);
    writer.put(
        "Total time spent on 0+ materialization: ",
        totalZeroPlusMaterializationTime.getConsumedTime());
    writer.put(
        "Max time spent on 0+ materialization: ", totalZeroPlusMaterializationTime.getMaxTime());
    writer.put(
        "Number of lists abstracted in precision adjustment in total: ",
        listAbstractions.getValue());
    writer.put(
        "Number of exceptions ignored during list abstraction in precision adjustment in total"
            + " (debug only): ",
        exceptionsIgnoredDuringListAbstractions.getValue());
    writer.put(
        "Total time spent on list abstraction in precision adjustment: ",
        totalAbstractionTime.getConsumedTime());
    writer.put(
        "Max time spent on list abstraction in precision adjustment: ",
        totalAbstractionTime.getMaxTime());
    writer.put(
        "Total time spent on searching for list abstractions in precision adjustment: ",
        totalListSearchTime.getConsumedTime());
    writer.put(
        "Max time spent on searching a list abstraction in precision adjustment: ",
        totalListSearchTime.getMaxTime());

    writer.put("Total time spent in merge operator: ", totalMergeOpTime.getConsumedTime());
    writer.put("Max time spent in merge operator: ", totalMergeOpTime.getMaxTime());
    writer.put(
        "Total time spent on merging states, excluding precondition checks: ",
        totalActualMergeTimeWithoutChecks.getConsumedTime());
    writer.put(
        "Max time spent on merging two states, excluding precondition checks: ",
        totalActualMergeTimeWithoutChecks.getMaxTime());

    writer.put("Number of merge operator applications: ", mergeOperatorCalls);
    writer.put(
        "Number of actual merge attempts after preconditions have been met: ",
        mergeAttemptsAfterChecks);
    writer.put(
        "Number of failed merges due to precision loss (partially set by options): ",
        failedMergesDueToPrecicionLoss);
    writer.put("Number of successful merges: ", successfulMerges);

    writer.put(
        "Total time spent on allocating values in a concrete error-path: ",
        totalErrorPathsAllocationTime.getConsumedTime());
    writer.put(
        "Max time spent on allocating values in a concrete error-path: ",
        totalErrorPathsAllocationTime.getMaxTime());
    writer.put(
        "Number of concrete error-paths allocated with concrete values: ",
        errorPathAllocations.getValue());
  }

  /**
   * This method exports the precision to file.
   *
   * @param reached the set of reached states.
   */
  private void exportPrecision(UnmodifiableReachedSet reached) {
    VariableTrackingPrecision consolidatedPrecision =
        VariableTrackingPrecision.joinVariableTrackingPrecisionsInReachedSet(reached);
    try (Writer writer = IO.openOutputFile(precisionFile, Charset.defaultCharset())) {
      consolidatedPrecision.serialize(writer);
    } catch (IOException e) {
      // cpa.getLogger().logUserException(Level.WARNING, e, "Could not write SMG precision to
      // file");
    }
  }

  public void incrementListMaterializations() {
    listMaterializations.inc();
  }

  public void startTotalMaterializationTime() {
    totalMaterializationTime.start();
  }

  public void stopTotalMaterializationTime() {
    totalMaterializationTime.stop();
  }

  public void incrementZeroPlusMaterializations() {
    zeroPlusMaterializations.inc();
  }

  public void startTotalZeroPlusMaterializationTime() {
    totalZeroPlusMaterializationTime.start();
  }

  public void stopTotalZeroPlusMaterializationTime() {
    totalZeroPlusMaterializationTime.stop();
  }

  public void incrementListAbstractions() {
    listAbstractions.inc();
  }

  public void incrementExceptionsIgnoredDuringListAbstractions() {
    exceptionsIgnoredDuringListAbstractions.inc();
  }

  public void incrementConcreteErrorPathsAllocated() {
    errorPathAllocations.inc();
  }

  public void startTotalConcreteErrorPathsAllocationTime() {
    totalErrorPathsAllocationTime.start();
  }

  public void stopTotalConcreteErrorPathsAllocationTime() {
    totalErrorPathsAllocationTime.stop();
  }

  public void startTotalAbstractionTime() {
    totalAbstractionTime.start();
  }

  public void stopTotalAbstractionTime() {
    totalAbstractionTime.stop();
  }

  public void startTotalListSearchTime() {
    totalListSearchTime.start();
  }

  public void stopTotalListSearchTime() {
    totalListSearchTime.stop();
  }

  void incrementIterations() {
    iterations.increment();
  }

  void incrementAssumptions() {
    assumptions.inc();
  }

  void incrementDeterministicAssumptions() {
    assumptions.inc();
  }

  int getCurrentNumberOfIterations() {
    return iterations.intValue();
  }

  StatTimer getTotalMergeOperatorTime() {
    return totalMergeOpTime;
  }

  StatTimer getTimeForMergeWithoutPreprocessingAndChecks() {
    return totalActualMergeTimeWithoutChecks;
  }

  void incrementNumberOfSuccessfulMerges() {
    successfulMerges.inc();
  }

  void incrementMergeOperatorCalls() {
    mergeOperatorCalls.inc();
  }

  void incrementMergeAttemptsAfterPreconditions() {
    mergeAttemptsAfterChecks.inc();
  }

  void incrementFailedMergesDueToPrecisionLoss() {
    failedMergesDueToPrecicionLoss.inc();
  }

  int getCurrentLevelOfDeterminism() {
    if (assumptions.getValue() == 0) {
      return 100;
    } else {
      return (int)
          Math.round((deterministicAssumptions.getValue() * 100) / (double) assumptions.getValue());
    }
  }
}
