/*
*
* export NUSER=admin
* export NPASS=<NexusAdminPass>
* groovy NexusCleaner.groovy  | xargs -iF curl -v -X DELETE -u $NUSER:$NPASS F
*
*/

class NexusCleaner {

    def settings = [ 
        baseUrl: 'http://localhost:8081/nexus/service/local/repositories/releases/content/', 
     ];


    def static main( def args )
    {
        def nc = new NexusCleaner();
        nc.findRC();
    }

    def findRC()
    {

        def urls = scanRepo( settings.baseUrl );

        urls.findAll(){ it ==~ /.*-RC\d+\/$/ }.each() {
            ver -> 
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
            item
            def name = item.text.text();
            if( item.leaf.text() == 'false' )
            {
                if(!( name ==~ /^\d+\.\d+\.\d+\.\d+.*/ )) // it's a release number level
                {
                    urls += scanRepo( item.resourceURI.text() );
                }
                else
                {
                    urls << item.resourceURI.text();
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
