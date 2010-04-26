/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.fllesh.fql2.ast.pathpattern;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;

import org.sosy_lab.cpachecker.fllesh.cpa.edgevisit.Annotations;
import org.sosy_lab.cpachecker.fllesh.ecp.reduced.Pattern;
import org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph.Edge;
import org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph.TargetGraph;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.Edges;

public class Translator implements Annotations {
  
  private TargetGraph mTargetGraph;
  private Map<Set<CFAEdge>, String> mIds;
  private Map<CFAEdge, Set<String>> mAnnotations;
  private Map<CFAEdge, String> mMapping;
  private Set<CFAEdge> mCFAEdges;
  private Visitor mVisitor;
  
  public Translator(TargetGraph pTargetGraph) {
    mTargetGraph = pTargetGraph;
    mIds = new HashMap<Set<CFAEdge>, String>();
    mAnnotations = new HashMap<CFAEdge, Set<String>>();
    mMapping = new HashMap<CFAEdge, String>();
    
    mCFAEdges = new HashSet<CFAEdge>();
    
    for (Edge lEdge : mTargetGraph.getEdges()) {
      mCFAEdges.add(lEdge.getCFAEdge());
    }
    
    mVisitor = new Visitor();
  }
  
  public Set<CFAEdge> getCFAEdges() {
    return mCFAEdges;
  }
  
  public Set<String> getAnnotations(CFAEdge pEdge) {
    return getOrCreateAnnotationEntry(pEdge);
  }
  
  public Pattern translate(PathPattern pPattern) {
    return pPattern.accept(mVisitor);
  }
  
  private class Visitor implements ASTVisitor<Pattern> {

    @Override
    public Pattern visit(Concatenation pConcatenation) {
      Pattern lFirstSubpattern = pConcatenation.getFirstSubpattern().accept(this);
      Pattern lSecondSubpattern = pConcatenation.getSecondSubpattern().accept(this);
      
      return new org.sosy_lab.cpachecker.fllesh.ecp.reduced.Concatenation(lFirstSubpattern, lSecondSubpattern);
    }

    @Override
    public Pattern visit(Repetition pRepetition) {
      Pattern lSubpattern = pRepetition.getSubpattern().accept(this);
      
      return new org.sosy_lab.cpachecker.fllesh.ecp.reduced.Repetition(lSubpattern);
    }

    @Override
    public Pattern visit(Union pUnion) {
      Pattern lFirstSubpattern = pUnion.getFirstSubpattern().accept(this);
      Pattern lSecondSubpattern = pUnion.getSecondSubpattern().accept(this);
      
      return new org.sosy_lab.cpachecker.fllesh.ecp.reduced.Union(lFirstSubpattern, lSecondSubpattern);
    }

    @Override
    public Pattern visit(Edges pEdges) {
      TargetGraph lFilteredTargetGraph = mTargetGraph.apply(pEdges.getFilter());

      Set<CFAEdge> lCFAEdges = new HashSet<CFAEdge>();
      
      for (Edge lEdge : lFilteredTargetGraph.getEdges()) {
        lCFAEdges.add(lEdge.getCFAEdge());
      }
      
      Pattern lAtom = new org.sosy_lab.cpachecker.fllesh.ecp.reduced.Atom(getId(lCFAEdges));
      
      return lAtom;
    }

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
    mCFAEdges.add(pEdge);
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
    mCFAEdges.add(pEdge);
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
    
    for (CFAEdge lEdge : this.getCFAEdges()) {
      lWriter.println(lEdge);
    }
    
    for (Entry<CFAEdge, String> lEntry : mMapping.entrySet())  {
      lWriter.println(lEntry.getKey().toString() + " : " + lEntry.getValue());
    }
    
    for (Entry<CFAEdge, Set<String>> lEntry :  mAnnotations.entrySet()) {
      lWriter.println(lEntry);
    }
    
    return lResult.toString();
  }

}
