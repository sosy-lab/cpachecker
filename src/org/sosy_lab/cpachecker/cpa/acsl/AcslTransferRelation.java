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
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

@SuppressWarnings("unused")
public class AcslTransferRelation extends SingleEdgeTransferRelation {

  private final CFA cfa;
  private final LogManager logger;
  private final LoopPatternFinder loopPatternFinder;
  private final FormulaToAcslConverter formulaConverter;
  private final FormulaManagerView fmgr;
  private final Level logLevel = Level.FINER; // TODO change this to INFO while debugging

  public AcslTransferRelation(
      CFA pCFA,
      LogManager pLogManager,
      FormulaToAcslConverter pFormulaConverter,
      FormulaManagerView pFmgr) {
    this.cfa = pCFA;
    this.logger = pLogManager;
    this.loopPatternFinder = new LoopPatternFinder(logger, pFormulaConverter);
    this.formulaConverter = pFormulaConverter;
    this.fmgr = pFmgr;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {

    logger.log(
        logLevel, "[ACSL] Transfer: " + cfaEdge.getPredecessor() + " -> " + cfaEdge.getSuccessor());

    if (cfa.getAllLoopHeads().get().contains(cfaEdge.getSuccessor())) {
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

    if (cfa.getAllLoopHeads().isEmpty() || cfa.getAllLoopHeads().get().size() > 1) {
      logger.log(
          Level.INFO, "Generating ACSL Loop invariants with number of loop heads != 1 impossible");
      return super.strengthen(state, otherStates, cfaEdge, precision);
    }

    AbstractState updatedState = state;
    for (AbstractState otherState : otherStates) {

      if (otherState instanceof PredicateAbstractState predicateState) {

        if (state instanceof AcslState acslState
            && cfaEdge != null
            && !acslState
                .getInvariantsForNode(cfaEdge.getSuccessor(), ArrayInitialization.class)
                .isEmpty()) {
          logger.log(
              logLevel,
              "Skipping detection of array initialization, as one already exists for the node.");
        } else {
          Optional<ArrayInitialization> detected =
              loopPatternFinder.detect(cfaEdge, cfa.getLoopStructure(), predicateState);

          if (detected.isPresent()) {
            ArrayInitialization init = detected.get();
            logger.log(Level.INFO, "SUCCESS: Detected array initialization: " + init);

            CProgramScope scope = new CProgramScope(cfa, logger);
            AcslPredicate inv = detected.get().toInvariant(scope, FileLocation.DUMMY, logger);
            logger.log(Level.INFO, "Generated ACSL invariant: " + inv);

            if (updatedState instanceof AcslState acslState) {
              updatedState = acslState.withAddedInvariant(cfaEdge.getSuccessor(), init);
            }
          }
        }
      }
      if (otherState instanceof ValueAnalysisState valueState) {
        logger.log(logLevel, valueState.toString());
      }
    }

    return super.strengthen(updatedState, otherStates, cfaEdge, precision);
  }
}
