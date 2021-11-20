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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.components.parallel.Message.MessageConverter;
import org.sosy_lab.cpachecker.core.algorithm.components.parallel.Message.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.components.util.MessageLogger;

public class WorkerSocket {

  private Selector selector;
  private final InetSocketAddress listenAddress;
  private final BlockingQueue<Message> sharedQueue;
  private final LogManager logger;
  private final String workerId;
  private final MessageConverter converter;

  private static final int BUFFER_SIZE = 1024;

  private WorkerSocket(
      LogManager pLogger,
      BlockingQueue<Message> pSharedQueue,
      InetSocketAddress pAddress,
      String pWorkerId) {
    listenAddress = pAddress;
    sharedQueue = pSharedQueue;
    logger = pLogger;
    workerId = pWorkerId;
    converter = new MessageConverter();
  }

  // create server channel
  public void startServer() throws IOException {
    selector = Selector.open();
    ServerSocketChannel serverChannel = ServerSocketChannel.open();
    serverChannel.configureBlocking(false);

    // retrieve server socket and bind to port
    serverChannel.socket().bind(listenAddress);
    serverChannel.register(selector, SelectionKey.OP_ACCEPT);

    logger.log(Level.INFO, "Server started...");

    while (true) {
      // wait for events
      selector.select();

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
          accept(key);
        } else if (key.isReadable()) {
          if (read(key)) {
            return;
          }
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
    channel.register(this.selector, SelectionKey.OP_READ);
  }

  //read from the socket channel
  private boolean read(SelectionKey key) throws IOException {
    SocketChannel channel = (SocketChannel) key.channel();
    ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

    int numRead = channel.read(buffer);

    if (numRead == -1) {
      Socket socket = channel.socket();
      SocketAddress remoteSocketAddress = socket.getRemoteSocketAddress();
      logger.log(Level.INFO, "Connection closed by client: " + remoteSocketAddress);
      channel.close();
      key.cancel();
      return false;
    }

    StringBuilder builder = new StringBuilder();

    do {
      if (numRead > 0) {
        buffer.flip();
        byte[] read = new byte[numRead];
        buffer.get(read, 0, numRead);
        builder.append(new String(read));
        buffer.compact();
      }
      if (numRead != BUFFER_SIZE) {
        break;
      }
      numRead = channel.read(buffer);
    } while(true);

    Message received = converter.jsonToMessage(builder.toString());
    sharedQueue.add(received);
    logger.log(Level.INFO, "Socket received message: " + received);
    return received.getType() == MessageType.FINISHED && workerId.equals(received.getUniqueBlockId());
  }

  public static class WorkerSocketFactory {

    private final Set<InetSocketAddress> addresses;

    public WorkerSocketFactory() {
      addresses = new HashSet<>();
    }

    public WorkerSocket makeSocket(
        LogManager pLogger,
        BlockingQueue<Message> pSharedQueue,
        String pWorkerId,
        String pAddress,
        int pPort) throws IOException {
      InetSocketAddress address = new InetSocketAddress(pAddress, pPort);
      addresses.add(address);
      return new WorkerSocket(pLogger, pSharedQueue, address, pWorkerId);
    }

    public Set<InetSocketAddress> getAddresses() {
      return ImmutableSet.copyOf(addresses);
    }
  }
}
