import java.util.HashMap;
import java.util.Map;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/*
//tb/160224
This class reads raw NMEA files dumped from a GNSS/GPS receiver, parses the sentences and prints CSV style output.
THIS IS NOT A COMPLETE PARSER. The program tries to parse some common sentences with talker IDs GP, GN, GL, GA and GB.
On linux: see gpsd, gpsdecode

Code is based on this gist: https://gist.github.com/javisantana/1326141
This software is under the terms of MIT license: http://opensource.org/licenses/MIT

Excellent NMEA resource here: http://www.catb.org/gpsd/NMEA.html
(a local copy can be found in archive folder)

According to [UNMEA], the NMEA standard requires that a field (such as altitude, latitude, or longitude) 
must be left empty when the GPS has no valid data for it. However, many receivers violate this. 
It’s common, for example, to see latitude/longitude/altitude figures filled with zeros when the GPS has no valid data.
*/

//=============================================================================
//=============================================================================
public class NMEA
{	
	GPSPosition position = new GPSPosition();
	
	Map<String, SentenceParser> sentenceParsers = new HashMap<String, SentenceParser>();

	//1 knot = 0.5144444444 meter per second
	static final double knot_to_meters_coeff=1.0f/0.5144444444;

	boolean debug=false;
	boolean read_from_stdin=false;
	boolean write_to_stdin=true;

	BufferedReader buffered_reader=null;
	PrintWriter print_writer=null;

	//sentences with talker ID GP (Global Positioning System receiver)
	static String GP_sentences[]={"GPGGA","GPGLL","GPRMC","GPVTG","GPGST","GPGNS","GPGSA","GPZDA"};

	//sentences with talker ID GN (Mixed GPS and GLONASS data, according to IEIC 61162-1)
	static String GN_sentences[]={"GNGGA","GNGLL","GNRMC","GNVTG","GNGST","GNGNS","GNGSA","GNZDA"};

	//sentences with talker ID GL (GLONASS, according to IEIC 61162-1)
	static String GL_sentences[]={"GLGGA","GLGLL","GLRMC","GLVTG","GLGST","GLGNS","GLGSA","GLZDA"};

	//sentences with talker ID GA (Galileo Positioning System)
	static String GA_sentences[]={"GAGGA","GAGLL","GARMC","GAVTG","GAGST","GAGNS","GAGSA","GAZDA"};

	//sentences with talker ID GB (BeiDou (China))
	//*QZ is an alternative talker ID for BeiDou not supported here (only sentences starting with 'G' for simplicity)
	static String GB_sentences[]={"GBGGA","GBGLL","GBRMC","GBVTG","GBGST","GBGNS","GBGSA","GBZDA"};

//=============================================================================
	public NMEA()
	{
		//you might want to change this.
		//by default all known sentences will be parsed
		addAllGPSentences();
		addAllGNSentences();
		addAllGLSentences();
		addAllGASentences();
		addAllGBSentences();

		DTime.setTimeZoneUTC();
		//GSV (satellites in view)
	}//end constructor

//=============================================================================
	void removeAllSentences()
	{
		sentenceParsers.clear();
	}

//=============================================================================
	boolean addAllGPSentences()
	{
		return addSentences(GP_sentences);
	}
//=============================================================================
	boolean addAllGNSentences()
	{
		return addSentences(GN_sentences);
	}
//=============================================================================
	boolean addAllGLSentences()
	{
		return addSentences(GL_sentences);
	}
//=============================================================================
	boolean addAllGASentences()
	{
		return addSentences(GA_sentences);
	}
//=============================================================================
	boolean addAllGBSentences()
	{
		return addSentences(GB_sentences);
	}

//=============================================================================
	boolean addSentences(String[] sentences)
	{
		boolean success=true;
		for(int i=0;i<sentences.length;i++)
		{
			success=success && addSentence(sentences[i]);
		}
		return success;
	}

//=============================================================================
	boolean addSentence(String sentence)
	{
		if(sentence.length()!=5 || !sentence.startsWith("G"))
		{
			if(debug)
			{
				System.err.println("sentence has invalid length or doesn't start with 'G': "+sentence);
			}
			return false;
		}
		String sentence_type=sentence.substring(2,5);

		if(sentence_type.equals("GGA"))
		{
			sentenceParsers.put(sentence, new GxGGA());
		}
		else if(sentence_type.equals("GLL"))
		{
			sentenceParsers.put(sentence, new GxGLL());
		}
		else if(sentence_type.equals("RMC"))
		{
			sentenceParsers.put(sentence, new GxRMC());
		}
		else if(sentence_type.equals("VTG"))
		{
			sentenceParsers.put(sentence, new GxVTG());
		}
		else if(sentence_type.equals("GST"))
		{
			sentenceParsers.put(sentence, new GxGST());
		}
		else if(sentence_type.equals("GNS"))
		{
			sentenceParsers.put(sentence, new GxGNS());
		}
		else if(sentence_type.equals("GSA"))
		{
			sentenceParsers.put(sentence, new GxGSA());
		}
		else if(sentence_type.equals("ZDA"))
		{
			sentenceParsers.put(sentence, new GxZDA());
		}

		if(debug)
		{
			System.err.println("sentence added: "+sentence+" (Gx"+sentence_type+")");
		}

		return true;
	}//end addSentence()

//=============================================================================
	void setDebug(boolean debug)
	{
		this.debug=debug;
	}

//=============================================================================
	void setReadFromStdIn()
	{
		this.read_from_stdin=true;
		try
		{
			buffered_reader=new BufferedReader(new InputStreamReader(System.in));
		}
		catch(Exception e)
		{
			System.err.println(e);
			System.exit(1);
		}
	}

//=============================================================================
	void setReadFromFile(String file_uri)
	{
		this.read_from_stdin=false;
		try
		{
			buffered_reader=new BufferedReader(new FileReader(file_uri));
		}
		catch(Exception e)
		{
			System.err.println(e);
			System.exit(1);
		}
	}

//=============================================================================
	void setWriteToStdIn()
	{
		this.write_to_stdin=true;
	}

//=============================================================================
	void setWriteToFile(String file_uri)
	{
		this.write_to_stdin=false;
		try
		{
			print_writer=new PrintWriter(file_uri, "UTF-8");
		}
		catch(Exception e)
		{
			System.err.println(e);
			System.exit(1);
		}
	}

//=============================================================================
	void startProcessingInput()
	{
		GPSPosition pos;
		try
		{
			if(write_to_stdin)
			{
				System.out.println(position.getCSVHeader());
			}
			else if(print_writer!=null)
			{
				print_writer.println(position.getCSVHeader());
			}
			String line;
			while ((line = buffered_reader.readLine()) != null)
			{
				if(debug)
				{
					System.err.println("input line: '"+line+"'");
				}
				pos=parseLine(line);
				if(pos!=null && pos.last_sentence_type.equals("RMC")) ///
				{
					if(write_to_stdin)
					{
						System.out.println(pos);
					}
					else if(print_writer!=null)
					{
						print_writer.println(pos);
					}
				}
			}
			if(print_writer!=null)
			{
				print_writer.flush();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.err.println("Could not parse file");
			System.exit(1);
		}
	}

//=============================================================================
	public static void main(String[] args)
	{
		if(args.length<1)
		{
			System.err.println("Need file argument (- to read from stdin)");
			System.err.println("Syntax: <file to parse> (outfile)");
			System.exit(1);
		}

		NMEA n=new NMEA();

		if(args[0].equals("-"))
		{
			n.setReadFromStdIn();
		}
		else
		{
			n.setReadFromFile(args[0]);
		}

		if(args.length>1)
		{
			 n.setWriteToFile(args[1]);
			///was:
			//n.setDebug(true);
		}
		else
		{
			n.setWriteToStdIn();
		}
		n.startProcessingInput();
	}//end main

//=============================================================================
	void print_tokens(String[] tokens)
	{
		if(debug)
		{
			for(int i=0;i<tokens.length;i++)
			{
				System.err.println(i+": "+tokens[i]);
			}
		}
	}

//=============================================================================
	void print_parse_error(String hint)
	{
		if(debug)
		{
			System.err.println("parse error ("+hint+")");
		}
	}

//=============================================================================	
	static float Latitude2Decimal(String lat, String NS)
	{
		float med = Float.parseFloat(lat.substring(2))/60.0f;
		med += Float.parseFloat(lat.substring(0, 2));
		if(NS.startsWith("S"))
		{
			med = -med;
		}
		return med;
	}

//=============================================================================
	static float Longitude2Decimal(String lon, String WE)
	{
		float med = Float.parseFloat(lon.substring(3))/60.0f;
		med += Float.parseFloat(lon.substring(0, 3));
		if(WE.startsWith("W"))
		{
			med = -med;
		}
		return med;
	}

//=============================================================================	
	interface SentenceParser
	{
		public void parse(String [] tokens, GPSPosition position);
	}

//Parsers 
//=============================================================================
/*

GGA - Global Positioning System Fix Data

This is one of the sentences commonly emitted by GPS units.

Time, Position and fix related data for a GPS receiver.

                                                      11
        1         2       3 4        5 6 7  8   9  10 |  12 13  14   15
        |         |       | |        | | |  |   |   | |   | |   |    |
 $--GGA,hhmmss.ss,llll.ll,a,yyyyy.yy,a,x,xx,x.x,x.x,M,x.x,M,x.x,xxxx*hh<CR><LF>

Field Number:
1    Universal Time Coordinated (UTC)
2    Latitude
3    N or S (North or South)
4    Longitude
5    E or W (East or West)
6    GPS Quality Indicator,
        0 - fix not available,
        1 - GPS fix,
        2 - Differential GPS fix (values above 2 are 2.3 features)
        3 = PPS fix
        4 = Real Time Kinematic
        5 = Float RTK
        6 = estimated (dead reckoning)
        7 = Manual input mode
        8 = Simulation mode
7    Number of satellites in view, 00 - 12
8    Horizontal Dilution of precision (meters)
9    Antenna Altitude above/below mean-sea-level (geoid) (in meters)
10   Units of antenna altitude, meters
11   Geoidal separation, the difference between the WGS-84 earth ellipsoid and mean-sea-level (geoid), "-" means mean-sea-level below ellipsoid
12   Units of geoidal separation, meters
13   Age of differential GPS data, time in seconds since last SC104 type 1 or 9 update, null field when DGPS is not used
14   Differential reference station ID, 0000-1023
15   Checksum
*/
	class GxGGA implements SentenceParser
	{
		public void parse(String [] tokens, GPSPosition position)
		{
			print_tokens(tokens);
			position.last_sentence_type="GGA";
			try{position.time = Float.parseFloat(tokens[1]);}catch(Exception e1){print_parse_error("time");}
			try{position.lat = Latitude2Decimal(tokens[2], tokens[3]);}catch(Exception e1){print_parse_error("lat");}
			try{position.lon = Longitude2Decimal(tokens[4], tokens[5]);}catch(Exception e1){print_parse_error("lon");}
			try{position.quality = Integer.parseInt(tokens[6]);}catch(Exception e1){print_parse_error("quality");}
			try{position.sat_in_use = Integer.parseInt(tokens[7]);}catch(Exception e1){print_parse_error("sat_in_use");}
			try{position.HDOP = Float.parseFloat(tokens[8]);}catch(Exception e){print_parse_error("HDOP");}
			try{position.altitude = Float.parseFloat(tokens[9]);}catch(Exception e1){print_parse_error("altitude");}
			try{position.dgps_age = Float.parseFloat(tokens[13]);}catch(Exception e1){print_parse_error("dgps_age");}
		}
	}
	
//=============================================================================
/*                                                          12
        1         2 3       4 5        6  7   8   9    10 11|  13
        |         | |       | |        |  |   |   |    |  | |   |
 $--RMC,hhmmss.ss,A,llll.ll,a,yyyyy.yy,a,x.x,x.x,xxxx,x.x,a,m,*hh<CR><LF>

Field Number:
1     UTC Time
2     Status, V=Navigation receiver warning A=Valid
3     Latitude
4     N or S
5     Longitude
6     E or W
7     Speed over ground, knots
8     Track made good, degrees true
9     Date, ddmmyy
10    Magnetic Variation, degrees
11    E or W
12    FAA mode indicator (NMEA 2.3 and later)
13    Checksum

A status of V means the GPS has a valid fix that is below an internal quality threshold, e.g. because the dilution of precision is too high or an elevation mask test failed.
*/
	class GxRMC implements SentenceParser
	{
		public void parse(String [] tokens, GPSPosition position)
		{
			print_tokens(tokens);
			position.last_sentence_type="RMC";
			try{position.millis_utc_sys=DTime.nowMillis();}catch(Exception e8){print_parse_error("millis_utc_sys");}
			try{position.time = Float.parseFloat(tokens[1]);}catch(Exception e1){print_parse_error("time");}
			try{position.lat = Latitude2Decimal(tokens[3], tokens[4]);}catch(Exception e2){print_parse_error("lat");}
			try{position.lon = Longitude2Decimal(tokens[5], tokens[6]);}catch(Exception e3){print_parse_error("lon");}
			try{position.velocity = (float)(Float.parseFloat(tokens[7]) * knot_to_meters_coeff);}catch(Exception e4){print_parse_error("velocity");}
			try{position.direction = Float.parseFloat(tokens[8]);}catch(Exception e5){print_parse_error("direction");}
			try{position.date = DTime.yyyymmdd_from_ddmmyy(tokens[9]);}catch(Exception e6){print_parse_error("date");}
			try{position.millis_utc
				= DTime.millisFrom_yyyymmdd(position.date)
				+ DTime.millisFrom_HHMMSSpSSS(""+DTime.formatTimeLeadingZeros(position.time));
			}catch(Exception e7){print_parse_error("millis_utc");}

		}
	}

//=============================================================================
/*
GLL - Geographic Position - Latitude/Longitude

This is one of the sentences commonly emitted by GPS units.

        1       2 3        4 5         6 7   8
        |       | |        | |         | |   |
 $--GLL,llll.ll,a,yyyyy.yy,a,hhmmss.ss,a,m,*hh<CR><LF>

Field Number:
1    Latitude
2    N or S (North or South)
3    Longitude
4    E or W (East or West)
5    Universal Time Coordinated (UTC)
6    Status A - Data Valid, V - Data Invalid
7    FAA mode indicator (NMEA 2.3 and later)
8    Checksum
*/
	class GxGLL implements SentenceParser
	{
		public void parse(String [] tokens, GPSPosition position)
		{
			print_tokens(tokens);
			position.last_sentence_type="GLL";
			try{position.lat = Latitude2Decimal(tokens[1], tokens[2]);}catch(Exception e){print_parse_error("lat");}
			try{position.lon = Longitude2Decimal(tokens[3], tokens[4]);}catch(Exception e){print_parse_error("lon");}
			try{position.time = Float.parseFloat(tokens[5]);}catch(Exception e){print_parse_error("time");}
		}
	}

//=============================================================================
/*
GST - GPS Pseudorange Noise Statistics

              1    2 3 4 5 6 7 8   9
              |    | | | | | | |   |
 $ --GST,hhmmss.ss,x,x,x,x,x,x,x,*hh<CR><LF>

Field Number:
1    TC time of associated GGA fix
2    Total RMS standard deviation of ranges inputs to the navigation solution
3    Standard deviation (meters) of semi-major axis of error ellipse
4    Standard deviation (meters) of semi-minor axis of error ellipse
5    Orientation of semi-major axis of error ellipse (true north degrees)
6    Standard deviation (meters) of latitude error
7    Standard deviation (meters) of longitude error
8    Standard deviation (meters) of altitude error
9    Checksum
*/
	class GxGST implements SentenceParser
	{
		public void parse(String [] tokens, GPSPosition position)
		{
			print_tokens(tokens);
			position.last_sentence_type="GST";
			try{position.lat_err = Float.parseFloat(tokens[6]);}catch(Exception e){print_parse_error("lat_err");}
			try{position.lon_err = Float.parseFloat(tokens[7]);}catch(Exception e){print_parse_error("lon_err");}
			try{position.alt_err = Float.parseFloat(tokens[8]);}catch(Exception e){print_parse_error("alt_err");}
		}
	}

//=============================================================================
/*
VTG - Track made good and Ground speed

This is one of the sentences commonly emitted by GPS units.

         1  2  3  4  5  6  7  8 9   10
         |  |  |  |  |  |  |  | |   |
 $--VTG,x.x,T,x.x,M,x.x,N,x.x,K,m,*hh<CR><LF>

Field Number:
1    Track Degrees
2    T = True
3    Track Degrees
4    M = Magnetic
5    Speed Knots
6    N = Knots
7    Speed Kilometers Per Hour
8    K = Kilometers Per Hour
9    FAA mode indicator (NMEA 2.3 and later)
*/

//,"$GNVTG,,T,,M,0.009,N,0.016,K,D*36"
	
	class GxVTG implements SentenceParser
	{
		public void parse(String [] tokens, GPSPosition position)
		{
			print_tokens(tokens);
			position.last_sentence_type="VTG";
			try{position.direction = Float.parseFloat(tokens[1]);}catch(Exception e){print_parse_error("direction");}
//			try{position.direction = Float.parseFloat(tokens[3]);}catch(Exception e){print_parse_error("direction");}
			try{position.velocity = (float)(Float.parseFloat(tokens[5]) * knot_to_meters_coeff);}catch(Exception e4){print_parse_error("velocity");}
		}
	}

//=============================================================================
/*
GNS - Fix data

       1         2       3 4        5 6    7  8   9   10  11  12  13
       |         |       | |        | |    |  |   |   |   |   |   |
$--GNS,hhmmss.ss,llll.ll,a,yyyyy.yy,a,c--c,xx,x.x,x.x,x.x,x.x,x.x*hh

Field Number:
1    UTC
2    Latitude
3    N or S (North or South)
4    Longitude
5    E or W (East or West)
6    Mode indicator (non-null)
7    Total number of satelites in use,00-99
8    HDROP
9    Antenna altitude, meters, re:mean-sea-level(geoid.
10   Goeidal separation meters
11   Age of diferential data
12   Differential reference station ID
13   Checksum
The Mode indicator is two or more characters, with the first and second defined for GPS and GLONASS. Further characters may be defined. For each system, the character can have a value (table may be incomplete):
    N = Constellation not in use, or no valid fix
    A = Autonomous (non-differential)
    D = Differential mode
    P = Precise (no degradation, like Selective Availability)
    R = Real Time Kinematic
    F = Float RTK
    E = Estimated (dead reckoning) Mode
    M = Manual Input Mode
    S = Simulator Mode
*/

	class GxGNS implements SentenceParser
	{
		public void parse(String [] tokens, GPSPosition position)
		{
			print_tokens(tokens);
			position.last_sentence_type="GNS";
			try{position.time = Float.parseFloat(tokens[1]);}catch(Exception e){print_parse_error("time");}
			try{position.lat = Latitude2Decimal(tokens[2], tokens[3]);}catch(Exception e){print_parse_error("lat");}
			try{position.lon = Longitude2Decimal(tokens[4], tokens[5]);}catch(Exception e){print_parse_error("lon");}
			try{position.sat_in_use = Integer.parseInt(tokens[7]);}catch(Exception e1){print_parse_error("sat_in_use");}
			try{position.altitude = Float.parseFloat(tokens[9]);}catch(Exception e1){print_parse_error("altitude");}
			try{position.dgps_age = Float.parseFloat(tokens[11]);}catch(Exception e1){print_parse_error("dgps_age");}
			try{position.mode = tokens[6].substring(0,1);}catch(Exception e){print_parse_error("mode");}
		}
	}

//=============================================================================
/*
GSA - GPS DOP and active satellites

This is one of the sentences commonly emitted by GPS units.

        1 2 3                        14 15  16  17  18
        | | |                         |  |   |   |   |
 $--GSA,a,a,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x.x,x.x,x.x*hh<CR><LF>

Field Number:
1    Selection mode: M=Manual, forced to operate in 2D or 3D, A=Automatic, 3D/2D
2    Mode (1 = no fix, 2 = 2D fix, 3 = 3D fix)
3    ID of 1st satellite used for fix
4    ID of 2nd satellite used for fix
5    ID of 3rd satellite used for fix
6    ID of 4th satellite used for fix
7    ID of 5th satellite used for fix
8    ID of 6th satellite used for fix
9    ID of 7th satellite used for fix
10   ID of 8th satellite used for fix
11   ID of 9th satellite used for fix
12   ID of 10th satellite used for fix
13   ID of 11th satellite used for fix
14   ID of 12th satellite used for fix
15   PDOP
16   HDOP
17   VDOP
18   Checksum

DOP: Dilution of precision
HDOP, VDOP, PDOP, and TDOP are respectively Horizontal, Vertical, Position (3D), and Time Dilution of Precision.

DOP Value 	Rating 		Description
< 1 		Ideal 		Highest possible confidence level to be used for applications demanding the highest possible precision at all times.
1-2 		Excellent 	At this confidence level, positional measurements are considered accurate enough to meet all but the most sensitive applications.
2-5 		Good 		Represents a level that marks the minimum appropriate for making business decisions. Positional measurements could be used to make reliable in-route navigation suggestions to the user.
5-10 		Moderate 	Positional measurements could be used for calculations, but the fix quality could still be improved. A more open view of the sky is recommended.
10-20 		Fair 		Represents a low confidence level. Positional measurements should be discarded or used only to indicate a very rough estimate of the current location.
>20 		Poor 		At this level, measurements are inaccurate by as much as 300 meters with a 6-meter accurate device (50 DOP × 6 meters) and should be discarded.
*/

	class GxGSA implements SentenceParser
	{
		public void parse(String [] tokens, GPSPosition position)
		{
			print_tokens(tokens);
			position.last_sentence_type="GSA";
			try{position.fix_type = Integer.parseInt(tokens[2]);}catch(Exception e1){print_parse_error("fix_type");}
			try{position.PDOP = Float.parseFloat(tokens[15]);}catch(Exception e){print_parse_error("PDOP");}
			try{position.HDOP = Float.parseFloat(tokens[16]);}catch(Exception e){print_parse_error("HDOP");}
			try{position.VDOP = Float.parseFloat(tokens[17]);}catch(Exception e){print_parse_error("VDOP");}
		}
	}

//=============================================================================
/*
ZDA - Time & Date - UTC, day, month, year and local time zone

This is one of the sentences commonly emitted by GPS units.

        1         2  3  4    5  6  7
        |         |  |  |    |  |  |
 $--ZDA,hhmmss.ss,xx,xx,xxxx,xx,xx*hh<CR><LF>

Field Number:
1    UTC time (hours, minutes, seconds, may have fractional subsecond)
2    Day, 01 to 31
3    Month, 01 to 12
4    Year (4 digits)
5    Local zone description, 00 to +- 13 hours
6    Local zone minutes description, apply same sign as local hours
7    Checksum

Example: $GPZDA,160012.71,11,03,2004,-1,00*7D
*/
	class GxZDA implements SentenceParser
	{
		public void parse(String [] tokens, GPSPosition position)
		{
			print_tokens(tokens);
			position.last_sentence_type="ZDA";
			try{position.local_tz = Integer.parseInt(tokens[5]);}catch(Exception e){print_parse_error("local_tz");}
		}
	}

//=============================================================================	
	public GPSPosition parseLine(String line)
	{
		if(line.startsWith("$"))
		{
			String nmea = line.substring(1);
			String[] tokens = nmea.split(",|\\*"); //split comma, * (last token, crc)
			String type = tokens[0];
			//TODO check crc
			if(sentenceParsers.containsKey(type))
			{
				sentenceParsers.get(type).parse(tokens, position);
				position.updatefix();
				return new GPSPosition(position);
			}
			else if(debug)
			{
				System.err.println("UNKOWN SENTENCE: "+type);
			}
		}
		return null;
	}
}//end class NEMA
//EOF
