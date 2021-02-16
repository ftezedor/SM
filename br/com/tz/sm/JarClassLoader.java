package br.com.tz.sm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

public final class JarClassLoader extends java.net.URLClassLoader
{
	//protected Class<?> mainClass = null;
	
	public JarClassLoader(JarFile file) throws MalformedURLException
	{
		this( new URL[] { (new File(file.getName()).toURI().toURL()) } );
	}
	
	public JarClassLoader(URL[] urls)
	{
		super(urls);
	}
	
	protected void finalize()
	{
	    System.out.println("JarClassLoader for " + getURLs()[0].toString() + " is being garbage collected");
	}

	/**
	 * Appends the .jar file to the classpath and returns the class defined by the manifest's Main-Class attribute
	 * @param jarFile String
	 * @return Class
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static Class<?> loadJar(String jarFile) throws IOException, ClassNotFoundException
	{
		return loadJar(new File(jarFile));
	}

	/**
	 * Appends the .jar file to the classpath and returns the class defined by the manifest's Main-Class attribute
	 * @param jarFile File
	 * @return Class
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static Class<?> loadJar(File jarFile) throws IOException, ClassNotFoundException
	{
		try ( JarFile jf = new JarFile(jarFile) )
		{
			String jf_mclass = jf.getManifest().getMainAttributes().getValue("Main-Class");
			if ( jf_mclass == null ) throw new ClassNotFoundException("Main-Class not defined in " + jarFile);
			System.out.println(jarFile + " has the manifest's Main-Class attribute set to " + jf_mclass);
			JarClassLoader jcl =  new JarClassLoader( jf );
			Class<?> clazz = jcl.loadClass(jf_mclass);
			return clazz;
		}
		catch (IOException | ClassNotFoundException e) 
		{
			throw e;
		}
	}

}
