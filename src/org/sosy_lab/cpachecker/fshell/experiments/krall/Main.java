package org.sosy_lab.cpachecker.fshell.experiments.krall;


import java.util.Map.Entry;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.HashMap;
import java.util.Set;

enum fun{
	VOID (1,"void");
	private int size;
	
	fun(int a, String s){

		size =a;
		
	}
	int getSize(){
		return size;
	}
}

public class Main {

	/**
	 * @param args
	 */
	
	enum OP{
		PLUS,
		MINUS,
		MULITPLY,
		DIVIDE;
		
		int performop(int a, int b){
			switch(this){
			case PLUS:
				return a+b;
			case MINUS:
				return a-b;
			case MULITPLY:
				return a*b;
			case DIVIDE:
				return a/b;
			}
			return 0;
		}
	}
	
	
	

	public static void main(String[] args){

		HashMap<Integer,Integer> PMmap = new HashMap<Integer, Integer>();
		HashMap<Integer,Integer> Smap = new HashMap<Integer, Integer>();
		HashMap<Integer,Integer> MBmap = new HashMap<Integer, Integer>();
		HashMap<Integer,Integer> Amap = new HashMap<Integer, Integer>();
		HashMap<Integer,Integer> Dmap = new HashMap<Integer, Integer>();
		HashMap<Integer,Integer> Fmap = new HashMap<Integer, Integer>();
		HashMap<Integer,Integer> ref=null;
		HashMap<?, ?> test[]={PMmap,Smap,MBmap,Amap,Dmap,Fmap};
	
		Integer data;
		String input;
		int status=0;
		int step[]={5,10,20,50,100};
		
		File f = new File("/home/krall/workspace/CPAchecker/test/programs/fql/result/Hist");
		try {
			for(File s:f.listFiles()){
				int max_x[] = {0,0,0,0,0,0};
				int max_y[] ={1,1,1,1,1,1};
				status=0;
				if(s.isFile()==true && s.isHidden()==false){
					System.out.print(s.getName()+"\n");
					FileReader t = new FileReader(s);
					BufferedReader tt = new BufferedReader(t);
					while(tt.ready()){
						input = tt.readLine();
						if(input==null){
							continue;
						}

						if(input.compareTo( "---")==0){
							status++;
							continue;
						}
						
						data = Integer.parseInt(input);
						if(data>max_x[status]){
							max_x[status]=data;
						}
						ref = (HashMap<Integer,Integer>)test[status];
						if(ref.containsKey(data)==true){
							Integer k = ref.get(data)+1;
							if(k>max_y[status]){
								max_y[status]=k;
							}
							ref.put(data, k);
						}else{
							ref.put(data, 1);
						}
							
						
						
					}
					
					DecimalFormat form = new DecimalFormat("#.###");
					int y;
					int z;
					
					
					for(y=0;y<6;y++){
						FileWriter fo = new FileWriter(s.getName()+"_"+y+".tex");
						PrintWriter out = new PrintWriter(fo);
						
						out.println("\\documentclass[a4paper,10pt]{article}\n");
						out.println("\\usepackage{pgf}");					
						out.println("\\usepackage{tikz}");
						out.println("\\usetikzlibrary{arrows,automata}");

						out.println("\\begin{document}");
						out.println("\\scalebox{0.8}{");
						out.println("\\begin{tikzpicture}[->,>=stealth',shorten >=0.5pt,auto,node distance=2cm, semithick]");
						                   
						out.println("\\tikzstyle{every state}=[fill=white,draw=black,text=black]");
						out.println("\\draw  [->](0.0,0)->(0.0,9);");
						out.println("\\draw [->](0,0)->(16,0);");

						out.println("\\coordinate [label=-90:\\small{Depth}] (A14) at (16,-0.1);");
						out.println("\\coordinate [label=-180:\\small{Quantity}] (A14) at (-0.1,8.7);");
						
							
						int st=1;
				
						int labelx;
				
						
						
						if(max_x[y]>10 && max_x[y]<=50){
							st = 5;
						}
						if(max_x[y]>50 && max_x[y]<=100){
							st = 10;
						}
						if(max_x[y]>100 && max_x[y]<=300){
							st =20;
						}
						if(max_x[y]>300 && max_x[y]<=700){
							st=50;
						}
						if(max_x[y]>700){
							st = 100;
						}
						
						labelx = ((max_x[y]/st)+1)*st;
						int k = ((max_x[y]/st)+1);
						double stx=0;
						String stmp="";
						stx= (double)15/k;
						for(int v =0; v<=k;v++){
							out.println("\\coordinate [label=-90:$"+st*v+"$] (A"+v+") at ("+form.format(v*stx)+",-0.1);");
						}
						
						out.print("\\foreach \\g in {");
						for(int v =0; v<=k;v++){
							stmp=stmp.concat("(A"+v+"),");
						}
						
						stmp=stmp.substring(0, stmp.length()-1);
						out.print(stmp);
						out.println("}\n{ \\draw [-] \\g+(0,0.1)-> \\g;\n}");
						
						
						if(max_y[y]>10 && max_y[y]<=50){
							st = 5;
						}
						if(max_y[y]>50 && max_y[y]<=100){
							st = 10;
						}
						if(max_y[y]>100 && max_y[y]<=300){
							st =20;
						}
						if(max_y[y]>300 && max_y[y]<=700){
							st=50;
						}
						if(max_y[y]>700){
							st = 100;
						}
						
						
						
			
						k = ((max_y[y]/st)+1);
						stx=0;
						 stmp="";
						stx= (double)8/k;
						for(int v =0; v<=k;v++){
							out.println("\\coordinate [label=-180:$"+st*v+"$] (B"+v+") at (-0.1,"+form.format(v*stx)+");");
						}
						
						out.print("\\foreach \\g in {");
						for(int v =0; v<=k;v++){
							stmp=stmp.concat("(B"+v+"),");
						}
						
						stmp=stmp.substring(0, stmp.length()-1);
						out.print(stmp);
						out.println("}\n{ \\draw [-] \\g+(0.1,0)-> \\g;\n}");
						
						
						
						
						stmp="";
						out.print("\\foreach \\k in {");
						for(z=0;z<=max_x[y];z++){
							ref = (HashMap<Integer,Integer>)test[y];
							if(ref.containsKey(z)){
								double tmp,tmp2;
								tmp = (z*15); 
								tmp=(double)tmp/labelx;
								tmp2 = (ref.get(z)*8); 
								tmp2=(double)tmp2/max_y[y];
								stmp = stmp.concat("("+form.format(tmp)+","+form.format(tmp2)+"),");
							}
						}
						stmp=stmp.substring(0,stmp.length()-1);
						out.print(stmp+"}");
						
						
						out.println("{  \\fill[black] \\k circle (0.5pt);");
						out.println("\\draw [-]\\k + (-0.05,-0.05) -> \\k ;");
						out.println("\\draw [-]\\k +(0.05,0.05)->\\k;");
						out.println("\\draw [-]\\k + (-0.05,+0.05) -> \\k ;");
						out.println("\\draw [-]\\k +(0.05,-0.05)->\\k;}");
						out.println("\\end{tikzpicture}}");



						out.println("\\end{document}");
						out.close();
						
					
						
					}
					
					for(HashMap<?, ?>r : test){
						r.clear();
						
					}
					
					
					
				}
			}
		}catch(Exception e){
	
			e.printStackTrace();
		}
		
		
	
		
	}
	
	
	public static void main2(String[] args) {
		HashMap<String, Integer[]> x;
		Integer v[] =  new Integer[20];
		x = new HashMap<String,Integer[]>();
		long k2,k1,t1,t2;

		t1 = System.nanoTime();
		k1= System.currentTimeMillis();
		//System.out.println();
		

		v[1]= 33;
		k2= System.currentTimeMillis();
		t2 = System.nanoTime();
	
		File f = new File("/home/krall/workspace/CPAchecker/test/programs/fql/result/");
		System.out.println(f.exists());

		try {
			for(File s:f.listFiles()){
				if(s.isFile()==true && s.isHidden()==false){
					
					FileReader t = new FileReader(s);
					BufferedReader tt = new BufferedReader(t);
					String numbers[];
					int data1[]=new int[1000];
					int data2[]=new int[1000];
					int z=0;
					int max1=0,max2=0;
					int min1=10000000,  min2=10000000;
					while(tt.ready()){
						numbers = tt.readLine().split(",");
						data1[z]= Integer.parseInt( numbers[0]);
						data2[z] = Integer.parseInt(numbers[1]);
						if(max1<data1[z]){
							max1=data1[z];
						}
						if(max2<data2[z]){
							max2=data2[z];
						}
						if(min1>data1[z]){
							min1 = data1[z];
						}
						if(min2> data2[z]){
							min2 = data2[z];
						}
		    	
		    	
						z++;
		    	
					}
					tt.close();
					
					double avg1=0, avg2=0;
					for(z=0; z<data1.length; z++){
						avg1 += data1[z];
						avg2 += data2[z];
						
		    	
					}
					avg1/=data1.length;
					avg2/=data2.length;
					double var1=0, var2=0;
					for(z=0; z<data1.length; z++){
						var1 += Math.pow((data1[z]-avg1),2);
						var2 += Math.pow((data2[z]-avg2),2);
						
		    	
					}
					var1/=(data1.length-1);
					var2/=(data2.length-1);
					
					DecimalFormat form = new DecimalFormat("#.###");
					System.out.print(s.getName()+" & "+form.format(avg1)+" & "+form.format(var1)+" & "+max1+" & "+min1+" & ");
					System.out.println(form.format(avg2)+" & "+form.format(var2)+" & "+max2+" & "+min2+" \\\\");
		    
					}
		
		
			}	
		
		
		
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(IOException e){
			
		}
	}	
	
	
	
	
	public static int getvar(){
		return 3;
	}
	public static void calc(){
		
			
		
		
	}
	
	

}



