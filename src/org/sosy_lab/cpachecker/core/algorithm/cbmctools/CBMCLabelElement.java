/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.cbmctools;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.cpa.art.ARTElement;

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
