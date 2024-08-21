// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package pack;

public class IFE {


  public static void main(String args[]) {
    boolean a = false;
    boolean b = false;
    boolean c = true;
    boolean d = false;
    boolean e = false;
    boolean f = true;
    boolean g = false;
    boolean h = true;

    boolean i = a && b | c & d || e && f | g & h;


  }

}
