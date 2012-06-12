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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.java;

import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.cpachecker.cfa.CFACreationUtils.isReachableNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.IASTAssignment;
import org.sosy_lab.cpachecker.cfa.ast.IASTFileLocation;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.ast.IASTVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.objectmodel.BlankEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionExitNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.CFAGenerationRuntimeException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFAUtils;

import com.google.common.collect.ImmutableList;

/**
 * Builder to traverse AST.
 * Known Limitations:
 * <p> -- K&R style function definitions not implemented
 * <p> -- Pointer modifiers not tracked (i.e. const, volatile, etc. for *
 */
class CFAFunctionBuilder extends ASTVisitor {

  private static final boolean PROCESS_CONTINUE = true;

  private static final boolean PROCESS_SKIP = false;

  // Data structure for maintaining our scope stack in a function
  private final Deque<CFANode> locStack = new ArrayDeque<CFANode>();

  // Data structures for handling loops & else conditions
  //private final Deque<CFANode> loopStartStack = new ArrayDeque<CFANode>();
  //private final Deque<CFANode> loopNextStack  = new ArrayDeque<CFANode>(); // For the node following the current if / while block
  private final Deque<CFANode> elseStack      = new ArrayDeque<CFANode>();

  // Data structure for handling switch-statements
  //private final Deque<org.sosy_lab.cpachecker.cfa.ast.IASTExpression> switchExprStack =
  //  new ArrayDeque<org.sosy_lab.cpachecker.cfa.ast.IASTExpression>();
  //private final Deque<CFANode> switchCaseStack = new ArrayDeque<CFANode>();

  // Data structures for handling goto
  //private final Map<String, CFALabelNode> labelMap = new HashMap<String, CFALabelNode>();
  //private final Multimap<String, CFANode> gotoLabelNeeded = ArrayListMultimap.create();

  // Data structures for handling function declarations
  private CFAFunctionDefinitionNode cfa = null;
  private final Set<CFANode> cfaNodes = new HashSet<CFANode>();

  private final Scope scope;
  private final ASTConverter astCreator;

  private final LogManager logger;


  public CFAFunctionBuilder(LogManager pLogger, boolean pIgnoreCasts,
      Scope pScope, ASTConverter pAstCreator) {

    logger = pLogger;
    scope = pScope;
    astCreator = pAstCreator;


  }

  CFAFunctionDefinitionNode getStartNode() {
    checkState(cfa != null);
    return cfa;
  }

  Set<CFANode> getCfaNodes() {
    checkState(cfa != null);
    return cfaNodes;
  }

  /* (non-Javadoc)
   * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#visit(org.eclipse.cdt.core.dom.ast.IASTDeclaration)
   */
  @Override
  public boolean visit(MethodDeclaration declaration) {



    if (locStack.size() != 0) {
      throw new CFAGenerationRuntimeException("Nested function declarations?");
    }

    assert cfa == null;

    final org.sosy_lab.cpachecker.cfa.ast.IASTFunctionDeclaration fdef = astCreator.convert(declaration);
    final String nameOfFunction = fdef.getName();
    assert !nameOfFunction.isEmpty();

    scope.enterFunction(fdef);

    final List<org.sosy_lab.cpachecker.cfa.ast.IASTParameterDeclaration> parameters = fdef.getDeclSpecifier().getParameters();
    final List<String> parameterNames = new ArrayList<String>(parameters.size());

    for (org.sosy_lab.cpachecker.cfa.ast.IASTParameterDeclaration param : parameters) {
      scope.registerDeclaration(param); // declare parameter as local variable
      parameterNames.add(param.getName());
    }

    final CFAFunctionExitNode returnNode = new CFAFunctionExitNode(fdef.getFileLocation().getEndingLineNumber(), nameOfFunction);
    cfaNodes.add(returnNode);

    final CFAFunctionDefinitionNode startNode = new FunctionDefinitionNode(
        fdef.getFileLocation().getStartingLineNumber(), fdef, returnNode, parameterNames);
    cfaNodes.add(startNode);
    cfa = startNode;

    final CFANode nextNode = new CFANode(fdef.getFileLocation().getStartingLineNumber(), nameOfFunction);
    cfaNodes.add(nextNode);
    locStack.add(nextNode);

    final BlankEdge dummyEdge = new BlankEdge("", fdef.getFileLocation().getStartingLineNumber(),
        startNode, nextNode, "Function start dummy edge");
    addToCFA(dummyEdge);

    // Stop , and manually go to Block, to protect parameter variables to be processed
    // more than one time
    // TODO Find a better Solution, this should not work
    declaration.getBody().accept(this);
    return PROCESS_SKIP;

  }

  @Override
  public boolean visit(final VariableDeclarationFragment sd) {

    assert (locStack.size() > 0) : "not in a function's scope";

    CFANode prevNode = locStack.pop();

    CFANode nextNode = addDeclarationsToCFA(sd, prevNode);

    assert nextNode != null;
    locStack.push(nextNode);

    return PROCESS_SKIP;
  }

  @Override
  public boolean visit(final SingleVariableDeclaration sd) {

    assert (locStack.size() > 0) : "not in a function's scope";

    CFANode prevNode = locStack.pop();

    CFANode nextNode = addDeclarationsToCFA(sd, prevNode);

    assert nextNode != null;
    locStack.push(nextNode);

    return PROCESS_SKIP;
  }

  @Override
  public void endVisit(MethodDeclaration declaration) {

      if (locStack.size() != 1) {
        throw new CFAGenerationRuntimeException("Depth wrong. Geoff needs to do more work");
      }

      CFANode lastNode = locStack.pop();

      if (isReachableNode(lastNode)) {
        BlankEdge blankEdge = new BlankEdge("",
            lastNode.getLineNumber(), lastNode, cfa.getExitNode(), "default return");
        addToCFA(blankEdge);
      }



      Set<CFANode> reachableNodes = CFATraversal.dfs().collectNodesReachableFrom(cfa);


      Iterator<CFANode> it = cfaNodes.iterator();
      while (it.hasNext()) {
        CFANode n = it.next();

        if (!reachableNodes.contains(n)) {
          // node was created but isn't part of CFA (e.g. because of dead code)
          it.remove(); // remove n from currentCFANodes
        }
      }

      scope.leaveFunction();
  }





  /**
   * This method creates statement and declaration edges for all sideassignments.
   *
   * @return the nextnode
   */
  private CFANode handleSideassignments(CFANode prevNode, String rawSignature, int filelocStart) {
    CFANode nextNode = null;
    while(astCreator.numberOfSideAssignments() > 0){
      nextNode = new CFANode(filelocStart, cfa.getFunctionName());
      cfaNodes.add(nextNode);

      IASTNode sideeffect = astCreator.getNextSideAssignment();

      createSideAssignmentEdges(prevNode, nextNode, rawSignature, filelocStart, sideeffect);
      prevNode = nextNode;
    }
    return prevNode;
  }


  /**
   * Submethod from handleSideassignments, takes an IASTNode and depending on its
   * type creates an edge.
   */
  private void createSideAssignmentEdges(CFANode prevNode, CFANode nextNode, String rawSignature,
      int filelocStart, IASTNode sideeffect) {
    CFAEdge previous;
    if(sideeffect instanceof org.sosy_lab.cpachecker.cfa.ast.IASTStatement) {
      previous = new StatementEdge(rawSignature, (org.sosy_lab.cpachecker.cfa.ast.IASTStatement)sideeffect, filelocStart, prevNode, nextNode);
    } else if (sideeffect instanceof IASTAssignment) {
      previous = new StatementEdge(rawSignature, (org.sosy_lab.cpachecker.cfa.ast.IASTStatement)sideeffect, filelocStart, prevNode, nextNode);
    } else if (sideeffect instanceof IASTIdExpression) {
      previous = new StatementEdge(rawSignature, new org.sosy_lab.cpachecker.cfa.ast.IASTExpressionStatement(sideeffect.getFileLocation(), (org.sosy_lab.cpachecker.cfa.ast.IASTExpression) sideeffect), filelocStart, prevNode, nextNode);
    } else {
      previous = new DeclarationEdge(rawSignature, filelocStart, prevNode, nextNode, (org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration) sideeffect);
    }
    addToCFA(previous);
  }





  /**
   * This method takes a list of Declarations and adds them to the CFA.
   * The edges are inserted after startNode.
   * @return the node after the last of the new declarations
   */
  private CFANode addDeclarationsToCFA(final VariableDeclaration sd, CFANode prevNode) {

    final List<org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration> declList =
        astCreator.convert(sd);
    final String rawSignature = sd.getName().getFullyQualifiedName();

    prevNode = handleSideassignments(prevNode, rawSignature, declList.get(0).getFileLocation().getStartingLineNumber());

    // create one edge for every declaration
    for (org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration newD : declList) {

      if (newD instanceof IASTVariableDeclaration) {
        scope.registerDeclaration(newD);
      } else if (newD instanceof IASTFunctionDeclaration) {
        scope.registerFunctionDeclaration((IASTFunctionDeclaration) newD);
      }

      CFANode nextNode = new CFANode(declList.get(0).getFileLocation().getStartingLineNumber(), cfa.getFunctionName());
      cfaNodes.add(nextNode);

      final DeclarationEdge edge = new DeclarationEdge(rawSignature, declList.get(0).getFileLocation().getStartingLineNumber(),
          prevNode, nextNode, newD);
      addToCFA(edge);

      prevNode = nextNode;
    }

    return prevNode;
  }


 private void handleStatements(Statement statement){
    // TODO Investigate if this works
    // Iterate through parent till parent is not a Block
    ASTNode node = statement;

    while(node.getParent().getNodeType() == ASTNode.BLOCK){
       node = node.getParent();
    }


   // Handle special condition for else
   // TODO Ask why == works, but not equals
   if (node.getLocationInParent() == IfStatement.ELSE_STATEMENT_PROPERTY) {
     // Edge from current location to post if-statement location
     CFANode prevNode = locStack.pop();
     CFANode nextNode = locStack.peek();

     if (isReachableNode(prevNode)) {
       BlankEdge blankEdge = new BlankEdge("", nextNode.getLineNumber(), prevNode, nextNode, "");
       addToCFA(blankEdge);
     }

     //  Push the start of the else clause onto our location stack
     CFANode elseNode = elseStack.pop();
     locStack.push(elseNode);
   }
 }

 @Override
 public boolean visit(ExpressionStatement expressionStatement) {

   handleStatements(expressionStatement);

   CFANode prevNode = locStack.pop ();

   org.sosy_lab.cpachecker.cfa.ast.IASTStatement statement = astCreator.convert(expressionStatement);

   //TODO Solution not allowed, find better Solution
   String rawSignature = expressionStatement.toString();

   CFANode lastNode = new CFANode(statement.getFileLocation().getStartingLineNumber(), cfa.getFunctionName());
   cfaNodes.add(lastNode);

   if(astCreator.getConditionalExpression() != null) {
     //TODO Implement ConditionalStatement

     //IASTConditionalExpression condExp = astCreator.getConditionalExpression();
     //astCreator.resetConditionalExpression();
     //handleConditionalStatement(condExp, prevNode, lastNode, statement);
   } else {
     CFANode nextNode = handleSideassignments(prevNode, rawSignature, statement.getFileLocation().getStartingLineNumber());

     StatementEdge edge = new StatementEdge(rawSignature, statement,
         statement.getFileLocation().getStartingLineNumber(), nextNode, lastNode);
     addToCFA(edge);
   }
   locStack.push(lastNode);



   return PROCESS_SKIP;
 }


 @Override
  public boolean visit(IfStatement ifStatement) {
   IASTFileLocation fileloc = astCreator.getFileLocation(ifStatement);

   handleStatements(ifStatement);


    CFANode prevNode = locStack.pop();

    CFANode postIfNode = new CFANode(fileloc.getEndingLineNumber(),
        cfa.getFunctionName());
    cfaNodes.add(postIfNode);
    locStack.push(postIfNode);

    CFANode thenNode = new CFANode(fileloc.getStartingLineNumber(),
        cfa.getFunctionName());
    cfaNodes.add(thenNode);
    locStack.push(thenNode);

    CFANode elseNode;
    // elseNode is the start of the else branch,
    // or the node after the loop if there is no else branch
    if (ifStatement.getElseStatement() == null) {
      elseNode = postIfNode;
    } else {
      elseNode = new CFANode(fileloc.getStartingLineNumber(),
          cfa.getFunctionName());
      cfaNodes.add(elseNode);
      elseStack.push(elseNode);
    }

    createConditionEdges(ifStatement.getExpression(),
        fileloc.getStartingLineNumber(), prevNode, thenNode, elseNode);

    return PROCESS_CONTINUE;
  }

 @Override
 public void endVisit(IfStatement ifStatement) {

     final CFANode prevNode = locStack.pop();
     final CFANode nextNode = locStack.peek();

     if (isReachableNode(prevNode)) {

       for (CFAEdge prevEdge : ImmutableList.copyOf(CFAUtils.allEnteringEdges(prevNode))) {
         if ((prevEdge instanceof BlankEdge)
             && prevEdge.getDescription().equals("")) {

           // the only entering edge is a BlankEdge, so we delete this edge and prevNode

           CFANode prevPrevNode = prevEdge.getPredecessor();
           assert prevPrevNode.getNumLeavingEdges() == 1;
           prevNode.removeEnteringEdge(prevEdge);
           prevPrevNode.removeLeavingEdge(prevEdge);

           BlankEdge blankEdge = new BlankEdge("", prevNode.getLineNumber(),
               prevPrevNode, nextNode, "");
           addToCFA(blankEdge);
         }
       }

       if (prevNode.getNumEnteringEdges() > 0) {
         BlankEdge blankEdge = new BlankEdge("", prevNode.getLineNumber(),
             prevNode, nextNode, "");
         addToCFA(blankEdge);
       }
     }
 }





  private static enum CONDITION { NORMAL, ALWAYS_FALSE, ALWAYS_TRUE };

  private void createConditionEdges(final Expression condition,
      final int filelocStart, CFANode rootNode, CFANode thenNode,
      final CFANode elseNode) {

    assert condition != null;

    final CONDITION kind = getConditionKind(condition);
    //TODO solution not allowed, find better Solution
        String rawSignature = condition.toString();

    switch (kind) {
    case ALWAYS_FALSE:
      // no edge connecting rootNode with thenNode,
      // so the "then" branch won't be connected to the rest of the CFA

      final BlankEdge falseEdge = new BlankEdge(rawSignature, filelocStart, rootNode, elseNode, "");
      addToCFA(falseEdge);
      break;

    case ALWAYS_TRUE:
      final BlankEdge trueEdge = new BlankEdge(rawSignature, filelocStart, rootNode, thenNode, "");
      addToCFA(trueEdge);

      // no edge connecting prevNode with elseNode,
      // so the "else" branch won't be connected to the rest of the CFA
      break;

    case NORMAL:
        buildConditionTree(condition, filelocStart, rootNode, thenNode, elseNode, thenNode, elseNode, true, true);
      break;

    default:
      throw new InternalError("Missing switch clause");
    }
  }




  private void buildConditionTree(Expression condition, final int filelocStart,
      CFANode rootNode, CFANode thenNode, final CFANode elseNode,
      CFANode thenNodeForLastThen, CFANode elseNodeForLastElse,
      boolean furtherThenComputation, boolean furtherElseComputation) {

    //TODO eager And Implementation

    while (condition.getNodeType() == ASTNode.PARENTHESIZED_EXPRESSION) {
      condition = ((ParenthesizedExpression) condition).getExpression();
    }


    if (condition.getNodeType() == ASTNode.INFIX_EXPRESSION
        && ((InfixExpression) condition).getOperator() == InfixExpression.Operator.CONDITIONAL_AND) {
      CFANode innerNode = new CFANode(filelocStart, cfa.getFunctionName());
      cfaNodes.add(innerNode);
      buildConditionTree(((InfixExpression) condition).getLeftOperand(), filelocStart, rootNode, innerNode, elseNode,
          thenNodeForLastThen, elseNodeForLastElse, true, false);
      buildConditionTree(((InfixExpression) condition).getRightOperand(), filelocStart, innerNode, thenNode, elseNode,
          thenNodeForLastThen, elseNodeForLastElse, true, true);

    } else if (condition.getNodeType() == ASTNode.INFIX_EXPRESSION
      &&  ((InfixExpression) condition).getOperator() == InfixExpression.Operator.CONDITIONAL_OR) {
      CFANode innerNode = new CFANode(filelocStart, cfa.getFunctionName());
      cfaNodes.add(innerNode);
      buildConditionTree(((InfixExpression) condition).getLeftOperand(), filelocStart, rootNode, thenNode, innerNode,
          thenNodeForLastThen, elseNodeForLastElse, false, true);
      buildConditionTree(((InfixExpression) condition).getRightOperand(), filelocStart, innerNode, thenNode, elseNode,
          thenNodeForLastThen, elseNodeForLastElse, true, true);

    } else {

      final org.sosy_lab.cpachecker.cfa.ast.IASTExpression exp = astCreator.convertBooleanExpression(condition);

      //TODO Solution not allowed, find better Solution
      String rawSignature = condition.toString();

      CFANode nextNode =
          handleSideassignments(rootNode, rawSignature, exp.getFileLocation().getStartingLineNumber());

      if (furtherThenComputation) {
        thenNodeForLastThen = thenNode;
      }
      if (furtherElseComputation) {
        elseNodeForLastElse = elseNode;
      }

      //TODO Assume Edge not neccessary for Java
      // edge connecting last condition with elseNode
      final AssumeEdge assumeEdgeFalse = new AssumeEdge("!(" + rawSignature + ")",
          filelocStart,
          nextNode,
          elseNodeForLastElse,
          exp,
          false);
      addToCFA(assumeEdgeFalse);

      // edge connecting last condition with thenNode
      final AssumeEdge assumeEdgeTrue = new AssumeEdge(rawSignature,
          filelocStart,
          nextNode,
          thenNodeForLastThen,
          exp,
          true);
      addToCFA(assumeEdgeTrue);
    }
  }





  private CONDITION getConditionKind(final Expression cond) {
    if (cond.getNodeType() == ASTNode.BOOLEAN_LITERAL) {
         if(((BooleanLiteral) cond).booleanValue()){
           return CONDITION.ALWAYS_TRUE;
         } else{
           return CONDITION.ALWAYS_FALSE;
         }
    }
    return CONDITION.NORMAL;
}





  /**
   * This method adds this edge to the leaving and entering edges
   * of its predecessor and successor respectively, but it does so only
   * if the edge does not contain dead code
   */
  private void addToCFA(CFAEdge edge) {
    CFACreationUtils.addEdgeToCFA(edge, logger);
  }
}
