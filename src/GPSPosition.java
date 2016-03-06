//=============================================================================
public class GPSPosition
{
	//millis since 1970-01-01_00:00:00.000
	public long millis_utc = 0;
	//from local system time
	public long millis_utc_sys = 0;
	//yyyymmdd
	public String date="";
	//HHMMSS.SSS
	public float time = -1.0f;
	public float lon = 0.0f;
	public float lat = 0.0f;
	public int quality = 0;
	public float direction = -1.0f;
	public float altitude = -1.0f;
	public float velocity = -1.0f;
	public float lat_err = -1.0f;
	public float lon_err = -1.0f;
	public float alt_err = -1.0f;
	public float dgps_age = -1.0f;
	public String mode = "N";
	public int sat_in_use = 0;
	public int fix_type = 0;
	public float PDOP = -1;
	public float HDOP = -1;
	public float VDOP = -1;
	public int local_tz = 0;
	///
	public String last_sentence_type ="";
	public boolean fixed = false;

//	String csv_header="date;time;lon;lat;quality;dir;alt;vel;lat_err;lon_err;alt_err;dgps_age;mode;sat;fix;PDOP;HDOP;VDOP;";
	String csv_header="millis_utc;millis_utc_sys;date;time;lon;lat;quality;dir;alt;vel;lat_err;lon_err;alt_err;dgps_age;mode;sat;fix;PDOP;HDOP;VDOP;";

//=============================================================================	
	public void updatefix()
	{
		fixed = quality > 0;
	}

//=============================================================================		
	public String toString()
	{
//		return String.format("%d;%s%s%s;%f;%f;%f;%d;%f;%f;%f;%f;%f;%f;%f;%s;%d;%d;%f;%f;%f"
//			,String.format("%04d",year),String.format("%02d",month),String.format("%02d",day),time
		return String.format("%d;%d;%s;%s;%f;%f;%d;%f;%f;%f;%f;%f;%f;%f;%s;%d;%d;%f;%f;%f"
			,millis_utc,millis_utc_sys,date,String.format("%06.3f",time)
			,lon,lat,quality,direction,altitude,velocity,lat_err,lon_err,alt_err
			,dgps_age,mode,sat_in_use,fix_type,PDOP,HDOP,VDOP);
	}

//=============================================================================
	String getCSVHeader()
	{
		return csv_header;
	}
}//end class GPSPosition
//EOF
