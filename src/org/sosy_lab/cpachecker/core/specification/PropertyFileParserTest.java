// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.specification;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.io.CharSource;
import com.google.common.truth.Expect;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.sosy_lab.cpachecker.core.specification.Property.CommonCoverageProperty;
import org.sosy_lab.cpachecker.core.specification.Property.CommonVerificationProperty;
import org.sosy_lab.cpachecker.core.specification.Property.CoverFunctionCallProperty;
import org.sosy_lab.cpachecker.core.specification.Property.OtherLtlProperty;
import org.sosy_lab.cpachecker.core.specification.PropertyFileParser.InvalidPropertyFileException;

public class PropertyFileParserTest {

  private static final ImmutableMap<String, Property> TEST_PROPERTIES =
      ImmutableMap.<String, Property>builder()
          // https://github.com/sosy-lab/sv-benchmarks/tree/master/java/properties
          .put("CHECK( init(Main.main()), LTL(G assert) )", CommonVerificationProperty.ASSERT)
          .put(
              "CHECK( init(Main.main()), LTL(G !deadlock) )",
              new OtherLtlProperty("G !deadlock")) // TODO fix!
          // https://github.com/sosy-lab/sv-benchmarks/tree/master/c/properties
          .put("CHECK( init(main()), LTL(F end) )", CommonVerificationProperty.TERMINATION)
          .put(
              "CHECK( init(main()), LTL(G ! call(reach_error())) )",
              CommonVerificationProperty.REACHABILITY_ERROR)
          .put("CHECK( init(main()), LTL(G ! data-race) )", new OtherLtlProperty("G ! data-race"))
          .put("CHECK( init(main()), LTL(G def-behavior) )", new OtherLtlProperty("G def-behavior"))
          .put("CHECK( init(main()), LTL(G ! overflow) )", CommonVerificationProperty.OVERFLOW)
          .put("CHECK( init(main()), LTL(G valid-deref) )", CommonVerificationProperty.VALID_DEREF)
          .put("CHECK( init(main()), LTL(G valid-free) )", CommonVerificationProperty.VALID_FREE)
          .put(
              "CHECK( init(main()), LTL(G valid-memcleanup) )",
              CommonVerificationProperty.VALID_MEMCLEANUP)
          .put(
              "CHECK( init(main()), LTL(G valid-memtrack) )",
              CommonVerificationProperty.VALID_MEMTRACK)
          .put(
              "COVER( init(main()), FQL(COVER EDGES(@BASICBLOCKENTRY)) )",
              CommonCoverageProperty.COVERAGE_STATEMENT)
          .put(
              "COVER( init(main()), FQL(COVER EDGES(@CALL(reach_error))) )",
              new CoverFunctionCallProperty("reach_error"))
          .put(
              "COVER( init(main()), FQL(COVER EDGES(@CONDITIONEDGE)) )",
              CommonCoverageProperty.COVERAGE_CONDITION)
          .put(
              "COVER( init(main()), FQL(COVER EDGES(@DECISIONEDGE)) )",
              CommonCoverageProperty.COVERAGE_BRANCH)
          // historic
          .put(
              "CHECK( init(main()), LTL(G ! label(ERROR)) )",
              CommonVerificationProperty.REACHABILITY_LABEL)
          .put(
              "CHECK( init(main()), LTL(G ! call(__VERIFIER_error())) )",
              CommonVerificationProperty.REACHABILITY)
          .put(
              "COVER( init(main()), FQL(COVER EDGES(@CALL(__VERIFIER_error))) )",
              CommonCoverageProperty.COVERAGE_ERROR)
          // expected to come
          .put("CHECK( init(main()), LTL(G ! deadlock) )", CommonVerificationProperty.DEADLOCK)
          // non-hardcoded values
          .put(
              "COVER( init(main()), FQL(COVER EDGES(@CALL(some_arbitrary_function))) )",
              new CoverFunctionCallProperty("some_arbitrary_function"))
          .put(
              "CHECK( init(main()), LTL( F G (x = 1) ) )",
              new OtherLtlProperty(" F G (x = 1) ")) // TODO should trim
          .buildOrThrow();

  private static final String VALID_ASSERT_PROPERTY = "CHECK( init(main()), LTL(G assert) )";

  public final @Rule Expect expect = Expect.create();

  @Test
  public void checkTestCompletness() {
    expect
        .withMessage("Please add tests when adding new properties")
        .that(TEST_PROPERTIES.values())
        .containsAtLeastElementsIn(CommonVerificationProperty.values());
    expect
        .withMessage("Please add tests when adding new properties")
        .that(TEST_PROPERTIES.values())
        .containsAtLeastElementsIn(CommonCoverageProperty.values());
  }

  @Test
  public void testSingleProperty() throws InvalidPropertyFileException, IOException {
    for (Map.Entry<String, Property> entry : TEST_PROPERTIES.entrySet()) {
      PropertyFileParser parser = new PropertyFileParser(CharSource.wrap(entry.getKey()));
      parser.parse();
      expect.that(parser.getProperties()).containsExactly(entry.getValue());
    }
  }

  @Test
  public void testRedundant() throws InvalidPropertyFileException, IOException {
    String fileContent = VALID_ASSERT_PROPERTY + "\n" + VALID_ASSERT_PROPERTY;

    PropertyFileParser parser = new PropertyFileParser(CharSource.wrap(fileContent));
    parser.parse();
    assertThat(parser.getProperties()).containsExactly(CommonVerificationProperty.ASSERT);
  }

  @Test
  public void testMemsafety() throws InvalidPropertyFileException, IOException {
    Set<Property> properties =
        ImmutableSet.of(
            CommonVerificationProperty.VALID_DEREF,
            CommonVerificationProperty.VALID_FREE,
            CommonVerificationProperty.VALID_MEMTRACK);
    Set<String> propertyStrings =
        Maps.filterValues(TEST_PROPERTIES, v -> properties.contains(v)).keySet();
    String fileContent = Joiner.on('\n').join(propertyStrings);

    PropertyFileParser parser = new PropertyFileParser(CharSource.wrap(fileContent));
    parser.parse();
    assertThat(parser.getProperties()).containsExactlyElementsIn(properties).inOrder();
  }

  @Test
  public void testEntryFunction() throws InvalidPropertyFileException, IOException {
    for (String entryFunction : ImmutableList.of("main", "foo", "package.Class.method")) {
      String fileContent = String.format("CHECK( init(%s()), LTL(G assert) )", entryFunction);
      PropertyFileParser parser = new PropertyFileParser(CharSource.wrap(fileContent));
      parser.parse();
      expect.that(parser.getEntryFunction()).isEqualTo(entryFunction.trim());
    }
  }

  @Test
  public void testInvalid() {
    List<String> invalidFiles =
        ImmutableList.of(
            "",
            "  \n  ",
            "# " + VALID_ASSERT_PROPERTY,
            " " + VALID_ASSERT_PROPERTY, // TODO trim first?
            VALID_ASSERT_PROPERTY + "\n#",
            "CHECK( init(main), LTL(G assert) )",
            // "CHECK( init(()), LTL(G assert) )", TODO fix
            // "CHECK( init(m(ai)n()), LTL(G assert) )", TODO fix
            "CHECK( LTL(G assert) )",
            VALID_ASSERT_PROPERTY + "\nCHECK( init(foo()), LTL(G assert) )");

    for (String fileContent : invalidFiles) {
      PropertyFileParser parser = new PropertyFileParser(CharSource.wrap(fileContent));
      assertThrows(InvalidPropertyFileException.class, () -> parser.parse());
    }
  }
}
