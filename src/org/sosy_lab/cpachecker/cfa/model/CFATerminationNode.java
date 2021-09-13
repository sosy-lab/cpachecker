// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model;

import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaNode;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaNodeVisitor;

/** A CFANode that marks the end of a path. */
public class CFATerminationNode extends CFANode implements CCfaNode {

  private static final long serialVersionUID = -8328879108494506389L;

  public CFATerminationNode(AFunctionDeclaration pFunction) {
    super(pFunction);
  }

  @Override
  public void addLeavingEdge(CFAEdge pNewLeavingEdge) {
    throw new AssertionError(pNewLeavingEdge);
  }

  @Override
  public void addLeavingSummaryEdge(FunctionSummaryEdge pEdge) {
    throw new AssertionError(pEdge);
  }

  @Override
  public <R, X extends Exception> R accept(CCfaNodeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }
}
