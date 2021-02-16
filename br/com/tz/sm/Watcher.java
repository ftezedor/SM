package br.com.tz.sm;

/*
 to check if the file copy is done
 (1)
 try(FileChannel ch = FileChannel.open(p, StandardOpenOption.WRITE);
    FileLock lock = ch.tryLock()){

    if(lock == null) {
        //no lock, other process is still writing            
    } else {
        //you got a lock, other process is done writing
    }
 } catch (IOException e) {
     //something else went wrong
 }
 (2)
 private boolean isCompletelyWritten(File file) {
    RandomAccessFile stream = null;
    try {
        stream = new RandomAccessFile(file, "rw");
        return true;
    } catch (Exception e) {
        log.info("Skipping file " + file.getName() + " for this iteration due it's not completely written");
    } finally {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                log.error("Exception during closing file " + file.getName());
            }
        }
    }
    return false;
}
 */

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import br.com.tz.sm.Environment;
import br.com.tz.sm.Services;
import br.com.tz.util.Chronometer;

public final class Watcher implements br.com.tz.sm.Service
{
	private static boolean stopped = false;
	public static final List<File> lfiles = new ArrayList<File>();
	// this thread checks out the files on the 'files' list
	public static final Thread thread = new Thread(new FileHandler(),"Observer");
	
	static 
	{
		thread.setDaemon(true);
		thread.start();
	}
	
	public static void main(String[] args)
	{
	}

	@Override
	public void run()
	{
		// in case this service gets rerun, let's make sure flag 'stopped' is set to false
		stopped = false;
		
		System.out.println("Starting Watcher service");
		System.out.println("Watching directory '" + Environment.incomingPath.toAbsolutePath().toString() + "' for incoming services");
		
		handleExistingFiles();
		
		handleIncomingFiles();
		
		System.out.println("Stopping Watcher service");
	}
	
	private void handleIncomingFiles()
	{
		try
		{
			String spath = Environment.incomingPath.toAbsolutePath().toString();
			WatchService watchService = FileSystems.getDefault().newWatchService();
			Path path = Environment.incomingPath.toAbsolutePath();
		    WatchEvent.Kind<?>[] events = { StandardWatchEventKinds.ENTRY_CREATE };
//		            StandardWatchEventKinds.ENTRY_DELETE,
//		            StandardWatchEventKinds.ENTRY_MODIFY, 
//		            StandardWatchEventKinds.OVERFLOW };
			WatchKey watchKey = path.register(watchService, events);
			
	        WatchKey key;
	        try
			{
				while ((key = watchService.take()) != null && !stopped) 
				{
					//System.out.println("***** " + Thread.currentThread().getName() + " *****");
				    for (WatchEvent<?> event : watchKey.pollEvents()) 
				    {
				    	if ( event.context().toString().endsWith(".jar") )
				    	{
				    		// delegates the processing of the file to the thread Observer
				    		synchronized (lfiles)
							{
				    			lfiles.add( new File(spath + "/" + event.context().toString()) );
				    			lfiles.notifyAll();
							}
				    	}
				    }
				    key.reset();
				}
			} 
	        catch (InterruptedException e)
			{
				//e.printStackTrace();
			}
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void handleExistingFiles()
	{
		File[] files = Environment.incomingPath.toFile().listFiles( f -> f.getName().toLowerCase().endsWith(".jar") );
		
		if ( files == null || files.length <= 0 ) return;
		
		synchronized (lfiles)
		{
			for ( File file : files )
			{
				lfiles.add( file );
			}
			lfiles.notifyAll();
		}
	}
	
	@Override
	public void stop()
	{
		//Thread.currentThread().interrupt();
		stopped = true;
	}

	private static class FileHandler implements Runnable
	{
		@Override
		public void run()
		{
			while ( true )
			{
				while ( lfiles.size() > 0 )
				{
					// traverse the list in the reverse order to avoid problems when an item is removed
					for ( int i=lfiles.size()-1; i >= 0; i-- )
					{
						// if the modified time of the file is older than 5 secs 
						// assume the upload has finished then process it
						//System.out.println(lfiles.get(i).getName() + ", " + (System.currentTimeMillis() - lfiles.get(i).lastModified()));
						if ( (System.currentTimeMillis() - lfiles.get(i).lastModified()) >= 5_000 )
						{
							//Chronometer ch = new Chronometer();
							//ch.start("files list lock acquiring");
							// get exclusive access to the list in order to get out an item from it
							File file = null;
							synchronized (lfiles)
							{
								file = lfiles.remove(i);
							}
							//ch.stop();
							
							System.out.println("New incoming JAR file detected: " + file.getName() );
							try
							{
								System.out.println("Moving it from incoming directory to services directory");
								//file = org.tz.io.Files.move( file, Environment.servicesPath );
								//ch.start("file move");
								file = br.com.tz.io.Files.move(file).to(Environment.servicesPath).go();
								//ch.stop();
								//processJarFile( file );
								//ch.start("services searching and registering");
								for ( Service svc : Services.getServicesFromJarFile(file) )
								{
									System.out.println( "Registering the found service '" + svc.getClass().getName() + "' for later start" );
									Services.registerService(svc).startServiceLater(5);
								}
								//ch.stop();
							} 
							catch (IOException | ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | 
									InvocationTargetException | NoSuchMethodException | SecurityException | ServiceNotFoundException e)
							{
								e.printStackTrace();
							}
							//ch.printHistory();
						}
					}
					// take a nap before performing the next check
					try
					{
						Thread.sleep(500);
					} 
					catch (InterruptedException e)
					{
						break;
					}
				}
				// since the list got empty, wait until a new file arrives 
				synchronized (lfiles)
				{
					try
					{
						lfiles.wait();
					} 
					catch (InterruptedException e)
					{
						break;
					}
				}
			}
		}
	}
}
