// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

/**
 * CFA metadata stores additional data about a CFA and may contain all data that isn't necessary for
 * the actual graph representation of a program.
 */
public final class CfaMetadata {

  private final MachineModel machineModel;
  private final Language cfaLanguage;
  private final Language inputLanguage;
  private final ImmutableList<Path> fileNames;
  private final FunctionEntryNode mainFunctionEntry;
  private final CfaConnectedness connectedness;

  /** The original {@link CFA} from the input program, in case this instance is transformed. */
  private final Optional<CFA> originalCfa;

  private final AstCfaRelation astCFARelation;
  private final @Nullable LoopStructure loopStructure;
  private final @Nullable VariableClassification variableClassification;
  private final @Nullable LiveVariables liveVariables;

  private CfaMetadata(
      MachineModel pMachineModel,
      Language pCFALanguage,
      Language pInputLanguage,
      List<Path> pFileNames,
      FunctionEntryNode pMainFunctionEntry,
      CfaConnectedness pConnectedness,
      Optional<CFA> pOriginalCfa,
      @Nullable AstCfaRelation pAstCfaRelation,
      @Nullable LoopStructure pLoopStructure,
      @Nullable VariableClassification pVariableClassification,
      @Nullable LiveVariables pLiveVariables) {

    machineModel = checkNotNull(pMachineModel);
    cfaLanguage = checkNotNull(pCFALanguage);
    inputLanguage = checkNotNull(pInputLanguage);
    fileNames = ImmutableList.copyOf(pFileNames);
    mainFunctionEntry = checkNotNull(pMainFunctionEntry);
    connectedness = checkNotNull(pConnectedness);

    originalCfa = pOriginalCfa;
    astCFARelation = pAstCfaRelation;
    loopStructure = pLoopStructure;
    variableClassification = pVariableClassification;
    liveVariables = pLiveVariables;
  }

  /**
   * Returns a new CFA metadata instance for the specified parameters (only mandatory attributes).
   *
   * <p>The returned CFA metadata instance doesn't contain any optional attributes.
   *
   * @param pMachineModel the machine model to use for CFA analysis (defines sizes for all basic
   *     types)
   * @param pCFALanguage the programming language of the CFA (e.g., C, Java, etc.)
   * @param pInputLanguage the input language of the CFA (e.g., C, Java, etc.)
   * @param pFileNames the source code files from which the CFA was created
   * @param pMainFunctionEntry the entry point of the program represented by the CFA
   * @param pConnectedness specifies whether the CFA is a supergraph
   * @return a new CFA metadata instance for the specified parameters
   * @throws NullPointerException if any parameter is {@code null} or if {@code pFileNames} contains
   *     {@code null}
   */
  public static CfaMetadata forMandatoryAttributes(
      MachineModel pMachineModel,
      Language pCFALanguage,
      Language pInputLanguage,
      List<Path> pFileNames,
      FunctionEntryNode pMainFunctionEntry,
      CfaConnectedness pConnectedness) {

    return new CfaMetadata(
        pMachineModel,
        pCFALanguage,
        pInputLanguage,
        pFileNames,
        pMainFunctionEntry,
        pConnectedness,
        Optional.empty(),
        null,
        null,
        null,
        null);
  }

  /**
   * Returns the machine model to use for CFA analysis.
   *
   * @return the machine model to use for CFA analysis (defines sizes for all basic types)
   */
  public MachineModel getMachineModel() {
    return machineModel;
  }

  /**
   * Returns a copy of this metadata instance, but with the specified machine model.
   *
   * @param pMachineModel the machine model to use for CFA analysis (defines sizes for all basic
   *     types)
   * @return a copy of this metadata instance, but with the specified machine model
   * @throws NullPointerException if {@code pMachineModel == null}
   */
  public CfaMetadata withMachineModel(MachineModel pMachineModel) {
    return new CfaMetadata(
        checkNotNull(pMachineModel),
        cfaLanguage,
        inputLanguage,
        fileNames,
        mainFunctionEntry,
        connectedness,
        originalCfa,
        astCFARelation,
        loopStructure,
        variableClassification,
        liveVariables);
  }

  /**
   * Returns the programming language of the CFA.
   *
   * @return the programming language of the CFA (e.g., C, Java, etc.)
   */
  public Language getLanguage() {
    return cfaLanguage;
  }

  /**
   * Returns the input language of the CFA.
   *
   * @return the input language of the CFA (e.g., C, Java, etc.)
   */
  public Language getInputLanguage() {
    return inputLanguage;
  }

  /**
   * Returns the source code files from which the CFA was created.
   *
   * @return the source code files from which the CFA was created
   */
  public ImmutableList<Path> getFileNames() {
    return fileNames;
  }

  /**
   * Returns the entry point of the program represented by the CFA.
   *
   * @return the entry point of the program represented by the CFA (i.e., function entry node of the
   *     main function)
   */
  public FunctionEntryNode getMainFunctionEntry() {
    return mainFunctionEntry;
  }

  /**
   * Returns a copy of this metadata instance, but with the specified program entry point.
   *
   * @param pMainFunctionEntry the program entry point (i.e., function entry node of the main
   *     function)
   * @return a copy of this metadata instance, but with the specified program entry point
   * @throws NullPointerException if {@code pMainFunctionEntry == null}
   */
  public CfaMetadata withMainFunctionEntry(FunctionEntryNode pMainFunctionEntry) {
    return new CfaMetadata(
        machineModel,
        cfaLanguage,
        inputLanguage,
        fileNames,
        checkNotNull(pMainFunctionEntry),
        connectedness,
        originalCfa,
        astCFARelation,
        loopStructure,
        variableClassification,
        liveVariables);
  }

  /**
   * Returns the connectedness of the CFA which indicates whether the CFA is a supergraph.
   *
   * @return the connectedness of the CFA which indicates whether the CFA is a supergraph
   */
  public CfaConnectedness getConnectedness() {
    return connectedness;
  }

  /**
   * Returns a copy of this metadata instance, but with the specified CFA connectedness.
   *
   * @param pConnectedness the CFA connectedness that indicates whether the CFA is a supergraph
   * @return a copy of this metadata instance, but with the specified CFA connectedness
   * @throws NullPointerException if {@code pConnectedness == null}
   */
  public CfaMetadata withConnectedness(CfaConnectedness pConnectedness) {
    return new CfaMetadata(
        machineModel,
        cfaLanguage,
        inputLanguage,
        fileNames,
        mainFunctionEntry,
        checkNotNull(pConnectedness),
        originalCfa,
        astCFARelation,
        loopStructure,
        variableClassification,
        liveVariables);
  }

  public Optional<CFA> getOriginalCfa() {
    return originalCfa;
  }

  public CfaMetadata withOriginalCfa(CFA pOriginalCfa) {
    return new CfaMetadata(
        machineModel,
        cfaLanguage,
        inputLanguage,
        fileNames,
        mainFunctionEntry,
        connectedness,
        Optional.of(checkNotNull(pOriginalCfa)),
        astCFARelation,
        loopStructure,
        variableClassification,
        liveVariables);
  }

  /**
   * Returns the relation between the AST and the CFA, if it's stored in this metadata instance.
   *
   * @return If this metadata instance contains the AST structure for the CFA, an optional
   *     containing the AST structure is returned. Otherwise, if this metadata instance does not
   *     contain the AST structure for the CFA, an empty optional is returned.
   */
  public AstCfaRelation getAstCfaRelation() {
    return astCFARelation;
  }

  /**
   * Returns the loop structure for the CFA, if it's stored in this metadata instance.
   *
   * @return If this metadata instance contains the loop structure for the CFA, an optional
   *     containing the loop structure is returned. Otherwise, if this metadata instance doesn't
   *     contain the loop structure for the CFA, an empty optional is returned.
   */
  public Optional<LoopStructure> getLoopStructure() {
    return Optional.ofNullable(loopStructure);
  }

  /**
   * Returns a copy of this metadata instance, but with the specified AST structure.
   *
   * @param pAstCfaRelation the AST structure to store in the returned metadata instance (use {@code
   *     null} to create an instance without AST structure)
   * @return a copy of this metadata instance, but with the specified AST structure
   */
  public CfaMetadata withAstCfaRelation(@Nullable AstCfaRelation pAstCfaRelation) {
    return new CfaMetadata(
        machineModel,
        cfaLanguage,
        inputLanguage,
        fileNames,
        mainFunctionEntry,
        connectedness,
        originalCfa,
        pAstCfaRelation,
        loopStructure,
        variableClassification,
        liveVariables);
  }

  /**
   * Returns a copy of this metadata instance, but with the specified loop structure.
   *
   * @param pLoopStructure the loop structure to store in the returned metadata instance (use {@code
   *     null} to create an instance without loop structure)
   * @return a copy of this metadata instance, but with the specified loop structure
   */
  public CfaMetadata withLoopStructure(@Nullable LoopStructure pLoopStructure) {
    return new CfaMetadata(
        machineModel,
        cfaLanguage,
        inputLanguage,
        fileNames,
        mainFunctionEntry,
        connectedness,
        originalCfa,
        astCFARelation,
        pLoopStructure,
        variableClassification,
        liveVariables);
  }

  /**
   * Returns the variable classification for the CFA, if it's stored in this metadata instance.
   *
   * @return If this metadata instance contains the variable classification for the CFA, an optional
   *     containing the variable classification is returned. Otherwise, if this metadata instance
   *     doesn't contain the variable classification for the CFA, an empty optional is returned.
   */
  public Optional<VariableClassification> getVariableClassification() {
    return Optional.ofNullable(variableClassification);
  }

  /**
   * Returns a copy of this metadata instance, but with the specified variable classification.
   *
   * @param pVariableClassification the variable classification to store in the returned metadata
   *     instance (use {@code null} to create an instance without variable classification)
   * @return a copy of this metadata instance, but with the specified variable classification
   */
  public CfaMetadata withVariableClassification(
      @Nullable VariableClassification pVariableClassification) {
    return new CfaMetadata(
        machineModel,
        cfaLanguage,
        inputLanguage,
        fileNames,
        mainFunctionEntry,
        connectedness,
        originalCfa,
        astCFARelation,
        loopStructure,
        pVariableClassification,
        liveVariables);
  }

  /**
   * Returns the live variables for the CFA, if the information is stored in this metadata instance.
   *
   * @return If this metadata instance contains the live variables for the CFA, an optional
   *     containing the live variables is returned. Otherwise, if this metadata instance doesn't
   *     contain the live variables for the CFA, an empty optional is returned.
   */
  public Optional<LiveVariables> getLiveVariables() {
    return Optional.ofNullable(liveVariables);
  }

  /**
   * Returns a copy of this metadata instance, but with the specified live variables.
   *
   * @param pLiveVariables the live variables to store in the returned metadata instance (use {@code
   *     null} to create an instance without live variables)
   * @return a copy of this metadata instance, but with the specified {@link LiveVariables}
   */
  public CfaMetadata withLiveVariables(@Nullable LiveVariables pLiveVariables) {
    return new CfaMetadata(
        machineModel,
        cfaLanguage,
        inputLanguage,
        fileNames,
        mainFunctionEntry,
        connectedness,
        originalCfa,
        astCFARelation,
        loopStructure,
        variableClassification,
        pLiveVariables);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        machineModel,
        cfaLanguage,
        inputLanguage,
        fileNames,
        mainFunctionEntry,
        connectedness,
        loopStructure,
        variableClassification,
        liveVariables);
  }

  @Override
  public boolean equals(Object pObject) {
    if (this == pObject) {
      return true;
    }
    return pObject instanceof CfaMetadata other
        && machineModel == other.machineModel
        && cfaLanguage == other.cfaLanguage
        && inputLanguage == other.inputLanguage
        && Objects.equals(fileNames, other.fileNames)
        && Objects.equals(mainFunctionEntry, other.mainFunctionEntry)
        && connectedness == other.connectedness
        && Objects.equals(loopStructure, other.loopStructure)
        && Objects.equals(variableClassification, other.variableClassification)
        && Objects.equals(liveVariables, other.liveVariables);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("machineModel", machineModel)
        .add("language", cfaLanguage)
        .add("inputLanguage", inputLanguage)
        .add("fileNames", fileNames)
        .add("mainFunctionEntry", mainFunctionEntry)
        .add("connectedness", connectedness)
        .add("loopStructure", loopStructure)
        .add("variableClassification", variableClassification)
        .add("liveVariables", liveVariables)
        .toString();
  }
}
