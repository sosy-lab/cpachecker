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
/**
 *
 */
package cpa.pointsto;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.c.FunctionDefinitionNode;
import cpa.common.defaults.AbstractCPAFactory;
import cpa.common.defaults.MergeJoinOperator;
import cpa.common.defaults.StaticPrecisionAdjustment;
import cpa.common.defaults.StopSepOperator;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.CPAFactory;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class PointsToCPA implements ConfigurableProgramAnalysis {

  private static class PointsToCPAFactory extends AbstractCPAFactory {
    
    @Override
    public ConfigurableProgramAnalysis createInstance() {
      return new PointsToCPA();
    }
  }
  
  public static CPAFactory factory() {
    return new PointsToCPAFactory();
  }
  
  private final PointsToDomain abstractDomain;
  private final TransferRelation transferRelation;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final PrecisionAdjustment precisionAdjustment;

  private PointsToCPA() {
    abstractDomain = new PointsToDomain();
    transferRelation = new PointsToTransferRelation();
    mergeOperator = new MergeJoinOperator(abstractDomain.getJoinOperator());
    stopOperator = new StopSepOperator(abstractDomain.getPartialOrder());
    precisionAdjustment = StaticPrecisionAdjustment.getInstance();
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProblemAnalysis#getAbstractDomain()
   */
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProblemAnalysis#getTransferRelation()
   */
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProblemAnalysis#getMergeOperator()
   */
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProblemAnalysis#getStopOperator()
   */
  public StopOperator getStopOperator() {
    return stopOperator;
  }


  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProblemAnalysis#getInitialElement(cfa.objectmodel.CFAFunctionDefinitionNode)
   */
  public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
    PointsToElement initial = new PointsToElement();

    if (node instanceof FunctionDefinitionNode) {
      List<IASTParameterDeclaration> parameters = ((FunctionDefinitionNode)node).getFunctionParameters ();
      for (IASTParameterDeclaration parameter : parameters) {
        if (0 != parameter.getDeclarator().getPointerOperators().length) {
          if (parameter.getDeclarator().getNestedDeclarator() != null) {
            initial.addVariable(parameter.getDeclarator().getNestedDeclarator());
          } else {
            initial.addVariable(parameter.getDeclarator());
          }
        }
      }
    }

    return initial;
  }

  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return null;
  }
}
