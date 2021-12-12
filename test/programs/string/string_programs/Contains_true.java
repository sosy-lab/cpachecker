// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class Contains_true {

  private static String s1 = "man"; 
  
  public static void main(String[] args) {
    String contain1 = "ma";
    assert s1.contains(contain1);
      
    String s2 = "Bat";
    String s3 = s2+ s1;
    String contain2 = "t"+contain1;
    assert s3.contains(contain2);
    }

}