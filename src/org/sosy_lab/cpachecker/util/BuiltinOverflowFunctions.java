/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.util;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class BuiltinOverflowFunctions {
  // https://gcc.gnu.org/onlinedocs/gcc/Integer-Overflow-Builtins.html
  private static final String ADD = of("addl");
  private static final String ADDL = of("addl");
  private static final String SADDL = of("saddl");
  private static final String SADDLL = of("saddll");
  private static final String UADD = of("uadd");
  private static final String UADDL = of("uaddl");
  private static final String UADDLL = of("uaddll");

  private static final String SUB = of("sub");
  private static final String SUBL = of("subl");
  private static final String SSUBL = of("ssubl");
  private static final String SSUBLL = of("ssubll");
  private static final String USUB = of("usub");
  private static final String USUBL = of("usubl");
  private static final String USUBLL = of("usubll");

  // TODO: add missing overflow functions like multiplication; also add more tests to
  // test/programs/simple/builtin_overflow_functions/

  private static final String PREFIX = "__builtin_";
  private static final String SUFFIX = "_overflow";

  private static final ImmutableList<String> possibleIdentifiers =
      ImmutableList.<String>builder()
          .add(ADD)
          .add(ADDL)
          .add(SADDL)
          .add(SADDLL)
          .add(UADD)
          .add(UADDL)
          .add(UADDLL)
          .add(SUB)
          .add(SUBL)
          .add(SSUBL)
          .add(SSUBLL)
          .add(USUB)
          .add(USUBL)
          .add(USUBLL)
          .build();

  private static String of(String identifier) {
    return PREFIX + identifier + SUFFIX;
  }

  private static String getShortIdentifiers(String identifier) {
    // TODO: replace this by a compiled regex
    return identifier.replaceFirst(PREFIX, "").replaceFirst(SUFFIX, "");
  }

  /**
   * resolve the type of the built-yin overflow function. This is important since the input
   * parameters have to be casted in case their type differs TODO: solve this with an enum of the
   * different function names instead.
   */
  public static CSimpleType getType(String functionName) {
    String shortIdentifier = getShortIdentifiers(functionName);
    boolean unsigned = (shortIdentifier.startsWith("u"));
    int size;
    if (shortIdentifier.endsWith("ll")) {
      size = 2;
    } else if (shortIdentifier.endsWith("l")) {
      size = 1;
    } else {
      size = 0;
    }
    if (unsigned) {
      switch (size) {
        case 0:
          return CNumericTypes.UNSIGNED_INT;
        case 1:
          return CNumericTypes.UNSIGNED_LONG_INT;
        case 2:
          return CNumericTypes.UNSIGNED_LONG_LONG_INT;
      }
    } else {
      switch (size) {
        case 0:
          return CNumericTypes.SIGNED_INT;
        case 1:
          return CNumericTypes.SIGNED_LONG_INT;
        case 2:
          return CNumericTypes.SIGNED_LONG_LONG_INT;
      }
    }
    return null;
  }

  private static BinaryOperator getOperator(String functionName) {
    String shortIdentifier = getShortIdentifiers(functionName);
    if (shortIdentifier.contains("add")) {
      return BinaryOperator.PLUS;
    } else {
      return BinaryOperator.MINUS;
    }
  }

  /**
   * Check whether a given function is a builtin function specific to overflows that can be further
   * analyzed with this class.
   */
  public static boolean isBuiltinOverflowFunction(String pFunctionName) {
    return possibleIdentifiers.contains(pFunctionName);
  }

  /**
   * This method returns a {@link CExpression} that represents the truth value of checking whether
   * an arithmetic operation performed on the input expressions var1 and var1 overflows.
   *
   * @throws UnrecognizedCodeException when building the result fails due to unrecognized code
   */
  public static CExpression handleOverflow(
      OverflowAssumptionManager ofmgr, CExpression var1, CExpression var2, String pFunctionName)
      throws UnrecognizedCodeException {
    // TODO: make this more efficient (but probably not worth the effort):
    checkState(possibleIdentifiers.contains(pFunctionName));
    CSimpleType type = getType(pFunctionName);
    BinaryOperator operator = getOperator(pFunctionName);
    CExpression castedVar1 = new CCastExpression(FileLocation.DUMMY, type, var1);
    CExpression castedVar2 = new CCastExpression(FileLocation.DUMMY, type, var2);
    return ofmgr.getConjunctionOfAdditiveAssumptions(
        castedVar1, castedVar2, operator, type, true);
  }

  public static CExpression handleOverflowSideeffects(
      OverflowAssumptionManager ofmgr, CExpression var1, CExpression var2, String pFunctionName)
      throws UnrecognizedCodeException {
    // TODO: make this more efficient (but probably not worth the effort):
    checkState(possibleIdentifiers.contains(pFunctionName));
    // TODO: remove code duplication between handleOverflowSideeffects and handleOverflow
    String shortIdentifier = getShortIdentifiers(pFunctionName);
    CSimpleType type = getType(shortIdentifier);
    BinaryOperator operator = getOperator(shortIdentifier);
    CExpression castedVar1 = new CCastExpression(FileLocation.DUMMY, type, var1);
    CExpression castedVar2 = new CCastExpression(FileLocation.DUMMY, type, var2);
    return ofmgr.getResultOfAdditiveOperation(castedVar1, castedVar2, operator);
  }
}
