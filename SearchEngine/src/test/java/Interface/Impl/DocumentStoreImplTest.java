package Interface.Impl;

import Interface.Command;
import Interface.Impl.DocumentStoreImpl;
import Interface.DocumentStore;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class DocumentStoreImplTest {

    DocumentStoreImpl docStore = new DocumentStoreImpl(new File("C:/Users/ndrwk/Desktop/GIT/DataStructures/project"));
    URI[] keys;
    String[] docs;
    DocumentStore.CompressionFormat[] formats;

    @Before
    public void setup(){
        try{
            keys = new URI[]{
                    new URI("dsi/doc0"),
                    new URI("dsi/doc1"),
                    new URI("dsi/doc2"),
                    new URI("dsi/doc3"),
                    new URI("dsi/doc4"),
                    new URI("dsi/doc5"),
                    new URI("dsi/doc6"),
                    new URI("dsi/doc7"),
                    new URI("dsi/doc8"),
                    new URI("dsi/doc9"),
                    new URI("dsi/doc10"),
                    new URI("dsi/doc11")
            };
        }catch(URISyntaxException e){
        }
        docs = new String[]{//https://www.shopify.com/partners/blog/79940998-15-funny-lorem-ipsum-generators-to-shake-up-your-design-mockups
                "Don't underestimate the Force. bump bump bump bump Partially, but it also obeys your commands. Escape is not his plan. I must face him, alone. a a I don't know what you're talking about. I am a member of the Imperial Senate on a diplomatic mission to Alderaan--",
                "Obi-Wan is here. The Force is with bump bump bump him. I'm surprised you had the courage to take the responsibility yourself. a a She must have hidden the plans in the escape pod. Send a detachment down to retrieve them, and see to it personally, Commander. There'll be no one to stop us this time!",
                "On second thoughts, let's not go bump bump there. It is a silly place. Bring her forward! Who's that then? The Lady a of the Lake, her arm clad in the purest shimmering samite, held aloft Excalibur from the bosom of the water, signifying by divine providence that I, Arthur, was to carry Excalibur. That is why I am your king.",
                "I don't like being bump outdoors, Smithers. For one thing, there's too many fat children. Fat Tony is a cancer on this fair city! He is the cancer and I am the uh what cures cancer? I've had it with this school, Skinner. Low test scores, class after class of ugly, ugly children",
                "Beer. Now there's a temporary solution. How could you?! Haven't you learned anything from that guy who gives those sermons at church? Captain Whatshisname? We live in a society of laws! Why do you think I took you to all those Police Academy movies? For fun? Well, I didn't hear anybody laughing, did you? Except at that guy who made sound effects. Makes sound effects and laughs. Where was I? Oh yeah! Stay out of my booze.",
                "It must be wonderful. And when we woke up, we had these bodies. Hey! I'm a porno-dealing monster, what do I care what you think? Well, then good news! It's a suppository. How much did you make me? Bender! Ship! Stop bickering or I'm going to come back there and change your opinions manually!",
                "Oh yeah, good luck with that. But I've never been to a moon! I've been there. My folks were always on me to groom myself and wear underpants. What am I, the pope? You're going to do his laundry? Now that the, uh, garbage ball is in space, Doctor, perhaps you can help me with my sexual inhibitions?",
                "I've opened a door here that I regret. Now, when you do this without getting punched in the chest, you'll have more fun. That's what it said on 'Ask Jeeves.' But I bought a yearbook ad from you, doesn't that mean anything anymore?",
                "t6436..:*!!he th12e th2e the3 th3452e t132he t12323he down on corner out on street",
                "the the the about time yall got in yaherrrd me",
                "the the im just a poor boy nobody loves me because im easy come easy go little high little low",
                "the the the the mmbob oo oo ooobob doo doo do"
        };

        formats = new DocumentStore.CompressionFormat[]{
                DocumentStore.CompressionFormat.ZIP,
                DocumentStore.CompressionFormat.GZIP,
                DocumentStore.CompressionFormat.BZIP2,
                DocumentStore.CompressionFormat.SEVENZ,
        };

        for(int i = 0; i <= 3; i ++) {
            byte[] doc = docs[i].getBytes();
            ByteArrayInputStream in = new ByteArrayInputStream(doc);
            docStore.putDocument(in, keys[i], formats[i%formats.length]);
            sleep();
        }//At setup there are now 4 documents in the store
    }

    @Test
    public void putDocument() {
        assertEquals(4,docStore.commandStack.size());
        int docCount = docStore.currentDocCount;
        int byteCount = docStore.currentByteCount;

        for(int i = 4; i <= 5; i ++) {
            byte[] doc = docs[i].getBytes();
            ByteArrayInputStream in = new ByteArrayInputStream(doc);
            //test successful bTree insert
            assertEquals(docStore.putDocument(in, keys[i], formats[i%formats.length]),docs[i].hashCode());
            docCount++;
            byteCount += docStore.bTree.get(keys[i]).compressedDoc.length;
            sleep();
        }
        //test successful command push on the stack
        assertEquals(6,docStore.commandStack.size());
        //test successful currentByteCount & currentDocCount update
        assertEquals(docCount,docStore.currentDocCount);
        assertEquals(byteCount,docStore.currentByteCount);
        //wordmap in the trie
        //successful minheap insert
        //at this point the min should be doc0
        assertTrue(keys[0].equals(docStore.minHeap.elements[1]));
    }

    @Test
    public void putDocument1() {
        for(int i = 6; i <= 7; i++) {
            byte[] doc = docs[i].getBytes();
            ByteArrayInputStream in = new ByteArrayInputStream(doc);
            docStore.putDocument(in, keys[i]);
        }
    }

    @Test
    public void getDocument() {
        assertEquals(docStore.getDocument(keys[0]),docs[0]);
        //now doc1 should be the min
        assertTrue(keys[1].equals(docStore.minHeap.elements[1]));
    }

    @Test
    public void getCompressedDocument() {
        for(int i = 3; i >= 0; i --) {
            byte[] cd = docStore.getCompressedDocument(keys[i]);
            byte[] res = docStore.decompress(cd, formats[i%formats.length]);
            String orig = new String(res);
            assertEquals(orig, docs[i]);
            sleep();
        }
        //because these docs were accessed in reverse order, doc3 should be the min
        assertTrue(keys[3].equals(docStore.minHeap.elements[1]));
    }

    @Test
    public void deleteDocument() {
        assertEquals(4,docStore.commandStack.size());
        DocumentImpl doc = docStore.bTree.get(keys[0]);
        docStore.deleteDocument(keys[0]);
        assertEquals(null,docStore.bTree.get(keys[0]));
        assertEquals(5,docStore.commandStack.size());
        //should have been deleted from the heap
        //assertEquals(null,docStore.minHeap.getArrayIndex(doc));
    }

    @Test
    public void undo(){
        byte[] doc = docs[6].getBytes();
        ByteArrayInputStream in = new ByteArrayInputStream(doc);
        docStore.putDocument(in, keys[6]);
        assertEquals(docs[6],docStore.getDocument(keys[6]));
        docStore.undo();
        assertEquals(null,docStore.getDocument(keys[6]));
    }

    @Test
    public void undo2(){
        //the last action in setup was to add the 4th document
        Command c = docStore.commandStack.peek();
        c.undo();
        assertEquals(null,docStore.getDocument(keys[3]));
        c.redo();
        assertEquals(docs[3],docStore.getDocument(keys[3]));
    }

    @Test
    public void undoURI(){
        assertEquals(docs[0],docStore.getDocument(keys[0]));
        docStore.undo(keys[0]);
        assertEquals(null,docStore.getDocument(keys[0]));
        assertEquals(docs[3],docStore.getDocument(keys[3]));
    }

    @Test
    public void undoReplace(){
        byte[] doc = docs[4].getBytes();
        ByteArrayInputStream in = new ByteArrayInputStream(doc);
        docStore.putDocument(in, keys[3]);//replaced doc[3] with doc[4]
        assertEquals(docs[4],docStore.getDocument(keys[3]));
        docStore.undo();
        assertEquals(docs[3],docStore.getDocument(keys[3]));
    }

    @Test
    public void undoReplaceAtUri(){
        //first put a doc in
        byte[] doc = docs[0].getBytes();
        ByteArrayInputStream in = new ByteArrayInputStream(doc);
        docStore.putDocument(in, keys[4]);

        for(int i = 4; i <= 7; i++) {//Now replace that doc and do a few other actions
            byte[] doc1 = docs[i].getBytes();
            ByteArrayInputStream in1 = new ByteArrayInputStream(doc1);
            docStore.putDocument(in1, keys[i]);
        }

        assertEquals(docs[4],docStore.getDocument(keys[4]));
        docStore.undo(keys[4]);
        assertEquals(docs[0],docStore.getDocument(keys[4]));
    }

    @Test
    public void undoDelete(){
        docStore.deleteDocument(keys[3]);
        assertEquals(null,docStore.getDocument(keys[3]));
        docStore.undo();
        assertEquals(docs[3],docStore.getDocument(keys[3]));
    }

    @Test
    public void undoDeleteAtUri(){
        //first put a doc in
        byte[] doc = docs[4].getBytes();
        ByteArrayInputStream in = new ByteArrayInputStream(doc);
        docStore.putDocument(in, keys[4]);
        docStore.deleteDocument(keys[4]);
        for(int i = 5; i <= 7; i++) {//Now replace that doc and do a few other actions
            byte[] doc1 = docs[i].getBytes();
            ByteArrayInputStream in1 = new ByteArrayInputStream(doc1);
            docStore.putDocument(in1, keys[i]);
        }

        assertEquals(null,docStore.getDocument(keys[4]));
        docStore.undo(keys[4]);
        assertEquals(docs[4],docStore.getDocument(keys[4]));
    }


    @Test
    public void doesSearchWork(){
        //add test docs
        List<String> expected = new ArrayList<>();
        for(int i = 0; i <= 3; i++){
            expected.add(docs[i]);
        }
        List<String> actual = docStore.search("bump");
        assertEquals(expected, actual);

        List<byte[]> expected2 = new ArrayList<>();
        for(int i = 0; i <= 3; i++){
            expected2.add(docStore.bTree.get(keys[i]).compressedDoc);
        }
        List<byte[]> actual2 = docStore.searchCompressed("bump");
        assertEquals(expected2, actual2);
    }

    @Test
    public void doesSetMaxDocCountWork(){
        docStore.setMaxDocumentCount(4);
        byte[] doc = docs[4].getBytes();
        ByteArrayInputStream in = new ByteArrayInputStream(doc);
        docStore.putDocument(in, keys[4], formats[4%formats.length]);
        //assertEquals(null,docStore.getDocument(keys[0]));
        assertFalse(docStore.minHeap.elementsToArrayIndex.containsKey(keys[0]));
        byte[] doc2 = docs[5].getBytes();
        ByteArrayInputStream in2 = new ByteArrayInputStream(doc2);
        docStore.putDocument(in2, keys[5], formats[5%formats.length]);
        //assertEquals(null,docStore.getDocument(keys[1]));
        assertFalse(docStore.minHeap.elementsToArrayIndex.containsKey(keys[1]));
    }

    @Test
    public void doesSetMaxByteCountWork(){
        int[] docSizes = new int[12];
        for(int i = 0; i < docStore.currentDocCount; i++) {
            docSizes[i] = docStore.bTree.get(keys[i]).compressedDoc.length;
        }
        docStore.setMaxDocumentBytes(IntStream.of(docSizes).sum());
        byte[] doc = docs[4].getBytes();
        ByteArrayInputStream in = new ByteArrayInputStream(doc);
        docStore.putDocument(in, keys[4], formats[4%formats.length]);
        docSizes[4] = docStore.bTree.get(keys[4]).compressedDoc.length;
        assertFalse(docStore.minHeap.elementsToArrayIndex.containsKey(keys[0]));
        assertFalse(docStore.minHeap.elementsToArrayIndex.containsKey(keys[1]));
    }

    @Test
    public void deleteMinAfterUsingReHeapify(){
        docStore.setMaxDocumentCount(5);//set max
        byte[] doc = docs[4].getBytes();
        ByteArrayInputStream in = new ByteArrayInputStream(doc);
        docStore.putDocument(in, keys[4], formats[4%formats.length]);//add doc
        List<String> expected = new ArrayList<>();
        for(int i = 0; i <= 3; i++){
            expected.add(docs[i]);
        }
        sleep();
        List<String> actual = docStore.search("bump");//search docs 0-3, now most recent put is min
        assertEquals(expected, actual);
        byte[] doc2 = docs[5].getBytes();
        ByteArrayInputStream in2 = new ByteArrayInputStream(doc2);
        docStore.putDocument(in2, keys[5], formats[5%formats.length]);
        assertFalse(docStore.minHeap.elementsToArrayIndex.containsKey(keys[4]));
        //docStore.undo(keys[4]);
    }

    @Test
    public void decompressDeserializedDoc(){
        docStore.setMaxDocumentCount(4);
        byte[] doc = docs[4].getBytes();
        ByteArrayInputStream in = new ByteArrayInputStream(doc);
        docStore.putDocument(in, keys[4], formats[4%formats.length]);//add doc
        assertEquals(docs[0],docStore.getDocument(keys[0]));
    }

    @Test
    public void multipleUndos(){
        docStore.setMaxDocumentCount(4);
        byte[] doc = docs[4].getBytes();
        ByteArrayInputStream in = new ByteArrayInputStream(doc);
        docStore.putDocument(in, keys[4], formats[4%formats.length]);//doc0 pushed out of memory
        byte[] doc2 = docs[5].getBytes();
        ByteArrayInputStream in2 = new ByteArrayInputStream(doc2);
        docStore.putDocument(in2, keys[0], formats[5%formats.length]);//doc0 overwritten
        docStore.undo(keys[4]);//Delete doc4
        docStore.deleteDocument(keys[0]);//Delete doc5
        docStore.undo();//Restore doc5
        docStore.undo(keys[0]);//Restore doc0
        assertEquals(docs[0],docStore.getDocument(keys[0]));
    }

    @Test
    public void multiplePutsDocLimit(){
        DocumentStoreImpl dsi = new DocumentStoreImpl();
        dsi.setMaxDocumentCount(3);
        for(int i = 0; i <= 5; i ++) {
            byte[] doc = docs[i].getBytes();
            ByteArrayInputStream in = new ByteArrayInputStream(doc);
            dsi.putDocument(in, keys[i], formats[i%formats.length]);
            sleep();
        }
        dsi.deleteDocument(keys[1]);//delete doc1


        byte[] doc = docs[6].getBytes();
        ByteArrayInputStream in = new ByteArrayInputStream(doc);
        dsi.putDocument(in, keys[1], formats[6%formats.length]);
        assertEquals(docs[6],dsi.getDocument(keys[1]));
        sleep();

        doc = docs[7].getBytes();
        in = new ByteArrayInputStream(doc);
        dsi.putDocument(in, keys[1], formats[7%formats.length]);
        assertEquals(docs[7],dsi.getDocument(keys[1]));
        sleep();

        doc = docs[8].getBytes();
        in = new ByteArrayInputStream(doc);
        dsi.putDocument(in, keys[1], formats[8%formats.length]);
        assertEquals(docs[8],dsi.getDocument(keys[1]));
        sleep();

        doc = docs[9].getBytes();
        in = new ByteArrayInputStream(doc);
        dsi.putDocument(in, keys[1], formats[9%formats.length]);
        assertEquals(docs[9],dsi.getDocument(keys[1]));
        sleep();

        doc = docs[10].getBytes();
        in = new ByteArrayInputStream(doc);
        dsi.putDocument(in, keys[1], formats[10%formats.length]);
        assertEquals(docs[10],dsi.getDocument(keys[1]));
        sleep();

        doc = docs[11].getBytes();
        in = new ByteArrayInputStream(doc);
        dsi.putDocument(in, keys[1], formats[11%formats.length]);
        assertEquals(docs[11],dsi.getDocument(keys[1]));
        sleep();

        dsi.undo();
        assertEquals(docs[10],dsi.getDocument(keys[1]));

        dsi.undo();
        assertEquals(docs[9],dsi.getDocument(keys[1]));

        dsi.undo();
        assertEquals(docs[8],dsi.getDocument(keys[1]));

        dsi.undo();
        assertEquals(docs[7],dsi.getDocument(keys[1]));

        dsi.undo();
        assertEquals(docs[6],dsi.getDocument(keys[1]));

        dsi.undo();
        assertNull(dsi.getDocument(keys[1]));

        dsi.undo();
        assertEquals(docs[1],dsi.getDocument(keys[1]));
    }

    @Test
    public void multiplePutsDocLimit2(){
        DocumentStoreImpl dsi = new DocumentStoreImpl();
        dsi.setMaxDocumentCount(3);
        for(int i = 0; i <= 5; i ++) {
            byte[] doc = docs[i].getBytes();
            ByteArrayInputStream in = new ByteArrayInputStream(doc);
            dsi.putDocument(in, keys[i], formats[i%formats.length]);
            sleep();
        }
        dsi.deleteDocument(keys[1]);//delete doc1


        byte[] doc = docs[6].getBytes();
        ByteArrayInputStream in = new ByteArrayInputStream(doc);
        dsi.putDocument(in, keys[6], formats[6%formats.length]);
        assertEquals(docs[6],dsi.getDocument(keys[6]));
        sleep();

        doc = docs[7].getBytes();
        in = new ByteArrayInputStream(doc);
        dsi.putDocument(in, keys[1], formats[7%formats.length]);
        assertEquals(docs[7],dsi.getDocument(keys[1]));
        sleep();

        doc = docs[8].getBytes();
        in = new ByteArrayInputStream(doc);
        dsi.putDocument(in, keys[8], formats[8%formats.length]);
        assertEquals(docs[8],dsi.getDocument(keys[8]));
        sleep();

        doc = docs[9].getBytes();
        in = new ByteArrayInputStream(doc);
        dsi.putDocument(in, keys[1], formats[9%formats.length]);
        assertEquals(docs[9],dsi.getDocument(keys[1]));
        sleep();

        doc = docs[10].getBytes();
        in = new ByteArrayInputStream(doc);
        dsi.putDocument(in, keys[10], formats[10%formats.length]);
        assertEquals(docs[10],dsi.getDocument(keys[10]));
        sleep();

        doc = docs[11].getBytes();
        in = new ByteArrayInputStream(doc);
        dsi.putDocument(in, keys[11], formats[11%formats.length]);
        assertEquals(docs[11],dsi.getDocument(keys[11]));
        sleep();

        dsi.undo(keys[1]);
        assertEquals(docs[7],dsi.getDocument(keys[1]));

        dsi.undo();
        assertNull(dsi.getDocument(keys[11]));

        dsi.undo(keys[1]);
        assertNull(dsi.getDocument(keys[1]));

        dsi.undo(keys[1]);
        assertEquals(docs[1],dsi.getDocument(keys[1]));
    }

    private void sleep(){
        try{
            Thread.sleep(1);
        }
        catch(InterruptedException e){
            e.printStackTrace();
        }
    }

}