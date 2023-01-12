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
import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

/**
 * CFA metadata stores additional data about a CFA and may contain all data that isn't necessary for
 * the actual graph representation of a program.
 */
public final class CfaMetadata implements Serializable {

  private static final long serialVersionUID = -4976424764995656485L;

  private final MachineModel machineModel;
  private final Language language;
  // `fileNames` isn't `final` due to serialization, but shouldn't be reassigned anywhere else
  private transient ImmutableList<Path> fileNames;
  private final FunctionEntryNode mainFunctionEntry;
  private final CfaConnectedness connectedness;

  private final @Nullable LoopStructure loopStructure;
  private final @Nullable VariableClassification variableClassification;
  private final @Nullable LiveVariables liveVariables;

  private CfaMetadata(
      MachineModel pMachineModel,
      Language pLanguage,
      ImmutableList<Path> pFileNames,
      FunctionEntryNode pMainFunctionEntry,
      CfaConnectedness pConnectedness,
      @Nullable LoopStructure pLoopStructure,
      @Nullable VariableClassification pVariableClassification,
      @Nullable LiveVariables pLiveVariables) {

    machineModel = pMachineModel;
    language = pLanguage;
    fileNames = pFileNames;
    mainFunctionEntry = pMainFunctionEntry;
    connectedness = pConnectedness;

    loopStructure = pLoopStructure;
    variableClassification = pVariableClassification;
    liveVariables = pLiveVariables;
  }

  /**
   * Returns a new CFA metadata instance for the specified parameters.
   *
   * @param pMachineModel the machine model to use for CFA analysis (defines sizes for all basic
   *     types)
   * @param pLanguage the programming language of the CFA (e.g., C, Java, etc.)
   * @param pFileNames the source code files from which the CFA was created
   * @param pMainFunctionEntry the entry point of the program represented by the CFA
   * @param pConnectedness specifies whether the CFA is a supergraph
   * @return a new CFA metadata instance for the specified parameters
   * @throws NullPointerException if any parameter is {@code null} or if {@code pFileNames} contains
   *     {@code null}
   */
  public static CfaMetadata of(
      MachineModel pMachineModel,
      Language pLanguage,
      List<Path> pFileNames,
      FunctionEntryNode pMainFunctionEntry,
      CfaConnectedness pConnectedness) {

    return new CfaMetadata(
        checkNotNull(pMachineModel),
        checkNotNull(pLanguage),
        ImmutableList.copyOf(pFileNames),
        checkNotNull(pMainFunctionEntry),
        checkNotNull(pConnectedness),
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
        language,
        fileNames,
        mainFunctionEntry,
        connectedness,
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
    return language;
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
        language,
        fileNames,
        checkNotNull(pMainFunctionEntry),
        connectedness,
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
        language,
        fileNames,
        mainFunctionEntry,
        checkNotNull(pConnectedness),
        loopStructure,
        variableClassification,
        liveVariables);
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
   * Returns a copy of this metadata instance, but with the specified loop structure.
   *
   * @param pLoopStructure the loop structure to store in the returned metadata instance (use {@code
   *     null} to create an instance without loop structure)
   * @return a copy of this metadata instance, but with the specified loop structure
   */
  public CfaMetadata withLoopStructure(@Nullable LoopStructure pLoopStructure) {
    return new CfaMetadata(
        machineModel,
        language,
        fileNames,
        mainFunctionEntry,
        connectedness,
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
        language,
        fileNames,
        mainFunctionEntry,
        connectedness,
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
        language,
        fileNames,
        mainFunctionEntry,
        connectedness,
        loopStructure,
        variableClassification,
        pLiveVariables);
  }

  /** Serializes CFA metadata. */
  private void writeObject(java.io.ObjectOutputStream pObjectOutputStream) throws IOException {

    pObjectOutputStream.defaultWriteObject();

    // some `Path` implementations are not serializable, so we serialize paths as list of strings
    List<String> stringFileNames = ImmutableList.copyOf(Lists.transform(fileNames, Path::toString));
    pObjectOutputStream.writeObject(stringFileNames);
  }

  /** Deserializes CFA metadata. */
  private void readObject(java.io.ObjectInputStream pObjectInputStream)
      throws IOException, ClassNotFoundException {

    pObjectInputStream.defaultReadObject();

    @SuppressWarnings("unchecked") // paths are always serialized as a list of strings
    List<String> stringFileNames = (List<String>) pObjectInputStream.readObject();
    fileNames = ImmutableList.copyOf(Lists.transform(stringFileNames, Path::of));
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        machineModel,
        language,
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

    if (!(pObject instanceof CfaMetadata)) {
      return false;
    }

    CfaMetadata other = (CfaMetadata) pObject;
    return machineModel == other.machineModel
        && language == other.language
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
        .add("language", language)
        .add("fileNames", fileNames)
        .add("mainFunctionEntry", mainFunctionEntry)
        .add("connectedness", connectedness)
        .add("loopStructure", loopStructure)
        .add("variableClassification", variableClassification)
        .add("liveVariables", liveVariables)
        .toString();
  }
}
