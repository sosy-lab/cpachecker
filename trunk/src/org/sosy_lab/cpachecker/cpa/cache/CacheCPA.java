/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.cache;

import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;

import java.util.HashMap;
import java.util.Map;

/*
 * CAUTION: The cache for precision adjustment is only correct for CPAs that do
 * _NOT_ depend on the reached set when performing prec.
 */
public class CacheCPA implements ConfigurableProgramAnalysis, WrapperCPA {

  private final ConfigurableProgramAnalysis mCachedCPA;
  private final Map<CFANode, AbstractState> mInitialStatesCache;
  private final Map<CFANode, Precision> mInitialPrecisionsCache;
  private final CacheTransferRelation mCacheTransferRelation;
  private final CachePrecisionAdjustment mCachePrecisionAdjustment;
  private final CacheMergeOperator mCacheMergeOperator;

  public static CPAFactory factory() {
    return new AutomaticCPAFactory(CacheCPA.class);
  }

  public CacheCPA(ConfigurableProgramAnalysis pCachedCPA) {
    mCachedCPA = pCachedCPA;
    mInitialStatesCache = new HashMap<>();
    mInitialPrecisionsCache = new HashMap<>();
    mCacheTransferRelation = new CacheTransferRelation(mCachedCPA.getTransferRelation());
    mCachePrecisionAdjustment = new CachePrecisionAdjustment(mCachedCPA.getPrecisionAdjustment());
    mCacheMergeOperator = new CacheMergeOperator(mCachedCPA.getMergeOperator());
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return mCachedCPA.getAbstractDomain();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return mCacheTransferRelation;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mCacheMergeOperator;
  }

  @Override
  public StopOperator getStopOperator() {
    return mCachedCPA.getStopOperator();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return mCachePrecisionAdjustment;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) throws InterruptedException {
    AbstractState lInitialState = mInitialStatesCache.get(pNode);

    if (lInitialState == null) {
      lInitialState = mCachedCPA.getInitialState(pNode, pPartition);
      mInitialStatesCache.put(pNode, lInitialState);
    }

    return lInitialState;
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) throws InterruptedException {
    Precision lInitialPrecision = mInitialPrecisionsCache.get(pNode);

    if (lInitialPrecision == null) {
      lInitialPrecision = mCachedCPA.getInitialPrecision(pNode, pPartition);
      mInitialPrecisionsCache.put(pNode, lInitialPrecision);
    }

    return lInitialPrecision;
  }

  @Override
  public <T extends ConfigurableProgramAnalysis> T retrieveWrappedCpa(
      Class<T> pType) {
    if (pType.isAssignableFrom(getClass())) {
      return pType.cast(this);
    }

    if (pType.isAssignableFrom(mCachedCPA.getClass())) {
      return pType.cast(mCachedCPA);
    } else if (mCachedCPA instanceof WrapperCPA) {
      return ((WrapperCPA)mCachedCPA).retrieveWrappedCpa(pType);
    }

    return null;
  }

  @Override
  public ImmutableList<ConfigurableProgramAnalysis> getWrappedCPAs() {
    return ImmutableList.of(mCachedCPA);
  }
}
