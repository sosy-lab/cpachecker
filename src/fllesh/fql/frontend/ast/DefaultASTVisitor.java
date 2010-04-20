package fllesh.fql.frontend.ast;

import fllesh.fql.frontend.ast.coverage.ConditionalCoverage;
import fllesh.fql.frontend.ast.coverage.Edges;
import fllesh.fql.frontend.ast.coverage.Paths;
import fllesh.fql.frontend.ast.coverage.Sequence;
import fllesh.fql.frontend.ast.coverage.States;
import fllesh.fql.frontend.ast.filter.BasicBlockEntry;
import fllesh.fql.frontend.ast.filter.Column;
import fllesh.fql.frontend.ast.filter.Complement;
import fllesh.fql.frontend.ast.filter.Compose;
import fllesh.fql.frontend.ast.filter.ConditionEdge;
import fllesh.fql.frontend.ast.filter.ConditionGraph;
import fllesh.fql.frontend.ast.filter.DecisionEdge;
import fllesh.fql.frontend.ast.filter.EnclosingScopes;
import fllesh.fql.frontend.ast.filter.Expression;
import fllesh.fql.frontend.ast.filter.File;
import fllesh.fql.frontend.ast.filter.Function;
import fllesh.fql.frontend.ast.filter.FunctionCall;
import fllesh.fql.frontend.ast.filter.FunctionCalls;
import fllesh.fql.frontend.ast.filter.FunctionEntry;
import fllesh.fql.frontend.ast.filter.FunctionExit;
import fllesh.fql.frontend.ast.filter.Identity;
import fllesh.fql.frontend.ast.filter.Intersection;
import fllesh.fql.frontend.ast.filter.Label;
import fllesh.fql.frontend.ast.filter.Line;
import fllesh.fql.frontend.ast.filter.RegularExpression;
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

public class DefaultASTVisitor<T> implements ASTVisitor<T> {

  @Override
  public T visit(Identity pIdentity) {
    throw new UnsupportedOperationException("The method visit(Identity pIdentity) is not supported!");
  }

  @Override
  public T visit(File pFileFilter) {
    throw new UnsupportedOperationException("The method visit(File pFileFilter) is not supported!");
  }

  @Override
  public T visit(BasicBlockEntry pBasicBlockEntry) {
    throw new UnsupportedOperationException("The method visit(BasicBlockEntry pBasicBlockEntry) is not supported!");
  }

  @Override
  public T visit(ConditionEdge pConditionEdge) {
    throw new UnsupportedOperationException("The method visit(ConditionEdge pConditionEdge) is not supported!");
  }

  @Override
  public T visit(ConditionGraph pConditionGraph) {
    throw new UnsupportedOperationException("The method visit(ConditionGraph pConditionGraph) is not supported!");
  }

  @Override
  public T visit(DecisionEdge pDecisionEdge) {
    throw new UnsupportedOperationException("The method visit(DecisionEdge pDecisionEdge) is not supported!");
  }

  @Override
  public T visit(Line pLine) {
    throw new UnsupportedOperationException("The method visit(Line pLine) is not supported!");
  }

  @Override
  public T visit(FunctionCalls pCalls) {
    throw new UnsupportedOperationException("The method visit(FunctionCalls pCalls) is not supported!");
  }

  @Override
  public T visit(Column pColumn) {
    throw new UnsupportedOperationException("The method visit(Column pColumn) is not supported!");
  }

  @Override
  public T visit(Function pFunc) {
    throw new UnsupportedOperationException("The method visit(Function pFunc) is not supported!");
  }

  @Override
  public T visit(FunctionCall pCall) {
    throw new UnsupportedOperationException("The method visit(FunctionCall pCall) is not supported!");
  }

  @Override
  public T visit(FunctionEntry pEntry) {
    throw new UnsupportedOperationException("The method visit(FunctionEntry pEntry) is not supported!");
  }

  @Override
  public T visit(FunctionExit pExit) {
    throw new UnsupportedOperationException("The method visit(FunctionExit pExit) is not supported!");
  }

  @Override
  public T visit(Label pLabel) {
    throw new UnsupportedOperationException("The method visit(Label pLabel) is not supported!");
  }

  @Override
  public T visit(Expression pExpression) {
    throw new UnsupportedOperationException("The method visit(Expression pExpression) is not supported!");
  }

  @Override
  public T visit(RegularExpression pRegularExpression) {
    throw new UnsupportedOperationException("The method visit(RegularExpression pRegularExpression) is not supported!");
  }

  @Override
  public T visit(Complement pComplement) {
    throw new UnsupportedOperationException("The method visit(Complement pComplement) is not supported!");
  }

  @Override
  public T visit(Union pUnion) {
    throw new UnsupportedOperationException("The method visit(Union pUnion) is not supported!");
  }

  @Override
  public T visit(Compose pCompose) {
    throw new UnsupportedOperationException("The method visit(Compose pCompose) is not supported!");
  }

  @Override
  public T visit(Intersection pIntersection) {
    throw new UnsupportedOperationException("The method visit(Intersection pIntersection) is not supported!");
  }

  @Override
  public T visit(SetMinus pSetMinus) {
    throw new UnsupportedOperationException("The method visit(SetMinus pSetMinus) is not supported!");
  }

  @Override
  public T visit(EnclosingScopes pEnclosingScopes) {
    throw new UnsupportedOperationException("The method visit(EnclosingScopes pEnclosingScopes) is not supported!");
  }

  @Override
  public T visit(Predicate pPredicate) {
    throw new UnsupportedOperationException("The method visit(Predicate pPredicate) is not supported!");
  }

  @Override
  public T visit(CIdentifier pCIdentifier) {
    throw new UnsupportedOperationException("The method visit(CIdentifier pCIdentifier) is not supported!");
  }

  @Override
  public T visit(NaturalNumber pNaturalNumber) {
    throw new UnsupportedOperationException("The method visit(NaturalNumber pNaturalNumber) is not supported!");
  }

  @Override
  public T visit(Predicates pPredicates) {
    throw new UnsupportedOperationException("The method visit(Predicates pPredicates) is not supported!");
  }

  @Override
  public T visit(States pStates) {
    throw new UnsupportedOperationException("The method visit(States pStates) is not supported!");
  }

  @Override
  public T visit(Edges pEdges) {
    throw new UnsupportedOperationException("The method visit(Edges pEdges) is not supported!");
  }

  @Override
  public T visit(Paths pPaths) {
    throw new UnsupportedOperationException("The method visit(Paths pPaths) is not supported!");
  }

  @Override
  public T visit(fllesh.fql.frontend.ast.coverage.SetMinus pSetMinus) {
    throw new UnsupportedOperationException("The method visit(fql.frontend.ast.coverage.SetMinus pSetMinus) is not supported!");
  }

  @Override
  public T visit(fllesh.fql.frontend.ast.coverage.Union pUnion) {
    throw new UnsupportedOperationException("The method visit(fql.frontend.ast.coverage.Union pUnion) is not supported!");
  }

  @Override
  public T visit(fllesh.fql.frontend.ast.coverage.Intersection pIntersection) {
    throw new UnsupportedOperationException("The method visit(fql.frontend.ast.coverage.Intersection pIntersection) is not supported!");
  }

  @Override
  public T visit(ConditionalCoverage pConditionalCoverage) {
    throw new UnsupportedOperationException("The method visit(ConditionalCoverage pConditionalCoverage) is not supported!");
  }

  @Override
  public T visit(Sequence pSequence) {
    throw new UnsupportedOperationException("The method visit(Sequence pSequence) is not supported!");
  }

  @Override
  public T visit(ConditionalMonitor pConditionalMonitor) {
    throw new UnsupportedOperationException("The method visit(ConditionalMonitor pConditionalMonitor) is not supported!");
  }

  @Override
  public T visit(Alternative pAlternative) {
    throw new UnsupportedOperationException("The method visit(Alternative pAlternative) is not supported!");
  }

  @Override
  public T visit(Concatenation pConcatenation) {
    throw new UnsupportedOperationException("The method visit(Concatenation pConcatenation) is not supported!");
  }

  @Override
  public T visit(UpperBound pUpperBound) {
    throw new UnsupportedOperationException("The method visit(UpperBound pUpperBound) is not supported!");
  }

  @Override
  public T visit(LowerBound pLowerBound) {
    throw new UnsupportedOperationException("The method visit(LowerBound pLowerBound) is not supported!");
  }

  @Override
  public T visit(Query pQuery) {
    throw new UnsupportedOperationException("The method visit(Query pQuery) is not supported!");
  }

}
