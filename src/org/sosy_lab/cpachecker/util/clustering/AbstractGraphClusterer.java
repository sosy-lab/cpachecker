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
package org.sosy_lab.cpachecker.util.clustering;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.sosy_lab.ccvisu.graph.GraphData;
import org.sosy_lab.ccvisu.graph.GraphEdge;
import org.sosy_lab.ccvisu.graph.GraphVertex;
import org.sosy_lab.ccvisu.writers.WriterDataClustersRSF;
import org.sosy_lab.ccvisu.writers.WriterDataLayoutLAY;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;


public abstract class AbstractGraphClusterer {
  protected void writeGraphLayout(org.sosy_lab.ccvisu.Options pOptions, GraphData pGraph, File pFile) {
    if (pFile == null) {
      return;
    }

    try {
      PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(pFile)));
      WriterDataLayoutLAY writerLay = new WriterDataLayoutLAY(out, pGraph, pOptions);
      writerLay.write();
      out.flush();
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  protected void writeGraphClustering(org.sosy_lab.ccvisu.Options pOptions, GraphData pGraph, File pFile) {
    if (pFile == null) {
      return;
    }

    try {
      PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(pFile)));
      WriterDataClustersRSF writerRsf = new WriterDataClustersRSF(out, pGraph);
      writerRsf.write();
      out.flush();
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  protected void writeGraph(org.sosy_lab.ccvisu.Options pOptions, GraphData pGraph, File pFile) {
    if (pFile == null) {
      return;
    }

    try {
      PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(pFile)));
      for (GraphEdge e: pGraph.getEdges()) {
        out.write(String.format("REL\t%s\t%s\n", e.getSource().getName(), e.getTarget().getName()));
      }
      out.flush();
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  protected Multimap<String, String> loadClusteringFromFile(File pFile) throws IOException {
    Multimap<String, String> result = HashMultimap.create();

    BufferedReader reader = new BufferedReader(new FileReader(pFile));

    String line = null;
    while((line = reader.readLine()) != null)
    {
      String[] cols = line.split("\\s+");
      String clusterId = cols[1];
      String elementId = cols[2];

      result.put(clusterId, elementId);
    }

    reader.close();

    return result;
  }
}
