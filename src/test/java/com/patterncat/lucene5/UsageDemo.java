package com.patterncat.lucene5;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * http://www.norconex.com/upgrading-code-to-lucene-4/
 * http://www.lucenetutorial.com/index.html
 * https://github.com/macluq/helloLucene
 * Created by patterncat on 2016-02-07.
 */
public class UsageDemo {


    @Test
    public void createIndex() throws IOException, URISyntaxException {
//        Directory index = new RAMDirectory();
        Path indexPath = Paths.get(this.getClass().getClassLoader().getResource("").toURI());
        Directory index = FSDirectory.open(indexPath);
        // 0. Specify the analyzer for tokenizing text.
        //    The same analyzer should be used for indexing and searching
        StandardAnalyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        // 1. create the index
        IndexWriter w = new IndexWriter(index, config);
        addDoc(w, "Lucene in Action","action for beginners, cover lucene 3.x","193398817");
        addDoc(w, "Lucene for Dummies","dummies series , good tutorial for dummy","55320055Z");
        addDoc(w, "Managing Gigabytes","deep into the detail of gigabytes","55063554A");
        addDoc(w, "The Art of Computer Science","a good book for artiest about cs","9900333X");
        w.close();
    }

    private void addDoc(IndexWriter w, String title,String desc,String isbn) throws IOException {
        Document doc = new Document();
        doc.add(new TextField("title", title, Field.Store.YES));
        //add for sorting
        //http://stackoverflow.com/questions/29695307/sortiing-string-field-alphabetically-in-lucene-5-0
        doc.add(new SortedDocValuesField("title", new BytesRef(title)));
        doc.add(new TextField("desc",desc,Field.Store.YES));
        // use a string field for isbn because we don't want it tokenized
        doc.add(new StringField("isbn", isbn, Field.Store.YES));
        w.addDocument(doc);
    }

    @Test
    public void iterateIndex() throws IOException, URISyntaxException {
        Path indexPath = Paths.get(this.getClass().getClassLoader().getResource("").toURI());
        Directory index = FSDirectory.open(indexPath);
        IndexReader reader = DirectoryReader.open(index);
        List<LeafReaderContext> leaves = reader.leaves();
        for (LeafReaderContext context : leaves) {
            LeafReader atomicReader = context.reader();
            Fields fields = atomicReader.fields();
            for (String fieldName : fields) {
                System.out.println("field:"+fieldName);
                Terms terms = atomicReader.terms(fieldName);
                TermsEnum te = terms.iterator();
                BytesRef term;
                while ((term = te.next()) != null) {
                    System.out.println(term.utf8ToString());
                }
            }
        }
    }

    @Test
    public void search() throws IOException, URISyntaxException {
        // 2. query
        String querystr = "lucene";

        // the "title" arg specifies the default field to use
        // when no field is explicitly specified in the query.
        Query q = null;
        try {
            StandardAnalyzer analyzer = new StandardAnalyzer();
            q = new QueryParser("title", analyzer).parse(querystr);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 3. search
        int hitsPerPage = 10;

        Path indexPath = Paths.get(this.getClass().getClassLoader().getResource("").toURI());
        Directory index = FSDirectory.open(indexPath);
        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage);
        searcher.search(q, collector);
        ScoreDoc[] hits = collector.topDocs().scoreDocs;

        // 4. display results
        System.out.println("Found " + hits.length + " hits.");
        for (int i = 0; i < hits.length; ++i) {
            int docId = hits[i].doc;
            Document d = searcher.doc(docId);
            System.out.println((i + 1) + ". " + d.get("isbn") + "\t" + d.get("title"));
        }

        // reader can only be closed when there
        // is no need to access the documents any more.
        reader.close();
    }

    /**
     * https://github.com/wgybzbrobot/sina-services/blob/d7b2b995d067c6b641a2b92f3dbb5b811a8ec433/fuzzy-search/src/main/java/cc/pp/fuzzy/search/analyzer/MMSeg4jAnalyzerDemo.java
     * @throws IOException
     */
    @Test
    public void cutWords() throws IOException {
//        StandardAnalyzer analyzer = new StandardAnalyzer();
        CJKAnalyzer analyzer = new CJKAnalyzer();
        String text = "Spark是当前最流行的开源大数据内存计算框架，采用Scala语言实现，由UC伯克利大学AMPLab实验室开发并于2010年开源。";
        TokenStream tokenStream = analyzer.tokenStream("content", new StringReader(text));
        CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        try {
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                System.out.println(charTermAttribute.toString());
            }
            tokenStream.end();
        } finally {
            tokenStream.close();
            analyzer.close();
        }
    }
}
