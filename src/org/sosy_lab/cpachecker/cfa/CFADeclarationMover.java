/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;


/**
 * This class moves the declarations inside of each function to the beginning of
 * them.
 */
public class CFADeclarationMover {

  LogManager logger;

  public CFADeclarationMover(LogManager log) {
    logger = log;
  }

  public void moveDeclarationsToFunctionStart(MutableCFA cfa) {
    Collection<FunctionEntryNode> startNodes = cfa.getAllFunctionHeads();
    for (FunctionEntryNode node : startNodes) {
      if (node.getNodeNumber() != 1) {
        handleDeclarationsInFunction(node, cfa);
      }
    }
  }

  private void handleDeclarationsInFunction(FunctionEntryNode startNode, MutableCFA cfa) {
    CFAEdge firstRealFunctionEdge = startNode.getLeavingEdge(0);
    List<CFAEdge> secondRealFunctionEdge = new ArrayList<>();
    String functionName = startNode.getFunctionName();

    CFAEdge tmpEdge;
    // all Blank -or Declarationedges are valid before we insert the moved declarations
    // if we would not take this order, there could be some problems with initializing
    // variables
    while (firstRealFunctionEdge instanceof CDeclarationEdge || firstRealFunctionEdge instanceof BlankEdge) {
      // we found an end of the cfa, so we can just skip here
      if (firstRealFunctionEdge.getSuccessor().getNumLeavingEdges() == 0) {
        return;
      }
      // get next edge
      tmpEdge =  firstRealFunctionEdge.getSuccessor().getLeavingEdge(0);

      // we can skip this loop here if we have more than one leaving edge or something
      // else than Blank or Declarationedges
      if (firstRealFunctionEdge.getSuccessor().getNumLeavingEdges() == 1 && (tmpEdge instanceof CDeclarationEdge || tmpEdge instanceof BlankEdge)) {
        // these are for example Loops like Label Error: -> GOTO: Error
        if (tmpEdge.equals(firstRealFunctionEdge)) {
          return;
        }
        firstRealFunctionEdge = tmpEdge;

      } else {
        // check if there is more than one successor
        if (firstRealFunctionEdge.getSuccessor().getNumLeavingEdges() == 1) {
          secondRealFunctionEdge.add(tmpEdge);
        } else if (firstRealFunctionEdge.getSuccessor().getNumLeavingEdges() == 2) {
          secondRealFunctionEdge.add(tmpEdge);
          secondRealFunctionEdge.add(firstRealFunctionEdge.getSuccessor().getLeavingEdge(1));
        } else {
          assert false;
        }
        break;
      }
    }

    CFANode actNode = firstRealFunctionEdge.getSuccessor();
    List<CFAEdge> declarations = collectDeclarations(actNode);

    Iterator<CFAEdge> it = declarations.iterator();

    // if some declarations were found we need to remove the leaving edges from the
    // node where we want to insert the declarations
    if (it.hasNext()) {
      for (CFAEdge e : secondRealFunctionEdge) {
        actNode.removeLeavingEdge(e);
      }
    }

    // insert declarations into the desired destination
    while (it.hasNext()) {
      CFAEdge decl = it.next();
      CFANode middleNode = new CFANode(decl.getLineNumber(), functionName);
      cfa.addNode(middleNode);
      moveDeclEdgeToNewLocation((CDeclarationEdge)decl, actNode, middleNode, cfa);
      actNode = middleNode;
    }

    // set new predecessors for bevore deleted leavingedges
    for (CFAEdge e : secondRealFunctionEdge) {
      moveEdgeToOtherPredecessor(e, actNode);
    }
  }

  private void moveEdgeToOtherPredecessor(CFAEdge edge, CFANode pred) {
    CFANode succ = edge.getSuccessor();
    succ.removeEnteringEdge(edge);
    switch (edge.getEdgeType()) {
    case AssumeEdge:
      edge = new CAssumeEdge(((CAssumeEdge)edge).getRawStatement(),
                             edge.getLineNumber(),
                             pred,
                             edge.getSuccessor(),
                             ((CAssumeEdge)edge).getExpression(),
                             ((CAssumeEdge)edge).getTruthAssumption());
      pred.addLeavingEdge(edge);
      succ.addEnteringEdge(edge);
      return;
    case BlankEdge:
      edge = new BlankEdge(((BlankEdge)edge).getRawStatement(),
                            edge.getLineNumber(),
                            pred,
                            edge.getSuccessor(),
                            ((CAssumeEdge)edge).getDescription());
      pred.addLeavingEdge(edge);
      succ.addEnteringEdge(edge);
      return;
    case DeclarationEdge:
      edge = new CDeclarationEdge(((CDeclarationEdge)edge).getRawStatement(),
                                  edge.getLineNumber(),
                                  pred,
                                  edge.getSuccessor() ,
                                  ((CDeclarationEdge)edge).getDeclaration());
      pred.addLeavingEdge(edge);
      succ.addEnteringEdge(edge);
      return;
    case ReturnStatementEdge:
      edge = new CReturnStatementEdge(((CReturnStatementEdge)edge).getRawStatement(),
                                      ((CReturnStatementEdge)edge).getRawAST().orNull(),
                                      edge.getLineNumber(),
                                      pred ,
                                      (FunctionExitNode) edge.getSuccessor());
      pred.addLeavingEdge(edge);
      succ.addEnteringEdge(edge);
      return;
    case StatementEdge:
      edge = new CStatementEdge(((CStatementEdge)edge).getRawStatement(),
                                ((CStatementEdge)edge).getStatement(),
                                edge.getLineNumber(),
                                pred,
                                edge.getSuccessor());
      pred.addLeavingEdge(edge);
      succ.addEnteringEdge(edge);
      break;
    case CallToReturnEdge:
    case FunctionReturnEdge:
    case MultiEdge:
      throw new AssertionError("should never happen");
    default:
      break;
    }
  }

  private void moveDeclEdgeToNewLocation(CDeclarationEdge edge, CFANode pred, CFANode succ, MutableCFA cfa) {
    CDeclaration decl = edge.getDeclaration();
    if (!(decl instanceof CVariableDeclaration)) {
      throw new AssertionError("Only variable declaration edges should be moved!");
    }

    // get previous predecessor and successor (this is where an assignment statement will be inserted
    // with the initializer expression as righthandside)
    CFANode actPred = edge.getPredecessor();
    CFANode actSucc = edge.getSuccessor();
    CVariableDeclaration varDecl = (CVariableDeclaration) decl;
    CInitializer init = varDecl.getInitializer();
    if (init instanceof CInitializerExpression) {
      actPred.removeLeavingEdge(edge);
      actSucc.removeEnteringEdge(edge);
      CExpressionAssignmentStatement stmt = new CExpressionAssignmentStatement(varDecl.getFileLocation(),
                                                                               new CIdExpression(varDecl.getFileLocation(), varDecl.getType(), varDecl.getName(), varDecl),
                                                                               ((CInitializerExpression) init).getExpression());
      CStatementEdge midEdge = new CStatementEdge(edge.getRawStatement(), stmt, edge.getLineNumber(), actPred, actSucc);
      actPred.addLeavingEdge(midEdge);
      actSucc.addEnteringEdge(midEdge);

    } else if (init != null) {
      throw new UnsupportedOperationException("Designated initializers, more implementation work to be done in CFADeclarationMover...");

      // in order to not have to remove one node, and the edges around the previous place
      // of the declaration, we just insert a blankedge instead
    } else {
      actPred.removeLeavingEdge(edge);
      actSucc.removeEnteringEdge(edge);
      BlankEdge midEdge = new BlankEdge(edge.getRawStatement(), edge.getLineNumber(), actPred, actSucc, "Declaration was moved to function start");
      actPred.addLeavingEdge(midEdge);
      actSucc.addEnteringEdge(midEdge);
    }

    // create the new variabledeclaration, always without initializer
    // the initializer is now just an assignment, so we don't need it in the
    // declaration anymore
    CVariableDeclaration declWithoutInitializer = new CVariableDeclaration(varDecl.getFileLocation(), varDecl.isGlobal(), varDecl.getCStorageClass(), varDecl.getType(), varDecl.getName(), varDecl.getOrigName(), varDecl.getQualifiedName(), null);
    CDeclarationEdge newEdge = new CDeclarationEdge(edge.getRawStatement(), edge.getLineNumber(), pred, succ, declWithoutInitializer);

    pred.addLeavingEdge(newEdge);
    succ.addEnteringEdge(newEdge);
  }

  /**
   * This method collects all Declarations in a function from a given starting node.
   */
  private List<CFAEdge> collectDeclarations(CFANode startNode) {
    DeclarationCollector dc = new DeclarationCollector();
    CFATraversal.dfs().ignoreSummaryEdges().ignoreFunctionCalls().traverseOnce(startNode, dc);
    return dc.getCollectedDeclarations();
  }

  /**
   * This visitor collects all DeclarationEdges from a single startNode.
   * Using it with ignoreSummaryEdges and ignoreFunctionCalls, collects all
   * Declarations in a Function.
   */
  class DeclarationCollector extends DefaultCFAVisitor {

    private final List<CFAEdge> edges;

    public DeclarationCollector() {
      edges = new ArrayList<>();
    }

    public List<CFAEdge> getCollectedDeclarations() {
      return edges;
    }

    @Override
    public TraversalProcess visitEdge(CFAEdge edge) {
      if (edge instanceof CDeclarationEdge) {
        edges.add(edge);
      }
      return TraversalProcess.CONTINUE;
    }
  }
}
