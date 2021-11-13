// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class MiscTest {
  public static void main(String[] args) {
    String a = "Hello";
    String b = "Final";
    String c = b + a;
    assert a.length() == 5;
    assert c.equals(b + a);
    }
}
