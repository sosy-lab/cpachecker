// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public abstract class AbstractCFAEdge implements CFAEdge {

  private static final long serialVersionUID = -8493135984889665408L;
  private final CFANode predecessor;
  private final CFANode successor;
  private final String rawStatement;
  private final FileLocation fileLocation;

  protected AbstractCFAEdge(
      String pRawStatement, FileLocation pFileLocation, CFANode pPredecessor, CFANode pSuccessor) {

    Preconditions.checkNotNull(pRawStatement);
    Preconditions.checkNotNull(pPredecessor);
    Preconditions.checkNotNull(pSuccessor);

    predecessor = pPredecessor;
    successor = pSuccessor;
    rawStatement = pRawStatement;
    fileLocation = checkNotNull(pFileLocation);
  }

  @Override
  public CFANode getPredecessor() {
    return predecessor;
  }

  @Override
  public CFANode getSuccessor() {
    return successor;
  }

  @Override
  public String getRawStatement() {
    return rawStatement;
  }

  @Override
  public Optional<AAstNode> getRawAST() {
    return Optional.empty();
  }

  @Override
  public String getDescription() {
    return getCode();
  }

  @Override
  public int getLineNumber() {
    return fileLocation.getStartingLineNumber();
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public int hashCode() {
    return 31 * predecessor.hashCode() + successor.hashCode();
  }

  @Override
  public boolean equals(Object pOther) {
    if (!(pOther instanceof AbstractCFAEdge)) {
      return false;
    }

    AbstractCFAEdge otherEdge = (AbstractCFAEdge) pOther;
    return predecessor.equals(otherEdge.predecessor) && successor.equals(otherEdge.successor);
  }

  @Override
  public String toString() {
    return getFileLocation()
        + ":\t"
        + getPredecessor()
        + " -{"
        + getDescription().replace('\n', ' ')
        + "}-> "
        + getSuccessor();
  }
}
