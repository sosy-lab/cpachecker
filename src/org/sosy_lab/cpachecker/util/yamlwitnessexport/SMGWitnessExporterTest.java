// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGInvariant;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class SMGWitnessExporterTest {

  private SMGWitnessExporter exporter;
  private SMGState mockSMGState;
  private ARGState mockARGState;
  private Path tempOutputPath;

  @Before
  public void setUp() throws InvalidConfigurationException, IOException {
    Configuration config = TestDataTools.configurationForTest().build();
    CFA mockCFA = mock(CFA.class);
    Specification mockSpec = mock(Specification.class);
    LogManager mockLogger = mock(LogManager.class);

    exporter = new SMGWitnessExporter(config, mockCFA, mockSpec, mockLogger);
    mockSMGState = mock(SMGState.class);
    mockARGState = mock(ARGState.class);
    tempOutputPath = Files.createTempFile("witness_test", ".yml");
  }

  @Test
  public void testMemorySafetyWitnessExport() {
    List<SMGInvariant> testInvariants = ImmutableList.of(
        new SMGInvariant(
            SMGInvariant.InvariantType.POINTER_VALIDITY,
            SMGInvariant.Property.MEMORY_SAFETY,
            "ptr",
            null,
            0,
            null
        ),
        new SMGInvariant(
            SMGInvariant.InvariantType.ALLOCATION_STATUS,
            SMGInvariant.Property.MEMORY_SAFETY,
            "ptr",
            null,
            0,
            null
        )
    );

    when(mockSMGState.getInvariants()).thenReturn(testInvariants);

    List<ACSLInvariant> acslInvariants = exporter.extractMemorySafetyInvariants(mockSMGState);

    assertThat(acslInvariants).isNotNull();
    assertThat(acslInvariants).hasSize(2);

    ACSLInvariant validInvariant = acslInvariants.get(0);
    assertThat(validInvariant.getType()).isEqualTo("memory_safety");
    assertThat(validInvariant.getFormat()).isEqualTo("acsl");
    assertThat(validInvariant.getExpression()).contains("\\valid");
  }

  @Test
  public void testACSLConversion() {
    SMGInvariant bufferBoundsInvariant = new SMGInvariant(
        SMGInvariant.InvariantType.BUFFER_BOUNDS,
        SMGInvariant.Property.MEMORY_SAFETY,
        "buffer",
        null,
        32,
        null
    );

    ACSLConverter converter = new ACSLConverter();
    String acslExpression = converter.convertToACSL(bufferBoundsInvariant);

    assertThat(acslExpression).isNotNull();
    assertThat(acslExpression).contains("\\valid(buffer+(0..31))");
  }

  @Test
  public void testWitnessExportWithEmptyInvariants() {
    when(mockSMGState.getInvariants()).thenReturn(ImmutableList.of());

    List<ACSLInvariant> acslInvariants = exporter.extractMemorySafetyInvariants(mockSMGState);

    assertThat(acslInvariants).isNotNull();
    assertThat(acslInvariants).isEmpty();
  }

  @Test
  public void testTemporalSafetyInvariant() {
    SMGInvariant temporalInvariant = new SMGInvariant(
        SMGInvariant.InvariantType.TEMPORAL_SAFETY,
        SMGInvariant.Property.MEMORY_SAFETY,
        "ptr",
        "use_after_free",
        0,
        "\\old"
    );

    when(mockSMGState.getInvariants()).thenReturn(ImmutableList.of(temporalInvariant));

    List<ACSLInvariant> acslInvariants = exporter.extractMemorySafetyInvariants(mockSMGState);

    assertThat(acslInvariants).hasSize(1);
    assertThat(acslInvariants.get(0).getExpression()).contains("\\at");
  }

  @Test
  public void testInvariantFiltering() {
    List<SMGInvariant> mixedInvariants = ImmutableList.of(
        new SMGInvariant(
            SMGInvariant.InvariantType.POINTER_VALIDITY,
            SMGInvariant.Property.MEMORY_SAFETY,
            "ptr1",
            null,
            0,
            null
        ),
        new SMGInvariant(
            SMGInvariant.InvariantType.POINTER_VALIDITY,
            SMGInvariant.Property.REACHABILITY, // Different property
            "ptr2",
            null,
            0,
            null
        )
    );

    when(mockSMGState.getInvariants()).thenReturn(mixedInvariants);

    List<ACSLInvariant> acslInvariants = exporter.extractMemorySafetyInvariants(mockSMGState);

    assertThat(acslInvariants).hasSize(1);
  }
}
