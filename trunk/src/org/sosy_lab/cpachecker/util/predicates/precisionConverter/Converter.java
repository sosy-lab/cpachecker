// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.precisionConverter;

import static java.lang.String.format;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.precisionConverter.SymbolEncoding.Type;
import org.sosy_lab.cpachecker.util.predicates.precisionConverter.SymbolEncoding.UnknownFormulaSymbolException;
import org.sosy_lab.java_smt.api.FormulaType;

/** This is a dummy converter that returns the identity of the given terms and types. */
@SuppressWarnings("MissingSummary")
public class Converter {

  protected final Set<String> binBooleanOps = Sets.newHashSet("and", "or");
  protected final SymbolEncoding symbolEncoding;
  protected final LogManager logger;

  public Converter(LogManager logger, CFA cfa) {
    symbolEncoding = new SymbolEncoding(cfa);
    this.logger = logger;
  }

  @VisibleForTesting
  public Converter() {
    symbolEncoding = null;
    logger = null;
  }

  /**
   * @throws UnknownFormulaSymbolException may be thrown in subclasses
   */
  public String convertFunctionDeclaration(String symbol, Type<String> pFt)
      throws UnknownFormulaSymbolException {
    return format(
        "%s (%s) %s", symbol, Joiner.on(' ').join(pFt.getParameterTypes()), pFt.getReturnType());
  }

  /**
   * @throws UnknownFormulaSymbolException may be thrown in subclasses
   */
  public String convertFunctionDefinition(
      String symbol, Type<String> type, Pair<String, Type<FormulaType<?>>> initializerTerm)
      throws UnknownFormulaSymbolException {
    return format(
        "%s (%s) %s %s",
        symbol,
        Joiner.on(' ').join(type.getParameterTypes()),
        type.getReturnType(),
        initializerTerm.getFirst());
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
    return Pair.of(s, new Type<FormulaType<?>>(FormulaType.getBitvectorTypeWithSize(0)));
  }

  public enum PrecisionConverter {
    DISABLE,
    INT2BV,
    BV2INT
  }

  public static Converter getConverter(
      PrecisionConverter encodePredicates, CFA cfa, LogManager logger) {
    switch (encodePredicates) {
      case INT2BV:
        {
          return new BVConverter(cfa, logger);
        }
      case BV2INT:
        {
          return new IntConverter(cfa, logger);
        }
      case DISABLE:
        {
          return null;
        }
      default:
        throw new AssertionError("invalid value for option");
    }
  }
}
