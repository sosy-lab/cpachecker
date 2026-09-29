// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.invariants.FormulaInvariantsSupplier;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;

/** Computes optional entry invariants from a complete, sound, bounded data-flow pass. */
public final class DssBlockEntryInvariants {
  private DssBlockEntryInvariants() {}

  /**
   * Returns facts about original CFA locations. A timeout, an incomplete exploration, or a
   * conditional/unsound result contributes no facts. The auxiliary analysis never supplies the
   * verification verdict: DSS still explores its blocks and enforces its ordinary publication gate.
   */
  public static ImmutableMap<CFANode, DssPredicateEntryInvariant> compute(
      CFA cfa,
      Collection<CFANode> locations,
      Specification specification,
      Configuration config,
      PredicateCPA predicate,
      LogManager logger,
      ShutdownManager parentShutdown,
      TimeSpan timeLimit)
      throws InvalidConfigurationException, InterruptedException {
    ShutdownManager shutdown = ShutdownManager.createWithParent(parentShutdown.getNotifier());
    ResourceLimitChecker limit =
        ResourceLimitChecker.createWallTimeLimitChecker(shutdown, timeLimit);
    ConfigurableProgramAnalysis cpa = null;
    CPAAlgorithm algorithm = null;
    limit.start();
    try {
      var factory =
          new CoreComponentsFactory(
              config, logger, shutdown.getNotifier(), AggregatedReachedSets.empty(), cfa);
      cpa = factory.createCPA(specification);
      algorithm = CPAAlgorithm.create(cpa, logger, config, shutdown.getNotifier());
      var reached = factory.createReachedSet(cpa);
      var partition = StateSpacePartition.getDefaultPartition();
      reached.add(
          cpa.getInitialState(cfa.getMainFunction(), partition),
          cpa.getInitialPrecision(cfa.getMainFunction(), partition));
      while (reached.hasWaitingState()) {
        shutdown.getNotifier().shutdownIfNecessary();
        if (!algorithm.run(reached).isSound()) {
          return ImmutableMap.of();
        }
      }
      if (reached.isEmpty()) {
        return ImmutableMap.of();
      }
      for (var state : reached) {
        var assumption = AbstractStates.extractStateByType(state, AssumptionStorageState.class);
        if (assumption != null
            && (!assumption.isAssumptionTrue() || !assumption.isStopFormulaTrue())) {
          return ImmutableMap.of();
        }
      }
      var supplier = new FormulaInvariantsSupplier(AggregatedReachedSets.singleton(reached));
      var fmgr = predicate.getSolver().getFormulaManager();
      Map<String, CType> types = variableTypes(cfa);
      var result = ImmutableMap.<CFANode, DssPredicateEntryInvariant>builder();
      for (var location : locations) {
        shutdown.getNotifier().shutdownIfNecessary();
        var invariant =
            supplier.getInvariantFor(
                location, Optional.empty(), fmgr, predicate.getPathFormulaManager(), null);
        var variables = fmgr.extractVariableNames(invariant);
        if (fmgr.getBooleanFormulaManager().isTrue(invariant)
            || !types.keySet().containsAll(variables)) {
          // Unrecognized memory-location encodings need a dedicated translation. Dropping the
          // whole optional fact is safe, including when it contains disjunctions.
          continue;
        }
        var usedTypes = ImmutableMap.<String, CType>builder();
        variables.forEach(name -> usedTypes.put(name, types.get(name)));
        result.put(
            location,
            new DssPredicateEntryInvariant(
                fmgr.dumpFormula(invariant).toString(), usedTypes.buildOrThrow()));
      }
      shutdown.getNotifier().shutdownIfNecessary();
      return result.buildOrThrow();
    } catch (InterruptedException e) {
      parentShutdown.getNotifier().shutdownIfNecessary();
      if (!shutdown.getNotifier().shouldShutdown()) {
        throw e;
      }
      logger.log(Level.FINE, "Entry-invariant time limit reached; continuing without these facts.");
      return ImmutableMap.of();
    } catch (CPAException e) {
      logger.logUserException(Level.FINE, e, "Entry invariants unavailable; continuing DSS");
      return ImmutableMap.of();
    } finally {
      limit.cancel();
      if (algorithm != null) {
        CPAs.closeIfPossible(algorithm, logger);
      }
      if (cpa != null) {
        CPAs.closeCpaIfPossible(cpa, logger);
      }
    }
  }

  private static Map<String, CType> variableTypes(CFA cfa) {
    Map<String, CType> types = new HashMap<>();
    for (var node : cfa.nodes()) {
      for (var edge : node.getLeavingEdges()) {
        if (edge instanceof CDeclarationEdge declaration
            && declaration.getDeclaration() instanceof CVariableDeclaration variable) {
          types.put(variable.getQualifiedName(), variable.getType());
        }
      }
      if (node instanceof CFunctionEntryNode function) {
        for (var parameter : function.getFunctionParameters()) {
          types.put(parameter.getQualifiedName(), parameter.getType());
        }
        function
            .getReturnVariable()
            .ifPresent(variable -> types.put(variable.getQualifiedName(), variable.getType()));
      }
    }
    return types;
  }
}
