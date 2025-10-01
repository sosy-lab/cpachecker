// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ast;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public final class DeclarationElement {

  private final ASTElement completeElement;

  public DeclarationElement(FileLocation pStatementLocation, ImmutableSet<CFAEdge> pEdges) {
    completeElement = determineElement(pStatementLocation, pEdges);
  }

  public ASTElement getCompleteElement() {
    return completeElement;
  }

  public ASTElement determineElement(
      FileLocation pConditionLocation, ImmutableSet<CFAEdge> pEdges) {
    return new ASTElement(pConditionLocation, pEdges);
  }
}
