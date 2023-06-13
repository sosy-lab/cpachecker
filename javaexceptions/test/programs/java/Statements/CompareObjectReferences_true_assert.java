// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class CompareObjectReferences_true_assert {

  public static void main(String[] args) {
    CompareObjectReferences_true_assert a = new CompareObjectReferences_true_assert();
    CompareObjectReferences_true_assert b = a;

    assert (a == b); // always true
  }
}
