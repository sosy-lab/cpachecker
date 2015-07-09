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

import java.math.BigInteger;

import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractBitvectorFormulaManager;

class Z3BitvectorFormulaManager extends AbstractBitvectorFormulaManager<Long, Long, Long> {

  private final long z3context;

  Z3BitvectorFormulaManager(Z3FormulaCreator creator) {
    super(creator);
    this.z3context = creator.getEnv();
  }

  @Override
  public Long concat(Long pFirst, Long pSecond) {
    return mk_concat(z3context, pFirst, pSecond);
  }

  @Override
  public Long extract(Long pFirst, int pMsb, int pLsb, boolean pSigned) {
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
    long sort = mk_bv_sort(z3context, pLength);
    return mk_numeral(z3context, pI.toString(), sort);
  }

  @Override
  public Long makeVariableImpl(int length, String varName) {
    long type = getFormulaCreator().getBitvectorType(length);
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
  protected Long modularCongruence(Long pNumber1, Long pNumber2, long pModulo) {
    return mk_true(z3context);
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
}