/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTASMDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTBreakStatement;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTContinueStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTGotoStatement;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTLabelStatement;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTNullStatement;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTProblem;
import org.eclipse.cdt.core.dom.ast.IASTProblemDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTProblemStatement;
import org.eclipse.cdt.core.dom.ast.IASTReturnStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStandardFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.objectmodel.BlankEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionExitNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.GlobalDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.ReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.exceptions.CFAGenerationRuntimeException;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Builder to traverse AST.
 * @author erkan
 * Known Limitations:
 * <p> -- K&R style function definitions not implemented
 * <p> -- Pointer modifiers not tracked (i.e. const, volatile, etc. for *
 */
public class CFABuilder extends ASTVisitor
{
  // Data structure for maintaining our scope stack in a function
  private final Deque<CFANode> locStack = new ArrayDeque<CFANode>();

  // Data structures for handling loops & else conditions
  private final Deque<CFANode> loopStartStack = new ArrayDeque<CFANode>();
  private final Deque<CFANode> loopNextStack  = new ArrayDeque<CFANode>(); // For the node following the current if / while block
  private final Deque<CFANode> elseStack      = new ArrayDeque<CFANode>();

  // Data structures for handling goto
  private final Map<String, CFALabelNode> labelMap = new HashMap<String, CFALabelNode>();
  private final Multimap<String, CFANode> gotoLabelNeeded = ArrayListMultimap.create();

  // Data structures for handling function declarations
  private final Map<String, CFAFunctionDefinitionNode> cfas = new HashMap<String, CFAFunctionDefinitionNode>();
  private CFAFunctionDefinitionNode currentCFA = null;

  // Data structure for storing global declarations
  private final List<IASTDeclaration> globalDeclarations = new ArrayList<IASTDeclaration>();

  private final LogManager logger;

  public CFABuilder (LogManager logger) {
    this.logger = logger;

    //shouldVisitComments = false;
    shouldVisitDeclarations = true;
    shouldVisitDeclarators = false;
    shouldVisitDeclSpecifiers = false;
    shouldVisitEnumerators = true;
    shouldVisitExpressions = false;
    shouldVisitInitializers = false;
    shouldVisitNames = false;
    shouldVisitParameterDeclarations = true;
    shouldVisitProblems = true;
    shouldVisitStatements = true;
    shouldVisitTranslationUnit = false;
    shouldVisitTypeIds = false;
  }

  /**
   * Retrieves list of all functions
   * @return all CFAs in the program
   */
  public Map<String, CFAFunctionDefinitionNode> getCFAs()  {
    return cfas;
  }

  /**
   * Retrieves list of all global declarations
   * @return global declarations
   */
  public List<IASTDeclaration> getGlobalDeclarations ()
  {
    return globalDeclarations;
  }

  /* (non-Javadoc)
   * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#visit(org.eclipse.cdt.core.dom.ast.IASTDeclaration)
   */
  @Override
  public int visit (IASTDeclaration declaration)
  {
    IASTFileLocation fileloc = declaration.getFileLocation ();

    if (declaration instanceof IASTSimpleDeclaration)
    {
      if (locStack.size () > 0) // i.e. we're in a function
      {
        CFANode prevNode = locStack.pop ();
        CFANode nextNode = new CFANode(fileloc.getStartingLineNumber(), currentCFA.getFunctionName());

        DeclarationEdge edge = new DeclarationEdge ((IASTSimpleDeclaration) declaration,
                fileloc.getStartingLineNumber(), prevNode, nextNode);
        addToCFA(edge);

        locStack.push (nextNode);
      }
      else if (declaration.getParent()
              instanceof IASTTranslationUnit)
      {
              // else we're in the global scope
        globalDeclarations.add (declaration);
      }
    }
    else if (declaration instanceof IASTFunctionDefinition)
    {
      if (locStack.size () != 0)
        throw new CFAGenerationRuntimeException ("Nested function declarations?");

      assert labelMap.isEmpty();
      assert gotoLabelNeeded.isEmpty();
      assert currentCFA == null;
      
      IASTFunctionDefinition fdef = (IASTFunctionDefinition) declaration;
      String nameOfFunction = fdef.getDeclarator().getName().toString();

      IASTFunctionDeclarator decl = fdef.getDeclarator();
      if (!(decl instanceof IASTStandardFunctionDeclarator)) {
        throw new CFAGenerationRuntimeException("Unknown non-standard function definition", decl);
      }
  
      IASTParameterDeclaration[] params = ((IASTStandardFunctionDeclarator)decl).getParameters();
      List<IASTParameterDeclaration> parameters = new ArrayList<IASTParameterDeclaration>(params.length);
      List<String> parameterNames = new ArrayList<String>(params.length);
      
      for (IASTParameterDeclaration param : params) {
        String name = param.getDeclarator().getName().toString();

        if (name.isEmpty() &&
            param.getDeclarator().getNestedDeclarator() != null) {
          name = param.getDeclarator().getNestedDeclarator().getName().toString();
        }

        // function may have the parameter "void", so we need this check
        if (!name.isEmpty()) {
          parameters.add(param);
          parameterNames.add(name);
        }
      }
      
      CFAFunctionExitNode returnNode = new CFAFunctionExitNode(fileloc.getEndingLineNumber(), nameOfFunction);

      currentCFA = new FunctionDefinitionNode(fileloc.getStartingLineNumber(), fdef, returnNode, parameters, parameterNames);

      CFANode functionStartDummyNode = new CFANode(fileloc.getStartingLineNumber(), currentCFA.getFunctionName());
      BlankEdge dummyEdge = new BlankEdge("Function start dummy edge", fileloc.getStartingLineNumber(), currentCFA, functionStartDummyNode);
      addToCFA(dummyEdge);

      locStack.add (functionStartDummyNode);
      cfas.put(nameOfFunction, currentCFA);

    } else if (declaration instanceof IASTProblemDeclaration) {
      // CDT parser struggles on GCC's __attribute__((something)) constructs because we use C99 as default
      // Either insert the following macro before compiling with CIL:
      // #define  __attribute__(x)  /*NOTHING*/
      // or insert "parser.dialect = GNUC" into properties file
      visit(((IASTProblemDeclaration)declaration).getProblem());

    } else if (declaration instanceof IASTASMDeclaration) {
      // TODO Assembler code is ignored here
      logger.log(Level.WARNING, "Ignoring inline assembler code at line " + fileloc.getStartingLineNumber() + ", analysis is probably unsound!");

      // locStack may be empty here, which happens when there is assembler code
      // outside of a function
      if (!locStack.isEmpty()) {
        CFANode prevNode = locStack.pop();
        CFANode nextNode = new CFANode(fileloc.getStartingLineNumber(), currentCFA.getFunctionName());
  
        BlankEdge edge = new BlankEdge("Ignored inline assembler code", fileloc.getStartingLineNumber(), prevNode, nextNode);
        addToCFA(edge);

        locStack.push(nextNode);
      }

    } else {
      throw new CFAGenerationRuntimeException("Unknown declaration type " + declaration.getClass().getSimpleName(),  declaration);
    }

    return PROCESS_CONTINUE;
  }

  @Override
  public int leave (IASTDeclaration declaration)
  {
    if (declaration instanceof IASTFunctionDefinition)
    {
      if (locStack.size () != 1)
        throw new CFAGenerationRuntimeException ("Depth wrong. Geoff needs to do more work");

      CFANode lastNode = locStack.pop ();

      if (!lastNode.hasJumpEdgeLeaving ())
      {
        BlankEdge blankEdge = new BlankEdge ("default return", lastNode.getLineNumber(), lastNode, currentCFA.getExitNode());
        addToCFA(blankEdge);
      }

      if (!gotoLabelNeeded.isEmpty()) {
        throw new CFAGenerationRuntimeException("Following labels were not found in function " + currentCFA.getFunctionName() + ": " + gotoLabelNeeded.keySet());
      }
      
      for (CFALabelNode n : labelMap.values()) {
        if (n.getNumEnteringEdges() == 0) {
          logger.log(Level.INFO, "Dead code detected at line", n.getLineNumber() + ": Label", n.getLabel(), "is not reachable.");
          
          // remove this dead code from CFA
          removeChainOfNodesFromCFA(n);
        }
      }
      
      labelMap.clear();
      
      currentCFA = null;
    }

    return PROCESS_CONTINUE;
  }

  /**
   * Remove nodes from the CFA beginning at a certain node n until there is a node
   * that is reachable via some other path (not going through n).
   * Useful for eliminating dead node, if node n is not reachable.
   */
	static void removeChainOfNodesFromCFA(CFANode n) {
	  if (n.getNumEnteringEdges() > 0) {
	    return;
	  }
	  
	  for (int i = n.getNumLeavingEdges()-1; i >= 0; i--) {
	    CFAEdge e = n.getLeavingEdge(i);
	    CFANode succ = e.getSuccessor();
	    
	    n.removeLeavingEdge(e);
	    succ.removeEnteringEdge(e);
	    removeChainOfNodesFromCFA(succ);
	  }
	}
	
  // Methods for to handle visiting and leaving Statements
  /* (non-Javadoc)
   * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#visit(org.eclipse.cdt.core.dom.ast.IASTStatement)
   */
  @Override
  public int visit (IASTStatement statement)
  {
    IASTFileLocation fileloc = statement.getFileLocation ();

    // Handle special condition for else
    if (statement.getPropertyInParent () == IASTIfStatement.ELSE)
    {
      // Edge from current location to post if-statement location
      CFANode prevNode = locStack.pop ();
      CFANode elseNode = elseStack.pop ();
      if (!prevNode.hasJumpEdgeLeaving ())
      {
          CFANode nextNode = locStack.peek ();

          BlankEdge blankEdge = new BlankEdge ("", nextNode.getLineNumber(), prevNode, nextNode);
          addToCFA(blankEdge);
      }

      //  Push the start of the else clause onto our location stack
      locStack.push (elseNode);
    }

    // Handle each kind of expression
    if (statement instanceof IASTCompoundStatement)
    {
      // Do nothing, just continue visiting
    }
    else if (statement instanceof IASTExpressionStatement)
      handleExpressionStatement ((IASTExpressionStatement)statement, fileloc);
    else if (statement instanceof IASTIfStatement)
      handleIfStatement ((IASTIfStatement)statement, fileloc);
    else if (statement instanceof IASTWhileStatement)
      handleWhileStatement ((IASTWhileStatement)statement, fileloc);
    else if (statement instanceof IASTBreakStatement)
      handleBreakStatement ((IASTBreakStatement)statement, fileloc);
    else if (statement instanceof IASTContinueStatement)
      handleContinueStatement ((IASTContinueStatement)statement, fileloc);
    else if (statement instanceof IASTLabelStatement)
      handleLabelStatement ((IASTLabelStatement)statement, fileloc);
    else if (statement instanceof IASTGotoStatement)
      handleGotoStatement ((IASTGotoStatement)statement, fileloc);
    else if (statement instanceof IASTReturnStatement)
      handleReturnStatement ((IASTReturnStatement)statement, fileloc);
/* switch statements are removed by CIL
    else if (statement instanceof IASTSwitchStatement)
      handleSwitchStatement ((IASTSwitchStatement)statement, fileloc);
    else if (statement instanceof IASTCaseStatement)
      handleCaseStatement ((IASTCaseStatement)statement, fileloc);
    else if (statement instanceof IASTDefaultStatement)
      handleDefaultStatement ((IASTDefaultStatement)statement, fileloc);
*/
    else if (statement instanceof IASTNullStatement)
    {
      // We really don't care about blank statements
    }
    else if (statement instanceof IASTDeclarationStatement)
    {
      // TODO: I think we can ignore these here...
    } else if (statement instanceof IASTProblemStatement) {
      visit(((IASTProblemStatement)statement).getProblem());
    }
    else {
      throw new CFAGenerationRuntimeException("Unknown AST node " + statement.getClass().getSimpleName() + " in line " + fileloc.getStartingLineNumber() + ": " + statement.getRawSignature());
    }

    return PROCESS_CONTINUE;
  }

  private void handleExpressionStatement (IASTExpressionStatement exprStatement, IASTFileLocation fileloc)
  {
    CFANode prevNode = locStack.pop ();
    CFANode nextNode = new CFANode(fileloc.getStartingLineNumber(), currentCFA.getFunctionName());

    StatementEdge edge = new StatementEdge(exprStatement, fileloc.getStartingLineNumber(), prevNode, nextNode, exprStatement.getExpression());
    addToCFA(edge);

    locStack.push (nextNode);
  }

  private static enum IF_CONDITION {  NORMAL, ALWAYS_FALSE, ALWAYS_TRUE };

  private IF_CONDITION getIfConditionKind(IASTIfStatement ifStatement) {
      IASTExpression cond = ifStatement.getConditionExpression();
      if (cond instanceof IASTLiteralExpression) {
          if (((IASTLiteralExpression)cond).getKind() ==
              IASTLiteralExpression.lk_integer_constant) {
              int c = Integer.parseInt(cond.getRawSignature());
              if (c == 0) return IF_CONDITION.ALWAYS_FALSE;
              else return IF_CONDITION.ALWAYS_TRUE;
          }
      }
      return IF_CONDITION.NORMAL;
  }

  private void handleIfStatement (IASTIfStatement ifStatement, IASTFileLocation fileloc)
  {
    CFANode prevNode = locStack.pop ();
    CFANode postIfNode = new CFANode(fileloc.getEndingLineNumber(), currentCFA.getFunctionName());

    locStack.push (postIfNode);

    IF_CONDITION kind = getIfConditionKind(ifStatement);

    switch (kind) {
    case ALWAYS_FALSE: {
        if (ifStatement.getElseClause() == null) {
            CFANode ifNode = new CFANode(fileloc.getStartingLineNumber(), currentCFA.getFunctionName());
            BlankEdge edge = new BlankEdge("", fileloc.getStartingLineNumber(), prevNode, postIfNode);
            addToCFA(edge);
            locStack.push(ifNode);
        } else {
            CFANode elseNode =
                new CFANode(fileloc.getStartingLineNumber(), currentCFA.getFunctionName());
            BlankEdge edge = new BlankEdge("", fileloc.getStartingLineNumber(), prevNode, elseNode);
            addToCFA(edge);
            elseStack.push(elseNode);
            CFANode n = new CFANode(-1, currentCFA.getFunctionName());
            locStack.push(n);
        }
    }
        break;
    case ALWAYS_TRUE: {
        CFANode thenNode =
            new CFANode(fileloc.getStartingLineNumber(), currentCFA.getFunctionName());
        BlankEdge edge = new BlankEdge("", fileloc.getStartingLineNumber(), prevNode, thenNode);
        addToCFA(edge);
        locStack.push(thenNode);
        if (ifStatement.getElseClause() != null) {
            CFANode n = new CFANode(-1, currentCFA.getFunctionName());
            elseStack.push(n);
        }
    }
        break;
    case NORMAL: {
        CFANode ifStartTrue = new CFANode(fileloc.getStartingLineNumber(), currentCFA.getFunctionName());
        AssumeEdge assumeEdgeTrue = new AssumeEdge (ifStatement.getConditionExpression ().getRawSignature (),
                fileloc.getStartingLineNumber(), prevNode, ifStartTrue,
                ifStatement.getConditionExpression (),
                true);

        addToCFA(assumeEdgeTrue);
        locStack.push (ifStartTrue);

        if (ifStatement.getElseClause () != null) {
            CFANode ifStartFalse = new CFANode(fileloc.getStartingLineNumber(), currentCFA.getFunctionName());
            AssumeEdge assumeEdgeFalse = new AssumeEdge ("!(" + ifStatement.getConditionExpression ().getRawSignature () + ")",
                    fileloc.getStartingLineNumber(), prevNode, ifStartFalse,
                    ifStatement.getConditionExpression (),
                    false);

            addToCFA(assumeEdgeFalse);
            elseStack.push (ifStartFalse);
        } else {
            AssumeEdge assumeEdgeFalse = new AssumeEdge ("!(" + ifStatement.getConditionExpression ().getRawSignature () + ")",
                    fileloc.getStartingLineNumber(), prevNode, postIfNode,
                    ifStatement.getConditionExpression (),
                    false);

            addToCFA(assumeEdgeFalse);
        }
    } // end of IF_CONDITION_NORMAL case
        break;
    default:
        throw new InternalError("Missing switch clause");
    } // end of switch statement
  }

  private void handleWhileStatement (IASTWhileStatement whileStatement, IASTFileLocation fileloc)
  {
    CFANode prevNode = locStack.pop ();
    CFANode loopStart = new CFANode(fileloc.getStartingLineNumber(), currentCFA.getFunctionName());
    loopStart.setLoopStart();

    CFANode postLoopNode = new CFANode(fileloc.getEndingLineNumber(), currentCFA.getFunctionName());
    locStack.push (postLoopNode);
    locStack.push (loopStart);

    loopStartStack.push (loopStart);
    loopNextStack.push (postLoopNode);

    BlankEdge blankEdge = new BlankEdge("while", fileloc.getStartingLineNumber(), prevNode, loopStart);
    addToCFA(blankEdge);
  }

  private void handleBreakStatement (IASTBreakStatement breakStatement, IASTFileLocation fileloc)
  {
    CFANode prevNode = locStack.pop();
    CFANode nextNode = loopNextStack.peek ();

    BlankEdge blankEdge = new BlankEdge(breakStatement.getRawSignature(), fileloc.getStartingLineNumber(), prevNode, nextNode, true);
    addToCFA(blankEdge);
    
    locStack.push(new CFANode(fileloc.getEndingLineNumber(), currentCFA.getFunctionName()));
  }

  private void handleContinueStatement (IASTContinueStatement continueStatement, IASTFileLocation fileloc)
  {
    CFANode prevNode = locStack.pop();
    CFANode loopStart = loopStartStack.peek ();

    BlankEdge blankEdge = new BlankEdge(continueStatement.getRawSignature(), fileloc.getStartingLineNumber(), prevNode, loopStart, true);
    addToCFA(blankEdge);
    
    locStack.push(new CFANode(fileloc.getEndingLineNumber(), currentCFA.getFunctionName()));
  }

  private void handleLabelStatement (IASTLabelStatement labelStatement, IASTFileLocation fileloc)
  {
    String labelName = labelStatement.getName ().toString ();

    CFALabelNode labelNode = new CFALabelNode(fileloc.getStartingLineNumber(), currentCFA.getFunctionName(), labelName);

    CFANode prevNode = locStack.pop ();

    BlankEdge blankEdge = new BlankEdge("Label: " + labelName, fileloc.getStartingLineNumber(), prevNode, labelNode);
    addToCFA(blankEdge);

    locStack.push (labelNode);

    if (labelMap.containsKey(labelName)) {
      throw new CFAGenerationRuntimeException("Duplicate label " + labelName + " in function " + currentCFA.getFunctionName(), labelStatement);
    }
    labelMap.put (labelName, labelNode);

    // Check if any goto's previously analyzed need connections to this label
    for (CFANode gotoNode : gotoLabelNeeded.get(labelName)) {
      BlankEdge gotoEdge = new BlankEdge("Goto: " + labelName, gotoNode.getLineNumber(), gotoNode, labelNode, true);
      addToCFA(gotoEdge);
    }
    gotoLabelNeeded.removeAll(labelName);
  }

  private void handleGotoStatement (IASTGotoStatement gotoStatement, 
      IASTFileLocation fileloc)
  {
    String labelName = gotoStatement.getName ().toString ();

    CFANode prevNode = locStack.pop();
    CFANode labelNode = labelMap.get (labelName);
    if (labelNode != null)
    {
      BlankEdge gotoEdge = new BlankEdge("Goto: " 
          + labelName, fileloc.getStartingLineNumber(), prevNode, labelNode, true);
      
      /* labelNode was analyzed before, so it is in the labelMap, 
       * then there can be a jump backwards and this can create a loop.
       * If LabelNode has not been the start of a loop, Node labelNode can be 
       * the start of a loop, so check if there is a path from labelNode to 
       * the current Node through DFS-search */
      ArrayList<Integer> visitedNodes = new ArrayList<Integer>();
      if (!labelNode.isLoopStart() 
          && isPathFromTo(visitedNodes, labelNode, prevNode)) {
        labelNode.setLoopStart();
      }

      addToCFA(gotoEdge);
    } else {
      gotoLabelNeeded.put(labelName, prevNode);
    }
    locStack.push(new CFANode(fileloc.getEndingLineNumber(), currentCFA.getFunctionName()));
  }

  /** isPathFromTo() makes a DSF-search from a given Node to search 
   * if there is a way to the target Node. 
   * the condition for this function is, that every Node has another NodeNumber
   * 
   * The code for this function was taken from CFATopologicalSort.java and is modified.
   * 
   * @param pVisitedNodes needed for DSF-search
   * @param fromNode starting node for DSF-search
   * @param toNode target node for isPath
   */
  private boolean isPathFromTo(ArrayList<Integer> pVisitedNodes,
      CFANode fromNode, CFANode toNode) {

    // add current node to visited nodes
    pVisitedNodes.add(fromNode.getNodeNumber());
    boolean isPath = false;

    // check if the target is reached
    if (fromNode.getNodeNumber() == toNode.getNodeNumber()) {
      isPath = true;

    } else {
      // BSF-search with the children of current node
      for (int i = 0; i < fromNode.getNumLeavingEdges(); i++) {
        CFANode successor = fromNode.getLeavingEdge(i).getSuccessor();
        if (!pVisitedNodes.contains(successor.getNodeNumber())) {
          isPath = isPathFromTo(pVisitedNodes, successor, toNode);
        }
        // if there is a path, break the search and return isPath (=true)
        if (isPath) {
          break;
        }
      }
    }
    return isPath;
  }
  
  private void handleReturnStatement (IASTReturnStatement returnStatement, IASTFileLocation fileloc)
  {
    CFANode prevNode = locStack.pop ();
    CFAFunctionExitNode nextNode = currentCFA.getExitNode();

    ReturnStatementEdge edge = new ReturnStatementEdge(returnStatement, fileloc.getStartingLineNumber(), prevNode, nextNode, returnStatement.getReturnValue());
    addToCFA(edge);

    locStack.push(new CFANode(fileloc.getEndingLineNumber(), currentCFA.getFunctionName()));
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#leave(org.eclipse.cdt.core.dom.ast.IASTStatement)
   */
  @Override
  public int leave (IASTStatement statement)
  {
    if (statement instanceof IASTIfStatement)
    {
      CFANode prevNode = locStack.pop ();

      if (!prevNode.hasJumpEdgeLeaving ())
      {
        CFANode nextNode = locStack.peek ();

        BlankEdge blankEdge = new BlankEdge("", prevNode.getLineNumber(), prevNode, nextNode);
        addToCFA(blankEdge);
      }
    }
    else if ((statement instanceof IASTCompoundStatement) && (statement.getPropertyInParent () == IASTWhileStatement.BODY))
    {
      CFANode prevNode = locStack.pop ();

      if (!prevNode.hasJumpEdgeLeaving ())
      {
        CFANode startNode = loopStartStack.peek ();

        if (!prevNode.hasEdgeTo (startNode))
        {
          BlankEdge blankEdge = new BlankEdge("", prevNode.getLineNumber(), prevNode, startNode);
          addToCFA(blankEdge);
        }
      }
      loopStartStack.pop ();
      loopNextStack.pop ();
    }
    else if (statement instanceof IASTWhileStatement) // Code never hit due to bug in Eclipse CDT
    {
      /* Commented out, because with CDT 6, the branch above _and_ this branch
       * are hit, which would result in an exception.
      CFANode prevNode = locStack.pop ();

      if (!prevNode.hasJumpEdgeLeaving ())
      {
        CFANode startNode = loopStartStack.peek ();

        if (!prevNode.hasEdgeTo (startNode))
        {
          BlankEdge blankEdge = new BlankEdge ("");
          blankEdge.initialize (prevNode, startNode);
        }
      }

      loopStartStack.pop ();
      loopNextStack.pop ();
      */
    }
    return PROCESS_CONTINUE;
  }

  //Method to handle visiting a parsing problem.  Hopefully none exist
  /* (non-Javadoc)
   * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#visit(org.eclipse.cdt.core.dom.ast.IASTProblem)
   */
  @Override
  public int visit (IASTProblem problem) {
    throw new CFAGenerationRuntimeException(problem.getMessage(), problem);
  }
  
  /**
   * This method adds this edge to the leaving and entering edges
   * of its predecessor and successor respectively, but it does so only
   * if the edge does not contain dead code 
   */
  private void addToCFA(CFAEdge edge) {
    CFANode predecessor = edge.getPredecessor();

    // Nodes with an outgoing jump edge (return/break/goto) don't have other outgoing edges!
    assert !predecessor.hasJumpEdgeLeaving();
    if (edge.isJumpEdge()) {
      assert predecessor.getNumLeavingEdges() == 0;
    }
    
    if ((predecessor.getNumEnteringEdges() == 0)
          && !(predecessor instanceof CFAFunctionDefinitionNode)
          && !(predecessor instanceof CFALabelNode)) {
        
      if (!(edge instanceof BlankEdge) || edge.isJumpEdge()) {
        // don't log if the dead code begins with a blank edge, this is most often a false positive
        // but do log jump edges
        logger.log(Level.INFO, "Dead code detected at line", edge.getLineNumber() + ":", edge.getRawStatement());
      }
        
      // don't add this edge to the CFA

    } else { 

      registerEdgeAtNodes(edge);
    }
  }

  /**
   * Insert nodes for global declarations after first node of CFA.
   */
  public static void insertGlobalDeclarations(final CFAFunctionDefinitionNode cfa, List<IASTDeclaration> globalVars, LogManager logger) {
    if (globalVars.isEmpty()) {
      return;
    }
    // create a series of GlobalDeclarationEdges, one for each declaration,
    // and add them as successors of the input node
    final CFANode first = new CFANode(0, cfa.getFunctionName());
    CFANode cur = first;

    for (IASTDeclaration d : globalVars) {
      assert(d instanceof IASTSimpleDeclaration);
      IASTSimpleDeclaration sd = (IASTSimpleDeclaration)d;
      CFANode n = new CFANode(sd.getFileLocation().getStartingLineNumber(), cur.getFunctionName());
      GlobalDeclarationEdge e = new GlobalDeclarationEdge(sd,
          sd.getFileLocation().getStartingLineNumber(), cur, n);
      registerEdgeAtNodes(e);
      cur = n;
    }

    // split off first node of CFA
    assert cfa.getNumLeavingEdges() == 1;
    assert cfa.getLeavingSummaryEdge() == null;
    CFAEdge firstEdge = cfa.getLeavingEdge(0);
    assert firstEdge instanceof BlankEdge && !firstEdge.isJumpEdge();
    CFANode secondNode = firstEdge.getSuccessor();

    cfa.removeLeavingEdge(firstEdge);
    secondNode.removeEnteringEdge(firstEdge);

    // and add a blank edge connecting the first node of CFA with declarations
    BlankEdge be = new BlankEdge("INIT GLOBAL VARS", 0, cfa, first);
    registerEdgeAtNodes(be);

    // and a blank edge connecting the declarations with the second node of CFA
    be = new BlankEdge(firstEdge.getRawStatement(), firstEdge.getLineNumber(), cur, secondNode);
    registerEdgeAtNodes(be);

    return;
  }
  
  private static void registerEdgeAtNodes(CFAEdge edge) {
    edge.getPredecessor().addLeavingEdge(edge);
    edge.getSuccessor().addEnteringEdge(edge);
  }
}
