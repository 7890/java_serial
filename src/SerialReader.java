import jssc.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

//========================================================================
//========================================================================
public class SerialReader
{
	static SerialPort serialPort;
	static String portname="/dev/ttyUSB0";
	static int baudrate=9600;

	static PrintWriter writer_raw;

	//this file is loaded if found in current directory
	static String propertiesFileUri="SerialReader.properties";

	//===configurable parameters (here: default values)
	//none | progress | none
	static String stdout_show="raw";
	static boolean raw_log_to_file=false;
	static String raw_log_file_base_path="./";
	static String raw_log_file_prefix="serial_log_";
	static String date_format_string="yyyy-MM-dd_HH-mm-ss";//.SSSZ";
	static String timezone_id="UTC";
	static String raw_log_file_postfix="_"+timezone_id+".nmea";
	//===end configurable parameters

	static SimpleDateFormat date_format = new SimpleDateFormat(date_format_string);

	static long total_bytes_received=0;

//========================================================================
	public static void main(String[] args)
	{
		if(args.length<1)
		{
			System.err.println("Need port argument");
			System.err.println("Syntax: <serial portname> (baudrate)");
			System.err.println("Example: /dev/ttyACM0 115200");
			System.err.println("Default baudrate: "+baudrate);
			System.exit(1);
		}

		try
		{
			if(!loadProps(propertiesFileUri))
			{
				System.err.println("/!\\ Could not load properties");
			}

			if(raw_log_to_file)
			{
				date_format.setTimeZone(TimeZone.getTimeZone(timezone_id));
				String raw_log_file_name=raw_log_file_prefix+date_format.format(new Date())+raw_log_file_postfix;
				String raw_log_file_uri=raw_log_file_base_path+File.separator+raw_log_file_name;
				System.err.println("Writing to log file '"+raw_log_file_uri+"'");
				writer_raw=new PrintWriter(raw_log_file_uri, "UTF-8");
			}

			if(args.length>1)
			{
				baudrate=Integer.parseInt(args[1]);	
			}

			portname=args[0];
			serialPort=new SerialPort(portname); 

			serialPort.openPort();
			serialPort.setParams(baudrate, 8, 1, 0);
			serialPort.setEventsMask(SerialPort.MASK_RXCHAR);
			serialPort.addEventListener(new SerialPortReader());
			//serialPort.writeString("HelloWorld");
		}
		catch (FileNotFoundException e)
		{
			System.out.println(e);
			System.exit(1);
		}
		catch (UnsupportedEncodingException e)
		{
			System.out.println(e);
			System.exit(1);
		}
		catch (SerialPortException e)
		{
			System.out.println(e);
			System.exit(1);
		}
	}//end main()

	//inner class
//========================================================================
	static class SerialPortReader implements SerialPortEventListener
	{
		public void serialEvent(SerialPortEvent event)
		{
			//data available in buffer
			if(event.isRXCHAR())
			{
				try 
				{
					int bytes_available=event.getEventValue();
					total_bytes_received+=bytes_available;

					//read all available bytes to string
					String data= serialPort.readString(bytes_available);
					if(stdout_show.equals("raw"))
					{
						System.out.print(data);
					}
					else if(stdout_show.equals("progress"))
					{
						System.out.print("\r"+total_bytes_received);
					}
					//else if (none)

					if(raw_log_to_file)
					{
						writer_raw.print(data);
						writer_raw.flush();
					}
				}
				catch(SerialPortException e)
				{
					System.err.println(e);
				}
			}
			//If the CTS line status has changed, then the method event.getEventValue() returns 1 if the line is ON and 0 if it is OFF.
			else if(event.isCTS())
			{
				if(event.getEventValue()==1)
				{
					System.err.println("CTS - ON");
				}
				else
				{
					System.err.println("CTS - OFF");
				}
			}
			else if(event.isDSR())
			{
				if(event.getEventValue()==1)
				{
					System.err.println("DSR - ON");
				}
				else
				{
					System.err.println("DSR - OFF");
				}
			}
		}//end serialEvent
	}//endclass SerialPortReader

//========================================================================
	public static boolean loadProps(String configfile_uri)
	{
		Properties props=new Properties();
		InputStream is=null;
 
		//try loading from the current directory
		try 
		{
			File f=new File(configfile_uri);
			if(f.exists() && f.canRead())
			{
				is=new FileInputStream(f);
				if(is!=null)
				{
					props.load(is);
					if(props!=null)
					{
						if(props.getProperty("stdout_show")!=null)
							{stdout_show=props.getProperty("stdout_show");}
						if(props.getProperty("raw_log_to_file")!=null)
							{raw_log_to_file=Boolean.parseBoolean(props.getProperty("raw_log_to_file"));}
						if(props.getProperty("raw_log_file_base_path")!=null)
							{raw_log_file_base_path=props.getProperty("raw_log_file_base_path");}
						if(props.getProperty("raw_log_file_prefix")!=null)
							{raw_log_file_prefix=props.getProperty("raw_log_file_prefix");}
						if(props.getProperty("date_format_string")!=null)
							{date_format_string=props.getProperty("date_format_string");}
						if(props.getProperty("timezone_id")!=null)
							{timezone_id=props.getProperty("timezone_id");}
						if(props.getProperty("raw_log_file_postfix")!=null)
							{raw_log_file_postfix=props.getProperty("raw_log_file_postfix");}
						//if(props.getProperty("")!=null){=props.getProperty("");}
						return true;
					}
				}
			}//end if file exists
		}//end try
		catch(Exception e)
		{
			System.err.println(e);
		}
		return false;
	}//end loadProps
}//end class SerialReader
