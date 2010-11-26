package org.sosy_lab.cpachecker.fllesh.cfa;

import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;

public class FlleShAssumeEdge extends AssumeEdge {

  public FlleShAssumeEdge(CFANode pNode, IASTExpression pExpression) {
    super(pExpression.getRawSignature(), FlleShCFANode.FlleShLineNumber, new FlleShCFANode(pNode), new FlleShCFANode(pNode), pExpression, true);
  }

}
