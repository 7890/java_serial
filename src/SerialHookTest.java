class SerialHookTest implements SerialHookInterface
{
	public SerialHookTest(){}

	public void startup()
	{
		System.err.println("hook: prog startup");
	}

	public void shutdown()
	{
		System.err.println("hook: prog shutdown");
	}

	public void serialData(String data)
	{
		System.err.println("hook: serial data");
	}

	public void logFile(String basePath, String fileName)
	{
		System.err.println("hook: log file");
	}

	public void portConnect(String portName, int baudRate)
	{
		System.err.println("hook: port connected");
	}
}
