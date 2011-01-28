package org.dbpedia.spotlight.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: PabloMendes
 * Date: Jul 20, 2010
 * Time: 4:00:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class SeparateLuceneManager {

    Analyzer analyzer;
    Directory contextIndexDirectory;
    Directory surrogatesMappingDirectory;

    public SeparateLuceneManager(Directory directory, Directory surrogatesMappingDirectory) throws IOException {
        // How to break down the input text
        this.analyzer = new StandardAnalyzer(Version.LUCENE_29);
        this.contextIndexDirectory = directory;
        this.surrogatesMappingDirectory = surrogatesMappingDirectory;
    }

    public SeparateLuceneManager(File indexPath, File surrogatesIndexPath) throws IOException {
        this(FSDirectory.open(indexPath), FSDirectory.open(surrogatesIndexPath));
    }

    public Directory surrogatesMappingDirectory() {
        return surrogatesMappingDirectory;
    }
    

}
