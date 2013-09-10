/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.StringReader;

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.Appenders;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smtInterpol.SmtInterpolEnvironment.Type;

import de.uni_freiburg.informatik.ultimate.logic.FormulaLet;
import de.uni_freiburg.informatik.ultimate.logic.Logics;
import de.uni_freiburg.informatik.ultimate.logic.PrintTerm;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;

public class SmtInterpolFormulaManager extends AbstractFormulaManager<Term> {

  private final SmtInterpolEnvironment env;
  private final SmtInterpolFormulaCreator creator;

  private SmtInterpolFormulaManager(
      SmtInterpolUnsafeFormulaManager pUnsafeManager,
      SmtInterpolFunctionFormulaManager pFunctionManager,
      SmtInterpolBooleanFormulaManager pBooleanManager,
      SmtInterpolRationalFormulaManager pNumericManager) {
    super(pUnsafeManager, pFunctionManager, pBooleanManager, pNumericManager, null);
    this.creator = checkNotNull((SmtInterpolFormulaCreator)getFormulaCreator());
    this.env = creator.getEnv();
  }

  public static SmtInterpolFormulaManager create(Configuration config, LogManager logger, boolean pUseIntegers) throws InvalidConfigurationException {

    Logics logic = pUseIntegers ? Logics.QF_UFLIA : Logics.QF_UFLRA;
    SmtInterpolEnvironment env = new SmtInterpolEnvironment(config, logic, logger);

    Type type = pUseIntegers ? Type.INT : Type.REAL;
    final Sort t = env.sort(type);
    SmtInterpolFormulaCreator creator = new SmtInterpolFormulaCreator(env, env.sort(Type.BOOL), t);

    // Create managers
    SmtInterpolUnsafeFormulaManager unsafeManager = new SmtInterpolUnsafeFormulaManager(creator);
    SmtInterpolFunctionFormulaManager functionTheory = new SmtInterpolFunctionFormulaManager(creator, unsafeManager);
    SmtInterpolBooleanFormulaManager booleanTheory = SmtInterpolBooleanFormulaManager.create(creator);
    SmtInterpolRationalFormulaManager rationalTheory = new SmtInterpolRationalFormulaManager(creator, functionTheory, pUseIntegers);
    return new SmtInterpolFormulaManager(unsafeManager, functionTheory, booleanTheory, rationalTheory);
  }

  /** Parse a String to Terms and Declarations.
   * The String may contain terms and function-declarations in SMTLIB2-format.
   * Use Prefix-notation! */
  private Term[] parseStringToTerms(String s) {
    Parser parser = new Parser(env, new StringReader(s));

    try {
      parser.parse();
    } catch (Exception e) {
      throw new IllegalArgumentException("Could not parse term:" + e.getMessage(), e);
    }

    Term[] terms = parser.getTerms();
    return terms;
  }

  private <T extends Formula> T encapsulateTerm(Class<T> pClazz, Term t) {
    return creator.encapsulate(pClazz, t);
  }

  @Override
  public <T extends Formula> T parse(Class<T> pClazz, String pS) throws IllegalArgumentException {
    return encapsulateTerm(pClazz, parseStringToTerms(pS)[0]);
  }


  @Override
  public Appender dumpFormula(final Term t) {
    return new Appenders.AbstractAppender() {

      @Override
      public void appendTo(Appendable pAppendable) throws IOException {
        pAppendable.append("(assert ");

        // This is the same as t.toString() does,
        // but directly uses the Appendable for better performance
        // and less memory consumption.
        Term letted = (new FormulaLet()).let(t);
        new PrintTerm().append(pAppendable, letted);

        pAppendable.append(")");
      }
    };
  }

  @Override
  public String getVersion() {
    return env.getVersion();
  }

  protected <T extends Formula> T encapsulate(Class<T> pClazz, Term t) {
    return encapsulateTerm(pClazz, t);
  }


  /** This method returns a 'shared' environment or
   * a complete new environment. */
  SmtInterpolEnvironment createEnvironment() {
    assert env != null;
    return env;
  }


  SmtInterpolEnvironment getEnv() {
    return env;
  }

  @Override
  protected Term getTerm(Formula pF) {
    // for visibility
    return super.getTerm(pF);
  }
}
