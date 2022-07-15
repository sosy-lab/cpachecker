// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.reachdef;

import static org.sosy_lab.cpachecker.util.reachingdef.ReachingDefUtils.possiblePointees;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState.ProgramDefinitionPoint;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.reachingdef.ReachingDefUtils;
import org.sosy_lab.cpachecker.util.reachingdef.ReachingDefUtils.VariableExtractor;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

@Options(prefix = "cpa.reachdef")
public class ReachingDefTransferRelation implements TransferRelation {

  @Option(
      secure = true,
      name = "constraintIsDef",
      description =
          "Whether to consider constraints on program variables (e.g., x > 10)"
              + " as definitions)")
  private boolean considerConstraintAsDef = false;

  private Map<FunctionEntryNode, Set<MemoryLocation>> localVariablesPerFunction;

  private CFANode main;

  private final LogManagerWithoutDuplicates logger;
  private final ShutdownNotifier shutdownNotifier;

  public ReachingDefTransferRelation(
      LogManager pLogger, ShutdownNotifier pShutdownNotifier, Configuration pConfig)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = new LogManagerWithoutDuplicates(pLogger);
    shutdownNotifier = pShutdownNotifier;
  }

  public void provideLocalVariablesOfFunctions(
      Map<FunctionEntryNode, Set<MemoryLocation>> localVars) {
    localVariablesPerFunction = localVars;
  }

  public void setMainFunctionNode(CFANode pMain) {
    main = pMain;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pState, Precision pPrecision)
      throws CPATransferException, InterruptedException {
    List<CFANode> nodes = ReachingDefUtils.getAllNodesFromCFA();
    if (nodes == null) {
      throw new CPATransferException("CPA not properly initialized.");
    }
    List<AbstractState> successors = new ArrayList<>();
    List<CFAEdge> definitions = new ArrayList<>();
    for (CFANode node : nodes) {
      for (CFAEdge cfaedge : CFAUtils.leavingEdges(node)) {
        shutdownNotifier.shutdownIfNecessary();

        if (!(cfaedge.getEdgeType() == CFAEdgeType.FunctionReturnEdge)) {
          if (cfaedge.getEdgeType() == CFAEdgeType.StatementEdge
              || cfaedge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
            definitions.add(cfaedge);
          } else {
            successors.addAll(getAbstractSuccessors0(pState, cfaedge));
          }
        }
      }
    }
    for (CFAEdge edge : definitions) {
      successors.addAll(getAbstractSuccessors0(pState, edge));
    }
    return successors;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    Preconditions.checkNotNull(pCfaEdge);
    return getAbstractSuccessors0(pState, pCfaEdge);
  }

  private Collection<? extends AbstractState> getAbstractSuccessors0(
      AbstractState pState, CFAEdge pCfaEdge) throws CPATransferException {

    logger.log(Level.FINE, "Compute successor for ", pState, "along edge", pCfaEdge);

    if (localVariablesPerFunction == null) {
      throw new CPATransferException(
          "Incorrect initialization of reaching definition transfer relation.");
    }

    if (!(pState instanceof ReachingDefState)) {
      throw new CPATransferException(
          "Unexpected type of abstract state. The transfer relation is not defined for this type");
    }

    if (pCfaEdge == null) {
      throw new CPATransferException(
          "Expected an edge along which the successors should be computed");
    }

    if (pState == ReachingDefState.topElement) {
      return Collections.singleton(pState);
    }

    ReachingDefState result;

    switch (pCfaEdge.getEdgeType()) {
      case StatementEdge:
        {
          result = handleStatementEdge((ReachingDefState) pState, (CStatementEdge) pCfaEdge);
          break;
        }
      case DeclarationEdge:
        {
          result = handleDeclarationEdge((ReachingDefState) pState, (CDeclarationEdge) pCfaEdge);
          break;
        }
      case FunctionCallEdge:
        {
          result = handleCallEdge((ReachingDefState) pState, (CFunctionCallEdge) pCfaEdge);
          break;
        }
      case FunctionReturnEdge:
        {
          result = handleReturnEdge((ReachingDefState) pState, (CFunctionReturnEdge) pCfaEdge);
          break;
        }
      case ReturnStatementEdge:
        result = handleReturnStatement((CReturnStatementEdge) pCfaEdge, (ReachingDefState) pState);
        break;

      case AssumeEdge:
        result = handleAssumption((ReachingDefState) pState, (CAssumeEdge) pCfaEdge);
        break;

      case BlankEdge:
        // TODO still correct?
        // special case entering the main method for the first time (no local variables known)
        logger.log(
            Level.FINE,
            "Start of main function. ",
            "Add undefined position for local variables of main function. ",
            "Add definition of parameters of main function.");
        if (Objects.equals(pCfaEdge.getPredecessor(), main)
            && ((ReachingDefState) pState).getLocalReachingDefinitions().size() == 0) {
          result =
              ((ReachingDefState) pState)
                  .initVariables(
                      localVariablesPerFunction.get(pCfaEdge.getPredecessor()),
                      getParameters((CFunctionEntryNode) pCfaEdge.getPredecessor()),
                      pCfaEdge.getPredecessor(),
                      pCfaEdge.getSuccessor());
          break;
        }
        // $FALL-THROUGH$
      case CallToReturnEdge:
        logger.log(
            Level.FINE,
            "Reaching definition not affected by edge. ",
            "Keep reaching definition unchanged.");
        result = (ReachingDefState) pState;
        break;
      default:
        throw new CPATransferException("Unknown CFA edge type.");
    }

    return Collections.singleton(result);
  }

  private ReachingDefState handleAssumption(ReachingDefState pState, CAssumeEdge pCfaEdge) {
    if (!considerConstraintAsDef) {
      return pState;
    }

    Collection<CLeftHandSide> lhsExps =
        pCfaEdge
            .getExpression()
            .accept(
                new DefaultCExpressionVisitor<Collection<CLeftHandSide>, NoException>() {

                  @Override
                  protected Collection<CLeftHandSide> visitDefault(CExpression pExp)
                      throws NoException {
                    if (pExp instanceof CLeftHandSide) {
                      return ImmutableList.of((CLeftHandSide) pExp);

                    } else if (pExp instanceof CLiteralExpression) {
                      return ImmutableList.of();

                    } else if (pExp instanceof CBinaryExpression) {
                      CBinaryExpression binExp = (CBinaryExpression) pExp;

                      Collection<CLeftHandSide> firstRes = binExp.getOperand1().accept(this);
                      Collection<CLeftHandSide> sndRes = binExp.getOperand2().accept(this);

                      Set<CLeftHandSide> res = new HashSet<>(firstRes);

                      res.addAll(sndRes);

                      return res;

                    } else if (pExp instanceof CUnaryExpression) {
                      return ((CUnaryExpression) pExp).getOperand().accept(this);

                    } else {
                      throw new AssertionError("Unhandled operation: " + pExp);
                    }
                  }

                  @Override
                  public Collection<CLeftHandSide> visit(CCastExpression pExp) throws NoException {
                    return pExp.getOperand().accept(this);
                  }

                  @Override
                  public Collection<CLeftHandSide> visit(CAddressOfLabelExpression pExp)
                      throws NoException {
                    return ImmutableList.of();
                  }

                  @Override
                  public Collection<CLeftHandSide> visit(CTypeIdExpression pExp)
                      throws NoException {
                    return ImmutableList.of();
                  }
                });

    ReachingDefState newState = pState;
    for (CLeftHandSide lhs : lhsExps) {
      newState = handleOperation(pCfaEdge, lhs, newState);
    }
    return newState;
  }

  private ImmutableSet<MemoryLocation> getParameters(CFunctionEntryNode pNode) {
    return Collections3.transformedImmutableSetCopy(
        pNode.getFunctionParameters(), x -> MemoryLocation.forDeclaration(x));
  }

  private ReachingDefState handleReturnStatement(
      CReturnStatementEdge pCfaEdge, ReachingDefState pState) {
    Optional<CAssignment> asAssignment = pCfaEdge.asAssignment();
    if (asAssignment.isPresent()) {
      CAssignment assignment = asAssignment.orElseThrow();
      return handleStatement(pState, pCfaEdge, assignment);
    } else {
      return pState;
    }
  }

  private ReachingDefState handleStatement(
      ReachingDefState pState, CFAEdge pEdge, CStatement pStatement) {
    CLeftHandSide left;
    if (pStatement instanceof CExpressionAssignmentStatement) {
      left = ((CExpressionAssignmentStatement) pStatement).getLeftHandSide();
    } else if (pStatement instanceof CFunctionCallAssignmentStatement) {
      // handle function call on right hand side to external method
      left = ((CFunctionCallAssignmentStatement) pStatement).getLeftHandSide();
      logger.logOnce(
          Level.WARNING,
          "Analysis may be unsound if external method redefines global variables",
          "or considers extra global variables.");
    } else {
      return pState;
    }

    return handleOperation(pEdge, left, pState);
  }

  private ReachingDefState handleOperation(
      CFAEdge pEdge, CLeftHandSide pLeft, ReachingDefState pState) {
    MemoryLocation var = getVarName(pEdge, pLeft);
    if (var == null) {
      pState.addUnhandled(pLeft, pEdge.getPredecessor(), pEdge.getSuccessor());
      return pState;
    }

    if (pLeft instanceof CArraySubscriptExpression) {
      // Only add the reaching definition, don't replace
      ReachingDefState newState =
          addReachDef(pState, var, pEdge.getPredecessor(), pEdge.getSuccessor());
      return pState.join(newState);
    }

    logger.log(
        Level.FINE,
        "Edge provided a new definition of variable ",
        var,
        ". Update reaching definition.");
    return addReachDef(pState, var, pEdge.getPredecessor(), pEdge.getSuccessor());
  }

  /*
   * Note that currently it is not dealt with aliasing.
   * Thus, if two variables s1 and s2 of non basic type point to same element and
   * variable s1 is used to update the element,
   * only the reaching definition of s1 will be updated.
   */
  private ReachingDefState handleStatementEdge(ReachingDefState pState, CStatementEdge edge) {
    CStatement statement = edge.getStatement();

    return handleStatement(pState, edge, statement);
  }

  private ReachingDefState addReachDef(
      ReachingDefState pOld, MemoryLocation pVarName, CFANode pDefStart, CFANode pDefEnd) {
    if (pOld.getGlobalReachingDefinitions().containsKey(pVarName)) {
      return pOld.addGlobalReachDef(pVarName, pDefStart, pDefEnd);
    } else {
      assert pOld.getLocalReachingDefinitions().containsKey(pVarName);
      return pOld.addLocalReachDef(pVarName, pDefStart, pDefEnd);
    }
  }

  private MemoryLocation getVarName(CFAEdge pEdge, CExpression pLhs) {
    // if some array element is changed the whole array is considered to be changed
    /* if a field is changed the whole variable the field is associated with is considered to be changed,
     * e.g. a.p.c = 110, then a should be considered
     */
    VariableExtractor varExtractor = new VariableExtractor(pEdge);
    varExtractor.resetWarning();
    try {
      MemoryLocation var = pLhs.accept(varExtractor);
      if (varExtractor.getWarning() != null) {
        logger.logOnce(Level.WARNING, varExtractor.getWarning());
      }
      return var;

    } catch (UnsupportedCodeException e) {
      return null;
    }
  }

  private ReachingDefState handleDeclarationEdge(ReachingDefState pState, CDeclarationEdge edge) {
    if (edge.getDeclaration() instanceof CVariableDeclaration) {
      CVariableDeclaration dec = (CVariableDeclaration) edge.getDeclaration();
      // If there is no initialization at the declaration,
      // we still keep the declaration as a non-deterministic, first definition.
      MemoryLocation var = MemoryLocation.forDeclaration(dec);
      if (dec.isGlobal()) {
        return pState.addGlobalReachDef(var, edge.getPredecessor(), edge.getSuccessor());
      } else {
        return pState.addLocalReachDef(var, edge.getPredecessor(), edge.getSuccessor());
      }
    }
    return pState;
  }

  private ReachingDefState handleCallEdge(ReachingDefState pState, CFunctionCallEdge pCfaEdge) {
    logger.log(
        Level.FINE,
        "New internal function called. ",
        "Add undefined position for local " + "variables and return variable, if it exists. ",
        "Add definition of parameters.");
    return pState.initVariables(
        localVariablesPerFunction.get(pCfaEdge.getSuccessor()),
        getParameters(pCfaEdge.getSuccessor()),
        pCfaEdge.getPredecessor(),
        pCfaEdge.getSuccessor());
  }

  private ReachingDefState handleReturnEdge(
      ReachingDefState pState, CFunctionReturnEdge pReturnEdge) {
    logger.log(
        Level.FINE,
        "Return from internal function call. ",
        "Remove local variables and parameters of function from reaching definition.");
    ReachingDefState newState = pState.pop(pReturnEdge.getPredecessor().getFunctionName());

    CFunctionCall callExpression = pReturnEdge.getSummaryEdge().getExpression();
    CFunctionCallExpression functionCall = callExpression.getFunctionCallExpression();

    List<CExpression> outFunctionParams = functionCall.getParameterExpressions();
    List<CParameterDeclaration> inFunctionParams = functionCall.getDeclaration().getParameters();

    CFANode defStart = pReturnEdge.getPredecessor();
    CFANode defEnd = pReturnEdge.getSuccessor();
    // TODO support varargs
    for (int i = 0; i < inFunctionParams.size(); i++) {
      CParameterDeclaration inParam = inFunctionParams.get(i);
      CExpression outParam = outFunctionParams.get(i);

      CType parameterType = inParam.getType();
      if (parameterType instanceof CArrayType) {
        MemoryLocation var = getVarName(pReturnEdge, outParam);
        assert var != null;
        newState = addReachDef(newState, var, defStart, defEnd);
      } else if (parameterType instanceof CPointerType) {
        pState.addUnhandled(outParam, defStart, defEnd);
      }
    }

    newState = handleStatement(newState, pReturnEdge, callExpression);

    return newState;
  }

  @Override
  public @Nullable Collection<? extends AbstractState> strengthen(
      AbstractState state,
      Iterable<AbstractState> otherStates,
      CFAEdge cfaEdge,
      Precision precision)
      throws CPATransferException, InterruptedException {

    for (AbstractState o : otherStates) {
      if (o instanceof PointerState) {
        return strengthen((ReachingDefState) state, (PointerState) o);
      }
    }
    return Collections.singleton(state);
  }

  private Collection<? extends AbstractState> strengthen(ReachingDefState pState, PointerState pO) {

    Map<CExpression, ProgramDefinitionPoint> unhandledExps = pState.getAndResetUnhandled();

    ReachingDefState nextState = pState;
    for (Entry<CExpression, ProgramDefinitionPoint> e : unhandledExps.entrySet()) {
      CExpression exp = e.getKey();
      CFANode entry = e.getValue().getDefinitionEntryLocation();
      CFANode exit = e.getValue().getDefinitionExitLocation();

      Set<MemoryLocation> pointees = possiblePointees(exp, pO);
      if (pointees == null) {
        // every var could be reassigned
        for (MemoryLocation localVar : pState.getLocalReachingDefinitions().keySet()) {
          nextState = pState.addLocalReachDef(localVar, entry, exit).join(nextState);
        }
        for (MemoryLocation globalVar : pState.getGlobalReachingDefinitions().keySet()) {
          nextState = pState.addGlobalReachDef(globalVar, entry, exit).join(nextState);
        }
      } else {
        boolean ambiguous;
        if (pointees.size() == 1) {
          ambiguous = false;
        } else {
          ambiguous = true;
        }
        for (MemoryLocation p : pointees) {
          ReachingDefState intermediateState;
          if (p.isOnFunctionStack()) {
            intermediateState = nextState.addLocalReachDef(p, entry, exit);
          } else {
            intermediateState = nextState.addGlobalReachDef(p, entry, exit);
          }
          if (ambiguous) {
            nextState = nextState.join(intermediateState);
          } else {
            nextState = intermediateState;
          }
        }
      }
    }
    return ImmutableSet.of(nextState);
  }
}
