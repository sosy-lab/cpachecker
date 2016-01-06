package org.sosy_lab.cpachecker.cfa.postprocessing.sequencer;

import static org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.ExpressionUtils.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.FunctionCallCollector;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.FunctionCloner;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context.CThread;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context.CThreadContainer;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context.ContextSwitch;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.CFAEdgeUtils;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.CFAFunctionUtils;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.CFASequenceBuilder;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.PThreadUtils;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.Pair;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

public class SequencePreparator {

  private static final String THREAD_NAME_PATTERN = "thread";
  public static final String MAIN_THREAD_NAME = THREAD_NAME_PATTERN + '0';

  private final Map<CThread, CStatementEdge> creationEdges = new HashMap<>();
  private final Set<AStatementEdge> mutexEdges = new HashSet<>();
  private final Map<CThread, CStatementEdge> mutexLocks = new HashMap<>();
  private final Map<CThread, CStatementEdge> mutexUnlocks = new HashMap<>();
  private final MutableCFA cfa;


  private final List<CThread> threadsToProcess = new ArrayList<>();
  private final List<CThread> allThreads = new ArrayList<>();
  private final Multiset<FunctionEntryNode> functionUsedCounter = HashMultiset.<FunctionEntryNode> create();

  private final Set<String> originalFunctionNames = new HashSet<>();

  private int threadCounter = 1;

  private final StubDeclaration stubDeclaration;

  public SequencePreparator(StubDeclaration stubDeclaration, MutableCFA cfa) {
    this.cfa = cfa;
    originalFunctionNames.addAll(cfa.getAllFunctionNames());
    this.stubDeclaration = stubDeclaration;
  }

  /**
   * <p>
   * Discovers thread creation statements. Clones the used functions for every
   * thread so that every thread uses it's own functions. Stubs POSIX functions
   * so that the thread control mechanisms can be simulated in the sequenced
   * cfa.
   * </p>
   *
   * <p>
   * Returns a container which contains the threads discovered in the cfa. Note
   * that only threads which doesn't arise from a repeatedly creation call such
   * as a recursive creation or a creation call in a loop will be discovered.
   * </p>
   *
   * @return a container of threads which were discovered in the cfa
   */
  public CThreadContainer traverseAndReplaceFunctions() {

    CThread cMainThread = new CThread((CFunctionEntryNode) cfa.getMainFunction(), MAIN_THREAD_NAME, 0, null, null);
    threadsToProcess.add(cMainThread);
    allThreads.add(cMainThread);


    while (!threadsToProcess.isEmpty()) {
      CThread creatorThread = threadsToProcess.remove(0);

      ThreadPreparator threadProcessing = new ThreadPreparator(creatorThread);
      threadProcessing.processThread();
    }


    // delete main function?!!
    for(String functionName : originalFunctionNames) {
      cfa.removeFunction(functionName);
    }

    cfa.setMainFunction(cMainThread.getThreadFunction());

    assert checkContextSwitchConsistency(allThreads);

    return new CThreadContainer(allThreads);
  }



  private CStatementEdge createNewStatementForNewFunction(FunctionEntryNode node,
      CFunctionCall origianlFunctionCall) {
    String newFunctionName = node.getFunctionName();

    CFunctionCallExpression origExp = origianlFunctionCall.getFunctionCallExpression();
    CFunctionDeclaration origDec = origExp.getDeclaration();

    CFunctionDeclaration newDeclaration =
        copyFunctionDeclarationWithNewName(origDec, newFunctionName);
    CFunctionCallExpression newExpression = changeDeclaration(origExp, newDeclaration);

    CStatement callStatement = getFunctionCallWithDifferent(origianlFunctionCall, newExpression);
    return new CStatementEdge(callStatement.toString(), callStatement, FileLocation.DUMMY,
        CFASequenceBuilder.DUMMY_NODE, CFASequenceBuilder.DUMMY_NODE);
  }

  private CFunctionDeclaration copyFunctionDeclarationWithNewName(
      CFunctionDeclaration functionDeclaration, String newName) {

    CFunctionType type = functionDeclaration.getType();
    List<CParameterDeclaration> parameters = functionDeclaration.getParameters();
    return new CFunctionDeclaration(FileLocation.DUMMY, type, newName, parameters);
  }

  private CFunctionCallExpression changeDeclaration(CFunctionCallExpression origCallExp,
      CFunctionDeclaration newDeclaration) {

    CIdExpression newNameExp = CID_EXPRESSION_OF.apply(newDeclaration);
    CFunctionCallExpression newFunctionCallExpression =
        new CFunctionCallExpression(FileLocation.DUMMY,
            origCallExp.getExpressionType(), newNameExp,
            origCallExp.getParameterExpressions(), newDeclaration);

    return newFunctionCallExpression;
  }

  private CFunctionCallExpression changeParameter(CFunctionCallExpression origExp, List<CExpression> params) {
    List<CExpression> parameter = new ArrayList<>();
    for(CExpression param : params) {
      parameter.add(param);
    }
    return new CFunctionCallExpression(FileLocation.DUMMY, origExp.getExpressionType(), origExp.getFunctionNameExpression(), parameter, origExp.getDeclaration());
  }

  private CFunctionCall getFunctionCallWithDifferent(CFunctionCall functionCall, CFunctionCallExpression newFunctionCallExpression) {
    CFunctionCall newFunctionCallStatement;
    if (functionCall instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement assign = (CFunctionCallAssignmentStatement) functionCall;
      CLeftHandSide leftHandSide = assign.getLeftHandSide();
      newFunctionCallStatement =
          new CFunctionCallAssignmentStatement(FileLocation.DUMMY, leftHandSide,
              newFunctionCallExpression);
    } else {
      assert functionCall instanceof CFunctionCallStatement;
      newFunctionCallStatement =
          new CFunctionCallStatement(FileLocation.DUMMY, newFunctionCallExpression);
    }

    return newFunctionCallStatement;
  }

  private CThread createCThread(int threadCounter, CThread creatorThread, CFunctionCall threadCreationStatement) {
    String threadStartFunctionName = PThreadUtils.getCThreadStartFunctionName(threadCreationStatement);
    CFunctionEntryNode threadStartNode = (CFunctionEntryNode) cfa.getFunctionHead(threadStartFunctionName);
    String threadName = THREAD_NAME_PATTERN + threadCounter;
    assert threadStartNode != null;

    CThread thread = new CThread(threadStartNode, threadName, threadCounter, threadCreationStatement, creatorThread);
    return thread;
  }

  public Map<CThread, CStatementEdge> getCreationEdges() {
    return creationEdges;
  }

  public Set<AStatementEdge> getMutexInitEdges() {
     return mutexEdges;
  }

  /**
   * Assertion Only. Every context switch point is unique because every thread has it's own function
   * @param threads
   * @return
   */
  public static boolean checkContextSwitchConsistency(List<CThread> threads) {
    Map<ContextSwitch, CThread> css = new HashMap<>();
    for(CThread thread : threads) {
      for(ContextSwitch cs : thread.getContextSwitchPoints()) {
        assert !css.containsKey(cs) : "The conextswitch point " + cs + " of "
            + thread + " was already used by " + css.get(cs) + " before ";
        if(css.containsValue(cs)) {
          return false;
        }
        css.put(cs, thread);
      }
    }
    return true;
  }



  public class ThreadPreparator {

    private CThread creatorThread;

    private List<FunctionEntryNode> handleFunctionCallsInFunction;
    private Map<FunctionEntryNode, FunctionEntryNode> usedRegularFunctions;

    public ThreadPreparator(CThread pCreatorThread) {
      this.creatorThread = pCreatorThread;
    }

    void processThread() {
      {
        usedRegularFunctions = new HashMap<>();
        handleFunctionCallsInFunction = Lists.<FunctionEntryNode> newArrayList();
        FunctionEntryNode regularThreadEntry = creatorThread.getThreadFunction();

        // side effect! adds function to usedFunctions
        FunctionEntryNode uniqueFunctionEntry = getUniqueFunctionCopyForThread(regularThreadEntry);
        creatorThread.setThreadFunction(uniqueFunctionEntry);
      }

      while (!handleFunctionCallsInFunction.isEmpty()) {//TODO add function to thread as used
        FunctionEntryNode regularFunctionEntry = handleFunctionCallsInFunction.remove(0);
        FunctionEntryNode clonedFunctionEntry = usedRegularFunctions.get(regularFunctionEntry);
        assert clonedFunctionEntry != null;

        for (AStatementEdge edge : getFunctionCallsOfFunction(clonedFunctionEntry)) {
          processFunctionCallStatement(edge);
        }
      }
    }

    private Collection<AStatementEdge> getFunctionCallsOfFunction(
        FunctionEntryNode regularFunctionEntry) {
      CFATraversal traversalStrategy =
          CFATraversal.dfs().ignoreFunctionCalls().ignoreSummaryEdges();
      final FunctionCallCollector functionCallCollector = new FunctionCallCollector();

      traversalStrategy.traverseOnce(regularFunctionEntry, functionCallCollector);
      return functionCallCollector.getFunctionCalls();
    }

    private void processFunctionCallStatement(AStatementEdge edge) {
      assert edge.getStatement() instanceof AFunctionCall;
      assert CFAFunctionUtils.isFunctionCallStatement(edge);

      CFunctionCall functionCallStatement = (CFunctionCall) edge.getStatement();

      if (!CFAFunctionUtils.isFunctionDeclared(functionCallStatement)) { return; }
      String regularFunctionName = CFAFunctionUtils.getFunctionName(edge);
      cfa.getFunctionHead(regularFunctionName);


      switch (regularFunctionName) {
        case PThreadUtils.PTHREAD_CREATE_NAME:
          handlePthreadCreate((CStatementEdge) edge, functionCallStatement);
          break;
        case PThreadUtils.PTHREAD_JOIN_NAME:
//          handlePthreadJoin((CStatementEdge) edge, functionCallStatement);
          // nothing to stub
          break;
        case PThreadUtils.PTHREAD_MUTEX_INIT_NAME:
          mutexEdges.add(edge);

          break;
        case PThreadUtils.PTHREAD_MUTEX_LOCK_NAME:
          mutexLocks.put(creatorThread, (CStatementEdge) edge);
          break;
        case PThreadUtils.PTHREAD_MUTEX_UNLOCK_NAME:
          mutexUnlocks.put(creatorThread, (CStatementEdge) edge);
          break;
        case PThreadUtils.PTHREAD_MUTEX_DESTROY_NAME:

          // Will be stubbed by cpa
          break;
        default:

          CFunctionEntryNode regularFunctionEntry =
          (CFunctionEntryNode) cfa.getFunctionHead(regularFunctionName);
          if (regularFunctionEntry == null) {
            // there is a function call to a function without a body. This
            // appears if external functions were called
            return;
          }
          FunctionEntryNode node = getUniqueFunctionCopyForThread(regularFunctionEntry);
          CFAEdgeUtils.replaceCEdgeWith(edge,
              createNewStatementForNewFunction(node, (CFunctionCall) edge.getStatement()));
          break;
      }
    }

    private void handlePthreadJoin(CStatementEdge edge, CFunctionCall functionCallStatement) {
      List<CExpression> parameterExp = getPThreadJoinParameterExp(functionCallStatement);
      CFAEdgeUtils.replaceCEdgeWith(edge, changeToStubParameterCall(functionCallStatement, parameterExp));
    }

    private void handlePthreadCreate(CStatementEdge edge, CFunctionCall functionCallStatement) {
      CThread thread = createCThread(threadCounter, creatorThread, functionCallStatement);

      List<CExpression> parameterExp = getPThreadCreationParameterExp(functionCallStatement, thread);
      CFAEdgeUtils.replaceCEdgeWith(edge, changeToStubParameterCall(functionCallStatement, parameterExp));
      creationEdges.put(thread, edge);
      threadsToProcess.add(thread);
      allThreads.add(thread);
      threadCounter++;
    }

    private List<CExpression> getPThreadJoinParameterExp(CFunctionCall pFunctionCallStatement) {

      return null;
    }

    private List<CExpression> getPThreadCreationParameterExp(CFunctionCall pFunctionCallStatement,
        CThread thread) {
      List<CExpression> origExp = pFunctionCallStatement.getFunctionCallExpression().getParameterExpressions();
      List<CExpression> newExp = new ArrayList<>();
      newExp.add(origExp.get(0)); // pthread_t
      newExp.add(origExp.get(3)); // arg
      newExp.add(getThreadNumberNumberExpression(thread));

      return newExp;
    }

    /**
     * The pthread_create function call expression will be increased by a
     * parameter which holds the thread number.
     * @return a new CStatementEdge with changed parameter expression
     */
    private CStatementEdge changeToStubParameterCall(CFunctionCall functionCall, List<CExpression> paramExp) {
      CFunctionCallExpression origExp = functionCall.getFunctionCallExpression();
      CFunctionDeclaration pthreadCreateStubDeclaration = stubDeclaration.getPthreadCreateStubDeclaration();
      List<CParameterDeclaration> stubParamDec =  pthreadCreateStubDeclaration.getParameters();

      assert checkParameterDeclarationConsistency(stubParamDec, paramExp);

      CFunctionCallExpression newFunctionCallExp =
          changeParameter(origExp, paramExp);
      newFunctionCallExp = changeDeclaration(newFunctionCallExp, pthreadCreateStubDeclaration);

      CFunctionCall newFunctionCall = getFunctionCallWithDifferent(functionCall, newFunctionCallExp);

      return new CStatementEdge("", newFunctionCall, FileLocation.DUMMY,
          CFASequenceBuilder.DUMMY_NODE, CFASequenceBuilder.DUMMY_NODE);
    }

    private boolean checkParameterDeclarationConsistency(List<CParameterDeclaration> decl, List<CExpression> expression) {
      if(decl.size() != expression.size()) {
        return false;
      }
      // TODO check parameter type
      return true;
    }


    private FunctionEntryNode getUniqueFunctionCopyForThread(FunctionEntryNode regularFunction) {
      assert originalFunctionNames.contains(regularFunction.getFunctionName());

      // If already clone for this thread
      if (usedRegularFunctions.containsKey(regularFunction)) {
        return usedRegularFunctions.get(regularFunction);
      }
      // If not cloned for this thread
      else {
        String newFunctionName = regularFunction.getFunctionName() + "__"
            + functionUsedCounter.count(regularFunction);

        FunctionEntryNode uniqueFunctionEntry = cloneFunctionIntoCFA(regularFunction, newFunctionName);

        /* removed origial function reuse. Using original function and changing
         * the functionCallStatements will cause an error if the original is
         * cloned with its replaced functionCallStatements*/

//        // clone function if used already
//        if (functionUsedCounter.count(regularFunction) != 0) {
//          String newFunctionName = regularFunction.getFunctionName() + "__"
//              + functionUsedCounter.count(regularFunction);
//
//          uniqueFunctionEntry = cloneFunctionIntoCFA(regularFunction, newFunctionName);
//        } else {
//          // take original function if never used.
//          assert functionUsedCounter.count(regularFunction) == 0;
//          uniqueFunctionEntry = regularFunction;
//        }

        functionUsedCounter.add(regularFunction);
        usedRegularFunctions.put(regularFunction, uniqueFunctionEntry);
        handleFunctionCallsInFunction.add(regularFunction);

        return uniqueFunctionEntry;
      }
    }

    private FunctionEntryNode cloneFunctionIntoCFA(FunctionEntryNode startOfCalledFunction, String functionName) {
      Pair<FunctionEntryNode, Collection<CFANode>> clonedFunction = FunctionCloner.cloneCFA(startOfCalledFunction, functionName);
      FunctionEntryNode clonedStart = clonedFunction.getFirstNotNull();

      cfa.addFunction(clonedStart);
      for(CFANode nodeOfFunction : clonedFunction.getSecondNotNull()) {
        cfa.addNode(nodeOfFunction);
      }

      // function exit nodes may be not reachable by function start node and
      // therefore won't be added to the cfa
      FunctionExitNode functionExit = clonedStart.getExitNode();
      if(functionExit.getNumEnteringEdges() > 0) {
        cfa.addNode(functionExit);
      }

      return clonedStart;
    }



  }

  public Map<CThread, CStatementEdge> getMutexLockEdges() {
    return mutexLocks;
  }

  public Map<CThread, CStatementEdge> getMutexUnlockEdges() {
    return mutexUnlocks;
  }
}
