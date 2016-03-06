//http://docs.oracle.com/javase/tutorial/reflect/member/fieldTypes.html
//tb/160306
import java.util.Properties;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
//========================================================================
//========================================================================
class LProps
{
	public LProps(){}
//========================================================================
	public static boolean load(String configfile_uri, Object configurable_object)
	{
		Properties props=new Properties();
		InputStream is=null;
		try 
		{
			File f=new File(configfile_uri);
			if(!f.exists() || !f.canRead())
			{
				return false;
			}
			is=new FileInputStream(f);
			if(is==null)
			{
				return false;
			}
			props.load(is);
			if(props==null)
			{
				return false;
			}
			Class<?> c = configurable_object.getClass();
			Field[] fields = c.getFields();
			for(int i=0; i<fields.length;i++)
			{
//				System.err.println("field "+i+" : "+fields[i]);
				Class ctype=fields[i].getType();
				String fname=fields[i].getName();
				if(props.getProperty(fname)!=null)
				{
//					System.err.println("found matching member variable property in file");
					if(ctype==int.class || ctype==Integer.class)
					{
//						System.err.println("found int");
						try{fields[i].setInt(configurable_object, Integer.parseInt(props.getProperty(fname)));}
						catch(Exception e){System.err.println(""+e);}
					}
					else if(ctype==float.class || ctype==Float.class)
					{
//						System.err.println("found float");
						try{fields[i].setFloat(configurable_object, Float.parseFloat(props.getProperty(fname)));}
						catch(Exception e){System.err.println(""+e);}
					}
					else if(ctype==String.class)
					{
//						System.err.println("found string");
						try{fields[i].set(configurable_object, props.getProperty(fname));}
						catch(Exception e){System.err.println(""+e);}
					}
					else if(ctype==boolean.class || ctype==boolean.class)
					{
//						System.err.println("found boolean");
						try{fields[i].setBoolean(configurable_object, Boolean.parseBoolean(props.getProperty(fname)));}
						catch(Exception e){System.err.println(""+e);}
					}
					///else if byte,short,long,char,double
				}//end if found property
			}//end for all fields
			return true;
		}//end try
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return false;
	}//end load()
}//end class LProps
