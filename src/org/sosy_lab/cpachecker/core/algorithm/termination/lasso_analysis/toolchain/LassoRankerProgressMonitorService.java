/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.toolchain;

import de.uni_freiburg.informatik.ultimate.core.model.services.IProgressAwareTimer;
import de.uni_freiburg.informatik.ultimate.core.model.services.IProgressMonitorService;
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
  public boolean continueProcessingRoot() {
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
  public void addChildTimer(IProgressAwareTimer pTimer) {
    throw new UnsupportedOperationException(getClass() + "::addChildTimer is not implemented");
  }

  @Override
  public IProgressAwareTimer removeChildTimer() {
    throw new UnsupportedOperationException(getClass() + "::removeChildTimer is not implemented");
  }
}
