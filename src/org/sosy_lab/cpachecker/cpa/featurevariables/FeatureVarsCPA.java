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
package org.sosy_lab.cpachecker.cpa.featurevariables;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.util.predicates.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDRegionManager;

@Options(prefix="cpa.featurevars")
public class FeatureVarsCPA implements ConfigurableProgramAnalysis {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(FeatureVarsCPA.class);
  }

  @Option(name="variableWhitelist",
          description="whitelist regex for variables that will be tracked by FeatureVarsCPA")
  private String variableWhitelist = "";

  private final FeatureVarsState initialState;
  private final FeatureVarsPrecision initialPrecision;

  private final AbstractDomain abstractDomain;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final TransferRelation transferRelation;
  private final PrecisionAdjustment precisionAdjustment;

  private FeatureVarsCPA(Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this);

    NamedRegionManager manager = new NamedRegionManager(BDDRegionManager.getInstance(config));

    initialState = new FeatureVarsState(manager.makeTrue(), manager);
    initialPrecision = new FeatureVarsPrecision(variableWhitelist);

    abstractDomain = new FeatureVarsDomain(manager);
    mergeOperator = new MergeJoinOperator(abstractDomain);
    stopOperator =  new StopSepOperator(abstractDomain);
    transferRelation = new FeatureVarsTransferRelation(manager);
    precisionAdjustment = StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
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
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public AbstractState getInitialState(CFANode node) {
    return initialState;
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return initialPrecision;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

}
