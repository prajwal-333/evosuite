package org.evosuite.coverage.aes.method;

import java.io.BufferedReader;
import org.evosuite.utils.LoggingUtils;
import java.io.*;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;



import java.util.Arrays;

public class Readcsv {
	
	static Map<String,Double> map=new HashMap<String,Double>();  
	public static final String delimiter = ",";
    public static Double min_weight=Double.MAX_VALUE;
	 static { 
	       
		 //System.out.println("Static block executed");
		 LoggingUtils.getEvoLogger().info(System.getProperty("user.dir"));
		 LoggingUtils.getEvoLogger().info("Static block");
		      try {
		         File file = new File("./client/src/main/java/org/evosuite/coverage/aes/method/Metrics.csv");
		    	  //File file = new File("/Users/mam/Desktop/project/csvReader/src/csvReader/Metrics.csv");
		         FileReader fr = new FileReader(file);
		         BufferedReader br = new BufferedReader(fr);
		         String line = "";
		         String[] tempArr;
		         int lineno=0;
		         String metric_name="MIMS";
		         int key_coloumn=0;
		         int metric_coloumn=0;
		         while((line = br.readLine()) != null) {
		            tempArr = line.split(delimiter);
		            
		            
		            	//System.out.println(tempArr[0]);
		            	//System.out.println(tempArr[1]);
		            	String key="[METHOD] "+tempArr[0];
		            	
		            	//key = key.substring(1, key.length() - 1);
		            	String value=tempArr[1];
		            	//value = value.substring(1, value.length() - 1);
	            		double d=Double.parseDouble(value);
	            		map.put(key, d);
	            		if(min_weight>d)
	            		{
	            			min_weight=d;
	            		}
	            		//LoggingUtils.getEvoLogger().info("min_weight"+min_weight);
	            		//System.out.println(min_weight);
	            		//System.out.println("size of map is "+map.size());
	            	
	            	
		           
		         }
		         br.close();
		         } catch(IOException ioe) {
		            ioe.printStackTrace();
		         }
			
	 }
	 
	 public static void main(String args[])
	 {
		 System.out.println("from main");
	 }

}
