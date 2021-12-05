// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.message;

import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.ConcurrentStatisticsCollector.TaskStatistics;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.Scheduler;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.completion.ErrorReachedProgramEntryMessage;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.completion.StatsReportMessage;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.completion.TaskAbortedMessage;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.completion.TaskCompletedMessage;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.request.backward.BackwardAnalysisContinuationRequest;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.request.backward.BackwardAnalysisRequest;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.request.forward.ForwardAnalysisContinuationRequest;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.request.forward.ForwardAnalysisRequest;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.Task;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.util.ErrorOrigin;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.util.ReusableCoreComponents;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.util.ShareableBooleanFormula;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

public class MessageFactory {
  private final Configuration config;

  private final Specification specification;

  private final LogManager logManager;

  private final ShutdownNotifier shutdownNotifier;

  private final CFA cfa;

  private final Scheduler executor;

  private MessageFactory(
      final Configuration pConfig,
      final Specification pSpecification,
      final LogManager pLogManager,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCFA,
      final Scheduler pScheduler) {
    config = pConfig;
    specification = pSpecification;
    logManager = pLogManager;
    shutdownNotifier = pShutdownNotifier;
    cfa = pCFA;
    executor = pScheduler;
  }

  public static TaskManagerFactory factory() {
    return new TaskManagerFactory();
  }

  public void sendForwardAnalysisRequest(
      final Block pPredecessor,
      final int pPredecessorExpectedVersion,
      final Block pBlock,
      final ShareableBooleanFormula pNewPrecondition)
      throws InterruptedException, InvalidConfigurationException, CPAException {
    Message request =
        new ForwardAnalysisRequest(
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

    executor.sendMessage(request);
  }

  public void sendForwardAnalysisRequest(final Block pBlock)
      throws InterruptedException, InvalidConfigurationException, CPAException {
    Message message =
        new ForwardAnalysisRequest(
            null, pBlock, null, 0, config, specification, logManager, shutdownNotifier, cfa,
            this);
    executor.sendMessage(message);
  }

  public void sendForwardAnalysisContinuationRequest(
      final Block pTarget,
      final int pExpectedVersion,
      final ARGCPA pCPA,
      final Algorithm pAlgorithm,
      final ReachedSet pReachedSet,
      final Solver pSolver,
      final PathFormulaManager pPfMgr)
      throws InterruptedException, InvalidConfigurationException, CPAException {
    Message message = new ForwardAnalysisContinuationRequest(
        config, pTarget, pExpectedVersion, pCPA, pAlgorithm, pReachedSet, pSolver, pPfMgr, this,
        logManager,
        shutdownNotifier
    );
    executor.sendMessage(message);
  }

  public void sendBackwardAnalysisRequest(
      final Block pBlock,
      final CFANode pStart,
      final ErrorOrigin pOrigin)
      throws InterruptedException, InvalidConfigurationException, CPAException {
    assert pBlock.contains(pStart) : "Block must contain analysis start location";

    Message message =
        new BackwardAnalysisRequest(config,
            pBlock, pOrigin, pStart, null, null, config, logManager, shutdownNotifier, cfa, this);
    executor.sendMessage(message);
  }

  public void sendBackwardAnalysisRequest(
      final Block pBlock,
      final CFANode pStart,
      final Block pSource,
      final ErrorOrigin pOrigin,
      final ShareableBooleanFormula pCondition)
      throws InterruptedException, InvalidConfigurationException, CPAException {
    assert pBlock.contains(pStart) : "Block must contain analysis start location";

    Message message =
        new BackwardAnalysisRequest(config,
            pBlock, pOrigin, pStart, pSource, pCondition, config, logManager, shutdownNotifier, cfa,
            this);
    executor.sendMessage(message);
  }

  public void sendBackwardAnalysisContinuationRequest(
      final Block pBlock,
      final ErrorOrigin pOrigin,
      final ReachedSet pReachedSet,
      final Algorithm pAlgorithm,
      final ARGCPA pCPA, final Solver pSolver) {
    Message message =
        new BackwardAnalysisContinuationRequest(config,
            pBlock, pOrigin, pReachedSet, pAlgorithm, pCPA, pSolver, this, logManager,
            shutdownNotifier);

    executor.sendMessage(message);
  }

  public void sendForwardAnalysisContinuationRequest(
      final Block pBlock,
      final ErrorOrigin pOrigin,
      final ReachedSet pReachedSet,
      final Algorithm pAlgorithm,
      final ARGCPA pCPA, final Solver pSolver) {
    Message message =
        new BackwardAnalysisContinuationRequest(config,
            pBlock, pOrigin, pReachedSet, pAlgorithm, pCPA, pSolver, this, logManager,
            shutdownNotifier);

    executor.sendMessage(message);
  }

  public void sendTaskCompletedMessage(
      final Task pTask, final AlgorithmStatus pStatus,
      final TaskStatistics pStatistics) {
    Message msg = new TaskCompletedMessage(pTask, pStatus, pStatistics);
    executor.sendMessage(msg);
  }

  public void sendStatsReportMessage(
      final Task pTask, final TaskStatistics pStatistics) {
    Message msg = new StatsReportMessage(pTask, pStatistics);
    executor.sendMessage(msg);
  }

  public void sendTaskAbortedMessage(final Task pTask) {
    Message msg = new TaskAbortedMessage(pTask);
    executor.sendMessage(msg);
  }

  public void sendErrorReachedProgramEntryMessage(
      final ErrorOrigin pOrigin,
      final AlgorithmStatus pStatus) {
    Message msg = new ErrorReachedProgramEntryMessage(pOrigin, pStatus);
    executor.sendMessage(msg);
  }

  public Optional<ReusableCoreComponents> requestIdleForwardAnalysisComponents() {
    return executor.requestIdleForwardAnalysisComponents();
  }

  public Optional<ReusableCoreComponents> requestIdleBackwardAnalysisComponents() {
    return executor.requestIdleBackwardAnalysisComponents();
  }

  public static class TaskManagerFactory {
    private Configuration config = null;
    private Specification specification = null;
    private LogManager logManager = null;
    private ShutdownNotifier shutdownNotifier = null;
    private CFA cfa = null;
    private Scheduler executor = null;

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
      } else if (pClass == Scheduler.class) {
        executor = (Scheduler) pObject;
      } else {
        final String message = "TaskFactory requires no object of type " + pClass;
        throw new UnsupportedOperationException(message);
      }

      return this;
    }

    public MessageFactory createInstance() {
      return new MessageFactory(config, specification, logManager, shutdownNotifier, cfa, executor);
    }
  }
}
