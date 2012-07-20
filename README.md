sbt-dist
=========

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

 + libsDirectory a File abstraction pointing the directory containing file copies of the dependencies
 + transferDirectories providing a list of directory transfers apply from source directories to target directories
 + transferFilesInto providing a list of File to copy in destination directories

**Notice that the artifact files will be automatically copied into the libsDirectory**

For example if one wants to make a bulk copy of a set of scripts files :

          lazy val samiksa = Project(
            ...)
            .settings(DSbt.distSettings: _*)
            .settings(DSbt.transferDirectories  := Seq((new File("scripts") -> new File("dist/scripts"))))


Execution
-------------

Always execute a package command before using the plugin

The distribution is executed launching the command

>build-dist



