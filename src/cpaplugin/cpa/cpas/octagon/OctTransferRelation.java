package cpaplugin.cpa.cpas.octagon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import octagon.LibraryAccess;
import octagon.Num;
import octagon.OctLibraryTest;
import octagon.Octagon;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTPointerOperator;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;

import sun.awt.GlobalCursorManager;

import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.CFAExitNode;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cfa.objectmodel.c.AssumeEdge;
import cpaplugin.cfa.objectmodel.c.CallToReturnEdge;
import cpaplugin.cfa.objectmodel.c.DeclarationEdge;
import cpaplugin.cfa.objectmodel.c.FunctionCallEdge;
import cpaplugin.cfa.objectmodel.c.FunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.c.GlobalDeclarationEdge;
import cpaplugin.cfa.objectmodel.c.ReturnEdge;
import cpaplugin.cfa.objectmodel.c.StatementEdge;
import cpaplugin.cmdline.CPAMain;
import cpaplugin.cpa.common.CompositeElement;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.exceptions.CPAException;
import cpaplugin.exceptions.OctagonTransferException;
/**
 * Handles transfer relation for Octagon abstract domain library.
 * See <a href="http://www.di.ens.fr/~mine/oct/">Octagon abstract domain library</a>
 * @author Erkan
 *
 */
public class OctTransferRelation implements TransferRelation{

	// the domain of the cpa, this can be used to retrieve the bottom and the
	// top elements if needed
	private OctDomain octDomain;
	// set to set global variables
	private Set<String> globalVars;

	/**
	 * Class constructor.
	 * @param octDomain the domain of the Octagon library
	 */
	public OctTransferRelation (OctDomain octDomain)
	{
		this.octDomain = octDomain;
		globalVars = new HashSet<String>();
	}

	/* (non-Javadoc)
	 * @see cpaplugin.cpa.common.interfaces.TransferRelation#getAbstractDomain()
	 */
	public AbstractDomain getAbstractDomain ()
	{
		return octDomain;
	}

	/* (non-Javadoc)
	 * @see cpaplugin.cpa.common.interfaces.TransferRelation#getAbstractSuccessor(cpaplugin.cpa.common.interfaces.AbstractElement, cpaplugin.cfa.objectmodel.CFAEdge)
	 */
	public AbstractElement getAbstractSuccessor (AbstractElement element, CFAEdge cfaEdge)
	{
		System.out.println(" EDGE "+ cfaEdge.getRawStatement());
		// octElement is the region of the current state
		// this state will be updated using the edge
		OctElement octElement = (OctElement) element;

		// check the type of the edge
		switch (cfaEdge.getEdgeType ())
		{

		// if edge is a statement edge, e.g. a = b + c
		case StatementEdge:
		{
			octElement = octElement.clone ();

			StatementEdge statementEdge = (StatementEdge) cfaEdge;
			IASTExpression expression = statementEdge.getExpression ();

			// this statement is a function return, e.g. return (a);
			// note that this is different from return edge
			// this is a statement edge which leads the function to the 
			// last node of its CFA, where return edge is from that last node
			// to the return site of the caller function
			if(statementEdge.isJumpEdge())
			{
				try {
					handleExitFromFunction(octElement, expression, statementEdge);
				} catch (OctagonTransferException e) {
					e.printStackTrace();
				}
			}

			// this is a regular statement
			else{
				try {
					handleStatement (octElement, expression, cfaEdge);
				} catch (OctagonTransferException e) {
					e.printStackTrace();
				}
			}
			break;
		}

		// edge is a decleration edge, e.g. int a;
		case DeclarationEdge:
		{
			octElement = octElement.clone ();

			DeclarationEdge declarationEdge = (DeclarationEdge) cfaEdge;
			handleDeclaration (octElement, declarationEdge);
			break;
		}

		// this is an assumption, e.g. if(a == b)
		case AssumeEdge:
		{
			octElement = octElement.clone ();

			AssumeEdge assumeEdge = (AssumeEdge) cfaEdge;
			IASTExpression expression = assumeEdge.getExpression();
			try {
				handleAssumption (octElement, expression, cfaEdge, assumeEdge.getTruthAssumption());
			} catch (OctagonTransferException e) {
				e.printStackTrace();
			}
			break;

		}

		case BlankEdge:
		{
			octElement = octElement.clone ();
			break;
		}

		case FunctionCallEdge: 
		{
			octElement = octElement.clone ();
			FunctionCallEdge functionCallEdge = (FunctionCallEdge) cfaEdge;

			// TODO check later
			// call to an external function
			if(functionCallEdge.isExternalCall())
			{
				try {
					handleExternalFunctionCall(octElement, functionCallEdge);
				} catch (OctagonTransferException e) {
					e.printStackTrace();
				}
			}
			// this function is not on call stack
			else{
				try {
					handleFunctionCall(octElement, functionCallEdge);
				} catch (OctagonTransferException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			break;
		}

		// this is a return edge from function, this is different from return statement
		// of the function. See case for statement edge for details 
		case ReturnEdge:
		{
			octElement = octElement.clone ();
			ReturnEdge functionReturnEdge = (ReturnEdge) cfaEdge;
			try {
				handleFunctionReturn(octElement, functionReturnEdge);
			} catch (OctagonTransferException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		}

		// Summary edge, we handle this on function return, do nothing 
		case CallToReturnEdge:
		{
			assert(false);
			break;
		}

		case MultiStatementEdge:
		{
			break;
		}

		case MultiDeclarationEdge:
		{
			break;
		}
		}
		return octElement;
	}

	/* (non-Javadoc)
	 * @see cpaplugin.cpa.common.interfaces.TransferRelation#getAllAbstractSuccessors(cpaplugin.cpa.common.interfaces.AbstractElement)
	 */
	public List<AbstractElement> getAllAbstractSuccessors (AbstractElement element) throws CPAException
	{
		throw new CPAException ("Cannot get all abstract successors from non-location domain");
	}

	/**
	 * Used to determine the type of statement and delegate to other methods
	 * @param octElement element to be modified
	 * @param expression statement expression
	 * @param cfaEdge statement edge
	 * @throws OctagonTransferException
	 */
	private void handleStatement (OctElement octElement, IASTExpression expression, CFAEdge cfaEdge) throws OctagonTransferException
	{
		// expression is a binary operation, e.g. a = b;
		if (expression instanceof IASTBinaryExpression) {
			handleBinaryExpression(octElement, expression, cfaEdge);
		}
		// expression is a unary operation, e.g. a++;
		else if (expression instanceof IASTUnaryExpression)
		{
			handleUnaryExpression(octElement, expression, cfaEdge);
		}
		else{
			throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
		}
	}

	/**
	 * @param octElement element to be updated possibly by adding new variables
	 * @param cfaEdge declaration edge
	 */
	private void handleDeclaration(OctElement octElement,
			DeclarationEdge declarationEdge) {

		IASTDeclarator[] declarators = declarationEdge.getDeclarators();
		IASTDeclSpecifier specifier = declarationEdge.getDeclSpecifier();

		for (IASTDeclarator declarator : declarators)
		{
			if(declarator != null)
			{
				// get the variable name in the declarator
				String varName = declarator.getName().toString();

				// TODO check other types of variables later - just handle primitive 
				// types for the moment
				// get pointer operators of the declaration
				IASTPointerOperator[] pointerOps = declarator.getPointerOperators();
				// don't add pointer variables to the list since we don't track them
				if(pointerOps.length > 0)
				{
					continue;
				}
				// TODO here we assume that the type of the variable is int or double
				// get the function name, we need this because there is a unique
				// set of variables saved for each function
				String functionName = declarationEdge.getPredecessor().getFunctionName();

				// if this is a global variable, add to the list of global variables
				// and set function name to "".
				if(declarationEdge instanceof GlobalDeclarationEdge)
				{
					globalVars.add(varName);
					functionName = "";
				}

				// if the variable is added, update add a new dimension to the octagon
				if(octElement.addVar(varName, functionName))
				{
					octElement.update(LibraryAccess.addDimension(octElement, 1));
				}
			}
		}
	}

	/**
	 * Handles the interpretation of the assumption expression and delegate it 
	 * to other methods
	 * @param octElement element to be modified
	 * @param expression statement expression
	 * @param cfaEdge statement edge
	 * @param truthValue does it follow the path where assumption is true or the path
	 * where it is false
	 * @throws OctagonTransferException
	 */
	private void handleAssumption(OctElement octElement,
			IASTExpression expression, CFAEdge cfaEdge, boolean truthValue) throws OctagonTransferException
			{
		String functionName = cfaEdge.getPredecessor().getFunctionName();
		// Binary operation
		if (expression instanceof IASTBinaryExpression) {
			IASTBinaryExpression binExp = ((IASTBinaryExpression)expression);
			int opType = binExp.getOperator ();

			IASTExpression op1 = binExp.getOperand1();
			IASTExpression op2 = binExp.getOperand2();

			// a (bop) ?
			if(op1 instanceof IASTIdExpression)
			{
				// a (bop) 9
				if(op2 instanceof IASTLiteralExpression)
				{
					IASTIdExpression var = (IASTIdExpression)op1;
					String varName = var.getRawSignature();
					int variableId = octElement.getVariableId(globalVars, varName, functionName);

					double valueOfLiteral = Double.valueOf(op2.getRawSignature()).doubleValue();
					// a > 9
					if(opType == IASTBinaryExpression.op_greaterThan)
					{
						// this is the if then edge
						if(truthValue)
						{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_greaterThan, variableId, -1, true, true, valueOfLiteral);
						}
						else{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_lessEqual, variableId, -1, true, true, valueOfLiteral);
						}
					}
					// a >= 9
					else if(opType == IASTBinaryExpression.op_greaterEqual)
					{
						// this is the if then edge
						if(truthValue)
						{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_greaterEqual, variableId, -1, true, true, valueOfLiteral);
						}
						else{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_lessThan, variableId, -1, true, true, valueOfLiteral);
						}
					}
					// a < 9
					else if(opType == IASTBinaryExpression.op_lessThan)
					{
						// this is the if then edge
						if(truthValue)
						{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_lessThan, variableId, -1, true, true, valueOfLiteral);
						}
						else{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_greaterEqual, variableId, -1, true, true, valueOfLiteral);
						}
					}
					// a <= 9
					else if(opType == IASTBinaryExpression.op_lessEqual)
					{
						// this is the if then edge
						if(truthValue)
						{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_lessEqual, variableId, -1, true, true, valueOfLiteral);
						}
						else{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_greaterThan, variableId, -1, true, true, valueOfLiteral);
						}
					}
					// a == 9
					else if(opType == IASTBinaryExpression.op_equals)
					{
						// this is the if then edge
						if(truthValue)
						{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_equals, variableId, -1, true, true, valueOfLiteral);
						}
						else{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_notequals, variableId, -1, true, true, valueOfLiteral);
						}
					}
					// a != 9
					else if(opType == IASTBinaryExpression.op_notequals)
					{
						// this is the if then edge
						if(truthValue)
						{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_notequals, variableId, -1, true, true, valueOfLiteral);
						}
						else{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_equals, variableId, -1, true, true, valueOfLiteral);
						}
					}
					else{
						throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
					}
				}
				// a (bop) b
				else if(op2 instanceof IASTIdExpression)
				{
					IASTIdExpression leftVar = (IASTIdExpression)op1;
					String leftVarName = leftVar.getRawSignature();
					int leftVariableId = octElement.getVariableId(globalVars, leftVarName, functionName);

					IASTIdExpression rightVar = (IASTIdExpression)op2;
					String rightVarName = rightVar.getRawSignature();
					int rightVariableId = octElement.getVariableId(globalVars, rightVarName, functionName);

					// a > b
					if(opType == IASTBinaryExpression.op_greaterThan)
					{
						// this is the if then edge
						if(truthValue)
						{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_greaterThan, leftVariableId, rightVariableId, true, true, 0);
						}
						else{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_lessEqual, leftVariableId, rightVariableId, true, true, 0);
						}
					}
					// a >= b
					else if(opType == IASTBinaryExpression.op_greaterEqual)
					{
						// this is the if then edge
						if(truthValue)
						{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_greaterEqual, leftVariableId, rightVariableId, true, true, 0);
						}
						else{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_lessThan, leftVariableId, rightVariableId, true, true, 0);
						}
					}
					// a < b
					else if(opType == IASTBinaryExpression.op_lessThan)
					{
						// this is the if then edge
						if(truthValue)
						{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_lessThan, leftVariableId, rightVariableId, true, true, 0);
						}
						else{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_greaterEqual, leftVariableId, rightVariableId, true, true, 0);
						}
					}
					// a <= b
					else if(opType == IASTBinaryExpression.op_lessEqual)
					{
						// this is the if then edge
						if(truthValue)
						{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_lessEqual, leftVariableId, rightVariableId, true, true, 0);
						}
						else{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_greaterThan, leftVariableId, rightVariableId, true, true, 0);
						}
					}
					// a == b
					else if(opType == IASTBinaryExpression.op_equals)
					{
						// this is the if then edge
						if(truthValue)
						{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_equals, leftVariableId, rightVariableId, true, true, 0);
						}
						else{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_notequals, leftVariableId, rightVariableId, true, true, 0);
						}
					}
					// a != b
					else if(opType == IASTBinaryExpression.op_notequals)
					{
						// this is the if then edge
						if(truthValue)
						{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_notequals, leftVariableId, rightVariableId, true, true, 0);
						}
						else{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_equals, leftVariableId, rightVariableId, true, true, 0);
						}
					}
					else{
						throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
					}
				}
				else{
					throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
				}
			}
		}
		// Unary operation
		else if (expression instanceof IASTUnaryExpression)
		{
			IASTUnaryExpression unaryExp = ((IASTUnaryExpression)expression);
			// ! exp
			if(unaryExp.getOperator() == IASTUnaryExpression.op_not)
			{
				IASTExpression exp1 = unaryExp.getOperand();
				// ! unaryExp
				if(exp1 instanceof IASTUnaryExpression){
					IASTUnaryExpression unaryExp1 = ((IASTUnaryExpression)exp1);
					// (exp)
					if (unaryExp1.getOperator() == IASTUnaryExpression.op_bracketedPrimary){
						IASTExpression exp2 = unaryExp1.getOperand();
						// (binaryExp)
						if(exp2 instanceof IASTBinaryExpression){
							IASTBinaryExpression binExp2 = (IASTBinaryExpression)exp2;
							handleAssumption(octElement, binExp2, cfaEdge, !truthValue);
						}
						else {
							throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
						}
					}
					else {
						throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
					}
				}

				else {
					throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
				}
			}
			else {
				throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
			}
		}

		else{
			throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
		}
			}

	/**
	 * Process assumptions even further for final delegation
	 * note that we assume that if a > b, then a >= b+1 since we increment
	 * the value by 1. So, we assume that all conditions are comparing integer values.
	 * Some examples of conversions by this method
	 * a > b is converted to a - b >= 1
	 * a > 8 is converted to a >= 7
	 * a >= b is converted to a - b >= 0
	 * @param octElement element to be modified
	 * @param opType type of conditional operation, e.g. >, ==, != see <a href="http://help.eclipse.org/help33/topic/org.eclipse.cdt.doc.isv/reference/api/org/eclipse/cdt/core/dom/ast/IASTBinaryExpression.html">Operation types</a>
	 * @param leftVariableId id of the lefthand side variable of comparison, e.g. if condition is a == b, this is a's id on variable map
	 * @param rightVariableId id of the righthand side variable of comparison, e.g. if condition is a == b, this i 's id on variable map, -1 if righthand side variable is a single literal 
	 * @param isLeftVarPos sign of the left variable, if variable is -a, then false
	 * @param isRightVarPos sign of the right variable, if variable is -a, then false
	 * @param valueOfLiteral the value of the second variable if it is a literal, e.g. a < 9, alternatively can be used to represent cases such as a < b + 6, then the value will be 6
	 * @throws OctagonTransferException
	 * @see octagon.VariableMap
	 */
	private void propagateBooleanExpression(OctElement octElement, int opType,
			int leftVariableId, int rightVariableId, boolean isLeftVarPos, 
			boolean isRightVarPos, double valueOfLiteral) throws OctagonTransferException {

		boolean isFirstVarPos = true;
		boolean isSecondVarPos = false;
		double value = 0;

		if(opType == IASTBinaryExpression.op_greaterEqual){
			isFirstVarPos = isLeftVarPos;
			if(rightVariableId >=0){
				isSecondVarPos = !isRightVarPos;
				value = 0;
			}
			else{
				value = valueOfLiteral;
			}
		}
		else if(opType == IASTBinaryExpression.op_greaterThan){
			isFirstVarPos = isLeftVarPos;
			if(rightVariableId >=0){
				isSecondVarPos = !isRightVarPos;
				value = 1;
			}
			else{
				value = valueOfLiteral+1;
			}
		}
		else if(opType == IASTBinaryExpression.op_lessEqual){
			isFirstVarPos = !isLeftVarPos;
			if(rightVariableId >=0){
				isSecondVarPos = isRightVarPos;
				value = 0;
			}
			else{
				value = 0 - valueOfLiteral;
			}
		}
		else if(opType == IASTBinaryExpression.op_lessThan){
			isFirstVarPos = !isLeftVarPos;
			if(rightVariableId >=0){
				isSecondVarPos = isRightVarPos;
				value = 1;
			}
			else{
				value = 1 - valueOfLiteral;
			}
		}
		// (a == b) is same as (a <= b) and (a >= b)
		else if(opType == IASTBinaryExpression.op_equals){
			propagateBooleanExpression(octElement, IASTBinaryExpression.op_lessEqual, leftVariableId, rightVariableId, isLeftVarPos, isRightVarPos, valueOfLiteral);
			propagateBooleanExpression(octElement, IASTBinaryExpression.op_greaterEqual, leftVariableId, rightVariableId, isLeftVarPos, isRightVarPos, valueOfLiteral);
		}

		else if(opType == IASTBinaryExpression.op_notequals){
			if(rightVariableId >=0){
				handleNonEquality(octElement, leftVariableId, rightVariableId, isLeftVarPos, isRightVarPos, 0);
			}
			else{
				handleNonEquality(octElement, leftVariableId, rightVariableId, isLeftVarPos, isRightVarPos, valueOfLiteral);
			}
			return;
		}
		else{
			throw new OctagonTransferException("Unhandled case");
		}

		handleBooleanExpression(octElement, leftVariableId, rightVariableId, isFirstVarPos, isSecondVarPos, value);
	}


	/** Handles expressions of form +- x +- y <= 2.
	 * Final phase of delegation for assumption expressions.
	 * The element is updated in this method.
	 * @param octElement element to be modified
	 * @param FirstVariableId id of the lefthand side variable of comparison, e.g. if condition is a == b, this is a's id on variable map
	 * @param SecondVariableId id of the righthand side variable of comparison, e.g. if condition is a == b, this i 's id on variable map, -1 if righthand side variable is a single literal 
	 * @param isFirstVarPos sign of the left variable, if variable is -a, then false
	 * @param isSecondVarPos sign of the right variable, if variable is -a, then false
	 * @param valueOfLiteral the value of the second variable if it is a literal, e.g. a < 9, alternatively can be used to represent cases such as a < b + 6, then the value will be 6
	 * @see octagon.VariableMap
	 * @see octagon.LibraryAccess
	 */
	private void handleBooleanExpression(OctElement octElement,
			int FirstVariableId, int SecondVariableId, boolean isFirstVarPos, 
			boolean isSecondVarPos, double valueOfLiteral) {
		Num[] array = new Num[octElement.getNumberOfVars()+1];

		for(int i=0; i<array.length-1; i++){
			array[i] = new Num(0);
		}

		double val = 0 - valueOfLiteral;
		array[array.length-1] = new Num(val);
		if(isFirstVarPos)
		{
			array[FirstVariableId] = new Num(1);
		}
		else if(!isFirstVarPos)
		{
			array[FirstVariableId] = new Num(-1);
		}
		// If we have a second variable
		if(SecondVariableId >= 0){
			if(isSecondVarPos)
			{
				array[SecondVariableId] = new Num(1);
			}
			else if(!isSecondVarPos)
			{
				array[SecondVariableId] = new Num(-1);
			}
		}
		// update the abstract element
		octElement.update(LibraryAccess.addConstraint(octElement, array));
	}

	/** Handles not equals operation, e.g. a!=b
	 * Basically, converts uses the fact that a!= can be interpreted as
	 * (a > b) or (b > a), updates the element based on two facts and finally combine
	 * them 
	 * @param octElement element to be modified
	 * @param FirstVariableId id of the lefthand side variable of comparison, e.g. if condition is a == b, this is a's id on variable map
	 * @param SecondVariableId id of the righthand side variable of comparison, e.g. if condition is a == b, this i 's id on variable map, -1 if righthand side variable is a single literal 
	 * @param isFirstVarPos sign of the left variable, if variable is -a, then false
	 * @param isSecondVarPos sign of the right variable, if variable is -a, then false
	 * @param valueOfLiteral the value of the second variable if it is a literal, e.g. a < 9, alternatively can be used to represent cases such as a < b + 6, then the value will be 6
	 * @see octagon.VariableMap
	 * @see octagon.LibraryAccess
	 */
	private void handleNonEquality(OctElement octElement,
			int FirstVariableId, int SecondVariableId, boolean isFirstVarPos, 
			boolean isSecondVarPos, double valueOfLiteral) {
		Num[] array1 = new Num[octElement.getNumberOfVars()+1];
		Num[] array2 = new Num[octElement.getNumberOfVars()+1];

		for(int i=0; i<array1.length-1; i++){
			array1[i] = new Num(0);
			array2[i] = new Num(0);
		}

		if(isFirstVarPos)
		{
			array1[FirstVariableId] = new Num(-1);
			array2[FirstVariableId] = new Num(1);
		}
		else
		{
			array1[FirstVariableId] = new Num(1);
			array2[FirstVariableId] = new Num(-1);
		}
		// If we have a second variable
		if(SecondVariableId >= 0){
			if(isSecondVarPos)
			{
				array1[SecondVariableId] = new Num(1);
				array2[SecondVariableId] = new Num(-1);
			}
			else
			{
				array1[SecondVariableId] = new Num(-1);
				array2[SecondVariableId] = new Num(1);
			}
		}

		double val1 = valueOfLiteral - 1;
		array1[array1.length-1] = new Num(val1);

		double val2 = -1 - valueOfLiteral;
		array2[array1.length-1] = new Num(val2);

		OctElement oct1 = LibraryAccess.addConstraint(octElement, array1);
		OctElement oct2 = LibraryAccess.addConstraint(octElement, array2);
		// combine two elements
		OctElement finalElement = LibraryAccess.union(oct1, oct2);
		// update the element with the final element
		octElement.update(finalElement);
	}

	/**
	 * Decides on the type of the unary operation and converts it corresponding binary operation
	 * e.g. a++ is handled as a = a + 1;
	 * @param octElement element to be updated
	 * @param expression statement expression
	 * @param cfaEdge statement edge
	 * @throws OctagonTransferException
	 */
	private void handleUnaryExpression(OctElement octElement,
			IASTExpression expression, CFAEdge cfaEdge) throws OctagonTransferException {
		String functionName = cfaEdge.getPredecessor().getFunctionName();
		IASTUnaryExpression unaryExpression = (IASTUnaryExpression) expression;

		int operator = unaryExpression.getOperator ();

		String lParam = unaryExpression.getOperand ().getRawSignature ();
		int variableId = octElement.getVariableId(globalVars, lParam, functionName);

		// a++, ++a
		if (operator == IASTUnaryExpression.op_postFixIncr || 
				operator == IASTUnaryExpression.op_prefixIncr 
		)
		{
			addLiteralToVariable(octElement, cfaEdge, lParam, variableId, true, 1.0);
		}
		// a--, --a
		else if(operator == IASTUnaryExpression.op_prefixDecr ||
				operator == IASTUnaryExpression.op_postFixDecr)
		{
			addLiteralToVariable(octElement, cfaEdge, lParam, variableId, true, -1.0);
		}

		else
		{
			throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
		}
	}

	/**
	 * assignment of a binary operation such as a = a + b,; or a += b;
	 * @param octElement element to be updated
	 * @param expression statement expression
	 * @param cfaEdge statement edge
	 * @throws OctagonTransferException
	 */
	private void handleBinaryExpression(OctElement octElement, IASTExpression expression, CFAEdge cfaEdge) throws OctagonTransferException {

		IASTBinaryExpression binaryExpression = (IASTBinaryExpression) expression;
		switch (binaryExpression.getOperator ())
		{
		// a = ?
		case IASTBinaryExpression.op_assign:
		{
			handleAssignment(octElement, binaryExpression, cfaEdge);
			break;
		}
		// a += 2
		case IASTBinaryExpression.op_plusAssign:
		case IASTBinaryExpression.op_minusAssign:
		case IASTBinaryExpression.op_multiplyAssign:
		{
			handleOperationAndAssign(octElement, binaryExpression, cfaEdge);
			break;
		}
		default: throw new OctagonTransferException("Unhandled case ");
		}
	}

	/**
	 * handles assignment of a binary operations of form a += b;
	 * organized and delegates to other methods
	 * @param octElement element to be updated
	 * @param binaryExpression binary expression
	 * @param cfaEdge statement edge
	 * @throws OctagonTransferException
	 */
	private void handleOperationAndAssign(OctElement octElement,
			IASTBinaryExpression binaryExpression, CFAEdge cfaEdge) throws OctagonTransferException {
		String functionName = cfaEdge.getPredecessor().getFunctionName();
		IASTExpression op1 = binaryExpression.getOperand1();
		IASTExpression op2 = binaryExpression.getOperand2();
		int typeOfOperator = binaryExpression.getOperator();

		// First operand is not an id expression
		if (!(op1 instanceof IASTIdExpression))
		{
			System.out.println("First operand is not a proper variable");
			throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
		}
		// If first operand is an id expression
		else if(op1 instanceof IASTIdExpression)
		{
			IASTIdExpression lvar = ((IASTIdExpression)op1);
			String nameOfLVar = lvar.getRawSignature();
			int varLid = octElement.getVariableId(globalVars, nameOfLVar, functionName);
			// a op= 2
			if(op2 instanceof IASTLiteralExpression){
				double val = Double.valueOf(op2.getRawSignature()).doubleValue();
				// only if literal is intefer or double
				int typeOfLiteral = ((IASTLiteralExpression)op2).getKind();
				if( typeOfLiteral ==  IASTLiteralExpression.lk_integer_constant || 
						typeOfLiteral == IASTLiteralExpression.lk_float_constant)
				{
					// a += 2
					if(typeOfOperator == IASTBinaryExpression.op_plusAssign){
						addLiteralToVariable(octElement, cfaEdge, nameOfLVar, varLid, true, val);
					}
					// a -= 2
					else if(typeOfOperator == IASTBinaryExpression.op_minusAssign){
						double negVal = 0 - val;
						addLiteralToVariable(octElement, cfaEdge, nameOfLVar, varLid, true, negVal);
					}
					// a *= 2
					else if(typeOfOperator == IASTBinaryExpression.op_multiplyAssign){
						multiplyLiteralWithVariable(octElement, cfaEdge, nameOfLVar, varLid, true, val);
					}
					else{
						throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
					}
				}
				else{
					throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
				}
			}
			// a op= b
			else if(op2 instanceof IASTIdExpression){

				IASTIdExpression rvar = ((IASTIdExpression)op2);
				String nameOfRVar = rvar.getRawSignature();
				int varRid = octElement.getVariableId(globalVars, nameOfRVar, functionName);
				// a += b
				if(typeOfOperator == IASTBinaryExpression.op_plusAssign){
					addTwoVariables(octElement, cfaEdge, nameOfLVar, varLid, varRid, true, true);
				}
				// a -= b
				else if(typeOfOperator == IASTBinaryExpression.op_minusAssign){
					addTwoVariables(octElement, cfaEdge, nameOfLVar, varLid, varRid, true, false);
				}
				// a *= b
				else if(typeOfOperator == IASTBinaryExpression.op_multiplyAssign){
					octElement.update(LibraryAccess.forget(octElement, varLid));
				}

				else{
					throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
				}
			}
			else{
				throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
			}
		}
		else{
			throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
		}
	}

	/**
	 * handles assignment of a single value such as a = b, a = 8, a = (cast) b
	 * organized and delegates to other methods
	 * @param octElement element to be updated
	 * @param binaryExpression binary expression
	 * @param cfaEdge statement edge
	 * @throws OctagonTransferException
	 */
	private void handleAssignment(OctElement octElement, IASTBinaryExpression binaryExpression, 
			CFAEdge cfaEdge) throws OctagonTransferException {

		String functionName = cfaEdge.getPredecessor().getFunctionName();
		IASTExpression op1 = binaryExpression.getOperand1();
		IASTExpression op2 = binaryExpression.getOperand2();

		// First operand is not an id expression
		if (!(op1 instanceof IASTIdExpression))
		{
			throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
		}
		// If first operand is an id expression
		else if(op1 instanceof IASTIdExpression)
		{
			// a = 8.2
			if(op2 instanceof IASTLiteralExpression){
				handleLiteralAssignment(octElement, op1, op2, functionName);
			}
			// a = b
			else if (op2 instanceof IASTIdExpression){
				handleVariableAssignment(octElement, op1, op2, functionName);
			}
			// a = (cast) ?
			else if(op2 instanceof IASTCastExpression) {
				handleCasting(octElement, op1, op2, cfaEdge);
			}
			// a = b op c
			else if(op2 instanceof IASTBinaryExpression){
				handleAssignmentOfBinaryExp(octElement, op1, op2, cfaEdge);
			}
			// a = -b
			else if(op2 instanceof IASTUnaryExpression){
				IASTUnaryExpression unaryExp = (IASTUnaryExpression)op2;
				handleUnaryExpAssignment(octElement, op1, unaryExp, cfaEdge);
			}
			else{
				throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
			}
		}
		else{
			throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
		}
	}

	/** 
	 * Final method for delegated unary expression assignment of form a = -b
	 * @param octElement element to be updated
	 * @param op1 left hand side of the assignment as {@link IASTExpression}
	 * @param op2 left hand side of the assignment as {@link IASTExpression}
	 * @param cfaEdge unary assignment edge
	 * @throws OctagonTransferException
	 */
	private void handleUnaryExpAssignment(OctElement octElement,
			IASTExpression op1, IASTUnaryExpression op2, CFAEdge cfaEdge) throws OctagonTransferException {
		String functionName = cfaEdge.getPredecessor().getFunctionName();
		String lParam = op1.getRawSignature ();

		String nameOfVar = op2.getOperand().getRawSignature();
		int operatorType = op2.getOperator();
		// a = -b
		if(operatorType == IASTUnaryExpression.op_minus){
			int lvar = octElement.getVariableId(globalVars, lParam, functionName);
			int var2 = octElement.getVariableId(globalVars, nameOfVar, functionName);
			Num[] array = new Num[octElement.getNumberOfVars()+1];

			for(int i=0; i<array.length-1; i++){
				array[i] = new Num(0);
			}
			array[array.length-1] = new Num(0);
			array[var2] = new Num(-1);
			octElement.update(LibraryAccess.assignVar(octElement, lvar, array));
		}
		else {
			throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
		}
	}

	/** 
	 * Final method for delegated casting expression assignment of form a = (cast) b
	 * @param octElement element to be updated
	 * @param op1 left hand side of the assignment as {@link IASTExpression}
	 * @param op2 left hand side of the assignment as {@link IASTExpression}
	 * @param cfaEdge unary assignment edge
	 * @throws OctagonTransferException
	 */
	private void handleCasting(OctElement octElement, IASTExpression op1,
			IASTExpression op2, CFAEdge cfaEdge) throws OctagonTransferException {
		String functionName = cfaEdge.getPredecessor().getFunctionName();
		String lParam = op1.getRawSignature ();

		IASTExpression castOp = (((IASTCastExpression)op2).getOperand());

		// Only casting to double is valid
		if((((IASTCastExpression)op2).getTypeId().getRawSignature()).contains("double") ||
				(((IASTCastExpression)op2).getTypeId().getRawSignature()).contains("float")){
			// double a = (double) 7
			if(castOp instanceof IASTLiteralExpression){
				int typeOfCastLiteral = ((IASTLiteralExpression)castOp).getKind();
				// cast an integer or double
				if( typeOfCastLiteral ==  IASTLiteralExpression.lk_integer_constant || 
						typeOfCastLiteral == IASTLiteralExpression.lk_float_constant)
				{
					int lvar = octElement.getVariableId(globalVars, lParam, functionName);
					double val = Double.valueOf(castOp.getRawSignature()).doubleValue();
					Num n = new Num(val);
					Num[] array = new Num[octElement.getNumberOfVars()+1];

					for(int i=0; i<array.length-1; i++){
						array[i] = new Num(0);
					}
					array[array.length-1] = n;
					octElement.update(LibraryAccess.assignVar(octElement, lvar, array));
				}
			}
			// For handling expressions of form a = (double) b
			else if(castOp instanceof IASTIdExpression){
				IASTIdExpression varId = ((IASTIdExpression)castOp);
				String nameOfVar = varId.getRawSignature();
				int lvar = octElement.getVariableId(globalVars, lParam, functionName);
				int var2 = octElement.getVariableId(globalVars, nameOfVar, functionName);
				Num[] array = new Num[octElement.getNumberOfVars()+1];

				for(int i=0; i<array.length-1; i++){
					array[i] = new Num(0);
				}
				array[array.length-1] = new Num(0);
				array[var2] = new Num(1);
				octElement.update(LibraryAccess.assignVar(octElement, lvar, array));
			}
			else{
				throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
			}
		}
		// If we don't cast to double loose all information about the element
		else{
			int var = octElement.getVariableId(globalVars, lParam, functionName);
			octElement.update(LibraryAccess.forget(octElement, var));
		}
	}

	/** 
	 * Final method for delegated literal expression assignment of form a = 9
	 * @param octElement element to be updated
	 * @param op1 left hand side of the assignment as {@link IASTExpression}
	 * @param op2 left hand side of the assignment as {@link IASTExpression}
	 * @param functionName name of the function on the scope of the updated variable
	 * for example if a = 9 is handled, which function does a belong?
	 * @throws OctagonTransferException
	 */
	private void handleLiteralAssignment(OctElement octElement, IASTExpression op1, 
			IASTExpression op2, String functionName) throws OctagonTransferException {

		String lParam = op1.getRawSignature ();
		String rParam = op2.getRawSignature ();

		int typeOfLiteral = ((IASTLiteralExpression)op2).getKind();
		if( typeOfLiteral ==  IASTLiteralExpression.lk_integer_constant || 
				typeOfLiteral == IASTLiteralExpression.lk_float_constant)
		{
			int lvar = octElement.getVariableId(globalVars, lParam, functionName);
			double val = Double.valueOf(rParam).doubleValue();
			Num n = new Num(val);
			Num[] array = new Num[octElement.getNumberOfVars()+1];

			for(int i=0; i<array.length-1; i++){
				array[i] = new Num(0);
			}

			array[array.length-1] = n;
			octElement.update(LibraryAccess.assignVar(octElement, lvar, array));
		}
		else{
			throw new OctagonTransferException("Unhandled case");
		}
	}

	/** 
	 * Final method for delegated variable expression assignment of form a = b
	 * @param octElement element to be updated
	 * @param op1 left hand side of the assignment as {@link IASTExpression}
	 * @param op2 left hand side of the assignment as {@link IASTExpression}
	 * @param functionName name of the function on the scope of the updated variable
	 * for example if a = 9 is handled, which function does a belong?
	 * @throws OctagonTransferException
	 */
	private void handleVariableAssignment(OctElement octElement,
			IASTExpression op1, IASTExpression op2, String functionName) {

		String lParam = op1.getRawSignature ();
		IASTIdExpression varId = ((IASTIdExpression)op2);
		String nameOfVar = varId.getRawSignature();
		int lvar = octElement.getVariableId(globalVars, lParam, functionName);
		int var2 = octElement.getVariableId(globalVars, nameOfVar, functionName);
		Num[] array = new Num[octElement.getNumberOfVars()+1];

		for(int i=0; i<array.length-1; i++){
			array[i] = new Num(0);
		}
		array[array.length-1] = new Num(0);
		array[var2] = new Num(1);
		octElement.update(LibraryAccess.assignVar(octElement, lvar, array));
	}

	/** 
	 * Handles and delegates assignment of binary expressions of form a = b + c
	 * @param octElement element to be updated
	 * @param op1 left hand side of the assignment as {@link IASTExpression}
	 * @param op2 left hand side of the assignment as {@link IASTExpression}
	 * @param cfaEdge unary assignment edge
	 * @throws OctagonTransferException
	 */
	private void handleAssignmentOfBinaryExp(OctElement octElement,
			IASTExpression op1, IASTExpression op2, CFAEdge cfaEdge) throws OctagonTransferException {
		String functionName = cfaEdge.getPredecessor().getFunctionName();
		// name of the updated variable, so if a = b + c is handled, lParam is a
		String lParam = op1.getRawSignature ();
		//Binary Expression
		IASTBinaryExpression binExp = (IASTBinaryExpression) op2;
		//Right Operand of the binary expression
		IASTExpression lVarInBinaryExp = binExp.getOperand1();
		//Left Operand of the binary expression
		IASTExpression rVarInBinaryExp = binExp.getOperand2();

		switch (binExp.getOperator ())
		{
		// operand in left hand side of expression is an addition
		case IASTBinaryExpression.op_plus:
		{
			// a = -b + ?, left variable in right hand side of the expression is unary
			if(lVarInBinaryExp instanceof IASTUnaryExpression){
				IASTUnaryExpression unaryExpression = (IASTUnaryExpression) lVarInBinaryExp;
				int operator = unaryExpression.getOperator ();
				// make sure that unary expression is minus operator
				if(operator == IASTUnaryExpression.op_minus){
					IASTExpression unaryOperand = unaryExpression.getOperand();
					if(unaryOperand instanceof IASTLiteralExpression){
						double value = Double.valueOf(unaryOperand.getRawSignature()).doubleValue();
						double negVal = 0 - value;

						IASTIdExpression rvar = ((IASTIdExpression)rVarInBinaryExp);
						String nameOfRVar = rvar.getRawSignature();
						int varRid = octElement.getVariableId(globalVars, nameOfRVar, functionName);
						addLiteralToVariable(octElement, cfaEdge, lParam, varRid, true, negVal);
					}
					else{
						throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
					}
				}
				else{
					throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
				}

			}
			// a = b + ?, left variable in right hand side of the expression is a variable
			else if(lVarInBinaryExp instanceof IASTIdExpression){
				IASTIdExpression lvar = ((IASTIdExpression)lVarInBinaryExp);
				String nameOfLVar = lvar.getRawSignature();
				int varLid = octElement.getVariableId(globalVars, nameOfLVar, functionName);

				// a = b + 2
				if(rVarInBinaryExp instanceof IASTLiteralExpression){
					double val = Double.valueOf(rVarInBinaryExp.getRawSignature()).doubleValue();

					// only integers and doubles are handled
					int typeOfLiteral = ((IASTLiteralExpression)rVarInBinaryExp).getKind();
					if( typeOfLiteral ==  IASTLiteralExpression.lk_integer_constant || 
							typeOfLiteral == IASTLiteralExpression.lk_float_constant)
					{
						addLiteralToVariable(octElement, cfaEdge, lParam, varLid, true, val);
					}

					else{
						throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
					}
				}
				// a = b + c,
				else if(rVarInBinaryExp instanceof IASTIdExpression){
					IASTIdExpression rvar = ((IASTIdExpression)rVarInBinaryExp);
					String nameOfRVar = rvar.getRawSignature();
					int varRid = octElement.getVariableId(globalVars, nameOfRVar, functionName);
					addTwoVariables(octElement, cfaEdge, lParam, varLid, varRid, true, true);
				}
				else{
					throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
				}
			}

			// a = 9 + ? left variable in right hand side of the expression is a variable
			else if(lVarInBinaryExp instanceof IASTLiteralExpression){
				double val = Double.valueOf(lVarInBinaryExp.getRawSignature()).doubleValue();
				// left variable must be an integer or double value
				int typeOfLiteral = ((IASTLiteralExpression)lVarInBinaryExp).getKind();
				if( typeOfLiteral ==  IASTLiteralExpression.lk_integer_constant || 
						typeOfLiteral == IASTLiteralExpression.lk_float_constant)
				{
					// a = 8 + b
					if(rVarInBinaryExp instanceof IASTIdExpression){
						IASTIdExpression rvar = ((IASTIdExpression)rVarInBinaryExp);
						String nameOfRVar = rvar.getRawSignature();
						int varRid = octElement.getVariableId(globalVars, nameOfRVar, functionName);
						addLiteralToVariable(octElement, cfaEdge, lParam, varRid, true, val);
					}
					// a = 8 + 9
					else if(rVarInBinaryExp instanceof IASTLiteralExpression){
						//Cil eliminates this case
						throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
					}
				}
				else {
					throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
				}
			}

			else{
				throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
			}

			break;
		}

		// operand in left hand side of expression is a subtraction
		case IASTBinaryExpression.op_minus:
		{
			// a = -9 + ? left variable in right hand side of the expression is a unary expression
			if(lVarInBinaryExp instanceof IASTUnaryExpression){
				IASTUnaryExpression unaryExpression = (IASTUnaryExpression) lVarInBinaryExp;
				int operator = unaryExpression.getOperator ();
				// make sure it is minus op
				if(operator == IASTUnaryExpression.op_minus){
					IASTExpression unaryOperand = unaryExpression.getOperand();
					if(unaryOperand instanceof IASTLiteralExpression){
						double value = Double.valueOf(unaryOperand.getRawSignature()).doubleValue();
						double negVal = 0 - value;

						IASTIdExpression rvar = ((IASTIdExpression)rVarInBinaryExp);
						String nameOfRVar = rvar.getRawSignature();
						int varRid = octElement.getVariableId(globalVars, nameOfRVar, functionName);
						addLiteralToVariable(octElement, cfaEdge, lParam, varRid, false, negVal);
					}
					else{
						throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
					}
				}
				else{
					throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
				}

			}
			// a = b - ? left variable in right hand side of the expression is a variable
			else if(lVarInBinaryExp instanceof IASTIdExpression){
				IASTIdExpression lvar = ((IASTIdExpression)lVarInBinaryExp);
				String nameOfLVar = lvar.getRawSignature();
				int varLid = octElement.getVariableId(globalVars, nameOfLVar, functionName);

				// a = b - 2
				if(rVarInBinaryExp instanceof IASTLiteralExpression){
					double val = Double.valueOf(rVarInBinaryExp.getRawSignature()).doubleValue();
					double negVal = 0 - val;
					// only integers and doubles are handled
					int typeOfCastLiteral = ((IASTLiteralExpression)rVarInBinaryExp).getKind();
					if( typeOfCastLiteral ==  IASTLiteralExpression.lk_integer_constant || 
							typeOfCastLiteral == IASTLiteralExpression.lk_float_constant)
					{
						addLiteralToVariable(octElement, cfaEdge, lParam, varLid, true, negVal);
					}

					else{
						throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
					}
				}
				// a = b - c
				else if(rVarInBinaryExp instanceof IASTIdExpression){
					IASTIdExpression rvar = ((IASTIdExpression)rVarInBinaryExp);
					String nameOfRVar = rvar.getRawSignature();
					int varRid = octElement.getVariableId(globalVars, nameOfRVar, functionName);
					addTwoVariables(octElement, cfaEdge, lParam, varLid, varRid, true, false);
				}
				else{
					throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
				}
			}

			// a = 8 - ? left variable in right hand side of the expression is a literal
			else if(lVarInBinaryExp instanceof IASTLiteralExpression){
				double val = Double.valueOf(lVarInBinaryExp.getRawSignature()).doubleValue();
				int typeOfCastLiteral = ((IASTLiteralExpression)lVarInBinaryExp).getKind();
				// only integers and doubles are handled
				if( typeOfCastLiteral ==  IASTLiteralExpression.lk_integer_constant || 
						typeOfCastLiteral == IASTLiteralExpression.lk_float_constant)
				{
					// a = 8 - b
					if(rVarInBinaryExp instanceof IASTIdExpression){
						IASTIdExpression rvar = ((IASTIdExpression)rVarInBinaryExp);
						String nameOfRVar = rvar.getRawSignature();
						int varRid = octElement.getVariableId(globalVars, nameOfRVar, functionName);
						addLiteralToVariable(octElement, cfaEdge, lParam, varRid, false, val);

					}
					// a = 8 - 7
					else if(rVarInBinaryExp instanceof IASTLiteralExpression){
						//Cil eliminates this case
						throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
					}
				}
				else {
					throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
				}
			}
			else{
				throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
			}
			break;
		}

		// operand in left hand side of expression is a multiplication
		case IASTBinaryExpression.op_multiply:
		{
			// a = -2 * b
			if(lVarInBinaryExp instanceof IASTUnaryExpression){
				IASTUnaryExpression unaryExpression = (IASTUnaryExpression) lVarInBinaryExp;
				int operator = unaryExpression.getOperator ();

				if(operator == IASTUnaryExpression.op_minus){
					IASTExpression unaryOperand = unaryExpression.getOperand();
					if(unaryOperand instanceof IASTLiteralExpression){
						double value = Double.valueOf(unaryOperand.getRawSignature()).doubleValue();
						double negVal = 0 - value;

						IASTIdExpression rvar = ((IASTIdExpression)rVarInBinaryExp);
						String nameOfRVar = rvar.getRawSignature();
						int varRid = octElement.getVariableId(globalVars, nameOfRVar, functionName);

						multiplyLiteralWithVariable(octElement, cfaEdge, lParam, varRid, true, negVal);
					}
					else{
						throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
					}
				}
				else{
					throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
				}

			}
			// a = b * ?
			else if(lVarInBinaryExp instanceof IASTIdExpression){
				IASTIdExpression lvar = ((IASTIdExpression)lVarInBinaryExp);
				String nameOfLVar = lvar.getRawSignature();
				int varLid = octElement.getVariableId(globalVars, nameOfLVar, functionName);

				// a = b * 2
				if(rVarInBinaryExp instanceof IASTLiteralExpression){
					double val = Double.valueOf(rVarInBinaryExp.getRawSignature()).doubleValue();
					int typeOfCastLiteral = ((IASTLiteralExpression)rVarInBinaryExp).getKind();
					if( typeOfCastLiteral ==  IASTLiteralExpression.lk_integer_constant || 
							typeOfCastLiteral == IASTLiteralExpression.lk_float_constant)
					{
						multiplyLiteralWithVariable(octElement, cfaEdge, lParam, varLid, true, val);
					}
					else{
						throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
					}
				}
				// a = b * -2 
				else if(rVarInBinaryExp instanceof IASTUnaryExpression){
					IASTUnaryExpression unaryExpression = (IASTUnaryExpression) rVarInBinaryExp;
					int operator = unaryExpression.getOperator ();

					if(operator == IASTUnaryExpression.op_minus){
						IASTExpression unaryOperand = unaryExpression.getOperand();
						if(unaryOperand instanceof IASTLiteralExpression){
							double value = Double.valueOf(unaryOperand.getRawSignature()).doubleValue();
							double negVal = 0 - value;

							multiplyLiteralWithVariable(octElement, cfaEdge, lParam, varLid, true, negVal);
						}
						else{
							throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
						}
					}
					else{
						throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
					}

				}

				// a = b * c
				else if(rVarInBinaryExp instanceof IASTIdExpression){
					int var = octElement.getVariableId(globalVars, lParam, functionName);
					octElement.update(LibraryAccess.forget(octElement, var));
				}
				else{
					throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
				}
			}

			// a = 8 * ?
			else if(lVarInBinaryExp instanceof IASTLiteralExpression){
				double val = Double.valueOf(lVarInBinaryExp.getRawSignature()).doubleValue();
				//Num n = new Num(val);
				int typeOfCastLiteral = ((IASTLiteralExpression)lVarInBinaryExp).getKind();
				if( typeOfCastLiteral ==  IASTLiteralExpression.lk_integer_constant || 
						typeOfCastLiteral == IASTLiteralExpression.lk_float_constant)
				{
					// a = 8 * b
					if(rVarInBinaryExp instanceof IASTIdExpression){
						IASTIdExpression rvar = ((IASTIdExpression)rVarInBinaryExp);
						String nameOfRVar = rvar.getRawSignature();
						int varRid = octElement.getVariableId(globalVars, nameOfRVar, functionName);
						multiplyLiteralWithVariable(octElement, cfaEdge, lParam, varRid, true, val);

					}
					// a = 8 * 9
					else if(rVarInBinaryExp instanceof IASTLiteralExpression){
						//Cil eliminates this case
						throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
					}
				}
				else {
					throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
				}
			}

			else{
				throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
			}

			break;
		}
		// operand in left hand side of expression is a division
		case IASTBinaryExpression.op_divide:
		{
			int var = octElement.getVariableId(globalVars, lParam, functionName);
			octElement.update(LibraryAccess.forget(octElement, var));
			break;
		}

		// operand in left hand side of expression is modulo op
		case IASTBinaryExpression.op_modulo:
		{
			int var = octElement.getVariableId(globalVars, lParam, functionName);
			octElement.update(LibraryAccess.forget(octElement, var));
			break;
		}
		default: throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
		}
	}

	/**
	 * Final method for delegated binary expression assignment of form a = b + 8
	 * @param octElement element to be updated
	 * @param cfaEdge assignment edge
	 * @param leftOperator name of the left hand side variable of the assignment
	 * e.g. if operation is a = b + 8, leftOperator is a
	 * @param variableId id of the left hand side variable on the right hand side of assignment, see {@link VariableMap}
	 * e.g. if operation is a = b + 8, leftOperator is b's id. 
	 * @param isVariablePositive sign of the the left hand side variable on the right hand side of assignment
	 * e.g. if operation is a = b + 8, isVariablePositive is the sign of b, positive if true
	 * @param valueOfLiteral value of the literal, 8 for a = b + 8
	 */
	private void addLiteralToVariable(OctElement octElement,
			CFAEdge cfaEdge, String leftOperator, int variableId, 
			boolean isVariablePositive, double valueOfLiteral) {

		String functionName = cfaEdge.getPredecessor().getFunctionName();
		Num n = new Num(valueOfLiteral);

		int lvar = octElement.getVariableId(globalVars, leftOperator, functionName);
		Num[] array = new Num[octElement.getNumberOfVars()+1];

		for(int i=0; i<array.length-1; i++){
			array[i] = new Num(0);
		}

		array[array.length-1] = n;
		if(isVariablePositive){
			array[variableId] = new Num(1);
		}
		else
			array[variableId] = new Num(-1);
		octElement.update(LibraryAccess.assignVar(octElement, lvar, array));
	}

	/**
	 * Final method for delegated binary expression assignment of form a = b + c
	 * @param octElement element to be updated
	 * @param cfaEdge assignment edge
	 * @param leftOperator name of the left hand side variable of the assignment
	 * e.g. if operation is a = b + c, leftOperator is a
	 * @param LvariableId id of the left hand side variable on the right hand side of assignment, see {@link VariableMap}
	 * e.g. if operation is a = b + c, leftOperator is b's id.
	 * @param RvariableId id of the right hand side variable on the right hand side of assignment, see {@link VariableMap}
	 * e.g. if operation is a = b + c, leftOperator is c's id.
	 * @param isLVariablePositive sign of the the left hand side variable on the right hand side of assignment
	 * e.g. if operation is a = b + c, isVariablePositive is the sign of b, positive if true
	 * @param isRVariablePositive sign of the the left hand side variable on the right hand side of assignment
	 * e.g. if operation is a = b + c, isVariablePositive is the sign of c, positive if true
	 */
	private void addTwoVariables(OctElement octElement,
			CFAEdge cfaEdge, String leftOperator, int LvariableId, int RvariableId,
			boolean isLVariablePositive, boolean isRVariablePositive) {

		String functionName = cfaEdge.getPredecessor().getFunctionName();
		int lvar = octElement.getVariableId(globalVars, leftOperator, functionName);
		Num[] array = new Num[octElement.getNumberOfVars()+1];

		for(int i=0; i<array.length-1; i++){
			array[i] = new Num(0);
		}

		array[array.length-1] = new Num(0);
		if(isLVariablePositive){
			array[LvariableId] = new Num(1);
		}
		else 
			array[LvariableId] = new Num(-1);

		if(isRVariablePositive){
			array[RvariableId] = new Num(1);
		}
		else 
			array[RvariableId] = new Num(-1);
		octElement.update(LibraryAccess.assignVar(octElement, lvar, array));
	}

	/**
	 * Final method for delegated binary expression assignment of form a = 2 * b
	 * @param octElement element to be updated
	 * @param cfaEdge assignment edge
	 * @param leftParam name of the left hand side variable of the assignment
	 * e.g. if operation is a = 8 * b, leftOperator is a
	 * @param variableId id of the left hand side variable on the right hand side of assignment, see {@link VariableMap}
	 * e.g. if operation is a = 8 * b, leftOperator is b's id. 
	 * @param isVariablePositive sign of the the left hand side variable on the right hand side of assignment
	 * e.g. if operation is a = 8 * b, isVariablePositive is the sign of b, positive if true
	 * @param valueOfLiteral value of the literal, 2 for a = 2 * b
	 */

	private void multiplyLiteralWithVariable(OctElement octElement,
			CFAEdge cfaEdge, String leftParam, int variableId, boolean isVariablePositive, double valueOfLiteral) {

		String functionName = cfaEdge.getPredecessor().getFunctionName();
		int lvar = octElement.getVariableId(globalVars, leftParam, functionName);
		Num[] array = new Num[octElement.getNumberOfVars()+1];

		for(int i=0; i<array.length-1; i++){
			array[i] = new Num(0);
		}

		array[array.length-1] = new Num(0);
		if(isVariablePositive){
			Num n = new Num(valueOfLiteral);
			array[variableId] = n;
		}
		else{
			Num n = new Num(0 - valueOfLiteral);
			array[variableId] = n;
		}
		octElement.update(LibraryAccess.assignVar(octElement, lvar, array));
	}

	private void handleFunctionCall(OctElement octElement, 
			FunctionCallEdge callEdge) throws OctagonTransferException {
		FunctionDefinitionNode functionEntryNode = (FunctionDefinitionNode)callEdge.getSuccessor();
		String calledFunctionName = functionEntryNode.getFunctionName();
		String callerFunctionName = callEdge.getPredecessor().getFunctionName();

		List<String> paramNames = functionEntryNode.getFunctionParameterNames();
		IASTExpression[] arguments = callEdge.getArguments();

		assert (paramNames.size() == arguments.length);

		OctElement newOctElement = new OctElement();

		int noOfAddedVars = 0;
		for(String globalVar:globalVars){
			if(newOctElement.addVar(globalVar, "")){
				noOfAddedVars++;
			}
		}
		if(noOfAddedVars > 0)
			newOctElement.update(LibraryAccess.addDimension(octElement, noOfAddedVars));

		noOfAddedVars = 0;
		for(String paramName:paramNames){
			if(newOctElement.addVar(paramName, calledFunctionName)){
				noOfAddedVars++;
			}
		}
		if(noOfAddedVars > 0)
			newOctElement.update(LibraryAccess.addDimension(octElement, noOfAddedVars));

		HashMap<Integer, Integer> replacementMap = new HashMap<Integer, Integer>();

		for(String globalVar:globalVars){
			int id1 = octElement.getVariableId(globalVars, globalVar, callerFunctionName);
			int id2 = newOctElement.getVariableId(globalVars, globalVar, calledFunctionName);
			replacementMap.put(id1, id2);
		}

		for(int i=0; i<arguments.length; i++){
			IASTExpression arg = arguments[i];

			if(arg instanceof IASTIdExpression){
				IASTIdExpression idExp = (IASTIdExpression) arg;
				String nameOfArg = idExp.getRawSignature();
				String nameOfParam = paramNames.get(i);
				int id1 = octElement.getVariableId(globalVars, nameOfArg, callerFunctionName);
				int id2 = newOctElement.getVariableId(globalVars, nameOfParam, calledFunctionName);
				replacementMap.put(id1, id2);
			}

			else if(arg instanceof IASTLiteralExpression){
				IASTLiteralExpression literalExp = (IASTLiteralExpression) arg;
				String stringValOfArg = literalExp.getRawSignature();

				int typeOfLiteral = literalExp.getKind();
				if( typeOfLiteral ==  IASTLiteralExpression.lk_integer_constant || 
						typeOfLiteral == IASTLiteralExpression.lk_float_constant)
				{
					String paramName = paramNames.get(i);
					int lvar = octElement.getVariableId(globalVars, paramName, calledFunctionName);
					double val = Double.valueOf(stringValOfArg).doubleValue();
					Num n = new Num(val);
					Num[] array = new Num[octElement.getNumberOfVars()+1];

					for(int j=0; j<array.length-1; j++){
						array[j] = new Num(0);
					}

					array[array.length-1] = n;
					octElement.update(LibraryAccess.assignVar(octElement, lvar, array));
				}
				else{
					throw new OctagonTransferException("Unhandled case");
				}
			}
			else{
				throw new OctagonTransferException("Unhandled case");
			}
		}

		List<Integer> replacedValues = new ArrayList<Integer>();
		replacedValues.addAll(replacementMap.keySet());

		for(int i=0; i<replacedValues.size(); i++){
			int val1id = replacedValues.get(i);
			for(int j=i; j<replacedValues.size(); j++){
				int val2id = replacedValues.get(j);
				copyConstraintFromOctagon(octElement, newOctElement, val1id, val2id, 
						replacementMap.get(val1id).intValue(), replacementMap.get(val2id).intValue());
			}
		}

	}

	private void copyConstraintFromOctagon(OctElement octElement,
			OctElement newOctElement, int val1id, int val2id, int newVal1,
			int newVal2) {

		Octagon oct = octElement.getOctagon();
		Num num;
		Num[] array = new Num[newOctElement.getNumberOfVars()+1];
		fillArrayWithZero(array);

		if(val1id == val2id){
			// TODO
			if (oct.getMatrix()[oct.matPos(2*val1id,2*val1id)].f > 0) {
				assert(false);
				//s = s + "\n  " + varName + "-" + varName + " <= " + oct.getMatrix()[oct.matPos(2*i, 2*i)].f;
			}
			// TODO
			if (oct.getMatrix()[oct.matPos(2*val1id+1,2*val1id+1)].f > 0) {
				assert(false);
				//s = s + "\n  " + "-"+ varName + "+" + varName + " <= " + oct.getMatrix()[oct.matPos(2*i+1,2*i+1)].f;
			}

			num = new Num((oct.getMatrix()[oct.matPos(2*val1id+1,2*val1id)].f)/2);
			array[newVal1] = new Num(-1);
			array[newOctElement.getNumberOfVars()] = num;
			newOctElement.update(LibraryAccess.addConstraint(newOctElement, array));

			fillArrayWithZero(array);
			num = new Num((oct.getMatrix()[oct.matPos(2*val1id,2*val1id+1)].f)/2);
			array[newVal1] = new Num(1);
			array[newOctElement.getNumberOfVars()] = num;
			newOctElement.update(LibraryAccess.addConstraint(newOctElement, array));
		}
		else{

			num = new Num((oct.getMatrix()[oct.matPos(2*val2id,2*val1id)].f));
			array[newVal1] = new Num(-1);
			array[newVal2] = new Num(1);
			array[newOctElement.getNumberOfVars()] = num;
			newOctElement.update(LibraryAccess.addConstraint(newOctElement, array));

			fillArrayWithZero(array);
			num = new Num((oct.getMatrix()[oct.matPos(2*val2id,2*val1id+1)].f));
			array[newVal1] = new Num(1);
			array[newVal2] = new Num(1);
			array[newOctElement.getNumberOfVars()] = num;
			newOctElement.update(LibraryAccess.addConstraint(newOctElement, array));

			fillArrayWithZero(array);
			num = new Num((oct.getMatrix()[oct.matPos(2*val2id+1,2*val1id)].f));
			array[newVal1] = new Num(-1);
			array[newVal2] = new Num(-1);
			array[newOctElement.getNumberOfVars()] = num;
			newOctElement.update(LibraryAccess.addConstraint(newOctElement, array));

			fillArrayWithZero(array);
			num = new Num((oct.getMatrix()[oct.matPos(2*val2id+1,2*val1id+1)].f));
			array[newVal1] = new Num(1);
			array[newVal2] = new Num(-1);
			array[newOctElement.getNumberOfVars()] = num;
			newOctElement.update(LibraryAccess.addConstraint(newOctElement, array));
		}
	}

	private void fillArrayWithZero(Num[] array) {
		for(int i=0; i<array.length; i++){
			array[i] = new Num(0);
		}
	}

	//	TODO implement again
	/**
	 * Handles calls to external function calls. Basically we drop
	 * all information about the variable that gets the value of this
	 * function call. If the external call modifies the state, a stub
	 * should be provided.
	 * @param octElement Abstract element to be updated.
	 * @param functionCallEdge Call edge to the external function.
	 * @throws OctagonTransferException 
	 */
	private void handleExternalFunctionCall(OctElement octElement,
			FunctionCallEdge functionCallEdge) throws OctagonTransferException {

//		// get the summary edge from call site to return site
//		IASTExpression expr = functionCallEdge.getSuccessor().getEnteringSummaryEdge().getExpression();
//		String functionName = functionCallEdge.getPredecessor().getFunctionName();
//		if(expr instanceof IASTBinaryExpression){
//		IASTBinaryExpression binaryExpression = (IASTBinaryExpression) expr;
//		IASTExpression leftHandSideExp = binaryExpression.getOperand1();
//		if(leftHandSideExp instanceof IASTIdExpression){
//		IASTIdExpression variable = (IASTIdExpression)leftHandSideExp;
//		String variableName = variable.getRawSignature();
//		// TODO fix
//		int varid = octElement.getVarId(variableName);
//		octElement.update(LibraryAccess.forget(octElement, varid));
//		}
//		else if(leftHandSideExp instanceof IASTUnaryExpression){
//		System.out.println("Unary" + expr.getRawSignature());
//		assert(false);
//		}
//		else {
//		throw new OctagonTransferException("Unhandled case " + functionCallEdge.getPredecessor().getNodeNumber());
//		}
//		}
//		else if(expr instanceof IASTFunctionCallExpression){
//		return;
//		}
//		else{
//		throw new OctagonTransferException("Unhandled case " + functionCallEdge.getPredecessor().getNodeNumber());
//		}
	}


	private void handleExitFromFunction(OctElement octElement,
			IASTExpression expression, StatementEdge statementEdge) throws OctagonTransferException {

		String functionName = statementEdge.getPredecessor().getFunctionName();

		if(octElement.addVar("___cpa_temp_result_var_", functionName)){
			octElement.update(LibraryAccess.addDimension(octElement, 1));
		}

		if(expression instanceof IASTUnaryExpression){
			IASTUnaryExpression unaryExp = (IASTUnaryExpression)expression;
			if(unaryExp.getOperator() == IASTUnaryExpression.op_bracketedPrimary){
				IASTExpression exprInParanhesis = unaryExp.getOperand();
				if(exprInParanhesis instanceof IASTLiteralExpression){
					IASTLiteralExpression litExpr = (IASTLiteralExpression)exprInParanhesis;

					int resultvarID = octElement.getVariableId(globalVars, "___cpa_temp_result_var_", functionName);
					String literalValue = litExpr.getRawSignature ();
					int typeOfLiteral = (litExpr.getKind());
					if( typeOfLiteral ==  IASTLiteralExpression.lk_integer_constant || 
							typeOfLiteral == IASTLiteralExpression.lk_float_constant)
					{
						double val = Double.valueOf(literalValue).doubleValue();
						Num n = new Num(val);
						Num[] array = new Num[octElement.getNumberOfVars()+1];

						for(int j=0; j<array.length-1; j++){
							array[j] = new Num(0);
						}
						array[array.length-1] = n;
						octElement.update(LibraryAccess.assignVar(octElement, resultvarID, array));
					}
				}

				else if(exprInParanhesis instanceof IASTIdExpression){
					System.out.println(octElement);
					IASTIdExpression idExpr = (IASTIdExpression)exprInParanhesis;

					int lvar = octElement.getVariableId(globalVars, "___cpa_temp_result_var_", functionName);
					String idExpName = idExpr.getRawSignature ();

					int rvar = octElement.getVariableId(globalVars, idExpName, functionName);
					Num[] array = new Num[octElement.getNumberOfVars()+1];

					for(int i=0; i<array.length-1; i++){
						array[i] = new Num(0);
					}

					array[array.length-1] = new Num(0);
					array[rvar] = new Num(1);
					octElement.update(LibraryAccess.assignVar(octElement, lvar, array));
				}
				else{
					throw new OctagonTransferException("Unhandled case");
				}
			}
		}
		else if(expression instanceof IASTBinaryExpression){
			throw new OctagonTransferException("Unhandled case");
		}
		else {
			throw new OctagonTransferException("Unhandled case");
		}
	}

	private void handleFunctionReturn(OctElement octElement,
			ReturnEdge functionReturnEdge) throws OctagonTransferException {
		CallToReturnEdge summaryEdge = 
			functionReturnEdge.getSuccessor().getEnteringSummaryEdge();
		IASTExpression exprOnSummary = summaryEdge.getExpression();
		OctElement prevOctElem = (OctElement)summaryEdge.extractAbstractElement("OctElement");
		String callerFunctionName = functionReturnEdge.getSuccessor().getFunctionName();
		String calledFunctionName = functionReturnEdge.getPredecessor().getFunctionName();
octElement.update(prevOctElem);
//		HashMap<Integer, Integer> replaceMap = new HashMap<Integer, Integer>();
//
//		// TODO add previous element a temporary return value
//
//		// expression is a binary operation, e.g. a = g(b);
//		if (exprOnSummary instanceof IASTBinaryExpression) {
//			IASTBinaryExpression binExp = ((IASTBinaryExpression)exprOnSummary);
//			int opType = binExp.getOperator ();
//
//			assert(opType == IASTBinaryExpression.op_assign);
//
//			IASTExpression op1 = binExp.getOperand1();
//
//			// we expect left hand side of the expression to be a variable
//			if(op1 instanceof IASTIdExpression)
//			{
//				IASTIdExpression leftHandSideVar = (IASTIdExpression)op1;
//				String varName = leftHandSideVar.getRawSignature();
//				// TODO
//				int varId1 = prevOctElem.getVariableId(globalVars, varName, callerFunctionName);
//				int varId2 = octElement.getVariableId(globalVars, "___cpa_temp_result_var_", calledFunctionName);
//				replaceMap.put(varId1, varId2);
//			}
//			else{
//				throw new OctagonTransferException("Unhandled case " + functionReturnEdge.getPredecessor().getNodeNumber());
//			}
//		}
//		// expression is a unary operation, e.g. g(b);
//		else if (exprOnSummary instanceof IASTUnaryExpression)
//		{
//			// TODO
//			// do nothing
//		}
//		else{
//			throw new OctagonTransferException("Unhandled case " + functionReturnEdge.getPredecessor().getNodeNumber());
//		}
//
//
//		for(int i=0; i<octElement.getNumberOfVars(); i++)
//		{
//			String varName = octElement.getVarNameForId(i);
//			String tempVarName = varName.replace("::", "");
//			if(globalVars.contains(tempVarName)){
//				int varId1 = prevOctElem.getVariableId(globalVars, tempVarName, "");
//				int varId2 = octElement.getVariableId(globalVars, tempVarName, "");
//				replaceMap.put(varId1, varId2);
//			}
//		}
//		System.out.println(replaceMap);
	}

}
