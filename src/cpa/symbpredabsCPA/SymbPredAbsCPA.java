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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;

import symbpredabstraction.FixedPredicateMap;
import symbpredabstraction.PathFormula;
import symbpredabstraction.SSAMap;
import symbpredabstraction.UpdateablePredicateMap;
import symbpredabstraction.bdd.BDDAbstractFormulaManager;
import symbpredabstraction.interfaces.AbstractFormula;
import symbpredabstraction.interfaces.AbstractFormulaManager;
import symbpredabstraction.interfaces.InterpolatingTheoremProver;
import symbpredabstraction.interfaces.Predicate;
import symbpredabstraction.interfaces.PredicateMap;
import symbpredabstraction.interfaces.SymbolicFormulaManager;
import symbpredabstraction.interfaces.TheoremProver;
import symbpredabstraction.mathsat.MathsatInterpolatingProver;
import symbpredabstraction.mathsat.MathsatPredicateParser;
import symbpredabstraction.mathsat.MathsatSymbolicFormulaManager;
import symbpredabstraction.mathsat.MathsatTheoremProver;
import symbpredabstraction.mathsat.SimplifyTheoremProver;
import symbpredabstraction.mathsat.YicesTheoremProver;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cmdline.CPAMain;
import cpa.common.defaults.StaticPrecisisonAdjustment;
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
  private final AbstractFormulaManager abstractFormulaManager;
  private final MathsatSymbolicFormulaManager symbolicFormulaManager;
  private final BDDMathsatSymbPredAbstractionAbstractManager formulaManager;
  private final PredicateMap predicateMap;
  private final SymbPredAbsCPAStatistics stats;

  private SymbPredAbsCPA() throws CPAException {
    predicateMap = createPredicateMap();
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
    InterpolatingTheoremProver itpProver =
      new MathsatInterpolatingProver(symbolicFormulaManager, false);
    formulaManager = new BDDMathsatSymbPredAbstractionAbstractManager(abstractFormulaManager, symbolicFormulaManager, thmProver, itpProver);
    domain = new SymbPredAbsAbstractDomain(abstractFormulaManager);
    transfer = new SymbPredAbsTransferRelation(this);
    merge = new SymbPredAbsMergeOperator(this);
    stop = new StopSepOperator(domain.getPartialOrder());

    
    stats = new SymbPredAbsCPAStatistics(this);
  }

  private PredicateMap createPredicateMap() {
    // for testing purposes, it's nice to be able to use a given set of
    // predicates and disable refinement
    if (CPAMain.cpaConfig.getBooleanValue("cpas.symbpredabs.abstraction.norefinement")) {
      MathsatPredicateParser p = new MathsatPredicateParser(symbolicFormulaManager, formulaManager);
      Collection<Predicate> preds;
      try {
        String pth = CPAMain.cpaConfig.getProperty("predicates.path");
        File f = new File(pth, "predicates.msat");
        InputStream in = new FileInputStream(f);
        preds = p.parsePredicates(in);
      } catch (IOException e) {
        CPAMain.logManager.logException(Level.WARNING, e, "");
        preds = new Vector<Predicate>();
      }
      return new FixedPredicateMap(preds);
    } else {
      return new UpdateablePredicateMap();
    }

  }
  
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

  public PredicateMap getPredicateMap() {
    return predicateMap;
  }

  public SymbolicFormulaManager getSymbolicFormulaManager() {
    return symbolicFormulaManager;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getInitialElement(cfa.objectmodel.CFAFunctionDefinitionNode)
   */
  public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
    CPAMain.logManager.log(Level.FINEST,
        "Getting initial element from node: ", node);

    //SymbPredAbsAbstractElement e = new SymbPredAbsAbstractElement(domain, loc, null);

    List<Integer> parents = new ArrayList<Integer>();
    parents.add(node.getNodeNumber());
    SSAMap ssamap = new SSAMap();
    PathFormula pf = new PathFormula(symbolicFormulaManager.makeTrue(), ssamap);
    List<Integer> pfParents = new ArrayList<Integer>();
    PathFormula initPf = new PathFormula(symbolicFormulaManager.makeTrue(), ssamap);
    AbstractFormula initAbstraction = abstractFormulaManager.makeTrue();

    SymbPredAbsAbstractElement e = new SymbPredAbsAbstractElement(true, node,
        pf, pfParents, initPf, initAbstraction, parents, 0);
    // TODO check
//    e.setMaxIndex(new SSAMap());

    return e;
  }

  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return null;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisisonAdjustment.getInstance();
  }
  
  @Override
  public void collectStatistics(Collection<CPAStatistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }
}
