package cpa.explicit;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTPointerOperator;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.c.AssumeEdge;
import cfa.objectmodel.c.CallToReturnEdge;
import cfa.objectmodel.c.DeclarationEdge;
import cfa.objectmodel.c.FunctionCallEdge;
import cfa.objectmodel.c.FunctionDefinitionNode;
import cfa.objectmodel.c.GlobalDeclarationEdge;
import cfa.objectmodel.c.ReturnEdge;
import cfa.objectmodel.c.StatementEdge;
import exceptions.CPATransferException;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;
import exceptions.ExplicitAnalysisTransferException;

public class ExplicitAnalysisTransferRelation implements TransferRelation {

  private ExplicitAnalysisDomain explicitAnalysisDomain;

  private Set<String> globalVars;

  public ExplicitAnalysisTransferRelation (ExplicitAnalysisDomain explicitAnalysisfUseDomain)
  {
    this.explicitAnalysisDomain = explicitAnalysisfUseDomain;
    globalVars = new HashSet<String>();
  }

  public ExplicitAnalysisDomain getAbstractDomain ()
  {
    return explicitAnalysisDomain;
  }

  @Override
  public AbstractElement getAbstractSuccessor(AbstractElement element,
                                              CFAEdge cfaEdge) throws CPATransferException {
    System.out.println("EDGE" + cfaEdge);
    ExplicitAnalysisElement expAnalysisElement = (ExplicitAnalysisElement) element;

    // check the type of the edge
    switch (cfaEdge.getEdgeType ())
    {

    // if edge is a statement edge, e.g. a = b + c
    case StatementEdge:
    {
      expAnalysisElement = expAnalysisElement.clone();

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
          handleExitFromFunction(expAnalysisElement, expression, statementEdge);
        } catch (ExplicitAnalysisTransferException e) {
          e.printStackTrace();
        }
      }

      // this is a regular statement
      else{
        try {
          handleStatement (expAnalysisElement, expression, cfaEdge);
        } catch (ExplicitAnalysisTransferException e) {
          e.printStackTrace();
        }
      }
      break;
    }

    // edge is a decleration edge, e.g. int a;
    case DeclarationEdge:
    {
      expAnalysisElement = expAnalysisElement.clone();

      DeclarationEdge declarationEdge = (DeclarationEdge) cfaEdge;
      handleDeclaration (expAnalysisElement, declarationEdge);
      break;
    }

    // this is an assumption, e.g. if(a == b)
    case AssumeEdge:
    {
      expAnalysisElement = expAnalysisElement.clone();

      AssumeEdge assumeEdge = (AssumeEdge) cfaEdge;
      IASTExpression expression = assumeEdge.getExpression();
      try {
        handleAssumption (expAnalysisElement, expression, cfaEdge, assumeEdge.getTruthAssumption());
      } catch (ExplicitAnalysisTransferException e) {
        e.printStackTrace();
      }
      break;

    }

    case BlankEdge:
    {
      expAnalysisElement = expAnalysisElement.clone();
      break;
    }

    case FunctionCallEdge:
    {
      expAnalysisElement = expAnalysisElement.clone();
      FunctionCallEdge functionCallEdge = (FunctionCallEdge) cfaEdge;

      // call to an external function
      if(functionCallEdge.isExternalCall())
      {
        // TODO
//      try {
//      handleExternalFunctionCall(expAnalysisElement, functionCallEdge);
//      } catch (ExplicitAnalysisTransferException e) {
//      e.printStackTrace();
//      }
      }
      else{
        try {
          handleFunctionCall(expAnalysisElement, functionCallEdge);
        } catch (ExplicitAnalysisTransferException e) {
          e.printStackTrace();
        }
      }
      break;
    }

    // this is a return edge from function, this is different from return statement
    // of the function. See case for statement edge for details
    case ReturnEdge:
    {
      expAnalysisElement = expAnalysisElement.clone();
      ReturnEdge functionReturnEdge = (ReturnEdge) cfaEdge;
      try {
        handleFunctionReturn(expAnalysisElement, functionReturnEdge);
      } catch (ExplicitAnalysisTransferException e) {
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
    return expAnalysisElement;
  }

  private void handleFunctionReturn( ExplicitAnalysisElement expAnalysisElement,
                                     ReturnEdge functionReturnEdge) throws ExplicitAnalysisTransferException{

    CallToReturnEdge summaryEdge =
      functionReturnEdge.getSuccessor().getEnteringSummaryEdge();
    IASTExpression exprOnSummary = summaryEdge.getExpression();
    ExplicitAnalysisElement previousElem = (ExplicitAnalysisElement)summaryEdge.extractAbstractElement("ExplicitAnalysisElement");
    ExplicitAnalysisElement newElement = previousElem.clone();
    String callerFunctionName = functionReturnEdge.getSuccessor().getFunctionName();
    String calledFunctionName = functionReturnEdge.getPredecessor().getFunctionName();

    //expression is a binary operation, e.g. a = g(b);
    if (exprOnSummary instanceof IASTBinaryExpression) {
      IASTBinaryExpression binExp = ((IASTBinaryExpression)exprOnSummary);
      int opType = binExp.getOperator ();
      IASTExpression op1 = binExp.getOperand1();

      assert(opType == IASTBinaryExpression.op_assign);

      //we expect left hand side of the expression to be a variable
      if(op1 instanceof IASTIdExpression)
      {
        IASTIdExpression leftHandSideVar = (IASTIdExpression)op1;
        String varName = leftHandSideVar.getRawSignature();
        String returnVarName = calledFunctionName + "::" + "___cpa_temp_result_var_";

        for(String globalVar:globalVars){
          if(globalVar.equals(varName)){
            if(expAnalysisElement.contains(returnVarName)){
              newElement.assignConstant(varName, expAnalysisElement.getValueFor(returnVarName));
            }
            else{
              newElement.forget(varName);
            }
          }
          else{
            if(expAnalysisElement.contains(globalVar)){
              newElement.assignConstant(globalVar, expAnalysisElement.getValueFor(globalVar));
            }
            else{
              newElement.forget(varName);
            }
          }
        }

        if(!globalVars.contains(varName)){
          String assignedVarName = getvarName(varName, callerFunctionName);
          if(expAnalysisElement.contains(returnVarName)){
            newElement.assignConstant(assignedVarName, expAnalysisElement.getValueFor(returnVarName));
          }
          else{
            newElement.forget(assignedVarName);
          }
        }
      }
      else{
        throw new ExplicitAnalysisTransferException("Unhandled case " + functionReturnEdge.getPredecessor().getNodeNumber());
      }
    }
    // expression is a unary operation, e.g. g(b);
    else if (exprOnSummary instanceof IASTUnaryExpression)
    {
      // onyl globals
      for(String globalVar:globalVars){
        if(expAnalysisElement.contains(globalVar)){
          newElement.assignConstant(globalVar, expAnalysisElement.getValueFor(globalVar));
        }
        else{
          newElement.forget(globalVar);
        }
      }
    }
    else{
      throw new ExplicitAnalysisTransferException("Unhandled case " + functionReturnEdge.getPredecessor().getNodeNumber());
    }

    expAnalysisElement.update(newElement);
  }

  private void handleFunctionCall(ExplicitAnalysisElement expAnalysisElement,
                                  FunctionCallEdge callEdge)  throws ExplicitAnalysisTransferException{
    FunctionDefinitionNode functionEntryNode = (FunctionDefinitionNode)callEdge.getSuccessor();
    String calledFunctionName = functionEntryNode.getFunctionName();
    String callerFunctionName = callEdge.getPredecessor().getFunctionName();

    List<String> paramNames = functionEntryNode.getFunctionParameterNames();
    IASTExpression[] arguments = callEdge.getArguments();

    assert (paramNames.size() == arguments.length);

    ExplicitAnalysisElement newElement = new ExplicitAnalysisElement();

    for(String globalVar:globalVars){
      if(expAnalysisElement.contains(globalVar)){
        newElement.assignConstant(globalVar, expAnalysisElement.getValueFor(globalVar));
      }
    }

    for(int i=0; i<arguments.length; i++){
      IASTExpression arg = arguments[i];

      if(arg instanceof IASTIdExpression){
        IASTIdExpression idExp = (IASTIdExpression) arg;
        String nameOfArg = idExp.getRawSignature();
        String nameOfParam = paramNames.get(i);

        String formalParamName = getvarName(nameOfParam, calledFunctionName);
        String actualParamName = getvarName(nameOfArg, callerFunctionName);

        if(expAnalysisElement.contains(actualParamName)){
          newElement.assignConstant(formalParamName, expAnalysisElement.getValueFor(actualParamName));
        }
      }

      else if(arg instanceof IASTLiteralExpression){
        IASTLiteralExpression literalExp = (IASTLiteralExpression) arg;
        String stringValOfArg = literalExp.getRawSignature();

        int typeOfLiteral = literalExp.getKind();
        if( typeOfLiteral ==  IASTLiteralExpression.lk_integer_constant 
//          || typeOfLiteral == IASTLiteralExpression.lk_float_constant
        )
        {
          String paramName = paramNames.get(i);
          String formalParamName = getvarName(paramName, calledFunctionName);
          int value = Integer.valueOf(stringValOfArg).intValue();

          newElement.assignConstant(formalParamName, value);
        }
        else{
          throw new ExplicitAnalysisTransferException("Unhandled case");
        }
      }
      else{
        throw new ExplicitAnalysisTransferException("Unhandled case");
      }
    }

    expAnalysisElement.update(newElement);
  }

  private void handleExitFromFunction(ExplicitAnalysisElement expAnalysisElement,
                                      IASTExpression expression,
                                      StatementEdge statementEdge) throws ExplicitAnalysisTransferException
                                      {

    String functionName = statementEdge.getPredecessor().getFunctionName();

    String returnVarName = functionName + "::" + "___cpa_temp_result_var_";

    if(expression instanceof IASTUnaryExpression){
      IASTUnaryExpression unaryExp = (IASTUnaryExpression)expression;
      if(unaryExp.getOperator() == IASTUnaryExpression.op_bracketedPrimary){
        IASTExpression exprInParanhesis = unaryExp.getOperand();
        if(exprInParanhesis instanceof IASTLiteralExpression){
          IASTLiteralExpression litExpr = (IASTLiteralExpression)exprInParanhesis;
          String literalValue = litExpr.getRawSignature ();
          int typeOfLiteral = (litExpr.getKind());
          if( typeOfLiteral ==  IASTLiteralExpression.lk_integer_constant 
              //    || typeOfLiteral == IASTLiteralExpression.lk_float_constant
          )
          {
            int value = Integer.valueOf(literalValue).intValue();
            expAnalysisElement.assignConstant(returnVarName, value);
          }
        }

        else if(exprInParanhesis instanceof IASTIdExpression){
          IASTIdExpression idExpr = (IASTIdExpression)exprInParanhesis;

          String idExpName = idExpr.getRawSignature ();
          String rVarName = getvarName(idExpName, functionName);

          propagateVariableAssignment(expAnalysisElement, returnVarName, rVarName);
        }
        else{
          throw new ExplicitAnalysisTransferException("Unhandled case");
        }
      }
    }
    else if(expression instanceof IASTBinaryExpression){
      throw new ExplicitAnalysisTransferException("Unhandled case");
    }
    else {
      throw new ExplicitAnalysisTransferException("Unhandled case");
    }
                                      }

  private void handleAssumption(ExplicitAnalysisElement expAnalysisElement,
                                IASTExpression expression, CFAEdge cfaEdge, boolean truthValue) throws ExplicitAnalysisTransferException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    // Binary operation
    if (expression instanceof IASTBinaryExpression) {
      IASTBinaryExpression binExp = ((IASTBinaryExpression)expression);
      int opType = binExp.getOperator ();

      IASTExpression op1 = binExp.getOperand1();
      IASTExpression op2 = binExp.getOperand2();

      propagateBooleanExpression(expAnalysisElement, opType, op1, op2, functionName, truthValue);
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
              handleAssumption(expAnalysisElement, binExp2, cfaEdge, !truthValue);
            }
            else {
              throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
            }
          }
          else {
            throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
          }
        }

        else {
          throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
        }
      }
      else {
        throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
      }
    }

    else{
      throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
    }
  }

  private void propagateBooleanExpression(
                                          ExplicitAnalysisElement expAnalysisElement, int opType,
                                          IASTExpression op1, IASTExpression op2, String functionName, boolean truthValue) throws ExplicitAnalysisTransferException {

    // a (bop) ?
    if(op1 instanceof IASTIdExpression)
    {
      // a (bop) 9
      if(op2 instanceof IASTLiteralExpression)
      {
        IASTIdExpression var = (IASTIdExpression)op1;
        String varName = var.getRawSignature();
        int typeOfLiteral = ((IASTLiteralExpression)op2).getKind();
        if( typeOfLiteral ==  IASTLiteralExpression.lk_integer_constant 
            //  || typeOfLiteral == IASTLiteralExpression.lk_float_constant
        )
        {
          int valueOfLiteral = Integer.valueOf(op2.getRawSignature()).intValue();

          // a == 9
          if(opType == IASTBinaryExpression.op_equals) {
            if(truthValue){
              if(expAnalysisElement.contains(getvarName(varName, functionName))){
                if(expAnalysisElement.getValueFor(getvarName(varName, functionName)) != valueOfLiteral){
                  expAnalysisElement.setBottom();
                }
              }
              else{
                expAnalysisElement.assignConstant(getvarName(varName, functionName), valueOfLiteral);
              }
            }
            // ! a == 9
            else {
              propagateBooleanExpression(expAnalysisElement, IASTBinaryExpression.op_notequals, op1, op2, functionName, !truthValue);
            }
          }
          // a != 9
          else if(opType == IASTBinaryExpression.op_notequals)
          {
            if(truthValue){
              if(expAnalysisElement.contains(getvarName(varName, functionName))){
                if(expAnalysisElement.getValueFor(getvarName(varName, functionName)) == valueOfLiteral){
                  expAnalysisElement.setBottom();
                }
              }
              else{
              }
            }
            // ! a != 9
            else {
              propagateBooleanExpression(expAnalysisElement, IASTBinaryExpression.op_equals, op1, op2, functionName, !truthValue);
            }
          }

          // a > 9
          else if(opType == IASTBinaryExpression.op_greaterThan)
          {
            if(truthValue){
              if(expAnalysisElement.contains(getvarName(varName, functionName))){
                if(expAnalysisElement.getValueFor(getvarName(varName, functionName)) <= valueOfLiteral){
                  expAnalysisElement.setBottom();
                }
              }
              else{
              }
            }
            else {
              propagateBooleanExpression(expAnalysisElement, IASTBinaryExpression.op_lessEqual, op1, op2, functionName, !truthValue);
            }
          }
          // a >= 9
          else if(opType == IASTBinaryExpression.op_greaterEqual)
          {
            if(truthValue){
              if(expAnalysisElement.contains(getvarName(varName, functionName))){
                if(expAnalysisElement.getValueFor(getvarName(varName, functionName)) < valueOfLiteral){
                  expAnalysisElement.setBottom();
                }
              }
              else{
              }
            }
            else {
              propagateBooleanExpression(expAnalysisElement, IASTBinaryExpression.op_lessThan, op1, op2, functionName, !truthValue);
            }
          }
          // a < 9
          else if(opType == IASTBinaryExpression.op_lessThan)
          {
            if(truthValue){
              if(expAnalysisElement.contains(getvarName(varName, functionName))){
                if(expAnalysisElement.getValueFor(getvarName(varName, functionName)) >= valueOfLiteral){
                  expAnalysisElement.setBottom();
                }
              }
              else{
              }
            }
            else {
              propagateBooleanExpression(expAnalysisElement, IASTBinaryExpression.op_greaterEqual, op1, op2, functionName, !truthValue);
            }
          }
          // a <= 9
          else if(opType == IASTBinaryExpression.op_lessEqual)
          {
            if(truthValue){
              if(expAnalysisElement.contains(getvarName(varName, functionName))){
                if(expAnalysisElement.getValueFor(getvarName(varName, functionName)) > valueOfLiteral){
                  expAnalysisElement.setBottom();
                }
              }
              else{
              }
            }
            else {
              propagateBooleanExpression(expAnalysisElement, IASTBinaryExpression.op_greaterThan, op1, op2, functionName, !truthValue);
            }
          }
          else{
            throw new ExplicitAnalysisTransferException("Unhandled case ");
          }
        }
        else{
          throw new ExplicitAnalysisTransferException("Unhandled case ");
        }
      }
      // a (bop) b
      else if(op2 instanceof IASTIdExpression)
      {
        IASTIdExpression leftVar = (IASTIdExpression)op1;
        String leftVarName = leftVar.getRawSignature();

        IASTIdExpression rightVar = (IASTIdExpression)op2;
        String rightVarName = rightVar.getRawSignature();

        // a == b
        if(opType == IASTBinaryExpression.op_equals)
        {
          if(truthValue){
            if(expAnalysisElement.contains(getvarName(leftVarName, functionName)) && 
                !expAnalysisElement.contains(getvarName(rightVarName, functionName))){
              expAnalysisElement.assignConstant(getvarName(leftVarName, functionName),
                  expAnalysisElement.getValueFor(getvarName(rightVarName, functionName)));
            }
            else if(expAnalysisElement.contains(getvarName(rightVarName, functionName)) && 
                !expAnalysisElement.contains(getvarName(leftVarName, functionName))){
              expAnalysisElement.assignConstant(getvarName(rightVarName, functionName),
                  expAnalysisElement.getValueFor(getvarName(leftVarName, functionName)));
            }
            else if(expAnalysisElement.contains(getvarName(rightVarName, functionName)) && 
                expAnalysisElement.contains(getvarName(leftVarName, functionName))){
              if(expAnalysisElement.getValueFor(getvarName(rightVarName, functionName)) != 
                expAnalysisElement.getValueFor(getvarName(leftVarName, functionName))){
                expAnalysisElement.setBottom();
              }
            }
          }
          else{
            propagateBooleanExpression(expAnalysisElement, IASTBinaryExpression.op_notequals, op1, op2, functionName, !truthValue);
          }
        }
        // a != b
        else if(opType == IASTBinaryExpression.op_notequals)
        {
          if(expAnalysisElement.contains(getvarName(rightVarName, functionName)) && 
              expAnalysisElement.contains(getvarName(leftVarName, functionName))){
            if(expAnalysisElement.getValueFor(getvarName(rightVarName, functionName)) == 
              expAnalysisElement.getValueFor(getvarName(leftVarName, functionName))){
              expAnalysisElement.setBottom();
            }
          }
          else{
            propagateBooleanExpression(expAnalysisElement, IASTBinaryExpression.op_equals, op1, op2, functionName, !truthValue);
          }
        }
        // a > b
        else if(opType == IASTBinaryExpression.op_greaterThan)
        {
          if(expAnalysisElement.contains(getvarName(leftVarName, functionName)) && 
              expAnalysisElement.contains(getvarName(rightVarName, functionName))){
            if(expAnalysisElement.getValueFor(getvarName(leftVarName, functionName)) <= 
              expAnalysisElement.getValueFor(getvarName(rightVarName, functionName))){
              expAnalysisElement.setBottom();
            }
          }
          else{
            propagateBooleanExpression(expAnalysisElement, IASTBinaryExpression.op_lessEqual, op1, op2, functionName, !truthValue);
          }
        }
        // a >= b
        else if(opType == IASTBinaryExpression.op_greaterEqual)
        {
          if(expAnalysisElement.contains(getvarName(leftVarName, functionName)) && 
              expAnalysisElement.contains(getvarName(rightVarName, functionName))){
            if(expAnalysisElement.getValueFor(getvarName(leftVarName, functionName)) < 
                expAnalysisElement.getValueFor(getvarName(rightVarName, functionName))){
              expAnalysisElement.setBottom();
            }
          }
          else{
            propagateBooleanExpression(expAnalysisElement, IASTBinaryExpression.op_lessThan, op1, op2, functionName, !truthValue);
          }
        }
        // a < b
        else if(opType == IASTBinaryExpression.op_lessThan)
        {
          if(expAnalysisElement.contains(getvarName(leftVarName, functionName)) && 
              expAnalysisElement.contains(getvarName(rightVarName, functionName))){
            if(expAnalysisElement.getValueFor(getvarName(leftVarName, functionName)) >= 
              expAnalysisElement.getValueFor(getvarName(rightVarName, functionName))){
              expAnalysisElement.setBottom();					
            }
          }
          else{
            propagateBooleanExpression(expAnalysisElement, IASTBinaryExpression.op_greaterEqual, op1, op2, functionName, !truthValue);
          }
        }
        // a <= b
        else if(opType == IASTBinaryExpression.op_lessEqual)
        {
          if(expAnalysisElement.contains(getvarName(leftVarName, functionName)) && 
              expAnalysisElement.contains(getvarName(rightVarName, functionName))){
            if(expAnalysisElement.getValueFor(getvarName(leftVarName, functionName)) > 
            expAnalysisElement.getValueFor(getvarName(rightVarName, functionName))){
              expAnalysisElement.setBottom();			
            }
          }
          else{
            propagateBooleanExpression(expAnalysisElement, IASTBinaryExpression.op_greaterThan, op1, op2, functionName, !truthValue);
          }
        }
        else{
          throw new ExplicitAnalysisTransferException("Unhandled case ");
        }
      }
      else{
        throw new ExplicitAnalysisTransferException("Unhandled case ");
      }
    }
    else{
      throw new ExplicitAnalysisTransferException("Unhandled case ");
    }
  }

  private void handleDeclaration(ExplicitAnalysisElement expAnalysisElement,
                                 DeclarationEdge declarationEdge) {

    IASTDeclarator[] declarators = declarationEdge.getDeclarators();
    // IASTDeclSpecifier specifier = declarationEdge.getDeclSpecifier();

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
        // if this is a global variable, add to the list of global variables
        if(declarationEdge instanceof GlobalDeclarationEdge)
        {
          globalVars.add(varName);
        }
      }
    }
  }

  private void handleStatement(ExplicitAnalysisElement expAnalysisElement,
                               IASTExpression expression, CFAEdge cfaEdge) throws ExplicitAnalysisTransferException {
    // expression is a binary operation, e.g. a = b;
    if (expression instanceof IASTBinaryExpression) {
      handleBinaryExpression(expAnalysisElement, expression, cfaEdge);
    }
    // expression is a unary operation, e.g. a++;
    else if (expression instanceof IASTUnaryExpression)
    {
      handleUnaryExpression(expAnalysisElement, expression, cfaEdge);
    }
    else{
      throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
    }
  }

  private void handleUnaryExpression(
                                     ExplicitAnalysisElement expAnalysisElement,
                                     IASTExpression expression, CFAEdge cfaEdge) throws ExplicitAnalysisTransferException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    IASTUnaryExpression unaryExpression = (IASTUnaryExpression) expression;

    int operator = unaryExpression.getOperator ();

    String leftOpName = unaryExpression.getOperand().getRawSignature();
    String varName = getvarName(leftOpName, functionName); 

    // a++, ++a
    if (operator == IASTUnaryExpression.op_postFixIncr ||
        operator == IASTUnaryExpression.op_prefixIncr
    )
    {
      addLiteralToVariable(expAnalysisElement, cfaEdge, varName, varName, 1);
    }
    // a--, --a
    else if(operator == IASTUnaryExpression.op_prefixDecr ||
        operator == IASTUnaryExpression.op_postFixDecr)
    {
      addLiteralToVariable(expAnalysisElement, cfaEdge, varName, varName, -1);
    }

    else
    {
      throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
    }
  }

  private void addLiteralToVariable(
                                    ExplicitAnalysisElement expAnalysisElement, CFAEdge cfaEdge,
                                    String assignedVar, String varName, int val) {
    if(expAnalysisElement.contains(varName)){
      expAnalysisElement.assignConstant(assignedVar, expAnalysisElement.getValueFor(varName) + val);
    }
    else{
      expAnalysisElement.forget(assignedVar);
    }
  }

  private void subtractVariableFromLiteral(
                                           ExplicitAnalysisElement expAnalysisElement, CFAEdge cfaEdge,
                                           String assignedVar, String varName, int val) {
    if(expAnalysisElement.contains(varName)){
      expAnalysisElement.assignConstant(assignedVar, val - expAnalysisElement.getValueFor(varName));
    }
    else{
      expAnalysisElement.forget(assignedVar);
    }
  }

  private void multiplyLiteralWithVariable(
                                           ExplicitAnalysisElement expAnalysisElement, CFAEdge cfaEdge,
                                           String assignedVar, String varName, int val) {
    if(expAnalysisElement.contains(varName)){
      expAnalysisElement.assignConstant(assignedVar, expAnalysisElement.getValueFor(varName) * val);
    }
    else{
      expAnalysisElement.forget(assignedVar);
    }
  }

  private void handleBinaryExpression(
                                      ExplicitAnalysisElement expAnalysisElement,
                                      IASTExpression expression, CFAEdge cfaEdge) throws ExplicitAnalysisTransferException {

    IASTBinaryExpression binaryExpression = (IASTBinaryExpression) expression;
    switch (binaryExpression.getOperator ())
    {
    // a = ?
    case IASTBinaryExpression.op_assign:
    {
      handleAssignment(expAnalysisElement, binaryExpression, cfaEdge);
      break;
    }
    // a += 2
    case IASTBinaryExpression.op_plusAssign:
    case IASTBinaryExpression.op_minusAssign:
    case IASTBinaryExpression.op_multiplyAssign:
    {
      handleOperationAndAssign(expAnalysisElement, binaryExpression, cfaEdge);
      break;
    }
    default: throw new ExplicitAnalysisTransferException("Unhandled case ");
    }
  }

  private void handleOperationAndAssign(
                                        ExplicitAnalysisElement expAnalysisElement,
                                        IASTBinaryExpression binaryExpression, CFAEdge cfaEdge) throws ExplicitAnalysisTransferException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    IASTExpression op1 = binaryExpression.getOperand1();
    IASTExpression op2 = binaryExpression.getOperand2();
    int typeOfOperator = binaryExpression.getOperator();

    // First operand is not an id expression
    if (!(op1 instanceof IASTIdExpression))
    {
      System.out.println("First operand is not a proper variable");
      throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
    }
    // If first operand is an id expression
    else if(op1 instanceof IASTIdExpression)
    {
      IASTIdExpression lvar = ((IASTIdExpression)op1);
      String nameOfLVar = lvar.getRawSignature();
      String varName = getvarName(nameOfLVar, functionName);
      // a op= 2
      if(op2 instanceof IASTLiteralExpression){
        int val = Integer.valueOf(op2.getRawSignature()).intValue();
        // only if literal is intefer or double
        int typeOfLiteral = ((IASTLiteralExpression)op2).getKind();
        if( typeOfLiteral ==  IASTLiteralExpression.lk_integer_constant 
            //	|| typeOfLiteral == IASTLiteralExpression.lk_float_constant
        )
        {
          // a += 2
          if(typeOfOperator == IASTBinaryExpression.op_plusAssign){
            addLiteralToVariable(expAnalysisElement, cfaEdge, varName, varName, val);
          }
          // a -= 2
          else if(typeOfOperator == IASTBinaryExpression.op_minusAssign){
            int negVal = 0 - val;
            addLiteralToVariable(expAnalysisElement, cfaEdge, varName, varName,  negVal);
          }
          // a *= 2
          else if(typeOfOperator == IASTBinaryExpression.op_multiplyAssign){
            multiplyLiteralWithVariable(expAnalysisElement, cfaEdge, varName, varName, val);
          }
          else{
            throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
          }
        }
        else{
          throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
        }
      }
      // a op= b
      else if(op2 instanceof IASTIdExpression){

        IASTIdExpression rvar = ((IASTIdExpression)op2);
        String nameOfRVar = rvar.getRawSignature();
        String rightVar = getvarName(nameOfRVar, functionName);

        // a += b
        if(typeOfOperator == IASTBinaryExpression.op_plusAssign){
          addTwoVariables(expAnalysisElement, cfaEdge, varName, varName, rightVar);
        }
        // a -= b
        else if(typeOfOperator == IASTBinaryExpression.op_minusAssign){
          subtractOneVariable(expAnalysisElement, cfaEdge, varName, varName, rightVar);
        }
        // a *= b
        else if(typeOfOperator == IASTBinaryExpression.op_multiplyAssign){
          multiplyTwoVariables(expAnalysisElement, cfaEdge, varName, varName, rightVar);
        }

        else{
          throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
        }
      }
      else{
        throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
      }
    }
    else{
      throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
    }
  }

  private void multiplyTwoVariables(ExplicitAnalysisElement expAnalysisElement,
                                    CFAEdge cfaEdge, String assignedVar, String leftVar, String rightVar) {
    if(expAnalysisElement.contains(leftVar) && expAnalysisElement.contains(rightVar)){
      expAnalysisElement.assignConstant(assignedVar, 
          (expAnalysisElement.getValueFor(leftVar) * 
              expAnalysisElement.getValueFor(rightVar)));
    }
    else{
      expAnalysisElement.forget(assignedVar);
    }
  }

  private void subtractOneVariable(ExplicitAnalysisElement expAnalysisElement,
                                   CFAEdge cfaEdge, String assignedVar, String leftVar, String rightVar) {
    if(expAnalysisElement.contains(leftVar) && expAnalysisElement.contains(leftVar)){
      expAnalysisElement.assignConstant(assignedVar, 
          (expAnalysisElement.getValueFor(leftVar) - 
              expAnalysisElement.getValueFor(rightVar)));
    }
    else{
      expAnalysisElement.forget(assignedVar);
    }
  }

  private void addTwoVariables(ExplicitAnalysisElement expAnalysisElement,
                               CFAEdge cfaEdge, String assignedVar, String leftVar, String rightVar) {
    if(expAnalysisElement.contains(leftVar) && expAnalysisElement.contains(leftVar)){
      expAnalysisElement.assignConstant(assignedVar, 
          (expAnalysisElement.getValueFor(leftVar) + 
              expAnalysisElement.getValueFor(rightVar)));
    }
    else{
      expAnalysisElement.forget(assignedVar);
    }
  }

  private void handleAssignment(ExplicitAnalysisElement expAnalysisElement,
                                IASTBinaryExpression binaryExpression, CFAEdge cfaEdge) throws ExplicitAnalysisTransferException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    IASTExpression op1 = binaryExpression.getOperand1();
    IASTExpression op2 = binaryExpression.getOperand2();

    // First operand is not an id expression
    if (!(op1 instanceof IASTIdExpression))
    {
      throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
    }
    // If first operand is an id expression
    else if(op1 instanceof IASTIdExpression)
    {
      // a = 8.2
      if(op2 instanceof IASTLiteralExpression){
        handleLiteralAssignment(expAnalysisElement, op1, op2, functionName);
      }
      // a = b
      else if (op2 instanceof IASTIdExpression){
        handleVariableAssignment(expAnalysisElement, op1, op2, functionName);
      }
      // TODO later
//    // a = (cast) ?
//    else if(op2 instanceof IASTCastExpression) {
//    handleCasting(expAnalysisElement, op1, op2, cfaEdge);
//    }
      // a = b op c
      else if(op2 instanceof IASTBinaryExpression){
        handleAssignmentOfBinaryExp(expAnalysisElement, op1, op2, cfaEdge);
      }
      // a = -b
      else if(op2 instanceof IASTUnaryExpression){
        IASTUnaryExpression unaryExp = (IASTUnaryExpression)op2;
        handleUnaryExpAssignment(expAnalysisElement, op1, unaryExp, cfaEdge);
      }
      else{
        throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
      }
    }
    else{
      throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
    }
  }

  private void handleUnaryExpAssignment(
                                        ExplicitAnalysisElement expAnalysisElement, IASTExpression op1,
                                        IASTUnaryExpression unaryExp, CFAEdge cfaEdge) throws ExplicitAnalysisTransferException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    String lParam = op1.getRawSignature ();
    String assignedVar = getvarName(lParam , functionName);

    String nameOfVar = unaryExp.getOperand().getRawSignature();
    String varName = getvarName(nameOfVar, functionName);
    int operatorType = unaryExp.getOperator();
    // a = -b
    if(operatorType == IASTUnaryExpression.op_minus){
      subtractVariableFromLiteral(expAnalysisElement, cfaEdge, assignedVar, varName, 0);
    }
    else {
      throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
    }


  }

  private void handleAssignmentOfBinaryExp(
                                           ExplicitAnalysisElement expAnalysisElement, IASTExpression op1,
                                           IASTExpression op2, CFAEdge cfaEdge) throws ExplicitAnalysisTransferException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    // name of the updated variable, so if a = b + c is handled, lParam is a
    String lParam = op1.getRawSignature ();
    String assignedVar = getvarName(lParam, functionName);
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
            int typeOfLiteral = ((IASTLiteralExpression)unaryOperand).getKind();
            if( typeOfLiteral ==  IASTLiteralExpression.lk_integer_constant){ 
              int value = Integer.valueOf(unaryOperand.getRawSignature()).intValue();
              int negVal = 0 - value;

              IASTIdExpression rvar = ((IASTIdExpression)rVarInBinaryExp);
              String nameOfRVar = rvar.getRawSignature();
              String rightVariable = getvarName(nameOfRVar, functionName);
              addLiteralToVariable(expAnalysisElement, cfaEdge, assignedVar, rightVariable, negVal);
            }
            else{
              throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
            }
          }
          else{
            throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
          }
        }
        else{
          throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
        }

      }
      // a = b + ?, left variable in right hand side of the expression is a variable
      else if(lVarInBinaryExp instanceof IASTIdExpression){
        IASTIdExpression lvar = ((IASTIdExpression)lVarInBinaryExp);
        String nameOfLVar = lvar.getRawSignature();
        String nameOfLeftVarOfBinaryExp = getvarName(nameOfLVar, functionName);

        // a = b + 2
        if(rVarInBinaryExp instanceof IASTLiteralExpression){
          int value = Integer.valueOf(rVarInBinaryExp.getRawSignature()).intValue();
          // only integers are handled
          int typeOfLiteral = ((IASTLiteralExpression)rVarInBinaryExp).getKind();
          if( typeOfLiteral ==  IASTLiteralExpression.lk_integer_constant 
              //|| typeOfLiteral == IASTLiteralExpression.lk_float_constant
          )
          {
            addLiteralToVariable(expAnalysisElement, cfaEdge, assignedVar, nameOfLeftVarOfBinaryExp, value);
          }
          else{
            throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
          }
        }
        // a = b + c,
        else if(rVarInBinaryExp instanceof IASTIdExpression){
          IASTIdExpression rvar = ((IASTIdExpression)rVarInBinaryExp);
          String nameOfRVar = rvar.getRawSignature();
          String nameOfRightVarOfBinaryExp = getvarName(nameOfRVar, functionName);
          addTwoVariables(expAnalysisElement, cfaEdge, assignedVar, nameOfLeftVarOfBinaryExp, nameOfRightVarOfBinaryExp);
        }
        else{
          throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
        }
      }

      // a = 9 + ? left variable in right hand side of the expression is a literal
      else if(lVarInBinaryExp instanceof IASTLiteralExpression){
        // left variable must be an integer or double value
        int typeOfLiteral = ((IASTLiteralExpression)lVarInBinaryExp).getKind();
        if( typeOfLiteral ==  IASTLiteralExpression.lk_integer_constant 
            //|| typeOfLiteral == IASTLiteralExpression.lk_float_constant
        )
        {
          int val = Integer.valueOf(lVarInBinaryExp.getRawSignature()).intValue();
          // a = 8 + b
          if(rVarInBinaryExp instanceof IASTIdExpression){
            IASTIdExpression rvar = ((IASTIdExpression)rVarInBinaryExp);
            String nameOfRVar = rvar.getRawSignature();
            String rightVarName = getvarName(nameOfRVar, functionName);
            addLiteralToVariable(expAnalysisElement, cfaEdge, assignedVar, rightVarName, val);
          }
          // a = 8 + 9
          else if(rVarInBinaryExp instanceof IASTLiteralExpression){
            //Cil eliminates this case
            throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
          }
        }
        else {
          throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
        }
      }

      else{
        throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
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
            int typeOfLiteral = ((IASTLiteralExpression)lVarInBinaryExp).getKind();
            if( typeOfLiteral ==  IASTLiteralExpression.lk_integer_constant 
                //|| typeOfLiteral == IASTLiteralExpression.lk_float_constant
            )
            {

              int value = Integer.valueOf(unaryOperand.getRawSignature()).intValue();
              int negVal = 0 - value;

              IASTIdExpression rvar = ((IASTIdExpression)rVarInBinaryExp);
              String nameOfRVar = rvar.getRawSignature();
              String nameOfRightVariable = getvarName(nameOfRVar, functionName);
              addLiteralToVariable(expAnalysisElement, cfaEdge, assignedVar, nameOfRightVariable, negVal);
            }
            else{
              throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
            }
          }
          else{
            throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
          }
        }
        else{
          throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
        }

      }
      // a = b - ? left variable in right hand side of the expression is a variable
      else if(lVarInBinaryExp instanceof IASTIdExpression){
        IASTIdExpression lvar = ((IASTIdExpression)lVarInBinaryExp);
        String nameOfLVar = lvar.getRawSignature();
        String nameOfLeftVar = getvarName(nameOfLVar, functionName);
        // a = b - 2
        if(rVarInBinaryExp instanceof IASTLiteralExpression){
          // only integers and doubles are handled
          int typeOfLiteral = ((IASTLiteralExpression)rVarInBinaryExp).getKind();
          if( typeOfLiteral ==  IASTLiteralExpression.lk_integer_constant 
              //||typeOfCastLiteral == IASTLiteralExpression.lk_float_constant
          ){
            int val = Integer.valueOf(rVarInBinaryExp.getRawSignature()).intValue();
            int negVal = 0 - val;
            addLiteralToVariable(expAnalysisElement, cfaEdge, assignedVar, nameOfLeftVar, negVal);
          }
          else{
            throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
          }
        }
        // a = b - c
        else if(rVarInBinaryExp instanceof IASTIdExpression){
          IASTIdExpression rvar = ((IASTIdExpression)rVarInBinaryExp);
          String nameOfRVar = rvar.getRawSignature();
          String nameOfRightVar = getvarName(nameOfRVar, functionName);
          addTwoVariables(expAnalysisElement, cfaEdge, assignedVar, nameOfLeftVar, nameOfRightVar);
        }
        else{
          throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
        }
      }

      // a = 8 - ? left variable in right hand side of the expression is a literal
      else if(lVarInBinaryExp instanceof IASTLiteralExpression){
        int typeOfCastLiteral = ((IASTLiteralExpression)lVarInBinaryExp).getKind();
        // only integers and doubles are handled
        if( typeOfCastLiteral ==  IASTLiteralExpression.lk_integer_constant 
            //|| typeOfCastLiteral == IASTLiteralExpression.lk_float_constant
        )
        {
          int val = Integer.valueOf(lVarInBinaryExp.getRawSignature()).intValue();
          // a = 8 - b
          if(rVarInBinaryExp instanceof IASTIdExpression){
            IASTIdExpression rvar = ((IASTIdExpression)rVarInBinaryExp);
            String nameOfRVar = rvar.getRawSignature();
            String nameOfVar = getvarName(nameOfRVar, functionName);
            subtractVariableFromLiteral(expAnalysisElement, cfaEdge, assignedVar, nameOfVar, val);

          }
          // a = 8 - 7
          else if(rVarInBinaryExp instanceof IASTLiteralExpression){
            //Cil eliminates this case
            throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
          }
        }
        else {
          throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
        }
      }
      else{
        throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
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
            int typeOfCastLiteral = ((IASTLiteralExpression)lVarInBinaryExp).getKind();
            if(typeOfCastLiteral ==  IASTLiteralExpression.lk_integer_constant 
                //|| typeOfCastLiteral == IASTLiteralExpression.lk_float_constant
            )
            {
              int value = Integer.valueOf(unaryOperand.getRawSignature()).intValue();
              int negVal = 0 - value;

              IASTIdExpression rvar = ((IASTIdExpression)rVarInBinaryExp);
              String nameOfRVar = rvar.getRawSignature();
              String nameOfVar = getvarName(nameOfRVar, functionName);
              multiplyLiteralWithVariable(expAnalysisElement, cfaEdge, assignedVar, nameOfVar, negVal);
            }
            else{
              throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
            }
          }
          else{
            throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
          }
        }
        else{
          throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
        }

      }
      // a = b * ?
      else if(lVarInBinaryExp instanceof IASTIdExpression){
        IASTIdExpression lvar = ((IASTIdExpression)lVarInBinaryExp);
        String nameOfLVar = lvar.getRawSignature();
        String nameOfVar = getvarName(nameOfLVar, functionName);

        // a = b * 2
        if(rVarInBinaryExp instanceof IASTLiteralExpression){
          int typeOfCastLiteral = ((IASTLiteralExpression)rVarInBinaryExp).getKind();
          if( typeOfCastLiteral ==  IASTLiteralExpression.lk_integer_constant 
              // ||typeOfCastLiteral == IASTLiteralExpression.lk_float_constant
          ) {

            int value = Integer.valueOf(rVarInBinaryExp.getRawSignature()).intValue();
            multiplyLiteralWithVariable(expAnalysisElement, cfaEdge, assignedVar, nameOfVar, value);
          }
          else{
            throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
          }
        }
        // a = b * -2
        else if(rVarInBinaryExp instanceof IASTUnaryExpression){
          IASTUnaryExpression unaryExpression = (IASTUnaryExpression) rVarInBinaryExp;
          int operator = unaryExpression.getOperator ();

          if(operator == IASTUnaryExpression.op_minus){
            IASTExpression unaryOperand = unaryExpression.getOperand();
            if(unaryOperand instanceof IASTLiteralExpression){
              int typeOfCastLiteral = ((IASTLiteralExpression)rVarInBinaryExp).getKind();
              if( typeOfCastLiteral ==  IASTLiteralExpression.lk_integer_constant 
                  // ||typeOfCastLiteral == IASTLiteralExpression.lk_float_constant
              ) {

                int value = Integer.valueOf(rVarInBinaryExp.getRawSignature()).intValue();
                int negVal = 0 - value;
                multiplyLiteralWithVariable(expAnalysisElement, cfaEdge, assignedVar, nameOfVar, negVal);
              }
              else{
                throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
              }
            }
            else{
              throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
            }
          }
          else{
            throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
          }

        }

        // a = b * c
        else if(rVarInBinaryExp instanceof IASTIdExpression){
          String nameOfLeftVarOfBinaryExp = getvarName(nameOfLVar, functionName);
          String nameOfRightVarOfBinaryExp = getvarName(nameOfLVar, functionName);
          multiplyTwoVariables(expAnalysisElement, cfaEdge, assignedVar, nameOfLeftVarOfBinaryExp, nameOfRightVarOfBinaryExp);
        }
        else{
          throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
        }
      }

      // a = 8 * ?
      else if(lVarInBinaryExp instanceof IASTLiteralExpression){

        //Num n = new Num(val);
        int typeOfCastLiteral = ((IASTLiteralExpression)lVarInBinaryExp).getKind();
        if( typeOfCastLiteral ==  IASTLiteralExpression.lk_integer_constant 
            || typeOfCastLiteral == IASTLiteralExpression.lk_float_constant
        )
        {
          int val = Integer.valueOf(lVarInBinaryExp.getRawSignature()).intValue();
          // a = 8 * b
          if(rVarInBinaryExp instanceof IASTIdExpression){
            IASTIdExpression rvar = ((IASTIdExpression)rVarInBinaryExp);
            String nameOfRVar = rvar.getRawSignature();
            String nameOfVar = getvarName(nameOfRVar, functionName);
            multiplyLiteralWithVariable(expAnalysisElement, cfaEdge, assignedVar, nameOfVar, val);
          }
          // a = 8 * 9
          else if(rVarInBinaryExp instanceof IASTLiteralExpression){
            //Cil eliminates this case
            throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
          }
        }
        else {
          throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
        }
      }

      else{
        throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
      }

      break;
    }
    // operand in left hand side of expression is a division
    case IASTBinaryExpression.op_divide:
    {
      expAnalysisElement.forget(assignedVar);
      break;
    }

    // operand in left hand side of expression is modulo op
    case IASTBinaryExpression.op_modulo:
    {
      expAnalysisElement.forget(assignedVar);
      break;
    }
    default: throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getPredecessor().getNodeNumber());
    }
  }

  private void handleVariableAssignment(
                                        ExplicitAnalysisElement expAnalysisElement, IASTExpression op1,
                                        IASTExpression op2, String functionName) {
    String lParam = op1.getRawSignature ();
    String rParam = op2.getRawSignature();

    String leftVarName = getvarName(lParam, functionName);
    String rightVarName = getvarName(rParam, functionName);

    propagateVariableAssignment(expAnalysisElement, leftVarName, rightVarName);

  }

  private void propagateVariableAssignment(
                                           ExplicitAnalysisElement expAnalysisElement,
                                           String assignedVarName,
                                           String varName) {
    if(expAnalysisElement.contains(varName)){
      expAnalysisElement.assignConstant(assignedVarName, expAnalysisElement.getValueFor(varName));
    }
    else{
      expAnalysisElement.forget(assignedVarName);
    }

  }

  private void handleLiteralAssignment(
                                       ExplicitAnalysisElement expAnalysisElement, IASTExpression op1,
                                       IASTExpression op2, String functionName) throws ExplicitAnalysisTransferException {

    String lParam = op1.getRawSignature ();
    String rParam = op2.getRawSignature ();

    int typeOfLiteral = ((IASTLiteralExpression)op2).getKind();
    if( typeOfLiteral ==  IASTLiteralExpression.lk_integer_constant 
//      ||typeOfLiteral == IASTLiteralExpression.lk_float_constant
    )
    {
      String varName = getvarName(lParam, functionName);
      int val = Integer.valueOf(rParam).intValue();
      expAnalysisElement.assignConstant(varName, val);
    }
    else{
      throw new ExplicitAnalysisTransferException("Unhandled case");
    }
  }

  @Override
  public List<AbstractElement> getAllAbstractSuccessors(
      AbstractElement element) throws CPAException, CPATransferException {
    throw new CPAException ("Cannot get all abstract successors from non-location domain");
  }

  public String getvarName(String variableName, String functionName){
    if(globalVars.contains(variableName)){
      return variableName;
    }
    return functionName + "::" + variableName;
  }
}
