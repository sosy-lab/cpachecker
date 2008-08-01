package cpaplugin.cpa.cpas.symbpredabs.mathsat.summary;

import java.util.Collection;
import java.util.List;
import java.util.Vector;

import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cpa.cpas.symbpredabs.AbstractFormula;
import cpaplugin.cpa.cpas.symbpredabs.CounterexampleTraceInfo;
import cpaplugin.cpa.cpas.symbpredabs.Pair;
import cpaplugin.cpa.cpas.symbpredabs.Predicate;
import cpaplugin.cpa.cpas.symbpredabs.SSAMap;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormula;
import cpaplugin.cpa.cpas.symbpredabs.logging.LazyLogger;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.BDDAbstractFormula;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.BDDMathsatAbstractFormulaManager;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.MathsatSymbolicFormula;
import cpaplugin.cpa.cpas.symbpredabs.summary.InnerCFANode;
import cpaplugin.cpa.cpas.symbpredabs.summary.SummaryAbstractElement;
import cpaplugin.cpa.cpas.symbpredabs.summary.SummaryAbstractFormulaManager;
import cpaplugin.cpa.cpas.symbpredabs.summary.SummaryCFANode;
import cpaplugin.cpa.cpas.symbpredabs.summary.SummaryFormulaManager;
import cpaplugin.logging.CPACheckerLogger;

public class BDDMathsatSummaryAbstractManager extends
        BDDMathsatAbstractFormulaManager implements
        SummaryAbstractFormulaManager {

    public BDDMathsatSummaryAbstractManager() {
        super();
    }

    public AbstractFormula buildAbstraction(SummaryFormulaManager mgr,
            SummaryAbstractElement e, SummaryAbstractElement succ, 
            Collection<Predicate> predicates) {
        MathsatSummaryFormulaManager mmgr = (MathsatSummaryFormulaManager)mgr;
        
        long absEnv = mathsat.api.msat_create_env();
        long msatEnv = mmgr.getMsatEnv();
        
        // first, get all the paths in e that lead to succ
        Collection<Pair<SymbolicFormula, SSAMap>> relevantPaths = 
            new Vector<Pair<SymbolicFormula, SSAMap>>();
        for (CFANode leaf : e.getLeaves()) {
            for (int i = 0; i < leaf.getNumLeavingEdges(); ++i) {
                CFAEdge edge = leaf.getLeavingEdge(i);
                InnerCFANode s = (InnerCFANode)edge.getSuccessor();
                if (s.getSummaryNode().equals(succ.getLocation())) {
                    // ok, this path is relevant
                    relevantPaths.add(e.getPathFormula(leaf));

                    LazyLogger.log(LazyLogger.DEBUG_1,
                                   "FOUND RELEVANT PATH, leaf: ", 
                                   leaf.getNodeNumber());
                    LazyLogger.log(LazyLogger.DEBUG_3,
                                   "Formula: ", 
                                   e.getPathFormula(leaf).getFirst());
                }
            }
        }
        // now, we want to create a new formula that is the OR of all the 
        // possible paths. So we merge the SSA maps and OR the formulas
        SSAMap ssa = new SSAMap();
        SymbolicFormula f = mgr.makeFalse();
        for (Pair<SymbolicFormula, SSAMap> p : relevantPaths) {
            Pair<Pair<SymbolicFormula, SymbolicFormula>, SSAMap> mp = 
                mgr.mergeSSAMaps(ssa, p.getSecond(), false);
            SymbolicFormula curf = p.getFirst();
            f = mgr.makeAnd(f, mp.getFirst().getFirst());
            curf = mgr.makeAnd(curf, mp.getFirst().getSecond());
            f = mgr.makeOr(f, curf);
            ssa = mp.getSecond();
        }
        
        long term = mathsat.api.msat_make_copy_from(
                absEnv, ((MathsatSymbolicFormula)f).getTerm(), msatEnv);
        assert(!mathsat.api.MSAT_ERROR_TERM(term));
        
        
        // build the definition of the predicates, and instantiate them
        Pair<Long, long[]> predlist = buildPredList(mmgr, predicates);
        long preddef = predlist.getFirst();
        long[] important = predlist.getSecond();
        for (int i = 0; i < important.length; ++i) {
            important[i] = mathsat.api.msat_make_copy_from(
                    absEnv, important[i], msatEnv); 
        }
        
        if (CPACheckerLogger.getLevel() <= LazyLogger.DEBUG_3.intValue()) {
            StringBuffer importantStrBuf = new StringBuffer();
            for (long t : important) {
                importantStrBuf.append(mathsat.api.msat_term_repr(t));
                importantStrBuf.append(" ");
            }
            LazyLogger.log(LazyLogger.DEBUG_3,
                           "IMPORTANT SYMBOLS: ", importantStrBuf);
        }
        
        // first, create the new formula corresponding to 
        // (f & edges from e to succ)
        // TODO - at the moment, we assume that all the edges connecting e and
        // succ have no statement or assertion attached (i.e. they are just
        // return edges or gotos). This might need to change in the future!!
        // (So, for now we don't need to to anything...)         
        
        // instantiate the definitions with the right SSA
        MathsatSymbolicFormula inst = (MathsatSymbolicFormula)mmgr.instantiate(
                new MathsatSymbolicFormula(preddef), ssa);
        preddef = mathsat.api.msat_make_copy_from(absEnv, inst.getTerm(), 
                                                  msatEnv);
        // build the concrete representation of the abstract formula of e
        AbstractFormula abs = e.getAbstraction();
        MathsatSymbolicFormula fabs = 
            (MathsatSymbolicFormula)mmgr.instantiate(
                    toConcrete(mmgr, abs), null);
        long curstate = mathsat.api.msat_make_copy_from(absEnv, fabs.getTerm(),
                                                        msatEnv);
        
        // the formula is (curstate & term & preddef)
        // build the formula and send it to the absEnv
        long formula = mathsat.api.msat_make_and(absEnv, 
                mathsat.api.msat_make_and(absEnv, curstate, term), preddef);
        mathsat.api.msat_add_theory(absEnv, mathsat.api.MSAT_UF);
        mathsat.api.msat_add_theory(absEnv, mathsat.api.MSAT_LRA);
        mathsat.api.msat_set_theory_combination(absEnv, 
                                                mathsat.api.MSAT_COMB_DTC);
        mathsat.api.msat_assert_formula(absEnv, formula);

        LazyLogger.log(LazyLogger.DEBUG_3, "COMPUTING ALL-SMT ON FORMULA: ",
                       new MathsatSymbolicFormula(formula));

        int absbdd = bddManager.getZero();
        bddManager.ref(absbdd);
        AllSatCallback func = new AllSatCallback(absbdd, msatEnv, absEnv);
        int numModels = mathsat.api.msat_all_sat(absEnv, important, func);
        assert(numModels != -1);
        
        mathsat.api.msat_destroy_env(absEnv);

        if (numModels == -2) {
            bddManager.deref(absbdd);
            absbdd = bddManager.getOne();
            bddManager.ref(absbdd);
            return new BDDAbstractFormula(absbdd);
        } else {
            return new BDDAbstractFormula(func.getBDD());
        }
    }
    
    public CounterexampleTraceInfo buildCounterexampleTrace(
            SummaryFormulaManager mgr, List<SummaryCFANode> abstractTrace) {
        // TODO Auto-generated method stub
        return null;
    }
}
