/**
 * Copyright 2011 Pablo Mendes, Max Jakob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
