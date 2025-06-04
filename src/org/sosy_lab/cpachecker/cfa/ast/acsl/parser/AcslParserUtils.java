// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslScope;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.AcslParser.AcslParseException;
import org.sosy_lab.cpachecker.cfa.parser.Scope;

public class AcslParserUtils {

  /**
   * Creates a new {@link AcslScope} for the AcslParser
   * @return the AcslScope
   */
  public static AcslScope getAcslScope() {
    AcslScope scope = AcslScope.empty();
    return scope;
  }

  /**
   * Parses the {@link Scope} into a {@link CProgramScope} for the
   * @param pScope Scope to be parsed
   * @return Scope as CProgramScope
   * @throws AcslParseException if Scope is not of type {@link CProgramScope}
   */
  public static CProgramScope parseScopeToCProgramScope(Scope pScope) throws AcslParseException {
    if (pScope instanceof CProgramScope) {
      return (CProgramScope) pScope;
    }
    throw new AcslParseException("Scope is not of type CProgramScope");
  }
}
