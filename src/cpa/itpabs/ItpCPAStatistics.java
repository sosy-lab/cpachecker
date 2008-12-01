package cpa.itpabs;

import java.io.PrintWriter;

import cmdline.CPAMain;

import cpaplugin.CPAStatistics;
import cpaplugin.MainCPAStatistics;
import cpa.itpabs.ItpCPA;
import cpa.itpabs.ItpCounterexampleRefiner;
import cpa.itpabs.ItpStopOperator;
import cpa.itpabs.ItpTransferRelation;

/**
 * Statistics for interpolation-based lazy abstraction
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ItpCPAStatistics implements CPAStatistics {

    private ItpCPA cpa;
    private String name;

    public ItpCPAStatistics(ItpCPA cpa, String name) {
        this.cpa = cpa;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void printStatistics(PrintWriter out) {
        ItpTransferRelation trans =
            (ItpTransferRelation)cpa.getTransferRelation();

        ItpCounterexampleRefiner refiner = cpa.getRefiner();

        ItpCounterexampleRefiner.Stats bs = refiner.getStats();
        ItpStopOperator stop = (ItpStopOperator)cpa.getStopOperator();

        out.println("Number of abstract states visited: " +
                trans.getNumAbstractStates());
        out.println("Number of refinement steps: " + bs.numCallsCexAnalysis);
        out.println("");
        out.println("Number of covered states:  " + stop.numCoveredStates);
        out.println("Number of coverage checks: " + stop.numCoverageChecks +
                " (" + stop.numCachedCoverageChecks + " cached)");
        out.println("Number of forced covered states:  " +
                trans.forcedCoverStats.numForcedCoveredElements);
        out.println("Number of forced coverage checks: " +
                trans.forcedCoverStats.numForcedCoverChecks + " (" +
                trans.forcedCoverStats.numForcedCoverChecksCached + " cached)");
        out.println("Time for coverage check: " +
                toTime(stop.coverageCheckTime));
        out.println("Time for forced coverage checks: ");
        out.println("  Total:                 " + toTime(bs.forceCoverTime));
        out.println("  Max:                   " + toTime(bs.forceCoverMaxTime));
        out.println("");
        out.println(
                "Time for counterexample analysis/abstraction refinement: ");
        out.println("  Total:                 " + toTime(bs.cexAnalysisTime));
        out.println("  Max:                   " +
                toTime(bs.cexAnalysisMaxTime));
        out.println("  Solving time only:     " +
                toTime(bs.cexAnalysisMathsatTime));
        out.println("");
        int errorReached = CPAMain.cpaStats.getErrorReached();
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
        return String.format("% 5d.%03ds", timeMillis/1000, timeMillis%1000);
    }

}
