import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Random;

public class Main {

	public static void main(String[] args) {
		
		int nRule = Integer.parseInt("100");
		
		PrintWriter writer;
		try {
			writer = new PrintWriter("psaConf", "UTF-8");
			writer.println("#!/bin/bash");
			
			for(int i=0; i<nRule; i++){

				Random r = new Random();
				//avoid global broadcast
				String ip = r.nextInt(255) + "." + r.nextInt(255) + "." + r.nextInt(255) + "." + r.nextInt(255);
				if(i%2==0)
					writer.println("sudo iptables -A FORWARD -p TCP -d "+ ip +" -j DROP");
				
				else
					writer.println("sudo iptables -A FORWARD -p TCP -d "+ ip +" -j ACCEPT");
			}
			
			writer.println("COMMIT");
			writer.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		

	}

}
