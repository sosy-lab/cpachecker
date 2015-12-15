package org.sosy_lab.cpachecker.cfa.postprocessing.sequencer;

import java.math.BigInteger;
import java.util.List;
import java.util.Map.Entry;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context.CThread;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.CFAFunctionUtils;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.CFASequenceBuilder;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.ExpressionUtils;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

// TODO static posix stub functions. Add to POSIX Stub function
public class StubPosixFunctions {

  /**
   * Don't use this due it can only handle special cases
   */
  @Deprecated
  public static void stubThreadCreationNoFunction(
      CStatementEdge functionCallEdge, CThread thread,
      ControlVariables controlVariables, MutableCFA cfa, LogManager logger) throws UnrecognizedCCodeException {

    assert CFAFunctionUtils.isFunctionCallStatement(functionCallEdge);
    CFunctionCall functionCallStatement = (CFunctionCall) functionCallEdge.getStatement();

    List<CExpression> usedParameter = functionCallStatement
        .getFunctionCallExpression().getParameterExpressions();

    if (usedParameter.size() != 4) { throw new UnrecognizedCCodeException(
        "pthread_create function must have 4 parameters in expression",
        functionCallStatement); }

    CFANode predecessor = functionCallEdge.getPredecessor();
    CFANode successor = functionCallEdge.getSuccessor();

    CFASequenceBuilder builder = new CFASequenceBuilder(predecessor, cfa);



    ALeftHandSide givenThreadVariable;
    CExpression threadCreationArgument;

    givenThreadVariable = getGivenPthreadVariable(functionCallStatement, logger);
    assert givenThreadVariable instanceof CLeftHandSide;
    threadCreationArgument = getGivenPthreadCreationArgument(functionCallStatement);

    if (givenThreadVariable != null) {
      CFAEdge edge1 = getSavePthreadId(thread, givenThreadVariable);
      builder.addChainLink(edge1);
    }

    CFAEdge edge2 = getSavePThreadArgument(thread, threadCreationArgument, controlVariables);
    CFAEdge edge3 = getActivateThreadStatement(thread, controlVariables);
    CFAEdge edge5 = getNotFinishedThreadStatement(thread, controlVariables); // TODO only for test purpose
    CFAEdge edge4 = new BlankEdge("", FileLocation.DUMMY, CFASequenceBuilder.DUMMY_NODE, CFASequenceBuilder.DUMMY_NODE, "THREAD CREATION");

    builder.addChainLink(edge2);
    builder.addChainLink(edge3);
    builder.addChainLink(edge5);
    builder.addChainLink(edge4, successor);

    CFACreationUtils.removeEdgeFromNodes(functionCallEdge);

  }

  private static CFAEdge getNotFinishedThreadStatement(CThread thread,
      ControlVariables controlVariables) {
    CArraySubscriptExpression leftHandSide = ExpressionUtils.getArrayVarOfIndex(controlVariables.getIsThreadFinishedDeclaration(), thread.getThreadNumber());

    return ExpressionUtils.getDummyCStaticAssignement(leftHandSide, false);
  }

  private static CFAEdge getActivateThreadStatement(CThread thread,
      ControlVariables controlVariables) {
    CArraySubscriptExpression leftHandSide = ExpressionUtils.getArrayVarOfIndex(controlVariables.getIsThreadActiveArrayDeclaration(), thread.getThreadNumber());

    return ExpressionUtils.getDummyCStaticAssignement(leftHandSide, true);
  }

  private static ALeftHandSide getGivenPthreadVariable (
      CFunctionCall functionCallStatement, LogManager logger) {

    AExpression firstParameter =
        CFAFunctionUtils.getFunctionParameter(functionCallStatement, 0);

    return getVisibleParameter(firstParameter);
  }


  /**
   * Returns a variable if it is visible outside the called function. If the
   * parameter is a pointer address, the corresponding variable will be
   * returned.
   * @param expression - The function call expression
   * @return a leftHandSide if it is visible outside this function, null instead
   */
  private static ALeftHandSide getVisibleParameter(AExpression expression) {
    assert expression instanceof CExpression;

    // remove the cast expression to get the plain leftHandSide
    expression = unpackClassCast(expression);

    // remove the first AMPER to get the plain leftHandSide
    if (expression instanceof AUnaryExpression) {
      return getVariableFromPointer((AUnaryExpression) expression);
    } else if (expression instanceof CIdExpression) {
      // even global variable parameter will called by value and have no effect
      // because they are not visible outside this function
      return null;
    } else if (isNull(expression)) { return null; }


    throw new UnsupportedOperationException(
        "The first parameter of pthread_create must be a valid leftHandSide or NULL(void pointer with CIntegerLiteralExpression)");
  }

  private static AExpression unpackClassCast(AExpression expression) {
    if(expression instanceof CCastExpression) {
      CCastExpression a = (CCastExpression) expression;
      return unpackClassCast(a.getOperand());
    }
    return expression;
  }

  private static ALeftHandSide getVariableFromPointer(AUnaryExpression unary) {
    if(unary.getOperator().equals(UnaryOperator.AMPER)) {
      AExpression t1 = unary.getOperand();
      if(t1 instanceof ALeftHandSide) {
        return (ALeftHandSide) t1;
      }
    }

    throw new UnsupportedOperationException(
        "Cannot handle the first parameter of pthread_create");
  }

  private static boolean isNull(AExpression expression) {
    if(expression instanceof CIntegerLiteralExpression) {
      if(((CIntegerLiteralExpression) expression).getValue().equals(BigInteger.ZERO)) {
        return true;
      }
    }
    return false;
  }

  private static CExpression getGivenPthreadCreationArgument(CFunctionCall functionCallStatement) throws UnrecognizedCCodeException {
    List<CExpression> usedParameter = functionCallStatement.getFunctionCallExpression().getParameterExpressions();

    if (usedParameter.size() != 4) {
      throw new UnrecognizedCCodeException(
          "pthread_create function must have 4 parameters in expression",
          functionCallStatement);
    }
    CExpression exp = usedParameter.get(3);
    return exp;
  }

  private static CFAEdge getSavePthreadId(CThread thread, ALeftHandSide threadId) {
    CIntegerLiteralExpression threadNumber = ExpressionUtils.getThreadNumberNumberExpression(thread);

    AStatementEdge threadIdSave = ExpressionUtils.getDummyAssignement(threadId, threadNumber);
    return threadIdSave;
  }

  private static CFAEdge getSavePThreadArgument(CThread thread, CExpression threadCreationArgument, ControlVariables controlVariables) {
    CIntegerLiteralExpression threadNumber = ExpressionUtils.getThreadNumberNumberExpression(thread);

    CLeftHandSide leftHandSide = ExpressionUtils.getArrayVarOfIndex(controlVariables.getThreadCreationArgumentsArrayDeclaration(), threadNumber);

    return ExpressionUtils.getDummyAssignement(leftHandSide, threadCreationArgument);
  }

  @Deprecated
  public static void stubThreadCreationIntoFunction(
      SequencePreparator threadIdentificator,
      ControlVariables controlVariables, MutableCFA cfa, LogManager logger) {
    for (Entry<CThread, CStatementEdge> aThread : threadIdentificator.getCreationEdges().entrySet()) {
      try {
        StubPosixFunctions.stubThreadCreationNoFunction(aThread.getValue(),
            aThread.getKey(), controlVariables, cfa, logger);
      } catch (UnrecognizedCCodeException e) {
        throw new IllegalArgumentException(e);
      }
    }
  }

}
