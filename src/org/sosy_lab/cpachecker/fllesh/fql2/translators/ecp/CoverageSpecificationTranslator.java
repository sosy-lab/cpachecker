package org.sosy_lab.cpachecker.fllesh.fql2.translators.ecp;

import java.util.Collections;
import java.util.HashMap;
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

  private final Visitor mVisitor;
  private final PathPatternTranslator mPathPatternTranslator;
  private final HashMap<CoverageSpecification, Set<ElementaryCoveragePattern>> mResultCache;
  
  public CoverageSpecificationTranslator(CFANode pInitialNode) {
    this(TargetGraphUtil.cfa(pInitialNode), TargetGraphUtil.getBasicBlockEntries(pInitialNode));
  }
  
  public CoverageSpecificationTranslator(TargetGraph pTargetGraph, Set<CFAEdge> pBasicBlockEntries) {
    this (new PathPatternTranslator(pTargetGraph, pBasicBlockEntries));
  }
  
  public CoverageSpecificationTranslator(PathPatternTranslator pPatternTranslator) {
    mVisitor = new Visitor();
    mPathPatternTranslator = pPatternTranslator;
    mResultCache = new HashMap<CoverageSpecification, Set<ElementaryCoveragePattern>>();
  }
  
  public int getOverallCacheHits() {
    return mVisitor.mCacheHits + mPathPatternTranslator.getCacheHits(); 
  }
  
  public int getOverallCacheMisses() {
    return mVisitor.mCacheMisses + mPathPatternTranslator.getCacheMisses();
  }
  
  public Set<ElementaryCoveragePattern> translate(CoverageSpecification pCoverageSpecification) {
    return pCoverageSpecification.accept(mVisitor);
  }
  
  public ElementaryCoveragePattern translate(PathPattern pPathPattern) {
    return mPathPatternTranslator.translate(pPathPattern);
  }
  
  private class Visitor implements CoverageSpecificationVisitor<Set<ElementaryCoveragePattern>> {

    public int mCacheHits = 0;
    public int mCacheMisses = 0;
    
    @Override
    public Set<ElementaryCoveragePattern> visit(Concatenation pConcatenation) {
      Set<ElementaryCoveragePattern> lResultSet = mResultCache.get(pConcatenation);
      
      if (lResultSet == null) {
        Set<ElementaryCoveragePattern> lPrefixSet = pConcatenation.getFirstSubspecification().accept(this);
        Set<ElementaryCoveragePattern> lSuffixSet = pConcatenation.getSecondSubspecification().accept(this);
        
        lResultSet = new LinkedHashSet<ElementaryCoveragePattern>();
        
        for (ElementaryCoveragePattern lPrefix : lPrefixSet) {
          for (ElementaryCoveragePattern lSuffix : lSuffixSet) {
            lResultSet.add(new ECPConcatenation(lPrefix, lSuffix));
          }
        }
        
        mResultCache.put(pConcatenation, lResultSet);
        mCacheMisses++;
      }
      else {
        mCacheHits++;
      }
      
      return lResultSet;
    }

    @Override
    public Set<ElementaryCoveragePattern> visit(Quotation pQuotation) {
      Set<ElementaryCoveragePattern> lResultSet = mResultCache.get(pQuotation);
      
      if (lResultSet == null) {
        ElementaryCoveragePattern lPattern = mPathPatternTranslator.translate(pQuotation.getPathPattern());
        
        lResultSet = Collections.singleton(lPattern);
        
        mResultCache.put(pQuotation, lResultSet);
        mCacheMisses++;
      }
      else {
        mCacheHits++;
      }
      
      return lResultSet;
    }

    @Override
    public Set<ElementaryCoveragePattern> visit(Union pUnion) {
      Set<ElementaryCoveragePattern> lResultSet = mResultCache.get(pUnion);
      
      if (lResultSet == null) {
        lResultSet = new LinkedHashSet<ElementaryCoveragePattern>();

        lResultSet.addAll(pUnion.getFirstSubspecification().accept(this));
        lResultSet.addAll(pUnion.getSecondSubspecification().accept(this));
        
        mResultCache.put(pUnion, lResultSet);
        mCacheMisses++;
      }
      else {
        mCacheHits++;
      }
      
      return lResultSet;
    }

    @Override
    public Set<ElementaryCoveragePattern> visit(Edges pEdges) {
      TargetGraph lFilteredTargetGraph = mPathPatternTranslator.getFilterEvaluator().evaluate(pEdges.getFilter());

      Set<ElementaryCoveragePattern> lResultSet = mResultCache.get(pEdges);
      
      if (lResultSet == null) {
        lResultSet = new LinkedHashSet<ElementaryCoveragePattern>();

        for (Edge lEdge : lFilteredTargetGraph.getEdges()) {
          ElementaryCoveragePattern lPattern = mPathPatternTranslator.translate(lEdge);
          lResultSet.add(lPattern);
        }
        
        mResultCache.put(pEdges, lResultSet);
        mCacheMisses++;
      }
      else {
        mCacheHits++;
      }
            
      return lResultSet;
    }

    @Override
    public Set<ElementaryCoveragePattern> visit(Nodes pNodes) {
      TargetGraph lFilteredTargetGraph = mPathPatternTranslator.getFilterEvaluator().evaluate(pNodes.getFilter());

      Set<ElementaryCoveragePattern> lResultSet = mResultCache.get(pNodes);
      
      if (lResultSet == null) {
        lResultSet = new LinkedHashSet<ElementaryCoveragePattern>();

        for (Node lNode : lFilteredTargetGraph.getNodes()) {
          lResultSet.add(mPathPatternTranslator.translate(lNode));
        }

        mResultCache.put(pNodes, lResultSet);
        mCacheMisses++;
      }
      else {
        mCacheHits++;
      }
      
      return lResultSet;
    }

    @Override
    public Set<ElementaryCoveragePattern> visit(Paths pPaths) {
      TargetGraph lFilteredTargetGraph = mPathPatternTranslator.getFilterEvaluator().evaluate(pPaths.getFilter());
      
      Set<ElementaryCoveragePattern> lResultSet = mResultCache.get(pPaths);
      
      if (lResultSet == null) {
        lResultSet = new LinkedHashSet<ElementaryCoveragePattern>();
        
        for (Path lPath : lFilteredTargetGraph.getBoundedPaths(pPaths.getBound())) {
          lResultSet.add(mPathPatternTranslator.translate(lPath));
        }
       
        mResultCache.put(pPaths, lResultSet);
        mCacheMisses++;
      }
      else {
        mCacheHits++;
      }
      
      return lResultSet;
    }
    
    @Override
    public Set<ElementaryCoveragePattern> visit(Predicate pPredicate) {
      Set<ElementaryCoveragePattern> lResultSet = mResultCache.get(pPredicate);
      
      if (lResultSet == null) {
        ElementaryCoveragePattern lPattern = new ECPPredicate(pPredicate);
        lResultSet = Collections.singleton(lPattern);
        mResultCache.put(pPredicate, lResultSet);
        mCacheMisses++;
      }
      else {
        mCacheHits++;
      }
      
      return lResultSet;
    }
    
  }
  
}
