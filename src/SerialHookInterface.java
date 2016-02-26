//serial hooks need to implement this interface in order to be attached to SerialReader
interface SerialHookInterface
{
	//signalled after SerialReader has started and read properties
	public void startup();
	//signalled if SerialReader received terminate/kill signal (ctrl+c)
	public void shutdown();
	//signalled if SerialReader received serial data
	public void serialData(String data);
	//signalled if SerialReader has initialized log file (optional)
	public void logFile(String basePath, String fileName);
	//signalled if SerialReader connected to port
	public void portConnect(String portName, int baudRate);
}
