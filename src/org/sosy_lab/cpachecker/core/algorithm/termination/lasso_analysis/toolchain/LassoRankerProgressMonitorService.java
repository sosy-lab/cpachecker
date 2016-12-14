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

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;

import de.uni_freiburg.informatik.ultimate.core.model.services.IProgressAwareTimer;
import de.uni_freiburg.informatik.ultimate.core.model.services.IProgressMonitorService;

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
    throw new UnsupportedOperationException();
  }

  @Override
  public IProgressAwareTimer getChildTimer(double pPercentage) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setSubtask(String pTask) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setDeadline(long pDate) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void cancelToolchain() {
    throw new UnsupportedOperationException();
  }
}
