package com.example.musicplayer.adapters;


import java.util.ArrayList;

public class SelectionArray {

    static class node
    {
        int position;
        node next;

        node(int position, node next) {
            this.position = position;
            this.next = next;
        }

    }

    private node first = null;

    public void put (int position)
    {
        node curr, temp;
        temp = first;
        boolean flag = true;

        if (first == null) {
            first = new node(position, null);
        }
        else {

            while (temp != null && flag) {

                if (temp.next == null) {

                    curr = new node(position, null);
                    temp.next = curr;
                    flag = false;

                } else {
                    temp = temp.next;
                }
            }
        }

    }

    public ArrayList<Integer> getSelectedItem(){

        ArrayList<Integer> selectedItems = new ArrayList<>();

        node temp;
        temp=first;

        while (temp != null) {
            selectedItems.add(temp.position);
            temp = temp.next;
        }

        return selectedItems;
    }

    public void clear(){

        first = null;
    }

    public void remove (int position){

        node curr, prev;
        curr = first;
        prev = null;

        while (curr != null) {


            if (curr == first && curr.position == position){

                first = first.next;
                curr = null;
            }
            else if (curr.position == position){
                prev.next = curr.next;
                curr = null;
            }
            else{
                prev = curr;
                curr = curr.next;
            }
        }

    }

    boolean contains(int position){

        node curr;
        curr = first;

        while (curr != null) {

            if (curr.position == position){
                return true;
            }
            else{
                curr = curr.next;
            }
        }

        return false;
    }

    public int size(){

        int count = 0;
        node temp;
        temp=first;

        while (temp != null) {
            count++;
            temp = temp.next;
        }
        return count;
    }


}
