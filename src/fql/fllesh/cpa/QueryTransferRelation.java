package fql.fllesh.cpa;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import common.Pair;

import cfa.objectmodel.BlankEdge;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.c.AssumeEdge;
import cfa.objectmodel.c.DeclarationEdge;
import cfa.objectmodel.c.FunctionCallEdge;
import cfa.objectmodel.c.GlobalDeclarationEdge;
import cfa.objectmodel.c.ReturnEdge;
import cfa.objectmodel.c.StatementEdge;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import cpa.mustmay.MustMayAnalysisElement;
import cpa.mustmay.MustMayAnalysisPrecision;
import cpa.mustmay.MustMayAnalysisTransferRelation;
import exceptions.CPATransferException;
import fql.backend.pathmonitor.Automaton;
import fql.backend.pathmonitor.AutomatonEdge;
import fql.backend.pathmonitor.PredicatesEdge;
import fql.backend.pathmonitor.TargetGraphEdge;
import fql.backend.targetgraph.Edge;
import fql.fllesh.reachability.Query;
import fql.fllesh.util.DefaultFlleshCFAEdgeVisitor;

public class QueryTransferRelation implements TransferRelation {

  private class QueryCFAEdgeVisitor extends
      DefaultFlleshCFAEdgeVisitor<Set<QueryElement>> {

    private Query mQuery;
    private QueryStandardElement            mCurrentElement;
    private MustMayAnalysisTransferRelation mDataSpaceTransferRelation;
    private MustMayAnalysisPrecision mDataSpacePrecision;
    private AbstractElement mMustBottomElement;

    private QueryCFAEdgeVisitor(Query pQuery,
        MustMayAnalysisTransferRelation pTransferRelation, AbstractElement pMustBottomElement) {
      assert(pTransferRelation != null);
      assert(pQuery != null);
      assert(pMustBottomElement != null);

      mDataSpaceTransferRelation = pTransferRelation;
      mMustBottomElement = pMustBottomElement;
      mQuery = pQuery;
    }

    // TODO we also need precision
    public void setCurrentElementAndPrecision(QueryStandardElement pElement, MustMayAnalysisPrecision pPrecision) {
      assert(pElement != null);
      assert(pPrecision != null);

      mCurrentElement = pElement;
      mDataSpacePrecision = pPrecision;
    }

    private Set<Pair<Integer, Boolean>> getSuccessors(boolean pMustState, Set<AutomatonEdge> pOutgoingAutomatonEdges, CFAEdge pCFAEdge, MustMayAnalysisElement pMustMaySuccessor) {
      Set<Pair<Integer, Boolean>> lSuccessors = new HashSet<Pair<Integer, Boolean>>();
      
      for (AutomatonEdge lOutgoingEdge : pOutgoingAutomatonEdges) {
        if (lOutgoingEdge instanceof TargetGraphEdge) {
          TargetGraphEdge lTargetGraphEdge = (TargetGraphEdge)lOutgoingEdge;
          
          Set<Edge> lEdges = lTargetGraphEdge.getEdges();
          
          // TODO this operation is expensive in its current implementation
          for (Edge lEdge : lEdges) {
            if (lEdge.getCFAEdge().equals(pCFAEdge)) {
              
              if (!lEdge.getSource().getPredicates().isEmpty()) {
                // TODO implement predication handling
                throw new UnsupportedOperationException();
              }
              
              if (!lEdge.getSource().getPredicates().isEmpty()) {
                // TODO implement predication handling
                throw new UnsupportedOperationException();
              }
              
              boolean mMustState = pMustState;
              
              // TODO implement general determination of boolean value
              if (mMustState && pMustMaySuccessor.getMustElement().equals(mMustBottomElement)) {
                mMustState = false;
              }
              
              lSuccessors.add(new Pair<Integer, Boolean>(lOutgoingEdge.getTarget(), mMustState));
              
            }
          }
        }
      }
      
      return lSuccessors;
    }
    
    private Set<Pair<Integer, Boolean>> getPredicatesEdgeSuccessors(boolean pMustState, Set<AutomatonEdge> pOutgoingAutomatonEdges, CFAEdge pCFAEdge, MustMayAnalysisElement pMustMaySuccessor) {
      Set<Pair<Integer, Boolean>> lSuccessors = new HashSet<Pair<Integer, Boolean>>();
      
      for (AutomatonEdge lOutgoingEdge : pOutgoingAutomatonEdges) {
        if (lOutgoingEdge instanceof PredicatesEdge) {
          // TODO implement predicates check
          
          lSuccessors.add(new Pair<Integer, Boolean>(lOutgoingEdge.getTarget(), false));
        }
      }
      
      return lSuccessors;
    }
    
    private Set<QueryElement> processEdge(CFAEdge pEdge) {
      assert(pEdge != null);
      
      MustMayAnalysisElement lCurrentDataSpace = mCurrentElement.getDataSpace();
      
      Collection<? extends AbstractElement> lDataSpaceSuccessors = null;
      
      try {
        lDataSpaceSuccessors = mDataSpaceTransferRelation.getAbstractSuccessors(lCurrentDataSpace, mDataSpacePrecision, pEdge);
      } catch (CPATransferException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        
        throw new UnsupportedOperationException();
      }
      
      Set<QueryElement> lSuccessors = new HashSet<QueryElement>();
      
      Integer lAutomatonState1 = mCurrentElement.getAutomatonState1();
      Integer lAutomatonState2 = mCurrentElement.getAutomatonState2();
      
      // TODO pass only automata not query ?
      Automaton lAutomaton1 = mQuery.getFirstAutomaton();
      Automaton lAutomaton2 = mQuery.getSecondAutomaton();
      
      Set<AutomatonEdge> lOutgoingEdges1 = lAutomaton1.getOutgoingEdges(lAutomatonState1);
      Set<AutomatonEdge> lOutgoingEdges2 = lAutomaton2.getOutgoingEdges(lAutomatonState2);
      
      for (AbstractElement lDataSpaceSuccessor : lDataSpaceSuccessors) {
        MustMayAnalysisElement lMustMaySuccessor = (MustMayAnalysisElement)lDataSpaceSuccessor;
        
        Set<Pair<Integer, Boolean>> lSuccessors1 = getSuccessors(mCurrentElement.getMustState1(), lOutgoingEdges1, pEdge, lMustMaySuccessor); 
        
        Set<Pair<Integer, Boolean>> lSuccessors2 = getSuccessors(mCurrentElement.getMustState2(), lOutgoingEdges2, pEdge, lMustMaySuccessor);
        
        for (Pair<Integer, Boolean> lSuccessor1 : lSuccessors1) {
          for (Pair<Integer, Boolean> lSuccessor2 : lSuccessors2) {
            lSuccessors.add(new QueryStandardElement(lSuccessor1.getFirst(), lSuccessor1.getSecond(), lSuccessor2.getFirst(), lSuccessor2.getSecond(), lMustMaySuccessor));
          }
        }
      }
      
      return lSuccessors;
    }

    @Override
    public Set<QueryElement> visit(InternalSelfLoop pEdge) {
      assert(pEdge != null);
      
      MustMayAnalysisElement lCurrentDataSpace = mCurrentElement.getDataSpace();
      
      Set<QueryElement> lSuccessors = new HashSet<QueryElement>();
      
      Integer lAutomatonState1 = mCurrentElement.getAutomatonState1();
      Integer lAutomatonState2 = mCurrentElement.getAutomatonState2();
      
      // TODO pass only automata not query ?
      Automaton lAutomaton1 = mQuery.getFirstAutomaton();
      Automaton lAutomaton2 = mQuery.getSecondAutomaton();
      
      Set<AutomatonEdge> lOutgoingEdges1 = lAutomaton1.getOutgoingEdges(lAutomatonState1);
      Set<AutomatonEdge> lOutgoingEdges2 = lAutomaton2.getOutgoingEdges(lAutomatonState2);
      
      
      Set<Pair<Integer, Boolean>> lSuccessors1 = getPredicatesEdgeSuccessors(mCurrentElement.getMustState1(), lOutgoingEdges1, pEdge, lCurrentDataSpace); 
        
      for (Pair<Integer, Boolean> lSuccessor1 : lSuccessors1) {
        lSuccessors.add(new QueryStandardElement(lSuccessor1.getFirst(), lSuccessor1.getSecond(), mCurrentElement.getAutomatonState2(), mCurrentElement.getMustState2(), lCurrentDataSpace));
      }
      
      
      Set<Pair<Integer, Boolean>> lSuccessors2 = getPredicatesEdgeSuccessors(mCurrentElement.getMustState2(), lOutgoingEdges2, pEdge, lCurrentDataSpace);
      
      for (Pair<Integer, Boolean> lSuccessor2 : lSuccessors2) {
        lSuccessors.add(new QueryStandardElement(mCurrentElement.getAutomatonState1(), mCurrentElement.getMustState1(), lSuccessor2.getFirst(), lSuccessor2.getSecond(), lCurrentDataSpace));
      }
      
      
      return lSuccessors;
    }

    @Override
    public Set<QueryElement> visit(AssumeEdge pEdge) {
      return processEdge(pEdge);
    }

    @Override
    public Set<QueryElement> visit(DeclarationEdge pEdge) {
      return processEdge(pEdge);
    }

    @Override
    public Set<QueryElement> visit(FunctionCallEdge pEdge) {
      return processEdge(pEdge);
    }

    @Override
    public Set<QueryElement> visit(GlobalDeclarationEdge pEdge) {
      return processEdge(pEdge);
    }

    @Override
    public Set<QueryElement> visit(ReturnEdge pEdge) {
      return processEdge(pEdge);
    }

    @Override
    public Set<QueryElement> visit(StatementEdge pEdge) {
      return processEdge(pEdge);
    }

    @Override
    public Set<QueryElement> visit(BlankEdge pEdge) {
      return processEdge(pEdge);
    }

  }

  private QueryTopElement     mTopElement;
  private QueryBottomElement  mBottomElement;
  private QueryCFAEdgeVisitor mVisitor;

  public QueryTransferRelation(Query pQuery, QueryTopElement pTopElement,
      QueryBottomElement pBottomElement,
      MustMayAnalysisTransferRelation pTransferRelation, AbstractElement pMustBottomElement) {
    assert(pQuery != null);
    assert(pTopElement != null);
    assert(pBottomElement != null);
    assert(pTransferRelation != null);
    assert(pMustBottomElement != null);

    mTopElement = pTopElement;
    mBottomElement = pBottomElement;

    mVisitor = new QueryCFAEdgeVisitor(pQuery, pTransferRelation, pMustBottomElement);
  }

  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {

    if (pElement.equals(mTopElement)) {
      System.out.println("returning top element...");
      
      return Collections.singleton(mTopElement); 
    }

    if (pElement.equals(mBottomElement)) {
      System.out.println("returning empty set...");
      
      return Collections.emptySet(); 
    }

    if (!(pPrecision instanceof MustMayAnalysisPrecision)) {
      throw new UnsupportedOperationException();
    }
    
    mVisitor.setCurrentElementAndPrecision((QueryStandardElement) pElement, (MustMayAnalysisPrecision)pPrecision);

    System.out.println("visiting ..." + pElement.toString() + "*" + pCfaEdge.toString());
    
    Collection<? extends AbstractElement> lResult = mVisitor.visit(pCfaEdge);
    
    System.out.println(lResult);
    
    return lResult;
    
    //return mVisitor.visit(pCfaEdge);
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(
      AbstractElement pElement, List<AbstractElement> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException {
    // TODO Auto-generated method stub
    return null;
  }

}
