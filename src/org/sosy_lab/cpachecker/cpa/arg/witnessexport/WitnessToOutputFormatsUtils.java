// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.witnessexport;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.logging.Level.WARNING;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.Appender;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.counterexample.ReportGenerator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.formatter.WitnessToDotFormatter;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.formatter.WitnessToGraphMLFormatter;
import org.sosy_lab.cpachecker.cpa.slab.SLARGToDotWriter;
import org.sosy_lab.cpachecker.util.NumericIdProvider;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;

public class WitnessToOutputFormatsUtils {

  /** utility method */
  public static void writeWitness(
      Path filename, boolean compressFile, Appender content, LogManager logger) {
    try {
      if (compressFile) {
        Path file = filename.resolveSibling(filename.getFileName() + ".gz");
        IO.writeGZIPFile(file, Charset.defaultCharset(), content);
      } else {
        IO.writeFile(filename, Charset.defaultCharset(), content);
      }
    } catch (IOException e) {
      logger.logfException(WARNING, e, "Violation witness export to %s failed.", filename);
    }
  }

  /**
   * Appends the witness as GraphML to the supplied {@link Appendable}
   *
   * @param witness contains the information necessary to generate the GraphML representation
   * @param pTarget where to append the GraphML
   */
  public static void writeToGraphMl(Witness witness, Appendable pTarget) throws IOException {
    new WitnessToGraphMLFormatter(witness).appendTo(pTarget);
  }

  /** Appends the witness as Dot/Graphviz to the supplied {@link Appendable}. */
  public static void writeToDot(Witness witness, Appendable pTarget) throws IOException {
    new WitnessToDotFormatter(witness).appendTo(pTarget);
  }

  /**
   * This method can be used to extract information about the nodes and edges in the witness which
   * then can be used e.g. for display in the HTML report (c.f. @link ReportGenerator).
   *
   * @param witness the information that will be extracted
   * @param nodesMap map that will be filled with information about the nodes
   * @param edgesMap map that will be filled with information about the edges
   */
  public static void witnessToMapsForHTMLReport(
      Witness witness, Map<String, Map<String, Object>> nodesMap, Map<String, Object> edgesMap) {
    NumericIdProvider idProvider = NumericIdProvider.create();
    String entryStateNode = witness.getEntryStateNodeId();
    Set<String> nodes = new HashSet<>();
    Deque<String> waitlist = new ArrayDeque<>();
    waitlist.push(entryStateNode);
    // Element entryNode = createNewNode(doc, entryStateNodeId, witness);
    // addInvariantsData(doc, entryNode, entryStateNodeId, witness);
    nodes.add(entryStateNode);

    while (!waitlist.isEmpty()) {
      String source = waitlist.pop();

      Map<String, Object> sourceNode = nodesMap.get(source);
      if (sourceNode == null) {
        // targetNode = createNewNode(doc, edge.getTarget(), witness);
        sourceNode = new HashMap<>();

        List<Integer> nodeIds =
            witness.getARGStatesFor(source).stream()
                .map(ARGState::getStateId)
                .collect(ImmutableList.toImmutableList());
        String nodeString = SLARGToDotWriter.generateLocationString(nodeIds).toString();
        StringBuilder labelBuilder = new StringBuilder(source);
        if (!nodeString.isEmpty()) {
          labelBuilder.append(String.format("%nARG node%s: ", nodeIds.size() == 1 ? "" : "s"));
          labelBuilder.append(nodeString);
        }

        ExpressionTree<Object> tree = witness.getStateInvariant(source);
        if (!tree.equals(ExpressionTrees.getTrue())) {
          sourceNode.put(KeyDef.INVARIANT.toString(), tree.toString());
          String scope = witness.getStateScopes().get(source);
          labelBuilder.append(System.lineSeparator()).append(tree.toString());
          if (!isNullOrEmpty(scope) && !tree.equals(ExpressionTrees.getFalse())) {
            sourceNode.put(KeyDef.INVARIANTSCOPE.toString(), scope);
            labelBuilder.append(System.lineSeparator()).append(scope);
          }
        }

        sourceNode.put("index", idProvider.provideNumericId(source));
        sourceNode.put("label", labelBuilder.toString());
        sourceNode.put("type", determineNodeType(witness, source));
        sourceNode.put("func", "main"); // TODO: add actual function here (but what if it's mixed?!)
        nodesMap.put(source, sourceNode);
      }

      for (Edge edge : witness.getLeavingEdges().get(source)) {
        ExpressionTree<Object> tree = witness.getStateInvariant(edge.getTarget());

        List<CFAEdge> edges = witness.getCFAEdgeFor(edge);
        Map<String, Object> edgeMap =
            ReportGenerator.createArgEdge(
                idProvider.provideNumericId(source),
                idProvider.provideNumericId(edge.getTarget()),
                edges);
        for (java.util.Map.Entry<KeyDef, String> e : edge.getLabel().getMapping().entrySet()) {
          edgeMap.put(e.getKey().toString(), e.getValue());
        }
        edgesMap.put(edge.getSource() + "->" + edge.getTarget(), edgeMap);

        if (nodes.add(edge.getTarget())) {
          if (!ExpressionTrees.getFalse().equals(tree)) {
            waitlist.push(edge.getTarget());
          }
        }
      }
    }
  }

  private static String determineNodeType(Witness witness, String source) {
    Collection<ARGState> states = witness.getARGStatesFor(source);
    if (!witness.getViolatedProperties().get(source).isEmpty()
        || states.stream().anyMatch(ARGState::isTarget)) {
      return "target";
    } else if (!states.stream().allMatch(ARGState::wasExpanded)) {
      return "not-expanded";
    } else if (states.stream().anyMatch(ARGState::shouldBeHighlighted)) {
      return "highlighted";
    }
    return "";
  }
}
