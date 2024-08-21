// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.toolchain;

import de.uni_freiburg.informatik.ultimate.core.model.services.IProgressAwareTimer;
import de.uni_freiburg.informatik.ultimate.core.model.services.IProgressMonitorService;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import java.util.concurrent.CountDownLatch;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;

public class LassoRankerProgressMonitorService implements IProgressMonitorService {

  private final ShutdownManager shutdownManager;

  public LassoRankerProgressMonitorService(ShutdownNotifier pShutdownNotifier) {
    shutdownManager = ShutdownManager.createWithParent(pShutdownNotifier);
  }

  @Override
  public boolean continueProcessing() {
    return !shutdownManager.getNotifier().shouldShutdown();
  }

  @Override
  public IProgressAwareTimer getChildTimer(long pTimeout) {
    throw new UnsupportedOperationException(
        getClass() + "::getChildTimer(long) is not implemented");
  }

  @Override
  public IProgressAwareTimer getChildTimer(double pPercentage) {
    throw new UnsupportedOperationException(
        getClass() + "::getChildTimer(double) is not implemented");
  }

  @Override
  public void setSubtask(String pTask) {
    throw new UnsupportedOperationException(getClass() + "::setSubtask is not implemented");
  }

  @Override
  public void setDeadline(long pDate) {
    throw new UnsupportedOperationException(getClass() + "::setDeadline is not implemented");
  }

  @Override
  public IProgressAwareTimer getTimer(long pTimeout) {
    throw new UnsupportedOperationException(getClass() + "::getTimer is not implemented");
  }

  @Override
  public IProgressAwareTimer getParent() {
    throw new UnsupportedOperationException(getClass() + "::getParent is not implemented");
  }

  @Override
  public long getDeadline() {
    throw new UnsupportedOperationException(getClass() + "::getDeadline is not implemented");
  }

  @Override
  public CountDownLatch cancelToolchain() {
    throw new UnsupportedOperationException(getClass() + "::cancelToolchain is not implemented");
  }

  @Override
  public long remainingTime() {
    throw new UnsupportedOperationException(getClass() + "::remainingTime is not implemented");
  }

  @Override
  public IUltimateServiceProvider registerChildTimer(
      IUltimateServiceProvider pServices, IProgressAwareTimer pTimer) {
    throw new UnsupportedOperationException(getClass() + "::registerChildTimer is not implemented");
  }
}
