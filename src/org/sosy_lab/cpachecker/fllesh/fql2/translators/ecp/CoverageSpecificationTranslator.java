package org.sosy_lab.cpachecker.fllesh.fql2.translators.ecp;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.fllesh.ecp.ECPConcatenation;
import org.sosy_lab.cpachecker.fllesh.ecp.ECPPredicate;
import org.sosy_lab.cpachecker.fllesh.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.fllesh.targetgraph.Edge;
import org.sosy_lab.cpachecker.fllesh.targetgraph.Node;
import org.sosy_lab.cpachecker.fllesh.targetgraph.Path;
import org.sosy_lab.cpachecker.fllesh.targetgraph.TargetGraph;
import org.sosy_lab.cpachecker.fllesh.targetgraph.TargetGraphUtil;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.Edges;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.Nodes;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.Paths;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.Predicate;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.coveragespecification.Concatenation;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.coveragespecification.CoverageSpecification;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.coveragespecification.CoverageSpecificationVisitor;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.coveragespecification.Quotation;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.coveragespecification.Union;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.pathpattern.PathPattern;

public class CoverageSpecificationTranslator {

  private Visitor mVisitor;
  private PathPatternTranslator mPathPatternTranslator;
  
  public CoverageSpecificationTranslator(CFANode pInitialNode) {
    this(TargetGraphUtil.cfa(pInitialNode), TargetGraphUtil.getBasicBlockEntries(pInitialNode));
  }
  
  public CoverageSpecificationTranslator(TargetGraph pTargetGraph, Set<CFAEdge> pBasicBlockEntries) {
    mVisitor = new Visitor();
    mPathPatternTranslator = new PathPatternTranslator(pTargetGraph, pBasicBlockEntries);
  }
  
  public CoverageSpecificationTranslator(PathPatternTranslator pPatternTranslator) {
    mVisitor = new Visitor();
    mPathPatternTranslator = pPatternTranslator;
  }
  
  public Set<ElementaryCoveragePattern> translate(CoverageSpecification pCoverageSpecification) {
    return pCoverageSpecification.accept(mVisitor);
  }
  
  public ElementaryCoveragePattern translate(PathPattern pPathPattern) {
    return mPathPatternTranslator.translate(pPathPattern);
  }
  
  private class Visitor implements CoverageSpecificationVisitor<Set<ElementaryCoveragePattern>> {

    @Override
    public Set<ElementaryCoveragePattern> visit(Concatenation pConcatenation) {
      Set<ElementaryCoveragePattern> lPrefixSet = pConcatenation.getFirstSubspecification().accept(this);
      Set<ElementaryCoveragePattern> lSuffixSet = pConcatenation.getSecondSubspecification().accept(this);
      
      HashSet<ElementaryCoveragePattern> lResultSet = new LinkedHashSet<ElementaryCoveragePattern>();
      
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
      Set<ElementaryCoveragePattern> lResultSet = new LinkedHashSet<ElementaryCoveragePattern>();
      
      lResultSet.addAll(pUnion.getFirstSubspecification().accept(this));
      lResultSet.addAll(pUnion.getSecondSubspecification().accept(this));
      
      return lResultSet;
    }

    @Override
    public Set<ElementaryCoveragePattern> visit(Edges pEdges) {
      TargetGraph lFilteredTargetGraph = mPathPatternTranslator.getFilterEvaluator().evaluate(pEdges.getFilter());

      Set<ElementaryCoveragePattern> lResultSet = new LinkedHashSet<ElementaryCoveragePattern>();

      for (Edge lEdge : lFilteredTargetGraph.getEdges()) {
        lResultSet.add(mPathPatternTranslator.translate(lEdge));
      }
      
      return lResultSet;
    }

    @Override
    public Set<ElementaryCoveragePattern> visit(Nodes pNodes) {
      TargetGraph lFilteredTargetGraph = mPathPatternTranslator.getFilterEvaluator().evaluate(pNodes.getFilter());

      Set<ElementaryCoveragePattern> lResultSet = new LinkedHashSet<ElementaryCoveragePattern>();

      for (Node lNode : lFilteredTargetGraph.getNodes()) {
        lResultSet.add(mPathPatternTranslator.translate(lNode));
      }

      return lResultSet;
    }

    @Override
    public Set<ElementaryCoveragePattern> visit(Paths pPaths) {
      TargetGraph lFilteredTargetGraph = mPathPatternTranslator.getFilterEvaluator().evaluate(pPaths.getFilter());
      
      Set<ElementaryCoveragePattern> lResultSet = new LinkedHashSet<ElementaryCoveragePattern>();
      
      for (Path lPath : lFilteredTargetGraph.getBoundedPaths(pPaths.getBound())) {
        lResultSet.add(mPathPatternTranslator.translate(lPath));
      }
      
      return lResultSet;
    }
    
    @Override
    public Set<ElementaryCoveragePattern> visit(Predicate pPredicate) {
      ElementaryCoveragePattern lPattern = new ECPPredicate(pPredicate);
      
      return Collections.singleton(lPattern);
    }
    
  }
  
}
