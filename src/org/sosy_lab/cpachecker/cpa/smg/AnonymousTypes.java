/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg;

import java.math.BigInteger;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;


public class AnonymousTypes {
  public static CType createTypeWithLength(int pSizeInBits) {
    if (pSizeInBits % 8 == 0) {
      CIntegerLiteralExpression arrayLen = new CIntegerLiteralExpression(FileLocation.DUMMY,
          CNumericTypes.UNSIGNED_LONG_INT, BigInteger.valueOf(pSizeInBits / 8));
      return new CArrayType(false, false, CNumericTypes.SIGNED_CHAR, arrayLen);
    } else {
      CSimpleType fieldType = new CSimpleType(false, false, CBasicType.CHAR, false, false, true, false, false, false, false);
      CType bitFieldType = fieldType.withBitFieldSize(pSizeInBits);
      return bitFieldType;
    }
  }
}
