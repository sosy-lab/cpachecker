package fllesh.fql2.ast.coveragespecification;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAFunctionDefinitionNode;

import fllesh.cpa.edgevisit.Annotations;
import fllesh.ecp.reduced.Atom;
import fllesh.ecp.reduced.Pattern;
import fllesh.fql.backend.targetgraph.Edge;
import fllesh.fql.backend.targetgraph.TargetGraph;
import fllesh.fql.frontend.ast.filter.Filter;
import fllesh.fql2.ast.Edges;

public class Translator {

  private Visitor mVisitor;
  private TargetGraph mTargetGraph;
  private fllesh.fql2.ast.pathpattern.Translator mPathPatternTranslator;
  
  public Translator(CFAFunctionDefinitionNode pMainFunction) {
    mVisitor = new Visitor();
    
    mTargetGraph = TargetGraph.createTargetGraphFromCFA(pMainFunction);
    mPathPatternTranslator = new fllesh.fql2.ast.pathpattern.Translator(mTargetGraph);
  }
  
  public Annotations getAnnotations() {
    return mPathPatternTranslator;
  }
  
  public Set<Pattern> translate(CoverageSpecification pSpecification) {
    return pSpecification.accept(mVisitor);
  }
  
  private class Visitor implements ASTVisitor<Set<Pattern>> {

    @Override
    public Set<Pattern> visit(Concatenation pConcatenation) {
      Set<Pattern> lResultSet = new HashSet<Pattern>();
      
      Set<Pattern> lPrefixSet = pConcatenation.getFirstSubspecification().accept(this);
      Set<Pattern> lSuffixSet = pConcatenation.getSecondSubspecification().accept(this);
      
      for (Pattern lPrefix : lPrefixSet) {
        for (Pattern lSuffix : lSuffixSet) {
          Pattern lConcatenation = new fllesh.ecp.reduced.Concatenation(lPrefix, lSuffix);
          lResultSet.add(lConcatenation);
        }
      }
      
      return lResultSet;
    }

    @Override
    public Set<Pattern> visit(Quotation pQuotation) {
      Pattern pPattern = mPathPatternTranslator.translate(pQuotation.getPathPattern());
      
      return Collections.singleton(pPattern);
    }

    @Override
    public Set<Pattern> visit(Union pUnion) {
      Set<Pattern> lResultSet = new HashSet<Pattern>();
      
      Set<Pattern> lFirstSet = pUnion.getFirstSubspecification().accept(this);
      Set<Pattern> lSecondSet = pUnion.getSecondSubspecification().accept(this);
      
      lResultSet.addAll(lFirstSet);
      lResultSet.addAll(lSecondSet);
      
      return lResultSet;
    }

    @Override
    public Set<Pattern> visit(Edges pEdges) {
      Set<Pattern> lResultSet = new HashSet<Pattern>();
      
      Filter lFilter = pEdges.getFilter();
      
      TargetGraph lFilteredTargetGraph = mTargetGraph.apply(lFilter);
      
      for (Edge lEdge : lFilteredTargetGraph.getEdges()) {
        CFAEdge lCFAEdge = lEdge.getCFAEdge();
        
        lResultSet.add(new Atom(mPathPatternTranslator.getId(lCFAEdge)));
      }
      
      return lResultSet;
    }
    
  }
  
}
