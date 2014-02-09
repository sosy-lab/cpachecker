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
import java.io.Writer;
import java.util.Collections;
import java.util.EnumSet;
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
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.And;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.IntersectionMatchEdgeTokens;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.MatchEdgeTokens;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.Negation;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.Or;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.SubsetMatchEdgeTokens;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.GraphMlTag;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.NodeFlag;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.NodeType;
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

  @Option(name="spec.transitionToStopForNegatedTokensetMatch", description="")
  private boolean transitionToStopForNegatedTokensetMatch = true;

  @Option(name="spec.matchSourcecodeData", description="Match the source code provided with the witness.")
  private boolean matchSourcecodeData = false;

  @Option(name="spec.automatonDumpFile", description="")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path specAutomatonDumpFile = null;

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
          // Subset!
          if (tokens.containsAll(matcher.getMatchTokens())) {
            it.remove();
          }
        }
      }
    }
  }

  private Set<Integer> getTokensOfExpr(AutomatonBoolExpr expr, boolean excludeNegations) {
    if (expr instanceof MatchEdgeTokens) {
      return ((AutomatonBoolExpr.MatchEdgeTokens) expr).getMatchTokens();
    } else if (expr instanceof And) {
      Set<Integer> result = Sets.newTreeSet();
      And andExpr = (And) expr;
      result.addAll(getTokensOfExpr(andExpr.getA(), excludeNegations));
      result.addAll(getTokensOfExpr(andExpr.getB(), excludeNegations));
      return result;
    } else if (expr instanceof Or) {
      Set<Integer> result = Sets.newTreeSet();
      Or orExpr = (Or) expr;
      result.addAll(getTokensOfExpr(orExpr.getA(), excludeNegations));
      result.addAll(getTokensOfExpr(orExpr.getB(), excludeNegations));
      return result;
    } else if (expr instanceof Negation) {
      if (!excludeNegations) {
        Negation negExpr = (Negation) expr;
        return getTokensOfExpr(negExpr.getA(), excludeNegations);
      }
   }

   return Collections.emptySet();
  }

  private boolean tokenSetsDisjoint(List<AutomatonTransition> transitions) {
    Set<Integer> allTokens = Sets.newTreeSet();
    for (AutomatonTransition t : transitions) {
      Set<Integer> exprTokens = getTokensOfExpr(t.getTrigger(), true);
      if (exprTokens.isEmpty()) {
        continue;
      }

      int differentTokensWithout = allTokens.size();
      allTokens.addAll(exprTokens);
      int differentTokensWith = allTokens.size();

      if (differentTokensWith - differentTokensWithout != exprTokens.size()) {
        return false;
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
      String initialStateName = null;

      // Create transitions ----
      AutomatonBoolExpr epsilonTrigger = new SubsetMatchEdgeTokens(Collections.<Integer>emptySet());
      NodeList edges = doc.getElementsByTagName(GraphMlTag.EDGE.toString());
      Map<String, List<AutomatonTransition>> stateTransitions = Maps.newHashMap();
      for (int i=0; i<edges.getLength(); i++) {
        Node stateTransitionEdge = edges.item(i);

        String sourceStateId = docDat.getAttributeValue(stateTransitionEdge, "source", "Every transition needs a source!");
        String targetStateId = docDat.getAttributeValue(stateTransitionEdge, "target", "Every transition needs a target!");
        Element targetStateNode = docDat.getNodeWithId(targetStateId);
        EnumSet<NodeFlag> targetNodeFlags = docDat.getNodeFlags(targetStateNode);

        List<AutomatonBoolExpr> emptyAssertions = Collections.emptyList();
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

        AutomatonBoolExpr conjunctedTriggers = AutomatonBoolExpr.TRUE;

        if (matchSourcecodeData) {
          Set<String> sourceCodeDataTags = docDat.getDataOnNode(stateTransitionEdge, KeyDef.SOURCECODE);
          Preconditions.checkArgument(sourceCodeDataTags.size() < 2, "At most one source code data-tag must be provided!");
          if (sourceCodeDataTags.isEmpty()) {
            conjunctedTriggers = new AutomatonBoolExpr.And(conjunctedTriggers, new AutomatonBoolExpr.MatchCFAEdgeExact(""));
          } else {
            final String sourceCode = sourceCodeDataTags.iterator().next();
            conjunctedTriggers = new AutomatonBoolExpr.And(conjunctedTriggers, new AutomatonBoolExpr.MatchCFAEdgeExact(sourceCode));
          }

          if (targetStateId.equalsIgnoreCase(SINK_NODE_ID) || targetNodeFlags.contains(NodeFlag.ISSINKNODE)) {
            // Transition to the BOTTOM state
            transitions.add(new AutomatonTransition(conjunctedTriggers, emptyAssertions, actions, AutomatonInternalState.BOTTOM));
          } else {
            // Transition to the next state
            transitions.add(new AutomatonTransition(conjunctedTriggers, emptyAssertions, actions, targetStateId));
            transitions.add(new AutomatonTransition(new AutomatonBoolExpr.Negation(conjunctedTriggers), emptyAssertions, actions, AutomatonInternalState.BOTTOM));
          }
        } else {
          if (considerNegativeSemanticsAttribute) {
            Optional<Boolean> matchPositiveCase = Optional.absent();
            switch(docDat.getDataValueWithDefault(stateTransitionEdge, KeyDef.TOKENSNEGATED, "").toLowerCase()) {
              case "true":
                matchPositiveCase = Optional.of(false);
                break;
              case "false":
                matchPositiveCase = Optional.of(true);
                break;
            }
            conjunctedTriggers = new AutomatonBoolExpr.And(conjunctedTriggers, new AutomatonBoolExpr.MatchAssumeCase(matchPositiveCase));
          }

          Set<Integer> matchTokens = getTokensOfEdge(docDat, stateTransitionEdge);

          AutomatonBoolExpr tokensSubsetTrigger = new SubsetMatchEdgeTokens(matchTokens);
          conjunctedTriggers = new AutomatonBoolExpr.And(conjunctedTriggers, tokensSubsetTrigger);

          removeTransitions(auxilaryTransitions, transitions, matchTokens);

          if (targetStateId.equalsIgnoreCase(SINK_NODE_ID)) {
            // Transition to the BOTTOM state
            transitions.add(new AutomatonTransition(conjunctedTriggers, emptyAssertions, actions, AutomatonInternalState.BOTTOM));
          } else {
            // Transition to the next state
            transitions.add(new AutomatonTransition(conjunctedTriggers, emptyAssertions, actions, targetStateId));

            // -- Add some auxiliary transitions --
            // Repeated set of tokens
            if (!matchTokens.isEmpty()) {
              AutomatonTransition tr = new AutomatonTransition(tokensSubsetTrigger, emptyAssertions, actions, targetStateId);
              auxilaryTransitions.add(tr);
              targetStateTransitions.add(tr);
            }

            // Empty set of tokens
            if (!matchTokens.isEmpty()) {
              AutomatonTransition tr = new AutomatonTransition(epsilonTrigger, emptyAssertions, actions, targetStateId);
              auxilaryTransitions.add(tr);
              targetStateTransitions.add(tr);
            }

            // Negated sets of tokens to STOP
            if (transitionToStopForNegatedTokensetMatch && !matchTokens.isEmpty()) {
              AutomatonBoolExpr trigger = new AutomatonBoolExpr.And(
                      new AutomatonBoolExpr.MatchNonEmptyEdgeTokens(),
                      new AutomatonBoolExpr.And(
                          new AutomatonBoolExpr.Negation(new AutomatonBoolExpr.MatchAssumeEdge()),
                          new AutomatonBoolExpr.Negation(
                              new IntersectionMatchEdgeTokens(matchTokens))));
              AutomatonTransition tr = new AutomatonTransition(trigger, emptyAssertions, actions, AutomatonInternalState.BOTTOM);
              auxilaryTransitions.add(tr);
              transitions.add(tr);
            }
          }
        }
      }

      // Create states ----
      List<AutomatonInternalState> automatonStates = Lists.newArrayList();
      for (String stateId : docDat.getIdToNodeMap().keySet()) {
        Element stateNode = docDat.getIdToNodeMap().get(stateId);
        EnumSet<NodeFlag> nodeFlags = docDat.getNodeFlags(stateNode);

        List<AutomatonTransition> transitions = stateTransitions.get(stateId);
        if (transitions == null) {
          transitions = Collections.emptyList();
        }

        if (nodeFlags.contains(NodeFlag.ISENTRY)) {
          Preconditions.checkArgument(initialStateName == null, "Only one entrynode is supported!");
          initialStateName = stateId;
        }

        // Determine if "matchAll" should be enabled
        boolean matchAll = !tokenSetsDisjoint(transitions);

        // ...
        AutomatonInternalState state = new AutomatonInternalState(stateId, transitions, false, matchAll);
        automatonStates.add(state);
      }

      // Build and return the result ----
      Preconditions.checkNotNull(initialStateName, "Every graph needs a specified entry node!");
      Map<String, AutomatonVariable> automatonVariables = Collections.emptyMap();
      List<Automaton> result = Lists.newArrayList();
      Automaton automaton = new Automaton(automatonName, automatonVariables, automatonStates, initialStateName);
      result.add(automaton);

      if (specAutomatonDumpFile != null) {
        try (Writer w = Files.openOutputFile(specAutomatonDumpFile)) {
          automaton.writeDotFile(w);
        } catch (IOException e) {
         // logger.logUserException(Level.WARNING, e, "Could not write the automaton to DOT file");
        }
      }

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

    private Map<String, Element> idToNodeMap = null;

    public GraphMlDocumentData(Document doc) {
      this.doc = doc;
    }

    public EnumSet<NodeFlag> getNodeFlags(Element pStateNode) {
      EnumSet<NodeFlag> result = EnumSet.noneOf(NodeFlag.class);

      NodeList dataChilds = pStateNode.getElementsByTagName(GraphMlTag.DATA.toString());

      for (int i=0; i<dataChilds.getLength(); i++) {
        Node dataChild = dataChilds.item(i);
        Node attribute = dataChild.getAttributes().getNamedItem("key");
        Preconditions.checkNotNull(attribute, "Every data element must have a key attribute!");
        String key = attribute.getTextContent();
        NodeFlag flag = NodeFlag.getNodeFlagByKey(key);
        if (flag != null) {
          result.add(flag);
        }
      }

      return result;
    }

    public Map<String, Element> getIdToNodeMap() {
      if (idToNodeMap != null) {
        return idToNodeMap;
      }

      idToNodeMap = Maps.newHashMap();

      NodeList nodes = doc.getElementsByTagName(GraphMlTag.NODE.toString());
      for (int i=0; i<nodes.getLength(); i++) {
        Element stateNode = (Element) nodes.item(i);
        String stateId = getNodeId(stateNode);

        idToNodeMap.put(stateId, stateNode);
      }

      return idToNodeMap;
    }

    public String getAttributeValue(Node of, String attributeName, String exceptionMessage) {
      Node attribute = of.getAttributes().getNamedItem(attributeName);
      Preconditions.checkNotNull(attribute, exceptionMessage);
      return attribute.getTextContent();
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

    public String getNodeId(Node stateNode) {
      return getAttributeValue(stateNode, "id", "Every state needs an ID!");
    }

    public Element getNodeWithId(String nodeId) {
      Element result = getIdToNodeMap().get(nodeId);
      Preconditions.checkNotNull(result, "Node not found. Id: " + nodeId);
      return result;
    }

    public NodeType getNodeType(Node checkDataOnNode) {
      return NodeType.fromString(getDataValue(checkDataOnNode, KeyDef.NODETYPE, "The type of each node must be specified!"));
    }

    private String getDataValue(Node dataOnNode, KeyDef dataKey, String exceptionMessage) {
      Set<String> values = getDataOnNode(dataOnNode, dataKey);
      if (values.isEmpty()) {
        Optional<String> defaultValue = getDataDefault(dataKey);
        if (defaultValue.isPresent()) {
          values.add(defaultValue.get());
        }
      }
      Preconditions.checkArgument(values.size() <= 1, getNodeId(dataOnNode) + ": " + exceptionMessage + " More than one specified!");
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
