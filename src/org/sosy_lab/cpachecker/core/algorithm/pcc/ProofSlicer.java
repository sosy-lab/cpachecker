// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.pcc;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class ProofSlicer {

  private final ReachedSetFactory reachedSetFactory;

  private int numNotCovered;

  public ProofSlicer(LogManager pLogger) throws InvalidConfigurationException {
    // TODO: Really always a standard reached set instead of user-specified configuration?
    reachedSetFactory = new ReachedSetFactory(Configuration.defaultConfiguration(), pLogger);
  }

  public UnmodifiableReachedSet sliceProof(
      final UnmodifiableReachedSet pReached, final ConfigurableProgramAnalysis pCpa) {
    AbstractState first = pReached.getFirstState();
    if (first instanceof ARGState
        && AbstractStates.extractLocation(first) != null
        && AbstractStates.extractStateByType(first, ValueAnalysisState.class) != null
        && AbstractStates.extractStateByType(first, CallstackState.class) != null
        && ((ARGState) first).getWrappedState() instanceof CompositeState) {
      numNotCovered = 0;
      Map<ARGState, Set<String>> varMap = Maps.newHashMapWithExpectedSize(pReached.size());

      computeRelevantVariablesPerState((ARGState) first, varMap);

      assert (numNotCovered == pReached.size());
      return buildSlicedARG(varMap, pReached, pCpa);
    }

    return pReached;
  }

  private void computeRelevantVariablesPerState(
      final ARGState root, final Map<ARGState, Set<String>> varMap) {
    Deque<ARGState> waitlist = new ArrayDeque<>();

    initializeStates(root, varMap, waitlist);

    ARGState next;
    while (!waitlist.isEmpty()) {
      next = waitlist.pop();

      for (ARGState succ : getStateAndItsCoveredNodes(next)) {
        assert varMap.containsKey(succ);

        for (ARGState p : succ.getParents()) {
          if (p.getEdgeToChild(succ) == null) {
            if (computeTransferTo(p, succ, varMap.get(succ), varMap)) {
              waitlist.push(p);
            }
          } else {
            if (computeTransferTo(p, p.getEdgeToChild(succ), varMap.get(succ), varMap)) {
              waitlist.push(p);
            }
          }
        }
      }
    }
  }

  private Collection<ARGState> getStateAndItsCoveredNodes(final ARGState state) {
    Collection<ARGState> result = new HashSet<>();
    Deque<ARGState> waitlist = new ArrayDeque<>();

    waitlist.add(state);
    result.add(state);

    while (!waitlist.isEmpty()) {
      for (ARGState covered : waitlist.pop().getCoveredByThis()) {
        if (result.add(covered)) {
          waitlist.add(covered);
        }
      }
    }

    return result;
  }

  private boolean computeTransferTo(
      final ARGState pred,
      final ARGState succ,
      final Set<String> succVars,
      final Map<ARGState, Set<String>> varMap) {
    assert varMap.containsKey(pred);
    Set<String> updatedVars = new HashSet<>(varMap.get(pred));

    Set<String> sSet = new HashSet<>(succVars);
    Set<String> pSet = new HashSet<>();

    List<CFAEdge> edges = pred.getEdgesToChild(succ);
    for (int i = edges.size() - 1; i >= 0; i--) {
      addTransferSet(edges.get(i), sSet, pSet);

      sSet = pSet;
      pSet = new HashSet<>();
    }

    updatedVars.addAll(sSet);

    if (varMap.get(pred).size() != updatedVars.size()) {
      assert (varMap.get(pred).size() < updatedVars.size());
      varMap.put(pred, updatedVars);
      updateCoveredNodes(pred, updatedVars, varMap);
      return true;
    }
    return false;
  }

  private boolean computeTransferTo(
      final ARGState pred,
      final CFAEdge edge,
      final Set<String> succVars,
      final Map<ARGState, Set<String>> varMap) {
    assert varMap.containsKey(pred);
    Set<String> updatedPredVars = new HashSet<>(varMap.get(pred));

    addTransferSet(edge, succVars, updatedPredVars);

    if (varMap.get(pred).size() != updatedPredVars.size()) {
      assert (varMap.get(pred).size() < updatedPredVars.size());
      varMap.put(pred, updatedPredVars);
      updateCoveredNodes(pred, updatedPredVars, varMap);
      return true;
    }
    return false;
  }

  private void addTransferSet(
      final CFAEdge edge, Set<String> succVars, final Set<String> updatedVars) {
    switch (edge.getEdgeType()) {
      case StatementEdge:
        CStatement stm = ((CStatementEdge) edge).getStatement();
        if (stm instanceof CExpressionStatement || stm instanceof CFunctionCallStatement) {
          updatedVars.addAll(succVars);
        } else {
          String varNameAssigned;
          if (stm instanceof CFunctionCallAssignmentStatement) {
            varNameAssigned =
                VarNameRetriever.getVarName(
                    ((CFunctionCallAssignmentStatement) stm).getLeftHandSide());
            if (succVars.contains(varNameAssigned)) {
              for (CExpression expr :
                  ((CFunctionCallAssignmentStatement) stm)
                      .getRightHandSide()
                      .getParameterExpressions()) {
                CFAUtils.getVariableNamesOfExpression(expr).copyInto(updatedVars);
              }
            }
          } else { // CExpressionAssignmentStatement
            varNameAssigned =
                VarNameRetriever.getVarName(
                    ((CExpressionAssignmentStatement) stm).getLeftHandSide());
            if (succVars.contains(varNameAssigned)) {
              CFAUtils.getVariableNamesOfExpression(
                      ((CExpressionAssignmentStatement) stm).getRightHandSide())
                  .copyInto(updatedVars);
            }
          }
          if (succVars.contains(varNameAssigned)) {
            addAllExceptVar(varNameAssigned, succVars, updatedVars);
          } else {
            updatedVars.addAll(succVars);
          }
        }
        return;
      case DeclarationEdge:
        if (((CDeclarationEdge) edge).getDeclaration() instanceof CVariableDeclaration) {
          CVariableDeclaration varDec =
              (CVariableDeclaration) ((CDeclarationEdge) edge).getDeclaration();
          String varName = varDec.getQualifiedName();
          if (succVars.contains(varName)) {
            if (varDec.getInitializer() != null) {
              updatedVars.addAll(getInitializerVars(varDec.getInitializer()));
            }

            addAllExceptVar(varName, succVars, updatedVars);

          } else {
            updatedVars.addAll(succVars);
          }

        } else {
          updatedVars.addAll(succVars);
        }
        return;
      case ReturnStatementEdge:
        CReturnStatementEdge retStm = ((CReturnStatementEdge) edge);
        if (retStm.getExpression().isPresent()
            && !retStm.getSuccessor().getEntryNode().getReturnVariable().isPresent()) {
          throw new AssertionError("Return statement but no return variable available");
        }

        if (retStm.getSuccessor().getEntryNode().getReturnVariable().isPresent()) {
          String varName =
              retStm.getSuccessor().getEntryNode().getReturnVariable().get().getQualifiedName();
          addAllExceptVar(varName, succVars, updatedVars);

          if (retStm.getExpression().isPresent()) {
            CFAUtils.getVariableNamesOfExpression(retStm.getExpression().orElseThrow())
                .copyInto(updatedVars);
          }
        } else {
          updatedVars.addAll(succVars);
        }
        return;
      case FunctionCallEdge:
        CFunctionCallEdge funCall = ((CFunctionCallEdge) edge);
        Collection<String> paramNames = new HashSet<>();

        String paramName;
        List<CParameterDeclaration> paramDecl = funCall.getSuccessor().getFunctionParameters();
        List<CExpression> args = funCall.getArguments();
        assert (paramDecl.size() == args.size());
        for (int i = 0; i < paramDecl.size(); i++) {
          paramName = paramDecl.get(i).getQualifiedName();
          if (succVars.contains(paramName)) {
            CFAUtils.getVariableNamesOfExpression(args.get(i)).copyInto(updatedVars);
            paramNames.add(paramName);
          }
        }

        for (String var : succVars) {
          if (!paramNames.contains(var)) {
            updatedVars.add(var);
          }
        }
        return;
      case FunctionReturnEdge:
        CFunctionReturnEdge funRet = ((CFunctionReturnEdge) edge);
        String varName;
        if (funRet.getSummaryEdge().getExpression() instanceof CFunctionCallAssignmentStatement) {
          varName =
              VarNameRetriever.getVarName(
                  ((CFunctionCallAssignmentStatement) funRet.getSummaryEdge().getExpression())
                      .getLeftHandSide());
          addAllExceptVar(varName, succVars, updatedVars);
          if (!funRet.getFunctionEntry().getReturnVariable().isPresent()) {
            throw new AssertionError("No return variable provided for non-void function.");
          }
          updatedVars.add(
              funRet.getFunctionEntry().getReturnVariable().orElseThrow().getQualifiedName());
        } else {
          updatedVars.addAll(succVars);
        }
        return;
      case CallToReturnEdge:
        throw new AssertionError();
      case AssumeEdge:
        Set<String> assumeVars =
            CFAUtils.getVariableNamesOfExpression(((CAssumeEdge) edge).getExpression()).toSet();
        for (String var : assumeVars) {
          if (succVars.contains(var)) {
            updatedVars.addAll(assumeVars);
            break;
          }
        }
        // $FALL-THROUGH$
      case BlankEdge:
        updatedVars.addAll(succVars);
        return;
      default:
        throw new AssertionError();
    }
  }

  private Collection<? extends String> getInitializerVars(CInitializer pInitializer) {
    if (pInitializer instanceof CDesignatedInitializer) {
      throw new AssertionError(
          "CDesignatedInitializer unsupported in slicing"); // currently not supported
    } else if (pInitializer instanceof CInitializerExpression) {
      return CFAUtils.getVariableNamesOfExpression(
              ((CInitializerExpression) pInitializer).getExpression())
          .toSet();
    } else { // CInitializerList
      Collection<String> result = new HashSet<>();

      for (CInitializer init : ((CInitializerList) pInitializer).getInitializers()) {
        result.addAll(getInitializerVars(init));
      }
      return result;
    }
  }

  private void addAllExceptVar(
      final String varName, final Set<String> toAdd, final Set<String> addTo) {
    for (String var : toAdd) {
      if (!var.equals(varName)) {
        addTo.add(var);
      }
    }
  }

  private void initializeStates(
      final ARGState root,
      final Map<ARGState, Set<String>> varMap,
      final Collection<ARGState> pWaitlist) {
    Deque<ARGState> waitlist = new ArrayDeque<>();
    Set<ARGState> visited = new HashSet<>();

    waitlist.add(root);
    visited.add(root);

    ARGState next;
    Set<String> init;
    while (!waitlist.isEmpty()) {
      next = waitlist.pop();

      if (!next.isCovered()) {
        numNotCovered++;
        init = initState(next);
        varMap.put(next, init);

        if (!init.isEmpty()) {
          pWaitlist.add(next);
        }
        updateCoveredNodes(next, init, varMap);

        for (ARGState child : next.getChildren()) {
          if (visited.add(child)) {
            waitlist.add(child);
          }
        }
      }
    }
  }

  private Set<String> initState(final ARGState parent) {
    assert !parent.isCovered();
    for (CFAEdge edge : CFAUtils.leavingEdges(AbstractStates.extractLocation(parent))) {

      if (edge.getEdgeType() == CFAEdgeType.AssumeEdge) {
        for (ARGState child : parent.getChildren()) {
          if (parent.getEdgeToChild(child) == edge) {
            continue;
          }
        }
        // assume edge not present
        return CFAUtils.getVariableNamesOfExpression(((CAssumeEdge) edge).getExpression()).toSet();
      }
    }

    return ImmutableSet.of();
  }

  private void updateCoveredNodes(
      ARGState pCovering, Set<String> varSet, Map<ARGState, Set<String>> pVarMap) {
    Deque<ARGState> waitlist = new ArrayDeque<>(pCovering.getCoveredByThis());

    ARGState covered;
    while (!waitlist.isEmpty()) {
      covered = waitlist.pop();
      if (pVarMap.get(covered) != varSet) {
        pVarMap.put(covered, varSet);
        waitlist.addAll(covered.getCoveredByThis());
      }
    }
  }

  private UnmodifiableReachedSet buildSlicedARG(
      final Map<ARGState, Set<String>> pVarMap,
      final UnmodifiableReachedSet pReached,
      final ConfigurableProgramAnalysis pCpa) {
    Map<ARGState, ARGState> oldToSliced = Maps.newHashMapWithExpectedSize(pVarMap.size());
    ARGState root = (ARGState) pReached.getFirstState();
    assert pVarMap.containsKey(root);

    for (Entry<ARGState, Set<String>> entry : pVarMap.entrySet()) {
      oldToSliced.put(entry.getKey(), getSlicedARGState(entry.getKey(), entry.getValue()));
    }

    for (Entry<ARGState, ARGState> entry : oldToSliced.entrySet()) {
      for (ARGState parent : entry.getKey().getParents()) {
        entry.getValue().addParent(oldToSliced.get(parent));
      }
      if (entry.getKey().isCovered()) {
        entry.getValue().setCovered(oldToSliced.get(entry.getKey().getCoveringState()));
      }
    }

    ReachedSet returnReached = reachedSetFactory.create(pCpa);
    // add root
    returnReached.add(oldToSliced.get(root), pReached.getPrecision(root));
    // add remaining elements
    for (Entry<ARGState, ARGState> entry : oldToSliced.entrySet()) {
      if (Objects.equals(entry.getKey(), root) && !entry.getKey().isCovered()) {
        returnReached.add(entry.getValue(), pReached.getPrecision(entry.getKey()));
      }
    }
    return returnReached;
  }

  private ARGState getSlicedARGState(ARGState unslicedState, Collection<String> necessaryVars) {
    List<AbstractState> compOldStates =
        ((CompositeState) unslicedState.getWrappedState()).getWrappedStates();
    List<AbstractState> newStates = new ArrayList<>(compOldStates.size());

    for (AbstractState state : compOldStates) {
      newStates.add(
          state instanceof ValueAnalysisState
              ? sliceState((ValueAnalysisState) state, necessaryVars)
              : state);
    }

    return new ARGState(new CompositeState(newStates), null);
  }

  private ValueAnalysisState sliceState(
      final ValueAnalysisState vState, final Collection<String> necessaryVars) {
    ValueAnalysisState returnState = ValueAnalysisState.copyOf(vState);

    for (MemoryLocation ml : vState.getTrackedMemoryLocations()) {
      if (!necessaryVars.contains(getVarName(ml))) {
        returnState.forget(ml);
      }
    }

    return returnState;
  }

  private String getVarName(final MemoryLocation pMl) {
    String prefix = pMl.isOnFunctionStack() ? pMl.getFunctionName() + "::" : "";
    return prefix + pMl.getIdentifier();
  }

  private static class VarNameRetriever implements CExpressionVisitor<String, NoException> {

    private static VarNameRetriever retriever = new VarNameRetriever();

    public static String getVarName(final CLeftHandSide lhsAssign) {
      return lhsAssign.accept(retriever);
    }

    @Override
    public String visit(CArraySubscriptExpression pIastArraySubscriptExpression) {
      return pIastArraySubscriptExpression.getArrayExpression().accept(this);
    }

    @Override
    public String visit(CFieldReference pIastFieldReference) {
      return pIastFieldReference.getFieldOwner().accept(this);
    }

    @Override
    public String visit(CIdExpression pIastIdExpression) {
      return pIastIdExpression.getDeclaration().getQualifiedName();
    }

    @Override
    public String visit(CPointerExpression pPointerExpression) {
      return pPointerExpression.getOperand().accept(this);
    }

    @Override
    public String visit(CComplexCastExpression pComplexCastExpression) {
      return pComplexCastExpression.getOperand().accept(this);
    }

    @Override
    public String visit(CBinaryExpression pIastBinaryExpression) {
      throw new AssertionError();
    }

    @Override
    public String visit(CCastExpression pIastCastExpression) {
      return pIastCastExpression.getOperand().accept(this);
    }

    @Override
    public String visit(CCharLiteralExpression pIastCharLiteralExpression) {
      throw new AssertionError();
    }

    @Override
    public String visit(CFloatLiteralExpression pIastFloatLiteralExpression) {
      throw new AssertionError();
    }

    @Override
    public String visit(CIntegerLiteralExpression pIastIntegerLiteralExpression) {
      throw new AssertionError();
    }

    @Override
    public String visit(CStringLiteralExpression pIastStringLiteralExpression) {
      throw new AssertionError();
    }

    @Override
    public String visit(CTypeIdExpression pIastTypeIdExpression) {
      throw new AssertionError(); // TODO assumption correct?
    }

    @Override
    public String visit(CUnaryExpression pIastUnaryExpression) {
      throw new AssertionError();
    }

    @Override
    public String visit(CImaginaryLiteralExpression PIastLiteralExpression) {
      throw new AssertionError();
    }

    @Override
    public String visit(CAddressOfLabelExpression pAddressOfLabelExpression) {
      throw new AssertionError();
    }
  }
}
