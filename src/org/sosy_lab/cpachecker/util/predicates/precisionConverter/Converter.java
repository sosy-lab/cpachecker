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
import java.util.Set;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.precisionConverter.SymbolEncoding.Type;
import org.sosy_lab.cpachecker.util.predicates.precisionConverter.SymbolEncoding.UnknownFormulaSymbolException;
import org.sosy_lab.java_smt.api.FormulaType;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/** This is a dummy converter
 * that returns the identity of the given terms and types. */
public class Converter {

  protected final Set<String> binBooleanOps = Sets.newHashSet("and", "or");
  protected final SymbolEncoding symbolEncoding;
  protected final LogManager logger;

  public Converter(LogManager logger, CFA cfa) {
    this.symbolEncoding = new SymbolEncoding(cfa);
    this.logger = logger;
  }

  @VisibleForTesting
  public Converter() {
    this.symbolEncoding = null;
    this.logger = null;
  }

  /**
   * @throws UnknownFormulaSymbolException may be thrown in subclasses
   */
  public String convertFunctionDeclaration(String symbol, Type<String> pFt)
      throws UnknownFormulaSymbolException {
    return format("%s (%s) %s",
      symbol, Joiner.on(' ').join(pFt.getParameterTypes()), pFt.getReturnType());
  }

  /**
   * @throws UnknownFormulaSymbolException may be thrown in subclasses
   */
  public String convertFunctionDefinition(String symbol,
      Type<String> type, Pair<String, Type<FormulaType<?>>> initializerTerm)
          throws UnknownFormulaSymbolException {
    return format("%s (%s) %s %s",
        symbol, Joiner.on(' ').join(type.getParameterTypes()),
        type.getReturnType(), initializerTerm.getFirst());
  }

  public Pair<String, Type<FormulaType<?>>> convertNumeral(String num) {
    return wrap(num);
  }

  /**
   * @throws UnknownFormulaSymbolException may be thrown in subclasses
   */
  public Pair<String, Type<FormulaType<?>>> convertSymbol(String symbol)
      throws UnknownFormulaSymbolException {
    return wrap(symbol);
  }

  /**
   * @throws UnknownFormulaSymbolException may be thrown in subclasses
   */
  public Pair<String, Type<FormulaType<?>>> convertTerm(
      Pair<String, Type<FormulaType<?>>> op, List<Pair<String, Type<FormulaType<?>>>> terms)
          throws UnknownFormulaSymbolException {
    if (terms.isEmpty()) {
      return wrap("(" + op.getFirst() + ")"); // should not happen?
    } else {
      return wrap(
          "("
              + op.getFirst()
              + " "
              + Joiner.on(' ').join(Lists.transform(terms, Pair::getFirst))
              + ")");
    }
  }

  private static Pair<String, Type<FormulaType<?>>> wrap(String s) {
    // return dummy type with size 0
    return Pair.of(s,  new Type<FormulaType<?>>(FormulaType.getBitvectorTypeWithSize(0)));
  }

  public enum PrecisionConverter {DISABLE, INT2BV, BV2INT}

  public static Converter getConverter(PrecisionConverter encodePredicates, CFA cfa, LogManager logger) {
    switch (encodePredicates) {
    case INT2BV: {
      return new BVConverter(cfa, logger);
    }
    case BV2INT: {
      return new IntConverter(cfa, logger);
    }
    case DISABLE: {
      return null;
    }
    default:
      throw new AssertionError("invalid value for option");
    }
  }
}
