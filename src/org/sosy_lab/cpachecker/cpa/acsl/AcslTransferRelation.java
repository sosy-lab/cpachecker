// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

@SuppressWarnings("unused")
public class AcslTransferRelation extends SingleEdgeTransferRelation {

  private final CFA cfa;
  private final LogManager logger;
  private final LoopPatternFinder loopPatternFinder;
  private final FormulaToAcslConverter formulaConverter;
  private final Level logLevel = Level.FINER; // TODO change this to INFO while debugging

  public AcslTransferRelation(
      CFA pCFA, LogManager pLogManager, FormulaToAcslConverter pFormulaConverter) {
    this.cfa = pCFA;
    this.logger = pLogManager;
    this.loopPatternFinder = new LoopPatternFinder(logger);
    this.formulaConverter = pFormulaConverter;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {

    logger.log(
        logLevel, "[ACSL] Transfer: " + cfaEdge.getPredecessor() + " -> " + cfaEdge.getSuccessor());

    if (cfaEdge.getSuccessor().isLoopStart()) {
      // TODO
    }
    return ImmutableList.of(state);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState state,
      Iterable<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision precision)
      throws CPATransferException, InterruptedException {

    for (AbstractState otherState : otherStates) {
      if (otherState instanceof PredicateAbstractState predicateState) {
        loopPatternFinder
            .detect(cfaEdge, predicateState)
            .ifPresent(
                arrayInit -> {
                  logger.log(logLevel, "Detected array initialization: " + arrayInit);
                });

        System.out.println(
            "Edge:"
                + cfaEdge
                + " Predicate state: "
                + predicateState.getPathFormula().getFormula());
        logger.log(logLevel, predicateState.getPathFormula().getFormula());
        try {
          // TODO This does not work with select and store yet, and needs to be fixed
          AcslPredicate pred =
              formulaConverter.formulaToAcslExpression(
                  predicateState.getPathFormula().getFormula());
          System.out.println("Converted to ACSL: " + pred);
        } catch (Exception pE) {
          logger.log(
              Level.WARNING, "Error converting formula to Acsl expression: " + pE.getMessage());
        }
      }
      if (otherState instanceof ValueAnalysisState valueState) {
        logger.log(logLevel, valueState.toString());
      }
    }

    return super.strengthen(state, otherStates, cfaEdge, precision);
  }
}
