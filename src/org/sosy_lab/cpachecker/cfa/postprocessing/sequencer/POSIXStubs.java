package org.sosy_lab.cpachecker.cfa.postprocessing.sequencer;

import static org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.ExpressionUtils.*;

import java.math.BigInteger;
import java.util.List;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.CFAFunctionUtils;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.CFASequenceBuilder;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;

import com.google.common.base.Optional;

public class POSIXStubs {

  public final static CTypedefType THREAD_TYPE = new CTypedefType(false, false, "pthread_t", CNumericTypes.UNSIGNED_LONG_INT);
  public final static CType MUTEX_TYPE = null; //TODO

  public static final AExpression FREE = new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.INT, BigInteger.valueOf(-1));
  public static final AExpression DESTROY = new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.INT, BigInteger.valueOf(-2));

  private final ControlVariables controlVariables;
  private final StubDeclaration stubDeclaration;
  private final CBinaryExpressionBuilder BINARY_BUILDER;
  private final LogManager logger;
  private final MutableCFA cfa;

  public POSIXStubs(ControlVariables controlVariables,StubDeclaration stubDeclaration, MutableCFA cfa, LogManager logger) {
    this.controlVariables = controlVariables;
    this.stubDeclaration = stubDeclaration;
    this.BINARY_BUILDER = new CBinaryExpressionBuilder(cfa.getMachineModel(), logger);
    this.logger = logger;
    this.cfa = cfa;
  }

  public void buildPthreadCreateBody() {
    CFunctionDeclaration pthreadCreateDeclaration = stubDeclaration.getPthreadCreateStubDeclaration();
    CVariableDeclaration threadCreationArgumentStorage = controlVariables.getThreadCreationArgumentsArrayDeclaration();
    CVariableDeclaration threadActiveStorage = controlVariables.getIsThreadActiveArrayDeclaration();
    CVariableDeclaration threadFinishedStorage = controlVariables.getIsThreadFinishedDeclaration();

    List<CParameterDeclaration> parameters = pthreadCreateDeclaration.getParameters();
    assert parameters.size() == 3;
    CParameterDeclaration firstParameter = parameters.get(0);
    CParameterDeclaration secondParameter = parameters.get(1);
    CParameterDeclaration thirdParameter = parameters.get(2);

    // create edges
    CPointerExpression threadID = POINTER_OF.apply(firstParameter);

    CIdExpression threadNumber = CID_EXPRESSION_OF.apply(thirdParameter);
    CArraySubscriptExpression creationArgument =
        getArrayVarOfIndex(threadCreationArgumentStorage, threadNumber);
    CArraySubscriptExpression activeState = getArrayVarOfIndex(threadActiveStorage, threadNumber);
    CArraySubscriptExpression finishedState = getArrayVarOfIndex(threadFinishedStorage, threadNumber);

    AStatementEdge threadIDSaveStatement = getDummyAssignement(threadID, threadNumber);
    AStatementEdge saveCreationArgument =
        getDummyAssignement(creationArgument, CID_EXPRESSION_OF.apply(secondParameter));
    AStatementEdge activateThread = getDummyCStaticAssignement(activeState, true);
    AStatementEdge setUnfinished = getDummyCStaticAssignement(finishedState, false);
    BlankEdge defaultReturn =
        new BlankEdge("", FileLocation.DUMMY, CFASequenceBuilder.DUMMY_NODE,
            CFASequenceBuilder.DUMMY_NODE, CFAFunctionUtils.DEFAULT_RETURN);

    // create function
    FunctionEntryNode entryNode = CFAFunctionUtils.createFunctionFromDeclaration(
        pthreadCreateDeclaration, Optional.<CVariableDeclaration> absent());

    cfa.addFunction(entryNode);

    CFASequenceBuilder builder = new CFASequenceBuilder(entryNode, cfa);
    // activate edge is missing!!
    builder.addChainLink(threadIDSaveStatement);
    builder.addChainLink(saveCreationArgument);
    builder.addChainLink(activateThread);
    //debug only
//    builder.addChainLink(setUnfinished);  // normally not necessary, because set in initializer
    builder.addChainLink(defaultReturn, entryNode.getExitNode());
  }

  public void buildPThreadJoinBody() {
    // TODO review
    // TODO return value is buggy
    CVariableDeclaration retVal = new CVariableDeclaration(FileLocation.DUMMY, false,
        CStorageClass.AUTO, CNumericTypes.INT, "__temp_retval_", "__temp_retval_",
        "__temp_retval_", new CInitializerExpression(FileLocation.DUMMY, null));

    FunctionEntryNode functionEntry = CFAFunctionUtils.createFunctionFromDeclaration(stubDeclaration.getPthreadJoinDeclaration(), Optional.<CVariableDeclaration>of(retVal));
    cfa.addFunction(functionEntry);
    CFASequenceBuilder builder = new CFASequenceBuilder(functionEntry, cfa);

    final String joinFunctionName = stubDeclaration.getPthreadJoinDeclaration().getName();

    CIdExpression currentThread = new CIdExpression(FileLocation.DUMMY, stubDeclaration.getPthreadJoinDeclaration()
        .getParameters().get(0));
    CExpression isThreadNotFinished = new CArraySubscriptExpression(FileLocation.DUMMY,
        CNumericTypes.BOOL, new CIdExpression(FileLocation.DUMMY, controlVariables.getIsThreadFinishedDeclaration()),
        currentThread);

    isThreadNotFinished = BINARY_BUILDER.buildBinaryExpressionUnchecked(isThreadNotFinished, new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.BOOL, BigInteger.valueOf(0)), BinaryOperator.EQUALS);

    AssumeEdge assumeEdge = new CAssumeEdge("", FileLocation.DUMMY, CFASequenceBuilder.DUMMY_NODE,
        CFASequenceBuilder.DUMMY_NODE, isThreadNotFinished, true);

    CExpression arrayExpression = new CIdExpression(FileLocation.DUMMY,
        controlVariables.getThreadReturnValueArrayDeclaration());
    CArraySubscriptExpression returnValue = new CArraySubscriptExpression(FileLocation.DUMMY,
        CNumericTypes.INT, arrayExpression, new CIdExpression(FileLocation.DUMMY,
            controlVariables.getCurrentThreadDeclaration()));
    CReturnStatement a = new CReturnStatement(FileLocation.DUMMY,
        Optional.<CExpression> of(returnValue), Optional.<CAssignment> absent());
    CReturnStatementEdge returnEdge = new CReturnStatementEdge("", a, FileLocation.DUMMY,
        CFASequenceBuilder.DUMMY_NODE, new FunctionExitNode("DUMMY"));

    CFASequenceBuilder functionReadyToJoin = builder.addAssumeEdge(assumeEdge, new CFATerminationNode(joinFunctionName), new CFANode(
        joinFunctionName));
    functionReadyToJoin.lockSequenceBuilder();
    builder.addChainLink(returnEdge, functionEntry.getExitNode());
  }


}
