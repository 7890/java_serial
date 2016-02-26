class SerialHookTest implements SerialHookInterface
{
	public SerialHookTest(){}

	public void startup()
	{
		System.out.println("hook: prog startup");
	}

	public void shutdown()
	{
		System.out.println("hook: prog shutdown");
	}

	public void serialData(String data)
	{
		System.out.println("hook: serial data");
	}

	public void logFile(String basePath, String fileName)
	{
		System.out.println("hook: log file");
	}

	public void portConnect(String portName, int baudRate)
	{
		System.out.println("hook: port connected");
	}
}
