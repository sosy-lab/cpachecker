// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.util;

import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;

public class SubtaskResult {
  public final Result result;
  
  public final AlgorithmStatus status;
  
  private SubtaskResult(final Result pResult, final AlgorithmStatus pStatus) {
    result = pResult;
    status = pStatus;
  }
  
  public SubtaskResult withResult(final Result pResult) {
    if(result == Result.FALSE) {
      return this; 
    } 
    else {
      return new SubtaskResult(pResult, status);
    }
  }

  public SubtaskResult withStatus(final AlgorithmStatus pStatus) {
    return new SubtaskResult(result, pStatus);
  }
  
  public static SubtaskResult soundAndPreciseWithResult(final Result result) {
    return new SubtaskResult(result, AlgorithmStatus.SOUND_AND_PRECISE);
  }

  public static SubtaskResult create(final Result pResult, final AlgorithmStatus pStatus) {
    return new SubtaskResult(pResult, pStatus);
  }
}
