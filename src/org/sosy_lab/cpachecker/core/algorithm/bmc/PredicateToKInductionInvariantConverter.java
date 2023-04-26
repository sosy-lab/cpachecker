// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Stream;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.SingleLocationFormulaInvariant;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecisionBootstrapper.InitialPredicatesOptions;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicateMapParser;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.PredicateParsingFailedException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.regions.SymbolicRegionManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.resources.ResourceLimit;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;
import org.sosy_lab.cpachecker.util.resources.WalltimeLimit;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "bmc.kinduction")
public class PredicateToKInductionInvariantConverter implements Statistics, AutoCloseable {

  public enum PredicateConverterStrategy {
    ALL(true, true, true),
    GLOBAL(true, false, false),
    FUNCTION(false, true, false),
    LOCAL(false, false, true),
    GLOBAL_AND_FUNCTION(true, true, false),
    GLOBAL_AND_LOCAL(true, false, true),
    FUNCTION_AND_LOCAL(false, true, true);
    
    private final Boolean global;
    private final Boolean function;
    private final Boolean local;
    
    private PredicateConverterStrategy(Boolean global, Boolean function, Boolean local) {
      this.global = global;
      this.function = function;
      this.local = local;
    }
  }

  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final CFA cfa;

  @Option(
      secure = true,
      name = "strategy",
      description = "which strategy to use to convert predicate precision to k-induction invariant")
  private PredicateConverterStrategy converterStrategy = PredicateConverterStrategy.ALL;

  @Option(
      secure = true,
      description =
          "Overall timelimit for computing initial k-induction invariant from given predicate precision"
              + "(use seconds or specify a unit; 0 for infinite)")
  @TimeSpanOption(codeUnit = TimeUnit.NANOSECONDS, defaultUserUnit = TimeUnit.SECONDS, min = 0)
  private TimeSpan adaptionLimit = TimeSpan.ofNanos(0);
  
  @Option(
      secure = true,
      name = "localAsFunction",
      description = "Treat local predicates like function predicates of the function they are in. Works only if local predicates are analyzed.")
  private Boolean localAsFunction = false;

  private final Timer conversionTime = new Timer();
  private int numInvariants = 0;

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
  }

  public Set<CandidateInvariant> convertPredPrecToKInductionInvariant(
      final Path pPredPrecFile) throws InvalidConfigurationException {
    ResourceLimitChecker limitChecker = null;
    ShutdownNotifier conversionShutdownNotifier;
    if (!adaptionLimit.isEmpty()) {
      ShutdownManager conversionShutdownManager =
          ShutdownManager.createWithParent(shutdownNotifier);
      conversionShutdownNotifier = conversionShutdownManager.getNotifier();
      ResourceLimit limit = WalltimeLimit.fromNowOn(adaptionLimit);
      limitChecker = new ResourceLimitChecker(conversionShutdownManager, ImmutableList.of(limit));
      limitChecker.start();
    } else {
      conversionShutdownNotifier = shutdownNotifier;
    }

    Set<CandidateInvariant> result = null;

    conversionTime.start();
    try (Solver solver = Solver.create(config, logger, conversionShutdownNotifier)) {
      FormulaManagerView formulaManager = solver.getFormulaManager();
      
      RegionManager regionManager = new SymbolicRegionManager(solver);
      AbstractionManager abstractionManager =
          new AbstractionManager(regionManager, config, logger, solver);

      PredicatePrecision predPrec =
          parsePredPrecFile(formulaManager, abstractionManager, pPredPrecFile);

      conversionShutdownNotifier.shutdownIfNecessary();

      if (!predPrec.isEmpty()) {
        logger.log(Level.INFO, "Derive k-induction invariant from given predicate precision");
        result = convertPredPrecToKInductionInvariant(predPrec, formulaManager, conversionShutdownNotifier, solver);

        conversionShutdownNotifier.shutdownIfNecessary();

      } else {
        logger.log(
            Level.WARNING,
            "Provided predicate precision is empty and does not contain predicates.");
      }
    } catch (InterruptedException e) {
      logger.logException(Level.INFO, e, "Precision adaption was interrupted.");
    } catch (SolverException e) {
      logger.logException(Level.INFO, e, "Precision adaption threw an exception.");
    }
    finally {
      conversionTime.stopIfRunning();
    }

    if (limitChecker != null) {
      limitChecker.cancel();
    }

    if (result == null) {
      return ImmutableSet.of();
    }
    numInvariants += result.size();
    return ImmutableSet.copyOf(result);
  }

  private PredicatePrecision parsePredPrecFile(
      final FormulaManagerView pFMgr,
      final AbstractionManager abstractionManager,
      final Path pPredPrecFile) {

    // create managers for the predicate map parser for parsing the predicates from the given
    // predicate precision file

    PredicateMapParser mapParser =
        new PredicateMapParser(
            cfa, logger, pFMgr, abstractionManager, new InitialPredicatesOptions());

    try {
      return mapParser.parsePredicates(pPredPrecFile);
    } catch (IOException | PredicateParsingFailedException e) {
      logger.logUserException(
          Level.WARNING, e, "Could not read precision from file named " + pPredPrecFile);
      return PredicatePrecision.empty();
    }
  }

  private Set<CandidateInvariant> convertPredPrecToKInductionInvariant(
      final PredicatePrecision pPredPrec,
      final FormulaManagerView pFMgr,
      final ShutdownNotifier pConversionShutdownNotifier,
      final Solver pSolver) throws InterruptedException, SolverException {
    
    //since k-induction only works with invariants at loopheads
    //if there are no loopheads, no invariants are needed
    if(!cfa.getAllLoopHeads().isPresent()) return new HashSet<>();
    
    SetMultimap<CFANode, BooleanFormula> allPreds = HashMultimap.create();
    
    SetMultimap<String, CFANode> loopHeadsPerFunction = HashMultimap.create();
    for(CFANode loopHead : cfa.getAllLoopHeads().get()) {
      loopHeadsPerFunction.put(loopHead.getFunctionName(), loopHead);
    }
    
    pConversionShutdownNotifier.shutdownIfNecessary();
    
    // Get the whole predicate precision in the same set
    if(converterStrategy.global) {
      for (AbstractionPredicate pred : new HashSet<>(pPredPrec.getGlobalPredicates())) {
        for(CFANode loopHead : loopHeadsPerFunction.values()) {
          allPreds.put(loopHead, pred.getSymbolicAtom());
        }
      }
    }
    
    pConversionShutdownNotifier.shutdownIfNecessary();

    if(converterStrategy.function) {
      for (Map.Entry<String, AbstractionPredicate> entry : pPredPrec.getFunctionPredicates().entries()) {
        for(CFANode loopHead : loopHeadsPerFunction.get(entry.getKey())) {
          allPreds.put(loopHead, entry.getValue().getSymbolicAtom());
        }
      }
    }
    
    pConversionShutdownNotifier.shutdownIfNecessary();
    
    if(converterStrategy.local) {
      for (Map.Entry<CFANode, AbstractionPredicate> entry : pPredPrec.getLocalPredicates().entries()) {
        if(localAsFunction) {
          for(CFANode loopHead : loopHeadsPerFunction.get(entry.getKey().getFunctionName())) {
            allPreds.put(loopHead, entry.getValue().getSymbolicAtom());
          }
        } else {
          if(entry.getKey().isLoopStart()) {
            allPreds.put(entry.getKey(), entry.getValue().getSymbolicAtom());
          }
        }
      }
    }
    
    pConversionShutdownNotifier.shutdownIfNecessary();
    
    Set<CandidateInvariant> candidates = new HashSet<>();
    BooleanFormulaManagerView booleanFMgr = pFMgr.getBooleanFormulaManager();
    
      // Combine all boolean formulas per node and make invariant
      for(CFANode node : allPreds.keySet()) {
        BooleanFormula completeFormula = booleanFMgr.and(allPreds.get(node));
        //try (ProverEnvironment prover = pSolver.newProverEnvironment()) {
        //  prover.push(completeFormula);
        //  if (!prover.isUnsat()) {
        //    continue;
        //  }
        //}
        candidates.add(SingleLocationFormulaInvariant.makeLocationInvariant(node, completeFormula, pFMgr));
      }
    
    return candidates;
    
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
