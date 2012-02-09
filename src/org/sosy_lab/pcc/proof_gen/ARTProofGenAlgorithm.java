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
package org.sosy_lab.pcc.proof_gen;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Stack;
import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.pcc.common.Separators;

@Options(prefix = "pcc.proofgen")
public abstract class ARTProofGenAlgorithm implements ProofGenAlgorithm {

  protected Vector<String> nodes = new Vector<String>();
  protected Vector<String> edges = new Vector<String>();
  protected HashSet<Integer> visitedNodes = new HashSet<Integer>();
  protected Configuration config;
  protected LogManager logger;

  @Option(
      description = "export ART representation needed for proof checking in PCC, if the error location is not reached, the representation depends on the algorithm used for proof checking")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File file = new File("pccProof.txt");

  @Option(name="cpa.predicate.abstraction.solver", toUppercase=true, values={"MATHSAT", "YICES"},
      description="which solver to use?")
  protected String whichProver = "MATHSAT";

  public ARTProofGenAlgorithm(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    config = pConfig;
    logger = pLogger;
    // set configuration options
    config.inject(this, ARTProofGenAlgorithm.class);
  }

  @Override
  public boolean writeProof(ARTElement pFirst) {
    // check if file instantiated properly
    if (file == null) {
      logger.log(Level.SEVERE, "No file specified to write ART.");
      return false;
    }
    // collects the information about ART nodes and edges
    if (!visitART(pFirst)) { return false; }
    // writes information about ART nodes and edges to file
    return writeART();
  }

  private boolean visitART(ARTElement pRoot) {
    Stack<ARTElement> toVisit = new Stack<ARTElement>();
    toVisit.push(pRoot);
    visitedNodes.add(pRoot.getElementId());
    ARTElement current;
    while (!toVisit.isEmpty()) {
      current = toVisit.pop();
      logger.log(Level.INFO, "Visit ART node " + current + " .");
      // build and save representation for this node
      if (!addARTNode(current)) { return false; }
      // consider current node's edges
      for (ARTElement child : current.getChildren()) {
        if (!(visitedNodes.contains(child.getElementId()))) {
          toVisit.push(child);
          visitedNodes.add(child.getElementId());
        }
        if (!addARTEdge(current, current.getEdgeToChild(child), child)) { return false; }
      }
    }
    return true;
  }

  private boolean writeART() {
    StringBuilder output = new StringBuilder();
    logger.log(Level.INFO, "Write ART nodes.");
    // add all nodes
    for (String node : nodes) {
      output.append(node);
    }
    // add separation between nodes and edges
    output.append(Separators.nodesFromEdgesSeparator+Separators.commonSeparator);
    logger.log(Level.INFO, "Write ART edges.");
    // add all edges
    for (String edge : edges) {
      output.append(edge);
    }
    try {
      Files.writeFile(file, output);
    } catch (IOException e) {
      logger.logUserException(Level.SEVERE, e,
          "Unable to write the abstract reachability tree.");
      return false;
    }
    return true;
  }

  protected ARTElement getFinalCoveringElement(ARTElement pNode){
    if(!pNode.isCovered()){
      return null;
    }
    pNode = pNode.getCoveringElement();
    while(pNode!= null && pNode.isCovered()){
      pNode = pNode.getCoveringElement();
    }
    return pNode;
  }

  protected abstract boolean addARTNode(ARTElement pNode);

  protected abstract boolean addARTEdge(ARTElement pSource, CFAEdge pEdge,
      ARTElement pTarget);

}