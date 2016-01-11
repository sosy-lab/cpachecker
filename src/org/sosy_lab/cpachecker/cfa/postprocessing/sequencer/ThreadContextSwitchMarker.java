package org.sosy_lab.cpachecker.cfa.postprocessing.sequencer;


import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
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

    CFunctionCallExpression parameter = (CFunctionCallExpression) functionCall.getFunctionCallExpression();
    for(CExpression exp : parameter.getParameterExpressions()) {
      if(isGlobalVarInvolved(exp)) {
        return true;
      }
//
//      //TODO structs typedefs, casts etc.
//      if(exp instanceof CPointerExpression) {
//        return true;
//      } else if (exp instanceof CArraySubscriptExpression) {
//        return true;
//      }
    }
    // parameter called by value and therefore hasn't effect on global var
    return false;
  }

  private boolean isGlobalVariableAssignement(CAssignment variableAssignment) {
    CLeftHandSide leftHandSide = variableAssignment.getLeftHandSide();

    if(leftHandSide instanceof CPointerExpression) {
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



  private boolean isGlobalVarInvolved(AExpression expression) {
    if (expression instanceof CExpression) {
      CExpression cExpression = (CExpression) expression;

      return cExpression.accept(GlobalExpression.isGlobalExpression);
    } else if (expression instanceof JExpression) {
      throw new UnsupportedOperationException("Sequentialization is not yet supported for Java programs!");
    }
    return false;
  }

  /**
   * Checks if an expression can affect any global variables. This is the case
   * if a global variable or a pointer is involved which could hold an address
   * to a global variable. Beyond that such objects as structs can hold pointer
   * also.
   */
  private static class GlobalExpression implements CExpressionVisitor<Boolean, RuntimeException> {
    public final static GlobalExpression isGlobalExpression = new GlobalExpression();

    @Override
    public Boolean visit(CArraySubscriptExpression pIastArraySubscriptExpression)
        throws RuntimeException {
      // handle like a pointer because a subscript which overflows the reserved
      // memory can access unallocated/global memory
      return true;
    }

    @Override
    public Boolean visit(CFieldReference pIastFieldReference) throws RuntimeException {
      // TODO what is this. check if global is involved
      throw new UnsupportedOperationException("TODO CTypeIdExpression");
    }

    @Override
    public Boolean visit(CIdExpression pIastIdExpression) throws RuntimeException {
      CSimpleDeclaration declaration = pIastIdExpression.getDeclaration();
      if (!(declaration instanceof CVariableDeclaration)) {
        return false;
      }

      CVariableDeclaration variableDec = (CVariableDeclaration) declaration;
      return variableDec.isGlobal();
    }

    @Override
    public Boolean visit(CPointerExpression pointerExpression) throws RuntimeException {
      return true;
    }

    @Override
    public Boolean visit(CComplexCastExpression complexCastExpression) throws RuntimeException {
      return complexCastExpression.getOperand().accept(this);
    }

    @Override
    public Boolean visit(CBinaryExpression pIastBinaryExpression) throws RuntimeException {
      return pIastBinaryExpression.getOperand1().accept(this) || pIastBinaryExpression.getOperand2().accept(this);
    }

    @Override
    public Boolean visit(CCastExpression pIastCastExpression) throws RuntimeException {
      return pIastCastExpression.getOperand().accept(this);
    }

    @Override
    public Boolean visit(CCharLiteralExpression pIastCharLiteralExpression) throws RuntimeException {
      return false;
    }

    @Override
    public Boolean visit(CFloatLiteralExpression pIastFloatLiteralExpression)
        throws RuntimeException {
      return false;
    }

    @Override
    public Boolean visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
        throws RuntimeException {
      return false;
    }

    @Override
    public Boolean visit(CStringLiteralExpression pIastStringLiteralExpression)
        throws RuntimeException {
      return false;
    }

    @Override
    public Boolean visit(CTypeIdExpression pIastTypeIdExpression) throws RuntimeException {
      // TODO what is this. check if global is involved
      throw new UnsupportedOperationException("TODO CTypeIdExpression");
    }

    @Override
    public Boolean visit(CUnaryExpression pIastUnaryExpression) throws RuntimeException {
      return pIastUnaryExpression.getOperand().accept(this);
    }

    @Override
    public Boolean visit(CImaginaryLiteralExpression PIastLiteralExpression)
        throws RuntimeException {
      return false;
    }

    @Override
    public Boolean visit(CAddressOfLabelExpression pAddressOfLabelExpression)
        throws RuntimeException {
      return false;
    }

  }
}
