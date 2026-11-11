// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.cdt.core.dom.ast.IASTAttribute;
import org.eclipse.cdt.core.dom.ast.IASTToken;
import org.eclipse.cdt.core.dom.ast.IASTTokenList;
import org.eclipse.cdt.core.parser.IGCCToken;
import org.eclipse.cdt.core.parser.IToken;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;

/**
 * Evaluates the argument of the GCC attribute aligned
 * (https://gcc.gnu.org/onlinedocs/gcc/Common-Attributes.html#index-aligned). CDT provides it only
 * as token sequence, so we support only integer literals, parentheses, the operators {@code << >> +
 * - * / %}, and sizeof and alignof of basic types, typedef names, and pointers.
 */
final class AlignedAttributeEvaluator {

  /** Binary operators, from lowest to highest precedence. */
  private static final ImmutableList<
          ImmutableMap<Integer, BiFunction<BigInteger, BigInteger, BigInteger>>>
      OPERATORS =
          ImmutableList.of(
              ImmutableMap.of(
                  IToken.tSHIFTL, (a, b) -> a.shiftLeft(b.intValueExact()),
                  IToken.tSHIFTR, (a, b) -> a.shiftRight(b.intValueExact())),
              ImmutableMap.of(IToken.tPLUS, BigInteger::add, IToken.tMINUS, BigInteger::subtract),
              ImmutableMap.of(
                  IToken.tSTAR, BigInteger::multiply,
                  IToken.tDIV, BigInteger::divide,
                  IToken.tMOD, BigInteger::mod));

  private static final ImmutableMap<Integer, CBasicType> BASIC_TYPES =
      ImmutableMap.<Integer, CBasicType>builder()
          .put(IToken.t_char, CBasicType.CHAR)
          .put(IToken.t_int, CBasicType.INT)
          .put(IToken.t_float, CBasicType.FLOAT)
          .put(IToken.t_double, CBasicType.DOUBLE)
          .put(IToken.t__Bool, CBasicType.BOOL)
          .put(IGCCToken.t__int128, CBasicType.INT128)
          .put(IGCCToken.t__float128, CBasicType.FLOAT128)
          .buildOrThrow();

  private static final ImmutableSet<Integer> TYPE_MODIFIERS =
      ImmutableSet.of(IToken.t_long, IToken.t_short, IToken.t_signed, IToken.t_unsigned);

  private final MachineModel machineModel;
  private final ParseContext parseContext;
  private final ASTLiteralConverter literalConverter;
  private final Function<String, @Nullable CType> typedefLookup;

  /**
   * @param pTypedefLookup returns the type of a typedef name, or null if it is not a typedef name
   */
  AlignedAttributeEvaluator(
      MachineModel pMachineModel,
      ParseContext pParseContext,
      Function<String, @Nullable CType> pTypedefLookup) {
    machineModel = pMachineModel;
    parseContext = pParseContext;
    literalConverter = new ASTLiteralConverter(pMachineModel, pParseContext);
    typedefLookup = pTypedefLookup;
  }

  /** Return the tokens of an attribute argument as flat list. */
  static ImmutableList<IASTToken> getTokens(IASTToken pArgument) {
    ImmutableList.Builder<IASTToken> result = ImmutableList.builder();
    addTokens(pArgument, result);
    return result.build();
  }

  private static void addTokens(IASTToken pToken, ImmutableList.Builder<IASTToken> pResult) {
    if (pToken instanceof IASTTokenList tokenList) {
      for (IASTToken token : tokenList.getTokens()) {
        addTokens(token, pResult);
      }
    } else {
      pResult.add(pToken);
    }
  }

  /**
   * Evaluate the tokens of the argument of the given attribute.
   *
   * @param pAttribute the attribute, used for locations and error messages
   */
  BigInteger evaluate(List<IASTToken> pTokens, IASTAttribute pAttribute) {
    PeekingIterator<IASTToken> it = Iterators.peekingIterator(pTokens.iterator());
    BigInteger result = evaluate(it, 0, pAttribute);
    if (it.hasNext()) {
      throw parseContext.parseError("Unsupported argument of aligned attribute", pAttribute);
    }
    return result;
  }

  /** Evaluate operators of the given precedence level and higher (recursive descent). */
  private BigInteger evaluate(
      PeekingIterator<IASTToken> pTokens, int pLevel, IASTAttribute pAttribute) {
    if (pLevel == OPERATORS.size()) {
      return evaluatePrimary(pTokens, pAttribute);
    }
    Map<Integer, BiFunction<BigInteger, BigInteger, BigInteger>> operators = OPERATORS.get(pLevel);
    BigInteger result = evaluate(pTokens, pLevel + 1, pAttribute);
    while (pTokens.hasNext() && operators.containsKey(pTokens.peek().getTokenType())) {
      BiFunction<BigInteger, BigInteger, BigInteger> operator =
          operators.get(pTokens.next().getTokenType());
      result = operator.apply(result, evaluate(pTokens, pLevel + 1, pAttribute));
    }
    return result;
  }

  private BigInteger evaluatePrimary(PeekingIterator<IASTToken> pTokens, IASTAttribute pAttribute) {
    IASTToken token = nextToken(pTokens, pAttribute);
    return switch (token.getTokenType()) {
      case IToken.tINTEGER ->
          ((CIntegerLiteralExpression)
                  literalConverter.parseIntegerLiteral(
                      parseContext.getLocation(pAttribute),
                      String.valueOf(token.getTokenCharImage()),
                      token))
              .getValue();
      case IToken.tLPAREN -> {
        BigInteger result = evaluate(pTokens, 0, pAttribute);
        expectToken(pTokens, IToken.tRPAREN, pAttribute);
        yield result;
      }
      case IToken.t_sizeof -> machineModel.getSizeof(convertType(pTokens, pAttribute));
      case IGCCToken.t___alignof__, IToken.t__Alignof, IToken.t_alignof ->
          BigInteger.valueOf(machineModel.getAlignof(convertType(pTokens, pAttribute)));
      default -> throw parseContext.parseError("Unsupported argument of aligned attribute", token);
    };
  }

  /** Convert a parenthesized type name: a pointer, a typedef name, or a basic type. */
  private CType convertType(PeekingIterator<IASTToken> pTokens, IASTAttribute pAttribute) {
    expectToken(pTokens, IToken.tLPAREN, pAttribute);
    List<IASTToken> tokens = new ArrayList<>();
    for (IASTToken token = nextToken(pTokens, pAttribute);
        token.getTokenType() != IToken.tRPAREN;
        token = nextToken(pTokens, pAttribute)) {
      tokens.add(token);
    }
    List<Integer> types = Lists.transform(tokens, IASTToken::getTokenType);

    if (types.contains(IToken.tSTAR)) {
      return CPointerType.POINTER_TO_VOID; // all pointers have the same size and alignment
    }
    if (types.equals(ImmutableList.of(IToken.tIDENTIFIER))) {
      CType type = typedefLookup.apply(String.valueOf(tokens.getFirst().getTokenCharImage()));
      if (type != null) {
        return type;
      }
    }
    if (types.isEmpty()
        || !types.stream()
            .allMatch(t -> BASIC_TYPES.containsKey(t) || TYPE_MODIFIERS.contains(t))) {
      throw parseContext.parseError(
          "Unsupported type in argument of aligned attribute", pAttribute);
    }
    int longs = Collections.frequency(types, IToken.t_long);
    return new CSimpleType(
        CTypeQualifiers.NONE,
        types.stream()
            .map(BASIC_TYPES::get)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(CBasicType.UNSPECIFIED),
        longs == 1,
        types.contains(IToken.t_short),
        types.contains(IToken.t_signed),
        types.contains(IToken.t_unsigned),
        false,
        false,
        longs == 2);
  }

  private IASTToken nextToken(PeekingIterator<IASTToken> pTokens, IASTAttribute pAttribute) {
    if (!pTokens.hasNext()) {
      throw parseContext.parseError("Unsupported argument of aligned attribute", pAttribute);
    }
    return pTokens.next();
  }

  private void expectToken(
      PeekingIterator<IASTToken> pTokens, int pTokenType, IASTAttribute pAttribute) {
    if (nextToken(pTokens, pAttribute).getTokenType() != pTokenType) {
      throw parseContext.parseError("Unsupported argument of aligned attribute", pAttribute);
    }
  }
}
