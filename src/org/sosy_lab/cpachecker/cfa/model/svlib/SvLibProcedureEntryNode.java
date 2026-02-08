// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.svlib;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVariableDeclarationTuple;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

public class SvLibProcedureEntryNode extends FunctionEntryNode {
  public SvLibProcedureEntryNode(
      FileLocation pFileLocation,
      @Nullable FunctionExitNode pExitNode,
      SvLibFunctionDeclaration pFunctionDefinition,
      SvLibVariableDeclarationTuple pReturnVariable) {
    super(pFileLocation, pExitNode, pFunctionDefinition, Optional.of(pReturnVariable));
  }

  @Override
  public SvLibFunctionDeclaration getFunctionDefinition() {
    return (SvLibFunctionDeclaration) super.getFunctionDefinition();
  }

  @Override
  public ImmutableList<SvLibParameterDeclaration> getFunctionParameters() {
    return getFunctionDefinition().getParameters();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Optional<SvLibVariableDeclarationTuple> getReturnVariable() {
    return (Optional<SvLibVariableDeclarationTuple>) super.getReturnVariable();
  }
}
