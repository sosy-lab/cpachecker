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
 * STILL ON-GOING, NOT FINISHED, AND CURRENTLY BROKEN
 * 
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
