package org.sosy_lab.cpachecker.util.ecp;

import java.util.HashMap;
import java.util.Map;

public class ECPPrettyPrinter {

  private Visitor mVisitor = new Visitor();
  
  public ECPPrettyPrinter() {

  }
  
  public String printPretty(ElementaryCoveragePattern pPattern) {
    String lPatternString = pPattern.accept(mVisitor);
    
    StringBuffer lResult = new StringBuffer();
    
    for (Map.Entry<ECPEdgeSet, Integer> lEntry : mVisitor.mEdgeSetIds.entrySet()) {
      lResult.append("E" + lEntry.getValue() + ": " + lEntry.getKey().toString());
      lResult.append("\n");
    }
    
    for (Map.Entry<ECPNodeSet, Integer> lEntry : mVisitor.mNodeSetIds.entrySet()) {
      lResult.append("N" + lEntry.getValue() + ": " + lEntry.getKey().toString());
      lResult.append("\n");
    }
    
    lResult.append(lPatternString);
    
    return lResult.toString();
  }
  
  private static class Visitor implements ECPVisitor<String> {
    
    private Map<ECPEdgeSet, Integer> mEdgeSetIds;
    private Map<ECPNodeSet, Integer> mNodeSetIds;
    
    public Visitor() {
      mEdgeSetIds = new HashMap<ECPEdgeSet, Integer>();
      mNodeSetIds = new HashMap<ECPNodeSet, Integer>();
    }
    
    private int getEdgeSetId(ECPEdgeSet pEdgeSet) {
      if (mEdgeSetIds.containsKey(pEdgeSet)) {
        return mEdgeSetIds.get(pEdgeSet);
      }
      else {
        int lId = mEdgeSetIds.size();
        mEdgeSetIds.put(pEdgeSet, lId);
        return lId;
      }
    }
    
    private int getNodeSetId(ECPNodeSet pNodeSet) {
      if (mNodeSetIds.containsKey(pNodeSet)) {
        return mNodeSetIds.get(pNodeSet);
      }
      else {
        int lId = mNodeSetIds.size();
        mNodeSetIds.put(pNodeSet, lId);
        return lId;
      }
    }
    
    @Override
    public String visit(ECPEdgeSet pEdgeSet) {
      return "E" + getEdgeSetId(pEdgeSet);
    }

    @Override
    public String visit(ECPNodeSet pNodeSet) {
      return "N" + getNodeSetId(pNodeSet);
    }

    @Override
    public String visit(ECPPredicate pPredicate) {
      return "{ " + pPredicate.toString() + " }";
    }

    @Override
    public String visit(ECPConcatenation pConcatenation) {
      StringBuffer lResult = new StringBuffer();
      
      boolean isFirst = true;
      
      for (ElementaryCoveragePattern lSubpattern : pConcatenation) {
        if (isFirst) {
          isFirst = false;
        }
        else {
          lResult.append(".");
        }
        
        if (lSubpattern instanceof ECPUnion) {
          lResult.append("(");
        }
        
        lResult.append(lSubpattern.accept(this));
        
        if (lSubpattern instanceof ECPUnion) {
          lResult.append(")");
        }
      }
      
      return lResult.toString();
    }

    @Override
    public String visit(ECPUnion pUnion) {
      StringBuffer lResult = new StringBuffer();
      
      boolean isFirst = true;
      
      for (ElementaryCoveragePattern lSubpattern : pUnion) {
        if (isFirst) {
          isFirst = false;
        }
        else {
          lResult.append(" + ");
        }
        
        if (lSubpattern instanceof ECPConcatenation) {
          lResult.append("(");
        }
        
        lResult.append(lSubpattern.accept(this));
        
        if (lSubpattern instanceof ECPConcatenation) {
          lResult.append(")");
        }
      }
      
      return lResult.toString();
    }

    @Override
    public String visit(ECPRepetition pRepetition) {
      if (pRepetition.getSubpattern() instanceof ECPAtom) {
        return pRepetition.getSubpattern().accept(this) + "*";
      }
      else {
        return "(" + pRepetition.getSubpattern().accept(this) + ")*";
      }
    }
  }

}
