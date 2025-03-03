// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessV2ParserUtils;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessV2ParserUtils.InvalidYAMLWitnessException;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LemmaSetEntry;

public class ACSLParserUtilsTest {
  public static final String TEST_DIR_PATH = "test/lemma";

  @Test
  public void testParseDeclarations() throws IOException, InvalidYAMLWitnessException {
    Path witnessFile = Path.of(TEST_DIR_PATH, "witness.yml");
    List<LemmaSetEntry> lemmaSetEntries = AutomatonWitnessV2ParserUtils.readLemmaFile(witnessFile);
    List<String> declarations =
        AutomatonWitnessV2ParserUtils.parseDeclarationsFromFile(lemmaSetEntries).asList();
    List<CDeclaration> cDeclarations = ACSLParserUtils.parseDeclarations(declarations);

    CFunctionType retType =
        CFunctionType.functionTypeWithReturnType(ACSLParserUtils.toCtype("int"));
    CParameterDeclaration firstParameter =
        new CParameterDeclaration(FileLocation.DUMMY, ACSLParserUtils.toCtype("int*"), "A");
    CParameterDeclaration secondParameter =
        new CParameterDeclaration(FileLocation.DUMMY, ACSLParserUtils.toCtype("int"), "I");
    List<CParameterDeclaration> parameters = Arrays.asList(firstParameter, secondParameter);
    CFunctionDeclaration functionDeclaration =
        new CFunctionDeclaration(
            FileLocation.DUMMY, retType, "MaxArray", parameters, ImmutableSet.of());

    CVariableDeclaration variableDeclaration =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            false,
            CStorageClass.AUTO,
            ACSLParserUtils.toCtype("int*"),
            "A",
            "A",
            "A",
            null);

    assertThat(cDeclarations).hasSize(3);
    assertThat(cDeclarations.get(0)).isInstanceOf(CFunctionDeclaration.class);
    assertThat(cDeclarations.get(0)).isEqualTo(functionDeclaration);
    assertThat(cDeclarations.get(1)).isInstanceOf(CVariableDeclaration.class);
    assertThat(cDeclarations.get(1)).isEqualTo(variableDeclaration);
  }
}
