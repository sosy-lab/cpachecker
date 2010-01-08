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
package cpa.symbpredabs.summary;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.logging.Level;

import symbpredabstraction.FixedPredicateMap;
import symbpredabstraction.SSAMap;
import symbpredabstraction.UpdateablePredicateMap;
import symbpredabstraction.bdd.BDDAbstractFormulaManager;
import symbpredabstraction.interfaces.AbstractFormula;
import symbpredabstraction.interfaces.AbstractFormulaManager;
import symbpredabstraction.interfaces.InterpolatingTheoremProver;
import symbpredabstraction.interfaces.Predicate;
import symbpredabstraction.interfaces.PredicateMap;
import symbpredabstraction.interfaces.SymbolicFormula;
import symbpredabstraction.interfaces.TheoremProver;
import symbpredabstraction.mathsat.MathsatInterpolatingProver;
import symbpredabstraction.mathsat.MathsatPredicateParser;
import symbpredabstraction.mathsat.MathsatTheoremProver;
import symbpredabstraction.mathsat.SimplifyTheoremProver;
import symbpredabstraction.mathsat.YicesTheoremProver;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.CFANode;
import cmdline.CPAMain;

import common.Pair;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.CPAWithStatistics;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import cpaplugin.CPAStatistics;
import exceptions.UnrecognizedCFAEdgeException;


/**
 * CPA for symbolic lazy abstraction with summaries
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SummaryCPA implements ConfigurableProgramAnalysis, CPAWithStatistics {

    private SummaryAbstractDomain domain;
    private SummaryTransferRelation trans;
    // private SummaryMergeOperator merge;
    private SummaryStopOperator stop;
    private PrecisionAdjustment precisionAdjustment;
    private AbstractFormulaManager abstractFormulaManager;
    private MathsatSummaryFormulaManager mgr;
    private SummaryAbstractFormulaManager amgr;
    private PredicateMap pmap;
    private Map<SummaryCFANode, Map<CFANode, Pair<SymbolicFormula, SSAMap>>>
        summaryToFormulaMap;
    private Set<SummaryAbstractElement> covered;

    private SummaryCPAStatistics stats;

    private SummaryCPA() {
        domain = new SummaryAbstractDomain(this);
        trans = new SummaryTransferRelation(domain);
        // merge = new SummaryMergeOperator(domain);
        stop = new SummaryStopOperator(domain);
        precisionAdjustment = new SummaryPrecisionAdjustment();
        abstractFormulaManager = new BDDAbstractFormulaManager();
        mgr = new MathsatSummaryFormulaManager();
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
          CPAMain.logManager.log(Level.SEVERE, "ERROR, Unknown prover: " + whichProver);
            assert(false);
            System.exit(1);
        }
        InterpolatingTheoremProver<Integer> itpProver =
            new MathsatInterpolatingProver(mgr, false);
        amgr = new BDDMathsatSummaryAbstractManager<Integer>(abstractFormulaManager, mgr, thmProver, itpProver);
        covered = new HashSet<SummaryAbstractElement>();

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
              CPAMain.logManager.logException(Level.WARNING, e, "");
                preds = new Vector<Predicate>();
            }
            pmap = new FixedPredicateMap(preds);
        } else {
            pmap = new UpdateablePredicateMap();
        }

        summaryToFormulaMap =
            new HashMap<SummaryCFANode,
                        Map<CFANode, Pair<SymbolicFormula, SSAMap>>>();

        stats = new SummaryCPAStatistics(this);
    }

    /**
     * Constructor conforming to the "contract" in CompositeCPA. The two
     * arguments are ignored
     * @param s1
     * @param s2
     */
    public SummaryCPA(String s1, String s2) {
        this();
    }

    @Override
    public AbstractDomain getAbstractDomain() {
        return domain;
    }

    public TransferRelation getTransferRelation() {
        return trans;
    } 

    public MergeOperator getMergeOperator() {
        //return merge;
        return null;
    }

    @Override
    public StopOperator getStopOperator() {
        return stop;
    }

    @Override
    public PrecisionAdjustment getPrecisionAdjustment() {
      return precisionAdjustment;
    }

    public AbstractFormulaManager getAbstractFormulaManager() {
        return abstractFormulaManager;
    }

    public SummaryFormulaManager getFormulaManager() {
        return mgr;
    }

    public SummaryAbstractFormulaManager getSummaryFormulaManager() {
      return amgr;
    }
    
    public PredicateMap getPredicateMap() {
        return pmap;
    }

    // builds the path formulas corresponding to the leaves of the inner
    // subgraph of the given summary location
    public Map<CFANode, Pair<SymbolicFormula, SSAMap>> getPathFormulas(
            SummaryCFANode succLoc) {
        try {
            if (!summaryToFormulaMap.containsKey(succLoc)) {
                Map<CFANode, Pair<SymbolicFormula, SSAMap>> p =
                    mgr.buildPathFormulas(succLoc);
                summaryToFormulaMap.put(succLoc, p);

//                CPAMain.logManager.log(Level.FINEST,
//                        "SYMBOLIC FORMULA FOR " + succLoc.toString() + ": " +
//                        p.getFirst().toString());

            }
            return summaryToFormulaMap.get(succLoc);
        } catch (UnrecognizedCFAEdgeException e) {
          CPAMain.logManager.logException(Level.WARNING, e, "");
            return null;
        }
    }

    public void setCovered(SummaryAbstractElement e1) {
        covered.add(e1);        
    }
    
    public Collection<SummaryAbstractElement> getCovered() {
        return covered;
    }
    
    public void setUncovered(SummaryAbstractElement e1) {
        covered.remove(e1);
    }
    
    public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
      CPAMain.logManager.log(Level.FINEST,
                       "Getting initial element from node: ", node);

        SummaryCFANode loc = (SummaryCFANode)node;
        SummaryAbstractElement e = new SummaryAbstractElement(loc);
        Map<CFANode, Pair<SymbolicFormula, SSAMap>> p = getPathFormulas(loc);
        e.setPathFormulas(p);
        e.setAbstraction(abstractFormulaManager.makeTrue());
        e.setContext(new Stack<Pair<AbstractFormula, SummaryCFANode>>(), true);
        return e;
    }

    public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
      return new SummaryPrecision();
    }
    
    @Override
    public void collectStatistics(Collection<CPAStatistics> pStatsCollection) {
      pStatsCollection.add(stats);
    }
}
