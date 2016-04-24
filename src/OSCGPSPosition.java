import java.util.List;
import com.illposed.osc.OSCMessage;

//=============================================================================
public class OSCGPSPosition extends ConvertibleGPSPosition
{
	public OSCGPSPosition(){}

//========================================================================
	public OSCGPSPosition(GPSPosition pos)
	{
		this.millis_utc=	pos.millis_utc;
		this.millis_utc_sys=	pos.millis_utc_sys;
		this.date=		pos.date;
		this.time=		pos.time;
		this.lon=		pos.lon;
		this.lat=		pos.lat;
		this.quality=		pos.quality;
		this.direction=		pos.direction;
		this.altitude=		pos.altitude;
		this.velocity=		pos.velocity;
		this.lat_err=		pos.lat_err;
		this.lon_err=		pos.lon_err;
		this.alt_err=		pos.alt_err;
		this.dgps_age=		pos.dgps_age;
		this.mode=		pos.mode;
		this.sat_in_use=	pos.sat_in_use;
		this.fix_type=		pos.fix_type;
		this.PDOP=		pos.PDOP;
		this.HDOP=		pos.HDOP;
		this.VDOP=		pos.VDOP;
	}

//========================================================================
	public OSCMessage getValuesAsOSCMessage()
	{
		return new OSCMessage("/pos",getValuesAsVector());
	}

//========================================================================
	public void setValuesFromOSCMessage(OSCMessage msg)
	{
		String path=msg.getAddress();
		java.util.List<Object> args=msg.getArguments();
		int argsSize=args.size();

		if(path.equals("/pos") && argsSize==TOKEN_COUNT)
		{
			try
			{
				this.millis_utc=	(Long)		args.get(0);
				this.millis_utc_sys=	(Long)		args.get(1);
				this.date=		(String)	args.get(2);
				this.time=		(Float)		args.get(3);
				this.lon=		(Double)	args.get(4);
				this.lat=		(Double)	args.get(5);
				this.quality=		(Integer)	args.get(6);
				this.direction=		(Float)		args.get(7);
				this.altitude=		(Float)		args.get(8);
				this.velocity=		(Float)		args.get(9);
				this.lat_err=		(Float)		args.get(10);
				this.lon_err=		(Float)		args.get(11);
				this.alt_err=		(Float)		args.get(12);
				this.dgps_age=		(Float)		args.get(13);
				this.mode=		(String)	args.get(14);
				this.sat_in_use=	(Integer)	args.get(15);
				this.fix_type=		(Integer)	args.get(16);
				this.PDOP=		(Float)		args.get(17);
				this.HDOP=		(Float)		args.get(18);
				this.VDOP=		(Float)		args.get(19);
			}
                        catch(Exception e){e.printStackTrace();}
		}//end msg path /pos
		else
		{
			System.err.println("received message not supported: "+path);
		}
	}//end setValuesFromOSCMessage
}//end class OSCGPSPosition
//EOF
