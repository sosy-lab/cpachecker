/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package org.sosy_lab.cpachecker.cpa.defuse;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.common.configuration.Configuration;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

public class DefUseCPA implements ConfigurableProgramAnalysis{

  private static class DefUseCPAFactory extends AbstractCPAFactory {
    
    @Override
    public ConfigurableProgramAnalysis createInstance() {
      Configuration config = getConfiguration();
      String mergeType = config.getProperty("cpas.defuse.merge");
      String stopType = config.getProperty("cpas.defuse.stop");
      return new DefUseCPA(mergeType, stopType);
    }
  }
  
  public static CPAFactory factory() {
    return new DefUseCPAFactory();
  }
  
  private AbstractDomain abstractDomain;
  private TransferRelation transferRelation;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private PrecisionAdjustment precisionAdjustment;

  private DefUseCPA (String mergeType, String stopType) {
    DefUseDomain defUseDomain = new DefUseDomain ();
    this.abstractDomain = defUseDomain;

    this.transferRelation = new DefUseTransferRelation ();

    this.mergeOperator = null;
    if(mergeType.equals("sep")){
      this.mergeOperator = MergeSepOperator.getInstance();
    } else if(mergeType.equals("join")){
      this.mergeOperator = new DefUseMergeJoin ();
    }

    this.stopOperator = null;
    if(stopType.equals("sep")){
      this.stopOperator = new StopSepOperator(defUseDomain.getPartialOrder());
    } else if(stopType.equals("join")){
      this.stopOperator = new DefUseStopJoin ();
    }

    this.precisionAdjustment = StaticPrecisionAdjustment.getInstance();
  }

  public AbstractDomain getAbstractDomain ()
  {
    return abstractDomain;
  }

  public TransferRelation getTransferRelation ()
  {
    return transferRelation;
  }

  public MergeOperator getMergeOperator ()
  {
    return mergeOperator;
  }

  public StopOperator getStopOperator ()
  {
    return stopOperator;
  }

  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }


  public AbstractElement getInitialElement (CFAFunctionDefinitionNode node)
  {
    List<DefUseDefinition> defUseDefinitions = null;
    if (node instanceof FunctionDefinitionNode)
    {
      List<String> parameterNames = ((FunctionDefinitionNode)node).getFunctionParameterNames ();
      defUseDefinitions = new ArrayList<DefUseDefinition> ();

      for (String parameterName : parameterNames)
      {
        DefUseDefinition newDef = new DefUseDefinition (parameterName, null);
        defUseDefinitions.add (newDef);
      }
    }

    return new DefUseElement (defUseDefinitions);
  }

  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return null;
  }
}
