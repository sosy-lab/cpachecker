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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.java;

import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.cpachecker.cfa.CFACreationUtils.isReachableNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.IAInitializer;
import org.sosy_lab.cpachecker.cfa.ast.java.JAssignment;
import org.sosy_lab.cpachecker.cfa.ast.java.JAstNode;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBooleanLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JClassInstanceCreation;
import org.sosy_lab.cpachecker.cfa.ast.java.JDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JReferencedMethodInvocationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.java.JSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.cfa.model.java.JAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodEntryNode;
import org.sosy_lab.cpachecker.cfa.model.java.JReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * Builder to traverse AST.
 *
 */
class CFAMethodBuilder extends ASTVisitor {

  private static final boolean VISIT_CHILDS = true;

  private static final boolean SKIP_CHILDS = false;

  private static final int ONLY_EDGE = 0;

  // Data structure for maintaining our scope stack in a function
  private final Deque<CFANode> locStack = new ArrayDeque<>();

  // Data structures for handling loops & else conditions
  private final Deque<CFANode> loopStartStack = new ArrayDeque<>();
  private final Deque<CFANode> loopNextStack  = new ArrayDeque<>(); // For the node following the current if / while block
  private final Deque<CFANode> elseStack      = new ArrayDeque<>();

  // Data structure for handling switch-statements
  private final Deque<JExpression> switchExprStack = new ArrayDeque<>();
  private final Deque<CFANode> switchCaseStack = new ArrayDeque<>();

  // Data structures for label , continue , break
  private final Map<String, CLabelNode> labelMap = new HashMap<>();
  private final Map<String, List<Pair<CFANode, ContinueStatement>>> registeredContinues = new HashMap<>();

  // Data structures for handling method declarations
  private JMethodEntryNode cfa = null;
  private final Set<CFANode> cfaNodes = new HashSet<>();


  private final Scope scope;
  private final ASTConverter astCreator;

  private final LogManager logger;

  public CFAMethodBuilder(LogManager pLogger,
                              Scope pScope, ASTConverter pAstCreator) {
    logger = pLogger;
    scope = pScope;
    astCreator = pAstCreator;
  }

  JMethodEntryNode getStartNode() {
    checkState(cfa != null);
    return cfa;
  }

  Set<CFANode> getCfaNodes() {
    checkState(cfa != null);
    return cfaNodes;
  }


  //Method to handle visiting a parsing problem.  Hopefully none exist
  /*
   *
   */
  @Override
  public void preVisit(ASTNode problem) {

    if (ASTNode.RECOVERED == problem.getFlags()
        || ASTNode.MALFORMED  == problem.getFlags()) {
      throw new CFAGenerationRuntimeException("Parse Error", problem);
    }
  }

  /*
   *
   */
  @Override
  public boolean visit(MethodDeclaration mDeclaration) {

    if (locStack.size() != 0) {
      throw new CFAGenerationRuntimeException("Nested method declarations?");
    }

    assert cfa == null;

    final JMethodDeclaration mdef = astCreator.convert(mDeclaration);

    handleMethodDeclaration(mdef);

    //If Declaration is Constructor add non static field member Declarations
    if (mDeclaration.isConstructor()) {
      addNonStaticFieldMember();
    }

    // Check if method has a body. Interface methods are always marked with abstract.
    if (!mdef.isAbstract() && !mdef.isNative()) {
      // Skip Children , and manually go to Block, to protect parameter variables to be processed
      // more than one time

      //TODO insert super Constructor if not explicit given.

      mDeclaration.getBody().accept(this);
    }

    return SKIP_CHILDS;
  }


  private void addNonStaticFieldMember() {

    JClassOrInterfaceType currentClassType = scope.getCurrentClassType();

    Map<String, JFieldDeclaration> fieldDecl = scope.getNonStaticFieldDeclarationOfClass(currentClassType);

    Collection<JFieldDeclaration> classFieldDeclaration =
      fieldDecl.values();

    for (JDeclaration decl : classFieldDeclaration) {

      List<JDeclaration> declaration = new LinkedList<>();
      declaration.add(decl);

      String rawSignature = decl.toASTString();

      CFANode nextNode = addDeclarationsToCFA(declaration, decl.getFileLocation(),
          rawSignature, locStack.poll());

      locStack.push(nextNode);
    }
  }

  private void handleMethodDeclaration(JMethodDeclaration fdef) {

    final String nameOfFunction = fdef.getName();
    assert !nameOfFunction.isEmpty();

    scope.enterMethod(fdef);

    final List<JParameterDeclaration> parameters = fdef.getParameters();
    final List<String> parameterNames = new ArrayList<>(parameters.size());

    for (JParameterDeclaration param : parameters) {
      scope.registerDeclarationOfThisClass(param); // declare parameter as local variable
      parameterNames.add(param.getName());
    }

    // Create initial CFA Nodes for Method (start, return, next)
    final FunctionExitNode returnNode =
        new FunctionExitNode(nameOfFunction);
    cfaNodes.add(returnNode);

    final JMethodEntryNode startNode =
        new JMethodEntryNode(fdef.getFileLocation(), fdef, returnNode, parameterNames);
    returnNode.setEntryNode(startNode);
    cfaNodes.add(startNode);
    cfa = startNode;

    final CFANode nextNode = new CFANode(nameOfFunction);
    cfaNodes.add(nextNode);
    locStack.add(nextNode);

    final BlankEdge dummyEdge =
        new BlankEdge("", fdef.getFileLocation(), startNode,
            nextNode, "Function start dummy edge");
    addToCFA(dummyEdge);
  }


  @Override
  public boolean visit(final VariableDeclarationStatement sd) {

    assert (locStack.size() > 0) : "not in a methods's scope";

    CFANode prevNode = locStack.pop();

    CFANode nextNode = addDeclarationsToCFA(sd, prevNode);

    assert nextNode != null;
    locStack.push(nextNode);

    return SKIP_CHILDS;
  }


   @Override
  public boolean visit(final SingleVariableDeclaration sd) {

    assert (locStack.size() > 0) : "not in a methods's scope";

    CFANode prevNode = locStack.pop();

    CFANode nextNode = addDeclarationsToCFA(sd, prevNode);

    assert nextNode != null;
    locStack.push(nextNode);

    return SKIP_CHILDS;
  }


  private void handleReturnFromObject(FileLocation fileloc,
                                       String rawSignature, ITypeBinding cb) {

    assert cb.isClass() : cb.getName() + "is no Object Return";

    CFANode prevNode = locStack.pop();
    FunctionExitNode functionExitNode = cfa.getExitNode();

    JReturnStatement cfaObjectReturn =
        astCreator.getConstructorObjectReturn(cb);


    JReturnStatementEdge edge =
        new JReturnStatementEdge("", cfaObjectReturn, fileloc,
                                 prevNode, functionExitNode);
    addToCFA(edge);

    CFANode nextNode = new CFANode(cfa.getFunctionName());

    cfaNodes.add(nextNode);
    locStack.push(nextNode);
  }

  @Override
  public void endVisit(MethodDeclaration declaration) {
    // If declaration is Constructor, add return for Object
    if (declaration.isConstructor()) {
      FileLocation fileloc = astCreator.getFileLocation(declaration);

      String rawSignature = declaration.toString();

      ITypeBinding declaringClass =
          declaration.resolveBinding().getDeclaringClass();

      handleReturnFromObject(fileloc, rawSignature, declaringClass);
    }

    handleEndVisitMethodDeclaration();
  }


  private void handleEndVisitMethodDeclaration() {

    if (locStack.size() != 1) {
      throw new CFAGenerationRuntimeException(
                    "Depth wrong. Geoff needs to do more work");
    }

    CFANode lastNode = locStack.pop();

    if (isReachableNode(lastNode)) {
      BlankEdge blankEdge = new BlankEdge("",
                                 FileLocation.DUMMY,
                                 lastNode, cfa.getExitNode(),
                                 "default return");
      addToCFA(blankEdge);
    }

    Set<CFANode> reachableNodes =
        CFATraversal.dfs().collectNodesReachableFrom(cfa);

    Iterator<CFANode> it = cfaNodes.iterator();

    while (it.hasNext()) {
      CFANode n = it.next();

      if (!reachableNodes.contains(n)) {
        // node was created but isn't part of CFA (e.g. because of dead code)
        it.remove(); // remove n from currentCFANodes
      }
    }

    scope.leaveMethod();
  }



  private CFANode handleSideassignments(CFANode prevNode,
                                         String rawSignature, FileLocation fileLocation) {
    // When Expressions, which are expected to be side effect free, are converted,
    // all side effects are transformed to Side Assignments. This Method
    // inserts them befor the expression is inserted in the AST:

    CFANode nextNode = null;

    while (astCreator.numberOfSideAssignments() > 0) {
      nextNode = new CFANode(cfa.getFunctionName());
      cfaNodes.add(nextNode);

      JAstNode sideeffect = astCreator.getNextSideAssignment();

      createSideAssignmentEdges(prevNode, nextNode, rawSignature,
                                fileLocation, sideeffect);
      prevNode = nextNode;
    }
    return prevNode;
  }


  private void handleSideassignments(CFANode prevNode, String rawSignature,
                                              FileLocation fileLocation, CFANode lastNode) {
    CFANode nextNode = null;

    while (astCreator.numberOfPreSideAssignments() > 0) {
      JAstNode sideeffect = astCreator.getNextPreSideAssignment();

      if (astCreator.numberOfPreSideAssignments() > 0) {
        nextNode = new CFANode(cfa.getFunctionName());
        cfaNodes.add(nextNode);
      } else {
        nextNode = lastNode;
      }

      createSideAssignmentEdges(prevNode, nextNode, rawSignature,
                                            fileLocation, sideeffect);
      prevNode = nextNode;
    }
  }

  @SuppressWarnings("unused")
  private CFANode handlePreSideassignments(CFANode prevNode,
      String rawSignature, FileLocation fileLocation) {

    CFANode nextNode = null;

    while (astCreator.numberOfPreSideAssignments() > 0) {
      nextNode = new CFANode(cfa.getFunctionName());
      cfaNodes.add(nextNode);

      JAstNode sideeffect = astCreator.getNextPreSideAssignment();

      createSideAssignmentEdges(prevNode, nextNode, rawSignature,
          fileLocation, sideeffect);
      prevNode = nextNode;
    }
    return prevNode;
  }

  @SuppressWarnings("unused")
  private CFANode handlePreSideassignments(CFANode prevNode,
      String rawSignature, FileLocation fileLocation, CFANode lastNode) {


    CFANode nextNode = null;

    while (astCreator.numberOfPreSideAssignments() > 0) {
      nextNode = new CFANode(cfa.getFunctionName());
      cfaNodes.add(nextNode);

      JAstNode sideeffect = astCreator.getNextPreSideAssignment();

      createSideAssignmentEdges(prevNode, nextNode, rawSignature,
          fileLocation, sideeffect);
      prevNode = nextNode;
    }
    return prevNode;
  }

  @SuppressWarnings("unused")
  private CFANode handlePostSideassignments(CFANode prevNode,
      String rawSignature, FileLocation fileLocation) {


    CFANode nextNode = null;

    while (astCreator.numberOfPostSideAssignments() > 0) {
      nextNode = new CFANode(cfa.getFunctionName());
      cfaNodes.add(nextNode);

      JAstNode sideeffect = astCreator.getNextPostSideAssignment();

      createSideAssignmentEdges(prevNode, nextNode, rawSignature,
          fileLocation, sideeffect);
      prevNode = nextNode;
    }
    return prevNode;
  }

  @SuppressWarnings("unused")
  private CFANode handlePostSideassignments(CFANode prevNode,
      String rawSignature, FileLocation fileLocation, CFANode lastNode) {


    CFANode nextNode = null;

    while (astCreator.numberOfPostSideAssignments() > 0) {
      nextNode = new CFANode(cfa.getFunctionName());
      cfaNodes.add(nextNode);

      JAstNode sideeffect = astCreator.getNextPostSideAssignment();

      createSideAssignmentEdges(prevNode, nextNode, rawSignature,
          fileLocation, sideeffect);
      prevNode = nextNode;
    }
    return prevNode;
  }


  private void createSideAssignmentEdges(CFANode prevNode, CFANode nextNode,
                                            String rawSignature, FileLocation fileLocation,
                                                             JAstNode sideeffect) {
    CFAEdge previous;

    if (sideeffect instanceof JStatement) {

      previous = new JStatementEdge(rawSignature, (JStatement) sideeffect,
                                           fileLocation, prevNode, nextNode);

    } else if (sideeffect instanceof JAssignment) { // TODO always false??

      previous = new JStatementEdge(rawSignature, (JStatement) sideeffect,
                                           fileLocation, prevNode, nextNode);

    } else if (sideeffect instanceof JIdExpression) {

      previous = new JStatementEdge(rawSignature,
                      new JExpressionStatement(sideeffect.getFileLocation(),
                                                (JExpression) sideeffect),
                                                fileLocation, prevNode, nextNode);

    } else {
      previous = new JDeclarationEdge(rawSignature, fileLocation,
                                                prevNode, nextNode,
                                                  (JDeclaration) sideeffect);
    }

    addToCFA(previous);
  }

  private CFANode addDeclarationsToCFA(final List<JDeclaration> declList,
      FileLocation fileLocation, String rawSignature,
      CFANode prevNode) {

    CFANode middleNode = new CFANode(cfa.getFunctionName());
    cfaNodes.add(middleNode);

    if (astCreator.getConditionalExpression() != null) {
      handleConditionalStatementWithDeclaration(prevNode, middleNode);
    } else {
      middleNode = prevNode;
    }

    middleNode =
        handleSideassignments(middleNode, rawSignature, fileLocation);

    // create one edge for every declaration
    for (JDeclaration newD : declList) {

     middleNode = addDeclarationtoCFA(newD, rawSignature, middleNode);

    }

    return middleNode;
  }


  private CFANode addDeclarationtoCFA(JDeclaration newD,
                                       String rawSignature, CFANode prevNode) {

    CFANode nextNode = new CFANode(cfa.getFunctionName());
    cfaNodes.add(nextNode);

    final JDeclarationEdge edge =
        new JDeclarationEdge(rawSignature, newD.getFileLocation(), prevNode, nextNode, newD);
    addToCFA(edge);

    prevNode = nextNode;

    // JFieldDeclaration are already parsed elsewhere
    if (newD instanceof JVariableDeclaration
        && !(newD instanceof JFieldDeclaration)) {

      scope.registerDeclarationOfThisClass(newD);

      IAInitializer initializer = ((JVariableDeclaration) newD).getInitializer();

      // resolve Boolean Initializer for easier analysis
      // if initializer is boolean. Not necessary for simple boolean literal.
      boolean resolveInitializer = initializer instanceof JInitializerExpression
          && astCreator.isBooleanExpression(
              ((JInitializerExpression) initializer).getExpression())
          && !(((JInitializerExpression) initializer).getExpression()
          instanceof JBooleanLiteralExpression);

      if (resolveInitializer) {
        prevNode =
            resolveBooleanInitializer((JVariableDeclaration) newD, prevNode);
      }

    }

    return prevNode;
  }

  private CFANode resolveBooleanInitializer(JVariableDeclaration newD,
                                                        CFANode prevNode) {

    IAInitializer initializer = newD.getInitializer();

    CFANode afterResolvedBooleanExpressionNode =
        new CFANode(cfa.getFunctionName());

    cfaNodes.add(afterResolvedBooleanExpressionNode);

    JExpression booleanInitializer =
        ((JInitializerExpression) initializer).getExpression();

    JIdExpression variableExpression =
        new JIdExpression(newD.getFileLocation(),
            newD.getType(), newD.getName(), newD);

    resolveBooleanAssignment(booleanInitializer,
        variableExpression, prevNode, afterResolvedBooleanExpressionNode);

    prevNode = afterResolvedBooleanExpressionNode;

    return prevNode;
  }

  private void handleConditionalStatementWithDeclaration(CFANode prevNode,
                                                            CFANode lastNode) {
    ConditionalExpression condExp = astCreator.getConditionalExpression();
    astCreator.resetConditionalExpression();
    handleTernaryExpression(condExp, prevNode, lastNode);
  }

  private CFANode addDeclarationsToCFA(
      final VariableDeclarationStatement sd, CFANode prevNode) {

    FileLocation fileLocation = astCreator.getFileLocation(sd);

    // If statement is an else condition Statement
    // if(condition){..} else int dec;
    handleElseCondition(sd);

    final List<JDeclaration> declList = astCreator.convert(sd);

    final String rawSignature = sd.toString();
    return addDeclarationsToCFA(declList, fileLocation, rawSignature, prevNode);
  }

  private CFANode addDeclarationsToCFA(
      final SingleVariableDeclaration sd, CFANode prevNode) {

    FileLocation fileLocation = astCreator.getFileLocation(sd);

    final List<JDeclaration> declList = new ArrayList<>(1);
    declList.add(astCreator.convert(sd));

    final String rawSignature = sd.toString();

    return addDeclarationsToCFA(declList, fileLocation, rawSignature, prevNode);
  }


  @Override
  public boolean visit(Block bl) {
    // TODO This works if else is a Block (else {Statement})
    // but not if it has just one statement (else Statement)
    // In that case, every Statement needs to be visited
    // and handleElsoCondition be implemented.
    handleElseCondition(bl);
    scope.enterBlock();

    return VISIT_CHILDS;
  }

  @Override
  public void  endVisit(Block bl) {
    scope.leaveBlock();
  }

  @Override
  public boolean visit(AssertStatement assertStatement) {

    handleElseCondition(assertStatement);

    FileLocation fileloc = astCreator.getFileLocation(assertStatement);
    String methodName = cfa.getFunctionName();
    Expression condition = assertStatement.getExpression();
    String rawSignature = assertStatement.toString();

    CFANode prevNode = locStack.pop();

    //Create CFA Node for end of assert Location and push to local Stack
    CFANode postAssertNode = new CFANode(methodName);
    cfaNodes.add(postAssertNode);
    locStack.push(postAssertNode);

    // Node for successful assert
    CFANode successfulNode = new CFANode(methodName);
    cfaNodes.add(successfulNode);

    // Error Label Node and unsuccessfulNode for unSuccessful assert,
    CFANode unsuccessfulNode = new CFANode(methodName);
    cfaNodes.add(unsuccessfulNode);

    CFANode endNode = new CFATerminationNode(methodName);
    cfaNodes.add(endNode);

    CONDITION kind = getConditionKind(condition);

    createConditionEdges(condition, fileloc, prevNode,
        successfulNode, unsuccessfulNode);

    boolean createUnsuccessfulEdge = true;
    boolean createSuccessfulEdge = true;

    switch (kind) {
    case ALWAYS_TRUE:
      createUnsuccessfulEdge = false;
      break;
    case ALWAYS_FALSE:
      createSuccessfulEdge = false;
      break;
    }


    BlankEdge blankEdge;

    //Blank Edge from successful assert to  postAssert location
    if (createSuccessfulEdge) {
      blankEdge =
          new BlankEdge(rawSignature, fileloc,
              successfulNode, postAssertNode, "assert success");
      addToCFA(blankEdge);
    }

    // Create Expression if necessary, then blank Edge to Error Node
    if (createUnsuccessfulEdge) {

      boolean hasMessage = assertStatement.getMessage() != null;

      if (!hasMessage) {
        blankEdge = new BlankEdge(rawSignature,
            fileloc,
            unsuccessfulNode, endNode, "assert fail");
        addToCFA(blankEdge);

      } else {

        astCreator.convertExpressionWithoutSideEffects(assertStatement.getMessage());

        unsuccessfulNode =
            handleSideassignments(unsuccessfulNode, rawSignature, fileloc);

        blankEdge = new BlankEdge(rawSignature, fileloc,
            unsuccessfulNode, endNode, "assert fail");
        addToCFA(blankEdge);
      }
    }

    return SKIP_CHILDS;
  }

  /**
   * This Method checks, if Statement is start of a else Condition Block
   * or Statement, changes the cfa accordingly.
   *
   *
   * @param statement Given statement to be checked.
   */
  private void handleElseCondition(Statement statement) {

    ASTNode node = statement;
    boolean isFirstElseStatement =
        node.getLocationInParent() == IfStatement.ELSE_STATEMENT_PROPERTY;

    // Handle special condition for else
    if (isFirstElseStatement) {
      // Edge from current location to post if-statement location
      CFANode prevNode = locStack.pop();
      CFANode nextNode = locStack.peek();

      if (isReachableNode(prevNode)) {
        BlankEdge blankEdge =
            new BlankEdge("", FileLocation.DUMMY, prevNode, nextNode, "");
        addToCFA(blankEdge);
      }

      //  Push the start of the else clause onto our location stack
      CFANode elseNode = elseStack.pop();
      locStack.push(elseNode);
    }
  }

  @Override
  public boolean visit(ExpressionStatement expressionStatement) {

    //TODO at the moment SideAssignmentSolving is wrong.
    // It has to be done from left to right with Side effects

    // When else is not in a block (else Statement)
    handleElseCondition(expressionStatement);

    CFANode prevNode = locStack.pop();

    JStatement statement = astCreator.convert(expressionStatement);

    boolean isReferencedInstanceMethod = statement instanceof AFunctionCall
        && ((AFunctionCall) statement).getFunctionCallExpression()
            instanceof JReferencedMethodInvocationExpression;

    // If this is a ReferencedFunctionCall, see if
    // the Run-Time-Class can be inferred
    if (isReferencedInstanceMethod) {
      JReferencedMethodInvocationExpression methodInvocation =
          (JReferencedMethodInvocationExpression) ((AFunctionCall) statement).getFunctionCallExpression();

      searchForRunTimeClass(methodInvocation, prevNode);
    }

    String rawSignature = expressionStatement.toString();

    CFANode lastNode = new CFANode(cfa.getFunctionName());

    cfaNodes.add(lastNode);

    if (astCreator.getConditionalExpression() != null) {

      handleConditionalStatement(prevNode, lastNode, statement);
      lastNode = handleSideassignments(lastNode, rawSignature, statement.getFileLocation());

    } else {

      CFANode nextNode =
          handleSideassignments(prevNode, rawSignature, statement.getFileLocation());

      boolean isResolvable = statement instanceof JExpressionAssignmentStatement
          && astCreator.isBooleanExpression(((JExpressionAssignmentStatement) statement).getRightHandSide())
          && !(((JExpressionAssignmentStatement) statement).getRightHandSide() instanceof JBooleanLiteralExpression);

      // Resolve boolean Assignments, resolve & , && , | , || to be easier analyzed
      if (isResolvable) {

        Assignment booleanAssignment =
            (Assignment) expressionStatement.getExpression();

        JExpressionAssignmentStatement booleanAssignmentExpression =
            (JExpressionAssignmentStatement) statement;

        resolveBooleanAssignment(booleanAssignment.getRightHandSide(),
            booleanAssignmentExpression.getLeftHandSide(), nextNode, lastNode);

      } else {

        JStatementEdge edge =
            new JStatementEdge(rawSignature, statement,
                statement.getFileLocation(), nextNode, lastNode);
        addToCFA(edge);
      }
    }

    locStack.push(lastNode);
    return SKIP_CHILDS;
  }


  @Override
  public boolean visit(SuperConstructorInvocation sCI) {

    // When else is not in a block (else Statement)
    handleElseCondition(sCI);

    CFANode prevNode = locStack.pop();

    JStatement statement = astCreator.convert(sCI);

    String rawSignature = sCI.toString();

    CFANode lastNode =
        new CFANode(cfa.getFunctionName());
    cfaNodes.add(lastNode);

    CFANode nextNode = handleSideassignments(prevNode, rawSignature, statement.getFileLocation());

    JStatementEdge edge =
        new JStatementEdge(rawSignature, statement, statement.getFileLocation(),
            nextNode, lastNode);
    addToCFA(edge);

    locStack.push(lastNode);

    return SKIP_CHILDS;
  }



  private void resolveBooleanAssignment(Expression condition,
              JLeftHandSide variableExpression, CFANode prevNode,
                    CFANode afterResolvedBooleanExpressionNode) {

    resolveBooleanAssignment(astCreator.convertBooleanExpression(condition),
        variableExpression, prevNode, afterResolvedBooleanExpressionNode);
  }

  private void resolveBooleanAssignment(JExpression condition,
      JLeftHandSide variableExpression, CFANode prevNode,
      CFANode afterResolvedBooleanExpressionNode) {

    FileLocation fileLocation =
        variableExpression.getFileLocation();

    String methodName = cfa.getFunctionName();
    String rawSignature = condition.toString();

    CFANode trueNode = new CFANode(methodName);
    cfaNodes.add(trueNode);

    CFANode falseNode = new CFANode(methodName);
    cfaNodes.add(falseNode);

    createConditionEdges(condition, fileLocation, prevNode, trueNode, falseNode);

    JExpressionAssignmentStatement trueAssign =
        astCreator.getBooleanAssign(variableExpression, true);

    JExpressionAssignmentStatement falseAssign =
        astCreator.getBooleanAssign(variableExpression, false);

    JStatementEdge trueAssignmentEdge =
        new JStatementEdge(rawSignature, trueAssign,
            variableExpression.getFileLocation(), trueNode, afterResolvedBooleanExpressionNode);
    addToCFA(trueAssignmentEdge);

    JStatementEdge falseAssignmentEdge =
        new JStatementEdge(rawSignature, falseAssign,
            variableExpression.getFileLocation(), falseNode, afterResolvedBooleanExpressionNode);
    addToCFA(falseAssignmentEdge);

  }



  private void searchForRunTimeClass(
      JReferencedMethodInvocationExpression methodInvocation, CFANode prevNode) {

    // This Algorithm  goes backwards from methodInvocation and searches
    // for a Class Instance Creation, which Class can be distinctly assigned as Run Time Class
    // If there is a distinct variable Assignment , the searched for variable reference will
    // changes to the assigned variable reference.

    // Class can only be found if there is only one Path to
    // a ClassInstanceCreation, stop if there is not exactly one
    boolean finished = prevNode.getNumEnteringEdges() != 1;
    CFANode traversedNode = prevNode;

    JSimpleDeclaration referencedVariable =
        methodInvocation.getReferencedVariable().getDeclaration();

    while (!finished) {

      CFAEdge currentEdge = traversedNode.getEnteringEdge(ONLY_EDGE);

      // Look for Instance Creation Assignment and Variable Assignment.
      // Stop if there is a function Call and the Variable is a FieldDeclaration
      // or there is an Assignment Function Call which isn't a Instance Creation Assignment

      if (currentEdge.getEdgeType() == CFAEdgeType.StatementEdge) {

        JStatement statement =  ((JStatementEdge) currentEdge).getStatement();

        if (statement instanceof JExpressionAssignmentStatement) {

          if (isReferencableVariable(referencedVariable, (JAssignment) statement)) {

            referencedVariable = assignVariableReference((JExpressionAssignmentStatement) statement);

          } else {

            finished = isReferenced(referencedVariable, (JAssignment) statement);
          }
        } else if (statement instanceof JMethodInvocationStatement) {

          finished = (referencedVariable instanceof JFieldDeclaration);

        } else if (statement instanceof JMethodInvocationAssignmentStatement) {

          finished = isReferenced(referencedVariable, (JAssignment) statement);

          if (finished) {
            assignClassRunTimeInstanceIfInstanceCreation(methodInvocation,
                (JMethodInvocationAssignmentStatement) statement);
          }
        }
      }

      // if not finished, continue iff there is only one path
      finished = finished || !(traversedNode.getNumEnteringEdges() != 1);

      if (!finished) {
        traversedNode = currentEdge.getPredecessor();
      }
    }
  }


  private boolean isReferenced(JSimpleDeclaration referencedVariable, JAssignment assignment) {
    JExpression leftHandSide = assignment.getLeftHandSide();

    return (leftHandSide instanceof JIdExpression)
        && ((JIdExpression) leftHandSide).getDeclaration().getName().equals(
            referencedVariable.getName());
  }

  private void assignClassRunTimeInstanceIfInstanceCreation(
      JReferencedMethodInvocationExpression methodInvocation,
      JMethodInvocationAssignmentStatement functionCallAssignment) {

    JMethodInvocationExpression methodCall =
        functionCallAssignment.getFunctionCallExpression();

    if (methodCall instanceof JClassInstanceCreation) {
      astCreator.assignRunTimeClass(
          methodInvocation, (JClassInstanceCreation) methodCall);
    }

  }

  private JSimpleDeclaration assignVariableReference(JExpressionAssignmentStatement expressionAssignment) {

    JIdExpression newReferencedVariable =
        (JIdExpression) expressionAssignment.getRightHandSide();
    return newReferencedVariable.getDeclaration();
  }

  private boolean isReferencableVariable(JSimpleDeclaration referencedVariable, JAssignment assignment) {

    JExpression leftHandSide = assignment.getLeftHandSide();
    JRightHandSide rightHandSide = assignment.getRightHandSide();

    return (leftHandSide instanceof JIdExpression)
        && (rightHandSide instanceof JIdExpression)
        && ((JIdExpression) leftHandSide).getDeclaration().getName().equals(
            referencedVariable.getName());
  }



private void handleConditionalStatement(CFANode prevNode,
                                      CFANode lastNode, JStatement statement) {

  ConditionalExpression condExp = astCreator.getConditionalExpression();
  astCreator.resetConditionalExpression();

  //unpack unaryExpressions or Statements if there are some
  ASTNode parentExp = condExp.getParent();
  while (parentExp.getNodeType() == ASTNode.PARENTHESIZED_EXPRESSION
         || parentExp.getNodeType() == ASTNode.POSTFIX_EXPRESSION
         || parentExp.getNodeType() == ASTNode.PREFIX_EXPRESSION) {
    parentExp = parentExp.getParent();
  }

  //evaluates to true if the ternary expressions return value is not used (i. e. var==0 ? 0 : 1;)
  if (parentExp.getNodeType() != ASTNode.VARIABLE_DECLARATION_STATEMENT
      && parentExp.getNodeType() != ASTNode.ASSIGNMENT) {
    handleTernaryStatement(condExp, prevNode, lastNode);
  } else {
    if (statement != null) {
      handleTernaryExpression(condExp, prevNode, lastNode, statement);
    } else {
      handleTernaryExpression(condExp, prevNode, lastNode);
    }
  }
}

  private void handleTernaryExpression(ConditionalExpression condExp,
                                CFANode rootNode, CFANode lastNode, JAstNode pExp) {

    FileLocation fileLoc = astCreator.getFileLocation(condExp);

    CFANode middle = new CFANode(cfa.getFunctionName());
    cfaNodes.add(middle);

    handleTernaryExpression(condExp, rootNode, middle);

    String signature = pExp.toASTString();

    createSideAssignmentEdges(middle, lastNode, signature, fileLoc, pExp);
  }

private void handleTernaryExpression(ConditionalExpression condExp,
                                        CFANode rootNode, CFANode lastNode) {

   FileLocation fileLoc = astCreator.getFileLocation(condExp);
   String rawSignature = condExp.toString();

   JIdExpression tempVar = astCreator.getConditionalTemporaryVariable();

   rootNode = handleSideassignments(rootNode, rawSignature, fileLoc);

   CFANode thenNode = new CFANode(cfa.getFunctionName());
   cfaNodes.add(thenNode);
   CFANode elseNode = new CFANode(cfa.getFunctionName());
   cfaNodes.add(elseNode);

   Expression condtion = condExp.getExpression();

   buildConditionTree(condtion, fileLoc, rootNode, thenNode, elseNode,
                                            thenNode, elseNode, true, true);

   Expression thenExp = condExp.getThenExpression();
   Expression elseExp = condExp.getElseExpression();

   createTernaryExpressionEdges(thenExp, lastNode, fileLoc, thenNode, tempVar);
   createTernaryExpressionEdges(elseExp, lastNode, fileLoc, elseNode, tempVar);
 }

 private void handleTernaryStatement(ConditionalExpression condExp,
                                               CFANode rootNode, CFANode lastNode) {

   FileLocation fileLoc = astCreator.getFileLocation(condExp);

   while (astCreator.numberOfPreSideAssignments() > 0) {
     astCreator.getNextPreSideAssignment();
   }

   CFANode thenNode = new CFANode(cfa.getFunctionName());
   cfaNodes.add(thenNode);
   CFANode elseNode = new CFANode(cfa.getFunctionName());
   cfaNodes.add(elseNode);

   Expression condition = condExp.getExpression();

   buildConditionTree(condition, fileLoc, rootNode, thenNode, elseNode, thenNode, elseNode, true, true);

   Expression thenExp = condExp.getThenExpression();
   Expression elseExp = condExp.getElseExpression();

   createTernaryStatementEdges(thenExp, lastNode, fileLoc, thenNode);
   createTernaryStatementEdges(elseExp, lastNode, fileLoc, elseNode);
 }

  private void createTernaryExpressionEdges(
      Expression condExp, CFANode lastNode, FileLocation fileLocation,
      CFANode prevNode, JIdExpression tempVar) {

    JAstNode exp = astCreator.convertExpressionWithSideEffects(condExp);

    boolean noFurtherConditionalExpression =
        exp != astCreator.getConditionalTemporaryVariable()
            && astCreator.getConditionalExpression() == null;

    if (noFurtherConditionalExpression) {

      CFANode tmp;

      if (astCreator.getConditionalExpression() != null) {

        tmp = new CFANode(cfa.getFunctionName());
        cfaNodes.add(tmp);
        handleTernaryExpressionTail(exp, fileLocation, prevNode, tmp, tempVar);
        prevNode = tmp;

      } else if (astCreator.numberOfSideAssignments() > 0) {

        tmp = new CFANode(cfa.getFunctionName());
        cfaNodes.add(tmp);
        handleSideassignments(prevNode, exp.toASTString(), fileLocation, tmp);
        prevNode = tmp;

      }

      JStatementEdge edge;

      FileLocation fileLoc = astCreator.getFileLocation(condExp);
      String rawSignature = condExp.toString();

      if (exp instanceof JExpression) {

        JExpressionAssignmentStatement assignment =
            new JExpressionAssignmentStatement(
                  fileLoc, tempVar, (JExpression) exp);

        edge = new JStatementEdge(rawSignature, assignment,
                                    fileLocation, prevNode, lastNode);
        addToCFA(edge);
      } else if (exp instanceof JMethodInvocationExpression) {

        JMethodInvocationAssignmentStatement assignment =
            new JMethodInvocationAssignmentStatement(
                fileLoc, tempVar, (JMethodInvocationExpression) exp);

        edge = new JStatementEdge(rawSignature, assignment,
                                    fileLocation, prevNode, lastNode);
        addToCFA(edge);

      } else {

        CFANode middle = new CFANode(cfa.getFunctionName());
        cfaNodes.add(middle);
        edge = new JStatementEdge(rawSignature, (JStatement) exp,
                                     fileLocation, prevNode, middle);
        addToCFA(edge);

        JExpressionAssignmentStatement assignment =
            new JExpressionAssignmentStatement(
               fileLoc, tempVar, ((JAssignment) exp).getLeftHandSide());

        edge = new JStatementEdge(condExp.toString(),
            assignment, fileLocation, middle, lastNode);
        addToCFA(edge);
      }

    } else {
      handleTernaryExpressionTail(exp, fileLocation, prevNode, lastNode, tempVar);
    }
  }

  private void createTernaryStatementEdges(Expression condExp, CFANode lastNode, FileLocation fileLocation, CFANode prevNode) {
    JAstNode exp = astCreator.convertExpressionWithSideEffects(condExp);

    if (exp != astCreator.getConditionalTemporaryVariable() && astCreator.getConditionalExpression() == null) {

      CFANode tmp;
      if (astCreator.getConditionalExpression() != null) {
        tmp = new CFANode(cfa.getFunctionName());
        cfaNodes.add(tmp);
        handleTernaryStatementTail(exp, fileLocation, prevNode, tmp);
        prevNode = tmp;
      } else if (astCreator.numberOfPreSideAssignments() > 0) {
        tmp = new CFANode(cfa.getFunctionName());
        cfaNodes.add(tmp);
        handleSideassignments(prevNode, exp.toASTString(), fileLocation, tmp);
        prevNode = tmp;
      }

      JStatementEdge edge;



      if (exp instanceof JExpression) {
        edge = new JStatementEdge(condExp.toString(),
            new JExpressionStatement(astCreator.getFileLocation(condExp), (JExpression) exp),
            fileLocation, prevNode, lastNode);
        addToCFA(edge);
      } else if (exp instanceof JMethodInvocationExpression) {
        edge = new JStatementEdge(condExp.toString(),
            (new JMethodInvocationStatement(astCreator.getFileLocation(condExp), (JMethodInvocationExpression) exp)),
            fileLocation, prevNode, lastNode);
        addToCFA(edge);
      } else {
        CFANode middle = new CFANode(cfa.getFunctionName());
        cfaNodes.add(middle);
        edge = new JStatementEdge(condExp.toString(), (JStatement) exp, fileLocation, prevNode, middle);
        addToCFA(edge);
        edge = new JStatementEdge(condExp.toString(),
            new JExpressionStatement(astCreator.getFileLocation(condExp),
                ((JExpressionAssignmentStatement) exp).getLeftHandSide()),
                fileLocation, middle, lastNode);
        addToCFA(edge);
      }
    } else {
      handleTernaryStatementTail(exp, fileLocation, prevNode, lastNode);
    }
  }

  private void handleTernaryExpressionTail(JAstNode exp, FileLocation fileLocation, CFANode branchNode, CFANode lastNode,
      JIdExpression leftHandSide) {
    CFANode nextNode = new CFANode(cfa.getFunctionName());
    cfaNodes.add(nextNode);

    ConditionalExpression condExp = astCreator.getConditionalExpression();
    astCreator.resetConditionalExpression();

    JIdExpression rightHandSide = astCreator.getConditionalTemporaryVariable();

    handleTernaryExpression(condExp, branchNode, nextNode, exp);
    JStatement stmt = new JExpressionAssignmentStatement(exp.getFileLocation(), leftHandSide, rightHandSide);
    addToCFA(new JStatementEdge(stmt.toASTString(), stmt, fileLocation, nextNode, lastNode));
  }

  private void handleTernaryStatementTail(JAstNode exp, FileLocation fileLocation, CFANode branchNode, CFANode lastNode) {
    CFANode nextNode;
    nextNode = new CFANode(cfa.getFunctionName());
    cfaNodes.add(nextNode);

    ConditionalExpression condExp = astCreator.getConditionalExpression();
    astCreator.resetConditionalExpression();

    JIdExpression rightHandSide = astCreator.getConditionalTemporaryVariable();

    handleTernaryExpression(condExp, branchNode, nextNode);
    JStatement stmt = new JExpressionStatement(exp.getFileLocation(), rightHandSide);
    addToCFA(new JStatementEdge(stmt.toASTString(), stmt, fileLocation, nextNode, lastNode));
  }

  @Override
  public boolean visit(IfStatement ifStatement) {

    FileLocation fileloc = astCreator.getFileLocation(ifStatement);

    // If parent Else is not a Block
    handleElseCondition(ifStatement);

    CFANode prevNode = locStack.pop();

    CFANode postIfNode = new CFANode(cfa.getFunctionName());

    cfaNodes.add(postIfNode);
    locStack.push(postIfNode);

    CFANode thenNode = new CFANode(cfa.getFunctionName());
    cfaNodes.add(thenNode);
    locStack.push(thenNode);

    CFANode elseNode;

    // elseNode is the start of the else branch,
    // or the node after the loop if there is no else branch
    boolean noElseBranch = ifStatement.getElseStatement() == null;

    if (noElseBranch) {
      elseNode = postIfNode;
    } else {
      elseNode = new CFANode(cfa.getFunctionName());
      cfaNodes.add(elseNode);
      elseStack.push(elseNode);
    }

    Expression condition = ifStatement.getExpression();

    createConditionEdges(condition, fileloc, prevNode, thenNode, elseNode);

    return VISIT_CHILDS;
  }

  @Override
  public void endVisit(IfStatement ifStatement) {

    final CFANode prevNode = locStack.pop();
    final CFANode nextNode = locStack.peek();

    if (isReachableNode(prevNode)) {

      for (CFAEdge prevEdge : CFAUtils.allEnteringEdges(prevNode).toList()) {

        boolean isBlankEdge = (prevEdge instanceof BlankEdge)
                                && prevEdge.getDescription().equals("");

        if (isBlankEdge) {

          // the only entering edge is a BlankEdge, so we delete this edge and prevNode

          CFANode prevPrevNode = prevEdge.getPredecessor();
          assert prevPrevNode.getNumLeavingEdges() == 1;
          prevNode.removeEnteringEdge(prevEdge);
          prevPrevNode.removeLeavingEdge(prevEdge);

          BlankEdge blankEdge = new BlankEdge("", prevEdge.getFileLocation(),
              prevPrevNode, nextNode, "");
          addToCFA(blankEdge);
        }

      }

      if (prevNode.getNumEnteringEdges() > 0) {
        BlankEdge blankEdge = new BlankEdge("", FileLocation.DUMMY,
            prevNode, nextNode, "");
        addToCFA(blankEdge);
      }
    }
  }

  private static enum CONDITION {
    NORMAL,
    ALWAYS_FALSE,
    ALWAYS_TRUE
  }

  private void createConditionEdges(Expression condition,
       FileLocation fileLocation, CFANode rootNode, CFANode thenNode, CFANode elseNode) {

    createConditionEdges(astCreator.convertBooleanExpression(condition),
                                fileLocation, rootNode, thenNode, elseNode);
  }

  private void createConditionEdges(JExpression condition, final FileLocation fileLocation,
      CFANode rootNode, CFANode thenNode, final CFANode elseNode) {

    assert condition != null;
    final CONDITION kind = getConditionKind(condition);
    String rawSignature = condition.toString();

    switch (kind) {
    case ALWAYS_FALSE:
      // no edge connecting rootNode with thenNode,
      // so the "then" branch won't be connected to the rest of the CFA

      final BlankEdge falseEdge =
          new BlankEdge(rawSignature, fileLocation, rootNode, elseNode, "");

      addToCFA(falseEdge);
      break;

    case ALWAYS_TRUE:

      final BlankEdge trueEdge =
          new BlankEdge(rawSignature, fileLocation, rootNode, thenNode, "");
      addToCFA(trueEdge);

      // no edge connecting prevNode with elseNode,
      // so the "else" branch won't be connected to the rest of the CFA
      break;

    case NORMAL:

      buildConditionTree(condition, fileLocation, rootNode,
          thenNode, elseNode, thenNode, elseNode, true, true);

      break;
    default:
      throw new InternalError("Missing switch clause");
    }
  }

  private void buildConditionTree(JExpression condition, final FileLocation fileLocation,
      CFANode rootNode, CFANode thenNode, final CFANode elseNode,
      CFANode thenNodeForLastThen, CFANode elseNodeForLastElse,
      boolean furtherThenComputation, boolean furtherElseComputation) {

    int filelocStart = fileLocation.getStartingLineNumber();
    if (condition instanceof JBinaryExpression
        && (((JBinaryExpression) condition).getOperator() == JBinaryExpression.BinaryOperator.CONDITIONAL_AND)) {
      CFANode innerNode = new CFANode(cfa.getFunctionName());
      cfaNodes.add(innerNode);
      buildConditionTree(((JBinaryExpression) condition).getOperand1(), fileLocation, rootNode, innerNode, elseNode,
          thenNodeForLastThen, elseNodeForLastElse, true, true);
      buildConditionTree(((JBinaryExpression) condition).getOperand2(), fileLocation, innerNode, thenNode, elseNode,
          thenNodeForLastThen, elseNodeForLastElse, true, true);

    } else if (condition instanceof JBinaryExpression
        && ((JBinaryExpression) condition).getOperator() == JBinaryExpression.BinaryOperator.CONDITIONAL_OR) {
      CFANode innerNode = new CFANode(cfa.getFunctionName());
      cfaNodes.add(innerNode);
      buildConditionTree(((JBinaryExpression) condition).getOperand1(), fileLocation, rootNode, thenNode, innerNode,
          thenNodeForLastThen, elseNodeForLastElse, true, true);
      buildConditionTree(((JBinaryExpression) condition).getOperand2(), fileLocation, innerNode, thenNode, elseNode,
          thenNodeForLastThen, elseNodeForLastElse, true, true);

    } else if (condition instanceof JBinaryExpression
        && ((JBinaryExpression) condition).getOperator() == JBinaryExpression.BinaryOperator.LOGICAL_OR) {
      CFANode innerNode = new CFANode(cfa.getFunctionName());
      CFANode innerEagerNode = new CFANode(cfa.getFunctionName());
      cfaNodes.add(innerNode);
      cfaNodes.add(innerEagerNode);
      buildConditionTree(((JBinaryExpression) condition).getOperand1(), fileLocation, rootNode, innerEagerNode,
          innerNode,
          thenNodeForLastThen, elseNodeForLastElse, true, true);
      buildConditionTree(((JBinaryExpression) condition).getOperand2(), fileLocation, innerNode, thenNode, elseNode,
          thenNodeForLastThen, elseNodeForLastElse, true, true);
      buildConditionTree(((JBinaryExpression) condition).getOperand2(), fileLocation, innerEagerNode, thenNode,
          thenNode,
          thenNodeForLastThen, elseNodeForLastElse, true, true);

    } else if (condition instanceof JBinaryExpression
        && ((JBinaryExpression) condition).getOperator() == JBinaryExpression.BinaryOperator.LOGICAL_AND) {
      CFANode innerNode = new CFANode(cfa.getFunctionName());
      CFANode innerEagerNode = new CFANode(cfa.getFunctionName());
      cfaNodes.add(innerNode);
      cfaNodes.add(innerEagerNode);
      buildConditionTree(((JBinaryExpression) condition).getOperand1(), fileLocation, rootNode, innerNode,
          innerEagerNode,
          thenNodeForLastThen, elseNodeForLastElse, true, true);
      buildConditionTree(((JBinaryExpression) condition).getOperand2(), fileLocation, innerNode, thenNode, elseNode,
          thenNodeForLastThen, elseNodeForLastElse, true, true);
      buildConditionTree(((JBinaryExpression) condition).getOperand2(), fileLocation, innerEagerNode, elseNode,
          elseNode,
          thenNodeForLastThen, elseNodeForLastElse, true, true);


    } else {
      buildConditionTreeCondition(condition, fileLocation,
          rootNode, thenNode, elseNode,
          thenNodeForLastThen, elseNodeForLastElse,
          furtherThenComputation, furtherElseComputation);

    }
  }



  private void buildConditionTreeCondition(JExpression condition, final FileLocation fileLocation,
      CFANode rootNode, CFANode thenNode, final CFANode elseNode,
      CFANode thenNodeForLastThen, CFANode elseNodeForLastElse,
      boolean furtherThenComputation, boolean furtherElseComputation) {



    String rawSignature = condition.toASTString();

    if (furtherThenComputation) {
      thenNodeForLastThen = thenNode;
    }
    if (furtherElseComputation) {
      elseNodeForLastElse = elseNode;
    }

    CFANode nextNode = null;

    if (astCreator.getConditionalExpression() != null) {
      nextNode = new CFANode(cfa.getFunctionName());
      cfaNodes.add(nextNode);
      handleConditionalStatement(rootNode, nextNode, null);
    } else {
      nextNode = rootNode;
    }

    nextNode = handleSideassignments(nextNode, rawSignature, condition.getFileLocation());


    if (thenNode.equals(elseNode)) {
      final BlankEdge blankEdge = new BlankEdge(rawSignature, fileLocation, nextNode, elseNode, rawSignature);
      addToCFA(blankEdge);
      return;
    }

    // edge connecting last condition with elseNode
    final JAssumeEdge JAssumeEdgeFalse = new JAssumeEdge("!(" + rawSignature + ")",
        fileLocation,
        nextNode,
        elseNodeForLastElse,
        condition,
        false);
    addToCFA(JAssumeEdgeFalse);

    // edge connecting last condition with thenNode
    final JAssumeEdge JAssumeEdgeTrue = new JAssumeEdge(rawSignature,
        fileLocation,
        nextNode,
        thenNodeForLastThen,
        condition,
        true);
    addToCFA(JAssumeEdgeTrue);
  }

  private void buildConditionTree(Expression condition, final FileLocation fileLocation,
      CFANode rootNode, CFANode thenNode, final CFANode elseNode,
      CFANode thenNodeForLastThen, CFANode elseNodeForLastElse,
      boolean furtherThenComputation, boolean furtherElseComputation) {

    JExpression cond = astCreator.convertBooleanExpression(condition);

    buildConditionTree(cond, fileLocation, rootNode, thenNode, elseNode, thenNodeForLastThen, elseNodeForLastElse,
        furtherThenComputation, furtherElseComputation);
  }

  private CONDITION getConditionKind(JExpression condition) {
    if (condition instanceof JBooleanLiteralExpression) {
      if (((JBooleanLiteralExpression) condition).getValue()) {
        return CONDITION.ALWAYS_TRUE;
      } else {
        return CONDITION.ALWAYS_FALSE;
      }
    }
    return CONDITION.NORMAL;
  }



  private CONDITION getConditionKind(Expression cond) {

    while (cond.getNodeType() == ASTNode.PARENTHESIZED_EXPRESSION) {
      cond = ((ParenthesizedExpression) cond).getExpression();
    }

    if (cond.getNodeType() == ASTNode.BOOLEAN_LITERAL) {
      if (((BooleanLiteral) cond).booleanValue()) {
        return CONDITION.ALWAYS_TRUE;
      } else {
        return CONDITION.ALWAYS_FALSE;
      }
    }
    return CONDITION.NORMAL;
  }

  @Override
  public boolean visit(LabeledStatement labelStatement) {

    //If parent is a else Condition without block
    handleElseCondition(labelStatement);

    String labelName = labelStatement.getLabel().getIdentifier();

    if (labelMap.containsKey(labelName)) {
      throw new CFAGenerationRuntimeException("Duplicate label " + labelName
        + " in method " + cfa.getFunctionName(), labelStatement);
    }


    String mehtodName = cfa.getFunctionName();
    // In Java label Node is placed after Label Body
    CLabelNode labelNode = new CLabelNode(mehtodName, labelName);
    cfaNodes.add(labelNode);
    labelMap.put(labelName, labelNode);

    // initialize continueMap if necessary
    int innerStatementTyp = labelStatement.getBody().getNodeType();
    boolean innerStatementIsLoop = innerStatementTyp == ASTNode.FOR_STATEMENT
        || innerStatementTyp == ASTNode.WHILE_STATEMENT
        || innerStatementTyp == ASTNode.DO_STATEMENT;

    if (innerStatementIsLoop) {
      registeredContinues.put(labelName, new LinkedList<Pair<CFANode, ContinueStatement>>());
    }

    //  Skip to Body
    labelStatement.getBody().accept(this);

    return SKIP_CHILDS;
  }

  @Override
  public void endVisit(LabeledStatement labelStatement) {

    String labelName = labelStatement.getLabel().getIdentifier();

    assert labelMap.containsKey(labelName) :
          "Label Name " + labelName + " to be deleted "
        + "out of scope, but scope does not contain it";

    // Add Edge from end of Label Body to Label
    CLabelNode labelNode = labelMap.get(labelStatement.getLabel().getIdentifier());
    CFANode prevNode = locStack.pop();

    if (isReachableNode(prevNode)) {
      BlankEdge blankEdge = new BlankEdge(labelStatement.toString(),
          FileLocation.DUMMY, prevNode, labelNode, "Label: " + labelName);
      addToCFA(blankEdge);
    }

    if (registeredContinues.containsKey(labelName)) {
      handleContinues(prevNode, registeredContinues.get(labelName));
      registeredContinues.remove(labelName);
    }


    locStack.push(labelNode);
    labelMap.remove(labelStatement.getLabel().getIdentifier());
  }



  private void handleContinues(CFANode nodeAfterLoopStart, List<Pair<CFANode, ContinueStatement>> continues) {

    CFANode startLoopNode = getStartLoopNodeFromPrevLabelNode(nodeAfterLoopStart);

    for (Pair<CFANode, ContinueStatement> continuePair : continues) {
      handleLabledContinueStatement(continuePair.getSecond(), continuePair.getFirst(), startLoopNode);
    }


  }

  private CFANode getStartLoopNodeFromPrevLabelNode(CFANode nodeAfterLoopStart) {

    CFAEdge edge = nodeAfterLoopStart.getEnteringEdge(ONLY_EDGE);

    assert edge.getPredecessor().isLoopStart() : "Did not find start Loop";

    return edge.getPredecessor();
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean visit(final SwitchStatement statement) {

    // If parent is else and not a block
    handleElseCondition(statement);

    FileLocation fileloc = astCreator.getFileLocation(statement);
    final CFANode prevNode = locStack.pop();

    // firstSwitchNode is first Node of switch-Statement.
    // TODO useful or unnecessary? it can be replaced through prevNode.
    final CFANode firstSwitchNode =
        new CFANode(cfa.getFunctionName());
    cfaNodes.add(firstSwitchNode);

    JExpression switchExpression = astCreator
        .convertExpressionWithoutSideEffects(statement.getExpression());

    // TODO rawSignature ok in that way?
    String rawSignature = "switch (" + statement.getExpression().toString() + ")";
    String description = "switch (" + switchExpression.toASTString() + ")";

    BlankEdge firstSwitchEdge = new BlankEdge(rawSignature, fileloc,
        prevNode, firstSwitchNode, description);

    addToCFA(firstSwitchEdge);

    switchExprStack.push(switchExpression);
    switchCaseStack.push(firstSwitchNode);

    // postSwitchNode is Node after the switch-statement
    final CFANode postSwitchNode =
        new CFANode(cfa.getFunctionName());

    cfaNodes.add(postSwitchNode);
    loopNextStack.push(postSwitchNode);
    locStack.push(postSwitchNode);

    locStack.push(new CFANode(cfa.getFunctionName()));

    // visit body,
    for (Statement st : (List<Statement>) statement.statements()) {
      st.accept(this);
    }

    // leave switch
    final CFANode lastNodeInSwitch = locStack.pop();
    final CFANode lastNotCaseNode = switchCaseStack.pop();
    switchExprStack.pop(); // switchExpr is not needed after this point

    assert postSwitchNode == loopNextStack.pop();
    assert postSwitchNode == locStack.peek();
    assert switchExprStack.size() == switchCaseStack.size();

    final BlankEdge blankEdge = new BlankEdge("", FileLocation.DUMMY,
        lastNotCaseNode, postSwitchNode, "");
    addToCFA(blankEdge);

    final BlankEdge blankEdge2 = new BlankEdge("", FileLocation.DUMMY,
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


  private void handleCase(final SwitchCase statement, FileLocation fileloc) {

    // build condition, left part, "a"
    final JExpression switchExpr =
        switchExprStack.peek();

    // build condition, right part, "2"
    final JExpression caseExpr =
        astCreator.convertExpressionWithoutSideEffects(statement.getExpression());

    // build condition, "a= 2
    final JBinaryExpression binExp =
        new JBinaryExpression(fileloc,
            switchExpr.getExpressionType(), switchExpr, caseExpr,
            JBinaryExpression.BinaryOperator.EQUALS);

    // build condition edges, to caseNode with "a==2", to notCaseNode with "!(a==2)"
    final CFANode rootNode = switchCaseStack.pop();
    final CFANode caseNode = new CFANode(cfa.getFunctionName());
    final CFANode notCaseNode = new CFANode(cfa.getFunctionName());
    cfaNodes.add(caseNode);
    cfaNodes.add(notCaseNode);

    // fall-through (case before has no "break")
    final CFANode oldNode = locStack.pop();
    if (oldNode.getNumEnteringEdges() > 0) {
      final BlankEdge blankEdge =
          new BlankEdge("", fileloc, oldNode, caseNode, "fall through");
      addToCFA(blankEdge);
    }


    switchCaseStack.push(notCaseNode);
    locStack.push(caseNode);

    // edge connecting rootNode with notCaseNode, "!(a==2)"
    final JAssumeEdge JAssumeEdgeFalse = new JAssumeEdge("!(" + binExp.toASTString() + ")",
        fileloc, rootNode, notCaseNode, binExp, false);
    addToCFA(JAssumeEdgeFalse);

    // edge connecting rootNode with caseNode, "a==2"
    final JAssumeEdge JAssumeEdgeTrue = new JAssumeEdge(binExp.toASTString(),
        fileloc, rootNode, caseNode, binExp, true);
    addToCFA(JAssumeEdgeTrue);


  }


  private void handleDefault(FileLocation fileloc) {

    // build blank edge to caseNode with "default", no edge to notCaseNode
    final CFANode rootNode = switchCaseStack.pop();
    final CFANode caseNode = new CFANode(cfa.getFunctionName());
    final CFANode notCaseNode = new CFANode(cfa.getFunctionName());
    cfaNodes.add(caseNode);
    cfaNodes.add(notCaseNode);

    // fall-through (case before has no "break")
    final CFANode oldNode = locStack.pop();
    if (oldNode.getNumEnteringEdges() > 0) {
      final BlankEdge blankEdge =
          new BlankEdge("", fileloc, oldNode, caseNode, "fall through");
      addToCFA(blankEdge);
    }

    switchCaseStack.push(notCaseNode); // for later cases, only reachable through jumps
    locStack.push(caseNode);

    // blank edge connecting rootNode with caseNode
    final BlankEdge trueEdge =
        new BlankEdge("default :", fileloc, rootNode, caseNode, "default");
    addToCFA(trueEdge);
  }


  @Override
  public boolean visit(WhileStatement whileStatement) {

    handleElseCondition(whileStatement);

    FileLocation fileloc = astCreator.getFileLocation(whileStatement);
    final CFANode prevNode = locStack.pop();

    final CFANode loopStart = new CFANode(cfa.getFunctionName());
    cfaNodes.add(loopStart);
    loopStart.setLoopStart();
    loopStartStack.push(loopStart);

    final CFANode firstLoopNode = new CFANode(cfa.getFunctionName());
    cfaNodes.add(firstLoopNode);

    final CFANode postLoopNode = new CFANode(cfa.getFunctionName());
    cfaNodes.add(postLoopNode);
    loopNextStack.push(postLoopNode);

    // inverse order here!
    locStack.push(postLoopNode);
    locStack.push(firstLoopNode);

    final BlankEdge blankEdge =
        new BlankEdge("", fileloc, prevNode, loopStart, "while");
    addToCFA(blankEdge);

    Expression condition = whileStatement.getExpression();
    createConditionEdges(condition, fileloc,
        loopStart, firstLoopNode, postLoopNode);

    //Visit Body, Expression already handled.
    whileStatement.getBody().accept(this);

    return SKIP_CHILDS;
  }

  @Override
  public boolean visit(DoStatement doStatement) {

    handleElseCondition(doStatement);

    FileLocation fileloc = astCreator.getFileLocation(doStatement);
    final CFANode prevNode = locStack.pop();

    final CFANode loopStart = new CFANode(cfa.getFunctionName());
    cfaNodes.add(loopStart);
    loopStart.setLoopStart();
    loopStartStack.push(loopStart);

    final CFANode firstLoopNode = new CFANode(cfa.getFunctionName());
    cfaNodes.add(firstLoopNode);

    final CFANode postLoopNode = new CFANode(cfa.getFunctionName());

    cfaNodes.add(postLoopNode);
    loopNextStack.push(postLoopNode);

    // inverse order here!
    locStack.push(postLoopNode);
    locStack.push(firstLoopNode);

    final BlankEdge blankEdge =
        new BlankEdge("", fileloc, prevNode, firstLoopNode, "do");
    addToCFA(blankEdge);

    Expression condition = doStatement.getExpression();

    createConditionEdges(condition, fileloc,
        loopStart, firstLoopNode, postLoopNode);

    // Visit Body not Children
    doStatement.getBody().accept(this);

    return SKIP_CHILDS;
  }


  @Override
  public void endVisit(WhileStatement whileStatement) {
    handleLeaveWhileLoop();
  }

  @Override
  public void endVisit(DoStatement doStatement) {
    handleLeaveWhileLoop();
  }

  private void handleLeaveWhileLoop() {
    CFANode prevNode = locStack.pop();
    CFANode startNode = loopStartStack.pop();

    if (isReachableNode(prevNode)) {
      BlankEdge blankEdge = new BlankEdge("", FileLocation.DUMMY,
          prevNode, startNode, "");
      addToCFA(blankEdge);
    }
    CFANode nextNode = loopNextStack.pop();
    assert nextNode == locStack.peek();
  }


  @Override
  public boolean visit(EnhancedForStatement forStatement) {

    scope.enterBlock();

    handleElseCondition(forStatement);

    final FileLocation fileloc = astCreator.getFileLocation(forStatement);

    // Declare Formal Parameter for Loop
    forStatement.getParameter().accept(this);

    final CFANode prevNode = locStack.pop();

    // loopInit is Node before the Iterator
    final CFANode loopInit = new CFANode(cfa.getFunctionName());
    cfaNodes.add(loopInit);
    addToCFA(new BlankEdge("", fileloc, prevNode, loopInit, "enhanced for"));

    Expression iterable = forStatement.getExpression();

    // loopStartNodes is the Node before the loop itself,
    // it is the the one after the iterator
    final CFANode loopStart =
        createIteratorEdgeForEnhancedForLoop(iterable, fileloc, loopInit);
    loopStart.setLoopStart();

    // firstLoopNode is Node after "it.hasNext()"
    final CFANode firstLoopNode =
        new CFANode(cfa.getFunctionName());
    cfaNodes.add(firstLoopNode);

    // postLoopNode is Node after "!(it.hasNext())"
    final CFANode postLoopNode =
        new CFANode(cfa.getFunctionName());
    cfaNodes.add(postLoopNode);
    loopNextStack.push(postLoopNode);

    // inverse order here!
    locStack.push(postLoopNode);
    locStack.push(firstLoopNode);

    JExpression condition = astCreator.createIteratorCondition(forStatement.getExpression());

    createConditionEdgesForForLoop(condition,
        fileloc, loopStart, postLoopNode, firstLoopNode);

    // last node in loop
    final CFANode lastNodeInLoop =
        new CFANode(cfa.getFunctionName());
    cfaNodes.add(lastNodeInLoop);

    assignFormalParameterForLoop(forStatement.getParameter(), fileloc);

    // visit only loop body, not children
    forStatement.getBody().accept(this);

    // leave loop
    final CFANode prev = locStack.pop();

    final BlankEdge blankEdge = new BlankEdge("",
        fileloc, prev, lastNodeInLoop, "");
    addToCFA(blankEdge);

    // create Edge to Beginning of Loop (Enhanced For lacks update )
    final BlankEdge loopEndToStart = new BlankEdge("",
        fileloc, lastNodeInLoop, loopStart, "");
    addToCFA(loopEndToStart);

    assert postLoopNode == loopNextStack.pop();
    assert postLoopNode == locStack.peek();

    scope.leaveBlock();

    // skip visiting children of loop, because loopbody was handled before
    return SKIP_CHILDS;
  }

  private void assignFormalParameterForLoop(SingleVariableDeclaration parameter, FileLocation fileLocation) {

    CFANode prevNode = locStack.poll();

    JStatement assignment = astCreator.assignParameterToNextIteratorItem(parameter);

    CFANode nextNode = new CFANode(cfa.getFunctionName());
    cfaNodes.add(nextNode);

    final JStatementEdge edge = new JStatementEdge(assignment.toASTString(),
        assignment, fileLocation, prevNode, nextNode);
   addToCFA(edge);

   locStack.push(nextNode);

  }

  private CFANode createIteratorEdgeForEnhancedForLoop(Expression expr, FileLocation fileLocation, CFANode loopInit) {

    CFANode prevNode = loopInit;

    JMethodInvocationAssignmentStatement assignment = astCreator.getIteratorFromIterable(expr);

    prevNode = handleSideassignments(prevNode, "", fileLocation);

    CFANode nextNode = new CFANode(cfa.getFunctionName());
    cfaNodes.add(nextNode);

    final JStatementEdge edge = new JStatementEdge("", assignment, fileLocation, prevNode, nextNode);
    addToCFA(edge);

    return nextNode;
  }

  @Override
  public boolean visit(final ForStatement forStatement) {

    scope.enterBlock();

    handleElseCondition(forStatement);

    final FileLocation fileloc = astCreator.getFileLocation(forStatement);

    final CFANode prevNode = locStack.pop();

    // loopInit is Node before "counter = 0;"
    final CFANode loopInit = new CFANode(cfa.getFunctionName());
    cfaNodes.add(loopInit);
    addToCFA(new BlankEdge("", fileloc, prevNode, loopInit, "for"));

    // loopStartNodes is the Node before the loop itself,
    // it is the the one after the init edge(s)
    @SuppressWarnings("unchecked")
    List<Expression> iniBlock = forStatement.initializers();

    final CFANode loopStart =
        createInitEdgeForForLoop(iniBlock, fileloc, loopInit);
    loopStart.setLoopStart();

    // firstLoopNode is Node after "counter < 5"
    final CFANode firstLoopNode = new CFANode(cfa.getFunctionName());
    cfaNodes.add(firstLoopNode);

    // postLoopNode is Node after "!(counter < 5)"
    final CFANode postLoopNode = new CFANode(cfa.getFunctionName());
    cfaNodes.add(postLoopNode);
    loopNextStack.push(postLoopNode);

    // inverse order here!
    locStack.push(postLoopNode);
    locStack.push(firstLoopNode);

    Expression condition = forStatement.getExpression();

    createConditionEdgesForForLoop(condition,
        fileloc, loopStart, postLoopNode, firstLoopNode);

    // Node before Update "counter++"
    final CFANode lastNodeInLoop = new CFANode(cfa.getFunctionName());
    cfaNodes.add(lastNodeInLoop);

    loopStartStack.push(lastNodeInLoop);


    // visit only loop body, not children
    forStatement.getBody().accept(this);

    // leave loop
    final CFANode prev = locStack.pop();

    final BlankEdge blankEdge = new BlankEdge("",
        fileloc, prev, lastNodeInLoop, "");
    addToCFA(blankEdge);

    // loopEnd is end of Loop after "counter++;"
    @SuppressWarnings("unchecked")
    List<Expression> updateBlock = forStatement.updaters();

    createLastNodesAndEdgeForForLoop(updateBlock, fileloc,
                                        lastNodeInLoop, loopStart);

    assert lastNodeInLoop == loopStartStack.pop();
    assert postLoopNode == loopNextStack.pop();
    assert postLoopNode == locStack.peek();

    scope.leaveBlock();

    // skip visiting children of loop, because loopbody was handled before
    return SKIP_CHILDS;
  }


  private void createConditionEdgesForForLoop(
      Expression condition, FileLocation fileLocation, CFANode loopStart,
                    CFANode postLoopNode, CFANode firstLoopNode) {

    if (condition == null) {
      // no condition -> only a blankEdge from loopStart to firstLoopNode
      final BlankEdge blankEdge = new BlankEdge("", fileLocation, loopStart,
          firstLoopNode, "");
      addToCFA(blankEdge);

    } else {
      createConditionEdges(condition, fileLocation, loopStart, firstLoopNode,
          postLoopNode);
    }
  }

  private void createConditionEdgesForForLoop(
      JExpression condition, FileLocation fileLocation, CFANode loopStart,
                    CFANode postLoopNode, CFANode firstLoopNode) {

    if (condition == null) {
      // no condition -> only a blankEdge from loopStart to firstLoopNode
      final BlankEdge blankEdge = new BlankEdge("", fileLocation, loopStart,
          firstLoopNode, "");
      addToCFA(blankEdge);

    } else {
      createConditionEdges(condition, fileLocation, loopStart, firstLoopNode,
          postLoopNode);
    }
  }

  private void createLastNodesAndEdgeForForLoop(List<Expression> updaters,
                        FileLocation fileLocation, CFANode loopEnd, CFANode loopStart) {

    int size = updaters.size();

    if (size == 0) {
      // no update

      final BlankEdge blankEdge = new BlankEdge("",
          fileLocation, loopEnd, loopStart, "");
      addToCFA(blankEdge);


    } else {

      CFANode prevNode = loopEnd;
      CFANode nextNode = null;

      for (Expression exp : updaters) {

        final JAstNode node = astCreator.convertExpressionWithSideEffects(exp);

        // If last Expression, use last loop Node

        nextNode = new CFANode(cfa.getFunctionName());
        cfaNodes.add(nextNode);


        if (node instanceof JIdExpression) {
          final BlankEdge blankEdge = new BlankEdge(node.toASTString(),
              fileLocation, prevNode, nextNode, "");
          addToCFA(blankEdge);

          // "counter++;"
        } else if (node instanceof JExpressionAssignmentStatement) {

          final JStatementEdge lastEdge = new JStatementEdge(exp.toString(),
              (JExpressionAssignmentStatement) node,
              fileLocation, prevNode, nextNode);
          addToCFA(lastEdge);


        } else if (node instanceof JMethodInvocationAssignmentStatement) {

          final JStatementEdge edge = new JStatementEdge(exp.toString(),
              (JMethodInvocationAssignmentStatement) node,
              fileLocation, prevNode, nextNode);
          addToCFA(edge);

        } else { // TODO: are there other iteration-expressions in a for-loop?

          throw new AssertionError(
              "CFABuilder: unknown iteration-expressions in for-statement:\n"
                                                              + exp.getClass());
        }
      }

      final BlankEdge blankEdge = new BlankEdge("",
          fileLocation, nextNode, loopStart, "");
      addToCFA(blankEdge);

    }
  }


  private CFANode createInitEdgeForForLoop(List<Expression> initializers, FileLocation fileLocation, CFANode loopInit) {

    CFANode nextNode = loopInit;

    // counter indicating current element
    for (Expression exp : initializers) {

      final JAstNode node = astCreator.convertExpressionWithSideEffects(exp);




      if (node == null && astCreator.numberOfForInitDeclarations() > 0) {


        List<JDeclaration> initBlock = astCreator.getForInitDeclaration();

        nextNode = addDeclarationsToCFA(initBlock, fileLocation,
            initializers.toString(), nextNode);

        astCreator.getForInitDeclaration().clear();

      } else if (node instanceof JIdExpression) {


        nextNode = new CFANode(cfa.getFunctionName());
        cfaNodes.add(nextNode);


        final BlankEdge blankEdge = new BlankEdge(node.toASTString(),
            fileLocation, loopInit, nextNode, "");
        addToCFA(blankEdge);

      } else if (node instanceof JExpressionAssignmentStatement) {


        nextNode = new CFANode(cfa.getFunctionName());
        cfaNodes.add(nextNode);


        final JStatementEdge lastEdge = new JStatementEdge(exp.toString(),
            (JExpressionAssignmentStatement) node, fileLocation, loopInit, nextNode);
        addToCFA(lastEdge);


      } else if (node instanceof JMethodInvocationAssignmentStatement) {


        nextNode = new CFANode(cfa.getFunctionName());
        cfaNodes.add(nextNode);


        final JStatementEdge edge = new JStatementEdge(exp.toString(),
            (JMethodInvocationAssignmentStatement) node, fileLocation, loopInit, nextNode);
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

    if (breakStatement.getLabel() == null) {
      handleBreakStatement(breakStatement);
    } else {
      handleLabeledBreakStatement(breakStatement);
    }

    return SKIP_CHILDS;
  }


  private void handleBreakStatement(BreakStatement breakStatement) {


    FileLocation fileloc = astCreator.getFileLocation(breakStatement);
    CFANode prevNode = locStack.pop();
    CFANode postLoopNode = loopNextStack.peek();


    BlankEdge blankEdge = new BlankEdge(breakStatement.toString(),
        fileloc, prevNode, postLoopNode, "break");
    addToCFA(blankEdge);

    CFANode nextNode = new CFANode(cfa.getFunctionName());
    cfaNodes.add(nextNode);
    locStack.push(nextNode);

  }


  private void handleLabeledBreakStatement(BreakStatement breakStatement) {

    FileLocation fileloc = astCreator.getFileLocation(breakStatement);
    CFANode prevNode = locStack.pop();
    CFANode postLoopNode = labelMap.get(breakStatement.getLabel().getIdentifier());


    BlankEdge blankEdge = new BlankEdge(breakStatement.toString(),
        fileloc, prevNode, postLoopNode, "break ");
    addToCFA(blankEdge);

    CFANode nextNode = new CFANode(cfa.getFunctionName());
    cfaNodes.add(nextNode);
    locStack.push(nextNode);
  }


  @Override
  public boolean visit(ContinueStatement continueStatement) {

    handleElseCondition(continueStatement);

    if (continueStatement.getLabel() == null) {
      handleContinueStatement(continueStatement);
    } else {
      registerLabledContinueStatement(continueStatement);
    }

    return SKIP_CHILDS;
  }

  private void handleLabledContinueStatement(ContinueStatement continueStatement, CFANode prevNode, CFANode startLoopNode) {

    FileLocation fileloc = astCreator.getFileLocation(continueStatement);

    BlankEdge blankEdge = new BlankEdge(continueStatement.toString(),
        fileloc, prevNode, startLoopNode, "continue ");
    addToCFA(blankEdge);

  }

  private void registerLabledContinueStatement(ContinueStatement continueStatement) {

    CFANode prevNode = locStack.pop();

     List<Pair<CFANode, ContinueStatement>> prevNodeList = registeredContinues.get(
               continueStatement.getLabel().getIdentifier());

     if (prevNodeList == null) {
       throw new CFAGenerationRuntimeException(
           "Could not Find Loop of Label Name "
       + continueStatement.getLabel().getIdentifier());
     }

     prevNodeList.add(Pair.of(prevNode, continueStatement));

    CFANode nextNode = new CFANode(cfa.getFunctionName());
    cfaNodes.add(nextNode);
    locStack.push(nextNode);
  }

  private void handleContinueStatement(ContinueStatement continueStatement) {

    FileLocation fileloc = astCreator.getFileLocation(continueStatement);

    CFANode prevNode = locStack.pop();

    // TODO Does not apply to a for-loop
    CFANode loopStartNode = loopStartStack.peek();


    BlankEdge blankEdge = new BlankEdge(continueStatement.toString(),
        fileloc, prevNode, loopStartNode, "continue");
    addToCFA(blankEdge);

    CFANode nextNode = new CFANode(cfa.getFunctionName());
    locStack.push(nextNode);
  }




  @Override
  public boolean visit(ReturnStatement returnStatement) {

    FileLocation fileloc = astCreator.getFileLocation(returnStatement);

    CFANode prevNode = locStack.pop();

    CFANode nextNode = new CFANode(cfa.getFunctionName());
    cfaNodes.add(nextNode);


    FunctionExitNode functionExitNode = cfa.getExitNode();

    JReturnStatement cfJReturnStatement = astCreator.convert(returnStatement);

    // If return expression is function
    prevNode = handleSideassignments(prevNode, returnStatement.toString(), fileloc);

    // TODO After Assignments within Expressions are supported, change this
    if (astCreator.getConditionalExpression() != null) {
      astCreator.resetConditionalExpression();
    }

    JReturnStatementEdge edge = new JReturnStatementEdge(returnStatement.toString(),
        cfJReturnStatement, fileloc, prevNode, functionExitNode);
    addToCFA(edge);

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

  public void createDefaultConstructor(ITypeBinding classBinding) {

    if (locStack.size() != 0) {
      throw new CFAGenerationRuntimeException("Nested function declarations?");
      }

    assert cfa == null;

    final JMethodDeclaration fdef = astCreator.createDefaultConstructor(classBinding);
    handleMethodDeclaration(fdef);
    addNonStaticFieldMember();

    //TODO insertFileOfType
    handleReturnFromObject(
        FileLocation.DUMMY, classBinding.getName(), classBinding);
    handleEndVisitMethodDeclaration();

  }
}