package com.twardyece.dmtf.rust;

import com.twardyece.dmtf.text.PascalCaseName;
import com.twardyece.dmtf.text.SnakeCaseName;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RustIdentifier implements Comparable<RustIdentifier> {
    private String value;
    private RustType owner;
    private static final Pattern numbersOnly = Pattern.compile("^[0-9]+$");

    public RustIdentifier(PascalCaseName name) {
        this.value = name.toString();
        Matcher matcher = numbersOnly.matcher(this.value);
        if (matcher.find()) {
            this.value = "_" + this.value;
        }
    }

    public RustIdentifier(SnakeCaseName name) {
        this.value = name.toUpperCase();
        Matcher matcher = numbersOnly.matcher(this.value);
        if (matcher.find()) {
            this.value = "_" + this.value;
        }
    }

    public RustIdentifier(RustType owner, PascalCaseName name) {
        this(name);
        this.owner = owner;
    }

    @Override
    public String toString() {
        String value = this.value;
        if (null != this.owner) {
            value = this.owner + "::" + value;
        }

        return value;
    }

    @Override
    public int compareTo(RustIdentifier o) {
        return this.toString().compareTo(o.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof RustIdentifier) {
            return this.toString().equals(o.toString());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }
}
