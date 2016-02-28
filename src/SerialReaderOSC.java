import java.util.*;
import java.net.*;
import java.io.*;
import com.illposed.osc.*;

//========================================================================
//========================================================================
class SerialReaderOSC extends SerialReader
{
	//this file is loaded if found in current directory
	String propertiesFileUri="SerialReaderOSC.properties";

	//===configurable parameters (here: default values)
	int	local_osc_port			=3040;
	String	remote_osc_host			="127.0.0.1";
	int	remote_osc_port			=3041;
	//===end configurable parameters

	OSCPortOut portOut; //send
	OSCPortIn portIn; //receive

//========================================================================
	public SerialReaderOSC()
	{
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
	public static void main(String[] args)
	{
		try
		{
			SerialReaderOSC sro=new SerialReaderOSC();
			sro.loadProps();
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

			int baudrate=9600;
			if(args.length>1)
			{
				baudrate=Integer.parseInt(args[1]);
			}

			String portname=args[0];

			sro.connectSerialPort(portname,baudrate);
			sro.addEventListener();

			//check stop condition here
			while(!sro.shutdown_requested)
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

		portIn.addListener("/*", new StatusListener());
		portIn.startListening();

		System.err.println("SerialReaderOSC: OSC server started on local port "+local_port);
		System.err.println("SerialReaderOSC: OSC sending messages to host "+remote_host+":"+target_port);
/*
portInSend.stopListening();
portInSend.close();
*/
	}

//========================================================================
	public boolean loadProps(String configfile_uri)
	{
		super.loadProps(super.propertiesFileUri);

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
						if(props.getProperty("local_osc_port")!=null)
							{local_osc_port=Integer.parseInt(props.getProperty("local_osc_port"));}
						if(props.getProperty("remote_osc_host")!=null)
							{remote_osc_host=props.getProperty("remote_osc_host");}
						if(props.getProperty("remote_osc_port")!=null)
							{remote_osc_port=Integer.parseInt(props.getProperty("remote_osc_port"));}
						//if(props.getProperty("")!=null){=props.getProperty("");}
						return true;
					}
				}
			}//end if file exists
		}//end try
		catch(Exception e)
		{
			System.err.println("SerialReaderOSC: "+e);
		}
		return false;
	}//end loadProps

//inner class
//========================================================================
//========================================================================
class StatusListener implements OSCListener
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
}//end inner class StatusListener
}//end classs SerialReaderOSC
//EOF
