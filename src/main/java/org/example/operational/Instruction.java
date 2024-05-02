package org.example.operational;

import org.example.Registry;

import java.util.Map;

public abstract class Instruction {

    public abstract boolean execute(Map<String, Short> dataRegistries);
}
