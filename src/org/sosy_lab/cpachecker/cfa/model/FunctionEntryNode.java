/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.model;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

import com.google.common.collect.ImmutableList;


public  class FunctionEntryNode extends CFANode {

  private final FileLocation location;
  private final AFunctionDeclaration functionDefinition;
  private final List<String> parameterNames;

  // Check if call edges are added in the second pass
  private final FunctionExitNode exitNode;


  protected FunctionEntryNode(final FileLocation pFileLocation,
      final AFunctionDeclaration pFunctionDefinition,
      final FunctionExitNode pExitNode,
      final List<String> pParameterNames) {

    super(pFunctionDefinition.getName());
    location = checkNotNull(pFileLocation);
    functionDefinition = pFunctionDefinition;
    parameterNames = ImmutableList.copyOf(pParameterNames);
    exitNode = pExitNode;
  }

  public FunctionEntryNode(final FileLocation pFileLocation, String pFunctionName,
      FunctionExitNode pExitNode, final AFunctionDeclaration pFunctionDefinition,
      final List<String> pParameterNames) {

    super(pFunctionName);
    location = checkNotNull(pFileLocation);
    functionDefinition = pFunctionDefinition;
    parameterNames = ImmutableList.copyOf(pParameterNames);
    exitNode = pExitNode;
  }

  public FileLocation getFileLocation() {
    return location;
  }

  public FunctionExitNode getExitNode() {
    return exitNode;
  }

  public AFunctionDeclaration getFunctionDefinition() {
    return functionDefinition;
  }

  public List<String> getFunctionParameterNames() {
    return parameterNames;
  }

  public List<? extends AParameterDeclaration> getFunctionParameters() {
    //return functionDefinition.getType().getParameters();
    return null;
  }

}