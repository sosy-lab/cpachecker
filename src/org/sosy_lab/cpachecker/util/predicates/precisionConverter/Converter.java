/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.precisionConverter;

import static java.lang.String.format;

import java.util.List;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.SymbolEncoding.Type;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/** This is a dummy converter
 * that returns the identity of the given terms and types. */
public class Converter {

  public Converter() {}

  public String convertFunctionDeclaration(String symbol, Type<String> pFt) {
    return format("%s (%s) %s",
      symbol, Joiner.on(' ').join(pFt.getParameterTypes()), pFt.getReturnType());
  }

  public String convertFunctionDefinition(String symbol,
      Type<String> type, Pair<String, Type<FormulaType<?>>> initializerTerm) {
    return format("%s (%s) %s %s",
        symbol, Joiner.on(' ').join(type.getParameterTypes()),
        type.getReturnType(), initializerTerm.getFirst());
  }

  public Pair<String, Type<FormulaType<?>>> convertNumeral(String num) {
    return wrap(num);
  }

  public Pair<String, Type<FormulaType<?>>> convertSymbol(String symbol) {
    return wrap(symbol);
  }

  public Pair<String, Type<FormulaType<?>>> convertTerm(
      Pair<String, Type<FormulaType<?>>> op, List<Pair<String, Type<FormulaType<?>>>> terms) {
    if (terms.isEmpty()) {
      return wrap("(" + op.getFirst() + ")"); // should not happen?
    } else {
      return wrap("(" + op.getFirst() + " " +
          Joiner.on(' ').join(Lists.transform(terms, Pair.getProjectionToFirst())) + ")");
    }
  }

  private static Pair<String, Type<FormulaType<?>>> wrap(String s) {
    // return dummy type with size 0
    return Pair.of(s,  new Type<FormulaType<?>>(FormulaType.getBitvectorTypeWithSize(0)));
  }
}
