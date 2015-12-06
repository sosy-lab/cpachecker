package org.sosy_lab.cpachecker.cpa.callstack;

  /*
   *  CPAchecker is a tool for configurable software verification.
   *  This file is part of CPAchecker.
   *
   *  Copyright (C) 2007-2014  Dirk Beyer
   *  All rights reserved.
   *
   *  Licensed under the Apache License, Version 2.0 (the "License");
   *  you may not use this file except in compliance with the License.
   *  You may obtain a copy of the License at
   *
   *      http://www.apache.org/licenses/LICENSE-2.0
   *
   *  Unless required by applicable law or agreed to in writing, software
   *  distributed under the License is distributed on an "AS IS" BASIS,
   *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   *  See the License for the specific language governing permissions and
   *  limitations under the License.
   *
   *
   *  CPAchecker web page:
   *    http://cpachecker.sosy-lab.org
   */
import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.ContextSwitchEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.ThreadScheduleEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.ControlCodeBuilder;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;



//TODO Copied much code from callstack cpa. Refactor for a generalized implementation
  @Options(prefix="cpa.callstack")
  public class MultiCallstackTransferRelation extends SingleEdgeTransferRelation {

    static final String THREAD_SIMULATION_FUNCTION_NAME = ControlCodeBuilder.THREAD_SIMULATION_FUNCTION_NAME;
    static final String THREAD_DELIGATOR_NAME = "__THREAD_DELIGATOR";

    @Option(secure=true, name="depth",
        description = "depth of recursion bound")
    protected int recursionBoundDepth = 0;

    @Option(secure=true, name="skipRecursion", description = "Skip recursion (this is unsound)." +
        " Treat function call as a statement (the same as for functions without bodies)")
    protected boolean skipRecursion = false;

    /**
     * This flag might be set by external CPAs (e.g. BAM) to indicate
     * a recursive context that might not be recognized by the CallstackCPA.
     * (In case of BAM the operator Reduce splits an indirect recursive call f-g-f
     * into two calls f-g and g-f, which are both non-recursive.)
     * A function-call in a recursive context will be skipped,
     * if the Option 'skipRecursion' is enabled.
     */
    private boolean isRecursiveContext = false;

    @Option(secure=true, description = "Skip recursion if it happens only by going via a function pointer (this is unsound)." +
        " Imprecise function pointer tracking often lead to false recursions.")
    protected boolean skipFunctionPointerRecursion = false;

    @Option(secure=true, description = "Skip recursion if it happens only by going via a void function (this is unsound).")
    protected boolean skipVoidRecursion = false;

    protected final LogManagerWithoutDuplicates logger;

    public MultiCallstackTransferRelation(Configuration config, LogManager pLogger) throws InvalidConfigurationException {
      config.inject(this);
      logger = new LogManagerWithoutDuplicates(pLogger);
    }

    @Override
    public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
        AbstractState pElement, Precision pPrecision, CFAEdge pEdge)
        throws CPATransferException {

      final MultiCallstackState e = (MultiCallstackState) pElement;

      switch (pEdge.getEdgeType()) {
      case StatementEdge:
      case AssumeEdge:
      case BlankEdge:
      case CallToReturnEdge:
      case DeclarationEdge:
      case MultiEdge:
      case ReturnStatementEdge:
        break;

      case ThreadScheduleEdge:
        return handleScheduleEdge((ThreadScheduleEdge) pEdge, e);
      case FunctionCallEdge: 
        return handleFunctionCallEdge((FunctionCallEdge) pEdge, e);
      case FunctionReturnEdge: 
        return handleFunctionRetrunEdge((FunctionReturnEdge) pEdge, e);
      case ContextSwtichEdge: 
        return handleContextSwitchEdge((ContextSwitchEdge) pEdge, e);

      default:
        throw new UnsupportedOperationException("Unrecognized edge type " + pEdge.getEdgeType());
      }

      return Collections.singleton(pElement);
    }
    

    

  private Collection<? extends AbstractState> handleScheduleEdge(
      ThreadScheduleEdge pEdge, MultiCallstackState e) {

    assert e.isContextLess();
    assert e.getThreadName() == null; // The only place where it is null
    ThreadScheduleEdge threadScheduleEdge = pEdge;
    String thread = threadScheduleEdge.getThreadContext().getThreadName();
    MultiCallstackState context = e.setContext(thread);

    context.setContextLess(true);
    return Collections.singleton(context);
  }

  private Collection<? extends AbstractState> handleFunctionCallEdge(
      FunctionCallEdge pEdge, MultiCallstackState e) throws UnsupportedCodeException {
    CFANode callerNode = pEdge.getPredecessor();
    String calledFunctionName = pEdge.getSuccessor().getFunctionName();
    String callerFunctionName = callerNode.getFunctionName();
    assert !THREAD_SIMULATION_FUNCTION_NAME.equals(calledFunctionName);

    if (THREAD_SIMULATION_FUNCTION_NAME.equals(callerFunctionName)) {
      String thread = e.getThreadName();
      MultiCallstackState newTop = new MultiCallstackState(e, thread, calledFunctionName, callerNode);
      newTop.setContextLess(e.isContextLess());
      
      return enterThread(newTop, thread);
    }

    // handle recursion
    if (hasRecursion(e.getCurrentStack(), calledFunctionName)) {
      if (skipRecursiveFunctionCall(e.getCurrentStack(),
          pEdge)) {
        // skip recursion, don't enter function
        logger.logOnce(Level.WARNING, "Skipping recursive function call from",
            callerFunctionName, "to", calledFunctionName);
        return Collections.emptySet();
      } else {
        // recursion is unsupported
        logger.log(Level.INFO, "Recursion detected, aborting. "
                    + "To ignore recursion, add -skipRecursion to the command line.");
        throw new UnsupportedCodeException("recursion", pEdge);
      }
    }

    assert !e.isContextLess();
    // regular function call: add the called function to the current stack

    return Collections.singleton(new MultiCallstackState(e, e.getThreadName(),
        calledFunctionName, callerNode));

  }

  private Collection<? extends AbstractState> handleFunctionRetrunEdge(
      FunctionReturnEdge pEdge, MultiCallstackState e) {
    final String calledFunction = pEdge.getPredecessor().getFunctionName();
    final String callerFunction = pEdge.getSuccessor().getFunctionName();

    CFANode callerNode = e.getCurrentStack().getCallNode();
    assert callerNode.getLeavingSummaryEdge() != null;
    CFANode realReturnNode = callerNode.getLeavingSummaryEdge().getSuccessor();

    assert calledFunction.equals(e.getCurrentStack().getCurrentFunction());
    if (callerFunction.equals(THREAD_SIMULATION_FUNCTION_NAME)) {
      String threadName = e.getThreadName();
      MultiCallstackState stackBottom = e.getPreviousState();
      assert !stackBottom.hasContext(threadName);
      assert pEdge.getPredecessor().getNumLeavingEdges() == 1;
      stackBottom.setContextLess(e.isContextLess());
      
      return returnFromThread(stackBottom, callerFunction);
    }

    if (!realReturnNode.equals(pEdge.getSuccessor())) {
      // this is not the right return edge
      return Collections.emptySet();
    }

    // we are in a function return:
    // remove the current function from the stack;
    // the new abstract state is the predecessor state in the stack
    MultiCallstackState topElement = e.getPreviousState();

    assert callerFunction.equals(topElement.getCurrentStack()
        .getCurrentFunction());

    return Collections.singleton(topElement);
  }

  
  private Collection<? extends AbstractState> handleContextSwitchEdge(
      ContextSwitchEdge contextSwitchEdge, MultiCallstackState e) {

    final String thread = contextSwitchEdge.getContextSwitch().getThread()
        .getThreadName();

    final String calledFunction = contextSwitchEdge.getSuccessor()
        .getFunctionName();
    final String callerFunction = contextSwitchEdge.getPredecessor()
        .getFunctionName();

    assert thread.equals(e.getThreadName());
    
    assert !contextSwitchEdge.isToScheduler()
        || THREAD_SIMULATION_FUNCTION_NAME.equals(calledFunction);
    assert contextSwitchEdge.isToScheduler()
        || THREAD_SIMULATION_FUNCTION_NAME.equals(callerFunction);

    if (contextSwitchEdge.isToScheduler()) {
      assert callerFunction.equals(e.getCurrentStack().getCurrentFunction());
      return returnFromThread(e, callerFunction);
    } else {
      return enterThread(e, thread);
    }
  }
  
  private static Collection<MultiCallstackState> returnFromThread(MultiCallstackState e, String callerFunction) {
    assert !e.isContextLess();
    assert e.getThreadName() != null;
    
    MultiCallstackState returnElement = e.setContext(null);
    returnElement.setContextLess(true);
    
    return Collections
        .singleton(returnElement);
  }
  
  private static Collection<MultiCallstackState> enterThread(MultiCallstackState e, String thread) {
    assert e.isContextLess();
    assert e.getThreadName() != null;
    
    MultiCallstackState returnElement = e.setContext(thread);
    returnElement.setContextLess(false);
    return Collections.singleton(returnElement);
  }

    @Override
    public Collection<? extends AbstractState> strengthen(
        AbstractState pElement, List<AbstractState> pOtherElements,
        CFAEdge pCfaEdge, Precision pPrecision) {

      return null;
    }

    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    protected boolean skipRecursiveFunctionCall(final CallstackState element,
        final FunctionCallEdge callEdge) {
      // Cannot skip if there is no edge for skipping
      // (this would just terminate the path here -> unsound).
      if (leavingEdges(callEdge.getPredecessor()).filter(CFunctionSummaryStatementEdge.class).isEmpty()) {
        return false;
      }

      if (skipRecursion) {
        return true;
      }
      if (skipFunctionPointerRecursion && hasFunctionPointerRecursion(element, callEdge)) {
        return true;
      }
      if (skipVoidRecursion && hasVoidRecursion(element, callEdge)) {
        return true;
      }
      return false;
    }

    /** check, if the current function-call has already appeared in the call-stack. */
    protected boolean hasRecursion(final CallstackState pCurrentState, final String pCalledFunction) {
      if (isRecursiveContext) { // external CPA has seen recursion
        return true;
      }
      // iterate through the current stack and search for an equal name
      CallstackState e = pCurrentState;
      int counter = 0;
      
      while (e != null) {
        if (e.getCurrentFunction().equals(pCalledFunction)) {
          counter++;
          if (counter > recursionBoundDepth) {
            return true;
          }
        }
        e = e.getPreviousState();
      }
      return false;
    }

    protected boolean hasFunctionPointerRecursion(final CallstackState element,
        final FunctionCallEdge pCallEdge) {
      throw new UnsupportedOperationException("not impelemnted yet");
//      if (pCallEdge.getRawStatement().startsWith("pointer call(")) { // Hack, see CFunctionPointerResolver
//        return true;
//      }
//
//      final String functionName = pCallEdge.getSuccessor().getFunctionName();
//      CallstackState e = element;
//      while (e != null) {
//        if (e.getCurrentFunction().equals(functionName)) {
//          // reached the previous stack frame of the same function,
//          // and no function pointer so far
//          return false;
//        }
//
//        FunctionCallEdge callEdge = findOutgoingCallEdge(element.getCallNode());
//        if (callEdge.getRawStatement().startsWith("pointer call(")) {
//          return true;
//        }
//
//        e = e.getPreviousState();
//      }
//      throw new AssertionError();
    }

    protected boolean hasVoidRecursion(final CallstackState element,
        final FunctionCallEdge pCallEdge) {
      if (pCallEdge.getSummaryEdge().getExpression() instanceof AFunctionCallStatement) {
        return true;
      }

      final String functionName = pCallEdge.getSuccessor().getFunctionName();
      CallstackState e = element;
      while (e != null) {
        if (e.getCurrentFunction().equals(functionName)) {
          // reached the previous stack frame of the same function,
          // and no function pointer so far
          return false;
        }

        FunctionSummaryEdge summaryEdge = (FunctionSummaryEdge) element.getCallNode().getLeavingSummaryEdge();
        if (summaryEdge.getExpression() instanceof AFunctionCallStatement) {
          return true;
        }

        e = e.getPreviousState();
      }
      throw new AssertionError();
    }

    protected boolean shouldGoByFunctionSummaryStatement(CallstackState element, CFunctionSummaryStatementEdge sumEdge) {
      String functionName = sumEdge.getFunctionName();
      FunctionCallEdge callEdge = findOutgoingCallEdge(sumEdge.getPredecessor());
      assert functionName.equals(callEdge.getSuccessor().getFunctionName());
      return hasRecursion(element, functionName) && skipRecursiveFunctionCall(element, callEdge);
    }

    protected FunctionCallEdge findOutgoingCallEdge(CFANode predNode) {
      for (CFAEdge edge : leavingEdges(predNode)) {
        if (edge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
          return (FunctionCallEdge)edge;
        }
      }
      throw new AssertionError("Missing function call edge for function call summary edge");
    }

    public void enableRecursiveContext() {
      isRecursiveContext = true;
    }

    public void disableRecursiveContext() {
      isRecursiveContext = false;
    }

}
