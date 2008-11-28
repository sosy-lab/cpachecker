package cpaplugin.cpa.cpas.itpabs;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cmdline.CPAMain;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpaplugin.cpa.common.interfaces.MergeOperator;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.cpa.cpas.symbpredabs.InterpolatingTheoremProver;
import cpaplugin.cpa.cpas.symbpredabs.Pair;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormula;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormulaManager;
import cpaplugin.cpa.cpas.symbpredabs.TheoremProver;
import cpaplugin.cpa.cpas.symbpredabs.explicit.BDDMathsatExplicitAbstractManager;
import cpaplugin.cpa.cpas.symbpredabs.explicit.ExplicitAbstractFormulaManager;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.MathsatInterpolatingProver;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.MathsatSymbolicFormulaManager;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.MathsatTheoremProver;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.SimplifyTheoremProver;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.YicesTheoremProver;
import cpaplugin.logging.CustomLogLevel;
import cpaplugin.logging.LazyLogger;


/**
 * CPA for interpolation-based lazy abstraction.
 *
 * This is an abstract class. See in the 'explicit/' and 'symbolic/'
 * subpackages for implementations.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public abstract class ItpCPA implements ConfigurableProgramAnalysis {
    
    protected ItpAbstractDomain domain;
    protected ItpMergeOperator merge;
    protected ItpStopOperator stop;
    protected ItpTransferRelation trans;
    protected MathsatSymbolicFormulaManager mgr;
    protected ItpCounterexampleRefiner refiner;
    
    // covering relation
    protected Map<ItpAbstractElement,
                Set<ItpAbstractElement>> covers;
    
    protected ItpCPA() {
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
            System.err.println("Unknown solver: " + whichProver);
            assert(false);
            System.exit(1);
        }
        InterpolatingTheoremProver itpProver = 
            new MathsatInterpolatingProver(mgr, true);
        ExplicitAbstractFormulaManager amgr = 
            new BDDMathsatExplicitAbstractManager(thmProver, itpProver);        

        domain = new ItpAbstractDomain(this);
        merge = new ItpMergeOperator(domain);
        stop = new ItpStopOperator(domain, thmProver);
        trans = new ItpTransferRelation(domain);
                
        refiner = new ItpCounterexampleRefiner(amgr, itpProver);
        
        covers = new HashMap<ItpAbstractElement, 
                             Set<ItpAbstractElement>>();
        
        if (CPAMain.cpaConfig.getBooleanValue("analysis.bfs")) {
            // this analysis only works with dfs traversal
            System.out.println(
                    "ERROR: Interpolation-based analysis works only with DFS " +
                    "traversal!");
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

    @Override
    public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
        LazyLogger.log(CustomLogLevel.SpecificCPALevel, 
                       "Getting initial element from node: ", node);
        
        ItpAbstractElement e = getElementCreator().create(node);
        e.setAbstraction(mgr.makeTrue());
        e.setContext(new Stack<Pair<SymbolicFormula, CFANode>>(), true);
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
