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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.IASTAssignment;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFileLocation;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.ast.IASTVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.objectmodel.BlankEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionExitNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFAUtils;

import com.google.common.collect.ImmutableList;

/**
 * Builder to traverse AST.
 *
 *
 *
 */
class CFAFunctionBuilder extends ASTVisitor {

  private static final boolean VISIT_CHILDS = true;

  private static final boolean SKIP_CHILDS = false;

  // The first Element of a List of arguments of a
  // Assert Method Invocation is the Assert Condition
  private static final int ASSERT_CONDITION = 0;

  // Data structure for maintaining our scope stack in a function
  private final Deque<CFANode> locStack = new ArrayDeque<CFANode>();

  // Data structures for handling loops & else conditions
  private final Deque<CFANode> loopStartStack = new ArrayDeque<CFANode>();
  private final Deque<CFANode> loopNextStack  = new ArrayDeque<CFANode>(); // For the node following the current if / while block
  private final Deque<CFANode> elseStack      = new ArrayDeque<CFANode>();

  // Data structure for handling switch-statements
  private final Deque<org.sosy_lab.cpachecker.cfa.ast.IASTExpression> switchExprStack =
    new ArrayDeque<org.sosy_lab.cpachecker.cfa.ast.IASTExpression>();
  private final Deque<CFANode> switchCaseStack = new ArrayDeque<CFANode>();

  // Data structures for label , continue , break
  private final Map<String, CFALabelNode> labelMap = new HashMap<String, CFALabelNode>();
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



  //Method to handle visiting a parsing problem.  Hopefully none exist
  /* (non-Javadoc)
   * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#visit(org.eclipse.cdt.core.dom.ast.IASTProblem)
   */
  @Override
  public void preVisit(ASTNode problem) {

    if(ASTNode.RECOVERED == problem.getFlags() || ASTNode.MALFORMED == problem.getFlags() )
    throw new CFAGenerationRuntimeException("Parse Error", problem);
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
    return SKIP_CHILDS;

  }

  @Override
  public boolean visit(final VariableDeclarationFragment sd) {

    assert (locStack.size() > 0) : "not in a function's scope";

    CFANode prevNode = locStack.pop();

    CFANode nextNode = addDeclarationsToCFA(sd, prevNode);

    assert nextNode != null;
    locStack.push(nextNode);

    return SKIP_CHILDS;
  }

  @Override
  public boolean visit(final SingleVariableDeclaration sd) {

    assert (locStack.size() > 0) : "not in a function's scope";

    CFANode prevNode = locStack.pop();

    CFANode nextNode = addDeclarationsToCFA(sd, prevNode);

    assert nextNode != null;
    locStack.push(nextNode);

    return SKIP_CHILDS;
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



  @Override
  public boolean  visit(Block bl) {
    // TODO This works if else is a Block (else {Statement})
    // , but not if it has just one statement (else Statement)
    // In that case, every Statement needs to be visited
    // and handleElsoCondition be implemented.
    handleElseCondition(bl);
    return VISIT_CHILDS;
  }


  private boolean  handleAsserts(MethodInvocation assertStatement) {



    IASTFileLocation fileloc = astCreator.getFileLocation(assertStatement);


     CFANode prevNode = locStack.pop();

     //Create CFA Node for end of assert Location and push to local Stack
     CFANode postAssertNode = new CFANode(fileloc.getEndingLineNumber(),
         cfa.getFunctionName());
     cfaNodes.add(postAssertNode);
     locStack.push(postAssertNode);

     // Node for successful assert
     CFANode successfulNode = new CFANode(fileloc.getStartingLineNumber(),
         cfa.getFunctionName());
     cfaNodes.add(successfulNode);

     // Error Label Node and unsuccessfulNode for unSuccessful assert,
     CFANode unsuccessfulNode = new CFANode(fileloc.getStartingLineNumber(),
         cfa.getFunctionName());
     cfaNodes.add(unsuccessfulNode);
     CFALabelNode  errorLabelNode = new CFALabelNode(fileloc.getStartingLineNumber(),cfa.getFunctionName(), "ERROR");
       cfaNodes.add(errorLabelNode);


     createConditionEdges((Expression)assertStatement.arguments().get(ASSERT_CONDITION),
         fileloc.getStartingLineNumber(), prevNode, successfulNode, unsuccessfulNode);


       //Blank Edge from successful assert to  postAssert location

       BlankEdge blankEdge = new BlankEdge("", postAssertNode.getLineNumber(),
                                                 successfulNode, postAssertNode, "");
       addToCFA(blankEdge);


      // Blank Edge from unsuccessful assert to Error  location

       blankEdge = new BlankEdge("", errorLabelNode.getLineNumber(),
           unsuccessfulNode, errorLabelNode, "");
       addToCFA(blankEdge);


    return SKIP_CHILDS;
  }

 /**
  * This Method checks, if Statement is start of a else Condition Block
  * or Statement, changes the cfa accordingly.
  *
  *
  * @param statement Given statement to be checked.
  */
 private void handleElseCondition(Statement statement){


    ASTNode node = statement;


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

    // The parser seems to be unable to parse Assertion Statements
    // correctly, it is represented as Method_Invocation
    if (expressionStatement.getExpression().getNodeType() == ASTNode.METHOD_INVOCATION
        && ((MethodInvocation)expressionStatement.getExpression()).getName().getIdentifier().equals("assert") ) {
      handleAsserts((MethodInvocation)expressionStatement.getExpression());
     return SKIP_CHILDS;
   }

   // When else is not in blocks (else Statement)
   handleElseCondition(expressionStatement);

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



   return SKIP_CHILDS;
 }


 @Override
  public boolean visit(IfStatement ifStatement) {
   IASTFileLocation fileloc = astCreator.getFileLocation(ifStatement);

   handleElseCondition(ifStatement);


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

    return VISIT_CHILDS;
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



  @Override
  public boolean visit(LabeledStatement labelStatement) {

    IASTFileLocation fileloc = astCreator.getFileLocation(labelStatement);

    String labelName = labelStatement.getLabel().getIdentifier();
    if (labelMap.containsKey(labelName)) {
      throw new CFAGenerationRuntimeException("Duplicate label " + labelName
          + " in function " + cfa.getFunctionName(), labelStatement);
    }


    // Label Node is in Java after Label Body
    CFALabelNode labelNode = new CFALabelNode(fileloc.getStartingLineNumber(),
        cfa.getFunctionName(), labelName);
    cfaNodes.add(labelNode);
    labelMap.put(labelName, labelNode);



    //  Skip to Body
    labelStatement.getBody().accept(this);

    return SKIP_CHILDS;
  }

  @Override
  public void endVisit(LabeledStatement labelStatement) {

    String labelName = labelStatement.getLabel().getIdentifier();

    assert labelMap.containsKey(labelName) : "Label Name " + labelName + " to be deleted "
    + "out of scope, but scope does not contain it";

    // Add Edge from end of Label Body to Label
    CFALabelNode labelNode = labelMap.get(labelStatement.getLabel().getIdentifier());
    CFANode prevNode = locStack.pop();

    if (isReachableNode(prevNode)) {
      BlankEdge blankEdge = new BlankEdge(labelStatement.toString(),
          labelNode.getLineNumber(), prevNode, labelNode, "Label: " + labelName);
      addToCFA(blankEdge);
    }


    locStack.push(labelNode);

    labelMap.remove(labelStatement.getLabel().getIdentifier());
  }


  @SuppressWarnings("unchecked")
  @Override
  public boolean visit(final SwitchStatement statement) {

    IASTFileLocation fileloc = astCreator.getFileLocation(statement);

    final CFANode prevNode = locStack.pop();

    // firstSwitchNode is first Node of switch-Statement.
    // TODO useful or unnecessary? it can be replaced through prevNode.
    final CFANode firstSwitchNode =
        new CFANode(fileloc.getStartingLineNumber(),
            cfa.getFunctionName());
    cfaNodes.add(firstSwitchNode);

    org.sosy_lab.cpachecker.cfa.ast.IASTExpression switchExpression = astCreator
        .convertExpressionWithoutSideEffects(statement.getExpression());

    // TODO Solution not allowed (toString() ASTNode)
    String rawSignature = "switch (" + statement.getExpression().toString() + ")";
    String description = "switch (" + switchExpression.toASTString() + ")";
    addToCFA(new BlankEdge(rawSignature, fileloc.getStartingLineNumber(),
        prevNode, firstSwitchNode, description));

    switchExprStack.push(switchExpression);
    switchCaseStack.push(firstSwitchNode);

    // postSwitchNode is Node after the switch-statement
    final CFANode postSwitchNode =
        new CFALabelNode(fileloc.getEndingLineNumber(),
            cfa.getFunctionName(), "");
    cfaNodes.add(postSwitchNode);
    loopNextStack.push(postSwitchNode);
    locStack.push(postSwitchNode);

    locStack.push(new CFANode(fileloc.getStartingLineNumber(),
        cfa.getFunctionName()));

    // visit body,
    for( Statement st :(List<Statement>) statement.statements()){
         st.accept(this);
    }



    // leave switch
    final CFANode lastNodeInSwitch = locStack.pop();
    final CFANode lastNotCaseNode = switchCaseStack.pop();
    switchExprStack.pop(); // switchExpr is not needed after this point

    assert postSwitchNode == loopNextStack.pop();
    assert postSwitchNode == locStack.peek();
    assert switchExprStack.size() == switchCaseStack.size();

    final BlankEdge blankEdge = new BlankEdge("", lastNotCaseNode.getLineNumber(),
        lastNotCaseNode, postSwitchNode, "");
    addToCFA(blankEdge);

    final BlankEdge blankEdge2 = new BlankEdge("", lastNodeInSwitch.getLineNumber(),
        lastNodeInSwitch, postSwitchNode, "");
    addToCFA(blankEdge2);

    // skip visiting children of switch, because switchBody was handled before
    return SKIP_CHILDS;
  }



  @Override
  public boolean visit(SwitchCase switchCase) {
    if (switchCase.isDefault()) {
      handleDefault(astCreator.getFileLocation(switchCase));
    } else {
      handleCase(switchCase, astCreator.getFileLocation(switchCase));
    }
    return VISIT_CHILDS;
  }


  private void handleCase(final SwitchCase statement ,IASTFileLocation fileloc) {

    final int filelocStart = fileloc.getStartingLineNumber();

    // build condition, left part, "a"
    final org.sosy_lab.cpachecker.cfa.ast.IASTExpression switchExpr =
        switchExprStack.peek();

    // build condition, right part, "2"
    final org.sosy_lab.cpachecker.cfa.ast.IASTExpression caseExpr =
        astCreator.convertExpressionWithoutSideEffects(statement
            .getExpression());

    // build condition, "a==2", TODO correct type?
    final org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression binExp =
        new org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression(fileloc,
            switchExpr.getExpressionType(), switchExpr, caseExpr,
            org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression.BinaryOperator.EQUALS);

    // build condition edges, to caseNode with "a==2", to notCaseNode with "!(a==2)"
    final CFANode rootNode = switchCaseStack.pop();
    final CFANode caseNode = new CFANode(filelocStart,
        cfa.getFunctionName());
    final CFANode notCaseNode = new CFANode(filelocStart,
        cfa.getFunctionName());
    cfaNodes.add(caseNode);
    cfaNodes.add(notCaseNode);

    // fall-through (case before has no "break")
    final CFANode oldNode = locStack.pop();
    if (oldNode.getNumEnteringEdges() > 0) {
      final BlankEdge blankEdge =
          new BlankEdge("", filelocStart, oldNode, caseNode, "fall through");
      addToCFA(blankEdge);
    }


    switchCaseStack.push(notCaseNode);
    locStack.push(caseNode);

    // edge connecting rootNode with notCaseNode, "!(a==2)"
    final AssumeEdge assumeEdgeFalse = new AssumeEdge("!(" + binExp.toASTString() + ")",
        filelocStart, rootNode, notCaseNode, binExp, false);
    addToCFA(assumeEdgeFalse);

    // edge connecting rootNode with caseNode, "a==2"
    final AssumeEdge assumeEdgeTrue = new AssumeEdge(binExp.toASTString(),
        filelocStart, rootNode, caseNode, binExp, true);
    addToCFA(assumeEdgeTrue);


  }


  private void handleDefault(IASTFileLocation fileloc) {

    final int filelocStart = fileloc.getStartingLineNumber();

    // build blank edge to caseNode with "default", no edge to notCaseNode
    final CFANode rootNode = switchCaseStack.pop();
    final CFANode caseNode = new CFANode(filelocStart, cfa.getFunctionName());
    final CFANode notCaseNode = new CFANode(filelocStart, cfa.getFunctionName());
    cfaNodes.add(caseNode);
    cfaNodes.add(notCaseNode);

    // fall-through (case before has no "break")
    final CFANode oldNode = locStack.pop();
    if (oldNode.getNumEnteringEdges() > 0) {
      final BlankEdge blankEdge =
          new BlankEdge("", filelocStart, oldNode, caseNode, "fall through");
      addToCFA(blankEdge);
    }

    switchCaseStack.push(notCaseNode); // for later cases, only reachable through jumps
    locStack.push(caseNode);

    // blank edge connecting rootNode with caseNode
    final BlankEdge trueEdge =
        new BlankEdge("default :", filelocStart, rootNode, caseNode, "default");
    addToCFA(trueEdge);
  }


  @Override
  public boolean visit (WhileStatement whileStatement) {

    IASTFileLocation fileloc = astCreator.getFileLocation(whileStatement);

    final CFANode prevNode = locStack.pop();

    final CFANode loopStart = new CFANode(fileloc.getStartingLineNumber(),
        cfa.getFunctionName());
    cfaNodes.add(loopStart);
    loopStart.setLoopStart();
    loopStartStack.push(loopStart);

    final CFANode firstLoopNode = new CFANode(fileloc.getStartingLineNumber(),
        cfa.getFunctionName());
    cfaNodes.add(firstLoopNode);

    final CFANode postLoopNode = new CFALabelNode(fileloc.getEndingLineNumber(),
        cfa.getFunctionName(), "");
    cfaNodes.add(postLoopNode);
    loopNextStack.push(postLoopNode);

    // inverse order here!
    locStack.push(postLoopNode);
    locStack.push(firstLoopNode);

    final BlankEdge blankEdge = new BlankEdge("", fileloc.getStartingLineNumber(),
        prevNode, loopStart, "while");
    addToCFA(blankEdge);

    createConditionEdges(whileStatement.getExpression(), fileloc.getStartingLineNumber(),
        loopStart, firstLoopNode, postLoopNode);

    //Visit Body, Expression already handled.
    whileStatement.getBody().accept(this);

    return SKIP_CHILDS;
  }

  @Override
  public boolean visit(DoStatement doStatement) {

    IASTFileLocation fileloc = astCreator.getFileLocation(doStatement);

    final CFANode prevNode = locStack.pop();

    final CFANode loopStart = new CFANode(fileloc.getStartingLineNumber(),
        cfa.getFunctionName());
    cfaNodes.add(loopStart);
    loopStart.setLoopStart();
    loopStartStack.push(loopStart);

    final CFANode firstLoopNode = new CFANode(fileloc.getStartingLineNumber(),
        cfa.getFunctionName());
    cfaNodes.add(firstLoopNode);

    final CFANode postLoopNode = new CFALabelNode(fileloc.getEndingLineNumber(),
        cfa.getFunctionName(), "");
    cfaNodes.add(postLoopNode);
    loopNextStack.push(postLoopNode);

    // inverse order here!
    locStack.push(postLoopNode);
    locStack.push(firstLoopNode);

    final BlankEdge blankEdge = new BlankEdge("", fileloc.getStartingLineNumber(),
        prevNode, firstLoopNode, "do");
    addToCFA(blankEdge);
    createConditionEdges(doStatement.getExpression(), fileloc.getStartingLineNumber(),
        loopStart, firstLoopNode, postLoopNode);

    // Visit Body not Children
    doStatement.getBody().accept(this);

    return SKIP_CHILDS;
  }


  @Override
  public void endVisit(WhileStatement whileStatement){
    handleLeaveWhileLoop();
  }

  @Override
  public void endVisit(DoStatement doStatement){
    handleLeaveWhileLoop();
  }

  private void handleLeaveWhileLoop(){
    CFANode prevNode = locStack.pop();
    CFANode startNode = loopStartStack.pop();

    if (isReachableNode(prevNode)) {
      BlankEdge blankEdge = new BlankEdge("", prevNode.getLineNumber(),
          prevNode, startNode, "");
      addToCFA(blankEdge);
    }
    CFANode nextNode = loopNextStack.pop();
    assert nextNode == locStack.peek();
  }


  @SuppressWarnings({ "cast", "unchecked" })
  @Override
  public boolean visit(final ForStatement forStatement) {

    final IASTFileLocation fileloc = astCreator.getFileLocation(forStatement);
    final int filelocStart = fileloc.getStartingLineNumber();
    final CFANode prevNode = locStack.pop();

    // loopInit is Node before "counter = 0;"
    final CFANode loopInit = new CFANode(filelocStart, cfa.getFunctionName());
    cfaNodes.add(loopInit);
    addToCFA(new BlankEdge("", filelocStart, prevNode, loopInit, "for"));

    // loopStartNodes is the Node before the loop itself,
    // it is the the one after the init edge(s)

    final CFANode loopStart = createInitEdgeForForLoop(((List<Expression>)forStatement.initializers()),
        filelocStart, loopInit);
    loopStart.setLoopStart();

    // loopEnd is Node before "counter++;"
    final CFANode loopEnd = new CFALabelNode(filelocStart, cfa.getFunctionName(), "");
    cfaNodes.add(loopEnd);
    loopStartStack.push(loopEnd);

    // this edge connects loopEnd with loopStart and contains the statement "counter++;"

    createLastNodesForForLoop(((List<Expression>)forStatement.updaters()),
        filelocStart, loopEnd, loopStart);

    // firstLoopNode is Node after "counter < 5"
    final CFANode firstLoopNode = new CFANode(
        filelocStart, cfa.getFunctionName());
    cfaNodes.add(firstLoopNode);

    // firstLoopNode is Node after "!(counter < 5)"
    final CFANode postLoopNode = new CFALabelNode(
        fileloc.getEndingLineNumber(), cfa.getFunctionName(), "");
    cfaNodes.add(postLoopNode);
    loopNextStack.push(postLoopNode);

    // inverse order here!
    locStack.push(postLoopNode);
    locStack.push(firstLoopNode);

    createConditionEdgesForForLoop(forStatement.getExpression(),
        filelocStart, loopStart, postLoopNode, firstLoopNode);

    // visit only loopbody, not children
    forStatement.getBody().accept(this);

    // leave loop
    final CFANode lastNodeInLoop = locStack.pop();

    // loopEnd is the Node before "counter++;"
    assert loopEnd == loopStartStack.pop();
    assert postLoopNode == loopNextStack.pop();
    assert postLoopNode == locStack.peek();

    if (isReachableNode(lastNodeInLoop)) {
      final BlankEdge blankEdge = new BlankEdge("", lastNodeInLoop.getLineNumber(),
          lastNodeInLoop, loopEnd, "");
      addToCFA(blankEdge);
    }

    // skip visiting children of loop, because loopbody was handled before
    return SKIP_CHILDS;
  }


  private void createConditionEdgesForForLoop(Expression condition, int filelocStart, CFANode loopStart,
      CFANode postLoopNode, CFANode firstLoopNode) {

    if (condition == null) {
      // no condition -> only a blankEdge from loopStart to firstLoopNode
      final BlankEdge blankEdge = new BlankEdge("", filelocStart, loopStart,
          firstLoopNode, "");
      addToCFA(blankEdge);

    } else {
      createConditionEdges(condition, filelocStart, loopStart, firstLoopNode,
          postLoopNode);
    }
  }

  private void createLastNodesForForLoop(List<Expression> updaters, int filelocStart, CFANode loopEnd, CFANode loopStart) {

    int size = updaters.size();

    if (size == 0) {
      // ignore, only add blankEdge
      final BlankEdge blankEdge = new BlankEdge("", filelocStart, loopEnd, loopStart, "");
      addToCFA(blankEdge);


    } else {

      CFANode nextNode;

      // counter indicating current element
      int c = 0;

      for (Expression exp : updaters) {
        //TODO Investigate if we can use Expression without Side Effect here
        final IASTNode node = astCreator.convertExpressionWithSideEffects(exp);

        // If last Expression, use last loop Node
        if(size - 1 != c){
          nextNode = new CFANode(filelocStart, cfa.getFunctionName());
          c++;
          cfaNodes.add(nextNode);
        }else {
          nextNode = loopEnd;
        }

        if (node instanceof IASTIdExpression) {
          final BlankEdge blankEdge = new BlankEdge(node.toASTString(),
              filelocStart, nextNode, loopStart, "");
          addToCFA(blankEdge);

          // "counter++;"
          //TODO Find better Solution (to String not allowed)
        } else if (node instanceof IASTExpressionAssignmentStatement) {
          final StatementEdge lastEdge = new StatementEdge(exp.toString(),
              (IASTExpressionAssignmentStatement) node, filelocStart, nextNode, loopStart);
          addToCFA(lastEdge);

          //TODO Find  better Solution (to String not allowed)
        } else if (node instanceof IASTFunctionCallAssignmentStatement) {
          final StatementEdge edge = new StatementEdge(exp.toString(),
              (IASTFunctionCallAssignmentStatement) node, filelocStart, nextNode, loopStart);
          addToCFA(edge);

        } else { // TODO: are there other iteration-expressions in a for-loop?
          throw new AssertionError("CFABuilder: unknown iteration-expressions in for-statement:\n"
              + exp.getClass());
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private CFANode createInitEdgeForForLoop(List<Expression> initializers, int filelocStart, CFANode loopInit) {

      CFANode nextNode = loopInit;

      // counter indicating current element


      for (Expression exp : initializers) {
        //TODO Investigate if we can use Expression without Side Effect here
        final IASTNode node = astCreator.convertExpressionWithSideEffects(exp);

        // If last Expression, use last loop Node

          nextNode = new CFANode(filelocStart, cfa.getFunctionName());
          cfaNodes.add(nextNode);


        if (node instanceof IASTIdExpression) {
          final BlankEdge blankEdge = new BlankEdge(node.toASTString(),
              filelocStart, nextNode, loopInit, "");
          addToCFA(blankEdge);

          // "counter++;"
          //TODO Find better Solution (to String not allowed)
        } else if (node instanceof IASTExpressionAssignmentStatement) {
          final StatementEdge lastEdge = new StatementEdge(exp.toString(),
              (IASTExpressionAssignmentStatement) node, filelocStart, nextNode, loopInit);
          addToCFA(lastEdge);

          //TODO Find  better Solution (to String not allowed)
        } else if (node instanceof IASTFunctionCallAssignmentStatement) {
          final StatementEdge edge = new StatementEdge(exp.toString(),
              (IASTFunctionCallAssignmentStatement) node, filelocStart, nextNode, loopInit);
          addToCFA(edge);

        } else { // TODO: are there other iteration-expressions in a for-loop?
          throw new AssertionError("CFABuilder: unknown iteration-expressions in for-statement:\n"
              + exp.getClass());
        }
      }

      assert nextNode != null;

      return nextNode;


      /*
    if (initializers.size() == 0) {
      // no edge inserted
      return loopInit;
    } else {

      if(initializers.get(0).getNodeType() == ASTNode.VARIABLE_DECLARATION_EXPRESSION){
        VariableDeclarationExpression variableDeclaraion =
            (VariableDeclarationExpression) initializers.get(0);

        CFANode prevNode = loopInit;

        for (VariableDeclarationFragment fr : (List<VariableDeclarationFragment>) variableDeclaraion.fragments()) {
          prevNode = addDeclarationsToCFA(fr, prevNode);
        }

        return prevNode;
      } else {

        //Initializer start with loopInit
        locStack.push( loopInit );

        // Go through initializer
        for(ASTNode ast : initializers){
          System.out.println(ast.getNodeType());
        }

        return locStack.pop();

      }
    }
    */
  }

  @Override
  public boolean visit(BreakStatement breakStatement) {

    //TODO Check Validity of Break Statement

    if(breakStatement.getLabel() == null ){
      handleBreakStatement(breakStatement);
    }else{
      handleLabeledBreakStatement(breakStatement);
    }

    return SKIP_CHILDS;
  }


  private void handleBreakStatement (BreakStatement breakStatement) {


    IASTFileLocation fileloc = astCreator.getFileLocation(breakStatement);
    CFANode prevNode = locStack.pop();
    CFANode postLoopNode = loopNextStack.peek();

    //TODO SOlution not allowed (toString()) find better Solution
    BlankEdge blankEdge = new BlankEdge(breakStatement.toString(),
        fileloc.getStartingLineNumber(), prevNode, postLoopNode, "break");
    addToCFA(blankEdge);

    CFANode nextNode = new CFANode(fileloc.getEndingLineNumber(),
        cfa.getFunctionName());
    cfaNodes.add(nextNode);
    locStack.push(nextNode);

  }


  private void handleLabeledBreakStatement (BreakStatement breakStatement) {

    IASTFileLocation fileloc = astCreator.getFileLocation(breakStatement);
    CFANode prevNode = locStack.pop();
    CFANode postLoopNode = labelMap.get(breakStatement.getLabel().getIdentifier());

    //TODO SOlution not allowed (toString()) find better Solution
    BlankEdge blankEdge = new BlankEdge(breakStatement.toString(),
        fileloc.getStartingLineNumber(), prevNode, postLoopNode, "break ");
    addToCFA(blankEdge);

    CFANode nextNode = new CFANode(fileloc.getEndingLineNumber(),
        cfa.getFunctionName());
    cfaNodes.add(nextNode);
    locStack.push(nextNode);
  }


  @Override
  public boolean visit(ContinueStatement continueStatement) {

    //TODO Check Validity of Continue Statement

    if(continueStatement.getLabel() == null ){
      handleContinueStatement(continueStatement);
    }else{
      //TODO Implement Labeled Continue
     throw new CFAGenerationRuntimeException("Labeled Continue not yet implemented");
    }

    return SKIP_CHILDS;
  }

  private void handleContinueStatement(ContinueStatement continueStatement) {

    IASTFileLocation fileloc = astCreator.getFileLocation(continueStatement);

    CFANode prevNode = locStack.pop();
    CFANode loopStartNode = loopStartStack.peek();

    //TODO toString() not allowed
    BlankEdge blankEdge = new BlankEdge(continueStatement.toString(),
        fileloc.getStartingLineNumber(), prevNode, loopStartNode, "continue");
    addToCFA(blankEdge);

    CFANode nextNode = new CFANode(fileloc.getEndingLineNumber(),
        cfa.getFunctionName());
    locStack.push(nextNode);
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
