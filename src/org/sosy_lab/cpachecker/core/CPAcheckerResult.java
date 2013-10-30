/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core;

import java.io.PrintStream;

import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

import com.google.common.base.Preconditions;

/**
 * Class that represents the result of a CPAchecker analysis.
 */
public class CPAcheckerResult {

  /**
   * Enum for the possible outcomes of a CPAchecker analysis:
   * - UNKNOWN: analysis did not terminate
   * - UNSAFE: bug found
   * - SAFE: no bug found
   */
  public static enum Result { NOT_YET_STARTED, UNKNOWN, UNSAFE, SAFE }

  private final Result result;

  private final ReachedSet reached;

  private final Statistics stats;

  CPAcheckerResult(Result result, ReachedSet reached, Statistics stats) {
    Preconditions.checkNotNull(result);
    this.result = result;
    this.reached = reached;
    this.stats = stats;
  }

  /**
   * Return the result of the analysis.
   */
  public Result getResult() {
    return result;
  }

  /**
   * Return the final reached set.
   */
  public UnmodifiableReachedSet getReached() {
    return reached;
  }

  /**
   * Write the statistics to a given PrintWriter. Additionally some output files
   * may be written here, if configuration says so.
   */
  public void printStatistics(PrintStream target) {
    if (stats != null && reached != null && result != Result.NOT_YET_STARTED) {
      stats.printStatistics(target, result, reached);
    }
  }

  public void printResult(PrintStream out) {
    if (result == Result.NOT_YET_STARTED) {
      return;
    }

    out.print("Verification result: ");
    switch (result) {
    case UNKNOWN:
      out.println("UNKNOWN, incomplete analysis.");
      break;
    case UNSAFE:
      out.println("UNSAFE. Error path found by chosen configuration.");
      break;
    case SAFE:
      out.println("SAFE. No error path found by chosen configuration.");
      break;
    default:
      out.println("UNKNOWN result: " + result);
    }
  }
}
