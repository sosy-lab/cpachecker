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

import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Predicates.*;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.getOnlyElement;

import java.io.PrintStream;
import java.util.Set;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.Targetable.ViolatedProperty;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;

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

  private final Set<ViolatedProperty> violatedProperties; // does not contain OTHER

  private final @Nullable ReachedSet reached;

  private final @Nullable Statistics stats;

  private @Nullable Statistics proofGeneratorStats = null;

  CPAcheckerResult(Result result, Set<ViolatedProperty> pProperties,
        @Nullable ReachedSet reached, @Nullable Statistics stats) {
    if (result == Result.FALSE) {
      checkArgument(!pProperties.isEmpty());
      violatedProperties = from(pProperties).filter(not(equalTo(ViolatedProperty.OTHER))).toSet();
    } else {
      checkArgument(pProperties.isEmpty());
      violatedProperties = ImmutableSet.of();
    }
    this.result = checkNotNull(result);
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
  }

  public String getResultString() {
    switch (result) {
      case UNKNOWN:
        return "UNKNOWN, incomplete analysis.";
      case FALSE:
        StringBuilder sb = new StringBuilder();
        sb.append("FALSE. ");
        switch (violatedProperties.size()) {
        case 0:
          sb.append("Property violation");
          break;
        case 1:
          sb.append("Violation of property ").append(getOnlyElement(violatedProperties));
          break;
        default:
          sb.append("Violation of properties ");
          Joiner.on(" and ").appendTo(sb, violatedProperties);
          break;
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
