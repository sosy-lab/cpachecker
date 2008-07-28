package cpaplugin.cpa.cpas.symbpredabs.summary;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cmdline.CPAMain;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.ConfigurableProblemAnalysis;
import cpaplugin.cpa.common.interfaces.MergeOperator;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.cpa.cpas.symbpredabs.FixedPredicateMap;
import cpaplugin.cpa.cpas.symbpredabs.Pair;
import cpaplugin.cpa.cpas.symbpredabs.Predicate;
import cpaplugin.cpa.cpas.symbpredabs.PredicateMap;
import cpaplugin.cpa.cpas.symbpredabs.SSAMap;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormula;
import cpaplugin.cpa.cpas.symbpredabs.UnrecognizedCFAEdgeException;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.MathsatPredicateParser;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.summary.BDDMathsatSummaryAbstractManager;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.summary.MathsatSummaryFormulaManager;
import cpaplugin.logging.CPACheckerLogger;
import cpaplugin.logging.CustomLogLevel;

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
    
    private SummaryCPA() {
        domain = new SummaryAbstractDomain(this);
        merge = new SummaryMergeOperator(domain);
        stop = new SummaryStopOperator(domain);
        trans = new SummaryTransferRelation(domain);
        mgr = new MathsatSummaryFormulaManager();
        amgr = new BDDMathsatSummaryAbstractManager();
        
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
        
        summaryToFormulaMap = 
            new HashMap<SummaryCFANode, 
                        Map<CFANode, Pair<SymbolicFormula, SSAMap>>>();
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

    @Override
    public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
        CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel, 
                "Getting initial element from node: " + node.toString());
        
        SummaryCFANode loc = (SummaryCFANode)node;
        SummaryAbstractElement e = new SummaryAbstractElement(loc);
        Map<CFANode, Pair<SymbolicFormula, SSAMap>> p = getPathFormulas(loc);  
        e.setPathFormulas(p);
        e.setAbstraction(amgr.makeTrue());
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

    public SummaryAbstractFormulaManager getAbstractFormulaManager() {
        return amgr;
    }

    public SummaryFormulaManager getFormulaManager() {
        return mgr;
    }

    public PredicateMap getPredicateMap() {
        return pmap;
    }

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
            return null;
        }
    }

}
