/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.run;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;

public class JoobyTask extends ConventionTask {

  private List<String> includes;

  private List<String> excludes;

  private String logLevel;

  private boolean block;

  private Set<File> classpath;

  private String mainClassName;

  @TaskAction
  public void run() throws Exception {

    System.setProperty("logLevel", getLogLevel());

    Project project = getProject();

    String mId = project.getName();

    List<File> cp = new ArrayList<>();

    // conf & public
    getClasspath().forEach(cp::add);

    Main app = new Main(mId, getMainClassName(), cp.toArray(new File[cp.size()]));
    if (includes != null) {
      app.includes(includes.stream().collect(Collectors.joining(File.pathSeparator)));
    }

    if (excludes != null) {
      app.excludes(excludes.stream().collect(Collectors.joining(File.pathSeparator)));
    }

    app.run(isBlock());
  }

  public void setIncludes(final List<String> includes) {
    this.includes = includes;
  }

  public List<String> getIncludes() {
    return includes;
  }

  public void setExcludes(final List<String> excludes) {
    this.excludes = excludes;
  }

  public List<String> getExcludes() {
    return excludes;
  }

  public void setLogLevel(final String logLevel) {
    this.logLevel = logLevel;
  }

  public String getLogLevel() {
    return logLevel;
  }

  @InputFiles
  public Set<File> getClasspath() {
    return classpath;
  }

  public void setClasspath(final Set<File> classpath) {
    this.classpath = classpath;
  }

  public void setBlock(final boolean block) {
    this.block = block;
  }

  public boolean isBlock() {
    return block;
  }

  public String getMainClassName() {
    return mainClassName;
  }

  public void setMainClassName(final String mainClassName) {
    this.mainClassName = mainClassName;
  }
}
