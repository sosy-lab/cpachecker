// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;
import org.sosy_lab.java_smt.api.visitors.DefaultFormulaVisitor;

/**
 * Creates a map of program variables to concrete values if the value can be known from the path
 * formula. This Visitor creates the Map via side effects.
 *
 * <p>This is a temporary solution for lemmas in predicate abstraction. It should not be used
 * anywhere else!
 */
public class ConcreteValueExtractionVisitor
    extends DefaultFormulaVisitor<ImmutableMap<Formula, Formula>> {

  private final FormulaManager fmgr;
  private Formula variable;
  private Formula concreteValue;
  private final ImmutableMap.Builder<Formula, Formula> valueMap;

  public ConcreteValueExtractionVisitor(FormulaManager pFmgr) {
    fmgr = pFmgr;
    variable = null;
    concreteValue = null;
    valueMap = ImmutableMap.builder();
  }

  @Override
  public ImmutableMap<Formula, Formula> visitFunction(
      Formula f, List<Formula> args, FunctionDeclaration<?> functionDeclaration) {
    variable = null;
    concreteValue = null;

    // We are looking for (EQ <Bitvektor> <Bitvektor>)
    if (functionDeclaration.getKind().equals(FunctionDeclarationKind.EQ)
        && args.size() == 2
        && args.get(0) instanceof BitvectorFormula
        && args.get(1) instanceof BitvectorFormula) {

      for (Formula arg : args) {
        fmgr.visit(arg, this);
      }
      if (variable != null && concreteValue != null) {
        valueMap.put(variable, concreteValue);
      }
    } else {
      // Otherwise just unroll the formula
      for (Formula arg : args) {
        fmgr.visit(arg, this);
      }
    }
    ImmutableMap<Formula, Formula> result = valueMap.buildKeepingLast();
    return result;
  }

  @Override
  public ImmutableMap<Formula, Formula> visitFreeVariable(Formula f, String name) {
    // For a function EQ this is the key, if the other argument is a constant
    variable = f;
    return ImmutableMap.of();
  }

  @Override
  public ImmutableMap<Formula, Formula> visitConstant(Formula f, Object value) {
    // For a EQ-function this is the concrete value
    concreteValue = f;
    return ImmutableMap.of();
  }

  @Override
  protected ImmutableMap<Formula, Formula> visitDefault(Formula pFormula) {
    return ImmutableMap.of();
  }
}
