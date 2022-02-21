// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.exchange.nio_network;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message.CompressedMessageConverter;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message.MessageConverter;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message.MessageType;

public class NetworkReceiver implements Closeable {

  private static final int BUFFER_SIZE = 1024;
  private final Selector selector;
  private final InetSocketAddress listenAddress;
  private final BlockingQueue<Message> sharedQueue;
  private final MessageConverter converter;
  private boolean finished;

  NetworkReceiver(
      BlockingQueue<Message> pSharedQueue,
      InetSocketAddress pAddress
  ) throws IOException {
    listenAddress = pAddress;
    sharedQueue = pSharedQueue;
    converter = new CompressedMessageConverter();
    selector = Selector.open();
    ServerSocketChannel serverChannel = ServerSocketChannel.open();
    serverChannel.configureBlocking(false);

    // retrieve server socket and bind to port
    serverChannel.socket().bind(listenAddress);
    serverChannel.register(selector, SelectionKey.OP_ACCEPT);
    Thread receiverThread = new Thread(() -> {
      try {
        startServer();
      } catch (IOException pE) {
        sharedQueue.add(Message.newErrorMessage("own", pE));
        Thread.currentThread().interrupt();
        throw new AssertionError(pE);
      }
    });
    receiverThread.setDaemon(true);
    receiverThread.start();
  }

  public BlockingQueue<Message> getSharedQueue() {
    return sharedQueue;
  }

  // create server channel
  public void startServer() throws IOException {
    while (!finished && selector.isOpen()) {
      // wait for events
      selector.select();

      //work on selected keys
      Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
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
    selector.close();
  }

  //accept a connection made to this channel's socket
  private void accept(SelectionKey key) throws IOException {
    ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
    SocketChannel channel = serverChannel.accept();
    channel.configureBlocking(false);

    // register channel with selector for further IO
    channel.register(selector, SelectionKey.OP_READ);
  }

  public InetSocketAddress getListenAddress() {
    return listenAddress;
  }

  //read from the socket channel
  private boolean read(SelectionKey key) throws IOException {
    try (SocketChannel channel = (SocketChannel) key.channel()) {
      ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

      int numRead = channel.read(buffer);

      if (numRead == -1) {
        channel.close();
        key.cancel();
        return false;
      }

      List<Byte> readBytes = new ArrayList<>();

      do {
        if (numRead > 0) {
          buffer.flip();
          byte[] read = new byte[numRead];
          buffer.get(read, 0, numRead);
          for (byte b : read) {
            readBytes.add(b);
          }
          buffer.compact();
        }
        if (numRead != BUFFER_SIZE) {
          break;
        }
        numRead = channel.read(buffer);
      } while (true);

      byte[] bytes = new byte[readBytes.size()];
      for (int i = 0; i < readBytes.size(); i++) {
        bytes[i] = readBytes.get(i);
      }
      Message received = converter.jsonToMessage(bytes);
      sharedQueue.add(received);
      return received.getType() == MessageType.FOUND_RESULT;
    } catch (Exception pE) {
      throw new IOException(pE);
    }
  }

  @Override
  public void close() throws IOException {
    finished = true;
  }
}
