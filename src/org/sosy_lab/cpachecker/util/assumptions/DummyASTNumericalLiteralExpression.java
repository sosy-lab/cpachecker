/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.assumptions;

import org.sosy_lab.cpachecker.cfa.ast.IASTLiteralExpression;

/**
 * Hack!!
 * @author g.theoduloz
 */
public class DummyASTNumericalLiteralExpression extends IASTLiteralExpression {

  public DummyASTNumericalLiteralExpression(final String r) {
    super(null, null, null, lk_integer_constant, r);
  }

  @Override
  public String getRawSignature() {
    return getValue().toString();
  }

  @Override
  public String toString() {
    return getRawSignature();
  }

  /* Constants, assuming 32-bit machine */
  public static final DummyASTNumericalLiteralExpression ZERO = new DummyASTNumericalLiteralExpression("0");
  public static final DummyASTNumericalLiteralExpression FALSE = ZERO;
  public static final DummyASTNumericalLiteralExpression ONE = new DummyASTNumericalLiteralExpression("1");
  public static final DummyASTNumericalLiteralExpression TRUE = ONE;

  public static final DummyASTNumericalLiteralExpression INT_MAX = new DummyASTNumericalLiteralExpression("2147483647");
  public static final DummyASTNumericalLiteralExpression INT_MIN = new DummyASTNumericalLiteralExpression("-2147483648");
  public static final DummyASTNumericalLiteralExpression UINT_MIN = ZERO;
  public static final DummyASTNumericalLiteralExpression UINT_MAX = new DummyASTNumericalLiteralExpression("4294967295");

  public static final DummyASTNumericalLiteralExpression LONG_MAX = new DummyASTNumericalLiteralExpression("2147483647");
  public static final DummyASTNumericalLiteralExpression LONG_MIN = new DummyASTNumericalLiteralExpression("-2147483648");
  public static final DummyASTNumericalLiteralExpression ULONG_MIN = ZERO;
  public static final DummyASTNumericalLiteralExpression ULONG_MAX = new DummyASTNumericalLiteralExpression("4294967295");

  public static final DummyASTNumericalLiteralExpression SHRT_MAX = new DummyASTNumericalLiteralExpression("32767");
  public static final DummyASTNumericalLiteralExpression SHRT_MIN = new DummyASTNumericalLiteralExpression("-32768");
  public static final DummyASTNumericalLiteralExpression USHRT_MIN = ZERO;
  public static final DummyASTNumericalLiteralExpression USHRT_MAX = new DummyASTNumericalLiteralExpression("65535");

  public static final DummyASTNumericalLiteralExpression CHAR_MAX = new DummyASTNumericalLiteralExpression("127");
  public static final DummyASTNumericalLiteralExpression CHAR_MIN = new DummyASTNumericalLiteralExpression("-128");
  public static final DummyASTNumericalLiteralExpression UCHAR_MIN = ZERO;
  public static final DummyASTNumericalLiteralExpression UCHAR_MAX = new DummyASTNumericalLiteralExpression("255");

}
