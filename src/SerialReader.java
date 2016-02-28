import jssc.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.lang.*;
import java.lang.reflect.*;

//========================================================================
//========================================================================
public class SerialReader
{
	SerialPort serialPort=null;
	PrintWriter writer_raw=null;
	//this file is loaded if found in current directory
	String propertiesFileUri="SerialReader.properties";

	//===configurable parameters (here: default values)
	//none | progress | none
	String stdout_show="raw";
	boolean raw_log_to_file=false;
	String raw_log_file_base_path="./";
	String raw_log_file_prefix="serial_log_";
	String date_format_string="yyyy-MM-dd_HH-mm-ss";//.SSSZ";
	String timezone_id="UTC";
	String raw_log_file_postfix="_"+timezone_id+".nmea";
	boolean hook_enabled=false;
	String hook_class="";
	//===end configurable parameters

	SimpleDateFormat date_format=new SimpleDateFormat(date_format_string);
	long total_bytes_received=0;
	SerialHookInterface sh=null;

	boolean shutdown_requested=false;

//========================================================================
	public SerialReader(){}

//========================================================================
	public static void main(String[] args)
	{
		if(args.length<1)
		{
			System.err.println("Need port argument");
			System.err.println("Syntax: <serial portname> (baudrate)");
			System.err.println("Example: /dev/ttyACM0 115200");
			System.err.println("Default baudrate: 9600");
			System.exit(1);
		}

		SerialReader sr=new SerialReader();

		try
		{
			sr.loadProps();

			if(sr.hook_enabled)
			{
				sr.addHook(sr.hook_class);
			}
			if(sr.raw_log_to_file)
			{
				sr.createLogWriter(sr.createLogFileUri());
			}

			sr.addShutdownHook();

			int baudrate=9600;
			if(args.length>1)
			{
				baudrate=Integer.parseInt(args[1]);	
			}

			String portname=args[0];

			sr.connectSerialPort(portname,baudrate);
			sr.addEventListener();

			//check stop condition here
			while(!sr.shutdown_requested)
			{
				Thread.sleep(100);
			}
		}
		catch (Exception e)
		{
			System.err.println("SerialReader: "+e);
			System.exit(1);
		}
	}//end main()

	//catch ctrl+c / term signal
//========================================================================
	void addShutdownHook()
	{
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			public void run()
			{
				//prevent second shutdown if shutdown() called directly
				if(!shutdown_requested)
				{
					shutdown();
				}
			}
		});
	}

//========================================================================
	void shutdown()
	{
		System.err.println("SerialReader: shutdown()");
		try
		{
			if(serialPort!=null)
			{
				serialPort.removeEventListener();
				serialPort.closePort();
			}

			if(raw_log_to_file && writer_raw!=null)
			{
				writer_raw.close();
			}
			if(hook_enabled && sh!=null)
			{
				sh.shutdown();
			}
		}
		catch(Exception e)
		{
			System.err.println("SerialReader: "+e);
		}
		shutdown_requested=true;
	}

//========================================================================
	void loadProps()
	{
		if(!this.loadProps(propertiesFileUri))
		{
			System.err.println("SerialReader: /!\\ Could not load properties: "+propertiesFileUri);
		}
	}

//========================================================================
	void addHook(String classname) throws Exception
	{
		hook_enabled=true;

		Class<?> c = Class.forName(classname);
		Constructor<?> cons = c.getConstructor();//String.class);
		sh = (SerialHookInterface)cons.newInstance();//"MyAttributeValue");
		sh.startup();
	}

//========================================================================
	String createLogFileUri()
	{
		date_format.setTimeZone(TimeZone.getTimeZone(timezone_id));
		String raw_log_file_name=raw_log_file_prefix+date_format.format(new Date())+raw_log_file_postfix;
		String raw_log_file_uri=raw_log_file_base_path+File.separator+raw_log_file_name;

		if(hook_enabled)
		{
			sh.logFile(raw_log_file_base_path,raw_log_file_name);
		}

		return raw_log_file_uri;
	}

//========================================================================
	void createLogWriter(String log_file_uri) throws Exception
	{
		writer_raw=new PrintWriter(log_file_uri, "UTF-8");
		System.err.println("SerialReader: Logging to file "+log_file_uri);
	}

//========================================================================
	void splitLogfile() throws Exception
	{
		System.err.println("SerialReader: Split logfile");
		if(serialPort!=null)
		{
			serialPort.removeEventListener();
		}

		if(raw_log_to_file && writer_raw!=null)
		{
			writer_raw.close();
		}

		if(hook_enabled && sh!=null)
		{///
		}

		if(raw_log_to_file)
		{
			createLogWriter(createLogFileUri());
		}
		addEventListener();
	}

//========================================================================
	void connectSerialPort(String portname, int baudrate) throws Exception
	{
		serialPort=new SerialPort(portname); 

		serialPort.openPort();
		serialPort.setParams(baudrate, 8, 1, 0);

		if(hook_enabled)
		{
			sh.portConnect(portname,baudrate);
		}

		serialPort.setEventsMask(SerialPort.MASK_RXCHAR);
	}

//========================================================================
	void addEventListener() throws Exception
	{
		serialPort.addEventListener(new SerialPortReader());
	}

	//inner class
//========================================================================
	class SerialPortReader implements SerialPortEventListener
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

					if(hook_enabled)
					{
						sh.serialData(data);
					}

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
					System.err.println("SerialReader: "+e);
				}
			}
			//If the CTS line status has changed, then the method event.getEventValue() returns 1 if the line is ON and 0 if it is OFF.
			else if(event.isCTS())
			{
				if(event.getEventValue()==1)
				{
					System.err.println("SerialReader: CTS - ON");
				}
				else
				{
					System.err.println("SerialReader: CTS - OFF");
				}
			}
			else if(event.isDSR())
			{
				if(event.getEventValue()==1)
				{
					System.err.println("SerialReader: DSR - ON");
				}
				else
				{
					System.err.println("SerialReader: DSR - OFF");
				}
			}
		}//end serialEvent
	}//endclass SerialPortReader

//========================================================================
	public boolean loadProps(String configfile_uri)
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
						if(props.getProperty("hook_enabled")!=null)
							{hook_enabled=Boolean.parseBoolean(props.getProperty("hook_enabled"));}
						if(props.getProperty("hook_class")!=null)
							{hook_class=props.getProperty("hook_class");}
						//if(props.getProperty("")!=null){=props.getProperty("");}
						return true;
					}
				}
			}//end if file exists
		}//end try
		catch(Exception e)
		{
			System.err.println("SerialReader: "+e);
		}
		return false;
	}//end loadProps
}//end class SerialReader
