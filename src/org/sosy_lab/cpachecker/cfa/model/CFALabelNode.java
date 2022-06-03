// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model;

import static com.google.common.base.Preconditions.checkArgument;

import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;

/** A CFANode for a location that a label in the original source code. */
public class CFALabelNode extends CFANode {

  private static final long serialVersionUID = 9172364902060726643L;
  private final String label;

  public CFALabelNode(AFunctionDeclaration pFunction, String pLabel) {
    super(pFunction);
    checkArgument(!pLabel.isEmpty());
    label = pLabel;
  }

  public String getLabel() {
    return label;
  }
}
