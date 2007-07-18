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

import java.io.File;

/**
 * Maven mojo to compile jython source files to Java class files.
 * 
 * @author <a href="mailto:kmenard@servprise.com">Kevin Menard</a>
 *
 * @phase compile
 * @goal compile
 */
public class CompilerMojo extends AbstractCompilerMojo
{
    // TODO: (KJM 12/3/06) Check that the expression mirrors that of the compile mojo.
    /**
     *  Destination directory for Java classes (ignoring their package names).
     *
     * @parameter expression="${jython.sourceDir}" default-value="${project.build.sourceDirectory}/../jython/"
     */
    private File sourceDir;
    
    /**
     * The directory where compiled test classes go.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    private File destDir;

    
    public File getSourceDir()
    {
        return sourceDir;
    }

    public File getDestDir()
    {
        return destDir;
    }
}
