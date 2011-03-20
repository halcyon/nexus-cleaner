#!/usr/bin/env groovy
import groovy.time.*

class NexusCleaner {

  def static settings = [
      baseUri:
      '/service/local/repositories/releases/content/',
      curlCommand: 'curl -v -X DELETE -u admin:admin123'
    ]

  def static main(def args) {

    def cli = new CliBuilder(usage:'./NexusCleander.groovy -url <url> -ns <ns> -age <age> -user <user> -pass <pass> [real] [debug]')
    cli.url(args:1, argName:'url', required:true, 'URL to Nexus instance')
    cli.ns(args:1, argName:'ns', required:true, 'Namespace in Nexus to clean e.g. com/theice/ptms/ICEptms')
    cli.age(args:1, argName:'age', required:true, 'Artifacts older than this age given in months will be removed')
    cli.debug('Enable Debug Mode')
    cli.real('Execute removal - if not set defaults to a dry run')
    cli.user(args:1, argName:'user', required:true, 'Nexus user')
    cli.pass(args:1, argName: 'pass', required:true, 'Nexus password')
    cli.help('This help message')
    def options = cli.parse(args)

    def nc = new NexusCleaner()
    def url = options.url+settings.baseUri+options.ns
    nc.findRelease(url, options.age.toInteger(), options.debug, options.real)
  }

  def findRelease(def url, def age, def debug, def real) {
    def urls = scanRepo(url)
    urls = urls.collect() { [it[0], new Date().parse('yyyy-MM-dd HH:mm:ss.S zzz',it[1])] }

    use (TimeCategory) {
      urls.each {
        if (it[1] < age.seconds.ago) {
          def command = settings.curlCommand + " " + it[0]
          println command
          if (real) {
            def proc = command.execute()
            proc.waitFor()
            println "return code: ${ proc.exitValue()}"
            if (debug) {
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
      def txt = new URL(url).text
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
