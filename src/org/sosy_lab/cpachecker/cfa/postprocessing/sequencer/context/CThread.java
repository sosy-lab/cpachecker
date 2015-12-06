package org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class CThread extends AThread {
  
  
  public CThread(CFunctionEntryNode threadFunction, String threadName, int threadNumber, @Nullable CFunctionCall pthread_createStatement, CThread creator) {
    super(threadFunction, threadName, threadNumber, pthread_createStatement, HashMultimap.<String, CStatementEdge> create(), creator);
  }

  public CFunctionEntryNode getThreadFunction() {
    return (CFunctionEntryNode) super.getThreadFunction();
  }

  @Override
  public void setThreadFunction(FunctionEntryNode threadFunction) {
    assert threadFunction instanceof CFunctionEntryNode;
    super.setThreadFunction(threadFunction);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Optional<CFunctionCall> getThreadCreationStatement() {
    return (Optional<CFunctionCall>) super.getThreadCreationStatement();
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public void addUsedFunction(String usedFunction, AStatementEdge functionCallStatement) {
    assert functionCallStatement instanceof CStatementEdge;
    ((SetMultimap<String, CStatementEdge>) usedFunctions).put(usedFunction, (CStatementEdge) functionCallStatement);
  }

  @SuppressWarnings("unchecked")
  public SetMultimap<String, CStatementEdge> getUsedFunctions() {
    return (SetMultimap<String, CStatementEdge>) super.getUsedFunctions();
  }
  
  @SuppressWarnings("unchecked")
  public Optional<CThread> getCreator() {
    return (Optional<CThread>) super.getCreator();
  }
  
}