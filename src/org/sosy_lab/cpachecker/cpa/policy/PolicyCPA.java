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
package org.sosy_lab.cpachecker.cpa.policy;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.defaults.*;
import org.sosy_lab.cpachecker.core.interfaces.*;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;

/**
 * Configurable-Program-Analysis implementation for policy iteration.
 */
public class PolicyCPA implements ConfigurableProgramAnalysis{

  private final ShutdownNotifier shutdownNotifier;

  /**
   * Formula handling.
   */
  private final FormulaManagerFactory formulaManagerFactory;
  private final FormulaManager realFormulaManager;
  private final FormulaManagerView formulaManager;

  /**
   * Converting code to formulas.
   */
  private final PathFormulaManager pathFormulaManager;
  private final ValueDeterminationFormulaManager valueDeterminationFormulaManager;

  private final AbstractDomain abstractDomain;
  private final TransferRelation transferRelation;
  private final LogManager logger;
  private final Configuration config;

  private final Solver solver;

  // Potential FEATURE: make configurable.
  private String mergeType = "JOIN";

  private MergeOperator mergeOperator;
  private StopOperator stopOperator;

  private PrecisionAdjustment precisionAdjustment;



  // NOTE: I *think* this magic method allows the CPA to ask for, like, whatever in the
  // constructor, and then the factory supplies the instances of all the required classes.
  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PolicyCPA.class);
  }

  private PolicyCPA(
        Configuration config,
        LogManager logger,
        ShutdownNotifier shutdownNotifier,
        CFA cfa // NOTE: I have CFA, therefore I can do any pre-processing I should desire.
        ) throws InvalidConfigurationException {
    config.inject(this);


    this.config = config;
    this.logger = logger;
    this.shutdownNotifier = shutdownNotifier;

    formulaManagerFactory = new FormulaManagerFactory(config, logger, shutdownNotifier);

    realFormulaManager = formulaManagerFactory.getFormulaManager();
    formulaManager = new FormulaManagerView(realFormulaManager, config, logger);

    solver = new Solver(formulaManager, formulaManagerFactory);

    pathFormulaManager = new PathFormulaManagerImpl(
        formulaManager, config, logger, shutdownNotifier, cfa);
    valueDeterminationFormulaManager = new ValueDeterminationFormulaManager(
        pathFormulaManager, formulaManager, config, logger, shutdownNotifier, cfa.getMachineModel()
    );

    abstractDomain = new PolicyAbstractDomain(valueDeterminationFormulaManager);


    transferRelation = new PolicyTransferRelation(
        config,
        formulaManager,
        formulaManagerFactory,
        pathFormulaManager);

    mergeOperator = new MergeJoinOperator(abstractDomain);
    stopOperator = new StopJoinOperator(abstractDomain);

    precisionAdjustment = StaticPrecisionAdjustment.getInstance();



    // NOTE: in [PredicateTransferRelation] for some reason they use 4 different formula managers:
    // predicateMeneger, pathFormulaManager, formulaManager and finally booleanFormulaManager.

    // Apparently all of those can be generated from [formulaManagerView]?..
  }

  @Override
  public AbstractState getInitialState(CFANode node) {
    // Abstract state for a _single_ node.
    // E.g. map from a variable map to a policy->bound.
    return PolicyAbstractState.withEmptyState(node);
  }


  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }


  @Override
  public Precision getInitialPrecision(CFANode node) {
    return SingletonPrecision.getInstance();
  }
}
