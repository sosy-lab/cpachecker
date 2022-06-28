// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries;

import com.google.common.collect.FluentIterable;
import com.google.common.io.Files;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * This class can be used to rewrite files using delete and insert operations. All changes are
 * tracked based on the offset in the original file or string. If a Path object is passed, we assume
 * the file is encoded in the UTF-8 charset.
 */
public class Rewrite {

  private final Optional<Path> path;
  private final String content;
  private final List<Modification> changes = new ArrayList<>();

  public Rewrite(Path pPath) throws IOException {
    path = Optional.of(pPath);
    content = Files.asCharSource(pPath.toFile(), StandardCharsets.UTF_8).read();
  }

  public Rewrite(String pContent) {
    path = Optional.empty();
    content = pContent;
  }

  /**
   * This method has the same effect like {@link Rewrite#insert(int, String)}, with the difference
   * that the inserted snipped will be indented, i.e., all but the first line of the snippet will be
   * prepended by the whitespace that is found in the beginning of the line of the offset where the
   * snippet shall be inserted
   */
  public void insertIndented(int offset, String snippet) throws ConflictingModificationException {
    snippet = indent(snippet, determineIndent(offset));
    insert(offset, snippet);
  }

  public void insert(int offset, String snippet) throws ConflictingModificationException {
    checkUntouched(offset - 1, 1);
    changes.add(new Insertion(offset, snippet));
  }

  public Optional<Path> getPath() {
    return path;
  }

  private void checkUntouched(int offset, int length) throws ConflictingModificationException {
    if (!untouched(offset, length)) {
      throw new ConflictingModificationException(
          "This offset is already modified by a different modification operation!");
    }
  }

  public void delete(int offset, int length) throws ConflictingModificationException {
    checkUntouched(offset, length);
    changes.add(new Deletion(offset, length));
  }

  public String apply() {
    StringBuilder builder = new StringBuilder();
    int currentOffset = 0;
    int readOffset = 0;
    Collections.sort(changes);
    for (Modification m : changes) {
      int offset = m.getOffset();
      if (offset > currentOffset) {
        builder.append(content.subSequence(readOffset, offset));
        readOffset = offset;
      }
      currentOffset = offset;
      if (m instanceof Deletion) {
        Deletion d = (Deletion) m;
        readOffset += d.getLength();
      } else if (m instanceof Insertion) {
        Insertion i = (Insertion) m;
        builder.append(i.getSnippet());
      }
    }
    builder.append(content.substring(readOffset));
    return builder.toString();
  }

  public boolean untouched(int offset, int length) {
    return FluentIterable.from(changes).filter(x -> x.touches(offset, length)).isEmpty();
  }

  private String determineIndent(int offset) {
    int start = offset;
    while (start > 0) {
      if (!content.substring(start - 1, offset).matches("\\s.*")) {
        break;
      }
      start--;
    }
    return content.substring(start, offset);
  }

  private String indent(String toindent, String indentation) {
    StringBuilder builder = new StringBuilder();
    toindent.lines().findFirst().ifPresent(builder::append);
    toindent.lines().skip(1).map(x -> indentation + x).forEach(builder::append);
    return builder.toString();
  }

  private class Modification implements Comparable<Modification> {

    private final int offset;

    public Modification(int pOffset) {
      offset = pOffset;
    }

    public int getOffset() {
      return offset;
    }

    @SuppressWarnings("unused")
    public boolean touches(int pOffset, int pLength) {
      return false;
    }

    @Override
    public int compareTo(Modification other) {
      return this.getOffset() - other.getOffset();
    }
  }

  private class Deletion extends Modification {

    private final int length;

    public Deletion(int pOffset, int pLength) {
      super(pOffset);
      length = pLength;
    }

    @Override
    public boolean touches(int pOffset, int pLength) {
      return !(pOffset + pLength <= getOffset() || getOffset() + getLength() <= pOffset);
    }

    public int getLength() {
      return length;
    }
  }

  private class Insertion extends Modification {

    private final String snippet;

    public Insertion(int pOffset, String pSnippet) {
      super(pOffset);
      snippet = pSnippet;
    }

    public String getSnippet() {
      return snippet;
    }
  }

  public class ConflictingModificationException extends Exception {
    private static final long serialVersionUID = -6169337864659759938L;

    public ConflictingModificationException(String msg) {
      super(msg);
    }
  }
}
