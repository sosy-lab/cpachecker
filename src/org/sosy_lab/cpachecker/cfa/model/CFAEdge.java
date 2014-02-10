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


import org.sosy_lab.cpachecker.cfa.ast.IAstNode;

import com.google.common.base.Optional;

public interface CFAEdge {

  public CFAEdgeType getEdgeType();

  public CFANode getPredecessor();

  public CFANode getSuccessor();

  public Optional<? extends IAstNode> getRawAST();

  public int getLineNumber();

  /**
   * Returns the part of the original input file from which this edge was
   * produced. This should usually be a single statement / declaration etc.
   * (what normal programmers write on one line).
   *
   * The result does not reflect any post-processing and simplification which
   * was done after parsing and thus may show different C code than the result
   * of {@link #getCode()}.
   */
  public String getRawStatement();

  /**
   * Returns a string representation of the code attached to this edge.
   * If there is no such representation, the method returns the empty string.
   */
  public String getCode();

  /**
   * Returns a representation of this edge which is meant to be shown to the
   * user. This description should only contain the code content of the edge
   * (i.e., no meta-information like line number and predecessor/successor) and
   * thus usually be similar to the output of {@link #getCode()}.
   */
  public String getDescription();

  /**
   * Returns a full representation of this edge (including as many information
   * as possible) which is meant to be shown to the user.
   */
  @Override
  public String toString();
}
