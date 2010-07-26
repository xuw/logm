package stream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class FilePump {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		
		
		String hostport = System.getProperty("host");
		String[] hostports = hostport.split(":");
		String host = hostports[0];
		int port = Integer.parseInt( hostports[1] );
		
		Socket client;
		PrintStream out=null;
		
		boolean connect=false;
		
		while(!connect) {
		try {
			client = new Socket(host,port);
			out = new PrintStream(client.getOutputStream());
			connect = true;
		} catch (UnknownHostException e) {
			System.err.println("unknown host " + host +".  did you set the -Dhost parameter correctly?");
			return;
		} catch (IOException e) {
			System.err.println("cannot connect.. is server on? retrying..");
			Thread.sleep(1000);
		}
		}
		
		while(true) {
			
			String line = reader.readLine();  // block if no more..
			
			if (line==null || line.length()==0) {
				Thread.sleep(500);
				continue;
			}
			
			//System.err.println("sending: " + line);
			out.println(line);
			
		}
		
	}

}
