/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.arraySegmentation.usageAnalysis;

import java.util.List;
import java.util.function.Predicate;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.ArraySegmentationState;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.CGenericInterval;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.CPropertySpec;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.ExtendedLocationArrayContentCPA;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.UnreachableSegmentation;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.util.EnhancedCExpressionSimplificationVisitor;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options(prefix = "cpa.arrayContentCPA")
public class ExtendedCLUAnalysisCPA extends ExtendedLocationArrayContentCPA<VariableUsageState> {

  protected ExtendedCLUAnalysisCPA(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier shutdownNotifier,
      CFA cfa)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, shutdownNotifier, cfa);
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ExtendedCLUAnalysisCPA.class);
  }

  @Override
  protected AbstractCPA constructInnerCPA(
      Configuration pConfig,
      LogManager pLogger,
      CFA pCfa,
      String pVarnameArray,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    return new UsageAnalysisCPA(pConfig, pLogger, pCfa, pVarnameArray, pShutdownNotifier);
  }

  @Override
  protected String getName() {
    return "UsageAnalysisCPA";
  }

  @Override
  protected VariableUsageState getEmptyElement() {
    return VariableUsageState.getEmptyElement();
  }

  @Override
  protected VariableUsageState getInitialInnerState(CFANode pNode, StateSpacePartition pPartition) {
    return new VariableUsageState(VariableUsageType.NOT_USED);
  }

  @Override
  protected Predicate<ArraySegmentationState<VariableUsageState>> getPredicate() {
    EnhancedCExpressionSimplificationVisitor visitor =
        new EnhancedCExpressionSimplificationVisitor(
            cfa.getMachineModel(),
            new LogManagerWithoutDuplicates(logger));
    CBinaryExpressionBuilder builder = new CBinaryExpressionBuilder(cfa.getMachineModel(), logger);
    return new Predicate<ArraySegmentationState<VariableUsageState>>() {
      @Override
      public boolean test(ArraySegmentationState<VariableUsageState> pT) {
        if (pT instanceof UnreachableSegmentation) {
          return false;
        }

        CPropertySpec<VariableUsageState> properties = null;
        try {
          properties =
              pT.getSegmentsForProperty(
                  new VariableUsageState(VariableUsageType.USED),
                  visitor,
                  builder);
        } catch (CPAException e) {
          throw new IllegalArgumentException(e);
        }
        List<CGenericInterval> overApproxP = properties.getOverApproxIntervals();
        boolean isCorrect =
            pT.isEmptyArray()
                || (overApproxP.size() == 1
                    && overApproxP.get(0).getLow().equals(CIntegerLiteralExpression.ZERO)
                    && overApproxP.get(0).getHigh().equals(pT.getSizeVar()));
        return !isCorrect;
      }
    };
  }

}
