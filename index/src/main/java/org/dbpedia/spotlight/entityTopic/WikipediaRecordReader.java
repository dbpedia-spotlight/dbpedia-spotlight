package org.dbpedia.spotlight.entityTopic;

import java.io.*;
import java.io.ByteArrayOutputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;



/**
 * Scan a media wiki markup dump to read page title as key and page markup
 * payload as value.

 * WikipediaRecordReader class to read through a given xml to output the
 * page article title and un-escaped markup payload.
 */
public class WikipediaRecordReader  {
    public static final Charset UTF8 = Charset.forName("UTF-8");
    public static final byte[] START_TITLE_MARKER = "<title>".getBytes(UTF8);

    public static final byte[] END_TITLE_MARKER = "</title>".getBytes(UTF8);

    public static final byte[] START_ID_MARKER = "<id>".getBytes(UTF8);

    public static final byte[] END_ID_MARKER = "</id>".getBytes(UTF8);

    public static final byte[] START_TEXT_MARKER = "<text xml:space=\"preserve\">".getBytes(UTF8);

    public static final byte[] END_TEXT_MARKER = "</text>".getBytes(UTF8);

    private long start;

    private long end;

    private FileInputStream fsin;

    private FileChannel fch;

    private ByteArrayOutputStream buffer;



    private String currentKey = null;

    private String currentValue = null;

    private String currentId = null;

    public WikipediaRecordReader(File wikidump)
            throws IOException {
        // open the file and seek to the start of the split
        fsin=new FileInputStream(wikidump);
        start=0;
        fch=fsin.getChannel();
        end=fch.size();

        buffer = new ByteArrayOutputStream();

    }



    protected boolean next() throws IOException {
        String content;
        if ( fch.position() < end) {
            try {
                if (readUntilMatch(START_TITLE_MARKER, false)) {
                    if (readUntilMatch(END_TITLE_MARKER, true)) {
                        content=buffer.toString("UTF-8");
                        currentKey=content.substring(0,content.length()-END_TITLE_MARKER.length);
                        buffer.reset();
                        if (readUntilMatch(START_ID_MARKER, false)) {
                            if (readUntilMatch(END_ID_MARKER, true)) {
                                content=buffer.toString("UTF-8");
                                currentId=content.substring(0,content.length()-END_ID_MARKER.length);
                                //System.err.println(id);
                                buffer.reset();

                                if (readUntilMatch(START_TEXT_MARKER, false)) {
                                    if (readUntilMatch(END_TEXT_MARKER, true)) {
                                        // un-escape the XML entities encoding and
                                        // re-encode the result as raw UTF8 bytes

                                        content=buffer.toString("UTF-8");
                                        currentValue = content.substring(0, content.length()- END_TEXT_MARKER.length);
                                        buffer.reset();
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            } finally {

            }
        }
        return false;
    }

    public String createKey() {
        return null;
    }

    public String createValue() {
        return null;
    }

    public long getPos() throws IOException {
        return  fch.position();
    }

    public void close() throws IOException {
        fsin.close();
    }

    public float getProgress() throws IOException {
        return (fch.position() - start) / (float) (end - start);
    }

    private boolean readUntilMatch(byte[] match, boolean withinBlock)
            throws IOException {
        int i = 0;
        while (true) {
            int b = fsin.read();
            // end of file:
            if (b == -1) {
                return false;
            }
            // save to buffer:
            if (withinBlock) {
                buffer.write(b);
            }

            // check if we're matching:
            if (b == match[i]) {
                i++;
                if (i >= match.length) {
                    return true;
                }
            } else {
                i = 0;
            }
            // see if we've passed the stop point:
            if (!withinBlock && i == 0 &&  fch.position() >= end) {
                return false;
            }
        }
    }


    public String getCurrentKey() throws IOException, InterruptedException {
        return currentKey;
    }


    public String getCurrentValue() throws IOException, InterruptedException {
        return currentValue;
    }

    public String getWikipediaId() throws IOException, InterruptedException {
        return currentId;
    }




    public boolean nextKeyValue() throws IOException, InterruptedException {

        return next();
    }

    public static void main(String args[]) throws IOException, InterruptedException {
        WikipediaRecordReader reader=new WikipediaRecordReader(new File("../data/test.xml"));
        reader.nextKeyValue();
        System.out.println(reader.getCurrentKey());
        System.out.println(reader.getCurrentValue());
        System.out.println(reader.getWikipediaId());

        AnnotatingMarkupParser converter = new AnnotatingMarkupParser();
        String simpleText = converter.parse(reader.getCurrentValue());
        System.out.println(simpleText);
        for (Annotation a: converter.getWikiLinkAnnotations()) {
            // internal anchors are not extracted as links
            System.out.println(a.label+" "+simpleText.substring(a.begin,a.end));
        }
        reader.nextKeyValue();
        System.out.println(reader.getCurrentKey());
        System.out.println(reader.getCurrentValue());
        System.out.println(reader.getWikipediaId());
    }
}
