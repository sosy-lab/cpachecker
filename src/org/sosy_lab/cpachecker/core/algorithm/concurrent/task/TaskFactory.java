// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.task;

import static com.google.common.base.Preconditions.checkState;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.JobExecutor;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.BooleanFormula;

@SuppressWarnings("FieldCanBeLocal")
public class TaskFactory {
  private Configuration config = null;

  private Specification specification = null;

  private LogManager logManager= null;

  private ShutdownNotifier shutdownNotifier = null;

  private CFA cfa = null;

  private JobExecutor executor = null;

  public TaskFactory() {
  }

  public <T> TaskFactory set(T pObject, Class<T> pClass) throws UnsupportedOperationException {
    if(pClass == Configuration.class) {
      config = (Configuration) pObject;
    }
    else if(pClass == Specification.class) {
      specification = (Specification) pObject;
    }
    else if(pClass == LogManager.class) {
      logManager = (LogManager) pObject;
    }
    else if(pClass == ShutdownNotifier.class) {
      shutdownNotifier = (ShutdownNotifier) pObject;
    }
    else if(pClass == CFA.class) {
      cfa = (CFA) pObject;
    }
    else if(pClass == JobExecutor.class) {
      executor = (JobExecutor) pObject;
    }
    else {
      final String message = "TaskFactory requires no object of type " + pClass;
      throw new UnsupportedOperationException(message);
    }

    return this;
  }

  private void checkFactoryReadiness() {
    final String message = "Task Factory is missing: ";
    checkState(config != null, message + "Configuration");
    checkState(specification != null, message + "Specification");
    checkState(logManager != null, message + "LogManager");
    checkState(shutdownNotifier != null, message + "ShutdownNotifier");
    checkState(cfa != null, message + "CFA");
    checkState(executor != null, message + "JobExecutor");
  }

  public ForwardAnalysis createForwardAnalysis(
      final Block pBlock, final BooleanFormula pNewPrecondition)
      throws InterruptedException, InvalidConfigurationException, CPAException {
    checkFactoryReadiness();
    return new ForwardAnalysis(
        pBlock, pNewPrecondition, config, specification, logManager, shutdownNotifier, cfa, this
    );
  }

  public ForwardAnalysis createForwardAnalysis(final Block pBlock)
      throws InterruptedException, InvalidConfigurationException, CPAException {
    checkFactoryReadiness();
    return new ForwardAnalysis(
        pBlock, null, config, specification, logManager, shutdownNotifier, cfa, this
    );
  }

  public JobExecutor getExecutor() {
    return executor;
  }
}
