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
package org.sosy_lab.cpachecker.util.predicates.mathsat;

import static org.sosy_lab.cpachecker.util.predicates.mathsat.NativeApi.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;

/**
 * Implementation of MathsatFormulaManager for formulas with bitvectors.
 */
@Options(prefix="cpa.predicate.mathsat")
public class BitwiseMathsatFormulaManager extends MathsatFormulaManager {

  private final int bitWidth;
  private final boolean signed;

  private static Pattern BITVECTOR_PATTERN = Pattern.compile("^0d\\d+_(\\d+)$");

  public BitwiseMathsatFormulaManager(Configuration config, LogManager logger, int pBitWidth, boolean pSigned) throws InvalidConfigurationException {
    super(config, logger, MSAT_BV + pBitWidth);
    config.inject(this, BitwiseMathsatFormulaManager.class);

    bitWidth = pBitWidth;
    signed = pSigned;
  }

  @Override
  long createEnvironment(boolean pShared, boolean pGhostFilter) {
    long env = super.createEnvironment(pShared, pGhostFilter);

    msat_add_theory(env, MSAT_WORD);

    return env;
  }

  @Override
  long interpreteBitvector(long pBv) {
    String lTermRepresentation = msat_term_repr(pBv);
    // the term is of the format "0d<WIDTH>_<VALUE>"
    Matcher matcher =  BITVECTOR_PATTERN.matcher(lTermRepresentation);
    if (!matcher.matches()) {
      throw new NumberFormatException("Unknown bitvector format: " + lTermRepresentation);
    }

    String term = matcher.group(1);
    long value = Long.valueOf(term);

    if (signed && (bitWidth <= 63)) {
      if (value >= (1L << (bitWidth-1))) {
        // positive number that should be interpreted as negative
        value = value - (1L << bitWidth);
      }
    }

    return value;
  }

  // ----------------- Numeric formulas -----------------

  @Override
  public Formula makeNumber(int i) {
    return makeNumber(Integer.toString(i));
  }

  @Override
  public Formula makeNumber(String i) {
    i = "0d" + bitWidth + "_" + i;
    Formula result = encapsulate(msat_make_number(msatEnv, i));
    return result;
  }

  @Override
  public Formula makeNegate(Formula pF) {
    return makeMinus(makeNumber(0), pF);
  }

  @Override
  public Formula makePlus(Formula pF1, Formula pF2) {
    return encapsulate(msat_make_bv_plus(msatEnv, getTerm(pF1), getTerm(pF2)));
  }

  @Override
  public Formula makeMinus(Formula pF1, Formula pF2) {
    return encapsulate(msat_make_bv_minus(msatEnv, getTerm(pF1), getTerm(pF2)));
  }

  @Override
  public Formula makeDivide(Formula pF1, Formula pF2) {
    if (signed) {
      return encapsulate(msat_make_bv_sdiv(msatEnv, getTerm(pF1), getTerm(pF2)));
    } else {
      return encapsulate(msat_make_bv_udiv(msatEnv, getTerm(pF1), getTerm(pF2)));
    }
  }

  @Override
  public Formula makeModulo(Formula pF1, Formula pF2) {
    if (signed) {
      return encapsulate(msat_make_bv_smod(msatEnv, getTerm(pF1), getTerm(pF2)));
    } else {
      throw new UnsupportedOperationException("Unsigned modulo");
    }
  }

  @Override
  public Formula makeMultiply(Formula pF1, Formula pF2) {
    return encapsulate(msat_make_bv_times(msatEnv, getTerm(pF1), getTerm(pF2)));
  }


  // ----------------- Numeric relations -----------------

  @Override
  public Formula makeEqual(Formula pF1, Formula pF2) {
    return encapsulate(msat_make_equal(msatEnv, getTerm(pF1), getTerm(pF2)));
  }

  @Override
  public Formula makeGt(Formula pF1, Formula pF2) {
    if (signed) {
      return encapsulate(msat_make_bv_sgt(msatEnv, getTerm(pF1), getTerm(pF2)));
    } else {
      return encapsulate(msat_make_bv_ugt(msatEnv, getTerm(pF1), getTerm(pF2)));
    }
  }

  @Override
  public Formula makeGeq(Formula pF1, Formula pF2) {
    if (signed) {
      return encapsulate(msat_make_bv_sgeq(msatEnv, getTerm(pF1), getTerm(pF2)));
    } else {
      return encapsulate(msat_make_bv_ugeq(msatEnv, getTerm(pF1), getTerm(pF2)));
    }
  }

  @Override
  public Formula makeLt(Formula pF1, Formula pF2) {
    if (signed) {
      return encapsulate(msat_make_bv_slt(msatEnv, getTerm(pF1), getTerm(pF2)));
    } else {
      return encapsulate(msat_make_bv_ult(msatEnv, getTerm(pF1), getTerm(pF2)));
    }
  }

  @Override
  public Formula makeLeq(Formula pF1, Formula pF2) {
    if (signed) {
      return encapsulate(msat_make_bv_sgeq(msatEnv, getTerm(pF1), getTerm(pF2)));
    } else {
      return encapsulate(msat_make_bv_uleq(msatEnv, getTerm(pF1), getTerm(pF2)));
    }
  }

  // ----------------- Bit-manipulation functions -----------------

  @Override
  public Formula makeBitwiseNot(Formula pF) {
    return encapsulate(msat_make_bv_not(msatEnv, getTerm(pF)));
  }

  @Override
  public Formula makeBitwiseAnd(Formula pF1, Formula pF2) {
    return encapsulate(msat_make_bv_and(msatEnv, getTerm(pF1), getTerm(pF2)));
  }

  @Override
  public Formula makeBitwiseOr(Formula pF1, Formula pF2) {
    return encapsulate(msat_make_bv_or(msatEnv, getTerm(pF1), getTerm(pF2)));
  }

  @Override
  public Formula makeBitwiseXor(Formula pF1, Formula pF2) {
    return encapsulate(msat_make_bv_xor(msatEnv, getTerm(pF1), getTerm(pF2)));
  }

  @Override
  public Formula makeShiftLeft(Formula pF1, Formula pAmount) {
    return encapsulate(msat_make_bv_lsl(msatEnv, getTerm(pF1), getTerm(pAmount)));
  }

  @Override
  public Formula makeShiftRight(Formula pF1, Formula pAmount) {
    if (signed) {
      return encapsulate(msat_make_bv_asr(msatEnv, getTerm(pF1), getTerm(pAmount)));
    } else {
      return encapsulate(msat_make_bv_lsr(msatEnv, getTerm(pF1), getTerm(pAmount)));
    }
  }

  @Override
  public Formula getBitwiseAxioms(Formula pF) {
    return makeTrue();
  }
}
