package org.sosy_lab.cpachecker.cfa.postprocessing.sequencer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.FunctionCallCollector;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
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
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.Pair;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

public class SequencePreparator {

  private static final String THREAD_NAME_PATTERN = "thread";
  public static final String MAIN_THREAD_NAME = THREAD_NAME_PATTERN + '0';

  private final Map<CThread, CStatementEdge> creationEdges = new HashMap<>();
  private final Map<CThread, CStatementEdge> mutexEdges = new HashMap<>();
  private final MutableCFA cfa;


  private final List<CThread> threadsToProcess = new ArrayList<>();
  private final List<CThread> allThreads = new ArrayList<>();
  private final Multiset<FunctionEntryNode> functionUsedCounter = HashMultiset.<FunctionEntryNode> create();

  private int threadCounter = 1;

  public SequencePreparator(MutableCFA cfa) {
    this.cfa = cfa;
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

    assert checkContextSwitchConsistency(allThreads);

    return new CThreadContainer(allThreads);
  }


  private CStatementEdge createNewStatementEdge(FunctionEntryNode newNode, CFunctionCall originalFunctionCallStatement) {
    // TODO review
    CFunctionCallExpression originalFunctionCallExpression = originalFunctionCallStatement.getFunctionCallExpression();
    CFunctionDeclaration originalFunctionDeclaration = originalFunctionCallExpression.getDeclaration();

    CFunctionDeclaration newFunctionDeclaration = new CFunctionDeclaration(FileLocation.DUMMY, originalFunctionDeclaration.getType(),
        newNode.getFunctionName(), originalFunctionDeclaration.getParameters());
    CIdExpression newFunctionNameExpression = new CIdExpression(FileLocation.DUMMY, newFunctionDeclaration);
    CFunctionCallExpression newFunctionCallExpression = new CFunctionCallExpression(FileLocation.DUMMY,
        originalFunctionCallExpression.getExpressionType(), newFunctionNameExpression,
        originalFunctionCallExpression.getParameterExpressions(), newFunctionDeclaration);

    CFunctionCall newFunctionCallStatement;
    if(originalFunctionCallStatement instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement assign = (CFunctionCallAssignmentStatement) originalFunctionCallStatement;
      CLeftHandSide leftHandSide = assign.getLeftHandSide();
      newFunctionCallStatement = new CFunctionCallAssignmentStatement(FileLocation.DUMMY, leftHandSide, newFunctionCallExpression);
    } else {
      assert originalFunctionCallStatement instanceof CFunctionCallStatement;
      newFunctionCallStatement = new CFunctionCallStatement(FileLocation.DUMMY, newFunctionCallExpression);
    }

    CStatementEdge edge = new CStatementEdge("", newFunctionCallStatement, FileLocation.DUMMY, CFASequenceBuilder.DUMMY_NODE,
        CFASequenceBuilder.DUMMY_NODE);
    return edge;
  }

  private void stubThreadJoin(AStatementEdge edge) {
    // nothing to stub
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

  public Map<CThread, CStatementEdge> getMutexEdges() {
     return mutexEdges;
  }

  /**
   * Assertion Only. Every context switch point is unique because every thread has it's own function
   * @param threads
   * @return
   */
  public static boolean checkContextSwitchConsistency(List<CThread> threads) {
    Map<ContextSwitch, CThread> css = new HashMap<ContextSwitch, CThread>();
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


        FunctionEntryNode uniqueFunctionEntry = getUniqueFunctionCopyForThread(regularThreadEntry);
        creatorThread.setThreadFunction(uniqueFunctionEntry);
      }

      while (!handleFunctionCallsInFunction.isEmpty()) {
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

      switch (regularFunctionName) {
        case PThreadUtils.PTHREAD_CREATE_NAME:
          CThread thread = createCThread(threadCounter, creatorThread, functionCallStatement);

          creationEdges.put(thread, (CStatementEdge) edge);
          threadsToProcess.add(thread);
          allThreads.add(thread);
          threadCounter++;
          break;
        case PThreadUtils.PTHREAD_JOIN_NAME:
          stubThreadJoin(edge);
          break;
        case PThreadUtils.PTHREAD_MUTEX_INIT_NAME:


          break;
        case PThreadUtils.PTHREAD_MUTEX_DESTROY_NAME:
        case PThreadUtils.PTHREAD_MUTEX_LOCK_NAME:
        case PThreadUtils.PTHREAD_MUTEX_UNLOCK_NAME:
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
              createNewStatementEdge(node, (CFunctionCall) edge.getStatement()));
          break;
      }
    }


    private FunctionEntryNode getUniqueFunctionCopyForThread(FunctionEntryNode regularFunction) {

      // If already clone for this thread
      if (usedRegularFunctions.containsKey(regularFunction)) {
        return usedRegularFunctions.get(regularFunction);
      }
      // If not cloned for this thread
      else {
        FunctionEntryNode uniqueFunctionEntry;
        // clone function if used already
        if (functionUsedCounter.count(regularFunction) != 0) {
          String newFunctionName = regularFunction.getFunctionName() + "__"
              + functionUsedCounter.count(regularFunction);

          uniqueFunctionEntry = cloneFunctionIntoCFA(regularFunction, newFunctionName);
        } else {
          // take original function if never used.
          assert functionUsedCounter.count(regularFunction) == 0;
          uniqueFunctionEntry = regularFunction;
        }

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

      return clonedStart;
    }



  }
}
