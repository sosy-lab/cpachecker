/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cfa;

import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class DummyCFAEdge implements CFAEdge {

  private static final long serialVersionUID = 1L;
  private final CFANode successor;
  private final CFANode predecessor;

  public DummyCFAEdge(CFANode pPredecessor, CFANode pSuccessor) {
    predecessor = pPredecessor;
    successor = pSuccessor;
  }

  @Override
  public String getRawStatement() {
    return "";
  }

  @Override
  public com.google.common.base.Optional<? extends AAstNode> getRawAST() {
    return com.google.common.base.Optional.absent();
  }

  @Override
  public CFANode getPredecessor() {
    return predecessor;
  }

  @Override
  public CFANode getSuccessor() {
    return successor;
  }

  @Override
  public int getLineNumber() {
    return getFileLocation().getStartingLineNumber();
  }

  @Override
  public FileLocation getFileLocation() {
    return FileLocation.DUMMY;
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.BlankEdge;
  }

  @Override
  public String getDescription() {
    return "";
  }

  @Override
  public String getCode() {
    return "";
  }
}