// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;

/**
 * A CFANode that marks the end of a path.
 */
public class CFATerminationNode extends CFANode {

  private static final long serialVersionUID = -8328879108494506389L;

  public CFATerminationNode(AFunctionDeclaration pFunction) {
    super(pFunction);
  }

  public CFATerminationNode(String dummyName) {
    super(
        new CFunctionDeclaration(
            FileLocation.DUMMY,
            CFunctionType.NO_ARGS_VOID_FUNCTION,
            dummyName,
            ImmutableList.of()));
  }

  @Override
  public void addLeavingEdge(CFAEdge pNewLeavingEdge) {
    throw new AssertionError(pNewLeavingEdge);
  }

  @Override
  public void addLeavingSummaryEdge(FunctionSummaryEdge pEdge) {
    throw new AssertionError(pEdge);
  }
}
