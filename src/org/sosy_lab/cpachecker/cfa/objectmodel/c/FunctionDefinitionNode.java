/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.objectmodel.c;

import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionDefinition;
import org.sosy_lab.cpachecker.cfa.ast.IASTParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionExitNode;

import com.google.common.collect.ImmutableList;

public class FunctionDefinitionNode extends CFAFunctionDefinitionNode {

  private final IASTFunctionDefinition                   functionDefinition;
  private final List<IASTParameterDeclaration>           parameters;
  private final List<String>                             parameterNames;

  public FunctionDefinitionNode(final int lineNumber,
      final String functionName,
      final IASTFunctionDefinition functionDefinition,
      final CFAFunctionExitNode exitNode,
      final List<IASTParameterDeclaration> parameters,
      final List<String> parameterNames) {
    super(lineNumber, functionName, exitNode);
    this.functionDefinition = functionDefinition;
    this.parameters = ImmutableList.copyOf(parameters);
    this.parameterNames = ImmutableList.copyOf(parameterNames);
  }

  public FunctionDefinitionNode(FunctionDefinitionNode other){
    super(other.getLineNumber(), other.getFunctionName(), other.getExitNode());
    this.functionDefinition = other.functionDefinition;
    this.parameters = other.parameters;
    this.parameterNames = other.parameterNames;
  }

  public IASTFunctionDefinition getFunctionDefinition() {
    return functionDefinition;
  }

  public List<String> getFunctionParameterNames() {
    return parameterNames;
  }

  public List<IASTParameterDeclaration> getFunctionParameters() {
    return parameters;
  }
}
