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
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTGotoStatement;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTLabelStatement;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTNullStatement;
import org.eclipse.cdt.core.dom.ast.IASTProblem;
import org.eclipse.cdt.core.dom.ast.IASTProblemDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTProblemStatement;
import org.eclipse.cdt.core.dom.ast.IASTReturnStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.objectmodel.BlankEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAErrorNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionExitNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.exceptions.CFAGenerationRuntimeException;

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
	private Deque<CFANode> locStack;

	// Data structures for handling loops & else conditions
	private Deque<CFANode> loopStartStack;
	private Deque<CFANode> loopNextStack; // For the node following the current if / while block
	private Deque<CFANode> elseStack;

//  switch statements are removed by CIL, also CFA would have been wrong
//	private Deque<CFANode> switchStartStack = new ArrayDeque<CFANode> ();

	// Data structures for handling goto
	private Map<String, CFALabelNode> labelMap;
	private Map<String, List<CFANode>> gotoLabelNeeded;

	// Data structures for handling function declarations
	private Map<String, CFAFunctionDefinitionNode> cfas;
	private CFAFunctionExitNode returnNode;

	// Data structure for storing global declarations
	private List<IASTDeclaration> globalDeclarations;

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

		locStack = new ArrayDeque<CFANode> ();
		loopStartStack = new ArrayDeque<CFANode> ();
		loopNextStack = new ArrayDeque<CFANode> ();
		elseStack = new ArrayDeque<CFANode> ();

		labelMap = new HashMap<String, CFALabelNode> ();
		gotoLabelNeeded = new HashMap<String, List<CFANode>> ();

		cfas = new HashMap<String, CFAFunctionDefinitionNode>();
		returnNode = null;

		globalDeclarations = new ArrayList<IASTDeclaration> ();
	}

	/**
	 * Retrieves list of all functions
	 * @return all CFAs in the program
	 */
	public Map<String, CFAFunctionDefinitionNode> getCFAs()	{
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
				CFANode nextNode = new CFANode (fileloc.getStartingLineNumber ());

				DeclarationEdge edge = new DeclarationEdge ((IASTSimpleDeclaration) declaration,
				        fileloc.getStartingLineNumber(), prevNode, nextNode);
				edge.addToCFA(logger);

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
			if (gotoLabelNeeded.size () != 0)
				throw new CFAGenerationRuntimeException ("Goto labels not found in previous function definition?");

			labelMap.clear ();

			IASTFunctionDefinition fdef = (IASTFunctionDefinition) declaration;

			FunctionDefinitionNode newCFA = new FunctionDefinitionNode (fileloc.getStartingLineNumber (), fdef);
			String nameOfFunction = newCFA.getFunctionName();
			newCFA.setFunctionName(nameOfFunction);

			CFANode functionStartDummyNode = new CFANode(fileloc.getStartingLineNumber ());
			BlankEdge dummyEdge = new BlankEdge("Function start dummy edge", fileloc.getStartingLineNumber(), newCFA, functionStartDummyNode);
			dummyEdge.addToCFA(logger);

			locStack.add (functionStartDummyNode);
			cfas.put(nameOfFunction, newCFA);

			returnNode = new CFAFunctionExitNode (fileloc.getEndingLineNumber (), nameOfFunction);
			newCFA.setExitNode(returnNode);

		} else if (declaration instanceof IASTProblemDeclaration) {
		  // CDT parser struggles on GCC's __attribute__((something)) constructs because we use C99 as default
		  // Either insert the following macro before compiling with CIL:
		  // #define  __attribute__(x)  /*NOTHING*/
		  // or insert "parser.dialect = GNUC" into properties file
		  visit(((IASTProblemDeclaration)declaration).getProblem());

		} else if (declaration instanceof IASTASMDeclaration) {
		  // TODO Assembler code is ignored here
		  logger.log(Level.WARNING, "Ignoring inline assembler code at line " + fileloc.getStartingLineNumber() + ", analysis is probably unsound!");

		  CFANode prevNode = locStack.pop();
		  CFANode nextNode = new CFANode(fileloc.getStartingLineNumber());

		  BlankEdge edge = new BlankEdge("Ignored inline assembler code", fileloc.getStartingLineNumber(), prevNode, nextNode);
		  edge.addToCFA(logger);

		  locStack.push(nextNode);

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
				BlankEdge blankEdge = new BlankEdge ("default return", lastNode.getLineNumber(), lastNode, returnNode);
				blankEdge.addToCFA(logger);
			}

			returnNode = null;
		}

		return PROCESS_CONTINUE;
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
			    blankEdge.addToCFA(logger);
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
/* switch statements are removed by CIL, also CFA would have been wrong
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
		CFANode nextNode = new CFANode (fileloc.getStartingLineNumber ());

		StatementEdge edge = new StatementEdge(exprStatement, fileloc.getStartingLineNumber(), prevNode, nextNode, exprStatement.getExpression(), false);
		edge.addToCFA(logger);

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
		CFANode postIfNode = new CFANode (fileloc.getEndingLineNumber ());

		locStack.push (postIfNode);

		IF_CONDITION kind = getIfConditionKind(ifStatement);

		switch (kind) {
		case ALWAYS_FALSE: {
		    if (ifStatement.getElseClause() == null) {
		        BlankEdge edge = new BlankEdge("", fileloc.getStartingLineNumber(), prevNode, postIfNode);
		        edge.addToCFA(logger);
		    } else {
		        CFANode elseNode =
		            new CFANode(fileloc.getStartingLineNumber());
            BlankEdge edge = new BlankEdge("", fileloc.getStartingLineNumber(), prevNode, elseNode);
		        edge.addToCFA(logger);
		        elseStack.push(elseNode);
		        CFANode n = new CFANode(-1);
		        locStack.push(n);
		    }
		}
		    break;
		case ALWAYS_TRUE: {
		    CFANode thenNode =
		        new CFANode(fileloc.getStartingLineNumber());
        BlankEdge edge = new BlankEdge("", fileloc.getStartingLineNumber(), prevNode, thenNode);
		    edge.addToCFA(logger);
		    locStack.push(thenNode);
		    if (ifStatement.getElseClause() != null) {
		        CFANode n = new CFANode(-1);
		        elseStack.push(n);
		    }
		}
		    break;
		case NORMAL: {
		    CFANode ifStartTrue = new CFANode (fileloc.getStartingLineNumber ());
		    AssumeEdge assumeEdgeTrue = new AssumeEdge (ifStatement.getConditionExpression ().getRawSignature (),
		            fileloc.getStartingLineNumber(), prevNode, ifStartTrue,
		            ifStatement.getConditionExpression (),
		            true);

		    assumeEdgeTrue.addToCFA(logger);
		    locStack.push (ifStartTrue);

		    if (ifStatement.getElseClause () != null) {
		        CFANode ifStartFalse = new CFANode (fileloc.getStartingLineNumber ());
		        AssumeEdge assumeEdgeFalse = new AssumeEdge ("!(" + ifStatement.getConditionExpression ().getRawSignature () + ")",
		                fileloc.getStartingLineNumber(), prevNode, ifStartFalse,
		                ifStatement.getConditionExpression (),
		                false);

		        assumeEdgeFalse.addToCFA(logger);
		        elseStack.push (ifStartFalse);
		    } else {
		        AssumeEdge assumeEdgeFalse = new AssumeEdge ("!(" + ifStatement.getConditionExpression ().getRawSignature () + ")",
		                fileloc.getStartingLineNumber(), prevNode, postIfNode,
		                ifStatement.getConditionExpression (),
		                false);

		        assumeEdgeFalse.addToCFA(logger);
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
		CFANode loopStart = new CFANode (fileloc.getStartingLineNumber ());
		loopStart.setLoopStart();

		CFANode postLoopNode = new CFANode (fileloc.getEndingLineNumber ());
		locStack.push (postLoopNode);
		locStack.push (loopStart);

		loopStartStack.push (loopStart);
		loopNextStack.push (postLoopNode);

		BlankEdge blankEdge = new BlankEdge("while", fileloc.getStartingLineNumber(), prevNode, loopStart);
		blankEdge.addToCFA(logger);
	}

	private void handleBreakStatement (IASTBreakStatement breakStatement, IASTFileLocation fileloc)
	{
		CFANode prevNode = locStack.peek ();
		CFANode nextNode = loopNextStack.peek ();

		BlankEdge blankEdge = new BlankEdge(breakStatement.getRawSignature(), fileloc.getStartingLineNumber(), prevNode, nextNode, true);
		blankEdge.addToCFA(logger);
	}

	private void handleContinueStatement (IASTContinueStatement continueStatement, IASTFileLocation fileloc)
	{
		CFANode prevNode = locStack.peek ();
		CFANode loopStart = loopStartStack.peek ();

		BlankEdge blankEdge = new BlankEdge(continueStatement.getRawSignature(), fileloc.getStartingLineNumber(), prevNode, loopStart, true);
		blankEdge.addToCFA(logger);
	}

	private void handleLabelStatement (IASTLabelStatement labelStatement, IASTFileLocation fileloc)
	{
		String labelName = labelStatement.getName ().toString ();

		CFANode prevNode = locStack.pop ();
		CFALabelNode labelNode = null; // AG
		if (labelName.toLowerCase().startsWith("error")) {
		    // AG - we want to know which are the error locations: each
		    // node with a label starting with "error"
		    labelNode = new CFAErrorNode(
		            fileloc.getStartingLineNumber(), labelName);
		} else {
		    labelNode = new CFALabelNode (fileloc.getStartingLineNumber(), labelName);
		}

		BlankEdge blankEdge = new BlankEdge("Label: " + labelName, fileloc.getStartingLineNumber(), prevNode, labelNode);
		blankEdge.addToCFA(logger);

		locStack.push (labelNode);

		labelMap.put (labelName, labelNode);

		// Check if any goto's previously analyzed need connections to this label
		List<CFANode> labelsNeeded = gotoLabelNeeded.get (labelName);
		if (labelsNeeded != null)
		{
			for (CFANode gotoNode : labelsNeeded)
			{
				BlankEdge gotoEdge = new BlankEdge("Goto: " + labelName, gotoNode.getLineNumber(), gotoNode, labelNode, true);
				gotoEdge.addToCFA(logger);
			}

			gotoLabelNeeded.remove (labelName);
		}
	}

	private void handleGotoStatement (IASTGotoStatement gotoStatement, IASTFileLocation fileloc)
	{
		String labelName = gotoStatement.getName ().toString ();

		CFANode prevNode = locStack.peek ();
		CFANode labelNode = labelMap.get (labelName);
		if (labelNode != null)
		{
			BlankEdge gotoEdge = new BlankEdge("Goto: " + labelName, fileloc.getStartingLineNumber(), prevNode, labelNode, true);
			gotoEdge.addToCFA(logger);
		}
		else
		{
			List<CFANode> labelsNeeded = gotoLabelNeeded.get (labelName);
			if (labelsNeeded == null)
			{
				labelsNeeded = new ArrayList<CFANode> ();
				gotoLabelNeeded.put (labelName, labelsNeeded);
			}

			labelsNeeded.add (prevNode);
		}
	}

	private void handleReturnStatement (IASTReturnStatement returnStatement, IASTFileLocation fileloc)
	{
		CFANode prevNode = locStack.peek ();
		CFANode nextNode = returnNode;

		StatementEdge edge = new StatementEdge(returnStatement, fileloc.getStartingLineNumber(), prevNode, nextNode, returnStatement.getReturnValue(), true);
		edge.addToCFA(logger);
	}

/* switch statements are removed by CIL, also CFA would have been wrong
	private void handleSwitchStatement (IASTSwitchStatement switchStatement, IASTFileLocation fileloc)
	{
		CFANode prevNode = locStack.pop ();
		CFANode switchStart = new CFANode (fileloc.getStartingLineNumber ());

		CFANode postLoopNode = new CFANode (fileloc.getEndingLineNumber ());
		locStack.push (postLoopNode);
		locStack.push (switchStart);

		switchStartStack.push (switchStart); // Continue shouldn't go to the beginning of a switch, but the beginning of the current loop
		loopNextStack.push (postLoopNode);   // Break should still just leave the switch, not the loop

		StatementEdge switchEdge = new StatementEdge(switchStatement.getControllerExpression().getRawSignature(), fileloc.getStartingLineNumber(), prevNode, switchStart, switchStatement.getControllerExpression(), false);
		switchEdge.addToCFA();
	}

	private void handleCaseStatement (IASTCaseStatement caseStatement, IASTFileLocation fileloc)
	{
		CFANode prevNode = locStack.pop ();
		CFANode switchStart = switchStartStack.peek ();

		CFANode caseNode = new CFANode (fileloc.getStartingLineNumber ());
		IASTExpression caseExpression = caseStatement.getExpression ();

		AssumeEdge assumeEdge1 = new AssumeEdge(caseExpression.getRawSignature(), fileloc.getStartingLineNumber(), switchStart, caseNode, caseExpression, true);
		assumeEdge1.addToCFA();

		if (prevNode != switchStart)
		{
			BlankEdge assumeEdge2 = new BlankEdge("", fileloc.getStartingLineNumber(), prevNode, caseNode);
			assumeEdge2.addToCFA();
		}

		locStack.push (caseNode);
	}

	private void handleDefaultStatement (IASTDefaultStatement defaultStatement, IASTFileLocation fileloc)
	{
		CFANode prevNode = locStack.pop ();
		CFANode switchStart = switchStartStack.peek ();

		CFANode caseNode = new CFANode (fileloc.getStartingLineNumber ());

		BlankEdge blankEdge1 = new BlankEdge("default", fileloc.getStartingLineNumber(), switchStart, caseNode);
		blankEdge1.addToCFA();

		if (prevNode != switchStart)
		{
			BlankEdge blankEdge2 = new BlankEdge("default", fileloc.getStartingLineNumber(), prevNode, caseNode);
			blankEdge2.addToCFA();
		}

		locStack.push (caseNode);
	}
*/

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
				blankEdge.addToCFA(logger);
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
					blankEdge.addToCFA(logger);
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
/* switch statements are removed by CIL, also CFA would have been wrong
		else if (statement instanceof IASTSwitchStatement)
		{
			CFANode prevNode = locStack.pop ();

			if (!prevNode.hasJumpEdgeLeaving ())
			{
				CFANode endNode = loopNextStack.peek ();

				if (!prevNode.hasEdgeTo (endNode))
				{
					BlankEdge blankEdge = new BlankEdge("", prevNode.getLineNumber(), prevNode, endNode);
					blankEdge.addToCFA();
				}
			}

			switchStartStack.pop ();
			loopNextStack.pop ();
		}
*/
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
}
