// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CFloatType;

public class CFloatTest {

  /**
   * Following are test cases as provided by a x86_64 amd architecture with a 64-bit linux (ubuntu),
   * compiled using gcc in c11 compliance.
   */
  @Test
  public void infTest() {
    CFloat f_n1 = new CFloatNative("-1", CFloatNativeAPI.CFloatType.SINGLE);
    CFloat d_n0 = new CFloatNative("-0.0", CFloatNativeAPI.CFloatType.DOUBLE);

    CFloat f_1 =
        new CFloatNative(
            CFloatNativeAPI.ONE_SINGLE.copyWrapper(), CFloatNativeAPI.CFloatType.SINGLE);
    CFloat d_1 =
        new CFloatNative(
            CFloatNativeAPI.ONE_DOUBLE.copyWrapper(), CFloatNativeAPI.CFloatType.DOUBLE);
    CFloat ld_1 =
        new CFloatNative(
            CFloatNativeAPI.ONE_LONG_DOUBLE.copyWrapper(), CFloatNativeAPI.CFloatType.LONG_DOUBLE);

    CFloat cf_f = new CFloatNative("0.0", CFloatNativeAPI.CFloatType.SINGLE);
    CFloat cf_d = new CFloatNative("0.0", CFloatNativeAPI.CFloatType.DOUBLE);
    CFloat cf_ld = new CFloatNative("0.0", CFloatNativeAPI.CFloatType.LONG_DOUBLE);

    assertThat(f_1.divideBy(cf_f).toString()).isEqualTo("inf");
    assertThat(f_1.divideBy(cf_d).toString()).isEqualTo("inf");
    assertThat(f_1.divideBy(cf_ld).toString()).isEqualTo("inf");

    assertThat(d_1.divideBy(cf_f).toString()).isEqualTo("inf");
    assertThat(d_1.divideBy(cf_d).toString()).isEqualTo("inf");
    assertThat(d_1.divideBy(cf_ld).toString()).isEqualTo("inf");

    assertThat(ld_1.divideBy(cf_f).toString()).isEqualTo("inf");
    assertThat(ld_1.divideBy(cf_d).toString()).isEqualTo("inf");
    assertThat(ld_1.divideBy(cf_ld).toString()).isEqualTo("inf");

    assertThat(f_n1.divideBy(cf_f).toString()).isEqualTo("-inf");
    assertThat(f_n1.divideBy(cf_d).toString()).isEqualTo("-inf");
    assertThat(f_n1.divideBy(cf_ld).toString()).isEqualTo("-inf");

    assertThat(f_1.divideBy(d_n0).toString()).isEqualTo("-inf");
    assertThat(d_1.divideBy(d_n0).toString()).isEqualTo("-inf");
    assertThat(ld_1.divideBy(d_n0).toString()).isEqualTo("-inf");

    CFloat inf_f = f_1.divideBy(cf_f);
    CFloat inf_nf = f_n1.divideBy(cf_f);
    assertThat(inf_f.add(inf_f).toString()).isEqualTo("inf");
    assertThat(inf_nf.add(inf_nf).toString()).isEqualTo("-inf");
    assertThat(inf_f.subtract(inf_nf).toString()).isEqualTo("inf");
    assertThat(inf_nf.subtract(inf_f).toString()).isEqualTo("-inf");
  }

  @Test
  public void nanTest() {
    CFloat cf_f = new CFloatNative("0.0", CFloatNativeAPI.CFloatType.SINGLE);
    CFloat cf_d = new CFloatNative("0.0", CFloatNativeAPI.CFloatType.DOUBLE);
    CFloat cf_ld = new CFloatNative("0.0", CFloatNativeAPI.CFloatType.LONG_DOUBLE);

    assertThat(cf_f.divideBy(cf_f).toString()).isEqualTo("-nan");
    assertThat(cf_f.divideBy(cf_d).toString()).isEqualTo("-nan");
    assertThat(cf_f.divideBy(cf_ld).toString()).isEqualTo("-nan");

    assertThat(cf_d.divideBy(cf_f).toString()).isEqualTo("-nan");
    assertThat(cf_d.divideBy(cf_d).toString()).isEqualTo("-nan");
    assertThat(cf_d.divideBy(cf_ld).toString()).isEqualTo("-nan");

    assertThat(cf_ld.divideBy(cf_f).toString()).isEqualTo("-nan");
    assertThat(cf_ld.divideBy(cf_d).toString()).isEqualTo("-nan");
    assertThat(cf_ld.divideBy(cf_ld).toString()).isEqualTo("-nan");

    CFloat f_1 =
        new CFloatNative(
            CFloatNativeAPI.ONE_SINGLE.copyWrapper(), CFloatNativeAPI.CFloatType.SINGLE);
    CFloat f_n1 = new CFloatNative("-1", CFloatNativeAPI.CFloatType.SINGLE);
    CFloat inf_f = f_1.divideBy(cf_f);
    CFloat inf_nf = f_n1.divideBy(cf_f);

    assertThat(inf_f.subtract(inf_f).toString()).isEqualTo("-nan");
    assertThat(inf_nf.subtract(inf_nf).toString()).isEqualTo("-nan");
    assertThat(inf_f.add(inf_nf).toString()).isEqualTo("-nan");
    assertThat(inf_nf.add(inf_f).toString()).isEqualTo("-nan");

    cf_f = cf_f.divideBy(cf_f);
    assertThat(cf_f.add(f_1).toString()).isEqualTo("-nan");
  }

  @Test
  public void formatTest() {
    CFloat cf_f = new CFloatNative("71236.262625", CFloatNativeAPI.CFloatType.SINGLE);
    CFloat cf_d =
        new CFloatNative("7891274812.82489681243896484375", CFloatNativeAPI.CFloatType.DOUBLE);
    CFloat cf_ld =
        new CFloatNative("82173928379128.897125244140625", CFloatNativeAPI.CFloatType.LONG_DOUBLE);
    CFloat cf_f2 = new CFloatNative("10.0", CFloatNativeAPI.CFloatType.SINGLE);

    assertThat(cf_f.toString()).isEqualTo("7.12362656e+04");
    assertThat(cf_f2.toString()).isEqualTo("1.00000000e+01");
    assertThat(cf_d.toString()).isEqualTo("7.8912748128248968e+09");
    assertThat(cf_ld.toString()).isEqualTo("8.21739283791288971252e+13");

    CFloatNative two = new CFloatNative("2.0", CFloatNativeAPI.CFloatType.SINGLE);
    cf_ld = cf_ld.multiply(two);

    assertThat(cf_ld.toString()).isEqualTo("1.64347856758257794250e+14");

    for (int i = 0; i < 10; i++) {
      cf_ld = cf_ld.multiply(two);
    }
    assertThat(cf_ld.toString()).isEqualTo("1.68292205320455981312e+17");

    for (int i = 0; i < 10; i++) {
      cf_ld = cf_ld.multiply(two);
    }
    assertThat(cf_ld.toString()).isEqualTo("1.72331218248146924864e+20");

    for (int i = 0; i < 10; i++) {
      cf_ld = cf_ld.multiply(two);
    }
    assertThat(cf_ld.toString()).isEqualTo("1.76467167486102451061e+23");

    for (int i = 0; i < 800; i++) {
      cf_ld = cf_ld.multiply(two);
    }
    assertThat(cf_ld.toString()).isEqualTo("1.17668561972675769465e+264");

    for (int i = 0; i < 15506; i++) {
      cf_ld = cf_ld.multiply(two);
    }
    assertThat(cf_ld.toString()).isEqualTo("6.94661471029285501213e+4931");

    cf_ld = cf_ld.multiply(two);
    assertThat(cf_ld.toString()).isEqualTo("inf");
  }

  @Test
  public void zeroTest() {
    CFloatNative zero = new CFloatNative("0.0", CFloatNativeAPI.CFloatType.DOUBLE);
    CFloatNative nZero = new CFloatNative("-0.0", CFloatNativeAPI.CFloatType.DOUBLE);
    CFloatNative nOne = new CFloatNative("-1.0", CFloatNativeAPI.CFloatType.DOUBLE);

    assertThat(zero.add(nZero).toString()).isEqualTo("0.0000000000000000e+00");
    assertThat(nZero.add(zero).toString()).isEqualTo("0.0000000000000000e+00");
    assertThat(nZero.add(nZero).toString()).isEqualTo("-0.0000000000000000e+00");
    assertThat(nZero.subtract(zero).toString()).isEqualTo("-0.0000000000000000e+00");
    assertThat(zero.subtract(nZero).toString()).isEqualTo("0.0000000000000000e+00");
    assertThat(nZero.subtract(nZero).toString()).isEqualTo("0.0000000000000000e+00");
    assertThat(zero.subtract(zero).toString()).isEqualTo("0.0000000000000000e+00");
    assertThat(nOne.multiply(zero).toString()).isEqualTo("-0.0000000000000000e+00");
    assertThat(nOne.multiply(nZero).toString()).isEqualTo("0.0000000000000000e+00");
    assertThat(nZero.divideBy(nOne).toString()).isEqualTo("0.0000000000000000e+00");
    assertThat(zero.divideBy(nOne).toString()).isEqualTo("-0.0000000000000000e+00");
    assertThat(zero.multiply(nZero).toString()).isEqualTo("-0.0000000000000000e+00");
  }

  @Test
  public void additionTest() {
    CFloat ten = new CFloatImpl("10.0", CFloatNativeAPI.CFloatType.DOUBLE);
    CFloat five = new CFloatImpl("5.0", CFloatNativeAPI.CFloatType.SINGLE);
    CFloat nOne = new CFloatImpl("-1.0", CFloatNativeAPI.CFloatType.LONG_DOUBLE);

    CFloatNative p = new CFloatNative(ten.copyWrapper(), ten.getType());
    CFloatNative q = new CFloatNative(five.copyWrapper(), five.getType());
    assertThat(p.toString()).isEqualTo("1.0000000000000000e+01");
    assertThat(q.toString()).isEqualTo("5.00000000e+00");

    CFloat res = ten.add(ten);
    p = new CFloatNative(res.copyWrapper(), CFloatNativeAPI.CFloatType.DOUBLE);
    assertThat(p.toString()).isEqualTo("2.0000000000000000e+01");

    res = res.add(ten).add(ten).add(five.castTo(CFloatType.DOUBLE));
    p = new CFloatNative(res.copyWrapper(), CFloatNativeAPI.CFloatType.DOUBLE);
    assertThat(p.toString()).isEqualTo("4.5000000000000000e+01");

    res = res.castTo(CFloatType.LONG_DOUBLE).add(nOne).add(nOne).add(nOne);
    p = new CFloatNative(res.copyWrapper(), CFloatNativeAPI.CFloatType.LONG_DOUBLE);
    assertThat(p.toString()).isEqualTo("4.20000000000000000000e+01");
  }

  @Test
  public void additionTest_With_Overflowing_Floats() {
    CFloat a = new CFloatNative("1.00000011920928955078125", CFloatNativeAPI.CFloatType.SINGLE);
    CFloat b = new CFloatNative("0.000000059604644775390625", CFloatNativeAPI.CFloatType.SINGLE);

    CFloat aI = new CFloatImpl(a.copyWrapper(), a.getType());
    CFloat bI = new CFloatImpl(b.copyWrapper(), b.getType());

    CFloat resI = aI.add(bI);
    CFloat res = new CFloatNative(resI.copyWrapper(), resI.getType());

    assertThat(res.toString()).isEqualTo("1.00000024e+00");

    CFloatWrapper wrapper = bI.copyWrapper();
    wrapper.setExponent(wrapper.getExponent() - 23);
    CFloat bIFractioned = new CFloatImpl(wrapper, bI.getType());
    CFloat bFractioned = new CFloatNative(wrapper, b.getType());

    CFloat resI2 = resI.add(bI.add(bIFractioned));
    res = new CFloatNative(resI2.copyWrapper(), resI2.getType());

    assertThat(res.toString()).isEqualTo("1.00000036e+00");

    wrapper.setExponent(wrapper.getExponent() - 1);
    // We need to update the CFloat after the wrapper changed as our code is non-mutable
    bIFractioned = new CFloatImpl(wrapper, bI.getType());

    resI2 = resI.add(bI.add(bIFractioned));
    res = new CFloatNative(resI2.copyWrapper(), resI2.getType());

    assertThat(res.toString()).isEqualTo(a.add(b).add(b.add(bFractioned)).toString());
    assertThat(res.toString()).isEqualTo("1.00000024e+00");

    wrapper = bI.copyWrapper();
    wrapper.setExponent(wrapper.getExponent() ^ bI.getSignBitMask());
    bI = new CFloatImpl(wrapper, bI.getType());
    wrapper = bI.copyWrapper();
    wrapper.setExponent(wrapper.getExponent() - 1);
    CFloat bI2 = new CFloatImpl(wrapper, bI.getType());

    bI = bI.add(bI2);

    resI = aI.add(bI);
    resI2 = aI.add(bI2);

    assertThat(new CFloatNative(resI.copyWrapper(), resI.getType()).toString())
        .isEqualTo("1.00000000e+00");
    assertThat(new CFloatNative(resI2.copyWrapper(), resI2.getType()).toString())
        .isEqualTo("1.00000012e+00");
  }

  @Test
  public void additionTest_With_Overflowing_Doubles() {
    CFloat a =
        new CFloatNative(
            "1.0000000000000002220446049250313080847263336181640625",
            CFloatNativeAPI.CFloatType.DOUBLE);
    CFloat b =
        new CFloatNative(
            "0.00000000000000011102230246251565404236316680908203125",
            CFloatNativeAPI.CFloatType.DOUBLE);

    CFloat aI = new CFloatImpl(a.copyWrapper(), a.getType());
    CFloat bI = new CFloatImpl(b.copyWrapper(), b.getType());

    CFloat resI = aI.add(bI);
    CFloat res = new CFloatNative(resI.copyWrapper(), resI.getType());

    assertThat(res.toString()).isEqualTo("1.0000000000000004e+00");

    CFloatWrapper wrapper = bI.copyWrapper();
    wrapper.setExponent(wrapper.getExponent() - 52);
    CFloat bIFractioned = new CFloatImpl(wrapper, bI.getType());

    CFloat resI2 = resI.add(bI.add(bIFractioned));
    res = new CFloatNative(resI2.copyWrapper(), resI2.getType());

    assertThat(res.toString()).isEqualTo("1.0000000000000007e+00");

    wrapper.setExponent(wrapper.getExponent() - 1);
    // We need to update the CFloat after the wrapper changed as our code is non-mutable
    bIFractioned = new CFloatImpl(wrapper, bI.getType());

    resI2 = resI.add(bI.add(bIFractioned));
    res = new CFloatNative(resI2.copyWrapper(), resI2.getType());

    assertThat(res.toString()).isEqualTo("1.0000000000000004e+00");
  }

  @Test
  public void multiplicationTest() {
    CFloat a = new CFloatImpl("2.0", CFloatNativeAPI.CFloatType.SINGLE);
    CFloat b = new CFloatImpl("3.0", CFloatNativeAPI.CFloatType.SINGLE);

    CFloat res = a.multiply(b);
    CFloat cRes = new CFloatNative(res.copyWrapper(), res.getType());
    assertThat(cRes.toString()).isEqualTo("6.00000000e+00");

    res = b.multiply(b);
    cRes = new CFloatNative(res.copyWrapper(), res.getType());
    assertThat(cRes.toString()).isEqualTo("9.00000000e+00");

    res = b.multiply(b).multiply(a).multiply(b);
    cRes = new CFloatNative(res.copyWrapper(), res.getType());
    assertThat(cRes.toString()).isEqualTo("5.40000000e+01");

    res = b.multiply(b).multiply(a).multiply(b).multiply(b);
    cRes = new CFloatNative(res.copyWrapper(), res.getType());
    assertThat(cRes.toString()).isEqualTo("1.62000000e+02");
  }

  @Test
  public void createTest() {
    CFloat a = new CFloatImpl("12345.0", CFloatNativeAPI.CFloatType.DOUBLE);
    CFloat test = new CFloatNative(a.copyWrapper(), a.getType());

    assertThat(test.toString()).isEqualTo("1.2345000000000000e+04");

    CFloat b = new CFloatImpl("-2345.0", CFloatNativeAPI.CFloatType.DOUBLE);
    test = new CFloatNative(b.copyWrapper(), b.getType());

    assertThat(test.toString()).isEqualTo("-2.3450000000000000e+03");

    a = new CFloatImpl("1235124562371616235.0", CFloatNativeAPI.CFloatType.SINGLE);
    b = new CFloatNative("1235124562371616235.0", CFloatNativeAPI.CFloatType.SINGLE);

    assertThat(b.toString()).isEqualTo("1.23512457e+18");
    assertThat(new CFloatNative(a.copyWrapper(), a.getType()).toString())
        .isEqualTo("1.23512457e+18");

    a = new CFloatImpl("0.1235124562371616235", CFloatNativeAPI.CFloatType.SINGLE);
    b = new CFloatNative("0.1235124562371616235", CFloatNativeAPI.CFloatType.SINGLE);

    assertThat(b.toString()).isEqualTo("1.23512454e-01");
    assertThat(new CFloatNative(a.copyWrapper(), a.getType()).toString())
        .isEqualTo("1.23512454e-01");

    a = new CFloatImpl("8388609.0", CFloatNativeAPI.CFloatType.SINGLE);
    b = new CFloatNative("8388609.0", CFloatNativeAPI.CFloatType.SINGLE);

    assertThat(new CFloatNative(a.copyWrapper(), a.getType()).toString()).isEqualTo(b.toString());

    a = new CFloatImpl("16777217.0", CFloatNativeAPI.CFloatType.SINGLE);
    b = new CFloatNative("16777217.0", CFloatNativeAPI.CFloatType.SINGLE);

    assertThat(new CFloatNative(a.copyWrapper(), a.getType()).toString()).isEqualTo(b.toString());

    a = new CFloatImpl("36893488147419103233.0", CFloatNativeAPI.CFloatType.LONG_DOUBLE);
    b = new CFloatNative("36893488147419103233.0", CFloatNativeAPI.CFloatType.LONG_DOUBLE);

    assertThat(b.toString()).isEqualTo("3.68934881474191032320e+19");
    assertThat(new CFloatNative(a.copyWrapper(), a.getType()).toString()).isEqualTo(b.toString());

    a = new CFloatImpl("18446744073709551617.0", CFloatNativeAPI.CFloatType.LONG_DOUBLE);
    b = new CFloatNative("18446744073709551617.0", CFloatNativeAPI.CFloatType.LONG_DOUBLE);

    assertThat(b.toString()).isEqualTo("1.84467440737095516160e+19");
    assertThat(new CFloatNative(a.copyWrapper(), a.getType()).toString()).isEqualTo(b.toString());

    a = new CFloatImpl("36893488147419103235.0", CFloatNativeAPI.CFloatType.LONG_DOUBLE);
    b = new CFloatNative("36893488147419103235.0", CFloatNativeAPI.CFloatType.LONG_DOUBLE);

    assertThat(b.toString()).isEqualTo("3.68934881474191032360e+19");
    assertThat(new CFloatNative(a.copyWrapper(), a.getType()).toString()).isEqualTo(b.toString());
  }

  @Test
  public void nativeAdditionTest() {
    CFloat a =
        new CFloatNative(
            new CFloatWrapper(
                17000L, 0b11000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L),
            CFloatNativeAPI.CFloatType.LONG_DOUBLE);
    CFloat b =
        new CFloatNative(
            new CFloatWrapper(
                17063L, 0b11000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L),
            CFloatNativeAPI.CFloatType.LONG_DOUBLE);

    assertThat(a.add(b).toString()).isEqualTo("7.52468476516967798424e+204");

    b =
        new CFloatNative(
            new CFloatWrapper(
                17064L, 0b11000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L),
            CFloatNativeAPI.CFloatType.LONG_DOUBLE);

    assertThat(a.add(b).toString()).isEqualTo("1.50493695303393559674e+205");

    b =
        new CFloatNative(
            new CFloatWrapper(
                17065L, 0b11000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L),
            CFloatNativeAPI.CFloatType.LONG_DOUBLE);

    assertThat(a.add(b).toString()).isEqualTo("3.00987390606787119326e+205");

    a =
        new CFloatImpl(
            new CFloatWrapper(
                17000L, 0b11000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L),
            CFloatNativeAPI.CFloatType.LONG_DOUBLE);
    b =
        new CFloatImpl(
            new CFloatWrapper(
                17063L, 0b11000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L),
            CFloatNativeAPI.CFloatType.LONG_DOUBLE);

    assertThat(new CFloatNative(a.add(b).copyWrapper(), a.getType()).toString())
        .isEqualTo("7.52468476516967798424e+204");
    b =
        new CFloatImpl(
            new CFloatWrapper(
                17064L, 0b11000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L),
            CFloatNativeAPI.CFloatType.LONG_DOUBLE);

    assertThat(new CFloatNative(a.add(b).copyWrapper(), a.getType()).toString())
        .isEqualTo("1.50493695303393559674e+205");
    b =
        new CFloatImpl(
            new CFloatWrapper(
                17065L, 0b11000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L),
            CFloatNativeAPI.CFloatType.LONG_DOUBLE);

    assertThat(new CFloatNative(a.add(b).copyWrapper(), a.getType()).toString())
        .isEqualTo("3.00987390606787119326e+205");
  }

  @Test
  public void maskTest() {
    assertThat(-1)
        .isEqualTo(0b11111111_11111111_11111111_11111111_11111111_11111111_11111111_11111111L);
  }

  @Test
  public void subtractionOverflowTest() {
    CFloatWrapper wrapperA =
        new CFloatWrapper(
            0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_01111111L,
            0b00000000_00000000_00000000_00000000_00000000_00100000_00000000_00000001L);

    CFloatWrapper wrapperB =
        new CFloatWrapper(
            0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_01111101L,
            0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00000010L);

    CFloat aI = new CFloatImpl(wrapperA, CFloatNativeAPI.CFloatType.SINGLE);
    CFloat bI = new CFloatImpl(wrapperB, aI.getType());

    assertThat(aI.subtract(bI).copyWrapper().getMantissa()).isEqualTo(0);
    assertThat(bI.subtract(aI).copyWrapper().getMantissa()).isEqualTo(0);

    assertThat(aI.subtract(bI).copyWrapper().getExponent()).isEqualTo(127);
    assertThat(bI.subtract(aI).copyWrapper().getExponent()).isEqualTo(383);

    wrapperB.setMantissa(
        0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00000001L);
    // We need to update the CFloat after the wrapper changed as our code is non-mutable
    bI = new CFloatImpl(wrapperB, bI.getType());

    assertThat(aI.subtract(bI).copyWrapper().getMantissa()).isEqualTo(1);
    assertThat(bI.subtract(aI).copyWrapper().getMantissa()).isEqualTo(1);

    assertThat(aI.subtract(bI).copyWrapper().getExponent()).isEqualTo(127);
    assertThat(bI.subtract(aI).copyWrapper().getExponent()).isEqualTo(383);

    wrapperA =
        new CFloatWrapper(
            0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_01111111L,
            0b10100000_00000000_00000000_00000000_00000000_00000000_00000000_00000001L);

    wrapperB =
        new CFloatWrapper(
            0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_01111101L,
            0b10000000_00000000_00000000_00000000_00000000_00000000_00000000_00000010L);

    aI = new CFloatImpl(wrapperA, CFloatNativeAPI.CFloatType.LONG_DOUBLE);
    bI = new CFloatImpl(wrapperB, aI.getType());

    assertThat(aI.subtract(bI).copyWrapper().getMantissa()).isEqualTo(-9223372036854775808L);
    assertThat(bI.subtract(aI).copyWrapper().getMantissa()).isEqualTo(-9223372036854775808L);

    assertThat(aI.subtract(bI).copyWrapper().getExponent()).isEqualTo(127);
    assertThat(bI.subtract(aI).copyWrapper().getExponent()).isEqualTo(32895);

    wrapperB.setMantissa(
        0b10000000_00000000_00000000_00000000_00000000_00000000_00000000_00000001L);
    // We need to update the CFloat after the wrapper changed as our code is non-mutable
    bI = new CFloatImpl(wrapperB, bI.getType());

    assertThat(aI.subtract(bI).copyWrapper().getMantissa()).isEqualTo(-9223372036854775807L);
    assertThat(bI.subtract(aI).copyWrapper().getMantissa()).isEqualTo(-9223372036854775807L);

    assertThat(aI.subtract(bI).copyWrapper().getExponent()).isEqualTo(127);
    assertThat(bI.subtract(aI).copyWrapper().getExponent()).isEqualTo(32895);

    CFloat a = new CFloatNative("12345.03125", CFloatNativeAPI.CFloatType.LONG_DOUBLE);
    CFloat b = new CFloatNative("0.0001220703125", CFloatNativeAPI.CFloatType.SINGLE);

    aI = new CFloatImpl(a.copyWrapper(), a.getType());
    bI = new CFloatImpl(b.copyWrapper(), b.getType()).castTo(CFloatType.LONG_DOUBLE);

    CFloat res = aI.subtract(bI);
    assertThat(new CFloatNative(res.copyWrapper(), res.getType()).toString())
        .isEqualTo("1.23450311279296875000e+04");

    res = bI.subtract(aI);
    assertThat(new CFloatNative(res.copyWrapper(), res.getType()).toString())
        .isEqualTo("-1.23450311279296875000e+04");
  }

  @Test
  public void isZeroTest() {
    CFloat a = new CFloatImpl("0.0", CFloatNativeAPI.CFloatType.SINGLE);
    CFloat b = new CFloatNative("0.0", CFloatNativeAPI.CFloatType.SINGLE);

    assertThat(a.isZero()).isEqualTo(b.isZero());

    CFloat c = new CFloatImpl("-1.0", CFloatNativeAPI.CFloatType.SINGLE);

    assertThat(new CFloatNative(c.copyWrapper(), c.getType()).toString())
        .isEqualTo("-1.00000000e+00");

    a = a.multiply(c);
    b = b.multiply(c);

    assertThat(b.toString()).isEqualTo("-0.00000000e+00");
    assertThat(b.isNegative()).isTrue();
    assertThat(a.isZero()).isEqualTo(b.isZero());
    assertThat(a.isNegative()).isEqualTo(b.isNegative());
  }

  @Test
  public void divisionTest() {
    CFloat a = new CFloatImpl("4.0", CFloatNativeAPI.CFloatType.DOUBLE);
    CFloat b = new CFloatImpl("2.0", CFloatNativeAPI.CFloatType.SINGLE);

    CFloat c = a.divideBy(b.castTo(CFloatType.DOUBLE)).divideBy(a);
    c = new CFloatNative(a.divideBy(c).copyWrapper(), a.getType());

    assertThat(c.toString()).isEqualTo("8.0000000000000000e+00");

    CFloat d = new CFloatImpl("12.5625", CFloatNativeAPI.CFloatType.LONG_DOUBLE);
    assertThat(new CFloatNative(d.copyWrapper(), d.getType()).toString())
        .isEqualTo("1.25625000000000000000e+01");
  }

  @Test
  public void truncTest() {
    CFloat a = new CFloatImpl("-0.25", CFloatNativeAPI.CFloatType.SINGLE);
    CFloat b = new CFloatNative(a.copyWrapper(), a.getType());

    assertThat(b.toString()).isEqualTo("-2.50000000e-01");

    a = a.trunc();
    b = b.trunc();

    assertThat(a.isZero()).isTrue();
    assertThat(b.isZero()).isTrue();
    assertThat(a.isNegative()).isTrue();
    assertThat(b.isNegative()).isTrue();
    assertThat(new CFloatNative(a.copyWrapper(), a.getType()).toString()).isEqualTo(b.toString());

    a = new CFloatImpl("123.625", CFloatNativeAPI.CFloatType.DOUBLE);
    b = new CFloatNative(a.copyWrapper(), a.getType());

    assertThat(b.toString()).isEqualTo("1.2362500000000000e+02");

    a = a.trunc();
    b = b.trunc();

    assertThat(b.toString()).isEqualTo("1.2300000000000000e+02");
    assertThat(new CFloatNative(a.copyWrapper(), a.getType()).toString()).isEqualTo(b.toString());

    a = new CFloatImpl("12345667.0", CFloatNativeAPI.CFloatType.DOUBLE);
    b = new CFloatNative(a.copyWrapper(), a.getType());

    assertThat(b.toString()).isEqualTo("1.2345667000000000e+07");
    assertThat(new CFloatNative(a.copyWrapper(), a.getType()).toString()).isEqualTo(b.toString());
  }

  @Test
  public void roundTest() {
    CFloat a = new CFloatImpl("2134.5625", CFloatNativeAPI.CFloatType.DOUBLE);
    CFloat b = new CFloatNative(a.copyWrapper(), a.getType());

    assertThat(b.toString()).isEqualTo("2.1345625000000000e+03");

    a = a.round();
    b = b.round();

    assertThat(b.toString()).isEqualTo("2.1350000000000000e+03");
    assertThat(a.copyWrapper().getExponent())
        .isEqualTo(b.copyWrapper().getExponent() & (b.getExponentMask() ^ b.getSignBitMask()));
    assertThat(a.copyWrapper().getMantissa())
        .isEqualTo(b.copyWrapper().getMantissa() & b.getNormalizedMantissaMask());

    a = new CFloatImpl("-2134.5625", CFloatNativeAPI.CFloatType.DOUBLE);
    b = new CFloatNative(a.copyWrapper(), a.getType());

    assertThat(b.toString()).isEqualTo("-2.1345625000000000e+03");

    a = a.round();
    b = b.round();

    assertThat(b.toString()).isEqualTo("-2.1350000000000000e+03");
    assertThat(a.copyWrapper().getExponent())
        .isEqualTo(b.copyWrapper().getExponent() & (b.getExponentMask() ^ b.getSignBitMask()));
    assertThat(a.copyWrapper().getMantissa())
        .isEqualTo(b.copyWrapper().getMantissa() & b.getNormalizedMantissaMask());

    a = new CFloatImpl("-2134.3125", CFloatNativeAPI.CFloatType.DOUBLE);
    b = new CFloatNative(a.copyWrapper(), a.getType());

    assertThat(b.toString()).isEqualTo("-2.1343125000000000e+03");

    a = a.round();
    b = b.round();

    assertThat(b.toString()).isEqualTo("-2.1340000000000000e+03");
    assertThat(a.copyWrapper().getExponent())
        .isEqualTo(b.copyWrapper().getExponent() & (b.getExponentMask() ^ b.getSignBitMask()));
    assertThat(a.copyWrapper().getMantissa())
        .isEqualTo(b.copyWrapper().getMantissa() & b.getNormalizedMantissaMask());

    a = new CFloatImpl("63.96875", CFloatNativeAPI.CFloatType.DOUBLE);
    b = new CFloatNative(a.copyWrapper(), a.getType());

    assertThat(b.toString()).isEqualTo("6.3968750000000000e+01");

    a = a.round();
    b = b.round();

    assertThat(b.toString()).isEqualTo("6.4000000000000000e+01");

    assertThat(a.copyWrapper().getExponent())
        .isEqualTo(b.copyWrapper().getExponent() & (b.getExponentMask() ^ b.getSignBitMask()));
    assertThat(a.copyWrapper().getMantissa())
        .isEqualTo(b.copyWrapper().getMantissa() & b.getNormalizedMantissaMask());
  }

  @Test
  public void divisionTest_2() {
    CFloat a = new CFloatImpl("625.0", CFloatNativeAPI.CFloatType.DOUBLE);
    CFloat b = new CFloatImpl("1000.0", CFloatNativeAPI.CFloatType.DOUBLE);

    CFloat c = new CFloatNative(a.copyWrapper(), a.getType());
    CFloat d = new CFloatNative(b.copyWrapper(), b.getType());

    assertThat(c.toString()).isEqualTo("6.2500000000000000e+02");
    assertThat(d.toString()).isEqualTo("1.0000000000000000e+03");

    CFloat e = a.divideBy(b);
    CFloat f = c.divideBy(d);

    assertThat(f.toString()).isEqualTo("6.2500000000000000e-01");
    assertThat(e.copyWrapper().getExponent())
        .isEqualTo(f.copyWrapper().getExponent() & (b.getExponentMask() ^ b.getSignBitMask()));
    assertThat(e.copyWrapper().getMantissa())
        .isEqualTo(f.copyWrapper().getMantissa() & b.getNormalizedMantissaMask());

    a = new CFloatImpl("96875.0", CFloatNativeAPI.CFloatType.DOUBLE);
    b = new CFloatImpl("100000.0", CFloatNativeAPI.CFloatType.DOUBLE);
    CFloat one = new CFloatImpl("1.0", CFloatNativeAPI.CFloatType.DOUBLE);

    c = new CFloatNative(a.copyWrapper(), a.getType());
    d = new CFloatNative(b.copyWrapper(), b.getType());

    assertThat(c.toString()).isEqualTo("9.6875000000000000e+04");
    assertThat(d.toString()).isEqualTo("1.0000000000000000e+05");

    e = one.divideBy(b.divideBy(a));
    one = new CFloatNative(one.copyWrapper(), one.getType());
    f = one.divideBy(d.divideBy(c));

    assertThat(f.toString()).isEqualTo("9.6875000000000000e-01");
    assertThat(e.copyWrapper().getExponent())
        .isEqualTo(f.copyWrapper().getExponent() & (b.getExponentMask() ^ b.getSignBitMask()));
    assertThat(e.copyWrapper().getMantissa())
        .isEqualTo(f.copyWrapper().getMantissa() & b.getNormalizedMantissaMask());

    a = new CFloatImpl("87500.0", CFloatNativeAPI.CFloatType.DOUBLE);
    b = new CFloatImpl("100000.0", CFloatNativeAPI.CFloatType.DOUBLE);

    c = new CFloatNative(a.copyWrapper(), a.getType());
    d = new CFloatNative(b.copyWrapper(), b.getType());

    assertThat(c.toString()).isEqualTo("8.7500000000000000e+04");
    assertThat(d.toString()).isEqualTo("1.0000000000000000e+05");

    e = a.divideBy(b);
    f = c.divideBy(d);

    assertThat(f.toString()).isEqualTo("8.7500000000000000e-01");
    assertThat(e.copyWrapper().getExponent())
        .isEqualTo(f.copyWrapper().getExponent() & (b.getExponentMask() ^ b.getSignBitMask()));
    assertThat(e.copyWrapper().getMantissa())
        .isEqualTo(f.copyWrapper().getMantissa() & b.getNormalizedMantissaMask());
  }

  @Test
  public void divisionTest_3() {
    CFloat one = CFloatNativeAPI.ONE_DOUBLE;
    CFloat nOne = new CFloatNative(one.copyWrapper(), one.getType());

    CFloat a = new CFloatImpl("123348857384573888.0", CFloatNativeAPI.CFloatType.DOUBLE);
    CFloat b = new CFloatNative(a.copyWrapper(), a.getType());

    assertThat(b.toString()).isEqualTo("1.2334885738457389e+17");

    a = one.divideBy(a);
    b = nOne.divideBy(b);

    assertThat(b.toString()).isEqualTo("8.1070876634246049e-18");
    assertThat(new CFloatNative(a.copyWrapper(), a.getType()).toString())
        .isEqualTo("8.1070876634246049e-18");

    a = new CFloatImpl("123.0", CFloatNativeAPI.CFloatType.DOUBLE);
    b = new CFloatNative(a.copyWrapper(), a.getType());

    assertThat(b.toString()).isEqualTo("1.2300000000000000e+02");

    a = one.divideBy(a);
    b = nOne.divideBy(b);

    assertThat(b.toString()).isEqualTo("8.1300813008130090e-03");
    assertThat(new CFloatNative(a.copyWrapper(), a.getType()).toString()).isEqualTo(b.toString());
  }

  @Test
  public void toStringTest() {
    CFloat a = new CFloatImpl("2784365.34543", CFloatNativeAPI.CFloatType.SINGLE);
    CFloat b = new CFloatNative("2784365.34543", CFloatNativeAPI.CFloatType.SINGLE);

    assertThat(a.copyWrapper().getExponent()).isEqualTo(b.copyWrapper().getExponent());
    assertThat(a.copyWrapper().getMantissa()).isEqualTo(b.copyWrapper().getMantissa());
    assertThat(a.toString()).isEqualTo(b.toString());
  }

  @Test
  public void toStringTest_floatValueWithLeadingZero() {
    CFloat a = new CFloatImpl("0.6", CFloatNativeAPI.CFloatType.SINGLE);
    CFloat b = new CFloatNative("0.6", CFloatNativeAPI.CFloatType.SINGLE);

    assertThat(a.copyWrapper().getExponent()).isEqualTo(b.copyWrapper().getExponent());
    assertThat(a.copyWrapper().getMantissa()).isEqualTo(b.copyWrapper().getMantissa());
    assertThat(a.toString()).isEqualTo(b.toString());
  }

  @Test
  public void toStringTest_negativeFloatValueWithLeadingZero() {
    CFloat a = new CFloatImpl("-0.6", CFloatNativeAPI.CFloatType.SINGLE);
    CFloat b = new CFloatNative("-0.6", CFloatNativeAPI.CFloatType.SINGLE);

    assertThat(a.copyWrapper().getExponent()).isEqualTo(b.copyWrapper().getExponent());
    assertThat(a.copyWrapper().getMantissa()).isEqualTo(b.copyWrapper().getMantissa());
    assertThat(a.toString()).isEqualTo(b.toString());
  }

  @Test
  public void toStringTest_doubleValueWithLeadingZero() {
    CFloat a = new CFloatImpl("0.6", CFloatNativeAPI.CFloatType.DOUBLE);
    CFloat b = new CFloatNative("0.6", CFloatNativeAPI.CFloatType.DOUBLE);

    assertThat(a.copyWrapper().getExponent()).isEqualTo(b.copyWrapper().getExponent());
    assertThat(a.copyWrapper().getMantissa()).isEqualTo(b.copyWrapper().getMantissa());
    assertThat(a.toString()).isEqualTo(b.toString());
  }

  @Test
  public void toStringTest_negativeDoubleValueWithLeadingZero() {
    CFloat a = new CFloatImpl("-0.6", CFloatNativeAPI.CFloatType.DOUBLE);
    CFloat b = new CFloatNative("-0.6", CFloatNativeAPI.CFloatType.DOUBLE);
    assertThat(a.copyWrapper().getExponent()).isEqualTo(b.copyWrapper().getExponent());
    assertThat(a.copyWrapper().getMantissa()).isEqualTo(b.copyWrapper().getMantissa());
    assertThat(a.toString()).isEqualTo(b.toString());
  }

  @Test
  public void toStringTest_longDoubleValueWithLeadingZero() {
    CFloat a = new CFloatImpl("0.6", CFloatNativeAPI.CFloatType.LONG_DOUBLE);
    CFloat b = new CFloatNative("0.6", CFloatNativeAPI.CFloatType.LONG_DOUBLE);

    assertThat(a.copyWrapper().getExponent()).isEqualTo(b.copyWrapper().getExponent());
    assertThat(a.copyWrapper().getMantissa()).isEqualTo(b.copyWrapper().getMantissa());
    assertThat(a.toString()).isEqualTo(b.toString());
  }

  @Test
  public void toStringTest_negativeLongDoubleValueWithLeadingZero() {
    CFloat a = new CFloatImpl("-0.6", CFloatNativeAPI.CFloatType.LONG_DOUBLE);
    CFloat b = new CFloatNative("-0.6", CFloatNativeAPI.CFloatType.LONG_DOUBLE);

    assertThat(a.copyWrapper().getExponent()).isEqualTo(b.copyWrapper().getExponent());
    assertThat(a.copyWrapper().getMantissa()).isEqualTo(b.copyWrapper().getMantissa());
    assertThat(a.toString()).isEqualTo(b.toString());
  }

  @Test
  public void toStringTest_floatValueWithZeroExponent() {
    CFloat a = new CFloatImpl("1.000001", CFloatNativeAPI.CFloatType.SINGLE);
    CFloat b = new CFloatNative("1.000001", CFloatNativeAPI.CFloatType.SINGLE);

    assertThat(a.copyWrapper().getExponent()).isEqualTo(b.copyWrapper().getExponent());
    assertThat(a.copyWrapper().getMantissa()).isEqualTo(b.copyWrapper().getMantissa());
    assertThat(a.toString()).isEqualTo(b.toString());
  }

  @Test
  public void toStringTest_doubleValueWithZeroExponent() {
    CFloat a = new CFloatImpl("1.000001", CFloatNativeAPI.CFloatType.DOUBLE);
    CFloat b = new CFloatNative("1.000001", CFloatNativeAPI.CFloatType.DOUBLE);

    assertThat(a.copyWrapper().getExponent()).isEqualTo(b.copyWrapper().getExponent());
    assertThat(a.copyWrapper().getMantissa()).isEqualTo(b.copyWrapper().getMantissa());
    assertThat(a.toString()).isEqualTo(b.toString());
  }

  @Test
  public void toStringTest_longDoubleValueWithZeroExponent() {
    CFloat a = new CFloatImpl("1.000001", CFloatNativeAPI.CFloatType.LONG_DOUBLE);
    CFloat b = new CFloatNative("1.000001", CFloatNativeAPI.CFloatType.LONG_DOUBLE);

    assertThat(a.copyWrapper().getExponent()).isEqualTo(b.copyWrapper().getExponent());
    assertThat(a.copyWrapper().getMantissa()).isEqualTo(b.copyWrapper().getMantissa());
    assertThat(a.toString()).isEqualTo(b.toString());
  }

  @Test
  public void castFloatToLongDoubleTest() {
    CFloat a = new CFloatImpl("893473.378465376", CFloatNativeAPI.CFloatType.SINGLE);
    CFloat b = new CFloatNative("893473.378465376", CFloatNativeAPI.CFloatType.SINGLE);

    assertThat(a.toString()).isEqualTo(b.toString());
    a = a.castTo(CFloatType.LONG_DOUBLE);
    b = b.castTo(CFloatType.LONG_DOUBLE);
    assertThat(a.toString()).isEqualTo(b.toString());
  }
}
