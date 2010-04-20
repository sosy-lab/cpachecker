package fllesh.fql.frontend.ast;

import fllesh.fql.frontend.ast.coverage.Sequence;
import fllesh.fql.frontend.ast.coverage.States;
import fllesh.fql.frontend.ast.coverage.Edges;
import fllesh.fql.frontend.ast.coverage.Paths;
import fllesh.fql.frontend.ast.coverage.ConditionalCoverage;
import fllesh.fql.frontend.ast.filter.BasicBlockEntry;
import fllesh.fql.frontend.ast.filter.Compose;
import fllesh.fql.frontend.ast.filter.EnclosingScopes;
import fllesh.fql.frontend.ast.filter.FunctionCall;
import fllesh.fql.frontend.ast.filter.FunctionCalls;
import fllesh.fql.frontend.ast.filter.Column;
import fllesh.fql.frontend.ast.filter.ConditionEdge;
import fllesh.fql.frontend.ast.filter.ConditionGraph;
import fllesh.fql.frontend.ast.filter.DecisionEdge;
import fllesh.fql.frontend.ast.filter.FunctionEntry;
import fllesh.fql.frontend.ast.filter.FunctionExit;
import fllesh.fql.frontend.ast.filter.File;
import fllesh.fql.frontend.ast.filter.Function;
import fllesh.fql.frontend.ast.filter.Identity;
import fllesh.fql.frontend.ast.filter.Intersection;
import fllesh.fql.frontend.ast.filter.Label;
import fllesh.fql.frontend.ast.filter.Line;
import fllesh.fql.frontend.ast.filter.Expression;
import fllesh.fql.frontend.ast.filter.RegularExpression;
import fllesh.fql.frontend.ast.filter.Complement;
import fllesh.fql.frontend.ast.filter.SetMinus;
import fllesh.fql.frontend.ast.filter.Union;
import fllesh.fql.frontend.ast.pathmonitor.Alternative;
import fllesh.fql.frontend.ast.pathmonitor.Concatenation;
import fllesh.fql.frontend.ast.pathmonitor.ConditionalMonitor;
import fllesh.fql.frontend.ast.pathmonitor.LowerBound;
import fllesh.fql.frontend.ast.pathmonitor.UpperBound;
import fllesh.fql.frontend.ast.predicate.CIdentifier;
import fllesh.fql.frontend.ast.predicate.NaturalNumber;
import fllesh.fql.frontend.ast.predicate.Predicate;
import fllesh.fql.frontend.ast.predicate.Predicates;
import fllesh.fql.frontend.ast.query.Query;

public interface ASTVisitor<T> {
  // filter function expressions
  public T visit(Identity pIdentity);
  public T visit(File pFileFilter);
  public T visit(BasicBlockEntry pBasicBlockEntry);
  public T visit(ConditionEdge pConditionEdge);
  public T visit(ConditionGraph pConditionGraph);
  public T visit(DecisionEdge pDecisionEdge);
  public T visit(Line pLine);
  public T visit(FunctionCalls pCalls);
  public T visit(Column pColumn);
  public T visit(Function pFunc);
  public T visit(FunctionCall pCall);
  public T visit(FunctionEntry pEntry);
  public T visit(FunctionExit pExit);
  public T visit(Label pLabel);
  public T visit(Expression pExpression);
  public T visit(RegularExpression pRegularExpression);
  public T visit(Complement pComplement);
  public T visit(Union pUnion);
  public T visit(Compose pCompose);
  public T visit(Intersection pIntersection);
  public T visit(SetMinus pSetMinus);
  public T visit(EnclosingScopes pEnclosingScopes);
  
  // predicates
  public T visit(Predicate pPredicate);
  public T visit(CIdentifier pCIdentifier);
  public T visit(NaturalNumber pNaturalNumber);
  public T visit(Predicates pPredicates);
  
  // coverage expressions
  public T visit(States pStates);
  public T visit(Edges pEdges);
  public T visit(Paths pPaths);
  public T visit(fllesh.fql.frontend.ast.coverage.SetMinus pSetMinus);
  public T visit(fllesh.fql.frontend.ast.coverage.Union pUnion);
  public T visit(fllesh.fql.frontend.ast.coverage.Intersection pIntersection);
  public T visit(ConditionalCoverage pConditionalCoverage);
  
  public T visit(Sequence pSequence);
  
  // path monitor expressions
  public T visit(ConditionalMonitor pConditionalMonitor);
  public T visit(Alternative pAlternative);
  public T visit(Concatenation pConcatenation);
  public T visit(UpperBound pUpperBound);
  public T visit(LowerBound pLowerBound);
  
  // queries
  public T visit(Query pQuery);
}
