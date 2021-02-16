# SM
Services Manager

### Licence
GNU General Public License v2.0

### About
SM is intended to dinamically load/reload JAR files (plugin like).  
It monitors the incoming folder for .jar files.  
When one is dropped there it inspects it looking for a class implementing br.com.tz.sm.Service.  
If found, it loads the JAR's classes.  

### Dependencies
- Java 11
- log4j 2.11.1 (core and api) for logging stdout and stderr

### To do
The reload functionality isn't developed yet

### Testing
Create one or more jar files (see Usage section) and drop them into the incoming folder after starting the SM
```
$ java -cp .:/path/to/log4j-core-2.11.1.jar:/path/to/log4j-api-2.11.1.jar br.com.tz.sm.Manager -Dlog4j.configurationFile=etc/log4j2.xml -Dsm.servicesPath=/tmp/services -Dsm.incomingPath=/tmp/services/landing -Xms258m -Xmx1024m
2021-02-15 23:12:20.192  I  Initiating Services Manager
2021-02-15 23:12:20.198  I  Starting services lookup
2021-02-15 23:12:20.199  I  Starting Watcher service
2021-02-15 23:12:20.200  I  Services lookup is done
2021-02-15 23:12:20.209  I  Watching directory '/tmp/services/landing' for incoming services
2021-02-15 23:12:20.209  I  Services Manager's job is done
2021-02-15 23:12:52.472  I  New incoming JAR file detected: Test1.jar
2021-02-15 23:12:52.473  I  Moving it from incoming directory to services directory
2021-02-15 23:12:52.475  I  Inspecting file Test1.jar
2021-02-15 23:12:52.479  E  br.com.tz.sm.ServiceNotFoundException: No service found in '/tmp/services/Test1.jar'
2021-02-15 23:12:52.480  E  	at br.com.tz.sm.Services.getServicesFromJarFile(Services.java:256)
2021-02-15 23:12:52.480  E  	at br.com.tz.sm.Watcher$FileHandler.run(Watcher.java:187)
2021-02-15 23:12:52.480  E  	at java.base/java.lang.Thread.run(Thread.java:834)
2021-02-15 23:13:11.640  I  New incoming JAR file detected: Test2.jar
2021-02-15 23:13:11.641  I  Moving it from incoming directory to services directory
2021-02-15 23:13:11.641  I  Inspecting file Test2.jar
2021-02-15 23:13:11.644  I  Registering the found service 'com.tz.sm.test.Test2' for later start
2021-02-15 23:13:16.646  I  Starting Test2
2021-02-15 23:13:31.066  I  New incoming JAR file detected: Test3.jar
2021-02-15 23:13:31.067  I  Moving it from incoming directory to services directory
2021-02-15 23:13:31.068  I  Inspecting file Test3.jar
2021-02-15 23:13:31.070  I  Registering the found service 'com.tz.sm.test.Test3' for later start
2021-02-15 23:13:36.072  I  Starting Test3
2021-02-15 23:14:27.962  I  Shutdown process has been triggered
2021-02-15 23:14:27.963  I   stopping all services
2021-02-15 23:14:27.963  I  Stopping Watcher service
2021-02-15 23:14:27.964  I  Stopping Test2
2021-02-15 23:14:27.965  I  Stopping Test3```
```

### Usage
```
package br.com.tz.testing;

import br.com.tz.sm.Service;

public class Test01 implements Service
{
	private boolean stopped;
	
	@Override
	public void run()
	{
		System.out.println(this.getClass().getName() + ": starting");
		while ( !stopped )
		{
			System.out.println(this.getClass().getName());
			try
			{
				Thread.sleep(5000);
			} 
			catch (InterruptedException e)
			{
				break;
			}
		}
		System.out.println(this.getClass().getName() + ": ending");
	}

	@Override
	public void stop()
	{
		//stopped = true;
	}

}
```
