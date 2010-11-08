package org.sosy_lab.cpachecker.util.assumptions;

import org.eclipse.cdt.core.dom.ast.ASTNodeProperty;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.ExpansionOverlapsBoundaryException;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNodeLocation;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.parser.IToken;

public class DummyIASTFunctionCallExpression implements IASTFunctionCallExpression{

  private IASTExpression functionNameExpression;
  private IASTExpression parameterExpression;
  
  public DummyIASTFunctionCallExpression(IASTExpression pFunctionNameExpression, IASTExpression pParameterExpression) {
    functionNameExpression = pFunctionNameExpression;
    parameterExpression = pParameterExpression;
  }
  
  @Override
  public IASTFunctionCallExpression copy() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTExpression getFunctionNameExpression() {
    return functionNameExpression;
  }

  @Override
  public IASTExpression getParameterExpression() {
    return parameterExpression;
  }

  @Override
  public void setFunctionNameExpression(IASTExpression pExpression) {
    functionNameExpression = pExpression;
  }

  @Override
  public void setParameterExpression(IASTExpression pExpression) {
    parameterExpression = pExpression;
  }

  @Override
  public IType getExpressionType() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean accept(ASTVisitor pVisitor) {
    return false;
  }

  @Override
  public boolean contains(IASTNode pNode) {
    return false;
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
    return functionNameExpression.getRawSignature() + "(" +
    parameterExpression != null ? "[]" : parameterExpression.getRawSignature() + ");";
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
  public String toString() {
    return this.getRawSignature();
  }
}
