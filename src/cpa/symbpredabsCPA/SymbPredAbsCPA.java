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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import logging.CustomLogLevel;
import logging.LazyLogger;
import symbpredabstraction.FixedPredicateMap;
import symbpredabstraction.PathFormula;
import symbpredabstraction.SSAMap;
import symbpredabstraction.UpdateablePredicateMap;
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
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.RefinableCPA;
import cpa.common.interfaces.RefinementManager;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import cpaplugin.CPAStatistics;

/**
 * CPA that defines symbolic predicate abstraction.
 * @author Erkan
 *
 */
public class SymbPredAbsCPA implements RefinableCPA {

  private SymbPredAbsAbstractDomain domain;
  private SymbPredAbsTransferRelation transfer;
  private SymbPredAbsMergeOperator merge;
  private SymbPredAbsStopOperator stop;
  private PrecisionAdjustment precisionAdjustment;
  private SymbPredAbsRefinementManager refinementManager;
  private MathsatSymbolicFormulaManager symbolicFormulaManager;
  private BDDMathsatSymbPredAbstractionAbstractManager abstractFormulaManager;
  private Map<SymbPredAbsAbstractElement, Set<SymbPredAbsAbstractElement>> covers;
  private PredicateMap predicateMap;
  private SymbPredAbsCPAStatistics stats;

  private SymbPredAbsCPA() {
    symbolicFormulaManager = new MathsatSymbolicFormulaManager();
    TheoremProver thmProver = null;
    String whichProver = CPAMain.cpaConfig.getProperty(
        "cpas.symbpredabs.explicit.abstraction.solver", "mathsat");
    if (whichProver.equals("mathsat")) {
      thmProver = new MathsatTheoremProver(symbolicFormulaManager, false);
    } else if (whichProver.equals("simplify")) {
      thmProver = new SimplifyTheoremProver(symbolicFormulaManager);
    } else if (whichProver.equals("yices")) {
      thmProver = new YicesTheoremProver(symbolicFormulaManager);
    } else {
      System.out.println("ERROR, Unknown prover: " + whichProver);
      assert(false);
      System.exit(1);
    }
    InterpolatingTheoremProver itpProver =
      new MathsatInterpolatingProver(symbolicFormulaManager, false);
    abstractFormulaManager = new BDDMathsatSymbPredAbstractionAbstractManager(thmProver, itpProver);
    covers = new HashMap<SymbPredAbsAbstractElement, Set<SymbPredAbsAbstractElement>>();
    domain = new SymbPredAbsAbstractDomain(this);
    transfer = new SymbPredAbsTransferRelation(domain, symbolicFormulaManager, abstractFormulaManager);
    merge = new SymbPredAbsMergeOperator(domain);
    stop = new SymbPredAbsStopOperator(domain);
    precisionAdjustment = new SymbPredAbsPrecisionAdjustment();

    // for testing purposes, it's nice to be able to use a given set of
    // predicates and disable refinement
    if (CPAMain.cpaConfig.getBooleanValue(
    "cpas.symbpredabs.abstraction.norefinement")) {
      MathsatPredicateParser p = new MathsatPredicateParser(symbolicFormulaManager, abstractFormulaManager);
      Collection<Predicate> preds = null;
      try {
        String pth = CPAMain.cpaConfig.getProperty("predicates.path");
        File f = new File(pth, "predicates.msat");
        InputStream in = new FileInputStream(f);
        preds = p.parsePredicates(in);
      } catch (IOException e) {
        e.printStackTrace();
        preds = new Vector<Predicate>();
      }
      predicateMap = new FixedPredicateMap(preds);
    } else {
      predicateMap = new UpdateablePredicateMap();
    }

    stats = new SymbPredAbsCPAStatistics(this);
    refinementManager = new SymbPredAbsRefinementManager(this);
  }

  public SymbPredAbsCPA(String s1, String s2) {
    this();
  }

  public CPAStatistics getStatistics() {
    return stats;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return domain;
  }

  public TransferRelation getTransferRelation() {
    return transfer;
  }

  public MergeOperator getMergeOperator() {
    return merge;
  }

  @Override
  public StopOperator getStopOperator() {
    return stop;
  }

  public AbstractFormulaManager getAbstractFormulaManager() {
    return abstractFormulaManager;
  }

  public SymbolicFormulaManager getFormulaManager() {
    return symbolicFormulaManager;
  }

  public PredicateMap getPredicateMap() {
    return predicateMap;
  }

  public Set<SymbPredAbsAbstractElement> getCoveredBy(SymbPredAbsAbstractElement e){
    if (covers.containsKey(e)) {
      return covers.get(e);
    } else {
      return Collections.emptySet();
    }
  }

  public void setCoveredBy(SymbPredAbsAbstractElement covered,
                           SymbPredAbsAbstractElement e) {
    Set<SymbPredAbsAbstractElement> s;
    if (covers.containsKey(e)) {
      s = covers.get(e);
    } else {
      s = new HashSet<SymbPredAbsAbstractElement>();
    }
    s.add(covered);
    covers.put(e, s);
  }

  public void uncoverAll(SymbPredAbsAbstractElement e) {
    if (covers.containsKey(e)) {
      covers.remove(e);
    }
  }

  public SymbolicFormulaManager getSymbolicFormulaManager() {
    return symbolicFormulaManager;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.ConfigurableProgramAnalysis#getInitialElement(cfa.objectmodel.CFAFunctionDefinitionNode)
   */
  public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
    LazyLogger.log(CustomLogLevel.SpecificCPALevel,
        "Getting initial element from node: ", node);

    //SymbPredAbsAbstractElement e = new SymbPredAbsAbstractElement(domain, loc, null);

    AbstractionPathList parents = new AbstractionPathList();
    parents.addToList(node.getNodeNumber());
    SSAMap ssamap = new SSAMap();
    PathFormula pf = new PathFormula(symbolicFormulaManager.makeTrue(), ssamap);
    List<Integer> pfParents = new ArrayList<Integer>();
    PathFormula initPf = new PathFormula(symbolicFormulaManager.makeTrue(), ssamap);
    AbstractFormula initAbstraction = abstractFormulaManager.makeTrue();

    SymbPredAbsAbstractElement e = new SymbPredAbsAbstractElement(domain, true, node,
        pf, pfParents, initPf, initAbstraction, parents, null, predicateMap);
    // TODO check
//    e.setMaxIndex(new SSAMap());

    return e;
  }

  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return new SymbPredAbsPrecision();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public RefinementManager getRefinementManager() {
    return refinementManager;
  }
}
