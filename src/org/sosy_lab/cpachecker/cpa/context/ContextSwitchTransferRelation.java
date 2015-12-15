package org.sosy_lab.cpachecker.cpa.context;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.ContextSwitchEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.ThreadScheduleEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.ControlCodeBuilder;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context.ContextSwitch;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.CFAFunctionUtils;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.PThreadUtils;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

@Options(prefix="cpa.contextswitch")
public class ContextSwitchTransferRelation extends SingleEdgeTransferRelation {

  @Option(description = "Bounds the number of context switches which will be performed for every thread")
  protected int contextSwitchBound = 50;

  private CFA cfa;

  static final String THREAD_SIMULATION_FUNCTION_NAME = ControlCodeBuilder.THREAD_SIMULATION_FUNCTION_NAME;
  private LogManager logger;

  protected ContextSwitchTransferRelation(Configuration config, LogManager logger, CFA cfa) throws InvalidConfigurationException {
    config.inject(this);
    this.logger = logger;
    this.cfa = cfa;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(AbstractState state,
      Precision precision, CFAEdge cfaEdge) throws CPATransferException, InterruptedException {
    ThreadState threadState = (ThreadState) state;
//    assert !threadState.getCurrentThread().isFinished();
//    assert threadState.getCurrentThread().isActive();

    switch (cfaEdge.getEdgeType()) {
    case AssumeEdge:
    case BlankEdge:
    case CallToReturnEdge:
    case DeclarationEdge:
    case MultiEdge:
    case ReturnStatementEdge:
    case ContextSwitchSummaryEdge:
      break;
    case ThreadScheduleEdge:
      return handleThreadScheduleEdge((ThreadScheduleEdge) cfaEdge, threadState);
    case FunctionCallEdge:
      return handleFunctionCallEdge((FunctionCallEdge) cfaEdge, threadState);
    case FunctionReturnEdge:
      return handleFunctionRetrunEdge((FunctionReturnEdge) cfaEdge, threadState);
    case StatementEdge:
      handleMutex((AStatementEdge) cfaEdge, threadState);
      handlePThreadCreate((AStatementEdge) cfaEdge, threadState);
      break;
    case ContextSwtichEdge:
      assert cfaEdge instanceof ContextSwitchEdge;
      return handleContextSwitch((ContextSwitchEdge) cfaEdge, (ThreadState) state);

    default:
      throw new UnsupportedOperationException("Unrecognized edge type " + cfaEdge.getEdgeType());
    }

    return Collections.singleton(state);
  }

  private void handlePThreadCreate(AStatementEdge cfaEdge,
      ThreadState threadState) {
    if (!(cfaEdge.getStatement() instanceof AFunctionCall)) {
      return;
    }

    if(CFAFunctionUtils.isStatementEdgeName(PThreadUtils.PTHREAD_CREATE_NAME).apply(cfaEdge)) {
      // TODO ka
    }
  }

  private Collection<? extends AbstractState> handleFunctionCallEdge(
      FunctionCallEdge cfaEdge, ThreadState threadState) {
    assert !threadState.getCurrentThread().isFinished();

    String callerFunction = cfaEdge.getPredecessor().getFunctionName();
    if(!THREAD_SIMULATION_FUNCTION_NAME.equals(callerFunction)) {
      return Collections.singleton(threadState);
    }

    return contextSwitchGuard(0, threadState);
  }

  private Collection<? extends AbstractState> handleThreadScheduleEdge(ThreadScheduleEdge threadScheduleEdge,
      ThreadState e) {

    // TODO boost if thread doesn't change

    String nextCurrentThreadName = threadScheduleEdge.getThreadContext().getThreadName();
    Thread nextCurrentThread = e.getThread(nextCurrentThreadName);
    if(nextCurrentThread.isFinished()) {
      return Collections.emptySet();
    }

    assert e.getThreads().contains(nextCurrentThread);
    return Collections.singleton(new ThreadState(e.getThreads(), nextCurrentThread));
  }

  private Collection<? extends AbstractState> handleFunctionRetrunEdge(FunctionReturnEdge functionReturnEdge,
      ThreadState state) {
    assert !state.getCurrentThread().isFinished();

    String callerFunction = functionReturnEdge.getSuccessor().getFunctionName();
    if(!THREAD_SIMULATION_FUNCTION_NAME.equals(callerFunction)) {
      return Collections.singleton(state);
    }

    // thread is finished because of thread return
    Thread currentThread = state.getCurrentThread();

    // thread is finished. This means the program counter reached maximum. In
    // assume conditions where context switches can be skipped the program
    // counter is not right. Handle function call edge like the context switch
    // edge with the maximum context switch.
    Thread newNextThread = setThreadProgramcounter(currentThread, currentThread.getMaxProgramCounter());
    Thread finishedThread = finishThread(newNextThread);
    ThreadState newState = changeThreadInState(state, finishedThread);

    return Collections.singleton(newState);
  }

  private void handleMutex(AStatementEdge cfaEdge, ThreadState state) {
      if (cfaEdge.getStatement() instanceof AFunctionCall) {
        AFunctionCall functionCallStatement = (AFunctionCall) cfaEdge.getStatement();

        if(functionCallStatement.getFunctionCallExpression().getDeclaration() == null) {
          return;
        }
        String functionName = functionCallStatement.getFunctionCallExpression().getDeclaration().getName();
        //TODO implement!!
        if(PThreadUtils.PTHREAD_MUTEX_INIT_NAME.equals(functionName)) {
          logger.log(Level.WARNING, "Mutex not supported yet!");
        }

      }
  }

  public Collection<? extends AbstractState> handleContextSwitch(
      ContextSwitchEdge contextSwitchEdge, ThreadState e) {
    assert !e.getCurrentThread().isFinished();

    final ContextSwitch contextSwitch = contextSwitchEdge.getContextSwitch();
    final String thread = contextSwitch.getThread().getThreadName();
    final int programCounter = contextSwitch.getContextSwitchNumber();

    String callerFunction = contextSwitchEdge.getSuccessor().getFunctionName();
    String calledFunction = contextSwitchEdge.getPredecessor().getFunctionName();
    assert !contextSwitchEdge.isToScheduler() || THREAD_SIMULATION_FUNCTION_NAME.equals(callerFunction);
    assert contextSwitchEdge.isToScheduler() || THREAD_SIMULATION_FUNCTION_NAME.equals(calledFunction);


    Thread nextThread = e.getCurrentThread();
    assert nextThread.getThreadName().equals(thread);

    if (contextSwitchEdge.isToScheduler()) {
      return updateThreadState(e, contextSwitch.getContextSwitchNumber());
    } else {
      return contextSwitchGuard(programCounter, e);
    }
  }

  private Collection<? extends AbstractState> updateThreadState(ThreadState currentThreadState, int pc) {
    Thread newNextThread = setThreadProgramcounter(currentThreadState.getCurrentThread(), pc);
    ThreadState newState = changeThreadInState(currentThreadState, newNextThread);
    return Collections.singleton(newState);
  }

  private Collection<? extends AbstractState> contextSwitchGuard(int programCounter, ThreadState e) {
    if (e.getCurrentThread().getLastProgramCounter() != programCounter) {
      return Collections.emptySet();
    } else {
      return Collections.singleton(e);
    }
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState state, List<AbstractState> otherStates, CFAEdge cfaEdge,
      Precision precision) throws CPATransferException, InterruptedException {
    return null;
  }


  public static Thread setThreadProgramcounter(Thread thread, int pc) {
    assert !thread.isFinished();
    assert thread.getLastProgramCounter() <= thread.getMaxProgramCounter();
    if(thread.getLastProgramCounter() == thread.getMaxProgramCounter()) {
      // TODO max program counter is the thread return point. thread will be finished. Made manually in function return handling
//      return new Thread(thread.getThreadName(), thread.isActive(), true, pc, thread.getMaxProgramCounter());
    }
    return new Thread(thread.getThreadName(), thread.isActive(), thread.isFinished(), pc, thread.getMaxProgramCounter());
  }

//  public static Thread incrementThreadProgramcounter(Thread thread) {
//    assert !thread.isFinished();
//    if(thread.getLastProgramCounter() == thread.getMaxProgramCounter()) {
//      return thread; //TODO ?!?!
//    }
//    return new Thread(thread.getThreadName(), thread.isActive(), thread.isFinished(), thread.getLastProgramCounter() + 1, thread.getMaxProgramCounter());
//  }

  private static Thread setThreadActive(Thread thread, boolean flag) {
    assert flag ^ thread.isActive();
    assert !thread.isFinished(); //TODO evaluate. maybe throw exception because c program fill fail either

    return new Thread(thread.getThreadName(), flag, thread.isFinished(), thread.getLastProgramCounter(), thread.getMaxProgramCounter());
  }

  private static Thread finishThread(Thread thread) {
    assert !thread.isFinished(); //TODO evaluate. maybe throw exception because c program fill fail either
//    assert thread.getLastProgramCounter() == thread.getMaxProgramCounter();

    // thread can be finished at every program counter position
    return new Thread(thread.getThreadName(), thread.isActive(), true, thread.getLastProgramCounter(), thread.getMaxProgramCounter());
  }

  /**
   * @return a new instance of the thread state with changed states.
   */
  // TODO very inefficient. Implemented while feasibility analysis
  private static ThreadState changeThreadInState(ThreadState threadState, Thread changedThread) {
    final int threadCount = threadState.getThreads().size();

    Set<Thread> threads = new HashSet<Thread>();
    for(Thread containingThread : threadState.getThreads()) {
      if(containingThread.getThreadName().equals(changedThread.getThreadName())) {
        threads.add(changedThread);
      } else {
        threads.add(containingThread);
      }
    }
    assert threads.size() == threadCount;

    return new ThreadState(threads, changedThread);
  }

}
