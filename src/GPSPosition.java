//=============================================================================
public class GPSPosition
{
	//millis since 1970-01-01_00:00:00.000
	public long millis_utc = 0;
	//from local system time
	public long millis_utc_sys = 0;
	//yyyymmdd
	public String date="19700101";
	//HHMMSS.SSS
	public float time = 0f;
	public double lon = 0.0d;
	public double lat = 0.0d;
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
	public String last_sentence_type ="";
	public boolean fixed = false;

	public String csv_header="millis_utc;millis_utc_sys;date;time;lon;lat;quality;dir;alt;vel;lat_err;lon_err;alt_err;dgps_age;mode;sat;fix;PDOP;HDOP;VDOP;";

//========================================================================
	public GPSPosition(){}

//create new clone
//========================================================================
	public GPSPosition(GPSPosition pos)
	{
		this.millis_utc=        pos.millis_utc;
		this.millis_utc_sys=    pos.millis_utc_sys;
		this.date=              pos.date;
		this.time=              pos.time;
		this.lon=               pos.lon;
		this.lat=               pos.lat;
		this.quality=           pos.quality;
		this.direction=         pos.direction;
		this.altitude=          pos.altitude;
		this.velocity=          pos.velocity;
		this.lat_err=           pos.lat_err;
		this.lon_err=           pos.lon_err;
		this.alt_err=           pos.alt_err;
		this.dgps_age=          pos.dgps_age;
		this.mode=              pos.mode;
		this.sat_in_use=        pos.sat_in_use;
		this.fix_type=          pos.fix_type;
		this.PDOP=              pos.PDOP;
		this.HDOP=              pos.HDOP;
		this.VDOP=              pos.VDOP;

		this.local_tz=          pos.local_tz;
		this.last_sentence_type=pos.last_sentence_type;
		this.fixed=		pos.fixed;
}

//=============================================================================	
	public void updatefix()
	{
		fixed = quality > 0;
	}

//=============================================================================		
	public String toString()
	{
		return String.format("%d;%d;%s;%s;%.9f;%.9f;%d;%f;%f;%f;%f;%f;%f;%f;%s;%d;%d;%f;%f;%f"
			,millis_utc,millis_utc_sys,date,time	///DTime.formatTimeLeadingZeros(time)
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
