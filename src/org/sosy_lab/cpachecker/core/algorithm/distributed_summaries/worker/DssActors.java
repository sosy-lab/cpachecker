// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;

public class DssActors implements AutoCloseable {

  private final ImmutableList<DssObserverWorker> observers;
  private final ImmutableList<DssAnalysisWorker> analysisWorkers;
  private final ImmutableList<Statistics> workersWithStats;
  private final ImmutableList<DssActor> actors;

  public DssActors(ImmutableList<DssActor> pActors) {
    ImmutableList.Builder<DssObserverWorker> observerBuilder = ImmutableList.builder();
    ImmutableList.Builder<DssAnalysisWorker> analysisWorkerBuilder = ImmutableList.builder();
    ImmutableList.Builder<Statistics> statsBuilder = ImmutableList.builder();

    for (DssActor actor : pActors) {
      if (actor instanceof DssObserverWorker observer) {
        observerBuilder.add(observer);
        statsBuilder.add(observer);
      } else if (actor instanceof DssAnalysisWorker analysisWorker) {
        analysisWorkerBuilder.add(analysisWorker);
        if (analysisWorker instanceof Statistics statistics) {
          statsBuilder.add(statistics);
        }
      }
    }

    observers = observerBuilder.build();
    analysisWorkers = analysisWorkerBuilder.build();
    workersWithStats = statsBuilder.build();
    actors = ImmutableList.copyOf(pActors);
  }

  public ImmutableList<DssObserverWorker> getObservers() {
    return observers;
  }

  public ImmutableList<DssAnalysisWorker> getAnalysisWorkers() {
    return analysisWorkers;
  }

  public ImmutableList<Statistics> getWorkersWithStats() {
    return workersWithStats;
  }

  public ImmutableList<DssActor> getActors() {
    return actors;
  }

  public DssActor getOnlyActor() {
    return Iterables.getOnlyElement(actors);
  }

  @Override
  public void close() {
    // close analysis workers to free resources
    for (var actor : analysisWorkers) {
      actor.close();
    }
  }
}
