package org.sosy_lab.cpachecker.fshell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class FeasibilityInformation {

  enum FeasibilityStatus {
    FEASIBLE,
    INFEASIBLE,
    IMPRECISE,
    UNKNOWN
  }
  
  protected static final String FEASIBLE_ABBREV = "f";
  protected static final String INFEASIBLE_ABBREV = "if";
  protected static final String IMPRECISE_ABBREV = "ip";
  
  private final Map<Integer, FeasibilityStatus> mFeasibilityInformation;
  private String mTestsuiteFilename;
  
  public FeasibilityInformation() {
    mFeasibilityInformation = new HashMap<Integer, FeasibilityStatus>();
  }
    
  public boolean hasTestsuiteFilename() {
    return (mTestsuiteFilename != null);
  }
  
  public String getTestsuiteFilename() {
    return mTestsuiteFilename;
  }
  
  public void setTestsuiteFilename(String pFilename) {
    mTestsuiteFilename = pFilename;
  }
  
  public void write(String pFeasibilityFilename) throws FileNotFoundException {
    write(new File(pFeasibilityFilename));
  }
  
  public void write(File pFeasibilityFile) throws FileNotFoundException {
    PrintWriter lWriter = new PrintWriter(pFeasibilityFile);
    
    if (!hasTestsuiteFilename()) {
      throw new RuntimeException();
    }
    
    lWriter.println(mTestsuiteFilename);
    
    /*
     * We don't write information where UNKNOWN is the information we have.
     */
    for (Map.Entry<Integer, FeasibilityStatus> lEntry : mFeasibilityInformation.entrySet()) {
      switch (lEntry.getValue()) {
      case FEASIBLE:
        lWriter.println(lEntry.getKey() + " " + FEASIBLE_ABBREV);
        break;
      case INFEASIBLE:
        lWriter.println(lEntry.getKey() + " " + INFEASIBLE_ABBREV);
        break;
      case IMPRECISE:
        lWriter.println(lEntry.getKey() + " " + IMPRECISE_ABBREV);
        break;
      }
    }
    
    lWriter.close();
  }
  
  public static FeasibilityInformation load(String pFeasibilityFilename) throws NumberFormatException, IOException {
    return load(new File(pFeasibilityFilename));
  }
  
  public static FeasibilityInformation load(File pFeasibilityFile) throws NumberFormatException, IOException {
    FeasibilityInformation lInformation = new FeasibilityInformation();
    
    BufferedReader lReader = new BufferedReader(new FileReader(pFeasibilityFile));
    String lLine;
    
    boolean lIsFirstLine = true;
    
    while ((lLine = lReader.readLine()) != null) {
      lLine = lLine.trim();
      
      if (lLine.equals("")) {
        continue;
      }
      
      if (lIsFirstLine) {
        lIsFirstLine = false;
        
        lInformation.setTestsuiteFilename(lLine);
      }
      else {
        String[] lParts = lLine.split(" ");
        
        if (lParts.length != 2) {
          throw new RuntimeException();
        }
        
        Integer lGoalIndex = Integer.valueOf(lParts[0]);
        
        if (lParts[1].toLowerCase().equals(FEASIBLE_ABBREV)) {
          lInformation.setStatus(lGoalIndex, FeasibilityStatus.FEASIBLE);
        }
        else if (lParts[1].toLowerCase().equals(INFEASIBLE_ABBREV)) {
          lInformation.setStatus(lGoalIndex, FeasibilityStatus.INFEASIBLE);
        }
        else if (lParts[1].toLowerCase().equals(IMPRECISE_ABBREV)) {
          lInformation.setStatus(lGoalIndex, FeasibilityStatus.IMPRECISE);
        }
        else {
          throw new RuntimeException();
        }
      }
    }
    
    return lInformation;
  }
  
  public boolean isKnown(int pGoalIndex) {
    return (mFeasibilityInformation.containsKey(pGoalIndex));
  }
  
  public boolean isUnknown(int pGoalIndex) {
    return !isKnown(pGoalIndex);
  }
  
  public void setStatus(int pGoalIndex, FeasibilityStatus pStatus) {
    
    // TODO shall we remove feasibility information if information is set back to unknown?
    
    // we don't store unknown
    if (pStatus.equals(FeasibilityStatus.UNKNOWN)) {
      return;
    }
    
    mFeasibilityInformation.put(pGoalIndex, pStatus);
  }
  
  public FeasibilityStatus getStatus(int pGoalIndex) {
    FeasibilityStatus lStatus = mFeasibilityInformation.get(pGoalIndex);
    
    if (lStatus == null) {
      return FeasibilityStatus.UNKNOWN;
    }
    
    return lStatus;
  }
  
}
