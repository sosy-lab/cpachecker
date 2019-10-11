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

import com.google.common.base.Throwables;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.ArraySegmentationState;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.CGenericInterval;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.CPropertySpec;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.UnreachableSegmentation;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.usageAnalysis.UsageAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.usageAnalysis.UsageAnalysisTransferRelation;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.usageAnalysis.VariableUsageState;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.usageAnalysis.VariableUsageType;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.util.ArraySegmentationCPAHelper;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.util.EnhancedCExpressionSimplificationVisitor;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options(prefix = UsageAnalysisCPA.NAME_OF_ANALYSIS)
public class UsageAnalysisCPA extends AbstractCPA {

  @Option(
    secure = true,
    name = "merge",
    toUppercase = true,
    values = {"JOIN"},
    description = "which merge operator to use for UsageOfArrayElemensCPA")
  private String mergeType = "JOIN";

  @Option(
    secure = true,
    name = "stop",
    toUppercase = true,
    values = {"SEP"},
    description = "which stop operator to use for UsageOfArrayElemensCPA")
  private String stopType = "SEP";

  @Option(
    name = "arrayName",
    toUppercase = false,
    description = "The array that needs to be analyzed")
  private String varnameArray = "";

  private final CFA cfa;
  public static final String NAME_OF_ANALYSIS = "cpa.usageCPA";
  private final LogManager logger;
  private ArraySegmentationCPAHelper<VariableUsageState> helper;

  /**
   * This method acts as the constructor of the analysis CPA.
   *
   * @param config the configuration of the CPAinterval analysis CPA.
   */
  public UsageAnalysisCPA(
      Configuration config,
      LogManager pLogger,
      ShutdownNotifier shutdownNotifier,
      CFA cfa)
      throws InvalidConfigurationException {
    super(
        "join",
        "sep",
        DelegateAbstractDomain.<ArraySegmentationState<VariableUsageState>>getInstance(),
        null);
    config.inject(this, UsageAnalysisCPA.class);
    this.logger = pLogger;
    this.cfa = cfa;
    helper = new ArraySegmentationCPAHelper<>(cfa, logger, varnameArray, shutdownNotifier, config);
  }

  /**
   * This method acts as the constructor of the interval analysis CPA.
   *
   * @param config the configuration of the CPAinterval analysis CPA.
   * @param pLogger the logger for the analysis
   * @param cfa the CFA of the program
   * @param pVarnameArray the name of the array, that should be analyzed
   * @param shutdownNotifier of the analysis
   */
  public UsageAnalysisCPA(
      Configuration config,
      LogManager pLogger,
      CFA cfa,
      String pVarnameArray,
      ShutdownNotifier shutdownNotifier)
      throws InvalidConfigurationException {
    super(
        "join",
        "sep",
        DelegateAbstractDomain.<ArraySegmentationState<VariableUsageState>>getInstance(),
        null);
    config.inject(this, UsageAnalysisCPA.class);
    this.logger = pLogger;
    this.cfa = cfa;
    helper = new ArraySegmentationCPAHelper<>(cfa, logger, pVarnameArray, shutdownNotifier, config);
  }

  @Override
  public MergeOperator getMergeOperator() {
    return buildMergeOperator(mergeType);
  }

  @Override
  public StopOperator getStopOperator() {
    return buildStopOperator(stopType);
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(UsageAnalysisCPA.class);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new UsageAnalysisTransferRelation(
        new LogManagerWithoutDuplicates(logger),
        this.cfa.getMachineModel());

  }

  @Override
  public ArraySegmentationState<VariableUsageState>
      getInitialState(CFANode pNode, StateSpacePartition pPartition) throws InterruptedException {

    EnhancedCExpressionSimplificationVisitor visitor =
        new EnhancedCExpressionSimplificationVisitor(
            cfa.getMachineModel(),
            new LogManagerWithoutDuplicates(logger));
    CBinaryExpressionBuilder builder = new CBinaryExpressionBuilder(cfa.getMachineModel(), logger);
    Predicate<ArraySegmentationState<VariableUsageState>> predicate =
        new Predicate<ArraySegmentationState<VariableUsageState>>() {
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
              logger.log(
                  Level.CONFIG,
                  "Cannot check the state due to an interal error "
                      + Throwables.getStackTraceAsString(e));
              return false;
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

    return helper.computeInitaleState(
        new VariableUsageState(VariableUsageType.NOT_USED),
        predicate,
        VariableUsageState.getEmptyElement(),
        "UsageAnalysisCPA",
        pNode);
  }

}
