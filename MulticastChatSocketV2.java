import java.io.*;
import java.util.Timer;
import java.util.TimerTask;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class MulticastChatSocket extends MulticastSocket {

	private int day = 0;
	private int month = 0;
	private int year = 0;
	private int versio = 0;
	private String kurssinNimi = "";
	private String viestinLahettajanNimi = ""; 
	private String sovelluksenKayttajanNimi = ""; 
	private String tapahtuma = ""; 
	private String message = "";
	private int portti = 0;
	private InetAddress address = null;
	private byte[] data = null;
	private int pituus = 0;
	/*
	 * NELJƒ ERILAISTA HEADERIA, VERSIO 2
	 */
	private List<String> kayttajaLista = new ArrayList<String>();
	private byte[] messageHeader = new byte[]{0x23, 0x6b, 0x7b, 0x30, 0x07,0x54,0x49,0x45,0x41,0x33,0x32,0x32}; 
	private byte[] joinHeader = new byte[]{0x21, 0x6b, 0x7b, 0x30, 0x07,0x54,0x49,0x45,0x41,0x33,0x32,0x32};
	private byte[] quitHeader = new byte[]{0x22, 0x6b, 0x7b, 0x30, 0x07,0x54,0x49,0x45,0x41,0x33,0x32,0x32};
	private byte[] userListHeader = new byte[]{0x24, 0x6b, 0x7b, 0x30, 0x07,0x54,0x49,0x45,0x41,0x33,0x32,0x32};

	public MulticastChatSocket() throws SocketException, IOException {
		super();
	}

	public MulticastChatSocket(int portti) throws SocketException, IOException {
		super(portti);
		this.portti = portti;
		
	}
	
 /**
  * Ryhm‰‰n liittyminen, samalla l‰hetet‰‰n JOIN-viesti
  * @param mcastaddr  osoite 
  * @param nimi k‰ytt‰j‰n nimi
  * @throws IOException 
  */
	public void joinGroup(InetAddress mcastaddr, String nimi) throws IOException {
		super.joinGroup(mcastaddr);
		this.sovelluksenKayttajanNimi = nimi;
		this.address = mcastaddr;
		sendP("/join", nimi, joinHeader);
	}
	
	/**
	 * Ryhm‰st‰ l‰hteminen
	 * @param mcastaddr
	 * @param nimi K‰ytt‰j‰n nimi
	 * @throws IOException
	 */
	public void leaveGroup(InetAddress mcastaddr, String nimi) throws IOException {
		super.leaveGroup(mcastaddr);
		this.address = mcastaddr;
		sendP("/quit", nimi, quitHeader);
	}
	
	/**
	 * P‰ivitet‰‰n k‰ytt‰j‰lista
	 * @param mcastaddr
	 * @param nimi
	 * @throws IOException
	 */
	public void sendUserList(InetAddress mcastaddr, String nimi) throws IOException{
		this.address =mcastaddr;
		if(kayttajaLista.size() > 1)
		sendUserList(nimi);
		
	}

	/**
	 * Ollaan saatu pakettu, selvitet‰‰n siit‰ tarvittavat tiedot
	 * kuten mit‰ se tekee= komento
	 * p‰iv‰m‰‰r‰
	 * kurssinNimi
	 * K‰ytt‰j‰nNimi 
	 * jne
	 */
	public void receive(DatagramPacket p) throws IOException {
		super.receive(p);
		byte[] temp = p.getData();

		int komentoLuku = temp[0] & 0xF;
		int paikka = 0; // miss‰ ollaan menossa
		paikka = selvitaVersio(temp, 0);
		paikka = selvitaPvm(temp, paikka);
		paikka = selvitaKurssinNimi(temp, paikka);
		paikka = selvitaKayttajanNimi(temp, paikka);

		if(komentoLuku == 1){  	// selvitet‰‰n mik‰ komento on kyseess‰
			tapahtuma = "joined"; 
			this.message = "joined"; // viesti‰ ei tarvitse selvitt‰‰, laitetaan vain loppuun message
			kayttajaLista.add(getUser());
			System.out.println(tulosta()); 
		}
		else if(komentoLuku == 2) {
			tapahtuma = "quit"; 
			this.message = "joined"; 
			kayttajaLista.remove(this.getUser());
			System.out.println(tulosta()); 
		}
		else if(komentoLuku == 3) {
			tapahtuma = "message";  
			paikka = selvitetaanViesti(temp, paikka);
			System.out.println(tulosta()); 
		}
		
		else if(komentoLuku == 4) {
			tapahtuma = "userListUpdate"; 
			if(kayttajaLista.size() == 1) {
		    paikka = selvitaKayttajaLista(temp, paikka);
		    if (!(viestinLahettajanNimi == sovelluksenKayttajanNimi))
		    System.out.println(tulosta()); 
			}
		}  // tulostetaan tiedot
		else {
			tapahtuma = "unknown"; 
			this.message = "unknown message";
		}
		
	}
	
	/**
	 * P‰ivitet‰‰n k‰ytt‰j‰lista
	 * @param temp
	 * @param paikka
	 * @return
	 */
	private int selvitaKayttajaLista(byte[] temp, int paikka) {
         /* Viesti sis‰lt‰‰ kytt‰j‰listan */
		 kayttajaLista.clear();
         int laskekierrokset = 0;
         while(true) {
        	 int viestinpituus = 0; 
        	 paikka+=laskekierrokset;
        	 laskekierrokset = 1;
        	try{
        	
        	 viestinpituus = temp[paikka]; 
        	 if (viestinpituus == 0) return paikka+=laskekierrokset; // montako tavua seuraavassa nimess‰ on
        	}catch (IndexOutOfBoundsException e)
        	{
        		break;
        	}
             	StringBuilder nimi = new StringBuilder();
             	for(int i = paikka+1; i < paikka+viestinpituus+1; i++){
             		laskekierrokset++;
             		nimi.append((char)temp[i]);
             	}
             	
             kayttajaLista.add(nimi.toString());
         }
         return (paikka+=laskekierrokset);
        } 
	

	/**
	 * Palauttaa p‰iv‰n 0-31
	 * @return P‰iv‰
	 */
	public int getDay(){
		return this.day;
	}
	
	/**
	 * Palauttaa p‰iv‰n 0-31
	 * @return Versio
	 */
	public int getVersion(){
		return this.versio;
	}
	
/**
 * Palauttaa sovelluksen nimen
 * @return esim. TIEA322
 */
	public String getKurssinNimi(){
		return this.kurssinNimi;
	}

	/**
	 * Palauttaa kuukauden 0-12
	 * @return
	 */
	public int getMonth(){
		return this.month;
	}

	/**
	 * Palauttaa vuoden 0-2047
	 * @return year
	 */
	public int getYear(){
		return this.year;
	}
	
	/**
	 * Paluttaa k‰ytt‰j‰n nimen 
	 * @return username
	 */
	public String getUser(){
		return this.viestinLahettajanNimi;
	}
	
 /**
 * Palauttaa tapahtuman
 * message = viestin l‰hett‰minen
 * join = liittyminen ryhm‰‰n
 * quit = poistuminen ryhm‰st‰
 * @return
 */
	public String getHappening(){
		return this.tapahtuma;
	}
	
	/**
	 * K‰ytt‰j‰n l‰hett‰m‰ viesti
	 * @return user message
	 */
	public String getMessage(){
		return this.message;
	}

	
	
	
	/**
	 * Kokoaa tapahtuman tiedot: p‰iv‰m‰‰r‰, sovelluksen nimi, tapahtuma
	 * @return info about received message 
	 */
	public String tulosta() {
		StringBuilder viesti = new StringBuilder(getKurssinNimi() + " User: " + getUser()+"("+  getDay() + "." + getMonth() + "." + getYear() + "):" );
		if(this.tapahtuma == "message")
				viesti.append(" " +getMessage());
		else viesti.append(" " +getHappening());
		return viesti.toString();
	}
	
	/**
	 * Kutsuu senP-aliohjelmaa oletuskomennolla: l‰hetet‰‰n viesti
	 * @param teksti
	 * @param nimi
	 * @throws IOException
	 */
	public void sendPP(String teksti, String nimi) throws IOException {
		sendP(teksti, nimi, messageHeader);
	}
	
	
	/**
	 * L‰hett‰‰ viestipaketin
	 * Toimii kolmella erilaisella komennolla: viesti, join ja quit
	 * @param teksti mik‰ teksti l‰hetet‰‰n
	 * @param nimi k‰ytt‰j‰n nimi
	 * @throws IOException
	 */
	public void sendP(String teksti, String nimi, byte[] header) throws IOException {
		
		//header = new byte[]{0x13, 0x6b, 0x7b, 0x30, 0x07,0x54,0x49,0x45,0x41,0x33,0x32,0x32}; // header ilman nime‰
		//header sis‰lt‰‰ 12 tavua, loppuu vakiolliseen kurssin nimeen
		this.data = new byte[header.length + 1 + nimi.length() + teksti.length()+1];  // datan koko = headerin koko + nimen koko 
		System.arraycopy(header, 0, this.data, 0, header.length);  // dataan menee headeri
		
		// seuraavaan paikkaan menee nimen pituus
		this.data[header.length] = (byte)nimi.length(); // ennen nime‰ tulee sen koko
		// nimi sijoitetaan dataan
		System.arraycopy(nimi.getBytes(), 0, this.data, 13, nimi.length()); // dataan laitetaan nimi
		
		// viesti sijoitetaan dataan
		this.data[header.length+nimi.length()+1] = (byte)teksti.length();
		System.arraycopy(teksti.getBytes(), 0, this.data, header.length+1+nimi.length()+1, teksti.length()); 
		
		this.pituus = this.data.length;
		DatagramPacket p = new DatagramPacket(this.data, this.pituus, this.address, this.portti);
		super.send(p);
	}

	public List<String> tulostaKayttajaLista() {
		return kayttajaLista; 
		
	}
	
	
	/**
	 * L‰hett‰‰ k‰ytt‰j‰listan
	 * @param teksti mik‰ teksti l‰hetet‰‰n
	 * @param nimi k‰ytt‰j‰n nimi
	 * @throws IOException
	 */
	public void sendUserList(String nimi) throws IOException {
		byte header[] = this.userListHeader;
		
		//header = new byte[]{0x13, 0x6b, 0x7b, 0x30, 0x07,0x54,0x49,0x45,0x41,0x33,0x32,0x32}; // header ilman nime‰
		//header sis‰lt‰‰ 12 tavua, loppuu vakiolliseen kurssin nimeen
		// lasketaan kuinka iso data tarvitaan:
		int kayttajalistanNimetjaKoot = 0;
		for(int i = 0; i<kayttajaLista.size(); i++){
			kayttajalistanNimetjaKoot++;
		    kayttajalistanNimetjaKoot += kayttajaLista.get(i).length();
	     }
		this.data = new byte[header.length + 1 + nimi.length() + kayttajalistanNimetjaKoot];  // datan koko = headerin koko + nimen koko  + kayttajalistankoko
		System.arraycopy(header, 0, this.data, 0, header.length);  // dataan menee headeri
		
		// seuraavaan paikkaan menee nimen pituus
		this.data[header.length] = (byte)nimi.length(); // ennen nime‰ tulee sen koko
		// nimi sijoitetaan dataan
		System.arraycopy(nimi.getBytes(), 0, this.data, 13, nimi.length()); // dataan laitetaan nimi
		
		// k‰ytt‰j‰lista sijoitetaan dataan, yksi k‰ytt‰j‰ kerrallaan
		int j = 1;
		for(int i = 0; i<kayttajaLista.size();i++ ){
		this.data[header.length+nimi.length()+j] = (byte)kayttajaLista.get(i).length();
		// 
		System.arraycopy(kayttajaLista.get(i).getBytes(), 0, this.data, header.length+1+nimi.length()+j, kayttajaLista.get(i).length()); 
		j +=kayttajaLista.get(i).length()+1;
	    }
		
		this.pituus = this.data.length;
		DatagramPacket p = new DatagramPacket(this.data, this.pituus, this.address, this.portti);
		super.send(p);
	}
	
	/**
	 * Selvitet‰‰n viesti
	 * @param temp
	 * @param paikka
	 * @return paikka, miss‰ ollaan viestin loputtua
	 */
private int selvitetaanViesti(byte[] temp, int paikka) {
	  /*SELVITETƒƒN VIESTI*/
    int viestinpituus = temp[paikka];
    if(this.tapahtuma =="message") { 
    paikka++;
    	StringBuilder viesti = new StringBuilder();
    	for(int i = paikka; i < paikka+viestinpituus ; i++){
    		if(temp[i] != 0) {
    			viesti.append((char)temp[i]);
    		}
    		else {
    			break;
    		}
    	}
    	this.message = viesti.toString();
    } 
    else this.message = "";  // varmuudeksi viel‰ tyhj‰t‰‰n message, jos komento ei ollut message
    return paikka + viestinpituus;
	}


/**
 * Selvitet‰‰n username
 * @param temp paketti
 * @param paikka mist‰ paikasta tieto lˆytyy
 * @return paikka miss‰ ollaan menossa taulukossa
 */
private int selvitaKayttajanNimi(byte[] temp, int paikka) {

	  /*SELVITETƒƒN KƒYTTƒJƒN NIMI */
    
    int KayttajanNimenPituus = temp[paikka];
    paikka++;
    StringBuilder viestinLahettajanNimi = new StringBuilder(); 
    int laskekierrokset = 0;
    for(int i= paikka; i<paikka+KayttajanNimenPituus; i++){
    	laskekierrokset++;
        viestinLahettajanNimi.append((char)temp[i]);
        }
        this.viestinLahettajanNimi = viestinLahettajanNimi.toString();
        
     return paikka +=laskekierrokset; // jatketaan siit‰ mihin nimen j‰lkeen j‰‰tiin
	}

/**
 * Selvitet‰‰n kurssin nimi
 * @param temp
 * @param paikka
 * @return paikka miss‰ ollaan menossa taulukossa
 */
private int selvitaKurssinNimi(byte[] temp, int paikka) {
    /*SELVITETƒƒN KURSSIN NIMI */
    int nimenPituus = temp[paikka];
    StringBuilder kurssinNimi = new StringBuilder();
    paikka++;
    for(int i= paikka; i<paikka+nimenPituus; i++){
    kurssinNimi.append((char)temp[i]);
    }
    this.kurssinNimi = kurssinNimi.toString();
    
    // jatkuu nimen j‰lkeen, eli paikasta 4 + nimenpituus
    int mistaJatketaan = paikka + nimenPituus; // vakio 7, koska nimelle on aina varattu tasan 8 tavua
		return mistaJatketaan;
	}


/**
 * Selvitet‰‰n henkilˆn syntym‰aika
 * @param temp
 * @param paikka
 * @return taulukon paikka, johon j‰‰tiin
 */
private int selvitaPvm(byte[] temp, int paikka) {
	/*SELVITETƒƒN PVM */
	this.day = temp[1] >>> 3 & 0x1f;  // p‰iv‰ on ekat 5 bitti‰ -> ne laitetaan oikeaan reunaan ja tehd‰‰n AND
	
	byte apubyte = (byte) (temp[1] & 0x7); 
	apubyte = (byte) (apubyte << 1);// jos kuukausi = 7 eli 0111, t‰st‰ saadaan kolme ensimm‰ist‰ bitti‰ eli 11, arvo on 3
    // t‰ytyy viel‰ list‰t‰ viimeinen bitti seuraavan tavun ensimm‰isest‰ bitist‰
    
	
	this.month = apubyte;
	
	//byte apubyte2 = (byte) (temp[2]  >>> 6 & 0x1); // siirret‰‰n kaikkia seitsem‰n oikealle ja maskataan kaikki ykkˆsell‰
	//this.month =  (byte)(apubyte | apubyte2); // lopuksi yhdistet‰‰n kaksi tavua: esimerkki, jos luku oli seitsem‰n:
	// 0000 0110
	// 0000 0001  TULOS = 0000 0111 eli 7
	
	// vuosi on 7 bitti‰ samasta tavusta kuin kuukauden viimeinen bitti
	
    
    // X1111001 0111		 = 1943
    // tavu 2 sis‰lt‰‰ 7 merkki‰, ensimm‰ist‰ ei oteta mukaan
    // tavu 3 sis‰lt‰‰ 4 merkki‰, 4 merkitsevint‰
    
	// X1111001 0111
	byte apubyte3 = (byte) (temp[2] & 0x7F);  // tavusta nro2 otetaan 7 v‰hiten merkitsev‰‰ bitti‰
	//01111001
	//int y=  (apubyte3 << 1);
	//11110010
	byte apubyte4 = (byte) (temp[3] & 0xF0);  // tavusta 3 otetaan 4 merkitsevint‰
	// 0111 0000
	apubyte4 = (byte) (apubyte4 >> 4); // siirret‰‰n alkuun
	//this.year = (byte)(apubyte3 | apubyte4);    
	int vuosi = 0;
	vuosi = apubyte3;
	vuosi = vuosi << 4;	// siirret‰‰n bittej‰ nelj‰ oikealle
    this.year = vuosi | apubyte4 ;
    // versio 4  komento 4   p‰iv‰ 5 + kuukausi 3   kuukausi 1 + vuosi 11   
    // 0001	     0001        1000 0101              0111 1010    0000          00000000111	54 49 45 41 33 32 32 05	0000101	51 75 69 56 74	0000101	2f 6a 6f 69 6e
    // nyt on k‰ytetty kolme tavua ja tasan kolme tavua
    return 4;
	}

/**
 * Selvitet‰‰n henkilˆn syntym‰aika
 * @param temp
 * @param paikka
 * @return taulukon paikka, johon j‰‰tiin
 */
private int selvitaPvm2(byte[] temp, int paikka) {
	/*SELVITETƒƒN PVM */
	this.day = temp[1] >>> 3 & 0x1f;  // p‰iv‰ on ekat 5 bitti‰ -> ne laitetaan oikeaan reunaan ja tehd‰‰n AND
	
	byte apubyte = (byte) (temp[1] & 0x7); 
	apubyte = (byte) (apubyte << 1);// jos kuukausi = 7 eli 0111, t‰st‰ saadaan kolme ensimm‰ist‰ bitti‰ eli 11, arvo on 3
    // t‰ytyy viel‰ list‰t‰ viimeinen bitti seuraavan tavun ensimm‰isest‰ bitist‰

	byte apubyte2 = (byte) (temp[2]  >>> 6 & 0x1); // siirret‰‰n kaikkia seitsem‰n oikealle ja maskataan kaikki ykkˆsell‰
	this.month =  (byte)(apubyte | apubyte2); // lopuksi yhdistet‰‰n kaksi tavua: esimerkki, jos luku oli seitsem‰n:
	// 0000 0110
	// 0000 0001  TULOS = 0000 0111 eli 7
	
	// vuosi on 7 bitti‰ samasta tavusta kuin kuukauden viimeinen bitti
	
    
    // X1111001 0111		 = 1943
    // tavu 2 sis‰lt‰‰ 7 merkki‰, ensimm‰ist‰ ei oteta mukaan
    // tavu 3 sis‰lt‰‰ 4 merkki‰, 4 merkitsevint‰
    
	// X1111001 0111
	byte apubyte3 = (byte) (temp[2] & 0x7F);  // tavusta nro2 otetaan 7 v‰hiten merkitsev‰‰ bitti‰
	//01111001
	//int y=  (apubyte3 << 1);
	//11110010
	byte apubyte4 = (byte) (temp[3] & 0xF0);  // tavusta 3 otetaan 4 merkitsevint‰
	// 0111 0000
	apubyte4 = (byte) (apubyte4 >> 4);
	//this.year = (byte)(apubyte3 | apubyte4);    
	int vuosi = 0;
	vuosi = apubyte3;
	vuosi = vuosi << 4;	
    this.year = vuosi | apubyte4;
    // versio 4  komento 4   p‰iv‰ 5 + kuukausi 3   kuukausi 1 + vuosi 11   
    // 0001	     0001        1000 0101              0111 1010    0000          00000000111	54 49 45 41 33 32 32 05	0000101	51 75 69 56 74	0000101	2f 6a 6f 69 6e
    // nyt on k‰ytetty kolme tavua ja tasan kolme tavua
    return 4;
	}

/**
 * Palauttaa version annetusta paikasta i
 * @param temp
 * @param i
 * @return i
 */
	private int selvitaVersio(byte[] temp, int i) {
		this.versio = temp[i] >>> 4 & 0xf; // versio on ekat nelj‰ bitti‰ -> oikeaan reunaan ja AND
        return i;
	} 

	
}



/*
TURHAT KOODIT ==========================================================================
*/
//


/**
 * Lis‰t‰‰n dataosuuden kaikki k‰ytt‰j‰t listaan
 * @param p
 */
// 
//**
//* Kokoaa tapahtuman tiedot: p‰iv‰m‰‰r‰, sovelluksen nimi, tapahtuma
//* @return info about received message 
//*/
//public String tulosta() {
//	StringBuilder viesti = new StringBuilder("Day:" + getDay() + "." + getMonth() + "." + getYear() + " Course: "+ getKurssinNimi() + " User: " + getUser() + ":");
//	if(this.tapahtuma == "message")
//			viesti.append(" " +getMessage());
//	else viesti.append(" " +getHappening());
//	return viesti.toString();
//


// private void LisaaKayttajatListaan(DatagramPacket p) {
//	byte[] temp = p.getData();
//  this.versio = temp[0] >>> 4 & 0xf; // versio on ekat nelj‰ bitti‰ -> oikeaan reunaan ja AND
//
//
//	/*SELVITETƒƒN PVM */
//	this.day = temp[1] >>> 3 & 0x1f;  // p‰iv‰ on ekat 5 bitti‰ -> ne laitetaan oikeaan reunaan ja tehd‰‰n AND
//
//	byte apubyte = (byte) (temp[1] & 0x7); 
//	apubyte = (byte) (apubyte << 1);// jos kuukausi = 7 eli 0111, t‰st‰ saadaan kolme ensimm‰ist‰ bitti‰ eli 11, arvo on 3
//  // t‰ytyy viel‰ list‰t‰ viimeinen bitti seuraavan tavun ensimm‰isest‰ bitist‰
//
//	byte apubyte2 = (byte) (temp[2]  >>> 6 & 0x1); // siirret‰‰n kaikkia seitsem‰n oikealle ja maskataan kaikki ykkˆsell‰
//	this.month =  (byte)(apubyte | apubyte2); // lopuksi yhdistet‰‰n kaksi tavua: esimerkki, jos luku oli seitsem‰n:
//	byte apubyte3 = (byte) (temp[2] & 0x7F);  // tavusta nro2 otetaan 7 v‰hiten merkitsev‰‰ bitti‰
//	byte apubyte4 = (byte) (temp[3] & 0xF0);  // tavusta 3 otetaan 4 merkitsevint‰
//	apubyte4 = (byte) (apubyte4 >> 4);
//	int vuosi = 0;
//	vuosi = apubyte3;
//	vuosi = vuosi << 4;	
//  this.year = vuosi | apubyte4;
//  
//  /*SELVITETƒƒN KURSSIN NIMI */
//  int nimenPituus = temp[4];
//  StringBuilder kurssinNimi = new StringBuilder();
//  for(int i= 5; i<5+nimenPituus; i++){
//  kurssinNimi.append((char)temp[i]);
//  }
//  this.kurssinNimi = kurssinNimi.toString();
//  
//  // jatkuu nimen j‰lkeen, eli paikasta 4 + nimenpituus
//  int mistaJatketaan = 4 + 8; // vakio 8, koska nimelle on aina varattu tasan 8 tavua
//  
//   /*SELVITETƒƒN KƒYTTƒJƒN NIMI */
//  // seuraava tavu sis‰lt‰‰ tiedon nimen pituudesta
//  
//  int KayttajanNimenPituus = temp[mistaJatketaan];
//  mistaJatketaan++;
//  
//  StringBuilder viestinLahettajanNimi = new StringBuilder(); 
//  int laskekierrokset = 0;
//  for(int i= mistaJatketaan; i<mistaJatketaan+KayttajanNimenPituus; i++){
//  	laskekierrokset++;
//      viestinLahettajanNimi.append((char)temp[i]);
//      }
//      this.viestinLahettajanNimi = viestinLahettajanNimi.toString();
//    if(tapahtuma=="joined") {
//  	  if (kayttajaLista.contains(viestinLahettajanNimi.toString())) ;
//  	  else kayttajaLista.add(this.viestinLahettajanNimi);
//    }
//    if(tapahtuma=="quit") kayttajaLista.remove(viestinLahettajanNimi.toString());
//  
//   mistaJatketaan +=laskekierrokset; // jatketaan siit‰ mihin nimen j‰lkeen j‰‰tiin
//  
//
//  /*SELVITETƒƒN VIESTI
//   * Viesti sis‰lt‰‰ kytt‰j‰listan */
//   laskekierrokset = 0;
//   while(true) {
//  	 int viestinpituus = 0;
//  	 mistaJatketaan+=laskekierrokset;
//  	 laskekierrokset = 0;
//  	try{
//  	
//  	 viestinpituus = temp[mistaJatketaan];
//  	}catch (IndexOutOfBoundsException e)
//  	{
//  		break;
//  	}
//  	  mistaJatketaan++;
//       	StringBuilder nimi = new StringBuilder();
//       	for(int i = mistaJatketaan; i < mistaJatketaan+viestinpituus ; i++){
//       		laskekierrokset++;
//       		nimi.append((char)temp[i]);
//       	}
//       	
//       kayttajaLista.add(nimi.toString());
//   }
//  }
//

///**

//public void receive(DatagramPacket p) throws IOException {
//	super.receive(p);
//	byte[] temp = p.getData();
//
//	int komentoLuku = temp[0] & 0xF;
//
//	if(komentoLuku == 1){  	// selvitet‰‰n mik‰ komento on kyseess‰
//		tapahtuma = "joined"; 
//	}
//	else if(komentoLuku == 2) {
//		tapahtuma = "quit"; 
//	}
//	else if(komentoLuku == 3) {
//		tapahtuma = "message";  
//	}
//
//	else if(komentoLuku == 4) {
//		tapahtuma = "userListUpdate"; 
//		if(kayttajaLista.size() == 1) LisaaKayttajatListaan(p); 
//		return;
//	}
//
//	else {
//		tapahtuma = "unknown"; 
//	}
//
//	/*
//	 * Selvitet‰‰n versio
//
//	 */
//	int paikka = 0;
//	selvitaVersio(temp, 0);
//
//	this.versio = temp[0] >>> 4 & 0xf; // versio on ekat nelj‰ bitti‰ -> oikeaan reunaan ja AND
//
//
//	/*SELVITETƒƒN PVM */
//	this.day = temp[1] >>> 3 & 0x1f;  // p‰iv‰ on ekat 5 bitti‰ -> ne laitetaan oikeaan reunaan ja tehd‰‰n AND
//
//	byte apubyte = (byte) (temp[1] & 0x7); 
//	apubyte = (byte) (apubyte << 1);// jos kuukausi = 7 eli 0111, t‰st‰ saadaan kolme ensimm‰ist‰ bitti‰ eli 11, arvo on 3
//   // t‰ytyy viel‰ list‰t‰ viimeinen bitti seuraavan tavun ensimm‰isest‰ bitist‰
//
//	byte apubyte2 = (byte) (temp[2]  >>> 6 & 0x1); // siirret‰‰n kaikkia seitsem‰n oikealle ja maskataan kaikki ykkˆsell‰
//	this.month =  (byte)(apubyte | apubyte2); // lopuksi yhdistet‰‰n kaksi tavua: esimerkki, jos luku oli seitsem‰n:
//	// 0000 0110
//	// 0000 0001  TULOS = 0000 0111 eli 7
//
//	// vuosi on 7 bitti‰ samasta tavusta kuin kuukauden viimeinen bitti
//
//   
//   // X1111001 0111		 = 1943
//   // tavu 2 sis‰lt‰‰ 7 merkki‰, ensimm‰ist‰ ei oteta mukaan
//   // tavu 3 sis‰lt‰‰ 4 merkki‰, 4 merkitsevint‰
//   
//	// X1111001 0111
//	byte apubyte3 = (byte) (temp[2] & 0x7F);  // tavusta nro2 otetaan 7 v‰hiten merkitsev‰‰ bitti‰
//	//01111001
//	//int y=  (apubyte3 << 1);
//	//11110010
//	byte apubyte4 = (byte) (temp[3] & 0xF0);  // tavusta 3 otetaan 4 merkitsevint‰
//	// 0111 0000
//	apubyte4 = (byte) (apubyte4 >> 4);
//	//this.year = (byte)(apubyte3 | apubyte4);    
//	int vuosi = 0;
//	vuosi = apubyte3;
//	vuosi = vuosi << 4;	
//   this.year = vuosi | apubyte4;
//   // versio 4  komento 4   p‰iv‰ 5 + kuukausi 3   kuukausi 1 + vuosi 11   
//   // 0001	     0001        1000 0101              0111 1010    0000          00000000111	54 49 45 41 33 32 32 05	0000101	51 75 69 56 74	0000101	2f 6a 6f 69 6e
//   // nyt on k‰ytetty kolme tavua ja tasan kolme tavua
//   
//   /*SELVITETƒƒN KURSSIN NIMI */
//   int nimenPituus = temp[4];
//   StringBuilder kurssinNimi = new StringBuilder();
//   for(int i= 5; i<5+nimenPituus; i++){
//   kurssinNimi.append((char)temp[i]);
//   }
//   this.kurssinNimi = kurssinNimi.toString();
//   
//   // jatkuu nimen j‰lkeen, eli paikasta 4 + nimenpituus
//   int mistaJatketaan = 4 + 8; // vakio 8, koska nimelle on aina varattu tasan 8 tavua
//   
//    /*SELVITETƒƒN KƒYTTƒJƒN NIMI */
//   // seuraava tavu sis‰lt‰‰ tiedon nimen pituudesta
//   
//   int KayttajanNimenPituus = temp[mistaJatketaan];
//   mistaJatketaan++;
//   
//   StringBuilder viestinLahettajanNimi = new StringBuilder(); 
//   int laskekierrokset = 0;
//   for(int i= mistaJatketaan; i<mistaJatketaan+KayttajanNimenPituus; i++){
//   	laskekierrokset++;
//       viestinLahettajanNimi.append((char)temp[i]);
//       }
//       this.viestinLahettajanNimi = viestinLahettajanNimi.toString();
//     if(tapahtuma=="joined") {
//   	  if (kayttajaLista.contains(viestinLahettajanNimi.toString())) ;
//   	  else kayttajaLista.add(this.viestinLahettajanNimi);
//     }
//     if(tapahtuma=="quit") kayttajaLista.remove(viestinLahettajanNimi.toString());
//   
//    mistaJatketaan +=laskekierrokset; // jatketaan siit‰ mihin nimen j‰lkeen j‰‰tiin
//   
//
//   /*SELVITETƒƒN VIESTI*/
//   int viestinpituus = temp[mistaJatketaan];
//   if(this.tapahtuma =="message") { 
//   mistaJatketaan++;
//   	StringBuilder viesti = new StringBuilder();
//   	for(int i = mistaJatketaan; i < mistaJatketaan+viestinpituus ; i++){
//   		if(temp[i] != 0) {
//   			viesti.append((char)temp[i]);
//   		}
//   		else break;
//   	}
//   	this.message = viesti.toString();
//   } 
//   else this.message = "";  // varmuudeksi viel‰ tyhj‰t‰‰n message, jos komento ei ollut message
//

// * Kokoaa tapahtuman tiedot: p‰iv‰m‰‰r‰, sovelluksen nimi, tapahtuma
// * @return info about received message 
// */
//public String tulosta() {
//	StringBuilder viesti = new StringBuilder("Day :" + getDay() + "." + getMonth() + "." + getYear() + " Course: "+ getKurssinNimi() + " User: " + getUser() + ":" + getHappening());
//	if(this.tapahtuma == "message")
//			viesti.append(" " +getMessage());
//	return viesti.toString();
//}

///**
//   private byte[] header = new byte[]{0x13, 0x6b, 0x7b, 0x30, 0x07,0x54,0x49,0x45,0x41,0x33,0x32,0x32,0x06,0x61,0x72,0x6a,0x75,0x76,0x69};
// //private byte[] headerJoin = new byte[]{0x11, 0x6b, 0x7b, 0x30, 0x07,0x54,0x49,0x45,0x41,0x33,0x32,0x32,0x06,0x61,0x72,0x6a,0x75,0x76,0x69};
//   private byte[] headerQuit = new byte[]{0x12, 0x6b, 0x7b, 0x30, 0x07,0x54,0x49,0x45,0x41,0x33,0x32,0x32,0x06,0x61,0x72,0x6a,0x75,0x76,0x69};
// * L‰hett‰‰ viestipaketin
// * @param teksti mik‰ teksti l‰hetet‰‰n
// * @param nimi k‰ytt‰j‰n nimi
// * @throws IOException
// */
//public void send(String teksti, String nimi) throws IOException {
//	
//	header = new byte[]{0x13, 0x6b, 0x7b, 0x30, 0x07,0x54,0x49,0x45,0x41,0x33,0x32,0x32}; // header ilman nime‰
//	// header sis‰lt‰‰ 12 tavua, loppuu vakiolliseen kurssin nimeen
//	this.data = new byte[header.length + 1 + nimi.length() + teksti.length()+1];  // datan koko = headerin koko + nimen koko 
//	System.arraycopy(header, 0, this.data, 0, header.length);  // dataan menee headeri
//	
//	// seuraavaan paikkaan menee nimen pituus
//	this.data[header.length] = (byte)nimi.length(); // ennen nime‰ tulee sen koko
//	// nimi sijoitetaan dataan
//	System.arraycopy(nimi.getBytes(), 0, this.data, 13, nimi.length()); // dataan laitetaan nimi
//	
//	
//	// viesti sijoitetaan dataan
//	this.data[header.length+nimi.length()+1] = (byte)teksti.length();
//	System.arraycopy(teksti.getBytes(), 0, this.data, header.length+1+nimi.length()+1, teksti.length()); // dataan laitetaan nimi
//	
//	
//	
//	this.pituus = this.data.length;
//	DatagramPacket p = new DatagramPacket(this.data, this.pituus, this.address, this.portti);
//	super.send(p);
//	
//	
//	
//	// IN CASE OF MISTAKES: THIS WILL WORK AS IT IS:
//	/*
//     this.data = new byte[this.header.length + 1 + teksti.length()];
//	System.arraycopy(this.header, 0, this.data, 0, this.header.length);
//	this.data[this.header.length] = (byte)teksti.length();
//	System.arraycopy(teksti.getBytes(), 0, this.data, this.header.length + 1, teksti.length());
//	this.pituus = this.data.length;
//	DatagramPacket p = new DatagramPacket(this.data, this.pituus, this.address, this.portti);
//	super.send(p);*/
//}
//
///**
// * L‰hett‰‰ Join-paketin
// * @param teksti mik‰ teksti loppuun
// * @param nimi K‰ytt‰j‰n nimi
// * @throws IOException
// */
//public void sendJoin(String teksti, String nimi) throws IOException {
//	
//	header = new byte[]{0x11, 0x6b, 0x7b, 0x30, 0x07,0x54,0x49,0x45,0x41,0x33,0x32,0x32}; // header ilman nime‰
//	// header sis‰lt‰‰ 12 tavua, loppuu vakiolliseen kurssin nimeen
//	this.data = new byte[header.length + 1 + nimi.length() + teksti.length()+1];  // datan koko = headerin koko + nimen koko 
//	System.arraycopy(header, 0, this.data, 0, header.length);  // dataan menee headeri
//	
//	// seuraavaan paikkaan menee nimen pituus
//	this.data[header.length] = (byte)nimi.length(); // ennen nime‰ tulee sen koko
//	// nimi sijoitetaan dataan
//	System.arraycopy(nimi.getBytes(), 0, this.data, 13, nimi.length()); // dataan laitetaan nimi
//	
//	
//	// viesti sijoitetaan dataan
//	this.data[header.length+nimi.length()+1] = (byte)teksti.length();
//	System.arraycopy(teksti.getBytes(), 0, this.data, header.length+1+nimi.length()+1, teksti.length()); // dataan laitetaan nimi
//	
//	
//	
//	this.pituus = this.data.length;
//	DatagramPacket p = new DatagramPacket(this.data, this.pituus, this.address, this.portti);
//	super.send(p);
//	
//	
//	
//	
//	
//	// WORKING ONE:
//	/*
//	//this.data = new byte[]{0x13, 0x6b, 0x7b, 0x30, 0x07,0x54,0x49,0x45,0x41,0x33,0x32,0x32,0x06,0x61,0x72,0x6a,0x75,0x76,0x69,0x05,0x74,0x65,0x72,0x76,0x65};
//	this.data = new byte[this.headerJoin.length + 1];
//	System.arraycopy(this.headerJoin, 0, this.data, 0, this.headerJoin.length);
//	//this.data[this.header.length] = (byte)teksti.length();
//	//System.arraycopy(teksti.getBytes(), 0, this.data, this.header.length + 1);
//	this.pituus = this.data.length;
//	DatagramPacket p = new DatagramPacket(this.data, this.pituus, this.address, this.portti);
//	super.send(p);
//	//this.data = new byte[]{0x13, 0x6b, 0x7b, 0x30, 0x07,0x54,0x49,0x45,0x41,0x33,0x32,0x32,0x06,0x61,0x72,0x6a,0x75,0x76,0x69,0x05,0x74,0x65,0x72,0x76,0x65};
//	// DatagramPacket p = new DatagramPacket(this.data, this.pituus, this.address, this.portti);
//	*/
//}
//
///**
// * L‰hett‰‰ poistumispaketin
// * @param teksti Mik‰ teksti loppuun
// * @param nimi K‰ytt‰j‰n nimi
// * @throws IOException
// */
//public void sendQuit(String teksti, String nimi) throws IOException {
//	
//	header = new byte[]{0x12, 0x6b, 0x7b, 0x30, 0x07,0x54,0x49,0x45,0x41,0x33,0x32,0x32}; // header ilman nime‰
//	// header sis‰lt‰‰ 12 tavua, loppuu vakiolliseen kurssin nimeen
//	this.data = new byte[header.length + 1 + nimi.length() + teksti.length()+1];  // datan koko = headerin koko + nimen koko 
//	System.arraycopy(header, 0, this.data, 0, header.length);  // dataan menee headeri
//	
//	// seuraavaan paikkaan menee nimen pituus
//	this.data[header.length] = (byte)nimi.length(); // ennen nime‰ tulee sen koko
//	// nimi sijoitetaan dataan
//	System.arraycopy(nimi.getBytes(), 0, this.data, 13, nimi.length()); // dataan laitetaan nimi
//	
//	
//	// viesti sijoitetaan dataan
//	this.data[header.length+nimi.length()+1] = (byte)teksti.length();
//	System.arraycopy(teksti.getBytes(), 0, this.data, header.length+1+nimi.length()+1, teksti.length()); // dataan laitetaan nimi
//	
//	
//	
//	this.pituus = this.data.length;
//	DatagramPacket p = new DatagramPacket(this.data, this.pituus, this.address, this.portti);
//	super.send(p);
//	
//	
//	/*WORKING ONE 
//	//this.data = new byte[]{0x13, 0x6b, 0x7b, 0x30, 0x07,0x54,0x49,0x45,0x41,0x33,0x32,0x32,0x06,0x61,0x72,0x6a,0x75,0x76,0x69,0x05,0x74,0x65,0x72,0x76,0x65};
//	this.data = null;
//	this.data = new byte[this.headerQuit.length +1];
//	System.arraycopy(this.headerQuit, 0, this.data, 0, this.headerQuit.length);
//	//this.data[this.header.length] = (byte)teksti.length();
//	//System.arraycopy(teksti.getBytes(), 0, this.data, this.header.length + 1);
//	this.pituus = this.data.length;
//	DatagramPacket p = new DatagramPacket(this.data, this.pituus, this.address, this.portti);
//	super.send(p);*/
//	
//}