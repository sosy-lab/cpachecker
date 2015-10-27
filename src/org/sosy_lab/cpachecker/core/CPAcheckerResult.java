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

import java.io.PrintStream;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.PropertySummary;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

import com.google.common.base.Joiner;

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
  private final PropertySummary propertySummary;

  private final @Nullable ReachedSet reached;

  private final @Nullable Statistics stats;

  private @Nullable Statistics proofGeneratorStats = null;

  CPAcheckerResult(Result pResult,
        PropertySummary pSummary,
        @Nullable ReachedSet reached,
        @Nullable Statistics stats) {

    this.propertySummary = checkNotNull(pSummary);
    this.result = checkNotNull(pResult);

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

    out.print("Verification result: ");
    out.println(getResultString());

    out.println(String.format("\tNumber of violated properties: %d", propertySummary.getViolatedProperties().size()));

    if (propertySummary.getUnknownProperties().isPresent()) {
      out.println(String.format("\tNumber of unknown properties: %d", propertySummary.getUnknownProperties().get().size()));
    }

    out.println("\tStatus by property:");
    for (Property prop: propertySummary.getViolatedProperties()) {
      out.println(String.format("\t\tProperty %s: %s", prop.toString(), "FALSE"));
    }

    if (propertySummary.getUnknownProperties().isPresent()) {
      for (Property prop: propertySummary.getUnknownProperties().get()) {
        out.println(String.format("\t\tProperty %s: %s", prop.toString(), "UNKNOWN"));
      }

    }
  }

  private String getResultString() {
    StringBuilder ret = new StringBuilder();

    switch (result) {
      case UNKNOWN:
        ret.append("UNKNOWN, incomplete analysis.");
        break;
      case FALSE:
        ret.append("FALSE. Property violation");
        break;
      case TRUE:
        ret.append("TRUE. No property violation found by chosen configuration.");
        break;
      default:
        ret.append("UNKNOWN result: " + result);
    }

    if (!propertySummary.getViolatedProperties().isEmpty()) {
      ret.append(" (").append(Joiner.on(", ").join(propertySummary.getViolatedProperties())).append(")");
    }
    ret.append(" found by chosen configuration.");

    return ret.toString();
  }
}
