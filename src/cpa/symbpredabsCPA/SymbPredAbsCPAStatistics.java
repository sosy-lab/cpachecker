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
package cpa.symbpredabsCPA;

import java.io.PrintWriter;

import cmdline.CPAMain;
import cmdline.CPAMain.Result;
import cpa.common.ReachedElements;
import cpa.common.interfaces.CPAStatistics;

public class SymbPredAbsCPAStatistics implements CPAStatistics {

    private final SymbPredAbsCPA cpa;

    public SymbPredAbsCPAStatistics(SymbPredAbsCPA cpa) {
      this.cpa = cpa;
    }

    @Override
    public String getName() {
      return "Summary Predicate Abstraction CPA";
    }

    @Override
    public void printStatistics(PrintWriter out, Result result, ReachedElements reached) {
      MathsatSymbPredAbsFormulaManager<?> amgr =
        (MathsatSymbPredAbsFormulaManager<?>)cpa.getFormulaManager();

/* TODO re-enable this with precision based approach      
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
      if (result == Result.SAFE) {
        String outfilePath = CPAMain.cpaConfig.getProperty("output.path");
        String outfileName = CPAMain.cpaConfig.getProperty(
            "cpas.symbpredabs.refinement.finalPredMapFile", "");
        if (outfileName == null) {
          outfileName = "predmap.txt";
        }
        if (!outfileName.equals("")) {
          File f = new File(outfilePath + outfileName);
          try {
            PrintWriter pw = new PrintWriter(f);
            pw.println("ALL PREDICATES:");
            for (Predicate p : allPreds) {
              Pair<? extends SymbolicFormula, ? extends SymbolicFormula> d =
                amgr.getPredicateVarAndAtom(p);
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
                + outfilePath + outfileName + "'");
          }
        }
      }
*/
      MathsatSymbPredAbsFormulaManager.Stats bs = amgr.stats;
      SymbPredAbsTransferRelation trans = cpa.getTransferRelation();

      out.println("Number of abstraction steps:       " + bs.numCallsAbstraction + " (" + bs.numCallsAbstractionCached + " cached)");
      out.println("Max LBE block size:                " + trans.maxBlockSize);
      out.println("Number of abstract states reached: " + trans.numAbstractStates);
      out.println("Number of refinement steps:        " + bs.numCallsCexAnalysis);
      out.println("Number of coverage checks:         " + bs.numCoverageChecks);
      out.println();
//      out.println("Total number of predicates discovered: " + allPreds.size());
//      out.println("Avg number of predicates per location: " + avgPreds);
//      out.println("Max number of predicates per location: " + maxPreds);
      out.println();
      out.println("Time for merge:                " + toTime(cpa.getMergeOperator().totalMergeTime));
      out.println("Time for abstraction post:     " + toTime(trans.abstractionTime));
      out.println("  initial abstraction formula: " + toTime(trans.initAbstractionFormulaTime));
      out.println("  computing abstraction:       " + toTime(trans.computingAbstractionTime));
      out.println("    Time for All-SMT: ");
      out.println("      Total:                   " + toTime(bs.abstractionMathsatTime));
      out.println("      Max:                     " + toTime(bs.abstractionMaxMathsatTime));
      out.println("      Solving time only:       " + toTime(bs.abstractionMathsatSolveTime));
      out.println("    Time for BDD construction: ");
      out.println("      Total:                   " + toTime(bs.abstractionBddTime));
      out.println("      Max:                     " + toTime(bs.abstractionMaxBddTime));
      out.println("    Time for coverage check: ");
      out.println("      Total:                   " + toTime(bs.bddCoverageCheckTime));
      out.println("      Max:                     " + toTime(bs.bddCoverageCheckMaxTime));
      out.println("Time for non-abstraction post: " + toTime(trans.nonAbstractionTime));
      out.println("Time for finding path formula: " + toTime(trans.pathFormulaTime));
      out.println("  actual computation of PF:    " + toTime(trans.pathFormulaComputationTime));
      out.println("Time for counterexample analysis/abstraction refinement: ");
      out.println("  Total:                       " + toTime(bs.cexAnalysisTime));
      out.println("  Max:                         " + toTime(bs.cexAnalysisMaxTime));
      out.println("  Solving time only:           " + toTime(bs.cexAnalysisMathsatTime));
      if (CPAMain.cpaConfig.getBooleanValue("cpas.symbpredabs.explicit.getUsefulBlocks")) {
        out.println("  Cex.focusing total:          " + toTime(bs.cexAnalysisGetUsefulBlocksTime));
        out.println("  Cex.focusing max:            " + toTime(bs.cexAnalysisGetUsefulBlocksMaxTime));
      }
    }

    private String toTime(long timeMillis) {
      return String.format("% 5d.%03ds", timeMillis/1000, timeMillis%1000);
    }
}