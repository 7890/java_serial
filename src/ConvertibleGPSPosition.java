import java.util.Vector;

//=============================================================================
public class ConvertibleGPSPosition extends GPSPosition
{
	String CSV_DELIMITER=";";
	int TOKEN_COUNT=20;
	
//=============================================================================	
	public void setValuesFromCsvLine(String csvLine)
	{
		try
		{
			String[] tokens = csvLine.split(CSV_DELIMITER);
			if(tokens.length!=TOKEN_COUNT)
			{
				System.err.println("/!\\ token count does not match");
				return;
			}

			setValuesFromStringArray(tokens);
		}
		catch(Exception e){e.printStackTrace();}
	}

//========================================================================
	void print_parse_error(String s)
	{
		System.err.println("parse error: "+s);
	}

//========================================================================
	public void setValuesFromStringArray(String[] tokens)
	{
		try{this.millis_utc = Long.parseLong(tokens[0]);}catch(Exception e){print_parse_error("millis_utc");}
		try{this.millis_utc_sys = Long.parseLong(tokens[1]);}catch(Exception e){print_parse_error("millis_utc_sys");}
		try{this.date = 			tokens[2];}catch(Exception e){print_parse_error("date");}
		try{this.time = Float.parseFloat	(tokens[3]);}catch(Exception e){print_parse_error("time");}
		try{this.lon =  Double.parseDouble	(tokens[4]);}catch(Exception e){print_parse_error("lon");}
		try{this.lat =  Double.parseDouble	(tokens[5]);}catch(Exception e){print_parse_error("lat");}
		try{this.quality = Integer.parseInt     (tokens[6]);}catch(Exception e){print_parse_error("quality");}
		try{this.direction = Float.parseFloat   (tokens[7]);}catch(Exception e){print_parse_error("direction");}
		try{this.altitude = Float.parseFloat    (tokens[8]);}catch(Exception e){print_parse_error("altitude");}
		try{this.velocity = Float.parseFloat    (tokens[9]);}catch(Exception e){print_parse_error("velocity");}
		try{this.lat_err = Float.parseFloat     (tokens[10]);}catch(Exception e){print_parse_error("lat_err");}
		try{this.lon_err = Float.parseFloat     (tokens[11]);}catch(Exception e){print_parse_error("lon_err");}
		try{this.alt_err = Float.parseFloat     (tokens[12]);}catch(Exception e){print_parse_error("alt_err");}
		try{this.dgps_age = Float.parseFloat    (tokens[13]);}catch(Exception e){print_parse_error("dgps_age");}
		try{this.mode =			 	tokens[14].substring(0,1);}catch(Exception e){print_parse_error("mode");}
		try{this.sat_in_use = Integer.parseInt  (tokens[15]);}catch(Exception e){print_parse_error("sat_in_use");}
		try{this.fix_type = Integer.parseInt    (tokens[16]);}catch(Exception e){print_parse_error("fix_type");}
		try{this.PDOP = Float.parseFloat	(tokens[17]);}catch(Exception e){print_parse_error("PDOP");}
		try{this.HDOP = Float.parseFloat	(tokens[18]);}catch(Exception e){print_parse_error("HDOP");}
		try{this.VDOP = Float.parseFloat	(tokens[19]);}catch(Exception e){print_parse_error("VDOP");}
	}//end setValuesFromStringArray()

//========================================================================
	public Vector getValuesAsVector()
	{
		Vector v=new Vector();
		v.add(this.millis_utc);
		v.add(this.millis_utc_sys);
		v.add(this.date);
		v.add(this.time);
		v.add(this.lon);
		v.add(this.lat);
		v.add(this.quality);
		v.add(this.direction);
		v.add(this.altitude);
		v.add(this.velocity);
		v.add(this.lat_err);
		v.add(this.lon_err);
		v.add(this.alt_err);
		v.add(this.dgps_age);
		v.add(this.mode);
		v.add(this.sat_in_use);
		v.add(this.fix_type);
		v.add(this.PDOP);
		v.add(this.HDOP);
		v.add(this.VDOP);
		return v;
	}//end getValuesAsVector()
}//end class ConvertibleGPSPosition
//EOF
