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
package org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.PathFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.bdd.BDDAbstractFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractFormulaManager;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.interfaces.InterpolatingTheoremProver;
import org.sosy_lab.cpachecker.util.symbpredabstraction.Predicate;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.interfaces.TheoremProver;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.mathsat.MathsatInterpolatingProver;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.mathsat.MathsatTheoremProver;

/**
 * CPA that defines symbolic predicate abstraction.
 * @author Erkan
 *
 */
@Options(prefix="cpas.symbpredabs")
public class SymbPredAbsCPA implements ConfigurableProgramAnalysis, StatisticsProvider {

  private static class SymbPredAbsCPAFactory extends AbstractCPAFactory {
    @Override
    public ConfigurableProgramAnalysis createInstance() throws InvalidConfigurationException {
      return new SymbPredAbsCPA(getConfiguration(), getLogger());
    }
  }

  public static CPAFactory factory() {
    return new SymbPredAbsCPAFactory();
  }

  @Option(name="explicit.abstraction.solver", toUppercase=true, values={"MATHSAT", "YICES"})
  private String whichProver = "MATHSAT";

  @Option(name="interpolatingProver", toUppercase=true, values={"MATHSAT", "CSISAT"})
  private String whichItpProver = "MATHSAT";

  @Option
  private boolean symbolicCoverageCheck = false; 

  private final Configuration config;
  private final LogManager logger;

  private final SymbPredAbsAbstractDomain domain;
  private final SymbPredAbsTransferRelation transfer;
  private final SymbPredAbsMergeOperator merge;
  private final StopOperator stop;
  private SymbPredAbsPrecision initialPrecision;
  private final AbstractFormulaManager abstractFormulaManager;
  private final SymbPredAbsFormulaManagerImpl<?, ?> formulaManager;
  private final SymbPredAbsCPAStatistics stats;

  private final AbstractionElement.Factory mAbstractionElementFactory; 
  
  private SymbPredAbsCPA(Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this);

    this.config = config;
    this.logger = logger;
    

    mAbstractionElementFactory = new AbstractionElement.Factory();
    

    abstractFormulaManager = BDDAbstractFormulaManager.getInstance();
    MathsatSymbolicFormulaManager symbolicFormulaManager = new MathsatSymbolicFormulaManager(config, logger);
    
    TheoremProver thmProver;
    if (whichProver.equals("MATHSAT")) {
      thmProver = new MathsatTheoremProver(symbolicFormulaManager);
    } else {
      throw new InternalError("Update list of allowed solvers!");
    }

    InterpolatingTheoremProver<Integer> itpProver;
    InterpolatingTheoremProver<Integer> alternativeItpProver = null;
    if (whichItpProver.equals("MATHSAT")) {
      itpProver = new MathsatInterpolatingProver(symbolicFormulaManager, false);
    } else {
      throw new InternalError("Update list of allowed solvers!");
    }
    formulaManager = new SymbPredAbsFormulaManagerImpl<Integer, Integer>(abstractFormulaManager, symbolicFormulaManager, thmProver, itpProver, alternativeItpProver, config, logger);
    domain = new SymbPredAbsAbstractDomain(abstractFormulaManager, formulaManager, symbolicCoverageCheck);
    transfer = new SymbPredAbsTransferRelation(this);
    merge = new SymbPredAbsMergeOperator(this);
    stop = new StopSepOperator(domain);
    
    Set<Predicate> predicates = null;
    initialPrecision = new SymbPredAbsPrecision(predicates);

    stats = new SymbPredAbsCPAStatistics(this);
  }
  
  public AbstractionElement.Factory getAbstractionElementFactory() {
    return mAbstractionElementFactory;
  }

  @Override
  public SymbPredAbsAbstractDomain getAbstractDomain() {
    return domain;
  }

  @Override
  public SymbPredAbsTransferRelation getTransferRelation() {
    return transfer;
  }

  @Override
  public SymbPredAbsMergeOperator getMergeOperator() {
    return merge;
  }

  @Override
  public StopOperator getStopOperator() {
    return stop;
  }

  protected AbstractFormulaManager getAbstractFormulaManager() {
    return abstractFormulaManager;
  }

  protected SymbPredAbsFormulaManagerImpl<?, ?> getFormulaManager() {
    return formulaManager;
  }

  protected Configuration getConfiguration() {
    return config;
  }

  protected LogManager getLogger() {
    return logger;
  }

  private Map<CFANode, AbstractionElement> mInitialElementsCache = 
    new HashMap<CFANode, AbstractionElement>();
  
  @Override
  public AbstractionElement getInitialElement(CFAFunctionDefinitionNode node) {
    AbstractionElement lInitialElement = mInitialElementsCache.get(node);
    
    if (lInitialElement == null) {
      PathFormula pf = formulaManager.makeEmptyPathFormula();
      AbstractFormula initAbstraction = abstractFormulaManager.makeTrue();
      //lInitialElement = new AbstractionElement(node, initAbstraction, pf);
      lInitialElement = mAbstractionElementFactory.createInitialElement(initAbstraction, pf);
      
      mInitialElementsCache.put(node, lInitialElement);
    }
    
    return lInitialElement;
  }

  public void updateInitialPrecision(SymbPredAbsPrecision pPrecision) {
    initialPrecision = initialPrecision.update(pPrecision);
  }
  
  @Override
  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return initialPrecision;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }
}
