package cpa.common.algorithm.cbmctools;

import java.util.ArrayList;
import java.util.List;

import cpa.art.ARTElement;

public class CBMCLabelElement {

  private static List<String> globalLabelsMap = new ArrayList<String>();
  private static List<String> writtenLabelsMap = new ArrayList<String>();
  private String labelEdge;
  private int nextArtElemId;

  public CBMCLabelElement(String pLabelEdge, ARTElement pNextArtElement) {
    labelEdge = pLabelEdge;
    nextArtElemId = pNextArtElement.getElementId();
    if(labelEdge.contains("Label:")){
      globalLabelsMap.add((labelEdge.replace("Label: ", "")).concat("_"+ nextArtElemId));
    }
  }

  public String getCode(){
    if(labelEdge.contains("Goto:")){
      String label = labelEdge.replace("Goto: ", "");
      label = label + "_"+ nextArtElemId;
      if(globalLabelsMap.contains(label.concat("_"+ nextArtElemId))){
        return "goto " + label + ";\n";
      }
      else{
        if(!writtenLabelsMap.contains(label)){
          writtenLabelsMap.add(label);
          return "goto " + label + ";\n" + label  + ": ;\n";
        }
      }
    }
    else if(labelEdge.contains("Label:")){
      String label = labelEdge.replace("Label: ", "");
      label = label + "_"+ nextArtElemId;
      writtenLabelsMap.add(label);
      return label  + ": ;\n";
    }
    return "";
  }
}
