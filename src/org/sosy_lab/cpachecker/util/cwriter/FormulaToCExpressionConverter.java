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
package org.sosy_lab.cpachecker.util.cwriter;

import com.google.common.base.Joiner;

import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.Tactic;
import org.sosy_lab.java_smt.api.visitors.DefaultFormulaVisitor;

import java.util.List;

/**
 * Class for converting a formula to a C expression.
 *
 * <p>Uses recursion, hence would not work on very large formulas.
 */
public class FormulaToCExpressionConverter {
  private final FormulaManagerView fmgr;

  // TODO: do not hardcode the function separator.
  private static final String FUNCTION_NAME_SEPARATOR = "::";

  public FormulaToCExpressionConverter(FormulaManagerView pFmgr) {
    fmgr = pFmgr;
  }

  /**
   * Convert the input formula to a C expression.
   */
  public String formulaToCExpression(BooleanFormula input) throws InterruptedException {
    BooleanFormula nnfied = fmgr.applyTactic(input, Tactic.NNF);
    BooleanFormula simplified = fmgr.simplify(nnfied);
    return recFormulaToCExpression(simplified);
  }

  private String recFormulaToCExpression(Formula invariant) {
    return fmgr.visit(invariant, new DefaultFormulaVisitor<String>() {

      @Override
      protected String visitDefault(Formula f) {
        throw new UnsupportedOperationException("Unexpected constraint");
      }

      @Override
      public String visitFreeVariable(Formula f, String name) {

        if (name.contains(FUNCTION_NAME_SEPARATOR)) {
          return name.split(FUNCTION_NAME_SEPARATOR)[1];
        }
        return name;
      }

      @Override
      public String visitConstant(Formula f, Object value) {
        return value.toString();
      }

      @Override
      public String visitFunction(
          Formula f,
          List<Formula> args,
          FunctionDeclaration<?> functionDeclaration) {
        switch (functionDeclaration.getKind()) {
          case NOT:
          case UMINUS:
            return String.format("%s(%s)",
                operatorFromFunctionDeclaration(functionDeclaration),
                recFormulaToCExpression(args.get(0)));
          default:
            return joinWithSeparator(
                operatorFromFunctionDeclaration(functionDeclaration), args);
        }
      }

      private String operatorFromFunctionDeclaration(FunctionDeclaration<?> pDeclaration) {
        switch (pDeclaration.getKind()) {
          case NOT:
            return "!";
          case UMINUS:
            return "- ";
          case AND:
            return " && ";
          case OR:
            return "\n|| ";
          case SUB:
            return " - ";
          case ADD:
            return " + ";
          case DIV:
            return " / ";
          case MUL:
            return " * ";
          case MODULO:
            return " % ";
          case LT:
            return " < ";
          case LTE:
            return " <= ";
          case GT:
            return " > ";
          case GTE:
            return " >= ";
          case EQ:
            return " == ";
          default:
            throw new UnsupportedOperationException("Unexpected operand");
        }
      }

      private String joinWithSeparator(String separator, List<Formula> args) {
        return "("
            + Joiner.on(separator).join(
              args.stream().map(c ->
                  String.format("%s", recFormulaToCExpression(c))).iterator()
            )
            + ")";
      }
    });
  }
}
