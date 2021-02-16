package br.com.tz.sm;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TimerTask;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.Thread;
import java.lang.Thread.State;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

import org.tz.test.Test01;

public class Services
{
	// 'entries' variable is gonna hold all the services instances and their corresponding threads
	private static final List<ServiceEntry> entries = new ArrayList<ServiceEntry>();
	
	/**
	 * add a new service to the services' registry
	 * 
	 * @param svc : a Service instance
	 * @return ServiceEntry
	 */
	public static synchronized ServiceEntry registerService( Service svc )
	{
		//System.out.println( svc.getClass().getName() );
		ServiceEntry se = new ServiceEntry( svc );
		entries.add( se );
		return se;
	}

	/**
	 * stops the specified service and remove it from the services' registry
	 * 
	 * @param svc : a Service instance
	 * @throws InterruptedException
	 */
	public static void unregisterService( Service svc ) throws InterruptedException
	{
		unregisterService( svc.getClass().getName() );
	}
	
	/**
	 * stops the specified service and remove it from the services' registry
	 * 
	 * @param svcName : a simple String
	 * @throws InterruptedException
	 */
	public static synchronized void unregisterService( String svcName ) throws InterruptedException
	{
		for ( int i=entries.size();i>0;i-- ) 
		{
			if ( entries.get(i-1).service.getClass().getName().equals(svcName) )
			{
				System.out.println("Calling " + svcName + "'s stopp method");
				stopService(entries.get(i-1));
				System.out.println("Unregistering service " + svcName);
				entries.remove((i-1));
				break;
			}
		}
		// it's a good time to invoke gc to clean the memory trash up
		System.gc();
	}
	
	/**
	 * returns a list containg all registered services
	 * 
	 * @return List<Service>
	 */
	public static synchronized List<Service> getAllServices()
	{
		return entries.stream().map( e -> e.service ).collect( Collectors.toList() );
	}

	/**
	 * starts a single service
	 * 
	 * @param e ServiceEntry
	 */
	private static synchronized void startService( ServiceEntry e )
	{
		if ( e == null ) return;
		
		if ( !e.thread.isAlive() && (e.thread.getState() == State.NEW || e.thread.getState() == State.RUNNABLE))
		{
			e.thread.start(); 
		}
	}
	
	/**
	 * starts a single service
	 * 
	 * @param svc Service
	 */
	public static synchronized void startService( Service svc )
	{
		startService( getEntry(svc) );
	}
	
	/**
	 * starts all services at once
	 */
	public static void startAllServices()
	{
		for ( ServiceEntry e : entries )
		{
			startService( e );
		}
	}
	
	/**
	 * stops the service that corresponds to the given ServiceEntry
	 *  
	 * @param e ServiceEntry
	 * @throws InterruptedException
	 */
	private static synchronized void stopService( ServiceEntry e ) throws InterruptedException
	{
		// if service is well designed it will stop when the stop method is 
		// called and its thread will terminate as the result of this
		e.service.stop();
		if ( !e.thread.isAlive() ) return;
		// otherwise, let's try forcing the thread termination although it's 
		// not guaranteed it will work since it depends on the implementation
		e.thread.interrupt();
		e.thread.join(1000);
	}
	
	/**
	 * stops the service specified
	 * 
	 * @param svc : a Service instance
	 */
	public static synchronized void stopService( Service svc )
	{
		startService( getEntry(svc) );
	}

	/**
	 * stops all registered services at once
	 */
	public static synchronized void stopAllServices()
	{
		//entries.keySet().stream().forEach( t -> stopService(entries.get(t)) );
		for ( ServiceEntry e : entries )
		{
			try
			{
				stopService( e );
			} 
			catch (InterruptedException e1)
			{
				e1.printStackTrace();
			}
		}
	}
	
//	public static void stopService( Service svc ) throws InterruptedException
//	{
//		Entry e = getEntry(svc);
//		if ( e == null ) return;
//		e.service.stop();
//		if ( !e.thread.isAlive() ) return;
//		e.thread.join(1000);
//		//System.out.println("Service.stop()::" + thread.isAlive());
//		if ( e.thread.isAlive() )
//		{
//			System.out.println("Service.stop()::" + e.thread.getName());
//			e.thread.interrupt();
//			e.thread.join(1000);	
//		}
//	}
	
	/**
	 * Finds and returns a specific service entry by the service object  
	 * @param svc Service
	 * @return ServiceEntry
	 */
	private static ServiceEntry getEntry( final Service svc ) 
	{
		return entries.stream().filter( e -> e.service.equals(svc) ).findFirst().get();
//		for ( Entry e : entries )
//	    {
//	        if (e.service.equals(svc)) return e;
//	    }
//	    return null;
	}

	/**
	 * searches inside the .jar file looking for the main class that implements the interface Service. 
	 * if found, it then returns the instance of such a class without registering it as a service. 
	 * the registration itself is left up to the caller to do or not.
	 *  
	 * @param path : Path, the full path to the file
	 * @return Service
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws ServiceNotFoundException 
	 */
	public static Service[] getServicesFromJarFile(Path path) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ServiceNotFoundException
	{
		return getServicesFromJarFile( path.toFile() );
	}
	
	public static Service[] getServicesFromJarFile(File file) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ServiceNotFoundException
	{
		if ( !(file.exists() && file.isFile()) )
		{
			throw new FileNotFoundException(file.getAbsolutePath());
		}
		List<Service> svcs = new ArrayList<Service>();
		System.out.println("Inspecting file " + file.getName());
		try ( JarFile jfile = new JarFile(file) )
		{
			// gets new URLClassLoader instance for the new class being loaded
			try ( URLClassLoader loader = new URLClassLoader( new URL[] { file.toURI().toURL()  } ) )
			//try ( URLClassLoader loader = new CustomClassLoader( file.toURI().toURL() ) )
			{
				// traverses the .jar file and load all its inner classes 
				Enumeration<JarEntry> e = jfile.entries();
				while (e.hasMoreElements()) 
				{
					JarEntry je = e.nextElement();
					// if entry is a directory or is not .class file resume the lopp
				    if (je.isDirectory() || !je.getName().endsWith(".class")) continue;
				    // gets the class name without .class suffix
				    String className = je.getName().replace(".class", "").replace("/", ".");
				    // if the loading class is other than the main class go for it
				    Class<?> clazz = loader.loadClass( className );
				    // if clazz is a class that implements Service, add it to svcs array
					if ( Service.class.isAssignableFrom(clazz) )
					{
						svcs.add( (Service) clazz.getConstructor().newInstance() );
					}
				}
			}
		}
		// if no service was found, shout it all loud
		if ( svcs.size() == 0 ) throw new ServiceNotFoundException("No service found in '" + file + "'");
		// finally, return the main class instance
		return svcs.toArray(Service[]::new);
	}

	private static List<String> getAllClassesNames( Enumeration<JarEntry> e )
	{
		List<String> classes = new ArrayList<String>();
		while (e.hasMoreElements()) 
		{
			JarEntry je = e.nextElement();
			// if entry is a directory or is not .class file resume the lopp
		    if (je.isDirectory() || !je.getName().endsWith(".class")) continue;
		    // gets the class name without .class suffix
		    classes.add( je.getName().replace(".class", "").replace("/", ".") );
		}
		return classes;
	}
	
	/**
	 * searches inside the .jar file looking for the main class that implements the interface Service. 
	 * if found, it then returns the instance of such a class without registering it as a service. 
	 * the registration itself is left up to the caller to do or not.
	 *  
	 * @param file : File
	 * @return Service
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	public static Service __getServiceFromJarFile(File file) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException
	{
		if ( !(file.exists() && file.isFile()) )
		{
			throw new FileNotFoundException(file.getAbsolutePath());
		}
		System.out.println("Inspecting file " + file.getName());
		try ( JarFile jfile = new JarFile(file) )
		{
			String mclass = jfile.getManifest().getMainAttributes().getValue("Main-Class");
			if ( mclass == null || mclass.isEmpty() )
			{
				throw new ClassNotFoundException(file + " does not have the manifest's Main-Class attribute defined");
			}
			// gets new URLClassLoader instance for the new class being loaded
			try ( URLClassLoader loader = new URLClassLoader( new URL[] { file.toURI().toURL()  } ) )
			{
				System.out.println("Loading " + file.getName() + "'s main class " + mclass);
				Class<?> clazz = loader.loadClass( mclass );
				if ( !Service.class.isAssignableFrom(clazz) )
				{
					throw new ClassCastException( "Class '" + clazz.getCanonicalName() + "' does not " + (Service.class.isInterface()?"implement":"extend") + " '" + Service.class.getCanonicalName() + "'");
				}
				// traverses the .jar file and load all its inner classes 
				Enumeration<JarEntry> e = jfile.entries();
				while (e.hasMoreElements()) 
				{
					JarEntry je = e.nextElement();
					// if entry is a directory or is not .class file resume the lopp
				    if (je.isDirectory() || !je.getName().endsWith(".class")) continue;
				    // gets the class name without .class suffix
				    String className = je.getName().replace(".class", "").replace("/", ".");
				    // if the loading class is other than the main class go for it
				    if ( ! mclass.equals(className) ) loader.loadClass( className );
				}
				// finally, return the main class instance
				return (Service) clazz.getConstructor().newInstance();
			}
		}
	}
	
	public static void main(String[] args)
	{
		Services.registerService( new Test01() );
		Services.startAllServices();
		try
		{
			Thread.sleep(10000);
		} 
		catch (InterruptedException e)
		{
		}
		Services.startAllServices();
		Services.stopAllServices();
		try
		{
			Thread.sleep(3000);
		} 
		catch (InterruptedException e)
		{
		}
	}
	
	/**
	 * the construct to hold the service object and its related thread instance
	 * 
	 * @author blau
	 */
	static class ServiceEntry
	{
		protected final Thread thread;
		protected final Service service;
		
		protected ServiceEntry( Service svc )
		{
			thread = new Thread( svc, svc.getClass().getName() );
			service = svc; 
		}
		
		public boolean isStarted()
		{
			return thread.isAlive();
		}

		/**
		 * start the service right away
		 */
		public void startService()
		{
			if ( isStarted() ) return;
			this.thread.start();
		}

		/**
		 * starts the service with a delay, in seconds, up to 1 hour (3,600 seconds). 
		 * 
		 * @param seconds : int, ranges from 1 to 3,600
		 */
		public void startServiceLater( int seconds ) throws IllegalArgumentException
		{
			if ( isStarted() ) return;
			if ( seconds < 1 || seconds > 3_600 ) 
			{
				throw new IllegalArgumentException("the value ranges from 1 to 3,600");
			}
//			seconds = Math.abs(seconds);
//			if ( seconds > 3600 ) seconds = 3600;
			Environment.timer.schedule( createTimerTask( this.thread ), seconds*1_000);
		}
		
		private static TimerTask createTimerTask( final Thread thread )
		{
			return new TimerTask() 
			{
				@Override
				public void run()
				{
					try
					{
						thread.start();
					}
					catch( Exception ex )
					{
						ex.printStackTrace();
					}
				}
			};
		}
	}

	//public static final ServiceHelper helper = new ServiceHelper();
	
	static class ServiceHelper
	{
		protected ServiceHelper()
		{
		}
		
		/**
		 * Inspects the .jar file looking for a class that implements the Service interface
		 * @param path java.nio.file.Path, full path to the jar file
		 * @return Service
		 * @throws ClassNotFoundException
		 * @throws IOException
		 * @throws InstantiationException
		 * @throws IllegalAccessException
		 * @throws IllegalArgumentException
		 * @throws InvocationTargetException
		 * @throws NoSuchMethodException
		 * @throws SecurityException
		 */
		public static Service handleFile( Path path ) throws ClassNotFoundException, IOException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException
		{
			return handleFile( path.toFile() );
		}
		
		/**
		 * Inspects the .jar file looking for a class that implements the Service interface
		 * @param jarFile java.io.File, full path to the jar file
		 * @return Service
		 * @throws ClassNotFoundException
		 * @throws IOException
		 * @throws InstantiationException
		 * @throws IllegalAccessException
		 * @throws IllegalArgumentException
		 * @throws InvocationTargetException
		 * @throws NoSuchMethodException
		 * @throws SecurityException
		 */
		public static Service handleFile( File jarFile ) throws ClassNotFoundException, IOException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException
		{
			Class<?> clazz = JarClassLoader.loadJar(jarFile);
			if ( !Service.class.isAssignableFrom(clazz) ) 
			{
				throw new ClassCastException(clazz.getName() + " cannot be cast to " + Service.class.getCanonicalName() );
			}
			System.out.println(clazz.getName() + (Service.class.isInterface()?" implements ":" extends ") + Service.class.getCanonicalName());
			return Service.class.cast(clazz.getConstructor().newInstance());
		}
		
		/*
		public static Service _handleFile( File file )
		{
			try
			{
				System.out.println("Inspecting file " + file.getName());
				String mclass = LLLoader.getNainClassFromJar(file);
				System.out.println("Loading " + file.getName() + "'s main class " + mclass);
				Class<?> clazz = LLLoader.loadClazz( mclass );
				return (Service) clazz.getConstructor().newInstance();
				
			} 
			catch ( Exception e )
			{
				e.printStackTrace();
			}
			return null;
		}
		*/
	}
}
