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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.MatchEdgeTokens;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;


public class AutomatonGraphmlParser {

  private final static String SINK_NODE_ID = "sink";

  /**
  * Parses a Specification File and returns the Automata found in the file.
  */
  public static List<Automaton> parseAutomatonFile(Path pInputFile, Configuration config, LogManager pLogger, MachineModel pMachine) throws InvalidConfigurationException {

    //CParser cparser = CParser.Factory.getParser(config, pLogger, CParser.Factory.getOptions(config), pMachine);

    try (InputStream input = pInputFile.asByteSource().openStream()) {
      // Parse the XML document ----
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      Document doc = docBuilder.parse(input);
      doc.getDocumentElement().normalize();

      // (The one) root node of the graph ----
      NodeList graphs = doc.getElementsByTagName("graph");
      Preconditions.checkArgument(graphs.getLength() == 1, "The graph file must describe exactly one automaton.");
      Node graphNode = graphs.item(0);

      // Extract the information on the automaton ----
      Node nameAttribute = graphNode.getAttributes().getNamedItem("name");
      String automatonName = nameAttribute == null ? "" : nameAttribute.getTextContent();
      String initialStateName = getAttributeValue(graphNode, "entrynode", "Every graph needs a specified entry node!");

      // Create transitions ----
      NodeList edges = doc.getElementsByTagName("edge");
      Map<String, List<AutomatonTransition>> stateTransitions = Maps.newHashMap();
      for (int i=0; i<edges.getLength(); i++) {
        Node stateTransitionNode = edges.item(i);

        String sourceStateId = getAttributeValue(stateTransitionNode, "source", "Every transition needs a source!");
        String targetStateId = getAttributeValue(stateTransitionNode, "target", "Every transition needs a target!");
        String tokenString = getAttributeValue(stateTransitionNode, "tokens", "Every transition has to specify the set of tokens!");

        Optional<Boolean> matchNegativeSemantics = Optional.absent();
        switch(getAttributeValueWithDefault(stateTransitionNode, "negation", "").toLowerCase()) {
          case "true": matchNegativeSemantics = Optional.of(true); break;
          case "false": matchNegativeSemantics = Optional.of(false); break;
        }

        Set<Integer> matchTokens = parseTokens(tokenString);
        AutomatonBoolExpr trigger = new MatchEdgeTokens(matchTokens, matchNegativeSemantics);
        List<AutomatonBoolExpr> assertions = Collections.emptyList();
        List<AutomatonAction> actions = Collections.emptyList();

        List<AutomatonTransition> transitions = stateTransitions.get(sourceStateId);
        if (transitions == null) {
          transitions = Lists.newArrayList();
          stateTransitions.put(sourceStateId, transitions);
        }

        if (targetStateId.equalsIgnoreCase(SINK_NODE_ID)) {
          transitions.add(new AutomatonTransition(trigger, assertions, actions, AutomatonInternalState.BOTTOM));
          transitions.add(new AutomatonTransition(new AutomatonBoolExpr.Negation(trigger), assertions, actions, AutomatonInternalState.BOTTOM));
        } else {
          transitions.add(new AutomatonTransition(trigger, assertions, actions, targetStateId));
          transitions.add(new AutomatonTransition(new AutomatonBoolExpr.Negation(trigger), assertions, actions, AutomatonInternalState.BOTTOM));
        }

      }

      // Create states ----
      NodeList nodes = doc.getElementsByTagName("node");
      List<AutomatonInternalState> automatonStates = Lists.newArrayList();
      for (int i=0; i<nodes.getLength(); i++) {
        Node stateNode = nodes.item(i);

        String stateId = getAttributeValue(stateNode, "id", "Every state needs an ID!");

        List<AutomatonTransition> transitions = stateTransitions.get(stateId);
        if (transitions == null) {
          transitions = Collections.emptyList();
        }

        AutomatonInternalState state = new AutomatonInternalState(stateId, transitions, false, true);
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

  static Set<Integer> parseTokens(final String tokenString) {
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

  static String getAttributeValueWithDefault(Node of, String attributeName, String defaultValue) {
    Node attribute = of.getAttributes().getNamedItem(attributeName);
    if (attribute == null) {
      return defaultValue;
    } else {
      return attribute.getTextContent();
    }
  }

  static String getAttributeValue(Node of, String attributeName, String exceptionMessage) {
    Node attribute = of.getAttributes().getNamedItem(attributeName);
    Preconditions.checkNotNull(attribute, exceptionMessage);
    return attribute.getTextContent();
  }



}
