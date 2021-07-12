// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.taint;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AInitializer;
import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.APointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.java.JArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.BuiltinOverflowFunctions;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class TaintAnalysisTransferRelation
    extends ForwardingTransferRelation<
        TaintAnalysisState, TaintAnalysisState, VariableTrackingPrecision> {

  @Options(prefix = "cpa.taint")
  public static class TaintTransferOptions {

    @Option(
        secure = true,
        name = "useCriticalSourceFunctions",
        description =
            "Determines if predefined critical functions should be used as taint sources.")
    private boolean useCriticalSourceFunctions = true;

    @Option(
        secure = false,
        name = "criticalSourceFunctions",
        description =
            "List of critical functions that should be used as taint sources.")
    private String criticalSourceFunctions;

    @Option(
        secure = true,
        name = "useCriticalSinkFunctions",
        description = "Determines if predefined critical functions should be used as taint sinks.")
    private boolean useCriticalSinkFunctions = true;

    @Option(
        secure = false,
        name = "criticalSinkFunctions",
        description =
            "List of critical functions that should be used as taint sinks.")
    private String criticalSinkFunctions;

    public TaintTransferOptions(Configuration config) throws InvalidConfigurationException {
      config.inject(this);
    }

    boolean isUseCriticalSourceFunctions() {
      return useCriticalSourceFunctions;
    }

    Supplier<Stream<String>> sourceFunctionSupplier() {
      Supplier<Stream<String>> streamSupplier = () -> Stream.of();
      if(criticalSourceFunctions != null && criticalSourceFunctions.length() > 0) {
        streamSupplier = () -> Arrays.stream(criticalSourceFunctions.split(","));
      }
      return streamSupplier;
    }

    boolean isUseCriticalSinkFunctions() {
      return useCriticalSinkFunctions;
    }

    Supplier<Stream<String>> sinkFunctionSupplier() {
      Supplier<Stream<String>> streamSupplier = () -> Stream.of();
      if(criticalSinkFunctions != null && criticalSinkFunctions.length() > 0) {
        streamSupplier = () -> Arrays.stream(criticalSinkFunctions.split(","));
      }
      return streamSupplier;
    }
  }

  private final TaintTransferOptions options;
  private final LogManagerWithoutDuplicates logger;

  public TaintAnalysisTransferRelation(LogManager pLogger, TaintTransferOptions pOptions) {
    logger = new LogManagerWithoutDuplicates(pLogger);
    options = pOptions;
  }

  @Override
  protected void setInfo(
      AbstractState pAbstractState, Precision pAbstractPrecision, CFAEdge pCfaEdge) {
    super.setInfo(pAbstractState, pAbstractPrecision, pCfaEdge);
  }

  @Override
  protected TaintAnalysisState handleFunctionCallEdge(
      FunctionCallEdge callEdge,
      List<? extends AExpression> arguments,
      List<? extends AParameterDeclaration> parameters,
      String calledFunctionName)
      throws UnrecognizedCodeException {
    TaintAnalysisState newElement = TaintAnalysisState.copyOf(state);
    String msg = "handleFunctionCallEdge:";

    assert (parameters.size() == arguments.size())
        || callEdge.getSuccessor().getFunctionDefinition().getType().takesVarArgs();

    // visitor for getting the values of the actual parameters in caller function context
    // final ExpressionValueVisitor visitor = getVisitor();

    // get value of actual parameter in caller function context
    for (int i = 0; i < parameters.size(); i++) {
      AParameterDeclaration param = parameters.get(i);
      String paramName = param.getName();
      AExpression exp = arguments.get(i);
      MemoryLocation formalParamName = MemoryLocation.valueOf(calledFunctionName, paramName);
      MemoryLocation formalArgName = MemoryLocation.valueOf(functionName, exp.toString());
      Boolean consoleParam = formalArgName.getIdentifier().contains("argv") || formalArgName.getIdentifier().contains("argc");
      Boolean mainFunction = formalArgName.getFunctionName().equals("main");
      if(mainFunction && consoleParam) {
        newElement.assignTaint(formalParamName, true);
      }
      else {
        newElement.assignTaint(formalParamName, state.getStatus(formalArgName));
      }
      msg = msg + "\nArg: "+formalArgName +": "+ state.getStatus(formalArgName);
      msg = msg + " => Param: "+formalParamName +": "+ newElement.getStatus(formalParamName);
    }
    logger.log(Level.INFO, msg);
    return newElement;
  }

  @Override
  protected TaintAnalysisState handleBlankEdge(BlankEdge cfaEdge) {
    if (cfaEdge.getSuccessor() instanceof FunctionExitNode) {
      // clone state, because will be changed through removing all variables of current function's
      // scope
      state = TaintAnalysisState.copyOf(state);
      // state.dropFrame(functionName);
    }

    return state;
  }

  @Override
  protected TaintAnalysisState handleReturnStatementEdge(AReturnStatementEdge returnEdge)
      throws UnrecognizedCodeException {
    state = TaintAnalysisState.copyOf(state);
    return state;
  }

  /**
   * Handles return from one function to another function.
   *
   * @param functionReturnEdge return edge from a function to its call site
   * @return new abstract state
   */
  @Override
  protected TaintAnalysisState handleFunctionReturnEdge(
      FunctionReturnEdge functionReturnEdge,
      FunctionSummaryEdge summaryEdge,
      AFunctionCall exprOnSummary,
      String callerFunctionName)
      throws UnrecognizedCodeException {

    TaintAnalysisState newElement = TaintAnalysisState.copyOf(state);

    return newElement;
  }

  @Override
  protected TaintAnalysisState handleFunctionSummaryEdge(CFunctionSummaryEdge cfaEdge)
      throws CPATransferException {
    TaintAnalysisState newState = TaintAnalysisState.copyOf(state);

    return newState;
  }

  @Override
  protected TaintAnalysisState handleAssumption(
      AssumeEdge cfaEdge, AExpression expression, boolean truthValue)
      throws UnrecognizedCodeException {
    return state;
  }

  @Override
  protected TaintAnalysisState handleDeclarationEdge(
      ADeclarationEdge declarationEdge, ADeclaration declaration) throws UnrecognizedCodeException {
    
    if (!(declaration instanceof AVariableDeclaration)) {
      return state;
    }
    
    TaintAnalysisState newElement = TaintAnalysisState.copyOf(state);
    AVariableDeclaration decl = (AVariableDeclaration) declaration;
    Type declarationType = decl.getType();

    // get the variable name in the declarator
    String varName = decl.getName();

    // Value initialValue = getDefaultInitialValue(decl);

    // // get initializing statement
    AInitializer init = decl.getInitializer();

    MemoryLocation memoryLocation;

    // assign initial value if necessary
    if (decl.isGlobal()) {
      memoryLocation = MemoryLocation.valueOf(varName);
    } else {
      memoryLocation = MemoryLocation.valueOf(functionName, varName);
    }

    newElement.assignTaint(memoryLocation, false);
    String msg;
    msg = memoryLocation + " | tainted: false | Type: " + declarationType;

    if (init instanceof AInitializerExpression) {
      AExpression exp = ((AInitializerExpression) init).getExpression();
      if(exp instanceof AUnaryExpression) {
        AUnaryExpression unary = (AUnaryExpression) exp;
        if(unary.getOperator() == UnaryOperator.AMPER) {
          MemoryLocation dest = MemoryLocation.valueOf(functionName, unary.getOperand().toString());
          newElement.addPointerTo(memoryLocation, dest);
          msg = msg + " | Pointer " + memoryLocation + " points to " + dest;
        }
      }
      else if(exp instanceof APointerExpression) {
        APointerExpression pointer = (APointerExpression) exp;
        MemoryLocation p = MemoryLocation.valueOf(functionName, pointer.getOperand().toString());
        MemoryLocation dest = newElement.getPointerTo(p);
        newElement.change(memoryLocation, newElement.getStatus(dest));
        msg = msg + " | data from pointer: " + dest +" => tainted: "+newElement.getStatus(dest);
      }
      msg = msg + " | EXP: " + exp;
    }
    logger.log(Level.INFO, msg);
    return newElement;
  }

  @Override
  protected TaintAnalysisState handleStatementEdge(AStatementEdge cfaEdge, AStatement expression)
      throws UnrecognizedCodeException {

    TaintAnalysisState newElement = TaintAnalysisState.copyOf(state);

    String msg = "";

    if (expression instanceof CFunctionCall) {
      CFunctionCall functionCall = (CFunctionCall) expression;
      CFunctionCallExpression functionCallExp = functionCall.getFunctionCallExpression();
      CExpression fn = functionCallExp.getFunctionNameExpression();

      msg = msg + "functionCall: " + functionCall + " | functionCallExp: " + functionCallExp + " | fn: " + fn;

      if (fn instanceof CIdExpression) {
        String func = ((CIdExpression) fn).getName();
        msg = msg + " | func: " + func;

        if (expression instanceof CFunctionCallAssignmentStatement) {
          msg = msg + " | CFunctionCallAssignmentStatement";
          CFunctionCallAssignmentStatement pFunctionCallAssignment = (CFunctionCallAssignmentStatement) expression;
          
          if (options.isUseCriticalSourceFunctions()) {
            MemoryLocation memoryLocation1 = TaintSourceFunctions(pFunctionCallAssignment, func);
            if(memoryLocation1 != null) {
              newElement.change(memoryLocation1, true);
            }
          }
        }
        
        else if (BuiltinOverflowFunctions.isBuiltinOverflowFunction(func)) {
          if (!BuiltinOverflowFunctions.isFunctionWithoutSideEffect(func)) {
            throw new UnsupportedCodeException(func + " is unsupported for this analysis", null);
          }
        }
        
        else if (expression instanceof CFunctionCallAssignmentStatement) {
          msg = msg + " | CFunctionCallAssignmentStatement";
        }
        
        else if (expression instanceof CFunctionCallStatement) {
          msg = msg + " | CFunctionCallStatement";
          AFunctionCallStatement stm = (AFunctionCallStatement) expression;
          AFunctionCallExpression exp = stm.getFunctionCallExpression();
          AExpression param = exp.getParameterExpressions().get(0);
          // VERIFIER logic
          MemoryLocation memoryLocation = MemoryLocation.valueOf(functionName, param.toString());
          if (func.equals("__VERIFIER_mark_tainted")) {
            newElement.change(memoryLocation, true);
          } else if (func.equals("__VERIFIER_mark_untainted")) {
            newElement.change(memoryLocation, false);
          } else if (func.equals("__VERIFIER_assert_untainted")) {
            if (state.getStatus(memoryLocation)) {
              newElement = TaintAnalysisState.copyOf(state, true, "Assumed variable '" + memoryLocation + "' was untainted");
            }
          } else if (func.equals("__VERIFIER_assert_tainted")) {
            if (!state.getStatus(memoryLocation)) {
              newElement = TaintAnalysisState.copyOf(state, true, "Assumed variable '" + memoryLocation + "' was tainted");
            }
          } else if(func.equals("strncpy")) {
            AExpression param0 = exp.getParameterExpressions().get(0);
            MemoryLocation memoryLocation0 = MemoryLocation.valueOf(functionName, param0.toString());
            AExpression param1 = exp.getParameterExpressions().get(1);
            MemoryLocation memoryLocation1 = MemoryLocation.valueOf(functionName, param1.toString());
            Boolean consoleParam = memoryLocation1.getIdentifier().contains("argv") || memoryLocation1.getIdentifier().contains("argc");
            Boolean mainFunction = memoryLocation1.getFunctionName().equals("main");
            if((mainFunction && consoleParam) || (state.getStatus(memoryLocation1) != null && state.getStatus(memoryLocation1))) {
              newElement.change(memoryLocation0, true);
              msg = msg + " | "+memoryLocation0.toString() + " got tainted!";
            }
          }
          if (options.isUseCriticalSourceFunctions()) {
            // Critical Source Functions
            Supplier<Stream<String>> sourceSupplier = options.sourceFunctionSupplier();
            if(sourceSupplier.get().anyMatch(str -> func.equals(str.strip()))) {
                param = exp.getParameterExpressions().get(1);
                String param_ = param.toString();
                param_ = param_.replace("&", "");
                MemoryLocation memoryLocation1 = MemoryLocation.valueOf(functionName, param_);
                newElement.change(memoryLocation1, true);
            }
          } if (options.isUseCriticalSinkFunctions()) {
            // Critical Sink Functions
            Supplier<Stream<String>> sinkSupplier = options.sinkFunctionSupplier();
            MemoryLocation memoryLocation1;
            if(exp.getParameterExpressions().size() == 1) {
              memoryLocation1 = MemoryLocation.valueOf(functionName, exp.getParameterExpressions().get(0).toString());
            }
            else {
              memoryLocation1 = MemoryLocation.valueOf(functionName, exp.getParameterExpressions().get(1).toString());
            }
            if (func.equals("printf") || func.equals("snprintf") || func.equals("syslog")) {
              Boolean tainted = false;
              List<? extends AExpression> params = exp.getParameterExpressions();
              for (int i = 0; i < params.size(); i++) {
                memoryLocation = MemoryLocation.valueOf(functionName, exp.getParameterExpressions().get(i).toString());
                msg = msg + " | "+memoryLocation;
                logger.log(Level.INFO, msg);
                Boolean consoleParam = memoryLocation.getIdentifier().contains("argv") || memoryLocation.getIdentifier().contains("argc");
                Boolean mainFunction = memoryLocation.getFunctionName().equals("main");
                if((mainFunction && consoleParam) || (state.getStatus(memoryLocation) != null && state.getStatus(memoryLocation))) {
                  tainted = true;
                }
              }
              if(tainted) {
                newElement = TaintAnalysisState.copyOf(state, true, "Critical function '" + func + "' was called with a tainted parameter");
              }
            }
            else if(sinkSupplier.get().anyMatch(str -> func.equals(str.strip()))) {
              msg = msg + " | MemLoc:" + memoryLocation1+": "+state.getStatus(memoryLocation1);
              Boolean consoleParam = memoryLocation1.getIdentifier().contains("argv") || memoryLocation1.getIdentifier().contains("argc");
              Boolean mainFunction = memoryLocation1.getFunctionName().equals("main");
              if((mainFunction && consoleParam) || (state.getStatus(memoryLocation1) != null && state.getStatus(memoryLocation1))) {
                newElement = TaintAnalysisState.copyOf(state, true, "Critical function '" + func + "' was called with a tainted parameter");
              }
            }
          }
          msg = msg + " | " + param;
        } else {
          msg = msg + " | else: " + expression.getClass();
        }
      }
    }

    // expression is a binary operation, e.g. a = b;

    else if (expression instanceof AAssignment) {
      newElement = handleAssignment((AAssignment) expression, cfaEdge);
      return newElement;

    } else if (expression instanceof AFunctionCallStatement) {
      msg = msg + " | AFunctionCallStatement";

    } else if (expression instanceof AExpressionStatement) {
      msg = msg + " | AExpressionStatement";

    } else {
      throw new UnrecognizedCodeException("Unknown statement", cfaEdge, expression);
    }
    logger.log(Level.INFO, msg);
    return newElement;
  }

  private MemoryLocation TaintSourceFunctions(CFunctionCallAssignmentStatement pFunctionCallAssignment, String func) {
    Supplier<Stream<String>> streamSupplier = options.sourceFunctionSupplier();
    if(streamSupplier.get().anyMatch(str -> func.equals(str.strip()))) {
      AFunctionCallExpression exp = pFunctionCallAssignment.getRightHandSide();
      CLeftHandSide leftSide = pFunctionCallAssignment.getLeftHandSide();
      if(!exp.getParameterExpressions().isEmpty() && exp.getParameterExpressions().get(0) != null) {
        AExpression param = exp.getParameterExpressions().get(0);
        String param_ = param.toString();
        param_ = param_.replace("&", "");
        MemoryLocation memoryLocation1 = MemoryLocation.valueOf(functionName, param_);
        return memoryLocation1;
      } else {
        MemoryLocation memoryLocation1 = MemoryLocation.valueOf(functionName, leftSide.toString());
        return memoryLocation1;
      }
    }
    else {
      return null;
    }
  }

  private TaintAnalysisState handleAssignment(AAssignment assignExpression, CFAEdge cfaEdge)
      throws UnrecognizedCodeException {
    TaintAnalysisState newElement = TaintAnalysisState.copyOf(state);
    String msg = "";

    AExpression op1 = assignExpression.getLeftHandSide();
    ARightHandSide op2 = assignExpression.getRightHandSide();
    msg = msg + op1 + " = " + op2 + " " + op2.getExpressionType();

    if (op1 instanceof AIdExpression) {
      // a = ...
      msg = msg + " | AIdExpression";
      if (op2 instanceof ALiteralExpression) {
        msg = msg + " | ALiteralExpression";
        MemoryLocation memoryLocation = MemoryLocation.valueOf(functionName, op1.toString());
        newElement.change(memoryLocation, false);
        return newElement;
      } else if (op2 instanceof ABinaryExpression) {
        msg = msg + " | ABinaryExpression";
        ABinaryExpression binOp = (ABinaryExpression) op2;
        msg = msg + " | Operator: "+binOp.getOperator();
        if (binOp.getOperator() == BinaryOperator.MINUS && binOp.getOperand1().toString().equals(binOp.getOperand2().toString())) {
          msg = msg + " | Self sebstract";
          MemoryLocation memoryLocation = MemoryLocation.valueOf(functionName, op1.toString());
          newElement.change(memoryLocation, false);
        } else {
          Boolean result = false;
          MemoryLocation memOp1 = MemoryLocation.valueOf(functionName, binOp.getOperand1().toString());
          MemoryLocation memOp2 = MemoryLocation.valueOf(functionName, binOp.getOperand2().toString());
          if(binOp.getOperand1() instanceof AIdExpression && binOp.getOperand2() instanceof AIdExpression) {
            result = state.getStatus(memOp1) || state.getStatus(memOp2);
          }
          else if(binOp.getOperand1() instanceof ALiteralExpression) {
            result = state.getStatus(memOp2);
          }
          else if(binOp.getOperand2() instanceof ALiteralExpression) {
            result = state.getStatus(memOp1);
          }
          MemoryLocation memoryLocation = MemoryLocation.valueOf(functionName, op1.toString());
          if(result != null) {
            newElement.change(memoryLocation, result);
          }
          msg = msg + " | result: "+result;
        }
      } else if (op2 instanceof AArraySubscriptExpression) {
        msg = msg + " | AArraySubscriptExpression";
        AArraySubscriptExpression arraySubsExp = (AArraySubscriptExpression) op2;
        msg = msg + " | getSubscriptExpression: " + arraySubsExp.getSubscriptExpression();
        msg = msg + " | getArrayExpression: " + arraySubsExp.getArrayExpression();
        Boolean result = false;
        if(!arraySubsExp.getArrayExpression().toString().equals("argv")) {
          MemoryLocation arr = MemoryLocation.valueOf(functionName, arraySubsExp.getArrayExpression().toString());
          result = state.getStatus(arr);
        } else {
          result = true;
        }
        MemoryLocation memoryLocation = MemoryLocation.valueOf(functionName, op1.toString());
        newElement.change(memoryLocation, result);
      } else {
        msg = msg + " | ELSE: " + op2.getClass();
        if(op1.getExpressionType().toString().indexOf('*') != -1) {
          MemoryLocation memOp1 = MemoryLocation.valueOf(functionName, op1.toString());
          MemoryLocation memOp2 = MemoryLocation.valueOf(functionName, op2.toString());
          newElement.addPointerTo(memOp1, memOp2);
        } else {
          MemoryLocation memOp1 = MemoryLocation.valueOf(functionName, op1.toString());
          MemoryLocation memOp2 = MemoryLocation.valueOf(functionName, op2.toString());
          msg = msg + " | Status op2: " + state.getStatus(memOp2);
          if(newElement.getStatus(memOp2) != null) {
            newElement.change(memOp1, newElement.getStatus(memOp2));
          }
          msg = msg + " | Status op1: " + newElement.getStatus(memOp1);
        }
      }
    } else if (op1 instanceof APointerExpression) {
      // *a = ...
      msg = msg + "\nAPointerExpression";

    } else if (op1 instanceof CFieldReference) {
      // ???
      msg = msg + "\nCFieldReference";

    } else if (op1 instanceof AArraySubscriptExpression) {
      // array cell
      msg = msg + " | AArraySubscriptExpression";
      if (op1 instanceof CArraySubscriptExpression) {
        msg = msg + " | CArraySubscriptExpression";
      } else if (op1 instanceof JArraySubscriptExpression) {
        msg = msg + " | JArraySubscriptExpression";
      }
    } else {
      throw new UnrecognizedCodeException(
          "left operand of assignment has to be a variable", cfaEdge, op1);
    }

    logger.log(Level.INFO, msg);
    return newElement;
  }
}
