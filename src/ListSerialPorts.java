import jssc.*;

public class ListSerialPorts
{
	public static void main(String[] args)
	{
		String[] portNames = SerialPortList.getPortNames();
		for(int i = 0; i < portNames.length; i++)
		{
			System.out.println(portNames[i]);
		}
		if(portNames.length<1)
		{
			System.err.println("no ports found.");
			System.err.println("on linux:");
			System.err.println("  ls -l /dev/ #look for tty* devices.");
			System.err.println("check permissions, group membership");
			System.err.println("if a plugged-in device port is still not shown:");
			System.err.println("  sudo chmod 666 /dev/ttyXXX #as a temporary test");
			System.err.println("  sudo chown :$USER /dev/ttyACM0 #alternative");
		}
	}
}
