// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.k3;

import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

public class K3ProcedureEntryNode extends FunctionEntryNode {
  public K3ProcedureEntryNode(
      FileLocation pFileLocation,
      @Nullable FunctionExitNode pExitNode,
      K3ProcedureDeclaration pFunctionDefinition) {
    super(pFileLocation, pExitNode, pFunctionDefinition, Optional.empty());
  }

  @Override
  public List<? extends AParameterDeclaration> getFunctionParameters() {
    return getFunctionDefinition().getParameters();
  }
}
