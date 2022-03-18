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
import java.nio.file.Path;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;

public final class CfaMetadata {

  private final MachineModel machineModel;
  private final Language language;
  private final ImmutableList<Path> fileNames;
  private final FunctionEntryNode mainFunctionEntry;
  private final CfaConnectedness connectedness;

  private CfaMetadata(
      MachineModel pMachineModel,
      Language pLanguage,
      ImmutableList<Path> pFileNames,
      FunctionEntryNode pMainFunctionEntry,
      CfaConnectedness pConnectedness) {

    machineModel = pMachineModel;
    language = pLanguage;
    fileNames = pFileNames;
    mainFunctionEntry = pMainFunctionEntry;
    connectedness = pConnectedness;
  }

  public static CfaMetadata of(
      MachineModel pMachineModel,
      Language pLanguage,
      ImmutableList<Path> pFileNames,
      FunctionEntryNode pMainFunctionEntry,
      CfaConnectedness pConnectedness) {

    return new CfaMetadata(
        checkNotNull(pMachineModel),
        checkNotNull(pLanguage),
        checkNotNull(pFileNames),
        checkNotNull(pMainFunctionEntry),
        checkNotNull(pConnectedness));
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

  private static final class Builder {

    private MachineModel machineModel;
    private Language language;
    private ImmutableList<Path> fileNames;
    private FunctionEntryNode mainFunctionEntry;
    private CfaConnectedness connectedness;

    private Builder(CfaMetadata pCfaMetadata) {

      machineModel = pCfaMetadata.getMachineModel();
      language = pCfaMetadata.getLanguage();
      fileNames = pCfaMetadata.getFileNames();
      mainFunctionEntry = pCfaMetadata.getMainFunctionEntry();
      connectedness = pCfaMetadata.getConnectedness();
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

    private CfaMetadata build() {
      return new CfaMetadata(machineModel, language, fileNames, mainFunctionEntry, connectedness);
    }
  }
}
