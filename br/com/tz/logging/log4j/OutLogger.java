package br.com.tz.logging.log4j;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.FileAppender;

public class OutLogger extends java.io.PrintStream
//public class Logger extends java.io.OutputStream
{
	//private final java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	private static Logger LOGGER;
	
	public OutLogger( java.io.PrintStream s )
	{
		super(s);
		LOGGER = LogManager.getLogger( s.getClass() );
	}

    @Override
    public void write(byte[] b) throws IOException 
    {
    	super.write(b);
    	doWrite(new String(b));
    }

    @Override
    public void write(byte[] b, int off, int len) 
    {
    	doWrite(new String(b, off, len));
    }

    @Override
    public void write(int b) 
    {
    	doWrite(String.valueOf((char)b));
    }

    private void doWrite(String str) 
	{
    	for ( String s : str.split("[\\r?\\n]+") )
		{
    		LOGGER.log( Level.INFO, s );
		}
    }
}

