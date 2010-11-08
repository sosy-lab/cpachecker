package org.sosy_lab.cpachecker.util.assumptions;

import org.eclipse.cdt.core.dom.ast.ASTNodeProperty;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.ExpansionOverlapsBoundaryException;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNodeLocation;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IEnumerator;
import org.eclipse.cdt.core.dom.ast.IFunction;
import org.eclipse.cdt.core.dom.ast.IProblemBinding;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.IVariable;
import org.eclipse.cdt.core.parser.IToken;

public class DummyASTIdExpression implements IASTIdExpression{

  private IASTName dummyName;

  public DummyASTIdExpression(IASTName pDummyName) {
    dummyName = pDummyName;
  }

  @Override
  public IASTIdExpression copy() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTName getName() {
    return dummyName;
  }

  @Override
  public void setName(IASTName pName) {
    dummyName = pName;
  }

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

  @Override
  public boolean accept(ASTVisitor pVisitor) {
    if( pVisitor.shouldVisitExpressions ){
      switch( pVisitor.visit( this ) ){
      case ASTVisitor.PROCESS_ABORT : return false;
      case ASTVisitor.PROCESS_SKIP  : return true;
      default : break;
      }
    }

    if( dummyName != null ) if( !dummyName.accept( pVisitor ) ) return false;

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
  public boolean contains(IASTNode pNode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTNode[] getChildren() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getContainingFilename() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTFileLocation getFileLocation() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IToken getLeadingSyntax() throws ExpansionOverlapsBoundaryException,
  UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTNodeLocation[] getNodeLocations() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTNode getParent() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ASTNodeProperty getPropertyInParent() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getRawSignature() {
    return dummyName.getRawSignature();
  }

  @Override
  public IToken getSyntax() throws ExpansionOverlapsBoundaryException {
    throw new UnsupportedOperationException();
  }

  @Override
  public IToken getTrailingSyntax() throws ExpansionOverlapsBoundaryException,
  UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTTranslationUnit getTranslationUnit() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isActive() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isFrozen() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isPartOfTranslationUnitFile() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setParent(IASTNode pNode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setPropertyInParent(ASTNodeProperty pProperty) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getRoleForName(IASTName pName) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return getRawSignature();
  }

}
