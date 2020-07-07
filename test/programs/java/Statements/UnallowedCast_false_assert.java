// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class UnallowedCast_false_assert {

  public static void main(String[] args) {
    checkInDeclaration();
    checkInAssignment();
  }

  private static void checkInDeclaration() {
    char n = 65536; // not allowed without explicit cast

    assert n == 65536;
  }

  private static void checkInAssignment() {
    char n;
    
    n = 65536;
    assert n == 65536;
  }
}
