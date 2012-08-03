sbt-dist
=========

Getting the sbt-dist plugin
----------------------------

The sbt-dist plugin can be cloned from git using the line command

> git clone git@github.com:eligotech/sbt-dist.git


The project has been implemented using version 11.3 of sbt.
Scala Cross version compilation has been activated including versions 2.9.1 and 2.9.2

Not sure of your scala version ? Then publish it locally for both versions using the line
command:

> +publish-local

pre-pending the command with the **+** symbol


Installation
--------------

Create a plugins.sbt file in your project directory if not present already

Add the following line in your plugins.sbt file:

>addSbtPlugin("com.eligotech" %% "sbt-dist" % "0.1-SNAPSHOT")


Setting your Build.scala file
-----------------------------

Import com.eligotech.sbt.plugins.DSbt in your Build.scala file

Then initialise the plugin settings as following


          lazy val samiksa = Project(
            ...)
            .settings(DSbt.distSettings: _*)

**Important**

In the context of an multi-module project one should deactivate the parallelExecution as in

      lazy val samiksa = Project(
        ...)
        .settings(parallelExecution in DSbt.buildDist := false)
        .settings(DSbt.distSettings: _*)
        .settings(DSbt.transferDirectories  := Seq((new File("scripts") -> new File("dist/scripts"))))
        .aggregate(commons, core, webCollectors, akkaPipeline, enhancers, archivers, rotten)


distSettings exposes 3 environment settings

 + libsDirectory a File abstraction pointing the directory containing file copies of the dependencies.
 + excludeLibs as set of excluding rules allowing to exclude some libraries.
A rule is a function taking a file name as a parameter and returning a boolean true value if the file is to be excluded
 + transferDirectories providing a list of directory transfers apply from source directories to target directories
 + transferFilesInto providing a list of File to copy in destination directories


**The Default value of the libsDirectory is dist/lib**
**Notice that the artifact files will be automatically copied into the libsDirectory**

For example if one wants to make a bulk copy of a set of scripts files in a scripts directory and copy a specific Mahout model in a
sentiment directory,  one will provide the following definition:

          lazy val samiksa = Project(
            ...)
            .settings(DSbt.distSettings: _*)
            .settings(DSbt.transferDirectories  := Seq((new File("scripts") -> new File("dist/scripts"))))
            .settings(DSbt.transferFilesInto  := Seq((new File("modules/rotten/sentiment/model") -> new File("dist/sentiment"))))


Execution
-------------

Always execute a **package command** before using the plugin

The distribution is executed launching the command

>build-dist

One can clean the distribution files only using the line command

>clean-dist





