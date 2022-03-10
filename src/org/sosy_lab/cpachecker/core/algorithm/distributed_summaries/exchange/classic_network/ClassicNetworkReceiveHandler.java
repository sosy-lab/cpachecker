// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.classic_network;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Message.MessageConverter;


public class ClassicNetworkReceiveHandler implements Runnable {

  private final Socket clientSocket;
  private final MessageConverter converter;
  private final BlockingQueue<Message> queue;

  public ClassicNetworkReceiveHandler(Socket pClientSocket, BlockingQueue<Message> pQueue) {
    clientSocket = pClientSocket;
    converter = new MessageConverter();
    queue = pQueue;
  }

  @Override
  public void run() {
    try {
      DataInputStream input = new DataInputStream(clientSocket.getInputStream());
      ByteArrayOutputStream output = new ByteArrayOutputStream();

      byte[] buf = new byte[4096];
      while(true) {
        int n = input.read(buf);
        if( n < 0 ) {
          break;
        }
        output.write(buf,0,n);
      }

      byte[] data = output.toByteArray();
      queue.put(converter.jsonToMessage(data));
    } catch (IOException | InterruptedException pE) {
      try {
        queue.put(Message.newErrorMessage("-", pE));
        throw new AssertionError(pE);
      } catch (InterruptedException pEx) {
        // ignore
      }
    }
  }
}
