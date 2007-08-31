/**
 * Copyright 2006 - 2007 Servprise International, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.servprise.plugins.jython;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Base set of functionality for concrete mojos to compile
 * jython source files to Java class files.
 * 
 * @author <a href="mailto:kmenard@servprise.com">Kevin Menard</a>
 */
public abstract class AbstractCompilerMojo extends AbstractMojo
{   
    /**
     * @parameter default-value="${plugin.artifacts}"
     * @required
     * @readonly
     */
    private List pluginArftifacts;
    
    /**
     * Path to a jython installation.
     * 
     * @parameter expression="${jython.jythonHome}"
     * @required
     */
    private String jythonHome;
        
    /**
     * Compile all Python dependencies of the module. This is used for creating applets.
     * 
     * @parameter expression="${jython.deep}" default-value="false"
     */
    private boolean deep;
    
    /**
     * Include the core Jython libraries (about 130K). Needed for applets since Netscape
     * doesn't yet support multiple archives. Implies <code>deep = true</code>.
     * 
     * @parameter expression="${jython.core}" default-value="false"
     */
    private boolean core;
        
    /**
     * Include all of the Jython libraries (everything in core + compiler and parser).
     * Implies <code>deep = true</code> and <code>core = true</code>.
     * 
     * @parameter expression="${jython.all}" default-value="false"
     */
    private boolean all;
    
    public abstract File getSourceDir();
    
    public abstract File getDestDir();
    
    public void execute() throws MojoExecutionException, MojoFailureException
    {   
        // Firs thing to do is perform a sanity check on the mojo's parameters.
        checkValidDirectory("sourceDir", getSourceDir());
        checkValidDirectory("jythonHome", new File(jythonHome));
        
        // Cycle through all this plugin's artifacts until we find Jython.  Then add it to the classpath.
        // This is necessary for Jython to work properly, since Maven's use of classworlds rather than classpaths
        // messes with Jython.
        for (final Iterator it = pluginArftifacts.iterator(); it.hasNext();)
        {
            final Artifact a = (Artifact) it.next();

            // Add the Jython JAR to the classpath so that it may be indexed properly by jythonc.
            if (a.getArtifactId().equals("jython"))
            {
                System.setProperty("java.class.path", System.getProperty("java.class.path") + ";" + a.getFile().getAbsolutePath());
            }
        }
        
        // Since Jython relies on .py files being installed on the local machine, we need a path
        // to that local installation.
        System.setProperty("python.home", jythonHome);
        
        
        
        // Start building up the command and arguments for executing jythonc in its own process.
        final List<String> partialArgs = new ArrayList<String>();
        
        // We assume that "java" is on the system path.  At some point, however, we should build up a complete path to the binary.
        partialArgs.add("java");
        
        // Make sure that the jython jar is on the classpath.
        partialArgs.add("-cp");
        partialArgs.add(jythonHome + "/jython.jar");
        
        // This is the main jython class we will be executing.
        partialArgs.add("org.python.util.jython");
        
        // Tell the jython class that we want to execute the jythonc tool.
        partialArgs.add(jythonHome + "/Tools/jythonc/jythonc.py");

        // Disable the warning message about jythonc being deprecated.
        partialArgs.add("-i");
        
        
        
        // From here on out, we're passing additional arguments that the jythonc.py file expects to see.
        // org.python.util.jython will make sure that the jythonc.py file executes with the proper set of arguments.
        
        // Let jythonc know what directory we want to work out of.
        partialArgs.add("--workdir=" + getDestDir().getAbsolutePath());
        
        if (true == deep)
        {
            partialArgs.add("--deep");
        }
        
        if (true == core)
        {
            partialArgs.add("--core");
        }
        
        if (true == all)
        {
            partialArgs.add("--all");
        }
        
        final List<String> args = new ArrayList<String>();
        for (final File file: getJythonFiles(getSourceDir()))
        {
            try
            {
                args.clear();
                args.addAll(partialArgs);
                
                final String path = file.getCanonicalPath();
                final String packageName = extractPackage(file);
                
                // If the jython file has an implied package name, tell jythonc to use that
                // package when generating the Java source file.
                if (null != packageName)
                {
                    args.add("--package");
                    args.add(packageName);
                }
                
                // Finish off the command by passing the jython source file as the last argument.
                args.add(path);
                
                // TODO: (KJM 4/3/07) Compiling each file in its own process is a bit slow, but necessary right now to ensure correctness of package name.  At some point, we should probably hash by package name and compile all files in a package at once.
                // Compile the jython source file.
                runJythonc(args);
            }
            catch (final IOException e)
            {
                throw new MojoExecutionException("Failed to retrieve the path to a jython source file.", e);
            }
            catch (final Exception e)
            {
                throw new MojoExecutionException("Failed to run jythonc.", e);
            }
        }
    }
    
    /**
     * Log the debug message.
     * 
     * @param message The message to log.
     */
    private void debug(final String message)
    {
        this.getLog().debug(message);
    }
    
    /**
     * Retrieves all the jython source files in the given directory and all its subdirectories.
     * 
     * @param directory The directory to scan.
     * 
     * @return A list of all Jython source files contained in the directory.
     */
    private List<File> getJythonFiles(final File directory)
    {
        final List<File> ret = new ArrayList<File>();
        
        // Scan the directory for Jython files.
        for (final File file : directory.listFiles(new SimpleFilenameFilter()))
        {
            // If the file is a directory, then recursively scan that directory.
            if (file.isDirectory())
            {
                ret.addAll(getJythonFiles(file));
            }
            
            // Otherwise, add the file to the returned output.
            else
            {
                ret.add(file);
            }
        }
        
        return ret;
    }
    
    /**
     * Extract the appropriate Java package to use for the Jython source file based
     * upon its directory structure.
     * 
     * @param file The file to extract the package for.
     * 
     * @return The extracted package name or <code>null</code> if none exists.
     * 
     * @throws IOException On file I/O errors.
     */
    private String extractPackage(final File file) throws IOException
    {
        String ret = null;
        
        final String path = file.getCanonicalPath();
        
        // A file only has a package if "sourceDir" is not the directory in which the file is contained.
        // In this case, the package is the empty package, represented by null.
        if (false == file.getParentFile().equals(getSourceDir()))
        {
            ret = path.substring(getSourceDir().getCanonicalPath().length() + 1, path.indexOf(file.getName()) - 1);
            ret = ret.replaceAll("\\" + File.separator, ".");
        }
        
        debug("Discovered the package: " + ret);
        
        return ret;
    }
    
    private void runJythonc(final List<String> args) throws IOException, InterruptedException
    {
        // Now that we've built up the command we want to run, set things up so it can run in its own process.                
        final StringBuffer cmd = new StringBuffer();
        for (final String arg : args)
        {
            cmd.append(arg).append(" ");
        }
        debug("Command executed: " + cmd);

        // Build up the process environment and start the process.
        final ProcessBuilder pb = new ProcessBuilder(args);
        pb.redirectErrorStream(true);
        final Process p = pb.start();

        // Capture all of the output from jythonc and dump it to STDOUT.
        final BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = "";
        while (null != line)
        {
            System.out.println(line);

            line = in.readLine();
        }

        // Block until the jythonc process finishes executing.
        debug("Jythonc status: " + p.waitFor());
    }
    
    /**
     * Utility method to ensure that a supplied parameter is actually a directory.
     * 
     * @param directoryName The name of the parameter.
     * @param value The parameter value supplied by the user.
     * 
     * @throws MojoFailureException If the value is not a valid directory.
     */
    private void checkValidDirectory(final String directoryName, final File value) throws MojoFailureException
    {
        if ((null == value) || (false == value.isDirectory()) || (false == value.exists()))
        {
            throw new MojoFailureException("'" + directoryName + "' is not a valid directory: " + value);
        }
    }
    
    /**
     * Simple filter so that we only deal with actual Jython source files.
     * 
     * @author Kevin Menard
     */
    private class SimpleFilenameFilter implements FilenameFilter
    {
        /**
         * We accept all files that end with the ".py" extension or that are themselves directories.
         */
        public boolean accept(final File dir, final String name)
        {
            final File full = new File(dir, name);
            
            // TODO: (KJM 03/20/07) This should probably be configurable.
            return name.endsWith(".py") || full.isDirectory();
        }   
    }
}
