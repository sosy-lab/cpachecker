// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.task;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.ShareableBooleanFormula;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class TaskManager {
  private final Configuration config;

  private final Specification specification;

  private final LogManager logManager;

  private final ShutdownNotifier shutdownNotifier;

  private final CFA cfa;

  private final TaskExecutor executor;

  private TaskManager(
      final Configuration pConfig,
      final Specification pSpecification,
      final LogManager pLogManager,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCFA,
      final TaskExecutor pTaskExecutor) {
    config = pConfig;
    specification = pSpecification;
    logManager = pLogManager;
    shutdownNotifier = pShutdownNotifier;
    cfa = pCFA;
    executor = pTaskExecutor;
  }

  public static TaskManagerFactory factory() {
    return new TaskManagerFactory();
  }

  public void spawnForwardAnalysis(
      final Block pPredecessor,
      final int pPredecessorExpectedVersion,
      final Block pBlock,
      final ShareableBooleanFormula pNewPrecondition)
      throws InterruptedException, InvalidConfigurationException, CPAException {

    ForwardAnalysis task =
        new ForwardAnalysis(
            pPredecessor,
            pBlock,
            pNewPrecondition,
            pPredecessorExpectedVersion,
            config,
            specification,
            logManager,
            shutdownNotifier,
            cfa,
            this);

    executor.requestJob(task);
  }

  public void spawnForwardAnalysis(final Block pBlock)
      throws InterruptedException, InvalidConfigurationException, CPAException {
    ForwardAnalysis task =
        new ForwardAnalysis(
            null, pBlock, null, 0, config, specification, logManager, shutdownNotifier, cfa, this);

    executor.requestJob(task);
  }

  public static class TaskManagerFactory {
    private Configuration config = null;
    private Specification specification = null;
    private LogManager logManager = null;
    private ShutdownNotifier shutdownNotifier = null;
    private CFA cfa = null;
    private TaskExecutor executor = null;

    public <T> TaskManagerFactory set(T pObject, Class<T> pClass)
        throws UnsupportedOperationException {
      if (pClass == Configuration.class) {
        config = (Configuration) pObject;
      } else if (pClass == Specification.class) {
        specification = (Specification) pObject;
      } else if (pClass == LogManager.class) {
        logManager = (LogManager) pObject;
      } else if (pClass == ShutdownNotifier.class) {
        shutdownNotifier = (ShutdownNotifier) pObject;
      } else if (pClass == CFA.class) {
        cfa = (CFA) pObject;
      } else if (pClass == TaskExecutor.class) {
        executor = (TaskExecutor) pObject;
      } else {
        final String message = "TaskFactory requires no object of type " + pClass;
        throw new UnsupportedOperationException(message);
      }

      return this;
    }

    public TaskManager createInstance() {
      return new TaskManager(config, specification, logManager, shutdownNotifier, cfa, executor);
    }
  }
}
