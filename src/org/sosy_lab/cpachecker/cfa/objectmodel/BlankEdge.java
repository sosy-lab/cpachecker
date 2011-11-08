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
package org.sosy_lab.cpachecker.cfa.objectmodel;

public class BlankEdge extends AbstractCFAEdge {

  private final boolean jumpEdge;

  public BlankEdge(String pRawStatement, int pLineNumber, CFANode pPredecessor,
      CFANode pSuccessor) {

    this(pRawStatement, pLineNumber, pPredecessor, pSuccessor, false);
  }

  public BlankEdge(String pRawStatement, int pLineNumber, CFANode pPredecessor,
      CFANode pSuccessor, boolean pJumpEdge) {

    super(pRawStatement, pLineNumber, pPredecessor, pSuccessor);
    jumpEdge = pJumpEdge;
  }

  /**
   * Gives information whether this edge is a jump as produced by a goto,
   * continue and break statements.
   */
  @Override
  public boolean isJumpEdge() {
    return jumpEdge;
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.BlankEdge;
  }
}
