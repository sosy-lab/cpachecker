package cpa.symbpredabs.explicit;

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
import java.util.Stack;
import java.util.Vector;

import logging.CustomLogLevel;
import logging.LazyLogger;

import common.LocationMappedReachedSet;

import cmdline.CPAMain;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.CFANode;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import cpaplugin.CPAStatistics;
import cpa.symbpredabs.AbstractFormula;
import cpa.symbpredabs.FixedPredicateMap;
import cpa.symbpredabs.InterpolatingTheoremProver;
import cpa.symbpredabs.Pair;
import cpa.symbpredabs.Predicate;
import cpa.symbpredabs.PredicateMap;
import cpa.symbpredabs.SymbolicFormulaManager;
import cpa.symbpredabs.TheoremProver;
import cpa.symbpredabs.UpdateablePredicateMap;
import cpa.symbpredabs.mathsat.MathsatInterpolatingProver;
import cpa.symbpredabs.mathsat.MathsatPredicateParser;
import cpa.symbpredabs.mathsat.MathsatSymbolicFormulaManager;
import cpa.symbpredabs.mathsat.MathsatTheoremProver;
import cpa.symbpredabs.mathsat.SimplifyTheoremProver;
import cpa.symbpredabs.mathsat.YicesTheoremProver;


/**
 * CPA for Explicit-state lazy abstraction.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ExplicitCPA implements ConfigurableProgramAnalysis {
    
    private ExplicitAbstractDomain domain;
    private ExplicitMergeOperator merge;
    private ExplicitStopOperator stop;
    private ExplicitTransferRelation trans;
    private MathsatSymbolicFormulaManager mgr;
    private BDDMathsatExplicitAbstractManager amgr;
    private PredicateMap pmap;
    
    // covering relation
    private Map<ExplicitAbstractElement, Set<ExplicitAbstractElement>> covers;
    
    private ExplicitCPAStatistics stats;
    
    private ExplicitCPA() {
        domain = new ExplicitAbstractDomain(this);
        merge = new ExplicitMergeOperator(domain);
        stop = new ExplicitStopOperator(domain);
        trans = new ExplicitTransferRelation(domain);
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
        
        covers = new HashMap<ExplicitAbstractElement, 
                             Set<ExplicitAbstractElement>>();
        
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
    
    public Collection<AbstractElement> newReachedSet() {
        return new LocationMappedReachedSet();
    }
    
    @Override
    public AbstractDomain getAbstractDomain() {
        return domain;
    }

    @Override
    public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
        LazyLogger.log(CustomLogLevel.SpecificCPALevel, 
                       "Getting initial element from node: ", node);
        
        ExplicitAbstractElement e = new ExplicitAbstractElement(node);
        e.setAbstraction(amgr.makeTrue());
        e.setContext(new Stack<Pair<AbstractFormula, CFANode>>(), true);
        return e;
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
    public TransferRelation getTransferRelation() {
        return trans;
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
    
    
    public Set<ExplicitAbstractElement> getCoveredBy(ExplicitAbstractElement e){
        if (covers.containsKey(e)) {
            return covers.get(e);
        } else {
            return Collections.emptySet();
        }
    }

    public void setCoveredBy(ExplicitAbstractElement covered, 
                             ExplicitAbstractElement e) {
        Set<ExplicitAbstractElement> s;
        if (covers.containsKey(e)) {
            s = covers.get(e);
        } else {
            s = new HashSet<ExplicitAbstractElement>();
        }
        s.add(covered);
        covers.put(e, s);
    }
    
    public void uncoverAll(ExplicitAbstractElement e) {
        if (covers.containsKey(e)) {
            covers.remove(e);
        }
    }
}
