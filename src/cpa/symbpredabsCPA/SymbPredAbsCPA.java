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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
import cmdline.CPAMain;
import cpa.common.defaults.StaticPrecisionAdjustment;
import cpa.common.defaults.StopSepOperator;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.CPAWithStatistics;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpaplugin.CPAStatistics;
import exceptions.CPAException;

/**
 * CPA that defines symbolic predicate abstraction.
 * @author Erkan
 *
 */
public class SymbPredAbsCPA implements ConfigurableProgramAnalysis, CPAWithStatistics {

  private final AbstractDomain domain;
  private final SymbPredAbsTransferRelation transfer;
  private final SymbPredAbsMergeOperator merge;
  private final StopOperator stop;
  private final SymbPredAbsPrecision initialPrecision;
  private final AbstractFormulaManager abstractFormulaManager;
  private final MathsatSymbolicFormulaManager symbolicFormulaManager;
  private final SymbPredAbstFormulaManager formulaManager;
  private final SymbPredAbsCPAStatistics stats;

  private SymbPredAbsCPA() throws CPAException {
    abstractFormulaManager = new BDDAbstractFormulaManager();
    symbolicFormulaManager = new MathsatSymbolicFormulaManager();
    TheoremProver thmProver;
    String whichProver = CPAMain.cpaConfig.getProperty(
        "cpas.symbpredabs.explicit.abstraction.solver", "mathsat");
    if (whichProver.equals("mathsat")) {
      thmProver = new MathsatTheoremProver(symbolicFormulaManager, false);
    } else if (whichProver.equals("simplify")) {
      thmProver = new SimplifyTheoremProver(symbolicFormulaManager);
    } else if (whichProver.equals("yices")) {
      thmProver = new YicesTheoremProver(symbolicFormulaManager);
    } else {
      throw new CPAException("Unknown theorem prover " + whichProver
          + ", check option cpas.symbpredabs.explicit.abstraction.solver");
    }
    InterpolatingTheoremProver<Integer> itpProver =
      new MathsatInterpolatingProver(symbolicFormulaManager, false);
    formulaManager = new BDDMathsatSymbPredAbstractionAbstractManager<Integer>(abstractFormulaManager, symbolicFormulaManager, thmProver, itpProver);
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
  
  public SymbPredAbsCPA(String s1, String s2) throws CPAException {
    this();
  }

  @Override
  public AbstractDomain getAbstractDomain() {
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
  
  public SymbPredAbstFormulaManager getFormulaManager() {
    return formulaManager;
  }

  public SymbolicFormulaManager getSymbolicFormulaManager() {
    return symbolicFormulaManager;
  }

  public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
    List<Integer> abstractionPath = Collections.singletonList(node.getNodeNumber());
    SSAMap ssamap = new SSAMap();
    PathFormula pf = new PathFormula(symbolicFormulaManager.makeTrue(), ssamap);
    List<Integer> pfParents = Collections.emptyList();
    AbstractFormula initAbstraction = abstractFormulaManager.makeTrue();

    return new SymbPredAbsAbstractElement(true, node,
        pf, pfParents, pf, initAbstraction, abstractionPath, 0);
  }

  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return initialPrecision;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisionAdjustment.getInstance();
  }
  
  @Override
  public void collectStatistics(Collection<CPAStatistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }
}
