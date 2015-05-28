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

import javax.annotation.Nullable;

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.Appenders;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.PathCounterTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.OptEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractFormulaManager;

import de.uni_freiburg.informatik.ultimate.logic.AnnotatedTerm;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.FormulaLet;
import de.uni_freiburg.informatik.ultimate.logic.FormulaUnLet;
import de.uni_freiburg.informatik.ultimate.logic.FunctionSymbol;
import de.uni_freiburg.informatik.ultimate.logic.PrintTerm;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;

class SmtInterpolFormulaManager extends AbstractFormulaManager<Term, Sort, SmtInterpolEnvironment> {

  private SmtInterpolFormulaManager(
      SmtInterpolFormulaCreator pCreator,
      SmtInterpolUnsafeFormulaManager pUnsafeManager,
      SmtInterpolFunctionFormulaManager pFunctionManager,
      SmtInterpolBooleanFormulaManager pBooleanManager,
      SmtInterpolIntegerFormulaManager pIntegerManager,
      SmtInterpolRationalFormulaManager pRationalManager) {
    super(pCreator, pUnsafeManager, pFunctionManager, pBooleanManager, pIntegerManager, pRationalManager, null, null, null, null);
  }

  public static SmtInterpolFormulaManager create(Configuration config, LogManager logger,
      ShutdownNotifier pShutdownNotifier, @Nullable PathCounterTemplate smtLogfile,
      long randomSeed)
          throws InvalidConfigurationException {

    SmtInterpolEnvironment env = new SmtInterpolEnvironment(config, logger, pShutdownNotifier, smtLogfile, randomSeed);
    SmtInterpolFormulaCreator creator = new SmtInterpolFormulaCreator(env);

    // Create managers
    SmtInterpolUnsafeFormulaManager unsafeManager = new SmtInterpolUnsafeFormulaManager(creator);
    SmtInterpolFunctionFormulaManager functionTheory = new SmtInterpolFunctionFormulaManager(creator, unsafeManager);
    SmtInterpolBooleanFormulaManager booleanTheory = new SmtInterpolBooleanFormulaManager(creator, env.getTheory());
    SmtInterpolIntegerFormulaManager integerTheory = new SmtInterpolIntegerFormulaManager(creator, functionTheory);
    SmtInterpolRationalFormulaManager rationalTheory = new SmtInterpolRationalFormulaManager(creator, functionTheory);

    return new SmtInterpolFormulaManager(creator, unsafeManager, functionTheory,
            booleanTheory, integerTheory, rationalTheory);
  }

  @Override
  public ProverEnvironment newProverEnvironment(boolean pGenerateModels, boolean pGenerateUnsatCore) {
    return getEnvironment().createProver(this);
  }

  @Override
  public InterpolatingProverEnvironment<?> newProverEnvironmentWithInterpolation(boolean pShared) {
    return getEnvironment().getInterpolator(this);
  }

  @Override
  public OptEnvironment newOptEnvironment() {
    throw new UnsupportedOperationException("SMTInterpol does not support optimization");
  }

  BooleanFormula encapsulateBooleanFormula(Term t) {
    return getFormulaCreator().encapsulateBoolean(t);
  }

  @Override
  public BooleanFormula parse(String pS) throws IllegalArgumentException {
    Term term = getOnlyElement(getEnvironment().parseStringToTerms(pS));
    return encapsulateBooleanFormula(new FormulaUnLet().unlet(term));
  }


  @Override
  public Appender dumpFormula(final Term formula) {
    return new Appenders.AbstractAppender() {

      @Override
      public void appendTo(Appendable out) throws IOException {
        Set<Term> seen = new HashSet<>();
        Set<FunctionSymbol> declaredFunctions = new HashSet<>();
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
            if (declaredFunctions.add(func)) {
              out.append("(declare-fun ");
              out.append(PrintTerm.quoteIdentifier(func.getName()));
              out.append(" (");
              int counter = 0;
              for (Sort paramSort : func.getParameterSorts()) {
                termPrinter.append(out, paramSort);

                if (++counter < func.getParameterSorts().length) {
                  out.append(' ');
                }
              }
              out.append(") ");
              termPrinter.append(out, func.getReturnSort());
              out.append(")\n");
            }
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
}
