package test;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TooManyListenersException;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

/** 
* @ClassName: SerialComm 
* @Description: TODO RXTX串口操作类
* @author chinaliteng@gmail.com
* @date 2014-6-4 下午7:43:32 
*  
*/
public class SerialComm implements SerialPortEventListener, Runnable
{
	public final static String PORT_OWER = "UPS";

	private boolean isOpen;

	private boolean isStart;

	private boolean isSave;

	private boolean isPrint;

	private Thread readThread;

	private String portName;

	private String portAddress;

	private CommPortIdentifier portId;

	private SerialPort serialPort;

	private DataInputStream inputStream;

	private OutputStream outputStream;

	private SimpleDateFormat formatter;

	// prase data with process
	private String dataProtocol;

	private Object readWriteLock = new Object();


	public SerialComm() {
		isOpen = false;
		isStart = false;
		isSave = true;
		isPrint = false;
		formatter = new SimpleDateFormat("[yyyy-MM-dd hh:mm:ss,SSS]");

		portName = "COM1";
		portAddress = "LOCAL";
		dataProtocol = "Gooseli";
	}

	public void init(String port, String protocol) throws Exception
	{
		portName = port;
		portAddress = portName;
		dataProtocol = protocol;

		init();
	}

	public void init(String port, String address, String protocol) throws Exception
	{
		portName = port;
		portAddress = address;
		dataProtocol = protocol;

		init();
	}

	public void init() throws IOException, Exception, Exception
	{
		if (isOpen)
		{
			close();
		}

		try
		{
			//传送串口名创建CommPortIdentifier对象服务。
			portId = CommPortIdentifier.getPortIdentifier(portName);

			//使用portId对象服务打开串口，并获得串口对象
			serialPort = (SerialPort) portId.open(PORT_OWER, 2000);

			//通过串口对象获得读串口流对象
			inputStream = new DataInputStream(serialPort.getInputStream());

			//通过串口对象获得写串口流对象
			outputStream = serialPort.getOutputStream();

			isOpen = true;
		} catch (NoSuchPortException ex)
		{
			throw new Exception(ex.toString());
		} catch (PortInUseException ex)
		{
			throw new Exception(ex.toString());
		}
	}

	public void start() throws Exception
	{
		if (!isOpen)
		{
			throw new Exception(portName + " has not been opened.");
		}

		try
		{
			//创建对象线程
			readThread = new Thread(this);
			readThread.start();

			//设置串口数据时间有效
			serialPort.notifyOnDataAvailable(true);

			//增加监听
			serialPort.addEventListener(this);

			isStart = true;

		} catch (TooManyListenersException ex)
		{
			throw new Exception(ex.toString());
		}
	}

	public void run()
	{
		String at = "at^hcmgr=1\r";

		String strTemp = at + (char) Integer.parseInt("1a", 16) + "z";

		writeComm(strTemp);
		isPrint = true;
	}

	public void stop()
	{
		if (isStart)
		{
			serialPort.notifyOnDataAvailable(false);
			serialPort.removeEventListener();

			isStart = false;
		}
	}

	public void close()
	{
		stop();

		if (isOpen)
		{
			try
			{
				inputStream.close();
				outputStream.close();
				serialPort.close();

				isOpen = false;
			} catch (IOException ex)
			{
			}
		}
	}

	//如果串口有数据上报则主动调用此方法
	public void serialEvent(SerialPortEvent event)
	{
		switch (event.getEventType())
		{
		case SerialPortEvent.BI:
		case SerialPortEvent.OE:
		case SerialPortEvent.FE:
		case SerialPortEvent.PE:
		case SerialPortEvent.CD:
		case SerialPortEvent.CTS:
		case SerialPortEvent.DSR:
		case SerialPortEvent.RI:
		case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
			break;
		case SerialPortEvent.DATA_AVAILABLE:
			readComm();
			break;
		default:
			break;
		}
	}

	public void readComm()
	{
		StringBuffer readBuffer = new StringBuffer();
		String scannedInput = "";
		Date currentTime = null;
		String TimeStamp = "";
		int c;
		char a;
		try
		{
			InputStreamReader fis = new InputStreamReader(inputStream, "utf-8");
			while ((c = fis.read()) != -1)
			{
				readBuffer.append((char) c);
			}
			scannedInput = readBuffer.toString().trim();
			currentTime = new Date();

			TimeStamp = formatter.format(currentTime);

		} catch (IOException ex)
		{
			ex.printStackTrace();

		} catch (Exception ex)
		{

			ex.printStackTrace();
		}

	}

	public void writeComm(String outString)
	{
		synchronized (readWriteLock)
		{
			try
			{
				outputStream.write(outString.getBytes());
			} catch (IOException ex)
			{

			}
		}
	}

	public static void main(String[] args)
	{
		SerialComm serialcomm = new SerialComm();

		try
		{
			serialcomm.init("COM2", "Air");// windows下测试端口
			
			// serialcomm.init("/dev/ttyUSB0", "Air");//linux下测试端口
			serialcomm.start();
			serialcomm.writeComm("111");
		} catch (Exception ex)
		{
		}
	}

}