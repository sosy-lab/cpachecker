// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

/**
 * An enumeration listing the visibility modifier public, protected, none, private of Java.
 */
public enum VisibilityModifier {

  PUBLIC     ("public"),
  NONE   (""),
  PROTECTED    ("protected"),
  PRIVATE        ("private")
  ;

  private final String mod;

  VisibilityModifier(String pMod) {
    mod = pMod;
  }

  public String getModifierString() {
    return mod;
  }
}
