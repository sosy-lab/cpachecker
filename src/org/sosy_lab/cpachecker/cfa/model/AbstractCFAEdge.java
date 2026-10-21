// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Preconditions;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.CfaClonerData;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public abstract class AbstractCFAEdge implements CFAEdge {

  private final CFANode predecessor;
  private final CFANode successor;
  private final String rawStatement;
  private final FileLocation fileLocation;

  private volatile CfaClonerData clonerData;
  private volatile CFAEdge originalEdge;

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
  public CFAEdge getOriginal() {
    CfaClonerData data = clonerData;
    if (data == null) {
      return this;
    }
    CFAEdge result = originalEdge;
    if (result == null) {
      result = data.getOriginal(this);
      originalEdge = result;
    }
    return result;
  }

  /** Associates this edge with the data of the CFA cloner that created it. */
  public final void setClonerData(CfaClonerData pClonerData) {
    checkState(clonerData == null, "CFA cloner data already set for edge %s", this);
    clonerData = checkNotNull(pClonerData);
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
  public final int hashCode() {
    return 31 * predecessor.hashCode() + successor.hashCode();
  }

  @Override
  public final boolean equals(Object pOther) {
    return pOther instanceof AbstractCFAEdge otherEdge
        && predecessor.equals(otherEdge.predecessor)
        && successor.equals(otherEdge.successor);
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
