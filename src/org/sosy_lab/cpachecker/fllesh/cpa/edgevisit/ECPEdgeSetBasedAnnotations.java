package org.sosy_lab.cpachecker.fllesh.cpa.edgevisit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.fllesh.ecp.ECPEdgeSet;
import org.sosy_lab.cpachecker.fllesh.ecp.translators.observerautomaton.ToControlAutomatonTranslator.EdgeSetNameMap;

public class ECPEdgeSetBasedAnnotations {

  private Map<CFAEdge, Set<String>> mAnnotations;
  private EdgeSetNameMap mEdgeSetNameMap;
  
  public ECPEdgeSetBasedAnnotations(EdgeSetNameMap pEdgeSetNameMap) {
    mAnnotations = new HashMap<CFAEdge, Set<String>>();
    mEdgeSetNameMap = pEdgeSetNameMap;
  }
  
  public void add(ECPEdgeSet pEdgeSet) {
    String lAnnotation = mEdgeSetNameMap.get(pEdgeSet);
    
    for (CFAEdge lCFAEdge : pEdgeSet) {
      Set<String> lAnnotations = getAnnotations(lCFAEdge);
      lAnnotations.add(lAnnotation);
    }
  }
  
  public Set<String> getAnnotations(CFAEdge pCFAEdge) {
    if (mAnnotations.containsKey(pCFAEdge)) {
      return mAnnotations.get(pCFAEdge);
    }
    else {
      Set<String> lAnnotations = new HashSet<String>();
      mAnnotations.put(pCFAEdge, lAnnotations);
      return lAnnotations;
    }
  }
  
}
