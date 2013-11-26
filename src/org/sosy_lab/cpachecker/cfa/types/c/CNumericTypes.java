/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import java.math.BigDecimal;
import java.math.BigInteger;

import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;


public class CNumericTypes {

  private CNumericTypes() { }

  // type constants
  public final static CSimpleType CHAR          = new CSimpleType(false, false, CBasicType.CHAR, false, false, false, false, false, false, false);
  public static final CSimpleType SIGNED_CHAR   = new CSimpleType(false, false, CBasicType.CHAR, false, false, true, false, false, false, false);
  public final static CSimpleType INT           = new CSimpleType(false, false, CBasicType.INT, false, false, false, false, false, false, false);
  public final static CSimpleType SIGNED_INT    = new CSimpleType(false, false, CBasicType.INT, false, false, true, false, false, false, false);
  public final static CSimpleType SHORT_INT     = new CSimpleType(false, false, CBasicType.INT, false, true, false, false, false, false, false);
  public final static CSimpleType LONG_INT      = new CSimpleType(false, false, CBasicType.INT, true, false, false, false, false, false, false);
  public final static CSimpleType LONG_LONG_INT = new CSimpleType(false, false, CBasicType.INT, false, false, false, false, false, false, true);
  // New for floating point number support
  public final static CSimpleType FLOAT         = new CSimpleType(false, false, CBasicType.FLOAT, false, false, false, false, false, false, false);
  public final static CSimpleType DOUBLE        = new CSimpleType(false, false, CBasicType.DOUBLE, false, false, false, false, false, false, false);
  public final static CSimpleType LONG_DOUBLE   = new CSimpleType(false, false, CBasicType.DOUBLE, true, false, false, false, false, false, false);

  public final static CSimpleType VOID          = new CSimpleType(false, false, CBasicType.VOID, false, false, false, false, false, false, false);

  private static CIntegerLiteralExpression createInt(long l, CType type) {
    return new CIntegerLiteralExpression(null, type, BigInteger.valueOf(l));
  }

  //New for floating point number support
  private static CFloatLiteralExpression createFloat(long l, CType type) {
    return new CFloatLiteralExpression(null, type, BigDecimal.valueOf(l));
  }

  public static final CIntegerLiteralExpression ZERO = createInt(0L, INT);
  public static final CIntegerLiteralExpression ONE = createInt(1L, INT);

  // TODO: Tobi: what are these good for?
  //public static final CFloatLiteralExpression ZERO = create(0L, INT);
  //public static final CFloatLiteralExpression ONE = create(1L, INT);

  /* type bounds, assuming 32-bit machine */
  // TODO move to MachineModel
  public static final CIntegerLiteralExpression INT_MAX = createInt(2147483647L, INT);
  public static final CIntegerLiteralExpression INT_MIN = createInt(-2147483648L, INT);

  // TODO: Tobi: what are the correct values?
  public static final CFloatLiteralExpression LONG_MAX = createFloat(Long.MAX_VALUE, LONG_DOUBLE);
  public static final CFloatLiteralExpression LONG_MIN = createFloat(Long.MIN_VALUE, LONG_DOUBLE);

}
