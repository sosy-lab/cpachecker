// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ast;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public class StatementElement {

  private final ASTElement completeElement;

  public StatementElement(FileLocation pStatementLocation) {
    completeElement = determineElement(pStatementLocation);
  }

  public ASTElement getCompleteElement() {
    return completeElement;
  }

  public ASTElement determineElement(FileLocation pConditionLocation) {
    return new ASTElement(pConditionLocation);
  }

  protected void setEdges(ImmutableSet<CFAEdge> pEdges) {
    completeElement.setEdges(pEdges);
  }
}
