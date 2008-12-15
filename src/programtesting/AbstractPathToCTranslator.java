/**
 * 
 */
package programtesting;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.AssumeEdge;

import cpa.symbpredabs.explicit.ExplicitAbstractElement;

/**
 * @author holzera
 *
 */
public class AbstractPathToCTranslator {
  public static void translatePaths(Collection<Deque<ExplicitAbstractElement>> pPaths) {
    assert(pPaths != null);
    
    int i = 0;
    
    for (Deque<ExplicitAbstractElement> lAbstractPath : pPaths) {
      System.out.println("#### PATH " + i + " ####");
      
      translatePath(lAbstractPath);
      
      i++;
    }
    
  }
  
  public static void translatePath(Deque<ExplicitAbstractElement> pAbstractPath) {
    assert(pAbstractPath != null);
    
    ExplicitAbstractElement lPredecessorElement = pAbstractPath.getFirst();
    
    boolean first = true;
    
    List<CFAEdge> lEdges = new ArrayList<CFAEdge>();
    
    for (ExplicitAbstractElement lElement : pAbstractPath) {
      if (first) {
        first = false;
        continue;
      }
      
      CFANode lPredecessorNode = lPredecessorElement.getLocationNode();
      CFANode lNode = lElement.getLocationNode();
      
      // reconstruct edge
      int lNumberOfFoundEdges = 0;
      
      for (int lIndex = 0; lIndex < lPredecessorNode.getNumLeavingEdges(); lIndex++) {
        CFAEdge lEdge = lPredecessorNode.getLeavingEdge(lIndex);
        
        if (lEdge.getSuccessor().equals(lNode)) {
          lEdges.add(lEdge);
          lNumberOfFoundEdges++;
        }
      }
      
      assert(lNumberOfFoundEdges == 1);
      
      lPredecessorElement = lElement;
    }
    
    translatePath(lEdges);
  }
  
  public static void translatePath(List<CFAEdge> pAbstractPath) {
    StringWriter lProgramTextWriter = new StringWriter();
    PrintWriter lProgramText = new PrintWriter(lProgramTextWriter);
    
    for (CFAEdge lEdge : pAbstractPath) {
      System.out.println(lEdge.getRawStatement());
      
      switch (lEdge.getEdgeType()) {
      case BlankEdge: {
        // nothing to do
        break;
      }
      case AssumeEdge: {
        AssumeEdge lAssumeEdge = (AssumeEdge)lEdge;
        
        String lExpressionString = lAssumeEdge.getExpression().getRawSignature();
        
        String lAssumptionString;
        
        if (lAssumeEdge.getTruthAssumption()) {
          lAssumptionString = lExpressionString;
        }
        else {
          lAssumptionString = "!(" + lExpressionString + ")";
        }
        
        lProgramText.println("__CPROVER_assume(" + lAssumptionString + ");");
        
        break;
      }
      case StatementEdge: {
        // TODO implement
        assert(false);
        
        break;
      }
      case DeclarationEdge: {
        // TODO implement
        assert(false);
        
        break;
      }
      case FunctionCallEdge: {
        // TODO implement
        assert(false);
        
        break;
      }
      case ReturnEdge: {
        // TODO implement
        assert(false);
        
        break;
      }
      case MultiStatementEdge: {
        // TODO implement
        assert(false);
        
        break;
      }
      case MultiDeclarationEdge: {
        // TODO implement
        assert(false);
        
        break;
      }
      case CallToReturnEdge: {
        // TODO implement
        assert(false);
        
        break;
      }
      default: {
        assert(false);
      }
      }
    }
    
    System.out.println(lProgramTextWriter.toString());
  }
}
