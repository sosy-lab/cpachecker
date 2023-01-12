// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.function;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
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
import org.sosy_lab.cpachecker.util.CFAUtils;

/** This class moves the declarations inside of each function to the beginning of them. */
public class CFADeclarationMover {

  LogManager logger;

  public CFADeclarationMover(LogManager log) {
    logger = log;
  }

  public void moveDeclarationsToFunctionStart(MutableCFA cfa) {
    Collection<FunctionEntryNode> startNodes = cfa.getAllFunctionHeads();
    for (FunctionEntryNode node : startNodes) {
      handleDeclarationsInFunction(node, cfa);
    }
  }

  private void handleDeclarationsInFunction(FunctionEntryNode startNode, MutableCFA cfa) {
    CFAEdge firstRealFunctionEdge = startNode.getLeavingEdge(0);
    List<CFAEdge> secondRealFunctionEdge = new ArrayList<>();
    AFunctionDeclaration function = startNode.getFunction();

    // all Blank -or Declarationedges are valid before we insert the moved declarations
    // if we would not take this order, there could be some problems with initializing
    // variables
    while (!firstRealFunctionEdge.getDescription().equals("Function start dummy edge")) {
      firstRealFunctionEdge = firstRealFunctionEdge.getSuccessor().getLeavingEdge(0);
    }

    CFANode actNode = firstRealFunctionEdge.getSuccessor();
    CFAUtils.leavingEdges(actNode).copyInto(secondRealFunctionEdge);
    List<CFAEdge> declarations = collectDeclarations(actNode);

    // if some declarations were found we need to create the blank edge which
    // should occur after the declarations, and then remove the leaving edges from the
    // node where we want to insert the declarations
    if (!declarations.isEmpty()) {
      // create declaration end edge, no need to add it as leaving edge to the actNode
      // this will be done in the end
      CFANode tmpNode = new CFANode(function);
      cfa.addNode(tmpNode);
      CFAEdge declEndEdge =
          new BlankEdge(
              "End of Declarations", FileLocation.DUMMY, actNode, tmpNode, "End of Declarations");
      tmpNode.addEnteringEdge(declEndEdge);

      // move former second function edge to node after declEndEdge and set
      // second function edge to declEndedge
      for (CFAEdge e : secondRealFunctionEdge) {
        CFAEdge tmpEdge = moveEdgeToOtherPredecessor(e, tmpNode);
        actNode.removeLeavingEdge(e);
        if (declarations.contains(e)) {
          declarations.add(declarations.indexOf(e), tmpEdge);
          declarations.remove(e);
        }
      }
      secondRealFunctionEdge.clear();
      secondRealFunctionEdge.add(declEndEdge);
    }

    // insert declarations into the desired destination
    for (CFAEdge decl : declarations) {
      CFANode middleNode = new CFANode(function);
      cfa.addNode(middleNode);
      moveDeclEdgeToNewLocation((CDeclarationEdge) decl, actNode, middleNode);
      actNode = middleNode;
    }

    // set new predecessors for before deleted leavingedges if there were some
    // declarations inserted
    if (!declarations.isEmpty()) {
      for (CFAEdge e : secondRealFunctionEdge) {
        moveEdgeToOtherPredecessor(e, actNode);
      }
    }
  }

  private CFAEdge moveEdgeToOtherPredecessor(CFAEdge edge, CFANode pred) {
    CFANode succ = edge.getSuccessor();
    succ.removeEnteringEdge(edge);
    switch (edge.getEdgeType()) {
      case AssumeEdge:
        edge =
            new CAssumeEdge(
                edge.getRawStatement(),
                edge.getFileLocation(),
                pred,
                edge.getSuccessor(),
                ((CAssumeEdge) edge).getExpression(),
                ((CAssumeEdge) edge).getTruthAssumption(),
                ((CAssumeEdge) edge).isSwapped(),
                ((CAssumeEdge) edge).isArtificialIntermediate());
        pred.addLeavingEdge(edge);
        succ.addEnteringEdge(edge);
        return edge;
      case BlankEdge:
        edge =
            new BlankEdge(
                edge.getRawStatement(),
                edge.getFileLocation(),
                pred,
                edge.getSuccessor(),
                edge.getDescription());
        pred.addLeavingEdge(edge);
        succ.addEnteringEdge(edge);
        return edge;
      case DeclarationEdge:
        edge =
            new CDeclarationEdge(
                edge.getRawStatement(),
                edge.getFileLocation(),
                pred,
                edge.getSuccessor(),
                ((CDeclarationEdge) edge).getDeclaration());
        pred.addLeavingEdge(edge);
        succ.addEnteringEdge(edge);
        return edge;
      case ReturnStatementEdge:
        edge =
            new CReturnStatementEdge(
                edge.getRawStatement(),
                ((CReturnStatementEdge) edge).getReturnStatement(),
                edge.getFileLocation(),
                pred,
                (FunctionExitNode) edge.getSuccessor());
        pred.addLeavingEdge(edge);
        succ.addEnteringEdge(edge);
        return edge;
      case StatementEdge:
        edge =
            new CStatementEdge(
                edge.getRawStatement(),
                ((CStatementEdge) edge).getStatement(),
                edge.getFileLocation(),
                pred,
                edge.getSuccessor());
        pred.addLeavingEdge(edge);
        succ.addEnteringEdge(edge);
        return edge;
      case CallToReturnEdge:
      case FunctionReturnEdge:
      default:
        throw new AssertionError("should never happen");
    }
  }

  private void moveDeclEdgeToNewLocation(CDeclarationEdge edge, CFANode pred, CFANode succ) {
    CDeclaration decl = edge.getDeclaration();
    if (!(decl instanceof CVariableDeclaration)) {
      throw new AssertionError("Only variable declaration edges should be moved!");
    }

    // get previous predecessor and successor (this is where an assignment statement will be
    // inserted
    // with the initializer expression as righthandside)
    CFANode actPred = edge.getPredecessor();
    CFANode actSucc = edge.getSuccessor();
    CVariableDeclaration varDecl = (CVariableDeclaration) decl;
    CInitializer init = varDecl.getInitializer();
    if (init instanceof CInitializerExpression) {
      actPred.removeLeavingEdge(edge);
      actSucc.removeEnteringEdge(edge);
      CExpressionAssignmentStatement stmt =
          new CExpressionAssignmentStatement(
              varDecl.getFileLocation(),
              new CIdExpression(varDecl.getFileLocation(), varDecl),
              ((CInitializerExpression) init).getExpression());
      CStatementEdge midEdge =
          new CStatementEdge(
              edge.getRawStatement(), stmt, edge.getFileLocation(), actPred, actSucc);
      actPred.addLeavingEdge(midEdge);
      actSucc.addEnteringEdge(midEdge);

      // this is a struct or array initialisation with either values or designators
    } else if (init != null) {
      logger.log(
          Level.WARNING,
          "Moving declaration to function start does not work correctly for initializer lists and"
              + " designated initializers for arrays or structs, do not use the CFADeclarationMover"
              + " if you are able to handle such expressions.");
      actPred.removeLeavingEdge(edge);
      actSucc.removeEnteringEdge(edge);
      BlankEdge midEdge =
          new BlankEdge(
              edge.getRawStatement(),
              edge.getFileLocation(),
              actPred,
              actSucc,
              "Declaration was moved to function start");
      actPred.addLeavingEdge(midEdge);
      actSucc.addEnteringEdge(midEdge);

      // in order to not have to remove one node, and the edges around the previous place
      // of the declaration, we just insert a blankedge instead
    } else {
      actPred.removeLeavingEdge(edge);
      actSucc.removeEnteringEdge(edge);
      BlankEdge midEdge =
          new BlankEdge(
              edge.getRawStatement(),
              edge.getFileLocation(),
              actPred,
              actSucc,
              "Declaration was moved to function start");
      actPred.addLeavingEdge(midEdge);
      actSucc.addEnteringEdge(midEdge);
    }

    // create the new variabledeclaration, always without initializer
    // the initializer is now just an assignment, so we don't need it in the
    // declaration anymore
    CVariableDeclaration declWithoutInitializer =
        new CVariableDeclaration(
            varDecl.getFileLocation(),
            varDecl.isGlobal(),
            varDecl.getCStorageClass(),
            varDecl.getType(),
            varDecl.getName(),
            varDecl.getOrigName(),
            varDecl.getQualifiedName(),
            null);
    CDeclarationEdge newEdge =
        new CDeclarationEdge(
            edge.getRawStatement(), edge.getFileLocation(), pred, succ, declWithoutInitializer);

    pred.addLeavingEdge(newEdge);
    succ.addEnteringEdge(newEdge);
  }

  /** This method collects all Declarations in a function from a given starting node. */
  private List<CFAEdge> collectDeclarations(CFANode startNode) {
    DeclarationCollector dc = new DeclarationCollector();
    CFATraversal.dfs().ignoreSummaryEdges().ignoreFunctionCalls().traverseOnce(startNode, dc);
    return dc.getCollectedDeclarations();
  }

  /**
   * This visitor collects all DeclarationEdges from a single startNode. Using it with
   * ignoreSummaryEdges and ignoreFunctionCalls, collects all Declarations in a Function.
   */
  private static class DeclarationCollector extends DefaultCFAVisitor {

    private final List<CFAEdge> edges;

    public DeclarationCollector() {
      edges = new ArrayList<>();
    }

    public List<CFAEdge> getCollectedDeclarations() {
      return edges;
    }

    @Override
    public TraversalProcess visitEdge(CFAEdge edge) {
      // only sum up variabledeclarations
      if (edge instanceof ADeclarationEdge
          && (((ADeclarationEdge) edge).getDeclaration() instanceof AVariableDeclaration)) {
        edges.add(edge);
      }
      return TraversalProcess.CONTINUE;
    }
  }
}
