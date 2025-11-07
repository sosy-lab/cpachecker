// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.svlib;

import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

public class SvLibProcedureEntryNode extends FunctionEntryNode {
  public SvLibProcedureEntryNode(
      FileLocation pFileLocation,
      @Nullable FunctionExitNode pExitNode,
      SvLibProcedureDeclaration pFunctionDefinition) {
    super(pFileLocation, pExitNode, pFunctionDefinition, Optional.empty());
  }

  @Override
  public SvLibProcedureDeclaration getFunctionDefinition() {
    return (SvLibProcedureDeclaration) super.getFunctionDefinition();
  }

  @Override
  public List<SvLibParameterDeclaration> getFunctionParameters() {
    return getFunctionDefinition().getParameters();
  }
}
