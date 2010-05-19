package org.sosy_lab.cpachecker.fllesh.fql2.translators;

import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.fllesh.ecp.ECPConcatenation;
import org.sosy_lab.cpachecker.fllesh.ecp.ECPEdgeSet;
import org.sosy_lab.cpachecker.fllesh.ecp.ECPNodeSet;
import org.sosy_lab.cpachecker.fllesh.ecp.ECPPredicate;
import org.sosy_lab.cpachecker.fllesh.ecp.ECPRepetition;
import org.sosy_lab.cpachecker.fllesh.ecp.ECPUnion;
import org.sosy_lab.cpachecker.fllesh.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph.Edge;
import org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph.Node;
import org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph.TargetGraph;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.Edges;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.Nodes;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.Paths;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.Predicate;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.pathpattern.Concatenation;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.pathpattern.PathPattern;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.pathpattern.PathPatternVisitor;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.pathpattern.Repetition;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.pathpattern.Union;

public class PathPatternToECPTranslator {

  private TargetGraph mTargetGraph;
  private Visitor mVisitor;
  
  public PathPatternToECPTranslator(TargetGraph pTargetGraph) {
    mTargetGraph = pTargetGraph;
    mVisitor = new Visitor();
  }
  
  public ElementaryCoveragePattern translate(PathPattern pPattern) {
    return pPattern.accept(mVisitor);
  }
  
  private class Visitor implements PathPatternVisitor<ElementaryCoveragePattern> {

    @Override
    public ElementaryCoveragePattern visit(Concatenation pConcatenation) {
      ElementaryCoveragePattern lFirstSubpattern = pConcatenation.getFirstSubpattern().accept(this);
      ElementaryCoveragePattern lSecondSubpattern = pConcatenation.getSecondSubpattern().accept(this);
      
      return new ECPConcatenation(lFirstSubpattern, lSecondSubpattern);
    }

    @Override
    public ElementaryCoveragePattern visit(Repetition pRepetition) {
      ElementaryCoveragePattern lSubpattern = pRepetition.getSubpattern().accept(this);
      
      return new ECPRepetition(lSubpattern);
    }

    @Override
    public ECPUnion visit(Union pUnion) {
      ElementaryCoveragePattern lFirstSubpattern = pUnion.getFirstSubpattern().accept(this);
      ElementaryCoveragePattern lSecondSubpattern = pUnion.getSecondSubpattern().accept(this);
      
      return new ECPUnion(lFirstSubpattern, lSecondSubpattern);
    }

    @Override
    public ECPEdgeSet visit(Edges pEdges) {
      TargetGraph lFilteredTargetGraph = mTargetGraph.apply(pEdges.getFilter());

      Set<CFAEdge> lCFAEdges = new HashSet<CFAEdge>();

      for (Edge lEdge : lFilteredTargetGraph.getEdges()) {
        
        // TODO add support for predicates
        if (!lEdge.getSource().getPredicates().isEmpty() || !lEdge.getTarget().getPredicates().isEmpty()) {
          throw new UnsupportedOperationException();
        }
        
        lCFAEdges.add(lEdge.getCFAEdge());
      }
      
      return new ECPEdgeSet(lCFAEdges);
    }

    @Override
    public ECPNodeSet visit(Nodes pNodes) {
      TargetGraph lFilteredTargetGraph = mTargetGraph.apply(pNodes.getFilter());

      Set<CFANode> lCFANodes = new HashSet<CFANode>();
      
      for (Node lNode : lFilteredTargetGraph.getNodes()) {
        
        // TODO add support for predicates
        if (!lNode.getPredicates().isEmpty()) {
          throw new UnsupportedOperationException();
        }
        
        lCFANodes.add(lNode.getCFANode());
      }
      
      return new ECPNodeSet(lCFANodes);
    }

    @Override
    public ElementaryCoveragePattern visit(Paths pPaths) {
      // TODO add support for path specifications
      throw new UnsupportedOperationException();
    }

    @Override
    public ECPPredicate visit(Predicate pPredicate) {
      return new ECPPredicate(pPredicate.getPredicate());
    }
    
  }
  
}
