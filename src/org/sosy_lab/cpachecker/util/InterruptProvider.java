/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util;

import java.util.concurrent.atomic.AtomicReference;

import org.sosy_lab.common.ShutdownManager;

import com.google.common.base.Preconditions;

public class InterruptProvider {

  private final ShutdownManager irreversibleParent;

  private AtomicReference<ShutdownManager> temporaryManager;

  public InterruptProvider(ShutdownManager pIrreversibleParent) {
    this.irreversibleParent = Preconditions.checkNotNull(pIrreversibleParent);
    this.temporaryManager = new AtomicReference<>();

    reset();
  }

  public void reset() {
    temporaryManager.set(ShutdownManager.createWithParent(irreversibleParent.getNotifier()));
  }

  public void canInterrupt() throws InterruptedException {
    irreversibleParent.getNotifier().shutdownIfNecessary();
    temporaryManager.get().getNotifier().shutdownIfNecessary();
  }

  public ShutdownManager getReversibleManager() {
    return temporaryManager.get();
  }

  public boolean hasTemporaryInterruptRequest() {
    return temporaryManager.get().getNotifier().shouldShutdown();
  }


}
