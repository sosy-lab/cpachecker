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
package org.sosy_lab.cpachecker.cpa.assume;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;

/*
 * CPA that stores parameter of a prespecified external function.
 * Used for modeling assumptions. PredicateTransferRelation strengthens
 * its abstract state with respect to the given parameter.
 */
public class AssumeCPA implements ConfigurableProgramAnalysis {

  private AssumeDomain mDomain;
  private StopOperator mStopOperator;
  private AssumeTransferRelation mTransferRelation;

  public static AssumeCPA getCBMCAssume() {
    return new AssumeCPA("__CPROVER_assume");
  }

  public AssumeCPA(String pAssumeFunctionName) {
    mDomain = new AssumeDomain();
    mStopOperator = new StopSepOperator(mDomain);
    mTransferRelation = new AssumeTransferRelation(pAssumeFunctionName);
  }

  @Override
  public AssumeDomain getAbstractDomain() {
    return mDomain;
  }

  @Override
  public AssumeState getInitialState(CFANode pNode) {
    return UnconstrainedAssumeState.getInstance();
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return SingletonPrecision.getInstance();
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
  public AssumeTransferRelation getTransferRelation() {
    return mTransferRelation;
  }

}
