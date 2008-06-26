package cpaplugin.cpa.cpas.predicateabstraction;

import java.io.IOException;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;

import predicateabstraction.Operator;
import predicateabstraction.SimplifiedInstruction;
import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.c.AssumeEdge;
import cpaplugin.cfa.objectmodel.c.StatementEdge;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.exceptions.CPAException;
import cpaplugin.exceptions.PredicateAbstractionTransferException;

public class PredicateAbstractionTransferRelation implements TransferRelation 
{
	private PredicateAbstractionDomain predAbsDomain;

	public PredicateAbstractionTransferRelation (PredicateAbstractionDomain predAbsDomain)
	{
		this.predAbsDomain = predAbsDomain;
	}

	public AbstractDomain getAbstractDomain ()
	{
		return predAbsDomain;
	}

	public AbstractElement getAbstractSuccessor (AbstractElement element, CFAEdge cfaEdge)
	{
		System.out.println(" =============== " + cfaEdge.getRawStatement());
		PredicateAbstractionElement predAbsElement = (PredicateAbstractionElement) element;
		switch (cfaEdge.getEdgeType ())
		{
		case StatementEdge:
		{
			predAbsElement = predAbsElement.clone ();

			StatementEdge statementEdge = (StatementEdge) cfaEdge;
			IASTExpression expression = statementEdge.getExpression ();

			// TODO function call
//			// handling function return
//			if(statementEdge.isJumpEdge()){
//			try {
//			handleExitFromFunction(octElement, expression, statementEdge);
//			} catch (OctagonTransferException e) {
//			e.printStackTrace();
//			}
//			}

//			else{
			try {
				handleStatement (predAbsElement, expression, cfaEdge);
			} catch (PredicateAbstractionTransferException e) {
				e.printStackTrace();
			}
//			}
			break;
		}

		case DeclarationEdge:
		{
			// we don't do anything for declarations
			predAbsElement = predAbsElement.clone ();

//			DeclarationEdge declarationEdge = (DeclarationEdge) cfaEdge;
//			IASTDeclarator [] declarators = declarationEdge.getDeclarators ();
//			handleDeclaration (octElement, declarators, cfaEdge);
			break;
		}

		case AssumeEdge:
		{
			predAbsElement = predAbsElement.clone ();

			AssumeEdge assumeEdge = (AssumeEdge) cfaEdge;
			IASTExpression expression = assumeEdge.getExpression();
			try {
				handleAssumption (predAbsElement, expression, cfaEdge, assumeEdge.getTruthAssumption());
			} catch (PredicateAbstractionTransferException e) {
				e.printStackTrace();
			}
			break;

		}

		case BlankEdge:
		{
			predAbsElement = predAbsElement.clone ();
			System.out.println("Blank Edge -- Do Nothing");
			break;
		}

		// TODO function call
//		case FunctionCallEdge: 
//		{
//		octElement = octElement.clone ();
//		FunctionCallEdge functionCallEdge = (FunctionCallEdge) cfaEdge;

//		if(functionCallEdge.isRecursive()){
//		handleRecursiveFunctionCall(octElement, functionCallEdge);
//		}
//		else{
//		handleFunctionCall(octElement, functionCallEdge);
//		}
//		break;
//		}

		// TODO function call
//		case ReturnEdge:
//		{
//		octElement = octElement.clone ();
//		ReturnEdge exitEdge = (ReturnEdge) cfaEdge;
//		CFANode predecessorNode = exitEdge.getPredecessor();
//		CFANode successorNode = exitEdge.getSuccessor();
//		String pfName = predecessorNode.getFunctionName();
//		String callerFunctionName = successorNode.getFunctionName();

//		CallToReturnEdge summaryEdge = (CallToReturnEdge)successorNode.getEnteringSummaryEdge();
//		IASTBinaryExpression expression = (IASTBinaryExpression)summaryEdge.getExpression();
//		String varName = ((IASTIdExpression)expression.getOperand1()).getRawSignature();

//		if(exitEdge.isExitingRecursiveCall()){
//		// TODO recursive return
//		handleSummaryEdge(octElement, varName, pfName, callerFunctionName);
//		}
//		else if(predecessorNode.getFunctionName().compareTo("main") == 0){
//		//Do nothing
//		}
//		else{
//		handleSummaryEdge(octElement, varName, pfName, callerFunctionName);
//		CFANode callerNode =  summaryEdge.getPredecessor();
//		assert ((FunctionDefinitionNode)callerNode.getLeavingEdge(0) != null);
//		assert ((FunctionDefinitionNode)callerNode.getLeavingEdge(0) instanceof FunctionDefinitionNode);
//		FunctionDefinitionNode fdefNode = (FunctionDefinitionNode)callerNode.getLeavingEdge(0).getSuccessor();
//		List<String> parameters = fdefNode.getFunctionParameterNames();
//		removeFunctionParameters(octElement, parameters, pfName);
//		}
//		break;
//		}

		// TODO function call
//		case CallToReturnEdge:
//		{
//		//octElement = octElement.clone ();
//		break;
//		}

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

		return predAbsElement;
	}

	public List<AbstractElement> getAllAbstractSuccessors (AbstractElement element) throws CPAException
	{
		throw new CPAException ("Cannot get all abstract successors from non-location domain");
	}

	private void handleStatement (PredicateAbstractionElement predAbsElement, IASTExpression expression, CFAEdge cfaEdge) throws PredicateAbstractionTransferException
	{
		// Binary operation
		if (expression instanceof IASTBinaryExpression) {
			handleBinaryExpression(predAbsElement, expression, cfaEdge);
		}
		// Unary operation
		else if (expression instanceof IASTUnaryExpression)
		{
			handleUnaryExpression(predAbsElement, expression, cfaEdge);
		}

		else{
			throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
		}
	}

	// We don't have to handle decl's
//	private void handleDeclaration(OctElement octElement,
//	IASTDeclarator[] declarators, CFAEdge cfaEdge) {

//	}

	private void handleAssumption(PredicateAbstractionElement predAbsElement,
			IASTExpression expression, CFAEdge cfaEdge, boolean truthValue) throws PredicateAbstractionTransferException
			{

		IASTBinaryExpression binExp = ((IASTBinaryExpression)expression);
		int opType = binExp.getOperator ();

		IASTExpression op1 = binExp.getOperand1();
		IASTExpression op2 = binExp.getOperand2();

		String leftOperator = op1.getRawSignature();
		String rightOperator = op2.getRawSignature();

		if (expression instanceof IASTBinaryExpression) {
			if(opType == IASTBinaryExpression.op_greaterThan){
				// this is the if then edge
				if(truthValue){
					propagateBooleanExpression(predAbsElement, IASTBinaryExpression.op_greaterThan, leftOperator, rightOperator);
				}
				else{
					propagateBooleanExpression(predAbsElement, IASTBinaryExpression.op_lessEqual, leftOperator, rightOperator);
				}
			}
			// a >= 9
			else if(opType == IASTBinaryExpression.op_greaterEqual){
				// this is the if then edge
				if(truthValue){
					propagateBooleanExpression(predAbsElement, IASTBinaryExpression.op_greaterEqual, leftOperator, rightOperator);
				}
				else{
					propagateBooleanExpression(predAbsElement, IASTBinaryExpression.op_lessThan, leftOperator, rightOperator);
				}
			}
			// a < 9
			else if(opType == IASTBinaryExpression.op_lessThan){
				// this is the if then edge
				if(truthValue){
					propagateBooleanExpression(predAbsElement, IASTBinaryExpression.op_lessThan, leftOperator, rightOperator);
				}
				else{
					propagateBooleanExpression(predAbsElement, IASTBinaryExpression.op_greaterEqual, leftOperator, rightOperator);
				}
			}
			// a <= 9
			else if(opType == IASTBinaryExpression.op_lessEqual){
				// this is the if then edge
				if(truthValue){
					propagateBooleanExpression(predAbsElement, IASTBinaryExpression.op_lessEqual, leftOperator, rightOperator);
				}
				else{
					propagateBooleanExpression(predAbsElement, IASTBinaryExpression.op_greaterThan, leftOperator, rightOperator);
				}
			}
			// a == 9
			else if(opType == IASTBinaryExpression.op_equals){
				// this is the if then edge
				if(truthValue){
					propagateBooleanExpression(predAbsElement, IASTBinaryExpression.op_equals, leftOperator, rightOperator);
				}
				else{
					propagateBooleanExpression(predAbsElement, IASTBinaryExpression.op_notequals, leftOperator, rightOperator);
				}
			}
			// a != 9
			else if(opType == IASTBinaryExpression.op_notequals){
				// this is the if then edge
				if(truthValue){
					propagateBooleanExpression(predAbsElement, IASTBinaryExpression.op_notequals, leftOperator, rightOperator);
				}
				else{
					propagateBooleanExpression(predAbsElement, IASTBinaryExpression.op_equals, leftOperator, rightOperator);
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
							handleAssumption(predAbsElement, binExp2, cfaEdge, !truthValue);
						}
						else {
							throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
						}
					}
					else {
						throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
					}
				}

				else {
					throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
				}
			}

			else {
				throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
			}
		}

		else{
			throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
		}
			}

	private void propagateBooleanExpression(PredicateAbstractionElement predAbsElement ,int opType, String leftOperator, String rightOperator) throws PredicateAbstractionTransferException {

		String query = "";
		// Binary operation

		if(opType == IASTBinaryExpression.op_lessEqual){
			query = "<= " + leftOperator + " " + rightOperator;
		}

		else if(opType == IASTBinaryExpression.op_lessThan){
			query = "& [ <= " + leftOperator + " " + rightOperator + " ~ = " + leftOperator + " " + rightOperator + " ]";
		}

		else if(opType == IASTBinaryExpression.op_greaterEqual){
			query = "<= " + rightOperator + " " + leftOperator;
		}

		else if(opType == IASTBinaryExpression.op_greaterThan){
			query = "& [ <= " + rightOperator + " " + leftOperator + " ~ = " + leftOperator + " " + rightOperator + " ]";
		}

		else if(opType == IASTBinaryExpression.op_equals){
			query = " = " + leftOperator + " " + rightOperator;
		}

		else if(opType == IASTBinaryExpression.op_notequals){
			query = " ~= " + leftOperator + " " + rightOperator;
		}
		
		else{
			//TODO exception
			System.out.println("exiting here");
			System.exit(0);
		}

		handleBooleanExpression(predAbsElement, query);
	}

	private void handleBooleanExpression(
			PredicateAbstractionElement predAbsElement, String query) {
		String prevState = predAbsElement.getRegion();

		try {
			predAbsElement.updateAssumption(prevState, query);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handleUnaryExpression(PredicateAbstractionElement predAbsElement,
			IASTExpression expression, CFAEdge cfaEdge) throws PredicateAbstractionTransferException {
		String functionName = cfaEdge.getPredecessor().getFunctionName();
		IASTUnaryExpression unaryExpression = (IASTUnaryExpression) expression;

		int operator = unaryExpression.getOperator ();
		IASTExpression operand = unaryExpression.getOperand();
		String operandName = operand.getRawSignature();
		SimplifiedInstruction simpIns = new SimplifiedInstruction();

		// a++, ++a
		if (operator == IASTUnaryExpression.op_postFixIncr || 
				operator == IASTUnaryExpression.op_prefixIncr 
		)
		{
			simpIns = new SimplifiedInstruction(operandName, "+ [ " + operandName + " 1 " + "]", Operator.equals);
		}

		// a--, --a
		else if(operator == IASTUnaryExpression.op_prefixDecr ||
				operator == IASTUnaryExpression.op_postFixDecr)
		{
			simpIns = new SimplifiedInstruction(operandName, "+ [ " + operandName + " -1 " + "]", Operator.equals);
		}

		else
		{
			throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
		}

		handleAssignmetQuery(predAbsElement, simpIns);
	}

	private void handleBinaryExpression(PredicateAbstractionElement predAbsElement, IASTExpression expression, CFAEdge cfaEdge) throws PredicateAbstractionTransferException {

		IASTBinaryExpression binaryExpression = (IASTBinaryExpression) expression;
		switch (binaryExpression.getOperator ())
		{
		// a = ?
		case IASTBinaryExpression.op_assign:
		{
			handleAssignment(predAbsElement, binaryExpression, cfaEdge);
			break;
		}
		// a += 2
		case IASTBinaryExpression.op_plusAssign:
		case IASTBinaryExpression.op_minusAssign:
		case IASTBinaryExpression.op_multiplyAssign:
		{
			handleOperationAndAssign(predAbsElement, binaryExpression, cfaEdge);
			break;
		}

		default: throw new PredicateAbstractionTransferException("Unhandled case ");

		}
	}

	private void handleOperationAndAssign(PredicateAbstractionElement predAbsElement,
			IASTBinaryExpression binaryExpression, CFAEdge cfaEdge) throws PredicateAbstractionTransferException {

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
			SimplifiedInstruction simpIns = new SimplifiedInstruction();
			// a op= 2
			if(op2 instanceof IASTLiteralExpression){

				int typeOfLiteral = ((IASTLiteralExpression)op2).getKind();
				if( typeOfLiteral ==  IASTLiteralExpression.lk_integer_constant || 
						typeOfLiteral == IASTLiteralExpression.lk_float_constant)
				{
					// a += 2
					if(typeOfOperator == IASTBinaryExpression.op_plusAssign){
						String leftVar = op1.getRawSignature();
						String rightVar = op2.getRawSignature();

						simpIns = new SimplifiedInstruction(leftVar, " + [ " + leftVar + " " + rightVar + " ]", Operator.equals);
					}
					// a -= 2
					else if(typeOfOperator == IASTBinaryExpression.op_minusAssign){
						String leftVar = op1.getRawSignature();
						String rightVar = op2.getRawSignature();

						simpIns = new SimplifiedInstruction(leftVar, " + [ " + leftVar + " -" + rightVar + " ]", Operator.equals);
					}
					// a *= 2
					else if(typeOfOperator == IASTBinaryExpression.op_multiplyAssign){
						String leftVar = op1.getRawSignature();
						String rightVar = op2.getRawSignature();

						simpIns = new SimplifiedInstruction(leftVar, " * " + rightVar + " " + leftVar, Operator.equals);
					}
				}
				else{
					throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
				}
			}
			// a op= b
			else if(op2 instanceof IASTIdExpression){

				// a += b
				if(typeOfOperator == IASTBinaryExpression.op_plusAssign){
					String leftVar = op1.getRawSignature();
					String rightVar = op2.getRawSignature();

					simpIns = new SimplifiedInstruction(leftVar, " + [ " + leftVar + " " + rightVar + " ]", Operator.equals);
				}
				// a -= b
				else if(typeOfOperator == IASTBinaryExpression.op_minusAssign){
					String leftVar = op1.getRawSignature();
					String rightVar = op2.getRawSignature();

					simpIns = new SimplifiedInstruction(leftVar, " + [ " + leftVar + " -" + rightVar + " ]", Operator.equals);
				}
				// a *= b
				else if(typeOfOperator == IASTBinaryExpression.op_multiplyAssign){
					String leftVar = op1.getRawSignature();
					simpIns = new SimplifiedInstruction(leftVar, "__________cpa_________unknownVal___", Operator.equals);
				}

				else{
					throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
				}
			}
			else{
				throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
			}
			handleAssignmetQuery(predAbsElement, simpIns);
		}
	}

	private void handleAssignment(PredicateAbstractionElement predAbsElement, IASTBinaryExpression binaryExpression, CFAEdge cfaEdge) throws PredicateAbstractionTransferException {

		String functionName = cfaEdge.getPredecessor().getFunctionName();
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
			// cases for a = 8.2, a = b, a = b op c, a = -b
			if(		op2 instanceof IASTLiteralExpression || 
					op2 instanceof IASTIdExpression ||
					op2 instanceof IASTUnaryExpression)
			{
				String leftVariable = op1.getRawSignature();
				String rightVariable = op2.getRawSignature();
				int binaryOp = binaryExpression.getOperator();

				assert(binaryOp != IASTBinaryExpression.op_assign);

				SimplifiedInstruction simpIns =  new SimplifiedInstruction(leftVariable, rightVariable, Operator.equals);
				handleAssignmetQuery(predAbsElement, simpIns);
			}

			// a = (cast) ?
			else if(op2 instanceof IASTCastExpression) {
				handleCasting(predAbsElement, op1, op2, cfaEdge);
			}
			else if(op2 instanceof IASTBinaryExpression){
				String leftVariable = op1.getRawSignature();
				handleAssignmentOfBinaryOp(predAbsElement, leftVariable, binaryExpression, cfaEdge);
			}
			else{
				throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
			}
		}
	}

	private void handleAssignmentOfBinaryOp(PredicateAbstractionElement predAbsElement, String leftVariable, IASTBinaryExpression binaryExp, CFAEdge cfaEdge) throws PredicateAbstractionTransferException {
		IASTBinaryExpression binaryExpression = (IASTBinaryExpression)binaryExp.getOperand2();
		//Left Operand of the binary expression
		IASTExpression lVarInBinaryExp = binaryExpression.getOperand1();
		String leftVarNameInBinExp = lVarInBinaryExp.getRawSignature();
		//Right Operand of the binary expression
		IASTExpression rVarInBinaryExp = binaryExpression.getOperand2();
		String rightVarNameInBinExp = rVarInBinaryExp.getRawSignature();

		SimplifiedInstruction simpIns = new SimplifiedInstruction();
		String rightVariable = "";
		
		switch (binaryExpression.getOperator ())
		{
		
		/** Second operand is an addition*/
		case IASTBinaryExpression.op_plus:
		{
			// Addition
			// a = a + b
			// TODO handle this case
//			if(		!(lVarInBinaryExp instanceof IASTUnaryExpression) ||
//					!(lVarInBinaryExp instanceof IASTIdExpression) ||
//					!(lVarInBinaryExp instanceof IASTLiteralExpression) ||
//					!(rVarInBinaryExp instanceof IASTUnaryExpression) ||
//					!(rVarInBinaryExp instanceof IASTIdExpression) ||
//					!(rVarInBinaryExp instanceof IASTLiteralExpression))
//			{
//				throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
//			}

			rightVariable = "+ [ " + leftVarNameInBinExp + " " + rightVarNameInBinExp + " ]";
			simpIns =  new SimplifiedInstruction(leftVariable, rightVariable, Operator.equals);

			break;
		}

		case IASTBinaryExpression.op_minus:
		{
			// Addition
			// a = a - b
			// TODO unhandled case
//			if(		!(lVarInBinaryExp instanceof IASTUnaryExpression) ||
//					!(lVarInBinaryExp instanceof IASTIdExpression) ||
//					!(lVarInBinaryExp instanceof IASTLiteralExpression) ||
//					!(rVarInBinaryExp instanceof IASTUnaryExpression) ||
//					!(rVarInBinaryExp instanceof IASTIdExpression) ||
//					!(rVarInBinaryExp instanceof IASTLiteralExpression))
//			{
//				throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
//			}

			rightVariable = "+ [ " + leftVarNameInBinExp + " -" + rightVarNameInBinExp + " ]";
			simpIns =  new SimplifiedInstruction(leftVariable, rightVariable, Operator.equals);

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
						rightVariable = "* " + leftVarNameInBinExp + " " + rightVarNameInBinExp;
						simpIns =  new SimplifiedInstruction(leftVariable, rightVariable, Operator.equals);
					}
					else{
						throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
					}
				}
				else{
					throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
				}

			}
			/** a = b * ? */
			else if(lVarInBinaryExp instanceof IASTIdExpression){

				/** a = b * 2 */
				if(rVarInBinaryExp instanceof IASTLiteralExpression){
					rightVariable = "* " + leftVarNameInBinExp + " " + rightVarNameInBinExp;
					simpIns =  new SimplifiedInstruction(leftVariable, rightVariable, Operator.equals);
				}
				// a = b * -2 
				else if(rVarInBinaryExp instanceof IASTUnaryExpression){
					IASTUnaryExpression unaryExpression = (IASTUnaryExpression) rVarInBinaryExp;
					int operator = unaryExpression.getOperator ();

					if(operator == IASTUnaryExpression.op_minus){
						IASTExpression unaryOperand = unaryExpression.getOperand();
						if(unaryOperand instanceof IASTLiteralExpression){
							rightVariable = "* " + leftVarNameInBinExp + " " + rightVarNameInBinExp;
							simpIns =  new SimplifiedInstruction(leftVariable, rightVariable, Operator.equals);
						}
						else{
							throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
						}
					}
					else{
						throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
					}
				}

				// a = b * c
				else if(rVarInBinaryExp instanceof IASTIdExpression){
					simpIns = new SimplifiedInstruction(leftVariable, "__________cpa_________unknownVal___", Operator.equals);
				}
				else{
					throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
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
						rightVariable = "* " + leftVarNameInBinExp + " " + rightVarNameInBinExp;
						simpIns =  new SimplifiedInstruction(leftVariable, rightVariable, Operator.equals);
					}
					/** a = 8 * 9 */
					else if(rVarInBinaryExp instanceof IASTLiteralExpression){
						//Cil eliminates this case
						throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
					}
				}
				else {
					throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
				}
			}

			else{
				throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
			}

			break;
		}

		case IASTBinaryExpression.op_divide:
		{
			simpIns = new SimplifiedInstruction(leftVariable, "__________cpa_________unknownVal___", Operator.equals);
			break;
		}

		case IASTBinaryExpression.op_modulo:
		{
			simpIns = new SimplifiedInstruction(leftVariable, "__________cpa_________unknownVal___", Operator.equals);
			break;
		}

		default: throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());

		}
		handleAssignmetQuery(predAbsElement, simpIns);
	}

	private void handleCasting(PredicateAbstractionElement predAbsElement, IASTExpression op1,
			IASTExpression op2, CFAEdge cfaEdge) throws PredicateAbstractionTransferException {
		String functionName = cfaEdge.getPredecessor().getFunctionName();
		String leftVar = op1.getRawSignature ();

		IASTExpression castOp = (((IASTCastExpression)op2).getOperand());
		//IASTTypeId typeOfOperator = (((IASTCastExpression)op2).getTypeId());

		// Only casting to double is valid
		if((((IASTCastExpression)op2).getTypeId().getRawSignature()).contains("double") ||
				(((IASTCastExpression)op2).getTypeId().getRawSignature()).contains("float")){
			SimplifiedInstruction simpIns = new SimplifiedInstruction();

			/** double a = (double) 7 */
			if(castOp instanceof IASTLiteralExpression){
				int typeOfCastLiteral = ((IASTLiteralExpression)castOp).getKind();
				if( typeOfCastLiteral ==  IASTLiteralExpression.lk_integer_constant || 
						typeOfCastLiteral == IASTLiteralExpression.lk_float_constant)
				{
					String val = castOp.getRawSignature();
					simpIns = new SimplifiedInstruction(leftVar, val, Operator.equals);
				}
			}
			/** For handling expressions of form a = (double) b */
			else if(castOp instanceof IASTIdExpression){
				IASTIdExpression exp = ((IASTIdExpression)castOp);
				String rightVar = exp.getRawSignature();

				// TODO test here
				System.out.println("Right hand side: " + rightVar);

				simpIns = new SimplifiedInstruction(leftVar, rightVar, Operator.equals);
			}

			else{
				throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
			}

			handleAssignmetQuery(predAbsElement, simpIns);
		}
		// If we don't cast to double
		else{
			SimplifiedInstruction simpIns = new SimplifiedInstruction(leftVar, "__________cpa_________unknownVal___", Operator.equals);
			handleAssignmetQuery(predAbsElement, simpIns);
		}
	}

	private void handleAssignmetQuery(PredicateAbstractionElement predAbsElement, SimplifiedInstruction ins){
		String prevState = predAbsElement.getRegion();
		try {
			predAbsElement.updateAssignment(prevState, ins);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

//	TODO function calls
//	private void handleFunctionCall(OctElement prevOctElement, FunctionCallEdge callEdge) {
//	OctElement octElement = prevOctElement;
//	FunctionDefinitionNode functionEntryNode = (FunctionDefinitionNode)callEdge.getSuccessor();
//	String calledFunctionName = functionEntryNode.getFunctionName();
//	String callerFunctionName = callEdge.getPredecessor().getFunctionName();

//	List<String> paramNames = functionEntryNode.getFunctionParameterNames();
//	// TODO here we assume that all parameters are type of int or double
//	for(int i=0; i<paramNames.size(); i++){
//	octElement.update(LibraryAccess.addDimension(octElement, 1));
//	String paramName = paramNames.get(i);
//	octElement.addVar(paramName, calledFunctionName);
//	}

//	octElement.update(LibraryAccess.addDimension(octElement, 1));
//	octElement.addVar("___cpa_temp_result_var_" + functionEntryNode.getFunctionName() + "()", calledFunctionName);

//	IASTExpression[] arguments = callEdge.getArguments();
//	assert (paramNames.size() == arguments.length);

//	for(int i=0; i<arguments.length; i++){

//	IASTExpression argument = arguments[i];
//	String paramName = paramNames.get(i);

//	if(argument instanceof IASTLiteralExpression){
//	String argumentValue = argument.getRawSignature ();

//	int typeOfLiteral = ((IASTLiteralExpression)argument).getKind();
//	if( typeOfLiteral ==  IASTLiteralExpression.lk_integer_constant || 
//	typeOfLiteral == IASTLiteralExpression.lk_float_constant)
//	{
//	int lvar = octElement.getVarId(paramName, calledFunctionName);
//	double val = Double.valueOf(argumentValue).doubleValue();
//	Num n = new Num(val);
//	Num[] array = new Num[octElement.getNumberOfVars()+1];

//	for(int j=0; j<array.length-1; j++){
//	array[j] = new Num(0);
//	}
//	array[array.length-1] = n;
//	octElement.update(LibraryAccess.assignVar(octElement, lvar, array));
//	}
//	}

//	else if(argument instanceof IASTIdExpression){

//	IASTIdExpression varId = ((IASTIdExpression)argument);
//	String nameOfVar = varId.getRawSignature();

//	int lvar = octElement.getVarId(paramName, calledFunctionName);
//	int var2 = octElement.getVarId(nameOfVar, callerFunctionName);
//	Num[] array = new Num[octElement.getNumberOfVars()+1];

//	for(int j=0; j<array.length-1; j++){
//	array[j] = new Num(0);
//	}

//	array[array.length-1] = new Num(0);
//	array[var2] = new Num(1);
//	octElement.update(LibraryAccess.assignVar(octElement, lvar, array));
//	}
//	}
//	}

//	private void handleRecursiveFunctionCall(OctElement octElement, FunctionCallEdge callEdge){

//	FunctionDefinitionNode functionEntryNode = (FunctionDefinitionNode)callEdge.getSuccessor();
//	List<String> paramNames = functionEntryNode.getFunctionParameterNames();
//	// TODO here we assume that all parameters are type of int or double

//	IASTExpression[] arguments = callEdge.getArguments();
//	assert (paramNames.size() == arguments.length);

//	for(int i=0; i<arguments.length; i++){

//	IASTExpression argument = arguments[i];
//	String paramName = paramNames.get(i);

//	if(argument instanceof IASTLiteralExpression){
//	String argumentValue = argument.getRawSignature ();

//	int typeOfLiteral = ((IASTLiteralExpression)argument).getKind();
//	if( typeOfLiteral ==  IASTLiteralExpression.lk_integer_constant || 
//	typeOfLiteral == IASTLiteralExpression.lk_float_constant)
//	{
//	int lvar = octElement.getVarId(paramName);
//	double val = Double.valueOf(argumentValue).doubleValue();
//	Num n = new Num(val);
//	Num[] array = new Num[octElement.getNumberOfVars()+1];

//	for(int j=0; j<array.length-1; j++){
//	array[j] = new Num(0);
//	}
//	array[array.length-1] = n;
//	octElement.update(LibraryAccess.assignVar(octElement, lvar, array));
//	}
//	}

//	else if(argument instanceof IASTIdExpression){

//	IASTIdExpression varId = ((IASTIdExpression)argument);
//	String nameOfVar = varId.getRawSignature();
//	int lvar = octElement.getVarId(paramName);
//	int var2 = octElement.getVarId(nameOfVar);
//	Num[] array = new Num[octElement.getNumberOfVars()+1];

//	for(int j=0; j<array.length-1; j++){
//	array[j] = new Num(0);
//	}

//	array[array.length-1] = new Num(0);
//	array[var2] = new Num(1);
//	octElement.update(LibraryAccess.assignVar(octElement, lvar, array));
//	}
//	}
//	}

//	private void handleExitFromFunction(OctElement octElement,
//	IASTExpression expression, StatementEdge statementEdge) throws OctagonTransferException {

//	String returnedFunction = statementEdge.getPredecessor().getFunctionName();

//	if(expression instanceof IASTUnaryExpression){
//	IASTUnaryExpression unaryExp = (IASTUnaryExpression)expression;
//	if(unaryExp.getOperator() == IASTUnaryExpression.op_bracketedPrimary){
//	IASTExpression exprInParanhesis = unaryExp.getOperand();
//	if(exprInParanhesis instanceof IASTLiteralExpression){
//	IASTLiteralExpression litExpr = (IASTLiteralExpression)exprInParanhesis;
//	String fName = ((CFAExitNode)statementEdge.getSuccessor()).getFunctionName();
//	int resultvarID = octElement.getVarId("___cpa_temp_result_var_" + fName + "()", returnedFunction);
//	String literalValue = litExpr.getRawSignature ();
//	int typeOfLiteral = (litExpr.getKind());
//	if( typeOfLiteral ==  IASTLiteralExpression.lk_integer_constant || 
//	typeOfLiteral == IASTLiteralExpression.lk_float_constant)
//	{
//	double val = Double.valueOf(literalValue).doubleValue();
//	Num n = new Num(val);
//	Num[] array = new Num[octElement.getNumberOfVars()+1];

//	for(int j=0; j<array.length-1; j++){
//	array[j] = new Num(0);
//	}
//	array[array.length-1] = n;
//	octElement.update(LibraryAccess.assignVar(octElement, resultvarID, array));
//	}
//	}

//	else if(exprInParanhesis instanceof IASTIdExpression){
//	IASTIdExpression idExpr = (IASTIdExpression)exprInParanhesis;
//	String fName = ((CFAExitNode)statementEdge.getSuccessor()).getFunctionName();
//	int lvar = octElement.getVarId("___cpa_temp_result_var_" + fName + "()", returnedFunction);
//	String idExpName = idExpr.getRawSignature ();

//	int rvar = octElement.getVarId(idExpName, returnedFunction);
//	Num[] array = new Num[octElement.getNumberOfVars()+1];

//	for(int i=0; i<array.length-1; i++){
//	array[i] = new Num(0);
//	}

//	array[array.length-1] = new Num(0);
//	array[rvar] = new Num(1);
//	octElement.update(LibraryAccess.assignVar(octElement, lvar, array));
//	}
//	}
//	}
//	else if(expression instanceof IASTBinaryExpression){
//	// TODO is there such a case?
//	}
//	else {
//	throw new OctagonTransferException("Unhandled case");
//	}
//	}

//	private void handleSummaryEdge(OctElement octElement, String varName,
//	String functionName, String callerFunctionName) {

//	int lvar = octElement.getVarId(varName, callerFunctionName);

//	System.out.println("___cpa_temp_result_var_" + functionName + "()");
//	int rvar = octElement.getVarId("___cpa_temp_result_var_" + functionName + "()", functionName);
//	Num[] array = new Num[octElement.getNumberOfVars()+1];

//	for(int i=0; i<array.length-1; i++){
//	array[i] = new Num(0);
//	}

//	array[array.length-1] = new Num(0);
//	array[rvar] = new Num(1);
//	octElement.update(LibraryAccess.assignVar(octElement, lvar, array));
//	}

//	private void removeFunctionParameters(OctElement octElement,
//	List<String> parameters, String fname) {
////	int numOfValuesToRemove = parameters.size()+1;
////	// TODO removing from the end of octagon?
////	octElement.update(LibraryAccess.removeDimension(octElement, numOfValuesToRemove));
////	for(int i=0; i<parameters.size(); i++){
////	octElement.removeVar((String)parameters.get(i));
////	}

////	octElement.removeVar("___cpa_temp_result_var_" + fname + "()");
////	TempVariableCount tempVars = octElement.getTempVarList().get(fname);
////	Iterator<String> it = tempVars.getIterator();
////	while(it.hasNext()){
////	String s = it.next();
////	octElement.removeVar(s);
////	octElement.update(LibraryAccess.removeDimension(octElement, 1));
////	}
////	octElement.getTempVarList().remove(fname);

//	int numOfValuesToRemove = octElement.removeVariablesOfFunction(fname);
//	octElement.update(LibraryAccess.removeDimension(octElement, numOfValuesToRemove));

//	}
}
