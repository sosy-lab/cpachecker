// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
// 
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// 
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.timedautomata;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

public class TCFAEntryNode extends FunctionEntryNode {

  public TCFAEntryNode(
      FileLocation pFileLocation,
      FunctionExitNode pExitNode,
      AFunctionDeclaration pFunctionDefinition) {
    super(pFileLocation, pExitNode, pFunctionDefinition, Optional.absent());
  }

  private static final long serialVersionUID = 1L;

  @Override
  public List<? extends AParameterDeclaration> getFunctionParameters() {
    return ImmutableList.of();
  }
}
