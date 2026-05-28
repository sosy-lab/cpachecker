// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.executors;

import com.google.common.collect.ImmutableList;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

/**
 * A class that can delegate to multiple {@link Statistics} objects. Normally, the statistics are
 * collected before the analysis, where we might not know yet what we have.
 */
public class CollectingCompositeStatistics implements Statistics {

  private List<Statistics> children = new ArrayList<>();

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    for (Statistics s : children) {
      s.printStatistics(pOut, pResult, pReached);
    }
  }

  @Override
  public void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {
    for (Statistics s : children) {
      s.writeOutputFiles(pResult, pReached);
    }
  }

  @Override
  public @Nullable String getName() {
    return "Collection: "
        + children.stream().map(s -> s.getName()).collect(Collectors.joining(", "));
  }

  public void addChild(Statistics s) {
    children.add(s);
  }

  public void addAll(ImmutableList<Statistics> pWorkersWithStats) {
    children.addAll(pWorkersWithStats);
  }
}
