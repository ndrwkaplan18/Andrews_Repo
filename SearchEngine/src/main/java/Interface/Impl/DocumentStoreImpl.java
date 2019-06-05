package Interface.Impl;

import Interface.Command;
import Interface.Document;
import Interface.DocumentStore;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import static Interface.DocumentStore.CompressionFormat.ZIP;
import static java.lang.System.currentTimeMillis;

public class DocumentStoreImpl implements DocumentStore {

	protected final StackImpl<Command> commandStack = new StackImpl<>();
	protected final TrieImpl<URI> trie = new TrieImpl<>();
	protected final int DEFAULT_MINHEAP_ARRAY_SIZE = 21;
	protected final long MY_BIRTHDAY_TIMESTAMP = 797644741000L;
	protected MinHeapImpl<URI> minHeap;
	protected int maxDocCount, maxDocBytes, currentByteCount, currentDocCount;
	protected CompressionFormat compressionFormat = ZIP;
	protected File storageBaseDir;
	protected DocumentIOImpl documentIO;
	protected DocumentIOImpl docGraveyard;
	protected BTreeImpl<URI, DocumentImpl> bTree;
	protected HashMap<URI, Boolean> serialized = new HashMap<>();


	class DocumentComparator implements Comparator<URI>{

		String key;
		public DocumentComparator(String key){
			this.key = key;
		}

		@Override
		public int compare(URI o1, URI o2) {
			return bTree.get(o2).wordCount(key) - bTree.get(o1).wordCount(key);
		}
	}

	class UriComparator implements Comparator<URI>{

		@Override
		public int compare(URI o1, URI o2) {
			long lastUse1 = bTree.get(o1).getLastUseTime();
			long lastUse2 = bTree.get(o2).getLastUseTime();
			long sub = lastUse1 - lastUse2;
			if(sub == 0){ return (int) sub;}
			int result = (int)(sub/Math.abs(sub));
			return result;
		}
	}

	public DocumentStoreImpl(File storageBaseDir){
		File graveyard = new File(storageBaseDir, "/graveyard");
		//graveyard.deleteOnExit();
		this.storageBaseDir = storageBaseDir;
		this.documentIO = new DocumentIOImpl(this.storageBaseDir);
		this.docGraveyard = new DocumentIOImpl(graveyard);
		this.bTree = new BTreeImpl<>(this.documentIO);
		this.minHeap = new MinHeapImpl<>(new URI[DEFAULT_MINHEAP_ARRAY_SIZE],new UriComparator());
	}

	public DocumentStoreImpl()
	{
		File storageBaseDir = new File(System.getProperty("user.dir"));
		new DocumentStoreImpl(storageBaseDir);
		File graveyard = new File(storageBaseDir, "/graveyard");
		graveyard.deleteOnExit();
		this.storageBaseDir = storageBaseDir;
		this.documentIO = new DocumentIOImpl(this.storageBaseDir);
		this.docGraveyard = new DocumentIOImpl(graveyard);
		this.bTree = new BTreeImpl<>(this.documentIO);
		this.minHeap = new MinHeapImpl<>(new URI[DEFAULT_MINHEAP_ARRAY_SIZE],new UriComparator());
	}

	/**
	 * Retrieve all documents that contain the given key word.
	 * Documents are returned in sorted, in descending order, by the number of times the keyword appears in the document.
	 * Search is CASE INSENSITIVE.
	 *
	 * @param keyword
	 * @return
	 */
	@Override
	public List<String> search(String keyword) {
		trie.comparator = new DocumentComparator(keyword);
		List<URI> uris = trie.getAllSorted(removePunct(keyword));
		List<String> result = new ArrayList<>();
		if(uris == null){ return null; }
		for(URI uri: uris){
			if(serialized.get(uri)){
				serialized.put(uri,false);
				heapInsert(uri);
				documentIO.deserialize(uri);
			}
			result.add(new String(decompress(bTree.get(uri).getDocument(),bTree.get(uri).getCompressionFormat())));
			bTree.get(uri).setLastUseTime(currentTimeMillis());
			minHeap.reHeapify(uri);
		}
		return result;
	}

	/**
	 * Retrieve the compressed form of all documents that contain the given key word.
	 * Documents are returned in sorted, in descending order, by the number of times the keyword appears in the document
	 * Search is CASE INSENSITIVE.
	 *
	 * @param keyword
	 * @return
	 */
	@Override
	public List<byte[]> searchCompressed(String keyword) {
		List<URI> uris = trie.getAllSorted(removePunct(keyword));
		List<byte[]> result = new ArrayList<>();
		if(uris == null){ return null; }
		for(URI uri: uris){
			if(serialized.get(uri)){
				serialized.put(uri, false);
				heapInsert(uri);
				documentIO.deserialize(uri);
			}
			result.add(bTree.get(uri).getDocument());
			bTree.get(uri).setLastUseTime(currentTimeMillis());
			minHeap.reHeapify(uri);
		}
		return result;
	}

	/**
	 * specify which compression format should be used if none is specified on a putDocument,
	 * either because the two-argument putDocument method is used or the CompressionFormat is
	 * null in the three-argument version of putDocument
	 *
	 * @param format
	 */
	public void setDefaultCompressionFormat(CompressionFormat format) {
		this.compressionFormat = format;
	}

	/**
	 * since the user does not specify a compression format, use the default compression format
	 * @param input the document being put
	 * @param uri unique identifier for the document
	 * @return the hashcode of the document
	 */
	public int putDocument(InputStream input, URI uri) {
		return putDocument(input,uri,compressionFormat);
	}

	/**
	 * @param input the document being put
	 * @param uri unique identifier for the document
	 * @param format compression format to use for compressing this document
	 * @return the hashcode of the document
	 */
	public int putDocument(InputStream input, URI uri, CompressionFormat format) {
		String string;
		try { string = new String(IOUtils.toByteArray(input)); }
		catch (IOException e) { return -1; }
		int hashCode = string.hashCode();
		if(serialized.containsKey(uri) && serialized.get(uri)){
			bTree.getFromDisk(uri);
			serialized.put(uri, false);
		}
		if(bTree.get(uri)!=null) {
			if (bTree.get(uri).hashCode() == hashCode) return -1;
		}
		//Can't use the original InputStream because IOUtils.toByteArray() closed that stream
		byte[] stringAsByte = string.getBytes();
		ByteArrayInputStream is = new ByteArrayInputStream(stringAsByte);
		DocumentImpl prevVal = bTree.put(uri,new DocumentImpl(compress(is,format), uri, hashCode, format, countWords(string)));
		serialized.put(uri, false);
		File path = null;
		if(prevVal != null){//need to erase
			return overwritePut(prevVal, uri, hashCode);
		}
		putUndoLogic(null,uri);
		heapInsert(uri);
		for(String key: bTree.get(uri).wordMap.keySet()){//for each key in doc.wordMap, trie.put(key,doc)
			trie.put(key,uri);
		}
		return hashCode;
	}

	protected int overwritePut(DocumentImpl prevVal, URI uri, int hashCode){
		File path = docGraveyard.serialize(prevVal);
		currentByteCount -= prevVal.compressedDoc.length;
		putUndoLogic(path, uri);
		for(String key: prevVal.wordMap.keySet()){
			trie.delete(key,uri);
		}
		for(String key2: bTree.get(uri).wordMap.keySet()){
			trie.put(key2, uri);
		}
		updateMemory(uri);
		minHeap.reHeapify(uri);
		return hashCode;
	}

	protected HashMap<String, Integer> countWords(String docText){
		String[] words = parseString(docText);
		//Map each unique word to the # of times it appears
		HashMap<String, Integer> wordMap = new HashMap<>();
		for(int i = 0; i < words.length; i++) {
			if ((wordMap.get(words[i]) == null)) {
				wordMap.put(words[i], 1);
			} else {
				wordMap.put(words[i], wordMap.get(words[i]) + 1);
			}
		}
		return wordMap;
	}

	protected String[] parseString(String s){ return removePunct(s).split("\\s"); }

	protected String removePunct(String s){
		Pattern pattern = Pattern.compile("\\p{Punct}|\\p{Digit}");
		Matcher matcher = pattern.matcher(s);
		return matcher.replaceAll("").toLowerCase();
	}

	protected void putUndoLogic(File prevVal, URI uri){
		Function<URI, Boolean> undo = undoPut(prevVal,uri);
		Function<URI, Boolean> redo = redoPut(prevVal, uri);
		commandStack.push(new Command(uri, undo, redo));
	}
	protected Function<URI, Boolean> undoPut(File prevVal, URI uri){
		return (uri2) -> {
			if(serialized.get(uri2)){
				bTree.getFromDisk(uri2);
				serialized.put(uri2, false);
			}
			currentByteCount -= bTree.get(uri).compressedDoc.length;
			if(prevVal == null){
				docGraveyard.serialize(bTree.get(uri2));
				for(String key: bTree.get(uri).wordMap.keySet()){ trie.delete(key,uri2); }
				currentDocCount--;
				heapDelete(uri2);
				bTree.put(uri2,null);//Effectively deletes the put value
			}
			else { //prevVal goes into bTree, currentVal goes to graveyard
				Document prevDoc = docGraveyard.deserialize(uri2);
				//docGraveyard.serialize(bTree.get(uri2));// serialize currentDoc
				for(String key: bTree.get(uri2).wordMap.keySet()){//Delete undone put in trie
					trie.delete(key,uri2);
				}
				updateMemory(uri2);
				minHeap.reHeapify(uri2);
				bTree.put(uri2, prevDoc);
				for(String key: bTree.get(uri2).wordMap.keySet()){
					trie.put(key,uri2);
				}
				prevDoc = null; //Dereference prevDoc
			}
			return true;
		};
	}

	protected Function<URI, Boolean> redoPut(File prevVal, URI uri){
		return (uri2) -> {
			Document prevDoc = docGraveyard.deserialize(uri2);
			if(prevVal != null){
				for(String key: bTree.get(uri2).wordMap.keySet()){
					trie.delete(key,uri2);
				}
				if(serialized.get(uri2)){
					bTree.getFromDisk(uri2);
					serialized.put(uri2, false);
				}
                docGraveyard.serialize(bTree.get(uri2));
				heapDelete(uri2);
			}
			bTree.put(uri2,prevDoc);
			heapInsert(uri2);
			serialized.put(uri2, false);
			for(String key: bTree.get(uri2).wordMap.keySet()){
				trie.put(key,uri2);
			}
			prevDoc = null;
			return true;
		};
	}


	/**
	 * @param uri the unique identifier of the document to get
	 * @return the <B>uncompressed</B> document as a String, or null if no such document exists
	 */
	public String getDocument(URI uri) {
		if(serialized.get(uri)){
			bTree.getFromDisk(uri);
			serialized.put(uri, false);
			heapInsert(uri);
		}
		if(bTree.get(uri) == null) {
			return null;
		}
		bTree.get(uri).setLastUseTime(currentTimeMillis());
		minHeap.reHeapify(uri);
		return new String(decompress(bTree.get(uri).getDocument(),bTree.get(uri).getCompressionFormat()));
	}


	/**
	 * @param uri the unique identifier of the document to get
	 * @return the <B>compressed</B> version of the document
	 */
	public byte[] getCompressedDocument(URI uri) {
		if(serialized.get(uri)){
			bTree.getFromDisk(uri);
			serialized.put(uri, false);
			heapInsert(uri);
		}
		if(bTree.get(uri) == null){
			return null;
		}
		bTree.get(uri).setLastUseTime(currentTimeMillis());
		minHeap.reHeapify(uri);
		return bTree.get(uri).getDocument();
	}

	/**
	 * @param uri the unique identifier of the document to delete
	 * @return true if the document is deleted, false if no document exists with that URI
	 */
	public boolean deleteDocument(URI uri) {
		if(serialized.get(uri)){
			bTree.getFromDisk(uri);
			serialized.put(uri, false);
		}
		if(bTree.get(uri) == null){ return false;}
		docGraveyard.serialize(bTree.get(uri));
		deleteUndoLogic(uri, bTree.get(uri).getDocumentHashCode());
		currentByteCount -= bTree.get(uri).compressedDoc.length;
		currentDocCount--;
		heapDelete(uri);
		for(String key: bTree.get(uri).wordMap.keySet()){
			trie.delete(key,uri);
		}
		bTree.put(uri,null);
		return true;
	}

	protected void deleteUndoLogic(URI uri, int hashCode){
		Function<URI, Boolean> undo = deleteUndo(uri, hashCode);
		Function<URI, Boolean> redo = deleteRedo(uri, hashCode);
		commandStack.push(new Command(uri, undo, redo));
	}

	protected Function<URI, Boolean> deleteUndo(URI uri, int hashCode){
		return (uri2) -> {
			//restore deleted doc
			if(serialized.get(uri)){
				bTree.getFromDisk(uri);
				serialized.put(uri, false);
			}
			StackImpl<Document> stack = new StackImpl<>();
			Document doc = docGraveyard.deserialize(uri2);
			while(doc.getDocumentHashCode() != hashCode){
				stack.push(doc);
				doc = docGraveyard.deserialize(uri2);
			}
			DocumentImpl prevVal = bTree.put(uri2,doc);
			while(!stack.isEmpty()){
				docGraveyard.serialize(stack.pop());
			}
			if(prevVal != null){//need to erase
				currentByteCount -= prevVal.compressedDoc.length;
				heapDelete(uri2);
				docGraveyard.serialize(prevVal);
			}
			bTree.get(uri2).setLastUseTime(currentTimeMillis());
			heapInsert(uri2);
			serialized.put(uri, false);
			for(String key: bTree.get(uri2).wordMap.keySet()){
				trie.put(key,uri2);
			}
			return true;
		};
	}

	protected Function<URI, Boolean> deleteRedo(URI uri, int hashCode){
		return (uri2) -> {
			if(serialized.get(uri)){
				bTree.getFromDisk(uri);
				serialized.put(uri, false);
			}
			docGraveyard.serialize(bTree.get(uri2));
			bTree.put(uri2,null);
			serialized.put(uri,false);
			currentByteCount -= bTree.get(uri2).compressedDoc.length;
			currentDocCount--;
			for(String key: bTree.get(uri2).wordMap.keySet()){
				trie.delete(key,uri2);
			}
			return true;
		};
	}

	/**
	 *
	 * DO NOT IMPLEMENT IN STAGE 1 OF THE PROJECT. THIS IS FOR STAGE 2.
	 *
	 * undo the last put or delete command
	 * @return true if successfully undid command, false if not successful
	 * @throws IllegalStateException if there are no actions to be undone, i.e. the command stack is empty
	 */
	public boolean undo() throws IllegalStateException {
		if(commandStack.isEmpty()){ throw new IllegalStateException("Command stack is empty"); }

		Command command = commandStack.pop();

		return command.undo();
	}

	/**
	 *
	 * DO NOT IMPLEMENT IN STAGE 1 OF THE PROJECT. THIS IS FOR STAGE 2.
	 *
	 * undo the last put or delete that was done with the given URI as its key
	 * @param uri
	 * @return
	 * @throws IllegalStateException if there are no actions on the command stack for the given URI
	 */
	public boolean undo(URI uri) throws IllegalStateException {
		boolean notDone, result;
		notDone = result = true;

		StackImpl<Command> tempStack = new StackImpl<>();
		while(notDone){
			Command command = commandStack.peek();
			if(command.getUri().equals(uri)){
				result = command.undo();
				commandStack.pop();
				notDone = false;
			} else {
				command.undo();
				tempStack.push(commandStack.pop());
				if(commandStack.isEmpty()){throw new IllegalStateException("No actions on command stack for given uri");}
			}
		}
		while(!tempStack.isEmpty()){
			Command command = tempStack.pop();
			command.redo();
			commandStack.push(command);
		}
		return result;
	}

	/**
	 * set maximum number of documents that may be stored
	 *
	 * @param limit
	 */
	public void setMaxDocumentCount(int limit) {
		maxDocCount = limit;
		while(currentDocCount > maxDocCount){
			makeRoom();
		}
	}

	/**
	 * set maximum number of bytes of memory that may be used by all the compressed
	 * documents in memory combined
	 *
	 * @param limit
	 */
	public void setMaxDocumentBytes(int limit) {
		maxDocBytes = limit;
		while(currentByteCount > maxDocBytes){
			makeRoom();
		}
	}

	public void setStorageBaseDirectory(File dir) {
		storageBaseDir = dir;
	}

	protected void updateDocCount(){
		if(maxDocCount != 0 && (currentDocCount + 1) > maxDocCount){
			makeRoom();
			currentDocCount++;
		}
		else{
			currentDocCount++;
		}
	}

	protected void updateMemory(URI uri){
		int memoryToAdd = bTree.get(uri).compressedDoc.length;
		if(maxDocBytes != 0 && (currentByteCount + memoryToAdd) > maxDocBytes){
			makeRoom(memoryToAdd);
			currentByteCount += memoryToAdd;
		}
		else {
			currentByteCount += memoryToAdd;
		}
	}

	//Deletes least used doc from minHeap
	protected void makeRoom(){
		leaveNoTrace(minHeap.removeMin());
	}

	//Deletes as many least-used docs from minHeap as needed to make room for new doc
	protected void makeRoom(int memoryToAdd){
		while((currentByteCount + memoryToAdd) > maxDocBytes){
			makeRoom();
		}
	}

	protected void leaveNoTrace(URI uri){
		currentByteCount -= bTree.get(uri).compressedDoc.length;
		currentDocCount--;
		try {
			bTree.moveToDisk(uri);
			serialized.put(uri, true);
		} catch (Exception e) {
			//e.printStackTrace();
		}
		removeFromStack(uri);
	}

	protected void removeFromStack(URI uri){
		StackImpl<Command> tempStack = new StackImpl<>();
		while(!commandStack.isEmpty()){
			Command command = commandStack.peek();
			if(command.getUri().equals(uri)){
				commandStack.pop();
			} else {
				tempStack.push(commandStack.pop());
			}
		}
		while(!tempStack.isEmpty()){
			commandStack.push(tempStack.pop());
		}
	}

	protected void heapDelete(URI uri){
		bTree.get(uri).setLastUseTime(MY_BIRTHDAY_TIMESTAMP);
		minHeap.reHeapify(uri);
		minHeap.removeMin();
	}

	protected void heapInsert(URI uri){
		updateDocCount();
		updateMemory(uri);
		minHeap.insert(uri);
	}


	protected byte[] compress(InputStream input,CompressionFormat format){

		switch(format){
			case ZIP:	return compressZIP(input);
			case JAR: return compressJAR(input);
			case SEVENZ: return compressSEVENZ(input);
			case GZIP: return compressGZIP(input);
			case BZIP2: return compressBZIP2(input);
		}
		return null;
	}
	protected byte[] decompress(byte[] compressedDoc, CompressionFormat format){

		switch(format){
			case ZIP: return decompressZIP(compressedDoc);
			case JAR: return decompressJAR(compressedDoc);
			case SEVENZ: return decompressSEVENZ(compressedDoc);
			case GZIP: return decompressGZIP(compressedDoc);
			case BZIP2: return decompressBZIP2(compressedDoc);
		}
		return null;
	}

	private byte[] compressGZIP(InputStream input){
		try {
			byte[] toBeCompressed = input.readAllBytes();
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			GzipCompressorOutputStream gzipStream = new GzipCompressorOutputStream(byteStream);
			gzipStream.write(toBeCompressed);
			gzipStream.close();
			byteStream.close();
			return byteStream.toByteArray();
		}
		catch(IOException e){
			e.printStackTrace();
			return null;
		}
	}
	private byte[] decompressGZIP(byte[] compressedDoc){
		try {
			ByteArrayInputStream byteInput = new ByteArrayInputStream(compressedDoc);
			GzipCompressorInputStream gzipInput = new GzipCompressorInputStream(byteInput);
			return IOUtils.toByteArray(gzipInput);
		}
		catch(IOException e){
			e.printStackTrace();
			return null;
		}
	}

	private byte[] compressZIP(InputStream input){
		try {
			byte[] toBeCompressed = input.readAllBytes();
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			DeflaterOutputStream zipStream = new DeflaterOutputStream(byteStream);
			zipStream.write(toBeCompressed);
			zipStream.close();
			byteStream.close();
			return byteStream.toByteArray();
		}
		catch(IOException e){
			e.printStackTrace();
			return null;
		}

	}
	private byte[] decompressZIP(byte[] compressedDoc){
		try {
			ByteArrayInputStream byteInput = new ByteArrayInputStream(compressedDoc);
			InflaterInputStream zipInput = new InflaterInputStream(byteInput);
			return IOUtils.toByteArray(zipInput);
		}
		catch(IOException e){
			e.printStackTrace();
			return null;
		}
	}

	private byte[] compressBZIP2(InputStream input){
		try {
			byte[] toBeCompressed = input.readAllBytes();
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			BZip2CompressorOutputStream bzip2Stream = new BZip2CompressorOutputStream(byteStream);
			bzip2Stream.write(toBeCompressed);
			bzip2Stream.close();
			byteStream.close();
			return byteStream.toByteArray();
		}
		catch(IOException e){
			e.printStackTrace();
			return null;
		}
	}
	private byte[] decompressBZIP2(byte[] compressedDoc){
		try {
			ByteArrayInputStream byteInput = new ByteArrayInputStream(compressedDoc);
			BZip2CompressorInputStream bzip2Input = new BZip2CompressorInputStream(byteInput);
			return IOUtils.toByteArray(bzip2Input);
		}
		catch(IOException e){
			e.printStackTrace();
			return null;
		}
	}
	private byte[] compressJAR(InputStream input){
		try {
			byte[] toBeCompressed = input.readAllBytes();

			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			JarArchiveOutputStream jarStream = new JarArchiveOutputStream(byteStream);
			JarArchiveEntry temp = new JarArchiveEntry("");
			jarStream.putArchiveEntry(temp);
			jarStream.write(toBeCompressed);
			jarStream.closeArchiveEntry();
			jarStream.close();
			byteStream.close();
			return byteStream.toByteArray();
		}
		catch(IOException e){
			e.printStackTrace();
			return null;
		}
	}
	private byte[] decompressJAR(byte[] compressedDoc){
		try {
			ByteArrayInputStream byteInput = new ByteArrayInputStream(compressedDoc);
			JarArchiveInputStream jarInput = new JarArchiveInputStream(byteInput);
			jarInput.getNextEntry();
			return IOUtils.toByteArray(jarInput);
		}
		catch(IOException e){
			e.printStackTrace();
			return null;
		}
	}
	private byte[] compressSEVENZ(InputStream input){
		try{

			byte[] toBeCompressed = input.readAllBytes();

			//7z archives NEED a file destination so just make a temp one
			File destination = File.createTempFile("prefix-","-suffix");
			destination.deleteOnExit();

			SevenZOutputFile sevenZOutFile = new SevenZOutputFile(destination);
			sevenZOutFile.putArchiveEntry(sevenZOutFile.createArchiveEntry(destination, ""));
			sevenZOutFile.write(toBeCompressed);
			sevenZOutFile.closeArchiveEntry();
			sevenZOutFile.close();

			//Now the file contains 7z compressed data
			FileInputStream fileInput = new FileInputStream(destination);
			return fileInput.readAllBytes();
		}
		catch(IOException e){
			e.printStackTrace();
			return null;
		}
	}
	private byte[] decompressSEVENZ(byte[] compressedDoc){
		try{
			File tempFile = File.createTempFile("prefix","suffix");
			tempFile.deleteOnExit();
			FileOutputStream bytesToFile = new FileOutputStream(tempFile);
			ByteArrayInputStream inp = new ByteArrayInputStream(compressedDoc);
			IOUtils.copy(inp, bytesToFile);
			bytesToFile.close();


			SevenZFile sevenZFile = new SevenZFile(tempFile);
			SevenZArchiveEntry entry = sevenZFile.getNextEntry();
			long length = entry.getSize();
			byte[] result = new byte[(int)length];
			sevenZFile.read(result,0,result.length);
			sevenZFile.close();
			return result;

		}
		catch(IOException e){
			e.printStackTrace();
			return null;
		}
	}
}
