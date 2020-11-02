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

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.ElementType;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.GraphMlBuilder;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.NodeFlag;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.NodeType;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.w3c.dom.Element;

public class WitnessToGraphMlUtils {

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
    WitnessToGraphMlUtils.writeElementsOfGraphToDoc(doc, witness);
    doc.appendTo(pTarget);
  }

  private static void writeElementsOfGraphToDoc(GraphMlBuilder doc, Witness witness) {
    String entryStateNodeId = witness.getEntryStateNodeId();
    Map<String, Element> nodes = new HashMap<>();
    Deque<String> waitlist = new ArrayDeque<>();
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
