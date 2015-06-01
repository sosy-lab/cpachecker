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

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.util.predicates.AssignableTerm.Variable;
import org.sosy_lab.cpachecker.util.predicates.AssignableTerm;
import org.sosy_lab.cpachecker.util.predicates.AssignableTerm.Function;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.TermType;

import scala.Option;
import ap.SimpleAPI;
import ap.parser.IAtom;
import ap.parser.IConstant;
import ap.parser.IExpression;
import ap.parser.IFunApp;

import com.google.common.base.Verify;

class PrincessModel {

  private static AssignableTerm toVariable(IExpression t) {
    if (!PrincessUtil.isVariable(t)) {
      throw new IllegalArgumentException("Given term is no variable! (" + t.toString() + ")");
    }

    final String lName;
    final TermType lType;
    if (t instanceof IAtom) {
      lName = ((IAtom) t).pred().name();
      lType = TermType.Boolean;
    } else {
      IConstant v = (IConstant) t;
      lName = v.c().name();
      lType = TermType.Integer;
    }
    return new Variable(lName, lType);
  }


  private static Function toFunction(IExpression t,
      PrincessEnvironment env, SimpleAPI.PartialModel partialModel) {
    if (PrincessUtil.isVariable(t)) {
      throw new IllegalArgumentException("Given term is no function! (" + t.toString() + ")");
    }

    IFunApp appTerm = (IFunApp)t;
    String lName = appTerm.fun().name();

    int lArity = PrincessUtil.getArity(appTerm);

    // TODO we assume only constants (ints) as parameters for now
    Object[] lArguments = new Object[lArity];

    for (int lArgumentIndex = 0; lArgumentIndex < lArity; lArgumentIndex++) {
      IExpression lArgument = PrincessUtil.getArg(appTerm, lArgumentIndex);
      Option<SimpleAPI.ModelValue> argumentValue = partialModel.evalExpression(lArgument);
      if (argumentValue.isDefined()) {
        lArguments[lArgumentIndex] = getValue(argumentValue.get());
      } else {
        lArguments[lArgumentIndex] = lArgument.toString();
      }
    }

    // currently only int is supported in princess as return type, this needs to
    // be changed if
    return new Function(lName, TermType.Integer, lArguments);
  }


  private static AssignableTerm toAssignable(IExpression t,
      PrincessEnvironment env, SimpleAPI.PartialModel partialModel) {
    if (PrincessUtil.isVariable(t)) {
      return toVariable(t);
    } else {
      return toFunction(t, env, partialModel);
    }
  }

  private static Object getValue(SimpleAPI.ModelValue value) {
    if (value instanceof SimpleAPI.BoolValue) {
      return ((SimpleAPI.BoolValue)value).v();

    } else if (value instanceof SimpleAPI.IntValue) {
      return ((SimpleAPI.IntValue)value).v().bigIntValue();

    } else {
      throw new IllegalArgumentException("unhandled model value " + value + " of type " + value.getClass());
    }
  }

  static Model createModel(PrincessStack stack, Collection<IExpression> assertedFormulas) {
    Map<AssignableTerm, Object> model = new LinkedHashMap<>();

    checkArgument(stack.checkSat(), "model is only available for SAT environments");

    SimpleAPI.PartialModel partialModel = stack.getPartialModel();

    for (IExpression lKeyTerm : PrincessUtil.getVarsAndUIFs(assertedFormulas)) {
      Option<SimpleAPI.ModelValue> value = partialModel.evalExpression(lKeyTerm);

      if (!value.isDefined()) {
        continue;
      }

      AssignableTerm lAssignable = toAssignable(lKeyTerm, stack.getEnv(), partialModel);
      Object lValue = getValue(value.get());

      // Duplicate entries may occur if "uf(a)" and "uf(b)" occur in the formulas
      // and "a" and "b" have the same value, because "a" and "b" will both be resolved,
      // leading to two entries for "uf(1)" (if value is 1).
      Object existingValue = model.get(lAssignable);
      Verify.verify(existingValue == null || lValue.equals(existingValue),
          "Duplicate values for model entry %s: %s and %s", lAssignable, existingValue, lValue
          );

      model.put(lAssignable, lValue);
    }

    return new Model(model);
  }

}
