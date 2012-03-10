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
package org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.interpolation;

import java.util.Deque;
import java.util.LinkedList;



public class DOTTreeBuilder {

  private static String graphName  = "G";
  private static String envColor   = "chocolate";
  private static String errorColor = "red";
  private static String nonAbstractionColor = "lightgrey";
  private static String normalNodeColor = "white";


  public static String generateDOT(InterpolationTreeNode root){

    StringBuilder sb = new StringBuilder();

    sb.append("digraph "+graphName+" {\n");
    sb.append("\tnode [style=filled];\n\n");

    Deque<InterpolationTreeNode> queue = new LinkedList<InterpolationTreeNode>();
    if (root != null){
      queue.add(root);
    }

    // construct the tree from the root to the leafs
    while(!queue.isEmpty()){
      InterpolationTreeNode node = queue.pop();
      String nodeStr = generateARTElement(node);

      for (InterpolationTreeNode child : node.children){
        queue.addLast(child);
        String childStr = generateARTElement(child);

        sb.append("\t");
        sb.append(nodeStr);
        sb.append(" -> ");
        sb.append(childStr);

        // color for env. edges
        if (node.tid != child.tid){
          sb.append(" [color="+envColor+"]");
        }
        sb.append(";\n");
      }

      // color for roots and non-abstraction points
      if (node.parent == null){
        sb.append("\t");
        sb.append(nodeStr);
        sb.append(" [fillcolor="+errorColor+"]");
        sb.append(";\n");
      } else if (!node.isARTAbstraction){
        sb.append("\t");
        sb.append(nodeStr);
        sb.append(" [fillcolor="+nonAbstractionColor+"]");
        sb.append(";\n");
      } else {
        sb.append("\t");
        sb.append(nodeStr);
        sb.append(" [fillcolor="+normalNodeColor+"]");
        sb.append(";\n");
      }

    }

    sb.append("}");
    return sb.toString();
  }

  private static String generateARTElement(InterpolationTreeNode node) {
    String str = "\"("+node.artElement.getElementId()+","+node.uniqueId+")\"";
    return  str;
  }


}

