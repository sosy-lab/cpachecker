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
import org.sosy_lab.cpachecker.core.specification.Property.CommonCoverageType;
import org.sosy_lab.cpachecker.core.specification.Property.CommonPropertyType;
import org.sosy_lab.cpachecker.core.specification.Property.CoverFunction;
import org.sosy_lab.cpachecker.core.specification.Property.OtherVerificationProperty;
import org.sosy_lab.cpachecker.core.specification.PropertyFileParser.InvalidPropertyFileException;

public class PropertyFileParserTest {

  private static final ImmutableMap<String, Property> TEST_PROPERTIES =
      ImmutableMap.<String, Property>builder()
          // https://github.com/sosy-lab/sv-benchmarks/tree/master/java/properties
          .put("CHECK( init(Main.main()), LTL(G assert) )", CommonPropertyType.ASSERT)
          .put(
              "CHECK( init(Main.main()), LTL(G !deadlock) )",
              new OtherVerificationProperty("G !deadlock")) // TODO fix!
          // https://github.com/sosy-lab/sv-benchmarks/tree/master/c/properties
          .put("CHECK( init(main()), LTL(F end) )", CommonPropertyType.TERMINATION)
          .put(
              "CHECK( init(main()), LTL(G ! call(reach_error())) )",
              CommonPropertyType.REACHABILITY_ERROR)
          .put(
              "CHECK( init(main()), LTL(G ! data-race) )",
              new OtherVerificationProperty("G ! data-race"))
          .put(
              "CHECK( init(main()), LTL(G def-behavior) )",
              new OtherVerificationProperty("G def-behavior"))
          .put("CHECK( init(main()), LTL(G ! overflow) )", CommonPropertyType.OVERFLOW)
          .put("CHECK( init(main()), LTL(G valid-deref) )", CommonPropertyType.VALID_DEREF)
          .put("CHECK( init(main()), LTL(G valid-free) )", CommonPropertyType.VALID_FREE)
          .put(
              "CHECK( init(main()), LTL(G valid-memcleanup) )", CommonPropertyType.VALID_MEMCLEANUP)
          .put("CHECK( init(main()), LTL(G valid-memtrack) )", CommonPropertyType.VALID_MEMTRACK)
          .put(
              "COVER( init(main()), FQL(COVER EDGES(@BASICBLOCKENTRY)) )",
              CommonCoverageType.COVERAGE_STATEMENT)
          .put(
              "COVER( init(main()), FQL(COVER EDGES(@CALL(reach_error))) )",
              new CoverFunction("reach_error"))
          .put(
              "COVER( init(main()), FQL(COVER EDGES(@CONDITIONEDGE)) )",
              CommonCoverageType.COVERAGE_CONDITION)
          .put(
              "COVER( init(main()), FQL(COVER EDGES(@DECISIONEDGE)) )",
              CommonCoverageType.COVERAGE_BRANCH)
          // historic
          .put(
              "CHECK( init(main()), LTL(G ! label(ERROR)) )", CommonPropertyType.REACHABILITY_LABEL)
          .put(
              "CHECK( init(main()), LTL(G ! call(__VERIFIER_error())) )",
              CommonPropertyType.REACHABILITY)
          .put(
              "COVER( init(main()), FQL(COVER EDGES(@CALL(__VERIFIER_error))) )",
              CommonCoverageType.COVERAGE_ERROR)
          // expected to come
          .put("CHECK( init(main()), LTL(G ! deadlock) )", CommonPropertyType.DEADLOCK)
          // non-hardcoded values
          .put(
              "COVER( init(main()), FQL(COVER EDGES(@CALL(some_arbitrary_function))) )",
              new CoverFunction("some_arbitrary_function"))
          .put(
              "CHECK( init(main()), LTL( F G (x = 1) ) )",
              new OtherVerificationProperty(" F G (x = 1) ")) // TODO should trim
          .build();

  private static final String VALID_ASSERT_PROPERTY = "CHECK( init(main()), LTL(G assert) )";

  public final @Rule Expect expect = Expect.create();

  @Test
  public void checkTestCompletness() {
    expect
        .withMessage("Please add tests when adding new properties")
        .that(TEST_PROPERTIES.values())
        .containsAtLeastElementsIn(CommonPropertyType.values());
    expect
        .withMessage("Please add tests when adding new properties")
        .that(TEST_PROPERTIES.values())
        .containsAtLeastElementsIn(CommonCoverageType.values());
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
    assertThat(parser.getProperties()).containsExactly(CommonPropertyType.ASSERT);
  }

  @Test
  public void testMemsafety() throws InvalidPropertyFileException, IOException {
    Set<Property> properties =
        ImmutableSet.of(
            CommonPropertyType.VALID_DEREF,
            CommonPropertyType.VALID_FREE,
            CommonPropertyType.VALID_MEMTRACK);
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
