package org.sosy_lab.cpachecker.cfa.ast;

import org.eclipse.cdt.core.dom.ILinkage;
import org.eclipse.cdt.core.dom.IName;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTNodeSelector;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIncludeStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorMacroDefinition;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorMacroExpansion;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IASTProblem;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.INodeFactory;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexFileSet;
import org.eclipse.cdt.core.parser.ParserLanguage;

public class IASTTranslationUnit extends IASTNode implements
    org.eclipse.cdt.core.dom.ast.IASTTranslationUnit {

  public IASTTranslationUnit(final String pRawSignature,
      final IASTFileLocation pFileLocation) {
    super(pRawSignature, pFileLocation);
    // TODO Auto-generated constructor stub
  }

  @Override
  @Deprecated
  public void addDeclaration(
      final org.eclipse.cdt.core.dom.ast.IASTDeclaration pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTTranslationUnit copy() {
    throw new UnsupportedOperationException();
  }

  @Override
  public org.eclipse.cdt.core.dom.ast.IASTFileLocation flattenLocationsToFile(
     final org.eclipse.cdt.core.dom.ast.IASTNodeLocation[] pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void freeze() {
    throw new UnsupportedOperationException();
  }

  @Override
  public INodeFactory getASTNodeFactory() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTPreprocessorStatement[] getAllPreprocessorStatements() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTPreprocessorMacroDefinition[] getBuiltinMacroDefinitions() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTComment[] getComments() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getContainingFilename(int pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTDeclaration[] getDeclarations() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IName[] getDeclarations(IBinding pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTName[] getDeclarationsInAST(IBinding pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public IName[] getDefinitions(IBinding pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTName[] getDefinitionsInAST(IBinding pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public IDependencyTree getDependencyTree() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getFilePath() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTPreprocessorIncludeStatement[] getIncludeDirectives() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IIndex getIndex() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IIndexFileSet getIndexFileSet() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ILinkage getLinkage() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTPreprocessorMacroDefinition[] getMacroDefinitions() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTPreprocessorMacroExpansion[] getMacroExpansions() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTNodeSelector getNodeSelector(String pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ParserLanguage getParserLanguage() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTProblem[] getPreprocessorProblems() {
    throw new UnsupportedOperationException();

  }

  @Override
  public int getPreprocessorProblemsCount() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTName[] getReferences(IBinding pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public IScope getScope() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isHeaderUnit() {
    throw new UnsupportedOperationException();
  }

  @Override
  public org.eclipse.cdt.core.dom.ast.IASTNode selectNodeForLocation(
      String pArg0, int pArg1, int pArg2) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setIndex(IIndex pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setIsHeaderUnit(boolean pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTDeclaration[] getDeclarations(boolean pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object getAdapter(Class pArg0) {
    throw new UnsupportedOperationException();
  }
}