package com.patterncat.lucene5;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.search.spans.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * http://wwty.iteye.com/blog/683557
 * Created by patterncat on 2016-02-08.
 */
public class QueryDemo {

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

    public void executeQuery(Query query) throws IOException {
//        TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage);
//        searcher.search(query, collector);
//        ScoreDoc[] hits = collector.topDocs().scoreDocs;
//        System.out.println("Found " + hits.length + " hits.");
//        for (int i = 0; i < hits.length; ++i) {
//            int docId = hits[i].doc;
//            Document d = searcher.doc(docId);
//            System.out.println((i + 1) + ". " + d.get("isbn") + "\t" + d.get("title"));
//        }
        TopDocs topDocs = searcher.search(query, hitsPerPage);
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = searcher.doc(scoreDoc.doc);
            System.out.println(doc);
        }
    }

    /**
     * 查找指定field中包含某个关键字
     * @throws IOException
     */
    @Test
    public void termQuery() throws IOException {
        String field = "title";
//        String queryStr = "in";
//        String queryStr = "Lucene in Action";
//        String queryStr = "action";
        String queryStr = "lucene";
        Term term = new Term(field,queryStr);
        Query query = new TermQuery(term);
        executeQuery(query);
    }

    /**
     * 查找指定字段中包含与关键字相似的文档
     * 查询用于匹配与指定项相似的项
     * 编辑距离算法，两个字符串之间相似度的一个度量方法
     * 用来决定索引文件中的项与指定目标项的相似程度.
     * 取所有相同前缀(前缀长度可以设定)的词项做编辑距离
     *
     * 编辑距离实际上是表明两个不同的字符串需要经过多少次编辑和变换才能变为对方。
     * 通常的编辑行为包括了增加一个检索项，删除一个检索项，修改一个检索项，
     * 与普通的字符串匹配函数不同，模糊搜索里的编辑距离是以索引项为单位的。
     *
     * http://www.xinxilong.com/html/?2481.html
     * @throws IOException
     */
    @Test
    public void fuzzyQuery() throws IOException {
        String field = "title";
        String queryStr = "act";// 自动在结尾添加 ~ ,即查询act~
        Term term = new Term(field,queryStr);
        int maxEdits = 1;  //编辑距离最多不能超过多少
        int prefixLength = 3; //相同的前缀长度
//        Query query = new FuzzyQuery(term,maxEdits,prefixLength);
        Query query = new FuzzyQuery(term,maxEdits);
//        Query query = new FuzzyQuery(term);
        executeQuery(query);
    }

    /**
     * http://my.oschina.net/MrMichael/blog/220694
     * 同一个关键词多个字段搜索
     * 用MultiFieldQueryParser类实现对同一关键词的跨域搜索
     */
    @Test
    public void multiFieldQueryCrossFields() throws ParseException, IOException {
        String[] fields = new String[]{"title","desc"};
        String queryStr = "good";
        Map<String , Float> boosts = new HashMap<String, Float>();
        //设定它们在搜索结果排序过程中的权重，权重越高，排名越靠前
        boosts.put("title", 1.0f);
        boosts.put("desc", 0.7f);
        MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, new StandardAnalyzer(),boosts);
        Query query = parser.parse(queryStr);
        executeQuery(query);
    }

    /**
     * 使用多个关键字,及多个field进行查询
     */
    @Test
    public void multiFieldQueryMultiKeyword() throws ParseException, IOException {
        String[] queries = {"good","lucene"};
        String[] fields = {"title","desc"};
        BooleanClause.Occur[] clauses = {BooleanClause.Occur.SHOULD, BooleanClause.Occur.SHOULD};
        Query query = MultiFieldQueryParser.parse(queries,fields,clauses,new StandardAnalyzer());
        executeQuery(query);
    }

    /**
     * 通配符查询
     * 星号*:代表0个或多个字母
     * 问号?:代表0个或1个字母
     */
    @Test
    public void wildcardQuery() throws IOException {
        String field = "title";
//        String queryStr = "*pute?";
        String queryStr = "act*";
        Term term = new Term(field,queryStr);
        Query query = new WildcardQuery(term);
        executeQuery(query);
    }

    /**
     * 前缀查询
     * 自动在关键词末尾添加*
     */
    @Test
    public void prefixQuery() throws IOException {
        String field = "title";
        String queryStr = "act"; //act*
        Term term = new Term(field,queryStr);
        Query query = new PrefixQuery(term);
        executeQuery(query);
    }

    /**
     * http://blog.csdn.net/rick_123/article/details/6708527
     * 短语查询,对关键词加引号,通过位置移动来匹配
     * slop的概念：slop是指两个项的位置之间允许的最大间隔距离
     * 例如:slop设置为1,则 quick brown fox 可以匹配 quick fox
     */
    @Test
    public void phraseQuery() throws IOException {
        Query query = new PhraseQuery.Builder()
                .setSlop(3)
                .add(new Term("title", "computer"))
                .add(new Term("title","art"))
                .build();
        executeQuery(query);
    }

    /**
     * http://callan.iteye.com/blog/154251
     * 跨度查询,用于查询多个词的时候考虑几个词在文档中的匹配位置
     * 与phraseQuery和multiFieldQuery很相似,都是通过位置限制匹配
     * 但是spanQuery更加灵活
     *
     * SpanQuery包括以下几种：
     * SpanTermQuery：词距查询的基础，结果和TermQuery相似，只不过是增加了查询结果中单词的距离信息。
     * SpanFirstQuery：在指定距离可以找到第一个单词的查询。
     * SpanNearQuery：查询的几个语句之间保持者一定的距离。
     * SpanOrQuery：同时查询几个词句查询。
     * SpanNotQuery：从一个词距查询结果中，去除一个词距查询。
     */
    @Test
    public void spanQuery() throws IOException {
        SpanTermQuery query = new SpanTermQuery(new Term("title","art"));
        executeQuery(query);
    }

    /**
     * 第一次出现在指定位置
     * @throws IOException
     */
    @Test
    public void spanFirstQuery() throws IOException {
        SpanTermQuery query = new SpanTermQuery(new Term("title","art"));
        SpanFirstQuery spanFirstQuery =new SpanFirstQuery(query,2); //出现在第2个位置
        executeQuery(spanFirstQuery);
    }

    /**
     * SpanNearQuery中将SpanTermQuery对象作为SpanQuery对象使用的效果，与使用PharseQuery的效果非常相似。
     * 最大的区别是：在SpanNearQuery的构造函数中的第三个参数为inOrder标志，设置这个标志为true，项添加的顺序和其文档中出现的顺序相同
     */
    @Test
    public void spanNearQuery() throws IOException {
        SpanTermQuery queryScience = new SpanTermQuery(new Term("title","science"));
        SpanTermQuery queryArt = new SpanTermQuery(new Term("title","art"));
        SpanQuery[] queries = new SpanQuery[]{queryScience,queryArt};
        int slop = 2;//science 与 art两个词间隔在2以内
        boolean inOrder = false;//不需要按数组中的顺序出现在文档中
        SpanNearQuery query = new SpanNearQuery(queries,slop,inOrder);
        executeQuery(query);
    }

    @Test
    public void spanOrQuery() throws IOException {
        SpanTermQuery queryScience = new SpanTermQuery(new Term("title","science"));
        SpanTermQuery queryArt = new SpanTermQuery(new Term("title","art"));
        SpanQuery[] queries = new SpanQuery[]{queryScience,queryArt};
        int slop = 2;//science 与 art两个词间隔在2以内
        boolean inOrder = false;//不需要按数组中的顺序出现在文档中
        SpanNearQuery spanNearQuery = new SpanNearQuery(queries,slop,inOrder);

        SpanTermQuery queryComputer = new SpanTermQuery(new Term("title","lucene"));

        SpanOrQuery query = new SpanOrQuery(new SpanQuery[]{spanNearQuery,queryComputer});
        executeQuery(query);
    }

    /**
     * 组合查询
     * MUST与MUST组合表示并集
     * MUST与MUST_NOT表示包含与不包含
     * MUST_NOT与MUST_NOT组合没有意义
     * SHOULD与SHOULD组合表示或
     * SHOULD与MUST表示MUST,其中SHOULD没有任何价值
     * SHOULD与MUST_NOT相当于MUST与MUST_NOT表示包含与不包含
     */
    @Test
    public void booleanQuery() throws IOException {
        TermQuery queryComputerInTitle = new TermQuery(new Term("title","computer"));
        TermQuery queryGoodInDesc = new TermQuery(new Term("desc","good"));

        BooleanQuery booleanQuery = new BooleanQuery.Builder()
                .add(queryComputerInTitle,BooleanClause.Occur.SHOULD)
                .add(queryGoodInDesc,BooleanClause.Occur.SHOULD)
                .setMinimumNumberShouldMatch(1)
                .build();
        executeQuery(booleanQuery);
    }


}
