package cpaplugin.cpa.cpas.predicateabstraction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTPointerOperator;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;

import predicateabstraction.Operator;
import predicateabstraction.PredAbstractionConstants;
import predicateabstraction.Predicate;
import predicateabstraction.SimplifiedInstruction;
import predicateabstraction.ThreeValuedBoolean;
import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.CFAExitNode;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cfa.objectmodel.c.AliasedPointers;
import cpaplugin.cfa.objectmodel.c.AssumeEdge;
import cpaplugin.cfa.objectmodel.c.CallToReturnEdge;
import cpaplugin.cfa.objectmodel.c.FunctionCallEdge;
import cpaplugin.cfa.objectmodel.c.FunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.c.ReturnEdge;
import cpaplugin.cfa.objectmodel.c.StatementEdge;
import cpaplugin.cmdline.CPAMain;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.exceptions.CPAException;
import cpaplugin.exceptions.PredicateAbstractionTransferException;
import cpaplugin.logging.CPACheckerLogger;
import cpaplugin.logging.CustomLogLevel;

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
		System.out.println(cfaEdge.getRawStatement());
		PredicateAbstractionElement predAbsElement = (PredicateAbstractionElement) element;
		switch (cfaEdge.getEdgeType ())
		{
		case StatementEdge:
		{
			predAbsElement = predAbsElement.clone ();

			StatementEdge statementEdge = (StatementEdge) cfaEdge;
			IASTExpression expression = statementEdge.getExpression ();

			if(statementEdge.isJumpEdge()){
				// this is the main function
				if(expression == null){

				}
				else{
					try {
						handleExitFromFunction (predAbsElement, expression, statementEdge);
					} catch (PredicateAbstractionTransferException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

			else{
				try {
					handleStatement (predAbsElement, expression, cfaEdge);
				} catch (PredicateAbstractionTransferException e) {
					e.printStackTrace();
				}
			}
			
			predAbsElement = predAbsElement.clone ();
			
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
			//System.out.println("Blank Edge -- Do Nothing");
			break;
		}

		case FunctionCallEdge: 
		{
			FunctionCallEdge functionCallEdge = (FunctionCallEdge) cfaEdge;
			CallToReturnEdge summaryEdge = cfaEdge.getPredecessor().getLeavingSummaryEdge();
			// TODO String?
			summaryEdge.registerElementOnSummaryEdge("cpaplugin.cpa.cpas.predicateabstraction.PredicateAbstractionCPA", predAbsElement);

			predAbsElement = predAbsElement.clone ();

			if(functionCallEdge.isExternalCall()){
				try {
					handleExternalCalls(predAbsElement, functionCallEdge);
				} catch (PredicateAbstractionTransferException e) {
					e.printStackTrace();
				}
				break;
			}
			// save the data on the summary edge before proceeding with the function call

			//			if(functionCallEdge.isRecursive()){
//			handleRecursiveFunctionCall(predAbsElement, functionCallEdge);
//			}
//			else{
			try {
				CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel, "Function Call Edge from node " +  
						cfaEdge.getPredecessor().getNodeNumber() + " to " + cfaEdge.getSuccessor().getNodeNumber() );
				handleFunctionCall(predAbsElement, functionCallEdge, summaryEdge);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (PredicateAbstractionTransferException e) {
				e.printStackTrace();
			}
//			}
			break;
		}

		// handle return from function
		case ReturnEdge:
		{
			predAbsElement = predAbsElement.clone ();
			ReturnEdge exitEdge = (ReturnEdge) cfaEdge;

			try {
				handleReturnFromFunction (predAbsElement, exitEdge);
			} catch (PredicateAbstractionTransferException e) {
				e.printStackTrace();
			}


//			if(exitEdge.isExitingRecursiveCall()){
//			// TODO recursive return
//			handleSummaryEdge(octElement, varName, pfName, callerFunctionName);
//			}
//			else if(predecessorNode.getFunctionName().compareTo(CPAConfig.entryFunction) == 0){
//			//Do nothing
//			}
//			else{
//			handleSummaryEdge(octElement, varName, pfName, callerFunctionName);
//			CFANode callerNode =  summaryEdge.getPredecessor();
//			assert ((FunctionDefinitionNode)callerNode.getLeavingEdge(0) != null);
//			assert ((FunctionDefinitionNode)callerNode.getLeavingEdge(0) instanceof FunctionDefinitionNode);
//			FunctionDefinitionNode fdefNode = (FunctionDefinitionNode)callerNode.getLeavingEdge(0).getSuccessor();
//			List<String> parameters = fdefNode.getFunctionParameterNames();
//			removeFunctionParameters(octElement, parameters, pfName);
//			}
			break;
		}

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
		
		long end = System.currentTimeMillis();

		return predAbsElement;
	}

	public List<AbstractElement> getAllAbstractSuccessors (AbstractElement element) throws CPAException
	{
		throw new CPAException ("Cannot get all abstract successors from non-location domain");
	}

	private void handleFunctionCall(PredicateAbstractionElement predAbsElement,
			FunctionCallEdge functionCallEdge, CallToReturnEdge summaryEdge) throws IOException, PredicateAbstractionTransferException {
		FunctionDefinitionNode functionEntryNode = (FunctionDefinitionNode)functionCallEdge.getSuccessor();
		String calledFunctionName = functionEntryNode.getFunctionName();
		String callerFunctionName = functionCallEdge.getPredecessor().getFunctionName();
		//String fileName = functionCallEdge.getSuccessor().

		String previousState = predAbsElement.getRegion();
		PredicateAbstractionElement newElement = new PredicateAbstractionElement(calledFunctionName, functionEntryNode.getContainingFileName());

		List<IASTParameterDeclaration> parameters = functionEntryNode.getFunctionParameters();
		IASTExpression[] arguments = functionCallEdge.getArguments();

		if(calledFunctionName.equals("ERROR")){
			System.out.println("----- ERROR -----");
			System.exit(0);
		}

		CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel, "Function Paramater: " +  functionEntryNode.getFunctionParameterNames());

		assert (parameters.size() == arguments.length);
		String instr = "& [";
		// TODO here we assume that all parameters are type of int or double
		for(int i=0; i<parameters.size(); i++){
			IASTExpression argument = arguments[i];
			IASTParameterDeclaration parameter = parameters.get(i);
			String parameterName = parameter.getDeclarator().getName().toString();
			String argumentName = argument.getRawSignature();

			if(parameter.getDeclarator().getPointerOperators().length == 1){
				IASTPointerOperator pointOp= parameter.getDeclarator().getPointerOperators()[0];
				if(pointOp.getRawSignature().equals("*")){
					summaryEdge.registerAliasesOnFunctionCalls(argumentName, parameterName);
				}
				else {
					throw new PredicateAbstractionTransferException("Unhandled case --> " +
					"Only *p operations are handled");
				}
			}
			else if(parameter.getDeclarator().getPointerOperators().length == 0){

			}
			else{
				throw new PredicateAbstractionTransferException("Unhandled case --> " +
				"Only *p operations are handled");
			}
			instr = instr + " = " + argumentName + " " + parameterName;
		}

		instr = instr + " ]";
		CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel, "Query for function call: " +  instr);

		newElement.updateFunctionCall(previousState, instr);

		CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel, "State is updated to: " +  newElement);

		predAbsElement.empty();
		predAbsElement.addPredicates(newElement);
	}

	private void handleExitFromFunction (
			PredicateAbstractionElement predAbsElement,
			IASTExpression expression, StatementEdge statementEdge) throws PredicateAbstractionTransferException {

		String functionName = statementEdge.getPredecessor().getFunctionName();
		if(functionName.equals(CPAMain.cpaConfig.getProperty("analysis.entryFunction"))){
			return;
		}

		if(expression instanceof IASTUnaryExpression){
			IASTUnaryExpression unaryExp = (IASTUnaryExpression)expression;
			if(unaryExp.getOperator() == IASTUnaryExpression.op_bracketedPrimary){
				IASTExpression exprInParanhesis = unaryExp.getOperand();
				if(exprInParanhesis instanceof IASTLiteralExpression){
					IASTLiteralExpression litExpr = (IASTLiteralExpression)exprInParanhesis;
					String returnSiteFuncName = ((CFAExitNode)statementEdge.getSuccessor()).getFunctionName();
					String literalValue = litExpr.getRawSignature ();
					int typeOfLiteral = (litExpr.getKind());
					if( typeOfLiteral ==  IASTLiteralExpression.lk_integer_constant || 
							typeOfLiteral == IASTLiteralExpression.lk_float_constant)
					{
						double val = Double.valueOf(literalValue).doubleValue();
						Predicate tempElement = new Predicate("___cpa_temp_result_var_", Operator.equals, String.valueOf(val));
						tempElement.setTruthValue(ThreeValuedBoolean.TRUE);
						predAbsElement.addPredicateOnTheFly(tempElement);
					}
					else {
						throw new PredicateAbstractionTransferException("Unhandled case");
					}
				}

				else if(exprInParanhesis instanceof IASTIdExpression){
					IASTIdExpression idExpr = (IASTIdExpression)exprInParanhesis;
					String fName = ((CFAExitNode)statementEdge.getSuccessor()).getFunctionName();
					String idExpName = idExpr.getRawSignature ();

					Predicate tempElement = new Predicate("___cpa_temp_result_var_", Operator.equals, idExpName);
					tempElement.setTruthValue(ThreeValuedBoolean.TRUE);
					predAbsElement.addPredicateOnTheFly(tempElement);
				}
				else if(exprInParanhesis instanceof IASTBinaryExpression){
					IASTBinaryExpression binExpr = (IASTBinaryExpression)exprInParanhesis;
					IASTExpression leftHandSide = binExpr.getOperand1();
					String nameOfLeftVar = leftHandSide.getRawSignature();
					IASTExpression rightHandSide = binExpr.getOperand2();
					String nameOfRightVar = rightHandSide.getRawSignature();
					int binOperator = binExpr.getOperator();

					switch (binOperator)
					{
					case IASTBinaryExpression.op_plus:
					{
						String instr = "+ [ " + nameOfLeftVar + " " + nameOfRightVar + " ]";
						Predicate tempElement = new Predicate("___cpa_temp_result_var_", Operator.equals, instr);
						tempElement.setTruthValue(ThreeValuedBoolean.TRUE);
						predAbsElement.addPredicateOnTheFly(tempElement);
						break;
					}
					case IASTBinaryExpression.op_minus:
					{
						String instr = "+ [ " + nameOfLeftVar + " -" + nameOfRightVar + " ]";
						Predicate tempElement = new Predicate("___cpa_temp_result_var_", Operator.equals, instr);
						tempElement.setTruthValue(ThreeValuedBoolean.TRUE);
						predAbsElement.addPredicateOnTheFly(tempElement);
						break;
					}
					default: throw new PredicateAbstractionTransferException("Unhandled case");
					}
				}
				else if(exprInParanhesis instanceof IASTUnaryExpression){
					IASTUnaryExpression unaryExpInParant = (IASTUnaryExpression) exprInParanhesis;
					int operationInParant = unaryExpInParant.getOperator();

					if(operationInParant==IASTUnaryExpression.op_star){
						IASTExpression returnValue = unaryExpInParant.getOperand();
						String variableName = returnValue.getRawSignature();
						String modifiedReturnValue = PredAbstractionConstants.getStarOperator(variableName);
						Predicate tempElement = new Predicate("___cpa_temp_result_var_", Operator.equals, modifiedReturnValue);
						tempElement.setTruthValue(ThreeValuedBoolean.TRUE);
						predAbsElement.addPredicateOnTheFly(tempElement);
					}
					else{
						throw new PredicateAbstractionTransferException("Unhandled case");
					}
				}
				else{
					throw new PredicateAbstractionTransferException("Unhandled case");
				}
			}
		}
		else if(expression instanceof IASTBinaryExpression){
			throw new PredicateAbstractionTransferException("Unhandled case");
		}
		else {
			throw new PredicateAbstractionTransferException("Unhandled case");
		}
	}

	private void handleReturnFromFunction(
			PredicateAbstractionElement predAbsElement, CFAEdge exitEdge) throws PredicateAbstractionTransferException {
		CFANode predecessorNode = exitEdge.getPredecessor();
		CFANode successorNode = exitEdge.getSuccessor();
		String pfName = predecessorNode.getFunctionName();
		String callerFunctionName = successorNode.getFunctionName();
		List<String> modifiedVariables = new ArrayList<String>();

		CallToReturnEdge summaryEdge = (CallToReturnEdge)successorNode.getEnteringSummaryEdge();

		IASTExpression methodCallExpression = summaryEdge.getExpression();

		if(summaryEdge.hasAnyPointerParameters()){
			for(AliasedPointers ap:summaryEdge.getAliasedPointersList()){
				modifiedVariables.add("__cpa___starOp__[" + ap.getFirstVar() + "]");
				Predicate pred = new Predicate(ap.getFirstVar(), Operator.equals, ap.getSecondVar());
				pred.setTruthValue(ThreeValuedBoolean.TRUE);
				predAbsElement.addPredicateOnTheFly(pred);
			}
		}

		if(methodCallExpression instanceof IASTFunctionCallExpression){

		}

		else if(methodCallExpression instanceof IASTBinaryExpression){
			IASTBinaryExpression binExpr = (IASTBinaryExpression)methodCallExpression;
			String modifiedVariableName = binExpr.getOperand1().getRawSignature();
			modifiedVariables.add(modifiedVariableName);
			String varName = ((IASTIdExpression)binExpr.getOperand1()).getRawSignature();
			Predicate pred = new Predicate(modifiedVariableName, Operator.equals, "___cpa_temp_result_var_");
			pred.setTruthValue(ThreeValuedBoolean.TRUE);
			predAbsElement.addPredicateOnTheFly(pred);
		}

		else{
			throw new PredicateAbstractionTransferException("Unhandled case ");
		}

		PredicateAbstractionElement newElement = (PredicateAbstractionElement)summaryEdge.retrieveAbstractElement("cpaplugin.cpa.cpas.predicateabstraction.PredicateAbstractionCPA");
		CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel, "New element is " + newElement );

		String prevRegion = newElement.getRegionWithoutVariable(modifiedVariables);
		String exitRegion = predAbsElement.getRegion();

		String query = " & [ " + prevRegion + " " + exitRegion + " ] ";
		CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel, "Return from function, prev region" +
				" is " +  prevRegion + " exit region is " + exitRegion);

		predAbsElement.empty();
		predAbsElement.addPredicates(newElement);

		try {
			predAbsElement.updateFunctionReturn(query);
		} catch (IOException e) {
			e.printStackTrace();
		}
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

		if (expression instanceof IASTBinaryExpression) {
			IASTBinaryExpression binExp = ((IASTBinaryExpression)expression);
			int opType = binExp.getOperator ();
			IASTExpression op1 = binExp.getOperand1();
			IASTExpression op2 = binExp.getOperand2();
			String leftOperator;
			String rightOperator;

			if(op1 instanceof IASTFieldReference){
				String fieldName = ((IASTFieldReference)op1).getRawSignature();
				leftOperator = "\"".concat(fieldName).concat("\"");
			}

			else{
				leftOperator = op1.getRawSignature();
			}

			if(op2 instanceof IASTFieldReference){
				String fieldName = ((IASTFieldReference)op2).getRawSignature();
				rightOperator = "\"".concat(fieldName).concat("\"");
			}

			else{
				rightOperator = op2.getRawSignature();
			}

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
				
				// ! a-> b
				else if(exp1 instanceof IASTFieldReference){
					IASTFieldReference fieldRef = (IASTFieldReference)exp1;
					handleAssumption(predAbsElement, fieldRef, cfaEdge, !truthValue);
				}
				
				// ! a
				else if(exp1 instanceof IASTIdExpression){
					IASTIdExpression idExpression = (IASTIdExpression)exp1;
					handleAssumption(predAbsElement, idExpression, cfaEdge, !truthValue);
				}

				else {
					throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
				}
			}

			else {
				throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
			}
		}

		else if(expression instanceof IASTIdExpression){
			IASTIdExpression idExp = (IASTIdExpression)expression;
			String leftOperator = idExp.getRawSignature();

			if(truthValue){
				propagateBooleanExpression(predAbsElement, IASTBinaryExpression.op_notequals, leftOperator, "0");
			}
			else{
				propagateBooleanExpression(predAbsElement, IASTBinaryExpression.op_equals, leftOperator, "0");
			}
		}
		
		else if(expression instanceof IASTFieldReference){
			IASTFieldReference filedRefExp = (IASTFieldReference)expression;
			String leftOperator = "\"".concat(filedRefExp.getRawSignature()).concat("\"");
			
			if(truthValue){
				propagateBooleanExpression(predAbsElement, IASTBinaryExpression.op_notequals, leftOperator, "0");
			}
			else{
				propagateBooleanExpression(predAbsElement, IASTBinaryExpression.op_equals, leftOperator, "0");
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

		// First operand is a pointer derefencing
		if (op1 instanceof IASTUnaryExpression)
		{
			System.out.println("cil should be eliminating this case");
		}
		// If first operand is a variable
		else if (op1 instanceof IASTIdExpression)
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
		else{
			throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
		}
	}

	private void handleAssignment(PredicateAbstractionElement predAbsElement, IASTBinaryExpression binaryExpression, CFAEdge cfaEdge) throws PredicateAbstractionTransferException {

		IASTExpression op1 = binaryExpression.getOperand1();
		IASTExpression op2 = binaryExpression.getOperand2();

		// First operand is pointer dereferencing *p = ?
		if (op1 instanceof IASTUnaryExpression)
		{
			IASTUnaryExpression unaryExpression = (IASTUnaryExpression) op1;
			int operator = unaryExpression.getOperator();

			if(operator == IASTUnaryExpression.op_star){
				IASTExpression operand = unaryExpression.getOperand();
				if(operand instanceof IASTIdExpression){
					IASTIdExpression variable = (IASTIdExpression) operand;
					String variableName = variable.getRawSignature();
					if(		op2 instanceof IASTLiteralExpression || 
							op2 instanceof IASTIdExpression ||
							op2 instanceof IASTUnaryExpression)
					{
						String rightVariable = op2.getRawSignature();
						int binaryOp = binaryExpression.getOperator();

						assert(binaryOp == IASTBinaryExpression.op_assign);

						SimplifiedInstruction simpIns =  new SimplifiedInstruction(PredAbstractionConstants.getStarOperator(variableName), rightVariable, Operator.equals);
						handleAssignmetQuery(predAbsElement, simpIns);
					}
				}
				else{
					throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
				}
			}

			else{
				throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
			}

		}
		// If first operand is a variable
		else if (op1 instanceof IASTIdExpression)
		{
			// cases for a = 8.2, a = b, a = -b
			if(		op2 instanceof IASTLiteralExpression || 
					op2 instanceof IASTIdExpression)
			{
				String leftVariable = op1.getRawSignature();
				String rightVariable = op2.getRawSignature();
				int binaryOp = binaryExpression.getOperator();

				assert(binaryOp == IASTBinaryExpression.op_assign);

				SimplifiedInstruction simpIns =  new SimplifiedInstruction(leftVariable, rightVariable, Operator.equals);
				handleAssignmetQuery(predAbsElement, simpIns);
			}

			// a = (cast) ?
			else if(op2 instanceof IASTCastExpression) {
				handleCasting(predAbsElement, op1, op2, cfaEdge);
			}

			// cases a = *p, a = -b
			else if(op2 instanceof IASTUnaryExpression){
				IASTUnaryExpression unaryExp = (IASTUnaryExpression) op2;
				int operator = unaryExp.getOperator();

				String leftVariable = "";
				String rightVariable = ""; 
				if(operator == IASTUnaryExpression.op_minus){
					leftVariable = op1.getRawSignature();
					rightVariable = op2.getRawSignature();
				}

				else if(operator == IASTUnaryExpression.op_star){
					leftVariable = op1.getRawSignature();
					String operandName = unaryExp.getOperand().getRawSignature();
					rightVariable = PredAbstractionConstants.getStarOperator(operandName);
				}

				else{
					throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber()
							+ " operator is " + operator);
				}

				int binaryOp = binaryExpression.getOperator();

				assert(binaryOp == IASTBinaryExpression.op_assign);

				SimplifiedInstruction simpIns =  new SimplifiedInstruction(leftVariable, rightVariable, Operator.equals);
				handleAssignmetQuery(predAbsElement, simpIns);

			}

			// a = b op c
			else if(op2 instanceof IASTBinaryExpression){
				String leftVariable = op1.getRawSignature();
				handleAssignmentOfBinaryOp(predAbsElement, leftVariable, binaryExpression, cfaEdge);
			}

			else if(op2 instanceof IASTFieldReference){
				String fieldName = ((IASTFieldReference)op2).getRawSignature();
				String rightvariable = "\"".concat(fieldName).concat("\"");
				
				String leftVariable = op1.getRawSignature();
				int binaryOp = binaryExpression.getOperator();
				if(binaryOp == IASTBinaryExpression.op_assign){
					SimplifiedInstruction ins = new SimplifiedInstruction(leftVariable, rightvariable, Operator.equals);
					handleAssignmetQuery(predAbsElement, ins);
				}
				else{
					throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
				}
			}

			else{
				throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
			}
		}

		// a->s = X
		else if(op1 instanceof IASTFieldReference){

			IASTFieldReference fieldRef = (IASTFieldReference)op1;

			// cases for a = 8.2, a = b, a = -b
			if(		op2 instanceof IASTLiteralExpression || 
					op2 instanceof IASTIdExpression)
			{
				
				String leftVariable =  "\"".concat(fieldRef.getRawSignature()).concat("\"");
				String rightVariable = op2.getRawSignature();
				int binaryOp = binaryExpression.getOperator();

				assert(binaryOp == IASTBinaryExpression.op_assign);

				SimplifiedInstruction simpIns =  new SimplifiedInstruction(leftVariable, rightVariable, Operator.equals);
				handleAssignmetQuery(predAbsElement, simpIns);
			}

			// a = (cast) ?
			else if(op2 instanceof IASTCastExpression) {
				handleCasting(predAbsElement, op1, op2, cfaEdge);
			}

			// cases a = *p, a = -b
			else if(op2 instanceof IASTUnaryExpression){
				IASTUnaryExpression unaryExp = (IASTUnaryExpression) op2;
				int operator = unaryExp.getOperator();

				String leftVariable = "";
				String rightVariable = ""; 
				if(operator == IASTUnaryExpression.op_minus){
					leftVariable =  "\"".concat(fieldRef.getRawSignature()).concat("\"");
					rightVariable = op2.getRawSignature();
				}

				else if(operator == IASTUnaryExpression.op_star){
					leftVariable =  "\"".concat(fieldRef.getRawSignature()).concat("\"");
					String operandName = unaryExp.getOperand().getRawSignature();
					rightVariable = PredAbstractionConstants.getStarOperator(operandName);
				}

				else{
					throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber()
							+ " operator is " + operator);
				}

				int binaryOp = binaryExpression.getOperator();
				assert(binaryOp == IASTBinaryExpression.op_assign);

				SimplifiedInstruction simpIns =  new SimplifiedInstruction(leftVariable, rightVariable, Operator.equals);
				handleAssignmetQuery(predAbsElement, simpIns);

			}

			// a = b op c
			else if(op2 instanceof IASTBinaryExpression){
				String leftVariable =  "\"".concat(fieldRef.getRawSignature()).concat("\"");
				handleAssignmentOfBinaryOp(predAbsElement, leftVariable, binaryExpression, cfaEdge);
			}
			// a->b = c->d
			else if(op2 instanceof IASTFieldReference){
				String leftVariable =  "\"".concat(fieldRef.getRawSignature()).concat("\"");
				String fieldName = ((IASTFieldReference)op2).getRawSignature();
				String rightVariable =  "\"".concat(fieldName).concat("\"");

				int binaryOp = binaryExpression.getOperator();
				assert(binaryOp == IASTBinaryExpression.op_assign);
				
				SimplifiedInstruction simpIns =  new SimplifiedInstruction(leftVariable, rightVariable, Operator.equals);
				handleAssignmetQuery(predAbsElement, simpIns);			}
			
			else{
				throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
			}

		}

		else{
			throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
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

		/** Binary expression's operand is an addition*/
		case IASTBinaryExpression.op_plus:
		{
			// Addition
			// a = a + b
			// TODO handle this case
//			if(		!(lVarInBinaryExp instanceof IASTUnaryExpression) ||
//			!(lVarInBinaryExp instanceof IASTIdExpression) ||
//			!(lVarInBinaryExp instanceof IASTLiteralExpression) ||
//			!(rVarInBinaryExp instanceof IASTUnaryExpression) ||
//			!(rVarInBinaryExp instanceof IASTIdExpression) ||
//			!(rVarInBinaryExp instanceof IASTLiteralExpression))
//			{
//			throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
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
//			!(lVarInBinaryExp instanceof IASTIdExpression) ||
//			!(lVarInBinaryExp instanceof IASTLiteralExpression) ||
//			!(rVarInBinaryExp instanceof IASTUnaryExpression) ||
//			!(rVarInBinaryExp instanceof IASTIdExpression) ||
//			!(rVarInBinaryExp instanceof IASTLiteralExpression))
//			{
//			throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
//			}

			rightVariable = "+ [ " + leftVarNameInBinExp + " -" + rightVarNameInBinExp + " ]";
			simpIns =  new SimplifiedInstruction(leftVariable, rightVariable, Operator.equals);

			break;
		}

		case IASTBinaryExpression.op_multiply:
		{
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
						throw new PredicateAbstractionTransferException("Cil eliminates this case " + cfaEdge.getPredecessor().getNodeNumber());
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

		IASTExpression castOperand = (((IASTCastExpression)op2).getOperand());

		//IASTTypeId typeOfOperator = (((IASTCastExpression)op2).getTypeId());

//		if((((IASTCastExpression)op2).getTypeId().getRawSignature()).contains("double") ||
//		(((IASTCastExpression)op2).getTypeId().getRawSignature()).contains("float")){
		SimplifiedInstruction simpIns = new SimplifiedInstruction();

		/** double a = (double) 7 */
		if(castOperand instanceof IASTLiteralExpression){
			int typeOfCastLiteral = ((IASTLiteralExpression)castOperand).getKind();
//			if( typeOfCastLiteral ==  IASTLiteralExpression.lk_integer_constant || 
//			typeOfCastLiteral == IASTLiteralExpression.lk_float_constant)
//			{
			String val = castOperand.getRawSignature();
			simpIns = new SimplifiedInstruction(leftVar, val, Operator.equals);
//			}
		}
		/** For handling expressions of form a = (double) b */
		else if(castOperand instanceof IASTIdExpression){
			IASTIdExpression exp = ((IASTIdExpression)castOperand);
			String rightVar = exp.getRawSignature();
			simpIns = new SimplifiedInstruction(leftVar, rightVar, Operator.equals);
		}

		// (void (*)())((void *)0)
		else if(castOperand instanceof IASTUnaryExpression){
			IASTUnaryExpression unaryExp = (IASTUnaryExpression) castOperand;
			if(unaryExp.getOperator() == IASTUnaryExpression.op_bracketedPrimary){
				IASTExpression exprInsidePrths = unaryExp.getOperand();
				if(exprInsidePrths instanceof IASTCastExpression){
					handleCasting(predAbsElement, op1, exprInsidePrths, cfaEdge);
					return;
				}
				else{
					throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
				}
			}
			else{
				throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
			}
		}
		
		/** For handling expressions of the form a = (double) b->d */
		else if(castOperand instanceof IASTFieldReference){
			IASTFieldReference fieldRef = ((IASTFieldReference)castOperand);
			String fieldName = fieldRef.getRawSignature();
			String rightVar = "\"".concat(fieldName).concat("\"");

			
			simpIns = new SimplifiedInstruction(leftVar, rightVar, Operator.equals);
		}

		else{
			throw new PredicateAbstractionTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
		}

		handleAssignmetQuery(predAbsElement, simpIns);
//		}
	}

	private void handleAssignmetQuery(PredicateAbstractionElement predAbsElement, SimplifiedInstruction ins){
		String prevState = predAbsElement.getRegion();
		try {
			predAbsElement.updateAssignment(prevState, ins);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handleExternalCalls(PredicateAbstractionElement predAbsElement, FunctionCallEdge functionCallEdge) throws PredicateAbstractionTransferException {

		IASTExpression expr = functionCallEdge.getSuccessor().getEnteringSummaryEdge().getExpression();

		if(expr instanceof IASTBinaryExpression){
			IASTBinaryExpression binaryExpression = (IASTBinaryExpression) expr;
			IASTExpression leftHandSideExp = binaryExpression.getOperand1();
			if(leftHandSideExp instanceof IASTIdExpression){
				IASTIdExpression variable = (IASTIdExpression)leftHandSideExp;
				String variableName = variable.getRawSignature();
				SimplifiedInstruction simpIns = new SimplifiedInstruction(variableName, "__________cpa_________unknownVal___", Operator.equals);
				handleAssignmetQuery(predAbsElement, simpIns);
			}
			else if(leftHandSideExp instanceof IASTUnaryExpression){
				System.out.println("Unary" + expr.getRawSignature());
				assert(false);
			}
			else {
				throw new PredicateAbstractionTransferException("Unhandled case " + functionCallEdge.getPredecessor().getNodeNumber());
			}
		}
		else if(expr instanceof IASTFunctionCallExpression){
			return;
		}
		else{
			throw new PredicateAbstractionTransferException("Unhandled case " + functionCallEdge.getPredecessor().getNodeNumber());
		}
	}

}
