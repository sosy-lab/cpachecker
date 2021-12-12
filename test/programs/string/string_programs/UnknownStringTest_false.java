// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

import java.util.Scanner;

public class UnknownStringTest_false {
  public static void main(String[]main) {
    Scanner sc = new Scanner(System.in);
    String b = sc.nextLine();
    assert b.length()> 20; 
    }
}
