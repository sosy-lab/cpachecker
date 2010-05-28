package org.sosy_lab.cpachecker.fllesh.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class AutomaticStreamReader implements Runnable {

  private InputStream mStream;
  private StringBuffer mBuffer;

  public AutomaticStreamReader(InputStream pStream) {
    assert(pStream != null);

    mStream = pStream;
    mBuffer = new StringBuffer();
  }

  @Override
  public void run() {
    mBuffer.delete(0, mBuffer.length());

    BufferedReader lReader = new BufferedReader(new InputStreamReader(mStream));

    String lLine = null;

    try {
      while ((lLine = lReader.readLine()) != null) {
        mBuffer.append(lLine);
        mBuffer.append("\n");
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public String getInput() {
    return mBuffer.toString();
  }

}
