package cpaplugin.cpa.cpas.symbpredabs.summary;

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
import cpaplugin.cpa.cpas.symbpredabs.AbstractFormula;
import cpaplugin.cpa.cpas.symbpredabs.FixedPredicateMap;
import cpaplugin.cpa.cpas.symbpredabs.InterpolatingTheoremProver;
import cpaplugin.cpa.cpas.symbpredabs.Pair;
import cpaplugin.cpa.cpas.symbpredabs.Predicate;
import cpaplugin.cpa.cpas.symbpredabs.PredicateMap;
import cpaplugin.cpa.cpas.symbpredabs.SSAMap;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormula;
import cpaplugin.cpa.cpas.symbpredabs.TheoremProver;
import cpaplugin.cpa.cpas.symbpredabs.UnrecognizedCFAEdgeException;
import cpaplugin.cpa.cpas.symbpredabs.UpdateablePredicateMap;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.MathsatInterpolatingProver;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.MathsatPredicateParser;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.MathsatTheoremProver;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.SimplifyTheoremProver;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.YicesTheoremProver;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.summary.BDDMathsatSummaryAbstractManager;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.summary.MathsatSummaryFormulaManager;
import cpaplugin.logging.CustomLogLevel;
import cpaplugin.logging.LazyLogger;


/**
 * CPA for symbolic lazy abstraction with summaries
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SummaryCPA implements ConfigurableProblemAnalysis {
    
    private SummaryAbstractDomain domain;
    private SummaryMergeOperator merge;
    private SummaryStopOperator stop;
    private SummaryTransferRelation trans;
    private MathsatSummaryFormulaManager mgr;
    private BDDMathsatSummaryAbstractManager amgr;
    private PredicateMap pmap;
    private Map<SummaryCFANode, Map<CFANode, Pair<SymbolicFormula, SSAMap>>> 
        summaryToFormulaMap;
    private Map<SummaryAbstractElement, Set<SummaryAbstractElement>> covers;
    
    private SummaryCPAStatistics stats;
    
    private SummaryCPA() {
        domain = new SummaryAbstractDomain(this);
        merge = new SummaryMergeOperator(domain);
        stop = new SummaryStopOperator(domain);
        trans = new SummaryTransferRelation(domain);
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
            System.out.println("ERROR, Unknown prover: " + whichProver);
            assert(false);
            System.exit(1);
        }
        InterpolatingTheoremProver itpProver = 
            new MathsatInterpolatingProver(mgr, false);
        amgr = new BDDMathsatSummaryAbstractManager(thmProver, itpProver);
        covers = new HashMap<SummaryAbstractElement, 
                             Set<SummaryAbstractElement>>();

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
        
        SummaryCFANode loc = (SummaryCFANode)node;
        SummaryAbstractElement e = new SummaryAbstractElement(loc);
        Map<CFANode, Pair<SymbolicFormula, SSAMap>> p = getPathFormulas(loc);  
        e.setPathFormulas(p);
        e.setAbstraction(amgr.makeTrue());
        e.setContext(new Stack<Pair<AbstractFormula, SummaryCFANode>>(), true);
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

    public SummaryAbstractFormulaManager getAbstractFormulaManager() {
        return amgr;
    }

    public SummaryFormulaManager getFormulaManager() {
        return mgr;
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
                
//                CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel,
//                        "SYMBOLIC FORMULA FOR " + succLoc.toString() + ": " + 
//                        p.getFirst().toString());
                
            }
            return summaryToFormulaMap.get(succLoc);
        } catch (UnrecognizedCFAEdgeException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public Set<SummaryAbstractElement> getCoveredBy(SummaryAbstractElement e){
        if (covers.containsKey(e)) {
            return covers.get(e);
        } else {
            return Collections.emptySet();
        }
    }

    public void setCoveredBy(SummaryAbstractElement covered, 
                             SummaryAbstractElement e) {
        Set<SummaryAbstractElement> s;
        if (covers.containsKey(e)) {
            s = covers.get(e);
        } else {
            s = new HashSet<SummaryAbstractElement>();
        }
        s.add(covered);
        covers.put(e, s);
    }
    
    public void uncoverAll(SummaryAbstractElement e) {
        if (covers.containsKey(e)) {
            covers.remove(e);
        }
    }
    

}
