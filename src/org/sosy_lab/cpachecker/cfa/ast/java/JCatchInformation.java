// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class JCatchInformation {

  private final CFANode node;
  private final String rawStatement;
  private final JStatement statement;
  private final FileLocation fileLocation;
  private final boolean conditional;

  public JCatchInformation(
      CFANode pNode,
      String pRawStatement,
      JStatement pStatement,
      FileLocation pFileLocation,
      boolean pConditional) {
    node = pNode;
    rawStatement = pRawStatement;
    statement = pStatement;
    fileLocation = pFileLocation;
    conditional = pConditional;
  }

  public CFANode getNode() {
    return node;
  }

  public String getRawStatement() {
    return rawStatement;
  }

  public JStatement getStatement() {
    return statement;
  }

  public FileLocation getFileLocation() {
    return fileLocation;
  }

  public boolean getConditional() {
    return conditional;
  }

  @Override
  public String toString() {
    return "[Node: "
        + node
        + " ;Raw Statement: "
        + rawStatement
        + " ;Statement: "
        + statement
        + " ;FileLocation: "
        + fileLocation
        + " ; Conditional: "
        + conditional
        + "]";
  }
}
