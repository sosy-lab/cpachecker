package cpa.errorlocation;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;

import cfa.objectmodel.BlankEdge;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAEdgeType;
import cfa.objectmodel.CFAErrorNode;
import cfa.objectmodel.c.StatementEdge;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;
import exceptions.CPATransferException;

public class ErrorLocationTransferRelation implements TransferRelation {

  private final AbstractDomain domain;
  
  public ErrorLocationTransferRelation(AbstractDomain domain) {
    this.domain = domain;
  }
  
  @Override
  public AbstractElement getAbstractSuccessor(AbstractElement element,
                                              CFAEdge cfaEdge, Precision precision)
                                              throws CPATransferException {
    int lineNumber = cfaEdge.getSuccessor().getLineNumber();
    
    if (cfaEdge.getSuccessor() instanceof CFAErrorNode) {
      System.err.println("ERROR: Reaching error node with edge " + cfaEdge.getRawStatement() + " in line " + lineNumber + "!");
      return domain.getBottomElement();
    }
    
    switch (cfaEdge.getEdgeType()) {
      case StatementEdge:
        IASTExpression expression = ((StatementEdge)cfaEdge).getExpression();
        if (expression instanceof IASTFunctionCallExpression) {
          IASTFunctionCallExpression funcExpression = (IASTFunctionCallExpression)expression;
          
          String functionName = funcExpression.getFunctionNameExpression().getRawSignature();
          if (functionName.equals("__assert_fail")) {

            System.err.println("ERROR: Hit assertion " + expression.getRawSignature() + " in line " + lineNumber + "!");
            return domain.getBottomElement();
          }
        }
        break;
        
      case BlankEdge:
        if (cfaEdge.isJumpEdge() && cfaEdge.getRawStatement().toLowerCase().startsWith("goto: error")) {
          // This case is currently never reached, as cfaEdge.getSuccessor is a
          // CFAErrorNode in this case, but it is left here if the special
          // handling for error labels is removed from the CFA generation
          // (where it doesn't belong IMHO)
          
          System.err.println("ERROR: Reaching error node with edge " + cfaEdge.getRawStatement() + " in line " + lineNumber + "!");
          return domain.getBottomElement();
        }
        break;
    }
    
    return element;
  }

  @Override
  public List<AbstractElementWithLocation> getAllAbstractSuccessors(
                      AbstractElementWithLocation element, Precision precision)
                      throws CPAException, CPATransferException {
    throw new CPAException ("Cannot get all abstract successors from non-location domain");
  }

  @Override
  public AbstractElement strengthen(AbstractElement element,
                                    List<AbstractElement> otherElements,
                                    CFAEdge cfaEdge, Precision precision)
                                    throws CPATransferException {
    return null;
  }

}