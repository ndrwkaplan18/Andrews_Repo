package Interface.Impl;

import Interface.HashTable;

public class HashTableImpl<Key,Value> implements HashTable<Key, Value> {

	class Node<Key,Value> {
		Node next;
		Key key;
		Value value;

		Node(Key k, Value v) {
			this.key = k;
			this.value = v;
		}

		public boolean equals(Object var1) {
			if (this == var1) {
				return true;
			} else if (var1 == null) {
				return false;
			} else if (this.getClass() != var1.getClass()) {
				return false;
			} else {
				Node node = (Node)var1;
				return this.key.equals(node.key) && this.value.equals(node.value);
			}
		}
	}//End of Node subclass

	//Since we're going to be doubling the array as needed, let's have an instance variable arraySize
	protected final int DEFAULT_ARRAY_SIZE = 997;
	protected int arraySize = DEFAULT_ARRAY_SIZE;
	protected int count; //Count will keep track of the number of puts in the array
	//Because we're dealing with collisions via separate chaining, we need the base array to be of type Node
	protected Node<Key,Value>[] array;
	
	/**
	 * no argument constructor
	 */
	public HashTableImpl() {
		this.array = new Node[arraySize];
	}
	public HashTableImpl(int arraySize){
		this.arraySize = arraySize;
		this.array = new Node[arraySize];
	}


    /**
     * @param k the key whose value should be returned
     * @return the value that is stored in the HashTable for k, or null if there is no such key in the table
     */

	public Value get(Key k) {
		if(k == null){
			throw new IllegalArgumentException("Key cannot be null!");
		}

    	//Check if hashed index contains any nodes, if not return null
    	if(array[index(k)]==null){
    		return null;
		}
    	//Now there is a node there. Set variable current to that value
    	Node<Key,Value> current = array[index(k)];
    	//Step through however many nodes there are in this index
    	while(current != null){
    		if(current.key.equals(k)){//Check if current.key equals parameter Key k
    			return current.value;
			}
    		current = current.next;//Increment to next node
		}
    	//There are no nodes which match Key k
    	return null;
    }

    /**
     * @param k the key at which to store the value
     * @param v the value to store
     * @return if the key was already present in the HashTable, return the previous value stored for the key. If the key was not already present, return null.
     */

    public Value put(Key k, Value v) {

    	if(v == null){ return delete(k,v); }
    	//Maintain ratio of 4:1 hashtable elements to array size (respectively)
    	if(count/arraySize >= 4) { resize(); }

    	Node<Key,Value> newNode = new Node<>(k,v);
    	//If the index does not already contain an initialized Node, just put newNode there
    	if(array[index(k)] == null){
    		array[index(k)] = newNode;
    		count++;
    		return null;
		}
		Node<Key,Value> current = array[index(k)];
		//If the index already contains a node, find last one and add there
		//If any of those nodes' keys == this value's key, return previous value stored
		while (current != null) {
			if (current.key.equals(k)) {//Current.key equals parameter Key k
				Value val = current.value;//Store previous value in val
				current.value = v;
				return val;
			}
			if(current.next != null){
				current = current.next;//Increment to next node
			} else break;
		}
		//Now current.next = null so we'll insert the newNode there
		current.next = newNode;
		count++;
		return null;
    }
    
    //hash the key,value pair to an index
    protected int index(Key k) {
    	if(k == null) {
    		return -1;
    	}
    	int hashCode = Math.abs(k.hashCode());
    	int result = hashCode % arraySize;
		return result;
    }

	/**
	 * Helper method. Sets new array size to a prime number roughly double the previous size
	 * @see HashTableImpl nextPrime(int n)
	 * @see HashTableImpl isPrime(int n)
	 */
	protected void resize() {
    	
    	this.arraySize = nextPrime(arraySize*2); //Double size of array and find next prime
    	Node<Key,Value>[] temp = array; //Copy previous array
    	this.array = new Node[arraySize]; //Make new instance array of size arraySize

		int oldCount = count;//Every time this method calls put() to fill the new array, it will increment count
		//so we need to store count at this point to make sure at the end of this method count is accurate

		//walk through old array (temp) and put() all values in new array
    	for(int i = 0; i < temp.length; i++) {
			Node<Key,Value> current = temp[i];
    		if(current==null) continue;

			while(current != null){
				put(current.key, current.value);
				current = current.next;//Increment to next node
			}
    	}//End for loop
		count = count - oldCount;//correct count variable to reflect actual count
    }
    
	/**
	 * Helper method. Finds next prime number.
	 * @param n
	 * @return int next prime num
	 * @see HashTableImpl isPrime(int n)
	 */
	protected int nextPrime(int n) {
		int next = (n%2==0) ? ++n : n+2;
	
		while(!isPrime(next)) {
			next+=2;	
		}
		return next;
		
	}
	
	/**
	 * Helper method. Checks if n is prime
	 * @param n
	 * @return boolean
	 */
	protected boolean isPrime(int n) {
		if(n <= 1){
			return false;
		}
		int index = 2;
		while(index <= Math.sqrt(n)){
			//Any composite number, n, has a factor f <= sqrt(n)
			if(n % index == 0) return false;
			index++;
	    }
		return true;			
	}

	protected Value delete(Key k, Value v){
		Node<Key,Value> current = array[index(k)];
		Node<Key,Value> previous, first;
		previous = first = current;
		while (current != null) {
			if (current.key.equals(k)) {//Current.key equals parameter Key k
				Value val = current.value;//Store previous value in val
				//Node is first in list
				if (current.equals(first)) {
					if (current.next == null) {
						array[index(k)] = null;
					}//First & only
					else {
						array[index(k)] = current.next;
					}
				} else {
					previous.next = current.next;
				}//Middle or last
				count--;
				return val;
			}
		}
		return null;
	}

}
