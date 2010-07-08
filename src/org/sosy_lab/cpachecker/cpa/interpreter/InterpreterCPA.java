/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.interpreter;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;

public class InterpreterCPA implements org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis {

  private static class ConcreteAnalysisCPAFactory extends AbstractCPAFactory {

    @Override
    public ConfigurableProgramAnalysis createInstance() {
      return new InterpreterCPA(getLogger());
    }
  }

  public static CPAFactory factory() {
    return new ConcreteAnalysisCPAFactory();
  }

  private InterpreterDomain mAbstractDomain;
  private MergeOperator mMergeOperator;
  private StopOperator mStopOperator;
  private InterpreterTransferRelation mTransferRelation;
  private PrecisionAdjustment mPrecisionAdjustment;

  public InterpreterCPA(LogManager logger) {

    this.mAbstractDomain = InterpreterDomain.getInstance();

    this.mTransferRelation = new InterpreterTransferRelation(this.mAbstractDomain, logger);

    this.mMergeOperator = MergeSepOperator.getInstance();
    this.mStopOperator = new StopSepOperator(this.mAbstractDomain.getPartialOrder());
    this.mPrecisionAdjustment = StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public InterpreterDomain getAbstractDomain()
  {
    return mAbstractDomain;
  }

  @Override
  public MergeOperator getMergeOperator()
  {
    return this.mMergeOperator;
  }

  @Override
  public StopOperator getStopOperator()
  {
    return mStopOperator;
  }

  @Override
  public InterpreterTransferRelation getTransferRelation()
  {
    return mTransferRelation;
  }

  @Override
  public InterpreterElement getInitialElement(CFAFunctionDefinitionNode node)
  {
    return new InterpreterElement();
  }

  @Override
  public SingletonPrecision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return SingletonPrecision.getInstance();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return mPrecisionAdjustment;
  }

}
