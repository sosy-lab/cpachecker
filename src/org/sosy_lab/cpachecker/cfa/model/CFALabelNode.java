// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;

/** A CFANode for a location that a label in the original source code. */
public final class CFALabelNode extends CFANode {

  private static final long serialVersionUID = 9172364902060726643L;
  private final String label;

  public CFALabelNode(AFunctionDeclaration pFunction, String pLabel) {
    super(pFunction);
    checkArgument(!pLabel.isEmpty());
    label = pLabel;
  }

  /**
   * Constructor for a label node with additional information about the variables in scope at this
   * node for the original program.
   *
   * @param pFunction the function this node belongs to
   * @param pLabel the label of this node
   * @param pLocalInScopeVariables the local variables that are in scope at this node in the
   *     original program
   * @param pGlobalInScopeVariables the global variables that are in scope at this node in the
   *     original program
   */
  public CFALabelNode(
      AFunctionDeclaration pFunction,
      String pLabel,
      ImmutableSet<CSimpleDeclaration> pLocalInScopeVariables,
      ImmutableSet<CSimpleDeclaration> pGlobalInScopeVariables) {
    super(pFunction, pLocalInScopeVariables, pGlobalInScopeVariables);
    checkArgument(!pLabel.isEmpty());
    label = pLabel;
  }

  public String getLabel() {
    return label;
  }
}
