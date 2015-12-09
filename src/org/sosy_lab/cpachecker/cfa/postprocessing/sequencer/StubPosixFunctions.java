package org.sosy_lab.cpachecker.cfa.postprocessing.sequencer;

import java.math.BigInteger;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context.CThread;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.CFAEdgeUtils;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.CFAFunctionUtils;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.CFASequenceBuilder;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

public class StubPosixFunctions {

  public static void replaceThreadCreationWithStub(
      CStatementEdge functionCallEdge, CThread thread,
      ControlVariables controlVariables, MutableCFA cfa, LogManager logger) {
    assert CFAFunctionUtils.isFunctionCallStatement(functionCallEdge);

    CFANode predecessor = functionCallEdge.getPredecessor();
    CFANode successor = functionCallEdge.getSuccessor();

    CFASequenceBuilder builder = new CFASequenceBuilder(predecessor, cfa);

    CFunctionCall functionCallStatement = (CFunctionCall) functionCallEdge.getStatement();

    CLeftHandSide givenThreadVariable;
    CExpression threadCreationArgument;

    try {
      givenThreadVariable = getGivenPthreadVariable(functionCallStatement, logger);
      threadCreationArgument = getGivenPthreadCreationArgument(functionCallStatement);
    } catch (UnrecognizedCCodeException e) {
      throw new RuntimeException(e);
    }

    if(givenThreadVariable != null) {
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
    CArraySubscriptExpression leftHandSide = new CArraySubscriptExpression(FileLocation.DUMMY,
        CNumericTypes.BOOL, new CIdExpression(FileLocation.DUMMY,
            controlVariables.getIsThreadFinishedDeclaration()), new CIntegerLiteralExpression(
            FileLocation.DUMMY, CNumericTypes.INT, BigInteger.valueOf(thread.getThreadNumber())));

    CExpressionAssignmentStatement activeAssignement = new CExpressionAssignmentStatement(
        FileLocation.DUMMY, leftHandSide, new CIntegerLiteralExpression(FileLocation.DUMMY,
            CNumericTypes.BOOL, BigInteger.valueOf(0)));

    return new CStatementEdge("", activeAssignement, FileLocation.DUMMY,
        CFASequenceBuilder.DUMMY_NODE, CFASequenceBuilder.DUMMY_NODE);
  }

  private static CFAEdge getActivateThreadStatement(CThread thread,
      ControlVariables controlVariables) {
    CArraySubscriptExpression leftHandSide = new CArraySubscriptExpression(FileLocation.DUMMY,
        CNumericTypes.BOOL, new CIdExpression(FileLocation.DUMMY,
            controlVariables.getIsThreadActiveArrayDeclaration()), new CIntegerLiteralExpression(
            FileLocation.DUMMY, CNumericTypes.INT, BigInteger.valueOf(thread.getThreadNumber())));

    CExpressionAssignmentStatement activeAssignement = new CExpressionAssignmentStatement(
        FileLocation.DUMMY, leftHandSide, new CIntegerLiteralExpression(FileLocation.DUMMY,
            CNumericTypes.BOOL, BigInteger.valueOf(1)));

    return new CStatementEdge("", activeAssignement, FileLocation.DUMMY,
        CFASequenceBuilder.DUMMY_NODE, CFASequenceBuilder.DUMMY_NODE);
  }

  private static CLeftHandSide getGivenPthreadVariable (
      CFunctionCall functionCallStatement, LogManager logger) throws UnrecognizedCCodeException {

    List<CExpression> usedParameter = functionCallStatement
        .getFunctionCallExpression().getParameterExpressions();

    if (usedParameter.size() != 4) {
      throw new UnrecognizedCCodeException(
          "pthread_create function must have 4 parameters in expression",
          functionCallStatement);
    }
    CExpression exp = usedParameter.get(0);
    if(exp instanceof CCastExpression) {
      CCastExpression a = (CCastExpression) exp;
      exp = a.getOperand();
    }
    if (exp instanceof CUnaryExpression
        && ((CUnaryExpression) exp).getOperator().equals(UnaryOperator.AMPER)) {
      CUnaryExpression unaryExpression = (CUnaryExpression) exp;

      CExpression parameterExpression = unaryExpression.getOperand();
      if (parameterExpression instanceof CLeftHandSide) {
        return (CLeftHandSide) parameterExpression;
      }
    } else if(exp instanceof CLiteralExpression) {
      logger.log(Level.WARNING, "Given parameter in pthread_create is an literal. "
          + "Dyamic pointer operations are not supported during the sequencing process!");

      // legal operation but not determined and not supported
      return null;
    }
    throw new UnsupportedOperationException(
        "Cannot handle parameter of pthread_create at "
            + functionCallStatement.getFileLocation());

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

  private static CFAEdge getSavePthreadId(CThread thread, CLeftHandSide threadId) {
    CIntegerLiteralExpression threadNumber = CFAEdgeUtils.getThreadAsNumberExpression(thread);

//    CIdExpression threadNumberSave = new CIdExpression(FileLocation.DUMMY, threadId);
    CStatementEdge threadIdSave = CFAEdgeUtils.getDummyAssignementEdge(threadId, threadNumber);
    return threadIdSave;
  }

  private static CFAEdge getSavePThreadArgument(CThread thread, CExpression threadCreationArgument, ControlVariables controlVariables) {
    CIntegerLiteralExpression threadNumber = CFAEdgeUtils.getThreadAsNumberExpression(thread);
    CType voidPointer = new CPointerType(false, false, CVoidType.VOID);

    CIdExpression threadCreationArgumentArray = new CIdExpression(FileLocation.DUMMY, controlVariables.getThreadCreationArgumentsArrayDeclaration());
    CArraySubscriptExpression left = new CArraySubscriptExpression(FileLocation.DUMMY, voidPointer, threadCreationArgumentArray, threadNumber);

    CStatementEdge variableSaveEdge = CFAEdgeUtils.getDummyAssignementEdge(left, threadCreationArgument);

    return variableSaveEdge;
  }

}
