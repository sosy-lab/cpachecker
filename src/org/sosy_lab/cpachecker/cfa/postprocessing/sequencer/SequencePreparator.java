package org.sosy_lab.cpachecker.cfa.postprocessing.sequencer;

import java.util.ArrayList;
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
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context.CThread;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context.CThreadContainer;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context.ContextSwitch;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.CFAEdgeUtils;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.CFAFunctionUtils;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.CFASequenceBuilder;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.PThreadUtils;
import org.sosy_lab.cpachecker.util.CFATraversal;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

public class SequencePreparator {
  
  private static final String THREAD_NAME_PATTERN = "thread";
  public static final String MAIN_THREAD_NAME = THREAD_NAME_PATTERN + '0';
  
  private final Map<CThread, CStatementEdge> creationEdges = new HashMap<CThread, CStatementEdge>();
  private final MutableCFA cfa;
  
  
  private final List<CThread> threadsToProcess = new ArrayList<CThread>();
  private final List<CThread> allThreads = new ArrayList<CThread>();
  private final Multiset<String> functionUsedCounter = HashMultiset.<String> create();
  
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
      
      processThread(creatorThread);
    }
    
    assert checkContextSwitchConsistency(allThreads);
    
    return new CThreadContainer(allThreads);
  }
  
  
  
  private void processThread(CThread creatorThread) {
    Map<FunctionEntryNode, FunctionEntryNode> alreadyClonedFunctionsForThread = new HashMap<FunctionEntryNode, FunctionEntryNode>();
    List<FunctionEntryNode> functionPatternsToProcress = Lists.<FunctionEntryNode> newArrayList();
    functionPatternsToProcress.add(creatorThread.getThreadFunction());

    String originalThreadStartFunction = creatorThread.getThreadFunction().getFunctionName();

    FunctionEntryNode functionEntryNode = getNewFunction(originalThreadStartFunction, creatorThread);
    FunctionEntryNode patternFunctionEntry = creatorThread.getThreadFunction();
    
    creatorThread.setThreadFunction(functionEntryNode);
    alreadyClonedFunctionsForThread.put(patternFunctionEntry, functionEntryNode);
    functionUsedCounter.add(originalThreadStartFunction);

    /*
     * continue the function format for every function called from the thread
     * start function. The function format consists of
     * 
     * -- function cloning for every thread and
     * 
     * -- the stubbing of POSIX functions
     */
    CFATraversal traversalStrategy = CFATraversal.dfs().ignoreFunctionCalls().ignoreSummaryEdges();
    while (!functionPatternsToProcress.isEmpty()) {
            
      // This are the already clone functions. These functions will be
      // traversed for function calls so that the called functions can be
      // traversed too. 
      patternFunctionEntry = functionPatternsToProcress.remove(0);

      final FunctionCallCollector functionCallCollector = new FunctionCallCollector();
      
      traversalStrategy.traverseOnce(patternFunctionEntry, functionCallCollector);
      
      for (AStatementEdge edge : functionCallCollector.getFunctionCalls()) {
        processFunctionCallStatement(edge, creatorThread, alreadyClonedFunctionsForThread, functionPatternsToProcress);
        
      }
    }
  }
  
  private CFunctionEntryNode getNewFunction(String originalThreadStartFunction, CThread creatingThread) {
    // the start node for the thread which is valid now
    
    CFunctionEntryNode functionEntryNode;

    // clone function if used already
    if(functionUsedCounter.count(originalThreadStartFunction) != 0) {
      String newFunctionName = originalThreadStartFunction + "__" + functionUsedCounter.count(originalThreadStartFunction);
      functionEntryNode = CFAFunctionUtils.cloneCFunction(creatingThread.getThreadFunction(),
          newFunctionName, cfa);
    } 
    
    // take original function if never used.
    else {
      assert functionUsedCounter.count(originalThreadStartFunction) == 0;
      FunctionEntryNode patternFunctionEntry = creatingThread.getThreadFunction();
      functionEntryNode = (CFunctionEntryNode) patternFunctionEntry;
    }
    
    return functionEntryNode;
  }

  private void processFunctionCallStatement(AStatementEdge edge,
      CThread creatingThread,
      Map<FunctionEntryNode, FunctionEntryNode> alreadyClonedFunctionsForThread, List<FunctionEntryNode> functionPatternsToProcress) {
    
    assert edge.getStatement() instanceof AFunctionCall;
    AFunctionCall statement = (AFunctionCall) edge.getStatement();
    
    assert CFAFunctionUtils.isFunctionCallStatement(edge);
    CFunctionCall functionCallStatement = (CFunctionCall) edge.getStatement();

    if(!CFAFunctionUtils.isFunctionDeclared(functionCallStatement)) {
      return;
    }
    
    String originalFunctionName = CFAFunctionUtils.getFunctionName(edge);
    CFunctionEntryNode startOfCalledFunction = (CFunctionEntryNode) cfa.getFunctionHead(originalFunctionName);

    switch (CFAFunctionUtils.getFunctionName(edge)) {
    case PThreadUtils.PTHREAD_CREATE_NAME:
      CThread thread = createCThread(threadCounter, creatingThread, functionCallStatement);

      creationEdges.put(thread, (CStatementEdge) edge);
      threadsToProcess.add(thread);
      allThreads.add(thread);
      threadCounter++;
      break;
    case PThreadUtils.PTHREAD_JOIN_NAME:
      stubThreadJoin(edge);
      break;
    case PThreadUtils.PTHREAD_MUTEX_INIT_NAME:
    case PThreadUtils.PTHREAD_MUTEX_DESTROY_NAME:
    case PThreadUtils.PTHREAD_MUTEX_LOCK_NAME:
    case PThreadUtils.PTHREAD_MUTEX_UNLOCK_NAME:
      // Will be stubbed by cpa
      break;
    default:
      
      if(startOfCalledFunction == null) {
        // there is a function call to a function without a body. This
        // appears if external functions were called
        break;
      }
      
      // If function has a body, then clone it (if not already done).
      FunctionEntryNode node = null;
      if (!alreadyClonedFunctionsForThread.containsKey(startOfCalledFunction)) {
        String functionName;
        FunctionEntryNode clone;
        
        // clone function if used already
        if (functionUsedCounter.count(originalFunctionName) != 0) {
          functionName = originalFunctionName + "__"
              + functionUsedCounter.count(originalFunctionName);
          clone = cloneFunction(startOfCalledFunction, functionName,
              (CStatementEdge) edge);
        } else {
          // take original function if never used.
          assert functionUsedCounter.count(originalFunctionName) == 0;
          functionName = originalFunctionName;
          clone = startOfCalledFunction;
        }
        
        functionUsedCounter.add(originalFunctionName);
        alreadyClonedFunctionsForThread.put(startOfCalledFunction, clone);
        functionPatternsToProcress.add(clone);
      }
      
      // if clone function was created in if block above, then the node
      // must be the same as in the map.
      node = alreadyClonedFunctionsForThread.get(startOfCalledFunction);

      CFAEdgeUtils.replaceCEdgeWith(edge, createNewStatementEdge(node, (CFunctionCall) edge.getStatement()));
      break;
    }
    
    
  }
  
 

  private CStatementEdge createNewStatementEdge(FunctionEntryNode newNode, CFunctionCall originalFunctionCallStatement) {
    
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
  
  private FunctionEntryNode cloneFunction(CFunctionEntryNode originalTargetFunctionEntry, String newFunctionName, CStatementEdge edge) {
    assert CFAFunctionUtils.isFunctionCallStatement(edge);
    assert originalTargetFunctionEntry != null;
    FunctionEntryNode node = null;

    node = CFAFunctionUtils.cloneCFunction(originalTargetFunctionEntry, newFunctionName, cfa);

    CFAEdgeUtils.replaceCEdgeWith(edge, createNewStatementEdge(node, (CFunctionCall) edge.getStatement()));
    return node;
  }
  
  public Map<CThread, CStatementEdge> getCreationEdges() {
    return creationEdges;
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
}
