package cpaplugin.cpa.cpas.octagon;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import octagon.LibraryAccess;
import octagon.Num;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;

import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.CFAExitNode;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cfa.objectmodel.c.AssumeEdge;
import cpaplugin.cfa.objectmodel.c.CallToReturnEdge;
import cpaplugin.cfa.objectmodel.c.DeclarationEdge;
import cpaplugin.cfa.objectmodel.c.FunctionCallEdge;
import cpaplugin.cfa.objectmodel.c.FunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.c.ReturnEdge;
import cpaplugin.cfa.objectmodel.c.StatementEdge;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.exceptions.CPAException;
import cpaplugin.exceptions.OctagonTransferException;

public class OctTransferRelation implements TransferRelation{

	private OctDomain octDomain;

	public OctTransferRelation (OctDomain octDomain)
	{
		this.octDomain = octDomain;
	}

	public AbstractDomain getAbstractDomain ()
	{
		return octDomain;
	}

	public AbstractElement getAbstractSuccessor (AbstractElement element, CFAEdge cfaEdge)
	{
		System.out.println(" EDGE "+ cfaEdge.getRawStatement());
		OctElement octElement = (OctElement) element;
		switch (cfaEdge.getEdgeType ())
		{
		case StatementEdge:
		{
			octElement = octElement.clone ();

			StatementEdge statementEdge = (StatementEdge) cfaEdge;
			IASTExpression expression = statementEdge.getExpression ();

			// handling function return
			if(statementEdge.isJumpEdge()){
				try {
					handleExitFromFunction(octElement, expression, statementEdge);
				} catch (OctagonTransferException e) {
					e.printStackTrace();
				}
			}

			else{
				try {
					handleStatement (octElement, expression, cfaEdge);
				} catch (OctagonTransferException e) {
					e.printStackTrace();
				}
			}
			break;
		}

		case DeclarationEdge:
		{
			octElement = octElement.clone ();

			DeclarationEdge declarationEdge = (DeclarationEdge) cfaEdge;
			IASTDeclarator [] declarators = declarationEdge.getDeclarators ();
			handleDeclaration (octElement, declarators, cfaEdge);
			break;
		}

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
			System.out.println("Blank Edge -- Do Nothing");
			break;
		}

		case FunctionCallEdge: 
		{
			octElement = octElement.clone ();
			FunctionCallEdge functionCallEdge = (FunctionCallEdge) cfaEdge;

			if(functionCallEdge.isRecursive()){
				handleRecursiveFunctionCall(octElement, functionCallEdge);
			}
			else{
				handleFunctionCall(octElement, functionCallEdge);
			}
			break;
		}

		case ReturnEdge:
		{
			octElement = octElement.clone ();
			ReturnEdge exitEdge = (ReturnEdge) cfaEdge;
			CFANode predecessorNode = exitEdge.getPredecessor();
			CFANode successorNode = exitEdge.getSuccessor();
			String pfName = predecessorNode.getFunctionName();
			String callerFunctionName = successorNode.getFunctionName();

			CallToReturnEdge summaryEdge = (CallToReturnEdge)successorNode.getEnteringSummaryEdge();
			IASTBinaryExpression expression = (IASTBinaryExpression)summaryEdge.getExpression();
			String varName = ((IASTIdExpression)expression.getOperand1()).getRawSignature();

			if(exitEdge.isExitingRecursiveCall()){
				// TODO recursive return
				handleSummaryEdge(octElement, varName, pfName, callerFunctionName);
			}
			else if(predecessorNode.getFunctionName().compareTo("main") == 0){
				//Do nothing
			}
			else{
				handleSummaryEdge(octElement, varName, pfName, callerFunctionName);
				CFANode callerNode =  summaryEdge.getPredecessor();
				assert ((FunctionDefinitionNode)callerNode.getLeavingEdge(0) != null);
				assert ((FunctionDefinitionNode)callerNode.getLeavingEdge(0) instanceof FunctionDefinitionNode);
				FunctionDefinitionNode fdefNode = (FunctionDefinitionNode)callerNode.getLeavingEdge(0).getSuccessor();
				List<String> parameters = fdefNode.getFunctionParameterNames();
				removeFunctionParameters(octElement, parameters, pfName);
			}
			break;
		}

		case CallToReturnEdge:
		{
			//octElement = octElement.clone ();
			break;
		}

		case MultiStatementEdge:
		{
			System.out.println("Multi Statement Edge");
			break;
		}

		case MultiDeclarationEdge:
		{
			System.out.println("Multi DeclarationEdge Edge");
			break;
		}
		}

		return octElement;

	}

	public List<AbstractElement> getAllAbstractSuccessors (AbstractElement element) throws CPAException
	{
		throw new CPAException ("Cannot get all abstract successors from non-location domain");
	}

	private void handleStatement (OctElement octElement, IASTExpression expression, CFAEdge cfaEdge) throws OctagonTransferException
	{
		// Binary operation
		if (expression instanceof IASTBinaryExpression) {
			handleBinaryExpression(octElement, expression, cfaEdge);
		}
		// Unary operation
		else if (expression instanceof IASTUnaryExpression)
		{
			handleUnaryExpression(octElement, expression, cfaEdge);
		}

		else{
			throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
		}
	}	

	private void handleDeclaration(OctElement octElement,
			IASTDeclarator[] declarators, CFAEdge cfaEdge) {

		for (IASTDeclarator declarator : declarators)
		{
			//IASTInitializer initializer = declarator.getInitializer ();
			//if (initializer != null)
			// {
			//  String varName = declarator.getName ().getRawSignature ();
			if(declarator != null){
				String varName = declarator.getName().toString();
				String functionName = cfaEdge.getPredecessor().getFunctionName();

				if(octElement.getNumberOfVars() == 0){
					System.out.println(varName + " is added to VARIABLES of " + functionName);
					OctElement temp = new OctElement(LibraryAccess.universe(1), new VariableMap(0));
					octElement.update(temp);
					octElement.addVar(varName, functionName);
				}
				else{
					if(cfaEdge.getSuccessor().getFunctionName().compareTo("main") !=0){
						if(!octElement.contains(varName, functionName)){
							System.out.println(varName + " is added to VARIABLES of " + functionName);
							octElement.update(LibraryAccess.addDimension(octElement, 1));
							octElement.addVar(varName, functionName);
						}
					}
					else{
						System.out.println(varName + " is added to VARIABLES of " + functionName);
						octElement.update(LibraryAccess.addDimension(octElement, 1));
						octElement.addVar(varName, functionName);
					}
				}
			}
			// }
		}
	}

	private void handleAssumption(OctElement octElement,
			IASTExpression expression, CFAEdge cfaEdge, boolean truthValue) throws OctagonTransferException
			{
		String functionName = cfaEdge.getPredecessor().getFunctionName();
		// Binary operation
		if (expression instanceof IASTBinaryExpression) {
			IASTBinaryExpression binExp = ((IASTBinaryExpression)expression);
			int opType = binExp.getOperator ();
			System.out.println(opType);

			IASTExpression op1 = binExp.getOperand1();
			IASTExpression op2 = binExp.getOperand2();

			// a (bOp) ?
			if(op1 instanceof IASTIdExpression){
				// a (bop) 9
				if(op2 instanceof IASTLiteralExpression){
					IASTIdExpression var = (IASTIdExpression)op1;
					String varName = var.getRawSignature();
					int variableId = octElement.getVarId(varName, functionName);

					double valueOfLiteral = Double.valueOf(op2.getRawSignature()).doubleValue();
					// a > 9
					if(opType == IASTBinaryExpression.op_greaterThan){
						// this is the if then edge
						if(truthValue){
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_greaterThan, variableId, -1, true, true, valueOfLiteral);
						}
						else{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_lessEqual, variableId, -1, true, true, valueOfLiteral);
						}
					}
					// a >= 9
					else if(opType == IASTBinaryExpression.op_greaterEqual){
						// this is the if then edge
						if(truthValue){
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_greaterEqual, variableId, -1, true, true, valueOfLiteral);
						}
						else{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_lessThan, variableId, -1, true, true, valueOfLiteral);
						}
					}
					// a < 9
					else if(opType == IASTBinaryExpression.op_lessThan){
						// this is the if then edge
						if(truthValue){
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_lessThan, variableId, -1, true, true, valueOfLiteral);
						}
						else{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_greaterEqual, variableId, -1, true, true, valueOfLiteral);
						}
					}
					// a <= 9
					else if(opType == IASTBinaryExpression.op_lessEqual){
						// this is the if then edge
						if(truthValue){
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_lessEqual, variableId, -1, true, true, valueOfLiteral);
						}
						else{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_greaterThan, variableId, -1, true, true, valueOfLiteral);
						}
					}
					// a == 9
					else if(opType == IASTBinaryExpression.op_equals){
						// this is the if then edge
						if(truthValue){
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_equals, variableId, -1, true, true, valueOfLiteral);
						}
						else{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_notequals, variableId, -1, true, true, valueOfLiteral);
						}
					}
					// a != 9
					else if(opType == IASTBinaryExpression.op_notequals){
						// this is the if then edge
						if(truthValue){
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_notequals, variableId, -1, true, true, valueOfLiteral);
						}
						else{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_equals, variableId, -1, true, true, valueOfLiteral);
						}
					}
				}

				// a (bop) b
				else if(op2 instanceof IASTIdExpression){
					IASTIdExpression leftVar = (IASTIdExpression)op1;
					String leftVarName = leftVar.getRawSignature();
					int leftVariableId = octElement.getVarId(leftVarName, functionName);

					IASTIdExpression rightVar = (IASTIdExpression)op2;
					String rightVarName = rightVar.getRawSignature();
					int rightVariableId = octElement.getVarId(rightVarName, functionName);

					// a > b
					if(opType == IASTBinaryExpression.op_greaterThan){
						// this is the if then edge
						if(truthValue){
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_greaterThan, leftVariableId, rightVariableId, true, true, 0);
						}
						else{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_lessEqual, leftVariableId, rightVariableId, true, true, 0);
						}
					}
					// a >= b
					else if(opType == IASTBinaryExpression.op_greaterEqual){
						// this is the if then edge
						if(truthValue){
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_greaterEqual, leftVariableId, rightVariableId, true, true, 0);
						}
						else{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_lessThan, leftVariableId, rightVariableId, true, true, 0);
						}
					}
					// a < b
					else if(opType == IASTBinaryExpression.op_lessThan){
						// this is the if then edge
						if(truthValue){
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_lessThan, leftVariableId, rightVariableId, true, true, 0);
						}
						else{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_greaterEqual, leftVariableId, rightVariableId, true, true, 0);
						}
					}
					// a <= b
					else if(opType == IASTBinaryExpression.op_lessEqual){
						// this is the if then edge
						if(truthValue){
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_lessEqual, leftVariableId, rightVariableId, true, true, 0);
						}
						else{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_greaterThan, leftVariableId, rightVariableId, true, true, 0);
						}
					}
					// a == b
					else if(opType == IASTBinaryExpression.op_equals){
						// this is the if then edge
						if(truthValue){
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_equals, leftVariableId, rightVariableId, true, true, 0);
						}
						else{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_notequals, leftVariableId, rightVariableId, true, true, 0);
						}
					}
					// a != b
					else if(opType == IASTBinaryExpression.op_notequals){
						// this is the if then edge
						if(truthValue){
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_notequals, leftVariableId, rightVariableId, true, true, 0);
						}
						else{
							propagateBooleanExpression(octElement, IASTBinaryExpression.op_equals, leftVariableId, rightVariableId, true, true, 0);
						}
					}
				}
			}
		}
		// Unary operation
		else if (expression instanceof IASTUnaryExpression)
		{
			IASTUnaryExpression unaryExp = ((IASTUnaryExpression)expression);
			// ! exp
			if(unaryExp.getOperator() == IASTUnaryExpression.op_not){
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

	// handles the expressions of type +-x +- y >= 9
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
		octElement.update(LibraryAccess.addConstraint(octElement, array));
	}

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
		else if(!isFirstVarPos)
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
			else if(!isSecondVarPos)
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
		OctElement finalElement = LibraryAccess.union(oct1, oct2);
		octElement.update(finalElement);
	}

	private void handleUnaryExpression(OctElement octElement,
			IASTExpression expression, CFAEdge cfaEdge) throws OctagonTransferException {
		String functionName = cfaEdge.getPredecessor().getFunctionName();
		IASTUnaryExpression unaryExpression = (IASTUnaryExpression) expression;

		int operator = unaryExpression.getOperator ();

		String lParam = unaryExpression.getOperand ().getRawSignature ();
		int variableId = octElement.getVarId(lParam, functionName);

		if (operator == IASTUnaryExpression.op_postFixIncr || 
				operator == IASTUnaryExpression.op_prefixIncr 
		)
		{
			addLiteralToVariable(octElement, cfaEdge, lParam, variableId, true, 1.0);
		}

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

	private void handleBinaryExpression(OctElement octElement, IASTExpression expression, CFAEdge cfaEdge) throws OctagonTransferException {

		System.out.println("IASTBinaryExpression: " + expression.getRawSignature());
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

	private void handleOperationAndAssign(OctElement octElement,
			IASTBinaryExpression binaryExpression, CFAEdge cfaEdge) throws OctagonTransferException {

		String functionName = cfaEdge.getPredecessor().getFunctionName();
		IASTExpression op1 = binaryExpression.getOperand1();
		IASTExpression op2 = binaryExpression.getOperand2();
		int typeOfOperator = binaryExpression.getOperator();
		
		// First operand is not a proper variable
		if (!(op1 instanceof IASTIdExpression))
		{
			System.out.println("First operand is not a proper variable");
			//TODO Erkan - Unhandled case
		}
		// If first operand is a variable
		else
		{
			IASTIdExpression lvar = ((IASTIdExpression)op1);
			String nameOfLVar = lvar.getRawSignature();
			int varLid = octElement.getVarId(nameOfLVar, functionName);

			// a op= 2
			if(op2 instanceof IASTLiteralExpression){
				double val = Double.valueOf(op2.getRawSignature()).doubleValue();

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
				}

				else{
					throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
				}
			}
			// a op= b
			else if(op2 instanceof IASTIdExpression){

				IASTIdExpression rvar = ((IASTIdExpression)op2);
				String nameOfRVar = rvar.getRawSignature();
				int varRid = octElement.getVarId(nameOfRVar, functionName);

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


	}

	private void handleAssignment(OctElement octElement, IASTBinaryExpression binaryExpression, CFAEdge cfaEdge) throws OctagonTransferException {

		String functionName = cfaEdge.getPredecessor().getFunctionName();
//		String lParam = binaryExpression.getOperand1 ().getRawSignature ();
//		String rParam = binaryExpression.getOperand2 ().getRawSignature ();

		IASTExpression op1 = binaryExpression.getOperand1();
		IASTExpression op2 = binaryExpression.getOperand2();

		// First operand is not a proper variable
		if (!(op1 instanceof IASTIdExpression))
		{
			System.out.println("First operand is not a proper variable");
			//TODO Erkan - Unhandled case
		}
		// If first operand is a variable
		else
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
		}
	}

	private void handleUnaryExpAssignment(OctElement octElement,
			IASTExpression op1, IASTUnaryExpression op2, CFAEdge cfaEdge) throws OctagonTransferException {
		String functionName = cfaEdge.getPredecessor().getFunctionName();
		String lParam = op1.getRawSignature ();
		//String rParam = op2.getRawSignature ();

		String nameOfVar = op2.getOperand().getRawSignature();
		int operatorType = op2.getOperator();
		if(operatorType == IASTUnaryExpression.op_minus){
			int lvar = octElement.getVarId(lParam, functionName);
			int var2 = octElement.getVarId(nameOfVar, functionName);
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

	private void handleCasting(OctElement octElement, IASTExpression op1,
			IASTExpression op2, CFAEdge cfaEdge) throws OctagonTransferException {
		String functionName = cfaEdge.getPredecessor().getFunctionName();
		String lParam = op1.getRawSignature ();
		//String rParam = op2.getRawSignature ();

		IASTExpression castOp = (((IASTCastExpression)op2).getOperand());
		//IASTTypeId typeOfOperator = (((IASTCastExpression)op2).getTypeId());

		// Only casting to double is valid
		if((((IASTCastExpression)op2).getTypeId().getRawSignature()).contains("double") ||
				(((IASTCastExpression)op2).getTypeId().getRawSignature()).contains("float")){
			/** double a = (double) 7 */
			if(castOp instanceof IASTLiteralExpression){
				int typeOfCastLiteral = ((IASTLiteralExpression)castOp).getKind();
				if( typeOfCastLiteral ==  IASTLiteralExpression.lk_integer_constant || 
						typeOfCastLiteral == IASTLiteralExpression.lk_float_constant)
				{
					int lvar = octElement.getVarId(lParam, functionName);
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
			/** For handling expressions of form a = (double) b */
			else if(castOp instanceof IASTIdExpression){
				IASTIdExpression varId = ((IASTIdExpression)castOp);
				String nameOfVar = varId.getRawSignature();
				int lvar = octElement.getVarId(lParam, functionName);
				int var2 = octElement.getVarId(nameOfVar, functionName);
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
		// If we don't cast to double
		else{
			int var = octElement.getVarId(lParam, functionName);
			octElement.update(LibraryAccess.forget(octElement, var));
		}


	}

	private void handleLiteralAssignment(OctElement octElement, IASTExpression op1, IASTExpression op2, String functionName) throws OctagonTransferException {

		String lParam = op1.getRawSignature ();
		String rParam = op2.getRawSignature ();

		int typeOfLiteral = ((IASTLiteralExpression)op2).getKind();
		if( typeOfLiteral ==  IASTLiteralExpression.lk_integer_constant || 
				typeOfLiteral == IASTLiteralExpression.lk_float_constant)
		{
			int lvar = octElement.getVarId(lParam, functionName);
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

	private void handleVariableAssignment(OctElement octElement,
			IASTExpression op1, IASTExpression op2, String functionName) {

		String lParam = op1.getRawSignature ();
		//String rParam = op2.getRawSignature ();

		IASTIdExpression varId = ((IASTIdExpression)op2);
		String nameOfVar = varId.getRawSignature();
		int lvar = octElement.getVarId(lParam, functionName);
		int var2 = octElement.getVarId(nameOfVar, functionName);
		Num[] array = new Num[octElement.getNumberOfVars()+1];

		for(int i=0; i<array.length-1; i++){
			array[i] = new Num(0);
		}

		array[array.length-1] = new Num(0);
		array[var2] = new Num(1);
		octElement.update(LibraryAccess.assignVar(octElement, lvar, array));
	}

	private void handleAssignmentOfBinaryExp(OctElement octElement,
			IASTExpression op1, IASTExpression op2, CFAEdge cfaEdge) throws OctagonTransferException {
		String functionName = cfaEdge.getPredecessor().getFunctionName();
		String lParam = op1.getRawSignature ();
		//String rParam = op2.getRawSignature ();

		//Binary Expression
		IASTBinaryExpression binExp = (IASTBinaryExpression) op2;
		//Right Operand of the binary expression
		IASTExpression lVarInBinaryExp = binExp.getOperand1();
		//Left Operand of the binary expression
		IASTExpression rVarInBinaryExp = binExp.getOperand2();

		switch (binExp.getOperator ())
		{
		/** Second operand is an addition*/
		case IASTBinaryExpression.op_plus:
		{
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
						int varRid = octElement.getVarId(nameOfRVar, functionName);

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
			/** a = b + ? */
			else if(lVarInBinaryExp instanceof IASTIdExpression){
				IASTIdExpression lvar = ((IASTIdExpression)lVarInBinaryExp);
				String nameOfLVar = lvar.getRawSignature();
				int varLid = octElement.getVarId(nameOfLVar, functionName);

				/** a = b + 2 */
				if(rVarInBinaryExp instanceof IASTLiteralExpression){
					double val = Double.valueOf(rVarInBinaryExp.getRawSignature()).doubleValue();

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
				/** a = b + c*/
				else if(rVarInBinaryExp instanceof IASTIdExpression){
					IASTIdExpression rvar = ((IASTIdExpression)rVarInBinaryExp);
					String nameOfRVar = rvar.getRawSignature();
					int varRid = octElement.getVarId(nameOfRVar, functionName);
					addTwoVariables(octElement, cfaEdge, lParam, varLid, varRid, true, true);
				}
				else{
					throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
				}
			}

			/** a = 9 + ? */
			else if(lVarInBinaryExp instanceof IASTLiteralExpression){
				double val = Double.valueOf(lVarInBinaryExp.getRawSignature()).doubleValue();
				//Num n = new Num(val);
				int typeOfCastLiteral = ((IASTLiteralExpression)lVarInBinaryExp).getKind();
				if( typeOfCastLiteral ==  IASTLiteralExpression.lk_integer_constant || 
						typeOfCastLiteral == IASTLiteralExpression.lk_float_constant)
				{
					/** a = 8 + b */
					if(rVarInBinaryExp instanceof IASTIdExpression){
						IASTIdExpression rvar = ((IASTIdExpression)rVarInBinaryExp);
						String nameOfRVar = rvar.getRawSignature();
						int varRid = octElement.getVarId(nameOfRVar, functionName);
						addLiteralToVariable(octElement, cfaEdge, lParam, varRid, true, val);

					}
					/** a = 8 + 9 */
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

		case IASTBinaryExpression.op_minus:
		{
			System.out.println("SUBTRACTION");

			if(lVarInBinaryExp instanceof IASTUnaryExpression){
				IASTUnaryExpression unaryExpression = (IASTUnaryExpression) lVarInBinaryExp;
				int operator = unaryExpression.getOperator ();

				// a = -8 - b
				if(operator == IASTUnaryExpression.op_minus){
					IASTExpression unaryOperand = unaryExpression.getOperand();
					if(unaryOperand instanceof IASTLiteralExpression){
						double value = Double.valueOf(unaryOperand.getRawSignature()).doubleValue();
						double negVal = 0 - value;

						IASTIdExpression rvar = ((IASTIdExpression)rVarInBinaryExp);
						String nameOfRVar = rvar.getRawSignature();
						int varRid = octElement.getVarId(nameOfRVar, functionName);

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
			/** a = b - ? */
			else if(lVarInBinaryExp instanceof IASTIdExpression){
				IASTIdExpression lvar = ((IASTIdExpression)lVarInBinaryExp);
				String nameOfLVar = lvar.getRawSignature();
				int varLid = octElement.getVarId(nameOfLVar, functionName);

				/** a = b - 2 */
				if(rVarInBinaryExp instanceof IASTLiteralExpression){
					double val = Double.valueOf(rVarInBinaryExp.getRawSignature()).doubleValue();
					double negVal = 0 - val;
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
				/** a = b - c*/
				else if(rVarInBinaryExp instanceof IASTIdExpression){
					IASTIdExpression rvar = ((IASTIdExpression)rVarInBinaryExp);
					String nameOfRVar = rvar.getRawSignature();
					int varRid = octElement.getVarId(nameOfRVar, functionName);
					addTwoVariables(octElement, cfaEdge, lParam, varLid, varRid, true, false);
				}
				else{
					throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
				}
			}

			/** a = 8 - ? */
			else if(lVarInBinaryExp instanceof IASTLiteralExpression){
				double val = Double.valueOf(lVarInBinaryExp.getRawSignature()).doubleValue();
				//Num n = new Num(val);
				int typeOfCastLiteral = ((IASTLiteralExpression)lVarInBinaryExp).getKind();
				if( typeOfCastLiteral ==  IASTLiteralExpression.lk_integer_constant || 
						typeOfCastLiteral == IASTLiteralExpression.lk_float_constant)
				{
					/** a = 8 - b */
					if(rVarInBinaryExp instanceof IASTIdExpression){
						IASTIdExpression rvar = ((IASTIdExpression)rVarInBinaryExp);
						String nameOfRVar = rvar.getRawSignature();
						int varRid = octElement.getVarId(nameOfRVar, functionName);
						addLiteralToVariable(octElement, cfaEdge, lParam, varRid, false, val);

					}
					/** a = 8 - 7 */
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

		case IASTBinaryExpression.op_multiply:
		{
			System.out.println("MULTIPLICATION");

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
						int varRid = octElement.getVarId(nameOfRVar, functionName);

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
			/** a = b * ? */
			else if(lVarInBinaryExp instanceof IASTIdExpression){
				IASTIdExpression lvar = ((IASTIdExpression)lVarInBinaryExp);
				String nameOfLVar = lvar.getRawSignature();
				int varLid = octElement.getVarId(nameOfLVar, functionName);

				/** a = b * 2 */
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

				/** a = b * c */
				else if(rVarInBinaryExp instanceof IASTIdExpression){
					int var = octElement.getVarId(lParam, functionName);
					octElement.update(LibraryAccess.forget(octElement, var));
				}
				else{
					throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
				}
			}

			/** a = 8 * ? */
			else if(lVarInBinaryExp instanceof IASTLiteralExpression){
				double val = Double.valueOf(lVarInBinaryExp.getRawSignature()).doubleValue();
				//Num n = new Num(val);
				int typeOfCastLiteral = ((IASTLiteralExpression)lVarInBinaryExp).getKind();
				if( typeOfCastLiteral ==  IASTLiteralExpression.lk_integer_constant || 
						typeOfCastLiteral == IASTLiteralExpression.lk_float_constant)
				{
					/** a = 8 * b */
					if(rVarInBinaryExp instanceof IASTIdExpression){
						IASTIdExpression rvar = ((IASTIdExpression)rVarInBinaryExp);
						String nameOfRVar = rvar.getRawSignature();
						int varRid = octElement.getVarId(nameOfRVar, functionName);
						multiplyLiteralWithVariable(octElement, cfaEdge, lParam, varRid, true, val);

					}
					/** a = 8 * 9 */
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

		case IASTBinaryExpression.op_divide:
		{
			System.out.println("DIVISION");
			int var = octElement.getVarId(lParam, functionName);
			octElement.update(LibraryAccess.forget(octElement, var));
			break;
		}

		case IASTBinaryExpression.op_modulo:
		{
			System.out.println("MODULO");
			int var = octElement.getVarId(lParam, functionName);
			octElement.update(LibraryAccess.forget(octElement, var));
			break;
		}

		default: throw new OctagonTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
		}

	}

	private void addLiteralToVariable(OctElement octElement,
			CFAEdge cfaEdge, String leftOperator, int variableId, 
			boolean isVariablePositive, double valueOfLiteral) {

		String functionName = cfaEdge.getPredecessor().getFunctionName();
		Num n = new Num(valueOfLiteral);

		int lvar = octElement.getVarId(leftOperator, functionName);
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

	private void addTwoVariables(OctElement octElement,
			CFAEdge cfaEdge, String leftOperator, int LvariableId, int RvariableId,
			boolean isLVariablePositive, boolean isRVariablePositive) {

		String functionName = cfaEdge.getPredecessor().getFunctionName();
		int lvar = octElement.getVarId(leftOperator, functionName);
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

	private void multiplyLiteralWithVariable(OctElement octElement,
			CFAEdge cfaEdge, String leftParam, int variableId, boolean isVariablePositive, double valueOfLiteral) {
		
		String functionName = cfaEdge.getPredecessor().getFunctionName();
		int lvar = octElement.getVarId(leftParam, functionName);
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

	private void handleFunctionCall(OctElement prevOctElement, FunctionCallEdge callEdge) {
		OctElement octElement = prevOctElement;
		FunctionDefinitionNode functionEntryNode = (FunctionDefinitionNode)callEdge.getSuccessor();
		String calledFunctionName = functionEntryNode.getFunctionName();
		String callerFunctionName = callEdge.getPredecessor().getFunctionName();
		
		List<String> paramNames = functionEntryNode.getFunctionParameterNames();
		// TODO here we assume that all parameters are type of int or double
		for(int i=0; i<paramNames.size(); i++){
			octElement.update(LibraryAccess.addDimension(octElement, 1));
			String paramName = paramNames.get(i);
			octElement.addVar(paramName, calledFunctionName);
		}

		octElement.update(LibraryAccess.addDimension(octElement, 1));
		octElement.addVar("___cpa_temp_result_var_" + functionEntryNode.getFunctionName() + "()", calledFunctionName);

		IASTExpression[] arguments = callEdge.getArguments();
		assert (paramNames.size() == arguments.length);

		for(int i=0; i<arguments.length; i++){

			IASTExpression argument = arguments[i];
			String paramName = paramNames.get(i);

			if(argument instanceof IASTLiteralExpression){
				String argumentValue = argument.getRawSignature ();

				int typeOfLiteral = ((IASTLiteralExpression)argument).getKind();
				if( typeOfLiteral ==  IASTLiteralExpression.lk_integer_constant || 
						typeOfLiteral == IASTLiteralExpression.lk_float_constant)
				{
					int lvar = octElement.getVarId(paramName, calledFunctionName);
					double val = Double.valueOf(argumentValue).doubleValue();
					Num n = new Num(val);
					Num[] array = new Num[octElement.getNumberOfVars()+1];

					for(int j=0; j<array.length-1; j++){
						array[j] = new Num(0);
					}
					array[array.length-1] = n;
					octElement.update(LibraryAccess.assignVar(octElement, lvar, array));
				}
			}

			else if(argument instanceof IASTIdExpression){

				IASTIdExpression varId = ((IASTIdExpression)argument);
				String nameOfVar = varId.getRawSignature();

				int lvar = octElement.getVarId(paramName, calledFunctionName);
				int var2 = octElement.getVarId(nameOfVar, callerFunctionName);
				Num[] array = new Num[octElement.getNumberOfVars()+1];

				for(int j=0; j<array.length-1; j++){
					array[j] = new Num(0);
				}

				array[array.length-1] = new Num(0);
				array[var2] = new Num(1);
				octElement.update(LibraryAccess.assignVar(octElement, lvar, array));
			}
		}
	}

	private void handleRecursiveFunctionCall(OctElement octElement, FunctionCallEdge callEdge){

		FunctionDefinitionNode functionEntryNode = (FunctionDefinitionNode)callEdge.getSuccessor();
		List<String> paramNames = functionEntryNode.getFunctionParameterNames();
		// TODO here we assume that all parameters are type of int or double

		IASTExpression[] arguments = callEdge.getArguments();
		assert (paramNames.size() == arguments.length);

		for(int i=0; i<arguments.length; i++){

			IASTExpression argument = arguments[i];
			String paramName = paramNames.get(i);

			if(argument instanceof IASTLiteralExpression){
				String argumentValue = argument.getRawSignature ();

				int typeOfLiteral = ((IASTLiteralExpression)argument).getKind();
				if( typeOfLiteral ==  IASTLiteralExpression.lk_integer_constant || 
						typeOfLiteral == IASTLiteralExpression.lk_float_constant)
				{
					int lvar = octElement.getVarId(paramName);
					double val = Double.valueOf(argumentValue).doubleValue();
					Num n = new Num(val);
					Num[] array = new Num[octElement.getNumberOfVars()+1];

					for(int j=0; j<array.length-1; j++){
						array[j] = new Num(0);
					}
					array[array.length-1] = n;
					octElement.update(LibraryAccess.assignVar(octElement, lvar, array));
				}
			}

			else if(argument instanceof IASTIdExpression){

				IASTIdExpression varId = ((IASTIdExpression)argument);
				String nameOfVar = varId.getRawSignature();
				int lvar = octElement.getVarId(paramName);
				int var2 = octElement.getVarId(nameOfVar);
				Num[] array = new Num[octElement.getNumberOfVars()+1];

				for(int j=0; j<array.length-1; j++){
					array[j] = new Num(0);
				}

				array[array.length-1] = new Num(0);
				array[var2] = new Num(1);
				octElement.update(LibraryAccess.assignVar(octElement, lvar, array));
			}
		}
	}

	private void handleExitFromFunction(OctElement octElement,
			IASTExpression expression, StatementEdge statementEdge) throws OctagonTransferException {

		String returnedFunction = statementEdge.getPredecessor().getFunctionName();
		
		if(expression instanceof IASTUnaryExpression){
			IASTUnaryExpression unaryExp = (IASTUnaryExpression)expression;
			if(unaryExp.getOperator() == IASTUnaryExpression.op_bracketedPrimary){
				IASTExpression exprInParanhesis = unaryExp.getOperand();
				if(exprInParanhesis instanceof IASTLiteralExpression){
					IASTLiteralExpression litExpr = (IASTLiteralExpression)exprInParanhesis;
					String fName = ((CFAExitNode)statementEdge.getSuccessor()).getFunctionName();
					int resultvarID = octElement.getVarId("___cpa_temp_result_var_" + fName + "()", returnedFunction);
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
					IASTIdExpression idExpr = (IASTIdExpression)exprInParanhesis;
					String fName = ((CFAExitNode)statementEdge.getSuccessor()).getFunctionName();
					int lvar = octElement.getVarId("___cpa_temp_result_var_" + fName + "()", returnedFunction);
					String idExpName = idExpr.getRawSignature ();

					int rvar = octElement.getVarId(idExpName, returnedFunction);
					Num[] array = new Num[octElement.getNumberOfVars()+1];

					for(int i=0; i<array.length-1; i++){
						array[i] = new Num(0);
					}

					array[array.length-1] = new Num(0);
					array[rvar] = new Num(1);
					octElement.update(LibraryAccess.assignVar(octElement, lvar, array));
				}
			}
		}
		else if(expression instanceof IASTBinaryExpression){
			// TODO is there such a case?
		}
		else {
			throw new OctagonTransferException("Unhandled case");
		}
	}

	private void handleSummaryEdge(OctElement octElement, String varName,
			String functionName, String callerFunctionName) {
		
		int lvar = octElement.getVarId(varName, callerFunctionName);

		System.out.println("___cpa_temp_result_var_" + functionName + "()");
		int rvar = octElement.getVarId("___cpa_temp_result_var_" + functionName + "()", functionName);
		Num[] array = new Num[octElement.getNumberOfVars()+1];

		for(int i=0; i<array.length-1; i++){
			array[i] = new Num(0);
		}

		array[array.length-1] = new Num(0);
		array[rvar] = new Num(1);
		octElement.update(LibraryAccess.assignVar(octElement, lvar, array));
	}

	private void removeFunctionParameters(OctElement octElement,
			List<String> parameters, String fname) {
//		int numOfValuesToRemove = parameters.size()+1;
//		// TODO removing from the end of octagon?
//		octElement.update(LibraryAccess.removeDimension(octElement, numOfValuesToRemove));
//		for(int i=0; i<parameters.size(); i++){
//			octElement.removeVar((String)parameters.get(i));
//		}
//
//		octElement.removeVar("___cpa_temp_result_var_" + fname + "()");
//		TempVariableCount tempVars = octElement.getTempVarList().get(fname);
//		Iterator<String> it = tempVars.getIterator();
//		while(it.hasNext()){
//			String s = it.next();
//			octElement.removeVar(s);
//			octElement.update(LibraryAccess.removeDimension(octElement, 1));
//		}
//		octElement.getTempVarList().remove(fname);
		
		int numOfValuesToRemove = octElement.removeVariablesOfFunction(fname);
		octElement.update(LibraryAccess.removeDimension(octElement, numOfValuesToRemove));
		
	}
}
