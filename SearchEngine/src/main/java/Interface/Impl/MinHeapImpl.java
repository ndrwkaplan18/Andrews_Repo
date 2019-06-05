package Interface.Impl;

import Interface.MinHeap;

import java.util.*;

public class MinHeapImpl<E extends Comparable> extends MinHeap<E> {
    /*
    protected E[] elements;
    protected int count;
    */
    protected Map<E,Integer> elementsToArrayIndex; //used to store the index in the elements array

    protected E[] elements;
    protected Comparator<E> comparator;

    //Normally I would initialize a generic via
    //E[] array = (E[]) new Object[size];
    //but because E extends Comparable that isn't possible in this case
    //So I'm just requiring the user to initialize the array for me and pass it in the constructor here
    public MinHeapImpl(E[] typeArray, Comparator<E> comparator){
        elements =  typeArray;
        this.comparator = comparator;
        elementsToArrayIndex = new HashMap<>();
    }

    public void reHeapify(Comparable element) {
        if(elementsToArrayIndex.containsKey(element)) {
            this.downHeap(this.getArrayIndex(element));
            this.upHeap(this.getArrayIndex(element));
        }
    }

    protected int getArrayIndex(Comparable element) {
            return elementsToArrayIndex.get(element);
    }

    //Normally I would double the array explicitly like I've done in other classes, but because of the problem
    //I explained in the constructor, I have to deal with this array indirectly, so I'm using this method instead.
    protected void doubleArraySize() {
        elements = Arrays.copyOf(elements, elements.length*2);
    }

    @Override
     protected  boolean isEmpty()
    {
        return this.count == 0;
    }
    /**
     * is elements[i] > elements[j]?
     */
     @Override
    protected  boolean isGreater(int i, int j)
    {
        return comparator.compare(this.elements[i],this.elements[j]) > 0;
    }

    /**
     * swap the values stored at elements[i] and elements[j]
     */
    @Override
    protected  void swap(int i, int j)
    {
        E temp = this.elements[i];
        this.elements[i] = this.elements[j];
        elementsToArrayIndex.put(this.elements[i],i);
        this.elements[j] = temp;
        elementsToArrayIndex.put(this.elements[j],j);
    }

    /**
     *while the key at index k is less than its
     *parent's key, swap its contents with its parent’s
     */
    @Override
    protected  void upHeap(int k)
    {
        while (k > 1 && this.isGreater(k / 2, k))
        {
            this.swap(k, k / 2);
            k = k / 2;
        }
    }

    /**
     * move an element down the heap until it is less than
     * both its children or is at the bottom of the heap
     */
    @Override
    protected  void downHeap(int k)
    {
        while (2 * k <= this.count)
        {
            //identify which of the 2 children are smaller
            int j = 2 * k;
            if (j < this.count && this.isGreater(j, j + 1))
            {
                j++;
            }
            //if the current value is < the smaller child, we're done
            if (!this.isGreater(k, j))
            {
                break;
            }
            //if not, swap and continue testing
            this.swap(k, j);
            k = j;
        }
    }
    @Override
    public void insert(E x)
    {
        // double size of array if necessary
        if (this.count >= this.elements.length - 1)
        {
            this.doubleArraySize();
        }
        //add x to the bottom of the heap
        this.elements[++this.count] = x;
        elementsToArrayIndex.put(x,this.count);
        //percolate it up to maintain heap order property
        this.upHeap(this.count);
    }
    @Override
    public E removeMin()
    {
        if (isEmpty())
        {
            throw new NoSuchElementException("Heap is empty");
        }
        E min = this.elements[1];
        //swap root with last, decrement count
        this.swap(1, this.count--);
        //move new root down as needed
        this.downHeap(1);
        this.elements[this.count + 1] = null; //null it to prepare for GC
        elementsToArrayIndex.remove(min);
        return min;
    }

}
