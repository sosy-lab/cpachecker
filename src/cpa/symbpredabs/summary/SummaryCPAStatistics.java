package cpa.symbpredabs.summary;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import cmdline.CPAMain;

import cfa.objectmodel.CFANode;

import cpaplugin.CPAStatistics;
import cpaplugin.MainCPAStatistics;
import cpa.symbpredabs.Pair;
import cpa.symbpredabs.Predicate;
import cpa.symbpredabs.PredicateMap;
import cpa.symbpredabs.mathsat.BDDPredicate;
import cpa.symbpredabs.mathsat.MathsatSymbolicFormula;
import cpa.symbpredabs.mathsat.summary.BDDMathsatSummaryAbstractManager;

/**
 * Statistics for symbolic lazy abstraction with summaries
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SummaryCPAStatistics implements CPAStatistics {
    
    private SummaryCPA cpa;
    
    public SummaryCPAStatistics(SummaryCPA cpa) {
        this.cpa = cpa;
    }

    @Override
    public String getName() {
        return "Symbolic Predicate Abstraction with Summaries";
    }

    @Override
    public void printStatistics(PrintWriter out) {
        SummaryTransferRelation trans = 
            (SummaryTransferRelation)cpa.getTransferRelation();
        PredicateMap pmap = cpa.getPredicateMap();
        BDDMathsatSummaryAbstractManager amgr = 
            (BDDMathsatSummaryAbstractManager)cpa.getAbstractFormulaManager();
        
        Set<Predicate> allPreds = new HashSet<Predicate>();
        Collection<CFANode> allLocs = null;
        Collection<String> allFuncs = null;
        int maxPreds = 0;
        int totPreds = 0;
        int avgPreds = 0;
        if (!CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.refinement.addPredicatesGlobally")) {
            allLocs = pmap.getKnownLocations();
            for (CFANode l : allLocs) {
                Collection<Predicate> p = pmap.getRelevantPredicates(l);
                maxPreds = Math.max(maxPreds, p.size());
                totPreds += p.size();
                allPreds.addAll(p);
            }
            avgPreds = allLocs.size() > 0 ? totPreds/allLocs.size() : 0;
        } else {
            allFuncs = pmap.getKnownFunctions();
            for (String s : allFuncs) {
                Collection<Predicate> p = pmap.getRelevantPredicates(s);
                maxPreds = Math.max(maxPreds, p.size());
                totPreds += p.size();
                allPreds.addAll(p);
            }
            avgPreds = allFuncs.size() > 0 ? totPreds/allFuncs.size() : 0;
        }
        
        // check if/where to dump the predicate map
        int errorReached = CPAMain.cpaStats.getErrorReached();
        if (errorReached == MainCPAStatistics.ERROR_NOT_REACHED) {
            String pth = CPAMain.cpaConfig.getProperty(
                    "cpas.symbpredabs.refinement.finalPredMapPath", "");
            if (!pth.equals("")) {
                File f = new File(pth);
                try {
                    PrintWriter pw = new PrintWriter(f);
                    pw.println("ALL PREDICATES:");
                    for (Predicate p : allPreds) {
                        Pair<MathsatSymbolicFormula, MathsatSymbolicFormula> d =
                            amgr.getPredicateNameAndDef((BDDPredicate)p);
                        pw.format("%s ==> %s <-> %s\n", p, d.getFirst(),
                                d.getSecond());
                    }
                    if (!CPAMain.cpaConfig.getBooleanValue(
                            "cpas.symbpredabs.refinement." +
                            "addPredicatesGlobally")) {
                        pw.println("\nFOR EACH LOCATION:");
                        for (CFANode l : allLocs) {
                            Collection<Predicate> c = 
                                pmap.getRelevantPredicates(l);
                            pw.println("LOCATION: " + l);
                            for (Predicate p : c) {
                                pw.println(p);
                            }
                            pw.println("");
                        }
                    }
                    pw.close();
                } catch (FileNotFoundException e) {
                    // just issue a warning to the user
                    out.println("WARNING: impossible to dump predicate map on `"
                                + pth + "'");
                }
            }
        }
        
        BDDMathsatSummaryAbstractManager.Stats bs = amgr.getStats();

        out.println("Number of abstract states visited: " + 
                trans.getNumAbstractStates());
        out.println("Number of abstraction steps: " + bs.numCallsAbstraction +
                " (" + bs.numCallsAbstractionCached + " cached)");
        out.println("Number of refinement steps: " + bs.numCallsCexAnalysis);
        out.println("Number of coverage checks: " + bs.numCoverageChecks);
        out.println("");
        out.println("Total number of predicates discovered: " + 
                allPreds.size());
        out.println("Average number of predicates per location: " + avgPreds);
        out.println("Max number of predicates per location: " + maxPreds);
        out.println("");
        out.println("Total time for abstraction computation: " + 
                toTime(bs.abstractionMathsatTime + bs.abstractionBddTime));
        out.println("  Time for All-SMT: ");
        out.println("    Total:             " + 
                toTime(bs.abstractionMathsatTime)); 
        out.println("    Max:               " + 
                toTime(bs.abstractionMaxMathsatTime));
        out.println("    Solving time only: " + 
                toTime(bs.abstractionMathsatSolveTime));
        out.println("  Time for BDD construction: ");
        out.println("    Total:             " + toTime(bs.abstractionBddTime)); 
        out.println("    Max:               " + 
                toTime(bs.abstractionMaxBddTime));
        out.println("  Time for coverage check: ");
        out.println("    Total:             " + 
                toTime(bs.bddCoverageCheckTime)); 
        out.println("    Max:               " + 
                toTime(bs.bddCoverageCheckMaxTime));
        out.println(
                "Time for counterexample analysis/abstraction refinement: ");
        out.println("  Total:               " + toTime(bs.cexAnalysisTime)); 
        out.println("  Max:                 " + toTime(bs.cexAnalysisMaxTime));
        out.println("  Solving time only:   " + 
                toTime(bs.cexAnalysisMathsatTime));
        if (CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.explicit.getUsefulBlocks")) {
            out.println("  Cex.focusing total:  " + 
                    toTime(bs.cexAnalysisGetUsefulBlocksTime));
            out.println("  Cex.focusing max:    " +
                    toTime(bs.cexAnalysisGetUsefulBlocksMaxTime));
        }
        out.println("");
        out.print("Error location(s) reached? ");
        switch (errorReached) { 
        case MainCPAStatistics.ERROR_UNKNOWN:
            out.println("UNKNOWN, analysis has not completed");
            break;
        case MainCPAStatistics.ERROR_REACHED:
            out.println("YES, there is a BUG!");
            break;
        case MainCPAStatistics.ERROR_NOT_REACHED:
            out.println("NO, the system is safe");            
        }
    }
    
    private String toTime(long timeMillis) {
//        return String.format("%02dh:%02dm:%02d.%03ds",
//                timeMillis / (1000 * 60 * 60),  
//                timeMillis / (1000 * 60), 
//                timeMillis / 1000, 
//                timeMillis % 1000);
        return String.format("% 5d.%03ds", timeMillis/1000, timeMillis%1000);
    }

}
