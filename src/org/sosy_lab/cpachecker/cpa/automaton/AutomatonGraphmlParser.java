/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.automaton;

import static org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.SINK_NODE_ID;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.MatchEdgeTokens;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.GraphMlTag;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@Options
public class AutomatonGraphmlParser {

  @Option(name="spec.considerNegativeSemanticsAttribute",
      description="Consider the negative semantics of tokens provided with path automatons.")
  private boolean considerNegativeSemanticsAttribute = true;

  public AutomatonGraphmlParser(Configuration pConfig, LogManager pLogger, MachineModel pMachine) throws InvalidConfigurationException {
    pConfig.inject(this);
  }

  private void removeTransitions(Set<AutomatonTransition> whitelist, List<AutomatonTransition> from, Set<Integer> tokens) {
    Iterator<AutomatonTransition> it = from.iterator();
    while (it.hasNext()) {
      AutomatonTransition t = it.next();
      if (whitelist.contains(t)) {
        if (t.getTrigger() instanceof AutomatonBoolExpr.MatchEdgeTokens) {
          AutomatonBoolExpr.MatchEdgeTokens matcher = (AutomatonBoolExpr.MatchEdgeTokens) t.getTrigger();
          if (matcher.getMatchTokens().equals(tokens)) {
            it.remove();
          }
        }
      }
    }
  }

  private boolean tokenSetsDisjoint(List<AutomatonTransition> transitions) {
    Set<Integer> allTokens = Sets.newTreeSet();
    for (AutomatonTransition t : transitions) {
      if (t.getTrigger() instanceof AutomatonBoolExpr.MatchEdgeTokens) {
        AutomatonBoolExpr.MatchEdgeTokens matcher = (AutomatonBoolExpr.MatchEdgeTokens) t.getTrigger();
        int differentTokensWithout = allTokens.size();
        allTokens.addAll(matcher.getMatchTokens());
        int differentTokensWith = allTokens.size();
        if (differentTokensWith - differentTokensWithout != matcher.getMatchTokens().size()) {
          return false;
        }
      }
    }
    return true;
  }

  /**
  * Parses a Specification File and returns the Automata found in the file.
  */
  public List<Automaton> parseAutomatonFile(Path pInputFile) throws InvalidConfigurationException {
    Set<AutomatonTransition> auxilaryTransitions = Sets.newHashSet();
    try (InputStream input = pInputFile.asByteSource().openStream()) {
      // Parse the XML document ----
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      Document doc = docBuilder.parse(input);
      doc.getDocumentElement().normalize();

      GraphMlDocumentData docDat = new GraphMlDocumentData(doc);

      // (The one) root node of the graph ----
      NodeList graphs = doc.getElementsByTagName(GraphMlTag.GRAPH.toString());
      Preconditions.checkArgument(graphs.getLength() == 1, "The graph file must describe exactly one automaton.");
      Node graphNode = graphs.item(0);

      // Extract the information on the automaton ----
      Node nameAttribute = graphNode.getAttributes().getNamedItem("name");
      String automatonName = nameAttribute == null ? "" : nameAttribute.getTextContent();
      String initialStateName = docDat.getDataValue(graphNode, KeyDef.ENTRYNODE, "Every graph needs a specified entry node!");

      // Create transitions ----
      AutomatonBoolExpr epsilonTrigger = new MatchEdgeTokens(Collections.<Integer>emptySet(), Optional.<Boolean>absent());
      NodeList edges = doc.getElementsByTagName(GraphMlTag.EDGE.toString());
      Map<String, List<AutomatonTransition>> stateTransitions = Maps.newHashMap();
      for (int i=0; i<edges.getLength(); i++) {
        Node stateTransitionEdge = edges.item(i);

        String sourceStateId = getAttributeValue(stateTransitionEdge, "source", "Every transition needs a source!");
        String targetStateId = getAttributeValue(stateTransitionEdge, "target", "Every transition needs a target!");

        Optional<Boolean> matchNegativeSemantics = Optional.absent();
        if (considerNegativeSemanticsAttribute) {
          switch(docDat.getDataValueWithDefault(stateTransitionEdge, KeyDef.TOKENSNEGATED, "").toLowerCase()) {
            case "true": matchNegativeSemantics = Optional.of(true); break;
            case "false": matchNegativeSemantics = Optional.of(false); break;
          }
        }

        Set<Integer> matchTokens = getTokensOfEdge(docDat, stateTransitionEdge);
        AutomatonBoolExpr trigger = new MatchEdgeTokens(matchTokens, matchNegativeSemantics);
        List<AutomatonBoolExpr> assertions = Collections.emptyList();
        List<AutomatonAction> actions = Collections.emptyList();

        List<AutomatonTransition> transitions = stateTransitions.get(sourceStateId);
        if (transitions == null) {
          transitions = Lists.newArrayList();
          stateTransitions.put(sourceStateId, transitions);
        }

        List<AutomatonTransition> targetStateTransitions = stateTransitions.get(targetStateId);
        if (targetStateTransitions == null) {
          targetStateTransitions = Lists.newArrayList();
          stateTransitions.put(targetStateId, targetStateTransitions);
        }

        removeTransitions(auxilaryTransitions, transitions, matchTokens);
        if (targetStateId.equalsIgnoreCase(SINK_NODE_ID)) {
          transitions.add(new AutomatonTransition(trigger, assertions, actions, AutomatonInternalState.BOTTOM));
          if (!matchTokens.isEmpty()) {
            AutomatonTransition tr = new AutomatonTransition(new AutomatonBoolExpr.Negation(trigger), assertions, actions, AutomatonInternalState.BOTTOM);
            auxilaryTransitions.add(tr);
            transitions.add(tr);
          }
        } else {
          // Set of tokens
          transitions.add(new AutomatonTransition(trigger, assertions, actions, targetStateId));

          // Repeated set of tokens
          if (!matchTokens.isEmpty()) {
            AutomatonTransition tr = new AutomatonTransition(trigger, assertions, actions, targetStateId);
            auxilaryTransitions.add(tr);
            targetStateTransitions.add(tr);
          }

          // Empty set of tokens
          if (!matchTokens.isEmpty()) {
            AutomatonTransition tr = new AutomatonTransition(epsilonTrigger, assertions, actions, targetStateId);
            auxilaryTransitions.add(tr);
            targetStateTransitions.add(tr);
          }
          // Negated sets of tokens
//          if (!matchTokens.isEmpty()) {
//            AutomatonTransition tr = new AutomatonTransition(new AutomatonBoolExpr.Negation(trigger), assertions, actions, AutomatonInternalState.BOTTOM);
//            auxilaryTransitions.add(tr);
//            transitions.add(tr);
//          }
        }

      }

      // Create states ----
      NodeList nodes = doc.getElementsByTagName(GraphMlTag.NODE.toString());
      List<AutomatonInternalState> automatonStates = Lists.newArrayList();
      for (int i=0; i<nodes.getLength(); i++) {
        Node stateNode = nodes.item(i);

        String stateId = getAttributeValue(stateNode, "id", "Every state needs an ID!");

        List<AutomatonTransition> transitions = stateTransitions.get(stateId);
        if (transitions == null) {
          transitions = Collections.emptyList();
        }

        // Determine if "matchAll" should be enabled
        boolean matchAll = !tokenSetsDisjoint(transitions);

        // ...
        AutomatonInternalState state = new AutomatonInternalState(stateId, transitions, false, matchAll);
        automatonStates.add(state);
      }

      // Build and return the result ----
      Map<String, AutomatonVariable> automatonVariables = Collections.emptyMap();
      List<Automaton> result = Lists.newArrayList();
      Automaton automaton = new Automaton(automatonName, automatonVariables, automatonStates, initialStateName);
      result.add(automaton);

//      try (Writer w = Files.openOutputFile(Paths.get("autom_test.dot"))) {
//        automaton.writeDotFile(w);
//      } catch (IOException e) {
//        //logger.logUserException(Level.WARNING, e, "Could not write the automaton to DOT file");
//      }

      return result;

    } catch (FileNotFoundException e) {
      throw new InvalidConfigurationException("Invalid automaton file provided! File not found!: " + pInputFile.getPath());
    } catch (IOException | ParserConfigurationException | SAXException e) {
      throw new InvalidConfigurationException("Error while accessing automaton file!", e);
    } catch (InvalidAutomatonException e) {
      throw new InvalidConfigurationException("The automaton provided is invalid!", e);
    }
  }

  Set<Integer> parseTokens(final String tokenString) {
    Set<Integer> result = Sets.newTreeSet();
    String[] ranges = tokenString.trim().split(",");
    for (String range : ranges) {
      if (range.trim().isEmpty()) {
        continue;
      }
      String[] rangeDef = range.trim().split("-");
      int rangeStart = Integer.parseInt(rangeDef[0]);
      int rangeEnd = rangeStart;
      if (rangeDef.length > 1) {
        rangeEnd = Integer.parseInt(rangeDef[1]);
      }
      for (int tokenPos=rangeStart; tokenPos<=rangeEnd; tokenPos++) {
        result.add(tokenPos);
      }
    }
    return result;
  }

  String getAttributeValueWithDefault(Node of, String attributeName, String defaultValue) {
    Node attribute = of.getAttributes().getNamedItem(attributeName);
    if (attribute == null) {
      return defaultValue;
    } else {
      return attribute.getTextContent();
    }
  }

  String getAttributeValue(Node of, String attributeName, String exceptionMessage) {
    Node attribute = of.getAttributes().getNamedItem(attributeName);
    Preconditions.checkNotNull(attribute, exceptionMessage);
    return attribute.getTextContent();
  }

  private Set<Integer> getTokensOfEdge(GraphMlDocumentData docDat, Node edgeDef) {
    Set<String> data = docDat.getDataOnNode(edgeDef, KeyDef.TOKENS);
    Preconditions.checkArgument(data.size() <= 1, "Each edge must include at most one data-node with the key 'tokens'!");
    if (data.size() == 0) {
      return Collections.emptySet();
    } else {
      String tokenString = data.iterator().next();
      return parseTokens(tokenString);
    }
  }

  private static class GraphMlDocumentData {

    private final HashMap<String, Optional<String>> defaultDataValues = Maps.newHashMap();
    private final Document doc;

    public GraphMlDocumentData(Document doc) {
      this.doc = doc;
    }

    private Optional<String> getDataDefault(KeyDef dataKey) {
      Optional<String> result = defaultDataValues.get(dataKey.id);
      if (result != null) {
        return result;
      }

      NodeList keyDefs = doc.getElementsByTagName(GraphMlTag.KEY.toString());
      for (int i=0; i<keyDefs.getLength(); i++) {
        Element keyDef = (Element) keyDefs.item(i);
        Node id = keyDef.getAttributes().getNamedItem("id");
        if (dataKey.id.equals(id.getTextContent())) {
          NodeList defaultTags = keyDef.getElementsByTagName(GraphMlTag.DEFAULT.toString());
          result = Optional.absent();
          if (defaultTags.getLength() > 0) {
            Preconditions.checkArgument(defaultTags.getLength() == 1);
            result = Optional.of(defaultTags.item(0).getTextContent());
          }
          defaultDataValues.put(dataKey.id, result);
          return result;
        }
      }
      return Optional.absent();
    }

    private String getDataValue(Node dataOnNode, KeyDef dataKey, String exceptionMessage) {
      Set<String> values = getDataOnNode(dataOnNode, dataKey);
      if (values.isEmpty()) {
        Optional<String> defaultValue = getDataDefault(dataKey);
        if (defaultValue.isPresent()) {
          values.add(defaultValue.get());
        }
      }
      Preconditions.checkArgument(values.size() == 1, exceptionMessage);
      return values.iterator().next();
    }

    public String getDataValueWithDefault(Node dataOnNode, KeyDef dataKey, final String defaultValue) {
      Set<String> values = getDataOnNode(dataOnNode, dataKey);
      if (values.size() == 0) {
        Optional<String> dataDefault = getDataDefault(dataKey);
        if (dataDefault.isPresent()) {
          return dataDefault.get();
        } else {
          return defaultValue;
        }
      } else {
        return values.iterator().next();
      }
    }


    private Set<String> getDataOnNode(Node node, final KeyDef dataKey) {
      Preconditions.checkNotNull(node);
      Preconditions.checkArgument(node.getNodeType() == Node.ELEMENT_NODE);

      Element nodeElement = (Element) node;
      Set<Node> dataNodes = findKeyedDataNode(nodeElement, dataKey);

      Set<String> result = Sets.newHashSet();
      for (Node n: dataNodes) {
        result.add(n.getTextContent());
      }

      return result;
    }

    private Set<Node> findKeyedDataNode(Element of, final KeyDef dataKey) {
      Set<Node> result = Sets.newHashSet();
      NodeList dataChilds = of.getElementsByTagName(GraphMlTag.DATA.toString());
      for (int i=0; i<dataChilds.getLength(); i++) {
        Node dataChild = dataChilds.item(i);
        Node attribute = dataChild.getAttributes().getNamedItem("key");
        Preconditions.checkNotNull(attribute, "Every data element must have a key attribute!");
        if (attribute.getTextContent().equals(dataKey.id)) {
          result.add(dataChild);
        }
      }
      return result;
    }

  }


}
