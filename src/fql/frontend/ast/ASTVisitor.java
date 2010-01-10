package fql.frontend.ast;

import fql.frontend.ast.coverage.Sequence;
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
import fql.frontend.ast.pathmonitor.Alternative;
import fql.frontend.ast.pathmonitor.Concatenation;
import fql.frontend.ast.pathmonitor.ConditionalMonitor;
import fql.frontend.ast.pathmonitor.LowerBound;
import fql.frontend.ast.pathmonitor.UpperBound;
import fql.frontend.ast.predicate.CIdentifier;
import fql.frontend.ast.predicate.NaturalNumber;
import fql.frontend.ast.predicate.Predicate;
import fql.frontend.ast.predicate.Predicates;
import fql.frontend.ast.query.Query;

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
  public T visit(fql.frontend.ast.coverage.SetMinus pSetMinus);
  public T visit(fql.frontend.ast.coverage.Union pUnion);
  public T visit(fql.frontend.ast.coverage.Intersection pIntersection);
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
