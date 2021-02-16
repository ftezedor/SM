package br.com.tz.sm;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
//import org.tz.util.function.CheckedFunction;

public class Manager
{
	public static void main(String[] args)
	{
		if (System.getProperty("log4j.configurationFile") != null)
		{
			System.setOut( new br.com.tz.logging.log4j.OutLogger( System.out ) );
			System.setErr( new br.com.tz.logging.log4j.ErrLogger( System.err ) );
		}
		
		System.out.println("Initiating Services Manager");

		try
		{
			setShutdownHook();
			loadServices();	
		}
		catch( Exception ex )
		{
			ex.printStackTrace();
		}

		System.out.println("Services Manager's job is done");
	}
	
	private static void loadServices()
	{
//		String servicesPath = orElse(System.getProperty("sm.servicesPath"),"services");
//		File dir = new File(servicesPath);
//		
//		if ( !(dir.exists() && dir.isDirectory()) )
//		{
//			(new IOException("Directory '" + servicesPath + "' not found")).printStackTrace();
//			return;
//		}

		if ( Environment.servicesPath == null )
		{
			(new IOException("Services directory not found")).printStackTrace();
			return;
		}
		
		if ( Environment.incomingPath == null )
		{
			(new java.io.FileNotFoundException("Incoming services directory not found")).printStackTrace();
			System.err.println("Watcher service is disabled");
		}
		else
		{
			//Services.addService( new org.tz.sm.test.Watcher(), "Watcher" );
			Services.registerService( new br.com.tz.sm.Watcher() ).startService();
		}
		
		System.out.println("Starting services lookup");
 		
		File[] files = Environment.servicesPath.toFile().listFiles();
		files = Arrays.stream(files).filter( f -> f.getName().endsWith(".jar") ).toArray(File[]::new);
		
		for ( File file : files )
		{
			try
			{
				for ( Service svc : Services.getServicesFromJarFile(file) )
				{
					System.out.println( "Registering the found service '" + svc.getClass().getName() + "'" );
					Services.registerService(svc).startServiceLater(7);
				}
				//Services.loadServiceFromJarFile(file).startServiceLater(7);
				System.out.println( "Done with file " + file.getName() );
			} 
			catch ( Exception e )
			{
				e.printStackTrace();
			}
		}
			
		System.out.println("Services lookup is done");
		
		// a lot has been done so, let's take a moment to invoke gc and let it perform the memory clean up
		System.gc();
	}

	private static void setShutdownHook()
	{
		Runtime.getRuntime().addShutdownHook(new Thread() 
		{
			@Override
			public void run() 
			{
				Thread.currentThread().setName("shutdown");
				System.out.println("Shutdown process has been triggered");
				
				// stop all services
				System.out.println(" stopping all services");
				try
				{
					Services.stopAllServices();
				}
				catch( Exception ex )
				{
					ex.printStackTrace();
				}
				
				final List<String> exceptions = Arrays.asList( "DestroyJavaVM", Thread.currentThread().getName() );

//				Thread.getAllStackTraces().keySet().stream()
//					.filter( t -> !t.isDaemon() && t.isAlive() && !exceptions.contains(t.getName()) )
//					.forEach( t -> System.out.println(t.getName()));
				
				// get the running non-daemon threads
				List<Thread> trds = Thread.getAllStackTraces().keySet().stream()
						.filter( t -> !t.isDaemon() && t.isAlive() && !exceptions.contains(t.getName()) )
						.collect(Collectors.toList());

				trds.stream().forEach( t -> System.out.println( t.getName() ) );
				//trds.stream().forEach( t -> System.out.println(t.getContextClassLoader().getParent()) );

				Thread mainThread = null;

				// wait 5 secs for each thread to stop by their own
				for ( Thread t : trds )
				{
					// save the main thread to be handled next
					if ( t.getName().equals("main") )
					{
						mainThread = t;
					}
					else
					{
						System.out.println( "trying to stop thread " + t.getName() );
						try
						{
							t.join(5000);
						}
						catch( Exception ex )
						{
							ex.printStackTrace();							
						}
					}
				}

				// now let's wait 5 secs for the main thread to end
				if ( mainThread != null && mainThread.isAlive() )
				{
					System.out.println( "waiting for main thread to stop" );
					try 
					{
						mainThread.join(5000);
					}
					catch( Exception ex )
					{
						ex.printStackTrace();		
					}
				}

				// if main thread didn't end yet, call its interrupt method and wait 5 secs for it to end
				if ( mainThread != null && mainThread.isAlive() )
				{
					System.out.println( "waiting for main thread to stop (last chance)" );
					try
					{
						mainThread.interrupt();
						mainThread.join(5000);
					}
					catch( Exception ex )
					{
						ex.printStackTrace();		
					}
				}

				// get the remaining running non-daemon threads
				trds = Thread.getAllStackTraces().keySet().stream()
						.filter( t -> !t.isDaemon() && t.isAlive() && !exceptions.contains(t.getName()) )
						.collect(Collectors.toList());
				
				// call their interrupt method as the last resort to get them shut down
				for ( Thread t : trds )
				{
					System.out.println( "trying to stop thread " + t.getName() );
					try
					{
						t.interrupt();
						t.join(5000);
					}
					catch( Exception ex )
					{
						ex.printStackTrace();							
					}
				}
			}
		}); 
	} 
}
