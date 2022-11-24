// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange.entryimport;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.FluentIterable;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.LoopInvariantEntry;

/**
 * Watches a directory for new invariant-store entries and reads and returns them.
 *
 * <p>This class queues available invariants. Thus the getter methods {@link #getNext()} and {@link
 * #awaitNext()} might return invariants directly in multiple successive calls, but may return empty
 * or block other times.
 *
 * <p>The method {@link #start()} must be called before any of the getter methods.
 *
 * <p>This class is thread-safe. However, to avoid "invariant stealing" clients should not call a
 * getter method n different thread.
 */
@Options(prefix = "invariantStore.import")
class FromDiskEntryProvider implements AutoCloseable {
  @Option(secure = true, description = "The directory where the invariants are stored.")
  @FileOption(FileOption.Type.OUTPUT_DIRECTORY)
  private Path storeDirectory = Path.of("invariantWitnesses");

  private final Queue<LoopInvariantEntry> loadedEntries;
  private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
  private final JavaType entryType;

  private WatchService watchService;

  private FromDiskEntryProvider(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
    loadedEntries = new ArrayDeque<>();

    entryType = mapper.getTypeFactory().constructCollectionType(List.class, AbstractEntry.class);
  }

  static FromDiskEntryProvider getNewFromDiskEntryProvider(Configuration pConfig)
      throws InvalidConfigurationException {
    return new FromDiskEntryProvider(pConfig);
  }

  /**
   * Returns an optional that contains the next available invariant or is empty if no invariant is
   * available.
   *
   * @return optional invariant
   * @throws IOException if accessing the invariant files fails
   */
  Optional<LoopInvariantEntry> getNext() throws IOException {
    synchronized (this) {
      if (!loadedEntries.isEmpty()) {
        return Optional.of(loadedEntries.remove());
      }
    }
    WatchKey key = getWatchService().poll();
    if (key == null) {
      return Optional.empty();
    }
    loadEntriesFromWatchKey(key);
    return getNext();
  }

  /**
   * Blocks until there is a new invariant available and returns it.
   *
   * @return next available invariant.
   * @throws IOException if accessing the invariant files fails
   */
  LoopInvariantEntry awaitNext() throws InterruptedException, IOException {
    synchronized (this) {
      if (!loadedEntries.isEmpty()) {
        return loadedEntries.remove();
      }
    }
    WatchKey key = getWatchService().take();
    loadEntriesFromWatchKey(key);
    return awaitNext();
  }

  private synchronized void loadEntriesFromWatchKey(WatchKey key) throws IOException {
    for (WatchEvent<?> event : key.pollEvents()) {
      if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
        // An "error" event that we can ignore
        continue;
      }

      // At this point we know that context is a path.
      Path newFilePath = (Path) event.context();
      File newFile = storeDirectory.resolve(newFilePath).toFile();
      loadEntries(newFile);
    }

    key.reset();
  }

  /**
   * Provides synchronized reads on this' watch service.
   *
   * @return this.watchService
   */
  private synchronized WatchService getWatchService() {
    return watchService;
  }

  synchronized void start() throws IOException {
    watchService = FileSystems.getDefault().newWatchService();
    storeDirectory.register(
        watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);

    // Load already present files
    try (DirectoryStream<Path> stream =
        Files.newDirectoryStream(storeDirectory, (p) -> p.toFile().isFile())) {
      for (Path file : stream) {
        loadEntries(file.toFile());
      }
    }
  }

  private synchronized void loadEntries(File entriesFile) throws IOException {
    List<AbstractEntry> entries = mapper.readValue(entriesFile, entryType);
    FluentIterable.from(entries).filter(LoopInvariantEntry.class).copyInto(loadedEntries);
  }

  @Override
  public void close() throws IOException {
    getWatchService().close();
  }
}
