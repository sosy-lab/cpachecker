// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

public final class SMGListAbstractionTestInputs {

  private static final int LONG_TEST_LIST_LENGTH = 6;

  private static final SMGValue REGION_A = newVal();
  private static final SMGValue REGION_B = newVal();

  private static final SMGValue[] LIST_EQ_2 = {REGION_A, REGION_A};
  private static final SMGValue[] LIST_DIFF_2 = {newVal(), newVal()};
  private static final SMGValue[] LIST_EQ_3 = {REGION_A, REGION_A, REGION_A};
  private static final SMGValue[] LIST_DIFF_3 = {newVal(), newVal(), newVal()};
  private static final SMGValue[] LIST_EQ_4 = {REGION_A, REGION_A, REGION_A, REGION_A};
  private static final SMGValue[] LIST_DIFF_4 = {newVal(), newVal(), newVal(), newVal()};
  private static final SMGValue[] LIST_EQ_5 = {REGION_A, REGION_A, REGION_A, REGION_A, REGION_A};
  private static final SMGValue[] LIST_DIFF_5 = {newVal(), newVal(), newVal(), newVal(), newVal()};
  private static final SMGValue[] LIST_EQ_X = new SMGValue[LONG_TEST_LIST_LENGTH];
  private static final SMGValue[] LIST_DIFF_X = new SMGValue[LONG_TEST_LIST_LENGTH];

  static {
    for (int i = 0; i < LIST_EQ_X.length; i++) {
      LIST_EQ_X[i] = REGION_A;
    }
    for (int i = 0; i < LIST_DIFF_X.length; i++) {
      LIST_DIFF_X[i] = newVal();
    }
  }

  private static final SMGValue[][] SUBLISTS_A =
      new SMGValue[][] {{REGION_A, REGION_A}, {REGION_A, REGION_A}};
  private static final SMGValue[][] SUBLISTS_B =
      new SMGValue[][] {
        {newVal(), newVal(), newVal()},
        {newVal(), newVal()},
        {newVal(), newVal()},
        {newVal(), newVal(), newVal()}
      };
  private static final SMGValue[][] SUBLISTS_C = new SMGValue[][] {{}, {}};
  private static final SMGValue[][] SUBLISTS_D = new SMGValue[][] {{newVal()}, {newVal()}};
  private static final SMGValue[][] SUBLISTS_E = new SMGValue[][] {{newVal()}, {}};
  private static final SMGValue[][] SUBLISTS_F =
      new SMGValue[][] {LIST_EQ_2, LIST_DIFF_2, LIST_EQ_3};
  private static final SMGValue[][] SUBLISTS_G = new SMGValue[][] {LIST_EQ_3, LIST_EQ_3, LIST_EQ_3};
  private static final SMGValue[][] SUBLISTS_H = new SMGValue[LONG_TEST_LIST_LENGTH][1];

  static {
    for (int i = 0; i < SUBLISTS_H.length; i++) {
      SUBLISTS_H[i] = new SMGValue[] {newVal()};
    }
  }

  private static SMGValue newVal() {
    return SMGKnownSymValue.of();
  }

  private static List<Object[]> cartesian(Object[] objects0, Object[] objects1, Object[] objects2) {
    List<Object[]> result = new ArrayList<>(objects0.length * objects1.length * objects2.length);
    for (Object obj0 : objects0) {
      for (Object obj1 : objects1) {
        for (Object obj2 : objects2) {
          result.add(new Object[] {obj0, obj1, obj2});
        }
      }
    }
    return result;
  }

  private static List<Object[]> cartesian(
      Object[] objects0, Object[] objects1, Object[] objects2, Object[] objects3) {
    List<Object[]> result =
        new ArrayList<>(objects0.length * objects1.length * objects2.length * objects3.length);
    for (Object obj0 : objects0) {
      for (Object obj1 : objects1) {
        for (Object obj2 : objects2) {
          for (Object obj3 : objects3) {
            result.add(new Object[] {obj0, obj1, obj2, obj3});
          }
        }
      }
    }
    return result;
  }

  public static List<Object[]> getValuesAsTestInputs() {
    return Arrays.asList(
        new Object[] {LIST_EQ_2},
        new Object[] {LIST_DIFF_2},
        new Object[] {LIST_EQ_3},
        new Object[] {LIST_DIFF_3},
        new Object[] {LIST_EQ_4},
        new Object[] {LIST_DIFF_4},
        new Object[] {LIST_EQ_5},
        new Object[] {LIST_DIFF_5},
        new Object[] {LIST_EQ_X},
        new Object[] {LIST_DIFF_X});
  }

  public static List<Object[]> getAttachRegionToListTestInputs() {
    return cartesian(
        new Object[] {LIST_EQ_2, LIST_DIFF_2, LIST_EQ_3, LIST_DIFF_3},
        new Object[] {REGION_A, REGION_B},
        new Object[] {SMGListCircularity.OPEN, SMGListCircularity.CIRCULAR},
        new Object[] {SMGListLinkage.SINGLE_LINKED, SMGListLinkage.DOUBLY_LINKED});
  }

  public static List<Object[]> getListsWithValuesAsTestInputs() {
    return cartesian(
        new Object[] {LIST_EQ_2, LIST_DIFF_2, LIST_EQ_3, LIST_DIFF_3},
        new Object[] {SMGListCircularity.OPEN, SMGListCircularity.CIRCULAR},
        new Object[] {SMGListLinkage.SINGLE_LINKED, SMGListLinkage.DOUBLY_LINKED});
  }

  public static List<Object[]> getListsWithSublistsAsTestInputs() {
    return cartesian(
        new Object[] {
          SUBLISTS_A,
          SUBLISTS_B,
          SUBLISTS_C,
          SUBLISTS_D,
          SUBLISTS_E,
          SUBLISTS_F,
          SUBLISTS_G,
          SUBLISTS_H
        },
        new Object[] {SMGListCircularity.OPEN, SMGListCircularity.CIRCULAR},
        new Object[] {SMGListLinkage.SINGLE_LINKED, SMGListLinkage.DOUBLY_LINKED});
  }

  private SMGListAbstractionTestInputs() {}
}
