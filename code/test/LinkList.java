package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.A;

public class LinkList {
    LinkList next;
    LinkList geti(int i) {
        LinkList l = this;
        for(int k = 0; k < i; ++k) {
            l = l.next;
        }
        return l;
    }
    void setNext(LinkList n) {
        this.next = n;
    }
    LinkList getNext() {
        return next;
    }

    public static void main(String[] args) {
        BenchmarkN.alloc(1);
        LinkList node1 = new LinkList();
        BenchmarkN.alloc(2);
        LinkList node2 = new LinkList();
        BenchmarkN.alloc(3);
        LinkList node3 = new LinkList();
        BenchmarkN.alloc(4);
        LinkList node4 = new LinkList();
        BenchmarkN.alloc(5);
        LinkList node5 = new LinkList();
        BenchmarkN.alloc(6);
        LinkList node6 = new LinkList();
        node1.setNext(node2);
        node2.setNext(node3);
        node3.setNext(node4);
        node4.setNext(node5);
        node5.setNext(node6);

        LinkList test1 = node1.geti(3);
        BenchmarkN.test(1, test1); // {1,2,3,4,5,6}
        LinkList test2 = node1.getNext().getNext().getNext();
        BenchmarkN.test(2, test2); // {4}
        LinkList test3 = node1.next.next.next;
        BenchmarkN.test(3, test3); // {4}
    }
}