/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package oap.logstream.disk;

import oap.io.Files;
import oap.logstream.LogId;
import oap.testng.Fixtures;
import oap.testng.TestDirectory;
import oap.util.Dates;
import oap.util.Maps;
import org.testng.annotations.Test;

import java.util.Map;

import static oap.io.IoStreams.Encoding.GZIP;
import static oap.io.IoStreams.Encoding.PLAIN;
import static oap.logstream.Timestamp.BPH_12;
import static oap.testng.Asserts.assertFile;
import static oap.testng.Env.tmpPath;

public class WriterTest extends Fixtures {
    private static final String FILE_PATTERN = "${p}-file-${INTERVAL}-${LOG_VERSION}.log.gz";

    {
        fixture( TestDirectory.FIXTURE );
    }

    @Test
    public void testMetadataChanged() {
        var headers = "REQUEST_ID";
        Dates.setTimeFixed( 2015, 10, 10, 1, 0 );
        var content = "1234567890";
        var bytes = content.getBytes();
        var logs = tmpPath( "logs" );

        var writer = new Writer( logs, FILE_PATTERN, new LogId( "", "type", "log", 0, Maps.of2( "p", "1" ), headers ), 10, BPH_12 );

        writer.write( bytes, ( msg ) -> {
        } );

        writer.close();

        writer = new Writer( logs, FILE_PATTERN, new LogId( "", "type", "log", 0, Maps.of2( "p", "1", "p2", "2" ), headers ), 10, BPH_12 );
        writer.write( bytes, ( msg ) -> {
        } );

        writer.close();

        assertFile( logs.resolve( "1-file-00-1.log.gz" ) )
            .hasContent( "REQUEST_ID\n" + content, GZIP );
        assertFile( logs.resolve( "1-file-00-1.log.gz.metadata.yaml" ) )
            .hasContent( """
                ---
                filePrefixPattern: ""
                type: "type"
                shard: "0"
                clientHostname: "log"
                p: "1"
                """.stripIndent() );

        assertFile( logs.resolve( "1-file-00-2.log.gz" ) )
            .hasContent( "REQUEST_ID\n" + content, GZIP );
        assertFile( logs.resolve( "1-file-00-2.log.gz.metadata.yaml" ) )
            .hasContent( """
                ---
                filePrefixPattern: ""
                type: "type"
                shard: "0"
                clientHostname: "log"
                p: "1"
                p2: "2"
                """.stripIndent() );

    }

    @Test
    public void write() {
        var headers = "REQUEST_ID";
        var new_headers = "REQUEST_ID\tH2";

        Dates.setTimeFixed( 2015, 10, 10, 1, 0 );
        var content = "1234567890";
        var bytes = content.getBytes();
        var logs = tmpPath( "logs" );
        Files.writeString(
            logs.resolve( "1-file-00-1.log.gz" ),
            PLAIN, "corrupted file" );
        Files.writeString(
            logs.resolve( "1-file-00-1.log.gz.metadata.yaml" ),
            PLAIN, """
                ---
                filePrefixPattern: ""
                type: "type"
                shard: "0"
                clientHostname: "log"
                p: "1"
                """.stripIndent() );

        var writer = new Writer( logs, FILE_PATTERN, new LogId( "", "type", "log", 0, Map.of( "p", "1" ), headers ), 10, BPH_12 );

        writer.write( bytes, ( msg ) -> {
        } );

        Dates.setTimeFixed( 2015, 10, 10, 1, 5 );
        writer.write( bytes, ( msg ) -> {
        } );

        Dates.setTimeFixed( 2015, 10, 10, 1, 10 );
        writer.write( bytes, ( msg ) -> {
        } );

        writer.close();

        writer = new Writer( logs, FILE_PATTERN, new LogId( "", "type", "log", 0, Map.of( "p", "1" ), headers ), 10, BPH_12 );

        Dates.setTimeFixed( 2015, 10, 10, 1, 14 );
        writer.write( bytes, ( msg ) -> {
        } );

        Dates.setTimeFixed( 2015, 10, 10, 1, 59 );
        writer.write( bytes, ( msg ) -> {
        } );
        writer.close();

        writer = new Writer( logs, FILE_PATTERN, new LogId( "", "type", "log", 0, Map.of( "p", "1" ), new_headers ), 10, BPH_12 );

        Dates.setTimeFixed( 2015, 10, 10, 1, 14 );
        writer.write( bytes, ( msg ) -> {
        } );
        writer.close();


        assertFile( logs.resolve( "1-file-01-1.log.gz" ) )
            .hasContent( "REQUEST_ID\n" + content, GZIP );
        assertFile( logs.resolve( "1-file-01-1.log.gz.metadata.yaml" ) )
            .hasContent( """
                ---
                filePrefixPattern: ""
                type: "type"
                shard: "0"
                clientHostname: "log"
                p: "1"
                """.stripIndent() );

        assertFile( logs.resolve( "1-file-02-1.log.gz" ) )
            .hasContent( "REQUEST_ID\n" + content + content, GZIP );
        assertFile( logs.resolve( "1-file-02-1.log.gz.metadata.yaml" ) )
            .hasContent( """
                ---
                filePrefixPattern: ""
                type: "type"
                shard: "0"
                clientHostname: "log"
                p: "1"
                """.stripIndent() );

        assertFile( logs.resolve( "1-file-11-1.log.gz" ) )
            .hasContent( "REQUEST_ID\n" + content, GZIP );

        assertFile( logs.resolve( "1-file-11-1.log.gz" ) )
            .hasContent( "REQUEST_ID\n" + content, GZIP );

        assertFile( logs.resolve( ".corrupted/1-file-00-1.log.gz" ) )
            .hasContent( "corrupted file" );
        assertFile( logs.resolve( ".corrupted/1-file-00-1.log.gz.metadata.yaml" ) )
            .hasContent( """
                ---
                filePrefixPattern: ""
                type: "type"
                shard: "0"
                clientHostname: "log"
                p: "1"
                """.stripIndent() );

        assertFile( logs.resolve( "1-file-02-2.log.gz" ) )
            .hasContent( "REQUEST_ID\tH2\n" + content, GZIP );
    }
}
