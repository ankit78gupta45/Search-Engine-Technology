package cecs429.documents;

/**
 * Represents a collection of documents used to build an index.
 */
public interface DocumentCorpus {
	/**
	 * Gets all documents in the corpus.
	 */
	Iterable<Document> getDocuments();

	/**
	 * The number of documents in the corpus.
	 */
	int getCorpusSize();

	/**
	 * Returns the document with the given document ID.
	 */
	Document getDocument(int id);

	/**
	 * Returns the name of corpus.
	 */
	String getCorpusName();

	/**
	 * Returns the path of corpus.
	 */
	String getCorpusPath();
}
