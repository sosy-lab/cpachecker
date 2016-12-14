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

import apache.harmony.math.BigInteger;

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
import org.sosy_lab.java_smt.api.FormulaType.BitvectorType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;


public class BVConverter extends Converter {

  private final Map<String,String> unaryOps; // input-type == output-type
  private final Map<String,Pair<String,String>> binOps; // type is Bool
  private final Map<String,Pair<String,String>> arithmeticOps; // type is BV
  private final Set<String> ignorableFunctions = Sets.newHashSet("to_int", "to_real");

  public BVConverter(CFA pCfa, LogManager pLogger) {
    super(pLogger, pCfa);

    unaryOps = new HashMap<>();
    unaryOps.put("-", "bvneg");
    unaryOps.put("not", "not");
    unaryOps.put("_~_", "bvnot");
    //unaryOps.put("__isSubnormal__", "bvneg");// TODO ?? Boolean [Rational]

    // add Pair<signed operator, unsigned operator>
    binOps = new HashMap<>();
    binOps.put("=", Pair.of("=", "="));
    binOps.put("<", Pair.of("bvslt", "bvult"));
    binOps.put("<=", Pair.of("bvsle", "bvule"));
    binOps.put(">", Pair.of("bvsgt", "bvugt"));
    binOps.put(">=", Pair.of("bvsge", "bvuge"));

    // add Pair<signed operator, unsigned operator>
    arithmeticOps = new HashMap<>();
    arithmeticOps.put("+", Pair.of("bvadd", "bvadd"));
    arithmeticOps.put("-", Pair.of("bvsub", "bvsub"));
    arithmeticOps.put("*", Pair.of("bvmul", "bvmul"));
    arithmeticOps.put("/", Pair.of("bvsdiv", "bvudiv"));
    arithmeticOps.put("%", Pair.of("bvsrem", "bvurem"));
    arithmeticOps.put("Integer__*_", Pair.of("bvmul", "bvmul"));
    arithmeticOps.put("Integer__/_", Pair.of("bvsdiv", "bvudiv"));
    arithmeticOps.put("Integer__%_", Pair.of("bvsrem", "bvurem"));
    arithmeticOps.put("Rational__*_", Pair.of("bvmul", "bvmul"));
    arithmeticOps.put("Rational__/_", Pair.of("bvsdiv", "bvudiv"));
    arithmeticOps.put("Rational__%_", Pair.of("bvsrem)", "bvurem"));
    arithmeticOps.put("_&_", Pair.of("bvand", "bvand"));
    arithmeticOps.put("_!!_", Pair.of("bvor", "bvor"));
    arithmeticOps.put("_^_", Pair.of("bvxor", "bvxor"));
    arithmeticOps.put("_<<_", Pair.of("bvshl", "bvshl"));
    arithmeticOps.put("_>>_", Pair.of("bvlshr", "bvashr"));
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

  private Integer getBVsize(FormulaType<?> t) {
    if (t.isBitvectorType()) {
      return ((BitvectorType)t).getSize();
    } else {
      throw new AssertionError("unhandled type: " + t);
    }
  }

  /** This is a small heuristic to get signess of the result-type.
   * It is sound, because we only convert precision-predicates and not program-specific SMT-formulas.
   *
   * A fully precise method would have to deal with CalculationTypes and ResultTypes
   * (see {@link org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder} for detail).
   * As this would create many casts and extractions, we ignore it in favor of this heuristic. */
  private boolean isOperationSigned(Type<FormulaType<?>> t1, Type<FormulaType<?>> t2) {
    if (t1.getReturnType().isBitvectorType() && t2.getReturnType().isBitvectorType()) {
      int s1 = getBVsize(t1.getReturnType());
      int s2 = getBVsize(t2.getReturnType());
      // return the signess of the bigger type
      if (s1 < s2) {
        return t2.isSigned();
      } else if (s1 > s2) {
        return t1.isSigned();
      } else {
        // return, whether any of the types is unsigned
        return t1.isSigned() && t2.isSigned();
      }
    }
    return false;
  }

  private String getSMTType(FormulaType<?> t) {
    if (t == FormulaType.BooleanType) {
      return "Bool";
    } else if (t == FormulaType.IntegerType) {
      return "Int";
    } else if (t.isBitvectorType()) {
      return format("(_ BitVec %d)", ((BitvectorType)t).getSize());
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
      final int defaultBitsize = 1;
      final String ignorePrefix = "_CPAchecker_ignored_symbol_";
      logger.log(Level.SEVERE, "ignoring unknown function symbol '" + symbol + "'");
      for (@SuppressWarnings("unused") String i : type.getParameterTypes()) {
        lst.add(getSMTType(FormulaType.getBitvectorTypeWithSize(defaultBitsize)));
      }
      retType = FormulaType.getBitvectorTypeWithSize(defaultBitsize);
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
          throws UnknownFormulaSymbolException{
    assert !symbolEncoding.containsSymbol(symbol) : "function re-defined";
    assert type.getParameterTypes().isEmpty() : "tmp-function with complex type";
    symbolEncoding.put(symbol, checkNotNull(initializerTerm.getSecond()));
    return format("%s (%s) %s %s",
        symbol, Joiner.on(' ').join(type.getParameterTypes()),
        getSMTType(getType(symbol).getReturnType()), initializerTerm.getFirst());
  }

  private String cast(String term, @Nullable Integer availableBitsize, int neededBitsize) {

    // simplify numerals, use the correct size directly instead of expensive casting
    if (term.matches("(\\(_\\sbv\\d+\\s\\d+\\))")) {
      String[] splitted = term.split(" ");
      BigInteger num = BigInteger.valueOf(splitted[1].substring(2));
      assert num.bitLength() <= neededBitsize:
        format("numeral %s does not fit into bitvector of length %d", num, neededBitsize);
      return getNumber(num, neededBitsize);
    }

    final String casted;
    if (availableBitsize == null) {
      casted = term;
    } else if (availableBitsize < neededBitsize) {
      // TODO sign_extend vs zero_extend?
      // we produce predicates, so wrong predicates are filtered out automatically later.
      // we just have to produce matching types.
      casted = format("((_ sign_extend %d) %s)", neededBitsize - availableBitsize, term);
    } else if (availableBitsize > neededBitsize) {
      casted = format("((_ extract %d %d) %s)", neededBitsize, 0, term);
    } else {
      // availableBitsize == neededBitsize
      casted = term;
    }
    return casted;
  }

  @Override
  public Pair<String, Type<FormulaType<?>>> convertNumeral(String num) {
    BigInteger n = BigInteger.valueOf(num);
    // sufficient for a valid formula, we want one bit for bv0
    int bitsize = Math.max(1, n.bitLength());
    return Pair.of(
        getNumber(n, bitsize),
        new Type<FormulaType<?>>(FormulaType.getBitvectorTypeWithSize(bitsize)));
  }

  private String getNumber(BigInteger num, int bitsize) {
    assert !num.isNegative() : "Negative numbers should be written with an unary minus.";
    return format("(_ bv%s %d)", num, bitsize);
  }

  @Override
  public Pair<String, Type<FormulaType<?>>> convertSymbol(String symbol)
      throws UnknownFormulaSymbolException {
    return Pair.of(symbol, getType(symbol));
  }

  @Override
  public Pair<String, Type<FormulaType<?>>> convertTerm(
      Pair<String, Type<FormulaType<?>>> op, List<Pair<String, Type<FormulaType<?>>>> terms) {

    if (terms.isEmpty()) {
      return Pair.of(format("(%s)", op.getFirst()), op.getSecond()); // should not happen?

    } else if (terms.size() == 1 && op.getFirst().startsWith("(_ divisible")) {
      // we convert "((_ divisible N) X)" into "(= (_ bv0 32) (bvmod X (_ bvN 32))"
      assert op.getSecond() == null : "type of MODULO should be unknown.";
      // extract number N from "(_ divisible (_ bvN M)"
      int N = Integer.parseInt(op.getFirst().split(" ")[3].substring(2));
      int bitsize = getBVsize(Iterables.getOnlyElement(terms).getSecond().getReturnType());
      return Pair.of(
          format("(= (_ bv0 %d) (bvsrem %s (_ bv%d %d)))",
              bitsize, Iterables.getOnlyElement(terms).getFirst(), N, bitsize),
          new Type<FormulaType<?>>(FormulaType.getBitvectorTypeWithSize(bitsize)));

    } else if (terms.size() == 1 && "__string__".equals(op.getFirst())) {
      // we convert "(__string__ (_ bvN M))" into "(__string__ N)",
      // extract number N from "(_ bvN 32)", we want the "N" from "bvN"
      int n = Integer.parseInt(terms.get(0).getFirst().split(" ")[1].substring(2));
      return Pair.of(
          format("(__string__ %d)", n),
          new Type<FormulaType<?>>(op.getSecond().getReturnType()));

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
      Type<FormulaType<?>> t1 = e1.getSecond();
      Type<FormulaType<?>> t2 = e2.getSecond();
      int s1 = getBVsize(t1.getReturnType());
      int s2 = getBVsize(t2.getReturnType());
      int commonBitsize = Math.max(s1, s2); // maximum should be sound
      boolean isOpSigned = isOperationSigned(t1,t2);
      Type<FormulaType<?>> type;
      Pair<String,String> operator;
      if (binOps.containsKey(op.getFirst())) {
        operator = binOps.get(op.getFirst());
        type = new Type<>(FormulaType.BooleanType);
      } else {
        operator = arithmeticOps.get(op.getFirst());
        type = new Type<>(FormulaType.getBitvectorTypeWithSize(commonBitsize));
        type.setSigness(isOpSigned);
      }
      return Pair.of(format("(%s %s %s)",
          isOpSigned ? operator.getFirst() : operator.getSecond(),
          cast(e1.getFirst(), s1, commonBitsize),
          cast(e2.getFirst(), s2, commonBitsize)),
          type);

    } else if (terms.size() == 3 && "ite".equals(op.getFirst())) {
      Pair<String, Type<FormulaType<?>>> cond = terms.get(0);
      Pair<String, Type<FormulaType<?>>> eIf = terms.get(1);
      Pair<String, Type<FormulaType<?>>> eElse = terms.get(2);
      if (Type.BOOL.equals(eIf.getSecond())) {
        return Pair.of(format("(ite %s %s %s)",
            cond.getFirst(),
            eIf.getFirst(),
            eElse.getFirst()),
            new Type<FormulaType<?>>(FormulaType.BooleanType));
      } else {
        int sIf = getBVsize(eIf.getSecond().getReturnType());
        int sElse = getBVsize(eElse.getSecond().getReturnType());
        int commonBitsize = Math.max(sIf, sElse); // maximum should be sound
        boolean isOpSigned = isOperationSigned(eIf.getSecond(), eElse.getSecond());
        Type<FormulaType<?>> type = new Type<>(FormulaType.getBitvectorTypeWithSize(commonBitsize));
        type.setSigness(isOpSigned);
        return Pair.of(format("(ite %s %s %s)",
            cond.getFirst(),
            cast(eIf.getFirst(), sIf, commonBitsize),
            cast(eElse.getFirst(), sElse, commonBitsize)),
            type);
      }

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
        params.add(cast(terms.get(i).getFirst(),
            getBVsize(terms.get(i).getSecond().getReturnType()),
            getBVsize(op.getSecond().getParameterTypes().get(i))));
      }
      return Pair.of(
          format("(%s %s)",
              op.getFirst(), Joiner.on(' ').join(params)),
          op.getSecond());

    } else { // UF
      if (!("_".equals(op.getFirst()) && "divisible".equals(terms.get(0).getFirst()))) {
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
