import java.lang.*;
import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

import jssc.*;

//tb/1603

//========================================================================
//========================================================================
public class SerialReader
{
	private SerialPort serialPort=null;
	private PrintWriter writer_raw=null;
	//this file is loaded if found in current directory
	private String propertiesFileUri="SerialReader.properties";

	//===configurable parameters (here: default values)
	public String serial_port="/dev/ttyUSB0";
	public int baudrate=9600;
	//none | progress | raw
	public String stdout_show="raw";
	public boolean raw_log_to_file=false;
	//path where logfiles are put, directory will be created if not existing and logging enabled
	public String raw_log_file_base_path="./";
	public String raw_log_file_prefix="serial_log_";
	public String date_format_string="yyyy-MM-dd_HH-mm-ss";//.SSSZ";
	public String timezone_id="UTC";
	public String raw_log_file_postfix="_"+timezone_id+".nmea";
	public boolean hook_enabled=false;
	public String hook_class="";
	//===end configurable parameters

	private SimpleDateFormat date_format;
	private long total_bytes_received=0;
	private SerialHookInterface sh=null;

	private boolean shutdown_requested=false;

//========================================================================
	public SerialReader(){}

//========================================================================
	public static void main(String[] args)
	{
		SerialReader sr=new SerialReader();

		if(args.length==1
			&& (args[0].equals("-h") || args[0].equals("--help"))
			|| args.length>2
		)
		{
			System.err.println("Syntax: (serial portname (baudrate)) | -c (config file)\n");
			System.err.println("Example: /dev/ttyACM0 115200");
			System.err.println("Example: -c my.properties\n");
			System.err.println("Default portname: /dev/ttyUSB0");
			System.err.println("Default baudrate: 9600");
			System.err.println("Default properties file: ./"+sr.getPropertiesFileUri()+"\n");
			System.err.println("If no parameters provided, default values will be used.\n");
			System.exit(0);
		}
		else if(args.length==1 && (args[0].equals("-d") || args[0].equals("--dump")))
		{
			System.out.println("#default hardcoded values (without any .properties file loaded).");
			System.out.println("#the output contains a complete set of configurable keys and can be used as a template.");
			System.out.println("#'"+sr.propertiesFileUri+"' is the default name of the file being loaded if not explicitely set with -c");
			System.out.println(LProps.dumpObject(sr));
			System.exit(0);
		}
		try
		{
			if(args.length==2 && (args[0].equals("-c") || args[0].equals("--config")))
			{
				if(!sr.loadProps(args[1]))
				{
					System.err.println("Could not load properties "+args[1]);
					System.exit(1);
				}
			}
			else
			{
				sr.loadProps();
			}

			if(sr.hook_enabled)
			{
				sr.addHook(sr.hook_class);
			}
			if(sr.raw_log_to_file)
			{
				sr.createLogWriter(sr.createLogFileUri());
			}

			sr.addShutdownHook();

			//if args available but no config file given try parse port, baudrate
			if(args.length>0 && !(args[0].equals("-c") || args[0].equals("--config")))
			{
				String portname=args[0];
				int rate=9600;
				if(args.length>1)
				{
					rate=Integer.parseInt(args[1]);	
				}
				sr.connectSerialPort(portname,rate);
			}
			else
			{
				sr.connectSerialPort(sr.serial_port,sr.baudrate);
			}

			sr.addEventListener();

			//check stop condition here
			while(!sr.shutdownRequested())
			{
				Thread.sleep(100);
			}
		}
		catch (FileNotFoundException e)
		{
			System.err.println("SerialReader: "+e);
			System.err.println("If logging is enabled in the .properties file, make sure that the directory to write log files exists. ");
			System.exit(1);
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
					shutdown_requested=true;
					shutdown();
				}
			}
		});
	}

//========================================================================
	boolean shutdownRequested()
	{
		return shutdown_requested;
	}

//========================================================================
	String getPropertiesFileUri()
	{
		return propertiesFileUri;
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
	void addHook(String classname) throws Exception
	{
		hook_enabled=true;

		Class<?> c = Class.forName(classname);
		Constructor<?> cons = c.getConstructor();//String.class);
		sh = (SerialHookInterface)cons.newInstance();//"MyAttributeValue");
		sh.startup();
	}

//========================================================================
	String createLogFileUri() throws Exception
	{
		File log_dir=new File(raw_log_file_base_path);
		if(!log_dir.exists())
		{
			System.err.println("Creating log dir: "+raw_log_file_base_path);
			log_dir.mkdirs();
		}
		if(!log_dir.exists() || !log_dir.isDirectory() || !log_dir.canWrite())
		{
			throw new Exception("Invalid raw_log_file_base_path: "+raw_log_file_base_path);
		}

		date_format=new SimpleDateFormat(date_format_string);
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

		total_bytes_received=0;

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
	void loadProps()
	{
		if(!this.loadProps(propertiesFileUri))
		{
			System.err.println("SerialReader: /!\\ Could not load properties: "+propertiesFileUri);
		}
	}

//========================================================================
	public boolean loadProps(String configfile_uri)
	{
		propertiesFileUri=configfile_uri;
		return LProps.load(propertiesFileUri,this);
	}
}//end class SerialReader
