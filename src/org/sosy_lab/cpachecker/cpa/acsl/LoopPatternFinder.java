// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;

/**
 * Detect if a loop follows a certain pattern that makes it possible to infer an ACSL invariant. For
 * now the goal is to detect only one pattern: array initialization with a constant value.
 */
class LoopPatternFinder {

  private final LogManager logger;
  private final Level logLevel = Level.FINER; // TODO change to INFO for debugging

  LoopPatternFinder(LogManager pLogger) {
    this.logger = pLogger;
  }

  public Optional<ArrayInitialization> detect(CFANode loopHead) {
    Optional<CAssumeEdge> boundEdge = Optional.empty();
    logger.log(logLevel, "[ACSL] Inspecting loop head: " + loopHead);

    for (CFAEdge edge : loopHead.getLeavingEdges()) {
      if (edge instanceof CAssumeEdge cAssumeEdge && cAssumeEdge.getTruthAssumption()) {
        boundEdge = Optional.of(cAssumeEdge);
        logger.log(logLevel, "[ACSL] Loop bound found: " + edge);
      }
    }

    if (boundEdge.isEmpty()) {
      logger.log(logLevel, "[ACSL] Could not detect bound in loop.");
      return Optional.empty();
    }

    // Walk through loop body
    List<ArrayStore> stores =
        new ArrayList<>(); // TODO pipeline will probably hate this datastructure
    List<ScalarUpdate> updates = new ArrayList<>();

    CFANode current = boundEdge.orElseThrow().getSuccessor();

    while (current != loopHead) {
      // Simple Initialization should not branch anywhere
      if (current.getNumLeavingEdges() != 1) {

        logger.log(logLevel, "[ACSL] Loop body branches. Ignoring loop.");

        return Optional.empty();
      }

      CFAEdge edge = current.getLeavingEdge(0);
      if (edge.getSuccessor() == loopHead) {
        current = loopHead;
        break;
      }

      Optional<ArrayStore> store = extractArrayStore(edge);
      Optional<ScalarUpdate> update = extractScalarUpdate(edge);

      // Deal with edges we do not recognize
      if (store.isEmpty() && update.isEmpty()) {
        if (edge instanceof CDeclarationEdge) {
          // This is fine because it does not change our state
          logger.log(logLevel, "[ACSL] Found declaration edge" + edge);
        } else if (edge instanceof CStatementEdge cStatementEdge
            && cStatementEdge.getStatement() instanceof CExpressionStatement) {
          // This is fine because there is no function call and no assignment
          logger.log(logLevel, "[ACSL] Found statement edge" + edge);
        } else {
          // The edge might have an effect on the state, so to be safe ignore this loop.
          logger.log(logLevel, "[ACSL] Loop body contains unwanted edge. Ignoring loop.");
          return Optional.empty();
        }
      }

      store.ifPresent(
          pStore -> {
            logger.log(logLevel, "[ACSL] Array store found: " + pStore);

            stores.add(pStore);
          });

      update.ifPresent(
          pUpdate -> {
            logger.log(logLevel, "[ACSL] Scalar update found: " + pUpdate);

            updates.add(pUpdate);
          });

      current = edge.getSuccessor();
    }

    // There should only be one array store and update for this toy example
    if (stores.size() != 1 || updates.size() != 1) {

      logger.log(
          logLevel,
          "[ACSL] Expected exactly one store or update, found "
              + stores.size()
              + ", "
              + updates.size());

      return Optional.empty();
    }
    ArrayStore store = stores.getFirst();
    ScalarUpdate update = updates.getFirst();

    logger.log(
        logLevel,
        "[ACSL] Candidate for array initialization pattern found:\n"
            + "  array = "
            + store.array()
            + "\n"
            + "  index = "
            + store.index()
            + "\n"
            + "  value = "
            + store.value()
            + "\n"
            + "  update = "
            + update);

    // TODO check that array index in store, incremented variable in update and variable from loop
    // condition match
    // TODO check that the increment is by 1
    // TODO extract bound from loop condition and create actual record with it

    return Optional.empty();
  }

  private Optional<ArrayStore> extractArrayStore(CFAEdge pCfaEdge) {
    if (pCfaEdge instanceof CStatementEdge statementEdge) {
      CStatement statement = statementEdge.getStatement();
      if (statement instanceof CAssignment assign) {
        if (assign.getLeftHandSide() instanceof CArraySubscriptExpression left) {
          return Optional.of(
              new ArrayStore(
                  left.getArrayExpression(),
                  left.getSubscriptExpression(),
                  assign.getRightHandSide()));
        }
      }
    }
    return Optional.empty();
  }

  private Optional<ScalarUpdate> extractScalarUpdate(CFAEdge pCfaEdge) {
    if (pCfaEdge instanceof CStatementEdge statementEdge) {
      CStatement statement = statementEdge.getStatement();
      if (statement instanceof CAssignment assign) {
        if (assign.getLeftHandSide() instanceof CIdExpression left) {
          return Optional.of(new ScalarUpdate(left, assign.getRightHandSide()));
        }
      }
    }
    return Optional.empty();
  }
}
