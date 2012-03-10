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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

import org.sosy_lab.cpachecker.cpa.art.ARTElement;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;


public class DOTDagBuilder {

  private static String graphName  = "G";
  private static String tLabel     = "T";
  private static String[] tColors  = {"white", "lightgrey", "blue"};
  private static String envColor   = "chocolate";
  private static String errorColor = "red";


  public static String generateDOT(List<InterpolationDagNode> roots){
    StringBuilder sb = new StringBuilder();

    sb.append("digraph "+graphName+" {\n\n");

    // map tid -> roots
    Multimap<Integer, InterpolationDagNode> rootMap = HashMultimap.create();
    for (InterpolationDagNode root : roots){
      rootMap.put(root.tid, root);
    }

    // the ARTs
    for (Integer tid : rootMap.keys()){
      sb.append("subgraph cluster_"+tLabel+tid+" {\n");
      sb.append("\tnode [style=filled,fillcolor="+tColors[tid]+"];\n");
      sb.append("\tlabel = \""+tLabel+tid+"\";\n");
      Collection<InterpolationDagNode> rColl = rootMap.get(tid);
      for (InterpolationDagNode root : rColl){
        sb.append(generateART(root));
      }
      sb.append("}\n\n");
    }


    // env transitions
    for (int tid=0; tid<roots.size(); tid++){
      sb.append(generateEnv(roots.get(tid)));
    }

    sb.append("}");

    return sb.toString();
  }


  private static Object generateEnv(InterpolationDagNode root) {
    StringBuilder sb = new StringBuilder();

    Deque<InterpolationDagNode> queue = new ArrayDeque<InterpolationDagNode>();
    queue.addLast(root);

    while(!queue.isEmpty()){
      InterpolationDagNode node = queue.removeFirst();
      for (InterpolationDagNode child : node.getChildren()){
        if (child.getTid() == root.getTid()){

          queue.addLast(child);
        } else {
          sb.append("\t");
          sb.append(generateARTElement(node.getArtElement()));
          sb.append(" -> ");
          sb.append(generateARTElement(child.getArtElement()));
          sb.append("[color="+envColor+"];\n");
        }
      }
    }

    return sb;
  }


  private static StringBuilder generateART(InterpolationDagNode root) {
    StringBuilder sb = new StringBuilder();

    Deque<InterpolationDagNode> queue = new ArrayDeque<InterpolationDagNode>();
    queue.addLast(root);

    sb.append("\t");
    sb.append(generateARTElement(root.getArtElement()));

    while(!queue.isEmpty()){
      InterpolationDagNode node = queue.poll();



      if (node.children.isEmpty()){
        sb.append("\t");
        sb.append(generateARTElement(node.getArtElement()));
        sb.append(" [fillcolor="+errorColor+"];\n");
      }

      for (InterpolationDagNode child : node.getChildren()){
        if (child.getTid() == root.getTid()){
          sb.append("\t");
          sb.append(generateARTElement(node.getArtElement()));
          sb.append(" -> ");
          sb.append(generateARTElement(child.getArtElement()));
          sb.append(";\n");
          queue.addLast(child);
        }
      }


    }

    return sb;
  }


  private static String generateARTElement(ARTElement artElement) {
    String str = "\""+artElement.getElementId()+"\"";
    return  str;
  }
}

/*
digraph G {

        subgraph cluster_T0 {
                 node [style=filled,fillcolor=lightgrey];
                 label = "T0";
                 "1" -> "2" -> "3";

        }

        subgraph cluster_T1 {
                 node [style=filled,fillcolor=white];
                 label = "T1";
                 "11" -> "22" -> "33";
        }


        "2" -> "33" [color=green];
        "3" -> "5";
}
 */