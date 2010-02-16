package fql.fllesh.cpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Test;

import cfa.objectmodel.BlankEdge;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.ReturnEdge;

import cpa.common.interfaces.AbstractElement;
import cpa.mustmay.SimpleMustMayAnalysisCPA;
import exceptions.CPATransferException;
import fql.fllesh.reachability.Query;


public class QueryTransferRelationTest {

  @Test
  public void test_01() throws CPATransferException {
    SimpleMustMayAnalysisCPA lMustMayCPA = new SimpleMustMayAnalysisCPA("", "");
    
    AbstractElement lMustBottomElement = lMustMayCPA.getAbstractDomain().getBottomElement().getMustElement();
    
    // TODO implement
    Query lQuery = null;
    
    QueryTransferRelation lTransferRelation = new QueryTransferRelation(lQuery, QueryTopElement.getInstance(), QueryBottomElement.getInstance(), lMustMayCPA.getTransferRelation(), lMustBottomElement);
    
    Collection<? extends AbstractElement> lSuccessors = lTransferRelation.getAbstractSuccessors(QueryBottomElement.getInstance(), null, null);
    
    assertTrue(lSuccessors.isEmpty());
  }
  
  @Test
  public void test_02() throws CPATransferException {
    SimpleMustMayAnalysisCPA lMustMayCPA = new SimpleMustMayAnalysisCPA("", "");
    
    AbstractElement lMustBottomElement = lMustMayCPA.getAbstractDomain().getBottomElement().getMustElement();
    
    // TODO implement
    Query lQuery = null;
    
    QueryTransferRelation lTransferRelation = new QueryTransferRelation(lQuery, QueryTopElement.getInstance(), QueryBottomElement.getInstance(), lMustMayCPA.getTransferRelation(), lMustBottomElement);
    
    Collection<? extends AbstractElement> lSuccessors = lTransferRelation.getAbstractSuccessors(QueryTopElement.getInstance(), null, null);
    
    assertEquals(lSuccessors.size(), 1);
    assertEquals(lSuccessors.iterator().next(), QueryTopElement.getInstance());
  }

  @Test
  public void test_03() throws CPATransferException {
    SimpleMustMayAnalysisCPA lMustMayCPA = new SimpleMustMayAnalysisCPA("", "");
    
    AbstractElement lMustBottomElement = lMustMayCPA.getAbstractDomain().getBottomElement().getMustElement();
    
    // TODO implement
    Query lQuery = null;
    
    QueryTransferRelation lTransferRelation = new QueryTransferRelation(lQuery, QueryTopElement.getInstance(), QueryBottomElement.getInstance(), lMustMayCPA.getTransferRelation(), lMustBottomElement);
    
    QueryStandardElement lElement = new QueryStandardElement(0, true, 1, false, lMustMayCPA.getAbstractDomain().getTopElement());
    
    CFANode lNode = new CFANode(10);
    
    CFAEdge lCFAEdge = InternalSelfLoop.getOrCreate(lNode);
    
    Collection<? extends AbstractElement> lSuccessors = lTransferRelation.getAbstractSuccessors(lElement, null, lCFAEdge);
  }
  
  @Test
  public void test_04() throws CPATransferException {
    SimpleMustMayAnalysisCPA lMustMayCPA = new SimpleMustMayAnalysisCPA("", "");
    
    AbstractElement lMustBottomElement = lMustMayCPA.getAbstractDomain().getBottomElement().getMustElement();
    
    // TODO implement
    Query lQuery = null;
    
    QueryTransferRelation lTransferRelation = new QueryTransferRelation(lQuery, QueryTopElement.getInstance(), QueryBottomElement.getInstance(), lMustMayCPA.getTransferRelation(), lMustBottomElement);
    
    QueryStandardElement lElement = new QueryStandardElement(0, true, 1, false, lMustMayCPA.getAbstractDomain().getTopElement());
    
    CFAEdge lCFAEdge = new ReturnEdge("bla");
    
    Collection<? extends AbstractElement> lSuccessors = lTransferRelation.getAbstractSuccessors(lElement, null, lCFAEdge);
  }
  
  @Test
  public void test_05() throws CPATransferException {
    SimpleMustMayAnalysisCPA lMustMayCPA = new SimpleMustMayAnalysisCPA("", "");
    
    AbstractElement lMustBottomElement = lMustMayCPA.getAbstractDomain().getBottomElement().getMustElement();
    
    // TODO implement
    Query lQuery = null;
    
    QueryTransferRelation lTransferRelation = new QueryTransferRelation(lQuery, QueryTopElement.getInstance(), QueryBottomElement.getInstance(), lMustMayCPA.getTransferRelation(), lMustBottomElement);
    
    QueryStandardElement lElement = new QueryStandardElement(0, true, 1, false, lMustMayCPA.getAbstractDomain().getTopElement());
    
    CFAEdge lCFAEdge = new BlankEdge("blub");
    
    Collection<? extends AbstractElement> lSuccessors = lTransferRelation.getAbstractSuccessors(lElement, null, lCFAEdge);
  }
  
}
