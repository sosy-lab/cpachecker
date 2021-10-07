// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class PrefixSuffix {
  public static void main(String[] args) {
    String pref = "suffix";
    String suff = "prefix";
    assert pref.substring(0, 3).equals("suf");
    assert suff.substring(4, 6).equals("fix");
    return;
  }

}
