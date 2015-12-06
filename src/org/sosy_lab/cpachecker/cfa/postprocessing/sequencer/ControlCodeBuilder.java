package org.sosy_lab.cpachecker.cfa.postprocessing.sequencer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.ContextSwitchEdge;
import org.sosy_lab.cpachecker.cfa.model.ContextSwitchSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.SummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.ThreadScheduleEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context.AThread;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context.CThread;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context.CThreadContainer;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context.ContextSwitch;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.CFAEdgeUtils;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.CFAFunctionUtils;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.CFASequenceBuilder;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.util.CFAUtils;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class ControlCodeBuilder {
    
  private LogManager logger;

  private MutableCFA cfa;

  private CThreadContainer threads;
  
  public static final String THREAD_SIMULATION_FUNCTION_NAME = "__schedulerSimulation";

  private final CBinaryExpressionBuilder BINARY_BUILDER;

  private ControlVariables controlVariables;
  
  private final int THREAD_COUNT;
  
  /**
   * The function entry node of the scheduler simulation function
   */
  private FunctionEntryNode schedulerSimulationFunctionEntry;

  /**
   * 
   * @param cfa
   * @param threads - A container which wraps informations about the threads used in the original c code
   * @param logger
   */
  public ControlCodeBuilder(ControlVariables controlVariables, MutableCFA cfa, CThreadContainer threads, LogManager logger) {
    this.controlVariables = controlVariables;
    this.BINARY_BUILDER = new CBinaryExpressionBuilder(cfa.getMachineModel(), logger);
    this.THREAD_COUNT = threads.getThreadCount();
    this.cfa = cfa;
    this.threads = threads;
    this.logger = logger;
  }

  public void buildControlVariableDeclaration() {

    FunctionEntryNode mainFunctionEntry = cfa.getMainFunction();

    assert cfa.getMainFunction().getNumLeavingEdges() == 1;
    CFAEdge initGlobalVarsEdge = mainFunctionEntry.getLeavingEdge(0);

    assert initGlobalVarsEdge.getEdgeType().equals(CFAEdgeType.BlankEdge);
    assert CFAFunctionUtils.INIT_GLOBAL_VARS.equals(initGlobalVarsEdge
        .getDescription());

    CFANode startSequence = new CFANode("main");
    CFANode injectionPoint = initGlobalVarsEdge.getSuccessor();

    CFASequenceBuilder globalVarBuilder = new CFASequenceBuilder(startSequence,
        cfa);

    globalVarBuilder.addChainLink(controlVariables
        .getDummyCurrentThreadDeclarationEdge());
    globalVarBuilder.addChainLink(controlVariables
        .getDummyThreadCreationArgumentsArrayDeclarationEdge());
    globalVarBuilder.addChainLink(controlVariables
        .getDummyThreadReturnValueArrayDeclarationEdge());
    globalVarBuilder.addChainLink(controlVariables
        .getDummyIsThreadActiveArrayDeclarationEdge());
    globalVarBuilder.addChainLink(controlVariables
        .getDummyIsThreadFinishedDeclarationEdge());

    CFANode endSequence = globalVarBuilder.lockSequenceBuilder();

    CFAEdgeUtils.injectInBetween(injectionPoint, startSequence, endSequence,
        cfa);
  }
  
  /**
   * Builds the scheduler simulation function including the context switch edges in the whole cfa.
   * @return the function entry node of the scheduler simulation function 
   */
  public FunctionEntryNode buildScheduleSimulationFunction() {
    
    schedulerSimulationFunctionEntry = buildSchedulerSimulationEntry(cfa);
    
    injectOutGoingContextSwitchEdges();
    
    CFANode exitBeforeReturn = new CFANode(THREAD_SIMULATION_FUNCTION_NAME);
    
    CFASequenceBuilder sequenceBuilder = new CFASequenceBuilder(schedulerSimulationFunctionEntry, cfa);
    
//    for(CThread thread : threads.getAllThreads()) {
//      CExpression expression = BINARY_BUILDER.buildBinaryExpressionUnchecked(new CIdExpression(FileLocation.d, pDeclaration), op2, op)
//      CAssumeEdge a = new CAssumeEdge("", FileLocation.DUMMY, CFASequenceBuilder.DUMMY_NODE, CFASequenceBuilder.DUMMY_NODE, , true)
//      sequenceBuilder.addAssumeEdge();
//    }
    
    for(CThread thread : threads.getAllThreads()) {
      CFASequenceBuilder threadBuilder = appendThreadSchedule(sequenceBuilder, thread);

      CFASequenceBuilder isThreadActivePath = createThreadUseableAssume(threadBuilder, thread); //TODO remove finished assume!!

      if(threads.getMainThread().equals(thread)) {
        createThreadAttendant(isThreadActivePath, thread);
      } else {
        CFASequenceBuilder threadStartPath = isThreadActivePath.addMultipleEdge(getDummyThreadStartStatement(thread));
        
        CFAEdge edge = getDummyAssingFinishedStatement(thread);
        threadStartPath.addChainLink(edge, schedulerSimulationFunctionEntry);
        
        FunctionExitNode contextSwitchExit = new FunctionExitNode(THREAD_SIMULATION_FUNCTION_NAME);
        contextSwitchExit.setEntryNode(schedulerSimulationFunctionEntry);
        CFASequenceBuilder threadContextSwitchPath = isThreadActivePath.addMultipleEdge(new BlankEdge("", FileLocation.DUMMY, CFASequenceBuilder.DUMMY_NODE, CFASequenceBuilder.DUMMY_NODE, "context switch"), contextSwitchExit);

        createThreadAttendant(threadContextSwitchPath, thread);
      }
      isThreadActivePath.lockSequenceBuilder();
    }

    
//    CFASequenceBuilder scheduleEnd = new CFASequenceBuilder(exitBeforeReturn, cfa);
//    
//    //TODO replace this with the last assume edge to make a more detailed check
////    for(CThread thread : threads.getAllThreads()) {
////      AssumeEdge isThreadNotFinished = new CAssumeEdge("", FileLocation.DUMMY, CFASequenceBuilder.DUMMY_NODE, CFASequenceBuilder.DUMMY_NODE, createThreadNotFinishedAssumeExpression(thread), true);
////      CFASequenceBuilder threadIsNotFinished = scheduleEnd.addAssumeEdge(isThreadNotFinished, schedulerSimulationFunctionEntry);
////      threadIsNotFinished.lockSequenceBuilder();
////    }
//    
//    scheduleEnd = scheduleEnd.addAssumeEdge(getAnyThreadNotFinishedAssume(), new CFANode(THREAD_SIMULATION_FUNCTION_NAME), schedulerSimulationFunctionEntry);
//    scheduleEnd.addChainLink(new BlankEdge("", FileLocation.DUMMY, CFASequenceBuilder.DUMMY_NODE, CFASequenceBuilder.DUMMY_NODE, "default return"), schedulerSimulationFunctionEntry.getExitNode());
//    scheduleEnd.lockSequenceBuilder();
    
    return schedulerSimulationFunctionEntry;
  }
  
  private static CFunctionEntryNode buildSchedulerSimulationEntry(MutableCFA cfa) {
    FunctionExitNode schedulerSimulationExit = new FunctionExitNode(THREAD_SIMULATION_FUNCTION_NAME);
    CFunctionType schedulerFunctionType = new CFunctionType(false, false, CVoidType.VOID, Lists.<CType>newArrayList(), false);
    CFunctionDeclaration schedulerFunctionDeclaration = new CFunctionDeclaration(FileLocation.DUMMY, schedulerFunctionType,
        THREAD_SIMULATION_FUNCTION_NAME, Lists.<CParameterDeclaration>newArrayList());
    CFunctionEntryNode schedulerSimulationFunctionEntry = new CFunctionEntryNode(FileLocation.DUMMY, schedulerFunctionDeclaration,
        schedulerSimulationExit, Lists.<String>newArrayList(), Optional.<CVariableDeclaration> absent());
    schedulerSimulationExit.setEntryNode(schedulerSimulationFunctionEntry);

    cfa.addFunction(schedulerSimulationFunctionEntry);

    return schedulerSimulationFunctionEntry;
  }

  /**
   * Appends edges to the builder which determines the current thread the thread
   * is running
   * 
   * @param builder
   *          the builder where the threads will be appended
   * @param threads
   *          the threads which will be accessible with by thread delegation
   *          edges
   * @return a map of threads mapped to a builder which can be used to append
   *         {@link #CFAEdge}s to the current thread context
   */
  private CFASequenceBuilder appendThreadSchedule(CFASequenceBuilder builder, CThread thread) {
    
    CFAEdge dummyThreadEdge = new ThreadScheduleEdge(CFASequenceBuilder.DUMMY_NODE, CFASequenceBuilder.DUMMY_NODE, thread);
    return builder.addMultipleEdge(dummyThreadEdge);
  }

  private AssumeEdge getAnyThreadNotFinishedAssume() {
    CExpression threadIsNotFinished = getConstantSubscriptExpression(0, controlVariables.getIsThreadFinishedDeclaration(), CNumericTypes.BOOL);
    
    threadIsNotFinished = BINARY_BUILDER.buildBinaryExpressionUnchecked(threadIsNotFinished, CIntegerLiteralExpression.ONE, BinaryOperator.EQUALS);    
    for(int i = 1; i <THREAD_COUNT; i++) {
      threadIsNotFinished = BINARY_BUILDER.buildBinaryExpressionUnchecked(threadIsNotFinished, getConstantSubscriptExpression(i, controlVariables.getIsThreadFinishedDeclaration(), CNumericTypes.BOOL), BinaryOperator.BINARY_AND);
      threadIsNotFinished = BINARY_BUILDER.buildBinaryExpressionUnchecked(threadIsNotFinished, CIntegerLiteralExpression.ONE, BinaryOperator.EQUALS);
    }
    return new CAssumeEdge(threadIsNotFinished.toString(), FileLocation.DUMMY, CFASequenceBuilder.DUMMY_NODE, CFASequenceBuilder.DUMMY_NODE, threadIsNotFinished, true);
  }
  
  
  private AssumeEdge getIsAnyThreadActive() {
    CExpression exp1 = getConstantSubscriptExpression(0, controlVariables.getIsThreadActiveArrayDeclaration(), CNumericTypes.BOOL);
    for(int i = 1; i <THREAD_COUNT; i++) {
      exp1 = BINARY_BUILDER.buildBinaryExpressionUnchecked(exp1, getConstantSubscriptExpression(i, controlVariables.getIsThreadActiveArrayDeclaration(), CNumericTypes.BOOL), BinaryOperator.BINARY_OR);
    }
    return new CAssumeEdge(exp1.toString(), FileLocation.DUMMY, CFASequenceBuilder.DUMMY_NODE, CFASequenceBuilder.DUMMY_NODE, exp1, true);
  }
  
  private CExpression getConstantSubscriptExpression(int i, CVariableDeclaration dec, CType arrayType) {
    CArraySubscriptExpression subscript = new CArraySubscriptExpression(FileLocation.DUMMY, arrayType,
        new CIdExpression(FileLocation.DUMMY, dec), new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.INT, BigInteger.valueOf(i)));
    
    return subscript;
  }

  private CFASequenceBuilder createThreadUseableAssume(CFASequenceBuilder schedulerBuilder, CThread thread) {
    CFANode infeasableLoc = schedulerSimulationFunctionEntry.getExitNode();
    int threadNumber = thread.getThreadNumber();
    CExpression isThreadActiveExpression = createThreadActiveAssumeExpression(threadNumber);

    // Add thread finished expression. This helps the value analysis to prune
    // away infeasible context switch and don't stuck at a infinite program
    // counter iteration
    CExpression threadNotFinishedExpression = createThreadNotFinishedAssumeExpression(thread);

    AssumeEdge assumeEdge = new CAssumeEdge("", FileLocation.DUMMY, CFASequenceBuilder.DUMMY_NODE, CFASequenceBuilder.DUMMY_NODE, isThreadActiveExpression, true);
    AssumeEdge threadNotFinishedAssume = new CAssumeEdge("", FileLocation.DUMMY, CFASequenceBuilder.DUMMY_NODE, CFASequenceBuilder.DUMMY_NODE, threadNotFinishedExpression, true);
    
    CFANode newCFANode = new CFANode(schedulerBuilder.getFunctionName());
    CFANode threadNotFinishedNode = new CFANode(schedulerBuilder.getFunctionName());
    
    CFASequenceBuilder isNotFinishedBranch = schedulerBuilder.addAssumeEdge(threadNotFinishedAssume, threadNotFinishedNode, infeasableLoc);
    CFASequenceBuilder isActiveBranch = isNotFinishedBranch.addAssumeEdge(assumeEdge, newCFANode, infeasableLoc);
    
    isNotFinishedBranch.lockSequenceBuilder();
    
    return isActiveBranch;
  }

  private CExpression createThreadActiveAssumeExpression(int threadNumber) {
      // e.g. for threadNumber=1  ->  isThreadActive[1]
      CArraySubscriptExpression isThreadWithNumberActiveExpression = new CArraySubscriptExpression(FileLocation.DUMMY, CNumericTypes.BOOL,
          new CIdExpression(FileLocation.DUMMY, controlVariables.getIsThreadActiveArrayDeclaration()), new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.INT, BigInteger.valueOf(threadNumber)));
      CExpression isActiveExpression = BINARY_BUILDER.buildBinaryExpressionUnchecked(isThreadWithNumberActiveExpression, CIntegerLiteralExpression.ONE, BinaryOperator.EQUALS);
      
      return isActiveExpression;
    }
  
  private CExpression createThreadNotFinishedAssumeExpression(AThread thread) {
    CExpression isThreadWithNumber = getConstantSubscriptExpression(
        thread.getThreadNumber(), controlVariables.getIsThreadFinishedDeclaration(), CNumericTypes.BOOL);
    
    CExpression notFinished = BINARY_BUILDER.buildBinaryExpressionUnchecked(
        isThreadWithNumber, new CIntegerLiteralExpression(FileLocation.DUMMY,
            CNumericTypes.BOOL, BigInteger.valueOf(0)), BinaryOperator.EQUALS);

    return notFinished;
  }

 /**  
  * This edge displays the return to the function head at next
  * context-switch. Besides no return edge is needed because all "function
  * calls" will behave like one single function call. Actually the function
  * will never be called by an functionCallEdge but an context-switch edge
  */
  private void createThreadAttendant(
      CFASequenceBuilder nextProgramLocation, CThread targetThread) {

    for (ContextSwitch contextSwitch : targetThread.getContextSwitchPoints()) {
      assert contextSwitch.getContextSwitchReturnNode() != null;

      CFAEdge contextSwitchEdge = new ContextSwitchEdge(contextSwitch, "",
          FileLocation.DUMMY, CFASequenceBuilder.DUMMY_NODE,
          CFASequenceBuilder.DUMMY_NODE, false);

      nextProgramLocation.addMultipleEdge(contextSwitchEdge,
          contextSwitch.getContextSwitchReturnNode());
    }

  }
  
  /**
   * A special function call statement
   */
  private CStatementEdge getDummyThreadStartStatement(CThread targetThread) {
    List<CParameterDeclaration> param = targetThread.getThreadFunction().getFunctionDefinition().getParameters();
    List<CExpression> parameter;
    
    // the thread start function can have either one or none parameters
    if(param.size() == 0) {
      parameter = ImmutableList.of();
    } else {
      assert param.size() == 1;
      CArraySubscriptExpression threadCreationArgument = new CArraySubscriptExpression(FileLocation.DUMMY, new CPointerType(false, false, CVoidType.VOID), new CIdExpression(FileLocation.DUMMY, controlVariables.getThreadCreationArgumentsArrayDeclaration()), CFAEdgeUtils.getThreadAsNumberExpression(targetThread));
      parameter = ImmutableList.<CExpression>of(threadCreationArgument);
    }
    return threadCreationStatementEdge(targetThread, parameter);
  }

  
  private CFAEdge getDummyAssingFinishedStatement(CThread thread) {
    return getThreadsBooleanStateStatementEdge(thread, controlVariables.getIsThreadFinishedDeclaration(), true);
  }
  
  private CFAEdge getThreadsBooleanStateStatementEdge(CThread thread, CVariableDeclaration variableDeclaraion, boolean value) {
    CIdExpression isThreadFinishedArray = new CIdExpression(FileLocation.DUMMY,
        variableDeclaraion);
    CIntegerLiteralExpression currentThreadNumber = CFAEdgeUtils.getThreadAsNumberExpression(thread);
    CArraySubscriptExpression threadFinishedIdentificator = new CArraySubscriptExpression(
        FileLocation.DUMMY, CNumericTypes.BOOL, isThreadFinishedArray,
        currentThreadNumber);

    CExpressionAssignmentStatement assignement = new CExpressionAssignmentStatement(
        FileLocation.DUMMY, threadFinishedIdentificator,
        new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.BOOL,
            BigInteger.valueOf(1)));

    return new CStatementEdge(assignement.toString(), assignement,
        FileLocation.DUMMY, CFASequenceBuilder.DUMMY_NODE,
        CFASequenceBuilder.DUMMY_NODE);
  }
  

  
  CFAEdge getThreadToCurrentThreadStatement(CThread currentThread) {
    int threadNumber = currentThread.getThreadNumber();
    CIdExpression currentThreadNumber = new CIdExpression(FileLocation.DUMMY, controlVariables.getCurrentThreadDeclaration());

    CExpressionAssignmentStatement assignement = new CExpressionAssignmentStatement(FileLocation.DUMMY, currentThreadNumber, CFAEdgeUtils.getThreadAsNumberExpression(currentThread));
    return new CStatementEdge("currentThread = " + threadNumber, assignement, FileLocation.DUMMY, CFASequenceBuilder.DUMMY_NODE, CFASequenceBuilder.DUMMY_NODE);
  }
  
  
  public void injectOutGoingContextSwitchEdges() {
    assert SequencePreparator.checkContextSwitchConsistency(threads.getAllThreads());
    
    for (CThread thread : threads.getAllThreads()) {
      for (ContextSwitch contextSwitch : thread.getContextSwitchPoints()) {
        CFANode contextSwitchPosition = contextSwitch.getContextSwitchReturnNode();
        
        CFANode newNode = new CFANode(contextSwitchPosition.getFunctionName());
        cfa.addNode(newNode);
        for(CFAEdge edge: contextSwitch.getContextStatementCause()) {
          CFAEdgeUtils.bypassCEdgeNodes(edge, edge.getPredecessor(), newNode);
        }
        
        SummaryEdge summaryEdge = new ContextSwitchSummaryEdge("", FileLocation.DUMMY, newNode, contextSwitchPosition, contextSwitch);
        summaryEdge.getPredecessor().addLeavingSummaryEdge(summaryEdge);
        summaryEdge.getSuccessor().addEnteringSummaryEdge(summaryEdge);
        
        ContextSwitchEdge contextSwitchEdge = new ContextSwitchEdge(contextSwitch, "", FileLocation.DUMMY, newNode,
            schedulerSimulationFunctionEntry, true);
        CFACreationUtils.addEdgeUnconditionallyToCFA(contextSwitchEdge);
        
        

        assert summaryEdge.getSuccessor() != null;
        assert summaryEdge.getPredecessor() != null;
        assert CFAUtils.leavingEdges(summaryEdge.getPredecessor()).size() == 1;
        assert CFAUtils.leavingEdges(summaryEdge.getPredecessor()).get(0) instanceof ContextSwitchEdge;

        // there is no context switch edge at this moment
//        assert CFAUtils.enteringEdges(summaryEdge.getSuccessor()).size() == 0; //TODO dont forget to uncomment!!!
      }
    }
    
    assert isContextSwitchConsistency();

  }  

  private boolean isContextSwitchConsistency() {
    for(CThread thread : threads.getAllThreads()) {
      for (ContextSwitch contextSwitch : thread.getContextSwitchPoints()) {
        assert CFAEdgeUtils.isEdgeForbiddenEdge(contextSwitch
            .getContextStatementCause()) : "The edge "
            + contextSwitch.getContextStatementCause()
            + " was found in contextswitch which was replaced by the cfa building tools!";
        return true;
      }
    }
    return true;
  }
  
  public AssumeEdge createIsCurrentThreadAssume(CThread thread) {
    CExpression currentThreadValue = new CIdExpression(FileLocation.DUMMY, controlVariables.getCurrentThreadDeclaration());
    CExpression IthreadNumber = CFAEdgeUtils.getThreadAsNumberExpression(thread);
    CExpression isThisCurrentThreadAssume = null;
    isThisCurrentThreadAssume = BINARY_BUILDER.buildBinaryExpressionUnchecked(currentThreadValue, IthreadNumber, BinaryOperator.EQUALS);

    return new CAssumeEdge("", FileLocation.DUMMY, CFASequenceBuilder.DUMMY_NODE, CFASequenceBuilder.DUMMY_NODE, isThisCurrentThreadAssume, true);
  }

  
  private CFAEdge getThreadActiveExpression(CThread thread) { 
    return getThreadsBooleanStateStatementEdge(thread, controlVariables.getIsThreadActiveArrayDeclaration(), true);
  }

  public void buildPthreadCreateBody(CFunctionDeclaration pthreadStubDeclaration) {
    FunctionEntryNode entryNode = getFunctionFromDeclaration(pthreadStubDeclaration, Optional.<CVariableDeclaration>absent());
    cfa.addFunction(entryNode);
    CFASequenceBuilder builder = new CFASequenceBuilder(entryNode, cfa);
    CParameterDeclaration firstParameter = pthreadStubDeclaration.getParameters().get(0);
    CParameterDeclaration secondParameter = pthreadStubDeclaration.getParameters().get(1);
    CParameterDeclaration thirdParameter = pthreadStubDeclaration.getParameters().get(2);

    CType pthreadType = new CTypedefType(false, false, "pthread_t", CNumericTypes.UNSIGNED_LONG_INT);
    CPointerExpression a = new CPointerExpression(FileLocation.DUMMY, pthreadType, new CIdExpression(FileLocation.DUMMY, firstParameter));
    
    CStatement threadAssignmentExpression = new CExpressionAssignmentStatement(FileLocation.DUMMY, a, new CIdExpression(FileLocation.DUMMY, thirdParameter));
    CStatementEdge threadCountStatement = new CStatementEdge("", threadAssignmentExpression, FileLocation.DUMMY,
        CFASequenceBuilder.DUMMY_NODE, CFASequenceBuilder.DUMMY_NODE);

    CIdExpression threadArgSave = new CIdExpression(FileLocation.DUMMY, controlVariables.getThreadCreationArgumentsArrayDeclaration());
    
    CPointerType b = new CPointerType(false, false, CVoidType.VOID);
    CArraySubscriptExpression sub = new CArraySubscriptExpression(FileLocation.DUMMY, b, threadArgSave, new CIdExpression(FileLocation.DUMMY, thirdParameter));
    CStatement variableSaveExpression = new CExpressionAssignmentStatement(FileLocation.DUMMY, sub, new CIdExpression(
        FileLocation.DUMMY, secondParameter));
    CStatementEdge variable = new CStatementEdge("", variableSaveExpression, FileLocation.DUMMY, CFASequenceBuilder.DUMMY_NODE,
        CFASequenceBuilder.DUMMY_NODE);
    BlankEdge defaultReturn = new BlankEdge("", FileLocation.DUMMY, CFASequenceBuilder.DUMMY_NODE, CFASequenceBuilder.DUMMY_NODE, CFAFunctionUtils.DEFAULT_RETURN);
    builder.addChainLink(threadCountStatement);
    builder.addChainLink(variable);
    builder.addChainLink(defaultReturn, entryNode.getExitNode());
  }
  
  private static FunctionEntryNode getFunctionFromDeclaration(CFunctionDeclaration dec, Optional<CVariableDeclaration> returnValue) {
    List<String> parameterName = new ArrayList<String>();
    for(CParameterDeclaration pa : dec.getParameters()) {
      parameterName.add(pa.getName());
    }
    FunctionExitNode exitNode = new FunctionExitNode(dec.getName());
    FunctionEntryNode entryNode = new CFunctionEntryNode(FileLocation.DUMMY, dec, exitNode, parameterName, returnValue);
    exitNode.setEntryNode(entryNode);
    
    return entryNode;
  }

  public void buildPThreadJoinBody(CFunctionDeclaration joinDeclaration) {
    CVariableDeclaration retVal = new CVariableDeclaration(FileLocation.DUMMY, false,
        CStorageClass.AUTO, CNumericTypes.INT, "__temp_retval_", "__temp_retval_",
        "__temp_retval_", new CInitializerExpression(FileLocation.DUMMY, null));
    
    FunctionEntryNode functionEntry = getFunctionFromDeclaration(joinDeclaration, Optional.<CVariableDeclaration>of(retVal));
    cfa.addFunction(functionEntry);
    CFASequenceBuilder builder = new CFASequenceBuilder(functionEntry, cfa);

    final String joinFunctionName = joinDeclaration.getName();

    CIdExpression currentThread = new CIdExpression(FileLocation.DUMMY, joinDeclaration
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

  public CStatementEdge threadCreationStatementEdge(CThread thread, List<CExpression> param) {
    assert thread.getThreadCreationStatement().isPresent();
    
    CFunctionEntryNode threadEntryNode = thread.getThreadFunction();
    CFunctionDeclaration threadDeclaration = threadEntryNode.getFunctionDefinition();
  
    assert threadDeclaration.getParameters().size() == param.size();
    // thread start functions must have at least one parameter which must be a generic pointer
//    Preconditions.checkElementIndex(0, threadDeclaration.getParameters().size());
    
    CFunctionCallExpression functionCallExpression = new CFunctionCallExpression(FileLocation.DUMMY, threadDeclaration.getType(),
        new CIdExpression(FileLocation.DUMMY, threadDeclaration), param, threadDeclaration);

    
    CIdExpression a = new CIdExpression(FileLocation.DUMMY, controlVariables.getThreadReturnValueArrayDeclaration());
    CArraySubscriptExpression b = new CArraySubscriptExpression(FileLocation.DUMMY, CNumericTypes.INT, a, CFAEdgeUtils.getThreadAsNumberExpression(thread));
    
    
    CFunctionCallAssignmentStatement threadStartExpression = new CFunctionCallAssignmentStatement(FileLocation.DUMMY, b, functionCallExpression);
    
//    CFunctionCallStatement statement = new CFunctionCallStatement(FileLocation.DUMMY, functionCallExpression);
    return new CStatementEdge("", threadStartExpression, FileLocation.DUMMY, CFASequenceBuilder.DUMMY_NODE, CFASequenceBuilder.DUMMY_NODE);
  }

  public CExpression getCurrentThreadExpression() {
    return new CIdExpression(FileLocation.DUMMY, controlVariables.getCurrentThreadDeclaration());
  }


  
}
