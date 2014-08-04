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
package org.sosy_lab.cpachecker.util.predicates.z3;

import static org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApi.*;
import static org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApiConstants.*;

import java.math.BigInteger;

import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractBitvectorFormulaManager;

import com.google.common.base.Preconditions;

class Z3BitvectorFormulaManager extends AbstractBitvectorFormulaManager<Long, Long, Long> {

  private final long z3context;

  protected Z3BitvectorFormulaManager(Z3FormulaCreator creator) {
    super(creator);
    this.z3context = creator.getEnv();
  }

  @Override
  public Long concat(Long pFirst, Long pSecond) {
    return mk_concat(z3context, pFirst, pSecond);
  }

  @Override
  public Long extract(Long pFirst, int pMsb, int pLsb) {
    return mk_extract(z3context, pMsb, pLsb, pFirst);
  }

  @Override
  public Long extend(Long pNumber, int pExtensionBits, boolean pSigned) {
    if (pSigned) {
      return mk_sign_ext(z3context, pExtensionBits, pNumber);
    } else {
      return mk_zero_ext(z3context, pExtensionBits, pNumber);
    }
  }

  @Override
  public Long makeBitvectorImpl(int pLength, long pI) {
    long sort = mk_bv_sort(z3context, pLength);
    return mk_int64(z3context, pI, sort);
  }

  @Override
  protected Long makeBitvectorImpl(int pLength, BigInteger pI) {
    return makeBitvectorImpl(pLength, pI.toString());
  }

  @Override
  public Long makeBitvectorImpl(int pLength, String pI) {
    long sort = mk_bv_sort(z3context, pLength);
    return mk_numeral(z3context, pI, sort);
  }


  @Override
  public Long makeVariableImpl(int length, String varName) {
    long type = getFormulaCreator().getBittype(length);
    return getFormulaCreator().makeVariable(type, varName);
  }

  /**
   * Returns a term representing the (arithmetic if signed is true) right shift of number by toShift.
   */
  @Override
  public Long shiftRight(Long number, Long toShift, boolean signed) {
    if (signed) {
      return mk_bvashr(z3context, number, toShift);
    } else {
      return mk_bvlshr(z3context, number, toShift);
    }
  }

  @Override
  public Long shiftLeft(Long number, Long toShift) {
    return mk_bvshl(z3context, number, toShift);
  }

  @Override
  public Long not(Long pBits) {
    return mk_bvnot(z3context, pBits);
  }

  @Override
  public Long and(Long pBits1, Long pBits2) {
    return mk_bvand(z3context, pBits1, pBits2);
  }

  @Override
  public Long or(Long pBits1, Long pBits2) {
    return mk_bvor(z3context, pBits1, pBits2);
  }

  @Override
  public Long xor(Long pBits1, Long pBits2) {
    return mk_bvxor(z3context, pBits1, pBits2);
  }

  @Override
  public boolean isNot(Long pBits) {
    return isOP(z3context, pBits, Z3_OP_BNOT);
  }

  @Override
  public boolean isAnd(Long pBits) {
    return isOP(z3context, pBits, Z3_OP_BAND);
  }

  @Override
  public boolean isOr(Long pBits) {
    return isOP(z3context, pBits, Z3_OP_BOR);
  }

  @Override
  public boolean isXor(Long pBits) {
    return isOP(z3context, pBits, Z3_OP_BXOR);
  }

  @Override
  public Long negate(Long pNumber) {
    return mk_bvneg(z3context, pNumber);
  }

  @Override
  public Long add(Long pNumber1, Long pNumber2) {
    return mk_bvadd(z3context, pNumber1, pNumber2);
  }

  @Override
  public Long subtract(Long pNumber1, Long pNumber2) {
    return mk_bvsub(z3context, pNumber1, pNumber2);
  }

  @Override
  public Long divide(Long pNumber1, Long pNumber2, boolean signed) {
    if (signed) {
      return mk_bvsdiv(z3context, pNumber1, pNumber2);
    } else {
      return mk_bvudiv(z3context, pNumber1, pNumber2);
    }
  }

  @Override
  public Long modulo(Long pNumber1, Long pNumber2, boolean signed) {
    if (signed) {
      return mk_bvsrem(z3context, pNumber1, pNumber2);
    } else {
      return mk_bvurem(z3context, pNumber1, pNumber2);
    }
  }

  @Override
  public Long multiply(Long pNumber1, Long pNumber2) {
    return mk_bvmul(z3context, pNumber1, pNumber2);
  }

  @Override
  public Long equal(Long pNumber1, Long pNumber2) {
    return mk_eq(z3context, pNumber1, pNumber2);
  }

  @Override
  public Long lessThan(Long pNumber1, Long pNumber2, boolean signed) {
    if (signed) {
      return mk_bvslt(z3context, pNumber1, pNumber2);
    } else {
      return mk_bvult(z3context, pNumber1, pNumber2);
    }
  }

  @Override
  public Long lessOrEquals(Long pNumber1, Long pNumber2, boolean signed) {
    if (signed) {
      return mk_bvsle(z3context, pNumber1, pNumber2);
    } else {
      return mk_bvule(z3context, pNumber1, pNumber2);
    }
  }

  @Override
  public Long greaterThan(Long pNumber1, Long pNumber2, boolean signed) {
    return lessThan(pNumber2, pNumber1, signed);
  }

  @Override
  public Long greaterOrEquals(Long pNumber1, Long pNumber2, boolean signed) {
    return lessOrEquals(pNumber2, pNumber1, signed);
  }

  @Override
  public boolean isNegate(Long pNumber) {
    return isOP(z3context, pNumber, Z3_OP_BNOT);
  }

  @Override
  public boolean isAdd(Long pNumber) {
    return isOP(z3context, pNumber, Z3_OP_BADD);
  }

  @Override
  public boolean isSubtract(Long pNumber) {
    return isOP(z3context, pNumber, Z3_OP_BSUB);
  }

  @Override
  public boolean isDivide(Long pNumber, boolean signed) {
    if (signed) {
      return isOP(z3context, pNumber, Z3_OP_BSDIV);
    } else {
      return isOP(z3context, pNumber, Z3_OP_BUDIV);
    }
  }

  @Override
  public boolean isModulo(Long pNumber, boolean signed) {
    if (signed) {
      return isOP(z3context, pNumber, Z3_OP_BSREM);
    } else {
      return isOP(z3context, pNumber, Z3_OP_BUREM);
    }
  }

  @Override
  public boolean isMultiply(Long pNumber) {
    return isOP(z3context, pNumber, Z3_OP_BMUL);
  }

  @Override
  public boolean isEqual(Long pNumber) {
    return isOP(z3context, pNumber, Z3_OP_EQ);
  }

  @Override
  public boolean isGreaterThan(Long pNumber, boolean signed) {
    return isLessThan(pNumber, signed);
  }

  @Override
  public boolean isGreaterOrEquals(Long pNumber, boolean signed) {
    return isLessOrEquals(pNumber, signed);
  }

  @Override
  public boolean isLessThan(Long pNumber, boolean signed) {
    if (signed) {
      return isOP(z3context, pNumber, Z3_OP_SLT);
    } else {
      return isOP(z3context, pNumber, Z3_OP_ULT);
    }
  }

  @Override
  public boolean isLessOrEquals(Long pNumber, boolean signed) {
    if (signed) {
      return isOP(z3context, pNumber, Z3_OP_SLEQ);
    } else {
      return isOP(z3context, pNumber, Z3_OP_ULEQ);
    }
  }

  @Override
  public int getLength(Long pParam) {
    long sort = get_sort(z3context, pParam);
    Preconditions.checkArgument(get_sort_kind(z3context, sort) == Z3_BV_SORT);
    return get_bv_sort_size(z3context, sort);
  }
}
