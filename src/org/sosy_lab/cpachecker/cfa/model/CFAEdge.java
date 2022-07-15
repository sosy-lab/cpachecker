// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model;

import java.io.Serializable;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public interface CFAEdge extends Serializable {

  CFAEdgeType getEdgeType();

  CFANode getPredecessor();

  CFANode getSuccessor();

  Optional<AAstNode> getRawAST();

  int getLineNumber();

  FileLocation getFileLocation();

  /**
   * Returns the part of the original input file from which this edge was produced. This should
   * usually be a single statement / declaration etc. (what normal programmers write on one line).
   *
   * <p>The result does not reflect any post-processing and simplification which was done after
   * parsing and thus may show different C code than the result of {@link #getCode()}.
   */
  String getRawStatement();

  /**
   * Returns a string representation of the code attached to this edge. If there is no such
   * representation, the method returns the empty string.
   */
  String getCode();

  /**
   * Returns a representation of this edge which is meant to be shown to the user. This description
   * should only contain the code content of the edge (i.e., no meta-information like line number
   * and predecessor/successor) and thus usually be similar to the output of {@link #getCode()}.
   */
  String getDescription();

  /**
   * Returns a full representation of this edge (including as many information as possible) which is
   * meant to be shown to the user.
   */
  @Override
  String toString();
}
