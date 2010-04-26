package org.sosy_lab.cpachecker.fllesh.cpa.edgevisit;

import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;

public interface Annotations {

  public Set<CFAEdge> getCFAEdges();
  
  public Set<String> getAnnotations(CFAEdge pEdge);
  public void annotate(CFAEdge pEdge, String pAnnotation);
  
  public String getId(CFAEdge pEdge);
  
}
