/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.acsl;

import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class WitnessInvariant {

  private int lineNumber;
  private CExpression exp;
  private boolean atLoopStart;
  private boolean atFunctionEntry;
  //Contains those nodes that this invariant holds for
  private List<CFANode> holdsFor;
  private final CBinaryExpressionBuilder builder;

  public WitnessInvariant(CFANode node, CExpression pExpression, CBinaryExpressionBuilder pBuilder,
                          int pLineNumber) {
    this(node, pExpression, pBuilder);
    lineNumber = pLineNumber;
  }

  public WitnessInvariant(CFANode node, CExpression pExpression, CBinaryExpressionBuilder pBuilder) {
    if(node.getNumLeavingEdges() > 0) {
      lineNumber = node.getLeavingEdge(0).getFileLocation().getStartingLineNumber();
    } else {
      lineNumber = node.getEnteringEdge(0).getFileLocation().getStartingLineNumber();
    }
    exp = pExpression;
    atLoopStart = node.isLoopStart();
    atFunctionEntry = node instanceof FunctionEntryNode;
    holdsFor = new ArrayList<>();
    holdsFor.add(node);
    builder = pBuilder;
  }

  public int getLocation() {
    return lineNumber;
  }

  public CExpression getExpression() {
    return exp;
  }

  public boolean isAtLoopStart() {
    return atLoopStart;
  }

  public boolean isAtFunctionEntry()  {
    return atFunctionEntry;
  }

  private List<CFANode> getForWhichHolds() {
    return holdsFor;
  }

  public void mergeWith(WitnessInvariant other) throws UnrecognizedCodeException {
    assert lineNumber == other.getLocation();
    if(!exp.equals(other.getExpression())) {
      //TODO: Find better way to merge expressions or change makeACSLAnnotation
      exp = builder.buildBinaryExpression(exp, other.getExpression(), BinaryOperator.BINARY_AND);
    }
    atLoopStart = atLoopStart || other.isAtLoopStart();
    atFunctionEntry = atFunctionEntry || other.isAtFunctionEntry();
    holdsFor.addAll(other.getForWhichHolds());
  }
}
