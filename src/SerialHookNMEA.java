import java.util.*;
import java.net.*;
import java.io.*;
import com.illposed.osc.*;

//========================================================================
//========================================================================
class SerialHookNMEA implements SerialHookInterface
{
	private PrintWriter writer_csv=null;
	private NMEA n;
	private GPSPosition pos;

	//this file is loaded if found in current directory
	private String propertiesFileUri="SerialHookNMEA.properties";

	//===configurable parameters (here: default values)
	public int	local_osc_port			=3050;
	public String	remote_osc_host			="127.0.0.1";
	public int	remote_osc_port			=3051;
        //===end configurable parameters

	private OSCPortOut portOut; //send

//========================================================================
	public SerialHookNMEA()
	{
		n=new NMEA();
		loadProps();
	}

//callback from SerialReader
//========================================================================
	public void startup()
	{
		System.err.println("hook: prog startup");
		try
		{
			init_osc_server(local_osc_port,remote_osc_host,remote_osc_port);
		}
		catch(Exception e)
		{
			System.err.println("SerialHookNMEA: "+e);
		}
	}

//callback from SerialReader
//========================================================================
	public void shutdown()
	{
		System.err.println("hook: prog shutdown");
		if(writer_csv!=null)
		{
			writer_csv.close();
		}
	}

//callback from SerialReader
//========================================================================
	public void serialData(String data)
	{
//		System.err.println("hook: serial data");

		String[] lines = data.split("\n");
		for(int i=0;i<lines.length;i++)
		{
//			System.err.println(lines[i]);
			pos=n.parseLine(lines[i]);
			if(pos!=null&& pos.last_sentence_type.equals("RMC"))
			{
				//if....

				//System.out.println(pos);
				if(writer_csv!=null)
				{
					writer_csv.println(pos);
					writer_csv.flush();
				}

				try
				{
					portOut.send(new OSCGPSPosition(pos).getValuesAsOSCMessage());
				}
				catch(Exception e)
				{
					System.err.println("SerialHookNMEA: "+e);
				}
			}
		}
	}//end serialData()

//callback from SerialReader
//========================================================================
	public void logFile(String basePath, String fileName)
	{
		String file_uri=basePath+File.separator+fileName+".csv";

		System.err.println("SerialHookNMEA: Logging to file "+file_uri);

		try
		{
			if(writer_csv!=null)
			{
				writer_csv.close();
			}
			writer_csv=new PrintWriter(file_uri, "UTF-8");
			writer_csv.println(new GPSPosition().getCSVHeader());
		}
		catch(Exception e)
		{
			System.err.println("SerialHookNMEA: "+e);
		}
	}

//callback from SerialReader
//========================================================================
	public void portConnect(String portName, int baudRate)
	{
		System.err.println("hook: port connected");
	}

//callback from SerialReader
//========================================================================
	public void init_osc_server(int local_port, String remote_host, int target_port) throws Exception
	{
		DatagramSocket ds=new DatagramSocket(local_port);
		portOut=new OSCPortOut(InetAddress.getByName(remote_host), target_port, ds);

		System.err.println("SerialHookNMEA: OSC server started on local port "+local_port);
		System.err.println("SerialHookNMEA: OSC sending messages to host "+remote_host+":"+target_port);
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
			System.err.println("SerialHookNMEA: /!\\ Could not load properties: "+propertiesFileUri);
		}
	}

//========================================================================
	public boolean loadProps(String configfile_uri)
	{
		propertiesFileUri=configfile_uri;
		return LProps.load(propertiesFileUri,this);
	}

}//end class SerialHookNMEA
//EOF
