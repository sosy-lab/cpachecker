// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.c;

import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

public class CFunctionEntryNode extends FunctionEntryNode {

  private static final long serialVersionUID = -730687961628864953L;

  public CFunctionEntryNode(
      final FileLocation pFileLocation,
      final CFunctionDeclaration pFunctionDefinition,
      final FunctionExitNode pExitNode,
      final Optional<CVariableDeclaration> pReturnVariable) {

    super(pFileLocation, pExitNode, pFunctionDefinition, pReturnVariable);
  }

  @Override
  public CFunctionDeclaration getFunctionDefinition() {
    return (CFunctionDeclaration) super.getFunctionDefinition();
  }

  @Override
  public List<CParameterDeclaration> getFunctionParameters() {
    return getFunctionDefinition().getParameters();
  }

  @SuppressWarnings("unchecked") // safe because Optional is covariant
  @Override
  public Optional<CVariableDeclaration> getReturnVariable() {
    return (Optional<CVariableDeclaration>) super.getReturnVariable();
  }
}
