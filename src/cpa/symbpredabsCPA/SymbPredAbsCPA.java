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
package cpa.symbpredabsCPA;

import java.util.Collection;

import symbpredabstraction.PathFormula;
import symbpredabstraction.SSAMap;
import symbpredabstraction.bdd.BDDAbstractFormulaManager;
import symbpredabstraction.interfaces.AbstractFormula;
import symbpredabstraction.interfaces.AbstractFormulaManager;
import symbpredabstraction.interfaces.InterpolatingTheoremProver;
import symbpredabstraction.interfaces.SymbolicFormulaManager;
import symbpredabstraction.interfaces.TheoremProver;
import symbpredabstraction.mathsat.MathsatInterpolatingProver;
import symbpredabstraction.mathsat.MathsatSymbolicFormulaManager;
import symbpredabstraction.mathsat.MathsatTheoremProver;
import symbpredabstraction.mathsat.SimplifyTheoremProver;
import symbpredabstraction.mathsat.YicesTheoremProver;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.CFANode;

import com.google.common.collect.ImmutableList;

import cpa.common.CPAchecker;
import cpa.common.defaults.AbstractCPAFactory;
import cpa.common.defaults.StaticPrecisionAdjustment;
import cpa.common.defaults.StopSepOperator;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.CPAFactory;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.Statistics;
import cpa.common.interfaces.StatisticsProvider;
import cpa.common.interfaces.StopOperator;
import exceptions.CPAException;
import exceptions.InvalidConfigurationException;

/**
 * CPA that defines symbolic predicate abstraction.
 * @author Erkan
 *
 */
public class SymbPredAbsCPA implements ConfigurableProgramAnalysis, StatisticsProvider {

  private static class SymbPredAbsCPAFactory extends AbstractCPAFactory {
    @Override
    public ConfigurableProgramAnalysis createInstance() throws CPAException {
      return new SymbPredAbsCPA();
    }
  }
  
  public static CPAFactory factory() {
    return new SymbPredAbsCPAFactory();
  }
  
  private final SymbPredAbsAbstractDomain domain;
  private final SymbPredAbsTransferRelation transfer;
  private final SymbPredAbsMergeOperator merge;
  private final StopOperator stop;
  private final SymbPredAbsPrecision initialPrecision;
  private final AbstractFormulaManager abstractFormulaManager;
  private final MathsatSymbolicFormulaManager symbolicFormulaManager;
  private final SymbPredAbsFormulaManager formulaManager;
  private final SymbPredAbsCPAStatistics stats;

  private SymbPredAbsCPA() throws CPAException {
    abstractFormulaManager = new BDDAbstractFormulaManager();
    symbolicFormulaManager = new MathsatSymbolicFormulaManager();
    TheoremProver thmProver;
    String whichProver = CPAchecker.config.getProperty(
        "cpas.symbpredabs.explicit.abstraction.solver", "mathsat");
    if (whichProver.equals("mathsat")) {
      thmProver = new MathsatTheoremProver(symbolicFormulaManager, false);
    } else if (whichProver.equals("simplify")) {
      thmProver = new SimplifyTheoremProver(symbolicFormulaManager);
    } else if (whichProver.equals("yices")) {
      thmProver = new YicesTheoremProver(symbolicFormulaManager);
    } else {
      throw new InvalidConfigurationException("Unknown theorem prover " + whichProver
          + ", check option cpas.symbpredabs.explicit.abstraction.solver");
    }
    InterpolatingTheoremProver<Integer> itpProver =
      new MathsatInterpolatingProver(symbolicFormulaManager, false);
    formulaManager = new MathsatSymbPredAbsFormulaManager<Integer>(abstractFormulaManager, symbolicFormulaManager, thmProver, itpProver);
    domain = new SymbPredAbsAbstractDomain(abstractFormulaManager);
    transfer = new SymbPredAbsTransferRelation(this);
    merge = new SymbPredAbsMergeOperator(this);
    stop = new StopSepOperator(domain.getPartialOrder());
    initialPrecision = new SymbPredAbsPrecision();

    stats = new SymbPredAbsCPAStatistics(this);
  }

/* TODO do we still need this?  
  private PredicateMap createPredicateMap() {
    Collection<Predicate> preds = null;

    String path = CPAMain.cpaConfig.getProperty("predicates.path");
    if (path != null) {
      File f = new File(path, "predicates.msat");
      try {
        InputStream in = new FileInputStream(f);
        
        MathsatPredicateParser p = new MathsatPredicateParser(symbolicFormulaManager, formulaManager);
        preds = p.parsePredicates(in);
      } catch (IOException e) {
        CPAMain.logManager.log(Level.WARNING, "Cannot read predicates from", f.getPath());
      }
    }
    
    if (CPAMain.cpaConfig.getBooleanValue("cpas.symbpredabs.abstraction.norefinement")) {
      // for testing purposes, it's nice to be able to use a given set of
      // predicates and disable refinement
      return new FixedPredicateMap(preds);
    } else {
      return new UpdateablePredicateMap(preds);
    }
  }
*/

  @Override
  public SymbPredAbsAbstractDomain getAbstractDomain() {
    return domain;
  }

  public SymbPredAbsTransferRelation getTransferRelation() {
    return transfer;
  }

  public SymbPredAbsMergeOperator getMergeOperator() {
    return merge;
  }

  @Override
  public StopOperator getStopOperator() {
    return stop;
  }

  public AbstractFormulaManager getAbstractFormulaManager() {
    return abstractFormulaManager;
  }
  
  public SymbPredAbsFormulaManager getFormulaManager() {
    return formulaManager;
  }

  public SymbolicFormulaManager getSymbolicFormulaManager() {
    return symbolicFormulaManager;
  }

  public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
    ImmutableList<CFANode> oldAbstractionPath = ImmutableList.of();
    PathFormula pf = new PathFormula(symbolicFormulaManager.makeTrue(), new SSAMap());
    AbstractFormula initAbstraction = abstractFormulaManager.makeTrue();

    return new SymbPredAbsAbstractElement(node,
        pf, pf, initAbstraction, oldAbstractionPath);
  }

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
