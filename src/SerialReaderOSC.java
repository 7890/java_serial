import java.util.*;
import java.net.*;
import java.io.*;
import com.illposed.osc.*;

//tb/1603

/*

osc messages received/understood
================================

/split		split logfile
/quit		stop and shutdown serial reader

*/

//========================================================================
//========================================================================
class SerialReaderOSC extends SerialReader
{
	//this file is loaded if found in current directory
	private String propertiesFileUri="SerialReaderOSC.properties";

	//===configurable parameters (here: default values)
	public int	local_osc_port			=3040;
	public String	remote_osc_host			="127.0.0.1";
	public int	remote_osc_port			=3041;
	//===end configurable parameters

	private OSCPortOut portOut; //send
	private OSCPortIn portIn; //receive

//========================================================================
	public SerialReaderOSC(){}

//========================================================================
	public static void main(String[] args)
	{
		SerialReaderOSC sro=new SerialReaderOSC();

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
			System.err.println("Default properties file: ./"+sro.getPropertiesFileUri()+"\n");
			System.err.println("If no parameters provided, default values will be used.\n");
			System.exit(0);
		}

		try
		{
			if(args.length==2 && (args[0].equals("-c") || args[0].equals("--config")))
			{
				if(!sro.loadProps(args[1]))
				{
					System.err.println("Could not load properties "+args[1]);
					System.exit(1);
				}
			}
			else
			{
				sro.loadProps();
			}

			sro.init_osc_server(sro.local_osc_port,sro.remote_osc_host,sro.remote_osc_port);

			if(sro.hook_enabled)
			{
				sro.addHook(sro.hook_class);
			}
			if(sro.raw_log_to_file)
			{
				sro.createLogWriter(sro.createLogFileUri());
			}

			sro.addShutdownHook();

			//if args available but no config file given try parse port, baudrate
			if(args.length>0 && !(args[0].equals("-c") || args[0].equals("--config")))
			{
				String portname=args[0];
				int rate=9600;
				if(args.length>1)
				{
					rate=Integer.parseInt(args[1]);
				}
				sro.connectSerialPort(portname,rate);
			}
			else
			{
				sro.connectSerialPort(sro.serial_port,sro.baudrate);
			}

			sro.addEventListener();

			//check stop condition here
			while(!sro.shutdownRequested())
			{
				Thread.sleep(100);
			}
		}
		catch(Exception e)
		{
			System.err.println("SerialReaderOSC: "+e);
			System.exit(1);
		}
	}

//========================================================================
	public void init_osc_server(int local_port, String remote_host, int target_port) throws Exception
	{
		DatagramSocket ds=new DatagramSocket(local_port);
		portIn=new OSCPortIn(ds);

		portOut=new OSCPortOut(InetAddress.getByName(remote_host), target_port, ds);

		portIn.addListener("/*", new CommandListener());
		portIn.startListening();

		System.err.println("SerialReaderOSC: OSC server started on local port "+local_port);
		System.err.println("SerialReaderOSC: OSC sending messages to host "+remote_host+":"+target_port);
/*
portInSend.stopListening();
portInSend.close();
*/
	}

//========================================================================
	public void loadProps()
	{
		if(!loadProps(propertiesFileUri))
		{
			System.err.println("SerialReaderOSC: /!\\ Could not load properties: "+propertiesFileUri);
		}
	}

//========================================================================
	public boolean loadProps(String configfile_uri)
	{
		propertiesFileUri=configfile_uri;
		super.loadProps(super.getPropertiesFileUri());
		return LProps.load(propertiesFileUri,this);
	}

//inner class
//========================================================================
//========================================================================
class CommandListener implements OSCListener
{
//========================================================================
	public void accept(OSCMessage msg)
	{
		String path=msg.getAddress();
		java.util.List<Object> args=msg.getArguments();
		int argsSize=args.size();

		System.err.println("SerialReaderOSC: got message "+path);

		//split logfile (close current, create new)
		if(path.equals("/split") && argsSize==0)
		{
			try
			{
				splitLogfile();
			}
			catch(Exception e)
			{
				System.err.println("SerialReaderOSC: "+e);
			}
		}
		if(path.equals("/quit") && argsSize==0)
		{
			shutdown();
		}

	}//end accpept()

//========================================================================
	public void acceptMessage(Date time,OSCMessage msg) 
	{
		accept(msg);
	}
}//end inner class CommandListener
}//end classs SerialReaderOSC
//EOF
