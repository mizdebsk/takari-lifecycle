package io.takari.maven.plugins.resources;

import io.takari.maven.plugins.TakariLifecycleMojo;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

public abstract class AbstractProcessResourcesMojo extends TakariLifecycleMojo {

  @Parameter(defaultValue = "${project.properties}")
  private Properties properties;

  @Inject
  private ResourcesProcessor processor;

  protected void process(List<Resource> resources, File outputDirectory)
      throws MojoExecutionException {
    for (Resource resource : resources) {
      boolean filter = Boolean.parseBoolean(resource.getFiltering());
      try {
        File sourceDirectory = new File(resource.getDirectory());
        // Ensure the sourceDirectory is actually present before attempting to process any resources
        if (!sourceDirectory.exists()) {
          continue;
        }
        sourceDirectory = sourceDirectory.getCanonicalFile();
        File targetDirectory;
        if (resource.getTargetPath() != null) {
          targetDirectory = new File(outputDirectory, resource.getTargetPath());
        } else {
          targetDirectory = outputDirectory;
        }
        if (filter) {
          processor.process(sourceDirectory, targetDirectory, resource.getIncludes(),
              resource.getExcludes(), properties);
        } else {
          processor.process(sourceDirectory, targetDirectory, resource.getIncludes(),
              resource.getExcludes());
        }
      } catch (IOException e) {
        throw new MojoExecutionException(e.getMessage(), e);
      }
    }
  }

}