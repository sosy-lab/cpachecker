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
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context.CThread;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.CFAFunctionUtils;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.CFASequenceBuilder;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.ExpressionUtils;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

// TODO static posix stub functions. Add to POSIX Stub function
@Deprecated
public class StubPosixFunctions {

  private static CBinaryExpressionBuilder BINARY_BUILDER;

  public static final CExpression FREE = new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.INT, BigInteger.valueOf(-1));
  public static final CExpression DESTROY = new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.INT, BigInteger.valueOf(-2));

  public StubPosixFunctions(MachineModel machineModel, LogManager logger) {

  }

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
//    CFAEdge edge5 = getNotFinishedThreadStatement(thread, controlVariables); // TODO only for test purpose
    CFAEdge edge4 = new BlankEdge("", FileLocation.DUMMY, CFASequenceBuilder.DUMMY_NODE, CFASequenceBuilder.DUMMY_NODE, "THREAD CREATION");

    builder.addChainLink(edge2);
    builder.addChainLink(edge3);
//    builder.addChainLink(edge5);
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

  @Deprecated
  public static void stubMutexIntoFunction(SequencePreparator threadIdentificator,
      ControlVariables controlVariables, MutableCFA cfa, LogManager logger) {

    for(AStatementEdge mutexInit : threadIdentificator.getMutexInitEdges()) {
      try {
        StubPosixFunctions.stubThreadMutexInitNoFunction(mutexInit, cfa, logger);
      } catch (UnrecognizedCCodeException e) {
        throw new IllegalArgumentException(e);
      }
    }
  }

  @Deprecated
  public static void stubMutexLocktoFunction(SequencePreparator threadIdentificator,
      MutableCFA cfa, LogManager logger) {

    for(Entry<CThread, CStatementEdge> mutexLock : threadIdentificator.getMutexLockEdges().entrySet()) {
      try {
        StubPosixFunctions.stubThreadMutexLockNoFunction(mutexLock.getKey(), mutexLock.getValue(), cfa, logger);
      } catch (UnrecognizedCCodeException e) {
        throw new IllegalArgumentException(e);
      }
    }
  }

  @Deprecated
  public static void stubMutexUnlocktoFunction(SequencePreparator threadIdentificator,
      MutableCFA cfa, LogManager logger) {
    for(Entry<CThread, CStatementEdge> mutexUnlock : threadIdentificator.getMutexUnlockEdges().entrySet()) {
      try {
        StubPosixFunctions.stubThreadMutexUnlockNoFunction(mutexUnlock.getKey(), mutexUnlock.getValue(), cfa, logger);
      } catch (UnrecognizedCCodeException e) {
        throw new IllegalArgumentException(e);
      }
    }
  }

  private static void stubThreadMutexUnlockNoFunction(CThread thread, CStatementEdge functionCall,
      MutableCFA cfa, LogManager logger) throws UnrecognizedCCodeException {

    assert CFAFunctionUtils.isFunctionCallStatement(functionCall);
    CFunctionCall functionCallStatement = (CFunctionCall) functionCall.getStatement();

    List<CExpression> usedParameter = functionCallStatement
        .getFunctionCallExpression().getParameterExpressions();

    if (usedParameter.size() != 1) { throw new UnrecognizedCCodeException(
        "pthread_mutex_init function must have 1 parameters in expression",
        functionCallStatement); }


    CFANode predecessor = functionCall.getPredecessor();
    CFANode successor = functionCall.getSuccessor();
    String functionName = successor.getFunctionName();

    CFASequenceBuilder builder = new CFASequenceBuilder(predecessor, cfa);

    ALeftHandSide givenMutexVariable;

    givenMutexVariable = getGivenMutexVariable(functionCallStatement, 0, logger);
    assert givenMutexVariable instanceof CLeftHandSide;

    if (givenMutexVariable == null) {
      throw new UnrecognizedCCodeException("Given parameter must be a valid mutex", functionCallStatement);
    }

    ALeftHandSide flag = getMutexFlag(givenMutexVariable);

    CFAEdge freeMutex = ExpressionUtils.getDummyAssignement(flag, FREE);

    // TODO add assertion. only locking thread can unlock

    builder.addChainLink(freeMutex, successor);

    CFACreationUtils.removeEdgeFromNodes(functionCall);
  }

  @Deprecated
  private static void stubThreadMutexLockNoFunction(CThread threadContext, CStatementEdge functionCall,
      MutableCFA cfa, LogManager logger) throws UnrecognizedCCodeException {

    assert CFAFunctionUtils.isFunctionCallStatement(functionCall);
    CFunctionCall functionCallStatement = (CFunctionCall) functionCall.getStatement();

    List<CExpression> usedParameter = functionCallStatement
        .getFunctionCallExpression().getParameterExpressions();

    if (usedParameter.size() != 1) { throw new UnrecognizedCCodeException(
        "pthread_mutex_init function must have 1 parameters in expression",
        functionCallStatement); }


    CFANode predecessor = functionCall.getPredecessor();
    CFANode successor = functionCall.getSuccessor();
    String functionName = successor.getFunctionName();

    CFASequenceBuilder builder = new CFASequenceBuilder(predecessor, cfa);

    ALeftHandSide givenMutexVariable;

    givenMutexVariable = getGivenMutexVariable(functionCallStatement, 0, logger);
    assert givenMutexVariable instanceof CLeftHandSide;

    if (givenMutexVariable == null) {
      throw new UnrecognizedCCodeException("Given parameter must be a valid mutex", functionCallStatement);
    }

    //TODO add assertion. only not destroyed mutex can be locked
    ALeftHandSide flag = getMutexFlag(givenMutexVariable);

    CExpression mutexFree = BINARY_BUILDER.buildBinaryExpression((CExpression) flag, FREE, BinaryOperator.EQUALS);

    CAssumeEdge edge1 = ExpressionUtils.ASSUME_EDGE_OF.apply(mutexFree);
    CFAEdge edge2 = ExpressionUtils.getDummyAssignement(flag, ExpressionUtils.getThreadNumberNumberExpression(threadContext));

    CFANode infeasableState = new CFATerminationNode(functionName);

    CFASequenceBuilder feasableBuilder = builder.addAssumeEdge(edge1, new CFANode(functionName), infeasableState);
    builder.lockSequenceBuilder();
    feasableBuilder.addChainLink(edge2, successor);

    CFACreationUtils.removeEdgeFromNodes(functionCall);
  }

  @Deprecated
  private static void stubThreadMutexInitNoFunction(AStatementEdge pMutexInit,
      MutableCFA cfa, LogManager logger) throws UnrecognizedCCodeException {


    assert CFAFunctionUtils.isFunctionCallStatement(pMutexInit);
    CFunctionCall functionCallStatement = (CFunctionCall) pMutexInit.getStatement();

    List<CExpression> usedParameter = functionCallStatement
        .getFunctionCallExpression().getParameterExpressions();

    if (usedParameter.size() != 2) { throw new UnrecognizedCCodeException(
        "pthread_mutex_init function must have 2 parameters in expression",
        functionCallStatement); }


    CFANode predecessor = pMutexInit.getPredecessor();
    CFANode successor = pMutexInit.getSuccessor();

    CFASequenceBuilder builder = new CFASequenceBuilder(predecessor, cfa);

    ALeftHandSide givenMutexVariable;
    CExpression threadCreationArgument;

    givenMutexVariable = getGivenMutexVariable(functionCallStatement, 0, logger);
    assert givenMutexVariable instanceof CLeftHandSide;

    if (givenMutexVariable == null) {
      throw new UnrecognizedCCodeException("Given parameter must be a valid mutex", functionCallStatement);
    }

    ALeftHandSide flag = getMutexFlag(givenMutexVariable);
    CFAEdge edge1 = ExpressionUtils.getDummyAssignement(flag, FREE);

    builder.addChainLink(edge1, successor);

    CFACreationUtils.removeEdgeFromNodes(pMutexInit);
  }

  private static CFieldReference getMutexFlag(ALeftHandSide mutexDeclaration) {
    if(mutexDeclaration instanceof AExpression) {
      return new CFieldReference(FileLocation.DUMMY, CNumericTypes.LONG_INT, "__align", (CExpression) mutexDeclaration, false);
    }
    throw new UnsupportedOperationException("The given mutext type is not supported");
  }

  private static ALeftHandSide getGivenMutexVariable(CFunctionCall functionCallStatement, int pos,
      LogManager pLogger) throws UnrecognizedCCodeException {

    List<CExpression> parameter =
        functionCallStatement.getFunctionCallExpression().getParameterExpressions();

    CExpression mutext = parameter.get(pos);

    return checkIfMutex(mutext);
  }

  private static ALeftHandSide checkIfMutex(AExpression pMutext) {
    assert pMutext instanceof CExpression;

    // remove the cast expression to get the plain leftHandSide
    pMutext = unpackClassCast(pMutext);

    // remove the first AMPER to get the plain leftHandSide
    if (pMutext instanceof AUnaryExpression) {
      return getVariableFromPointer((AUnaryExpression) pMutext);
    } else {
      return null;
    }

  }

  public static void setBinaryBuilder(CBinaryExpressionBuilder pCBinaryExpressionBuilder) {
    BINARY_BUILDER =  pCBinaryExpressionBuilder;
  }



}
