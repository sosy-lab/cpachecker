/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.util.predicates.smt;

import java.util.List;

import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.visitors.DefaultFormulaVisitor;
import org.sosy_lab.solver.visitors.FormulaVisitor;

import com.google.common.base.Function;

/**
 * Extended variant of {@link DefaultFormulaVisitor} that has more individual methods.
 * Every method of this class is allowed to be overriden.
 * By default, all methods delegate to {@link #visitDefault(Formula)},
 * or to a more specific method of this class.
 * @param <R> Desired return type.
 */
public abstract class ExtendedFormulaVisitor<R> extends DefaultFormulaVisitor<R> {

  /**
   * Visit an uninterpreted function.
   *
   * @param f Input function.
   * @param args List of arguments
   * @param functionName Name of the function (such as "and" or "or")
   * @param newApplicationConstructor Construct a new function of the same type,
   *                                  with the new arguments as given.
   */
  protected R visitUF(
      Formula f,
      List<Formula> args,
      String functionName,
      Function<List<Formula>, Formula> newApplicationConstructor) {
    return visitDefault(f);
  }

  /**
   * Visit an interpreted function, i.e. a function where the solver applies some semantics.
   * This includes all known operators such as "and", "or", "+", "=", etc.
   * (with the exception of "if-then-else").
   *
   * @param f Input function.
   * @param args List of arguments
   * @param functionName Name of the function (such as "and" or "or")
   * @param newApplicationConstructor Construct a new function of the same type,
   *                                  with the new arguments as given.
   */
  protected R visitOperator(
      Formula f,
      List<Formula> args,
      String functionName,
      Function<List<Formula>, Formula> newApplicationConstructor) {
    return visitDefault(f);
  }

  /**
   * Visit an interpreted function, i.e. a function where the solver applies some semantics.
   * This includes all known operators such as "and", "or", "+", "=", etc.
   * (with the exception of "if-then-else").
   *
   * @param f The whole if-then-else formula.
   * @param condition The condition part of the if-then-else formula.
   * @param thenBranch The "then" part  of the if-then-else formula.
   * @param elseBranch The "else" part  of the if-then-else formula.
   */
  protected R visitIfThenElse(
      Formula f, BooleanFormula condition, Formula thenBranch, Formula elseBranch) {
    return visitDefault(f);
  }

  /**
   * By default, this method delegates to {@list #visitUF(Formula, List, String, Function)},
   * {@link #visitOperator(Formula, List, String, Function)},
   * or {@link #visitIfThenElse(Formula, BooleanFormula, Formula, Formula)}.
   *
   * @see FormulaVisitor#visitFunction(Formula, List, String, Function, boolean)
   */
  @Override
  public R visitFunction(
      Formula f,
      List<Formula> args,
      String functionName,
      Function<List<Formula>, Formula> newApplicationConstructor,
      boolean isUF) {
    if (isUF) {
      return visitUF(f, args, functionName, newApplicationConstructor);

    } else if (functionName.equals("ite")) {
      assert args.size() == 3;
      assert args.get(0) instanceof BooleanFormula;
      return visitIfThenElse(f, (BooleanFormula) args.get(0), args.get(1), args.get(2));

    } else {
      return visitUF(f, args, functionName, newApplicationConstructor);
    }
  }
}
