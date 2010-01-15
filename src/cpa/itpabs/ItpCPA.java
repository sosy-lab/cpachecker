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
package cpa.itpabs;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.logging.Level;

import symbpredabstraction.bdd.BDDAbstractFormulaManager;
import symbpredabstraction.interfaces.AbstractFormulaManager;
import symbpredabstraction.interfaces.InterpolatingTheoremProver;
import symbpredabstraction.interfaces.SymbolicFormula;
import symbpredabstraction.interfaces.SymbolicFormulaManager;
import symbpredabstraction.interfaces.TheoremProver;
import symbpredabstraction.mathsat.MathsatInterpolatingProver;
import symbpredabstraction.mathsat.MathsatSymbolicFormulaManager;
import symbpredabstraction.mathsat.MathsatTheoremProver;
import symbpredabstraction.mathsat.SimplifyTheoremProver;
import symbpredabstraction.mathsat.YicesTheoremProver;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.CFANode;
import cmdline.CPAMain;

import common.Pair;

import cpa.common.defaults.MergeSepOperator;
import cpa.common.defaults.StaticPrecisionAdjustment;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.CPAWithStatistics;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import cpa.symbpredabs.explicit.BDDMathsatExplicitAbstractManager;
import cpa.symbpredabs.explicit.ExplicitAbstractFormulaManager;


/**
 * CPA for interpolation-based lazy abstraction.
 * STILL ON-GOING, NOT FINISHED, AND CURRENTLY BROKEN
 *
 * This is an abstract class. See in the 'explicit/' and 'symbolic/'
 * subpackages for implementations.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public abstract class ItpCPA implements ConfigurableProgramAnalysis, CPAWithStatistics {

    protected ItpAbstractDomain domain;
    protected ItpTransferRelation trans;
    protected MergeOperator merge;
    protected ItpStopOperator stop;
    protected PrecisionAdjustment precisionAdjustment;
    protected MathsatSymbolicFormulaManager mgr;
    protected ItpCounterexampleRefiner refiner;

    // covering relation
    protected Map<ItpAbstractElement,
                Set<ItpAbstractElement>> covers;

    protected ItpCPA() {
        AbstractFormulaManager abstractFormulaManager = new BDDAbstractFormulaManager();
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
          CPAMain.logManager.log(Level.SEVERE, "Unknown solver: " + whichProver);
            assert(false);
            System.exit(1);
        }
        InterpolatingTheoremProver itpProver =
            new MathsatInterpolatingProver(mgr, true);
        ExplicitAbstractFormulaManager amgr =
            new BDDMathsatExplicitAbstractManager(abstractFormulaManager, mgr, thmProver, itpProver);

        domain = new ItpAbstractDomain(this);
        trans = new ItpTransferRelation(domain);
        merge = MergeSepOperator.getInstance();
        stop = new ItpStopOperator(domain, thmProver);
        precisionAdjustment = StaticPrecisionAdjustment.getInstance();

        refiner = new ItpCounterexampleRefiner(amgr, itpProver);

        covers = new HashMap<ItpAbstractElement,
                             Set<ItpAbstractElement>>();
        
        // TODO analysis.traversal is actually a property of ReachedElements now
        // the solution below is possible, but ignores the new enumerator for traversal method 
        if (!CPAMain.cpaConfig.getProperty("analysis.traversal").toUpperCase().equals("DFS")) {
            // this analysis only works with dfs traversal
          CPAMain.logManager.log(Level.SEVERE, 
                    "ERROR: Interpolation-based analysis works only with DFS traversal!");
            System.out.flush();
            assert(false);
            System.exit(1);
        }
    }

    /**
     * Constructor conforming to the "contract" in CompositeCPA. The two
     * arguments are ignored
     * @param s1
     * @param s2
     */
    public ItpCPA(String s1, String s2) {
        this();
    }

    @Override
    public AbstractDomain getAbstractDomain() {
        return domain;
    }
    
    public TransferRelation getTransferRelation() {
        return trans;
    }

    @Override
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

    public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
      CPAMain.logManager.log(Level.FINEST,
                       "Getting initial element from node: ", node);

        ItpAbstractElement e = getElementCreator().create(node);
        e.setAbstraction(mgr.makeTrue());
        e.setContext(new Stack<Pair<SymbolicFormula, CFANode>>(), true);
        return e;
    }

    public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
      return null;
    }

    public ItpCounterexampleRefiner getRefiner() {
        return refiner;
    }

    public SymbolicFormulaManager getFormulaManager() {
        return mgr;
    }

    public Set<ItpAbstractElement> getCoveredBy(
        ItpAbstractElement e){
        if (covers.containsKey(e)) {
            return covers.get(e);
        } else {
            return Collections.emptySet();
        }
    }

    public void setCoveredBy(ItpAbstractElement covered,
                             ItpAbstractElement e) {
        Set<ItpAbstractElement> s;
        if (covers.containsKey(e)) {
            s = covers.get(e);
        } else {
            s = new TreeSet<ItpAbstractElement>();
        }
        s.add(covered);
        covers.put(e, s);
        covered.setCoveredBy(e);
    }

    /**
     * uncovers all the element that were covered by "e"
     */
    public Collection<ItpAbstractElement> uncoverAll(
            ItpAbstractElement e) {
        if (covers.containsKey(e)) {
            Collection<ItpAbstractElement> ret = covers.remove(e);
            for (ItpAbstractElement el : ret) {
                el.setCoveredBy(null);
            }
            return ret;
        } else {
            return Collections.emptySet();
        }
    }
    
    public void uncover(ItpAbstractElement e) {
        ItpAbstractElement c = e.getCoveredBy();
        assert(c != null);
        e.setCoveredBy(null);
        assert (covers.containsKey(c));
        covers.get(c).remove(e);
    }

    /**
     * uncovers all the descendants of the given element
     */
    public Collection<ItpAbstractElement> removeDescendantsFromCovering(
            ItpAbstractElement e2) {
        Collection<AbstractElement> sub =
            trans.getART().getSubtree(e2, false, true);
        Set<ItpAbstractElement> ret =
            new TreeSet<ItpAbstractElement>();
        for (AbstractElement ae : sub) {
            ItpAbstractElement e = (ItpAbstractElement)ae;
            if (covers.containsKey(e)) {
                ret.addAll(uncoverAll(e));
            }
        }
        return ret;
    }

    /**
     * checks whether an element is covered
     */
    public boolean isCovered(ItpAbstractElement e) {
        ItpAbstractElement cur = e;
        if (cur.getAbstraction().isFalse()) return true;
//        while (cur != null) {
            if (cur.isCovered()) return true;
//            cur = cur.getParent();
//        }
        return false;
    }

    public abstract ItpAbstractElementManager getElementCreator();
}
