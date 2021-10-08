// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.parallel;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;

public class WorkerSocket {

  private Selector selector;
  private final Map<SocketChannel, List<byte[]>> dataMapper;
  private final InetSocketAddress listenAddress;
  private final BlockingQueue<Message> sharedQueue;
  private final LogManager logger;

  private WorkerSocket(
      LogManager pLogger,
      BlockingQueue<Message> pSharedQueue,
      InetSocketAddress pAddress) {
    listenAddress = pAddress;
    dataMapper = new HashMap<>();
    sharedQueue = pSharedQueue;
    logger = pLogger;
  }

  // create server channel
  public void startServer() throws IOException {
    this.selector = Selector.open();
    ServerSocketChannel serverChannel = ServerSocketChannel.open();
    serverChannel.configureBlocking(false);

    // retrieve server socket and bind to port
    serverChannel.socket().bind(listenAddress);
    serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);

    logger.log(Level.INFO, "Server started...");

    while (true) {
      // wait for events
      this.selector.select();

      //work on selected keys
      Iterator<SelectionKey> keys = this.selector.selectedKeys().iterator();
      while (keys.hasNext()) {
        SelectionKey key = keys.next();

        // this is necessary to prevent the same key from coming up
        // again the next time around.
        keys.remove();

        if (!key.isValid()) {
          continue;
        }

        if (key.isAcceptable()) {
          this.accept(key);
        }
        else if (key.isReadable()) {
          this.read(key);
        }
      }
    }
  }

  //accept a connection made to this channel's socket
  private void accept(SelectionKey key) throws IOException {
    ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
    SocketChannel channel = serverChannel.accept();
    channel.configureBlocking(false);
    Socket socket = channel.socket();
    SocketAddress remoteAddr = socket.getRemoteSocketAddress();
    logger.log(Level.INFO, "Connected to: " + remoteAddr);

    // register channel with selector for further IO
    dataMapper.put(channel, new ArrayList<>());
    channel.register(this.selector, SelectionKey.OP_READ);
  }

  //read from the socket channel
  private void read(SelectionKey key) throws IOException {
    SocketChannel channel = (SocketChannel) key.channel();
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    int numRead = channel.read(buffer);

    if (numRead == -1) {
      this.dataMapper.remove(channel);
      Socket socket = channel.socket();
      SocketAddress remoteSocketAddress = socket.getRemoteSocketAddress();
      logger.log(Level.INFO, "Connection closed by client: " + remoteSocketAddress);
      channel.close();
      key.cancel();
      return;
    }

    byte[] data = new byte[numRead];
    System.arraycopy(buffer.array(), 0, data, 0, numRead);
    Message received = Message.decode(new String(data));
    sharedQueue.add(received);
    logger.log(Level.INFO, "Socket received message: " + received);
  }

  public static class WorkerSocketFactory {

    private final Set<InetSocketAddress> addresses;

    public WorkerSocketFactory() {
      addresses = new HashSet<>();
    }

    public WorkerSocket makeSocket(
        LogManager pLogger,
        BlockingQueue<Message> pSharedQueue,
        String pAddress,
        int pPort) throws IOException {
      InetSocketAddress address = new InetSocketAddress(pAddress, pPort);
      addresses.add(address);
      return new WorkerSocket(pLogger, pSharedQueue, address);
    }

    public Set<InetSocketAddress> getAddresses() {
      return ImmutableSet.copyOf(addresses);
    }
  }
}
