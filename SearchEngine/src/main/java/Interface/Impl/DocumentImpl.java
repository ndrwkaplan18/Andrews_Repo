package Interface.Impl;

import Interface.Document;
import Interface.DocumentStore;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static java.lang.System.currentTimeMillis;

public class DocumentImpl implements Document {

    protected byte[] compressedDoc;
    protected URI docURI;
    protected int hashCode;
    protected DocumentStore.CompressionFormat compressionFormat;
    protected HashMap<String, Integer> wordMap;
    protected long lastUseTime;

    public DocumentImpl(byte[] compressedDoc, URI docURI, int hashCode, DocumentStore.CompressionFormat compressionFormat, HashMap<String,Integer> wordMap){
        this.compressedDoc = compressedDoc;
        this.docURI = docURI;
        this.hashCode = hashCode;
        this.compressionFormat = compressionFormat;
        this.wordMap = wordMap;
        this.lastUseTime = currentTimeMillis();
    }

    public byte[] getDocument() {
        return compressedDoc;
    }


    public int getDocumentHashCode() {
        return hashCode;
    }


    public URI getKey() {
        return docURI;
    }


    public DocumentStore.CompressionFormat getCompressionFormat() {
        return compressionFormat;
    }

    public long getLastUseTime() { return lastUseTime; }

    public void setLastUseTime(long timeInMilliseconds) { lastUseTime = timeInMilliseconds; }

    /**
     * how many times does the given word appear in the document?
     *
     * @param word
     * @return
     */
    @Override
    public int wordCount(String word) { return wordMap.get(word); }

    public Map<String, Integer> getWordMap() {
        return wordMap;
    }

    public void setWordMap(Map<String, Integer> wordMap) {
        this.wordMap = (HashMap<String, Integer>) wordMap;
    }

    @Override
    public int hashCode(){
        return hashCode;
    }

    @Override
    public boolean equals(Object that){
        if(this == that){
            return true;
        }
        if(that == null){
            return false;
        }
        if(getClass() != that.getClass()){
            return false;
        }
        DocumentImpl otherDoc = (DocumentImpl) that;
        return docURI.equals(otherDoc.getKey());
    }

    @Override
    public String toString(){return docURI.toString(); }

    public int compareTo(Document o) {
        long t2 = o.getLastUseTime();
        int sub = (int) (lastUseTime - t2);
        return sub;
    }
}
