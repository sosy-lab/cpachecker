// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import static com.google.common.base.Preconditions.checkNotNull;

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
 * Class for storing additional data about a {@link CFA}, including its {@link Language} and main
 * function entry node.
 *
 * <p>Instances of this class are immutable. Instead of using {@code setXYZ}, use {@code withXYZ} to
 * create a new instance that has the specified value for {@code XYZ}.
 */
public final class CfaMetadata implements Serializable {

  private static final long serialVersionUID = -4976424764995656485L;

  private final MachineModel machineModel;
  private final Language language;
  // `fileNames` isn't `final` due to serialization, but shouldn't be reassigned anywhere else
  private transient ImmutableList<Path> fileNames;
  private final FunctionEntryNode mainFunctionEntry;
  private final CfaConnectedness connectedness;

  // TODO: make it easier to add additional metadata attributes
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
   * Returns a new {@code CfaMetadata} instance for the specified parameters.
   *
   * @param pMachineModel the machine model to use (defines sizes for all basic types)
   * @param pLanguage the language of the CFA (e.g., C, Java, etc.)
   * @param pFileNames the source code files from which the CFA was created
   * @param pMainFunctionEntry the entry point of the program represented by the CFA
   * @param pConnectedness specifies whether functions are connected by super-edges (i.e., function
   *     call and return edges)
   * @return a new {@code CfaMetadata} instance for the specified parameters
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

  public MachineModel getMachineModel() {
    return machineModel;
  }

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

  public Language getLanguage() {
    return language;
  }

  public ImmutableList<Path> getFileNames() {
    return fileNames;
  }

  public FunctionEntryNode getMainFunctionEntry() {
    return mainFunctionEntry;
  }

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

  public CfaConnectedness getConnectedness() {
    return connectedness;
  }

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

  public Optional<LoopStructure> getLoopStructure() {
    return Optional.ofNullable(loopStructure);
  }

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

  public Optional<VariableClassification> getVariableClassification() {
    return Optional.ofNullable(variableClassification);
  }

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

  public Optional<LiveVariables> getLiveVariables() {
    return Optional.ofNullable(liveVariables);
  }

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

  private void writeObject(java.io.ObjectOutputStream pObjectOutputStream) throws IOException {

    pObjectOutputStream.defaultWriteObject();

    // some `Path` implementations are not serializable, so we serialize paths as list of strings
    List<String> stringFileNames = ImmutableList.copyOf(Lists.transform(fileNames, Path::toString));
    pObjectOutputStream.writeObject(stringFileNames);
  }

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
}
