package org.sosy_lab.cpachecker.fshell.experiments;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

public class ExperimentsRunner {

	private static String CLASSPATH =
		"bin:lib/sigar.jar:lib/guava-r09.jar:" +
		"lib/icu4j-4_2_1.jar:" +
		"lib/javabdd-1.0b2.jar:" +
		"lib/java-cup-11a.jar:" +
		"lib/JFlex.jar:" +
		"lib/jgrapht-jdk1.6.jar:" +
		"lib/mathsat.jar:" +
		"lib/eclipse/org.eclipse.cdt.core_5.2.1.201102110609.jar:" +
		"lib/eclipse/org.eclipse.core.resources_3.6.1.R36x_v20110131-1630.jar:" +
		"lib/eclipse/org.eclipse.equinox.common_3.6.0.v20100503.jar:" +
		"lib/eclipse/org.eclipse.osgi_3.6.2.R36x_v20110210.jar:" +
		"lib/eclipse/org.hamcrest.core_1.1.0.v20090501071000.jar:" +
		"lib/eclipse-cdt6-parser.jar:" +
		"lib/json_simple-1.1.jar:.";

	public static class Configuration {

		public static Configuration read(File pConfigurationFile) throws IOException {
			BufferedReader lReader = new BufferedReader(new FileReader(pConfigurationFile));


			String lFQLQuery = lReader.readLine();

			if (lFQLQuery == null) {
				throw new RuntimeException();
			}

			String lSourceFile = lReader.readLine();

			if (lSourceFile == null) {
				throw new RuntimeException();
			}

			String lEntryFunction = lReader.readLine();

			if (lEntryFunction == null) {
				throw new RuntimeException();
			}

			LinkedList<String> lArguments = new LinkedList<String>();

			String lLine;

			while ((lLine = lReader.readLine()) != null) {
				if (lLine.trim().equals("") || lLine.startsWith("#")) {
					continue;
				}

				lArguments.add(lLine);
			}

			return new Configuration(lFQLQuery, lSourceFile, lEntryFunction, lArguments);
		}

		private final String mFQLQuery;
		private final String mSourceFile;
		private final String mEntryFunction;
		private final List<String> mArguments;

		public Configuration(String pFQLQuery, String pSourceFile, String pEntryFunction, List<String> pArguments) {
			mFQLQuery = pFQLQuery;
			mSourceFile = pSourceFile;
			mEntryFunction = pEntryFunction;
			mArguments = new LinkedList<String>(pArguments);
		}

		public String getFQLQuery() {
			return mFQLQuery;
		}

		public String getSourceFile() {
			return mSourceFile;
		}

		public String getEntryFunction() {
			return mEntryFunction;
		}

		public List<String> getArguments() {
			return mArguments;
		}

		public List<String> toCommand() {
			LinkedList<String> lCommands = new LinkedList<String>();

			lCommands.add(this.getFQLQuery());
			lCommands.add(this.getSourceFile());
			lCommands.add(this.getEntryFunction());
			lCommands.addAll(this.getArguments());

			return lCommands;
		}

		@Override
		public String toString() {
			return toCommand().toString();
		}

	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, InterruptedException {

	  boolean lUse64BitSetting = false;

	  LinkedList<String> lTmpArguments = new LinkedList<String>();

	  for (String lArgument : args) {
	    if (lArgument.equals("--32")) {
	      lUse64BitSetting = false;
	    }
	    else {
	      lTmpArguments.add(lArgument);
	    }
	  }

	  if (lTmpArguments.size() != args.length) {
	    String[] lTmpArray = new String[lTmpArguments.size()];

	    lTmpArguments.toArray(lTmpArray);

	    args = lTmpArray;
	  }

		ProcessBuilder lBuilder = new ProcessBuilder();

		Configuration lConfiguration = Configuration.read(new File(args[0]));

		LinkedList<String> lCommand = new LinkedList<String>();
		lCommand.add("java");
		if (lUse64BitSetting) {
		  lCommand.add("-Xms10240M");
	    	  lCommand.add("-Xmx10240M");
	    lCommand.add("-Djava.library.path=lib/native/x86_64-linux");
		}
		else {
		  lCommand.add("-Xms2560M");
	    	  lCommand.add("-Xmx2560M");
	    	  lCommand.add("-Djava.library.path=lib/native/x86-linux");
		}
		lCommand.add("-cp");
		lCommand.add(ExperimentsRunner.CLASSPATH);
		lCommand.add("org.sosy_lab.cpachecker.fshell.Main");
		lCommand.addAll(lConfiguration.toCommand());

		lBuilder.redirectErrorStream(true);
		lBuilder.command(lCommand);

		long lTimeStampStart = System.currentTimeMillis();

		Process lProcess = lBuilder.start();


		BufferedReader lInput = new BufferedReader(new InputStreamReader(lProcess.getInputStream()));

    String lLine = null;


    PrintStream lPrintStream = null;

    if (args.length == 1) {
      lPrintStream = System.out;
    }
    else {
      lPrintStream = new PrintStream(new BufferedOutputStream(new FileOutputStream(args[1])));
    }


    while ((lLine = lInput.readLine()) != null) {
	    	lPrintStream.println(lLine);
    }

		lProcess.waitFor();

		long lTimeStampEnd = System.currentTimeMillis();

		lPrintStream.println(args[0]);
		lPrintStream.println("Time: " + (lTimeStampEnd - lTimeStampStart)/1000.0 + " s");
		lPrintStream.println("Bye.");

		lPrintStream.close();
	}

}
