package net.prsv.rengine;

public class Main {
    public static void main(String[] args) {
        String pattern = "ab*c|ca*bd";
        RegexParser rp = new RegexParser(pattern);
    }
}
