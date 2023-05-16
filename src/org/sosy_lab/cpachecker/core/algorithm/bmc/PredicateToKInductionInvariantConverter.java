// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.SingleLocationFormulaInvariant;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecisionBootstrapper.InitialPredicatesOptions;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicateMapParser;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.PredicateParsingFailedException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

/*
 * Instead of providing option localPredicatesInFunctionScope, use the options of
 * InitialPredicatesOptions
 */
@Options(prefix = "bmc.kinduction.reuse")
public class PredicateToKInductionInvariantConverter implements Statistics, AutoCloseable {

  public enum PredicatePrecisionConverterStrategy {
    ALL(true, true, true),
    GLOBAL(true, false, false),
    FUNCTION(false, true, false),
    LOCAL(false, false, true),
    GLOBAL_AND_FUNCTION(true, true, false),
    GLOBAL_AND_LOCAL(true, false, true),
    FUNCTION_AND_LOCAL(false, true, true);

    private final boolean useGlobalPreds;
    private final boolean useFunctionPreds;
    private final boolean useLocalPreds;

    private PredicatePrecisionConverterStrategy(
        final boolean pUseGlobalPreds, final boolean pUseFunctionPreds, final boolean pUseLocalPreds) {
      useGlobalPreds = pUseGlobalPreds;
      useFunctionPreds = pUseFunctionPreds;
      useLocalPreds = pUseLocalPreds;
    }
  }

  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final CFA cfa;

  @Option(
      secure = true,
      name = "pred.strategy",
      description = "which strategy to use to convert predicate precision to k-induction invariant")
  private PredicatePrecisionConverterStrategy converterStrategy =
      PredicatePrecisionConverterStrategy.GLOBAL;

  private final Timer conversionTime = new Timer();
  private int numInvariants = 0;
  private InitialPredicatesOptions initialPredicatesOptions;

  public PredicateToKInductionInvariantConverter(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa)
      throws InvalidConfigurationException {
    config = pConfig;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    cfa = pCfa;
    config.inject(this);
    initialPredicatesOptions = new InitialPredicatesOptions();
    config.inject(initialPredicatesOptions);
  }

  public ImmutableSet<CandidateInvariant> convertPredPrecToKInductionInvariant(
      final Path pPredPrecFile,
      final Solver pSolver,
      final AbstractionManager pAbstractionManager) {
    conversionTime.start();
    try {
      FormulaManagerView formulaManager = pSolver.getFormulaManager();

      PredicatePrecision predPrec =
          new PredicateMapParser(
                  cfa, logger, formulaManager, pAbstractionManager, initialPredicatesOptions)
              .parsePredicates(pPredPrecFile);

      shutdownNotifier.shutdownIfNecessary();

      if (!predPrec.isEmpty()) {
        logger.log(Level.INFO, "Derive k-induction invariant from given predicate precision");
        ImmutableSet<CandidateInvariant> candidates =
            convertPredPrecToKInductionInvariant(predPrec, formulaManager, shutdownNotifier);
        numInvariants += candidates.size();
        return candidates;
      } else {
        logger.log(
            Level.WARNING,
            "Provided predicate precision is empty and does not contain predicates.");
      }
    } catch (IOException | PredicateParsingFailedException e) {
      logger.logUserException(
          Level.WARNING, e, "Could not read precision from file named " + pPredPrecFile);
    } catch (InterruptedException e) {
      logger.logException(Level.INFO, e, "Precision adaption was interrupted.");
    }
    finally {
      conversionTime.stopIfRunning();
    }

      return ImmutableSet.of();
  }

  private ImmutableSet<CandidateInvariant> convertPredPrecToKInductionInvariant(
      final PredicatePrecision pPredPrec,
      final FormulaManagerView pFMgr,
      final ShutdownNotifier pShutdownNotifier) throws InterruptedException {

    //since k-induction only works with invariants at loop heads
    //if there are no loop heads, no invariants are needed
    if(!cfa.getAllLoopHeads().isPresent()) {
      return ImmutableSet.of();
    }

    ImmutableSet.Builder<CandidateInvariant> candidates = ImmutableSet.builder();

    //sort loop heads for easier access later on
    SetMultimap<String, CFANode> loopHeadsPerFunction = HashMultimap.create();
    for(CFANode loopHead : cfa.getAllLoopHeads().orElseThrow()) {
      loopHeadsPerFunction.put(loopHead.getFunctionName(), loopHead);
    }

    pShutdownNotifier.shutdownIfNecessary();
    if(converterStrategy.useGlobalPreds) {
      for (AbstractionPredicate pred : pPredPrec.getGlobalPredicates()) {
        for (CFANode loopHead : cfa.getAllLoopHeads().orElseThrow()) {
          candidates.add(SingleLocationFormulaInvariant.makeLocationInvariant(
              loopHead, pred.getSymbolicAtom(), pFMgr));
        }
      }
    }

    pShutdownNotifier.shutdownIfNecessary();
    if(converterStrategy.useFunctionPreds) {
      for (Map.Entry<String, AbstractionPredicate> entry : pPredPrec.getFunctionPredicates().entries()) {
        for(CFANode loopHead : loopHeadsPerFunction.get(entry.getKey())) {
          candidates.add(SingleLocationFormulaInvariant.makeLocationInvariant(
              loopHead, entry.getValue().getSymbolicAtom(), pFMgr));
        }
      }
    }

    pShutdownNotifier.shutdownIfNecessary();
    if(converterStrategy.useLocalPreds) {
      for (Map.Entry<CFANode, AbstractionPredicate> entry : pPredPrec.getLocalPredicates().entries()) {
          if(entry.getKey().isLoopStart()) {
            candidates.add(SingleLocationFormulaInvariant.makeLocationInvariant(
                entry.getKey(), entry.getValue().getSymbolicAtom(), pFMgr));
          }
      }
    }

    pShutdownNotifier.shutdownIfNecessary();
    return candidates.build();
  }

  @Override
  public void printStatistics(
      final PrintStream pOut, final Result pResult, final UnmodifiableReachedSet pReached) {
    put(pOut, 0, "Time for adapting predicate precision", conversionTime);
    put(pOut, 0, "Number of invariants proposed", numInvariants);
  }

  @Override
  public String getName() {
    return "Predicate Precision to K-Induction Invariants Converter";
  }

  @Override
  public void close(){

  }
}
