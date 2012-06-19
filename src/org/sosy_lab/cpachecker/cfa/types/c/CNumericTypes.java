/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.types.c;

import java.math.BigInteger;

import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;


public class CNumericTypes {

  private CNumericTypes() { }

  private static CIntegerLiteralExpression create(long l) {
    return new CIntegerLiteralExpression(null, null, BigInteger.valueOf(l));
  }

  public static final CIntegerLiteralExpression ZERO = create(0L);
  public static final CIntegerLiteralExpression ONE = create(1L);

  public static final CIntegerLiteralExpression FALSE = ZERO;
  public static final CIntegerLiteralExpression TRUE = ONE;

  /* type bounds, assuming 32-bit machine */
  // TODO use MachineModel here
  public static final CIntegerLiteralExpression INT_MAX = create(2147483647L);
  public static final CIntegerLiteralExpression INT_MIN = create(-2147483648L);
  public static final CIntegerLiteralExpression UINT_MIN = ZERO;
  public static final CIntegerLiteralExpression UINT_MAX = create(4294967295L);

  public static final CIntegerLiteralExpression LONG_MAX = create(2147483647L);
  public static final CIntegerLiteralExpression LONG_MIN = create(-2147483648L);
  public static final CIntegerLiteralExpression ULONG_MIN = ZERO;
  public static final CIntegerLiteralExpression ULONG_MAX = create(4294967295L);

  public static final CIntegerLiteralExpression SHRT_MAX = create(32767L);
  public static final CIntegerLiteralExpression SHRT_MIN = create(-32768L);
  public static final CIntegerLiteralExpression USHRT_MIN = ZERO;
  public static final CIntegerLiteralExpression USHRT_MAX = create(65535L);

  public static final CIntegerLiteralExpression CHAR_MAX = create(127L);
  public static final CIntegerLiteralExpression CHAR_MIN = create(-128L);
  public static final CIntegerLiteralExpression UCHAR_MIN = ZERO;
  public static final CIntegerLiteralExpression UCHAR_MAX = create(255L);

}
