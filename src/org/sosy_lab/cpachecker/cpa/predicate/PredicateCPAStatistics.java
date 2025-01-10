// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsUtils.div;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsUtils.toPercent;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsUtils.valueWithPercentage;

import com.google.common.base.Preconditions;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.LoopInvariantsWriter;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicateAbstractionsWriter;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicateMapWriter;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

@Options(prefix = "cpa.predicate")
final class PredicateCPAStatistics implements Statistics {

  @Option(secure = true, description = "generate statistics about precisions (may be slow)")
  private boolean precisionStatistics = true;

  @Option(secure = true, description = "export final predicate map", name = "predmap.export")
  private boolean exportPredmap = true;

  @Option(
      secure = true,
      description = "file for exporting final predicate map",
      name = "predmap.file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path predmapFile = Path.of("predmap.txt");

  @Option(secure = true, description = "export final loop invariants", name = "invariants.export")
  private boolean exportInvariants = true;

  @Option(
      secure = true,
      description = "export invariants as precision file?",
      name = "invariants.exportAsPrecision")
  private boolean exportInvariantsAsPrecision = true;

  @Option(
      secure = true,
      description = "file for exporting final loop invariants",
      name = "invariants.file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path invariantsFile = Path.of("invariants.txt");

  @Option(
      secure = true,
      description = "file for precision that consists of invariants.",
      name = "invariants.precisionFile")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path invariantPrecisionsFile = Path.of("invariantPrecs.txt");

  @Option(
      description = "Export one abstraction formula for each abstraction state into a file?",
      name = "abstractions.export")
  private boolean abstractionsExport = true;

  @Option(
      description = "Export abstraction formulas as (way more readable) expressions.",
      name = "abstractions.asExpressions")
  private boolean abstractionsAsExpressions = false;

  @Option(
      secure = true,
      description = "file that consists of one abstraction formula for each abstraction state",
      name = "abstractions.file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path abstractionsFile = Path.of("abstractions.txt");

  private final LogManager logger;

  private final PredicateAbstractDomain domain;
  private final @Nullable PredicateMergeOperator merge;
  private final PredicatePrecisionAdjustment prec;
  private final PredicateTransferRelation transfer;

  private final Solver solver;
  private final PathFormulaManager pfmgr;
  private final BlockOperator blk;
  private final RegionManager rmgr;
  private final AbstractionManager absmgr;
  private final PredicateAbstractionManager predAbsMgr;
  private final PredicatePrecision initialPrecision;

  private final PredicateMapWriter precisionWriter;
  private final LoopInvariantsWriter loopInvariantsWriter;
  private final PredicateAbstractionsWriter abstractionsWriter;

  PredicateCPAStatistics(
      Configuration pConfig,
      LogManager pLogger,
      CFA pCfa,
      PredicateAbstractDomain pDomain,
      @Nullable PredicateMergeOperator pMerge,
      PredicatePrecisionAdjustment pPrec,
      PredicateTransferRelation pTransfer,
      Solver pSolver,
      PathFormulaManager pPfmgr,
      BlockOperator pBlk,
      RegionManager pRmgr,
      AbstractionManager pAbsmgr,
      PredicateAbstractionManager pPredAbsMgr,
      PredicatePrecision pInitialPrecision)
      throws InvalidConfigurationException {
    pConfig.inject(this, PredicateCPAStatistics.class);

    logger = pLogger;
    domain = pDomain;
    merge = pMerge;
    prec = pPrec;
    transfer = pTransfer;
    solver = pSolver;
    pfmgr = pPfmgr;
    blk = pBlk;
    rmgr = pRmgr;
    absmgr = pAbsmgr;
    predAbsMgr = pPredAbsMgr;
    initialPrecision = pInitialPrecision;

    FormulaManagerView fmgr = pSolver.getFormulaManager();
    loopInvariantsWriter = new LoopInvariantsWriter(pCfa, pLogger, pAbsmgr, fmgr, pRmgr);
    abstractionsWriter = new PredicateAbstractionsWriter(pLogger, fmgr);

    if (exportPredmap && predmapFile != null) {
      precisionWriter = new PredicateMapWriter(pConfig, fmgr);
    } else {
      precisionWriter = null;
    }
  }

  @Override
  public String getName() {
    return "PredicateCPA";
  }

  /** TreeMap to sort output for the user and sets for no duplication. */
  private static class MutablePredicateSets {

    private final SetMultimap<PredicatePrecision.LocationInstance, AbstractionPredicate>
        locationInstance;
    private final SetMultimap<CFANode, AbstractionPredicate> location;
    private final SetMultimap<String, AbstractionPredicate> function;
    private final Set<AbstractionPredicate> global;

    private MutablePredicateSets() {
      // Use special multimaps with set-semantics and an ordering only on keys (not on values)
      locationInstance = MultimapBuilder.treeKeys().linkedHashSetValues().build();

      location = MultimapBuilder.treeKeys().linkedHashSetValues().build();
      function = MultimapBuilder.treeKeys().linkedHashSetValues().build();
      global = new LinkedHashSet<>();
    }
  }

  private void exportPredmapToFile(Path targetFile, MutablePredicateSets predicates) {
    Preconditions.checkNotNull(targetFile);
    Preconditions.checkNotNull(predicates);

    Set<AbstractionPredicate> allPredicates = new LinkedHashSet<>(predicates.global);
    allPredicates.addAll(predicates.function.values());
    allPredicates.addAll(predicates.location.values());
    allPredicates.addAll(predicates.locationInstance.values());

    try (Writer w = IO.openOutputFile(targetFile, Charset.defaultCharset())) {
      precisionWriter.writePredicateMap(
          predicates.locationInstance,
          predicates.location,
          predicates.function,
          predicates.global,
          allPredicates,
          w);
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write predicate map to file");
    }
  }

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    int maxPredsPerLocation = -1;
    int allLocs = -1;
    int avgPredsPerLocation = -1;
    if (precisionStatistics) {
      MutablePredicateSets predicates = new MutablePredicateSets();
      {
        Set<Precision> seenPrecisions = Collections.newSetFromMap(new IdentityHashMap<>());

        for (Precision precision : reached.getPrecisions()) {
          PredicatePrecision preds =
              Precisions.extractPrecisionByType(precision, PredicatePrecision.class);
          if (preds != null && seenPrecisions.add(preds)) {
            predicates.locationInstance.putAll(preds.getLocationInstancePredicates());
            predicates.location.putAll(preds.getLocalPredicates());
            predicates.function.putAll(preds.getFunctionPredicates());
            predicates.global.addAll(preds.getGlobalPredicates());
          }
        }
      }

      // check if/where to dump the predicate map
      if (exportPredmap && predmapFile != null) {
        exportPredmapToFile(predmapFile, predicates);
      }

      maxPredsPerLocation = 0;
      for (Collection<AbstractionPredicate> p : predicates.location.asMap().values()) {
        maxPredsPerLocation = Math.max(maxPredsPerLocation, p.size());
      }

      allLocs = predicates.location.keySet().size();
      int totPredsUsed = predicates.location.size();
      avgPredsPerLocation = allLocs > 0 ? totPredsUsed / allLocs : 0;
    }

    int allDistinctPreds = absmgr.getNumberOfPredicates();

    if (exportInvariants && invariantsFile != null) {
      loopInvariantsWriter.exportLoopInvariants(invariantsFile, reached);
    }

    if (abstractionsExport && abstractionsFile != null) {
      if (abstractionsAsExpressions) {
        abstractionsWriter.writeAbstractionsAsExpressions(abstractionsFile, reached);
      } else {
        abstractionsWriter.writeAbstractions(abstractionsFile, reached);
      }
    }

    if (exportInvariantsAsPrecision && invariantPrecisionsFile != null) {
      loopInvariantsWriter.exportLoopInvariantsAsPrecision(invariantPrecisionsFile, reached);
    }

    PredicateAbstractionManager.Stats as = predAbsMgr.stats;

    final int numAbstractions = prec.numAbstractions;
    out.println(
        "Number of abstractions:            "
            + numAbstractions
            + " ("
            + toPercent(numAbstractions, transfer.postTimer.getNumberOfIntervals())
            + " of all post computations)");
    if (numAbstractions > 0) {
      out.println("  Times abstraction was reused:    " + as.numAbstractionReuses);
      out.println(
          "  Because of function entry/exit:  "
              + valueWithPercentage(blk.numBlkFunctions.getValue(), numAbstractions));
      out.println(
          "  Because of loop head:            "
              + valueWithPercentage(blk.numBlkLoops.getValue(), numAbstractions));
      out.println(
          "  Because of join nodes:           "
              + valueWithPercentage(blk.numBlkJoins.getValue(), numAbstractions));
      out.println(
          "  Because of threshold:            "
              + valueWithPercentage(blk.numBlkThreshold.getValue(), numAbstractions));
      out.println(
          "  Because of target state:         "
              + valueWithPercentage(prec.numTargetAbstractions, numAbstractions));
      out.println(
          "  Times precision was empty:       "
              + valueWithPercentage(as.numSymbolicAbstractions, as.numCallsAbstraction));
      out.println(
          "  Times precision was {false}:     "
              + valueWithPercentage(as.numSatCheckAbstractions, as.numCallsAbstraction));
      out.println(
          "  Times result was cached:         "
              + valueWithPercentage(as.numCallsAbstractionCached, as.numCallsAbstraction));
      out.println(
          "  Times cartesian abs was used:    "
              + valueWithPercentage(
                  as.cartesianAbstractionTime.getNumberOfIntervals(), as.numCallsAbstraction));
      out.println(
          "  Times boolean abs was used:      "
              + valueWithPercentage(
                  as.booleanAbstractionTime.getNumberOfIntervals(), as.numCallsAbstraction));
      out.println(
          "  Times result was 'false':        "
              + valueWithPercentage(prec.numAbstractionsFalse, numAbstractions));
      if (as.inductivePredicatesTime.getNumberOfIntervals() > 0) {
        out.println(
            "  Times inductive cache was used:  "
                + valueWithPercentage(as.numInductivePathFormulaCacheUsed, as.numCallsAbstraction));
      }
    }

    if (transfer.satCheckTimer.getNumberOfIntervals() > 0) {
      out.println(
          "Number of satisfiability checks:   " + transfer.satCheckTimer.getNumberOfIntervals());
      out.println(
          "  Times result was 'false':        "
              + transfer.numSatChecksFalse
              + " ("
              + toPercent(transfer.numSatChecksFalse, transfer.satCheckTimer.getNumberOfIntervals())
              + ")");
    }
    out.println(
        "Number of strengthen sat checks:   "
            + transfer.strengthenCheckTimer.getNumberOfIntervals());
    if (transfer.strengthenCheckTimer.getNumberOfIntervals() > 0) {
      out.println(
          "  Times result was 'false':        "
              + transfer.numStrengthenChecksFalse
              + " ("
              + toPercent(
                  transfer.numStrengthenChecksFalse,
                  transfer.strengthenCheckTimer.getNumberOfIntervals())
              + ")");
    }
    out.println(
        "Number of coverage checks:         " + domain.coverageCheckTimer.getNumberOfIntervals());
    out.println(
        "  BDD entailment checks:           "
            + domain.bddCoverageCheckTimer.getNumberOfIntervals());
    if (domain.symbolicCoverageCheckTimer.getNumberOfIntervals() > 0) {
      out.println(
          "  Symbolic coverage check:         "
              + domain.symbolicCoverageCheckTimer.getNumberOfIntervals());
    }
    out.println("Number of SMT sat checks:          " + solver.satChecks);
    out.println("  trivial:                         " + solver.trivialSatChecks);
    out.println("  cached:                          " + solver.cachedSatChecks);
    out.println();
    out.println("Max ABE block size:                       " + prec.blockSize.getMaxValue());
    put(out, 0, prec.blockSize);
    if (!initialPrecision.isEmpty()) {
      out.println(
          "Number of initial global predicates:      "
              + initialPrecision.getGlobalPredicates().size());
      out.println(
          "Number of initial function predicates:    "
              + initialPrecision.getFunctionPredicates().size());
      out.println(
          "Number of initial local predicates:       "
              + initialPrecision.getLocalPredicates().size());
    }
    out.println("Number of predicates discovered:          " + allDistinctPreds);
    if (precisionStatistics && allDistinctPreds > 0) {
      out.println("Number of abstraction locations:          " + allLocs);
      out.println("Max number of predicates per location:    " + maxPredsPerLocation);
      out.println("Avg number of predicates per location:    " + avgPredsPerLocation);
    }
    if (as.numCallsAbstraction.get() > as.numSymbolicAbstractions.get()) {
      int numRealAbstractions =
          as.numCallsAbstraction.get()
              - as.numSymbolicAbstractions.get()
              - as.numCallsAbstractionCached.get();
      out.println("Total predicates per abstraction:         " + as.numTotalPredicates);
      out.println("Max number of predicates per abstraction: " + as.maxPredicates);
      out.println(
          "Avg number of predicates per abstraction: "
              + div(as.numTotalPredicates.get(), numRealAbstractions));
      out.println(
          "Number of irrelevant predicates:          "
              + valueWithPercentage(as.numIrrelevantPredicates, as.numTotalPredicates));
      if (as.trivialPredicatesTime.getNumberOfIntervals() > 0) {
        out.println(
            "Number of trivially used predicates:      "
                + valueWithPercentage(as.numTrivialPredicates, as.numTotalPredicates));
      }
      if (as.inductivePredicatesTime.getNumberOfIntervals() > 0) {
        out.println(
            "Number of inductive predicates:           "
                + valueWithPercentage(as.numInductivePredicates, as.numTotalPredicates));
      }
      if (as.cartesianAbstractionTime.getNumberOfIntervals() > 0) {
        out.println(
            "Number of preds cached for cartesian abs: "
                + valueWithPercentage(as.numCartesianAbsPredicatesCached, as.numTotalPredicates));
        out.println(
            "Number of preds solved by cartesian abs:  "
                + valueWithPercentage(as.numCartesianAbsPredicates, as.numTotalPredicates));
      }
      if (as.booleanAbstractionTime.getNumberOfIntervals() > 0) {
        out.println(
            "Number of preds handled by boolean abs:   "
                + valueWithPercentage(as.numBooleanAbsPredicates, as.numTotalPredicates));
        out.println("  Total number of models for allsat:      " + as.allSatCount);
        out.println("  Max number of models for allsat:        " + as.maxAllSatCount);
        out.println(
            "  Avg number of models for allsat:        "
                + div(as.allSatCount, as.booleanAbstractionTime.getNumberOfIntervals()));
      }
    }
    pfmgr.printStatistics(out);
    out.println();

    put(out, 0, transfer.postTimer);
    put(out, 1, transfer.pathFormulaTimer);
    if (transfer.satCheckTimer.getNumberOfIntervals() > 0) {
      put(out, 1, transfer.satCheckTimer);
    }
    put(out, 0, transfer.strengthenTimer);
    if (transfer.strengthenCheckTimer.getNumberOfIntervals() > 0) {
      put(out, 1, transfer.strengthenCheckTimer);
    }
    put(out, 0, prec.totalPrecTime);
    if (numAbstractions > 0) {
      out.println(
          "  Time for abstraction:              "
              + prec.computingAbstractionTime
              + " (Max: "
              + prec.computingAbstractionTime.getMaxTime().formatAs(SECONDS)
              + ", Count: "
              + prec.computingAbstractionTime.getNumberOfIntervals()
              + ")");
      if (as.trivialPredicatesTime.getNumberOfIntervals() > 0) {
        out.println("    Relevant predicate analysis:     " + as.trivialPredicatesTime);
      }
      if (as.inductivePredicatesTime.getNumberOfIntervals() > 0) {
        out.println("    Inductive predicate analysis:    " + as.inductivePredicatesTime);
      }
      if (as.cartesianAbstractionTime.getNumberOfIntervals() > 0) {
        out.println("    Cartesian abstraction:           " + as.cartesianAbstractionTime);
      }
      if (as.booleanAbstractionTime.getNumberOfIntervals() > 0) {
        out.println("    Boolean abstraction:             " + as.booleanAbstractionTime);
      }
      if (as.abstractionReuseTime.getNumberOfIntervals() > 0) {
        out.println("    Abstraction reuse:              " + as.abstractionReuseTime);
        out.println("    Abstraction reuse implication:  " + as.abstractionReuseImplicationTime);
      }
      out.println(
          "    Solving time:                    "
              + as.abstractionSolveTime
              + " (Max: "
              + as.abstractionSolveTime.getMaxTime().formatAs(SECONDS)
              + ")");
      out.println(
          "    Model enumeration time:          "
              + as.abstractionModelEnumTime.getSumTime().formatAs(SECONDS));
      out.println(
          "    Time for BDD construction:       "
              + as.abstractionBddConstructionTime.getSumTime().formatAs(SECONDS)
              + " (Max: "
              + as.abstractionBddConstructionTime.getMaxTime().formatAs(SECONDS)
              + ")");
    }

    if (merge != null) {
      put(out, 0, merge.totalMergeTime);
    }

    put(out, 0, domain.coverageCheckTimer);
    if (domain.bddCoverageCheckTimer.getNumberOfIntervals() > 0) {
      put(out, 1, domain.bddCoverageCheckTimer);
    }
    if (domain.symbolicCoverageCheckTimer.getNumberOfIntervals() > 0) {
      put(out, 1, domain.symbolicCoverageCheckTimer);
    }
    out.println(
        "Total time for SMT solver (w/o itp): "
            + TimeSpan.sum(
                    solver.solverTime.getSumTime(),
                    as.abstractionSolveTime.getSumTime(),
                    as.abstractionModelEnumTime.getSumTime())
                .formatAs(SECONDS));

    if (transfer.abstractionCheckTimer.getNumberOfIntervals() > 0) {
      put(out, 0, transfer.abstractionCheckTimer);
      out.println(
          "Time for unsat checks:             "
              + transfer.satCheckTimer
              + " (Calls: "
              + transfer.satCheckTimer.getNumberOfIntervals()
              + ")");
    }
    out.println();
    rmgr.printStatistics(out);
    solver.printStatistics(out);
  }
}
