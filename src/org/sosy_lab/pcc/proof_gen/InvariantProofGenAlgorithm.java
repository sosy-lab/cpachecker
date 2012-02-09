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

import static org.sosy_lab.cpachecker.util.AbstractElements.extractLocation;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Stack;
import java.util.logging.Level;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.pcc.common.Pair;
import org.sosy_lab.pcc.common.Separators;

@Options
public abstract class InvariantProofGenAlgorithm implements ProofGenAlgorithm {

  protected Hashtable<Integer, StringBuilder> cfaNodeInvariants =
      new Hashtable<Integer, StringBuilder>();

  protected Configuration config;
  protected LogManager logger;

  @Option(
      description = "export ART representation needed for proof checking in PCC, if the error location is not reached, the representation depends on the algorithm used for proof checking")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File file = new File("pccProof.txt");

  @Option(name = "cpa.predicate.abstraction.solver", toUppercase = true, values = { "MATHSAT", "YICES" },
      description = "which solver to use?")
  protected String whichProver = "MATHSAT";

  public InvariantProofGenAlgorithm(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    config = pConfig;
    logger = pLogger;
    // set configuration options
    config.inject(this, InvariantProofGenAlgorithm.class);
  }

  @Override
  public boolean writeProof(ARTElement pFirst) {
    // check if file instantiated properly
    if (file == null) {
      logger.log(Level.SEVERE, "No file specified to write proof.");
      return false;
    }
    // collect necessary information from ART
    if (!visitART(pFirst)) { return false; }
    // write proof to file
    return writeInvariantsAndOperations();
  }

  private boolean visitART(ARTElement pRoot) {
    Stack<Pair<ARTElement, String>> toVisit =
        new Stack<Pair<ARTElement, String>>();
    HashSet<Integer> visitedNodes = new HashSet<Integer>();
    //set up
    toVisit.push(new Pair<ARTElement, String>(pRoot, ""));
    visitedNodes.add(pRoot.getElementId());
    Pair<ARTElement, String> visiting;
    ARTElement current;
    CFAEdge edge;
    String newCallstack;
    //start visit
    while (!toVisit.isEmpty()) {
      visiting = toVisit.pop();
      current = visiting.getFirst();
      logger.log(Level.INFO, "Visit ART node " + current + " .");
      // build and save representation for this node
      if (!addInvariant(current, visiting.getSecond())) { return false; }
      // consider current node's edges
      for (ARTElement child : current.getChildren()) {
        if (!(visitedNodes.contains(child.getElementId()))) {
          edge = current.getEdgeToChild(child);
          if (edge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
            newCallstack =
                visiting.getSecond()
                    + Separators.stackEntrySeparator
                    + extractLocation(current)
                        .getLeavingSummaryEdge().getSuccessor().getNodeNumber();
            toVisit.push(new Pair<ARTElement, String>(child, newCallstack));
          } else {
            if (edge.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
              newCallstack = visiting.getSecond();
              newCallstack =
                  newCallstack.substring(0,
                      newCallstack.lastIndexOf(Separators.stackEntrySeparator));
              toVisit.push(new Pair<ARTElement, String>(child, newCallstack));
            } else {
              toVisit.push(new Pair<ARTElement, String>(child, visiting
                  .getSecond()));
            }
          }
          visitedNodes.add(child.getElementId());
        }
        if (!addOperation(current, child)) { return false; }
      }
    }
    return true;
  }

  protected abstract boolean addOperation(ARTElement pSource, ARTElement pTarget);

  protected abstract StringBuilder writeOperations();

  private boolean writeInvariantsAndOperations() {
    StringBuilder output = new StringBuilder();
    // add all invariants
    logger.log(Level.INFO, "Write regions per CFA node.");
    StringBuilder toWrite;
    for (Integer cfaID : cfaNodeInvariants.keySet()) {
      output.append(cfaID + Separators.commonSeparator);
      toWrite = cfaNodeInvariants.get(cfaID);
      output.append(countNumOccurrences(toWrite, Separators.commonSeparator)
          + Separators.commonSeparator + toWrite.toString());
    }
    // add separation between invariants and operations
    output.append(Separators.nodesFromEdgesSeparator + Separators.commonSeparator);
    // add all operations
    logger.log(Level.INFO, "Write edges between regions.");
    toWrite = writeOperations();
    if (toWrite == null) { return false; }
    output.append(toWrite.toString());
    try {
      Files.writeFile(file, output);
    } catch (IOException e) {
      logger.logUserException(Level.SEVERE, e,
          "Unable to write the abstract reachability tree.");
      return false;
    }
    return true;
  }

  protected int countNumOccurrences(StringBuilder pSource, String pOccur) {
    int i = -1;
    int pos, index = 0;
    do {
      i++;
      pos = pSource.indexOf(pOccur, index);
      index = pos + 1;
    } while (pos != -1);
    return i;
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

  protected abstract boolean addInvariant(ARTElement pNode, String pStack);
}
