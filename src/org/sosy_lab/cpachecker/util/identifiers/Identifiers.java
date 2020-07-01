/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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
