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
import com.google.common.collect.FluentIterable;
import com.google.errorprone.annotations.DoNotCall;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;

public final class FunctionExitNode extends CFANode {

  private FunctionEntryNode entryNode;

  public FunctionExitNode(AFunctionDeclaration pFunction) {
    super(pFunction);
  }

  public void setEntryNode(FunctionEntryNode pEntryNode) {
    checkState(entryNode == null);
    entryNode = checkNotNull(pEntryNode);
  }

  public FunctionEntryNode getEntryNode() {
    checkState(entryNode != null);
    return entryNode;
  }

  @Override
  public void addLeavingEdge(CFAEdge pLeavingEdge) {
    checkArgument(pLeavingEdge instanceof FunctionReturnEdge || pLeavingEdge instanceof BlankEdge);
    super.addLeavingEdge(pLeavingEdge);
  }

  @Override
  public CFAEdge getLeavingEdge(int pIndex) {
    Preconditions.checkState(
        super.getLeavingEdge(pIndex) instanceof BlankEdge
            || super.getLeavingEdge(pIndex) instanceof FunctionReturnEdge);
    return super.getLeavingEdge(pIndex);
  }

  /**
   * @deprecated use {@link #getLeavingReturnEdges()} instead, it has a stronger return type
   */
  @Deprecated
  @Override
  public FluentIterable<CFAEdge> getLeavingEdges() {
    return super.getLeavingEdges();
  }

  @SuppressWarnings("unchecked")
  public FluentIterable<FunctionReturnEdge> getLeavingReturnEdges() {
    // TODO this case is broken since e2a8384a (cf. #1319)
    return (FluentIterable<FunctionReturnEdge>) (FluentIterable<?>) getLeavingEdges();
  }

  @Override
  public void addEnteringSummaryEdge(FunctionSummaryEdge pEdge) {
    throw new AssertionError("function-exit nodes cannot have summary edges");
  }

  @Override
  public void addLeavingSummaryEdge(FunctionSummaryEdge pEdge) {
    throw new AssertionError("function-exit nodes cannot have summary edges");
  }

  @Override
  @Deprecated
  @DoNotCall // safe to call but useless
  public @Nullable FunctionSummaryEdge getEnteringSummaryEdge() {
    return null;
  }

  @Override
  @Deprecated
  @DoNotCall // safe to call but useless
  public @Nullable FunctionSummaryEdge getLeavingSummaryEdge() {
    return null;
  }

  /**
   * @deprecated use {@link #getEnteringEdges()} instead, there is no summary edge anyway
   */
  @Override
  @Deprecated
  @DoNotCall
  public FluentIterable<CFAEdge> getAllEnteringEdges() {
    return getEnteringEdges();
  }

  /**
   * @deprecated use {@link #getLeavingReturnEdges()} instead, there is no summary edge anyway
   */
  @Override
  @Deprecated
  @DoNotCall
  public FluentIterable<CFAEdge> getAllLeavingEdges() {
    return getLeavingEdges();
  }
}
