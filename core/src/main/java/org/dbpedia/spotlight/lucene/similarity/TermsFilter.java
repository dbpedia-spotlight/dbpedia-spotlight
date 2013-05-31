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

package org.dbpedia.spotlight.lucene.similarity;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.OpenBitSet;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class TermsFilter extends Filter {

    Set<Term> terms=new TreeSet<Term>();

    /**
     * Adds a term to the list of acceptable terms
     * @param term
     */
    public void addTerm(Term term)
    {
        terms.add(term);
    }

    /* (non-Javadoc)
    * @see org.apache.lucene.search.Filter#getDocIdSet(org.apache.lucene.index.IndexReader)
    */
    @Override
    public DocIdSet getDocIdSet(IndexReader reader) throws IOException
    {
        OpenBitSet result=new OpenBitSet(reader.maxDoc());
        TermDocs td = reader.termDocs();
        try {
            int c = 0;
            for (Iterator<Term> iter = terms.iterator(); iter.hasNext();)
            {
                Term term = iter.next();
                td.seek(term);
                while (td.next())
                {
                    c++;
                    result.set(td.doc());
                }
            }
        }
        finally
        {
            td.close();
        }
        return result;
    }

//        public DocIdSet getDocIdSet(IndexReader reader) throws IOException
//        {
//            OpenBitSet[] buffer = new OpenBitSet[terms.size()];
//
//            OpenBitSet result=new OpenBitSet(reader.maxDoc());
//            TermDocs td = reader.termDocs();
//            try {
//                boolean first = true;
//                for (Iterator<Term> iter = terms.iterator(); iter.hasNext();)
//                {
//                    OpenBitSet r = new OpenBitSet(reader.maxDoc());
//                    Term term = iter.next();
//                    td.seek(term);
//                    while (td.next())
//                    {
//                        r.set(td.doc());
//                    }
//                    if (first)
//                        result = r;
//                    else
//                        result.and(r);
//                    first = false;
//                    long test1 = r.cardinality();
//                    long test2 = result.cardinality();
//                    System.out.println("r:"+test1);
//                    System.out.println("result:"+test2);
//                }
//            }
//            finally
//            {
//                td.close();
//            }
//            return result;
//        }


    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
            return true;
        if((obj == null) || (obj.getClass() != this.getClass()))
            return false;
        TermsFilter test = (TermsFilter)obj;
        return (terms == test.terms ||
                (terms != null && terms.equals(test.terms)));
    }

    @Override
    public int hashCode()
    {
        int hash=9;
        for (Iterator<Term> iter = terms.iterator(); iter.hasNext();)
        {
            Term term = iter.next();
            hash = 31 * hash + term.hashCode();
        }
        return hash;
    }

}
