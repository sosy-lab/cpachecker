package org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.fllesh.fql.backend.testgoals.DefaultTestGoalVisitor;
import org.sosy_lab.cpachecker.fllesh.fql.backend.testgoals.EdgeSequence;
import org.sosy_lab.cpachecker.fllesh.fql.backend.testgoals.TestGoal;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.DefaultASTVisitor;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.ConditionalCoverage;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Coverage;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Edges;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Paths;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.States;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.predicate.Predicate;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.predicate.Predicates;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.Filter;

public class CoverageSpecificationEvaluator extends DefaultASTVisitor<Set<? extends TestGoal>> {

  private TargetGraph mTargetGraph;
  
  public CoverageSpecificationEvaluator(TargetGraph pTargetGraph) {
    mTargetGraph = pTargetGraph;
  }
  
  public Set<? extends TestGoal> evaluate(Coverage pCoverageSpecification) {
    return pCoverageSpecification.accept(this);
  }
  
  private class TestGoalPredicator extends DefaultTestGoalVisitor<TestGoal> {

    private Predicates mPreconditions;
    private Predicates mPostconditions;

    public TestGoalPredicator(Predicates pPreconditions, Predicates pPostconditions) {
      assert(pPreconditions != null);
      assert(pPostconditions != null);

      mPreconditions = pPreconditions;
      mPostconditions = pPostconditions;
    }

    private Node getPredicatedNode(Node pNode, Predicates pPredicates) {
      assert(pNode != null);
      assert(pPredicates != null);

      if (pPredicates.isEmpty()) {
        return pNode;
      }
      else {
        Node lNode = new Node(pNode);

        for (Predicate lPredicate : pPredicates) {
          lNode.addPredicate(lPredicate, true);
        }

        return lNode;
      }
    }

    @Override
    public Edge visit(Edge pEdge) {
      assert(pEdge != null);

      Node lSource = pEdge.getSource();
      Node lTarget = pEdge.getTarget();
      CFAEdge lCFAEdge = pEdge.getCFAEdge();

      Node lNewSource = getPredicatedNode(lSource, mPreconditions);

      Node lNewTarget = getPredicatedNode(lTarget, mPostconditions);

      return new Edge(lNewSource, lNewTarget, lCFAEdge);
    }

    @Override
    public EdgeSequence visit(EdgeSequence pEdgeSequence) {
      assert(pEdgeSequence != null);

      assert(pEdgeSequence.size() > 1);

      Edge lFirstEdge = pEdgeSequence.get(0);
      Edge lLastEdge = pEdgeSequence.get(pEdgeSequence.size() - 1);


      Edge lNewFirstEdge;

      if (mPreconditions.isEmpty()) {
        lNewFirstEdge = lFirstEdge;
      }
      else {
        Node lFirstNode = lFirstEdge.getSource();

        Node lNewFirstNode = getPredicatedNode(lFirstNode, mPreconditions);

        lNewFirstEdge = new Edge(lNewFirstNode, lFirstEdge.getTarget(), lFirstEdge.getCFAEdge());
      }

      Edge lNewLastEdge;

      if (mPostconditions.isEmpty()) {
        lNewLastEdge = lLastEdge;
      }
      else {
        Node lLastNode = lLastEdge.getTarget();

        Node lNewLastNode = getPredicatedNode(lLastNode, mPostconditions);

        lNewLastEdge = new Edge(lLastEdge.getSource(), lNewLastNode, lLastEdge.getCFAEdge());
      }

      List<Edge> lEdgeSequence = new LinkedList<Edge>();

      lEdgeSequence.add(lNewFirstEdge);

      for (int lIndex = 1; lIndex < pEdgeSequence.size() - 1; lIndex++) {
        lEdgeSequence.add(pEdgeSequence.get(lIndex));
      }

      lEdgeSequence.add(lNewLastEdge);

      return new EdgeSequence(lEdgeSequence);
    }

    @Override
    public Node visit(Node pNode) {
      assert(pNode != null);

      Node lNode = new Node(pNode);

      for (Predicate lPredicate : mPreconditions) {
        lNode.addPredicate(lPredicate, true);
      }

      for (Predicate lPredicate : mPostconditions) {
        lNode.addPredicate(lPredicate, true);
      }

      return lNode;
    }

  }

  @Override
  public Set<? extends TestGoal> visit(ConditionalCoverage pConditionalCoverage) {
    assert(pConditionalCoverage != null);

    Predicates lPreconditions = pConditionalCoverage.getPreconditions();
    Predicates lPostconditions = pConditionalCoverage.getPostconditions();

    Coverage lCoverage = pConditionalCoverage.getCoverage();
    
    Set<? extends TestGoal> lTestGoals = lCoverage.accept(this);

    Set<TestGoal> lPredicatedTestGoals = new HashSet<TestGoal>();

    TestGoalPredicator lPredicator = new TestGoalPredicator(lPreconditions, lPostconditions);

    for (TestGoal lTestGoal : lTestGoals) {
      lPredicatedTestGoals.add(lTestGoal.accept(lPredicator));
    }

    return lPredicatedTestGoals;
  }

  @Override
  public Set<? extends TestGoal> visit(Edges pEdges) {
    assert(pEdges != null);

    Filter lFilter = pEdges.getFilter();
    Predicates lPredicates = pEdges.getPredicates();

    FilterEvaluator lFilterEvaluator = new FilterEvaluator(mTargetGraph);
    
    TargetGraph lFilteredTargetGraph = lFilterEvaluator.evaluate(lFilter);
    TargetGraph lPredicatedTargetGraph = TargetGraph.applyPredication(lFilteredTargetGraph, lPredicates);

    return lPredicatedTargetGraph.getEdges();
  }

  @Override
  public Set<? extends TestGoal> visit(Paths pPaths) {
    assert(pPaths != null);

    Filter lFilter = pPaths.getFilter();
    Predicates lPredicates = pPaths.getPredicates();

    FilterEvaluator lFilterEvaluator = new FilterEvaluator(mTargetGraph);
    
    TargetGraph lFilteredTargetGraph = lFilterEvaluator.evaluate(lFilter);
    TargetGraph lPredicatedTargetGraph = TargetGraph.applyPredication(lFilteredTargetGraph, lPredicates);

    Iterator<Node> lInitialNodesIterator = lPredicatedTargetGraph.getInitialNodes();

    HashSet<TestGoal> lTestGoals = new HashSet<TestGoal>();

    while (lInitialNodesIterator.hasNext()) {
      Node lInitialNode = lInitialNodesIterator.next();

      Set<Edge> lOutgoingEdges = lPredicatedTargetGraph.getOutgoingEdges(lInitialNode);

      if (lOutgoingEdges.size() == 0) {
        lTestGoals.add(lInitialNode);
      }
      else {
        // enumerate all k-bounded paths starting in lInitialNode
        HashMap<Node, Integer> lNumberOfOccurrences = new HashMap<Node, Integer>();

        lNumberOfOccurrences.put(lInitialNode, 1);

        LinkedList<Iterator<Edge>> lIteratorStack = new LinkedList<Iterator<Edge>>();

        lIteratorStack.addFirst(lOutgoingEdges.iterator());

        LinkedList<Edge> lEdgeStack = new LinkedList<Edge>();

        while (!lIteratorStack.isEmpty()) {
          Iterator<Edge> lIterator = lIteratorStack.getLast();

          if (lIterator.hasNext()) {
            Edge lEdge = lIterator.next();

            lEdgeStack.addLast(lEdge);

            Node lTarget = lEdge.getTarget();

            int lOccurrences;

            if (lNumberOfOccurrences.containsKey(lTarget)) {
              lOccurrences = lNumberOfOccurrences.get(lTarget);

              lOccurrences++;
            }
            else {
              lOccurrences = 1;
            }

            if (lOccurrences > pPaths.getBound()) {
              lEdgeStack.removeLast();
              lNumberOfOccurrences.put(lTarget, lOccurrences - 1);

              switch (lEdgeStack.size()) {
              case 0:
                lTestGoals.add(lInitialNode);
                break;
              case 1:
                lTestGoals.add(lEdgeStack.getFirst());
                break;
              default:
                lTestGoals.add(new EdgeSequence(lEdgeStack));
                break;
              }
            }
            else {
              // add iterator to stack
              Iterator<Edge> lNextIterator = lPredicatedTargetGraph.getOutgoingEdges(lTarget).iterator();

              if (lNextIterator.hasNext()) {
                // update occurrences
                lNumberOfOccurrences.put(lTarget, lOccurrences);

                // add to stack
                lIteratorStack.addLast(lNextIterator);
              }
              else {
                assert(lEdgeStack.size() > 0);

                if (lEdgeStack.size() == 1) {
                  lTestGoals.add(lEdge);
                }
                else {
                  lTestGoals.add(new EdgeSequence(lEdgeStack));
                }

                lEdgeStack.removeLast();
              }
            }
          }
          else {
            lIteratorStack.removeLast();

            // first iterator is not associated with an edge,
            // so remove only edge if it is not the first one
            if (lIteratorStack.size() > 0) {
              Edge lEdge = lEdgeStack.getLast();
              Node lTarget = lEdge.getTarget();
              int lOccurrences = lNumberOfOccurrences.get(lTarget);
              lNumberOfOccurrences.put(lTarget, lOccurrences - 1);

              lEdgeStack.removeLast();
            }
          }
        }
      }
    }

    return lTestGoals;
  }

  @Override
  public Set<? extends TestGoal> visit(States pStates) {
    assert(pStates != null);

    Filter lFilter = pStates.getFilter();
    Predicates lPredicates = pStates.getPredicates();

    FilterEvaluator lFilterEvaluator = new FilterEvaluator(mTargetGraph);
    
    TargetGraph lFilteredTargetGraph = lFilterEvaluator.evaluate(lFilter);
    TargetGraph lPredicatedTargetGraph = TargetGraph.applyPredication(lFilteredTargetGraph, lPredicates);

    return lPredicatedTargetGraph.getNodes();
  }

  @Override
  public Set<? extends TestGoal> visit(org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Union pUnion) {
    assert(pUnion != null);

    Coverage lLeftCoverage = pUnion.getLeftCoverage();
    Coverage lRightCoverage = pUnion.getRightCoverage();

    assert(lLeftCoverage != null);
    assert(lRightCoverage != null);

    Set<? extends TestGoal> lFirstTestGoalSet = lLeftCoverage.accept(this);
    Set<? extends TestGoal> lSecondTestGoalSet = lRightCoverage.accept(this);

    Set<TestGoal> lResult = new HashSet<TestGoal>();

    lResult.addAll(lFirstTestGoalSet);
    lResult.addAll(lSecondTestGoalSet);

    return lResult;
  }

  @Override
  public Set<? extends TestGoal> visit(org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Intersection pIntersection) {
    // TODO Auto-generated method stub
    return super.visit(pIntersection);
  }

  @Override
  public Set<? extends TestGoal> visit(org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.SetMinus pSetMinus) {
    // TODO Auto-generated method stub
    return super.visit(pSetMinus);
  }
  
}
