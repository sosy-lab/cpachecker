/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.templates;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.java_smt.api.Formula;

/**
 * Implementation of {@link TVariable} using
 * {@link org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression}.
 */
public class CIdVariable implements TVariable {

  /**
   * Converters require extra types for logging.
   * We just give them dummy ones.
   */
  private static final CFAEdge dummyEdge = new BlankEdge("",
      FileLocation.DUMMY,
      new CFANode("dummy-1"), new CFANode("dummy-2"), "Dummy Edge");

  private final CIdExpression identifier;

  public CIdVariable(CIdExpression pIdentifier) {
    identifier = pIdentifier;
  }

  @Override
  public String getName() {
    return identifier.getDeclaration().getQualifiedName();
  }

  @Override
  public Formula toFormula(
      PathFormulaManager pfmgr,
      PathFormula contextFormula) {
    try {
      return pfmgr.expressionToFormula(
          contextFormula, identifier, dummyEdge
      );
    } catch (UnrecognizedCCodeException pE) {
      throw new UnsupportedOperationException(pE);
    }
  }

  @Override
  public CSimpleType getType() {
    return (CSimpleType) identifier.getExpressionType().getCanonicalType();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }
    CIdVariable that = (CIdVariable) pO;
    return Objects.equals(identifier, that.identifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier);
  }

  @Override
  public String toString() {
    return identifier.toString();
  }
}
