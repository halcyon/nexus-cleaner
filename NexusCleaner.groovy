#!/usr/bin/env groovy
import groovy.time.*

class NexusCleaner {

  def static settings = [
      baseUri:
      '/service/local/repositories/releases/content/',
      curlCommand: 'curl -v -X DELETE -u admin:admin123'
    ]


  def static main(def args) {
    if (args.length < 3) {
      println "Usage: ./NexusCleaner.groovy <nexusurl> <namespace> <months> [debug]"
      println "e.g.: ./NexusCleaner.groovy http://localhost:8081/nexus com/theice 1 debug"
      System.exit(1)
    }

    def nexusurl = args[0]
    def namespace = args[1]
    def months = args[2].toInteger()
    if (months < 1) {
      println "Months must be greater than 0"
      System.exit(1)
    }

    def debug = false
    if (args.length > 3) debug = true

    def nc = new NexusCleaner()
    def url = nexusurl+settings.baseUri+namespace
    nc.findRelease( url, months, debug)
  }

  def findRelease(def url, def months, def debug) {
    def urls = scanRepo(url)
    urls = urls.collect() { [it[0], new Date().parse('yyyy-MM-dd HH:mm:ss.S zzz',it[1])] }

    use (TimeCategory) {
      urls.each {
        if (it[1] < months.seconds.ago) {
          def command = settings.curlCommand + " " + it[0]
//          def proc = command.execute()
//          proc.waitFor()
          println command
//          println "return code: ${ proc.exitValue()}"
          if (debug) {
            println "stderr: ${proc.err.text}"
            println "stdout: ${proc.in.text}"
          }
        }
      }
    }
  }

  def scanRepo(def url) {
    def urls = []
    def data = fetchContent( url )
    data.data.'content-item'.each() {
      item->
      def name = item.text.text()
      if (item.leaf.text() == 'false') {
        if (name ==~ /^\d+(\.\d+)*.*/) {
            // it's a release number level
            urls << [item.resourceURI.text(),item.lastModified.text()]
        }
        else {
            urls += scanRepo( item.resourceURI.text() )
        }
      }
    }
    return urls
  }

  def fetchContent(String url) {
    try {
      def txt = new URL( url ).text
      def recs = new XmlSlurper().parseText( txt )
      return recs
    }
    catch (FileNotFoundException e) {
      println "FileNotFound Invalid URL: ${e.message}"
      System.exit(1)
    }
    catch (ConnectException e) {
      println "ConnectionException Invalid URL: ${e.message}"
      System.exit(1)
    }
  }
}
