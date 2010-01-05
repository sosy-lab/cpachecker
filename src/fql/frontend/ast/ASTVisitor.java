package fql.frontend.ast;

import fql.frontend.ast.filter.BasicBlockEntry;
import fql.frontend.ast.filter.FunctionCall;
import fql.frontend.ast.filter.FunctionCalls;
import fql.frontend.ast.filter.Column;
import fql.frontend.ast.filter.ConditionEdge;
import fql.frontend.ast.filter.ConditionGraph;
import fql.frontend.ast.filter.DecisionEdge;
import fql.frontend.ast.filter.FunctionEntry;
import fql.frontend.ast.filter.FunctionExit;
import fql.frontend.ast.filter.File;
import fql.frontend.ast.filter.Function;
import fql.frontend.ast.filter.Identity;
import fql.frontend.ast.filter.Label;
import fql.frontend.ast.filter.Line;

public interface ASTVisitor {
  public void visit(Identity pIdentity);
  public void visit(File pFileFilter);
  public void visit(BasicBlockEntry pBasicBlockEntry);
  public void visit(ConditionEdge pConditionEdge);
  public void visit(ConditionGraph pConditionGraph);
  public void visit(DecisionEdge pDecisionEdge);
  public void visit(Line pLine);
  public void visit(FunctionCalls pCalls);
  public void visit(Column pColumn);
  public void visit(Function pFunc);
  public void visit(FunctionCall pCall);
  public void visit(FunctionEntry pEntry);
  public void visit(FunctionExit pExit);
  public void visit(Label pLabel);
}
