//rough method to set system time from GNSS
//read first NEMA RMC sentence, read time, delay up to next full second (seconds_to_next_full_second), execute command

//========================================================================
//========================================================================
class SerialHookGNSSTime implements SerialHookInterface
{
	private NMEA n;
	private GPSPosition pos;

	//this file is loaded if found in current directory
	private String propertiesFileUri="SerialHookGNSSTime.properties";

	//===configurable parameters (here: default values)
	// /C time		/C date (omitted)
	public String set_system_time_command="cmd /C nircmdc.exe elevate cmd /C time %"; //HH:MM:SS
	public float seconds_to_next_full_second=0.9f;
	//===end configurable parameters

//========================================================================
	public SerialHookGNSSTime()
	{
		n=new NMEA();
		loadProps();
		DTime.setTimeZoneUTC();
	}

//callback from SerialReader
//========================================================================
	public void startup()
	{
//		System.err.println("hook: prog startup");
	}

//callback from SerialReader
//========================================================================
	public void shutdown()
	{
//		System.err.println("hook: prog shutdown");
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
				System.out.print("GNSS TIME "+pos.time);
				if(pos.time % 1.0f == 0)
				{
					System.out.println(" FOUND .0 TIME");
					//String strDateToSet="02-03-14";
					//String strTimeToSet="16:17:18";
					try
					{
						//System.err.println(""+pos.time+" "+(pos.time+1.0f));

						///to set next second, respecting offset
						String strTimeToSet=
							DTime.timeCFromMillis(
								DTime.millisFrom_HHMMSSpSSS(
									DTime.formatTimeLeadingZeros(pos.time+1.0f)
								)
							);

						System.err.print("NEXT TICK AT "+strTimeToSet);

						Thread.sleep((int)(seconds_to_next_full_second*1000));
						System.err.println(" .");

						Runtime.getRuntime().exec(set_system_time_command.replaceAll("%",strTimeToSet)); // hh:mm:ss

						//drop other lines until next call
						///break;
						System.exit(0);
					}
					catch(Exception e)
					{
						System.err.println("SerialHookGNSSTime: "+e);
					}
				}//end if(pos.time % 1.0f == 0)
				else
				{
					System.out.println("");
				}
			}//end if RMC
		}//end for every line
	}//end serialData()

//callback from SerialReader
//========================================================================
	public void logFile(String basePath, String fileName)
	{
	}

//callback from SerialReader
//========================================================================
	public void portConnect(String portName, int baudRate)
	{
//		System.err.println("hook: port connected");
	}

//========================================================================
	public void loadProps()
	{
		if(!loadProps(propertiesFileUri))
		{
			System.err.println("SerialHookGNSSTime: /!\\ Could not load properties: "+propertiesFileUri);
		}
	}

//========================================================================
	public boolean loadProps(String configfile_uri)
	{
		propertiesFileUri=configfile_uri;
		return LProps.load(propertiesFileUri,this);
	}

}//end class SerialHookGNSSTime
//EOF
