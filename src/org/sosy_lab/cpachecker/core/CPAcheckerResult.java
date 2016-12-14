/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

import java.io.PrintStream;

import javax.annotation.Nullable;

/**
 * Class that represents the result of a CPAchecker analysis.
 */
public class CPAcheckerResult {

  /**
   * Enum for the possible outcomes of a CPAchecker analysis:
   * - UNKNOWN: analysis did not terminate
   * - FALSE: bug found
   * - TRUE: no bug found
   */
  public static enum Result { NOT_YET_STARTED, UNKNOWN, FALSE, TRUE }

  private final Result result;

  private final String violatedPropertyDescription;

  private final @Nullable ReachedSet reached;

  private final @Nullable CFA cfa;

  private final @Nullable Statistics stats;

  private @Nullable Statistics proofGeneratorStats = null;

  private final boolean programNeverTerminates;

  CPAcheckerResult(
      Result result,
      String violatedPropertyDescription,
      @Nullable ReachedSet reached,
      @Nullable CFA cfa,
      @Nullable Statistics stats,
      boolean programNeverTerminates) {
    this.violatedPropertyDescription = checkNotNull(violatedPropertyDescription);
    this.result = checkNotNull(result);
    this.reached = reached;
    this.cfa = cfa;
    this.stats = stats;
    this.programNeverTerminates = programNeverTerminates;
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
  public @Nullable UnmodifiableReachedSet getReached() {
    return reached;
  }

  /**
   * Return the CFA.
   */
  public @Nullable CFA getCfa() {
    return cfa;
  }

  public void addProofGeneratorStatistics(Statistics pProofGeneratorStatistics) {
    proofGeneratorStats = pProofGeneratorStatistics;
  }

  /**
   * Write the statistics to a given PrintWriter. Additionally some output files
   * may be written here, if configuration says so.
   */
  public void printStatistics(PrintStream target) {
    if (stats != null) {
      stats.printStatistics(target, result, reached);
    }
    if (proofGeneratorStats != null) {
      proofGeneratorStats.printStatistics(target, result, reached);
    }
  }

  public void printResult(PrintStream out) {
    if (result == Result.NOT_YET_STARTED) {
      return;
    }

    if (programNeverTerminates) {
      out.println("The program will never terminate.");
    }

    out.println("Verification result: " + getResultString());
  }

  public String getResultString() {
    switch (result) {
      case UNKNOWN:
        return "UNKNOWN, incomplete analysis.";
      case FALSE:
        StringBuilder sb = new StringBuilder();
        sb.append("FALSE. Property violation");
        if (!violatedPropertyDescription.isEmpty()) {
          sb.append(" (").append(violatedPropertyDescription).append(")");
        }
        sb.append(" found by chosen configuration.");
        return sb.toString();
      case TRUE:
        return "TRUE. No property violation found by chosen configuration.";
      default:
        return "UNKNOWN result: " + result;
    }
  }
}
