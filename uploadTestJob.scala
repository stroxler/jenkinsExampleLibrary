#!/usr/bin/env amm
import $ivy.`com.offbytwo.jenkins:jenkins-client:0.3.7`

import java.net.URI
import ammonite.ops._
import com.offbytwo.jenkins.JenkinsServer

@main
def main(scriptName: String, path: Path = pwd) = {
  val jenkinsFile = scriptName + ".groovy"
  val jenkinsPath = path / jenkinsFile
  val groovyScript: String = read! jenkinsPath
  val jenkins = getJenkinsServer()
  val xml = jenkinsXml(groovyScript, scriptName)
  if (jobExists(scriptName, jenkins)) {
    jenkins.updateJob(scriptName, xml.toString)
    println(s"Updated job ${scriptName}")
  } else {
    jenkins.createJob(scriptName, xml.toString)
    println(s"Created job ${scriptName}")
  }

}


def jobExists(name: String, jenkins: JenkinsServer): Boolean = {
  val job = jenkins.getJob(name)
  job != null
}


def jenkinsXml(groovyScript: String, scriptName: String) = {
  <flow-definition plugin="workflow-job">
    <actions>
      <io.jenkins.blueocean.service.embedded.BlueOceanUrlAction plugin="blueocean-rest-impl">
        <blueOceanUrlObject class="io.jenkins.blueocean.service.embedded.BlueOceanUrlObjectImpl">
          <mappedUrl>
            blue/organizations/jenkins/{ scriptName }
          </mappedUrl>
        </blueOceanUrlObject>
      </io.jenkins.blueocean.service.embedded.BlueOceanUrlAction>
    </actions>
    <description>
{ groovyScript }
    </description>
    <keepDependencies>
      false
    </keepDependencies>
    <properties>
      <org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty>
        <triggers/>
      </org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty>
    </properties>
    <definition class="org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition" plugin="workflow-cps">
      <script>
{ groovyScript }
      </script>
      <sandbox>true</sandbox>
    </definition>
    <triggers/>
    <disabled>false</disabled>
  </flow-definition>
}


def getJenkinsServer() = {
  val host = "localhost"
  val port = "49001"
  val user = "dev"
  val password = "dev"
  new JenkinsServer(new URI(s"http://${host}:${port}"), user, password)
}
