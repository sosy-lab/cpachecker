package cpa.symbpredabsCPA;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import logging.CustomLogLevel;
import logging.LazyLogger;
import symbpredabstraction.BDDMathsatSymbPredAbstractionAbstractManager;
import symbpredabstraction.ParentsList;
import symbpredabstraction.PathFormula;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cmdline.CPAMain;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import cpa.symbpredabs.AbstractFormula;
import cpa.symbpredabs.AbstractFormulaManager;
import cpa.symbpredabs.FixedPredicateMap;
import cpa.symbpredabs.InterpolatingTheoremProver;
import cpa.symbpredabs.Predicate;
import cpa.symbpredabs.PredicateMap;
import cpa.symbpredabs.SSAMap;
import cpa.symbpredabs.SymbolicFormulaManager;
import cpa.symbpredabs.TheoremProver;
import cpa.symbpredabs.UpdateablePredicateMap;
import cpa.symbpredabs.mathsat.MathsatInterpolatingProver;
import cpa.symbpredabs.mathsat.MathsatPredicateParser;
import cpa.symbpredabs.mathsat.MathsatSymbolicFormulaManager;
import cpa.symbpredabs.mathsat.MathsatTheoremProver;
import cpa.symbpredabs.mathsat.SimplifyTheoremProver;
import cpa.symbpredabs.mathsat.YicesTheoremProver;

public class SymbPredAbsCPA implements ConfigurableProgramAnalysis {

  private SymbPredAbsAbstractDomain domain;
  private SymbPredAbsMergeOperator merge;
  private SymbPredAbsStopOperator stop;
  private SymbPredAbsTransferRelation trans;
  private MathsatSymbolicFormulaManager mgr;
  private BDDMathsatSymbPredAbstractionAbstractManager amgr;
  private Map<SymbPredAbsAbstractElement, Set<SymbPredAbsAbstractElement>> covers;
  private PredicateMap pmap;

  // TODO later
  //private SymbPredAbsCPAStatistics stats;

  private SymbPredAbsCPA() {
    mgr = new MathsatSymbolicFormulaManager();
    TheoremProver thmProver = null;
    String whichProver = CPAMain.cpaConfig.getProperty(
        "cpas.symbpredabs.explicit.abstraction.solver", "mathsat");
    if (whichProver.equals("mathsat")) {
      thmProver = new MathsatTheoremProver(mgr, false);
    } else if (whichProver.equals("simplify")) {
      thmProver = new SimplifyTheoremProver(mgr);
    } else if (whichProver.equals("yices")) {
      thmProver = new YicesTheoremProver(mgr);
    } else {
      System.out.println("ERROR, Unknown prover: " + whichProver);
      assert(false);
      System.exit(1);
    }
    InterpolatingTheoremProver itpProver =
      new MathsatInterpolatingProver(mgr, false);
    amgr = new BDDMathsatSymbPredAbstractionAbstractManager(thmProver, itpProver);
    covers = new HashMap<SymbPredAbsAbstractElement,
    Set<SymbPredAbsAbstractElement>>();
    domain = new SymbPredAbsAbstractDomain(this);
    merge = new SymbPredAbsMergeOperator(domain);
    stop = new SymbPredAbsStopOperator(domain);
    trans = new SymbPredAbsTransferRelation(domain, mgr, amgr);

    // for testing purposes, it's nice to be able to use a given set of
    // predicates and disable refinement
    if (CPAMain.cpaConfig.getBooleanValue(
    "cpas.symbpredabs.abstraction.norefinement")) {
      MathsatPredicateParser p = new MathsatPredicateParser(mgr, amgr);
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
      pmap = new FixedPredicateMap(preds);
    } else {
      pmap = new UpdateablePredicateMap();
    }

    // TODO fix
    //stats = new SummaryCPAStatistics(this);
  }

  /**
   * Constructor conforming to the "contract" in CompositeCPA. The two
   * arguments are ignored
   * @param s1
   * @param s2
   */
  public SymbPredAbsCPA(String s1, String s2) {
    this();
  }

  // TODO fix
//public CPAStatistics getStatistics() {
//return stats;
//}

  @Override
  public AbstractDomain getAbstractDomain() {
    return domain;
  }

  @Override
  public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
    LazyLogger.log(CustomLogLevel.SpecificCPALevel,
        "Getting initial element from node: ", node);

    //SymbPredAbsAbstractElement e = new SymbPredAbsAbstractElement(domain, loc, null);
    
    ParentsList parents = new ParentsList();
    parents.addToList(node.getNodeNumber());
    SSAMap ssamap = new SSAMap();
    PathFormula pf = new PathFormula(mgr.makeTrue(), ssamap);
    PathFormula initPf = new PathFormula(mgr.makeTrue(), ssamap);
    AbstractFormula initAbstraction = amgr.makeTrue();
    
    PredicateMap pmap;
    if (CPAMain.cpaConfig.getBooleanValue("cpas.symbpredabs.abstraction.norefinement")) {
      MathsatPredicateParser p = new MathsatPredicateParser(mgr, amgr);
      Collection<Predicate> preds = null;
      try {
        String pth = CPAMain.cpaConfig.getProperty("predicates.path");
        File f = new File(pth, "predicates.msat");
        InputStream in = new FileInputStream(f);
        preds = p.parsePredicates(in);
      } catch (IOException er) {
        er.printStackTrace();
        preds = new Vector<Predicate>();
      }
      pmap = new FixedPredicateMap(preds);
    } else {
      pmap = new UpdateablePredicateMap();
    }
    assert(pmap != null);
    
    SymbPredAbsAbstractElement e = new SymbPredAbsAbstractElement(domain, true, node,
        pf, initPf, initAbstraction, parents, null, pmap);
    e.setMaxIndex(new SSAMap());

    return e;
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
  public TransferRelation getTransferRelation() {
    return trans;
  }

  public AbstractFormulaManager getAbstractFormulaManager() {
    return amgr;
  }

  public SymbolicFormulaManager getFormulaManager() {
    return mgr;
  }

  public PredicateMap getPredicateMap() {
    return pmap;
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
    return mgr;
  }
}
