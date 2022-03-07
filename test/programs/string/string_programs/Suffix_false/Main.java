// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class Main {

  public static void main(String[] args) {

    String s1 = "batman";
    String nonSuffix = "tma";

    assert s1.endsWith(nonSuffix);

    String s2 = "superman";
    String s3 = s1 + s2;
    assert s3.endsWith(nonSuffix);
  }
}
