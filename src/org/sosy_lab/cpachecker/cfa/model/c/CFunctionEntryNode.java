// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.c;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.KnownFunctionBehavior;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

public final class CFunctionEntryNode extends FunctionEntryNode {

  /**
   * The known behavior of this function, if it behaves like a well-known function (see {@link
   * KnownFunctionBehavior}). Set during CFA creation, if the detection is enabled.
   */
  private @Nullable KnownFunctionBehavior knownBehavior = null;

  public CFunctionEntryNode(
      final FileLocation pFileLocation,
      final CFunctionDeclaration pFunctionDefinition,
      final @Nullable FunctionExitNode pExitNode,
      final Optional<CVariableDeclaration> pReturnVariable) {

    super(pFileLocation, pExitNode, pFunctionDefinition, pReturnVariable);
  }

  @Override
  public CFunctionDeclaration getFunctionDefinition() {
    return (CFunctionDeclaration) super.getFunctionDefinition();
  }

  /**
   * Returns the known behavior of this function, if it is known to behave like a well-known
   * function (e.g., it is a user-defined implementation of {@code memset}).
   */
  public Optional<KnownFunctionBehavior> getKnownBehavior() {
    return Optional.ofNullable(knownBehavior);
  }

  /**
   * Stores that this function behaves like a well-known function. Only to be used during CFA
   * creation, and when copying this node.
   */
  public void setKnownBehavior(@Nullable KnownFunctionBehavior pKnownBehavior) {
    knownBehavior = pKnownBehavior;
  }

  @Override
  public ImmutableList<CParameterDeclaration> getFunctionParameters() {
    return getFunctionDefinition().getParameters();
  }

  @SuppressWarnings("unchecked") // safe because Optional is covariant
  @Override
  public Optional<CVariableDeclaration> getReturnVariable() {
    return (Optional<CVariableDeclaration>) super.getReturnVariable();
  }

  @Override
  @SuppressWarnings("unchecked")
  public final FluentIterable<CFunctionCallEdge> getEnteringCallEdges() {
    return (FluentIterable<CFunctionCallEdge>) super.getEnteringCallEdges();
  }
}
