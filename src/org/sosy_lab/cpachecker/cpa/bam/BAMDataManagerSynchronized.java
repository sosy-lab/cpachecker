/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.bam;

import java.util.List;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

public class BAMDataManagerSynchronized implements BAMDataManager {

  private final BAMDataManager manager;

  BAMDataManagerSynchronized(BAMDataManager pManager) {
    manager = pManager;
  }

  @Override
  public void replaceStateInCaches(
      AbstractState pOldState, AbstractState pNewState, boolean pOldStateMustExist) {
    synchronized (this) {
      manager.replaceStateInCaches(pOldState, pNewState, pOldStateMustExist);
    }
  }

  @Override
  public ReachedSet createAndRegisterNewReachedSet(
      AbstractState pInitialState, Precision pInitialPrecision, Block pContext) {
    synchronized (this) {
      return manager.createAndRegisterNewReachedSet(pInitialState, pInitialPrecision, pContext);
    }
  }

  @Override
  public void registerExpandedState(
      AbstractState pExpandedState,
      Precision pExpandedPrecision,
      AbstractState pReducedState,
      Block pInnerBlock) {
    synchronized (this) {
      manager.registerExpandedState(pExpandedState, pExpandedPrecision, pReducedState, pInnerBlock);
    }
  }

  @Override
  public boolean alreadyReturnedFromSameBlock(AbstractState pState, Block pBlock) {
    synchronized (this) {
      return manager.alreadyReturnedFromSameBlock(pState, pBlock);
    }
  }

  @Override
  public AbstractState getInnermostState(AbstractState pState) {
    synchronized (this) {
      return manager.getInnermostState(pState);
    }
  }

  @Override
  public List<AbstractState> getExpandedStatesList(AbstractState pState) {
    synchronized (this) {
      return manager.getExpandedStatesList(pState);
    }
  }

  @Override
  public void registerInitialState(AbstractState pState, ReachedSet pReachedSet) {
    synchronized (this) {
      manager.registerInitialState(pState, pReachedSet);
    }
  }

  @Override
  public ReachedSet getReachedSetForInitialState(AbstractState pState) {
    synchronized (this) {
      return manager.getReachedSetForInitialState(pState);
    }
  }

  @Override
  public boolean hasInitialState(AbstractState pState) {
    synchronized (this) {
      return manager.hasInitialState(pState);
    }
  }

  @Override
  public AbstractState getReducedStateForExpandedState(AbstractState pState) {
    synchronized (this) {
      return manager.getReducedStateForExpandedState(pState);
    }
  }

  @Override
  public boolean hasExpandedState(AbstractState pState) {
    synchronized (this) {
      return manager.hasExpandedState(pState);
    }
  }

  @Override
  public BAMCache getCache() {
    synchronized (this) {
      return manager.getCache();
    }
  }

  @Override
  public void clearExpandedStateToExpandedPrecision() {
    synchronized (this) {
      manager.clearExpandedStateToExpandedPrecision();
    }
  }

  @Override
  @Nullable
  public Precision getExpandedPrecisionForState(AbstractState pState) {
    synchronized (this) {
      return manager.getExpandedPrecisionForState(pState);
    }
  }

  @Override
  public void clear() {
    synchronized (this) {
      manager.clear();
    }
  }
}
