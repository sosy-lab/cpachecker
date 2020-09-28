// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;


public class BlankEdge extends AbstractCFAEdge {

  private static final long serialVersionUID = 6394933292868202442L;

  private final String description;

  public BlankEdge(String pRawStatement, FileLocation pFileLocation,  CFANode pPredecessor,
      CFANode pSuccessor, String pDescription) {

    super(pRawStatement, pFileLocation, pPredecessor, pSuccessor);
    description = pDescription;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public String getCode() {
    return "";
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.BlankEdge;
  }
}
