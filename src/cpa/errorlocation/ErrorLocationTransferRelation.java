package cpa.errorlocation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAErrorNode;
import cfa.objectmodel.c.StatementEdge;
import cpa.common.CPAchecker;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPATransferException;

public class ErrorLocationTransferRelation implements TransferRelation {

  private static Set<Integer> messages = new HashSet<Integer>();

  private final AbstractElement errorElement;
  
  public ErrorLocationTransferRelation(AbstractElement errorElement) {
    this.errorElement = errorElement;
  }
  

  public static void addError(String message, CFAEdge edge) {
    Integer lineNumber = edge.getSuccessor().getLineNumber();
    
    if (!messages.contains(lineNumber)) {
      messages.add(lineNumber);
      CPAchecker.logger.log(Level.WARNING, "ERROR: " + message + " in line " + lineNumber + "!");
    }
  }
  
  private AbstractElement getAbstractSuccessor(AbstractElement element,
                                              CFAEdge cfaEdge, Precision precision)
                                              throws CPATransferException {
    
    if (cfaEdge.getSuccessor() instanceof CFAErrorNode) {
      addError("Reaching error node with edge " + cfaEdge.getRawStatement(), cfaEdge);
      return errorElement;
    }
    
    switch (cfaEdge.getEdgeType()) {
      case StatementEdge:
        IASTExpression expression = ((StatementEdge)cfaEdge).getExpression();
        if (expression instanceof IASTFunctionCallExpression) {
          IASTFunctionCallExpression funcExpression = (IASTFunctionCallExpression)expression;
          
          String functionName = funcExpression.getFunctionNameExpression().getRawSignature();
          if (functionName.equals("__assert_fail")) {

            addError("Hit assertion " + expression.getRawSignature(), cfaEdge);
            return errorElement;
          }
        }
        break;
        
      case BlankEdge:
        if (cfaEdge.isJumpEdge() && cfaEdge.getRawStatement().toLowerCase().startsWith("goto: error")) {
          // This case is currently never reached, as cfaEdge.getSuccessor is a
          // CFAErrorNode in this case, but it is left here if the special
          // handling for error labels is removed from the CFA generation
          // (where it doesn't belong IMHO)
          
          addError("Reaching error node with edge " + cfaEdge.getRawStatement(), cfaEdge);
          return errorElement;
        }
        break;
    }
    
    return element;
  }

  @Override
  public Collection<AbstractElement> getAbstractSuccessors(
                      AbstractElement element, Precision precision, CFAEdge cfaEdge)
                      throws CPATransferException {
    return Collections.singleton(getAbstractSuccessor(element, cfaEdge, precision));
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(AbstractElement element,
                                    List<AbstractElement> otherElements,
                                    CFAEdge cfaEdge, Precision precision)
                                    throws CPATransferException {
    return null;
  }

}