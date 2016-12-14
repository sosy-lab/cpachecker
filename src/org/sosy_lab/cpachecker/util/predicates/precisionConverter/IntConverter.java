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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.precisionConverter.SymbolEncoding.Type;
import org.sosy_lab.cpachecker.util.predicates.precisionConverter.SymbolEncoding.UnknownFormulaSymbolException;
import org.sosy_lab.java_smt.api.FormulaType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;


public class IntConverter extends Converter {

  private final Map<String,String> unaryOps; // input-type == output-type
  private final Map<String,String> binOps; // type is Bool
  private final Map<String,String> arithmeticOps; // type is Int
  private final Set<String> ignorableFunctions = Sets.newHashSet();

  public IntConverter(CFA pCfa, LogManager pLogger) {
    super(pLogger, pCfa);

    unaryOps = new HashMap<>();
    unaryOps.put("bvneg", "-");
    unaryOps.put("not", "not");
    unaryOps.put("bvnot", "_~_");

    // add Pair<signed operator, unsigned operator>
    binOps = new HashMap<>();
    binOps.put("=", "=");
    binOps.put("bvslt", "<");
    binOps.put("bvult", "<");
    binOps.put("bvsle", "<=");
    binOps.put("bvule", "<=");
    binOps.put("bvsgt", ">");
    binOps.put("bvugt", ">");
    binOps.put("bvsge", ">=");
    binOps.put("bvuge", ">=");

    // add Pair<signed operator, unsigned operator>
    arithmeticOps = new HashMap<>();
    arithmeticOps.put("bvadd", "+");
    arithmeticOps.put("bvsub", "-");

    // TODO encode as UFs?
    arithmeticOps.put("bvmul", "Integer__*_"); // TODO direct handling of plain numeral multiplication
    arithmeticOps.put("bvsdiv", "Integer__/_");
    arithmeticOps.put("bvudiv", "Integer__/_");
    arithmeticOps.put("bvsrem", "Integer__%_");
    arithmeticOps.put("bvurem", "Integer__%_");
//    arithmeticOps.put("Rational__*_", Pair.of("bvmul", "bvmul"));
//    arithmeticOps.put("Rational__/_", Pair.of("bvsdiv", "bvudiv"));
//    arithmeticOps.put("Rational__%_", Pair.of("bvsrem)", "bvurem"));
    arithmeticOps.put("bvand", "_&_");
    arithmeticOps.put("bvor", "_!!_");
    arithmeticOps.put("bvxor", "_^_");
    arithmeticOps.put("bvshl", "_<<_");
    arithmeticOps.put("bvlshr", "_>>_");
    arithmeticOps.put("bvashr", "_>>_");
  }

  private String unescapeSymbol(final String symbol) {
    if (symbol.startsWith("|") && symbol.endsWith("|")) {
      return symbol.substring(1, symbol.length() - 1);
    }
    return symbol;
  }

  /** returns the type of a variable as (N,{}),
   * and the type of a uninterpreted function as (N,{N1,N2,...}),
   * where N is the return-type and {N1,N2,...} are the parameter-types. */
  private Type<FormulaType<?>> getType(String symbol) throws UnknownFormulaSymbolException {
    symbol = unescapeSymbol(symbol);
    return symbolEncoding.getType(symbol);
  }

  private String getSMTType(FormulaType<?> t) {
    if (t == FormulaType.BooleanType) {
      return "Bool";
    } else if (t == FormulaType.IntegerType) {
      return "Int";
    } else if (t.isBitvectorType()) {
      return "Int"; // this is important: use "Int" instead of "BV"
    } else {
      throw new AssertionError("unhandled type: " + t);
    }
  }

  @Override
  public String convertFunctionDeclaration(String symbol, Type<String> type)
      throws UnknownFormulaSymbolException {

    final Type<FormulaType<?>> t = getType(symbol);

    final List<String> lst = new ArrayList<>();
    final FormulaType<?> retType;

    if (t == null) {
      // ignore some special symbols like "Integer__*_" and replace them by dummy-symbols
      // TODO we could also remove those symbols completely,
      // but then the parser would have to support empty lines.
      final String ignorePrefix = "_CPAchecker_ignored_symbol_";
      logger.log(Level.SEVERE, "ignoring unknown function symbol '" + symbol + "'");
      for (@SuppressWarnings("unused") String i : type.getParameterTypes()) {
        lst.add(getSMTType(FormulaType.IntegerType));
      }
      retType = FormulaType.IntegerType;
      symbol = ignorePrefix + symbol;
    } else {
      for (FormulaType<?> i : t.getParameterTypes()) {
        lst.add(getSMTType(i));
      }
      retType = t.getReturnType();
    }

    return format("%s (%s) %s",
        symbol, Joiner.on(" ").join(lst), getSMTType(retType));
  }

  @Override
  public String convertFunctionDefinition(String symbol,
      Type<String> type, @Nullable Pair<String, Type<FormulaType<?>>> initializerTerm)
          throws UnknownFormulaSymbolException {
    assert !symbolEncoding.containsSymbol(symbol) : "function re-defined";
    assert type.getParameterTypes().isEmpty() : "tmp-function with complex type";
    symbolEncoding.put(symbol, checkNotNull(initializerTerm.getSecond(), initializerTerm));
    return format("%s (%s) %s %s",
        symbol, Joiner.on(' ').join(type.getParameterTypes()),
        getSMTType(getType(symbol).getReturnType()), initializerTerm.getFirst());
  }

  @Override
  public Pair<String, Type<FormulaType<?>>> convertNumeral(String num) {
    return Pair.of(
        num,
        new Type<FormulaType<?>>(FormulaType.IntegerType));
  }

  @Override
  public Pair<String, Type<FormulaType<?>>> convertSymbol(String symbol)
      throws UnknownFormulaSymbolException {
    return Pair.of(symbol, /*@Nullable*/ getType(symbol));
  }

  @Override
  public Pair<String, Type<FormulaType<?>>> convertTerm(
      Pair<String, Type<FormulaType<?>>> op, List<Pair<String, Type<FormulaType<?>>>> terms) {

    if (terms.isEmpty()) {
      return Pair.of(format("(%s)", op.getFirst()), op.getSecond()); // should not happen?

    } else if (terms.size() == 1
        && (op.getFirst().startsWith("(_ extract ")
            || op.getFirst().startsWith("(_ zero_extend ")
            || op.getFirst().startsWith("(_ sign_extend "))) {
      // we convert "((_ extract N M) X)" into "(X)" and ignore the extraction.
      // we convert "((_ extend N) X)" into "(X)" and ignore the extension.
      // soundness guaranteed, because we only change the precision-predicates.
      assert op.getSecond() == null : "type of EXTRACT/EXTEND should be unknown.";
      return Iterables.getOnlyElement(terms);

    } else if (terms.size() == 2 && op.getFirst().equals("_")
        && terms.get(0).getFirst().startsWith("bv")) {
      // we convert "(_ bvN L)" into "N" and ignore the bit-length L.
      assert op.getSecond() == null : "type of BV-NUMBER should be unknown.";
      return Pair.of(terms.get(0).getFirst().substring(2), new Type<FormulaType<?>>(FormulaType.IntegerType));

    } else if (terms.size() == 1 && unaryOps.containsKey(op.getFirst())) {
      return Pair.of(
          format(
              "(%s %s)",
              unaryOps.get(op.getFirst()),
              Joiner.on(' ').join(Lists.transform(terms, Pair::getFirst))),
          Iterables.getOnlyElement(terms).getSecond());

    } else if (terms.size() == 1 && ignorableFunctions.contains(op.getFirst())) {
      // ignore and remove ignorable functions, e.g. casts from INT to REAL, we do not need them in BV-theory
      return Iterables.getOnlyElement(terms);

    } else if (terms.size() == 2
        && (binOps.containsKey(op.getFirst()) || arithmeticOps.containsKey(op.getFirst()))) {
      Pair<String, Type<FormulaType<?>>> e1 = terms.get(0);
      Pair<String, Type<FormulaType<?>>> e2 = terms.get(1);
      Type<FormulaType<?>> type;
      String operator;
      if (binOps.containsKey(op.getFirst())) {
        operator = binOps.get(op.getFirst());
        type = new Type<>(FormulaType.BooleanType);
      } else {
        operator = arithmeticOps.get(op.getFirst());
        type = new Type<>(FormulaType.IntegerType);
      }
      return Pair.of(format("(%s %s %s)",
          operator,
          e1.getFirst(),
          e2.getFirst()),
          type);

    } else if (terms.size() == 3 && "ite".equals(op.getFirst())) {
      Pair<String, Type<FormulaType<?>>> cond = terms.get(0);
      Pair<String, Type<FormulaType<?>>> eIf = terms.get(1);
      Pair<String, Type<FormulaType<?>>> eElse = terms.get(2);
      FormulaType<?> type;
      if (Type.BOOL.equals(eIf.getSecond())) {
        type = FormulaType.BooleanType;
      } else {
        type = FormulaType.IntegerType;
      }
      return Pair.of(format("(ite %s %s %s)",
          cond.getFirst(),
          eIf.getFirst(),
          eElse.getFirst()),
          new Type<FormulaType<?>>(type));

    } else if (binBooleanOps.contains(op.getFirst())) {
      return Pair.of(
          format(
              "(%s %s)",
              op.getFirst(),
              Joiner.on(' ').join(Lists.transform(terms, Pair::getFirst))),
          new Type<FormulaType<?>>(FormulaType.BooleanType));

    } else if (symbolEncoding.containsSymbol(op.getFirst())) {
      // UF --> cast every parameter to correct bitsize
      assert op.getSecond().getParameterTypes().size() == terms.size();
      List<String> params = new ArrayList<>();
      for (int i=0; i<terms.size(); i++) {
        params.add(terms.get(i).getFirst());
      }
      return Pair.of(
          format("(%s %s)",
              op.getFirst(), Joiner.on(' ').join(params)),
          op.getSecond());

    } else { // UF
      if (!"_".equals(op.getFirst())) {
        logger.log(Level.SEVERE, "unhandled term:", op, terms);
      }
      return Pair.of(
          format(
              "(%s %s)",
              op.getFirst(),
              Joiner.on(' ').join(Lists.transform(terms, Pair::getFirst))),
          op.getSecond());
    }
  }
}
