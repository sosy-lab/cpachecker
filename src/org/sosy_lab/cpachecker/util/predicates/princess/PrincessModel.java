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

import ap.SimpleAPI;
import ap.parser.IAtom;
import ap.parser.IConstant;
import ap.parser.IExpression;
import ap.parser.IFunApp;
import com.google.common.collect.ImmutableMap;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.core.counterexample.Model.AssignableTerm;
import org.sosy_lab.cpachecker.core.counterexample.Model.Constant;
import org.sosy_lab.cpachecker.core.counterexample.Model.Function;
import org.sosy_lab.cpachecker.core.counterexample.Model.TermType;
import org.sosy_lab.cpachecker.core.counterexample.Model.Variable;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import scala.Option;

import java.util.Collection;
import java.util.Set;

class PrincessModel {

  private static TermType toType(PrincessEnvironment.Type type) {
    switch (type) {
      case BOOL:
        return TermType.Boolean;
      case INT:
        return TermType.Integer;
      default:
        throw new IllegalArgumentException("Given parameter cannot be converted to a TermType!");
    }
  }

  private static AssignableTerm toVariable(IExpression t) {
    if (!PrincessUtil.isVariable(t)) {
      throw new IllegalArgumentException("Given term is no variable! (" + t.toString() + ")");
    }

    final String lName;
    final TermType lType;
    if (t instanceof IAtom) {
      lName = ((IAtom) t).pred().name();
      lType = toType(PrincessEnvironment.Type.BOOL);
    } else {
      IConstant v = (IConstant) t;
      lName = v.c().name();
      lType = toType(PrincessEnvironment.Type.INT);
    }

    Pair<String, Integer> lSplitName = FormulaManagerView.parseName(lName);
    if (lSplitName.getSecond() != null) {
      return new Variable(lSplitName.getFirst(), lSplitName.getSecond(), lType);
    } else {
      return new Constant(lSplitName.getFirst(), lType);
    }
  }


  private static Function toFunction(PrincessEnvironment env, IExpression t) {
    if (PrincessUtil.isVariable(t)) {
      throw new IllegalArgumentException("Given term is no function! (" + t.toString() + ")");
    }

    IFunApp appTerm = (IFunApp)t;
    String lName = appTerm.fun().name();
    TermType lType = toType(env.getFunctionDeclaration(appTerm.fun()).getResultType());

    int lArity = PrincessUtil.getArity(appTerm);

    // TODO we assume only constants (ints) as parameters for now
    Object[] lArguments = new Object[lArity];

    for (int lArgumentIndex = 0; lArgumentIndex < lArity; lArgumentIndex++) {
      IExpression lArgument = PrincessUtil.getArg(appTerm, lArgumentIndex);
      String lTermRepresentation = lArgument.toString();
      Object lValue = Integer.valueOf(lTermRepresentation);
      lArguments[lArgumentIndex] = lValue;
    }

    return new Function(lName, lType, lArguments);
  }


  private static AssignableTerm toAssignable(PrincessEnvironment env, IExpression t) {
    if (PrincessUtil.isVariable(t)) {
      return toVariable(t);
    } else {
      return toFunction(env, t);
    }
  }

  static Model createModel(PrincessFormulaManager mgr, Collection<IExpression> terms) {
    PrincessEnvironment env = mgr.getEnvironment();
    // model can only return values for keys, not for terms
    Set<IExpression> keys = PrincessUtil.getVars(terms);

    ImmutableMap.Builder<AssignableTerm, Object> model = ImmutableMap.builder();

    try {
      assert env.checkSat() : "model is only available for SAT environments";
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    SimpleAPI.PartialModel partialModel = env.getModel();

    for (IExpression lKeyTerm : keys) {
      Option<SimpleAPI.ModelValue> value = partialModel.evalExpression(lKeyTerm);

      if (!value.isDefined()) {
        continue;
      }

      SimpleAPI.ModelValue lValueTerm = value.get();

      AssignableTerm lAssignable = toAssignable(env, lKeyTerm);

      Object lValue;

      switch (lAssignable.getType()) {
      case Boolean:
        lValue = ((SimpleAPI.BoolValue)lValueTerm).v();
        break;

      case Integer:
        lValue = ((SimpleAPI.IntValue)lValueTerm).v().longValue();
        break;

      default:
        throw new IllegalArgumentException("unhandled type " + lAssignable.getType());
      }

      model.put(lAssignable, lValue);
    }

    return new Model(model.build());
  }

}
