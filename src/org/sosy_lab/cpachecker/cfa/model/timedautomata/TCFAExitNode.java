package org.sosy_lab.cpachecker.cfa.model.timedautomata;

import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

public class TCFAExitNode extends FunctionExitNode {

  private static final long serialVersionUID = 7922161957289748341L;

  public TCFAExitNode(AFunctionDeclaration pFunction) {
    super(pFunction);
  }
}
