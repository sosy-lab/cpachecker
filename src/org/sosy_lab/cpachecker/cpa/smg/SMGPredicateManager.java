// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGPredRelation;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGPredRelation.ExplicitRelation;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGPredRelation.SymbolicRelation;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGType;
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
  private final Map<String, BitvectorFormula> variables;
  private final Map<String, SMGType> variableSizes;

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
    variables = new HashMap<>();
    variableSizes = new HashMap<>();
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

  private BooleanFormula addPredicateToFormula(
      BooleanFormula pFormula, ExplicitRelation pRelation, boolean conjunction) {
    BooleanFormula result;
    BigInteger explicitValue = pRelation.getExplicitValue().getValue();
    SMGType symbolicSMGType = pRelation.getSymbolicSMGType();
    int explicitSize = symbolicSMGType.getSize();
    boolean isExplicitSigned = symbolicSMGType.isSigned();
    BinaryOperator op = pRelation.getOperator();

    BitvectorFormula explicitValueFormula = efmgr.makeBitvector(explicitSize + 1, explicitValue);
    BitvectorFormula explicitValueFormulaCasted =
        efmgr.extract(explicitValueFormula, explicitSize - 1, 0, isExplicitSigned);

    String name = SYM_NAME + pRelation.getSymbolicValue();
    BitvectorFormula symbolicValue = getCastedVariable(name, symbolicSMGType);
    result = createBooleanFormula(symbolicValue, explicitValueFormulaCasted, op);

    if (conjunction) {
      result = bfmgr.and(result, pFormula);
    } else {
      result = bfmgr.or(result, pFormula);
    }
    return result;
  }

  private BitvectorFormula getCastedVariable(String pName, SMGType pSMGType) {
    BitvectorFormula variableFormula = variables.get(pName);
    if (variableFormula == null) {
      int size = pSMGType.getOriginSize();
      boolean isSigned = pSMGType.getOriginSigned();
      variableFormula = efmgr.makeVariable(size, pName);
      variableFormula = efmgr.extend(variableFormula, 0, isSigned);
      variables.put(pName, variableFormula);
      variableSizes.put(pName, pSMGType);
    }
    variableFormula = cast(variableFormula, variableSizes.get(pName), pSMGType);
    return variableFormula;
  }

  private BitvectorFormula cast(
      BitvectorFormula pVariableFormula, SMGType pFromSMGType, SMGType pToSMGType) {
    BitvectorFormula result;
    int toSize = pToSMGType.getSize();
    int fromSize = pFromSMGType.getOriginSize();
    boolean isToSigned = pToSMGType.isSigned();
    boolean isFromSigned = pFromSMGType.isSigned();
    result = pVariableFormula;
    if (toSize > fromSize) {
      result = efmgr.extend(result, toSize - fromSize, isToSigned);
    } else if (toSize < fromSize) {
      result = efmgr.extract(result, toSize - 1, 0, isToSigned);
    } else if (isToSigned != isFromSigned) {
      result = efmgr.extend(result, 0, isToSigned);
    }
    return result;
  }

  private BooleanFormula addPredicateToFormula(
      BooleanFormula pFormula, SymbolicRelation pRelation, boolean conjunction) {
    BooleanFormula result;
    String nameOne = SYM_NAME + pRelation.getFirstValue();
    String nameTwo = SYM_NAME + pRelation.getSecondValue();

    BitvectorFormula formulaOne;
    BitvectorFormula formulaTwo;

    SMGType firstValSMGType = pRelation.getFirstValSMGType();
    int firstCastedSize = firstValSMGType.getSize();

    SMGType secondValSMGType = pRelation.getSecondValSMGType();
    int secondCastedSize = secondValSMGType.getSize();

    // Special case for NULL value
    if (pRelation.getFirstValue().isZero()) {
      firstCastedSize = secondCastedSize;
      formulaOne = efmgr.makeBitvector(firstCastedSize, 0);
    } else {
      formulaOne = getCastedVariable(nameOne, firstValSMGType);
    }

    if (pRelation.getSecondValue().isZero()) {
      secondCastedSize = firstCastedSize;
      formulaTwo = efmgr.makeBitvector(secondCastedSize, 0);
    } else {
      formulaTwo = getCastedVariable(nameTwo, secondValSMGType);
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

  public BooleanFormula getPredicateFormula(SMGPredRelation pRelation) {
    return getPredicateFormula(pRelation, true);
  }

  public BooleanFormula getErrorPredicateFormula(
      SMGPredRelation pErrorPredicate, SMGPredRelation pPathRelation) {
    BooleanFormula errorFormula = getPredicateFormula(pErrorPredicate, false);
    BooleanFormula pathFormula = getPredicateFormula(pPathRelation, true);
    return fmgr.makeAnd(pathFormula, errorFormula);
  }

  private BooleanFormula getPredicateFormula(SMGPredRelation pRelation, boolean conjunction) {
    BooleanFormula result = bfmgr.makeBoolean(conjunction);

    if (!verifyPredicates) {
      return result;
    }

    for (Entry<Pair<SMGValue, SMGValue>, SymbolicRelation> entry : pRelation.getValuesRelations()) {
      if (entry.getKey().getSecond().compareTo(entry.getKey().getFirst()) > 0) {
        SymbolicRelation value = entry.getValue();
        result = addPredicateToFormula(result, value, conjunction);
      }
    }

    for (ExplicitRelation relation : pRelation.getExplicitRelations()) {
      result = addPredicateToFormula(result, relation, conjunction);
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

    SMGPredRelation errorPredicate = pState.getErrorPredicateRelation();
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