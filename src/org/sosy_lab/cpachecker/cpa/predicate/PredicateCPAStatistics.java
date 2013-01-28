/*
 *  CPAchecker is a tool for configurable software verification.
 *  This predmapFile is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this predmapFile except in compliance with the License.
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
package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.*;
import static org.sosy_lab.cpachecker.util.StatisticsUtils.*;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.WrapperPrecision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.CachingPathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Supplier;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

@Options(prefix="cpa.predicate")
class PredicateCPAStatistics implements Statistics {

    @Option(description="export final predicate map",
            name="predmap.export")
    private boolean exportPredmap = true;

    @Option(description="file for exporting final predicate map",
            name="predmap.file")
    @FileOption(FileOption.Type.OUTPUT_FILE)
    private Path predmapFile = Paths.get("predmap.txt");

    @Option(description="export final loop invariants",
            name="invariants.export")
    private boolean exportInvariants = true;

    @Option(description="file for exporting final loop invariants",
            name="invariants.file")
    @FileOption(FileOption.Type.OUTPUT_FILE)
    private Path invariantsFile = Paths.get("invariants.txt");

    private final PredicateCPA cpa;
    private final BlockOperator blk;
    private final RegionManager rmgr;
    private final CFA cfa;

    public PredicateCPAStatistics(PredicateCPA pCpa, BlockOperator pBlk,
        RegionManager pRmgr, CFA pCfa) throws InvalidConfigurationException {
      cpa = pCpa;
      blk = pBlk;
      rmgr = pRmgr;
      cfa = pCfa;
      cpa.getConfiguration().inject(this, PredicateCPAStatistics.class);
    }

    @Override
    public String getName() {
      return "PredicateCPA";
    }

    @Override
    public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
      PredicateAbstractionManager amgr = cpa.getPredicateManager();

      Supplier<Set<AbstractionPredicate>> hashSetSupplier = new Supplier<Set<AbstractionPredicate>>() {
        @Override
        public Set<AbstractionPredicate> get() {
          return Sets.newHashSet();
        }
      };

      SetMultimap<CFANode, AbstractionPredicate> localPredicates = Multimaps.newSetMultimap(new TreeMap<CFANode, Collection<AbstractionPredicate>>(), hashSetSupplier);
      SetMultimap<String, AbstractionPredicate> functionPredicates = Multimaps.newSetMultimap(new TreeMap<String, Collection<AbstractionPredicate>>(), hashSetSupplier);
      Set<AbstractionPredicate> globalPredicates = Sets.newHashSet();

      for (Precision precision : reached.getPrecisions()) {
        if (precision instanceof WrapperPrecision) {
          PredicatePrecision preds = ((WrapperPrecision)precision).retrieveWrappedPrecision(PredicatePrecision.class);
          localPredicates.putAll(preds.getLocalPredicates());
          functionPredicates.putAll(preds.getFunctionPredicates());
          globalPredicates.addAll(preds.getGlobalPredicates());
        }
      }

      Set<AbstractionPredicate> allPredicates = Sets.newHashSet(globalPredicates);
      allPredicates.addAll(localPredicates.values());
      allPredicates.addAll(functionPredicates.values());

      // check if/where to dump the predicate map
      if (exportPredmap && predmapFile != null) {
        try (Writer w = Files.openOutputFile(predmapFile)) {
          PredicateMapWriter writer = new PredicateMapWriter(cpa);
          writer.writePredicateMap(localPredicates, functionPredicates, globalPredicates, allPredicates, w);
        } catch (IOException e) {
          cpa.getLogger().logUserException(Level.WARNING, e, "Could not write predicate map to file");
        }
      }

      int maxPredsPerLocation = 0;
      for (Collection<AbstractionPredicate> p : localPredicates.asMap().values()) {
        maxPredsPerLocation = Math.max(maxPredsPerLocation, p.size());
      }

      int allLocs = localPredicates.keySet().size();
      int totPredsUsed = localPredicates.size();
      int avgPredsPerLocation = allLocs > 0 ? totPredsUsed/allLocs : 0;

      int allDistinctPreds = allPredicates.size();

      if (result == Result.SAFE && exportInvariants && invariantsFile != null) {
        exportInvariants(reached);
      }

      PredicateAbstractionManager.Stats as = amgr.stats;
      PredicateAbstractDomain domain = cpa.getAbstractDomain();
      PredicateTransferRelation trans = cpa.getTransferRelation();
      PredicatePrecisionAdjustment prec = cpa.getPrecisionAdjustment();
      Solver solver = cpa.getSolver();

      CachingPathFormulaManager pfMgr = null;
      if (cpa.getPathFormulaManager() instanceof CachingPathFormulaManager) {
        pfMgr = (CachingPathFormulaManager)cpa.getPathFormulaManager();
      }

      out.println("Number of abstractions:            " + prec.numAbstractions + " (" + toPercent(prec.numAbstractions, trans.postTimer.getNumberOfIntervals()) + " of all post computations)");
      if (prec.numAbstractions > 0) {
        out.println("  Because of function entry/exit:  " + blk.numBlkFunctions + " (" + toPercent(blk.numBlkFunctions, prec.numAbstractions) + ")");
        out.println("  Because of loop head:            " + blk.numBlkLoops + " (" + toPercent(blk.numBlkLoops, prec.numAbstractions) + ")");
        out.println("  Because of threshold:            " + blk.numBlkThreshold + " (" + toPercent(blk.numBlkThreshold, prec.numAbstractions) + ")");
        out.println("  Times precision was empty:       " + as.numSymbolicAbstractions + " (" + toPercent(as.numSymbolicAbstractions, as.numCallsAbstraction) + ")");
        out.println("  Times precision was {false}:     " + as.numSatCheckAbstractions + " (" + toPercent(as.numSatCheckAbstractions, as.numCallsAbstraction) + ")");
        out.println("  Times result was 'false':        " + prec.numAbstractionsFalse + " (" + toPercent(prec.numAbstractionsFalse, prec.numAbstractions) + ")");
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
      if (domain.symbolicCoverageCheckTimer.getNumberOfIntervals() > 0) {
        out.println("  Symbolic coverage check:         " + domain.symbolicCoverageCheckTimer.getNumberOfIntervals());
      }
      out.println("Number of implication checks:      " + solver.implicationChecks);
      out.println("  trivial:                         " + solver.trivialImplicationChecks);
      out.println("  cached:                          " + solver.cachedImplicationChecks);
      out.println();
      out.println("Max ABE block size:                       " + prec.maxBlockSize);
      out.println("Number of predicates discovered:          " + allDistinctPreds);
      if (allDistinctPreds > 0) {
        out.println("Number of abstraction locations:          " + allLocs);
        out.println("Max number of predicates per location:    " + maxPredsPerLocation);
        out.println("Avg number of predicates per location:    " + avgPredsPerLocation);
      }
      int numAbstractions = as.numCallsAbstraction-as.numSymbolicAbstractions;
      if (numAbstractions > 0) {
        out.println("Max number of predicates per abstraction: " + prec.maxPredsPerAbstraction);
        out.println("Avg number of predicates per abstraction: " + div(prec.totalPredsPerAbstraction, prec.numAbstractions));
        out.println("Total number of models for allsat:        " + as.allSatCount);
        out.println("Max number of models for allsat:          " + as.maxAllSatCount);
        out.println("Avg number of models for allsat:          " + div(as.allSatCount, as.numCallsAbstraction));
      }
      out.println();
      if (pfMgr != null) {
        int pathFormulaCacheHits = pfMgr.pathFormulaCacheHits;
        int totalPathFormulaComputations = pfMgr.pathFormulaComputationTimer.getNumberOfIntervals() + pathFormulaCacheHits;
        out.println("Number of path formula cache hits:   " + pathFormulaCacheHits + " (" + toPercent(pathFormulaCacheHits, totalPathFormulaComputations) + ")");
      }
      if (numAbstractions > 0) {
        out.println("Number of abstraction cache hits:    " + as.numCallsAbstractionCached + " (" + toPercent(as.numCallsAbstractionCached, numAbstractions) + ")");
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
      if (trans.strengthenCheckTimer.getNumberOfIntervals() > 0) {
        out.println("  Time for satisfiability checks:    " + trans.strengthenCheckTimer);
      }
      out.println("Time for prec operator:              " + prec.totalPrecTime);
      if (prec.numAbstractions > 0) {
        out.println("  Time for abstraction:              " + prec.computingAbstractionTime + " (Max: " + prec.computingAbstractionTime.printMaxTime() + ", Count: " + prec.computingAbstractionTime.getNumberOfIntervals() + ")");
        out.println("    Solving time:                    " + as.abstractionSolveTime + " (Max: " + as.abstractionSolveTime.printMaxTime() + ")");
        out.println("    Model enumeration time:          " + as.abstractionEnumTime.printOuterSumTime());
        out.println("    Time for BDD construction:       " + as.abstractionEnumTime.printInnerSumTime()   + " (Max: " + as.abstractionEnumTime.printInnerMaxTime() + ")");
      }

      MergeOperator merge = cpa.getMergeOperator();
      if (merge instanceof PredicateMergeOperator) {
        out.println("Time for merge operator:             " + ((PredicateMergeOperator)merge).totalMergeTime);
      }

      out.println("Time for coverage check:             " + domain.coverageCheckTimer);
      if (domain.bddCoverageCheckTimer.getNumberOfIntervals() > 0) {
        out.println("  Time for BDD entailment checks:    " + domain.bddCoverageCheckTimer);
      }
      if (domain.symbolicCoverageCheckTimer.getNumberOfIntervals() > 0) {
        out.println("  Time for symbolic coverage checks: " + domain.symbolicCoverageCheckTimer);
      }
      out.println("Total time for SMT solver (w/o itp): " + Timer.formatTime(solver.solverTime.getSumTime() + as.abstractionSolveTime.getSumTime() + as.abstractionEnumTime.getOuterSumTime()));

      if (trans.pathFormulaCheckTimer.getNumberOfIntervals() > 0 || trans.abstractionCheckTimer.getNumberOfIntervals() > 0) {
        out.println("Time for abstraction checks:       " + trans.abstractionCheckTimer);
        out.println("Time for path formulae checks:     " + trans.pathFormulaCheckTimer + " (Num: " + as.numPathFormulaCoverageChecks + ", Equal: " + as.numEqualPathFormulae + ", Syn. entailed: " + as.numSyntacticEntailedPathFormulae + ", Sem. entailed: " + as.numSemanticEntailedPathFormulae + ")");
        out.println("Time for unsat checks:             " + trans.satCheckTimer + " (Calls: " + trans.satCheckTimer.getNumberOfIntervals() + ")");
      }
      out.println();
      rmgr.printStatistics(out);
    }

    private void exportInvariants(ReachedSet reached) {
      Map<CFANode, Region> regions = Maps.newHashMap();
      for (AbstractState state : reached) {
        CFANode loc = extractLocation(state);
        if (loc.isLoopStart()) {
          PredicateAbstractState predicateState = extractStateByType(state, PredicateAbstractState.class);
          assert predicateState.isAbstractionState();
          Region region = firstNonNull(regions.get(loc), rmgr.makeFalse());
          region = rmgr.makeOr(region, predicateState.getAbstractionFormula().asRegion());
          regions.put(loc, region);
        }
      }

      AbstractionManager absmgr = cpa.getAbstractionManager();
      FormulaManagerView fmgr = cpa.getFormulaManager();
      try (Writer invariants = Files.openOutputFile(invariantsFile)) {
        for (CFANode loc : from(cfa.getAllNodes())
                             .filter(CFAUtils.IS_LOOP_NODE)
                             .toImmutableSortedSet(CFAUtils.LINE_NUMBER_COMPARATOR)) {
          Region region = firstNonNull(regions.get(loc), rmgr.makeFalse());
          BooleanFormula formula = absmgr.toConcrete(region);
          invariants.append("loop__");
          invariants.append(loc.getFunctionName());
          invariants.append("__");
          invariants.append(""+loc.getLineNumber());
          invariants.append(":\n");
          invariants.append(fmgr.dumpFormula(formula));
          invariants.append('\n');
        }
      } catch (IOException e) {
        cpa.getLogger().logUserException(Level.WARNING, e, "Could not write loop invariants to file");
      }
    }
}
