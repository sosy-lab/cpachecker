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
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.smt.BitvectorFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

import java.math.BigInteger;
import java.util.Map.Entry;
import java.util.logging.Level;

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
      Formula pFormulaOne,
      Formula pFormulaTwo,
      BinaryOperator pOp) {
    BooleanFormula result;
    switch (pOp) {
      case GREATER_THAN:
        result = fmgr.makeGreaterThan(pFormulaOne, pFormulaTwo, true);
        break;
      case GREATER_EQUAL:
        result = fmgr.makeGreaterOrEqual(pFormulaOne, pFormulaTwo, true);
        break;
      case LESS_THAN:
        result = fmgr.makeLessThan(pFormulaOne, pFormulaTwo, true);
        break;
      case LESS_EQUAL:
        result = fmgr.makeLessOrEqual(pFormulaOne, pFormulaTwo, true);
        break;
      case EQUALS:
        result = fmgr.makeEqual(pFormulaOne, pFormulaTwo);
        break;
      case NOT_EQUALS:
        result = bfmgr.not(fmgr.makeEqual(pFormulaOne, pFormulaTwo));
        break;
      default:
        throw new AssertionError();

    }
    return result;
  }

  private BooleanFormula addPredicateToFormula(BooleanFormula pFormula, ExplicitRelation
      pRelation, PredRelation pPredRelationRelation, boolean conjunction) {
    BooleanFormula result;
    BigInteger explicitValue = pRelation.getExplicitValue().getValue();
    BitvectorFormula explicitValueFormula = efmgr.makeBitvector(pPredRelationRelation.getSymbolicSize(pRelation
        .getSymbolicValue()).intValue(), explicitValue);
    String name = SYM_NAME + pRelation.getSymbolicValue();
    Formula symbolicValue = efmgr.makeVariable(pPredRelationRelation.getSymbolicSize(pRelation
        .getSymbolicValue()),
        name);
    BinaryOperator op = pRelation.getOperator();
    result = createBooleanFormula(symbolicValue, explicitValueFormula, op);
    if (conjunction) {
      result = bfmgr.and(result, pFormula);
    } else {
      result = bfmgr.or(result, pFormula);
    }
    return result;
  }

  private BooleanFormula addPredicateToFormula(BooleanFormula pFormula, SymbolicRelation
      pRelation, PredRelation pPredRelationRelation, boolean conjunction) {
    BooleanFormula result;
    String nameOne = SYM_NAME + pRelation.getFirstValue();
    String nameTwo = SYM_NAME + pRelation.getSecondValue();
    Integer firstSize = pPredRelationRelation.getSymbolicSize(pRelation.getFirstValue());
    Integer secondSize = pPredRelationRelation.getSymbolicSize(pRelation.getSecondValue());
    //Special case for NULL value
    if (pRelation.getFirstValue() == 0) {
      firstSize = secondSize;
    }
    if (pRelation.getSecondValue() == 0) {
      secondSize = firstSize;
    }
    Formula formulaOne = efmgr.makeVariable(firstSize, nameOne);
    Formula formulaTwo = efmgr.makeVariable(secondSize, nameTwo);
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

    for (Entry<Pair<Integer, Integer>, SymbolicRelation> entry: pRelation.getValuesRelations()) {
      if (entry.getKey().getSecond() > entry.getKey().getFirst()) {
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

  public boolean isErrorPathFeasible(SMGState pState) {
    PredRelation errorPredicate = pState.getErrorPredicateRelation();
    if (verifyPredicates && !errorPredicate.isEmpty()) {
      BooleanFormula errorPredicateFormula = getErrorPredicateFormula(errorPredicate, pState.getPathPredicateRelation());
      try {
        if (!isUnsat(errorPredicateFormula)) {
          logger.log(Level.INFO, "Sat: ", errorPredicateFormula);
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