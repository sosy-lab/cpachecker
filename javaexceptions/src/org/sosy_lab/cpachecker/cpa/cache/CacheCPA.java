// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.cache;

import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.Map;
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
    return AutomaticCPAFactory.forType(CacheCPA.class);
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
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    AbstractState lInitialState = mInitialStatesCache.get(pNode);

    if (lInitialState == null) {
      lInitialState = mCachedCPA.getInitialState(pNode, pPartition);
      mInitialStatesCache.put(pNode, lInitialState);
    }

    return lInitialState;
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    Precision lInitialPrecision = mInitialPrecisionsCache.get(pNode);

    if (lInitialPrecision == null) {
      lInitialPrecision = mCachedCPA.getInitialPrecision(pNode, pPartition);
      mInitialPrecisionsCache.put(pNode, lInitialPrecision);
    }

    return lInitialPrecision;
  }

  @Override
  public <T extends ConfigurableProgramAnalysis> T retrieveWrappedCpa(Class<T> pType) {
    if (pType.isAssignableFrom(getClass())) {
      return pType.cast(this);
    }

    if (pType.isAssignableFrom(mCachedCPA.getClass())) {
      return pType.cast(mCachedCPA);
    } else if (mCachedCPA instanceof WrapperCPA) {
      return ((WrapperCPA) mCachedCPA).retrieveWrappedCpa(pType);
    }

    return null;
  }

  @Override
  public ImmutableList<ConfigurableProgramAnalysis> getWrappedCPAs() {
    return ImmutableList.of(mCachedCPA);
  }
}
