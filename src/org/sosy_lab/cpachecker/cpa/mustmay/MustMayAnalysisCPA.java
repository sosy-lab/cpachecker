/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.mustmay;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;

public class MustMayAnalysisCPA implements ConfigurableProgramAnalysis {

  MustMayAnalysisDomain mDomain;

  ConfigurableProgramAnalysis mMustCPA;
  ConfigurableProgramAnalysis mMayCPA;

  Precision mPrecision;

  StopOperator mStopOperator;

  MustMayAnalysisTransferRelation mTransferRelation;

  public MustMayAnalysisCPA(ConfigurableProgramAnalysis pMustCPA, ConfigurableProgramAnalysis pMayCPA) {
    assert(pMustCPA != null);
    assert(pMayCPA != null);

    mMustCPA = pMustCPA;
    mMayCPA = pMayCPA;

    AbstractDomain lMustDomain = mMustCPA.getAbstractDomain();
    AbstractDomain lMayDomain = mMayCPA.getAbstractDomain();

    mDomain = new MustMayAnalysisDomain(lMustDomain, lMayDomain);

    mStopOperator = new StopSepOperator(mDomain);

    mTransferRelation = new MustMayAnalysisTransferRelation(pMustCPA.getTransferRelation(), pMayCPA.getTransferRelation());
  }

  @Override
  public MustMayAnalysisDomain getAbstractDomain() {
    return mDomain;
  }

  @Override
  public MustMayAnalysisElement getInitialElement(
      CFANode pNode) {
    AbstractElement lInitialMustElement = mMustCPA.getInitialElement(pNode);
    AbstractElement lInitialMayElement = mMayCPA.getInitialElement(pNode);

    return new MustMayAnalysisElement(lInitialMustElement, lInitialMayElement);
  }

  @Override
  public MustMayAnalysisPrecision getInitialPrecision(CFANode pNode) {
    Precision lInitialMustPrecision = mMustCPA.getInitialPrecision(pNode);
    Precision lInitialMayPrecision = mMayCPA.getInitialPrecision(pNode);

    return new MustMayAnalysisPrecision(lInitialMustPrecision, lInitialMayPrecision);
  }

  @Override
  public MergeOperator getMergeOperator() {
    return MergeSepOperator.getInstance();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public StopOperator getStopOperator() {
    return mStopOperator;
  }

  @Override
  public MustMayAnalysisTransferRelation getTransferRelation() {
    return mTransferRelation;
  }

}
