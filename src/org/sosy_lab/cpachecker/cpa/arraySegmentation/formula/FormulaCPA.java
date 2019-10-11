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
package org.sosy_lab.cpachecker.cpa.arraySegmentation.formula;

import java.util.Map;
import java.util.NavigableSet;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
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
import org.sosy_lab.cpachecker.cpa.arraySegmentation.ExtendedLocationArrayContentCPA;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.ControlDependenceComputer;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.DominanceFrontier;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.Dominators;

@Options(prefix="cpa.ifcsecurity")
public class FormulaCPA implements ConfigurableProgramAnalysis {

  @SuppressWarnings("unused")
  private LogManager logger;
  private AbstractDomain domain;
  private FormulaRelation transfer;

  @Option(secure=true, name="merge", toUppercase=true, values={"SEP", "JOIN"},
      description="which merge operator to use for DependencyTrackerCPA")
  private String mergeType = "JOIN";

  @Option(secure=true, name="stop", toUppercase=true, values={"SEP", "JOIN"},
      description="which stop operator to use for DependencyTrackerCPA")
  private String stopType = "SEP";

  private StopOperator stop;
  private MergeOperator merge;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(FormulaCPA.class);
  }

  private FormulaCPA(LogManager pLogger, Configuration pConfig, ShutdownNotifier pShutdownNotifier, CFA pCfa) throws InvalidConfigurationException {
    pConfig.inject(this);
    this.logger = pLogger;

    domain = DelegateAbstractDomain.<FormulaState>getInstance();


    Dominators postdom = new Dominators(pCfa, 1);
    postdom.execute();
    Map<CFANode, CFANode> postdominators = postdom.getDom();
    pLogger.log(ExtendedLocationArrayContentCPA.GENERAL_LOG_LEVEL, "Postdominators");
    pLogger.log(ExtendedLocationArrayContentCPA.GENERAL_LOG_LEVEL, postdominators);

    DominanceFrontier domfron = new DominanceFrontier(pCfa, postdominators);
    domfron.execute();
    Map<CFANode, NavigableSet<CFANode>> df = domfron.getDominanceFrontier();
    pLogger.log(ExtendedLocationArrayContentCPA.GENERAL_LOG_LEVEL, "Dominance Frontier");
    pLogger.log(ExtendedLocationArrayContentCPA.GENERAL_LOG_LEVEL, df);

    ControlDependenceComputer cdcom = new ControlDependenceComputer(pCfa, df);
    cdcom.execute();
    Map<CFANode, NavigableSet<CFANode>> cd = cdcom.getControlDependency();
    pLogger.log(ExtendedLocationArrayContentCPA.GENERAL_LOG_LEVEL, "Control Dependency");
    pLogger.log(ExtendedLocationArrayContentCPA.GENERAL_LOG_LEVEL, cd);

    Map<CFANode, NavigableSet<CFANode>> recd = cdcom.getReversedControlDependency();
    pLogger.log(ExtendedLocationArrayContentCPA.GENERAL_LOG_LEVEL, "Reversed Control Dependency");
    pLogger.log(ExtendedLocationArrayContentCPA.GENERAL_LOG_LEVEL, recd);
    transfer = new FormulaRelation(pConfig, pLogger, pShutdownNotifier, pCfa);
     if (stopType.equals("SEP")) {
      stop = new StopSepOperator(domain);
    } else if (mergeType.equals("JOIN")) {
      stop = new StopJoinOperator(domain);
    }
    if (mergeType.equals("SEP")) {
      merge = MergeSepOperator.getInstance();
    } else if (mergeType.equals("JOIN")) {
      merge = new MergeJoinOperator(domain);
    }
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return domain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transfer;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return merge;
  }

  @Override
  public StopOperator getStopOperator() {
    return stop;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    //TODO ADD OWN ONE
    return StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
        return  transfer.makeInitial();
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return SingletonPrecision.getInstance();
  }

}
