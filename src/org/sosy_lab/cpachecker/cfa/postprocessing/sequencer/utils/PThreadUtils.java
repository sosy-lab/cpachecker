package org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils;

import java.util.Collection;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context.CThread;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context.CThreadContainer;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

public class PThreadUtils {

  public static final String PTHREAD_CREATE_NAME = "pthread_create";
  public static final String PTHREAD_JOIN_NAME = "pthread_join";
  public static final String PTHREAD_EXIT_NAME = "pthread_exit";
  public static final String PTHREAD_MUTEX_T_NAME = "pthread_mutex_t";
  public static final String PTHREAD_MUTEX_INIT_NAME = "pthread_mutex_init";
  public static final String PTHREAD_MUTEX_DESTROY_NAME = "pthread_mutex_destroy";
  public static final String PTHREAD_MUTEX_LOCK_NAME = "pthread_mutex_lock";
  public static final String PTHREAD_MUTEX_UNLOCK_NAME = "pthread_mutex_unlock";

  public static final String[] ALL_PTHREAD_FUNCTIONS = { PTHREAD_CREATE_NAME,
      PTHREAD_EXIT_NAME, PTHREAD_JOIN_NAME, PTHREAD_MUTEX_DESTROY_NAME,
      PTHREAD_MUTEX_INIT_NAME, PTHREAD_MUTEX_LOCK_NAME, PTHREAD_MUTEX_T_NAME,
      PTHREAD_MUTEX_UNLOCK_NAME };

  //TODO return type to CFunctionCall!!
  public static Collection<CStatementEdge> getPThreadCreateStatementsReachableBy(CFA cfa, CFunctionEntryNode entryNode) {
    Collection<AStatementEdge> threadCreationCalls = CFAFunctionUtils
        .getAllFunctionCallEdgesReachableBy(cfa, entryNode, 0);
    threadCreationCalls = PThreadUtils.filterPthreadFunctionCallStatements(threadCreationCalls);
    
    return Collections2.transform(threadCreationCalls,
        new Function<AStatementEdge, CStatementEdge>() {

          @Override
          public CStatementEdge apply(AStatementEdge arg0) {
            // pthread_create is a c only function call!
            assert arg0.getStatement() instanceof CFunctionCall;
            return (CStatementEdge) arg0;
          }

        });
  }

  public static Collection<AStatementEdge> filterPthreadFunctionCallStatements(
      Collection<AStatementEdge> functionCalls) {
    return CFAFunctionUtils.filterFunctionCallStatementByNames(functionCalls,
        ALL_PTHREAD_FUNCTIONS);
  }

  
  public static void stubPOSIXFunctions(MutableCFA cfa) {
    // just to be sure
    for (FunctionEntryNode function : PThreadUtils.getPthreadFunctionHeads(cfa)) {
      CFAFunctionUtils.stubFunction(function);
    }
  }
  

  
  public static CThread getCThreadByPThread_Create(AFunctionCall functionCall, CThreadContainer threads) {
    functionCall.getFunctionCallExpression().getParameterExpressions();
    
    String functionName = getCThreadStartFunctionName(functionCall);
    
    for (CThread thread : threads.getAllThreads()) {
      if(thread.getThreadFunction().getFunctionName().equals(functionName)) {
        return thread;
      }
    }
    return null;
  }
  

  /**
   * Returns all FunctionEntryNodes of function which must be stubbed in order
   * to prevent real thread creation
   * 
   * @param cfa
   *          the cfa where the pthread functions should be searched
   */
  public static Collection<FunctionEntryNode> getPthreadFunctionHeads(
      MutableCFA cfa) {

    return Collections2.filter(cfa.getAllFunctionHeads(),
        CFAFunctionUtils.isFunctionEntryName(ALL_PTHREAD_FUNCTIONS));

  }

  public static boolean isPthreadFunction(AStatementEdge edge) {
    Predicate<AStatementEdge> isPthreadFunction = CFAFunctionUtils
        .isStatementEdgeName(ALL_PTHREAD_FUNCTIONS);
    return isPthreadFunction.apply(edge);
  }

  public static String getCThreadStartFunctionName(
      AFunctionCall functionCallStatement) {
    assert PTHREAD_CREATE_NAME.equals(CFAFunctionUtils.getFunctionName(functionCallStatement));
    assert functionCallStatement instanceof CFunctionCall;
    
    AFunctionCallExpression exp = functionCallStatement
        .getFunctionCallExpression();
    List<? extends AExpression> parameterExpressions = exp
        .getParameterExpressions();

    assert parameterExpressions.size() > 2;
    Preconditions.checkElementIndex(2, parameterExpressions.size());
    
    // third parameter of pthread_create is the function pointer
    AExpression p = parameterExpressions.get(2);

    AUnaryExpression pointer = null;
    if (p instanceof AIdExpression) {
      AIdExpression exp2 = (AIdExpression) p;
      exp2.getDeclaration();
      AVariableDeclaration variableDeclaration = (AVariableDeclaration) exp2
          .getDeclaration();
      pointer = (AUnaryExpression) ((AInitializerExpression) variableDeclaration
          .getInitializer()).getExpression();
    } else if (p instanceof AUnaryExpression) {
      pointer = (AUnaryExpression) p;
    }
    AIdExpression aid = (AIdExpression) pointer.getOperand();
    AFunctionDeclaration pointedFunctionDec = (AFunctionDeclaration) aid
        .getDeclaration();

    String functionName = pointedFunctionDec.getName();

    return functionName;
  }

}
