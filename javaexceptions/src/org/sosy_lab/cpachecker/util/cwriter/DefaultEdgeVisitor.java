// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter;

import java.util.Deque;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

public class DefaultEdgeVisitor implements EdgeVisitor {

  protected PathTranslator translator;

  protected DefaultEdgeVisitor(PathTranslator t) {
    translator = t;
  }

  @Override
  public void visit(ARGState childElement, CFAEdge edge, Deque<FunctionBody> functionStack) {
    translator.processEdge(childElement, edge, functionStack);
  }
}
