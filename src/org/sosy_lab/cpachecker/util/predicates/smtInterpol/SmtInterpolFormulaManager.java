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

import java.io.StringReader;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaList;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractFormulaCreator.CreateBitType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smtInterpol.SmtInterpolEnvironment.Type;

import de.uni_freiburg.informatik.ultimate.logic.Logics;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;


public class SmtInterpolFormulaManager extends AbstractFormulaManager<Term> {

  private SmtInterpolEnvironment env;
  private SmtInterpolFormulaCreator creator;

  public SmtInterpolFormulaManager(
      SmtInterpolUnsafeFormulaManager pUnsafeManager,
      SmtInterpolFunctionFormulaManager pFunctionManager,
      SmtInterpolBooleanFormulaManager pBooleanManager,
      SmtInterpolRationalFormulaManager pNumericManager) {
    super(pUnsafeManager, pFunctionManager, pBooleanManager, pNumericManager, null);
    this.creator = (SmtInterpolFormulaCreator)getFormulaCreator();
    assert creator != null;
    this.env = creator.getEnv();
  }


  public static SmtInterpolFormulaManager create(Configuration config, LogManager logger, boolean pUseIntegers) throws InvalidConfigurationException{

    SmtInterpolEnvironment env = new SmtInterpolEnvironment(config);
    Type type = pUseIntegers ? Type.INT : Type.REAL;
    if (pUseIntegers) {
      env.setLogic(Logics.QF_UFLIA);
    } else {
      env.setLogic(Logics.QF_UFLRA);
    }
    final Sort t = env.sort(type);
    CreateBitType<Sort> bitTypeCreator = new CreateBitType<Sort>(){
      @Override
      public Sort fromSize(int pSize) {
        return t;
      }};

    SmtInterpolFormulaCreator creator = new SmtInterpolFormulaCreator(env, env.sort(Type.BOOL), t, bitTypeCreator);

    // Create managers
    SmtInterpolUnsafeFormulaManager unsafeManager = new SmtInterpolUnsafeFormulaManager(creator);
    SmtInterpolFunctionFormulaManager functionTheory = new SmtInterpolFunctionFormulaManager(creator, unsafeManager);
    SmtInterpolBooleanFormulaManager booleanTheory = SmtInterpolBooleanFormulaManager.create(creator);
    SmtInterpolRationalFormulaManager rationalTheory = SmtInterpolRationalFormulaManager.create(creator, functionTheory);
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

  @SuppressWarnings("unchecked")
  private <T extends Formula> T encapsulateTerm(Class<T> pClazz, Term t) {
    return creator.encapsulate(pClazz, t);
  }

  @Override
  public <T extends Formula> T parse(Class<T> pClazz, String pS) throws IllegalArgumentException {
    return encapsulateTerm(pClazz, parseStringToTerms(pS)[0]);
  }


  @Override
  public String dumpFormula(Term t) {
    return t.toStringDirect();
  }

  @Override
  public String getVersion() {
    return env.getVersion();
  }

  protected static Term[] getTerm(FormulaList f) {
    return ((SmtInterpolFormulaList) f).getTerms();
  }

  protected <T extends Formula > T encapsulate(Class<T> pClazz, Term t) {
    return encapsulateTerm(pClazz, t);
  }

  protected static FormulaList encapsulate(Term[] t) {
    return new SmtInterpolFormulaList(t);
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

}
