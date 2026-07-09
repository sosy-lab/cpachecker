// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.witness;

import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.cpa.path.ViolationWitness;

public class ResultWithWitnessInformation {

  private final Result result;

  final ViolationWitness violationPath;
  final DssArgStateCollector correctnessPreConditionCollector;

  private ResultWithWitnessInformation(
      Result pResult,
      ViolationWitness pViolationPath,
      DssArgStateCollector pCorrectnessPreConditionCollector) {
    result = pResult;
    violationPath = pViolationPath;
    correctnessPreConditionCollector = pCorrectnessPreConditionCollector;
  }

  public Result getResult() {
    return result;
  }

  public static ResultWithWitnessInformation ofViolationPath(ViolationWitness violationPath) {
    return new ResultWithWitnessInformation(Result.FALSE, violationPath, null);
  }

  public static ResultWithWitnessInformation ofCorrectnessPreConditionCollector(
      DssArgStateCollector correctnessPreConditions) {
    return new ResultWithWitnessInformation(Result.TRUE, null, correctnessPreConditions);
  }

  public static ResultWithWitnessInformation ofResultWithoutInformation(Result result) {
    return new ResultWithWitnessInformation(result, null, null);
  }
}
