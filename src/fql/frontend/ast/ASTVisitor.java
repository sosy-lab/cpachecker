package fql.frontend.ast;

import fql.frontend.ast.coverage.States;
import fql.frontend.ast.coverage.Edges;
import fql.frontend.ast.coverage.Paths;
import fql.frontend.ast.coverage.ConditionalCoverage;
import fql.frontend.ast.filter.BasicBlockEntry;
import fql.frontend.ast.filter.Compose;
import fql.frontend.ast.filter.EnclosingScopes;
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
import fql.frontend.ast.filter.Intersection;
import fql.frontend.ast.filter.Label;
import fql.frontend.ast.filter.Line;
import fql.frontend.ast.filter.Expression;
import fql.frontend.ast.filter.RegularExpression;
import fql.frontend.ast.filter.Complement;
import fql.frontend.ast.filter.SetMinus;
import fql.frontend.ast.filter.Union;
import fql.frontend.ast.predicate.CIdentifier;
import fql.frontend.ast.predicate.NaturalNumber;
import fql.frontend.ast.predicate.Predicate;
import fql.frontend.ast.predicate.Predicates;

public interface ASTVisitor {
  // filter function expressions
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
  public void visit(Expression pExpression);
  public void visit(RegularExpression pRegularExpression);
  public void visit(Complement pComplement);
  public void visit(Union pUnion);
  public void visit(Compose pCompose);
  public void visit(Intersection pIntersection);
  public void visit(SetMinus pSetMinus);
  public void visit(EnclosingScopes pEnclosingScopes);
  
  // predicates
  public void visit(Predicate pPredicate);
  public void visit(CIdentifier pCIdentifier);
  public void visit(NaturalNumber pNaturalNumber);
  public void visit(Predicates pPredicates);
  
  // coverage expressions
  public void visit(States pStates);
  public void visit(Edges pEdges);
  public void visit(Paths pPaths);
  public void visit(fql.frontend.ast.coverage.SetMinus pPaths);
  public void visit(fql.frontend.ast.coverage.Union pUnion);
  public void visit(fql.frontend.ast.coverage.Intersection pIntersection);
  public void visit(ConditionalCoverage pConditionalCoverage);
}
