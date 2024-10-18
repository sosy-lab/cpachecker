/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.relyguarantee;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.WrapperPrecision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefiner;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.CachingPathFormulaManager;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

@Options(prefix="cpa.relyguarantee.predmap")
public class  RelyGuaranteeCPAStatistics implements Statistics {

    @Option(description="export final predicate map, if the error location is not reached")
    private boolean export = true;

    @Option(type=Option.Type.OUTPUT_FILE,
        description="export final predicate map, if the error location is not reached")
    private File file = new File("predmap.txt");

    private final RelyGuaranteeCPA cpa;
    private PredicateRefiner refiner = null;

    public RelyGuaranteeCPAStatistics(RelyGuaranteeCPA cpa) throws InvalidConfigurationException {
      this.cpa = cpa;
      cpa.getConfiguration().inject(this, RelyGuaranteeCPAStatistics.class);
    }

    void addRefiner(PredicateRefiner ref) {
      refiner = ref;
    }

    @Override
    public String getName() {
      return "RelyGuaranteeCPA";
    }

    @Override
    public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
      RelyGuaranteeRefinementManager<?, ?> amgr = cpa.refinerManager;

      Multimap<CFANode, AbstractionPredicate> predicates = HashMultimap.create();

      for (Precision precision : reached.getPrecisions()) {
        if (precision instanceof WrapperPrecision) {
          PredicatePrecision preds = ((WrapperPrecision)precision).retrieveWrappedPrecision(PredicatePrecision.class);
          predicates.putAll(preds.getPredicateMap());
        }
      }

      // check if/where to dump the predicate map
      if ((result != Result.UNSAFE) && export && file != null) {
        TreeMap<CFANode, Collection<AbstractionPredicate>> sortedPredicates
              = new TreeMap<CFANode, Collection<AbstractionPredicate>>(predicates.asMap());
        StringBuilder sb = new StringBuilder();

        for (Entry<CFANode, Collection<AbstractionPredicate>> e : sortedPredicates.entrySet()) {
          sb.append("LOCATION: ");
          sb.append(e.getKey());
          sb.append('\n');
          Joiner.on('\n').appendTo(sb, e.getValue());
          sb.append("\n\n");
        }

        try {
          Files.writeFile(file, sb);
        } catch (IOException e) {
          cpa.getLogger().log(Level.WARNING, "Could not write predicate map to file ", file,
              (e.getMessage() != null ? "(" + e.getMessage() + ")" : ""));
        }
      }

      int maxPredsPerLocation = 0;
      for (Collection<AbstractionPredicate> p : predicates.asMap().values()) {
        maxPredsPerLocation = Math.max(maxPredsPerLocation, p.size());
      }

      int allLocs = predicates.keySet().size();
      int totPredsUsed = predicates.size();
      int avgPredsPerLocation = allLocs > 0 ? totPredsUsed/allLocs : 0;
      int allDistinctPreds = (new HashSet<AbstractionPredicate>(predicates.values())).size();

      RelyGuaranteeRefinementManager.PredStats as = amgr.stats;
      RelyGuaranteeRefinementManager.RefStats bs = amgr.refStats;
      RelyGuaranteeAbstractDomain domain = cpa.domain;
      RelyGuaranteeTransferRelation trans = cpa.transfer;
      RelyGuaranteePrecisionAdjustment prec = cpa.prec;

      CachingPathFormulaManager pfMgr = null;
      if (cpa.pathFormulaManager instanceof CachingPathFormulaManager) {
        pfMgr = (CachingPathFormulaManager)cpa.pathFormulaManager;
      }

      out.println("Number of abstractions:            " + prec.numAbstractions + " (" + toPercent(prec.numAbstractions, trans.postTimer.getNumberOfIntervals()) + " of all post computations)");
      if (prec.numAbstractions > 0) {
        out.println("  Because of function entry/exit:  " + trans.numBlkFunctions + " (" + toPercent(trans.numBlkFunctions, prec.numAbstractions) + ")");
        out.println("  Because of loop head:            " + trans.numBlkLoops + " (" + toPercent(trans.numBlkLoops, prec.numAbstractions) + ")");
        out.println("  Because of threshold:            " + trans.numBlkThreshold + " (" + toPercent(trans.numBlkThreshold, prec.numAbstractions) + ")");
        out.println("  Times precision was empty:       " + as.numSymbolicAbstractions + " (" + toPercent(as.numSymbolicAbstractions, as.numCallsAbstraction) + ")");
        out.println("  Times precision was {false}:     " + as.numSatCheckAbstractions + " (" + toPercent(as.numSatCheckAbstractions, as.numCallsAbstraction) + ")");
        out.println("  Times result was 'false':        " + prec.numAbstractionsFalse + " (" + toPercent(prec.numAbstractionsFalse, prec.numAbstractions) + ")");
        out.println("  Extract predicates timer: " + as.extractTimer);
      }
      if (trans.satCheckTimer.getNumberOfIntervals() > 0) {
        out.println("Number of satisfiability checks:   " + trans.satCheckTimer.getNumberOfIntervals());
        out.println("  Times result was 'false':        " + trans.numSatChecksFalse + " (" + toPercent(trans.numSatChecksFalse, trans.satCheckTimer.getNumberOfIntervals()) + ")");
      }
      out.println("Number of strengthen sat checks:   " + trans.strengthenCheckTimer.getNumberOfIntervals());
      if (trans.strengthenCheckTimer.getNumberOfIntervals() > 0) {
        out.println("  Times result was 'false':        " + trans.numStrengthenChecksFalse + " (" + toPercent(trans.numStrengthenChecksFalse, trans.strengthenCheckTimer.getNumberOfIntervals()) + ")");
      }
      out.println("Number of coverage checks:         " + domain.coverageCheckTimer.getNumberOfIntervals());
      out.println("  BDD entailment checks:           " + domain.bddCoverageCheckTimer.getNumberOfIntervals());
      out.println("  Symbolic coverage check:         " + domain.symbolicCoverageCheckTimer.getNumberOfIntervals());
      out.println();
      out.println("Max ABE block size:                       " + prec.maxBlockSize);
      out.println("Number of predicates discovered:          " + allDistinctPreds);
      out.println("Number of abstraction locations:          " + allLocs);
      out.println("Max number of predicates per location:    " + maxPredsPerLocation);
      out.println("Avg number of predicates per location:    " + avgPredsPerLocation);
      out.println("Max number of predicates per abstraction: " + prec.maxPredsPerAbstraction);
      out.println("Total number of models for allsat:        " + as.allSatCount);
      out.println("Max number of models for allsat:          " + as.maxAllSatCount);
      if (as.numCallsAbstraction > 0) {
        out.println("Avg number of models for allsat:          " + as.allSatCount / as.numCallsAbstraction);
      }
      out.println();
      if (pfMgr != null) {
        out.println("Number of path formula cache hits:   " + pfMgr.pathFormulaCacheHits + " (" + toPercent(pfMgr.pathFormulaCacheHits, pfMgr.pathFormulaComputationTimer.getNumberOfIntervals()) + ")");
      }
      if (as.numCallsAbstraction > 0) {
        out.println("Number of abstraction cache hits:    " + as.numCallsAbstractionCached + " (" + toPercent(as.numCallsAbstractionCached, as.numCallsAbstraction) + ")");
      }
      out.println();
      out.println("Time for post operator:              " + trans.postTimer);
      out.println("  Time for path formula creation:    " + trans.pathFormulaTimer);
      if (pfMgr != null) {
        out.println("    Actual computation:              " + pfMgr.pathFormulaComputationTimer);
      }
      if (trans.satCheckTimer.getNumberOfIntervals() > 0) {
        out.println("  Time for satisfiability checks:    " + trans.satCheckTimer);
      }
      out.println("Time for strengthen operator:        " + trans.strengthenTimer);
      out.println("  Time for satisfiability checks:    " + trans.strengthenCheckTimer);
      out.println("Time for prec operator:             " + prec.totalPrecTime);
      out.println("  Time for abstraction:              " + prec.computingAbstractionTime + " (Max: " + prec.computingAbstractionTime.printMaxTime() + ", Count: " + prec.computingAbstractionTime.getNumberOfIntervals() + ")");
      out.println("    Solving time:                    " + as.abstractionTime.printOuterSumTime() + " (Max: " + as.abstractionTime.printOuterMaxTime() + ")");
      out.println("    Time for BDD construction:       " + as.abstractionTime.printInnerSumTime()   + " (Max: " + as.abstractionTime.printInnerMaxTime() + ")");
      out.println("Time for merge operator:             " + cpa.merge.totalMergeTime);
      out.println("Time for coverage check:             " + domain.coverageCheckTimer);
      if (domain.bddCoverageCheckTimer.getNumberOfIntervals() > 0) {
        out.println("  Time for BDD entailment checks:    " + domain.bddCoverageCheckTimer);
      }
      if (domain.symbolicCoverageCheckTimer.getNumberOfIntervals() > 0) {
        out.println("  Time for symbolic coverage checks: " + domain.bddCoverageCheckTimer);
      }
      if (refiner != null && refiner.totalRefinement.getSumTime() > 0) {
        out.println("Time for refinement:                 " + refiner.totalRefinement);
        out.println("  Counterexample analysis:           " + bs.cexAnalysisTimer + " (Max: " + bs.cexAnalysisTimer.printMaxTime() + ", Calls: " + bs.cexAnalysisTimer.getNumberOfIntervals() + ")");
        if (bs.cexAnalysisGetUsefulBlocksTimer.getMaxTime() != 0) {
          out.println("    Cex.focusing:                    " + bs.cexAnalysisGetUsefulBlocksTimer + " (Max: " + bs.cexAnalysisGetUsefulBlocksTimer.printMaxTime() + ")");
        }
        out.println("    Solving time only:               " + bs.cexAnalysisSolverTimer + " (Max: " + bs.cexAnalysisSolverTimer.printMaxTime() + ", Calls: " + bs.cexAnalysisSolverTimer.getNumberOfIntervals() + ")");
        out.println("  Precision update:                  " + refiner.precisionUpdate);
        out.println("  ART update:                        " + refiner.artUpdate);
        out.println("  Error path post-processing:        " + refiner.errorPathProcessing);
      }
    }

    private String toPercent(double val, double full) {
      return String.format("%1.0f", val/full*100) + "%";
    }
}