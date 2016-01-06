package org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;

import com.google.common.base.Optional;
import com.google.common.collect.SetMultimap;

public abstract class AThread {

  private FunctionEntryNode threadFunction;
  private int threadNumber;
  private String threadName;
  private Map<CFANode, ContextSwitch> contextSwitchPoints = new HashMap<CFANode, ContextSwitch>();
  protected SetMultimap<String, ? extends AStatementEdge> usedFunctions;
  private Optional<? extends AFunctionCall> threadCreationStatement;
  private Optional<? extends AThread> creator;

  public AThread(FunctionEntryNode threadFunction, String threadName,
      int threadNumber, @Nullable AFunctionCall threadCreationStatement,
      SetMultimap<String, ? extends AStatementEdge> usedFunctions, @Nullable AThread creator) {
    assert threadNumber >= 0;

    this.threadFunction = threadFunction;
    this.threadName = threadName;
    this.threadNumber = threadNumber;
    this.usedFunctions = usedFunctions;
    this.creator = creator == null ? Optional.<AThread>absent() : Optional.<AThread>of(creator);
    this.threadCreationStatement = threadCreationStatement == null ? Optional.<AFunctionCall>absent() : Optional.<AFunctionCall>of(threadCreationStatement);

  }

  public FunctionEntryNode getThreadFunction() {
    return threadFunction;
  }

  public void setThreadFunction(FunctionEntryNode threadFunction) {
    this.threadFunction = threadFunction;
  }

  public String getThreadName() {
    return threadName;
  }

  public int getThreadNumber() {
    return threadNumber;
  }

  public Optional<? extends AFunctionCall> getThreadCreationStatement() {
    return threadCreationStatement;
  }

  public Optional<? extends AThread> getCreator() {
    return creator;
  }

  public void addContextSwitch(CFAEdge switchEdge) {
    if (contextSwitchPoints.containsKey(switchEdge.getSuccessor())) {
      ContextSwitch contextSwitchLocation = contextSwitchPoints.get(switchEdge.getSuccessor());
      assert equals(contextSwitchLocation.getThread());
      contextSwitchLocation.addContextStatementCause(switchEdge);

    } else {
      ContextSwitch cs = new ContextSwitch(contextSwitchPoints.size() + 1, this, switchEdge);
      contextSwitchPoints.put(switchEdge.getSuccessor(), cs);
    }

  }

  public abstract void addUsedFunction(String usedFunction, AStatementEdge functionCallStatement);

  public SetMultimap<String, ? extends AStatementEdge> getUsedFunctions() {
    return usedFunctions;
  }

  public List<ContextSwitch> getContextSwitchPoints() {
    List<ContextSwitch> sorted = new ArrayList<ContextSwitch>(contextSwitchPoints.values());
    Collections.sort(sorted,   new Comparator<ContextSwitch>() {

      @Override
      public int compare(ContextSwitch o1, ContextSwitch o2) {
        return o1.getContextSwitchNumber() - o2.getContextSwitchNumber();
      }
    });

    // context switch points musn't appear twice
    assert sorted.size() == new HashSet<>(sorted).size();

    return sorted;
  }


  @Override
  public String toString() {
    return threadName + "[" + threadFunction.getFunctionName() + "]";
  }

}
