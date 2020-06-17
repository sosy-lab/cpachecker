// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
// 
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// 
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.timedautomata;

import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

public class TCFAExitNode extends FunctionExitNode {

  private static final long serialVersionUID = 7922161957289748341L;

  public TCFAExitNode(AFunctionDeclaration pFunction) {
    super(pFunction);
  }
}
