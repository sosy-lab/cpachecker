/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.cpalien;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;

import com.google.common.base.Joiner;


public final class SMGPlotter {
  private SMGPlotter() {} /* utility class */

  public static void produceAsDotFile(CLangSMG smg, String name) throws IOException{
    File graphvizSMG = new File("smg-" + name + ".dot");
    Writer out = new OutputStreamWriter(new FileOutputStream(graphvizSMG), "UTF-8");
    try {
      out.write(smgAsDot(smg, name));
    } finally {
      out.close();
    }
  }

  private static String smgAsDot(CLangSMG smg, String name){
    StringBuilder sb = new StringBuilder();

    sb.append("digraph " + name.replace('-', '_') + "{\n");
    sb.append("  subgraph cluster_stack {\n");
    sb.append("    label=\"Stack\";\n");
    int i = 0;
    for (CLangStackFrame stack_item : smg.getStackObjects() ){
      sb.append("    subgraph cluster_stack_" + stack_item.getFunctionName() + "{\n");
      sb.append("      fontcolor=blue;\n");
      sb.append("      label=\"" + stack_item.getFunctionSignature() + "\";\n");
      sb.append("      struct" + i + "[shape=record,label=\"{ ");
      HashMap<String, SMGObject> stack_items = stack_item.getVariables();
      sb.append(Joiner.on(" | ").join(stack_items.keySet()));
      sb.append("}\"");
      sb.append("];\n");
      sb.append("    }\n");
      i++;
    }
    sb.append("  }\n");

    for (SMGObject heapObject : smg.getHeapObjects()){
      if (heapObject.notNull()){
        sb.append("  " + heapObject.getLabel() + " [ shape=rectangle, label = \"" + heapObject.toString() + "\"];\n");
      }
    }
    sb.append("}");

    return sb.toString();
  }
}
