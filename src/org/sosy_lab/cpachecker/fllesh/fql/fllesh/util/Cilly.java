package org.sosy_lab.cpachecker.fllesh.fql.fllesh.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/*
 * This class provides basic access to some functionality provided by the CIL infrastructure.
 */

public class Cilly {
  
  private class AutomaticStreamReader implements Runnable {
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
  
  public static void main(String[] pArguments) throws IOException {
    assert(pArguments != null);
    
    if (pArguments.length == 0) {
      throw new IllegalArgumentException("You have to specify a source file!");
    }
    
    String lNiceCILName = Cilly.getNiceCILName(pArguments[0]);
    
    Cilly lCilly = new Cilly();
    
    lCilly.cillyfy(pArguments[0], lNiceCILName);
    
    System.out.println("Wrote output to " + lNiceCILName);
  }
  
  //private File mCillyFile;
  private String mCillyAsmExePath;
  
  // TODO: make set flags explicit via method calls?
  private boolean mDoSimplify = true;
  private boolean mPrintCilAsIs = true;
  // TODO: irrelevant for cilly.asm.exe
  // TODO: why is this option requested by HowTo.txt?
  //private boolean mSaveTemps = true;
  private boolean mDoMakeCFG = true;
  private boolean mDoSimpleMem = false;
  
  public Cilly() {
    this("cilly.asm.exe");
  }
  
  public Cilly(String pCillyAsmExePath) {
    assert(pCillyAsmExePath != null);
    
    //mCillyFile = new File(pCillyAsmExePath);
    
    mCillyAsmExePath = pCillyAsmExePath;
    
    /*assert(mCillyFile.exists());
    assert(mCillyFile.canExecute());*/
  }
  
  public void setDoSimplify(boolean pValue) {
    mDoSimplify = pValue;
  }
  
  public void setPrintCilAsIs(boolean pValue) {
    mPrintCilAsIs = pValue;
  }
  
  /*public void setSaveTemps(boolean pValue) {
    mSaveTemps = pValue;
  }*/
  
  public void setDoMakeCFG(boolean pValue) {
    mDoMakeCFG = pValue;
  }
  
  public void setDoSimpleMem(boolean pValue) {
    mDoSimpleMem = pValue;
  }
  
  public boolean isCillyInvariant(String pSourceFile) throws IOException {
    assert(pSourceFile != null);
    
    return isCillyInvariant(new File(pSourceFile));
  }
  
  public boolean isCillyInvariant(File pSourceFile) throws IOException {
    assert(pSourceFile != null);
    assert(pSourceFile.exists());
    assert(pSourceFile.canRead());
    
    File lCillyfiedFile = cillyfy(pSourceFile);
    
    BufferedReader lSourceReader = new BufferedReader(new FileReader(pSourceFile));
    
    BufferedReader lCillyfiedReader = new BufferedReader(new FileReader(lCillyfiedFile));
    
    do {
      String lSourceLine = lSourceReader.readLine();
      String lCillyfiedLine = lCillyfiedReader.readLine();
      
      if (lSourceLine == null) {
        return (lCillyfiedLine == null);
      }
      else if (!lSourceLine.trim().equals(lCillyfiedLine.trim())) {
        //System.out.println(lSourceLine);
        //System.out.println(lCillyfiedLine);
        
        return false;
      }
    }
    while (true);
  }
  
  public File cillyfy(String pSourceFile) throws IOException {
    assert(pSourceFile != null);
    
    return cillyfy(new File(pSourceFile));
  }
  
  public static String getNiceCILName(String pFileName) {
    assert(pFileName != null);
    
    int lLastIndex = pFileName.lastIndexOf(".");
    
    String lPrefix;
    String lPostfix;
    
    if (lLastIndex < 0) {
      lPrefix = pFileName;
      lPostfix = "c";
    }
    else {
      // TODO what if lLastIndex == 0?
      
      lPrefix = pFileName.substring(0, lLastIndex);
      lPostfix = pFileName.substring(lLastIndex + 1, pFileName.length());
    }
    
    return lPrefix + ".cil." + lPostfix;
  }
  
  public File cillyfy(File pSourceFile) throws IOException {
    assert(pSourceFile != null);
    
    String lSourceFileString = pSourceFile.getName();
    
    int lLastIndex = lSourceFileString.lastIndexOf(".");
    
    String lPrefix;
    String lPostfix;
    
    if (lLastIndex < 0) {
      lPrefix = lSourceFileString;
      lPostfix = "c";
    }
    else {
      // TODO what if lLastIndex == 0?
      
      lPrefix = lSourceFileString.substring(0, lLastIndex);
      lPostfix = lSourceFileString.substring(lLastIndex + 1, lSourceFileString.length());
    }
    
    File lTargetFile = File.createTempFile(lPrefix + ".cil.", "." + lPostfix);
    
    cillyfy(pSourceFile, lTargetFile);
    
    return lTargetFile;
  }
  
  public File cillyfy(String pSourceFile, String pTargetFile) throws IOException {
    assert(pSourceFile != null);
    assert(pTargetFile != null);
    
    File lTargetFile = new File(pTargetFile);
    
    cillyfy(new File(pSourceFile), lTargetFile);
    
    return lTargetFile;
  }
  
  public void cillyfy(File pSourceFile, File pTargetFile) throws IOException {
    assert(pSourceFile != null);
    assert(pSourceFile.exists());
    assert(pSourceFile.canRead());
    
    assert(pTargetFile != null);
    
    StringBuffer lOptionsString = new StringBuffer();
    
    if (mDoSimplify) {
      lOptionsString.append(" --dosimplify");
    }
    
    if (mPrintCilAsIs) {
      lOptionsString.append(" --printCilAsIs");
    }
    
    // TODO: irrelevant for cilly.asm.exe
    // TODO: why is this option requested by HowTo.txt?
    /*if (mSaveTemps) {
      lOptionsString.append(" --save-temps");
    }*/
    
    if (mDoMakeCFG) {
      lOptionsString.append(" --domakeCFG");
    }
    
    if (mDoSimpleMem) {
      lOptionsString.append(" --dosimpleMem");
    }
    
    lOptionsString.append(" --out ");
    lOptionsString.append(pTargetFile.toString());
    
    String lExecString = mCillyAsmExePath + lOptionsString.toString() + " " + pSourceFile.getAbsolutePath();
    
    //System.out.println("EXEC STRING: " + lExecString);
    
    Process lCillyProcess = Runtime.getRuntime().exec(lExecString);            

    AutomaticStreamReader lInputReader = new AutomaticStreamReader(lCillyProcess.getInputStream());
    Thread lInputReaderThread = new Thread(lInputReader);
    lInputReaderThread.start();
    
    AutomaticStreamReader lErrorReader = new AutomaticStreamReader(lCillyProcess.getErrorStream());
    Thread lErrorReaderThread = new Thread(lErrorReader);
    lErrorReaderThread.start();
    
    try {
      lCillyProcess.waitFor();
      lInputReaderThread.join();
      lErrorReaderThread.join();
      
      //System.out.println(lInputReader.getInput());
      //System.out.println(lErrorReader.getInput());
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
  
}
