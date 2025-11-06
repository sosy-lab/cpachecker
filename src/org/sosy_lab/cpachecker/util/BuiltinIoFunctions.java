// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import static org.sosy_lab.cpachecker.util.BuiltinFunctions.isFilePointer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

/**
 * This function contains utility methods for handling built-in functions in CPAchecker. It provides
 * some methods to identify, process, and manage built-in functions.
 */
public class BuiltinIoFunctions {

  private static final ImmutableMap<String, CType> supportedScanfFormatSpecifiers =
      ImmutableMap.<String, CType>builder()
          .put("%d", CNumericTypes.INT) // decimal integer
          .put("%i", CNumericTypes.INT) // decimal, octal, or hexadecimal integer
          .put("%o", CNumericTypes.UNSIGNED_INT) // octal integer
          .put("%u", CNumericTypes.UNSIGNED_INT) // unsigned decimal integer
          .put("%x", CNumericTypes.UNSIGNED_INT) // hexadecimal integer
          .put("%ld", CNumericTypes.LONG_INT) // long decimal integer
          .put("%li", CNumericTypes.LONG_INT) // long decimal, octal, or hexadecimal integer
          .put("%lo", CNumericTypes.UNSIGNED_LONG_INT) // long octal integer
          .put("%lu", CNumericTypes.UNSIGNED_LONG_INT) // long unsigned decimal integer
          .put("%lx", CNumericTypes.UNSIGNED_LONG_INT) // long hexadecimal integer
          .put("%hd", CNumericTypes.SHORT_INT) // short decimal integer
          .put("%hi", CNumericTypes.SHORT_INT) // short decimal, octal, or hexadecimal integer
          .put("%ho", CNumericTypes.UNSIGNED_SHORT_INT) // short octal integer
          .put("%hu", CNumericTypes.UNSIGNED_SHORT_INT) // short unsigned decimal integer
          .put("%hx", CNumericTypes.UNSIGNED_SHORT_INT) // short hexadecimal integer
          .put("%lld", CNumericTypes.LONG_LONG_INT) // long long decimal integer
          .put(
              "%lli",
              CNumericTypes.LONG_LONG_INT) // long long decimal, octal, or hexadecimal integer
          .put("%llo", CNumericTypes.UNSIGNED_LONG_LONG_INT) // long long octal integer
          .put("%llu", CNumericTypes.UNSIGNED_LONG_LONG_INT) // long long unsigned decimal integer
          .put("%llx", CNumericTypes.UNSIGNED_LONG_LONG_INT) // long long hexadecimal integer
          .put("%hhd", CNumericTypes.SIGNED_CHAR)
          .put("%hhi", CNumericTypes.SIGNED_CHAR)
          .put("%hhx", CNumericTypes.UNSIGNED_CHAR)
          .put("%hho", CNumericTypes.UNSIGNED_CHAR)
          .put("%hhu", CNumericTypes.UNSIGNED_CHAR)
          .put("%f", CNumericTypes.FLOAT)
          .put("%e", CNumericTypes.FLOAT)
          .put("%g", CNumericTypes.FLOAT)
          .put("%a", CNumericTypes.FLOAT)
          .put("%lf", CNumericTypes.DOUBLE)
          .put("%le", CNumericTypes.DOUBLE)
          .put("%lg", CNumericTypes.DOUBLE)
          .put("%la", CNumericTypes.DOUBLE)
          .put("%Lf", CNumericTypes.LONG_DOUBLE)
          .put("%Le", CNumericTypes.LONG_DOUBLE)
          .put("%Lg", CNumericTypes.LONG_DOUBLE)
          .put("%La", CNumericTypes.LONG_DOUBLE)
          .put("%c", CNumericTypes.CHAR)
          .buildOrThrow();

  private static final String FSCANF = "fscanf";

  public static boolean matchesFscanf(String pFunctionName) {
    return pFunctionName.equals(FSCANF);
  }

  public static Optional<CType> getTypeFromScanfFormatSpecifier(String specifier) {
    return Optional.ofNullable(supportedScanfFormatSpecifiers.get(specifier));
  }

  /**
   * Result of validating the parameters of a fscanf call.
   *
   * @param format the validated format string
   * @param receiver the receiving parameter expression
   */
  private record ValidatedFScanFParameter(String format, CExpression receiver) {}

  /**
   * Returns the set of allowed format specifiers for scanf-like functions.
   *
   * @return the set of allowed format specifiers
   */
  public static ImmutableSet<String> getAllowedScanfFormatSpecifiers() {
    return supportedScanfFormatSpecifiers.keySet();
  }

  /**
   * Checks whether the format specifier in the second argument of fscanf agrees with the type of
   * the parameter it writes to. Paragraph § 7.21.6.2 (10) of the C Standard says, that input item
   * read form the stream is converted to the `appropriate` type according to the conversion
   * specifier, e.g., %d. Further § 7.21.6.2 (11-12) tells us the expected argument (receiver) type
   * for each argument, corresponding to a conversion specifier and length modifier .The exact
   * mapping brought forward by the standard is reflected in {@link
   * BuiltinIoFunctions#getTypeFromScanfFormatSpecifier(String)}.
   *
   * @param formatString the scanf format string
   * @param pVariableType the type of the receiving variable
   * @return whether the scanf-format-specifier agrees with the type it writes to
   * @throws UnsupportedCodeException if the format specifier is not supported
   */
  private static boolean isCompatibleWithScanfFormatString(
      String formatString, CType pVariableType, CFAEdge pEdge) throws UnsupportedCodeException {
    CType expectedType =
        BuiltinIoFunctions.getTypeFromScanfFormatSpecifier(formatString)
            .orElseThrow(
                () ->
                    new UnsupportedCodeException(
                        "format specifier " + formatString + " not supported.", pEdge));

    return pVariableType.getCanonicalType().equals(expectedType.getCanonicalType());
  }

  private static ValidatedFScanFParameter validateFscanfParameters(
      List<CExpression> pParameters, CFunctionCallExpression e, CFAEdge pEdge)
      throws UnrecognizedCodeException {
    if (pParameters.size() < 2) {
      throw new UnrecognizedCodeException("fscanf() needs at least 2 parameters", pEdge, e);
    }

    if (pParameters.size() > 3) {
      throw new UnsupportedCodeException(
          "fscanf() with more than 3 parameters is not supported", pEdge, e);
    }

    CExpression file = pParameters.getFirst();

    if (file instanceof CIdExpression idExpression) {
      if (!isFilePointer(idExpression.getExpressionType())) {
        throw new UnrecognizedCodeException(
            "First parameter of fscanf() must be a FILE*", pEdge, e);
      }
    }

    CExpression format = pParameters.get(1);
    String formatString =
        checkFscanfFormatString(format)
            .orElseThrow(
                () ->
                    new UnsupportedCodeException(
                        "Format string of fscanf is not supported", pEdge, e));

    return new ValidatedFScanFParameter(formatString, pParameters.get(2));
  }

  private static Optional<String> checkFscanfFormatString(CExpression pFormat) {
    ImmutableSet<String> allowlistedFormatStrings =
        BuiltinIoFunctions.getAllowedScanfFormatSpecifiers();
    if (pFormat instanceof CStringLiteralExpression stringLiteral) {
      String content = stringLiteral.getContentWithoutNullTerminator();
      if (allowlistedFormatStrings.contains(content)) {
        return Optional.of(content);
      }
    }

    return Optional.empty();
  }

  /**
   * Creates a nondet assignment {@code CFunctionCallAssignmentStatement} to overapproximate the
   * effect of a fscanf call. The created statement assigns to the variable passed as receiving
   * parameter to fscanf a call to __VERIFIER_nondet_* of the correct type. In case this cannot be
   * done precisely, an {@code UnrecognizedCodeException} is thrown.
   *
   * <p>Only the particular cases where the call can be replaced into a __VERIFIER_nondet_* call are
   * handled. Anything else results in an {@code UnsupportedCodeException}.
   *
   * <p>The function returns a function named `__VERIFIER_nondet_ANY` whose return type matches the
   * type of the receiving variable of fscanf. The analysis making use of this function must ensure
   * that such a function is properly modeled, and that the same function name being used with
   * multiple different return types is handled correctly, by for example ignoring the function name
   * completely.
   *
   * @param e the {@code CFunctionCallExpression} representing the fscanf call
   * @param pEdge the CFA edge where the fscanf call occurs, required for proper exception handling
   * @return a {@code CFunctionCallAssignmentStatement} setting the receiving parameter of fscanf to
   *     a __VERIFIER_nondet_* call of the correct type
   * @throws UnrecognizedCodeException in case is not precise to create such an assignment
   */
  public static CFunctionCallAssignmentStatement createNondetCallModellingFscanf(
      CFunctionCallExpression e, CFAEdge pEdge) throws UnrecognizedCodeException {
    final List<CExpression> parameters = e.getParameterExpressions();
    if (!(e.getFunctionNameExpression() instanceof CIdExpression funcNameIdExpression
        && matchesFscanf(funcNameIdExpression.getName()))) {
      throw new UnrecognizedCodeException(
          "fscanf function call expected to have a direct function name called 'fscanf'.",
          pEdge,
          e);
    }

    ValidatedFScanFParameter receivingParameter =
        BuiltinIoFunctions.validateFscanfParameters(parameters, e, pEdge);

    if (receivingParameter.receiver() instanceof CUnaryExpression unaryParameter) {
      UnaryOperator operator = unaryParameter.getOperator();
      CExpression operand = unaryParameter.getOperand();
      if (operator.equals(UnaryOperator.AMPER) && operand instanceof CIdExpression idExpression) {
        // For simplicity, we start with the case where only parameters of the form "&id" occur
        CType variableType = idExpression.getExpressionType();

        if (!BuiltinIoFunctions.isCompatibleWithScanfFormatString(
            receivingParameter.format(), variableType, pEdge)) {
          throw new UnsupportedCodeException(
              "fscanf with receiving type <-> format specifier mismatch is not supported.",
              pEdge,
              e);
        }

        if (!(variableType.getCanonicalType() instanceof CSimpleType pSimpleType)) {
          throw new UnsupportedCodeException(
              "Currently, only simple types are supported as receiving parameters of fscanf.",
              pEdge,
              e);
        }

        CFunctionDeclaration nondetFun =
            new CFunctionDeclaration(
                pEdge.getFileLocation(),
                CFunctionType.functionTypeWithReturnType(pSimpleType),
                "__VERIFIER_nondet_ANY",
                ImmutableList.of(),
                ImmutableSet.of());
        CIdExpression nondetFunctionName =
            new CIdExpression(pEdge.getFileLocation(), pSimpleType, nondetFun.getName(), nondetFun);

        CFunctionCallExpression rhs =
            new CFunctionCallExpression(
                pEdge.getFileLocation(),
                pSimpleType,
                nondetFunctionName,
                ImmutableList.of(),
                nondetFun);
        return new CFunctionCallAssignmentStatement(pEdge.getFileLocation(), idExpression, rhs);
      } else {
        throw new UnsupportedCodeException(
            "Currently, only fscanf with a single parameter of the form &id is supported.",
            pEdge,
            e);
      }
    } else {
      throw new UnsupportedCodeException(
          "Currently, only fscanf with a single parameter of the form &id is supported.", pEdge, e);
    }
  }
}
