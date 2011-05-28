#!/usr/bin/env groovy
import groovy.time.*

class NexusCleaner {

  def static settings = [
      baseUri:
      '/service/local/repositories/releases/content/',
      curlCommand: 'curl -v -X DELETE -u'
    ]

  def static main(def args) {

    def cli = new CliBuilder(usage:'./NexusCleander.groovy -url <url> -ns <ns> -age <age> -user <user> -pass <pass> [-arm] [-debug <debugLevel>]')
    cli.url(args:1, argName:'url', required:true, 'URL to Nexus instance e.g. http://localhost:8081/nexus')
    cli.ns(args:1, argName:'ns', required:true, 'Namespace in Nexus to clean e.g. com/nexuscleaner/someproject')
    cli.age(args:1, argName:'# of months', required:true, 'Remove everything in the namespace older than age')
    cli.debug(args:1, argName:'level', 'Enable Debug Mode')
    cli.arm('Execute removal - if not set defaults to a dry run')
    cli.user(args:1, argName:'user', required:true, 'Nexus user')
    cli.pass(args:1, argName: 'pass', required:true, 'Nexus password')
    cli.help('This help message')
    def options = cli.parse(args)
    if (!options) {
      System.exit(1)
    }

    if (options.age.toInteger() < 3) {
      println 'Age must be at least 3 months'
      System.exit(1)
    }

    def debugLevel = 0
    if (options.debug) {
      debugLevel = options.debug.toInteger()
    }

    def nc = new NexusCleaner()
    def url = options.url+settings.baseUri+options.ns
    nc.findRelease(url, options.age.toInteger(), debugLevel, options.arm,
    options.user, options.pass)
  }

  def findRelease(def url, def age, def debugLevel, def armed, def user, def pass) {
    def urls = scanRepo(url)
    urls = urls.collect() { [it[0], new Date().parse('yyyy-MM-dd HH:mm:ss.S zzz',it[1])] }

    use (TimeCategory) {
      urls.each {
        if (it[1] < age.months.ago) {
          def command = settings.curlCommand + " ${user}:${pass} " + it[0]
          println command
          println "timestamp: ${it[1]}"
          if (armed) {
            def proc = command.execute()
            proc.waitFor()
            if (debugLevel > 0) println "return code: ${ proc.exitValue()}"
            if (debugLevel > 1) {
              println "stderr: ${proc.err.text}"
              println "stdout: ${proc.in.text}"
            }
          }
        }
      }
    }
  }

  def scanRepo(def url) {
    def urls = []
    def data = fetchContent(url)
    data.data.'content-item'.each() {
      def name = it.text.text()
      if (it.leaf.text() == 'false') {
        if (name ==~ /^\d+(\.\d+)*.*/) {
            // it's a release number level
            urls << [it.resourceURI.text(),it.lastModified.text()]
        }
        else {
            urls += scanRepo(it.resourceURI.text())
        }
      }
    }
    return urls
  }

  def fetchContent(String url) {
    try {
      def txt = new URL(url).text
      def recs = new XmlSlurper().parseText(txt)
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
