package org.codeisland.aggregato.service.api;

import java.util.Random;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class Bean {

    private String data;

    public Bean() {
    }

    public Bean(String s) {
        this.data = s;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getNumber() {
        return new Random().nextInt();
    }
}
