/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.smtInterpol;

import static com.google.common.collect.Iterables.getOnlyElement;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.Appenders;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smtInterpol.SmtInterpolEnvironment.Type;

import de.uni_freiburg.informatik.ultimate.logic.AnnotatedTerm;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.FormulaLet;
import de.uni_freiburg.informatik.ultimate.logic.FunctionSymbol;
import de.uni_freiburg.informatik.ultimate.logic.Logics;
import de.uni_freiburg.informatik.ultimate.logic.PrintTerm;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;

class SmtInterpolFormulaManager extends AbstractFormulaManager<Term, Sort, SmtInterpolEnvironment> {

  private SmtInterpolFormulaManager(
      SmtInterpolEnvironment pEnv,
      SmtInterpolFormulaCreator pCreator,
      SmtInterpolUnsafeFormulaManager pUnsafeManager,
      SmtInterpolFunctionFormulaManager pFunctionManager,
      SmtInterpolBooleanFormulaManager pBooleanManager,
      SmtInterpolIntegerFormulaManager pIntegerManager,
      SmtInterpolRationalFormulaManager pRationalManager) {
    super(pEnv, pCreator, pUnsafeManager, pFunctionManager, pBooleanManager, pIntegerManager, pRationalManager, null);
  }

  public static SmtInterpolFormulaManager create(Configuration config, LogManager logger,
      ShutdownNotifier pShutdownNotifier, boolean pUseIntegers) throws InvalidConfigurationException {

    Logics logic = pUseIntegers ? Logics.QF_UFLIA : Logics.QF_UFLIRA;
    SmtInterpolEnvironment env = new SmtInterpolEnvironment(config, logic, logger, pShutdownNotifier);

    final Sort integerType = env.sort(Type.INT);
    final Sort realType = env.sort(Type.REAL);
    SmtInterpolFormulaCreator creator = new SmtInterpolFormulaCreator(env, env.sort(Type.BOOL), integerType, realType);

    // Create managers
    SmtInterpolUnsafeFormulaManager unsafeManager = new SmtInterpolUnsafeFormulaManager(creator);
    SmtInterpolFunctionFormulaManager functionTheory = new SmtInterpolFunctionFormulaManager(creator, unsafeManager);
    SmtInterpolBooleanFormulaManager booleanTheory = new SmtInterpolBooleanFormulaManager(creator, env.getTheory());
    SmtInterpolIntegerFormulaManager integerTheory = new SmtInterpolIntegerFormulaManager(creator, functionTheory);
    SmtInterpolRationalFormulaManager rationalTheory = new SmtInterpolRationalFormulaManager(creator, functionTheory);

    return new SmtInterpolFormulaManager(env, creator, unsafeManager, functionTheory,
            booleanTheory, integerTheory, rationalTheory);
  }

  public SmtInterpolInterpolatingProver createInterpolator() {
    return getEnvironment().getInterpolator(this);
  }

  SmtInterpolTheoremProver createProver() {
    return getEnvironment().createProver(this);
  }

  BooleanFormula encapsulateBooleanFormula(Term t) {
    return getFormulaCreator().encapsulate(BooleanFormula.class, t);
  }

  @Override
  public BooleanFormula parse(String pS) throws IllegalArgumentException {
    return encapsulateBooleanFormula(getOnlyElement(getEnvironment().parseStringToTerms(pS)));
  }


  @Override
  public Appender dumpFormula(final Term formula) {
    return new Appenders.AbstractAppender() {

      @Override
      public void appendTo(Appendable out) throws IOException {
        Set<Term> seen = new HashSet<>();
        Deque<Term> todo = new ArrayDeque<>();
        PrintTerm termPrinter = new PrintTerm();

        todo.addLast(formula);

        while (!todo.isEmpty()) {
          Term t = todo.removeLast();
          while (t instanceof AnnotatedTerm) {
            t = ((AnnotatedTerm) t).getSubterm();
          }
          if (!(t instanceof ApplicationTerm)
              || !seen.add(t)) {
            continue;
          }

          ApplicationTerm term = (ApplicationTerm)t;
          Collections.addAll(todo, term.getParameters());

          FunctionSymbol func = term.getFunction();
          if (func.isIntern()) {
            continue;
          }

          if (func.getDefinition() == null) {
            out.append("(declare-fun ");
            out.append(PrintTerm.quoteIdentifier(func.getName()));
            out.append(" (");
            for (Sort paramSort : func.getParameterSorts()) {
              termPrinter.append(out, paramSort);
              out.append(' ');
            }
            out.append(") ");
            termPrinter.append(out, func.getReturnSort());
            out.append(")\n");

          } else {
            // We would have to print a (define-fun) command and
            // recursively traverse into func.getDefinition() (in post-order!).
            // However, such terms should actually not occur.
            throw new IllegalArgumentException("Terms with definition are unsupported.");
          }
        }

        out.append("(assert ");

        // This is the same as t.toString() does,
        // but directly uses the Appendable for better performance
        // and less memory consumption.
        Term letted = (new FormulaLet()).let(formula);
        termPrinter.append(out, letted);

        out.append(")");
      }
    };
  }

  @Override
  public String getVersion() {
    return getEnvironment().getVersion();
  }

  /** This method returns a 'shared' environment or
   * a complete new environment. */
  SmtInterpolEnvironment createEnvironment() {
    assert getEnvironment() != null;
    return getEnvironment();
  }

  @Override
  protected Term getTerm(Formula pF) {
    // for visibility
    return super.getTerm(pF);
  }
}
