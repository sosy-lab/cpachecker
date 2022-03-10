// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.classic_network;

import com.google.common.base.MoreObjects;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Message;


public class ClassicNetworkReceiver implements Closeable {

  private final ServerSocket socket;
  private final BlockingQueue<Message> blockingQueue;
  private final InetSocketAddress address;
  private boolean finished;

  public ClassicNetworkReceiver(BlockingQueue<Message> pQueue, int pPort) throws IOException {
    blockingQueue = pQueue;
    socket = new ServerSocket();
    socket.bind(address = new InetSocketAddress("127.0.0.1", pPort));
    finished = false;
    Thread server = new Thread(() -> {
      try {
        run();
      } catch (IOException pE) {
        finished = true;
      }
    });
    server.setDaemon(true);
    server.start();
  }

  public InetSocketAddress getAddress() {
    return address;
  }

  public BlockingQueue<Message> getBlockingQueue() {
    return blockingQueue;
  }

  public void run() throws IOException {
    while (!finished) {
      Socket clientSocket = socket.accept();
      Thread threadHandler = new Thread(new ClassicNetworkReceiveHandler(clientSocket, blockingQueue));
      threadHandler.setDaemon(true);
      threadHandler.start();
    }
  }

  @Override
  public void close() throws IOException {
    finished = true;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("address", address).toString();
  }
}
