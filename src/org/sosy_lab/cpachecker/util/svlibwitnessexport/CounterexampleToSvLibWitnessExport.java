// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.svlibwitnessexport;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibChoiceStep;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibHavocStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibHavocVariablesStep;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIncorrectTagProperty;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibLocalVariablesStep;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibProcedureCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSelectTraceCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTagProperty;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTrace;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTraceEntryCall;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTraceSetGlobalVariable;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTraceStep;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibType;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibViolatedProperty;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibBlankChoiceEdge;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibCfaMetadata;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibProcedureCallEdge;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibStatementEdge;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteState;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.ConcreteStatePathNode;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.SingleConcreteState;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.counterexample.IDExpression;

public class CounterexampleToSvLibWitnessExport {
  @SuppressWarnings("unused")
  private final LogManager logger;

  private final SvLibCfaMetadata svLibMetadata;

  public CounterexampleToSvLibWitnessExport(LogManager pLogger, CFA pCFA) {
    Verify.verify(
        pCFA.getMetadata().getSvLibCfaMetadata().isPresent(),
        "SV-LIB metadata must be present in CFA in order to export a SV-LIB witness.");
    svLibMetadata = pCFA.getMetadata().getSvLibCfaMetadata().orElseThrow();
    logger = pLogger;
  }

  private SvLibViolatedProperty getViolatedProperty(CFAEdge pEdge) {
    CFANode nodeWithViolatedTag = pEdge.getPredecessor();
    Set<SvLibTagProperty> setOfPropertiesAtLeastOneViolated =
        svLibMetadata.tagAnnotations().get(nodeWithViolatedTag);
    Set<SvLibTagProperty> violatedTags;
    // Find the actual violated tags.
    // TODO: Currently we do not handle the case where more than one tag is present.
    if (setOfPropertiesAtLeastOneViolated.size() != 1) {
      throw new UnsupportedOperationException(
          "SV-LIB witness export currently only supports a single violated property tag at a"
              + " CFANode.");
    } else {
      violatedTags = setOfPropertiesAtLeastOneViolated;
    }

    Set<SvLibTagReference> tagReferences = svLibMetadata.tagReferences().get(nodeWithViolatedTag);
    if (tagReferences.size() == 1) {
      // If we know exactly which tag was violated, include it in the property.
      SvLibTagReference tagRef = tagReferences.iterator().next();
      return new SvLibIncorrectTagProperty(tagRef.getFileLocation(), tagRef, violatedTags);
    } else if (tagReferences.isEmpty()) {
      // Otherwise, just return the violated tags without a specific reference.
      return new SvLibIncorrectTagProperty(FileLocation.DUMMY, violatedTags);
    } else {
      // TODO: There can be multiple tag references, and only one of them could have the actual
      //  property, so we need to be able to identify the correct one.
      throw new UnsupportedOperationException(
          "SV-LIB witness export currently only supports a single violated property tag reference"
              + " at a CFANode.");
    }
  }

  private SvLibConstantTerm getValueOrDefault(
      ConcreteState pState, String pVarName, @Nullable String pProcedureName, SvLibType pType) {
    // Variable declarations can only be global
    IDExpression idExpr = new IDExpression(pVarName, pProcedureName);
    if (pState.hasValueForLeftHandSide(idExpr)) {
      // Transform the value into a SvLibConstantTerm
      Object value = pState.getVariableValue(idExpr);
      return SvLibConstantTerm.of(value, pType);
    } else {
      // Return a default value for the variable type
      // not all variables have necessarily been added to the SMT
      // solver (e.g. unused variables), so we need to return
      // a default value here.
      return pType.defaultValue();
    }
  }

  private SvLibLocalVariablesStep setLocalVariablesForFunctionCall(
      ConcreteState pConcreteState, SvLibProcedureDeclaration pProcedureDeclaration) {
    ImmutableMap.Builder<SvLibIdTerm, SvLibConstantTerm> functionCallAssignmentsBuilder =
        ImmutableMap.builder();
    for (SvLibParameterDeclaration paramDecl :
        FluentIterable.concat(
            pProcedureDeclaration.getParameters(),
            pProcedureDeclaration.getLocalVariables(),
            pProcedureDeclaration.getReturnValues())) {

      SvLibConstantTerm assignedValue =
          getValueOrDefault(
              pConcreteState,
              paramDecl.getName(),
              paramDecl.getProcedureName(),
              paramDecl.getType());

      SvLibIdTerm idTerm = new SvLibIdTerm(paramDecl, FileLocation.DUMMY);
      functionCallAssignmentsBuilder.put(idTerm, assignedValue);
    }
    return new SvLibLocalVariablesStep(
        functionCallAssignmentsBuilder.buildOrThrow(), FileLocation.DUMMY);
  }

  private SvLibHavocVariablesStep setHavocVariablesForFunctionCall(
      ConcreteState pConcreteState, SvLibHavocStatement pStatement) {
    ImmutableMap.Builder<SvLibIdTerm, SvLibConstantTerm> havocedVariablesBuilder =
        ImmutableMap.builder();
    for (SvLibSimpleDeclaration varTerm : pStatement.getVariables()) {
      SvLibConstantTerm assignedValue =
          getValueOrDefault(
              pConcreteState, varTerm.getName(), varTerm.getProcedureName(), varTerm.getType());

      SvLibIdTerm idTerm = new SvLibIdTerm(varTerm, FileLocation.DUMMY);
      havocedVariablesBuilder.put(idTerm, assignedValue);
    }
    return new SvLibHavocVariablesStep(havocedVariablesBuilder.build(), FileLocation.DUMMY);
  }

  private SvLibTraceSetGlobalVariable handleGlobalVariableAssignment(
      ConcreteState pConcreteState, SvLibDeclaration pDeclaration) {
    checkState(
        (pDeclaration instanceof SvLibVariableDeclaration),
        "Expected only variable or constant declarations in the global declaration phase.");

    SvLibVariableDeclaration pVarDecl = (SvLibVariableDeclaration) pDeclaration;
    SvLibConstantTerm assignedValue =
        getValueOrDefault(pConcreteState, pVarDecl.getName(), null, pVarDecl.getType());

    return new SvLibTraceSetGlobalVariable(
        new SvLibIdTerm(pVarDecl, FileLocation.DUMMY), assignedValue, FileLocation.DUMMY);
  }

  private SvLibTraceEntryCall handleTraceEntryCall(
      ConcreteState pConcreteState, SvLibProcedureCallEdge pCallEdge) {
    SvLibProcedureCallStatement procedureCallStatement = pCallEdge.getFunctionCall();

    // Now build the argument values
    ImmutableList.Builder<SvLibConstantTerm> argumentValuesBuilder = ImmutableList.builder();
    for (SvLibTerm argumentExpr : procedureCallStatement.getParameterExpressions()) {
      if (argumentExpr instanceof SvLibIdTerm pIdTerm
          && pIdTerm.getDeclaration() instanceof SvLibVariableDeclaration varDecl) {
        SvLibConstantTerm argValue =
            getValueOrDefault(pConcreteState, varDecl.getName(), null, varDecl.getType());
        argumentValuesBuilder.add(argValue);
      } else if (argumentExpr instanceof SvLibConstantTerm pConstTerm) {
        argumentValuesBuilder.add(pConstTerm);
      } else {
        // More complex expressions are currently not supported.
        // They would require expression evaluation here.
        throw new UnsupportedOperationException(
            "Only ID terms and constant terms are supported as procedure call arguments in SV-LIB"
                + " witness export.");
      }
    }

    return new SvLibTraceEntryCall(
        procedureCallStatement.getProcedureDeclaration(),
        argumentValuesBuilder.build(),
        pCallEdge.getFileLocation());
  }

  // We need to suppress the warnings, because error-prone says that the continue for the BlankEdge
  // is misleading, and when I remove it, IntelliJ complains about an empty if body. So I keep it as
  // is.
  @SuppressWarnings("RedundantControlFlow")
  public List<SvLibCommand> generateWitnessCommands(CounterexampleInfo pCounterexample) {
    ConcreteStatePath concretePath =
        pCounterexample.getCFAPathWithAssignments().getConcreteStatePath().orElseThrow();
    ImmutableList.Builder<SvLibTraceSetGlobalVariable> globalVariableAssignmentBuilder =
        ImmutableList.builder();
    @Nullable SvLibTraceEntryCall entryCall = null;
    ImmutableList.Builder<SvLibTraceStep> stepsBuilder = ImmutableList.builder();
    @Nullable SvLibViolatedProperty violatedProperty = null;

    boolean inGlobalDeclarationPhase = true;

    int stateIndex = 0;
    for (ConcreteStatePathNode state : concretePath) {
      stateIndex++;

      if (!(state instanceof SingleConcreteState pSingleConcreteState)) {
        // Currently this is guaranteed by the type system, so the check is redundant.
        // However, in the future we might have other implementations of ConcreteStatePathNode,
        // so we keep this check here for safety.
        throw new UnsupportedOperationException(
            "Only single concrete states are supported in SV-LIB witness export.");
      }

      ConcreteState concreteState = pSingleConcreteState.getConcreteState();
      CFAEdge edge = pSingleConcreteState.getCfaEdge();

      if (stateIndex == concretePath.size()) {
        // If we get to the last state in the path, we need to handle the violated property.
        violatedProperty = getViolatedProperty(edge);
      } else if (edge instanceof BlankEdge && !(edge instanceof SvLibBlankChoiceEdge)) {
        // Blank edges do not contribute to the witness trace.
        continue;
      } else if (inGlobalDeclarationPhase && edge instanceof SvLibProcedureCallEdge pCallEdge) {
        // The first procedure call edge is the entry call.
        inGlobalDeclarationPhase = false;
        entryCall = handleTraceEntryCall(concreteState, pCallEdge);
        stepsBuilder.add(
            setLocalVariablesForFunctionCall(
                concreteState, pCallEdge.getFunctionCall().getProcedureDeclaration()));
      } else if (inGlobalDeclarationPhase) {
        // Handle all the global declarations
        checkState(
            (edge instanceof SvLibDeclarationEdge),
            "Expected global declaration edges in the global declaration phase of SV-LIB witness"
                + " export.");

        globalVariableAssignmentBuilder.add(
            handleGlobalVariableAssignment(
                concreteState, ((SvLibDeclarationEdge) edge).getDeclaration()));

      } else if (edge instanceof SvLibProcedureCallEdge pCallEdge) {
        // Initialize all the local variables of the called procedure
        stepsBuilder.add(
            setLocalVariablesForFunctionCall(
                concreteState, pCallEdge.getFunctionCall().getProcedureDeclaration()));
      } else if (edge instanceof SvLibStatementEdge pStatementEdge
          && pStatementEdge.getStatement() instanceof SvLibHavocStatement pSvLibHavocStatement) {
        // Handle havoc statements as trace steps
        stepsBuilder.add(setHavocVariablesForFunctionCall(concreteState, pSvLibHavocStatement));
      } else if (edge instanceof SvLibBlankChoiceEdge pChoiceEdge) {
        // Handle choice statements as trace steps
        stepsBuilder.add(new SvLibChoiceStep(pChoiceEdge.getChoiceIndex(), FileLocation.DUMMY));
      }

      // TODO: Handle leap commands, once CPAchecker can verify programs by using abstractions.
      // In all other cases, we do not need to do anything special for the witness export.
    }

    Verify.verify(entryCall != null, "Entry call must be set in SV-LIB witness export.");
    Verify.verify(
        violatedProperty != null, "Violated property must be set in SV-LIB witness export.");

    SvLibTrace trace =
        new SvLibTrace(
            globalVariableAssignmentBuilder.build(),
            entryCall,
            stepsBuilder.build(),
            violatedProperty,
            // TODO: Currently we do not handle anything other than safety properties,
            //  so no tags are necessary.
            ImmutableList.of(),
            FileLocation.DUMMY);

    return ImmutableList.of(new SvLibSelectTraceCommand(trace, FileLocation.DUMMY));
  }
}
