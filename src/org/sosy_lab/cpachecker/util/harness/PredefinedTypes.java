// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.harness;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;

public final class PredefinedTypes {

  private PredefinedTypes() {

  }

  public static boolean isPredefinedType(@Nullable CTypeDeclaration pDeclaration) {
    if (pDeclaration == null) {
      return false;
    }
    String originalName = pDeclaration.getOrigName();
    if (originalName == null) {
      return false;
    }
    return ImmutableSet.of("size_t", "wchar_t").contains(originalName)
        || isCalledDiv(originalName);
  }

  public static boolean isPredefinedFunction(@Nullable AFunctionDeclaration pDeclaration) {
    return isMalloc(pDeclaration)
        || isMemcpy(pDeclaration)
        || isMemset(pDeclaration)
        || isFree(pDeclaration)
        || isExit(pDeclaration)
        || isAbort(pDeclaration)
        || isPrintf(pDeclaration)
        || isSwprintf(pDeclaration)
        || isWcstombs(pDeclaration)
        || isDiv(pDeclaration)
        || isVerifierError(pDeclaration)
        || isVerifierAssume(pDeclaration);
  }

  public static boolean isKnownTestFunction(@Nullable AFunctionDeclaration pDeclaration) {
    return isMalloc(pDeclaration)
        || isCalloc(pDeclaration)
        || isFree(pDeclaration)
        || isExit(pDeclaration)
        || isAbort(pDeclaration)
        || isPrintf(pDeclaration)
        || isVerifierError(pDeclaration)
        || isVerifierAssume(pDeclaration);
  }

  public static boolean isPredefinedFunctionWithoutVerifierError(
      @Nullable AFunctionDeclaration pDeclaration) {
    return !isVerifierError(pDeclaration) && isPredefinedFunction(pDeclaration);
  }

  public static Type getCanonicalType(Type pType) {
    if (pType instanceof CType) {
      return ((CType) pType).getCanonicalType();
    }
    return pType;
  }

  private static boolean isMalloc(@Nullable AFunctionDeclaration pDeclaration) {
    return functionMatchesExactType(
        pDeclaration,
        "malloc",
        CPointerType.POINTER_TO_VOID,
        Collections.singletonList(PredefinedTypes::isIntegerType));
  }

  private static boolean isCalloc(@Nullable AFunctionDeclaration pDeclaration) {
    return functionMatchesExactType(
        pDeclaration,
        "calloc",
        CPointerType.POINTER_TO_VOID,
        ImmutableList.of(PredefinedTypes::isIntegerType, PredefinedTypes::isIntegerType));
  }

  private static boolean isMemcpy(@Nullable AFunctionDeclaration pDeclaration) {
    return functionMatchesExactType(
        pDeclaration,
        "memcpy",
        CPointerType.POINTER_TO_VOID,
        ImmutableList.of(
            Predicate.isEqual(CPointerType.POINTER_TO_VOID),
            Predicate.isEqual(new CPointerType(false, false, CVoidType.create(true, false))),
            PredefinedTypes::isIntegerType));
  }

  private static boolean isMemset(@Nullable AFunctionDeclaration pDeclaration) {
    return functionMatchesExactType(
        pDeclaration,
        "memset",
        CPointerType.POINTER_TO_VOID,
        ImmutableList.of(
            Predicate.isEqual(CPointerType.POINTER_TO_VOID),
            PredefinedTypes::isIntegerType,
            PredefinedTypes::isIntegerType));
  }

  private static boolean isFree(@Nullable AFunctionDeclaration pDeclaration) {
    return functionMatchesExactType(
        pDeclaration,
        "free",
        CVoidType.VOID,
        Collections.singletonList(Predicate.isEqual(CPointerType.POINTER_TO_VOID)));
  }

  private static boolean isExit(@Nullable AFunctionDeclaration pDeclaration) {
    return functionMatches(
        pDeclaration,
        ImmutableList.of("exit", "_Exit")::contains,
        Predicate.isEqual(CVoidType.VOID),
        Collections.singletonList(PredefinedTypes::isIntegerType));
  }

  private static boolean isAbort(@Nullable AFunctionDeclaration pDeclaration) {
    return functionMatchesExactType(pDeclaration, "abort", CVoidType.VOID, ImmutableList.of());
  }

  private static boolean isPrintf(@Nullable AFunctionDeclaration pDeclaration) {
    return functionMatchesExactType(
        pDeclaration,
        "printf",
        CNumericTypes.INT,
        Collections.singletonList(
            Predicate.isEqual(
                new CPointerType(false, false, CNumericTypes.CHAR.getCanonicalType(true, false)))));
  }

  private static boolean isSwprintf(@Nullable AFunctionDeclaration pDeclaration) {
    Predicate<Type> isPointerToIntegral = t -> {
      Type type = getCanonicalType(t);
      if (!(type instanceof CPointerType)) {
        return false;
      }
      return isIntegerType(((CPointerType) type).getType());
    };
    return functionMatchesExactType(
        pDeclaration,
        "swprintf",
        CNumericTypes.INT,
        ImmutableList.of(isPointerToIntegral, isPointerToIntegral));
  }

  private static boolean isWcstombs(@Nullable AFunctionDeclaration pDeclaration) {
    Predicate<Type> isPointerToIntegral = t -> {
      Type type = getCanonicalType(t);
      if (!(type instanceof CPointerType)) {
        return false;
      }
      return isIntegerType(((CPointerType) type).getType());
    };
    return functionMatches(
        pDeclaration,
        "wcstombs",
        PredefinedTypes::isIntegerType,
        ImmutableList.of(isPointerToIntegral, isPointerToIntegral, PredefinedTypes::isIntegerType));
  }

  private static boolean isDiv(@Nullable AFunctionDeclaration pDeclaration) {
    if (functionMatches(
        pDeclaration,
        "div",
        PredefinedTypes::isDivType,
        ImmutableList.of(
            Predicate.isEqual(CNumericTypes.INT),
            Predicate.isEqual(CNumericTypes.INT)))) {
      return true;
    }
    if (functionMatches(
            pDeclaration,
            "ldiv",
            PredefinedTypes::isDivType,
            ImmutableList.of(
                Predicate.isEqual(CNumericTypes.LONG_INT),
                Predicate.isEqual(CNumericTypes.LONG_INT)))) {
      return true;
    }
    return functionMatches(
        pDeclaration,
        "lldiv",
        PredefinedTypes::isDivType,
        ImmutableList.of(
            Predicate.isEqual(CNumericTypes.LONG_LONG_INT),
            Predicate.isEqual(CNumericTypes.LONG_LONG_INT)));
  }

  private static boolean isDivType(Type pType) {
    return isCalledDiv(pType.toString().trim());
  }

  private static boolean isCalledDiv(String pTypeName) {
    return ImmutableList.of("div_t", "ldiv_t", "lldiv_t").contains(pTypeName);
  }

  private static boolean isVerifierError(@Nullable AFunctionDeclaration pDeclaration) {
    return functionMatchesExactType(pDeclaration, "__VERIFIER_error", CVoidType.VOID, ImmutableList.of());
  }

  public static boolean isVerifierAssume(@Nullable AFunctionDeclaration pDeclaration) {
    return functionMatchesExactType(
        pDeclaration,
        "__VERIFIER_assume",
        CVoidType.VOID,
        ImmutableList.of(PredefinedTypes::isIntegerType));
  }

  private static boolean isIntegerType(Type pType) {
    Type type = getCanonicalType(pType);
    if (type instanceof JSimpleType) {
      return ((JSimpleType) type).getType().isIntegerType();
    }
    if (type instanceof CSimpleType) {
      return ((CSimpleType) type).getType().isIntegerType();
    }
    return false;
  }

  private static boolean functionMatchesExactType(
      @Nullable AFunctionDeclaration pDeclaration,
      String pExpectedName,
      Type pExpectedReturnType,
      List<Predicate<Type>> pExpectedParameterTypes) {
    return functionMatches(pDeclaration, pExpectedName, Predicate.isEqual(getCanonicalType(pExpectedReturnType)), pExpectedParameterTypes);
  }

  private static boolean functionMatches(
      @Nullable AFunctionDeclaration pDeclaration,
      String pExpectedName,
      Predicate<Type> pExpectedReturnType,
      List<Predicate<Type>> pExpectedParameterTypes) {
    return functionMatches(pDeclaration, Predicate.isEqual(pExpectedName), pExpectedReturnType, pExpectedParameterTypes);
  }

  private static boolean functionMatches(
      @Nullable AFunctionDeclaration pDeclaration,
      Predicate<String> pExpectedName,
      Predicate<Type> pExpectedReturnType,
      List<Predicate<Type>> pExpectedParameterTypes) {
    if (pDeclaration == null) {
      return false;
    }
    if (!pExpectedName.test(pDeclaration.getOrigName())) {
      return false;
    }
    Type actualReturnType = pDeclaration.getType().getReturnType();
    if (!pExpectedReturnType.test(actualReturnType)
        && !pExpectedReturnType.test(getCanonicalType(actualReturnType))) {
      return false;
    }
    if (pDeclaration.getParameters().size() != pExpectedParameterTypes.size()) {
      return false;
    }
    Iterator<Predicate<Type>> expectedParameterTypeIt = pExpectedParameterTypes.iterator();
    for (AParameterDeclaration parameterDeclaration : pDeclaration.getParameters()) {
      Type actualParameterType = parameterDeclaration.getType();
      Predicate<Type> expectedParameterType = expectedParameterTypeIt.next();
      if (!expectedParameterType.test(actualParameterType)
          && !expectedParameterType.test(getCanonicalType(actualParameterType))) {
        return false;
      }
    }
    return true;
  }
}