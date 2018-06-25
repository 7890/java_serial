//rough method to set system time from GNSS
//read first NEMA RMC sentence, read time, delay up to next full second (seconds_to_next_full_second), execute command

//========================================================================
//========================================================================
class SerialHookGNSSTime implements SerialHookInterface
{
	private NMEA n;
	private GPSPosition pos;
	private boolean all_done=false;

	//this file is loaded if found in current directory
	private String propertiesFileUri="SerialHookGNSSTime.properties";

/*
	set_system_time_command:
	__date_string__ is a placeholder for date %Y-%m-%d	(1970-01-01)
	__time_string__ is a placeholder for time %H:%M:%S	(00:00:00)
	command tokens are separated with ','
	command tokens will be trimmed (remove whitespace at beginning and end)
	NO USE OF QUOTES in command tokens!
*/

	//===configurable parameters (here: default values)
	// /C time		/C date (omitted)
	public String set_system_time_command="cmd, /C, nircmdc.exe, elevate, cmd, /C, time, __time_string__"; //HH:MM:SS
	public float seconds_to_next_full_second=0.9f;
	public int minimal_sat_count=4;
	public int minimal_quality=1;
	public int seconds_offset_to_utc=0;
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
		if(all_done){return;}

		String[] lines = data.split("\n");
		for(int i=0;i<lines.length;i++)
		{
//			System.err.println(lines[i]);
			pos=n.parseLine(lines[i]);
			if(pos==null || pos.quality<minimal_quality || pos.sat_in_use<minimal_sat_count)
			{
				System.err.println("Received data is not good enough.");
				if(pos!=null)
				{
					System.err.println("last sentence: "+pos.last_sentence_type+" quality: "+pos.quality+" satellites: "+pos.sat_in_use+" time: "+pos.time);
				}
				//break; //dismiss all other lines
				continue;
			}

			if(pos!=null&& pos.last_sentence_type.equals("RMC"))
			{
				System.out.print("GNSS TIME "+pos.time +" quality: "+pos.quality+" satellites: "+pos.sat_in_use+" time: "+pos.time);

				/*
				//test day switch (with positive seconds_offset_to_utc offset >1)
				pos.date="20180101";
				pos.time=235959.0f;
				*/
				/*
				//test day switch (with negative seconds_offset_to_utc offset <-1)
				pos.date="20180102";
				pos.time=1.0f;
				*/

				if(pos.time % 1.0f == 0)
				{
					System.out.println(" FOUND .0 TIME");
					try
					{
						long millisDate=DTime.millisFrom_yyyymmdd(pos.date);
						long millisTime=DTime.millisFrom_HHMMSS(DTime.formatTimeLeadingZeros(pos.time).substring(0,6));
						long millisWithOffsetNextTick=millisDate+millisTime
							+1000 * (seconds_offset_to_utc+1); //+1: one second ahead (next tick)

						String strDate=DTime.dateFromMillis(millisWithOffsetNextTick);
						String strTime=DTime.timeFromMillis(millisWithOffsetNextTick);

						System.err.println(strDate+" "+strTime);

						String cmd=
							set_system_time_command.replaceAll("__date_string__", strDate)
								.replaceAll("__time_string__", strTime);


						System.err.println("command tokens: "+cmd);
						//get tokens
						String[] cmd_parts = cmd.split(",");
						//trim whitespace
						for(int k=0;k<cmd_parts.length;k++)
						{
							cmd_parts[k]=cmd_parts[k].trim();
						}

						System.err.print("NEXT TICK AT "+pos.date+" "+strTime+"  (including offset "+seconds_offset_to_utc+")");

						Thread.sleep((int)(seconds_to_next_full_second*1000));
						System.err.println(" .");

						System.err.println("Setting system time...");

						//new String[] {"sh", "-l", "-c", "./foo"}
						Process p=Runtime.getRuntime().exec(cmd_parts);
						int exitval=p.waitFor();
						if(exitval!=0)
						{
							throw new Exception("setting system time was not successful. exit code of command was "+exitval);
						}
						System.err.println("done.");
						all_done=true;
						System.exit(0);
					}
					catch(Exception e)
					{
						System.err.println("SerialHookGNSSTime: "+e);
						all_done=true;
						System.exit(1);
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
