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

import com.google.errorprone.annotations.DoNotCall;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;

public final class FunctionExitNode extends CFANode {

  private static final long serialVersionUID = -7883542777389959334L;
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
  public void addEnteringSummaryEdge(FunctionSummaryEdge pEdge) {
    throw new AssertionError("function-exit nodes cannot have summary eges");
  }

  @Override
  public void addLeavingSummaryEdge(FunctionSummaryEdge pEdge) {
    throw new AssertionError("function-exit nodes cannot have summary eges");
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
}
