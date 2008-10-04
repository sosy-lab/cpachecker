package cpaplugin.cpa.cpas.symbpredabsCPA;

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

import symbpredabstraction.FixedPredicateMap;
import symbpredabstraction.PathFormula;
import symbpredabstraction.Predicate;
import symbpredabstraction.PredicateMap;
import symbpredabstraction.SSAMap;
import symbpredabstraction.UpdateablePredicateMap;
import cpaplugin.CPAStatistics;
import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cmdline.CPAMain;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.ConfigurableProblemAnalysis;
import cpaplugin.cpa.common.interfaces.MergeOperator;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.MathsatPredicateParser;
import cpaplugin.logging.CustomLogLevel;
import cpaplugin.logging.LazyLogger;


/**
 * CPA for symbolic lazy abstraction with summaries
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SymbPredAbsCPA implements ConfigurableProblemAnalysis {
    
    private SymbPredAbsAbstractDomain domain;
    private SymbPredAbsMergeOperator merge;
    private SymbPredAbsStopOperator stop;
    private SymbPredAbsTransferRelation trans;
    private MathsatSymbPredAbsFormulaManager mgr;
    private BDDMathsatSymbPredAbsAbstractManager amgr;
    private PredicateMap pmap;
    private Map<SymbPredAbsAbstractElement, Set<SymbPredAbsAbstractElement>> covers;
    
    private SymbPredAbsCPAStatistics stats;
    
    private SymbPredAbsCPA() {
        domain = new SymbPredAbsAbstractDomain(this);
        merge = new SymbPredAbsMergeOperator(domain);
        stop = new SymbPredAbsStopOperator(domain);
        trans = new SymbPredAbsTransferRelation(domain);
        mgr = new MathsatSymbPredAbsFormulaManager();
        amgr = new BDDMathsatSymbPredAbsAbstractManager();
        covers = new HashMap<SymbPredAbsAbstractElement, 
                             Set<SymbPredAbsAbstractElement>>();

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
        
//        summaryToFormulaMap = 
//            new HashMap<SymbPredAbsCFANode, 
//                        Map<CFANode, Pair<SymbolicFormula, SSAMap>>>();
        
        stats = new SymbPredAbsCPAStatistics(this);
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
    
    public CPAStatistics getStatistics() {
        return stats;
    }
    
    @Override
    public AbstractDomain getAbstractDomain() {
        return domain;
    }

    @Override
    public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
        LazyLogger.log(CustomLogLevel.SpecificCPALevel, 
                       "Getting initial element from node: ", node);
        
        CFANode loc = node;
        SymbPredAbsAbstractElement e = new SymbPredAbsAbstractElement(loc, loc);
        PathFormula pf = getNewPathFormula(loc);  
        e.setPathFormula(pf);
        e.setAbstraction(amgr.makeTrue());
        // TODO update here
        // we can start we initial set of predicated
        // or we can use fixed predicate maps
        PredicateMap pmap = new UpdateablePredicateMap();
        e.setPredicates(pmap);
        //e.setContext(new Stack<Pair<AbstractFormula, SymbPredAbsCFANode>>(), true);
        // we return an tuple (loc, loc, pf, abst, null), the parent is null since this is the 
        // initial element
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

    public SymbPredAbsAbstractFormulaManager getAbstractFormulaManager() {
        return amgr;
    }

    public SymbPredAbsFormulaManager getFormulaManager() {
        return mgr;
    }

    public PredicateMap getPredicateMap() {
        return pmap;
    }

    // builds the path formulas corresponding to the leaves of the inner
    // subgraph of the given summary location
    public PathFormula getNewPathFormula(CFANode succLoc) {
    	
    	SSAMap ssamap = new SSAMap();
    	return new PathFormula(mgr.makeTrue(), ssamap);
    	
//        try {
//            if (!summaryToFormulaMap.containsKey(succLoc)) {
//                Map<CFANode, Pair<SymbolicFormula, SSAMap>> p = 
//                    mgr.buildPathFormulas(succLoc); 
//                summaryToFormulaMap.put(succLoc, p);
//                
////                CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel,
////                        "SYMBOLIC FORMULA FOR " + succLoc.toString() + ": " + 
////                        p.getFirst().toString());
//                
//            }
//            return summaryToFormulaMap.get(succLoc);
//        } catch (UnrecognizedCFAEdgeException e) {
//            e.printStackTrace();
//            return null;
//        }
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
}
