package org.sosy_lab.cpachecker.cfa.ast;

public abstract class ASTVisitor {

  @Deprecated
  protected static final int PROCESS_CONTINUE = 0;

  @Deprecated
  protected boolean          pVisitNodes;
  @Deprecated
  protected boolean          shouldVisitDeclarations;
  @Deprecated
  protected boolean          shouldVisitDeclarators;
  @Deprecated
  protected boolean          shouldVisitDeclSpecifiers;
  @Deprecated
  protected boolean          shouldVisitEnumerators;
  @Deprecated
  protected boolean          shouldVisitExpressions;
  @Deprecated
  protected boolean          shouldVisitInitializers;
  @Deprecated
  protected boolean          shouldVisitNames;
  @Deprecated
  protected boolean          shouldVisitParameterDeclarations;
  @Deprecated
  protected boolean          shouldVisitProblems;
  @Deprecated
  protected boolean          shouldVisitStatements;
  @Deprecated
  protected boolean          shouldVisitTranslationUnit;
  @Deprecated
  protected boolean          shouldVisitTypeIds;

  @Deprecated
  public ASTVisitor() {
  }

  @Deprecated
  public ASTVisitor(boolean pVisitNodes) {
    this.pVisitNodes = pVisitNodes;
  }

  @Deprecated
  public abstract int visit(IASTDeclaration pDeclaration);
  @Deprecated
  public abstract int leave(IASTDeclaration pDeclaration);
  @Deprecated 
  public abstract int visit(IASTStatement pStatement);
  @Deprecated 
  public abstract int leave(IASTStatement pStatement);
  @Deprecated 
  public abstract int visit(IASTProblem pProblem);
}
