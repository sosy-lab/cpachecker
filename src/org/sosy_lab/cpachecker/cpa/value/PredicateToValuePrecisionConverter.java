// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecisionBootstrapper.InitialPredicatesOptions;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicateMapParser;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.PredicateParsingFailedException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.regions.SymbolicRegionManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

@Options(prefix = "cpa.value")
public class PredicateToValuePrecisionConverter {

  public enum PredicateConverterStrategy {
    CONVERT_ONLY
  } // TODO extend// dependencies-backwards, forwards (property), only data or also
    // control-dependencies?

  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final CFA cfa;

  @Option(
      secure = true,
      name = "predToValPrecStrategy",
      description = "which strategy to use to convert predicate to value precision")
  private PredicateConverterStrategy converterStrategy;

  public PredicateToValuePrecisionConverter(
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

  public Multimap<CFANode, MemoryLocation> convertPredPrecToVariableTrackingPrec(
      final Path pPredPrecFile) throws InvalidConfigurationException {
    try (Solver solver = Solver.create(config, logger, shutdownNotifier)) {
      FormulaManagerView formulaManager = solver.getFormulaManager();

      RegionManager regionManager = new SymbolicRegionManager(solver);
      AbstractionManager abstractionManager =
          new AbstractionManager(regionManager, config, logger, solver);

      PredicatePrecision predPrec =
          parsePredPrecFile(formulaManager, abstractionManager, pPredPrecFile);

      if (!predPrec.isEmpty()) {
        Multimap<CFANode, MemoryLocation> result =
            convertPredPrecToVariableTrackingPrec(predPrec, formulaManager);

        switch (converterStrategy) {
          case CONVERT_ONLY:
            break;
          default:
            break;
        }

        return ImmutableListMultimap.copyOf(result);
      } else {
        logger.log(
            Level.WARNING,
            "Provided predicate precision is empty and does not contain predicates.");
      }
    }
    return ImmutableListMultimap.of();
  }

  private PredicatePrecision parsePredPrecFile(
      final FormulaManagerView pFMgr,
      final AbstractionManager abstractionManager,
      final Path pPredPrecFile) {

    // create managers for the predicate map parser for parsing the predicates from the given
    // predicate precision file

    PredicateMapParser mapParser =
        new PredicateMapParser(
            cfa, this.logger, pFMgr, abstractionManager, new InitialPredicatesOptions());

    try {
      return mapParser.parsePredicates(pPredPrecFile);
    } catch (IOException | PredicateParsingFailedException e) {
      logger.logUserException(
          Level.WARNING, e, "Could not read precision from file named " + pPredPrecFile);
      return PredicatePrecision.empty();
    }
  }

  private Multimap<CFANode, MemoryLocation> convertPredPrecToVariableTrackingPrec(
      final PredicatePrecision pPredPrec, final FormulaManagerView pFMgr) {
    Collection<AbstractionPredicate> predicates = new HashSet<>();

    predicates.addAll(pPredPrec.getLocalPredicates().values());
    predicates.addAll(pPredPrec.getGlobalPredicates());
    predicates.addAll(pPredPrec.getFunctionPredicates().values());

    SetMultimap<CFANode, MemoryLocation> trackedVariables = HashMultimap.create();
    CFANode dummyNode = new CFANode(CFunctionDeclaration.DUMMY);

    // Get the variables from the predicate precision
    for (AbstractionPredicate pred : predicates) {
      for (String var : pFMgr.extractVariables(pred.getSymbolicAtom()).keySet()) {
        trackedVariables.put(dummyNode, MemoryLocation.parseExtendedQualifiedName(var));
      }
    }

    return trackedVariables;
  }
}
