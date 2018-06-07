/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa.model.js;

import com.google.common.base.Optional;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

public class JSFunctionEntryNode extends FunctionEntryNode {

  private static final long serialVersionUID = 5418184531017520116L;

  public JSFunctionEntryNode(
      final FileLocation pFileLocation,
      final JSFunctionDeclaration pFunctionDefinition,
      final FunctionExitNode pExitNode,
      final Optional<JSVariableDeclaration> pReturnVariable) {

    super(
        pFileLocation,
        pFunctionDefinition.getQualifiedName(),
        pExitNode,
        pFunctionDefinition,
        pReturnVariable);
  }

  @Override
  public JSFunctionDeclaration getFunctionDefinition() {
    return (JSFunctionDeclaration) super.getFunctionDefinition();
  }

  @Override
  public List<JSParameterDeclaration> getFunctionParameters() {
    return getFunctionDefinition().getParameters();
  }

  @SuppressWarnings("unchecked") // safe because Optional is covariant
  @Override
  public Optional<JSVariableDeclaration> getReturnVariable() {
    return (Optional<JSVariableDeclaration>) super.getReturnVariable();
  }
}
