package br.com.tz.sm;

public class ServiceNotFoundException extends Exception
{
	private static final long serialVersionUID = 1L;
	
	public ServiceNotFoundException()
	{
		super( "No service has been found" );
	}
	
	public ServiceNotFoundException(String s)
	{
		super( s );
	}
}
