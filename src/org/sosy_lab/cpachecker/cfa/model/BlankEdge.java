// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model;

import java.util.Objects;
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

  @Override
  public boolean equals(final Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (!(pOther instanceof BlankEdge)) {
      return false;
    }
    if (!super.equals(pOther)) {
      return false;
    }
    final BlankEdge blankEdge = (BlankEdge) pOther;
    return description.equals(blankEdge.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), description);
  }
}
