*******************************************************************************************
				Simple Search Engine
*******************************************************************************************

The general purpose class in this project is DocumentStoreImpl.java
A user of DocumentStoreImpl (dsi) has the capability of:
- put a document in the dsi
- keyword search all dsi documents
- get a document in either compressed or uncompressed form
- delete a document
- set max document count in memory
- set max document bytes in memory
- undo last action
- undo last action on given document key

Support classes in this project are:

DocumentImpl.java
- Object encapsulating documents

BTreeImpl.java
- BTree class for document storage and O(n) retrieval

DocumentIOImpl.java
- Utility class for document serialization/deserialization

MinHeapImpl.java
- tracks in-memory document usage to determine which documents to serialize when memory limits are reached

StackImpl.java
- undo stack

TrieImpl.java
- provides keyword search capability
