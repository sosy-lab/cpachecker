package org.sosy_lab.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;


import com.google.common.base.Preconditions;

/**
 * This class can be used to execute a separate process and read it's output in
 * a convenient way. It is only useful for processes which handle only one task
 * and exit afterwards.
 * 
 * This class is not thread-safe, it assumes that never two of it's methods are
 * executed simultaneously.
 * 
 * When an instance of this class is created, the corresponding process is
 * started immediately. Then some text may be written to stdin of the process
 * with the {@link #println(String)} method. Afterwards {@link #read()} has to be
 * called, which reads the output from the process and calls the handle* methods.
 * This method blocks, i.e. when it returns the process has terminated. Now the
 * get* methods may be used to get the output of the process.  
 *
 * @param <E> The type of the exceptions the handle* methods may throw.
 */
public class ProcessExecutor<E extends Exception> {

  private final String name;
  
  private final Process process;
  private final BufferedReader out;
  private final BufferedReader err;
  private final PrintWriter in;
  
  private final List<String> output = new ArrayList<String>();
  private final List<String> errorOutput = new ArrayList<String>();
  private int exitCode = 0;
  
  private boolean finished = false;
  
  protected final LogManager logger;
  
  /**
   * Create an instance and immediately execute the supplied command.
   * 
   * @see Runtime#exec(String[])
   * @param cmd The command with arguments to execute.
   * @throws IOException If the process cannot be executed.
   */
  public ProcessExecutor(LogManager logger, String... cmd) throws IOException {
    Preconditions.checkNotNull(logger);
    Preconditions.checkNotNull(cmd);
    Preconditions.checkArgument(cmd.length > 0);

    this.logger = logger;
    this.name = cmd[0];
    
    logger.log(Level.ALL, "Executing", cmd);
    
    process = Runtime.getRuntime().exec(cmd);
    out = new BufferedReader(new InputStreamReader(process.getInputStream()));
    err = new BufferedReader(new InputStreamReader(process.getErrorStream()));
    in = new PrintWriter(process.getOutputStream());
  }
  
  /**
   * Write a String to the process. May only be called before {@link #read()}
   * was called, as afterwards the process is not running anymore.
   */
  public void println(String s) {
    if (finished) {
      throw new IllegalStateException("Cannot write to process that has already terminated.");
    }
    in.println(s);
    in.flush();
  }
  
  /**
   * Wait for the process to terminate and read all of it's output. Whenever a
   * line is read on stdout or stderr of the process, the {@link #handleOutput(String)}
   * or the {@link #handleErrorOutput(String)} are called respectively.
   * 
   * @throws IOException
   * @throws E passed from the handle* methods.
   */
  public void read() throws IOException, E {
    if (finished) {
      throw new IllegalStateException("Cannot read from process that has already terminated.");
    }
    try {
      String line;
      while ((line = out.readLine()) != null) {
        handleOutput(line);
      }
  
      while ((line = err.readLine()) != null) {
        handleErrorOutput(line);
      }
    } finally {
      process.destroy();
      try {
        out.close();
        err.close();
        in.close();
      } catch (IOException e) {
        // ignore errors here
      }
      exitCode = process.exitValue();
      finished = true;
      
      handleExitCode(exitCode);
    }
  }
  
  /**
   * Handle one line of output from the process. This method may be overwritten
   * by clients. The default implementation logs the line on level ALL and adds it
   * to a list which may later be retrieved with {@link #getOutput()}. It never
   * throws an exception (but client implementations may do so).
   */
  public void handleOutput(String line) throws E {
    logger.log(Level.ALL, name, "output:", line);
    output.add(line);
  }
  
  /**
   * Handle one line of stderr output from the process. This method may be overwritten
   * by clients. The default implementation logs the line on level WARNING and adds it
   * to a list which may later be retrieved with {@link #getErrorOutput()}. It never
   * throws an exception (but client implementations may do so).
   */
  public void handleErrorOutput(String line) throws E {
    logger.log(Level.WARNING, name, "error output:", line);
    errorOutput.add(line);
  }
  
  /**
   * Handle the exit code of the process. This method may be overwritten
   * by clients. The default implementation logs the code on level WARNING, if
   * it is non-zero.
   */
  public void handleExitCode(int code) throws E {
    if (code != 0) {
      logger.log(Level.WARNING, "Exit code from", name, "was", code);
    }
  }
  
  /**
   * Checks whether the process has finished already.
   * This is true exactly if {@link #read()} has been called.
   */
  public boolean isFinished() {
    return finished;
  }
  
  /**
   * Returns the complete output of the process.
   * May only be called after {@link #read()} has been called.
   */
  public List<String> getOutput() {
    if (!finished) {
      throw new IllegalStateException("Cannot get output while process is not yet finished");
    }
    return output;
  }
  
  /**
   * Returns the complete output to stderr of the process.
   * May only be called after {@link #read()} has been called.
   */
  public List<String> getErrorOutput() {
    if (!finished) {
      throw new IllegalStateException("Cannot get error output while process is not yet finished");
    }
    return errorOutput;
  }
  
  /**
   * Returns the exit code of the process.
   * May only be called after {@link #read()} has been called.
   */
  public int getExitCode() {
    if (!finished) {
      throw new IllegalStateException("Cannot get exit code while process is not yet finished");
    }
    return exitCode;
  }
}
