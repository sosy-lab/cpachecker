package org.sosy_lab.cpachecker.util.assumptions;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.DOMException;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IEnumerator;
import org.eclipse.cdt.core.dom.ast.IFunction;
import org.eclipse.cdt.core.dom.ast.IProblemBinding;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.IVariable;

public class DummyASTIdExpression extends IASTIdExpression{

  public DummyASTIdExpression(final IASTName pDummyName) {
    super(null, null, null, pDummyName);
  }
  
/* this code exist, because Philipp wants it to exist
 
  @Override
  public IType getExpressionType() {
    IBinding binding = getName().resolveBinding();
    try {
      if (binding instanceof IVariable) {
        return ((IVariable)binding).getType();
      } 
      if (binding instanceof IFunction) {
        return ((IFunction)binding).getType();
      }
      if (binding instanceof IEnumerator) {
        return ((IEnumerator)binding).getType();
      }
      if (binding instanceof IProblemBinding) {
        return (IProblemBinding)binding;
      }
    } catch (DOMException e) {
      return e.getProblem();
    }
    return null;
  }
*/

  @Override
  public boolean accept(ASTVisitor pVisitor) {
    if( pVisitor.shouldVisitExpressions ){
      switch( pVisitor.visit( this ) ){
      case ASTVisitor.PROCESS_ABORT : return false;
      case ASTVisitor.PROCESS_SKIP  : return true;
      default : break;
      }
    }

    if( getName() != null ) if( !getName().accept( pVisitor ) ) return false;

    if( pVisitor.shouldVisitExpressions ){
      switch( pVisitor.leave( this ) ){
      case ASTVisitor.PROCESS_ABORT : return false;
      case ASTVisitor.PROCESS_SKIP  : return true;
      default : break;
      }
    }
    return false;
  }

  @Override
  public String getRawSignature() {
    return getName().getRawSignature();
  }

  @Override
  public String toString() {
    return getRawSignature();
  }

}
