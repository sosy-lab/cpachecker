/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.slicing;

import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import org.sosy_lab.common.Concurrency;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.cwriter.CFAToCTranslator;

@Options(prefix = "programSlice")
public class SliceExporter {

  @Option(
      secure = true,
      name = "exportToC.enable",
      description = "Whether to export slices as C program files")
  private boolean exportToC = false;

  @Option(
      secure = true,
      name = "exportToC.file",
      description = "File template for exported C program slices")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate exportToCFile = PathTemplate.ofFormatString("programSlice.%d.c");

  public SliceExporter(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
  }

  private boolean containsRelevantEdge(Collection<CFANode> pNodes, Set<CFAEdge> pRelevantEdges) {

    for (CFANode node : pNodes) {
      for (int index = 0; index < node.getNumLeavingEdges(); index++) {
        if (pRelevantEdges.contains(node.getLeavingEdge(index))) {
          return true;
        }
      }
    }

    return false;
  }

  private void replaceIrrelevantEdges(Collection<CFANode> pNodes, Set<CFAEdge> pRelevantEdges) {

    for (CFANode node : pNodes) {
      for (int index = 0; index < node.getNumLeavingEdges(); index++) {

        CFAEdge edge = node.getLeavingEdge(index);

        if (edge.getEdgeType() == CFAEdgeType.StatementEdge && !pRelevantEdges.contains(edge)) {

          CFANode successor = edge.getSuccessor();

          CFAEdge newEdge =
              new BlankEdge(
                  edge.getRawStatement(),
                  edge.getFileLocation(),
                  node,
                  successor,
                  edge.getDescription());

          node.removeLeavingEdge(edge);
          node.addLeavingEdge(newEdge);

          successor.removeEnteringEdge(edge);
          successor.addEnteringEdge(newEdge);

          index = 0; // prevent skipping of edges
        }
      }
    }
  }

  private CFA createSliceCfa(CFA pCfa, Set<CFAEdge> pRelevantEdges) {

    NavigableMap<String, FunctionEntryNode> functions = new TreeMap<>();
    SortedSetMultimap<String, CFANode> nodes = TreeMultimap.create();
    FunctionEntryNode mainEntryNode = null;

    for (String functionName : pCfa.getAllFunctionNames()) {
      FunctionEntryNode entryNode = pCfa.getFunctionHead(functionName);

      if (mainEntryNode == null && functionName.equals(pCfa.getMainFunction().getFunctionName())) {
        mainEntryNode = entryNode;
      }

      Collection<CFANode> functionNodes = CFATraversal.dfs().collectNodesReachableFrom(entryNode);

      if (containsRelevantEdge(functionNodes, pRelevantEdges)) {
        replaceIrrelevantEdges(functionNodes, pRelevantEdges);
      } else {

        while (entryNode.getNumLeavingEdges() > 0) {
          entryNode.removeLeavingEdge(entryNode.getLeavingEdge(0));
        }

        entryNode.addLeavingEdge(
            new BlankEdge("", entryNode.getFileLocation(), entryNode, entryNode.getExitNode(), ""));
      }

      functions.put(functionName, entryNode);
      nodes.putAll(functionName, functionNodes);
    }

    return new MutableCFA(
        pCfa.getMachineModel(),
        functions,
        nodes,
        mainEntryNode,
        pCfa.getFileNames(),
        pCfa.getLanguage());
  }

  public void execute(
      CFA pCfa, Set<CFAEdge> pRelevantEdges, int pSliceCounter, LogManager pLogger) {

    if (exportToC) {

      Concurrency.newThread(
              "Slice-Exporter",
              () -> {
                Path path = exportToCFile.getPath(pSliceCounter);

                // TODO: clone CFA before slice creation
                CFA sliceCfa = createSliceCfa(pCfa, pRelevantEdges);

                try (Writer writer = IO.openOutputFile(path, Charset.defaultCharset())) {

                  String code = new CFAToCTranslator().translateCfa(sliceCfa);
                  writer.write(code);

                } catch (CPAException | IOException | InvalidConfigurationException e) {
                  pLogger.logUserException(Level.WARNING, e, "Could not write CFA to C file.");
                }
              })
          .start();
    }
  }
}
