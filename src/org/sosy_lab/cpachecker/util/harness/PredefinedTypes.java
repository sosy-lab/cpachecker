/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.harness;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
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

final class PredefinedTypes {

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
    return originalName.equals("size_t");
  }

  public static boolean isPredefinedFunction(@Nullable AFunctionDeclaration pDeclaration) {
    return isMalloc(pDeclaration)
        || isMemcpy(pDeclaration)
        || isMemset(pDeclaration)
        || isFree(pDeclaration)
        || isExit(pDeclaration)
        || isPrintf(pDeclaration)
        || isVerifierError(pDeclaration)
        || isVerifierAssume(pDeclaration);
  }

  public static Type getCanonicalType(Type pType) {
    if (pType instanceof CType) {
      return ((CType) pType).getCanonicalType();
    }
    return pType;
  }

  private static boolean isMalloc(@Nullable AFunctionDeclaration pDeclaration) {
    return functionMatches(
        pDeclaration,
        "malloc",
        CPointerType.POINTER_TO_VOID,
        Collections.singletonList(PredefinedTypes::isIntegerType));
  }

  private static boolean isMemcpy(@Nullable AFunctionDeclaration pDeclaration) {
    return functionMatches(
        pDeclaration,
        "memcpy",
        CPointerType.POINTER_TO_VOID,
        ImmutableList.of(
            Predicates.equalTo(CPointerType.POINTER_TO_VOID),
            Predicates.equalTo(new CPointerType(false, false, CVoidType.create(true, false))),
            PredefinedTypes::isIntegerType));
  }

  private static boolean isMemset(@Nullable AFunctionDeclaration pDeclaration) {
    return functionMatches(
        pDeclaration,
        "memset",
        CPointerType.POINTER_TO_VOID,
        ImmutableList.of(
            Predicates.equalTo(CPointerType.POINTER_TO_VOID),
            PredefinedTypes::isIntegerType,
            PredefinedTypes::isIntegerType));
  }

  private static boolean isFree(@Nullable AFunctionDeclaration pDeclaration) {
    return functionMatches(
        pDeclaration,
        "free",
        CVoidType.VOID,
        Collections.singletonList(Predicates.equalTo(CPointerType.POINTER_TO_VOID)));
  }

  private static boolean isExit(@Nullable AFunctionDeclaration pDeclaration) {
    return functionMatches(
        pDeclaration,
        "exit",
        CVoidType.VOID,
        Collections.singletonList(PredefinedTypes::isIntegerType));
  }

  private static boolean isPrintf(@Nullable AFunctionDeclaration pDeclaration) {
    return functionMatches(
        pDeclaration,
        "printf",
        CNumericTypes.INT,
        ImmutableList.of(
            Predicates.equalTo(
                new CPointerType(false, false, CNumericTypes.CHAR.getCanonicalType(true, false)))));
  }

  private static boolean isVerifierError(@Nullable AFunctionDeclaration pDeclaration) {
    return functionMatches(pDeclaration, "__VERIFIER_error", CVoidType.VOID, ImmutableList.of());
  }

  private static boolean isVerifierAssume(@Nullable AFunctionDeclaration pDeclaration) {
    return functionMatches(
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
    assert false : "Unsupported type: " + pType;
    return false;
  }

  private static boolean functionMatches(
      @Nullable AFunctionDeclaration pDeclaration,
      String pExpectedName,
      Type pExpectedReturnType,
      List<Predicate<Type>> pExpectedParameterTypes) {
    if (pDeclaration == null) {
      return false;
    }
    if (!pDeclaration.getOrigName().equals(pExpectedName)) {
      return false;
    }
    Type actualReturnType = getCanonicalType(pDeclaration.getType().getReturnType());
    if (!actualReturnType.equals(getCanonicalType(pExpectedReturnType))) {
      return false;
    }
    if (pDeclaration.getParameters().size() != pExpectedParameterTypes.size()) {
      return false;
    }
    Iterator<Predicate<Type>> expectedParameterTypeIt = pExpectedParameterTypes.iterator();
    for (AParameterDeclaration parameterDeclaration : pDeclaration.getParameters()) {
      Type actualParameterType = getCanonicalType(parameterDeclaration.getType());
      if (!expectedParameterTypeIt.next().apply(actualParameterType)) {
        return false;
      }
    }
    return true;
  }
}