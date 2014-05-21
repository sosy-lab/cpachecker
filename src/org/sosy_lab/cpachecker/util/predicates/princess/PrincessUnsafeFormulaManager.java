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
package org.sosy_lab.cpachecker.util.predicates.princess;

import ap.parser.IExpression;
import ap.parser.IFormula;
import ap.parser.IFunApp;
import ap.parser.IFunction;
import ap.parser.ITerm;
import com.google.common.collect.Lists;
import org.eclipse.jdt.core.JavaConventions;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractUnsafeFormulaManager;
import scala.collection.JavaConversions;

import java.util.ArrayList;
import java.util.List;

class PrincessUnsafeFormulaManager extends AbstractUnsafeFormulaManager<IExpression, PrincessEnvironment.Type, PrincessEnvironment> {

  PrincessUnsafeFormulaManager(PrincessFormulaCreator pCreator) {
    super(pCreator);
  }

  /** ApplicationTerms can be wrapped with "|".
   * This function removes those chars. */
  static String dequote(String s) {
   return s.replace("|", "");
  }

 /** ApplicationTerms can be wrapped with "|".
   * This function replaces those chars with "\"". */
  // TODO: Check where this was used in the past.
  @SuppressWarnings("unused")
  private static String convertQuotes(String s) {
    return s.replace("|", "\"");
  }

  @Override
  public boolean isAtom(IExpression t) {
    return PrincessUtil.isAtom(t);
  }

  @Override
  public int getArity(IExpression pT) {
    return PrincessUtil.getArity(pT);
  }

  @Override
  public IExpression getArg(IExpression pT, int pN) {
    return PrincessUtil.getArg(pT, pN);
  }

  @Override
  public boolean isVariable(IExpression pT) {
    return PrincessUtil.isVariable(pT);
  }

  @Override
  public boolean isUF(IExpression t) {
    return PrincessUtil.isUIF(t);
  }

  @Override
  public String getName(IExpression t) {
    if (isVariable(t)) {
      return dequote(t.toString());
    } else if (isUF(t)) {
      return ((IFunApp)t).fun().name();
    } else {
      throw new IllegalArgumentException("The Term " + t + " has no name!");
    }
  }

  @Override
  public IExpression replaceArgs(IExpression pT, List<IExpression> newArgs) {
    return PrincessUtil.replaceArgs(getFormulaCreator().getEnv(), pT, newArgs);
  }

  @Override
  public IExpression replaceName(IExpression t, String pNewName) {

    if (isVariable(t)) {
      boolean isBoolean = t instanceof IFormula;
      PrincessEnvironment.Type type = isBoolean ? PrincessEnvironment.Type.BOOL : PrincessEnvironment.Type.INT;
      return getFormulaCreator().makeVariable(type, pNewName);
    } else if (isUF(t)) {
      IFunApp fun = (IFunApp) t;
      PrincessEnvironment.FunctionType funcDecl = getFormulaCreator().getEnv().getFunctionDeclaration(fun.fun());
      List<IExpression> args = new ArrayList<>(fun.length());
      for (ITerm arg: JavaConversions.asJavaIterable(fun.args())) {
        args.add(arg);
      }
      return createUIFCallImpl(fun.fun(), funcDecl.getResultType(), args);
    } else {
      throw new IllegalArgumentException("The Term " + t + " has no name!");
    }
  }

  IExpression createUIFCallImpl(IFunction funcDecl, PrincessEnvironment.Type resultType, List<IExpression> args) {
    IExpression ufc = getFormulaCreator().getEnv().makeFunction(funcDecl, resultType, args);
    assert PrincessUtil.isUIF(ufc);
    return ufc;
  }

  @Override
  public boolean isNumber(IExpression pT) {
    return PrincessUtil.isNumber(pT);
  }
}
