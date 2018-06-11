/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.floatingpoint;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class CFloatImpl implements CFloat {

  private static final ImmutableList<String> DEFAULT_VALUES =
      ImmutableList
          .copyOf(
              new String[] {"-1", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "nan",
                  "-nan", "inf", "-inf"});
  private CFloatWrapper wrapper;
  private int type;

  public CFloatImpl(CFloatWrapper pWrapper, int pType) {
    this.wrapper = pWrapper;
    this.type = pType;
  }

  public CFloatImpl(String pRep, int pType) {
    this.type = pType;
    this.wrapper = new CFloatWrapper();

    if (DEFAULT_VALUES.contains(pRep.toLowerCase())) {
      long exp = 0;
      long man = 0;

      switch (pRep.toLowerCase()) {
        case "0":
          break;
        case "1":
          exp = CFloatUtil.getBias(pType);
          man =
              CFloatUtil.getNormalizationMask(pType) & CFloatUtil.getNormalizedMantissaMask(pType);
          break;
        case "2":
          exp = CFloatUtil.getBias(pType) + 1L;
          man =
              CFloatUtil.getNormalizationMask(pType) & CFloatUtil.getNormalizedMantissaMask(pType);
          break;
        case "3":
          exp = CFloatUtil.getBias(pType) + 1L;
          man =
              ((CFloatUtil.getNormalizationMask(pType) >>> 1)
                  + CFloatUtil.getNormalizationMask(pType))
                  & CFloatUtil.getNormalizedMantissaMask(pType);
          break;
        case "4":
          exp = CFloatUtil.getBias(pType) + 2L;
          man =
              CFloatUtil.getNormalizationMask(pType) & CFloatUtil.getNormalizedMantissaMask(pType);
          break;
        case "5":
          exp = CFloatUtil.getBias(pType) + 2L;
          man =
              ((CFloatUtil.getNormalizationMask(pType) >>> 2)
                  + CFloatUtil.getNormalizationMask(pType))
                  & CFloatUtil.getNormalizedMantissaMask(pType);
          break;
        case "6":
          exp = CFloatUtil.getBias(pType) + 2L;
          man =
              ((CFloatUtil.getNormalizationMask(pType) >>> 1)
                  + CFloatUtil.getNormalizationMask(pType))
                  & CFloatUtil.getNormalizedMantissaMask(pType);
          break;
        case "7":
          exp = CFloatUtil.getBias(pType) + 2L;
          man =
              ((CFloatUtil.getNormalizationMask(pType) >>> 2)
                  + (CFloatUtil.getNormalizationMask(pType) >>> 1)
                      + CFloatUtil.getNormalizationMask(pType))
                  & CFloatUtil.getNormalizedMantissaMask(pType);
          break;
        case "8":
          exp = CFloatUtil.getBias(pType) + 3L;
          man =
              CFloatUtil.getNormalizationMask(pType) & CFloatUtil.getNormalizedMantissaMask(pType);
          break;
        case "9":
          exp = CFloatUtil.getBias(pType) + 3L;
          man =
              ((CFloatUtil.getNormalizationMask(pType) >>> 3)
                  + CFloatUtil.getNormalizationMask(pType))
                  & CFloatUtil.getNormalizedMantissaMask(pType);
          break;
        case "10":
          exp = CFloatUtil.getBias(pType) + 3L;
          man =
              ((CFloatUtil.getNormalizationMask(pType) >>> 2)
                  + CFloatUtil.getNormalizationMask(pType))
                  & CFloatUtil.getNormalizedMantissaMask(pType);
          break;
        case "-1":
          exp = CFloatUtil.getSignBitMask(pType) + (CFloatUtil.getBias(pType));
          man =
              (pType == CFloatNativeAPI.FP_TYPE_LONG_DOUBLE
                  ? CFloatUtil.getNormalizationMask(pType)
                  : 0L);
          break;
        case "nan":
          exp = 1L;
          man = 0L;
          break;
        case "-nan":
          exp = CFloatUtil.getSignBitMask(pType) + 1L;
          man = 0L;
          break;
        case "inf":
          exp = CFloatUtil.getExponentMask(pType);
          man = 1L;
          break;
        case "-inf":
          exp = CFloatUtil.getExponentMask(pType) + CFloatUtil.getSignBitMask(pType);
          man = 1L;
          break;
        default:
          throw new RuntimeException("Default case '" + pRep + "' is not yet implemented!");
      }

      this.wrapper.setExponent(exp);
      this.wrapper.setMantissa(man);
    } else {
      List<String> parts = Splitter.on('.').splitToList(pRep);

      CFloat integral = null;
      CFloat fractional = null;

      if (!parts.get(0).equals("")) {
        integral = makeIntegralPart(parts.get(0), pType);
      }
      if (parts.size() > 1 && !parts.get(1).equals("")) {
        fractional = makeFractionalPart(parts.get(1), pType);
      }

      CFloat result = new CFloatImpl("0", pType);

      if (integral != null) {
        result = result.add(integral);
      }
      if (fractional != null) {
        result = result.add(fractional);
      }

      this.wrapper = result.copyWrapper();
    }
  }

  private CFloat makeIntegralPart(String pRep, int pType) {
    CFloat nOne = new CFloatImpl("-1", pType);
    boolean negative = false;

    String rep = null;

    if (pRep.startsWith("-")) {
      rep = pRep.substring(1);
      negative = true;
    } else {
      rep = pRep;
    }

    List<String> digits = Splitter.on("").splitToList(rep);
    CFloat ten = new CFloatImpl("10", pType);

    CFloat result = new CFloatImpl("0", pType);
    for (String d : digits) {
      result = result.multiply(ten);
      CFloat fD = new CFloatImpl(d, pType);
      result = result.add(fD);
    }

    if (negative) {
      result = result.multiply(nOne);
    }

    return result;
  }

  private CFloat makeFractionalPart(String pRep, int pType) {
    List<String> digits = Splitter.on("").splitToList(pRep);
    CFloat ten = new CFloatImpl("10", pType);

    CFloat result = new CFloatImpl("0", pType);
    for (int i = digits.size() - 1; i > -1; i--) {
      CFloat fD = new CFloatImpl(digits.get(i), pType);
      result = result.add(fD);
      result = result.divideBy(ten);
    }

    return result;
  }

  @Override
  public CFloat add(CFloat pSummand) {
    CFloat tSummand = this;
    CFloat oSummand = pSummand;

    if (tSummand.getType() != oSummand.getType()) {
      if (tSummand.getType() > oSummand.getType()) {
        oSummand = oSummand.castTo(tSummand.getType());
      } else {
        tSummand = tSummand.castTo(oSummand.getType());
      }
    }

    if (tSummand.isNan() || oSummand.isNan()) {
      // TODO: determine smoothly the sign of the nan
      return new CFloatNaN(tSummand.getType());
    }

    if (tSummand.isInfinity()) {
      if (oSummand.isInfinity() && (tSummand.isNegative() != oSummand.isNegative())) {
        // TODO: determine smoothly the sign of the nan
        return new CFloatNaN(tSummand.getType());
      }
      return new CFloatInf(tSummand.isNegative(), tSummand.getType());
    } else if (oSummand.isInfinity()) {
      return new CFloatInf(oSummand.isNegative(), tSummand.getType());
    }

    long rExp = 0;
    long rMan = 0;

    long tExp =
        tSummand.copyWrapper().getExponent()
            & CFloatUtil.getExponentMask(tSummand.getType());
    long tMan = tSummand.copyWrapper().getMantissa();

    long oExp =
        oSummand.copyWrapper().getExponent()
            & CFloatUtil.getExponentMask(tSummand.getType());
    long oMan = oSummand.copyWrapper().getMantissa();

    boolean differentSign = tSummand.isNegative() ^ oSummand.isNegative();
    boolean tGTO = false;
    long diff;

    if (tExp > oExp) {
      diff = tExp - oExp;
      rExp = tExp;
      tGTO = true;

      if (diff >= CFloatUtil.getMantissaLength(tSummand.getType())) {
        oMan = 0;
      } else {
        if (tSummand.getType() != CFloatNativeAPI.FP_TYPE_LONG_DOUBLE) {
          oMan ^= CFloatUtil.getNormalizationMask(tSummand.getType());
        }
        oMan >>>= diff;
      }
    } else {
      diff = oExp - tExp;
      rExp = oExp;

      if (diff >= CFloatUtil.getMantissaLength(tSummand.getType())) {
        tMan = 0;
      } else if (diff > 0) {
        if (tSummand.getType() != CFloatNativeAPI.FP_TYPE_LONG_DOUBLE) {
          oMan ^= CFloatUtil.getNormalizationMask(tSummand.getType());
        }
        tMan >>>= diff;
      }
    }
    rMan = ((!tGTO && differentSign) ? -1 : 1) * tMan + ((tGTO && differentSign) ? -1 : 1) * oMan;

    if (differentSign) {
      while ((rMan
          & CFloatUtil.getNormalizationMask(tSummand.getType())) == 0
          && (rMan
              & CFloatUtil.getMantissaMask(tSummand.getType())) != 0
          && rExp > 0) {
        rMan <<= 1;
        rExp--;
      }
      if (tSummand.getType() != CFloatNativeAPI.FP_TYPE_LONG_DOUBLE) {
        rMan &= CFloatUtil.getMantissaMask(tSummand.getType());
      }
      if (rMan == 0) {
        rExp &= CFloatUtil.getSignBitMask(tSummand.getType());
      }
    } else {
      switch (tSummand.getType()) {
        case CFloatNativeAPI.FP_TYPE_LONG_DOUBLE:
          if ((rMan & CFloatUtil.getNormalizationMask(tSummand.getType())) == 0) {
            rMan >>>= 1;
            rMan |=
                CFloatUtil.getNormalizationMask(tSummand.getType())
                    & CFloatUtil.getNormalizedMantissaMask(tSummand.getType());
            rExp++;
          }
          break;
        case CFloatNativeAPI.FP_TYPE_DOUBLE:
        case CFloatNativeAPI.FP_TYPE_SINGLE:
          if (diff == 0 || (rMan & CFloatUtil.getNormalizationMask(tSummand.getType())) != 0) {
            rMan &= CFloatUtil.getMantissaMask(tSummand.getType());
            rMan >>>= 1;
            rExp++;
          }
          break;
        default:
          throw new IllegalArgumentException(
              "Unimplemented floating-point-type: " + tSummand.getType());
      }
    }

    if (((rExp
        & CFloatUtil.getExponentMask(tSummand.getType())) == CFloatUtil
            .getExponentMask(tSummand.getType()))
        || rExp >= (CFloatUtil.getSignBitMask(tSummand.getType()) * 2)) {
      return new CFloatInf(tSummand.getType());
    }

    return new CFloatImpl(new CFloatWrapper(rExp, rMan), tSummand.getType());
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
  public CFloat multiply(CFloat pFactor) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public CFloat multiply(CFloat... pFactors) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public CFloat subtract(CFloat pSubtrahend) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public CFloat divideBy(CFloat pDivisor) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public CFloat powTo(CFloat pExponent) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public CFloat powToIntegral(int pExponent) {
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
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public CFloat trunc() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public CFloat ceil() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public CFloat floor() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public CFloat abs() {
    CFloatWrapper wrap = copyWrapper();
    wrap.setExponent(wrap.getExponent() & CFloatUtil.getExponentMask(type));

    return new CFloatImpl(wrap, type);
  }

  @Override
  public boolean isZero() {
    boolean mantissaZero =
        (CFloatUtil.getNormalizedMantissaMask(type) & wrapper.getMantissa()) == 0;
    // somehow -0.0 results in "non-zero" when asking in C...
    boolean exponentZero = wrapper.getExponent() == 0;

    return mantissaZero && exponentZero;
  }

  @Override
  public boolean isOne() {
    return ((CFloatUtil.getBias(type) ^ wrapper.getExponent()) == 0)
        && ((wrapper.getMantissa() ^ CFloatUtil.getNormalizedMantissaMask(type)) == CFloatUtil
            .getMantissaMask(type));
  }

  @Override
  public boolean isNegative() {
    return (wrapper.getExponent() & CFloatUtil.getSignBitMask(type)) != 0;
  }

  @Override
  public CFloat copySignFrom(CFloat pSource) {
    CFloatWrapper wrap =
        new CFloatWrapper(this.wrapper.getExponent(), this.wrapper.getMantissa());
    long tExp = wrap.getExponent();
    long oExp = pSource.copyWrapper().getExponent();

    long tMask = CFloatUtil.getSignBitMask(type);
    long oMask = CFloatUtil.getSignBitMask(pSource.getType());

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
  public CFloat castTo(int pToType) {
    long rExp = wrapper.getExponent();
    long rMan = wrapper.getMantissa();

    boolean signed = (rExp & CFloatUtil.getSignBitMask(type)) != 0;
    long expDiff = (rExp & CFloatUtil.getExponentMask(type)) - CFloatUtil.getBias(type);
    expDiff *= expDiff < 0 ? -1 : 1;
    int manDiff = CFloatUtil.getMantissaLength(type) - CFloatUtil.getMantissaLength(pToType);
    manDiff *= manDiff < 0 ? -1 : 1;

    if (type == CFloatNativeAPI.FP_TYPE_LONG_DOUBLE
        && pToType != CFloatNativeAPI.FP_TYPE_LONG_DOUBLE) {
      rMan &= CFloatUtil.getMantissaMask(type);
      manDiff--;
    } else if (type != CFloatNativeAPI.FP_TYPE_LONG_DOUBLE
        && pToType == CFloatNativeAPI.FP_TYPE_LONG_DOUBLE) {
      rMan |= CFloatUtil.getNormalizationMask(type);
      manDiff--;
    }

    rExp = CFloatUtil.getBias(pToType) + expDiff;

    if (signed) {
      rExp ^= CFloatUtil.getSignBitMask(pToType);
    }

    if (type > pToType) {
      rMan >>>= manDiff;
    } else {
      rMan <<= manDiff;
    }

    return new CFloatImpl(new CFloatWrapper(rExp, rMan), pToType);
  }

  @Override
  public Number castToOther(int pToType) {
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
}
