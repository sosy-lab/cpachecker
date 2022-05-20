// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;

/**
 * This class implements {@link CFloat} to propose a bit-precise representation of different
 * floating point number formats, which are used in C, and operations on them.
 *
 * <p>The implementation is oriented at the descriptions of the IEEE-754 Standard as well as various
 * observations and experiments performed using the GNU gcc compiler in version 5.4.0.
 */
public class CFloatImpl extends CFloat {

  /**
   * Those default values are used to handle some cases with a good performance and can be used to
   * easily compose other values, e.g., <code>-35 = (3 * 10 + 5) * -1</code>
   */
  private static final ImmutableList<String> DEFAULT_VALUES =
      ImmutableList.copyOf(
          new String[] {
            "-0.0", "-0", "-1", "0", "0.0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
            "nan", "-nan", "inf", "-inf"
          });

  /** The wrapper contains the exponent and significant (mantissa) of the {@link CFloat} instance */
  private final CFloatWrapper wrapper;

  /** The type of the represented floating point number, e.g., <code>double</code> */
  private final int type;

  /**
   * A simple constructor to create an instance of {@link CFloat} given a bit representation and a
   * type.
   *
   * @param pWrapper the {@link CFloatWrapper} instance containing the bit representation
   * @param pType the type of the {@link CFloat} instance - be careful to choose a type that
   *     actually corresponds to the given {@link CFloatWrapper}
   */
  public CFloatImpl(CFloatWrapper pWrapper, int pType) {
    wrapper = pWrapper;
    type = pType;
  }

  /**
   * A more complex constructor to create a {@link CFloat} instance from a {@link String}
   * representation and a given type.
   *
   * <p>Note that there are literals that may not necessarily have a precise representation in a
   * finite binary floating point format.
   *
   * @param pRep the {@link String} literal representing the floating point number
   * @param pType the type of the {@link CFloat} instance
   */
  public CFloatImpl(final String pRep, final int pType) {
    type = pType;

    if (DEFAULT_VALUES.contains(pRep.toLowerCase())) {
      wrapper = new CFloatWrapper();
      long exp = 0;
      long man = 0;

      switch (pRep.toLowerCase()) {
        case "0.0":
        case "0":
          break;
        case "1":
          exp = getBias();
          man = getNormalizationMask() & getNormalizedMantissaMask();
          break;
        case "2":
          exp = getBias() + 1L;
          man = getNormalizationMask() & getNormalizedMantissaMask();
          break;
        case "3":
          exp = getBias() + 1L;
          man =
              ((getNormalizationMask() >>> 1) + getNormalizationMask())
                  & getNormalizedMantissaMask();
          break;
        case "4":
          exp = getBias() + 2L;
          man = getNormalizationMask() & getNormalizedMantissaMask();
          break;
        case "5":
          exp = getBias() + 2L;
          man =
              ((getNormalizationMask() >>> 2) + getNormalizationMask())
                  & getNormalizedMantissaMask();
          break;
        case "6":
          exp = getBias() + 2L;
          man =
              ((getNormalizationMask() >>> 1) + getNormalizationMask())
                  & getNormalizedMantissaMask();
          break;
        case "7":
          exp = getBias() + 2L;
          man =
              ((getNormalizationMask() >>> 2)
                      + (getNormalizationMask() >>> 1)
                      + getNormalizationMask())
                  & getNormalizedMantissaMask();
          break;
        case "8":
          exp = getBias() + 3L;
          man = getNormalizationMask() & getNormalizedMantissaMask();
          break;
        case "9":
          exp = getBias() + 3L;
          man =
              ((getNormalizationMask() >>> 3) + getNormalizationMask())
                  & getNormalizedMantissaMask();
          break;
        case "10":
          exp = getBias() + 3L;
          man =
              ((getNormalizationMask() >>> 2) + getNormalizationMask())
                  & getNormalizedMantissaMask();
          break;
        case "-1":
          exp = getSignBitMask() + getBias();
          man = (pType == CFloatNativeAPI.FP_TYPE_LONG_DOUBLE ? getNormalizationMask() : 0L);
          break;
        case "nan":
          exp = 1L;
          man = 0L;
          break;
        case "-nan":
          exp = getSignBitMask() + 1L;
          man = 0L;
          break;
        case "inf":
          exp = getExponentMask();
          man = 1L;
          break;
        case "-inf":
          exp = getExponentMask() + getSignBitMask();
          man = 1L;
          break;
        case "-0.0":
        case "-0":
          exp = getSignBitMask();
          man = 0L;
          break;
        default:
          throw new RuntimeException("Default case '" + pRep + "' is not yet implemented!");
      }

      wrapper.setExponent(exp);
      wrapper.setMantissa(man);
    } else {
      List<String> parts = Splitter.on('.').splitToList(pRep);
      boolean negative = pRep.startsWith("-");

      CFloat integral = null;
      CFloat fractional = null;

      if (!parts.get(0).isEmpty()) {
        integral = makeIntegralPart(parts.get(0), pType);
      }
      if (parts.size() > 1 && !parts.get(1).isEmpty()) {
        fractional = makeFractionalPart(parts.get(1), pType);
      }

      CFloat result = new CFloatImpl("0", pType);

      if (integral != null) {
        result = result.add(integral);
      }
      if (fractional != null) {
        result = result.add(fractional);
      }

      if (negative) {
        CFloat nOne = new CFloatImpl("-1", pType);
        result = result.multiply(nOne);
      }

      wrapper = result.castTo(pType).copyWrapper();
    }
  }

  private CFloat makeIntegralPart(final String pRep, final int pType) {
    String rep = null;

    if (pRep.startsWith("-")) {
      rep = pRep.substring(1);
    } else {
      rep = pRep;
    }

    List<String> digits = Arrays.asList(rep.split(""));

    CFloat result = fromString(pType, digits);

    return result;
  }

  private CFloat fromString(final int pType, final List<String> pDigits) {
    int[] decArray = new int[pDigits.size()];
    for (int i = 0; i < decArray.length; i++) {
      decArray[i] = Byte.parseByte(pDigits.get(i));
    }

    int[] auxArray = new int[decArray.length];
    int[] bitArray =
        new int[getMantissaLength() * 2 + (pType == CFloatNativeAPI.FP_TYPE_LONG_DOUBLE ? 0 : 2)];

    int effectiveExponent = 0;

    if (decimalEqual(decArray, auxArray)) {
      // shouldn't happen since the case 0 should already
      // be caught by default, but for completeness consider
      // it anyway
      // TODO initialize better default-objects
      return CFloatNativeAPI.ZERO_SINGLE;
    }

    boolean incrementExponent = true;
    boolean stillConverging = true;
    while (decimalAGreaterThanB(decArray, auxArray) && stillConverging) {
      int[] loopArray = new int[auxArray.length];
      loopArray[loopArray.length - 1] = 1;
      int[] bitAuxArray = new int[bitArray.length];
      if (effectiveExponent < bitAuxArray.length - 1) {
        bitAuxArray[effectiveExponent] = 1;
      }
      int safeCounter = effectiveExponent;

      stillConverging = false;

      while (decimalAGreaterThanB(decArray, decimalAdd(loopArray, auxArray))) {
        stillConverging = true;
        if (incrementExponent) {
          effectiveExponent++;
        } else {
          if (safeCounter >= bitAuxArray.length) {
            if (safeCounter == bitAuxArray.length) {
              bitAuxArray[bitAuxArray.length - 1] = 1;
            }
            safeCounter--;
          } else {
            bitAuxArray = binaryDouble(bitAuxArray);
          }
        }
        loopArray = decimalDouble(loopArray);
      }

      if (decimalEqual(decArray, decimalAdd(loopArray, auxArray))) {
        bitArray = binaryAdd(bitAuxArray, bitArray);
        auxArray = decimalAdd(loopArray, auxArray);
      } else if (stillConverging) {
        if (incrementExponent) {
          effectiveExponent--;
        } else {
          binaryHalf(bitAuxArray);
        }
        decimalHalf(loopArray);

        bitArray = binaryAdd(bitAuxArray, bitArray);
        auxArray = decimalAdd(loopArray, auxArray);
      }
      incrementExponent = false;
    }

    long mantissa = 0L;
    long overflow = 0L;

    for (int i = 0; i < (bitArray.length / 2); i++) {
      mantissa ^= ((long) bitArray[i]) << (bitArray.length / 2 - 1 - i);
      overflow ^= ((long) bitArray[i + bitArray.length / 2]) << (63 - i);
    }

    CFloatWrapper rWrapper = new CFloatWrapper(effectiveExponent + getBias(), mantissa);

    rWrapper = round(rWrapper, overflow);
    rWrapper.setMantissa(rWrapper.getMantissa() & getNormalizedMantissaMask());

    return new CFloatImpl(rWrapper, pType);
  }

  private void binaryHalf(final int[] pArray) {
    digitwiseHalf(pArray, 2);
  }

  private void decimalHalf(final int[] pArray) {
    digitwiseHalf(pArray, 10);
  }

  private void digitwiseHalf(final int[] pArray, final int pRadix) {
    for (int i = pArray.length - 1; i > 0; i--) {
      if ((pArray[i - 1] % 2) != 0) {
        pArray[i] += pRadix;
      }
      pArray[i] /= 2;

      if (i == 1) {
        pArray[0] /= 2;
      }
    }
  }

  private int[] binaryAdd(final int[] pArrayA, final int[] pArrayB) {
    return digitwiseAdd(pArrayA, pArrayB, 2);
  }

  private int[] decimalAdd(final int[] pArrayA, final int[] pArrayB) {
    return digitwiseAdd(pArrayA, pArrayB, 10);
  }

  private int[] digitwiseAdd(final int[] pArrayA, final int[] pArrayB, final int pRadix) {
    int[] rArray = new int[Math.max(pArrayB.length, pArrayA.length) + 1];

    for (int i = rArray.length - 1; i >= 0; i--) {
      rArray[i] +=
          (((i - 1) < pArrayA.length && i > 0) ? pArrayA[i - 1] : 0)
              + (((i - 1) < pArrayB.length && i > 0) ? pArrayB[i - 1] : 0);
      if (rArray[i] > (pRadix - 1)) {
        if (i > 0) {
          // TODO that shouldn't happen, perhaps assert?
          rArray[i - 1] += (rArray[i] / pRadix);
        }
        rArray[i] %= pRadix;
      }
    }

    if (rArray[0] == 0) {
      rArray = copyAllButFirstCell(rArray);
    }

    return rArray;
  }

  private boolean decimalEqual(final int[] pA, final int[] pB) {
    int diff = pB.length - pA.length;

    if (diff < 0) {
      diff *= -1;
      for (int i = 0; i < diff; i++) {
        if (pA[i] > 0) {
          return false;
        }
      }

      for (int i = 0; i < pB.length; i++) {
        if (pA[i + diff] != pB[i]) {
          return false;
        }
      }
    } else {
      for (int i = 0; i < diff; i++) {
        if (pB[i] > 0) {
          return false;
        }
      }

      for (int i = 0; i < pA.length; i++) {
        if (pA[i] != pB[i + diff]) {
          return false;
        }
      }
    }

    return true;
  }

  private boolean decimalAGreaterThanB(final int[] pA, final int[] pB) {
    int diff = pB.length - pA.length;

    if (diff < 0) {
      diff *= -1;
      for (int i = 0; i < diff; i++) {
        if (pA[i] > 0) {
          return true;
        }
      }

      for (int i = 0; i < pB.length; i++) {
        if (pA[i + diff] > pB[i]) {
          return true;
        }
        if (pA[i + diff] < pB[i]) {
          return false;
        }
      }
    } else {
      for (int i = 0; i < diff; i++) {
        if (pB[i] > 0) {
          return false;
        }
      }

      for (int i = 0; i < pA.length; i++) {
        if (pA[i] > pB[i + diff]) {
          return true;
        }
        if (pA[i] < pB[i + diff]) {
          return false;
        }
      }
    }

    return false;
  }

  private int[] binaryDouble(final int[] pArray) {
    return digitwiseDouble(pArray, 2);
  }

  private int[] decimalDouble(final int[] pDecimalArray) {
    return digitwiseDouble(pDecimalArray, 10);
  }

  private int[] digitwiseDouble(int[] pArray, int pRadix) {
    int[] rArray = new int[pArray.length + 1];

    for (int i = 1; i < rArray.length; i++) {
      // Overflow case can be ignored, just caught to avoid array out of bounds for meaningless
      // input
      rArray[i] = (byte) (pArray[i - 1] * 2);
      if (rArray[i] > (pRadix - 1)) {
        rArray[i - 1] += rArray[i] / pRadix;
        rArray[i] %= pRadix;
      }
    }

    if (rArray[0] == 0) {
      for (int i = 0; i < rArray.length - 1; i++) {
        rArray[i] = rArray[i + 1];
      }
      rArray = Arrays.copyOf(rArray, pArray.length);
    }

    return rArray;
  }

  private CFloat makeFractionalPart(final String pRep, final int pType) {
    List<String> digits = Arrays.asList(pRep.split(""));
    CFloat ten = new CFloatImpl("10", CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);
    CFloat divisor = new CFloatImpl("1", CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);

    CFloat result = new CFloatImpl("0", pType);
    for (String pDigit : digits) {
      result = result.multiply(ten);
      divisor = divisor.multiply(ten);
      CFloat fD = new CFloatImpl(pDigit, pType);
      result = result.add(fD);
    }
    result = result.divideBy(divisor);

    return result;
  }

  @Override
  public CFloat add(final CFloat pSummand) {
    CFloat tSummand = this;
    CFloat oSummand = pSummand;

    // cast to equal types to simplify operations
    if (tSummand.getType() != oSummand.getType()) {
      if (tSummand.getType() > oSummand.getType()) {
        oSummand = oSummand.castTo(tSummand.getType());
      } else {
        tSummand = tSummand.castTo(oSummand.getType());
      }
    }

    // if at least one operand is nan, the result is too
    if (tSummand.isNan() || oSummand.isNan()) {
      return new CFloatNaN(tSummand.isNegative(), tSummand.getType());
    }

    // handle infinities separately:
    // > x + inf = inf
    // > x - inf = -inf
    // > inf + inf = inf
    // > inf - inf = -nan
    if (tSummand.isInfinity()) {
      if (oSummand.isInfinity() && (tSummand.isNegative() != oSummand.isNegative())) {
        return new CFloatNaN(true, tSummand.getType());
      }
      return new CFloatInf(tSummand.isNegative(), tSummand.getType());
    } else if (oSummand.isInfinity()) {
      return new CFloatInf(oSummand.isNegative(), tSummand.getType());
    }

    // if one of the operands is zero, the result has the value of the other one
    if (tSummand.isZero()) {
      return new CFloatImpl(oSummand.copyWrapper(), oSummand.getType());
    }
    if (oSummand.isZero()) {
      return new CFloatImpl(tSummand.copyWrapper(), tSummand.getType());
    }

    long rExp = 0;

    // extract bit representations for operation
    long tExp = tSummand.getExponent() & tSummand.getExponentMask();
    long tMan = tSummand.getMantissa();

    long oExp = oSummand.getExponent() & tSummand.getExponentMask();
    long oMan = oSummand.getMantissa();

    boolean negResult = false;
    boolean differentSign = tSummand.isNegative() ^ oSummand.isNegative();
    boolean tGTO = tSummand.greaterThan(oSummand);

    // if the signs differ, instead of addition subtract the absolute value of the smaller operand
    // from the greater operand
    if (differentSign) {
      return tGTO
          ? tSummand.subtract(new CFloatImpl(new CFloatWrapper(oExp, oMan), tSummand.getType()))
          : oSummand.subtract(new CFloatImpl(new CFloatWrapper(tExp, tMan), tSummand.getType()));
    }

    // if this is reached, the signs of the operands are the same. if one is negative, both are and
    // so is the result
    if (tSummand.isNegative()) {
      negResult = true;
    }

    long diff;
    long overflow = 0;
    // TODO: implement normal/subnormal number distinction
    if (tSummand.getType() != CFloatNativeAPI.FP_TYPE_LONG_DOUBLE && !tSummand.isZero()) {
      tMan ^= tSummand.getNormalizationMask();
    }
    if (tSummand.getType() != CFloatNativeAPI.FP_TYPE_LONG_DOUBLE && !oSummand.isZero()) {
      oMan ^= tSummand.getNormalizationMask();
    }

    if (tExp > oExp) {
      diff = tExp - oExp;
      rExp = tExp;

      if ((diff > 2 * (long) tSummand.getMantissaLength())
          || (diff == 2 * (long) tSummand.getMantissaLength()
              && tSummand.getType() == CFloatNativeAPI.FP_TYPE_LONG_DOUBLE)) {
        oMan = 0;
      } else {

        if ((diff <= tSummand.getMantissaLength()
                && tSummand.getType() != CFloatNativeAPI.FP_TYPE_LONG_DOUBLE)
            || (diff < tSummand.getMantissaLength())) {
          if (tSummand.getType() != CFloatNativeAPI.FP_TYPE_LONG_DOUBLE) {
            overflow = oMan << (63 - diff);
          } else {
            overflow = oMan << (64 - diff);
          }
          oMan >>>= diff;

        } else {
          if (tSummand.getType() != CFloatNativeAPI.FP_TYPE_LONG_DOUBLE) {
            overflow = oMan << (63 - tSummand.getMantissaLength());
            overflow >>>= (diff - tSummand.getMantissaLength() - 1);
          } else {
            overflow = oMan << (64 - tSummand.getMantissaLength());
            overflow >>>= (diff - tSummand.getMantissaLength());
          }
          overflow &= tSummand.getOverflowHighBitsMask();

          oMan = 0;
        }
      }
    } else {
      diff = oExp - tExp;
      rExp = oExp;

      if ((diff > 2 * (long) tSummand.getMantissaLength())
          || (diff == 2 * (long) tSummand.getMantissaLength()
              && tSummand.getType() == CFloatNativeAPI.FP_TYPE_LONG_DOUBLE)) {
        tMan = 0;
      } else {

        if ((diff <= tSummand.getMantissaLength()
                && tSummand.getType() != CFloatNativeAPI.FP_TYPE_LONG_DOUBLE)
            || (diff < tSummand.getMantissaLength())) {
          if (tSummand.getType() != CFloatNativeAPI.FP_TYPE_LONG_DOUBLE) {
            overflow = tMan << (63 - diff);
          } else {
            overflow = tMan << (64 - diff);
          }
          tMan >>>= diff;
        } else {
          if (tSummand.getType() != CFloatNativeAPI.FP_TYPE_LONG_DOUBLE) {
            overflow = tMan << (63 - tSummand.getMantissaLength());
            overflow >>>= (diff - tSummand.getMantissaLength() - 1);
          } else {
            overflow = tMan << (64 - tSummand.getMantissaLength());
            overflow >>>= (diff - tSummand.getMantissaLength());
          }
          overflow &= tSummand.getOverflowHighBitsMask();

          tMan = 0;
        }
      }
    }
    long rMan = tMan + oMan;

    switch (tSummand.getType()) {
      case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
        if ((rMan & tSummand.getNormalizationMask()) == 0) {
          rMan >>>= 1;
          rMan |= tSummand.getNormalizationMask() & tSummand.getNormalizedMantissaMask();
          rExp++;
          overflow <<= 1;
        }
        break;
      case CFloatNativeAPI.FP_TYPE_DOUBLE:
      case CFloatNativeAPI.FP_TYPE_SINGLE:
        if ((rMan & (tSummand.getNormalizationMask() << 1)) != 0) {
          rMan >>>= 1;
          rExp++;
          overflow <<= 1;
        }
        break;
      default:
        throw new IllegalArgumentException(
            "Unimplemented floating-point-type: " + tSummand.getType());
    }

    rMan &= tSummand.getNormalizedMantissaMask();
    CFloatWrapper rWrapper = tSummand.round(new CFloatWrapper(rExp, rMan), overflow);

    if (((rExp & tSummand.getExponentMask()) == tSummand.getExponentMask())
        || rExp >= (tSummand.getSignBitMask() * 2)) {
      return new CFloatInf(negResult, tSummand.getType());
    }

    if (negResult) {
      rWrapper.setExponent(rWrapper.getExponent() ^ tSummand.getSignBitMask());
    }

    return new CFloatImpl(rWrapper, tSummand.getType());
  }

  @Override
  public CFloat add(CFloat... pSummands) {
    // TODO: optimize by refactoring and integration of the approach in {@link
    // CFloatImpl#add(CFloat)}
    CFloat result = this;
    for (CFloat s : pSummands) {
      result = result.add(s);
    }
    return result;
  }

  @Override
  public CFloat multiply(final CFloat pFactor) {
    CFloat tFactor = this;
    CFloat oFactor = pFactor;
    boolean negativeResult = tFactor.isNegative() ^ oFactor.isNegative();

    // cast to resulting type
    if (tFactor.getType() != oFactor.getType()) {
      if (tFactor.getType() > oFactor.getType()) {
        oFactor = oFactor.castTo(tFactor.getType());
      } else {
        tFactor = tFactor.castTo(oFactor.getType());
      }
    }

    // return NaN if either of the operands is NaN already
    if (tFactor.isNan() || oFactor.isNan()) {
      return new CFloatNaN(negativeResult, tFactor.getType());
    }

    // return infinity if either of the operands is infinite already
    if (tFactor.isInfinity() || oFactor.isInfinity()) {
      return new CFloatInf(negativeResult, tFactor.getType());
    }

    // return 0 if either of the operands is 0 already
    if (tFactor.isZero() || oFactor.isZero()) {
      if (negativeResult) {
        return new CFloatImpl("-0.0", tFactor.getType());
      } else {
        return new CFloatImpl("0", tFactor.getType());
      }
    }

    long signBit = negativeResult ? tFactor.getSignBitMask() : 0L;
    if (oFactor.isOne()) {
      CFloatWrapper rWrapper = tFactor.copyWrapper();
      rWrapper.setExponent((rWrapper.getExponent() & tFactor.getExponentMask()) ^ signBit);
      return new CFloatImpl(rWrapper, tFactor.getType());
    } else if (tFactor.isOne()) {
      CFloatWrapper rWrapper = oFactor.copyWrapper();
      rWrapper.setExponent((rWrapper.getExponent() & tFactor.getExponentMask()) ^ signBit);
      return new CFloatImpl(rWrapper, tFactor.getType());
    }

    long rMan = 0;
    long rOverflow = 0;

    long tMan = tFactor.getMantissa();
    long oMan = oFactor.getMantissa();

    long tExp = tFactor.getExponent() & tFactor.getExponentMask();
    long oExp = oFactor.getExponent() & tFactor.getExponentMask();

    // determine sign of product
    boolean negResult = tFactor.isNegative() ^ oFactor.isNegative();

    // determine multiplied mantissa and overflow value of product
    int mantissaLength = tFactor.getMantissaLength();

    if (tFactor.getType() != CFloatNativeAPI.FP_TYPE_LONG_DOUBLE) {
      mantissaLength++;
      if (tExp > 0) {
        tMan ^= tFactor.getNormalizationMask();
      }
      if (oExp > 0) {
        oMan ^= tFactor.getNormalizationMask();
      }
    }

    int[] bitfield = new int[mantissaLength * 2];

    if (Long.bitCount(tMan) > Long.bitCount(oMan)) {
      multiplyBits(tMan, oMan, mantissaLength, bitfield);
    } else {
      multiplyBits(oMan, tMan, mantissaLength, bitfield);
    }

    // correction summand for default normalization
    int cExp = (bitfield[mantissaLength * 2 - 1] == 1 ? 1 : 0);

    for (int i = cExp; i <= mantissaLength; i++) {
      if (bitfield[i + mantissaLength - 1] == 1) {
        rMan += (1L << (i - cExp));
      }
      if (bitfield[i] == 1) {
        rOverflow += (1L << ((i - cExp) + (64 - mantissaLength)));
      }
    }

    // determine exponent of product
    long rExp = tExp + oExp - tFactor.getBias() + cExp;

    // if product too small
    while (rExp < 0 && rMan != 0) {
      rOverflow >>>= 1;
      rOverflow |= (rMan << 63);
      rMan >>>= 1;
      rExp++;
    }

    // if number denormalized due to denormal factor, renormalize as far as possible
    while ((rMan & tFactor.getNormalizationMask()) == 0 && rExp > 0) {
      rMan <<= 1;
      rMan |= (rOverflow >>> 63);
      rOverflow <<= 1;
      rExp--;
    }

    rMan &= tFactor.getNormalizedMantissaMask();

    // round according to overflown bits
    CFloatWrapper rWrapper = tFactor.round(new CFloatWrapper(rExp, rMan), rOverflow);

    // set sign
    if (negResult) {
      rWrapper.setExponent(rWrapper.getExponent() ^ tFactor.getSignBitMask());
    }

    CFloat result = new CFloatImpl(rWrapper, tFactor.getType());

    // special cases
    if (result.isNan()) {
      return new CFloatNaN(result.isNegative(), result.getType());
    }

    if (result.isInfinity()) {
      return new CFloatInf(result.isNegative(), result.getType());
    }

    return result;
  }

  /**
   * Implement bit multiplication to keep track of the overflowing (underflowing respectively) bits
   * for correct rounding.
   *
   * @param manyOnesMantissa the mantissa containing more bits set to one
   * @param lesserOnesMantissa the mantissa containing less bits set to one
   * @param mantissaLength the length of the mantissas
   * @param bitfield the bitfield to store the multiplication result
   */
  private void multiplyBits(
      final long manyOnesMantissa,
      final long lesserOnesMantissa,
      final int mantissaLength,
      final int[] bitfield) {
    for (int i = 0; i < mantissaLength; i++) {
      if ((lesserOnesMantissa & (1L << i)) != 0) {
        for (int j = 0; j < mantissaLength; j++) {
          if ((manyOnesMantissa & (1L << j)) != 0) {
            bitfield[j + i] += 1;
          }
        }
      }
    }

    for (int i = 0; i < bitfield.length; i++) {
      if (bitfield[i] > 1) {
        bitfield[i + 1] += bitfield[i] / 2;
        bitfield[i] %= 2;
      }
    }
  }

  @Override
  public CFloat multiply(CFloat... pFactors) {
    // TODO: optimize by refactoring and integration of the approach in {@link
    // CFloatImpl#multiply(CFloat)}
    CFloat result = this;
    for (CFloat f : pFactors) {
      result = result.multiply(f);
    }
    return result;
  }

  @Override
  public CFloat subtract(final CFloat pSubtrahend) {
    CFloat tSubtrahend = this;
    CFloat oSubtrahend = pSubtrahend;

    if (tSubtrahend.getType() != oSubtrahend.getType()) {
      if (tSubtrahend.getType() > oSubtrahend.getType()) {
        oSubtrahend = oSubtrahend.castTo(tSubtrahend.getType());
      } else {
        tSubtrahend = tSubtrahend.castTo(oSubtrahend.getType());
      }
    }

    if (tSubtrahend.isNan() || oSubtrahend.isNan()) {
      return new CFloatNaN(tSubtrahend.isNegative(), tSubtrahend.getType());
    }

    if (tSubtrahend.isInfinity()) {
      if (oSubtrahend.isInfinity()) {
        if (tSubtrahend.isNegative() ^ oSubtrahend.isNegative()) {
          return new CFloatNaN(true, tSubtrahend.getType());
        }
      }
      return new CFloatInf(tSubtrahend.isNegative(), tSubtrahend.getType());
    } else if (oSubtrahend.isInfinity()) {
      return new CFloatInf(!oSubtrahend.isNegative(), tSubtrahend.getType());
    }

    long tExp = tSubtrahend.getExponent() & tSubtrahend.getExponentMask();
    long tMan = tSubtrahend.getMantissa();

    if (tExp != 0) {
      tMan |= tSubtrahend.getNormalizationMask();
    }

    long oExp = oSubtrahend.getExponent() & tSubtrahend.getExponentMask();
    long oMan = oSubtrahend.getMantissa();
    if (oExp != 0) {
      oMan |= tSubtrahend.getNormalizationMask();
    }

    boolean differentSign = tSubtrahend.isNegative() ^ oSubtrahend.isNegative();
    boolean oGTT = oSubtrahend.greaterThan(tSubtrahend);
    boolean negResult =
        (tSubtrahend.isNegative() && (differentSign || oGTT))
            || (!tSubtrahend.isNegative() && !(differentSign || !oGTT));

    if (differentSign) {
      if (tSubtrahend.isNegative()) {
        tExp ^= tSubtrahend.getSignBitMask();
      }
      if (!oSubtrahend.isNegative()) {
        oExp ^= tSubtrahend.getSignBitMask();
      }

      CFloat summandA = new CFloatImpl(new CFloatWrapper(tExp, tMan), tSubtrahend.getType());
      CFloat summandB = new CFloatImpl(new CFloatWrapper(oExp, oMan), tSubtrahend.getType());

      return summandA.add(summandB);
    }

    CFloatWrapper resultWrapper = null;
    if (tSubtrahend.isNegative()) {
      resultWrapper = bitwiseSubtraction(negResult, oExp, oMan, tExp, tMan, tSubtrahend);
    } else {
      resultWrapper = bitwiseSubtraction(negResult, tExp, tMan, oExp, oMan, tSubtrahend);
    }

    return new CFloatImpl(resultWrapper, tSubtrahend.getType());
  }

  private CFloatWrapper bitwiseSubtraction(
      final boolean pNegResult,
      final long pExpMinuend,
      final long pManMinuend,
      final long pExpSubtrahend,
      final long pManSubtrahend,
      final CFloat pSubtrahend) {

    long manSubtrahendComplement = (pManSubtrahend ^ -1L) + 1;

    long diff = 0;
    long overflow = 0;
    long rExp = 0;
    long rMan = 0;

    if (pExpMinuend > pExpSubtrahend) {
      diff = pExpMinuend - pExpSubtrahend;
      rExp = pExpMinuend;

      overflow = (manSubtrahendComplement << (64 - diff));

      // for single and double precision the long already pads correctly,
      // which doesn't work for extended double precision, since the mantissa
      // already fills up the whole long-bitfield
      long complementedSubtrahend = manSubtrahendComplement;
      if (pSubtrahend.getType() == CFloatNativeAPI.FP_TYPE_LONG_DOUBLE) {
        if ((complementedSubtrahend & pSubtrahend.getNormalizationMask()) == 0) {
          diff--;
          complementedSubtrahend >>= 1;
          complementedSubtrahend |= pSubtrahend.getNormalizationMask();
        }
      }
      complementedSubtrahend >>= diff;

      rMan = pManMinuend + complementedSubtrahend;
    } else {
      diff = pExpSubtrahend - pExpMinuend;
      rExp = pExpSubtrahend;

      overflow = (pManMinuend << (64 - diff));

      rMan = manSubtrahendComplement + (pManMinuend >>> diff);
    }
    if (pNegResult) {
      rMan ^= -1L;

      if (overflow != 0) {
        overflow ^= -1L;
        overflow++;
      } else {
        rMan++;
      }
    }

    while (((rMan & pSubtrahend.getNormalizationMask()) == 0)
        && rExp > 0
        && (rMan & pSubtrahend.getMantissaMask()) != 0) {
      rMan <<= 1;
      rMan += ((overflow & CFloatNativeAPI.ONE_LONG_DOUBLE.getNormalizationMask()) != 0) ? 1 : 0;
      overflow <<= 1;
      rExp--;
    }

    rMan &= pSubtrahend.getNormalizedMantissaMask();
    if (pNegResult) {
      rExp ^= pSubtrahend.getSignBitMask();
    }

    CFloatWrapper result = new CFloatWrapper(rExp, rMan);
    return pSubtrahend.round(result, overflow);
  }

  @Override
  public CFloat divideBy(final CFloat pDivisor) {
    CFloat tDividend = this;
    CFloat oDivisor = pDivisor;
    boolean negativeResult = tDividend.isNegative() ^ oDivisor.isNegative();

    if (tDividend.getType() != oDivisor.getType()) {
      if (tDividend.getType() > oDivisor.getType()) {
        oDivisor = oDivisor.castTo(tDividend.getType());
      } else {
        tDividend = tDividend.castTo(oDivisor.getType());
      }
    }

    if (tDividend.isNan() || oDivisor.isNan()) {
      return new CFloatNaN(negativeResult, tDividend.getType());
    }

    if (oDivisor.isZero()) {
      if (tDividend.isZero()) {
        // 0.0/0.0 | -0.0/0.0 | 0.0/-0.0 | -0.0/-0.0 all give -nan
        return new CFloatNaN(true, tDividend.getType());
      } else {
        return new CFloatInf(negativeResult, tDividend.getType());
      }
    }

    if (tDividend.isZero()) {
      CFloatWrapper rWrapper = tDividend.copyWrapper();
      final long signBit = negativeResult ? tDividend.getSignBitMask() : 0L;
      rWrapper.setExponent((rWrapper.getExponent() & tDividend.getExponentMask()) ^ signBit);
      return new CFloatImpl(rWrapper, tDividend.getType());
    }

    // TODO: infinities

    long tMan = tDividend.getMantissa();
    long oMan = oDivisor.getMantissa();

    long tExp = tDividend.getExponent() & tDividend.getExponentMask();
    long oExp = oDivisor.getExponent() & tDividend.getExponentMask();

    // the only possible shift in exponents is symmetrical,
    // hence insignificant for subtraction
    long bias = tDividend.getBias();
    long rExp = (tExp - oExp + bias) ^ (negativeResult ? tDividend.getSignBitMask() : 0);

    int quotientLength = tDividend.getMantissaLength() * 2;

    if (tDividend.getType() != CFloatNativeAPI.FP_TYPE_LONG_DOUBLE) {
      quotientLength += 2;

      if (tExp != 0) {
        tMan ^= tDividend.getNormalizationMask();
      }
      if (oExp != 0) {
        oMan ^= tDividend.getNormalizationMask();
      }
    }

    int[] bitArray = new int[quotientLength];

    boolean adjustExp = divideBits(tMan, oMan, quotientLength, bitArray, tDividend);

    long rMan = 0;
    long overflow = 0;

    int index = 0;
    if (bitArray[index] == 0) {
      index = 1;
    }
    assert bitArray[index] == 1;

    for (int count = quotientLength - 1; index < bitArray.length; index++, count--) {
      if (bitArray[index] == 1) {
        if (count >= quotientLength / 2) {
          rMan ^= 1L << (count - quotientLength / 2);
        } else {
          overflow ^= 1L << (64 - quotientLength / 2 + count);
        }
      }
    }

    rMan &= tDividend.getNormalizedMantissaMask();

    if (adjustExp) {
      rExp--;
    }

    CFloatWrapper rWrapper = new CFloatWrapper(rExp, rMan);
    rWrapper = tDividend.round(rWrapper, overflow);

    return new CFloatImpl(rWrapper, tDividend.getType());
  }

  /**
   * Implement bitwise division to keep track of overflowing (underflowing respectively) bits for
   * rounding.
   *
   * @param pTMan the mantissa of the {@link CFloat} that is divided
   * @param pOMan the mantissa of the {@link CFloat} with which is divided
   * @param pQuotientLength the length of the results mantissa
   * @param pBitArray the bitarray in which the division result is stored
   * @param pDividend the dividend of the division
   * @return if an offset is necessary to perform the next step of the division
   */
  private boolean divideBits(
      final long pTMan,
      final long pOMan,
      final int pQuotientLength,
      final int[] pBitArray,
      final CFloat pDividend) {
    boolean initialOffsetNeeded = false;

    int[] dividArray = new int[pQuotientLength];
    int mantissaLength = pDividend.getNormalizedMantissaLength();

    long tMan = pTMan;
    long oMan = pOMan;

    int[] divisArray = createDivisorArray(pOMan);
    int[] divisArrayComplement = makeTwoComplement(divisArray);

    // initialize bit-vectors
    for (int i = 0; i < pQuotientLength / 2; i++) {
      if (((1L << (mantissaLength - i - 1)) & tMan) != 0) {
        dividArray[i] = 1;
      }

      if ((oMan & pDividend.getMantissaMask()) != 0) {
        oMan <<= 1L;
      }
    }

    for (int offset = 0, i = 0; i < pQuotientLength; i++, offset++) {
      if (i == 0) {
        offset = divideStep(dividArray, divisArray, divisArrayComplement, offset);
        pBitArray[i] = 1;
        if (offset > 0) {
          initialOffsetNeeded = true;
        }
      } else {
        int rO = divideStep(dividArray, divisArray, divisArrayComplement, offset);
        i += rO;
        offset += rO;
        if (i < pQuotientLength) {
          pBitArray[i] = 1;
        }
      }
    }

    return initialOffsetNeeded;
  }

  private int divideStep(
      final int[] pDividend,
      final int[] pDivisor,
      final int[] pDivisorComplement,
      final int pOffset) {
    assert pDivisor.length == pDivisorComplement.length;
    assert pDivisor.length < pDividend.length;

    int count = 0;

    for (int i = 0; i < pDivisor.length; i++) {
      if (i == 0) {
        for (int j = 0; j < count + pOffset; j++) {
          if (pDividend[j] > 0) {
            offsetBitAddition(pDividend, pDivisorComplement, count + pOffset);
            return count;
          }
        }
      }

      if (i + count + pOffset >= pDividend.length
          || pDividend[i + count + pOffset] > pDivisor[i]
          || i == (pDivisor.length - 1)) {
        offsetBitAddition(pDividend, pDivisorComplement, count + pOffset);
        // conditional loop end
        break;
      }
      if (pDividend[i + count + pOffset] < pDivisor[i]) {
        i = -1;
        count++;
      }
    }

    return count;
  }

  private void offsetBitAddition(
      final int[] pDividend, final int[] pDivisorComplement, final int pOffset) {
    if (pOffset >= pDividend.length) {
      // bits outside our precision
      return;
    }

    int offset = pOffset;
    int ignoreBits = 0;
    if (offset > pDivisorComplement.length) {
      ignoreBits = offset - pDivisorComplement.length;
      offset -= ignoreBits;
    }

    int carry = 0;
    // overflow is ignored
    for (int i = pDivisorComplement.length - 1; i >= (-1 * offset); i--) {
      pDividend[i + offset] +=
          (i >= ignoreBits)
              ? carry + pDivisorComplement[i - ignoreBits]
              : carry + 1; // consider the two-complement to have padding 1s to the left
      carry = pDividend[i + offset] / 2;
      pDividend[i + offset] %= 2;
    }
  }

  private int[] makeTwoComplement(final int[] pDivisArray) {
    int[] complement = new int[pDivisArray.length];

    for (int i = 0; i < pDivisArray.length; i++) {
      complement[i] = (pDivisArray[i] + 1) % 2;
    }
    int carry = 1;
    for (int i = complement.length - 1; i >= 0; i--) {
      complement[i] = (complement[i] + carry) % 2;

      // first time carry becomes 0 we are done
      if (complement[i] == 1) {
        break;
      }
    }

    return complement;
  }

  private int[] createDivisorArray(final long pOMan) {
    int size = 0;

    for (int i = 0; i < 64; i++) {
      if (((1L << i) & pOMan) != 0) {
        size = i + 1;
      }
    }

    assert size > 0;

    int[] bitArray = new int[size];

    int count = 0;
    boolean started = false;
    for (int i = 0; i < 64; i++) {
      if ((pOMan & (1L << (63 - i))) != 0) {
        bitArray[count] = 1;
        started = true;
      }
      if (started) {
        count++;
      }
    }

    return bitArray;
  }

  @Override
  public CFloat powTo(final CFloat pExponent) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public CFloat powToIntegral(final int pExponent) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public CFloat sqrt() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public CFloat round() {
    long exp = wrapper.getExponent() & getExponentMask();
    long man = wrapper.getMantissa();
    long sign = isNegative() ? getSignBitMask() : 0L;
    int diff = (int) (exp - getBias());

    if (diff >= 0 && diff < getMantissaLength()) {
      long intMask = 0L;
      for (int i = (getMantissaLength() - diff); i < getMantissaLength(); i++) {
        intMask ^= (1L << i);
      }
      long fracMask = ~intMask;
      long fractionalPart = man & fracMask;

      man &= intMask;

      long carryMask;
      if ((carryMask = ((intMask >> 1) & fractionalPart)) != 0) {
        man += (carryMask << 1);
        if ((man & intMask) == 0) {
          exp++;
        }
      }
    } else if (diff < 0) {
      if (diff == -1) {
        // the number is between 0.5 and 1.0,
        // so the mantissa becomes 0 and the exponent is incremented
        // resulting in the floating point number 1.0 (or -1.0, respectively)
        exp++;
        man = 0L;
      } else {
        exp = 0L;
        man = 0L;
      }
    }
    // if none of the above conditions is matched, the number is completely integral already and
    // nothing has to be done

    exp &= getExponentMask();
    man &= getNormalizedMantissaMask();
    return new CFloatImpl(new CFloatWrapper(exp ^ sign, man), type);
  }

  @Override
  public CFloat trunc() {
    long exp = wrapper.getExponent() & getExponentMask();
    long man = wrapper.getMantissa();
    long sign = isNegative() ? getSignBitMask() : 0L;
    int diff = (int) (exp - getBias());

    if (diff >= 0 && diff < getMantissaLength()) {
      long intMask = 0L;
      for (int i = (getMantissaLength() - diff); i < getMantissaLength(); i++) {
        intMask ^= (1L << i);
      }

      // trunc just drops the fractional part
      man &= intMask;
    } else if (diff < 0) {
      // the number is completely fractional and smaller than 1, hence 0 is returned
      exp = 0L;
      man = 0L;
    }
    // if none of the above conditions is matched, the number is completely integral already and
    // nothing has to be done

    return new CFloatImpl(new CFloatWrapper(exp ^ sign, man), type);
  }

  @Override
  public CFloat ceil() {
    CFloat res = trunc();
    if (greaterThan(res)) {
      res.add(new CFloatImpl("1", type));
    }
    return res;
  }

  @Override
  public CFloat floor() {
    CFloat res = trunc();
    if (res.greaterThan(this)) {
      res.add(new CFloatImpl("-1", type));
    }
    return res;
  }

  @Override
  public CFloat abs() {
    CFloatWrapper wrap = copyWrapper();
    wrap.setExponent(wrap.getExponent() & getExponentMask());

    return new CFloatImpl(wrap, type);
  }

  @Override
  public boolean isZero() {
    boolean mantissaZero = (getNormalizedMantissaMask() & wrapper.getMantissa()) == 0;
    boolean exponentZero = (wrapper.getExponent() & getExponentMask()) == 0;

    return mantissaZero && (exponentZero || type == CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);
  }

  @Override
  public boolean isOne() {
    return ((getBias() ^ wrapper.getExponent()) == 0)
        && ((wrapper.getMantissa() ^ getNormalizedMantissaMask()) == getMantissaMask());
  }

  @Override
  public boolean isNegative() {
    return (wrapper.getExponent() & getSignBitMask()) != 0;
  }

  @Override
  public CFloat copySignFrom(final CFloat pSource) {
    CFloatWrapper wrap = copyWrapper();
    long tExp = wrap.getExponent();
    long oExp = pSource.getExponent();

    long tMask = getSignBitMask();
    long oMask = pSource.getSignBitMask();

    if ((oExp & oMask) == 0) {
      if ((tExp & tMask) != 0) {
        tExp ^= tMask;
      }
    } else {
      if ((tExp & tMask) == 0) {
        tExp ^= tMask;
      }
    }

    wrap.setExponent(tExp);

    return new CFloatImpl(wrap, type);
  }

  @Override
  public CFloat castTo(final int pToType) {
    CFloat zero = new CFloatImpl("0", pToType);
    if (isZero()) {
      if (isNegative()) {
        return new CFloatImpl("-0", pToType);
      } else {
        return zero;
      }
    }

    long rExp = wrapper.getExponent();
    long rMan = wrapper.getMantissa();
    long overflow = 0L;

    boolean signed = (rExp & getSignBitMask()) != 0;
    long expDiff = (rExp & getExponentMask()) - getBias();
    int manDiff = getMantissaLength() - zero.getMantissaLength();
    manDiff *= manDiff < 0 ? -1 : 1;

    if (type == CFloatNativeAPI.FP_TYPE_LONG_DOUBLE
        && pToType != CFloatNativeAPI.FP_TYPE_LONG_DOUBLE) {
      rMan &= getMantissaMask();
      manDiff--;
    } else if (type != CFloatNativeAPI.FP_TYPE_LONG_DOUBLE
        && pToType == CFloatNativeAPI.FP_TYPE_LONG_DOUBLE) {
      rMan |= getNormalizationMask();
      manDiff--;
    }

    rExp = zero.getBias() + expDiff;

    if (signed) {
      rExp ^= zero.getSignBitMask();
    }

    if (type > pToType) {
      long overflowMask = (1L << manDiff) - 1;
      overflow = (rMan & overflowMask) << (64 - manDiff);
      rMan >>>= manDiff;
    } else {
      rMan <<= manDiff;
    }

    CFloatWrapper rWrapper = new CFloatWrapper(rExp, rMan);
    rWrapper = zero.round(rWrapper, overflow);

    return new CFloatImpl(rWrapper, pToType);
  }

  @Override
  public Number castToOther(final int pToType) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public CFloatWrapper copyWrapper() {
    return wrapper.copy();
  }

  @Override
  public int getType() {
    return type;
  }

  @Override
  public boolean greaterThan(final CFloat pFloat) {
    if (pFloat.isNan() || (pFloat.isInfinity() && !pFloat.isNegative())) {
      return false;
    }

    CFloat oFloat = pFloat;
    CFloat tFloat = this;

    if (oFloat.getType() > tFloat.getType()) {
      tFloat = tFloat.castTo(oFloat.getType());
    } else if (tFloat.getType() > oFloat.getType()) {
      oFloat = oFloat.castTo(tFloat.getType());
    }

    int oType = oFloat.getType();

    boolean greater = false;

    boolean tNeg = tFloat.isNegative();
    boolean oNeg = oFloat.isNegative();

    long tExp = tFloat.getExponent();
    long tMan = tFloat.getMantissa();

    long oExp = oFloat.getExponent();
    long oMan = oFloat.getMantissa();

    if (oNeg) {
      if (tNeg) {
        if (tExp < oExp) {
          greater = true;
        } else if (tExp == oExp) {
          if (oType == CFloatNativeAPI.FP_TYPE_LONG_DOUBLE) {
            if (((tMan & tFloat.getNormalizationMask()) == 0)
                && ((oMan & tFloat.getNormalizationMask()) != 0)) {
              greater = true;
            } else if ((((tMan & tFloat.getNormalizationMask()) != 0)
                    && ((oMan & tFloat.getNormalizationMask()) != 0))
                || (((tMan & tFloat.getNormalizationMask()) == 0)
                    && ((oMan & tFloat.getNormalizationMask()) == 0))) {
              if ((tMan & tFloat.getMantissaMask()) < (oMan & tFloat.getMantissaMask())) {
                greater = true;
              }
            }
          } else if (tMan < oMan) {
            greater = true;
          }
        }
      } else {
        greater = true;
      }
    } else {
      if (!tNeg) {
        if (tExp > oExp) {
          greater = true;
        } else if (tExp == oExp) {
          if (oType == CFloatNativeAPI.FP_TYPE_LONG_DOUBLE) {
            if (((tMan & tFloat.getNormalizationMask()) != 0)
                && ((oMan & tFloat.getNormalizationMask()) == 0)) {
              greater = true;
            } else if ((((tMan & tFloat.getNormalizationMask()) != 0)
                    && ((oMan & tFloat.getNormalizationMask()) != 0))
                || (((tMan & tFloat.getNormalizationMask()) == 0)
                    && ((oMan & tFloat.getNormalizationMask()) == 0))) {
              if ((tMan & tFloat.getMantissaMask()) > (oMan & tFloat.getMantissaMask())) {
                greater = true;
              }
            }
          } else if (tMan > oMan) {
            greater = true;
          }
        }
      }
    }

    return greater;
  }

  @Override
  public String toString() {
    long exp = wrapper.getExponent();
    long man = wrapper.getMantissa();
    StringBuilder builder = new StringBuilder();

    if ((exp & getSignBitMask()) != 0) {
      builder.append("-");
    }

    int[] decArray = getDecimalArray((exp & getExponentMask()), man);
    boolean started = false;
    for (int i : decArray) {
      if (i < 0) {
        if (!started) {
          builder.append("0");
          started = true;
        }
        builder.append(".");
      } else if (started || i > 0) {
        builder.append(i);
        started = true;
      }
    }

    return builder.toString().replaceAll("(\\.[0-9]+?)0*$", "$1");
  }

  private int[] getDecimalArray(final long pExp, final long pMan) {

    int[] fracArray = CFloatUtil.getDecimalArray(type, pMan & 1);
    for (int i = 1; i < getMantissaLength(); i++) {
      fracArray = decimalAdd(fracArray, CFloatUtil.getDecimalArray(type, pMan & (1L << i)));
    }

    int[] integralArray = new int[1];
    if ((type != CFloatNativeAPI.FP_TYPE_LONG_DOUBLE && pExp != 0)
        || (type == CFloatNativeAPI.FP_TYPE_LONG_DOUBLE
            && ((pMan & getNormalizationMask()) != 0))) {
      integralArray[0] = 1;
    }

    long exp = pExp - getBias();
    if (exp > 0) {
      int startLength = fracArray.length;
      for (int i = 0; i < exp; i++) {
        integralArray = decimalDouble(integralArray);
        fracArray = decimalDouble(fracArray);
        if (fracArray.length > startLength) {
          int iAL = integralArray.length;
          integralArray[iAL - 1] += fracArray[0];
          fracArray = copyAllButFirstCell(fracArray);
          if (integralArray[iAL - 1] > 9) {
            integralArray[iAL - 2] = (integralArray[iAL - 1] / 10);
            integralArray[iAL - 1] = (integralArray[iAL - 1] % 10);
          }
        }
      }
    } else {
      for (int i = 0; i < -exp; i++) {
        int last = fracArray[fracArray.length - 1];
        decimalHalf(fracArray);
        if (last % 2 != 0) {
          fracArray = Arrays.copyOf(fracArray, fracArray.length + 1);
          fracArray[fracArray.length - 1] = 5;
        }
        assert integralArray.length == 1 && integralArray[0] <= 1
            : "Exponent <= 0, but integral of mantissa larger than 1 - shouldn't be possible in"
                + " IEEE 754";
        if (integralArray[0] == 1) {
          integralArray = copyAllButFirstCell(integralArray);
          int[] oneHalf = new int[fracArray.length];
          oneHalf[0] = 5;
          int startLength = fracArray.length;
          fracArray = decimalAdd(fracArray, oneHalf);
          assert startLength == fracArray.length
              : "overflow on (frac * 0.5 + 0.5) - shouldn't happen.";
        }
      }
    }
    int[] result = new int[integralArray.length + fracArray.length + 1];
    for (int i = 0; i < result.length; i++) {
      if (i < integralArray.length) {
        result[i] = integralArray[i];
      } else if (i == integralArray.length) {
        result[i] = -1;
      } else {
        result[i] = fracArray[i - integralArray.length - 1];
      }
    }

    return result;
  }

  private int[] copyAllButFirstCell(final int[] pArray) {
    return Arrays.copyOfRange(pArray, 1, pArray.length);
  }

  @Override
  protected CFloatWrapper getWrapper() {
    return wrapper;
  }
}
