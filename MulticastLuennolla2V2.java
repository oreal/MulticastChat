import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;


//L‰hde: TIEA322 Tietoliikenneprotokollat -luentosarja
// muokattu versio
public class MulticastLuennolla2 {
	//private static MulticastSocket socket;
	private static MulticastChatSocket socket;
	private static InetAddress group;
	private static byte[] buffer;
	private static DatagramPacket packet;
	private static BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
	private static String nimi;
	
	public static void main(String[] args) {
		try {
			//socket = new MulticastSocket(6666);
			socket = new MulticastChatSocket(6666);
			socket.setSoTimeout(500);
			group = InetAddress.getByName("239.0.0.1");
			buffer = new byte[256];
			packet = new DatagramPacket(buffer, buffer.length);
			
			System.out.println("anna kayttajanimi: ");
			String nimi =  in.readLine();
			if (nimi == "") nimi = "Fred";
			
			socket.joinGroup(group, nimi);
			
			System.out.println("MulticastChat versio2");
			System.out.println("Lopeta ohjelma: quit");
			System.out.println("K‰ytt‰j‰t: users");
			
			ViestienLahetys aa = new ViestienLahetys(socket, packet, group, nimi); 
			aa.start();
			
	        ViestienVastaanotto bb = new ViestienVastaanotto(socket, group, aa, nimi); 
			bb.start();
		
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
		/*
		boolean on = true;
		while(on) {
			try {
				if (!in.ready()) {
					try {
						socket.receive(packet);
						//System.out.println(new String(packet.getData()));
						System.out.println(socket.tulosta());  // hoitaa varsinaisen tulostamisen, mit‰ MulticastChatSocket palauttaa
					}
					catch (SocketTimeoutException se) {
						// do nothing here
					}
					catch (IOException ie) {
						System.out.println(ie.getMessage());
					}
				}
				else {
					//System.out.println(in.readLine());
					socket.send(in.readLine());
					on=false;
				}
			}
			catch (IOException ie) {
				System.out.println(ie.getMessage());
			}
		}
		try {
			socket.leaveGroup(group);
			socket.close();
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}*/
}
 
}


class ViestienLahetys extends Thread
{
   private MulticastChatSocket socket;
   private  DatagramPacket packet;
   private  BufferedReader in;
   InetAddress group;
   String kayttajanNimi;
   boolean saaLahettaaListan = true;
 
 
   ViestienLahetys(MulticastChatSocket socket, DatagramPacket packet, InetAddress grou, String nimi) //konstruktori
   {
	  this.packet = packet;
      this.socket = socket;
      in = new BufferedReader(new InputStreamReader(System.in));
      group = grou;
      kayttajanNimi = nimi;
    
   }
 
   public void run()
   {
	   Timer a = new Timer(); 
	   while(true){
		   saaLahettaaListan=true;
	   try {
		   socket.receive(packet);  // otetaan paketti vastaan ja k‰sitell‰‰n se
		   if(socket.getHappening().equals("userListUpdate")) {   // jos joku l‰hetti k‰ytt‰j‰listan, ei l‰hetet‰ meid‰n omaa k‰ytt‰j‰listaa
			  saaLahettaaListan=false;
			  }
		   
		   if(socket.getHappening().equals("joined") ) {   // jos oli join - l‰hetet‰‰n meid‰n k‰ytt‰j‰lista muille, kunhan se ei ollut meid‰n oma join viesti
		   a.schedule(new TimerTask(){
				@Override
				  public void run() {
					if(saaLahettaaListan) {
					try {
						socket.sendUserList(group, kayttajanNimi);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				  }}
			}, 5000);
		   }
		   //System.out.println(new String(packet.getData()));
		   
			//System.out.println(new String(packet.getData()));
			/* send = true;
			
			String lahettaja = socket.getUser();
			if(komento == "userListUpdate")
				send = false;
			if(lahettaja.equals(kayttajanNimi))
				send = false;
			else send = true;
			if(komento == "join" && (!lahettaja.equals(kayttajanNimi))){
			
		
			}*/
	   }
			catch (IOException e) {
				   
			       }
	   //on=false;
   }
}
}

class ViestienVastaanotto extends Thread
{
   private MulticastChatSocket socket;
   private volatile boolean running = true;
   private  BufferedReader in;
   InetAddress group;
   ViestienLahetys vl;
   String kayttajanNimi;
 
   ViestienVastaanotto(MulticastChatSocket socket, InetAddress grou, ViestienLahetys v, String kayttajanNim) //konstruktori
   {
      this.socket = socket;
      group = grou;
      vl = v;
      in = new BufferedReader(new InputStreamReader(System.in));
      kayttajanNimi = kayttajanNim;
   }
   
   public void terminate() {
       running = false;
   }
 
   public void run() 
   {
	   while(true){
   
	   try {
		  String syote =  in.readLine();
		  if(syote.equals("quit")) {
			  socket.leaveGroup(group, kayttajanNimi);
			  socket.close();
			  socket.disconnect();
			  running = false; 
			 /* vl.interrupt();
			  this.interrupt();*/
              System.exit(0);
		
			  
			 
			  
		  }
		  else if(syote.equals("users")) {
			 	List<String> a = socket.tulostaKayttajaLista();
				 for(int i = 0; i<a.size(); i++){
					 System.out.println(a.get(i));
				 }
			
		  }
		  else {
		socket.sendPP(syote, kayttajanNimi);
	         } }
	   catch (IOException e) {
		   // TODO Auto-generated catch block
		     
	       }
	   //on=false;
   }

   }
   
 
}