package org.sosy_lab.common;

import static com.google.common.base.Preconditions.checkState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.io.Closeables;

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
 * with the {@link #println(String)} method. Afterwards {@link #join()} has to be
 * called, which reads the output from the process and calls the handle* methods.
 * This method blocks, i.e. when it returns the process has terminated. Now the
 * get* methods may be used to get the output of the process.  
 *
 * @param <E> The type of the exceptions the handle* methods may throw.
 */
public class ProcessExecutor<E extends Exception> {

  private final String name;
  private final Class<E> exceptionClass;
  
  private final Process process;
  private final BufferedReader out;
  private final BufferedReader err;
  private final Writer in;
  
  private final ExecutorService executor = Executors.newFixedThreadPool(3);
  private final Future<?> outResult;
  private final Future<?> errResult;
  
  private final List<String> output = new ArrayList<String>();
  private final List<String> errorOutput = new ArrayList<String>();
  private int exitCode = -1;

  private volatile boolean stoppedByTimeout = false;
  private boolean finished = false;
  
  protected final LogManager logger;
  
  /**
   * Create an instance and immediately execute the supplied command.
   * 
   * It is strongly advised to call {@link #join()} sometimes, as otherwise
   * the exitCode of the process will not be handled and there may be resources
   * not being cleaned up properly. Also exceptions thrown by the handling
   * methods would get swallowed.
   * 
   * @see Runtime#exec(String[])
   * @param cmd The command with arguments to execute.
   * @throws IOException If the process cannot be executed.
   */
  public ProcessExecutor(LogManager logger, Class<E> exceptionClass, String... cmd) throws IOException {
    Preconditions.checkNotNull(cmd);
    Preconditions.checkArgument(cmd.length > 0);

    this.logger = Preconditions.checkNotNull(logger);
    this.exceptionClass = Preconditions.checkNotNull(exceptionClass);
    this.name = cmd[0];
    
    logger.log(Level.FINEST, "Executing", name);
    logger.log(Level.ALL, (Object[])cmd);
    
    process = Runtime.getRuntime().exec(cmd);
    out = new BufferedReader(new InputStreamReader(process.getInputStream()));
    err = new BufferedReader(new InputStreamReader(process.getErrorStream()));
    in = new OutputStreamWriter(process.getOutputStream());
    outResult = executor.submit(outCallable);
    errResult = executor.submit(errCallable);
  }
  
  /**
   * Write a String to the process. May only be called before {@link #join()}
   * was called, as afterwards the process is not running anymore.
   * @throws IOException 
   */
  public void println(String s) throws IOException {
    print(s + "\n");
  }
  
  /**
   * Write a String to the process. May only be called before {@link #join()}
   * was called, as afterwards the process is not running anymore.
   * @throws IOException 
   */
  public void print(String s) throws IOException {
    checkState(!finished, "Cannot write to process that has already terminated.");

    in.write(s);
    in.flush();
  }
  
  /**
   * Sends the EOF (end of file) signal to stdin of the process.
   * @throws IOException
   */
  public void sendEOF() throws IOException {
    checkState(!finished, "Cannot write to process that has already terminated.");

    in.close();
  }
  
  private final Callable<?> outCallable = new Callable<Void>() {
    @Override
    public Void call() throws E, IOException {
      String line;
      while ((line = out.readLine()) != null) {
        handleOutput(line);
      }
      return null;
    }
  };

  private final Callable<?> errCallable = new Callable<Void>() {
    @Override
    public Void call() throws E, IOException {
      String line;
      while ((line = err.readLine()) != null) {
        handleErrorOutput(line);
      }
      return null;
    } 
  };
  
  /**
   * Wait for the process to terminate and read all of it's output. Whenever a
   * line is read on stdout or stderr of the process, the {@link #handleOutput(String)}
   * or the {@link #handleErrorOutput(String)} are called respectively.
   * 
   * @param timelimit Maximum time to wait for process (in milliseconds)
   * @throws IOException
   * @throws E passed from the handle* methods.
   * @throws TimeoutException If timeout is hit.
   */
  public void join(final long timelimit) throws IOException, E, TimeoutException {
    checkState(!finished, "Cannot read from process that has already terminated.");

    Future<?> timeout = null;
    if (timelimit > 0) {
      timeout = executor.submit(new Callable<Void>() {
        @Override
        public Void call() throws InterruptedException {
          Thread.sleep(timelimit);

          stoppedByTimeout = true;
          process.destroy();
          logger.log(Level.WARNING, "Killing", name, "due to timeout");
          return null;
        }
      });
    }
    
    boolean interrupted = false;
    while (exitCode < 0) {
      try {
        logger.log(Level.FINEST, "Waiting for", name);
        exitCode = process.waitFor();
        logger.log(Level.FINEST, name, "has terminated");
        
      } catch (InterruptedException e) {
        logger.log(Level.WARNING, "Killing", name, "due to stop request");
        
        // remove stoppedByTimeout
        if (timeout != null) {
          timeout.cancel(true);
        }
        
        // kill process, and call waitFor() again so that we get the exitCode 
        process.destroy();
        
        interrupted = true;
      }
    }
    // now it's guaranteed that process.waitFor() has returned, i.e., the process is dead

    // cleanup the executor with the communication threads
    try {
      errResult.get();
      outResult.get();
    
    } catch (InterruptedException e) {
      interrupted = true;
    
    } catch (ExecutionException e) {
      Throwable t = e.getCause();
      Throwables.propagateIfPossible(t, IOException.class, exceptionClass);
      logger.logException(Level.SEVERE, t, "");
      assert false : "Callables threw undeclared checked exception, this is impossible!";

    } finally {
      executor.shutdownNow();
      Closeables.closeQuietly(out);
      Closeables.closeQuietly(err);
      Closeables.closeQuietly(in);
    }
    
    // other cleanup
    finished = true;

    if (interrupted) {
      Thread.currentThread().interrupt();
    }
    
    if (stoppedByTimeout) {
      // process was killed by stoppedByTimeout
      throw new TimeoutException();
    }

    // do this last, because we don't want to call it in case of timeout
    handleExitCode(exitCode);
  }
  
  /**
   * Wait for the process to terminate and read all of it's output. Whenever a
   * line is read on stdout or stderr of the process, the {@link #handleOutput(String)}
   * or the {@link #handleErrorOutput(String)} are called respectively.
   * 
   * @throws IOException
   * @throws E passed from the handle* methods.
   */
  public void join() throws IOException, E {
    try {
      join(0);
    } catch (TimeoutException e) {
      // cannot occur with timeout==0
      throw new AssertionError(e);
    }
  }
  
  /**
   * Handle one line of output from the process. This method may be overwritten
   * by clients. The default implementation logs the line on level ALL and adds it
   * to a list which may later be retrieved with {@link #getOutput()}. It never
   * throws an exception (but client implementations may do so).
   */
  protected void handleOutput(String line) throws E {
    logger.log(Level.ALL, name, "output:", line);
    output.add(line);
  }
  
  /**
   * Handle one line of stderr output from the process. This method may be overwritten
   * by clients. The default implementation logs the line on level WARNING and adds it
   * to a list which may later be retrieved with {@link #getErrorOutput()}. It never
   * throws an exception (but client implementations may do so).
   */
  protected void handleErrorOutput(String line) throws E {
    logger.log(Level.WARNING, name, "error output:", line);
    errorOutput.add(line);
  }
  
  /**
   * Handle the exit code of the process. This method may be overwritten
   * by clients. The default implementation logs the code on level WARNING, if
   * it is non-zero.
   */
  protected void handleExitCode(int code) throws E {
    if (code != 0) {
      logger.log(Level.WARNING, "Exit code from", name, "was", code);
    }
  }
  
  /**
   * Checks whether the process has finished already.
   * This is true exactly if {@link #join()} has been called.
   */
  public boolean isFinished() {
    return finished;
  }
  
  /**
   * Returns the complete output of the process.
   * May only be called after {@link #join()} has been called.
   */
  public List<String> getOutput() {
    checkState(finished, "Cannot get output while process is not yet finished");

    return output;
  }
  
  /**
   * Returns the complete output to stderr of the process.
   * May only be called after {@link #join()} has been called.
   */
  public List<String> getErrorOutput() {
    checkState(finished, "Cannot get error output while process is not yet finished");

    return errorOutput;
  }
  
  /**
   * Returns the exit code of the process.
   * May only be called after {@link #join()} has been called.
   */
  public int getExitCode() {
    checkState(finished, "Cannot get exit code while process is not yet finished");
    checkState(exitCode >= 0, "Cannot get exit code after the process timed out");
    
    return exitCode;
  }
}
