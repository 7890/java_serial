import jssc.*;

public class SerialReader
{
	static SerialPort serialPort;
	static String portname="/dev/ttyUSB0";
	static int baudrate=9600;

	public static void main(String[] args)
	{
		if(args.length<1)
		{
			System.err.println("Need port argument");
			System.err.println("Syntax: <serial portname> (baudrate)");
			System.err.println("Example: /dev/ttyACM0 115200");
			System.err.println("Default baudrate: "+baudrate);

			System.exit(1);
		}

		try
		{
			if(args.length>1)
			{
				baudrate=Integer.parseInt(args[1]);	
			}

			portname=args[0];
			serialPort=new SerialPort(portname); 

			serialPort.openPort();
			serialPort.setParams(baudrate, 8, 1, 0);
			serialPort.setEventsMask(SerialPort.MASK_RXCHAR);
			serialPort.addEventListener(new SerialPortReader());
			//serialPort.writeString("HelloWorld");
		}
		catch (SerialPortException e)
		{
			System.out.println(e);
			System.exit(1);
		}
	}//end main()

	//inner class
	static class SerialPortReader implements SerialPortEventListener
	{
		public void serialEvent(SerialPortEvent event)
		{
			//data available in buffer
			if(event.isRXCHAR())
			{
				try 
				{
					//read all available bytes to string
					String data= serialPort.readString(event.getEventValue());
					System.out.print(data);
				}
				catch(SerialPortException e)
				{
					System.err.println(e);
				}
			}
			//If the CTS line status has changed, then the method event.getEventValue() returns 1 if the line is ON and 0 if it is OFF.
			else if(event.isCTS())
			{
				if(event.getEventValue()==1)
				{
					System.err.println("CTS - ON");
				}
				else
				{
					System.err.println("CTS - OFF");
				}
			}
			else if(event.isDSR())
			{
				if(event.getEventValue()==1)
				{
					System.err.println("DSR - ON");
				}
				else
				{
					System.err.println("DSR - OFF");
				}
			}
		}//end serialEvent
	}//endclass SerialPortReader
}//end class SerialReader
