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
package org.sosy_lab.cpachecker.util.predicates.z3;

import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractRationalFormulaManager;

import com.microsoft.z3.Native;
import com.microsoft.z3.Z3Exception;
import com.microsoft.z3.enumerations.Z3_decl_kind;


public class Z3RationalFormulaManager extends AbstractRationalFormulaManager<Long> {

  private Z3FormulaCreator creator;
  private long ctx;

  public Z3RationalFormulaManager(Z3FormulaCreator pCreator) {
    super(pCreator);
    this.creator = pCreator;
    this.ctx = pCreator.getEnv();
  }

  @Override
  protected Long makeNumberImpl(long pI) {
    try {
      return Native.mkNumeral(ctx, String.valueOf(pI), creator.getNumberType());
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long makeVariableImpl(String pI) {
    try {
      return Native.mkConst(ctx, Native.mkStringSymbol(ctx, pI), creator.getNumberType());
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long negate(Long pParam1) {
    try {
      return Native.mkNot(ctx, pParam1);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long add(Long pParam1, Long pParam2) {
    try {
      return Native.mkAdd(ctx, 2, new long[] { pParam1, pParam2 });
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long subtract(Long pParam1, Long pParam2) {
    try {
      return Native.mkSub(ctx, 2, new long[] { pParam1, pParam2 });
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long divide(Long pParam1, Long pParam2) {
    try {
      return Native.mkDiv(ctx, pParam1, pParam2);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long modulo(Long pParam1, Long pParam2) {
    try {
      return Native.mkMod(ctx, pParam1, pParam2);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long multiply(Long pParam1, Long pParam2) {
    try {
      return Native.mkMul(ctx, 2, new long[] { pParam1, pParam2 });
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long equal(Long pParam1, Long pParam2) {
    try {
      return Native.mkEq(ctx, pParam1, pParam2);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long greaterThan(Long pParam1, Long pParam2) {
    try {
      return Native.mkGt(ctx, pParam1, pParam2);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long greaterOrEquals(Long pParam1, Long pParam2) {
    try {
      return Native.mkGe(ctx, pParam1, pParam2);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long lessThan(Long pParam1, Long pParam2) {
    try {
      return Native.mkLt(ctx, pParam1, pParam2);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long lessOrEquals(Long pParam1, Long pParam2) {
    try {
      return Native.mkLe(ctx, pParam1, pParam2);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isNegate(Long pParam) {
    try {
      return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_NOT.toInt();
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isAdd(Long pParam) {
    try {
      return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_ADD.toInt();
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isSubtract(Long pParam) {
    try {
      return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_SUB.toInt();
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isDivide(Long pParam) {
    try {
      return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_DIV.toInt();
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isModulo(Long pParam) {
    try {
      return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_NOT.toInt();
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isMultiply(Long pParam) {
    try {
      return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_MUL.toInt();
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isEqual(Long pParam) {
    try {
      return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_EQ.toInt();
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isGreaterThan(Long pParam) {
    try {
      return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_GT.toInt();
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isGreaterOrEquals(Long pParam) {
    try {
      return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_GE.toInt();
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isLessThan(Long pParam) {
    try {
      return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_LT.toInt();
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isLessOrEquals(Long pParam) {
    try {
      return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_LE.toInt();
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

}
