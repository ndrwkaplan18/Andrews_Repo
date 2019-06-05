package Interface.Impl;

import Interface.Stack;

public class StackImpl<T> implements Stack<T> {

    protected final int DEFAULT_SIZE = 100;
    protected T[] stack = (T[]) new Object[DEFAULT_SIZE];
    protected int top;//Both the index at which to insert a new element and the current size of the stack
    
    /**
     * @param element object to add to the Stack
     */
    public void push(T element) {
        if(size() == stack.length){
            resize();
        }
        stack[top] = element;
        top++;
    }

    /**
     * removes and returns element at the top of the stack
     *
     * @return element at the top of the stack, null if the stack is empty
     */
    public T pop() {
        if(isEmpty()) return null;

        top--;
        T element = stack[top];
        stack[top] = null;
        return element;
    }

    /**
     * @return the element at the top of the stack without removing it
     */
    public T peek() {

        if(isEmpty()) return null;
        return stack[top-1];
    }

    /**
     * @return how many elements are currently in the stack
     */
    public int size() { return top; }

    protected void resize(){
        T[] temp = stack;
        this.stack = (T[]) new Object[stack.length * 2];

        for(int i = 0; i <= temp.length; i++){
            stack[i] = temp[i];
        }
    }

    protected boolean isEmpty(){
        return size() == 0;
    }
}
