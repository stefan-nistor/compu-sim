package org.example;

import org.example.operational.Add;

public class Main {

    public static void main(String[] args) {
        Processor processor = Processor.getInstance();
        processor.execute(new Add("AX", "BX"));
    }
}