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

  public CfaMetadata(
      MachineModel pMachineModel,
      Language pLanguage,
      ImmutableList<Path> pFileNames,
      FunctionEntryNode pMainFunctionEntry,
      CfaConnectedness pConnectedness) {

    machineModel = checkNotNull(pMachineModel);
    language = checkNotNull(pLanguage);
    fileNames = checkNotNull(pFileNames);
    mainFunctionEntry = checkNotNull(pMainFunctionEntry);
    connectedness = checkNotNull(pConnectedness);
  }

  public MachineModel getMachineModel() {
    return machineModel;
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

  public CfaConnectedness getConnectedness() {
    return connectedness;
  }
}
