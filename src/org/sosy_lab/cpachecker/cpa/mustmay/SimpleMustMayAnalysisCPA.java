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
package org.sosy_lab.cpachecker.cpa.mustmay;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cpa.alwaystop.AlwaysTopCPA;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.cpa.concrete.ConcreteAnalysisCPA;

public class SimpleMustMayAnalysisCPA implements ConfigurableProgramAnalysis {

  private static class SimpleMustMayAnalysisCPAFactory extends AbstractCPAFactory {
    
    @Override
    public ConfigurableProgramAnalysis createInstance() {
      return new SimpleMustMayAnalysisCPA();
    }
  }
  
  public static CPAFactory factory() {
    return new SimpleMustMayAnalysisCPAFactory();
  }
  
  private final MustMayAnalysisCPA mMustMayAnalysisCPA;
  
  public SimpleMustMayAnalysisCPA() {
    AlwaysTopCPA lMayCPA = new AlwaysTopCPA();
    ConcreteAnalysisCPA lMustCPA = new ConcreteAnalysisCPA();
    
    mMustMayAnalysisCPA = new MustMayAnalysisCPA(lMustCPA, lMayCPA);
  }
  
  @Override
  public MustMayAnalysisElement getInitialElement(
      CFAFunctionDefinitionNode pNode) {
    return mMustMayAnalysisCPA.getInitialElement(pNode);
  }

  @Override
  public MustMayAnalysisDomain getAbstractDomain() {
    return mMustMayAnalysisCPA.getAbstractDomain();
  }

  @Override
  public MustMayAnalysisPrecision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return mMustMayAnalysisCPA.getInitialPrecision(pNode);
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mMustMayAnalysisCPA.getMergeOperator();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return mMustMayAnalysisCPA.getPrecisionAdjustment();
  }

  @Override
  public StopOperator getStopOperator() {
    return mMustMayAnalysisCPA.getStopOperator();
  }

  @Override
  public MustMayAnalysisTransferRelation getTransferRelation() {
    return mMustMayAnalysisCPA.getTransferRelation();
  }

}
