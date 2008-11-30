package cpaplugin.cfa;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBreakStatement;
import org.eclipse.cdt.core.dom.ast.IASTCaseStatement;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTContinueStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDefaultStatement;
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
import org.eclipse.cdt.core.dom.ast.IASTReturnStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTSwitchStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;

import cpaplugin.cfa.objectmodel.BlankEdge;
import cpaplugin.cfa.objectmodel.CFAErrorNode;
import cpaplugin.cfa.objectmodel.CFAExitNode;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cfa.objectmodel.c.AssumeEdge;
import cpaplugin.cfa.objectmodel.c.DeclarationEdge;
import cpaplugin.cfa.objectmodel.c.FunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.c.StatementEdge;
import cpaplugin.exceptions.CFAGenerationRuntimeException;

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
	private Deque<CFANode> switchStartStack;

	// Data structures for handling goto
	private Map<String, CFANode> labelMap;
	private Map<String, List<CFANode>> gotoLabelNeeded;

	// Data structures for handling function declarations
	private CFAMap cfas;
	private CFAExitNode returnNode;

	// Data structure for storing global declarations
	private List<IASTDeclaration> globalDeclarations;

	public CFABuilder ()
	{
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
		switchStartStack = new ArrayDeque<CFANode> ();

		labelMap = new HashMap<String, CFANode> ();
		gotoLabelNeeded = new HashMap<String, List<CFANode>> ();

		cfas = new CFAMap ();
		returnNode = null;

		globalDeclarations = new ArrayList<IASTDeclaration> ();
	}

	/**
	 * Retrieves list of all functions
	 * @return all CFAs in the program
	 */
	public CFAMap getCFAs ()
	{
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
	public int visit (IASTDeclaration declaration)
	{
		IASTFileLocation fileloc = declaration.getFileLocation ();

		if (declaration instanceof IASTSimpleDeclaration)
		{
			if (locStack.size () > 0) // i.e. we're in a function
			{
				CFANode prevNode = locStack.pop ();
				CFANode nextNode = new CFANode (fileloc.getStartingLineNumber ());

				IASTSimpleDeclaration simpleDeclaration = (IASTSimpleDeclaration) declaration;
				DeclarationEdge edge = new DeclarationEdge (
				        simpleDeclaration.getRawSignature (), simpleDeclaration.getDeclarators (),
				        simpleDeclaration.getDeclSpecifier());
				edge.initialize (prevNode, nextNode);

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
			
			locStack.add (newCFA);
			cfas.addCFA(nameOfFunction, newCFA);

			returnNode = new CFAExitNode (fileloc.getEndingLineNumber (), nameOfFunction);
			newCFA.setExitNode(returnNode);
		}
		
		return PROCESS_CONTINUE;
	}

	public int leave (IASTDeclaration declaration)
	{
		if (declaration instanceof IASTFunctionDefinition)
		{
			if (locStack.size () != 1)
				throw new CFAGenerationRuntimeException ("Depth wrong. Geoff needs to do more work");

			CFANode lastNode = locStack.pop ();

			if (!lastNode.hasJumpEdgeLeaving ())
			{
				BlankEdge blankEdge = new BlankEdge ("default return");
				blankEdge.initialize (lastNode, returnNode);
			}

			returnNode = null;
		}

		return PROCESS_CONTINUE;
	}

	// Methods for to handle visiting and leaving Statements
	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#visit(org.eclipse.cdt.core.dom.ast.IASTStatement)
	 */
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

			    BlankEdge blankEdge = new BlankEdge ("");
			    blankEdge.initialize (prevNode, nextNode);
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
			handleBreakStatement ((IASTBreakStatement)statement);
		else if (statement instanceof IASTContinueStatement)
			handleContinueStatement ((IASTContinueStatement)statement);
		else if (statement instanceof IASTLabelStatement)
			handleLabelStatement ((IASTLabelStatement)statement, fileloc);
		else if (statement instanceof IASTGotoStatement)
			handleGotoStatement ((IASTGotoStatement)statement, fileloc);
		else if (statement instanceof IASTReturnStatement)
			handleReturnStatement ((IASTReturnStatement)statement);
		else if (statement instanceof IASTSwitchStatement)
			handleSwitchStatement ((IASTSwitchStatement)statement, fileloc);
		else if (statement instanceof IASTCaseStatement)
			handleCaseStatement ((IASTCaseStatement)statement, fileloc);
		else if (statement instanceof IASTDefaultStatement)
			handleDefaultStatement ((IASTDefaultStatement)statement, fileloc);
		else if (statement instanceof IASTNullStatement)
		{
			// We really don't care about blank statements
		}
		else if (statement instanceof IASTDeclarationStatement)
		{
			// TODO: I think we can ignore these here...
		}
		else
		{
			System.out.println("statement " + statement.getRawSignature());
			throw new CFAGenerationRuntimeException ("Type: " + statement.getClass ().getName () + " support not implemented", fileloc.getStartingLineNumber ());
		}

		return PROCESS_CONTINUE;
	}

	private void handleExpressionStatement (IASTExpressionStatement exprStatement, IASTFileLocation fileloc)
	{
		CFANode prevNode = locStack.pop ();
		CFANode nextNode = new CFANode (fileloc.getStartingLineNumber ());

		StatementEdge edge = new StatementEdge (exprStatement.getRawSignature (), exprStatement.getExpression ());
		edge.initialize (prevNode, nextNode);

		locStack.push (nextNode);
	}

	private final int IF_CONDITION_NORMAL = -1;
	private final int IF_CONDITION_ALWAYS_FALSE = 0;
	private final int IF_CONDITION_ALWAYS_TRUE = 1;
	
	private int getIfConditionKind(IASTIfStatement ifStatement) {
	    IASTExpression cond = ifStatement.getConditionExpression();
	    if (cond instanceof IASTLiteralExpression) {
	        if (((IASTLiteralExpression)cond).getKind() == 
	            IASTLiteralExpression.lk_integer_constant) {
	            int c = Integer.parseInt(cond.getRawSignature());
	            if (c == 0) return IF_CONDITION_ALWAYS_FALSE;
	            else return IF_CONDITION_ALWAYS_TRUE;
	        }	         
	    } 
	    return IF_CONDITION_NORMAL;
	}
	
	private void handleIfStatement (IASTIfStatement ifStatement, IASTFileLocation fileloc)
	{       
		CFANode prevNode = locStack.pop ();
		CFANode postIfNode = new CFANode (fileloc.getEndingLineNumber ());

		locStack.push (postIfNode);
		
		int kind = getIfConditionKind(ifStatement);
		
		switch (kind) {
		case IF_CONDITION_ALWAYS_FALSE: {
		    BlankEdge edge = new BlankEdge("");
		    if (ifStatement.getElseClause() == null) {
		        edge.initialize(prevNode, postIfNode);
		    } else {
		        CFANode elseNode = 
		            new CFANode(fileloc.getStartingLineNumber());
		        edge.initialize(prevNode, elseNode);
		        elseStack.push(elseNode);
		        CFANode n = new CFANode(-1);
		        locStack.push(n);
		    }
		}
		    break;
		case IF_CONDITION_ALWAYS_TRUE: {
		    BlankEdge edge = new BlankEdge("");
		    CFANode thenNode = 
		        new CFANode(fileloc.getStartingLineNumber());
		    edge.initialize(prevNode, thenNode);
		    locStack.push(thenNode);
		    if (ifStatement.getElseClause() != null) {
		        CFANode n = new CFANode(-1);
		        elseStack.push(n);
		    }
		}
		    break;
		case IF_CONDITION_NORMAL: {
		    CFANode ifStartTrue = new CFANode (fileloc.getStartingLineNumber ());
		    AssumeEdge assumeEdgeTrue = new AssumeEdge (ifStatement.getConditionExpression ().getRawSignature (), 
		            ifStatement.getConditionExpression (), 
		            true);

		    assumeEdgeTrue.initialize (prevNode, ifStartTrue);
		    locStack.push (ifStartTrue);

		    if (ifStatement.getElseClause () != null) {
		        CFANode ifStartFalse = new CFANode (fileloc.getStartingLineNumber ());
		        AssumeEdge assumeEdgeFalse = new AssumeEdge ("!(" + ifStatement.getConditionExpression ().getRawSignature () + ")", 
		                ifStatement.getConditionExpression (), 
		                false);

		        assumeEdgeFalse.initialize (prevNode, ifStartFalse);
		        elseStack.push (ifStartFalse);
		    } else {
		        AssumeEdge assumeEdgeFalse = new AssumeEdge ("!(" + ifStatement.getConditionExpression ().getRawSignature () + ")", 
		                ifStatement.getConditionExpression (), 
		                false);

		        assumeEdgeFalse.initialize (prevNode, postIfNode);
		    }
		} // end of IF_CONDITION_NORMAL case
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

		BlankEdge blankEdge = new BlankEdge ("while");
		blankEdge.initialize (prevNode, loopStart);
	}

	private void handleBreakStatement (IASTBreakStatement breakStatement)
	{
		CFANode prevNode = locStack.peek ();
		CFANode nextNode = loopNextStack.peek ();

		BlankEdge blankEdge = new BlankEdge (breakStatement.getRawSignature ());
		blankEdge.setIsJumpEdge (true);
		blankEdge.initialize (prevNode, nextNode);
	}

	private void handleContinueStatement (IASTContinueStatement continueStatement)
	{
		CFANode prevNode = locStack.peek ();
		CFANode loopStart = loopStartStack.peek ();

		BlankEdge blankEdge = new BlankEdge (continueStatement.getRawSignature ());
		blankEdge.setIsJumpEdge (true);
		blankEdge.initialize (prevNode, loopStart);
	}

	private void handleLabelStatement (IASTLabelStatement labelStatement, IASTFileLocation fileloc)
	{
		String labelName = labelStatement.getName ().toString ();

		CFANode prevNode = locStack.pop ();
		CFANode labelNode = null; // AG
		if (labelName.toLowerCase().startsWith("error")) {
		    // AG - we want to know which are the error locations: each
		    // node with a label starting with "error"
		    labelNode = new CFAErrorNode(
		            fileloc.getStartingLineNumber());
		} else {
		    labelNode = new CFANode (fileloc.getStartingLineNumber ());
		}

		BlankEdge blankEdge = new BlankEdge ("Label: " + labelName);
		blankEdge.initialize (prevNode, labelNode);

		locStack.push (labelNode);

		labelMap.put (labelName, labelNode);

		// Check if any goto's previously analyzed need connections to this label
		List<CFANode> labelsNeeded = gotoLabelNeeded.get (labelName);
		if (labelsNeeded != null)
		{
			for (CFANode gotoNode : labelsNeeded)
			{
				BlankEdge gotoEdge = new BlankEdge ("Goto: " + labelName);
				gotoEdge.setIsJumpEdge (true);
				gotoEdge.initialize (gotoNode, labelNode);
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
			BlankEdge gotoEdge = new BlankEdge ("Goto: " + labelName);
			gotoEdge.setIsJumpEdge (true);
			gotoEdge.initialize (prevNode, labelNode);
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

	private void handleReturnStatement (IASTReturnStatement returnStatement)
	{
		CFANode prevNode = locStack.peek ();            
		CFANode nextNode = returnNode;

		StatementEdge edge = new StatementEdge (returnStatement.getRawSignature (), returnStatement.getReturnValue ());
		edge.setIsJumpEdge (true);
		edge.initialize (prevNode, nextNode);
	}

	private void handleSwitchStatement (IASTSwitchStatement switchStatement, IASTFileLocation fileloc)
	{
		CFANode prevNode = locStack.pop ();
		CFANode switchStart = new CFANode (fileloc.getStartingLineNumber ());

		CFANode postLoopNode = new CFANode (fileloc.getEndingLineNumber ());
		locStack.push (postLoopNode);
		locStack.push (switchStart);

		switchStartStack.push (switchStart); // Continue shouldn't go to the beginning of a switch, but the beginning of the current loop
		loopNextStack.push (postLoopNode);   // Break should still just leave the switch, not the loop

		StatementEdge switchEdge = new StatementEdge (switchStatement.getControllerExpression ().getRawSignature (), switchStatement.getControllerExpression ());
		switchEdge.initialize (prevNode, switchStart);
	}

	private void handleCaseStatement (IASTCaseStatement caseStatement, IASTFileLocation fileloc)
	{
		CFANode prevNode = locStack.pop ();
		CFANode switchStart = switchStartStack.peek ();

		CFANode caseNode = new CFANode (fileloc.getStartingLineNumber ());
		IASTExpression caseExpression = caseStatement.getExpression ();

		AssumeEdge assumeEdge1 = new AssumeEdge (caseExpression.getRawSignature (), caseExpression, true);
		assumeEdge1.initialize (switchStart, caseNode);

		if (prevNode != switchStart)
		{
			BlankEdge assumeEdge2 = new BlankEdge ("");
			assumeEdge2.initialize (prevNode, caseNode);
		}

		locStack.push (caseNode);
	}

	private void handleDefaultStatement (IASTDefaultStatement defaultStatement, IASTFileLocation fileloc)
	{
		CFANode prevNode = locStack.pop ();
		CFANode switchStart = switchStartStack.peek ();

		CFANode caseNode = new CFANode (fileloc.getStartingLineNumber ());

		BlankEdge blankEdge1 = new BlankEdge ("default");       
		blankEdge1.initialize (switchStart, caseNode);

		if (prevNode != switchStart)
		{
			BlankEdge blankEdge2 = new BlankEdge ("default");
			blankEdge2.initialize (prevNode, caseNode);
		}

		locStack.push (caseNode);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#leave(org.eclipse.cdt.core.dom.ast.IASTStatement)
	 */
	public int leave (IASTStatement statement)
	{
		if (statement instanceof IASTIfStatement)
		{
			CFANode prevNode = locStack.pop ();

			if (!prevNode.hasJumpEdgeLeaving ())
			{
				CFANode nextNode = locStack.peek ();

				BlankEdge blankEdge = new BlankEdge ("");
				blankEdge.initialize (prevNode, nextNode);
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
					BlankEdge blankEdge = new BlankEdge ("");
					blankEdge.initialize (prevNode, startNode);
				}
			}
			loopStartStack.pop ();
			loopNextStack.pop ();
		}
		else if (statement instanceof IASTWhileStatement) // Code never hit due to bug in Eclipse CDT
		{
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
		}
		else if (statement instanceof IASTSwitchStatement)
		{
			CFANode prevNode = locStack.pop ();

			if (!prevNode.hasJumpEdgeLeaving ())
			{
				CFANode endNode = loopNextStack.peek ();

				if (!prevNode.hasEdgeTo (endNode))
				{
					BlankEdge blankEdge = new BlankEdge ("");
					blankEdge.initialize (prevNode, endNode);
				}
			}

			switchStartStack.pop ();
			loopNextStack.pop ();
		}

		return PROCESS_CONTINUE;
	}

	//Method to handle visiting a parsing problem.  Hopefully none exist
	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#visit(org.eclipse.cdt.core.dom.ast.IASTProblem)
	 */
	public int visit (IASTProblem problem)
	{
		IASTFileLocation fileloc = problem.getFileLocation ();

		System.out.println ("IASTProblem Raw Signature: " + problem.getRawSignature ());
		System.out.println ("    File Loc: " + fileloc);

		return PROCESS_CONTINUE;
	}
}
