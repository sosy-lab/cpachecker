// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints;

import java.io.PrintStream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

/** Statistics of the {@link ConstraintsCPA} and all related components. */
public class ConstraintsStatistics implements Statistics {

  public final StatTimer trivialRemovalTime = new StatTimer("Time for trivial constraint removal");
  public final StatTimer outdatedRemovalTime =
      new StatTimer("Time for outdated constraint " + "removal");
  public final StatInt removedTrivial =
      new StatInt(StatKind.SUM, "Number of removed trivial " + "constraints");
  public final StatInt removedOutdated =
      new StatInt(StatKind.SUM, "Number of removed outdated " + "constraints");

  public final StatTimer timeForSolving =
      new StatTimer(StatKind.SUM, "Time for solving constraints");
  public final StatTimer timeForIndependentComputation =
      new StatTimer(StatKind.SUM, "Time for independent computation");
  public final StatTimer timeForDefinitesComputation =
      new StatTimer(StatKind.SUM, "Time for resolving definites");
  public final StatTimer timeForModelReuse =
      new StatTimer(StatKind.SUM, "Time for model re-use attempts");
  public final StatTimer timeForSatCheck = new StatTimer(StatKind.SUM, "Time for SMT check");
  public final StatCounter modelReuseSuccesses = new StatCounter("Successful model re-uses");

  public StatCounter cacheLookups = new StatCounter("Cache lookups");
  public StatTimer directCacheLookupTime = new StatTimer(StatKind.SUM, "Direct cache lookup time");
  public StatCounter directCacheHits = new StatCounter("Direct cache hits");
  public StatTimer subsetLookupTime = new StatTimer(StatKind.SUM, "Subset cache lookup time");
  public StatCounter subsetCacheHits = new StatCounter("Subset cache hits");
  public StatTimer supersetLookupTime = new StatTimer(StatKind.SUM, "Superset cache lookup time");
  public StatCounter supersetCacheHits = new StatCounter("Superset cache hits");

  public StatInt constraintNumberBeforeAdj =
      new StatInt(StatKind.SUM, "Constraints before refinement in state");
  public StatInt constraintNumberAfterAdj =
      new StatInt(StatKind.SUM, "Constraints after refinement in state");
  public final StatTimer adjustmentTime =
      new StatTimer(StatKind.SUM, "Time for constraints adjustment");

  public StatCounter constraintsRemovedInMerge =
      new StatCounter("Number of constraints removed in merge");

  private String name;

  /**
   * Creates a new <code>ConstraintsStatistics</code> object with a default name. This name is used
   * to identify the statistics in the output.
   */
  public ConstraintsStatistics() {
    name = ConstraintsCPA.class.getSimpleName();
  }

  /**
   * Creates a new <code>ConstraintsStatistics</code> object with the given name. This name is used
   * to identify the statistics in the output.
   */
  public ConstraintsStatistics(String pName) {
    name = pName;
  }

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {

    StatisticsWriter.writingStatisticsTo(out)
        .spacer() // Constraints solver
        .putIfUpdatedAtLeastOnce(timeForSolving)
        .beginLevel()
        .putIfUpdatedAtLeastOnce(timeForIndependentComputation)
        .putIfUpdatedAtLeastOnce(timeForModelReuse)
        .putIfUpdatedAtLeastOnce(timeForSatCheck)
        .putIfUpdatedAtLeastOnce(timeForDefinitesComputation)
        .endLevel()
        .putIfUpdatedAtLeastOnce(modelReuseSuccesses)
        .spacer() // Direct constraints solver cache
        .putIf(cacheLookups.getUpdateCount() > 0, cacheLookups)
        .putIf(cacheLookups.getUpdateCount() > 0, directCacheHits)
        .putIfUpdatedAtLeastOnce(directCacheLookupTime)
        // Subset constraints solver cache
        .putIf(subsetLookupTime.getUpdateCount() > 0, subsetCacheHits)
        .putIf(subsetLookupTime.getUpdateCount() > 0, subsetLookupTime)
        // Superset constraints solver cache
        .putIf(supersetLookupTime.getUpdateCount() > 0, supersetCacheHits)
        .putIf(supersetLookupTime.getUpdateCount() > 0, supersetLookupTime)
        .spacer() // Constraints state simplifier
        .putIf(trivialRemovalTime.getUpdateCount() > 0, removedTrivial)
        .putIf(trivialRemovalTime.getUpdateCount() > 0, trivialRemovalTime)
        .putIf(outdatedRemovalTime.getUpdateCount() > 0, removedOutdated)
        .putIf(outdatedRemovalTime.getUpdateCount() > 0, outdatedRemovalTime)
        .spacer() // Precision adjustment
        .putIfUpdatedAtLeastOnce(constraintNumberAfterAdj)
        .putIfUpdatedAtLeastOnce(constraintNumberBeforeAdj)
        .putIfUpdatedAtLeastOnce(adjustmentTime);
  }

  @Nullable
  @Override
  public String getName() {
    return name;
  }
}
