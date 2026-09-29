// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.DssConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.witness.DssWitnessArgStateCollector;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.test.TestUtils;

public class DssObserverWorkerTest {

  private DssObserverWorker observerWithError(ShutdownManager pShutdown, Throwable pError)
      throws Exception {
    var factory =
        new DssMessageFactory(new DssAnalysisOptions(TestUtils.configurationForTest().build()));
    var observer =
        new DssObserverWorker(
            "observer",
            mock(DssConnection.class),
            mock(BlockGraph.class),
            factory,
            LogManager.createNullLogManager(),
            mock(DssWitnessArgStateCollector.class),
            pShutdown.getNotifier());
    observer.processMessage(factory.createDssExceptionMessage("worker", pError));
    return observer;
  }

  @Test
  public void stageTimeoutRemainsAnInterruption() throws Exception {
    var stageShutdown = ShutdownManager.create();
    var workerShutdown = ShutdownManager.createWithParent(stageShutdown.getNotifier());
    var observer =
        observerWithError(workerShutdown, new InterruptedException("worker interrupted"));
    stageShutdown.requestShutdown("The walltime limit of 30s has elapsed.");

    var failure = assertThrows(InterruptedException.class, observer::observe);
    assertThat(failure).hasMessageThat().isEqualTo("The walltime limit of 30s has elapsed.");
  }

  @Test
  public void unexpectedWorkerInterruptionRemainsAnError() throws Exception {
    var observer =
        observerWithError(
            ShutdownManager.create(), new InterruptedException("unexpected interrupt"));

    var failure = assertThrows(CPAException.class, observer::observe);
    assertThat(failure).hasMessageThat().contains("unexpected interrupt");
  }

  @Test
  public void workerFailureRemainsAnError() throws Exception {
    var observer = observerWithError(ShutdownManager.create(), new CPAException("analysis failed"));

    var failure = assertThrows(CPAException.class, observer::observe);
    assertThat(failure).hasMessageThat().contains("analysis failed");
  }
}
