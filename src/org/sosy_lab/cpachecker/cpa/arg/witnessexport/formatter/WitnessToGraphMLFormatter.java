// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.witnessexport.formatter;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.IOException;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.Edge;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.Witness;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.ElementType;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.GraphMlBuilder;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.NodeFlag;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.NodeType;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.w3c.dom.Element;

public class WitnessToGraphMLFormatter extends WitnessToOutputFormatter<Element> {

  private GraphMlBuilder doc;

  public WitnessToGraphMLFormatter(Witness pWitness) {
    super(pWitness);
  }

  @Override
  protected void initialize(Appendable pTarget) throws IOException {
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
  }

  @Override
  protected void finish(Appendable pTarget) throws IOException {
    doc.appendTo(pTarget);
  }

  @Override
  protected Element createNewNode(String pNodeId, Appendable pTarget) {
    final Element result = doc.createNodeElement(pNodeId, NodeType.ONPATH);
    if (witness.getWitnessOptions().exportNodeLabel()) {
      // add a printable label that for example is shown in yEd
      doc.addDataElementChild(result, KeyDef.LABEL, pNodeId);
    }
    for (NodeFlag f : witness.getNodeFlags().get(pNodeId)) {
      doc.addDataElementChild(result, f.key, "true");
    }
    for (Property violation : witness.getViolatedProperties().get(pNodeId)) {
      doc.addDataElementChild(result, KeyDef.VIOLATEDPROPERTY, violation.toString());
    }
    if (witness.hasQuasiInvariant(pNodeId)) {
      ExpressionTree<Object> tree = witness.getQuasiInvariant(pNodeId);
      doc.addDataElementChild(result, KeyDef.INVARIANT, tree.toString());
    }
    return result;
  }

  @Override
  protected void createNewEdge(
      Edge pEdge, Element pSourceNode, Element pTargetNode, Appendable pTarget) {
    final Element edge = doc.createEdgeElement(pEdge.getSource(), pEdge.getTarget());
    for (Map.Entry<KeyDef, String> entry : pEdge.getLabel().getMapping().entrySet()) {
      KeyDef keyDef = entry.getKey();
      String value = entry.getValue();
      if (keyDef.keyFor.equals(ElementType.EDGE)) {
        doc.addDataElementChild(edge, keyDef, value);
      } else if (keyDef.keyFor.equals(ElementType.NODE)) {
        doc.addDataElementChild(pTargetNode, keyDef, value);
      }
    }
  }

  @Override
  protected void addInvariantsData(
      Element pNode, ExpressionTree<Object> pTree, @Nullable String pScope, Appendable pTarget) {
    doc.addDataElementChild(pNode, KeyDef.INVARIANT, pTree.toString());
    if (!isNullOrEmpty(pScope) && !pTree.equals(ExpressionTrees.getFalse())) {
      doc.addDataElementChild(pNode, KeyDef.INVARIANTSCOPE, pScope);
    }
  }
}
