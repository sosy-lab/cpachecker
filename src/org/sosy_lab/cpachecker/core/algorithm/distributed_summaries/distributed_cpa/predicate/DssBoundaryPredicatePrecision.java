// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MultimapBuilder;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import org.jspecify.annotations.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

/** Mines boundary guards as candidate predicates, never as assumed entry facts. */
public final class DssBoundaryPredicatePrecision {
  private final PredicateCPA cpa;
  private final ImmutableSet<CFANode> boundaries;
  private final LogManager logger;
  private final ShutdownNotifier shutdown;
  private @Nullable PredicatePrecision precision;

  public DssBoundaryPredicatePrecision(
      PredicateCPA pCpa,
      ImmutableSet<CFANode> pBoundaries,
      LogManager pLogger,
      ShutdownNotifier pShutdown) {
    cpa = pCpa;
    boundaries = pBoundaries;
    logger = pLogger;
    shutdown = pShutdown;
  }

  public PredicatePrecision getPrecision() throws InterruptedException {
    if (precision != null) {
      return precision;
    }
    var predicates =
        MultimapBuilder.treeKeys().linkedHashSetValues().<CFANode, AbstractionPredicate>build();
    var bfmgr = cpa.getSolver().getFormulaManager().getBooleanFormulaManager();
    var pfmgr = cpa.getPathFormulaManager();
    for (CFANode boundary : boundaries) {
      for (AssumeEdge assume : entryAssumptions(boundary)) {
        shutdown.shutdownIfNecessary();
        try {
          var guard = pfmgr.makeAnd(pfmgr.makeEmptyPathFormula(), assume).getFormula();
          if (!assume.getTruthAssumption()) {
            guard = bfmgr.not(guard);
          }
          if (!bfmgr.isTrue(guard) && !bfmgr.isFalse(guard)) {
            predicates.put(boundary, cpa.getPredicateManager().getPredicateFor(guard));
          }
        } catch (CPATransferException e) {
          logger.logUserException(Level.FINE, e, "Could not seed an optional boundary predicate");
        }
      }
    }
    precision = PredicatePrecision.empty().addLocalPredicates(predicates.entries());
    return precision;
  }

  /** Traverse only blank edges, which leave the boundary's variable values unchanged. */
  private Set<AssumeEdge> entryAssumptions(CFANode boundary) throws InterruptedException {
    Set<AssumeEdge> result = new LinkedHashSet<>();
    Set<CFANode> visited = new HashSet<>();
    var pending = new ArrayDeque<CFANode>();
    pending.add(boundary);
    while (!pending.isEmpty()) {
      shutdown.shutdownIfNecessary();
      CFANode node = pending.removeFirst();
      if (!visited.add(node)) {
        continue;
      }
      for (var edge : node.getLeavingEdges()) {
        if (edge instanceof AssumeEdge assume) {
          result.add(assume);
        } else if (edge instanceof BlankEdge) {
          pending.addLast(edge.getSuccessor());
        }
      }
    }
    return result;
  }
}
