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
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.AcslMetadata;
import org.sosy_lab.cpachecker.cfa.ast.acslDeprecated.ACSLAnnotation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibCfaMetadata;
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

  private final AstCfaRelation astCFARelation;
  private final @Nullable LoopStructure loopStructure;
  private final @Nullable VariableClassification variableClassification;
  private final @Nullable LiveVariables liveVariables;
  private final @Nullable ImmutableListMultimap<CFAEdge, ACSLAnnotation> edgesToAnnotations;
  private final @Nullable AcslMetadata acslMetadata;
  private final @Nullable SvLibCfaMetadata svLibCfaMetadata;

  private final @Nullable CfaTransformationMetadata transformationMetadata;

  private CfaMetadata(
      MachineModel pMachineModel,
      Language pCFALanguage,
      Language pInputLanguage,
      List<Path> pFileNames,
      FunctionEntryNode pMainFunctionEntry,
      CfaConnectedness pConnectedness,
      @Nullable AstCfaRelation pAstCfaRelation,
      @Nullable LoopStructure pLoopStructure,
      @Nullable VariableClassification pVariableClassification,
      @Nullable LiveVariables pLiveVariables,
      @Nullable ImmutableListMultimap<CFAEdge, ACSLAnnotation> pEdgesToAnnotations,
      @Nullable SvLibCfaMetadata pSvLibCfaMetadata,
      @Nullable CfaTransformationMetadata pCfaTransformationMetadata,
      @Nullable AcslMetadata pAcslMetadata) {
    machineModel = checkNotNull(pMachineModel);
    cfaLanguage = checkNotNull(pCFALanguage);
    inputLanguage = checkNotNull(pInputLanguage);
    fileNames = ImmutableList.copyOf(pFileNames);
    mainFunctionEntry = checkNotNull(pMainFunctionEntry);
    connectedness = checkNotNull(pConnectedness);

    astCFARelation = pAstCfaRelation;
    loopStructure = pLoopStructure;
    variableClassification = pVariableClassification;
    liveVariables = pLiveVariables;
    edgesToAnnotations = pEdgesToAnnotations;
    svLibCfaMetadata = pSvLibCfaMetadata;
    transformationMetadata = pCfaTransformationMetadata;
    acslMetadata = pAcslMetadata;
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
        null,
        null,
        null,
        null,
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
        astCFARelation,
        loopStructure,
        variableClassification,
        liveVariables,
        edgesToAnnotations,
        svLibCfaMetadata,
        transformationMetadata,
        acslMetadata);
  }

  public CfaMetadata withTransformationMetadata(CfaTransformationMetadata pTransformationMetadata) {
    CfaMetadata newMetadata =
        new CfaMetadata(
            machineModel,
            cfaLanguage,
            inputLanguage,
            fileNames,
            mainFunctionEntry,
            connectedness,
            astCFARelation,
            loopStructure,
            variableClassification,
            liveVariables,
            edgesToAnnotations,
            svLibCfaMetadata,
            pTransformationMetadata,
            acslMetadata);
    return newMetadata;
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

  public @Nullable CfaTransformationMetadata getTransformationMetadata() {
    return transformationMetadata;
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
        astCFARelation,
        loopStructure,
        variableClassification,
        liveVariables,
        edgesToAnnotations,
        svLibCfaMetadata,
        transformationMetadata,
        acslMetadata);
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
        astCFARelation,
        loopStructure,
        variableClassification,
        liveVariables,
        edgesToAnnotations,
        svLibCfaMetadata,
        transformationMetadata,
        acslMetadata);
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
        pAstCfaRelation,
        loopStructure,
        variableClassification,
        liveVariables,
        edgesToAnnotations,
        svLibCfaMetadata,
        transformationMetadata,
        acslMetadata);
  }

  /**
   * Returns the SV-LIB specific CFA metadata, if it's stored in this metadata instance.
   *
   * @return If this metadata instance contains the SV-LIB-specific CFA metadata, an optional
   *     containing the SV-LIB-specific CFA metadata is returned. Otherwise, if this metadata
   *     instance doesn't contain the SV-LIB-specific CFA metadata, an empty optional is returned.
   */
  public Optional<SvLibCfaMetadata> getSvLibCfaMetadata() {
    return Optional.ofNullable(svLibCfaMetadata);
  }

  /**
   * Returns a copy of this metadata instance, but with the specified SvLibCfaMetadata.
   *
   * @param pSvLibCfaMetadata the SvLibCfaMetadata to store in the returned metadata instance (use
   *     {@code null} to create an instance without SvLibCfaMetadata)
   * @return a copy of this metadata instance, but with the specified AST structure
   */
  public CfaMetadata withSvLibCfaMetadata(@Nullable SvLibCfaMetadata pSvLibCfaMetadata) {
    Preconditions.checkArgument(
        inputLanguage == Language.SVLIB ? pSvLibCfaMetadata != null : pSvLibCfaMetadata == null);
    return new CfaMetadata(
        machineModel,
        cfaLanguage,
        inputLanguage,
        fileNames,
        mainFunctionEntry,
        connectedness,
        astCFARelation,
        loopStructure,
        variableClassification,
        liveVariables,
        edgesToAnnotations,
        pSvLibCfaMetadata,
        transformationMetadata,
        acslMetadata);
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
        astCFARelation,
        pLoopStructure,
        variableClassification,
        liveVariables,
        edgesToAnnotations,
        svLibCfaMetadata,
        transformationMetadata,
        acslMetadata);
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
        astCFARelation,
        loopStructure,
        pVariableClassification,
        liveVariables,
        edgesToAnnotations,
        svLibCfaMetadata,
        transformationMetadata,
        acslMetadata);
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
        astCFARelation,
        loopStructure,
        variableClassification,
        pLiveVariables,
        edgesToAnnotations,
        svLibCfaMetadata,
        transformationMetadata,
        acslMetadata);
  }

  /**
   * Returns the map from edges to ACSL annotations for the CFA, if the information is stored in
   * this metadata instance.
   *
   * @return If this metadata instance contains the map from edges to ACSL annotations for the CFA,
   *     an optional containing the map is returned. Otherwise, if this metadata instance doesn't
   *     contain the map for the CFA, an empty optional is returned.
   */
  public Optional<ImmutableListMultimap<CFAEdge, ACSLAnnotation>> getEdgesToAnnotations() {
    return Optional.ofNullable(edgesToAnnotations);
  }

  /**
   * Returns a copy of this metadata instance, but with the specified map from edges to ACSL
   * annotations.
   *
   * @param pedgesToAnnotations the map to store in the returned metadata instance (use {@code null}
   *     to create an instance without map)
   * @return a copy of this metadata instance, but with the specified map from edges to ACSL
   *     annotations
   */
  public CfaMetadata withEdgesToAnnotations(
      @Nullable ImmutableListMultimap<CFAEdge, ACSLAnnotation> pedgesToAnnotations) {
    return new CfaMetadata(
        machineModel,
        cfaLanguage,
        inputLanguage,
        fileNames,
        mainFunctionEntry,
        connectedness,
        astCFARelation,
        loopStructure,
        variableClassification,
        liveVariables,
        pedgesToAnnotations,
        svLibCfaMetadata,
        transformationMetadata,
        acslMetadata);
  }

  /**
   * Returns a copy of this metadata instance but with the specified Acsl Metadata.
   *
   * @param pAcslMetadata A recored of Acsl annotations to store in the returned metadata instance
   *     (use {@code null} to create an instance without map)
   * @return A copy of this metadata instance but with the specified Acsl Metadata
   */
  public CfaMetadata withAcslMetadata(@Nullable AcslMetadata pAcslMetadata) {
    return new CfaMetadata(
        machineModel,
        cfaLanguage,
        inputLanguage,
        fileNames,
        mainFunctionEntry,
        connectedness,
        astCFARelation,
        loopStructure,
        variableClassification,
        liveVariables,
        edgesToAnnotations,
        svLibCfaMetadata,
        transformationMetadata,
        pAcslMetadata);
  }

  public AcslMetadata getAcslMetadata() {
    return acslMetadata;
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
        liveVariables,
        edgesToAnnotations,
        svLibCfaMetadata,
        transformationMetadata);
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
        && Objects.equals(liveVariables, other.liveVariables)
        && Objects.equals(edgesToAnnotations, other.edgesToAnnotations)
        && Objects.equals(astCFARelation, other.astCFARelation)
        && Objects.equals(svLibCfaMetadata, other.svLibCfaMetadata)
        && Objects.equals(transformationMetadata, other.transformationMetadata);
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
        .add("edgesToAnnotations", edgesToAnnotations)
        .add("transformationMetadata", transformationMetadata)
        .toString();
  }
}
