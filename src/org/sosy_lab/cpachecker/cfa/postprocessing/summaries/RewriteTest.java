// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries;

import com.google.common.truth.Truth;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.Rewrite.ConflictingModificationException;

public class RewriteTest {

  @Test
  public void testDeletion() throws ConflictingModificationException {
    String content = "01234567890\n23456789";
    Rewrite r = new Rewrite(content);
    r.delete(9, 3);
    Truth.assertThat(r.apply()).isEqualTo("01234567823456789");
  }

  @Test
  public void testInsertion() throws ConflictingModificationException {
    String content = "01234567890\n23456789";
    Rewrite r = new Rewrite(content);
    r.insert(9, "foo");
    Truth.assertThat(r.apply()).isEqualTo("012345678foo90\n23456789");
  }

  @Test
  public void testDeletionAndInsertion() throws ConflictingModificationException {
    String content = "01234567890\n23456789";
    Rewrite r = new Rewrite(content);
    r.delete(9, 3);
    r.insert(9, "foo");
    Truth.assertThat(r.apply()).isEqualTo("012345678foo23456789");
    Rewrite r2 = new Rewrite(content);
    r2.insert(9, "foo");
    r2.delete(9, 3);
    Truth.assertThat(r.apply()).isEqualTo(r2.apply());
  }

  @Test
  public void testTouched() throws ConflictingModificationException {
    String content = "01234567890\n23456789";
    Rewrite r = new Rewrite(content);
    r.delete(9, 3);
    r.insert(9, "foo");
    Truth.assertThat(r.untouched(8, 1)).isTrue();
    Truth.assertThat(r.untouched(9, 1)).isFalse();
  }
}
