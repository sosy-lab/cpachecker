// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;

import java.io.PrintStream;
import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ResultProviderReachedSet;
import org.sosy_lab.cpachecker.util.CPAs;

/** Class that represents the result of a CPAchecker analysis. */
public class CPAcheckerResult implements AutoCloseable {

  /** Enum for the possible outcomes of a CPAchecker analysis */
  public enum Result {
    /** Aborted during analysis setup */
    NOT_YET_STARTED,
    /** Terminated but no property should be checked */
    DONE,
    /** Not possible to determine whether property holds */
    UNKNOWN,
    /** Property violation found */
    FALSE,
    /** Property holds */
    TRUE,
  }

  private final Result result;

  private final String targetDescription;

  private final @Nullable ReachedSet reached;

  private final @Nullable CFA cfa;

  private final @Nullable ConfigurableProgramAnalysis cpa;

  private final LogManager logger;

  private final @Nullable Statistics stats;

  private @Nullable Statistics proofGeneratorStats = null;

  CPAcheckerResult(
      Result pResult,
      String pTargetDescription,
      @Nullable ReachedSet pReached,
      @Nullable CFA pCfa,
      @Nullable ConfigurableProgramAnalysis pCpa,
      LogManager pLogger,
      @Nullable Statistics pStats) {
    this.targetDescription = checkNotNull(pTargetDescription);
    this.result = checkNotNull(pResult);
    this.reached = pReached;
    this.cfa = pCfa;
    this.cpa = pCpa;
    this.logger = pLogger;
    this.stats = pStats;
  }

  private CPAcheckerResult(Result pResult) {
    this(pResult, "");
  }

  private CPAcheckerResult(Result pResult, String pTargetDescription) {
    this(pResult, pTargetDescription, null, null, null, LogManager.createNullLogManager(), null);
  }

  /** Return the result of the analysis. */
  public Result getResult() {
    return result;
  }

  /**
   * Return information about the reached target. If the result does not contain a target, then
   * calling this method will result in an error.
   */
  public String getTargetDescription() {
    checkState(result == Result.FALSE);
    return targetDescription;
  }

  /** Return the final reached set. */
  public @Nullable ReachedSet getReached() {
    return reached;
  }

  /** Return the CFA. */
  public @Nullable CFA getCfa() {
    return cfa;
  }

  public void addProofGeneratorStatistics(Statistics pProofGeneratorStatistics) {
    proofGeneratorStats = pProofGeneratorStatistics;
  }

  /**
   * Write the statistics to a given PrintWriter. Additionally, some output files may be written
   * here, if configuration says so.
   */
  public void printStatistics(PrintStream target) {
    if (stats != null) {
      stats.printStatistics(target, result, reached);
    }
    if (proofGeneratorStats != null) {
      proofGeneratorStats.printStatistics(target, result, reached);
    }
  }

  public void writeOutputFiles() {
    if (result == Result.NOT_YET_STARTED) {
      return;
    }

    stats.writeOutputFiles(result, reached);
    if (proofGeneratorStats != null) {
      proofGeneratorStats.writeOutputFiles(result, reached);
    }
  }

  public void printResult(PrintStream out) {
    if (result == Result.NOT_YET_STARTED) {
      return;
    }

    if (reached instanceof ResultProviderReachedSet resultProviderReachedSet) {
      resultProviderReachedSet.printResults(out);
    }
    if (result == Result.DONE) {
      out.println("Finished.");
    } else {
      out.println("Verification result: " + getResultString());
    }
  }

  public String getResultString() {
    switch (result) {
      case UNKNOWN -> {
        return "UNKNOWN, incomplete analysis.";
      }
      case FALSE -> {
        StringBuilder sb = new StringBuilder();
        sb.append("FALSE. Property violation");
        if (!targetDescription.isEmpty()) {
          sb.append(" (").append(targetDescription).append(")");
        }
        sb.append(" found by chosen configuration.");
        return sb.toString();
      }
      case TRUE -> {
        return "TRUE. No property violation found by chosen configuration.";
      }
      default -> throw new AssertionError(result);
    }
  }

  public static Optional<CPAcheckerResult> parseResultString(String pResult) {
    Objects.requireNonNull(pResult);

    if (pResult.startsWith("Verification result: ")) {
      String property = pResult.substring(21);
      if (property.equals("TRUE. No property violation found by chosen configuration.")) {
        return Optional.of(new CPAcheckerResult(Result.TRUE));
      } else if (property.startsWith("FALSE. Property violation")) {
        if (property.contains("(")) {
          String propertyDesc =
              property.substring(property.indexOf("(") + 1, property.indexOf(")"));
          return Optional.of(new CPAcheckerResult(Result.FALSE, propertyDesc));
        }
        return Optional.of(new CPAcheckerResult(Result.FALSE));
      } else {
        verify(property.equals("UNKNOWN, incomplete analysis."));
        return Optional.of(new CPAcheckerResult(Result.UNKNOWN));
      }
    }

    return Optional.empty();
  }

  public Statistics getStatistics() {
    return stats;
  }

  @Override
  public void close() {
    if (cpa != null) {
      CPAs.closeCpaIfPossible(cpa, logger);
    }
  }
}
