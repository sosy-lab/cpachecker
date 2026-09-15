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

public class DssActors implements AutoCloseable {

  private final ImmutableList<DssObserverWorker> observers;
  private final ImmutableList<DssAnalysisWorker> analysisWorkers;
  private final ImmutableList<DssActor> actors;
  private final ImmutableList<DssActor> remainingActors;

  public DssActors(ImmutableList<DssActor> pActors) {
    ImmutableList.Builder<DssObserverWorker> observerBuilder = ImmutableList.builder();
    ImmutableList.Builder<DssAnalysisWorker> analysisWorkerBuilder = ImmutableList.builder();
    ImmutableList.Builder<DssActor> remainingBuilder = ImmutableList.builder();

    for (DssActor actor : pActors) {
      if (actor instanceof DssObserverWorker observer) {
        observerBuilder.add(observer);
      } else if (actor instanceof DssAnalysisWorker analysisWorker) {
        analysisWorkerBuilder.add(analysisWorker);
      } else {
        remainingBuilder.add(actor);
      }
    }

    observers = observerBuilder.build();
    analysisWorkers = analysisWorkerBuilder.build();
    actors = ImmutableList.copyOf(pActors);
    remainingActors = remainingBuilder.build();
  }

  public ImmutableList<DssObserverWorker> getObservers() {
    return observers;
  }

  public ImmutableList<DssActor> getRemainingActors() {
    return remainingActors;
  }

  public ImmutableList<DssAnalysisWorker> getAnalysisWorkers() {
    return analysisWorkers;
  }

  public ImmutableList<DssActor> getActors() {
    return actors;
  }

  public DssActor getOnlyActor() {
    return Iterables.getOnlyElement(actors);
  }

  public int size() {
    return actors.size();
  }

  @Override
  public void close() {
    // close analysis workers to free resources
    for (var actor : analysisWorkers) {
      actor.close();
    }
  }
}
