/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.explicit;

import java.util.HashMap;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.StopNeverOperator;
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
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.ExtendedFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFactory;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatTheoremProver;

@Options(prefix="cpa.explicit")
public class ExplicitCPA implements ConfigurableProgramAnalysis {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ExplicitCPA.class);
  }

  @Option(name="merge", toUppercase=true, values={"SEP", "JOIN"},
      description="which merge operator to use for ExplicitCPA")
  private String mergeType = "SEP";

  @Option(name="stop", toUppercase=true, values={"SEP", "JOIN", "NEVER"},
      description="which stop operator to use for ExplicitCPA")
  private String stopType = "SEP";

  @Option(name="variableBlacklist",
      description="blacklist regex for variables that won't be tracked by ExplicitCPA")
  private String variableBlacklist = "";

  private ExplicitPrecision precision;

  private AbstractDomain abstractDomain;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private TransferRelation transferRelation;
  private PrecisionAdjustment precisionAdjustment;

  private final Configuration config;
  private final LogManager logger;

  private final RegionManager regionManager;
  private final ExtendedFormulaManager formulaManager;
  private final PathFormulaManager pathFormulaManager;
  private final TheoremProver theoremProver;

  private final PredicateAbstractionManager predicateManager;

  private ExplicitCPA(Configuration config, LogManager logger) throws InvalidConfigurationException
  {
    this.config = config;
    this.logger = logger;

    config.inject(this);

    if(this.useCegar())
      precision = new ExplicitPrecision(variableBlacklist, new HashMap<CFANode, Set<String>>());
    else
      precision = new ExplicitPrecision(variableBlacklist, null);

    ExplicitDomain explicitDomain = new ExplicitDomain ();
    MergeOperator explicitMergeOp = null;
    if (mergeType.equals("SEP")){
      explicitMergeOp = MergeSepOperator.getInstance();
    } else if (mergeType.equals("JOIN")){
      explicitMergeOp = new MergeJoinOperator(explicitDomain);
    }

    StopOperator explicitStopOp = null;

    if(stopType.equals("SEP")){
      explicitStopOp = new StopSepOperator(explicitDomain);
    }
    else if(stopType.equals("JOIN")){
      explicitStopOp = new StopJoinOperator(explicitDomain);
    }
    else if(stopType.equals("NEVER")){
      explicitStopOp = new StopNeverOperator();
    }

    TransferRelation explicitRelation = new ExplicitTransferRelation(config);

    this.abstractDomain = explicitDomain;
    this.mergeOperator = explicitMergeOp;
    this.stopOperator = explicitStopOp;
    this.transferRelation = explicitRelation;
    this.precisionAdjustment = StaticPrecisionAdjustment.getInstance();

    MathsatFormulaManager mathsatFormulaManager = MathsatFactory.createFormulaManager(config, logger);

    regionManager       = BDDRegionManager.getInstance();
    formulaManager      = new ExtendedFormulaManager(mathsatFormulaManager, config, logger);
    pathFormulaManager  = new PathFormulaManagerImpl(formulaManager, config, logger);
    theoremProver       = new MathsatTheoremProver(mathsatFormulaManager);
    predicateManager    = new PredicateAbstractionManager(regionManager, formulaManager, theoremProver, config, logger);
  }

  @Override
  public AbstractDomain getAbstractDomain ()
  {
    return abstractDomain;
  }

  @Override
  public MergeOperator getMergeOperator ()
  {
    return mergeOperator;
  }

  @Override
  public StopOperator getStopOperator ()
  {
    return stopOperator;
  }

  @Override
  public TransferRelation getTransferRelation ()
  {
    return transferRelation;
  }

  @Override
  public AbstractElement getInitialElement (CFANode node)
  {
    return new ExplicitElement();
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    //return SingletonPrecision.getInstance();
    return precision;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  protected PredicateAbstractionManager getPredicateManager() {
    return predicateManager;
  }

  protected Configuration getConfiguration() {
    return config;
  }

  protected LogManager getLogger() {
    return logger;
  }

  protected ExtendedFormulaManager getFormulaManager()
  {
    return formulaManager;
  }

  protected PathFormulaManager getPathFormulaManager()
  {
    return pathFormulaManager;
  }

  protected TheoremProver getTheoremProver()
  {
    return theoremProver;
  }

  private boolean useCegar()
  {
    return this.config.getProperty("analysis.useRefinement") != null
      && this.config.getProperty("cegar.refiner").equals("cpa.explicit.ExplicitRefiner");
  }

}
