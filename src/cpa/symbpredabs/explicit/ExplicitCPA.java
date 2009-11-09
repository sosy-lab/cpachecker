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
package cpa.symbpredabs.explicit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import logging.CustomLogLevel;
import logging.LazyLogger;
import symbpredabstraction.FixedPredicateMap;
import symbpredabstraction.UpdateablePredicateMap;
import symbpredabstraction.interfaces.AbstractFormula;
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
import cfa.objectmodel.CFANode;
import cmdline.CPAMain;

import common.Pair;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import cpaplugin.CPAStatistics;


/**
 * CPA for Explicit-state lazy abstraction.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ExplicitCPA implements ConfigurableProgramAnalysis {

    private final ExplicitAbstractDomain domain;
    private final ExplicitTransferRelation trans;
    private final ExplicitMergeOperator merge;
    private final ExplicitStopOperator stop;
    private final PrecisionAdjustment precisionAdjustment;
    private final MathsatSymbolicFormulaManager mgr;
    private final BDDMathsatExplicitAbstractManager amgr;
    private PredicateMap pmap;

    // covering relation
    //private final Map<ExplicitAbstractElement, Set<ExplicitAbstractElement>> covers;
    private Set<ExplicitAbstractElement> covered;

    private final ExplicitCPAStatistics stats;

    private ExplicitCPA() {
        domain = new ExplicitAbstractDomain(this);
        trans = new ExplicitTransferRelation(domain);
        merge = new ExplicitMergeOperator();
        stop = new ExplicitStopOperator(domain);
        precisionAdjustment = new ExplicitPrecisionAdjustment();
        mgr = new MathsatSymbolicFormulaManager();
        String whichProver = CPAMain.cpaConfig.getProperty(
                "cpas.symbpredabs.explicit.abstraction.solver", "mathsat");
        TheoremProver prover = null;
        if (whichProver.equals("mathsat")) {
            prover = new MathsatTheoremProver(mgr, false);
        } else if (whichProver.equals("simplify")) {
            prover = new SimplifyTheoremProver(mgr);
        } else if (whichProver.equals("yices")) {
            prover = new YicesTheoremProver(mgr);
        } else {
            System.out.println("ERROR, UNSUPPORTED SOLVER: " + whichProver);
            System.exit(1);
        }
        InterpolatingTheoremProver itpProver =
            new MathsatInterpolatingProver(mgr, true);
        amgr = new BDDMathsatExplicitAbstractManager(prover, itpProver);

//        covers = new HashMap<ExplicitAbstractElement,
//                             Set<ExplicitAbstractElement>>();
        covered = new HashSet<ExplicitAbstractElement>();

        MathsatPredicateParser p = new MathsatPredicateParser(mgr, amgr);
        Collection<Predicate> preds = null;
        try {
            String pth = CPAMain.cpaConfig.getProperty(
                    "cpas.symbpredabs.abstraction.fixedPredMap", null);
            if (pth != null) {
                File f = new File(pth);
                InputStream in = new FileInputStream(f);
                preds = p.parsePredicates(in);
            } else {
                preds = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            preds = new Vector<Predicate>();
        }
        if (CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.abstraction.norefinement")) {
            pmap = new FixedPredicateMap(preds);
        } else {
            pmap = new UpdateablePredicateMap(preds);
        }

        stats = new ExplicitCPAStatistics(this);
    }

    /**
     * Constructor conforming to the "contract" in CompositeCPA. The two
     * arguments are ignored
     * @param s1
     * @param s2
     */
    public ExplicitCPA(String s1, String s2) {
        this();
    }

    public CPAStatistics getStatistics() {
        return stats;
    }
    
    @Override
    public AbstractDomain getAbstractDomain() {
        return domain;
    }

    @Override
    public TransferRelation getTransferRelation() {
        return trans;
    }

    public MergeOperator getMergeOperator() {
        return merge;
        //return null;
    }

    @Override
    public StopOperator getStopOperator() {
        return stop;
    }

    @Override
    public PrecisionAdjustment getPrecisionAdjustment() {
      return precisionAdjustment;
    }

    public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
        LazyLogger.log(CustomLogLevel.SpecificCPALevel,
                       "Getting initial element from node: ", node);

        ExplicitAbstractElement e = new ExplicitAbstractElement(node);
        e.setAbstraction(amgr.makeTrue());
        e.setContext(new Stack<Pair<AbstractFormula, CFANode>>(), true);
        return e;
    }
    
    public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
      return new ExplicitPrecision();
    }

    public ExplicitAbstractFormulaManager getAbstractFormulaManager() {
        return amgr;
    }

    public SymbolicFormulaManager getFormulaManager() {
        return mgr;
    }

    public PredicateMap getPredicateMap() {
        return pmap;
    }

    public void setCovered(ExplicitAbstractElement e1) {
        covered.add(e1);        
    }
    
    public Collection<ExplicitAbstractElement> getCovered() {
        return covered;
    }
    
    public void setUncovered(ExplicitAbstractElement e1) {
        covered.remove(e1);
    }


//    public Set<ExplicitAbstractElement> getCoveredBy(ExplicitAbstractElement e){
//        if (covers.containsKey(e)) {
//            return covers.get(e);
//        } else {
//            return Collections.emptySet();
//        }
//    }
//
//    public void setCoveredBy(ExplicitAbstractElement covered,
//                             ExplicitAbstractElement e) {
//        Set<ExplicitAbstractElement> s;
//        if (covers.containsKey(e)) {
//            s = covers.get(e);
//        } else {
//            s = new HashSet<ExplicitAbstractElement>();
//        }
//        s.add(covered);
//        covers.put(e, s);
//    }
//
//    public void uncoverAll(ExplicitAbstractElement e) {
//        if (covers.containsKey(e)) {
//            covers.remove(e);
//        }
//    }

}
