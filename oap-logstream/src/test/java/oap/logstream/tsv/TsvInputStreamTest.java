package oap.logstream.tsv;

import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Created by igor.petrenko on 2019-10-01.
 */
public class TsvInputStreamTest {

    @Test
    public void testReadCells_IndexOf() throws IOException {
        var data = "a\tb\tc\t".getBytes( UTF_8 );
        var is = new TsvInputStream( new ByteArrayInputStream( data ), new byte[1024] );
        assertTrue( is.readCells() );

        assertThat( is.line.indexOf( "a" ) ).isEqualTo( 0 );
        assertThat( is.line.indexOf( "c" ) ).isEqualTo( 2 );
        assertThat( is.line.indexOf( "d" ) ).isEqualTo( -1 );

        assertFalse( is.readCells() );
    }
}
