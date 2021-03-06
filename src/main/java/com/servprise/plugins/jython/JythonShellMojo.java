/**
 * Copyright 2006 Servprise International, Inc.
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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.python.util.jython;

/**
 * Maven mojo to start an interactive jython shell.
 * 
 * @author Kevin Menard
 *
 * @prefix jython
 * @goal shell
 */
public class JythonShellMojo extends AbstractMojo
{
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        jython.main(new String[]{});
    }
}
