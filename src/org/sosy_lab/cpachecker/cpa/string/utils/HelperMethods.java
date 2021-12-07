// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string.utils;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;

/*
 * Helper - class
 */
public class HelperMethods {

  public static boolean isString(Type pType) {

    if (pType instanceof JClassType) {

      JClassType t = (JClassType) pType;

      if (t.getName().equals("java.lang.String") && t.getSimpleName().equals("String")) {
        return true;
      }

    }

    return false;
  }
}
