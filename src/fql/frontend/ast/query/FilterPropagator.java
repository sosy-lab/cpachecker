package fql.frontend.ast.query;

import common.Pair;

import fql.frontend.ast.ASTVisitor;
import fql.frontend.ast.FQLNode;
import fql.frontend.ast.coverage.ConditionalCoverage;
import fql.frontend.ast.coverage.Coverage;
import fql.frontend.ast.coverage.Edges;
import fql.frontend.ast.coverage.Paths;
import fql.frontend.ast.coverage.Sequence;
import fql.frontend.ast.coverage.States;
import fql.frontend.ast.filter.BasicBlockEntry;
import fql.frontend.ast.filter.Column;
import fql.frontend.ast.filter.Complement;
import fql.frontend.ast.filter.Compose;
import fql.frontend.ast.filter.ConditionEdge;
import fql.frontend.ast.filter.ConditionGraph;
import fql.frontend.ast.filter.DecisionEdge;
import fql.frontend.ast.filter.EnclosingScopes;
import fql.frontend.ast.filter.Expression;
import fql.frontend.ast.filter.File;
import fql.frontend.ast.filter.Filter;
import fql.frontend.ast.filter.Function;
import fql.frontend.ast.filter.FunctionCall;
import fql.frontend.ast.filter.FunctionCalls;
import fql.frontend.ast.filter.FunctionEntry;
import fql.frontend.ast.filter.FunctionExit;
import fql.frontend.ast.filter.Identity;
import fql.frontend.ast.filter.Intersection;
import fql.frontend.ast.filter.Label;
import fql.frontend.ast.filter.Line;
import fql.frontend.ast.filter.RegularExpression;
import fql.frontend.ast.filter.SetMinus;
import fql.frontend.ast.filter.Union;
import fql.frontend.ast.pathmonitor.Alternative;
import fql.frontend.ast.pathmonitor.Concatenation;
import fql.frontend.ast.pathmonitor.ConditionalMonitor;
import fql.frontend.ast.pathmonitor.LowerBound;
import fql.frontend.ast.pathmonitor.PathMonitor;
import fql.frontend.ast.pathmonitor.UpperBound;
import fql.frontend.ast.predicate.CIdentifier;
import fql.frontend.ast.predicate.NaturalNumber;
import fql.frontend.ast.predicate.Predicate;
import fql.frontend.ast.predicate.Predicates;

public class FilterPropagator implements ASTVisitor<FQLNode> {

  private Filter mFilter;
  
  public FilterPropagator(Filter pFilter) {
    assert(pFilter != null);
    
    mFilter = pFilter;
  }
  
  private Compose apply(Filter pFilter) {
    assert(pFilter != null);
    
    return new Compose(pFilter, mFilter);
  }
  
  @Override
  public FQLNode visit(Identity pIdentity) {
    return apply(pIdentity);
  }

  @Override
  public FQLNode visit(File pFileFilter) {
    return apply(pFileFilter);
  }

  @Override
  public FQLNode visit(BasicBlockEntry pBasicBlockEntry) {
    return apply(pBasicBlockEntry);
  }

  @Override
  public FQLNode visit(ConditionEdge pConditionEdge) {
    return apply(pConditionEdge);
  }

  @Override
  public FQLNode visit(ConditionGraph pConditionGraph) {
    return apply(pConditionGraph);
  }

  @Override
  public FQLNode visit(DecisionEdge pDecisionEdge) {
    return apply(pDecisionEdge);
  }

  @Override
  public FQLNode visit(Line pLine) {
    return apply(pLine);
  }

  @Override
  public FQLNode visit(FunctionCalls pCalls) {
    return apply(pCalls);
  }

  @Override
  public FQLNode visit(Column pColumn) {
    return apply(pColumn);
  }

  @Override
  public FQLNode visit(Function pFunc) {
    return apply(pFunc);
  }

  @Override
  public FQLNode visit(FunctionCall pCall) {
    return apply(pCall);
  }

  @Override
  public FQLNode visit(FunctionEntry pEntry) {
    return apply(pEntry);
  }

  @Override
  public FQLNode visit(FunctionExit pExit) {
    return apply(pExit);
  }

  @Override
  public FQLNode visit(Label pLabel) {
    return apply(pLabel);
  }

  @Override
  public FQLNode visit(Expression pExpression) {
    return apply(pExpression);
  }

  @Override
  public FQLNode visit(RegularExpression pRegularExpression) {
    return apply(pRegularExpression);
  }

  @Override
  public FQLNode visit(Complement pComplement) {
    return apply(pComplement);
  }

  @Override
  public FQLNode visit(Union pUnion) {
    return apply(pUnion);
  }

  @Override
  public FQLNode visit(Compose pCompose) {
    return apply(pCompose);
  }

  @Override
  public FQLNode visit(Intersection pIntersection) {
    return apply(pIntersection);
  }

  @Override
  public FQLNode visit(SetMinus pSetMinus) {
    return apply(pSetMinus);
  }

  @Override
  public FQLNode visit(EnclosingScopes pEnclosingScopes) {
    return apply(pEnclosingScopes);
  }

  @Override
  public FQLNode visit(Predicate pPredicate) {
    assert(false);
    
    return null;
  }

  @Override
  public FQLNode visit(CIdentifier pCIdentifier) {
    assert(false);
    
    return null;
  }

  @Override
  public FQLNode visit(NaturalNumber pNaturalNumber) {
    assert(false);
    
    return null;
  }

  @Override
  public FQLNode visit(Predicates pPredicates) {
    assert(false);
    
    return null;
  }

  @Override
  public FQLNode visit(States pStates) {
    return new States(apply(pStates.getFilter()), pStates.getPredicates());
  }

  @Override
  public FQLNode visit(Edges pEdges) {
    return new Edges(apply(pEdges.getFilter()), pEdges.getPredicates());
  }

  @Override
  public FQLNode visit(Paths pPaths) {
    return new Paths(apply(pPaths.getFilter()), pPaths.getBound(), pPaths.getPredicates());
  }

  @Override
  public FQLNode visit(fql.frontend.ast.coverage.SetMinus pSetMinus) {
    FQLNode lNode1 = pSetMinus.getLeftCoverage().accept(this);
    FQLNode lNode2 = pSetMinus.getRightCoverage().accept(this);
    
    assert(lNode1 instanceof Coverage);
    assert(lNode2 instanceof Coverage);
    
    return new fql.frontend.ast.coverage.SetMinus((Coverage)lNode1, (Coverage)lNode2);
  }

  @Override
  public FQLNode visit(fql.frontend.ast.coverage.Union pUnion) {
    FQLNode lNode1 = pUnion.getLeftCoverage().accept(this);
    FQLNode lNode2 = pUnion.getRightCoverage().accept(this);
    
    assert(lNode1 instanceof Coverage);
    assert(lNode2 instanceof Coverage);
    
    return new fql.frontend.ast.coverage.Union((Coverage)lNode1, (Coverage)lNode2);
  }

  @Override
  public FQLNode visit(fql.frontend.ast.coverage.Intersection pIntersection) {
    FQLNode lNode1 = pIntersection.getLeftCoverage().accept(this);
    FQLNode lNode2 = pIntersection.getRightCoverage().accept(this);
    
    assert(lNode1 instanceof Coverage);
    assert(lNode2 instanceof Coverage);
    
    return new fql.frontend.ast.coverage.Intersection((Coverage)lNode1, (Coverage)lNode2);
  }

  @Override
  public FQLNode visit(ConditionalCoverage pConditionalCoverage) {
    FQLNode lNode = pConditionalCoverage.getCoverage().accept(this);
    
    assert(lNode instanceof Coverage);
    
    return new ConditionalCoverage(pConditionalCoverage.getPreconditions(), (Coverage)lNode, pConditionalCoverage.getPostconditions());
  }

  @Override
  public FQLNode visit(Sequence pSequence) {
    // we do not change initial and final path monitor
    
    Pair<PathMonitor, Coverage> lPair = pSequence.get(0);
    
    PathMonitor lInitialMonitor = lPair.getFirst();
    Coverage lCoverage = lPair.getSecond();
    
    FQLNode lNode = lCoverage.accept(this);
    
    assert(lNode instanceof Coverage);
    
    Sequence lSequence = new Sequence(lInitialMonitor, (Coverage)lNode, pSequence.getFinalMonitor());
    
    for (int i = 1; i < pSequence.size(); i++) {
      Pair<PathMonitor, Coverage> lPair2 = pSequence.get(i);
      
      FQLNode lNode1 = lPair2.getFirst().accept(this);
      FQLNode lNode2 = lPair2.getSecond().accept(this);
      
      assert(lNode1 instanceof PathMonitor);
      assert(lNode2 instanceof Coverage);
      
      lSequence.extend((PathMonitor)lNode1, (Coverage)lNode2);
    }
    
    return lSequence;
  }

  @Override
  public FQLNode visit(ConditionalMonitor pConditionalMonitor) {
    FQLNode lNode = pConditionalMonitor.getSubmonitor().accept(this);
    
    assert(lNode instanceof PathMonitor);
    
    return new ConditionalMonitor(pConditionalMonitor.getPreconditions(), (PathMonitor)lNode, pConditionalMonitor.getPostconditions());
  }

  @Override
  public FQLNode visit(Alternative pAlternative) {
    FQLNode lNode1 = pAlternative.getLeftSubmonitor().accept(this);
    FQLNode lNode2 = pAlternative.getRightSubmonitor().accept(this);
    
    assert(lNode1 instanceof PathMonitor);
    assert(lNode2 instanceof PathMonitor);
    
    return new Alternative((PathMonitor)lNode1, (PathMonitor)lNode2);
  }

  @Override
  public FQLNode visit(Concatenation pConcatenation) {
    FQLNode lNode1 = pConcatenation.getLeftSubmonitor().accept(this);
    FQLNode lNode2 = pConcatenation.getRightSubmonitor().accept(this);
    
    assert(lNode1 instanceof PathMonitor);
    assert(lNode2 instanceof PathMonitor);
    
    return new Concatenation((PathMonitor)lNode1, (PathMonitor)lNode2);
  }

  @Override
  public FQLNode visit(UpperBound pUpperBound) {
    FQLNode lNode = pUpperBound.getSubmonitor().accept(this);
    
    assert(lNode instanceof PathMonitor);
    
    return new UpperBound((PathMonitor)lNode, pUpperBound.getBound());
  }

  @Override
  public FQLNode visit(LowerBound pLowerBound) {
    FQLNode lNode = pLowerBound.getSubmonitor().accept(this);
    
    assert(lNode instanceof PathMonitor);
    
    return new LowerBound((PathMonitor)lNode, pLowerBound.getBound());
  }

  @Override
  public FQLNode visit(Query pQuery) {
    FQLNode lNode = pQuery.getCoverage().accept(this);
    
    assert(lNode instanceof Coverage);
    
    return new Query((Coverage)lNode, pQuery.getPassingMonitor());
  }

}
