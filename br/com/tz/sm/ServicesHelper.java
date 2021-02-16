package br.com.tz.sm;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ServicesHelper
{
	//private static boolean stopped = false;
	//private static final List<File> ifiles = new ArrayList<File>();
	private static final Timer timer = new Timer();
	private static final List<FileEntry> ifiles = new ArrayList<FileEntry>();
	//private static final List<ServiceEntry> iservices = new ArrayList<ServiceEntry>();
//	private static final Thread itrd2 = new Thread() {
//		@Override
//		public void run()
//		{
//			boolean interrupted = false;
//			
//			while( !interrupted )
//			{
//				while (iservices.size() > 0)
//				{
//					if ( iservices.get(0).timeout <= System.currentTimeMillis() )
//					{
//						Service svc = (iservices.remove(0)).service;
//						Services.startService(svc);
//					}
//					try
//					{
//						Thread.sleep(500);
//					} 
//					catch (InterruptedException e)
//					{
//						interrupted = true;
//						break;
//					}
//				}
//				if ( interrupted ) break;
//				synchronized (iservices)
//				{
//					try
//					{
//						iservices.wait();
//					}
//					catch (InterruptedException e)
//					{
//						break;
//					}
//				}
//			}
//		}
//	};
	private static final Thread itrd = new Thread() {
		@Override
		public void run()
		{
			synchronized (ifiles)
			{
				while ( true )
				{
					while (ifiles.size() > 0)
					{
						System.out.println("Service candidate file " + ifiles.get(0).file + " detected");
						try
						{
							FileEntry fe = ifiles.remove(0); 
							System.out.println(" Inspecting file " + fe.file.getName());
							String mclass = LLLoader.getNainClassFromJar(fe.file);
							System.out.println(" Loading " + fe.file.getName() + "'s main class " + mclass);
							Class<?> clazz = LLLoader.loadClazz( mclass );
							Service svc = (Service) clazz.getConstructor().newInstance();
							System.out.println(" Registering service");
							Services.registerService( svc );
							//if ( fe.timeout > System.currentTimeMillis())
							if ( fe.timeout > 0 )
							{
//								synchronized (iservices)
//								{
//									iservices.add(new ServiceEntry(svc,fe.timeout));
//									iservices.notifyAll();
//								}
								timer.schedule(new TimerTask() {

									@Override
									public void run()
									{
										Services.startService(svc);
										
									}}, (fe.timeout <= 0?1000:fe.timeout));
							}
							else
							{
								Services.startService(svc);
							}
						} 
						catch ( Exception e )
						{
							e.printStackTrace();
						}
					}
//					while (iservices.size() > 0)
//					{
//						//System.out.println("Service candidate file " + ifiles.get(0) + " detected");
//						try
//						{
//							Service svc = iservices.remove(0);
//							Services.registerService( svc );
//							Services.startService( svc );
//						} 
//						catch ( Exception e )
//						{
//							e.printStackTrace();
//						}
//					}
					System.out.println("wating");
					try
					{
						ifiles.wait();
					} 
					catch (InterruptedException e)
					{
						break;
					}
				}
			}
		}
	};
	
	static {
//		itrd2.setDaemon(true);
//		itrd2.start();
		itrd.setDaemon(true);
		itrd.start();
	}

	public static Handler handler = new Handler();
	
	public static void handleNewFile( String file )
	{
		handleNewFile( new File(file) );
	}
	
	public static void handleNewFile( File file )
	{
		synchronized (ifiles)
		{
			ifiles.add( new FileEntry(file, 5) );
			ifiles.notifyAll();
		}
	}
	

	static class FileEntry
	{
		public final File file;
		public final long timeout;
		
		public FileEntry( File file )
		{
			this.file = file;
			timeout = 0;//System.currentTimeMillis();
		}

		public FileEntry( File file, int secs )
		{
			this.file = file;
			timeout = secs;//System.currentTimeMillis() + (secs * 1000);
		}
	}
	
	static class ServiceEntry
	{
		public final Service service;
		public final long timeout;
		
		public ServiceEntry( Service svc )
		{
			service = svc;
			timeout = 0;//System.currentTimeMillis();
		}

		public ServiceEntry( Service svc, int secs )
		{
			service = svc;
			timeout = 0;//System.currentTimeMillis() + (secs * 1000);
		}

		public ServiceEntry( Service svc, long timeout )
		{
			service = svc;
			this.timeout = timeout;
		}
	}
	

	public static class Handler
	{
		protected Handler()
		{
		}
		
		public static final DelayedHandler delayed = new DelayedHandler(5);
		
		public void handleFile( File file )
		{
			synchronized (ifiles)
			{
				ifiles.add( new FileEntry(file) );
				ifiles.notifyAll();
			}			
		}
	}
	
	public static class DelayedHandler
	{
		private int seconds = 1;
		
		protected DelayedHandler()
		{
		}

		protected DelayedHandler( int seconds )
		{
			this.seconds = Math.abs(seconds);
		}
		
		public void handleFile( File file )
		{
			synchronized (ifiles)
			{
				ifiles.add( new FileEntry(file,seconds*1000) );
				ifiles.notifyAll();
			}			
		}
	}
}
