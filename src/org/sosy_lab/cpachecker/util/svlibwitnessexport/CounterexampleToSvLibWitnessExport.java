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
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagProperty;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibBlankChoiceEdge;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibCfaMetadata;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSimpleParsingDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibSelectTraceCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibHavocStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.trace.SmtLibModel;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.trace.SvLibChoiceStep;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.trace.SvLibHavocVariablesStep;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.trace.SvLibIncorrectTagProperty;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.trace.SvLibInitProcVariablesStep;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.trace.SvLibTrace;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.trace.SvLibTraceEntryProcedure;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.trace.SvLibTraceSetGlobalVariable;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.trace.SvLibTraceStep;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.trace.SvLibViolatedProperty;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;
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

  private Optional<SvLibConstantTerm> getValue(
      ConcreteState pState, String pVarName, @Nullable String pProcedureName, SvLibType pType) {
    // Variable declarations can only be global
    IDExpression idExpr = new IDExpression(pVarName, pProcedureName);
    if (pState.hasValueForLeftHandSide(idExpr)) {
      // Transform the value into a SvLibConstantTerm
      Object value = pState.getVariableValue(idExpr);
      return Optional.of(SvLibConstantTerm.of(value, pType));
    }

    return Optional.empty();
  }

  private SvLibInitProcVariablesStep setLocalVariablesForFunctionCall(
      ConcreteState pConcreteState, SvLibProcedureDeclaration pProcedureDeclaration) {
    ImmutableMap.Builder<SvLibIdTerm, SvLibConstantTerm> functionCallAssignmentsBuilder =
        ImmutableMap.builder();
    for (SvLibSimpleParsingDeclaration paramDecl :
        FluentIterable.concat(
            pProcedureDeclaration.getLocalVariables(), pProcedureDeclaration.getReturnValues())) {

      Optional<SvLibConstantTerm> assignedValue =
          getValue(
              pConcreteState,
              paramDecl.toASTString(),
              pProcedureDeclaration.getName(),
              paramDecl.getType());

      SvLibIdTerm idTerm = new SvLibIdTerm(paramDecl.toSimpleDeclaration(), FileLocation.DUMMY);
      if (assignedValue.isPresent()) {
        functionCallAssignmentsBuilder.put(idTerm, assignedValue.orElseThrow());
      }
    }
    return new SvLibInitProcVariablesStep(
        pProcedureDeclaration, functionCallAssignmentsBuilder.buildOrThrow(), FileLocation.DUMMY);
  }

  private SvLibHavocVariablesStep setHavocVariablesForFunctionCall(
      ConcreteState pConcreteState, SvLibHavocStatement pStatement) {
    ImmutableMap.Builder<SvLibIdTerm, SvLibConstantTerm> havocedVariablesBuilder =
        ImmutableMap.builder();
    for (SvLibSimpleParsingDeclaration varTerm : pStatement.getVariables()) {
      Optional<SvLibConstantTerm> assignedValue =
          getValue(
              pConcreteState, varTerm.toASTString(), varTerm.getProcedureName(), varTerm.getType());

      SvLibIdTerm idTerm = new SvLibIdTerm(varTerm.toSimpleDeclaration(), FileLocation.DUMMY);
      if (assignedValue.isPresent()) {
        havocedVariablesBuilder.put(idTerm, assignedValue.orElseThrow());
      }
    }
    return new SvLibHavocVariablesStep(havocedVariablesBuilder.buildOrThrow(), FileLocation.DUMMY);
  }

  private Optional<SvLibTraceSetGlobalVariable> handleGlobalVariableAssignment(
      ConcreteState pConcreteState, SvLibDeclaration pDeclaration) {
    checkState(
        (pDeclaration instanceof SvLibVariableDeclaration),
        "Expected only variable or constant declarations in the global declaration phase.");

    SvLibVariableDeclaration pVarDecl = (SvLibVariableDeclaration) pDeclaration;
    Optional<SvLibConstantTerm> assignedValue =
        getValue(pConcreteState, pVarDecl.getName(), null, pVarDecl.getType());

    if (assignedValue.isPresent()) {
      return Optional.of(
          new SvLibTraceSetGlobalVariable(
              new SvLibIdTerm(pVarDecl, FileLocation.DUMMY),
              assignedValue.orElseThrow(),
              FileLocation.DUMMY));
    }
    return Optional.empty();
  }

  public List<SvLibCommand> generateWitnessCommands(CounterexampleInfo pCounterexample) {
    ConcreteStatePath concretePath =
        pCounterexample.getCFAPathWithAssignments().getConcreteStatePath().orElseThrow();
    ImmutableList.Builder<SvLibTraceSetGlobalVariable> globalVariableAssignmentBuilder =
        ImmutableList.builder();
    @Nullable SvLibTraceEntryProcedure entryCall = null;
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
      } else //noinspection StatementWithEmptyBody
      if (edge instanceof BlankEdge && !(edge instanceof SvLibBlankChoiceEdge)) {
        // Blank edges do not contribute to the witness trace.
      } else if (inGlobalDeclarationPhase && edge instanceof SvLibFunctionCallEdge pCallEdge) {
        // The first procedure call edge is the entry call.

        inGlobalDeclarationPhase = false;
        SvLibFunctionDeclaration functionDeclaration =
            pCallEdge.getFunctionCall().getFunctionCallExpression().getDeclaration();
        SvLibProcedureDeclaration procedureDeclaration =
            svLibMetadata.functionToProcedureDeclaration().get(functionDeclaration);
        entryCall = new SvLibTraceEntryProcedure(procedureDeclaration, pCallEdge.getFileLocation());
      } else if (inGlobalDeclarationPhase) {
        // Handle all the global declarations
        checkState(
            (edge instanceof SvLibDeclarationEdge),
            "Expected global declaration edges in the global declaration phase of SV-LIB witness"
                + " export.");

        Optional<SvLibTraceSetGlobalVariable> globalVarAssignment =
            handleGlobalVariableAssignment(
                concreteState, ((SvLibDeclarationEdge) edge).getDeclaration());

        if (globalVarAssignment.isPresent()) {
          globalVariableAssignmentBuilder.add(globalVarAssignment.orElseThrow());
        }

      } else if (svLibMetadata.nodesToActualHavocStatementEnd().containsKey(edge.getSuccessor())) {
        // Handle havoc statements, which are split into single assignments in the CFA
        // so we take the last one for the witness export, in order to have all the information
        // about the values.
        SvLibHavocStatement pSvLibHavocStatement =
            svLibMetadata.nodesToActualHavocStatementEnd().get(edge.getSuccessor());
        stepsBuilder.add(setHavocVariablesForFunctionCall(concreteState, pSvLibHavocStatement));
      } else if (svLibMetadata
          .nodesToActualProcedureDefinitionEnd()
          .containsKey(edge.getSuccessor())) {
        SvLibProcedureDeclaration procedureDeclaration =
            svLibMetadata.nodesToActualProcedureDefinitionEnd().get(edge.getSuccessor());
        stepsBuilder.add(setLocalVariablesForFunctionCall(concreteState, procedureDeclaration));
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
            new SmtLibModel(
                ImmutableList.of(), ImmutableList.of(), ImmutableList.of(), FileLocation.DUMMY),
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
