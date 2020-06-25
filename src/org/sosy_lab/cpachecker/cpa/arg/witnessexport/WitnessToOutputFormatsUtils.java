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
 */
package org.sosy_lab.cpachecker.cpa.arg.witnessexport;

import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.counterexample.ReportGenerator;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.slab.SLARGToDotWriter;
import org.sosy_lab.cpachecker.util.NumericIdProvider;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.ElementType;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.GraphMlBuilder;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.NodeFlag;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.NodeType;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.w3c.dom.Element;

public class WitnessToOutputFormatsUtils {

  /**
   * Appends the witness as GraphML to the supplied {@link Appendable}
   *
   * @param witness contains the information necessary to generate the GraphML representation
   * @param pTarget where to append the GraphML
   */
  public static void writeToGraphMl(Witness witness, Appendable pTarget) throws IOException {
    // Write elements
    final GraphMlBuilder doc;
    try {
      doc =
          new GraphMlBuilder(
              witness.getWitnessType(),
              witness.getOriginFile(),
              witness.getCfa(),
              witness.getMetaData());
    } catch (ParserConfigurationException e) {
      throw new IOException(e);
    }
    WitnessToOutputFormatsUtils.writeElementsOfGraphToDoc(doc, witness);
    doc.appendTo(pTarget);
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
      Witness witness, Map<String, Object> nodesMap, Map<String, Object> edgesMap) {
    NumericIdProvider idProvider = NumericIdProvider.create();
    String entryStateNode = witness.getEntryStateNodeId();
    Set<String> nodes = Sets.newHashSet();
    Deque<String> waitlist = Queues.newArrayDeque();
    waitlist.push(entryStateNode);
    // Element entryNode = createNewNode(doc, entryStateNodeId, witness);
    // addInvariantsData(doc, entryNode, entryStateNodeId, witness);
    nodes.add(entryStateNode);

    while (!waitlist.isEmpty()) {
      String source = waitlist.pop();

      @SuppressWarnings("unchecked")
      Map<String, Object> sourceNode = (Map<String, Object>) nodesMap.get(source);
      if (sourceNode == null) {
        // targetNode = createNewNode(doc, edge.getTarget(), witness);
        sourceNode = Maps.newHashMap();

        List<Integer> nodeIds = witness
            .getARGStatesFor(source)
            .stream()
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
        edgesMap.put(
            edge.getSource() + "->" + edge.getTarget(),
            edgeMap);

        if (!nodes.contains(edge.getTarget())) {
          nodes.add(edge.getTarget());
          if (!ExpressionTrees.getFalse().equals(tree)) {
          waitlist.push(edge.getTarget());
          }
        } else {
          continue;
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
    } else if (states.stream().anyMatch(ARGState::shouldBeHighlighted)){
      return "highlighted";
    }
    return "";
  }

  private static void writeElementsOfGraphToDoc(GraphMlBuilder doc, Witness witness) {
    String entryStateNodeId = witness.getEntryStateNodeId();
    Map<String, Element> nodes = Maps.newHashMap();
    Deque<String> waitlist = Queues.newArrayDeque();
    waitlist.push(entryStateNodeId);
    Element entryNode = createNewNode(doc, entryStateNodeId, witness);
    addInvariantsData(doc, entryNode, entryStateNodeId, witness);
    nodes.put(entryStateNodeId, entryNode);
    while (!waitlist.isEmpty()) {
      String source = waitlist.pop();
      for (Edge edge : witness.getLeavingEdges().get(source)) {
        Element targetNode = nodes.get(edge.getTarget());
        if (targetNode == null) {
          targetNode = createNewNode(doc, edge.getTarget(), witness);
          if (!ExpressionTrees.getFalse()
              .equals(addInvariantsData(doc, targetNode, edge.getTarget(), witness))) {
            waitlist.push(edge.getTarget());
          }
          nodes.put(edge.getTarget(), targetNode);
        }
        createNewEdge(doc, edge, targetNode);
      }
    }
  }

  private static Element createNewNode(
      GraphMlBuilder pDoc, String pEntryStateNodeId, Witness witness) {
    Element result = pDoc.createNodeElement(pEntryStateNodeId, NodeType.ONPATH);

    if (witness.getWitnessOptions().exportNodeLabel()) {
      // add a printable label that for example is shown in yEd
      pDoc.addDataElementChild(result, KeyDef.LABEL, pEntryStateNodeId);
    }

    for (NodeFlag f : witness.getNodeFlags().get(pEntryStateNodeId)) {
      pDoc.addDataElementChild(result, f.key, "true");
    }
    for (Property violation : witness.getViolatedProperties().get(pEntryStateNodeId)) {
      pDoc.addDataElementChild(result, KeyDef.VIOLATEDPROPERTY, violation.toString());
    }

    if (witness.hasQuasiInvariant(pEntryStateNodeId)) {
      ExpressionTree<Object> tree = witness.getQuasiInvariant(pEntryStateNodeId);
      pDoc.addDataElementChild(result, KeyDef.INVARIANT, tree.toString());
    }

    return result;
  }

  private static ExpressionTree<Object> addInvariantsData(
      GraphMlBuilder pDoc, Element pNode, String pStateId, Witness witness) {
    if (!witness.getInvariantExportStates().contains(pStateId)) {
      return ExpressionTrees.getTrue();
    }
    ExpressionTree<Object> tree = witness.getStateInvariant(pStateId);
    if (!tree.equals(ExpressionTrees.getTrue())) {
      pDoc.addDataElementChild(pNode, KeyDef.INVARIANT, tree.toString());
      String scope = witness.getStateScopes().get(pStateId);
      if (!isNullOrEmpty(scope) && !tree.equals(ExpressionTrees.getFalse())) {
        pDoc.addDataElementChild(pNode, KeyDef.INVARIANTSCOPE, scope);
      }
    }
    return tree;
  }

  private static Element createNewEdge(GraphMlBuilder pDoc, Edge pEdge, Element pTargetNode) {
    Element edge = pDoc.createEdgeElement(pEdge.getSource(), pEdge.getTarget());
    for (Map.Entry<KeyDef, String> entry : pEdge.getLabel().getMapping().entrySet()) {
      KeyDef keyDef = entry.getKey();
      String value = entry.getValue();
      if (keyDef.keyFor.equals(ElementType.EDGE)) {
        pDoc.addDataElementChild(edge, keyDef, value);
      } else if (keyDef.keyFor.equals(ElementType.NODE)) {
        pDoc.addDataElementChild(pTargetNode, keyDef, value);
      }
    }
    return edge;
  }
}
