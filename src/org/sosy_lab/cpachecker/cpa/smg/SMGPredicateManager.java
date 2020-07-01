/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.smg;

import java.math.BigInteger;
import java.util.Map.Entry;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cpa.smg.graphs.PredRelation;
import org.sosy_lab.cpachecker.cpa.smg.graphs.PredRelation.ExplicitRelation;
import org.sosy_lab.cpachecker.cpa.smg.graphs.PredRelation.SymbolicRelation;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.smt.BitvectorFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "cpa.smg")
public class SMGPredicateManager {
  private static final String SYM_NAME = "Sym_";

  @Option(secure=true, name="verifyPredicates", description = "Allow SMG to check predicates")
  private boolean verifyPredicates = false;

  private final Configuration config;
  private final LogManager logger;
  private final Solver solver;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final BitvectorFormulaManagerView efmgr;


  public SMGPredicateManager(Configuration pConfig, LogManager pLogger, ShutdownNotifier
      shutdownNotifier)
      throws InvalidConfigurationException {
    config = pConfig;
    config.inject(this);
    logger = pLogger;
    solver = Solver.create(pConfig, pLogger,shutdownNotifier);
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    efmgr = fmgr.getBitvectorFormulaManager();
  }

  private BooleanFormula createBooleanFormula(
      Formula pFormulaOne, Formula pFormulaTwo, BinaryOperator pOp) {
    switch (pOp) {
      case GREATER_THAN:
        return fmgr.makeGreaterThan(pFormulaOne, pFormulaTwo, true);
      case GREATER_EQUAL:
        return fmgr.makeGreaterOrEqual(pFormulaOne, pFormulaTwo, true);
      case LESS_THAN:
        return fmgr.makeLessThan(pFormulaOne, pFormulaTwo, true);
      case LESS_EQUAL:
        return fmgr.makeLessOrEqual(pFormulaOne, pFormulaTwo, true);
      case EQUALS:
        return fmgr.makeEqual(pFormulaOne, pFormulaTwo);
      case NOT_EQUALS:
        return bfmgr.not(fmgr.makeEqual(pFormulaOne, pFormulaTwo));
      default:
        throw new AssertionError();
    }
  }

  private BooleanFormula addPredicateToFormula(BooleanFormula pFormula, ExplicitRelation
      pRelation, PredRelation pPredRelationRelation, boolean conjunction) {
    BooleanFormula result;
    BigInteger explicitValue = pRelation.getExplicitValue().getValue();
    int explicitSize = explicitValue.bitLength();
    int symbolicSize = pPredRelationRelation.getSymbolicSize(pRelation.getSymbolicValue());
    BinaryOperator op = pRelation.getOperator();
    if (explicitSize > symbolicSize && op.equals(BinaryOperator.GREATER_THAN)) {
      result = bfmgr.makeFalse();
    } else {
      BitvectorFormula explicitValueFormula = efmgr.makeBitvector(symbolicSize, explicitValue);
      String name = SYM_NAME + pRelation.getSymbolicValue();
      Formula symbolicValue = efmgr.makeVariable(symbolicSize, name);
      result = createBooleanFormula(symbolicValue, explicitValueFormula, op);
    }
    if (conjunction) {
      result = bfmgr.and(result, pFormula);
    } else {
      result = bfmgr.or(result, pFormula);
    }
    return result;
  }

  private BooleanFormula addPredicateToFormula(BooleanFormula pFormula, SymbolicRelation
      pRelation, PredRelation pPredRelation, boolean conjunction) {
    BooleanFormula result;
    String nameOne = SYM_NAME + pRelation.getFirstValue();
    String nameTwo = SYM_NAME + pRelation.getSecondValue();
    Integer firstSize = pPredRelation.getSymbolicSize(pRelation.getFirstValue());
    Integer secondSize = pPredRelation.getSymbolicSize(pRelation.getSecondValue());
    BitvectorFormula formulaOne;
    BitvectorFormula formulaTwo;
    // Special case for NULL value
    if (pRelation.getFirstValue().isZero()) {
      firstSize = secondSize;
      formulaOne = efmgr.makeBitvector(firstSize, 0);
    } else {
      formulaOne = efmgr.makeVariable(firstSize, nameOne);
    }
    if (pRelation.getSecondValue().isZero()) {
      secondSize = firstSize;
      formulaTwo = efmgr.makeBitvector(firstSize, 0);
    } else {
      formulaTwo = efmgr.makeVariable(secondSize, nameTwo);
    }
    if (!firstSize.equals(secondSize)) {
      if (firstSize > secondSize) {
        formulaTwo = efmgr.extend(formulaTwo, firstSize - secondSize, true);
      } else {
        formulaOne = efmgr.extend(formulaOne, secondSize - firstSize, true);
      }
    }
    BinaryOperator op = pRelation.getOperator();
    result = createBooleanFormula(formulaOne, formulaTwo, op);
    if (conjunction) {
      result = fmgr.makeAnd(pFormula, result);
    } else {
      result = fmgr.makeOr(pFormula, result);
    }
    return result;
  }

  public BooleanFormula getPredicateFormula(PredRelation pRelation) {
    return getPredicateFormula(pRelation, true);
  }

  public BooleanFormula getErrorPredicateFormula(PredRelation pErrorPredicate, PredRelation
      pPathRelation) {
    BooleanFormula errorFormula = getPredicateFormula(pErrorPredicate, false);
    BooleanFormula pathFormula = getPredicateFormula(pPathRelation, true);
    return fmgr.makeAnd(pathFormula, errorFormula);
  }

  private BooleanFormula getPredicateFormula(PredRelation pRelation, boolean conjunction) {
    BooleanFormula result = bfmgr.makeBoolean(conjunction);

    if (!verifyPredicates) {
      return result;
    }

    for (Entry<Pair<SMGValue, SMGValue>, SymbolicRelation> entry : pRelation.getValuesRelations()) {
      if (entry.getKey().getSecond().compareTo(entry.getKey().getFirst()) > 0) {
        SymbolicRelation value = entry.getValue();
        result = addPredicateToFormula(result, value, pRelation, conjunction);
      }
    }

    for (ExplicitRelation relation : pRelation.getExplicitRelations()) {
      result = addPredicateToFormula(result, relation, pRelation, conjunction);
    }
    return result;
  }

  public boolean isUnsat(BooleanFormula pFormula) throws SolverException, InterruptedException {
    if (verifyPredicates && pFormula != null) {
      boolean result = solver.isUnsat(pFormula);
      if (result) {
        logger.log(Level.FINER, "Unsat: " + pFormula);
      }
      return result;
    } else {
      return false;
    }
  }

  public boolean isErrorPathFeasible(UnmodifiableSMGState pState) {
    if (!verifyPredicates) {
      return false;
    }

    PredRelation errorPredicate = pState.getErrorPredicateRelation();
    if (!errorPredicate.isEmpty()) {
      BooleanFormula errorPredicateFormula = getErrorPredicateFormula(errorPredicate, pState.getPathPredicateRelation());
      try {
        if (!isUnsat(errorPredicateFormula)) {
          logger.log(Level.FINER, "Sat: ", errorPredicateFormula);
          return true;
        } else {
          return false;
        }
      } catch (SolverException pE) {
        logger.log(Level.WARNING, "Solver Exception: " + pE + " on predicate " + errorPredicate);
      } catch (InterruptedException pE) {
        logger.log(Level.WARNING, "Solver Interrupted Exception: " + pE + " on predicate " +
            errorPredicate);
      }
    }

    return !errorPredicate.isEmpty();
  }
}