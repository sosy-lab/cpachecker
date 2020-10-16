// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.identifiers;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;

public class Identifiers {

  public static Collection<AbstractIdentifier> getDereferencedIdentifiers(AbstractIdentifier id) {
    Set<AbstractIdentifier> result = new HashSet<>();
    // Add itself
    for (int d = id.getDereference(); d >= 0; d--) {
      result.add(id.cloneWithDereference(d));
    }
    return result;
  }

  static String getCharsOf(int dereference) {
    StringBuilder info = new StringBuilder();
    if (dereference > 0) {
      for (int i = 0; i < dereference; i++) {
        info.append("*");
      }
    } else if (dereference == -1) {
      info.append("&");
    } else if (dereference < -1) {
      info.append("Error in string representation, dereference < -1");
    }
    return info.toString();
  }

  public static AbstractIdentifier createIdentifier(CExpression expression, String function) {
    return createIdentifier(expression, 0, function);
  }

  public static AbstractIdentifier createIdentifier(
      CExpression expression, int dereference, String function) {
    IdentifierCreator idCreator = new IdentifierCreator(function);
    return idCreator.createIdentifier(expression, dereference);
  }
}
