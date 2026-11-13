// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import com.google.common.collect.ImmutableList;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessV2ParserUtils;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.YAMLWitnessExpressionType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.YAMLWitnessVersion;

public class YAMLWitnessModelTest {

  public static final String TEST_DIR_PATH = "test/witness/";

  private static final Path MEMSAFETY_CORRECTNESS_WITNESS_DIR =
      Path.of("test/programs/witness-v2-validation/memsafety-correctness");

  // Copy of the handcrafted witness examples/memsafety_sll-blank.witness-2.4.yml from SV-Witnesses
  private static final String MEMSAFETY_WITNESS_V2D4 = "memsafety_sll-blank.witness-2.4.yml";

  private static final String MEMSAFETY_WITNESS_V2D4_PROGRAM = "./memsafety_sll-blank.c";

  @Test
  public void testParsingMemorySafetyWitness() throws IOException {
    Queue<AbstractEntry> entries = parseMemorySafetyWitnessV2d4();
    assertThat(entries).hasSize(2);
    assertThat(AutomatonWitnessV2ParserUtils.getWitnessVersion(ImmutableList.copyOf(entries)))
        .hasValue(YAMLWitnessVersion.V2d4);

    AbstractEntry firstEntry = entries.remove();
    assertThat(firstEntry).isInstanceOf(PredicateDefinitionSetEntry.class);
    PredicateDefinitionSetEntry predicateSet = (PredicateDefinitionSetEntry) firstEntry;
    assertThat(predicateSet.getEntryType()).isEqualTo("predicate_definition_set");
    assertMemorySafetyMetadata(predicateSet.getMetadata(), "c66c3ee7-e0de-4bae-87f0-fd48fb1a961e");
    assertThat(predicateSet.getContent()).hasSize(1);

    PredicateDefinitionRecord predicate = predicateSet.getContent().getFirst();
    assertThat(predicate.getName()).isEqualTo("sll_seg");
    // The line breaks of the multi-line YAML string are folded into single spaces
    assertThat(predicate.getDefinition())
        .isEqualTo(
            "(start == end && \\canAccess(start, 1) && start->next == 0) || (start != 0 && start"
                + " != end && \\separated(\\canAccess(start, 1), sll_seg(start->next, end)))");
    assertThat(predicate.getFormat()).isEqualTo(YAMLWitnessExpressionType.EXT_C);
    assertThat(predicate.getParameters())
        .containsExactly(
            new PredicateParameterRecord("start", "struct sll *"),
            new PredicateParameterRecord("end", "struct sll *"))
        .inOrder();
    assertThat(predicate.getLocations()).hasSize(1);
    LocationRecord fileLocation = predicate.getLocations().getFirst();
    assertThat(fileLocation.getFileName()).isEqualTo(MEMSAFETY_WITNESS_V2D4_PROGRAM);
    assertThat(fileLocation.getLine()).isNull();
    assertThat(fileLocation.getColumn()).isEmpty();
    assertThat(fileLocation.getFunction()).isNull();

    AbstractEntry secondEntry = entries.remove();
    assertThat(secondEntry).isInstanceOf(InvariantSetEntry.class);
    InvariantSetEntry invariantSet = (InvariantSetEntry) secondEntry;
    assertThat(invariantSet.getEntryType()).isEqualTo("invariant_set");
    assertMemorySafetyMetadata(invariantSet.getMetadata(), "d77d4ff8-f1ef-4baf-98f1-fe59fc2b072f");
    assertThat(invariantSet.content).hasSize(1);
    assertThat(invariantSet.content.getFirst()).isInstanceOf(InvariantEntry.class);
    InvariantEntry invariant = (InvariantEntry) invariantSet.content.getFirst();
    assertThat(invariant.getType()).isEqualTo("loop_invariant");
    assertThat(invariant.getValue()).isEqualTo("sll_seg(sll, now)");
    assertThat(invariant.getFormat()).isEqualTo(YAMLWitnessExpressionType.EXT_C);
    assertThat(invariant.getLocation().getFileName()).isEqualTo(MEMSAFETY_WITNESS_V2D4_PROGRAM);
    assertThat(invariant.getLocation().getLine()).isEqualTo(25);
    assertThat(invariant.getLocation().getColumn()).hasValue(3);
    assertThat(invariant.getLocation().getFunction()).isEqualTo("create");
  }

  // Helper method for testParsingMemorySafetyWitness()
  private static void assertMemorySafetyMetadata(MetadataRecord metadata, String uuid) {
    assertThat(metadata.getFormatVersion()).isEqualTo("2.4");
    assertThat(metadata.getUuid()).isEqualTo(uuid);
    assertThat(metadata.getCreationTime()).isEqualTo("2055-11-21T10:29:38+01:00");
    assertThat(metadata.getProducer().getName()).isEqualTo("Handcrafted");
    assertThat(metadata.getProducer().getVersion()).isEqualTo("1.0");
    assertThat(metadata.getProducer().getConfiguration()).isNull();
    assertThat(metadata.getProducer().getDescription()).isNull();
    assertThat(metadata.getProducer().getCommandLine()).isNull();
    TaskRecord task = metadata.getTask();
    assertThat(task.getInputFiles()).containsExactly(MEMSAFETY_WITNESS_V2D4_PROGRAM);
    assertThat(task.getInputFileHashes())
        .containsExactly(
            MEMSAFETY_WITNESS_V2D4_PROGRAM,
            "e7b6a9391431fb21366a91d72411e21e14d71f699e8e2ee9ddeb0ffa5e442ea8");
    assertThat(task.getSpecification())
        .isEqualTo(
            """
            CHECK( init(main()), LTL(G valid-free) )
            CHECK( init(main()), LTL(G valid-deref) )
            CHECK( init(main()), LTL(G valid-memtrack) )
            """);
    assertThat(task.getDataModel()).isEqualTo("ILP32");
    assertThat(task.getLanguage()).isEqualTo("C");
  }

  @Test
  public void testRoundTripParsingOfMemorySafetyWitness() throws IOException {
    List<AbstractEntry> entries1 = ImmutableList.copyOf(parseMemorySafetyWitnessV2d4());

    // Serialize the same way as the YAML witness export does
    ObjectMapper mapper =
        new ObjectMapper(
            YAMLFactory.builder()
                .disable(Feature.WRITE_DOC_START_MARKER, Feature.SPLIT_LINES)
                .build());
    mapper.setSerializationInclusion(Include.NON_NULL);
    String yamlString = mapper.writeValueAsString(entries1);
    // The predicate definitions are wrapped in the 'predicate_definition' key
    assertThat(yamlString).contains("entry_type: \"predicate_definition_set\"");
    assertThat(yamlString).contains("- predicate_definition:");
    assertThat(yamlString).contains("format: \"ext_c_expression\"");

    List<AbstractEntry> entries2;
    try (InputStream input = new ByteArrayInputStream(yamlString.getBytes(UTF_8))) {
      entries2 = AutomatonWitnessV2ParserUtils.parseYAML(input);
    }
    assertThat(entries2).hasSize(2);

    assertThat(entries2.getFirst()).isEqualTo(entries1.getFirst());

    assertThat(entries2.get(1)).isInstanceOf(InvariantSetEntry.class);
    InvariantSetEntry invariantSet1 = (InvariantSetEntry) entries1.get(1);
    InvariantSetEntry invariantSet2 = (InvariantSetEntry) entries2.get(1);
    assertThat(invariantSet2.getMetadata()).isEqualTo(invariantSet1.getMetadata());
    assertThat(invariantSet2.content).isEqualTo(invariantSet1.content);
  }

  @Test
  public void testParsingPredicateDefinitionWithoutParameters() throws IOException {
    String predicateDefinition =
        """
        predicate_definition:
          name: "trivial"
          parameters:
          definition: "1"
          format: "c_expression"
          locations:
            - file_name: "./program.c"
              line: 5
              column: 3
              function: "main"
            - file_name: "./program.c"
        """;
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    PredicateDefinitionRecord predicate =
        mapper.readValue(predicateDefinition, PredicateDefinitionRecord.class);

    assertThat(predicate.getName()).isEqualTo("trivial");
    assertThat(predicate.getParameters()).isEmpty();
    assertThat(predicate.getDefinition()).isEqualTo("1");
    assertThat(predicate.getFormat()).isEqualTo(YAMLWitnessExpressionType.C);
    assertThat(predicate.getLocations())
        .containsExactly(
            new LocationRecord("./program.c", 5, 3, "main"),
            new LocationRecord("./program.c", null, null, null))
        .inOrder();
  }

  private static Queue<AbstractEntry> parseMemorySafetyWitnessV2d4() throws IOException {
    try (InputStream input =
        Files.newInputStream(MEMSAFETY_CORRECTNESS_WITNESS_DIR.resolve(MEMSAFETY_WITNESS_V2D4))) {
      return new ArrayDeque<>(AutomatonWitnessV2ParserUtils.parseYAML(input));
    }
  }

  @Test
  public void testParsingViolationWitness() throws IOException {
    Queue<AbstractEntry> loadedEntries = parseTestFile("violation-witness.yml");
    for (AbstractEntry e : loadedEntries) {
      if (e instanceof ViolationSequenceEntry violationSequenceEntry) {
        List<SegmentRecord> sequence = violationSequenceEntry.getContent();
        assertThat(sequence).hasSize(4);
        assertThat(sequence.getFirst().getSegment().size()).isAtLeast(1);
        assertThat(sequence.getFirst().getSegment().getFirst().getConstraint().getValue())
            .isEqualTo("(x >= 1024U)");
      }
    }
  }

  @Test
  public void testRoundTripParsingOfViolationWitness() throws IOException {
    Queue<AbstractEntry> entries1 = parseTestFile("violation-witness.yml");
    assertThat(entries1).hasSize(1);
    assertThat(entries1.element()).isInstanceOf(ViolationSequenceEntry.class);
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    String yamlString = mapper.writeValueAsString(ImmutableList.of(entries1.element()));
    ViolationSequenceEntry entry1 = (ViolationSequenceEntry) entries1.element();

    JavaType entryType =
        mapper.getTypeFactory().constructCollectionType(List.class, AbstractEntry.class);
    List<AbstractEntry> entries2 = mapper.readValue(yamlString, entryType);
    assertThat(entries2).hasSize(1);
    assertThat(entries2.getFirst()).isInstanceOf(ViolationSequenceEntry.class);
    ViolationSequenceEntry entry2 = (ViolationSequenceEntry) entries2.getFirst();

    // actual check of both representations
    assertThat(entry1.getMetadata()).isEqualTo(entry2.getMetadata());
    // TODO: check also the content for equality once we have decided on default values in case keys
    // are missing:
    // assertThat(entry1.getContent()).isEqualTo(entry2.getContent());
  }

  private static Queue<AbstractEntry> parseTestFile(String filename) throws IOException {
    Path yamlWitness = Path.of(TEST_DIR_PATH, filename);
    return parseTestFile(yamlWitness);
  }

  private static Queue<AbstractEntry> parseTestFile(Path pathToYamlWitness) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    JavaType entryType =
        mapper.getTypeFactory().constructCollectionType(List.class, AbstractEntry.class);
    List<AbstractEntry> entries = mapper.readValue(pathToYamlWitness.toFile(), entryType);
    return new ArrayDeque<>(entries);
  }
}
