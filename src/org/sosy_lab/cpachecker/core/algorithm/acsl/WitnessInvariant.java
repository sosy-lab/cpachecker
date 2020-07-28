// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
  private final List<CFANode> holdsFor = new ArrayList<>();
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
