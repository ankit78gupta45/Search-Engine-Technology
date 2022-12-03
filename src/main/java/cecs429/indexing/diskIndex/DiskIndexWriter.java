package cecs429.indexing.diskIndex;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.List;

import cecs429.indexing.Index;
import cecs429.indexing.Posting;
import cecs429.indexing.database.TermPositionCrud;
import cecs429.indexing.database.TermPositionModel;

public class DiskIndexWriter {
    private Index positionalInvertedIndex;
    private Index biwordInvertedIndex;
    private String diskDirectoryPath;

    private final int MAXIMUM_BATCH_LIMIT = 1000;

    TermPositionCrud termPositionCrud;
    TermPositionModel termPositionModel;

    public DiskIndexWriter() {
    }

    public void setPositionalIndex(Index positionalInvertedIndex, String diskDirectoryPath) {
        this.positionalInvertedIndex = positionalInvertedIndex;
        this.diskDirectoryPath = diskDirectoryPath;
    }

    public void setBiwordIndex(Index biwordInvertedIndex, String diskDirectoryPath) {
        this.biwordInvertedIndex = biwordInvertedIndex;
        this.diskDirectoryPath = diskDirectoryPath;
    }

    public void writeIndex() throws SQLException {
        try {
            long startTime = System.currentTimeMillis();
            System.out.println("Disk Indexing...");
            termPositionCrud = new TermPositionCrud(DiskIndexEnum.POSITIONAL_INDEX.getDbIndexFileName());
            termPositionCrud.openConnection();
            termPositionCrud.createTable();

            termPositionModel = new TermPositionModel();

            RandomAccessFile raf = new RandomAccessFile(
                    diskDirectoryPath + DiskIndexEnum.POSITIONAL_INDEX.getIndexFileName(), "rw");
            raf.seek(0);

            List<String> vocab = positionalInvertedIndex.getVocabulary();
            int vocabCount = 0;
            int flag = 0;
            for (String term : vocab) {

                if (vocabCount % MAXIMUM_BATCH_LIMIT == 0) {
                    if (flag == 1)
                        termPositionCrud.executeInsertBatch();
                    flag = 1;
                    termPositionCrud.initializePreparestatement();
                } 
                
                termPositionCrud.add(term, raf.getChannel().position());

                List<Posting> postings = positionalInvertedIndex.getPostings(term);
                byte[] docFreqterm = ByteBuffer.allocate(4).putInt(postings.size()).array();
                raf.write(docFreqterm, 0, docFreqterm.length);

                int lastDocId = 0;
                for (Posting p : postings) {
                    int docId = p.getDocumentId();

                    // docId - lastDocId includes a gap
                    byte[] docIdBytes = ByteBuffer.allocate(4).putInt(docId - lastDocId).array();
                    raf.write(docIdBytes, 0, docIdBytes.length);

                    List<Integer> positions = p.getPositions();
                    int termFrequency = positions.size();
                    byte[] termFreqBytes = ByteBuffer.allocate(4).putInt(termFrequency).array();
                    raf.write(termFreqBytes);

                    int lastPos = 0;
                    for (int pos : positions) {
                        byte[] posBytes = ByteBuffer.allocate(4).putInt(pos - lastPos).array();
                        raf.write(posBytes, 0, posBytes.length);
                        lastPos = pos;
                    }
                    lastDocId = docId;
                }
                ++vocabCount;
            }

            termPositionCrud.executeInsertBatch();
            termPositionCrud.sqlCommit();

            long endTime = System.currentTimeMillis(); // End time to build positional Inverted Index

            System.out.println("Time taken to write disk positional index: " + ((endTime - startTime) / 1000)
                    + " seconds");

            raf.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void writeBiwordIndex() throws SQLException {
        try {
            long startTime = System.currentTimeMillis();
            System.out.println("Biword Disk Indexing...");

            termPositionCrud = new TermPositionCrud(DiskIndexEnum.BIWORD_INDEX.getDbIndexFileName());

            termPositionCrud.openConnection();
            termPositionCrud.createTable();

            termPositionModel = new TermPositionModel();

            RandomAccessFile raf = new RandomAccessFile(
                    diskDirectoryPath + DiskIndexEnum.BIWORD_INDEX.getIndexFileName(), "rw");
            raf.seek(0);

            List<String> vocab = biwordInvertedIndex.getVocabulary();
            int vocabCount = 0;
            int flag = 0;
            for (String term : vocab) {

                if (vocabCount % MAXIMUM_BATCH_LIMIT == 0) {
                    if (flag == 1)
                        termPositionCrud.executeInsertBatch();
                    flag = 1;
                    termPositionCrud.initializePreparestatement();
                } 
                
                termPositionCrud.add(term, raf.getChannel().position());

                List<Posting> postings = biwordInvertedIndex.getPostings(term);
                byte[] docFreqterm = ByteBuffer.allocate(4).putInt(postings.size()).array();
                raf.write(docFreqterm, 0, docFreqterm.length);

                int lastDocId = 0;
                for (Posting p : postings) {
                    int docId = p.getDocumentId();

                    // docId - lastDocId includes a gap
                    byte[] docIdBytes = ByteBuffer.allocate(4).putInt(docId - lastDocId).array();
                    raf.write(docIdBytes, 0, docIdBytes.length);

                    lastDocId = docId;
                }
                ++vocabCount;
            }

            termPositionCrud.executeInsertBatch();
            termPositionCrud.sqlCommit();

            long endTime = System.currentTimeMillis(); // End time to build positional Inverted Index

            System.out.println("Time taken to write disk biword index: " + ((endTime - startTime) / 1000)
                    + " seconds");

            raf.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
