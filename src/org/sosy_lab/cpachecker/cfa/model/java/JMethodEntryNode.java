// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.java;

import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

public final class JMethodEntryNode extends FunctionEntryNode {

  public JMethodEntryNode(
      final FileLocation pFileLocation,
      final JMethodDeclaration pMethodDefinition,
      final @Nullable FunctionExitNode pExitNode,
      final Optional<? extends JVariableDeclaration> pReturnVariable) {

    super(pFileLocation, pExitNode, pMethodDefinition, pReturnVariable);
  }

  @Override
  public JMethodDeclaration getFunctionDefinition() {
    return (JMethodDeclaration) super.getFunctionDefinition();
  }

  @Override
  public List<JParameterDeclaration> getFunctionParameters() {
    return getFunctionDefinition().getParameters();
  }

  @SuppressWarnings("unchecked") // safe because Optional is covariant
  @Override
  public Optional<? extends JVariableDeclaration> getReturnVariable() {
    return (Optional<? extends JVariableDeclaration>) super.getReturnVariable();
  }
}
