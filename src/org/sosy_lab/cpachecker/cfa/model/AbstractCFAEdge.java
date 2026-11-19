// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Preconditions;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public abstract class AbstractCFAEdge implements CFAEdge {

  private final CFANode predecessor;
  private final CFANode successor;
  private final String rawStatement;
  private final FileLocation fileLocation;

  /** The edge this edge is a per-thread clone of, or null (see {@link CFAEdge#getOriginal()}). */
  private @Nullable CFAEdge original = null;

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
    return original == null ? this : original;
  }

  /**
   * Marks this edge as the per-thread clone of {@code pOriginal}. Only for the cloner that created
   * this edge, directly after creating it (see {@link CFAEdge#getOriginal()}).
   */
  public final void setOriginal(CFAEdge pOriginal) {
    checkState(original == null, "Original already set for edge %s", this);
    checkArgument(pOriginal != this && pOriginal.getOriginal() == pOriginal);
    original = pOriginal;
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
