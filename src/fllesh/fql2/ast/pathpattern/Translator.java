package fllesh.fql2.ast.pathpattern;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import cfa.objectmodel.CFAEdge;

import fllesh.cpa.edgevisit.Annotations;
import fllesh.ecp.reduced.Pattern;
import fllesh.fql.backend.targetgraph.Edge;
import fllesh.fql.backend.targetgraph.TargetGraph;
import fllesh.fql2.ast.Edges;

public class Translator implements ASTVisitor<Pattern>, Annotations {

  private TargetGraph mTargetGraph;
  private Map<Set<CFAEdge>, String> mIds;
  private Map<CFAEdge, Set<String>> mAnnotations;
  private Map<CFAEdge, String> mMapping;
  private Set<CFAEdge> mCFAEdges;
  
  public Translator(TargetGraph pTargetGraph) {
    mTargetGraph = pTargetGraph;
    mIds = new HashMap<Set<CFAEdge>, String>();
    mAnnotations = new HashMap<CFAEdge, Set<String>>();
    mMapping = new HashMap<CFAEdge, String>();
    
    mCFAEdges = new HashSet<CFAEdge>();
    
    for (Edge lEdge : mTargetGraph.getEdges()) {
      mCFAEdges.add(lEdge.getCFAEdge());
    }
  }
  
  public Set<CFAEdge> getCFAEdges() {
    return mCFAEdges;
  }
  
  public Set<String> getAnnotations(CFAEdge pEdge) {
    return getOrCreateAnnotationEntry(pEdge);
  }
  
  @Override
  public Pattern visit(Concatenation pConcatenation) {
    Pattern lFirstSubpattern = pConcatenation.getFirstSubpattern().accept(this);
    Pattern lSecondSubpattern = pConcatenation.getSecondSubpattern().accept(this);
    
    return new fllesh.ecp.reduced.Concatenation(lFirstSubpattern, lSecondSubpattern);
  }

  @Override
  public Pattern visit(Repetition pRepetition) {
    Pattern lSubpattern = pRepetition.getSubpattern().accept(this);
    
    return new fllesh.ecp.reduced.Repetition(lSubpattern);
  }

  @Override
  public Pattern visit(Union pUnion) {
    Pattern lFirstSubpattern = pUnion.getFirstSubpattern().accept(this);
    Pattern lSecondSubpattern = pUnion.getSecondSubpattern().accept(this);
    
    return new fllesh.ecp.reduced.Union(lFirstSubpattern, lSecondSubpattern);
  }

  @Override
  public Pattern visit(Edges pEdges) {
    TargetGraph lFilteredTargetGraph = mTargetGraph.apply(pEdges.getFilter());

    Set<CFAEdge> lCFAEdges = new HashSet<CFAEdge>();
    
    for (Edge lEdge : lFilteredTargetGraph.getEdges()) {
      lCFAEdges.add(lEdge.getCFAEdge());
    }
    
    Pattern lAtom = new fllesh.ecp.reduced.Atom(getId(lCFAEdges));
    
    return lAtom;
  }
  
  private String getId(Set<CFAEdge> pCFAEdges) {
    if (!mIds.containsKey(pCFAEdges)) {
      String lId = "T" + mIds.size();
      
      mIds.put(pCFAEdges, lId);
      
      for (CFAEdge lEdge : pCFAEdges) {
        annotate(lEdge, lId);
      }
      
      return lId;
    }
    else {
      return mIds.get(pCFAEdges);
    }
  }
  
  public void annotate(CFAEdge pEdge, String pAnnotation) {
    getOrCreateAnnotationEntry(pEdge).add(pAnnotation);
  }
  
  private Set<String> getOrCreateAnnotationEntry(CFAEdge pEdge) {
    if (mAnnotations.containsKey(pEdge)) {
      return mAnnotations.get(pEdge);
    }
    else {
      Set<String> lAnnotations = new HashSet<String>();
      mAnnotations.put(pEdge, lAnnotations);
      return lAnnotations;
    }
  }

  @Override
  public String getId(CFAEdge pEdge) {
    if (mMapping.containsKey(pEdge)) {
      return mMapping.get(pEdge);
    }
    else {
      String lId = "E" + mMapping.size();
      mMapping.put(pEdge, lId);
      return lId;
    }
  }
  
  @Override
  public String toString() {
    StringWriter lResult = new StringWriter();
    PrintWriter lWriter = new PrintWriter(lResult);
    
    for (Entry<CFAEdge, String> lEntry : mMapping.entrySet())  {
      lWriter.println(lEntry.getKey().toString() + " : " + lEntry.getValue());
    }
    
    return lResult.toString();
  }

}
