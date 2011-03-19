#!/usr/bin/env groovy
import groovy.time.*;

/*
*
* export NUSER=admin
* export NPASS=<NexusAdminPass>
* groovy NexusCleaner.groovy  | xargs -iF curl -v -X DELETE -u $NUSER:$NPASS F *
*/

class NexusCleaner {

    def settings = [
        baseUrl: 'http://localhost:8081/nexus/service/local/repositories/releases/content/'
     ];


    def static main( def args )
    {
      if (args.length != 2)
      {
        println "Usage: groovy NexusCleaner.groovy URI months"
        System.exit(1)
      }
      def uri = args[0]
      def months = args[1].toInteger()

      if ( months < 1 )
      {
        println "Months must be greater than 0"
        System.exit(1)
      }
      def nc = new NexusCleaner();
      nc.findRelease(uri, months);
    }

    def findRelease( def uri, def months )
    {
      def urls = scanRepo( settings.baseUrl+uri )
      urls = urls.collect() { [it[0], new Date().parse('yyyy-mm-dd hh:mm:ss.S zzz',it[1])] }
      println months

      urls.each { println it }
      use ( TimeCategory ) {
            // application on numbers:
//            println 1.minute.from.now
 //           println 10.days.ago
            // application on dates
  //          def someDate = new Date()
   //         println someDate - 3.months 
            urls.each{ println it[1] }
      }



    }


    def findRC()
    {

        def urls = scanRepo( settings.baseUrl );

        println urls

        urls.findAll(){ it ==~ /.*-RC\d+\/$/ }.each() {
            ver
            // calculate what would be the final version
            def rel = ver.replaceAll(/-RC\d+\/$/, '/' );
            // check if the final version already exists
            if( urls.find(){ it == rel } != null )
                println ver;
        }
        // delete irregular SNAPSHOTS ie: 1.0.2.0-SNAPSHOT2, 1.0.2.0-RC1SNAPSHOT2
        urls.findAll(){ it ==~ /.*SNAPSHOT.*\//}.each(){println it }

    }

    def scanRepo( def url ) {
        def urls = [];
        def data = fetchContent( url );
        data.data.'content-item'.each(){
            item->
            def name = item.text.text();
            if( item.leaf.text() == 'false' )
            {
                if(!( name ==~ /^\d+(\.\d+)*.*/ )) // it's a release number level
                {
                    urls += scanRepo( item.resourceURI.text() );
                }
                else
                {
                    urls << [item.resourceURI.text(),item.lastModified.text()];
                }
            }
        }
        return urls;
    }

    def fetchContent( String url )
    {
        def txt = new URL( url ).text;
        def recs = new XmlSlurper().parseText( txt );
        return recs;
    }

}
