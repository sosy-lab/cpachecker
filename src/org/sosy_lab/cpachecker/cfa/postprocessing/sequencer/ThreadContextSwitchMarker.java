package org.sosy_lab.cpachecker.cfa.postprocessing.sequencer;


import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context.CThread;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.CFAFunctionUtils;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.PThreadUtils;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

public class ThreadContextSwitchMarker extends CFATraversal.DefaultCFAVisitor {
  private final static String FUNCTION_DUMMY_START_NAME = "Function start dummy edge";
  private final static String INIT_GLOBAL_VARS = "INIT GLOBAL VARS";
  private final static String FORCE_CS = "THREAD CREATION";

  private boolean isAtomic = false;

  private boolean isInitializingGlobalVar = false;
  private CThread runningThread;
  private CFA cfa;

  public ThreadContextSwitchMarker(CThread thread, CFA cfa) {
    super();
    this.runningThread = thread;
    if(runningThread.getThreadName().equals("thread0")) {
      isInitializingGlobalVar = true;
    }
    this.cfa = cfa;
  }


  @Override
  public TraversalProcess visitEdge(CFAEdge pEdge) {
    if (CFAFunctionUtils.isFunctionCallStatement(pEdge)) {
      if(!CFAFunctionUtils.isExternFunction((AStatementEdge) pEdge, cfa)) {

        String functionName = CFAFunctionUtils.getFunctionName((AStatementEdge) pEdge);
        assert functionName != null;
        assert pEdge.getEdgeType().equals(CFAEdgeType.StatementEdge);

        runningThread.addUsedFunction(functionName, (CStatementEdge) pEdge);
      }
    }

    if(isInitializingGlobalVar(pEdge)) {
      return TraversalProcess.CONTINUE;
    }

    /* In sv-comp benchmark definitions and rules several statements can be
     * summarized to an atomic statement by a special notation, see
     * <a href="http://sv-comp.sosy-lab.org/" />
     *
     * (__VERIFIER_atomic_begin(), __VERIFIER_atomic_end() )
     */
    if(isAtomic) {
      // TODO implement
    }

    if (isContextSwitch(runningThread, pEdge)) {
      // new context switch possible
      runningThread.addContextSwitch(pEdge);
    }

    return TraversalProcess.CONTINUE;
  }

  /**
   * returns if the given edge is an edge which triggers a context switch point
   */
  private boolean isContextSwitch(CThread runningThread, CFAEdge edge) {
    switch (edge.getEdgeType()) {
    case StatementEdge:
      return isGlobalStatement((AStatementEdge) edge);
    case DeclarationEdge:
      return isInitializingGlobalVar((ADeclarationEdge) edge);
    case BlankEdge:
      return isContextSwitchPoint((BlankEdge) edge);
    default:
      return false;
    }
  }

  private boolean isContextSwitchPoint(BlankEdge edge) {
    return FORCE_CS.equals(edge.getDescription());
  }

  private boolean isInitializingGlobalVar(ADeclarationEdge edge) {
      ADeclaration declaration = edge.getDeclaration();
      if (!(declaration instanceof CVariableDeclaration)) {
        return false;
      }

      CVariableDeclaration variableDec = (CVariableDeclaration) declaration;

      boolean hasInitializer = variableDec.getInitializer() != null;
      return declaration.isGlobal() && hasInitializer;
  }

  private boolean isGlobalStatement(AStatementEdge edge) {
    boolean isContextSwitchTrigger = false;

    AStatement statement = edge.getStatement();
    if (statement instanceof CAssignment) {
      isContextSwitchTrigger = isGlobalVariableAssignement((CAssignment) statement);
    }

    if(statement instanceof AFunctionCall) {
      // function may change global variables by side effect.
      isContextSwitchTrigger |= canFunctionCallInfluenceGlobalVar((AFunctionCall) statement);
      isContextSwitchTrigger |= isPosixFunction((AFunctionCall) statement);
    }

    return isContextSwitchTrigger;
  }

  private boolean canFunctionCallInfluenceGlobalVar(AFunctionCall functionCall) {
    if(!CFAFunctionUtils.isExternFunctionCall(functionCall, cfa)) {
      // a participation of global variable can be located in the function body
      return false;
    }

    // An external function may use an pointer in an unsave way and change a
    // global variable. No assumptions can be made by the parameters
    return true;
  }

  private boolean isGlobalVariableAssignement(CAssignment variableAssignment) {
    // check only writes
    CLeftHandSide leftHandSide = variableAssignment.getLeftHandSide();

    if(leftHandSide instanceof CPointerExpression) {
      // influence of global variable cannot be excluded
      return true;
    }

    if (!(leftHandSide instanceof CIdExpression)) {
      return false;
    }
    CIdExpression variable = (CIdExpression) leftHandSide;

    CSimpleDeclaration a = variable.getDeclaration();
    if (!(a instanceof CVariableDeclaration)) {
      return false;
    }

    CVariableDeclaration variableDec = (CVariableDeclaration) a;
    return variableDec.isGlobal();
  }

  private boolean isPosixFunction(AFunctionCall statement) {
    AFunctionDeclaration declaration = statement.getFunctionCallExpression().getDeclaration();
    if(declaration == null) {
      return false;
    }
    return PThreadUtils.PTHREAD_CREATE_NAME.equals(declaration.getName());
  }

  /**
   * Optimization: prunes away every possible context switch point before the global variable initialization
   */
  private boolean isInitializingGlobalVar(CFAEdge edge) {
    if (!isInitializingGlobalVar) {
      return false;
    }

    if (edge instanceof BlankEdge) {
      BlankEdge be = (BlankEdge) edge;
      if (FUNCTION_DUMMY_START_NAME.equals(be.getDescription())) {
        isInitializingGlobalVar = false;
      }
    }

    return isInitializingGlobalVar;
  }

}
