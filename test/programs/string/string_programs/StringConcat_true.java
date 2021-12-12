// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class StringConcat_true {
  public class Main {
  }

  public static void main(String[] args) {

    String s1 = "ar";
    String s2 = "mo";
    String s3 = "ur";

    String result = s1 + s2;
    assert result.equals("armo");
    result = result + s3;

    assert result.equals("armour");

    result = s3 + "card";
    String compareTo = "urcard";
    assert result.equals(compareTo);
  }
}
