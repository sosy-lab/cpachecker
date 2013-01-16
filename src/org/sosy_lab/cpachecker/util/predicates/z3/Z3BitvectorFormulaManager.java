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

import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractBitvectorFormulaManager;

import com.microsoft.z3.Native;
import com.microsoft.z3.Z3Exception;
import com.microsoft.z3.enumerations.Z3_decl_kind;


public class Z3BitvectorFormulaManager extends AbstractBitvectorFormulaManager<Long> {

  private final Z3FormulaCreator creator;
  private final long ctx;

  protected Z3BitvectorFormulaManager(Z3FormulaCreator creator) {
    super(creator);
    this.creator = creator;
    this.ctx = creator.getEnv();
  }

  public static Z3BitvectorFormulaManager create(Z3FormulaCreator creator) {
    return new Z3BitvectorFormulaManager(creator);
  }

  @Override
  protected Long negate(Long pParam1) {
    try {
      return Native.mkBvneg(ctx, pParam1);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long add(Long pParam1, Long pParam2) {
    try {
      return Native.mkBvadd(ctx, pParam1, pParam2);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long subtract(Long pParam1, Long pParam2) {
    try {
      return Native.mkBvsub(ctx, pParam1, pParam2);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long divide(Long pParam1, Long pParam2, boolean pSigned) {
    try {
      if (pSigned) {
        return Native.mkBvsdiv(ctx, pParam1, pParam2);
      } else {
        return Native.mkBvudiv(ctx, pParam1, pParam2);
      }
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long modulo(Long pParam1, Long pParam2, boolean pSigned) {
    try {
      return Native.mkBvsmod(ctx, pParam1, pParam2); // TODO: unsigned?
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long multiply(Long pParam1, Long pParam2) {
    try {
      return Native.mkBvmul(ctx, pParam1, pParam2);
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
  protected Long greaterThan(Long pParam1, Long pParam2, boolean pSigned) {
    try {
      if (pSigned) {
        return Native.mkBvsgt(ctx, pParam1, pParam2);
      } else {
        return Native.mkBvugt(ctx, pParam1, pParam2);
      }
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long greaterOrEquals(Long pParam1, Long pParam2, boolean pSigned) {
    try {
      if (pSigned) {
        return Native.mkBvsge(ctx, pParam1, pParam2);
      } else {
        return Native.mkBvuge(ctx, pParam1, pParam2);
      }
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long lessThan(Long pParam1, Long pParam2, boolean pSigned) {
    try {
      if (pSigned) {
        return Native.mkBvslt(ctx, pParam1, pParam2);
      } else {
        return Native.mkBvult(ctx, pParam1, pParam2);
      }
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long lessOrEquals(Long pParam1, Long pParam2, boolean pSigned) {
    try {
      if (pSigned) {
        return Native.mkBvsle(ctx, pParam1, pParam2);
      } else {
        return Native.mkBvule(ctx, pParam1, pParam2);
      }
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isNegate(Long pParam) {
    try {
      return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_BNEG.toInt();
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isAdd(Long pParam) {
    try {
      return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_BADD.toInt();
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isSubtract(Long pParam) {
    try {
      return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_BSUB.toInt();
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isDivide(Long pParam, boolean pSigned) {
    try {
      if (pSigned) {
        return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_BSDIV.toInt();
      } else {
        return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_BUDIV.toInt();
      }
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isModulo(Long pParam, boolean pSigned) {
    try {
      return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_BSMOD.toInt(); // TODO: unsigned?
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isMultiply(Long pParam) {
    try {
      return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_BMUL.toInt();
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
  protected boolean isGreaterThan(Long pParam, boolean pSigned) {
    try {
      if (pSigned) {
        return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_SGT.toInt();
      } else {
        return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_UGT.toInt();
      }
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isGreaterOrEquals(Long pParam, boolean pSigned) {
    try {
      if (pSigned) {
        return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_SGEQ.toInt();
      } else {
        return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_UGEQ.toInt();
      }
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isLessThan(Long pParam, boolean pSigned) {
    try {
      if (pSigned) {
        return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_SLT.toInt();
      } else {
        return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_ULT.toInt();
      }
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isLessOrEquals(Long pParam, boolean pSigned) {
    try {
      if (pSigned) {
        return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_SLEQ.toInt();
      } else {
        return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_ULEQ.toInt();
      }
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long not(Long pParam1) {
    try {
      return Native.mkBvnot(ctx, pParam1);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long and(Long pParam1, Long pParam2) {
    try {
      return Native.mkBvand(ctx, pParam1, pParam2);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long or(Long pParam1, Long pParam2) {
    try {
      return Native.mkBvor(ctx, pParam1, pParam2);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long xor(Long pParam1, Long pParam2) {
    try {
      return Native.mkBvxor(ctx, pParam1, pParam2);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isNot(Long pParam) {
    try {
      return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_BNOT.toInt();
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isAnd(Long pParam) {
    try {
      return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_BAND.toInt();
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isOr(Long pParam) {
    try {
      return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_BOR.toInt();
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isXor(Long pParam) {
    try {
      return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_BXOR.toInt();
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long makeBitvectorImpl(int pLength, long pI) {
    try {
      return Native.mkNumeral(ctx, String.valueOf(pI), creator.getBittype(pLength));
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long makeVariableImpl(int pLength, String pVar) {
    return creator.makeVariable(creator.getBittype(pLength), pVar);
  }

  @Override
  protected Long shiftRight(Long pNumber, Long pToShift, boolean pSigned) {
    try {
      return Native.mkBvlshr(ctx, pToShift, pNumber); // TODO: (un)signed & artihmetical?
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long shiftLeft(Long pExtract, Long pExtract2) { // why extract?
    try {
      return Native.mkBvshl(ctx, pExtract2, pExtract);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long concat(Long pNumber, Long pAppend) {
    try {
      return Native.mkConcat(ctx, pAppend, pNumber);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long extract(Long pNumber, int pMsb, int pLsb) {
    try {
      return Native.mkExtract(ctx, pMsb, pLsb, pNumber);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected int getLength(Long pParam) {
    try {
      return Native.getBvSortSize(ctx, Native.getSort(ctx, pParam));
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

}
