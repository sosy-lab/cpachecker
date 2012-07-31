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
import org.eclipse.jdt.core.dom.AssertStatement;
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
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.CFileLocation;
import org.sosy_lab.cpachecker.cfa.ast.IADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IAExpression;
import org.sosy_lab.cpachecker.cfa.ast.IAStatement;
import org.sosy_lab.cpachecker.cfa.ast.IAstNode;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CLabelNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.types.AFunctionType;
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

  // Data structure for maintaining our scope stack in a function
  private final Deque<CFANode> locStack = new ArrayDeque<CFANode>();

  // Data structures for handling loops & else conditions
  private final Deque<CFANode> loopStartStack = new ArrayDeque<CFANode>();
  private final Deque<CFANode> loopNextStack  = new ArrayDeque<CFANode>(); // For the node following the current if / while block
  private final Deque<CFANode> elseStack      = new ArrayDeque<CFANode>();

  // Data structure for handling switch-statements
  private final Deque<IAExpression> switchExprStack =
    new ArrayDeque<IAExpression>();
  private final Deque<CFANode> switchCaseStack = new ArrayDeque<CFANode>();

  // Data structures for label , continue , break
  private final Map<String, CLabelNode> labelMap = new HashMap<String, CLabelNode>();
  //private final Multimap<String, CFANode> gotoLabelNeeded = ArrayListMultimap.create();

  // Data structures for handling function declarations
  private FunctionEntryNode cfa = null;
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

  FunctionEntryNode getStartNode() {
    checkState(cfa != null);
    return cfa;
  }

  Set<CFANode> getCfaNodes() {
    checkState(cfa != null);
    return cfaNodes;
  }



  //Method to handle visiting a parsing problem.  Hopefully none exist
  /* (non-Javadoc)
   * @see org.eclipse.cdt.core.dom.ast.c.ASTVisitor#visit(org.eclipse.cdt.core.dom.ast.c.CProblem)
   */
  @Override
  public void preVisit(ASTNode problem) {

    if(ASTNode.RECOVERED == problem.getFlags() || ASTNode.MALFORMED == problem.getFlags() )
    throw new CFAGenerationRuntimeException("Parse Error", problem);
  }

  /* (non-Javadoc)
   * @see org.eclipse.cdt.core.dom.ast.c.ASTVisitor#visit(org.eclipse.cdt.core.dom.ast.c.IADeclaration)
   */
  @Override
  public boolean visit(MethodDeclaration declaration) {



    if (locStack.size() != 0) {
      throw new CFAGenerationRuntimeException("Nested function declarations?");
    }

    assert cfa == null;

    final AFunctionDeclaration fdef = astCreator.convert(declaration);
    final String nameOfFunction = fdef.getName();
    assert !nameOfFunction.isEmpty();

    scope.enterFunction(fdef);

    final List<AParameterDeclaration> parameters =   ((AFunctionType) fdef.getType()).getParameters();
    final List<String> parameterNames = new ArrayList<String>(parameters.size());

    for (AParameterDeclaration param : parameters) {
      scope.registerDeclaration(param); // declare parameter as local variable
      parameterNames.add(param.getName());
    }

    final FunctionExitNode returnNode = new FunctionExitNode(fdef.getFileLocation().getEndingLineNumber(), nameOfFunction);
    cfaNodes.add(returnNode);

    final FunctionEntryNode startNode = new FunctionEntryNode(
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
  public boolean visit(final VariableDeclarationStatement sd) {

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

      IAstNode sideeffect = astCreator.getNextSideAssignment();

      createSideAssignmentEdges(prevNode, nextNode, rawSignature, filelocStart, sideeffect);
      prevNode = nextNode;
    }
    return prevNode;
  }


  /**
   * Submethod from handleSideassignments, takes an IAstNode and depending on its
   * type creates an edge.
   */
  private void createSideAssignmentEdges(CFANode prevNode, CFANode nextNode, String rawSignature,
      int filelocStart, IAstNode sideeffect) {
    CFAEdge previous;
    if(sideeffect instanceof org.sosy_lab.cpachecker.cfa.ast.c.CStatement) {
      previous = new AStatementEdge(rawSignature, (org.sosy_lab.cpachecker.cfa.ast.c.CStatement)sideeffect, filelocStart, prevNode, nextNode);
    } else if (sideeffect instanceof AAssignment) {
      previous = new AStatementEdge(rawSignature, (org.sosy_lab.cpachecker.cfa.ast.c.CStatement)sideeffect, filelocStart, prevNode, nextNode);
    } else if (sideeffect instanceof AIdExpression) {
      previous = new AStatementEdge(rawSignature, new AExpressionStatement(sideeffect.getFileLocation(), (IAExpression) sideeffect), filelocStart, prevNode, nextNode);
    } else {
      previous = new ADeclarationEdge(rawSignature, filelocStart, prevNode, nextNode, (IADeclaration) sideeffect);
    }
    addToCFA(previous);
  }





  /**
   * This method takes a list of Declarations and adds them to the CFA.
   * The edges are inserted after startNode.
   * @return the node after the last of the new declarations
   */


  private CFANode addDeclarationsToCFA(final List<IADeclaration> declList, String rawSignature , CFANode prevNode) {



    prevNode = handleSideassignments(prevNode, rawSignature, declList.get(0).getFileLocation().getStartingLineNumber());

    // create one edge for every declaration
    for (IADeclaration newD : declList) {

      if (newD instanceof AVariableDeclaration) {
        scope.registerDeclaration(newD);
      } else if (newD instanceof AFunctionDeclaration) {
        scope.registerFunctionDeclaration((AFunctionDeclaration) newD);
      }

      CFANode nextNode = new CFANode(declList.get(0).getFileLocation().getStartingLineNumber(), cfa.getFunctionName());
      cfaNodes.add(nextNode);

      final ADeclarationEdge edge = new ADeclarationEdge(rawSignature, declList.get(0).getFileLocation().getStartingLineNumber(),
          prevNode, nextNode, newD);
      addToCFA(edge);

      prevNode = nextNode;
    }

    return prevNode;
  }

  private CFANode addDeclarationsToCFA(final VariableDeclarationStatement sd, CFANode prevNode) {

    // If statement is an else condition Statement
    // if(condition){..} else int dec;
    handleElseCondition(sd);

    final List<IADeclaration> declList =
        astCreator.convert(sd);
    //TODO  Search for better Solution.
    final String rawSignature = sd.toString();
    return addDeclarationsToCFA(declList , rawSignature, prevNode);
  }

  private CFANode addDeclarationsToCFA(final SingleVariableDeclaration sd, CFANode prevNode) {

    final List<IADeclaration> declList =new ArrayList<IADeclaration>(1) ;

    declList.add(astCreator.convert(sd));

    //TODO  Search for better Solution.
    final String rawSignature = sd.toString();

    return addDeclarationsToCFA(declList , rawSignature, prevNode);
  }


  @Override
  public boolean  visit(Block bl) {
    // TODO This works if else is a Block (else {Statement})
    // , but not if it has just one statement (else Statement)
    // In that case, every Statement needs to be visited
    // and handleElsoCondition be implemented.
    handleElseCondition(bl);
    scope.enterBlock();

    return VISIT_CHILDS;
  }

  @Override
  public void  endVisit(Block bl) {
    // TODO This works if else is a Block (else {Statement})
    // , but not if it has just one statement (else Statement)
    // In that case, every Statement needs to be visited
    // and handleElsoCondition be implemented.
    scope.leaveBlock();


  }


  @Override
  public boolean  visit(AssertStatement assertStatement) {


     final String message;

     if(assertStatement.getMessage() == null){
       message = "";
     } else {
       //TODO Extract Message from String
       message = assertStatement.getMessage().toString();
     }


     CFileLocation fileloc = astCreator.getFileLocation(assertStatement);
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
     CLabelNode  errorLabelNode = new CLabelNode(fileloc.getStartingLineNumber(),cfa.getFunctionName(), "ERROR");
       cfaNodes.add(errorLabelNode);

     createConditionEdges( assertStatement.getExpression() ,
         fileloc.getStartingLineNumber(), prevNode, successfulNode, unsuccessfulNode);

       //Blank Edge from successful assert to  postAssert location
       BlankEdge blankEdge = new BlankEdge(assertStatement.toString(), postAssertNode.getLineNumber(), successfulNode, postAssertNode, "assert success");
       addToCFA(blankEdge);

      // Blank Edge from unsuccessful assert to Error  location
       blankEdge = new BlankEdge(assertStatement.toString(), errorLabelNode.getLineNumber(),
           unsuccessfulNode, errorLabelNode, "asssert fail:" + message);
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

   // When else is not in blocks (else Statement)
   handleElseCondition(expressionStatement);

   CFANode prevNode = locStack.pop ();

   IAStatement statement = astCreator.convert(expressionStatement);

   //TODO Solution not allowed, find better Solution
   String rawSignature = expressionStatement.toString();

   CFANode lastNode = new CFANode(statement.getFileLocation().getStartingLineNumber(), cfa.getFunctionName());
   cfaNodes.add(lastNode);

   if(astCreator.getConditionalExpression() != null) {
     //TODO Implement ConditionalStatement

     //CConditionalExpression condExp = astCreator.getConditionalExpression();
     //astCreator.resetConditionalExpression();
     //handleConditionalStatement(condExp, prevNode, lastNode, statement);
   } else {
     CFANode nextNode = handleSideassignments(prevNode, rawSignature, statement.getFileLocation().getStartingLineNumber());

     AStatementEdge edge = new AStatementEdge(rawSignature, statement,
         statement.getFileLocation().getStartingLineNumber(), nextNode, lastNode);
     addToCFA(edge);
   }
   locStack.push(lastNode);



   return SKIP_CHILDS;
 }


 @Override
  public boolean visit(IfStatement ifStatement) {
   CFileLocation fileloc = astCreator.getFileLocation(ifStatement);

   // If parent Else is not a Block
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





  private static enum CONDITION { NORMAL, ALWAYS_FALSE, ALWAYS_TRUE }

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

      final IAExpression exp = astCreator.convertBooleanExpression(condition);

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


      // edge connecting last condition with elseNode
      final AssumeEdge JAssumeEdgeFalse = new AssumeEdge("!(" + rawSignature + ")",
          filelocStart,
          nextNode,
          elseNodeForLastElse,
          exp,
          false);
      addToCFA(JAssumeEdgeFalse);

      // edge connecting last condition with thenNode
      final AssumeEdge JAssumeEdgeTrue = new AssumeEdge(rawSignature,
          filelocStart,
          nextNode,
          thenNodeForLastThen,
          exp,
          true);
      addToCFA(JAssumeEdgeTrue);
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

    //If parent is a else Condition without block
    handleElseCondition(labelStatement);

    CFileLocation fileloc = astCreator.getFileLocation(labelStatement);

    String labelName = labelStatement.getLabel().getIdentifier();
    if (labelMap.containsKey(labelName)) {
      throw new CFAGenerationRuntimeException("Duplicate label " + labelName
          + " in function " + cfa.getFunctionName(), labelStatement);
    }


    // Label Node is in Java after Label Body
    CLabelNode labelNode = new CLabelNode(fileloc.getStartingLineNumber(),
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
    CLabelNode labelNode = labelMap.get(labelStatement.getLabel().getIdentifier());
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

    // If parent is else and not a block
    handleElseCondition(statement);


    CFileLocation fileloc = astCreator.getFileLocation(statement);

    final CFANode prevNode = locStack.pop();

    // firstSwitchNode is first Node of switch-Statement.
    // TODO useful or unnecessary? it can be replaced through prevNode.
    final CFANode firstSwitchNode =
        new CFANode(fileloc.getStartingLineNumber(),
            cfa.getFunctionName());
    cfaNodes.add(firstSwitchNode);

    IAExpression switchExpression = astCreator
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
        new CLabelNode(fileloc.getEndingLineNumber(),
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


  private void handleCase(final SwitchCase statement ,CFileLocation fileloc) {

    final int filelocStart = fileloc.getStartingLineNumber();

    // build condition, left part, "a"
    final IAExpression switchExpr =
        switchExprStack.peek();

    // build condition, right part, "2"
    final IAExpression caseExpr =
        astCreator.convertExpressionWithoutSideEffects(statement
            .getExpression());

    // build condition, "a==2", TODO correct type?
    final ABinaryExpression binExp =
        new ABinaryExpression(fileloc,
            switchExpr.getExpressionType(), switchExpr, caseExpr,
            org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.EQUALS);

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
    final AssumeEdge JAssumeEdgeFalse = new AssumeEdge("!(" + binExp.toASTString() + ")",
        filelocStart, rootNode, notCaseNode, binExp, false);
    addToCFA(JAssumeEdgeFalse);

    // edge connecting rootNode with caseNode, "a==2"
    final AssumeEdge JAssumeEdgeTrue = new AssumeEdge(binExp.toASTString(),
        filelocStart, rootNode, caseNode, binExp, true);
    addToCFA(JAssumeEdgeTrue);


  }


  private void handleDefault(CFileLocation fileloc) {

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

    handleElseCondition(whileStatement);

    CFileLocation fileloc = astCreator.getFileLocation(whileStatement);

    final CFANode prevNode = locStack.pop();

    final CFANode loopStart = new CFANode(fileloc.getStartingLineNumber(),
        cfa.getFunctionName());
    cfaNodes.add(loopStart);
    loopStart.setLoopStart();
    loopStartStack.push(loopStart);

    final CFANode firstLoopNode = new CFANode(fileloc.getStartingLineNumber(),
        cfa.getFunctionName());
    cfaNodes.add(firstLoopNode);

    final CFANode postLoopNode = new CLabelNode(fileloc.getEndingLineNumber(),
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

    handleElseCondition(doStatement);

    CFileLocation fileloc = astCreator.getFileLocation(doStatement);

    final CFANode prevNode = locStack.pop();

    final CFANode loopStart = new CFANode(fileloc.getStartingLineNumber(),
        cfa.getFunctionName());
    cfaNodes.add(loopStart);
    loopStart.setLoopStart();
    loopStartStack.push(loopStart);

    final CFANode firstLoopNode = new CFANode(fileloc.getStartingLineNumber(),
        cfa.getFunctionName());
    cfaNodes.add(firstLoopNode);

    final CFANode postLoopNode = new CLabelNode(fileloc.getEndingLineNumber(),
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

    handleElseCondition(forStatement);

    final CFileLocation fileloc = astCreator.getFileLocation(forStatement);
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



    // firstLoopNode is Node after "counter < 5"
    final CFANode firstLoopNode = new CFANode(
        filelocStart, cfa.getFunctionName());
    cfaNodes.add(firstLoopNode);

    // firstLoopNode is Node after "!(counter < 5)"
    final CFANode postLoopNode = new CLabelNode(
        fileloc.getEndingLineNumber(), cfa.getFunctionName(), "");
    cfaNodes.add(postLoopNode);
    loopNextStack.push(postLoopNode);

    // inverse order here!
    locStack.push(postLoopNode);
    locStack.push(firstLoopNode);

    createConditionEdgesForForLoop(forStatement.getExpression(),
        filelocStart, loopStart, postLoopNode, firstLoopNode);


    // Node before Update "counter++"
    final CFANode lastNodeInLoop = new CFANode(filelocStart, cfa.getFunctionName());
    cfaNodes.add(lastNodeInLoop);

    loopStartStack.push(lastNodeInLoop);


    // visit only loop body, not children
    forStatement.getBody().accept(this);

 // leave loop
    final CFANode prev = locStack.pop();

    final BlankEdge blankEdge = new BlankEdge("",
        filelocStart, prev, lastNodeInLoop , "");
    addToCFA(blankEdge);

    // loopEnd is end of Loop after "counter++;"

    createLastNodesAndEdgeForForLoop(((List<Expression>)forStatement.updaters()),
        filelocStart, lastNodeInLoop, loopStart);

    assert lastNodeInLoop == loopStartStack.pop();
    assert postLoopNode == loopNextStack.pop();
    assert postLoopNode == locStack.peek();

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

  private void createLastNodesAndEdgeForForLoop(List<Expression> updaters, int filelocStart, CFANode loopEnd , CFANode loopStart) {

    int size = updaters.size();

    if (size == 0) {
      // no update

      final BlankEdge blankEdge = new BlankEdge("",
          filelocStart, loopEnd, loopStart , "");
      addToCFA(blankEdge);


    } else {

      CFANode prevNode = loopEnd;
      CFANode nextNode = null;


      for (Expression exp : updaters) {
        //TODO Investigate if we can use Expression without Side Effect here
        final IAstNode node = astCreator.convertExpressionWithSideEffects(exp);

        // If last Expression, use last loop Node

          nextNode = new CFANode(filelocStart, cfa.getFunctionName());
          cfaNodes.add(nextNode);


        if (node instanceof AIdExpression) {
          final BlankEdge blankEdge = new BlankEdge(node.toASTString(),
              filelocStart, prevNode, nextNode , "");
          addToCFA(blankEdge);

          // "counter++;"
          //TODO Find better Solution (to String not allowed)
        } else if (node instanceof AExpressionAssignmentStatement) {
          final AStatementEdge lastEdge = new AStatementEdge(exp.toString(),
              (AExpressionAssignmentStatement) node, filelocStart, prevNode, nextNode);
          addToCFA(lastEdge);

          //TODO Find  better Solution (to String not allowed)
        } else if (node instanceof AFunctionCallAssignmentStatement) {
          final AStatementEdge edge = new AStatementEdge(exp.toString(),
              (AFunctionCallAssignmentStatement) node, filelocStart, prevNode, nextNode);
          addToCFA(edge);

        } else { // TODO: are there other iteration-expressions in a for-loop?
          throw new AssertionError("CFABuilder: unknown iteration-expressions in for-statement:\n"
              + exp.getClass());
        }
      }

      //TODO Blank edge here right?
      final BlankEdge blankEdge = new BlankEdge("",
          filelocStart, nextNode, loopStart , "");
      addToCFA(blankEdge);

      assert(nextNode != null) : "Null Pointer not expected. Unexpected behaviour in For Statementen" ;


    }
  }

  @SuppressWarnings("unchecked")
  private CFANode createInitEdgeForForLoop(List<Expression> initializers, int filelocStart, CFANode loopInit) {

      CFANode nextNode = loopInit;

      // counter indicating current element


      for (Expression exp : initializers) {

        //TODO Investigate if we can use Expression without Side Effect here
        final IAstNode node = astCreator.convertExpressionWithSideEffects(exp);






          nextNode = new CFANode(filelocStart, cfa.getFunctionName());
          cfaNodes.add(nextNode);


        if (node instanceof AIdExpression) {
          final BlankEdge blankEdge = new BlankEdge(node.toASTString(),
              filelocStart, loopInit, nextNode , "");
          addToCFA(blankEdge);


          //TODO Find better Solution (to String not allowed)
        } else if (node instanceof AExpressionAssignmentStatement) {
          final AStatementEdge lastEdge = new AStatementEdge(exp.toString(),
              (AExpressionAssignmentStatement) node, filelocStart, loopInit, nextNode);
          addToCFA(lastEdge);

          //TODO Find  better Solution (to String not allowed)
        } else if (node instanceof AFunctionCallAssignmentStatement) {
          final AStatementEdge edge = new AStatementEdge(exp.toString(),
              (AFunctionCallAssignmentStatement) node, filelocStart,loopInit ,   nextNode);
          addToCFA(edge);

        } else { // TODO: are there other iteration-expressions in a for-loop?
          throw new AssertionError("CFABuilder: unknown iteration-expressions in for-statement:\n"
              + exp.getClass());
        }
      }

      assert nextNode != null;

      return nextNode;



  }

  @Override
  public boolean visit(BreakStatement breakStatement) {

    handleElseCondition(breakStatement);

    //TODO Check Validity of Break Statement

    if(breakStatement.getLabel() == null ){
      handleBreakStatement(breakStatement);
    }else{
      handleLabeledBreakStatement(breakStatement);
    }

    return SKIP_CHILDS;
  }


  private void handleBreakStatement (BreakStatement breakStatement) {


    CFileLocation fileloc = astCreator.getFileLocation(breakStatement);
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

    CFileLocation fileloc = astCreator.getFileLocation(breakStatement);
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

    handleElseCondition(continueStatement);

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

    CFileLocation fileloc = astCreator.getFileLocation(continueStatement);

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


  @Override
  public boolean visit(ReturnStatement returnStatement) {

    CFileLocation fileloc = astCreator.getFileLocation(returnStatement);

    CFANode prevNode = locStack.pop();
    FunctionExitNode functionExitNode = cfa.getExitNode();


    //TODO toString() not allowed
    AReturnStatementEdge edge = new AReturnStatementEdge(returnStatement.toString(),
        astCreator.convert(returnStatement), fileloc.getStartingLineNumber(), prevNode, functionExitNode);
    addToCFA(edge);

    CFANode nextNode = new CFANode(fileloc.getEndingLineNumber(),
        cfa.getFunctionName());
    cfaNodes.add(nextNode);
    locStack.push(nextNode);

    return SKIP_CHILDS;
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
