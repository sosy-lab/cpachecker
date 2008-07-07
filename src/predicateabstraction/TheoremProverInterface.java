package predicateabstraction;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class TheoremProverInterface {
	
	private static final long serialVersionUID = 112L;
	static String answer = "";

	public TheoremProverInterface(String query)
	{
		initProcess(query);
	}

	public static String satis(String query) {
		new TheoremProverInterface(query);
		return answer;
	}
	
	public static ThreeValuedBoolean sat(String query) {
		String ans = satis(query);
		//System.out.println(ans);
		if(ans.equalsIgnoreCase("satisfiable")){
			return ThreeValuedBoolean.TRUE;
		}
		else if(ans.equalsIgnoreCase("unsatisfiable")){
			return ThreeValuedBoolean.FALSE;
		}
		
		assert(false);
		//System.out.println(query);
		return ThreeValuedBoolean.DONTKNOW;
	}

	public void initProcess(String query)
	{
		try
		{
			//FileOutputStream fos = new FileOutputStream(fileLocation);
			String str = "/home/erkan/csisat/bin/csisat -sat";
			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(str);
			StreamHandler errorHandler = new StreamHandler(proc.getErrorStream());            

			// any output?
			StreamHandler outputHandler = new StreamHandler(proc.getInputStream());

			// kick them off
			errorHandler.start();
			outputHandler.start();
			
			OutputStream ostream = proc.getOutputStream();
			
			// TODO we're processing the String in UTF-8
			// maybe we should know about the character encoding of the system
            InputStream istream = new ByteArrayInputStream(query.getBytes("UTF-8")); 
            //new FileInputStream(fileLocation);
            byte[] buffer = new byte[4096];
            for (int count = 0; (count = istream.read(buffer)) >= 0;)
            {
                ostream.write(buffer, 0, count);
            }

            ostream.close();
            istream.close();

			// any error?
			int exitVal = proc.waitFor();
			//System.out.println("ExitValue: " + exitVal);
			if(exitVal != 0){
				// TODO exception
				System.out.println("ExitValue: " + exitVal);
				System.exit(0);
			}
//			fos.flush();
//			fos.close();  

		} catch (Throwable t)
		{
			t.printStackTrace();
		}
	}

	class StreamHandler extends Thread
	{
		InputStream is;
		OutputStream os;

		StreamHandler(InputStream is)
		{
			this(is, null);
		}

		StreamHandler(InputStream is, OutputStream redirect)
		{
			this.is = is;
			//this.type = type;
			this.os = redirect;
		}

		public void run()
		{
			try
			{
				//PrintWriter pw = null;
//				if (os != null)
//					pw = new PrintWriter(os);

				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line;
				while ( (line = br.readLine()) != null){
//					if (pw != null)
//						pw.println(line);
					answer = line;
				}
				isr.close();
				br.close();
				
//				if (pw != null)
//					pw.flush();

			} catch (IOException ioe)
			{
				ioe.printStackTrace();  
			}
		}
	}
	
	public static ThreeValuedBoolean satisfiability(String query){
		//createFile(query);
		//ThreeValuedBoolean result = TheoremProverInterface.sat(CPAConfig.tempTheoremProverFile);
		ThreeValuedBoolean result = TheoremProverInterface.sat(query);
		//System.out.println("result: === " + result);
		return result;
	}

	// returns true if r1 ==> r2
	public static ThreeValuedBoolean implies(String r1, String r2) throws IOException{
//		System.out.println("======> " + r1 + " implies " + r2);
		String s;
		s = "~ | [ " + "~ " + r1 + " " + r2 + " ]";
//		System.out.println(s);
		return negate(satisfiability(s));
		//return ThreeValuedBoolean.FALSE;
	}
	
	private static ThreeValuedBoolean negate(ThreeValuedBoolean res) {
		if(res == ThreeValuedBoolean.TRUE){
			return ThreeValuedBoolean.FALSE;
		}
		else if(res == ThreeValuedBoolean.FALSE){
			return ThreeValuedBoolean.TRUE;
		}
		return ThreeValuedBoolean.DONTKNOW;
	}
}
