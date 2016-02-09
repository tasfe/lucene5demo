package com.patterncat.lucene5;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by patterncat on 2016-02-09.
 */
public class SortDemo {

    IndexReader reader;
    IndexSearcher searcher;
    //返回最相关的文档数
    int hitsPerPage = 10;

    @Before
    public void prepare() throws URISyntaxException, IOException {
        Path indexPath = Paths.get(this.getClass().getClassLoader().getResource("").toURI());
        Directory index = FSDirectory.open(indexPath);
        reader = DirectoryReader.open(index);
        searcher = new IndexSearcher(reader);
    }

    @After
    public void destory() throws IOException {
        reader.close();
    }

    /**
     * doDocScores,对每个搜索结果进行评分,不占用太多资源,一般为true
     * doMaxScore,对最大分值的搜索结果进行评分,比较占用系统资源,一般false
     * @param query
     * @param sort
     * @throws IOException
     */
    public void executeQuery(Query query,Sort sort) throws IOException {
        boolean doDocScores = true;
        boolean doMaxScore = false;
        TopDocs topDocs = searcher.search(query, hitsPerPage,sort,doDocScores,doMaxScore);
        ScoreDoc[] hits = topDocs.scoreDocs;
        for(ScoreDoc doc:hits){
            System.out.println(reader.document(doc.doc));
            System.out.println(searcher.explain(query,doc.doc));
        }
    }

    /**
     * 按指定字段排序
     * @throws IOException
     * @throws ParseException
     */
    @Test
    public void sortByField() throws IOException, ParseException {
        //Sort using term values as encoded Integers.  Sort values are Integer and lower values are at the front.
        boolean isReverse = false;
        SortField sortField = new SortField("title", SortField.Type.STRING,isReverse);
        Query query = new TermQuery(new Term("title","lucene"));
        Sort sort = new Sort(sortField);
        executeQuery(query, sort);
    }

    /**
     * 按索引顺序排序
     * @throws IOException
     */
    @Test
    public void sortByIndexOrder() throws IOException {
        Query query = new TermQuery(new Term("title","lucene"));
        executeQuery(query,Sort.INDEXORDER);
    }

    /**
     * 按文档的得分排序
     * @throws IOException
     */
    @Test
    public void sortByRelevance() throws IOException {
        TermQuery queryComputerInTitle = new TermQuery(new Term("title","computer"));
        TermQuery queryGoodInDesc = new TermQuery(new Term("desc","good"));

        BooleanQuery query = new BooleanQuery.Builder()
                .add(queryComputerInTitle,BooleanClause.Occur.SHOULD)
                .add(queryGoodInDesc,BooleanClause.Occur.SHOULD)
                .setMinimumNumberShouldMatch(1)
                .build();
        executeQuery(query,Sort.RELEVANCE);
    }

}
