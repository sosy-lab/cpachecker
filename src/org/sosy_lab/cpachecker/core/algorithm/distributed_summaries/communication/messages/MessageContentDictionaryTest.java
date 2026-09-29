// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

public class MessageContentDictionaryTest {
  private static final String LONG_VALUE =
      "Repeated domain-independent message content ".repeat(20);

  @Test
  public void sharesRepeatedPartsAcrossUnrelatedNamespaces() {
    var content =
        ImmutableMap.of(
            "states",
            "2",
            "state0.domainA.precision",
            LONG_VALUE,
            "state1.domainB.precision",
            LONG_VALUE + " , " + "different",
            "state1.domainB.state",
            LONG_VALUE);
    var encoded = MessageContentDictionary.encode(content);
    assertThat(encoded.values().stream().filter(LONG_VALUE::equals).count()).isEqualTo(1);
    assertThat(MessageContentDictionary.decode(encoded)).isEqualTo(content);
    assertThat(MessageContentDictionary.encode(encoded)).isEqualTo(encoded);
    DssMessage restored =
        DssMessage.fromJson(new DssPostConditionMessage("test", content).asJson());
    assertThat(restored.getNumberOfContainedStates().orElseThrow()).isEqualTo(2);
    assertThat(restored.advance("state1").asJson().get(DssMessage.DSS_MESSAGE_CONTENT_ID))
        .isEqualTo(
            MessageContentDictionary.encode(
                ImmutableMap.<String, String>builder()
                    .putAll(content)
                    .put("domainB.precision", LONG_VALUE + " , " + "different")
                    .put("domainB.state", LONG_VALUE)
                    .buildOrThrow()));
  }

  @Test
  public void preservesRepetitionsEmptyPartsAndDelimitersExactly() {
    var content =
        ImmutableMap.of(
            "empty",
            "",
            "ordered",
            " , " + LONG_VALUE + " ,  , " + LONG_VALUE + " , ",
            "unicode",
            "\n\"\\ä" + LONG_VALUE);
    assertThat(MessageContentDictionary.decode(MessageContentDictionary.encode(content)))
        .isEqualTo(content);
  }

  @Test
  public void skipsEncodingWhenItWouldGrowTheMessage() {
    var content = ImmutableMap.of("a", "", "b", "short", "c", "short");
    assertThat(MessageContentDictionary.encode(content)).isEqualTo(content);
    assertThat(MessageContentDictionary.decode(content)).isEqualTo(content);
  }

  @Test
  public void rejectsUnknownEncodingsAndDanglingReferences() {
    assertThrows(
        IllegalArgumentException.class,
        () -> MessageContentDictionary.decode(ImmutableMap.of("dssContentEncoding", "2")));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            MessageContentDictionary.decode(
                ImmutableMap.of("dssContentEncoding", "1", "content.a", "3")));
  }
}
