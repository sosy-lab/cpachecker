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
import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.DoNotCall;
import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

// TODO should be sealed but cannot permit subclasses in other packages until we use modules
public abstract non-sealed class FunctionEntryNode extends CFANode {

  private final FileLocation location;
  private final AFunctionDeclaration functionDefinition;
  private final @Nullable AVariableDeclaration returnVariable;

  // Check if call edges are added in the second pass
  // Some function entry nodes do not have a corresponding function exit node that is also part of
  // the CFA. If a function never returns, because it always aborts the program or always executes
  // an infinite loop, the CFA doesn't contain an exit node for the function. If this is the case,
  // this field is null.
  private @Nullable FunctionExitNode exitNode;

  // Cache for getEnteringEdges that is backed by an ImmutableSet.
  // This makes getEnteringEdges().contains(edge) much faster if there are many entering edges
  // (which can happen for FunctionEntryNodes). Cf. #1388
  private FluentIterable<CFAEdge> enteringEdges;

  protected FunctionEntryNode(
      final FileLocation pFileLocation,
      @Nullable FunctionExitNode pExitNode,
      final AFunctionDeclaration pFunctionDefinition,
      final Optional<? extends AVariableDeclaration> pReturnVariable) {

    super(pFunctionDefinition);
    location = checkNotNull(pFileLocation);
    functionDefinition = pFunctionDefinition;
    exitNode = pExitNode;
    returnVariable = pReturnVariable.orElse(null);
  }

  public FileLocation getFileLocation() {
    return location;
  }

  /**
   * Returns an optional containing the corresponding function exit node for this entry node, if it
   * exists.
   *
   * <p>Some function entry nodes do not have a corresponding function exit node. If a function
   * never returns, because it always aborts the program or always executes an infinite loop, the
   * CFA doesn't contain an exit node for the function.
   *
   * @return If this function entry node has a corresponding function exit node, an optional
   *     containing the function exit node is returned. Otherwise, if the function entry node does
   *     not have a corresponding function exit node, {@code Optional.empty()} is returned.
   */
  public Optional<FunctionExitNode> getExitNode() {
    return Optional.ofNullable(exitNode);
  }

  /**
   * Removes the corresponding function exit node from this entry node.
   *
   * <p>Only call this method if the function exit node isn't part of the CFA. Do not call this
   * method outside CFA construction.
   */
  public void removeExitNode() {
    exitNode = null;
  }

  public AFunctionDeclaration getFunctionDefinition() {
    return functionDefinition;
  }

  public List<String> getFunctionParameterNames() {
    return Lists.transform(functionDefinition.getParameters(), AParameterDeclaration::getName);
  }

  public abstract ImmutableList<? extends AParameterDeclaration> getFunctionParameters();

  /**
   * Return a declaration for a pseudo variable that can be used to store the return value of this
   * function (if it has one). This variable is the same as the one used by {@link
   * AReturnStatement#asAssignment()}.
   */
  public Optional<? extends AVariableDeclaration> getReturnVariable() {
    return Optional.ofNullable(returnVariable);
  }

  @Override
  public void addEnteringEdge(CFAEdge pEnteringEdge) {
    checkArgument(pEnteringEdge instanceof FunctionCallEdge);
    enteringEdges = null;
    super.addEnteringEdge(pEnteringEdge);
  }

  @Override
  public void removeEnteringEdge(CFAEdge pEdge) {
    enteringEdges = null;
    super.removeEnteringEdge(pEdge);
  }

  @Override
  public FunctionCallEdge getEnteringEdge(int pIndex) {
    return (FunctionCallEdge) super.getEnteringEdge(pIndex);
  }

  /**
   * @deprecated use {@link #getEnteringCallEdges()} instead, it has a stronger return type
   */
  @Deprecated
  @Override
  public final FluentIterable<CFAEdge> getEnteringEdges() {
    if (enteringEdges == null) {
      // In general edges in sets are problematic to do their equals(),
      // but for function-call edges there should never be two edges with the same predecessor and
      // successor nodes.
      enteringEdges = from(super.getEnteringEdges().toSet());
      assert super.getEnteringEdges().size() == enteringEdges.size();
    }
    return enteringEdges;
  }

  @SuppressWarnings("unchecked")
  public FluentIterable<? extends FunctionCallEdge> getEnteringCallEdges() {
    return (FluentIterable<FunctionCallEdge>) (FluentIterable<?>) getEnteringEdges();
  }

  @Override
  public final void addEnteringSummaryEdge(FunctionSummaryEdge pEdge) {
    throw new AssertionError("function-entry nodes cannot have summary edges");
  }

  @Override
  public final void addLeavingSummaryEdge(FunctionSummaryEdge pEdge) {
    throw new AssertionError("function-entry nodes cannot have summary edges");
  }

  @Override
  @Deprecated
  @DoNotCall // safe to call but useless
  public final @Nullable FunctionSummaryEdge getEnteringSummaryEdge() {
    return null;
  }

  @Override
  @Deprecated
  @DoNotCall // safe to call but useless
  public final @Nullable FunctionSummaryEdge getLeavingSummaryEdge() {
    return null;
  }

  /**
   * @deprecated use {@link #getEnteringCallEdges()} instead, there is no summary edge anyway
   */
  @Override
  @Deprecated
  @DoNotCall
  public final FluentIterable<CFAEdge> getAllEnteringEdges() {
    return getEnteringEdges();
  }

  /**
   * @deprecated use {@link #getLeavingEdges()} instead, there is no summary edge anyway
   */
  @Override
  @Deprecated
  @DoNotCall
  public final FluentIterable<CFAEdge> getAllLeavingEdges() {
    return getLeavingEdges();
  }
}
