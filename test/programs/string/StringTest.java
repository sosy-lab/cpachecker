// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class StringTest {

  public static void main() {
    int e=5;
    String a = "hello";
    String b = " World!";
    String c = a + b;
    String d = a;
    d=b;
    assert c == "hello World!";
  }
}