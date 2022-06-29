// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.assumptions.storage;

import static org.sosy_lab.java_smt.api.FormulaType.getSinglePrecisionFloatingPointType;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AssumptionReportingState;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AvoidanceReportingState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisStateWithSavedValue;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAEdgeUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;
import org.sosy_lab.cpachecker.util.expressions.Or;
import org.sosy_lab.cpachecker.util.expressions.ToFormulaVisitor;
import org.sosy_lab.cpachecker.util.expressions.ToFormulaVisitor.ToFormulaException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FloatingPointFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.IntegerFormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType.FloatingPointType;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

/** Transfer relation and strengthening for the DumpInvariant CPA */
@Options(prefix = "cpa.assumptionStorage")
public class AssumptionStorageTransferRelation extends SingleEdgeTransferRelation {

  @Option(
      secure = true,
      description =
          "If it is enabled, assumptions are extracted from the value analysis states,"
              + "if the ValueAnalyssi loads TESTCOMP-testcases.")
  private boolean extractAssumptionsFromValueAnalysisState = false;

  @Option(
      secure = true,
      description =
          "If it is enabled, assumptions are extracted from the correctness-witness state"
              + " invariants")
  private boolean extractAssumptionsFromAutomatonState = false;

  private final CtoFormulaConverter converter;
  private final FormulaManagerView formulaManager;

  private final LogManager logger;
  private final Collection<AbstractState> topStateSet;
  private final PathFormulaManager pathFormulaManager;

  public AssumptionStorageTransferRelation(
      CtoFormulaConverter pConverter,
      FormulaManagerView pFormulaManager,
      AbstractState pTopState,
      Configuration pConfig,
      PathFormulaManager pPathFormulaManager,
      LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    converter = pConverter;
    formulaManager = pFormulaManager;
    topStateSet = Collections.singleton(pTopState);
    pathFormulaManager = pPathFormulaManager;
    logger = pLogger;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pElement, Precision pPrecision, CFAEdge pCfaEdge) {
    AssumptionStorageState element = (AssumptionStorageState) pElement;

    // If we must stop, then let's stop by returning an empty set
    if (element.isStop()) {
      return ImmutableSet.of();
    }

    return topStateSet;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState el, Iterable<AbstractState> others, CFAEdge edge, Precision p)
      throws CPATransferException, InterruptedException {
    AssumptionStorageState asmptStorageElem = (AssumptionStorageState) el;
    return Collections.singleton(strengthen(asmptStorageElem, others, edge));
  }

  AssumptionStorageState strengthen(
      AssumptionStorageState pAsmptStorageElem, Iterable<AbstractState> pOthers, CFAEdge pEdge)
      throws UnrecognizedCodeException, InterruptedException {
    BooleanFormulaManagerView bfmgr = formulaManager.getBooleanFormulaManager();

    String function = pEdge.getSuccessor().getFunctionName();

    BooleanFormula assumption = pAsmptStorageElem.getAssumption();
    BooleanFormula stopFormula = pAsmptStorageElem.getStopFormula();
    if (bfmgr.isTrue(stopFormula)) {
      // if there is no avoidance condition,
      // initialize with false because we create a disjunction over possible
      // new conditions below
      stopFormula = bfmgr.makeFalse();
    }

    // process stop flag
    boolean stop = false;

    for (AbstractState element : AbstractStates.asFlatIterable(pOthers)) {
      if (element instanceof AssumptionReportingState) {
        List<CExpression> assumptions = ((AssumptionReportingState) element).getAssumptions();
        for (CExpression inv : assumptions) {
          BooleanFormula invFormula =
              converter.makePredicate(inv, pEdge, function, SSAMap.emptySSAMap().builder());
          assumption = bfmgr.and(assumption, formulaManager.uninstantiate(invFormula));
        }
      }

      if (element instanceof AvoidanceReportingState) {
        AvoidanceReportingState e = (AvoidanceReportingState) element;

        if (e.mustDumpAssumptionForAvoidance()) {
          stopFormula = bfmgr.or(stopFormula, e.getReasonFormula(formulaManager));
          stop = true;
        }
      } else if (extractAssumptionsFromValueAnalysisState
          && element instanceof ValueAnalysisStateWithSavedValue
          && ((ValueAnalysisStateWithSavedValue) element).getValueFromLastIteration() != null) {
        // We have a AssumptionStorage State and we added an value of the testcomp testcase, hence
        // store this as assumption
        // Firstly, get the value from the current state:
        Value curValue =
            ((ValueAnalysisStateWithSavedValue) element).getValueFromLastIteration();
        ALeftHandSide lhs = CFAEdgeUtils.getLeftHandSide(pEdge);
        if (lhs instanceof CIdExpression) {
          CIdExpression idExpr = (CIdExpression) lhs;
          if (curValue instanceof NumericValue) {
            Pair<Formula, Formula> formulaAndNumber =
                getVariableAndNumFormula((NumericValue) curValue, idExpr.getName());
            BooleanFormula addAssumption =
                formulaManager.makeEqual(formulaAndNumber.getFirst(), formulaAndNumber.getSecond());
            assumption = bfmgr.and(addAssumption, assumption);
          } else if (curValue instanceof BooleanValue) {
            BooleanFormulaManagerView bmgr = formulaManager.getBooleanFormulaManager();
            BooleanFormula var = bmgr.makeVariable(idExpr.getName());
            BooleanFormula addAssumption =
                formulaManager.makeEqual(
                    var, ((BooleanValue) curValue).isTrue() ? bmgr.makeTrue() : bmgr.makeFalse());
            assumption = bfmgr.and(addAssumption, assumption);
          }
        }
      } else if (extractAssumptionsFromAutomatonState && element instanceof AutomatonState) {

        AutomatonState automatonState = (AutomatonState) element;

        ExpressionTree<AExpression> stateInv = automatonState.getCandidateInvariants();
        if (!ExpressionTrees.isConstant(stateInv)) {
          ToFormulaVisitor visitor = new ToFormulaVisitor(formulaManager, pathFormulaManager, null);
          try {
            if (ExpressionTrees.isAnd(stateInv)) {
              BooleanFormula invFormula = visitor.visit((And<AExpression>) stateInv);
              assumption = bfmgr.and(assumption, formulaManager.uninstantiate(invFormula));
            } else if (ExpressionTrees.isOr(stateInv)) {
              BooleanFormula invFormula = visitor.visit((Or<AExpression>) stateInv);
              assumption = bfmgr.and(assumption, formulaManager.uninstantiate(invFormula));
            } else if (ExpressionTrees.isLeaf(stateInv)) {
              BooleanFormula invFormula = visitor.visit((LeafExpression<AExpression>) stateInv);
              assumption = bfmgr.and(assumption, formulaManager.uninstantiate(invFormula));
            }
          } catch (ToFormulaException pE) {
            logger.logf(
                Level.WARNING,
                "Cannot parse the expression tree %s due to %s",
                stateInv,
                Throwables.getStackTraceAsString(pE));
          }
        }
      }
    }
    Preconditions.checkState(!bfmgr.isTrue(stopFormula));

    if (!stop) {
      stopFormula = bfmgr.makeTrue();
    }

    if (bfmgr.isTrue(assumption) && bfmgr.isTrue(stopFormula)) {
      return pAsmptStorageElem; // nothing has changed
    }
    return new AssumptionStorageState(formulaManager, assumption, stopFormula);
  }

  private Pair<Formula, Formula> getVariableAndNumFormula(
      NumericValue pNumValue, String nameOfVar) {
    Number num = pNumValue.getNumber();
    final IntegerFormulaManagerView intmgr = formulaManager.getIntegerFormulaManager();
    final FloatingPointFormulaManagerView fpmgr = formulaManager.getFloatingPointFormulaManager();
    if (num instanceof Long) {
      IntegerFormula var = intmgr.makeVariable(nameOfVar);
      return Pair.of(var, intmgr.makeNumber((Long) num));
    } else {
      final FloatingPointType singlePrecisionFloatingPointType =
          getSinglePrecisionFloatingPointType();
      if (num instanceof Float) {
        FloatingPointFormula var = fpmgr.makeVariable(nameOfVar, singlePrecisionFloatingPointType);
        return Pair.of(var, fpmgr.makeNumber((Float) num, singlePrecisionFloatingPointType));
      } else if (num instanceof Rational) {
        FloatingPointFormula var = fpmgr.makeVariable(nameOfVar, singlePrecisionFloatingPointType);
        return Pair.of(var, fpmgr.makeNumber((Rational) num, singlePrecisionFloatingPointType));
      } else if (num instanceof BigDecimal) {
        IntegerFormula var = intmgr.makeVariable(nameOfVar);
        return Pair.of(var, intmgr.makeNumber((BigDecimal) num));
      } else {
        IntegerFormula var = intmgr.makeVariable(nameOfVar);
        return Pair.of(var, intmgr.makeNumber(pNumValue.bigInteger()));
      }
    }
  }
}
