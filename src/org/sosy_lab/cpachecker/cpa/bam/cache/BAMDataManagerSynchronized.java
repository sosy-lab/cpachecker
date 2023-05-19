// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam.cache;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.bam.AbstractBAMCPA;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMCache.BAMCacheEntry;

public class BAMDataManagerSynchronized implements BAMDataManager {

  private final BAMDataManager manager;

  public BAMDataManagerSynchronized(
      AbstractBAMCPA pBamCpa,
      BAMCache pCache,
      ReachedSetFactory pReachedsetFactory,
      LogManager pLogger) {
    manager = new BAMDataManagerImpl(pBamCpa, pCache, pReachedsetFactory, pLogger);
  }

  @Override
  public void replaceStateInCaches(
      AbstractState pOldState, AbstractState pNewState, boolean pOldStateMustExist) {
    synchronized (this) {
      manager.replaceStateInCaches(pOldState, pNewState, pOldStateMustExist);
    }
  }

  @Override
  public BAMCacheEntry createAndRegisterNewReachedSet(
      AbstractState pInitialState, Precision pInitialPrecision, Block pContext) {
    synchronized (this) {
      return manager.createAndRegisterNewReachedSet(pInitialState, pInitialPrecision, pContext);
    }
  }

  @Override
  public ReachedSetFactory getReachedSetFactory() {
    synchronized (this) {
      return manager.getReachedSetFactory();
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
  public void registerInitialState(
      AbstractState pState, AbstractState pExitState, ReachedSet pReachedSet) {
    synchronized (this) {
      manager.registerInitialState(pState, pExitState, pReachedSet);
    }
  }

  @Override
  public ReachedSet getReachedSetForInitialState(AbstractState pState, AbstractState pExitState) {
    synchronized (this) {
      return manager.getReachedSetForInitialState(pState, pExitState);
    }
  }

  @Override
  public boolean hasInitialState(AbstractState pState) {
    synchronized (this) {
      return manager.hasInitialState(pState);
    }
  }

  @Override
  public ImmutableSet<AbstractState> getNonReducedInitialStates(AbstractState pReducedState) {
    synchronized (this) {
      return manager.getNonReducedInitialStates(pReducedState);
    }
  }

  @Override
  public AbstractState getReducedStateForExpandedState(AbstractState pState) {
    synchronized (this) {
      return manager.getReducedStateForExpandedState(pState);
    }
  }

  @Override
  public Block getInnerBlockForExpandedState(AbstractState pState) {
    synchronized (this) {
      return manager.getInnerBlockForExpandedState(pState);
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

  @Override
  public boolean addUncachedBlockEntry(CFANode pNode) {
    // Not sure how the option works with ParallelBAM
    return true;
  }

  @Override
  public boolean isUncachedBlockEntry(CFANode pNode) {
    // Not sure how the option works with ParallelBAM
    return false;
  }

  @Override
  public String toString() {
    synchronized (this) {
      return manager.toString();
    }
  }
}
