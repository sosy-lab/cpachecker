package org.sosy_lab.cpachecker.fllesh.fql2.translators.ecp;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.fllesh.ecp.ECPConcatenation;
import org.sosy_lab.cpachecker.fllesh.ecp.ECPEdgeSet;
import org.sosy_lab.cpachecker.fllesh.ecp.ECPNodeSet;
import org.sosy_lab.cpachecker.fllesh.ecp.ECPPredicate;
import org.sosy_lab.cpachecker.fllesh.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph.Edge;
import org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph.Node;
import org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph.TargetGraph;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.Edges;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.Nodes;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.Paths;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.Predicate;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.coveragespecification.Concatenation;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.coveragespecification.CoverageSpecification;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.coveragespecification.CoverageSpecificationVisitor;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.coveragespecification.Quotation;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.coveragespecification.Union;

public class CoverageSpecificationToECPTranslator {

  private Visitor mVisitor;
  private PathPatternToECPTranslator mPathPatternTranslator;
  
  public CoverageSpecificationToECPTranslator(TargetGraph pTargetGraph) {
    mVisitor = new Visitor();
    mPathPatternTranslator = new PathPatternToECPTranslator(pTargetGraph);
  }
  
  public Set<ElementaryCoveragePattern> translate(CoverageSpecification pCoverageSpecification) {
    return pCoverageSpecification.accept(mVisitor);
  }
  
  private class Visitor implements CoverageSpecificationVisitor<Set<ElementaryCoveragePattern>> {

    @Override
    public Set<ElementaryCoveragePattern> visit(Concatenation pConcatenation) {
      Set<ElementaryCoveragePattern> lPrefixSet = pConcatenation.getFirstSubspecification().accept(this);
      Set<ElementaryCoveragePattern> lSuffixSet = pConcatenation.getSecondSubspecification().accept(this);
      
      HashSet<ElementaryCoveragePattern> lResultSet = new HashSet<ElementaryCoveragePattern>();
      
      for (ElementaryCoveragePattern lPrefix : lPrefixSet) {
        for (ElementaryCoveragePattern lSuffix : lSuffixSet) {
          lResultSet.add(new ECPConcatenation(lPrefix, lSuffix));
        }
      }
      
      return lResultSet;
    }

    @Override
    public Set<ElementaryCoveragePattern> visit(Quotation pQuotation) {
      ElementaryCoveragePattern lPattern = mPathPatternTranslator.translate(pQuotation.getPathPattern());
      
      return Collections.singleton(lPattern);
    }

    @Override
    public Set<ElementaryCoveragePattern> visit(Union pUnion) {
      Set<ElementaryCoveragePattern> lResultSet = new HashSet<ElementaryCoveragePattern>();
      
      lResultSet.addAll(pUnion.getFirstSubspecification().accept(this));
      lResultSet.addAll(pUnion.getSecondSubspecification().accept(this));
      
      return lResultSet;
    }

    @Override
    public Set<ElementaryCoveragePattern> visit(Edges pEdges) {
      TargetGraph lFilteredTargetGraph = mPathPatternTranslator.getTargetGraph().apply(pEdges.getFilter());

      Set<ElementaryCoveragePattern> lResultSet = new HashSet<ElementaryCoveragePattern>();

      for (Edge lEdge : lFilteredTargetGraph.getEdges()) {
        
        // TODO add support for predicates
        if (!lEdge.getSource().getPredicates().isEmpty() || !lEdge.getTarget().getPredicates().isEmpty()) {
          throw new UnsupportedOperationException();
        }
        
        lResultSet.add(new ECPEdgeSet(lEdge.getCFAEdge()));
      }
      
      return lResultSet;
    }

    @Override
    public Set<ElementaryCoveragePattern> visit(Nodes pNodes) {
      TargetGraph lFilteredTargetGraph = mPathPatternTranslator.getTargetGraph().apply(pNodes.getFilter());

      Set<ElementaryCoveragePattern> lResultSet = new HashSet<ElementaryCoveragePattern>();

      for (Node lNode : lFilteredTargetGraph.getNodes()) {
        
        // TODO add support for predicates
        if (!lNode.getPredicates().isEmpty()) {
          throw new UnsupportedOperationException();
        }
        
        lResultSet.add(new ECPNodeSet(lNode.getCFANode()));
      }

      return lResultSet;
    }

    @Override
    public Set<ElementaryCoveragePattern> visit(Paths pPaths) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<ElementaryCoveragePattern> visit(Predicate pPredicate) {
      ElementaryCoveragePattern lPattern = new ECPPredicate(pPredicate.getPredicate());
      
      return Collections.singleton(lPattern);
    }
    
  }
  
}
