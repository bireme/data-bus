package org.bireme.barr;

import java.io.File;
import java.io.IOException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;
import org.apache.lucene.util.Version;

/**
 *
 * @author Heitor Barbieri
 * date 20130416
 */
public class ProcessDAOLucene implements ProcessDAO {
    static final Version matchVersion = Version.LUCENE_42;
    static final int MAX_OUTPUT_LEN = 256;
    
    private static final String ID_TAG = "tag";
    private static final String COMMAND_TAG = "command";
    private static final String STATUS_TAG = "status";
    private static final String EXITCODE_TAG = "exitcode";
    private static final String OUTPUT_TAG = "output";
    
    final Analyzer analyzer;
    final Directory directory;
    final IndexWriter iwriter;    
    final FieldType ftypeIS;
    
    public ProcessDAOLucene(final String lucDir) throws IOException {
        if (lucDir == null) {
            throw new NullPointerException("data base name");
        }        
        analyzer = new StandardAnalyzer(matchVersion);
        directory = new MMapDirectory(new File(lucDir));
        
        final IndexWriterConfig config = 
                new IndexWriterConfig(matchVersion, analyzer)
                    .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        iwriter = new IndexWriter(directory, config);
        
        ftypeIS = new FieldType();                        
        ftypeIS.setStored(true);
        ftypeIS.setIndexed(false);        
    }
    
    @Override
    public void close() throws Exception {
        iwriter.close();
        analyzer.close();
        directory.close();
    }

    @Override
    public void insertResult(final long id, 
                              final String command, 
                              final String status, 
                              final int exitCode, 
                              final String output) throws Exception {
        if (command == null) {
            throw new NullPointerException("command");
        }
        if (status == null) {
            throw new NullPointerException("status");
        }
        final String out = trimOutput(output);
        final Document doc = new Document();
        
        doc.add(new LongField(ID_TAG, id, Field.Store.YES));
        doc.add(new Field(COMMAND_TAG, command, ftypeIS));
        doc.add(new Field(STATUS_TAG, status, ftypeIS));
        doc.add(new IntField(EXITCODE_TAG, exitCode, Field.Store.YES));
        doc.add(new Field(OUTPUT_TAG, out, ftypeIS));
        
        iwriter.addDocument(doc);
        iwriter.commit();
    }

    @Override
    public ProcessResult retrieveResult(long id) throws Exception {    
        ProcessResult result = null;
        
        final BytesRef bytes = new BytesRef(NumericUtils.BUF_SIZE_LONG);
        NumericUtils.longToPrefixCoded(id, 0, bytes);
        final Term term = new Term(ID_TAG, bytes);
        final Query query = new TermQuery(term);
            
        try (DirectoryReader ireader = DirectoryReader.open(directory)) {
            final IndexSearcher isearcher = new IndexSearcher(ireader);            
            final ScoreDoc[] hits = isearcher.search(query, null, 1).scoreDocs;
            
            if (hits.length > 0) {
                final Document hitDoc = isearcher.doc(hits[0].doc);

                result = new ProcessResult(
                    hitDoc.getField(ID_TAG).numericValue().longValue(),
                    hitDoc.getField(COMMAND_TAG).stringValue(),
                    hitDoc.getField(STATUS_TAG).stringValue(),
                    hitDoc.getField(EXITCODE_TAG).numericValue().intValue(),      
                    hitDoc.getField(OUTPUT_TAG).stringValue());      
            }
        }
        
        return result;
    }

    @Override
    public void updateResult(final long id, 
                              final String command, 
                              final String status, 
                              final int exitCode, 
                              final String output) throws Exception {
        if (command == null) {
            throw new NullPointerException("command");
        }
        if (status == null) {
            throw new NullPointerException("status");
        }
        final String out = trimOutput(output);
        final BytesRef bytes = new BytesRef(NumericUtils.BUF_SIZE_LONG);
        NumericUtils.longToPrefixCoded(id, 0, bytes);
        final Term term = new Term(ID_TAG, bytes);
        final Document doc = new Document();
               
        doc.add(new LongField(ID_TAG, id, Field.Store.YES));
        doc.add(new Field(COMMAND_TAG, command, ftypeIS));
        doc.add(new Field(STATUS_TAG, status, ftypeIS));
        doc.add(new IntField(EXITCODE_TAG, exitCode, Field.Store.YES));
        doc.add(new Field(OUTPUT_TAG, out, ftypeIS));
        
        iwriter.updateDocument(term, doc);
        iwriter.commit();
    } 
    
    private String trimOutput(final String output) {
        final String out;
        
        if (output == null) {
            out = "";
        } else {
            if (output.length() > MAX_OUTPUT_LEN) {
                final StringBuilder builder = 
                                          new StringBuilder(MAX_OUTPUT_LEN + 1);
                builder.append(output.substring(0, MAX_OUTPUT_LEN - 5));
                builder.append("[...]");
                out = builder.toString();
            } else {
                out = output;
            }
        }
        
        return out;
    }
}