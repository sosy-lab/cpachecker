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
package org.sosy_lab.cpachecker.cpa.cfapath;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopNeverOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

public class CFAPathCPA implements ConfigurableProgramAnalysis {

  private final CFAPathDomain mDomain;
  private final CFAPathTransferRelation mTransferRelation;
  private final PrecisionAdjustment mPrecisionAdjustment;
  private final Precision mPrecision;
  private final CFAPathStandardState mInitialState;
  private final StopOperator mStopOperator;
  private final MergeOperator mMergeOperator;

  private static final CFAPathCPA sInstance = new CFAPathCPA();

  public static CFAPathCPA getInstance() {
    return sInstance;
  }

  public CFAPathCPA() {
    mDomain = CFAPathDomain.getInstance();
    mTransferRelation = new CFAPathTransferRelation();
    mPrecisionAdjustment = StaticPrecisionAdjustment.getInstance();
    mPrecision = SingletonPrecision.getInstance();
    mInitialState = CFAPathStandardState.getEmptyPath();
    mStopOperator = StopNeverOperator.getInstance();
    mMergeOperator = MergeSepOperator.getInstance();
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return mDomain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return mTransferRelation;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mMergeOperator;
  }

  @Override
  public StopOperator getStopOperator() {
    return mStopOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return mPrecisionAdjustment;
  }

  @Override
  public CFAPathState getInitialState(CFANode pNode) {
    return mInitialState;
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return mPrecision;
  }

}
