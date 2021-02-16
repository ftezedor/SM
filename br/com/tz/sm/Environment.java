package br.com.tz.sm;

import java.io.File;
import java.nio.file.Path;
import java.util.Timer;
import java.util.function.Consumer;

public class Environment
{
	public final static Timer timer = new Timer("System timer", true);
	public final static Path servicesPath = getPath(orElse(System.getProperty("sm.servicesPath"),"services"));
	public final static Path incomingPath = getPath(orElse(System.getProperty("sm.incomingPath"),"incoming"));
	
	public static Path getPath( String path )
	{
		File dir = new File( path );
		if ( dir.exists() && dir.isDirectory() )
		{
			return dir.toPath();	
		}
		return null;
	}
	
	public static <T> T orElse( T t1, T t2 )
	{
		if ( t1 == null ) return t2;
		//if ( t1.getClass() == String.class && t1.equals("") ) return t2;
		if ( t1.getClass() == String.class && t1.toString().isEmpty() ) return t2;
		return t1;
	}
}
