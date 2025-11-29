package ru.nsu.chernikov;

import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;
import java.util.ArrayList;
import java.util.List;

public class MyLinkedList implements Iterable<String> {
    private Node head = null;
    private final ReentrantLock listLock = new ReentrantLock(true);

    private static class Node {
        String value;
        Node next;

        public Node(String value) {
            this.value = value;
        }
    }

    public void addFirst(String value) {
        listLock.lock();
        try {
            Node newNode = new Node(value);
            newNode.next = head;
            head = newNode;
        } finally {
            listLock.unlock();
        }
    }

    // Быстрый метод для одного шага сортировки
    public int bubbleSortStep() throws InterruptedException {
        listLock.lock();
        try {
            if (head == null || head.next == null) return 0;

            int swapsCount = 0;
            Node prev = null;
            Node curr = head;

            while (curr != null && curr.next != null) {
                Node next = curr.next;

                if (curr.value.compareTo(next.value) > 0) {
                    // Меняем узлы местами
                    curr.next = next.next;
                    next.next = curr;

                    if (prev == null) {
                        head = next;
                    } else {
                        prev.next = next;
                    }

                    prev = next;
                    swapsCount++;
                    break;
                } else {
                    prev = curr;
                    curr = curr.next;
                }
            }

            // задержка внутри шага
            if (swapsCount > 0) {
                Thread.sleep(1);
            }

            return swapsCount;
        } finally {
            listLock.unlock();
        }
    }

    @Override
    public Iterator<String> iterator() {
        listLock.lock();
        try {
            // копия для безопасного итерирования
            List<String> copy = new ArrayList<>();
            Node current = head;
            while (current != null) {
                copy.add(current.value);
                current = current.next;
            }
            return copy.iterator();
        } finally {
            listLock.unlock();
        }
    }

    public int size() {
        listLock.lock();
        try {
            int size = 0;
            Node current = head;
            while (current != null) {
                size++;
                current = current.next;
            }
            return size;
        } finally {
            listLock.unlock();
        }
    }

}