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
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

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
    return new Builder(this).setMachineModel(pMachineModel).build();
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
    return new Builder(this).setMainFunctionEntry(pMainFunctionEntry).build();
  }

  public CfaConnectedness getConnectedness() {
    return connectedness;
  }

  public CfaMetadata withConnectedness(CfaConnectedness pConnectedness) {
    return new Builder(this).setConnectedness(pConnectedness).build();
  }

  public Optional<LoopStructure> getLoopStructure() {
    return Optional.ofNullable(loopStructure);
  }

  public CfaMetadata withLoopStructure(@Nullable LoopStructure pLoopStructure) {
    return new Builder(this).setLoopStructure(pLoopStructure).build();
  }

  public Optional<VariableClassification> getVariableClassification() {
    return Optional.ofNullable(variableClassification);
  }

  public CfaMetadata withVariableClassification(
      @Nullable VariableClassification pVariableClassification) {
    return new Builder(this).setVariableClassification(pVariableClassification).build();
  }

  public Optional<LiveVariables> getLiveVariables() {
    return Optional.ofNullable(liveVariables);
  }

  public CfaMetadata withLiveVariables(@Nullable LiveVariables pLiveVariables) {
    return new Builder(this).setLiveVariables(pLiveVariables).build();
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

  private static final class Builder {

    private MachineModel machineModel;
    private Language language;
    private ImmutableList<Path> fileNames;
    private FunctionEntryNode mainFunctionEntry;
    private CfaConnectedness connectedness;

    private @Nullable LoopStructure loopStructure;
    private @Nullable VariableClassification variableClassification;
    private @Nullable LiveVariables liveVariables;

    private Builder(CfaMetadata pCfaMetadata) {

      machineModel = pCfaMetadata.machineModel;
      language = pCfaMetadata.language;
      fileNames = pCfaMetadata.fileNames;
      mainFunctionEntry = pCfaMetadata.mainFunctionEntry;
      connectedness = pCfaMetadata.connectedness;

      loopStructure = pCfaMetadata.loopStructure;
      variableClassification = pCfaMetadata.variableClassification;
      liveVariables = pCfaMetadata.liveVariables;
    }

    private Builder setMachineModel(MachineModel pMachineModel) {

      machineModel = checkNotNull(pMachineModel);

      return this;
    }

    private Builder setMainFunctionEntry(FunctionEntryNode pMainFunctionEntry) {

      mainFunctionEntry = checkNotNull(pMainFunctionEntry);

      return this;
    }

    private Builder setConnectedness(CfaConnectedness pConnectedness) {

      connectedness = checkNotNull(pConnectedness);

      return this;
    }

    private Builder setLoopStructure(@Nullable LoopStructure pLoopStructure) {

      loopStructure = pLoopStructure;

      return this;
    }

    private Builder setVariableClassification(
        @Nullable VariableClassification pVariableClassification) {

      variableClassification = pVariableClassification;

      return this;
    }

    private Builder setLiveVariables(@Nullable LiveVariables pLiveVariables) {

      liveVariables = pLiveVariables;

      return this;
    }

    private CfaMetadata build() {
      return new CfaMetadata(
          machineModel,
          language,
          fileNames,
          mainFunctionEntry,
          connectedness,
          loopStructure,
          variableClassification,
          liveVariables);
    }
  }
}
